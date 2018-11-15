package com.huawei.nb.model.meta;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class TableSync extends AManagedObject {
    public static final Creator<TableSync> CREATOR = new Creator<TableSync>() {
        public TableSync createFromParcel(Parcel in) {
            return new TableSync(in);
        }

        public TableSync[] newArray(int size) {
            return new TableSync[size];
        }
    };
    private Integer mChannel;
    private String mCloudUri;
    private String mDBName;
    private Integer mId;
    private Integer mSyncMode;
    private Integer mSyncTime;
    private String mTableName;
    private Integer mTitle;

    public TableSync(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mDBName = cursor.getString(2);
        this.mTableName = cursor.getString(3);
        this.mCloudUri = cursor.getString(4);
        this.mTitle = cursor.isNull(5) ? null : Integer.valueOf(cursor.getInt(5));
        this.mChannel = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        this.mSyncMode = cursor.isNull(7) ? null : Integer.valueOf(cursor.getInt(7));
        if (!cursor.isNull(8)) {
            num = Integer.valueOf(cursor.getInt(8));
        }
        this.mSyncTime = num;
    }

    public TableSync(Parcel in) {
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mDBName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mTableName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mCloudUri = in.readByte() == (byte) 0 ? null : in.readString();
        this.mTitle = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mChannel = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mSyncMode = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.mSyncTime = num;
    }

    private TableSync(Integer mId, String mDBName, String mTableName, String mCloudUri, Integer mTitle, Integer mChannel, Integer mSyncMode, Integer mSyncTime) {
        this.mId = mId;
        this.mDBName = mDBName;
        this.mTableName = mTableName;
        this.mCloudUri = mCloudUri;
        this.mTitle = mTitle;
        this.mChannel = mChannel;
        this.mSyncMode = mSyncMode;
        this.mSyncTime = mSyncTime;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getMId() {
        return this.mId;
    }

    public void setMId(Integer mId) {
        this.mId = mId;
        setValue();
    }

    public String getMDBName() {
        return this.mDBName;
    }

    public void setMDBName(String mDBName) {
        this.mDBName = mDBName;
        setValue();
    }

    public String getMTableName() {
        return this.mTableName;
    }

    public void setMTableName(String mTableName) {
        this.mTableName = mTableName;
        setValue();
    }

    public String getMCloudUri() {
        return this.mCloudUri;
    }

    public void setMCloudUri(String mCloudUri) {
        this.mCloudUri = mCloudUri;
        setValue();
    }

    public Integer getMTitle() {
        return this.mTitle;
    }

    public void setMTitle(Integer mTitle) {
        this.mTitle = mTitle;
        setValue();
    }

    public Integer getMChannel() {
        return this.mChannel;
    }

    public void setMChannel(Integer mChannel) {
        this.mChannel = mChannel;
        setValue();
    }

    public Integer getMSyncMode() {
        return this.mSyncMode;
    }

    public void setMSyncMode(Integer mSyncMode) {
        this.mSyncMode = mSyncMode;
        setValue();
    }

    public Integer getMSyncTime() {
        return this.mSyncTime;
    }

    public void setMSyncTime(Integer mSyncTime) {
        this.mSyncTime = mSyncTime;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.mId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.mDBName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mDBName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTableName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mTableName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCloudUri != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mCloudUri);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTitle != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mTitle.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mChannel != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mChannel.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSyncMode != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mSyncMode.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSyncTime != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mSyncTime.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<TableSync> getHelper() {
        return TableSyncHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.meta.TableSync";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("TableSync { mId: ").append(this.mId);
        sb.append(", mDBName: ").append(this.mDBName);
        sb.append(", mTableName: ").append(this.mTableName);
        sb.append(", mCloudUri: ").append(this.mCloudUri);
        sb.append(", mTitle: ").append(this.mTitle);
        sb.append(", mChannel: ").append(this.mChannel);
        sb.append(", mSyncMode: ").append(this.mSyncMode);
        sb.append(", mSyncTime: ").append(this.mSyncTime);
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
