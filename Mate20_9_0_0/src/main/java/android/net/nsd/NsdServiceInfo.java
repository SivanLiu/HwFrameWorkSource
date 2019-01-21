package android.net.nsd;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

public final class NsdServiceInfo implements Parcelable {
    public static final Creator<NsdServiceInfo> CREATOR = new Creator<NsdServiceInfo>() {
        public NsdServiceInfo createFromParcel(Parcel in) {
            NsdServiceInfo info = new NsdServiceInfo();
            info.mServiceName = in.readString();
            info.mServiceType = in.readString();
            if (in.readInt() == 1) {
                try {
                    info.mHost = InetAddress.getByAddress(in.createByteArray());
                } catch (UnknownHostException e) {
                }
            }
            info.mPort = in.readInt();
            int recordCount = in.readInt();
            for (int i = 0; i < recordCount; i++) {
                byte[] valueArray = null;
                if (in.readInt() == 1) {
                    valueArray = new byte[in.readInt()];
                    in.readByteArray(valueArray);
                }
                info.mTxtRecord.put(in.readString(), valueArray);
            }
            return info;
        }

        public NsdServiceInfo[] newArray(int size) {
            return new NsdServiceInfo[size];
        }
    };
    private static final String TAG = "NsdServiceInfo";
    private InetAddress mHost;
    private int mPort;
    private String mServiceName;
    private String mServiceType;
    private final ArrayMap<String, byte[]> mTxtRecord = new ArrayMap();

    public NsdServiceInfo(String sn, String rt) {
        this.mServiceName = sn;
        this.mServiceType = rt;
    }

    public String getServiceName() {
        return this.mServiceName;
    }

    public void setServiceName(String s) {
        this.mServiceName = s;
    }

    public String getServiceType() {
        return this.mServiceType;
    }

    public void setServiceType(String s) {
        this.mServiceType = s;
    }

    public InetAddress getHost() {
        return this.mHost;
    }

    public void setHost(InetAddress s) {
        this.mHost = s;
    }

    public int getPort() {
        return this.mPort;
    }

    public void setPort(int p) {
        this.mPort = p;
    }

    public void setTxtRecords(String rawRecords) {
        int pos = 0;
        byte[] txtRecordsRawBytes = Base64.decode(rawRecords, 0);
        while (pos < txtRecordsRawBytes.length) {
            int recordLen = txtRecordsRawBytes[pos] & 255;
            pos++;
            if (recordLen != 0) {
                String key;
                try {
                    if (pos + recordLen > txtRecordsRawBytes.length) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Corrupt record length (pos = ");
                        stringBuilder.append(pos);
                        stringBuilder.append("): ");
                        stringBuilder.append(recordLen);
                        Log.w(str, stringBuilder.toString());
                        recordLen = txtRecordsRawBytes.length - pos;
                    }
                    int valueLen = 0;
                    byte[] value = null;
                    key = null;
                    for (int i = pos; i < pos + recordLen; i++) {
                        if (key != null) {
                            if (value == null) {
                                value = new byte[((recordLen - key.length()) - 1)];
                            }
                            value[valueLen] = txtRecordsRawBytes[i];
                            valueLen++;
                        } else if (txtRecordsRawBytes[i] == (byte) 61) {
                            key = new String(txtRecordsRawBytes, pos, i - pos, StandardCharsets.US_ASCII);
                        }
                    }
                    if (key == null) {
                        key = new String(txtRecordsRawBytes, pos, recordLen, StandardCharsets.US_ASCII);
                    }
                    if (TextUtils.isEmpty(key)) {
                        throw new IllegalArgumentException("Invalid txt record (key is empty)");
                    } else if (getAttributes().containsKey(key)) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Invalid txt record (duplicate key \"");
                        stringBuilder2.append(key);
                        stringBuilder2.append("\")");
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    } else {
                        setAttribute(key, value);
                        pos += recordLen;
                    }
                } catch (IllegalArgumentException e) {
                    key = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("While parsing txt records (pos = ");
                    stringBuilder3.append(pos);
                    stringBuilder3.append("): ");
                    stringBuilder3.append(e.getMessage());
                    Log.e(key, stringBuilder3.toString());
                }
            } else {
                throw new IllegalArgumentException("Zero sized txt record");
            }
        }
    }

    public void setAttribute(String key, byte[] value) {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        int i = 0;
        int i2 = 0;
        while (i2 < key.length()) {
            char character = key.charAt(i2);
            if (character < ' ' || character > '~') {
                throw new IllegalArgumentException("Key strings must be printable US-ASCII");
            } else if (character != '=') {
                i2++;
            } else {
                throw new IllegalArgumentException("Key strings must not include '='");
            }
        }
        if (key.length() + (value == null ? 0 : value.length) < 255) {
            if (key.length() > 9) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Key lengths > 9 are discouraged: ");
                stringBuilder.append(key);
                Log.w(str, stringBuilder.toString());
            }
            int length = key.length() + getTxtRecordSize();
            if (value != null) {
                i = value.length;
            }
            length = (length + i) + 2;
            if (length <= 1300) {
                if (length > 400) {
                    Log.w(TAG, "Total length of all attributes exceeds 400 bytes; truncation may occur");
                }
                this.mTxtRecord.put(key, value);
                return;
            }
            throw new IllegalArgumentException("Total length of attributes must be < 1300 bytes");
        }
        throw new IllegalArgumentException("Key length + value length must be < 255 bytes");
    }

    public void setAttribute(String key, String value) {
        byte[] e;
        if (value == null) {
            try {
                e = (byte[]) null;
            } catch (UnsupportedEncodingException e2) {
                throw new IllegalArgumentException("Value must be UTF-8");
            }
        }
        e = value.getBytes("UTF-8");
        setAttribute(key, e);
    }

    public void removeAttribute(String key) {
        this.mTxtRecord.remove(key);
    }

    public Map<String, byte[]> getAttributes() {
        return Collections.unmodifiableMap(this.mTxtRecord);
    }

    private int getTxtRecordSize() {
        int txtRecordSize = 0;
        for (Entry<String, byte[]> entry : this.mTxtRecord.entrySet()) {
            byte[] value = (byte[]) entry.getValue();
            txtRecordSize = ((txtRecordSize + 2) + ((String) entry.getKey()).length()) + (value == null ? 0 : value.length);
        }
        return txtRecordSize;
    }

    public byte[] getTxtRecord() {
        int txtRecordSize = getTxtRecordSize();
        if (txtRecordSize == 0) {
            return new byte[0];
        }
        byte[] txtRecord = new byte[txtRecordSize];
        int ptr = 0;
        for (Entry<String, byte[]> entry : this.mTxtRecord.entrySet()) {
            String key = (String) entry.getKey();
            byte[] value = (byte[]) entry.getValue();
            int ptr2 = ptr + 1;
            txtRecord[ptr] = (byte) ((key.length() + (value == null ? 0 : value.length)) + 1);
            System.arraycopy(key.getBytes(StandardCharsets.US_ASCII), 0, txtRecord, ptr2, key.length());
            ptr2 += key.length();
            ptr = ptr2 + 1;
            txtRecord[ptr2] = (byte) 61;
            if (value != null) {
                System.arraycopy(value, 0, txtRecord, ptr, value.length);
                ptr += value.length;
            }
        }
        return txtRecord;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("name: ");
        sb.append(this.mServiceName);
        sb.append(", type: ");
        sb.append(this.mServiceType);
        sb.append(", host: ");
        sb.append(this.mHost);
        sb.append(", port: ");
        sb.append(this.mPort);
        byte[] txtRecord = getTxtRecord();
        if (txtRecord != null) {
            sb.append(", txtRecord: ");
            sb.append(new String(txtRecord, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mServiceName);
        dest.writeString(this.mServiceType);
        if (this.mHost != null) {
            dest.writeInt(1);
            dest.writeByteArray(this.mHost.getAddress());
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.mPort);
        dest.writeInt(this.mTxtRecord.size());
        for (String key : this.mTxtRecord.keySet()) {
            byte[] value = (byte[]) this.mTxtRecord.get(key);
            if (value != null) {
                dest.writeInt(1);
                dest.writeInt(value.length);
                dest.writeByteArray(value);
            } else {
                dest.writeInt(0);
            }
            dest.writeString(key);
        }
    }
}
