package com.android.server.am;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DeprecatedTargetSdkVersionDialog$rNs5B1U3M8_obMr8Ws5cSODCFn8 implements OnClickListener {
    private final /* synthetic */ Context f$0;
    private final /* synthetic */ Intent f$1;

    public /* synthetic */ -$$Lambda$DeprecatedTargetSdkVersionDialog$rNs5B1U3M8_obMr8Ws5cSODCFn8(Context context, Intent intent) {
        this.f$0 = context;
        this.f$1 = intent;
    }

    public final void onClick(DialogInterface dialogInterface, int i) {
        this.f$0.startActivity(this.f$1);
    }
}
