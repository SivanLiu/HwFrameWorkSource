package com.huawei.odmf.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.SQLException;
import android.database.StaleDataException;
import android.database.sqlite.SQLiteAccessPermException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import com.huawei.odmf.database.AndroidSQLiteDatabase;
import com.huawei.odmf.database.DataBase;
import com.huawei.odmf.database.ODMFSQLiteDatabase;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFIllegalStateException;
import com.huawei.odmf.exception.ODMFNullPointerException;
import com.huawei.odmf.exception.ODMFRelatedObjectNotFoundException;
import com.huawei.odmf.exception.ODMFRuntimeException;
import com.huawei.odmf.exception.ODMFSQLiteAccessPermException;
import com.huawei.odmf.exception.ODMFSQLiteCantOpenDatabaseException;
import com.huawei.odmf.exception.ODMFSQLiteDatabaseCorruptException;
import com.huawei.odmf.exception.ODMFSQLiteDiskIOException;
import com.huawei.odmf.exception.ODMFSQLiteFullException;
import com.huawei.odmf.exception.ODMFXmlParserException;
import com.huawei.odmf.model.AEntityHelper;
import com.huawei.odmf.model.ARelationship;
import com.huawei.odmf.model.api.Attribute;
import com.huawei.odmf.model.api.Entity;
import com.huawei.odmf.model.api.ObjectModel;
import com.huawei.odmf.model.api.ObjectModelFactory;
import com.huawei.odmf.model.api.Relationship;
import com.huawei.odmf.predicate.FetchRequest;
import com.huawei.odmf.predicate.SaveRequest;
import com.huawei.odmf.store.AndroidDatabaseHelper;
import com.huawei.odmf.store.DatabaseHelper;
import com.huawei.odmf.store.DatabaseTableHelper;
import com.huawei.odmf.store.ODMFDatabaseHelper;
import com.huawei.odmf.user.api.ObjectContext;
import com.huawei.odmf.utils.CursorUtils;
import com.huawei.odmf.utils.LOG;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

class AndroidSqlPersistentStore extends PersistentStore {
    private Context context;
    protected DatabaseHelper databaseHelper;
    protected DataBase db;
    private Map<String, AEntityHelper> helperMap;
    private RelationshipLoader relationshipLoader;
    private final Object statementLock;
    private String uriString;

    AndroidSqlPersistentStore(Context context, String modelPath, String uriString, Configuration configuration) {
        this(context, modelPath, uriString, configuration, null);
    }

    AndroidSqlPersistentStore(Context context, String modelFile, String uriString, Configuration configuration, byte[] key) {
        super(configuration.getPath(), configuration.getDatabaseType(), configuration.getStorageMode(), uriString);
        this.statementLock = new Object();
        try {
            this.model = ObjectModelFactory.parse(context, modelFile);
            this.context = context;
            String databaseName = configuration.getPath();
            if (databaseName == null || databaseName.equals("")) {
                databaseName = this.model.getDatabaseName();
                this.path = databaseName;
            }
            if (configuration.getStorageMode() == Configuration.CONFIGURATION_STORAGE_MODE_MEMORY) {
                databaseName = null;
            }
            init(databaseName, key, configuration.isThrowException(), configuration.isDetectDelete());
            this.uriString = uriString;
        } catch (ODMFIllegalArgumentException | ODMFXmlParserException e) {
            LOG.logE("Create AndroidSqlPersistentStore failed : parser objectModel failed.");
            throw new ODMFRuntimeException("Xml parser failed : " + e.getMessage());
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:24:0x0087=Splitter:B:24:0x0087, B:42:0x00ca=Splitter:B:42:0x00ca} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void init(String databasePath, byte[] key, boolean throwException, boolean detectDelete) {
        SQLException e;
        int i;
        try {
            if (getDatabaseType() == Configuration.CONFIGURATION_DATABASE_ANDROID) {
                this.databaseHelper = new AndroidDatabaseHelper(this.context, databasePath, getModel());
                this.db = new AndroidSQLiteDatabase(((AndroidDatabaseHelper) this.databaseHelper).getWritableDatabase());
            } else if (getDatabaseType() == Configuration.CONFIGURATION_DATABASE_ODMF) {
                this.databaseHelper = new ODMFDatabaseHelper(this.context, databasePath, getModel(), throwException, detectDelete);
                if (key != null && key.length > 0) {
                    this.databaseHelper.setDatabaseEncrypted(key);
                }
                this.db = new ODMFSQLiteDatabase(((ODMFDatabaseHelper) this.databaseHelper).getWritableDatabase());
            } else {
                LOG.logE("Init database failed : incorrect configuration of database.");
                throw new ODMFRuntimeException("The configuration of database is wrong.");
            }
            loadMetadata();
            this.helperMap = new ConcurrentHashMap();
            initHelper(getModel());
            this.relationshipLoader = new RelationshipLoader(this.db, getModel(), this.helperMap);
            if (key != null && key.length > 0) {
                for (i = 0; i < key.length; i++) {
                    key[i] = (byte) 0;
                }
            }
        } catch (SQLiteDatabaseCorruptException e2) {
            e = e2;
            LOG.logE("Init database failed : A sqliteDatabase corrupt exception occurred when initializing database.");
            throw new ODMFSQLiteDatabaseCorruptException("Init database failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteDatabaseCorruptException e3) {
            e = e3;
            LOG.logE("Init database failed : A sqliteDatabase corrupt exception occurred when initializing database.");
            throw new ODMFSQLiteDatabaseCorruptException("Init database failed : " + e.getMessage(), e);
        } catch (SQLiteDiskIOException e22) {
            e = e22;
            LOG.logE("Init database failed : A disk io exception occurred when initializing database.");
            throw new ODMFSQLiteDiskIOException("Init database failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteDiskIOException e4) {
            e = e4;
            LOG.logE("Init database failed : A disk io exception occurred when initializing database.");
            throw new ODMFSQLiteDiskIOException("Init database failed : " + e.getMessage(), e);
        } catch (SQLiteFullException e222) {
            e = e222;
            LOG.logE("Init database failed : A disk full exception occurred when initializing database.");
            throw new ODMFSQLiteFullException("Init database failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteFullException e5) {
            e = e5;
            LOG.logE("Init database failed : A disk full exception occurred when initializing database.");
            throw new ODMFSQLiteFullException("Init database failed : " + e.getMessage(), e);
        } catch (SQLiteAccessPermException e2222) {
            e = e2222;
            LOG.logE("Init database failed : An access permission exception occurred when initializing database.");
            throw new ODMFSQLiteAccessPermException("Init database failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteAccessPermException e6) {
            e = e6;
            LOG.logE("Init database failed : An access permission exception occurred when initializing database.");
            throw new ODMFSQLiteAccessPermException("Init database failed : " + e.getMessage(), e);
        } catch (SQLiteCantOpenDatabaseException e22222) {
            e = e22222;
            LOG.logE("Init database failed : An cant open exception occurred when initializing database.");
            throw new ODMFSQLiteCantOpenDatabaseException("Init database failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteCantOpenDatabaseException e7) {
            e = e7;
            LOG.logE("Init database failed : An cant open exception occurred when initializing database.");
            throw new ODMFSQLiteCantOpenDatabaseException("Init database failed : " + e.getMessage(), e);
        } catch (SQLiteException e222222) {
            e = e222222;
            LOG.logE("Init database failed : A SQLite exception occurred when initializing database.");
            throw new ODMFRuntimeException("Init database failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteException e8) {
            e = e8;
            LOG.logE("Init database failed : A SQLite exception occurred when initializing database.");
            throw new ODMFRuntimeException("Init database failed : " + e.getMessage(), e);
        } catch (Exception e9) {
            LOG.logE("Init database failed : A unknown exception occurred when initializing database.");
            throw new ODMFRuntimeException("Init database failed : " + e9.getMessage(), e9);
        } catch (Throwable th) {
            if (key != null && key.length > 0) {
                for (i = 0; i < key.length; i++) {
                    key[i] = (byte) 0;
                }
            }
        }
    }

    private void loadMetadata() {
        setMetadata("databaseVersion", this.databaseHelper.getDatabaseVersion(this.db));
        setMetadata("databaseVersionCode", Integer.valueOf(this.databaseHelper.getDatabaseVersionCode(this.db)));
        if (getModel() == null || getModel().getEntities() == null) {
            LOG.logE("Execute loadMetadata failed : the objectModel or the entities is null.");
            throw new ODMFRuntimeException("Execute loadMetadata failed : the objectModel or the entities is null.");
        }
        for (String entityName : getModel().getEntities().keySet()) {
            String tableName = ((Entity) getModel().getEntities().get(entityName)).getTableName();
            setMetadata(tableName + "_version", this.databaseHelper.getEntityVersion(this.db, tableName));
            setMetadata(tableName + "_versionCode", Integer.valueOf(this.databaseHelper.getEntityVersionCode(this.db, tableName)));
        }
    }

    private void initHelper(ObjectModel model) {
        if (model == null) {
            LOG.logE("Execute initHelper failed : the objectModel is null.");
            throw new ODMFIllegalArgumentException("Execute initHelper failed : the model is null");
        }
        Map<String, ? extends Entity> entities = model.getEntities();
        if (entities == null) {
            LOG.logE("Execute initHelper failed : the entities is null.");
            throw new ODMFIllegalStateException("Execute initHelper failed : the entities in model is null");
        }
        for (Entry<String, ? extends Entity> entry : entities.entrySet()) {
            String key = (String) entry.getKey();
            try {
                AEntityHelper helper = (AEntityHelper) Class.forName(key + "Helper").getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
                helper.setEntity((Entity) entry.getValue());
                this.helperMap.put(key, helper);
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                LOG.logE("Execute initHelper failed : An exception occurred when use reflection to get helper class.");
                throw new ODMFRuntimeException("Execute initHelper failed : " + e.getMessage());
            }
        }
    }

    private void overwriteJoinClause(FetchRequest fetchRequest) {
        int i;
        String clause = fetchRequest.getJoinClause().toString();
        StringBuilder stringBuilder = new StringBuilder("");
        String[] clauseSplit = clause.split("\\s+");
        for (i = 0; i < clauseSplit.length; i++) {
            if (clauseSplit[i].contains("=")) {
                String[] entitySplit = clauseSplit[i].split("=");
                String entityNameNative = entitySplit[0];
                Entity entityNative = getModel().getEntity(entityNameNative);
                String tableNameNative = entityNative.getTableName();
                String entityNameRelated = entitySplit[1];
                Entity entityRelated = getModel().getEntity(entityNameRelated);
                String tableNameRelated = entityRelated.getTableName();
                for (ARelationship aRelationship : getModel().getEntity(entityNameNative).getRelationships()) {
                    if (aRelationship.getRelatedEntity().getEntityName().equals(entityNameRelated)) {
                        switch (aRelationship.getRelationShipType()) {
                            case 0:
                                String midTableName = DatabaseTableHelper.getManyToManyMidTableName(aRelationship);
                                clauseSplit[i - 2] = midTableName;
                                clauseSplit[i] = tableNameNative + "." + DatabaseQueryService.getRowidColumnName() + " = " + midTableName + "." + DatabaseTableHelper.getRelationshipColumnName(entityNative) + " INNER JOIN " + tableNameRelated + " ON " + midTableName + "." + DatabaseTableHelper.getRelationshipColumnName(entityRelated) + " = " + tableNameRelated + "." + DatabaseQueryService.getRowidColumnName();
                                break;
                            case 2:
                                clauseSplit[i] = tableNameNative + "." + aRelationship.getFieldName() + " = " + tableNameRelated + "." + aRelationship.getRelatedColumnName();
                                break;
                            case 4:
                                clauseSplit[i] = tableNameNative + "." + aRelationship.getInverseRelationship().getRelatedColumnName() + " = " + tableNameRelated + "." + aRelationship.getInverseRelationship().getFieldName();
                                break;
                            case 6:
                                if (!aRelationship.isMajor()) {
                                    clauseSplit[i] = tableNameNative + "." + aRelationship.getInverseRelationship().getRelatedColumnName() + " = " + tableNameRelated + "." + aRelationship.getInverseRelationship().getFieldName();
                                    break;
                                } else {
                                    clauseSplit[i] = tableNameNative + "." + aRelationship.getFieldName() + " = " + tableNameRelated + "." + aRelationship.getRelatedColumnName();
                                    break;
                                }
                            default:
                                LOG.logE("Execute overwriteJoinClause failed : the relation type is wrong.");
                                throw new ODMFIllegalStateException("Execute overwriteJoinClause failed : No such relationship type.");
                        }
                    }
                }
                continue;
            }
        }
        for (String append : clauseSplit) {
            stringBuilder.append(" ");
            stringBuilder.append(append);
        }
        fetchRequest.setJoinClause(stringBuilder);
    }

    protected <T extends ManagedObject> List<T> executeFetchRequest(FetchRequest request, ObjectContext context) {
        SQLException e;
        if (request == null) {
            LOG.logE("Execute FetchRequest failed : the relation type is wrong.");
            throw new ODMFIllegalArgumentException("Execute FetchRequest failed : The parameter request is null.");
        }
        Cursor cursor = null;
        Cursor fastCursor = null;
        CursorWindow window = null;
        List<T> results = new ArrayList();
        if (this.model.getEntity(request.getEntityName()) == null) {
            LOG.logE("Execute FetchRequest failed : The entity which the entityName specified is not in the model.");
            throw new ODMFIllegalArgumentException("Execute FetchRequest failed : The entity which the entityName specified is not in the model.");
        }
        AEntityHelper entityHelper = getHelper(request.getEntityName());
        Entity entity = entityHelper.getEntity();
        String tableName = entity.getTableName();
        overwriteJoinClause(request);
        try {
            cursor = DatabaseQueryService.query(this.db, tableName + request.getJoinClause().toString(), request);
            boolean useFastCursor = false;
            if (getDatabaseType() == Configuration.CONFIGURATION_DATABASE_ANDROID) {
                int count = cursor.getCount();
                if (count == 0) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (window != null) {
                        window.close();
                    }
                    return results;
                } else if (cursor instanceof CrossProcessCursor) {
                    window = ((CrossProcessCursor) cursor).getWindow();
                    if (window != null) {
                        if (window.getNumRows() == count) {
                            useFastCursor = true;
                            fastCursor = new FastCursor(window);
                        } else {
                            LOG.logD("Window vs. result size: " + window.getNumRows() + "/" + count);
                        }
                    }
                }
            }
            if (cursor.moveToFirst()) {
                Cursor cursor2;
                if (useFastCursor) {
                    cursor2 = fastCursor;
                } else {
                    cursor2 = cursor;
                }
                loadManagedObjectFromCursor(entityHelper, entity, cursor2, results, context);
                if (cursor != null) {
                    cursor.close();
                }
                if (window != null) {
                    window.close();
                }
            } else {
                if (cursor != null) {
                    cursor.close();
                }
                if (window != null) {
                    window.close();
                }
            }
            return results;
        } catch (StaleDataException e2) {
            LOG.logE("Execute FetchRequest failed : A StaleDataException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e2.getMessage(), e2);
        } catch (IllegalStateException e3) {
            LOG.logE("Execute FetchRequest failed : A IllegalStateException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e3.getMessage(), e3);
        } catch (IllegalArgumentException e4) {
            LOG.logE("Execute FetchRequest failed : A IllegalArgumentException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e4.getMessage(), e4);
        } catch (SQLiteDatabaseCorruptException e5) {
            e = e5;
            LOG.logE("Execute FetchRequest failed : A SQLiteDatabaseCorruptException occurred when query");
            throw new ODMFSQLiteDatabaseCorruptException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteDatabaseCorruptException e6) {
            e = e6;
            LOG.logE("Execute FetchRequest failed : A SQLiteDatabaseCorruptException occurred when query");
            throw new ODMFSQLiteDatabaseCorruptException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (SQLiteDiskIOException e52) {
            e = e52;
            LOG.logE("Execute FetchRequest failed : A SQLiteDiskIOException occurred when query");
            throw new ODMFSQLiteDiskIOException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteDiskIOException e7) {
            e = e7;
            LOG.logE("Execute FetchRequest failed : A SQLiteDiskIOException occurred when query");
            throw new ODMFSQLiteDiskIOException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (SQLiteFullException e522) {
            e = e522;
            LOG.logE("Execute FetchRequest failed : A SQLiteFullException occurred when query");
            throw new ODMFSQLiteFullException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteFullException e8) {
            e = e8;
            LOG.logE("Execute FetchRequest failed : A SQLiteFullException occurred when query");
            throw new ODMFSQLiteFullException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (SQLiteAccessPermException e5222) {
            e = e5222;
            LOG.logE("Execute FetchRequest failed : A SQLiteAccessPermException occurred when query");
            throw new ODMFSQLiteAccessPermException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteAccessPermException e9) {
            e = e9;
            LOG.logE("Execute FetchRequest failed : A SQLiteAccessPermException occurred when query");
            throw new ODMFSQLiteAccessPermException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (SQLiteCantOpenDatabaseException e52222) {
            e = e52222;
            LOG.logE("Execute FetchRequest failed : A SQLiteCantOpenDatabaseException occurred when query");
            throw new ODMFSQLiteCantOpenDatabaseException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteCantOpenDatabaseException e10) {
            e = e10;
            LOG.logE("Execute FetchRequest failed : A SQLiteCantOpenDatabaseException occurred when query");
            throw new ODMFSQLiteCantOpenDatabaseException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (SQLiteException e522222) {
            e = e522222;
            LOG.logE("Execute FetchRequest failed : A SQLiteException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteException e11) {
            e = e11;
            LOG.logE("Execute FetchRequest failed : A SQLiteException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (Exception e12) {
            LOG.logE("Execute FetchRequest failed : A unknown exception occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e12.getMessage(), e12);
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            if (window != null) {
                window.close();
            }
        }
    }

    private <T extends ManagedObject> void loadManagedObjectFromCursor(AEntityHelper helper, Entity entity, Cursor cursor, List<T> results, ObjectContext context) {
        boolean existCache = CacheConfig.getDefault().isOpenObjectCache();
        do {
            long id = cursor.getLong(DatabaseQueryService.getOdmfRowidIndex());
            if (existCache) {
                ManagedObject cacheObject = (ManagedObject) PersistentStoreCoordinator.getDefault().getObjectsCache().get(new AObjectId(entity.getEntityName(), Long.valueOf(id), getUriString()));
                if (cacheObject != null) {
                    if (!cacheObject.isDirty()) {
                        results.add(cacheObject);
                    } else if (cacheObject.getLastObjectContext() == context) {
                        results.add(cacheObject);
                    }
                }
            }
            ManagedObject obj = (ManagedObject) helper.readObject(cursor, 0);
            obj.setRowId(Long.valueOf(id));
            obj.setState(4);
            obj.setObjectContext(context);
            obj.setUriString(this.uriString);
            results.add(obj);
        } while (cursor.moveToNext());
    }

    private AEntityHelper getHelper(String entityName) {
        return (AEntityHelper) this.helperMap.get(entityName);
    }

    protected List<ObjectId> executeFetchRequestGetObjectID(FetchRequest request) {
        if (request == null) {
            LOG.logE("Execute FetchRequestGetObjectID failed : The parameter request is null.");
            throw new ODMFIllegalArgumentException("The parameter request is null.");
        }
        List<ObjectId> results = new ArrayList();
        Entity entity = getModel().getEntity(request.getEntityName());
        if (entity == null) {
            LOG.logE("Execute FetchRequestGetObjectID failed : he entity which the entityName specified is not in the model.");
            throw new ODMFIllegalArgumentException("Execute FetchRequestGetObjectID failed : The entity which the entityName specified is not in the model");
        }
        String tableName = entity.getTableName();
        overwriteJoinClause(request);
        Cursor cursor = null;
        try {
            cursor = DatabaseQueryService.queryRowID(this.db, tableName + request.getJoinClause().toString(), request);
            while (cursor.moveToNext()) {
                results.add(createObjectID(entity, Long.valueOf(cursor.getLong(DatabaseQueryService.getOdmfRowidIndex()))));
            }
            if (cursor != null) {
                cursor.close();
            }
            return results;
        } catch (SQLiteException e) {
            LOG.logE("Execute FetchRequestGetObjectID failed : A SQLiteException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequestGetObjectID failed : " + e.getMessage());
        } catch (StaleDataException e2) {
            LOG.logE("Execute FetchRequestGetObjectID failed : A StaleDataException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequestGetObjectID failed : " + e2.getMessage());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:24:0x009e=Splitter:B:24:0x009e, B:33:0x00de=Splitter:B:33:0x00de} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<Object> executeFetchRequestWithAggregateFunction(FetchRequest request) {
        SQLException e;
        if (request == null) {
            LOG.logE("Execute FetchRequestWithAggregateFunction failed : The parameter request is null.");
            throw new ODMFIllegalArgumentException("The parameter request is null.");
        } else if (this.model.getEntity(request.getEntityName()) == null) {
            LOG.logE("Execute FetchRequestWithAggregateFunction failed : The entity which the entityName specified is not in the model.");
            throw new ODMFIllegalArgumentException("The entity which the entityName specified is not in the model.");
        } else {
            Entity entity = getHelper(request.getEntityName()).getEntity();
            String tableName = entity.getTableName();
            overwriteJoinClause(request);
            tableName = tableName + request.getJoinClause().toString();
            String[] columns = request.getColumns();
            int[] aggregateOp = request.getAggregateOp();
            if (columns == null || aggregateOp == null) {
                LOG.logE("Execute FetchRequestWithAggregateFunction failed : the querying columns is null or aggregateOp is null.");
                throw new ODMFRuntimeException("Execute FetchRequestWithAggregateFunction failed : the querying columns is null or aggregateOp is null.");
            }
            Cursor cursor = null;
            List<Object> result = new ArrayList();
            try {
                cursor = DatabaseQueryService.queryWithAggregateFunction(this.db, tableName, request);
                cursor.moveToFirst();
                for (int i = 0; i < columns.length; i++) {
                    result.add(CursorUtils.extractAggregateResult(cursor.getString(i), aggregateOp[i], entity.getAttribute(columns[i])));
                }
                if (cursor != null) {
                    cursor.close();
                }
                return result;
            } catch (SQLiteDatabaseCorruptException e2) {
                e = e2;
                LOG.logE("Execute fetchRequest with aggregateFunction failed : " + e.getMessage());
                throw new ODMFSQLiteDatabaseCorruptException("End Transaction failed : " + e.getMessage(), e);
            } catch (com.huawei.hwsqlite.SQLiteDatabaseCorruptException e3) {
                e = e3;
                LOG.logE("Execute fetchRequest with aggregateFunction failed : " + e.getMessage());
                throw new ODMFSQLiteDatabaseCorruptException("End Transaction failed : " + e.getMessage(), e);
            } catch (SQLiteDiskIOException e22) {
                e = e22;
                LOG.logE("End Transaction failed : " + e.getMessage());
                throw new ODMFSQLiteDiskIOException("End Transaction failed : " + e.getMessage(), e);
            } catch (com.huawei.hwsqlite.SQLiteDiskIOException e4) {
                e = e4;
                LOG.logE("End Transaction failed : " + e.getMessage());
                throw new ODMFSQLiteDiskIOException("End Transaction failed : " + e.getMessage(), e);
            } catch (SQLiteException e222) {
                e = e222;
                LOG.logE("Execute fetchRequest With aggregateFunction failed : A SQLiteException occurred when query.");
                throw new ODMFRuntimeException("Execute fetchRequest With aggregateFunction failed : " + e.getMessage(), e);
            } catch (com.huawei.hwsqlite.SQLiteException e5) {
                e = e5;
                LOG.logE("Execute fetchRequest With aggregateFunction failed : A SQLiteException occurred when query.");
                throw new ODMFRuntimeException("Execute fetchRequest With aggregateFunction failed : " + e.getMessage(), e);
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    protected Cursor executeFetchRequestGetCursor(FetchRequest request) {
        if (request == null) {
            LOG.logE("Execute FetchRequestGetCursor failed : The parameter request is null.");
            throw new ODMFIllegalArgumentException("Execute FetchRequestGetCursor failed : The parameter request is null");
        }
        Entity entity = getModel().getEntity(request.getEntityName());
        if (entity == null) {
            LOG.logE("Execute FetchRequestGetCursor failed : The entity which the entityName specified is not in the model.");
            throw new ODMFIllegalArgumentException("Execute FetchRequestGetCursor failed : The entity which the entityName specified is not in the model.");
        }
        String tableName = entity.getTableName();
        overwriteJoinClause(request);
        try {
            return DatabaseQueryService.commonquery(this.db, tableName + request.getJoinClause().toString(), request);
        } catch (StaleDataException e) {
            LOG.logE("Execute FetchRequest failed : A StaleDataException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e.getMessage(), e);
        } catch (IllegalStateException e2) {
            LOG.logE("Execute FetchRequest failed : A IllegalStateException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e2.getMessage(), e2);
        } catch (IllegalArgumentException e3) {
            LOG.logE("Execute FetchRequest failed : A IllegalArgumentException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e3.getMessage(), e3);
        } catch (SQLiteDatabaseCorruptException | com.huawei.hwsqlite.SQLiteDatabaseCorruptException e4) {
            LOG.logE("Execute FetchRequest failed : A SQLiteDatabaseCorruptException occurred when query");
            throw new ODMFSQLiteDatabaseCorruptException("Execute FetchRequest failed : " + e4.getMessage(), e4);
        } catch (SQLiteDiskIOException | com.huawei.hwsqlite.SQLiteDiskIOException e42) {
            LOG.logE("Execute FetchRequest failed : A SQLiteDiskIOException occurred when query");
            throw new ODMFSQLiteDiskIOException("Execute FetchRequest failed : " + e42.getMessage(), e42);
        } catch (SQLiteFullException | com.huawei.hwsqlite.SQLiteFullException e422) {
            LOG.logE("Execute FetchRequest failed : A SQLiteFullException occurred when query");
            throw new ODMFSQLiteFullException("Execute FetchRequest failed : " + e422.getMessage(), e422);
        } catch (SQLiteAccessPermException | com.huawei.hwsqlite.SQLiteAccessPermException e4222) {
            LOG.logE("Execute FetchRequest failed : A SQLiteAccessPermException occurred when query");
            throw new ODMFSQLiteAccessPermException("Execute FetchRequest failed : " + e4222.getMessage(), e4222);
        } catch (SQLiteCantOpenDatabaseException | com.huawei.hwsqlite.SQLiteCantOpenDatabaseException e42222) {
            LOG.logE("Execute FetchRequest failed : A SQLiteCantOpenDatabaseException occurred when query");
            throw new ODMFSQLiteCantOpenDatabaseException("Execute FetchRequest failed : " + e42222.getMessage(), e42222);
        } catch (SQLiteException | com.huawei.hwsqlite.SQLiteException e422222) {
            LOG.logE("Execute FetchRequest failed : A SQLiteException occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e422222.getMessage(), e422222);
        } catch (Exception e5) {
            LOG.logE("Execute FetchRequest failed : A unknown exception occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e5.getMessage(), e5);
        }
    }

    protected void executeSaveRequestWithTransaction(SaveRequest request) {
        this.db.beginTransaction();
        try {
            executeSaveRequest(request);
            this.db.setTransactionSuccessful();
            increaseVersion();
        } finally {
            this.db.endTransaction();
        }
    }

    private void executeInsert(List<ManagedObject> insertList) {
        int insertSize = insertList.size();
        int i = 0;
        while (i < insertSize) {
            ManagedObject object = (ManagedObject) insertList.get(i);
            AEntityHelper entityHelper = ((AManagedObject) object).getHelper();
            if (checkEntityHelper(entityHelper)) {
                Entity entity = entityHelper.getEntity();
                Statement statement = entity.getStatements().getInsertStatement(this.db, entity.getTableName(), entity.getAttributes());
                synchronized (this.statementLock) {
                    entityHelper.bindValue(statement, object);
                    long lastRowID = statement.executeInsert();
                    object.setRowId(Long.valueOf(lastRowID));
                    object.setUriString(this.uriString);
                    if (entity.isKeyAutoIncrement()) {
                        entityHelper.setPrimaryKeyValue(object, lastRowID);
                    }
                    statement.clearBindings();
                }
                object.setState(4);
                i++;
            } else {
                throw new ODMFIllegalArgumentException("Execute SaveRequest failed : The object is incompatible with the ObjectContext.");
            }
        }
        this.relationshipLoader.handleRelationship(insertList);
    }

    private boolean checkEntityHelper(AEntityHelper entityHelper) {
        Entity entity = entityHelper.getEntity();
        if (entity.getModel() == this.model) {
            return true;
        }
        entity = getModel().getEntity(entity.getEntityName());
        if (entity == null) {
            return false;
        }
        entityHelper.setEntity(entity);
        return true;
    }

    private void executeUpdate(List<ManagedObject> updatedList) {
        int updateSize = updatedList.size();
        int i = 0;
        while (i < updateSize) {
            ManagedObject object = (ManagedObject) updatedList.get(i);
            long id = object.getRowId().longValue();
            AEntityHelper entityHelper = ((AManagedObject) object).getHelper();
            if (checkEntityHelper(entityHelper)) {
                Entity entity = entityHelper.getEntity();
                Statement statement = entity.getStatements().getUpdateStatement(this.db, entity.getTableName(), entity.getAttributes());
                synchronized (this.statementLock) {
                    entityHelper.bindValue(statement, object);
                    statement.bindLong(entity.getAttributes().size() + 1, id);
                    statement.execute();
                    statement.clearBindings();
                }
                object.setState(4);
                i++;
            } else {
                throw new ODMFIllegalArgumentException("Execute SaveRequest failed : The object is incompatible with the ObjectContext.");
            }
        }
        this.relationshipLoader.handleRelationship(updatedList);
    }

    private void executeDelete(List<ManagedObject> deleteList) {
        int deleteSize = deleteList.size();
        int i = 0;
        while (i < deleteSize) {
            ManagedObject object = (ManagedObject) deleteList.get(i);
            AEntityHelper entityHelper = ((AManagedObject) object).getHelper();
            if (checkEntityHelper(entityHelper)) {
                Entity entity = entityHelper.getEntity();
                List<ManagedObject> cascadeDeleteObjects = null;
                if (!entity.getRelationships().isEmpty()) {
                    cascadeDeleteObjects = new ArrayList();
                    this.relationshipLoader.handleCascadeDelete(object, cascadeDeleteObjects);
                }
                Statement statement = entity.getStatements().getDeleteStatement(this.db, entity.getTableName(), entity.getAttributes());
                synchronized (this.statementLock) {
                    statement.bindLong(1, object.getRowId().longValue());
                    statement.execute();
                    statement.clearBindings();
                }
                if (!(cascadeDeleteObjects == null || cascadeDeleteObjects.isEmpty())) {
                    deleteObjectFromSet(cascadeDeleteObjects, object);
                    deleteObjectSetFromSet(cascadeDeleteObjects, deleteList);
                    while (!cascadeDeleteObjects.isEmpty()) {
                        List<ManagedObject> moreObjectNeedDelete = new ArrayList(cascadeDeleteObjects);
                        moreObjectNeedDelete.addAll(deleteList);
                        for (ManagedObject obj : cascadeDeleteObjects) {
                            long objId = obj.getRowId().longValue();
                            Entity objEntity = ((AManagedObject) obj).getHelper().getEntity();
                            this.relationshipLoader.handleCascadeDelete(obj, moreObjectNeedDelete);
                            statement = objEntity.getStatements().getDeleteStatement(this.db, objEntity.getTableName(), objEntity.getAttributes());
                            synchronized (this.statementLock) {
                                statement.bindLong(1, objId);
                                statement.executeUpdateDelete();
                                statement.clearBindings();
                            }
                        }
                        deleteObjectSetFromSet(moreObjectNeedDelete, cascadeDeleteObjects);
                        deleteObjectSetFromSet(moreObjectNeedDelete, deleteList);
                        cascadeDeleteObjects.clear();
                        cascadeDeleteObjects.addAll(moreObjectNeedDelete);
                    }
                    continue;
                }
                object.setState(0);
                i++;
            } else {
                throw new ODMFIllegalArgumentException("Execute SaveRequest failed : The object is incompatible with the ObjectContext.");
            }
        }
    }

    protected void executeSaveRequest(SaveRequest request) {
        if (request == null) {
            LOG.logE("Execute SaveRequest failed : The parameter request is null");
            throw new ODMFIllegalArgumentException("The parameter request is null");
        }
        try {
            executeInsert(request.getInsertedObjects());
            executeUpdate(request.getUpdatedObjects());
            executeDelete(request.getDeletedObjects());
        } catch (NullPointerException e) {
            LOG.logE("Execute SaveRequest failed : A NullPointerException occurred when save");
            throw new ODMFNullPointerException("Execute SaveRequest failed : A NullPointerException occurred when save, the object in a relationship may not inserted yet, or the entityName in the class not match it in the xml,or some condition else.", e);
        } catch (SQLiteDatabaseCorruptException | com.huawei.hwsqlite.SQLiteDatabaseCorruptException e2) {
            LOG.logE("Execute SaveRequest failed : A SQLiteDatabaseCorruptException occurred when save");
            throw new ODMFSQLiteDatabaseCorruptException("Execute SaveRequest failed : " + e2.getMessage(), e2);
        } catch (SQLiteDiskIOException | com.huawei.hwsqlite.SQLiteDiskIOException e22) {
            LOG.logE("Execute SaveRequest failed : A SQLiteDiskIOException occurred when save");
            throw new ODMFSQLiteDiskIOException("Execute SaveRequest failed : " + e22.getMessage(), e22);
        } catch (SQLiteFullException | com.huawei.hwsqlite.SQLiteFullException e222) {
            LOG.logE("Execute SaveRequest failed : A SQLiteFullException occurred when save");
            throw new ODMFSQLiteFullException("Execute SaveRequest failed : " + e222.getMessage(), e222);
        } catch (SQLiteAccessPermException | com.huawei.hwsqlite.SQLiteAccessPermException e2222) {
            LOG.logE("Execute SaveRequest failed : A SQLiteAccessPermException occurred when save");
            throw new ODMFSQLiteAccessPermException("Execute SaveRequest failed : " + e2222.getMessage(), e2222);
        } catch (SQLiteCantOpenDatabaseException | com.huawei.hwsqlite.SQLiteCantOpenDatabaseException e22222) {
            LOG.logE("Execute SaveRequest failed : A SQLiteCantOpenDatabaseException occurred when save");
            throw new ODMFSQLiteCantOpenDatabaseException("Execute SaveRequest failed : " + e22222.getMessage(), e22222);
        } catch (SQLiteException | com.huawei.hwsqlite.SQLiteException | IllegalArgumentException e3) {
            LOG.logE("Execute SaveRequest failed : A SQLiteException occurred when save");
            throw new ODMFRuntimeException("Execute SaveRequest failed : " + e3.getMessage());
        } catch (Exception e4) {
            LOG.logE("Execute FetchRequest failed : A unknown exception occurred when query");
            throw new ODMFRuntimeException("Execute FetchRequest failed : " + e4.getMessage(), e4);
        }
    }

    private void deleteObjectSetFromSet(List<ManagedObject> deleteFrom, List<ManagedObject> deletes) {
        int size = deletes.size();
        for (int i = 0; i < size; i++) {
            deleteObjectFromSet(deleteFrom, (ManagedObject) deletes.get(i));
        }
    }

    private void deleteObjectFromSet(List<ManagedObject> deleteFrom, ManagedObject delete) {
        int size = deleteFrom.size();
        for (int i = 0; i < size; i++) {
            ManagedObject manageObject = (ManagedObject) deleteFrom.get(i);
            if (manageObject.getRowId().equals(delete.getRowId()) && manageObject.getEntityName().equals(delete.getEntityName())) {
                deleteFrom.remove(manageObject);
                return;
            }
        }
    }

    protected void beginTransaction() {
        try {
            if (this.db.inTransaction()) {
                LOG.logE("Execute beginTransaction failed : The database " + this.db.getPath() + " is already in a transaction.");
                throw new ODMFIllegalStateException("The database " + this.db.getPath() + " is already in a transaction.");
            } else {
                this.db.beginTransaction();
            }
        } catch (IllegalStateException e) {
            LOG.logE("Execute beginTransaction failed :" + e.getMessage());
            throw new ODMFIllegalStateException("execute beginTransaction failed." + e.getMessage(), e);
        }
    }

    protected boolean inTransaction() {
        try {
            return this.db.inTransaction();
        } catch (IllegalStateException e) {
            LOG.logE("Check inTransaction failed :" + e.getMessage());
            throw new ODMFIllegalStateException("Check inTransaction failed." + e.getMessage(), e);
        }
    }

    protected void rollback() {
        try {
            if (this.db.inTransaction()) {
                this.db.endTransaction();
            } else {
                LOG.logE("Execute rollback failed : The database " + this.db.getPath() + " is not in a transaction.");
                throw new ODMFIllegalStateException("Execute rollback failed : The database " + this.db.getPath() + " is not in a transaction.");
            }
        } catch (IllegalStateException e) {
            LOG.logE("Execute rollback transaction failed : " + e.getMessage());
            throw new ODMFIllegalStateException("Execute rollback transaction failed." + e.getMessage(), e);
        }
    }

    protected void commit() {
        try {
            if (this.db.inTransaction()) {
                this.db.setTransactionSuccessful();
                increaseVersion();
                this.db.endTransaction();
                return;
            }
            LOG.logE("Execute commit failed : The database " + this.db.getPath() + " is not in a transaction.");
            throw new ODMFIllegalStateException("Execute commit failed : The database " + this.db.getPath() + " is not in a transaction");
        } catch (IllegalStateException e) {
            LOG.logE("Execute commit transaction failed :" + e.getMessage());
            throw new ODMFIllegalStateException("Execute rollback commit failed." + e.getMessage(), e);
        }
    }

    public void clearTable(String entityName) {
        SQLException e;
        if (entityName == null) {
            LOG.logE("Execute deleteEntityData failed : The entityName is null.");
            throw new ODMFIllegalArgumentException("Execute deleteEntityData failed : The entityName is null.");
        }
        Entity entity = getModel().getEntity(entityName);
        if (entity == null) {
            LOG.logE("Execute deleteEntityData failed : The entity which the entityName specified not in current model.");
            throw new ODMFIllegalArgumentException("Execute deleteEntityData failed : The entity which the entityName specified not in current model.");
        }
        String tableName = entity.getTableName();
        boolean hasCorruptException = false;
        Cursor cursor = null;
        try {
            this.db.beginTransaction();
            this.db.delete(tableName, null, null);
            List<? extends Relationship> relationships = entity.getRelationships();
            int relationshipSize = relationships.size();
            for (int i = 0; i < relationshipSize; i++) {
                Relationship relationship = (Relationship) relationships.get(i);
                if (relationship.getRelationShipType() == 0) {
                    this.db.delete(DatabaseTableHelper.getManyToManyMidTableName(relationship), null, null);
                }
            }
            cursor = this.db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='sqlite_sequence'", null);
            if (cursor.getCount() > 0) {
                this.db.execSQL("UPDATE sqlite_sequence SET seq = 0 WHERE name = '" + tableName + "'");
            }
            this.db.setTransactionSuccessful();
            if (cursor != null) {
                cursor.close();
            }
            this.db.endTransaction(false);
        } catch (SQLiteDatabaseCorruptException e2) {
            e = e2;
            hasCorruptException = true;
            LOG.logE("Execute deleteEntityData failed : A SQLiteDatabaseCorruptException occurred : " + e.getMessage());
            throw new ODMFSQLiteDatabaseCorruptException("End Transaction failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteDatabaseCorruptException e3) {
            e = e3;
            hasCorruptException = true;
            LOG.logE("Execute deleteEntityData failed : A SQLiteDatabaseCorruptException occurred : " + e.getMessage());
            throw new ODMFSQLiteDatabaseCorruptException("End Transaction failed : " + e.getMessage(), e);
        } catch (SQLiteDiskIOException e22) {
            e = e22;
            hasCorruptException = true;
            LOG.logE("Execute deleteEntityData failed : A SQLiteDiskIOException occurred : " + e.getMessage());
            throw new ODMFSQLiteDiskIOException("End Transaction failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteDiskIOException e4) {
            e = e4;
            hasCorruptException = true;
            LOG.logE("Execute deleteEntityData failed : A SQLiteDiskIOException occurred : " + e.getMessage());
            throw new ODMFSQLiteDiskIOException("End Transaction failed : " + e.getMessage(), e);
        } catch (SQLiteException e222) {
            e = e222;
            LOG.logE("Execute deleteEntityData failed : " + e.getMessage());
            throw new ODMFRuntimeException("Execute deleteEntityData failed : " + e.getMessage());
        } catch (com.huawei.hwsqlite.SQLiteException e5) {
            e = e5;
            LOG.logE("Execute deleteEntityData failed : " + e.getMessage());
            throw new ODMFRuntimeException("Execute deleteEntityData failed : " + e.getMessage());
        } catch (IllegalStateException e6) {
            LOG.logE("Execute deleteEntityData failed : " + e6.getMessage());
            throw new ODMFIllegalStateException("Execute deleteEntityData failed." + e6.getMessage(), e6);
        } catch (RuntimeException e7) {
            LOG.logE("Execute deleteEntityData failed : A RuntimeException occurred : " + e7.getMessage());
            throw new ODMFRuntimeException("Execute delete failed : " + e7.getMessage(), e7);
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            this.db.endTransaction(hasCorruptException);
        }
    }

    protected ManagedObject getObjectValues(ObjectId objectId) {
        Entity entity = getModel().getEntity(objectId.getEntityName());
        Long id = (Long) objectId.getId();
        AEntityHelper entityHelper = getHelper(objectId.getEntityName());
        ManagedObject managedObject = null;
        Cursor cursor = null;
        try {
            cursor = DatabaseQueryService.query(this.db, entity.getTableName(), new String[]{DatabaseQueryService.getRowidColumnName() + " AS " + DatabaseQueryService.getRowidColumnName(), "*"}, DatabaseQueryService.getRowidColumnName() + "=?", new String[]{String.valueOf(id)});
            if (cursor.moveToNext()) {
                managedObject = (ManagedObject) entityHelper.readObject(cursor, 0);
                managedObject.setObjectId(objectId);
                managedObject.setRowId((Long) objectId.getId());
            }
            if (cursor != null) {
                cursor.close();
            }
            return managedObject;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:13:0x0036=Splitter:B:13:0x0036, B:38:0x00a8=Splitter:B:38:0x00a8} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected List<ObjectId> getRelationshipObjectId(ObjectId objectId, Relationship relationship) {
        SQLException e;
        Entity relatedEntity = relationship.getRelatedEntity();
        List<ObjectId> results = new ArrayList();
        Cursor cursor = null;
        try {
            if (relationship.getRelationShipType() == 2) {
                cursor = this.relationshipLoader.getManyToOneRelationshipCursor(objectId, relationship);
            } else if (relationship.getRelationShipType() == 6) {
                cursor = this.relationshipLoader.getOneToOneRelationshipCursor(objectId, relationship);
            } else if (relationship.getRelationShipType() == 4) {
                cursor = this.relationshipLoader.getOneToManyRelationshipCursor(objectId, relationship);
            } else {
                cursor = this.relationshipLoader.getManyToManyRelationshipCursor(objectId, relationship);
            }
            while (cursor.moveToNext()) {
                String idStr = cursor.getString(0);
                if (idStr != null) {
                    results.add(createObjectID(relatedEntity, Long.valueOf(Long.parseLong(idStr))));
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            if (!relationship.getNotFound().equals(ARelationship.EXCEPTION) || results.size() != 0) {
                return results;
            }
            LOG.logE("Execute getRelationshipObjectId failed : The relevant object not found.");
            throw new ODMFRelatedObjectNotFoundException("Execute getRelationshipObjectId failed : The relevant object not found.");
        } catch (SQLiteDatabaseCorruptException e2) {
            e = e2;
            LOG.logE("Execute getRelationshipObjectId failed : A SQLiteException occurred when try to get related ObjectId.");
            throw new ODMFSQLiteDatabaseCorruptException("End Transaction failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteDatabaseCorruptException e3) {
            e = e3;
            LOG.logE("Execute getRelationshipObjectId failed : A SQLiteException occurred when try to get related ObjectId.");
            throw new ODMFSQLiteDatabaseCorruptException("End Transaction failed : " + e.getMessage(), e);
        } catch (SQLiteDiskIOException e22) {
            e = e22;
            LOG.logE("Execute getRelationshipObjectId failed : A SQLiteException occurred when try to get related ObjectId.");
            throw new ODMFSQLiteDiskIOException("End Transaction failed : " + e.getMessage(), e);
        } catch (com.huawei.hwsqlite.SQLiteDiskIOException e4) {
            e = e4;
            LOG.logE("Execute getRelationshipObjectId failed : A SQLiteException occurred when try to get related ObjectId.");
            throw new ODMFSQLiteDiskIOException("End Transaction failed : " + e.getMessage(), e);
        } catch (SQLiteException e5) {
            LOG.logE("Execute getRelationshipObjectId failed : A SQLiteException occurred when try to get related ObjectId.");
            throw new ODMFRuntimeException("Execute getRelationshipObjectId failed : " + e5.getMessage());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    protected void close() {
        try {
            this.databaseHelper.close();
        } catch (SQLiteDatabaseCorruptException | com.huawei.hwsqlite.SQLiteDatabaseCorruptException e) {
            LOG.logE("Close database failed : A SQLiteDatabaseCorruptException occurred when close.");
            throw new ODMFSQLiteDatabaseCorruptException("Close database failed : " + e.getMessage(), e);
        } catch (SQLiteDiskIOException | com.huawei.hwsqlite.SQLiteDiskIOException e2) {
            LOG.logE("Close database failed : A SQLiteDiskIOException occurred when close.");
            throw new ODMFSQLiteDiskIOException("Close database failed : " + e2.getMessage(), e2);
        } catch (RuntimeException e3) {
            LOG.logE("Close database failed : A RuntimeException occurred when close.");
            throw new ODMFRuntimeException("Close database failed : " + e3.getMessage(), e3);
        }
    }

    protected void createEntityForMigration(Entity entity) {
        if (entity == null) {
            LOG.logE("Execute createTable failed : The input parameter entity is null.");
            throw new ODMFIllegalArgumentException("Execute createTable failed : The input parameter entity is null.");
        }
        try {
            this.databaseHelper.addTable(this.db, entity);
        } catch (SQLiteException e) {
            LOG.logE("Execute createTable Failed : A SQLiteException occurred when createTable");
            throw new ODMFRuntimeException("Execute createTable Failed : " + e.getMessage());
        }
    }

    public void dropEntityForMigration(String entityName) {
        if (entityName == null || entityName.equals("")) {
            LOG.logE("Execute dropTable Failed : The input parameter entityName is null.");
            throw new ODMFIllegalArgumentException("Execute dropTable Failed : The input parameter entityName is null.");
        }
        try {
            this.databaseHelper.dropTable(this.db, entityName);
        } catch (SQLiteException e) {
            LOG.logE("Execute dropTable Failed : A SQLiteException occurred when dropTable");
            throw new ODMFRuntimeException("Execute dropTable Failed : " + e.getMessage());
        }
    }

    protected void renameEntityForMigration(String newName, String oldName) {
        if (newName == null || oldName == null) {
            LOG.logE("Execute renameTable Failed :The newName or the oldName is null.");
            throw new ODMFIllegalArgumentException("Execute renameTable Failed : The newName or the oldName is null.");
        }
        try {
            this.databaseHelper.alterTableName(this.db, oldName, newName);
        } catch (SQLiteException e) {
            LOG.logE("Execute renameTable Failed : A SQLiteException occurred when renameTable");
            throw new ODMFRuntimeException("Execute renameTable Failed : " + e.getMessage());
        }
    }

    protected void addColumnForMigration(Entity entity, Attribute attribute) {
        if (entity == null || attribute == null) {
            LOG.logE("Execute addColumn Failed : The entity or the attribute is null.");
            throw new ODMFIllegalArgumentException("Execute addColumn Failed : The entity or the attribute is null.");
        }
        try {
            this.databaseHelper.alterTableAddColumn(this.db, entity.getTableName(), attribute);
        } catch (SQLiteException e) {
            LOG.logE("Execute addColumn Failed : A SQLiteException occurred when addColumn");
            throw new ODMFRuntimeException("Execute addColumn Failed : " + e.getMessage());
        }
    }

    protected void addRelationshipForMigration(Entity entity, Relationship relationship) {
        if (entity == null || relationship == null) {
            LOG.logE("Execute addRelationship Failed : The entity or the relationship is null.");
            throw new ODMFIllegalArgumentException("Execute addRelationship Failed : The entity or the relationship is null.");
        }
        try {
            this.databaseHelper.alterTableAddRelationship(this.db, entity.getTableName(), relationship);
        } catch (SQLiteException e) {
            LOG.logE("Execute addRelationship Failed : A SQLiteException occurred when addRelationship");
            throw new ODMFRuntimeException("Execute addRelationship Failed : " + e.getMessage());
        }
    }

    protected ManagedObject getToOneRelationshipValue(String fieldName, ManagedObject object, ObjectContext objectContext) {
        return loadToOneRelationshipValue(fieldName, object, objectContext);
    }

    protected List<ManagedObject> getToManyRelationshipValue(String fieldName, ManagedObject object, ObjectContext objectContext) {
        if (fieldName == null || object == null || objectContext == null) {
            LOG.logE("Execute getToManyRelationshipValue Failed : The input parameter is null.");
            throw new ODMFIllegalArgumentException("Execute getToManyRelationshipValue Failed : The input parameter is null.");
        }
        Relationship relationship = getModel().getEntity(object.getEntityName()).getRelationship(fieldName);
        if (relationship == null) {
            LOG.logE("Execute getToManyRelationshipValue Failed : This field name is not in the class.");
            throw new ODMFIllegalArgumentException("Execute getToManyRelationshipValue Failed : This field name is not in the class.");
        }
        List<ObjectId> resultID = getRelationshipObjectId(object.getObjectId(), relationship);
        int resultIdSize = resultID.size();
        if (resultIdSize == 0) {
            return null;
        }
        if (relationship.isLazy()) {
            return new LazyList(resultID, objectContext, relationship.getRelatedEntity().getEntityName(), object);
        }
        List<ManagedObject> odmfList = new ODMFList(objectContext, relationship.getRelatedEntity().getEntityName(), object);
        for (int i = 0; i < resultIdSize; i++) {
            odmfList.addObj(i, getManagedObjectWithCache((ObjectId) resultID.get(i), objectContext));
        }
        return odmfList;
    }

    protected ManagedObject remoteGetToOneRelationshipValue(String fieldName, ObjectId objectID, ObjectContext objectContext) {
        return loadToOneRelationshipValue(fieldName, objectID, objectContext);
    }

    protected List<ManagedObject> remoteGetToManyRelationshipValue(String fieldName, ObjectId objectID, ObjectContext objectContext) {
        if (fieldName == null || objectID == null || objectContext == null) {
            LOG.logE("Execute remoteGetToManyRelationshipValue Failed : The input parameter is null.");
            throw new ODMFIllegalArgumentException("Execute remoteGetToManyRelationshipValue Failed : The input parameter is null.");
        }
        Relationship relationship = getModel().getEntity(objectID.getEntityName()).getRelationship(fieldName);
        if (relationship == null) {
            LOG.logE("Execute remoteGetToManyRelationshipValue Failed : This field name is not in the class.");
            throw new ODMFIllegalArgumentException("Execute remoteGetToManyRelationshipValue Failed : This field name is not in the class.");
        }
        List<ObjectId> resultID = getRelationshipObjectId(objectID, relationship);
        int resultIdSize = resultID.size();
        if (resultIdSize == 0) {
            return null;
        }
        List<ManagedObject> resultList = new ArrayList();
        for (int i = 0; i < resultIdSize; i++) {
            resultList.add(getManagedObjectWithCache((ObjectId) resultID.get(i), objectContext));
        }
        return resultList;
    }

    private ManagedObject loadToOneRelationshipValue(String fieldName, Object object, ObjectContext objectContext) {
        if (fieldName == null || object == null || objectContext == null) {
            LOG.logE("Execute loadToOneRelationshipValue Failed : The input parameter is null.");
            throw new ODMFIllegalArgumentException("The input parameter is null.");
        }
        ObjectId id;
        Entity entity;
        if (object instanceof ObjectId) {
            id = (ObjectId) object;
            entity = getModel().getEntity(id.getEntityName());
        } else if (object instanceof ManagedObject) {
            ManagedObject temp = (ManagedObject) object;
            id = temp.getObjectId();
            entity = getModel().getEntity(temp.getEntityName());
        } else {
            LOG.logE("Execute loadToOneRelationshipValue Failed : The related object neither a ObjectId or a ManagedObject.");
            throw new ODMFIllegalArgumentException("Execute loadToOneRelationshipValue Failed : The related object neither a ObjectId nor a ManagedObject.");
        }
        if (id == null || entity == null) {
            LOG.logE("Execute loadToOneRelationshipValue Failed : The id or the entity of the related object does not exist.");
            throw new ODMFIllegalArgumentException("Execute loadToOneRelationshipValue Failed : The id or the entity of the related object does not exist.");
        }
        Relationship relationship = entity.getRelationship(fieldName);
        if (relationship == null) {
            LOG.logE("Execute loadToOneRelationshipValue Failed : The relationship which the fieldName specified does not exist.");
            throw new ODMFIllegalArgumentException("Execute loadToOneRelationshipValue Failed : The relationship which the fieldName specified does not exist.");
        }
        List<ObjectId> resultIDs = getRelationshipObjectId(id, relationship);
        if (resultIDs.size() == 0) {
            return null;
        }
        ObjectId resultID = (ObjectId) resultIDs.get(0);
        ManagedObject managedObject = PersistentStoreCoordinator.getDefault().getObjectFromCache(resultID);
        if (managedObject != null && (!managedObject.isDirty() || managedObject.getLastObjectContext() == objectContext)) {
            return managedObject;
        }
        managedObject = getObjectValues(resultID);
        if (managedObject == null) {
            return managedObject;
        }
        managedObject.setObjectContext(objectContext);
        managedObject.setState(4);
        managedObject.setRowId((Long) resultID.getId());
        PersistentStoreCoordinator.getDefault().putObjectIntoCache(managedObject);
        return managedObject;
    }

    private ManagedObject getManagedObjectWithCache(ObjectId objectId, ObjectContext objectContext) {
        ManagedObject managedObject = PersistentStoreCoordinator.getDefault().getObjectFromCache(objectId);
        if (managedObject == null) {
            managedObject = getObjectValues(objectId);
            if (managedObject != null) {
                managedObject.setObjectContext(objectContext);
                managedObject.setState(4);
                managedObject.setRowId((Long) objectId.getId());
                PersistentStoreCoordinator.getDefault().putObjectIntoCache(managedObject);
            }
        } else if (managedObject.isDirty() && managedObject.getLastObjectContext() != objectContext) {
            managedObject = getObjectValues(objectId);
            if (managedObject != null) {
                managedObject.setObjectContext(objectContext);
                managedObject.setState(4);
                managedObject.setRowId((Long) objectId.getId());
                PersistentStoreCoordinator.getDefault().putObjectIntoCache(managedObject);
            }
        }
        return managedObject;
    }

    protected Cursor executeRawQuerySQL(String sql) {
        if (sql == null) {
            LOG.logE("Execute RawQuerySQL Failed : The sql is null.");
            throw new ODMFIllegalArgumentException("Execute RawQuerySQL Failed : The sql is null.");
        }
        try {
            return this.db.rawSelect(sql, null, null, false);
        } catch (IllegalStateException e) {
            LOG.logE("Execute rawQuerySQL failed : A IllegalStateException occurred when execute rawQuerySQL.");
            throw new ODMFRuntimeException("Execute rawQuerySQL failed : " + e.getMessage(), e);
        } catch (SQLiteDatabaseCorruptException | com.huawei.hwsqlite.SQLiteDatabaseCorruptException e2) {
            LOG.logE("Execute rawQuerySQL failed : A sqliteDatabase occurred when execute rawQuerySQL.");
            throw new ODMFSQLiteDatabaseCorruptException("Execute rawQuerySQL failed : " + e2.getMessage(), e2);
        } catch (SQLiteDiskIOException | com.huawei.hwsqlite.SQLiteDiskIOException e22) {
            LOG.logE("Execute rawQuerySQL failed : A disk io exception occurred when execute rawQuerySQL.");
            throw new ODMFSQLiteDiskIOException("Execute rawQuerySQL failed : " + e22.getMessage(), e22);
        } catch (SQLiteFullException | com.huawei.hwsqlite.SQLiteFullException e222) {
            LOG.logE("Execute rawQuerySQL failed : A disk full exception occurred when execute rawQuerySQL.");
            throw new ODMFSQLiteFullException("Execute rawQuerySQL failed : " + e222.getMessage(), e222);
        } catch (SQLiteAccessPermException | com.huawei.hwsqlite.SQLiteAccessPermException e2222) {
            LOG.logE("Execute rawQuerySQL failed : An access permission exception occurred when execute rawQuerySQL.");
            throw new ODMFSQLiteAccessPermException("Execute rawQuerySQL failed : " + e2222.getMessage(), e2222);
        } catch (SQLiteCantOpenDatabaseException | com.huawei.hwsqlite.SQLiteCantOpenDatabaseException e22222) {
            LOG.logE("Execute rawQuerySQL failed : An cant open exception occurred when execute rawQuerySQL.");
            throw new ODMFSQLiteCantOpenDatabaseException("Execute rawQuerySQL failed : " + e22222.getMessage(), e22222);
        } catch (SQLiteException | com.huawei.hwsqlite.SQLiteException e222222) {
            LOG.logE("Execute rawQuerySQL failed : A SQLite exception occurred when execute rawQuerySQL.");
            throw new ODMFRuntimeException("Execute rawQuerySQL failed : " + e222222.getMessage(), e222222);
        } catch (Exception e3) {
            LOG.logE("Execute rawQuerySQL failed : A unknown exception occurred when execute rawQuerySQL.");
            throw new ODMFRuntimeException("Execute rawQuerySQL failed : " + e3.getMessage(), e3);
        }
    }

    protected Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        try {
            return DatabaseQueryService.query(this.db, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
        } catch (IllegalStateException e) {
            LOG.logE("Execute query failed : A IllegalStateException occurred when execute query.");
            throw new ODMFRuntimeException("Execute query failed : " + e.getMessage(), e);
        } catch (SQLiteDatabaseCorruptException | com.huawei.hwsqlite.SQLiteDatabaseCorruptException e2) {
            LOG.logE("Execute query failed : A sqliteDatabase corrupt exception occurred when execute query.");
            throw new ODMFSQLiteDatabaseCorruptException("Execute query failed : " + e2.getMessage(), e2);
        } catch (SQLiteDiskIOException | com.huawei.hwsqlite.SQLiteDiskIOException e22) {
            LOG.logE("Execute query failed : A disk io exception occurred when execute query.");
            throw new ODMFSQLiteDiskIOException("Execute query failed : " + e22.getMessage(), e22);
        } catch (SQLiteFullException | com.huawei.hwsqlite.SQLiteFullException e222) {
            LOG.logE("Execute query failed : A disk full exception occurred when execute query.");
            throw new ODMFSQLiteFullException("Execute query failed : " + e222.getMessage(), e222);
        } catch (SQLiteAccessPermException | com.huawei.hwsqlite.SQLiteAccessPermException e2222) {
            LOG.logE("Execute query failed : An access permission exception occurred when execute query.");
            throw new ODMFSQLiteAccessPermException("Execute query failed : " + e2222.getMessage(), e2222);
        } catch (SQLiteCantOpenDatabaseException | com.huawei.hwsqlite.SQLiteCantOpenDatabaseException e22222) {
            LOG.logE("Execute query failed : An cant open exception occurred when execute query.");
            throw new ODMFSQLiteCantOpenDatabaseException("Execute query failed : " + e22222.getMessage(), e22222);
        } catch (SQLiteException | com.huawei.hwsqlite.SQLiteException e222222) {
            LOG.logE("Execute query failed : A SQLite exception occurred when execute query.");
            throw new ODMFRuntimeException("Execute query failed : " + e222222.getMessage(), e222222);
        } catch (Exception e3) {
            LOG.logE("Execute query failed : A unknown exception occurred when execute query.");
            throw new ODMFRuntimeException("Execute query failed : " + e3.getMessage(), e3);
        }
    }

    protected void executeRawSQL(String sql) {
        if (sql == null) {
            LOG.logE("Execute RawSQL Failed : The sql is null.");
            throw new ODMFIllegalArgumentException("Execute RawSQL Failed : The sql is null.");
        }
        try {
            this.db.execSQL(sql);
        } catch (IllegalStateException e) {
            LOG.logE("Execute rawSQL failed : A IllegalStateException occurred when execute rawSQL.");
            throw new ODMFRuntimeException("Execute rawSQL failed : " + e.getMessage(), e);
        } catch (SQLiteDatabaseCorruptException | com.huawei.hwsqlite.SQLiteDatabaseCorruptException e2) {
            LOG.logE("Execute rawSQL failed : A sqliteDatabase corrupt exception occurred when execute rawSQL.");
            throw new ODMFSQLiteDatabaseCorruptException("Execute rawSQL failed : " + e2.getMessage(), e2);
        } catch (SQLiteDiskIOException | com.huawei.hwsqlite.SQLiteDiskIOException e22) {
            LOG.logE("Execute rawSQL failed: A disk io exception occurred when execute rawSQL.");
            throw new ODMFSQLiteDiskIOException("Execute rawSQL failed : " + e22.getMessage(), e22);
        } catch (SQLiteFullException | com.huawei.hwsqlite.SQLiteFullException e222) {
            LOG.logE("Execute rawSQL failed : A disk full exception occurred when execute rawSQL.");
            throw new ODMFSQLiteFullException("Execute rawSQL failed : " + e222.getMessage(), e222);
        } catch (SQLiteAccessPermException | com.huawei.hwsqlite.SQLiteAccessPermException e2222) {
            LOG.logE("Execute rawSQL failed : An access permission exception occurred when execute rawSQL.");
            throw new ODMFSQLiteAccessPermException("Execute rawSQL failed : " + e2222.getMessage(), e2222);
        } catch (SQLiteCantOpenDatabaseException | com.huawei.hwsqlite.SQLiteCantOpenDatabaseException e22222) {
            LOG.logE("Execute rawSQL failed : An cant open exception occurred when execute rawSQL.");
            throw new ODMFSQLiteCantOpenDatabaseException("Execute rawSQL failed : " + e22222.getMessage(), e22222);
        } catch (SQLiteException | com.huawei.hwsqlite.SQLiteException e222222) {
            LOG.logE("Execute rawSQL failed : A SQLite exception occurred when execute rawSQL.");
            throw new ODMFRuntimeException("Execute rawSQL failed : " + e222222.getMessage(), e222222);
        } catch (Exception e3) {
            LOG.logE("Execute rawSQL failed : A unknown exception occurred when execute rawSQL.");
            throw new ODMFRuntimeException("Execute rawSQL failed : " + e3.getMessage(), e3);
        }
    }

    protected void setDBVersions(String dbVersion, int dbVersionCode) {
        this.databaseHelper.setDatabaseVersions(this.db, dbVersion, dbVersionCode);
    }

    protected void setEntityVersions(String tableName, String entityVersion, int entityVersionCode) {
        this.databaseHelper.setEntityVersions(this.db, tableName, entityVersion, entityVersionCode);
    }

    public void insertDataForMigration(String tableName, ContentValues values) {
        if (tableName == null || values == null) {
            LOG.logE("Execute insertData Failed : The tableName or the values is null.");
            throw new ODMFIllegalArgumentException("The tableName or the values is null.");
        }
        try {
            this.db.insertOrThrow(tableName, null, values);
        } catch (SQLiteException e) {
            LOG.logE("Execute insertData Failed : A SQLiteException occurred when insertData.");
            throw new ODMFRuntimeException("Execute insertData Failed : " + e.getMessage());
        }
    }

    public void exportDatabase(String newDBName, byte[] newKey) {
        byte[] key;
        Exception e;
        this.context.deleteDatabase(newDBName);
        File newDbFile = this.context.getDatabasePath(newDBName);
        String export = String.format("SELECT sqlcipher_export('%s');", new Object[]{newDBName});
        if (newKey == null || newKey.length == 0) {
            key = new byte[0];
        } else {
            key = newKey;
        }
        Cursor cursor = null;
        try {
            this.db.addAttachAlias(newDBName, newDbFile.getCanonicalPath(), key);
            cursor = executeRawQuerySQL(export);
            cursor.moveToNext();
            this.db.removeAttachAlias(newDBName);
            LOG.logI("Database " + this.path + " is exported to " + newDBName + ".");
            clearKey(key);
            if (cursor != null) {
                cursor.close();
            }
        } catch (IOException e2) {
            e = e2;
            try {
                LOG.logE("Execute exportDatabase Failed : A exception occurred when export database.");
                throw new ODMFRuntimeException("Execute exportDatabase Failed : " + e.toString());
            } catch (Throwable th) {
                clearKey(key);
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (SQLException e22) {
            e = e22;
            LOG.logE("Execute exportDatabase Failed : A exception occurred when export database.");
            throw new ODMFRuntimeException("Execute exportDatabase Failed : " + e.toString());
        }
    }

    public void resetMetadata() {
        this.databaseHelper.resetMetadata(this.db);
        loadMetadata();
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }
}
