package com.android.server.autofill.ui;

import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutoFillUI$56AC3ykfo4h_e2LSjdkJ3XQn370 implements Runnable {
    private final /* synthetic */ AutoFillUI f$0;
    private final /* synthetic */ AutoFillUiCallback f$1;

    public /* synthetic */ -$$Lambda$AutoFillUI$56AC3ykfo4h_e2LSjdkJ3XQn370(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback) {
        this.f$0 = autoFillUI;
        this.f$1 = autoFillUiCallback;
    }

    public final void run() {
        this.f$0.hideAllUiThread(this.f$1);
    }
}
