package com.huawei.device.connectivitychrlog;

public class LogShort {
    private static final String LOG_TAG = "LogShort";
    private byte[] bytesValue = null;
    private int length = 2;
    private short value = (short) 0;

    public short getValue() {
        return this.value;
    }

    public void setValue(short value) {
        this.value = value;
        this.bytesValue = ByteConvert.shortToBytes(this.value);
    }

    public void setValue(int value) {
        this.value = (short) value;
        this.bytesValue = ByteConvert.shortToBytes(this.value);
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
        this.value = ByteConvert.littleEndianBytesToShort(src);
        this.bytesValue = ByteConvert.shortToBytes(this.value);
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setByByteArray this.value = ");
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
        return ByteConvert.shortToBytes(this.value);
    }
}
