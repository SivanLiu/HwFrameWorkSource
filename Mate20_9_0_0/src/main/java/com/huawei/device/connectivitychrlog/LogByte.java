package com.huawei.device.connectivitychrlog;

public class LogByte {
    private static final String LOG_TAG = "LogByte";
    private int length = 1;
    private byte value = (byte) 0;

    public byte getValue() {
        return this.value;
    }

    public void setValue(byte value) {
        this.value = value;
    }

    public void setByByteArray(byte[] src, int len, boolean bIsLittleEndian) {
        String str;
        StringBuilder stringBuilder;
        if (this.length != len) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setByByteArray failed ,not support len = ");
            stringBuilder.append(len);
            ChrLog.chrLogE(str, stringBuilder.toString());
        }
        this.value = src[0];
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setByByteArray value = ");
        stringBuilder.append(this.value);
        ChrLog.chrLogD(str, stringBuilder.toString());
    }

    public void setValue(int value) {
        this.value = (byte) value;
    }

    public int getLength() {
        return this.length;
    }

    public byte[] toByteArray() {
        return new byte[]{this.value};
    }
}
