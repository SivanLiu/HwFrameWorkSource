package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.app.WaitResult;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hdm.HwDeviceManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.service.voice.IVoiceInteractionSession;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Jlog;
import android.util.Pools.SynchronizedPool;
import android.util.Slog;
import android.view.WindowManager.LayoutParams;
import android.vrsystem.IVRSystemServiceManager;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.HeavyWeightSwitcherActivity;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.HwServiceExFactory;
import com.android.server.HwServiceFactory;
import com.android.server.HwServiceFactory.IHwActivityStarter;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.os.HwBootCheck;
import com.android.server.pm.DumpState;
import com.android.server.pm.InstantAppResolver;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.wm.ConfigurationContainer;
import com.huawei.server.am.IHwActivityStarterEx;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import huawei.cust.HwCustUtils;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActivityStarter extends AbsActivityStarter {
    private static final String ACTION_HWCHOOSER = "com.huawei.intent.action.hwCHOOSER";
    static final int DEFAULT_DISPLAY_STARTED_APP = 0;
    private static final String EXTRA_ALWAYS_USE_OPTION = "alwaysUseOption";
    private static final String HWPCEXPLORER_PACKAGE_NAME = "com.huawei.desktop.explorer";
    private static final String INCALLUI_ACTIVITY_CLASS_NAME = "com.android.incallui/.InCallActivity";
    private static final int INVALID_LAUNCH_MODE = -1;
    static final int OTHER_DISPLAY_NOT_STARTED_APP = -1;
    static final int OTHER_DISPLAY_SPECIAL_APP = 2;
    static final int OTHER_DISPLAY_STARTED_APP = 1;
    protected static final String SUW_FRP_STATE = "hw_suw_frp_state";
    private static final String TAG = "ActivityManager";
    private static final String TAG_CONFIGURATION;
    private static final String TAG_FOCUS = "ActivityManager";
    private static final String TAG_RESULTS = "ActivityManager";
    private static final String TAG_USER_LEAVING = "ActivityManager";
    static Map<Integer, Boolean> mLauncherStartState = new HashMap();
    private boolean mAddingToTask;
    private boolean mAvoidMoveToFront;
    private int mCallingUid;
    private final ActivityStartController mController;
    private HwCustActivityStackSupervisor mCustAss = ((HwCustActivityStackSupervisor) HwCustUtils.createObj(HwCustActivityStackSupervisor.class, new Object[0]));
    private boolean mDoResume;
    private IHwActivityStarterEx mHwActivityStarterEx;
    private TaskRecord mInTask;
    private Intent mIntent;
    private boolean mIntentDelivered;
    private final ActivityStartInterceptor mInterceptor;
    private boolean mKeepCurTransition;
    private final ActivityRecord[] mLastStartActivityRecord = new ActivityRecord[1];
    private int mLastStartActivityResult;
    private long mLastStartActivityTimeMs;
    private String mLastStartReason;
    private int mLaunchFlags;
    private int mLaunchMode;
    private LaunchParams mLaunchParams = new LaunchParams();
    private boolean mLaunchTaskBehind;
    private boolean mMovedToFront;
    private ActivityInfo mNewTaskInfo;
    private Intent mNewTaskIntent;
    private boolean mNoAnimation;
    private ActivityRecord mNotTop;
    protected ActivityOptions mOptions;
    private int mPreferredDisplayId;
    private Request mRequest = new Request();
    private TaskRecord mReuseTask;
    final ActivityManagerService mService;
    private ActivityRecord mSourceRecord;
    private ActivityStack mSourceStack;
    private ActivityRecord mStartActivity;
    private int mStartFlags;
    final ActivityStackSupervisor mSupervisor;
    private ActivityStack mTargetStack;
    private IVoiceInteractor mVoiceInteractor;
    private IVoiceInteractionSession mVoiceSession;

    @VisibleForTesting
    interface Factory {
        ActivityStarter obtain();

        void recycle(ActivityStarter activityStarter);

        void setController(ActivityStartController activityStartController);
    }

    private static class Request {
        private static final int DEFAULT_CALLING_PID = 0;
        private static final int DEFAULT_CALLING_UID = -1;
        ActivityInfo activityInfo;
        SafeActivityOptions activityOptions;
        boolean allowPendingRemoteAnimationRegistryLookup;
        boolean avoidMoveToFront;
        IApplicationThread caller;
        String callingPackage;
        int callingPid = -1;
        int callingUid = 0;
        boolean componentSpecified;
        Intent ephemeralIntent;
        int filterCallingUid;
        Configuration globalConfig;
        boolean ignoreTargetSecurity;
        TaskRecord inTask;
        Intent intent;
        boolean mayWait;
        ActivityRecord[] outActivity;
        ProfilerInfo profilerInfo;
        int realCallingPid;
        int realCallingUid;
        String reason;
        int requestCode;
        ResolveInfo resolveInfo;
        String resolvedType;
        IBinder resultTo;
        String resultWho;
        int startFlags;
        int userId;
        IVoiceInteractor voiceInteractor;
        IVoiceInteractionSession voiceSession;
        WaitResult waitResult;

        Request() {
            reset();
        }

        void reset() {
            this.caller = null;
            this.intent = null;
            this.ephemeralIntent = null;
            this.resolvedType = null;
            this.activityInfo = null;
            this.resolveInfo = null;
            this.voiceSession = null;
            this.voiceInteractor = null;
            this.resultTo = null;
            this.resultWho = null;
            this.requestCode = 0;
            this.callingPid = 0;
            this.callingUid = -1;
            this.callingPackage = null;
            this.realCallingPid = 0;
            this.realCallingUid = 0;
            this.startFlags = 0;
            this.activityOptions = null;
            this.ignoreTargetSecurity = false;
            this.componentSpecified = false;
            this.outActivity = null;
            this.inTask = null;
            this.reason = null;
            this.profilerInfo = null;
            this.globalConfig = null;
            this.userId = 0;
            this.waitResult = null;
            this.mayWait = false;
            this.avoidMoveToFront = false;
            this.allowPendingRemoteAnimationRegistryLookup = true;
            this.filterCallingUid = -10000;
        }

        void set(Request request) {
            this.caller = request.caller;
            this.intent = request.intent;
            this.ephemeralIntent = request.ephemeralIntent;
            this.resolvedType = request.resolvedType;
            this.activityInfo = request.activityInfo;
            this.resolveInfo = request.resolveInfo;
            this.voiceSession = request.voiceSession;
            this.voiceInteractor = request.voiceInteractor;
            this.resultTo = request.resultTo;
            this.resultWho = request.resultWho;
            this.requestCode = request.requestCode;
            this.callingPid = request.callingPid;
            this.callingUid = request.callingUid;
            this.callingPackage = request.callingPackage;
            this.realCallingPid = request.realCallingPid;
            this.realCallingUid = request.realCallingUid;
            this.startFlags = request.startFlags;
            this.activityOptions = request.activityOptions;
            this.ignoreTargetSecurity = request.ignoreTargetSecurity;
            this.componentSpecified = request.componentSpecified;
            this.outActivity = request.outActivity;
            this.inTask = request.inTask;
            this.reason = request.reason;
            this.profilerInfo = request.profilerInfo;
            this.globalConfig = request.globalConfig;
            this.userId = request.userId;
            this.waitResult = request.waitResult;
            this.mayWait = request.mayWait;
            this.avoidMoveToFront = request.avoidMoveToFront;
            this.allowPendingRemoteAnimationRegistryLookup = request.allowPendingRemoteAnimationRegistryLookup;
            this.filterCallingUid = request.filterCallingUid;
        }
    }

    static class DefaultFactory implements Factory {
        private final int MAX_STARTER_COUNT = 3;
        private ActivityStartController mController;
        private ActivityStartInterceptor mInterceptor;
        private ActivityManagerService mService;
        private SynchronizedPool<ActivityStarter> mStarterPool = new SynchronizedPool(3);
        private ActivityStackSupervisor mSupervisor;

        DefaultFactory(ActivityManagerService service, ActivityStackSupervisor supervisor, ActivityStartInterceptor interceptor) {
            this.mService = service;
            this.mSupervisor = supervisor;
            this.mInterceptor = interceptor;
        }

        public void setController(ActivityStartController controller) {
            this.mController = controller;
        }

        public ActivityStarter obtain() {
            ActivityStarter starter = (ActivityStarter) this.mStarterPool.acquire();
            if (starter != null) {
                return starter;
            }
            IHwActivityStarter iActivitySt = HwServiceFactory.getHwActivityStarter();
            if (iActivitySt != null) {
                return iActivitySt.getInstance(this.mController, this.mService, this.mSupervisor, this.mInterceptor);
            }
            return new ActivityStarter(this.mController, this.mService, this.mSupervisor, this.mInterceptor);
        }

        public void recycle(ActivityStarter starter) {
            starter.reset(true);
            this.mStarterPool.release(starter);
        }
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ActivityManagerService.TAG);
        stringBuilder.append(ActivityManagerDebugConfig.POSTFIX_CONFIGURATION);
        TAG_CONFIGURATION = stringBuilder.toString();
    }

    ActivityStarter(ActivityStartController controller, ActivityManagerService service, ActivityStackSupervisor supervisor, ActivityStartInterceptor interceptor) {
        this.mController = controller;
        this.mService = service;
        this.mSupervisor = supervisor;
        this.mInterceptor = interceptor;
        reset(true);
        this.mHwActivityStarterEx = HwServiceExFactory.getHwActivityStarterEx(service);
    }

    void set(ActivityStarter starter) {
        this.mStartActivity = starter.mStartActivity;
        this.mIntent = starter.mIntent;
        this.mCallingUid = starter.mCallingUid;
        this.mOptions = starter.mOptions;
        this.mLaunchTaskBehind = starter.mLaunchTaskBehind;
        this.mLaunchFlags = starter.mLaunchFlags;
        this.mLaunchMode = starter.mLaunchMode;
        this.mLaunchParams.set(starter.mLaunchParams);
        this.mNotTop = starter.mNotTop;
        this.mDoResume = starter.mDoResume;
        this.mStartFlags = starter.mStartFlags;
        this.mSourceRecord = starter.mSourceRecord;
        this.mPreferredDisplayId = starter.mPreferredDisplayId;
        this.mInTask = starter.mInTask;
        this.mAddingToTask = starter.mAddingToTask;
        this.mReuseTask = starter.mReuseTask;
        this.mNewTaskInfo = starter.mNewTaskInfo;
        this.mNewTaskIntent = starter.mNewTaskIntent;
        this.mSourceStack = starter.mSourceStack;
        this.mTargetStack = starter.mTargetStack;
        this.mMovedToFront = starter.mMovedToFront;
        this.mNoAnimation = starter.mNoAnimation;
        this.mKeepCurTransition = starter.mKeepCurTransition;
        this.mAvoidMoveToFront = starter.mAvoidMoveToFront;
        this.mVoiceSession = starter.mVoiceSession;
        this.mVoiceInteractor = starter.mVoiceInteractor;
        this.mIntentDelivered = starter.mIntentDelivered;
        this.mRequest.set(starter.mRequest);
    }

    ActivityRecord getStartActivity() {
        return this.mStartActivity;
    }

    boolean relatedToPackage(String packageName) {
        if ((this.mLastStartActivityRecord[0] == null || !packageName.equals(this.mLastStartActivityRecord[0].packageName)) && (this.mStartActivity == null || !packageName.equals(this.mStartActivity.packageName))) {
            return false;
        }
        return true;
    }

    int execute() {
        try {
            IApplicationThread iApplicationThread;
            int i;
            int startActivityMayWait;
            if (this.mRequest.mayWait) {
                iApplicationThread = this.mRequest.caller;
                int i2 = this.mRequest.callingUid;
                String str = this.mRequest.callingPackage;
                Intent intent = this.mRequest.intent;
                String str2 = this.mRequest.resolvedType;
                IVoiceInteractionSession iVoiceInteractionSession = this.mRequest.voiceSession;
                IVoiceInteractor iVoiceInteractor = this.mRequest.voiceInteractor;
                IBinder iBinder = this.mRequest.resultTo;
                String str3 = this.mRequest.resultWho;
                int i3 = this.mRequest.requestCode;
                i = this.mRequest.startFlags;
                ProfilerInfo profilerInfo = this.mRequest.profilerInfo;
                WaitResult waitResult = this.mRequest.waitResult;
                Configuration configuration = this.mRequest.globalConfig;
                SafeActivityOptions safeActivityOptions = this.mRequest.activityOptions;
                boolean z = this.mRequest.ignoreTargetSecurity;
                int i4 = this.mRequest.userId;
                TaskRecord taskRecord = this.mRequest.inTask;
                String str4 = this.mRequest.reason;
                boolean z2 = this.mRequest.allowPendingRemoteAnimationRegistryLookup;
                startActivityMayWait = startActivityMayWait(iApplicationThread, i2, str, intent, str2, iVoiceInteractionSession, iVoiceInteractor, iBinder, str3, i3, i, profilerInfo, waitResult, configuration, safeActivityOptions, z, i4, taskRecord, str4, z2);
                return startActivityMayWait;
            }
            iApplicationThread = this.mRequest.caller;
            Intent intent2 = this.mRequest.intent;
            Intent intent3 = this.mRequest.ephemeralIntent;
            String str5 = this.mRequest.resolvedType;
            ActivityInfo activityInfo = this.mRequest.activityInfo;
            ResolveInfo resolveInfo = this.mRequest.resolveInfo;
            IVoiceInteractionSession iVoiceInteractionSession2 = this.mRequest.voiceSession;
            IVoiceInteractor iVoiceInteractor2 = this.mRequest.voiceInteractor;
            IBinder iBinder2 = this.mRequest.resultTo;
            String str6 = this.mRequest.resultWho;
            i = this.mRequest.requestCode;
            int i5 = this.mRequest.callingPid;
            int i6 = this.mRequest.callingUid;
            String str7 = this.mRequest.callingPackage;
            int i7 = this.mRequest.realCallingPid;
            int i8 = this.mRequest.realCallingUid;
            int i9 = this.mRequest.startFlags;
            SafeActivityOptions safeActivityOptions2 = this.mRequest.activityOptions;
            boolean z3 = this.mRequest.ignoreTargetSecurity;
            boolean z4 = this.mRequest.componentSpecified;
            ActivityRecord[] activityRecordArr = this.mRequest.outActivity;
            TaskRecord taskRecord2 = this.mRequest.inTask;
            String str8 = this.mRequest.reason;
            boolean z5 = this.mRequest.allowPendingRemoteAnimationRegistryLookup;
            startActivityMayWait = startActivity(iApplicationThread, intent2, intent3, str5, activityInfo, resolveInfo, iVoiceInteractionSession2, iVoiceInteractor2, iBinder2, str6, i, i5, i6, str7, i7, i8, i9, safeActivityOptions2, z3, z4, activityRecordArr, taskRecord2, str8, z5);
            onExecutionComplete();
            return startActivityMayWait;
        } finally {
            onExecutionComplete();
        }
    }

    int startResolvedActivity(ActivityRecord r, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask, ActivityRecord[] outActivity) {
        try {
            int startActivity = startActivity(r, sourceRecord, voiceSession, voiceInteractor, startFlags, doResume, options, inTask, outActivity);
            return startActivity;
        } finally {
            onExecutionComplete();
        }
    }

    int startActivity(IApplicationThread caller, Intent intent, Intent ephemeralIntent, String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, int realCallingPid, int realCallingUid, int startFlags, SafeActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified, ActivityRecord[] outActivity, TaskRecord inTask, String reason, boolean allowPendingRemoteAnimationRegistryLookup) {
        if (TextUtils.isEmpty(reason)) {
            throw new IllegalArgumentException("Need to specify a reason.");
        }
        this.mLastStartReason = reason;
        this.mLastStartActivityTimeMs = System.currentTimeMillis();
        this.mLastStartActivityRecord[0] = null;
        this.mLastStartActivityResult = startActivity(caller, intent, ephemeralIntent, resolvedType, aInfo, rInfo, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid, callingUid, callingPackage, realCallingPid, realCallingUid, startFlags, options, ignoreTargetSecurity, componentSpecified, this.mLastStartActivityRecord, inTask, allowPendingRemoteAnimationRegistryLookup);
        if (outActivity != null) {
            outActivity[0] = this.mLastStartActivityRecord[0];
        }
        return getExternalResult(this.mLastStartActivityResult);
    }

    static int getExternalResult(int result) {
        return result != 102 ? result : 0;
    }

    private void onExecutionComplete() {
        this.mController.onExecutionComplete(this);
    }

    /* JADX WARNING: Removed duplicated region for block: B:237:0x0677  */
    /* JADX WARNING: Removed duplicated region for block: B:236:0x0674  */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x06cf  */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x06b5  */
    /* JADX WARNING: Removed duplicated region for block: B:198:0x04de  */
    /* JADX WARNING: Removed duplicated region for block: B:197:0x04ba  */
    /* JADX WARNING: Removed duplicated region for block: B:205:0x0503  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x04e8 A:{SKIP} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int startActivity(IApplicationThread caller, Intent intent, Intent ephemeralIntent, String resolvedType, ActivityInfo aInfo, ResolveInfo rInfo, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, int realCallingPid, int realCallingUid, int startFlags, SafeActivityOptions options, boolean ignoreTargetSecurity, boolean componentSpecified, ActivityRecord[] outActivity, TaskRecord inTask, boolean allowPendingRemoteAnimationRegistryLookup) {
        Throwable th;
        long restoreCurId;
        IApplicationThread iApplicationThread = caller;
        Intent intent2 = intent;
        String resolvedType2 = resolvedType;
        ActivityInfo aInfo2 = aInfo;
        IBinder iBinder = resultTo;
        int i = realCallingUid;
        int i2 = startFlags;
        int err = 0;
        Bundle verificationBundle = options != null ? options.popAppVerificationBundle() : null;
        if (aInfo2 != null) {
            this.mService.mHwAMSEx.noteActivityStart(aInfo2.applicationInfo.packageName, aInfo2.processName, intent.getComponent() != null ? intent.getComponent().getClassName() : "NULL", 0, aInfo2.applicationInfo.uid, true);
        }
        if (aInfo2 != null && !this.mHwActivityStarterEx.isAbleToLaunchInVR(this.mService.mContext, aInfo2.packageName)) {
            return 0;
        }
        int callingPid2;
        Bundle verificationBundle2;
        int callingUid2;
        boolean z;
        int callingPid3;
        ProcessRecord callerApp;
        String str;
        StringBuilder stringBuilder;
        String strPkg;
        String str2;
        ActivityRecord sourceRecord;
        ActivityRecord resultRecord;
        ActivityRecord sourceRecord2;
        String resultWho2;
        int requestCode2;
        String callingPackage2;
        ActivityRecord resultRecord2;
        if (iApplicationThread != null) {
            ProcessRecord callerApp2 = this.mService.getRecordForAppLocked(iApplicationThread);
            ProcessRecord callerApp3;
            if (callerApp2 != null) {
                if (intent2.hasCategory("android.intent.category.HOME")) {
                    this.mService.checkIfScreenStatusRequestAndSendBroadcast();
                }
                callingPid2 = callerApp2.pid;
                verificationBundle2 = verificationBundle;
                verificationBundle = callerApp2.info.uid;
                long restoreCurId2 = Binder.clearCallingIdentity();
                int callingPid4;
                try {
                    callingUid2 = verificationBundle;
                    try {
                        int callingPid5 = callingPid2;
                        try {
                            callingPid4 = callingPid5;
                            callerApp3 = callerApp2;
                            z = false;
                            verificationBundle = i2;
                        } catch (Throwable th2) {
                            th = th2;
                            callerApp3 = callerApp2;
                            verificationBundle = i2;
                            restoreCurId = restoreCurId2;
                            callingPid4 = callingPid5;
                            Binder.restoreCallingIdentity(restoreCurId);
                            throw th;
                        }
                        try {
                            if (this.mService.mHwAMSEx.isAllowToStartActivity(this.mService.mContext, callerApp2.info.packageName, aInfo2, this.mService.isSleepingLocked(), ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getLastResumedActivity())) {
                                Binder.restoreCallingIdentity(restoreCurId2);
                                callingPid3 = callingPid4;
                                callerApp = callerApp3;
                                i2 = callingUid2;
                            } else {
                                Binder.restoreCallingIdentity(restoreCurId2);
                                return 0;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            restoreCurId = restoreCurId2;
                            Binder.restoreCallingIdentity(restoreCurId);
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        callingPid4 = callingPid2;
                        callerApp3 = callerApp2;
                        verificationBundle = i2;
                        restoreCurId = restoreCurId2;
                        Binder.restoreCallingIdentity(restoreCurId);
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    Bundle bundle = verificationBundle;
                    callingPid4 = callingPid2;
                    callerApp3 = callerApp2;
                    verificationBundle = i2;
                    restoreCurId = restoreCurId2;
                    Binder.restoreCallingIdentity(restoreCurId);
                    throw th;
                }
            }
            verificationBundle2 = verificationBundle;
            callerApp3 = callerApp2;
            verificationBundle = i2;
            z = false;
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to find app for caller ");
            stringBuilder.append(iApplicationThread);
            stringBuilder.append(" (pid=");
            int i3 = callingPid;
            stringBuilder.append(i3);
            stringBuilder.append(") when starting: ");
            stringBuilder.append(intent.toString());
            Slog.w(str, stringBuilder.toString());
            err = -94;
            i2 = callingUid;
            callingPid3 = i3;
            callerApp = callerApp3;
        } else {
            verificationBundle2 = verificationBundle;
            verificationBundle = i2;
            z = false;
            i2 = callingUid;
            callerApp = null;
            callingPid3 = callingPid;
        }
        int userId = (aInfo2 == null || aInfo2.applicationInfo == null) ? z : UserHandle.getUserId(aInfo2.applicationInfo.uid);
        int userId2 = userId;
        if (!(mLauncherStartState.containsKey(Integer.valueOf(userId2)) && ((Boolean) mLauncherStartState.get(Integer.valueOf(userId2))).booleanValue()) && this.mService.isStartLauncherActivity(intent2, userId2)) {
            Slog.w(ActivityManagerService.TAG, "check the USER_SETUP_COMPLETE is set 1 in first start launcher!");
            this.mService.forceValidateHomeButton(userId2);
            mLauncherStartState.put(Integer.valueOf(userId2), Boolean.valueOf(true));
            clearFrpRestricted(this.mService.mContext, userId2);
        }
        if (err == 0) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("START u");
            stringBuilder.append(userId2);
            stringBuilder.append(" {");
            stringBuilder.append(intent2.toShortString(true, true, true, z));
            stringBuilder.append("} from uid ");
            stringBuilder.append(i2);
            Slog.i(str, stringBuilder.toString());
            if (!this.mService.mActivityIdle) {
                if (!ActivityManagerService.IS_DEBUG_VERSION || intent.getComponent() == null) {
                    str = "start activity";
                } else {
                    str = new StringBuilder();
                    str.append("START u");
                    str.append(userId2);
                    str.append(" ");
                    str.append(intent.getComponent().toShortString());
                    str.append(" from uid ");
                    str.append(i2);
                    str = str.toString();
                }
                HwBootCheck.addBootInfo(str);
            }
            this.mSupervisor.recognitionMaliciousApp(iApplicationThread, intent2);
            ComponentName cmp = intent.getComponent();
            strPkg = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if (cmp != null) {
                strPkg = cmp.getPackageName();
            }
            if (intent.getCategories() != null && intent.getCategories().toString().contains("android.intent.category.LAUNCHER")) {
                this.mService.getRecordCust().appEnterRecord(strPkg);
            }
            if (ACTION_HWCHOOSER.equals(intent.getAction()) && intent2.getBooleanExtra(EXTRA_ALWAYS_USE_OPTION, z)) {
                str2 = callingPackage;
                if (!HWPCEXPLORER_PACKAGE_NAME.equals(str2)) {
                    intent2.putExtra(EXTRA_ALWAYS_USE_OPTION, z);
                }
            } else {
                str2 = callingPackage;
            }
            this.mHwActivityStarterEx.effectiveIawareToLaunchApp(intent2, aInfo2, this.mService.getActivityStartController().mCurActivityPkName);
        } else {
            str2 = callingPackage;
        }
        if (iBinder != null) {
            sourceRecord = this.mSupervisor.isInAnyStackLocked(iBinder);
            if (ActivityManagerDebugConfig.DEBUG_RESULTS) {
                String str3 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                resultRecord = null;
                stringBuilder2.append("Will send result to ");
                stringBuilder2.append(iBinder);
                stringBuilder2.append(" ");
                stringBuilder2.append(sourceRecord);
                Slog.v(str3, stringBuilder2.toString());
            } else {
                resultRecord = null;
            }
            if (sourceRecord == null || requestCode < 0 || sourceRecord.finishing) {
                sourceRecord2 = sourceRecord;
            } else {
                sourceRecord2 = sourceRecord;
                resultRecord = sourceRecord;
            }
        } else {
            resultRecord = null;
            sourceRecord2 = null;
        }
        int launchFlags = intent.getFlags();
        if ((launchFlags & DumpState.DUMP_HANDLE) == 0 || sourceRecord2 == null) {
            resultWho2 = resultWho;
            requestCode2 = requestCode;
            callingPackage2 = str2;
            resultRecord2 = resultRecord;
        } else if (requestCode >= 0) {
            SafeActivityOptions.abort(options);
            return -93;
        } else {
            sourceRecord = sourceRecord2.resultTo;
            if (!(sourceRecord == null || sourceRecord.isInStackLocked())) {
                sourceRecord = null;
            }
            strPkg = sourceRecord2.resultWho;
            int requestCode3 = sourceRecord2.requestCode;
            sourceRecord2.resultTo = null;
            if (sourceRecord != null) {
                sourceRecord.removeResultsLocked(sourceRecord2, strPkg, requestCode3);
            }
            if (sourceRecord2.launchedFromUid == i2) {
                resultRecord2 = sourceRecord;
                requestCode2 = requestCode3;
                resultWho2 = strPkg;
                callingPackage2 = sourceRecord2.launchedFromPackage;
            } else {
                requestCode2 = requestCode3;
                resultWho2 = strPkg;
                callingPackage2 = str2;
                resultRecord2 = sourceRecord;
            }
        }
        if (err == 0 && intent.getComponent() == null) {
            err = -91;
        }
        if (err == 0 && aInfo2 == null) {
            err = -92;
        }
        if (!(err != 0 || sourceRecord2 == null || sourceRecord2.getTask().voiceSession == null || (launchFlags & 268435456) != 0 || sourceRecord2.info.applicationInfo.uid == aInfo2.applicationInfo.uid)) {
            try {
                intent2.addCategory("android.intent.category.VOICE");
                if (!this.mService.getPackageManager().activitySupportsIntent(intent.getComponent(), intent2, resolvedType2)) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Activity being started in current voice task does not support voice: ");
                    stringBuilder.append(intent2);
                    Slog.w(str, stringBuilder.toString());
                    err = -97;
                }
            } catch (RemoteException e) {
                Slog.w(ActivityManagerService.TAG, "Failure checking voice capabilities", e);
                err = -97;
            }
        }
        if (err == 0 && voiceSession != null) {
            try {
                if (!this.mService.getPackageManager().activitySupportsIntent(intent.getComponent(), intent2, resolvedType2)) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Activity being started in new voice task does not support: ");
                    stringBuilder.append(intent2);
                    Slog.w(str, stringBuilder.toString());
                    err = -97;
                }
            } catch (RemoteException e2) {
                Slog.w(ActivityManagerService.TAG, "Failure checking voice capabilities", e2);
                err = -97;
            }
        }
        callingUid2 = err;
        ActivityStack resultStack = resultRecord2 == null ? null : resultRecord2.getStack();
        if (callingUid2 != 0) {
            if (resultRecord2 != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord2, resultWho2, requestCode2, 0, null);
            }
            SafeActivityOptions.abort(options);
            return callingUid2;
        }
        ActivityOptions checkedOptions;
        ActivityOptions activityOptions;
        ProcessRecord callerApp4;
        int i4;
        int i5;
        int callingUid3;
        resultRecord = resultRecord2;
        int userId3 = userId2;
        ProcessRecord callerApp5 = callerApp;
        int callingUid4 = i2;
        boolean abort = (this.mSupervisor.checkStartAnyActivityPermission(intent2, aInfo2, resultWho2, requestCode2, callingPid3, i2, callingPackage2, ignoreTargetSecurity, inTask != null, callerApp, resultRecord2, resultStack) ^ 1) | (this.mService.mIntentFirewall.checkStartActivity(intent2, i2, callingPid3, resolvedType2, aInfo2.applicationInfo) ^ 1);
        SafeActivityOptions safeActivityOptions = options;
        if (safeActivityOptions != null) {
            callerApp = callerApp5;
            checkedOptions = safeActivityOptions.getOptions(intent2, aInfo2, callerApp, this.mSupervisor);
        } else {
            callerApp = callerApp5;
            checkedOptions = null;
        }
        if (abort) {
            activityOptions = checkedOptions;
            callerApp4 = callerApp;
        } else {
            activityOptions = checkedOptions;
            callerApp4 = callerApp;
            if (this.mService.shouldPreventStartActivity(aInfo2, callingUid4, callingPid3, callingPackage2, userId3)) {
                abort = true;
            }
        }
        boolean abort2 = abort;
        if (allowPendingRemoteAnimationRegistryLookup) {
            activityOptions = this.mService.getActivityStartController().getPendingRemoteAnimationRegistry().overrideOptionsIfNeeded(callingPackage2, activityOptions);
        }
        if (this.mService.mController != null) {
            try {
                abort2 |= this.mService.mController.activityStarting(intent.cloneFilter(), aInfo2.applicationInfo.packageName) ^ 1;
            } catch (RemoteException e3) {
                this.mService.mController = null;
            }
        }
        abort = startingCustomActivity(abort2, intent2, aInfo2) | abort2;
        if ("startActivityAsCaller".equals(this.mLastStartReason)) {
            i2 = callingUid4;
            intent2.setCallingUid(i2);
        } else {
            i2 = callingUid4;
            if (!"PendingIntentRecord".equals(this.mLastStartReason) || intent.getCallingUid() == 0) {
                ResolveInfo rInfo2;
                TaskRecord inTask2;
                userId2 = 0;
                i4 = realCallingUid;
                intent2.setCallingUid(i4);
                i5 = userId2;
                callingUid3 = i2;
                this.mInterceptor.setStates(userId3, realCallingPid, i4, verificationBundle, callingPackage2);
                this.mInterceptor.setSourceRecord(sourceRecord2);
                if (this.mInterceptor.intercept(intent2, rInfo, aInfo2, resolvedType2, inTask, callingPid3, callingUid3, activityOptions)) {
                    rInfo2 = rInfo;
                    inTask2 = inTask;
                    i2 = callingUid3;
                    userId = callingPid3;
                } else {
                    intent2 = this.mInterceptor.mIntent;
                    rInfo2 = this.mInterceptor.mRInfo;
                    aInfo2 = this.mInterceptor.mAInfo;
                    resolvedType2 = this.mInterceptor.mResolvedType;
                    TaskRecord inTask3 = this.mInterceptor.mInTask;
                    userId = this.mInterceptor.mCallingPid;
                    i2 = this.mInterceptor.mCallingUid;
                    inTask2 = inTask3;
                    activityOptions = this.mInterceptor.mActivityOptions;
                }
                if (!abort) {
                    if (!(resultRecord == null || resultStack == null)) {
                        resultStack.sendActivityResultLocked(-1, resultRecord, resultWho2, requestCode2, 0, null);
                    }
                    ActivityOptions.abort(activityOptions);
                    return 102;
                } else if (this.mHwActivityStarterEx.isAbleToLaunchVideoActivity(this.mService.mContext, intent2)) {
                    String callingPackage3;
                    ResolveInfo rInfo3;
                    int callingPid6;
                    String resolvedType3;
                    ActivityInfo aInfo3;
                    ResolveInfo rInfo4;
                    Intent intent3;
                    String callingPackage4;
                    Intent intent4;
                    int callingUid5;
                    ActivityInfo aInfo4;
                    ActivityRecord r;
                    if (!this.mService.mPermissionReviewRequired || aInfo2 == null) {
                        callingPackage3 = callingPackage2;
                        rInfo3 = rInfo2;
                        callingPid3 = userId;
                        i4 = userId3;
                        userId = realCallingUid;
                    } else {
                        i4 = userId3;
                        if (this.mService.getPackageManagerInternalLocked().isPermissionsReviewRequired(aInfo2.packageName, i4)) {
                            abort = this.mService.getIntentSenderLocked(2, callingPackage2, i2, i4, null, null, 0, new Intent[]{intent2}, new String[]{resolvedType2}, 1342177280, null);
                            rInfo2 = intent2.getFlags();
                            Intent newIntent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
                            newIntent.setFlags(DumpState.DUMP_VOLUMES | rInfo2);
                            newIntent.putExtra("android.intent.extra.PACKAGE_NAME", aInfo2.packageName);
                            newIntent.putExtra("android.intent.extra.INTENT", new IntentSender(abort));
                            if (resultRecord != null) {
                                newIntent.putExtra("android.intent.extra.RESULT_NEEDED", true);
                            }
                            intent2 = newIntent;
                            i2 = realCallingUid;
                            callingPid6 = realCallingPid;
                            userId = realCallingUid;
                            ResolveInfo rInfo5 = this.mSupervisor.resolveIntent(intent2, null, i4, 0, computeResolveFilterUid(i2, userId, this.mRequest.filterCallingUid));
                            resolvedType3 = null;
                            aInfo2 = this.mSupervisor.resolveActivity(intent2, rInfo5, verificationBundle, null);
                            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                                resolvedType2 = ActivityManagerService.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                IIntentSender target = abort;
                                stringBuilder3.append("START u");
                                stringBuilder3.append(i4);
                                stringBuilder3.append(" {");
                                aInfo3 = aInfo2;
                                callingPackage3 = callingPackage2;
                                stringBuilder3.append(intent2.toShortString(true, true, true, null));
                                stringBuilder3.append("} from uid ");
                                stringBuilder3.append(i2);
                                stringBuilder3.append(" on display ");
                                stringBuilder3.append(!this.mSupervisor.mFocusedStack ? false : this.mSupervisor.mFocusedStack.mDisplayId);
                                Slog.i(resolvedType2, stringBuilder3.toString());
                            } else {
                                aInfo3 = aInfo2;
                                callingPackage3 = callingPackage2;
                            }
                            rInfo4 = rInfo5;
                            intent3 = intent2;
                            if (rInfo4 != null || rInfo4.auxiliaryInfo == null) {
                                i5 = verificationBundle;
                                verificationBundle = verificationBundle2;
                                callingPackage4 = callingPackage3;
                                intent4 = intent3;
                                callingUid5 = i2;
                                aInfo4 = aInfo3;
                            } else {
                                callingPackage4 = callingPackage3;
                                i5 = verificationBundle;
                                intent2 = createLaunchIntent(rInfo4.auxiliaryInfo, ephemeralIntent, callingPackage4, verificationBundle2, resolvedType3, i4);
                                callingUid5 = userId;
                                resolvedType3 = null;
                                callingPid6 = realCallingPid;
                                intent4 = intent2;
                                aInfo4 = this.mSupervisor.resolveActivity(intent2, rInfo4, i5, null);
                            }
                            callingPackage2 = ActivityManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("ActivityRecord info: ");
                            stringBuilder.append(aInfo4);
                            Slog.i(callingPackage2, stringBuilder.toString());
                            r = HwServiceFactory.createActivityRecord(this.mService, callerApp4, callingPid6, callingUid5, callingPackage4, intent4, resolvedType3, aInfo4, this.mService.getGlobalConfiguration(), resultRecord, resultWho2, requestCode2, componentSpecified, voiceSession == null, this.mSupervisor, activityOptions, sourceRecord2);
                            StringBuilder stringBuilder4;
                            if (this.mService.shouldPreventActivity(intent4, aInfo4, r, callingPid6, callingUid5, callerApp4)) {
                                if (outActivity != null) {
                                    outActivity[0] = r;
                                }
                                if (r.appTimeTracker == null && sourceRecord2 != null) {
                                    r.appTimeTracker = sourceRecord2.appTimeTracker;
                                }
                                ActivityStack stack = this.mSupervisor.mFocusedStack;
                                if (voiceSession == null && stack.mResumedActivity == null && this.mSupervisor.getResumedActivityLocked() != null && r.shortComponentName != null && !r.shortComponentName.equals(this.mSupervisor.getResumedActivityLocked().shortComponentName) && INCALLUI_ACTIVITY_CLASS_NAME.equals(this.mSupervisor.getResumedActivityLocked().shortComponentName) && this.mService.isStartLauncherActivity(intent4, i4) && ((stack.mLastPausedActivity == null || !INCALLUI_ACTIVITY_CLASS_NAME.equals(stack.mLastPausedActivity.shortComponentName)) && !this.mService.isSleepingLocked())) {
                                    strPkg = ActivityManagerService.TAG;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("abort launch for activity: ");
                                    stringBuilder4.append(r);
                                    Slog.d(strPkg, stringBuilder4.toString());
                                    SafeActivityOptions.abort(options);
                                    return 100;
                                } else if (this.mService.isSleepingLocked() && r.shortComponentName != null && "com.tencent.news/.push.alive.offactivity.OffActivity".equals(r.shortComponentName)) {
                                    strPkg = ActivityManagerService.TAG;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("abort launch for activity: ");
                                    stringBuilder4.append(r);
                                    Slog.i(strPkg, stringBuilder4.toString());
                                    SafeActivityOptions.abort(options);
                                    return 100;
                                } else {
                                    ActivityInfo activityInfo;
                                    int i6;
                                    ActivityStack activityStack;
                                    if (voiceSession != null) {
                                        activityInfo = aInfo4;
                                        i6 = i4;
                                        activityStack = stack;
                                    } else if (stack.getResumedActivity() == null || stack.getResumedActivity().info.applicationInfo.uid != userId) {
                                        activityStack = stack;
                                        if (this.mService.checkAppSwitchAllowedLocked(callingPid6, callingUid5, realCallingPid, realCallingUid, "Activity start")) {
                                            activityInfo = aInfo4;
                                        } else {
                                            PendingActivityLaunch pendingActivityLaunch = r6;
                                            ActivityStartController activityStartController = this.mController;
                                            PendingActivityLaunch pendingActivityLaunch2 = new PendingActivityLaunch(r, sourceRecord2, i5, activityStack, callerApp4);
                                            activityStartController.addPendingActivityLaunch(pendingActivityLaunch);
                                            ActivityOptions.abort(activityOptions);
                                            return 100;
                                        }
                                    } else {
                                        ResolveInfo resolveInfo = rInfo4;
                                        activityInfo = aInfo4;
                                        i6 = i4;
                                        activityStack = stack;
                                    }
                                    if (this.mService.mDidAppSwitch) {
                                        this.mService.mAppSwitchesAllowedTime = 0;
                                    } else {
                                        this.mService.mDidAppSwitch = true;
                                    }
                                    this.mController.doPendingActivityLaunches(false);
                                    ActivityOptions checkedOptions2 = activityOptions;
                                    callingPid2 = startActivity(r, sourceRecord2, voiceSession, voiceInteractor, i5, true, activityOptions, inTask2, outActivity);
                                    if (Jlog.isUBMEnable() && callingPid2 >= 0 && intent4.getComponent() != null) {
                                        StringBuilder stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("AL#");
                                        stringBuilder5.append(intent4.getComponent().flattenToShortString());
                                        stringBuilder5.append("(");
                                        stringBuilder5.append(intent4.getAction());
                                        stringBuilder5.append(",");
                                        stringBuilder5.append(intent4.getCategories());
                                        stringBuilder5.append(")");
                                        Jlog.d(272, stringBuilder5.toString());
                                    }
                                    return callingPid2;
                                }
                            }
                            strPkg = ActivityManagerService.TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("forbiden launch for activity: ");
                            stringBuilder4.append(r);
                            Slog.w(strPkg, stringBuilder4.toString());
                            SafeActivityOptions.abort(options);
                            return 100;
                        }
                        callingPackage3 = callingPackage2;
                        rInfo3 = rInfo2;
                        callingPid3 = userId;
                        userId = realCallingUid;
                    }
                    intent3 = intent2;
                    resolvedType3 = resolvedType2;
                    aInfo3 = aInfo2;
                    callingPid6 = callingPid3;
                    rInfo4 = rInfo3;
                    if (rInfo4 != null) {
                    }
                    i5 = verificationBundle;
                    verificationBundle = verificationBundle2;
                    callingPackage4 = callingPackage3;
                    intent4 = intent3;
                    callingUid5 = i2;
                    aInfo4 = aInfo3;
                    callingPackage2 = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ActivityRecord info: ");
                    stringBuilder.append(aInfo4);
                    Slog.i(callingPackage2, stringBuilder.toString());
                    if (voiceSession == null) {
                    }
                    r = HwServiceFactory.createActivityRecord(this.mService, callerApp4, callingPid6, callingUid5, callingPackage4, intent4, resolvedType3, aInfo4, this.mService.getGlobalConfiguration(), resultRecord, resultWho2, requestCode2, componentSpecified, voiceSession == null, this.mSupervisor, activityOptions, sourceRecord2);
                    if (this.mService.shouldPreventActivity(intent4, aInfo4, r, callingPid6, callingUid5, callerApp4)) {
                    }
                } else {
                    SafeActivityOptions.abort(options);
                    return i5;
                }
            }
        }
        userId2 = 0;
        i4 = realCallingUid;
        i5 = userId2;
        callingUid3 = i2;
        this.mInterceptor.setStates(userId3, realCallingPid, i4, verificationBundle, callingPackage2);
        this.mInterceptor.setSourceRecord(sourceRecord2);
        if (this.mInterceptor.intercept(intent2, rInfo, aInfo2, resolvedType2, inTask, callingPid3, callingUid3, activityOptions)) {
        }
        if (!abort) {
        }
    }

    private Intent createLaunchIntent(AuxiliaryResolveInfo auxiliaryResponse, Intent originalIntent, String callingPackage, Bundle verificationBundle, String resolvedType, int userId) {
        Intent intent;
        ComponentName componentName;
        AuxiliaryResolveInfo auxiliaryResolveInfo = auxiliaryResponse;
        if (auxiliaryResolveInfo == null || !auxiliaryResolveInfo.needsPhaseTwo) {
        } else {
            this.mService.getPackageManagerInternalLocked().requestInstantAppResolutionPhaseTwo(auxiliaryResolveInfo, originalIntent, resolvedType, callingPackage, verificationBundle, userId);
        }
        Intent sanitizeIntent = InstantAppResolver.sanitizeIntent(originalIntent);
        List list = null;
        if (auxiliaryResolveInfo == null) {
            intent = null;
        } else {
            intent = auxiliaryResolveInfo.failureIntent;
        }
        if (auxiliaryResolveInfo == null) {
            componentName = null;
        } else {
            componentName = auxiliaryResolveInfo.installFailureActivity;
        }
        String str = auxiliaryResolveInfo == null ? null : auxiliaryResolveInfo.token;
        boolean z = auxiliaryResolveInfo != null && auxiliaryResolveInfo.needsPhaseTwo;
        boolean z2 = z;
        if (auxiliaryResolveInfo != null) {
            list = auxiliaryResolveInfo.filters;
        }
        return InstantAppResolver.buildEphemeralInstallerIntent(originalIntent, sanitizeIntent, intent, callingPackage, verificationBundle, resolvedType, userId, componentName, str, z2, list);
    }

    void postStartActivityProcessing(ActivityRecord r, int result, ActivityStack targetStack) {
        if (!ActivityManager.isStartResultFatalError(result)) {
            this.mSupervisor.reportWaitingActivityLaunchedIfNeeded(r, result);
            ActivityStack startedActivityStack = null;
            ActivityStack currentStack = r.getStack();
            if (currentStack != null) {
                startedActivityStack = currentStack;
            } else if (this.mTargetStack != null) {
                startedActivityStack = targetStack;
            }
            if (startedActivityStack != null) {
                boolean clearedTask = (this.mLaunchFlags & 268468224) == 268468224 && this.mReuseTask != null;
                if (result == 2 || result == 3 || clearedTask) {
                    switch (startedActivityStack.getWindowingMode()) {
                        case 2:
                            this.mService.mTaskChangeNotificationController.notifyPinnedActivityRestartAttempt(clearedTask);
                            break;
                        case 3:
                            ActivityStack homeStack = this.mSupervisor.mHomeStack;
                            if (homeStack != null && homeStack.shouldBeVisible(null)) {
                                this.mService.mWindowManager.showRecentApps();
                                break;
                            }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:230:0x053e  */
    /* JADX WARNING: Removed duplicated region for block: B:221:0x0514 A:{SYNTHETIC, Splitter: B:221:0x0514} */
    /* JADX WARNING: Removed duplicated region for block: B:221:0x0514 A:{SYNTHETIC, Splitter: B:221:0x0514} */
    /* JADX WARNING: Removed duplicated region for block: B:230:0x053e  */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x02ca  */
    /* JADX WARNING: Removed duplicated region for block: B:116:0x028b A:{SYNTHETIC, Splitter: B:116:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x02d4 A:{SYNTHETIC, Splitter: B:130:0x02d4} */
    /* JADX WARNING: Removed duplicated region for block: B:230:0x053e  */
    /* JADX WARNING: Removed duplicated region for block: B:221:0x0514 A:{SYNTHETIC, Splitter: B:221:0x0514} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0185  */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x01ec  */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x01e3  */
    /* JADX WARNING: Removed duplicated region for block: B:100:0x0252 A:{SYNTHETIC, Splitter: B:100:0x0252} */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x01e3  */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x01ec  */
    /* JADX WARNING: Removed duplicated region for block: B:100:0x0252 A:{SYNTHETIC, Splitter: B:100:0x0252} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int startActivityMayWait(IApplicationThread caller, int callingUid, String callingPackage, Intent intent, String resolvedType, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, WaitResult outResult, Configuration globalConfig, SafeActivityOptions options, boolean ignoreTargetSecurity, int userId, TaskRecord inTask, String reason, boolean allowPendingRemoteAnimationRegistryLookup) {
        int callingUid2;
        ActivityManagerService activityManagerService;
        Throwable heavy;
        IApplicationThread iApplicationThread;
        int i;
        ActivityManagerService activityManagerService2;
        Configuration aInfo;
        IApplicationThread intent2;
        int i2;
        ActivityInfo activityInfo;
        Intent intent3;
        long token;
        UserManager userManager;
        UserInfo userInfo;
        IApplicationThread iApplicationThread2 = caller;
        String str = callingPackage;
        Intent intent4 = intent;
        WaitResult waitResult = outResult;
        Configuration configuration = globalConfig;
        int i3 = userId;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.ACTIVITYSTARTER_STARTACTIVITYMAYWAIT, new Object[]{intent4, str});
        if (intent4 == null || !intent.hasFileDescriptors()) {
            int callingPid;
            int callingUid3;
            int callingUid4;
            ResolveInfo rInfo;
            ActivityInfo aInfo2;
            String activityName;
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2;
            ResolveInfo resolveInfo;
            this.mSupervisor.getActivityMetricsLogger().notifyActivityLaunching();
            boolean componentSpecified = intent.getComponent() != null;
            int realCallingPid = Binder.getCallingPid();
            int realCallingUid = Binder.getCallingUid();
            if (callingUid >= 0) {
                callingPid = -1;
                callingUid3 = callingUid;
            } else {
                if (iApplicationThread2 == null) {
                    callingPid = realCallingPid;
                    callingUid4 = realCallingUid;
                } else {
                    callingPid = -1;
                    callingUid4 = -1;
                }
                callingUid3 = callingUid4;
            }
            int callingPid2 = callingPid;
            Intent ephemeralIntent = new Intent(intent4);
            Intent intent5 = new Intent(intent4);
            if (SystemProperties.getBoolean("sys.super_power_save", false)) {
                Set<String> categories = intent5.getCategories();
                if (categories != null && categories.contains("android.intent.category.HOME")) {
                    intent5.removeCategory("android.intent.category.HOME");
                    intent5.addFlags(DumpState.DUMP_CHANGES);
                    intent5.setClassName("com.huawei.android.launcher", "com.huawei.android.launcher.powersavemode.PowerSaveModeLauncher");
                    if (this.mCustAss != null) {
                        this.mCustAss.modifyIntentForLauncher3(intent5);
                    }
                }
            }
            if (SystemProperties.getBoolean("sys.ride_mode", false)) {
                Set<String> categories2 = intent5.getCategories();
                if (categories2 != null && categories2.contains("android.intent.category.HOME")) {
                    intent5.removeCategory("android.intent.category.HOME");
                    intent5.addFlags(DumpState.DUMP_CHANGES);
                    intent5.setClassName("com.huawei.android.launcher", "com.huawei.android.launcher.streetmode.StreetModeLauncher");
                }
            }
            if (componentSpecified && !(("android.intent.action.VIEW".equals(intent5.getAction()) && intent5.getData() == null) || "android.intent.action.INSTALL_INSTANT_APP_PACKAGE".equals(intent5.getAction()) || "android.intent.action.RESOLVE_INSTANT_APP_PACKAGE".equals(intent5.getAction()) || !this.mService.getPackageManagerInternalLocked().isInstantAppInstallerComponent(intent5.getComponent()))) {
                intent5.setComponent(null);
                componentSpecified = false;
            }
            Intent intent6 = intent5;
            int callingUid5 = callingUid3;
            boolean componentSpecified2 = componentSpecified;
            int callingPid3 = callingPid2;
            Intent ephemeralIntent2 = ephemeralIntent;
            Intent intent7 = intent6;
            ResolveInfo rInfo2 = this.mSupervisor.resolveIntent(intent6, resolvedType, i3, 0, computeResolveFilterUid(callingUid3, realCallingUid, this.mRequest.filterCallingUid));
            if (standardizeHomeIntent(rInfo2, intent7)) {
                componentSpecified2 = false;
            }
            if (rInfo2 == null) {
                UserInfo userInfo2 = this.mSupervisor.getUserInfo(i3);
                if (userInfo2 != null) {
                    if (userInfo2.isManagedProfile() || userInfo2.isClonedProfile()) {
                        UserManager userManager2 = UserManager.get(this.mService.mContext);
                        long token2 = Binder.clearCallingIdentity();
                        try {
                            boolean z;
                            boolean profileLockedAndParentUnlockingOrUnlocked;
                            UserInfo parent = userManager2.getProfileParent(i3);
                            if (parent != null) {
                                try {
                                    if (userManager2.isUserUnlockingOrUnlocked(parent.id) && !userManager2.isUserUnlockingOrUnlocked(i3)) {
                                        z = true;
                                        profileLockedAndParentUnlockingOrUnlocked = z;
                                        Binder.restoreCallingIdentity(token2);
                                        if (profileLockedAndParentUnlockingOrUnlocked) {
                                            UserInfo userInfo3 = userInfo2;
                                            int callingUid6 = callingUid5;
                                            callingUid2 = callingUid6;
                                            rInfo = this.mSupervisor.resolveIntent(intent7, resolvedType, i3, 786432, computeResolveFilterUid(callingUid6, realCallingUid, this.mRequest.filterCallingUid));
                                            aInfo2 = this.mSupervisor.resolveActivity(intent7, rInfo, startFlags, profilerInfo);
                                            if (!(aInfo2 == null || aInfo2.applicationInfo == null || str == null || !str.equals(aInfo2.applicationInfo.packageName))) {
                                                activityName = intent7.getComponent() != null ? intent7.getComponent().getClassName() : "NULL";
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append(aInfo2.applicationInfo.packageName);
                                                stringBuilder.append(SliceAuthority.DELIMITER);
                                                stringBuilder.append(activityName);
                                                Jlog.d(335, stringBuilder.toString(), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                            }
                                            if (!(!Jlog.isPerfTest() || aInfo2 == null || aInfo2.applicationInfo == null)) {
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("whopkg=");
                                                stringBuilder2.append(str);
                                                stringBuilder2.append("&pkg=");
                                                stringBuilder2.append(aInfo2.applicationInfo.packageName);
                                                stringBuilder2.append("&cls=");
                                                stringBuilder2.append(aInfo2.name);
                                                Jlog.i(3023, Jlog.getMessage("ActivityStarter", "startActivityMayWait", stringBuilder2.toString()));
                                            }
                                            activityManagerService = this.mService;
                                            synchronized (activityManagerService) {
                                                ResolveInfo rInfo3;
                                                String str2;
                                                int i4;
                                                WaitResult waitResult2;
                                                ActivityInfo aInfo3;
                                                try {
                                                    long origId;
                                                    ActivityStack stack;
                                                    int callingUid7;
                                                    IApplicationThread caller2;
                                                    ActivityManagerService.boostPriorityForLockedSection();
                                                    ActivityStack stack2 = this.mSupervisor.mFocusedStack;
                                                    if (configuration != null) {
                                                        try {
                                                            if (this.mService.getGlobalConfiguration().diff(configuration) != 0) {
                                                                profileLockedAndParentUnlockingOrUnlocked = true;
                                                                stack2.mConfigWillChange = profileLockedAndParentUnlockingOrUnlocked;
                                                                if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                                                                    rInfo3 = rInfo;
                                                                } else {
                                                                    try {
                                                                        activityName = TAG_CONFIGURATION;
                                                                        stringBuilder = new StringBuilder();
                                                                        rInfo3 = rInfo;
                                                                        try {
                                                                            stringBuilder.append("Starting activity when config will change = ");
                                                                            stringBuilder.append(stack2.mConfigWillChange);
                                                                            Slog.v(activityName, stringBuilder.toString());
                                                                        } catch (Throwable th) {
                                                                            heavy = th;
                                                                            str2 = resolvedType;
                                                                            iApplicationThread = iApplicationThread2;
                                                                            i4 = callingPid3;
                                                                            i = realCallingUid;
                                                                        }
                                                                    } catch (Throwable th2) {
                                                                        heavy = th2;
                                                                        rInfo3 = rInfo;
                                                                        str2 = resolvedType;
                                                                        iApplicationThread = iApplicationThread2;
                                                                        i4 = callingPid3;
                                                                        i = realCallingUid;
                                                                        activityManagerService2 = activityManagerService;
                                                                        intent5 = intent7;
                                                                        callingUid3 = callingUid2;
                                                                        ephemeralIntent = ephemeralIntent2;
                                                                        waitResult2 = outResult;
                                                                        callingUid2 = aInfo2;
                                                                        aInfo = configuration;
                                                                        while (true) {
                                                                            try {
                                                                                break;
                                                                            } catch (Throwable th3) {
                                                                                heavy = th3;
                                                                            }
                                                                        }
                                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                                        throw heavy;
                                                                    }
                                                                }
                                                                origId = Binder.clearCallingIdentity();
                                                                if (aInfo2 != null) {
                                                                    try {
                                                                        if ((aInfo2.applicationInfo.privateFlags & 2) != 0 && this.mService.mHasHeavyWeightFeature && aInfo2.processName.equals(aInfo2.applicationInfo.packageName)) {
                                                                            ProcessRecord heavy2 = this.mService.mHeavyWeightProcess;
                                                                            if (heavy2 != null) {
                                                                                ActivityStack stack3;
                                                                                Intent intent8;
                                                                                if (heavy2.info.uid == aInfo2.applicationInfo.uid) {
                                                                                    if (heavy2.processName.equals(aInfo2.processName)) {
                                                                                        i4 = callingPid3;
                                                                                        stack = stack2;
                                                                                        activityManagerService2 = activityManagerService;
                                                                                        aInfo3 = aInfo2;
                                                                                        str2 = resolvedType;
                                                                                        ephemeralIntent = intent7;
                                                                                        callingUid7 = callingUid2;
                                                                                        caller2 = caller;
                                                                                        callingUid2 = aInfo3;
                                                                                        this.mService.addCallerToIntent(ephemeralIntent, caller2);
                                                                                        if (HwDeviceManager.disallowOp(ephemeralIntent)) {
                                                                                            ActivityRecord[] outRecord = new ActivityRecord[1];
                                                                                            IApplicationThread iApplicationThread3 = caller2;
                                                                                            iApplicationThread = caller2;
                                                                                            intent7 = i4;
                                                                                            Intent intent9 = ephemeralIntent;
                                                                                            try {
                                                                                                ActivityStarter activityStarter;
                                                                                                boolean z2;
                                                                                                int res = startActivity(iApplicationThread3, ephemeralIntent, ephemeralIntent2, str2, callingUid2, rInfo3, voiceSession, voiceInteractor, resultTo, resultWho, requestCode, intent7, callingUid7, callingPackage, realCallingPid, realCallingUid, startFlags, options, ignoreTargetSecurity, componentSpecified2, outRecord, inTask, reason, allowPendingRemoteAnimationRegistryLookup);
                                                                                                Binder.restoreCallingIdentity(origId);
                                                                                                stack2 = stack;
                                                                                                if (stack2.mConfigWillChange) {
                                                                                                    activityStarter = this;
                                                                                                    try {
                                                                                                        activityStarter.mService.enforceCallingPermission("android.permission.CHANGE_CONFIGURATION", "updateConfiguration()");
                                                                                                        z2 = false;
                                                                                                        stack2.mConfigWillChange = false;
                                                                                                        if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                                                                                                            try {
                                                                                                                Slog.v(TAG_CONFIGURATION, "Updating to new configuration after starting activity.");
                                                                                                            } catch (Throwable th4) {
                                                                                                                heavy = th4;
                                                                                                                callingUid3 = callingUid7;
                                                                                                                aInfo = globalConfig;
                                                                                                            }
                                                                                                        }
                                                                                                        try {
                                                                                                            activityStarter.mService.updateConfigurationLocked(globalConfig, null, false);
                                                                                                        } catch (Throwable th5) {
                                                                                                            heavy = th5;
                                                                                                        }
                                                                                                    } catch (Throwable th6) {
                                                                                                        heavy = th6;
                                                                                                        aInfo = globalConfig;
                                                                                                        intent5 = intent9;
                                                                                                        waitResult2 = outResult;
                                                                                                        while (true) {
                                                                                                            break;
                                                                                                        }
                                                                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                                        throw heavy;
                                                                                                    }
                                                                                                }
                                                                                                activityStarter = this;
                                                                                                z2 = false;
                                                                                                aInfo = globalConfig;
                                                                                                waitResult2 = outResult;
                                                                                                if (waitResult2 != null) {
                                                                                                    try {
                                                                                                        waitResult2.result = res;
                                                                                                        intent7 = intent9;
                                                                                                        waitResult2.origin = intent7.getComponent();
                                                                                                        ActivityRecord r = outRecord[z2];
                                                                                                        if (res != 0) {
                                                                                                            switch (res) {
                                                                                                                case 2:
                                                                                                                    if (!r.nowVisible || !r.isState(ActivityState.RESUMED)) {
                                                                                                                        waitResult2.thisTime = SystemClock.uptimeMillis();
                                                                                                                        activityStarter.mSupervisor.waitActivityVisible(r.realActivity, waitResult2);
                                                                                                                        do {
                                                                                                                            try {
                                                                                                                                activityStarter.mService.wait();
                                                                                                                            } catch (InterruptedException e) {
                                                                                                                            }
                                                                                                                            if (waitResult2.timeout) {
                                                                                                                                break;
                                                                                                                            }
                                                                                                                        } while (waitResult2.who == null);
                                                                                                                        break;
                                                                                                                    }
                                                                                                                    waitResult2.timeout = z2;
                                                                                                                    waitResult2.who = r.realActivity;
                                                                                                                    waitResult2.totalTime = 0;
                                                                                                                    waitResult2.thisTime = 0;
                                                                                                                    break;
                                                                                                                    break;
                                                                                                                case 3:
                                                                                                                    waitResult2.timeout = z2;
                                                                                                                    waitResult2.who = r.realActivity;
                                                                                                                    waitResult2.totalTime = 0;
                                                                                                                    waitResult2.thisTime = 0;
                                                                                                                    break;
                                                                                                            }
                                                                                                        }
                                                                                                        activityStarter.mSupervisor.mWaitingActivityLaunched.add(waitResult2);
                                                                                                        do {
                                                                                                            try {
                                                                                                                activityStarter.mService.wait();
                                                                                                            } catch (InterruptedException e2) {
                                                                                                            }
                                                                                                            if (waitResult2.result == 2 || waitResult2.timeout) {
                                                                                                            }
                                                                                                        } while (waitResult2.who == null);
                                                                                                        if (waitResult2.result == 2) {
                                                                                                            res = 2;
                                                                                                        }
                                                                                                    } catch (Throwable th7) {
                                                                                                        heavy = th7;
                                                                                                        intent5 = intent7;
                                                                                                        callingUid3 = callingUid7;
                                                                                                        while (true) {
                                                                                                            break;
                                                                                                        }
                                                                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                                        throw heavy;
                                                                                                    }
                                                                                                }
                                                                                                intent7 = intent9;
                                                                                                activityStarter.mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(res, outRecord[z2]);
                                                                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                                return res;
                                                                                            } catch (Throwable th8) {
                                                                                                heavy = th8;
                                                                                                caller2 = intent9;
                                                                                                aInfo = globalConfig;
                                                                                                waitResult2 = outResult;
                                                                                                intent2 = caller2;
                                                                                                while (true) {
                                                                                                    break;
                                                                                                }
                                                                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                                throw heavy;
                                                                                            }
                                                                                        }
                                                                                        try {
                                                                                            Slog.i(ActivityManagerService.TAG, "due to disallow op launching activity aborted");
                                                                                            UiThread.getHandler().post(new Runnable() {
                                                                                                public void run() {
                                                                                                    Context context = ActivityStarter.this.mService.mUiContext;
                                                                                                    if (context != null) {
                                                                                                        Toast toast = Toast.makeText(context, context.getString(33686099), 0);
                                                                                                        LayoutParams windowParams = toast.getWindowParams();
                                                                                                        windowParams.privateFlags |= 16;
                                                                                                        toast.show();
                                                                                                    }
                                                                                                }
                                                                                            });
                                                                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                            return -96;
                                                                                        } catch (Throwable th9) {
                                                                                            heavy = th9;
                                                                                            i = realCallingUid;
                                                                                            aInfo = configuration;
                                                                                            iApplicationThread = caller2;
                                                                                            callingUid3 = callingUid7;
                                                                                            ephemeralIntent = ephemeralIntent2;
                                                                                            waitResult2 = outResult;
                                                                                            while (true) {
                                                                                                break;
                                                                                            }
                                                                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                            throw heavy;
                                                                                        }
                                                                                    }
                                                                                }
                                                                                callingUid4 = callingUid2;
                                                                                if (iApplicationThread2 != null) {
                                                                                    ProcessRecord callerApp = this.mService.getRecordForAppLocked(iApplicationThread2);
                                                                                    if (callerApp != null) {
                                                                                        callingUid4 = callerApp.info.uid;
                                                                                        stack3 = stack2;
                                                                                    } else {
                                                                                        int i5 = callingUid4;
                                                                                        String str3 = ActivityManagerService.TAG;
                                                                                        StringBuilder stringBuilder3 = new StringBuilder();
                                                                                        stringBuilder3.append("Unable to find app for caller ");
                                                                                        stringBuilder3.append(iApplicationThread2);
                                                                                        stringBuilder3.append(" (pid=");
                                                                                        stringBuilder3.append(callingPid3);
                                                                                        stringBuilder3.append(") when starting: ");
                                                                                        stringBuilder3.append(intent7.toString());
                                                                                        Slog.w(str3, stringBuilder3.toString());
                                                                                        SafeActivityOptions.abort(options);
                                                                                    }
                                                                                } else {
                                                                                    stack3 = stack2;
                                                                                }
                                                                                i4 = callingPid3;
                                                                                IIntentSender target = this.mService.getIntentSenderLocked(2, PackageManagerService.PLATFORM_PACKAGE_NAME, callingUid4, i3, null, null, 0, new Intent[]{intent7}, new String[]{resolvedType}, 1342177280, null);
                                                                                callingPid3 = new Intent();
                                                                                if (requestCode >= 0) {
                                                                                    try {
                                                                                        callingPid3.putExtra("has_result", true);
                                                                                    } catch (Throwable th10) {
                                                                                        heavy = th10;
                                                                                        str2 = resolvedType;
                                                                                        i = realCallingUid;
                                                                                        activityManagerService2 = activityManagerService;
                                                                                        intent5 = intent7;
                                                                                        callingUid3 = callingUid2;
                                                                                        ephemeralIntent = ephemeralIntent2;
                                                                                        waitResult2 = outResult;
                                                                                        iApplicationThread = caller;
                                                                                    }
                                                                                }
                                                                                try {
                                                                                    callingPid3.putExtra(HwBroadcastRadarUtil.KEY_BROADCAST_INTENT, new IntentSender(target));
                                                                                    if (heavy2.activities.size() > 0) {
                                                                                        ActivityRecord hist = (ActivityRecord) heavy2.activities.get(0);
                                                                                        callingPid3.putExtra("cur_app", hist.packageName);
                                                                                        callingPid3.putExtra("cur_task", hist.getTask().taskId);
                                                                                    }
                                                                                    callingPid3.putExtra("new_app", aInfo2.packageName);
                                                                                    callingPid3.setFlags(intent7.getFlags());
                                                                                    callingPid3.setClassName(PackageManagerService.PLATFORM_PACKAGE_NAME, HeavyWeightSwitcherActivity.class.getName());
                                                                                    intent8 = callingPid3;
                                                                                } catch (Throwable th11) {
                                                                                    heavy = th11;
                                                                                    activityManagerService2 = activityManagerService;
                                                                                    aInfo3 = aInfo2;
                                                                                    str2 = resolvedType;
                                                                                    i = realCallingUid;
                                                                                    aInfo = configuration;
                                                                                    callingUid3 = callingUid2;
                                                                                    ephemeralIntent = ephemeralIntent2;
                                                                                    waitResult2 = outResult;
                                                                                    iApplicationThread = caller;
                                                                                    callingUid2 = aInfo3;
                                                                                    while (true) {
                                                                                        break;
                                                                                    }
                                                                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                    throw heavy;
                                                                                }
                                                                                try {
                                                                                    callingPid = Binder.getCallingUid();
                                                                                    try {
                                                                                        int callingPid4 = Binder.getCallingPid();
                                                                                        componentSpecified2 = true;
                                                                                        try {
                                                                                            i2 = callingPid;
                                                                                            stack = stack3;
                                                                                            activityManagerService2 = activityManagerService;
                                                                                            aInfo3 = aInfo2;
                                                                                            try {
                                                                                                ActivityInfo activityInfo2;
                                                                                                ResolveInfo rInfo4 = this.mSupervisor.resolveIntent(intent8, null, i3, 0, computeResolveFilterUid(callingPid, realCallingUid, this.mRequest.filterCallingUid));
                                                                                                if (rInfo4 != null) {
                                                                                                    try {
                                                                                                        activityInfo2 = rInfo4.activityInfo;
                                                                                                    } catch (Throwable th12) {
                                                                                                        heavy = th12;
                                                                                                        rInfo3 = rInfo4;
                                                                                                    }
                                                                                                } else {
                                                                                                    activityInfo2 = null;
                                                                                                }
                                                                                                aInfo2 = activityInfo2;
                                                                                                if (aInfo2 != null) {
                                                                                                    try {
                                                                                                        callingUid2 = this.mService.getActivityInfoForUser(aInfo2, i3);
                                                                                                        rInfo3 = rInfo4;
                                                                                                        ephemeralIntent = intent8;
                                                                                                    } catch (Throwable th13) {
                                                                                                        heavy = th13;
                                                                                                        rInfo3 = rInfo4;
                                                                                                        i = realCallingUid;
                                                                                                        activityInfo = aInfo2;
                                                                                                        aInfo = configuration;
                                                                                                        intent3 = null;
                                                                                                        iApplicationThread = null;
                                                                                                        i4 = callingPid4;
                                                                                                        callingUid3 = i2;
                                                                                                        ephemeralIntent = ephemeralIntent2;
                                                                                                        waitResult2 = outResult;
                                                                                                        while (true) {
                                                                                                            break;
                                                                                                        }
                                                                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                                        throw heavy;
                                                                                                    }
                                                                                                }
                                                                                                rInfo3 = rInfo4;
                                                                                                ephemeralIntent = intent8;
                                                                                                callingUid2 = aInfo2;
                                                                                                str2 = null;
                                                                                                caller2 = null;
                                                                                                i4 = callingPid4;
                                                                                                callingUid7 = i2;
                                                                                                this.mService.addCallerToIntent(ephemeralIntent, caller2);
                                                                                                if (HwDeviceManager.disallowOp(ephemeralIntent)) {
                                                                                                }
                                                                                            } catch (Throwable th14) {
                                                                                                heavy = th14;
                                                                                                callingPid = intent8;
                                                                                                aInfo = configuration;
                                                                                                str2 = null;
                                                                                                iApplicationThread = null;
                                                                                                activityInfo = aInfo3;
                                                                                                callingUid3 = i2;
                                                                                                ephemeralIntent = ephemeralIntent2;
                                                                                                waitResult2 = outResult;
                                                                                                while (true) {
                                                                                                    break;
                                                                                                }
                                                                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                                throw heavy;
                                                                                            }
                                                                                        } catch (Throwable th15) {
                                                                                            heavy = th15;
                                                                                            i2 = callingPid;
                                                                                            activityManagerService2 = activityManagerService;
                                                                                            aInfo3 = aInfo2;
                                                                                            i = realCallingUid;
                                                                                            callingPid = intent8;
                                                                                            aInfo = configuration;
                                                                                            intent3 = null;
                                                                                            iApplicationThread = null;
                                                                                            i4 = callingPid4;
                                                                                            callingUid2 = aInfo3;
                                                                                            callingUid3 = i2;
                                                                                            waitResult2 = outResult;
                                                                                            while (true) {
                                                                                                break;
                                                                                            }
                                                                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                            throw heavy;
                                                                                        }
                                                                                    } catch (Throwable th16) {
                                                                                        heavy = th16;
                                                                                        i2 = callingPid;
                                                                                        activityManagerService2 = activityManagerService;
                                                                                        aInfo3 = aInfo2;
                                                                                        i = realCallingUid;
                                                                                        callingPid = intent8;
                                                                                        aInfo = configuration;
                                                                                        intent3 = null;
                                                                                        iApplicationThread = null;
                                                                                        callingUid2 = aInfo3;
                                                                                        callingUid3 = i2;
                                                                                        waitResult2 = outResult;
                                                                                        while (true) {
                                                                                            break;
                                                                                        }
                                                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                        throw heavy;
                                                                                    }
                                                                                } catch (Throwable th17) {
                                                                                    heavy = th17;
                                                                                    activityManagerService2 = activityManagerService;
                                                                                    aInfo3 = aInfo2;
                                                                                    i = realCallingUid;
                                                                                    intent5 = intent8;
                                                                                    aInfo = configuration;
                                                                                    intent3 = null;
                                                                                    iApplicationThread = null;
                                                                                    callingUid3 = callingUid2;
                                                                                    ephemeralIntent = ephemeralIntent2;
                                                                                    waitResult2 = outResult;
                                                                                    callingUid2 = aInfo3;
                                                                                    while (true) {
                                                                                        break;
                                                                                    }
                                                                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                                                                    throw heavy;
                                                                                }
                                                                            }
                                                                        }
                                                                    } catch (Throwable th18) {
                                                                        heavy = th18;
                                                                        i4 = callingPid3;
                                                                        activityManagerService2 = activityManagerService;
                                                                        aInfo3 = aInfo2;
                                                                        str2 = resolvedType;
                                                                        i = realCallingUid;
                                                                        aInfo = configuration;
                                                                        intent5 = intent7;
                                                                        callingUid3 = callingUid2;
                                                                        ephemeralIntent = ephemeralIntent2;
                                                                        waitResult2 = outResult;
                                                                        iApplicationThread = caller;
                                                                        callingUid2 = aInfo3;
                                                                        while (true) {
                                                                            break;
                                                                        }
                                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                                        throw heavy;
                                                                    }
                                                                }
                                                                i4 = callingPid3;
                                                                stack = stack2;
                                                                activityManagerService2 = activityManagerService;
                                                                aInfo3 = aInfo2;
                                                                str2 = resolvedType;
                                                                ephemeralIntent = intent7;
                                                                callingUid7 = callingUid2;
                                                                caller2 = caller;
                                                                callingUid2 = aInfo3;
                                                                this.mService.addCallerToIntent(ephemeralIntent, caller2);
                                                                if (HwDeviceManager.disallowOp(ephemeralIntent)) {
                                                                }
                                                            }
                                                        } catch (Throwable th19) {
                                                            heavy = th19;
                                                            iApplicationThread = iApplicationThread2;
                                                            activityManagerService2 = activityManagerService;
                                                            intent5 = intent7;
                                                            ephemeralIntent = ephemeralIntent2;
                                                            waitResult2 = outResult;
                                                            callingUid2 = aInfo2;
                                                            aInfo = configuration;
                                                            while (true) {
                                                                break;
                                                            }
                                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                                            throw heavy;
                                                        }
                                                    }
                                                    profileLockedAndParentUnlockingOrUnlocked = false;
                                                    stack2.mConfigWillChange = profileLockedAndParentUnlockingOrUnlocked;
                                                    if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                                                    }
                                                    try {
                                                        origId = Binder.clearCallingIdentity();
                                                        if (aInfo2 != null) {
                                                        }
                                                        i4 = callingPid3;
                                                        stack = stack2;
                                                        activityManagerService2 = activityManagerService;
                                                        aInfo3 = aInfo2;
                                                        str2 = resolvedType;
                                                        ephemeralIntent = intent7;
                                                        callingUid7 = callingUid2;
                                                        caller2 = caller;
                                                        callingUid2 = aInfo3;
                                                    } catch (Throwable th20) {
                                                        heavy = th20;
                                                        i4 = callingPid3;
                                                        i = realCallingUid;
                                                        activityManagerService2 = activityManagerService;
                                                        aInfo3 = aInfo2;
                                                        aInfo = configuration;
                                                        ephemeralIntent = ephemeralIntent2;
                                                        waitResult2 = outResult;
                                                        str2 = resolvedType;
                                                        intent5 = intent7;
                                                        callingUid3 = callingUid2;
                                                        iApplicationThread = caller;
                                                        callingUid2 = aInfo3;
                                                        while (true) {
                                                            break;
                                                        }
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                        throw heavy;
                                                    }
                                                    try {
                                                        this.mService.addCallerToIntent(ephemeralIntent, caller2);
                                                        if (HwDeviceManager.disallowOp(ephemeralIntent)) {
                                                        }
                                                    } catch (Throwable th21) {
                                                        heavy = th21;
                                                        i = realCallingUid;
                                                        aInfo = configuration;
                                                        iApplicationThread = caller2;
                                                        waitResult2 = outResult;
                                                        intent7 = ephemeralIntent;
                                                        intent5 = intent7;
                                                        callingUid3 = callingUid7;
                                                        while (true) {
                                                            break;
                                                        }
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                        throw heavy;
                                                    }
                                                } catch (Throwable th22) {
                                                    heavy = th22;
                                                    i4 = callingPid3;
                                                    i = realCallingUid;
                                                    rInfo2 = rInfo;
                                                    activityManagerService2 = activityManagerService;
                                                    aInfo3 = aInfo2;
                                                    aInfo = configuration;
                                                    waitResult2 = outResult;
                                                    str2 = resolvedType;
                                                    rInfo3 = rInfo2;
                                                    intent5 = intent7;
                                                    callingUid3 = callingUid2;
                                                    iApplicationThread = caller;
                                                    activityInfo = aInfo3;
                                                    while (true) {
                                                        break;
                                                    }
                                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                                    throw heavy;
                                                }
                                            }
                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                            return -94;
                                        }
                                    }
                                } catch (Throwable th23) {
                                    heavy = th23;
                                    token = token2;
                                    userManager = userManager2;
                                    userInfo = userInfo2;
                                    resolveInfo = rInfo2;
                                    callingUid2 = callingUid5;
                                    Binder.restoreCallingIdentity(token);
                                    throw heavy;
                                }
                            }
                            z = false;
                            profileLockedAndParentUnlockingOrUnlocked = z;
                            Binder.restoreCallingIdentity(token2);
                            if (profileLockedAndParentUnlockingOrUnlocked) {
                            }
                        } catch (Throwable th24) {
                            heavy = th24;
                            token = token2;
                            userManager = userManager2;
                            userInfo = userInfo2;
                            resolveInfo = rInfo2;
                            callingUid2 = callingUid5;
                            Binder.restoreCallingIdentity(token);
                            throw heavy;
                        }
                    }
                    resolveInfo = rInfo2;
                    callingUid2 = callingUid5;
                    rInfo = resolveInfo;
                    aInfo2 = this.mSupervisor.resolveActivity(intent7, rInfo, startFlags, profilerInfo);
                    if (intent7.getComponent() != null) {
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(aInfo2.applicationInfo.packageName);
                    stringBuilder.append(SliceAuthority.DELIMITER);
                    stringBuilder.append(activityName);
                    Jlog.d(335, stringBuilder.toString(), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("whopkg=");
                    stringBuilder2.append(str);
                    stringBuilder2.append("&pkg=");
                    stringBuilder2.append(aInfo2.applicationInfo.packageName);
                    stringBuilder2.append("&cls=");
                    stringBuilder2.append(aInfo2.name);
                    Jlog.i(3023, Jlog.getMessage("ActivityStarter", "startActivityMayWait", stringBuilder2.toString()));
                    activityManagerService = this.mService;
                    synchronized (activityManagerService) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return -94;
                }
            }
            resolveInfo = rInfo2;
            callingUid2 = callingUid5;
            rInfo = resolveInfo;
            aInfo2 = this.mSupervisor.resolveActivity(intent7, rInfo, startFlags, profilerInfo);
            if (intent7.getComponent() != null) {
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(aInfo2.applicationInfo.packageName);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(activityName);
            Jlog.d(335, stringBuilder.toString(), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("whopkg=");
            stringBuilder2.append(str);
            stringBuilder2.append("&pkg=");
            stringBuilder2.append(aInfo2.applicationInfo.packageName);
            stringBuilder2.append("&cls=");
            stringBuilder2.append(aInfo2.name);
            Jlog.i(3023, Jlog.getMessage("ActivityStarter", "startActivityMayWait", stringBuilder2.toString()));
            activityManagerService = this.mService;
            synchronized (activityManagerService) {
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return -94;
        }
        throw new IllegalArgumentException("File descriptors passed in Intent");
    }

    static int computeResolveFilterUid(int customCallingUid, int actualCallingUid, int filterCallingUid) {
        if (filterCallingUid != -10000) {
            return filterCallingUid;
        }
        return customCallingUid >= 0 ? customCallingUid : actualCallingUid;
    }

    private int startActivity(ActivityRecord r, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask, ActivityRecord[] outActivity) {
        ActivityRecord activityRecord;
        int result = -96;
        try {
            this.mService.mWindowManager.deferSurfaceLayout();
            result = startActivityUnchecked(r, sourceRecord, voiceSession, voiceInteractor, startFlags, doResume, options, inTask, outActivity);
            try {
                ActivityStack stack = this.mStartActivity.getStack();
                if (!(ActivityManager.isStartResultSuccessful(result) || stack == null)) {
                    stack.finishActivityLocked(this.mStartActivity, 0, null, "startActivity", true);
                }
                this.mService.mWindowManager.continueSurfaceLayout();
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("startActivity result is ");
                    stringBuilder.append(result);
                    Slog.v(str, stringBuilder.toString());
                }
                postStartActivityProcessing(r, result, this.mTargetStack);
                return result;
            } catch (Throwable th) {
                activityRecord = r;
                this.mService.mWindowManager.continueSurfaceLayout();
            }
        } catch (Throwable th2) {
            this.mService.mWindowManager.continueSurfaceLayout();
        }
    }

    private int startActivityUnchecked(ActivityRecord r, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags, boolean doResume, ActivityOptions options, TaskRecord inTask, ActivityRecord[] outActivity) {
        int startedResult;
        ConfigurationContainer sourceStack;
        TaskRecord task;
        ActivityRecord[] activityRecordArr = outActivity;
        setInitialState(r, options, inTask, doResume, startFlags, sourceRecord, voiceSession, voiceInteractor);
        computeLaunchingTaskFlags();
        computeSourceStack();
        this.mIntent.setFlags(this.mLaunchFlags);
        ActivityRecord reusedActivity = getReusableIntentActivity();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ReusedActivity is ");
        stringBuilder.append(reusedActivity);
        Flog.i(101, stringBuilder.toString());
        int preferredWindowingMode = 0;
        int preferredLaunchDisplayId = 0;
        if (this.mOptions != null) {
            preferredWindowingMode = this.mOptions.getLaunchWindowingMode();
            preferredLaunchDisplayId = this.mOptions.getLaunchDisplayId();
        }
        if (!this.mLaunchParams.isEmpty()) {
            if (this.mLaunchParams.hasPreferredDisplay()) {
                preferredLaunchDisplayId = this.mLaunchParams.mPreferredDisplayId;
            }
            if (this.mLaunchParams.hasWindowingMode()) {
                preferredWindowingMode = this.mLaunchParams.mWindowingMode;
            }
        }
        ConfigurationContainer configurationContainer = null;
        if (HwPCUtils.isPcCastModeInServer()) {
            startedResult = hasStartedOnOtherDisplay(this.mStartActivity, this.mPreferredDisplayId);
            if (startedResult != -1) {
                ActivityOptions.abort(this.mOptions);
                if (this.mStartActivity.resultTo != null) {
                    configurationContainer = this.mStartActivity.resultTo.getStack();
                }
                sourceStack = configurationContainer;
                if (sourceStack != null) {
                    sourceStack.sendActivityResultLocked(-1, this.mStartActivity.resultTo, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null);
                }
                if (startedResult == 1) {
                    return 99;
                }
                if (startedResult == 0) {
                    return 98;
                }
                return 0;
            }
            if (killProcessOnOtherDisplay(this.mStartActivity, this.mPreferredDisplayId)) {
                reusedActivity = null;
            }
            if (HwPCUtils.enabledInPad() && reusedActivity != null && (("com.android.systemui/.settings.BrightnessDialog".equals(reusedActivity.shortComponentName) || "com.android.incallui".equals(reusedActivity.packageName) || "com.huawei.android.wfdft".equals(reusedActivity.packageName)) && !HwPCUtils.isValidExtDisplayId(reusedActivity.getDisplayId()))) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("startActivityUnchecked reusedActivity :");
                stringBuilder2.append(reusedActivity);
                Slog.i(str, stringBuilder2.toString());
                reusedActivity = null;
            }
        } else if (killProcessOnDefaultDisplay(this.mStartActivity)) {
            reusedActivity = null;
        }
        startedResult = 3;
        if (!(reusedActivity == null || reusedActivity.getTask() == null)) {
            if (this.mService.getLockTaskController().isLockTaskModeViolation(reusedActivity.getTask(), (this.mLaunchFlags & 268468224) == 268468224)) {
                Slog.e(ActivityManagerService.TAG, "startActivityUnchecked: Attempt to violate Lock Task Mode");
                return 101;
            }
            boolean clearTopAndResetStandardLaunchMode = (this.mLaunchFlags & 69206016) == 69206016 && this.mLaunchMode == 0;
            if (this.mStartActivity.getTask() == null && !clearTopAndResetStandardLaunchMode) {
                this.mStartActivity.setTask(reusedActivity.getTask());
            }
            if (reusedActivity.getTask().intent == null) {
                reusedActivity.getTask().setIntent(this.mStartActivity);
            }
            if ((this.mLaunchFlags & 67108864) != 0 || isDocumentLaunchesIntoExisting(this.mLaunchFlags) || isLaunchModeOneOf(3, 2)) {
                task = reusedActivity.getTask();
                ActivityRecord top = task.performClearTaskForReuseLocked(this.mStartActivity, this.mLaunchFlags);
                if (reusedActivity.getTask() == null) {
                    reusedActivity.setTask(task);
                }
                if (top != null) {
                    if (top.frontOfTask) {
                        top.getTask().setIntent(this.mStartActivity);
                    }
                    deliverNewIntent(top);
                }
            }
            this.mSupervisor.sendPowerHintForLaunchStartIfNeeded(false, reusedActivity);
            reusedActivity = setTargetStackAndMoveToFrontIfNeeded(reusedActivity);
            ActivityRecord outResult = (activityRecordArr == null || activityRecordArr.length <= 0) ? null : activityRecordArr[0];
            if (outResult != null && (outResult.finishing || outResult.noDisplay)) {
                activityRecordArr[0] = reusedActivity;
            }
            if ((this.mStartFlags & 1) != 0) {
                resumeTargetStackIfNeeded();
                return 1;
            } else if (reusedActivity != null) {
                setTaskFromIntentActivity(reusedActivity);
                if (!this.mAddingToTask && this.mReuseTask == null) {
                    resumeTargetStackIfNeeded();
                    if (activityRecordArr != null && activityRecordArr.length > 0) {
                        activityRecordArr[0] = reusedActivity;
                    }
                    if (this.mMovedToFront) {
                        startedResult = 2;
                    }
                    return startedResult;
                }
            }
        }
        if (this.mStartActivity.packageName == null) {
            if (this.mStartActivity.resultTo != null) {
                configurationContainer = this.mStartActivity.resultTo.getStack();
            }
            sourceStack = configurationContainer;
            if (sourceStack != null) {
                sourceStack.sendActivityResultLocked(-1, this.mStartActivity.resultTo, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null);
            }
            ActivityOptions.abort(this.mOptions);
            return -92;
        }
        ActivityStack topStack = this.mSupervisor.mFocusedStack;
        ActivityRecord topFocused = topStack.getTopActivity();
        ActivityRecord top2 = topStack.topRunningNonDelayedActivityLocked(this.mNotTop);
        boolean z = top2 != null && this.mStartActivity.resultTo == null && top2.realActivity.equals(this.mStartActivity.realActivity) && top2.userId == this.mStartActivity.userId && top2.app != null && top2.app.thread != null && ((this.mLaunchFlags & 536870912) != 0 || isLaunchModeOneOf(1, 2));
        if (z) {
            topStack.mLastPausedActivity = null;
            if (this.mDoResume) {
                this.mSupervisor.resumeFocusedStackTopActivityLocked();
            }
            ActivityOptions.abort(this.mOptions);
            if ((this.mStartFlags & 1) != 0) {
                return 1;
            }
            deliverNewIntent(top2);
            this.mSupervisor.handleNonResizableTaskIfNeeded(top2.getTask(), preferredWindowingMode, preferredLaunchDisplayId, topStack);
            return 3;
        }
        boolean result = false;
        task = (!this.mLaunchTaskBehind || this.mSourceRecord == null) ? null : this.mSourceRecord.getTask();
        TaskRecord taskToAffiliate = task;
        int result2 = 0;
        if (this.mStartActivity.resultTo == null && this.mInTask == null && !this.mAddingToTask && (this.mLaunchFlags & 268435456) != 0) {
            result = true;
            result2 = setTaskFromReuseOrCreateNewTask(taskToAffiliate, topStack);
        } else if (this.mSourceRecord != null) {
            if (HwPCUtils.isPcCastModeInServer() && this.mSourceRecord.getTask() == null && this.mSourceRecord.getStack() == null) {
                Slog.i(ActivityManagerService.TAG, "ActivityStarter startActivityUnchecked task and stack null");
                setTaskToCurrentTopOrCreateNewTask();
            } else {
                result2 = setTaskFromSourceRecord();
            }
        } else if (this.mInTask != null) {
            result2 = setTaskFromInTask();
        } else {
            setTaskToCurrentTopOrCreateNewTask();
        }
        boolean newTask = result;
        startedResult = result2;
        if (startedResult != 0) {
            return startedResult;
        }
        if (this.mService != null && this.mService.mHwAMSEx != null && !this.mService.mHwAMSEx.checkActivityStartForPCMode(this, this.mOptions, this.mStartActivity, this.mTargetStack)) {
            return -96;
        }
        if (this.mCallingUid == 1000 && this.mIntent.getCallingUid() != 0) {
            this.mCallingUid = this.mIntent.getCallingUid();
            this.mIntent.setCallingUid(0);
        }
        this.mService.grantUriPermissionFromIntentLocked(this.mCallingUid, this.mStartActivity.packageName, this.mIntent, this.mStartActivity.getUriPermissionsLocked(), this.mStartActivity.userId);
        this.mService.grantEphemeralAccessLocked(this.mStartActivity.userId, this.mIntent, this.mStartActivity.appInfo.uid, UserHandle.getAppId(this.mCallingUid));
        if (newTask) {
            EventLog.writeEvent(EventLogTags.AM_CREATE_TASK, new Object[]{Integer.valueOf(this.mStartActivity.userId), Integer.valueOf(this.mStartActivity.getTask().taskId)});
        }
        ActivityStack.logStartActivity(EventLogTags.AM_CREATE_ACTIVITY, this.mStartActivity, this.mStartActivity.getTask());
        this.mTargetStack.mLastPausedActivity = null;
        this.mSupervisor.sendPowerHintForLaunchStartIfNeeded(false, this.mStartActivity);
        this.mTargetStack.startActivityLocked(this.mStartActivity, topFocused, newTask, this.mKeepCurTransition, this.mOptions);
        if (this.mDoResume) {
            ActivityRecord topTaskActivity = this.mStartActivity.getTask().topRunningActivityLocked();
            if (this.mTargetStack.isFocusable() && (topTaskActivity == null || !topTaskActivity.mTaskOverlay || this.mStartActivity == topTaskActivity)) {
                if (this.mTargetStack.isFocusable() && !this.mSupervisor.isFocusedStack(this.mTargetStack)) {
                    this.mTargetStack.moveToFront("startActivityUnchecked");
                }
                this.mSupervisor.resumeFocusedStackTopActivityLocked(this.mTargetStack, this.mStartActivity, this.mOptions);
            } else {
                this.mTargetStack.ensureActivitiesVisibleLocked(null, 0, false);
                this.mService.mWindowManager.executeAppTransition();
            }
        } else if (this.mStartActivity != null) {
            this.mSupervisor.mRecentTasks.add(this.mStartActivity.getTask());
        }
        this.mSupervisor.updateUserStackLocked(this.mStartActivity.userId, this.mTargetStack);
        this.mSupervisor.handleNonResizableTaskIfNeeded(this.mStartActivity.getTask(), preferredWindowingMode, preferredLaunchDisplayId, this.mTargetStack);
        return 0;
    }

    void reset(boolean clearRequest) {
        this.mStartActivity = null;
        this.mIntent = null;
        this.mCallingUid = -1;
        this.mOptions = null;
        this.mLaunchTaskBehind = false;
        this.mLaunchFlags = 0;
        this.mLaunchMode = -1;
        this.mLaunchParams.reset();
        this.mNotTop = null;
        this.mDoResume = false;
        this.mStartFlags = 0;
        this.mSourceRecord = null;
        this.mPreferredDisplayId = -1;
        this.mInTask = null;
        this.mAddingToTask = false;
        this.mReuseTask = null;
        this.mNewTaskInfo = null;
        this.mNewTaskIntent = null;
        this.mSourceStack = null;
        this.mTargetStack = null;
        this.mMovedToFront = false;
        this.mNoAnimation = false;
        this.mKeepCurTransition = false;
        this.mAvoidMoveToFront = false;
        this.mVoiceSession = null;
        this.mVoiceInteractor = null;
        this.mIntentDelivered = false;
        if (clearRequest) {
            this.mRequest.reset();
        }
    }

    protected int hasStartedOnOtherDisplay(ActivityRecord startActivity, int sourceDisplayId) {
        return -1;
    }

    protected boolean killProcessOnOtherDisplay(ActivityRecord startActivity, int sourceDisplayId) {
        return false;
    }

    protected boolean killProcessOnDefaultDisplay(ActivityRecord startActivity) {
        return false;
    }

    protected void setInitialState(ActivityRecord r, ActivityOptions options, TaskRecord inTask, boolean doResume, int startFlags, ActivityRecord sourceRecord, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor) {
        String str;
        ActivityRecord activityRecord = r;
        ActivityOptions activityOptions = options;
        TaskRecord taskRecord = inTask;
        boolean z = doResume;
        int i = startFlags;
        boolean z2 = false;
        reset(false);
        this.mStartActivity = activityRecord;
        this.mIntent = activityRecord.intent;
        this.mOptions = activityOptions;
        this.mCallingUid = activityRecord.launchedFromUid;
        ActivityRecord activityRecord2 = sourceRecord;
        this.mSourceRecord = activityRecord2;
        this.mVoiceSession = voiceSession;
        this.mVoiceInteractor = voiceInteractor;
        this.mPreferredDisplayId = getPreferedDisplayId(this.mSourceRecord, this.mStartActivity, activityOptions);
        this.mLaunchParams.reset();
        this.mSupervisor.getLaunchParamsController().calculate(taskRecord, null, activityRecord, activityRecord2, activityOptions, this.mLaunchParams);
        this.mLaunchMode = activityRecord.launchMode;
        this.mLaunchFlags = adjustLaunchFlagsToDocumentMode(activityRecord, 3 == this.mLaunchMode, 2 == this.mLaunchMode, this.mIntent.getFlags());
        boolean z3 = (!activityRecord.mLaunchTaskBehind || isLaunchModeOneOf(2, 3) || (this.mLaunchFlags & DumpState.DUMP_FROZEN) == 0) ? false : true;
        this.mLaunchTaskBehind = z3;
        sendNewTaskResultRequestIfNeeded();
        if ((this.mLaunchFlags & DumpState.DUMP_FROZEN) != 0 && activityRecord.resultTo == null) {
            this.mLaunchFlags |= 268435456;
        }
        if ((this.mLaunchFlags & 268435456) != 0 && (this.mLaunchTaskBehind || activityRecord.info.documentLaunchMode == 2)) {
            this.mLaunchFlags |= 134217728;
        }
        this.mSupervisor.mUserLeaving = (this.mLaunchFlags & 262144) == 0;
        if (ActivityManagerDebugConfig.DEBUG_USER_LEAVING) {
            str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startActivity() => mUserLeaving=");
            stringBuilder.append(this.mSupervisor.mUserLeaving);
            Slog.v(str, stringBuilder.toString());
        }
        this.mDoResume = z;
        if (!(z && r.okToShowLocked())) {
            activityRecord.delayedResume = true;
            this.mDoResume = false;
        }
        if (this.mOptions != null) {
            if (this.mOptions.getLaunchTaskId() != -1 && this.mOptions.getTaskOverlay()) {
                activityRecord.mTaskOverlay = true;
                if (!this.mOptions.canTaskOverlayResume()) {
                    TaskRecord task = this.mSupervisor.anyTaskForIdLocked(this.mOptions.getLaunchTaskId());
                    ActivityRecord top = task != null ? task.getTopActivity() : null;
                    if (!(top == null || top.isState(ActivityState.RESUMED))) {
                        this.mDoResume = false;
                        this.mAvoidMoveToFront = true;
                    }
                }
            } else if (this.mOptions.getAvoidMoveToFront()) {
                this.mDoResume = false;
                this.mAvoidMoveToFront = true;
            }
        }
        this.mNotTop = (this.mLaunchFlags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0 ? activityRecord : null;
        this.mInTask = taskRecord;
        if (!(taskRecord == null || taskRecord.inRecents)) {
            str = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Starting activity in task not in recents: ");
            stringBuilder2.append(taskRecord);
            Slog.w(str, stringBuilder2.toString());
            this.mInTask = null;
        }
        this.mStartFlags = i;
        if ((i & 1) != 0) {
            ActivityRecord checkedCaller = activityRecord2;
            if (checkedCaller == null) {
                checkedCaller = this.mSupervisor.mFocusedStack.topRunningNonDelayedActivityLocked(this.mNotTop);
            }
            if (!checkedCaller.realActivity.equals(activityRecord.realActivity)) {
                this.mStartFlags &= -2;
            }
        }
        if ((this.mLaunchFlags & 65536) != 0) {
            z2 = true;
        }
        this.mNoAnimation = z2;
    }

    private void sendNewTaskResultRequestIfNeeded() {
        ActivityStack sourceStack = this.mStartActivity.resultTo != null ? this.mStartActivity.resultTo.getStack() : null;
        if (!(sourceStack == null || (this.mLaunchFlags & 268435456) == 0)) {
            if (isInSkipCancelResultList(this.mStartActivity.shortComponentName)) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("we skip cancelling activity result from activity ");
                stringBuilder.append(this.mStartActivity.shortComponentName);
                Slog.w(str, stringBuilder.toString());
                return;
            }
            Slog.w(ActivityManagerService.TAG, "Activity is launching as a new task, so cancelling activity result.");
            sourceStack.sendActivityResultLocked(-1, this.mStartActivity.resultTo, this.mStartActivity.resultWho, this.mStartActivity.requestCode, 0, null);
            this.mStartActivity.resultTo = null;
        }
    }

    private void computeLaunchingTaskFlags() {
        if (this.mSourceRecord != null || this.mInTask == null || this.mInTask.getStack() == null) {
            this.mInTask = null;
            if ((this.mStartActivity.isResolverActivity() || this.mStartActivity.noDisplay) && this.mSourceRecord != null && this.mSourceRecord.inFreeformWindowingMode()) {
                this.mAddingToTask = true;
            }
        } else {
            Intent baseIntent = this.mInTask.getBaseIntent();
            ActivityRecord root = this.mInTask.getRootActivity();
            StringBuilder stringBuilder;
            if (baseIntent != null) {
                if (isLaunchModeOneOf(3, 2)) {
                    if (!baseIntent.getComponent().equals(this.mStartActivity.intent.getComponent())) {
                        ActivityOptions.abort(this.mOptions);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Trying to launch singleInstance/Task ");
                        stringBuilder.append(this.mStartActivity);
                        stringBuilder.append(" into different task ");
                        stringBuilder.append(this.mInTask);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    } else if (root != null) {
                        ActivityOptions.abort(this.mOptions);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Caller with mInTask ");
                        stringBuilder.append(this.mInTask);
                        stringBuilder.append(" has root ");
                        stringBuilder.append(root);
                        stringBuilder.append(" but target is singleInstance/Task");
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                if (root == null) {
                    this.mLaunchFlags = (this.mLaunchFlags & -403185665) | (baseIntent.getFlags() & 403185664);
                    this.mIntent.setFlags(this.mLaunchFlags);
                    this.mInTask.setIntent(this.mStartActivity);
                    this.mAddingToTask = true;
                } else if ((this.mLaunchFlags & 268435456) != 0) {
                    this.mAddingToTask = false;
                } else {
                    this.mAddingToTask = true;
                }
                this.mReuseTask = this.mInTask;
            } else {
                ActivityOptions.abort(this.mOptions);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Launching into task without base intent: ");
                stringBuilder.append(this.mInTask);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (this.mInTask != null) {
            return;
        }
        if (this.mSourceRecord == null) {
            if ((this.mLaunchFlags & 268435456) == 0 && this.mInTask == null) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("startActivity called from non-Activity context; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: ");
                stringBuilder2.append(this.mIntent);
                Slog.w(str, stringBuilder2.toString());
                this.mLaunchFlags |= 268435456;
            }
        } else if (this.mSourceRecord.launchMode == 3) {
            this.mLaunchFlags |= 268435456;
        } else if (isLaunchModeOneOf(3, 2)) {
            this.mLaunchFlags |= 268435456;
        }
    }

    private void computeSourceStack() {
        if (this.mSourceRecord == null) {
            this.mSourceStack = null;
        } else if (this.mSourceRecord.finishing) {
            if ((this.mLaunchFlags & 268435456) == 0) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startActivity called from finishing ");
                stringBuilder.append(this.mSourceRecord);
                stringBuilder.append("; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: ");
                stringBuilder.append(this.mIntent);
                Slog.w(str, stringBuilder.toString());
                this.mLaunchFlags |= 268435456;
                this.mNewTaskInfo = this.mSourceRecord.info;
                TaskRecord sourceTask = this.mSourceRecord.getTask();
                this.mNewTaskIntent = sourceTask != null ? sourceTask.intent : null;
            }
            this.mSourceRecord = null;
            this.mSourceStack = null;
        } else {
            this.mSourceStack = this.mSourceRecord.getStack();
        }
    }

    private ActivityRecord getReusableIntentActivity() {
        boolean z = false;
        boolean putIntoExistingTask = ((this.mLaunchFlags & 268435456) != 0 && (this.mLaunchFlags & 134217728) == 0) || isLaunchModeOneOf(3, 2);
        int i = (this.mInTask == null && this.mStartActivity.resultTo == null) ? 1 : 0;
        putIntoExistingTask &= i;
        if (this.mOptions != null && this.mOptions.getLaunchTaskId() != -1) {
            TaskRecord task = this.mSupervisor.anyTaskForIdLocked(this.mOptions.getLaunchTaskId());
            return task != null ? task.getTopActivity() : null;
        } else if (!putIntoExistingTask) {
            return null;
        } else {
            if (3 == this.mLaunchMode) {
                return this.mSupervisor.findActivityLocked(this.mIntent, this.mStartActivity.info, this.mStartActivity.isActivityTypeHome());
            }
            if ((this.mLaunchFlags & 4096) == 0) {
                return this.mSupervisor.findTaskLocked(this.mStartActivity, this.mPreferredDisplayId);
            }
            ActivityStackSupervisor activityStackSupervisor = this.mSupervisor;
            Intent intent = this.mIntent;
            ActivityInfo activityInfo = this.mStartActivity.info;
            if (2 != this.mLaunchMode) {
                z = true;
            }
            return activityStackSupervisor.findActivityLocked(intent, activityInfo, z);
        }
    }

    protected int getPreferedDisplayId(ActivityRecord sourceRecord, ActivityRecord startingActivity, ActivityOptions options) {
        if (startingActivity != null && startingActivity.requestedVrComponent != null) {
            return 0;
        }
        int displayId = this.mService.mVr2dDisplayId;
        if (displayId != -1) {
            if (ActivityManagerDebugConfig.DEBUG_STACK) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getSourceDisplayId :");
                stringBuilder.append(displayId);
                Slog.d(str, stringBuilder.toString());
            }
            return displayId;
        }
        int launchDisplayId = options != null ? options.getLaunchDisplayId() : -1;
        if (launchDisplayId != -1) {
            return launchDisplayId;
        }
        displayId = sourceRecord != null ? sourceRecord.getDisplayId() : -1;
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && ("com.android.incallui".equals(startingActivity.packageName) || "com.android.systemui/.settings.BrightnessDialog".equals(startingActivity.shortComponentName))) {
            String str2 = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getSourceDisplayId set displayId :");
            stringBuilder2.append(HwPCUtils.getPCDisplayID());
            stringBuilder2.append(", packageName :");
            stringBuilder2.append(startingActivity.packageName);
            stringBuilder2.append(", shortComponentName :");
            stringBuilder2.append(startingActivity.shortComponentName);
            Slog.d(str2, stringBuilder2.toString());
            displayId = HwPCUtils.getPCDisplayID();
        }
        IVRSystemServiceManager vrMananger = HwFrameworkFactory.getVRSystemServiceManager();
        if (vrMananger != null ? vrMananger.isVirtualScreenMode() : false) {
            displayId = -1;
        }
        if (displayId != -1) {
            return displayId;
        }
        return 0;
    }

    private ActivityRecord setTargetStackAndMoveToFrontIfNeeded(ActivityRecord intentActivity) {
        ActivityRecord activityRecord = intentActivity;
        this.mTargetStack = intentActivity.getStack();
        this.mTargetStack.mLastPausedActivity = null;
        ActivityStack focusStack = this.mSupervisor.getFocusedStack();
        ActivityRecord curTop = focusStack == null ? null : focusStack.topRunningNonDelayedActivityLocked(this.mNotTop);
        TaskRecord topTask = curTop != null ? curTop.getTask() : null;
        if (!(topTask == null || ((topTask == intentActivity.getTask() && topTask == focusStack.topTask()) || this.mAvoidMoveToFront))) {
            this.mStartActivity.intent.addFlags(DumpState.DUMP_CHANGES);
            if (this.mSourceRecord == null || (this.mSourceStack.getTopActivity() != null && this.mSourceStack.getTopActivity().getTask() == this.mSourceRecord.getTask())) {
                if (this.mLaunchTaskBehind && this.mSourceRecord != null) {
                    activityRecord.setTaskToAffiliateWith(this.mSourceRecord.getTask());
                }
                if (!((this.mLaunchFlags & 268468224) == 268468224)) {
                    ActivityStack launchStack = getLaunchStack(this.mStartActivity, this.mLaunchFlags, this.mStartActivity.getTask(), this.mOptions);
                    TaskRecord intentTask = intentActivity.getTask();
                    if (launchStack == null || launchStack == this.mTargetStack) {
                        this.mTargetStack.moveTaskToFrontLocked(intentTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "bringingFoundTaskToFront");
                        this.mMovedToFront = true;
                    } else if (launchStack.inSplitScreenWindowingMode()) {
                        if ((this.mLaunchFlags & 4096) != 0) {
                            intentTask.reparent(launchStack, true, 0, true, true, "launchToSide");
                        } else {
                            this.mTargetStack.moveTaskToFrontLocked(intentTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "bringToFrontInsteadOfAdjacentLaunch");
                        }
                        this.mMovedToFront = launchStack != launchStack.getDisplay().getTopStackInWindowingMode(launchStack.getWindowingMode());
                    } else if (launchStack.mDisplayId != this.mTargetStack.mDisplayId) {
                        if (HwPCUtils.isPcCastModeInServer()) {
                            String str = ActivityManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(" the activity will reparentToDisplay because computer stack is:");
                            stringBuilder.append(launchStack.mStackId);
                            stringBuilder.append("#");
                            stringBuilder.append(launchStack.mDisplayId);
                            stringBuilder.append(" target stack is ");
                            stringBuilder.append(this.mTargetStack.mStackId);
                            stringBuilder.append("#");
                            stringBuilder.append(this.mTargetStack.mDisplayId);
                            HwPCUtils.log(str, stringBuilder.toString());
                        }
                        intentActivity.getTask().reparent(launchStack, true, 0, true, true, "reparentToDisplay");
                        this.mMovedToFront = true;
                    } else if (launchStack.isActivityTypeHome() && !this.mTargetStack.isActivityTypeHome()) {
                        intentActivity.getTask().reparent(launchStack, true, 0, true, true, "reparentingHome");
                        this.mMovedToFront = true;
                    }
                    this.mOptions = null;
                    if (launchStack != null && launchStack.getAllTasks().isEmpty() && HwPCUtils.isExtDynamicStack(launchStack.mStackId)) {
                        launchStack.remove();
                    }
                    if (!INCALLUI_ACTIVITY_CLASS_NAME.equals(activityRecord.shortComponentName)) {
                        activityRecord.showStartingWindow(null, false, true);
                    }
                }
            }
        }
        this.mTargetStack = intentActivity.getStack();
        if (!this.mMovedToFront && this.mDoResume) {
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                String str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Bring to front target: ");
                stringBuilder2.append(this.mTargetStack);
                stringBuilder2.append(" from ");
                stringBuilder2.append(activityRecord);
                Slog.d(str2, stringBuilder2.toString());
            }
            this.mTargetStack.moveToFront("intentActivityFound");
        }
        this.mSupervisor.handleNonResizableTaskIfNeeded(intentActivity.getTask(), 0, 0, this.mTargetStack);
        if ((this.mLaunchFlags & DumpState.DUMP_COMPILER_STATS) != 0) {
            return this.mTargetStack.resetTaskIfNeededLocked(activityRecord, this.mStartActivity);
        }
        return activityRecord;
    }

    private void setTaskFromIntentActivity(ActivityRecord intentActivity) {
        if ((this.mLaunchFlags & 268468224) == 268468224) {
            TaskRecord task = intentActivity.getTask();
            task.performClearTaskLocked();
            this.mReuseTask = task;
            this.mReuseTask.setIntent(this.mStartActivity);
        } else if ((this.mLaunchFlags & 67108864) != 0 || isLaunchModeOneOf(3, 2)) {
            if (intentActivity.getTask().performClearTaskLocked(this.mStartActivity, this.mLaunchFlags) == null) {
                this.mAddingToTask = true;
                if (!isInSkipCancelResultList(this.mStartActivity.shortComponentName)) {
                    this.mStartActivity.setTask(null);
                }
                this.mSourceRecord = intentActivity;
                TaskRecord task2 = this.mSourceRecord.getTask();
                if (task2 != null && task2.getStack() == null) {
                    this.mTargetStack = computeStackFocus(this.mSourceRecord, false, this.mLaunchFlags, this.mOptions);
                    this.mTargetStack.addTask(task2, true ^ this.mLaunchTaskBehind, "startActivityUnchecked");
                }
            }
        } else if (this.mStartActivity.realActivity.equals(intentActivity.getTask().realActivity)) {
            if (((this.mLaunchFlags & 536870912) != 0 || 1 == this.mLaunchMode) && intentActivity.realActivity.equals(this.mStartActivity.realActivity)) {
                if (intentActivity.frontOfTask) {
                    intentActivity.getTask().setIntent(this.mStartActivity);
                }
                deliverNewIntent(intentActivity);
            } else if (!intentActivity.getTask().isSameIntentFilter(this.mStartActivity)) {
                if (!this.mStartActivity.intent.filterEquals(intentActivity.intent) || !"android.intent.action.MAIN".equals(this.mStartActivity.intent.getAction())) {
                    this.mAddingToTask = true;
                    this.mSourceRecord = intentActivity;
                }
            }
        } else if ((this.mLaunchFlags & DumpState.DUMP_COMPILER_STATS) == 0) {
            this.mAddingToTask = true;
            this.mSourceRecord = intentActivity;
        } else if (!intentActivity.getTask().rootWasReset) {
            intentActivity.getTask().setIntent(this.mStartActivity);
        }
    }

    private void resumeTargetStackIfNeeded() {
        if (this.mDoResume) {
            this.mSupervisor.resumeFocusedStackTopActivityLocked(this.mTargetStack, null, this.mOptions);
        } else {
            ActivityOptions.abort(this.mOptions);
        }
        this.mSupervisor.updateUserStackLocked(this.mStartActivity.userId, this.mTargetStack);
    }

    private int setTaskFromReuseOrCreateNewTask(TaskRecord taskToAffiliate, ActivityStack topStack) {
        TaskRecord taskRecord = taskToAffiliate;
        this.mTargetStack = computeStackFocus(this.mStartActivity, true, this.mLaunchFlags, this.mOptions);
        if (this.mReuseTask == null) {
            addOrReparentStartingActivity(this.mTargetStack.createTaskRecord(this.mSupervisor.getNextTaskIdForUserLocked(this.mStartActivity.userId), this.mNewTaskInfo != null ? this.mNewTaskInfo : this.mStartActivity.info, this.mNewTaskIntent != null ? this.mNewTaskIntent : this.mIntent, this.mVoiceSession, this.mVoiceInteractor, this.mLaunchTaskBehind ^ 1, this.mStartActivity, this.mSourceRecord, this.mOptions), "setTaskFromReuseOrCreateNewTask - mReuseTask");
            updateBounds(this.mStartActivity.getTask(), this.mLaunchParams.mBounds);
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Starting new activity ");
                stringBuilder.append(this.mStartActivity);
                stringBuilder.append(" in new task ");
                stringBuilder.append(this.mStartActivity.getTask());
                Slog.v(str, stringBuilder.toString());
            }
        } else {
            addOrReparentStartingActivity(this.mReuseTask, "setTaskFromReuseOrCreateNewTask");
        }
        if (taskRecord != null) {
            this.mStartActivity.setTaskToAffiliateWith(taskRecord);
        }
        if (this.mService.getLockTaskController().isLockTaskModeViolation(this.mStartActivity.getTask())) {
            String str2 = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Attempted Lock Task Mode violation mStartActivity=");
            stringBuilder2.append(this.mStartActivity);
            Slog.e(str2, stringBuilder2.toString());
            return 101;
        }
        if (this.mDoResume) {
            this.mTargetStack.moveToFront("reuseOrNewTask");
        }
        return 0;
    }

    private void deliverNewIntent(ActivityRecord activity) {
        if (!this.mIntentDelivered) {
            ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, activity, activity.getTask());
            activity.deliverNewIntentLocked(this.mCallingUid, this.mStartActivity.intent, this.mStartActivity.launchedFromPackage);
            this.mIntentDelivered = true;
        }
    }

    private int setTaskFromSourceRecord() {
        if (this.mService.getLockTaskController().isLockTaskModeViolation(this.mSourceRecord.getTask())) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempted Lock Task Mode violation mStartActivity=");
            stringBuilder.append(this.mStartActivity);
            Slog.e(str, stringBuilder.toString());
            return 101;
        }
        int i;
        TaskRecord sourceTask = this.mSourceRecord.getTask();
        ActivityStack sourceStack = this.mSourceRecord.getStack();
        if (this.mTargetStack != null) {
            i = this.mTargetStack.mDisplayId;
        } else {
            i = sourceStack.mDisplayId;
        }
        int targetDisplayId = i;
        boolean z = (sourceStack.topTask() == sourceTask && this.mStartActivity.canBeLaunchedOnDisplay(targetDisplayId)) ? false : true;
        if (z) {
            this.mTargetStack = getLaunchStack(this.mStartActivity, this.mLaunchFlags, this.mStartActivity.getTask(), this.mOptions);
            if (this.mTargetStack == null && targetDisplayId != sourceStack.mDisplayId) {
                this.mTargetStack = this.mService.mStackSupervisor.getValidLaunchStackOnDisplay(sourceStack.mDisplayId, this.mStartActivity);
            }
            if (this.mTargetStack == null) {
                this.mTargetStack = this.mService.mStackSupervisor.getNextValidLaunchStackLocked(this.mStartActivity, -1);
            }
        }
        if (this.mTargetStack == null) {
            this.mTargetStack = sourceStack;
        } else if (this.mTargetStack != sourceStack) {
            sourceTask.reparent(this.mTargetStack, true, 0, false, true, "launchToSide");
        }
        if (this.mTargetStack.topTask() != sourceTask && !this.mAvoidMoveToFront) {
            this.mTargetStack.moveTaskToFrontLocked(sourceTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "sourceTaskToFront");
        } else if (this.mDoResume) {
            this.mTargetStack.moveToFront("sourceStackToFront");
        }
        ActivityRecord top;
        if (!this.mAddingToTask && (this.mLaunchFlags & 67108864) != 0) {
            top = sourceTask.performClearTaskLocked(this.mStartActivity, this.mLaunchFlags);
            this.mKeepCurTransition = true;
            if (top != null) {
                ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, this.mStartActivity, top.getTask());
                deliverNewIntent(top);
                this.mTargetStack.mLastPausedActivity = null;
                if (this.mDoResume) {
                    this.mSupervisor.resumeFocusedStackTopActivityLocked();
                }
                ActivityOptions.abort(this.mOptions);
                return 3;
            }
        } else if (!(this.mAddingToTask || (this.mLaunchFlags & 131072) == 0)) {
            top = sourceTask.findActivityInHistoryLocked(this.mStartActivity);
            if (top != null) {
                TaskRecord task = top.getTask();
                task.moveActivityToFrontLocked(top);
                top.updateOptionsLocked(this.mOptions);
                ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, this.mStartActivity, task);
                deliverNewIntent(top);
                this.mTargetStack.mLastPausedActivity = null;
                if (this.mDoResume) {
                    this.mSupervisor.resumeFocusedStackTopActivityLocked();
                }
                return 3;
            }
        }
        addOrReparentStartingActivity(sourceTask, "setTaskFromSourceRecord");
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            String str2 = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Starting new activity ");
            stringBuilder2.append(this.mStartActivity);
            stringBuilder2.append(" in existing task ");
            stringBuilder2.append(this.mStartActivity.getTask());
            stringBuilder2.append(" from source ");
            stringBuilder2.append(this.mSourceRecord);
            Slog.v(str2, stringBuilder2.toString());
        }
        return 0;
    }

    private int setTaskFromInTask() {
        if (this.mService.getLockTaskController().isLockTaskModeViolation(this.mInTask)) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempted Lock Task Mode violation mStartActivity=");
            stringBuilder.append(this.mStartActivity);
            Slog.e(str, stringBuilder.toString());
            return 101;
        }
        this.mTargetStack = this.mInTask.getStack();
        ActivityRecord top = this.mInTask.getTopActivity();
        if (top != null && top.realActivity.equals(this.mStartActivity.realActivity) && top.userId == this.mStartActivity.userId && ((this.mLaunchFlags & 536870912) != 0 || isLaunchModeOneOf(1, 2))) {
            this.mTargetStack.moveTaskToFrontLocked(this.mInTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "inTaskToFront");
            if ((this.mStartFlags & 1) != 0) {
                return 1;
            }
            deliverNewIntent(top);
            return 3;
        } else if (this.mAddingToTask) {
            if (!this.mLaunchParams.mBounds.isEmpty()) {
                ActivityStack stack = this.mSupervisor.getLaunchStack(null, null, this.mInTask, true);
                if (stack != this.mInTask.getStack()) {
                    this.mInTask.reparent(stack, true, 1, false, true, "inTaskToFront");
                    this.mTargetStack = this.mInTask.getStack();
                }
                updateBounds(this.mInTask, this.mLaunchParams.mBounds);
            }
            this.mTargetStack.moveTaskToFrontLocked(this.mInTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "inTaskToFront");
            addOrReparentStartingActivity(this.mInTask, "setTaskFromInTask");
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                String str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Starting new activity ");
                stringBuilder2.append(this.mStartActivity);
                stringBuilder2.append(" in explicit task ");
                stringBuilder2.append(this.mStartActivity.getTask());
                Slog.v(str2, stringBuilder2.toString());
            }
            return 0;
        } else {
            this.mTargetStack.moveTaskToFrontLocked(this.mInTask, this.mNoAnimation, this.mOptions, this.mStartActivity.appTimeTracker, "inTaskToFront");
            ActivityOptions.abort(this.mOptions);
            return 2;
        }
    }

    @VisibleForTesting
    void updateBounds(TaskRecord task, Rect bounds) {
        if (!bounds.isEmpty()) {
            ActivityStack stack = task.getStack();
            if (stack == null || !stack.resizeStackWithLaunchBounds()) {
                task.updateOverrideConfiguration(bounds);
            } else {
                this.mService.resizeStack(stack.mStackId, bounds, true, false, true, -1);
            }
        }
    }

    private void setTaskToCurrentTopOrCreateNewTask() {
        this.mTargetStack = computeStackFocus(this.mStartActivity, false, this.mLaunchFlags, this.mOptions);
        if (this.mDoResume) {
            this.mTargetStack.moveToFront("addingToTopTask");
        }
        ActivityRecord prev = this.mTargetStack.getTopActivity();
        TaskRecord task = prev != null ? prev.getTask() : this.mTargetStack.createTaskRecord(this.mSupervisor.getNextTaskIdForUserLocked(this.mStartActivity.userId), this.mStartActivity.info, this.mIntent, null, null, true, this.mStartActivity, this.mSourceRecord, this.mOptions);
        addOrReparentStartingActivity(task, "setTaskToCurrentTopOrCreateNewTask");
        this.mTargetStack.positionChildWindowContainerAtTop(task);
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Starting new activity ");
            stringBuilder.append(this.mStartActivity);
            stringBuilder.append(" in new guessed ");
            stringBuilder.append(this.mStartActivity.getTask());
            Slog.v(str, stringBuilder.toString());
        }
    }

    private void addOrReparentStartingActivity(TaskRecord parent, String reason) {
        if (ActivityManagerDebugConfig.DEBUG_TASKS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addOrReparentStartingActivity reason: ");
            stringBuilder.append(reason);
            Slog.v(str, stringBuilder.toString());
        }
        if (this.mStartActivity.getTask() == null || this.mStartActivity.getTask() == parent) {
            parent.addActivityToTop(this.mStartActivity);
        } else {
            this.mStartActivity.reparent(parent, parent.mActivities.size(), reason);
        }
    }

    private int adjustLaunchFlagsToDocumentMode(ActivityRecord r, boolean launchSingleInstance, boolean launchSingleTask, int launchFlags) {
        if ((launchFlags & DumpState.DUMP_FROZEN) == 0 || !(launchSingleInstance || launchSingleTask)) {
            switch (r.info.documentLaunchMode) {
                case 1:
                    return launchFlags | DumpState.DUMP_FROZEN;
                case 2:
                    return launchFlags | DumpState.DUMP_FROZEN;
                case 3:
                    return launchFlags & -134217729;
                default:
                    return launchFlags;
            }
        }
        Slog.i(ActivityManagerService.TAG, "Ignoring FLAG_ACTIVITY_NEW_DOCUMENT, launchMode is \"singleInstance\" or \"singleTask\"");
        return launchFlags & -134742017;
    }

    private ActivityStack computeStackFocus(ActivityRecord r, boolean newTask, int launchFlags, ActivityOptions aOptions) {
        TaskRecord task = r.getTask();
        ActivityStack stack = getLaunchStack(r, launchFlags, task, aOptions);
        if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getLaunchStack stack:");
            stringBuilder.append(stack);
            Slog.d(str, stringBuilder.toString());
        }
        if (stack != null) {
            return stack;
        }
        ActivityStack currentStack = task != null ? task.getStack() : null;
        String str2;
        StringBuilder stringBuilder2;
        if (currentStack != null) {
            if (this.mSupervisor.mFocusedStack != currentStack) {
                if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("computeStackFocus: Setting focused stack to r=");
                    stringBuilder2.append(r);
                    stringBuilder2.append(" task=");
                    stringBuilder2.append(task);
                    Slog.d(str2, stringBuilder2.toString());
                }
            } else if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                str2 = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("computeStackFocus: Focused stack already=");
                stringBuilder2.append(this.mSupervisor.mFocusedStack);
                Slog.d(str2, stringBuilder2.toString());
            }
            return currentStack;
        } else if (canLaunchIntoFocusedStack(r, newTask)) {
            if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                str2 = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("computeStackFocus: Have a focused stack=");
                stringBuilder2.append(this.mSupervisor.mFocusedStack);
                Slog.d(str2, stringBuilder2.toString());
            }
            return this.mSupervisor.mFocusedStack;
        } else {
            if (this.mPreferredDisplayId != 0) {
                stack = this.mSupervisor.getValidLaunchStackOnDisplay(this.mPreferredDisplayId, r);
                if (stack == null) {
                    if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                        str2 = ActivityManagerService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("computeStackFocus: Can't launch on mPreferredDisplayId=");
                        stringBuilder2.append(this.mPreferredDisplayId);
                        stringBuilder2.append(", looking on all displays.");
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    stack = this.mSupervisor.getNextValidLaunchStackLocked(r, this.mPreferredDisplayId);
                }
            }
            if (stack == null) {
                ActivityDisplay display = this.mSupervisor.getDefaultDisplay();
                int stackNdx = display.getChildCount() - 1;
                while (stackNdx >= 0) {
                    stack = display.getChildAt(stackNdx);
                    if (stack.isOnHomeDisplay()) {
                        stackNdx--;
                    } else {
                        if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                            String str3 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("computeStackFocus: Setting focused stack=");
                            stringBuilder3.append(stack);
                            Slog.d(str3, stringBuilder3.toString());
                        }
                        return stack;
                    }
                }
                stack = this.mSupervisor.getLaunchStack(r, aOptions, task, true);
            }
            if (ActivityManagerDebugConfig.DEBUG_FOCUS || ActivityManagerDebugConfig.DEBUG_STACK) {
                str2 = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("computeStackFocus: New stack r=");
                stringBuilder2.append(r);
                stringBuilder2.append(" stackId=");
                stringBuilder2.append(stack.mStackId);
                Slog.d(str2, stringBuilder2.toString());
            }
            return stack;
        }
    }

    private boolean canLaunchIntoFocusedStack(ActivityRecord r, boolean newTask) {
        boolean canUseFocusedStack;
        ActivityStack focusedStack = this.mSupervisor.mFocusedStack;
        boolean z = false;
        if (focusedStack.isActivityTypeAssistant()) {
            canUseFocusedStack = r.isActivityTypeAssistant();
        } else {
            int windowingMode = focusedStack.getWindowingMode();
            if (windowingMode != 1) {
                switch (windowingMode) {
                    case 3:
                    case 4:
                        canUseFocusedStack = r.supportsSplitScreenWindowingMode();
                        break;
                    case 5:
                        canUseFocusedStack = r.supportsFreeform();
                        break;
                    default:
                        if (!HwPCUtils.isExtDynamicStack(focusedStack.mStackId)) {
                            if (!focusedStack.isOnHomeDisplay() && r.canBeLaunchedOnDisplay(focusedStack.mDisplayId)) {
                                canUseFocusedStack = true;
                                break;
                            }
                            canUseFocusedStack = false;
                            break;
                        }
                        return false;
                }
            }
            canUseFocusedStack = true;
        }
        if (canUseFocusedStack && !newTask && this.mPreferredDisplayId == focusedStack.mDisplayId) {
            z = true;
        }
        return z;
    }

    private ActivityStack getLaunchStack(ActivityRecord r, int launchFlags, TaskRecord task, ActivityOptions aOptions) {
        if (this.mReuseTask != null) {
            return this.mReuseTask.getStack();
        }
        if ((launchFlags & 4096) == 0 || this.mPreferredDisplayId != 0) {
            return this.mSupervisor.getLaunchStack(r, aOptions, task, true, this.mPreferredDisplayId != 0 ? this.mPreferredDisplayId : -1);
        }
        ActivityStack parentStack = task != null ? task.getStack() : this.mSupervisor.mFocusedStack;
        if (parentStack != this.mSupervisor.mFocusedStack) {
            return parentStack;
        }
        if (this.mSupervisor.mFocusedStack != null && task == this.mSupervisor.mFocusedStack.topTask()) {
            return this.mSupervisor.mFocusedStack;
        }
        if (parentStack == null || !parentStack.inSplitScreenPrimaryWindowingMode()) {
            ActivityStack dockedStack = this.mSupervisor.getDefaultDisplay().getSplitScreenPrimaryStack();
            if (dockedStack == null || dockedStack.shouldBeVisible(r)) {
                return dockedStack;
            }
            return this.mSupervisor.getLaunchStack(r, aOptions, task, true);
        }
        return parentStack.getDisplay().getOrCreateStack(4, this.mSupervisor.resolveActivityType(r, this.mOptions, task), true);
    }

    private boolean isLaunchModeOneOf(int mode1, int mode2) {
        return mode1 == this.mLaunchMode || mode2 == this.mLaunchMode;
    }

    static boolean isDocumentLaunchesIntoExisting(int flags) {
        return (DumpState.DUMP_FROZEN & flags) != 0 && (134217728 & flags) == 0;
    }

    ActivityStarter setIntent(Intent intent) {
        this.mRequest.intent = intent;
        return this;
    }

    @VisibleForTesting
    Intent getIntent() {
        return this.mRequest.intent;
    }

    ActivityStarter setReason(String reason) {
        this.mRequest.reason = reason;
        return this;
    }

    ActivityStarter setCaller(IApplicationThread caller) {
        this.mRequest.caller = caller;
        return this;
    }

    ActivityStarter setEphemeralIntent(Intent intent) {
        this.mRequest.ephemeralIntent = intent;
        return this;
    }

    ActivityStarter setResolvedType(String type) {
        this.mRequest.resolvedType = type;
        return this;
    }

    ActivityStarter setActivityInfo(ActivityInfo info) {
        this.mRequest.activityInfo = info;
        return this;
    }

    ActivityStarter setResolveInfo(ResolveInfo info) {
        this.mRequest.resolveInfo = info;
        return this;
    }

    ActivityStarter setVoiceSession(IVoiceInteractionSession voiceSession) {
        this.mRequest.voiceSession = voiceSession;
        return this;
    }

    ActivityStarter setVoiceInteractor(IVoiceInteractor voiceInteractor) {
        this.mRequest.voiceInteractor = voiceInteractor;
        return this;
    }

    ActivityStarter setResultTo(IBinder resultTo) {
        this.mRequest.resultTo = resultTo;
        return this;
    }

    ActivityStarter setResultWho(String resultWho) {
        this.mRequest.resultWho = resultWho;
        return this;
    }

    ActivityStarter setRequestCode(int requestCode) {
        this.mRequest.requestCode = requestCode;
        return this;
    }

    ActivityStarter setCallingPid(int pid) {
        this.mRequest.callingPid = pid;
        return this;
    }

    ActivityStarter setCallingUid(int uid) {
        this.mRequest.callingUid = uid;
        return this;
    }

    ActivityStarter setCallingPackage(String callingPackage) {
        this.mRequest.callingPackage = callingPackage;
        return this;
    }

    ActivityStarter setRealCallingPid(int pid) {
        this.mRequest.realCallingPid = pid;
        return this;
    }

    ActivityStarter setRealCallingUid(int uid) {
        this.mRequest.realCallingUid = uid;
        return this;
    }

    ActivityStarter setStartFlags(int startFlags) {
        this.mRequest.startFlags = startFlags;
        return this;
    }

    ActivityStarter setActivityOptions(SafeActivityOptions options) {
        this.mRequest.activityOptions = options;
        return this;
    }

    ActivityStarter setActivityOptions(Bundle bOptions) {
        return setActivityOptions(SafeActivityOptions.fromBundle(bOptions));
    }

    ActivityStarter setIgnoreTargetSecurity(boolean ignoreTargetSecurity) {
        this.mRequest.ignoreTargetSecurity = ignoreTargetSecurity;
        return this;
    }

    ActivityStarter setFilterCallingUid(int filterCallingUid) {
        this.mRequest.filterCallingUid = filterCallingUid;
        return this;
    }

    ActivityStarter setComponentSpecified(boolean componentSpecified) {
        this.mRequest.componentSpecified = componentSpecified;
        return this;
    }

    ActivityStarter setOutActivity(ActivityRecord[] outActivity) {
        this.mRequest.outActivity = outActivity;
        return this;
    }

    ActivityStarter setInTask(TaskRecord inTask) {
        this.mRequest.inTask = inTask;
        return this;
    }

    ActivityStarter setWaitResult(WaitResult result) {
        this.mRequest.waitResult = result;
        return this;
    }

    ActivityStarter setProfilerInfo(ProfilerInfo info) {
        this.mRequest.profilerInfo = info;
        return this;
    }

    ActivityStarter setGlobalConfiguration(Configuration config) {
        this.mRequest.globalConfig = config;
        return this;
    }

    ActivityStarter setUserId(int userId) {
        this.mRequest.userId = userId;
        return this;
    }

    ActivityStarter setMayWait(int userId) {
        this.mRequest.mayWait = true;
        this.mRequest.userId = userId;
        return this;
    }

    ActivityStarter setAllowPendingRemoteAnimationRegistryLookup(boolean allowLookup) {
        this.mRequest.allowPendingRemoteAnimationRegistryLookup = allowLookup;
        return this;
    }

    void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  ");
        prefix = stringBuilder.toString();
        pw.print(prefix);
        pw.print("mCurrentUser=");
        pw.println(this.mSupervisor.mCurrentUser);
        pw.print(prefix);
        pw.print("mLastStartReason=");
        pw.println(this.mLastStartReason);
        pw.print(prefix);
        pw.print("mLastStartActivityTimeMs=");
        pw.println(DateFormat.getDateTimeInstance().format(new Date(this.mLastStartActivityTimeMs)));
        pw.print(prefix);
        pw.print("mLastStartActivityResult=");
        pw.println(this.mLastStartActivityResult);
        boolean z = false;
        ActivityRecord r = this.mLastStartActivityRecord[0];
        if (r != null) {
            pw.print(prefix);
            pw.println("mLastStartActivityRecord:");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("  ");
            r.dump(pw, stringBuilder2.toString());
        }
        if (this.mStartActivity != null) {
            pw.print(prefix);
            pw.println("mStartActivity:");
            ActivityRecord activityRecord = this.mStartActivity;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(prefix);
            stringBuilder3.append("  ");
            activityRecord.dump(pw, stringBuilder3.toString());
        }
        if (this.mIntent != null) {
            pw.print(prefix);
            pw.print("mIntent=");
            pw.println(this.mIntent);
        }
        if (this.mOptions != null) {
            pw.print(prefix);
            pw.print("mOptions=");
            pw.println(this.mOptions);
        }
        pw.print(prefix);
        pw.print("mLaunchSingleTop=");
        pw.print(1 == this.mLaunchMode);
        pw.print(" mLaunchSingleInstance=");
        pw.print(3 == this.mLaunchMode);
        pw.print(" mLaunchSingleTask=");
        if (2 == this.mLaunchMode) {
            z = true;
        }
        pw.println(z);
        pw.print(prefix);
        pw.print("mLaunchFlags=0x");
        pw.print(Integer.toHexString(this.mLaunchFlags));
        pw.print(" mDoResume=");
        pw.print(this.mDoResume);
        pw.print(" mAddingToTask=");
        pw.println(this.mAddingToTask);
    }

    protected static boolean clearFrpRestricted(Context context, int userId) {
        return Secure.putIntForUser(context.getContentResolver(), SUW_FRP_STATE, 0, userId);
    }

    protected static boolean isFrpRestricted(Context context, int userId) {
        return Secure.getIntForUser(context.getContentResolver(), SUW_FRP_STATE, 0, userId) == 1;
    }
}
