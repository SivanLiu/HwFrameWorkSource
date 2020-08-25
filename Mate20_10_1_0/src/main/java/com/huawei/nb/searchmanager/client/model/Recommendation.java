package com.huawei.nb.searchmanager.client.model;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

public class Recommendation implements Parcelable {
    public static final Creator<Recommendation> CREATOR = new Creator<Recommendation>() {
        /* class com.huawei.nb.searchmanager.client.model.Recommendation.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public Recommendation createFromParcel(Parcel in) {
            return new Recommendation(in);
        }

        @Override // android.os.Parcelable.Creator
        public Recommendation[] newArray(int size) {
            return new Recommendation[size];
        }
    };
    private long count;
    private String field;
    private List<IndexData> indexDataList;
    private String value;

    public Recommendation(String field2, String value2, List<IndexData> indexDataList2, long count2) {
        this.field = field2;
        this.value = value2;
        this.indexDataList = indexDataList2;
        this.count = count2;
    }

    public Recommendation(Parcel in) {
        this.field = in.readString();
        this.value = in.readString();
        this.indexDataList = in.readArrayList(IndexData.class.getClassLoader());
        this.count = in.readLong();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.field);
        dest.writeString(this.value);
        dest.writeList(this.indexDataList);
        dest.writeLong(this.count);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "Recommendation[field=" + this.field + ",value=" + this.value + ",count=" + this.count + "]";
    }

    public void setField(String field2) {
        this.field = field2;
    }

    public void setValue(String value2) {
        this.value = value2;
    }

    public void setIndexDataList(List<IndexData> indexDataList2) {
        this.indexDataList = indexDataList2;
    }

    public void setCount(long count2) {
        this.count = count2;
    }

    public void appendCount(long increment) {
        this.count += increment;
    }

    public String getField() {
        return this.field;
    }

    public String getValue() {
        return this.value;
    }

    public List<IndexData> getIndexDataList() {
        return this.indexDataList;
    }

    public long getCount() {
        return this.count;
    }
}
