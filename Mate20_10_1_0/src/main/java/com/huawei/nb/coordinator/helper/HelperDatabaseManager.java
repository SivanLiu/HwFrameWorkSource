package com.huawei.nb.coordinator.helper;

import android.content.Context;
import com.huawei.nb.client.DataServiceProxy;
import com.huawei.nb.client.ServiceConnectCallback;
import com.huawei.nb.coordinator.NetWorkStateUtil;
import com.huawei.nb.coordinator.common.CoordinatorSwitchParameter;
import com.huawei.nb.model.coordinator.CoordinatorAudit;
import com.huawei.nb.model.coordinator.CoordinatorSwitch;
import com.huawei.nb.query.Query;
import com.huawei.nb.utils.logger.DSLog;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class HelperDatabaseManager {
    private static final String TAG = "HelperDatabaseManager";
    private static final long WAIT_FOR_CONNECT = 100;

    private HelperDatabaseManager() {
    }

    public static CoordinatorAudit createCoordinatorAudit(Context context) {
        CoordinatorAudit coordinatorAudit = new CoordinatorAudit();
        try {
            coordinatorAudit.setAppPackageName(context.getPackageName());
            coordinatorAudit.setUrl(" ");
            coordinatorAudit.setNetWorkState("" + NetWorkStateUtil.getCurrentNetWorkType(context));
            long timeStamp = System.currentTimeMillis();
            coordinatorAudit.setTimeStamp(Long.valueOf(timeStamp));
            coordinatorAudit.setRequestDate(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Long.valueOf(timeStamp)));
            coordinatorAudit.setSuccessTransferTime(0L);
            coordinatorAudit.setSuccessVerifyTime(Long.valueOf(timeStamp));
            coordinatorAudit.setDataSize(0L);
            coordinatorAudit.setIsNeedRetry(0L);
        } catch (Throwable e) {
            DSLog.e("HelperDatabaseManager caught a throwable when create CooxxxxxtorAxxxt." + e.getMessage(), new Object[0]);
        }
        return coordinatorAudit;
    }

    public static void insertCoordinatorAudit(Context context, CoordinatorAudit coordinatorAudit) {
        new Thread(new HelperDatabaseManager$$Lambda$0(context, coordinatorAudit)).start();
    }

    static final /* synthetic */ void lambda$insertCoordinatorAudit$0$HelperDatabaseManager(Context context, CoordinatorAudit coordinatorAudit) {
        try {
            DataServiceProxy dataServiceProxy = new DataServiceProxy(context);
            final CountDownLatch callbackWaiter = new CountDownLatch(1);
            dataServiceProxy.connect(new ServiceConnectCallback() {
                /* class com.huawei.nb.coordinator.helper.HelperDatabaseManager.AnonymousClass1 */

                @Override // com.huawei.nb.client.ServiceConnectCallback
                public void onConnect() {
                    callbackWaiter.countDown();
                }

                @Override // com.huawei.nb.client.ServiceConnectCallback
                public void onDisconnect() {
                }
            });
            try {
                if (callbackWaiter.await(WAIT_FOR_CONNECT, TimeUnit.MILLISECONDS)) {
                    DSLog.d("HelperDatabaseManager Success to connect DataService.", new Object[0]);
                    if (dataServiceProxy.executeInsert(coordinatorAudit) != null) {
                        DSLog.d("Success to insert to CooxxxxxtorAxxxt.", new Object[0]);
                    } else {
                        DSLog.e("Fail to insert to CooxxxxxtorAxxxt, error: insertedResInfo instanceof CooxxxxxtorAxxxt is false.", new Object[0]);
                    }
                    dataServiceProxy.disconnect();
                    return;
                }
                DSLog.e("HelperDatabaseManager Fail to connect DataService.", new Object[0]);
                dataServiceProxy.disconnect();
            } catch (InterruptedException e) {
                DSLog.e("Get Coordinator Service Flag InterruptedException:" + e.getMessage(), new Object[0]);
            }
        } catch (Throwable e2) {
            DSLog.e("HelperDatabaseManager caught a throwable when insert CooxxxxxtorAxxxt." + e2.getMessage(), new Object[0]);
        }
    }

    public static boolean getCoordinatorServiceFlag(Context context) {
        try {
            DataServiceProxy dataServiceProxy = new DataServiceProxy(context);
            final CountDownLatch callbackWaiter = new CountDownLatch(1);
            dataServiceProxy.connect(new ServiceConnectCallback() {
                /* class com.huawei.nb.coordinator.helper.HelperDatabaseManager.AnonymousClass2 */

                @Override // com.huawei.nb.client.ServiceConnectCallback
                public void onConnect() {
                    callbackWaiter.countDown();
                }

                @Override // com.huawei.nb.client.ServiceConnectCallback
                public void onDisconnect() {
                }
            });
            try {
                callbackWaiter.await();
            } catch (InterruptedException e) {
                DSLog.e("Get Coordinator Service Flag InterruptedException:" + e.getMessage(), new Object[0]);
            }
            List<CoordinatorSwitch> serviceSwitchList = dataServiceProxy.executeQuery(Query.select(CoordinatorSwitch.class).equalTo("serviceName", CoordinatorSwitchParameter.TRAVELASSISTANT));
            dataServiceProxy.disconnect();
            if (serviceSwitchList != null && serviceSwitchList.size() > 0) {
                return serviceSwitchList.get(0).getIsSwitchOn();
            }
        } catch (Throwable e2) {
            DSLog.e("HelperDatabaseManager caught a throwable when get the CoordinatorServiceFlag." + e2.getMessage(), new Object[0]);
        }
        return false;
    }
}
