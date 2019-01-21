package java.time.chrono;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class ChronoPeriodImpl implements ChronoPeriod, Serializable {
    private static final List<TemporalUnit> SUPPORTED_UNITS = Collections.unmodifiableList(Arrays.asList(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS));
    private static final long serialVersionUID = 57387258289L;
    private final Chronology chrono;
    final int days;
    final int months;
    final int years;

    ChronoPeriodImpl(Chronology chrono, int years, int months, int days) {
        Objects.requireNonNull((Object) chrono, "chrono");
        this.chrono = chrono;
        this.years = years;
        this.months = months;
        this.days = days;
    }

    public long get(TemporalUnit unit) {
        if (unit == ChronoUnit.YEARS) {
            return (long) this.years;
        }
        if (unit == ChronoUnit.MONTHS) {
            return (long) this.months;
        }
        if (unit == ChronoUnit.DAYS) {
            return (long) this.days;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported unit: ");
        stringBuilder.append((Object) unit);
        throw new UnsupportedTemporalTypeException(stringBuilder.toString());
    }

    public List<TemporalUnit> getUnits() {
        return SUPPORTED_UNITS;
    }

    public Chronology getChronology() {
        return this.chrono;
    }

    public boolean isZero() {
        return this.years == 0 && this.months == 0 && this.days == 0;
    }

    public boolean isNegative() {
        return this.years < 0 || this.months < 0 || this.days < 0;
    }

    public ChronoPeriod plus(TemporalAmount amountToAdd) {
        ChronoPeriodImpl amount = validateAmount(amountToAdd);
        return new ChronoPeriodImpl(this.chrono, Math.addExact(this.years, amount.years), Math.addExact(this.months, amount.months), Math.addExact(this.days, amount.days));
    }

    public ChronoPeriod minus(TemporalAmount amountToSubtract) {
        ChronoPeriodImpl amount = validateAmount(amountToSubtract);
        return new ChronoPeriodImpl(this.chrono, Math.subtractExact(this.years, amount.years), Math.subtractExact(this.months, amount.months), Math.subtractExact(this.days, amount.days));
    }

    private ChronoPeriodImpl validateAmount(TemporalAmount amount) {
        Objects.requireNonNull((Object) amount, "amount");
        if (amount instanceof ChronoPeriodImpl) {
            ChronoPeriodImpl period = (ChronoPeriodImpl) amount;
            if (this.chrono.equals(period.getChronology())) {
                return period;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Chronology mismatch, expected: ");
            stringBuilder.append(this.chrono.getId());
            stringBuilder.append(", actual: ");
            stringBuilder.append(period.getChronology().getId());
            throw new ClassCastException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unable to obtain ChronoPeriod from TemporalAmount: ");
        stringBuilder2.append(amount.getClass());
        throw new DateTimeException(stringBuilder2.toString());
    }

    public ChronoPeriod multipliedBy(int scalar) {
        if (isZero() || scalar == 1) {
            return this;
        }
        return new ChronoPeriodImpl(this.chrono, Math.multiplyExact(this.years, scalar), Math.multiplyExact(this.months, scalar), Math.multiplyExact(this.days, scalar));
    }

    public ChronoPeriod normalized() {
        long monthRange = monthRange();
        if (monthRange <= 0) {
            return this;
        }
        long totalMonths = (((long) this.years) * monthRange) + ((long) this.months);
        long splitYears = totalMonths / monthRange;
        int splitMonths = (int) (totalMonths % monthRange);
        if (splitYears == ((long) this.years) && splitMonths == this.months) {
            return this;
        }
        return new ChronoPeriodImpl(this.chrono, Math.toIntExact(splitYears), splitMonths, this.days);
    }

    private long monthRange() {
        ValueRange startRange = this.chrono.range(ChronoField.MONTH_OF_YEAR);
        if (startRange.isFixed() && startRange.isIntValue()) {
            return (startRange.getMaximum() - startRange.getMinimum()) + 1;
        }
        return -1;
    }

    public Temporal addTo(Temporal temporal) {
        validateChrono(temporal);
        if (this.months != 0) {
            long monthRange = monthRange();
            if (monthRange > 0) {
                temporal = temporal.plus((((long) this.years) * monthRange) + ((long) this.months), ChronoUnit.MONTHS);
            } else {
                if (this.years != 0) {
                    temporal = temporal.plus((long) this.years, ChronoUnit.YEARS);
                }
                temporal = temporal.plus((long) this.months, ChronoUnit.MONTHS);
            }
        } else if (this.years != 0) {
            temporal = temporal.plus((long) this.years, ChronoUnit.YEARS);
        }
        if (this.days != 0) {
            return temporal.plus((long) this.days, ChronoUnit.DAYS);
        }
        return temporal;
    }

    public Temporal subtractFrom(Temporal temporal) {
        validateChrono(temporal);
        if (this.months != 0) {
            long monthRange = monthRange();
            if (monthRange > 0) {
                temporal = temporal.minus((((long) this.years) * monthRange) + ((long) this.months), ChronoUnit.MONTHS);
            } else {
                if (this.years != 0) {
                    temporal = temporal.minus((long) this.years, ChronoUnit.YEARS);
                }
                temporal = temporal.minus((long) this.months, ChronoUnit.MONTHS);
            }
        } else if (this.years != 0) {
            temporal = temporal.minus((long) this.years, ChronoUnit.YEARS);
        }
        if (this.days != 0) {
            return temporal.minus((long) this.days, ChronoUnit.DAYS);
        }
        return temporal;
    }

    private void validateChrono(TemporalAccessor temporal) {
        Objects.requireNonNull((Object) temporal, "temporal");
        Chronology temporalChrono = (Chronology) temporal.query(TemporalQueries.chronology());
        if (temporalChrono != null && !this.chrono.equals(temporalChrono)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Chronology mismatch, expected: ");
            stringBuilder.append(this.chrono.getId());
            stringBuilder.append(", actual: ");
            stringBuilder.append(temporalChrono.getId());
            throw new DateTimeException(stringBuilder.toString());
        }
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChronoPeriodImpl)) {
            return false;
        }
        ChronoPeriodImpl other = (ChronoPeriodImpl) obj;
        if (!(this.years == other.years && this.months == other.months && this.days == other.days && this.chrono.equals(other.chrono))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return ((this.years + Integer.rotateLeft(this.months, 8)) + Integer.rotateLeft(this.days, 16)) ^ this.chrono.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder;
        if (isZero()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(getChronology().toString());
            stringBuilder.append(" P0D");
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(getChronology().toString());
        stringBuilder.append(' ');
        stringBuilder.append('P');
        if (this.years != 0) {
            stringBuilder.append(this.years);
            stringBuilder.append('Y');
        }
        if (this.months != 0) {
            stringBuilder.append(this.months);
            stringBuilder.append('M');
        }
        if (this.days != 0) {
            stringBuilder.append(this.days);
            stringBuilder.append('D');
        }
        return stringBuilder.toString();
    }

    protected Object writeReplace() {
        return new Ser((byte) 9, this);
    }

    private void readObject(ObjectInputStream s) throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeUTF(this.chrono.getId());
        out.writeInt(this.years);
        out.writeInt(this.months);
        out.writeInt(this.days);
    }

    static ChronoPeriodImpl readExternal(DataInput in) throws IOException {
        return new ChronoPeriodImpl(Chronology.of(in.readUTF()), in.readInt(), in.readInt(), in.readInt());
    }
}
