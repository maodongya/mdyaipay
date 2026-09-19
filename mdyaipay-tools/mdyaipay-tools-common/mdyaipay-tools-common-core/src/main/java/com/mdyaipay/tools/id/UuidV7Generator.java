package com.mdyaipay.tools.id;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * RFC 9562 UUID 版本 7 生成器：48 位毫秒时间戳 + 随机位，适合排序主键。
 * <p>
 * 线程安全；同一毫秒内通过 12 位序列递增保证单调性（溢出则等待下一毫秒）。
 */
public final class UuidV7Generator {

    private static final int SEQUENCE_MASK = 0xFFF;

    private final SecureRandom random = new SecureRandom();
    private long lastMillis = -1L;
    private int sequence;

    /**
     * 生成下一个 UUIDv7。
     */
    public synchronized UUID nextUuid() {
        long now = System.currentTimeMillis();
        if (now == lastMillis) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                now = waitNextMillis(now);
            }
        } else {
            sequence = random.nextInt(SEQUENCE_MASK + 1);
            lastMillis = now;
        }
        long msb = (now << 16) | (0x7L << 12) | (sequence & (long) SEQUENCE_MASK);
        long lsb = (random.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }

    /**
     * 生成 UUIDv7 并编码为 {@link UuidV7Binary#toBytes(UUID)}。
     */
    public byte[] nextBytes() {
        return UuidV7Binary.toBytes(nextUuid());
    }

    private long waitNextMillis(long current) {
        long now = System.currentTimeMillis();
        while (now <= current) {
            now = System.currentTimeMillis();
        }
        lastMillis = now;
        sequence = random.nextInt(SEQUENCE_MASK + 1);
        return now;
    }
}
