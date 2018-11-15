package com.huawei.nb.query;

import android.os.Parcel;
import android.os.Parcelable.Creator;

public class RelationshipQuery implements IQuery {
    public static final Creator<RelationshipQuery> CREATOR = new Creator<RelationshipQuery>() {
        public RelationshipQuery createFromParcel(Parcel in) {
            return new RelationshipQuery(in);
        }

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

    public RelationshipQuery(String entityName, Object objectId, String fieldName, RelationType type) {
        this.entityName = entityName;
        this.objectId = objectId;
        this.fieldName = fieldName;
        this.type = type;
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

    public String getEntityName() {
        return this.entityName;
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
