package com.mdyaipay.tools.trace.internal;

import java.security.SecureRandom;

/**
 * 生成 W3C 规范的随机 traceId / spanId（十六进制）。
 */
public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
    }

    public static String newTraceId() {
        return randomHex(16);
    }

    public static String newSpanId() {
        return randomHex(8);
    }

    private static String randomHex(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(byteLength * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
