package android.icu.impl;

import android.icu.impl.locale.BaseLocale;
import android.icu.impl.number.Padder;
import android.icu.text.PluralRanges;
import android.icu.text.PluralRules;
import android.icu.text.PluralRules.Factory;
import android.icu.text.PluralRules.PluralType;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;

public class PluralRulesLoader extends Factory {
    private static final PluralRanges UNKNOWN_RANGE = new PluralRanges().freeze();
    public static final PluralRulesLoader loader = new PluralRulesLoader();
    private static Map<String, PluralRanges> localeIdToPluralRanges;
    private Map<String, String> localeIdToCardinalRulesId;
    private Map<String, String> localeIdToOrdinalRulesId;
    private Map<String, ULocale> rulesIdToEquivalentULocale;
    private final Map<String, PluralRules> rulesIdToRules = new HashMap();

    private PluralRulesLoader() {
    }

    public ULocale[] getAvailableULocales() {
        Set<String> keys = getLocaleIdToRulesIdMap(PluralType.CARDINAL).keySet();
        ULocale[] locales = new ULocale[keys.size()];
        int n = 0;
        for (String createCanonical : keys) {
            int n2 = n + 1;
            locales[n] = ULocale.createCanonical(createCanonical);
            n = n2;
        }
        return locales;
    }

    public ULocale getFunctionalEquivalent(ULocale locale, boolean[] isAvailable) {
        if (isAvailable != null && isAvailable.length > 0) {
            isAvailable[0] = getLocaleIdToRulesIdMap(PluralType.CARDINAL).containsKey(ULocale.canonicalize(locale.getBaseName()));
        }
        String rulesId = getRulesIdForLocale(locale, PluralType.CARDINAL);
        if (rulesId == null || rulesId.trim().length() == 0) {
            return ULocale.ROOT;
        }
        ULocale result = (ULocale) getRulesIdToEquivalentULocaleMap().get(rulesId);
        if (result == null) {
            return ULocale.ROOT;
        }
        return result;
    }

    private Map<String, String> getLocaleIdToRulesIdMap(PluralType type) {
        checkBuildRulesIdMaps();
        return type == PluralType.CARDINAL ? this.localeIdToCardinalRulesId : this.localeIdToOrdinalRulesId;
    }

    private Map<String, ULocale> getRulesIdToEquivalentULocaleMap() {
        checkBuildRulesIdMaps();
        return this.rulesIdToEquivalentULocale;
    }

    private void checkBuildRulesIdMaps() {
        int i;
        boolean haveMap;
        synchronized (this) {
            i = 0;
            haveMap = this.localeIdToCardinalRulesId != null;
        }
        if (!haveMap) {
            Map<String, String> tempLocaleIdToCardinalRulesId;
            Map<String, ULocale> tempRulesIdToEquivalentULocale;
            Map<String, String> tempLocaleIdToOrdinalRulesId;
            try {
                UResourceBundle b;
                UResourceBundle pluralb = getPluralBundle();
                UResourceBundle localeb = pluralb.get("locales");
                tempLocaleIdToCardinalRulesId = new TreeMap();
                tempRulesIdToEquivalentULocale = new HashMap();
                for (int i2 = 0; i2 < localeb.getSize(); i2++) {
                    b = localeb.get(i2);
                    String id = b.getKey();
                    String value = b.getString().intern();
                    tempLocaleIdToCardinalRulesId.put(id, value);
                    if (!tempRulesIdToEquivalentULocale.containsKey(value)) {
                        tempRulesIdToEquivalentULocale.put(value, new ULocale(id));
                    }
                }
                localeb = pluralb.get("locales_ordinals");
                tempLocaleIdToOrdinalRulesId = new TreeMap();
                while (i < localeb.getSize()) {
                    b = localeb.get(i);
                    tempLocaleIdToOrdinalRulesId.put(b.getKey(), b.getString().intern());
                    i++;
                }
            } catch (MissingResourceException e) {
                tempLocaleIdToCardinalRulesId = Collections.emptyMap();
                tempLocaleIdToOrdinalRulesId = Collections.emptyMap();
                tempRulesIdToEquivalentULocale = Collections.emptyMap();
            }
            Map<String, ULocale> tempRulesIdToEquivalentULocale2 = tempRulesIdToEquivalentULocale;
            synchronized (this) {
                if (this.localeIdToCardinalRulesId == null) {
                    this.localeIdToCardinalRulesId = tempLocaleIdToCardinalRulesId;
                    this.localeIdToOrdinalRulesId = tempLocaleIdToOrdinalRulesId;
                    this.rulesIdToEquivalentULocale = tempRulesIdToEquivalentULocale2;
                }
            }
        }
    }

    public String getRulesIdForLocale(ULocale locale, PluralType type) {
        String rulesId;
        Map<String, String> idMap = getLocaleIdToRulesIdMap(type);
        String localeId = ULocale.canonicalize(locale.getBaseName());
        while (true) {
            String str = (String) idMap.get(localeId);
            rulesId = str;
            if (str != null) {
                break;
            }
            int ix = localeId.lastIndexOf(BaseLocale.SEP);
            if (ix == -1) {
                break;
            }
            localeId = localeId.substring(0, ix);
        }
        return rulesId;
    }

    public PluralRules getRulesForRulesId(String rulesId) {
        boolean hasRules;
        PluralRules rules = null;
        synchronized (this.rulesIdToRules) {
            hasRules = this.rulesIdToRules.containsKey(rulesId);
            if (hasRules) {
                rules = (PluralRules) this.rulesIdToRules.get(rulesId);
            }
        }
        if (!hasRules) {
            try {
                UResourceBundle setb = getPluralBundle().get("rules").get(rulesId);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < setb.getSize(); i++) {
                    UResourceBundle b = setb.get(i);
                    if (i > 0) {
                        sb.append("; ");
                    }
                    sb.append(b.getKey());
                    sb.append(PluralRules.KEYWORD_RULE_SEPARATOR);
                    sb.append(b.getString());
                }
                rules = PluralRules.parseDescription(sb.toString());
            } catch (ParseException | MissingResourceException e) {
            }
            synchronized (this.rulesIdToRules) {
                if (this.rulesIdToRules.containsKey(rulesId)) {
                    rules = (PluralRules) this.rulesIdToRules.get(rulesId);
                } else {
                    this.rulesIdToRules.put(rulesId, rules);
                }
            }
        }
        return rules;
    }

    public UResourceBundle getPluralBundle() throws MissingResourceException {
        return ICUResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "plurals", ICUResourceBundle.ICU_DATA_CLASS_LOADER, true);
    }

    public PluralRules forLocale(ULocale locale, PluralType type) {
        String rulesId = getRulesIdForLocale(locale, type);
        if (rulesId == null || rulesId.trim().length() == 0) {
            return PluralRules.DEFAULT;
        }
        PluralRules rules = getRulesForRulesId(rulesId);
        if (rules == null) {
            rules = PluralRules.DEFAULT;
        }
        return rules;
    }

    static {
        pluralRangeData = new String[171][];
        int i = 0;
        pluralRangeData[0] = new String[]{"locales", "id ja km ko lo ms my th vi zh"};
        pluralRangeData[1] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[2] = new String[]{"locales", "am bn fr gu hi hy kn mr pa zu"};
        pluralRangeData[3] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[4] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[5] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[6] = new String[]{"locales", "fa"};
        pluralRangeData[7] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER};
        pluralRangeData[8] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[9] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[10] = new String[]{"locales", "ka"};
        pluralRangeData[11] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE};
        pluralRangeData[12] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER};
        pluralRangeData[13] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[14] = new String[]{"locales", "az de el gl hu it kk ky ml mn ne nl pt sq sw ta te tr ug uz"};
        pluralRangeData[15] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[16] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[17] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[18] = new String[]{"locales", "af bg ca en es et eu fi nb sv ur"};
        pluralRangeData[19] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[20] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER};
        pluralRangeData[21] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[22] = new String[]{"locales", "da fil is"};
        pluralRangeData[23] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[24] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[25] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[26] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[27] = new String[]{"locales", "si"};
        pluralRangeData[28] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[29] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[30] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER};
        pluralRangeData[31] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[32] = new String[]{"locales", "mk"};
        pluralRangeData[33] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER};
        pluralRangeData[34] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[35] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER};
        pluralRangeData[36] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[37] = new String[]{"locales", "lv"};
        pluralRangeData[38] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER};
        pluralRangeData[39] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[40] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[41] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER};
        pluralRangeData[42] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[43] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[44] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER};
        pluralRangeData[45] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[46] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[47] = new String[]{"locales", "ro"};
        pluralRangeData[48] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[49] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[50] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW};
        pluralRangeData[51] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[52] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[53] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[54] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[55] = new String[]{"locales", "hr sr bs"};
        pluralRangeData[56] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[57] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[58] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[59] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[60] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[61] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[62] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[63] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[64] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[65] = new String[]{"locales", "sl"};
        pluralRangeData[66] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW};
        pluralRangeData[67] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO};
        pluralRangeData[68] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[69] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[70] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW};
        pluralRangeData[71] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO};
        pluralRangeData[72] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[73] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[74] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW};
        pluralRangeData[75] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO};
        pluralRangeData[76] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[77] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[78] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW};
        pluralRangeData[79] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO};
        pluralRangeData[80] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[81] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[82] = new String[]{"locales", "he"};
        pluralRangeData[83] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER};
        pluralRangeData[84] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[85] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[86] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER};
        pluralRangeData[87] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[88] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[89] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY};
        pluralRangeData[90] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER};
        pluralRangeData[91] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER};
        pluralRangeData[92] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[93] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[94] = new String[]{"locales", "cs pl sk"};
        pluralRangeData[95] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[96] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[97] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[98] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[99] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[100] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[101] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[102] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[103] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[104] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[105] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[106] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[107] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[108] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[109] = new String[]{"locales", "lt ru uk"};
        pluralRangeData[110] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[111] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[112] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[113] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[114] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[115] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[116] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[117] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[118] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[119] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[120] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[121] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[122] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[123] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[124] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[125] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[126] = new String[]{"locales", "cy"};
        pluralRangeData[127] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[128] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO};
        pluralRangeData[129] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[130] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[131] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[132] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO};
        pluralRangeData[133] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[134] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[135] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[136] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[137] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[138] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[139] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[140] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[141] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[142] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ONE};
        pluralRangeData[143] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_TWO};
        pluralRangeData[144] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[145] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[146] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[147] = new String[]{"locales", "ar"};
        pluralRangeData[148] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_ZERO};
        pluralRangeData[149] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_ZERO};
        pluralRangeData[150] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[151] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[152] = new String[]{PluralRules.KEYWORD_ZERO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[153] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER};
        pluralRangeData[154] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[155] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[156] = new String[]{PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[157] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[158] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[159] = new String[]{PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[160] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[161] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[162] = new String[]{PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[163] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[164] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[165] = new String[]{PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        pluralRangeData[166] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_ONE, PluralRules.KEYWORD_OTHER};
        pluralRangeData[167] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_TWO, PluralRules.KEYWORD_OTHER};
        pluralRangeData[168] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_FEW, PluralRules.KEYWORD_FEW};
        pluralRangeData[169] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_MANY, PluralRules.KEYWORD_MANY};
        pluralRangeData[170] = new String[]{PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER, PluralRules.KEYWORD_OTHER};
        String[] locales = null;
        HashMap<String, PluralRanges> tempLocaleIdToPluralRanges = new HashMap();
        PluralRanges pr = null;
        for (String[] row : pluralRangeData) {
            if (row[0].equals("locales")) {
                if (pr != null) {
                    pr.freeze();
                    for (String locale : locales) {
                        tempLocaleIdToPluralRanges.put(locale, pr);
                    }
                }
                locales = row[1].split(Padder.FALLBACK_PADDING_STRING);
                pr = new PluralRanges();
            } else {
                pr.add(StandardPlural.fromString(row[0]), StandardPlural.fromString(row[1]), StandardPlural.fromString(row[2]));
            }
        }
        int pr2 = locales.length;
        while (i < pr2) {
            tempLocaleIdToPluralRanges.put(locales[i], pr);
            i++;
        }
        localeIdToPluralRanges = Collections.unmodifiableMap(tempLocaleIdToPluralRanges);
    }

    public boolean hasOverride(ULocale locale) {
        return false;
    }

    public PluralRanges getPluralRanges(ULocale locale) {
        String localeId = ULocale.canonicalize(locale.getBaseName());
        while (true) {
            PluralRanges pluralRanges = (PluralRanges) localeIdToPluralRanges.get(localeId);
            PluralRanges result = pluralRanges;
            if (pluralRanges != null) {
                return result;
            }
            int ix = localeId.lastIndexOf(BaseLocale.SEP);
            if (ix == -1) {
                return UNKNOWN_RANGE;
            }
            localeId = localeId.substring(0, ix);
        }
    }

    public boolean isPluralRangesAvailable(ULocale locale) {
        return getPluralRanges(locale) == UNKNOWN_RANGE;
    }
}
