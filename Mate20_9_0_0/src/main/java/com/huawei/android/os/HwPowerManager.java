package com.huawei.android.os;

import android.os.IPowerManager;
import android.os.IPowerManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Singleton;

public class HwPowerManager {
    private static final Singleton<IHwPowerManager> IPowerManagerSingleton = new Singleton<IHwPowerManager>() {
        protected IHwPowerManager create() {
            try {
                IPowerManager pms = Stub.asInterface(ServiceManager.getService("power"));
                if (pms == null) {
                    return null;
                }
                return IHwPowerManager.Stub.asInterface(pms.getHwInnerService());
            } catch (RemoteException e) {
                String str = HwPowerManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IHwPowerManager create() fail: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
    };
    private static final String TAG = "HwPowerManager";

    public static IHwPowerManager getService() {
        return (IHwPowerManager) IPowerManagerSingleton.get();
    }

    public static boolean registerPowerMonitorCallback(IHwPowerDAMonitorCallback callback) {
        if (callback == null || getService() == null) {
            return false;
        }
        try {
            getService().registerPowerMonitorCallback(callback);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "registerPowerMonitorCallback catch RemoteException!");
            return false;
        }
    }

    public static void requestNoUserActivityNotification(int timeout) {
        try {
            getService().requestNoUserActivityNotification(timeout);
        } catch (RemoteException e) {
            Log.e(TAG, "requestUserInActivityNotification catch RemoteException!");
        }
    }
}
