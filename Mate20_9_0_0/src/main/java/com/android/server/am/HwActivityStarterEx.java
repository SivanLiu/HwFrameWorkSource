package com.android.server.am;

import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.iawareperf.UniPerf;
import android.os.SystemProperties;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.vrsystem.IVRSystemServiceManager;
import android.widget.Toast;
import com.android.server.UiThread;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.huawei.pgmng.log.LogPower;
import com.huawei.server.am.IHwActivityStarterEx;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.ArrayList;

public class HwActivityStarterEx implements IHwActivityStarterEx {
    public static final String TAG = "HwActivityStarterEx";
    private static final boolean mIsSupportGameAssist;
    final ActivityManagerService mService;
    private Toast mToast = null;

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.config.gameassist", 0) == 1) {
            z = true;
        }
        mIsSupportGameAssist = z;
    }

    public HwActivityStarterEx(ActivityManagerService service) {
        this.mService = service;
    }

    private String buildMsg(int canBoost, boolean isScreenOn) {
        boolean screenOn = isScreenOn;
        StringBuilder strMsg = new StringBuilder();
        strMsg.append("canBoost=");
        strMsg.append(canBoost);
        strMsg.append("|screenOn=");
        strMsg.append(screenOn);
        return strMsg.toString();
    }

    public void effectiveIawareToLaunchApp(Intent targetIntent, ActivityInfo targetAInfo, String curActivityPkName) {
        Intent intent = targetIntent;
        ActivityInfo activityInfo = targetAInfo;
        String str = curActivityPkName;
        if (intent != null && activityInfo != null) {
            String strPkg = "";
            String shortComponentName = "";
            String lastShortComponentName = null;
            if (targetIntent.getComponent() != null) {
                strPkg = targetIntent.getComponent().getPackageName();
                shortComponentName = targetIntent.getComponent().flattenToShortString();
            }
            if (!isAppHotStart(intent, activityInfo, this.mService.getRecentTasks().getRawTasks())) {
                this.mService.mDAProxy.notifyAppEventToIaware(3, strPkg);
            }
            boolean isScreenOn = false;
            BatteryStatsService mBatteryStatsService = this.mService.mBatteryStatsService;
            if (mBatteryStatsService != null) {
                isScreenOn = mBatteryStatsService.getActiveStatistics().isScreenOn();
            }
            int canBoost = this.mService.mHwAMSEx.canAppBoost(activityInfo, isScreenOn);
            String strMsg = buildMsg(canBoost, isScreenOn);
            ActivityRecord lastResumeActivity = this.mService.getLastResumedActivity();
            if (lastResumeActivity != null) {
                lastShortComponentName = lastResumeActivity.shortComponentName;
            }
            StringBuilder appEventStr = new StringBuilder();
            appEventStr.append(strPkg);
            boolean z = false;
            if (str == null || !str.equals(strPkg)) {
                boolean hasActivityInStack = false;
                if (this.mService.mWarmColdSwitch) {
                    hasActivityInStack = this.mService.mStackSupervisor.hasActivityInStackLocked(activityInfo);
                }
                if (hasActivityInStack) {
                    UniPerf.getInstance().uniPerfEvent(4399, strMsg, new int[0]);
                } else {
                    if (FingerViewController.PKGNAME_OF_KEYGUARD.equals(strPkg)) {
                        UniPerf.getInstance().uniPerfEvent(4098, strMsg, new int[0]);
                    } else {
                        UniPerf.getInstance().uniPerfEvent(4099, strMsg, new int[0]);
                    }
                }
                if (canBoost > 0) {
                    this.mService.mDAProxy.notifyAppEventToIaware(1, appEventStr.toString());
                }
                LogPower.push(CPUFeature.MSG_SET_FG_CGROUP, strPkg);
                return;
            }
            boolean diffComponent = shortComponentName.equals(lastShortComponentName) ^ 1;
            if (isScreenOn && diffComponent) {
                z = true;
            }
            sendUniperfEvent(4098, strMsg, z, appEventStr);
            if (canBoost > 0) {
                this.mService.mDAProxy.notifyAppEventToIaware(2, appEventStr.toString());
            }
        }
    }

    private void sendUniperfEvent(int uniperfCmdId, String extra, boolean needOnOff, StringBuilder appEventStr) {
        if (needOnOff) {
            appEventStr.append(":on");
            UniPerf.getInstance().uniPerfEvent(uniperfCmdId, extra, new int[]{0});
            return;
        }
        UniPerf.getInstance().uniPerfEvent(uniperfCmdId, extra, new int[0]);
    }

    private boolean isAppHotStart(Intent targetIntent, ActivityInfo targetAInfo, ArrayList<TaskRecord> recentTasks) {
        if (!(targetIntent == null || targetAInfo == null || recentTasks == null)) {
            if (!"android.intent.action.MAIN".equals(targetIntent.getAction()) || targetIntent.getCategories() == null || !targetIntent.getCategories().contains("android.intent.category.LAUNCHER")) {
                return true;
            }
            ComponentName cls = targetIntent.getComponent();
            int taskSize = recentTasks.size();
            int i = 0;
            while (i < taskSize) {
                TaskRecord task = (TaskRecord) recentTasks.get(i);
                Intent taskIntent = task.intent;
                Intent affinityIntent = task.affinityIntent;
                if ((task.rootAffinity == null || !task.rootAffinity.equals(targetAInfo.taskAffinity)) && ((taskIntent == null || taskIntent.getComponent() == null || taskIntent.getComponent().compareTo(cls) != 0) && (affinityIntent == null || affinityIntent.getComponent() == null || affinityIntent.getComponent().compareTo(cls) != 0))) {
                    i++;
                } else if (task.mActivities.size() > 0) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public boolean isAbleToLaunchInVR(Context context, String packageName) {
        IVRSystemServiceManager vrMananger = HwFrameworkFactory.getVRSystemServiceManager();
        if (packageName == null || context == null || vrMananger == null || !vrMananger.isVRMode()) {
            return true;
        }
        if (!vrMananger.isVRApplication(context, packageName) && !vrMananger.isVirtualScreenMode()) {
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

    public boolean isAbleToLaunchVideoActivity(Context context, Intent intent) {
        if (!mIsSupportGameAssist) {
            return true;
        }
        if (!HwSnsVideoManager.getInstance(context).getReadyToShowActivity(intent)) {
            return false;
        }
        HwSnsVideoManager.getInstance(this.mService.mContext).setReadyToShowActivity(false);
        return true;
    }

    /* JADX WARNING: Missing block: B:16:0x003c, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isAbleToLaunchInPCCastMode(String packageName, int displayId) {
        if (displayId == 0 || displayId == -1 || packageName == null || !HwPCUtils.isPcCastModeInServer() || !HwPCUtils.sPackagesCanStartedInPCMode.contains(packageName)) {
            return true;
        }
        HwPCUtils.log(TAG, "about to launch app which cannot be started in PC mode, abort.");
        final Context context = HwPCUtils.getDisplayContext(this.mService.mContext, HwPCUtils.getPCDisplayID());
        if (context != null) {
            UiThread.getHandler().post(new Runnable() {
                public void run() {
                    if (HwActivityStarterEx.this.mToast != null) {
                        HwActivityStarterEx.this.mToast.cancel();
                    }
                    HwActivityStarterEx.this.mToast = Toast.makeText(context, context.getResources().getString(33686015), 0);
                    HwActivityStarterEx.this.mToast.show();
                }
            });
        }
        return false;
    }
}
