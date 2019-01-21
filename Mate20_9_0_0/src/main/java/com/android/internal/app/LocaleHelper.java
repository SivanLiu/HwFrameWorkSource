package com.android.internal.app;

import android.icu.text.ListFormatter;
import android.icu.util.ULocale;
import android.os.LocaleList;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import com.android.internal.app.LocaleStore.LocaleInfo;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import libcore.icu.ICU;

public class LocaleHelper {
    private static final boolean IS_HIDE_COUNTRY_NAME = SystemProperties.getBoolean("ro.config.hw_hide_country_name", false);
    private static final boolean isCN = SystemProperties.get("ro.product.locale.region").equals("CN");
    private static final boolean isDT = SystemProperties.get("ro.config.hw_opta", "0").equals("150");
    private static final boolean isDomesticVersion;
    private static final boolean isMKD = SystemProperties.get("ro.config.hw_optb", "0").equals("807");
    private static final boolean isZH = SystemProperties.get("ro.product.locale.language").equals("zh");

    public static final class LocaleInfoComparator implements Comparator<LocaleInfo> {
        private static final String PREFIX_ARABIC = "ال";
        private final Collator mCollator;
        private final boolean mCountryMode;

        public LocaleInfoComparator(Locale sortLocale, boolean countryMode) {
            this.mCollator = Collator.getInstance(sortLocale);
            this.mCountryMode = countryMode;
        }

        private String removePrefixForCompare(Locale locale, String str) {
            if ("ar".equals(locale.getLanguage()) && str.startsWith(PREFIX_ARABIC)) {
                return str.substring(PREFIX_ARABIC.length());
            }
            return str;
        }

        public int compare(LocaleInfo lhs, LocaleInfo rhs) {
            if (lhs.isSuggested() == rhs.isSuggested()) {
                return this.mCollator.compare(removePrefixForCompare(lhs.getLocale(), lhs.getLabel(this.mCountryMode)), removePrefixForCompare(rhs.getLocale(), rhs.getLabel(this.mCountryMode)));
            }
            return lhs.isSuggested() ? -1 : 1;
        }
    }

    static {
        boolean z = isZH && isCN;
        isDomesticVersion = z;
    }

    public static String toSentenceCase(String str, Locale locale) {
        if (str.isEmpty()) {
            return str;
        }
        int firstCodePointLen = str.offsetByCodePoints(0, 1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str.substring(0, firstCodePointLen).toUpperCase(locale));
        stringBuilder.append(str.substring(firstCodePointLen));
        return stringBuilder.toString();
    }

    public static String normalizeForSearch(String str, Locale locale) {
        return str.toUpperCase();
    }

    private static boolean shouldUseDialectName(Locale locale) {
        String lang = locale.getLanguage();
        return "fa".equals(lang) || "zh".equals(lang);
    }

    public static String getDisplayName(Locale locale, Locale displayLocale, boolean sentenceCase) {
        String result;
        Locale systemLocale = Locale.getDefault();
        if ("my".equals(locale.getLanguage()) && "my".equals(displayLocale.getLanguage())) {
            if ("ZG".equals(systemLocale.getCountry())) {
                displayLocale = Locale.forLanguageTag("my-ZG");
            } else {
                displayLocale = Locale.forLanguageTag("my");
            }
        }
        ULocale displayULocale = ULocale.forLocale(displayLocale);
        String[] specialCode1 = new String[]{"ar_XB", "en_XA", "zz_ZX", "zz"};
        String[] specialNames1 = new String[]{"[Bidirection test locale]", "[Pseudo locale]", "[DBID version]", "[DBID version]"};
        String[] specialCode3 = new String[]{"mk_MK", "mk"};
        String[] specialNames3 = new String[]{"FYROM", "FYROM"};
        String[] specialNames4 = new String[]{"Macedonian (Macedonia)", "Macedonian (Macedonia)"};
        if (shouldUseDialectName(locale)) {
            result = ULocale.getDisplayNameWithDialect(locale.toLanguageTag(), displayULocale);
        } else {
            result = ULocale.getDisplayName(locale.toLanguageTag(), displayULocale);
        }
        result = getDisplayName(locale, specialCode1, specialNames1, result);
        if ((isGreeceSIM() && (!isDT || !isMKD)) || (isDT && !isMKD)) {
            result = getDisplayName(locale, specialCode3, specialNames3, result);
        } else if (displayLocale.toString().startsWith("en")) {
            result = getDisplayName(locale, specialCode3, specialNames4, result);
        }
        if (IS_HIDE_COUNTRY_NAME) {
            result = result.replace("(", "（").split("（")[0];
        }
        if (isDomesticVersion) {
            result = replaceTaiwan2TaiwanChina(locale, displayLocale, result);
        }
        return sentenceCase ? toSentenceCase(result, displayLocale) : result;
    }

    public static String replaceTaiwan2TaiwanChina(Locale locale, Locale displayLocale, String result) {
        String localLanguage = displayLocale.getLanguage();
        String localCountry = locale.getCountry();
        String localScript = displayLocale.getScript();
        String simplifiedTaiwan = "台湾";
        String tranditionalTaiwan = "台灣";
        String englishTaiwan = "Taiwan";
        String simplifiedTaiwanChina = "中国台湾";
        String tranditionalTaiwanChina = "中國台灣";
        String englishTaiwanChina = "Taiwan,China";
        if (result.isEmpty()) {
            return result;
        }
        if ("zh".equals(localLanguage) && "TW".equals(localCountry)) {
            if ("Hant".equals(localScript)) {
                result = result.replace("台灣", "中國台灣");
            } else {
                result = result.replace("台湾", "中国台湾");
            }
        }
        if ("en".equals(localLanguage) && "TW".equals(localCountry)) {
            result = result.replace("Taiwan", "Taiwan,China");
        }
        return result;
    }

    public static String getDisplayName(Locale locale, boolean sentenceCase) {
        return getDisplayName(locale, Locale.getDefault(), sentenceCase);
    }

    private static String getDisplayName(Locale locale, String[] specialLocaleCodes, String[] specialLocaleNames, String originalStr) {
        String code = locale.toString();
        for (int i = 0; i < specialLocaleCodes.length; i++) {
            if (specialLocaleCodes[i].equals(code)) {
                return specialLocaleNames[i];
            }
        }
        return originalStr;
    }

    private static boolean isGreeceSIM() {
        ArrayList<String> mccList = new ArrayList();
        mccList.add("202");
        String simOperator = SystemProperties.get("persist.sys.mcc_match_fyrom");
        if (simOperator == null || simOperator.length() < 4) {
            return false;
        }
        if (simOperator.charAt(0) == ',') {
            simOperator = simOperator.substring(1);
        }
        if (mccList.contains(simOperator.substring(0, 3))) {
            return true;
        }
        return false;
    }

    public static String getDisplayCountry(Locale locale, Locale displayLocale) {
        Locale systemLocale = Locale.getDefault();
        if ("my".equals(locale.getLanguage()) && "my".equals(displayLocale.getLanguage())) {
            if ("ZG".equals(systemLocale.getCountry())) {
                displayLocale = Locale.forLanguageTag("my-ZG");
            } else {
                displayLocale = Locale.forLanguageTag("my");
            }
        }
        String country = ULocale.getDisplayCountry(locale.toLanguageTag(), ULocale.forLocale(displayLocale));
        if (locale.getUnicodeLocaleType("nu") != null) {
            return String.format("%s (%s)", new Object[]{country, ULocale.getDisplayKeywordValue(languageTag, "numbers", uDisplayLocale)});
        } else if (isDomesticVersion) {
            return replaceTaiwan2TaiwanChina(locale, displayLocale, country);
        } else {
            return country;
        }
    }

    public static String getDisplayCountry(Locale locale) {
        return ULocale.getDisplayCountry(locale.toLanguageTag(), ULocale.getDefault());
    }

    public static String getDisplayLocaleList(LocaleList locales, Locale displayLocale, int maxLocales) {
        int localeCount;
        int listCount;
        Locale dispLocale = displayLocale == null ? Locale.getDefault() : displayLocale;
        boolean ellipsisNeeded = locales.size() > maxLocales;
        if (ellipsisNeeded) {
            localeCount = maxLocales;
            listCount = maxLocales + 1;
        } else {
            listCount = locales.size();
            localeCount = listCount;
        }
        String[] localeNames = new String[listCount];
        for (int i = 0; i < localeCount; i++) {
            localeNames[i] = getDisplayName(locales.get(i), dispLocale, false);
        }
        if (ellipsisNeeded) {
            localeNames[maxLocales] = TextUtils.getEllipsisString(TruncateAt.END);
        }
        return ListFormatter.getInstance(dispLocale).format((Object[]) localeNames);
    }

    public static Locale addLikelySubtags(Locale locale) {
        return ICU.addLikelySubtags(locale);
    }
}
