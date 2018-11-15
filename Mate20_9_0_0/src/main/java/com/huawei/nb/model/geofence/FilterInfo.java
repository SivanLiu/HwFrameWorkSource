package com.huawei.nb.model.geofence;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import com.huawei.odmf.utils.BindUtils;
import java.sql.Blob;

public class FilterInfo extends AManagedObject {
    public static final Creator<FilterInfo> CREATOR = new Creator<FilterInfo>() {
        public FilterInfo createFromParcel(Parcel in) {
            return new FilterInfo(in);
        }

        public FilterInfo[] newArray(int size) {
            return new FilterInfo[size];
        }
    };
    private Integer mBlockId;
    private Blob mContent;
    private Long mLastUpdated;
    private Integer mReserved1;
    private Integer mReserved2;
    private Integer mRuleId;
    private Integer mType;

    public FilterInfo(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mRuleId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mBlockId = cursor.isNull(2) ? null : Integer.valueOf(cursor.getInt(2));
        this.mType = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.mContent = cursor.isNull(4) ? null : new com.huawei.odmf.data.Blob(cursor.getBlob(4));
        this.mLastUpdated = cursor.isNull(5) ? null : Long.valueOf(cursor.getLong(5));
        this.mReserved1 = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        if (!cursor.isNull(7)) {
            num = Integer.valueOf(cursor.getInt(7));
        }
        this.mReserved2 = num;
    }

    public FilterInfo(Parcel in) {
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mRuleId = null;
            in.readInt();
        } else {
            this.mRuleId = Integer.valueOf(in.readInt());
        }
        this.mBlockId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mContent = in.readByte() == (byte) 0 ? null : (Blob) com.huawei.odmf.data.Blob.CREATOR.createFromParcel(in);
        this.mLastUpdated = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.mReserved1 = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.mReserved2 = num;
    }

    private FilterInfo(Integer mRuleId, Integer mBlockId, Integer mType, Blob mContent, Long mLastUpdated, Integer mReserved1, Integer mReserved2) {
        this.mRuleId = mRuleId;
        this.mBlockId = mBlockId;
        this.mType = mType;
        this.mContent = mContent;
        this.mLastUpdated = mLastUpdated;
        this.mReserved1 = mReserved1;
        this.mReserved2 = mReserved2;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getMRuleId() {
        return this.mRuleId;
    }

    public void setMRuleId(Integer mRuleId) {
        this.mRuleId = mRuleId;
        setValue();
    }

    public Integer getMBlockId() {
        return this.mBlockId;
    }

    public void setMBlockId(Integer mBlockId) {
        this.mBlockId = mBlockId;
        setValue();
    }

    public Integer getMType() {
        return this.mType;
    }

    public void setMType(Integer mType) {
        this.mType = mType;
        setValue();
    }

    public Blob getMContent() {
        return this.mContent;
    }

    public void setMContent(Blob mContent) {
        this.mContent = mContent;
        setValue();
    }

    public Long getMLastUpdated() {
        return this.mLastUpdated;
    }

    public void setMLastUpdated(Long mLastUpdated) {
        this.mLastUpdated = mLastUpdated;
        setValue();
    }

    public Integer getMReserved1() {
        return this.mReserved1;
    }

    public void setMReserved1(Integer mReserved1) {
        this.mReserved1 = mReserved1;
        setValue();
    }

    public Integer getMReserved2() {
        return this.mReserved2;
    }

    public void setMReserved2(Integer mReserved2) {
        this.mReserved2 = mReserved2;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.mRuleId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mRuleId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.mBlockId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mBlockId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mContent != null) {
            out.writeByte((byte) 1);
            if (this.mContent instanceof com.huawei.odmf.data.Blob) {
                ((com.huawei.odmf.data.Blob) this.mContent).writeToParcel(out, 0);
            } else {
                out.writeByteArray(BindUtils.bindBlob(this.mContent));
            }
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mLastUpdated != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mLastUpdated.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReserved1 != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mReserved1.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReserved2 != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mReserved2.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<FilterInfo> getHelper() {
        return FilterInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.geofence.FilterInfo";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("FilterInfo { mRuleId: ").append(this.mRuleId);
        sb.append(", mBlockId: ").append(this.mBlockId);
        sb.append(", mType: ").append(this.mType);
        sb.append(", mContent: ").append(this.mContent);
        sb.append(", mLastUpdated: ").append(this.mLastUpdated);
        sb.append(", mReserved1: ").append(this.mReserved1);
        sb.append(", mReserved2: ").append(this.mReserved2);
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
