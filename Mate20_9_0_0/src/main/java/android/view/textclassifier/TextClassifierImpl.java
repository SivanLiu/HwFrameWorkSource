package android.view.textclassifier;

import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.ParcelFileDescriptor;
import android.os.UserManager;
import android.provider.Browser;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.ContactsContract.Contacts;
import android.service.notification.ZenModeConfig;
import android.view.textclassifier.TextClassifier.Utils;
import android.view.textclassifier.TextClassifierImplNative.AnnotatedSpan;
import android.view.textclassifier.TextClassifierImplNative.AnnotationOptions;
import android.view.textclassifier.TextClassifierImplNative.ClassificationOptions;
import android.view.textclassifier.TextClassifierImplNative.ClassificationResult;
import android.view.textclassifier.TextClassifierImplNative.SelectionOptions;
import android.view.textclassifier.TextSelection.Request;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class TextClassifierImpl implements TextClassifier {
    private static final String LOG_TAG = "androidtc";
    private static final String MODEL_DIR = "/etc/textclassifier/";
    private static final String MODEL_FILE_REGEX = "textclassifier\\.(.*)\\.model";
    private static final String UPDATED_MODEL_FILE_PATH = "/data/misc/textclassifier/textclassifier.model";
    @GuardedBy("mLock")
    private List<ModelFile> mAllModelFiles;
    private final Context mContext;
    private final TextClassifier mFallback;
    private final GenerateLinksLogger mGenerateLinksLogger;
    private final Object mLock;
    private final Object mLoggerLock;
    @GuardedBy("mLock")
    private ModelFile mModel;
    @GuardedBy("mLock")
    private TextClassifierImplNative mNative;
    @GuardedBy("mLoggerLock")
    private SelectionSessionLogger mSessionLogger;
    private final TextClassificationConstants mSettings;

    static final class IntentFactory {
        private static final long DEFAULT_EVENT_DURATION = TimeUnit.HOURS.toMillis(1);
        private static final long MIN_EVENT_FUTURE_MILLIS = TimeUnit.MINUTES.toMillis(5);

        private IntentFactory() {
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static List<LabeledIntent> create(Context context, Instant referenceTime, ClassificationResult classification, String text) {
            Object obj;
            String type = classification.getCollection().trim().toLowerCase(Locale.ENGLISH);
            text = text.trim();
            switch (type.hashCode()) {
                case -1271823248:
                    if (type.equals(TextClassifier.TYPE_FLIGHT_NUMBER)) {
                        obj = 6;
                        break;
                    }
                case -1147692044:
                    if (type.equals("address")) {
                        obj = 2;
                        break;
                    }
                case 116079:
                    if (type.equals("url")) {
                        obj = 3;
                        break;
                    }
                case 3076014:
                    if (type.equals("date")) {
                        obj = 4;
                        break;
                    }
                case 96619420:
                    if (type.equals("email")) {
                        obj = null;
                        break;
                    }
                case 106642798:
                    if (type.equals("phone")) {
                        obj = 1;
                        break;
                    }
                case 1793702779:
                    if (type.equals(TextClassifier.TYPE_DATE_TIME)) {
                        obj = 5;
                        break;
                    }
                default:
                    obj = -1;
                    break;
            }
            switch (obj) {
                case null:
                    return createForEmail(context, text);
                case 1:
                    return createForPhone(context, text);
                case 2:
                    return createForAddress(context, text);
                case 3:
                    return createForUrl(context, text);
                case 4:
                case 5:
                    if (classification.getDatetimeResult() != null) {
                        return createForDatetime(context, type, referenceTime, Instant.ofEpochMilli(classification.getDatetimeResult().getTimeMsUtc()));
                    }
                    return new ArrayList();
                case 6:
                    return createForFlight(context, text);
                default:
                    return new ArrayList();
            }
        }

        private static List<LabeledIntent> createForEmail(Context context, String text) {
            LabeledIntent[] labeledIntentArr = new LabeledIntent[2];
            labeledIntentArr[0] = new LabeledIntent(context.getString(17039979), context.getString(17039985), new Intent("android.intent.action.SENDTO").setData(Uri.parse(String.format("mailto:%s", new Object[]{text}))), 0);
            labeledIntentArr[1] = new LabeledIntent(context.getString(17039557), context.getString(17039558), new Intent("android.intent.action.INSERT_OR_EDIT").setType(Contacts.CONTENT_ITEM_TYPE).putExtra("email", text), text.hashCode());
            return Arrays.asList(labeledIntentArr);
        }

        private static List<LabeledIntent> createForPhone(Context context, String text) {
            List<LabeledIntent> actions = new ArrayList();
            UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
            Bundle userRestrictions = userManager != null ? userManager.getUserRestrictions() : new Bundle();
            if (!userRestrictions.getBoolean(UserManager.DISALLOW_OUTGOING_CALLS, false)) {
                actions.add(new LabeledIntent(context.getString(17039938), context.getString(17039939), new Intent("android.intent.action.DIAL").setData(Uri.parse(String.format("tel:%s", new Object[]{text}))), 0));
            }
            actions.add(new LabeledIntent(context.getString(17039557), context.getString(17039558), new Intent("android.intent.action.INSERT_OR_EDIT").setType(Contacts.CONTENT_ITEM_TYPE).putExtra("phone", text), text.hashCode()));
            if (!userRestrictions.getBoolean(UserManager.DISALLOW_SMS, false)) {
                actions.add(new LabeledIntent(context.getString(17041124), context.getString(17041129), new Intent("android.intent.action.SENDTO").setData(Uri.parse(String.format("smsto:%s", new Object[]{text}))), 0));
            }
            return actions;
        }

        private static List<LabeledIntent> createForAddress(Context context, String text) {
            List<LabeledIntent> actions = new ArrayList();
            try {
                String encText = URLEncoder.encode(text, "UTF-8");
                actions.add(new LabeledIntent(context.getString(17040407), context.getString(17040408), new Intent("android.intent.action.VIEW").setData(Uri.parse(String.format("geo:0,0?q=%s", new Object[]{encText}))), 0));
            } catch (UnsupportedEncodingException e) {
                Log.e("androidtc", "Could not encode address", e);
            }
            return actions;
        }

        private static List<LabeledIntent> createForUrl(Context context, String text) {
            if (Uri.parse(text).getScheme() == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("http://");
                stringBuilder.append(text);
                text = stringBuilder.toString();
            }
            return Arrays.asList(new LabeledIntent[]{new LabeledIntent(context.getString(17039698), context.getString(17039699), new Intent("android.intent.action.VIEW", Uri.parse(text)).putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName()), 0)});
        }

        private static List<LabeledIntent> createForDatetime(Context context, String type, Instant referenceTime, Instant parsedTime) {
            if (referenceTime == null) {
                referenceTime = Instant.now();
            }
            List<LabeledIntent> actions = new ArrayList();
            actions.add(createCalendarViewIntent(context, parsedTime));
            if (referenceTime.until(parsedTime, ChronoUnit.MILLIS) > MIN_EVENT_FUTURE_MILLIS) {
                actions.add(createCalendarCreateEventIntent(context, parsedTime, type));
            }
            return actions;
        }

        private static List<LabeledIntent> createForFlight(Context context, String text) {
            return Arrays.asList(new LabeledIntent[]{new LabeledIntent(context.getString(17041317), context.getString(17041318), new Intent("android.intent.action.WEB_SEARCH").putExtra(SuggestionColumns.QUERY, text), text.hashCode())});
        }

        private static LabeledIntent createCalendarViewIntent(Context context, Instant parsedTime) {
            Builder builder = CalendarContract.CONTENT_URI.buildUpon();
            builder.appendPath("time");
            ContentUris.appendId(builder, parsedTime.toEpochMilli());
            return new LabeledIntent(context.getString(17041315), context.getString(17041316), new Intent("android.intent.action.VIEW").setData(builder.build()), 0);
        }

        private static LabeledIntent createCalendarCreateEventIntent(Context context, Instant parsedTime, String type) {
            return new LabeledIntent(context.getString(17039555), context.getString(17039556), new Intent("android.intent.action.INSERT").setData(Events.CONTENT_URI).putExtra("allDay", "date".equals(type)).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, parsedTime.toEpochMilli()).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, parsedTime.toEpochMilli() + DEFAULT_EVENT_DURATION), parsedTime.hashCode());
        }
    }

    private static final class LabeledIntent {
        static final int DEFAULT_REQUEST_CODE = 0;
        private final String mDescription;
        private final Intent mIntent;
        private final int mRequestCode;
        private final String mTitle;

        LabeledIntent(String title, String description, Intent intent, int requestCode) {
            this.mTitle = title;
            this.mDescription = description;
            this.mIntent = intent;
            this.mRequestCode = requestCode;
        }

        String getTitle() {
            return this.mTitle;
        }

        String getDescription() {
            return this.mDescription;
        }

        Intent getIntent() {
            return this.mIntent;
        }

        int getRequestCode() {
            return this.mRequestCode;
        }

        RemoteAction asRemoteAction(Context context) {
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(this.mIntent, 0);
            String packageName = (resolveInfo == null || resolveInfo.activityInfo == null) ? null : resolveInfo.activityInfo.packageName;
            Icon icon = null;
            boolean shouldShowIcon = false;
            if (!(packageName == null || ZenModeConfig.SYSTEM_AUTHORITY.equals(packageName) || resolveInfo == null || resolveInfo.activityInfo == null)) {
                this.mIntent.setComponent(new ComponentName(packageName, resolveInfo.activityInfo.name));
                if (resolveInfo.activityInfo.getIconResource() != 0) {
                    icon = Icon.createWithResource(packageName, resolveInfo.activityInfo.getIconResource());
                    shouldShowIcon = true;
                }
            }
            if (icon == null) {
                icon = Icon.createWithResource(ZenModeConfig.SYSTEM_AUTHORITY, 17302696);
            }
            PendingIntent pendingIntent = TextClassification.createPendingIntent(context, this.mIntent, this.mRequestCode);
            if (pendingIntent == null) {
                return null;
            }
            RemoteAction action = new RemoteAction(icon, this.mTitle, this.mDescription, pendingIntent);
            action.setShouldShowIcon(shouldShowIcon);
            return action;
        }
    }

    private static final class ModelFile {
        private final boolean mLanguageIndependent;
        private final String mName;
        private final String mPath;
        private final List<Locale> mSupportedLocales;
        private final int mVersion;

        static ModelFile fromPath(String path) {
            File file = new File(path);
            try {
                ParcelFileDescriptor modelFd = ParcelFileDescriptor.open(file, 268435456);
                int version = TextClassifierImplNative.getVersion(modelFd.getFd());
                String supportedLocalesStr = TextClassifierImplNative.getLocales(modelFd.getFd());
                if (supportedLocalesStr.isEmpty()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Ignoring ");
                    stringBuilder.append(file.getAbsolutePath());
                    Log.d("androidtc", stringBuilder.toString());
                    TextClassifierImpl.closeAndLogError(modelFd);
                    return null;
                }
                boolean languageIndependent = supportedLocalesStr.equals("*");
                List supportedLocales = new ArrayList();
                for (String langTag : supportedLocalesStr.split(",")) {
                    supportedLocales.add(Locale.forLanguageTag(langTag));
                }
                TextClassifierImpl.closeAndLogError(modelFd);
                return new ModelFile(path, file.getName(), version, supportedLocales, languageIndependent);
            } catch (FileNotFoundException e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to peek ");
                stringBuilder2.append(file.getAbsolutePath());
                Log.e("androidtc", stringBuilder2.toString(), e);
                return null;
            }
        }

        String getPath() {
            return this.mPath;
        }

        String getName() {
            return this.mName;
        }

        int getVersion() {
            return this.mVersion;
        }

        boolean isAnyLanguageSupported(List<LanguageRange> languageRanges) {
            return this.mLanguageIndependent || Locale.lookup(languageRanges, this.mSupportedLocales) != null;
        }

        List<Locale> getSupportedLocales() {
            return Collections.unmodifiableList(this.mSupportedLocales);
        }

        public boolean isPreferredTo(ModelFile model) {
            if (model == null) {
                return true;
            }
            if ((this.mLanguageIndependent || !model.mLanguageIndependent) && getVersion() <= model.getVersion()) {
                return false;
            }
            return true;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || !ModelFile.class.isAssignableFrom(other.getClass())) {
                return false;
            }
            return this.mPath.equals(((ModelFile) other).mPath);
        }

        public String toString() {
            StringJoiner localesJoiner = new StringJoiner(",");
            for (Locale locale : this.mSupportedLocales) {
                localesJoiner.add(locale.toLanguageTag());
            }
            return String.format(Locale.US, "ModelFile { path=%s name=%s version=%d locales=%s }", new Object[]{this.mPath, this.mName, Integer.valueOf(this.mVersion), localesJoiner.toString()});
        }

        private ModelFile(String path, String name, int version, List<Locale> supportedLocales, boolean languageIndependent) {
            this.mPath = path;
            this.mName = name;
            this.mVersion = version;
            this.mSupportedLocales = supportedLocales;
            this.mLanguageIndependent = languageIndependent;
        }
    }

    public TextClassifierImpl(Context context, TextClassificationConstants settings, TextClassifier fallback) {
        this.mLock = new Object();
        this.mLoggerLock = new Object();
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mFallback = (TextClassifier) Preconditions.checkNotNull(fallback);
        this.mSettings = (TextClassificationConstants) Preconditions.checkNotNull(settings);
        this.mGenerateLinksLogger = new GenerateLinksLogger(this.mSettings.getGenerateLinksLogSampleRate());
    }

    public TextClassifierImpl(Context context, TextClassificationConstants settings) {
        this(context, settings, TextClassifier.NO_OP);
    }

    public TextSelection suggestSelection(Request request) {
        Preconditions.checkNotNull(request);
        Utils.checkMainThread();
        try {
            int rangeLength = request.getEndIndex() - request.getStartIndex();
            String string = request.getText().toString();
            if (string.length() > 0 && rangeLength <= this.mSettings.getSuggestSelectionMaxRangeLength()) {
                int start;
                int start2;
                String localesString = concatenateLocales(request.getDefaultLocales());
                ZonedDateTime refTime = ZonedDateTime.now();
                TextClassifierImplNative nativeImpl = getNative(request.getDefaultLocales());
                int i = 0;
                if (!this.mSettings.isModelDarkLaunchEnabled() || request.isDarkLaunchAllowed()) {
                    int[] startEnd = nativeImpl.suggestSelection(string, request.getStartIndex(), request.getEndIndex(), new SelectionOptions(localesString));
                    start = startEnd[0];
                    int i2 = startEnd[1];
                    start2 = start;
                    start = i2;
                } else {
                    start2 = request.getStartIndex();
                    start = request.getEndIndex();
                }
                if (start2 >= start || start2 < 0 || start > string.length() || start2 > request.getStartIndex() || start < request.getEndIndex()) {
                    Log.d("androidtc", "Got bad indices for input text. Ignoring result.");
                } else {
                    TextSelection.Builder tsBuilder = new TextSelection.Builder(start2, start);
                    ClassificationResult[] results = nativeImpl.classifyText(string, start2, start, new ClassificationOptions(refTime.toInstant().toEpochMilli(), refTime.getZone().getId(), localesString));
                    int size = results.length;
                    while (i < size) {
                        tsBuilder.setEntityType(results[i].getCollection(), results[i].getScore());
                        i++;
                    }
                    return tsBuilder.setId(createId(string, request.getStartIndex(), request.getEndIndex())).build();
                }
            }
        } catch (Throwable t) {
            Log.e("androidtc", "Error suggesting selection for text. No changes to selection suggested.", t);
        }
        return this.mFallback.suggestSelection(request);
    }

    public TextClassification classifyText(TextClassification.Request request) {
        Preconditions.checkNotNull(request);
        Utils.checkMainThread();
        try {
            int rangeLength = request.getEndIndex() - request.getStartIndex();
            String string = request.getText().toString();
            if (string.length() > 0 && rangeLength <= this.mSettings.getClassifyTextMaxRangeLength()) {
                ZonedDateTime refTime = request.getReferenceTime() != null ? request.getReferenceTime() : ZonedDateTime.now();
                ClassificationResult[] results = getNative(request.getDefaultLocales()).classifyText(string, request.getStartIndex(), request.getEndIndex(), new ClassificationOptions(refTime.toInstant().toEpochMilli(), refTime.getZone().getId(), concatenateLocales(request.getDefaultLocales())));
                if (results.length > 0) {
                    return createClassificationResult(results, string, request.getStartIndex(), request.getEndIndex(), refTime.toInstant());
                }
            }
        } catch (Throwable t) {
            Log.e("androidtc", "Error getting text classification info.", t);
        }
        return this.mFallback.classifyText(request);
    }

    public TextLinks generateLinks(TextLinks.Request request) {
        Preconditions.checkNotNull(request);
        Utils.checkTextLength(request.getText(), getMaxGenerateLinksTextLength());
        Utils.checkMainThread();
        if (!this.mSettings.isSmartLinkifyEnabled() && request.isLegacyFallback()) {
            return Utils.generateLegacyLinks(request);
        }
        String textString = request.getText().toString();
        TextLinks.Builder builder = new TextLinks.Builder(textString);
        try {
            Collection<String> entitiesToIdentify;
            String packageName;
            long startTimeMs = System.currentTimeMillis();
            ZonedDateTime refTime = ZonedDateTime.now();
            if (request.getEntityConfig() != null) {
                entitiesToIdentify = request.getEntityConfig().resolveEntityListModifications(getEntitiesForHints(request.getEntityConfig().getHints()));
            } else {
                entitiesToIdentify = this.mSettings.getEntityListDefault();
            }
            AnnotatedSpan[] annotations = getNative(request.getDefaultLocales()).annotate(textString, new AnnotationOptions(refTime.toInstant().toEpochMilli(), refTime.getZone().getId(), concatenateLocales(request.getDefaultLocales())));
            int length = annotations.length;
            int i = 0;
            int i2 = 0;
            while (i2 < length) {
                ZonedDateTime refTime2;
                AnnotatedSpan span = annotations[i2];
                ClassificationResult[] results = span.getClassification();
                if (results.length == 0) {
                    refTime2 = refTime;
                } else if (entitiesToIdentify.contains(results[i].getCollection())) {
                    Map<String, Float> entityScores = new HashMap();
                    int i3 = i;
                    while (i3 < results.length) {
                        refTime2 = refTime;
                        entityScores.put(results[i3].getCollection(), Float.valueOf(results[i3].getScore()));
                        i3++;
                        refTime = refTime2;
                    }
                    refTime2 = refTime;
                    builder.addLink(span.getStartIndex(), span.getEndIndex(), entityScores);
                } else {
                    refTime2 = refTime;
                }
                i2++;
                refTime = refTime2;
                i = 0;
            }
            TextLinks links = builder.build();
            long endTimeMs = System.currentTimeMillis();
            if (request.getCallingPackageName() == null) {
                packageName = this.mContext.getPackageName();
            } else {
                packageName = request.getCallingPackageName();
            }
            TextLinks textLinks = links;
            this.mGenerateLinksLogger.logGenerateLinks(request.getText(), textLinks, packageName, endTimeMs - startTimeMs);
            return links;
        } catch (Throwable t) {
            Log.e("androidtc", "Error getting links info.", t);
            return this.mFallback.generateLinks(request);
        }
    }

    public int getMaxGenerateLinksTextLength() {
        return this.mSettings.getGenerateLinksMaxTextLength();
    }

    private Collection<String> getEntitiesForHints(Collection<String> hints) {
        boolean editable = hints.contains(TextClassifier.HINT_TEXT_IS_EDITABLE);
        if (editable == hints.contains(TextClassifier.HINT_TEXT_IS_NOT_EDITABLE)) {
            return this.mSettings.getEntityListDefault();
        }
        if (editable) {
            return this.mSettings.getEntityListEditable();
        }
        return this.mSettings.getEntityListNotEditable();
    }

    public void onSelectionEvent(SelectionEvent event) {
        Preconditions.checkNotNull(event);
        synchronized (this.mLoggerLock) {
            if (this.mSessionLogger == null) {
                this.mSessionLogger = new SelectionSessionLogger();
            }
            this.mSessionLogger.writeEvent(event);
        }
    }

    private TextClassifierImplNative getNative(LocaleList localeList) throws FileNotFoundException {
        TextClassifierImplNative textClassifierImplNative;
        synchronized (this.mLock) {
            LocaleList emptyLocaleList;
            if (localeList == null) {
                try {
                    emptyLocaleList = LocaleList.getEmptyLocaleList();
                } catch (Throwable th) {
                }
            } else {
                emptyLocaleList = localeList;
            }
            localeList = emptyLocaleList;
            ModelFile bestModel = findBestModelLocked(localeList);
            StringBuilder stringBuilder;
            if (bestModel != null) {
                if (this.mNative == null || !Objects.equals(this.mModel, bestModel)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Loading ");
                    stringBuilder.append(bestModel);
                    Log.d("androidtc", stringBuilder.toString());
                    destroyNativeIfExistsLocked();
                    ParcelFileDescriptor fd = ParcelFileDescriptor.open(new File(bestModel.getPath()), 268435456);
                    this.mNative = new TextClassifierImplNative(fd.getFd());
                    closeAndLogError(fd);
                    this.mModel = bestModel;
                }
                textClassifierImplNative = this.mNative;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("No model for ");
                stringBuilder.append(localeList.toLanguageTags());
                throw new FileNotFoundException(stringBuilder.toString());
            }
        }
        return textClassifierImplNative;
    }

    private String createId(String text, int start, int end) {
        String createId;
        synchronized (this.mLock) {
            createId = SelectionSessionLogger.createId(text, start, end, this.mContext, this.mModel.getVersion(), this.mModel.getSupportedLocales());
        }
        return createId;
    }

    @GuardedBy("mLock")
    private void destroyNativeIfExistsLocked() {
        if (this.mNative != null) {
            this.mNative.close();
            this.mNative = null;
        }
    }

    private static String concatenateLocales(LocaleList locales) {
        return locales == null ? "" : locales.toLanguageTags();
    }

    @GuardedBy("mLock")
    private ModelFile findBestModelLocked(LocaleList localeList) {
        String languages;
        if (localeList.isEmpty()) {
            languages = LocaleList.getDefault().toLanguageTags();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(localeList.toLanguageTags());
            stringBuilder.append(",");
            stringBuilder.append(LocaleList.getDefault().toLanguageTags());
            languages = stringBuilder.toString();
        }
        List<LanguageRange> languageRangeList = LanguageRange.parse(languages);
        ModelFile bestModel = null;
        for (ModelFile model : listAllModelsLocked()) {
            if (model.isAnyLanguageSupported(languageRangeList) && model.isPreferredTo(bestModel)) {
                bestModel = model;
            }
        }
        return bestModel;
    }

    @GuardedBy("mLock")
    private List<ModelFile> listAllModelsLocked() {
        if (this.mAllModelFiles == null) {
            List<ModelFile> allModels = new ArrayList();
            if (new File(UPDATED_MODEL_FILE_PATH).exists()) {
                ModelFile updatedModel = ModelFile.fromPath(UPDATED_MODEL_FILE_PATH);
                if (updatedModel != null) {
                    allModels.add(updatedModel);
                }
            }
            File modelsDir = new File(MODEL_DIR);
            if (modelsDir.exists() && modelsDir.isDirectory()) {
                File[] modelFiles = modelsDir.listFiles();
                Pattern modelFilenamePattern = Pattern.compile(MODEL_FILE_REGEX);
                if (!(modelFiles == null || modelFilenamePattern == null)) {
                    for (File modelFile : modelFiles) {
                        if (modelFilenamePattern.matcher(modelFile.getName()).matches() && modelFile.isFile()) {
                            ModelFile model = ModelFile.fromPath(modelFile.getAbsolutePath());
                            if (model != null) {
                                allModels.add(model);
                            }
                        }
                    }
                }
            }
            this.mAllModelFiles = allModels;
        }
        return this.mAllModelFiles;
    }

    private TextClassification createClassificationResult(ClassificationResult[] classifications, String text, int start, int end, Instant referenceTime) {
        ClassificationResult[] classificationResultArr = classifications;
        String classifiedText = text.substring(start, end);
        TextClassification.Builder builder = new TextClassification.Builder().setText(classifiedText);
        int size = classificationResultArr.length;
        ClassificationResult highestScoringResult = null;
        float highestScore = Float.MIN_VALUE;
        for (int i = 0; i < size; i++) {
            builder.setEntityType(classificationResultArr[i].getCollection(), classificationResultArr[i].getScore());
            if (classificationResultArr[i].getScore() > highestScore) {
                highestScoringResult = classificationResultArr[i];
                highestScore = classificationResultArr[i].getScore();
            }
        }
        boolean isPrimaryAction = true;
        if (highestScoringResult != null) {
            for (LabeledIntent labeledIntent : IntentFactory.create(this.mContext, referenceTime, highestScoringResult, classifiedText)) {
                RemoteAction action = labeledIntent.asRemoteAction(this.mContext);
                if (action != null) {
                    if (isPrimaryAction) {
                        builder.setIcon(action.getIcon().loadDrawable(this.mContext));
                        builder.setLabel(action.getTitle().toString());
                        builder.setIntent(labeledIntent.getIntent());
                        builder.setOnClickListener(TextClassification.createIntentOnClickListener(TextClassification.createPendingIntent(this.mContext, labeledIntent.getIntent(), labeledIntent.getRequestCode())));
                        isPrimaryAction = false;
                    }
                    builder.addAction(action);
                }
            }
        } else {
            Instant instant = referenceTime;
        }
        return builder.setId(createId(text, start, end)).build();
    }

    private static void closeAndLogError(ParcelFileDescriptor fd) {
        try {
            fd.close();
        } catch (IOException e) {
            Log.e("androidtc", "Error closing file.", e);
        }
    }
}
