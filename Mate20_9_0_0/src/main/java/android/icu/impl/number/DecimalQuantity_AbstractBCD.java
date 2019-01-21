package android.icu.impl.number;

import android.icu.impl.StandardPlural;
import android.icu.text.PluralRules;
import android.icu.text.PluralRules.IFixedDecimal;
import android.icu.text.PluralRules.Operand;
import android.icu.text.UFieldPosition;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.FieldPosition;

public abstract class DecimalQuantity_AbstractBCD implements DecimalQuantity {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final double[] DOUBLE_MULTIPLIERS = new double[]{1.0d, 10.0d, 100.0d, 1000.0d, 10000.0d, 100000.0d, 1000000.0d, 1.0E7d, 1.0E8d, 1.0E9d, 1.0E10d, 1.0E11d, 1.0E12d, 1.0E13d, 1.0E14d, 1.0E15d, 1.0E16d, 1.0E17d, 1.0E18d, 1.0E19d, 1.0E20d, 1.0E21d};
    protected static final int INFINITY_FLAG = 2;
    protected static final int NAN_FLAG = 4;
    protected static final int NEGATIVE_FLAG = 1;
    private static final int SECTION_LOWER_EDGE = -1;
    private static final int SECTION_UPPER_EDGE = -2;
    @Deprecated
    public boolean explicitExactDouble = false;
    protected byte flags;
    protected boolean isApproximate;
    protected int lOptPos = Integer.MAX_VALUE;
    protected int lReqPos = 0;
    protected int origDelta;
    protected double origDouble;
    protected int precision;
    protected int rOptPos = Integer.MIN_VALUE;
    protected int rReqPos = 0;
    protected int scale;

    protected abstract BigDecimal bcdToBigDecimal();

    protected abstract void compact();

    protected abstract void copyBcdFrom(DecimalQuantity decimalQuantity);

    protected abstract byte getDigitPos(int i);

    protected abstract void readBigIntegerToBcd(BigInteger bigInteger);

    protected abstract void readIntToBcd(int i);

    protected abstract void readLongToBcd(long j);

    protected abstract void setBcdToZero();

    protected abstract void setDigitPos(int i, byte b);

    protected abstract void shiftLeft(int i);

    protected abstract void shiftRight(int i);

    public void copyFrom(DecimalQuantity _other) {
        copyBcdFrom(_other);
        DecimalQuantity_AbstractBCD other = (DecimalQuantity_AbstractBCD) _other;
        this.lOptPos = other.lOptPos;
        this.lReqPos = other.lReqPos;
        this.rReqPos = other.rReqPos;
        this.rOptPos = other.rOptPos;
        this.scale = other.scale;
        this.precision = other.precision;
        this.flags = other.flags;
        this.origDouble = other.origDouble;
        this.origDelta = other.origDelta;
        this.isApproximate = other.isApproximate;
    }

    public DecimalQuantity_AbstractBCD clear() {
        this.lOptPos = Integer.MAX_VALUE;
        this.lReqPos = 0;
        this.rReqPos = 0;
        this.rOptPos = Integer.MIN_VALUE;
        this.flags = (byte) 0;
        setBcdToZero();
        return this;
    }

    public void setIntegerLength(int minInt, int maxInt) {
        this.lOptPos = maxInt;
        this.lReqPos = minInt;
    }

    public void setFractionLength(int minFrac, int maxFrac) {
        this.rReqPos = -minFrac;
        this.rOptPos = -maxFrac;
    }

    public long getPositionFingerprint() {
        return (((0 ^ ((long) this.lOptPos)) ^ ((long) (this.lReqPos << 16))) ^ (((long) this.rReqPos) << 32)) ^ (((long) this.rOptPos) << 48);
    }

    public void roundToIncrement(BigDecimal roundingIncrement, MathContext mathContext) {
        BigDecimal temp = toBigDecimal().divide(roundingIncrement, 0, mathContext.getRoundingMode()).multiply(roundingIncrement).round(mathContext);
        if (temp.signum() == 0) {
            setBcdToZero();
        } else {
            setToBigDecimal(temp);
        }
    }

    public void multiplyBy(BigDecimal multiplicand) {
        if (!isInfinite() && !isZero() && !isNaN()) {
            setToBigDecimal(toBigDecimal().multiply(multiplicand));
        }
    }

    public int getMagnitude() throws ArithmeticException {
        if (this.precision != 0) {
            return (this.scale + this.precision) - 1;
        }
        throw new ArithmeticException("Magnitude is not well-defined for zero");
    }

    public void adjustMagnitude(int delta) {
        if (this.precision != 0) {
            this.scale += delta;
            this.origDelta += delta;
        }
    }

    public StandardPlural getStandardPlural(PluralRules rules) {
        if (rules == null) {
            return StandardPlural.OTHER;
        }
        return StandardPlural.orOtherFromString(rules.select((IFixedDecimal) this));
    }

    public double getPluralOperand(Operand operand) {
        switch (operand) {
            case i:
                return (double) toLong();
            case f:
                return (double) toFractionLong(true);
            case t:
                return (double) toFractionLong(false);
            case v:
                return (double) fractionCount();
            case w:
                return (double) fractionCountWithoutTrailingZeros();
            default:
                return Math.abs(toDouble());
        }
    }

    public void populateUFieldPosition(FieldPosition fp) {
        if (fp instanceof UFieldPosition) {
            ((UFieldPosition) fp).setFractionDigits((int) getPluralOperand(Operand.v), (long) getPluralOperand(Operand.f));
        }
    }

    public int getUpperDisplayMagnitude() {
        int magnitude = this.scale + this.precision;
        int result = this.lReqPos > magnitude ? this.lReqPos : this.lOptPos < magnitude ? this.lOptPos : magnitude;
        return result - 1;
    }

    public int getLowerDisplayMagnitude() {
        int magnitude = this.scale;
        if (this.rReqPos < magnitude) {
            return this.rReqPos;
        }
        return this.rOptPos > magnitude ? this.rOptPos : magnitude;
    }

    public byte getDigit(int magnitude) {
        return getDigitPos(magnitude - this.scale);
    }

    private int fractionCount() {
        return -getLowerDisplayMagnitude();
    }

    private int fractionCountWithoutTrailingZeros() {
        return Math.max(-this.scale, 0);
    }

    public boolean isNegative() {
        return (this.flags & 1) != 0;
    }

    public boolean isInfinite() {
        return (this.flags & 2) != 0;
    }

    public boolean isNaN() {
        return (this.flags & 4) != 0;
    }

    public boolean isZero() {
        return this.precision == 0;
    }

    public void setToInt(int n) {
        setBcdToZero();
        this.flags = (byte) 0;
        if (n < 0) {
            this.flags = (byte) (this.flags | 1);
            n = -n;
        }
        if (n != 0) {
            _setToInt(n);
            compact();
        }
    }

    private void _setToInt(int n) {
        if (n == Integer.MIN_VALUE) {
            readLongToBcd(-((long) n));
        } else {
            readIntToBcd(n);
        }
    }

    public void setToLong(long n) {
        setBcdToZero();
        this.flags = (byte) 0;
        if (n < 0) {
            this.flags = (byte) (this.flags | 1);
            n = -n;
        }
        if (n != 0) {
            _setToLong(n);
            compact();
        }
    }

    private void _setToLong(long n) {
        if (n == Long.MIN_VALUE) {
            readBigIntegerToBcd(BigInteger.valueOf(n).negate());
        } else if (n <= 2147483647L) {
            readIntToBcd((int) n);
        } else {
            readLongToBcd(n);
        }
    }

    public void setToBigInteger(BigInteger n) {
        setBcdToZero();
        this.flags = (byte) 0;
        if (n.signum() == -1) {
            this.flags = (byte) (this.flags | 1);
            n = n.negate();
        }
        if (n.signum() != 0) {
            _setToBigInteger(n);
            compact();
        }
    }

    private void _setToBigInteger(BigInteger n) {
        if (n.bitLength() < 32) {
            readIntToBcd(n.intValue());
        } else if (n.bitLength() < 64) {
            readLongToBcd(n.longValue());
        } else {
            readBigIntegerToBcd(n);
        }
    }

    public void setToDouble(double n) {
        setBcdToZero();
        this.flags = (byte) 0;
        if (Double.compare(n, 0.0d) < 0) {
            this.flags = (byte) (this.flags | 1);
            n = -n;
        }
        if (Double.isNaN(n)) {
            this.flags = (byte) (this.flags | 4);
        } else if (Double.isInfinite(n)) {
            this.flags = (byte) (this.flags | 2);
        } else if (n != 0.0d) {
            _setToDoubleFast(n);
            compact();
        }
    }

    private void _setToDoubleFast(double n) {
        this.isApproximate = true;
        this.origDouble = n;
        this.origDelta = 0;
        int exponent = ((int) ((9218868437227405312L & Double.doubleToLongBits(n)) >> 52)) - 1023;
        if (exponent > 52 || ((double) ((long) n)) != n) {
            double n2;
            int fracLength = (int) (((double) (52 - exponent)) / 3.32192809489d);
            int i;
            if (fracLength >= 0) {
                n2 = n;
                i = fracLength;
                while (i >= 22) {
                    n2 *= 1.0E22d;
                    i -= 22;
                }
                n2 *= DOUBLE_MULTIPLIERS[i];
            } else {
                n2 = n;
                i = fracLength;
                while (i <= -22) {
                    n2 /= 1.0E22d;
                    i += 22;
                }
                n2 /= DOUBLE_MULTIPLIERS[-i];
            }
            long result = Math.round(n2);
            if (result != 0) {
                _setToLong(result);
                this.scale -= fracLength;
            }
            return;
        }
        _setToLong((long) n);
    }

    private void convertToAccurateDouble() {
        double n = this.origDouble;
        int delta = this.origDelta;
        setBcdToZero();
        String dstr = Double.toString(n);
        int expPos;
        StringBuilder stringBuilder;
        if (dstr.indexOf(69) != -1) {
            expPos = dstr.indexOf(69);
            stringBuilder = new StringBuilder();
            stringBuilder.append(dstr.charAt(0));
            stringBuilder.append(dstr.substring(2, expPos));
            _setToLong(Long.parseLong(stringBuilder.toString()));
            this.scale += (Integer.parseInt(dstr.substring(expPos + 1)) - (expPos - 1)) + 1;
        } else if (dstr.charAt(0) == '0') {
            _setToLong(Long.parseLong(dstr.substring(2)));
            this.scale += 2 - dstr.length();
        } else if (dstr.charAt(dstr.length() - 1) == '0') {
            _setToLong(Long.parseLong(dstr.substring(0, dstr.length() - 2)));
        } else {
            expPos = dstr.indexOf(46);
            stringBuilder = new StringBuilder();
            stringBuilder.append(dstr.substring(0, expPos));
            stringBuilder.append(dstr.substring(expPos + 1));
            _setToLong(Long.parseLong(stringBuilder.toString()));
            this.scale += (expPos - dstr.length()) + 1;
        }
        this.scale += delta;
        compact();
        this.explicitExactDouble = true;
    }

    public void setToBigDecimal(BigDecimal n) {
        setBcdToZero();
        this.flags = (byte) 0;
        if (n.signum() == -1) {
            this.flags = (byte) (this.flags | 1);
            n = n.negate();
        }
        if (n.signum() != 0) {
            _setToBigDecimal(n);
            compact();
        }
    }

    private void _setToBigDecimal(BigDecimal n) {
        int fracLength = n.scale();
        _setToBigInteger(n.scaleByPowerOfTen(fracLength).toBigInteger());
        this.scale -= fracLength;
    }

    protected long toLong() {
        long result = 0;
        for (int magnitude = (this.scale + this.precision) - 1; magnitude >= 0; magnitude--) {
            result = (10 * result) + ((long) getDigitPos(magnitude - this.scale));
        }
        return result;
    }

    protected long toFractionLong(boolean includeTrailingZeros) {
        long result = 0;
        int magnitude = -1;
        while (true) {
            if ((magnitude >= this.scale || (includeTrailingZeros && magnitude >= this.rReqPos)) && magnitude >= this.rOptPos) {
                result = (10 * result) + ((long) getDigitPos(magnitude - this.scale));
                magnitude--;
            }
        }
        return result;
    }

    public double toDouble() {
        if (this.isApproximate) {
            return toDoubleFromOriginal();
        }
        if (isNaN()) {
            return Double.NaN;
        }
        if (isInfinite()) {
            return isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        int shift;
        double result;
        long tempLong = 0;
        int lostDigits = this.precision - Math.min(this.precision, 17);
        for (shift = this.precision - 1; shift >= lostDigits; shift--) {
            tempLong = (10 * tempLong) + ((long) getDigitPos(shift));
        }
        double result2 = (double) tempLong;
        int _scale = this.scale + lostDigits;
        if (_scale >= 0) {
            result = result2;
            result2 = _scale;
            while (result2 >= 22) {
                result *= 1.0E22d;
                result2 -= 22;
            }
            result *= DOUBLE_MULTIPLIERS[result2];
        } else {
            result = result2;
            shift = _scale;
            while (shift <= -22) {
                result /= 1.0E22d;
                shift += 22;
            }
            result /= DOUBLE_MULTIPLIERS[-shift];
        }
        if (isNegative()) {
            result = -result;
        }
        return result;
    }

    public BigDecimal toBigDecimal() {
        if (this.isApproximate) {
            convertToAccurateDouble();
        }
        return bcdToBigDecimal();
    }

    protected double toDoubleFromOriginal() {
        double result = this.origDouble;
        int delta = this.origDelta;
        if (delta >= 0) {
            while (delta >= 22) {
                result *= 1.0E22d;
                delta -= 22;
            }
            result *= DOUBLE_MULTIPLIERS[delta];
        } else {
            while (delta <= -22) {
                result /= 1.0E22d;
                delta += 22;
            }
            result /= DOUBLE_MULTIPLIERS[-delta];
        }
        if (isNegative()) {
            return result * -1.0d;
        }
        return result;
    }

    private static int safeSubtract(int a, int b) {
        int diff = a - b;
        if (b < 0 && diff < a) {
            return Integer.MAX_VALUE;
        }
        if (b <= 0 || diff <= a) {
            return diff;
        }
        return Integer.MIN_VALUE;
    }

    public void roundToMagnitude(int magnitude, MathContext mathContext) {
        int i = magnitude;
        int position = safeSubtract(i, this.scale);
        int _mcPrecision = mathContext.getPrecision();
        if (i == Integer.MAX_VALUE || (_mcPrecision > 0 && this.precision - position > _mcPrecision)) {
            position = this.precision - _mcPrecision;
        }
        if ((position > 0 || this.isApproximate) && this.precision != 0) {
            int p;
            byte leadingDigit = getDigitPos(safeSubtract(position, 1));
            byte trailingDigit = getDigitPos(position);
            int section = 2;
            if (this.isApproximate) {
                p = safeSubtract(position, 2);
                int minP = Math.max(0, this.precision - 14);
                if (leadingDigit == (byte) 0) {
                    section = -1;
                    while (p >= minP) {
                        if (getDigitPos(p) != (byte) 0) {
                            section = 1;
                            break;
                        }
                        p--;
                    }
                } else if (leadingDigit == (byte) 4) {
                    while (p >= minP) {
                        if (getDigitPos(p) != (byte) 9) {
                            section = 1;
                            break;
                        }
                        p--;
                    }
                } else if (leadingDigit == (byte) 5) {
                    while (p >= minP) {
                        if (getDigitPos(p) != (byte) 0) {
                            section = 3;
                            break;
                        }
                        p--;
                    }
                } else if (leadingDigit == (byte) 9) {
                    section = -2;
                    while (p >= minP) {
                        if (getDigitPos(p) != (byte) 9) {
                            section = 3;
                            break;
                        }
                        p--;
                    }
                } else {
                    section = leadingDigit < (byte) 5 ? 1 : 3;
                }
                boolean roundsAtMidpoint = RoundingUtils.roundsAtMidpoint(mathContext.getRoundingMode().ordinal());
                if (safeSubtract(position, 1) < this.precision - 14 || ((roundsAtMidpoint && section == 2) || (!roundsAtMidpoint && section < 0))) {
                    convertToAccurateDouble();
                    roundToMagnitude(magnitude, mathContext);
                    return;
                }
                this.isApproximate = false;
                this.origDouble = 0.0d;
                this.origDelta = 0;
                if (position > 0) {
                    if (section == -1) {
                        section = 1;
                    }
                    if (section == -2) {
                        section = 3;
                    }
                } else {
                    return;
                }
            } else if (leadingDigit < (byte) 5) {
                section = 1;
            } else if (leadingDigit > (byte) 5) {
                section = 3;
            } else {
                for (p = safeSubtract(position, 2); p >= 0; p--) {
                    if (getDigitPos(p) != (byte) 0) {
                        section = 3;
                        break;
                    }
                }
            }
            p = RoundingUtils.getRoundingDirection(trailingDigit % 2 == 0 ? 1 : 0, isNegative(), section, mathContext.getRoundingMode().ordinal(), this);
            if (position >= this.precision) {
                setBcdToZero();
                this.scale = i;
            } else {
                shiftRight(position);
            }
            if (p == 0) {
                if (trailingDigit == (byte) 9) {
                    int bubblePos = 0;
                    while (getDigitPos(bubblePos) == (byte) 9) {
                        bubblePos++;
                    }
                    shiftRight(bubblePos);
                }
                setDigitPos(0, (byte) (getDigitPos(0) + 1));
                this.precision++;
            }
            compact();
        }
    }

    public void roundToInfinity() {
        if (this.isApproximate) {
            convertToAccurateDouble();
        }
    }

    @Deprecated
    public void appendDigit(byte value, int leadingZeros, boolean appendAsInteger) {
        if (value == (byte) 0) {
            if (appendAsInteger && this.precision != 0) {
                this.scale += leadingZeros + 1;
            }
            return;
        }
        if (this.scale > 0) {
            leadingZeros += this.scale;
            if (appendAsInteger) {
                this.scale = 0;
            }
        }
        shiftLeft(leadingZeros + 1);
        setDigitPos(0, value);
        if (appendAsInteger) {
            this.scale += leadingZeros + 1;
        }
    }

    public String toPlainString() {
        StringBuilder sb = new StringBuilder();
        if (isNegative()) {
            sb.append('-');
        }
        for (int m = getUpperDisplayMagnitude(); m >= getLowerDisplayMagnitude(); m--) {
            sb.append(getDigit(m));
            if (m == 0) {
                sb.append('.');
            }
        }
        return sb.toString();
    }
}
