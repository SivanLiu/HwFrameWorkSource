package android.icu.impl;

import android.icu.text.CurrencyDisplayNames;
import android.icu.util.ULocale;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Map;

public class CurrencyData {
    public static final CurrencyDisplayInfoProvider provider;

    public interface CurrencyDisplayInfoProvider {
        CurrencyDisplayInfo getInstance(ULocale uLocale, boolean z);

        boolean hasData();
    }

    public static final class CurrencyFormatInfo {
        public final String currencyPattern;
        public final String isoCode;
        public final String monetaryDecimalSeparator;
        public final String monetaryGroupingSeparator;

        public CurrencyFormatInfo(String isoCode, String currencyPattern, String monetarySeparator, String monetaryGroupingSeparator) {
            this.isoCode = isoCode;
            this.currencyPattern = currencyPattern;
            this.monetaryDecimalSeparator = monetarySeparator;
            this.monetaryGroupingSeparator = monetaryGroupingSeparator;
        }
    }

    public static final class CurrencySpacingInfo {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        public static final CurrencySpacingInfo DEFAULT = new CurrencySpacingInfo(DEFAULT_CUR_MATCH, DEFAULT_CTX_MATCH, " ", DEFAULT_CUR_MATCH, DEFAULT_CTX_MATCH, " ");
        private static final String DEFAULT_CTX_MATCH = "[:digit:]";
        private static final String DEFAULT_CUR_MATCH = "[:letter:]";
        private static final String DEFAULT_INSERT = " ";
        public boolean hasAfterCurrency = false;
        public boolean hasBeforeCurrency = false;
        private final String[][] symbols = ((String[][]) Array.newInstance(String.class, new int[]{SpacingType.COUNT.ordinal(), SpacingPattern.COUNT.ordinal()}));

        public enum SpacingPattern {
            CURRENCY_MATCH(0),
            SURROUNDING_MATCH(1),
            INSERT_BETWEEN(2),
            COUNT;

            private SpacingPattern(int value) {
            }
        }

        public enum SpacingType {
            BEFORE,
            AFTER,
            COUNT
        }

        static {
            Class cls = CurrencyData.class;
        }

        public CurrencySpacingInfo(String... strings) {
            int k = 0;
            int i = 0;
            while (i < SpacingType.COUNT.ordinal()) {
                int k2 = k;
                for (k = 0; k < SpacingPattern.COUNT.ordinal(); k++) {
                    this.symbols[i][k] = strings[k2];
                    k2++;
                }
                i++;
                k = k2;
            }
        }

        public void setSymbolIfNull(SpacingType type, SpacingPattern pattern, String value) {
            int i = type.ordinal();
            int j = pattern.ordinal();
            if (this.symbols[i][j] == null) {
                this.symbols[i][j] = value;
            }
        }

        public String[] getBeforeSymbols() {
            return this.symbols[SpacingType.BEFORE.ordinal()];
        }

        public String[] getAfterSymbols() {
            return this.symbols[SpacingType.AFTER.ordinal()];
        }
    }

    public static abstract class CurrencyDisplayInfo extends CurrencyDisplayNames {
        public abstract CurrencyFormatInfo getFormatInfo(String str);

        public abstract String getNarrowSymbol(String str);

        public abstract CurrencySpacingInfo getSpacingInfo();

        public abstract Map<String, String> getUnitPatterns();
    }

    public static class DefaultInfo extends CurrencyDisplayInfo {
        private static final CurrencyDisplayInfo FALLBACK_INSTANCE = new DefaultInfo(true);
        private static final CurrencyDisplayInfo NO_FALLBACK_INSTANCE = new DefaultInfo(false);
        private final boolean fallback;

        private DefaultInfo(boolean fallback) {
            this.fallback = fallback;
        }

        public static final CurrencyDisplayInfo getWithFallback(boolean fallback) {
            return fallback ? FALLBACK_INSTANCE : NO_FALLBACK_INSTANCE;
        }

        public String getName(String isoCode) {
            return this.fallback ? isoCode : null;
        }

        public String getPluralName(String isoCode, String pluralType) {
            return this.fallback ? isoCode : null;
        }

        public String getSymbol(String isoCode) {
            return this.fallback ? isoCode : null;
        }

        public String getNarrowSymbol(String isoCode) {
            return this.fallback ? isoCode : null;
        }

        public Map<String, String> symbolMap() {
            return Collections.emptyMap();
        }

        public Map<String, String> nameMap() {
            return Collections.emptyMap();
        }

        public ULocale getULocale() {
            return ULocale.ROOT;
        }

        public Map<String, String> getUnitPatterns() {
            if (this.fallback) {
                return Collections.emptyMap();
            }
            return null;
        }

        public CurrencyFormatInfo getFormatInfo(String isoCode) {
            return null;
        }

        public CurrencySpacingInfo getSpacingInfo() {
            return this.fallback ? CurrencySpacingInfo.DEFAULT : null;
        }
    }

    private CurrencyData() {
    }

    static {
        CurrencyDisplayInfoProvider temp;
        try {
            temp = (CurrencyDisplayInfoProvider) Class.forName("android.icu.impl.ICUCurrencyDisplayInfoProvider").newInstance();
        } catch (Throwable th) {
            temp = new CurrencyDisplayInfoProvider() {
                public CurrencyDisplayInfo getInstance(ULocale locale, boolean withFallback) {
                    return DefaultInfo.getWithFallback(withFallback);
                }

                public boolean hasData() {
                    return false;
                }
            };
        }
        provider = temp;
    }
}
