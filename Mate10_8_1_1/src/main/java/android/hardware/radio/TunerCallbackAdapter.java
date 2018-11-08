package android.hardware.radio;

import android.hardware.radio.-$Lambda$JnOBQcNE2QHtc2zY4hNL33J974o.AnonymousClass1;
import android.hardware.radio.-$Lambda$JnOBQcNE2QHtc2zY4hNL33J974o.AnonymousClass2;
import android.hardware.radio.-$Lambda$JnOBQcNE2QHtc2zY4hNL33J974o.AnonymousClass3;
import android.hardware.radio.ITunerCallback.Stub;
import android.hardware.radio.RadioManager.BandConfig;
import android.hardware.radio.RadioManager.ProgramInfo;
import android.hardware.radio.RadioTuner.Callback;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

class TunerCallbackAdapter extends Stub {
    private static final String TAG = "BroadcastRadio.TunerCallbackAdapter";
    private final Callback mCallback;
    private final Handler mHandler;

    TunerCallbackAdapter(Callback callback, Handler handler) {
        this.mCallback = callback;
        if (handler == null) {
            this.mHandler = new Handler(Looper.getMainLooper());
        } else {
            this.mHandler = handler;
        }
    }

    /* synthetic */ void lambda$-android_hardware_radio_TunerCallbackAdapter_1493(int status) {
        this.mCallback.onError(status);
    }

    public void onError(int status) {
        this.mHandler.post(new AnonymousClass2(status, this));
    }

    /* synthetic */ void lambda$-android_hardware_radio_TunerCallbackAdapter_1643(BandConfig config) {
        this.mCallback.onConfigurationChanged(config);
    }

    public void onConfigurationChanged(BandConfig config) {
        this.mHandler.post(new AnonymousClass1((byte) 0, this, config));
    }

    public void onCurrentProgramInfoChanged(ProgramInfo info) {
        if (info == null) {
            Log.e(TAG, "ProgramInfo must not be null");
        } else {
            this.mHandler.post(new AnonymousClass1((byte) 1, this, info));
        }
    }

    /* synthetic */ void lambda$-android_hardware_radio_TunerCallbackAdapter_1927(ProgramInfo info) {
        this.mCallback.onProgramInfoChanged(info);
        RadioMetadata metadata = info.getMetadata();
        if (metadata != null) {
            this.mCallback.onMetadataChanged(metadata);
        }
    }

    /* synthetic */ void lambda$-android_hardware_radio_TunerCallbackAdapter_2227(boolean active) {
        this.mCallback.onTrafficAnnouncement(active);
    }

    public void onTrafficAnnouncement(boolean active) {
        this.mHandler.post(new AnonymousClass3((byte) 3, active, this));
    }

    /* synthetic */ void lambda$-android_hardware_radio_TunerCallbackAdapter_2376(boolean active) {
        this.mCallback.onEmergencyAnnouncement(active);
    }

    public void onEmergencyAnnouncement(boolean active) {
        this.mHandler.post(new AnonymousClass3((byte) 2, active, this));
    }

    /* synthetic */ void lambda$-android_hardware_radio_TunerCallbackAdapter_2521(boolean connected) {
        this.mCallback.onAntennaState(connected);
    }

    public void onAntennaState(boolean connected) {
        this.mHandler.post(new AnonymousClass3((byte) 0, connected, this));
    }

    /* synthetic */ void lambda$-android_hardware_radio_TunerCallbackAdapter_2682(boolean isAvailable) {
        this.mCallback.onBackgroundScanAvailabilityChange(isAvailable);
    }

    public void onBackgroundScanAvailabilityChange(boolean isAvailable) {
        this.mHandler.post(new AnonymousClass3((byte) 1, isAvailable, this));
    }

    /* synthetic */ void lambda$-android_hardware_radio_TunerCallbackAdapter_2836() {
        this.mCallback.onBackgroundScanComplete();
    }

    public void onBackgroundScanComplete() {
        this.mHandler.post(new -$Lambda$JnOBQcNE2QHtc2zY4hNL33J974o((byte) 0, this));
    }

    /* synthetic */ void lambda$-android_hardware_radio_TunerCallbackAdapter_2965() {
        this.mCallback.onProgramListChanged();
    }

    public void onProgramListChanged() {
        this.mHandler.post(new -$Lambda$JnOBQcNE2QHtc2zY4hNL33J974o((byte) 1, this));
    }
}
