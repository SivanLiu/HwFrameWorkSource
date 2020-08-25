package com.huawei.nb.searchmanager.client;

import android.content.Context;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.huawei.nb.query.bulkcursor.BulkCursorDescriptor;
import com.huawei.nb.query.bulkcursor.BulkCursorToCursorAdaptor;
import com.huawei.nb.searchmanager.callback.IndexChangeCallback;
import com.huawei.nb.searchmanager.client.ISearchServiceCall;
import com.huawei.nb.searchmanager.client.connect.RemoteServiceConnection;
import com.huawei.nb.searchmanager.client.connect.ServiceConnectCallback;
import com.huawei.nb.searchmanager.client.listener.IIndexChangeListener;
import com.huawei.nb.searchmanager.client.model.IndexData;
import com.huawei.nb.searchmanager.client.model.IndexForm;
import com.huawei.nb.searchmanager.service.Waiter;
import com.huawei.nb.utils.logger.DSLog;
import java.util.List;
import java.util.Map;

public class SearchServiceProxy implements ISearchClient {
    private static final int BINDER_ERROR = -1;
    private static final String SEARCH_SERVICE_ACTION = "com.huawei.nb.searchmanager.service.SearchService.START";
    private static final String TAG = "SearchServiceProxy";
    private static final long TIMEOUT_MILLISECONDS = 5000;
    private volatile long callbackTimeout;
    /* access modifiers changed from: private */
    public final String callingPkgName;
    /* access modifiers changed from: private */
    public IBinder clientDeathBinder;
    private volatile ServiceConnectCallback connectCallback;
    private RemoteServiceConnection.OnConnectListener connectListener = new RemoteServiceConnection.OnConnectListener() {
        /* class com.huawei.nb.searchmanager.client.SearchServiceProxy.AnonymousClass1 */

        @Override // com.huawei.nb.searchmanager.client.connect.RemoteServiceConnection.OnConnectListener
        public void onConnect(IBinder binder) {
            if (binder != null) {
                synchronized (SearchServiceProxy.this.lock) {
                    ISearchServiceCall unused = SearchServiceProxy.this.searchService = ISearchServiceCall.Stub.asInterface(binder);
                    try {
                        SearchServiceProxy.this.searchService.registerClientDeathBinder(SearchServiceProxy.this.clientDeathBinder, SearchServiceProxy.this.callingPkgName);
                    } catch (RemoteException e) {
                        DSLog.et(SearchServiceProxy.TAG, "registerClientDeathBinder RemoteException: " + e.getMessage(), new Object[0]);
                    }
                    SearchServiceProxy.this.waiter.signalAll();
                }
                DSLog.it(SearchServiceProxy.TAG, "Succeed sync connect search service", new Object[0]);
            }
        }

        @Override // com.huawei.nb.searchmanager.client.connect.RemoteServiceConnection.OnConnectListener
        public void onDisconnect() {
            synchronized (SearchServiceProxy.this.lock) {
                ISearchServiceCall unused = SearchServiceProxy.this.searchService = null;
            }
            DSLog.it(SearchServiceProxy.TAG, "sync connection to search service is broken down.", new Object[0]);
        }
    };
    /* access modifiers changed from: private */
    public final Object lock;
    private final RemoteServiceConnection searchConnection;
    /* access modifiers changed from: private */
    public volatile ISearchServiceCall searchService;
    /* access modifiers changed from: private */
    public final Waiter waiter = new Waiter();

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    @Deprecated
    public int getApiVersionCode() {
        return getApiVersion();
    }

    public static int getApiVersion() {
        return 3;
    }

    public static String getApiVersionName() {
        return "10.1.0";
    }

    public SearchServiceProxy(Context context) {
        this.searchConnection = new RemoteServiceConnection(context, SEARCH_SERVICE_ACTION);
        this.connectCallback = null;
        this.searchService = null;
        this.callingPkgName = context.getPackageName();
        this.lock = new Object();
        this.callbackTimeout = TIMEOUT_MILLISECONDS;
        this.clientDeathBinder = new Binder();
    }

    /* access modifiers changed from: private */
    public void invokeConnectCallback(boolean connected) {
        if (connected) {
            if (this.connectCallback != null) {
                this.connectCallback.onConnect();
            }
        } else if (this.connectCallback != null) {
            this.connectCallback.onDisconnect();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x001a, code lost:
        if (r8.searchConnection.open(r8.connectListener) == false) goto L_0x0026;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0024, code lost:
        if (r8.waiter.await(r8.callbackTimeout) != false) goto L_0x0010;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:?, code lost:
        return true;
     */
    public boolean connect() {
        int connectionTimes = 3;
        while (true) {
            connectionTimes--;
            if (connectionTimes > 0) {
                synchronized (this.lock) {
                    if (this.searchService != null) {
                        return true;
                    }
                }
            } else {
                DSLog.et(TAG, "failed to connect to search service in 3 times", new Object[0]);
                return false;
            }
        }
    }

    public boolean connect(ServiceConnectCallback callback) {
        boolean result = true;
        synchronized (this.lock) {
            this.connectCallback = callback;
            if (this.searchService != null) {
                invokeConnectCallback(true);
            } else {
                result = this.searchConnection.open(new RemoteServiceConnection.OnConnectListener() {
                    /* class com.huawei.nb.searchmanager.client.SearchServiceProxy.AnonymousClass2 */

                    @Override // com.huawei.nb.searchmanager.client.connect.RemoteServiceConnection.OnConnectListener
                    public void onConnect(IBinder binder) {
                        if (binder != null) {
                            synchronized (SearchServiceProxy.this.lock) {
                                ISearchServiceCall unused = SearchServiceProxy.this.searchService = ISearchServiceCall.Stub.asInterface(binder);
                                try {
                                    SearchServiceProxy.this.searchService.registerClientDeathBinder(SearchServiceProxy.this.clientDeathBinder, SearchServiceProxy.this.callingPkgName);
                                } catch (RemoteException e) {
                                    DSLog.et(SearchServiceProxy.TAG, "registerClientDeathBinder RemoteException: " + e.getMessage(), new Object[0]);
                                }
                            }
                            DSLog.it(SearchServiceProxy.TAG, "Succeed async connect search service", new Object[0]);
                            SearchServiceProxy.this.invokeConnectCallback(true);
                        }
                    }

                    @Override // com.huawei.nb.searchmanager.client.connect.RemoteServiceConnection.OnConnectListener
                    public void onDisconnect() {
                        synchronized (SearchServiceProxy.this.lock) {
                            ISearchServiceCall unused = SearchServiceProxy.this.searchService = null;
                        }
                        DSLog.it(SearchServiceProxy.TAG, "async connection to search service is broken down.", new Object[0]);
                        SearchServiceProxy.this.invokeConnectCallback(false);
                    }
                });
                if (!result) {
                    DSLog.et(TAG, "Failed to open search service connection.", new Object[0]);
                }
            }
        }
        return result;
    }

    public boolean disconnect() {
        synchronized (this.lock) {
            if (this.searchService != null) {
                try {
                    this.searchService.unRegisterClientDeathBinder(this.clientDeathBinder, this.callingPkgName);
                } catch (RemoteException e) {
                    DSLog.et(TAG, "unRegisterClientDeathBinder RemoteException: " + e.getMessage(), new Object[0]);
                }
                this.searchConnection.close();
                this.searchService = null;
                DSLog.it(TAG, "succeed close search service connection.", new Object[0]);
                invokeConnectCallback(false);
            } else {
                DSLog.it(TAG, "search service connection has been closed already.", new Object[0]);
            }
        }
        return true;
    }

    public boolean hasConnected() {
        return this.searchService != null;
    }

    public boolean isBinded() {
        return this.searchService != null;
    }

    public void setExecutionTimeout(long timeout) {
        this.callbackTimeout = timeout;
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public void executeDBCrawl(String pkgName, List<String> idList, int op) {
        if (this.searchService == null) {
            DSLog.e("Failed to executeDBCrawl, error: searchService is null.", new Object[0]);
            return;
        }
        try {
            this.searchService.executeDBCrawl(pkgName, idList, op, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to executeDBCrawl, errMsg: %s", e.getMessage());
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public BulkCursorDescriptor executeSearch(String pkgName, String queryString, List<String> fieldList, List<Attributes> attrsList) {
        if (this.searchService == null) {
            DSLog.e("Failed to executeSearch, error: searchService is null.", new Object[0]);
            return null;
        }
        try {
            return this.searchService.executeSearch(pkgName, queryString, fieldList, attrsList, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to executeSearch, errMsg: %s", e.getMessage());
            return null;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public Cursor executeSearch(String pkgName, Bundle searchParas) {
        if (this.searchService == null) {
            DSLog.e("Failed to executeSearch, error: searchService is null.", new Object[0]);
            return null;
        }
        try {
            BulkCursorDescriptor bulkCursorDescriptor = this.searchService.executeMultiSearch(pkgName, searchParas, this.callingPkgName);
            if (bulkCursorDescriptor == null) {
                return null;
            }
            BulkCursorToCursorAdaptor adaptor = new BulkCursorToCursorAdaptor();
            adaptor.initialize(bulkCursorDescriptor);
            return adaptor;
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to executeSearch, errMsg: %s", e.getMessage());
            return null;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public void clearUserIndexSearchData(String pkgName, int userId) {
        if (this.searchService == null) {
            DSLog.e("Failed to clearUserIndexSearchData, error: searchService is null.", new Object[0]);
            return;
        }
        try {
            this.searchService.executeClearData(pkgName, userId, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to executeClearData, errMsg: %s", e.getMessage());
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public void executeFileCrawl(String pkgName, String filePath, boolean crawlContent, int op) {
        if (this.searchService == null) {
            DSLog.e("Failed to executeFileCrawl, error: searchService is null.", new Object[0]);
            return;
        }
        try {
            this.searchService.executeFileCrawl(pkgName, filePath, crawlContent, op, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to executeFileCrawl, errMsg: %s", e.getMessage());
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public int executeInsertIndex(String pkgName, List<SearchIndexData> dataList, List<Attributes> attrsList) {
        if (this.searchService == null) {
            DSLog.e("Failed to executeInsertIndex, error: searchService is null.", new Object[0]);
            return 0;
        }
        try {
            return this.searchService.executeInsertIndex(pkgName, dataList, attrsList, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to executeInsertIndex, errMsg: %s", e.getMessage());
            return 0;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public int executeUpdateIndex(String pkgName, List<SearchIndexData> dataList, List<Attributes> attrsList) {
        if (this.searchService == null) {
            DSLog.e("Failed to executeUpdateIndex, error: searchService is null.", new Object[0]);
            return 0;
        }
        try {
            return this.searchService.executeUpdateIndex(pkgName, dataList, attrsList, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to executeUpdateIndex, errMsg: %s", e.getMessage());
            return 0;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public int executeDeleteIndex(String pkgName, List<String> idList, List<Attributes> attrsList) {
        if (this.searchService == null) {
            DSLog.e("Failed to executeDeleteIndex, error: searchService is null.", new Object[0]);
            return 0;
        }
        try {
            return this.searchService.executeDeleteIndex(pkgName, idList, attrsList, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to executeDeleteIndex, errMsg: %s", e.getMessage());
            return 0;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public List<Word> executeAnalyzeText(String pkgName, String text) {
        if (this.searchService == null) {
            DSLog.e("Failed to executeAnalyzeText, error: searchService is null.", new Object[0]);
            return null;
        }
        try {
            return this.searchService.executeAnalyzeText(pkgName, text, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to executeAnalyzeText, errMsg: %s", e.getMessage());
            return null;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public List<SearchIntentItem> executeIntentSearch(String pkgName, String queryString, List<String> fieldList, String type) {
        if (this.searchService == null) {
            DSLog.e("Failed to executeIntentSearch, error: searchService is null.", new Object[0]);
            return null;
        }
        try {
            return this.searchService.executeIntentSearch(pkgName, queryString, fieldList, type, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to executeIntentSearch, errMsg: %s", e.getMessage());
            return null;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public void setSearchSwitch(String pkgName, boolean isSwitchOn) {
        if (this.searchService == null) {
            DSLog.e("Failed to setSearchSwitch, error: searchService is null.", new Object[0]);
            return;
        }
        try {
            this.searchService.setSearchSwitch(pkgName, isSwitchOn, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to setSearchSwitch, errMsg: %s", e.getMessage());
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public String grantFilePermission(String pkgName, String paraType, String pathOrUri, int modeFlags) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to grantFilePermission, error: searchService is null.", new Object[0]);
            return "";
        }
        try {
            return this.searchService.grantFilePermission(pkgName, paraType, pathOrUri, modeFlags, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to grantFilePermission, errMsg: %s", e.getMessage());
            return "";
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public String revokeFilePermission(String pkgName, String paraType, String pathOrUri, int modeFlags) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to revokeFilePermission, error: searchService is null.", new Object[0]);
            return "";
        }
        try {
            return this.searchService.revokeFilePermission(pkgName, paraType, pathOrUri, modeFlags, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to revokeFilePermission, errMsg: %s", e.getMessage());
            return "";
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public int clearIndexForm(String pkgName) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to clearIndexForm, error: searchService is null.", new Object[0]);
            return 0;
        }
        try {
            return this.searchService.clearIndexForm(pkgName, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to clearIndexForm, errMsg: %s", e.getMessage());
            return 0;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public int setIndexForm(String pkgName, int version, List<IndexForm> indexFormList) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to setIndexForm, error: searchService is null.", new Object[0]);
            return 0;
        }
        try {
            return this.searchService.setIndexForm(pkgName, version, indexFormList, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to setIndexForm, errMsg: %s", e.getMessage());
            return 0;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public int getIndexFormVersion(String pkgName) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to getIndexFormVersion, error: searchService is null.", new Object[0]);
            return -1;
        }
        try {
            return this.searchService.getIndexFormVersion(pkgName, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to getIndexFormVersion, errMsg: %s", e.getMessage());
            return -1;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public List<IndexForm> getIndexForm(String pkgName) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to getIndexForm, error: searchService is null.", new Object[0]);
            return null;
        }
        try {
            return this.searchService.getIndexForm(pkgName, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to getIndexForm, errMsg: %s", e.getMessage());
            return null;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public List<IndexData> insert(String groupId, String pkgName, List<IndexData> indexDataList) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to insert index, error: searchService is null", new Object[0]);
            return indexDataList;
        }
        try {
            return this.searchService.insert(groupId, pkgName, indexDataList, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to insert index, errMsg: %s", e.getMessage());
            return indexDataList;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public List<IndexData> update(String groupId, String pkgName, List<IndexData> indexDataList) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to update index, error: searchService is null", new Object[0]);
            return indexDataList;
        }
        try {
            return this.searchService.update(groupId, pkgName, indexDataList, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to update index, errMsg: %s", e.getMessage());
            return indexDataList;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public List<IndexData> delete(String groupId, String pkgName, List<IndexData> indexDataList) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to delete index, error: searchService is null", new Object[0]);
            return indexDataList;
        }
        try {
            return this.searchService.delete(groupId, pkgName, indexDataList, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to delete index, errMsg: %s", e.getMessage());
            return indexDataList;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public List<String> deleteByTerm(String groupId, String pkgName, String indexFieldName, List<String> indexFieldValueList) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to delete index, error: searchService is null", new Object[0]);
            return indexFieldValueList;
        }
        try {
            return this.searchService.deleteByTerm(groupId, pkgName, indexFieldName, indexFieldValueList, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to delete index, errMsg: %s", e.getMessage());
            return indexFieldValueList;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public int deleteByQuery(String groupId, String pkgName, String queryJsonStr) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to delete index, error: searchService is null", new Object[0]);
            return 0;
        }
        try {
            return this.searchService.deleteByQuery(groupId, pkgName, queryJsonStr, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to delete index, errMsg: %s", e.getMessage());
            return 0;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public int clearIndex(String groupId, String pkgName, Map<String, List<String>> deviceIds) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to clear index, error: searchService is null", new Object[0]);
            return 0;
        }
        try {
            return this.searchService.clearIndex(groupId, pkgName, deviceIds, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to clear index, errMsg: %s", e.getMessage());
            return 0;
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public SearchSession beginSearch(String groupId, String pkgName) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to begin search, error: searchService is null", new Object[0]);
            return null;
        }
        ISearchSession searchSessionProxy = null;
        try {
            searchSessionProxy = this.searchService.beginSearch(groupId, pkgName, this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to begin search, errMsg: %s", e.getMessage());
        }
        if (searchSessionProxy != null) {
            return new SearchSession(searchSessionProxy);
        }
        DSLog.et(TAG, "search session proxy is null", new Object[0]);
        return null;
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public void endSearch(String groupId, String pkgName, SearchSession searchSession) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to end search, error: searchService is null", new Object[0]);
        } else if (searchSession == null) {
            DSLog.et(TAG, "null SearchSession instance cannot endSearch", new Object[0]);
        } else {
            try {
                this.searchService.endSearch(groupId, pkgName, searchSession.getSearchSessionProxy(), this.callingPkgName);
            } catch (RemoteException e) {
                DSLog.et(TAG, "Failed to end search, errMsg: %s", e.getMessage());
            }
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public void registerIndexChangeListener(String groupId, String pkgName, IIndexChangeListener listener) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to registerIndexChangeListener, error: searchService is null", new Object[0]);
            return;
        }
        try {
            this.searchService.registerIndexChangeListener(groupId, pkgName, new IndexChangeCallback(listener), this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to registerIndexChangeListener, errMsg: %s", e.getMessage());
        }
    }

    @Override // com.huawei.nb.searchmanager.client.ISearchClient
    public void unRegisterIndexChangeListener(String groupId, String pkgName, IIndexChangeListener listener) {
        if (this.searchService == null) {
            DSLog.et(TAG, "Failed to unRegisterIndexChangeListener, error: searchService is null", new Object[0]);
            return;
        }
        try {
            this.searchService.unRegisterIndexChangeListener(groupId, pkgName, new IndexChangeCallback(listener), this.callingPkgName);
        } catch (RemoteException e) {
            DSLog.et(TAG, "Failed to unRegisterIndexChangeListener, errMsg: %s", e.getMessage());
        }
    }
}
