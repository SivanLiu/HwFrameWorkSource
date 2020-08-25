package com.huawei.nb.query;

import android.os.Parcel;
import android.os.Parcelable;

public class RelationshipQuery implements IQuery {
    public static final Creator<RelationshipQuery> CREATOR = new Creator<RelationshipQuery>() {
        /* class com.huawei.nb.query.RelationshipQuery.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public RelationshipQuery createFromParcel(Parcel in) {
            return new RelationshipQuery(in);
        }

        @Override // android.os.Parcelable.Creator
        public RelationshipQuery[] newArray(int size) {
            return new RelationshipQuery[size];
        }
    };
    private String entityName;
    private String fieldName;
    private Object objectId;
    private RelationType type;

    public enum RelationType {
        TO_ONE,
        TO_MANY
    }

    public RelationshipQuery(String entityName2, Object objectId2, String fieldName2, RelationType type2) {
        this.entityName = entityName2;
        this.objectId = objectId2;
        this.fieldName = fieldName2;
        this.type = type2;
    }

    protected RelationshipQuery(Parcel in) {
        this.entityName = in.readString();
        this.objectId = in.readValue(null);
        this.fieldName = in.readString();
        this.type = RelationType.values()[in.readInt()];
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.entityName);
        dest.writeValue(this.objectId);
        dest.writeString(this.fieldName);
        dest.writeInt(this.type.ordinal());
    }

    public int describeContents() {
        return 0;
    }

    @Override // com.huawei.nb.query.IQuery
    public String getEntityName() {
        return this.entityName;
    }

    @Override // com.huawei.nb.query.IQuery
    public boolean isValid() {
        return true;
    }

    public Object getObjectId() {
        return this.objectId;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public RelationType getType() {
        return this.type;
    }
}
