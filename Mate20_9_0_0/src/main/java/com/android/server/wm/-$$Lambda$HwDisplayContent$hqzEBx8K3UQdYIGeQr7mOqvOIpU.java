package com.android.server.wm;

import android.graphics.Rect;
import android.os.IBinder;
import android.view.SurfaceControl;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwDisplayContent$hqzEBx8K3UQdYIGeQr7mOqvOIpU implements ScreenshoterForExternalDisplay {
    public static final /* synthetic */ -$$Lambda$HwDisplayContent$hqzEBx8K3UQdYIGeQr7mOqvOIpU INSTANCE = new -$$Lambda$HwDisplayContent$hqzEBx8K3UQdYIGeQr7mOqvOIpU();

    private /* synthetic */ -$$Lambda$HwDisplayContent$hqzEBx8K3UQdYIGeQr7mOqvOIpU() {
    }

    public final Object screenshotForExternalDisplay(IBinder iBinder, Rect rect, int i, int i2, int i3, int i4, boolean z, int i5) {
        return SurfaceControl.screenshotToBufferForExternalDisplay(iBinder, rect, i, i2, i3, i4, z, i5);
    }
}
