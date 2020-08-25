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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PersistentStoreCoordinator {
    private static final Singleton<PersistentStoreCoordinator> DEFAULT = new Singleton<PersistentStoreCoordinator>() {
        /* class com.huawei.odmf.core.PersistentStoreCoordinator.AnonymousClass1 */

        @Override // com.huawei.odmf.utils.Singleton
        public PersistentStoreCoordinator create() {
            return new PersistentStoreCoordinator();
        }
    };
    private static final int EXIST = 1;
    private static final int FAIL = -1;
    private static final int SUCCESS = 0;
    private final Object cacheLock;
    private final ConcurrentHashMap<ObjectContext, PersistentStore> contextToStore;
    private NotifyManager notifyManager;
    private ODMFCache<ObjectId, ManagedObject> objectsCache;
    private final Object persistentStoreLock;
    private final ConcurrentHashMap<String, PersistentStore> uriToStore;

    private PersistentStoreCoordinator() {
        this.uriToStore = new ConcurrentHashMap<>();
        this.contextToStore = new ConcurrentHashMap<>();
        this.persistentStoreLock = new Object();
        this.cacheLock = new Object();
        this.objectsCache = null;
        CacheConfig cacheConfig = CacheConfig.getDefault();
        if (cacheConfig.isOpenObjectCache()) {
            this.objectsCache = new ODMFCache<>(cacheConfig.getObjectCacheNum());
        }
        this.notifyManager = new NotifyManager();
    }

    public static PersistentStoreCoordinator getDefault() {
        return DEFAULT.get();
    }

    /* access modifiers changed from: package-private */
    public ConcurrentHashMap<String, PersistentStore> getUriToStore() {
        return this.uriToStore;
    }

    /* access modifiers changed from: package-private */
    public ConcurrentHashMap<ObjectContext, PersistentStore> getContextToStore() {
        return this.contextToStore;
    }

    /* access modifiers changed from: package-private */
    public int createPersistentStore(Uri uri, Configuration config, Context appCtx, String modelPath) {
        return addPersistentStore(uri, config, appCtx, modelPath, null);
    }

    /* access modifiers changed from: package-private */
    public int createEncryptedPersistentStore(Uri uri, Configuration config, Context appCtx, String modelPath, byte[] key) {
        return addPersistentStore(uri, config, appCtx, modelPath, key);
    }

    /* access modifiers changed from: package-private */
    /*  JADX ERROR: StackOverflowError in pass: MarkFinallyVisitor
        java.lang.StackOverflowError
        	at jadx.core.dex.instructions.IfNode.isSame(IfNode.java:122)
        	at jadx.core.dex.visitors.MarkFinallyVisitor.sameInsns(MarkFinallyVisitor.java:451)
        	at jadx.core.dex.visitors.MarkFinallyVisitor.compareBlocks(MarkFinallyVisitor.java:436)
        	at jadx.core.dex.visitors.MarkFinallyVisitor.checkBlocksTree(MarkFinallyVisitor.java:408)
        	at jadx.core.dex.visitors.MarkFinallyVisitor.checkBlocksTree(MarkFinallyVisitor.java:411)
        */
    public int createCrossPersistentStore(Uri r13, Configuration r14, Context r15, Map<Uri, byte[]> r16) {
        /*
            r12 = this;
            if (r13 == 0) goto L_0x0006
            if (r15 == 0) goto L_0x0006
            if (r16 != 0) goto L_0x0013
        L_0x0006:
            java.lang.String r1 = "createCrossPersistentStore : The parameters has null."
            com.huawei.odmf.utils.LOG.logE(r1)
            com.huawei.odmf.exception.ODMFIllegalArgumentException r1 = new com.huawei.odmf.exception.ODMFIllegalArgumentException
            java.lang.String r2 = "The input parameter of uri, appCtx and the uriList has null."
            r1.<init>(r2)
            throw r1
        L_0x0013:
            int r1 = r16.size()
            if (r1 != 0) goto L_0x0026
            java.lang.String r1 = "createCrossPersistentStore : The uriMap contains nothing."
            com.huawei.odmf.utils.LOG.logE(r1)
            com.huawei.odmf.exception.ODMFIllegalArgumentException r1 = new com.huawei.odmf.exception.ODMFIllegalArgumentException
            java.lang.String r2 = "The uriList contains nothing."
            r1.<init>(r2)
            throw r1
        L_0x0026:
            java.util.ArrayList r5 = new java.util.ArrayList
            r5.<init>()
            java.util.ArrayList r6 = new java.util.ArrayList
            r6.<init>()
            java.lang.Object r11 = r12.persistentStoreLock     // Catch:{ all -> 0x006b }
            monitor-enter(r11)     // Catch:{ all -> 0x006b }
            java.util.Set r1 = r16.entrySet()     // Catch:{ all -> 0x0068 }
            java.util.Iterator r2 = r1.iterator()     // Catch:{ all -> 0x0068 }
        L_0x003b:
            boolean r1 = r2.hasNext()     // Catch:{ all -> 0x0068 }
            if (r1 == 0) goto L_0x00a1
            java.lang.Object r7 = r2.next()     // Catch:{ all -> 0x0068 }
            java.util.Map$Entry r7 = (java.util.Map.Entry) r7     // Catch:{ all -> 0x0068 }
            java.util.concurrent.ConcurrentHashMap<java.lang.String, com.huawei.odmf.core.PersistentStore> r3 = r12.uriToStore     // Catch:{ all -> 0x0068 }
            java.lang.Object r1 = r7.getKey()     // Catch:{ all -> 0x0068 }
            android.net.Uri r1 = (android.net.Uri) r1     // Catch:{ all -> 0x0068 }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x0068 }
            java.lang.Object r10 = r3.get(r1)     // Catch:{ all -> 0x0068 }
            com.huawei.odmf.core.PersistentStore r10 = (com.huawei.odmf.core.PersistentStore) r10     // Catch:{ all -> 0x0068 }
            if (r10 != 0) goto L_0x0092
            java.lang.String r1 = "createCrossPersistentStore : Some uri in uriList does not indicates a persistentStore."
            com.huawei.odmf.utils.LOG.logE(r1)     // Catch:{ all -> 0x0068 }
            com.huawei.odmf.exception.ODMFIllegalArgumentException r1 = new com.huawei.odmf.exception.ODMFIllegalArgumentException     // Catch:{ all -> 0x0068 }
            java.lang.String r2 = "Some uri in uriList does not indicates a persistentStore."
            r1.<init>(r2)     // Catch:{ all -> 0x0068 }
            throw r1     // Catch:{ all -> 0x0068 }
        L_0x0068:
            r1 = move-exception
            monitor-exit(r11)     // Catch:{ all -> 0x0068 }
            throw r1
        L_0x006b:
            r1 = move-exception
            java.util.Set r2 = r16.entrySet()
            java.util.Iterator r2 = r2.iterator()
        L_0x0074:
            boolean r3 = r2.hasNext()
            if (r3 == 0) goto L_0x00e7
            java.lang.Object r7 = r2.next()
            java.util.Map$Entry r7 = (java.util.Map.Entry) r7
            java.lang.Object r9 = r7.getValue()
            byte[] r9 = (byte[]) r9
            if (r9 == 0) goto L_0x0074
            r8 = 0
        L_0x0089:
            int r3 = r9.length
            if (r8 >= r3) goto L_0x0074
            r3 = 0
            r9[r8] = r3
            int r8 = r8 + 1
            goto L_0x0089
        L_0x0092:
            java.lang.String r1 = r10.getPath()
            r5.add(r1)
            java.lang.Object r1 = r7.getValue()
            r6.add(r1)
            goto L_0x003b
        L_0x00a1:
            r1 = 301(0x12d, float:4.22E-43)
            r14.setDatabaseType(r1)
            r1 = 401(0x191, float:5.62E-43)
            r14.setStorageMode(r1)
            com.huawei.odmf.core.CrossPersistentStore r0 = new com.huawei.odmf.core.CrossPersistentStore
            r2 = 0
            java.lang.String r3 = r13.toString()
            r1 = r15
            r4 = r14
            r0.<init>(r1, r2, r3, r4, r5, r6)
            java.util.concurrent.ConcurrentHashMap<java.lang.String, com.huawei.odmf.core.PersistentStore> r1 = r12.uriToStore
            java.lang.String r2 = r13.toString()
            r1.put(r2, r0)
            monitor-exit(r11)
            java.util.Set r1 = r16.entrySet()
            java.util.Iterator r1 = r1.iterator()
        L_0x00c9:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L_0x00e8
            java.lang.Object r7 = r1.next()
            java.util.Map$Entry r7 = (java.util.Map.Entry) r7
            java.lang.Object r9 = r7.getValue()
            byte[] r9 = (byte[]) r9
            if (r9 == 0) goto L_0x00c9
            r8 = 0
        L_0x00de:
            int r2 = r9.length
            if (r8 >= r2) goto L_0x00c9
            r2 = 0
            r9[r8] = r2
            int r8 = r8 + 1
            goto L_0x00de
        L_0x00e7:
            throw r1
        L_0x00e8:
            r1 = 0
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.huawei.odmf.core.PersistentStoreCoordinator.createCrossPersistentStore(android.net.Uri, com.huawei.odmf.core.Configuration, android.content.Context, java.util.Map):int");
    }

    private int addPersistentStore(Uri uri, Configuration config, Context appCtx, String modelPath, byte[] key) {
        PersistentStore cacheStore;
        if (uri == null || config == null || appCtx == null) {
            LOG.logE("addPersistentStore : The parameters has null.");
            return -1;
        }
        synchronized (this.persistentStoreLock) {
            if (this.uriToStore.containsKey(uri.toString())) {
                LOG.logE("addPersistentStore : uri is exist, so return directly");
                return 1;
            }
            switch (config.getType()) {
                case 200:
                    if (!isStoreExist(config.getPath())) {
                        if (key == null) {
                            cacheStore = new AndroidSqlPersistentStore(appCtx, modelPath, uri.toString(), config);
                        } else {
                            config.setDatabaseType(Configuration.CONFIGURATION_DATABASE_ODMF);
                            config.setStorageMode(Configuration.CONFIGURATION_STORAGE_MODE_DISK);
                            cacheStore = new EncryptedAndroidSqlPersistentStore(appCtx, modelPath, uri.toString(), config, key);
                        }
                        for (PersistentStore ps : this.uriToStore.values()) {
                            if (ps.getPath() != null && ps.equals(cacheStore)) {
                                LOG.logE("addPersistentStore : database path is exist, so return directly");
                                cacheStore.close();
                                return 1;
                            }
                        }
                        this.uriToStore.put(uri.toString(), cacheStore);
                        break;
                    } else {
                        LOG.logW("addPersistentStore : database path exists already, so return directly");
                        return 1;
                    }
                    break;
                case Configuration.CONFIGURATION_TYPE_PROVIDER:
                    if (isStoreExist(config.getPath())) {
                        LOG.logW("addPersistentStore : database path exists already, so return directly");
                        return 1;
                    }
                    break;
            }
            return 0;
        }
    }

    private boolean isStoreExist(String path) {
        boolean z;
        synchronized (this.persistentStoreLock) {
            Iterator<PersistentStore> it = this.uriToStore.values().iterator();
            while (true) {
                if (!it.hasNext()) {
                    z = false;
                    break;
                }
                PersistentStore psVal = it.next();
                if (psVal.getPath() != null && psVal.getPath().equals(path)) {
                    z = true;
                    break;
                }
            }
        }
        return z;
    }

    /* access modifiers changed from: package-private */
    public int removePersistentStore(Uri uri) {
        int i = -1;
        if (uri == null) {
            LOG.logE("removePersistentStore : The parameter uri is null.");
        } else {
            synchronized (this.persistentStoreLock) {
                PersistentStore ps = this.uriToStore.get(uri.toString());
                if (ps == null) {
                    LOG.logE("the uri " + uri.toString() + "with the corresponding PersistentStore not exist.");
                } else {
                    if (ps.getPath() != null) {
                        Iterator<Map.Entry<ObjectContext, PersistentStore>> it = this.contextToStore.entrySet().iterator();
                        while (true) {
                            if (it.hasNext()) {
                                if (ps.equals(it.next().getValue())) {
                                    LOG.logE("Because the persistentStore is used by other context, so cannot remove the persistentStore currently.");
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                    if (this.uriToStore.containsKey(uri.toString())) {
                        this.uriToStore.remove(uri.toString());
                        ps.close();
                        i = 0;
                    }
                }
            }
        }
        return i;
    }

    /* access modifiers changed from: package-private */
    public int connectPersistentStore(Uri uri, ObjectContext context) {
        int i = -1;
        if (uri == null || context == null) {
            LOG.logE("connectPersistentStore : The parameters has null.");
        } else {
            synchronized (this.persistentStoreLock) {
                PersistentStore ps = this.uriToStore.get(uri.toString());
                if (ps != null) {
                    this.contextToStore.putIfAbsent(context, ps);
                    i = 0;
                } else {
                    LOG.logE("connectPersistentStore : The context can not connect to the persistentStore correspond to the input uri.");
                }
            }
        }
        return i;
    }

    /* access modifiers changed from: package-private */
    public PersistentStore getPersistentStore(Uri uri) {
        if (uri == null) {
            LOG.logE("getPersistentStore : The parameter uri is null.");
            return null;
        }
        synchronized (this.persistentStoreLock) {
            PersistentStore ps = this.uriToStore.get(uri.toString());
            if (ps != null) {
                return ps;
            }
            LOG.logE("getPersistentStore : Can not to get the persistentStore correspond to the input uri.");
            return null;
        }
    }

    /* access modifiers changed from: package-private */
    public int disconnectPersistentStore(Uri uri, ObjectContext context) {
        int i = -1;
        if (uri == null || context == null) {
            LOG.logE("disconnectPersistentStore : The parameters has null.");
        } else {
            synchronized (this.persistentStoreLock) {
                if (this.contextToStore.containsKey(context)) {
                    this.contextToStore.remove(context);
                    removePersistentStore(uri);
                    i = 0;
                }
            }
        }
        return i;
    }

    /* access modifiers changed from: package-private */
    public <T extends ManagedObject> List<T> executeFetchRequest(FetchRequest<T> request, ObjectContext context) {
        if (request == null) {
            LOG.logE("executeFetchRequest : The parameter has null.");
            throw new ODMFIllegalArgumentException("The parameter has null.");
        }
        List<T> resultList = getPersistentStore(context).executeFetchRequest(request, context);
        int size = resultList.size();
        for (int i = 0; i < size; i++) {
            ManagedObject managedObject = resultList.get(i);
            managedObject.setObjectContext(context);
            putObjectIntoCache(managedObject);
        }
        return resultList;
    }

    /* access modifiers changed from: package-private */
    public List<ObjectId> executeFetchRequestGetObjectId(FetchRequest request, ObjectContext context) {
        return getPersistentStore(context).executeFetchRequestGetObjectId(request);
    }

    /* access modifiers changed from: package-private */
    public Cursor executeFetchRequestGetCursor(FetchRequest request, ObjectContext objectContext) {
        return getPersistentStore(objectContext).executeFetchRequestGetCursor(request);
    }

    /* access modifiers changed from: package-private */
    public long getPersistentStoreVersion(ObjectContext context) {
        return getPersistentStore(context).getVersion();
    }

    /* access modifiers changed from: package-private */
    public void save(SaveRequest saveRequest, ObjectContext context) {
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
        List<ObjectContext> notifyTargets = hasListeners(ps.getUriString());
        if (notifyTargets.size() != 0) {
            sendMessageToObjectContext(saveRequest, context, ps.getUriString(), notifyTargets);
        }
    }

    /* access modifiers changed from: package-private */
    public List<ObjectContext> hasListeners(ObjectContext objectContext) {
        return this.notifyManager.hasListeners(getPersistentStore(objectContext).getUriString());
    }

    /* access modifiers changed from: package-private */
    public List<ObjectContext> hasListeners(String uriString) {
        return this.notifyManager.hasListeners(uriString);
    }

    private void sendMessageToObjectContext(SaveRequest saveRequest, ObjectContext context, String psUri, List<ObjectContext> notifyTargets) {
        this.notifyManager.addMessageToQueue(saveRequest, context, psUri, notifyTargets);
    }

    private void successFinishWork(SaveRequest saveRequest) {
        if (CacheConfig.getDefault().isOpenObjectCache()) {
            for (ManagedObject deleteObj : saveRequest.getDeletedObjects()) {
                removeObjectInCache(deleteObj);
            }
            for (ManagedObject insertObj : saveRequest.getInsertedObjects()) {
                putObjectIntoCache(insertObj);
            }
            for (ManagedObject updateObj : saveRequest.getUpdatedObjects()) {
                putObjectIntoCache(updateObj);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public ManagedObject getObjectValues(ObjectId objectId, ObjectContext objectContext) {
        if (objectId == null) {
            LOG.logE("getObjectValues : The parameter objectID is null.");
            return null;
        }
        ManagedObject cacheObject = getObjectFromCache(objectId);
        if (cacheObject != null && (!cacheObject.isDirty() || cacheObject.getLastObjectContext() == objectContext)) {
            return cacheObject;
        }
        PersistentStore ps = getPersistentStore(objectContext);
        if (!objectId.getUriString().equals(ps.getUriString())) {
            objectId.setUriString(ps.getUriString());
        }
        ManagedObject object = ps.getObjectValues(objectId);
        if (object == null) {
            LOG.logE("getObjectValues : Can not get the object correspond to the input objectID.");
            return null;
        }
        object.setState(4);
        object.setObjectContext(objectContext);
        return object;
    }

    /* access modifiers changed from: package-private */
    public void beginTransaction(ObjectContext context) {
        getPersistentStore(context).beginTransaction();
    }

    /* access modifiers changed from: package-private */
    public boolean inTransaction(ObjectContext context) {
        return getPersistentStore(context).inTransaction();
    }

    /* access modifiers changed from: package-private */
    public void commit(ObjectContext context, SaveRequest request) {
        PersistentStore ps = getPersistentStore(context);
        ps.commit();
        if (request != null) {
            List<ObjectContext> notifyTargets = this.notifyManager.hasListeners(ps.getUriString());
            if (notifyTargets.size() != 0) {
                sendMessageToObjectContext(request, context, ps.getUriString(), notifyTargets);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void rollback(ObjectContext context) {
        getPersistentStore(context).rollback();
    }

    /* access modifiers changed from: package-private */
    public void clearTable(ObjectContext objectContext, String entityName) {
        PersistentStore ps = getPersistentStore(objectContext);
        ps.clearTable(entityName);
        List<ObjectContext> notifyTarget = hasListeners(ps.getUriString());
        if (notifyTarget.size() != 0) {
            this.notifyManager.addMessageToQueue(objectContext, entityName, ps.getUriString(), notifyTarget);
        }
    }

    /* access modifiers changed from: package-private */
    public ManagedObject getToOneRelationshipValue(String fieldName, ManagedObject object, ObjectContext objectContext) {
        if (!TextUtils.isEmpty(fieldName) && object != null) {
            return getPersistentStore(objectContext).getToOneRelationshipValue(fieldName, object, objectContext);
        }
        LOG.logE("getToOneRelationshipValue : The parameters has null.");
        throw new ODMFIllegalArgumentException("The parameters has null.");
    }

    /* access modifiers changed from: package-private */
    public List<ManagedObject> getToManyRelationshipValue(String fieldName, ManagedObject object, ObjectContext objectContext) {
        if (!TextUtils.isEmpty(fieldName) && object != null) {
            return getPersistentStore(objectContext).getToManyRelationshipValue(fieldName, object, objectContext);
        }
        LOG.logE("getToManyRelationshipValue : The parameters has null.");
        throw new ODMFIllegalArgumentException("The parameters has null.");
    }

    /* access modifiers changed from: package-private */
    public ManagedObject remoteGetToOneRelationshipValue(String fieldName, ObjectId objectId, ObjectContext objectContext) {
        if (!TextUtils.isEmpty(fieldName) && objectId != null) {
            return getPersistentStore(objectContext).remoteGetToOneRelationshipValue(fieldName, objectId, objectContext);
        }
        LOG.logE("remoteGetToOneRelationshipValue : The parameters has null.");
        throw new ODMFIllegalArgumentException("The parameters has null.");
    }

    /* access modifiers changed from: package-private */
    public List<ManagedObject> remoteGetToManyRelationshipValue(String fieldName, ObjectId objectId, ObjectContext objectContext) {
        if (!TextUtils.isEmpty(fieldName) && objectId != null) {
            return getPersistentStore(objectContext).remoteGetToManyRelationshipValue(fieldName, objectId, objectContext);
        }
        LOG.logE("remoteGetToManyRelationshipValue : The parameters has null.");
        throw new ODMFIllegalArgumentException("The parameters has null.");
    }

    /* access modifiers changed from: package-private */
    public Cursor executeRawQuerySQL(String sql, ObjectContext objectContext) {
        if (!TextUtils.isEmpty(sql)) {
            return getPersistentStore(objectContext).executeRawQuerySQL(sql);
        }
        LOG.logE("executeRawQuerySql : The parameter sql is null.");
        throw new ODMFIllegalArgumentException("The parameter sql is null.");
    }

    /* access modifiers changed from: package-private */
    public void executeRawSQL(String sql, ObjectContext objectContext) {
        if (TextUtils.isEmpty(sql)) {
            LOG.logE("executeRawSql : The parameter sql is null.");
            throw new ODMFIllegalArgumentException("The parameter sql is null.");
        } else {
            getPersistentStore(objectContext).executeRawSQL(sql);
        }
    }

    /* access modifiers changed from: package-private */
    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, ObjectContext objectContext) {
        return getPersistentStore(objectContext).query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    /* access modifiers changed from: package-private */
    public ODMFCache<ObjectId, ManagedObject> getObjectsCache() {
        return this.objectsCache;
    }

    /* access modifiers changed from: package-private */
    public void createObjectsCache() {
        synchronized (this.cacheLock) {
            if (this.objectsCache == null) {
                CacheConfig cacheConfig = CacheConfig.getDefault();
                if (cacheConfig.isOpenObjectCache()) {
                    this.objectsCache = new ODMFCache<>(cacheConfig.getObjectCacheNum());
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void putObjectIntoCache(ManagedObject managedObject) {
        if (CacheConfig.getDefault().isOpenObjectCache()) {
            ManagedObject temp = this.objectsCache.get(managedObject.getObjectId());
            if (temp == null || !temp.isDirty()) {
                this.objectsCache.put(managedObject.getObjectId(), managedObject);
                managedObject.setDirty(1);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public ManagedObject getObjectFromCache(ObjectId objectId) {
        if (CacheConfig.getDefault().isOpenObjectCache()) {
            return this.objectsCache.get(objectId);
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public void removeObjectInCache(ManagedObject managedObject) {
        if (CacheConfig.getDefault().isOpenObjectCache()) {
            this.objectsCache.remove(managedObject.getObjectId());
        }
    }

    /* access modifiers changed from: package-private */
    public List<Object> executeFetchRequestWithAggregateFunction(FetchRequest fetchRequest, ObjectContext objectContext) {
        if (fetchRequest != null) {
            return getPersistentStore(objectContext).executeFetchRequestWithAggregateFunction(fetchRequest);
        }
        LOG.logE("executeFetchRequestWithAggregateFunction : The input parameter fetchRequest is null.");
        throw new ODMFIllegalArgumentException("The input parameter fetchRequest is null.");
    }

    /* access modifiers changed from: package-private */
    public String getDbVersion(ObjectContext objectContext) {
        return getPersistentStore(objectContext).getCurrentDbVersion();
    }

    /* access modifiers changed from: package-private */
    public String getEntityVersion(String tableName, ObjectContext objectContext) {
        return getPersistentStore(objectContext).getCurrentEntityVersion(tableName);
    }

    /* access modifiers changed from: package-private */
    public void setDbVersions(String dbVersion, int dbVersionCode, ObjectContext objectContext) {
        getPersistentStore(objectContext).setNewDbVersions(dbVersion, dbVersionCode);
    }

    /* access modifiers changed from: package-private */
    public void setEntityVersions(String tableName, String entityVersion, int entityVersionCode, ObjectContext objectContext) {
        getPersistentStore(objectContext).setNewEntityVersions(tableName, entityVersion, entityVersionCode);
    }

    /* access modifiers changed from: package-private */
    public int getDbVersionCode(ObjectContext objectContext) {
        return getPersistentStore(objectContext).getCurrentDbVersionCode();
    }

    /* access modifiers changed from: package-private */
    public int getEntityVersionCode(String tableName, ObjectContext objectContext) {
        return getPersistentStore(objectContext).getCurrentEntityVersionCode(tableName);
    }

    /* access modifiers changed from: package-private */
    public void exportDatabase(String newDbName, byte[] newKey, ObjectContext objectContext) {
        getPersistentStore(objectContext).exportDatabase(newDbName, newKey);
    }

    /* access modifiers changed from: package-private */
    public void resetMetadata(ObjectContext objectContext) {
        getPersistentStore(objectContext).resetMetadata();
    }

    private PersistentStore getPersistentStore(ObjectContext objectContext) {
        if (objectContext == null) {
            LOG.logE("getPersistentStore : The input parameter objectContext is null.");
            throw new ODMFIllegalArgumentException("The input parameter objectContext is null.");
        }
        PersistentStore ps = this.contextToStore.get(objectContext);
        if (ps != null) {
            return ps;
        }
        LOG.logE("getPersistentStore : not found persistentStore.");
        throw new ODMFRuntimeException("The persistentStore correspond to the ObjectContext does not found.This may because you had close this object context.");
    }

    /* access modifiers changed from: package-private */
    public List<? extends Attribute> getEntityAttributes(ObjectContext objectContext, String entityName) {
        return getPersistentStore(objectContext).getEntityAttributes(entityName);
    }

    /* access modifiers changed from: package-private */
    public Set<String> getTableInvolvedInSQL(ObjectContext objectContext, String rawSql) {
        return getPersistentStore(objectContext).getTableInvolvedInSQL(rawSql);
    }
}
