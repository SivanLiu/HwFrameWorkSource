package com.android.server.am;

import android.app.ActivityManagerInternal.SleepToken;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Trace;
import android.util.HwPCUtils;
import android.util.Slog;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;

class KeyguardController {
    private static final String TAG = "ActivityManager";
    private int mBeforeUnoccludeTransit;
    private boolean mDismissalRequested;
    private ActivityRecord mDismissingKeyguardActivity;
    private boolean mKeyguardGoingAway;
    private boolean mKeyguardShowing;
    private boolean mOccluded;
    private int mSecondaryDisplayShowing = -1;
    private final ActivityManagerService mService;
    private SleepToken mSleepToken;
    private final ActivityStackSupervisor mStackSupervisor;
    private int mVisibilityTransactionDepth;
    private WindowManagerService mWindowManager;

    KeyguardController(ActivityManagerService service, ActivityStackSupervisor stackSupervisor) {
        this.mService = service;
        this.mStackSupervisor = stackSupervisor;
    }

    void setWindowManager(WindowManagerService windowManager) {
        this.mWindowManager = windowManager;
    }

    boolean isKeyguardShowing(int displayId) {
        if (!this.mKeyguardShowing || (this.mKeyguardGoingAway ^ 1) == 0) {
            return false;
        }
        if (displayId == 0) {
            return this.mOccluded ^ 1;
        }
        if (displayId == this.mSecondaryDisplayShowing) {
            return true;
        }
        return false;
    }

    boolean isKeyguardLocked() {
        return this.mKeyguardShowing ? this.mKeyguardGoingAway ^ 1 : false;
    }

    void setKeyguardShown(boolean showing, int secondaryDisplayShowing) {
        boolean showingChanged = showing != this.mKeyguardShowing;
        if (showingChanged || secondaryDisplayShowing != this.mSecondaryDisplayShowing) {
            this.mKeyguardShowing = showing;
            this.mSecondaryDisplayShowing = secondaryDisplayShowing;
            if (showingChanged) {
                dismissDockedStackIfNeeded();
                if (showing) {
                    setKeyguardGoingAway(false);
                    this.mDismissalRequested = false;
                }
            }
            this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
            updateKeyguardSleepToken();
        }
    }

    void keyguardGoingAway(int flags) {
        if (this.mKeyguardShowing) {
            Trace.traceBegin(64, "keyguardGoingAway");
            this.mWindowManager.deferSurfaceLayout();
            try {
                setKeyguardGoingAway(true);
                this.mWindowManager.prepareAppTransition(20, false, convertTransitFlags(flags), false);
                updateKeyguardSleepToken();
                this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                this.mStackSupervisor.addStartingWindowsForVisibleActivities(true);
                this.mWindowManager.executeAppTransition();
            } finally {
                Trace.traceBegin(64, "keyguardGoingAway: surfaceLayout");
                this.mWindowManager.continueSurfaceLayout();
                Trace.traceEnd(64);
                Trace.traceEnd(64);
            }
        }
    }

    void dismissKeyguard(IBinder token, IKeyguardDismissCallback callback) {
        ActivityRecord activityRecord = ActivityRecord.forTokenLocked(token);
        if (activityRecord == null || (activityRecord.visibleIgnoringKeyguard ^ 1) != 0) {
            failCallback(callback);
            return;
        }
        Slog.i(TAG, "Activity requesting to dismiss Keyguard: " + activityRecord);
        if (activityRecord.getTurnScreenOnFlag() && activityRecord.isTopRunningActivity()) {
            this.mStackSupervisor.wakeUp("dismissKeyguard");
        }
        this.mWindowManager.dismissKeyguard(callback);
    }

    private void setKeyguardGoingAway(boolean keyguardGoingAway) {
        this.mKeyguardGoingAway = keyguardGoingAway;
        this.mWindowManager.setKeyguardGoingAway(keyguardGoingAway);
    }

    private void failCallback(IKeyguardDismissCallback callback) {
        try {
            callback.onDismissError();
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to call callback", e);
        }
    }

    private int convertTransitFlags(int keyguardGoingAwayFlags) {
        int result = 0;
        if ((keyguardGoingAwayFlags & 1) != 0) {
            result = 1;
        }
        if ((keyguardGoingAwayFlags & 2) != 0) {
            result |= 2;
        }
        if ((keyguardGoingAwayFlags & 4) != 0) {
            return result | 4;
        }
        return result;
    }

    void beginActivityVisibilityUpdate() {
        this.mVisibilityTransactionDepth++;
    }

    void endActivityVisibilityUpdate() {
        this.mVisibilityTransactionDepth--;
        if (this.mVisibilityTransactionDepth == 0) {
            visibilitiesUpdated();
        }
    }

    boolean canShowActivityWhileKeyguardShowing(ActivityRecord r, boolean dismissKeyguard) {
        if (dismissKeyguard && canDismissKeyguard()) {
            return this.mDismissalRequested || r != this.mDismissingKeyguardActivity;
        } else {
            return false;
        }
    }

    boolean canShowWhileOccluded(boolean dismissKeyguard, boolean showWhenLocked) {
        if (showWhenLocked) {
            return true;
        }
        return dismissKeyguard ? this.mWindowManager.isKeyguardSecure() ^ 1 : false;
    }

    private void visibilitiesUpdated() {
        boolean lastOccluded = this.mOccluded;
        ActivityRecord lastDismissingKeyguardActivity = this.mDismissingKeyguardActivity;
        this.mOccluded = false;
        this.mDismissingKeyguardActivity = null;
        ArrayList<ActivityStack> stacks = (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()) ? this.mStackSupervisor.getStacks() : this.mStackSupervisor.getStacksOnDefaultDisplay();
        for (int stackNdx = stacks.size() - 1; stackNdx >= 0; stackNdx--) {
            ActivityStack stack = (ActivityStack) stacks.get(stackNdx);
            if (this.mStackSupervisor.isFocusedStack(stack)) {
                boolean z;
                ActivityRecord topDismissing = stack.getTopDismissingKeyguardActivity();
                if (stack.topActivityOccludesKeyguard()) {
                    z = true;
                } else if (topDismissing == null || stack.topRunningActivityLocked() != topDismissing) {
                    z = false;
                } else {
                    z = canShowWhileOccluded(true, false);
                }
                this.mOccluded = z;
            }
            if (this.mDismissingKeyguardActivity == null && stack.getTopDismissingKeyguardActivity() != null) {
                this.mDismissingKeyguardActivity = stack.getTopDismissingKeyguardActivity();
            }
        }
        this.mOccluded |= this.mWindowManager.isShowingDream();
        if (this.mOccluded != lastOccluded) {
            handleOccludedChanged();
        }
        if (this.mDismissingKeyguardActivity != lastDismissingKeyguardActivity) {
            handleDismissKeyguard();
        }
    }

    private void handleOccludedChanged() {
        Slog.d(TAG, "handleOccludedChanged mOccluded: " + this.mOccluded);
        this.mWindowManager.onKeyguardOccludedChanged(this.mOccluded);
        if (isKeyguardLocked()) {
            this.mWindowManager.deferSurfaceLayout();
            try {
                this.mWindowManager.prepareAppTransition(resolveOccludeTransit(), false, 0, true);
                updateKeyguardSleepToken();
                this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                this.mWindowManager.executeAppTransition();
            } finally {
                this.mWindowManager.continueSurfaceLayout();
            }
        }
        dismissDockedStackIfNeeded();
    }

    private void handleDismissKeyguard() {
        if (!this.mOccluded && this.mDismissingKeyguardActivity != null && this.mWindowManager.isKeyguardSecure()) {
            Slog.d(TAG, "handleDismissKeyguard");
            this.mWindowManager.dismissKeyguard(null);
            this.mDismissalRequested = true;
            if (this.mKeyguardShowing && canDismissKeyguard() && this.mWindowManager.getPendingAppTransition() == 23) {
                this.mWindowManager.prepareAppTransition(this.mBeforeUnoccludeTransit, false, 0, true);
                this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                this.mWindowManager.executeAppTransition();
            }
        }
    }

    boolean canDismissKeyguard() {
        return !this.mWindowManager.isKeyguardTrusted() ? this.mWindowManager.isKeyguardSecure() ^ 1 : true;
    }

    private int resolveOccludeTransit() {
        if (this.mBeforeUnoccludeTransit != -1 && this.mWindowManager.getPendingAppTransition() == 23 && this.mOccluded) {
            return this.mBeforeUnoccludeTransit;
        }
        if (this.mOccluded) {
            return 22;
        }
        this.mBeforeUnoccludeTransit = this.mWindowManager.getPendingAppTransition();
        return 23;
    }

    private void dismissDockedStackIfNeeded() {
        if (this.mKeyguardShowing && this.mOccluded) {
            this.mStackSupervisor.moveTasksToFullscreenStackLocked(3, this.mStackSupervisor.mFocusedStack.getStackId() == 3);
        }
    }

    private void updateKeyguardSleepToken() {
        if (this.mSleepToken == null && isKeyguardShowing(0)) {
            this.mSleepToken = this.mService.acquireSleepToken("Keyguard", 0);
        } else if (this.mSleepToken != null && (isKeyguardShowing(0) ^ 1) != 0) {
            this.mSleepToken.release();
            this.mSleepToken = null;
        }
    }

    void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "KeyguardController:");
        pw.println(prefix + "  mKeyguardShowing=" + this.mKeyguardShowing);
        pw.println(prefix + "  mKeyguardGoingAway=" + this.mKeyguardGoingAway);
        pw.println(prefix + "  mOccluded=" + this.mOccluded);
        pw.println(prefix + "  mDismissingKeyguardActivity=" + this.mDismissingKeyguardActivity);
        pw.println(prefix + "  mDismissalRequested=" + this.mDismissalRequested);
        pw.println(prefix + "  mVisibilityTransactionDepth=" + this.mVisibilityTransactionDepth);
    }
}
