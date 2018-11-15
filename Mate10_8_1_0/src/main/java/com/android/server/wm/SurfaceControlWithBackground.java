package com.android.server.wm;

import android.graphics.Rect;
import android.graphics.Region;
import android.os.IBinder;
import android.view.Surface;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceControl;
import android.view.SurfaceSession;

class SurfaceControlWithBackground extends SurfaceControl {
    private SurfaceControl mBackgroundControl;
    private boolean mHiddenForCrop = false;
    private float mLastDsDx = 1.0f;
    private float mLastDsDy = 1.0f;
    private float mLastHeight;
    private float mLastWidth;
    private float mLastX;
    private float mLastY;
    private Rect mTmpContainerRect = new Rect();
    private boolean mVisible;
    private WindowSurfaceController mWindowSurfaceController;

    public SurfaceControlWithBackground(SurfaceControlWithBackground other) {
        super(other);
        this.mBackgroundControl = other.mBackgroundControl;
        this.mVisible = other.mVisible;
        this.mWindowSurfaceController = other.mWindowSurfaceController;
    }

    public SurfaceControlWithBackground(SurfaceSession s, String name, int w, int h, int format, int flags, int windowType, int ownerUid, WindowSurfaceController windowSurfaceController) throws OutOfResourcesException {
        super(s, name, w, h, format, flags, windowType, ownerUid);
        if ((windowType == 1 || windowType == 3) && (windowSurfaceController.mAnimator.mWin.isLetterboxedAppWindow() ^ 1) == 0) {
            this.mWindowSurfaceController = windowSurfaceController;
            this.mLastWidth = (float) w;
            this.mLastHeight = (float) h;
            this.mWindowSurfaceController.getContainerRect(this.mTmpContainerRect);
            this.mBackgroundControl = new SurfaceControl(s, "Background for - " + name, this.mTmpContainerRect.width(), this.mTmpContainerRect.height(), -1, flags | DumpState.DUMP_INTENT_FILTER_VERIFIERS);
        }
    }

    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (this.mBackgroundControl != null) {
            this.mBackgroundControl.setAlpha(alpha);
        }
    }

    public void setLayer(int zorder) {
        super.setLayer(zorder);
        if (this.mBackgroundControl != null) {
            this.mBackgroundControl.setLayer(zorder - 1);
        }
    }

    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        if (this.mBackgroundControl != null) {
            this.mLastX = x;
            this.mLastY = y;
            updateBgPosition();
        }
    }

    private void updateBgPosition() {
        this.mWindowSurfaceController.getContainerRect(this.mTmpContainerRect);
        Rect winFrame = this.mWindowSurfaceController.mAnimator.mWin.mFrame;
        this.mBackgroundControl.setPosition(this.mLastX + (((float) (this.mTmpContainerRect.left - winFrame.left)) * this.mLastDsDx), this.mLastY + (((float) (this.mTmpContainerRect.top - winFrame.top)) * this.mLastDsDy));
    }

    public void setSize(int w, int h) {
        super.setSize(w, h);
        if (this.mBackgroundControl != null) {
            this.mLastWidth = (float) w;
            this.mLastHeight = (float) h;
            this.mWindowSurfaceController.getContainerRect(this.mTmpContainerRect);
            this.mBackgroundControl.setSize(this.mTmpContainerRect.width(), this.mTmpContainerRect.height());
        }
    }

    public void setWindowCrop(Rect crop) {
        super.setWindowCrop(crop);
        if (this.mBackgroundControl != null) {
            calculateBgCrop(crop);
            this.mBackgroundControl.setWindowCrop(this.mTmpContainerRect);
            this.mHiddenForCrop = this.mTmpContainerRect.isEmpty();
            updateBackgroundVisibility();
        }
    }

    public void setFinalCrop(Rect crop) {
        super.setFinalCrop(crop);
        if (this.mBackgroundControl != null) {
            this.mWindowSurfaceController.getContainerRect(this.mTmpContainerRect);
            this.mBackgroundControl.setFinalCrop(this.mTmpContainerRect);
        }
    }

    private void calculateBgCrop(Rect crop) {
        Rect contentInsets = this.mWindowSurfaceController.mAnimator.mWin.mContentInsets;
        float d = contentInsets.top == 0 ? 0.0f : ((float) crop.top) / ((float) contentInsets.top);
        if (d > 1.0f) {
            this.mTmpContainerRect.setEmpty();
            return;
        }
        if (d < 0.025f) {
            d = 0.0f;
        }
        this.mWindowSurfaceController.getContainerRect(this.mTmpContainerRect);
        int backgroundWidth = 0;
        int backgroundHeight = 0;
        Rect winFrame = this.mWindowSurfaceController.mAnimator.mWin.mFrame;
        int offsetX = (int) (((float) (winFrame.left - this.mTmpContainerRect.left)) * this.mLastDsDx);
        int offsetY = (int) (((float) (winFrame.top - this.mTmpContainerRect.top)) * this.mLastDsDy);
        switch (this.mWindowSurfaceController.mAnimator.mService.getNavBarPosition()) {
            case 1:
                backgroundWidth = (int) (((double) ((((float) this.mTmpContainerRect.width()) - this.mLastWidth) * (1.0f - d))) + 0.5d);
                backgroundHeight = crop.height();
                offsetX += crop.left - backgroundWidth;
                offsetY += crop.top;
                break;
            case 2:
                backgroundWidth = (int) (((double) ((((float) this.mTmpContainerRect.width()) - this.mLastWidth) * (1.0f - d))) + 0.5d);
                backgroundHeight = crop.height();
                offsetX += crop.right;
                offsetY += crop.top;
                break;
            case 4:
                backgroundWidth = crop.width();
                backgroundHeight = (int) (((double) ((((float) this.mTmpContainerRect.height()) - this.mLastHeight) * (1.0f - d))) + 0.5d);
                offsetX += crop.left;
                offsetY += crop.bottom;
                break;
        }
        this.mTmpContainerRect.set(offsetX, offsetY, offsetX + backgroundWidth, offsetY + backgroundHeight);
    }

    public void setLayerStack(int layerStack) {
        super.setLayerStack(layerStack);
        if (this.mBackgroundControl != null) {
            this.mBackgroundControl.setLayerStack(layerStack);
        }
    }

    public void setOpaque(boolean isOpaque) {
        super.setOpaque(isOpaque);
        updateBackgroundVisibility();
    }

    public void setSecure(boolean isSecure) {
        super.setSecure(isSecure);
    }

    public void setMatrix(float dsdx, float dtdx, float dtdy, float dsdy) {
        super.setMatrix(dsdx, dtdx, dtdy, dsdy);
        if (this.mBackgroundControl != null) {
            this.mBackgroundControl.setMatrix(dsdx, dtdx, dtdy, dsdy);
            this.mLastDsDx = dsdx;
            this.mLastDsDy = dsdy;
            updateBgPosition();
        }
    }

    public void hide() {
        super.hide();
        this.mVisible = false;
        updateBackgroundVisibility();
    }

    public void show() {
        super.show();
        this.mVisible = true;
        updateBackgroundVisibility();
    }

    public void destroy() {
        super.destroy();
        if (this.mBackgroundControl != null) {
            this.mBackgroundControl.destroy();
        }
    }

    public void release() {
        super.release();
        if (this.mBackgroundControl != null) {
            this.mBackgroundControl.release();
        }
    }

    public void setTransparentRegionHint(Region region) {
        super.setTransparentRegionHint(region);
        if (this.mBackgroundControl != null) {
            this.mBackgroundControl.setTransparentRegionHint(region);
        }
    }

    public void deferTransactionUntil(IBinder handle, long frame) {
        super.deferTransactionUntil(handle, frame);
        if (this.mBackgroundControl != null) {
            this.mBackgroundControl.deferTransactionUntil(handle, frame);
        }
    }

    public void deferTransactionUntil(Surface barrier, long frame) {
        super.deferTransactionUntil(barrier, frame);
        if (this.mBackgroundControl != null) {
            this.mBackgroundControl.deferTransactionUntil(barrier, frame);
        }
    }

    private void updateBackgroundVisibility() {
        if (this.mBackgroundControl != null) {
            AppWindowToken appWindowToken = this.mWindowSurfaceController.mAnimator.mWin.mAppToken;
            if (this.mHiddenForCrop || !this.mVisible || appWindowToken == null || !appWindowToken.fillsParent()) {
                this.mBackgroundControl.hide();
            } else {
                this.mBackgroundControl.show();
            }
        }
    }
}
