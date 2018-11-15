package com.android.server.am;

import android.app.ActivityManager.RecentTaskInfo;
import android.app.IAppTask.Stub;
import android.app.IApplicationThread;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import com.android.server.pm.DumpState;

class AppTaskImpl extends Stub {
    private int mCallingUid;
    private ActivityManagerService mService;
    private int mTaskId;

    public AppTaskImpl(ActivityManagerService service, int taskId, int callingUid) {
        this.mService = service;
        this.mTaskId = taskId;
        this.mCallingUid = callingUid;
    }

    private void checkCaller() {
        if (this.mCallingUid != Binder.getCallingUid()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Caller ");
            stringBuilder.append(this.mCallingUid);
            stringBuilder.append(" does not match caller of getAppTasks(): ");
            stringBuilder.append(Binder.getCallingUid());
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public void finishAndRemoveTask() {
        checkCaller();
        synchronized (this.mService) {
            long origId;
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                origId = Binder.clearCallingIdentity();
                if (this.mService.mStackSupervisor.removeTaskByIdLocked(this.mTaskId, false, true, "finish-and-remove-task")) {
                    Binder.restoreCallingIdentity(origId);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to find task ID ");
                    stringBuilder.append(this.mTaskId);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    public RecentTaskInfo getTaskInfo() {
        RecentTaskInfo createRecentTaskInfo;
        checkCaller();
        synchronized (this.mService) {
            long origId;
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                origId = Binder.clearCallingIdentity();
                TaskRecord tr = this.mService.mStackSupervisor.anyTaskForIdLocked(this.mTaskId, 1);
                if (tr != null) {
                    createRecentTaskInfo = this.mService.getRecentTasks().createRecentTaskInfo(tr);
                    Binder.restoreCallingIdentity(origId);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to find task ID ");
                    stringBuilder.append(this.mTaskId);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return createRecentTaskInfo;
    }

    public void moveToFront() {
        checkCaller();
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mService) {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.mStackSupervisor.startActivityFromRecents(callingPid, callingUid, this.mTaskId, null);
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            Binder.restoreCallingIdentity(origId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int startActivity(IBinder whoThread, String callingPackage, Intent intent, String resolvedType, Bundle bOptions) {
        TaskRecord tr;
        IApplicationThread appThread;
        checkCaller();
        int callingUser = UserHandle.getCallingUserId();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                tr = this.mService.mStackSupervisor.anyTaskForIdLocked(this.mTaskId, 1);
                if (tr != null) {
                    appThread = IApplicationThread.Stub.asInterface(whoThread);
                    if (appThread != null) {
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Bad app thread ");
                        stringBuilder.append(appThread);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to find task ID ");
                stringBuilder2.append(this.mTaskId);
                throw new IllegalArgumentException(stringBuilder2.toString());
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        return this.mService.getActivityStartController().obtainStarter(intent, "AppTaskImpl").setCaller(appThread).setCallingPackage(callingPackage).setResolvedType(resolvedType).setActivityOptions(bOptions).setMayWait(callingUser).setInTask(tr).execute();
    }

    public void setExcludeFromRecents(boolean exclude) {
        checkCaller();
        synchronized (this.mService) {
            long origId;
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                origId = Binder.clearCallingIdentity();
                TaskRecord tr = this.mService.mStackSupervisor.anyTaskForIdLocked(this.mTaskId, 1);
                if (tr != null) {
                    Intent intent = tr.getBaseIntent();
                    if (exclude) {
                        intent.addFlags(DumpState.DUMP_VOLUMES);
                    } else {
                        intent.setFlags(intent.getFlags() & -8388609);
                    }
                    Binder.restoreCallingIdentity(origId);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to find task ID ");
                    stringBuilder.append(this.mTaskId);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }
}
