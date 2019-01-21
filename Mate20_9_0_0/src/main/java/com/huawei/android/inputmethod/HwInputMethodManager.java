package com.huawei.android.inputmethod;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Singleton;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.IInputMethodManager.Stub;

public class HwInputMethodManager {
    private static final Singleton<IInputMethodManager> IInputManagerSingleton = new Singleton<IInputMethodManager>() {
        protected IInputMethodManager create() {
            return Stub.asInterface(ServiceManager.getService("input_method"));
        }
    };
    private static final Singleton<IHwInputMethodManager> IInputMethodManagerSingleton = new Singleton<IHwInputMethodManager>() {
        protected IHwInputMethodManager create() {
            try {
                return IHwInputMethodManager.Stub.asInterface(HwInputMethodManager.getInputMethodManagerService().getHwInnerService());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    };
    private static final String TAG = "HwInputMethodManager";

    public static IHwInputMethodManager getService() {
        return (IHwInputMethodManager) IInputMethodManagerSingleton.get();
    }

    public static void setDefaultIme(String imeId) {
        try {
            getService().setDefaultIme(imeId);
        } catch (RemoteException e) {
            Log.e(TAG, "setDefaultIme failed: catch RemoteException!");
        }
    }

    public static void setInputSource(boolean isFingerTouch) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setInputSource isFingerTouch = ");
        stringBuilder.append(isFingerTouch);
        Log.i(str, stringBuilder.toString());
        try {
            getService().setInputSource(isFingerTouch);
        } catch (RemoteException e) {
            Log.e(TAG, "setInputSource failed: catch RemoteException!");
        }
    }

    public static IInputMethodManager getInputMethodManagerService() {
        return (IInputMethodManager) IInputManagerSingleton.get();
    }
}
