package com.android.internal.app;

import android.content.Context;
import android.os.LocaleList;
import android.provider.Settings.Global;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.content.NativeLibraryHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Builder;
import java.util.Set;

public class LocaleStore {
    private static boolean sFullyInitialized = false;
    private static final HashMap<String, LocaleInfo> sLocaleCache = new HashMap();

    public static class LocaleInfo {
        private static final int SUGGESTION_TYPE_CFG = 2;
        private static final int SUGGESTION_TYPE_NONE = 0;
        private static final int SUGGESTION_TYPE_SIM = 1;
        private String mFullCountryNameNative;
        private String mFullNameNative;
        private final String mId;
        private boolean mIsChecked;
        private boolean mIsPseudo;
        private boolean mIsTranslated;
        private String mLangScriptKey;
        private final Locale mLocale;
        private final Locale mParent;
        private int mSuggestionFlags;

        static /* synthetic */ int access$076(LocaleInfo x0, int x1) {
            int i = x0.mSuggestionFlags | x1;
            x0.mSuggestionFlags = i;
            return i;
        }

        private LocaleInfo(Locale locale) {
            this.mLocale = locale;
            this.mId = locale.toLanguageTag();
            this.mParent = getParent(locale);
            this.mIsChecked = false;
            this.mSuggestionFlags = 0;
            this.mIsTranslated = false;
            this.mIsPseudo = false;
        }

        private LocaleInfo(String localeId) {
            this(Locale.forLanguageTag(localeId));
        }

        private static Locale getParent(Locale locale) {
            if (locale.getCountry().isEmpty()) {
                return null;
            }
            Builder localeBuilder = new Builder().setLanguageTag("en");
            try {
                localeBuilder = new Builder().setLocale(locale).setRegion("");
            } catch (IllformedLocaleException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error locale: ");
                stringBuilder.append(locale.toLanguageTag());
                Log.e("LocaleStore", stringBuilder.toString());
            }
            return localeBuilder.setExtension('u', "").build();
        }

        public String toString() {
            return this.mId;
        }

        public Locale getLocale() {
            return this.mLocale;
        }

        public Locale getParent() {
            return this.mParent;
        }

        public String getId() {
            return this.mId;
        }

        public boolean isTranslated() {
            return this.mIsTranslated;
        }

        public void setTranslated(boolean isTranslated) {
            this.mIsTranslated = isTranslated;
        }

        boolean isSuggested() {
            boolean z = false;
            if (!this.mIsTranslated) {
                return false;
            }
            if (this.mSuggestionFlags != 0) {
                z = true;
            }
            return z;
        }

        private boolean isSuggestionOfType(int suggestionMask) {
            boolean z = false;
            if (!this.mIsTranslated) {
                return false;
            }
            if ((this.mSuggestionFlags & suggestionMask) == suggestionMask) {
                z = true;
            }
            return z;
        }

        public String getFullNameNative() {
            if (this.mFullNameNative == null) {
                this.mFullNameNative = LocaleHelper.getDisplayName(this.mLocale, this.mLocale, true);
            }
            return this.mFullNameNative;
        }

        String getFullCountryNameNative() {
            if (this.mFullCountryNameNative == null) {
                this.mFullCountryNameNative = LocaleHelper.getDisplayCountry(this.mLocale, this.mLocale);
            }
            return this.mFullCountryNameNative;
        }

        String getFullCountryNameInUiLanguage() {
            return LocaleHelper.getDisplayCountry(this.mLocale);
        }

        public String getFullNameInUiLanguage() {
            return LocaleHelper.getDisplayName(this.mLocale, true);
        }

        private String getLangScriptKey() {
            if (this.mLangScriptKey == null) {
                String toLanguageTag;
                Locale parentWithScript = getParent(LocaleHelper.addLikelySubtags(new Builder().setLocale(this.mLocale).setExtension('u', "").build()));
                if (parentWithScript == null) {
                    toLanguageTag = this.mLocale.toLanguageTag();
                } else {
                    toLanguageTag = parentWithScript.toLanguageTag();
                }
                this.mLangScriptKey = toLanguageTag;
            }
            return this.mLangScriptKey;
        }

        String getLabel(boolean countryMode) {
            if (countryMode) {
                return getFullCountryNameNative();
            }
            return getFullNameNative();
        }

        String getContentDescription(boolean countryMode) {
            if (countryMode) {
                return getFullCountryNameInUiLanguage();
            }
            return getFullNameInUiLanguage();
        }

        public boolean getChecked() {
            return this.mIsChecked;
        }

        public void setChecked(boolean checked) {
            this.mIsChecked = checked;
        }
    }

    private static Set<String> getSimCountries(Context context) {
        Set<String> result = new HashSet();
        TelephonyManager tm = TelephonyManager.from(context);
        if (tm != null) {
            String iso = tm.getSimCountryIso().toUpperCase(Locale.US);
            if (!iso.isEmpty()) {
                result.add(iso);
                if ("MM".equals(iso)) {
                    result.add("ZG");
                }
            }
            iso = tm.getNetworkCountryIso().toUpperCase(Locale.US);
            if (!iso.isEmpty()) {
                result.add(iso);
                if ("MM".equals(iso)) {
                    result.add("ZG");
                }
            }
        }
        return result;
    }

    public static void updateSimCountries(Context context) {
        Set<String> simCountries = getSimCountries(context);
        for (LocaleInfo li : sLocaleCache.values()) {
            if (simCountries.contains(li.getLocale().getCountry())) {
                LocaleInfo.access$076(li, 1);
            }
        }
    }

    private static void addSuggestedLocalesForRegion(Locale locale) {
        if (locale != null) {
            String country = locale.getCountry();
            if (!country.isEmpty()) {
                for (LocaleInfo li : sLocaleCache.values()) {
                    if (country.equals(li.getLocale().getCountry())) {
                        LocaleInfo.access$076(li, 1);
                    }
                    if ("ZG".equals(country) && country.equals(li.getLocale().getCountry())) {
                        LocaleInfo.access$076(li, 1);
                    }
                }
            }
        }
    }

    public static void fillCache(Context context) {
        if (!sFullyInitialized) {
            LocaleInfo li;
            Set<String> simCountries = getSimCountries(context);
            int i = 0;
            boolean isInDeveloperMode = Global.getInt(context.getContentResolver(), "development_settings_enabled", 0) != 0;
            for (String localeId : LocalePicker.getSupportedLocales(context)) {
                if (localeId.isEmpty()) {
                    throw new IllformedLocaleException("Bad locale entry in locale_config.xml");
                }
                li = new LocaleInfo(localeId, null);
                if (LocaleList.isPseudoLocale(li.getLocale())) {
                    if (isInDeveloperMode) {
                        li.setTranslated(true);
                        li.mIsPseudo = true;
                        LocaleInfo.access$076(li, 1);
                    } else {
                    }
                }
                if (simCountries.contains(li.getLocale().getCountry())) {
                    LocaleInfo.access$076(li, 1);
                }
                sLocaleCache.put(li.getId(), li);
                Locale parent = li.getParent();
                if (parent != null) {
                    String parentId = parent.toLanguageTag();
                    if (!sLocaleCache.containsKey(parentId)) {
                        sLocaleCache.put(parentId, new LocaleInfo(parent, null));
                    }
                }
            }
            HashSet<String> localizedLocales = new HashSet();
            String[] systemAssetLocales = LocalePicker.getSystemAssetLocales();
            int length = systemAssetLocales.length;
            while (i < length) {
                li = new LocaleInfo(systemAssetLocales[i], null);
                String country = li.getLocale().getCountry();
                if (!country.isEmpty()) {
                    LocaleInfo cachedLocale = null;
                    if (sLocaleCache.containsKey(li.getId())) {
                        cachedLocale = (LocaleInfo) sLocaleCache.get(li.getId());
                    } else {
                        String langScriptCtry = new StringBuilder();
                        langScriptCtry.append(li.getLangScriptKey());
                        langScriptCtry.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                        langScriptCtry.append(country);
                        langScriptCtry = langScriptCtry.toString();
                        if (sLocaleCache.containsKey(langScriptCtry)) {
                            cachedLocale = (LocaleInfo) sLocaleCache.get(langScriptCtry);
                        }
                    }
                    if (cachedLocale != null) {
                        LocaleInfo.access$076(cachedLocale, 2);
                    }
                }
                localizedLocales.add(li.getLangScriptKey());
                i++;
            }
            for (LocaleInfo li2 : sLocaleCache.values()) {
                li2.setTranslated(localizedLocales.contains(li2.getLangScriptKey()));
            }
            addSuggestedLocalesForRegion(Locale.getDefault());
            sFullyInitialized = true;
        }
    }

    private static int getLevel(Set<String> ignorables, LocaleInfo li, boolean translatedOnly) {
        if (ignorables.contains(li.getId())) {
            return 0;
        }
        if (li.mIsPseudo) {
            return 2;
        }
        if ((!translatedOnly || li.isTranslated()) && li.getParent() != null) {
            return 2;
        }
        return 0;
    }

    public static Set<LocaleInfo> getLevelLocales(Context context, Set<String> ignorables, LocaleInfo parent, boolean translatedOnly) {
        Log.i("LocaleStore", String.valueOf(System.currentTimeMillis()));
        fillCache(context);
        String parentId = parent == null ? null : parent.getId();
        HashSet<LocaleInfo> result = new HashSet();
        List<String> regularLocales = new ArrayList();
        HashMap<String, LocaleInfo> languageLocales = new HashMap();
        List<String> blackList = LocalePicker.getBlackLanguage(context);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("blackList: ");
        stringBuilder.append(blackList.toString());
        Log.i("LocaleStore", stringBuilder.toString());
        HashMap<String, LocaleInfo> localCache = new HashMap();
        localCache.putAll(sLocaleCache);
        Set<String> tIgnorables = new HashSet();
        for (String localeStr : ignorables) {
            if (localeStr.length() >= 2 && "my".equals(localeStr.substring(0, 2)) && localeStr.contains("-u-nu-latn")) {
                tIgnorables.add(localeStr.replace("-u-nu-latn", ""));
            } else {
                tIgnorables.add(localeStr);
            }
        }
        for (LocaleInfo li : localCache.values()) {
            regularLocales.add(li.toString());
            if (li.getParent() == null) {
                languageLocales.put(li.toString(), li);
            }
            if (getLevel(tIgnorables, li, translatedOnly) == 2) {
                if (parent != null) {
                    if (parentId.equals(li.getParent().toLanguageTag()) && !blackList.contains(li.getId())) {
                        result.add(li);
                    }
                } else if (!li.isSuggestionOfType(1) || blackList.contains(li.getId())) {
                    result.add(getLocaleInfo(li.getParent()));
                } else {
                    result.add(li);
                }
            }
        }
        boolean z = translatedOnly;
        for (LocaleInfo li2 : languageLocales.values()) {
            for (String country : getCountries(context)) {
                if (!"".equals(country)) {
                    String localeId = new StringBuilder();
                    localeId.append(li2.toString());
                    localeId.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                    localeId.append(country);
                    localeId = localeId.toString();
                    if (!regularLocales.contains(localeId)) {
                        if (!blackList.contains(localeId)) {
                            if (!tIgnorables.contains(localeId)) {
                                if (parent == null) {
                                    result.add(li2);
                                } else if (parentId.equals(li2.toString())) {
                                    result.add(new LocaleInfo(localeId, null));
                                }
                            }
                        }
                    }
                }
            }
        }
        Log.i("LocaleStore", String.valueOf(System.currentTimeMillis()));
        return result;
    }

    private static Set<String> getCountries(Context context) {
        Set<String> countries = new HashSet();
        for (String localeId : context.getResources().getStringArray(17236083)) {
            countries.add(Locale.forLanguageTag(localeId).getCountry());
        }
        countries.remove("");
        countries.remove("XA");
        countries.remove("XB");
        countries.remove("ZG");
        countries.remove("ZX");
        return countries;
    }

    public static LocaleInfo getLocaleInfo(Locale locale) {
        String id = locale.toLanguageTag();
        if (sLocaleCache.containsKey(id)) {
            return (LocaleInfo) sLocaleCache.get(id);
        }
        LocaleInfo result = new LocaleInfo(locale, null);
        sLocaleCache.put(id, result);
        return result;
    }
}
