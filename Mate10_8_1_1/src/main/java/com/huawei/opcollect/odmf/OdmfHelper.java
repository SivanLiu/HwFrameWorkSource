package com.huawei.opcollect.odmf;

import android.content.Context;
import com.huawei.nb.client.DataServiceProxy;
import com.huawei.nb.client.ServiceConnectCallback;
import com.huawei.nb.notification.ModelObserver;
import com.huawei.nb.notification.ObserverType;
import com.huawei.nb.query.Query;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.Waiter;
import java.util.List;

public class OdmfHelper {
    private static final int RECONNECT_TIMES = 10;
    private static final String TAG = "OdmfHelper";
    private int count = 0;
    private volatile boolean hasConnected = false;
    private DataServiceProxy mDataServiceProxy = null;
    protected ServiceConnectCallback odmfConnectCallback = new ServiceConnectCallback() {
        public void onConnect() {
            OPCollectLog.i(OdmfHelper.TAG, "Odmf service is connected");
            synchronized (this) {
                OdmfHelper.this.count = 0;
            }
            OdmfHelper.this.hasConnected = true;
            OdmfCollectScheduler.getInstance().getCtrlHandler().sendEmptyMessage(OdmfCollectScheduler.MSG_ODMF_CONNECTED);
            OdmfHelper.this.waiter.signal();
        }

        public void onDisconnect() {
            OPCollectLog.w(OdmfHelper.TAG, "Odmf service is disconnceted");
            OdmfHelper.this.hasConnected = false;
            OdmfCollectScheduler.getInstance().getCtrlHandler().sendEmptyMessage(OdmfCollectScheduler.MSG_ODMF_DISCONNECTED);
            OdmfHelper.this.waiter.signal();
        }
    };
    private final Waiter waiter = new Waiter();

    public OdmfHelper(Context context) {
        OPCollectLog.r(TAG, TAG);
        this.hasConnected = false;
        this.mDataServiceProxy = new DataServiceProxy(context);
    }

    public boolean connectOdmfService() {
        if (this.hasConnected) {
            return true;
        }
        OPCollectLog.r(TAG, "connectOdmfService");
        try {
            this.mDataServiceProxy.connect(this.odmfConnectCallback);
            this.waiter.await(500);
            if (this.hasConnected) {
                return true;
            }
            OPCollectLog.e(TAG, "connect failed");
            return false;
        } catch (RuntimeException e) {
            OPCollectLog.e(TAG, "connectOdmfService " + e.getMessage());
            return false;
        }
    }

    private void checkReConnectOdmfService() {
        synchronized (this) {
            this.count++;
            if (this.count > RECONNECT_TIMES) {
                this.count = 0;
                connectOdmfService();
            }
        }
    }

    public AManagedObject insertManageObject(AManagedObject rawData) {
        if (rawData == null) {
            return null;
        }
        if (this.hasConnected) {
            AManagedObject rInfo = null;
            try {
                rInfo = this.mDataServiceProxy.executeInsert(rawData);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "insertManageObject " + e.getMessage());
            }
            if (rInfo != null) {
                OPCollectLog.r(TAG, "insert " + rInfo.getDatabaseName() + " success ");
            } else {
                OPCollectLog.r(TAG, "insert " + rawData.getDatabaseName() + " failed ");
            }
            return rInfo;
        }
        checkReConnectOdmfService();
        OPCollectLog.w(TAG, "Odmf service is disconnceted, will reconnect later");
        return null;
    }

    public List<AManagedObject> bulkInsertManageObject(List<AManagedObject> rawDatas) {
        List<AManagedObject> rInfo = null;
        if (rawDatas == null) {
            return null;
        }
        if (this.hasConnected) {
            try {
                rInfo = this.mDataServiceProxy.executeInsert(rawDatas);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "bulkInsertManageObject " + e.getMessage());
            }
            if (rInfo != null) {
                if (rInfo.get(0) != null) {
                    OPCollectLog.i(TAG, "insert " + ((AManagedObject) rInfo.get(0)).getDatabaseName() + " success ");
                }
            } else if (rawDatas.get(0) != null) {
                OPCollectLog.r(TAG, "insert " + ((AManagedObject) rawDatas.get(0)).getDatabaseName() + " failed ");
            }
            return rInfo;
        }
        checkReConnectOdmfService();
        OPCollectLog.w(TAG, "Odmf service is disconnceted, will reconnect later");
        return null;
    }

    public long queryManageObjectCount(Query query) {
        long count = -1;
        if (query == null) {
            return count;
        }
        if (this.hasConnected) {
            try {
                count = this.mDataServiceProxy.executeCountQuery(query);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "queryManageObjectCount " + e.getMessage());
            }
            return count;
        }
        checkReConnectOdmfService();
        OPCollectLog.e(TAG, "Odmf service is disconnceted, will reconnect later");
        return count;
    }

    public List<AManagedObject> queryManageObject(Query query) {
        List<AManagedObject> objects = null;
        if (query == null) {
            return null;
        }
        if (this.hasConnected) {
            try {
                objects = this.mDataServiceProxy.executeQuery(query);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "queryManageObject " + e.getMessage());
            }
            return objects;
        }
        checkReConnectOdmfService();
        OPCollectLog.e(TAG, "Odmf service is disconnceted, will reconnect later");
        return null;
    }

    public AManagedObject querySingleManageObject(Query query) {
        AManagedObject aManagedObject = null;
        if (query == null) {
            return null;
        }
        if (this.hasConnected) {
            try {
                aManagedObject = this.mDataServiceProxy.executeSingleQuery(query);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "querySingleManageObject " + e.getMessage());
            }
            return aManagedObject;
        }
        checkReConnectOdmfService();
        OPCollectLog.e(TAG, "Odmf service is disconnceted, will reconnect later");
        return null;
    }

    public boolean updateManageObject(AManagedObject rawData) {
        boolean ret = false;
        if (rawData == null) {
            return ret;
        }
        if (this.hasConnected) {
            try {
                ret = this.mDataServiceProxy.executeUpdate(rawData);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "updateManageObject " + e.getMessage());
            }
            return ret;
        }
        checkReConnectOdmfService();
        OPCollectLog.w(TAG, "Odmf service is disconnceted, will reconnect later");
        return ret;
    }

    public boolean updateManageObjects(List<AManagedObject> rawDatas) {
        boolean ret = false;
        if (rawDatas == null || rawDatas.size() == 0) {
            return ret;
        }
        if (this.hasConnected) {
            try {
                ret = this.mDataServiceProxy.executeUpdate(rawDatas);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "updateManageObjects " + e.getMessage());
            }
            return ret;
        }
        checkReConnectOdmfService();
        OPCollectLog.w(TAG, "Odmf service is disconnceted, will reconnect later");
        return ret;
    }

    public boolean deleteManageObject(AManagedObject rawData) {
        boolean ret = false;
        if (rawData == null) {
            return ret;
        }
        if (this.hasConnected) {
            try {
                ret = this.mDataServiceProxy.executeDelete(rawData);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "deleteManageObjects " + e.getMessage());
            }
            return ret;
        }
        checkReConnectOdmfService();
        OPCollectLog.w(TAG, "Odmf service is disconnceted, will reconnect later");
        return ret;
    }

    public boolean deleteManageObjects(List<AManagedObject> rawDatas) {
        boolean ret = false;
        if (rawDatas == null) {
            return ret;
        }
        if (this.hasConnected) {
            try {
                ret = this.mDataServiceProxy.executeDelete(rawDatas);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "deleteManageObjects " + e.getMessage());
            }
            return ret;
        }
        checkReConnectOdmfService();
        OPCollectLog.w(TAG, "Odmf service is disconnceted, will reconnect later");
        return ret;
    }

    public boolean subscribeManagedObject(Class clazz, ObserverType type, ModelObserver observer) {
        boolean ret = false;
        if (observer == null) {
            return ret;
        }
        if (this.hasConnected) {
            try {
                ret = this.mDataServiceProxy.subscribe(clazz, type, observer);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "subscribeManagedObject " + e.getMessage());
            }
            return ret;
        }
        checkReConnectOdmfService();
        OPCollectLog.w(TAG, "Odmf service is disconnceted, will reconnect later");
        return ret;
    }

    public boolean unSubscribeManagedObject(Class clazz, ObserverType type, ModelObserver observer) {
        boolean ret = false;
        if (observer == null) {
            return ret;
        }
        if (this.hasConnected) {
            try {
                ret = this.mDataServiceProxy.unSubscribe(clazz, type, observer);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "unSubscribeManagedObject " + e.getMessage());
            }
            return ret;
        }
        checkReConnectOdmfService();
        OPCollectLog.w(TAG, "Odmf service is disconnceted, will reconnect later");
        return ret;
    }
}
