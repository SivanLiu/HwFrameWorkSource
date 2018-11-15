package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DSSwitchRecord extends AManagedObject {
    public static final Creator<DSSwitchRecord> CREATOR = new Creator<DSSwitchRecord>() {
        public DSSwitchRecord createFromParcel(Parcel in) {
            return new DSSwitchRecord(in);
        }

        public DSSwitchRecord[] newArray(int size) {
            return new DSSwitchRecord[size];
        }
    };
    private Integer id;
    private String packageName;
    private String reserved1;
    private String reserved2;
    private String reserved3;
    private String reserved4;
    private String switchName;
    private String switchStatus;
    private Long timeStamp;

    public DSSwitchRecord(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        if (!cursor.isNull(2)) {
            l = Long.valueOf(cursor.getLong(2));
        }
        this.timeStamp = l;
        this.switchStatus = cursor.getString(3);
        this.switchName = cursor.getString(4);
        this.packageName = cursor.getString(5);
        this.reserved1 = cursor.getString(6);
        this.reserved2 = cursor.getString(7);
        this.reserved3 = cursor.getString(8);
        this.reserved4 = cursor.getString(9);
    }

    public DSSwitchRecord(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.timeStamp = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.switchStatus = in.readByte() == (byte) 0 ? null : in.readString();
        this.switchName = in.readByte() == (byte) 0 ? null : in.readString();
        this.packageName = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved2 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved3 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserved4 = str;
    }

    private DSSwitchRecord(Integer id, Long timeStamp, String switchStatus, String switchName, String packageName, String reserved1, String reserved2, String reserved3, String reserved4) {
        this.id = id;
        this.timeStamp = timeStamp;
        this.switchStatus = switchStatus;
        this.switchName = switchName;
        this.packageName = packageName;
        this.reserved1 = reserved1;
        this.reserved2 = reserved2;
        this.reserved3 = reserved3;
        this.reserved4 = reserved4;
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

    public Long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
        setValue();
    }

    public String getSwitchStatus() {
        return this.switchStatus;
    }

    public void setSwitchStatus(String switchStatus) {
        this.switchStatus = switchStatus;
        setValue();
    }

    public String getSwitchName() {
        return this.switchName;
    }

    public void setSwitchName(String switchName) {
        this.switchName = switchName;
        setValue();
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
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

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.timeStamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.timeStamp.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.switchStatus != null) {
            out.writeByte((byte) 1);
            out.writeString(this.switchStatus);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.switchName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.switchName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.packageName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.packageName);
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
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<DSSwitchRecord> getHelper() {
        return DSSwitchRecordHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.DSSwitchRecord";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DSSwitchRecord { id: ").append(this.id);
        sb.append(", timeStamp: ").append(this.timeStamp);
        sb.append(", switchStatus: ").append(this.switchStatus);
        sb.append(", switchName: ").append(this.switchName);
        sb.append(", packageName: ").append(this.packageName);
        sb.append(", reserved1: ").append(this.reserved1);
        sb.append(", reserved2: ").append(this.reserved2);
        sb.append(", reserved3: ").append(this.reserved3);
        sb.append(", reserved4: ").append(this.reserved4);
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
