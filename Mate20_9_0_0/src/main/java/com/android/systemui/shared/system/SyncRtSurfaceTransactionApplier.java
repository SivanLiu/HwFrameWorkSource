package com.android.systemui.shared.system;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import android.view.View;
import android.view.ViewRootImpl;

public class SyncRtSurfaceTransactionApplier {
    private final Surface mTargetSurface;
    private final ViewRootImpl mTargetViewRootImpl;
    private final float[] mTmpFloat9 = new float[9];

    public static class SurfaceParams {
        final float alpha;
        final int layer;
        final Matrix matrix;
        final SurfaceControl surface;
        final Rect windowCrop;

        public SurfaceParams(SurfaceControlCompat surface, float alpha, Matrix matrix, Rect windowCrop, int layer) {
            this.surface = surface.mSurfaceControl;
            this.alpha = alpha;
            this.matrix = new Matrix(matrix);
            this.windowCrop = new Rect(windowCrop);
            this.layer = layer;
        }
    }

    public SyncRtSurfaceTransactionApplier(View targetView) {
        Surface surface = null;
        this.mTargetViewRootImpl = targetView != null ? targetView.getViewRootImpl() : null;
        if (this.mTargetViewRootImpl != null) {
            surface = this.mTargetViewRootImpl.mSurface;
        }
        this.mTargetSurface = surface;
    }

    public void scheduleApply(SurfaceParams... params) {
        if (this.mTargetViewRootImpl != null) {
            this.mTargetViewRootImpl.registerRtFrameCallback(new -$$Lambda$SyncRtSurfaceTransactionApplier$odiZXJPwVQjUmyGhNuNxcMPZIOM(this, params));
            this.mTargetViewRootImpl.getView().invalidate();
        }
    }

    public static /* synthetic */ void lambda$scheduleApply$0(SyncRtSurfaceTransactionApplier syncRtSurfaceTransactionApplier, SurfaceParams[] params, long frame) {
        if (syncRtSurfaceTransactionApplier.mTargetSurface != null && syncRtSurfaceTransactionApplier.mTargetSurface.isValid()) {
            Transaction t = new Transaction();
            for (int i = params.length - 1; i >= 0; i--) {
                SurfaceParams surfaceParams = params[i];
                t.deferTransactionUntilSurface(surfaceParams.surface, syncRtSurfaceTransactionApplier.mTargetSurface, frame);
                applyParams(t, surfaceParams, syncRtSurfaceTransactionApplier.mTmpFloat9);
            }
            t.setEarlyWakeup();
            t.apply();
        }
    }

    public static void applyParams(TransactionCompat t, SurfaceParams params) {
        applyParams(t.mTransaction, params, t.mTmpValues);
    }

    private static void applyParams(Transaction t, SurfaceParams params, float[] tmpFloat9) {
        t.setMatrix(params.surface, params.matrix, tmpFloat9);
        t.setWindowCrop(params.surface, params.windowCrop);
        t.setAlpha(params.surface, params.alpha);
        t.setLayer(params.surface, params.layer);
        t.show(params.surface);
    }
}
