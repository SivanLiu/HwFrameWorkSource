package com.android.server.broadcastradio;

import android.hardware.radio.ITunerCallback;
import android.hardware.radio.RadioManager.BandConfig;
import android.hardware.radio.RadioManager.ProgramInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.broadcastradio.-$Lambda$UibxWVH8zVvcNBN03iM01Oc7JJI.AnonymousClass1;
import com.android.server.broadcastradio.-$Lambda$UibxWVH8zVvcNBN03iM01Oc7JJI.AnonymousClass2;
import com.android.server.broadcastradio.-$Lambda$UibxWVH8zVvcNBN03iM01Oc7JJI.AnonymousClass3;

class TunerCallback implements ITunerCallback {
    private static final String TAG = "BroadcastRadioService.TunerCallback";
    private final ITunerCallback mClientCallback;
    private final long mNativeContext;
    private final Tuner mTuner;

    private interface RunnableThrowingRemoteException {
        void run() throws RemoteException;
    }

    private native void nativeDetach(long j);

    private native void nativeFinalize(long j);

    private native long nativeInit(Tuner tuner, int i);

    TunerCallback(Tuner tuner, ITunerCallback clientCallback, int halRev) {
        this.mTuner = tuner;
        this.mClientCallback = clientCallback;
        this.mNativeContext = nativeInit(tuner, halRev);
    }

    protected void finalize() throws Throwable {
        nativeFinalize(this.mNativeContext);
        super.finalize();
    }

    public void detach() {
        nativeDetach(this.mNativeContext);
    }

    private void dispatch(RunnableThrowingRemoteException func) {
        try {
            func.run();
        } catch (RemoteException e) {
            Slog.e(TAG, "client died", e);
        }
    }

    private void handleHwFailure() {
        onError(0);
        this.mTuner.close();
    }

    /* synthetic */ void lambda$-com_android_server_broadcastradio_TunerCallback_2499(int status) throws RemoteException {
        this.mClientCallback.onError(status);
    }

    public void onError(int status) {
        dispatch(new AnonymousClass2(status, this));
    }

    /* synthetic */ void lambda$-com_android_server_broadcastradio_TunerCallback_2650(BandConfig config) throws RemoteException {
        this.mClientCallback.onConfigurationChanged(config);
    }

    public void onConfigurationChanged(BandConfig config) {
        dispatch(new AnonymousClass1((byte) 0, this, config));
    }

    /* synthetic */ void lambda$-com_android_server_broadcastradio_TunerCallback_2820(ProgramInfo info) throws RemoteException {
        this.mClientCallback.onCurrentProgramInfoChanged(info);
    }

    public void onCurrentProgramInfoChanged(ProgramInfo info) {
        dispatch(new AnonymousClass1((byte) 1, this, info));
    }

    /* synthetic */ void lambda$-com_android_server_broadcastradio_TunerCallback_2972(boolean active) throws RemoteException {
        this.mClientCallback.onTrafficAnnouncement(active);
    }

    public void onTrafficAnnouncement(boolean active) {
        dispatch(new AnonymousClass3((byte) 3, active, this));
    }

    /* synthetic */ void lambda$-com_android_server_broadcastradio_TunerCallback_3122(boolean active) throws RemoteException {
        this.mClientCallback.onEmergencyAnnouncement(active);
    }

    public void onEmergencyAnnouncement(boolean active) {
        dispatch(new AnonymousClass3((byte) 2, active, this));
    }

    /* synthetic */ void lambda$-com_android_server_broadcastradio_TunerCallback_3268(boolean connected) throws RemoteException {
        this.mClientCallback.onAntennaState(connected);
    }

    public void onAntennaState(boolean connected) {
        dispatch(new AnonymousClass3((byte) 0, connected, this));
    }

    /* synthetic */ void lambda$-com_android_server_broadcastradio_TunerCallback_3430(boolean isAvailable) throws RemoteException {
        this.mClientCallback.onBackgroundScanAvailabilityChange(isAvailable);
    }

    public void onBackgroundScanAvailabilityChange(boolean isAvailable) {
        dispatch(new AnonymousClass3((byte) 1, isAvailable, this));
    }

    /* synthetic */ void lambda$-com_android_server_broadcastradio_TunerCallback_3585() throws RemoteException {
        this.mClientCallback.onBackgroundScanComplete();
    }

    public void onBackgroundScanComplete() {
        dispatch(new -$Lambda$UibxWVH8zVvcNBN03iM01Oc7JJI((byte) 0, this));
    }

    /* synthetic */ void lambda$-com_android_server_broadcastradio_TunerCallback_3715() throws RemoteException {
        this.mClientCallback.onProgramListChanged();
    }

    public void onProgramListChanged() {
        dispatch(new -$Lambda$UibxWVH8zVvcNBN03iM01Oc7JJI((byte) 1, this));
    }

    public IBinder asBinder() {
        throw new RuntimeException("Not a binder");
    }
}
