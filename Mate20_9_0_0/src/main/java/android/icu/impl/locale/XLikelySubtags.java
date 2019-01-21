package android.icu.impl.locale;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.Utility;
import android.icu.impl.locale.XCldrStub.HashMultimap;
import android.icu.impl.locale.XCldrStub.Multimap;
import android.icu.impl.locale.XCldrStub.Multimaps;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Minimize;
import android.icu.util.UResourceBundle;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class XLikelySubtags {
    private static final XLikelySubtags DEFAULT = new XLikelySubtags();
    final Map<String, Map<String, Map<String, LSR>>> langTable;

    public static class Aliases {
        final Multimap<String, String> toAliases;
        final Map<String, String> toCanonical;

        public String getCanonical(String alias) {
            String canonical = (String) this.toCanonical.get(alias);
            return canonical == null ? alias : canonical;
        }

        public Set<String> getAliases(String canonical) {
            Set<String> aliases = this.toAliases.get(canonical);
            return aliases == null ? Collections.singleton(canonical) : aliases;
        }

        public Aliases(String key) {
            UResourceBundle territoryAlias = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "metadata", ICUResourceBundle.ICU_DATA_CLASS_LOADER).get("alias").get(key);
            Map toCanonical1 = new HashMap();
            for (int i = 0; i < territoryAlias.getSize(); i++) {
                UResourceBundle res = territoryAlias.get(i);
                String aliasFrom = res.getKey();
                if (!(aliasFrom.contains(BaseLocale.SEP) || res.get("reason").getString().equals("overlong"))) {
                    String aliasTo = res.get("replacement").getString();
                    int spacePos = aliasTo.indexOf(32);
                    String aliasFirst = spacePos < 0 ? aliasTo : aliasTo.substring(0, spacePos);
                    if (!aliasFirst.contains(BaseLocale.SEP)) {
                        toCanonical1.put(aliasFrom, aliasFirst);
                    }
                }
            }
            if (key.equals("language")) {
                toCanonical1.put("mo", "ro");
            }
            this.toCanonical = Collections.unmodifiableMap(toCanonical1);
            this.toAliases = Multimaps.invertFrom(toCanonical1, HashMultimap.create());
        }
    }

    public static class LSR {
        public static Aliases LANGUAGE_ALIASES = new Aliases("language");
        public static Aliases REGION_ALIASES = new Aliases("territory");
        public final String language;
        public final String region;
        public final String script;

        public static LSR from(String language, String script, String region) {
            return new LSR(language, script, region);
        }

        static LSR from(String languageIdentifier) {
            String[] parts = languageIdentifier.split("[-_]");
            if (parts.length < 1 || parts.length > 3) {
                throw new ICUException("too many subtags");
            }
            String lang = parts[0].toLowerCase();
            String p2 = parts.length < 2 ? "" : parts[1];
            return p2.length() < 4 ? new LSR(lang, "", p2) : new LSR(lang, p2, parts.length < 3 ? "" : parts[2]);
        }

        public static LSR from(ULocale locale) {
            return new LSR(locale.getLanguage(), locale.getScript(), locale.getCountry());
        }

        public static LSR fromMaximalized(ULocale locale) {
            return fromMaximalized(locale.getLanguage(), locale.getScript(), locale.getCountry());
        }

        public static LSR fromMaximalized(String language, String script, String region) {
            return XLikelySubtags.DEFAULT.maximize(LANGUAGE_ALIASES.getCanonical(language), script, REGION_ALIASES.getCanonical(region));
        }

        public LSR(String language, String script, String region) {
            this.language = language;
            this.script = script;
            this.region = region;
        }

        public String toString() {
            StringBuilder result = new StringBuilder(this.language);
            if (!this.script.isEmpty()) {
                result.append('-');
                result.append(this.script);
            }
            if (!this.region.isEmpty()) {
                result.append('-');
                result.append(this.region);
            }
            return result.toString();
        }

        public LSR replace(String language2, String script2, String region2) {
            if (language2 == null && script2 == null && region2 == null) {
                return this;
            }
            String str;
            String str2;
            if (language2 == null) {
                str = this.language;
            } else {
                str = language2;
            }
            if (script2 == null) {
                str2 = this.script;
            } else {
                str2 = script2;
            }
            return new LSR(str, str2, region2 == null ? this.region : region2);
        }

        /* JADX WARNING: Missing block: B:9:0x002e, code skipped:
            if (r3.region.equals(r2.region) != false) goto L_0x0033;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean equals(Object obj) {
            if (this != obj) {
                if (obj != null && obj.getClass() == getClass()) {
                    LSR lsr = (LSR) obj;
                    LSR other = lsr;
                    if (this.language.equals(lsr.language)) {
                        if (this.script.equals(other.script)) {
                        }
                    }
                }
                return false;
            }
            return true;
        }

        public int hashCode() {
            return Utility.hash(this.language, this.script, this.region);
        }
    }

    static abstract class Maker {
        static final Maker HASHMAP = new Maker() {
            public Map<Object, Object> make() {
                return new HashMap();
            }
        };
        static final Maker TREEMAP = new Maker() {
            public Map<Object, Object> make() {
                return new TreeMap();
            }
        };

        abstract <V> V make();

        Maker() {
        }

        public <K, V> V getSubtable(Map<K, V> langTable, K language) {
            V scriptTable = langTable.get(language);
            if (scriptTable != null) {
                return scriptTable;
            }
            V make = make();
            scriptTable = make;
            langTable.put(language, make);
            return scriptTable;
        }
    }

    public static final XLikelySubtags getDefault() {
        return DEFAULT;
    }

    public XLikelySubtags() {
        this(getDefaultRawData(), true);
    }

    private static Map<String, String> getDefaultRawData() {
        Map<String, String> rawData = new TreeMap();
        UResourceBundle bundle = UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "likelySubtags");
        Enumeration<String> enumer = bundle.getKeys();
        while (enumer.hasMoreElements()) {
            String key = (String) enumer.nextElement();
            rawData.put(key, bundle.getString(key));
        }
        return rawData;
    }

    public XLikelySubtags(Map<String, String> rawData, boolean skipNoncanonical) {
        this.langTable = init(rawData, skipNoncanonical);
    }

    private Map<String, Map<String, Map<String, LSR>>> init(Map<String, String> rawData, boolean skipNoncanonical) {
        String region;
        Map<String, Map<String, Map<String, LSR>>> result = (Map) Maker.TREEMAP.make();
        Map<LSR, LSR> internCache = new HashMap();
        Iterator it = rawData.entrySet().iterator();
        while (it.hasNext()) {
            Iterator it2;
            Entry<String, String> sourceTarget = (Entry) it.next();
            LSR ltp = LSR.from((String) sourceTarget.getKey());
            String language = ltp.language;
            String script = ltp.script;
            region = ltp.region;
            LSR ltp2 = LSR.from((String) sourceTarget.getValue());
            String languageTarget = ltp2.language;
            String scriptTarget = ltp2.script;
            String regionTarget = ltp2.region;
            String scriptTarget2 = scriptTarget;
            String languageTarget2 = languageTarget;
            set(result, language, script, region, languageTarget, scriptTarget2, regionTarget, internCache);
            Collection<String> languageAliases = LSR.LANGUAGE_ALIASES.getAliases(language);
            Collection<String> regionAliases = LSR.REGION_ALIASES.getAliases(region);
            for (String languageAlias : languageAliases) {
                String script2;
                String language2;
                Entry<String, String> sourceTarget2;
                for (String scriptTarget3 : regionAliases) {
                    if (!languageAlias.equals(language) || !scriptTarget3.equals(region)) {
                        languageTarget = region;
                        script2 = script;
                        language2 = language;
                        sourceTarget2 = sourceTarget;
                        it2 = it;
                        set(result, languageAlias, script2, scriptTarget3, languageTarget2, scriptTarget2, regionTarget, internCache);
                        region = languageTarget;
                        script = script2;
                        language = language2;
                        sourceTarget = sourceTarget2;
                        it = it2;
                    }
                }
                script2 = script;
                language2 = language;
                sourceTarget2 = sourceTarget;
                it2 = it;
            }
            it2 = it;
        }
        set(result, "und", "Latn", "", "en", "Latn", "US", internCache);
        for (Entry<String, LSR> regionEntry : ((Map) ((Map) result.get("und")).get("")).entrySet()) {
            LSR value = (LSR) regionEntry.getValue();
            set(result, "und", value.script, value.region, value);
        }
        if (result.containsKey("und")) {
            for (Entry<String, Map<String, Map<String, LSR>>> langEntry : result.entrySet()) {
                String lang = (String) langEntry.getKey();
                Map<String, Map<String, LSR>> scriptMap = (Map) langEntry.getValue();
                StringBuilder stringBuilder;
                if (scriptMap.containsKey("")) {
                    for (Entry<String, Map<String, LSR>> scriptEntry : scriptMap.entrySet()) {
                        region = (String) scriptEntry.getKey();
                        if (!((Map) scriptEntry.getValue()).containsKey("")) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("failure: ");
                            stringBuilder.append(lang);
                            stringBuilder.append(LanguageTag.SEP);
                            stringBuilder.append(region);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                    }
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("failure: ");
                    stringBuilder.append(lang);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            return result;
        }
        throw new IllegalArgumentException("failure: base");
    }

    private void set(Map<String, Map<String, Map<String, LSR>>> langTable, String language, String script, String region, String languageTarget, String scriptTarget, String regionTarget, Map<LSR, LSR> internCache) {
        Map<LSR, LSR> map = internCache;
        LSR newValue = new LSR(languageTarget, scriptTarget, regionTarget);
        LSR oldValue = (LSR) map.get(newValue);
        if (oldValue == null) {
            map.put(newValue, newValue);
            oldValue = newValue;
        }
        set(langTable, language, script, region, oldValue);
    }

    private void set(Map<String, Map<String, Map<String, LSR>>> langTable, String language, String script, String region, LSR newValue) {
        ((Map) Maker.TREEMAP.getSubtable((Map) Maker.TREEMAP.getSubtable(langTable, language), script)).put(region, newValue);
    }

    public LSR maximize(String source) {
        return maximize(ULocale.forLanguageTag(source));
    }

    public LSR maximize(ULocale source) {
        return maximize(source.getLanguage(), source.getScript(), source.getCountry());
    }

    public LSR maximize(LSR source) {
        return maximize(source.language, source.script, source.region);
    }

    public LSR maximize(String language, String script, String region) {
        int retainOldMask = 0;
        Map<String, Map<String, LSR>> scriptTable = (Map) this.langTable.get(language);
        if (scriptTable == null) {
            retainOldMask = 0 | 4;
            scriptTable = (Map) this.langTable.get("und");
        } else if (!language.equals("und")) {
            retainOldMask = 0 | 4;
        }
        if (script.equals("Zzzz")) {
            script = "";
        }
        Map<String, LSR> regionTable = (Map) scriptTable.get(script);
        if (regionTable == null) {
            retainOldMask |= 2;
            regionTable = (Map) scriptTable.get("");
        } else if (!script.isEmpty()) {
            retainOldMask |= 2;
        }
        if (region.equals("ZZ")) {
            region = "";
        }
        LSR result = (LSR) regionTable.get(region);
        if (result == null) {
            retainOldMask |= 1;
            result = (LSR) regionTable.get("");
            if (result == null) {
                return null;
            }
        } else if (!region.isEmpty()) {
            retainOldMask |= 1;
        }
        switch (retainOldMask) {
            case 1:
                return result.replace(null, null, region);
            case 2:
                return result.replace(null, script, null);
            case 3:
                return result.replace(null, script, region);
            case 4:
                return result.replace(language, null, null);
            case 5:
                return result.replace(language, null, region);
            case 6:
                return result.replace(language, script, null);
            case 7:
                return result.replace(language, script, region);
            default:
                return result;
        }
    }

    private LSR minimizeSubtags(String languageIn, String scriptIn, String regionIn, Minimize fieldToFavor) {
        LSR result = maximize(languageIn, scriptIn, regionIn);
        LSR value00 = (LSR) ((Map) ((Map) this.langTable.get(result.language)).get("")).get("");
        boolean favorRegionOk = false;
        if (result.script.equals(value00.script)) {
            if (result.region.equals(value00.region)) {
                return result.replace(null, "", "");
            }
            if (fieldToFavor == Minimize.FAVOR_REGION) {
                return result.replace(null, "", null);
            }
            favorRegionOk = true;
        }
        if (maximize(languageIn, scriptIn, "").equals(result)) {
            return result.replace(null, null, "");
        }
        if (favorRegionOk) {
            return result.replace(null, "", null);
        }
        return result;
    }

    private static StringBuilder show(Map<?, ?> map, String indent, StringBuilder output) {
        String first = indent.isEmpty() ? "" : "\t";
        for (Entry<?, ?> e : map.entrySet()) {
            String key = e.getKey().toString();
            Object value = e.getValue();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(first);
            stringBuilder.append(key.isEmpty() ? "∅" : key);
            output.append(stringBuilder.toString());
            if (value instanceof Map) {
                Map map2 = (Map) value;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(indent);
                stringBuilder2.append("\t");
                show(map2, stringBuilder2.toString(), output);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("\t");
                stringBuilder.append(Utility.toString(value));
                output.append(stringBuilder.toString());
                output.append("\n");
            }
            first = indent;
        }
        return output;
    }

    public String toString() {
        return show(this.langTable, "", new StringBuilder()).toString();
    }
}
