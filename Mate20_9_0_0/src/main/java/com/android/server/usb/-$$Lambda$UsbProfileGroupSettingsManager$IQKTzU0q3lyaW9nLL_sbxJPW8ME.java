package com.android.server.usb;

import android.hardware.usb.UsbDevice;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UsbProfileGroupSettingsManager$IQKTzU0q3lyaW9nLL_sbxJPW8ME implements OnOpenInAppListener {
    private final /* synthetic */ UsbProfileGroupSettingsManager f$0;

    public /* synthetic */ -$$Lambda$UsbProfileGroupSettingsManager$IQKTzU0q3lyaW9nLL_sbxJPW8ME(UsbProfileGroupSettingsManager usbProfileGroupSettingsManager) {
        this.f$0 = usbProfileGroupSettingsManager;
    }

    public final void onOpenInApp(UsbDevice usbDevice) {
        this.f$0.resolveActivity(UsbProfileGroupSettingsManager.createDeviceAttachedIntent(usbDevice), usbDevice, false);
    }
}
