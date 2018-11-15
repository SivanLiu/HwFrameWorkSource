package com.android.server.wm;

import android.animation.AnimationHandler;
import android.animation.AnimationHandler.AnimationFrameCallbackProvider;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.ArrayMap;
import android.view.Choreographer;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.graphics.SfVsyncFrameCallbackProvider;
import com.android.server.AnimationThread;

class SurfaceAnimationRunner {
    private final AnimationHandler mAnimationHandler;
    @GuardedBy("mLock")
    private boolean mAnimationStartDeferred;
    private final AnimatorFactory mAnimatorFactory;
    private boolean mApplyScheduled;
    private final Runnable mApplyTransactionRunnable;
    private final Object mCancelLock;
    @VisibleForTesting
    Choreographer mChoreographer;
    private final Transaction mFrameTransaction;
    private final Object mLock;
    @GuardedBy("mLock")
    @VisibleForTesting
    final ArrayMap<SurfaceControl, RunningAnimation> mPendingAnimations;
    @GuardedBy("mLock")
    @VisibleForTesting
    final ArrayMap<SurfaceControl, RunningAnimation> mRunningAnimations;

    @VisibleForTesting
    interface AnimatorFactory {
        ValueAnimator makeAnimator();
    }

    private static final class RunningAnimation {
        ValueAnimator mAnim;
        final AnimationSpec mAnimSpec;
        @GuardedBy("mCancelLock")
        private boolean mCancelled;
        final Runnable mFinishCallback;
        final SurfaceControl mLeash;

        RunningAnimation(AnimationSpec animSpec, SurfaceControl leash, Runnable finishCallback) {
            this.mAnimSpec = animSpec;
            this.mLeash = leash;
            this.mFinishCallback = finishCallback;
        }
    }

    private class SfValueAnimator extends ValueAnimator {
        SfValueAnimator() {
            setFloatValues(new float[]{0.0f, 1.0f});
        }

        public AnimationHandler getAnimationHandler() {
            return SurfaceAnimationRunner.this.mAnimationHandler;
        }
    }

    SurfaceAnimationRunner() {
        this(null, null, new Transaction());
    }

    @VisibleForTesting
    SurfaceAnimationRunner(AnimationFrameCallbackProvider callbackProvider, AnimatorFactory animatorFactory, Transaction frameTransaction) {
        AnimationFrameCallbackProvider animationFrameCallbackProvider;
        this.mLock = new Object();
        this.mCancelLock = new Object();
        this.mApplyTransactionRunnable = new -$$Lambda$SurfaceAnimationRunner$lSzwjoKEGADoEFOzdEnwriAk0T4(this);
        this.mPendingAnimations = new ArrayMap();
        this.mRunningAnimations = new ArrayMap();
        SurfaceAnimationThread.getHandler().runWithScissors(new -$$Lambda$SurfaceAnimationRunner$xDyZdsMrcbp64p4BQmOGPvVnSWA(this), 0);
        this.mFrameTransaction = frameTransaction;
        this.mAnimationHandler = new AnimationHandler();
        AnimationHandler animationHandler = this.mAnimationHandler;
        if (callbackProvider != null) {
            animationFrameCallbackProvider = callbackProvider;
        } else {
            animationFrameCallbackProvider = new SfVsyncFrameCallbackProvider(this.mChoreographer);
        }
        animationHandler.setProvider(animationFrameCallbackProvider);
        this.mAnimatorFactory = animatorFactory != null ? animatorFactory : new -$$Lambda$SurfaceAnimationRunner$we7K92eAl3biB_bzyqbv5xCmasE(this);
    }

    void deferStartingAnimations() {
        synchronized (this.mLock) {
            this.mAnimationStartDeferred = true;
        }
    }

    void continueStartingAnimations() {
        synchronized (this.mLock) {
            this.mAnimationStartDeferred = false;
            if (!this.mPendingAnimations.isEmpty()) {
                this.mChoreographer.postFrameCallback(new -$$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60(this));
            }
        }
    }

    void startAnimation(AnimationSpec a, SurfaceControl animationLeash, Transaction t, Runnable finishCallback) {
        synchronized (this.mLock) {
            RunningAnimation runningAnim = new RunningAnimation(a, animationLeash, finishCallback);
            this.mPendingAnimations.put(animationLeash, runningAnim);
            if (!this.mAnimationStartDeferred) {
                this.mChoreographer.postFrameCallback(new -$$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60(this));
            }
            applyTransformation(runningAnim, t, 0);
        }
    }

    /* JADX WARNING: Missing block: B:24:0x003a, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void onAnimationCancelled(SurfaceControl leash) {
        synchronized (this.mLock) {
            if (this.mPendingAnimations.containsKey(leash)) {
                this.mPendingAnimations.remove(leash);
                return;
            }
            RunningAnimation anim = (RunningAnimation) this.mRunningAnimations.get(leash);
            if (anim != null) {
                this.mRunningAnimations.remove(leash);
                synchronized (this.mCancelLock) {
                    anim.mCancelled = true;
                }
                SurfaceAnimationThread.getHandler().post(new -$$Lambda$SurfaceAnimationRunner$SGOilG6qRe0XTsTJRQqQKhta0pA(this, anim));
            }
        }
    }

    public static /* synthetic */ void lambda$onAnimationCancelled$2(SurfaceAnimationRunner surfaceAnimationRunner, RunningAnimation anim) {
        anim.mAnim.cancel();
        surfaceAnimationRunner.applyTransaction();
    }

    @GuardedBy("mLock")
    private void startPendingAnimationsLocked() {
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            startAnimationLocked((RunningAnimation) this.mPendingAnimations.valueAt(i));
        }
        this.mPendingAnimations.clear();
    }

    @GuardedBy("mLock")
    private void startAnimationLocked(final RunningAnimation a) {
        ValueAnimator anim = this.mAnimatorFactory.makeAnimator();
        anim.overrideDurationScale(1.0f);
        anim.setDuration(a.mAnimSpec.getDuration());
        anim.addUpdateListener(new -$$Lambda$SurfaceAnimationRunner$puhYAP5tF0mSSJva-eUz59HnrkA(this, a, anim));
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                synchronized (SurfaceAnimationRunner.this.mCancelLock) {
                    if (!a.mCancelled) {
                        SurfaceAnimationRunner.this.mFrameTransaction.show(a.mLeash);
                    }
                }
            }

            public void onAnimationEnd(Animator animation) {
                synchronized (SurfaceAnimationRunner.this.mLock) {
                    SurfaceAnimationRunner.this.mRunningAnimations.remove(a.mLeash);
                    synchronized (SurfaceAnimationRunner.this.mCancelLock) {
                        if (!a.mCancelled) {
                            AnimationThread.getHandler().post(a.mFinishCallback);
                        }
                    }
                }
            }
        });
        a.mAnim = anim;
        this.mRunningAnimations.put(a.mLeash, a);
        anim.start();
        if (a.mAnimSpec.canSkipFirstFrame()) {
            anim.setCurrentPlayTime(this.mChoreographer.getFrameIntervalNanos() / 1000000);
        }
        anim.doAnimationFrame(this.mChoreographer.getFrameTime());
    }

    public static /* synthetic */ void lambda$startAnimationLocked$3(SurfaceAnimationRunner surfaceAnimationRunner, RunningAnimation a, ValueAnimator anim, ValueAnimator animation) {
        synchronized (surfaceAnimationRunner.mCancelLock) {
            if (!a.mCancelled) {
                long duration = anim.getDuration();
                long currentPlayTime = anim.getCurrentPlayTime();
                if (currentPlayTime > duration) {
                    currentPlayTime = duration;
                }
                surfaceAnimationRunner.applyTransformation(a, surfaceAnimationRunner.mFrameTransaction, currentPlayTime);
            }
        }
        surfaceAnimationRunner.scheduleApplyTransaction();
    }

    private void applyTransformation(RunningAnimation a, Transaction t, long currentPlayTime) {
        if (a.mAnimSpec.needsEarlyWakeup()) {
            t.setEarlyWakeup();
        }
        a.mAnimSpec.apply(t, a.mLeash, currentPlayTime);
    }

    private void startAnimations(long frameTimeNanos) {
        synchronized (this.mLock) {
            startPendingAnimationsLocked();
        }
    }

    private void scheduleApplyTransaction() {
        if (!this.mApplyScheduled) {
            this.mChoreographer.postCallback(2, this.mApplyTransactionRunnable, null);
            this.mApplyScheduled = true;
        }
    }

    private void applyTransaction() {
        this.mFrameTransaction.setAnimationTransaction();
        this.mFrameTransaction.apply();
        this.mApplyScheduled = false;
    }
}
