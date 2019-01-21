package android.net.nsd;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class DnsSdTxtRecord implements Parcelable {
    public static final Creator<DnsSdTxtRecord> CREATOR = new Creator<DnsSdTxtRecord>() {
        public DnsSdTxtRecord createFromParcel(Parcel in) {
            DnsSdTxtRecord info = new DnsSdTxtRecord();
            in.readByteArray(info.mData);
            return info;
        }

        public DnsSdTxtRecord[] newArray(int size) {
            return new DnsSdTxtRecord[size];
        }
    };
    private static final byte mSeperator = (byte) 61;
    private byte[] mData;

    public DnsSdTxtRecord() {
        this.mData = new byte[0];
    }

    public DnsSdTxtRecord(byte[] data) {
        this.mData = (byte[]) data.clone();
    }

    public DnsSdTxtRecord(DnsSdTxtRecord src) {
        if (src != null && src.mData != null) {
            this.mData = (byte[]) src.mData.clone();
        }
    }

    public void set(String key, String value) {
        byte[] valBytes;
        int valLen;
        int i = 0;
        if (value != null) {
            valBytes = value.getBytes();
            valLen = valBytes.length;
        } else {
            valBytes = null;
            valLen = 0;
        }
        try {
            byte[] keyBytes = key.getBytes("US-ASCII");
            while (i < keyBytes.length) {
                if (keyBytes[i] != mSeperator) {
                    i++;
                } else {
                    throw new IllegalArgumentException("= is not a valid character in key");
                }
            }
            if (keyBytes.length + valLen < 255) {
                i = remove(key);
                if (i == -1) {
                    i = keyCount();
                }
                insert(keyBytes, valBytes, i);
                return;
            }
            throw new IllegalArgumentException("Key and Value length cannot exceed 255 bytes");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("key should be US-ASCII");
        }
    }

    public String get(String key) {
        byte[] val = getValue(key);
        return val != null ? new String(val) : null;
    }

    public int remove(String key) {
        int avStart = 0;
        int i = 0;
        while (avStart < this.mData.length) {
            int avLen = this.mData[avStart];
            if (key.length() > avLen || !((key.length() == avLen || this.mData[(key.length() + avStart) + 1] == mSeperator) && key.compareToIgnoreCase(new String(this.mData, avStart + 1, key.length())) == 0)) {
                avStart += 255 & (avLen + 1);
                i++;
            } else {
                byte[] oldBytes = this.mData;
                this.mData = new byte[((oldBytes.length - avLen) - 1)];
                System.arraycopy(oldBytes, 0, this.mData, 0, avStart);
                System.arraycopy(oldBytes, (avStart + avLen) + 1, this.mData, avStart, ((oldBytes.length - avStart) - avLen) - 1);
                return i;
            }
        }
        return -1;
    }

    public int keyCount() {
        int count = 0;
        int nextKey = 0;
        while (nextKey < this.mData.length) {
            nextKey += 255 & (this.mData[nextKey] + 1);
            count++;
        }
        return count;
    }

    public boolean contains(String key) {
        String s = null;
        int i = 0;
        while (true) {
            String key2 = getKey(i);
            s = key2;
            if (key2 == null) {
                return false;
            }
            if (key.compareToIgnoreCase(s) == 0) {
                return true;
            }
            i++;
        }
    }

    public int size() {
        return this.mData.length;
    }

    public byte[] getRawData() {
        return (byte[]) this.mData.clone();
    }

    private void insert(byte[] keyBytes, byte[] value, int index) {
        int i;
        byte[] oldBytes = this.mData;
        int valLen = value != null ? value.length : 0;
        int insertion = 0;
        for (i = 0; i < index && insertion < this.mData.length; i++) {
            insertion += 255 & (this.mData[insertion] + 1);
        }
        i = (keyBytes.length + valLen) + (value != null ? 1 : 0);
        int newLen = (oldBytes.length + i) + 1;
        this.mData = new byte[newLen];
        System.arraycopy(oldBytes, 0, this.mData, 0, insertion);
        int secondHalfLen = oldBytes.length - insertion;
        System.arraycopy(oldBytes, insertion, this.mData, newLen - secondHalfLen, secondHalfLen);
        this.mData[insertion] = (byte) i;
        System.arraycopy(keyBytes, 0, this.mData, insertion + 1, keyBytes.length);
        if (value != null) {
            this.mData[(insertion + 1) + keyBytes.length] = mSeperator;
            System.arraycopy(value, 0, this.mData, (keyBytes.length + insertion) + 2, valLen);
        }
    }

    private String getKey(int index) {
        int i;
        int avStart = 0;
        for (i = 0; i < index && avStart < this.mData.length; i++) {
            avStart += this.mData[avStart] + 1;
        }
        if (avStart >= this.mData.length) {
            return null;
        }
        i = this.mData[avStart];
        int aLen = 0;
        while (aLen < i && this.mData[(avStart + aLen) + 1] != mSeperator) {
            aLen++;
        }
        return new String(this.mData, avStart + 1, aLen);
    }

    private byte[] getValue(int index) {
        int i;
        int avStart = 0;
        for (i = 0; i < index && avStart < this.mData.length; i++) {
            avStart += this.mData[avStart] + 1;
        }
        if (avStart >= this.mData.length) {
            return null;
        }
        i = this.mData[avStart];
        for (int aLen = 0; aLen < i; aLen++) {
            if (this.mData[(avStart + aLen) + 1] == mSeperator) {
                byte[] value = new byte[((i - aLen) - 1)];
                System.arraycopy(this.mData, (avStart + aLen) + 2, value, 0, (i - aLen) - 1);
                return value;
            }
        }
        return null;
    }

    private String getValueAsString(int index) {
        byte[] value = getValue(index);
        return value != null ? new String(value) : null;
    }

    private byte[] getValue(String forKey) {
        int i = 0;
        while (true) {
            String key = getKey(i);
            String s = key;
            if (key == null) {
                return null;
            }
            if (forKey.compareToIgnoreCase(s) == 0) {
                return getValue(i);
            }
            i++;
        }
    }

    public String toString() {
        String result = null;
        int i = 0;
        while (true) {
            String key = getKey(i);
            String a = key;
            if (key == null) {
                break;
            }
            StringBuilder stringBuilder;
            key = new StringBuilder();
            key.append("{");
            key.append(a);
            key = key.toString();
            String val = getValueAsString(i);
            if (val != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(key);
                stringBuilder.append("=");
                stringBuilder.append(val);
                stringBuilder.append("}");
                key = stringBuilder.toString();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(key);
                stringBuilder.append("}");
                key = stringBuilder.toString();
            }
            if (result == null) {
                result = key;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(result);
                stringBuilder.append(", ");
                stringBuilder.append(key);
                result = stringBuilder.toString();
            }
            i++;
        }
        return result != null ? result : "";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof DnsSdTxtRecord) {
            return Arrays.equals(((DnsSdTxtRecord) o).mData, this.mData);
        }
        return false;
    }

    public int hashCode() {
        return Arrays.hashCode(this.mData);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.mData);
    }
}
