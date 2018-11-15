package com.huawei.nb.model.meta;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class GuestInfo extends AManagedObject {
    public static final Creator<GuestInfo> CREATOR = new Creator<GuestInfo>() {
        public GuestInfo createFromParcel(Parcel in) {
            return new GuestInfo(in);
        }

        public GuestInfo[] newArray(int size) {
            return new GuestInfo[size];
        }
    };
    private Integer id;
    private String pkgName;

    public GuestInfo(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.pkgName = cursor.getString(2);
    }

    public GuestInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.pkgName = str;
    }

    private GuestInfo(Integer id, String pkgName) {
        this.id = id;
        this.pkgName = pkgName;
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

    public String getPkgName() {
        return this.pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
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
        if (this.pkgName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.pkgName);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<GuestInfo> getHelper() {
        return GuestInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.meta.GuestInfo";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("GuestInfo { id: ").append(this.id);
        sb.append(", pkgName: ").append(this.pkgName);
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
        return "0.0.13";
    }

    public int getDatabaseVersionCode() {
        return 13;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
