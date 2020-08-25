package com.huawei.nb.kv;

import android.os.Parcel;
import android.os.Parcelable;

public class KCompositeString implements Key {
    public static final Creator<KCompositeString> CREATOR = new Creator<KCompositeString>() {
        /* class com.huawei.nb.kv.KCompositeString.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public KCompositeString createFromParcel(Parcel in) {
            return new KCompositeString(in);
        }

        @Override // android.os.Parcelable.Creator
        public KCompositeString[] newArray(int size) {
            return new KCompositeString[size];
        }
    };
    private static final int HASH_CODE_NUM = 31;
    private static final String SEPARATOR = "/";
    private String primaryKey = null;
    private String secondaryKey = null;
    private int valueType = 4;

    public KCompositeString(String pKey) {
        this.primaryKey = pKey;
        this.secondaryKey = null;
        this.valueType = 4;
    }

    public KCompositeString(String pKey, String sKey) {
        this.primaryKey = pKey;
        this.secondaryKey = sKey;
        this.valueType = 4;
    }

    protected KCompositeString(Parcel in) {
        if (in != null) {
            this.primaryKey = in.readString();
            if (this.primaryKey != null && this.primaryKey.equals("")) {
                this.primaryKey = null;
            }
            this.secondaryKey = in.readString();
            if (this.secondaryKey != null && this.secondaryKey.equals("")) {
                this.secondaryKey = null;
            }
            this.valueType = in.readInt();
        }
    }

    public String getPrimaryKey() {
        return this.primaryKey;
    }

    public String getSecondaryKey() {
        return this.secondaryKey;
    }

    public String getSeparator() {
        return SEPARATOR;
    }

    public String getGrantFieldName() {
        return (this.secondaryKey == null || this.secondaryKey.trim().length() == 0) ? this.primaryKey : this.primaryKey + SEPARATOR + this.secondaryKey;
    }

    @Override // com.huawei.nb.kv.Key
    public Integer dType() {
        return 16;
    }

    @Override // com.huawei.nb.kv.Key
    public Integer vType() {
        return Integer.valueOf(this.valueType);
    }

    @Override // com.huawei.nb.kv.Key
    public void vType(Integer valueType2) {
        this.valueType = valueType2.intValue();
    }

    public boolean equals(Object aKey) {
        boolean primaryKeyEqual;
        boolean secondKeyEqual;
        if (aKey == null || !(aKey instanceof KCompositeString)) {
            return false;
        }
        KCompositeString otherKey = (KCompositeString) aKey;
        if (this.primaryKey != null) {
            primaryKeyEqual = this.primaryKey.equals(otherKey.getPrimaryKey());
        } else if (otherKey.getPrimaryKey() == null) {
            primaryKeyEqual = true;
        } else {
            primaryKeyEqual = false;
        }
        if (!primaryKeyEqual) {
            return false;
        }
        if (this.secondaryKey != null) {
            secondKeyEqual = this.secondaryKey.equals(otherKey.getSecondaryKey());
        } else if (otherKey.getSecondaryKey() == null) {
            secondKeyEqual = true;
        } else {
            secondKeyEqual = false;
        }
        return secondKeyEqual;
    }

    public int hashCode() {
        int result;
        int i = 0;
        if (this.primaryKey != null) {
            result = this.primaryKey.hashCode();
        } else {
            result = 0;
        }
        int i2 = result * HASH_CODE_NUM;
        if (this.secondaryKey != null) {
            i = this.secondaryKey.hashCode();
        }
        return ((((i2 + i) * HASH_CODE_NUM) + SEPARATOR.hashCode()) * HASH_CODE_NUM) + this.valueType;
    }

    @Override // com.huawei.nb.kv.Key
    public boolean verify() {
        if (this.primaryKey == null || this.primaryKey.trim().length() == 0) {
            return false;
        }
        if (this.secondaryKey == null) {
            return true;
        }
        for (String key : this.secondaryKey.split(SEPARATOR)) {
            if ("".equals(key.trim())) {
                return false;
            }
        }
        return true;
    }

    @Override // com.huawei.nb.kv.Key
    public String toString() {
        return "Primary Key:(" + (this.primaryKey == null ? "" : this.primaryKey) + "), Secondary Key:(" + (this.secondaryKey == null ? "" : this.secondaryKey) + ")[Separator:(" + SEPARATOR + ")], Value Type:(" + this.valueType + ")";
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.primaryKey == null ? "" : this.primaryKey);
        parcel.writeString(this.secondaryKey == null ? "" : this.secondaryKey);
        parcel.writeInt(this.valueType);
    }
}
