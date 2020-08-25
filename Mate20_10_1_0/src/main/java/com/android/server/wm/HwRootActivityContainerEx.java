package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.WindowConfiguration;
import android.content.Intent;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.wm.ActivityStack;
import com.huawei.server.wm.IHwRootActivityContainerEx;

public class HwRootActivityContainerEx implements IHwRootActivityContainerEx {
    private static final int APP_LOCK_START_DELAY_MAXTIMES = 5;
    private static final int APP_LOCK_START_DELAY_TIME = 500;
    private static final String EXTRA_USER_HANDLE_HWEX = "android.intent.extra.user_handle_hwex";
    private static final boolean IS_HW_MULTIWINDOW_SUPPORTED = SystemProperties.getBoolean("ro.config.hw_multiwindow_optimization", false);
    private static final String KEY_LAUNCH_WINDOWING_MODE = "android.activity.windowingMode";
    private static final String OPTIONS_KEY = "options";
    private static final String SEMICOLON_STR = ";";
    private static final String TAG = "HwRootActivityContainerEx";
    private static long sAppLockStartTimes = 0;
    private static long sApplockStartTimeBegin = 0;
    IHwRootActivityContainerInner mIRacInner = null;
    private final ActivityTaskManagerService mService;

    public HwRootActivityContainerEx(IHwRootActivityContainerInner rac, ActivityTaskManagerService service) {
        this.mService = service;
        this.mIRacInner = rac;
    }

    public boolean resumeAppLockActivityIfNeeded(ActivityStack stack, ActivityOptions targetOptions) {
        ActivityTaskManagerService activityTaskManagerService;
        ActivityRecord activityRecord;
        int windowMode;
        if (stack == null || (activityTaskManagerService = this.mService) == null || activityTaskManagerService.getCurrentUserId() != 0 || (activityRecord = stack.topRunningActivityLocked()) == null || isAppLockApplication(activityRecord)) {
            return false;
        }
        if (!IS_HW_MULTIWINDOW_SUPPORTED && stack.inMultiWindowMode() && "outofsleep".equals(this.mService.mStackSupervisor.mActivityLaunchTrack) && !isAppInLockList(activityRecord.packageName, activityRecord.mUserId)) {
            if (stack.inSplitScreenPrimaryWindowingMode()) {
                windowMode = 4;
            } else {
                windowMode = 3;
            }
            ActivityStack topOtherSplitStack = stack.getDisplay().getTopStackInWindowingMode(windowMode);
            if (topOtherSplitStack != null) {
                activityRecord = topOtherSplitStack.topRunningActivityLocked();
            }
        }
        if (activityRecord == null || activityRecord.isState(ActivityStack.ActivityState.RESUMED) || !isKeyguardDismiss() || !isAppInLockList(activityRecord.packageName, activityRecord.mUserId)) {
            return false;
        }
        long realTime = SystemClock.elapsedRealtime();
        if (realTime - sApplockStartTimeBegin < 500) {
            long j = sAppLockStartTimes;
            if (j >= 5) {
                Slog.e(TAG, "start applock too often, ignored in 500ms");
                return true;
            }
            sAppLockStartTimes = j + 1;
        } else {
            sApplockStartTimeBegin = realTime;
            sAppLockStartTimes = 0;
        }
        startAppLock(stack, activityRecord);
        return true;
    }

    private void startAppLock(ActivityStack stack, ActivityRecord activityRecord) {
        Intent newIntent = new Intent(HwActivityStartInterceptor.ACTION_CONFIRM_APPLOCK_CREDENTIAL_OPAQUE);
        newIntent.putExtra(EXTRA_USER_HANDLE_HWEX, activityRecord.mUserId);
        int flags = 109051904;
        if (IS_HW_MULTIWINDOW_SUPPORTED && !stack.getDisplay().isSleeping()) {
            flags = 109051904 | 1073741824;
        }
        newIntent.setFlags(flags);
        newIntent.setPackage(HwActivityStartInterceptor.ACTION_CONFIRM_APPLOCK_PACKAGENAME);
        newIntent.putExtra("android.intent.extra.TASK_ID", activityRecord.task.taskId);
        newIntent.putExtra("android.intent.extra.PACKAGE_NAME", activityRecord.packageName);
        newIntent.putExtra("android.intent.extra.COMPONENT_NAME", activityRecord.mActivityComponent);
        ActivityOptions options = ActivityOptions.makeBasic();
        if (!IS_HW_MULTIWINDOW_SUPPORTED) {
            newIntent.putExtra("windowMode", stack.getWindowingMode());
        } else {
            options.setLaunchWindowingMode(activityRecord.getWindowingMode());
            newIntent.putExtra(OPTIONS_KEY, options.toBundle());
        }
        options.setLaunchTaskId(activityRecord.task.taskId);
        this.mService.mContext.startActivity(newIntent, options.toBundle());
    }

    public boolean isAppInLockList(String pgkName, int userId) {
        ActivityTaskManagerService activityTaskManagerService;
        if (!(pgkName == null || (activityTaskManagerService = this.mService) == null || Settings.Secure.getInt(activityTaskManagerService.mContext.getContentResolver(), "app_lock_func_status", 0) != 1)) {
            if ((";" + Settings.Secure.getStringForUser(this.mService.mContext.getContentResolver(), "app_lock_list", userId) + ";").contains(";" + pgkName + ";")) {
                return true;
            }
        }
        return false;
    }

    private boolean isAppLockApplication(ActivityRecord activityRecord) {
        if (activityRecord.intent == null || !HwActivityStartInterceptor.isAppLockPackageName(activityRecord.packageName) || !HwActivityStartInterceptor.isAppLockAction(activityRecord.intent.getAction())) {
            return false;
        }
        return true;
    }

    private boolean isKeyguardDismiss() {
        return !this.mService.mStackSupervisor.getKeyguardController().isKeyguardLocked() && !this.mService.mWindowManager.isPendingLock();
    }

    public void checkStartAppLockActivity() {
        if (IS_HW_MULTIWINDOW_SUPPORTED) {
            for (int displayNdx = this.mService.mRootActivityContainer.getChildCount() - 1; displayNdx >= 0; displayNdx--) {
                ActivityDisplay display = this.mService.mRootActivityContainer.getChildAt(displayNdx);
                if (display.mDisplayId == 0 && display.isSleeping()) {
                    for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                        ActivityStack stack = display.getChildAt(stackNdx);
                        if (stack != null && stack.shouldBeVisible((ActivityRecord) null)) {
                            ActivityRecord topActivity = stack.topRunningActivityLocked();
                            if (topActivity != null && !isAppLockApplication(topActivity)) {
                                if (!topActivity.isState(ActivityStack.ActivityState.RESUMED) && isAppHasLock(topActivity.packageName, topActivity.mUserId)) {
                                    Intent newIntent = new Intent(HwActivityStartInterceptor.ACTION_CONFIRM_APPLOCK_CREDENTIAL_OPAQUE);
                                    newIntent.putExtra(EXTRA_USER_HANDLE_HWEX, topActivity.mUserId);
                                    newIntent.setPackage(HwActivityStartInterceptor.ACTION_CONFIRM_APPLOCK_PACKAGENAME);
                                    newIntent.putExtra("android.intent.extra.PACKAGE_NAME", topActivity.packageName);
                                    newIntent.putExtra("android.intent.extra.COMPONENT_NAME", topActivity.mActivityComponent);
                                    ActivityOptions options = ActivityOptions.makeBasic();
                                    options.setLaunchWindowingMode(topActivity.getWindowingMode());
                                    options.setLaunchTaskId(topActivity.task.taskId);
                                    Bundle bundle = options.toBundle();
                                    newIntent.putExtra(OPTIONS_KEY, bundle);
                                    newIntent.addFlags(268435456);
                                    this.mService.mH.post(new Runnable(newIntent, bundle) {
                                        /* class com.android.server.wm.$$Lambda$HwRootActivityContainerEx$OTUFHSluHUFSag2ryPfnZa1fZ8M */
                                        private final /* synthetic */ Intent f$1;
                                        private final /* synthetic */ Bundle f$2;

                                        {
                                            this.f$1 = r2;
                                            this.f$2 = r3;
                                        }

                                        public final void run() {
                                            HwRootActivityContainerEx.this.lambda$checkStartAppLockActivity$0$HwRootActivityContainerEx(this.f$1, this.f$2);
                                        }
                                    });
                                }
                                if (!stack.inMultiWindowMode()) {
                                    break;
                                }
                            } else if (!(topActivity == null || topActivity.intent == null)) {
                                topActivity.intent.removeFlags(1073741824);
                            }
                        }
                    }
                }
            }
        }
    }

    public /* synthetic */ void lambda$checkStartAppLockActivity$0$HwRootActivityContainerEx(Intent newIntent, Bundle bundle) {
        this.mService.mContext.startActivity(newIntent, bundle);
    }

    public boolean checkWindowModeForAppLock(ActivityRecord target, ActivityRecord activityRecord) {
        try {
            if (!HwActivityStartInterceptor.isAppLockPackageName(target.info.packageName) || !HwActivityStartInterceptor.isAppLockAction(target.intent.getAction())) {
                return false;
            }
            return isSkipReusableScenario(target, activityRecord);
        } catch (BadParcelableException e) {
            Slog.e(TAG, "Check window mode for applock fail.");
            return false;
        } catch (NullPointerException e2) {
            Slog.e(TAG, "Check window mode for applock fail: null");
            return false;
        }
    }

    private boolean isSkipReusableScenario(ActivityRecord target, ActivityRecord activityRecord) {
        if (!IS_HW_MULTIWINDOW_SUPPORTED) {
            int windowingMode = target.intent.getIntExtra("windowMode", 0);
            if (windowingMode != activityRecord.getWindowingMode() || WindowConfiguration.isSplitScreenWindowingMode(windowingMode)) {
                Slog.i(TAG, "Skipping " + activityRecord + ": mismatch windowMode");
                return true;
            }
        } else if (HwActivityStartInterceptor.isAppLockPackageName(activityRecord.info.packageName) && HwActivityStartInterceptor.isAppLockAction(activityRecord.intent.getAction())) {
            Bundle bundle = target.intent.getBundleExtra(OPTIONS_KEY);
            int windowingMode2 = bundle != null ? bundle.getInt(KEY_LAUNCH_WINDOWING_MODE) : 1;
            if (windowingMode2 != activityRecord.getWindowingMode() && (windowingMode2 != 1 || activityRecord.inMultiWindowMode())) {
                Slog.i(TAG, "Skipping " + activityRecord + ": mismatch window mode");
                return true;
            }
        }
        if (!HwActivityStartInterceptor.isAppLockPackageName(activityRecord.info.packageName)) {
            if (!(activityRecord.task == null || activityRecord.task.intent == null || !HwActivityStartInterceptor.isAppLockAction(activityRecord.task.intent.getAction()))) {
                Slog.i(TAG, "Skipping " + activityRecord + ": mismatch task");
                return true;
            }
        } else if (target.intent.getIntExtra(EXTRA_USER_HANDLE_HWEX, 0) != activityRecord.intent.getIntExtra(EXTRA_USER_HANDLE_HWEX, 0)) {
            return true;
        } else {
            String targetPackageName = target.intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
            String recordPackageName = activityRecord.intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
            if (targetPackageName != null) {
                return !targetPackageName.equals(recordPackageName);
            }
        }
        return false;
    }

    private boolean isAppHasLock(String pgkName, int userId) {
        if (pgkName != null && Settings.Secure.getInt(this.mService.mContext.getContentResolver(), "app_lock_func_status", 0) == 1) {
            if (!(";" + Settings.Secure.getStringForUser(this.mService.mContext.getContentResolver(), "applock_unlocked_list", userId) + ";").contains(";" + pgkName + ";")) {
                if ((";" + Settings.Secure.getStringForUser(this.mService.mContext.getContentResolver(), "app_lock_list", userId) + ";").contains(";" + pgkName + ";")) {
                    return true;
                }
            }
            return true;
        }
        return false;
    }
}
