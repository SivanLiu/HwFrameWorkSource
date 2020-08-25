package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.ProfilerInfo;
import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.pm.UserManagerService;
import java.util.Set;

public final class HwActivityStartInterceptor extends ActivityStartInterceptor {
    public static final String ACTION_CONFIRM_APPLOCK_CREDENTIAL = "huawei.intent.action.APPLOCK_FRAMEWORK_MANAGER";
    public static final String ACTION_CONFIRM_APPLOCK_CREDENTIAL_OPAQUE = "huawei.intent.action.APPLOCK_FRAMEWORK_MANAGER_LOCKSCREEN";
    public static final String ACTION_CONFIRM_APPLOCK_PACKAGENAME = "com.huawei.systemmanager";
    public static final String APP_LOCK = "com.huawei.systemmanager/com.huawei.securitycenter.applock.password.AuthLaunchLockedAppActivity";
    public static final String APP_OPAQUE_LOCK = "com.huawei.systemmanager/com.huawei.securitycenter.applock.password.LockScreenLaunchLockedAppActivity";
    private static final String EMERGENCY_BACKUP = "com.huawei.KoBackup.EmergencyBackupActivity";
    private static final String EXTRA_USER_HANDLE_HWEX = "android.intent.extra.user_handle_hwex";
    private static final boolean IS_BOPD = SystemProperties.getBoolean("sys.bopd", false);
    private static final boolean IS_HW_MULTIWINDOW_SUPPORTED = SystemProperties.getBoolean("ro.config.hw_multiwindow_optimization", false);
    private static final boolean IS_SUPPORT_GAME_ASSIST;
    private static final String KEY_BLUR_BACKGROUND = "blurBackground";
    private static final String KOBACK_PKG_NAME = "com.huawei.KoBackup";
    private static final String LAUNCHER_PKGNAME = "com.huawei.android.launcher";
    private static final String LAUNCHER_PKG_NAME = "com.huawei.android.launcher";
    private static final String LAUNCHER_POWER_SAVE = "com.huawei.android.launcher.powersavemode.PowerSaveModeLauncher";
    private static final String LAUNCHER_STREET_MODE = "com.huawei.android.launcher.streetmode.StreetModeLauncher";
    private static final String PROP_POWER_SAVE = "sys.super_power_save";
    private static final String PROP_RIDE_MODE = "sys.ride_mode";
    private static final int PROVISIONED_OFF = 0;
    private static final String TAG = "HwActivityStartInterceptor";
    private static final String WE_CHAT = "com.tencent.mm";
    private ActivityRecord mSourceRecord;

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.config.gameassist", 0) == 1) {
            z = true;
        }
        IS_SUPPORT_GAME_ASSIST = z;
    }

    public HwActivityStartInterceptor(ActivityTaskManagerService service, ActivityStackSupervisor supervisor) {
        super(service, supervisor);
    }

    public static boolean isAppLockAction(String action) {
        return ACTION_CONFIRM_APPLOCK_CREDENTIAL.equals(action) || ACTION_CONFIRM_APPLOCK_CREDENTIAL_OPAQUE.equals(action);
    }

    public static boolean isAppLockActivity(String activity) {
        return APP_LOCK.equals(activity) || APP_OPAQUE_LOCK.equals(activity);
    }

    public static boolean isAppLockPackageName(String packageName) {
        return ACTION_CONFIRM_APPLOCK_PACKAGENAME.equals(packageName);
    }

    public boolean interceptStartActivityIfNeed(Intent intent, ActivityOptions activityOptions) {
        if (intent == null) {
            return false;
        }
        if (!interceptStartVideoActivityIfNeed(intent) && !interceptStartActivityForAppLock(intent, activityOptions) && !changeIntentForDifferentModeIfNeed(intent)) {
            return false;
        }
        return true;
    }

    private boolean changeIntentForDifferentModeIfNeed(Intent intent) {
        boolean isChange;
        Set<String> categories = intent.getCategories();
        if (categories != null && categories.contains("android.intent.category.HOME")) {
            Intent targetIntent = new Intent(intent);
            if (SystemProperties.getBoolean("sys.super_power_save", false)) {
                targetIntent.removeCategory("android.intent.category.HOME");
                targetIntent.addFlags(4194304);
                targetIntent.setClassName("com.huawei.android.launcher", LAUNCHER_POWER_SAVE);
                isChange = true;
            } else if (SystemProperties.getBoolean(PROP_RIDE_MODE, false)) {
                targetIntent.removeCategory("android.intent.category.HOME");
                targetIntent.addFlags(4194304);
                targetIntent.setClassName("com.huawei.android.launcher", LAUNCHER_STREET_MODE);
                isChange = true;
            } else if (!IS_BOPD) {
                return false;
            } else {
                if (Settings.Global.getInt(this.mService.mContext.getContentResolver(), "device_provisioned", 0) == 0) {
                    Slog.i(TAG, "failed to set activity as EmergencyBackupActivity for bopd due to oobe not finished");
                    return false;
                }
                targetIntent.removeCategory("android.intent.category.HOME");
                targetIntent.addFlags(4194304);
                targetIntent.setComponent(new ComponentName(KOBACK_PKG_NAME, EMERGENCY_BACKUP));
                Slog.i(TAG, "set activity as EmergencyBackupActivity in the mode of bopd successfully.");
                isChange = true;
            }
            if (isChange) {
                ResolveInfo tempResolveInfo = this.mSupervisor.resolveIntent(targetIntent, (String) null, this.mUserId, 0, this.mRealCallingUid);
                ActivityInfo tempActivityInfo = this.mSupervisor.resolveActivity(targetIntent, tempResolveInfo, this.mStartFlags, (ProfilerInfo) null);
                if (tempResolveInfo == null || tempActivityInfo == null) {
                    Slog.e(TAG, "Change intent for different mode null. mRInfo:" + tempResolveInfo + ", mAInfo:" + tempActivityInfo);
                    return false;
                }
                this.mIntent = targetIntent;
                this.mResolvedType = null;
                this.mRInfo = tempResolveInfo;
                this.mAInfo = tempActivityInfo;
                Slog.i(TAG, "Change intent for different mode not null.");
                return true;
            }
        }
        return false;
    }

    private boolean interceptStartActivityForAppLock(Intent intent, ActivityOptions activityOptions) {
        if ((this.mUserId != 0 && !UserManagerService.getInstance().getUserInfo(this.mUserId).isClonedProfile() && !UserManagerService.getInstance().getUserInfo(this.mUserId).isManagedProfile()) || this.mSupervisor.getKeyguardController().isKeyguardLocked() || !this.mSupervisor.mRootActivityContainer.getHwRootActivityContainerEx().isAppInLockList(intent.getComponent().getPackageName(), this.mUserId)) {
            return false;
        }
        this.mIntent = getNewIntent(intent, activityOptions);
        this.mCallingPid = this.mRealCallingPid;
        this.mCallingUid = this.mRealCallingUid;
        this.mResolvedType = null;
        if (this.mInTask != null) {
            this.mIntent.putExtra("android.intent.extra.TASK_ID", this.mInTask.taskId);
            this.mInTask = null;
        }
        this.mRInfo = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, this.mUserId, 0, this.mRealCallingUid);
        this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, this.mRInfo, this.mStartFlags, (ProfilerInfo) null);
        return true;
    }

    public void setSourceRecord(ActivityRecord sourceRecord) {
        this.mSourceRecord = sourceRecord;
    }

    private boolean interceptStartVideoActivityIfNeed(Intent intent) {
        if (!IS_SUPPORT_GAME_ASSIST) {
            return false;
        }
        HwSnsVideoManager manager = HwSnsVideoManager.getInstance(this.mService.mContext);
        if (intent.getComponent() == null || !manager.getDeferLaunchingActivitys().contains(intent.getComponent().flattenToShortString()) || !this.mService.mHwATMSEx.isGameDndOn()) {
            manager.setReadyToShowActivity(true);
            return false;
        }
        String pkgName = intent.getComponent().getPackageName();
        if (!WE_CHAT.equals(pkgName) || this.mService.mRootActivityContainer.getTopDisplayFocusedStack() == null || this.mService.mRootActivityContainer.getTopDisplayFocusedStack().mResumedActivity == null || !pkgName.equals(this.mService.mRootActivityContainer.getTopDisplayFocusedStack().mResumedActivity.packageName)) {
            manager.setActivityManager(this.mService);
            if (manager.isAttached()) {
                manager.updateFloatView(pkgName, makeIntentSender(intent));
                manager.setReadyToShowActivity(false);
            } else if (manager.getReadyToShowActivity(intent)) {
                return false;
            } else {
                if (!manager.isTransferActivity(intent)) {
                    manager.addFloatView(pkgName, makeIntentSender(intent));
                }
                manager.setReadyToShowActivity(false);
            }
            return true;
        }
        manager.setReadyToShowActivity(true);
        return false;
    }

    private Intent getNewIntent(Intent intent, ActivityOptions activityOptions) {
        ActivityRecord activityRecord;
        Intent newIntent = new Intent(ACTION_CONFIRM_APPLOCK_CREDENTIAL);
        newIntent.putExtra(EXTRA_USER_HANDLE_HWEX, this.mUserId);
        newIntent.setPackage(ACTION_CONFIRM_APPLOCK_PACKAGENAME);
        int flags = 41943040;
        if (IS_HW_MULTIWINDOW_SUPPORTED) {
            flags = 41943040 | 1073741824;
        }
        newIntent.setFlags(flags);
        newIntent.putExtra("android.intent.extra.PACKAGE_NAME", this.mAInfo.packageName);
        if (this.mSourceRecord == null && this.mInTask == null) {
            this.mIntent.addFlags(268435456);
        }
        boolean isLaunchFlag = false;
        IIntentSender target = this.mService.getIntentSenderLocked(2, this.mCallingPackage, Binder.getCallingUid(), this.mUserId, (IBinder) null, (String) null, 0, new Intent[]{this.mIntent}, new String[]{this.mResolvedType}, 1207959552, (Bundle) null);
        this.mUserId = 0;
        newIntent.putExtra("android.intent.extra.INTENT", new IntentSender(target));
        if ((this.mSourceRecord == null && this.mInTask == null) || ((activityRecord = this.mSourceRecord) != null && activityRecord.getActivityType() == 2)) {
            newIntent.addFlags(268435456);
        }
        if (activityOptions != null && WindowConfiguration.isHwMultiStackWindowingMode(activityOptions.getLaunchWindowingMode())) {
            newIntent.putExtra("options", activityOptions.toBundle());
        }
        newIntent.putExtra("android.intent.extra.COMPONENT_NAME", intent.getComponent());
        ActivityStack topStack = this.mService.mRootActivityContainer.getTopDisplayFocusedStack();
        if (!IS_HW_MULTIWINDOW_SUPPORTED && topStack != null) {
            newIntent.putExtra("windowMode", topStack.getWindowingMode());
        }
        if ("com.huawei.android.launcher".equals(this.mCallingPackage) || (this.mIntent.getHwFlags() & 4096) != 0) {
            isLaunchFlag = true;
        }
        if (isLaunchFlag || !(topStack == null || topStack.mResumedActivity == null || !"com.huawei.android.launcher".equals(topStack.mResumedActivity.packageName))) {
            newIntent.putExtra(KEY_BLUR_BACKGROUND, true);
        }
        return newIntent;
    }

    /* access modifiers changed from: package-private */
    public IntentSender makeIntentSender(Intent intent) {
        return new IntentSender(this.mService.getIntentSenderLocked(2, this.mCallingPackage, this.mCallingUid, this.mUserId, (IBinder) null, (String) null, 0, new Intent[]{intent}, new String[]{this.mResolvedType}, 1409286144, (Bundle) null));
    }
}
