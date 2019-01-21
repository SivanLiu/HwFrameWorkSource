package java.time;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Objects;

public final class OffsetTime implements Temporal, TemporalAdjuster, Comparable<OffsetTime>, Serializable {
    public static final OffsetTime MAX = LocalTime.MAX.atOffset(ZoneOffset.MIN);
    public static final OffsetTime MIN = LocalTime.MIN.atOffset(ZoneOffset.MAX);
    private static final long serialVersionUID = 7264499704384272492L;
    private final ZoneOffset offset;
    private final LocalTime time;

    public static OffsetTime now() {
        return now(Clock.systemDefaultZone());
    }

    public static OffsetTime now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static OffsetTime now(Clock clock) {
        Objects.requireNonNull((Object) clock, "clock");
        Instant now = clock.instant();
        return ofInstant(now, clock.getZone().getRules().getOffset(now));
    }

    public static OffsetTime of(LocalTime time, ZoneOffset offset) {
        return new OffsetTime(time, offset);
    }

    public static OffsetTime of(int hour, int minute, int second, int nanoOfSecond, ZoneOffset offset) {
        return new OffsetTime(LocalTime.of(hour, minute, second, nanoOfSecond), offset);
    }

    public static OffsetTime ofInstant(Instant instant, ZoneId zone) {
        Objects.requireNonNull((Object) instant, "instant");
        Objects.requireNonNull((Object) zone, "zone");
        ZoneOffset offset = zone.getRules().getOffset(instant);
        return new OffsetTime(LocalTime.ofNanoOfDay((((long) ((int) Math.floorMod(instant.getEpochSecond() + ((long) offset.getTotalSeconds()), 86400))) * 1000000000) + ((long) instant.getNano())), offset);
    }

    public static OffsetTime from(TemporalAccessor temporal) {
        if (temporal instanceof OffsetTime) {
            return (OffsetTime) temporal;
        }
        try {
            return new OffsetTime(LocalTime.from(temporal), ZoneOffset.from(temporal));
        } catch (DateTimeException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to obtain OffsetTime from TemporalAccessor: ");
            stringBuilder.append((Object) temporal);
            stringBuilder.append(" of type ");
            stringBuilder.append(temporal.getClass().getName());
            throw new DateTimeException(stringBuilder.toString(), ex);
        }
    }

    public static OffsetTime parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_OFFSET_TIME);
    }

    public static OffsetTime parse(CharSequence text, DateTimeFormatter formatter) {
        Objects.requireNonNull((Object) formatter, "formatter");
        return (OffsetTime) formatter.parse(text, -$$Lambda$I08rBDhAPdxOG_R3AeLRKYX7Z-o.INSTANCE);
    }

    private OffsetTime(LocalTime time, ZoneOffset offset) {
        this.time = (LocalTime) Objects.requireNonNull((Object) time, "time");
        this.offset = (ZoneOffset) Objects.requireNonNull((Object) offset, "offset");
    }

    private OffsetTime with(LocalTime time, ZoneOffset offset) {
        if (this.time == time && this.offset.equals(offset)) {
            return this;
        }
        return new OffsetTime(time, offset);
    }

    public boolean isSupported(TemporalField field) {
        boolean z = true;
        if (field instanceof ChronoField) {
            if (!(field.isTimeBased() || field == ChronoField.OFFSET_SECONDS)) {
                z = false;
            }
            return z;
        }
        if (field == null || !field.isSupportedBy(this)) {
            z = false;
        }
        return z;
    }

    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isTimeBased();
        }
        boolean z = unit != null && unit.isSupportedBy(this);
        return z;
    }

    public ValueRange range(TemporalField field) {
        if (!(field instanceof ChronoField)) {
            return field.rangeRefinedBy(this);
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return field.range();
        }
        return this.time.range(field);
    }

    public int get(TemporalField field) {
        return super.get(field);
    }

    public long getLong(TemporalField field) {
        if (!(field instanceof ChronoField)) {
            return field.getFrom(this);
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return (long) this.offset.getTotalSeconds();
        }
        return this.time.getLong(field);
    }

    public ZoneOffset getOffset() {
        return this.offset;
    }

    public OffsetTime withOffsetSameLocal(ZoneOffset offset) {
        return (offset == null || !offset.equals(this.offset)) ? new OffsetTime(this.time, offset) : this;
    }

    public OffsetTime withOffsetSameInstant(ZoneOffset offset) {
        if (offset.equals(this.offset)) {
            return this;
        }
        return new OffsetTime(this.time.plusSeconds((long) (offset.getTotalSeconds() - this.offset.getTotalSeconds())), offset);
    }

    public LocalTime toLocalTime() {
        return this.time;
    }

    public int getHour() {
        return this.time.getHour();
    }

    public int getMinute() {
        return this.time.getMinute();
    }

    public int getSecond() {
        return this.time.getSecond();
    }

    public int getNano() {
        return this.time.getNano();
    }

    public OffsetTime with(TemporalAdjuster adjuster) {
        if (adjuster instanceof LocalTime) {
            return with((LocalTime) adjuster, this.offset);
        }
        if (adjuster instanceof ZoneOffset) {
            return with(this.time, (ZoneOffset) adjuster);
        }
        if (adjuster instanceof OffsetTime) {
            return (OffsetTime) adjuster;
        }
        return (OffsetTime) adjuster.adjustInto(this);
    }

    public OffsetTime with(TemporalField field, long newValue) {
        if (!(field instanceof ChronoField)) {
            return (OffsetTime) field.adjustInto(this, newValue);
        }
        if (field != ChronoField.OFFSET_SECONDS) {
            return with(this.time.with(field, newValue), this.offset);
        }
        return with(this.time, ZoneOffset.ofTotalSeconds(((ChronoField) field).checkValidIntValue(newValue)));
    }

    public OffsetTime withHour(int hour) {
        return with(this.time.withHour(hour), this.offset);
    }

    public OffsetTime withMinute(int minute) {
        return with(this.time.withMinute(minute), this.offset);
    }

    public OffsetTime withSecond(int second) {
        return with(this.time.withSecond(second), this.offset);
    }

    public OffsetTime withNano(int nanoOfSecond) {
        return with(this.time.withNano(nanoOfSecond), this.offset);
    }

    public OffsetTime truncatedTo(TemporalUnit unit) {
        return with(this.time.truncatedTo(unit), this.offset);
    }

    public OffsetTime plus(TemporalAmount amountToAdd) {
        return (OffsetTime) amountToAdd.addTo(this);
    }

    public OffsetTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return with(this.time.plus(amountToAdd, unit), this.offset);
        }
        return (OffsetTime) unit.addTo(this, amountToAdd);
    }

    public OffsetTime plusHours(long hours) {
        return with(this.time.plusHours(hours), this.offset);
    }

    public OffsetTime plusMinutes(long minutes) {
        return with(this.time.plusMinutes(minutes), this.offset);
    }

    public OffsetTime plusSeconds(long seconds) {
        return with(this.time.plusSeconds(seconds), this.offset);
    }

    public OffsetTime plusNanos(long nanos) {
        return with(this.time.plusNanos(nanos), this.offset);
    }

    public OffsetTime minus(TemporalAmount amountToSubtract) {
        return (OffsetTime) amountToSubtract.subtractFrom(this);
    }

    public OffsetTime minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE ? plus((long) Long.MAX_VALUE, unit).plus(1, unit) : plus(-amountToSubtract, unit);
    }

    public OffsetTime minusHours(long hours) {
        return with(this.time.minusHours(hours), this.offset);
    }

    public OffsetTime minusMinutes(long minutes) {
        return with(this.time.minusMinutes(minutes), this.offset);
    }

    public OffsetTime minusSeconds(long seconds) {
        return with(this.time.minusSeconds(seconds), this.offset);
    }

    public OffsetTime minusNanos(long nanos) {
        return with(this.time.minusNanos(nanos), this.offset);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.offset() || query == TemporalQueries.zone()) {
            return this.offset;
        }
        int i = 0;
        int i2 = query == TemporalQueries.zoneId() ? 1 : 0;
        if (query == TemporalQueries.chronology()) {
            i = 1;
        }
        if ((i2 | i) != 0 || query == TemporalQueries.localDate()) {
            return null;
        }
        if (query == TemporalQueries.localTime()) {
            return this.time;
        }
        if (query == TemporalQueries.precision()) {
            return ChronoUnit.NANOS;
        }
        return query.queryFrom(this);
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.NANO_OF_DAY, this.time.toNanoOfDay()).with(ChronoField.OFFSET_SECONDS, (long) this.offset.getTotalSeconds());
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        OffsetTime end = from(endExclusive);
        if (!(unit instanceof ChronoUnit)) {
            return unit.between(this, end);
        }
        long nanosUntil = end.toEpochNano() - toEpochNano();
        switch ((ChronoUnit) unit) {
            case NANOS:
                return nanosUntil;
            case MICROS:
                return nanosUntil / 1000;
            case MILLIS:
                return nanosUntil / 1000000;
            case SECONDS:
                return nanosUntil / 1000000000;
            case MINUTES:
                return nanosUntil / 60000000000L;
            case HOURS:
                return nanosUntil / 3600000000000L;
            case HALF_DAYS:
                return nanosUntil / 43200000000000L;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported unit: ");
                stringBuilder.append((Object) unit);
                throw new UnsupportedTemporalTypeException(stringBuilder.toString());
        }
    }

    public String format(DateTimeFormatter formatter) {
        Objects.requireNonNull((Object) formatter, "formatter");
        return formatter.format(this);
    }

    public OffsetDateTime atDate(LocalDate date) {
        return OffsetDateTime.of(date, this.time, this.offset);
    }

    private long toEpochNano() {
        return this.time.toNanoOfDay() - (((long) this.offset.getTotalSeconds()) * 1000000000);
    }

    public int compareTo(OffsetTime other) {
        if (this.offset.equals(other.offset)) {
            return this.time.compareTo(other.time);
        }
        int compare = Long.compare(toEpochNano(), other.toEpochNano());
        if (compare == 0) {
            compare = this.time.compareTo(other.time);
        }
        return compare;
    }

    public boolean isAfter(OffsetTime other) {
        return toEpochNano() > other.toEpochNano();
    }

    public boolean isBefore(OffsetTime other) {
        return toEpochNano() < other.toEpochNano();
    }

    public boolean isEqual(OffsetTime other) {
        return toEpochNano() == other.toEpochNano();
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OffsetTime)) {
            return false;
        }
        OffsetTime other = (OffsetTime) obj;
        if (!(this.time.equals(other.time) && this.offset.equals(other.offset))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return this.time.hashCode() ^ this.offset.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.time.toString());
        stringBuilder.append(this.offset.toString());
        return stringBuilder.toString();
    }

    private Object writeReplace() {
        return new Ser((byte) 9, this);
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(ObjectOutput out) throws IOException {
        this.time.writeExternal(out);
        this.offset.writeExternal(out);
    }

    static OffsetTime readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        return of(LocalTime.readExternal(in), ZoneOffset.readExternal(in));
    }
}
