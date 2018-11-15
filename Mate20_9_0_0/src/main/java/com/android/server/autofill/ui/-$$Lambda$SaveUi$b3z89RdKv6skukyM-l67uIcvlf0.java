package com.android.server.autofill.ui;

import android.view.View;
import android.view.View.OnClickListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SaveUi$b3z89RdKv6skukyM-l67uIcvlf0 implements OnClickListener {
    private final /* synthetic */ SaveUi f$0;

    public /* synthetic */ -$$Lambda$SaveUi$b3z89RdKv6skukyM-l67uIcvlf0(SaveUi saveUi) {
        this.f$0 = saveUi;
    }

    public final void onClick(View view) {
        this.f$0.mListener.onSave();
    }
}
