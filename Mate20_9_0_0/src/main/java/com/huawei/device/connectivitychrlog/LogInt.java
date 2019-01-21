package com.huawei.device.connectivitychrlog;

public class LogInt {
    private static final String LOG_TAG = "LogInt";
    private byte[] bytesValue = null;
    private int length = 4;
    private int value = 0;

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
        this.bytesValue = ByteConvert.intToBytes(this.value);
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
        this.value = ByteConvert.littleEndianBytesToInt(src);
        this.bytesValue = ByteConvert.intToBytes(this.value);
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setByByteArray value = ");
        stringBuilder.append(this.value);
        ChrLog.chrLogD(str, stringBuilder.toString());
    }

    public int getLength() {
        return this.length;
    }

    public byte[] toByteArray() {
        if (this.bytesValue != null) {
            return (byte[]) this.bytesValue.clone();
        }
        return ByteConvert.intToBytes(this.value);
    }
}
