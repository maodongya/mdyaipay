package com.mdyaipay.tools.ratelimit.memory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * 测试用可变时钟：仅推进 instant，zone 固定 UTC。
 */
final class MutableClock extends Clock {

    private Instant instant;

    /**
     * @param instant 初始时刻，非 null
     */
    MutableClock(Instant instant) {
        this.instant = Objects.requireNonNull(instant, "instant");
    }

    /**
     * 向前推进指定时长。
     *
     * @param duration 推进量，非 null
     */
    void advance(Duration duration) {
        instant = instant.plus(Objects.requireNonNull(duration, "duration"));
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
