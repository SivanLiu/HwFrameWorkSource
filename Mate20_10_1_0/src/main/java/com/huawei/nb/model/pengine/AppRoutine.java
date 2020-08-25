package com.huawei.nb.model.pengine;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class AppRoutine extends AManagedObject {
    public static final Parcelable.Creator<AppRoutine> CREATOR = new Parcelable.Creator<AppRoutine>() {
        /* class com.huawei.nb.model.pengine.AppRoutine.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public AppRoutine createFromParcel(Parcel in) {
            return new AppRoutine(in);
        }

        @Override // android.os.Parcelable.Creator
        public AppRoutine[] newArray(int size) {
            return new AppRoutine[size];
        }
    };
    private String column0;
    private String column1;
    private String column2;
    private String column3;
    private String column4;
    private String endTime;
    private Integer id;
    private String packageName;
    private String poi;
    private String startTime;
    private String support;
    private String timestamp;

    public AppRoutine(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.timestamp = cursor.getString(2);
        this.startTime = cursor.getString(3);
        this.endTime = cursor.getString(4);
        this.poi = cursor.getString(5);
        this.packageName = cursor.getString(6);
        this.support = cursor.getString(7);
        this.column0 = cursor.getString(8);
        this.column1 = cursor.getString(9);
        this.column2 = cursor.getString(10);
        this.column3 = cursor.getString(11);
        this.column4 = cursor.getString(12);
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public AppRoutine(Parcel in) {
        super(in);
        String str = null;
        if (in.readByte() == 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.timestamp = in.readByte() == 0 ? null : in.readString();
        this.startTime = in.readByte() == 0 ? null : in.readString();
        this.endTime = in.readByte() == 0 ? null : in.readString();
        this.poi = in.readByte() == 0 ? null : in.readString();
        this.packageName = in.readByte() == 0 ? null : in.readString();
        this.support = in.readByte() == 0 ? null : in.readString();
        this.column0 = in.readByte() == 0 ? null : in.readString();
        this.column1 = in.readByte() == 0 ? null : in.readString();
        this.column2 = in.readByte() == 0 ? null : in.readString();
        this.column3 = in.readByte() == 0 ? null : in.readString();
        this.column4 = in.readByte() != 0 ? in.readString() : str;
    }

    private AppRoutine(Integer id2, String timestamp2, String startTime2, String endTime2, String poi2, String packageName2, String support2, String column02, String column12, String column22, String column32, String column42) {
        this.id = id2;
        this.timestamp = timestamp2;
        this.startTime = startTime2;
        this.endTime = endTime2;
        this.poi = poi2;
        this.packageName = packageName2;
        this.support = support2;
        this.column0 = column02;
        this.column1 = column12;
        this.column2 = column22;
        this.column3 = column32;
        this.column4 = column42;
    }

    public AppRoutine() {
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int describeContents() {
        return 0;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id2) {
        this.id = id2;
        setValue();
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(String timestamp2) {
        this.timestamp = timestamp2;
        setValue();
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String startTime2) {
        this.startTime = startTime2;
        setValue();
    }

    public String getEndTime() {
        return this.endTime;
    }

    public void setEndTime(String endTime2) {
        this.endTime = endTime2;
        setValue();
    }

    public String getPoi() {
        return this.poi;
    }

    public void setPoi(String poi2) {
        this.poi = poi2;
        setValue();
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName2) {
        this.packageName = packageName2;
        setValue();
    }

    public String getSupport() {
        return this.support;
    }

    public void setSupport(String support2) {
        this.support = support2;
        setValue();
    }

    public String getColumn0() {
        return this.column0;
    }

    public void setColumn0(String column02) {
        this.column0 = column02;
        setValue();
    }

    public String getColumn1() {
        return this.column1;
    }

    public void setColumn1(String column12) {
        this.column1 = column12;
        setValue();
    }

    public String getColumn2() {
        return this.column2;
    }

    public void setColumn2(String column22) {
        this.column2 = column22;
        setValue();
    }

    public String getColumn3() {
        return this.column3;
    }

    public void setColumn3(String column32) {
        this.column3 = column32;
        setValue();
    }

    public String getColumn4() {
        return this.column4;
    }

    public void setColumn4(String column42) {
        this.column4 = column42;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
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
            out.writeString(this.timestamp);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.startTime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.startTime);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.endTime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.endTime);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.poi != null) {
            out.writeByte((byte) 1);
            out.writeString(this.poi);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.packageName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.packageName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.support != null) {
            out.writeByte((byte) 1);
            out.writeString(this.support);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column0 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column0);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column2);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column3 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column3);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column4 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column4);
            return;
        }
        out.writeByte((byte) 0);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<AppRoutine> getHelper() {
        return AppRoutineHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.pengine.AppRoutine";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getDatabaseName() {
        return "dsPengineData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AppRoutine { id: ").append(this.id);
        sb.append(", timestamp: ").append(this.timestamp);
        sb.append(", startTime: ").append(this.startTime);
        sb.append(", endTime: ").append(this.endTime);
        sb.append(", poi: ").append(this.poi);
        sb.append(", packageName: ").append(this.packageName);
        sb.append(", support: ").append(this.support);
        sb.append(", column0: ").append(this.column0);
        sb.append(", column1: ").append(this.column1);
        sb.append(", column2: ").append(this.column2);
        sb.append(", column3: ").append(this.column3);
        sb.append(", column4: ").append(this.column4);
        sb.append(" }");
        return sb.toString();
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.11";
    }

    public int getDatabaseVersionCode() {
        return 11;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
