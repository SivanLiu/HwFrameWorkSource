package com.huawei.android.app;

import android.app.StatusBarManager;
import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.IStatusBarService.Stub;
import com.android.internal.statusbar.NotificationVisibility;

public class StatusBarManagerEx {
    private static final int CODE_TRANSACT_CANCELPRELOAD_RECENT_APPS = 110;
    private static final int CODE_TRANSACT_PRELOAD_RECENT_APPS = 109;
    private static final int CODE_TRANSACT_TOGGLE_RECENT_APPS = 108;
    public static final int DISABLE_BACK = 4194304;
    public static final int DISABLE_EXPAND = 65536;
    public static final int DISABLE_HOME = 2097152;
    public static final int DISABLE_MASK = 67043328;
    public static final int DISABLE_NONE = 0;
    public static final int DISABLE_SEARCH = 33554432;

    public static void toggleRecentApps() throws RemoteException {
        transactToStatusBarManager(Stub.asInterface(ServiceManager.getService("statusbar")), 108);
    }

    public static void preloadRecentApps() throws RemoteException {
        transactToStatusBarManager(Stub.asInterface(ServiceManager.getService("statusbar")), 109);
    }

    public static void cancelPreloadRecentApps() throws RemoteException {
        transactToStatusBarManager(Stub.asInterface(ServiceManager.getService("statusbar")), 110);
    }

    private static boolean transactToStatusBarManager(IStatusBarService statusBarService, int code) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        if (statusBarService != null) {
            IBinder BarService = statusBarService.asBinder();
            if (BarService != null) {
                data.writeInterfaceToken("com.android.internal.statusbar.IStatusBarService");
                BarService.transact(code, data, reply, 0);
            }
        }
        if (data != null) {
            data.recycle();
        }
        if (reply != null) {
            reply.recycle();
        }
        return true;
    }

    public static int getDisableNoneFlag() {
        return 0;
    }

    public void disable(Context context, int what) {
        ((StatusBarManager) context.getSystemService("statusbar")).disable(what);
    }

    public static void expandNotificationsPanel(Context context) {
        ((StatusBarManager) context.getSystemService("statusbar")).expandNotificationsPanel();
    }

    public static void collapsePanels(Context context) {
        if (context != null) {
            ((StatusBarManager) context.getSystemService("statusbar")).collapsePanels();
        }
    }

    public static boolean isNotificationsPanelExpand(Context context) {
        if (context != null) {
            return ((StatusBarManager) context.getSystemService("statusbar")).isNotificationsPanelExpand();
        }
        return false;
    }

    public static void onNotificationClear(String pkg, String tag, int id, int userId, String key, int dismissalSurface, int rank, int count) throws RemoteException {
        String str = key;
        Stub.asInterface(ServiceManager.getService("statusbar")).onNotificationClear(pkg, tag, id, userId, str, dismissalSurface, NotificationVisibility.obtain(str, rank, count, true));
    }

    public static void onNotificationClick(String key, int rank, int count) throws RemoteException {
        Stub.asInterface(ServiceManager.getService("statusbar")).onNotificationClick(key, NotificationVisibility.obtain(key, rank, count, true));
    }

    public static void clearNotificationEffects() throws RemoteException {
        Stub.asInterface(ServiceManager.getService("statusbar")).clearNotificationEffects();
    }

    public static void onNotificationError(String pkg, String tag, int id, int uid, int initialPid, String message, int userId) throws RemoteException {
        Stub.asInterface(ServiceManager.getService("statusbar")).onNotificationError(pkg, tag, id, uid, initialPid, message, userId);
    }

    public static void onClearAllNotifications(int userId) throws RemoteException {
        Stub.asInterface(ServiceManager.getService("statusbar")).onClearAllNotifications(userId);
    }
}
