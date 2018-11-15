package com.huawei.odmf.core;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.huawei.odmf.exception.ODMFIllegalStateException;
import com.huawei.odmf.exception.ODMFUnsupportedOperationException;
import com.huawei.odmf.model.api.Attribute;
import com.huawei.odmf.model.api.Entity;
import com.huawei.odmf.model.api.ObjectModel;
import com.huawei.odmf.model.api.Relationship;
import com.huawei.odmf.predicate.FetchRequest;
import com.huawei.odmf.predicate.SaveRequest;
import com.huawei.odmf.user.api.ObjectContext;
import com.huawei.odmf.utils.LOG;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistentStore {
    protected static final String METADATA_DATABASE_VERSION = "databaseVersion";
    protected static final String METADATA_DATABASE_VERSION_CODE = "databaseVersionCode";
    protected static final String METADATA_ENTITY_VERSION_CODE_SUFFIX = "_versionCode";
    protected static final String METADATA_ENTITY_VERSION_SUFFIX = "_version";
    private final int databaseType;
    private final Object lock;
    private final Map<String, Object> metadata;
    protected ObjectModel model;
    protected String path;
    private final int storageMode;
    protected Uri uri;
    private long version;

    protected PersistentStore() {
        this.metadata = new HashMap();
        this.lock = new Object();
        this.version = 0;
        this.model = null;
        this.databaseType = Configuration.CONFIGURATION_DATABASE_ANDROID;
        this.storageMode = Configuration.CONFIGURATION_STORAGE_MODE_DISK;
        this.path = null;
        this.uri = null;
    }

    protected PersistentStore(String path, int databaseType, int storageMode, String uriString) {
        this.metadata = new HashMap();
        this.lock = new Object();
        this.path = path;
        this.databaseType = databaseType;
        this.storageMode = storageMode;
        this.version = 0;
        if (uriString != null) {
            this.uri = Uri.parse(uriString);
        } else {
            this.uri = null;
        }
    }

    protected void increaseVersion() {
        synchronized (this.lock) {
            this.version++;
        }
    }

    protected long getVersion() {
        long j;
        synchronized (this.lock) {
            j = this.version;
        }
        return j;
    }

    protected Map<String, Object> getMetadata() {
        return this.metadata;
    }

    protected void setMetadata(String key, Object metadata) {
        this.metadata.put(key, metadata);
    }

    public String getCurrentDBVersion() {
        Object obj = this.metadata.get(METADATA_DATABASE_VERSION);
        if (obj != null) {
            return (String) obj;
        }
        LOG.logE("Execute getCurrentDBVersion Failed : The metadata of database version does not exist.");
        throw new ODMFIllegalStateException("The metadata of database version does not exist.");
    }

    public String getCurrentEntityVersion(String tableName) {
        Object obj = this.metadata.get(tableName + METADATA_ENTITY_VERSION_SUFFIX);
        if (obj != null) {
            return (String) obj;
        }
        LOG.logE("Execute getCurrentEntityVersion Failed : The metadata of " + tableName + " version does not exist.");
        throw new ODMFIllegalStateException("The metadata of " + tableName + " version does not exist.");
    }

    protected void setNewDBVersions(String newDBVersion, int newDBVersionCode) {
        setMetadata(METADATA_DATABASE_VERSION, newDBVersion);
        setMetadata(METADATA_DATABASE_VERSION_CODE, Integer.valueOf(newDBVersionCode));
        setDBVersions(newDBVersion, newDBVersionCode);
    }

    protected void setNewEntityVersions(String tableName, String newEntityVersion, int newEntityVersionCode) {
        setMetadata(tableName + METADATA_ENTITY_VERSION_SUFFIX, newEntityVersion);
        setMetadata(tableName + METADATA_ENTITY_VERSION_CODE_SUFFIX, Integer.valueOf(newEntityVersionCode));
        setEntityVersions(tableName, newEntityVersion, newEntityVersionCode);
    }

    public int getCurrentDBVersionCode() {
        Object obj = this.metadata.get(METADATA_DATABASE_VERSION_CODE);
        if (obj != null) {
            return ((Integer) obj).intValue();
        }
        LOG.logE("Execute getCurrentDBVersion Failed : The metadata of database version does not exist.");
        throw new ODMFIllegalStateException("The metadata of database version does not exist.");
    }

    public int getCurrentEntityVersionCode(String tableName) {
        Object obj = this.metadata.get(tableName + METADATA_ENTITY_VERSION_CODE_SUFFIX);
        if (obj != null) {
            return ((Integer) obj).intValue();
        }
        LOG.logE("Execute getCurrentEntityVersion Failed : The metadata of " + tableName + " version does not exist.");
        throw new ODMFIllegalStateException("The metadata of " + tableName + " version does not exist.");
    }

    public ObjectModel getModel() {
        return this.model;
    }

    int getDatabaseType() {
        return this.databaseType;
    }

    String getPath() {
        return this.path;
    }

    String getUriString() {
        return this.uri.toString();
    }

    Uri getUri() {
        return this.uri;
    }

    void clearKey(byte[] key) {
        if (key != null && key.length > 0) {
            int length = key.length;
            for (int i = 0; i < length; i++) {
                key[i] = (byte) 0;
            }
        }
    }

    ObjectId createObjectID(Entity entity, Object referenceObject) {
        return new AObjectId(entity.getEntityName(), referenceObject, getUriString());
    }

    protected ManagedObject getObjectValues(ObjectId objectId) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method getObjectValues ");
    }

    protected List<ObjectId> getRelationshipObjectId(ObjectId objectId, Relationship relationship) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method getRelationshipObjectId ");
    }

    protected <T extends ManagedObject> List<T> executeFetchRequest(FetchRequest request, ObjectContext objectContext) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method executeFetchRequest ");
    }

    protected List<ObjectId> executeFetchRequestGetObjectID(FetchRequest request) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method executeFetchRequestGetObjectID ");
    }

    protected Cursor executeFetchRequestGetCursor(FetchRequest request) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method executeFetchRequestGetCursor ");
    }

    protected void executeSaveRequest(SaveRequest request) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method executeSaveRequest ");
    }

    protected void executeSaveRequestWithTransaction(SaveRequest request) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method executeSaveRequestWithTransaction ");
    }

    protected void close() {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method close ");
    }

    protected void beginTransaction() {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method beginTransaction ");
    }

    protected boolean inTransaction() {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method inTransaction ");
    }

    protected void rollback() {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method rollback ");
    }

    protected void commit() {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method commit ");
    }

    protected void clearTable(String entityName) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method clearTable ");
    }

    protected void createEntityForMigration(Entity entity) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method createTable ");
    }

    protected void renameEntityForMigration(String newName, String oldName) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method renameTable ");
    }

    protected void addColumnForMigration(Entity entity, Attribute attribute) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method addColumn ");
    }

    protected void addRelationshipForMigration(Entity entity, Relationship relationship) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method addRelationship ");
    }

    protected void dropEntityForMigration(String tableName) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method dropTable ");
    }

    protected void insertDataForMigration(String tableName, ContentValues values) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method insertData ");
    }

    protected void setDBVersions(String dbVersion, int dbVersionCode) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method setDBVersion ");
    }

    protected void setEntityVersions(String tableName, String entityVersion, int entityVersionCode) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method setEntityVersion ");
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersistentStore that = (PersistentStore) o;
        if (getDatabaseType() == that.getDatabaseType() && this.storageMode == that.storageMode) {
            return getPath().equals(that.getPath());
        }
        return false;
    }

    public int hashCode() {
        return (((getDatabaseType() * 31) + this.storageMode) * 31) + getPath().hashCode();
    }

    protected ManagedObject getToOneRelationshipValue(String fieldName, ManagedObject object, ObjectContext objectContext) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method getToOneRelationshipValue ");
    }

    protected List<ManagedObject> getToManyRelationshipValue(String fieldName, ManagedObject object, ObjectContext objectContext) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method getToManyRelationshipValue ");
    }

    protected Cursor executeRawQuerySQL(String sql) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method executeRawQuerySQL ");
    }

    protected void executeRawSQL(String sql) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method executeRawSQL ");
    }

    protected Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the method query ");
    }

    protected List<Object> executeFetchRequestWithAggregateFunction(FetchRequest fetchRequest) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support the query with aggregate function ");
    }

    protected List<ManagedObject> remoteGetToManyRelationshipValue(String fieldName, ObjectId objectID, ObjectContext objectContext) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support remoteGetToManyRelationshipValue function ");
    }

    protected ManagedObject remoteGetToOneRelationshipValue(String fieldName, ObjectId objectID, ObjectContext objectContext) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support remoteGetToOneRelationshipValue function ");
    }

    public void resetDatabaseEncryptKey(byte[] oldKey, byte[] newKey) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support to reset thr key of an encrypted database.");
    }

    public void exportDatabase(String newDBName, byte[] newKey) {
        throw new ODMFUnsupportedOperationException("The persistentStore not support to export database.");
    }

    public void resetMetadata() {
        throw new ODMFUnsupportedOperationException("The persistentStore not support reset metaData.");
    }

    public List<? extends Attribute> getEntityAttributes(String entityName) {
        Entity entity = getModel().getEntity(entityName);
        if (entity == null) {
            return null;
        }
        return entity.getAttributes();
    }
}
