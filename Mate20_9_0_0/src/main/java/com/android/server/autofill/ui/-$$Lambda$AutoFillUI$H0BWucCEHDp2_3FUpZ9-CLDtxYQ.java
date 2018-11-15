package com.android.server.autofill.ui;

import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.service.autofill.FillResponse;
import android.view.autofill.AutofillId;
import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutoFillUI$H0BWucCEHDp2_3FUpZ9-CLDtxYQ implements Runnable {
    private final /* synthetic */ AutoFillUI f$0;
    private final /* synthetic */ AutoFillUiCallback f$1;
    private final /* synthetic */ FillResponse f$2;
    private final /* synthetic */ AutofillId f$3;
    private final /* synthetic */ String f$4;
    private final /* synthetic */ CharSequence f$5;
    private final /* synthetic */ Drawable f$6;
    private final /* synthetic */ LogMaker f$7;

    public /* synthetic */ -$$Lambda$AutoFillUI$H0BWucCEHDp2_3FUpZ9-CLDtxYQ(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback, FillResponse fillResponse, AutofillId autofillId, String str, CharSequence charSequence, Drawable drawable, LogMaker logMaker) {
        this.f$0 = autoFillUI;
        this.f$1 = autoFillUiCallback;
        this.f$2 = fillResponse;
        this.f$3 = autofillId;
        this.f$4 = str;
        this.f$5 = charSequence;
        this.f$6 = drawable;
        this.f$7 = logMaker;
    }

    public final void run() {
        AutoFillUI.lambda$showFillUi$5(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7);
    }
}
