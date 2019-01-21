package android.hardware.radio;

import android.hardware.radio.ITunerCallback.Stub;
import android.hardware.radio.ProgramList.Chunk;
import android.hardware.radio.RadioManager.BandConfig;
import android.hardware.radio.RadioManager.ProgramInfo;
import android.hardware.radio.RadioTuner.Callback;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class TunerCallbackAdapter extends Stub {
    private static final String TAG = "BroadcastRadio.TunerCallbackAdapter";
    private final Callback mCallback;
    ProgramInfo mCurrentProgramInfo;
    private boolean mDelayedCompleteCallback = false;
    private final Handler mHandler;
    boolean mIsAntennaConnected = true;
    List<ProgramInfo> mLastCompleteList;
    private final Object mLock = new Object();
    ProgramList mProgramList;

    TunerCallbackAdapter(Callback callback, Handler handler) {
        this.mCallback = callback;
        if (handler == null) {
            this.mHandler = new Handler(Looper.getMainLooper());
        } else {
            this.mHandler = handler;
        }
    }

    void close() {
        synchronized (this.mLock) {
            if (this.mProgramList != null) {
                this.mProgramList.close();
            }
        }
    }

    void setProgramListObserver(ProgramList programList, OnCloseListener closeListener) {
        Objects.requireNonNull(closeListener);
        synchronized (this.mLock) {
            if (this.mProgramList != null) {
                Log.w(TAG, "Previous program list observer wasn't properly closed, closing it...");
                this.mProgramList.close();
            }
            this.mProgramList = programList;
            if (programList == null) {
                return;
            }
            programList.setOnCloseListener(new -$$Lambda$TunerCallbackAdapter$Hl80-0ppQ17uTjZuGamwBQMrO6Y(this, programList, closeListener));
            programList.addOnCompleteListener(new -$$Lambda$TunerCallbackAdapter$V-mJUy8dIlOVjsZ1ckkgn490jFI(this, programList));
        }
    }

    public static /* synthetic */ void lambda$setProgramListObserver$0(TunerCallbackAdapter tunerCallbackAdapter, ProgramList programList, OnCloseListener closeListener) {
        synchronized (tunerCallbackAdapter.mLock) {
            if (tunerCallbackAdapter.mProgramList != programList) {
                return;
            }
            tunerCallbackAdapter.mProgramList = null;
            tunerCallbackAdapter.mLastCompleteList = null;
            closeListener.onClose();
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$setProgramListObserver$1(TunerCallbackAdapter tunerCallbackAdapter, ProgramList programList) {
        synchronized (tunerCallbackAdapter.mLock) {
            if (tunerCallbackAdapter.mProgramList != programList) {
                return;
            }
            tunerCallbackAdapter.mLastCompleteList = programList.toList();
            if (tunerCallbackAdapter.mDelayedCompleteCallback) {
                Log.d(TAG, "Sending delayed onBackgroundScanComplete callback");
                tunerCallbackAdapter.sendBackgroundScanCompleteLocked();
            }
        }
    }

    List<ProgramInfo> getLastCompleteList() {
        List list;
        synchronized (this.mLock) {
            list = this.mLastCompleteList;
        }
        return list;
    }

    void clearLastCompleteList() {
        synchronized (this.mLock) {
            this.mLastCompleteList = null;
        }
    }

    ProgramInfo getCurrentProgramInformation() {
        ProgramInfo programInfo;
        synchronized (this.mLock) {
            programInfo = this.mCurrentProgramInfo;
        }
        return programInfo;
    }

    boolean isAntennaConnected() {
        return this.mIsAntennaConnected;
    }

    public void onError(int status) {
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$jl29exheqPoYrltfLs9fLsjsI1A(this, status));
    }

    /* JADX WARNING: Missing block: B:11:0x001f, code skipped:
            if (r4 != -1) goto L_0x003f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onTuneFailed(int status, ProgramSelector selector) {
        int errorCode;
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$Hj_P___HTEx_8p7qvYVPXmhwu7w(this, status, selector));
        if (!(status == Integer.MIN_VALUE || status == -38)) {
            if (status != -32) {
                if (!(status == -22 || status == -19)) {
                }
            }
            errorCode = 1;
            this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$HcS5_voI1xju970_jCP6Iz0LgPE(this, errorCode));
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Got an error with no mapping to the legacy API (");
        stringBuilder.append(status);
        stringBuilder.append("), doing a best-effort conversion to ERROR_SCAN_TIMEOUT");
        Log.i(str, stringBuilder.toString());
        errorCode = 3;
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$HcS5_voI1xju970_jCP6Iz0LgPE(this, errorCode));
    }

    public void onConfigurationChanged(BandConfig config) {
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$B4BuskgdSatf-Xt5wzgLniEltQk(this, config));
    }

    public void onCurrentProgramInfoChanged(ProgramInfo info) {
        if (info == null) {
            Log.e(TAG, "ProgramInfo must not be null");
            return;
        }
        synchronized (this.mLock) {
            this.mCurrentProgramInfo = info;
        }
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$RSNrzX5-O3nayC2_jg0kAR6KkKY(this, info));
    }

    public static /* synthetic */ void lambda$onCurrentProgramInfoChanged$6(TunerCallbackAdapter tunerCallbackAdapter, ProgramInfo info) {
        tunerCallbackAdapter.mCallback.onProgramInfoChanged(info);
        RadioMetadata metadata = info.getMetadata();
        if (metadata != null) {
            tunerCallbackAdapter.mCallback.onMetadataChanged(metadata);
        }
    }

    public void onTrafficAnnouncement(boolean active) {
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$tiaoLZrR2K56rYeqHvSRh5lRdBI(this, active));
    }

    public void onEmergencyAnnouncement(boolean active) {
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$ZwPm3xxjeLvbP12KweyzqFJVnj4(this, active));
    }

    public void onAntennaState(boolean connected) {
        this.mIsAntennaConnected = connected;
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$dR-VQmFrL_tBD2wpNvborTd8W08(this, connected));
    }

    public void onBackgroundScanAvailabilityChange(boolean isAvailable) {
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$4zf9n0sz_rU8z6a9GJmRInWrYkQ(this, isAvailable));
    }

    private void sendBackgroundScanCompleteLocked() {
        this.mDelayedCompleteCallback = false;
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$xIUT1Qu5TkA83V8ttYy1zv-JuFo(this));
    }

    public void onBackgroundScanComplete() {
        synchronized (this.mLock) {
            if (this.mLastCompleteList == null) {
                Log.i(TAG, "Got onBackgroundScanComplete callback, but the program list didn't get through yet. Delaying it...");
                this.mDelayedCompleteCallback = true;
                return;
            }
            sendBackgroundScanCompleteLocked();
        }
    }

    public void onProgramListChanged() {
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$UsmGhKordXy4lhCylRP0mm2NcYc(this));
    }

    public void onProgramListUpdated(Chunk chunk) {
        synchronized (this.mLock) {
            if (this.mProgramList == null) {
                return;
            }
            this.mProgramList.apply((Chunk) Objects.requireNonNull(chunk));
        }
    }

    public void onParametersUpdated(Map parameters) {
        this.mHandler.post(new -$$Lambda$TunerCallbackAdapter$Yz-4KCDu1MOynGdkDf_oMxqhjeY(this, parameters));
    }
}
