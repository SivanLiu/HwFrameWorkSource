package com.android.server.autofill.ui;

import android.service.autofill.FillResponse;
import android.view.View;
import android.view.View.OnClickListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FillUi$Vch3vycmBdfZEFnJyIbU-GchQzA implements OnClickListener {
    private final /* synthetic */ FillUi f$0;
    private final /* synthetic */ FillResponse f$1;

    public /* synthetic */ -$$Lambda$FillUi$Vch3vycmBdfZEFnJyIbU-GchQzA(FillUi fillUi, FillResponse fillResponse) {
        this.f$0 = fillUi;
        this.f$1 = fillResponse;
    }

    public final void onClick(View view) {
        this.f$0.mCallback.onResponsePicked(this.f$1);
    }
}
