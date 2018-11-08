package com.huawei.android.view;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.rms.AppAssociate;
import android.util.Log;
import android.util.Singleton;
import android.view.IWindowManager;
import com.huawei.android.view.IHwWindowManager.Stub;
import java.util.List;

public class HwWindowManager {
    private static final Singleton<IHwWindowManager> IWindowManagerSingleton = new Singleton<IHwWindowManager>() {
        protected IHwWindowManager create() {
            try {
                return Stub.asInterface(IWindowManager.Stub.asInterface(ServiceManager.getService(AppAssociate.ASSOC_WINDOW)).getHwInnerService());
            } catch (RemoteException e) {
                Log.e(HwWindowManager.TAG, "IHwWindowManager create() fail: " + e);
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
            Log.e(TAG, "getSystemApps failed " + e.getMessage());
            return null;
        }
    }

    public static int getFocusWindowWidth() {
        try {
            return getService().getFocusWindowWidth();
        } catch (RemoteException e) {
            Log.e(TAG, "getFocusWindowWidth failed " + e.getMessage());
            return 0;
        }
    }

    public static void setRecentPosition(int x, int y, int width, int height) {
        try {
            getService().setRecentPosition(x, y, width, height);
        } catch (RemoteException e) {
            Log.e(TAG, "setRecentPosition failed " + e.getMessage());
        }
    }
}
