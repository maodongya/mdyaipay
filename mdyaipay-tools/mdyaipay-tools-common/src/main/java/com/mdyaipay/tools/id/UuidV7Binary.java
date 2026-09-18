package com.mdyaipay.tools.id;

import java.util.Objects;
import java.util.UUID;

/**
 * UUID 与 MySQL {@code BINARY(16)} 互转（RFC 9562 标准字节序：高位在前）。
 */
public final class UuidV7Binary {

    private UuidV7Binary() {
    }

    /**
     * @param uuid 非 null UUID
     * @return 16 字节，可直接写入 {@code BINARY(16)} 主键列
     */
    public static byte[] toBytes(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        byte[] out = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            out[i] = (byte) (msb >>> (8 * (7 - i)));
            out[8 + i] = (byte) (lsb >>> (8 * (7 - i)));
        }
        return out;
    }

    /**
     * @param bytes 长度必须为 16
     * @return 解析后的 UUID
     */
    public static UUID fromBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length != 16) {
            throw new IllegalArgumentException("uuid bytes length must be 16, got " + bytes.length);
        }
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xFFL);
            lsb = (lsb << 8) | (bytes[8 + i] & 0xFFL);
        }
        return new UUID(msb, lsb);
    }
}
