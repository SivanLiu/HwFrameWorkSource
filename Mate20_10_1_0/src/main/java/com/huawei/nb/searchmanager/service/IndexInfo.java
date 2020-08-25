package com.huawei.nb.searchmanager.service;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.File;
import java.util.Objects;

public class IndexInfo implements Parcelable {
    public static final Creator<IndexInfo> CREATOR = new Creator<IndexInfo>() {
        /* class com.huawei.nb.searchmanager.service.IndexInfo.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public IndexInfo createFromParcel(Parcel in) {
            return new IndexInfo(in);
        }

        @Override // android.os.Parcelable.Creator
        public IndexInfo[] newArray(int size) {
            return new IndexInfo[size];
        }
    };
    private String groupId;
    private String indexBankName;
    private int userId;

    public IndexInfo(int userId2, String groupId2, String indexBankName2) {
        this.userId = userId2;
        this.groupId = groupId2;
        this.indexBankName = indexBankName2;
    }

    public IndexInfo(Parcel in) {
        this.userId = in.readInt();
        this.groupId = in.readString();
        this.indexBankName = in.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.userId);
        dest.writeString(this.groupId);
        dest.writeString(this.indexBankName);
    }

    public int describeContents() {
        return 0;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId2) {
        this.userId = userId2;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(String groupId2) {
        this.groupId = groupId2;
    }

    public String getIndexBankName() {
        return this.indexBankName;
    }

    public void setIndexBankName(String indexBankName2) {
        this.indexBankName = indexBankName2;
    }

    public String getIndexPath() {
        return this.userId + File.separator + this.indexBankName + File.separator + this.groupId;
    }

    public String toString() {
        return "IndexInfo[" + getIndexPath() + "]";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!(obj instanceof IndexInfo)) {
            return false;
        }
        IndexInfo indexInfo = (IndexInfo) obj;
        return this.userId == indexInfo.userId && this.groupId.equals(indexInfo.groupId) && this.indexBankName.equals(indexInfo.indexBankName);
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.userId), this.groupId, this.indexBankName);
    }
}
