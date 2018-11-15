package com.android.server.autofill.ui;

import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutoFillUI$Z-Di7CGd-L0nOI4i7_RO1FYbhgU implements Runnable {
    private final /* synthetic */ AutoFillUI f$0;
    private final /* synthetic */ AutoFillUiCallback f$1;

    public /* synthetic */ -$$Lambda$AutoFillUI$Z-Di7CGd-L0nOI4i7_RO1FYbhgU(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback) {
        this.f$0 = autoFillUI;
        this.f$1 = autoFillUiCallback;
    }

    public final void run() {
        AutoFillUI.lambda$setCallback$0(this.f$0, this.f$1);
    }
}
