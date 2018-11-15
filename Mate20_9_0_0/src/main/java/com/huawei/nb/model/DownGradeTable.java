package com.huawei.nb.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DownGradeTable extends AManagedObject {
    public static final Creator<DownGradeTable> CREATOR = new Creator<DownGradeTable>() {
        public DownGradeTable createFromParcel(Parcel in) {
            return new DownGradeTable(in);
        }

        public DownGradeTable[] newArray(int size) {
            return new DownGradeTable[size];
        }
    };
    private String mDBName;
    private String mFileName;
    private Integer mFromVersion;
    private Integer mId;
    private String mSqlText;
    private Integer mToVersion;

    public DownGradeTable(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mDBName = cursor.getString(2);
        this.mFromVersion = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        if (!cursor.isNull(4)) {
            num = Integer.valueOf(cursor.getInt(4));
        }
        this.mToVersion = num;
        this.mFileName = cursor.getString(5);
        this.mSqlText = cursor.getString(6);
    }

    public DownGradeTable(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mDBName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mFromVersion = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mToVersion = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mFileName = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mSqlText = str;
    }

    private DownGradeTable(Integer mId, String mDBName, Integer mFromVersion, Integer mToVersion, String mFileName, String mSqlText) {
        this.mId = mId;
        this.mDBName = mDBName;
        this.mFromVersion = mFromVersion;
        this.mToVersion = mToVersion;
        this.mFileName = mFileName;
        this.mSqlText = mSqlText;
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

    public Integer getMFromVersion() {
        return this.mFromVersion;
    }

    public void setMFromVersion(Integer mFromVersion) {
        this.mFromVersion = mFromVersion;
        setValue();
    }

    public Integer getMToVersion() {
        return this.mToVersion;
    }

    public void setMToVersion(Integer mToVersion) {
        this.mToVersion = mToVersion;
        setValue();
    }

    public String getMFileName() {
        return this.mFileName;
    }

    public void setMFileName(String mFileName) {
        this.mFileName = mFileName;
        setValue();
    }

    public String getMSqlText() {
        return this.mSqlText;
    }

    public void setMSqlText(String mSqlText) {
        this.mSqlText = mSqlText;
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
        if (this.mFromVersion != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mFromVersion.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mToVersion != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mToVersion.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mFileName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mFileName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSqlText != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mSqlText);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<DownGradeTable> getHelper() {
        return DownGradeTableHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.DownGradeTable";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DownGradeTable { mId: ").append(this.mId);
        sb.append(", mDBName: ").append(this.mDBName);
        sb.append(", mFromVersion: ").append(this.mFromVersion);
        sb.append(", mToVersion: ").append(this.mToVersion);
        sb.append(", mFileName: ").append(this.mFileName);
        sb.append(", mSqlText: ").append(this.mSqlText);
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
