package com.huawei.device.connectivitychrlog;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Cenum {
    private final String LOG_TAG;
    int length;
    Map<String, Integer> map = new LinkedHashMap();
    String name;

    public Cenum() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cenum");
        stringBuilder.append(getClass().getSimpleName());
        this.LOG_TAG = stringBuilder.toString();
    }

    public int getOrdinal() {
        if (this.map.get(this.name) != null) {
            return ((Integer) this.map.get(this.name)).intValue();
        }
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getOrdinal failed name is not in the enum map, name = ");
        stringBuilder.append(this.name);
        ChrLog.chrLogE(str, stringBuilder.toString());
        return -1;
    }

    public void setValue(String name) {
        this.name = name;
    }

    public void setByByteArray(byte[] src, int len, boolean bIsLittleEndian) {
        String str;
        StringBuilder stringBuilder;
        int data;
        if (this.length != len) {
            str = this.LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setByByteArray failed ,not support len = ");
            stringBuilder.append(len);
            ChrLog.chrLogE(str, stringBuilder.toString());
        }
        if (this.length == 1) {
            data = src[0];
        } else if (this.length == 2) {
            data = ByteConvert.littleEndianBytesToShort(src);
        } else if (this.length == 4) {
            data = ByteConvert.littleEndianBytesToInt(src);
        } else {
            str = this.LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setByByteArray failed ,not support length = ");
            stringBuilder.append(this.length);
            ChrLog.chrLogE(str, stringBuilder.toString());
            return;
        }
        for (Entry<String, Integer> m : this.map.entrySet()) {
            if (data == ((Integer) m.getValue()).intValue()) {
                this.name = (String) m.getKey();
            }
        }
        str = this.LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setByByteArray data = ");
        stringBuilder.append(data);
        stringBuilder.append(", name = ");
        stringBuilder.append(this.name);
        ChrLog.chrLogI(str, stringBuilder.toString());
    }

    public String getValue() {
        return this.name;
    }

    void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return this.length;
    }

    public byte[] toByteArray() {
        ByteBuffer bytebuf = ByteBuffer.wrap(new byte[this.length]);
        if (this.length == 1) {
            bytebuf.put((byte) getOrdinal());
        } else if (this.length == 2) {
            bytebuf.put(ByteConvert.shortToBytes((short) getOrdinal()));
        } else {
            bytebuf.put(ByteConvert.intToBytes(getOrdinal()));
        }
        return bytebuf.array();
    }
}
