package com.android.server.autofill.ui;

import android.view.WindowManager.LayoutParams;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FillUi$AutofillWindowPresenter$N4xQe2B0oe5MBiqZlsy3Lb7vZTg implements Runnable {
    private final /* synthetic */ AutofillWindowPresenter f$0;
    private final /* synthetic */ LayoutParams f$1;

    public /* synthetic */ -$$Lambda$FillUi$AutofillWindowPresenter$N4xQe2B0oe5MBiqZlsy3Lb7vZTg(AutofillWindowPresenter autofillWindowPresenter, LayoutParams layoutParams) {
        this.f$0 = autofillWindowPresenter;
        this.f$1 = layoutParams;
    }

    public final void run() {
        FillUi.this.mWindow.show(this.f$1);
    }
}
