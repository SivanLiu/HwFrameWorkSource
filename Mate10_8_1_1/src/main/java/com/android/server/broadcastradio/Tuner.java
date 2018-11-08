package com.android.server.broadcastradio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.radio.ITuner.Stub;
import android.hardware.radio.ITunerCallback;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager.BandConfig;
import android.hardware.radio.RadioManager.ProgramInfo;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Slog;
import java.util.List;
import java.util.Map;

class Tuner extends Stub {
    private static final String TAG = "BroadcastRadioService.Tuner";
    private final ITunerCallback mClientCallback;
    private final DeathRecipient mDeathRecipient;
    private boolean mIsClosed = false;
    private boolean mIsMuted = false;
    private final Object mLock = new Object();
    private final long mNativeContext;
    private int mRegion;
    private final TunerCallback mTunerCallback;
    private final boolean mWithAudio;

    private native void nativeCancel(long j);

    private native void nativeCancelAnnouncement(long j);

    private native void nativeClose(long j);

    private native void nativeFinalize(long j);

    private native BandConfig nativeGetConfiguration(long j, int i);

    private native byte[] nativeGetImage(long j, int i);

    private native ProgramInfo nativeGetProgramInformation(long j);

    private native List<ProgramInfo> nativeGetProgramList(long j, Map<String, String> map);

    private native long nativeInit(int i, boolean z, int i2);

    private native boolean nativeIsAnalogForced(long j);

    private native boolean nativeIsAntennaConnected(long j);

    private native void nativeScan(long j, boolean z, boolean z2);

    private native void nativeSetAnalogForced(long j, boolean z);

    private native void nativeSetConfiguration(long j, BandConfig bandConfig);

    private native void nativeSetMuted(long j, boolean z);

    private native boolean nativeStartBackgroundScan(long j);

    private native void nativeStep(long j, boolean z, boolean z2);

    private native void nativeTune(long j, ProgramSelector programSelector);

    Tuner(ITunerCallback clientCallback, int halRev, int region, boolean withAudio, int band) {
        this.mClientCallback = clientCallback;
        this.mTunerCallback = new TunerCallback(this, clientCallback, halRev);
        this.mRegion = region;
        this.mWithAudio = withAudio;
        this.mNativeContext = nativeInit(halRev, withAudio, band);
        this.mDeathRecipient = new -$Lambda$B3g7x97xEp_kpgRrmZTNuVQljJA(this);
        try {
            this.mClientCallback.asBinder().linkToDeath(this.mDeathRecipient, 0);
        } catch (RemoteException e) {
            close();
        }
    }

    /* synthetic */ void -com_android_server_broadcastradio_Tuner-mthref-0() {
        close();
    }

    protected void finalize() throws Throwable {
        nativeFinalize(this.mNativeContext);
        super.finalize();
    }

    public void close() {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mIsClosed = true;
            this.mTunerCallback.detach();
            this.mClientCallback.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
            nativeClose(this.mNativeContext);
        }
    }

    public boolean isClosed() {
        return this.mIsClosed;
    }

    private void checkNotClosedLocked() {
        if (this.mIsClosed) {
            throw new IllegalStateException("Tuner is closed, no further operations are allowed");
        }
    }

    public void setConfiguration(BandConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("The argument must not be a null pointer");
        }
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeSetConfiguration(this.mNativeContext, config);
            this.mRegion = config.getRegion();
        }
    }

    public BandConfig getConfiguration() {
        BandConfig nativeGetConfiguration;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeGetConfiguration = nativeGetConfiguration(this.mNativeContext, this.mRegion);
        }
        return nativeGetConfiguration;
    }

    public void setMuted(boolean mute) {
        if (this.mWithAudio) {
            synchronized (this.mLock) {
                checkNotClosedLocked();
                if (this.mIsMuted == mute) {
                    return;
                }
                this.mIsMuted = mute;
                nativeSetMuted(this.mNativeContext, mute);
                return;
            }
        }
        throw new IllegalStateException("Can't operate on mute - no audio requested");
    }

    public boolean isMuted() {
        if (this.mWithAudio) {
            boolean z;
            synchronized (this.mLock) {
                checkNotClosedLocked();
                z = this.mIsMuted;
            }
            return z;
        }
        Slog.w(TAG, "Tuner did not request audio, pretending it was muted");
        return true;
    }

    public void step(boolean directionDown, boolean skipSubChannel) {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeStep(this.mNativeContext, directionDown, skipSubChannel);
        }
    }

    public void scan(boolean directionDown, boolean skipSubChannel) {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeScan(this.mNativeContext, directionDown, skipSubChannel);
        }
    }

    public void tune(ProgramSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("The argument must not be a null pointer");
        }
        Slog.i(TAG, "Tuning to " + selector);
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeTune(this.mNativeContext, selector);
        }
    }

    public void cancel() {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeCancel(this.mNativeContext);
        }
    }

    public void cancelAnnouncement() {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeCancelAnnouncement(this.mNativeContext);
        }
    }

    public ProgramInfo getProgramInformation() {
        ProgramInfo nativeGetProgramInformation;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeGetProgramInformation = nativeGetProgramInformation(this.mNativeContext);
        }
        return nativeGetProgramInformation;
    }

    public Bitmap getImage(int id) {
        if (id == 0) {
            throw new IllegalArgumentException("Image ID is missing");
        }
        synchronized (this.mLock) {
            byte[] rawImage = nativeGetImage(this.mNativeContext, id);
        }
        if (rawImage == null || rawImage.length == 0) {
            return null;
        }
        return BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length);
    }

    public boolean startBackgroundScan() {
        boolean nativeStartBackgroundScan;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeStartBackgroundScan = nativeStartBackgroundScan(this.mNativeContext);
        }
        return nativeStartBackgroundScan;
    }

    public List<ProgramInfo> getProgramList(Map vendorFilter) {
        List<ProgramInfo> list;
        Map<String, String> sFilter = vendorFilter;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            list = nativeGetProgramList(this.mNativeContext, vendorFilter);
            if (list == null) {
                throw new IllegalStateException("Program list is not ready");
            }
        }
        return list;
    }

    public boolean isAnalogForced() {
        boolean nativeIsAnalogForced;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeIsAnalogForced = nativeIsAnalogForced(this.mNativeContext);
        }
        return nativeIsAnalogForced;
    }

    public void setAnalogForced(boolean isForced) {
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeSetAnalogForced(this.mNativeContext, isForced);
        }
    }

    public boolean isAntennaConnected() {
        boolean nativeIsAntennaConnected;
        synchronized (this.mLock) {
            checkNotClosedLocked();
            nativeIsAntennaConnected = nativeIsAntennaConnected(this.mNativeContext);
        }
        return nativeIsAntennaConnected;
    }
}
