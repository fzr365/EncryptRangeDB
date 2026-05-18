package com.encryprangedb.service;

import com.encryprangedb.model.PlainInsertRequest;
import com.encryprangedb.model.SqlImportResult;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.insert.Insert;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SqlImportService {

    private final RecordService recordService;

    public SqlImportService(RecordService recordService) {
        this.recordService = recordService;
    }

    public SqlImportResult importSql(String sql) throws JSQLParserException {
        Statements statements = CCJSqlParserUtil.parseStatements(sql);
        int total = statements.getStatements().size();
        int handledInsert = 0;
        int inserted = 0;
        List<String> errors = new ArrayList<>();

        for (Statement st : statements.getStatements()) {
            if (st instanceof Insert insert) {
                handledInsert++;
                try {
                    inserted += handleInsert(insert);
                } catch (RuntimeException ex) {
                    errors.add("INSERT failed: " + ex.getMessage());
                }
            }
        }

        return new SqlImportResult(total, handledInsert, inserted, errors);
    }

    private int handleInsert(Insert insert) {
        if (insert.getTable() == null) {
            throw new IllegalArgumentException("INSERT table is missing");
        }
        if (insert.getColumns() == null || insert.getColumns().isEmpty()) {
            throw new IllegalArgumentException("INSERT columns are required");
        }
        if (insert.getValues() == null) {
            throw new IllegalArgumentException("INSERT VALUES are required");
        }

        String table = insert.getTable().getName();
        var columns = insert.getColumns();
        var rows = extractRows(insert);
        if (rows.isEmpty()) {
            return 0;
        }

        int inserted = 0;
        for (var row : rows) {
            if (columns.size() != row.size()) {
                throw new IllegalArgumentException("INSERT columns and values size mismatch");
            }
            inserted += insertRow(insert.getTable().getName(), columns, row);
        }
        return inserted;
    }

    private int insertRow(String table, List<Column> columns, List<Expression> rowValues) {
        String recordId = null;
        List<PlainInsertRequest.Field> fields = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i).getColumnName();
            Expression expr = rowValues.get(i);
            Object val = extractValue(expr);
            if ("id".equalsIgnoreCase(col) || "record_id".equalsIgnoreCase(col)) {
                recordId = String.valueOf(val);
                continue;
            }
            boolean indexed = isNumericLiteral(expr) && isLikelyIndexedColumn(col);
            fields.add(new PlainInsertRequest.Field(col, val, indexed));
        }
        if (recordId == null || recordId.isBlank()) {
            recordId = "sql-" + System.currentTimeMillis();
        }
        recordService.insertPlain(new PlainInsertRequest(table, recordId, fields));
        return 1;
    }

    private List<List<Expression>> extractRows(Insert insert) {
        var expressions = insert.getValues().getExpressions();
        List<List<Expression>> rows = new ArrayList<>();
        for (Expression expression : expressions) {
            if (expression instanceof ParenthesedExpressionList<?> row) {
                rows.add(new ArrayList<>(row));
                continue;
            }
            if (expression instanceof ExpressionList<?> row) {
                rows.add(new ArrayList<>(row));
                continue;
            }
        }
        if (!rows.isEmpty()) {
            return rows;
        }
        rows.add(new ArrayList<>(expressions));
        return rows;
    }

    private boolean isLikelyIndexedColumn(String column) {
        // For demo: index typical numeric columns. You can refine by schema config later.
        return "salary".equalsIgnoreCase(column) || "age".equalsIgnoreCase(column) || column.endsWith("_id");
    }

    private boolean isNumericLiteral(Expression expr) {
        return expr != null && expr.toString().matches("[-+]?\\d+(\\.\\d+)?");
    }

    private Object extractValue(Expression expr) {
        if (expr instanceof StringValue sv) {
            return sv.getValue();
        }
        String raw = expr == null ? "" : expr.toString();
        if (raw.matches("[-+]?\\d+")) {
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ignored) {
            }
        }
        if (raw.matches("[-+]?\\d+\\.\\d+")) {
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException ignored) {
            }
        }
        return raw;
    }
}
