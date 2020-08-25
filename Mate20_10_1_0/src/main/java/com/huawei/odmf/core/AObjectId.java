package com.huawei.odmf.core;

import android.os.Parcel;
import android.os.Parcelable;

public class AObjectId implements Parcelable, ObjectId {
    public static final Creator<AObjectId> CREATOR = new Creator<AObjectId>() {
        /* class com.huawei.odmf.core.AObjectId.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public AObjectId createFromParcel(Parcel in) {
            return new AObjectId(in);
        }

        @Override // android.os.Parcelable.Creator
        public AObjectId[] newArray(int size) {
            return new AObjectId[size];
        }
    };
    private String entityName;
    private Object id;
    private String uriString;

    public AObjectId(String entityName2, Object id2) {
        this(entityName2, id2, null);
    }

    public AObjectId(String entityName2, Object id2, String uriString2) {
        this.entityName = entityName2;
        this.id = id2;
        this.uriString = uriString2;
    }

    public AObjectId(Parcel in) {
        this.entityName = in.readString();
        this.id = in.readValue(getClass().getClassLoader());
        this.uriString = in.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.entityName);
        dest.writeValue(this.id);
        dest.writeString(this.uriString);
    }

    public int describeContents() {
        return 0;
    }

    @Override // com.huawei.odmf.core.ObjectId
    public String getEntityName() {
        return this.entityName;
    }

    @Override // com.huawei.odmf.core.ObjectId
    public void setEntityName(String entityName2) {
        this.entityName = entityName2;
    }

    @Override // com.huawei.odmf.core.ObjectId
    public Object getId() {
        return this.id;
    }

    @Override // com.huawei.odmf.core.ObjectId
    public void setId(Object id2) {
        this.id = id2;
    }

    @Override // com.huawei.odmf.core.ObjectId
    public String getUriString() {
        return this.uriString;
    }

    @Override // com.huawei.odmf.core.ObjectId
    public void setUriString(String uriString2) {
        this.uriString = uriString2;
    }

    public int hashCode() {
        int i = 0;
        int hashCode = ((((this.entityName == null ? 0 : this.entityName.hashCode()) + 31) * 31) + (this.id == null ? 0 : this.id.hashCode())) * 31;
        if (this.uriString != null) {
            i = this.uriString.hashCode();
        }
        return hashCode + i;
    }

    public boolean equals(Object object) {
        boolean z = true;
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        AObjectId objectId = (AObjectId) object;
        if (this.id == null || objectId.id == null || !this.id.equals(objectId.id)) {
            return false;
        }
        if (this.entityName != null) {
            if (!this.entityName.equals(objectId.entityName)) {
                return false;
            }
        } else if (objectId.entityName != null) {
            return false;
        }
        if (this.uriString != null) {
            z = this.uriString.equals(objectId.uriString);
        } else if (objectId.uriString != null) {
            z = false;
        }
        return z;
    }

    public String toString() {
        return this.entityName + ":" + this.id + ":" + this.uriString;
    }
}
