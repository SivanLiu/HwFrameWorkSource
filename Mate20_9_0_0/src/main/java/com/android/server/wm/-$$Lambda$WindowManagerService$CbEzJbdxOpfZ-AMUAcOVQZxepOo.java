package com.android.server.wm;

import android.app.IAssistDataReceiver;
import android.graphics.Bitmap;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WindowManagerService$CbEzJbdxOpfZ-AMUAcOVQZxepOo implements Runnable {
    private final /* synthetic */ IAssistDataReceiver f$0;
    private final /* synthetic */ Bitmap f$1;

    public /* synthetic */ -$$Lambda$WindowManagerService$CbEzJbdxOpfZ-AMUAcOVQZxepOo(IAssistDataReceiver iAssistDataReceiver, Bitmap bitmap) {
        this.f$0 = iAssistDataReceiver;
        this.f$1 = bitmap;
    }

    public final void run() {
        WindowManagerService.lambda$requestAssistScreenshot$2(this.f$0, this.f$1);
    }
}
