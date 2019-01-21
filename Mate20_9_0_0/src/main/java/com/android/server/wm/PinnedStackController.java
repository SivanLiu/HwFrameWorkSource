package com.android.server.wm;

import android.app.RemoteAction;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.Slog;
import android.util.TypedValue;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.IPinnedStackController.Stub;
import android.view.IPinnedStackListener;
import com.android.internal.policy.PipSnapAlgorithm;
import com.android.server.UiThread;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class PinnedStackController {
    public static final float INVALID_SNAP_FRACTION = -1.0f;
    private static final String TAG = "WindowManager";
    private ArrayList<RemoteAction> mActions = new ArrayList();
    private float mAspectRatio = -1.0f;
    private final PinnedStackControllerCallback mCallbacks = new PinnedStackControllerCallback();
    private int mCurrentMinSize;
    private float mDefaultAspectRatio;
    private int mDefaultMinSize;
    private int mDefaultStackGravity;
    private final DisplayContent mDisplayContent;
    private final DisplayInfo mDisplayInfo = new DisplayInfo();
    private final Handler mHandler = UiThread.getHandler();
    private int mImeHeight;
    private boolean mIsImeShowing;
    private boolean mIsMinimized;
    private boolean mIsShelfShowing;
    private WeakReference<AppWindowToken> mLastPipActivity = null;
    private float mMaxAspectRatio;
    private float mMinAspectRatio;
    private IPinnedStackListener mPinnedStackListener;
    private final PinnedStackListenerDeathHandler mPinnedStackListenerDeathHandler = new PinnedStackListenerDeathHandler();
    private float mReentrySnapFraction = -1.0f;
    private Point mScreenEdgeInsets;
    private final WindowManagerService mService;
    private int mShelfHeight;
    private final PipSnapAlgorithm mSnapAlgorithm;
    private final Rect mStableInsets = new Rect();
    private final Rect mTmpAnimatingBoundsRect = new Rect();
    private final Point mTmpDisplaySize = new Point();
    private final Rect mTmpInsets = new Rect();
    private final DisplayMetrics mTmpMetrics = new DisplayMetrics();
    private final Rect mTmpRect = new Rect();

    private class PinnedStackControllerCallback extends Stub {
        private PinnedStackControllerCallback() {
        }

        public void setIsMinimized(boolean isMinimized) {
            PinnedStackController.this.mHandler.post(new -$$Lambda$PinnedStackController$PinnedStackControllerCallback$0SANOJyiLP67Pkj3NbDS5B-egBU(this, isMinimized));
        }

        public static /* synthetic */ void lambda$setIsMinimized$0(PinnedStackControllerCallback pinnedStackControllerCallback, boolean isMinimized) {
            PinnedStackController.this.mIsMinimized = isMinimized;
            PinnedStackController.this.mSnapAlgorithm.setMinimized(isMinimized);
        }

        public void setMinEdgeSize(int minEdgeSize) {
            PinnedStackController.this.mHandler.post(new -$$Lambda$PinnedStackController$PinnedStackControllerCallback$MdGjZinCTxKrX3GJTl1CXkAuFro(this, minEdgeSize));
        }

        public int getDisplayRotation() {
            int i;
            synchronized (PinnedStackController.this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    i = PinnedStackController.this.mDisplayInfo.rotation;
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            return i;
        }
    }

    private class PinnedStackListenerDeathHandler implements DeathRecipient {
        private PinnedStackListenerDeathHandler() {
        }

        public void binderDied() {
            if (PinnedStackController.this.mPinnedStackListener != null) {
                PinnedStackController.this.mPinnedStackListener.asBinder().unlinkToDeath(PinnedStackController.this.mPinnedStackListenerDeathHandler, 0);
            }
            PinnedStackController.this.mPinnedStackListener = null;
        }
    }

    PinnedStackController(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
        this.mSnapAlgorithm = new PipSnapAlgorithm(service.mContext);
        this.mDisplayInfo.copyFrom(this.mDisplayContent.getDisplayInfo());
        reloadResources();
        this.mAspectRatio = this.mDefaultAspectRatio;
    }

    void onConfigurationChanged() {
        reloadResources();
    }

    private void reloadResources() {
        Size screenEdgeInsetsDp;
        Point point;
        Resources res = this.mService.mContext.getResources();
        this.mDefaultMinSize = res.getDimensionPixelSize(17105016);
        this.mCurrentMinSize = this.mDefaultMinSize;
        this.mDefaultAspectRatio = res.getFloat(17104968);
        String screenEdgeInsetsDpString = res.getString(17039790);
        if (screenEdgeInsetsDpString.isEmpty()) {
            screenEdgeInsetsDp = null;
        } else {
            screenEdgeInsetsDp = Size.parseSize(screenEdgeInsetsDpString);
        }
        this.mDefaultStackGravity = res.getInteger(17694772);
        this.mDisplayContent.getDisplay().getRealMetrics(this.mTmpMetrics);
        if (screenEdgeInsetsDp == null) {
            point = new Point();
        } else {
            point = new Point(dpToPx((float) screenEdgeInsetsDp.getWidth(), this.mTmpMetrics), dpToPx((float) screenEdgeInsetsDp.getHeight(), this.mTmpMetrics));
        }
        this.mScreenEdgeInsets = point;
        this.mMinAspectRatio = res.getFloat(17104971);
        this.mMaxAspectRatio = res.getFloat(17104970);
    }

    void registerPinnedStackListener(IPinnedStackListener listener) {
        try {
            listener.asBinder().linkToDeath(this.mPinnedStackListenerDeathHandler, 0);
            listener.onListenerRegistered(this.mCallbacks);
            this.mPinnedStackListener = listener;
            notifyImeVisibilityChanged(this.mIsImeShowing, this.mImeHeight);
            notifyShelfVisibilityChanged(this.mIsShelfShowing, this.mShelfHeight);
            notifyMovementBoundsChanged(false, false);
            notifyActionsChanged(this.mActions);
            notifyMinimizeChanged(this.mIsMinimized);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register pinned stack listener", e);
        }
    }

    public boolean isValidPictureInPictureAspectRatio(float aspectRatio) {
        return Float.compare(this.mMinAspectRatio, aspectRatio) <= 0 && Float.compare(aspectRatio, this.mMaxAspectRatio) <= 0;
    }

    Rect transformBoundsToAspectRatio(Rect stackBounds, float aspectRatio, boolean useCurrentMinEdgeSize) {
        float snapFraction = this.mSnapAlgorithm.getSnapFraction(stackBounds, getMovementBounds(stackBounds));
        Size size = this.mSnapAlgorithm.getSizeForAspectRatio(aspectRatio, (float) (useCurrentMinEdgeSize ? this.mCurrentMinSize : this.mDefaultMinSize), this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight);
        int left = (int) (((float) stackBounds.centerX()) - (((float) size.getWidth()) / 2.0f));
        int top = (int) (((float) stackBounds.centerY()) - (((float) size.getHeight()) / 2.0f));
        stackBounds.set(left, top, size.getWidth() + left, size.getHeight() + top);
        this.mSnapAlgorithm.applySnapFraction(stackBounds, getMovementBounds(stackBounds), snapFraction);
        if (this.mIsMinimized) {
            applyMinimizedOffset(stackBounds, getMovementBounds(stackBounds));
        }
        return stackBounds;
    }

    void saveReentrySnapFraction(AppWindowToken token, Rect stackBounds) {
        this.mReentrySnapFraction = getSnapFraction(stackBounds);
        this.mLastPipActivity = new WeakReference(token);
    }

    void resetReentrySnapFraction(AppWindowToken token) {
        if (this.mLastPipActivity != null && this.mLastPipActivity.get() == token) {
            this.mReentrySnapFraction = -1.0f;
            this.mLastPipActivity = null;
        }
    }

    Rect getDefaultOrLastSavedBounds() {
        return getDefaultBounds(this.mReentrySnapFraction);
    }

    Rect getDefaultBounds(float snapFraction) {
        Rect defaultBounds;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Rect insetBounds = new Rect();
                getInsetBounds(insetBounds);
                defaultBounds = new Rect();
                Size size = this.mSnapAlgorithm.getSizeForAspectRatio(this.mDefaultAspectRatio, (float) this.mDefaultMinSize, this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight);
                int i = 0;
                if (snapFraction != -1.0f) {
                    defaultBounds.set(0, 0, size.getWidth(), size.getHeight());
                    this.mSnapAlgorithm.applySnapFraction(defaultBounds, getMovementBounds(defaultBounds), snapFraction);
                } else {
                    int i2;
                    int i3 = this.mDefaultStackGravity;
                    int width = size.getWidth();
                    int height = size.getHeight();
                    if (this.mIsImeShowing) {
                        i2 = this.mImeHeight;
                    } else {
                        i2 = 0;
                    }
                    if (this.mIsShelfShowing) {
                        i = this.mShelfHeight;
                    }
                    Gravity.apply(i3, width, height, insetBounds, 0, Math.max(i2, i), defaultBounds);
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return defaultBounds;
    }

    synchronized void onDisplayInfoChanged() {
        this.mDisplayInfo.copyFrom(this.mDisplayContent.getDisplayInfo());
        notifyMovementBoundsChanged(false, false);
    }

    boolean onTaskStackBoundsChanged(Rect targetBounds, Rect outBounds) {
        boolean z;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
                z = false;
                if (this.mDisplayInfo.equals(displayInfo)) {
                    outBounds.setEmpty();
                } else if (targetBounds.isEmpty()) {
                    this.mDisplayInfo.copyFrom(displayInfo);
                    outBounds.setEmpty();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else {
                    this.mTmpRect.set(targetBounds);
                    Rect postChangeStackBounds = this.mTmpRect;
                    float snapFraction = getSnapFraction(postChangeStackBounds);
                    this.mDisplayInfo.copyFrom(displayInfo);
                    Rect postChangeMovementBounds = getMovementBounds(postChangeStackBounds, false, false);
                    this.mSnapAlgorithm.applySnapFraction(postChangeStackBounds, postChangeMovementBounds, snapFraction);
                    if (this.mIsMinimized) {
                        applyMinimizedOffset(postChangeStackBounds, postChangeMovementBounds);
                    }
                    notifyMovementBoundsChanged(false, false);
                    outBounds.set(postChangeStackBounds);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return z;
    }

    void setAdjustedForIme(boolean adjustedForIme, int imeHeight) {
        boolean imeShowing = adjustedForIme && imeHeight > 0;
        imeHeight = imeShowing ? imeHeight : false;
        if (imeShowing != this.mIsImeShowing || imeHeight != this.mImeHeight) {
            this.mIsImeShowing = imeShowing;
            this.mImeHeight = imeHeight;
            notifyImeVisibilityChanged(imeShowing, imeHeight);
            notifyMovementBoundsChanged(true, false);
        }
    }

    void setAdjustedForShelf(boolean adjustedForShelf, int shelfHeight) {
        boolean shelfShowing = adjustedForShelf && shelfHeight > 0;
        if (shelfShowing != this.mIsShelfShowing || shelfHeight != this.mShelfHeight) {
            this.mIsShelfShowing = shelfShowing;
            this.mShelfHeight = shelfHeight;
            notifyShelfVisibilityChanged(shelfShowing, shelfHeight);
            notifyMovementBoundsChanged(false, true);
        }
    }

    void setAspectRatio(float aspectRatio) {
        if (Float.compare(this.mAspectRatio, aspectRatio) != 0) {
            this.mAspectRatio = aspectRatio;
            notifyMovementBoundsChanged(false, false);
        }
    }

    float getAspectRatio() {
        return this.mAspectRatio;
    }

    void setActions(List<RemoteAction> actions) {
        this.mActions.clear();
        if (actions != null) {
            this.mActions.addAll(actions);
        }
        notifyActionsChanged(this.mActions);
    }

    private void notifyImeVisibilityChanged(boolean imeVisible, int imeHeight) {
        if (this.mPinnedStackListener != null) {
            try {
                this.mPinnedStackListener.onImeVisibilityChanged(imeVisible, imeHeight);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error delivering bounds changed event.", e);
            }
        }
    }

    private void notifyShelfVisibilityChanged(boolean shelfVisible, int shelfHeight) {
        if (this.mPinnedStackListener != null) {
            try {
                this.mPinnedStackListener.onShelfVisibilityChanged(shelfVisible, shelfHeight);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error delivering bounds changed event.", e);
            }
        }
    }

    private void notifyMinimizeChanged(boolean isMinimized) {
        if (this.mPinnedStackListener != null) {
            try {
                this.mPinnedStackListener.onMinimizedStateChanged(isMinimized);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error delivering minimize changed event.", e);
            }
        }
    }

    private void notifyActionsChanged(List<RemoteAction> actions) {
        if (this.mPinnedStackListener != null) {
            try {
                this.mPinnedStackListener.onActionsChanged(new ParceledListSlice(actions));
            } catch (RemoteException e) {
                Slog.e(TAG, "Error delivering actions changed event.", e);
            }
        }
    }

    private void notifyMovementBoundsChanged(boolean fromImeAdjustment, boolean fromShelfAdjustment) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mPinnedStackListener == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Rect insetBounds = new Rect();
                getInsetBounds(insetBounds);
                Rect normalBounds = getDefaultBounds(-1.0f);
                if (isValidPictureInPictureAspectRatio(this.mAspectRatio)) {
                    transformBoundsToAspectRatio(normalBounds, this.mAspectRatio, false);
                }
                Rect animatingBounds = this.mTmpAnimatingBoundsRect;
                TaskStack pinnedStack = this.mDisplayContent.getPinnedStack();
                if (pinnedStack != null) {
                    pinnedStack.getAnimationOrCurrentBounds(animatingBounds);
                } else {
                    animatingBounds.set(normalBounds);
                }
                this.mPinnedStackListener.onMovementBoundsChanged(insetBounds, normalBounds, animatingBounds, fromImeAdjustment, fromShelfAdjustment, this.mDisplayInfo.rotation);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error delivering actions changed event.", e);
            } catch (Throwable th) {
                while (true) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    private void getInsetBounds(Rect outRect) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mPolicy.getStableInsetsLw(this.mDisplayInfo.rotation, this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight, this.mDisplayInfo.displayCutout, this.mTmpInsets);
                outRect.set(this.mTmpInsets.left + this.mScreenEdgeInsets.x, this.mTmpInsets.top + this.mScreenEdgeInsets.y, (this.mDisplayInfo.logicalWidth - this.mTmpInsets.right) - this.mScreenEdgeInsets.x, (this.mDisplayInfo.logicalHeight - this.mTmpInsets.bottom) - this.mScreenEdgeInsets.y);
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private Rect getMovementBounds(Rect stackBounds) {
        Rect movementBounds;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                movementBounds = getMovementBounds(stackBounds, true, true);
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return movementBounds;
    }

    private Rect getMovementBounds(Rect stackBounds, boolean adjustForIme, boolean adjustForShelf) {
        Rect movementBounds;
        synchronized (this.mService.mWindowMap) {
            try {
                int i;
                WindowManagerService.boostPriorityForLockedSection();
                movementBounds = new Rect();
                getInsetBounds(movementBounds);
                PipSnapAlgorithm pipSnapAlgorithm = this.mSnapAlgorithm;
                int i2 = 0;
                if (adjustForIme && this.mIsImeShowing) {
                    i = this.mImeHeight;
                } else {
                    i = 0;
                }
                if (adjustForShelf && this.mIsShelfShowing) {
                    i2 = this.mShelfHeight;
                }
                pipSnapAlgorithm.getMovementBounds(stackBounds, movementBounds, movementBounds, Math.max(i, i2));
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return movementBounds;
    }

    private void applyMinimizedOffset(Rect stackBounds, Rect movementBounds) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mTmpDisplaySize.set(this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight);
                this.mService.getStableInsetsLocked(this.mDisplayContent.getDisplayId(), this.mStableInsets);
                this.mSnapAlgorithm.applyMinimizedOffset(stackBounds, movementBounds, this.mTmpDisplaySize, this.mStableInsets);
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private float getSnapFraction(Rect stackBounds) {
        return this.mSnapAlgorithm.getSnapFraction(stackBounds, getMovementBounds(stackBounds));
    }

    private int dpToPx(float dpValue, DisplayMetrics dm) {
        return (int) TypedValue.applyDimension(1, dpValue, dm);
    }

    void dump(String prefix, PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("PinnedStackController");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  defaultBounds=");
        pw.print(stringBuilder.toString());
        getDefaultBounds(-1.0f).printShortString(pw);
        pw.println();
        this.mService.getStackBounds(2, 1, this.mTmpRect);
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  movementBounds=");
        pw.print(stringBuilder.toString());
        getMovementBounds(this.mTmpRect).printShortString(pw);
        pw.println();
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mIsImeShowing=");
        stringBuilder.append(this.mIsImeShowing);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mImeHeight=");
        stringBuilder.append(this.mImeHeight);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mIsShelfShowing=");
        stringBuilder.append(this.mIsShelfShowing);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mShelfHeight=");
        stringBuilder.append(this.mShelfHeight);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mReentrySnapFraction=");
        stringBuilder.append(this.mReentrySnapFraction);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mIsMinimized=");
        stringBuilder.append(this.mIsMinimized);
        pw.println(stringBuilder.toString());
        if (this.mActions.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  mActions=[]");
            pw.println(stringBuilder.toString());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  mActions=[");
            pw.println(stringBuilder.toString());
            for (int i = 0; i < this.mActions.size(); i++) {
                RemoteAction action = (RemoteAction) this.mActions.get(i);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append("    Action[");
                stringBuilder2.append(i);
                stringBuilder2.append("]: ");
                pw.print(stringBuilder2.toString());
                action.dump(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, pw);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ]");
            pw.println(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(" mDisplayInfo=");
        stringBuilder.append(this.mDisplayInfo);
        pw.println(stringBuilder.toString());
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        getDefaultBounds(-1.0f).writeToProto(proto, 1146756268033L);
        this.mService.getStackBounds(2, 1, this.mTmpRect);
        getMovementBounds(this.mTmpRect).writeToProto(proto, 1146756268034L);
        proto.end(token);
    }
}
