package com.android.server.usb;

public final class UsbAudioDevice {
    protected static final boolean DEBUG = false;
    private static final String TAG = "UsbAudioDevice";
    public static final int kAudioDeviceClassMask = 16777215;
    public static final int kAudioDeviceClass_External = 2;
    public static final int kAudioDeviceClass_Internal = 1;
    public static final int kAudioDeviceClass_Undefined = 0;
    public static final int kAudioDeviceMetaMask = -16777216;
    public static final int kAudioDeviceMeta_Alsa = Integer.MIN_VALUE;
    public final int mCard;
    public final int mDevice;
    public final int mDeviceClass;
    private String mDeviceDescription = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private String mDeviceName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    public final boolean mHasCapture;
    public final boolean mHasPlayback;
    public boolean mIsInputHeadset = false;
    public boolean mIsOutputHeadset = false;

    public UsbAudioDevice(int card, int device, boolean hasPlayback, boolean hasCapture, int deviceClass) {
        this.mCard = card;
        this.mDevice = device;
        this.mHasPlayback = hasPlayback;
        this.mHasCapture = hasCapture;
        this.mDeviceClass = deviceClass;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UsbAudioDevice: [card: ");
        stringBuilder.append(this.mCard);
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", device: ");
        stringBuilder.append(this.mDevice);
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", name: ");
        stringBuilder.append(this.mDeviceName);
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", hasPlayback: ");
        stringBuilder.append(this.mHasPlayback);
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", hasCapture: ");
        stringBuilder.append(this.mHasCapture);
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", class: 0x");
        stringBuilder.append(Integer.toHexString(this.mDeviceClass));
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        return sb.toString();
    }

    String toShortString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[card:");
        stringBuilder.append(this.mCard);
        stringBuilder.append(" device:");
        stringBuilder.append(this.mDevice);
        stringBuilder.append(" ");
        stringBuilder.append(this.mDeviceName);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    String getDeviceName() {
        return this.mDeviceName;
    }

    void setDeviceNameAndDescription(String deviceName, String deviceDescription) {
        this.mDeviceName = deviceName;
        this.mDeviceDescription = deviceDescription;
    }

    void setHeadsetStatus(boolean isInputHeadset, boolean isOutputHeadset) {
        this.mIsInputHeadset = isInputHeadset;
        this.mIsOutputHeadset = isOutputHeadset;
    }
}
