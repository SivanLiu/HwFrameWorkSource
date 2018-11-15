package com.huawei.odmf.core;

import com.huawei.odmf.user.api.ObjectContext;

public abstract class ManagedObject {
    public abstract String getDatabaseName();

    protected abstract int getDirty();

    public abstract String getEntityName();

    protected abstract ObjectContext getLastObjectContext();

    protected abstract ObjectContext getObjectContext();

    public abstract ObjectId getObjectId();

    public abstract Long getRowId();

    protected abstract int getState();

    protected abstract String getUriString();

    protected abstract boolean isDirty();

    protected abstract boolean isPersistent();

    protected abstract boolean isRelationshipUpdate(int i);

    protected abstract void reSetRelationshipUpdateSigns();

    protected abstract void setDirty(int i);

    protected abstract void setLastObjectContext(ObjectContext objectContext);

    public abstract void setObjectContext(ObjectContext objectContext);

    protected abstract void setObjectId(ObjectId objectId);

    protected abstract void setRowId(Long l);

    public abstract void setState(int i);

    protected abstract void setUriString(String str);
}
