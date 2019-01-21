package com.huawei.android.content.pm;

import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Singleton;
import com.huawei.android.content.pm.IHwPackageManager.Stub;

public class HwPackageManager {
    private static final Singleton<IHwPackageManager> IPackageManagerSingleton = new Singleton<IHwPackageManager>() {
        protected IHwPackageManager create() {
            try {
                return Stub.asInterface(IPackageManager.Stub.asInterface(ServiceManager.getService("package")).getHwInnerService());
            } catch (RemoteException e) {
                String str = HwPackageManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IHwPackageManager create() fail: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
    };
    private static final String TAG = "HwPackageManager";

    public static IHwPackageManager getService() {
        return (IHwPackageManager) IPackageManagerSingleton.get();
    }

    public static boolean isPerfOptEnable(String packageName, int optType) {
        try {
            return getService().isPerfOptEnable(packageName, optType);
        } catch (RemoteException e) {
            Log.e(TAG, "isPerfOptEnable failed: catch RemoteException!");
            return false;
        }
    }

    public static int getAppUseNotchMode(String packageName) {
        try {
            return getService().getAppUseNotchMode(packageName);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAppUseNotchMode failed ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return -1;
        }
    }

    public static void setAppUseNotchMode(String packageName, int mode) {
        try {
            getService().setAppUseNotchMode(packageName, mode);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAppUseNotchMode failed ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public static void setAppCanUninstall(String packageName, boolean canUninstall) {
        try {
            getService().setAppCanUninstall(packageName, canUninstall);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAppCanUninstall failed : packageName = ");
            stringBuilder.append(packageName);
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }
}
