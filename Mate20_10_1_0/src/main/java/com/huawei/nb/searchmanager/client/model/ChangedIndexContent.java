package com.huawei.nb.searchmanager.client.model;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

public class ChangedIndexContent implements Parcelable {
    public static final Creator<ChangedIndexContent> CREATOR = new Creator<ChangedIndexContent>() {
        /* class com.huawei.nb.searchmanager.client.model.ChangedIndexContent.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public ChangedIndexContent createFromParcel(Parcel in) {
            return new ChangedIndexContent(in);
        }

        @Override // android.os.Parcelable.Creator
        public ChangedIndexContent[] newArray(int size) {
            return new ChangedIndexContent[size];
        }
    };
    private List<IndexData> deletedItems;
    private List<IndexData> insertedItems;
    private List<IndexData> updatedItems;

    public ChangedIndexContent(List<IndexData> insertedItems2, List<IndexData> updatedItems2, List<IndexData> deletedItems2) {
        this.insertedItems = insertedItems2 == null ? new ArrayList<>(0) : insertedItems2;
        this.updatedItems = updatedItems2 == null ? new ArrayList<>(0) : updatedItems2;
        this.deletedItems = deletedItems2 == null ? new ArrayList<>(0) : deletedItems2;
    }

    private ChangedIndexContent(Parcel in) {
        this.insertedItems = in.readArrayList(IndexData.class.getClassLoader());
        this.updatedItems = in.readArrayList(IndexData.class.getClassLoader());
        this.deletedItems = in.readArrayList(IndexData.class.getClassLoader());
    }

    public List<IndexData> getInsertedItems() {
        return this.insertedItems;
    }

    public List<IndexData> getUpdatedItems() {
        return this.updatedItems;
    }

    public List<IndexData> getDeletedItems() {
        return this.deletedItems;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(this.insertedItems);
        dest.writeList(this.updatedItems);
        dest.writeList(this.deletedItems);
    }
}
