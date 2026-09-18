package com.mdyaipay.tools.id;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UuidV7Generator} 与 {@link UuidV7Binary} 行为测试。
 */
class UuidV7GeneratorTest {

    @Test
    void nextUuid_isVersion7AndVariantRfc4122() {
        UuidV7Generator generator = new UuidV7Generator();
        UUID uuid = generator.nextUuid();
        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());
    }

    @Test
    void binaryRoundTrip_preservesUuid() {
        UuidV7Generator generator = new UuidV7Generator();
        UUID original = generator.nextUuid();
        byte[] bytes = UuidV7Binary.toBytes(original);
        assertEquals(16, bytes.length);
        assertEquals(original, UuidV7Binary.fromBytes(bytes));
    }

    @Test
    void consecutiveUuids_areTimeOrderedByMsb() {
        UuidV7Generator generator = new UuidV7Generator();
        UUID first = generator.nextUuid();
        UUID second = generator.nextUuid();
        assertTrue(first.getMostSignificantBits() <= second.getMostSignificantBits());
    }

    @Test
    void nextBytes_matchesToBytesOfNextUuid() {
        UuidV7Generator generator = new UuidV7Generator();
        UUID uuid = generator.nextUuid();
        assertArrayEquals(UuidV7Binary.toBytes(uuid), UuidV7Binary.toBytes(uuid));
    }
}
