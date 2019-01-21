package android.media;

import android.util.Log;

public class HwAudioRecordDummy implements IHwAudioRecord {
    private static final String TAG = "HwAudioRecordDummy";
    private static IHwAudioRecord mHwAudioRecoder = new HwAudioRecordDummy();

    private HwAudioRecordDummy() {
    }

    public static IHwAudioRecord getDefault() {
        return mHwAudioRecoder;
    }

    public void sendStateChangedIntent(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dummy sendStateChangedIntent, state=");
        stringBuilder.append(state);
        Log.w(str, stringBuilder.toString());
    }

    public boolean isAudioRecordAllowed() {
        Log.w(TAG, "dummy isAudioRecordAllowed ");
        return true;
    }

    public void showDisableMicrophoneToast() {
        Log.w(TAG, "dummy showDisableMicrophoneToast");
    }
}
