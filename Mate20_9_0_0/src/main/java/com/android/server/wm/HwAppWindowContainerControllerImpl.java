package com.android.server.wm;

import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareLog;
import android.view.IApplicationToken;
import com.android.server.AttributeCache.Entry;
import com.android.server.rms.iaware.feature.StartWindowFeature;

public class HwAppWindowContainerControllerImpl implements IHwAppWindowContainerController {
    private static final int RETURN_CONTINUE_STARTWINDOW = 0;
    private static final int RETURN_CONTINUE_STARTWINDOW_AND_UPDATE_TRANSFROM = 1;
    private static final int RETURN_NOT_CONTINUE_STARTWINDOW = -1;
    private static final String TAG = "HwAppWindowContainerControllerImpl";

    public boolean isHwStartWindowEnabled(String pkg) {
        if (StartWindowFeature.isStartWindowEnable()) {
            int appType = AppTypeRecoManager.getInstance().getAppType(pkg);
            if (!(appType == -1 || appType == 13 || appType == 309)) {
                return true;
            }
        }
        return false;
    }

    public int continueHwStartWindow(String pkg, Entry ent, ApplicationInfo appInfo, boolean processRunning, boolean windowIsFloating, boolean windowIsTranslucent, boolean windowDisableStarting, boolean newTask, boolean taskSwitch, boolean windowShowWallpaper, IBinder transferFrom, IApplicationToken token, RootWindowContainer root) {
        boolean z;
        if (windowIsFloating || token == null) {
            ApplicationInfo applicationInfo = appInfo;
            z = windowIsTranslucent;
            return -1;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addHwStartingWindow Translucent=");
        z = windowIsTranslucent;
        stringBuilder.append(z);
        stringBuilder.append(" DisableStarting=");
        boolean z2 = windowDisableStarting;
        stringBuilder.append(z2);
        stringBuilder.append(" processRunning=");
        boolean z3 = processRunning;
        stringBuilder.append(z3);
        AwareLog.i(str, stringBuilder.toString());
        return updateStartWindowAppStatus(appInfo.uid, token.asBinder(), newTask, taskSwitch, z3, z2, z, windowShowWallpaper, transferFrom, root);
    }

    private int updateStartWindowAppStatus(int appUid, IBinder binder, boolean newTask, boolean taskSwitch, boolean processRunning, boolean windowDisableStarting, boolean windowIsTranslucent, boolean windowShowWallpaper, IBinder transferFrom, RootWindowContainer root) {
        boolean notContinueStartWindow = true;
        IBinder iBinder;
        if (HwStartWindowRecord.getInstance().isStartWindowApp(Integer.valueOf(appUid)) ^ true) {
            boolean isnewTaskAndNoStartWindow = newTask && taskSwitch && !processRunning && ((windowDisableStarting || windowIsTranslucent || windowShowWallpaper) && HwStartWindowRecord.getInstance().getStartFromMainAction(Integer.valueOf(appUid)));
            if (isnewTaskAndNoStartWindow) {
                HwStartWindowRecord.getInstance().updateStartWindowApp(Integer.valueOf(appUid), binder);
                return 0;
            }
            iBinder = binder;
            HwStartWindowRecord.getInstance().resetStartWindowApp(Integer.valueOf(appUid));
        } else {
            iBinder = binder;
        }
        if (!HwStartWindowRecord.getInstance().checkStartWindowApp(Integer.valueOf(appUid))) {
            if (!(windowIsTranslucent || windowDisableStarting)) {
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

    public IBinder getTransferFrom(ApplicationInfo appInfo) {
        return HwStartWindowRecord.getInstance().getTransferFromStartWindowApp(Integer.valueOf(appInfo.uid));
    }
}
