package com.android.server.usb;

import android.content.Intent;
import android.os.UserHandle;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UsbPortManager$FUqGOOupcl6RrRkZBk-BnrRQyPI implements Runnable {
    private final /* synthetic */ UsbPortManager f$0;
    private final /* synthetic */ Intent f$1;

    public /* synthetic */ -$$Lambda$UsbPortManager$FUqGOOupcl6RrRkZBk-BnrRQyPI(UsbPortManager usbPortManager, Intent intent) {
        this.f$0 = usbPortManager;
        this.f$1 = intent;
    }

    public final void run() {
        this.f$0.mContext.sendBroadcastAsUser(this.f$1, UserHandle.ALL);
    }
}
