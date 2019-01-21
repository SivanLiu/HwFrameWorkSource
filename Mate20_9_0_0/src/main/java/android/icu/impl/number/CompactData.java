package android.icu.impl.number;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.StandardPlural;
import android.icu.impl.UResource.Key;
import android.icu.impl.UResource.Sink;
import android.icu.impl.UResource.Table;
import android.icu.impl.UResource.Value;
import android.icu.text.CompactDecimalFormat.CompactStyle;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CompactData implements MultiplierProducer {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int COMPACT_MAX_DIGITS = 15;
    private static final String USE_FALLBACK = "<USE FALLBACK>";
    private boolean isEmpty = true;
    private byte largestMagnitude = (byte) 0;
    private final byte[] multipliers = new byte[16];
    private final String[] patterns = new String[(StandardPlural.COUNT * 16)];

    public enum CompactType {
        DECIMAL,
        CURRENCY
    }

    private static final class CompactDataSink extends Sink {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        CompactData data;

        static {
            Class cls = CompactData.class;
        }

        public CompactDataSink(CompactData data) {
            this.data = data;
        }

        public void put(Key key, Value value, boolean isRoot) {
            Table powersOfTenTable = value.getTable();
            for (int i3 = 0; powersOfTenTable.getKeyAndValue(i3, key, value); i3++) {
                byte magnitude = (byte) (key.length() - 1);
                byte multiplier = this.data.multipliers[magnitude];
                Table pluralVariantsTable = value.getTable();
                byte multiplier2 = multiplier;
                for (int i4 = 0; pluralVariantsTable.getKeyAndValue(i4, key, value); i4++) {
                    StandardPlural plural = StandardPlural.fromString(key.toString());
                    if (this.data.patterns[CompactData.getIndex(magnitude, plural)] == null) {
                        String patternString = value.toString();
                        if (patternString.equals(AndroidHardcodedSystemProperties.JAVA_VERSION)) {
                            patternString = CompactData.USE_FALLBACK;
                        }
                        this.data.patterns[CompactData.getIndex(magnitude, plural)] = patternString;
                        if (multiplier2 == (byte) 0) {
                            int numZeros = CompactData.countZeros(patternString);
                            if (numZeros > 0) {
                                multiplier2 = (byte) ((numZeros - magnitude) - 1);
                            }
                        }
                    }
                }
                if (this.data.multipliers[magnitude] == (byte) 0) {
                    this.data.multipliers[magnitude] = multiplier2;
                    if (magnitude > this.data.largestMagnitude) {
                        this.data.largestMagnitude = magnitude;
                    }
                    this.data.isEmpty = false;
                }
            }
        }
    }

    public void populate(ULocale locale, String nsName, CompactStyle compactStyle, CompactType compactType) {
        CompactDataSink sink = new CompactDataSink(this);
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale);
        boolean nsIsLatn = nsName.equals("latn");
        boolean compactIsShort = compactStyle == CompactStyle.SHORT;
        StringBuilder resourceKey = new StringBuilder();
        getResourceBundleKey(nsName, compactStyle, compactType, resourceKey);
        rb.getAllItemsWithFallbackNoFail(resourceKey.toString(), sink);
        if (this.isEmpty && !nsIsLatn) {
            getResourceBundleKey("latn", compactStyle, compactType, resourceKey);
            rb.getAllItemsWithFallbackNoFail(resourceKey.toString(), sink);
        }
        if (this.isEmpty && !compactIsShort) {
            getResourceBundleKey(nsName, CompactStyle.SHORT, compactType, resourceKey);
            rb.getAllItemsWithFallbackNoFail(resourceKey.toString(), sink);
        }
        if (!(!this.isEmpty || nsIsLatn || compactIsShort)) {
            getResourceBundleKey("latn", CompactStyle.SHORT, compactType, resourceKey);
            rb.getAllItemsWithFallbackNoFail(resourceKey.toString(), sink);
        }
        if (this.isEmpty) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not load compact decimal data for locale ");
            stringBuilder.append(locale);
            throw new ICUException(stringBuilder.toString());
        }
    }

    private static void getResourceBundleKey(String nsName, CompactStyle compactStyle, CompactType compactType, StringBuilder sb) {
        sb.setLength(0);
        sb.append("NumberElements/");
        sb.append(nsName);
        sb.append(compactStyle == CompactStyle.SHORT ? "/patternsShort" : "/patternsLong");
        sb.append(compactType == CompactType.DECIMAL ? "/decimalFormat" : "/currencyFormat");
    }

    public void populate(Map<String, Map<String, String>> powersToPluralsToPatterns) {
        for (Entry<String, Map<String, String>> magnitudeEntry : powersToPluralsToPatterns.entrySet()) {
            byte magnitude = (byte) (((String) magnitudeEntry.getKey()).length() - 1);
            for (Entry<String, String> pluralEntry : ((Map) magnitudeEntry.getValue()).entrySet()) {
                StandardPlural plural = StandardPlural.fromString(((String) pluralEntry.getKey()).toString());
                String patternString = ((String) pluralEntry.getValue()).toString();
                this.patterns[getIndex(magnitude, plural)] = patternString;
                int numZeros = countZeros(patternString);
                if (numZeros > 0) {
                    this.multipliers[magnitude] = (byte) ((numZeros - magnitude) - 1);
                    if (magnitude > this.largestMagnitude) {
                        this.largestMagnitude = magnitude;
                    }
                    this.isEmpty = false;
                }
            }
        }
    }

    public int getMultiplier(int magnitude) {
        if (magnitude < 0) {
            return 0;
        }
        if (magnitude > this.largestMagnitude) {
            magnitude = this.largestMagnitude;
        }
        return this.multipliers[magnitude];
    }

    public String getPattern(int magnitude, StandardPlural plural) {
        if (magnitude < 0) {
            return null;
        }
        if (magnitude > this.largestMagnitude) {
            magnitude = this.largestMagnitude;
        }
        String patternString = this.patterns[getIndex(magnitude, plural)];
        if (patternString == null && plural != StandardPlural.OTHER) {
            patternString = this.patterns[getIndex(magnitude, StandardPlural.OTHER)];
        }
        if (patternString == USE_FALLBACK) {
            patternString = null;
        }
        return patternString;
    }

    public void getUniquePatterns(Set<String> output) {
        output.addAll(Arrays.asList(this.patterns));
        output.remove(USE_FALLBACK);
        output.remove(null);
    }

    private static final int getIndex(int magnitude, StandardPlural plural) {
        return (StandardPlural.COUNT * magnitude) + plural.ordinal();
    }

    private static final int countZeros(String patternString) {
        int numZeros = 0;
        for (int i = 0; i < patternString.length(); i++) {
            if (patternString.charAt(i) == '0') {
                numZeros++;
            } else if (numZeros > 0) {
                break;
            }
        }
        return numZeros;
    }
}
