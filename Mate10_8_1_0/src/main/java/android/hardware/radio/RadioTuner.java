package android.hardware.radio;

import android.graphics.Bitmap;
import android.hardware.radio.RadioManager.BandConfig;
import android.hardware.radio.RadioManager.ProgramInfo;
import java.util.List;
import java.util.Map;

public abstract class RadioTuner {
    public static final int DIRECTION_DOWN = 1;
    public static final int DIRECTION_UP = 0;
    public static final int ERROR_BACKGROUND_SCAN_FAILED = 6;
    public static final int ERROR_BACKGROUND_SCAN_UNAVAILABLE = 5;
    public static final int ERROR_CANCELLED = 2;
    public static final int ERROR_CONFIG = 4;
    public static final int ERROR_HARDWARE_FAILURE = 0;
    public static final int ERROR_SCAN_TIMEOUT = 3;
    public static final int ERROR_SERVER_DIED = 1;

    public static abstract class Callback {
        public void onError(int status) {
        }

        public void onConfigurationChanged(BandConfig config) {
        }

        public void onProgramInfoChanged(ProgramInfo info) {
        }

        @Deprecated
        public void onMetadataChanged(RadioMetadata metadata) {
        }

        public void onTrafficAnnouncement(boolean active) {
        }

        public void onEmergencyAnnouncement(boolean active) {
        }

        public void onAntennaState(boolean connected) {
        }

        public void onControlChanged(boolean control) {
        }

        public void onBackgroundScanAvailabilityChange(boolean isAvailable) {
        }

        public void onBackgroundScanComplete() {
        }

        public void onProgramListChanged() {
        }
    }

    public abstract int cancel();

    public abstract void cancelAnnouncement();

    public abstract void close();

    public abstract int getConfiguration(BandConfig[] bandConfigArr);

    public abstract Bitmap getMetadataImage(int i);

    public abstract boolean getMute();

    public abstract int getProgramInformation(ProgramInfo[] programInfoArr);

    public abstract List<ProgramInfo> getProgramList(Map<String, String> map);

    public abstract boolean hasControl();

    public abstract boolean isAnalogForced();

    public abstract boolean isAntennaConnected();

    public abstract int scan(int i, boolean z);

    public abstract void setAnalogForced(boolean z);

    public abstract int setConfiguration(BandConfig bandConfig);

    public abstract int setMute(boolean z);

    public abstract boolean startBackgroundScan();

    public abstract int step(int i, boolean z);

    @Deprecated
    public abstract int tune(int i, int i2);

    public abstract void tune(ProgramSelector programSelector);
}
