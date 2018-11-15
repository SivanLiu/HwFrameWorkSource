package com.android.server.autofill.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SaveUi$ckPlzqJfB_ohleAkb5RXKU7mFY8 implements OnDismissListener {
    private final /* synthetic */ SaveUi f$0;

    public /* synthetic */ -$$Lambda$SaveUi$ckPlzqJfB_ohleAkb5RXKU7mFY8(SaveUi saveUi) {
        this.f$0 = saveUi;
    }

    public final void onDismiss(DialogInterface dialogInterface) {
        this.f$0.mListener.onCancel(null);
    }
}
