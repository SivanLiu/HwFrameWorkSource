package com.android.server.location;

import android.content.Context;
import android.hardware.contexthub.V1_0.ContextHub;
import android.hardware.contexthub.V1_0.ContextHubMsg;
import android.hardware.contexthub.V1_0.HubAppInfo;
import android.hardware.contexthub.V1_0.IContexthub;
import android.hardware.contexthub.V1_0.IContexthubCallback;
import android.hardware.location.ContextHubInfo;
import android.hardware.location.ContextHubMessage;
import android.hardware.location.ContextHubTransaction;
import android.hardware.location.IContextHubCallback;
import android.hardware.location.IContextHubClient;
import android.hardware.location.IContextHubClientCallback;
import android.hardware.location.IContextHubService.Stub;
import android.hardware.location.IContextHubTransactionCallback;
import android.hardware.location.NanoApp;
import android.hardware.location.NanoAppBinary;
import android.hardware.location.NanoAppFilter;
import android.hardware.location.NanoAppInstanceInfo;
import android.hardware.location.NanoAppMessage;
import android.hardware.location.NanoAppState;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.DumpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ContextHubService extends Stub {
    public static final int MSG_DISABLE_NANO_APP = 2;
    public static final int MSG_ENABLE_NANO_APP = 1;
    public static final int MSG_HUB_RESET = 7;
    public static final int MSG_LOAD_NANO_APP = 3;
    public static final int MSG_QUERY_MEMORY = 6;
    public static final int MSG_QUERY_NANO_APPS = 5;
    public static final int MSG_UNLOAD_NANO_APP = 4;
    private static final int OS_APP_INSTANCE = -1;
    private static final String TAG = "ContextHubService";
    private final RemoteCallbackList<IContextHubCallback> mCallbacksList = new RemoteCallbackList();
    private final ContextHubClientManager mClientManager;
    private final Context mContext;
    private final Map<Integer, ContextHubInfo> mContextHubIdToInfoMap;
    private final List<ContextHubInfo> mContextHubInfoList;
    private final IContexthub mContextHubProxy;
    private final Map<Integer, IContextHubClient> mDefaultClientMap;
    private final NanoAppStateManager mNanoAppStateManager = new NanoAppStateManager();
    private final ContextHubTransactionManager mTransactionManager;

    private class ContextHubServiceCallback extends IContexthubCallback.Stub {
        private final int mContextHubId;

        ContextHubServiceCallback(int contextHubId) {
            this.mContextHubId = contextHubId;
        }

        public void handleClientMsg(ContextHubMsg message) {
            ContextHubService.this.handleClientMessageCallback(this.mContextHubId, message);
        }

        public void handleTxnResult(int transactionId, int result) {
            ContextHubService.this.handleTransactionResultCallback(this.mContextHubId, transactionId, result);
        }

        public void handleHubEvent(int eventType) {
            ContextHubService.this.handleHubEventCallback(this.mContextHubId, eventType);
        }

        public void handleAppAbort(long nanoAppId, int abortCode) {
            ContextHubService.this.handleAppAbortCallback(this.mContextHubId, nanoAppId, abortCode);
        }

        public void handleAppsInfo(ArrayList<HubAppInfo> nanoAppInfoList) {
            ContextHubService.this.handleQueryAppsCallback(this.mContextHubId, nanoAppInfoList);
        }
    }

    public ContextHubService(Context context) {
        this.mContext = context;
        this.mContextHubProxy = getContextHubProxy();
        if (this.mContextHubProxy == null) {
            this.mTransactionManager = null;
            this.mClientManager = null;
            this.mDefaultClientMap = Collections.emptyMap();
            this.mContextHubIdToInfoMap = Collections.emptyMap();
            this.mContextHubInfoList = Collections.emptyList();
            return;
        }
        List<ContextHub> hubList;
        this.mClientManager = new ContextHubClientManager(this.mContext, this.mContextHubProxy);
        this.mTransactionManager = new ContextHubTransactionManager(this.mContextHubProxy, this.mClientManager, this.mNanoAppStateManager);
        try {
            hubList = this.mContextHubProxy.getHubs();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while getting Context Hub info", e);
            hubList = Collections.emptyList();
        }
        this.mContextHubIdToInfoMap = Collections.unmodifiableMap(ContextHubServiceUtil.createContextHubInfoMap(hubList));
        this.mContextHubInfoList = new ArrayList(this.mContextHubIdToInfoMap.values());
        HashMap<Integer, IContextHubClient> defaultClientMap = new HashMap();
        for (Integer contextHubId : this.mContextHubIdToInfoMap.keySet()) {
            int contextHubId2 = contextHubId.intValue();
            defaultClientMap.put(Integer.valueOf(contextHubId2), this.mClientManager.registerClient(createDefaultClientCallback(contextHubId2), contextHubId2));
            try {
                this.mContextHubProxy.registerCallback(contextHubId2, new ContextHubServiceCallback(contextHubId2));
            } catch (RemoteException e2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RemoteException while registering service callback for hub (ID = ");
                stringBuilder.append(contextHubId2);
                stringBuilder.append(")");
                Log.e(str, stringBuilder.toString(), e2);
            }
            queryNanoAppsInternal(contextHubId2);
        }
        this.mDefaultClientMap = Collections.unmodifiableMap(defaultClientMap);
    }

    private IContextHubClientCallback createDefaultClientCallback(final int contextHubId) {
        return new IContextHubClientCallback.Stub() {
            public void onMessageFromNanoApp(NanoAppMessage message) {
                ContextHubService.this.onMessageReceiptOldApi(message.getMessageType(), contextHubId, ContextHubService.this.mNanoAppStateManager.getNanoAppHandle(contextHubId, message.getNanoAppId()), message.getMessageBody());
            }

            public void onHubReset() {
                ContextHubService.this.onMessageReceiptOldApi(7, contextHubId, -1, new byte[]{(byte) 0});
            }

            public void onNanoAppAborted(long nanoAppId, int abortCode) {
            }

            public void onNanoAppLoaded(long nanoAppId) {
            }

            public void onNanoAppUnloaded(long nanoAppId) {
            }

            public void onNanoAppEnabled(long nanoAppId) {
            }

            public void onNanoAppDisabled(long nanoAppId) {
            }
        };
    }

    private IContexthub getContextHubProxy() {
        try {
            return IContexthub.getService(true);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while attaching to Context Hub HAL proxy", e);
            return null;
        } catch (NoSuchElementException e2) {
            Log.i(TAG, "Context Hub HAL service not found");
            return null;
        }
    }

    public int registerCallback(IContextHubCallback callback) throws RemoteException {
        checkPermissions();
        this.mCallbacksList.register(callback);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Added callback, total callbacks ");
        stringBuilder.append(this.mCallbacksList.getRegisteredCallbackCount());
        Log.d(str, stringBuilder.toString());
        return 0;
    }

    public int[] getContextHubHandles() throws RemoteException {
        checkPermissions();
        return ContextHubServiceUtil.createPrimitiveIntArray(this.mContextHubIdToInfoMap.keySet());
    }

    public ContextHubInfo getContextHubInfo(int contextHubHandle) throws RemoteException {
        checkPermissions();
        if (this.mContextHubIdToInfoMap.containsKey(Integer.valueOf(contextHubHandle))) {
            return (ContextHubInfo) this.mContextHubIdToInfoMap.get(Integer.valueOf(contextHubHandle));
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid Context Hub handle ");
        stringBuilder.append(contextHubHandle);
        stringBuilder.append(" in getContextHubInfo");
        Log.e(str, stringBuilder.toString());
        return null;
    }

    public List<ContextHubInfo> getContextHubs() throws RemoteException {
        checkPermissions();
        return this.mContextHubInfoList;
    }

    private IContextHubTransactionCallback createLoadTransactionCallback(final int contextHubId, final NanoAppBinary nanoAppBinary) {
        return new IContextHubTransactionCallback.Stub() {
            public void onTransactionComplete(int result) {
                ContextHubService.this.handleLoadResponseOldApi(contextHubId, result, nanoAppBinary);
            }

            public void onQueryResponse(int result, List<NanoAppState> list) {
            }
        };
    }

    private IContextHubTransactionCallback createUnloadTransactionCallback(final int contextHubId) {
        return new IContextHubTransactionCallback.Stub() {
            public void onTransactionComplete(int result) {
                ContextHubService.this.handleUnloadResponseOldApi(contextHubId, result);
            }

            public void onQueryResponse(int result, List<NanoAppState> list) {
            }
        };
    }

    private IContextHubTransactionCallback createQueryTransactionCallback(final int contextHubId) {
        return new IContextHubTransactionCallback.Stub() {
            public void onTransactionComplete(int result) {
            }

            public void onQueryResponse(int result, List<NanoAppState> list) {
                ContextHubService.this.onMessageReceiptOldApi(5, contextHubId, -1, new byte[]{(byte) result});
            }
        };
    }

    public int loadNanoApp(int contextHubHandle, NanoApp nanoApp) throws RemoteException {
        checkPermissions();
        if (this.mContextHubProxy == null) {
            return -1;
        }
        if (!isValidContextHubId(contextHubHandle)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid Context Hub handle ");
            stringBuilder.append(contextHubHandle);
            stringBuilder.append(" in loadNanoApp");
            Log.e(str, stringBuilder.toString());
            return -1;
        } else if (nanoApp == null) {
            Log.e(TAG, "NanoApp cannot be null in loadNanoApp");
            return -1;
        } else {
            NanoAppBinary nanoAppBinary = new NanoAppBinary(nanoApp.getAppBinary());
            this.mTransactionManager.addTransaction(this.mTransactionManager.createLoadTransaction(contextHubHandle, nanoAppBinary, createLoadTransactionCallback(contextHubHandle, nanoAppBinary)));
            return 0;
        }
    }

    public int unloadNanoApp(int nanoAppHandle) throws RemoteException {
        checkPermissions();
        if (this.mContextHubProxy == null) {
            return -1;
        }
        NanoAppInstanceInfo info = this.mNanoAppStateManager.getNanoAppInstanceInfo(nanoAppHandle);
        if (info == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid nanoapp handle ");
            stringBuilder.append(nanoAppHandle);
            stringBuilder.append(" in unloadNanoApp");
            Log.e(str, stringBuilder.toString());
            return -1;
        }
        int contextHubId = info.getContexthubId();
        this.mTransactionManager.addTransaction(this.mTransactionManager.createUnloadTransaction(contextHubId, info.getAppId(), createUnloadTransactionCallback(contextHubId)));
        return 0;
    }

    public NanoAppInstanceInfo getNanoAppInstanceInfo(int nanoAppHandle) throws RemoteException {
        checkPermissions();
        return this.mNanoAppStateManager.getNanoAppInstanceInfo(nanoAppHandle);
    }

    public int[] findNanoAppOnHub(int contextHubHandle, NanoAppFilter filter) throws RemoteException {
        checkPermissions();
        ArrayList<Integer> foundInstances = new ArrayList();
        for (NanoAppInstanceInfo info : this.mNanoAppStateManager.getNanoAppInstanceInfoCollection()) {
            if (filter.testMatch(info)) {
                foundInstances.add(Integer.valueOf(info.getHandle()));
            }
        }
        int[] retArray = new int[foundInstances.size()];
        for (int i = 0; i < foundInstances.size(); i++) {
            retArray[i] = ((Integer) foundInstances.get(i)).intValue();
        }
        return retArray;
    }

    private int queryNanoAppsInternal(int contextHubId) {
        if (this.mContextHubProxy == null) {
            return 1;
        }
        this.mTransactionManager.addTransaction(this.mTransactionManager.createQueryTransaction(contextHubId, createQueryTransactionCallback(contextHubId)));
        return 0;
    }

    public int sendMessage(int contextHubHandle, int nanoAppHandle, ContextHubMessage msg) throws RemoteException {
        checkPermissions();
        int i = -1;
        if (this.mContextHubProxy == null) {
            return -1;
        }
        if (msg == null) {
            Log.e(TAG, "ContextHubMessage cannot be null in sendMessage");
            return -1;
        } else if (msg.getData() == null) {
            Log.e(TAG, "ContextHubMessage message body cannot be null in sendMessage");
            return -1;
        } else if (isValidContextHubId(contextHubHandle)) {
            boolean success = false;
            boolean z = true;
            String str;
            if (nanoAppHandle != -1) {
                NanoAppInstanceInfo info = getNanoAppInstanceInfo(nanoAppHandle);
                if (info != null) {
                    if (((IContextHubClient) this.mDefaultClientMap.get(Integer.valueOf(contextHubHandle))).sendMessageToNanoApp(NanoAppMessage.createMessageToNanoApp(info.getAppId(), msg.getMsgType(), msg.getData())) != 0) {
                        z = false;
                    }
                    success = z;
                } else {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to send nanoapp message - nanoapp with handle ");
                    stringBuilder.append(nanoAppHandle);
                    stringBuilder.append(" does not exist.");
                    Log.e(str, stringBuilder.toString());
                }
            } else if (msg.getMsgType() == 5) {
                if (queryNanoAppsInternal(contextHubHandle) != 0) {
                    z = false;
                }
                success = z;
            } else {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid OS message params of type ");
                stringBuilder2.append(msg.getMsgType());
                Log.e(str, stringBuilder2.toString());
            }
            if (success) {
                i = 0;
            }
            return i;
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Invalid Context Hub handle ");
            stringBuilder3.append(contextHubHandle);
            stringBuilder3.append(" in sendMessage");
            Log.e(str2, stringBuilder3.toString());
            return -1;
        }
    }

    private void handleClientMessageCallback(int contextHubId, ContextHubMsg message) {
        this.mClientManager.onMessageFromNanoApp(contextHubId, message);
    }

    private void handleLoadResponseOldApi(int contextHubId, int result, NanoAppBinary nanoAppBinary) {
        if (nanoAppBinary == null) {
            Log.e(TAG, "Nanoapp binary field was null for a load transaction");
            return;
        }
        byte[] data = new byte[5];
        data[0] = (byte) result;
        ByteBuffer.wrap(data, 1, 4).order(ByteOrder.nativeOrder()).putInt(this.mNanoAppStateManager.getNanoAppHandle(contextHubId, nanoAppBinary.getNanoAppId()));
        onMessageReceiptOldApi(3, contextHubId, -1, data);
    }

    private void handleUnloadResponseOldApi(int contextHubId, int result) {
        onMessageReceiptOldApi(4, contextHubId, -1, new byte[]{(byte) result});
    }

    private void handleTransactionResultCallback(int contextHubId, int transactionId, int result) {
        this.mTransactionManager.onTransactionResponse(transactionId, result);
    }

    private void handleHubEventCallback(int contextHubId, int eventType) {
        if (eventType == 1) {
            this.mTransactionManager.onHubReset();
            queryNanoAppsInternal(contextHubId);
            this.mClientManager.onHubReset(contextHubId);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Received unknown hub event (hub ID = ");
        stringBuilder.append(contextHubId);
        stringBuilder.append(", type = ");
        stringBuilder.append(eventType);
        stringBuilder.append(")");
        Log.i(str, stringBuilder.toString());
    }

    private void handleAppAbortCallback(int contextHubId, long nanoAppId, int abortCode) {
        this.mClientManager.onNanoAppAborted(contextHubId, nanoAppId, abortCode);
    }

    private void handleQueryAppsCallback(int contextHubId, List<HubAppInfo> nanoAppInfoList) {
        List<NanoAppState> nanoAppStateList = ContextHubServiceUtil.createNanoAppStateList(nanoAppInfoList);
        this.mNanoAppStateManager.updateCache(contextHubId, nanoAppInfoList);
        this.mTransactionManager.onQueryResponse(nanoAppStateList);
    }

    private boolean isValidContextHubId(int contextHubId) {
        return this.mContextHubIdToInfoMap.containsKey(Integer.valueOf(contextHubId));
    }

    public IContextHubClient createClient(IContextHubClientCallback clientCallback, int contextHubId) throws RemoteException {
        checkPermissions();
        if (!isValidContextHubId(contextHubId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid context hub ID ");
            stringBuilder.append(contextHubId);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (clientCallback != null) {
            return this.mClientManager.registerClient(clientCallback, contextHubId);
        } else {
            throw new NullPointerException("Cannot register client with null callback");
        }
    }

    public void loadNanoAppOnHub(int contextHubId, IContextHubTransactionCallback transactionCallback, NanoAppBinary nanoAppBinary) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(contextHubId, transactionCallback, 0)) {
            return;
        }
        if (nanoAppBinary == null) {
            Log.e(TAG, "NanoAppBinary cannot be null in loadNanoAppOnHub");
            transactionCallback.onTransactionComplete(2);
            return;
        }
        this.mTransactionManager.addTransaction(this.mTransactionManager.createLoadTransaction(contextHubId, nanoAppBinary, transactionCallback));
    }

    public void unloadNanoAppFromHub(int contextHubId, IContextHubTransactionCallback transactionCallback, long nanoAppId) throws RemoteException {
        checkPermissions();
        if (checkHalProxyAndContextHubId(contextHubId, transactionCallback, 1)) {
            this.mTransactionManager.addTransaction(this.mTransactionManager.createUnloadTransaction(contextHubId, nanoAppId, transactionCallback));
        }
    }

    public void enableNanoApp(int contextHubId, IContextHubTransactionCallback transactionCallback, long nanoAppId) throws RemoteException {
        checkPermissions();
        if (checkHalProxyAndContextHubId(contextHubId, transactionCallback, 2)) {
            this.mTransactionManager.addTransaction(this.mTransactionManager.createEnableTransaction(contextHubId, nanoAppId, transactionCallback));
        }
    }

    public void disableNanoApp(int contextHubId, IContextHubTransactionCallback transactionCallback, long nanoAppId) throws RemoteException {
        checkPermissions();
        if (checkHalProxyAndContextHubId(contextHubId, transactionCallback, 3)) {
            this.mTransactionManager.addTransaction(this.mTransactionManager.createDisableTransaction(contextHubId, nanoAppId, transactionCallback));
        }
    }

    public void queryNanoApps(int contextHubId, IContextHubTransactionCallback transactionCallback) throws RemoteException {
        checkPermissions();
        if (checkHalProxyAndContextHubId(contextHubId, transactionCallback, 4)) {
            this.mTransactionManager.addTransaction(this.mTransactionManager.createQueryTransaction(contextHubId, transactionCallback));
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("Dumping ContextHub Service");
            pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            pw.println("=================== CONTEXT HUBS ====================");
            for (ContextHubInfo hubInfo : this.mContextHubIdToInfoMap.values()) {
                pw.println(hubInfo);
            }
            pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            pw.println("=================== NANOAPPS ====================");
            for (NanoAppInstanceInfo info : this.mNanoAppStateManager.getNanoAppInstanceInfoCollection()) {
                pw.println(info);
            }
        }
    }

    private void checkPermissions() {
        ContextHubServiceUtil.checkPermissions(this.mContext);
    }

    private int onMessageReceiptOldApi(int msgType, int contextHubHandle, int appInstance, byte[] data) {
        if (data == null) {
            return -1;
        }
        int callbacksCount = this.mCallbacksList.beginBroadcast();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Sending message ");
        stringBuilder.append(msgType);
        stringBuilder.append(" version ");
        stringBuilder.append(0);
        stringBuilder.append(" from hubHandle ");
        stringBuilder.append(contextHubHandle);
        stringBuilder.append(", appInstance ");
        stringBuilder.append(appInstance);
        stringBuilder.append(", callBackCount ");
        stringBuilder.append(callbacksCount);
        Log.d(str, stringBuilder.toString());
        if (callbacksCount < 1) {
            Log.v(TAG, "No message callbacks registered.");
            return 0;
        }
        ContextHubMessage msg = new ContextHubMessage(msgType, 0, data);
        for (int i = 0; i < callbacksCount; i++) {
            IContextHubCallback callback = (IContextHubCallback) this.mCallbacksList.getBroadcastItem(i);
            try {
                callback.onMessageReceipt(contextHubHandle, appInstance, msg);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exception (");
                stringBuilder2.append(e);
                stringBuilder2.append(") calling remote callback (");
                stringBuilder2.append(callback);
                stringBuilder2.append(").");
                Log.i(str2, stringBuilder2.toString());
            }
        }
        this.mCallbacksList.finishBroadcast();
        return 0;
    }

    private boolean checkHalProxyAndContextHubId(int contextHubId, IContextHubTransactionCallback callback, int transactionType) {
        if (this.mContextHubProxy == null) {
            try {
                callback.onTransactionComplete(8);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while calling onTransactionComplete", e);
            }
            return false;
        } else if (isValidContextHubId(contextHubId)) {
            return true;
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot start ");
            stringBuilder.append(ContextHubTransaction.typeToString(transactionType, false));
            stringBuilder.append(" transaction for invalid hub ID ");
            stringBuilder.append(contextHubId);
            Log.e(str, stringBuilder.toString());
            try {
                callback.onTransactionComplete(2);
            } catch (RemoteException e2) {
                Log.e(TAG, "RemoteException while calling onTransactionComplete", e2);
            }
            return false;
        }
    }
}
