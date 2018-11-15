package com.android.server.autofill.ui;

import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutoFillUI$S8lqjy9BKKn2SSfu43iaVPGD6rg implements Runnable {
    private final /* synthetic */ AutoFillUI f$0;
    private final /* synthetic */ AutoFillUiCallback f$1;
    private final /* synthetic */ CharSequence f$2;

    public /* synthetic */ -$$Lambda$AutoFillUI$S8lqjy9BKKn2SSfu43iaVPGD6rg(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback, CharSequence charSequence) {
        this.f$0 = autoFillUI;
        this.f$1 = autoFillUiCallback;
        this.f$2 = charSequence;
    }

    public final void run() {
        AutoFillUI.lambda$showError$2(this.f$0, this.f$1, this.f$2);
    }
}
