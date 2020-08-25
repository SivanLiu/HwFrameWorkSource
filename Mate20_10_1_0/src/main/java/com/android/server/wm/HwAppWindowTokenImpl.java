package com.android.server.wm;

import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.os.IBinder;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareLog;
import android.util.HwMwUtils;
import android.view.IApplicationToken;
import com.android.server.AttributeCache;
import com.android.server.rms.iaware.feature.StartWindowFeature;

public class HwAppWindowTokenImpl implements IHwAppWindowTokenEx {
    private static final int RETURN_CONTINUE_STARTWINDOW = 0;
    private static final int RETURN_CONTINUE_STARTWINDOW_AND_UPDATE_TRANSFROM = 1;
    private static final int RETURN_NOT_CONTINUE_STARTWINDOW = -1;
    private static final String TAG = "HwAppWindowTokenImpl";

    public boolean isHwStartWindowEnabled(String pkg, CompatibilityInfo compatInfo) {
        int appType;
        if (StartWindowFeature.isStartWindowEnable() && compatInfo.mAppInfo != null) {
            return (!(!compatInfo.mAppInfo.isSystemApp() && !compatInfo.mAppInfo.isPrivilegedApp() && !compatInfo.mAppInfo.isUpdatedSystemApp()) || (appType = AppTypeRecoManager.getInstance().getAppType(pkg)) == -1 || appType == 13 || appType == 28 || appType == 301 || appType == 309) ? false : true;
        }
    }

    public int continueHwStartWindow(String pkg, AttributeCache.Entry ent, ApplicationInfo appInfo, boolean processRunning, boolean windowIsFloating, boolean windowIsTranslucent, boolean windowDisableStarting, boolean newTask, boolean taskSwitch, boolean windowShowWallpaper, IBinder transferFrom, IApplicationToken token, RootWindowContainer root, boolean fromRecents) {
        if (windowIsFloating) {
            return -1;
        }
        if (token == null) {
            return -1;
        }
        AwareLog.i(TAG, "addHwStartingWindow Translucent=" + windowIsTranslucent + " DisableStarting=" + windowDisableStarting + " processRunning=" + processRunning);
        return updateStartWindowAppStatus(appInfo.uid, token.asBinder(), newTask, taskSwitch, processRunning, windowDisableStarting, windowIsTranslucent, windowShowWallpaper, transferFrom, root, fromRecents);
    }

    private int updateStartWindowAppStatus(int appUid, IBinder binder, boolean newTask, boolean taskSwitch, boolean processRunning, boolean windowDisableStarting, boolean windowIsTranslucent, boolean windowShowWallpaper, IBinder transferFrom, RootWindowContainer root, boolean fromRecents) {
        boolean notContinueStartWindow = true;
        if (!HwStartWindowRecord.getInstance().isStartWindowApp(Integer.valueOf(appUid))) {
            if (taskSwitch && (windowDisableStarting || windowIsTranslucent || windowShowWallpaper) && (HwStartWindowRecord.getInstance().getStartFromMainAction(Integer.valueOf(appUid)) || fromRecents)) {
                HwStartWindowRecord.getInstance().updateStartWindowApp(Integer.valueOf(appUid), binder);
                return 0;
            }
            HwStartWindowRecord.getInstance().resetStartWindowApp(Integer.valueOf(appUid));
        }
        if (!HwStartWindowRecord.getInstance().checkStartWindowApp(Integer.valueOf(appUid))) {
            if (!windowIsTranslucent && !windowDisableStarting) {
                notContinueStartWindow = false;
            }
            if (notContinueStartWindow) {
                return -1;
            }
        } else if (transferFrom == null) {
            return 1;
        }
        return 0;
    }

    public boolean isHwMwAnimationBelowStack(AppWindowToken appWindowToken) {
        if (HwMwUtils.ENABLED && appWindowToken != null && (appWindowToken.mActivityRecord instanceof HwActivityRecord) && appWindowToken.inHwMagicWindowingMode()) {
            HwActivityRecord hwActivityRecord = appWindowToken.mActivityRecord;
            boolean isExitInActivityOpen = !appWindowToken.mEnteringAnimation && appWindowToken.getTransit() == 6;
            boolean isEnterInActivityClose = appWindowToken.mEnteringAnimation && appWindowToken.getTransit() == 7;
            if (hwActivityRecord.mIsAniRunningBelow || isExitInActivityOpen || isEnterInActivityClose) {
                return true;
            }
        }
        return false;
    }

    public IBinder getTransferFrom(ApplicationInfo appInfo) {
        return HwStartWindowRecord.getInstance().getTransferFromStartWindowApp(Integer.valueOf(appInfo.uid));
    }
}
