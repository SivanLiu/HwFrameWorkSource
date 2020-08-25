package com.huawei.odmf.core;

import android.content.ContentValues;
import android.database.Cursor;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.model.api.Attribute;
import com.huawei.odmf.model.api.Entity;
import com.huawei.odmf.model.api.Relationship;

public class ASchema {
    private PersistentStore persistentStore = null;

    public ASchema(PersistentStore persistentStore2) {
        if (persistentStore2 == null) {
            throw new ODMFIllegalArgumentException("The persistentStore used to create a ASchema is null.");
        }
        this.persistentStore = persistentStore2;
    }

    public void addEntity(Entity entity) {
        this.persistentStore.createEntityForMigration(entity);
    }

    public void addAttribute(Entity entity, Attribute attribute) {
        this.persistentStore.addColumnForMigration(entity, attribute);
    }

    public void renameEntity(String newName, String oldName) {
        this.persistentStore.renameEntityForMigration(newName, oldName);
    }

    public void dropEntity(String tableName) {
        this.persistentStore.dropEntityForMigration(tableName);
    }

    public void addRelationship(Entity entity, Relationship relationship) {
        this.persistentStore.addRelationshipForMigration(entity, relationship);
    }

    public void insertData(String tableName, ContentValues values) {
        this.persistentStore.insertDataForMigration(tableName, values);
    }

    public Cursor rawQuery(String sql) {
        return this.persistentStore.executeRawQuerySQL(sql);
    }

    public void setNewDBVersion(String newVersion) {
        this.persistentStore.setNewDbVersions(newVersion, 1);
    }

    public void setNewEntityVersion(String entityName, String newVersion) {
        this.persistentStore.setNewEntityVersions(entityName, newVersion, 1);
    }
}
