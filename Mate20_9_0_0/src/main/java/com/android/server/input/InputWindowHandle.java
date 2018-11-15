package com.android.server.input;

import android.graphics.Region;
import android.view.IWindow;
import android.view.InputChannel;

public final class InputWindowHandle {
    public boolean canReceiveKeys;
    public final IWindow clientWindow;
    public long dispatchingTimeoutNanos;
    public int displayId;
    public int frameBottom;
    public int frameLeft;
    public int frameRight;
    public int frameTop;
    public boolean hasFocus;
    public boolean hasWallpaper;
    public final InputApplicationHandle inputApplicationHandle;
    public InputChannel inputChannel;
    public int inputFeatures;
    public int layer;
    public int layoutParamsFlags;
    public int layoutParamsPrivateFlags;
    public int layoutParamsType;
    public String name;
    public int ownerPid;
    public int ownerUid;
    public boolean paused;
    private long ptr;
    public float scaleFactor;
    public final Region touchableRegion = new Region();
    public boolean visible;
    public final Object windowState;

    private native void nativeDispose();

    public InputWindowHandle(InputApplicationHandle inputApplicationHandle, Object windowState, IWindow clientWindow, int displayId) {
        this.inputApplicationHandle = inputApplicationHandle;
        this.windowState = windowState;
        this.clientWindow = clientWindow;
        this.displayId = displayId;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(this.name);
        stringBuilder.append(", layer=");
        stringBuilder.append(this.layer);
        stringBuilder.append(", frame=[");
        stringBuilder.append(this.frameLeft);
        stringBuilder.append(",");
        stringBuilder.append(this.frameTop);
        stringBuilder.append(",");
        stringBuilder.append(this.frameRight);
        stringBuilder.append(",");
        stringBuilder.append(this.frameBottom);
        stringBuilder.append("]");
        stringBuilder.append(", touchableRegion=");
        stringBuilder.append(this.touchableRegion);
        stringBuilder.append(", visible=");
        stringBuilder.append(this.visible);
        return stringBuilder.toString();
    }

    protected void finalize() throws Throwable {
        try {
            nativeDispose();
        } finally {
            super.finalize();
        }
    }
}
