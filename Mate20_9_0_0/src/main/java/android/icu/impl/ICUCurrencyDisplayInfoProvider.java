package android.icu.impl;

import android.icu.impl.CurrencyData.CurrencyDisplayInfo;
import android.icu.impl.CurrencyData.CurrencyDisplayInfoProvider;
import android.icu.impl.CurrencyData.CurrencyFormatInfo;
import android.icu.impl.CurrencyData.CurrencySpacingInfo;
import android.icu.impl.CurrencyData.CurrencySpacingInfo.SpacingPattern;
import android.icu.impl.CurrencyData.CurrencySpacingInfo.SpacingType;
import android.icu.impl.ICUResourceBundle.OpenType;
import android.icu.impl.UResource.Array;
import android.icu.impl.UResource.Key;
import android.icu.impl.UResource.Sink;
import android.icu.impl.UResource.Table;
import android.icu.impl.UResource.Value;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

public class ICUCurrencyDisplayInfoProvider implements CurrencyDisplayInfoProvider {
    private volatile ICUCurrencyDisplayInfo currencyDisplayInfoCache = null;

    static class ICUCurrencyDisplayInfo extends CurrencyDisplayInfo {
        final boolean fallback;
        private volatile FormattingData formattingDataCache = null;
        final ULocale locale;
        private volatile NarrowSymbol narrowSymbolCache = null;
        private volatile SoftReference<ParsingData> parsingDataCache = new SoftReference(null);
        private volatile String[] pluralsDataCache = null;
        private final ICUResourceBundle rb;
        private volatile CurrencySpacingInfo spacingInfoCache = null;
        private volatile Map<String, String> unitPatternsCache = null;

        static class FormattingData {
            String displayName = null;
            CurrencyFormatInfo formatInfo = null;
            final String isoCode;
            String symbol = null;

            FormattingData(String isoCode) {
                this.isoCode = isoCode;
            }
        }

        static class NarrowSymbol {
            final String isoCode;
            String narrowSymbol = null;

            NarrowSymbol(String isoCode) {
                this.isoCode = isoCode;
            }
        }

        static class ParsingData {
            Map<String, String> nameToIsoCode = new HashMap();
            Map<String, String> symbolToIsoCode = new HashMap();

            ParsingData() {
            }
        }

        private static final class CurrencySink extends Sink {
            static final /* synthetic */ boolean $assertionsDisabled = false;
            final EntrypointTable entrypointTable;
            FormattingData formattingData = null;
            NarrowSymbol narrowSymbol = null;
            final boolean noRoot;
            ParsingData parsingData = null;
            String[] pluralsData = null;
            CurrencySpacingInfo spacingInfo = null;
            Map<String, String> unitPatterns = null;

            enum EntrypointTable {
                TOP,
                CURRENCIES,
                CURRENCY_PLURALS,
                CURRENCY_NARROW,
                CURRENCY_SPACING,
                CURRENCY_UNIT_PATTERNS
            }

            static {
                Class cls = ICUCurrencyDisplayInfoProvider.class;
            }

            CurrencySink(boolean noRoot, EntrypointTable entrypointTable) {
                this.noRoot = noRoot;
                this.entrypointTable = entrypointTable;
            }

            public void put(Key key, Value value, boolean isRoot) {
                if (!this.noRoot || !isRoot) {
                    switch (this.entrypointTable) {
                        case TOP:
                            consumeTopTable(key, value);
                            break;
                        case CURRENCIES:
                            consumeCurrenciesEntry(key, value);
                            break;
                        case CURRENCY_PLURALS:
                            consumeCurrencyPluralsEntry(key, value);
                            break;
                        case CURRENCY_NARROW:
                            consumeCurrenciesNarrowEntry(key, value);
                            break;
                        case CURRENCY_SPACING:
                            consumeCurrencySpacingTable(key, value);
                            break;
                        case CURRENCY_UNIT_PATTERNS:
                            consumeCurrencyUnitPatternsTable(key, value);
                            break;
                    }
                }
            }

            private void consumeTopTable(Key key, Value value) {
                Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    if (key.contentEquals("Currencies")) {
                        consumeCurrenciesTable(key, value);
                    } else if (key.contentEquals("Currencies%variant")) {
                        consumeCurrenciesVariantTable(key, value);
                    } else if (key.contentEquals("CurrencyPlurals")) {
                        consumeCurrencyPluralsTable(key, value);
                    }
                }
            }

            void consumeCurrenciesTable(Key key, Value value) {
                Table table = value.getTable();
                int i = 0;
                while (table.getKeyAndValue(i, key, value)) {
                    String isoCode = key.toString();
                    if (value.getType() == 8) {
                        Array array = value.getArray();
                        this.parsingData.symbolToIsoCode.put(isoCode, isoCode);
                        array.getValue(0, value);
                        this.parsingData.symbolToIsoCode.put(value.getString(), isoCode);
                        array.getValue(1, value);
                        this.parsingData.nameToIsoCode.put(value.getString(), isoCode);
                        i++;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unexpected data type in Currencies table for ");
                        stringBuilder.append(isoCode);
                        throw new ICUException(stringBuilder.toString());
                    }
                }
            }

            void consumeCurrenciesEntry(Key key, Value value) {
                String isoCode = key.toString();
                if (value.getType() == 8) {
                    Array array = value.getArray();
                    if (this.formattingData.symbol == null) {
                        array.getValue(0, value);
                        this.formattingData.symbol = value.getString();
                    }
                    if (this.formattingData.displayName == null) {
                        array.getValue(1, value);
                        this.formattingData.displayName = value.getString();
                    }
                    if (array.getSize() > 2 && this.formattingData.formatInfo == null) {
                        array.getValue(2, value);
                        Array formatArray = value.getArray();
                        formatArray.getValue(0, value);
                        String formatPattern = value.getString();
                        formatArray.getValue(1, value);
                        String decimalSeparator = value.getString();
                        formatArray.getValue(2, value);
                        String groupingSeparator = value.getString();
                        this.formattingData.formatInfo = new CurrencyFormatInfo(isoCode, formatPattern, decimalSeparator, groupingSeparator);
                        return;
                    }
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected data type in Currencies table for ");
                stringBuilder.append(isoCode);
                throw new ICUException(stringBuilder.toString());
            }

            void consumeCurrenciesNarrowEntry(Key key, Value value) {
                if (this.narrowSymbol.narrowSymbol == null) {
                    this.narrowSymbol.narrowSymbol = value.getString();
                }
            }

            void consumeCurrenciesVariantTable(Key key, Value value) {
                Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    this.parsingData.symbolToIsoCode.put(value.getString(), key.toString());
                }
            }

            void consumeCurrencyPluralsTable(Key key, Value value) {
                Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String isoCode = key.toString();
                    Table pluralsTable = value.getTable();
                    int j = 0;
                    while (pluralsTable.getKeyAndValue(j, key, value)) {
                        if (StandardPlural.orNullFromString(key.toString()) != null) {
                            this.parsingData.nameToIsoCode.put(value.getString(), isoCode);
                            j++;
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Could not make StandardPlural from keyword ");
                            stringBuilder.append(key);
                            throw new ICUException(stringBuilder.toString());
                        }
                    }
                }
            }

            void consumeCurrencyPluralsEntry(Key key, Value value) {
                Table pluralsTable = value.getTable();
                int j = 0;
                while (pluralsTable.getKeyAndValue(j, key, value)) {
                    StandardPlural plural = StandardPlural.orNullFromString(key.toString());
                    if (plural != null) {
                        if (this.pluralsData[plural.ordinal() + 1] == null) {
                            this.pluralsData[1 + plural.ordinal()] = value.getString();
                        }
                        j++;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not make StandardPlural from keyword ");
                        stringBuilder.append(key);
                        throw new ICUException(stringBuilder.toString());
                    }
                }
            }

            void consumeCurrencySpacingTable(Key key, Value value) {
                Table spacingTypesTable = value.getTable();
                for (int i = 0; spacingTypesTable.getKeyAndValue(i, key, value); i++) {
                    SpacingType type;
                    if (key.contentEquals("beforeCurrency")) {
                        type = SpacingType.BEFORE;
                        this.spacingInfo.hasBeforeCurrency = true;
                    } else if (key.contentEquals("afterCurrency")) {
                        type = SpacingType.AFTER;
                        this.spacingInfo.hasAfterCurrency = true;
                    } else {
                    }
                    Table patternsTable = value.getTable();
                    for (int j = 0; patternsTable.getKeyAndValue(j, key, value); j++) {
                        SpacingPattern pattern;
                        if (key.contentEquals("currencyMatch")) {
                            pattern = SpacingPattern.CURRENCY_MATCH;
                        } else if (key.contentEquals("surroundingMatch")) {
                            pattern = SpacingPattern.SURROUNDING_MATCH;
                        } else if (key.contentEquals("insertBetween")) {
                            pattern = SpacingPattern.INSERT_BETWEEN;
                        } else {
                        }
                        this.spacingInfo.setSymbolIfNull(type, pattern, value.getString());
                    }
                }
            }

            void consumeCurrencyUnitPatternsTable(Key key, Value value) {
                Table table = value.getTable();
                for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                    String pluralKeyword = key.toString();
                    if (this.unitPatterns.get(pluralKeyword) == null) {
                        this.unitPatterns.put(pluralKeyword, value.getString());
                    }
                }
            }
        }

        public ICUCurrencyDisplayInfo(ULocale locale, ICUResourceBundle rb, boolean fallback) {
            this.locale = locale;
            this.fallback = fallback;
            this.rb = rb;
        }

        public ULocale getULocale() {
            return this.rb.getULocale();
        }

        public String getName(String isoCode) {
            FormattingData formattingData = fetchFormattingData(isoCode);
            if (formattingData.displayName == null && this.fallback) {
                return isoCode;
            }
            return formattingData.displayName;
        }

        public String getSymbol(String isoCode) {
            FormattingData formattingData = fetchFormattingData(isoCode);
            if (formattingData.symbol == null && this.fallback) {
                return isoCode;
            }
            return formattingData.symbol;
        }

        public String getNarrowSymbol(String isoCode) {
            NarrowSymbol narrowSymbol = fetchNarrowSymbol(isoCode);
            if (narrowSymbol.narrowSymbol == null && this.fallback) {
                return isoCode;
            }
            return narrowSymbol.narrowSymbol;
        }

        public String getPluralName(String isoCode, String pluralKey) {
            StandardPlural plural = StandardPlural.orNullFromString(pluralKey);
            String[] pluralsData = fetchPluralsData(isoCode);
            String result = null;
            if (plural != null) {
                result = pluralsData[plural.ordinal() + 1];
            }
            if (result == null && this.fallback) {
                result = pluralsData[1 + StandardPlural.OTHER.ordinal()];
            }
            if (result == null && this.fallback) {
                result = fetchFormattingData(isoCode).displayName;
            }
            if (result == null && this.fallback) {
                return isoCode;
            }
            return result;
        }

        public Map<String, String> symbolMap() {
            return fetchParsingData().symbolToIsoCode;
        }

        public Map<String, String> nameMap() {
            return fetchParsingData().nameToIsoCode;
        }

        public Map<String, String> getUnitPatterns() {
            return fetchUnitPatterns();
        }

        public CurrencyFormatInfo getFormatInfo(String isoCode) {
            return fetchFormattingData(isoCode).formatInfo;
        }

        public CurrencySpacingInfo getSpacingInfo() {
            CurrencySpacingInfo spacingInfo = fetchSpacingInfo();
            if ((!spacingInfo.hasBeforeCurrency || !spacingInfo.hasAfterCurrency) && this.fallback) {
                return CurrencySpacingInfo.DEFAULT;
            }
            return spacingInfo;
        }

        FormattingData fetchFormattingData(String isoCode) {
            FormattingData result = this.formattingDataCache;
            if (result != null && result.isoCode.equals(isoCode)) {
                return result;
            }
            result = new FormattingData(isoCode);
            CurrencySink sink = new CurrencySink(this.fallback ^ 1, EntrypointTable.CURRENCIES);
            sink.formattingData = result;
            ICUResourceBundle iCUResourceBundle = this.rb;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Currencies/");
            stringBuilder.append(isoCode);
            iCUResourceBundle.getAllItemsWithFallbackNoFail(stringBuilder.toString(), sink);
            this.formattingDataCache = result;
            return result;
        }

        NarrowSymbol fetchNarrowSymbol(String isoCode) {
            NarrowSymbol result = this.narrowSymbolCache;
            if (result != null && result.isoCode.equals(isoCode)) {
                return result;
            }
            result = new NarrowSymbol(isoCode);
            CurrencySink sink = new CurrencySink(this.fallback ^ 1, EntrypointTable.CURRENCY_NARROW);
            sink.narrowSymbol = result;
            ICUResourceBundle iCUResourceBundle = this.rb;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Currencies%narrow/");
            stringBuilder.append(isoCode);
            iCUResourceBundle.getAllItemsWithFallbackNoFail(stringBuilder.toString(), sink);
            this.narrowSymbolCache = result;
            return result;
        }

        String[] fetchPluralsData(String isoCode) {
            String[] result = this.pluralsDataCache;
            if (result != null && result[0].equals(isoCode)) {
                return result;
            }
            result = new String[(StandardPlural.COUNT + 1)];
            result[0] = isoCode;
            CurrencySink sink = new CurrencySink(this.fallback ^ 1, EntrypointTable.CURRENCY_PLURALS);
            sink.pluralsData = result;
            ICUResourceBundle iCUResourceBundle = this.rb;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CurrencyPlurals/");
            stringBuilder.append(isoCode);
            iCUResourceBundle.getAllItemsWithFallbackNoFail(stringBuilder.toString(), sink);
            this.pluralsDataCache = result;
            return result;
        }

        ParsingData fetchParsingData() {
            ParsingData result = (ParsingData) this.parsingDataCache.get();
            if (result != null) {
                return result;
            }
            result = new ParsingData();
            CurrencySink sink = new CurrencySink(this.fallback ^ 1, EntrypointTable.TOP);
            sink.parsingData = result;
            this.rb.getAllItemsWithFallback("", sink);
            this.parsingDataCache = new SoftReference(result);
            return result;
        }

        Map<String, String> fetchUnitPatterns() {
            Map<String, String> result = this.unitPatternsCache;
            if (result != null) {
                return result;
            }
            HashMap result2 = new HashMap();
            CurrencySink sink = new CurrencySink(this.fallback ^ 1, EntrypointTable.CURRENCY_UNIT_PATTERNS);
            sink.unitPatterns = result2;
            this.rb.getAllItemsWithFallback("CurrencyUnitPatterns", sink);
            this.unitPatternsCache = result2;
            return result2;
        }

        CurrencySpacingInfo fetchSpacingInfo() {
            CurrencySpacingInfo result = this.spacingInfoCache;
            if (result != null) {
                return result;
            }
            result = new CurrencySpacingInfo();
            CurrencySink sink = new CurrencySink(this.fallback ^ 1, EntrypointTable.CURRENCY_SPACING);
            sink.spacingInfo = result;
            this.rb.getAllItemsWithFallback("currencySpacing", sink);
            this.spacingInfoCache = result;
            return result;
        }
    }

    public CurrencyDisplayInfo getInstance(ULocale locale, boolean withFallback) {
        if (locale == null) {
            locale = ULocale.ROOT;
        }
        ICUCurrencyDisplayInfo instance = this.currencyDisplayInfoCache;
        if (!(instance != null && instance.locale.equals(locale) && instance.fallback == withFallback)) {
            ICUResourceBundle rb;
            if (withFallback) {
                rb = ICUResourceBundle.getBundleInstance(ICUData.ICU_CURR_BASE_NAME, locale, OpenType.LOCALE_DEFAULT_ROOT);
            } else {
                try {
                    rb = ICUResourceBundle.getBundleInstance(ICUData.ICU_CURR_BASE_NAME, locale, OpenType.LOCALE_ONLY);
                } catch (MissingResourceException e) {
                    return null;
                }
            }
            instance = new ICUCurrencyDisplayInfo(locale, rb, withFallback);
            this.currencyDisplayInfoCache = instance;
        }
        return instance;
    }

    public boolean hasData() {
        return true;
    }
}
