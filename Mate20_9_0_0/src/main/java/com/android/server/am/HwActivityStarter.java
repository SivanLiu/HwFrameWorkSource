package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.app.WaitResult;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.hdm.HwDeviceManager;
import android.net.Uri;
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
import android.service.voice.IVoiceInteractionSession;
import android.text.TextUtils;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.HwBluetoothBigDataService;
import com.android.server.UiThread;
import com.android.server.gesture.GestureNavConst;
import com.android.server.pc.HwPCDataReporter;
import com.android.server.pm.HwPackageManagerService;
import com.android.server.wm.HwStartWindowRecord;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HwActivityStarter extends ActivityStarter {
    private static final Intent BROWSE_PROBE = new Intent().setAction("android.intent.action.VIEW").addCategory("android.intent.category.BROWSABLE").setData(Uri.parse("http:"));
    private static final ComponentName DOCOMOHOME_COMPONENT = new ComponentName("com.nttdocomo.android.dhome", "com.nttdocomo.android.dhome.HomeActivity");
    private static final ComponentName DRAWERHOME_COMPONENT = new ComponentName(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE, "com.huawei.android.launcher.drawer.DrawerLauncher");
    private static final String INTENT_FORWARD_USER_ID = "intent_forward_user_id";
    private static final boolean IS_SHOW_DCMUI = SystemProperties.getBoolean("ro.config.hw_show_dcmui", false);
    private static final ComponentName NEWSIMPLEHOME_COMPONENT = new ComponentName(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE, "com.huawei.android.launcher.newsimpleui.NewSimpleLauncher");
    private static final ComponentName SIMPLEHOME_COMPONENT = new ComponentName(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE, "com.huawei.android.launcher.simpleui.SimpleUILauncher");
    private static final String TAG = "ActivityStarter";
    private static final ComponentName UNIHOME_COMPONENT = new ComponentName(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE, "com.huawei.android.launcher.unihome.UniHomeLauncher");
    private static final HashSet<ComponentName> sHomecomponent = new HashSet<ComponentName>() {
        {
            if (SystemProperties.getBoolean("ro.config.hw_show_dcmui", false)) {
                add(HwActivityStarter.DOCOMOHOME_COMPONENT);
                return;
            }
            add(HwActivityStarter.UNIHOME_COMPONENT);
            add(HwActivityStarter.DRAWERHOME_COMPONENT);
            add(HwActivityStarter.SIMPLEHOME_COMPONENT);
            add(HwActivityStarter.NEWSIMPLEHOME_COMPONENT);
        }
    };
    private static Set<String> sPCPkgName = new HashSet();
    private static Set<String> sSkipCancelResultList = new HashSet();
    private boolean mIsStartupGuideFinished;

    static {
        sSkipCancelResultList.add("com.huawei.systemmanager/.applock.password.AuthLaunchLockedAppActivity");
        sPCPkgName.add("com.huawei.android.hwpay");
        sPCPkgName.add(HwBluetoothBigDataService.BIGDATA_RECEIVER_PACKAGENAME);
        sPCPkgName.add("com.huawei.screenrecorder");
    }

    public HwActivityStarter(ActivityStartController controller, ActivityManagerService service, ActivityStackSupervisor supervisor, ActivityStartInterceptor interceptor) {
        super(controller, service, supervisor, interceptor);
    }

    int startActivity(IApplicationThread caller, Intent intent, Intent ephemeralIntent, String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, int realCallingPid, int realCallingUid, int startFlags, SafeActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified, ActivityRecord[] outActivity, TaskRecord inTask, String reason, boolean allowPendingRemoteAnimationRegistryLookup) {
        Intent intent2 = intent;
        ActivityInfo activityInfo = aInfo;
        ActivityRecord[] activityRecordArr = outActivity;
        StringBuilder stringBuilder;
        if (intent2.getComponent() != null && HwDeviceManager.disallowOp(4, intent2.getComponent().getPackageName())) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            stringBuilder.append(intent2.getComponent().getPackageName());
            stringBuilder.append("] is disallowed running by MDM apk");
            Flog.i(101, stringBuilder.toString());
            UiThread.getHandler().post(new Runnable() {
                public void run() {
                    Context context = HwActivityStarter.this.mService.mUiContext;
                    if (context != null) {
                        Toast toast = Toast.makeText(context, context.getString(33686103), 0);
                        LayoutParams windowParams = toast.getWindowParams();
                        windowParams.privateFlags |= 16;
                        toast.show();
                    }
                }
            });
            return -96;
        } else if (isApplicationDisabledByMDM(activityInfo, intent2, resolvedType)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Application is disabled by MDM, intent = ");
            stringBuilder.append(intent2);
            Flog.i(101, stringBuilder.toString());
            return -92;
        } else {
            int i = 0;
            int userId = activityInfo != null ? UserHandle.getUserId(activityInfo.applicationInfo.uid) : 0;
            if (!(mLauncherStartState.containsKey(Integer.valueOf(userId)) && ((Boolean) mLauncherStartState.get(Integer.valueOf(userId))).booleanValue()) && isFrpRestricted(this.mService.mContext, userId) && isStartBrowserApps(activityInfo, userId)) {
                Log.w(TAG, "forbid launching browser because frp is restricted");
                return 0;
            }
            if (activityInfo != null) {
                HwStartWindowRecord.getInstance().setStartFromMainAction(Integer.valueOf(activityInfo.applicationInfo.uid), "android.intent.action.MAIN".equals(intent2.getAction()));
            }
            int startResult = super.startActivity(caller, intent, ephemeralIntent, resolvedType, aInfo, rInfo, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid, callingUid, callingPackage, realCallingPid, realCallingUid, startFlags, options, ignoreTargetSecurity, componentSpecified, outActivity, inTask, reason, allowPendingRemoteAnimationRegistryLookup);
            try {
                if (ActivityManager.isStartResultSuccessful(startResult) && HwPCUtils.isPcCastModeInServer() && !HwPCUtils.enabledInPad()) {
                    while (i < activityRecordArr.length) {
                        ActivityRecord r = activityRecordArr[i];
                        if (r.getWindowingMode() != 1) {
                            if (r.getActivityType() != 2 || !r.fullscreen) {
                                i++;
                            }
                        }
                        HwPCUtils.getHwPCManager().setScreenPower(true);
                    }
                }
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("startActivityLocked ");
                stringBuilder2.append(e);
                HwPCUtils.log(str, stringBuilder2.toString());
            }
            return startResult;
        }
    }

    private boolean isApplicationDisabledByMDM(ActivityInfo aInfo, Intent intent, String resolvedType) {
        boolean mdmDisabnled = false;
        if (intent.getComponent() == null) {
            ResolveInfo info = this.mSupervisor.resolveIntent(intent, resolvedType, aInfo != null ? UserHandle.getUserId(aInfo.applicationInfo.uid) : 0, 131584, Binder.getCallingUid());
            if (!(info == null || info.activityInfo == null)) {
                mdmDisabnled = HwDeviceManager.mdmDisallowOp(21, new Intent().setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name)));
            }
        } else {
            mdmDisabnled = HwDeviceManager.mdmDisallowOp(21, intent);
        }
        if (mdmDisabnled) {
            UiThread.getHandler().post(new Runnable() {
                public void run() {
                    Toast.makeText(HwActivityStarter.this.mService.mContext, HwActivityStarter.this.mService.mContext.getResources().getString(33685904), 0).show();
                }
            });
        }
        return mdmDisabnled;
    }

    /* JADX WARNING: Removed duplicated region for block: B:212:0x03a2  */
    /* JADX WARNING: Removed duplicated region for block: B:212:0x03a2  */
    /* JADX WARNING: Removed duplicated region for block: B:212:0x03a2  */
    /* JADX WARNING: Removed duplicated region for block: B:212:0x03a2  */
    /* JADX WARNING: Removed duplicated region for block: B:203:0x036c  */
    /* JADX WARNING: Missing block: B:41:?, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append("startActivityMayWait, callerApp: ");
            r0.append(r8);
            r0.append(", intent: ");
            r0.append(r9);
            r0.append(", userId = ");
            r0.append(r10);
            r0.append(", callingUid = ");
            r0.append(android.os.Binder.getCallingUid());
            android.util.Flog.i(101, r0.toString());
     */
    /* JADX WARNING: Missing block: B:42:0x00cb, code skipped:
            if (r10 != 0) goto L_0x0108;
     */
    /* JADX WARNING: Missing block: B:44:0x00d1, code skipped:
            if (r35.getComponent() == null) goto L_0x0108;
     */
    /* JADX WARNING: Missing block: B:46:0x00df, code skipped:
            if (com.android.server.pm.HwPackageManagerService.isSupportCloneAppInCust(r35.getComponent().getPackageName()) != false) goto L_0x0108;
     */
    /* JADX WARNING: Missing block: B:47:0x00e1, code skipped:
            r0 = super.startActivityMayWait(r32, r33, r34, r35, r36, r37, r38, r39, r40, r41, r42, r43, r44, r45, r46, r47, r48, r49, r50, r51);
     */
    /* JADX WARNING: Missing block: B:49:0x00e7, code skipped:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_HW_ACTIVITY == false) goto L_0x00ec;
     */
    /* JADX WARNING: Missing block: B:50:0x00e9, code skipped:
            android.os.Trace.traceEnd(64);
     */
    /* JADX WARNING: Missing block: B:51:0x00ec, code skipped:
            r1 = new java.lang.StringBuilder();
            r1.append("startActivityMayWait cost ");
            r1.append(android.os.SystemClock.uptimeMillis() - r22);
            android.util.Flog.i(101, r1.toString());
     */
    /* JADX WARNING: Missing block: B:52:0x0107, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:55:0x010c, code skipped:
            r6 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:57:?, code skipped:
            r5 = new java.util.HashMap();
            r4 = r10;
     */
    /* JADX WARNING: Missing block: B:58:0x011b, code skipped:
            if ((r35.getHwFlags() & 1024) == 0) goto L_0x011f;
     */
    /* JADX WARNING: Missing block: B:59:0x011d, code skipped:
            r0 = 1;
     */
    /* JADX WARNING: Missing block: B:60:0x011f, code skipped:
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:61:0x0120, code skipped:
            r16 = r0;
            r2 = r36;
     */
    /* JADX WARNING: Missing block: B:62:0x012a, code skipped:
            r1 = r15.mSupervisor.resolveIntent(r9, r2, r10);
     */
    /* JADX WARNING: Missing block: B:63:0x012e, code skipped:
            if (r1 != null) goto L_0x01f0;
     */
    /* JADX WARNING: Missing block: B:65:?, code skipped:
            r0 = r15.mService.mUserController.mInjector.getUserManagerInternal().getUserInfo(r10);
     */
    /* JADX WARNING: Missing block: B:66:0x013e, code skipped:
            if (r0 == null) goto L_0x01d8;
     */
    /* JADX WARNING: Missing block: B:68:0x0144, code skipped:
            if (r0.isClonedProfile() == false) goto L_0x01d8;
     */
    /* JADX WARNING: Missing block: B:70:0x014a, code skipped:
            if (android.os.storage.StorageManager.isUserKeyUnlocked(r48) != false) goto L_0x019d;
     */
    /* JADX WARNING: Missing block: B:72:0x0152, code skipped:
            if (android.os.storage.StorageManager.isUserKeyUnlocked(r0.profileGroupId) == false) goto L_0x019d;
     */
    /* JADX WARNING: Missing block: B:74:0x015a, code skipped:
            if (r15.mService.mUserController.mHaveTryCloneProUserUnlock == false) goto L_0x0164;
     */
    /* JADX WARNING: Missing block: B:75:0x015c, code skipped:
            showErrorDialogToRemoveUser(r15.mService.mContext, r10);
     */
    /* JADX WARNING: Missing block: B:78:?, code skipped:
            android.util.Slog.i(TAG, "Wait for CloneProfile user unLock, return!");
     */
    /* JADX WARNING: Missing block: B:80:?, code skipped:
            android.os.Binder.restoreCallingIdentity(r6);
     */
    /* JADX WARNING: Missing block: B:82:0x0171, code skipped:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_HW_ACTIVITY == false) goto L_0x0176;
     */
    /* JADX WARNING: Missing block: B:83:0x0173, code skipped:
            android.os.Trace.traceEnd(64);
     */
    /* JADX WARNING: Missing block: B:84:0x0176, code skipped:
            r3 = new java.lang.StringBuilder();
            r3.append("startActivityMayWait cost ");
            r3.append(android.os.SystemClock.uptimeMillis() - r22);
            android.util.Flog.i(101, r3.toString());
     */
    /* JADX WARNING: Missing block: B:85:0x0194, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:86:0x0195, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:87:0x0196, code skipped:
            r25 = r2;
            r1 = 64;
            r3 = 101;
     */
    /* JADX WARNING: Missing block: B:89:?, code skipped:
            r3 = r0.profileGroupId;
     */
    /* JADX WARNING: Missing block: B:91:?, code skipped:
            r10 = new java.lang.StringBuilder();
            r10.append("startActivityMayWait forward intent from clone user ");
            r10.append(r0.id);
            r10.append(" to parent user ");
            r10.append(r3);
            r10.append(" because clone user has non target apps to respond.");
     */
    /* JADX WARNING: Missing block: B:92:0x01c0, code skipped:
            r12 = 101;
     */
    /* JADX WARNING: Missing block: B:94:?, code skipped:
            android.util.Flog.i(101, r10.toString());
     */
    /* JADX WARNING: Missing block: B:95:0x01c5, code skipped:
            r10 = r3;
     */
    /* JADX WARNING: Missing block: B:96:0x01c7, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:97:0x01c8, code skipped:
            r1 = r2;
            r10 = r3;
     */
    /* JADX WARNING: Missing block: B:98:0x01cb, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:99:0x01cc, code skipped:
            r1 = r2;
            r10 = r3;
     */
    /* JADX WARNING: Missing block: B:100:0x01cf, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:101:0x01d0, code skipped:
            r1 = r2;
     */
    /* JADX WARNING: Missing block: B:102:0x01d1, code skipped:
            r2 = r6;
            r17 = r8;
     */
    /* JADX WARNING: Missing block: B:104:0x01d8, code skipped:
            r13 = r4;
            r14 = r5;
            r28 = r6;
            r17 = r8;
            r8 = 101;
            r30 = r2;
            r2 = r1;
            r1 = r30;
     */
    /* JADX WARNING: Missing block: B:105:0x01e8, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:106:0x01e9, code skipped:
            r1 = r2;
     */
    /* JADX WARNING: Missing block: B:107:0x01ea, code skipped:
            r2 = r6;
            r17 = r8;
            r8 = r12;
     */
    /* JADX WARNING: Missing block: B:108:0x01f0, code skipped:
            if (r8 != null) goto L_0x0200;
     */
    /* JADX WARNING: Missing block: B:109:0x01f2, code skipped:
            if (r16 == false) goto L_0x01f5;
     */
    /* JADX WARNING: Missing block: B:110:0x01f5, code skipped:
            r2 = r1;
            r13 = r4;
            r14 = r5;
            r28 = r6;
            r17 = r8;
            r8 = 101;
     */
    /* JADX WARNING: Missing block: B:113:0x0202, code skipped:
            if (r1.activityInfo == null) goto L_0x02d1;
     */
    /* JADX WARNING: Missing block: B:115:0x0205, code skipped:
            if (r8 != null) goto L_0x0209;
     */
    /* JADX WARNING: Missing block: B:116:0x0207, code skipped:
            r12 = null;
     */
    /* JADX WARNING: Missing block: B:118:?, code skipped:
            r12 = r8.info.packageName;
     */
    /* JADX WARNING: Missing block: B:119:0x020d, code skipped:
            r27 = r1;
            r2 = r12;
            r12 = 1;
            r13 = r4;
            r14 = r5;
            r28 = r6;
            r17 = r8;
     */
    /* JADX WARNING: Missing block: B:122:0x0225, code skipped:
            if (shouldDisplayClonedAppToChoose(r2, r9, r36, r27, r10, r14, r16) == null) goto L_0x029d;
     */
    /* JADX WARNING: Missing block: B:123:0x0227, code skipped:
            r9.addHwFlags(2);
            r2 = r27;
            r9.setComponent(new android.content.ComponentName(r2.activityInfo.packageName, r2.activityInfo.name));
     */
    /* JADX WARNING: Missing block: B:124:0x023d, code skipped:
            r1 = null;
     */
    /* JADX WARNING: Missing block: B:126:?, code skipped:
            standardizeIntentUriForClone(r9, r13);
            r1 = android.content.Intent.createChooser(r9, r15.mService.mContext.getResources().getText(17041359));
            r1.setFlags(r35.getFlags() & -536870913);
     */
    /* JADX WARNING: Missing block: B:127:0x0260, code skipped:
            r8 = 101;
     */
    /* JADX WARNING: Missing block: B:128:0x0264, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:129:0x0265, code skipped:
            r1 = r36;
            r2 = r28;
     */
    /* JADX WARNING: Missing block: B:130:0x026b, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:132:?, code skipped:
            r3 = new java.lang.StringBuilder();
            r3.append("startActivityMayWait, fail to create chooser for ");
            r3.append(r9);
     */
    /* JADX WARNING: Missing block: B:133:0x027e, code skipped:
            r8 = 101;
     */
    /* JADX WARNING: Missing block: B:135:?, code skipped:
            android.util.Flog.e(101, r3.toString(), r0);
     */
    /* JADX WARNING: Missing block: B:145:0x029d, code skipped:
            r2 = r27;
            r8 = 101;
     */
    /* JADX WARNING: Missing block: B:147:?, code skipped:
            r9.setHwFlags(r35.getHwFlags() & -3);
     */
    /* JADX WARNING: Missing block: B:148:0x02aa, code skipped:
            r1 = r36;
     */
    /* JADX WARNING: Missing block: B:153:0x02c0, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:155:0x02c2, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:157:0x02c5, code skipped:
            r1 = r36;
     */
    /* JADX WARNING: Missing block: B:158:0x02c8, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:159:0x02c9, code skipped:
            r17 = r8;
            r1 = r36;
            r2 = r6;
     */
    /* JADX WARNING: Missing block: B:160:0x02d1, code skipped:
            r2 = r1;
            r13 = r4;
            r14 = r5;
            r28 = r6;
            r17 = r8;
            r8 = 101;
     */
    /* JADX WARNING: Missing block: B:161:0x02da, code skipped:
            r1 = r36;
     */
    /* JADX WARNING: Missing block: B:164:0x02e8, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:171:0x02fd, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:172:0x02fe, code skipped:
            r2 = r6;
            r17 = r8;
     */
    /* JADX WARNING: Missing block: B:173:0x0304, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:174:0x0305, code skipped:
            r2 = r6;
            r17 = r8;
            r8 = 101;
     */
    /* JADX WARNING: Missing block: B:175:0x0309, code skipped:
            r1 = r36;
     */
    /* JADX WARNING: Missing block: B:178:0x030f, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:179:0x0310, code skipped:
            r25 = r1;
     */
    /* JADX WARNING: Missing block: B:190:0x0322, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:191:0x0323, code skipped:
            r25 = r36;
     */
    /* JADX WARNING: Missing block: B:192:0x0325, code skipped:
            r3 = r8;
            r1 = 64;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int startActivityMayWait(IApplicationThread caller, int callingUid, String callingPackage, Intent intent, String resolvedType, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, WaitResult outResult, Configuration globalConfig, SafeActivityOptions options, boolean ignoreTargetSecurity, int userId, TaskRecord inTask, String reason, boolean allowPendingRemoteAnimationRegistryLookup) {
        Throwable th;
        ProcessRecord callerApplication;
        int i;
        long j;
        StringBuilder stringBuilder;
        Intent intent2 = intent;
        int i2 = userId;
        long start = SystemClock.uptimeMillis();
        int i3 = 101;
        if (ActivityManagerDebugConfig.DEBUG_HW_ACTIVITY) {
            try {
                Trace.traceBegin(64, "startActivityMayWait");
            } catch (Throwable th2) {
                th = th2;
            }
        }
        String resolvedType2;
        try {
            IApplicationThread iApplicationThread;
            int startActivityMayWait;
            String resolvedType3 = null;
            if (HwPCUtils.enabledInPad()) {
                if (HwPCUtils.isPcCastModeInServer() && intent2 != null && "com.huawei.android.projectmenu".equals(intent.getPackage())) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("startActivityMayWait intent: ");
                    stringBuilder2.append(intent2);
                    HwPCUtils.log("HwActivityStarter", stringBuilder2.toString());
                    if (ActivityManagerDebugConfig.DEBUG_HW_ACTIVITY) {
                        Trace.traceEnd(64);
                    }
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("startActivityMayWait cost ");
                    stringBuilder3.append(SystemClock.uptimeMillis() - start);
                    Flog.i(101, stringBuilder3.toString());
                    return 0;
                }
            }
            if (HwActivityManagerService.IS_SUPPORT_CLONE_APP) {
                try {
                    if (this.mService.mUserController.mInjector.getUserManagerInternal().hasClonedProfile()) {
                        synchronized (this.mService) {
                            try {
                                iApplicationThread = caller;
                                try {
                                    callerApplication = this.mService.getRecordForAppLocked(iApplicationThread);
                                    try {
                                    } catch (Throwable th3) {
                                        th = th3;
                                        ProcessRecord processRecord = callerApplication;
                                        callerApplication = 101;
                                        ProcessRecord callerApplication2 = processRecord;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th4) {
                                                th = th4;
                                            }
                                        }
                                        throw th;
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                    callerApplication = 101;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            } catch (Throwable th6) {
                                th = th6;
                                iApplicationThread = caller;
                                callerApplication = 101;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        }
                    }
                } catch (Throwable th7) {
                    th = th7;
                    iApplicationThread = caller;
                    i = 101;
                    j = 64;
                    if (ActivityManagerDebugConfig.DEBUG_HW_ACTIVITY) {
                        Trace.traceEnd(j);
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("startActivityMayWait cost ");
                    stringBuilder.append(SystemClock.uptimeMillis() - start);
                    Flog.i(i, stringBuilder.toString());
                    throw th;
                }
            }
            iApplicationThread = caller;
            callerApplication = 101;
            resolvedType2 = resolvedType;
            Intent intent3 = intent2;
            int userId2 = i2;
            ProcessRecord processRecord2 = callerApplication;
            try {
                startActivityMayWait = super.startActivityMayWait(iApplicationThread, callingUid, callingPackage, intent3, resolvedType2, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, startFlags, profilerInfo, outResult, globalConfig, options, ignoreTargetSecurity, userId2, inTask, reason, allowPendingRemoteAnimationRegistryLookup);
                if (ActivityManagerDebugConfig.DEBUG_HW_ACTIVITY) {
                    Trace.traceEnd(64);
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("startActivityMayWait cost ");
                stringBuilder.append(SystemClock.uptimeMillis() - start);
                Flog.i(101, stringBuilder.toString());
                return startActivityMayWait;
            } catch (Throwable th8) {
                th = th8;
                j = 64;
                i = 101;
                intent2 = intent3;
                i2 = userId2;
                if (ActivityManagerDebugConfig.DEBUG_HW_ACTIVITY) {
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("startActivityMayWait cost ");
                stringBuilder.append(SystemClock.uptimeMillis() - start);
                Flog.i(i, stringBuilder.toString());
                throw th;
            }
            String resolvedType4;
            if (rInfo != null) {
                Intent intent4 = rInfo;
                try {
                    resolvedType4 = intent4.resolveTypeIfNeeded(this.mService.mContext.getContentResolver());
                    intent2 = intent4;
                } catch (Throwable th9) {
                    th = th9;
                    resolvedType3 = resolvedType;
                    intent2 = intent4;
                    long ident = ident;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            }
            resolvedType4 = resolvedType;
            resolvedType3 = resolvedType4;
            if (mapForwardUserId.size() == i3) {
                i2 = ((Integer) mapForwardUserId.get(INTENT_FORWARD_USER_ID)).intValue();
            }
            if (shouldCheckDual) {
                intent2.setHwFlags(intent2.getHwFlags() & -1025);
            }
            if (i2 != initialTargetUser) {
                intent2.prepareToLeaveUser(initialTargetUser);
                standardizeIntentUriForClone(intent2, initialTargetUser);
            }
            Binder.restoreCallingIdentity(ident);
            resolvedType2 = resolvedType3;
            Intent intent32 = intent2;
            int userId22 = i2;
            ProcessRecord processRecord22 = callerApplication;
            startActivityMayWait = super.startActivityMayWait(iApplicationThread, callingUid, callingPackage, intent32, resolvedType2, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, startFlags, profilerInfo, outResult, globalConfig, options, ignoreTargetSecurity, userId22, inTask, reason, allowPendingRemoteAnimationRegistryLookup);
            if (ActivityManagerDebugConfig.DEBUG_HW_ACTIVITY) {
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("startActivityMayWait cost ");
            stringBuilder.append(SystemClock.uptimeMillis() - start);
            Flog.i(101, stringBuilder.toString());
            return startActivityMayWait;
        } catch (Throwable th10) {
            th = th10;
            i = 101;
            j = 64;
            resolvedType2 = resolvedType;
            if (ActivityManagerDebugConfig.DEBUG_HW_ACTIVITY) {
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("startActivityMayWait cost ");
            stringBuilder.append(SystemClock.uptimeMillis() - start);
            Flog.i(i, stringBuilder.toString());
            throw th;
        }
    }

    /* JADX WARNING: Missing block: B:61:0x010f, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:62:0x0110, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean shouldDisplayClonedAppToChoose(String callerPackageName, Intent intent, String resolvedType, ResolveInfo rInfo, int userId, Map<String, Integer> mapForwardUserId, boolean shouldCheckDual) {
        if ((callerPackageName == null && !shouldCheckDual) || GestureNavConst.DEFAULT_LAUNCHER_PACKAGE.equals(callerPackageName) || "android".equals(callerPackageName) || FingerViewController.PKGNAME_OF_KEYGUARD.equals(callerPackageName) || "com.android.settings".equals(callerPackageName) || rInfo.activityInfo.packageName.equals(callerPackageName) || (intent.getHwFlags() & 2) != 0) {
            return false;
        }
        UserInfo clonedProfile = null;
        if ((shouldCheckDual || HwPackageManagerService.isSupportCloneAppInCust(callerPackageName)) && userId != 0) {
            clonedProfile = this.mService.mUserController.mInjector.getUserManagerInternal().findClonedProfile();
            if (clonedProfile != null && clonedProfile.id == userId) {
                ResolveInfo infoForParent = this.mSupervisor.resolveIntent(intent, resolvedType, clonedProfile.profileGroupId);
                if (!(infoForParent == null || infoForParent.activityInfo.getComponentName().equals(rInfo.activityInfo.getComponentName()))) {
                    mapForwardUserId.put(INTENT_FORWARD_USER_ID, Integer.valueOf(clonedProfile.profileGroupId));
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("startActivityMayWait forward intent from clone user ");
                    stringBuilder.append(clonedProfile.id);
                    stringBuilder.append(" to parent user ");
                    stringBuilder.append(clonedProfile.profileGroupId);
                    stringBuilder.append(" because clone user just has partial target apps to respond.");
                    Flog.i(101, stringBuilder.toString());
                    return false;
                }
            }
        }
        if (!HwPackageManagerService.isSupportCloneAppInCust(rInfo.activityInfo.packageName)) {
            return false;
        }
        if (clonedProfile == null) {
            clonedProfile = this.mService.mUserController.mInjector.getUserManagerInternal().findClonedProfile();
        }
        if (clonedProfile == null || ((clonedProfile.id != userId && clonedProfile.profileGroupId != userId) || this.mSupervisor.resolveIntent(intent, resolvedType, clonedProfile.id) == null)) {
            return false;
        }
        if (callerPackageName != null) {
            List<ResolveInfo> homeResolveInfos = new ArrayList();
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
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:25:0x0059, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean standardizeHomeIntent(ResolveInfo rInfo, Intent intent) {
        if (rInfo == null || rInfo.activityInfo == null || intent == null || !sHomecomponent.contains(new ComponentName(rInfo.activityInfo.applicationInfo.packageName, rInfo.activityInfo.name)) || isHomeIntent(intent)) {
            return false;
        }
        ComponentName cn = intent.getComponent();
        String packageName = cn != null ? cn.getPackageName() : intent.getPackage();
        intent.setComponent(null);
        if (packageName != null) {
            intent.setPackage(packageName);
        }
        Set<String> s = intent.getCategories();
        if (s != null) {
            s.clear();
        }
        intent.addCategory("android.intent.category.HOME");
        if (!IS_SHOW_DCMUI) {
            intent.setAction("android.intent.action.MAIN");
        }
        return true;
    }

    private boolean isHomeIntent(Intent intent) {
        return "android.intent.action.MAIN".equals(intent.getAction()) && intent.hasCategory("android.intent.category.HOME") && intent.getCategories().size() == 1 && intent.getData() == null && intent.getComponent() == null && intent.getType() == null;
    }

    public boolean startingCustomActivity(boolean abort, Intent intent, ActivityInfo aInfo) {
        if (((HwActivityManagerService) this.mService).mCustomController != null) {
            return ((HwActivityManagerService) this.mService).customActivityStarting(intent, aInfo.applicationInfo.packageName);
        }
        return abort;
    }

    protected void setInitialState(ActivityRecord r, ActivityOptions options, TaskRecord inTask, boolean doResume, int startFlags, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor) {
        ActivityRecord activityRecord = r;
        ActivityOptions activityOptions = options;
        if (HwPCUtils.isPcCastModeInServer()) {
            if (!HwPCUtils.enabledInPad() && activityOptions != null && HwPCUtils.isValidExtDisplayId(activityOptions.getLaunchDisplayId()) && (TextUtils.equals(activityRecord.packageName, "com.android.incallui") || TextUtils.equals(activityRecord.packageName, FingerViewController.PKGNAME_OF_KEYGUARD))) {
                activityOptions.setLaunchDisplayId(0);
            }
            if (HwPCUtils.enabledInPad() && (this.mService instanceof HwActivityManagerService) && !((HwActivityManagerService) this.mService).isLauncher(activityRecord.packageName) && activityOptions == null) {
                activityOptions = ActivityOptions.makeBasic();
            }
        }
        super.setInitialState(activityRecord, activityOptions, inTask, doResume, startFlags, sourceRecord, voiceSession, voiceInteractor);
    }

    protected int getPreferedDisplayId(ActivityRecord sourceRecord, ActivityRecord startingActivity, ActivityOptions options) {
        if (HwPCUtils.isPcCastModeInServer()) {
            if (startingActivity != null && startingActivity.isActivityTypeHome() && "android".equals(startingActivity.launchedFromPackage)) {
                return 0;
            }
            if (options != null && HwPCUtils.isValidExtDisplayId(options.getLaunchDisplayId())) {
                return options.getLaunchDisplayId();
            }
            if (startingActivity != null && (this.mService instanceof HwActivityManagerService)) {
                HashMap<String, Integer> maps = ((HwActivityManagerService) this.mService).mPkgDisplayMaps;
                int displayId = 0;
                if (TextUtils.isEmpty(startingActivity.launchedFromPackage)) {
                    if (!TextUtils.isEmpty(startingActivity.packageName) && maps.containsKey(startingActivity.packageName)) {
                        displayId = ((Integer) maps.get(startingActivity.packageName)).intValue();
                    }
                } else if (maps.containsKey(startingActivity.launchedFromPackage)) {
                    displayId = ((Integer) maps.get(startingActivity.launchedFromPackage)).intValue();
                }
                if (HwPCUtils.isValidExtDisplayId(displayId)) {
                    return displayId;
                }
            }
        }
        return super.getPreferedDisplayId(sourceRecord, startingActivity, options);
    }

    protected int hasStartedOnOtherDisplay(ActivityRecord startActivity, int sourceDisplayId) {
        if (HwPCUtils.isPcCastModeInServer() && !sPCPkgName.contains(startActivity.packageName)) {
            String activityName = startActivity.realActivity != null ? startActivity.realActivity.getClassName() : "";
            if (packageShouldNotHandle(startActivity.packageName) && !"com.huawei.filemanager.desktopinstruction.EasyProjection".equals(activityName)) {
                return -1;
            }
            if (HwPCUtils.isValidExtDisplayId(sourceDisplayId) && startActivity.isActivityTypeHome()) {
                return 2;
            }
            ArrayList<ProcessRecord> list;
            if ("com.huawei.filemanager.desktopinstruction.EasyProjection".equals(activityName)) {
                list = getPackageProcess("com.huawei.filemanager.desktopinstruction", startActivity.userId);
            } else {
                list = getPackageProcess(startActivity.packageName, startActivity.userId);
            }
            if (list != null) {
                int size = list.size();
                int i = 0;
                while (i < size) {
                    ProcessRecord pr = (ProcessRecord) list.get(i);
                    if (pr == null || pr.mDisplayId == sourceDisplayId || !(pr.foregroundActivities || ("com.huawei.works".equals(startActivity.packageName) && isConnectFromWeLink(sourceDisplayId)))) {
                        i++;
                    } else {
                        this.mService.mStackSupervisor.showToast(sourceDisplayId);
                        if (sourceDisplayId == 0) {
                            return 1;
                        }
                        return 0;
                    }
                }
            }
            if (this.mService instanceof HwActivityManagerService) {
                ((HwActivityManagerService) this.mService).mPkgDisplayMaps.put(startActivity.packageName, Integer.valueOf(sourceDisplayId));
            }
        }
        return -1;
    }

    private boolean isConnectFromWeLink(int displayId) {
        Display display = ((DisplayManager) this.mService.mContext.getSystemService("display")).getDisplay(displayId);
        if (display != null && "com.huawei.works".equals(display.getOwnerPackageName())) {
            return true;
        }
        return false;
    }

    protected boolean killProcessOnDefaultDisplay(ActivityRecord startActivity) {
        if (HwPCUtils.enabled()) {
            boolean killPackageProcess = false;
            ArrayList<ProcessRecord> list = getPackageProcess(startActivity.packageName, startActivity.userId);
            int N = list.size();
            for (int i = 0; i < N; i++) {
                if (HwPCUtils.isValidExtDisplayId(((ProcessRecord) list.get(i)).mDisplayId)) {
                    killPackageProcess = true;
                    break;
                }
            }
            if (killPackageProcess) {
                this.mService.forceStopPackageLocked(startActivity.packageName, UserHandle.getAppId(startActivity.appInfo.uid), false, false, true, false, false, UserHandle.getUserId(startActivity.appInfo.uid), "relaunch due to in diff display");
                return true;
            }
        }
        return false;
    }

    protected boolean killProcessOnOtherDisplay(ActivityRecord startActivity, int sourceDisplayId) {
        ActivityRecord activityRecord = startActivity;
        int i = sourceDisplayId;
        if (HwPCUtils.isPcCastModeInServer()) {
            String activityName = activityRecord.realActivity != null ? activityRecord.realActivity.getClassName() : "";
            if (packageShouldNotHandle(activityRecord.packageName) && !"com.huawei.filemanager.desktopinstruction.EasyProjection".equals(activityName)) {
                return false;
            }
            int N;
            if ("com.huawei.filemanager.desktopinstruction.EasyProjection".equals(activityName)) {
                ArrayList<ProcessRecord> list = getPackageProcess("com.huawei.filemanager.desktopinstruction", activityRecord.userId);
                N = list.size();
                boolean isRemoved = false;
                for (int i2 = 0; i2 < N; i2++) {
                    if (((ProcessRecord) list.get(i2)).mDisplayId != i) {
                        this.mService.removeProcessLocked((ProcessRecord) list.get(i2), false, true, "killProcessOnOtherDisplay");
                        isRemoved = true;
                    }
                }
                if (isRemoved) {
                    Set<String> disabledClasses = new HashSet();
                    disabledClasses.add("com.huawei.filemanager.desktopinstruction.EasyProjection");
                    this.mService.mStackSupervisor.finishDisabledPackageActivitiesLocked("com.huawei.desktop.explorer", disabledClasses, true, false, UserHandle.getUserId(activityRecord.appInfo.uid));
                }
                return isRemoved;
            }
            String processName = "";
            N = -1;
            boolean killPackageProcess = false;
            ArrayList<ProcessRecord> list2 = getPackageProcess(activityRecord.packageName, activityRecord.userId);
            int N2 = list2.size();
            int i3 = 0;
            while (i3 < N2) {
                if (((ProcessRecord) list2.get(i3)).mDisplayId != i) {
                    if (HwPCUtils.enabledInPad() && "com.android.settings".equals(activityRecord.packageName) && "com.android.phone".equals(((ProcessRecord) list2.get(i3)).processName)) {
                        HwPCUtils.log(TAG, "settings in phone process");
                    } else if (TextUtils.isEmpty(activityRecord.packageName) || !activityRecord.packageName.contains("com.tencent.mm") || activityRecord.app == null || activityRecord.app.pid == ((ProcessRecord) list2.get(i3)).pid) {
                        killPackageProcess = true;
                        processName = ((ProcessRecord) list2.get(i3)).processName;
                        N = ((ProcessRecord) list2.get(i3)).mDisplayId;
                        break;
                    } else {
                        HwPCUtils.log(TAG, "pid is not same so dont kill the process when killing Process on other display");
                    }
                }
                i3++;
            }
            if (sPCPkgName.contains(activityRecord.packageName)) {
                killPackageProcess = false;
            }
            if (killPackageProcess) {
                this.mService.forceStopPackageLocked(activityRecord.packageName, UserHandle.getAppId(activityRecord.appInfo.uid), false, false, true, false, false, UserHandle.getUserId(activityRecord.appInfo.uid), "relaunch due to in diff display");
                HwPCDataReporter.getInstance().reportKillProcessEvent(activityRecord.packageName, processName, i, N);
                return true;
            }
        }
        return false;
    }

    private boolean packageShouldNotHandle(String pkgName) {
        boolean z = true;
        if (HwPCUtils.enabledInPad()) {
            if (!("com.huawei.desktop.explorer".equals(pkgName) || "com.huawei.desktop.systemui".equals(pkgName) || FingerViewController.PKGNAME_OF_KEYGUARD.equals(pkgName) || "com.android.incallui".equals(pkgName) || "com.huawei.android.wfdft".equals(pkgName))) {
                z = false;
            }
            return z;
        }
        if (!("com.huawei.desktop.explorer".equals(pkgName) || "com.huawei.desktop.systemui".equals(pkgName))) {
            z = false;
        }
        return z;
    }

    private ArrayList<ProcessRecord> getPackageProcess(String pkg, int userId) {
        ArrayList<ProcessRecord> procs = new ArrayList();
        int NP = this.mService.mProcessNames.getMap().size();
        for (int ip = 0; ip < NP; ip++) {
            SparseArray<ProcessRecord> apps = (SparseArray) this.mService.mProcessNames.getMap().valueAt(ip);
            int NA = apps.size();
            for (int ia = 0; ia < NA; ia++) {
                ProcessRecord proc = (ProcessRecord) apps.valueAt(ia);
                if (proc.userId == userId && proc != this.mService.mHomeProcess && (proc.pkgList.containsKey(pkg) || ("com.huawei.filemanager.desktopinstruction".equals(pkg) && proc.processName != null && proc.processName.equals(pkg)))) {
                    procs.add(proc);
                }
            }
        }
        return procs;
    }

    private void standardizeIntentUriForClone(Intent intent, int userId) {
        Intent intent2 = intent;
        int i = userId;
        ClipData clipData = intent.getClipData();
        String volume = null;
        String sdcard = Environment.getLegacyExternalStorageDirectory().getAbsolutePath();
        int i2 = 0;
        if (clipData != null) {
            int itemCount = clipData.getItemCount();
            String volume2 = null;
            int i3 = 0;
            while (i3 < itemCount) {
                Item item = clipData.getItemAt(i3);
                Uri uri = item.getUri();
                if (uri != null && "file".equals(uri.getScheme()) && uri.getPath().startsWith(sdcard)) {
                    if (volume2 == null) {
                        this.mService.mContext.getSystemService("storage");
                        StorageVolume[] volumes = StorageManager.getVolumeList(i, 512);
                        if (volumes == null || volumes.length == 0) {
                            break;
                        }
                        volume2 = volumes[i2].getPath();
                    }
                    clipData.setItemAt(i3, new Item(item.getText(), item.getHtmlText(), item.getIntent(), Uri.parse(uri.toString().replace(sdcard, volume2))));
                }
                i3++;
                i2 = 0;
            }
            volume = volume2;
        }
        Uri uri2 = intent.getData();
        if (uri2 != null && "file".equals(uri2.getScheme()) && uri2.getPath().startsWith(sdcard)) {
            if (volume == null) {
                this.mService.mContext.getSystemService("storage");
                StorageVolume[] volumes2 = StorageManager.getVolumeList(i, 512);
                if (volumes2 != null && volumes2.length > 0) {
                    volume = volumes2[0].getPath();
                }
            }
            if (volume != null) {
                intent2.setData(Uri.parse(uri2.toString().replace(sdcard, volume)));
            }
        }
        Uri stream = (Uri) intent2.getParcelableExtra("android.intent.extra.STREAM");
        if (stream != null && "file".equals(stream.getScheme()) && stream.getPath().startsWith(sdcard)) {
            if (volume == null) {
                this.mService.mContext.getSystemService("storage");
                StorageVolume[] volumes3 = StorageManager.getVolumeList(i, 512);
                if (volumes3 != null && volumes3.length > 0) {
                    volume = volumes3[0].getPath();
                }
            }
            if (volume != null) {
                intent2.putExtra("android.intent.extra.STREAM", Uri.parse(stream.toString().replace(sdcard, volume)));
            }
        }
    }

    protected boolean isInSkipCancelResultList(String clsName) {
        return sSkipCancelResultList.contains(clsName);
    }

    private boolean isStartBrowserApps(ActivityInfo aInfo, int userId) {
        if (aInfo == null || aInfo.applicationInfo == null) {
            return false;
        }
        Set<String> specPkg = new HashSet();
        specPkg.add("com.google.android.setupwizard");
        specPkg.add("com.huawei.hwstartupguide");
        specPkg.add("com.android.settings");
        PackageManager pm = this.mService.mContext.getPackageManager();
        long origId = Binder.clearCallingIdentity();
        try {
            List<ResolveInfo> list = pm.queryIntentActivitiesAsUser(BROWSE_PROBE, 786432, userId);
            int count = list.size();
            for (int i = 0; i < count; i++) {
                ResolveInfo info = (ResolveInfo) list.get(i);
                if (!(info.activityInfo == null || info.activityInfo.getComponentName() == null || info.activityInfo.packageName == null)) {
                    if (info.handleAllWebDataURI) {
                        boolean z = true;
                        String str;
                        StringBuilder stringBuilder;
                        if (specPkg.contains(aInfo.packageName) && info.activityInfo.getComponentName().equals(aInfo.getComponentName())) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("skip launch aInfo:");
                            stringBuilder.append(aInfo);
                            stringBuilder.append(" because browser info:");
                            stringBuilder.append(info);
                            Log.w(str, stringBuilder.toString());
                            Binder.restoreCallingIdentity(origId);
                            return z;
                        } else if (!specPkg.contains(aInfo.packageName) && info.activityInfo.packageName.equals(aInfo.packageName)) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("skip launch aInfo:");
                            stringBuilder.append(aInfo);
                            stringBuilder.append(" because browser app:");
                            stringBuilder.append(aInfo.packageName);
                            Log.w(str, stringBuilder.toString());
                            return z;
                        }
                    }
                }
            }
            Binder.restoreCallingIdentity(origId);
            return false;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void showErrorDialogToRemoveUser(final Context context, final int userId) {
        UiThread.getHandler().post(new Runnable() {
            public void run() {
                AlertDialog ErrorDialog = new Builder(context, 33947691).setPositiveButton(33686057, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        UserManager.get(context).removeUser(userId);
                    }
                }).setNegativeButton(17039360, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setTitle(context.getString(33686059)).setMessage(context.getString(33686058)).create();
                ErrorDialog.getWindow().setType(2003);
                LayoutParams attributes = ErrorDialog.getWindow().getAttributes();
                attributes.privateFlags |= 16;
                ErrorDialog.setCanceledOnTouchOutside(false);
                ErrorDialog.show();
                ErrorDialog.getButton(-1).setTextColor(-65536);
            }
        });
    }
}
