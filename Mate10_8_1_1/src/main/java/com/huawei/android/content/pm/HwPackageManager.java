package com.huawei.android.content.pm;

import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony.Sms.Intents;
import android.util.Log;
import android.util.Singleton;
import com.huawei.android.content.pm.IHwPackageManager.Stub;

public class HwPackageManager {
    private static final Singleton<IHwPackageManager> IPackageManagerSingleton = new Singleton<IHwPackageManager>() {
        protected IHwPackageManager create() {
            try {
                return Stub.asInterface(IPackageManager.Stub.asInterface(ServiceManager.getService(Intents.EXTRA_PACKAGE_NAME)).getHwInnerService());
            } catch (RemoteException e) {
                Log.e(HwPackageManager.TAG, "IHwPackageManager create() fail: " + e);
                return null;
            }
        }
    };
    private static final String TAG = "HwPackageManager";

    public static IHwPackageManager getService() {
        return (IHwPackageManager) IPackageManagerSingleton.get();
    }

    public static int getAppUseNotchMode(String packageName) {
        try {
            return getService().getAppUseNotchMode(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "getAppUseNotchMode failed " + e.getMessage());
            return -1;
        }
    }

    public static void setAppUseNotchMode(String packageName, int mode) {
        try {
            getService().setAppUseNotchMode(packageName, mode);
        } catch (RemoteException e) {
            Log.e(TAG, "setAppUseNotchMode failed " + e.getMessage());
        }
    }
}
