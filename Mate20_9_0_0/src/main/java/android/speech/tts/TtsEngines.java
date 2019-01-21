package android.speech.tts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.provider.Settings.Secure;
import android.provider.SettingsStringUtil;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import org.xmlpull.v1.XmlPullParserException;

public class TtsEngines {
    private static final boolean DBG = false;
    private static final String LOCALE_DELIMITER_NEW = "_";
    private static final String LOCALE_DELIMITER_OLD = "-";
    private static final String TAG = "TtsEngines";
    private static final String XML_TAG_NAME = "tts-engine";
    private static final Map<String, String> sNormalizeCountry;
    private static final Map<String, String> sNormalizeLanguage;
    private final Context mContext;

    private static class EngineInfoComparator implements Comparator<EngineInfo> {
        static EngineInfoComparator INSTANCE = new EngineInfoComparator();

        private EngineInfoComparator() {
        }

        public int compare(EngineInfo lhs, EngineInfo rhs) {
            if (lhs.system && !rhs.system) {
                return -1;
            }
            if (!rhs.system || lhs.system) {
                return rhs.priority - lhs.priority;
            }
            return 1;
        }
    }

    static {
        String language;
        HashMap<String, String> normalizeLanguage = new HashMap();
        int i = 0;
        for (String language2 : Locale.getISOLanguages()) {
            try {
                normalizeLanguage.put(new Locale(language2).getISO3Language(), language2);
            } catch (MissingResourceException e) {
            }
        }
        sNormalizeLanguage = Collections.unmodifiableMap(normalizeLanguage);
        HashMap<String, String> normalizeCountry = new HashMap();
        String[] iSOCountries = Locale.getISOCountries();
        int length = iSOCountries.length;
        while (i < length) {
            language2 = iSOCountries[i];
            try {
                normalizeCountry.put(new Locale("", language2).getISO3Country(), language2);
            } catch (MissingResourceException e2) {
            }
            i++;
        }
        sNormalizeCountry = Collections.unmodifiableMap(normalizeCountry);
    }

    public TtsEngines(Context ctx) {
        this.mContext = ctx;
    }

    public String getDefaultEngine() {
        String engine = Secure.getString(this.mContext.getContentResolver(), Secure.TTS_DEFAULT_SYNTH);
        return isEngineInstalled(engine) ? engine : getHighestRankedEngineName();
    }

    public String getHighestRankedEngineName() {
        List<EngineInfo> engines = getEngines();
        if (engines.size() <= 0 || !((EngineInfo) engines.get(0)).system) {
            return null;
        }
        return ((EngineInfo) engines.get(0)).name;
    }

    public EngineInfo getEngineInfo(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = new Intent(Engine.INTENT_ACTION_TTS_SERVICE);
        intent.setPackage(packageName);
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent, 65536);
        if (resolveInfos == null || resolveInfos.size() != 1) {
            return null;
        }
        return getEngineInfo((ResolveInfo) resolveInfos.get(0), pm);
    }

    public List<EngineInfo> getEngines() {
        PackageManager pm = this.mContext.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(new Intent(Engine.INTENT_ACTION_TTS_SERVICE), 65536);
        if (resolveInfos == null) {
            return Collections.emptyList();
        }
        List<EngineInfo> engines = new ArrayList(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            EngineInfo engine = getEngineInfo(resolveInfo, pm);
            if (engine != null) {
                engines.add(engine);
            }
        }
        Collections.sort(engines, EngineInfoComparator.INSTANCE);
        return engines;
    }

    private boolean isSystemEngine(ServiceInfo info) {
        ApplicationInfo appInfo = info.applicationInfo;
        return (appInfo == null || (appInfo.flags & 1) == 0) ? false : true;
    }

    public boolean isEngineInstalled(String engine) {
        boolean z = false;
        if (engine == null) {
            return false;
        }
        if (getEngineInfo(engine) != null) {
            z = true;
        }
        return z;
    }

    public Intent getSettingsIntent(String engine) {
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = new Intent(Engine.INTENT_ACTION_TTS_SERVICE);
        intent.setPackage(engine);
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent, 65664);
        if (resolveInfos != null && resolveInfos.size() == 1) {
            ServiceInfo service = ((ResolveInfo) resolveInfos.get(0)).serviceInfo;
            if (service != null) {
                String settings = settingsActivityFromServiceInfo(service, pm);
                if (settings != null) {
                    Intent i = new Intent();
                    i.setClassName(engine, settings);
                    return i;
                }
            }
        }
        return null;
    }

    private String settingsActivityFromServiceInfo(ServiceInfo si, PackageManager pm) {
        StringBuilder stringBuilder;
        XmlResourceParser parser = null;
        String str;
        try {
            parser = si.loadXmlMetaData(pm, Engine.SERVICE_META_DATA);
            if (parser == null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("No meta-data found for :");
                stringBuilder2.append(si);
                Log.w(str2, stringBuilder2.toString());
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            Resources res = pm.getResourcesForApplication(si.applicationInfo);
            int type;
            do {
                int next = parser.next();
                type = next;
                if (next == 1) {
                    if (parser != null) {
                        parser.close();
                    }
                    return null;
                }
            } while (type != 2);
            if (XML_TAG_NAME.equals(parser.getName())) {
                TypedArray array = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.TextToSpeechEngine);
                String settings = array.getString(null);
                array.recycle();
                if (parser != null) {
                    parser.close();
                }
                return settings;
            }
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Package ");
            stringBuilder3.append(si);
            stringBuilder3.append(" uses unknown tag :");
            stringBuilder3.append(parser.getName());
            Log.w(str, stringBuilder3.toString());
            if (parser != null) {
                parser.close();
            }
            return null;
        } catch (NameNotFoundException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not load resources for : ");
            stringBuilder.append(si);
            Log.w(str, stringBuilder.toString());
            if (parser != null) {
                parser.close();
            }
            return null;
        } catch (XmlPullParserException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error parsing metadata for ");
            stringBuilder.append(si);
            stringBuilder.append(SettingsStringUtil.DELIMITER);
            stringBuilder.append(e2);
            Log.w(str, stringBuilder.toString());
            if (parser != null) {
                parser.close();
            }
            return null;
        } catch (IOException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error parsing metadata for ");
            stringBuilder.append(si);
            stringBuilder.append(SettingsStringUtil.DELIMITER);
            stringBuilder.append(e3);
            Log.w(str, stringBuilder.toString());
            if (parser != null) {
                parser.close();
            }
            return null;
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
            throw th;
        }
    }

    private EngineInfo getEngineInfo(ResolveInfo resolve, PackageManager pm) {
        ServiceInfo service = resolve.serviceInfo;
        if (service == null) {
            return null;
        }
        EngineInfo engine = new EngineInfo();
        engine.name = service.packageName;
        CharSequence label = service.loadLabel(pm);
        engine.label = TextUtils.isEmpty(label) ? engine.name : label.toString();
        engine.icon = service.getIconResource();
        engine.priority = resolve.priority;
        engine.system = isSystemEngine(service);
        return engine;
    }

    public Locale getLocalePrefForEngine(String engineName) {
        return getLocalePrefForEngine(engineName, Secure.getString(this.mContext.getContentResolver(), Secure.TTS_DEFAULT_LOCALE));
    }

    public Locale getLocalePrefForEngine(String engineName, String prefValue) {
        String localeString = parseEnginePrefFromList(prefValue, engineName);
        if (TextUtils.isEmpty(localeString)) {
            return Locale.getDefault();
        }
        Locale result = parseLocaleString(localeString);
        if (result == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to parse locale ");
            stringBuilder.append(localeString);
            stringBuilder.append(", returning en_US instead");
            Log.w(str, stringBuilder.toString());
            result = Locale.US;
        }
        return result;
    }

    public boolean isLocaleSetToDefaultForEngine(String engineName) {
        return TextUtils.isEmpty(parseEnginePrefFromList(Secure.getString(this.mContext.getContentResolver(), Secure.TTS_DEFAULT_LOCALE), engineName));
    }

    public Locale parseLocaleString(String localeString) {
        String str;
        String language = "";
        String country = "";
        String variant = "";
        if (!TextUtils.isEmpty(localeString)) {
            String[] split = localeString.split("[-_]");
            language = split[0].toLowerCase();
            StringBuilder stringBuilder;
            if (split.length == 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to convert ");
                stringBuilder.append(localeString);
                stringBuilder.append(" to a valid Locale object. Only separators");
                Log.w(str, stringBuilder.toString());
                return null;
            } else if (split.length > 3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to convert ");
                stringBuilder.append(localeString);
                stringBuilder.append(" to a valid Locale object. Too many separators");
                Log.w(str, stringBuilder.toString());
                return null;
            } else {
                if (split.length >= 2) {
                    country = split[1].toUpperCase();
                }
                if (split.length >= 3) {
                    variant = split[2];
                }
            }
        }
        String normalizedLanguage = (String) sNormalizeLanguage.get(language);
        if (normalizedLanguage != null) {
            language = normalizedLanguage;
        }
        str = (String) sNormalizeCountry.get(country);
        if (str != null) {
            country = str;
        }
        Locale result = new Locale(language, country, variant);
        try {
            result.getISO3Language();
            result.getISO3Country();
            return result;
        } catch (MissingResourceException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to convert ");
            stringBuilder2.append(localeString);
            stringBuilder2.append(" to a valid Locale object.");
            Log.w(str2, stringBuilder2.toString());
            return null;
        }
    }

    public static Locale normalizeTTSLocale(Locale ttsLocale) {
        String normalizedLanguage;
        String language = ttsLocale.getLanguage();
        if (!TextUtils.isEmpty(language)) {
            normalizedLanguage = (String) sNormalizeLanguage.get(language);
            if (normalizedLanguage != null) {
                language = normalizedLanguage;
            }
        }
        normalizedLanguage = ttsLocale.getCountry();
        if (!TextUtils.isEmpty(normalizedLanguage)) {
            String normalizedCountry = (String) sNormalizeCountry.get(normalizedLanguage);
            if (normalizedCountry != null) {
                normalizedLanguage = normalizedCountry;
            }
        }
        return new Locale(language, normalizedLanguage, ttsLocale.getVariant());
    }

    public static String[] toOldLocaleStringFormat(Locale locale) {
        String[] ret = new String[]{"", "", ""};
        try {
            ret[0] = locale.getISO3Language();
            ret[1] = locale.getISO3Country();
            ret[2] = locale.getVariant();
            return ret;
        } catch (MissingResourceException e) {
            return new String[]{"eng", "USA", ""};
        }
    }

    private static String parseEnginePrefFromList(String prefValue, String engineName) {
        if (TextUtils.isEmpty(prefValue)) {
            return null;
        }
        for (String value : prefValue.split(",")) {
            int delimiter = value.indexOf(58);
            if (delimiter > 0 && engineName.equals(value.substring(0, delimiter))) {
                return value.substring(delimiter + 1);
            }
        }
        return null;
    }

    public synchronized void updateLocalePrefForEngine(String engineName, Locale newLocale) {
        Secure.putString(this.mContext.getContentResolver(), Secure.TTS_DEFAULT_LOCALE, updateValueInCommaSeparatedList(Secure.getString(this.mContext.getContentResolver(), Secure.TTS_DEFAULT_LOCALE), engineName, newLocale != null ? newLocale.toString() : "").toString());
    }

    private String updateValueInCommaSeparatedList(String list, String key, String newValue) {
        StringBuilder newPrefList = new StringBuilder();
        if (TextUtils.isEmpty(list)) {
            newPrefList.append(key);
            newPrefList.append(':');
            newPrefList.append(newValue);
        } else {
            boolean found = false;
            boolean first = true;
            for (String value : list.split(",")) {
                int delimiter = value.indexOf(58);
                if (delimiter > 0) {
                    if (key.equals(value.substring(0, delimiter))) {
                        if (first) {
                            first = false;
                        } else {
                            newPrefList.append(',');
                        }
                        found = true;
                        newPrefList.append(key);
                        newPrefList.append(':');
                        newPrefList.append(newValue);
                    } else {
                        if (first) {
                            first = false;
                        } else {
                            newPrefList.append(',');
                        }
                        newPrefList.append(value);
                    }
                }
            }
            if (!found) {
                newPrefList.append(',');
                newPrefList.append(key);
                newPrefList.append(':');
                newPrefList.append(newValue);
            }
        }
        return newPrefList.toString();
    }
}
