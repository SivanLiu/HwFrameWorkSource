package com.android.server.wm;

import android.animation.AnimationHandler;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wm.WindowManagerInternal.AppTransitionListener;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ConcurrentModificationException;

public class BoundsAnimationController {
    private static final boolean DEBUG = false;
    private static final int DEBUG_ANIMATION_SLOW_DOWN_FACTOR = 1;
    private static final boolean DEBUG_LOCAL = false;
    private static final int DEFAULT_TRANSITION_DURATION = 425;
    public static final int NO_PIP_MODE_CHANGED_CALLBACKS = 0;
    public static final int SCHEDULE_PIP_MODE_CHANGED_ON_END = 2;
    public static final int SCHEDULE_PIP_MODE_CHANGED_ON_START = 1;
    private static final String TAG = "WindowManager";
    private static final int WAIT_FOR_DRAW_TIMEOUT_MS = 3000;
    private final AnimationHandler mAnimationHandler;
    private final AppTransition mAppTransition;
    private final AppTransitionNotifier mAppTransitionNotifier = new AppTransitionNotifier();
    private final Interpolator mFastOutSlowInInterpolator;
    private boolean mFinishAnimationAfterTransition = false;
    private final Handler mHandler;
    private ArrayMap<BoundsAnimationTarget, BoundsAnimator> mRunningAnimations = new ArrayMap();

    @VisibleForTesting
    final class BoundsAnimator extends ValueAnimator implements AnimatorUpdateListener, AnimatorListener {
        private final Rect mFrom = new Rect();
        private final int mFrozenTaskHeight;
        private final int mFrozenTaskWidth;
        private boolean mMoveFromFullscreen;
        private boolean mMoveToFullscreen;
        private int mPrevSchedulePipModeChangedState;
        private final Runnable mResumeRunnable = new -$$Lambda$BoundsAnimationController$BoundsAnimator$eIPNx9WcD7moTPCByy2XhPMSdCs(this);
        private int mSchedulePipModeChangedState;
        private boolean mSkipAnimationEnd;
        private boolean mSkipFinalResize;
        private final BoundsAnimationTarget mTarget;
        private final Rect mTmpRect = new Rect();
        private final Rect mTmpTaskBounds = new Rect();
        private final Rect mTo = new Rect();

        BoundsAnimator(BoundsAnimationTarget target, Rect from, Rect to, int schedulePipModeChangedState, int prevShedulePipModeChangedState, boolean moveFromFullscreen, boolean moveToFullscreen) {
            this.mTarget = target;
            this.mFrom.set(from);
            this.mTo.set(to);
            this.mSchedulePipModeChangedState = schedulePipModeChangedState;
            this.mPrevSchedulePipModeChangedState = prevShedulePipModeChangedState;
            this.mMoveFromFullscreen = moveFromFullscreen;
            this.mMoveToFullscreen = moveToFullscreen;
            addUpdateListener(this);
            addListener(this);
            if (animatingToLargerSize()) {
                this.mFrozenTaskWidth = this.mTo.width();
                this.mFrozenTaskHeight = this.mTo.height();
                return;
            }
            this.mFrozenTaskWidth = this.mFrom.width();
            this.mFrozenTaskHeight = this.mFrom.height();
        }

        public void onAnimationStart(Animator animation) {
            BoundsAnimationController.this.mFinishAnimationAfterTransition = false;
            this.mTmpRect.set(this.mFrom.left, this.mFrom.top, this.mFrom.left + this.mFrozenTaskWidth, this.mFrom.top + this.mFrozenTaskHeight);
            BoundsAnimationController.this.updateBooster();
            boolean z = true;
            if (this.mPrevSchedulePipModeChangedState == 0) {
                BoundsAnimationTarget boundsAnimationTarget = this.mTarget;
                if (this.mSchedulePipModeChangedState != 1) {
                    z = false;
                }
                boundsAnimationTarget.onAnimationStart(z, false);
                if (this.mMoveFromFullscreen && this.mTarget.shouldDeferStartOnMoveToFullscreen()) {
                    pause();
                }
            } else if (this.mPrevSchedulePipModeChangedState == 2 && this.mSchedulePipModeChangedState == 1) {
                this.mTarget.onAnimationStart(true, true);
            }
            if (animatingToLargerSize()) {
                this.mTarget.setPinnedStackSize(this.mFrom, this.mTmpRect);
                if (this.mMoveToFullscreen) {
                    pause();
                }
            }
        }

        public void pause() {
            super.pause();
            BoundsAnimationController.this.mHandler.postDelayed(this.mResumeRunnable, 3000);
        }

        public void resume() {
            BoundsAnimationController.this.mHandler.removeCallbacks(this.mResumeRunnable);
            super.resume();
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            float value = ((Float) animation.getAnimatedValue()).floatValue();
            float remains = 1.0f - value;
            this.mTmpRect.left = (int) (((((float) this.mFrom.left) * remains) + (((float) this.mTo.left) * value)) + 0.5f);
            this.mTmpRect.top = (int) (((((float) this.mFrom.top) * remains) + (((float) this.mTo.top) * value)) + 0.5f);
            this.mTmpRect.right = (int) (((((float) this.mFrom.right) * remains) + (((float) this.mTo.right) * value)) + 0.5f);
            this.mTmpRect.bottom = (int) (((((float) this.mFrom.bottom) * remains) + (((float) this.mTo.bottom) * value)) + 0.5f);
            this.mTmpTaskBounds.set(this.mTmpRect.left, this.mTmpRect.top, this.mTmpRect.left + this.mFrozenTaskWidth, this.mTmpRect.top + this.mFrozenTaskHeight);
            if (!this.mTarget.setPinnedStackSize(this.mTmpRect, this.mTmpTaskBounds)) {
                if (this.mSchedulePipModeChangedState == 1) {
                    this.mSchedulePipModeChangedState = 2;
                }
                cancelAndCallAnimationEnd();
            }
        }

        public void onAnimationEnd(Animator animation) {
            boolean z = true;
            if (!BoundsAnimationController.this.mAppTransition.isRunning() || BoundsAnimationController.this.mFinishAnimationAfterTransition) {
                if (!this.mSkipAnimationEnd) {
                    BoundsAnimationTarget boundsAnimationTarget = this.mTarget;
                    if (this.mSchedulePipModeChangedState != 2) {
                        z = false;
                    }
                    boundsAnimationTarget.onAnimationEnd(z, !this.mSkipFinalResize ? this.mTo : null, this.mMoveToFullscreen);
                }
                removeListener(this);
                removeUpdateListener(this);
                try {
                    BoundsAnimationController.this.mRunningAnimations.remove(this.mTarget);
                } catch (ConcurrentModificationException e) {
                    String str = BoundsAnimationController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onAnimationEnd: mTarget=");
                    stringBuilder.append(this.mTarget);
                    stringBuilder.append(" mSkipFinalResize=");
                    stringBuilder.append(this.mSkipFinalResize);
                    stringBuilder.append(" mFinishAnimationAfterTransition=");
                    stringBuilder.append(BoundsAnimationController.this.mFinishAnimationAfterTransition);
                    stringBuilder.append(" mAppTransitionIsRunning=");
                    stringBuilder.append(BoundsAnimationController.this.mAppTransition.isRunning());
                    stringBuilder.append(" callers=");
                    stringBuilder.append(Debug.getCallers(2));
                    Slog.d(str, stringBuilder.toString());
                }
                BoundsAnimationController.this.updateBooster();
                return;
            }
            BoundsAnimationController.this.mFinishAnimationAfterTransition = true;
        }

        public void onAnimationCancel(Animator animation) {
            this.mSkipFinalResize = true;
            this.mMoveToFullscreen = false;
        }

        private void cancelAndCallAnimationEnd() {
            this.mSkipAnimationEnd = false;
            super.cancel();
        }

        public void cancel() {
            this.mSkipAnimationEnd = true;
            super.cancel();
        }

        boolean isAnimatingTo(Rect bounds) {
            return this.mTo.equals(bounds);
        }

        @VisibleForTesting
        boolean animatingToLargerSize() {
            return this.mFrom.width() * this.mFrom.height() <= this.mTo.width() * this.mTo.height();
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public AnimationHandler getAnimationHandler() {
            if (BoundsAnimationController.this.mAnimationHandler != null) {
                return BoundsAnimationController.this.mAnimationHandler;
            }
            return super.getAnimationHandler();
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SchedulePipModeChangedState {
    }

    private final class AppTransitionNotifier extends AppTransitionListener implements Runnable {
        private AppTransitionNotifier() {
        }

        public void onAppTransitionCancelledLocked() {
            animationFinished();
        }

        public void onAppTransitionFinishedLocked(IBinder token) {
            animationFinished();
        }

        private void animationFinished() {
            if (BoundsAnimationController.this.mFinishAnimationAfterTransition) {
                BoundsAnimationController.this.mHandler.removeCallbacks(this);
                BoundsAnimationController.this.mHandler.post(this);
            }
        }

        public void run() {
            for (int i = 0; i < BoundsAnimationController.this.mRunningAnimations.size(); i++) {
                ((BoundsAnimator) BoundsAnimationController.this.mRunningAnimations.valueAt(i)).onAnimationEnd(null);
            }
        }
    }

    BoundsAnimationController(Context context, AppTransition transition, Handler handler, AnimationHandler animationHandler) {
        this.mHandler = handler;
        this.mAppTransition = transition;
        this.mAppTransition.registerListenerLocked(this.mAppTransitionNotifier);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.mAnimationHandler = animationHandler;
    }

    public void animateBounds(BoundsAnimationTarget target, Rect from, Rect to, int animationDuration, int schedulePipModeChangedState, boolean moveFromFullscreen, boolean moveToFullscreen) {
        animateBoundsImpl(target, from, to, animationDuration, schedulePipModeChangedState, moveFromFullscreen, moveToFullscreen);
    }

    @VisibleForTesting
    BoundsAnimator animateBoundsImpl(BoundsAnimationTarget target, Rect from, Rect to, int animationDuration, int schedulePipModeChangedState, boolean moveFromFullscreen, boolean moveToFullscreen) {
        Rect rect;
        int schedulePipModeChangedState2;
        boolean moveFromFullscreen2;
        boolean moveToFullscreen2;
        BoundsAnimationTarget boundsAnimationTarget = target;
        int schedulePipModeChangedState3 = schedulePipModeChangedState;
        BoundsAnimator existing = (BoundsAnimator) this.mRunningAnimations.get(boundsAnimationTarget);
        int prevSchedulePipModeChangedState = 0;
        if (existing != null) {
            rect = to;
            if (existing.isAnimatingTo(rect) && ((!moveToFullscreen || existing.mMoveToFullscreen) && (!moveFromFullscreen || existing.mMoveFromFullscreen))) {
                return existing;
            }
            boolean moveFromFullscreen3;
            boolean moveToFullscreen3;
            prevSchedulePipModeChangedState = existing.mSchedulePipModeChangedState;
            if (existing.mSchedulePipModeChangedState == 1) {
                if (schedulePipModeChangedState3 != 1) {
                    schedulePipModeChangedState3 = 2;
                }
            } else if (existing.mSchedulePipModeChangedState == 2 && schedulePipModeChangedState3 != 1) {
                schedulePipModeChangedState3 = 2;
            }
            if (moveFromFullscreen || moveToFullscreen) {
                moveFromFullscreen3 = moveFromFullscreen;
                moveToFullscreen3 = moveToFullscreen;
            } else {
                moveToFullscreen3 = existing.mMoveToFullscreen;
                moveFromFullscreen3 = existing.mMoveFromFullscreen;
            }
            existing.cancel();
            schedulePipModeChangedState2 = schedulePipModeChangedState3;
            moveFromFullscreen2 = moveFromFullscreen3;
            moveToFullscreen2 = moveToFullscreen3;
        } else {
            rect = to;
            moveFromFullscreen2 = moveFromFullscreen;
            moveToFullscreen2 = moveToFullscreen;
            schedulePipModeChangedState2 = schedulePipModeChangedState3;
        }
        BoundsAnimator animator = new BoundsAnimator(boundsAnimationTarget, from, rect, schedulePipModeChangedState2, prevSchedulePipModeChangedState, moveFromFullscreen2, moveToFullscreen2);
        this.mRunningAnimations.put(boundsAnimationTarget, animator);
        animator.setFloatValues(new float[]{0.0f, 1.0f});
        int i = animationDuration;
        animator.setDuration((long) ((i != -1 ? i : DEFAULT_TRANSITION_DURATION) * 1));
        animator.setInterpolator(this.mFastOutSlowInInterpolator);
        animator.start();
        return animator;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public void onAllWindowsDrawn() {
        this.mHandler.post(new -$$Lambda$BoundsAnimationController$MoVv_WhxoMrTVo-xz1qu2FMcYrM(this));
    }

    private void resume() {
        for (int i = 0; i < this.mRunningAnimations.size(); i++) {
            ((BoundsAnimator) this.mRunningAnimations.valueAt(i)).resume();
        }
    }

    private void updateBooster() {
        WindowManagerService.sThreadPriorityBooster.setBoundsAnimationRunning(this.mRunningAnimations.isEmpty() ^ 1);
    }
}
