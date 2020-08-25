package com.huawei.nb.notification;

import android.os.Parcel;
import android.os.Parcelable;

public class ModelObserverInfo extends ObserverInfo implements Parcelable {
    public static final Creator<ModelObserverInfo> CREATOR = new Creator<ModelObserverInfo>() {
        /* class com.huawei.nb.notification.ModelObserverInfo.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public ModelObserverInfo createFromParcel(Parcel in) {
            return new ModelObserverInfo(in);
        }

        @Override // android.os.Parcelable.Creator
        public ModelObserverInfo[] newArray(int size) {
            return new ModelObserverInfo[size];
        }
    };
    private static final int HASHCODE_RANDOM = 31;
    private Class modelClazz;

    public ModelObserverInfo(ObserverType type, Class modelClazz2, String pkgName) {
        super(type, pkgName);
        this.modelClazz = modelClazz2;
    }

    public ModelObserverInfo(ObserverType type, Class modelClazz2) {
        super(type, null);
        this.modelClazz = modelClazz2;
    }

    protected ModelObserverInfo(Parcel in) {
        super(in);
        this.modelClazz = (Class) in.readSerializable();
    }

    public Class getModelClazz() {
        return this.modelClazz;
    }

    public int describeContents() {
        return 0;
    }

    @Override // com.huawei.nb.notification.ObserverInfo
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeSerializable(this.modelClazz);
    }

    @Override // com.huawei.nb.notification.ObserverInfo
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        return this.modelClazz == ((ModelObserverInfo) obj).modelClazz;
    }

    @Override // com.huawei.nb.notification.ObserverInfo
    public int hashCode() {
        return (super.hashCode() * HASHCODE_RANDOM) + (this.modelClazz != null ? this.modelClazz.hashCode() : 0);
    }

    @Override // com.huawei.nb.notification.ObserverInfo
    public String toString() {
        return super.toString() + "\tModelObserverInfo{" + "modelClazz=" + this.modelClazz + '}';
    }
}
