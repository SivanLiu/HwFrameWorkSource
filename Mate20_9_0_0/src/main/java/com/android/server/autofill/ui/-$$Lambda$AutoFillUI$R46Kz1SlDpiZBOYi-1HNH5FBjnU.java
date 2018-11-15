package com.android.server.autofill.ui;

import android.os.IBinder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutoFillUI$R46Kz1SlDpiZBOYi-1HNH5FBjnU implements Runnable {
    private final /* synthetic */ AutoFillUI f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ IBinder f$2;

    public /* synthetic */ -$$Lambda$AutoFillUI$R46Kz1SlDpiZBOYi-1HNH5FBjnU(AutoFillUI autoFillUI, int i, IBinder iBinder) {
        this.f$0 = autoFillUI;
        this.f$1 = i;
        this.f$2 = iBinder;
    }

    public final void run() {
        AutoFillUI.lambda$onPendingSaveUi$7(this.f$0, this.f$1, this.f$2);
    }
}
