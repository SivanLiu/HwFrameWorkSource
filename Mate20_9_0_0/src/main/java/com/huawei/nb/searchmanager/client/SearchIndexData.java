package com.huawei.nb.searchmanager.client;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.HashMap;

public class SearchIndexData implements Parcelable {
    public static final Creator<SearchIndexData> CREATOR = new Creator<SearchIndexData>() {
        public SearchIndexData createFromParcel(Parcel in) {
            return new SearchIndexData(in);
        }

        public SearchIndexData[] newArray(int size) {
            return new SearchIndexData[size];
        }
    };
    private int dataType;
    private HashMap<String, String> fieldMap;
    private boolean status;

    public HashMap<String, String> getFieldMap() {
        return this.fieldMap;
    }

    public void setFieldMap(HashMap<String, String> fieldMap) {
        this.fieldMap = fieldMap;
    }

    public int getDataType() {
        return this.dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public boolean isStatus() {
        return this.status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    protected SearchIndexData(Parcel in) {
        this.dataType = in.readInt();
        this.status = in.readByte() != (byte) 0;
        this.fieldMap = in.readHashMap(HashMap.class.getClassLoader());
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.dataType);
        dest.writeByte((byte) (this.status ? 1 : 0));
        dest.writeMap(this.fieldMap);
    }

    public int describeContents() {
        return 0;
    }
}
