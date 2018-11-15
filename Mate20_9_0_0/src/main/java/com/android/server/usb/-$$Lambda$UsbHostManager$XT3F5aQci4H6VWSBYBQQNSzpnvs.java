package com.android.server.usb;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UsbHostManager$XT3F5aQci4H6VWSBYBQQNSzpnvs implements Runnable {
    private final /* synthetic */ UsbHostManager f$0;

    public /* synthetic */ -$$Lambda$UsbHostManager$XT3F5aQci4H6VWSBYBQQNSzpnvs(UsbHostManager usbHostManager) {
        this.f$0 = usbHostManager;
    }

    public final void run() {
        this.f$0.monitorUsbHostBus();
    }
}
