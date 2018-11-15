package com.android.server.am;

import android.app.ActivityOptions;
import android.app.AppOpsManager;
import android.app.IAssistDataReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Slog;
import android.view.IRecentsAnimationRunner;
import com.android.server.wm.RecentsAnimationController.RecentsAnimationCallbacks;
import com.android.server.wm.RecentsAnimationController.ReorderMode;
import com.android.server.wm.WindowManagerService;

class RecentsAnimation implements RecentsAnimationCallbacks, OnStackOrderChangedListener {
    private static final boolean DEBUG = false;
    private static final String LEGACY_RECENTS_PACKAGE_NAME_LAUNCHER = "com.huawei.android.launcher.quickstep.RecentsActivity";
    private static final String TAG = RecentsAnimation.class.getSimpleName();
    private final ActivityStartController mActivityStartController;
    private AssistDataRequester mAssistDataRequester;
    private final int mCallingPid;
    private final ActivityDisplay mDefaultDisplay;
    private ActivityStack mRestoreTargetBehindStack;
    private final ActivityManagerService mService;
    private final ActivityStackSupervisor mStackSupervisor;
    private int mTargetActivityType;
    private final UserController mUserController;
    private final WindowManagerService mWindowManager;

    RecentsAnimation(ActivityManagerService am, ActivityStackSupervisor stackSupervisor, ActivityStartController activityStartController, WindowManagerService wm, UserController userController, int callingPid) {
        this.mService = am;
        this.mStackSupervisor = stackSupervisor;
        this.mDefaultDisplay = stackSupervisor.getDefaultDisplay();
        this.mActivityStartController = activityStartController;
        this.mWindowManager = wm;
        this.mUserController = userController;
        this.mCallingPid = callingPid;
    }

    /* JADX WARNING: Removed duplicated region for block: B:19:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0064  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x006b  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x0103 A:{Catch:{ Exception -> 0x0129, all -> 0x0124 }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x009f A:{SYNTHETIC, Splitter: B:32:0x009f} */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x012f A:{Catch:{ Exception -> 0x0129, all -> 0x0124 }} */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x0107 A:{Catch:{ Exception -> 0x0129, all -> 0x0124 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void startRecentsActivity(Intent intent, IRecentsAnimationRunner recentsAnimationRunner, ComponentName recentsComponent, int recentsUid, IAssistDataReceiver assistDataReceiver) {
        ActivityRecord targetActivity;
        Exception e;
        int i;
        Throwable th;
        Intent intent2 = intent;
        IRecentsAnimationRunner iRecentsAnimationRunner = recentsAnimationRunner;
        IAssistDataReceiver iAssistDataReceiver = assistDataReceiver;
        Trace.traceBegin(64, "RecentsAnimation#startRecentsActivity");
        if (this.mWindowManager.canStartRecentsAnimation()) {
            int i2;
            ActivityStack targetStack;
            ActivityRecord targetActivity2;
            boolean hasExistingActivity;
            int i3;
            if (intent.getComponent() != null) {
                if (recentsComponent.equals(intent.getComponent())) {
                    ActivityStack targetStack2;
                    i2 = 3;
                    this.mTargetActivityType = i2;
                    if (intent.getComponent() != null && LEGACY_RECENTS_PACKAGE_NAME_LAUNCHER.equals(intent.getComponent().getClassName())) {
                        this.mTargetActivityType = 3;
                    }
                    targetStack = this.mDefaultDisplay.getStack(0, this.mTargetActivityType);
                    targetActivity2 = getTargetActivity(targetStack, intent.getComponent());
                    hasExistingActivity = targetActivity2 == null;
                    if (hasExistingActivity) {
                        this.mRestoreTargetBehindStack = targetActivity2.getDisplay().getStackAbove(targetStack);
                        if (this.mRestoreTargetBehindStack == null) {
                            notifyAnimationCancelBeforeStart(iRecentsAnimationRunner);
                            return;
                        }
                    }
                    if (targetActivity2 == null || !targetActivity2.visible) {
                        this.mStackSupervisor.sendPowerHintForLaunchStartIfNeeded(true, targetActivity2);
                    }
                    this.mStackSupervisor.getActivityMetricsLogger().notifyActivityLaunching();
                    this.mService.setRunningRemoteAnimation(this.mCallingPid, true);
                    this.mWindowManager.deferSurfaceLayout();
                    if (iAssistDataReceiver == null) {
                        AppOpsManager appOpsManager;
                        AssistDataReceiverProxy proxy;
                        AssistDataRequester assistDataRequester;
                        AssistDataRequester assistDataRequester2;
                        try {
                            appOpsManager = (AppOpsManager) this.mService.mContext.getSystemService("appops");
                            proxy = new AssistDataReceiverProxy(iAssistDataReceiver, recentsComponent.getPackageName());
                            assistDataRequester = assistDataRequester;
                            assistDataRequester2 = assistDataRequester;
                            targetActivity = targetActivity2;
                            targetStack2 = targetStack;
                        } catch (Exception e2) {
                            e = e2;
                            targetActivity = targetActivity2;
                            targetStack2 = targetStack;
                            i = recentsUid;
                            try {
                                Slog.e(TAG, "Failed to start recents activity", e);
                                throw e;
                            } catch (Throwable th2) {
                                th = th2;
                                targetActivity = targetActivity2;
                                this.mWindowManager.continueSurfaceLayout();
                                Trace.traceEnd(64);
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            targetActivity = targetActivity2;
                            targetStack2 = targetStack;
                            i = recentsUid;
                            this.mWindowManager.continueSurfaceLayout();
                            Trace.traceEnd(64);
                            throw th;
                        }
                        try {
                            assistDataRequester = new AssistDataRequester(this.mService.mContext, this.mService, this.mWindowManager, appOpsManager, proxy, this, 49, -1);
                            this.mAssistDataRequester = assistDataRequester2;
                            this.mAssistDataRequester.requestAssistData(this.mStackSupervisor.getTopVisibleActivities(), true, false, true, false, recentsUid, recentsComponent.getPackageName());
                        } catch (Exception e3) {
                            e = e3;
                            i = recentsUid;
                            targetActivity2 = targetActivity;
                            Slog.e(TAG, "Failed to start recents activity", e);
                            throw e;
                        } catch (Throwable th4) {
                            th = th4;
                            i = recentsUid;
                            this.mWindowManager.continueSurfaceLayout();
                            Trace.traceEnd(64);
                            throw th;
                        }
                    }
                    targetActivity = targetActivity2;
                    targetStack2 = targetStack;
                    if (hasExistingActivity) {
                        ActivityOptions options = ActivityOptions.makeBasic();
                        options.setLaunchActivityType(this.mTargetActivityType);
                        options.setAvoidMoveToFront();
                        intent2.addFlags(268500992);
                        try {
                            this.mActivityStartController.obtainStarter(intent2, "startRecentsActivity_noTargetActivity").setCallingUid(recentsUid).setCallingPackage(recentsComponent.getPackageName()).setActivityOptions(SafeActivityOptions.fromBundle(options.toBundle())).setMayWait(this.mUserController.getCurrentUserId()).execute();
                            i3 = 0;
                            this.mWindowManager.prepareAppTransition(0, false);
                            this.mWindowManager.executeAppTransition();
                            targetActivity = this.mDefaultDisplay.getStack(0, this.mTargetActivityType).getTopActivity();
                        } catch (Exception e4) {
                            e = e4;
                            targetActivity2 = targetActivity;
                            Slog.e(TAG, "Failed to start recents activity", e);
                            throw e;
                        } catch (Throwable th5) {
                            th = th5;
                            this.mWindowManager.continueSurfaceLayout();
                            Trace.traceEnd(64);
                            throw th;
                        }
                    }
                    this.mDefaultDisplay.moveStackBehindBottomMostVisibleStack(targetStack2);
                    if (targetStack2.topTask() != targetActivity.getTask()) {
                        targetStack2.addTask(targetActivity.getTask(), true, "startRecentsActivity");
                    }
                    i = recentsUid;
                    i3 = 0;
                    targetActivity.mLaunchTaskBehind = true;
                    this.mWindowManager.cancelRecentsAnimationSynchronously(2, "startRecentsActivity");
                    this.mWindowManager.initializeRecentsAnimation(this.mTargetActivityType, iRecentsAnimationRunner, this, this.mDefaultDisplay.mDisplayId, this.mStackSupervisor.mRecentTasks.getRecentTaskIds());
                    this.mStackSupervisor.ensureActivitiesVisibleLocked(null, i3, true);
                    this.mStackSupervisor.getActivityMetricsLogger().notifyActivityLaunched(2, targetActivity);
                    this.mDefaultDisplay.registerStackOrderChangedListener(this);
                    this.mWindowManager.continueSurfaceLayout();
                    Trace.traceEnd(64);
                    return;
                }
            }
            ComponentName componentName = recentsComponent;
            i2 = 2;
            this.mTargetActivityType = i2;
            this.mTargetActivityType = 3;
            targetStack = this.mDefaultDisplay.getStack(0, this.mTargetActivityType);
            targetActivity2 = getTargetActivity(targetStack, intent.getComponent());
            if (targetActivity2 == null) {
            }
            hasExistingActivity = targetActivity2 == null;
            if (hasExistingActivity) {
            }
            this.mStackSupervisor.sendPowerHintForLaunchStartIfNeeded(true, targetActivity2);
            this.mStackSupervisor.getActivityMetricsLogger().notifyActivityLaunching();
            this.mService.setRunningRemoteAnimation(this.mCallingPid, true);
            this.mWindowManager.deferSurfaceLayout();
            if (iAssistDataReceiver == null) {
            }
            if (hasExistingActivity) {
            }
            targetActivity.mLaunchTaskBehind = true;
            this.mWindowManager.cancelRecentsAnimationSynchronously(2, "startRecentsActivity");
            this.mWindowManager.initializeRecentsAnimation(this.mTargetActivityType, iRecentsAnimationRunner, this, this.mDefaultDisplay.mDisplayId, this.mStackSupervisor.mRecentTasks.getRecentTaskIds());
            this.mStackSupervisor.ensureActivitiesVisibleLocked(null, i3, true);
            this.mStackSupervisor.getActivityMetricsLogger().notifyActivityLaunched(2, targetActivity);
            this.mDefaultDisplay.registerStackOrderChangedListener(this);
            this.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64);
            return;
        }
        notifyAnimationCancelBeforeStart(iRecentsAnimationRunner);
    }

    private void finishAnimation(@ReorderMode int reorderMode) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mAssistDataRequester != null) {
                    this.mAssistDataRequester.cancel();
                    this.mAssistDataRequester = null;
                }
                this.mDefaultDisplay.unregisterStackOrderChangedListener(this);
                if (this.mWindowManager.getRecentsAnimationController() == null) {
                } else {
                    if (reorderMode != 0) {
                        this.mStackSupervisor.sendPowerHintForLaunchEndIfNeeded();
                    }
                    this.mService.setRunningRemoteAnimation(this.mCallingPid, false);
                    this.mWindowManager.inSurfaceTransaction(new -$$Lambda$RecentsAnimation$Zj0-OCbCxGCeVS-UKZSU82iNyXc(this, reorderMode));
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public static /* synthetic */ void lambda$finishAnimation$0(RecentsAnimation recentsAnimation, int reorderMode) {
        Trace.traceBegin(64, "RecentsAnimation#onAnimationFinished_inSurfaceTransaction");
        recentsAnimation.mWindowManager.deferSurfaceLayout();
        try {
            ActivityRecord targetActivity;
            recentsAnimation.mWindowManager.cleanupRecentsAnimation(reorderMode);
            ActivityStack targetStack = recentsAnimation.mDefaultDisplay.getStack(0, recentsAnimation.mTargetActivityType);
            if (targetStack != null) {
                targetActivity = targetStack.getTopActivity();
            } else {
                targetActivity = null;
            }
            if (targetActivity == null) {
                recentsAnimation.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
                return;
            }
            targetActivity.mLaunchTaskBehind = false;
            if (reorderMode == 1) {
                recentsAnimation.mStackSupervisor.mNoAnimActivities.add(targetActivity);
                targetStack.moveToFront("RecentsAnimation.onAnimationFinished()");
            } else if (reorderMode == 2) {
                targetActivity.getDisplay().moveStackBehindStack(targetStack, recentsAnimation.mRestoreTargetBehindStack);
            } else {
                recentsAnimation.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
                return;
            }
            recentsAnimation.mWindowManager.prepareAppTransition(0, false);
            recentsAnimation.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
            recentsAnimation.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            recentsAnimation.mWindowManager.executeAppTransition();
            recentsAnimation.mWindowManager.checkSplitScreenMinimizedChanged(true);
            recentsAnimation.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to clean up recents activity", e);
            throw e;
        } catch (Throwable th) {
            recentsAnimation.mWindowManager.continueSurfaceLayout();
            Trace.traceEnd(64);
        }
    }

    public void onAnimationFinished(@ReorderMode int reorderMode, boolean runSychronously) {
        if (runSychronously) {
            finishAnimation(reorderMode);
        } else {
            this.mService.mHandler.post(new -$$Lambda$RecentsAnimation$1UHkVDWv9CBej8qt8TWQICpmP60(this, reorderMode));
        }
    }

    public void onStackOrderChanged() {
        this.mWindowManager.cancelRecentsAnimationSynchronously(0, "stackOrderChanged");
    }

    private void notifyAnimationCancelBeforeStart(IRecentsAnimationRunner recentsAnimationRunner) {
        try {
            recentsAnimationRunner.onAnimationCanceled();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to cancel recents animation before start", e);
        }
    }

    private ActivityStack getTopNonAlwaysOnTopStack() {
        for (int i = this.mDefaultDisplay.getChildCount() - 1; i >= 0; i--) {
            ActivityStack s = this.mDefaultDisplay.getChildAt(i);
            if (!s.getWindowConfiguration().isAlwaysOnTop()) {
                return s;
            }
        }
        return null;
    }

    private ActivityRecord getTargetActivity(ActivityStack targetStack, ComponentName component) {
        if (targetStack == null) {
            return null;
        }
        for (int i = targetStack.getChildCount() - 1; i >= 0; i--) {
            TaskRecord task = (TaskRecord) targetStack.getChildAt(i);
            if (task.getBaseIntent().getComponent().equals(component)) {
                return task.getTopActivity();
            }
        }
        if (component == null || !LEGACY_RECENTS_PACKAGE_NAME_LAUNCHER.equals(component.getClassName())) {
            return targetStack.getTopActivity();
        }
        return null;
    }
}
