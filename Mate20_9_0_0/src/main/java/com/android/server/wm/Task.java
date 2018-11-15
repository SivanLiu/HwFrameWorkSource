package com.android.server.wm;

import android.app.ActivityManager.TaskDescription;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.EventLog;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.EventLogTags;
import java.io.PrintWriter;
import java.util.function.Consumer;

class Task extends WindowContainer<AppWindowToken> {
    static final String TAG = "WindowManager";
    private boolean mCanAffectSystemUiFlags = true;
    private boolean mDeferRemoval = false;
    private Dimmer mDimmer = new Dimmer(this);
    private int mDragResizeMode;
    private boolean mDragResizing;
    final Rect mPreparedFrozenBounds = new Rect();
    final Configuration mPreparedFrozenMergedConfig = new Configuration();
    private boolean mPreserveNonFloatingState = false;
    private int mResizeMode;
    private int mRotation;
    TaskStack mStack;
    private boolean mSupportsPictureInPicture;
    private TaskDescription mTaskDescription;
    final int mTaskId;
    private final Rect mTempInsetBounds = new Rect();
    private final Rect mTmpDimBoundsRect = new Rect();
    private Rect mTmpRect = new Rect();
    private Rect mTmpRect2 = new Rect();
    private Rect mTmpRect3 = new Rect();
    final int mUserId;

    Task(int taskId, TaskStack stack, int userId, WindowManagerService service, int resizeMode, boolean supportsPictureInPicture, TaskDescription taskDescription, TaskWindowContainerController controller) {
        super(service);
        this.mTaskId = taskId;
        this.mStack = stack;
        this.mUserId = userId;
        this.mResizeMode = resizeMode;
        this.mSupportsPictureInPicture = supportsPictureInPicture;
        setController(controller);
        setBounds(getOverrideBounds());
        this.mTaskDescription = taskDescription;
        setOrientation(-2);
    }

    DisplayContent getDisplayContent() {
        return this.mStack != null ? this.mStack.getDisplayContent() : null;
    }

    private int getAdjustedAddPosition(int suggestedPosition) {
        int size = this.mChildren.size();
        if (suggestedPosition >= size) {
            return Math.min(size, suggestedPosition);
        }
        int pos = 0;
        while (pos < size && pos < suggestedPosition) {
            if (((AppWindowToken) this.mChildren.get(pos)).removed) {
                suggestedPosition++;
            }
            pos++;
        }
        return Math.min(size, suggestedPosition);
    }

    void addChild(AppWindowToken wtoken, int position) {
        super.addChild((WindowContainer) wtoken, getAdjustedAddPosition(position));
        this.mDeferRemoval = false;
    }

    void positionChildAt(int position, AppWindowToken child, boolean includingParents) {
        super.positionChildAt(getAdjustedAddPosition(position), child, includingParents);
        this.mDeferRemoval = false;
    }

    private boolean hasWindowsAlive() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((AppWindowToken) this.mChildren.get(i)).hasWindowsAlive()) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    boolean shouldDeferRemoval() {
        return hasWindowsAlive() && this.mStack.isSelfOrChildAnimating();
    }

    void removeIfPossible() {
        if (shouldDeferRemoval()) {
            this.mDeferRemoval = true;
        } else {
            removeImmediately();
        }
    }

    void removeImmediately() {
        EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, new Object[]{Integer.valueOf(this.mTaskId), "removeTask"});
        this.mDeferRemoval = false;
        super.removeImmediately();
    }

    void reparent(TaskStack stack, int position, boolean moveParents) {
        if (stack != this.mStack) {
            EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, new Object[]{Integer.valueOf(this.mTaskId), "reParentTask"});
            DisplayContent prevDisplayContent = getDisplayContent();
            if (stack.inPinnedWindowingMode()) {
                this.mPreserveNonFloatingState = true;
            } else {
                this.mPreserveNonFloatingState = false;
            }
            getParent().removeChild(this);
            stack.addTask(this, position, showForAllUsers(), moveParents);
            DisplayContent displayContent = stack.getDisplayContent();
            displayContent.setLayoutNeeded();
            if (prevDisplayContent != displayContent) {
                onDisplayChanged(displayContent);
                prevDisplayContent.setLayoutNeeded();
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("task=");
        stringBuilder.append(this);
        stringBuilder.append(" already child of stack=");
        stringBuilder.append(this.mStack);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    void positionAt(int position) {
        this.mStack.positionChildAt(position, this, false);
    }

    void onParentSet() {
        super.onParentSet();
        updateDisplayInfo(getDisplayContent());
        if (getWindowConfiguration().windowsAreScaleable()) {
            forceWindowsScaleable(true);
        } else {
            forceWindowsScaleable(false);
        }
    }

    void removeChild(AppWindowToken token) {
        if (this.mChildren.contains(token)) {
            super.removeChild(token);
            if (this.mChildren.isEmpty()) {
                EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, new Object[]{Integer.valueOf(this.mTaskId), "removeAppToken: last token"});
                if (this.mDeferRemoval) {
                    removeIfPossible();
                }
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeChild: token=");
        stringBuilder.append(this);
        stringBuilder.append(" not found.");
        Slog.e(str, stringBuilder.toString());
    }

    void setSendingToBottom(boolean toBottom) {
        for (int appTokenNdx = 0; appTokenNdx < this.mChildren.size(); appTokenNdx++) {
            ((AppWindowToken) this.mChildren.get(appTokenNdx)).sendingToBottom = toBottom;
        }
    }

    public int setBounds(Rect bounds, boolean forceResize) {
        int boundsChanged = setBounds(bounds);
        if (!forceResize || (boundsChanged & 2) == 2) {
            return boundsChanged;
        }
        onResize();
        return 2 | boundsChanged;
    }

    public int setBounds(Rect bounds) {
        int rotation = 0;
        DisplayContent displayContent = this.mStack.getDisplayContent();
        if (displayContent != null) {
            rotation = displayContent.getDisplayInfo().rotation;
        } else if (bounds == null) {
            return 0;
        }
        if (equivalentOverrideBounds(bounds)) {
            return 0;
        }
        int boundsChange = super.setBounds(bounds);
        this.mRotation = rotation;
        return boundsChange;
    }

    void setTempInsetBounds(Rect tempInsetBounds) {
        if (tempInsetBounds != null) {
            this.mTempInsetBounds.set(tempInsetBounds);
        } else {
            this.mTempInsetBounds.setEmpty();
        }
    }

    void getTempInsetBounds(Rect out) {
        out.set(this.mTempInsetBounds);
    }

    void setResizeable(int resizeMode) {
        this.mResizeMode = resizeMode;
    }

    boolean isResizeable() {
        return ActivityInfo.isResizeableMode(this.mResizeMode) || this.mSupportsPictureInPicture || this.mService.mForceResizableTasks;
    }

    boolean preserveOrientationOnResize() {
        return this.mResizeMode == 6 || this.mResizeMode == 5 || this.mResizeMode == 7;
    }

    boolean cropWindowsToStackBounds() {
        return isResizeable();
    }

    void prepareFreezingBounds() {
        this.mPreparedFrozenBounds.set(getBounds());
        this.mPreparedFrozenMergedConfig.setTo(getConfiguration());
    }

    void alignToAdjustedBounds(Rect adjustedBounds, Rect tempInsetBounds, boolean alignBottom) {
        if (isResizeable() && !Configuration.EMPTY.equals(getOverrideConfiguration())) {
            getBounds(this.mTmpRect2);
            if (alignBottom) {
                this.mTmpRect2.offset(0, adjustedBounds.bottom - this.mTmpRect2.bottom);
            } else {
                this.mTmpRect2.offsetTo(adjustedBounds.left, adjustedBounds.top);
            }
            setTempInsetBounds(tempInsetBounds);
            setBounds(this.mTmpRect2, false);
        }
    }

    private boolean useCurrentBounds() {
        DisplayContent displayContent = getDisplayContent();
        return matchParentBounds() || !inSplitScreenSecondaryWindowingMode() || displayContent == null || displayContent.getSplitScreenPrimaryStackIgnoringVisibility() != null;
    }

    public void getBounds(Rect out) {
        if (useCurrentBounds()) {
            super.getBounds(out);
        } else {
            this.mStack.getDisplayContent().getBounds(out);
        }
    }

    boolean getMaxVisibleBounds(Rect out) {
        boolean foundTop = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            AppWindowToken token = (AppWindowToken) this.mChildren.get(i);
            if (!(token.mIsExiting || token.isClientHidden() || token.hiddenRequested)) {
                WindowState win = token.findMainWindow();
                if (win != null) {
                    if (foundTop) {
                        if (win.mVisibleFrame.left < out.left) {
                            out.left = win.mVisibleFrame.left;
                        }
                        if (win.mVisibleFrame.top < out.top) {
                            out.top = win.mVisibleFrame.top;
                        }
                        if (win.mVisibleFrame.right > out.right) {
                            out.right = win.mVisibleFrame.right;
                        }
                        if (win.mVisibleFrame.bottom > out.bottom) {
                            out.bottom = win.mVisibleFrame.bottom;
                        }
                    } else {
                        out.set(win.mVisibleFrame);
                        foundTop = true;
                    }
                }
            }
        }
        return foundTop;
    }

    public void getDimBounds(Rect out) {
        if (this.mStack != null) {
            DisplayContent displayContent = this.mStack.getDisplayContent();
            boolean dockedResizing = displayContent != null && displayContent.mDividerControllerLocked.isResizing();
            if (!useCurrentBounds()) {
                if (displayContent != null) {
                    displayContent.getBounds(out);
                }
            } else if ((HwPCUtils.isExtDynamicStack(this.mStack.mStackId) && matchParentBounds()) || ((!inFreeformWindowingMode() && !inHwPCFreeformWindowingMode()) || !getMaxVisibleBounds(out))) {
                if (matchParentBounds()) {
                    out.set(getBounds());
                } else if (dockedResizing) {
                    this.mStack.getBounds(out);
                } else {
                    this.mStack.getBounds(this.mTmpRect);
                    this.mTmpRect.intersect(getBounds());
                    out.set(this.mTmpRect);
                }
            }
        }
    }

    void setDragResizing(boolean dragResizing, int dragResizeMode) {
        if (this.mDragResizing == dragResizing) {
            return;
        }
        if (DragResizeMode.isModeAllowedForStack(this.mStack, dragResizeMode)) {
            this.mDragResizing = dragResizing;
            this.mDragResizeMode = dragResizeMode;
            resetDragResizingChangeReported();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Drag resize mode not allow for stack stackId=");
        stringBuilder.append(this.mStack.mStackId);
        stringBuilder.append(" dragResizeMode=");
        stringBuilder.append(dragResizeMode);
        stringBuilder.append(" mode=");
        stringBuilder.append(this.mStack.getWindowingMode());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    boolean isDragResizing() {
        return this.mDragResizing;
    }

    int getDragResizeMode() {
        return this.mDragResizeMode;
    }

    void updateDisplayInfo(DisplayContent displayContent) {
        if (displayContent != null) {
            if (matchParentBounds()) {
                setBounds(null);
                return;
            }
            int newRotation = displayContent.getDisplayInfo().rotation;
            if (this.mRotation != newRotation) {
                this.mTmpRect2.set(getBounds());
                if (getWindowConfiguration().canResizeTask()) {
                    displayContent.rotateBounds(this.mRotation, newRotation, this.mTmpRect2);
                    if (setBounds(this.mTmpRect2) != 0) {
                        TaskWindowContainerController controller = getController();
                        if (controller != null) {
                            controller.requestResize(getBounds(), 1);
                        }
                    }
                    return;
                }
                setBounds(this.mTmpRect2);
            }
        }
    }

    void cancelTaskWindowTransition() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((AppWindowToken) this.mChildren.get(i)).cancelAnimation();
        }
    }

    boolean isSamePackageInTask() {
        int uid = -1;
        boolean ret = true;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            AppWindowToken tmpToken = (AppWindowToken) this.mChildren.get(i);
            int i2 = -1;
            if (uid == -1) {
                if (!tmpToken.isEmpty()) {
                    i2 = ((WindowState) tmpToken.mChildren.peekLast()).mOwnerUid;
                }
                uid = i2;
            } else {
                if (!tmpToken.isEmpty()) {
                    i2 = ((WindowState) tmpToken.mChildren.peekLast()).mOwnerUid;
                }
                ret = uid == i2;
            }
            if (!ret) {
                return ret;
            }
        }
        return ret;
    }

    boolean showForAllUsers() {
        int tokensCount = this.mChildren.size();
        return tokensCount != 0 && ((AppWindowToken) this.mChildren.get(tokensCount - 1)).mShowForAllUsers;
    }

    boolean isFloating() {
        return (!getWindowConfiguration().tasksAreFloating() || this.mStack.isAnimatingBoundsToFullscreen() || this.mPreserveNonFloatingState) ? false : true;
    }

    public SurfaceControl getAnimationLeashParent() {
        return getAppAnimationLayer(0);
    }

    boolean isTaskAnimating() {
        RecentsAnimationController recentsAnim = this.mService.getRecentsAnimationController();
        if (recentsAnim == null || !recentsAnim.isAnimatingTask(this)) {
            return false;
        }
        return true;
    }

    WindowState getTopVisibleAppMainWindow() {
        AppWindowToken token = getTopVisibleAppToken();
        return token != null ? token.findMainWindow() : null;
    }

    AppWindowToken getTopFullscreenAppToken() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            AppWindowToken token = (AppWindowToken) this.mChildren.get(i);
            WindowState win = token.findMainWindow();
            if (win != null && win.mAttrs.isFullscreen()) {
                return token;
            }
        }
        return null;
    }

    AppWindowToken getTopVisibleAppToken() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            AppWindowToken token = (AppWindowToken) this.mChildren.get(i);
            if (!token.mIsExiting && !token.isClientHidden() && !token.hiddenRequested) {
                return token;
            }
        }
        return null;
    }

    boolean isFullscreen() {
        if (useCurrentBounds()) {
            return matchParentBounds();
        }
        return true;
    }

    void forceWindowsScaleable(boolean force) {
        this.mService.openSurfaceTransaction();
        try {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                ((AppWindowToken) this.mChildren.get(i)).forceWindowsScaleableInTransaction(force);
            }
        } finally {
            this.mService.closeSurfaceTransaction("forceWindowsScaleable");
        }
    }

    void setTaskDescription(TaskDescription taskDescription) {
        this.mTaskDescription = taskDescription;
    }

    TaskDescription getTaskDescription() {
        return this.mTaskDescription;
    }

    boolean fillsParent() {
        return matchParentBounds() || !getWindowConfiguration().canResizeTask();
    }

    TaskWindowContainerController getController() {
        return (TaskWindowContainerController) super.getController();
    }

    void forAllTasks(Consumer<Task> callback) {
        callback.accept(this);
    }

    void setCanAffectSystemUiFlags(boolean canAffectSystemUiFlags) {
        this.mCanAffectSystemUiFlags = canAffectSystemUiFlags;
    }

    boolean canAffectSystemUiFlags() {
        return this.mCanAffectSystemUiFlags;
    }

    void dontAnimateDimExit() {
        this.mDimmer.dontAnimateExit();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{taskId=");
        stringBuilder.append(this.mTaskId);
        stringBuilder.append(" appTokens=");
        stringBuilder.append(this.mChildren);
        stringBuilder.append(" mdr=");
        stringBuilder.append(this.mDeferRemoval);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    String getName() {
        return toShortString();
    }

    void clearPreserveNonFloatingState() {
        this.mPreserveNonFloatingState = false;
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

    public void writeToProto(ProtoOutputStream proto, long fieldId, boolean trim) {
        long token = proto.start(fieldId);
        super.writeToProto(proto, 1146756268033L, trim);
        proto.write(1120986464258L, this.mTaskId);
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((AppWindowToken) this.mChildren.get(i)).writeToProto(proto, 2246267895811L, trim);
        }
        proto.write(1133871366148L, matchParentBounds());
        getBounds().writeToProto(proto, 1146756268037L);
        this.mTempInsetBounds.writeToProto(proto, 1146756268038L);
        proto.write(1133871366151L, this.mDeferRemoval);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        super.dump(pw, prefix, dumpAll);
        String doublePrefix = new StringBuilder();
        doublePrefix.append(prefix);
        doublePrefix.append("  ");
        doublePrefix = doublePrefix.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("taskId=");
        stringBuilder.append(this.mTaskId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(doublePrefix);
        stringBuilder.append("mBounds=");
        stringBuilder.append(getBounds().toShortString());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(doublePrefix);
        stringBuilder.append("mdr=");
        stringBuilder.append(this.mDeferRemoval);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(doublePrefix);
        stringBuilder.append("appTokens=");
        stringBuilder.append(this.mChildren);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(doublePrefix);
        stringBuilder.append("mTempInsetBounds=");
        stringBuilder.append(this.mTempInsetBounds.toShortString());
        pw.println(stringBuilder.toString());
        String triplePrefix = new StringBuilder();
        triplePrefix.append(doublePrefix);
        triplePrefix.append("  ");
        triplePrefix = triplePrefix.toString();
        String quadruplePrefix = new StringBuilder();
        quadruplePrefix.append(triplePrefix);
        quadruplePrefix.append("  ");
        quadruplePrefix = quadruplePrefix.toString();
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            AppWindowToken wtoken = (AppWindowToken) this.mChildren.get(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(triplePrefix);
            stringBuilder2.append("Activity #");
            stringBuilder2.append(i);
            stringBuilder2.append(" ");
            stringBuilder2.append(wtoken);
            pw.println(stringBuilder2.toString());
            wtoken.dump(pw, quadruplePrefix, dumpAll);
        }
    }

    String toShortString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Task=");
        stringBuilder.append(this.mTaskId);
        return stringBuilder.toString();
    }
}
