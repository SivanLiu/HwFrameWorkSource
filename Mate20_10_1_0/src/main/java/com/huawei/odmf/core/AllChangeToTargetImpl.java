package com.huawei.odmf.core;

import com.huawei.odmf.user.api.AllChangeToTarget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AllChangeToTargetImpl implements AllChangeToTarget {
    private Set<ManagedObject> deletedObjectsSet = new HashSet();
    private Set<ManagedObject> insertedObjectsSet = new HashSet();
    private boolean isDeleteAll = false;
    private Set<ManagedObject> updatedObjectsSet = new HashSet();

    public AllChangeToTargetImpl() {
    }

    public AllChangeToTargetImpl(boolean isDeleteAll2) {
        this.isDeleteAll = isDeleteAll2;
    }

    @Override // com.huawei.odmf.user.api.AllChangeToTarget
    public void addToInsertList(ManagedObject manageObject) {
        if (!this.insertedObjectsSet.contains(manageObject)) {
            this.insertedObjectsSet.add(manageObject);
        }
    }

    @Override // com.huawei.odmf.user.api.AllChangeToTarget
    public void addToUpdatedList(ManagedObject manageObject) {
        if (!this.updatedObjectsSet.contains(manageObject)) {
            this.updatedObjectsSet.add(manageObject);
        }
    }

    @Override // com.huawei.odmf.user.api.AllChangeToTarget
    public void addToDeletedList(ManagedObject manageObject) {
        if (!this.deletedObjectsSet.contains(manageObject)) {
            this.deletedObjectsSet.add(manageObject);
        }
    }

    @Override // com.huawei.odmf.user.api.AllChangeToTarget
    public List<ManagedObject> getInsertList() {
        return new ArrayList(this.insertedObjectsSet);
    }

    @Override // com.huawei.odmf.user.api.AllChangeToTarget
    public List<ManagedObject> getUpdatedList() {
        return new ArrayList(this.updatedObjectsSet);
    }

    @Override // com.huawei.odmf.user.api.AllChangeToTarget
    public List<ManagedObject> getDeletedList() {
        return new ArrayList(this.deletedObjectsSet);
    }

    @Override // com.huawei.odmf.user.api.AllChangeToTarget
    public boolean isDeleteAll() {
        return this.isDeleteAll;
    }
}
