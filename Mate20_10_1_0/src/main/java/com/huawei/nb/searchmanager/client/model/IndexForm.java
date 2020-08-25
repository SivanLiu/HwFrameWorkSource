package com.huawei.nb.searchmanager.client.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.nb.utils.logger.DSLog;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;

public class IndexForm implements Parcelable {
    public static final Creator<IndexForm> CREATOR = new Creator<IndexForm>() {
        /* class com.huawei.nb.searchmanager.client.model.IndexForm.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public IndexForm createFromParcel(Parcel in) {
            return new IndexForm(in);
        }

        @Override // android.os.Parcelable.Creator
        public IndexForm[] newArray(int size) {
            return new IndexForm[size];
        }
    };
    private static final String TAG = "IndexForm";
    private String indexFieldName;
    private String indexType = IndexType.ANALYZED;
    private boolean isPrimaryKey = false;
    private boolean isSearch = true;
    private boolean isStore = true;

    public IndexForm(String indexFieldName2) {
        this.indexFieldName = indexFieldName2;
    }

    public IndexForm() {
    }

    public IndexForm(Parcel in) {
        boolean z;
        boolean z2;
        boolean z3 = true;
        this.indexFieldName = in.readString();
        this.indexType = in.readString();
        if (in.readByte() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.isPrimaryKey = z;
        if (in.readByte() != 0) {
            z2 = true;
        } else {
            z2 = false;
        }
        this.isStore = z2;
        this.isSearch = in.readByte() == 0 ? false : z3;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i;
        int i2;
        int i3 = 1;
        dest.writeString(this.indexFieldName);
        dest.writeString(this.indexType);
        if (this.isPrimaryKey) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeByte((byte) i);
        if (this.isStore) {
            i2 = 1;
        } else {
            i2 = 0;
        }
        dest.writeByte((byte) i2);
        if (!this.isSearch) {
            i3 = 0;
        }
        dest.writeByte((byte) i3);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "IndexForm[indexFieldName=" + this.indexFieldName + ",indexType=" + this.indexType + ",isPrimaryKey=" + this.isPrimaryKey + ",isStore=" + this.isStore + ",isSearch" + this.isSearch + "]";
    }

    public String getIndexFieldName() {
        return this.indexFieldName;
    }

    public void setIndexFieldName(String indexFieldName2) {
        this.indexFieldName = indexFieldName2;
    }

    public boolean isPrimaryKey() {
        return this.isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.isPrimaryKey = primaryKey;
    }

    public String getIndexType() {
        return this.indexType;
    }

    public void setIndexType(String indexType2) {
        this.indexType = indexType2;
    }

    public boolean isStore() {
        return this.isStore;
    }

    public void setStore(boolean store) {
        this.isStore = store;
    }

    public boolean isSearch() {
        return this.isSearch;
    }

    public void setSearch(boolean search) {
        this.isSearch = search;
    }

    public JSONObject toJsonObj() {
        JSONObject tmpObj = new JSONObject();
        try {
            tmpObj.put("indexFieldName", getIndexFieldName());
            tmpObj.put("isPrimaryKey", isPrimaryKey());
            tmpObj.put("indexType", getIndexType());
            tmpObj.put("isStore", isStore());
            tmpObj.put("isSearch", isSearch());
        } catch (JSONException e) {
            DSLog.et(TAG, "toJsonObj err: %s", e.getMessage());
        }
        return tmpObj;
    }

    public IndexForm fromJsonObj(JSONObject jsonObj) {
        if (jsonObj != null) {
            try {
                setIndexFieldName(jsonObj.getString("indexFieldName"));
                setPrimaryKey(jsonObj.getBoolean("isPrimaryKey"));
                setIndexType(jsonObj.getString("indexType"));
                setStore(jsonObj.getBoolean("isStore"));
                setSearch(jsonObj.getBoolean("isSearch"));
            } catch (JSONException e) {
                DSLog.et(TAG, "fromJsonObj err: %s", e.getMessage());
            }
        }
        return this;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!(obj instanceof IndexForm)) {
            return false;
        }
        IndexForm that = (IndexForm) obj;
        return this.isPrimaryKey == that.isPrimaryKey && this.isStore == that.isStore && this.isSearch == that.isSearch && Objects.equals(this.indexFieldName, that.indexFieldName) && Objects.equals(this.indexType, that.indexType);
    }

    public int hashCode() {
        return Objects.hash(this.indexFieldName, Boolean.valueOf(this.isPrimaryKey), this.indexType, Boolean.valueOf(this.isStore), Boolean.valueOf(this.isSearch));
    }
}
