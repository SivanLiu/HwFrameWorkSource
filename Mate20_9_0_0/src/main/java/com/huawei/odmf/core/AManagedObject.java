package com.huawei.odmf.core;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFIllegalStateException;
import com.huawei.odmf.model.AEntityHelper;
import com.huawei.odmf.user.api.ObjectContext;
import com.huawei.odmf.utils.LOG;

public class AManagedObject extends ManagedObject implements Parcelable {
    public static final Creator<AManagedObject> CREATOR = new Creator<AManagedObject>() {
        public AManagedObject createFromParcel(Parcel in) {
            return new AManagedObject(in);
        }

        public AManagedObject[] newArray(int size) {
            return new AManagedObject[size];
        }
    };
    public static final int IN_CACHE_DIRTY = 2;
    public static final int IN_CACHE_FRESH = 1;
    public static final int IN_DELETE_LIST = 3;
    public static final int IN_INSERT_LIST = 1;
    public static final int IN_UPDATE_LIST = 2;
    public static final int NEW_OBJ = 0;
    public static final int NOT_IN_CACHE = 0;
    public static final int PERSISTED = 4;
    private int dirty;
    protected ObjectContext lastObjectContext;
    protected ObjectContext objectContext;
    private AObjectId objectID;
    protected int[] relationshipUpdateSigns;
    private Long rowId;
    private int state;
    private String uriString;

    protected int getDirty() {
        return this.dirty;
    }

    protected boolean isDirty() {
        return this.dirty == 2;
    }

    protected void setDirty(int dirty) {
        this.dirty = dirty;
    }

    protected AManagedObject() {
        this.objectContext = null;
        this.lastObjectContext = null;
        this.dirty = 0;
        this.relationshipUpdateSigns = new int[getHelper().getNumberOfRelationships()];
    }

    protected String getUriString() {
        return this.uriString;
    }

    public void setUriString(String uriString) {
        this.uriString = uriString;
        if (this.objectID != null) {
            this.objectID.setUriString(uriString);
        }
    }

    protected void reSetRelationshipUpdateSigns() {
        for (int i = 0; i < this.relationshipUpdateSigns.length; i++) {
            this.relationshipUpdateSigns[i] = 0;
        }
    }

    protected boolean isRelationshipUpdate(int i) {
        if (i >= this.relationshipUpdateSigns.length || i < 0) {
            throw new ODMFIllegalArgumentException("The index of relationship is out of range");
        } else if (this.relationshipUpdateSigns[i] == 1) {
            return true;
        } else {
            return false;
        }
    }

    protected void setRelationshipUpdateSignsTrue(int index) {
        this.relationshipUpdateSigns[index] = 1;
    }

    protected ObjectContext getObjectContext() {
        return this.objectContext;
    }

    protected ObjectContext getLastObjectContext() {
        return this.lastObjectContext;
    }

    public void setObjectContext(ObjectContext context) {
        this.objectContext = context;
    }

    protected void setLastObjectContext(ObjectContext context) {
        this.lastObjectContext = context;
    }

    protected void setValue() {
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return getObjectId().equals(((AManagedObject) o).getObjectId());
    }

    public int hashCode() {
        return getObjectId().hashCode();
    }

    public ObjectId getObjectId() {
        if (this.objectID != null) {
            return this.objectID;
        }
        this.objectID = new AObjectId(getEntityName(), getRowId(), this.uriString);
        return this.objectID;
    }

    protected void setObjectId(ObjectId id) {
        this.objectID = (AObjectId) id;
    }

    public void setState(int state) {
        this.state = state;
    }

    protected int getState() {
        return this.state;
    }

    public Long getRowId() {
        return this.rowId;
    }

    protected void setRowId(Long rowId) {
        this.rowId = rowId;
        if (this.objectID != null) {
            this.objectID.setId(rowId);
        }
    }

    public AEntityHelper getHelper() {
        LOG.logE("execute getHelper error : A model class must implements this method, please check the class.");
        throw new ODMFIllegalStateException("execute getHelper error : A model class must implements this method, please check the class.");
    }

    public String getEntityName() {
        LOG.logE("execute getEntityName error : A model class must implements this method, please check the class.");
        throw new ODMFIllegalStateException("execute getEntityName error : A model class must implements this method, please check the class.");
    }

    protected boolean isPersistent() {
        return this.state > 1;
    }

    protected boolean isInUpdateList() {
        return this.state == 2;
    }

    protected AManagedObject(Parcel in) {
        this.objectContext = null;
        this.lastObjectContext = null;
        this.dirty = 0;
        this.objectID = (AObjectId) AObjectId.CREATOR.createFromParcel(in);
        if (in.readInt() == 0) {
            this.relationshipUpdateSigns = in.createIntArray();
        } else {
            this.relationshipUpdateSigns = new int[0];
        }
        this.rowId = Long.valueOf(in.readLong());
        if (this.rowId.longValue() == -1) {
            this.rowId = null;
        }
        this.state = in.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (this.objectID == null) {
            this.objectID = new AObjectId(getEntityName(), this.rowId == null ? Long.valueOf(-1) : this.rowId, this.uriString);
        }
        this.objectID.writeToParcel(dest, 0);
        if (this.relationshipUpdateSigns.length == 0) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(0);
            dest.writeIntArray(this.relationshipUpdateSigns);
        }
        dest.writeLong(this.rowId == null ? -1 : this.rowId.longValue());
        dest.writeInt(this.state);
    }

    public String getDatabaseName() {
        LOG.logE("execute getDatabaseName error : A model class must implements this method, please check the class.");
        throw new ODMFIllegalStateException("execute getDatabaseName error : A model class must implements this method, please check the class.");
    }
}
