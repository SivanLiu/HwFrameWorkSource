package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.Utility;
import android.icu.impl.number.Padder;
import android.icu.text.PluralRules.PluralType;
import android.icu.util.ULocale;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.List;

final class NFRule {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final int IMPROPER_FRACTION_RULE = -2;
    static final int INFINITY_RULE = -5;
    static final int MASTER_RULE = -4;
    static final int NAN_RULE = -6;
    static final int NEGATIVE_NUMBER_RULE = -1;
    static final int PROPER_FRACTION_RULE = -3;
    private static final String[] RULE_PREFIXES = new String[]{"<<", "<%", "<#", "<0", ">>", ">%", ">#", ">0", "=%", "=#", "=0"};
    static final Long ZERO = Long.valueOf(0);
    private long baseValue;
    private char decimalPoint = 0;
    private short exponent = (short) 0;
    private final RuleBasedNumberFormat formatter;
    private int radix = 10;
    private PluralFormat rulePatternFormat;
    private String ruleText;
    private NFSubstitution sub1;
    private NFSubstitution sub2;

    public static void makeRules(String description, NFRuleSet owner, NFRule predecessor, RuleBasedNumberFormat ownersOwner, List<NFRule> returnList) {
        NFRuleSet nFRuleSet = owner;
        NFRule nFRule = predecessor;
        RuleBasedNumberFormat ruleBasedNumberFormat = ownersOwner;
        List<NFRule> list = returnList;
        NFRule rule1 = new NFRule(ruleBasedNumberFormat, description);
        String description2 = rule1.ruleText;
        int brack1 = description2.indexOf(91);
        int brack2 = brack1 < 0 ? -1 : description2.indexOf(93);
        if (brack2 < 0 || brack1 > brack2 || rule1.baseValue == -3 || rule1.baseValue == -1 || rule1.baseValue == -5 || rule1.baseValue == -6) {
            rule1.extractSubstitutions(nFRuleSet, description2, nFRule);
        } else {
            NFRule rule2 = null;
            StringBuilder sbuf = new StringBuilder();
            if ((rule1.baseValue > 0 && rule1.baseValue % power((long) rule1.radix, rule1.exponent) == 0) || rule1.baseValue == -2 || rule1.baseValue == -4) {
                rule2 = new NFRule(ruleBasedNumberFormat, null);
                if (rule1.baseValue >= 0) {
                    rule2.baseValue = rule1.baseValue;
                    if (!owner.isFractionSet()) {
                        rule1.baseValue++;
                    }
                } else if (rule1.baseValue == -2) {
                    rule2.baseValue = -3;
                } else if (rule1.baseValue == -4) {
                    rule2.baseValue = rule1.baseValue;
                    rule1.baseValue = -2;
                }
                rule2.radix = rule1.radix;
                rule2.exponent = rule1.exponent;
                sbuf.append(description2.substring(0, brack1));
                if (brack2 + 1 < description2.length()) {
                    sbuf.append(description2.substring(brack2 + 1));
                }
                rule2.extractSubstitutions(nFRuleSet, sbuf.toString(), nFRule);
            }
            sbuf.setLength(0);
            sbuf.append(description2.substring(0, brack1));
            sbuf.append(description2.substring(brack1 + 1, brack2));
            if (brack2 + 1 < description2.length()) {
                sbuf.append(description2.substring(brack2 + 1));
            }
            rule1.extractSubstitutions(nFRuleSet, sbuf.toString(), nFRule);
            if (rule2 != null) {
                if (rule2.baseValue >= 0) {
                    list.add(rule2);
                } else {
                    nFRuleSet.setNonNumericalRule(rule2);
                }
            }
        }
        if (rule1.baseValue >= 0) {
            list.add(rule1);
        } else {
            nFRuleSet.setNonNumericalRule(rule1);
        }
    }

    public NFRule(RuleBasedNumberFormat formatter, String ruleText) {
        String str = null;
        this.ruleText = null;
        this.rulePatternFormat = null;
        this.sub1 = null;
        this.sub2 = null;
        this.formatter = formatter;
        if (ruleText != null) {
            str = parseRuleDescriptor(ruleText);
        }
        this.ruleText = str;
    }

    private String parseRuleDescriptor(String description) {
        String description2 = description;
        int p = description2.indexOf(":");
        if (p != -1) {
            int descriptorLength;
            char firstChar;
            char lastChar;
            char c;
            String descriptor = description2.substring(0, p);
            while (true) {
                p++;
                if (p >= description.length() || !PatternProps.isWhiteSpace(description2.charAt(p))) {
                    description2 = description2.substring(p);
                    descriptorLength = descriptor.length();
                    firstChar = descriptor.charAt(0);
                    lastChar = descriptor.charAt(descriptorLength - 1);
                    c = '0';
                }
            }
            description2 = description2.substring(p);
            descriptorLength = descriptor.length();
            firstChar = descriptor.charAt(0);
            lastChar = descriptor.charAt(descriptorLength - 1);
            c = '0';
            if (firstChar >= '0') {
                char c2 = '9';
                if (firstChar <= '9' && lastChar != ULocale.PRIVATE_USE_EXTENSION) {
                    StringBuilder stringBuilder;
                    long tempValue = 0;
                    char c3 = 0;
                    p = 0;
                    while (p < descriptorLength) {
                        c3 = descriptor.charAt(p);
                        if (c3 < '0' || c3 > '9') {
                            if (c3 == '/' || c3 == '>') {
                                break;
                            } else if (!(PatternProps.isWhiteSpace(c3) || c3 == ',' || c3 == '.')) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Illegal character ");
                                stringBuilder.append(c3);
                                stringBuilder.append(" in rule descriptor");
                                throw new IllegalArgumentException(stringBuilder.toString());
                            }
                        }
                        tempValue = (10 * tempValue) + ((long) (c3 - 48));
                        p++;
                    }
                    setBaseValue(tempValue);
                    if (c3 == '/') {
                        tempValue = 0;
                        p++;
                        while (p < descriptorLength) {
                            c3 = descriptor.charAt(p);
                            if (c3 >= c && c3 <= c2) {
                                tempValue = (tempValue * 10) + ((long) (c3 - 48));
                            } else if (c3 == '>') {
                                break;
                            } else if (!(PatternProps.isWhiteSpace(c3) || c3 == ',' || c3 == '.')) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Illegal character ");
                                stringBuilder.append(c3);
                                stringBuilder.append(" in rule descriptor");
                                throw new IllegalArgumentException(stringBuilder.toString());
                            }
                            p++;
                            c = '0';
                            c2 = '9';
                        }
                        this.radix = (int) tempValue;
                        if (this.radix != 0) {
                            this.exponent = expectedExponent();
                        } else {
                            throw new IllegalArgumentException("Rule can't have radix of 0");
                        }
                    }
                    if (c3 == '>') {
                        for (p = 
/*
Method generation error in method: android.icu.text.NFRule.parseRuleDescriptor(java.lang.String):java.lang.String, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r2_10 'p' int) = (r2_5 'p' int), (r2_8 'p' int) binds: {(r2_5 'p' int)=B:30:0x009a, (r2_8 'p' int)=B:47:0x00ea} in method: android.icu.text.NFRule.parseRuleDescriptor(java.lang.String):java.lang.String, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:185)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 35 more

*/

    private void extractSubstitutions(NFRuleSet owner, String ruleText, NFRule predecessor) {
        this.ruleText = ruleText;
        this.sub1 = extractSubstitution(owner, predecessor);
        if (this.sub1 == null) {
            this.sub2 = null;
        } else {
            this.sub2 = extractSubstitution(owner, predecessor);
        }
        ruleText = this.ruleText;
        int pluralRuleStart = ruleText.indexOf("$(");
        int pluralRuleEnd = pluralRuleStart >= 0 ? ruleText.indexOf(")$", pluralRuleStart) : -1;
        if (pluralRuleEnd >= 0) {
            int endType = ruleText.indexOf(44, pluralRuleStart);
            if (endType >= 0) {
                PluralType pluralType;
                String type = this.ruleText.substring(pluralRuleStart + 2, endType);
                if ("cardinal".equals(type)) {
                    pluralType = PluralType.CARDINAL;
                } else if ("ordinal".equals(type)) {
                    pluralType = PluralType.ORDINAL;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(type);
                    stringBuilder.append(" is an unknown type");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                this.rulePatternFormat = this.formatter.createPluralFormat(pluralType, ruleText.substring(endType + 1, pluralRuleEnd));
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Rule \"");
            stringBuilder2.append(ruleText);
            stringBuilder2.append("\" does not have a defined type");
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x0048  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0047 A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private NFSubstitution extractSubstitution(NFRuleSet owner, NFRule predecessor) {
        int subStart = indexOfAnyRulePrefix(this.ruleText);
        if (subStart == -1) {
            return null;
        }
        int subEnd;
        int subEnd2;
        if (this.ruleText.startsWith(">>>", subStart)) {
            subEnd = subStart + 2;
        } else {
            char c = this.ruleText.charAt(subStart);
            int subEnd3 = this.ruleText.indexOf(c, subStart + 1);
            if (c != '<' || subEnd3 == -1 || subEnd3 >= this.ruleText.length() - 1 || this.ruleText.charAt(subEnd3 + 1) != c) {
                subEnd2 = subEnd3;
                if (subEnd2 != -1) {
                    return null;
                }
                NFSubstitution result = NFSubstitution.makeSubstitution(subStart, this, predecessor, owner, this.formatter, this.ruleText.substring(subStart, subEnd2 + 1));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.ruleText.substring(0, subStart));
                stringBuilder.append(this.ruleText.substring(subEnd2 + 1));
                this.ruleText = stringBuilder.toString();
                return result;
            }
            subEnd = subEnd3 + 1;
        }
        subEnd2 = subEnd;
        if (subEnd2 != -1) {
        }
    }

    final void setBaseValue(long newBaseValue) {
        this.baseValue = newBaseValue;
        this.radix = 10;
        if (this.baseValue >= 1) {
            this.exponent = expectedExponent();
            if (this.sub1 != null) {
                this.sub1.setDivisor(this.radix, this.exponent);
            }
            if (this.sub2 != null) {
                this.sub2.setDivisor(this.radix, this.exponent);
                return;
            }
            return;
        }
        this.exponent = (short) 0;
    }

    private short expectedExponent() {
        if (this.radix == 0 || this.baseValue < 1) {
            return (short) 0;
        }
        short tempResult = (short) ((int) (Math.log((double) this.baseValue) / Math.log((double) this.radix)));
        if (power((long) this.radix, (short) (tempResult + 1)) <= this.baseValue) {
            return (short) (tempResult + 1);
        }
        return tempResult;
    }

    private static int indexOfAnyRulePrefix(String ruleText) {
        int result = -1;
        if (ruleText.length() > 0) {
            for (String string : RULE_PREFIXES) {
                int pos = ruleText.indexOf(string);
                if (pos != -1 && (result == -1 || pos < result)) {
                    result = pos;
                }
            }
        }
        return result;
    }

    public boolean equals(Object that) {
        boolean z = false;
        if (!(that instanceof NFRule)) {
            return false;
        }
        NFRule that2 = (NFRule) that;
        if (this.baseValue == that2.baseValue && this.radix == that2.radix && this.exponent == that2.exponent && this.ruleText.equals(that2.ruleText) && Utility.objectEquals(this.sub1, that2.sub1) && Utility.objectEquals(this.sub2, that2.sub2)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return 42;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        if (this.baseValue == -1) {
            result.append("-x: ");
        } else {
            char c = '.';
            if (this.baseValue == -2) {
                result.append(ULocale.PRIVATE_USE_EXTENSION);
                if (this.decimalPoint != 0) {
                    c = this.decimalPoint;
                }
                result.append(c);
                result.append("x: ");
            } else if (this.baseValue == -3) {
                result.append('0');
                if (this.decimalPoint != 0) {
                    c = this.decimalPoint;
                }
                result.append(c);
                result.append("x: ");
            } else if (this.baseValue == -4) {
                result.append(ULocale.PRIVATE_USE_EXTENSION);
                if (this.decimalPoint != 0) {
                    c = this.decimalPoint;
                }
                result.append(c);
                result.append("0: ");
            } else if (this.baseValue == -5) {
                result.append("Inf: ");
            } else if (this.baseValue == -6) {
                result.append("NaN: ");
            } else {
                result.append(String.valueOf(this.baseValue));
                if (this.radix != 10) {
                    result.append('/');
                    result.append(this.radix);
                }
                int numCarets = expectedExponent() - this.exponent;
                for (int i = 0; i < numCarets; i++) {
                    result.append('>');
                }
                result.append(PluralRules.KEYWORD_RULE_SEPARATOR);
            }
        }
        if (this.ruleText.startsWith(Padder.FALLBACK_PADDING_STRING) && (this.sub1 == null || this.sub1.getPos() != 0)) {
            result.append(PatternTokenizer.SINGLE_QUOTE);
        }
        StringBuilder ruleTextCopy = new StringBuilder(this.ruleText);
        if (this.sub2 != null) {
            ruleTextCopy.insert(this.sub2.getPos(), this.sub2.toString());
        }
        if (this.sub1 != null) {
            ruleTextCopy.insert(this.sub1.getPos(), this.sub1.toString());
        }
        result.append(ruleTextCopy.toString());
        result.append(';');
        return result.toString();
    }

    public final char getDecimalPoint() {
        return this.decimalPoint;
    }

    public final long getBaseValue() {
        return this.baseValue;
    }

    public long getDivisor() {
        return power((long) this.radix, this.exponent);
    }

    public void doFormat(long number, StringBuilder toInsertInto, int pos, int recursionCount) {
        int pluralRuleStart = this.ruleText.length();
        int lengthOffset = 0;
        int i = 0;
        if (this.rulePatternFormat == null) {
            toInsertInto.insert(pos, this.ruleText);
        } else {
            pluralRuleStart = this.ruleText.indexOf("$(");
            int pluralRuleEnd = this.ruleText.indexOf(")$", pluralRuleStart);
            int initialLength = toInsertInto.length();
            if (pluralRuleEnd < this.ruleText.length() - 1) {
                toInsertInto.insert(pos, this.ruleText.substring(pluralRuleEnd + 2));
            }
            toInsertInto.insert(pos, this.rulePatternFormat.format((double) (number / power((long) this.radix, this.exponent))));
            if (pluralRuleStart > 0) {
                toInsertInto.insert(pos, this.ruleText.substring(0, pluralRuleStart));
            }
            lengthOffset = this.ruleText.length() - (toInsertInto.length() - initialLength);
        }
        if (this.sub2 != null) {
            this.sub2.doSubstitution(number, toInsertInto, pos - (this.sub2.getPos() > pluralRuleStart ? lengthOffset : 0), recursionCount);
        }
        if (this.sub1 != null) {
            NFSubstitution nFSubstitution = this.sub1;
            if (this.sub1.getPos() > pluralRuleStart) {
                i = lengthOffset;
            }
            nFSubstitution.doSubstitution(number, toInsertInto, pos - i, recursionCount);
        }
    }

    public void doFormat(double number, StringBuilder toInsertInto, int pos, int recursionCount) {
        int pluralRuleStart = this.ruleText.length();
        int lengthOffset = 0;
        int i = 0;
        if (this.rulePatternFormat == null) {
            toInsertInto.insert(pos, this.ruleText);
        } else {
            pluralRuleStart = this.ruleText.indexOf("$(");
            int pluralRuleEnd = this.ruleText.indexOf(")$", pluralRuleStart);
            int initialLength = toInsertInto.length();
            if (pluralRuleEnd < this.ruleText.length() - 1) {
                toInsertInto.insert(pos, this.ruleText.substring(pluralRuleEnd + 2));
            }
            double pluralVal = number;
            if (0.0d > pluralVal || pluralVal >= 1.0d) {
                pluralVal /= (double) power((long) this.radix, this.exponent);
            } else {
                pluralVal = (double) Math.round(((double) power((long) this.radix, this.exponent)) * pluralVal);
            }
            toInsertInto.insert(pos, this.rulePatternFormat.format((double) ((long) pluralVal)));
            if (pluralRuleStart > 0) {
                toInsertInto.insert(pos, this.ruleText.substring(0, pluralRuleStart));
            }
            lengthOffset = this.ruleText.length() - (toInsertInto.length() - initialLength);
        }
        if (this.sub2 != null) {
            this.sub2.doSubstitution(number, toInsertInto, pos - (this.sub2.getPos() > pluralRuleStart ? lengthOffset : 0), recursionCount);
        }
        if (this.sub1 != null) {
            NFSubstitution nFSubstitution = this.sub1;
            if (this.sub1.getPos() > pluralRuleStart) {
                i = lengthOffset;
            }
            nFSubstitution.doSubstitution(number, toInsertInto, pos - i, recursionCount);
        }
    }

    static long power(long base, short exponent) {
        if (exponent < (short) 0) {
            throw new IllegalArgumentException("Exponent can not be negative");
        } else if (base >= 0) {
            long result = 1;
            while (exponent > (short) 0) {
                if ((exponent & 1) == 1) {
                    result *= base;
                }
                base *= base;
                exponent = (short) (exponent >> 1);
            }
            return result;
        } else {
            throw new IllegalArgumentException("Base can not be negative");
        }
    }

    public boolean shouldRollBack(long number) {
        boolean z = false;
        if ((this.sub1 == null || !this.sub1.isModulusSubstitution()) && (this.sub2 == null || !this.sub2.isModulusSubstitution())) {
            return false;
        }
        long divisor = power((long) this.radix, this.exponent);
        if (number % divisor == 0 && this.baseValue % divisor != 0) {
            z = true;
        }
        return z;
    }

    public Number doParse(String text, ParsePosition parsePosition, boolean isFractionRule, double upperBound) {
        NFRule nFRule = this;
        ParsePosition parsePosition2 = parsePosition;
        int i = 0;
        ParsePosition pp = new ParsePosition(0);
        int sub1Pos = nFRule.sub1 != null ? nFRule.sub1.getPos() : nFRule.ruleText.length();
        int sub2Pos = nFRule.sub2 != null ? nFRule.sub2.getPos() : nFRule.ruleText.length();
        String workText = nFRule.stripPrefix(text, nFRule.ruleText.substring(0, sub1Pos), pp);
        int prefixLength = text.length() - workText.length();
        if (pp.getIndex() == 0 && sub1Pos != 0) {
            return ZERO;
        }
        if (nFRule.baseValue == -5) {
            parsePosition2.setIndex(pp.getIndex());
            return Double.valueOf(Double.POSITIVE_INFINITY);
        } else if (nFRule.baseValue == -6) {
            parsePosition2.setIndex(pp.getIndex());
            return Double.valueOf(Double.NaN);
        } else {
            ParsePosition parsePosition3;
            int highWaterMark = 0;
            double result = 0.0d;
            int start = 0;
            double tempBaseValue = (double) Math.max(0, nFRule.baseValue);
            while (true) {
                int sub1Pos2;
                String workText2;
                int i2;
                NFRule nFRule2;
                int sub2Pos2;
                ParsePosition pp2;
                int start2;
                pp.setIndex(i);
                int highWaterMark2 = highWaterMark;
                String workText3 = workText;
                int sub2Pos3 = sub2Pos;
                double partialResult = nFRule.matchToDelimiter(workText, start, tempBaseValue, nFRule.ruleText.substring(sub1Pos, sub2Pos), nFRule.rulePatternFormat, pp, nFRule.sub1, upperBound).doubleValue();
                if (pp.getIndex() != 0 || nFRule.sub1 == null) {
                    int i3;
                    start = pp.getIndex();
                    String workText22 = workText3.substring(pp.getIndex());
                    ParsePosition pp22 = new ParsePosition(0);
                    sub1Pos2 = sub1Pos;
                    ParsePosition pp3 = pp;
                    workText2 = workText3;
                    i2 = 0;
                    nFRule2 = nFRule;
                    sub2Pos2 = sub2Pos3;
                    parsePosition3 = parsePosition;
                    double result2 = nFRule.matchToDelimiter(workText22, 0, partialResult, nFRule.ruleText.substring(sub2Pos3), nFRule.rulePatternFormat, pp22, nFRule.sub2, upperBound).doubleValue();
                    ParsePosition pp23 = pp22;
                    if (pp23.getIndex() != 0 || nFRule2.sub2 == null) {
                        pp2 = pp3;
                        i3 = highWaterMark2;
                        if ((prefixLength + pp2.getIndex()) + pp23.getIndex() > i3) {
                            result = result2;
                            highWaterMark = (prefixLength + pp2.getIndex()) + pp23.getIndex();
                            start2 = start;
                        }
                    } else {
                        i3 = highWaterMark2;
                        pp2 = pp3;
                    }
                    highWaterMark = i3;
                    start2 = start;
                } else {
                    sub1Pos2 = sub1Pos;
                    pp2 = pp;
                    workText2 = workText3;
                    nFRule2 = nFRule;
                    sub2Pos2 = sub2Pos3;
                    start2 = start;
                    highWaterMark = highWaterMark2;
                    parsePosition3 = parsePosition;
                    i2 = 0;
                }
                int sub1Pos3 = sub1Pos2;
                if (sub1Pos3 != sub2Pos2 && pp2.getIndex() > 0) {
                    String workText4 = workText2;
                    if (pp2.getIndex() >= workText4.length() || pp2.getIndex() == start2) {
                        break;
                    }
                    nFRule = nFRule2;
                    parsePosition2 = parsePosition3;
                    sub2Pos = sub2Pos2;
                    start = start2;
                    sub1Pos = sub1Pos3;
                    pp = pp2;
                    workText = workText4;
                    i = i2;
                    workText4 = text;
                }
            }
            parsePosition3.setIndex(highWaterMark);
            if (isFractionRule && highWaterMark > 0 && nFRule2.sub1 == null) {
                result = 1.0d / result;
            }
            double result3 = result;
            if (result3 == ((double) ((long) result3))) {
                return Long.valueOf((long) result3);
            }
            return new Double(result3);
        }
    }

    private String stripPrefix(String text, String prefix, ParsePosition pp) {
        if (prefix.length() == 0) {
            return text;
        }
        int pfl = prefixLength(text, prefix);
        if (pfl == 0) {
            return text;
        }
        pp.setIndex(pp.getIndex() + pfl);
        return text.substring(pfl);
    }

    private Number matchToDelimiter(String text, int startPos, double baseVal, String delimiter, PluralFormat pluralFormatDelimiter, ParsePosition pp, NFSubstitution sub, double upperBound) {
        String str = text;
        String str2 = delimiter;
        PluralFormat pluralFormat = pluralFormatDelimiter;
        ParsePosition parsePosition = pp;
        if (allIgnorable(str2)) {
            int i = startPos;
            if (sub == null) {
                return Double.valueOf(baseVal);
            }
            ParsePosition tempPP = new ParsePosition(0);
            Number result = ZERO;
            Number tempResult = sub.doParse(str, tempPP, baseVal, upperBound, this.formatter.lenientParseEnabled());
            if (tempPP.getIndex() != 0) {
                parsePosition.setIndex(tempPP.getIndex());
                if (tempResult != null) {
                    result = tempResult;
                }
            }
            return result;
        }
        ParsePosition tempPP2 = new ParsePosition(0);
        int[] temp = findText(str, str2, pluralFormat, startPos);
        int dPos = temp[0];
        int dLen = temp[1];
        while (dPos >= 0) {
            String subText = str.substring(0, dPos);
            if (subText.length() > 0) {
                Number tempResult2 = sub.doParse(subText, tempPP2, baseVal, upperBound, this.formatter.lenientParseEnabled());
                if (tempPP2.getIndex() == dPos) {
                    parsePosition.setIndex(dPos + dLen);
                    return tempResult2;
                }
            }
            tempPP2.setIndex(0);
            temp = findText(str, str2, pluralFormat, dPos + dLen);
            dPos = temp[0];
            dLen = temp[1];
        }
        parsePosition.setIndex(0);
        return ZERO;
    }

    private int prefixLength(String str, String prefix) {
        if (prefix.length() == 0) {
            return 0;
        }
        RbnfLenientScanner scanner = this.formatter.getLenientScanner();
        if (scanner != null) {
            return scanner.prefixLength(str, prefix);
        }
        if (str.startsWith(prefix)) {
            return prefix.length();
        }
        return 0;
    }

    private int[] findText(String str, String key, PluralFormat pluralFormatKey, int startingAt) {
        String str2 = str;
        String str3 = key;
        PluralFormat pluralFormat = pluralFormatKey;
        int i = startingAt;
        RbnfLenientScanner scanner = this.formatter.getLenientScanner();
        if (pluralFormat != null) {
            FieldPosition position = new FieldPosition(0);
            position.setBeginIndex(i);
            pluralFormat.parseType(str2, scanner, position);
            int start = position.getBeginIndex();
            if (start >= 0) {
                int pluralRuleStart = this.ruleText.indexOf("$(");
                int pluralRuleSuffix = this.ruleText.indexOf(")$", pluralRuleStart) + 2;
                int matchLen = position.getEndIndex() - start;
                String prefix = this.ruleText.substring(0, pluralRuleStart);
                String suffix = this.ruleText.substring(pluralRuleSuffix);
                if (str2.regionMatches(start - prefix.length(), prefix, 0, prefix.length()) && str2.regionMatches(start + matchLen, suffix, 0, suffix.length())) {
                    return new int[]{start - prefix.length(), (prefix.length() + matchLen) + suffix.length()};
                }
            }
            return new int[]{-1, 0};
        }
        int i2 = 2;
        if (scanner != null) {
            return scanner.findText(str2, str3, i);
        }
        int[] iArr = new int[i2];
        iArr[0] = str2.indexOf(str3, i);
        iArr[1] = key.length();
        return iArr;
    }

    private boolean allIgnorable(String str) {
        boolean z = true;
        if (str == null || str.length() == 0) {
            return true;
        }
        RbnfLenientScanner scanner = this.formatter.getLenientScanner();
        if (scanner == null || !scanner.allIgnorable(str)) {
            z = false;
        }
        return z;
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        if (this.sub1 != null) {
            this.sub1.setDecimalFormatSymbols(newSymbols);
        }
        if (this.sub2 != null) {
            this.sub2.setDecimalFormatSymbols(newSymbols);
        }
    }
}
