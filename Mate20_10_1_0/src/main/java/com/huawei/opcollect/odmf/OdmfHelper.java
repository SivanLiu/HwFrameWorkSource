package com.huawei.opcollect.odmf;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;
import com.huawei.nb.client.DataServiceProxy;
import com.huawei.nb.client.ServiceConnectCallback;
import com.huawei.nb.client.kv.KvClient;
import com.huawei.nb.kv.KCompositeString;
import com.huawei.nb.kv.VJson;
import com.huawei.nb.model.meta.DataLifeCycle;
import com.huawei.nb.notification.ModelObserver;
import com.huawei.nb.notification.ObserverType;
import com.huawei.nb.query.Query;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OdmfHelper {
    private static final long INSERT_INTERVAL_MS = 900000;
    private static final int MAX_BUFFER_COUNT = 50;
    public static final String ODMF_API_VERSION_2_11_2 = "2.11.2";
    public static final String ODMF_API_VERSION_2_11_3 = "2.11.3";
    public static final String ODMF_API_VERSION_2_11_6 = "2.11.6";
    public static final String ODMF_API_VERSION_2_11_7 = "2.11.7";
    private static final int RECONNECT_TIMES = 10;
    private static final String TAG = "OdmfHelper";
    /* access modifiers changed from: private */
    public int count = 0;
    /* access modifiers changed from: private */
    public volatile boolean hasConnected = false;
    private long lastTime = SystemClock.elapsedRealtime();
    /* access modifiers changed from: private */
    public CountDownLatch latch;
    private Context mContext = null;
    private DataServiceProxy mDataServiceProxy = null;
    private KvClient<KCompositeString, VJson> mKvClient;
    private final Object mLock = new Object();
    protected ServiceConnectCallback odmfConnectCallback = new ServiceConnectCallback() {
        /* class com.huawei.opcollect.odmf.OdmfHelper.AnonymousClass1 */

        public void onConnect() {
            OPCollectLog.i(OdmfHelper.TAG, "Odmf service is connected");
            synchronized (OdmfHelper.this) {
                int unused = OdmfHelper.this.count = 0;
            }
            boolean unused2 = OdmfHelper.this.hasConnected = true;
            OdmfHelper.this.latch.countDown();
            OdmfCollectScheduler.getInstance().getCtrlHandler().sendEmptyMessage(OdmfCollectScheduler.MSG_ODMF_CONNECTED);
        }

        public void onDisconnect() {
            OPCollectLog.w(OdmfHelper.TAG, "Odmf service is disconnected");
            boolean unused = OdmfHelper.this.hasConnected = false;
            OdmfCollectScheduler.getInstance().getCtrlHandler().sendEmptyMessage(OdmfCollectScheduler.MSG_ODMF_DISCONNECTED);
        }
    };
    private Map<String, List<AManagedObject>> rawDatas = new HashMap();
    private int record = 0;

    public OdmfHelper(Context context) {
        OPCollectLog.r(TAG, TAG);
        this.mContext = context;
        this.latch = new CountDownLatch(1);
        this.hasConnected = false;
        this.mKvClient = new KvClient<>(context);
        if (OPCollectUtils.checkODMFApiVersion(context, ODMF_API_VERSION_2_11_7)) {
            this.mDataServiceProxy = this.mKvClient.getDataServiceProxy();
        } else {
            this.mDataServiceProxy = new DataServiceProxy(context);
        }
    }

    public boolean connectOdmfService() {
        if (this.hasConnected) {
            return true;
        }
        OPCollectLog.r(TAG, "connectOdmfService");
        this.mKvClient.connect(this.odmfConnectCallback);
        try {
            this.latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            OPCollectLog.e(TAG, "connectOdmfService " + e.getMessage());
        }
        if (this.hasConnected) {
            return true;
        }
        OPCollectLog.e(TAG, "connect failed");
        return false;
    }

    private void checkReConnectOdmfService() {
        synchronized (this) {
            this.count++;
            if (this.count > 10) {
                this.count = 0;
                connectOdmfService();
            }
        }
    }

    private boolean isScreenOn() {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        if (powerManager != null) {
            return powerManager.isInteractive();
        }
        OPCollectLog.e(TAG, "isScreenOn powerManager is null");
        return true;
    }

    private boolean isCharging() {
        boolean isCharging = true;
        Intent batteryBroadcast = this.mContext.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        if (batteryBroadcast == null) {
            OPCollectLog.e(TAG, "isCharging batteryBroadcast is null");
        } else {
            if (batteryBroadcast.getIntExtra("plugged", -1) == 0) {
                isCharging = false;
            }
            OPCollectLog.i(TAG, "isCharging: " + isCharging);
        }
        return isCharging;
    }

    private void opcollectCache(AManagedObject rawData) {
        synchronized (this.mLock) {
            if (rawData != null) {
                this.record++;
                OPCollectLog.i(TAG, "opcollectCache " + this.record);
                String entityName = rawData.getEntityName();
                if (!this.rawDatas.containsKey(entityName)) {
                    List<AManagedObject> rawDataPerList = new ArrayList<>();
                    rawDataPerList.add(rawData);
                    this.rawDatas.put(entityName, rawDataPerList);
                } else {
                    this.rawDatas.get(entityName).add(rawData);
                }
            }
            long curTime = SystemClock.elapsedRealtime();
            long interval = curTime - this.lastTime;
            OPCollectLog.i(TAG, "opcollectCache curTime: " + curTime + ", interval: " + interval);
            if (isCharging() || isScreenOn() || this.record >= 50 || interval >= INSERT_INTERVAL_MS) {
                for (Map.Entry<String, List<AManagedObject>> entry : this.rawDatas.entrySet()) {
                    bulkInsertManageObject(entry.getValue());
                    OPCollectLog.i(TAG, "opcollectCache size: " + entry.getValue().size());
                }
                this.lastTime = SystemClock.elapsedRealtime();
                this.record = 0;
                this.rawDatas.clear();
                OPCollectLog.i(TAG, "opcollectCache lastTime: " + this.lastTime);
            }
        }
    }

    public void insertManageObject(AManagedObject rawData) {
        if (rawData != null) {
            opcollectCache(rawData);
        }
    }

    public AManagedObject insertManageObjectWithoutCache(AManagedObject rawData) {
        AManagedObject object = null;
        if (rawData != null) {
            if (!this.hasConnected) {
                checkReConnectOdmfService();
            } else {
                object = this.mDataServiceProxy.executeInsert(rawData);
                if (object != null) {
                    OPCollectLog.r(TAG, "insert database success ");
                } else {
                    OPCollectLog.r(TAG, "insert database failed ");
                }
            }
        }
        return object;
    }

    public List<AManagedObject> bulkInsertManageObject(List<AManagedObject> objectList) {
        List<AManagedObject> retObjectList = null;
        if (objectList != null) {
            if (!this.hasConnected) {
                checkReConnectOdmfService();
            } else {
                retObjectList = this.mDataServiceProxy.executeInsert(objectList);
                if (retObjectList != null) {
                    OPCollectLog.r(TAG, "insert database success ");
                } else {
                    OPCollectLog.r(TAG, "insert database failed ");
                }
            }
        }
        return retObjectList;
    }

    public long queryManageObjectCount(Query query) {
        if (query == null) {
            return -1;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.executeCountQuery(query);
        }
        checkReConnectOdmfService();
        return -1;
    }

    public List<AManagedObject> queryManageObject(Query query) {
        if (query == null) {
            return null;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.executeQuery(query);
        }
        checkReConnectOdmfService();
        return null;
    }

    public AManagedObject querySingleManageObject(Query query) {
        if (query == null) {
            return null;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.executeSingleQuery(query);
        }
        checkReConnectOdmfService();
        return null;
    }

    public boolean updateManageObject(AManagedObject rawData) {
        if (rawData == null) {
            return false;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.executeUpdate(rawData);
        }
        checkReConnectOdmfService();
        return false;
    }

    public boolean updateManageObjects(List<AManagedObject> objectList) {
        if (objectList == null || objectList.size() == 0) {
            return false;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.executeUpdate(objectList);
        }
        checkReConnectOdmfService();
        return false;
    }

    public boolean deleteManageObject(AManagedObject rawData) {
        if (rawData == null) {
            return false;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.executeDelete(rawData);
        }
        checkReConnectOdmfService();
        return false;
    }

    public boolean deleteManageObjects(List<AManagedObject> rawDatas2) {
        if (rawDatas2 == null) {
            return false;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.executeDelete(rawDatas2);
        }
        checkReConnectOdmfService();
        return false;
    }

    public boolean subscribeManagedObject(Class clazz, ObserverType type, ModelObserver observer) {
        if (observer == null) {
            return false;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.subscribe(clazz, type, observer);
        }
        checkReConnectOdmfService();
        return false;
    }

    public boolean unSubscribeManagedObject(Class clazz, ObserverType type, ModelObserver observer) {
        if (observer == null) {
            return false;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.unSubscribe(clazz, type, observer);
        }
        checkReConnectOdmfService();
        return false;
    }

    public int addDataLifeCycleConfig(String dbName, String tableName, String fieldName, int mode, int count2) {
        if (dbName == null || tableName == null || fieldName == null) {
            return 1;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.addDataLifeCycleConfig(dbName, tableName, fieldName, mode, count2);
        }
        checkReConnectOdmfService();
        return 2;
    }

    public int removeDataLifeCycleConfig(String dbName, String tableName, String fieldName, int mode, int count2) {
        if (dbName == null || tableName == null || fieldName == null) {
            return 1;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.removeDataLifeCycleConfig(dbName, tableName, fieldName, mode, count2);
        }
        checkReConnectOdmfService();
        return 2;
    }

    public List<DataLifeCycle> queryDataLifeCycleConfig(String dbName, String tableName) {
        if (dbName == null || tableName == null) {
            return null;
        }
        if (this.hasConnected) {
            return this.mDataServiceProxy.queryDataLifeCycleConfig(dbName, tableName);
        }
        checkReConnectOdmfService();
        return null;
    }

    public boolean put(KCompositeString key, VJson value) {
        if (this.hasConnected) {
            return this.mKvClient.put(key, value);
        }
        checkReConnectOdmfService();
        return false;
    }

    public VJson get(KCompositeString key) {
        if (this.hasConnected) {
            return this.mKvClient.get(key);
        }
        checkReConnectOdmfService();
        return null;
    }

    public boolean grant(KCompositeString key, String packageName, int authority) {
        if (this.hasConnected) {
            return this.mKvClient.grant(key, packageName, authority);
        }
        checkReConnectOdmfService();
        return false;
    }
}
