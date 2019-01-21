package android.icu.text;

import android.icu.impl.PluralRulesLoader;
import android.icu.impl.number.Padder;
import android.icu.util.Output;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class PluralRules implements Serializable {
    static final UnicodeSet ALLOWED_ID = new UnicodeSet("[a-z]").freeze();
    static final Pattern AND_SEPARATED = Pattern.compile("\\s*and\\s*");
    static final Pattern AT_SEPARATED = Pattern.compile("\\s*\\Q\\E@\\s*");
    @Deprecated
    public static final String CATEGORY_SEPARATOR = ";  ";
    static final Pattern COMMA_SEPARATED = Pattern.compile("\\s*,\\s*");
    public static final PluralRules DEFAULT = new PluralRules(new RuleList().addRule(DEFAULT_RULE));
    private static final Rule DEFAULT_RULE = new Rule(KEYWORD_OTHER, NO_CONSTRAINT, null, null);
    static final Pattern DOTDOT_SEPARATED = Pattern.compile("\\s*\\Q..\\E\\s*");
    public static final String KEYWORD_FEW = "few";
    public static final String KEYWORD_MANY = "many";
    public static final String KEYWORD_ONE = "one";
    public static final String KEYWORD_OTHER = "other";
    @Deprecated
    public static final String KEYWORD_RULE_SEPARATOR = ": ";
    public static final String KEYWORD_TWO = "two";
    public static final String KEYWORD_ZERO = "zero";
    private static final Constraint NO_CONSTRAINT = new Constraint() {
        private static final long serialVersionUID = 9163464945387899416L;

        public boolean isFulfilled(IFixedDecimal n) {
            return true;
        }

        public boolean isLimited(SampleType sampleType) {
            return false;
        }

        public String toString() {
            return "";
        }
    };
    public static final double NO_UNIQUE_VALUE = -0.00123456777d;
    static final Pattern OR_SEPARATED = Pattern.compile("\\s*or\\s*");
    static final Pattern SEMI_SEPARATED = Pattern.compile("\\s*;\\s*");
    static final Pattern TILDE_SEPARATED = Pattern.compile("\\s*~\\s*");
    private static final long serialVersionUID = 1;
    private final transient Set<String> keywords;
    private final RuleList rules;

    private interface Constraint extends Serializable {
        boolean isFulfilled(IFixedDecimal iFixedDecimal);

        boolean isLimited(SampleType sampleType);
    }

    @Deprecated
    public static abstract class Factory {
        @Deprecated
        public abstract PluralRules forLocale(ULocale uLocale, PluralType pluralType);

        @Deprecated
        public abstract ULocale[] getAvailableULocales();

        @Deprecated
        public abstract ULocale getFunctionalEquivalent(ULocale uLocale, boolean[] zArr);

        @Deprecated
        public abstract boolean hasOverride(ULocale uLocale);

        @Deprecated
        protected Factory() {
        }

        @Deprecated
        public final PluralRules forLocale(ULocale locale) {
            return forLocale(locale, PluralType.CARDINAL);
        }

        @Deprecated
        public static PluralRulesLoader getDefaultFactory() {
            return PluralRulesLoader.loader;
        }
    }

    @Deprecated
    public static class FixedDecimalRange {
        @Deprecated
        public final FixedDecimal end;
        @Deprecated
        public final FixedDecimal start;

        @Deprecated
        public FixedDecimalRange(FixedDecimal start, FixedDecimal end) {
            if (start.visibleDecimalDigitCount == end.visibleDecimalDigitCount) {
                this.start = start;
                this.end = end;
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ranges must have the same number of visible decimals: ");
            stringBuilder.append(start);
            stringBuilder.append("~");
            stringBuilder.append(end);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        @Deprecated
        public String toString() {
            String str;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.start);
            if (this.end == this.start) {
                str = "";
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("~");
                stringBuilder2.append(this.end);
                str = stringBuilder2.toString();
            }
            stringBuilder.append(str);
            return stringBuilder.toString();
        }
    }

    @Deprecated
    public static class FixedDecimalSamples {
        @Deprecated
        public final boolean bounded;
        @Deprecated
        public final SampleType sampleType;
        @Deprecated
        public final Set<FixedDecimalRange> samples;

        private FixedDecimalSamples(SampleType sampleType, Set<FixedDecimalRange> samples, boolean bounded) {
            this.sampleType = sampleType;
            this.samples = samples;
            this.bounded = bounded;
        }

        static FixedDecimalSamples parse(String source) {
            SampleType sampleType2;
            Set<FixedDecimalRange> samples2 = new LinkedHashSet();
            if (source.startsWith("integer")) {
                sampleType2 = SampleType.INTEGER;
            } else if (source.startsWith("decimal")) {
                sampleType2 = SampleType.DECIMAL;
            } else {
                throw new IllegalArgumentException("Samples must start with 'integer' or 'decimal'");
            }
            boolean haveBound = false;
            boolean haveBound2 = true;
            for (String range : PluralRules.COMMA_SEPARATED.split(source.substring(7).trim())) {
                StringBuilder stringBuilder;
                if (range.equals("…") || range.equals("...")) {
                    haveBound2 = false;
                    haveBound = true;
                } else if (haveBound) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Can only have … at the end of samples: ");
                    stringBuilder.append(range);
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else {
                    String[] rangeParts = PluralRules.TILDE_SEPARATED.split(range);
                    FixedDecimal sample;
                    switch (rangeParts.length) {
                        case 1:
                            sample = new FixedDecimal(rangeParts[0]);
                            checkDecimal(sampleType2, sample);
                            samples2.add(new FixedDecimalRange(sample, sample));
                            break;
                        case 2:
                            sample = new FixedDecimal(rangeParts[0]);
                            FixedDecimal end = new FixedDecimal(rangeParts[1]);
                            checkDecimal(sampleType2, sample);
                            checkDecimal(sampleType2, end);
                            samples2.add(new FixedDecimalRange(sample, end));
                            break;
                        default:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Ill-formed number range: ");
                            stringBuilder.append(range);
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
            }
            return new FixedDecimalSamples(sampleType2, Collections.unmodifiableSet(samples2), haveBound2);
        }

        private static void checkDecimal(SampleType sampleType2, FixedDecimal sample) {
            Object obj = null;
            Object obj2 = sampleType2 == SampleType.INTEGER ? 1 : null;
            if (sample.getVisibleDecimalDigitCount() == 0) {
                obj = 1;
            }
            if (obj2 != obj) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ill-formed number range: ");
                stringBuilder.append(sample);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        @Deprecated
        public Set<Double> addSamples(Set<Double> result) {
            for (FixedDecimalRange item : this.samples) {
                long startDouble = item.start.getShiftedValue();
                long endDouble = item.end.getShiftedValue();
                for (long d = startDouble; d <= endDouble; d += PluralRules.serialVersionUID) {
                    result.add(Double.valueOf(((double) d) / ((double) item.start.baseFactor)));
                }
            }
            return result;
        }

        @Deprecated
        public String toString() {
            StringBuilder b = new StringBuilder("@").append(this.sampleType.toString().toLowerCase(Locale.ENGLISH));
            boolean first = true;
            for (FixedDecimalRange item : this.samples) {
                if (first) {
                    first = false;
                } else {
                    b.append(",");
                }
                b.append(' ');
                b.append(item);
            }
            if (!this.bounded) {
                b.append(", …");
            }
            return b.toString();
        }

        @Deprecated
        public Set<FixedDecimalRange> getSamples() {
            return this.samples;
        }

        @Deprecated
        public void getStartEndSamples(Set<FixedDecimal> target) {
            for (FixedDecimalRange item : this.samples) {
                target.add(item.start);
                target.add(item.end);
            }
        }
    }

    @Deprecated
    public interface IFixedDecimal {
        @Deprecated
        double getPluralOperand(Operand operand);

        @Deprecated
        boolean isInfinite();

        @Deprecated
        boolean isNaN();
    }

    public enum KeywordStatus {
        INVALID,
        SUPPRESSED,
        UNIQUE,
        BOUNDED,
        UNBOUNDED
    }

    @Deprecated
    public enum Operand {
        n,
        i,
        f,
        t,
        v,
        w,
        j
    }

    public enum PluralType {
        CARDINAL,
        ORDINAL
    }

    private static class Rule implements Serializable {
        private static final long serialVersionUID = 1;
        private final Constraint constraint;
        private final FixedDecimalSamples decimalSamples;
        private final FixedDecimalSamples integerSamples;
        private final String keyword;

        public Rule(String keyword, Constraint constraint, FixedDecimalSamples integerSamples, FixedDecimalSamples decimalSamples) {
            this.keyword = keyword;
            this.constraint = constraint;
            this.integerSamples = integerSamples;
            this.decimalSamples = decimalSamples;
        }

        public Rule and(Constraint c) {
            return new Rule(this.keyword, new AndConstraint(this.constraint, c), this.integerSamples, this.decimalSamples);
        }

        public Rule or(Constraint c) {
            return new Rule(this.keyword, new OrConstraint(this.constraint, c), this.integerSamples, this.decimalSamples);
        }

        public String getKeyword() {
            return this.keyword;
        }

        public boolean appliesTo(IFixedDecimal n) {
            return this.constraint.isFulfilled(n);
        }

        public boolean isLimited(SampleType sampleType) {
            return this.constraint.isLimited(sampleType);
        }

        public String toString() {
            String str;
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.keyword);
            stringBuilder2.append(PluralRules.KEYWORD_RULE_SEPARATOR);
            stringBuilder2.append(this.constraint.toString());
            if (this.integerSamples == null) {
                str = "";
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(Padder.FALLBACK_PADDING_STRING);
                stringBuilder.append(this.integerSamples.toString());
                str = stringBuilder.toString();
            }
            stringBuilder2.append(str);
            if (this.decimalSamples == null) {
                str = "";
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(Padder.FALLBACK_PADDING_STRING);
                stringBuilder.append(this.decimalSamples.toString());
                str = stringBuilder.toString();
            }
            stringBuilder2.append(str);
            return stringBuilder2.toString();
        }

        @Deprecated
        public int hashCode() {
            return this.keyword.hashCode() ^ this.constraint.hashCode();
        }

        public String getConstraint() {
            return this.constraint.toString();
        }
    }

    private static class RuleList implements Serializable {
        private static final long serialVersionUID = 1;
        private boolean hasExplicitBoundingInfo;
        private final List<Rule> rules;

        private RuleList() {
            this.hasExplicitBoundingInfo = false;
            this.rules = new ArrayList();
        }

        /* synthetic */ RuleList(AnonymousClass1 x0) {
            this();
        }

        static /* synthetic */ boolean access$276(RuleList x0, int x1) {
            byte b = (byte) (x0.hasExplicitBoundingInfo | x1);
            x0.hasExplicitBoundingInfo = b;
            return b;
        }

        public RuleList addRule(Rule nextRule) {
            String keyword = nextRule.getKeyword();
            for (Rule rule : this.rules) {
                if (keyword.equals(rule.getKeyword())) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Duplicate keyword: ");
                    stringBuilder.append(keyword);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            this.rules.add(nextRule);
            return this;
        }

        public RuleList finish() throws ParseException {
            Rule otherRule = null;
            Iterator<Rule> it = this.rules.iterator();
            while (it.hasNext()) {
                Rule rule = (Rule) it.next();
                if (PluralRules.KEYWORD_OTHER.equals(rule.getKeyword())) {
                    otherRule = rule;
                    it.remove();
                }
            }
            if (otherRule == null) {
                otherRule = PluralRules.parseRule("other:");
            }
            this.rules.add(otherRule);
            return this;
        }

        private Rule selectRule(IFixedDecimal n) {
            for (Rule rule : this.rules) {
                if (rule.appliesTo(n)) {
                    return rule;
                }
            }
            return null;
        }

        public String select(IFixedDecimal n) {
            if (n.isInfinite() || n.isNaN()) {
                return PluralRules.KEYWORD_OTHER;
            }
            return selectRule(n).getKeyword();
        }

        public Set<String> getKeywords() {
            Set<String> result = new LinkedHashSet();
            for (Rule rule : this.rules) {
                result.add(rule.getKeyword());
            }
            return result;
        }

        public boolean isLimited(String keyword, SampleType sampleType) {
            if (!this.hasExplicitBoundingInfo) {
                return computeLimited(keyword, sampleType);
            }
            FixedDecimalSamples mySamples = getDecimalSamples(keyword, sampleType);
            return mySamples == null ? true : mySamples.bounded;
        }

        public boolean computeLimited(String keyword, SampleType sampleType) {
            boolean result = false;
            for (Rule rule : this.rules) {
                if (keyword.equals(rule.getKeyword())) {
                    if (!rule.isLimited(sampleType)) {
                        return false;
                    }
                    result = true;
                }
            }
            return result;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Rule rule : this.rules) {
                if (builder.length() != 0) {
                    builder.append(PluralRules.CATEGORY_SEPARATOR);
                }
                builder.append(rule);
            }
            return builder.toString();
        }

        public String getRules(String keyword) {
            for (Rule rule : this.rules) {
                if (rule.getKeyword().equals(keyword)) {
                    return rule.getConstraint();
                }
            }
            return null;
        }

        public boolean select(IFixedDecimal sample, String keyword) {
            for (Rule rule : this.rules) {
                if (rule.getKeyword().equals(keyword) && rule.appliesTo(sample)) {
                    return true;
                }
            }
            return false;
        }

        public FixedDecimalSamples getDecimalSamples(String keyword, SampleType sampleType) {
            for (Rule rule : this.rules) {
                if (rule.getKeyword().equals(keyword)) {
                    return sampleType == SampleType.INTEGER ? rule.integerSamples : rule.decimalSamples;
                }
            }
            return null;
        }
    }

    @Deprecated
    public enum SampleType {
        INTEGER,
        DECIMAL
    }

    static class SimpleTokenizer {
        static final UnicodeSet BREAK_AND_IGNORE = new UnicodeSet(9, 10, 12, 13, 32, 32).freeze();
        static final UnicodeSet BREAK_AND_KEEP = new UnicodeSet(33, 33, 37, 37, 44, 44, 46, 46, 61, 61).freeze();

        SimpleTokenizer() {
        }

        static String[] split(String source) {
            int last = -1;
            List<String> result = new ArrayList();
            for (int i = 0; i < source.length(); i++) {
                int ch = source.charAt(i);
                if (BREAK_AND_IGNORE.contains(ch)) {
                    if (last >= 0) {
                        result.add(source.substring(last, i));
                        last = -1;
                    }
                } else if (BREAK_AND_KEEP.contains(ch)) {
                    if (last >= 0) {
                        result.add(source.substring(last, i));
                    }
                    result.add(source.substring(i, i + 1));
                    last = -1;
                } else if (last < 0) {
                    last = i;
                }
            }
            if (last >= 0) {
                result.add(source.substring(last));
            }
            return (String[]) result.toArray(new String[result.size()]);
        }
    }

    private static abstract class BinaryConstraint implements Constraint, Serializable {
        private static final long serialVersionUID = 1;
        protected final Constraint a;
        protected final Constraint b;

        protected BinaryConstraint(Constraint a, Constraint b) {
            this.a = a;
            this.b = b;
        }
    }

    @Deprecated
    public static class FixedDecimal extends Number implements Comparable<FixedDecimal>, IFixedDecimal {
        static final long MAX = 1000000000000000000L;
        private static final long MAX_INTEGER_PART = 1000000000;
        private static final long serialVersionUID = -4756200506571685661L;
        private final int baseFactor;
        final long decimalDigits;
        final long decimalDigitsWithoutTrailingZeros;
        final boolean hasIntegerValue;
        final long integerValue;
        final boolean isNegative;
        final double source;
        final int visibleDecimalDigitCount;
        final int visibleDecimalDigitCountWithoutTrailingZeros;

        @Deprecated
        public double getSource() {
            return this.source;
        }

        @Deprecated
        public int getVisibleDecimalDigitCount() {
            return this.visibleDecimalDigitCount;
        }

        @Deprecated
        public int getVisibleDecimalDigitCountWithoutTrailingZeros() {
            return this.visibleDecimalDigitCountWithoutTrailingZeros;
        }

        @Deprecated
        public long getDecimalDigits() {
            return this.decimalDigits;
        }

        @Deprecated
        public long getDecimalDigitsWithoutTrailingZeros() {
            return this.decimalDigitsWithoutTrailingZeros;
        }

        @Deprecated
        public long getIntegerValue() {
            return this.integerValue;
        }

        @Deprecated
        public boolean isHasIntegerValue() {
            return this.hasIntegerValue;
        }

        @Deprecated
        public boolean isNegative() {
            return this.isNegative;
        }

        @Deprecated
        public int getBaseFactor() {
            return this.baseFactor;
        }

        @Deprecated
        public FixedDecimal(double n, int v, long f) {
            long j;
            boolean z = true;
            this.isNegative = n < 0.0d;
            this.source = this.isNegative ? -n : n;
            this.visibleDecimalDigitCount = v;
            this.decimalDigits = f;
            if (n > 1.0E18d) {
                j = MAX;
            } else {
                j = (long) n;
            }
            this.integerValue = j;
            if (this.source != ((double) this.integerValue)) {
                z = false;
            }
            this.hasIntegerValue = z;
            if (f == 0) {
                this.decimalDigitsWithoutTrailingZeros = 0;
                this.visibleDecimalDigitCountWithoutTrailingZeros = 0;
            } else {
                j = f;
                int trimmedCount = v;
                while (j % 10 == 0) {
                    j /= 10;
                    trimmedCount--;
                }
                this.decimalDigitsWithoutTrailingZeros = j;
                this.visibleDecimalDigitCountWithoutTrailingZeros = trimmedCount;
            }
            this.baseFactor = (int) Math.pow(10.0d, (double) v);
        }

        @Deprecated
        public FixedDecimal(double n, int v) {
            this(n, v, (long) getFractionalDigits(n, v));
        }

        private static int getFractionalDigits(double n, int v) {
            if (v == 0) {
                return 0;
            }
            if (n < 0.0d) {
                n = -n;
            }
            int baseFactor = (int) Math.pow(10.0d, (double) v);
            return (int) (Math.round(((double) baseFactor) * n) % ((long) baseFactor));
        }

        @Deprecated
        public FixedDecimal(double n) {
            this(n, decimals(n));
        }

        @Deprecated
        public FixedDecimal(long n) {
            this((double) n, 0);
        }

        @Deprecated
        public static int decimals(double n) {
            if (Double.isInfinite(n) || Double.isNaN(n)) {
                return 0;
            }
            if (n < 0.0d) {
                n = -n;
            }
            if (n == Math.floor(n)) {
                return 0;
            }
            if (n < 1.0E9d) {
                long temp = ((long) (1000000.0d * n)) % 1000000;
                int mask = 10;
                for (int digits = 6; digits > 0; digits--) {
                    if (temp % ((long) mask) != 0) {
                        return digits;
                    }
                    mask *= 10;
                }
                return 0;
            }
            String buf = String.format(Locale.ENGLISH, "%1.15e", new Object[]{Double.valueOf(n)});
            int ePos = buf.lastIndexOf(101);
            int expNumPos = ePos + 1;
            if (buf.charAt(expNumPos) == '+') {
                expNumPos++;
            }
            int numFractionDigits = (ePos - 2) - Integer.parseInt(buf.substring(expNumPos));
            if (numFractionDigits < 0) {
                return 0;
            }
            int i = ePos - 1;
            while (numFractionDigits > 0 && buf.charAt(i) == '0') {
                numFractionDigits--;
                i--;
            }
            return numFractionDigits;
        }

        @Deprecated
        public FixedDecimal(String n) {
            this(Double.parseDouble(n), getVisibleFractionCount(n));
        }

        private static int getVisibleFractionCount(String value) {
            value = value.trim();
            int decimalPos = value.indexOf(46) + 1;
            if (decimalPos == 0) {
                return 0;
            }
            return value.length() - decimalPos;
        }

        @Deprecated
        public double getPluralOperand(Operand operand) {
            switch (operand) {
                case n:
                    return this.source;
                case i:
                    return (double) this.integerValue;
                case f:
                    return (double) this.decimalDigits;
                case t:
                    return (double) this.decimalDigitsWithoutTrailingZeros;
                case v:
                    return (double) this.visibleDecimalDigitCount;
                case w:
                    return (double) this.visibleDecimalDigitCountWithoutTrailingZeros;
                default:
                    return this.source;
            }
        }

        @Deprecated
        public static Operand getOperand(String t) {
            return Operand.valueOf(t);
        }

        @Deprecated
        public int compareTo(FixedDecimal other) {
            int i = 1;
            if (this.integerValue != other.integerValue) {
                if (this.integerValue < other.integerValue) {
                    i = -1;
                }
                return i;
            } else if (this.source != other.source) {
                if (this.source < other.source) {
                    i = -1;
                }
                return i;
            } else if (this.visibleDecimalDigitCount != other.visibleDecimalDigitCount) {
                if (this.visibleDecimalDigitCount < other.visibleDecimalDigitCount) {
                    i = -1;
                }
                return i;
            } else {
                long diff = this.decimalDigits - other.decimalDigits;
                if (diff == 0) {
                    return 0;
                }
                if (diff < 0) {
                    i = -1;
                }
                return i;
            }
        }

        @Deprecated
        public boolean equals(Object arg0) {
            boolean z = false;
            if (arg0 == null) {
                return false;
            }
            if (arg0 == this) {
                return true;
            }
            if (!(arg0 instanceof FixedDecimal)) {
                return false;
            }
            FixedDecimal other = (FixedDecimal) arg0;
            if (this.source == other.source && this.visibleDecimalDigitCount == other.visibleDecimalDigitCount && this.decimalDigits == other.decimalDigits) {
                z = true;
            }
            return z;
        }

        @Deprecated
        public int hashCode() {
            return (int) (this.decimalDigits + ((long) (37 * (this.visibleDecimalDigitCount + ((int) (37.0d * this.source))))));
        }

        @Deprecated
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("%.");
            stringBuilder.append(this.visibleDecimalDigitCount);
            stringBuilder.append("f");
            return String.format(stringBuilder.toString(), new Object[]{Double.valueOf(this.source)});
        }

        @Deprecated
        public boolean hasIntegerValue() {
            return this.hasIntegerValue;
        }

        @Deprecated
        public int intValue() {
            return (int) this.integerValue;
        }

        @Deprecated
        public long longValue() {
            return this.integerValue;
        }

        @Deprecated
        public float floatValue() {
            return (float) this.source;
        }

        @Deprecated
        public double doubleValue() {
            return this.isNegative ? -this.source : this.source;
        }

        @Deprecated
        public long getShiftedValue() {
            return (this.integerValue * ((long) this.baseFactor)) + this.decimalDigits;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new NotSerializableException();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            throw new NotSerializableException();
        }

        @Deprecated
        public boolean isNaN() {
            return Double.isNaN(this.source);
        }

        @Deprecated
        public boolean isInfinite() {
            return Double.isInfinite(this.source);
        }
    }

    private static class RangeConstraint implements Constraint, Serializable {
        private static final long serialVersionUID = 1;
        private final boolean inRange;
        private final boolean integersOnly;
        private final double lowerBound;
        private final int mod;
        private final Operand operand;
        private final long[] range_list;
        private final double upperBound;

        RangeConstraint(int mod, boolean inRange, Operand operand, boolean integersOnly, double lowBound, double highBound, long[] vals) {
            this.mod = mod;
            this.inRange = inRange;
            this.integersOnly = integersOnly;
            this.lowerBound = lowBound;
            this.upperBound = highBound;
            this.range_list = vals;
            this.operand = operand;
        }

        public boolean isFulfilled(IFixedDecimal number) {
            double n = number.getPluralOperand(this.operand);
            if ((this.integersOnly && n - ((double) ((long) n)) != 0.0d) || (this.operand == Operand.j && number.getPluralOperand(Operand.v) != 0.0d)) {
                return this.inRange ^ 1;
            }
            if (this.mod != 0) {
                n %= (double) this.mod;
            }
            boolean z = false;
            boolean test = n >= this.lowerBound && n <= this.upperBound;
            if (test && this.range_list != null) {
                boolean test2 = false;
                int i = 0;
                while (!test2 && i < this.range_list.length) {
                    boolean z2 = n >= ((double) this.range_list[i]) && n <= ((double) this.range_list[i + 1]);
                    test2 = z2;
                    i += 2;
                }
                test = test2;
            }
            if (this.inRange == test) {
                z = true;
            }
            return z;
        }

        public boolean isLimited(SampleType sampleType) {
            boolean z = true;
            boolean valueIsZero = this.lowerBound == this.upperBound && this.lowerBound == 0.0d;
            boolean hasDecimals = (this.operand == Operand.v || this.operand == Operand.w || this.operand == Operand.f || this.operand == Operand.t) && this.inRange != valueIsZero;
            switch (sampleType) {
                case INTEGER:
                    if (!(hasDecimals || ((this.operand == Operand.n || this.operand == Operand.i || this.operand == Operand.j) && this.mod == 0 && this.inRange))) {
                        z = false;
                    }
                    return z;
                case DECIMAL:
                    if (!((!hasDecimals || this.operand == Operand.n || this.operand == Operand.j) && ((this.integersOnly || this.lowerBound == this.upperBound) && this.mod == 0 && this.inRange))) {
                        z = false;
                    }
                    return z;
                default:
                    return false;
            }
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(this.operand);
            if (this.mod != 0) {
                result.append(" % ");
                result.append(this.mod);
            }
            String str = !((this.lowerBound > this.upperBound ? 1 : (this.lowerBound == this.upperBound ? 0 : -1)) != 0) ? this.inRange ? " = " : " != " : this.integersOnly ? this.inRange ? " = " : " != " : this.inRange ? " within " : " not within ";
            result.append(str);
            if (this.range_list != null) {
                int i = 0;
                while (true) {
                    int i2 = i;
                    if (i2 >= this.range_list.length) {
                        break;
                    }
                    PluralRules.addRange(result, (double) this.range_list[i2], (double) this.range_list[i2 + 1], i2 != 0);
                    i = i2 + 2;
                }
            } else {
                PluralRules.addRange(result, this.lowerBound, this.upperBound, false);
            }
            return result.toString();
        }
    }

    private static class AndConstraint extends BinaryConstraint {
        private static final long serialVersionUID = 7766999779862263523L;

        AndConstraint(Constraint a, Constraint b) {
            super(a, b);
        }

        public boolean isFulfilled(IFixedDecimal n) {
            return this.a.isFulfilled(n) && this.b.isFulfilled(n);
        }

        public boolean isLimited(SampleType sampleType) {
            return this.a.isLimited(sampleType) || this.b.isLimited(sampleType);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.a.toString());
            stringBuilder.append(" and ");
            stringBuilder.append(this.b.toString());
            return stringBuilder.toString();
        }
    }

    private static class OrConstraint extends BinaryConstraint {
        private static final long serialVersionUID = 1405488568664762222L;

        OrConstraint(Constraint a, Constraint b) {
            super(a, b);
        }

        public boolean isFulfilled(IFixedDecimal n) {
            return this.a.isFulfilled(n) || this.b.isFulfilled(n);
        }

        public boolean isLimited(SampleType sampleType) {
            return this.a.isLimited(sampleType) && this.b.isLimited(sampleType);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.a.toString());
            stringBuilder.append(" or ");
            stringBuilder.append(this.b.toString());
            return stringBuilder.toString();
        }
    }

    public static PluralRules parseDescription(String description) throws ParseException {
        description = description.trim();
        return description.length() == 0 ? DEFAULT : new PluralRules(parseRuleChain(description));
    }

    public static PluralRules createRules(String description) {
        try {
            return parseDescription(description);
        } catch (Exception e) {
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:122:0x02a8, code skipped:
            r21 = r1;
            r38 = r4;
            r32 = r5;
            r33 = r6;
            r5 = r7;
     */
    /* JADX WARNING: Missing block: B:123:0x02b1, code skipped:
            if (r38 != null) goto L_0x02b6;
     */
    /* JADX WARNING: Missing block: B:124:0x02b3, code skipped:
            r0 = r5;
     */
    /* JADX WARNING: Missing block: B:125:0x02b4, code skipped:
            r4 = r0;
     */
    /* JADX WARNING: Missing block: B:126:0x02b6, code skipped:
            r0 = new android.icu.text.PluralRules.OrConstraint(r38, r5);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Constraint parseConstraint(String description) throws ParseException {
        String[] or_together = OR_SEPARATED.split(description);
        Constraint result = null;
        int i = 0;
        while (true) {
            String[] or_together2;
            int i2;
            String t;
            int i3 = i;
            if (i3 < or_together.length) {
                String[] and_together = AND_SEPARATED.split(or_together[i3]);
                Constraint andConstraint = null;
                i = 0;
                while (true) {
                    int j = i;
                    if (j >= and_together.length) {
                        break;
                    }
                    Constraint newConstraint = NO_CONSTRAINT;
                    String condition = and_together[j].trim();
                    String[] tokens = SimpleTokenizer.split(condition);
                    int mod = 0;
                    boolean inRange = true;
                    boolean integersOnly = true;
                    int x = 0 + 1;
                    or_together2 = or_together;
                    or_together = tokens[0];
                    boolean hackForCompatibility = false;
                    String[] and_together2;
                    int j2;
                    Constraint result2;
                    try {
                        Constraint andConstraint2;
                        Operand operand = FixedDecimal.getOperand(or_together);
                        if (x < tokens.length) {
                            i = x + 1;
                            or_together = tokens[x];
                            if ("mod".equals(or_together) || "%".equals(or_together)) {
                                x = i + 1;
                                mod = Integer.parseInt(tokens[i]);
                                i = x + 1;
                                or_together = nextToken(tokens, x, condition);
                            }
                            boolean z = true;
                            if ("not".equals(or_together)) {
                                inRange = 1 == null;
                                x = i + 1;
                                or_together = nextToken(tokens, i, condition);
                                if ("=".equals(or_together)) {
                                    throw unexpected(or_together, condition);
                                }
                            } else if ("!".equals(or_together)) {
                                inRange = 1 == null;
                                x = i + 1;
                                or_together = nextToken(tokens, i, condition);
                                if (!"=".equals(or_together)) {
                                    throw unexpected(or_together, condition);
                                }
                            } else {
                                x = i;
                            }
                            if ("is".equals(or_together) || "in".equals(or_together) || "=".equals(or_together)) {
                                hackForCompatibility = "is".equals(or_together);
                                if (!hackForCompatibility || inRange) {
                                    i = x + 1;
                                    or_together = nextToken(tokens, x, condition);
                                } else {
                                    throw unexpected(or_together, condition);
                                }
                            } else if ("within".equals(or_together)) {
                                integersOnly = false;
                                i = x + 1;
                                or_together = nextToken(tokens, x, condition);
                            } else {
                                throw unexpected(or_together, condition);
                            }
                            if ("not".equals(or_together)) {
                                if (hackForCompatibility || inRange) {
                                    if (inRange) {
                                        z = false;
                                    }
                                    boolean inRange2 = z;
                                    int x2 = i + 1;
                                    or_together = nextToken(tokens, i, condition);
                                    i = x2;
                                    inRange = inRange2;
                                } else {
                                    throw unexpected(or_together, condition);
                                }
                            }
                            List<Long> valueList = new ArrayList();
                            i2 = i3;
                            and_together2 = and_together;
                            j2 = j;
                            i3 = 9.223372036854776E18d;
                            long highBound = -4332462841530417152L;
                            while (true) {
                                boolean integersOnly2 = integersOnly;
                                integersOnly = Long.parseLong(or_together);
                                long high = integersOnly;
                                String t2 = or_together;
                                if (i < tokens.length) {
                                    or_together = i + 1;
                                    i = nextToken(tokens, i, condition);
                                    if (i.equals(".")) {
                                        int x3 = or_together + 1;
                                        i = nextToken(tokens, or_together, condition);
                                        if (i.equals(".") != null) {
                                            or_together = x3 + 1;
                                            i = nextToken(tokens, x3, condition);
                                            high = Long.parseLong(i);
                                            if (or_together < tokens.length) {
                                                x3 = or_together + 1;
                                                i = nextToken(tokens, or_together, condition);
                                                if (i.equals(",") != null) {
                                                    result2 = result;
                                                    result = x3;
                                                    t = i;
                                                } else {
                                                    throw unexpected(i, condition);
                                                }
                                            }
                                        }
                                        throw unexpected(i, condition);
                                    } else if (!i.equals(",")) {
                                        throw unexpected(i, condition);
                                    }
                                    t = i;
                                    result2 = result;
                                    result = or_together;
                                } else {
                                    result2 = result;
                                    t = t2;
                                    result = i;
                                }
                                long high2 = high;
                                StringBuilder stringBuilder;
                                if (integersOnly <= high2) {
                                    long highBound2;
                                    if (mod != 0) {
                                        andConstraint2 = andConstraint;
                                        highBound2 = highBound;
                                        if (high2 >= ((long) mod)) {
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append(high2);
                                            stringBuilder.append(">mod=");
                                            stringBuilder.append(mod);
                                            throw unexpected(stringBuilder.toString(), condition);
                                        }
                                    }
                                    andConstraint2 = andConstraint;
                                    highBound2 = highBound;
                                    valueList.add(Long.valueOf(integersOnly));
                                    valueList.add(Long.valueOf(high2));
                                    i3 = Math.min(i3, (double) integersOnly);
                                    highBound = Math.max(highBound2, (double) high2);
                                    if (result < tokens.length) {
                                        i = result + 1;
                                        or_together = nextToken(tokens, result, condition);
                                        integersOnly = integersOnly2;
                                        result = result2;
                                        andConstraint = andConstraint2;
                                        t = description;
                                    } else if (t.equals(",") == null) {
                                        if (valueList.size() == 2) {
                                            high2 = null;
                                        } else {
                                            high2 = new long[valueList.size()];
                                            for (or_together = null; or_together < high2.length; or_together++) {
                                                high2[or_together] = ((Long) valueList.get(or_together)).longValue();
                                            }
                                        }
                                        if (i3 == highBound || !hackForCompatibility || inRange) {
                                            RangeConstraint rangeConstraint = new RangeConstraint(mod, inRange, operand, integersOnly2, i3, highBound, high2);
                                            x = result;
                                            long j3 = i3;
                                            long j4 = highBound;
                                        } else {
                                            throw unexpected("is not <range>", condition);
                                        }
                                    } else {
                                        throw unexpected(t, condition);
                                    }
                                }
                                long high3 = high2;
                                andConstraint2 = andConstraint;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(integersOnly);
                                stringBuilder.append("~");
                                stringBuilder.append(high3);
                                throw unexpected(stringBuilder.toString(), condition);
                            }
                        }
                        result2 = result;
                        i2 = i3;
                        and_together2 = and_together;
                        andConstraint2 = andConstraint;
                        j2 = j;
                        String[] strArr = or_together;
                        long[] vals = null;
                        or_together = newConstraint;
                        if (andConstraint2 == null) {
                            andConstraint = or_together;
                        } else {
                            andConstraint = new AndConstraint(andConstraint2, or_together);
                        }
                        i = j2 + 1;
                        or_together = or_together2;
                        i3 = i2;
                        and_together = and_together2;
                        result = result2;
                        t = description;
                    } catch (Exception e) {
                        result2 = result;
                        i2 = i3;
                        and_together2 = and_together;
                        i3 = andConstraint;
                        j2 = j;
                        Constraint constraint = newConstraint;
                        Exception exception = e;
                        throw unexpected(or_together, condition);
                    }
                }
            }
            return result;
            i = i2 + 1;
            or_together = or_together2;
            t = description;
        }
    }

    private static ParseException unexpected(String token, String context) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unexpected token '");
        stringBuilder.append(token);
        stringBuilder.append("' in '");
        stringBuilder.append(context);
        stringBuilder.append("'");
        return new ParseException(stringBuilder.toString(), -1);
    }

    private static String nextToken(String[] tokens, int x, String context) throws ParseException {
        if (x < tokens.length) {
            return tokens[x];
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("missing token at end of '");
        stringBuilder.append(context);
        stringBuilder.append("'");
        throw new ParseException(stringBuilder.toString(), -1);
    }

    private static Rule parseRule(String description) throws ParseException {
        if (description.length() == 0) {
            return DEFAULT_RULE;
        }
        description = description.toLowerCase(Locale.ENGLISH);
        int x = description.indexOf(58);
        if (x != -1) {
            String keyword = description.substring(0, x).trim();
            if (isValidKeyword(keyword)) {
                description = description.substring(x + 1).trim();
                String[] constraintOrSamples = AT_SEPARATED.split(description);
                FixedDecimalSamples integerSamples = null;
                FixedDecimalSamples decimalSamples = null;
                boolean z = true;
                StringBuilder stringBuilder;
                switch (constraintOrSamples.length) {
                    case 1:
                        break;
                    case 2:
                        integerSamples = FixedDecimalSamples.parse(constraintOrSamples[1]);
                        if (integerSamples.sampleType == SampleType.DECIMAL) {
                            decimalSamples = integerSamples;
                            integerSamples = null;
                            break;
                        }
                        break;
                    case 3:
                        integerSamples = FixedDecimalSamples.parse(constraintOrSamples[1]);
                        decimalSamples = FixedDecimalSamples.parse(constraintOrSamples[2]);
                        if (!(integerSamples.sampleType == SampleType.INTEGER && decimalSamples.sampleType == SampleType.DECIMAL)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Must have @integer then @decimal in ");
                            stringBuilder.append(description);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Too many samples in ");
                        stringBuilder.append(description);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
                if (false) {
                    throw new IllegalArgumentException("Ill-formed samples—'@' characters.");
                }
                boolean isOther = keyword.equals(KEYWORD_OTHER);
                if (constraintOrSamples[0].length() != 0) {
                    z = false;
                }
                if (isOther == z) {
                    Constraint constraint;
                    if (isOther) {
                        constraint = NO_CONSTRAINT;
                    } else {
                        constraint = parseConstraint(constraintOrSamples[0]);
                    }
                    return new Rule(keyword, constraint, integerSamples, decimalSamples);
                }
                throw new IllegalArgumentException("The keyword 'other' must have no constraints, just samples.");
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("keyword '");
            stringBuilder2.append(keyword);
            stringBuilder2.append(" is not valid");
            throw new ParseException(stringBuilder2.toString(), 0);
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("missing ':' in rule description '");
        stringBuilder3.append(description);
        stringBuilder3.append("'");
        throw new ParseException(stringBuilder3.toString(), 0);
    }

    private static RuleList parseRuleChain(String description) throws ParseException {
        RuleList result = new RuleList();
        if (description.endsWith(";")) {
            description = description.substring(0, description.length() - 1);
        }
        String[] rules = SEMI_SEPARATED.split(description);
        for (String trim : rules) {
            Rule rule = parseRule(trim.trim());
            int i = (rule.integerSamples == null && rule.decimalSamples == null) ? 0 : 1;
            RuleList.access$276(result, i);
            result.addRule(rule);
        }
        return result.finish();
    }

    private static void addRange(StringBuilder result, double lb, double ub, boolean addSeparator) {
        if (addSeparator) {
            result.append(",");
        }
        if (lb == ub) {
            result.append(format(lb));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(format(lb));
        stringBuilder.append("..");
        stringBuilder.append(format(ub));
        result.append(stringBuilder.toString());
    }

    private static String format(double lb) {
        long lbi = (long) lb;
        return lb == ((double) lbi) ? String.valueOf(lbi) : String.valueOf(lb);
    }

    private boolean addConditional(Set<IFixedDecimal> toAddTo, Set<IFixedDecimal> others, double trial) {
        IFixedDecimal toAdd = new FixedDecimal(trial);
        if (toAddTo.contains(toAdd) || others.contains(toAdd)) {
            return false;
        }
        others.add(toAdd);
        return true;
    }

    public static PluralRules forLocale(ULocale locale) {
        return Factory.getDefaultFactory().forLocale(locale, PluralType.CARDINAL);
    }

    public static PluralRules forLocale(Locale locale) {
        return forLocale(ULocale.forLocale(locale));
    }

    public static PluralRules forLocale(ULocale locale, PluralType type) {
        return Factory.getDefaultFactory().forLocale(locale, type);
    }

    public static PluralRules forLocale(Locale locale, PluralType type) {
        return forLocale(ULocale.forLocale(locale), type);
    }

    private static boolean isValidKeyword(String token) {
        return ALLOWED_ID.containsAll(token);
    }

    private PluralRules(RuleList rules) {
        this.rules = rules;
        this.keywords = Collections.unmodifiableSet(rules.getKeywords());
    }

    @Deprecated
    public int hashCode() {
        return this.rules.hashCode();
    }

    public String select(double number) {
        return this.rules.select(new FixedDecimal(number));
    }

    @Deprecated
    public String select(double number, int countVisibleFractionDigits, long fractionaldigits) {
        return this.rules.select(new FixedDecimal(number, countVisibleFractionDigits, fractionaldigits));
    }

    @Deprecated
    public String select(IFixedDecimal number) {
        return this.rules.select(number);
    }

    @Deprecated
    public boolean matches(FixedDecimal sample, String keyword) {
        return this.rules.select(sample, keyword);
    }

    public Set<String> getKeywords() {
        return this.keywords;
    }

    public double getUniqueKeywordValue(String keyword) {
        Collection<Double> values = getAllKeywordValues(keyword);
        if (values == null || values.size() != 1) {
            return -0.00123456777d;
        }
        return ((Double) values.iterator().next()).doubleValue();
    }

    public Collection<Double> getAllKeywordValues(String keyword) {
        return getAllKeywordValues(keyword, SampleType.INTEGER);
    }

    @Deprecated
    public Collection<Double> getAllKeywordValues(String keyword, SampleType type) {
        Collection<Double> collection = null;
        if (!isLimited(keyword, type)) {
            return null;
        }
        Collection<Double> samples = getSamples(keyword, type);
        if (samples != null) {
            collection = Collections.unmodifiableCollection(samples);
        }
        return collection;
    }

    public Collection<Double> getSamples(String keyword) {
        return getSamples(keyword, SampleType.INTEGER);
    }

    /* JADX WARNING: Missing block: B:17:0x004b, code skipped:
            if (r3 >= 2000) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:19:0x005a, code skipped:
            if (addSample(r11, new android.icu.text.PluralRules.FixedDecimal(((double) r3) / 10.0d, 1), r2, r0) != false) goto L_0x005d;
     */
    /* JADX WARNING: Missing block: B:21:0x0060, code skipped:
            addSample(r11, new android.icu.text.PluralRules.FixedDecimal(1000000.0d, 1), r2, r0);
     */
    /* JADX WARNING: Missing block: B:23:0x0072, code skipped:
            if (r3 >= 200) goto L_0x0082;
     */
    /* JADX WARNING: Missing block: B:25:0x007c, code skipped:
            if (addSample(r11, java.lang.Integer.valueOf(r3), r2, r0) != false) goto L_0x007f;
     */
    /* JADX WARNING: Missing block: B:27:0x0082, code skipped:
            addSample(r11, java.lang.Integer.valueOf(1000000), r2, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Deprecated
    public Collection<Double> getSamples(String keyword, SampleType sampleType) {
        Collection<Double> collection = null;
        if (!this.keywords.contains(keyword)) {
            return null;
        }
        Set<Double> result = new TreeSet();
        if (this.rules.hasExplicitBoundingInfo) {
            Collection<Double> unmodifiableSet;
            FixedDecimalSamples samples = this.rules.getDecimalSamples(keyword, sampleType);
            if (samples == null) {
                unmodifiableSet = Collections.unmodifiableSet(result);
            } else {
                unmodifiableSet = Collections.unmodifiableSet(samples.addSamples(result));
            }
            return unmodifiableSet;
        }
        int maxCount = isLimited(keyword, sampleType) ? Integer.MAX_VALUE : 20;
        int i = 0;
        int i2;
        switch (sampleType) {
            case INTEGER:
                while (true) {
                    i2 = i;
                    i = i2 + 1;
                    break;
                }
            case DECIMAL:
                while (true) {
                    i2 = i;
                    i = i2 + 1;
                    break;
                }
        }
        if (result.size() != 0) {
            collection = Collections.unmodifiableSet(result);
        }
        return collection;
    }

    @Deprecated
    public boolean addSample(String keyword, Number sample, int maxCount, Set<Double> result) {
        if ((sample instanceof FixedDecimal ? select((FixedDecimal) sample) : select(sample.doubleValue())).equals(keyword)) {
            result.add(Double.valueOf(sample.doubleValue()));
            if (maxCount - 1 < 0) {
                return false;
            }
        }
        return true;
    }

    @Deprecated
    public FixedDecimalSamples getDecimalSamples(String keyword, SampleType sampleType) {
        return this.rules.getDecimalSamples(keyword, sampleType);
    }

    public static ULocale[] getAvailableULocales() {
        return Factory.getDefaultFactory().getAvailableULocales();
    }

    public static ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
        return Factory.getDefaultFactory().getFunctionalEquivalent(locale, isAvailable);
    }

    public String toString() {
        return this.rules.toString();
    }

    public boolean equals(Object rhs) {
        return (rhs instanceof PluralRules) && equals((PluralRules) rhs);
    }

    public boolean equals(PluralRules rhs) {
        return rhs != null && toString().equals(rhs.toString());
    }

    public KeywordStatus getKeywordStatus(String keyword, int offset, Set<Double> explicits, Output<Double> uniqueValue) {
        return getKeywordStatus(keyword, offset, explicits, uniqueValue, SampleType.INTEGER);
    }

    @Deprecated
    public KeywordStatus getKeywordStatus(String keyword, int offset, Set<Double> explicits, Output<Double> uniqueValue, SampleType sampleType) {
        if (uniqueValue != null) {
            uniqueValue.value = null;
        }
        if (!this.keywords.contains(keyword)) {
            return KeywordStatus.INVALID;
        }
        if (!isLimited(keyword, sampleType)) {
            return KeywordStatus.UNBOUNDED;
        }
        Collection<Double> values = getSamples(keyword, sampleType);
        int originalSize = values.size();
        if (explicits == null) {
            explicits = Collections.emptySet();
        }
        if (originalSize <= explicits.size()) {
            HashSet<Double> subtractedSet = new HashSet(values);
            for (Double explicit : explicits) {
                subtractedSet.remove(Double.valueOf(explicit.doubleValue() - ((double) offset)));
            }
            if (subtractedSet.size() == 0) {
                return KeywordStatus.SUPPRESSED;
            }
            if (uniqueValue != null && subtractedSet.size() == 1) {
                uniqueValue.value = subtractedSet.iterator().next();
            }
            return originalSize == 1 ? KeywordStatus.UNIQUE : KeywordStatus.BOUNDED;
        } else if (originalSize != 1) {
            return KeywordStatus.BOUNDED;
        } else {
            if (uniqueValue != null) {
                uniqueValue.value = values.iterator().next();
            }
            return KeywordStatus.UNIQUE;
        }
    }

    @Deprecated
    public String getRules(String keyword) {
        return this.rules.getRules(keyword);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        throw new NotSerializableException();
    }

    private Object writeReplace() throws ObjectStreamException {
        return new PluralRulesSerialProxy(toString());
    }

    @Deprecated
    public int compareTo(PluralRules other) {
        return toString().compareTo(other.toString());
    }

    @Deprecated
    public Boolean isLimited(String keyword) {
        return Boolean.valueOf(this.rules.isLimited(keyword, SampleType.INTEGER));
    }

    @Deprecated
    public boolean isLimited(String keyword, SampleType sampleType) {
        return this.rules.isLimited(keyword, sampleType);
    }

    @Deprecated
    public boolean computeLimited(String keyword, SampleType sampleType) {
        return this.rules.computeLimited(keyword, sampleType);
    }
}
