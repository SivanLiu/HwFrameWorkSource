package android.hardware.soundtrigger;

import android.Manifest.permission;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;

public class KeyphraseEnrollmentInfo {
    public static final String ACTION_MANAGE_VOICE_KEYPHRASES = "com.android.intent.action.MANAGE_VOICE_KEYPHRASES";
    public static final String EXTRA_VOICE_KEYPHRASE_ACTION = "com.android.intent.extra.VOICE_KEYPHRASE_ACTION";
    public static final String EXTRA_VOICE_KEYPHRASE_HINT_TEXT = "com.android.intent.extra.VOICE_KEYPHRASE_HINT_TEXT";
    public static final String EXTRA_VOICE_KEYPHRASE_LOCALE = "com.android.intent.extra.VOICE_KEYPHRASE_LOCALE";
    private static final String TAG = "KeyphraseEnrollmentInfo";
    private static final String VOICE_KEYPHRASE_META_DATA = "android.voice_enrollment";
    private final Map<KeyphraseMetadata, String> mKeyphrasePackageMap;
    private final KeyphraseMetadata[] mKeyphrases;
    private String mParseError;

    public KeyphraseEnrollmentInfo(PackageManager pm) {
        List<ResolveInfo> ris = pm.queryIntentActivities(new Intent(ACTION_MANAGE_VOICE_KEYPHRASES), 65536);
        if (ris == null || ris.isEmpty()) {
            this.mParseError = "No enrollment applications found";
            this.mKeyphrasePackageMap = Collections.emptyMap();
            this.mKeyphrases = null;
            return;
        }
        List<String> parseErrors = new LinkedList();
        this.mKeyphrasePackageMap = new HashMap();
        for (ResolveInfo ri : ris) {
            String str;
            StringBuilder stringBuilder;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(ri.activityInfo.packageName, 128);
                if ((ai.privateFlags & 8) == 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(ai.packageName);
                    stringBuilder.append("is not a privileged system app");
                    Slog.w(str, stringBuilder.toString());
                } else if (permission.MANAGE_VOICE_KEYPHRASES.equals(ai.permission)) {
                    KeyphraseMetadata metadata = getKeyphraseMetadataFromApplicationInfo(pm, ai, parseErrors);
                    if (metadata != null) {
                        this.mKeyphrasePackageMap.put(metadata, ai.packageName);
                    }
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(ai.packageName);
                    stringBuilder.append(" does not require MANAGE_VOICE_KEYPHRASES");
                    Slog.w(str, stringBuilder.toString());
                }
            } catch (NameNotFoundException e) {
                str = new StringBuilder();
                str.append("error parsing voice enrollment meta-data for ");
                str.append(ri.activityInfo.packageName);
                str = str.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append(": ");
                stringBuilder.append(e);
                parseErrors.add(stringBuilder.toString());
                Slog.w(TAG, str, e);
            }
        }
        if (this.mKeyphrasePackageMap.isEmpty()) {
            String error = "No suitable enrollment application found";
            parseErrors.add(error);
            Slog.w(TAG, error);
            this.mKeyphrases = null;
        } else {
            this.mKeyphrases = (KeyphraseMetadata[]) this.mKeyphrasePackageMap.keySet().toArray(new KeyphraseMetadata[this.mKeyphrasePackageMap.size()]);
        }
        if (!parseErrors.isEmpty()) {
            this.mParseError = TextUtils.join("\n", parseErrors);
        }
    }

    /* JADX WARNING: Missing block: B:23:0x007d, code skipped:
            if (r0 != null) goto L_0x007f;
     */
    /* JADX WARNING: Missing block: B:37:0x0117, code skipped:
            if (r0 == null) goto L_0x011b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private KeyphraseMetadata getKeyphraseMetadataFromApplicationInfo(PackageManager pm, ApplicationInfo ai, List<String> parseErrors) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        XmlResourceParser parser = null;
        String packageName = ai.packageName;
        KeyphraseMetadata keyphraseMetadata = null;
        String error;
        try {
            parser = ai.loadXmlMetaData(pm, VOICE_KEYPHRASE_META_DATA);
            if (parser == null) {
                error = new StringBuilder();
                error.append("No android.voice_enrollment meta-data for ");
                error.append(packageName);
                error = error.toString();
                parseErrors.add(error);
                Slog.w(TAG, error);
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
            Resources res = pm.getResourcesForApplication(ai);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            while (true) {
                int next = parser.next();
                int type = next;
                if (next == 1 || type == 2) {
                }
            }
            if ("voice-enrollment-application".equals(parser.getName())) {
                TypedArray array = res.obtainAttributes(attrs, R.styleable.VoiceEnrollmentApplication);
                keyphraseMetadata = getKeyphraseFromTypedArray(array, packageName, parseErrors);
                array.recycle();
            } else {
                String error2 = new StringBuilder();
                error2.append("Meta-data does not start with voice-enrollment-application tag for ");
                error2.append(packageName);
                error2 = error2.toString();
                parseErrors.add(error2);
                Slog.w(TAG, error2);
                if (parser != null) {
                    parser.close();
                }
                return null;
            }
        } catch (XmlPullParserException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error parsing keyphrase enrollment meta-data for ");
            stringBuilder.append(packageName);
            error = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(error);
            stringBuilder2.append(": ");
            stringBuilder2.append(e);
            parseErrors.add(stringBuilder2.toString());
            Slog.w(TAG, error, e);
        } catch (IOException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error parsing keyphrase enrollment meta-data for ");
            stringBuilder.append(packageName);
            error = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(error);
            stringBuilder2.append(": ");
            stringBuilder2.append(e2);
            parseErrors.add(stringBuilder2.toString());
            Slog.w(TAG, error, e2);
            if (parser != null) {
                parser.close();
            }
            return keyphraseMetadata;
        } catch (NameNotFoundException e3) {
            error = new StringBuilder();
            error.append("Error parsing keyphrase enrollment meta-data for ");
            error.append(packageName);
            error = error.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(error);
            stringBuilder2.append(": ");
            stringBuilder2.append(e3);
            parseErrors.add(stringBuilder2.toString());
            Slog.w(TAG, error, e3);
            if (parser != null) {
                parser.close();
            }
            return keyphraseMetadata;
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private KeyphraseMetadata getKeyphraseFromTypedArray(TypedArray array, String packageName, List<String> parseErrors) {
        String error;
        int i = 0;
        int searchKeyphraseId = array.getInt(0, -1);
        String error2;
        if (searchKeyphraseId <= 0) {
            error2 = new StringBuilder();
            error2.append("No valid searchKeyphraseId specified in meta-data for ");
            error2.append(packageName);
            error2 = error2.toString();
            parseErrors.add(error2);
            Slog.w(TAG, error2);
            return null;
        }
        String searchKeyphrase = array.getString(1);
        StringBuilder stringBuilder;
        if (searchKeyphrase == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No valid searchKeyphrase specified in meta-data for ");
            stringBuilder.append(packageName);
            error2 = stringBuilder.toString();
            parseErrors.add(error2);
            Slog.w(TAG, error2);
            return null;
        }
        String searchKeyphraseSupportedLocales = array.getString(2);
        if (searchKeyphraseSupportedLocales == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No valid searchKeyphraseSupportedLocales specified in meta-data for ");
            stringBuilder.append(packageName);
            error2 = stringBuilder.toString();
            parseErrors.add(error2);
            Slog.w(TAG, error2);
            return null;
        }
        ArraySet<Locale> locales = new ArraySet();
        if (!TextUtils.isEmpty(searchKeyphraseSupportedLocales)) {
            try {
                String[] supportedLocalesDelimited = searchKeyphraseSupportedLocales.split(",");
                while (i < supportedLocalesDelimited.length) {
                    locales.add(Locale.forLanguageTag(supportedLocalesDelimited[i]));
                    i++;
                }
            } catch (Exception e) {
                error = new StringBuilder();
                error.append("Error reading searchKeyphraseSupportedLocales from meta-data for ");
                error.append(packageName);
                error = error.toString();
                parseErrors.add(error);
                Slog.w(TAG, error);
                return null;
            }
        }
        int recognitionModes = array.getInt(3, -1);
        if (recognitionModes >= 0) {
            return new KeyphraseMetadata(searchKeyphraseId, searchKeyphrase, locales, recognitionModes);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("No valid searchKeyphraseRecognitionFlags specified in meta-data for ");
        stringBuilder2.append(packageName);
        error = stringBuilder2.toString();
        parseErrors.add(error);
        Slog.w(TAG, error);
        return null;
    }

    public String getParseError() {
        return this.mParseError;
    }

    public KeyphraseMetadata[] listKeyphraseMetadata() {
        return this.mKeyphrases;
    }

    public Intent getManageKeyphraseIntent(int action, String keyphrase, Locale locale) {
        if (this.mKeyphrasePackageMap == null || this.mKeyphrasePackageMap.isEmpty()) {
            Slog.w(TAG, "No enrollment application exists");
            return null;
        }
        KeyphraseMetadata keyphraseMetadata = getKeyphraseMetadata(keyphrase, locale);
        if (keyphraseMetadata != null) {
            return new Intent(ACTION_MANAGE_VOICE_KEYPHRASES).setPackage((String) this.mKeyphrasePackageMap.get(keyphraseMetadata)).putExtra(EXTRA_VOICE_KEYPHRASE_HINT_TEXT, keyphrase).putExtra(EXTRA_VOICE_KEYPHRASE_LOCALE, locale.toLanguageTag()).putExtra(EXTRA_VOICE_KEYPHRASE_ACTION, action);
        }
        return null;
    }

    public KeyphraseMetadata getKeyphraseMetadata(String keyphrase, Locale locale) {
        if (this.mKeyphrases != null && this.mKeyphrases.length > 0) {
            for (KeyphraseMetadata keyphraseMetadata : this.mKeyphrases) {
                if (keyphraseMetadata.supportsPhrase(keyphrase) && keyphraseMetadata.supportsLocale(locale)) {
                    return keyphraseMetadata;
                }
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No enrollment application supports the given keyphrase/locale: '");
        stringBuilder.append(keyphrase);
        stringBuilder.append("'/");
        stringBuilder.append(locale);
        Slog.w(str, stringBuilder.toString());
        return null;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("KeyphraseEnrollmentInfo [Keyphrases=");
        stringBuilder.append(this.mKeyphrasePackageMap.toString());
        stringBuilder.append(", ParseError=");
        stringBuilder.append(this.mParseError);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
