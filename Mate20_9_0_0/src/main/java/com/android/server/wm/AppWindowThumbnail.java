package com.android.server.wm;

import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.os.Binder;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Builder;
import android.view.SurfaceControl.Transaction;
import android.view.animation.Animation;
import com.android.server.job.controllers.JobStatus;
import com.android.server.os.HwBootFail;

class AppWindowThumbnail implements Animatable {
    private static final String TAG = "WindowManager";
    private final AppWindowToken mAppToken;
    private final int mHeight;
    private final SurfaceAnimator mSurfaceAnimator;
    private final SurfaceControl mSurfaceControl;
    private final int mWidth;

    AppWindowThumbnail(Transaction t, AppWindowToken appToken, GraphicBuffer thumbnailHeader) {
        this.mAppToken = appToken;
        this.mSurfaceAnimator = new SurfaceAnimator(this, new -$$Lambda$AppWindowThumbnail$hHTeq2FR5SSE1YyVM6K-wuzeLLo(this), appToken.mService);
        this.mWidth = thumbnailHeader.getWidth();
        this.mHeight = thumbnailHeader.getHeight();
        WindowState window = appToken.findMainWindow();
        Builder makeSurface = appToken.makeSurface();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("thumbnail anim: ");
        stringBuilder.append(appToken.toString());
        this.mSurfaceControl = makeSurface.setName(stringBuilder.toString()).setSize(this.mWidth, this.mHeight).setFormat(-3).setMetadata(appToken.windowType, window != null ? window.mOwnerUid : Binder.getCallingUid()).build();
        Surface drawSurface = new Surface();
        drawSurface.copyFrom(this.mSurfaceControl);
        drawSurface.attachAndQueueBuffer(thumbnailHeader);
        drawSurface.release();
        t.show(this.mSurfaceControl);
        t.setLayer(this.mSurfaceControl, HwBootFail.STAGE_BOOT_SUCCESS);
    }

    void startAnimation(Transaction t, Animation anim) {
        startAnimation(t, anim, null);
    }

    void startAnimation(Transaction t, Animation anim, Point position) {
        anim.restrictDuration(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        anim.scaleCurrentDuration(this.mAppToken.mService.getTransitionAnimationScaleLocked());
        this.mSurfaceAnimator.startAnimation(t, new LocalAnimationAdapter(new WindowAnimationSpec(anim, position, this.mAppToken.mService.mAppTransition.canSkipFirstFrame()), this.mAppToken.mService.mSurfaceAnimationRunner), false);
    }

    private void onAnimationFinished() {
    }

    void setShowing(Transaction pendingTransaction, boolean show) {
        if (show) {
            pendingTransaction.show(this.mSurfaceControl);
        } else {
            pendingTransaction.hide(this.mSurfaceControl);
        }
    }

    void destroy() {
        this.mSurfaceAnimator.cancelAnimation();
        this.mSurfaceControl.destroy();
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, this.mWidth);
        proto.write(1120986464258L, this.mHeight);
        this.mSurfaceAnimator.writeToProto(proto, 1146756268035L);
        proto.end(token);
    }

    public Transaction getPendingTransaction() {
        return this.mAppToken.getPendingTransaction();
    }

    public void commitPendingTransaction() {
        this.mAppToken.commitPendingTransaction();
    }

    public void onAnimationLeashCreated(Transaction t, SurfaceControl leash) {
        t.setLayer(leash, HwBootFail.STAGE_BOOT_SUCCESS);
    }

    public void onAnimationLeashDestroyed(Transaction t) {
        t.hide(this.mSurfaceControl);
    }

    public Builder makeAnimationLeash() {
        return this.mAppToken.makeSurface();
    }

    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    public SurfaceControl getAnimationLeashParent() {
        return this.mAppToken.getAppAnimationLayer();
    }

    public SurfaceControl getParentSurfaceControl() {
        return this.mAppToken.getParentSurfaceControl();
    }

    public int getSurfaceWidth() {
        return this.mWidth;
    }

    public int getSurfaceHeight() {
        return this.mHeight;
    }
}
