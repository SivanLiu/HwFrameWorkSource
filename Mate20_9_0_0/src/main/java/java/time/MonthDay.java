package java.time;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Objects;
import sun.util.locale.LanguageTag;

public final class MonthDay implements TemporalAccessor, TemporalAdjuster, Comparable<MonthDay>, Serializable {
    private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder().appendLiteral("--").appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter();
    private static final long serialVersionUID = -939150713474957432L;
    private final int day;
    private final int month;

    public static MonthDay now() {
        return now(Clock.systemDefaultZone());
    }

    public static MonthDay now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static MonthDay now(Clock clock) {
        LocalDate now = LocalDate.now(clock);
        return of(now.getMonth(), now.getDayOfMonth());
    }

    public static MonthDay of(Month month, int dayOfMonth) {
        Objects.requireNonNull((Object) month, "month");
        ChronoField.DAY_OF_MONTH.checkValidValue((long) dayOfMonth);
        if (dayOfMonth <= month.maxLength()) {
            return new MonthDay(month.getValue(), dayOfMonth);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Illegal value for DayOfMonth field, value ");
        stringBuilder.append(dayOfMonth);
        stringBuilder.append(" is not valid for month ");
        stringBuilder.append(month.name());
        throw new DateTimeException(stringBuilder.toString());
    }

    public static MonthDay of(int month, int dayOfMonth) {
        return of(Month.of(month), dayOfMonth);
    }

    public static MonthDay from(TemporalAccessor temporal) {
        if (temporal instanceof MonthDay) {
            return (MonthDay) temporal;
        }
        try {
            if (!IsoChronology.INSTANCE.equals(Chronology.from(temporal))) {
                temporal = LocalDate.from(temporal);
            }
            return of(temporal.get(ChronoField.MONTH_OF_YEAR), temporal.get(ChronoField.DAY_OF_MONTH));
        } catch (DateTimeException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to obtain MonthDay from TemporalAccessor: ");
            stringBuilder.append((Object) temporal);
            stringBuilder.append(" of type ");
            stringBuilder.append(temporal.getClass().getName());
            throw new DateTimeException(stringBuilder.toString(), ex);
        }
    }

    public static MonthDay parse(CharSequence text) {
        return parse(text, PARSER);
    }

    public static MonthDay parse(CharSequence text, DateTimeFormatter formatter) {
        Objects.requireNonNull((Object) formatter, "formatter");
        return (MonthDay) formatter.parse(text, -$$Lambda$sL_1zXqh7GXCv2G9X40ozp_OBMA.INSTANCE);
    }

    private MonthDay(int month, int dayOfMonth) {
        this.month = month;
        this.day = dayOfMonth;
    }

    public boolean isSupported(TemporalField field) {
        boolean z = true;
        if (field instanceof ChronoField) {
            if (!(field == ChronoField.MONTH_OF_YEAR || field == ChronoField.DAY_OF_MONTH)) {
                z = false;
            }
            return z;
        }
        if (field == null || !field.isSupportedBy(this)) {
            z = false;
        }
        return z;
    }

    public ValueRange range(TemporalField field) {
        if (field == ChronoField.MONTH_OF_YEAR) {
            return field.range();
        }
        if (field == ChronoField.DAY_OF_MONTH) {
            return ValueRange.of(1, (long) getMonth().minLength(), (long) getMonth().maxLength());
        }
        return super.range(field);
    }

    public int get(TemporalField field) {
        return range(field).checkValidIntValue(getLong(field), field);
    }

    public long getLong(TemporalField field) {
        if (!(field instanceof ChronoField)) {
            return field.getFrom(this);
        }
        switch ((ChronoField) field) {
            case DAY_OF_MONTH:
                return (long) this.day;
            case MONTH_OF_YEAR:
                return (long) this.month;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported field: ");
                stringBuilder.append((Object) field);
                throw new UnsupportedTemporalTypeException(stringBuilder.toString());
        }
    }

    public int getMonthValue() {
        return this.month;
    }

    public Month getMonth() {
        return Month.of(this.month);
    }

    public int getDayOfMonth() {
        return this.day;
    }

    public boolean isValidYear(int year) {
        int i = (this.day == 29 && this.month == 2 && !Year.isLeap((long) year)) ? 1 : 0;
        return i ^ 1;
    }

    public MonthDay withMonth(int month) {
        return with(Month.of(month));
    }

    public MonthDay with(Month month) {
        Objects.requireNonNull((Object) month, "month");
        if (month.getValue() == this.month) {
            return this;
        }
        return new MonthDay(month.getValue(), Math.min(this.day, month.maxLength()));
    }

    public MonthDay withDayOfMonth(int dayOfMonth) {
        if (dayOfMonth == this.day) {
            return this;
        }
        return of(this.month, dayOfMonth);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.chronology()) {
            return IsoChronology.INSTANCE;
        }
        return super.query(query);
    }

    public Temporal adjustInto(Temporal temporal) {
        if (Chronology.from(temporal).equals(IsoChronology.INSTANCE)) {
            temporal = temporal.with(ChronoField.MONTH_OF_YEAR, (long) this.month);
            return temporal.with(ChronoField.DAY_OF_MONTH, Math.min(temporal.range(ChronoField.DAY_OF_MONTH).getMaximum(), (long) this.day));
        }
        throw new DateTimeException("Adjustment only supported on ISO date-time");
    }

    public String format(DateTimeFormatter formatter) {
        Objects.requireNonNull((Object) formatter, "formatter");
        return formatter.format(this);
    }

    public LocalDate atYear(int year) {
        return LocalDate.of(year, this.month, isValidYear(year) ? this.day : 28);
    }

    public int compareTo(MonthDay other) {
        int cmp = this.month - other.month;
        if (cmp == 0) {
            return this.day - other.day;
        }
        return cmp;
    }

    public boolean isAfter(MonthDay other) {
        return compareTo(other) > 0;
    }

    public boolean isBefore(MonthDay other) {
        return compareTo(other) < 0;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MonthDay)) {
            return false;
        }
        MonthDay other = (MonthDay) obj;
        if (!(this.month == other.month && this.day == other.day)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (this.month << 6) + this.day;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(10);
        stringBuilder.append("--");
        stringBuilder.append(this.month < 10 ? "0" : "");
        stringBuilder.append(this.month);
        stringBuilder.append(this.day < 10 ? "-0" : LanguageTag.SEP);
        stringBuilder.append(this.day);
        return stringBuilder.toString();
    }

    private Object writeReplace() {
        return new Ser((byte) 13, this);
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeByte(this.month);
        out.writeByte(this.day);
    }

    static MonthDay readExternal(DataInput in) throws IOException {
        return of(in.readByte(), in.readByte());
    }
}
