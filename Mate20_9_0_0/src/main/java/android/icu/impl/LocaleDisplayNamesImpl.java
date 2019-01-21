package android.icu.impl;

import android.icu.impl.CurrencyData.CurrencyDisplayInfo;
import android.icu.impl.UResource.Key;
import android.icu.impl.UResource.Sink;
import android.icu.impl.UResource.Table;
import android.icu.impl.UResource.Value;
import android.icu.impl.coll.CollationSettings;
import android.icu.impl.locale.AsciiUtil;
import android.icu.lang.UCharacter;
import android.icu.lang.UScript;
import android.icu.text.BreakIterator;
import android.icu.text.CaseMap;
import android.icu.text.CaseMap.Title;
import android.icu.text.DisplayContext;
import android.icu.text.DisplayContext.Type;
import android.icu.text.LocaleDisplayNames;
import android.icu.text.LocaleDisplayNames.DialectHandling;
import android.icu.text.LocaleDisplayNames.UiListItem;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Builder;
import android.icu.util.ULocale.Minimize;
import android.icu.util.UResourceBundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Set;

public class LocaleDisplayNamesImpl extends LocaleDisplayNames {
    private static final Title TO_TITLE_WHOLE_STRING_NO_LOWERCASE = CaseMap.toTitle().wholeString().noLowercase();
    private static final Cache cache = new Cache();
    private static final Map<String, CapitalizationContextUsage> contextUsageTypeMap = new HashMap();
    private final DisplayContext capitalization;
    private transient BreakIterator capitalizationBrkIter;
    private boolean[] capitalizationUsage;
    private final CurrencyDisplayInfo currencyDisplayInfo;
    private final DialectHandling dialectHandling;
    private final String format;
    private final char formatCloseParen;
    private final char formatOpenParen;
    private final char formatReplaceCloseParen;
    private final char formatReplaceOpenParen;
    private final String keyTypeFormat;
    private final DataTable langData;
    private final ULocale locale;
    private final DisplayContext nameLength;
    private final DataTable regionData;
    private final String separatorFormat;
    private final DisplayContext substituteHandling;

    private static class Cache {
        private LocaleDisplayNames cache;
        private DisplayContext capitalization;
        private DialectHandling dialectHandling;
        private ULocale locale;
        private DisplayContext nameLength;
        private DisplayContext substituteHandling;

        private Cache() {
        }

        public LocaleDisplayNames get(ULocale locale, DialectHandling dialectHandling) {
            if (!(dialectHandling == this.dialectHandling && DisplayContext.CAPITALIZATION_NONE == this.capitalization && DisplayContext.LENGTH_FULL == this.nameLength && DisplayContext.SUBSTITUTE == this.substituteHandling && locale.equals(this.locale))) {
                this.locale = locale;
                this.dialectHandling = dialectHandling;
                this.capitalization = DisplayContext.CAPITALIZATION_NONE;
                this.nameLength = DisplayContext.LENGTH_FULL;
                this.substituteHandling = DisplayContext.SUBSTITUTE;
                this.cache = new LocaleDisplayNamesImpl(locale, dialectHandling);
            }
            return this.cache;
        }

        public LocaleDisplayNames get(ULocale locale, DisplayContext... contexts) {
            DialectHandling dialectHandlingIn = DialectHandling.STANDARD_NAMES;
            DisplayContext capitalizationIn = DisplayContext.CAPITALIZATION_NONE;
            DisplayContext nameLengthIn = DisplayContext.LENGTH_FULL;
            DisplayContext substituteHandling = DisplayContext.SUBSTITUTE;
            for (DisplayContext contextItem : contexts) {
                switch (contextItem.type()) {
                    case DIALECT_HANDLING:
                        dialectHandlingIn = contextItem.value() == DisplayContext.STANDARD_NAMES.value() ? DialectHandling.STANDARD_NAMES : DialectHandling.DIALECT_NAMES;
                        break;
                    case CAPITALIZATION:
                        capitalizationIn = contextItem;
                        break;
                    case DISPLAY_LENGTH:
                        nameLengthIn = contextItem;
                        break;
                    case SUBSTITUTE_HANDLING:
                        substituteHandling = contextItem;
                        break;
                    default:
                        break;
                }
            }
            if (!(dialectHandlingIn == this.dialectHandling && capitalizationIn == this.capitalization && nameLengthIn == this.nameLength && substituteHandling == this.substituteHandling && locale.equals(this.locale))) {
                this.locale = locale;
                this.dialectHandling = dialectHandlingIn;
                this.capitalization = capitalizationIn;
                this.nameLength = nameLengthIn;
                this.substituteHandling = substituteHandling;
                this.cache = new LocaleDisplayNamesImpl(locale, contexts);
            }
            return this.cache;
        }
    }

    private enum CapitalizationContextUsage {
        LANGUAGE,
        SCRIPT,
        TERRITORY,
        VARIANT,
        KEY,
        KEYVALUE
    }

    public static class DataTable {
        final boolean nullIfNotFound;

        DataTable(boolean nullIfNotFound) {
            this.nullIfNotFound = nullIfNotFound;
        }

        ULocale getLocale() {
            return ULocale.ROOT;
        }

        String get(String tableName, String code) {
            return get(tableName, null, code);
        }

        String get(String tableName, String subTableName, String code) {
            return this.nullIfNotFound ? null : code;
        }
    }

    public enum DataTableType {
        LANG,
        REGION
    }

    static abstract class DataTables {
        public abstract DataTable get(ULocale uLocale, boolean z);

        DataTables() {
        }

        public static DataTables load(String className) {
            try {
                return (DataTables) Class.forName(className).newInstance();
            } catch (Throwable th) {
                return new DataTables() {
                    public DataTable get(ULocale locale, boolean nullIfNotFound) {
                        return new DataTable(nullIfNotFound);
                    }
                };
            }
        }
    }

    static class LangDataTables {
        static final DataTables impl = DataTables.load("android.icu.impl.ICULangDataTables");

        LangDataTables() {
        }
    }

    static class RegionDataTables {
        static final DataTables impl = DataTables.load("android.icu.impl.ICURegionDataTables");

        RegionDataTables() {
        }
    }

    private final class CapitalizationContextSink extends Sink {
        boolean hasCapitalizationUsage;

        private CapitalizationContextSink() {
            this.hasCapitalizationUsage = false;
        }

        public void put(Key key, Value value, boolean noFallback) {
            Table contextsTable = value.getTable();
            for (int i = 0; contextsTable.getKeyAndValue(i, key, value); i++) {
                CapitalizationContextUsage usage = (CapitalizationContextUsage) LocaleDisplayNamesImpl.contextUsageTypeMap.get(key.toString());
                if (usage != null) {
                    int[] intVector = value.getIntVector();
                    if (intVector.length >= 2) {
                        if ((LocaleDisplayNamesImpl.this.capitalization == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU ? intVector[0] : intVector[1]) != 0) {
                            LocaleDisplayNamesImpl.this.capitalizationUsage[usage.ordinal()] = true;
                            this.hasCapitalizationUsage = true;
                        }
                    }
                }
            }
        }
    }

    static class ICUDataTable extends DataTable {
        private final ICUResourceBundle bundle;

        public ICUDataTable(String path, ULocale locale, boolean nullIfNotFound) {
            super(nullIfNotFound);
            this.bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(path, locale.getBaseName());
        }

        public ULocale getLocale() {
            return this.bundle.getULocale();
        }

        public String get(String tableName, String subTableName, String code) {
            String str;
            ICUResourceBundle iCUResourceBundle = this.bundle;
            if (this.nullIfNotFound) {
                str = null;
            } else {
                str = code;
            }
            return ICUResourceTableAccess.getTableString(iCUResourceBundle, tableName, subTableName, code, str);
        }
    }

    static abstract class ICUDataTables extends DataTables {
        private final String path;

        protected ICUDataTables(String path) {
            this.path = path;
        }

        public DataTable get(ULocale locale, boolean nullIfNotFound) {
            return new ICUDataTable(this.path, locale, nullIfNotFound);
        }
    }

    static {
        contextUsageTypeMap.put("languages", CapitalizationContextUsage.LANGUAGE);
        contextUsageTypeMap.put("script", CapitalizationContextUsage.SCRIPT);
        contextUsageTypeMap.put("territory", CapitalizationContextUsage.TERRITORY);
        contextUsageTypeMap.put("variant", CapitalizationContextUsage.VARIANT);
        contextUsageTypeMap.put("key", CapitalizationContextUsage.KEY);
        contextUsageTypeMap.put("keyValue", CapitalizationContextUsage.KEYVALUE);
    }

    private static String toTitleWholeStringNoLowercase(ULocale locale, String s) {
        return ((StringBuilder) TO_TITLE_WHOLE_STRING_NO_LOWERCASE.apply(locale.toLocale(), null, s, new StringBuilder(), null)).toString();
    }

    public static LocaleDisplayNames getInstance(ULocale locale, DialectHandling dialectHandling) {
        LocaleDisplayNames localeDisplayNames;
        synchronized (cache) {
            localeDisplayNames = cache.get(locale, dialectHandling);
        }
        return localeDisplayNames;
    }

    public static LocaleDisplayNames getInstance(ULocale locale, DisplayContext... contexts) {
        LocaleDisplayNames localeDisplayNames;
        synchronized (cache) {
            localeDisplayNames = cache.get(locale, contexts);
        }
        return localeDisplayNames;
    }

    public LocaleDisplayNamesImpl(ULocale locale, DialectHandling dialectHandling) {
        DisplayContext[] displayContextArr = new DisplayContext[2];
        displayContextArr[0] = dialectHandling == DialectHandling.STANDARD_NAMES ? DisplayContext.STANDARD_NAMES : DisplayContext.DIALECT_NAMES;
        displayContextArr[1] = DisplayContext.CAPITALIZATION_NONE;
        this(locale, displayContextArr);
    }

    public LocaleDisplayNamesImpl(ULocale locale, DisplayContext... contexts) {
        ULocale locale2;
        this.capitalizationUsage = null;
        this.capitalizationBrkIter = null;
        DialectHandling dialectHandling = DialectHandling.STANDARD_NAMES;
        DisplayContext capitalization = DisplayContext.CAPITALIZATION_NONE;
        DisplayContext nameLength = DisplayContext.LENGTH_FULL;
        DisplayContext substituteHandling = DisplayContext.SUBSTITUTE;
        DisplayContext substituteHandling2 = substituteHandling;
        substituteHandling = nameLength;
        nameLength = capitalization;
        DialectHandling dialectHandling2 = dialectHandling;
        for (DisplayContext contextItem : contexts) {
            switch (contextItem.type()) {
                case DIALECT_HANDLING:
                    dialectHandling2 = contextItem.value() == DisplayContext.STANDARD_NAMES.value() ? DialectHandling.STANDARD_NAMES : DialectHandling.DIALECT_NAMES;
                    break;
                case CAPITALIZATION:
                    nameLength = contextItem;
                    break;
                case DISPLAY_LENGTH:
                    substituteHandling = contextItem;
                    break;
                case SUBSTITUTE_HANDLING:
                    substituteHandling2 = contextItem;
                    break;
                default:
                    break;
            }
        }
        this.dialectHandling = dialectHandling2;
        this.capitalization = nameLength;
        this.nameLength = substituteHandling;
        this.substituteHandling = substituteHandling2;
        boolean z = true;
        this.langData = LangDataTables.impl.get(locale, substituteHandling2 == DisplayContext.NO_SUBSTITUTE);
        DataTables dataTables = RegionDataTables.impl;
        if (substituteHandling2 != DisplayContext.NO_SUBSTITUTE) {
            z = false;
        }
        this.regionData = dataTables.get(locale, z);
        if (ULocale.ROOT.equals(this.langData.getLocale())) {
            locale2 = this.regionData.getLocale();
        } else {
            locale2 = this.langData.getLocale();
        }
        this.locale = locale2;
        String sep = this.langData.get("localeDisplayPattern", "separator");
        if (sep == null || "separator".equals(sep)) {
            sep = "{0}, {1}";
        }
        StringBuilder sb = new StringBuilder();
        this.separatorFormat = SimpleFormatterImpl.compileToStringMinMaxArguments(sep, sb, 2, 2);
        String pattern = this.langData.get("localeDisplayPattern", "pattern");
        if (pattern == null || "pattern".equals(pattern)) {
            pattern = "{0} ({1})";
        }
        this.format = SimpleFormatterImpl.compileToStringMinMaxArguments(pattern, sb, 2, 2);
        if (pattern.contains("（")) {
            this.formatOpenParen = 65288;
            this.formatCloseParen = 65289;
            this.formatReplaceOpenParen = 65339;
            this.formatReplaceCloseParen = 65341;
        } else {
            this.formatOpenParen = '(';
            this.formatCloseParen = ')';
            this.formatReplaceOpenParen = '[';
            this.formatReplaceCloseParen = ']';
        }
        String keyTypePattern = this.langData.get("localeDisplayPattern", "keyTypePattern");
        if (keyTypePattern == null || "keyTypePattern".equals(keyTypePattern)) {
            keyTypePattern = "{0}={1}";
        }
        this.keyTypeFormat = SimpleFormatterImpl.compileToStringMinMaxArguments(keyTypePattern, sb, 2, 2);
        z = false;
        if (nameLength == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU || nameLength == DisplayContext.CAPITALIZATION_FOR_STANDALONE) {
            this.capitalizationUsage = new boolean[CapitalizationContextUsage.values().length];
            ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale);
            CapitalizationContextSink sink = new CapitalizationContextSink();
            try {
                rb.getAllItemsWithFallback("contextTransforms", sink);
            } catch (MissingResourceException e) {
            }
            z = sink.hasCapitalizationUsage;
        }
        if (z || nameLength == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE) {
            this.capitalizationBrkIter = BreakIterator.getSentenceInstance(locale);
        }
        this.currencyDisplayInfo = CurrencyData.provider.getInstance(locale, false);
    }

    public ULocale getLocale() {
        return this.locale;
    }

    public DialectHandling getDialectHandling() {
        return this.dialectHandling;
    }

    public DisplayContext getContext(Type type) {
        switch (type) {
            case DIALECT_HANDLING:
                return this.dialectHandling == DialectHandling.STANDARD_NAMES ? DisplayContext.STANDARD_NAMES : DisplayContext.DIALECT_NAMES;
            case CAPITALIZATION:
                return this.capitalization;
            case DISPLAY_LENGTH:
                return this.nameLength;
            case SUBSTITUTE_HANDLING:
                return this.substituteHandling;
            default:
                return DisplayContext.STANDARD_NAMES;
        }
    }

    private String adjustForUsageAndContext(CapitalizationContextUsage usage, String name) {
        if (name == null || name.length() <= 0 || !UCharacter.isLowerCase(name.codePointAt(0)) || (this.capitalization != DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE && (this.capitalizationUsage == null || !this.capitalizationUsage[usage.ordinal()]))) {
            return name;
        }
        String toTitleCase;
        synchronized (this) {
            if (this.capitalizationBrkIter == null) {
                this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.locale);
            }
            toTitleCase = UCharacter.toTitleCase(this.locale, name, this.capitalizationBrkIter, (int) CollationSettings.CASE_FIRST_AND_UPPER_MASK);
        }
        return toTitleCase;
    }

    public String localeDisplayName(ULocale locale) {
        return localeDisplayNameInternal(locale);
    }

    public String localeDisplayName(Locale locale) {
        return localeDisplayNameInternal(ULocale.forLocale(locale));
    }

    public String localeDisplayName(String localeId) {
        return localeDisplayNameInternal(new ULocale(localeId));
    }

    private String localeDisplayNameInternal(ULocale locale) {
        String langScriptCountry;
        String result;
        StringBuilder stringBuilder;
        String langCountry;
        String result2;
        String lang;
        String script;
        String country;
        String resultName = null;
        String lang2 = locale.getLanguage();
        if (locale.getBaseName().length() == 0) {
            lang2 = "root";
        }
        String script2 = locale.getScript();
        String country2 = locale.getCountry();
        String variant = locale.getVariant();
        boolean z = true;
        boolean hasScript = script2.length() > 0;
        boolean hasCountry = country2.length() > 0;
        boolean hasVariant = variant.length() > 0;
        if (this.dialectHandling == DialectHandling.DIALECT_NAMES) {
            if (hasScript && hasCountry) {
                langScriptCountry = new StringBuilder();
                langScriptCountry.append(lang2);
                langScriptCountry.append('_');
                langScriptCountry.append(script2);
                langScriptCountry.append('_');
                langScriptCountry.append(country2);
                langScriptCountry = langScriptCountry.toString();
                result = localeIdName(langScriptCountry);
                if (!(result == null || result.equals(langScriptCountry))) {
                    resultName = result;
                    hasScript = false;
                    hasCountry = false;
                }
            }
            if (hasScript) {
                langScriptCountry = new StringBuilder();
                langScriptCountry.append(lang2);
                langScriptCountry.append('_');
                langScriptCountry.append(script2);
                langScriptCountry = langScriptCountry.toString();
                result = localeIdName(langScriptCountry);
                if (!(result == null || result.equals(langScriptCountry))) {
                    resultName = result;
                    hasScript = false;
                }
            }
            if (hasCountry) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(lang2);
                stringBuilder.append('_');
                stringBuilder.append(country2);
                langCountry = stringBuilder.toString();
                langScriptCountry = localeIdName(langCountry);
                if (!(langScriptCountry == null || langScriptCountry.equals(langCountry))) {
                    resultName = langScriptCountry;
                    hasCountry = false;
                }
            }
        }
        if (resultName == null) {
            langScriptCountry = localeIdName(lang2);
            if (langScriptCountry == null) {
                return null;
            }
            resultName = langScriptCountry.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen);
        }
        stringBuilder = new StringBuilder();
        if (hasScript) {
            result = scriptDisplayNameInContext(script2, true);
            if (result == null) {
                return null;
            }
            stringBuilder.append(result.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen));
        }
        if (hasCountry) {
            result2 = regionDisplayName(country2, true);
            if (result2 == null) {
                return null;
            }
            appendWithSep(result2.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen), stringBuilder);
        }
        if (hasVariant) {
            result2 = variantDisplayName(variant, true);
            if (result2 == null) {
                return null;
            }
            appendWithSep(result2.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen), stringBuilder);
        }
        Iterator<String> keys = locale.getKeywords();
        if (keys != null) {
            while (keys.hasNext()) {
                String key = (String) keys.next();
                result = locale.getKeywordValue(key);
                langCountry = keyDisplayName(key, z);
                if (langCountry == null) {
                    return null;
                }
                lang = lang2;
                script = script2;
                lang2 = langCountry.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen);
                String valueDisplayName = keyValueDisplayName(key, result, true);
                if (valueDisplayName == null) {
                    return null;
                }
                country = country2;
                script2 = valueDisplayName.replace(this.formatOpenParen, this.formatReplaceOpenParen).replace(this.formatCloseParen, this.formatReplaceCloseParen);
                if (!script2.equals(result)) {
                    appendWithSep(script2, stringBuilder);
                } else if (key.equals(lang2)) {
                    StringBuilder appendWithSep = appendWithSep(lang2, stringBuilder);
                    appendWithSep.append("=");
                    appendWithSep.append(script2);
                } else {
                    appendWithSep(SimpleFormatterImpl.formatCompiledPattern(this.keyTypeFormat, lang2, script2), stringBuilder);
                }
                lang2 = lang;
                script2 = script;
                country2 = country;
                z = true;
            }
        }
        ULocale uLocale = locale;
        lang = lang2;
        script = script2;
        country = country2;
        lang2 = null;
        if (stringBuilder.length() > 0) {
            lang2 = stringBuilder.toString();
        }
        if (lang2 != null) {
            resultName = SimpleFormatterImpl.formatCompiledPattern(this.format, resultName, lang2);
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, resultName);
    }

    private String localeIdName(String localeId) {
        if (this.nameLength == DisplayContext.LENGTH_SHORT) {
            String locIdName = this.langData.get("Languages%short", localeId);
            if (!(locIdName == null || locIdName.equals(localeId))) {
                return locIdName;
            }
        }
        return this.langData.get("Languages", localeId);
    }

    public String languageDisplayName(String lang) {
        if (lang.equals("root") || lang.indexOf(95) != -1) {
            return this.substituteHandling == DisplayContext.SUBSTITUTE ? lang : null;
        }
        if (this.nameLength == DisplayContext.LENGTH_SHORT) {
            String langName = this.langData.get("Languages%short", lang);
            if (!(langName == null || langName.equals(lang))) {
                return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, langName);
            }
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.LANGUAGE, this.langData.get("Languages", lang));
    }

    public String scriptDisplayName(String script) {
        String str = this.langData.get("Scripts%stand-alone", script);
        if (str == null || str.equals(script)) {
            if (this.nameLength == DisplayContext.LENGTH_SHORT) {
                str = this.langData.get("Scripts%short", script);
                if (!(str == null || str.equals(script))) {
                    return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, str);
                }
            }
            str = this.langData.get("Scripts", script);
        }
        return adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, str);
    }

    private String scriptDisplayNameInContext(String script, boolean skipAdjust) {
        String scriptName;
        if (this.nameLength == DisplayContext.LENGTH_SHORT) {
            scriptName = this.langData.get("Scripts%short", script);
            if (!(scriptName == null || scriptName.equals(script))) {
                return skipAdjust ? scriptName : adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, scriptName);
            }
        }
        scriptName = this.langData.get("Scripts", script);
        return skipAdjust ? scriptName : adjustForUsageAndContext(CapitalizationContextUsage.SCRIPT, scriptName);
    }

    public String scriptDisplayNameInContext(String script) {
        return scriptDisplayNameInContext(script, false);
    }

    public String scriptDisplayName(int scriptCode) {
        return scriptDisplayName(UScript.getShortName(scriptCode));
    }

    private String regionDisplayName(String region, boolean skipAdjust) {
        String regionName;
        if (this.nameLength == DisplayContext.LENGTH_SHORT) {
            regionName = this.regionData.get("Countries%short", region);
            if (!(regionName == null || regionName.equals(region))) {
                return skipAdjust ? regionName : adjustForUsageAndContext(CapitalizationContextUsage.TERRITORY, regionName);
            }
        }
        regionName = this.regionData.get("Countries", region);
        return skipAdjust ? regionName : adjustForUsageAndContext(CapitalizationContextUsage.TERRITORY, regionName);
    }

    public String regionDisplayName(String region) {
        return regionDisplayName(region, false);
    }

    private String variantDisplayName(String variant, boolean skipAdjust) {
        String variantName = this.langData.get("Variants", variant);
        return skipAdjust ? variantName : adjustForUsageAndContext(CapitalizationContextUsage.VARIANT, variantName);
    }

    public String variantDisplayName(String variant) {
        return variantDisplayName(variant, false);
    }

    private String keyDisplayName(String key, boolean skipAdjust) {
        String keyName = this.langData.get("Keys", key);
        return skipAdjust ? keyName : adjustForUsageAndContext(CapitalizationContextUsage.KEY, keyName);
    }

    public String keyDisplayName(String key) {
        return keyDisplayName(key, false);
    }

    private String keyValueDisplayName(String key, String value, boolean skipAdjust) {
        String keyValueName = null;
        if (key.equals("currency")) {
            keyValueName = this.currencyDisplayInfo.getName(AsciiUtil.toUpperString(value));
            if (keyValueName == null) {
                keyValueName = value;
            }
        } else {
            if (this.nameLength == DisplayContext.LENGTH_SHORT) {
                String tmp = this.langData.get("Types%short", key, value);
                if (!(tmp == null || tmp.equals(value))) {
                    keyValueName = tmp;
                }
            }
            if (keyValueName == null) {
                keyValueName = this.langData.get("Types", key, value);
            }
        }
        return skipAdjust ? keyValueName : adjustForUsageAndContext(CapitalizationContextUsage.KEYVALUE, keyValueName);
    }

    public String keyValueDisplayName(String key, String value) {
        return keyValueDisplayName(key, value, false);
    }

    public List<UiListItem> getUiListCompareWholeItems(Set<ULocale> localeSet, Comparator<UiListItem> comparator) {
        ULocale loc;
        Builder builder;
        DisplayContext capContext = getContext(Type.CAPITALIZATION);
        List<UiListItem> result = new ArrayList();
        Map<ULocale, Set<ULocale>> baseToLocales = new HashMap();
        Builder builder2 = new Builder();
        for (ULocale locOriginal : localeSet) {
            builder2.setLocale(locOriginal);
            loc = ULocale.addLikelySubtags(locOriginal);
            ULocale base = new ULocale(loc.getLanguage());
            Set<ULocale> locales = (Set) baseToLocales.get(base);
            if (locales == null) {
                HashSet hashSet = new HashSet();
                locales = hashSet;
                baseToLocales.put(base, hashSet);
            }
            locales.add(loc);
        }
        for (Entry<ULocale, Set<ULocale>> entry : baseToLocales.entrySet()) {
            loc = (ULocale) entry.getKey();
            Set<ULocale> values = (Set) entry.getValue();
            if (values.size() == 1) {
                result.add(newRow(ULocale.minimizeSubtags((ULocale) values.iterator().next(), Minimize.FAVOR_SCRIPT), capContext));
            } else {
                Set<String> scripts = new HashSet();
                Set<String> regions = new HashSet();
                ULocale maxBase = ULocale.addLikelySubtags(loc);
                scripts.add(maxBase.getScript());
                regions.add(maxBase.getCountry());
                for (ULocale locale : values) {
                    scripts.add(locale.getScript());
                    regions.add(locale.getCountry());
                }
                boolean z = false;
                boolean hasScripts = scripts.size() > 1;
                if (regions.size() > 1) {
                    z = true;
                }
                boolean hasRegions = z;
                for (ULocale locale2 : values) {
                    Map<ULocale, Set<ULocale>> baseToLocales2 = baseToLocales;
                    Builder modified = builder2.setLocale(locale2);
                    if (hasScripts) {
                        builder = builder2;
                    } else {
                        builder = builder2;
                        modified.setScript("");
                    }
                    if (!hasRegions) {
                        modified.setRegion("");
                    }
                    result.add(newRow(modified.build(), capContext));
                    baseToLocales = baseToLocales2;
                    builder2 = builder;
                }
            }
            baseToLocales = baseToLocales;
            builder2 = builder2;
        }
        builder = builder2;
        Collections.sort(result, comparator);
        return result;
    }

    private UiListItem newRow(ULocale modified, DisplayContext capContext) {
        ULocale minimized = ULocale.minimizeSubtags(modified, Minimize.FAVOR_SCRIPT);
        String tempName = modified.getDisplayName(this.locale);
        String nameInDisplayLocale = capContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU ? toTitleWholeStringNoLowercase(this.locale, tempName) : tempName;
        tempName = modified.getDisplayName(modified);
        return new UiListItem(minimized, modified, nameInDisplayLocale, capContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU ? toTitleWholeStringNoLowercase(modified, tempName) : tempName);
    }

    public static boolean haveData(DataTableType type) {
        switch (type) {
            case LANG:
                return LangDataTables.impl instanceof ICUDataTables;
            case REGION:
                return RegionDataTables.impl instanceof ICUDataTables;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown type: ");
                stringBuilder.append(type);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private StringBuilder appendWithSep(String s, StringBuilder b) {
        if (b.length() == 0) {
            b.append(s);
        } else {
            SimpleFormatterImpl.formatAndReplace(this.separatorFormat, b, null, b, s);
        }
        return b;
    }
}
