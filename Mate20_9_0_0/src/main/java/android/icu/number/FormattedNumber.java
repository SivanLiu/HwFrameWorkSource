package android.icu.number;

import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.MicroProps;
import android.icu.impl.number.NumberStringBuilder;
import android.icu.text.PluralRules.IFixedDecimal;
import android.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.util.Arrays;

public class FormattedNumber {
    DecimalQuantity fq;
    MicroProps micros;
    NumberStringBuilder nsb;

    FormattedNumber(NumberStringBuilder nsb, DecimalQuantity fq, MicroProps micros) {
        this.nsb = nsb;
        this.fq = fq;
        this.micros = micros;
    }

    public String toString() {
        return this.nsb.toString();
    }

    public <A extends Appendable> A appendTo(A appendable) {
        try {
            appendable.append(this.nsb);
            return appendable;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public void populateFieldPosition(FieldPosition fieldPosition) {
        populateFieldPosition(fieldPosition, 0);
    }

    @Deprecated
    public void populateFieldPosition(FieldPosition fieldPosition, int offset) {
        this.nsb.populateFieldPosition(fieldPosition, offset);
        this.fq.populateUFieldPosition(fieldPosition);
    }

    public AttributedCharacterIterator getFieldIterator() {
        return this.nsb.getIterator();
    }

    public BigDecimal toBigDecimal() {
        return this.fq.toBigDecimal();
    }

    @Deprecated
    public String getPrefix() {
        NumberStringBuilder temp = new NumberStringBuilder();
        int length = this.micros.modOuter.apply(temp, 0, 0);
        this.micros.modInner.apply(temp, 0, length + this.micros.modMiddle.apply(temp, 0, length));
        return temp.subSequence(0, (this.micros.modOuter.getPrefixLength() + this.micros.modMiddle.getPrefixLength()) + this.micros.modInner.getPrefixLength()).toString();
    }

    @Deprecated
    public String getSuffix() {
        NumberStringBuilder temp = new NumberStringBuilder();
        int length = this.micros.modOuter.apply(temp, 0, 0);
        length += this.micros.modMiddle.apply(temp, 0, length);
        return temp.subSequence((this.micros.modOuter.getPrefixLength() + this.micros.modMiddle.getPrefixLength()) + this.micros.modInner.getPrefixLength(), length + this.micros.modInner.apply(temp, 0, length)).toString();
    }

    @Deprecated
    public IFixedDecimal getFixedDecimal() {
        return this.fq;
    }

    public int hashCode() {
        return (Arrays.hashCode(this.nsb.toCharArray()) ^ Arrays.hashCode(this.nsb.toFieldArray())) ^ this.fq.toBigDecimal().hashCode();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof FormattedNumber)) {
            return false;
        }
        FormattedNumber _other = (FormattedNumber) other;
        return (Arrays.equals(this.nsb.toCharArray(), _other.nsb.toCharArray()) ^ Arrays.equals(this.nsb.toFieldArray(), _other.nsb.toFieldArray())) ^ this.fq.toBigDecimal().equals(_other.fq.toBigDecimal());
    }
}
