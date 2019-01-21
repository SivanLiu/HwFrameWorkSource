package java.time.chrono;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Calendar;
import java.util.Objects;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.Era;
import sun.util.calendar.LocalGregorianCalendar.Date;

public final class JapaneseDate extends ChronoLocalDateImpl<JapaneseDate> implements ChronoLocalDate, Serializable {
    static final LocalDate MEIJI_6_ISODATE = LocalDate.of(1873, 1, 1);
    private static final long serialVersionUID = -305327627230580483L;
    private transient JapaneseEra era;
    private final transient LocalDate isoDate;
    private transient int yearOfEra;

    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    public /* bridge */ /* synthetic */ long until(Temporal temporal, TemporalUnit temporalUnit) {
        return super.until(temporal, temporalUnit);
    }

    public static JapaneseDate now() {
        return now(Clock.systemDefaultZone());
    }

    public static JapaneseDate now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static JapaneseDate now(Clock clock) {
        return new JapaneseDate(LocalDate.now(clock));
    }

    public static JapaneseDate of(JapaneseEra era, int yearOfEra, int month, int dayOfMonth) {
        Objects.requireNonNull((Object) era, "era");
        Date jdate = JapaneseChronology.JCAL.newCalendarDate(null);
        jdate.setEra(era.getPrivateEra()).setDate(yearOfEra, month, dayOfMonth);
        if (JapaneseChronology.JCAL.validate(jdate)) {
            return new JapaneseDate(era, yearOfEra, LocalDate.of(jdate.getNormalizedYear(), month, dayOfMonth));
        }
        throw new DateTimeException("year, month, and day not valid for Era");
    }

    public static JapaneseDate of(int prolepticYear, int month, int dayOfMonth) {
        return new JapaneseDate(LocalDate.of(prolepticYear, month, dayOfMonth));
    }

    static JapaneseDate ofYearDay(JapaneseEra era, int yearOfEra, int dayOfYear) {
        Objects.requireNonNull((Object) era, "era");
        CalendarDate firstDay = era.getPrivateEra().getSinceDate();
        Date jdate = JapaneseChronology.JCAL.newCalendarDate(null);
        jdate.setEra(era.getPrivateEra());
        if (yearOfEra == 1) {
            jdate.setDate(yearOfEra, firstDay.getMonth(), (firstDay.getDayOfMonth() + dayOfYear) - 1);
        } else {
            jdate.setDate(yearOfEra, 1, dayOfYear);
        }
        JapaneseChronology.JCAL.normalize(jdate);
        if (era.getPrivateEra() == jdate.getEra() && yearOfEra == jdate.getYear()) {
            return new JapaneseDate(era, yearOfEra, LocalDate.of(jdate.getNormalizedYear(), jdate.getMonth(), jdate.getDayOfMonth()));
        }
        throw new DateTimeException("Invalid parameters");
    }

    public static JapaneseDate from(TemporalAccessor temporal) {
        return JapaneseChronology.INSTANCE.date(temporal);
    }

    JapaneseDate(LocalDate isoDate) {
        if (isoDate.isBefore(MEIJI_6_ISODATE)) {
            throw new DateTimeException("JapaneseDate before Meiji 6 is not supported");
        }
        Date jdate = toPrivateJapaneseDate(isoDate);
        this.era = JapaneseEra.toJapaneseEra(jdate.getEra());
        this.yearOfEra = jdate.getYear();
        this.isoDate = isoDate;
    }

    JapaneseDate(JapaneseEra era, int year, LocalDate isoDate) {
        if (isoDate.isBefore(MEIJI_6_ISODATE)) {
            throw new DateTimeException("JapaneseDate before Meiji 6 is not supported");
        }
        this.era = era;
        this.yearOfEra = year;
        this.isoDate = isoDate;
    }

    public JapaneseChronology getChronology() {
        return JapaneseChronology.INSTANCE;
    }

    public JapaneseEra getEra() {
        return this.era;
    }

    public int lengthOfMonth() {
        return this.isoDate.lengthOfMonth();
    }

    public int lengthOfYear() {
        Calendar jcal = JapaneseChronology.createCalendar();
        jcal.set(0, this.era.getValue() + 2);
        jcal.set(this.yearOfEra, this.isoDate.getMonthValue() - 1, this.isoDate.getDayOfMonth());
        return jcal.getActualMaximum(6);
    }

    public boolean isSupported(TemporalField field) {
        if (field == ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH || field == ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR || field == ChronoField.ALIGNED_WEEK_OF_MONTH || field == ChronoField.ALIGNED_WEEK_OF_YEAR) {
            return false;
        }
        return super.isSupported(field);
    }

    public ValueRange range(TemporalField field) {
        if (!(field instanceof ChronoField)) {
            return field.rangeRefinedBy(this);
        }
        if (isSupported(field)) {
            ChronoField f = (ChronoField) field;
            switch (f) {
                case DAY_OF_MONTH:
                    return ValueRange.of(1, (long) lengthOfMonth());
                case DAY_OF_YEAR:
                    return ValueRange.of(1, (long) lengthOfYear());
                case YEAR_OF_ERA:
                    Calendar jcal = JapaneseChronology.createCalendar();
                    jcal.set(0, this.era.getValue() + 2);
                    jcal.set(this.yearOfEra, this.isoDate.getMonthValue() - 1, this.isoDate.getDayOfMonth());
                    return ValueRange.of(1, (long) jcal.getActualMaximum(1));
                default:
                    return getChronology().range(f);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported field: ");
        stringBuilder.append((Object) field);
        throw new UnsupportedTemporalTypeException(stringBuilder.toString());
    }

    public long getLong(TemporalField field) {
        if (!(field instanceof ChronoField)) {
            return field.getFrom(this);
        }
        switch ((ChronoField) field) {
            case DAY_OF_YEAR:
                Calendar jcal = JapaneseChronology.createCalendar();
                jcal.set(0, this.era.getValue() + 2);
                jcal.set(this.yearOfEra, this.isoDate.getMonthValue() - 1, this.isoDate.getDayOfMonth());
                return (long) jcal.get(6);
            case YEAR_OF_ERA:
                return (long) this.yearOfEra;
            case ALIGNED_DAY_OF_WEEK_IN_MONTH:
            case ALIGNED_DAY_OF_WEEK_IN_YEAR:
            case ALIGNED_WEEK_OF_MONTH:
            case ALIGNED_WEEK_OF_YEAR:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported field: ");
                stringBuilder.append((Object) field);
                throw new UnsupportedTemporalTypeException(stringBuilder.toString());
            case ERA:
                return (long) this.era.getValue();
            default:
                return this.isoDate.getLong(field);
        }
    }

    private static Date toPrivateJapaneseDate(LocalDate isoDate) {
        Date jdate = JapaneseChronology.JCAL.newCalendarDate(null);
        Era sunEra = JapaneseEra.privateEraFrom(isoDate);
        int year = isoDate.getYear();
        if (sunEra != null) {
            year -= sunEra.getSinceDate().getYear() - 1;
        }
        jdate.setEra(sunEra).setYear(year).setMonth(isoDate.getMonthValue()).setDayOfMonth(isoDate.getDayOfMonth());
        JapaneseChronology.JCAL.normalize(jdate);
        return jdate;
    }

    public JapaneseDate with(TemporalField field, long newValue) {
        if (!(field instanceof ChronoField)) {
            return (JapaneseDate) super.with(field, newValue);
        }
        ChronoField f = (ChronoField) field;
        if (getLong(f) == newValue) {
            return this;
        }
        int i = AnonymousClass1.$SwitchMap$java$time$temporal$ChronoField[f.ordinal()];
        if (i != 3) {
            switch (i) {
                case 8:
                case 9:
                    break;
            }
        }
        i = getChronology().range(f).checkValidIntValue(newValue, f);
        int i2 = AnonymousClass1.$SwitchMap$java$time$temporal$ChronoField[f.ordinal()];
        if (i2 == 3) {
            return withYear(i);
        }
        switch (i2) {
            case 8:
                return withYear(JapaneseEra.of(i), this.yearOfEra);
            case 9:
                return with(this.isoDate.withYear(i));
        }
        return with(this.isoDate.with(field, newValue));
    }

    public JapaneseDate with(TemporalAdjuster adjuster) {
        return (JapaneseDate) super.with(adjuster);
    }

    public JapaneseDate plus(TemporalAmount amount) {
        return (JapaneseDate) super.plus(amount);
    }

    public JapaneseDate minus(TemporalAmount amount) {
        return (JapaneseDate) super.minus(amount);
    }

    private JapaneseDate withYear(JapaneseEra era, int yearOfEra) {
        return with(this.isoDate.withYear(JapaneseChronology.INSTANCE.prolepticYear(era, yearOfEra)));
    }

    private JapaneseDate withYear(int year) {
        return withYear(getEra(), year);
    }

    JapaneseDate plusYears(long years) {
        return with(this.isoDate.plusYears(years));
    }

    JapaneseDate plusMonths(long months) {
        return with(this.isoDate.plusMonths(months));
    }

    JapaneseDate plusWeeks(long weeksToAdd) {
        return with(this.isoDate.plusWeeks(weeksToAdd));
    }

    JapaneseDate plusDays(long days) {
        return with(this.isoDate.plusDays(days));
    }

    public JapaneseDate plus(long amountToAdd, TemporalUnit unit) {
        return (JapaneseDate) super.plus(amountToAdd, unit);
    }

    public JapaneseDate minus(long amountToAdd, TemporalUnit unit) {
        return (JapaneseDate) super.minus(amountToAdd, unit);
    }

    JapaneseDate minusYears(long yearsToSubtract) {
        return (JapaneseDate) super.minusYears(yearsToSubtract);
    }

    JapaneseDate minusMonths(long monthsToSubtract) {
        return (JapaneseDate) super.minusMonths(monthsToSubtract);
    }

    JapaneseDate minusWeeks(long weeksToSubtract) {
        return (JapaneseDate) super.minusWeeks(weeksToSubtract);
    }

    JapaneseDate minusDays(long daysToSubtract) {
        return (JapaneseDate) super.minusDays(daysToSubtract);
    }

    private JapaneseDate with(LocalDate newDate) {
        return newDate.equals(this.isoDate) ? this : new JapaneseDate(newDate);
    }

    public final ChronoLocalDateTime<JapaneseDate> atTime(LocalTime localTime) {
        return super.atTime(localTime);
    }

    public ChronoPeriod until(ChronoLocalDate endDate) {
        Period period = this.isoDate.until(endDate);
        return getChronology().period(period.getYears(), period.getMonths(), period.getDays());
    }

    public long toEpochDay() {
        return this.isoDate.toEpochDay();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof JapaneseDate)) {
            return false;
        }
        return this.isoDate.equals(((JapaneseDate) obj).isoDate);
    }

    public int hashCode() {
        return getChronology().getId().hashCode() ^ this.isoDate.hashCode();
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    private Object writeReplace() {
        return new Ser((byte) 4, this);
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeInt(get(ChronoField.YEAR));
        out.writeByte(get(ChronoField.MONTH_OF_YEAR));
        out.writeByte(get(ChronoField.DAY_OF_MONTH));
    }

    static JapaneseDate readExternal(DataInput in) throws IOException {
        return JapaneseChronology.INSTANCE.date(in.readInt(), in.readByte(), in.readByte());
    }
}
