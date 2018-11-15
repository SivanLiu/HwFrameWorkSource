package com.android.server.autofill.ui;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.service.autofill.SaveInfo;
import android.service.autofill.ValueFinder;
import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutoFillUI$xTxq_LM_GKvWtCQ0xT88Q_Y8M7Q implements Runnable {
    private final /* synthetic */ AutoFillUI f$0;
    private final /* synthetic */ AutoFillUiCallback f$1;
    private final /* synthetic */ boolean f$10;
    private final /* synthetic */ PendingUi f$2;
    private final /* synthetic */ CharSequence f$3;
    private final /* synthetic */ Drawable f$4;
    private final /* synthetic */ String f$5;
    private final /* synthetic */ ComponentName f$6;
    private final /* synthetic */ SaveInfo f$7;
    private final /* synthetic */ ValueFinder f$8;
    private final /* synthetic */ LogMaker f$9;

    public /* synthetic */ -$$Lambda$AutoFillUI$xTxq_LM_GKvWtCQ0xT88Q_Y8M7Q(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback, PendingUi pendingUi, CharSequence charSequence, Drawable drawable, String str, ComponentName componentName, SaveInfo saveInfo, ValueFinder valueFinder, LogMaker logMaker, boolean z) {
        this.f$0 = autoFillUI;
        this.f$1 = autoFillUiCallback;
        this.f$2 = pendingUi;
        this.f$3 = charSequence;
        this.f$4 = drawable;
        this.f$5 = str;
        this.f$6 = componentName;
        this.f$7 = saveInfo;
        this.f$8 = valueFinder;
        this.f$9 = logMaker;
        this.f$10 = z;
    }

    public final void run() {
        AutoFillUI.lambda$showSaveUi$6(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10);
    }
}
