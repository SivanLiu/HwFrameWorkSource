package android.media;

import android.util.Log;

public class HwMediaRecorderDummy implements IHwMediaRecorder {
    private static final String TAG = "HwMediaRecorderDummy";
    private static IHwMediaRecorder mHwMediaRecoder = new HwMediaRecorderDummy();
    IAudioService mAudioService = null;

    private HwMediaRecorderDummy() {
    }

    public static IHwMediaRecorder getDefault() {
        return mHwMediaRecoder;
    }

    public void sendStateChangedIntent(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dummy sendStateChangedIntent, state=");
        stringBuilder.append(state);
        Log.w(str, stringBuilder.toString());
    }

    public void showDisableMicrophoneToast() {
        Log.w(TAG, "dummy showDisableMicrophoneToast");
    }
}
