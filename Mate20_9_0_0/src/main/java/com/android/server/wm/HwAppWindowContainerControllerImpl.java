package com.android.server.wm;

import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import android.rms.iaware.AwareLog;
import android.view.IApplicationToken;
import com.android.server.AttributeCache.Entry;
import com.android.server.rms.iaware.feature.StartWindowFeature;

public class HwAppWindowContainerControllerImpl implements IHwAppWindowContainerController {
    private static final int RETURN_CONTINUE_STARTWINDOW = 0;
    private static final int RETURN_CONTINUE_STARTWINDOW_AND_UPDATE_TRANSFROM = 1;
    private static final int RETURN_NOT_CONTINUE_STARTWINDOW = -1;
    private static final String TAG = "HwAppWindowContainerControllerImpl";

    public boolean isHwStartWindowEnabled() {
        return StartWindowFeature.isStartWindowEnable();
    }

    public int continueHwStartWindow(String pkg, Entry ent, ApplicationInfo appInfo, boolean processRunning, boolean windowIsFloating, boolean windowIsTranslucent, boolean windowDisableStarting, boolean newTask, boolean taskSwitch, boolean windowShowWallpaper, IBinder transferFrom, IApplicationToken token, RootWindowContainer root) {
        boolean z = windowIsTranslucent;
        boolean z2 = windowDisableStarting;
        boolean notContinueStartWindow = true;
        boolean z3 = appInfo == null || windowIsFloating || token == null;
        if (z3) {
            return -1;
        }
        boolean z4 = (appInfo.isSystemApp() || appInfo.isPrivilegedApp() || appInfo.isUpdatedSystemApp()) ? false : true;
        if (z4) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addHwStartingWindow Translucent=");
            stringBuilder.append(z);
            stringBuilder.append(" DisableStarting=");
            stringBuilder.append(z2);
            stringBuilder.append(" processRunning=");
            boolean z5 = processRunning;
            stringBuilder.append(z5);
            AwareLog.i(str, stringBuilder.toString());
            updateStartWindowAppStatus(token.asBinder(), newTask, taskSwitch, z5, z2, z, windowShowWallpaper, pkg);
            return checkContinueHwStartWindow(pkg, processRunning, z, z2, transferFrom);
        }
        if (!(z || z2)) {
            notContinueStartWindow = false;
        }
        if (notContinueStartWindow) {
            return -1;
        }
        return 0;
    }

    private void updateStartWindowAppStatus(IBinder binder, boolean newTask, boolean taskSwitch, boolean processRunning, boolean windowDisableStarting, boolean windowIsTranslucent, boolean windowShowWallpaper, String pkg) {
        boolean isnewTaskAndNoStartWindow = newTask && taskSwitch && !processRunning && (windowDisableStarting || windowIsTranslucent || windowShowWallpaper);
        if (!(true ^ HwStartWindowRecord.getInstance().isStartWindowApp(pkg))) {
            return;
        }
        if (isnewTaskAndNoStartWindow) {
            HwStartWindowRecord.getInstance().updateStartWindowApp(pkg, binder);
        } else {
            HwStartWindowRecord.getInstance().resetStartWindowApp(pkg);
        }
    }

    private int checkContinueHwStartWindow(String pkg, boolean processRunning, boolean windowIsTranslucent, boolean windowDisableStarting, IBinder transferFrom) {
        if (!processRunning) {
            return 0;
        }
        boolean z = true;
        if (!HwStartWindowRecord.getInstance().checkStartWindowApp(pkg)) {
            if (!(windowIsTranslucent || windowDisableStarting)) {
                z = false;
            }
            if (z) {
                return -1;
            }
            return 0;
        } else if (transferFrom == null) {
            return 1;
        } else {
            return 0;
        }
    }

    public IBinder getTransferFrom(String pkg) {
        return HwStartWindowRecord.getInstance().getTransferFromStartWindowApp(pkg);
    }
}
