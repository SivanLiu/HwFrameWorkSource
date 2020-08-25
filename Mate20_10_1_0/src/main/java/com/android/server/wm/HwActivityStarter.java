package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.app.WaitResult;
import android.common.HwFrameworkFactory;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.hdm.HwDeviceManager;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionSession;
import android.text.TextUtils;
import android.util.Flog;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.widget.Toast;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.HwBluetoothBigDataService;
import com.android.server.UiThread;
import com.android.server.am.PendingIntentRecord;
import com.android.server.gesture.GestureNavConst;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.pm.HwPackageManagerService;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HwActivityStarter extends ActivityStarter {
    private static final int CUST_HOME_SCREEN_OFF = 0;
    private static final int CUST_HOME_SCREEN_ON = 1;
    /* access modifiers changed from: private */
    public static final ComponentName DOCOMOHOME_COMPONENT = new ComponentName("com.nttdocomo.android.dhome", "com.nttdocomo.android.dhome.HomeActivity");
    /* access modifiers changed from: private */
    public static final ComponentName DRAWERHOME_COMPONENT = new ComponentName("com.huawei.android.launcher", "com.huawei.android.launcher.drawer.DrawerLauncher");
    private static final String FILE_STR = "file";
    private static final Set<ComponentName> HOME_COMPONENTS = new HashSet<ComponentName>() {
        /* class com.android.server.wm.HwActivityStarter.AnonymousClass1 */

        {
            if (HwActivityStarter.IS_SHOW_CUST_HOME_SCREEN) {
                add(HwActivityStarter.DOCOMOHOME_COMPONENT);
                return;
            }
            add(HwActivityStarter.UNIHOME_COMPONENT);
            add(HwActivityStarter.DRAWERHOME_COMPONENT);
            add(HwActivityStarter.SIMPLEHOME_COMPONENT);
            add(HwActivityStarter.NEWSIMPLEHOME_COMPONENT);
        }
    };
    private static final String HW_LAUNCHER_PKG_NAME = "com.huawei.android.launcher";
    private static final String INTENT_FORWARD_USER_ID = "intent_forward_user_id";
    private static final boolean IS_BOPD = SystemProperties.getBoolean("sys.bopd", false);
    /* access modifiers changed from: private */
    public static final boolean IS_SHOW_CUST_HOME_SCREEN;
    /* access modifiers changed from: private */
    public static final ComponentName NEWSIMPLEHOME_COMPONENT = new ComponentName("com.huawei.android.launcher", "com.huawei.android.launcher.newsimpleui.NewSimpleLauncher");
    private static final int PROVISIONED_OFF = 0;
    /* access modifiers changed from: private */
    public static final ComponentName SIMPLEHOME_COMPONENT = new ComponentName("com.huawei.android.launcher", "com.huawei.android.launcher.simpleui.SimpleUILauncher");
    private static final String TAG = "ActivityStarter";
    /* access modifiers changed from: private */
    public static final ComponentName UNIHOME_COMPONENT = new ComponentName("com.huawei.android.launcher", "com.huawei.android.launcher.unihome.UniHomeLauncher");
    private static Map<Integer, Boolean> sLauncherStartStates = new HashMap();
    private static Set<String> sPcPkgNames = new HashSet();
    private static Set<String> sSkipCancelResults = new HashSet();

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.config.show_cust_homescreen", 0) != 1) {
            z = false;
        }
        IS_SHOW_CUST_HOME_SCREEN = z;
        sSkipCancelResults.add(HwActivityStartInterceptor.APP_LOCK);
        sSkipCancelResults.add(HwActivityStartInterceptor.APP_OPAQUE_LOCK);
        sPcPkgNames.add("com.huawei.android.hwpay");
        sPcPkgNames.add(HwBluetoothBigDataService.BIGDATA_RECEIVER_PACKAGENAME);
        sPcPkgNames.add("com.huawei.screenrecorder");
    }

    public HwActivityStarter(ActivityStartController controller, ActivityTaskManagerService service, ActivityStackSupervisor supervisor, ActivityStartInterceptor interceptor) {
        super(controller, service, supervisor, interceptor);
    }

    private void forceValidateHomeButton(int userId) {
        if (Settings.Secure.getIntForUser(this.mService.mContext.getContentResolver(), "user_setup_complete", 0, userId) == 0 || Settings.Global.getInt(this.mService.mContext.getContentResolver(), "device_provisioned", 0) == 0) {
            Settings.Global.putInt(this.mService.mContext.getContentResolver(), "device_provisioned", 1);
            Settings.Secure.putIntForUser(this.mService.mContext.getContentResolver(), "user_setup_complete", 1, userId);
            Log.w(TAG, "DEVICE_PROVISIONED or USER_SETUP_COMPLETE set 0 to 1!");
        }
    }

    private boolean isStartLauncherActivity(Intent intent, int userId) {
        if (intent == null) {
            Log.w(TAG, "intent is null, not start launcher!");
            return false;
        }
        PackageManager pm = this.mService.mContext.getPackageManager();
        Intent mainIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").addCategory("android.intent.category.DEFAULT");
        ComponentName cmp = intent.getComponent();
        if (pm != null && intent.hasCategory("android.intent.category.HOME")) {
            long origId = Binder.clearCallingIdentity();
            try {
                ResolveInfo info = pm.resolveActivityAsUser(mainIntent, 0, userId);
                if (info == null || info.priority != 0 || cmp == null || info.activityInfo == null || !cmp.getPackageName().equals(info.activityInfo.packageName)) {
                    Binder.restoreCallingIdentity(origId);
                } else {
                    Log.d(TAG, "info priority is 0, cmp: " + cmp + ", userId: " + userId);
                    return true;
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public int startActivity(IApplicationThread caller, Intent intent, Intent ephemeralIntent, String resolvedType, ActivityInfo activityInfo, ResolveInfo resolveInfo, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, int realCallingPid, int realCallingUid, int startFlags, SafeActivityOptions options, boolean isIgnoreTargetSecurity, boolean isComponentSpecified, ActivityRecord[] outActivity, TaskRecord inTask, String reason, boolean isAllowPendingRemoteAnimationRegistryLookup, PendingIntentRecord originatingPendingIntent, boolean isAllowBackgroundActivityStart) {
        if (intent.getComponent() != null && HwDeviceManager.disallowOp(4, intent.getComponent().getPackageName())) {
            return showToastDisallowedByMdmApk(intent);
        }
        if (isMultiWindowDisabled()) {
            if (isHwMultiWindowMode(options, intent, activityInfo, caller)) {
                return showToastDisallowedByMdmApk(intent);
            }
        }
        if (isApplicationDisabledByMdm(activityInfo, intent, resolvedType)) {
            Flog.i(101, "Application is disabled by MDM, intent = " + intent);
            return -92;
        }
        int userId = activityInfo != null ? UserHandle.getUserId(activityInfo.applicationInfo.uid) : 0;
        if ((!sLauncherStartStates.containsKey(Integer.valueOf(userId)) || !sLauncherStartStates.get(Integer.valueOf(userId)).booleanValue()) && isFrpRestricted(this.mService.mContext, userId) && userId == 0 && activityInfo != null && isFrpRestrictedApp(this.mService.mContext, intent, activityInfo, userId)) {
            return sendFrpRestrictedBroadcast(activityInfo);
        }
        if (activityInfo != null) {
            HwStartWindowRecord.getInstance().setStartFromMainAction(Integer.valueOf(activityInfo.applicationInfo.uid), "android.intent.action.MAIN".equals(intent.getAction()));
        }
        if ((!sLauncherStartStates.containsKey(Integer.valueOf(userId)) || !sLauncherStartStates.get(Integer.valueOf(userId)).booleanValue()) && isStartLauncherActivity(intent, userId)) {
            Slog.w(TAG, "check the USER_SETUP_COMPLETE is set 1 in first start launcher!");
            forceValidateHomeButton(userId);
            sLauncherStartStates.put(Integer.valueOf(userId), true);
        }
        int startResult = HwActivityStarter.super.startActivity(caller, intent, ephemeralIntent, resolvedType, activityInfo, resolveInfo, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid, callingUid, callingPackage, realCallingPid, realCallingUid, startFlags, options, isIgnoreTargetSecurity, isComponentSpecified, outActivity, inTask, reason, isAllowPendingRemoteAnimationRegistryLookup, originatingPendingIntent, isAllowBackgroundActivityStart);
        pcSetScreenPower(startResult, outActivity);
        return startResult;
    }

    private int sendFrpRestrictedBroadcast(ActivityInfo activityInfo) {
        Intent intentBroadcast = new Intent();
        intentBroadcast.setAction("com.huawei.action.frp_activity_restricted");
        intentBroadcast.putExtra(SceneRecogFeature.DATA_COMP, activityInfo.getComponentName());
        this.mService.mH.post(new Runnable(intentBroadcast) {
            /* class com.android.server.wm.$$Lambda$HwActivityStarter$e6aEtGx3YirQhqD047ZnS3yuEFE */
            private final /* synthetic */ Intent f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwActivityStarter.this.lambda$sendFrpRestrictedBroadcast$0$HwActivityStarter(this.f$1);
            }
        });
        Log.w(TAG, "forbid launching Apps becasue of FRP, this App:" + activityInfo);
        return 0;
    }

    public /* synthetic */ void lambda$sendFrpRestrictedBroadcast$0$HwActivityStarter(Intent intentBroadcast) {
        this.mService.mContext.sendBroadcast(intentBroadcast, "com.huawei.android.permission.ANTITHEFT");
    }

    private void pcSetScreenPower(int startResult, ActivityRecord[] outActivity) {
        try {
            if (!ActivityManager.isStartResultSuccessful(startResult)) {
                return;
            }
            if ((HwPCUtils.isPcCastModeInServer() || HwPCUtils.isInWindowsCastMode() || HwPCUtils.isDisallowLockScreenForHwMultiDisplay()) && !HwPCUtils.enabledInPad()) {
                for (ActivityRecord r : outActivity) {
                    if (r != null) {
                        if (!HwPCUtils.isInWindowsCastMode() || "com.android.incallui".equals(r.packageName) || HwActivityStartInterceptor.isAppLockActivity(r.shortComponentName)) {
                            if (r.getWindowingMode() != 1) {
                                if (r.getActivityType() == 2 && r.fullscreen) {
                                }
                            }
                            HwPCUtils.getHwPCManager().setScreenPower(true);
                            return;
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            HwPCUtils.log(TAG, "startActivityLocked error");
        }
    }

    private int showToastDisallowedByMdmApk(Intent intent) {
        Flog.i(101, "[" + intent.getComponent().getPackageName() + "] is disallowed running by MDM apk");
        UiThread.getHandler().post(new Runnable() {
            /* class com.android.server.wm.HwActivityStarter.AnonymousClass2 */

            public void run() {
                Context context = HwActivityStarter.this.mService.mUiContext;
                if (context != null) {
                    Toast toast = Toast.makeText(context, context.getString(33686009), 0);
                    toast.getWindowParams().privateFlags |= 16;
                    toast.show();
                }
            }
        });
        return -96;
    }

    private boolean isApplicationDisabledByMdm(ActivityInfo activityInfo, Intent intent, String resolvedType) {
        boolean isMdmDisabnled = false;
        if (intent.getComponent() == null) {
            Log.i(TAG, "isApplicationDisabledByMdm intent component is null");
        } else {
            isMdmDisabnled = HwDeviceManager.mdmDisallowOp(21, intent);
        }
        if (isMdmDisabnled) {
            UiThread.getHandler().post(new Runnable() {
                /* class com.android.server.wm.HwActivityStarter.AnonymousClass3 */

                public void run() {
                    Toast.makeText(HwActivityStarter.this.mService.mContext, HwActivityStarter.this.mService.mContext.getResources().getString(33685904), 0).show();
                }
            });
        }
        return isMdmDisabnled;
    }

    private void changeIntentForDifferentModeIfNeed(Intent intent) {
        Set<String> categories = intent.getCategories();
        if (categories != null && categories.contains("android.intent.category.HOME")) {
            if (SystemProperties.getBoolean(GestureNavConst.KEY_SUPER_SAVE_MODE, false)) {
                intent.removeCategory("android.intent.category.HOME");
                intent.addFlags(4194304);
                intent.setClassName("com.huawei.android.launcher", "com.huawei.android.launcher.powersavemode.PowerSaveModeLauncher");
            } else if (SystemProperties.getBoolean("sys.ride_mode", false)) {
                intent.removeCategory("android.intent.category.HOME");
                intent.addFlags(4194304);
                intent.setClassName("com.huawei.android.launcher", "com.huawei.android.launcher.streetmode.StreetModeLauncher");
            } else if (!IS_BOPD) {
            } else {
                if (Settings.Global.getInt(this.mService.mContext.getContentResolver(), "device_provisioned", 0) == 0) {
                    Log.i(TAG, "failed to set activity as EmergencyBackupActivity for bopd due to oobe not finished.");
                    return;
                }
                intent.removeCategory("android.intent.category.HOME");
                intent.addFlags(4194304);
                intent.setComponent(new ComponentName("com.huawei.KoBackup", "com.huawei.KoBackup.EmergencyBackupActivity"));
                Log.i(TAG, "set activity as EmergencyBackupActivity in the mode of bopd successfully.");
            }
        }
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:100:0x025b, code lost:
        if (r0.activityInfo == null) goto L_0x0333;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:101:0x025d, code lost:
        if (r29 != null) goto L_0x0262;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:102:0x025f, code lost:
        r2 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:104:0x0266, code lost:
        r2 = r29.mInfo.packageName;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:105:0x0267, code lost:
        r18 = r31;
        r10 = r30;
        r15 = r34;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:108:0x027f, code lost:
        if (shouldDisplayClonedAppToChoose(r2, r30, r31, r0, r7, r0, r0) == false) goto L_0x02fd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:109:0x0281, code lost:
        r10.addHwFlags(2);
        r10.setComponent(new android.content.ComponentName(r0.activityInfo.packageName, r0.activityInfo.name));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:110:0x0296, code lost:
        r2 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:112:?, code lost:
        r15.standardizeIntentUriForClone(r10, r28, (android.os.storage.StorageManager) r15.mService.mContext.getSystemService("storage"));
        r2 = android.content.Intent.createChooser(r10, r15.mService.mContext.getResources().getText(17041497));
        r2.setFlags(r10.getFlags() & -536870913);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:113:0x02c6, code lost:
        r9 = 101;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:114:0x02c9, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:115:0x02ca, code lost:
        r9 = 101;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:117:?, code lost:
        android.util.Flog.e(101, "startActivityMayWait, fail to create chooser for " + r10, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:126:0x02fd, code lost:
        r9 = 101;
        r10.setHwFlags(r10.getHwFlags() & -3);
        r5 = r10;
        r4 = r18;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:135:0x032b, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:136:0x032e, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:138:0x0333, code lost:
        r18 = r31;
        r10 = r30;
        r20 = r7;
        r9 = r15;
        r15 = r34;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:139:0x033d, code lost:
        r5 = r10;
        r4 = r18;
        r6 = r20;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:149:0x0374, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:151:0x037f, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:153:0x038a, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:155:0x0396, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:159:0x03a6, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:160:0x03a8, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:161:0x03a9, code lost:
        r9 = r15;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:162:0x03ac, code lost:
        r3 = r9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:163:0x03af, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:164:0x03b0, code lost:
        r32 = 64;
        r3 = 101;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:165:0x03b5, code lost:
        r9 = 101;
        r32 = 64;
        r0 = r40;
        r29 = r41;
        r30 = r53;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:171:0x0420, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:172:0x0421, code lost:
        r3 = 101;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:181:0x0433, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:187:0x043f, code lost:
        android.os.Trace.traceEnd(r32);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0094, code lost:
        android.util.Flog.i(101, "startActivityMayWait, callerApp: " + r0 + ", intent: " + r40.toShortStringWithoutClip(true, true, true) + ", userId = " + r53 + ", callingUid = " + android.os.Binder.getCallingUid());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x00d1, code lost:
        if (com.android.server.am.HwActivityManagerService.IS_SUPPORT_CLONE_APP == false) goto L_0x03b5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x00db, code lost:
        if (r34.mService.mUserManagerInternal.hasClonedProfile() == false) goto L_0x03b5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x00dd, code lost:
        if (r53 != 0) goto L_0x0164;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x00e3, code lost:
        if (r40.getComponent() == null) goto L_0x0164;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x00f1, code lost:
        if (com.android.server.pm.HwPackageManagerService.isSupportCloneAppInCust(r40.getComponent().getPackageName()) != false) goto L_0x0158;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x00f3, code lost:
        r32 = 64;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:?, code lost:
        r0 = com.android.server.wm.HwActivityStarter.super.startActivityMayWait(r35, r36, r37, r38, r39, r40, r41, r42, r43, r44, r45, r46, r47, r48, r49, r50, r51, r52, r53, r54, r55, r56, r57, r58);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x0135, code lost:
        if (com.android.server.wm.ActivityTaskManagerDebugConfig.DEBUG_HW_ACTIVITY == false) goto L_0x013a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x0137, code lost:
        android.os.Trace.traceEnd(64);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x013a, code lost:
        android.util.Flog.i(101, "startActivityMayWait cost " + (android.os.SystemClock.uptimeMillis() - r26));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x0157, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x0158, code lost:
        r31 = r41;
        r30 = r40;
        r28 = r53;
        r29 = r0;
        r15 = 101;
        r32 = 64;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x0164, code lost:
        r31 = r41;
        r30 = r40;
        r28 = r53;
        r29 = r0;
        r15 = 101;
        r32 = 64;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:?, code lost:
        r0 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:?, code lost:
        r0 = new java.util.HashMap<>();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x0182, code lost:
        if ((r30.getHwFlags() & 1024) == 0) goto L_0x0186;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x0184, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x0186, code lost:
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x018d, code lost:
        r7 = r28;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:60:?, code lost:
        r0 = r34.mSupervisor.resolveIntent(r30, r31, r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:62:0x0198, code lost:
        if (r0 != null) goto L_0x0246;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:?, code lost:
        r0 = r34.mService.mUserManagerInternal.getUserInfo(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x01a2, code lost:
        if (r0 == null) goto L_0x0230;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x01a8, code lost:
        if (r0.isClonedProfile() == false) goto L_0x0230;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:0x01ae, code lost:
        if (android.os.storage.StorageManager.isUserKeyUnlocked(r7) != false) goto L_0x01fc;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x01b6, code lost:
        if (android.os.storage.StorageManager.isUserKeyUnlocked(r0.profileGroupId) == false) goto L_0x01fc;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x01c0, code lost:
        if (r34.mService.mAmInternal.getHaveTryCloneProUserUnlock() == false) goto L_0x01ca;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x01c2, code lost:
        showErrorDialogToRemoveUser(r34.mService.mContext, r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x01ca, code lost:
        android.util.Slog.i(com.android.server.wm.HwActivityStarter.TAG, "Wait for CloneProfile user unLock, return!");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:?, code lost:
        android.os.Binder.restoreCallingIdentity(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x01d7, code lost:
        if (com.android.server.wm.ActivityTaskManagerDebugConfig.DEBUG_HW_ACTIVITY == false) goto L_0x01dc;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x01d9, code lost:
        android.os.Trace.traceEnd(r32);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x01dc, code lost:
        android.util.Flog.i(r15, "startActivityMayWait cost " + (android.os.SystemClock.uptimeMillis() - r26));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x01f7, code lost:
        return 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x01f8, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x01f9, code lost:
        r3 = r15;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:85:0x01fc, code lost:
        r1 = r0.profileGroupId;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:?, code lost:
        android.util.Flog.i(r15, "startActivityMayWait forward intent from clone user " + r0.id + " to parent user " + r1 + " because clone user has non target apps to respond.");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:88:0x0222, code lost:
        r7 = r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:89:0x0224, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:91:0x0230, code lost:
        r4 = r31;
        r5 = r30;
        r6 = r7;
        r9 = r15;
        r15 = r34;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:92:0x023a, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:95:0x0248, code lost:
        if (r29 != null) goto L_0x0259;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:96:0x024a, code lost:
        if (r0 == false) goto L_0x024d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:97:0x024d, code lost:
        r18 = r31;
        r10 = r30;
        r20 = r7;
        r9 = r15;
        r15 = r34;
     */
    /* JADX WARNING: Removed duplicated region for block: B:187:0x043f  */
    public int startActivityMayWait(IApplicationThread caller, int callingUid, String callingPackage, int requestRealCallingPid, int requestRealCallingUid, Intent intent, String resolvedType, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, WaitResult outResult, Configuration globalConfig, SafeActivityOptions options, boolean isIgnoreTargetSecurity, int userId, TaskRecord inTask, String reason, boolean isAllowPendingRemoteAnimationRegistryLookup, PendingIntentRecord originatingPendingIntent, boolean isAllowBackgroundActivityStart) {
        long j;
        int i;
        long j2;
        int tempUserId;
        long ident;
        Map<String, Integer> mapForwardUserId;
        boolean shouldCheckDual;
        int tempUserId2;
        HwActivityStarter hwActivityStarter;
        int tempUserId3;
        String tempResolvedType;
        Intent tempIntent;
        Intent targetIntent;
        int i2 = 2;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(IHwBehaviorCollectManager.BehaviorId.ACTIVITYSTARTER_STARTACTIVITYMAYWAIT, new Object[]{intent, callingPackage});
        changeIntentForDifferentModeIfNeed(intent);
        long start = SystemClock.uptimeMillis();
        try {
            if (ActivityTaskManagerDebugConfig.DEBUG_HW_ACTIVITY) {
                Trace.traceBegin(64, "startActivityMayWait");
            }
            if (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer() || !"com.huawei.android.projectmenu".equals(intent.getPackage())) {
                synchronized (this.mService.getGlobalLock()) {
                    try {
                        WindowProcessController callerApplication = this.mService.getProcessController(caller);
                        try {
                        } catch (Throwable th) {
                            th = th;
                            i = 101;
                            j2 = 64;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        i = 101;
                        j2 = 64;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
            } else {
                if (Log.HWINFO) {
                    HwPCUtils.log("HwActivityStarter", "startActivityMayWait intent: " + intent);
                }
                if (ActivityTaskManagerDebugConfig.DEBUG_HW_ACTIVITY) {
                    Trace.traceEnd(64);
                }
                Flog.i(101, "startActivityMayWait cost " + (SystemClock.uptimeMillis() - start));
                return 0;
            }
            if (targetIntent != null) {
                try {
                    tempResolvedType = targetIntent.resolveTypeIfNeeded(hwActivityStarter.mService.mContext.getContentResolver());
                    tempIntent = targetIntent;
                } catch (Throwable th4) {
                    targetUser = th4;
                    Binder.restoreCallingIdentity(ident);
                    throw targetUser;
                }
            }
            Intent tempIntent2 = tempIntent;
            String tempResolvedType2 = tempResolvedType;
            try {
                if (mapForwardUserId.size() == 1) {
                    tempUserId3 = mapForwardUserId.get(INTENT_FORWARD_USER_ID).intValue();
                } else {
                    tempUserId3 = tempUserId2;
                }
                if (shouldCheckDual) {
                    try {
                        tempIntent2.setHwFlags(tempIntent2.getHwFlags() & -1025);
                    } catch (Throwable th5) {
                        targetUser = th5;
                        Binder.restoreCallingIdentity(ident);
                        throw targetUser;
                    }
                }
                if (tempUserId3 != tempUserId) {
                    tempIntent2.prepareToLeaveUser(tempUserId);
                    hwActivityStarter.standardizeIntentUriForClone(tempIntent2, tempUserId, (StorageManager) hwActivityStarter.mService.mContext.getSystemService("storage"));
                }
                Binder.restoreCallingIdentity(ident);
                String tempResolvedType3 = tempResolvedType2;
                Intent tempIntent3 = tempIntent2;
                int tempUserId4 = tempUserId3;
                int startActivityMayWait = HwActivityStarter.super.startActivityMayWait(caller, callingUid, callingPackage, requestRealCallingPid, requestRealCallingUid, tempIntent3, tempResolvedType3, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, startFlags, profilerInfo, outResult, globalConfig, options, isIgnoreTargetSecurity, tempUserId4, inTask, reason, isAllowPendingRemoteAnimationRegistryLookup, originatingPendingIntent, isAllowBackgroundActivityStart);
                if (ActivityTaskManagerDebugConfig.DEBUG_HW_ACTIVITY) {
                    Trace.traceEnd(j);
                }
                Flog.i(101, "startActivityMayWait cost " + (SystemClock.uptimeMillis() - start));
                return startActivityMayWait;
            } catch (Throwable th6) {
                targetUser = th6;
                Binder.restoreCallingIdentity(ident);
                throw targetUser;
            }
        } catch (Throwable th7) {
            targetUser = th7;
            i = 101;
            j = 64;
            if (ActivityTaskManagerDebugConfig.DEBUG_HW_ACTIVITY) {
            }
            Flog.i(i, "startActivityMayWait cost " + (SystemClock.uptimeMillis() - start));
            throw targetUser;
        }
    }

    private boolean shouldDisplayClonedAppToChoose(String callerPackageName, Intent intent, String resolvedType, ResolveInfo resolveInfo, int userId, Map<String, Integer> mapForwardUserId, boolean isShouldCheckDual) {
        UserInfo clonedProfile;
        if ((callerPackageName == null && !isShouldCheckDual) || startFromInExpectApps(callerPackageName) || resolveInfo.activityInfo.packageName.equals(callerPackageName) || (intent.getHwFlags() & 2) != 0) {
            return false;
        }
        UserInfo clonedProfile2 = null;
        if ((isShouldCheckDual || HwPackageManagerService.isSupportCloneAppInCust(callerPackageName)) && userId != 0) {
            clonedProfile2 = this.mService.mUserManagerInternal.findClonedProfile();
            if (clonePartialNeedRespond(clonedProfile2, userId, intent, resolvedType, resolveInfo, mapForwardUserId)) {
                return false;
            }
        }
        if (!HwPackageManagerService.isSupportCloneAppInCust(resolveInfo.activityInfo.packageName)) {
            return false;
        }
        if (clonedProfile2 == null) {
            clonedProfile = this.mService.mUserManagerInternal.findClonedProfile();
        } else {
            clonedProfile = clonedProfile2;
        }
        if (clonedProfile != null) {
            if (clonedProfile.id == userId || clonedProfile.profileGroupId == userId) {
                if (this.mSupervisor.resolveIntent(intent, resolvedType, clonedProfile.id) == null) {
                    return false;
                }
                if (callerPackageName == null) {
                    return true;
                }
                List<ResolveInfo> homeResolveInfos = new ArrayList<>();
                try {
                    AppGlobals.getPackageManager().getHomeActivities(homeResolveInfos);
                } catch (RemoteException e) {
                    Flog.e(101, "Failed to getHomeActivities from PackageManager.", e);
                }
                for (ResolveInfo ri : homeResolveInfos) {
                    if (callerPackageName.equals(ri.activityInfo.packageName)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean clonePartialNeedRespond(UserInfo clonedProfile, int userId, Intent intent, String resolvedType, ResolveInfo resolveInfo, Map<String, Integer> mapForwardUserId) {
        ResolveInfo infoForParent;
        if (clonedProfile == null || clonedProfile.id != userId || (infoForParent = this.mSupervisor.resolveIntent(intent, resolvedType, clonedProfile.profileGroupId)) == null || infoForParent.activityInfo.getComponentName().equals(resolveInfo.activityInfo.getComponentName())) {
            return false;
        }
        mapForwardUserId.put(INTENT_FORWARD_USER_ID, Integer.valueOf(clonedProfile.profileGroupId));
        Flog.i(101, "startActivityMayWait forward intent from clone user " + clonedProfile.id + " to parent user " + clonedProfile.profileGroupId + " because clone user just has partial target apps to respond.");
        return true;
    }

    private boolean startFromInExpectApps(String callerPackageName) {
        if ("com.huawei.android.launcher".equals(callerPackageName) || AppStartupDataMgr.HWPUSH_PKGNAME.equals(callerPackageName) || FingerViewController.PKGNAME_OF_KEYGUARD.equals(callerPackageName) || "com.android.settings".equals(callerPackageName)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean standardizeHomeIntent(ResolveInfo resolveInfo, Intent intent) {
        if (resolveInfo == null || resolveInfo.activityInfo == null || intent == null || !HOME_COMPONENTS.contains(new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name)) || isHomeIntent(intent)) {
            return false;
        }
        ComponentName cn = intent.getComponent();
        String packageName = cn != null ? cn.getPackageName() : intent.getPackage();
        intent.setComponent(null);
        if (packageName != null) {
            intent.setPackage(packageName);
        }
        Set<String> tempCategories = intent.getCategories();
        if (tempCategories != null) {
            tempCategories.clear();
        }
        intent.addCategory("android.intent.category.HOME");
        if (IS_SHOW_CUST_HOME_SCREEN) {
            return true;
        }
        intent.setAction("android.intent.action.MAIN");
        return true;
    }

    private boolean isHomeIntent(Intent intent) {
        return (("android.intent.action.MAIN".equals(intent.getAction()) && intent.hasCategory("android.intent.category.HOME") && intent.getCategories().size() == 1) && intent.getData() == null && intent.getComponent() == null) && intent.getType() == null;
    }

    private void standardizeIntentUriForClone(Intent intent, int userId, StorageManager storageManager) {
        StorageVolume[] volumes;
        StorageVolume[] volumes2;
        ClipData clipData = intent.getClipData();
        String volume = null;
        String sdcard = Environment.getLegacyExternalStorageDirectory().getAbsolutePath();
        char c = 0;
        if (clipData != null) {
            int itemCount = clipData.getItemCount();
            int i = 0;
            while (i < itemCount) {
                ClipData.Item item = clipData.getItemAt(i);
                Uri uri = item.getUri();
                if (uri != null && "file".equals(uri.getScheme()) && uri.getPath().startsWith(sdcard)) {
                    if (volume == null) {
                        StorageVolume[] volumes3 = StorageManager.getVolumeList(userId, 512);
                        if (volumes3 == null || volumes3.length == 0) {
                            break;
                        }
                        volume = volumes3[c].getPath();
                    }
                    clipData.setItemAt(i, new ClipData.Item(item.getText(), item.getHtmlText(), item.getIntent(), Uri.parse(uri.toString().replace(sdcard, volume))));
                }
                i++;
                c = 0;
            }
        }
        Uri uri2 = intent.getData();
        if (uri2 != null && "file".equals(uri2.getScheme()) && uri2.getPath().startsWith(sdcard)) {
            if (volume == null && (volumes2 = StorageManager.getVolumeList(userId, 512)) != null && volumes2.length > 0) {
                volume = volumes2[0].getPath();
            }
            if (volume != null) {
                intent.setData(Uri.parse(uri2.toString().replace(sdcard, volume)));
            }
        }
        Uri stream = (Uri) intent.getParcelableExtra("android.intent.extra.STREAM");
        if (stream != null && "file".equals(stream.getScheme()) && stream.getPath().startsWith(sdcard)) {
            if (volume == null && (volumes = StorageManager.getVolumeList(userId, 512)) != null && volumes.length > 0) {
                volume = volumes[0].getPath();
            }
            if (volume != null) {
                intent.putExtra("android.intent.extra.STREAM", Uri.parse(stream.toString().replace(sdcard, volume)));
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean isInSkipCancelResultList(String clsName) {
        return sSkipCancelResults.contains(clsName);
    }

    private void showErrorDialogToRemoveUser(final Context context, final int userId) {
        UiThread.getHandler().post(new Runnable() {
            /* class com.android.server.wm.HwActivityStarter.AnonymousClass4 */

            public void run() {
                AlertDialog errorDialog = new AlertDialog.Builder(context, 33947691).setPositiveButton(33685650, new DialogInterface.OnClickListener() {
                    /* class com.android.server.wm.HwActivityStarter.AnonymousClass4.AnonymousClass2 */

                    public void onClick(DialogInterface dialog, int which) {
                        UserManager.get(context).removeUser(userId);
                    }
                }).setNegativeButton(17039360, new DialogInterface.OnClickListener() {
                    /* class com.android.server.wm.HwActivityStarter.AnonymousClass4.AnonymousClass1 */

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setTitle(context.getString(33685653)).setMessage(context.getString(33685652)).create();
                errorDialog.getWindow().setType(2003);
                errorDialog.getWindow().getAttributes().privateFlags |= 16;
                errorDialog.setCanceledOnTouchOutside(false);
                errorDialog.show();
                errorDialog.getButton(-1).setTextColor(-65536);
            }
        });
    }

    private boolean isFrpRestrictedApp(Context context, Intent intent, ActivityInfo activityInfo, int userId) {
        String frpToken = Settings.Secure.getStringForUser(context.getContentResolver(), "hw_frp_token", userId);
        String frpComps = Settings.Secure.getStringForUser(context.getContentResolver(), "hw_frp_comps", userId);
        if (!TextUtils.isEmpty(frpToken) || !TextUtils.isEmpty(frpComps)) {
            String frpCompsTemp = "," + frpComps + ",";
            if (!frpCompsTemp.contains("," + activityInfo.packageName + ",")) {
                if (!frpCompsTemp.contains("," + activityInfo.packageName + "/" + activityInfo.name + ",")) {
                    if (frpToken == null) {
                        return true;
                    }
                    try {
                        if (!frpToken.equals(intent.getStringExtra("hw_frp_token"))) {
                            return true;
                        }
                        Slog.i(TAG, activityInfo + " gets matched token in intent");
                        return false;
                    } catch (BadParcelableException e) {
                        Slog.e(TAG, "Parse extra failed!");
                        return false;
                    }
                }
            }
            Slog.i(TAG, activityInfo + " is in frp comps");
            return false;
        }
        Slog.i(TAG, "Frp items are Empty");
        return false;
    }

    private boolean isMultiWindowDisabled() {
        return this.mService.mHwATMSEx.getMultiWindowDisabled();
    }

    private boolean isHwMultiWindowMode(SafeActivityOptions options, Intent intent, ActivityInfo activityInfo, IApplicationThread caller) {
        ActivityOptions activityOptions;
        if (options == null || intent == null || activityInfo == null || caller == null || (activityOptions = options.getOptions(intent, activityInfo, this.mService.getProcessController(caller), this.mSupervisor)) == null) {
            return false;
        }
        int activityMode = activityOptions.getLaunchWindowingMode();
        if (activityMode != 100 && activityMode != 101 && activityMode != 102 && activityMode != 103) {
            return false;
        }
        Slog.i(TAG, "isHwMultiWindowMode true activityMode: " + activityMode);
        return true;
    }

    /* access modifiers changed from: protected */
    public void setInitialState(ActivityRecord r, ActivityOptions options, TaskRecord inTask, boolean doResume, int startFlags, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, boolean restrictedBgActivity) {
        if (HwMwUtils.ENABLED) {
            HwMwUtils.performPolicy(1, new Object[]{r, options});
        }
        HwActivityStarter.super.setInitialState(r, options, inTask, doResume, startFlags, sourceRecord, voiceSession, voiceInteractor, restrictedBgActivity);
    }
}
