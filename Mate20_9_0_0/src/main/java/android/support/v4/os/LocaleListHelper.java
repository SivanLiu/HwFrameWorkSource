package android.support.v4.os;

import android.os.Build.VERSION;
import android.support.annotation.GuardedBy;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.annotation.Size;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

@RestrictTo({Scope.LIBRARY_GROUP})
final class LocaleListHelper {
    private static final Locale EN_LATN = LocaleHelper.forLanguageTag("en-Latn");
    private static final Locale LOCALE_AR_XB = new Locale("ar", "XB");
    private static final Locale LOCALE_EN_XA = new Locale("en", "XA");
    private static final int NUM_PSEUDO_LOCALES = 2;
    private static final String STRING_AR_XB = "ar-XB";
    private static final String STRING_EN_XA = "en-XA";
    @GuardedBy("sLock")
    private static LocaleListHelper sDefaultAdjustedLocaleList = null;
    @GuardedBy("sLock")
    private static LocaleListHelper sDefaultLocaleList = null;
    private static final Locale[] sEmptyList = new Locale[0];
    private static final LocaleListHelper sEmptyLocaleList = new LocaleListHelper(new Locale[0]);
    @GuardedBy("sLock")
    private static Locale sLastDefaultLocale = null;
    @GuardedBy("sLock")
    private static LocaleListHelper sLastExplicitlySetLocaleList = null;
    private static final Object sLock = new Object();
    private final Locale[] mList;
    @NonNull
    private final String mStringRepresentation;

    @RestrictTo({Scope.LIBRARY_GROUP})
    Locale get(int index) {
        return (index < 0 || index >= this.mList.length) ? null : this.mList[index];
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    boolean isEmpty() {
        return this.mList.length == 0;
    }

    @IntRange(from = 0)
    @RestrictTo({Scope.LIBRARY_GROUP})
    int size() {
        return this.mList.length;
    }

    @IntRange(from = -1)
    @RestrictTo({Scope.LIBRARY_GROUP})
    int indexOf(Locale locale) {
        for (int i = 0; i < this.mList.length; i++) {
            if (this.mList[i].equals(locale)) {
                return i;
            }
        }
        return -1;
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof LocaleListHelper)) {
            return false;
        }
        Locale[] otherList = ((LocaleListHelper) other).mList;
        if (this.mList.length != otherList.length) {
            return false;
        }
        for (int i = 0; i < this.mList.length; i++) {
            if (!this.mList[i].equals(otherList[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int result = 1;
        for (Locale hashCode : this.mList) {
            result = (31 * result) + hashCode.hashCode();
        }
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < this.mList.length; i++) {
            sb.append(this.mList[i]);
            if (i < this.mList.length - 1) {
                sb.append(',');
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    @NonNull
    String toLanguageTags() {
        return this.mStringRepresentation;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    LocaleListHelper(@NonNull Locale... list) {
        if (list.length == 0) {
            this.mList = sEmptyList;
            this.mStringRepresentation = "";
            return;
        }
        Locale[] localeList = new Locale[list.length];
        HashSet<Locale> seenLocales = new HashSet();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < list.length) {
            Locale l = list[i];
            StringBuilder stringBuilder;
            if (l == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("list[");
                stringBuilder.append(i);
                stringBuilder.append("] is null");
                throw new NullPointerException(stringBuilder.toString());
            } else if (seenLocales.contains(l)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("list[");
                stringBuilder.append(i);
                stringBuilder.append("] is a repetition");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else {
                Locale localeClone = (Locale) l.clone();
                localeList[i] = localeClone;
                sb.append(LocaleHelper.toLanguageTag(localeClone));
                if (i < list.length - 1) {
                    sb.append(',');
                }
                seenLocales.add(localeClone);
                i++;
            }
        }
        this.mList = localeList;
        this.mStringRepresentation = sb.toString();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    LocaleListHelper(@NonNull Locale topLocale, LocaleListHelper otherLocales) {
        if (topLocale != null) {
            int i;
            int i2 = 0;
            int inputLength = otherLocales == null ? 0 : otherLocales.mList.length;
            int topLocaleIndex = -1;
            for (i = 0; i < inputLength; i++) {
                if (topLocale.equals(otherLocales.mList[i])) {
                    topLocaleIndex = i;
                    break;
                }
            }
            int outputLength = (topLocaleIndex == -1 ? 1 : 0) + inputLength;
            Locale[] localeList = new Locale[outputLength];
            localeList[0] = (Locale) topLocale.clone();
            if (topLocaleIndex == -1) {
                for (i = 0; i < inputLength; i++) {
                    localeList[i + 1] = (Locale) otherLocales.mList[i].clone();
                }
            } else {
                for (i = 0; i < topLocaleIndex; i++) {
                    localeList[i + 1] = (Locale) otherLocales.mList[i].clone();
                }
                for (i = topLocaleIndex + 1; i < inputLength; i++) {
                    localeList[i] = (Locale) otherLocales.mList[i].clone();
                }
            }
            StringBuilder sb = new StringBuilder();
            while (i2 < outputLength) {
                sb.append(LocaleHelper.toLanguageTag(localeList[i2]));
                if (i2 < outputLength - 1) {
                    sb.append(',');
                }
                i2++;
            }
            this.mList = localeList;
            this.mStringRepresentation = sb.toString();
            return;
        }
        throw new NullPointerException("topLocale is null");
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    @NonNull
    static LocaleListHelper getEmptyLocaleList() {
        return sEmptyLocaleList;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    @NonNull
    static LocaleListHelper forLanguageTags(@Nullable String list) {
        if (list == null || list.isEmpty()) {
            return getEmptyLocaleList();
        }
        String[] tags = list.split(",", -1);
        Locale[] localeArray = new Locale[tags.length];
        for (int i = 0; i < localeArray.length; i++) {
            localeArray[i] = LocaleHelper.forLanguageTag(tags[i]);
        }
        return new LocaleListHelper(localeArray);
    }

    private static String getLikelyScript(Locale locale) {
        if (VERSION.SDK_INT < 21) {
            return "";
        }
        String script = locale.getScript();
        if (script.isEmpty()) {
            return "";
        }
        return script;
    }

    private static boolean isPseudoLocale(String locale) {
        return STRING_EN_XA.equals(locale) || STRING_AR_XB.equals(locale);
    }

    private static boolean isPseudoLocale(Locale locale) {
        return LOCALE_EN_XA.equals(locale) || LOCALE_AR_XB.equals(locale);
    }

    @IntRange(from = 0, to = 1)
    private static int matchScore(Locale supported, Locale desired) {
        int i = 1;
        if (supported.equals(desired)) {
            return 1;
        }
        if (!supported.getLanguage().equals(desired.getLanguage()) || isPseudoLocale(supported) || isPseudoLocale(desired)) {
            return 0;
        }
        String supportedScr = getLikelyScript(supported);
        if (!supportedScr.isEmpty()) {
            return supportedScr.equals(getLikelyScript(desired));
        }
        String supportedRegion = supported.getCountry();
        if (!(supportedRegion.isEmpty() || supportedRegion.equals(desired.getCountry()))) {
            i = 0;
        }
        return i;
    }

    private int findFirstMatchIndex(Locale supportedLocale) {
        for (int idx = 0; idx < this.mList.length; idx++) {
            if (matchScore(supportedLocale, this.mList[idx]) > 0) {
                return idx;
            }
        }
        return Integer.MAX_VALUE;
    }

    private int computeFirstMatchIndex(Collection<String> supportedLocales, boolean assumeEnglishIsSupported) {
        if (this.mList.length == 1) {
            return 0;
        }
        if (this.mList.length == 0) {
            return -1;
        }
        int bestIndex = Integer.MAX_VALUE;
        if (assumeEnglishIsSupported) {
            int idx = findFirstMatchIndex(EN_LATN);
            if (idx == 0) {
                return 0;
            }
            if (idx < Integer.MAX_VALUE) {
                bestIndex = idx;
            }
        }
        for (String languageTag : supportedLocales) {
            int idx2 = findFirstMatchIndex(LocaleHelper.forLanguageTag(languageTag));
            if (idx2 == 0) {
                return 0;
            }
            if (idx2 < bestIndex) {
                bestIndex = idx2;
            }
        }
        if (bestIndex == Integer.MAX_VALUE) {
            return 0;
        }
        return bestIndex;
    }

    private Locale computeFirstMatch(Collection<String> supportedLocales, boolean assumeEnglishIsSupported) {
        int bestIndex = computeFirstMatchIndex(supportedLocales, assumeEnglishIsSupported);
        return bestIndex == -1 ? null : this.mList[bestIndex];
    }

    @Nullable
    @RestrictTo({Scope.LIBRARY_GROUP})
    Locale getFirstMatch(String[] supportedLocales) {
        return computeFirstMatch(Arrays.asList(supportedLocales), false);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    int getFirstMatchIndex(String[] supportedLocales) {
        return computeFirstMatchIndex(Arrays.asList(supportedLocales), false);
    }

    @Nullable
    @RestrictTo({Scope.LIBRARY_GROUP})
    Locale getFirstMatchWithEnglishSupported(String[] supportedLocales) {
        return computeFirstMatch(Arrays.asList(supportedLocales), true);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    int getFirstMatchIndexWithEnglishSupported(Collection<String> supportedLocales) {
        return computeFirstMatchIndex(supportedLocales, true);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    int getFirstMatchIndexWithEnglishSupported(String[] supportedLocales) {
        return getFirstMatchIndexWithEnglishSupported(Arrays.asList(supportedLocales));
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    static boolean isPseudoLocalesOnly(@Nullable String[] supportedLocales) {
        if (supportedLocales == null) {
            return true;
        }
        if (supportedLocales.length > 3) {
            return false;
        }
        for (String locale : supportedLocales) {
            if (!locale.isEmpty() && !isPseudoLocale(locale)) {
                return false;
            }
        }
        return true;
    }

    @Size(min = 1)
    @RestrictTo({Scope.LIBRARY_GROUP})
    @NonNull
    static LocaleListHelper getDefault() {
        Locale defaultLocale = Locale.getDefault();
        synchronized (sLock) {
            LocaleListHelper localeListHelper;
            if (!defaultLocale.equals(sLastDefaultLocale)) {
                sLastDefaultLocale = defaultLocale;
                if (sDefaultLocaleList == null || !defaultLocale.equals(sDefaultLocaleList.get(0))) {
                    sDefaultLocaleList = new LocaleListHelper(defaultLocale, sLastExplicitlySetLocaleList);
                    sDefaultAdjustedLocaleList = sDefaultLocaleList;
                } else {
                    localeListHelper = sDefaultLocaleList;
                    return localeListHelper;
                }
            }
            localeListHelper = sDefaultLocaleList;
            return localeListHelper;
        }
    }

    @Size(min = 1)
    @NonNull
    static LocaleListHelper getAdjustedDefault() {
        LocaleListHelper localeListHelper;
        getDefault();
        synchronized (sLock) {
            localeListHelper = sDefaultAdjustedLocaleList;
        }
        return localeListHelper;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    static void setDefault(@Size(min = 1) @NonNull LocaleListHelper locales) {
        setDefault(locales, 0);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    static void setDefault(@Size(min = 1) @NonNull LocaleListHelper locales, int localeIndex) {
        if (locales == null) {
            throw new NullPointerException("locales is null");
        } else if (locales.isEmpty()) {
            throw new IllegalArgumentException("locales is empty");
        } else {
            synchronized (sLock) {
                sLastDefaultLocale = locales.get(localeIndex);
                Locale.setDefault(sLastDefaultLocale);
                sLastExplicitlySetLocaleList = locales;
                sDefaultLocaleList = locales;
                if (localeIndex == 0) {
                    sDefaultAdjustedLocaleList = sDefaultLocaleList;
                } else {
                    sDefaultAdjustedLocaleList = new LocaleListHelper(sLastDefaultLocale, sDefaultLocaleList);
                }
            }
        }
    }
}
