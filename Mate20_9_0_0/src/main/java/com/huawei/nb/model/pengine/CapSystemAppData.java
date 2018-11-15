package com.huawei.nb.model.pengine;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class CapSystemAppData extends AManagedObject {
    public static final Creator<CapSystemAppData> CREATOR = new Creator<CapSystemAppData>() {
        public CapSystemAppData createFromParcel(Parcel in) {
            return new CapSystemAppData(in);
        }

        public CapSystemAppData[] newArray(int size) {
            return new CapSystemAppData[size];
        }
    };
    private String column0;
    private String column1;
    private String column10;
    private String column11;
    private String column12;
    private String column13;
    private String column14;
    private String column15;
    private String column16;
    private String column17;
    private String column18;
    private String column19;
    private String column2;
    private String column3;
    private String column4;
    private String column5;
    private String column6;
    private String column7;
    private String column8;
    private String column9;
    private Integer dataType;
    private Integer id;
    private Long timestamp;

    public CapSystemAppData(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.dataType = cursor.isNull(2) ? null : Integer.valueOf(cursor.getInt(2));
        if (!cursor.isNull(3)) {
            l = Long.valueOf(cursor.getLong(3));
        }
        this.timestamp = l;
        this.column0 = cursor.getString(4);
        this.column1 = cursor.getString(5);
        this.column2 = cursor.getString(6);
        this.column3 = cursor.getString(7);
        this.column4 = cursor.getString(8);
        this.column5 = cursor.getString(9);
        this.column6 = cursor.getString(10);
        this.column7 = cursor.getString(11);
        this.column8 = cursor.getString(12);
        this.column9 = cursor.getString(13);
        this.column10 = cursor.getString(14);
        this.column11 = cursor.getString(15);
        this.column12 = cursor.getString(16);
        this.column13 = cursor.getString(17);
        this.column14 = cursor.getString(18);
        this.column15 = cursor.getString(19);
        this.column16 = cursor.getString(20);
        this.column17 = cursor.getString(21);
        this.column18 = cursor.getString(22);
        this.column19 = cursor.getString(23);
    }

    public CapSystemAppData(Parcel in) {
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
        this.column0 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column2 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column3 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column4 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column5 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column6 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column7 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column8 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column9 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column10 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column11 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column12 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column13 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column14 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column15 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column16 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column17 = in.readByte() == (byte) 0 ? null : in.readString();
        this.column18 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.column19 = str;
    }

    private CapSystemAppData(Integer id, Integer dataType, Long timestamp, String column0, String column1, String column2, String column3, String column4, String column5, String column6, String column7, String column8, String column9, String column10, String column11, String column12, String column13, String column14, String column15, String column16, String column17, String column18, String column19) {
        this.id = id;
        this.dataType = dataType;
        this.timestamp = timestamp;
        this.column0 = column0;
        this.column1 = column1;
        this.column2 = column2;
        this.column3 = column3;
        this.column4 = column4;
        this.column5 = column5;
        this.column6 = column6;
        this.column7 = column7;
        this.column8 = column8;
        this.column9 = column9;
        this.column10 = column10;
        this.column11 = column11;
        this.column12 = column12;
        this.column13 = column13;
        this.column14 = column14;
        this.column15 = column15;
        this.column16 = column16;
        this.column17 = column17;
        this.column18 = column18;
        this.column19 = column19;
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

    public String getColumn8() {
        return this.column8;
    }

    public void setColumn8(String column8) {
        this.column8 = column8;
        setValue();
    }

    public String getColumn9() {
        return this.column9;
    }

    public void setColumn9(String column9) {
        this.column9 = column9;
        setValue();
    }

    public String getColumn10() {
        return this.column10;
    }

    public void setColumn10(String column10) {
        this.column10 = column10;
        setValue();
    }

    public String getColumn11() {
        return this.column11;
    }

    public void setColumn11(String column11) {
        this.column11 = column11;
        setValue();
    }

    public String getColumn12() {
        return this.column12;
    }

    public void setColumn12(String column12) {
        this.column12 = column12;
        setValue();
    }

    public String getColumn13() {
        return this.column13;
    }

    public void setColumn13(String column13) {
        this.column13 = column13;
        setValue();
    }

    public String getColumn14() {
        return this.column14;
    }

    public void setColumn14(String column14) {
        this.column14 = column14;
        setValue();
    }

    public String getColumn15() {
        return this.column15;
    }

    public void setColumn15(String column15) {
        this.column15 = column15;
        setValue();
    }

    public String getColumn16() {
        return this.column16;
    }

    public void setColumn16(String column16) {
        this.column16 = column16;
        setValue();
    }

    public String getColumn17() {
        return this.column17;
    }

    public void setColumn17(String column17) {
        this.column17 = column17;
        setValue();
    }

    public String getColumn18() {
        return this.column18;
    }

    public void setColumn18(String column18) {
        this.column18 = column18;
        setValue();
    }

    public String getColumn19() {
        return this.column19;
    }

    public void setColumn19(String column19) {
        this.column19 = column19;
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
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column8 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column8);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column9 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column9);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column10 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column10);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column11 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column11);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column12 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column12);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column13 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column13);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column14 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column14);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column15 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column15);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column16 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column16);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column17 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column17);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column18 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column18);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.column19 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.column19);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<CapSystemAppData> getHelper() {
        return CapSystemAppDataHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.pengine.CapSystemAppData";
    }

    public String getDatabaseName() {
        return "dsPengineData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CapSystemAppData { id: ").append(this.id);
        sb.append(", dataType: ").append(this.dataType);
        sb.append(", timestamp: ").append(this.timestamp);
        sb.append(", column0: ").append(this.column0);
        sb.append(", column1: ").append(this.column1);
        sb.append(", column2: ").append(this.column2);
        sb.append(", column3: ").append(this.column3);
        sb.append(", column4: ").append(this.column4);
        sb.append(", column5: ").append(this.column5);
        sb.append(", column6: ").append(this.column6);
        sb.append(", column7: ").append(this.column7);
        sb.append(", column8: ").append(this.column8);
        sb.append(", column9: ").append(this.column9);
        sb.append(", column10: ").append(this.column10);
        sb.append(", column11: ").append(this.column11);
        sb.append(", column12: ").append(this.column12);
        sb.append(", column13: ").append(this.column13);
        sb.append(", column14: ").append(this.column14);
        sb.append(", column15: ").append(this.column15);
        sb.append(", column16: ").append(this.column16);
        sb.append(", column17: ").append(this.column17);
        sb.append(", column18: ").append(this.column18);
        sb.append(", column19: ").append(this.column19);
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
