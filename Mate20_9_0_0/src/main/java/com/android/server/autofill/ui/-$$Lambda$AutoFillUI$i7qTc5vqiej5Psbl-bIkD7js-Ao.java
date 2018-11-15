package com.android.server.autofill.ui;

import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutoFillUI$i7qTc5vqiej5Psbl-bIkD7js-Ao implements Runnable {
    private final /* synthetic */ AutoFillUI f$0;
    private final /* synthetic */ AutoFillUiCallback f$1;

    public /* synthetic */ -$$Lambda$AutoFillUI$i7qTc5vqiej5Psbl-bIkD7js-Ao(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback) {
        this.f$0 = autoFillUI;
        this.f$1 = autoFillUiCallback;
    }

    public final void run() {
        AutoFillUI.lambda$clearCallback$1(this.f$0, this.f$1);
    }
}
