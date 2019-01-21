package com.huawei.android.app;

import android.app.ActivityManager;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.IMWThirdpartyCallback;
import android.os.IMWThirdpartyCallback.Stub;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import com.huawei.android.os.BuildEx.VERSION;
import com.huawei.android.os.HwTransCodeEx;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class HwMultiWindowEx {
    private static final String TAG = HwMultiWindowEx.class.getSimpleName();
    private static final int VERSION_NO = 1;
    private static Method sGetMultiWinFrameMethod = null;
    private static Method sGetServiceMethod;
    private static Method sIsInMultiWindowModeMethod = null;
    private static boolean sIsMWSupported = false;
    private static Method sIsMultiWinMethod = null;
    private static final Object sLock = new Object();
    private static ThirdpartyCallBackHandler sMWCallBackHandler;
    private static Object sMultiWinService;
    private static Method sRegisterThirdPartyCallBackMethod = null;
    private static boolean sRegistered = false;
    private static Class<?> sServiceManagerClazz;
    private static List<StateChangeListener> sStateChangeListeners;
    private static Method sUnregisterThirdPartyCallBackMethod = null;

    public interface StateChangeListener {
        void onModeChanged(boolean z);

        void onSizeChanged();

        void onZoneChanged();
    }

    private static class ThirdpartyCallBackHandler extends Stub {
        private ThirdpartyCallBackHandler() {
        }

        public void onModeChanged(boolean aMWStatus) {
            for (StateChangeListener lListener : HwMultiWindowEx.sStateChangeListeners) {
                lListener.onModeChanged(aMWStatus);
            }
        }

        public void onZoneChanged() {
            for (StateChangeListener lListener : HwMultiWindowEx.sStateChangeListeners) {
                lListener.onZoneChanged();
            }
        }

        public void onSizeChanged() {
            for (StateChangeListener lListener : HwMultiWindowEx.sStateChangeListeners) {
                lListener.onSizeChanged();
            }
        }
    }

    static {
        initDeclaredMethods();
    }

    private static void initDeclaredMethods() {
        String str;
        StringBuilder stringBuilder;
        Class<?>[] isMultiWinArgs = new Class[]{Integer.TYPE};
        Class<?>[] getMultiWinFrameArgs = new Class[]{Integer.TYPE, Rect.class};
        Class<?>[] mMWCallBackArgs = new Class[]{IMWThirdpartyCallback.class};
        try {
            Method asInterface = Class.forName("android.os.IMultiWinService$Stub").getMethod("asInterface", new Class[]{IBinder.class});
            sServiceManagerClazz = Class.forName("android.os.ServiceManager");
            sGetServiceMethod = sServiceManagerClazz.getMethod("getService", new Class[]{String.class});
            Object[] objArr = new Object[1];
            objArr[0] = sGetServiceMethod.invoke(sServiceManagerClazz, new Object[]{"multiwin"});
            sMultiWinService = asInterface.invoke(null, objArr);
            if (sMultiWinService != null) {
                sIsMWSupported = true;
                Class<?> clazz = sMultiWinService.getClass();
                sIsMultiWinMethod = clazz.getDeclaredMethod("isPartOfMultiWindow", isMultiWinArgs);
                sIsInMultiWindowModeMethod = clazz.getDeclaredMethod("getMWMaintained", (Class[]) null);
                sGetMultiWinFrameMethod = clazz.getDeclaredMethod("getMultiWinFrameByTaskID", getMultiWinFrameArgs);
                sRegisterThirdPartyCallBackMethod = clazz.getDeclaredMethod("registerThirdPartyCallBack", mMWCallBackArgs);
                sUnregisterThirdPartyCallBackMethod = clazz.getDeclaredMethod("unregisterThirdPartyCallBack", mMWCallBackArgs);
            }
        } catch (ClassNotFoundException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("initDeclaredMethods failed:");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
        } catch (NoSuchMethodException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("initDeclaredMethods failed:");
            stringBuilder.append(e2.toString());
            Log.e(str, stringBuilder.toString());
        } catch (IllegalAccessException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("initDeclaredMethods failed:");
            stringBuilder.append(e3.toString());
            Log.e(str, stringBuilder.toString());
        } catch (IllegalArgumentException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("initDeclaredMethods failed:");
            stringBuilder.append(e4.toString());
            Log.e(str, stringBuilder.toString());
        } catch (InvocationTargetException e5) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("initDeclaredMethods failed:");
            stringBuilder.append(e5.toString());
            Log.e(str, stringBuilder.toString());
        } catch (NullPointerException e6) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("initDeclaredMethods failed:");
            stringBuilder.append(e6.toString());
            Log.e(str, stringBuilder.toString());
        }
    }

    public static boolean isMultiWin(int aTaskID) {
        String str;
        StringBuilder stringBuilder;
        if (isInMultiWindowMode()) {
            Method method = sIsMultiWinMethod;
            if (method != null) {
                try {
                    return ((Boolean) method.invoke(sMultiWinService, new Object[]{Integer.valueOf(aTaskID)})).booleanValue();
                } catch (IllegalAccessException e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("call method ");
                    stringBuilder.append(method.getName());
                    stringBuilder.append(" failed !!!");
                    Log.d(str, stringBuilder.toString());
                } catch (IllegalArgumentException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("call method ");
                    stringBuilder.append(method.getName());
                    stringBuilder.append(" failed !!!");
                    Log.d(str, stringBuilder.toString());
                } catch (InvocationTargetException e3) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("call method ");
                    stringBuilder.append(method.getName());
                    stringBuilder.append(" failed !!!");
                    Log.d(str, stringBuilder.toString());
                }
            }
        }
        return false;
    }

    public static boolean isInMultiWindowMode() {
        String str;
        StringBuilder stringBuilder;
        if (VERSION.EMUI_SDK_INT >= 11) {
            return isInMultiWindowMode_N();
        }
        Method method = sIsInMultiWindowModeMethod;
        if (method != null) {
            try {
                return ((Boolean) method.invoke(sMultiWinService, (Object[]) null)).booleanValue();
            } catch (IllegalAccessException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            } catch (IllegalArgumentException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            } catch (InvocationTargetException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            }
        }
        return false;
    }

    public static Rect getMultiWinFrame(int aTaskID) {
        String str;
        StringBuilder stringBuilder;
        Rect lFrame = new Rect();
        Method method = sGetMultiWinFrameMethod;
        if (method != null) {
            try {
                method.invoke(sMultiWinService, new Object[]{Integer.valueOf(aTaskID), lFrame});
            } catch (IllegalAccessException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            } catch (IllegalArgumentException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            } catch (InvocationTargetException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            }
        }
        return lFrame;
    }

    public static boolean isMultiWindowSupported() {
        return sIsMWSupported;
    }

    public static boolean setStateChangeListener(StateChangeListener aStateChangeListener) {
        if (VERSION.EMUI_SDK_INT >= 11) {
            return setStateChangeListener_N(aStateChangeListener);
        }
        boolean lListenerAdded = false;
        if (isMultiWindowSupported()) {
            synchronized (sLock) {
                if (sStateChangeListeners == null) {
                    sStateChangeListeners = new ArrayList();
                }
                if (!sStateChangeListeners.contains(aStateChangeListener)) {
                    lListenerAdded = sStateChangeListeners.add(aStateChangeListener);
                }
            }
            if (!sRegistered) {
                sMWCallBackHandler = new ThirdpartyCallBackHandler();
                sRegistered = registerThirdPartyCallBack(sMWCallBackHandler);
            }
        }
        return lListenerAdded;
    }

    public static boolean unregisterStateChangeListener(StateChangeListener aStateChangeListener) {
        if (VERSION.EMUI_SDK_INT >= 11) {
            return unregisterStateChangeListener_N(aStateChangeListener);
        }
        boolean lListenerRemoved = false;
        if (sStateChangeListeners != null) {
            lListenerRemoved = sStateChangeListeners.remove(aStateChangeListener);
            if (sStateChangeListeners.size() == 0 && sMWCallBackHandler != null) {
                unregisterThirdPartyCallBack(sMWCallBackHandler);
                sRegistered = false;
            }
        }
        return lListenerRemoved;
    }

    private static boolean registerThirdPartyCallBack(IMWThirdpartyCallback aCallBackReference) {
        String str;
        StringBuilder stringBuilder;
        Method method = sRegisterThirdPartyCallBackMethod;
        if (method != null) {
            try {
                return ((Boolean) method.invoke(sMultiWinService, new Object[]{aCallBackReference})).booleanValue();
            } catch (IllegalAccessException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            } catch (IllegalArgumentException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            } catch (InvocationTargetException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            }
        }
        return false;
    }

    private static boolean unregisterThirdPartyCallBack(IMWThirdpartyCallback aCallBackReference) {
        String str;
        StringBuilder stringBuilder;
        Method method = sUnregisterThirdPartyCallBackMethod;
        if (method != null) {
            try {
                return ((Boolean) method.invoke(sMultiWinService, new Object[]{aCallBackReference})).booleanValue();
            } catch (IllegalAccessException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            } catch (IllegalArgumentException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            } catch (InvocationTargetException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("call method ");
                stringBuilder.append(method.getName());
                stringBuilder.append(" failed !!!");
                Log.d(str, stringBuilder.toString());
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:5:0x0043, code skipped:
            if (r2 != null) goto L_0x0045;
     */
    /* JADX WARNING: Missing block: B:6:0x0045, code skipped:
            r2.recycle();
     */
    /* JADX WARNING: Missing block: B:13:0x0058, code skipped:
            if (r2 != null) goto L_0x0045;
     */
    /* JADX WARNING: Missing block: B:14:0x005b, code skipped:
            if (r0 <= 0) goto L_?;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isInMultiWindowMode_N() {
        int ret = 0;
        Parcel data = null;
        Parcel reply = null;
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.IS_IN_MULTIWINDOW_MODE_TRANSACTION, data, reply, 0);
            reply.readException();
            ret = reply.readInt();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isInMultiWindowMode ret: ");
            stringBuilder.append(ret);
            Log.d(str, stringBuilder.toString());
            if (data != null) {
                data.recycle();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception happened", e);
            if (data != null) {
                data.recycle();
            }
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
        }
    }

    private static boolean setStateChangeListener_N(StateChangeListener aStateChangeListener) {
        boolean lListenerAdded = false;
        synchronized (sLock) {
            if (sStateChangeListeners == null) {
                sStateChangeListeners = new ArrayList();
            }
            if (!sStateChangeListeners.contains(aStateChangeListener)) {
                lListenerAdded = sStateChangeListeners.add(aStateChangeListener);
            }
        }
        if (!sRegistered) {
            sMWCallBackHandler = new ThirdpartyCallBackHandler();
            sRegistered = registerThirdPartyCallBack_N(sMWCallBackHandler);
        }
        return lListenerAdded;
    }

    private static boolean unregisterStateChangeListener_N(StateChangeListener aStateChangeListener) {
        boolean lListenerRemoved = false;
        if (sStateChangeListeners != null) {
            lListenerRemoved = sStateChangeListeners.remove(aStateChangeListener);
            if (sStateChangeListeners.size() == 0 && sMWCallBackHandler != null) {
                unregisterThirdPartyCallBack_N(sMWCallBackHandler);
                sRegistered = false;
            }
        }
        return lListenerRemoved;
    }

    private static boolean registerThirdPartyCallBack_N(IMWThirdpartyCallback aCallBackReference) {
        return HwActivityManager.registerThirdPartyCallBack(aCallBackReference);
    }

    private static boolean unregisterThirdPartyCallBack_N(IMWThirdpartyCallback aCallBackReference) {
        return HwActivityManager.unregisterThirdPartyCallBack(aCallBackReference);
    }
}
