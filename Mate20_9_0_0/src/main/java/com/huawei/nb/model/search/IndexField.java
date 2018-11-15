package com.huawei.nb.model.search;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class IndexField extends AManagedObject {
    public static final Creator<IndexField> CREATOR = new Creator<IndexField>() {
        public IndexField createFromParcel(Parcel in) {
            return new IndexField(in);
        }

        public IndexField[] newArray(int size) {
            return new IndexField[size];
        }
    };
    private String fieldName;
    private Integer id;
    private String indexStatus;
    private Integer indexType;
    private Boolean isFieldConstants;
    private String storeStatus;

    public IndexField(Cursor cursor) {
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
        this.isFieldConstants = bool;
        this.storeStatus = cursor.getString(4);
        this.indexStatus = cursor.getString(5);
        if (!cursor.isNull(6)) {
            num = Integer.valueOf(cursor.getInt(6));
        }
        this.indexType = num;
    }

    public IndexField(Parcel in) {
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
        this.isFieldConstants = bool;
        this.storeStatus = in.readByte() == (byte) 0 ? null : in.readString();
        this.indexStatus = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.indexType = num;
    }

    private IndexField(Integer id, String fieldName, Boolean isFieldConstants, String storeStatus, String indexStatus, Integer indexType) {
        this.id = id;
        this.fieldName = fieldName;
        this.isFieldConstants = isFieldConstants;
        this.storeStatus = storeStatus;
        this.indexStatus = indexStatus;
        this.indexType = indexType;
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

    public Boolean getIsFieldConstants() {
        return this.isFieldConstants;
    }

    public void setIsFieldConstants(Boolean isFieldConstants) {
        this.isFieldConstants = isFieldConstants;
        setValue();
    }

    public String getStoreStatus() {
        return this.storeStatus;
    }

    public void setStoreStatus(String storeStatus) {
        this.storeStatus = storeStatus;
        setValue();
    }

    public String getIndexStatus() {
        return this.indexStatus;
    }

    public void setIndexStatus(String indexStatus) {
        this.indexStatus = indexStatus;
        setValue();
    }

    public Integer getIndexType() {
        return this.indexType;
    }

    public void setIndexType(Integer indexType) {
        this.indexType = indexType;
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
        if (this.isFieldConstants != null) {
            byte b;
            out.writeByte((byte) 1);
            if (this.isFieldConstants.booleanValue()) {
                b = (byte) 1;
            } else {
                b = (byte) 0;
            }
            out.writeByte(b);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.storeStatus != null) {
            out.writeByte((byte) 1);
            out.writeString(this.storeStatus);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.indexStatus != null) {
            out.writeByte((byte) 1);
            out.writeString(this.indexStatus);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.indexType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.indexType.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<IndexField> getHelper() {
        return IndexFieldHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.search.IndexField";
    }

    public String getDatabaseName() {
        return "dsSearch";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("IndexField { id: ").append(this.id);
        sb.append(", fieldName: ").append(this.fieldName);
        sb.append(", isFieldConstants: ").append(this.isFieldConstants);
        sb.append(", storeStatus: ").append(this.storeStatus);
        sb.append(", indexStatus: ").append(this.indexStatus);
        sb.append(", indexType: ").append(this.indexType);
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
