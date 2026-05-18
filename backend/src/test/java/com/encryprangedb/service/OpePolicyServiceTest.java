package com.encryprangedb.service;

import com.encryprangedb.config.CryptoProperties;
import com.encryprangedb.mapper.AnalyticsMapper;
import com.encryprangedb.model.OpePolicyRequest;
import com.encryprangedb.model.entity.OpePolicyEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class OpePolicyServiceTest {

    private CryptoProperties buildProps() {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Crypto c = new CryptoProperties.Crypto();
        c.setAesMasterKey("0123456789abcdef0123456789abcdef");
        c.setOpeMasterKey("0123456789abcdef0123456789abcdef");
        c.setSensitivity(1);
        props.setCrypto(c);
        return props;
    }

    @Test
    void encryptShouldBeDeterministicForSameValue() {
        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);
        when(analyticsMapper.activePolicy()).thenReturn(null);

        OpePolicyService service = new OpePolicyService(analyticsMapper, buildProps(), new ObjectMapper());
        long a = service.encrypt(75);
        long b = service.encrypt(75);
        assertEquals(a, b);
    }

    @Test
    void encryptShouldPreserveOrderForNearbyValues() {
        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);
        when(analyticsMapper.activePolicy()).thenReturn(null);

        OpePolicyService service = new OpePolicyService(analyticsMapper, buildProps(), new ObjectMapper());
        assertTrue(service.encrypt(60) <= service.encrypt(61));
    }

    @Test
    void getActivePolicyShouldReadConfiguredSegments() {
        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);
        OpePolicyEntity active = new OpePolicyEntity();
        active.setId(1L);
        active.setPolicyName("salary-policy");
        active.setSensitivity(2);
        active.setActiveFlag(true);
        active.setSegmentJson("""
                [
                  {"minValue":0,"a":2,"b":5,"label":"s1"},
                  {"minValue":100,"a":4,"b":10,"label":"s2"}
                ]
                """);
        when(analyticsMapper.activePolicy()).thenReturn(active);

        OpePolicyService service = new OpePolicyService(analyticsMapper, buildProps(), new ObjectMapper());
        var policy = service.getActivePolicy();
        assertEquals("salary-policy", policy.policyName());
        assertEquals(2, policy.sensitivity());
        assertEquals(2, policy.segments().size());
    }

    @Test
    void saveActivePolicyShouldFallbackToRequestWhenMapperReadBackFails() {
        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);
        when(analyticsMapper.activePolicy()).thenReturn(null);

        OpePolicyService service = new OpePolicyService(analyticsMapper, buildProps(), new ObjectMapper());
        var request = new OpePolicyRequest(
                "manual-policy",
                3,
                List.of(new OpePolicyRequest.Segment(0, 3, 10, "default"))
        );
        var response = service.saveActivePolicy(request);
        assertEquals("manual-policy", response.policyName());
        assertEquals(3, response.sensitivity());
        assertEquals(1, response.segments().size());
    }
}
