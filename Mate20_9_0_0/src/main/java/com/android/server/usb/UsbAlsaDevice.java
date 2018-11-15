package com.android.server.usb;

import android.media.IAudioService;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.audio.AudioService;
import com.android.server.pm.Settings;

public final class UsbAlsaDevice {
    protected static final boolean DEBUG = false;
    private static final String TAG = "UsbAlsaDevice";
    private static final String USB_PERSISTENT_CONFIG = "persist.sys.usb.capture";
    private IAudioService mAudioService;
    private final int mCardNum;
    private final String mDeviceAddress;
    private String mDeviceDescription = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private String mDeviceName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private final int mDeviceNum;
    private final boolean mHasInput;
    private final boolean mHasOutput;
    private int mInputState;
    private final boolean mIsInputHeadset;
    private final boolean mIsOutputHeadset;
    private UsbAlsaJackDetector mJackDetector;
    private int mOutputState;
    private boolean mSelected = false;

    public UsbAlsaDevice(IAudioService audioService, int card, int device, String deviceAddress, boolean hasOutput, boolean hasInput, boolean isInputHeadset, boolean isOutputHeadset) {
        this.mAudioService = audioService;
        this.mCardNum = card;
        this.mDeviceNum = device;
        this.mDeviceAddress = deviceAddress;
        this.mHasOutput = hasOutput;
        this.mHasInput = hasInput;
        this.mIsInputHeadset = isInputHeadset;
        this.mIsOutputHeadset = isOutputHeadset;
    }

    public int getCardNum() {
        return this.mCardNum;
    }

    public int getDeviceNum() {
        return this.mDeviceNum;
    }

    public String getDeviceAddress() {
        return this.mDeviceAddress;
    }

    public String getAlsaCardDeviceString() {
        if (this.mCardNum >= 0 && this.mDeviceNum >= 0) {
            return AudioService.makeAlsaAddressString(this.mCardNum, this.mDeviceNum);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid alsa card or device alsaCard: ");
        stringBuilder.append(this.mCardNum);
        stringBuilder.append(" alsaDevice: ");
        stringBuilder.append(this.mDeviceNum);
        Slog.e(str, stringBuilder.toString());
        return null;
    }

    public boolean hasOutput() {
        return this.mHasOutput;
    }

    public boolean hasInput() {
        return this.mHasInput;
    }

    public boolean isInputHeadset() {
        return this.mIsInputHeadset;
    }

    public boolean isOutputHeadset() {
        return this.mIsOutputHeadset;
    }

    private synchronized boolean isInputJackConnected() {
        if (this.mJackDetector == null) {
            return true;
        }
        return this.mJackDetector.isInputJackConnected();
    }

    private synchronized boolean isOutputJackConnected() {
        if (this.mJackDetector == null) {
            return true;
        }
        return this.mJackDetector.isOutputJackConnected();
    }

    private synchronized void startJackDetect() {
        this.mJackDetector = UsbAlsaJackDetector.startJackDetect(this);
    }

    private synchronized void stopJackDetect() {
        if (this.mJackDetector != null) {
            this.mJackDetector.pleaseStop();
        }
        this.mJackDetector = null;
    }

    public synchronized void start() {
        this.mSelected = true;
        this.mInputState = 0;
        this.mOutputState = 0;
        startJackDetect();
        updateWiredDeviceConnectionState(true);
    }

    public synchronized void stop() {
        stopJackDetect();
        updateWiredDeviceConnectionState(false);
        this.mSelected = false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x0038 A:{Catch:{ RemoteException -> 0x0027 }} */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x007b A:{Catch:{ RemoteException -> 0x0027 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void updateWiredDeviceConnectionState(boolean enable) {
        if (this.mSelected) {
            String alsaCardDeviceString = getAlsaCardDeviceString();
            if (alsaCardDeviceString != null) {
                int inputState;
                if (enable) {
                    try {
                        if (this.mHasInput) {
                            int i;
                            int device;
                            boolean connected;
                            String str;
                            StringBuilder stringBuilder;
                            SystemProperties.set(USB_PERSISTENT_CONFIG, "true");
                            inputState = 0;
                            if (this.mHasOutput) {
                                if (this.mIsOutputHeadset) {
                                    i = 67108864;
                                } else {
                                    i = 16384;
                                }
                                device = i;
                                connected = isOutputJackConnected();
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("OUTPUT JACK connected: ");
                                stringBuilder.append(connected);
                                Slog.i(str, stringBuilder.toString());
                                i = (enable && connected) ? 1 : 0;
                                int outputState = i;
                                if (outputState != this.mOutputState) {
                                    this.mOutputState = outputState;
                                    this.mAudioService.setWiredDeviceConnectionState(device, outputState, alsaCardDeviceString, this.mDeviceName, TAG);
                                }
                            }
                            if (this.mHasInput) {
                                if (this.mIsInputHeadset) {
                                    i = -2113929216;
                                } else {
                                    i = -2147479552;
                                }
                                device = i;
                                connected = isInputJackConnected();
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("INPUT JACK connected: ");
                                stringBuilder.append(connected);
                                Slog.i(str, stringBuilder.toString());
                                if (enable && connected) {
                                    inputState = 1;
                                }
                                if (inputState != this.mInputState) {
                                    this.mInputState = inputState;
                                    this.mAudioService.setWiredDeviceConnectionState(device, inputState, alsaCardDeviceString, this.mDeviceName, TAG);
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        Slog.e(TAG, "RemoteException in setWiredDeviceConnectionState");
                        return;
                    }
                }
                SystemProperties.set(USB_PERSISTENT_CONFIG, "false");
                inputState = 0;
                if (this.mHasOutput) {
                }
                if (this.mHasInput) {
                }
            } else {
                return;
            }
        }
        Slog.e(TAG, "updateWiredDeviceConnectionState on unselected AlsaDevice!");
    }

    public synchronized String toString() {
        StringBuilder stringBuilder;
        stringBuilder = new StringBuilder();
        stringBuilder.append("UsbAlsaDevice: [card: ");
        stringBuilder.append(this.mCardNum);
        stringBuilder.append(", device: ");
        stringBuilder.append(this.mDeviceNum);
        stringBuilder.append(", name: ");
        stringBuilder.append(this.mDeviceName);
        stringBuilder.append(", hasOutput: ");
        stringBuilder.append(this.mHasOutput);
        stringBuilder.append(", hasInput: ");
        stringBuilder.append(this.mHasInput);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public synchronized void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        dump.write("card", 1120986464257L, this.mCardNum);
        dump.write("device", 1120986464258L, this.mDeviceNum);
        dump.write(Settings.ATTR_NAME, 1138166333443L, this.mDeviceName);
        dump.write("has_output", 1133871366148L, this.mHasOutput);
        dump.write("has_input", 1133871366149L, this.mHasInput);
        dump.write(AudioService.CONNECT_INTENT_KEY_ADDRESS, 1138166333446L, this.mDeviceAddress);
        dump.end(token);
    }

    synchronized String toShortString() {
        StringBuilder stringBuilder;
        stringBuilder = new StringBuilder();
        stringBuilder.append("[card:");
        stringBuilder.append(this.mCardNum);
        stringBuilder.append(" device:");
        stringBuilder.append(this.mDeviceNum);
        stringBuilder.append(" ");
        stringBuilder.append(this.mDeviceName);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    synchronized String getDeviceName() {
        return this.mDeviceName;
    }

    synchronized void setDeviceNameAndDescription(String deviceName, String deviceDescription) {
        this.mDeviceName = deviceName;
        this.mDeviceDescription = deviceDescription;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof UsbAlsaDevice)) {
            return false;
        }
        UsbAlsaDevice other = (UsbAlsaDevice) obj;
        if (this.mCardNum == other.mCardNum && this.mDeviceNum == other.mDeviceNum && this.mHasOutput == other.mHasOutput && this.mHasInput == other.mHasInput && this.mIsInputHeadset == other.mIsInputHeadset && this.mIsOutputHeadset == other.mIsOutputHeadset) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * 1) + this.mCardNum)) + this.mDeviceNum)) + (this.mHasOutput ^ 1))) + (this.mHasInput ^ 1))) + (this.mIsInputHeadset ^ 1))) + (this.mIsOutputHeadset ^ 1);
    }
}
