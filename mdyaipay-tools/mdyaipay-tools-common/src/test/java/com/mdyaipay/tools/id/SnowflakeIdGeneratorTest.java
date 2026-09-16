package com.mdyaipay.tools.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTest {

    @Test
    void generatesUniqueIds() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertTrue(ids.add(generator.nextId()));
        }
        assertEquals(1000, ids.size());
    }

    @Test
    void idsAreMonotonicallyIncreasing() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(2, 3);
        long previous = generator.nextId();
        for (int i = 0; i < 100; i++) {
            long current = generator.nextId();
            assertTrue(current > previous);
            previous = current;
        }
    }
}
