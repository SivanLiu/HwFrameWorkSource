package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

final class NFRuleSet {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final int IMPROPER_FRACTION_RULE_INDEX = 1;
    static final int INFINITY_RULE_INDEX = 4;
    static final int MASTER_RULE_INDEX = 3;
    static final int NAN_RULE_INDEX = 5;
    static final int NEGATIVE_RULE_INDEX = 0;
    static final int PROPER_FRACTION_RULE_INDEX = 2;
    private static final int RECURSION_LIMIT = 64;
    LinkedList<NFRule> fractionRules;
    private boolean isFractionRuleSet = false;
    private final boolean isParseable;
    private final String name;
    final NFRule[] nonNumericalRules = new NFRule[6];
    final RuleBasedNumberFormat owner;
    private NFRule[] rules;

    public NFRuleSet(RuleBasedNumberFormat owner, String[] descriptions, int index) throws IllegalArgumentException {
        this.owner = owner;
        String description = descriptions[index];
        if (description.length() != 0) {
            if (description.charAt(0) == '%') {
                int pos = description.indexOf(58);
                if (pos != -1) {
                    String name = description.substring(0, pos);
                    this.isParseable = 1 ^ name.endsWith("@noparse");
                    if (!this.isParseable) {
                        name = name.substring(0, name.length() - 8);
                    }
                    this.name = name;
                    while (pos < description.length()) {
                        pos++;
                        if (!PatternProps.isWhiteSpace(description.charAt(pos))) {
                            break;
                        }
                    }
                    description = description.substring(pos);
                    descriptions[index] = description;
                } else {
                    throw new IllegalArgumentException("Rule set name doesn't end in colon");
                }
            }
            this.name = "%default";
            this.isParseable = true;
            if (description.length() == 0) {
                throw new IllegalArgumentException("Empty rule set description");
            }
            return;
        }
        throw new IllegalArgumentException("Empty rule set description");
    }

    public void parseRules(String description) {
        List<NFRule> tempRules = new ArrayList();
        NFRule predecessor = null;
        int oldP = 0;
        int descriptionLen = description.length();
        do {
            int p = description.indexOf(59, oldP);
            if (p < 0) {
                p = descriptionLen;
            }
            NFRule.makeRules(description.substring(oldP, p), this, predecessor, this.owner, tempRules);
            if (!tempRules.isEmpty()) {
                predecessor = (NFRule) tempRules.get(tempRules.size() - 1);
            }
            oldP = p + 1;
        } while (oldP < descriptionLen);
        long defaultBaseValue = 0;
        for (NFRule rule : tempRules) {
            long baseValue = rule.getBaseValue();
            if (baseValue == 0) {
                rule.setBaseValue(defaultBaseValue);
            } else if (baseValue >= defaultBaseValue) {
                defaultBaseValue = baseValue;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Rules are not in order, base: ");
                stringBuilder.append(baseValue);
                stringBuilder.append(" < ");
                stringBuilder.append(defaultBaseValue);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            if (!this.isFractionRuleSet) {
                defaultBaseValue++;
            }
        }
        this.rules = new NFRule[tempRules.size()];
        tempRules.toArray(this.rules);
    }

    void setNonNumericalRule(NFRule rule) {
        long baseValue = rule.getBaseValue();
        if (baseValue == -1) {
            this.nonNumericalRules[0] = rule;
        } else if (baseValue == -2) {
            setBestFractionRule(1, rule, true);
        } else if (baseValue == -3) {
            setBestFractionRule(2, rule, true);
        } else if (baseValue == -4) {
            setBestFractionRule(3, rule, true);
        } else if (baseValue == -5) {
            this.nonNumericalRules[4] = rule;
        } else if (baseValue == -6) {
            this.nonNumericalRules[5] = rule;
        }
    }

    private void setBestFractionRule(int originalIndex, NFRule newRule, boolean rememberRule) {
        if (rememberRule) {
            if (this.fractionRules == null) {
                this.fractionRules = new LinkedList();
            }
            this.fractionRules.add(newRule);
        }
        if (this.nonNumericalRules[originalIndex] == null) {
            this.nonNumericalRules[originalIndex] = newRule;
        } else if (this.owner.getDecimalFormatSymbols().getDecimalSeparator() == newRule.getDecimalPoint()) {
            this.nonNumericalRules[originalIndex] = newRule;
        }
    }

    public void makeIntoFractionRuleSet() {
        this.isFractionRuleSet = true;
    }

    public boolean equals(Object that) {
        if (!(that instanceof NFRuleSet)) {
            return false;
        }
        NFRuleSet that2 = (NFRuleSet) that;
        if (!this.name.equals(that2.name) || this.rules.length != that2.rules.length || this.isFractionRuleSet != that2.isFractionRuleSet) {
            return false;
        }
        int i;
        for (i = 0; i < this.nonNumericalRules.length; i++) {
            if (!Utility.objectEquals(this.nonNumericalRules[i], that2.nonNumericalRules[i])) {
                return false;
            }
        }
        for (i = 0; i < this.rules.length; i++) {
            if (!this.rules[i].equals(that2.rules[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return 42;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(this.name);
        result.append(":\n");
        int i = 0;
        for (NFRule rule : this.rules) {
            result.append(rule.toString());
            result.append("\n");
        }
        NFRule[] nFRuleArr = this.nonNumericalRules;
        int length = nFRuleArr.length;
        while (i < length) {
            NFRule rule2 = nFRuleArr[i];
            if (rule2 != null) {
                if (rule2.getBaseValue() == -2 || rule2.getBaseValue() == -3 || rule2.getBaseValue() == -4) {
                    Iterator it = this.fractionRules.iterator();
                    while (it.hasNext()) {
                        NFRule fractionRule = (NFRule) it.next();
                        if (fractionRule.getBaseValue() == rule2.getBaseValue()) {
                            result.append(fractionRule.toString());
                            result.append("\n");
                        }
                    }
                } else {
                    result.append(rule2.toString());
                    result.append("\n");
                }
            }
            i++;
        }
        return result.toString();
    }

    public boolean isFractionSet() {
        return this.isFractionRuleSet;
    }

    public String getName() {
        return this.name;
    }

    public boolean isPublic() {
        return this.name.startsWith("%%") ^ 1;
    }

    public boolean isParseable() {
        return this.isParseable;
    }

    public void format(long number, StringBuilder toInsertInto, int pos, int recursionCount) {
        if (recursionCount < 64) {
            findNormalRule(number).doFormat(number, toInsertInto, pos, recursionCount + 1);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Recursion limit exceeded when applying ruleSet ");
        stringBuilder.append(this.name);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void format(double number, StringBuilder toInsertInto, int pos, int recursionCount) {
        if (recursionCount < 64) {
            findRule(number).doFormat(number, toInsertInto, pos, recursionCount + 1);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Recursion limit exceeded when applying ruleSet ");
        stringBuilder.append(this.name);
        throw new IllegalStateException(stringBuilder.toString());
    }

    NFRule findRule(double number) {
        if (this.isFractionRuleSet) {
            return findFractionRuleSetRule(number);
        }
        NFRule rule;
        if (Double.isNaN(number)) {
            rule = this.nonNumericalRules[5];
            if (rule == null) {
                rule = this.owner.getDefaultNaNRule();
            }
            return rule;
        }
        if (number < 0.0d) {
            if (this.nonNumericalRules[0] != null) {
                return this.nonNumericalRules[0];
            }
            number = -number;
        }
        if (Double.isInfinite(number)) {
            rule = this.nonNumericalRules[4];
            if (rule == null) {
                rule = this.owner.getDefaultInfinityRule();
            }
            return rule;
        }
        if (number != Math.floor(number)) {
            if (number < 1.0d && this.nonNumericalRules[2] != null) {
                return this.nonNumericalRules[2];
            }
            if (this.nonNumericalRules[1] != null) {
                return this.nonNumericalRules[1];
            }
        }
        if (this.nonNumericalRules[3] != null) {
            return this.nonNumericalRules[3];
        }
        return findNormalRule(Math.round(number));
    }

    private NFRule findNormalRule(long number) {
        if (this.isFractionRuleSet) {
            return findFractionRuleSetRule((double) number);
        }
        if (number < 0) {
            if (this.nonNumericalRules[0] != null) {
                return this.nonNumericalRules[0];
            }
            number = -number;
        }
        int lo = 0;
        int hi = this.rules.length;
        if (hi <= 0) {
            return this.nonNumericalRules[3];
        }
        while (lo < hi) {
            int lo2 = (lo + hi) >>> 1;
            long ruleBaseValue = this.rules[lo2].getBaseValue();
            if (ruleBaseValue == number) {
                return this.rules[lo2];
            }
            if (ruleBaseValue > number) {
                hi = lo2;
            } else {
                lo = lo2 + 1;
            }
        }
        if (hi != 0) {
            NFRule result = this.rules[hi - 1];
            if (result.shouldRollBack(number)) {
                if (hi != 1) {
                    result = this.rules[hi - 2];
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("The rule set ");
                    stringBuilder.append(this.name);
                    stringBuilder.append(" cannot roll back from the rule '");
                    stringBuilder.append(result);
                    stringBuilder.append("'");
                    throw new IllegalStateException(stringBuilder.toString());
                }
            }
            return result;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("The rule set ");
        stringBuilder2.append(this.name);
        stringBuilder2.append(" cannot format the value ");
        stringBuilder2.append(number);
        throw new IllegalStateException(stringBuilder2.toString());
    }

    private NFRule findFractionRuleSetRule(double number) {
        int i;
        int i2 = 0;
        long leastCommonMultiple = this.rules[0].getBaseValue();
        for (i = 1; i < this.rules.length; i++) {
            leastCommonMultiple = lcm(leastCommonMultiple, this.rules[i].getBaseValue());
        }
        long numerator = Math.round(((double) leastCommonMultiple) * number);
        long difference = Long.MAX_VALUE;
        i = 0;
        while (i2 < this.rules.length) {
            long tempDifference = (this.rules[i2].getBaseValue() * numerator) % leastCommonMultiple;
            if (leastCommonMultiple - tempDifference < tempDifference) {
                tempDifference = leastCommonMultiple - tempDifference;
            }
            if (tempDifference < difference) {
                difference = tempDifference;
                i = i2;
                if (difference == 0) {
                    break;
                }
            }
            i2++;
        }
        if (i + 1 < this.rules.length && this.rules[i + 1].getBaseValue() == this.rules[i].getBaseValue() && (Math.round(((double) this.rules[i].getBaseValue()) * number) < 1 || Math.round(((double) this.rules[i].getBaseValue()) * number) >= 2)) {
            i++;
        }
        return this.rules[i];
    }

    private static long lcm(long x, long y) {
        long t;
        long x1 = x;
        long y1 = y;
        int p2 = 0;
        while ((x1 & 1) == 0 && (y1 & 1) == 0) {
            p2++;
            x1 >>= 1;
            y1 >>= 1;
        }
        if ((x1 & 1) == 1) {
            t = -y1;
        } else {
            t = x1;
        }
        while (t != 0) {
            while ((t & 1) == 0) {
                t >>= 1;
            }
            if (t > 0) {
                x1 = t;
            } else {
                y1 = -t;
            }
            t = x1 - y1;
        }
        return (x / (x1 << p2)) * y;
    }

    public Number parse(String text, ParsePosition parsePosition, double upperBound) {
        ParsePosition parsePosition2 = parsePosition;
        ParsePosition highWaterMark = new ParsePosition(0);
        Number result = NFRule.ZERO;
        if (text.length() == 0) {
            return result;
        }
        Number result2 = result;
        for (NFRule fractionRule : this.nonNumericalRules) {
            if (fractionRule != null) {
                result = fractionRule.doParse(text, parsePosition2, false, upperBound);
                if (parsePosition.getIndex() > highWaterMark.getIndex()) {
                    result2 = result;
                    highWaterMark.setIndex(parsePosition.getIndex());
                }
                parsePosition2.setIndex(0);
            }
        }
        int i = this.rules.length - 1;
        while (true) {
            int i2 = i;
            if (i2 < 0 || highWaterMark.getIndex() >= text.length()) {
                parsePosition2.setIndex(highWaterMark.getIndex());
            } else {
                if (this.isFractionRuleSet || ((double) this.rules[i2].getBaseValue()) < upperBound) {
                    result = this.rules[i2].doParse(text, parsePosition2, this.isFractionRuleSet, upperBound);
                    if (parsePosition.getIndex() > highWaterMark.getIndex()) {
                        result2 = result;
                        highWaterMark.setIndex(parsePosition.getIndex());
                    }
                    parsePosition2.setIndex(0);
                }
                i = i2 - 1;
            }
        }
        parsePosition2.setIndex(highWaterMark.getIndex());
        return result2;
    }

    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        NFRule rule;
        int i = 0;
        for (NFRule rule2 : this.rules) {
            rule2.setDecimalFormatSymbols(newSymbols);
        }
        if (this.fractionRules != null) {
            for (int nonNumericalIdx = 1; nonNumericalIdx <= 3; nonNumericalIdx++) {
                if (this.nonNumericalRules[nonNumericalIdx] != null) {
                    Iterator it = this.fractionRules.iterator();
                    while (it.hasNext()) {
                        rule = (NFRule) it.next();
                        if (this.nonNumericalRules[nonNumericalIdx].getBaseValue() == rule.getBaseValue()) {
                            setBestFractionRule(nonNumericalIdx, rule, false);
                        }
                    }
                }
            }
        }
        NFRule[] nFRuleArr = this.nonNumericalRules;
        int length = nFRuleArr.length;
        while (i < length) {
            rule = nFRuleArr[i];
            if (rule != null) {
                rule.setDecimalFormatSymbols(newSymbols);
            }
            i++;
        }
    }
}
