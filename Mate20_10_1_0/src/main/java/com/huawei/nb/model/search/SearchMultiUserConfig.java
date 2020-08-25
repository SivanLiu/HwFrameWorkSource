package com.huawei.nb.model.search;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class SearchMultiUserConfig extends AManagedObject {
    public static final Parcelable.Creator<SearchMultiUserConfig> CREATOR = new Parcelable.Creator<SearchMultiUserConfig>() {
        /* class com.huawei.nb.model.search.SearchMultiUserConfig.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public SearchMultiUserConfig createFromParcel(Parcel in) {
            return new SearchMultiUserConfig(in);
        }

        @Override // android.os.Parcelable.Creator
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
        this.userId = !cursor.isNull(2) ? Integer.valueOf(cursor.getInt(2)) : num;
        if (cursor.getInt(3) != 0) {
            z = true;
        } else {
            z = false;
        }
        this.isIntentIdleFinished = z;
        this.lastBuildIntentIndexTime = cursor.getString(4);
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public SearchMultiUserConfig(Parcel in) {
        super(in);
        String str = null;
        if (in.readByte() == 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.userId = in.readByte() == 0 ? null : Integer.valueOf(in.readInt());
        this.isIntentIdleFinished = in.readByte() != 0;
        this.lastBuildIntentIndexTime = in.readByte() != 0 ? in.readString() : str;
    }

    private SearchMultiUserConfig(Integer id2, Integer userId2, boolean isIntentIdleFinished2, String lastBuildIntentIndexTime2) {
        this.id = id2;
        this.userId = userId2;
        this.isIntentIdleFinished = isIntentIdleFinished2;
        this.lastBuildIntentIndexTime = lastBuildIntentIndexTime2;
    }

    public SearchMultiUserConfig() {
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

    public Integer getUserId() {
        return this.userId;
    }

    public void setUserId(Integer userId2) {
        this.userId = userId2;
        setValue();
    }

    public boolean getIsIntentIdleFinished() {
        return this.isIntentIdleFinished;
    }

    public void setIsIntentIdleFinished(boolean isIntentIdleFinished2) {
        this.isIntentIdleFinished = isIntentIdleFinished2;
        setValue();
    }

    public String getLastBuildIntentIndexTime() {
        return this.lastBuildIntentIndexTime;
    }

    public void setLastBuildIntentIndexTime(String lastBuildIntentIndexTime2) {
        this.lastBuildIntentIndexTime = lastBuildIntentIndexTime2;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
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
            b = 1;
        } else {
            b = 0;
        }
        out.writeByte(b);
        if (this.lastBuildIntentIndexTime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.lastBuildIntentIndexTime);
            return;
        }
        out.writeByte((byte) 0);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<SearchMultiUserConfig> getHelper() {
        return SearchMultiUserConfigHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.search.SearchMultiUserConfig";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
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

    @Override // com.huawei.odmf.core.AManagedObject
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override // com.huawei.odmf.core.AManagedObject
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
