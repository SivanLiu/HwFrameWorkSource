package com.huawei.android.app;

import android.app.ActivityManager;
import android.app.IHwActivityNotifier;
import android.os.IMWThirdpartyCallback;
import android.os.RemoteException;
import android.util.Log;
import android.util.Singleton;
import com.huawei.android.app.IHwActivityManager.Stub;
import java.util.List;
import java.util.Map;

public class HwActivityManager {
    private static final Singleton<IHwActivityManager> IActivityManagerSingleton = new Singleton<IHwActivityManager>() {
        protected IHwActivityManager create() {
            try {
                return Stub.asInterface(ActivityManager.getService().getHwInnerService());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    };
    private static final String TAG = "HwActivityManager";

    public static IHwActivityManager getService() {
        return (IHwActivityManager) IActivityManagerSingleton.get();
    }

    public static void registerDAMonitorCallback(IHwDAMonitorCallback callback) {
        try {
            getService().registerDAMonitorCallback(callback);
        } catch (RemoteException e) {
            Log.e(TAG, "registerDAMonitorCallback failed: catch RemoteException!");
        }
    }

    public static void setCpusetSwitch(boolean enable) {
        try {
            getService().setCpusetSwitch(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setCpusetSwitch failed: catch RemoteException!");
        }
    }

    public static void setWarmColdSwitch(boolean enable) {
        try {
            getService().setWarmColdSwitch(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setWarmColdSwitch failed: catch RemoteException!");
        }
    }

    public static boolean cleanPackageRes(List<String> packageList, Map alarmTags, int targetUid, boolean cleanAlarm, boolean isNative, boolean hasPerceptAlarm) {
        try {
            return getService().cleanPackageRes(packageList, alarmTags, targetUid, cleanAlarm, isNative, hasPerceptAlarm);
        } catch (RemoteException e) {
            Log.e(TAG, "cleanPackageRes failed: catch RemoteException!");
            return false;
        }
    }

    public static boolean registerThirdPartyCallBack(IMWThirdpartyCallback aCallBackHandler) {
        try {
            return getService().registerThirdPartyCallBack(aCallBackHandler);
        } catch (RemoteException e) {
            Log.e(TAG, "registerThirdPartyCallBack failed: catch RemoteException!");
            return false;
        }
    }

    public static boolean unregisterThirdPartyCallBack(IMWThirdpartyCallback aCallBackHandler) {
        try {
            return getService().unregisterThirdPartyCallBack(aCallBackHandler);
        } catch (RemoteException e) {
            Log.e(TAG, "registerThirdPartyCallBack failed: catch RemoteException!");
            return false;
        }
    }

    public static void reportScreenRecord(int uid, int pid, int status) {
        try {
            getService().reportScreenRecord(uid, pid, status);
        } catch (RemoteException e) {
            Log.e(TAG, "reportScreenRecord failed: catch RemoteException!");
        }
    }

    public static void registerHwActivityNotifier(IHwActivityNotifier notifier, String reason) {
        if (notifier != null) {
            try {
                getService().registerHwActivityNotifier(notifier, reason);
            } catch (RemoteException e) {
                Log.e(TAG, "registerHwActivityNotifier failed", e);
            }
        }
    }

    public static void unregisterHwActivityNotifier(IHwActivityNotifier notifier) {
        if (notifier != null) {
            try {
                getService().unregisterHwActivityNotifier(notifier);
            } catch (RemoteException e) {
                Log.e(TAG, "unregisterHwActivityNotifier failed", e);
            }
        }
    }

    public static void setActivityVisibleState(boolean state) {
        try {
            getService().setActivityVisibleState(state);
        } catch (RemoteException e) {
            Log.e(TAG, "setActivityVisibleState failed", e);
        }
    }

    public static void gestureToHome() {
        try {
            getService().gestureToHome();
        } catch (RemoteException e) {
            Log.e(TAG, "gestureToHome failed", e);
        }
    }
}
