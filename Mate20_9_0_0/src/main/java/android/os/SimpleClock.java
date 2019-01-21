package android.os;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public abstract class SimpleClock extends Clock {
    private final ZoneId zone;

    public abstract long millis();

    public SimpleClock(ZoneId zone) {
        this.zone = zone;
    }

    public ZoneId getZone() {
        return this.zone;
    }

    public Clock withZone(ZoneId zone) {
        return new SimpleClock(zone) {
            public long millis() {
                return SimpleClock.this.millis();
            }
        };
    }

    public Instant instant() {
        return Instant.ofEpochMilli(millis());
    }
}
