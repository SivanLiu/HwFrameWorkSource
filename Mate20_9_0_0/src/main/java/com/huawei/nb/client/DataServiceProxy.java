package com.huawei.nb.client;

import android.content.Context;
import android.database.Cursor;
import android.os.IBinder;
import android.os.RemoteException;
import com.huawei.nb.ai.AiModelResponse;
import com.huawei.nb.client.RemoteServiceConnection.OnConnectListener;
import com.huawei.nb.client.ai.NetworkType;
import com.huawei.nb.client.ai.UpdatePackageInfo;
import com.huawei.nb.client.ai.UpdateStatus;
import com.huawei.nb.client.callback.CallbackManager;
import com.huawei.nb.client.callback.DeleteResInfoCallBackAgent;
import com.huawei.nb.client.callback.FetchCallback;
import com.huawei.nb.client.callback.SendEventCallback;
import com.huawei.nb.client.callback.UpdatePackageCallBackAgent;
import com.huawei.nb.client.callback.UpdatePackageCheckCallBackAgent;
import com.huawei.nb.container.ObjectContainer;
import com.huawei.nb.model.coordinator.ResourceInformation;
import com.huawei.nb.model.meta.DataLifeCycle;
import com.huawei.nb.notification.LocalObservable;
import com.huawei.nb.notification.ModelObserver;
import com.huawei.nb.notification.ModelObserverInfo;
import com.huawei.nb.notification.ObserverType;
import com.huawei.nb.notification.RecordObserver;
import com.huawei.nb.odmfadapter.AObjectContextAdapter;
import com.huawei.nb.odmfadapter.OdmfHelper;
import com.huawei.nb.query.IQuery;
import com.huawei.nb.query.Query;
import com.huawei.nb.query.QueryContainer;
import com.huawei.nb.query.RawQuery;
import com.huawei.nb.query.RelationshipQuery;
import com.huawei.nb.query.bulkcursor.BulkCursorDescriptor;
import com.huawei.nb.service.IDataServiceCall;
import com.huawei.nb.service.IDataServiceCall.Stub;
import com.huawei.nb.utils.logger.DSLog;
import com.huawei.odmf.core.AManagedObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DataServiceProxy implements IClient {
    private static final int ADD_SINGLE_DLM_ACTION = 1;
    private static final String DATA_SERVICE_ACTION = "com.huawei.nb.service.DataService.START";
    private static final int DELETE_SINGLE_DLM_ACTION = 2;
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    private static final int QUERY_DLM_ACTION = 0;
    private static final String TAG_DATA_SERVICE = "NaturalBase Data Service";
    private static final long TIMEOUT_MILLISECONDS = 5000;
    private final CallbackManager callbackManager;
    private volatile long callbackTimeout;
    private volatile ServiceConnectCallback connectCallback;
    private volatile IDataServiceCall dataService;
    private final RemoteServiceConnection dsConnection;
    private final ThreadLocal<ErrorInfo> errorInfoThread = new ThreadLocal();
    private volatile boolean hasBinded;
    private volatile boolean hasConnected;
    private final int id = ID_GENERATOR.incrementAndGet();
    private final LocalObservable localObservable;
    private final Object locker;
    private final OdmfHelper odmfHelper;
    private final String pkgName;

    public DataServiceProxy(Context context) {
        this.dsConnection = new RemoteServiceConnection(context, DATA_SERVICE_ACTION);
        this.callbackManager = new CallbackManager();
        this.localObservable = new LocalObservable(this.callbackManager);
        this.odmfHelper = new OdmfHelper(new AObjectContextAdapter(this));
        this.pkgName = context.getPackageName();
        this.connectCallback = null;
        this.dataService = null;
        this.locker = new Object();
        this.hasConnected = false;
        this.hasBinded = false;
        this.callbackTimeout = TIMEOUT_MILLISECONDS;
    }

    private void invokeConnectCallback(boolean connected) {
        if (!connected) {
            this.callbackManager.interruptAll();
            if (this.connectCallback != null) {
                this.connectCallback.onDisconnect();
            }
        } else if (this.connectCallback != null) {
            this.connectCallback.onConnect();
        }
    }

    public boolean connect() {
        return connect(null);
    }

    public boolean connect(ServiceConnectCallback callback) {
        boolean z = true;
        synchronized (this.locker) {
            if (this.hasBinded) {
            } else {
                DSLog.init("HwNaturalBaseClient");
                this.connectCallback = callback;
                if (this.dataService != null) {
                    invokeConnectCallback(true);
                }
                this.hasBinded = this.dsConnection.open(new OnConnectListener() {
                    public void onConnect(IBinder binder) {
                        if (binder != null) {
                            DataServiceProxy.this.dataService = Stub.asInterface(binder);
                            DataServiceProxy.this.localObservable.setRemoteService(DataServiceProxy.this.dataService);
                            DataServiceProxy.this.hasConnected = true;
                            DataServiceProxy.this.invokeConnectCallback(true);
                            DSLog.i("Succeed to connect to %s.", DataServiceProxy.TAG_DATA_SERVICE);
                        }
                    }

                    public void onDisconnect() {
                        DataServiceProxy.this.dataService = null;
                        DataServiceProxy.this.hasConnected = false;
                        DataServiceProxy.this.hasBinded = false;
                        DataServiceProxy.this.invokeConnectCallback(false);
                        DSLog.w("Connection to %s is broken down.", DataServiceProxy.TAG_DATA_SERVICE);
                    }
                });
                if (!this.hasBinded) {
                    DSLog.e("Failed to open connection to %s.", TAG_DATA_SERVICE);
                }
                z = this.hasBinded;
            }
        }
        return z;
    }

    public boolean disconnect() {
        synchronized (this.locker) {
            if (this.hasBinded) {
                invokeConnectCallback(false);
                this.localObservable.dispose();
                this.dsConnection.close();
                this.dataService = null;
                this.hasBinded = false;
                this.hasConnected = false;
                DSLog.i("Connection to %s is closed.", TAG_DATA_SERVICE);
            } else {
                DSLog.i("Connection to %s has been closed already.", TAG_DATA_SERVICE);
            }
        }
        return true;
    }

    public int getId() {
        return this.id;
    }

    public boolean hasConnected() {
        return this.hasConnected;
    }

    public boolean isBinded() {
        return this.hasBinded;
    }

    public void setExecutionTimeout(long timeout) {
        this.callbackTimeout = timeout;
    }

    public <T extends AManagedObject> List<T> executeQuery(RelationshipQuery query) {
        if (query == null) {
            setAndPrintError(1, "Failed to execute relationship query, error: null input query.", new Object[0]);
            return null;
        }
        return this.odmfHelper.assignObjectContext(executeQueryDirect(query));
    }

    public <T extends AManagedObject> T executeSingleQuery(Query query) {
        if (query == null || !query.isValid()) {
            setAndPrintError(1, "Failed to execute single query, error: invalid input query.", new Object[0]);
            return null;
        }
        List<T> entities = executeQuery(query);
        if (entities == null || entities.isEmpty()) {
            return null;
        }
        return (AManagedObject) entities.get(0);
    }

    public <T extends AManagedObject> List<T> executeQuery(Query query) {
        if (query == null || !query.isValid()) {
            setAndPrintError(1, "Failed to execute query, error: invalid input query.", new Object[0]);
            return null;
        }
        List<T> entities = this.odmfHelper.parseCursor(query.getEntityName(), executeCursorQueryDirect(query));
        if (entities != null) {
            return entities;
        }
        setAndPrintError(4, "Failed to execute query, error: service operation failed.", new Object[0]);
        return entities;
    }

    public Cursor executeRawQuery(RawQuery query) {
        if (query == null) {
            setAndPrintError(1, "Failed to execute raw query, error: null input query.", new Object[0]);
            return null;
        }
        BulkCursorDescriptor descriptor = executeCursorQueryDirect(query);
        if (descriptor != null) {
            return this.odmfHelper.wrapCursor(descriptor);
        }
        setAndPrintError(4, "Failed to execute raw query, error: service operation failed.", new Object[0]);
        return null;
    }

    private Object executeAggregateQuery(Query query) {
        List result = executeQueryDirect(query);
        if (result == null || result.isEmpty() || !(result.get(0) instanceof List) || ((List) result.get(0)).isEmpty()) {
            return null;
        }
        return ((List) result.get(0)).get(0);
    }

    private List executeQueryDirect(IQuery query) {
        List list = null;
        if (this.dataService == null) {
            setAndPrintError(2, "Failed to execute query direct, error: not connected to data service.", new Object[0]);
            return list;
        }
        try {
            ObjectContainer oc = this.dataService.executeQueryDirect(new QueryContainer(query, this.pkgName));
            if (oc != null) {
                return oc.get();
            }
            setAndPrintError(4, "Failed to execute query direct, error: service operation failed.", new Object[0]);
            return list;
        } catch (RemoteException | RuntimeException e) {
            setAndPrintError(3, "Failed to execute query direct, error: %s.", e.getMessage());
            return list;
        }
    }

    private BulkCursorDescriptor executeCursorQueryDirect(IQuery query) {
        BulkCursorDescriptor bulkCursorDescriptor = null;
        if (this.dataService == null) {
            setAndPrintError(2, "Failed to execute cursor query, error: not connected to data service.", new Object[0]);
            return bulkCursorDescriptor;
        }
        try {
            return this.dataService.executeCursorQueryDirect(new QueryContainer(query, this.pkgName));
        } catch (RemoteException | RuntimeException e) {
            setAndPrintError(3, "Failed to execute cursor query, error: %s.", e.getMessage());
            return bulkCursorDescriptor;
        }
    }

    public long executeCountQuery(Query query) {
        if (query == null || !query.isValid()) {
            setAndPrintError(1, "Failed to execute count query, error: invalid input query.", new Object[0]);
            return -1;
        } else if (2 != query.getAggregateType()) {
            setAndPrintError(1, "Failed to execute count query, error: count query parameter does not match COUNT.", new Object[0]);
            return -1;
        } else {
            Object ret = executeAggregateQuery(query);
            if (ret != null) {
                return ((Long) ret).longValue();
            }
            return -1;
        }
    }

    public Object executeMaxQuery(Query query) {
        if (query == null || !query.isValid()) {
            setAndPrintError(1, "Failed to execute max query, error: invalid input query.", new Object[0]);
            return null;
        } else if (query.getAggregateType() == 0) {
            return executeAggregateQuery(query);
        } else {
            setAndPrintError(1, "Failed to execute max query, error: maximum query parameter does not match MAX.", new Object[0]);
            return null;
        }
    }

    public Object executeMinQuery(Query query) {
        if (query == null || !query.isValid()) {
            setAndPrintError(1, "Failed to execute min query, error: invalid input query.", new Object[0]);
            return null;
        } else if (1 == query.getAggregateType()) {
            return executeAggregateQuery(query);
        } else {
            setAndPrintError(1, "Failed to execute min query, error: minimum query parameter does not match MIN.", new Object[0]);
            return null;
        }
    }

    public Double executeAvgQuery(Query query) {
        if (query == null || !query.isValid()) {
            setAndPrintError(1, "Failed to execute avg query, error: invalid input query.", new Object[0]);
            return null;
        } else if (3 == query.getAggregateType()) {
            return (Double) executeAggregateQuery(query);
        } else {
            setAndPrintError(1, "Failed to execute avg query, error: average query parameter does not match AVG.", new Object[0]);
            return null;
        }
    }

    public Object executeSumQuery(Query query) {
        if (query == null || !query.isValid()) {
            setAndPrintError(1, "Failed to execute sum query, error: invalid input query.", new Object[0]);
            return null;
        } else if (4 == query.getAggregateType()) {
            return executeAggregateQuery(query);
        } else {
            setAndPrintError(1, "Failed to execute sum query, error: sum query parameter does not match SUM.", new Object[0]);
            return null;
        }
    }

    public Cursor executeFusionQuery(RawQuery fusionQuery) {
        return executeRawQuery(fusionQuery);
    }

    public <T extends AManagedObject> T executeInsert(T entity) {
        if (entity == null) {
            setAndPrintError(1, "Failed to execute insert, error: null entity to insert.", new Object[0]);
            return null;
        }
        List<T> insertedEntities = executeInsert(Arrays.asList(new AManagedObject[]{entity}));
        if (insertedEntities == null || insertedEntities.isEmpty()) {
            return null;
        }
        return (AManagedObject) insertedEntities.get(0);
    }

    public <T extends AManagedObject> List<T> executeInsert(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            setAndPrintError(1, "Failed to execute insert, error: null or empty entity list to insert.", new Object[0]);
            return null;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to execute insert, error: not connected to data service.", new Object[0]);
            return null;
        } else {
            this.odmfHelper.presetUriString(entities);
            try {
                ObjectContainer oc = this.dataService.executeInsertDirect(new ObjectContainer(((AManagedObject) entities.get(0)).getClass(), entities, this.pkgName));
                if (oc != null && oc.get() != null && !oc.get().isEmpty()) {
                    return this.odmfHelper.assignObjectContext(oc.get());
                }
                setAndPrintError(4, "Failed to execute insert, error: service operation failed.", new Object[0]);
                return null;
            } catch (RemoteException | RuntimeException e) {
                setAndPrintError(3, "Failed to execute insert, error: %s.", e.getMessage());
                return null;
            }
        }
    }

    public <T extends AManagedObject> boolean executeInsertEfficiently(T entity) {
        if (entity == null) {
            setAndPrintError(1, "Failed to execute executeInsertEfficiently, error: null entity to insert.", new Object[0]);
            return false;
        }
        return executeInsertEfficiently(Arrays.asList(new AManagedObject[]{entity}));
    }

    public <T extends AManagedObject> boolean executeInsertEfficiently(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            setAndPrintError(1, "Failed to execute executeInsertEfficiently, error: null or empty entity list to insert.", new Object[0]);
            return false;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to execute executeInsertEfficiently, error: not connected to data service.", new Object[0]);
            return false;
        } else {
            try {
                if (this.dataService.executeInsertEfficiently(new ObjectContainer(((AManagedObject) entities.get(0)).getClass(), entities, this.pkgName)) >= 0) {
                    return true;
                }
                setAndPrintError(4, "Failed to execute executeInsertEfficiently, error: service operation failed.", new Object[0]);
                return false;
            } catch (RemoteException | RuntimeException e) {
                setAndPrintError(3, "Failed to execute executeInsertEfficiently, error: %s.", e.getMessage());
                return false;
            }
        }
    }

    public <T extends AManagedObject> boolean executeUpdate(T entity) {
        if (entity == null) {
            setAndPrintError(1, "Failed to execute update, error: null entity to update.", new Object[0]);
            return false;
        }
        return executeUpdate(Arrays.asList(new AManagedObject[]{entity}));
    }

    public <T extends AManagedObject> boolean executeUpdate(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            setAndPrintError(1, "Failed to execute update, error: null or empty entity list to update.", new Object[0]);
            return false;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to execute update, error: not connected to data service.", new Object[0]);
            return false;
        } else {
            boolean z;
            int updatedCount = -1;
            try {
                updatedCount = this.dataService.executeUpdateDirect(new ObjectContainer(((AManagedObject) entities.get(0)).getClass(), entities, this.pkgName));
            } catch (RemoteException | RuntimeException e) {
                setAndPrintError(3, "Failed to execute update, error: %s.", e.getMessage());
            }
            if (updatedCount > 0) {
                z = true;
            } else {
                z = false;
            }
            return z;
        }
    }

    public <T extends AManagedObject> boolean executeDelete(T entity) {
        if (entity == null) {
            setAndPrintError(1, "Failed to execute delete, error: null entity to delete.", new Object[0]);
            return false;
        }
        return executeDelete(Arrays.asList(new AManagedObject[]{entity}));
    }

    public <T extends AManagedObject> boolean executeDelete(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            setAndPrintError(1, "Failed to execute delete, error: null or empty entity list to delete.", new Object[0]);
            return false;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to execute delete, error: not connected to data service.", new Object[0]);
            return false;
        } else {
            boolean z;
            int deletedCount = -1;
            try {
                deletedCount = this.dataService.executeDeleteDirect(new ObjectContainer(((AManagedObject) entities.get(0)).getClass(), entities, this.pkgName), false);
            } catch (RemoteException | RuntimeException e) {
                setAndPrintError(3, "Failed to execute delete, error:%s.", e.getMessage());
            }
            if (deletedCount > 0) {
                z = true;
            } else {
                z = false;
            }
            return z;
        }
    }

    public <T extends AManagedObject> boolean executeDeleteAll(Class<T> clazz) {
        boolean z = true;
        if (clazz == null) {
            setAndPrintError(1, "Failed to execute deleteAll, error: null entity class to delete.", new Object[0]);
            return false;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to execute deleteAll, error: not connected to data service.", new Object[0]);
            return false;
        } else {
            int deletedCount = -1;
            try {
                deletedCount = this.dataService.executeDeleteDirect(new ObjectContainer(clazz, null, this.pkgName), true);
            } catch (RemoteException | RuntimeException e) {
                setAndPrintError(3, "Failed to execute deleteAll, error: %s.", e.getMessage());
            }
            if (deletedCount < 0) {
                z = false;
            }
            return z;
        }
    }

    public <T extends AManagedObject> boolean subscribe(Class<T> clazz, ObserverType type, ModelObserver observer) {
        if (clazz == null || type == null || observer == null) {
            setAndPrintError(2, "Failed to execute subscribe, error: null observer information to subscribe.", new Object[0]);
            return false;
        } else if (type != ObserverType.OBSERVER_RECORD || (observer instanceof RecordObserver)) {
            ModelObserverInfo observerInfo = new ModelObserverInfo(type, clazz, this.pkgName);
            observerInfo.setProxyId(Integer.valueOf(this.id));
            return this.localObservable.registerObserver(observerInfo, observer);
        } else {
            setAndPrintError(1, "Failed to execute subscribe, error: invalid record observer.", new Object[0]);
            return false;
        }
    }

    public <T extends AManagedObject> boolean unSubscribe(Class<T> clazz, ObserverType type, ModelObserver observer) {
        if (clazz == null || type == null || observer == null) {
            setAndPrintError(1, "Failed to execute un-subscribe, error: null observer information to un-subscribe.", new Object[0]);
            return false;
        }
        ModelObserverInfo observerInfo = new ModelObserverInfo(type, clazz, this.pkgName);
        observerInfo.setProxyId(Integer.valueOf(this.id));
        return this.localObservable.unregisterObserver(observerInfo, observer);
    }

    public <T> List sendEvent(List<T> events) {
        if (events == null || events.size() == 0) {
            setAndPrintError(1, "Failed to send event, error: null or empty event list to send.", new Object[0]);
            return null;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to send event, error: not connected to data service.", new Object[0]);
            return null;
        } else {
            ObjectContainer container = new ObjectContainer(events.get(0).getClass(), events, this.pkgName);
            SendEventCallback sendEventCallback = this.callbackManager.createSendEventCallback();
            try {
                List retList = this.odmfHelper.assignObjectContext(sendEventCallback.await(this.dataService.sendEvent(container, sendEventCallback), this.callbackTimeout));
                if (retList == null) {
                    setAndPrintError(4, "Failed to send event, error: service operation failed.", new Object[0]);
                    return null;
                }
                if (retList.isEmpty()) {
                    retList = null;
                }
                return retList;
            } catch (RemoteException | RuntimeException e) {
                setAndPrintError(3, "Failed to send event, error: %s.", e.getMessage());
                return null;
            }
        }
    }

    public List fetchRecommendations(String businessName, String ruleName) {
        Exception e;
        if (businessName == null || businessName.isEmpty()) {
            setAndPrintError(1, "Failed to fetch recommendations, error: null or empty business name.", new Object[0]);
            return null;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to fetch recommendations, error: not connected to data service.", new Object[0]);
            return null;
        } else {
            try {
                ObjectContainer container = this.dataService.fetchRecommendations(businessName, ruleName);
                if (container == null) {
                    setAndPrintError(4, "Failed to fetch recommendations, error: service operation failed.", new Object[0]);
                    return null;
                }
                List retList = container.get();
                if (retList == null) {
                    setAndPrintError(4, "Failed to fetch recommendations, error: service operation failed.", new Object[0]);
                    return null;
                }
                if (retList.isEmpty()) {
                    retList = null;
                }
                return retList;
            } catch (RemoteException e2) {
                e = e2;
                setAndPrintError(3, "Failed to fetch recommendation, error: %s.", e.getMessage());
                return null;
            } catch (RuntimeException e3) {
                e = e3;
                setAndPrintError(3, "Failed to fetch recommendation, error: %s.", e.getMessage());
                return null;
            }
        }
    }

    public String getApiVersion() {
        return "2.7.1";
    }

    public int getApiVersionCode() {
        return 16;
    }

    public String getDatabaseVersion(String databaseName) {
        String str = null;
        if (databaseName == null || databaseName.isEmpty()) {
            setAndPrintError(1, "Failed to get database version, error: invalid database name.", new Object[0]);
            return str;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to get database version, error: not connected to data service.", new Object[0]);
            return str;
        } else {
            try {
                return this.dataService.getDatabaseVersion(databaseName);
            } catch (RemoteException | RuntimeException e) {
                setAndPrintError(3, "Failed to get database version, error: %s.", e.getMessage());
                return str;
            }
        }
    }

    public boolean clearUserData(String databaseName, int type) {
        boolean z = false;
        if (databaseName == null || databaseName.isEmpty()) {
            setAndPrintError(1, "Failed to clear user data, error: invalid database name.", new Object[z]);
            return z;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to clear user data, error: not connected to data service.", new Object[z]);
            return z;
        } else {
            try {
                return this.dataService.clearUserData(databaseName, type);
            } catch (RemoteException | RuntimeException e) {
                setAndPrintError(3, "Failed to clear user data, error: %s.", e.getMessage());
                return z;
            }
        }
    }

    public ObjectContainer requestAiModel(ObjectContainer requestContainer) {
        if (requestContainer == null || requestContainer.get() == null) {
            setAndPrintError(1, "Failed to request Ai model, error: null or empty request container.", new Object[0]);
            return null;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to request Ai model, error: not connected to data service.", new Object[0]);
            return null;
        } else {
            try {
                ObjectContainer responseContainer = this.dataService.requestAiModel(requestContainer);
                if (responseContainer == null || responseContainer.get() == null) {
                    return responseContainer;
                }
                for (Object response : responseContainer.get()) {
                    if (response != null) {
                        this.odmfHelper.resetObjectContext(((AiModelResponse) response).getAiModelList());
                    }
                }
                return responseContainer;
            } catch (RemoteException | RuntimeException e) {
                setAndPrintError(3, "Failed to request Ai model, error: %s.", e.getMessage());
                return null;
            }
        }
    }

    public ObjectContainer requestAiModelAsync(ObjectContainer requestContainer) {
        Exception e;
        if (requestContainer == null || requestContainer.get() == null) {
            setAndPrintError(1, "Failed to async request Ai model, error: null or empty request container.", new Object[0]);
            return null;
        } else if (this.dataService == null) {
            setAndPrintError(2, "Failed to async request Ai model, error: not connected to data service.", new Object[0]);
            return null;
        } else {
            FetchCallback callback = this.callbackManager.createFetchCallback();
            try {
                List result = callback.await(this.dataService.requestAiModelAsync(requestContainer, callback), this.callbackTimeout);
                if (result == null || result.size() == 0) {
                    setAndPrintError(4, "Failed to async request Ai model, error: service operation failed.", new Object[0]);
                    return null;
                }
                for (Object response : result) {
                    if (response != null) {
                        this.odmfHelper.resetObjectContext(((AiModelResponse) response).getAiModelList());
                    }
                }
                return new ObjectContainer(result.get(0).getClass(), result);
            } catch (RemoteException e2) {
                e = e2;
                setAndPrintError(3, "Failed to async request Ai model, error: %s.", e.getMessage());
                return null;
            } catch (RuntimeException e3) {
                e = e3;
                setAndPrintError(3, "Failed to async request Ai model, error: %s.", e.getMessage());
                return null;
            }
        }
    }

    private void setAndPrintError(int errorCode, String msg, Object... args) {
        String temp = String.format(msg, args);
        DSLog.e(temp, new Object[0]);
        this.errorInfoThread.set(new ErrorInfo(errorCode, temp));
    }

    public ErrorInfo getErrorInfo() {
        ErrorInfo errorInfo = (ErrorInfo) this.errorInfoThread.get();
        return errorInfo != null ? errorInfo : new ErrorInfo(0, "");
    }

    public void updatePackageCheckAgent(List<ResourceInformation> resInfoList, UpdatePackageCheckCallBackAgent cb) {
        ObjectContainer oc = new ObjectContainer(ResourceInformation.class, resInfoList, this.pkgName);
        if (this.dataService != null) {
            try {
                this.dataService.updatePackageCheckAgent(oc, cb);
                return;
            } catch (RemoteException e) {
                DSLog.e("Failed to update package check, error: %s.", e.getMessage());
                return;
            }
        }
        cb.onFinish(transferErrResourceInfor2UpdatePack(oc, 14, "dataservice error, dataService is null"), NetworkType.NONE.ordinal());
    }

    private ObjectContainer transferErrResourceInfor2UpdatePack(ObjectContainer oc, int errcode, String msg) {
        ObjectContainer result = new ObjectContainer(UpdatePackageInfo.class);
        if (oc == null) {
            return result;
        }
        List<ResourceInformation> infoList = oc.get();
        if (infoList == null || infoList.isEmpty()) {
            return result;
        }
        List<UpdatePackageInfo> updateInfoList = new ArrayList();
        for (ResourceInformation info : infoList) {
            UpdatePackageInfo updateInfo = new UpdatePackageInfo();
            updateInfo.setResid(info.getResid());
            updateInfo.setErrorCode(errcode);
            updateInfo.setErrorMessage(msg);
            updateInfo.setUpdateAvailable(false);
            updateInfo.setNewVersionCode(0);
            updateInfo.setNewPackageSize(0);
            updateInfoList.add(updateInfo);
        }
        ObjectContainer ocNew = new ObjectContainer(UpdatePackageInfo.class);
        for (UpdatePackageInfo updatePackageInfo : updateInfoList) {
            ocNew.add(updatePackageInfo);
        }
        return ocNew;
    }

    public void updatePackageAgent(List<ResourceInformation> resInfoList, UpdatePackageCallBackAgent cb, long refreshInterval, long refreshBucketSize, boolean wifiOnly) {
        ObjectContainer oc = new ObjectContainer(ResourceInformation.class, resInfoList, this.pkgName);
        if (this.dataService != null) {
            try {
                this.dataService.updatePackageAgent(oc, cb, refreshInterval, refreshBucketSize, wifiOnly);
                return;
            } catch (RemoteException e) {
                DSLog.e("Failed to update package, error: %s.", e.getMessage());
                return;
            }
        }
        cb.onRefresh(UpdateStatus.FAILURE.ordinal(), 0, 0, 0, 0, 14, "dataservice error, dataService is null");
    }

    public void deleteResInfoAgent(List<ResourceInformation> resInfoList, DeleteResInfoCallBackAgent cb) {
        ObjectContainer oc = new ObjectContainer(ResourceInformation.class, resInfoList, this.pkgName);
        if (this.dataService != null) {
            try {
                this.dataService.deleteResInfoAgent(oc, cb);
                return;
            } catch (RemoteException e) {
                DSLog.e("Failed to delete Res Info, error: %s.", e.getMessage());
                return;
            }
        }
        cb.onFailure(14, "dataservice error, dataService is null");
    }

    public ObjectContainer insertResInfoAgent(ResourceInformation resourceInformation) {
        List<ResourceInformation> resInfoList = new ArrayList();
        resInfoList.add(resourceInformation);
        ObjectContainer oc = new ObjectContainer(ResourceInformation.class, resInfoList, this.pkgName);
        ObjectContainer insertResult = null;
        if (this.dataService != null) {
            try {
                return this.dataService.insertResInfoAgent(oc);
            } catch (RemoteException | RuntimeException e) {
                DSLog.e("Failed to insert Res Info, error: %s.", e.getMessage());
                return insertResult;
            }
        }
        DSLog.e("Failed to insert Res Info, error: date service is null.", new Object[0]);
        return insertResult;
    }

    public boolean updateResInfoAgent(ResourceInformation resourceInformation) {
        boolean z = false;
        List<ResourceInformation> resInfoList = new ArrayList();
        resInfoList.add(resourceInformation);
        ObjectContainer oc = new ObjectContainer(ResourceInformation.class, resInfoList, this.pkgName);
        if (this.dataService != null) {
            try {
                return this.dataService.updateResInfoAgent(oc);
            } catch (RemoteException | RuntimeException e) {
                DSLog.e("Failed to update Res Info, error: %s.", e.getMessage());
            }
        }
        DSLog.e("Failed to update Res Info, error: date service is null.", new Object[z]);
        return z;
    }

    public List<DataLifeCycle> queryDataLifeCycleConfig(String dbName, String tableName) {
        if (dbName == null || dbName.equals("")) {
            DSLog.e("Failed to query dataLifeCycleConfig, error: param dbName is empty.", new Object[0]);
            return null;
        } else if (tableName == null || tableName.equals("")) {
            DSLog.e("Failed to query dataLifeCycleConfig, error: param tableName is empty.", new Object[0]);
            return null;
        } else {
            DSLog.d("query dataLifeCycleConfig for db[%s] table[%s] pkgname[%s].", dbName, tableName, this.pkgName);
            DataLifeCycle dlc = new DataLifeCycle();
            dlc.setMDBName(dbName);
            dlc.setMTableName(tableName);
            ObjectContainer oc = new ObjectContainer(DataLifeCycle.class, Collections.singletonList(dlc), this.pkgName);
            if (this.dataService != null) {
                try {
                    List<DataLifeCycle> responseList;
                    ObjectContainer result = this.dataService.handleDataLifeCycleConfig(0, oc);
                    if (result != null) {
                        responseList = result.get();
                    } else {
                        responseList = null;
                    }
                    if (responseList != null) {
                        return responseList;
                    }
                    DSLog.e("Failed to query dataLifeCycleConfig, error: query result null with db[%s] table[%s] pkgname[%s].", dbName, tableName, this.pkgName);
                    return null;
                } catch (RemoteException | RuntimeException e) {
                    DSLog.e("Failed to query dataLifeCycleConfig, error: %s.", e.getMessage());
                    return null;
                }
            }
            DSLog.e("Failed to query dataLifeCycleConfig, error: date service is null.", new Object[0]);
            return null;
        }
    }

    public int addDataLifeCycleConfig(String dbName, String tableName, String fieldName, int mode, int count) {
        DataLifeCycle dlc = new DataLifeCycle();
        dlc.setMDBName(dbName);
        dlc.setMTableName(tableName);
        dlc.setMFieldName(fieldName);
        dlc.setMMode(Integer.valueOf(mode));
        dlc.setMCount(Integer.valueOf(count));
        ObjectContainer oc = new ObjectContainer(DataLifeCycle.class, Collections.singletonList(dlc), this.pkgName);
        if (this.dataService != null) {
            try {
                ObjectContainer resultObj = this.dataService.handleDataLifeCycleConfig(1, oc);
                if (resultObj == null || resultObj.get() == null || resultObj.get().isEmpty()) {
                    DSLog.e("Failed to add dataLifeCycleConfig, error: add result null with db[%s] table[%s] fieldName[%s] mode[%s] count[%s] pkgname[%s].", dbName, tableName, fieldName, Integer.valueOf(mode), Integer.valueOf(count), this.pkgName);
                    return 4;
                } else if (resultObj.get().get(0) instanceof Integer) {
                    return ((Integer) resultObj.get().get(0)).intValue();
                } else {
                    DSLog.e("Failed to add dataLifeCycleConfig, error: result is not a integer with db[%s] table[%s] fieldName[%s] mode[%s] count[%s] pkgname[%s].", dbName, tableName, fieldName, Integer.valueOf(mode), Integer.valueOf(count), this.pkgName);
                    return 4;
                }
            } catch (RemoteException | RuntimeException e) {
                DSLog.e("Failed to add dataLifeCycleConfig, error: %s.", e.getMessage());
                return 3;
            }
        }
        DSLog.e("Failed to add dataLifeCycleConfig, error: date service is null.", new Object[0]);
        return 2;
    }

    public int removeDataLifeCycleConfig(String dbName, String tableName, String fieldName, int mode, int count) {
        DataLifeCycle dlc = new DataLifeCycle();
        dlc.setMDBName(dbName);
        dlc.setMTableName(tableName);
        dlc.setMFieldName(fieldName);
        dlc.setMMode(Integer.valueOf(mode));
        dlc.setMCount(Integer.valueOf(count));
        ObjectContainer oc = new ObjectContainer(DataLifeCycle.class, Collections.singletonList(dlc), this.pkgName);
        if (this.dataService != null) {
            try {
                ObjectContainer resultObj = this.dataService.handleDataLifeCycleConfig(2, oc);
                if (resultObj == null || resultObj.get() == null || resultObj.get().isEmpty()) {
                    DSLog.e("Failed to remove dataLifeCycleConfig, error: add result null with db[%s] table[%s] fieldName[%s] mode[%s] count[%s] pkgname[%s].", dbName, tableName, fieldName, Integer.valueOf(mode), Integer.valueOf(count), this.pkgName);
                    return 4;
                } else if (resultObj.get().get(0) instanceof Integer) {
                    return ((Integer) resultObj.get().get(0)).intValue();
                } else {
                    DSLog.e("Failed to remove dataLifeCycleConfig, error: result is not a integer with db[%s] table[%s] fieldName[%s] mode[%s] count[%s] pkgname[%s].", dbName, tableName, fieldName, Integer.valueOf(mode), Integer.valueOf(count), this.pkgName);
                    return 4;
                }
            } catch (RemoteException | RuntimeException e) {
                DSLog.e("Failed to remove dataLifeCycleConfig, error: %s.", e.getMessage());
                return 3;
            }
        }
        DSLog.e("Failed to remove dataLifeCycleConfig, error: date service is null.", new Object[0]);
        return 2;
    }
}
