package com.android.server.autofill.ui;

import android.service.autofill.SaveInfo;
import android.view.View;
import android.view.View.OnClickListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SaveUi$E9O26NP1L_DDYBfaO7fQ0hhPAx8 implements OnClickListener {
    private final /* synthetic */ SaveUi f$0;
    private final /* synthetic */ SaveInfo f$1;

    public /* synthetic */ -$$Lambda$SaveUi$E9O26NP1L_DDYBfaO7fQ0hhPAx8(SaveUi saveUi, SaveInfo saveInfo) {
        this.f$0 = saveUi;
        this.f$1 = saveInfo;
    }

    public final void onClick(View view) {
        this.f$0.mListener.onCancel(this.f$1.getNegativeActionListener());
    }
}
