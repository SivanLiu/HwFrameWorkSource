package com.huawei.device.connectivitychrlog;

public class LogLong {
    private static final String LOG_TAG = "LogLong";
    private byte[] bytesValue = null;
    private int length = 8;
    private long value = 0;

    public long getValue() {
        return this.value;
    }

    public void setValue(long value) {
        this.value = value;
        this.bytesValue = ByteConvert.longToBytes(this.value);
    }

    public void setValue(int value) {
        this.value = (long) value;
        this.bytesValue = ByteConvert.longToBytes(this.value);
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
        this.value = ByteConvert.littleEndianbytesToLong(src);
        this.bytesValue = ByteConvert.longToBytes(this.value);
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
        return ByteConvert.longToBytes(this.value);
    }
}
