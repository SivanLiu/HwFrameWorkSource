package com.android.server.autofill.ui;

import android.widget.Filter.FilterListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FillUi$IWXsdxX6-XHI4qKxpJLqtc98gZA implements FilterListener {
    private final /* synthetic */ FillUi f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$FillUi$IWXsdxX6-XHI4qKxpJLqtc98gZA(FillUi fillUi, int i) {
        this.f$0 = fillUi;
        this.f$1 = i;
    }

    public final void onFilterComplete(int i) {
        FillUi.lambda$applyNewFilterText$3(this.f$0, this.f$1, i);
    }
}
