package com.android.server.am;

import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import com.android.server.wm.ConfigurationContainer;

public abstract class AbsActivityStackSupervisor extends ConfigurationContainer {
    protected static final int REPORT_DISPLAY_REMOVE_TIMEOUT_MSG = 10002;
    protected static final int REPORT_WINDOW_STATE_CHANGED_MSG = 10001;

    protected void recognitionMaliciousApp(IApplicationThread caller, Intent intet) {
    }

    boolean shouldNotKillProcWhenRemoveTask(String pkg) {
        return false;
    }

    protected boolean resumeAppLockActivityIfNeeded(ActivityStack stack, ActivityOptions targetOptions) {
        return false;
    }

    protected boolean isAppInLockList(String pkgName) {
        return false;
    }

    boolean hasActivityInStackLocked(ActivityInfo aInfo) {
        return false;
    }

    protected void uploadUnSupportSplitScreenAppPackageName(String pkgName) {
    }

    protected ActivityStack getTargetSplitTopStack(ActivityStack current) {
        return null;
    }

    protected ActivityStack getNextStackInSplitSecondary(ActivityStack current) {
        return null;
    }
}
