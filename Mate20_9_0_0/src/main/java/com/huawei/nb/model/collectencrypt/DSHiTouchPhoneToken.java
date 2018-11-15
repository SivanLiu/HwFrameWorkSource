package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DSHiTouchPhoneToken extends AManagedObject {
    public static final Creator<DSHiTouchPhoneToken> CREATOR = new Creator<DSHiTouchPhoneToken>() {
        public DSHiTouchPhoneToken createFromParcel(Parcel in) {
            return new DSHiTouchPhoneToken(in);
        }

        public DSHiTouchPhoneToken[] newArray(int size) {
            return new DSHiTouchPhoneToken[size];
        }
    };
    private Integer compat = Integer.valueOf(0);
    private Integer id;
    private String reserved0;
    private String reserved1;
    private String reserved2;
    private String reserved3;
    private String reserved4;
    private String reserved5;
    private Long timestamp = Long.valueOf(0);
    private String tokencodes;

    public DSHiTouchPhoneToken(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.timestamp = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        if (!cursor.isNull(3)) {
            num = Integer.valueOf(cursor.getInt(3));
        }
        this.compat = num;
        this.tokencodes = cursor.getString(4);
        this.reserved0 = cursor.getString(5);
        this.reserved1 = cursor.getString(6);
        this.reserved2 = cursor.getString(7);
        this.reserved3 = cursor.getString(8);
        this.reserved4 = cursor.getString(9);
        this.reserved5 = cursor.getString(10);
    }

    public DSHiTouchPhoneToken(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.timestamp = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.compat = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.tokencodes = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved0 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved2 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved3 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved4 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserved5 = str;
    }

    private DSHiTouchPhoneToken(Integer id, Long timestamp, Integer compat, String tokencodes, String reserved0, String reserved1, String reserved2, String reserved3, String reserved4, String reserved5) {
        this.id = id;
        this.timestamp = timestamp;
        this.compat = compat;
        this.tokencodes = tokencodes;
        this.reserved0 = reserved0;
        this.reserved1 = reserved1;
        this.reserved2 = reserved2;
        this.reserved3 = reserved3;
        this.reserved4 = reserved4;
        this.reserved5 = reserved5;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
        setValue();
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        setValue();
    }

    public Integer getCompat() {
        return this.compat;
    }

    public void setCompat(Integer compat) {
        this.compat = compat;
        setValue();
    }

    public String getTokencodes() {
        return this.tokencodes;
    }

    public void setTokencodes(String tokencodes) {
        this.tokencodes = tokencodes;
        setValue();
    }

    public String getReserved0() {
        return this.reserved0;
    }

    public void setReserved0(String reserved0) {
        this.reserved0 = reserved0;
        setValue();
    }

    public String getReserved1() {
        return this.reserved1;
    }

    public void setReserved1(String reserved1) {
        this.reserved1 = reserved1;
        setValue();
    }

    public String getReserved2() {
        return this.reserved2;
    }

    public void setReserved2(String reserved2) {
        this.reserved2 = reserved2;
        setValue();
    }

    public String getReserved3() {
        return this.reserved3;
    }

    public void setReserved3(String reserved3) {
        this.reserved3 = reserved3;
        setValue();
    }

    public String getReserved4() {
        return this.reserved4;
    }

    public void setReserved4(String reserved4) {
        this.reserved4 = reserved4;
        setValue();
    }

    public String getReserved5() {
        return this.reserved5;
    }

    public void setReserved5(String reserved5) {
        this.reserved5 = reserved5;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.timestamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.timestamp.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.compat != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.compat.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.tokencodes != null) {
            out.writeByte((byte) 1);
            out.writeString(this.tokencodes);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved0 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved0);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved2);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved3 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved3);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved4 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved4);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved5 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved5);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<DSHiTouchPhoneToken> getHelper() {
        return DSHiTouchPhoneTokenHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.DSHiTouchPhoneToken";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DSHiTouchPhoneToken { id: ").append(this.id);
        sb.append(", timestamp: ").append(this.timestamp);
        sb.append(", compat: ").append(this.compat);
        sb.append(", tokencodes: ").append(this.tokencodes);
        sb.append(", reserved0: ").append(this.reserved0);
        sb.append(", reserved1: ").append(this.reserved1);
        sb.append(", reserved2: ").append(this.reserved2);
        sb.append(", reserved3: ").append(this.reserved3);
        sb.append(", reserved4: ").append(this.reserved4);
        sb.append(", reserved5: ").append(this.reserved5);
        sb.append(" }");
        return sb.toString();
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.10";
    }

    public int getDatabaseVersionCode() {
        return 10;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
