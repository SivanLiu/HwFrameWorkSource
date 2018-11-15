package com.huawei.nb.model.search;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class IndexMapping extends AManagedObject {
    public static final Creator<IndexMapping> CREATOR = new Creator<IndexMapping>() {
        public IndexMapping createFromParcel(Parcel in) {
            return new IndexMapping(in);
        }

        public IndexMapping[] newArray(int size) {
            return new IndexMapping[size];
        }
    };
    private String columnName;
    private String columnNums;
    private String fieldName;
    private Integer id;
    private Integer indexMappingType;
    private Boolean isColumnNum;

    public IndexMapping(Cursor cursor) {
        Boolean bool;
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.fieldName = cursor.getString(2);
        if (cursor.isNull(3)) {
            bool = null;
        } else {
            bool = Boolean.valueOf(cursor.getInt(3) != 0);
        }
        this.isColumnNum = bool;
        this.columnName = cursor.getString(4);
        this.columnNums = cursor.getString(5);
        if (!cursor.isNull(6)) {
            num = Integer.valueOf(cursor.getInt(6));
        }
        this.indexMappingType = num;
    }

    public IndexMapping(Parcel in) {
        Boolean bool;
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.fieldName = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() == (byte) 0) {
            bool = null;
        } else {
            bool = Boolean.valueOf(in.readByte() != (byte) 0);
        }
        this.isColumnNum = bool;
        this.columnName = in.readByte() == (byte) 0 ? null : in.readString();
        this.columnNums = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.indexMappingType = num;
    }

    private IndexMapping(Integer id, String fieldName, Boolean isColumnNum, String columnName, String columnNums, Integer indexMappingType) {
        this.id = id;
        this.fieldName = fieldName;
        this.isColumnNum = isColumnNum;
        this.columnName = columnName;
        this.columnNums = columnNums;
        this.indexMappingType = indexMappingType;
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

    public String getFieldName() {
        return this.fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
        setValue();
    }

    public Boolean getIsColumnNum() {
        return this.isColumnNum;
    }

    public void setIsColumnNum(Boolean isColumnNum) {
        this.isColumnNum = isColumnNum;
        setValue();
    }

    public String getColumnName() {
        return this.columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
        setValue();
    }

    public String getColumnNums() {
        return this.columnNums;
    }

    public void setColumnNums(String columnNums) {
        this.columnNums = columnNums;
        setValue();
    }

    public Integer getIndexMappingType() {
        return this.indexMappingType;
    }

    public void setIndexMappingType(Integer indexMappingType) {
        this.indexMappingType = indexMappingType;
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
        if (this.fieldName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.fieldName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isColumnNum != null) {
            byte b;
            out.writeByte((byte) 1);
            if (this.isColumnNum.booleanValue()) {
                b = (byte) 1;
            } else {
                b = (byte) 0;
            }
            out.writeByte(b);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.columnName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.columnName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.columnNums != null) {
            out.writeByte((byte) 1);
            out.writeString(this.columnNums);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.indexMappingType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.indexMappingType.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<IndexMapping> getHelper() {
        return IndexMappingHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.search.IndexMapping";
    }

    public String getDatabaseName() {
        return "dsSearch";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("IndexMapping { id: ").append(this.id);
        sb.append(", fieldName: ").append(this.fieldName);
        sb.append(", isColumnNum: ").append(this.isColumnNum);
        sb.append(", columnName: ").append(this.columnName);
        sb.append(", columnNums: ").append(this.columnNums);
        sb.append(", indexMappingType: ").append(this.indexMappingType);
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
        return "0.0.5";
    }

    public int getDatabaseVersionCode() {
        return 5;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
