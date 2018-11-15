package com.huawei.odmf.predicate;

import com.huawei.odmf.core.ManagedObject;
import java.util.List;

public class SaveRequest {
    private List<ManagedObject> deletedObjects = null;
    private List<ManagedObject> insertedObjects = null;
    private List<ManagedObject> updatedObjects = null;

    public SaveRequest(List<ManagedObject> insertedObjects, List<ManagedObject> updatedObjects, List<ManagedObject> deletedObjects) {
        this.insertedObjects = insertedObjects;
        this.updatedObjects = updatedObjects;
        this.deletedObjects = deletedObjects;
    }

    public List<ManagedObject> getInsertedObjects() {
        return this.insertedObjects;
    }

    public void setInsertedObjects(List<ManagedObject> insertedObjects) {
        this.insertedObjects = insertedObjects;
    }

    public List<ManagedObject> getUpdatedObjects() {
        return this.updatedObjects;
    }

    public void setUpdatedObjects(List<ManagedObject> updatedObjects) {
        this.updatedObjects = updatedObjects;
    }

    public List<ManagedObject> getDeletedObjects() {
        return this.deletedObjects;
    }

    public void setDeletedObjects(List<ManagedObject> deletedObjects) {
        this.deletedObjects = deletedObjects;
    }
}
