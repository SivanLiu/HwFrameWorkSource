package android.hardware.radio;

import android.graphics.Bitmap;
import android.hardware.radio.RadioManager.BandConfig;
import android.hardware.radio.RadioManager.ProgramInfo;
import android.os.RemoteException;
import android.util.Log;
import java.util.List;
import java.util.Map;

class TunerAdapter extends RadioTuner {
    private static final String TAG = "BroadcastRadio.TunerAdapter";
    private int mBand;
    private boolean mIsClosed = false;
    private final ITuner mTuner;

    TunerAdapter(ITuner tuner, int band) {
        if (tuner == null) {
            throw new NullPointerException();
        }
        this.mTuner = tuner;
        this.mBand = band;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void close() {
        synchronized (this.mTuner) {
            if (this.mIsClosed) {
                Log.v(TAG, "Tuner is already closed");
                return;
            }
            this.mIsClosed = true;
        }
    }

    public int setConfiguration(BandConfig config) {
        try {
            this.mTuner.setConfiguration(config);
            this.mBand = config.getType();
            return 0;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Can't set configuration", e);
            return -22;
        } catch (RemoteException e2) {
            Log.e(TAG, "service died", e2);
            return -32;
        }
    }

    public int getConfiguration(BandConfig[] config) {
        if (config == null || config.length != 1) {
            throw new IllegalArgumentException("The argument must be an array of length 1");
        }
        try {
            config[0] = this.mTuner.getConfiguration();
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return -32;
        }
    }

    public int setMute(boolean mute) {
        try {
            this.mTuner.setMuted(mute);
            return 0;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't set muted", e);
            return Integer.MIN_VALUE;
        } catch (RemoteException e2) {
            Log.e(TAG, "service died", e2);
            return -32;
        }
    }

    public boolean getMute() {
        try {
            return this.mTuner.isMuted();
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return true;
        }
    }

    public int step(int direction, boolean skipSubChannel) {
        boolean z = true;
        try {
            ITuner iTuner = this.mTuner;
            if (direction != 1) {
                z = false;
            }
            iTuner.step(z, skipSubChannel);
            return 0;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't step", e);
            return -38;
        } catch (RemoteException e2) {
            Log.e(TAG, "service died", e2);
            return -32;
        }
    }

    public int scan(int direction, boolean skipSubChannel) {
        boolean z = true;
        try {
            ITuner iTuner = this.mTuner;
            if (direction != 1) {
                z = false;
            }
            iTuner.scan(z, skipSubChannel);
            return 0;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't scan", e);
            return -38;
        } catch (RemoteException e2) {
            Log.e(TAG, "service died", e2);
            return -32;
        }
    }

    public int tune(int channel, int subChannel) {
        try {
            this.mTuner.tune(ProgramSelector.createAmFmSelector(this.mBand, channel, subChannel));
            return 0;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't tune", e);
            return -38;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "Can't tune", e2);
            return -22;
        } catch (RemoteException e3) {
            Log.e(TAG, "service died", e3);
            return -32;
        }
    }

    public void tune(ProgramSelector selector) {
        try {
            this.mTuner.tune(selector);
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    public int cancel() {
        try {
            this.mTuner.cancel();
            return 0;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Can't cancel", e);
            return -38;
        } catch (RemoteException e2) {
            Log.e(TAG, "service died", e2);
            return -32;
        }
    }

    public void cancelAnnouncement() {
        try {
            this.mTuner.cancelAnnouncement();
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    public int getProgramInformation(ProgramInfo[] info) {
        if (info == null || info.length != 1) {
            throw new IllegalArgumentException("The argument must be an array of length 1");
        }
        try {
            info[0] = this.mTuner.getProgramInformation();
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "service died", e);
            return -32;
        }
    }

    public Bitmap getMetadataImage(int id) {
        try {
            return this.mTuner.getImage(id);
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    public boolean startBackgroundScan() {
        try {
            return this.mTuner.startBackgroundScan();
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    public List<ProgramInfo> getProgramList(Map<String, String> vendorFilter) {
        try {
            return this.mTuner.getProgramList(vendorFilter);
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    public boolean isAnalogForced() {
        try {
            return this.mTuner.isAnalogForced();
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    public void setAnalogForced(boolean isForced) {
        try {
            this.mTuner.setAnalogForced(isForced);
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    public boolean isAntennaConnected() {
        try {
            return this.mTuner.isAntennaConnected();
        } catch (RemoteException e) {
            throw new RuntimeException("service died", e);
        }
    }

    public boolean hasControl() {
        try {
            return this.mTuner.isClosed() ^ 1;
        } catch (RemoteException e) {
            return false;
        }
    }
}
