package com.huawei.odmf.core;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFRuntimeException;
import com.huawei.odmf.model.api.Attribute;
import com.huawei.odmf.predicate.FetchRequest;
import com.huawei.odmf.predicate.SaveRequest;
import com.huawei.odmf.user.api.ObjectContext;
import com.huawei.odmf.utils.LOG;
import com.huawei.odmf.utils.ODMFCache;
import com.huawei.odmf.utils.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class PersistentStoreCoordinator {
    private static final int EXIST = 1;
    private static final int FAIL = -1;
    private static final int SUCCESS = 0;
    private static final Singleton<PersistentStoreCoordinator> gDefault = new Singleton<PersistentStoreCoordinator>() {
        public PersistentStoreCoordinator create() {
            return new PersistentStoreCoordinator();
        }
    };
    private final Object cacheLock;
    private final ConcurrentHashMap<ObjectContext, PersistentStore> mapContextToPersistentStore;
    private final ConcurrentHashMap<String, PersistentStore> mapUriToPersistentStore;
    private NotifyManager notifyManager;
    private ODMFCache<ObjectId, ManagedObject> objectsCache;
    private final Object persistentStoreLock;

    /* synthetic */ PersistentStoreCoordinator(AnonymousClass1 x0) {
        this();
    }

    protected ConcurrentHashMap<String, PersistentStore> getMapUriToPersistentStore() {
        return this.mapUriToPersistentStore;
    }

    ConcurrentHashMap<ObjectContext, PersistentStore> getMapContextToPersistentStore() {
        return this.mapContextToPersistentStore;
    }

    private PersistentStoreCoordinator() {
        this.mapUriToPersistentStore = new ConcurrentHashMap();
        this.mapContextToPersistentStore = new ConcurrentHashMap();
        this.objectsCache = null;
        this.persistentStoreLock = new Object();
        this.cacheLock = new Object();
        CacheConfig cacheConfig = CacheConfig.getDefault();
        if (cacheConfig.isOpenObjectCache()) {
            this.objectsCache = new ODMFCache(cacheConfig.getObjectCacheNum());
        }
        this.notifyManager = new NotifyManager();
    }

    public static PersistentStoreCoordinator getDefault() {
        return (PersistentStoreCoordinator) gDefault.get();
    }

    protected int createPersistentStore(Uri uri, Configuration storeConfiguration, Context appCtx, String modelPath) {
        return addPersistentStore(uri, storeConfiguration, appCtx, modelPath, null);
    }

    int createEncryptedPersistentStore(Uri uri, Configuration storeConfiguration, Context appCtx, String modelPath, byte[] key) {
        return addPersistentStore(uri, storeConfiguration, appCtx, modelPath, key);
    }

    int createCrossPersistentStore(Uri uri, Configuration storeConfiguration, Context appCtx, Map<Uri, byte[]> uriMap) {
        if (uri == null || appCtx == null || uriMap == null) {
            LOG.logE("createCrossPersistentStore : The parameters has null.");
            throw new ODMFIllegalArgumentException("The input parameter of uri, appCtx and the uriList has null");
        } else if (uriMap.size() == 0) {
            LOG.logE("createCrossPersistentStore : The uriMap contains nothing.");
            throw new ODMFIllegalArgumentException("The uriList contains nothing.");
        } else {
            List<String> databasePaths = new ArrayList();
            List<byte[]> keyList = new ArrayList();
            byte[] key;
            int i;
            try {
                synchronized (this.persistentStoreLock) {
                    for (Entry<Uri, byte[]> attachMap : uriMap.entrySet()) {
                        PersistentStore persistentStore = (PersistentStore) this.mapUriToPersistentStore.get(((Uri) attachMap.getKey()).toString());
                        if (persistentStore == null) {
                            LOG.logE("createCrossPersistentStore : Some uri in uriList does not indicates a persistentStore.");
                            throw new ODMFIllegalArgumentException("Some uri in uriList does not indicates a persistentStore.");
                        }
                        databasePaths.add(persistentStore.getPath());
                        keyList.add(attachMap.getValue());
                    }
                    storeConfiguration.setDatabaseType(Configuration.CONFIGURATION_DATABASE_ODMF);
                    storeConfiguration.setStorageMode(Configuration.CONFIGURATION_STORAGE_MODE_MEMORY);
                    this.mapUriToPersistentStore.put(uri.toString(), new CrossPersistentStore(appCtx, null, uri.toString(), storeConfiguration, databasePaths, keyList));
                }
                for (Entry<Uri, byte[]> attachMap2 : uriMap.entrySet()) {
                    key = (byte[]) attachMap2.getValue();
                    if (key != null) {
                        for (i = 0; i < key.length; i++) {
                            key[i] = (byte) 0;
                        }
                    }
                }
                return 0;
            } catch (Throwable th) {
                for (Entry<Uri, byte[]> attachMap22 : uriMap.entrySet()) {
                    key = (byte[]) attachMap22.getValue();
                    if (key != null) {
                        for (i = 0; i < key.length; i++) {
                            key[i] = (byte) 0;
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:72:?, code:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int addPersistentStore(Uri uri, Configuration storeConfiguration, Context appCtx, String modelPath, byte[] key) {
        if (uri != null && storeConfiguration != null && appCtx != null) {
            synchronized (this.persistentStoreLock) {
                if (!this.mapUriToPersistentStore.containsKey(uri.toString())) {
                    switch (storeConfiguration.getType()) {
                        case 200:
                            PersistentStore cacheStore;
                            for (PersistentStore psVal : this.mapUriToPersistentStore.values()) {
                                if (psVal.getPath() != null && psVal.getPath().equals(storeConfiguration.getPath())) {
                                    LOG.logE("addPersistentStore : database path is exist, so return directly");
                                    return 1;
                                }
                            }
                            if (key == null) {
                                cacheStore = new AndroidSqlPersistentStore(appCtx, modelPath, uri.toString(), storeConfiguration);
                            } else {
                                storeConfiguration.setDatabaseType(Configuration.CONFIGURATION_DATABASE_ODMF);
                                storeConfiguration.setStorageMode(Configuration.CONFIGURATION_STORAGE_MODE_DISK);
                                cacheStore = new EncryptedAndroidSqlPersistentStore(appCtx, modelPath, uri.toString(), storeConfiguration, key);
                            }
                            for (PersistentStore psVal2 : this.mapUriToPersistentStore.values()) {
                                if (psVal2.getPath() != null && psVal2.equals(cacheStore)) {
                                    LOG.logE("addPersistentStore : database path is exist, so return directly");
                                    cacheStore.close();
                                    return 1;
                                }
                            }
                            this.mapUriToPersistentStore.put(uri.toString(), cacheStore);
                            break;
                        case Configuration.CONFIGURATION_TYPE_PROVIDER /*201*/:
                            for (PersistentStore psVal22 : this.mapUriToPersistentStore.values()) {
                                if (psVal22.getPath() != null && psVal22.getPath().equals(storeConfiguration.getPath())) {
                                    LOG.logW("addPersistentStore : database path exists already, so return directly");
                                    return 1;
                                }
                            }
                            break;
                    }
                }
                LOG.logE("addPersistentStore : uri is exist, so return directly");
                return 1;
            }
        }
        LOG.logE("addPersistentStore : The parameters has null.");
        return -1;
    }

    int removePersistentStore(Uri uri) {
        int i = -1;
        if (uri == null) {
            LOG.logE("removePersistentStore : The parameter uri is null.");
        } else {
            synchronized (this.persistentStoreLock) {
                PersistentStore ps = (PersistentStore) this.mapUriToPersistentStore.get(uri.toString());
                if (ps == null) {
                    LOG.logE("the uri " + uri.toString() + "with the corresponding PersistentStore not exist.");
                } else {
                    if (ps.getPath() != null) {
                        for (Entry<ObjectContext, PersistentStore> entry : this.mapContextToPersistentStore.entrySet()) {
                            if (ps.equals((PersistentStore) entry.getValue())) {
                                LOG.logE("because persistentStore is used by other, so cannot remove the correspondence between uri and persistentStore.");
                                break;
                            }
                        }
                    }
                    if (this.mapUriToPersistentStore.containsKey(uri.toString())) {
                        this.mapUriToPersistentStore.remove(uri.toString());
                        ps.close();
                        i = 0;
                    }
                }
            }
        }
        return i;
    }

    int connectPersistentStore(Uri uri, ObjectContext context) {
        int i = -1;
        if (uri == null || context == null) {
            LOG.logE("connectPersistentStore : The parameters has null.");
        } else {
            synchronized (this.persistentStoreLock) {
                PersistentStore ps = (PersistentStore) this.mapUriToPersistentStore.get(uri.toString());
                if (ps != null) {
                    this.mapContextToPersistentStore.putIfAbsent(context, ps);
                    i = 0;
                } else {
                    LOG.logE("connectPersistentStore : The context can not connect to the persistentStore correspond to the input uri.");
                }
            }
        }
        return i;
    }

    PersistentStore getPersistentStore(Uri uri) {
        if (uri == null) {
            LOG.logE("getPersistentStore : The parameter uri is null.");
            return null;
        }
        synchronized (this.persistentStoreLock) {
            PersistentStore ps = (PersistentStore) this.mapUriToPersistentStore.get(uri.toString());
            if (ps != null) {
                return ps;
            }
            LOG.logE("getPersistentStore : Can not get the persistentStore correspond to the input uri.");
            return null;
        }
    }

    int disconnectPersistentStore(Uri uri, ObjectContext context) {
        int i = -1;
        if (uri == null || context == null) {
            LOG.logE("disconnectPersistentStore : The parameters has null.");
        } else {
            synchronized (this.persistentStoreLock) {
                if (this.mapContextToPersistentStore.containsKey(context)) {
                    this.mapContextToPersistentStore.remove(context);
                    removePersistentStore(uri);
                    i = 0;
                }
            }
        }
        return i;
    }

    <T extends ManagedObject> List<T> executeFetchRequest(FetchRequest<T> request, ObjectContext context) {
        if (request == null) {
            LOG.logE("executeFetchRequest : The parameter has null.");
            throw new ODMFIllegalArgumentException("The parameter has null.");
        }
        List<T> managedObjectsList = ((PersistentStore) this.mapContextToPersistentStore.get(context)).executeFetchRequest(request, context);
        int size = managedObjectsList.size();
        for (int i = 0; i < size; i++) {
            ManagedObject managedObject = (ManagedObject) managedObjectsList.get(i);
            managedObject.setObjectContext(context);
            putObjectIntoCache(managedObject);
        }
        return managedObjectsList;
    }

    List<ObjectId> executeFetchRequestGetObjectID(FetchRequest request, ObjectContext context) {
        return getPersistentStore(context).executeFetchRequestGetObjectID(request);
    }

    protected Cursor executeFetchRequestGetCursor(FetchRequest request, ObjectContext objectContext) {
        return getPersistentStore(objectContext).executeFetchRequestGetCursor(request);
    }

    long getPersistentStoreVersion(ObjectContext context) {
        return getPersistentStore(context).getVersion();
    }

    void save(SaveRequest saveRequest, ObjectContext context) {
        if (saveRequest == null) {
            LOG.logE("save : The parameter saveRequest is null.");
            return;
        }
        PersistentStore ps = getPersistentStore(context);
        if (ps.inTransaction()) {
            ps.executeSaveRequest(saveRequest);
            successFinishWork(saveRequest);
            return;
        }
        ps.executeSaveRequestWithTransaction(saveRequest);
        successFinishWork(saveRequest);
        List<ObjectContext> notifyTarget = hasListeners(ps.getUriString());
        if (notifyTarget.size() != 0) {
            sendMessageToObjectContext(saveRequest, context, ps.getUriString(), notifyTarget);
        }
    }

    List<ObjectContext> hasListeners(ObjectContext objectContext) {
        return this.notifyManager.hasListeners(getPersistentStore(objectContext).getUriString());
    }

    List<ObjectContext> hasListeners(String uriString) {
        return this.notifyManager.hasListeners(uriString);
    }

    private void sendMessageToObjectContext(SaveRequest saveRequest, ObjectContext context, String psUri, List<ObjectContext> notifyTarget) {
        this.notifyManager.addMessageToQueue(saveRequest, context, psUri, (List) notifyTarget);
    }

    private void successFinishWork(SaveRequest saveRequest) {
        if (CacheConfig.getDefault().isOpenObjectCache()) {
            for (ManagedObject tItDeleted : saveRequest.getDeletedObjects()) {
                removeObjectIntoCache(tItDeleted);
            }
            for (ManagedObject tInsertedObj : saveRequest.getInsertedObjects()) {
                putObjectIntoCache(tInsertedObj);
            }
            for (ManagedObject tUpdateObj : saveRequest.getUpdatedObjects()) {
                putObjectIntoCache(tUpdateObj);
            }
        }
    }

    ManagedObject getObjectValues(ObjectId objectID, ObjectContext objectContext) {
        if (objectID == null) {
            LOG.logE("getObjectValues : The parameter objectID is null.");
            return null;
        }
        ManagedObject cacheObject = getObjectFromCache(objectID);
        if (cacheObject != null && (!cacheObject.isDirty() || cacheObject.getLastObjectContext() == objectContext)) {
            return cacheObject;
        }
        PersistentStore ps = getPersistentStore(objectContext);
        if (!objectID.getUriString().equals(ps.getUriString())) {
            objectID.setUriString(ps.getUriString());
        }
        ManagedObject object = ps.getObjectValues(objectID);
        if (object == null) {
            LOG.logE("getObjectValues : Can not get the objectNode correspond to the input objectID.");
            return null;
        }
        object.setState(4);
        object.setObjectContext(objectContext);
        return object;
    }

    protected void beginTransaction(ObjectContext context) {
        getPersistentStore(context).beginTransaction();
    }

    protected boolean inTransaction(ObjectContext context) {
        return getPersistentStore(context).inTransaction();
    }

    protected void commit(ObjectContext context, SaveRequest request) {
        PersistentStore ps = getPersistentStore(context);
        ps.commit();
        if (request != null) {
            List<ObjectContext> notifyTarget = this.notifyManager.hasListeners(ps.getUriString());
            if (notifyTarget.size() != 0) {
                sendMessageToObjectContext(request, context, ps.getUriString(), notifyTarget);
            }
        }
    }

    protected void rollback(ObjectContext context) {
        getPersistentStore(context).rollback();
    }

    void clearTable(ObjectContext objectContext, String entityName) {
        PersistentStore ps = getPersistentStore(objectContext);
        ps.clearTable(entityName);
        List notifyTarget = hasListeners(ps.getUriString());
        if (notifyTarget.size() != 0) {
            this.notifyManager.addMessageToQueue(objectContext, entityName, ps.getUriString(), notifyTarget);
        }
    }

    protected ManagedObject getToOneRelationshipValue(String fieldName, ManagedObject object, ObjectContext objectContext) {
        if (!TextUtils.isEmpty(fieldName) && object != null) {
            return getPersistentStore(objectContext).getToOneRelationshipValue(fieldName, object, objectContext);
        }
        LOG.logE("getToOneRelationshipValue : The parameters has null.");
        throw new ODMFIllegalArgumentException("The parameters has null.");
    }

    protected List<ManagedObject> getToManyRelationshipValue(String fieldName, ManagedObject object, ObjectContext objectContext) {
        if (!TextUtils.isEmpty(fieldName) && object != null) {
            return getPersistentStore(objectContext).getToManyRelationshipValue(fieldName, object, objectContext);
        }
        LOG.logE("getToManyRelationshipValue : The parameters has null.");
        throw new ODMFIllegalArgumentException("The parameters has null.");
    }

    protected ManagedObject remoteGetToOneRelationshipValue(String fieldName, ObjectId objectID, ObjectContext objectContext) {
        if (!TextUtils.isEmpty(fieldName) && objectID != null) {
            return getPersistentStore(objectContext).remoteGetToOneRelationshipValue(fieldName, objectID, objectContext);
        }
        LOG.logE("remoteGetToOneRelationshipValue : The parameters has null.");
        throw new ODMFIllegalArgumentException("The parameters has null.");
    }

    protected List<ManagedObject> remoteGetToManyRelationshipValue(String fieldName, ObjectId objectID, ObjectContext objectContext) {
        if (!TextUtils.isEmpty(fieldName) && objectID != null) {
            return getPersistentStore(objectContext).remoteGetToManyRelationshipValue(fieldName, objectID, objectContext);
        }
        LOG.logE("remoteGetToManyRelationshipValue : The parameters has null.");
        throw new ODMFIllegalArgumentException("The parameters has null.");
    }

    protected Cursor executeRawQuerySQL(String sql, ObjectContext objectContext) {
        if (!TextUtils.isEmpty(sql)) {
            return getPersistentStore(objectContext).executeRawQuerySQL(sql);
        }
        LOG.logE("executeRawQuerySQL : The parameter sql is null.");
        throw new ODMFIllegalArgumentException("The parameter sql is null.");
    }

    protected void executeRawSQL(String sql, ObjectContext objectContext) {
        if (TextUtils.isEmpty(sql)) {
            LOG.logE("executeRawSQL : The parameter sql is null.");
            throw new ODMFIllegalArgumentException("The parameter sql is null.");
        } else {
            getPersistentStore(objectContext).executeRawSQL(sql);
        }
    }

    protected Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, ObjectContext objectContext) {
        return getPersistentStore(objectContext).query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    ODMFCache<ObjectId, ManagedObject> getObjectsCache() {
        return this.objectsCache;
    }

    /* JADX WARNING: Missing block: B:14:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void createObjectsCache() {
        synchronized (this.cacheLock) {
            if (this.objectsCache != null) {
                return;
            }
            CacheConfig cacheConfig = CacheConfig.getDefault();
            if (cacheConfig.isOpenObjectCache()) {
                this.objectsCache = new ODMFCache(cacheConfig.getObjectCacheNum());
            }
        }
    }

    void putObjectIntoCache(ManagedObject managedObject) {
        if (CacheConfig.getDefault().isOpenObjectCache()) {
            ManagedObject temp = (ManagedObject) this.objectsCache.get(managedObject.getObjectId());
            if (temp == null || !temp.isDirty()) {
                this.objectsCache.put(managedObject.getObjectId(), managedObject);
                managedObject.setDirty(1);
            }
        }
    }

    ManagedObject getObjectFromCache(ObjectId objectId) {
        if (CacheConfig.getDefault().isOpenObjectCache()) {
            return (ManagedObject) this.objectsCache.get(objectId);
        }
        return null;
    }

    void removeObjectIntoCache(ManagedObject managedObject) {
        if (CacheConfig.getDefault().isOpenObjectCache()) {
            this.objectsCache.remove(managedObject.getObjectId());
        }
    }

    List<Object> executeFetchRequestWithAggregateFunction(FetchRequest fetchRequest, ObjectContext objectContext) {
        if (fetchRequest != null) {
            return getPersistentStore(objectContext).executeFetchRequestWithAggregateFunction(fetchRequest);
        }
        LOG.logE("executeFetchRequestWithAggregateFunction : The input parameter fetchRequest is null.");
        throw new ODMFIllegalArgumentException("The input parameter fetchRequest is null.");
    }

    String getDBVersion(ObjectContext objectContext) {
        return getPersistentStore(objectContext).getCurrentDBVersion();
    }

    protected String getEntityVersion(String tableName, ObjectContext objectContext) {
        return getPersistentStore(objectContext).getCurrentEntityVersion(tableName);
    }

    void setDBVersions(String dbVersion, int dbVersionCode, ObjectContext objectContext) {
        getPersistentStore(objectContext).setNewDBVersions(dbVersion, dbVersionCode);
    }

    protected void setEntityVersions(String tableName, String entityVersion, int entityVersionCode, ObjectContext objectContext) {
        getPersistentStore(objectContext).setNewEntityVersions(tableName, entityVersion, entityVersionCode);
    }

    int getDBVersionCode(ObjectContext objectContext) {
        return getPersistentStore(objectContext).getCurrentDBVersionCode();
    }

    protected int getEntityVersionCode(String tableName, ObjectContext objectContext) {
        return getPersistentStore(objectContext).getCurrentEntityVersionCode(tableName);
    }

    void exportDatabase(String newDBName, byte[] newKey, ObjectContext objectContext) {
        getPersistentStore(objectContext).exportDatabase(newDBName, newKey);
    }

    void resetMetadata(ObjectContext objectContext) {
        getPersistentStore(objectContext).resetMetadata();
    }

    private PersistentStore getPersistentStore(ObjectContext objectContext) {
        if (objectContext == null) {
            LOG.logE("getPersistentStore : The input parameter objectContext is null.");
            throw new ODMFIllegalArgumentException("The input parameter objectContext is null.");
        }
        PersistentStore ps = (PersistentStore) this.mapContextToPersistentStore.get(objectContext);
        if (ps != null) {
            return ps;
        }
        LOG.logE("not found persistentStore.");
        throw new ODMFRuntimeException("The persistentStore correspond to the ObjectContext does not found.This may because you had close this object context.");
    }

    public List<? extends Attribute> getEntityAttributes(ObjectContext objectContext, String entityName) {
        return getPersistentStore(objectContext).getEntityAttributes(entityName);
    }
}
