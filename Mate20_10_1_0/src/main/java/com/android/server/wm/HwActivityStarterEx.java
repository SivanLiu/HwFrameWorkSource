package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.freeform.HwFreeFormUtils;
import android.hardware.display.DisplayManager;
import android.hdm.HwDeviceManager;
import android.iawareperf.UniPerf;
import android.os.Binder;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.widget.Toast;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.HwBluetoothBigDataService;
import com.android.server.UiThread;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.huawei.pgmng.log.LogPower;
import com.huawei.server.wm.IHwActivityStarterEx;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class HwActivityStarterEx implements IHwActivityStarterEx {
    private static final String PKG_PARENT_CONTROL = "com.huawei.parentcontrol";
    private static final boolean PRELOADAPP_EN = SystemProperties.getBoolean("persist.sys.appstart.preload.enable", false);
    public static final String TAG = "HwActivityStarterEx";
    private static final int WAIT_TIME = 10;
    private static final boolean mIsSupportGameAssist;
    private static Set<String> sPCPkgName = new HashSet();
    /* access modifiers changed from: private */
    public volatile boolean mNeedWait = true;
    final ActivityTaskManagerService mService;
    /* access modifiers changed from: private */
    public Toast mToast = null;

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.config.gameassist", 0) != 1) {
            z = false;
        }
        mIsSupportGameAssist = z;
        sPCPkgName.add("com.huawei.android.hwpay");
        sPCPkgName.add(HwBluetoothBigDataService.BIGDATA_RECEIVER_PACKAGENAME);
        sPCPkgName.add("com.huawei.screenrecorder");
        sPCPkgName.add("com.huawei.pcassistant");
        sPCPkgName.add("com.huawei.dmsdpdevice");
        sPCPkgName.add("com.huawei.associateassistant");
    }

    public HwActivityStarterEx(ActivityTaskManagerService service) {
        this.mService = service;
    }

    private String buildMsg(int canBoost, boolean isScreenOn) {
        return "canBoost=" + canBoost + "|screenOn=" + (isScreenOn ? 1 : 0);
    }

    public void effectiveIawareToLaunchApp(Intent targetIntent, ActivityInfo targetAInfo, String curActivityPkName) {
        if (targetIntent != null && targetAInfo != null) {
            String strPkg = "";
            String shortComponentName = "";
            String lastShortComponentName = null;
            if (targetIntent.getComponent() != null) {
                strPkg = targetIntent.getComponent().getPackageName();
                shortComponentName = targetIntent.getComponent().flattenToShortString();
            }
            if (!isAppHotStart(targetIntent, targetAInfo, this.mService.getRecentTasks().getRawTasks())) {
                this.mService.mAtmDAProxy.notifyAppEventToIaware(3, strPkg);
            }
            boolean isScreenOn = false;
            PowerManager pm = (PowerManager) this.mService.mContext.getSystemService("power");
            if (pm != null) {
                isScreenOn = pm.isScreenOn();
            }
            int canBoost = this.mService.mHwATMSEx.canAppBoost(targetAInfo, isScreenOn);
            String strMsg = buildMsg(canBoost, isScreenOn);
            ActivityRecord lastResumeActivity = this.mService.getLastResumedActivityRecord();
            if (lastResumeActivity != null) {
                lastShortComponentName = lastResumeActivity.shortComponentName;
            }
            StringBuilder appEventStr = new StringBuilder();
            appEventStr.append(strPkg);
            boolean z = false;
            if (curActivityPkName == null || !curActivityPkName.equals(strPkg)) {
                boolean hasActivityInStack = false;
                if (this.mService.mWarmColdSwitch) {
                    hasActivityInStack = this.mService.mStackSupervisor.hasActivityInStackLocked(targetAInfo);
                }
                if (hasActivityInStack) {
                    UniPerf.getInstance().uniPerfEvent(4399, strMsg, new int[0]);
                } else if (FingerViewController.PKGNAME_OF_KEYGUARD.equals(strPkg)) {
                    UniPerf.getInstance().uniPerfEvent(4098, strMsg, new int[0]);
                } else {
                    UniPerf.getInstance().uniPerfEvent(4099, strMsg, new int[0]);
                }
                if (canBoost > 0) {
                    this.mService.mAtmDAProxy.notifyAppEventToIaware(1, appEventStr.toString());
                }
                LogPower.push((int) CPUFeature.MSG_SET_FG_CGROUP, strPkg);
                return;
            }
            boolean diffComponent = !shortComponentName.equals(lastShortComponentName);
            if (isScreenOn && diffComponent) {
                z = true;
            }
            sendUniperfEvent(4098, strMsg, z, appEventStr);
            if (canBoost > 0) {
                this.mService.mAtmDAProxy.notifyAppEventToIaware(2, appEventStr.toString());
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
                TaskRecord task = recentTasks.get(i);
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

    public boolean isAbleToLaunchInVr(Context context, Intent intent, String callingPackage, ActivityInfo launchActivityInfo) {
        return this.mService.mVrMananger.isAbleToLaunchInVr(context, intent, callingPackage, launchActivityInfo);
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

    public boolean isAbleToLaunchInPCCastMode(String shortComponentName, int displayId, ActivityRecord reusedActivity) {
        if (((displayId == 0 || displayId == -1) && (reusedActivity == null || reusedActivity.getDisplayId() == 0 || reusedActivity.getDisplayId() == -1)) || shortComponentName == null || !HwPCUtils.isPcCastModeInServer()) {
            return true;
        }
        String[] tmp = shortComponentName.split("/");
        if (tmp.length != 2) {
            return true;
        }
        String packageName = tmp[0];
        boolean isAppLocked = HwActivityStartInterceptor.isAppLockActivity(shortComponentName);
        if (!HwPCUtils.sPackagesCanStartedInPCMode.contains(packageName) && !isAppLocked) {
            return true;
        }
        if (isAppLocked && reusedActivity != null) {
            try {
                forceStopPackageSync(reusedActivity);
            } catch (Exception e) {
                HwPCUtils.log(TAG, "Failed to kill app lock");
            }
        }
        showToast(displayId, false, isAppLocked);
        return false;
    }

    private boolean packageShouldNotHandle(String pkgName) {
        return HwPCUtils.enabledInPad() ? "com.huawei.desktop.explorer".equals(pkgName) || "com.huawei.desktop.systemui".equals(pkgName) || FingerViewController.PKGNAME_OF_KEYGUARD.equals(pkgName) || "com.android.incallui".equals(pkgName) || "com.huawei.android.wfdft".equals(pkgName) || PKG_PARENT_CONTROL.equals(pkgName) : HwPCUtils.isHiCarCastMode() ? "com.huawei.desktop.explorer".equals(pkgName) || "com.huawei.desktop.systemui".equals(pkgName) || "com.huawei.hicar".equals(pkgName) : "com.huawei.desktop.explorer".equals(pkgName) || "com.huawei.desktop.systemui".equals(pkgName);
    }

    private ArrayList<WindowProcessController> getProcessOnOtherDisplay(String pkg, int userId, int sourceDisplayId) {
        ArrayList<WindowProcessController> procs = new ArrayList<>();
        int NP = this.mService.mProcessNames.getMap().size();
        for (int ip = 0; ip < NP; ip++) {
            SparseArray<WindowProcessController> apps = (SparseArray) this.mService.mProcessNames.getMap().valueAt(ip);
            int NA = apps.size();
            for (int ia = 0; ia < NA; ia++) {
                WindowProcessController proc = apps.valueAt(ia);
                if (proc.mUserId == userId && proc != this.mService.mHomeProcess && ((proc.mPkgList.contains(pkg) || ("com.huawei.filemanager.desktopinstruction".equals(pkg) && proc.mName != null && proc.mName.equals(pkg))) && ((HwPCUtils.isValidExtDisplayId(sourceDisplayId) || HwPCUtils.isValidExtDisplayId(proc.mDisplayId)) && proc.mDisplayId != sourceDisplayId))) {
                    procs.add(proc);
                }
            }
        }
        return procs;
    }

    private boolean isConnectFromWeLink(int displayId) {
        Display display = ((DisplayManager) this.mService.mContext.getSystemService("display")).getDisplay(displayId);
        if (display != null && "com.huawei.works".equals(display.getOwnerPackageName())) {
            return true;
        }
        return false;
    }

    private void showToast(final int displayId, final boolean adaptInPCScreen, final boolean isAppLock) {
        final Context context;
        if (HwPCUtils.isPcCastModeInServer()) {
            if (HwPCUtils.isValidExtDisplayId(displayId)) {
                context = HwPCUtils.getDisplayContext(this.mService.mContext, displayId);
            } else {
                context = ((ActivityTaskManagerService) this.mService).mContext;
            }
            if (context != null) {
                UiThread.getHandler().post(new Runnable() {
                    /* class com.android.server.wm.HwActivityStarterEx.AnonymousClass1 */

                    public void run() {
                        if (HwActivityStarterEx.this.mToast != null) {
                            HwActivityStarterEx.this.mToast.cancel();
                        }
                        if (HwPCUtils.isValidExtDisplayId(displayId)) {
                            if (isAppLock) {
                                HwActivityStarterEx hwActivityStarterEx = HwActivityStarterEx.this;
                                Context context = context;
                                Toast unused = hwActivityStarterEx.mToast = Toast.makeText(context, context.getResources().getString(33686082), 0);
                            } else if (!adaptInPCScreen) {
                                HwActivityStarterEx hwActivityStarterEx2 = HwActivityStarterEx.this;
                                Context context2 = context;
                                Toast unused2 = hwActivityStarterEx2.mToast = Toast.makeText(context2, context2.getResources().getString(33686015), 0);
                            } else {
                                HwPCUtils.log(HwActivityStarterEx.TAG, "nothing to toast");
                            }
                        }
                        if (HwActivityStarterEx.this.mToast != null) {
                            HwActivityStarterEx.this.mToast.show();
                        }
                    }
                });
            }
        }
    }

    private int hasStartedOnOtherDisplay(ActivityRecord startActivity, int sourceDisplayId) {
        ArrayList<WindowProcessController> list;
        if (HwPCUtils.isPcCastModeInServer() && !sPCPkgName.contains(startActivity.packageName)) {
            String activityName = startActivity.mActivityComponent != null ? startActivity.mActivityComponent.getClassName() : "";
            if (packageShouldNotHandle(startActivity.packageName) && !"com.huawei.filemanager.desktopinstruction.EasyProjection".equals(activityName)) {
                return -1;
            }
            if (HwPCUtils.isValidExtDisplayId(sourceDisplayId) && startActivity.isActivityTypeHome()) {
                return 2;
            }
            if ((!HwPCUtils.isValidExtDisplayId(sourceDisplayId) || !HwPCUtils.isHiCarCastMode()) && hasTopActivityOnHiCarDisplay(startActivity.packageName)) {
                if ("com.huawei.filemanager.desktopinstruction.EasyProjection".equals(activityName)) {
                    list = getProcessOnOtherDisplay("com.huawei.filemanager.desktopinstruction", startActivity.mUserId, sourceDisplayId);
                } else {
                    list = getProcessOnOtherDisplay(startActivity.packageName, startActivity.mUserId, sourceDisplayId);
                }
                if (list != null) {
                    int size = list.size();
                    int i = 0;
                    while (i < size) {
                        if (!list.get(i).hasForegroundActivities() || HwPCUtils.enabledInPad() || HwActivityStartInterceptor.isAppLockActivity(startActivity.shortComponentName) || ("com.huawei.works".equals(startActivity.packageName) && HwPCUtils.isValidExtDisplayId(sourceDisplayId))) {
                            i++;
                        } else {
                            HwPCUtils.showDialogForSwitchDisplay(sourceDisplayId, startActivity.packageName);
                            if (sourceDisplayId == 0) {
                                return 1;
                            }
                            return 0;
                        }
                    }
                }
            }
            if (HwPCUtils.isHiCarCastMode() && HwPCUtils.isValidExtDisplayId(sourceDisplayId)) {
                startActivity.mShowWhenLocked = true;
            }
            this.mService.mHwATMSEx.getPkgDisplayMaps().put(startActivity.packageName, Integer.valueOf(sourceDisplayId));
        }
        return -1;
    }

    private boolean hasTopActivityOnHiCarDisplay(String pkgName) {
        if (!HwPCUtils.isHiCarCastMode()) {
            return true;
        }
        Optional<String> topPkgName = Optional.ofNullable(this.mService.getRootActivityContainer().getActivityDisplay(HwPCUtils.getPCDisplayID())).map($$Lambda$HwActivityStarterEx$acOL8KCZ6yIs_CbIVhBwdx9zM6M.INSTANCE).map($$Lambda$HwActivityStarterEx$eDBLZR9SLmQ0NA7ufbP9KcllCPY.INSTANCE).map($$Lambda$HwActivityStarterEx$HkPfId3HfP6HrH7s_t3B0IBWFQs.INSTANCE);
        if (topPkgName == null || !topPkgName.isPresent() || topPkgName.get().equals(pkgName)) {
            return true;
        }
        HwPCUtils.log(TAG, "Top activity on PC display is not " + pkgName);
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00ea, code lost:
        if (com.android.server.wm.HwActivityStarterEx.sPCPkgName.contains(r13.packageName) != false) goto L_0x0110;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00f4, code lost:
        if ("com.huawei.works".equals(r13.packageName) == false) goto L_0x00fa;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00f6, code lost:
        killOrMoveProcess(r14, r13);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00fa, code lost:
        forceStopPackageSync(r13);
        com.huawei.server.HwPCFactory.getHwPCFactory().getHwPCFactoryImpl().getHwPCDataReporter().reportKillProcessEvent(r13.packageName, r2, r14, r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:?, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:?, code lost:
        return true;
     */
    private boolean killProcessOnOtherDisplay(ActivityRecord startActivity, int sourceDisplayId) {
        if (HwPCUtils.isPcCastModeInServer()) {
            String activityName = startActivity.mActivityComponent != null ? startActivity.mActivityComponent.getClassName() : "";
            if (packageShouldNotHandle(startActivity.packageName) && !"com.huawei.filemanager.desktopinstruction.EasyProjection".equals(activityName)) {
                return false;
            }
            if ("com.huawei.filemanager.desktopinstruction.EasyProjection".equals(activityName)) {
                ArrayList<WindowProcessController> list = getProcessOnOtherDisplay("com.huawei.filemanager.desktopinstruction", startActivity.mUserId, sourceDisplayId);
                int size = list.size();
                boolean isRemoved = false;
                for (int i = 0; i < size; i++) {
                    Process.killProcess(list.get(i).mPid);
                    isRemoved = true;
                }
                if (isRemoved) {
                    Set<String> disabledClasses = new HashSet<>();
                    disabledClasses.add("com.huawei.filemanager.desktopinstruction.EasyProjection");
                    this.mService.mRootActivityContainer.finishDisabledPackageActivities("com.huawei.desktop.explorer", disabledClasses, true, false, UserHandle.getUserId(startActivity.appInfo.uid));
                }
                return isRemoved;
            }
            ArrayList<WindowProcessController> list2 = getProcessOnOtherDisplay(startActivity.packageName, startActivity.mUserId, sourceDisplayId);
            int N = list2.size();
            int i2 = 0;
            while (true) {
                if (i2 >= N) {
                    break;
                }
                if (HwPCUtils.enabledInPad() && "com.android.settings".equals(startActivity.packageName) && "com.android.phone".equals(list2.get(i2).mName)) {
                    HwPCUtils.log(TAG, "settings in phone process");
                } else if (TextUtils.isEmpty(startActivity.packageName) || !startActivity.packageName.contains("com.tencent.mm") || startActivity.app == null || startActivity.app.mPid == list2.get(i2).mPid) {
                    String processName = list2.get(i2).mName;
                    int targetDisplayId = list2.get(i2).mDisplayId;
                } else {
                    HwPCUtils.log(TAG, "pid is not same so dont kill the process when killing Process on other display");
                }
                i2++;
            }
        }
        return false;
    }

    private void killOrMoveProcess(int sourceDisplayId, ActivityRecord startActivity) {
        HwPCUtils.log(TAG, "killOrMoveProcess sourceDisplayId:" + sourceDisplayId);
        ArrayList<WindowProcessController> list = getProcessOnOtherDisplay(startActivity.packageName, startActivity.mUserId, sourceDisplayId);
        int N = list.size();
        for (int i = 0; i < N; i++) {
            WindowProcessController pr = list.get(i);
            if (pr.mActivities.size() > 0) {
                ActivityRecord record = (ActivityRecord) pr.mActivities.get(0);
                if (record != null) {
                    record.getActivityStack().finishAllActivitiesLocked(true);
                    Process.killProcess(pr.getPid());
                }
            } else if ("com.huawei.works".equals(pr.mName)) {
                Process.killProcess(pr.getPid());
            }
            pr.mDisplayId = sourceDisplayId;
        }
    }

    private boolean killProcessOnDefaultDisplay(ActivityRecord startActivity) {
        if (!HwPCUtils.enabled()) {
            return false;
        }
        boolean killPackageProcess = false;
        ArrayList<WindowProcessController> list = getProcessOnOtherDisplay(startActivity.packageName, startActivity.mUserId, 0);
        int N = list.size();
        int i = 0;
        while (true) {
            if (i >= N) {
                break;
            } else if (HwPCUtils.isValidExtDisplayId(list.get(i).mDisplayId)) {
                killPackageProcess = true;
                break;
            } else {
                i++;
            }
        }
        if (!killPackageProcess) {
            return false;
        }
        if ("com.huawei.works".equals(startActivity.packageName)) {
            killOrMoveProcess(0, startActivity);
            return true;
        }
        forceStopPackageSync(startActivity);
        return true;
    }

    public Bundle checkActivityStartedOnDisplay(ActivityRecord startActivity, int preferredDisplayId, ActivityOptions options, ActivityRecord reusedActivity) {
        ActivityRecord reusedActivity2 = reusedActivity;
        ActivityStack sourceStack = null;
        if (startActivity == null) {
            Slog.i(TAG, "startActivityUnchecked reusedActivity :" + reusedActivity2);
            return null;
        } else if (HwPCUtils.isPcCastModeInServer()) {
            Bundle ret = new Bundle();
            ret.putInt("startResult", 0);
            ret.putBoolean("skipReuse", false);
            ret.putBoolean("skipStart", false);
            if (!isAbleToLaunchInPCCastMode(startActivity.shortComponentName, preferredDisplayId, reusedActivity2)) {
                ret.putBoolean("skipStart", true);
                ret.putInt("startResult", 102);
                return ret;
            }
            int startedResult = hasStartedOnOtherDisplay(startActivity, preferredDisplayId);
            if (startedResult != -1) {
                ActivityOptions.abort(options);
                if (startActivity.resultTo != null) {
                    sourceStack = startActivity.resultTo.getActivityStack();
                }
                if (sourceStack != null) {
                    sourceStack.sendActivityResultLocked(-1, startActivity.resultTo, startActivity.resultWho, startActivity.requestCode, 0, (Intent) null);
                }
                if (startedResult == 1) {
                    ret.putInt("startResult", 99);
                    ret.putBoolean("skipStart", true);
                    return ret;
                } else if (startedResult == 0) {
                    ret.putInt("startResult", 98);
                    ret.putBoolean("skipStart", true);
                    return ret;
                } else {
                    ret.putBoolean("skipStart", true);
                    return ret;
                }
            } else {
                if (killProcessOnOtherDisplay(startActivity, preferredDisplayId)) {
                    reusedActivity2 = null;
                    ret.putBoolean("skipReuse", true);
                }
                if (HwPCUtils.enabledInPad() && reusedActivity2 != null && HwPCUtils.isValidExtDisplayId(preferredDisplayId) && reusedActivity2.getDisplayId() != preferredDisplayId) {
                    reusedActivity2 = null;
                    ret.putBoolean("skipReuse", true);
                    Slog.i(TAG, "reusedActivity is not in PCdisplay");
                }
                if (reusedActivity2 != null && !HwPCUtils.isValidExtDisplayId(reusedActivity2.getDisplayId()) && ((HwPCUtils.enabledInPad() && ("com.android.systemui/.settings.BrightnessDialog".equals(reusedActivity2.shortComponentName) || "com.android.incallui".equals(reusedActivity2.packageName) || "com.huawei.android.wfdft".equals(reusedActivity2.packageName))) || (HwPCUtils.isHiCarCastMode() && "com.android.incallui".equals(reusedActivity2.packageName)))) {
                    if (Log.HWINFO) {
                        Slog.i(TAG, "startActivityUnchecked reusedActivity :" + reusedActivity2);
                    }
                    ret.putBoolean("skipReuse", true);
                }
                return ret;
            }
        } else if (!killProcessOnDefaultDisplay(startActivity)) {
            return null;
        } else {
            Bundle ret2 = new Bundle();
            ret2.putBoolean("skipReuse", true);
            return ret2;
        }
    }

    public boolean checkActivityStartForPCMode(ActivityOptions options, ActivityRecord startActivity, ActivityStack targetStack) {
        if (HwPCUtils.isPcCastModeInServer()) {
            if (startActivity == null || targetStack == null) {
                HwPCUtils.log(TAG, "null params, return true for checkActivityStartForPCMode");
                return true;
            } else if (hasStartedOnOtherDisplay(startActivity, targetStack.mDisplayId) != -1) {
                ActivityOptions.abort(options);
                ActivityStack sourceStack = startActivity.resultTo != null ? startActivity.resultTo.getActivityStack() : null;
                if (sourceStack != null) {
                    sourceStack.sendActivityResultLocked(-1, startActivity.resultTo, startActivity.resultWho, startActivity.requestCode, 0, (Intent) null);
                }
                if (!Log.HWINFO) {
                    return false;
                }
                HwPCUtils.log(TAG, "cancel activity start, act:" + startActivity + " targetStack:" + targetStack);
                return false;
            }
        }
        return true;
    }

    private void forceStopPackageSync(final ActivityRecord r) {
        HwPCUtils.log(TAG, "forceStopPackageSync, enter.");
        this.mNeedWait = true;
        new Thread(new Runnable() {
            /* class com.android.server.wm.HwActivityStarterEx.AnonymousClass2 */

            public void run() {
                Process.setThreadPriority(-2);
                try {
                    HwPCUtils.log(HwActivityStarterEx.TAG, "AMS forceStopPackage, start.");
                    if (r == null || r.appInfo == null) {
                        HwPCUtils.log(HwActivityStarterEx.TAG, "AMS forceStopPackage break, activity record is null");
                    } else {
                        ActivityManager.getService().forceStopPackage(r.packageName, UserHandle.getUserId(r.appInfo.uid));
                    }
                    HwPCUtils.log(HwActivityStarterEx.TAG, "AMS forceStopPackage, end.");
                } catch (RemoteException e) {
                    if (Log.HWINFO) {
                        HwPCUtils.log(HwActivityStarterEx.TAG, "Failed to kill aps package of " + r.packageName);
                    }
                }
                boolean unused = HwActivityStarterEx.this.mNeedWait = false;
                synchronized (HwActivityStarterEx.this.mService.getGlobalLock()) {
                    HwActivityStarterEx.this.mService.getGlobalLock().notifyAll();
                }
            }
        }).start();
        while (this.mNeedWait) {
            try {
                this.mService.getGlobalLock().wait(10);
            } catch (InterruptedException e) {
                HwPCUtils.log(TAG, "forceStopPackageSync, Failed to mGlobalLock.wait.");
            }
        }
        HwPCUtils.log(TAG, "forceStopPackageSync, exit.");
    }

    public void handleFreeFormStackIfNeed(ActivityRecord startActivity) {
        ActivityStack freeFormStack;
        if (HwFreeFormUtils.isFreeFormEnable() && (freeFormStack = this.mService.getRootActivityContainer().getStack(5, 1)) != null) {
            ActivityRecord topActivity = freeFormStack.topRunningActivityLocked();
            if (topActivity != null && startActivity.launchedFromPackage != null && startActivity.launchedFromPackage.equals(topActivity.packageName)) {
                HwFreeFormUtils.log(TAG, "start activity:" + startActivity + " from:" + startActivity.launchedFromPackage + " in pkg:" + startActivity.packageName + " freeform topActivity.pkg:" + topActivity.packageName);
                if (freeFormStack.getCurrentPkgUnderFreeForm().equals(startActivity.packageName)) {
                    HwFreeFormUtils.log(TAG, "launch under-freeform app from the same app as freeform-app in fullscreen");
                } else if (!topActivity.packageName.equals(startActivity.packageName)) {
                    freeFormStack.setCurrentPkgUnderFreeForm("");
                    freeFormStack.setFreeFormStackVisible(false);
                    ActivityDisplay defaultDisplay = this.mService.mRootActivityContainer.getDefaultDisplay();
                    ActivityStack toStack = defaultDisplay != null ? defaultDisplay.getTopStackInWindowingMode(1) : null;
                    if (toStack == null) {
                        HwFreeFormUtils.log(TAG, "toStack is null, interrupt move freeform");
                        return;
                    }
                    HwFreeFormUtils.log(TAG, "move freeform to fullscreen for launch other app from freeform stack");
                    topActivity.getTaskRecord().reparent(toStack, true, 1, true, false, "exitFreeformMode");
                }
            } else if (topActivity != null && topActivity.packageName.equals(startActivity.packageName) && startActivity.getWindowingMode() == 1) {
                HwFreeFormUtils.log(TAG, "keep freeform for launch the same app as freeform-app in fullscreen stack");
            } else if (!"".equals(freeFormStack.getCurrentPkgUnderFreeForm()) && freeFormStack.getCurrentPkgUnderFreeForm() != null && !freeFormStack.getCurrentPkgUnderFreeForm().equals(startActivity.packageName) && !startActivity.inFreeformWindowingMode()) {
                freeFormStack.setFreeFormStackVisible(false);
                if (startActivity.getActivityType() != 3) {
                    freeFormStack.setCurrentPkgUnderFreeForm("");
                    freeFormStack.finishAllActivitiesLocked(true);
                }
                HwFreeFormUtils.log(TAG, "remove freeform for launch app from no-freeform stack");
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x0063 A[Catch:{ RemoteException -> 0x0074 }] */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x006d A[Catch:{ RemoteException -> 0x0074 }] */
    public void moveFreeFormToFullScreenStackIfNeed(ActivityRecord startActivity, boolean isInFreeformWindowingMode) {
        ActivityStack freeFormStack;
        boolean isNull;
        if (HwFreeFormUtils.isFreeFormEnable() && (freeFormStack = this.mService.mRootActivityContainer.getStack(5, 1)) != null && freeFormStack.getFreeFormStackVisible()) {
            String activityTitle = startActivity.toString();
            boolean isNeedExit = false;
            for (String str : HwFreeFormUtils.sExitFreeformActivity) {
                if (activityTitle.contains(str)) {
                    isNeedExit = true;
                }
            }
            ActivityRecord topActivity = freeFormStack.topRunningActivityLocked();
            if (isInFreeformWindowingMode && isNeedExit) {
                if (topActivity != null) {
                    try {
                        if (topActivity.task != null) {
                            ActivityRecord rootActivity = topActivity.task.getRootActivity();
                            if (!(rootActivity == null || rootActivity.app == null)) {
                                if (rootActivity.app.mThread != null) {
                                    isNull = false;
                                    if (isNull) {
                                        rootActivity.app.mThread.scheduleRestoreFreeFormConfig(rootActivity.appToken);
                                    } else {
                                        HwFreeFormUtils.log(TAG, "restoreFreeFormConfig failed : no rootActivity");
                                    }
                                }
                            }
                            isNull = true;
                            if (isNull) {
                            }
                        }
                    } catch (RemoteException e) {
                        HwFreeFormUtils.log(TAG, "scheduleRestoreFreeFormConfig error!");
                    }
                }
                freeFormStack.setCurrentPkgUnderFreeForm("");
                freeFormStack.setFreeFormStackVisible(false);
                HwFreeFormUtils.log(TAG, "launch some activity from freeform but move to fullscreen");
                this.mService.mHwATMSEx.toggleFreeformWindowingModeEx(topActivity);
                freeFormStack.setWindowingMode(1);
            }
        }
    }

    public void preloadApplication(ApplicationInfo appInfo, String callingPackage) {
        if (PRELOADAPP_EN && appInfo != null && callingPackage != null && !callingPackage.equals(appInfo.packageName) && callingPackage.equals("com.huawei.android.launcher") && this.mService.getProcessController(appInfo.processName, appInfo.uid) == null) {
            this.mService.mH.sendMessage(PooledLambda.obtainMessage($$Lambda$rk4xJMbKRM1lLgjaIamkhpdvq98.INSTANCE, this.mService.mAmInternal, appInfo));
        }
    }

    public boolean isAppDisabledByMdmNoComponent(ActivityInfo activityInfo, Intent intent, String resolvedType, ActivityStackSupervisor supervisor) {
        boolean isComponentExist = false;
        if (intent == null || supervisor == null || intent.getComponent() != null) {
            return false;
        }
        boolean isMdmDisabnled = false;
        ResolveInfo infoTemp = supervisor.resolveIntent(intent, resolvedType, activityInfo != null && activityInfo.applicationInfo != null ? UserHandle.getUserId(activityInfo.applicationInfo.uid) : 0, 131584, Binder.getCallingUid());
        if (!(!((infoTemp == null || infoTemp.activityInfo == null) ? false : true) || infoTemp.activityInfo.packageName == null || infoTemp.activityInfo.name == null)) {
            isComponentExist = true;
        }
        if (isComponentExist) {
            isMdmDisabnled = HwDeviceManager.mdmDisallowOp(21, new Intent().setComponent(new ComponentName(infoTemp.activityInfo.packageName, infoTemp.activityInfo.name)));
        }
        if (isMdmDisabnled) {
            UiThread.getHandler().post(new Runnable() {
                /* class com.android.server.wm.$$Lambda$HwActivityStarterEx$1jam1QK55C3XWv6UHzke5WpxMGo */

                public final void run() {
                    HwActivityStarterEx.this.lambda$isAppDisabledByMdmNoComponent$3$HwActivityStarterEx();
                }
            });
            Log.i(TAG, "Application is disabled by MDM, intent component is null.");
        }
        return isMdmDisabnled;
    }

    public /* synthetic */ void lambda$isAppDisabledByMdmNoComponent$3$HwActivityStarterEx() {
        Toast.makeText(this.mService.mContext, this.mService.mContext.getResources().getString(33685904), 0).show();
    }
}
