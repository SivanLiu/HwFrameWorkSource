package com.huawei.nb.model.pengine;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class PengineSwitch extends AManagedObject {
    public static final Creator<PengineSwitch> CREATOR = new Creator<PengineSwitch>() {
        public PengineSwitch createFromParcel(Parcel in) {
            return new PengineSwitch(in);
        }

        public PengineSwitch[] newArray(int size) {
            return new PengineSwitch[size];
        }
    };
    private String column0;
    private String column1;
    private String column2;
    private String column3;
    private String column4;
    private String column5;
    private String column6;
    private String column7;
    private Integer dataType;
    private Integer id;
    private Long timestamp;
    private String upProperty;

    public PengineSwitch(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.dataType = cursor.isNull(2) ? null : Integer.valueOf(cursor.getInt(2));
        if (!cursor.isNull(3)) {
            l = Long.valueOf(cursor.getLong(3));
        }
        this.timestamp = l;
        this.upProperty = cursor.getString(4);
        this.column0 = cursor.getString(5);
        this.column1 = cursor.getString(6);
        this.column2 = cursor.getString(7);
        this.column3 = cursor.getString(8);
        this.column4 = cursor.getString(9);
        this.column5 = cursor.getString(10);
        this.column6 = cursor.getString(11);
        this.column7 = cursor.getString(12);
    }

    public PengineSwitch(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.dataType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.timestamp = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.upProperty = in.readByte() == (byte) 0 ? null : in.readString();
        this.column0 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column2 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column3 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column4 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column5 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column6 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.column7 = str;
    }

    private PengineSwitch(Integer id, Integer dataType, Long timestamp, String upProperty, String column0, String column1, String column2, String column3, String column4, String column5, String column6, String column7) {
        this.id = id;
        this.dataType = dataType;
        this.timestamp = timestamp;
        this.upProperty = upProperty;
        this.column0 = column0;
        this.column1 = column1;
        this.column2 = column2;
        this.column3 = column3;
        this.column4 = column4;
        this.column5 = column5;
        this.column6 = column6;
        this.column7 = column7;
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

    public Integer getDataType() {
        return this.dataType;
    }

    public void setDataType(Integer dataType) {
        this.dataType = dataType;
        setValue();
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        setValue();
    }

    public String getUpProperty() {
        return this.upProperty;
    }

    public void setUpProperty(String upProperty) {
        this.upProperty = upProperty;
        setValue();
    }

    public String getColumn0() {
        return this.column0;
    }

    public void setColumn0(String column0) {
        this.column0 = column0;
        setValue();
    }

    public String getColumn1() {
        return this.column1;
    }

    public void setColumn1(String column1) {
        this.column1 = column1;
        setValue();
    }

    public String getColumn2() {
        return this.column2;
    }

    public void setColumn2(String column2) {
        this.column2 = column2;
        setValue();
    }

    public String getColumn3() {
        return this.column3;
    }

    public void setColumn3(String column3) {
        this.column3 = column3;
        setValue();
    }

    public String getColumn4() {
        return this.column4;
    }

    public void setColumn4(String column4) {
        this.column4 = column4;
        setValue();
    }

    public String getColumn5() {
        return this.column5;
    }

    public void setColumn5(String column5) {
        this.column5 = column5;
        setValue();
    }

    public String getColumn6() {
        return this.column6;
    }

    public void setColumn6(String column6) {
        this.column6 = column6;
        setValue();
    }

    public String getColumn7() {
        return this.column7;
    }

    public void setColumn7(String column7) {
        this.column7 = column7;
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
        if (this.dataType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.dataType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.timestamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.timestamp.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.upProperty != null) {
            out.writeByte((byte) 1);
            out.writeString(this.upProperty);
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
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column5 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column5);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column6 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column6);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column7 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column7);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<PengineSwitch> getHelper() {
        return PengineSwitchHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.pengine.PengineSwitch";
    }

    public String getDatabaseName() {
        return "dsPengineData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("PengineSwitch { id: ").append(this.id);
        sb.append(", dataType: ").append(this.dataType);
        sb.append(", timestamp: ").append(this.timestamp);
        sb.append(", upProperty: ").append(this.upProperty);
        sb.append(", column0: ").append(this.column0);
        sb.append(", column1: ").append(this.column1);
        sb.append(", column2: ").append(this.column2);
        sb.append(", column3: ").append(this.column3);
        sb.append(", column4: ").append(this.column4);
        sb.append(", column5: ").append(this.column5);
        sb.append(", column6: ").append(this.column6);
        sb.append(", column7: ").append(this.column7);
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
        return "0.0.7";
    }

    public int getDatabaseVersionCode() {
        return 7;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
