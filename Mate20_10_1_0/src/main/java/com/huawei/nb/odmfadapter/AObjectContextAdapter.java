package com.huawei.nb.odmfadapter;

import android.database.Cursor;
import com.huawei.nb.client.IClient;
import com.huawei.nb.query.RelationshipQuery;
import com.huawei.odmf.core.ManagedObject;
import com.huawei.odmf.core.ObjectId;
import com.huawei.odmf.model.api.Attribute;
import com.huawei.odmf.user.api.IListener;
import com.huawei.odmf.user.api.ObjectContext;
import com.huawei.odmf.user.api.Query;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AObjectContextAdapter implements ObjectContext {
    private IClient client;

    public AObjectContextAdapter(IClient client2) {
        this.client = client2;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public boolean insert(ManagedObject object) {
        return false;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public boolean update(ManagedObject object) {
        return false;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public boolean delete(ManagedObject object) {
        return false;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void beginTransaction() {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void commit() {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void rollback() {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public boolean inTransaction() {
        return false;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public boolean flush() {
        return false;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public <T extends ManagedObject> List<T> get(Class<T> cls) throws IllegalStateException {
        return null;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public <T extends ManagedObject> Query<T> where(Class<T> cls) {
        return null;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void deleteEntityData(Class clz) {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void close() {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void setOpenQueryCache(boolean isOpen) {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void setQueryCacheNumbers(int queryCacheNumbers) {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public ManagedObject getToOneRelationshipValue(String fieldName, ManagedObject object) {
        List retObjects;
        if (validateManagedObject(object) && (retObjects = this.client.executeQuery(new RelationshipQuery(object.getObjectId().getEntityName(), object.getObjectId().getId(), fieldName, RelationshipQuery.RelationType.TO_ONE))) != null && !retObjects.isEmpty()) {
            return (ManagedObject) retObjects.get(0);
        }
        return null;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public List<? extends ManagedObject> getToManyRelationshipValue(String fieldName, ManagedObject object) {
        if (!validateManagedObject(object)) {
            return Collections.emptyList();
        }
        return this.client.executeQuery(new RelationshipQuery(object.getObjectId().getEntityName(), object.getObjectId().getId(), fieldName, RelationshipQuery.RelationType.TO_MANY));
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public ManagedObject remoteGetToOneRelationshipValue(String fieldName, ObjectId objectId) {
        return null;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public List<? extends ManagedObject> remoteGetToManyRelationshipValue(String fieldName, ObjectId objectId) {
        return null;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void registerListener(Object obj, IListener listener) {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void unregisterListener(Object obj, IListener listener) {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public Cursor executeRawQuerySQL(String sql) {
        return null;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void executeRawSQL(String sql) {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return null;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public String getDbVersion() {
        return null;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public String getEntityVersion(String s) {
        return null;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public int getDbVersionCode() {
        return 0;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public int getEntityVersionCode(String s) {
        return 0;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void resetMetadata() {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public List<? extends Attribute> getEntityAttributes(String entityName) {
        return null;
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void setEntityVersions(String s1, String s2, int i) {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void setDbVersions(String s1, int i) {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public void exportDatabase(String newDBName, byte[] newKey) {
    }

    @Override // com.huawei.odmf.user.api.ObjectContext
    public Set<String> getTableInvolvedInSQL(String rawSQL) {
        return null;
    }

    private boolean validateManagedObject(ManagedObject object) {
        if (object == null || object.getObjectId() == null || object.getObjectId().getId() == null || object.getObjectId().getEntityName() == null) {
            return false;
        }
        return true;
    }
}
