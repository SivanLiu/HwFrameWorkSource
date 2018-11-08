package com.android.server.am;

import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.iawareperf.UniPerf;
import android.util.HwVRUtils;
import android.vrsystem.IVRSystemServiceManager;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.huawei.pgmng.log.LogPower;
import com.huawei.server.am.IHwActivityStarterEx;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.ArrayList;

public class HwActivityStarterEx implements IHwActivityStarterEx {
    final ActivityManagerService mService;

    public HwActivityStarterEx(ActivityManagerService service) {
        this.mService = service;
    }

    public void effectiveIawareToLaunchApp(Intent targetIntent, ActivityInfo targetAInfo, String curActivityPkName) {
        if (targetIntent != null && targetAInfo != null) {
            String strPkg = "";
            if (targetIntent.getComponent() != null) {
                strPkg = targetIntent.getComponent().getPackageName();
            }
            if (!isAppHotStart(targetIntent, targetAInfo, this.mService.getRecentTasks())) {
                this.mService.notifyAppEventToIaware(3, strPkg);
            }
            int canBoost = this.mService.mHwAMSEx.canAppBoost(targetAInfo.applicationInfo);
            if (curActivityPkName == null || !(curActivityPkName.equals(strPkg) || (FingerViewController.PKGNAME_OF_KEYGUARD.equals(strPkg) ^ 1) == 0)) {
                if (canBoost > 0) {
                    boolean hasActivityInStack = false;
                    if (this.mService.mWarmColdSwitch) {
                        hasActivityInStack = this.mService.mStackSupervisor.hasActivityInStackLocked(targetAInfo);
                    }
                    if (hasActivityInStack) {
                        UniPerf.getInstance().uniPerfEvent(4399, "", new int[0]);
                    } else {
                        UniPerf.getInstance().uniPerfEvent(4099, "", new int[0]);
                    }
                    this.mService.notifyAppEventToIaware(1, strPkg);
                }
                LogPower.push(CPUFeature.MSG_SET_FG_CGROUP, strPkg);
            } else if (canBoost > 0) {
                UniPerf.getInstance().uniPerfEvent(4098, "", new int[0]);
                this.mService.notifyAppEventToIaware(2, strPkg);
            }
        }
    }

    private boolean isAppHotStart(Intent targetIntent, ActivityInfo targetAInfo, ArrayList<TaskRecord> recentTasks) {
        if (!(targetIntent == null || targetAInfo == null || recentTasks == null)) {
            boolean z;
            if (!"android.intent.action.MAIN".equals(targetIntent.getAction()) || targetIntent.getCategories() == null) {
                z = false;
            } else {
                z = targetIntent.getCategories().contains("android.intent.category.LAUNCHER");
            }
            if (!z) {
                return true;
            }
            ComponentName cls = targetIntent.getComponent();
            int taskSize = recentTasks.size();
            int i = 0;
            while (i < taskSize) {
                TaskRecord task = (TaskRecord) recentTasks.get(i);
                Intent taskIntent = task.intent;
                Intent affinityIntent = task.affinityIntent;
                if ((task.rootAffinity != null && task.rootAffinity.equals(targetAInfo.taskAffinity)) || ((taskIntent != null && taskIntent.getComponent() != null && taskIntent.getComponent().compareTo(cls) == 0) || (affinityIntent != null && affinityIntent.getComponent() != null && affinityIntent.getComponent().compareTo(cls) == 0))) {
                    return task.mActivities.size() > 0;
                } else {
                    i++;
                }
            }
        }
        return false;
    }

    public boolean isAbleToLaunchInVR(Context context, String packageName) {
        IVRSystemServiceManager vrMananger = HwFrameworkFactory.getVRSystemServiceManager();
        if (packageName == null || context == null || vrMananger == null || (vrMananger.isVRMode() ^ 1) != 0) {
            return true;
        }
        if (!vrMananger.isVRApplication(context, packageName) && (vrMananger.isVirtualScreenMode() ^ 1) != 0) {
            return false;
        }
        HwVRUtils.addVRLowPowerAppList(packageName);
        if ("com.huawei.vrvirtualscreen".equals(packageName)) {
            vrMananger.setVirtualScreenMode(true);
        } else if ("com.huawei.vrlauncherx".equals(packageName)) {
            vrMananger.setVirtualScreenMode(false);
        }
        return true;
    }
}
