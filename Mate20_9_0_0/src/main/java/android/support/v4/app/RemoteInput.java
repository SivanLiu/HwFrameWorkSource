package android.support.v4.app;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class RemoteInput {
    private static final String EXTRA_DATA_TYPE_RESULTS_DATA = "android.remoteinput.dataTypeResultsData";
    public static final String EXTRA_RESULTS_DATA = "android.remoteinput.resultsData";
    public static final String RESULTS_CLIP_LABEL = "android.remoteinput.results";
    private static final String TAG = "RemoteInput";
    private final boolean mAllowFreeFormTextInput;
    private final Set<String> mAllowedDataTypes;
    private final CharSequence[] mChoices;
    private final Bundle mExtras;
    private final CharSequence mLabel;
    private final String mResultKey;

    public static final class Builder {
        private boolean mAllowFreeFormTextInput = true;
        private final Set<String> mAllowedDataTypes = new HashSet();
        private CharSequence[] mChoices;
        private final Bundle mExtras = new Bundle();
        private CharSequence mLabel;
        private final String mResultKey;

        public Builder(@NonNull String resultKey) {
            if (resultKey != null) {
                this.mResultKey = resultKey;
                return;
            }
            throw new IllegalArgumentException("Result key can't be null");
        }

        @NonNull
        public Builder setLabel(@Nullable CharSequence label) {
            this.mLabel = label;
            return this;
        }

        @NonNull
        public Builder setChoices(@Nullable CharSequence[] choices) {
            this.mChoices = choices;
            return this;
        }

        @NonNull
        public Builder setAllowDataType(@NonNull String mimeType, boolean doAllow) {
            if (doAllow) {
                this.mAllowedDataTypes.add(mimeType);
            } else {
                this.mAllowedDataTypes.remove(mimeType);
            }
            return this;
        }

        @NonNull
        public Builder setAllowFreeFormInput(boolean allowFreeFormTextInput) {
            this.mAllowFreeFormTextInput = allowFreeFormTextInput;
            return this;
        }

        @NonNull
        public Builder addExtras(@NonNull Bundle extras) {
            if (extras != null) {
                this.mExtras.putAll(extras);
            }
            return this;
        }

        @NonNull
        public Bundle getExtras() {
            return this.mExtras;
        }

        @NonNull
        public RemoteInput build() {
            return new RemoteInput(this.mResultKey, this.mLabel, this.mChoices, this.mAllowFreeFormTextInput, this.mExtras, this.mAllowedDataTypes);
        }
    }

    RemoteInput(String resultKey, CharSequence label, CharSequence[] choices, boolean allowFreeFormTextInput, Bundle extras, Set<String> allowedDataTypes) {
        this.mResultKey = resultKey;
        this.mLabel = label;
        this.mChoices = choices;
        this.mAllowFreeFormTextInput = allowFreeFormTextInput;
        this.mExtras = extras;
        this.mAllowedDataTypes = allowedDataTypes;
    }

    public String getResultKey() {
        return this.mResultKey;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public CharSequence[] getChoices() {
        return this.mChoices;
    }

    public Set<String> getAllowedDataTypes() {
        return this.mAllowedDataTypes;
    }

    public boolean isDataOnly() {
        return (getAllowFreeFormInput() || ((getChoices() != null && getChoices().length != 0) || getAllowedDataTypes() == null || getAllowedDataTypes().isEmpty())) ? false : true;
    }

    public boolean getAllowFreeFormInput() {
        return this.mAllowFreeFormTextInput;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public static Map<String, Uri> getDataResultsFromIntent(Intent intent, String remoteInputResultKey) {
        if (VERSION.SDK_INT >= 26) {
            return android.app.RemoteInput.getDataResultsFromIntent(intent, remoteInputResultKey);
        }
        Map<String, Uri> map = null;
        if (VERSION.SDK_INT >= 16) {
            Intent clipDataIntent = getClipDataIntentFromIntent(intent);
            if (clipDataIntent == null) {
                return null;
            }
            Map<String, Uri> results = new HashMap();
            for (String key : clipDataIntent.getExtras().keySet()) {
                if (key.startsWith(EXTRA_DATA_TYPE_RESULTS_DATA)) {
                    String mimeType = key.substring(EXTRA_DATA_TYPE_RESULTS_DATA.length());
                    if (!mimeType.isEmpty()) {
                        String uriStr = clipDataIntent.getBundleExtra(key).getString(remoteInputResultKey);
                        if (uriStr != null) {
                            if (!uriStr.isEmpty()) {
                                results.put(mimeType, Uri.parse(uriStr));
                            }
                        }
                    }
                }
            }
            if (!results.isEmpty()) {
                map = results;
            }
            return map;
        }
        Log.w(TAG, "RemoteInput is only supported from API Level 16");
        return null;
    }

    public static Bundle getResultsFromIntent(Intent intent) {
        if (VERSION.SDK_INT >= 20) {
            return android.app.RemoteInput.getResultsFromIntent(intent);
        }
        if (VERSION.SDK_INT >= 16) {
            Intent clipDataIntent = getClipDataIntentFromIntent(intent);
            if (clipDataIntent == null) {
                return null;
            }
            return (Bundle) clipDataIntent.getExtras().getParcelable(EXTRA_RESULTS_DATA);
        }
        Log.w(TAG, "RemoteInput is only supported from API Level 16");
        return null;
    }

    public static void addResultsToIntent(RemoteInput[] remoteInputs, Intent intent, Bundle results) {
        if (VERSION.SDK_INT >= 26) {
            android.app.RemoteInput.addResultsToIntent(fromCompat(remoteInputs), intent, results);
            return;
        }
        int i = 0;
        int length;
        RemoteInput input;
        if (VERSION.SDK_INT >= 20) {
            Bundle existingTextResults = getResultsFromIntent(intent);
            if (existingTextResults == null) {
                existingTextResults = results;
            } else {
                existingTextResults.putAll(results);
            }
            for (RemoteInput input2 : remoteInputs) {
                Map<String, Uri> existingDataResults = getDataResultsFromIntent(intent, input2.getResultKey());
                android.app.RemoteInput.addResultsToIntent(fromCompat(new RemoteInput[]{input2}), intent, existingTextResults);
                if (existingDataResults != null) {
                    addDataResultToIntent(input2, intent, existingDataResults);
                }
            }
        } else if (VERSION.SDK_INT >= 16) {
            Intent clipDataIntent = getClipDataIntentFromIntent(intent);
            if (clipDataIntent == null) {
                clipDataIntent = new Intent();
            }
            Bundle resultsBundle = clipDataIntent.getBundleExtra(EXTRA_RESULTS_DATA);
            if (resultsBundle == null) {
                resultsBundle = new Bundle();
            }
            length = remoteInputs.length;
            while (i < length) {
                input2 = remoteInputs[i];
                Object result = results.get(input2.getResultKey());
                if (result instanceof CharSequence) {
                    resultsBundle.putCharSequence(input2.getResultKey(), (CharSequence) result);
                }
                i++;
            }
            clipDataIntent.putExtra(EXTRA_RESULTS_DATA, resultsBundle);
            intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
        } else {
            Log.w(TAG, "RemoteInput is only supported from API Level 16");
        }
    }

    public static void addDataResultToIntent(RemoteInput remoteInput, Intent intent, Map<String, Uri> results) {
        if (VERSION.SDK_INT >= 26) {
            android.app.RemoteInput.addDataResultToIntent(fromCompat(remoteInput), intent, results);
        } else if (VERSION.SDK_INT >= 16) {
            Intent clipDataIntent = getClipDataIntentFromIntent(intent);
            if (clipDataIntent == null) {
                clipDataIntent = new Intent();
            }
            for (Entry<String, Uri> entry : results.entrySet()) {
                String mimeType = (String) entry.getKey();
                Uri uri = (Uri) entry.getValue();
                if (mimeType != null) {
                    Bundle resultsBundle = clipDataIntent.getBundleExtra(getExtraResultsKeyForData(mimeType));
                    if (resultsBundle == null) {
                        resultsBundle = new Bundle();
                    }
                    resultsBundle.putString(remoteInput.getResultKey(), uri.toString());
                    clipDataIntent.putExtra(getExtraResultsKeyForData(mimeType), resultsBundle);
                }
            }
            intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
        } else {
            Log.w(TAG, "RemoteInput is only supported from API Level 16");
        }
    }

    private static String getExtraResultsKeyForData(String mimeType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(EXTRA_DATA_TYPE_RESULTS_DATA);
        stringBuilder.append(mimeType);
        return stringBuilder.toString();
    }

    @RequiresApi(20)
    static android.app.RemoteInput[] fromCompat(RemoteInput[] srcArray) {
        if (srcArray == null) {
            return null;
        }
        android.app.RemoteInput[] result = new android.app.RemoteInput[srcArray.length];
        for (int i = 0; i < srcArray.length; i++) {
            result[i] = fromCompat(srcArray[i]);
        }
        return result;
    }

    @RequiresApi(20)
    static android.app.RemoteInput fromCompat(RemoteInput src) {
        return new android.app.RemoteInput.Builder(src.getResultKey()).setLabel(src.getLabel()).setChoices(src.getChoices()).setAllowFreeFormInput(src.getAllowFreeFormInput()).addExtras(src.getExtras()).build();
    }

    @RequiresApi(16)
    private static Intent getClipDataIntentFromIntent(Intent intent) {
        ClipData clipData = intent.getClipData();
        if (clipData == null) {
            return null;
        }
        ClipDescription clipDescription = clipData.getDescription();
        if (clipDescription.hasMimeType("text/vnd.android.intent") && clipDescription.getLabel().equals(RESULTS_CLIP_LABEL)) {
            return clipData.getItemAt(0).getIntent();
        }
        return null;
    }
}
