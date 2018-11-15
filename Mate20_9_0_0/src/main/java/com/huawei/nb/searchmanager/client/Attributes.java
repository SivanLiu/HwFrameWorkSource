package com.huawei.nb.searchmanager.client;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class Attributes implements Parcelable {
    public static final Creator<Attributes> CREATOR = new Creator<Attributes>() {
        public Attributes createFromParcel(Parcel in) {
            return new Attributes(in);
        }

        public Attributes[] newArray(int size) {
            return new Attributes[size];
        }
    };
    private String dataFieldName;
    private String indexFieldName;
    private String indexFieldValue;
    private String indexStatus;
    private boolean isPrimaryKey;
    private String storeStatus;

    public String getDataFieldName() {
        return this.dataFieldName;
    }

    public void setDataFieldName(String dataFieldName) {
        this.dataFieldName = dataFieldName;
    }

    public boolean isPrimaryKey() {
        return this.isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.isPrimaryKey = primaryKey;
    }

    public String getIndexFieldName() {
        return this.indexFieldName;
    }

    public void setIndexFieldName(String indexFieldName) {
        this.indexFieldName = indexFieldName;
    }

    public String getIndexFieldValue() {
        return this.indexFieldValue;
    }

    public void setIndexFieldValue(String indexFieldValue) {
        this.indexFieldValue = indexFieldValue;
    }

    public String getStoreStatus() {
        return this.storeStatus;
    }

    public void setStoreStatus(String storeStatus) {
        this.storeStatus = storeStatus;
    }

    public String getIndexStatus() {
        return this.indexStatus;
    }

    public void setIndexStatus(String indexStatus) {
        this.indexStatus = indexStatus;
    }

    protected Attributes(Parcel in) {
        this.dataFieldName = in.readString();
        this.isPrimaryKey = in.readByte() != (byte) 0;
        this.indexFieldName = in.readString();
        this.indexFieldValue = in.readString();
        this.storeStatus = in.readString();
        this.indexStatus = in.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.dataFieldName);
        dest.writeByte((byte) (this.isPrimaryKey ? 1 : 0));
        dest.writeString(this.indexFieldName);
        dest.writeString(this.indexFieldValue);
        dest.writeString(this.storeStatus);
        dest.writeString(this.indexStatus);
    }

    public int describeContents() {
        return 0;
    }
}
