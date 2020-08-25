package com.huawei.nb.kv;

import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.nb.kv.Key;
import com.huawei.nb.kv.Value;
import java.util.Objects;

public class KvPair<K extends Key, V extends Value> implements Parcelable {
    public static final Creator<KvPair> CREATOR = new Creator<KvPair>() {
        /* class com.huawei.nb.kv.KvPair.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public KvPair createFromParcel(Parcel in) {
            return new KvPair(in);
        }

        @Override // android.os.Parcelable.Creator
        public KvPair[] newArray(int size) {
            return new KvPair[size];
        }
    };
    private K key;
    private V val;

    public KvPair(K key2, V val2) {
        this.key = key2;
        this.val = val2;
    }

    protected KvPair(Parcel in) {
        Class keyClazz = (Class) in.readSerializable();
        if (keyClazz != null) {
            this.key = (K) ((Key) in.readParcelable(keyClazz.getClassLoader()));
        }
        Class valClazz = (Class) in.readSerializable();
        if (valClazz != null) {
            this.val = (V) ((Value) in.readParcelable(valClazz.getClassLoader()));
        }
    }

    public void key(K key2) {
        this.key = key2;
    }

    public void value(V val2) {
        this.val = val2;
    }

    public K getKey() {
        return this.key;
    }

    public V getValue() {
        return this.val;
    }

    public String toString() {
        return "KvPair{key=" + this.key + ", val=" + this.val + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KvPair<?, ?> kvPair = (KvPair) o;
        return Objects.equals(this.key, kvPair.key) && Objects.equals(this.val, kvPair.val);
    }

    public int hashCode() {
        return Objects.hash(this.key, this.val);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int i) {
        if (this.key == null) {
            dest.writeSerializable(null);
        } else {
            dest.writeSerializable(this.key.getClass());
            dest.writeParcelable(this.key, i);
        }
        if (this.val == null) {
            dest.writeSerializable(null);
            return;
        }
        dest.writeSerializable(this.val.getClass());
        dest.writeParcelable(this.val, i);
    }
}
