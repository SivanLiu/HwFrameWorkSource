package com.huawei.nb.notification;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Collections;
import java.util.List;

public class ChangeNotification implements Parcelable {
    public static final Creator<ChangeNotification> CREATOR = new Creator<ChangeNotification>() {
        public ChangeNotification createFromParcel(Parcel in) {
            return new ChangeNotification(in);
        }

        public ChangeNotification[] newArray(int size) {
            return new ChangeNotification[size];
        }
    };
    private Class clazz;
    private List deletedItems;
    private List insertedItems;
    private boolean isDeleteAll = false;
    private List updatedItems;

    public ChangeNotification(Class clazz, boolean isDeleteAll, List insertedItems, List updatedItems, List deletedItems) {
        this.clazz = clazz;
        this.isDeleteAll = isDeleteAll;
        if (insertedItems == null) {
            insertedItems = Collections.emptyList();
        }
        this.insertedItems = insertedItems;
        if (updatedItems == null) {
            updatedItems = Collections.emptyList();
        }
        this.updatedItems = updatedItems;
        if (deletedItems == null) {
            deletedItems = Collections.emptyList();
        }
        this.deletedItems = deletedItems;
    }

    protected ChangeNotification(Parcel in) {
        boolean z;
        this.clazz = (Class) in.readSerializable();
        if (in.readInt() == 1) {
            z = true;
        } else {
            z = false;
        }
        this.isDeleteAll = z;
        this.insertedItems = in.readArrayList(this.clazz.getClassLoader());
        this.updatedItems = in.readArrayList(this.clazz.getClassLoader());
        this.deletedItems = in.readArrayList(this.clazz.getClassLoader());
    }

    public Class getType() {
        return this.clazz;
    }

    public boolean isDeleteAll() {
        return this.isDeleteAll;
    }

    public List getInsertedItems() {
        return this.insertedItems;
    }

    public List getUpdatedItems() {
        return this.updatedItems;
    }

    public List getDeletedItems() {
        return this.deletedItems;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(this.clazz);
        dest.writeInt(this.isDeleteAll ? 1 : 0);
        dest.writeList(this.insertedItems);
        dest.writeList(this.updatedItems);
        dest.writeList(this.deletedItems);
    }
}
