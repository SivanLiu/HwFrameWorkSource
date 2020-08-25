package com.android.server.wm;

import android.animation.AnimationHandler;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.util.ArrayMap;
import android.view.Choreographer;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.graphics.SfVsyncFrameCallbackProvider;
import com.android.server.wm.BoundsAnimationController;
import com.android.server.wm.WindowManagerInternal;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class BoundsAnimationController {
    public static final int BOUNDS = 0;
    private static final boolean DEBUG = false;
    private static final int DEBUG_ANIMATION_SLOW_DOWN_FACTOR = 1;
    private static final boolean DEBUG_LOCAL = false;
    private static final int DEFAULT_TRANSITION_DURATION = 425;
    public static final int FADE_IN = 1;
    private static final int FADE_IN_DURATION = 500;
    public static final int NO_PIP_MODE_CHANGED_CALLBACKS = 0;
    public static final int SCHEDULE_PIP_MODE_CHANGED_ON_END = 2;
    public static final int SCHEDULE_PIP_MODE_CHANGED_ON_START = 1;
    private static final String TAG = "WindowManager";
    private static final int WAIT_FOR_DRAW_TIMEOUT_MS = 3000;
    /* access modifiers changed from: private */
    public final AnimationHandler mAnimationHandler;
    @AnimationType
    private int mAnimationType;
    /* access modifiers changed from: private */
    public final AppTransition mAppTransition;
    private final AppTransitionNotifier mAppTransitionNotifier = new AppTransitionNotifier();
    private Choreographer mChoreographer;
    private final Interpolator mFastOutSlowInInterpolator;
    /* access modifiers changed from: private */
    public boolean mFinishAnimationAfterTransition = false;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    /* access modifiers changed from: private */
    public ArrayMap<BoundsAnimationTarget, BoundsAnimator> mRunningAnimations = new ArrayMap<>();

    public @interface AnimationType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SchedulePipModeChangedState {
    }

    private final class AppTransitionNotifier extends WindowManagerInternal.AppTransitionListener implements Runnable {
        private AppTransitionNotifier() {
        }

        public void onAppTransitionCancelledLocked() {
            animationFinished();
        }

        @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
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
        if (animationHandler != null) {
            handler.post(new Runnable(animationHandler) {
                /* class com.android.server.wm.$$Lambda$BoundsAnimationController$3yWz6AXIW5r1KElGtHEgHZdi5Q */
                private final /* synthetic */ AnimationHandler f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    BoundsAnimationController.this.lambda$new$0$BoundsAnimationController(this.f$1);
                }
            });
        }
    }

    public /* synthetic */ void lambda$new$0$BoundsAnimationController(AnimationHandler animationHandler) {
        this.mChoreographer = Choreographer.getSfInstance();
        animationHandler.setProvider(new SfVsyncFrameCallbackProvider(this.mChoreographer));
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public final class BoundsAnimator extends ValueAnimator implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        /* access modifiers changed from: private */
        @AnimationType
        public final int mAnimationType;
        private final Rect mFrom = new Rect();
        /* access modifiers changed from: private */
        public int mFrozenTaskHeight;
        /* access modifiers changed from: private */
        public int mFrozenTaskWidth;
        /* access modifiers changed from: private */
        public boolean mMoveFromFullscreen;
        /* access modifiers changed from: private */
        public boolean mMoveToFullscreen;
        private int mPrevSchedulePipModeChangedState;
        private final Runnable mResumeRunnable = new Runnable() {
            /* class com.android.server.wm.$$Lambda$BoundsAnimationController$BoundsAnimator$eIPNx9WcD7moTPCByy2XhPMSdCs */

            public final void run() {
                BoundsAnimator.this.lambda$new$0$BoundsAnimationController$BoundsAnimator();
            }
        };
        /* access modifiers changed from: private */
        public int mSchedulePipModeChangedState;
        private boolean mSkipAnimationEnd;
        private boolean mSkipFinalResize;
        private final BoundsAnimationTarget mTarget;
        private final Rect mTmpRect = new Rect();
        private final Rect mTmpTaskBounds = new Rect();
        private final Rect mTo = new Rect();

        BoundsAnimator(BoundsAnimationTarget target, @AnimationType int animationType, Rect from, Rect to, int schedulePipModeChangedState, int prevShedulePipModeChangedState, boolean moveFromFullscreen, boolean moveToFullscreen, Rect frozenTask) {
            this.mTarget = target;
            this.mAnimationType = animationType;
            this.mFrom.set(from);
            this.mTo.set(to);
            this.mSchedulePipModeChangedState = schedulePipModeChangedState;
            this.mPrevSchedulePipModeChangedState = prevShedulePipModeChangedState;
            this.mMoveFromFullscreen = moveFromFullscreen;
            this.mMoveToFullscreen = moveToFullscreen;
            addUpdateListener(this);
            addListener(this);
            if (this.mAnimationType != 0) {
                return;
            }
            if (animatingToLargerSize()) {
                this.mFrozenTaskWidth = this.mTo.width();
                this.mFrozenTaskHeight = this.mTo.height();
                return;
            }
            this.mFrozenTaskWidth = frozenTask.isEmpty() ? this.mFrom.width() : frozenTask.width();
            this.mFrozenTaskHeight = frozenTask.isEmpty() ? this.mFrom.height() : frozenTask.height();
        }

        public void onAnimationStart(Animator animation) {
            boolean continueAnimation;
            boolean unused = BoundsAnimationController.this.mFinishAnimationAfterTransition = false;
            this.mTmpRect.set(this.mFrom.left, this.mFrom.top, this.mFrom.left + this.mFrozenTaskWidth, this.mFrom.top + this.mFrozenTaskHeight);
            BoundsAnimationController.this.updateBooster();
            int i = this.mPrevSchedulePipModeChangedState;
            boolean z = true;
            if (i == 0) {
                BoundsAnimationTarget boundsAnimationTarget = this.mTarget;
                if (this.mSchedulePipModeChangedState != 1) {
                    z = false;
                }
                continueAnimation = boundsAnimationTarget.onAnimationStart(z, false, this.mAnimationType);
                if (continueAnimation && this.mMoveFromFullscreen && this.mTarget.shouldDeferStartOnMoveToFullscreen()) {
                    pause();
                }
            } else {
                continueAnimation = (i == 2 && this.mSchedulePipModeChangedState == 1) ? this.mTarget.onAnimationStart(true, true, this.mAnimationType) : this.mTarget.isAttached();
            }
            if (!continueAnimation) {
                cancel();
            } else if (animatingToLargerSize()) {
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

        /* renamed from: resume */
        public void lambda$new$0$BoundsAnimationController$BoundsAnimator() {
            BoundsAnimationController.this.mHandler.removeCallbacks(this.mResumeRunnable);
            super.resume();
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            float value = ((Float) animation.getAnimatedValue()).floatValue();
            if (this.mAnimationType != 1) {
                float remains = 1.0f - value;
                this.mTmpRect.left = (int) ((((float) this.mFrom.left) * remains) + (((float) this.mTo.left) * value) + 0.5f);
                this.mTmpRect.top = (int) ((((float) this.mFrom.top) * remains) + (((float) this.mTo.top) * value) + 0.5f);
                this.mTmpRect.right = (int) ((((float) this.mFrom.right) * remains) + (((float) this.mTo.right) * value) + 0.5f);
                this.mTmpRect.bottom = (int) ((((float) this.mFrom.bottom) * remains) + (((float) this.mTo.bottom) * value) + 0.5f);
                this.mTmpTaskBounds.set(this.mTmpRect.left, this.mTmpRect.top, this.mTmpRect.left + this.mFrozenTaskWidth, this.mTmpRect.top + this.mFrozenTaskHeight);
                if (!this.mTarget.setPinnedStackSize(this.mTmpRect, this.mTmpTaskBounds)) {
                    if (this.mSchedulePipModeChangedState == 1) {
                        this.mSchedulePipModeChangedState = 2;
                    }
                    cancelAndCallAnimationEnd();
                }
            } else if (!this.mTarget.setPinnedStackAlpha(value)) {
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
                BoundsAnimationController.this.mRunningAnimations.remove(this.mTarget);
                BoundsAnimationController.this.updateBooster();
                return;
            }
            boolean unused = BoundsAnimationController.this.mFinishAnimationAfterTransition = true;
        }

        public void onAnimationCancel(Animator animation) {
            this.mSkipFinalResize = true;
            this.mMoveToFullscreen = false;
        }

        /* access modifiers changed from: private */
        public void cancelAndCallAnimationEnd() {
            this.mSkipAnimationEnd = false;
            super.cancel();
        }

        public void cancel() {
            this.mSkipAnimationEnd = true;
            super.cancel();
            BoundsAnimationController.this.updateBooster();
        }

        /* access modifiers changed from: package-private */
        public boolean isAnimatingTo(Rect bounds) {
            return this.mTo.equals(bounds);
        }

        /* access modifiers changed from: package-private */
        @VisibleForTesting
        public boolean animatingToLargerSize() {
            return this.mFrom.width() * this.mFrom.height() < this.mTo.width() * this.mTo.height();
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

    public void animateBounds(BoundsAnimationTarget target, Rect from, Rect to, int animationDuration, int schedulePipModeChangedState, boolean moveFromFullscreen, boolean moveToFullscreen, @AnimationType int animationType) {
        animateBoundsImpl(target, from, to, animationDuration, schedulePipModeChangedState, moveFromFullscreen, moveToFullscreen, animationType);
    }

    /* access modifiers changed from: package-private */
    public void cancel(BoundsAnimationTarget target) {
        BoundsAnimator existing = this.mRunningAnimations.get(target);
        if (existing != null) {
            existing.cancelAndCallAnimationEnd();
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public BoundsAnimator animateBoundsImpl(BoundsAnimationTarget target, Rect from, Rect to, int animationDuration, int schedulePipModeChangedState, boolean moveFromFullscreen, boolean moveToFullscreen, @AnimationType int animationType) {
        int animationType2;
        int prevSchedulePipModeChangedState;
        boolean moveToFullscreen2;
        boolean moveFromFullscreen2;
        int schedulePipModeChangedState2;
        long j;
        boolean moveFromFullscreen3;
        boolean moveToFullscreen3;
        int schedulePipModeChangedState3 = schedulePipModeChangedState;
        BoundsAnimator existing = this.mRunningAnimations.get(target);
        if (isRunningFadeInAnimation(target) && from.width() == to.width() && from.height() == to.height()) {
            animationType2 = 1;
        } else {
            animationType2 = animationType;
        }
        boolean replacing = existing != null;
        Rect frozenTask = new Rect();
        if (!replacing) {
            moveFromFullscreen2 = moveFromFullscreen;
            moveToFullscreen2 = moveToFullscreen;
            schedulePipModeChangedState2 = schedulePipModeChangedState3;
            prevSchedulePipModeChangedState = 0;
        } else if (existing.isAnimatingTo(to) && ((!moveToFullscreen || existing.mMoveToFullscreen) && (!moveFromFullscreen || existing.mMoveFromFullscreen))) {
            return existing;
        } else {
            int prevSchedulePipModeChangedState2 = existing.mSchedulePipModeChangedState;
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
            frozenTask.set(0, 0, existing.mFrozenTaskWidth, existing.mFrozenTaskHeight);
            existing.cancel();
            schedulePipModeChangedState2 = schedulePipModeChangedState3;
            prevSchedulePipModeChangedState = prevSchedulePipModeChangedState2;
            moveToFullscreen2 = moveToFullscreen3;
            moveFromFullscreen2 = moveFromFullscreen3;
        }
        if (animationType2 == 1) {
            target.setPinnedStackSize(to, null);
        }
        BoundsAnimator animator = new BoundsAnimator(target, animationType2, from, to, schedulePipModeChangedState2, prevSchedulePipModeChangedState, moveFromFullscreen2, moveToFullscreen2, frozenTask);
        this.mRunningAnimations.put(target, animator);
        animator.setFloatValues(0.0f, 1.0f);
        if (animationType2 == 1) {
            j = 500;
        } else {
            j = (long) ((animationDuration != -1 ? animationDuration : DEFAULT_TRANSITION_DURATION) * 1);
        }
        animator.setDuration(j);
        animator.setInterpolator(this.mFastOutSlowInInterpolator);
        animator.start();
        return animator;
    }

    public void setAnimationType(@AnimationType int animationType) {
        this.mAnimationType = animationType;
    }

    @AnimationType
    public int getAnimationType() {
        int animationType = this.mAnimationType;
        this.mAnimationType = 0;
        return animationType;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public void onAllWindowsDrawn() {
        this.mHandler.post(new Runnable() {
            /* class com.android.server.wm.$$Lambda$BoundsAnimationController$MoVv_WhxoMrTVoxz1qu2FMcYrM */

            public final void run() {
                BoundsAnimationController.this.resume();
            }
        });
    }

    private boolean isRunningFadeInAnimation(BoundsAnimationTarget target) {
        BoundsAnimator existing = this.mRunningAnimations.get(target);
        return existing != null && existing.mAnimationType == 1 && existing.isStarted();
    }

    /* access modifiers changed from: private */
    public void resume() {
        for (int i = 0; i < this.mRunningAnimations.size(); i++) {
            this.mRunningAnimations.valueAt(i).lambda$new$0$BoundsAnimationController$BoundsAnimator();
        }
    }

    /* access modifiers changed from: private */
    public void updateBooster() {
        WindowManagerService.sThreadPriorityBooster.setBoundsAnimationRunning(!this.mRunningAnimations.isEmpty());
    }
}
