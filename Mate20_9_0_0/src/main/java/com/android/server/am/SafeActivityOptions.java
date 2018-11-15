package com.android.server.am;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.util.Slog;
import android.view.RemoteAnimationAdapter;
import com.android.internal.annotations.VisibleForTesting;

class SafeActivityOptions {
    private static final String TAG = "ActivityManager";
    private ActivityOptions mCallerOptions;
    private final int mOriginalCallingPid = Binder.getCallingPid();
    private final int mOriginalCallingUid = Binder.getCallingUid();
    private final ActivityOptions mOriginalOptions;
    private int mRealCallingPid;
    private int mRealCallingUid;

    static SafeActivityOptions fromBundle(Bundle bOptions) {
        if (bOptions != null) {
            return new SafeActivityOptions(ActivityOptions.fromBundle(bOptions));
        }
        return null;
    }

    SafeActivityOptions(ActivityOptions options) {
        this.mOriginalOptions = options;
    }

    void setCallerOptions(ActivityOptions options) {
        this.mRealCallingPid = Binder.getCallingPid();
        this.mRealCallingUid = Binder.getCallingUid();
        this.mCallerOptions = options;
    }

    ActivityOptions getOptions(ActivityRecord r) throws SecurityException {
        return getOptions(r.intent, r.info, r.app, r.mStackSupervisor);
    }

    ActivityOptions getOptions(ActivityStackSupervisor supervisor) throws SecurityException {
        return getOptions(null, null, null, supervisor);
    }

    ActivityOptions getOptions(Intent intent, ActivityInfo aInfo, ProcessRecord callerApp, ActivityStackSupervisor supervisor) throws SecurityException {
        if (this.mOriginalOptions != null) {
            checkPermissions(intent, aInfo, callerApp, supervisor, this.mOriginalOptions, this.mOriginalCallingPid, this.mOriginalCallingUid);
            setCallingPidForRemoteAnimationAdapter(this.mOriginalOptions, this.mOriginalCallingPid);
        }
        if (this.mCallerOptions != null) {
            checkPermissions(intent, aInfo, callerApp, supervisor, this.mCallerOptions, this.mRealCallingPid, this.mRealCallingUid);
            setCallingPidForRemoteAnimationAdapter(this.mCallerOptions, this.mRealCallingPid);
        }
        return mergeActivityOptions(this.mOriginalOptions, this.mCallerOptions);
    }

    private void setCallingPidForRemoteAnimationAdapter(ActivityOptions options, int callingPid) {
        RemoteAnimationAdapter adapter = options.getRemoteAnimationAdapter();
        if (adapter != null) {
            if (callingPid == Process.myPid()) {
                Slog.wtf("ActivityManager", "Safe activity options constructed after clearing calling id");
            } else {
                adapter.setCallingPid(callingPid);
            }
        }
    }

    Bundle popAppVerificationBundle() {
        return this.mOriginalOptions != null ? this.mOriginalOptions.popAppVerificationBundle() : null;
    }

    private void abort() {
        if (this.mOriginalOptions != null) {
            ActivityOptions.abort(this.mOriginalOptions);
        }
        if (this.mCallerOptions != null) {
            ActivityOptions.abort(this.mCallerOptions);
        }
    }

    static void abort(SafeActivityOptions options) {
        if (options != null) {
            options.abort();
        }
    }

    @VisibleForTesting
    ActivityOptions mergeActivityOptions(ActivityOptions options1, ActivityOptions options2) {
        if (options1 == null) {
            return options2;
        }
        if (options2 == null) {
            return options1;
        }
        Bundle b1 = options1.toBundle();
        b1.putAll(options2.toBundle());
        return ActivityOptions.fromBundle(b1);
    }

    private void checkPermissions(Intent intent, ActivityInfo aInfo, ProcessRecord callerApp, ActivityStackSupervisor supervisor, ActivityOptions options, int callingPid, int callingUid) {
        String msg;
        if (options.getLaunchTaskId() == -1 || supervisor.mRecentTasks.isCallerRecents(callingUid) || supervisor.mService.checkPermission("android.permission.START_TASKS_FROM_RECENTS", callingPid, callingUid) != -1) {
            int launchDisplayId = options.getLaunchDisplayId();
            if (aInfo == null || launchDisplayId == -1 || supervisor.isCallerAllowedToLaunchOnDisplay(callingPid, callingUid, launchDisplayId, aInfo)) {
                boolean lockTaskMode = options.getLockTaskMode();
                if (aInfo != null && lockTaskMode && !supervisor.mService.getLockTaskController().isPackageWhitelisted(UserHandle.getUserId(callingUid), aInfo.packageName)) {
                    String msg2 = new StringBuilder();
                    msg2.append("Permission Denial: starting ");
                    msg2.append(getIntentString(intent));
                    msg2.append(" from ");
                    msg2.append(callerApp);
                    msg2.append(" (pid=");
                    msg2.append(callingPid);
                    msg2.append(", uid=");
                    msg2.append(callingUid);
                    msg2.append(") with lockTaskMode=true");
                    msg2 = msg2.toString();
                    Slog.w("ActivityManager", msg2);
                    throw new SecurityException(msg2);
                } else if (options.getRemoteAnimationAdapter() != null && supervisor.mService.checkPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", callingPid, callingUid) != 0) {
                    String msg3 = new StringBuilder();
                    msg3.append("Permission Denial: starting ");
                    msg3.append(getIntentString(intent));
                    msg3.append(" from ");
                    msg3.append(callerApp);
                    msg3.append(" (pid=");
                    msg3.append(callingPid);
                    msg3.append(", uid=");
                    msg3.append(callingUid);
                    msg3.append(") with remoteAnimationAdapter");
                    msg3 = msg3.toString();
                    Slog.w("ActivityManager", msg3);
                    throw new SecurityException(msg3);
                } else {
                    return;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Permission Denial: starting ");
            stringBuilder.append(getIntentString(intent));
            stringBuilder.append(" from ");
            stringBuilder.append(callerApp);
            stringBuilder.append(" (pid=");
            stringBuilder.append(callingPid);
            stringBuilder.append(", uid=");
            stringBuilder.append(callingUid);
            stringBuilder.append(") with launchDisplayId=");
            stringBuilder.append(launchDisplayId);
            msg = stringBuilder.toString();
            Slog.w("ActivityManager", msg);
            throw new SecurityException(msg);
        }
        msg = new StringBuilder();
        msg.append("Permission Denial: starting ");
        msg.append(getIntentString(intent));
        msg.append(" from ");
        msg.append(callerApp);
        msg.append(" (pid=");
        msg.append(callingPid);
        msg.append(", uid=");
        msg.append(callingUid);
        msg.append(") with launchTaskId=");
        msg.append(options.getLaunchTaskId());
        msg = msg.toString();
        Slog.w("ActivityManager", msg);
        throw new SecurityException(msg);
    }

    private String getIntentString(Intent intent) {
        return intent != null ? intent.toString() : "(no intent)";
    }
}
