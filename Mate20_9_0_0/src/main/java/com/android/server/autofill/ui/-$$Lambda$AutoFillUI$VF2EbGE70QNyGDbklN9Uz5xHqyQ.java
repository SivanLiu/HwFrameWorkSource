package com.android.server.autofill.ui;

import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutoFillUI$VF2EbGE70QNyGDbklN9Uz5xHqyQ implements Runnable {
    private final /* synthetic */ AutoFillUI f$0;
    private final /* synthetic */ AutoFillUiCallback f$1;

    public /* synthetic */ -$$Lambda$AutoFillUI$VF2EbGE70QNyGDbklN9Uz5xHqyQ(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback) {
        this.f$0 = autoFillUI;
        this.f$1 = autoFillUiCallback;
    }

    public final void run() {
        this.f$0.hideFillUiUiThread(this.f$1, true);
    }
}
