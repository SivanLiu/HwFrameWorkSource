package com.android.server.wm;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Builder;
import android.view.SurfaceControl.Transaction;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.internal.policy.DockedDividerUtils;
import com.android.server.EventLogTags;
import com.android.server.os.HwBootFail;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class TaskStack extends AbsTaskStack implements BoundsAnimationTarget {
    private static final float ADJUSTED_STACK_FRACTION_MIN = 0.3f;
    private static final float IME_ADJUST_DIM_AMOUNT = 0.25f;
    private float mAdjustDividerAmount;
    private float mAdjustImeAmount;
    private final Rect mAdjustedBounds = new Rect();
    private boolean mAdjustedForIme;
    private final AnimatingAppWindowTokenRegistry mAnimatingAppWindowTokenRegistry = new AnimatingAppWindowTokenRegistry();
    private WindowStateAnimator mAnimationBackgroundAnimator;
    private SurfaceControl mAnimationBackgroundSurface;
    private boolean mAnimationBackgroundSurfaceIsShown = false;
    private final Rect mBoundsAfterRotation = new Rect();
    private boolean mBoundsAnimating = false;
    private boolean mBoundsAnimatingRequested = false;
    private boolean mBoundsAnimatingToFullscreen = false;
    private Rect mBoundsAnimationSourceHintBounds = new Rect();
    private Rect mBoundsAnimationTarget = new Rect();
    private boolean mCancelCurrentBoundsAnimation = false;
    boolean mDeferRemoval;
    private int mDensity;
    private Dimmer mDimmer = new Dimmer(this);
    private DisplayContent mDisplayContent;
    private final int mDockedStackMinimizeThickness;
    final AppTokenList mExitingAppTokens = new AppTokenList();
    private final Rect mFullyAdjustedImeBounds = new Rect();
    private boolean mImeGoingAway;
    private WindowState mImeWin;
    private final Point mLastSurfaceSize = new Point();
    private float mMinimizeAmount;
    Rect mPreAnimationBounds = new Rect();
    private int mRotation;
    final int mStackId;
    private final Rect mTmpAdjustedBounds = new Rect();
    final AppTokenList mTmpAppTokens = new AppTokenList();
    final Rect mTmpDimBoundsRect = new Rect();
    private Rect mTmpRect = new Rect();
    private Rect mTmpRect2 = new Rect();
    private Rect mTmpRect3 = new Rect();

    public TaskStack(WindowManagerService service, int stackId, StackWindowController controller) {
        super(service);
        this.mStackId = stackId;
        setController(controller);
        this.mDockedStackMinimizeThickness = service.mContext.getResources().getDimensionPixelSize(17105036);
        EventLog.writeEvent(EventLogTags.WM_STACK_CREATED, stackId);
    }

    DisplayContent getDisplayContent() {
        return this.mDisplayContent;
    }

    Task findHomeTask() {
        if (!isActivityTypeHome() || this.mChildren.isEmpty()) {
            return null;
        }
        return (Task) this.mChildren.get(this.mChildren.size() - 1);
    }

    boolean setBounds(Rect stackBounds, SparseArray<Rect> taskBounds, SparseArray<Rect> taskTempInsetBounds) {
        setBounds(stackBounds);
        for (int taskNdx = this.mChildren.size() - 1; taskNdx >= 0; taskNdx--) {
            Task task = (Task) this.mChildren.get(taskNdx);
            task.setBounds((Rect) taskBounds.get(task.mTaskId), false);
            task.setTempInsetBounds(taskTempInsetBounds != null ? (Rect) taskTempInsetBounds.get(task.mTaskId) : null);
        }
        return true;
    }

    void prepareFreezingTaskBounds() {
        for (int taskNdx = this.mChildren.size() - 1; taskNdx >= 0; taskNdx--) {
            ((Task) this.mChildren.get(taskNdx)).prepareFreezingBounds();
        }
    }

    private void setAdjustedBounds(Rect bounds) {
        if (!this.mAdjustedBounds.equals(bounds) || isAnimatingForIme()) {
            this.mAdjustedBounds.set(bounds);
            boolean adjusted = this.mAdjustedBounds.isEmpty() ^ 1;
            Rect insetBounds = null;
            if (adjusted && isAdjustedForMinimizedDockedStack()) {
                insetBounds = getRawBounds();
            } else if (adjusted && this.mAdjustedForIme) {
                insetBounds = this.mImeGoingAway ? getRawBounds() : this.mFullyAdjustedImeBounds;
            }
            alignTasksToAdjustedBounds(adjusted ? this.mAdjustedBounds : getRawBounds(), insetBounds);
            this.mDisplayContent.setLayoutNeeded();
            updateSurfaceBounds();
        }
    }

    private void alignTasksToAdjustedBounds(Rect adjustedBounds, Rect tempInsetBounds) {
        if (!matchParentBounds()) {
            boolean alignBottom = this.mAdjustedForIme && getDockSide() == 2;
            int taskNdx = this.mChildren.size() - 1;
            while (true) {
                int taskNdx2 = taskNdx;
                if (taskNdx2 >= 0) {
                    ((Task) this.mChildren.get(taskNdx2)).alignToAdjustedBounds(adjustedBounds, tempInsetBounds, alignBottom);
                    taskNdx = taskNdx2 - 1;
                } else {
                    return;
                }
            }
        }
    }

    private void updateAnimationBackgroundBounds() {
        if (this.mAnimationBackgroundSurface != null) {
            getRawBounds(this.mTmpRect);
            Rect stackBounds = getBounds();
            getPendingTransaction().setSize(this.mAnimationBackgroundSurface, this.mTmpRect.width(), this.mTmpRect.height()).setPosition(this.mAnimationBackgroundSurface, (float) (this.mTmpRect.left - stackBounds.left), (float) (this.mTmpRect.top - stackBounds.top));
            scheduleAnimation();
        }
    }

    private void hideAnimationSurface() {
        if (this.mAnimationBackgroundSurface != null) {
            getPendingTransaction().hide(this.mAnimationBackgroundSurface);
            this.mAnimationBackgroundSurfaceIsShown = false;
            scheduleAnimation();
        }
    }

    private void showAnimationSurface(float alpha) {
        if (this.mAnimationBackgroundSurface != null) {
            getPendingTransaction().setLayer(this.mAnimationBackgroundSurface, Integer.MIN_VALUE).setAlpha(this.mAnimationBackgroundSurface, alpha).show(this.mAnimationBackgroundSurface);
            this.mAnimationBackgroundSurfaceIsShown = true;
            scheduleAnimation();
        }
    }

    public int setBounds(Rect bounds) {
        return setBounds(getOverrideBounds(), bounds);
    }

    private int setBounds(Rect existing, Rect bounds) {
        int rotation = 0;
        int density = 0;
        if (this.mDisplayContent != null) {
            this.mDisplayContent.getBounds(this.mTmpRect);
            rotation = this.mDisplayContent.getDisplayInfo().rotation;
            density = this.mDisplayContent.getDisplayInfo().logicalDensityDpi;
        }
        if (ConfigurationContainer.equivalentBounds(existing, bounds) && this.mRotation == rotation) {
            return 0;
        }
        int result = super.setBounds(bounds);
        if (this.mDisplayContent != null) {
            updateAnimationBackgroundBounds();
        }
        this.mRotation = rotation;
        this.mDensity = density;
        updateAdjustedBounds();
        updateSurfaceBounds();
        return result;
    }

    void getRawBounds(Rect out) {
        out.set(getRawBounds());
    }

    Rect getRawBounds() {
        return super.getBounds();
    }

    private boolean useCurrentBounds() {
        if (matchParentBounds() || !inSplitScreenSecondaryWindowingMode() || this.mDisplayContent == null || this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility() != null) {
            return true;
        }
        return false;
    }

    public void getBounds(Rect bounds) {
        bounds.set(getBounds());
    }

    public Rect getBounds() {
        if (!useCurrentBounds()) {
            return this.mDisplayContent.getBounds();
        }
        if (this.mAdjustedBounds.isEmpty()) {
            return super.getBounds();
        }
        return this.mAdjustedBounds;
    }

    void setAnimationFinalBounds(Rect sourceHintBounds, Rect destBounds, boolean toFullscreen) {
        this.mBoundsAnimatingRequested = true;
        this.mBoundsAnimatingToFullscreen = toFullscreen;
        if (destBounds != null) {
            this.mBoundsAnimationTarget.set(destBounds);
        } else {
            this.mBoundsAnimationTarget.setEmpty();
        }
        if (sourceHintBounds != null) {
            this.mBoundsAnimationSourceHintBounds.set(sourceHintBounds);
        } else {
            this.mBoundsAnimationSourceHintBounds.setEmpty();
        }
        this.mPreAnimationBounds.set(getRawBounds());
    }

    void getFinalAnimationBounds(Rect outBounds) {
        outBounds.set(this.mBoundsAnimationTarget);
    }

    void getFinalAnimationSourceHintBounds(Rect outBounds) {
        outBounds.set(this.mBoundsAnimationSourceHintBounds);
    }

    void getAnimationOrCurrentBounds(Rect outBounds) {
        if ((this.mBoundsAnimatingRequested || this.mBoundsAnimating) && !this.mBoundsAnimationTarget.isEmpty()) {
            getFinalAnimationBounds(outBounds);
        } else {
            getBounds(outBounds);
        }
    }

    public void getDimBounds(Rect out) {
        getBounds(out);
    }

    void updateDisplayInfo(Rect bounds) {
        if (this.mDisplayContent != null) {
            int taskNdx;
            for (taskNdx = this.mChildren.size() - 1; taskNdx >= 0; taskNdx--) {
                ((Task) this.mChildren.get(taskNdx)).updateDisplayInfo(this.mDisplayContent);
            }
            if (bounds != null) {
                setBounds(bounds);
            } else if (matchParentBounds()) {
                setBounds(null);
            } else {
                this.mTmpRect2.set(getRawBounds());
                taskNdx = this.mDisplayContent.getDisplayInfo().rotation;
                int newDensity = this.mDisplayContent.getDisplayInfo().logicalDensityDpi;
                if (this.mRotation == taskNdx && this.mDensity == newDensity) {
                    setBounds(this.mTmpRect2);
                }
            }
        }
    }

    boolean updateBoundsAfterConfigChange() {
        int i = 0;
        if (this.mDisplayContent == null) {
            return false;
        }
        if (inPinnedWindowingMode()) {
            getAnimationOrCurrentBounds(this.mTmpRect2);
            if (this.mDisplayContent.mPinnedStackControllerLocked.onTaskStackBoundsChanged(this.mTmpRect2, this.mTmpRect3)) {
                this.mBoundsAfterRotation.set(this.mTmpRect3);
                this.mBoundsAnimationTarget.setEmpty();
                this.mBoundsAnimationSourceHintBounds.setEmpty();
                this.mCancelCurrentBoundsAnimation = true;
                return true;
            }
        }
        int newRotation = getDisplayInfo().rotation;
        int newDensity = getDisplayInfo().logicalDensityDpi;
        if (this.mRotation == newRotation && this.mDensity == newDensity) {
            return false;
        }
        if (matchParentBounds()) {
            setBounds(null);
            return false;
        }
        this.mTmpRect2.set(getRawBounds());
        this.mDisplayContent.rotateBounds(this.mRotation, newRotation, this.mTmpRect2);
        if (inSplitScreenPrimaryWindowingMode()) {
            repositionPrimarySplitScreenStackAfterRotation(this.mTmpRect2);
            snapDockedStackAfterRotation(this.mTmpRect2);
            int newDockSide = getDockSide(this.mTmpRect2);
            WindowManagerService windowManagerService = this.mService;
            if (!(newDockSide == 1 || newDockSide == 2)) {
                i = 1;
            }
            windowManagerService.setDockedStackCreateStateLocked(i, null);
            this.mDisplayContent.getDockedDividerController().notifyDockSideChanged(newDockSide);
        }
        this.mBoundsAfterRotation.set(this.mTmpRect2);
        return true;
    }

    void getBoundsForNewConfiguration(Rect outBounds) {
        outBounds.set(this.mBoundsAfterRotation);
        this.mBoundsAfterRotation.setEmpty();
    }

    private void repositionPrimarySplitScreenStackAfterRotation(Rect inOutBounds) {
        int dockSide = getDockSide(inOutBounds);
        if (!this.mDisplayContent.getDockedDividerController().canPrimaryStackDockTo(dockSide)) {
            this.mDisplayContent.getBounds(this.mTmpRect);
            int movement;
            switch (DockedDividerUtils.invertDockSide(dockSide)) {
                case 1:
                    movement = inOutBounds.left;
                    inOutBounds.left -= movement;
                    inOutBounds.right -= movement;
                    break;
                case 2:
                    movement = inOutBounds.top;
                    inOutBounds.top -= movement;
                    inOutBounds.bottom -= movement;
                    break;
                case 3:
                    movement = this.mTmpRect.right - inOutBounds.right;
                    inOutBounds.left += movement;
                    inOutBounds.right += movement;
                    break;
                case 4:
                    movement = this.mTmpRect.bottom - inOutBounds.bottom;
                    inOutBounds.top += movement;
                    inOutBounds.bottom += movement;
                    break;
            }
        }
    }

    private void snapDockedStackAfterRotation(Rect outBounds) {
        int dockSide;
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        int dividerSize = this.mDisplayContent.getDockedDividerController().getContentWidth();
        int dockSide2 = getDockSide(outBounds);
        Rect rect = outBounds;
        int dividerPosition = DockedDividerUtils.calculatePositionForBounds(rect, dockSide2, dividerSize);
        int displayWidth = displayInfo.logicalWidth;
        int displayHeight = displayInfo.logicalHeight;
        int rotation = displayInfo.rotation;
        int orientation = this.mDisplayContent.getConfiguration().orientation;
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(this.mDisplayContent.getDisplayId())) {
            dockSide = dockSide2;
            dockSide2 = orientation;
            this.mService.mPolicy.getStableInsetsLw(rotation, displayWidth, displayHeight, rect, this.mDisplayContent.getDisplayId(), displayInfo.displayCutout);
        } else {
            dockSide = dockSide2;
            dockSide2 = orientation;
            this.mService.mPolicy.getStableInsetsLw(rotation, displayWidth, displayHeight, displayInfo.displayCutout, rect);
        }
        Resources resources = this.mService.mContext.getResources();
        boolean z = true;
        if (dockSide2 != 1) {
            z = false;
        }
        boolean z2 = z;
        rotation = getDockSide();
        DockedDividerUtils.calculateBoundsForPosition(new DividerSnapAlgorithm(resources, displayWidth, displayHeight, dividerSize, z2, rect, rotation, isMinimizedDockAndHomeStackResizable()).calculateNonDismissingSnapTarget(dividerPosition).position, dockSide, rect, displayInfo.logicalWidth, displayInfo.logicalHeight, dividerSize);
    }

    void addTask(Task task, int position) {
        addTask(task, position, task.showForAllUsers(), true);
    }

    void addTask(Task task, int position, boolean showForAllUsers, boolean moveParents) {
        TaskStack currentStack = task.mStack;
        if (currentStack == null || currentStack.mStackId == this.mStackId) {
            task.mStack = this;
            addChild((WindowContainer) task, null);
            positionChildAt(position, task, moveParents, showForAllUsers);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Trying to add taskId=");
        stringBuilder.append(task.mTaskId);
        stringBuilder.append(" to stackId=");
        stringBuilder.append(this.mStackId);
        stringBuilder.append(", but it is already attached to stackId=");
        stringBuilder.append(task.mStack.mStackId);
        throw new IllegalStateException(stringBuilder.toString());
    }

    void positionChildAt(int position, Task child, boolean includingParents) {
        positionChildAt(position, child, includingParents, child.showForAllUsers());
    }

    private void positionChildAt(int position, Task child, boolean includingParents, boolean showForAllUsers) {
        int targetPosition = findPositionForTask(child, position, showForAllUsers, false);
        super.positionChildAt(targetPosition, child, includingParents);
        int toTop = targetPosition == this.mChildren.size() - 1 ? 1 : 0;
        EventLog.writeEvent(EventLogTags.WM_TASK_MOVED, new Object[]{Integer.valueOf(child.mTaskId), Integer.valueOf(toTop), Integer.valueOf(targetPosition)});
    }

    private int findPositionForTask(Task task, int targetPosition, boolean showForAllUsers, boolean addingNew) {
        boolean canShowTask = showForAllUsers || this.mService.isCurrentProfileLocked(task.mUserId);
        int stackSize = this.mChildren.size();
        int minPosition = 0;
        int maxPosition = addingNew ? stackSize : stackSize - 1;
        if (canShowTask) {
            minPosition = computeMinPosition(0, stackSize);
        } else {
            maxPosition = computeMaxPosition(maxPosition);
        }
        if (targetPosition == Integer.MIN_VALUE && minPosition == 0) {
            return Integer.MIN_VALUE;
        }
        if (targetPosition == HwBootFail.STAGE_BOOT_SUCCESS) {
            if (maxPosition == (addingNew ? stackSize : stackSize - 1)) {
                return HwBootFail.STAGE_BOOT_SUCCESS;
            }
        }
        return Math.min(Math.max(targetPosition, minPosition), maxPosition);
    }

    private int computeMinPosition(int minPosition, int size) {
        while (minPosition < size) {
            Task tmpTask = (Task) this.mChildren.get(minPosition);
            boolean canShowTmpTask = tmpTask.showForAllUsers() || this.mService.isCurrentProfileLocked(tmpTask.mUserId);
            if (canShowTmpTask) {
                break;
            }
            minPosition++;
        }
        return minPosition;
    }

    private int computeMaxPosition(int maxPosition) {
        while (maxPosition > 0) {
            Task tmpTask = (Task) this.mChildren.get(maxPosition);
            boolean canShowTmpTask = tmpTask.showForAllUsers() || this.mService.isCurrentProfileLocked(tmpTask.mUserId);
            if (!canShowTmpTask) {
                break;
            }
            maxPosition--;
        }
        return maxPosition;
    }

    void removeChild(Task task) {
        super.removeChild(task);
        task.mStack = null;
        if (this.mDisplayContent != null) {
            if (this.mChildren.isEmpty()) {
                getParent().positionChildAt(Integer.MIN_VALUE, this, false);
            }
            this.mDisplayContent.setLayoutNeeded();
        }
        for (int appNdx = this.mExitingAppTokens.size() - 1; appNdx >= 0; appNdx--) {
            AppWindowToken wtoken = (AppWindowToken) this.mExitingAppTokens.get(appNdx);
            if (wtoken.getTask() == task) {
                wtoken.mIsExiting = false;
                this.mExitingAppTokens.remove(appNdx);
            }
        }
    }

    public void onConfigurationChanged(Configuration newParentConfig) {
        int prevWindowingMode = getWindowingMode();
        super.onConfigurationChanged(newParentConfig);
        updateSurfaceSize(getPendingTransaction());
        int windowingMode = getWindowingMode();
        if (this.mDisplayContent != null && prevWindowingMode != windowingMode) {
            this.mDisplayContent.onStackWindowingModeChanged(this);
            updateBoundsForWindowModeChange();
        }
    }

    private void updateSurfaceBounds() {
        updateSurfaceSize(getPendingTransaction());
        updateSurfacePosition();
        scheduleAnimation();
    }

    int getStackOutset() {
        DisplayContent displayContent = getDisplayContent();
        if (!inPinnedWindowingMode() || displayContent == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = displayContent.getDisplayMetrics();
        WindowManagerService windowManagerService = this.mService;
        return (int) Math.ceil((double) (WindowManagerService.dipToPixel(5, displayMetrics) * 2));
    }

    protected void updateSurfaceSize(Transaction transaction) {
        if (this.mSurfaceControl != null) {
            Rect stackBounds = getBounds();
            int width = stackBounds.width();
            int height = stackBounds.height();
            int outset = getStackOutset();
            width += 2 * outset;
            height += 2 * outset;
            if (!(this.mService.getLazyMode() == 0 || !inMultiWindowMode() || HwPCUtils.isPcDynamicStack(this.mStackId))) {
                width = (int) (((float) width) * 0.75f);
                height = (int) (((float) height) * 0.75f);
            }
            if (width != this.mLastSurfaceSize.x || height != this.mLastSurfaceSize.y) {
                transaction.setSize(this.mSurfaceControl, width, height);
                this.mLastSurfaceSize.set(width, height);
            }
        }
    }

    void onDisplayChanged(DisplayContent dc) {
        if (this.mDisplayContent == null) {
            this.mDisplayContent = dc;
            updateBoundsForWindowModeChange();
            Builder colorLayer = makeChildSurface(null).setColorLayer(true);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("animation background stackId=");
            stringBuilder.append(this.mStackId);
            this.mAnimationBackgroundSurface = colorLayer.setName(stringBuilder.toString()).build();
            super.onDisplayChanged(dc);
            return;
        }
        throw new IllegalStateException("onDisplayChanged: Already attached");
    }

    private void updateBoundsForWindowModeChange() {
        Rect bounds = calculateBoundsForWindowModeChange();
        if (inSplitScreenSecondaryWindowingMode()) {
            forAllWindows((Consumer) -$$Lambda$TaskStack$0Cm5zc_NsRa5nGarFvrp2KYfUYU.INSTANCE, true);
        }
        updateDisplayInfo(bounds);
        updateSurfaceBounds();
    }

    private Rect calculateBoundsForWindowModeChange() {
        boolean inSplitScreenPrimary = inSplitScreenPrimaryWindowingMode();
        TaskStack splitScreenStack = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
        if (inSplitScreenPrimary || !(splitScreenStack == null || !inSplitScreenSecondaryWindowingMode() || splitScreenStack.fillsParent())) {
            Rect bounds = new Rect();
            this.mDisplayContent.getBounds(this.mTmpRect);
            this.mTmpRect2.setEmpty();
            if (splitScreenStack != null) {
                if (inSplitScreenSecondaryWindowingMode() && this.mDisplayContent.mDividerControllerLocked.isMinimizedDock() && splitScreenStack.getTopChild() != null) {
                    ((Task) splitScreenStack.getTopChild()).getBounds(this.mTmpRect2);
                } else {
                    splitScreenStack.getRawBounds(this.mTmpRect2);
                }
            }
            getStackDockedModeBounds(this.mTmpRect, bounds, this.mTmpRect2, this.mDisplayContent.mDividerControllerLocked.getContentWidth(), this.mService.mDockedStackCreateMode == 0);
            return bounds;
        }
        if (inPinnedWindowingMode()) {
            getAnimationOrCurrentBounds(this.mTmpRect2);
            if (this.mDisplayContent.mPinnedStackControllerLocked.onTaskStackBoundsChanged(this.mTmpRect2, this.mTmpRect3)) {
                return new Rect(this.mTmpRect3);
            }
        }
        return null;
    }

    void getStackDockedModeBoundsLocked(Rect currentTempTaskBounds, Rect outStackBounds, Rect outTempTaskBounds, boolean ignoreVisibility) {
        outTempTaskBounds.setEmpty();
        if (isActivityTypeHome()) {
            Task homeTask = findHomeTask();
            if (homeTask == null || !homeTask.isResizeable()) {
                outStackBounds.setEmpty();
            } else {
                getDisplayContent().mDividerControllerLocked.getHomeStackBoundsInDockedMode(outStackBounds);
            }
            outTempTaskBounds.set(outStackBounds);
        } else if (isMinimizedDockAndHomeStackResizable() && currentTempTaskBounds != null) {
            outStackBounds.set(currentTempTaskBounds);
        } else if (!inSplitScreenWindowingMode() || this.mDisplayContent == null) {
            outStackBounds.set(getRawBounds());
        } else {
            TaskStack dockedStack = this.mDisplayContent.getSplitScreenPrimaryStackIgnoringVisibility();
            if (dockedStack == null) {
                throw new IllegalStateException("Calling getStackDockedModeBoundsLocked() when there is no docked stack.");
            } else if (ignoreVisibility || dockedStack.isVisible()) {
                int dockedSide = dockedStack.getDockSide();
                if (dockedSide == -1) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to get valid docked side for docked stack=");
                    stringBuilder.append(dockedStack);
                    Slog.e("WindowManager", stringBuilder.toString());
                    outStackBounds.set(getRawBounds());
                    return;
                }
                this.mDisplayContent.getBounds(this.mTmpRect);
                dockedStack.getRawBounds(this.mTmpRect2);
                boolean z = true;
                if (!(dockedSide == 2 || dockedSide == 1)) {
                    z = false;
                }
                Rect rect = outStackBounds;
                getStackDockedModeBounds(this.mTmpRect, rect, this.mTmpRect2, this.mDisplayContent.mDividerControllerLocked.getContentWidth(), z);
            } else {
                this.mDisplayContent.getBounds(outStackBounds);
            }
        }
    }

    private void getStackDockedModeBounds(Rect displayRect, Rect outBounds, Rect dockedBounds, int dockDividerWidth, boolean dockOnTopOrLeft) {
        Rect rect = outBounds;
        Rect rect2 = dockedBounds;
        boolean dockedStack = inSplitScreenPrimaryWindowingMode();
        boolean splitHorizontally = displayRect.width() > displayRect.height();
        rect.set(displayRect);
        if (!dockedStack) {
            if (dockOnTopOrLeft) {
                if (splitHorizontally) {
                    rect.left = rect2.right + dockDividerWidth;
                } else {
                    rect.top = rect2.bottom + dockDividerWidth;
                }
            } else if (splitHorizontally) {
                rect.right = rect2.left - dockDividerWidth;
            } else {
                rect.bottom = rect2.top - dockDividerWidth;
            }
            DockedDividerUtils.sanitizeStackBounds(rect, dockOnTopOrLeft ^ 1);
        } else if (this.mService.mDockedStackCreateBounds != null) {
            rect.set(this.mService.mDockedStackCreateBounds);
        } else {
            DisplayInfo di = this.mDisplayContent.getDisplayInfo();
            if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(this.mDisplayContent.getDisplayId())) {
                this.mService.mPolicy.getStableInsetsLw(di.rotation, di.logicalWidth, di.logicalHeight, this.mTmpRect2, this.mDisplayContent.getDisplayId(), di.displayCutout);
            } else {
                this.mService.mPolicy.getStableInsetsLw(di.rotation, di.logicalWidth, di.logicalHeight, di.displayCutout, this.mTmpRect2);
            }
            Resources resources = this.mService.mContext.getResources();
            int i = di.logicalWidth;
            int i2 = di.logicalHeight;
            boolean z = this.mDisplayContent.getConfiguration().orientation == 1;
            Rect rect3 = this.mTmpRect2;
            DividerSnapAlgorithm dividerSnapAlgorithm = r3;
            DividerSnapAlgorithm dividerSnapAlgorithm2 = new DividerSnapAlgorithm(resources, i, i2, dockDividerWidth, z, rect3);
            int position = dividerSnapAlgorithm.getMiddleTarget().position;
            if (dockOnTopOrLeft) {
                if (splitHorizontally) {
                    rect.right = position;
                } else {
                    rect.bottom = position;
                }
            } else if (splitHorizontally) {
                rect.left = position + dockDividerWidth;
            } else {
                rect.top = position + dockDividerWidth;
            }
        }
    }

    void resetDockedStackToMiddle() {
        if (inSplitScreenPrimaryWindowingMode()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not a docked stack=");
            stringBuilder.append(this);
            throw new IllegalStateException(stringBuilder.toString());
        }
        this.mService.mDockedStackCreateBounds = null;
        Rect bounds = new Rect();
        getStackDockedModeBoundsLocked(null, bounds, new Rect(), true);
        getController().requestResize(bounds);
    }

    StackWindowController getController() {
        return (StackWindowController) super.getController();
    }

    void removeIfPossible() {
        if (isSelfOrChildAnimating()) {
            this.mDeferRemoval = true;
        } else {
            removeImmediately();
        }
    }

    void onParentSet() {
        super.onParentSet();
        if (getParent() == null && this.mDisplayContent != null) {
            EventLog.writeEvent(EventLogTags.WM_STACK_REMOVED, this.mStackId);
            if (this.mAnimationBackgroundSurface != null) {
                this.mAnimationBackgroundSurface.destroy();
                this.mAnimationBackgroundSurface = null;
            }
            this.mDisplayContent = null;
            this.mService.mWindowPlacerLocked.requestTraversal();
        }
    }

    void resetAnimationBackgroundAnimator() {
        this.mAnimationBackgroundAnimator = null;
        hideAnimationSurface();
    }

    protected void setAnimationBackground(WindowStateAnimator winAnimator, int color) {
        int animLayer = winAnimator.mAnimLayer;
        if (this.mAnimationBackgroundAnimator == null || animLayer < this.mAnimationBackgroundAnimator.mAnimLayer) {
            this.mAnimationBackgroundAnimator = winAnimator;
            animLayer = this.mDisplayContent.getLayerForAnimationBackground(winAnimator);
            showAnimationSurface(((float) ((color >> 24) & 255)) / 255.0f);
        }
    }

    void switchUser() {
        super.switchUser();
        int top = this.mChildren.size();
        for (int taskNdx = 0; taskNdx < top; taskNdx++) {
            Task task = (Task) this.mChildren.get(taskNdx);
            if (this.mService.isCurrentProfileLocked(task.mUserId) || task.showForAllUsers()) {
                this.mChildren.remove(taskNdx);
                this.mChildren.add(task);
                top--;
            }
        }
    }

    void setAdjustedForIme(WindowState imeWin, boolean forceUpdate) {
        this.mImeWin = imeWin;
        this.mImeGoingAway = false;
        if (!this.mAdjustedForIme || forceUpdate) {
            this.mAdjustedForIme = true;
            this.mAdjustImeAmount = 0.0f;
            this.mAdjustDividerAmount = 0.0f;
            updateAdjustForIme(0.0f, 0.0f, true);
        }
    }

    boolean isAdjustedForIme() {
        return this.mAdjustedForIme;
    }

    boolean isAnimatingForIme() {
        return this.mImeWin != null && this.mImeWin.isAnimatingLw();
    }

    boolean updateAdjustForIme(float adjustAmount, float adjustDividerAmount, boolean force) {
        if (adjustAmount == this.mAdjustImeAmount && adjustDividerAmount == this.mAdjustDividerAmount && !force) {
            return false;
        }
        this.mAdjustImeAmount = adjustAmount;
        this.mAdjustDividerAmount = adjustDividerAmount;
        updateAdjustedBounds();
        return isVisible();
    }

    void resetAdjustedForIme(boolean adjustBoundsNow) {
        if (adjustBoundsNow) {
            this.mImeWin = null;
            this.mImeGoingAway = false;
            this.mAdjustImeAmount = 0.0f;
            this.mAdjustDividerAmount = 0.0f;
            if (this.mAdjustedForIme) {
                this.mAdjustedForIme = false;
                updateAdjustedBounds();
                this.mService.setResizeDimLayer(false, getWindowingMode(), 1.0f);
            } else {
                return;
            }
        }
        this.mImeGoingAway |= this.mAdjustedForIme;
    }

    boolean setAdjustedForMinimizedDock(float minimizeAmount) {
        if (minimizeAmount == this.mMinimizeAmount) {
            return false;
        }
        this.mMinimizeAmount = minimizeAmount;
        updateAdjustedBounds();
        return isVisible();
    }

    boolean shouldIgnoreInput() {
        return isAdjustedForMinimizedDockedStack() || (inSplitScreenPrimaryWindowingMode() && isMinimizedDockAndHomeStackResizable());
    }

    void beginImeAdjustAnimation() {
        for (int j = this.mChildren.size() - 1; j >= 0; j--) {
            Task task = (Task) this.mChildren.get(j);
            if (task.hasContentToDisplay()) {
                task.setDragResizing(true, 1);
                task.setWaitingForDrawnIfResizingChanged();
            }
        }
    }

    void endImeAdjustAnimation() {
        for (int j = this.mChildren.size() - 1; j >= 0; j--) {
            ((Task) this.mChildren.get(j)).setDragResizing(false, 1);
        }
    }

    int getMinTopStackBottom(Rect displayContentRect, int originalStackBottom) {
        return displayContentRect.top + ((int) (((float) (originalStackBottom - displayContentRect.top)) * ADJUSTED_STACK_FRACTION_MIN));
    }

    private boolean adjustForIME(WindowState imeWin) {
        int dockedSide = getDockSide();
        boolean dockedTopOrBottom = dockedSide == 2 || dockedSide == 4;
        int i;
        if (imeWin == null) {
        } else if (dockedTopOrBottom) {
            Rect displayStableRect = this.mTmpRect;
            Rect contentBounds = this.mTmpRect2;
            getDisplayContent().getStableRect(displayStableRect);
            contentBounds.set(displayStableRect);
            int imeTop = Math.max(imeWin.getFrameLw().top, contentBounds.top) + imeWin.getGivenContentInsetsLw().top;
            if (contentBounds.bottom > imeTop) {
                contentBounds.bottom = imeTop;
            }
            int yOffset = displayStableRect.bottom - contentBounds.bottom;
            int dividerWidth = getDisplayContent().mDividerControllerLocked.getContentWidth();
            int dividerWidthInactive = getDisplayContent().mDividerControllerLocked.getContentWidthInactive();
            if (dockedSide == 2) {
                int bottom = Math.max(((getRawBounds().bottom - yOffset) + dividerWidth) - dividerWidthInactive, getMinTopStackBottom(displayStableRect, getRawBounds().bottom));
                this.mTmpAdjustedBounds.set(getRawBounds());
                this.mTmpAdjustedBounds.bottom = (int) ((this.mAdjustImeAmount * ((float) bottom)) + ((1.0f - this.mAdjustImeAmount) * ((float) getRawBounds().bottom)));
                this.mFullyAdjustedImeBounds.set(getRawBounds());
                i = dockedSide;
                Rect rect = displayStableRect;
            } else {
                int dividerWidthDelta = dividerWidthInactive - dividerWidth;
                int topBeforeImeAdjust = (getRawBounds().top - dividerWidth) + dividerWidthInactive;
                int top = Math.max(getRawBounds().top - yOffset, getMinTopStackBottom(displayStableRect, getRawBounds().top - dividerWidth) + dividerWidthInactive);
                this.mTmpAdjustedBounds.set(getRawBounds());
                this.mTmpAdjustedBounds.top = getRawBounds().top + ((int) ((this.mAdjustImeAmount * ((float) (top - topBeforeImeAdjust))) + (this.mAdjustDividerAmount * ((float) dividerWidthDelta))));
                this.mFullyAdjustedImeBounds.set(getRawBounds());
                this.mFullyAdjustedImeBounds.top = top;
                this.mFullyAdjustedImeBounds.bottom = getRawBounds().height() + top;
            }
            return true;
        } else {
            i = dockedSide;
        }
        return false;
    }

    private boolean adjustForMinimizedDockedStack(float minimizeAmount) {
        int dockSide = getDockSide();
        if (dockSide == -1 && !this.mTmpAdjustedBounds.isEmpty()) {
            return false;
        }
        int topInset;
        if (dockSide == 2) {
            this.mService.getStableInsetsLocked(0, this.mTmpRect);
            topInset = this.mTmpRect.top;
            this.mTmpAdjustedBounds.set(getRawBounds());
            this.mTmpAdjustedBounds.bottom = (int) ((((float) topInset) * minimizeAmount) + ((1.0f - minimizeAmount) * ((float) getRawBounds().bottom)));
        } else if (dockSide == 1) {
            this.mTmpAdjustedBounds.set(getRawBounds());
            topInset = getRawBounds().width();
            this.mTmpAdjustedBounds.right = (int) ((((float) this.mDockedStackMinimizeThickness) * minimizeAmount) + ((1.0f - minimizeAmount) * ((float) getRawBounds().right)));
            this.mTmpAdjustedBounds.left = this.mTmpAdjustedBounds.right - topInset;
        } else if (dockSide == 3) {
            this.mTmpAdjustedBounds.set(getRawBounds());
            this.mTmpAdjustedBounds.left = (int) ((((float) (getRawBounds().right - this.mDockedStackMinimizeThickness)) * minimizeAmount) + ((1.0f - minimizeAmount) * ((float) getRawBounds().left)));
        }
        return true;
    }

    private boolean isMinimizedDockAndHomeStackResizable() {
        return this.mDisplayContent.mDividerControllerLocked.isMinimizedDock() && this.mDisplayContent.mDividerControllerLocked.isHomeStackResizable();
    }

    int getMinimizeDistance() {
        int dockSide = getDockSide();
        if (dockSide == -1) {
            return 0;
        }
        if (dockSide == 2) {
            this.mService.getStableInsetsLocked(0, this.mTmpRect);
            return getRawBounds().bottom - this.mTmpRect.top;
        } else if (dockSide == 1 || dockSide == 3) {
            return getRawBounds().width() - this.mDockedStackMinimizeThickness;
        } else {
            return 0;
        }
    }

    private void updateAdjustedBounds() {
        boolean adjust = false;
        if (this.mMinimizeAmount != 0.0f) {
            adjust = adjustForMinimizedDockedStack(this.mMinimizeAmount);
        } else if (this.mAdjustedForIme) {
            adjust = adjustForIME(this.mImeWin);
        }
        if (!adjust) {
            this.mTmpAdjustedBounds.setEmpty();
        }
        setAdjustedBounds(this.mTmpAdjustedBounds);
        boolean isImeTarget = this.mService.getImeFocusStackLocked() == this;
        if (this.mAdjustedForIme && adjust && !isImeTarget) {
            this.mService.setResizeDimLayer(true, getWindowingMode(), Math.max(this.mAdjustImeAmount, this.mAdjustDividerAmount) * IME_ADJUST_DIM_AMOUNT);
        }
    }

    void applyAdjustForImeIfNeeded(Task task) {
        if (this.mMinimizeAmount == 0.0f && this.mAdjustedForIme && !this.mAdjustedBounds.isEmpty()) {
            task.alignToAdjustedBounds(this.mAdjustedBounds, this.mImeGoingAway ? getRawBounds() : this.mFullyAdjustedImeBounds, getDockSide() == 2);
            this.mDisplayContent.setLayoutNeeded();
        }
    }

    boolean isAdjustedForMinimizedDockedStack() {
        return this.mMinimizeAmount != 0.0f;
    }

    boolean isTaskAnimating() {
        for (int j = this.mChildren.size() - 1; j >= 0; j--) {
            if (((Task) this.mChildren.get(j)).isTaskAnimating()) {
                return true;
            }
        }
        return false;
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, boolean trim) {
        long token = proto.start(fieldId);
        super.writeToProto(proto, 1146756268033L, trim);
        proto.write(1120986464258L, this.mStackId);
        for (int taskNdx = this.mChildren.size() - 1; taskNdx >= 0; taskNdx--) {
            ((Task) this.mChildren.get(taskNdx)).writeToProto(proto, 2246267895811L, trim);
        }
        proto.write(1133871366148L, matchParentBounds());
        getRawBounds().writeToProto(proto, 1146756268037L);
        proto.write(1133871366150L, this.mAnimationBackgroundSurfaceIsShown);
        proto.write(1133871366151L, this.mDeferRemoval);
        proto.write(1108101562376L, this.mMinimizeAmount);
        proto.write(1133871366153L, this.mAdjustedForIme);
        proto.write(1108101562378L, this.mAdjustImeAmount);
        proto.write(1108101562379L, this.mAdjustDividerAmount);
        this.mAdjustedBounds.writeToProto(proto, 1146756268044L);
        proto.write(1133871366157L, this.mBoundsAnimating);
        proto.end(token);
    }

    void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        int taskNdx;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mStackId=");
        stringBuilder.append(this.mStackId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mDeferRemoval=");
        stringBuilder.append(this.mDeferRemoval);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("mBounds=");
        stringBuilder.append(getRawBounds().toShortString());
        pw.println(stringBuilder.toString());
        if (this.mMinimizeAmount != 0.0f) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mMinimizeAmount=");
            stringBuilder.append(this.mMinimizeAmount);
            pw.println(stringBuilder.toString());
        }
        if (this.mAdjustedForIme) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mAdjustedForIme=true");
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mAdjustImeAmount=");
            stringBuilder.append(this.mAdjustImeAmount);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mAdjustDividerAmount=");
            stringBuilder.append(this.mAdjustDividerAmount);
            pw.println(stringBuilder.toString());
        }
        if (!this.mAdjustedBounds.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mAdjustedBounds=");
            stringBuilder.append(this.mAdjustedBounds.toShortString());
            pw.println(stringBuilder.toString());
        }
        for (taskNdx = this.mChildren.size() - 1; taskNdx >= 0; taskNdx--) {
            Task task = (Task) this.mChildren.get(taskNdx);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("  ");
            task.dump(pw, stringBuilder2.toString(), dumpAll);
        }
        if (this.mAnimationBackgroundSurfaceIsShown) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mWindowAnimationBackgroundSurface is shown");
            pw.println(stringBuilder.toString());
        }
        if (!this.mExitingAppTokens.isEmpty()) {
            pw.println();
            pw.println("  Exiting application tokens:");
            for (taskNdx = this.mExitingAppTokens.size() - 1; taskNdx >= 0; taskNdx--) {
                WindowToken token = (WindowToken) this.mExitingAppTokens.get(taskNdx);
                pw.print("  Exiting App #");
                pw.print(taskNdx);
                pw.print(' ');
                pw.print(token);
                pw.println(':');
                token.dump(pw, "    ", dumpAll);
            }
        }
        this.mAnimatingAppWindowTokenRegistry.dump(pw, "AnimatingApps:", prefix);
    }

    boolean fillsParent() {
        if (useCurrentBounds()) {
            return matchParentBounds();
        }
        return true;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{stackId=");
        stringBuilder.append(this.mStackId);
        stringBuilder.append(" tasks=");
        stringBuilder.append(this.mChildren);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    String getName() {
        return toShortString();
    }

    public String toShortString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Stack=");
        stringBuilder.append(this.mStackId);
        return stringBuilder.toString();
    }

    int getDockSide() {
        return getDockSide(getRawBounds());
    }

    int getDockSideForDisplay(DisplayContent dc) {
        return getDockSide(dc, getRawBounds());
    }

    private int getDockSide(Rect bounds) {
        if (this.mDisplayContent == null) {
            return -1;
        }
        return getDockSide(this.mDisplayContent, bounds);
    }

    private int getDockSide(DisplayContent dc, Rect bounds) {
        if (!inSplitScreenWindowingMode()) {
            return -1;
        }
        dc.getBounds(this.mTmpRect);
        return dc.getDockedDividerController().getDockSide(bounds, this.mTmpRect, dc.getConfiguration().orientation);
    }

    boolean hasTaskForUser(int userId) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((Task) this.mChildren.get(i)).mUserId == userId) {
                return true;
            }
        }
        return false;
    }

    int taskIdFromPoint(int x, int y) {
        getBounds(this.mTmpRect);
        if (!this.mTmpRect.contains(x, y) || isAdjustedForMinimizedDockedStack()) {
            return -1;
        }
        for (int taskNdx = this.mChildren.size() - 1; taskNdx >= 0; taskNdx--) {
            Task task = (Task) this.mChildren.get(taskNdx);
            if (task.getTopVisibleAppMainWindow() != null || (HwPCUtils.isPcCastModeInServer() && task.isVisible())) {
                task.getDimBounds(this.mTmpRect);
                if (this.mTmpRect.contains(x, y)) {
                    return task.mTaskId;
                }
            }
        }
        return -1;
    }

    protected void findTaskForResizePoint(int x, int y, int delta, TaskForResizePointSearchResult results) {
        if (getWindowConfiguration().canResizeTask()) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                Task task = (Task) this.mChildren.get(i);
                if (task.isFullscreen()) {
                    results.searchDone = true;
                    return;
                }
                task.getDimBounds(this.mTmpRect);
                this.mTmpRect.inset(-delta, -delta);
                if (this.mTmpRect.contains(x, y)) {
                    this.mTmpRect.inset(delta, delta);
                    results.searchDone = true;
                    if (!this.mTmpRect.contains(x, y)) {
                        results.taskForResize = task;
                        return;
                    }
                    return;
                }
            }
            return;
        }
        results.searchDone = true;
    }

    void setTouchExcludeRegion(Task focusedTask, int delta, Region touchExcludeRegion, Rect contentRect, Rect postExclude) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            Task task = (Task) this.mChildren.get(i);
            AppWindowToken token = task.getTopVisibleAppToken();
            if (token != null && token.hasContentToDisplay()) {
                if (task.isActivityTypeHome() && isMinimizedDockAndHomeStackResizable()) {
                    this.mDisplayContent.getBounds(this.mTmpRect);
                } else {
                    task.getDimBounds(this.mTmpRect);
                }
                if (task == focusedTask) {
                    postExclude.set(this.mTmpRect);
                }
                boolean isFreeformed = task.inFreeformWindowingMode() || task.inHwPCFreeformWindowingMode();
                if (task != focusedTask || isFreeformed) {
                    if (isFreeformed) {
                        this.mTmpRect.inset(-delta, -delta);
                        this.mTmpRect.intersect(contentRect);
                    }
                    touchExcludeRegion.op(this.mTmpRect, Op.DIFFERENCE);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0013, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:12:?, code:
            r2.mService.mActivityManager.resizePinnedStack(r3, r4);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setPinnedStackSize(Rect stackBounds, Rect tempTaskBounds) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mCancelCurrentBoundsAnimation) {
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return false;
        return true;
    }

    void onAllWindowsDrawn() {
        if (this.mBoundsAnimating || this.mBoundsAnimatingRequested) {
            this.mService.mBoundsAnimationController.onAllWindowsDrawn();
        }
    }

    public void onAnimationStart(boolean schedulePipModeChangedCallback, boolean forceUpdate) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mBoundsAnimatingRequested = false;
                this.mBoundsAnimating = true;
                this.mCancelCurrentBoundsAnimation = false;
                if (schedulePipModeChangedCallback) {
                    forAllWindows((Consumer) -$$Lambda$TaskStack$n0sDe5GcitIQB-Orca4W45Hcc98.INSTANCE, false);
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        if (inPinnedWindowingMode()) {
            try {
                this.mService.mActivityManager.notifyPinnedStackAnimationStarted();
            } catch (RemoteException e) {
            }
            PinnedStackWindowController controller = (PinnedStackWindowController) getController();
            if (schedulePipModeChangedCallback && controller != null) {
                controller.updatePictureInPictureModeForPinnedStackAnimation(null, forceUpdate);
            }
        }
    }

    public void onAnimationEnd(boolean schedulePipModeChangedCallback, Rect finalStackSize, boolean moveToFullscreen) {
        if (inPinnedWindowingMode()) {
            PinnedStackWindowController controller = (PinnedStackWindowController) getController();
            if (schedulePipModeChangedCallback && controller != null) {
                controller.updatePictureInPictureModeForPinnedStackAnimation(this.mBoundsAnimationTarget, false);
            }
            if (finalStackSize != null) {
                setPinnedStackSize(finalStackSize, null);
            } else {
                onPipAnimationEndResize();
            }
            try {
                this.mService.mActivityManager.notifyPinnedStackAnimationEnded();
                if (moveToFullscreen) {
                    this.mService.mActivityManager.moveTasksToFullscreenStack(this.mStackId, true);
                    return;
                }
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        onPipAnimationEndResize();
    }

    public void onPipAnimationEndResize() {
        int i = 0;
        this.mBoundsAnimating = false;
        while (i < this.mChildren.size()) {
            ((Task) this.mChildren.get(i)).clearPreserveNonFloatingState();
            i++;
        }
        this.mService.requestTraversal();
    }

    public boolean shouldDeferStartOnMoveToFullscreen() {
        TaskStack homeStack = this.mDisplayContent.getHomeStack();
        if (homeStack == null) {
            return true;
        }
        Task homeTask = (Task) homeStack.getTopChild();
        if (homeTask == null) {
            return true;
        }
        AppWindowToken homeApp = homeTask.getTopVisibleAppToken();
        if (!homeTask.isVisible() || homeApp == null) {
            return true;
        }
        return true ^ homeApp.allDrawn;
    }

    public boolean deferScheduleMultiWindowModeChanged() {
        boolean z = false;
        if (!inPinnedWindowingMode()) {
            return false;
        }
        if (this.mBoundsAnimatingRequested || this.mBoundsAnimating) {
            z = true;
        }
        return z;
    }

    public boolean isForceScaled() {
        return this.mBoundsAnimating;
    }

    public boolean isAnimatingBounds() {
        return this.mBoundsAnimating;
    }

    public boolean lastAnimatingBoundsWasToFullscreen() {
        return this.mBoundsAnimatingToFullscreen;
    }

    public boolean isAnimatingBoundsToFullscreen() {
        return isAnimatingBounds() && lastAnimatingBoundsWasToFullscreen();
    }

    public boolean pinnedStackResizeDisallowed() {
        if (this.mBoundsAnimating && this.mCancelCurrentBoundsAnimation) {
            return true;
        }
        return false;
    }

    boolean checkCompleteDeferredRemoval() {
        if (isSelfOrChildAnimating()) {
            return true;
        }
        if (this.mDeferRemoval) {
            removeImmediately();
        }
        return super.checkCompleteDeferredRemoval();
    }

    int getOrientation() {
        return canSpecifyOrientation() ? super.getOrientation() : -2;
    }

    private boolean canSpecifyOrientation() {
        int windowingMode = getWindowingMode();
        int activityType = getActivityType();
        return windowingMode == 1 || activityType == 2 || activityType == 3 || activityType == 4;
    }

    Dimmer getDimmer() {
        return this.mDimmer;
    }

    void prepareSurfaces() {
        this.mDimmer.resetDimStates();
        super.prepareSurfaces();
        getDimBounds(this.mTmpDimBoundsRect);
        this.mTmpDimBoundsRect.offsetTo(0, 0);
        if (this.mDimmer.updateDims(getPendingTransaction(), this.mTmpDimBoundsRect)) {
            scheduleAnimation();
        }
    }

    public DisplayInfo getDisplayInfo() {
        if (this.mDisplayContent != null) {
            return this.mDisplayContent.getDisplayInfo();
        }
        return new DisplayInfo();
    }

    void dim(float alpha) {
        this.mDimmer.dimAbove(getPendingTransaction(), alpha);
        scheduleAnimation();
    }

    void stopDimming() {
        this.mDimmer.stopDim(getPendingTransaction());
        scheduleAnimation();
    }

    void getRelativePosition(Point outPos) {
        super.getRelativePosition(outPos);
        int outset = getStackOutset();
        outPos.x -= outset;
        outPos.y -= outset;
    }

    AnimatingAppWindowTokenRegistry getAnimatingAppWindowTokenRegistry() {
        return this.mAnimatingAppWindowTokenRegistry;
    }

    void clearTempInsetBounds() {
        for (int taskNdx = this.mChildren.size() - 1; taskNdx >= 0; taskNdx--) {
            ((Task) this.mChildren.get(taskNdx)).setTempInsetBounds(null);
        }
    }
}
