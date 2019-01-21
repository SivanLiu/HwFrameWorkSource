package java.time.temporal;

import java.time.DateTimeException;
import java.util.Objects;

public interface TemporalAccessor {
    long getLong(TemporalField temporalField);

    boolean isSupported(TemporalField temporalField);

    ValueRange range(TemporalField field) {
        if (!(field instanceof ChronoField)) {
            Objects.requireNonNull((Object) field, "field");
            return field.rangeRefinedBy(this);
        } else if (isSupported(field)) {
            return field.range();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported field: ");
            stringBuilder.append((Object) field);
            throw new UnsupportedTemporalTypeException(stringBuilder.toString());
        }
    }

    int get(TemporalField field) {
        Object range = range(field);
        if (range.isIntValue()) {
            long value = getLong(field);
            if (range.isValidValue(value)) {
                return (int) value;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid value for ");
            stringBuilder.append((Object) field);
            stringBuilder.append(" (valid values ");
            stringBuilder.append(range);
            stringBuilder.append("): ");
            stringBuilder.append(value);
            throw new DateTimeException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Invalid field ");
        stringBuilder2.append((Object) field);
        stringBuilder2.append(" for get() method, use getLong() instead");
        throw new UnsupportedTemporalTypeException(stringBuilder2.toString());
    }

    <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.zoneId() || query == TemporalQueries.chronology() || query == TemporalQueries.precision()) {
            return null;
        }
        return query.queryFrom(this);
    }
}
