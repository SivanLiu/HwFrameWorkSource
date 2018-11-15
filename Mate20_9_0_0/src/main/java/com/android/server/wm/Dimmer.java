package com.android.server.wm;

import android.graphics.Rect;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Builder;
import android.view.SurfaceControl.Transaction;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.os.HwBootFail;
import java.io.PrintWriter;

class Dimmer {
    private static final int DEFAULT_DIM_ANIM_DURATION = 200;
    private static final String TAG = "WindowManager";
    @VisibleForTesting
    DimState mDimState;
    private WindowContainer mHost;
    private WindowContainer mLastRequestedDimContainer;
    private final SurfaceAnimatorStarter mSurfaceAnimatorStarter;

    @VisibleForTesting
    class DimState {
        boolean isVisible;
        boolean mAnimateExit = true;
        SurfaceControl mDimLayer;
        boolean mDimming;
        boolean mDontReset;
        SurfaceAnimator mSurfaceAnimator;

        DimState(SurfaceControl dimLayer) {
            this.mDimLayer = dimLayer;
            this.mDimming = true;
            this.mSurfaceAnimator = new SurfaceAnimator(new DimAnimatable(dimLayer), new -$$Lambda$Dimmer$DimState$jMIg4fVfhKsf8fm7mIcffBmkFt8(this), Dimmer.this.mHost.mService);
        }

        public static /* synthetic */ void lambda$new$0(DimState dimState) {
            if (!dimState.mDimming) {
                dimState.mDimLayer.destroy();
            }
        }
    }

    @VisibleForTesting
    interface SurfaceAnimatorStarter {
        void startAnimation(SurfaceAnimator surfaceAnimator, Transaction transaction, AnimationAdapter animationAdapter, boolean z);
    }

    private static class AlphaAnimationSpec implements AnimationSpec {
        private final long mDuration;
        private final float mFromAlpha;
        private final float mToAlpha;

        AlphaAnimationSpec(float fromAlpha, float toAlpha, long duration) {
            this.mFromAlpha = fromAlpha;
            this.mToAlpha = toAlpha;
            this.mDuration = duration;
        }

        public long getDuration() {
            return this.mDuration;
        }

        public void apply(Transaction t, SurfaceControl sc, long currentPlayTime) {
            t.setAlpha(sc, ((((float) currentPlayTime) / ((float) getDuration())) * (this.mToAlpha - this.mFromAlpha)) + this.mFromAlpha);
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.print("from=");
            pw.print(this.mFromAlpha);
            pw.print(" to=");
            pw.print(this.mToAlpha);
            pw.print(" duration=");
            pw.println(this.mDuration);
        }

        public void writeToProtoInner(ProtoOutputStream proto) {
            long token = proto.start(1146756268035L);
            proto.write(1108101562369L, this.mFromAlpha);
            proto.write(1108101562370L, this.mToAlpha);
            proto.write(1112396529667L, this.mDuration);
            proto.end(token);
        }
    }

    private class DimAnimatable implements Animatable {
        private final SurfaceControl mDimLayer;

        private DimAnimatable(SurfaceControl dimLayer) {
            this.mDimLayer = dimLayer;
        }

        public Transaction getPendingTransaction() {
            return Dimmer.this.mHost.getPendingTransaction();
        }

        public void commitPendingTransaction() {
            Dimmer.this.mHost.commitPendingTransaction();
        }

        public void onAnimationLeashCreated(Transaction t, SurfaceControl leash) {
        }

        public void onAnimationLeashDestroyed(Transaction t) {
        }

        public Builder makeAnimationLeash() {
            return Dimmer.this.mHost.makeAnimationLeash();
        }

        public SurfaceControl getAnimationLeashParent() {
            return Dimmer.this.mHost.getSurfaceControl();
        }

        public SurfaceControl getSurfaceControl() {
            return this.mDimLayer;
        }

        public SurfaceControl getParentSurfaceControl() {
            return Dimmer.this.mHost.getSurfaceControl();
        }

        public int getSurfaceWidth() {
            return Dimmer.this.mHost.getSurfaceWidth();
        }

        public int getSurfaceHeight() {
            return Dimmer.this.mHost.getSurfaceHeight();
        }
    }

    Dimmer(WindowContainer host) {
        this(host, -$$Lambda$yACUZqn1Ak-GL14-Nu3kHUSaLX0.INSTANCE);
    }

    Dimmer(WindowContainer host, SurfaceAnimatorStarter surfaceAnimatorStarter) {
        this.mHost = host;
        this.mSurfaceAnimatorStarter = surfaceAnimatorStarter;
    }

    private SurfaceControl makeDimLayer() {
        Builder colorLayer = this.mHost.makeChildSurface(null).setParent(this.mHost.getSurfaceControl()).setColorLayer(true);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Dim Layer for - ");
        stringBuilder.append(this.mHost.getName());
        return colorLayer.setName(stringBuilder.toString()).build();
    }

    private DimState getDimState(WindowContainer container) {
        if (this.mDimState == null) {
            try {
                this.mDimState = new DimState(makeDimLayer());
                if (container == null) {
                    this.mDimState.mDontReset = true;
                }
            } catch (OutOfResourcesException e) {
                Log.w(TAG, "OutOfResourcesException creating dim surface");
            }
        }
        this.mLastRequestedDimContainer = container;
        return this.mDimState;
    }

    private void dim(Transaction t, WindowContainer container, int relativeLayer, float alpha) {
        DimState d = getDimState(container);
        if (d != null) {
            if (container != null) {
                t.setRelativeLayer(d.mDimLayer, container.getSurfaceControl(), relativeLayer);
            } else {
                t.setLayer(d.mDimLayer, HwBootFail.STAGE_BOOT_SUCCESS);
            }
            t.setAlpha(d.mDimLayer, alpha);
            d.mDimming = true;
        }
    }

    void stopDim(Transaction t) {
        if (this.mDimState != null) {
            t.hide(this.mDimState.mDimLayer);
            this.mDimState.isVisible = false;
            this.mDimState.mDontReset = false;
        }
    }

    void dimAbove(Transaction t, float alpha) {
        dim(t, null, 1, alpha);
    }

    void dimAbove(Transaction t, WindowContainer container, float alpha) {
        dim(t, container, 1, alpha);
    }

    void dimBelow(Transaction t, WindowContainer container, float alpha) {
        dim(t, container, -1, alpha);
    }

    void resetDimStates() {
        if (this.mDimState != null && !this.mDimState.mDontReset) {
            this.mDimState.mDimming = false;
        }
    }

    void dontAnimateExit() {
        if (this.mDimState != null) {
            this.mDimState.mAnimateExit = false;
        }
    }

    boolean updateDims(Transaction t, Rect bounds) {
        if (this.mDimState == null) {
            return false;
        }
        if (this.mDimState.mDimming) {
            if (!(this.mHost == null || this.mHost.mService == null || this.mHost.mService.mHwWMSEx == null)) {
                this.mHost.mService.mHwWMSEx.updateDimPositionForPCMode(this.mHost, bounds);
            }
            t.setSize(this.mDimState.mDimLayer, bounds.width(), bounds.height());
            t.setPosition(this.mDimState.mDimLayer, (float) bounds.left, (float) bounds.top);
            if (!this.mDimState.isVisible) {
                this.mDimState.isVisible = true;
                t.show(this.mDimState.mDimLayer);
                startDimEnter(this.mLastRequestedDimContainer, this.mDimState.mSurfaceAnimator, t);
            }
            return true;
        }
        if (this.mDimState.mAnimateExit) {
            startDimExit(this.mLastRequestedDimContainer, this.mDimState.mSurfaceAnimator, t);
        } else {
            t.destroy(this.mDimState.mDimLayer);
        }
        this.mDimState = null;
        return false;
    }

    private void startDimEnter(WindowContainer container, SurfaceAnimator animator, Transaction t) {
        startAnim(container, animator, t, 0.0f, 1.0f);
    }

    private void startDimExit(WindowContainer container, SurfaceAnimator animator, Transaction t) {
        startAnim(container, animator, t, 1.0f, 0.0f);
    }

    private void startAnim(WindowContainer container, SurfaceAnimator animator, Transaction t, float startAlpha, float endAlpha) {
        this.mSurfaceAnimatorStarter.startAnimation(animator, t, new LocalAnimationAdapter(new AlphaAnimationSpec(startAlpha, endAlpha, getDimDuration(container)), this.mHost.mService.mSurfaceAnimationRunner), false);
    }

    private long getDimDuration(WindowContainer container) {
        if (container == null) {
            return 0;
        }
        long j;
        AnimationAdapter animationAdapter = container.mSurfaceAnimator.getAnimation();
        if (animationAdapter == null) {
            j = 200;
        } else {
            j = animationAdapter.getDurationHint();
        }
        return j;
    }
}
