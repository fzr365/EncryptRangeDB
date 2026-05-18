package com.encryprangedb.ope;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgrammableOPETest {

    @Test
    void deterministicForSameInput() {
        ProgrammableOPE ope = new ProgrammableOPE("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), 32);
        long a = ope.encrypt(75);
        long b = ope.encrypt(75);
        assertEquals(a, b);
    }

    @Test
    void shouldPreserveOrderInTypicalCase() {
        ProgrammableOPE ope = new ProgrammableOPE("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), 1);
        long x = ope.encrypt(60);
        long y = ope.encrypt(61);
        assertTrue(x <= y);
    }
}

