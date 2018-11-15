package com.android.server.autofill.ui;

import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutoFillUI$XWhvh2-Jd9NLMoEos-e8RkZdQaI implements Runnable {
    private final /* synthetic */ AutoFillUI f$0;
    private final /* synthetic */ PendingUi f$1;
    private final /* synthetic */ AutoFillUiCallback f$2;
    private final /* synthetic */ boolean f$3;

    public /* synthetic */ -$$Lambda$AutoFillUI$XWhvh2-Jd9NLMoEos-e8RkZdQaI(AutoFillUI autoFillUI, PendingUi pendingUi, AutoFillUiCallback autoFillUiCallback, boolean z) {
        this.f$0 = autoFillUI;
        this.f$1 = pendingUi;
        this.f$2 = autoFillUiCallback;
        this.f$3 = z;
    }

    public final void run() {
        this.f$0.destroyAllUiThread(this.f$1, this.f$2, this.f$3);
    }
}
