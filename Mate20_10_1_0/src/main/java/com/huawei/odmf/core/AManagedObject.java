package com.huawei.odmf.core;

import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFIllegalStateException;
import com.huawei.odmf.model.AEntityHelper;
import com.huawei.odmf.user.api.ObjectContext;
import com.huawei.odmf.utils.LOG;

public class AManagedObject extends ManagedObject implements Parcelable {
    public static final Creator<AManagedObject> CREATOR = new Creator<AManagedObject>() {
        /* class com.huawei.odmf.core.AManagedObject.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public AManagedObject createFromParcel(Parcel in) {
            return new AManagedObject(in);
        }

        @Override // android.os.Parcelable.Creator
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
    private int dirty = 0;
    protected ObjectContext lastObjectContext = null;
    protected ObjectContext objectContext = null;
    private AObjectId objectId;
    protected int[] relationshipUpdateSigns;
    private Long rowId;
    private int state;
    private String uriString;

    protected AManagedObject() {
        initRelationShipSigns();
    }

    protected AManagedObject(Parcel in) {
        this.objectId = AObjectId.CREATOR.createFromParcel(in);
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

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public int getDirty() {
        return this.dirty;
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public boolean isDirty() {
        return this.dirty == 2;
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public void setDirty(int dirty2) {
        this.dirty = dirty2;
    }

    private void initRelationShipSigns() {
        this.relationshipUpdateSigns = new int[getHelper().getNumberOfRelationships()];
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public String getUriString() {
        return this.uriString;
    }

    @Override // com.huawei.odmf.core.ManagedObject
    public void setUriString(String uriString2) {
        this.uriString = uriString2;
        if (this.objectId != null) {
            this.objectId.setUriString(uriString2);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public void reSetRelationshipUpdateSigns() {
        for (int i = 0; i < this.relationshipUpdateSigns.length; i++) {
            this.relationshipUpdateSigns[i] = 0;
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public boolean isRelationshipUpdate(int i) {
        if (i >= this.relationshipUpdateSigns.length || i < 0) {
            throw new ODMFIllegalArgumentException("The index of relationship is out of range");
        } else if (this.relationshipUpdateSigns[i] == 1) {
            return true;
        } else {
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public void setRelationshipUpdateSignsTrue(int index) {
        this.relationshipUpdateSigns[index] = 1;
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public ObjectContext getObjectContext() {
        return this.objectContext;
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public ObjectContext getLastObjectContext() {
        return this.lastObjectContext;
    }

    @Override // com.huawei.odmf.core.ManagedObject
    public void setObjectContext(ObjectContext context) {
        this.objectContext = context;
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public void setLastObjectContext(ObjectContext context) {
        this.lastObjectContext = context;
    }

    /* access modifiers changed from: protected */
    public void setValue() {
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        return getObjectId().equals(((AManagedObject) object).getObjectId());
    }

    public int hashCode() {
        return getObjectId().hashCode();
    }

    @Override // com.huawei.odmf.core.ManagedObject
    public ObjectId getObjectId() {
        if (this.objectId != null) {
            return this.objectId;
        }
        this.objectId = new AObjectId(getEntityName(), getRowId(), this.uriString);
        return this.objectId;
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public void setObjectId(ObjectId id) {
        this.objectId = (AObjectId) id;
    }

    @Override // com.huawei.odmf.core.ManagedObject
    public void setState(int state2) {
        this.state = state2;
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public int getState() {
        return this.state;
    }

    @Override // com.huawei.odmf.core.ManagedObject
    public Long getRowId() {
        return this.rowId;
    }

    @Override // com.huawei.odmf.core.ManagedObject
    public void setRowId(Long rowId2) {
        this.rowId = rowId2;
        if (this.objectId != null) {
            this.objectId.setId(rowId2);
        }
    }

    public AEntityHelper getHelper() {
        LOG.logE("execute getHelper error : A model class must implements this method, please check the class.");
        throw new ODMFIllegalStateException("execute getHelper error : A model class must implements this method, please check the class.");
    }

    @Override // com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        LOG.logE("execute getEntityName error : A model class must implements this method, please check the class.");
        throw new ODMFIllegalStateException("execute getEntityName error : A model class must implements this method, please check the class.");
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.odmf.core.ManagedObject
    public boolean isPersistent() {
        return this.state > 1;
    }

    /* access modifiers changed from: protected */
    public boolean isInUpdateList() {
        return this.state == 2;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (this.objectId == null) {
            this.objectId = new AObjectId(getEntityName(), this.rowId == null ? -1L : this.rowId, this.uriString);
        }
        this.objectId.writeToParcel(dest, 0);
        if (this.relationshipUpdateSigns.length == 0) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(0);
            dest.writeIntArray(this.relationshipUpdateSigns);
        }
        dest.writeLong(this.rowId == null ? -1 : this.rowId.longValue());
        dest.writeInt(this.state);
    }

    @Override // com.huawei.odmf.core.ManagedObject
    public String getDatabaseName() {
        LOG.logE("execute getDatabaseName error : A model class must implements this method, please check the class.");
        throw new ODMFIllegalStateException("execute getDatabaseName error : A model class must implements this method, please check the class.");
    }
}
