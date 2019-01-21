package com.android.server.rms.iaware.hiber.util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import android.util.ArraySet;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.rms.iaware.hiber.bean.AbsAppInfo;
import com.android.server.rms.iaware.hiber.bean.HiberAppInfo;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AppHiberUtil {
    private static final int SINGLE_ONE = 1;

    public static ArraySet<HiberAppInfo> getHiberProcInfoListByAbsAppInfo(Context cxt, AbsAppInfo appInfo) {
        if (cxt == null || appInfo == null) {
            return null;
        }
        List<RunningAppProcessInfo> listTmp = ((ActivityManager) cxt.getSystemService("activity")).getRunningAppProcesses();
        if (listTmp == null || listTmp.isEmpty()) {
            return null;
        }
        ArraySet<HiberAppInfo> HiberProcInfoList = new ArraySet();
        for (RunningAppProcessInfo app : listTmp) {
            if (appInfo.mUid == app.uid) {
                if (app.pkgList != null) {
                    for (String str : app.pkgList) {
                        if (appInfo.mPkgName.equals(str)) {
                            HiberProcInfoList.add(new HiberAppInfo(app.uid, str, app.pid, app.processName));
                            break;
                        }
                    }
                }
            }
        }
        if (HiberProcInfoList.isEmpty()) {
            HiberProcInfoList = null;
        }
        return HiberProcInfoList;
    }

    public static boolean illegalHiberAppInfoArraySet(ArraySet<HiberAppInfo> tmp) {
        return tmp == null || tmp.isEmpty();
    }

    public static boolean illegalProcessInfo(ProcessInfo process) {
        if (process == null || process.mPackageName == null || 1 != process.mPackageName.size()) {
            return true;
        }
        return false;
    }

    public static boolean illegalUid(int uid) {
        if (uid < 10000) {
            return true;
        }
        return false;
    }

    public static boolean isStrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static boolean isSameProc(HiberAppInfo curProcess, HiberAppInfo hisProcess) {
        return curProcess.mPid == hisProcess.mPid && curProcess.mUid == hisProcess.mUid;
    }

    private static int[] intLitToArray(List<Integer> pidList) {
        if (pidList == null || pidList.isEmpty()) {
            return AppHibernateCst.EMPTY_INT_ARRAY;
        }
        int size = pidList.size();
        int[] pidArray = new int[size];
        for (int i = 0; i < size; i++) {
            pidArray[i] = ((Integer) pidList.get(i)).intValue();
        }
        return pidArray;
    }

    public static int[] getPidsFromList(ArraySet<HiberAppInfo> currentChildList) {
        List<Integer> pidList = new ArrayList();
        Iterator it = currentChildList.iterator();
        while (it.hasNext()) {
            HiberAppInfo curProcess = (HiberAppInfo) it.next();
            if (curProcess != null) {
                pidList.add(Integer.valueOf(curProcess.mPid));
            }
        }
        return intLitToArray(pidList);
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x0059  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x004f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int[] getDiffPidArray(ArraySet<HiberAppInfo> hisChildList, ArraySet<HiberAppInfo> currentChildList) {
        if (illegalHiberAppInfoArraySet(hisChildList)) {
            return getPidsFromList(currentChildList);
        }
        List<Integer> pidList = new ArrayList();
        long hisReclaimTime = 0;
        Iterator it = currentChildList.iterator();
        while (it.hasNext()) {
            HiberAppInfo curProcess = (HiberAppInfo) it.next();
            boolean needAdd = true;
            Iterator it2 = hisChildList.iterator();
            while (it2.hasNext()) {
                HiberAppInfo hisProcess = (HiberAppInfo) it2.next();
                if (isSameProc(curProcess, hisProcess)) {
                    hisReclaimTime = hisProcess.mReclaimTime;
                    needAdd = SystemClock.uptimeMillis() - hisReclaimTime >= AwareAppMngSort.PREVIOUS_APP_DIRCACTIVITY_DECAYTIME;
                    if (needAdd) {
                        curProcess.mReclaimTime = hisReclaimTime;
                    } else {
                        pidList.add(Integer.valueOf(curProcess.mPid));
                    }
                }
            }
            if (needAdd) {
            }
        }
        return intLitToArray(pidList);
    }

    public static boolean illegalAbsAppInfo(AbsAppInfo absApp) {
        return absApp == null || illegalUid(absApp.mUid) || isStrEmpty(absApp.mPkgName);
    }

    /* JADX WARNING: Missing block: B:27:0x0062, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isTheSameAppUnderMultiUser(String curPkg, AbsAppInfo srcApp) {
        boolean z = false;
        if (isStrEmpty(curPkg) || srcApp == null || !curPkg.equals(srcApp.mPkgName)) {
            return false;
        }
        int sysUserId = 0;
        ApplicationInfo appInfo = null;
        UserInfo ui = null;
        try {
            ui = ActivityManagerNative.getDefault().getCurrentUser();
        } catch (RemoteException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't get current user ID; guessing it's ");
            stringBuilder.append(0);
            AwareLog.e("AppHiber_Util", stringBuilder.toString());
        }
        if (ui != null) {
            sysUserId = ui.id;
        }
        if (UserHandle.getUserId(srcApp.mUid) != sysUserId) {
            return false;
        }
        try {
            appInfo = AppGlobals.getPackageManager().getApplicationInfo(curPkg, 0, sysUserId);
        } catch (RemoteException e2) {
            AwareLog.e("AppHiber_Util", "getApplicationInfo RemoteException");
        }
        if (appInfo != null && appInfo.uid == srcApp.mUid) {
            z = true;
        }
        return z;
    }
}
