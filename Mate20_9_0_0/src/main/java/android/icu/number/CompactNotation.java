package android.icu.number;

import android.icu.impl.number.CompactData;
import android.icu.impl.number.CompactData.CompactType;
import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.MicroProps;
import android.icu.impl.number.MicroPropsGenerator;
import android.icu.impl.number.MutablePatternModifier;
import android.icu.impl.number.MutablePatternModifier.ImmutablePatternModifier;
import android.icu.impl.number.PatternStringParser;
import android.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import android.icu.text.CompactDecimalFormat.CompactStyle;
import android.icu.text.PluralRules;
import android.icu.util.ULocale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CompactNotation extends Notation {
    final Map<String, Map<String, String>> compactCustomData;
    final CompactStyle compactStyle;

    private static class CompactHandler implements MicroPropsGenerator {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        final CompactData data;
        final MicroPropsGenerator parent;
        final Map<String, CompactModInfo> precomputedMods;
        final PluralRules rules;

        private static class CompactModInfo {
            public ImmutablePatternModifier mod;
            public int numDigits;

            private CompactModInfo() {
            }
        }

        static {
            Class cls = CompactNotation.class;
        }

        private CompactHandler(CompactNotation notation, ULocale locale, String nsName, CompactType compactType, PluralRules rules, MutablePatternModifier buildReference, MicroPropsGenerator parent) {
            this.rules = rules;
            this.parent = parent;
            this.data = new CompactData();
            if (notation.compactStyle != null) {
                this.data.populate(locale, nsName, notation.compactStyle, compactType);
            } else {
                this.data.populate(notation.compactCustomData);
            }
            if (buildReference != null) {
                this.precomputedMods = new HashMap();
                precomputeAllModifiers(buildReference);
                return;
            }
            this.precomputedMods = null;
        }

        private void precomputeAllModifiers(MutablePatternModifier buildReference) {
            Set<String> allPatterns = new HashSet();
            this.data.getUniquePatterns(allPatterns);
            for (String patternString : allPatterns) {
                CompactModInfo info = new CompactModInfo();
                ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(patternString);
                buildReference.setPatternInfo(patternInfo);
                info.mod = buildReference.createImmutable();
                info.numDigits = patternInfo.positive.integerTotal;
                this.precomputedMods.put(patternString, info);
            }
        }

        public MicroProps processQuantity(DecimalQuantity quantity) {
            int magnitude;
            MicroProps micros = this.parent.processQuantity(quantity);
            if (quantity.isZero()) {
                magnitude = 0;
                micros.rounding.apply(quantity);
            } else {
                magnitude = (quantity.isZero() ? 0 : quantity.getMagnitude()) - micros.rounding.chooseMultiplierAndApply(quantity, this.data);
            }
            String patternString = this.data.getPattern(magnitude, quantity.getStandardPlural(this.rules));
            if (patternString != null) {
                int numDigits;
                if (this.precomputedMods != null) {
                    CompactModInfo info = (CompactModInfo) this.precomputedMods.get(patternString);
                    info.mod.applyToMicros(micros, quantity);
                    numDigits = info.numDigits;
                } else {
                    ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(patternString);
                    ((MutablePatternModifier) micros.modMiddle).setPatternInfo(patternInfo);
                    numDigits = patternInfo.positive.integerTotal;
                }
            }
            micros.rounding = Rounder.constructPassThrough();
            return micros;
        }
    }

    CompactNotation(CompactStyle compactStyle) {
        this.compactCustomData = null;
        this.compactStyle = compactStyle;
    }

    CompactNotation(Map<String, Map<String, String>> compactCustomData) {
        this.compactStyle = null;
        this.compactCustomData = compactCustomData;
    }

    MicroPropsGenerator withLocaleData(ULocale locale, String nsName, CompactType compactType, PluralRules rules, MutablePatternModifier buildReference, MicroPropsGenerator parent) {
        return new CompactHandler(locale, nsName, compactType, rules, buildReference, parent);
    }
}
