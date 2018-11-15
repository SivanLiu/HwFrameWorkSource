package com.android.server.wm;

import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Builder;
import android.view.SurfaceControl.Transaction;
import com.android.internal.annotations.VisibleForTesting;
import java.io.PrintWriter;

class SurfaceAnimator {
    private static final String TAG = "WindowManager";
    private final Animatable mAnimatable;
    private AnimationAdapter mAnimation;
    @VisibleForTesting
    final Runnable mAnimationFinishedCallback;
    private boolean mAnimationStartDelayed;
    private final OnAnimationFinishedCallback mInnerAnimationFinishedCallback;
    @VisibleForTesting
    SurfaceControl mLeash;
    private final WindowManagerService mService;

    interface Animatable {
        void commitPendingTransaction();

        SurfaceControl getAnimationLeashParent();

        SurfaceControl getParentSurfaceControl();

        Transaction getPendingTransaction();

        SurfaceControl getSurfaceControl();

        int getSurfaceHeight();

        int getSurfaceWidth();

        Builder makeAnimationLeash();

        void onAnimationLeashCreated(Transaction transaction, SurfaceControl surfaceControl);

        void onAnimationLeashDestroyed(Transaction transaction);

        boolean shouldDeferAnimationFinish(Runnable endDeferFinishCallback) {
            return false;
        }
    }

    interface OnAnimationFinishedCallback {
        void onAnimationFinished(AnimationAdapter animationAdapter);
    }

    SurfaceAnimator(Animatable animatable, Runnable animationFinishedCallback, WindowManagerService service) {
        this.mAnimatable = animatable;
        this.mService = service;
        this.mAnimationFinishedCallback = animationFinishedCallback;
        this.mInnerAnimationFinishedCallback = getFinishedCallback(animationFinishedCallback);
    }

    private OnAnimationFinishedCallback getFinishedCallback(Runnable animationFinishedCallback) {
        return new -$$Lambda$SurfaceAnimator$vdRZk66hQVbQCvVXEaQCT1kVmFc(this, animationFinishedCallback);
    }

    /* JADX WARNING: Missing block: B:19:0x0038, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:20:0x003b, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$getFinishedCallback$1(SurfaceAnimator surfaceAnimator, Runnable animationFinishedCallback, AnimationAdapter anim) {
        synchronized (surfaceAnimator.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                SurfaceAnimator target = (SurfaceAnimator) surfaceAnimator.mService.mAnimationTransferMap.remove(anim);
                if (target != null) {
                    target.mInnerAnimationFinishedCallback.onAnimationFinished(anim);
                } else if (anim != surfaceAnimator.mAnimation) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    Runnable resetAndInvokeFinish = new -$$Lambda$SurfaceAnimator$SIBia0mND666K8lMCPsoid8pUTI(surfaceAnimator, animationFinishedCallback);
                    if (!surfaceAnimator.mAnimatable.shouldDeferAnimationFinish(resetAndInvokeFinish)) {
                        resetAndInvokeFinish.run();
                    }
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public static /* synthetic */ void lambda$getFinishedCallback$0(SurfaceAnimator surfaceAnimator, Runnable animationFinishedCallback) {
        surfaceAnimator.reset(surfaceAnimator.mAnimatable.getPendingTransaction(), true);
        if (animationFinishedCallback != null) {
            animationFinishedCallback.run();
        }
    }

    void startAnimation(Transaction t, AnimationAdapter anim, boolean hidden) {
        cancelAnimation(t, true, true);
        this.mAnimation = anim;
        SurfaceControl surface = this.mAnimatable.getSurfaceControl();
        if (surface == null) {
            Slog.w(TAG, "Unable to start animation, surface is null or no children.");
            cancelAnimation();
            return;
        }
        this.mLeash = createAnimationLeash(surface, t, this.mAnimatable.getSurfaceWidth(), this.mAnimatable.getSurfaceHeight(), hidden);
        this.mAnimatable.onAnimationLeashCreated(t, this.mLeash);
        if (!this.mAnimationStartDelayed) {
            this.mAnimation.startAnimation(this.mLeash, t, this.mInnerAnimationFinishedCallback);
        }
    }

    void startDelayingAnimationStart() {
        if (!isAnimating()) {
            this.mAnimationStartDelayed = true;
        }
    }

    void endDelayingAnimationStart() {
        boolean delayed = this.mAnimationStartDelayed;
        this.mAnimationStartDelayed = false;
        if (delayed && this.mAnimation != null) {
            this.mAnimation.startAnimation(this.mLeash, this.mAnimatable.getPendingTransaction(), this.mInnerAnimationFinishedCallback);
            this.mAnimatable.commitPendingTransaction();
        }
    }

    boolean isAnimating() {
        return this.mAnimation != null;
    }

    AnimationAdapter getAnimation() {
        return this.mAnimation;
    }

    void cancelAnimation() {
        cancelAnimation(this.mAnimatable.getPendingTransaction(), false, true);
        this.mAnimatable.commitPendingTransaction();
    }

    void setLayer(Transaction t, int layer) {
        t.setLayer(this.mLeash != null ? this.mLeash : this.mAnimatable.getSurfaceControl(), layer);
    }

    void setRelativeLayer(Transaction t, SurfaceControl relativeTo, int layer) {
        t.setRelativeLayer(this.mLeash != null ? this.mLeash : this.mAnimatable.getSurfaceControl(), relativeTo, layer);
    }

    void reparent(Transaction t, SurfaceControl newParent) {
        t.reparent(this.mLeash != null ? this.mLeash : this.mAnimatable.getSurfaceControl(), newParent.getHandle());
    }

    boolean hasLeash() {
        return this.mLeash != null;
    }

    void transferAnimation(SurfaceAnimator from) {
        if (from.mLeash != null) {
            SurfaceControl surface = this.mAnimatable.getSurfaceControl();
            SurfaceControl parent = this.mAnimatable.getAnimationLeashParent();
            if (surface == null || parent == null) {
                Slog.w(TAG, "Unable to transfer animation, surface or parent is null");
                cancelAnimation();
                return;
            }
            endDelayingAnimationStart();
            Transaction t = this.mAnimatable.getPendingTransaction();
            cancelAnimation(t, true, true);
            this.mLeash = from.mLeash;
            this.mAnimation = from.mAnimation;
            from.cancelAnimation(t, false, false);
            t.reparent(surface, this.mLeash.getHandle());
            t.reparent(this.mLeash, parent.getHandle());
            this.mAnimatable.onAnimationLeashCreated(t, this.mLeash);
            this.mService.mAnimationTransferMap.put(this.mAnimation, this);
        }
    }

    boolean isAnimationStartDelayed() {
        return this.mAnimationStartDelayed;
    }

    private void cancelAnimation(Transaction t, boolean restarting, boolean forwardCancel) {
        SurfaceControl leash = this.mLeash;
        AnimationAdapter animation = this.mAnimation;
        reset(t, forwardCancel);
        if (animation != null) {
            if (!this.mAnimationStartDelayed && forwardCancel) {
                animation.onAnimationCancelled(leash);
            }
            if (!restarting) {
                this.mAnimationFinishedCallback.run();
            }
        }
        if (!restarting) {
            this.mAnimationStartDelayed = false;
        }
    }

    private void reset(Transaction t, boolean destroyLeash) {
        SurfaceControl surface = this.mAnimatable.getSurfaceControl();
        SurfaceControl parent = this.mAnimatable.getParentSurfaceControl();
        boolean scheduleAnim = false;
        boolean destroy = (this.mLeash == null || surface == null || parent == null) ? false : true;
        if (destroy) {
            t.reparent(surface, parent.getHandle());
            scheduleAnim = true;
        }
        this.mService.mAnimationTransferMap.remove(this.mAnimation);
        if (this.mLeash != null && destroyLeash) {
            t.destroy(this.mLeash);
            scheduleAnim = true;
        }
        this.mLeash = null;
        this.mAnimation = null;
        if (destroy) {
            this.mAnimatable.onAnimationLeashDestroyed(t);
            scheduleAnim = true;
        }
        if (scheduleAnim) {
            this.mService.scheduleAnimationLocked();
        }
    }

    private SurfaceControl createAnimationLeash(SurfaceControl surface, Transaction t, int width, int height, boolean hidden) {
        Builder parent = this.mAnimatable.makeAnimationLeash().setParent(this.mAnimatable.getAnimationLeashParent());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(surface);
        stringBuilder.append(" - animation-leash");
        SurfaceControl leash = parent.setName(stringBuilder.toString()).setSize(width, height).build();
        if (!hidden) {
            t.show(leash);
        }
        t.reparent(surface, leash.getHandle());
        return leash;
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        if (this.mAnimation != null) {
            this.mAnimation.writeToProto(proto, 1146756268035L);
        }
        if (this.mLeash != null) {
            this.mLeash.writeToProto(proto, 1146756268033L);
        }
        proto.write(1133871366146L, this.mAnimationStartDelayed);
        proto.end(token);
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mLeash=");
        pw.print(this.mLeash);
        if (this.mAnimationStartDelayed) {
            pw.print(" mAnimationStartDelayed=");
            pw.println(this.mAnimationStartDelayed);
        } else {
            pw.println();
        }
        pw.print(prefix);
        pw.println("Animation:");
        if (this.mAnimation != null) {
            AnimationAdapter animationAdapter = this.mAnimation;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            animationAdapter.dump(pw, stringBuilder.toString());
            return;
        }
        pw.print(prefix);
        pw.println("null");
    }
}
