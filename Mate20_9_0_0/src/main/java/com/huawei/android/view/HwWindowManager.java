package com.huawei.android.view;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Singleton;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import java.util.List;

public class HwWindowManager {
    private static final Singleton<IHwWindowManager> IWindowManagerSingleton = new Singleton<IHwWindowManager>() {
        protected IHwWindowManager create() {
            try {
                IWindowManager wms = Stub.asInterface(ServiceManager.getService("window"));
                if (wms == null) {
                    return null;
                }
                return IHwWindowManager.Stub.asInterface(wms.getHwInnerService());
            } catch (RemoteException e) {
                String str = HwWindowManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IHwWindowManager create() fail: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
    };
    public static final int NOTCH_MODE_ALWAYS = 1;
    private static final String TAG = "HwWindowManager";

    public static IHwWindowManager getService() {
        return (IHwWindowManager) IWindowManagerSingleton.get();
    }

    public static List<String> getNotchSystemApps() {
        try {
            return getService().getNotchSystemApps();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSystemApps failed ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    public static boolean registerWMMonitorCallback(IHwWMDAMonitorCallback callback) {
        if (getService() == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            getService().registerWMMonitorCallback(callback);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "registerWMMonitorCallback catch RemoteException!");
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static List<Bundle> getVisibleWindows(int ops) {
        if (getService() == null) {
            return null;
        }
        long token = Binder.clearCallingIdentity();
        List<Bundle> e;
        try {
            e = getService().getVisibleWindows(ops);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            Log.e(TAG, "getVisibleWindows catch RemoteException!");
            return null;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static int getFocusWindowWidth() {
        try {
            return getService().getFocusWindowWidth();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getFocusWindowWidth failed ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return 0;
        }
    }

    public static void startNotifyWindowFocusChange() {
        try {
            getService().startNotifyWindowFocusChange();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startNotifyWindowFocusChange failed ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public static void stopNotifyWindowFocusChange() {
        try {
            getService().stopNotifyWindowFocusChange();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopNotifyWindowFocusChange failed ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public static void getCurrFocusedWinInExtDisplay(Bundle outBundle) {
        try {
            getService().getCurrFocusedWinInExtDisplay(outBundle);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCurrFocusedWinInExtDisplay failed ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public static boolean hasLighterViewInPCCastMode() {
        try {
            return getService().hasLighterViewInPCCastMode();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hasLighterViewInPCCastMode failed ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public static boolean shouldDropMotionEventForTouchPad(float x, float y) {
        try {
            return getService().shouldDropMotionEventForTouchPad(x, y);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("shouldDropMotionEventForTouchPad failed ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public static HwTaskSnapshotWrapper getForegroundTaskSnapshotWrapper(boolean refresh) {
        try {
            return getService().getForegroundTaskSnapshotWrapper(refresh);
        } catch (RemoteException e) {
            Log.e(TAG, "getForegroundTaskSnapshotWrapper", e);
            return null;
        }
    }

    public static void setGestureNavMode(Context context, int leftMode, int rightMode, int bottomMode) {
        try {
            getService().setGestureNavMode(context.getPackageName(), leftMode, rightMode, bottomMode);
        } catch (RemoteException e) {
            Log.e(TAG, "setGestureNavMode failed", e);
        }
    }
}
