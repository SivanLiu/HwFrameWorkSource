package com.huawei.android.app;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.pm.IPackageDataObserver;
import android.content.res.Configuration;
import android.contentsensor.IActivityObserver;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.huawei.android.os.HwTransCodeEx;
import java.util.ArrayList;
import java.util.List;

public class ActivityManagerEx {
    public static int RECENT_IGNORE_HOME_AND_RECENTS_STACK_TASKS = 8;
    public static int RECENT_INCLUDE_PROFILES = 4;
    public static int RECENT_INGORE_DOCKED_STACK_TOP_TASK = 16;
    public static int RECENT_INGORE_PINNED_STACK_TASKS = 32;
    private static final String TAG = "ActivityManagerEx";

    public static class RunningAppProcessInfoEx {
        public static int FLAG_CANT_SAVE_STATE = 1;
        public static int FLAG_PERSISTENT = 2;
        public static int IMPORTANCE_CANT_SAVE_STATE = 270;
    }

    @Deprecated
    public static boolean isClonedProcess(int pid) {
        return false;
    }

    @Deprecated
    public static String getPackageNameForPid(int pid) {
        String str = null;
        try {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            data.writeInt(pid);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.GET_PACKAGE_NAME_FOR_PID_TRANSACTION, data, reply, 0);
            reply.readException();
            str = reply.readString();
            data.recycle();
            reply.recycle();
            return str;
        } catch (Exception e) {
            Log.e(TAG, "getPackageNameForPid", e);
            return str;
        }
    }

    public static int getFocusedStackId() throws RemoteException {
        return ActivityManagerNative.getDefault().getFocusedStackId();
    }

    public static int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    public static int checkComponentPermission(String permission, int uid, int owningUid, boolean exported) {
        return ActivityManager.checkComponentPermission(permission, uid, owningUid, exported);
    }

    public static void registerUserSwitchObserver(UserSwitchObserverEx observer, String name) throws RemoteException {
        ActivityManager.getService().registerUserSwitchObserver(observer.getUserSwitchObserver(), name);
    }

    public static void setProcessForeground(IBinder token, int pid, boolean isForeground) throws RemoteException {
        ActivityManager.getService().setProcessImportant(token, pid, isForeground, "");
    }

    public static Configuration getConfiguration() throws RemoteException {
        return ActivityManagerNative.getDefault().getConfiguration();
    }

    public static void updatePersistentConfiguration(Configuration values) throws RemoteException {
        ActivityManagerNative.getDefault().updatePersistentConfiguration(values);
    }

    public static void resumeAppSwitches() throws RemoteException {
        ActivityManagerNative.getDefault().resumeAppSwitches();
    }

    public static void stopAppSwitches() throws RemoteException {
        ActivityManagerNative.getDefault().stopAppSwitches();
    }

    public static List<RecentTaskInfo> getRecentTasksForUser(ActivityManager am, int maxNum, int flags, int userId) throws SecurityException {
        return am.getRecentTasksForUser(maxNum, flags, userId);
    }

    public static boolean startUserInBackground(int userId) throws RemoteException {
        return ActivityManagerNative.getDefault().startUserInBackground(userId);
    }

    public static boolean isUserRunning(int userId) throws RemoteException {
        return ActivityManagerNative.getDefault().isUserRunning(userId, 0);
    }

    public static long getLastActiveTime(RecentTaskInfo info) {
        return info != null ? info.lastActiveTime : 0;
    }

    public static long getUserId(RecentTaskInfo info) {
        return (long) (info != null ? info.userId : 0);
    }

    public static void switchUser(int userId) throws RemoteException {
        ActivityManagerNative.getDefault().switchUser(userId);
    }

    public static void registerActivityObserver(IActivityObserver observer) {
        IBinder iBinder = null;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            if (observer != null) {
                iBinder = observer.asBinder();
            }
            data.writeStrongBinder(iBinder);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.REGISTER_ACTIVITY_OBSERVER, data, reply, 0);
            reply.readException();
        } catch (Exception e) {
            Log.e(TAG, "registerActivityObserver ", e);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public static void unregisterActivityObserver(IActivityObserver observer) {
        IBinder iBinder = null;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            if (observer != null) {
                iBinder = observer.asBinder();
            }
            data.writeStrongBinder(iBinder);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.UNREGISTER_ACTIVITY_OBSERVER, data, reply, 0);
            reply.readException();
        } catch (Exception e) {
            Log.e(TAG, "registerActivityObserver ", e);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean requestContentNode(ComponentName componentName, Bundle bundle, int token) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean result = false;
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            if (componentName != null) {
                data.writeInt(1);
                componentName.writeToParcel(data, 0);
            } else {
                data.writeInt(0);
            }
            if (bundle != null) {
                data.writeInt(1);
                bundle.writeToParcel(data, 0);
            } else {
                data.writeInt(0);
            }
            data.writeInt(token);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.REQUEST_CONTENT, data, reply, 0);
            reply.readException();
            result = reply.readInt() != 0;
            data.recycle();
            reply.recycle();
        } catch (Exception e) {
            Log.e(TAG, "registerActivityObserver ", e);
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
        }
        return result;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean requestContentOther(ComponentName componentName, Bundle bundle, int token) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean result = false;
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            if (componentName != null) {
                data.writeInt(1);
                componentName.writeToParcel(data, 0);
            } else {
                data.writeInt(0);
            }
            if (bundle != null) {
                data.writeInt(1);
                bundle.writeToParcel(data, 0);
            } else {
                data.writeInt(0);
            }
            data.writeInt(token);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.REQUEST_CONTENT_OTHER, data, reply, 0);
            reply.readException();
            result = reply.readInt() != 0;
            data.recycle();
            reply.recycle();
        } catch (Exception e) {
            Log.e(TAG, "registerActivityObserver ", e);
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
        }
        return result;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean addGameSpacePackageList(List<String> packageList) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean result = false;
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            data.writeStringList(packageList);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.ADD_GAMESPACE_PACKAGE_LIST_TRANSACTION, data, reply, 0);
            reply.readException();
            result = reply.readInt() != 0;
            data.recycle();
            reply.recycle();
        } catch (RemoteException e) {
            Log.e(TAG, "addGameSpacePackageList error:" + e.getMessage());
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
        }
        return result;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean delGameSpacePackageList(List<String> packageList) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean result = false;
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            data.writeStringList(packageList);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.DEL_GAMESPACE_PACKAGE_LIST_TRANSACTION, data, reply, 0);
            reply.readException();
            result = reply.readInt() != 0;
            data.recycle();
            reply.recycle();
        } catch (RemoteException e) {
            Log.e(TAG, "delGameSpacePackageList error:" + e.getMessage());
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
        }
        return result;
    }

    public static void registerGameObserver(IGameObserver observer) {
        IBinder iBinder = null;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            if (observer != null) {
                iBinder = observer.asBinder();
            }
            data.writeStrongBinder(iBinder);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.REGISTER_GAME_OBSERVER_TRANSACTION, data, reply, 0);
            reply.readException();
        } catch (RemoteException e) {
            Log.e(TAG, "registerGameObserver error:" + e.getMessage());
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public static void unregisterGameObserver(IGameObserver observer) {
        IBinder iBinder = null;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            if (observer != null) {
                iBinder = observer.asBinder();
            }
            data.writeStrongBinder(iBinder);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.UNREGISTER_GAME_OBSERVER_TRANSACTION, data, reply, 0);
            reply.readException();
        } catch (RemoteException e) {
            Log.e(TAG, "unregisterGameObserver error:" + e.getMessage());
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isInGameSpace(String packageName) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean result = false;
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            data.writeString(packageName);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.IS_IN_GAMESPACE_TRANSACTION, data, reply, 0);
            reply.readException();
            result = reply.readInt() != 0;
            data.recycle();
            reply.recycle();
        } catch (RemoteException e) {
            Log.e(TAG, "isInGameSpace error:" + e.getMessage());
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
        }
        return result;
    }

    public static List<String> getGameList() {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        List<String> result = new ArrayList();
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.GET_GAMESPACE_LIST_TRANSACTION, data, reply, 0);
            reply.readException();
            reply.readStringList(result);
        } catch (RemoteException e) {
            Log.e(TAG, "getGameList error:" + e.getMessage());
        } finally {
            data.recycle();
            reply.recycle();
        }
        return result;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isGameDndOn() {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean result = false;
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.IS_GAME_DND_ON_TRANSACTION, data, reply, 0);
            reply.readException();
            result = reply.readInt() != 0;
            data.recycle();
            reply.recycle();
        } catch (RemoteException e) {
            Log.e(TAG, "isGameDndOn error:" + e.getMessage());
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
        }
        return result;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isGameKeyControlOn() {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean result = false;
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.IS_GAME_KEYCONTROL_ON_TRANSACTION, data, reply, 0);
            reply.readException();
            result = reply.readInt() != 0;
            data.recycle();
            reply.recycle();
        } catch (RemoteException e) {
            Log.e(TAG, "isGameKeyControlOn error:" + e.getMessage());
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
        }
        return result;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isGameGestureDisabled() {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean result = false;
        try {
            data.writeInterfaceToken(HwTransCodeEx.ACTIVITYMANAGER_DESCRIPTOR);
            ActivityManager.getService().asBinder().transact(HwTransCodeEx.IS_GAME_GESTURE_DISABLED_TRANSACTION, data, reply, 0);
            reply.readException();
            result = reply.readInt() != 0;
            data.recycle();
            reply.recycle();
        } catch (RemoteException e) {
            Log.e(TAG, "isGameGestureDisabled error:" + e.getMessage());
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
        }
        return result;
    }

    public static void registerProcessObserver(IProcessObserverEx observerEx) throws RemoteException {
        if (observerEx != null) {
            ActivityManager.getService().registerProcessObserver(observerEx.getIProcessObserver());
        }
    }

    public static void unregisterProcessObserver(IProcessObserverEx observerEx) throws RemoteException {
        if (observerEx != null) {
            ActivityManager.getService().unregisterProcessObserver(observerEx.getIProcessObserver());
        }
    }

    public static int startActivityFromRecents(int taskId, Bundle bOptions) {
        try {
            return ActivityManagerNative.getDefault().startActivityFromRecents(taskId, bOptions);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long[] getProcessPss(int[] pids) throws RemoteException {
        return ActivityManager.getService().getProcessPss(pids);
    }

    public static boolean clearApplicationUserData(String packageName, IPackageDataObserver observer) throws RemoteException {
        return ActivityManager.getService().clearApplicationUserData(packageName, observer, UserHandle.myUserId());
    }

    public static void forceStopPackage(String pkgname, int userId) throws RemoteException {
        ActivityManager.getService().forceStopPackage(pkgname, userId);
    }

    public static void registerHwActivityNotifier(IHwActivityNotifierEx notifier, String reason) {
        if (notifier != null) {
            HwActivityManager.registerHwActivityNotifier(notifier.getHwActivityNotifier(), reason);
        }
    }

    public static void unregisterHwActivityNotifier(IHwActivityNotifierEx notifier) {
        if (notifier != null) {
            HwActivityManager.unregisterHwActivityNotifier(notifier.getHwActivityNotifier());
        }
    }
}
