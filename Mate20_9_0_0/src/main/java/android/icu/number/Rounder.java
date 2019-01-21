package android.icu.number;

import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.MultiplierProducer;
import android.icu.impl.number.RoundingUtils;
import android.icu.util.Currency;
import android.icu.util.Currency.CurrencyUsage;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public abstract class Rounder implements Cloneable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final FracSigRounderImpl COMPACT_STRATEGY = new FracSigRounderImpl(0, 0, 2, -1);
    static final FractionRounderImpl FIXED_FRAC_0 = new FractionRounderImpl(0, 0);
    static final FractionRounderImpl FIXED_FRAC_2 = new FractionRounderImpl(2, 2);
    static final SignificantRounderImpl FIXED_SIG_2 = new SignificantRounderImpl(2, 2);
    static final SignificantRounderImpl FIXED_SIG_3 = new SignificantRounderImpl(3, 3);
    static final FractionRounderImpl MAX_FRAC_6 = new FractionRounderImpl(0, 6);
    static final CurrencyRounderImpl MONETARY_CASH = new CurrencyRounderImpl(CurrencyUsage.CASH);
    static final CurrencyRounderImpl MONETARY_STANDARD = new CurrencyRounderImpl(CurrencyUsage.STANDARD);
    static final IncrementRounderImpl NICKEL = new IncrementRounderImpl(BigDecimal.valueOf(0.05d));
    static final InfiniteRounderImpl NONE = new InfiniteRounderImpl();
    static final PassThroughRounderImpl PASS_THROUGH = new PassThroughRounderImpl();
    static final SignificantRounderImpl RANGE_SIG_2_3 = new SignificantRounderImpl(2, 3);
    MathContext mathContext = RoundingUtils.mathContextUnlimited(RoundingUtils.DEFAULT_ROUNDING_MODE);

    static class FracSigRounderImpl extends Rounder {
        final int maxFrac;
        final int maxSig;
        final int minFrac;
        final int minSig;

        public FracSigRounderImpl(int minFrac, int maxFrac, int minSig, int maxSig) {
            this.minFrac = minFrac;
            this.maxFrac = maxFrac;
            this.minSig = minSig;
            this.maxSig = maxSig;
        }

        public void apply(DecimalQuantity value) {
            int displayMag = Rounder.getDisplayMagnitudeFraction(this.minFrac);
            int roundingMag = Rounder.getRoundingMagnitudeFraction(this.maxFrac);
            if (this.minSig == -1) {
                roundingMag = Math.max(roundingMag, Rounder.getRoundingMagnitudeSignificant(value, this.maxSig));
            } else {
                roundingMag = Math.min(roundingMag, Rounder.getDisplayMagnitudeSignificant(value, this.minSig));
            }
            value.roundToMagnitude(roundingMag, this.mathContext);
            value.setFractionLength(Math.max(0, -displayMag), Integer.MAX_VALUE);
        }
    }

    static class IncrementRounderImpl extends Rounder {
        final BigDecimal increment;

        public IncrementRounderImpl(BigDecimal increment) {
            this.increment = increment;
        }

        public void apply(DecimalQuantity value) {
            value.roundToIncrement(this.increment, this.mathContext);
            value.setFractionLength(this.increment.scale(), this.increment.scale());
        }
    }

    static class InfiniteRounderImpl extends Rounder {
        public void apply(DecimalQuantity value) {
            value.roundToInfinity();
            value.setFractionLength(0, Integer.MAX_VALUE);
        }
    }

    static class PassThroughRounderImpl extends Rounder {
        public void apply(DecimalQuantity value) {
        }
    }

    static class SignificantRounderImpl extends Rounder {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        final int maxSig;
        final int minSig;

        static {
            Class cls = Rounder.class;
        }

        public SignificantRounderImpl(int minSig, int maxSig) {
            this.minSig = minSig;
            this.maxSig = maxSig;
        }

        public void apply(DecimalQuantity value) {
            value.roundToMagnitude(Rounder.getRoundingMagnitudeSignificant(value, this.maxSig), this.mathContext);
            value.setFractionLength(Math.max(0, -Rounder.getDisplayMagnitudeSignificant(value, this.minSig)), Integer.MAX_VALUE);
        }

        public void apply(DecimalQuantity quantity, int minInt) {
            quantity.setFractionLength(this.minSig - minInt, Integer.MAX_VALUE);
        }
    }

    static class CurrencyRounderImpl extends CurrencyRounder {
        final CurrencyUsage usage;

        public CurrencyRounderImpl(CurrencyUsage usage) {
            this.usage = usage;
        }

        public void apply(DecimalQuantity value) {
            throw new AssertionError();
        }
    }

    static class FractionRounderImpl extends FractionRounder {
        final int maxFrac;
        final int minFrac;

        public FractionRounderImpl(int minFrac, int maxFrac) {
            this.minFrac = minFrac;
            this.maxFrac = maxFrac;
        }

        public void apply(DecimalQuantity value) {
            value.roundToMagnitude(Rounder.getRoundingMagnitudeFraction(this.maxFrac), this.mathContext);
            value.setFractionLength(Math.max(0, -Rounder.getDisplayMagnitudeFraction(this.minFrac)), Integer.MAX_VALUE);
        }
    }

    @Deprecated
    public abstract void apply(DecimalQuantity decimalQuantity);

    Rounder() {
    }

    public static Rounder unlimited() {
        return constructInfinite();
    }

    public static FractionRounder integer() {
        return constructFraction(0, 0);
    }

    public static FractionRounder fixedFraction(int minMaxFractionPlaces) {
        if (minMaxFractionPlaces >= 0 && minMaxFractionPlaces <= 100) {
            return constructFraction(minMaxFractionPlaces, minMaxFractionPlaces);
        }
        throw new IllegalArgumentException("Fraction length must be between 0 and 100");
    }

    public static FractionRounder minFraction(int minFractionPlaces) {
        if (minFractionPlaces >= 0 && minFractionPlaces < 100) {
            return constructFraction(minFractionPlaces, -1);
        }
        throw new IllegalArgumentException("Fraction length must be between 0 and 100");
    }

    public static FractionRounder maxFraction(int maxFractionPlaces) {
        if (maxFractionPlaces >= 0 && maxFractionPlaces < 100) {
            return constructFraction(0, maxFractionPlaces);
        }
        throw new IllegalArgumentException("Fraction length must be between 0 and 100");
    }

    public static FractionRounder minMaxFraction(int minFractionPlaces, int maxFractionPlaces) {
        if (minFractionPlaces >= 0 && maxFractionPlaces <= 100 && minFractionPlaces <= maxFractionPlaces) {
            return constructFraction(minFractionPlaces, maxFractionPlaces);
        }
        throw new IllegalArgumentException("Fraction length must be between 0 and 100");
    }

    public static Rounder fixedDigits(int minMaxSignificantDigits) {
        if (minMaxSignificantDigits > 0 && minMaxSignificantDigits <= 100) {
            return constructSignificant(minMaxSignificantDigits, minMaxSignificantDigits);
        }
        throw new IllegalArgumentException("Significant digits must be between 0 and 100");
    }

    public static Rounder minDigits(int minSignificantDigits) {
        if (minSignificantDigits > 0 && minSignificantDigits <= 100) {
            return constructSignificant(minSignificantDigits, -1);
        }
        throw new IllegalArgumentException("Significant digits must be between 0 and 100");
    }

    public static Rounder maxDigits(int maxSignificantDigits) {
        if (maxSignificantDigits > 0 && maxSignificantDigits <= 100) {
            return constructSignificant(0, maxSignificantDigits);
        }
        throw new IllegalArgumentException("Significant digits must be between 0 and 100");
    }

    public static Rounder minMaxDigits(int minSignificantDigits, int maxSignificantDigits) {
        if (minSignificantDigits > 0 && maxSignificantDigits <= 100 && minSignificantDigits <= maxSignificantDigits) {
            return constructSignificant(minSignificantDigits, maxSignificantDigits);
        }
        throw new IllegalArgumentException("Significant digits must be between 0 and 100");
    }

    public static Rounder increment(BigDecimal roundingIncrement) {
        if (roundingIncrement != null && roundingIncrement.compareTo(BigDecimal.ZERO) > 0) {
            return constructIncrement(roundingIncrement);
        }
        throw new IllegalArgumentException("Rounding increment must be positive and non-null");
    }

    public static CurrencyRounder currency(CurrencyUsage currencyUsage) {
        if (currencyUsage != null) {
            return constructCurrency(currencyUsage);
        }
        throw new IllegalArgumentException("CurrencyUsage must be non-null");
    }

    public Rounder withMode(RoundingMode roundingMode) {
        return withMode(RoundingUtils.mathContextUnlimited(roundingMode));
    }

    @Deprecated
    public Rounder withMode(MathContext mathContext) {
        if (this.mathContext.equals(mathContext)) {
            return this;
        }
        Rounder other = (Rounder) clone();
        other.mathContext = mathContext;
        return other;
    }

    @Deprecated
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    static Rounder constructInfinite() {
        return NONE;
    }

    static FractionRounder constructFraction(int minFrac, int maxFrac) {
        if (minFrac == 0 && maxFrac == 0) {
            return FIXED_FRAC_0;
        }
        if (minFrac == 2 && maxFrac == 2) {
            return FIXED_FRAC_2;
        }
        if (minFrac == 0 && maxFrac == 6) {
            return MAX_FRAC_6;
        }
        return new FractionRounderImpl(minFrac, maxFrac);
    }

    static Rounder constructSignificant(int minSig, int maxSig) {
        if (minSig == 2 && maxSig == 2) {
            return FIXED_SIG_2;
        }
        if (minSig == 3 && maxSig == 3) {
            return FIXED_SIG_3;
        }
        if (minSig == 2 && maxSig == 3) {
            return RANGE_SIG_2_3;
        }
        return new SignificantRounderImpl(minSig, maxSig);
    }

    static Rounder constructFractionSignificant(FractionRounder base_, int minSig, int maxSig) {
        FractionRounderImpl base = (FractionRounderImpl) base_;
        if (base.minFrac == 0 && base.maxFrac == 0 && minSig == 2) {
            return COMPACT_STRATEGY;
        }
        return new FracSigRounderImpl(base.minFrac, base.maxFrac, minSig, maxSig);
    }

    static Rounder constructIncrement(BigDecimal increment) {
        if (increment.equals(NICKEL.increment)) {
            return NICKEL;
        }
        return new IncrementRounderImpl(increment);
    }

    static CurrencyRounder constructCurrency(CurrencyUsage usage) {
        if (usage == CurrencyUsage.STANDARD) {
            return MONETARY_STANDARD;
        }
        if (usage == CurrencyUsage.CASH) {
            return MONETARY_CASH;
        }
        throw new AssertionError();
    }

    static Rounder constructFromCurrency(CurrencyRounder base_, Currency currency) {
        CurrencyRounderImpl base = (CurrencyRounderImpl) base_;
        double incrementDouble = currency.getRoundingIncrement(base.usage);
        if (incrementDouble != 0.0d) {
            return constructIncrement(BigDecimal.valueOf(incrementDouble));
        }
        int minMaxFrac = currency.getDefaultFractionDigits(base.usage);
        return constructFraction(minMaxFrac, minMaxFrac);
    }

    static Rounder constructPassThrough() {
        return PASS_THROUGH;
    }

    Rounder withLocaleData(Currency currency) {
        if (this instanceof CurrencyRounder) {
            return ((CurrencyRounder) this).withCurrency(currency);
        }
        return this;
    }

    int chooseMultiplierAndApply(DecimalQuantity input, MultiplierProducer producer) {
        DecimalQuantity copy = input.createCopy();
        int magnitude = input.getMagnitude();
        int multiplier = producer.getMultiplier(magnitude);
        input.adjustMagnitude(multiplier);
        apply(input);
        if (input.isZero() || input.getMagnitude() != (magnitude + multiplier) + 1) {
            return multiplier;
        }
        magnitude++;
        input.copyFrom(copy);
        multiplier = producer.getMultiplier(magnitude);
        input.adjustMagnitude(multiplier);
        apply(input);
        return multiplier;
    }

    private static int getRoundingMagnitudeFraction(int maxFrac) {
        if (maxFrac == -1) {
            return Integer.MIN_VALUE;
        }
        return -maxFrac;
    }

    private static int getRoundingMagnitudeSignificant(DecimalQuantity value, int maxSig) {
        if (maxSig == -1) {
            return Integer.MIN_VALUE;
        }
        return ((value.isZero() ? 0 : value.getMagnitude()) - maxSig) + 1;
    }

    private static int getDisplayMagnitudeFraction(int minFrac) {
        if (minFrac == 0) {
            return Integer.MAX_VALUE;
        }
        return -minFrac;
    }

    private static int getDisplayMagnitudeSignificant(DecimalQuantity value, int minSig) {
        return ((value.isZero() ? 0 : value.getMagnitude()) - minSig) + 1;
    }
}
