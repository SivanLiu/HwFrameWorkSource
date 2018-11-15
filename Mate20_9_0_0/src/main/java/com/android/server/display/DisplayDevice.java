package com.android.server.display;

import android.graphics.Rect;
import android.hardware.display.DisplayViewport;
import android.os.IBinder;
import android.view.Surface;
import android.view.SurfaceControl.Transaction;
import java.io.PrintWriter;

abstract class DisplayDevice {
    private Rect mCurrentDisplayRect;
    private int mCurrentLayerStack = -1;
    private Rect mCurrentLayerStackRect;
    private int mCurrentOrientation = -1;
    private Surface mCurrentSurface;
    DisplayDeviceInfo mDebugLastLoggedDeviceInfo;
    private final DisplayAdapter mDisplayAdapter;
    private final IBinder mDisplayToken;
    private final String mUniqueId;

    public abstract DisplayDeviceInfo getDisplayDeviceInfoLocked();

    public abstract boolean hasStableUniqueId();

    public DisplayDevice(DisplayAdapter displayAdapter, IBinder displayToken, String uniqueId) {
        this.mDisplayAdapter = displayAdapter;
        this.mDisplayToken = displayToken;
        this.mUniqueId = uniqueId;
    }

    public final DisplayAdapter getAdapterLocked() {
        return this.mDisplayAdapter;
    }

    public final IBinder getDisplayTokenLocked() {
        return this.mDisplayToken;
    }

    public final String getNameLocked() {
        return getDisplayDeviceInfoLocked().name;
    }

    public final String getUniqueId() {
        return this.mUniqueId;
    }

    public void updateDesityforRog() {
    }

    public void applyPendingDisplayDeviceInfoChangesLocked() {
    }

    public void performTraversalLocked(Transaction t) {
    }

    public Runnable requestDisplayStateLocked(int state, int brightness) {
        return null;
    }

    public void requestDisplayModesLocked(int colorMode, int modeId) {
    }

    public void onOverlayChangedLocked() {
    }

    public final void setLayerStackLocked(Transaction t, int layerStack) {
        if (this.mCurrentLayerStack != layerStack) {
            this.mCurrentLayerStack = layerStack;
            t.setDisplayLayerStack(this.mDisplayToken, layerStack);
        }
    }

    public final void setProjectionLocked(Transaction t, int orientation, Rect layerStackRect, Rect displayRect) {
        if (this.mCurrentOrientation != orientation || this.mCurrentLayerStackRect == null || !this.mCurrentLayerStackRect.equals(layerStackRect) || this.mCurrentDisplayRect == null || !this.mCurrentDisplayRect.equals(displayRect)) {
            this.mCurrentOrientation = orientation;
            if (this.mCurrentLayerStackRect == null) {
                this.mCurrentLayerStackRect = new Rect();
            }
            this.mCurrentLayerStackRect.set(layerStackRect);
            if (this.mCurrentDisplayRect == null) {
                this.mCurrentDisplayRect = new Rect();
            }
            this.mCurrentDisplayRect.set(displayRect);
            t.setDisplayProjection(this.mDisplayToken, orientation, layerStackRect, displayRect);
        }
    }

    public final void setSurfaceLocked(Transaction t, Surface surface) {
        if (this.mCurrentSurface != surface) {
            this.mCurrentSurface = surface;
            t.setDisplaySurface(this.mDisplayToken, surface);
        }
    }

    public final void populateViewportLocked(DisplayViewport viewport) {
        viewport.orientation = this.mCurrentOrientation;
        if (this.mCurrentLayerStackRect != null) {
            viewport.logicalFrame.set(this.mCurrentLayerStackRect);
        } else {
            viewport.logicalFrame.setEmpty();
        }
        if (this.mCurrentDisplayRect != null) {
            viewport.physicalFrame.set(this.mCurrentDisplayRect);
        } else {
            viewport.physicalFrame.setEmpty();
        }
        boolean z = true;
        if (!(this.mCurrentOrientation == 1 || this.mCurrentOrientation == 3)) {
            z = false;
        }
        boolean isRotated = z;
        DisplayDeviceInfo info = getDisplayDeviceInfoLocked();
        viewport.deviceWidth = isRotated ? info.height : info.width;
        viewport.deviceHeight = isRotated ? info.width : info.height;
    }

    public void dumpLocked(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mAdapter=");
        stringBuilder.append(this.mDisplayAdapter.getName());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mUniqueId=");
        stringBuilder.append(this.mUniqueId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDisplayToken=");
        stringBuilder.append(this.mDisplayToken);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentLayerStack=");
        stringBuilder.append(this.mCurrentLayerStack);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentOrientation=");
        stringBuilder.append(this.mCurrentOrientation);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentLayerStackRect=");
        stringBuilder.append(this.mCurrentLayerStackRect);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentDisplayRect=");
        stringBuilder.append(this.mCurrentDisplayRect);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentSurface=");
        stringBuilder.append(this.mCurrentSurface);
        pw.println(stringBuilder.toString());
    }
}
