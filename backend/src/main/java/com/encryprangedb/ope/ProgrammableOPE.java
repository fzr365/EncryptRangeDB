package com.encryprangedb.ope;

import com.encryprangedb.crypto.HmacUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Programmable order-preserving index with bounded noise:
 * index = a * value + b + noise, where noise in [0, a*sensitivity].
 *
 */
public class ProgrammableOPE {

    private final byte[] masterKey;
    private final long sensitivity;

    public ProgrammableOPE(byte[] masterKey, long sensitivity) {
        if (masterKey == null || masterKey.length < 16) {
            throw new IllegalArgumentException("masterKey must be at least 16 bytes");
        }
        if (sensitivity < 0) {
            throw new IllegalArgumentException("sensitivity must be >= 0");
        }
        this.masterKey = masterKey.clone();
        this.sensitivity = sensitivity;
    }

    public long encrypt(long value) {
        Segment seg = selectSegment(value);
        long skindex = Math.addExact(Math.multiplyExact(seg.a, value), seg.b);
        long bound = Math.max(0L, Math.multiplyExact(seg.a, sensitivity));
        long noise = boundedNoise("default", value, bound);
        return Math.addExact(skindex, noise);
    }

    private Segment selectSegment(long value) {
        if (value < 100) {
            return new Segment(3, 10);
        }
        if (value < 500) {
            return new Segment(5, 20);
        }
        return new Segment(10, 30);
    }

    private long boundedNoise(String column, long value, long boundInclusive) {
        if (boundInclusive <= 0) {
            return 0L;
        }
        byte[] msg = (column + ":" + value).getBytes(StandardCharsets.UTF_8);
        byte[] mac = HmacUtil.hmacSha256(masterKey, msg);
        long seed = ByteBuffer.wrap(mac, 0, 8).getLong() & Long.MAX_VALUE;
        return seed % (boundInclusive + 1);
    }

    private record Segment(long a, long b) {
    }
}

