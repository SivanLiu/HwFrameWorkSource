package java.time.chrono;

import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;

public enum HijrahEra implements Era {
    AH;

    public static HijrahEra of(int hijrahEra) {
        if (hijrahEra == 1) {
            return AH;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid era: ");
        stringBuilder.append(hijrahEra);
        throw new DateTimeException(stringBuilder.toString());
    }

    public int getValue() {
        return 1;
    }

    public ValueRange range(TemporalField field) {
        if (field == ChronoField.ERA) {
            return ValueRange.of(1, 1);
        }
        return super.range(field);
    }
}
