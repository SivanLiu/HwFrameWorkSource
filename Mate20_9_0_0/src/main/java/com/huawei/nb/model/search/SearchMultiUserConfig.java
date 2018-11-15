package com.huawei.nb.model.search;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class SearchMultiUserConfig extends AManagedObject {
    public static final Creator<SearchMultiUserConfig> CREATOR = new Creator<SearchMultiUserConfig>() {
        public SearchMultiUserConfig createFromParcel(Parcel in) {
            return new SearchMultiUserConfig(in);
        }

        public SearchMultiUserConfig[] newArray(int size) {
            return new SearchMultiUserConfig[size];
        }
    };
    private Integer id;
    private boolean isIntentIdleFinished;
    private String lastBuildIntentIndexTime;
    private Integer userId;

    public SearchMultiUserConfig(Cursor cursor) {
        boolean z;
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        if (!cursor.isNull(2)) {
            num = Integer.valueOf(cursor.getInt(2));
        }
        this.userId = num;
        if (cursor.getInt(3) != 0) {
            z = true;
        } else {
            z = false;
        }
        this.isIntentIdleFinished = z;
        this.lastBuildIntentIndexTime = cursor.getString(4);
    }

    public SearchMultiUserConfig(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.userId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.isIntentIdleFinished = in.readByte() != (byte) 0;
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.lastBuildIntentIndexTime = str;
    }

    private SearchMultiUserConfig(Integer id, Integer userId, boolean isIntentIdleFinished, String lastBuildIntentIndexTime) {
        this.id = id;
        this.userId = userId;
        this.isIntentIdleFinished = isIntentIdleFinished;
        this.lastBuildIntentIndexTime = lastBuildIntentIndexTime;
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

    public Integer getUserId() {
        return this.userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
        setValue();
    }

    public boolean getIsIntentIdleFinished() {
        return this.isIntentIdleFinished;
    }

    public void setIsIntentIdleFinished(boolean isIntentIdleFinished) {
        this.isIntentIdleFinished = isIntentIdleFinished;
        setValue();
    }

    public String getLastBuildIntentIndexTime() {
        return this.lastBuildIntentIndexTime;
    }

    public void setLastBuildIntentIndexTime(String lastBuildIntentIndexTime) {
        this.lastBuildIntentIndexTime = lastBuildIntentIndexTime;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        byte b;
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.userId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.userId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isIntentIdleFinished) {
            b = (byte) 1;
        } else {
            b = (byte) 0;
        }
        out.writeByte(b);
        if (this.lastBuildIntentIndexTime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.lastBuildIntentIndexTime);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<SearchMultiUserConfig> getHelper() {
        return SearchMultiUserConfigHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.search.SearchMultiUserConfig";
    }

    public String getDatabaseName() {
        return "dsSearch";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("SearchMultiUserConfig { id: ").append(this.id);
        sb.append(", userId: ").append(this.userId);
        sb.append(", isIntentIdleFinished: ").append(this.isIntentIdleFinished);
        sb.append(", lastBuildIntentIndexTime: ").append(this.lastBuildIntentIndexTime);
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
