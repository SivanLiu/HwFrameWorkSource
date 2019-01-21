package com.huawei.android.app;

import android.app.ActivityManager;
import android.app.IHwActivityNotifier;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IMWThirdpartyCallback;
import android.os.RemoteException;
import android.util.Log;
import android.util.Singleton;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.huawei.android.app.IHwActivityManager.Stub;
import java.util.ArrayList;
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
        if (getService() != null) {
            long token = Binder.clearCallingIdentity();
            try {
                getService().registerDAMonitorCallback(callback);
            } catch (RemoteException e) {
                Log.e(TAG, "registerDAMonitorCallback failed: catch RemoteException!");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    public static void setCpusetSwitch(boolean enable) {
        if (getService() != null) {
            long token = Binder.clearCallingIdentity();
            try {
                getService().setCpusetSwitch(enable);
            } catch (RemoteException e) {
                Log.e(TAG, "setCpusetSwitch failed: catch RemoteException!");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    public static void setWarmColdSwitch(boolean enable) {
        if (getService() != null) {
            long token = Binder.clearCallingIdentity();
            try {
                getService().setWarmColdSwitch(enable);
            } catch (RemoteException e) {
                Log.e(TAG, "setWarmColdSwitch failed: catch RemoteException!");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean cleanPackageRes(List<String> packageList, Map alarmTags, int targetUid, boolean cleanAlarm, boolean isNative, boolean hasPerceptAlarm) {
        if (getService() == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        boolean e;
        try {
            e = getService().cleanPackageRes(packageList, alarmTags, targetUid, cleanAlarm, isNative, hasPerceptAlarm);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            Log.e(TAG, "cleanPackageRes failed: catch RemoteException!");
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
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
        if (getService() != null) {
            long token = Binder.clearCallingIdentity();
            try {
                getService().reportScreenRecord(uid, pid, status);
            } catch (RemoteException e) {
                Log.e(TAG, "reportScreenRecord failed: catch RemoteException!");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean isFreeFormVisible() {
        try {
            return getService().isFreeFormVisible();
        } catch (RemoteException e) {
            Log.e(TAG, "isFreeFormVisible failed", e);
            return false;
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

    public static boolean handleANRFilterFIFO(int uid, int cmd) {
        try {
            return getService().handleANRFilterFIFO(uid, cmd);
        } catch (RemoteException e) {
            Log.e(TAG, "handleANRFilterFIFO failed", e);
            return false;
        }
    }

    public static void handleShowAppEyeAnrUi(int pid, int uid, String processName, String packageName) {
        try {
            getService().handleShowAppEyeAnrUi(pid, uid, processName, packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "handleShowAppEyeAnrUi failed", e);
        }
    }

    public static boolean isTaskVisible(int id) {
        try {
            return getService().isTaskVisible(id);
        } catch (RemoteException e) {
            Log.e(TAG, "isTaskVisible failed", e);
            return false;
        }
    }

    public static List<String> getPidWithUiFromUid(int uid) {
        if (getService() == null) {
            return null;
        }
        long token = Binder.clearCallingIdentity();
        List<String> e;
        try {
            e = getService().getPidWithUiFromUid(uid);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            Log.e(TAG, "getPidWithUiFromUid failed: catch RemoteException!");
            return new ArrayList();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static int preloadApplication(String packageName, int userId) {
        if (getService() == null) {
            return -1;
        }
        try {
            return getService().preloadApplication(packageName, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "preloadApplication failed: catch RemoteException!");
            return -1;
        }
    }

    public static boolean killProcessRecordFromIAware(ProcessInfo procInfo, boolean restartservice, boolean isAsynchronous, String reason) {
        if (getService() == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        boolean e;
        try {
            e = getService().killProcessRecordFromIAware(procInfo, restartservice, isAsynchronous, reason);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            Log.e(TAG, "killProcessRecordFromIAware failed: catch RemoteException!");
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean killProcessRecordFromIAwareNative(ProcessInfo procInfo, boolean restartservice, boolean isAsynchronous, String reason) {
        if (getService() == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        boolean e;
        try {
            e = getService().killProcessRecordFromIAwareNative(procInfo, restartservice, isAsynchronous, reason);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            Log.e(TAG, "killProcessRecordFromIAwareNative failed: catch RemoteException!");
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean killProcessRecordFromMTM(ProcessInfo procInfo, boolean restartservice, String reason) {
        if (getService() == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        boolean e;
        try {
            e = getService().killProcessRecordFromMTM(procInfo, restartservice, reason);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            Log.e(TAG, "killProcessRecordFromMTM failed: catch RemoteException!");
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean isProcessExistLocked(String processName, int uid) {
        if (getService() == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        boolean e;
        try {
            e = getService().isProcessExistLocked(processName, uid);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            Log.e(TAG, "isProcessExistLocked failed: catch RemoteException!");
            return true;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static void removePackageAlarm(String pkg, List<String> tags, int targetUid) {
        if (getService() != null) {
            long token = Binder.clearCallingIdentity();
            try {
                getService().removePackageAlarm(pkg, tags, targetUid);
            } catch (RemoteException e) {
                Log.e(TAG, "removePackageAlarm failed: catch RemoteException!");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    public static boolean getProcessRecordFromMTM(ProcessInfo procInfo) {
        if (getService() == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        boolean e;
        try {
            e = getService().getProcessRecordFromMTM(procInfo);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            Log.e(TAG, "getProcessRecordFromMTM failed: catch RemoteException!");
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static void setAndRestoreMaxAdjIfNeed(List<String> adjCustPkg) {
        if (getService() != null) {
            long token = Binder.clearCallingIdentity();
            try {
                getService().setAndRestoreMaxAdjIfNeed(adjCustPkg);
            } catch (RemoteException e) {
                Log.e(TAG, "setAndRestoreMaxAdjIfNeed failed: catch RemoteException!");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    public static void reportProcessDied(int pid) {
        if (getService() != null) {
            long token = Binder.clearCallingIdentity();
            try {
                getService().reportProcessDied(pid);
            } catch (RemoteException e) {
                Log.e(TAG, "reportProcessDied failed: catch RemoteException!");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    public static void reportAssocDisable() {
        if (getService() != null) {
            long token = Binder.clearCallingIdentity();
            try {
                getService().reportAssocDisable();
            } catch (RemoteException e) {
                Log.e(TAG, "reportAssocDisable failed: catch RemoteException!");
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
            Binder.restoreCallingIdentity(token);
        }
    }

    public static ActivityInfo getLastResumedActivity() {
        try {
            return getService().getLastResumedActivity();
        } catch (RemoteException e) {
            Log.e(TAG, "getLastResumedActivity failed", e);
            return null;
        }
    }

    public static boolean isProcessExistPidsSelfLocked(String processName, int uid) {
        if (getService() == null) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        boolean e;
        try {
            e = getService().isProcessExistPidsSelfLocked(processName, uid);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            Log.e(TAG, "isProcessExistPidsSelfLocked failed: catch RemoteException!");
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static int getTopTaskIdInDisplay(int displayId, String pkgName, boolean invisibleAlso) {
        try {
            return getService().getTopTaskIdInDisplay(displayId, pkgName, invisibleAlso);
        } catch (RemoteException e) {
            Log.e(TAG, "getTopTaskIdInDisplay failed", e);
            return -1;
        }
    }

    public static boolean isTaskSupportResize(int taskId, boolean isFullscreen, boolean isMaximized) {
        try {
            return getService().isTaskSupportResize(taskId, isFullscreen, isMaximized);
        } catch (RemoteException e) {
            Log.e(TAG, "isTaskSupportResize failed", e);
            return false;
        }
    }

    public static Rect getPCTopTaskBounds(int displayId) {
        try {
            return getService().getPCTopTaskBounds(displayId);
        } catch (RemoteException e) {
            Log.e(TAG, "getPCTopTaskBounds failed", e);
            return null;
        }
    }
}
