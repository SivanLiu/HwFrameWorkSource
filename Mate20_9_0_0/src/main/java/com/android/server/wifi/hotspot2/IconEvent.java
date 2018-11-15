package com.android.server.wifi.hotspot2;

public class IconEvent {
    private final long mBSSID;
    private final byte[] mData;
    private final String mFileName;
    private final int mSize;

    public IconEvent(long bssid, String fileName, int size, byte[] data) {
        this.mBSSID = bssid;
        this.mFileName = fileName;
        this.mSize = size;
        this.mData = data;
    }

    public long getBSSID() {
        return this.mBSSID;
    }

    public String getFileName() {
        return this.mFileName;
    }

    public int getSize() {
        return this.mSize;
    }

    public byte[] getData() {
        return this.mData;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IconEvent: BSSID=");
        stringBuilder.append(String.format("%012x", new Object[]{Long.valueOf(this.mBSSID)}));
        stringBuilder.append(", fileName='");
        stringBuilder.append(this.mFileName);
        stringBuilder.append('\'');
        stringBuilder.append(", size=");
        stringBuilder.append(this.mSize);
        return stringBuilder.toString();
    }
}
