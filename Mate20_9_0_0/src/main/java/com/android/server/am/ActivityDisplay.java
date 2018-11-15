package com.android.server.am;

import android.app.ActivityManagerInternal.SleepToken;
import android.app.ActivityOptions;
import android.app.WindowConfiguration;
import android.graphics.Point;
import android.util.HwPCUtils;
import android.util.IntArray;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.HwServiceFactory;
import com.android.server.wm.ConfigurationContainer;
import com.android.server.wm.DisplayWindowController;
import com.android.server.wm.WindowContainerListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class ActivityDisplay extends ConfigurationContainer<ActivityStack> implements WindowContainerListener {
    static final int POSITION_BOTTOM = Integer.MIN_VALUE;
    static final int POSITION_TOP = Integer.MAX_VALUE;
    private static final String TAG = "ActivityManager";
    private static final String TAG_KEYGUARD = "ActivityManager_keyguard";
    private static final String TAG_STACK = "ActivityManager";
    private static int sNextFreeStackId = 0;
    final ArrayList<SleepToken> mAllSleepTokens;
    Display mDisplay;
    private IntArray mDisplayAccessUIDs;
    int mDisplayId;
    private ActivityStack mHomeStack;
    SleepToken mOffToken;
    private ActivityStack mPinnedStack;
    private ActivityStack mRecentsStack;
    private boolean mSleeping;
    private ActivityStack mSplitScreenPrimaryStack;
    private ArrayList<OnStackOrderChangedListener> mStackOrderChangedCallbacks;
    final ArrayList<ActivityStack> mStacks;
    private ActivityStackSupervisor mSupervisor;
    private Point mTmpDisplaySize;
    private DisplayWindowController mWindowContainerController;

    interface OnStackOrderChangedListener {
        void onStackOrderChanged();
    }

    @VisibleForTesting
    ActivityDisplay(ActivityStackSupervisor supervisor, int displayId) {
        this(supervisor, supervisor.mDisplayManager.getDisplay(displayId));
    }

    ActivityDisplay(ActivityStackSupervisor supervisor, Display display) {
        this.mStacks = new ArrayList();
        this.mStackOrderChangedCallbacks = new ArrayList();
        this.mDisplayAccessUIDs = new IntArray();
        this.mAllSleepTokens = new ArrayList();
        this.mHomeStack = null;
        this.mRecentsStack = null;
        this.mPinnedStack = null;
        this.mSplitScreenPrimaryStack = null;
        this.mTmpDisplaySize = new Point();
        this.mSupervisor = supervisor;
        this.mDisplayId = display.getDisplayId();
        this.mDisplay = display;
        this.mWindowContainerController = createWindowContainerController();
        updateBounds();
    }

    protected DisplayWindowController createWindowContainerController() {
        return new DisplayWindowController(this.mDisplay, this);
    }

    void updateBounds() {
        this.mDisplay.getSize(this.mTmpDisplaySize);
        setBounds(0, 0, this.mTmpDisplaySize.x, this.mTmpDisplaySize.y);
    }

    void addChild(ActivityStack stack, int position) {
        if (position == Integer.MIN_VALUE) {
            position = 0;
        } else if (position == Integer.MAX_VALUE) {
            position = this.mStacks.size();
        }
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addChild: attaching ");
            stringBuilder.append(stack);
            stringBuilder.append(" to displayId=");
            stringBuilder.append(this.mDisplayId);
            stringBuilder.append(" position=");
            stringBuilder.append(position);
            Slog.v(str, stringBuilder.toString());
        }
        addStackReferenceIfNeeded(stack);
        positionChildAt(stack, position);
        this.mSupervisor.mService.updateSleepIfNeededLocked();
    }

    void removeChild(ActivityStack stack) {
        if (ActivityManagerDebugConfig.DEBUG_STACK) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeChild: detaching ");
            stringBuilder.append(stack);
            stringBuilder.append(" from displayId=");
            stringBuilder.append(this.mDisplayId);
            Slog.v(str, stringBuilder.toString());
        }
        this.mStacks.remove(stack);
        removeStackReferenceIfNeeded(stack);
        this.mSupervisor.mService.updateSleepIfNeededLocked();
        onStackOrderChanged();
    }

    void positionChildAtTop(ActivityStack stack) {
        positionChildAt(stack, this.mStacks.size());
    }

    void positionChildAtBottom(ActivityStack stack) {
        positionChildAt(stack, 0);
    }

    private void positionChildAt(ActivityStack stack, int position) {
        this.mStacks.remove(stack);
        int insertPosition = getTopInsertPosition(stack, position);
        this.mStacks.add(insertPosition, stack);
        this.mWindowContainerController.positionChildAt(stack.getWindowContainerController(), insertPosition);
        onStackOrderChanged();
    }

    private int getTopInsertPosition(ActivityStack stack, int candidatePosition) {
        int position = this.mStacks.size();
        if (position > 0) {
            ActivityStack topStack = (ActivityStack) this.mStacks.get(position - 1);
            if (topStack.getWindowConfiguration().isAlwaysOnTop() && topStack != stack) {
                position--;
            }
        }
        return Math.min(position, candidatePosition);
    }

    <T extends ActivityStack> T getStack(int stackId) {
        for (int i = this.mStacks.size() - 1; i >= 0; i--) {
            ActivityStack stack = (ActivityStack) this.mStacks.get(i);
            if (stack.mStackId == stackId) {
                return stack;
            }
        }
        return null;
    }

    <T extends ActivityStack> T getStack(int windowingMode, int activityType) {
        if (activityType == 2) {
            return this.mHomeStack;
        }
        if (activityType == 3) {
            return this.mRecentsStack;
        }
        if (windowingMode == 2) {
            return this.mPinnedStack;
        }
        if (windowingMode == 3) {
            return this.mSplitScreenPrimaryStack;
        }
        for (int i = this.mStacks.size() - 1; i >= 0; i--) {
            ActivityStack stack = (ActivityStack) this.mStacks.get(i);
            if (stack.isCompatible(windowingMode, activityType)) {
                return stack;
            }
        }
        return null;
    }

    private boolean alwaysCreateStack(int windowingMode, int activityType) {
        return activityType == 1 && (windowingMode == 1 || windowingMode == 5 || windowingMode == 4 || windowingMode == 10);
    }

    <T extends ActivityStack> T getOrCreateStack(int windowingMode, int activityType, boolean onTop) {
        if (!alwaysCreateStack(windowingMode, activityType)) {
            T stack = getStack(windowingMode, activityType);
            if (stack != null) {
                return stack;
            }
        }
        return createStack(windowingMode, activityType, onTop);
    }

    <T extends ActivityStack> T getOrCreateStack(ActivityRecord r, ActivityOptions options, TaskRecord candidateTask, int activityType, boolean onTop) {
        return getOrCreateStack(resolveWindowingMode(r, options, candidateTask, activityType), activityType, onTop);
    }

    private int getNextStackId() {
        int i = sNextFreeStackId;
        sNextFreeStackId = i + 1;
        return i;
    }

    <T extends ActivityStack> T createStack(int windowingMode, int activityType, boolean onTop) {
        if (activityType == 0) {
            activityType = 1;
        }
        if (activityType != 1) {
            T stack = getStack(null, activityType);
            if (stack != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Stack=");
                stringBuilder.append(stack);
                stringBuilder.append(" of activityType=");
                stringBuilder.append(activityType);
                stringBuilder.append(" already on display=");
                stringBuilder.append(this);
                stringBuilder.append(". Can't have multiple.");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        ActivityManagerService service = this.mSupervisor.mService;
        if (isWindowingModeSupported(windowingMode, service.mSupportsMultiWindow, service.mSupportsSplitScreenMultiWindow, service.mSupportsFreeformWindowManagement, service.mSupportsPictureInPicture, activityType)) {
            if (windowingMode == 0) {
                windowingMode = getWindowingMode();
                if (windowingMode == 0) {
                    windowingMode = 1;
                }
            }
            return createStackUnchecked(windowingMode, activityType, getNextStackId(), onTop);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Can't create stack for unsupported windowingMode=");
        stringBuilder2.append(windowingMode);
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    @VisibleForTesting
    <T extends ActivityStack> T createStackUnchecked(int windowingMode, int activityType, int stackId, boolean onTop) {
        if (windowingMode == 2) {
            return new PinnedActivityStack(this, stackId, this.mSupervisor, onTop);
        }
        return HwServiceFactory.createActivityStack(this, stackId, this.mSupervisor, windowingMode, activityType, onTop);
    }

    void removeStacksInWindowingModes(int... windowingModes) {
        if (windowingModes != null && windowingModes.length != 0) {
            for (int j = windowingModes.length - 1; j >= 0; j--) {
                int windowingMode = windowingModes[j];
                for (int i = this.mStacks.size() - 1; i >= 0; i--) {
                    ActivityStack stack = (ActivityStack) this.mStacks.get(i);
                    if (stack.isActivityTypeStandardOrUndefined() && stack.getWindowingMode() == windowingMode) {
                        this.mSupervisor.removeStack(stack);
                    }
                }
            }
        }
    }

    void removeStacksWithActivityTypes(int... activityTypes) {
        if (activityTypes != null && activityTypes.length != 0) {
            for (int j = activityTypes.length - 1; j >= 0; j--) {
                int activityType = activityTypes[j];
                for (int i = this.mStacks.size() - 1; i >= 0; i--) {
                    ActivityStack stack = (ActivityStack) this.mStacks.get(i);
                    if (stack.getActivityType() == activityType) {
                        this.mSupervisor.removeStack(stack);
                    }
                }
            }
        }
    }

    void onStackWindowingModeChanged(ActivityStack stack) {
        removeStackReferenceIfNeeded(stack);
        addStackReferenceIfNeeded(stack);
    }

    private void addStackReferenceIfNeeded(ActivityStack stack) {
        StringBuilder stringBuilder;
        int activityType = stack.getActivityType();
        int windowingMode = stack.getWindowingMode();
        if (activityType == 2) {
            if (this.mHomeStack == null || this.mHomeStack == stack) {
                this.mHomeStack = stack;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("addStackReferenceIfNeeded: home stack=");
                stringBuilder.append(this.mHomeStack);
                stringBuilder.append(" already exist on display=");
                stringBuilder.append(this);
                stringBuilder.append(" stack=");
                stringBuilder.append(stack);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        } else if (activityType == 3) {
            if (this.mRecentsStack == null || this.mRecentsStack == stack) {
                this.mRecentsStack = stack;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("addStackReferenceIfNeeded: recents stack=");
                stringBuilder.append(this.mRecentsStack);
                stringBuilder.append(" already exist on display=");
                stringBuilder.append(this);
                stringBuilder.append(" stack=");
                stringBuilder.append(stack);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (windowingMode == 2) {
            if (this.mPinnedStack == null || this.mPinnedStack == stack) {
                this.mPinnedStack = stack;
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("addStackReferenceIfNeeded: pinned stack=");
            stringBuilder.append(this.mPinnedStack);
            stringBuilder.append(" already exist on display=");
            stringBuilder.append(this);
            stringBuilder.append(" stack=");
            stringBuilder.append(stack);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (windowingMode != 3) {
        } else {
            if (this.mSplitScreenPrimaryStack == null || this.mSplitScreenPrimaryStack == stack) {
                this.mSplitScreenPrimaryStack = stack;
                onSplitScreenModeActivated();
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("addStackReferenceIfNeeded: split-screen-primary stack=");
            stringBuilder.append(this.mSplitScreenPrimaryStack);
            stringBuilder.append(" already exist on display=");
            stringBuilder.append(this);
            stringBuilder.append(" stack=");
            stringBuilder.append(stack);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private void removeStackReferenceIfNeeded(ActivityStack stack) {
        if (stack == this.mHomeStack) {
            this.mHomeStack = null;
        } else if (stack == this.mRecentsStack) {
            this.mRecentsStack = null;
        } else if (stack == this.mPinnedStack) {
            this.mPinnedStack = null;
        } else if (stack == this.mSplitScreenPrimaryStack) {
            this.mSplitScreenPrimaryStack = null;
            onSplitScreenModeDismissed();
        }
    }

    private void onSplitScreenModeDismissed() {
        this.mSupervisor.mWindowManager.deferSurfaceLayout();
        ActivityStack topFullscreenStack;
        try {
            for (int i = this.mStacks.size() - 1; i >= 0; i--) {
                ActivityStack otherStack = (ActivityStack) this.mStacks.get(i);
                if (otherStack.inSplitScreenSecondaryWindowingMode()) {
                    otherStack.setWindowingMode(1, false, false, false, true);
                }
            }
            try {
                topFullscreenStack = getTopStackInWindowingMode(1);
                if (!(topFullscreenStack == null || this.mHomeStack == null || isTopStack(this.mHomeStack))) {
                    this.mHomeStack.moveToFront("onSplitScreenModeDismissed");
                    topFullscreenStack.moveToFront("onSplitScreenModeDismissed");
                }
                this.mSupervisor.mWindowManager.continueSurfaceLayout();
            } catch (Throwable th) {
                this.mSupervisor.mWindowManager.continueSurfaceLayout();
            }
        } catch (Throwable th2) {
            this.mSupervisor.mWindowManager.continueSurfaceLayout();
        }
    }

    private void onSplitScreenModeActivated() {
        this.mSupervisor.mWindowManager.deferSurfaceLayout();
        try {
            for (int i = this.mStacks.size() - 1; i >= 0; i--) {
                ActivityStack otherStack = (ActivityStack) this.mStacks.get(i);
                if (otherStack != this.mSplitScreenPrimaryStack && otherStack.affectedBySplitScreenResize()) {
                    otherStack.setWindowingMode(4, false, false, true, true);
                }
            }
        } finally {
            this.mSupervisor.mWindowManager.continueSurfaceLayout();
        }
    }

    private boolean isWindowingModeSupported(int windowingMode, boolean supportsMultiWindow, boolean supportsSplitScreen, boolean supportsFreeform, boolean supportsPip, int activityType) {
        boolean z = true;
        if (windowingMode == 0 || windowingMode == 1) {
            return true;
        }
        if (!supportsMultiWindow) {
            return false;
        }
        if (windowingMode == 3 || windowingMode == 4) {
            if (!(supportsSplitScreen && WindowConfiguration.supportSplitScreenWindowingMode(activityType))) {
                z = false;
            }
            return z;
        } else if (!supportsFreeform && windowingMode == 5) {
            return false;
        } else {
            if (windowingMode == 10 && !HwPCUtils.isPcCastModeInServer()) {
                return false;
            }
            if (supportsPip || windowingMode != 2) {
                return true;
            }
            return false;
        }
    }

    int resolveWindowingMode(ActivityRecord r, ActivityOptions options, TaskRecord task, int activityType) {
        int i;
        int windowingMode = options != null ? options.getLaunchWindowingMode() : 0;
        if (windowingMode == 0) {
            if (task != null) {
                windowingMode = task.getWindowingMode();
            }
            if (windowingMode == 0 && r != null) {
                windowingMode = r.getWindowingMode();
            }
            if (windowingMode == 0) {
                windowingMode = getWindowingMode();
            }
        }
        ActivityManagerService service = this.mSupervisor.mService;
        boolean supportsMultiWindow = service.mSupportsMultiWindow;
        boolean supportsSplitScreen = service.mSupportsSplitScreenMultiWindow;
        boolean supportsFreeform = service.mSupportsFreeformWindowManagement;
        boolean supportsPip = service.mSupportsPictureInPicture;
        if (supportsMultiWindow) {
            if (task != null) {
                supportsMultiWindow = task.isResizeable();
                supportsSplitScreen = task.supportsSplitScreenWindowingMode();
            } else if (r != null) {
                supportsMultiWindow = r.isResizeable();
                supportsSplitScreen = r.supportsSplitScreenWindowingMode();
                supportsFreeform = r.supportsFreeform();
                supportsPip = r.supportsPictureInPicture();
            }
        }
        boolean supportsMultiWindow2 = supportsMultiWindow;
        boolean supportsSplitScreen2 = supportsSplitScreen;
        boolean supportsFreeform2 = supportsFreeform;
        boolean supportsPip2 = supportsPip;
        boolean inSplitScreenMode = hasSplitScreenPrimaryStack();
        if (!inSplitScreenMode && windowingMode == 4) {
            windowingMode = 1;
        } else if (inSplitScreenMode && windowingMode == 1 && supportsSplitScreen2) {
            windowingMode = 4;
        }
        if (windowingMode != 0) {
            i = 1;
            if (isWindowingModeSupported(windowingMode, supportsMultiWindow2, supportsSplitScreen2, supportsFreeform2, supportsPip2, activityType)) {
                return windowingMode;
            }
        }
        i = 1;
        int windowingMode2 = getWindowingMode();
        if (windowingMode2 != 0) {
            i = windowingMode2;
        }
        return i;
    }

    ActivityStack getTopStack() {
        return this.mStacks.isEmpty() ? null : (ActivityStack) this.mStacks.get(this.mStacks.size() - 1);
    }

    boolean isTopStack(ActivityStack stack) {
        return stack == getTopStack();
    }

    boolean isTopNotPinnedStack(ActivityStack stack) {
        boolean z = true;
        int i = this.mStacks.size() - 1;
        while (i >= 0) {
            ActivityStack current = (ActivityStack) this.mStacks.get(i);
            if (current.inPinnedWindowingMode()) {
                i--;
            } else {
                if (current != stack) {
                    z = false;
                }
                return z;
            }
        }
        return false;
    }

    ActivityStack getTopStackInWindowingMode(int windowingMode) {
        for (int i = this.mStacks.size() - 1; i >= 0; i--) {
            ActivityStack current = (ActivityStack) this.mStacks.get(i);
            if (windowingMode == current.getWindowingMode()) {
                return current;
            }
        }
        return null;
    }

    int getIndexOf(ActivityStack stack) {
        return this.mStacks.indexOf(stack);
    }

    void onLockTaskPackagesUpdated() {
        for (int i = this.mStacks.size() - 1; i >= 0; i--) {
            ((ActivityStack) this.mStacks.get(i)).onLockTaskPackagesUpdated();
        }
    }

    void onExitingSplitScreenMode() {
        this.mSplitScreenPrimaryStack = null;
    }

    ActivityStack getSplitScreenPrimaryStack() {
        return this.mSplitScreenPrimaryStack;
    }

    boolean hasSplitScreenPrimaryStack() {
        return this.mSplitScreenPrimaryStack != null;
    }

    PinnedActivityStack getPinnedStack() {
        return (PinnedActivityStack) this.mPinnedStack;
    }

    boolean hasPinnedStack() {
        return this.mPinnedStack != null;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ActivityDisplay={");
        stringBuilder.append(this.mDisplayId);
        stringBuilder.append(" numStacks=");
        stringBuilder.append(this.mStacks.size());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    protected int getChildCount() {
        return this.mStacks.size();
    }

    protected ActivityStack getChildAt(int index) {
        return (ActivityStack) this.mStacks.get(index);
    }

    protected ConfigurationContainer getParent() {
        return this.mSupervisor;
    }

    boolean isPrivate() {
        return (this.mDisplay.getFlags() & 4) != 0;
    }

    boolean isUidPresent(int uid) {
        Iterator it = this.mStacks.iterator();
        while (it.hasNext()) {
            if (((ActivityStack) it.next()).isUidPresent(uid)) {
                return true;
            }
        }
        return false;
    }

    void remove() {
        boolean destroyContentOnRemoval = shouldDestroyContentOnRemove();
        while (getChildCount() > 0) {
            ActivityStack stack = getChildAt(0);
            if (destroyContentOnRemoval) {
                stack.onOverrideConfigurationChanged(stack.getConfiguration());
                this.mSupervisor.moveStackToDisplayLocked(stack.mStackId, 0, false);
                stack.finishAllActivitiesLocked(true);
            } else {
                this.mSupervisor.moveTasksToFullscreenStackLocked(stack, true);
            }
        }
        this.mWindowContainerController.removeContainer();
        this.mWindowContainerController = null;
    }

    IntArray getPresentUIDs() {
        this.mDisplayAccessUIDs.clear();
        Iterator it = this.mStacks.iterator();
        while (it.hasNext()) {
            ((ActivityStack) it.next()).getPresentUIDs(this.mDisplayAccessUIDs);
        }
        return this.mDisplayAccessUIDs;
    }

    private boolean shouldDestroyContentOnRemove() {
        return this.mDisplay.getRemoveMode() == 1;
    }

    boolean shouldSleep() {
        ActivityManagerService service = this.mSupervisor.mService;
        boolean z = false;
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && service.mHwAMSEx != null) {
            if (service.mHwAMSEx.canSleepForPCMode() && service.mRunningVoice == null) {
                z = true;
            }
            return z;
        }
        if ((this.mStacks.isEmpty() || !this.mAllSleepTokens.isEmpty()) && service.mRunningVoice == null) {
            z = true;
        }
        return z;
    }

    ActivityStack getStackAbove(ActivityStack stack) {
        int stackIndex = this.mStacks.indexOf(stack) + 1;
        return stackIndex < this.mStacks.size() ? (ActivityStack) this.mStacks.get(stackIndex) : null;
    }

    void moveStackBehindBottomMostVisibleStack(ActivityStack stack) {
        if (!stack.shouldBeVisible(null)) {
            positionChildAtBottom(stack);
            int numStacks = this.mStacks.size();
            for (int stackNdx = 0; stackNdx < numStacks; stackNdx++) {
                ActivityStack s = (ActivityStack) this.mStacks.get(stackNdx);
                if (s != stack) {
                    int winMode = s.getWindowingMode();
                    boolean isValidWindowingMode = true;
                    if (!(winMode == 1 || winMode == 4)) {
                        isValidWindowingMode = false;
                    }
                    if (s.shouldBeVisible(null) && isValidWindowingMode) {
                        positionChildAt(stack, Math.max(0, stackNdx - 1));
                        break;
                    }
                }
            }
        }
    }

    void moveStackBehindStack(ActivityStack stack, ActivityStack behindStack) {
        if (behindStack != null && behindStack != stack) {
            int stackIndex = this.mStacks.indexOf(stack);
            int behindStackIndex = this.mStacks.indexOf(behindStack);
            positionChildAt(stack, Math.max(0, stackIndex <= behindStackIndex ? behindStackIndex - 1 : behindStackIndex));
        }
    }

    boolean isSleeping() {
        return this.mSleeping;
    }

    void setIsSleeping(boolean asleep) {
        if (ActivityManagerDebugConfig.DEBUG_KEYGUARD) {
            String str = TAG_KEYGUARD;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set asleep:");
            stringBuilder.append(asleep);
            Slog.v(str, stringBuilder.toString(), new Exception());
        }
        this.mSleeping = asleep;
    }

    void registerStackOrderChangedListener(OnStackOrderChangedListener listener) {
        if (!this.mStackOrderChangedCallbacks.contains(listener)) {
            this.mStackOrderChangedCallbacks.add(listener);
        }
    }

    void unregisterStackOrderChangedListener(OnStackOrderChangedListener listener) {
        this.mStackOrderChangedCallbacks.remove(listener);
    }

    private void onStackOrderChanged() {
        for (int i = this.mStackOrderChangedCallbacks.size() - 1; i >= 0; i--) {
            ((OnStackOrderChangedListener) this.mStackOrderChangedCallbacks.get(i)).onStackOrderChanged();
        }
    }

    public void deferUpdateImeTarget() {
        if (this.mWindowContainerController != null) {
            this.mWindowContainerController.deferUpdateImeTarget();
            return;
        }
        String str = ActivityManagerService.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("controller is null when deferUpdateImeTarget for displayId:");
        stringBuilder.append(this.mDisplayId);
        Slog.e(str, stringBuilder.toString());
    }

    public void continueUpdateImeTarget() {
        if (this.mWindowContainerController != null) {
            this.mWindowContainerController.continueUpdateImeTarget();
            return;
        }
        String str = ActivityManagerService.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("controller is null when continueUpdateImeTarget for displayId:");
        stringBuilder.append(this.mDisplayId);
        Slog.e(str, stringBuilder.toString());
    }

    public void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append("displayId=");
        stringBuilder2.append(this.mDisplayId);
        stringBuilder2.append(" stacks=");
        stringBuilder2.append(this.mStacks.size());
        pw.println(stringBuilder2.toString());
        String myPrefix = new StringBuilder();
        myPrefix.append(prefix);
        myPrefix.append(" ");
        myPrefix = myPrefix.toString();
        if (this.mHomeStack != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(myPrefix);
            stringBuilder.append("mHomeStack=");
            stringBuilder.append(this.mHomeStack);
            pw.println(stringBuilder.toString());
        }
        if (this.mRecentsStack != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(myPrefix);
            stringBuilder.append("mRecentsStack=");
            stringBuilder.append(this.mRecentsStack);
            pw.println(stringBuilder.toString());
        }
        if (this.mPinnedStack != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(myPrefix);
            stringBuilder.append("mPinnedStack=");
            stringBuilder.append(this.mPinnedStack);
            pw.println(stringBuilder.toString());
        }
        if (this.mSplitScreenPrimaryStack != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(myPrefix);
            stringBuilder.append("mSplitScreenPrimaryStack=");
            stringBuilder.append(this.mSplitScreenPrimaryStack);
            pw.println(stringBuilder.toString());
        }
    }

    public void dumpStacks(PrintWriter pw) {
        for (int i = this.mStacks.size() - 1; i >= 0; i--) {
            pw.print(((ActivityStack) this.mStacks.get(i)).mStackId);
            if (i > 0) {
                pw.print(",");
            }
        }
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        super.writeToProto(proto, 1146756268033L, false);
        proto.write(1120986464258L, this.mDisplayId);
        for (int stackNdx = this.mStacks.size() - 1; stackNdx >= 0; stackNdx--) {
            ((ActivityStack) this.mStacks.get(stackNdx)).writeToProto(proto, 2246267895811L);
        }
        proto.end(token);
    }
}
