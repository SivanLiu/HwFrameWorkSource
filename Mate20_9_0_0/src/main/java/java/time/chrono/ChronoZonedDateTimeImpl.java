package java.time.chrono;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.List;
import java.util.Objects;

final class ChronoZonedDateTimeImpl<D extends ChronoLocalDate> implements ChronoZonedDateTime<D>, Serializable {
    private static final long serialVersionUID = -5261813987200935591L;
    private final transient ChronoLocalDateTimeImpl<D> dateTime;
    private final transient ZoneOffset offset;
    private final transient ZoneId zone;

    static <R extends ChronoLocalDate> ChronoZonedDateTime<R> ofBest(ChronoLocalDateTimeImpl<R> localDateTime, ZoneId zone, ZoneOffset preferredOffset) {
        Objects.requireNonNull((Object) localDateTime, "localDateTime");
        Objects.requireNonNull((Object) zone, "zone");
        if (zone instanceof ZoneOffset) {
            return new ChronoZonedDateTimeImpl(localDateTime, (ZoneOffset) zone, zone);
        }
        Object offset;
        ZoneRules rules = zone.getRules();
        LocalDateTime isoLDT = LocalDateTime.from(localDateTime);
        List<ZoneOffset> validOffsets = rules.getValidOffsets(isoLDT);
        if (validOffsets.size() == 1) {
            offset = (ZoneOffset) validOffsets.get(0);
        } else if (validOffsets.size() == 0) {
            ZoneOffset offset2 = rules.getTransition(isoLDT);
            localDateTime = localDateTime.plusSeconds(offset2.getDuration().getSeconds());
            offset = offset2.getOffsetAfter();
        } else if (preferredOffset == null || !validOffsets.contains(preferredOffset)) {
            offset = (ZoneOffset) validOffsets.get(0);
        } else {
            offset = preferredOffset;
        }
        Objects.requireNonNull(offset, "offset");
        return new ChronoZonedDateTimeImpl(localDateTime, offset, zone);
    }

    static ChronoZonedDateTimeImpl<?> ofInstant(Chronology chrono, Instant instant, ZoneId zone) {
        Object offset = zone.getRules().getOffset(instant);
        Objects.requireNonNull(offset, "offset");
        return new ChronoZonedDateTimeImpl((ChronoLocalDateTimeImpl) chrono.localDateTime(LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset)), offset, zone);
    }

    private ChronoZonedDateTimeImpl<D> create(Instant instant, ZoneId zone) {
        return ofInstant(getChronology(), instant, zone);
    }

    static <R extends ChronoLocalDate> ChronoZonedDateTimeImpl<R> ensureValid(Chronology chrono, Temporal temporal) {
        ChronoZonedDateTimeImpl<R> other = (ChronoZonedDateTimeImpl) temporal;
        if (chrono.equals(other.getChronology())) {
            return other;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Chronology mismatch, required: ");
        stringBuilder.append(chrono.getId());
        stringBuilder.append(", actual: ");
        stringBuilder.append(other.getChronology().getId());
        throw new ClassCastException(stringBuilder.toString());
    }

    private ChronoZonedDateTimeImpl(ChronoLocalDateTimeImpl<D> dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = (ChronoLocalDateTimeImpl) Objects.requireNonNull((Object) dateTime, "dateTime");
        this.offset = (ZoneOffset) Objects.requireNonNull((Object) offset, "offset");
        this.zone = (ZoneId) Objects.requireNonNull((Object) zone, "zone");
    }

    public ZoneOffset getOffset() {
        return this.offset;
    }

    public ChronoZonedDateTime<D> withEarlierOffsetAtOverlap() {
        ZoneOffsetTransition trans = getZone().getRules().getTransition(LocalDateTime.from(this));
        if (trans != null && trans.isOverlap()) {
            ZoneOffset earlierOffset = trans.getOffsetBefore();
            if (!earlierOffset.equals(this.offset)) {
                return new ChronoZonedDateTimeImpl(this.dateTime, earlierOffset, this.zone);
            }
        }
        return this;
    }

    public ChronoZonedDateTime<D> withLaterOffsetAtOverlap() {
        ZoneOffsetTransition trans = getZone().getRules().getTransition(LocalDateTime.from(this));
        if (trans != null) {
            ZoneOffset offset = trans.getOffsetAfter();
            if (!offset.equals(getOffset())) {
                return new ChronoZonedDateTimeImpl(this.dateTime, offset, this.zone);
            }
        }
        return this;
    }

    public ChronoLocalDateTime<D> toLocalDateTime() {
        return this.dateTime;
    }

    public ZoneId getZone() {
        return this.zone;
    }

    public ChronoZonedDateTime<D> withZoneSameLocal(ZoneId zone) {
        return ofBest(this.dateTime, zone, this.offset);
    }

    public ChronoZonedDateTime<D> withZoneSameInstant(ZoneId zone) {
        Objects.requireNonNull((Object) zone, "zone");
        return this.zone.equals(zone) ? this : create(this.dateTime.toInstant(this.offset), zone);
    }

    public boolean isSupported(TemporalField field) {
        return (field instanceof ChronoField) || (field != null && field.isSupportedBy(this));
    }

    public ChronoZonedDateTime<D> with(TemporalField field, long newValue) {
        if (!(field instanceof ChronoField)) {
            return ensureValid(getChronology(), field.adjustInto(this, newValue));
        }
        ChronoField f = (ChronoField) field;
        switch (f) {
            case INSTANT_SECONDS:
                return plus(newValue - toEpochSecond(), ChronoUnit.SECONDS);
            case OFFSET_SECONDS:
                return create(this.dateTime.toInstant(ZoneOffset.ofTotalSeconds(f.checkValidIntValue(newValue))), this.zone);
            default:
                return ofBest(this.dateTime.with(field, newValue), this.zone, this.offset);
        }
    }

    public ChronoZonedDateTime<D> plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return with((TemporalAdjuster) this.dateTime.plus(amountToAdd, unit));
        }
        return ensureValid(getChronology(), unit.addTo(this, amountToAdd));
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        Objects.requireNonNull((Object) endExclusive, "endExclusive");
        ChronoZonedDateTime<D> end = getChronology().zonedDateTime(endExclusive);
        if (unit instanceof ChronoUnit) {
            return this.dateTime.until(end.withZoneSameInstant(this.offset).toLocalDateTime(), unit);
        }
        Objects.requireNonNull((Object) unit, "unit");
        return unit.between(this, end);
    }

    private Object writeReplace() {
        return new Ser((byte) 3, this);
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.dateTime);
        out.writeObject(this.offset);
        out.writeObject(this.zone);
    }

    static ChronoZonedDateTime<?> readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        return ((ChronoLocalDateTime) in.readObject()).atZone((ZoneOffset) in.readObject()).withZoneSameLocal((ZoneId) in.readObject());
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChronoZonedDateTime)) {
            return false;
        }
        if (compareTo((ChronoZonedDateTime) obj) != 0) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (toLocalDateTime().hashCode() ^ getOffset().hashCode()) ^ Integer.rotateLeft(getZone().hashCode(), 3);
    }

    public String toString() {
        String str = new StringBuilder();
        str.append(toLocalDateTime().toString());
        str.append(getOffset().toString());
        str = str.toString();
        if (getOffset() == getZone()) {
            return str;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append('[');
        stringBuilder.append(getZone().toString());
        stringBuilder.append(']');
        return stringBuilder.toString();
    }
}
