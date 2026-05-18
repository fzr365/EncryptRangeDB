package com.encryprangedb.service;

import com.encryprangedb.config.CryptoProperties;
import com.encryprangedb.mapper.AnalyticsMapper;
import com.encryprangedb.model.entity.OpePolicyEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.JSQLParserException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlRewriteServiceTest {

    private SqlRewriteService buildService() {
        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);

        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Crypto c = new CryptoProperties.Crypto();
        c.setAesMasterKey("0123456789abcdef0123456789abcdef");
        c.setOpeMasterKey("0123456789abcdef0123456789abcdef");
        c.setSensitivity(1);
        props.setCrypto(c);

        OpePolicyEntity active = new OpePolicyEntity();
        active.setId(1L);
        active.setPolicyName("test-policy");
        active.setSensitivity(1);
        active.setActiveFlag(true);
        active.setSegmentJson("""
                [
                  {"minValue":0,"a":3,"b":10,"label":"low-range"},
                  {"minValue":100,"a":5,"b":20,"label":"mid-range"},
                  {"minValue":500,"a":10,"b":30,"label":"high-range"}
                ]
                """);
        Mockito.when(analyticsMapper.activePolicy()).thenReturn(active);

        OpePolicyService opePolicyService = new OpePolicyService(analyticsMapper, props, new ObjectMapper());
        return new SqlRewriteService(opePolicyService);
    }

    @Test
    void translateBetween() throws JSQLParserException {
        SqlRewriteService service = buildService();
        var req = service.translateRange("SELECT * FROM employees WHERE salary BETWEEN 20 AND 40");
        assertEquals("employees", req.table());
        assertEquals("salary", req.column());
        assertEquals(true, req.lowerIndex() <= req.upperIndex());
    }

    @Test
    void translateCombinedBounds() throws JSQLParserException {
        SqlRewriteService service = buildService();
        var req = service.translateRange("SELECT * FROM employees WHERE salary >= 20 AND salary <= 40");
        assertEquals("employees", req.table());
        assertEquals("salary", req.column());
        assertEquals(true, req.lowerIndex() <= req.upperIndex());
    }

    @Test
    void rejectMultiColumnRangePredicates() {
        SqlRewriteService service = buildService();
        assertThrows(IllegalArgumentException.class,
                () -> service.translateRange("SELECT * FROM employees WHERE salary >= 20 AND age <= 40"));
    }

    @Test
    void rejectMissingWhereClause() {
        SqlRewriteService service = buildService();
        assertThrows(IllegalArgumentException.class,
                () -> service.translateRange("SELECT * FROM employees"));
    }
}
