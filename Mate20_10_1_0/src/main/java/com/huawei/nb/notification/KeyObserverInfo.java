package com.huawei.nb.notification;

import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.nb.kv.Key;

public class KeyObserverInfo<K extends Key> extends ObserverInfo implements Parcelable {
    public static final Creator<KeyObserverInfo> CREATOR = new Creator<KeyObserverInfo>() {
        /* class com.huawei.nb.notification.KeyObserverInfo.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public KeyObserverInfo createFromParcel(Parcel in) {
            return new KeyObserverInfo(in);
        }

        @Override // android.os.Parcelable.Creator
        public KeyObserverInfo[] newArray(int size) {
            return new KeyObserverInfo[size];
        }
    };
    private K key;

    public KeyObserverInfo(ObserverType type, K key2, String pkgName) {
        super(type, pkgName);
        this.key = key2;
    }

    public KeyObserverInfo(ObserverType type, K key2) {
        super(type, null);
        this.key = key2;
    }

    protected KeyObserverInfo(Parcel in) {
        super(in);
        Class clazz = (Class) in.readSerializable();
        if (clazz != null) {
            this.key = (K) ((Key) in.readParcelable(clazz.getClassLoader()));
        }
    }

    public K getKey() {
        return this.key;
    }

    public void setKey(K key2) {
        this.key = key2;
    }

    public int describeContents() {
        return 0;
    }

    @Override // com.huawei.nb.notification.ObserverInfo
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        if (this.key == null) {
            dest.writeSerializable(null);
            return;
        }
        dest.writeSerializable(this.key.getClass());
        dest.writeParcelable(this.key, 0);
    }

    @Override // com.huawei.nb.notification.ObserverInfo
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass() || !super.equals(object)) {
            return false;
        }
        return this.key.equals(((KeyObserverInfo) object).key);
    }

    @Override // com.huawei.nb.notification.ObserverInfo
    public int hashCode() {
        return (super.hashCode() * 31) + (this.key != null ? this.key.hashCode() : 0);
    }

    @Override // com.huawei.nb.notification.ObserverInfo
    public String toString() {
        return super.toString() + "\tKeyObserverInfo{" + "key=" + this.key + '}';
    }
}
