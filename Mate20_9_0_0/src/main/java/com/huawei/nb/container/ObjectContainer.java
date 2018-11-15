package com.huawei.nb.container;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ObjectContainer<T> implements Container<T>, Parcelable {
    public static final Creator<ObjectContainer> CREATOR = new Creator<ObjectContainer>() {
        public ObjectContainer createFromParcel(Parcel in) {
            return new ObjectContainer(in);
        }

        public ObjectContainer[] newArray(int size) {
            return new ObjectContainer[size];
        }
    };
    public static final ObjectContainer EMPTY = new ObjectContainer(Object.class, Collections.emptyList());
    private Class<T> clazz;
    private List<T> objects;
    private String pkgName;

    public ObjectContainer(Class<T> clazz, List<T> objects, String pkgName) {
        this(clazz, objects);
        this.pkgName = pkgName;
    }

    public ObjectContainer(Class<T> clazz, List<T> objects) {
        if (clazz == null) {
            throw new IllegalArgumentException();
        }
        this.clazz = clazz;
        this.objects = objects;
    }

    public ObjectContainer(Class<T> clazz) {
        this(clazz, new ArrayList());
    }

    public String getPkgName() {
        return this.pkgName;
    }

    public boolean add(T object) {
        if (object != null) {
            return this.objects.add(object);
        }
        return false;
    }

    public boolean remove(T t) {
        return this.objects.remove(t);
    }

    public boolean delete(T t) {
        return remove(t);
    }

    public void clear() {
        this.objects.clear();
    }

    public void clearObjects() {
        this.objects.clear();
    }

    protected ObjectContainer(Parcel in) {
        this.clazz = (Class) in.readSerializable();
        if (this.clazz != null) {
            this.objects = in.readArrayList(this.clazz.getClassLoader());
        } else {
            this.objects = Collections.emptyList();
        }
        if (in.readInt() == 1) {
            this.pkgName = in.readString();
        } else {
            this.pkgName = null;
        }
    }

    public Class type() {
        return this.clazz;
    }

    public List<T> get() {
        return this.objects;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(this.clazz);
        dest.writeList(this.objects);
        if (this.pkgName != null) {
            dest.writeInt(1);
            dest.writeString(this.pkgName);
            return;
        }
        dest.writeInt(0);
    }

    public ObjectContainer readFromParcel(Parcel in) {
        return new ObjectContainer(in);
    }

    public int describeContents() {
        return 0;
    }
}
