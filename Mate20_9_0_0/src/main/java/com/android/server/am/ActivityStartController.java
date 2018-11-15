package com.android.server.am;

import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.Slog;
import android.view.IApplicationToken.Stub;
import android.view.RemoteAnimationAdapter;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.HwServiceFactory;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ActivityStartController {
    private static final int DO_PENDING_ACTIVITY_LAUNCHES_MSG = 1;
    private static final String TAG = "ActivityManager";
    String mCurActivityPkName;
    private final Factory mFactory;
    private final Handler mHandler;
    private ActivityRecord mLastHomeActivityStartRecord;
    private int mLastHomeActivityStartResult;
    private ActivityStarter mLastStarter;
    private final ArrayList<PendingActivityLaunch> mPendingActivityLaunches;
    private final PendingRemoteAnimationRegistry mPendingRemoteAnimationRegistry;
    private final ActivityManagerService mService;
    private final ActivityStackSupervisor mSupervisor;
    private ActivityRecord[] tmpOutRecord;

    private final class StartHandler extends Handler {
        public StartHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                synchronized (ActivityStartController.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        ActivityStartController.this.doPendingActivityLaunches(true);
                    } finally {
                        while (true) {
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                }
            }
        }
    }

    ActivityStartController(ActivityManagerService service) {
        this(service, service.mStackSupervisor, new DefaultFactory(service, service.mStackSupervisor, HwServiceFactory.createActivityStartInterceptor(service, service.mStackSupervisor)));
    }

    @VisibleForTesting
    ActivityStartController(ActivityManagerService service, ActivityStackSupervisor supervisor, Factory factory) {
        this.tmpOutRecord = new ActivityRecord[1];
        this.mPendingActivityLaunches = new ArrayList();
        this.mCurActivityPkName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        this.mService = service;
        this.mSupervisor = supervisor;
        this.mHandler = new StartHandler(this.mService.mHandlerThread.getLooper());
        this.mFactory = factory;
        this.mFactory.setController(this);
        this.mPendingRemoteAnimationRegistry = new PendingRemoteAnimationRegistry(service, service.mHandler);
    }

    ActivityStarter obtainStarter(Intent intent, String reason) {
        return this.mFactory.obtain().setIntent(intent).setReason(reason);
    }

    void onExecutionComplete(ActivityStarter starter) {
        if (this.mLastStarter == null) {
            this.mLastStarter = this.mFactory.obtain();
        }
        this.mLastStarter.set(starter);
        this.mFactory.recycle(starter);
    }

    void postStartActivityProcessingForLastStarter(ActivityRecord r, int result, ActivityStack targetStack) {
        if (this.mLastStarter != null) {
            this.mLastStarter.postStartActivityProcessing(r, result, targetStack);
        }
    }

    void startHomeActivity(Intent intent, ActivityInfo aInfo, String reason) {
        this.mSupervisor.moveHomeStackTaskToTop(reason);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startHomeActivity: ");
        stringBuilder.append(reason);
        this.mLastHomeActivityStartResult = obtainStarter(intent, stringBuilder.toString()).setOutActivity(this.tmpOutRecord).setCallingUid(0).setActivityInfo(aInfo).execute();
        this.mLastHomeActivityStartRecord = this.tmpOutRecord[0];
        if (this.mSupervisor.inResumeTopActivity) {
            this.mSupervisor.scheduleResumeTopActivities();
        }
    }

    void startSetupActivity() {
        if (!this.mService.getCheckedForSetup()) {
            ContentResolver resolver = this.mService.mContext.getContentResolver();
            if (!(this.mService.mFactoryTest == 1 || Global.getInt(resolver, "device_provisioned", 0) == 0)) {
                this.mService.setCheckedForSetup(true);
                Intent intent = new Intent("android.intent.action.UPGRADE_SETUP");
                List<ResolveInfo> ris = this.mService.mContext.getPackageManager().queryIntentActivities(intent, 1049728);
                if (!ris.isEmpty()) {
                    String vers;
                    ResolveInfo ri = (ResolveInfo) ris.get(0);
                    if (ri.activityInfo.metaData != null) {
                        vers = ri.activityInfo.metaData.getString("android.SETUP_VERSION");
                    } else {
                        vers = null;
                    }
                    if (vers == null && ri.activityInfo.applicationInfo.metaData != null) {
                        vers = ri.activityInfo.applicationInfo.metaData.getString("android.SETUP_VERSION");
                    }
                    String lastVers = Secure.getString(resolver, "last_setup_shown");
                    if (!(vers == null || vers.equals(lastVers))) {
                        intent.setFlags(268435456);
                        intent.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));
                        obtainStarter(intent, "startSetupActivity").setCallingUid(0).setActivityInfo(ri.activityInfo).execute();
                    }
                }
            }
        }
    }

    int checkTargetUser(int targetUserId, boolean validateIncomingUser, int realCallingPid, int realCallingUid, String reason) {
        if (validateIncomingUser) {
            return this.mService.mUserController.handleIncomingUser(realCallingPid, realCallingUid, targetUserId, false, 2, reason, null);
        }
        this.mService.mUserController.ensureNotSpecialUser(targetUserId);
        return targetUserId;
    }

    final int startActivityInPackage(int uid, int realCallingPid, int realCallingUid, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, SafeActivityOptions options, int userId, TaskRecord inTask, String reason, boolean validateIncomingUser) {
        return obtainStarter(intent, reason).setCallingUid(uid).setRealCallingPid(realCallingPid).setRealCallingUid(realCallingUid).setCallingPackage(callingPackage).setResolvedType(resolvedType).setResultTo(resultTo).setResultWho(resultWho).setRequestCode(requestCode).setStartFlags(startFlags).setActivityOptions(options).setMayWait(checkTargetUser(userId, validateIncomingUser, realCallingPid, realCallingUid, reason)).setInTask(inTask).execute();
    }

    final int startActivitiesInPackage(int uid, String callingPackage, Intent[] intents, String[] resolvedTypes, IBinder resultTo, SafeActivityOptions options, int userId, boolean validateIncomingUser) {
        String reason = "startActivityInPackage";
        return startActivities(null, uid, callingPackage, intents, resolvedTypes, resultTo, options, checkTargetUser(userId, validateIncomingUser, Binder.getCallingPid(), Binder.getCallingUid(), "startActivityInPackage"), "startActivityInPackage");
    }

    int startActivities(IApplicationThread caller, int callingUid, String callingPackage, Intent[] intents, String[] resolvedTypes, IBinder resultTo, SafeActivityOptions options, int userId, String reason) {
        long origId;
        Throwable th;
        ActivityStartController outActivity = this;
        long origId2 = caller;
        Intent[] intentArr = intents;
        String[] strArr = resolvedTypes;
        String str;
        if (intentArr == null) {
            str = reason;
            throw new NullPointerException("intents is null");
        } else if (strArr == null) {
            str = reason;
            throw new NullPointerException("resolvedTypes is null");
        } else if (intentArr.length == strArr.length) {
            int callingPid;
            int callingUid2;
            int realCallingPid = Binder.getCallingPid();
            int realCallingUid = Binder.getCallingUid();
            if (callingUid >= 0) {
                callingPid = -1;
                callingUid2 = callingUid;
            } else if (origId2 == null) {
                callingPid = realCallingPid;
                callingUid2 = realCallingUid;
            } else {
                callingUid2 = -1;
                callingPid = -1;
            }
            long origId3 = Binder.clearCallingIdentity();
            IBinder resultTo2;
            try {
                synchronized (outActivity.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        boolean z = true;
                        ActivityRecord[] outActivity2 = new ActivityRecord[1];
                        resultTo2 = resultTo;
                        int i = 0;
                        while (i < intentArr.length) {
                            try {
                                ActivityRecord[] outActivity3;
                                Intent intent = intentArr[i];
                                if (intent == null) {
                                    origId2 = origId3;
                                    outActivity3 = outActivity2;
                                    outActivity2 = reason;
                                } else {
                                    if (intent != null) {
                                        try {
                                            if (intent.hasFileDescriptors()) {
                                                origId = origId3;
                                                try {
                                                    throw new IllegalArgumentException("File descriptors passed in Intent");
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    str = reason;
                                                    origId2 = origId;
                                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                            }
                                            origId = origId3;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            str = reason;
                                            origId2 = origId3;
                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                    }
                                    origId = origId3;
                                    boolean componentSpecified = intent.getComponent() != null ? z : false;
                                    intent = new Intent(intent);
                                    outActivity.mService.addCallerToIntent(intent, origId2);
                                    ActivityRecord[] outActivity4 = outActivity2;
                                    ActivityInfo aInfo = outActivity.mService.getActivityInfoForUser(outActivity.mSupervisor.resolveActivity(intent, strArr[i], 0, null, userId, ActivityStarter.computeResolveFilterUid(callingUid2, realCallingUid, -10000)), userId);
                                    if (aInfo != null) {
                                        if ((aInfo.applicationInfo.privateFlags & 2) != 0) {
                                            throw new IllegalArgumentException("FLAG_CANT_SAVE_STATE not supported here");
                                        }
                                    }
                                    try {
                                        z = i == intentArr.length - 1;
                                        try {
                                            SafeActivityOptions checkedOptions = z ? options : null;
                                            outActivity3 = outActivity4;
                                            int res = outActivity.obtainStarter(intent, reason).setCaller(origId2).setResolvedType(strArr[i]).setActivityInfo(aInfo).setResultTo(resultTo2).setRequestCode(-1).setCallingPid(callingPid).setCallingUid(callingUid2).setCallingPackage(callingPackage).setRealCallingPid(realCallingPid).setRealCallingUid(realCallingUid).setActivityOptions(checkedOptions).setComponentSpecified(componentSpecified).setOutActivity(outActivity3).setAllowPendingRemoteAnimationRegistryLookup(z).execute();
                                            if (res < 0) {
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                                Binder.restoreCallingIdentity(origId);
                                                return res;
                                            }
                                            Stub stub;
                                            origId2 = origId;
                                            if (outActivity3[0] != null) {
                                                stub = outActivity3[0].appToken;
                                            } else {
                                                stub = null;
                                            }
                                            resultTo2 = stub;
                                        } catch (Throwable th4) {
                                            th = th4;
                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                        str = reason;
                                    }
                                }
                                i++;
                                outActivity2 = outActivity3;
                                origId3 = origId2;
                                outActivity = this;
                                origId2 = caller;
                                intentArr = intents;
                                z = true;
                            } catch (Throwable th6) {
                                th = th6;
                                str = reason;
                                origId2 = origId3;
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        str = reason;
                        origId2 = origId3;
                        try {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            Binder.restoreCallingIdentity(origId2);
                            return 0;
                        } catch (Throwable th7) {
                            th = th7;
                            Binder.restoreCallingIdentity(origId2);
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        str = reason;
                        origId2 = origId3;
                        resultTo2 = resultTo;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            } catch (Throwable th9) {
                th = th9;
                str = reason;
                origId2 = origId3;
                resultTo2 = resultTo;
                Binder.restoreCallingIdentity(origId2);
                throw th;
            }
        } else {
            str = reason;
            throw new IllegalArgumentException("intents are length different than resolvedTypes");
        }
    }

    void schedulePendingActivityLaunches(long delayMs) {
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), delayMs);
    }

    void doPendingActivityLaunches(boolean doResume) {
        while (!this.mPendingActivityLaunches.isEmpty()) {
            boolean z = false;
            PendingActivityLaunch pal = (PendingActivityLaunch) this.mPendingActivityLaunches.remove(0);
            if (doResume && this.mPendingActivityLaunches.isEmpty()) {
                z = true;
            }
            boolean resume = z;
            try {
                obtainStarter(null, "pendingActivityLaunch").startResolvedActivity(pal.r, pal.sourceRecord, null, null, pal.startFlags, resume, null, null, null);
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception during pending activity launch pal=");
                stringBuilder.append(pal);
                Slog.e("ActivityManager", stringBuilder.toString(), e);
                pal.sendErrorResult(e.getMessage());
            }
        }
    }

    void addPendingActivityLaunch(PendingActivityLaunch launch) {
        this.mPendingActivityLaunches.add(launch);
    }

    boolean clearPendingActivityLaunches(String packageName) {
        int pendingLaunches = this.mPendingActivityLaunches.size();
        for (int palNdx = pendingLaunches - 1; palNdx >= 0; palNdx--) {
            ActivityRecord r = ((PendingActivityLaunch) this.mPendingActivityLaunches.get(palNdx)).r;
            if (r != null && r.packageName.equals(packageName)) {
                this.mPendingActivityLaunches.remove(palNdx);
            }
        }
        return this.mPendingActivityLaunches.size() < pendingLaunches;
    }

    void registerRemoteAnimationForNextActivityStart(String packageName, RemoteAnimationAdapter adapter) {
        this.mPendingRemoteAnimationRegistry.addPendingAnimation(packageName, adapter);
    }

    PendingRemoteAnimationRegistry getPendingRemoteAnimationRegistry() {
        return this.mPendingRemoteAnimationRegistry;
    }

    void dump(PrintWriter pw, String prefix, String dumpPackage) {
        pw.print(prefix);
        pw.print("mLastHomeActivityStartResult=");
        pw.println(this.mLastHomeActivityStartResult);
        if (this.mLastHomeActivityStartRecord != null) {
            pw.print(prefix);
            pw.println("mLastHomeActivityStartRecord:");
            ActivityRecord activityRecord = this.mLastHomeActivityStartRecord;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            activityRecord.dump(pw, stringBuilder.toString());
        }
        boolean dump = false;
        boolean dumpPackagePresent = dumpPackage != null;
        if (this.mLastStarter != null) {
            if (!dumpPackagePresent || this.mLastStarter.relatedToPackage(dumpPackage) || (this.mLastHomeActivityStartRecord != null && dumpPackage.equals(this.mLastHomeActivityStartRecord.packageName))) {
                dump = true;
            }
            if (dump) {
                pw.print(prefix);
                ActivityStarter activityStarter = this.mLastStarter;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append("  ");
                activityStarter.dump(pw, stringBuilder2.toString());
                if (dumpPackagePresent) {
                    return;
                }
            }
        }
        if (dumpPackagePresent) {
            pw.print(prefix);
            pw.println("(nothing)");
        }
    }
}
