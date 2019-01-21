package com.huawei.device.connectivitychrlog;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class LogString {
    static String CHARSET = "UTF-8";
    static String EMPTY_STRING = "";
    private static final String LOG_TAG = "LogString";
    private int length;
    private String value;

    private String getEmptyString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(EMPTY_STRING);
        }
        return sb.toString();
    }

    public int getLength() {
        return this.length;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        if (value == null) {
            this.value = getEmptyString(this.length);
        } else {
            this.value = value;
        }
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
        try {
            this.value = new String(src, CHARSET);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setByByteArray value = ");
            stringBuilder.append(this.value);
            ChrLog.chrLogI(str, stringBuilder.toString());
        } catch (UnsupportedEncodingException e) {
            ChrLog.chrLogE(LOG_TAG, "setByByteArray UnsupportedEncodingException");
        }
    }

    public LogString(int length) {
        this.length = length;
        this.value = getEmptyString(length);
    }

    public String toString() {
        return this.value.toString();
    }

    public byte[] toByteArray() {
        try {
            ByteBuffer bytebuf = ByteBuffer.wrap(new byte[this.length]);
            byte[] subValueBytes = this.value.getBytes(CHARSET);
            if (subValueBytes.length > this.length) {
                bytebuf.put(subValueBytes, 0, this.length);
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("toByteArray length error, subValueBytes.length = ");
                stringBuilder.append(subValueBytes.length);
                stringBuilder.append(", length = ");
                stringBuilder.append(this.length);
                ChrLog.chrLogE(str, stringBuilder.toString());
            } else {
                bytebuf.put(subValueBytes);
            }
            return bytebuf.array();
        } catch (UnsupportedEncodingException e) {
            return new byte[this.length];
        }
    }
}
