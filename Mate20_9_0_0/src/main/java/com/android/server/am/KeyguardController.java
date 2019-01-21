package com.android.server.am;

import android.app.ActivityManagerInternal.SleepToken;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;

class KeyguardController {
    private static final String TAG = "ActivityManager";
    private boolean mAodShowing;
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

    boolean isKeyguardOrAodShowing(int displayId) {
        return (this.mKeyguardShowing || this.mAodShowing) && !this.mKeyguardGoingAway && (displayId != 0 ? displayId != this.mSecondaryDisplayShowing : this.mOccluded);
    }

    boolean isKeyguardShowing(int displayId) {
        return this.mKeyguardShowing && !this.mKeyguardGoingAway && (displayId != 0 ? displayId != this.mSecondaryDisplayShowing : this.mOccluded);
    }

    boolean isKeyguardLocked() {
        return this.mKeyguardShowing && !this.mKeyguardGoingAway;
    }

    boolean isKeyguardGoingAway() {
        return this.mKeyguardGoingAway && this.mKeyguardShowing;
    }

    void setKeyguardShown(boolean keyguardShowing, boolean aodShowing, int secondaryDisplayShowing) {
        int i = 1;
        boolean showingChanged = (keyguardShowing == this.mKeyguardShowing && aodShowing == this.mAodShowing) ? false : true;
        if (!(this.mKeyguardGoingAway && keyguardShowing)) {
            i = 0;
        }
        showingChanged |= i;
        if (showingChanged || secondaryDisplayShowing != this.mSecondaryDisplayShowing) {
            if (ActivityManagerDebugConfig.DEBUG_KEYGUARD) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("last mKeyguardShowing:");
                stringBuilder.append(this.mKeyguardShowing);
                stringBuilder.append(" mAodShowing:");
                stringBuilder.append(this.mAodShowing);
                stringBuilder.append(" mSecondaryDisplayShowing:");
                stringBuilder.append(this.mSecondaryDisplayShowing);
                Flog.i(107, stringBuilder.toString());
            }
            this.mKeyguardShowing = keyguardShowing;
            this.mAodShowing = aodShowing;
            this.mSecondaryDisplayShowing = secondaryDisplayShowing;
            this.mWindowManager.setAodShowing(aodShowing);
            if (showingChanged) {
                dismissDockedStackIfNeeded();
                setKeyguardGoingAway(false);
                this.mWindowManager.setKeyguardOrAodShowingOnDefaultDisplay(isKeyguardOrAodShowing(0));
                if (keyguardShowing) {
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
                this.mStackSupervisor.resumeFocusedStackTopActivityLocked();
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

    void dismissKeyguard(IBinder token, IKeyguardDismissCallback callback, CharSequence message) {
        ActivityRecord activityRecord = ActivityRecord.forTokenLocked(token);
        if (activityRecord == null || !activityRecord.visibleIgnoringKeyguard) {
            failCallback(callback);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Activity requesting to dismiss Keyguard: ");
        stringBuilder.append(activityRecord);
        Flog.i(107, stringBuilder.toString());
        if (activityRecord.getTurnScreenOnFlag() && activityRecord.isTopRunningActivity()) {
            this.mStackSupervisor.wakeUp("dismissKeyguard");
        }
        this.mWindowManager.dismissKeyguard(callback, message);
    }

    private void setKeyguardGoingAway(boolean keyguardGoingAway) {
        if (ActivityManagerDebugConfig.DEBUG_KEYGUARD) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("change mKeyguardGoingAway from:");
            stringBuilder.append(this.mKeyguardGoingAway);
            stringBuilder.append(" to:");
            stringBuilder.append(keyguardGoingAway);
            Flog.i(107, stringBuilder.toString());
        }
        this.mKeyguardGoingAway = keyguardGoingAway;
        this.mWindowManager.setKeyguardGoingAway(keyguardGoingAway);
    }

    private void failCallback(IKeyguardDismissCallback callback) {
        try {
            callback.onDismissError();
        } catch (RemoteException e) {
            Slog.w("ActivityManager", "Failed to call callback", e);
        }
    }

    private int convertTransitFlags(int keyguardGoingAwayFlags) {
        int result = 0;
        if ((keyguardGoingAwayFlags & 1) != 0) {
            result = 0 | 1;
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
        return dismissKeyguard && canDismissKeyguard() && !this.mAodShowing && (this.mDismissalRequested || r != this.mDismissingKeyguardActivity);
    }

    boolean canShowWhileOccluded(boolean dismissKeyguard, boolean showWhenLocked) {
        return showWhenLocked || (dismissKeyguard && !this.mWindowManager.isKeyguardSecure());
    }

    private void visibilitiesUpdated() {
        boolean lastOccluded = this.mOccluded;
        ActivityRecord lastDismissingKeyguardActivity = this.mDismissingKeyguardActivity;
        this.mOccluded = false;
        this.mDismissingKeyguardActivity = null;
        boolean isPadPcCastModeInServer = HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer();
        for (int displayNdx = this.mStackSupervisor.getChildCount() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay display = this.mStackSupervisor.getChildAt(displayNdx);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = display.getChildAt(stackNdx);
                if ((display.mDisplayId == 0 || isPadPcCastModeInServer) && this.mStackSupervisor.isFocusedStack(stack)) {
                    ActivityRecord topDismissing = stack.getTopDismissingKeyguardActivity();
                    boolean z = stack.topActivityOccludesKeyguard() || (topDismissing != null && stack.topRunningActivityLocked() == topDismissing && canShowWhileOccluded(true, false));
                    this.mOccluded = z;
                    if (ActivityManagerDebugConfig.DEBUG_KEYGUARD) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("topDismissing:");
                        stringBuilder.append(topDismissing);
                        stringBuilder.append(" mOccluded:");
                        stringBuilder.append(this.mOccluded);
                        Flog.i(107, stringBuilder.toString());
                    }
                }
                if (this.mDismissingKeyguardActivity == null && stack.getTopDismissingKeyguardActivity() != null) {
                    this.mDismissingKeyguardActivity = stack.getTopDismissingKeyguardActivity();
                }
            }
        }
        this.mOccluded |= this.mWindowManager.isShowingDream();
        if (this.mOccluded != lastOccluded) {
            handleOccludedChanged();
        }
        if (ActivityManagerDebugConfig.DEBUG_KEYGUARD && this.mDismissingKeyguardActivity != null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("dismissingKeyguardActivity:");
            stringBuilder2.append(this.mDismissingKeyguardActivity);
            Flog.i(107, stringBuilder2.toString());
        }
        if (this.mDismissingKeyguardActivity != lastDismissingKeyguardActivity) {
            handleDismissKeyguard();
        }
    }

    private void handleOccludedChanged() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleOccludedChanged mOccluded: ");
        stringBuilder.append(this.mOccluded);
        Flog.i(107, stringBuilder.toString());
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
            Flog.i(107, "handleDismissKeyguard");
            this.mWindowManager.dismissKeyguard(null, null);
            this.mDismissalRequested = true;
            if (this.mKeyguardShowing && canDismissKeyguard() && this.mWindowManager.getPendingAppTransition() == 23) {
                this.mWindowManager.prepareAppTransition(this.mBeforeUnoccludeTransit, false, 0, true);
                this.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
                this.mWindowManager.executeAppTransition();
            }
        }
    }

    boolean canDismissKeyguard() {
        return this.mWindowManager.isKeyguardTrusted() || !this.mWindowManager.isKeyguardSecure();
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
            ActivityStack stack = this.mStackSupervisor.getDefaultDisplay().getSplitScreenPrimaryStack();
            if (stack != null) {
                this.mStackSupervisor.moveTasksToFullscreenStackLocked(stack, this.mStackSupervisor.mFocusedStack == stack);
            }
        }
    }

    private void updateKeyguardSleepToken() {
        if (this.mSleepToken == null && isKeyguardOrAodShowing(0)) {
            if (ActivityManagerDebugConfig.DEBUG_KEYGUARD) {
                Flog.i(107, "acquireSleepToken");
            }
            this.mSleepToken = this.mService.acquireSleepToken("Keyguard", 0);
        } else if (this.mSleepToken != null && !isKeyguardOrAodShowing(0)) {
            if (ActivityManagerDebugConfig.DEBUG_KEYGUARD) {
                Flog.i(107, "releaseSleepToken");
            }
            this.mSleepToken.release();
            this.mSleepToken = null;
        }
    }

    void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("KeyguardController:");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mKeyguardShowing=");
        stringBuilder.append(this.mKeyguardShowing);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mAodShowing=");
        stringBuilder.append(this.mAodShowing);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mKeyguardGoingAway=");
        stringBuilder.append(this.mKeyguardGoingAway);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mOccluded=");
        stringBuilder.append(this.mOccluded);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mDismissingKeyguardActivity=");
        stringBuilder.append(this.mDismissingKeyguardActivity);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mDismissalRequested=");
        stringBuilder.append(this.mDismissalRequested);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mVisibilityTransactionDepth=");
        stringBuilder.append(this.mVisibilityTransactionDepth);
        pw.println(stringBuilder.toString());
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1133871366145L, this.mKeyguardShowing);
        proto.write(1133871366146L, this.mOccluded);
        proto.end(token);
    }
}
