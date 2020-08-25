package com.android.server.wm;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceControl;

public class InsetSurface {
    private static final int MULTIPLE = 101;
    private static final String TAG = "InsetSurface";
    private final Rect mCurrentSurfacePosition = new Rect();
    private DisplayContent mDc;
    private final Rect mLastSurfacePosition = new Rect();
    private final Surface mSurface = new Surface();
    private SurfaceControl mSurfaceControl;
    private final String mType;

    public InsetSurface(String type) {
        this.mType = type;
    }

    public void layout(int left, int top, int right, int bottom) {
        this.mCurrentSurfacePosition.set(left, top, right, bottom);
        if (!this.mCurrentSurfacePosition.isEmpty()) {
            Log.d(TAG, "layout SurfacePosition:" + this.mCurrentSurfacePosition + " mType:" + this.mType);
        }
    }

    public void setDisplayContent(DisplayContent dc) {
        this.mDc = dc;
    }

    private void createSurface(Rect rect) {
        SurfaceControl ctrl = null;
        try {
            SurfaceControl.Builder makeOverlay = this.mDc.makeOverlay();
            ctrl = makeOverlay.setName("InsetSurface " + this.mType).setBufferSize(rect.width(), rect.height()).build();
            ctrl.setLayer(WindowManagerConstants.INSETSURFACE_LAYER);
            ctrl.setPosition((float) rect.left, (float) rect.top);
            ctrl.show();
            this.mSurface.copyFrom(ctrl);
        } catch (Surface.OutOfResourcesException e) {
            Log.d(TAG, "createSurface fail");
        }
        this.mSurfaceControl = ctrl;
    }

    public void remove() {
        if (this.mSurfaceControl != null) {
            Log.d(TAG, "remove " + this.mType);
            this.mLastSurfacePosition.setEmpty();
            this.mSurfaceControl.remove();
            this.mSurfaceControl = null;
        }
    }

    public void show(SurfaceControl relativeTo) {
        if (!this.mLastSurfacePosition.equals(this.mCurrentSurfacePosition)) {
            this.mLastSurfacePosition.set(this.mCurrentSurfacePosition);
            if (!this.mLastSurfacePosition.isEmpty()) {
                if (this.mSurfaceControl == null) {
                    createSurface(this.mLastSurfacePosition);
                    if (relativeTo != null) {
                        this.mSurfaceControl.setRelativeLayer(relativeTo, 0);
                    }
                }
                Log.d(TAG, "show " + this.mType);
                drawIfNeeded();
                this.mSurfaceControl.show();
            }
        }
    }

    public void hide(SurfaceControl.Transaction t) {
        if (this.mSurfaceControl != null) {
            Log.d(TAG, "hide " + this.mType);
            this.mLastSurfacePosition.setEmpty();
            t.hide(this.mSurfaceControl);
        }
    }

    private void drawIfNeeded() {
        Canvas c = null;
        try {
            c = this.mSurface.lockCanvas(null);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "InsetSurface illegal argument");
        } catch (Surface.OutOfResourcesException e2) {
            Log.e(TAG, "InsetSurface out of resource");
        }
        if (c != null) {
            c.drawColor(-16777216);
            this.mSurface.unlockCanvasAndPost(c);
        }
    }
}
