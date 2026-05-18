package com.encryprangedb.service;

import com.encryprangedb.model.RangeQueryRequest;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Service;

@Service
public class SqlRewriteService {

    private final OpePolicyService opePolicyService;

    public SqlRewriteService(OpePolicyService opePolicyService) {
        this.opePolicyService = opePolicyService;
    }

    public RangeQueryRequest translateRange(String sql) throws JSQLParserException {
        // 把用户输入的 SELECT 范围查询翻译成后端索引查询参数。
        Statement statement = CCJSqlParserUtil.parse(sql);
        if (!(statement instanceof Select select)) {
            throw new IllegalArgumentException("Only SELECT is supported");
        }
        PlainSelect body = (PlainSelect) select.getSelectBody();
        Table table = (Table) body.getFromItem();
        Expression where = body.getWhere();
        if (where == null) {
            throw new IllegalArgumentException("WHERE clause is required");
        }

        Bounds bounds = new Bounds();
        collect(where, bounds);
        if (bounds.column == null) {
            throw new IllegalArgumentException("No supported range predicate found");
        }
        long lower = bounds.lower == null ? Long.MIN_VALUE / 4 : bounds.lower;
        long upper = bounds.upper == null ? Long.MAX_VALUE / 4 : bounds.upper;
        // 明文上下界先经过 OPE 映射，再拿密文索引范围去查。
        long lowerIdx = opePolicyService.encrypt(lower);
        long upperIdx = opePolicyService.encrypt(upper);
        if (lowerIdx > upperIdx) {
            long tmp = lowerIdx;
            lowerIdx = upperIdx;
            upperIdx = tmp;
        }
        return new RangeQueryRequest(table.getName(), bounds.column, lowerIdx, upperIdx);
    }

    private void collect(Expression expr, Bounds bounds) {
        // 目前只支持 AND 连接的单列范围条件。
        if (expr instanceof AndExpression andExpr) {
            collect(andExpr.getLeftExpression(), bounds);
            collect(andExpr.getRightExpression(), bounds);
            return;
        }
        if (expr instanceof Between between) {
            String col = columnName(between.getLeftExpression());
            ensureColumn(bounds, col);
            long lo = numericLong(between.getBetweenExpressionStart());
            long hi = numericLong(between.getBetweenExpressionEnd());
            // 多个条件同时出现时取交集。
            bounds.lower = bounds.lower == null ? lo : Math.max(bounds.lower, lo);
            bounds.upper = bounds.upper == null ? hi : Math.min(bounds.upper, hi);
            return;
        }
        if (expr instanceof GreaterThanEquals gte) {
            String col = columnName(gte.getLeftExpression());
            ensureColumn(bounds, col);
            long lo = numericLong(gte.getRightExpression());
            bounds.lower = bounds.lower == null ? lo : Math.max(bounds.lower, lo);
            return;
        }
        if (expr instanceof MinorThanEquals lte) {
            String col = columnName(lte.getLeftExpression());
            ensureColumn(bounds, col);
            long hi = numericLong(lte.getRightExpression());
            bounds.upper = bounds.upper == null ? hi : Math.min(bounds.upper, hi);
            return;
        }
        throw new IllegalArgumentException("Unsupported WHERE expression: " + expr);
    }

    private String columnName(Expression expr) {
        if (expr instanceof Column col) {
            return col.getColumnName();
        }
        throw new IllegalArgumentException("Left side must be a column");
    }

    private long numericLong(Expression expr) {
        if (expr instanceof LongValue lv) {
            return lv.getValue();
        }
        if (expr instanceof DoubleValue dv) {
            return (long) dv.getValue();
        }
        throw new IllegalArgumentException("Only numeric literals are supported");
    }

    private void ensureColumn(Bounds bounds, String col) {
        if (bounds.column == null) {
            bounds.column = col;
            return;
        }
        if (!bounds.column.equalsIgnoreCase(col)) {
            throw new IllegalArgumentException("Only single-column range predicates are supported");
        }
    }

    private static class Bounds {
        private String column;
        private Long lower;
        private Long upper;
    }
}
