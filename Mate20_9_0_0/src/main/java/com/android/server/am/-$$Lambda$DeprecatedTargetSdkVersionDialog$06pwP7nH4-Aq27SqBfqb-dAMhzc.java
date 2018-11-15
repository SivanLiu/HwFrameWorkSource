package com.android.server.am;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DeprecatedTargetSdkVersionDialog$06pwP7nH4-Aq27SqBfqb-dAMhzc implements OnClickListener {
    private final /* synthetic */ DeprecatedTargetSdkVersionDialog f$0;
    private final /* synthetic */ AppWarnings f$1;

    public /* synthetic */ -$$Lambda$DeprecatedTargetSdkVersionDialog$06pwP7nH4-Aq27SqBfqb-dAMhzc(DeprecatedTargetSdkVersionDialog deprecatedTargetSdkVersionDialog, AppWarnings appWarnings) {
        this.f$0 = deprecatedTargetSdkVersionDialog;
        this.f$1 = appWarnings;
    }

    public final void onClick(DialogInterface dialogInterface, int i) {
        this.f$1.setPackageFlag(this.f$0.mPackageName, 4, true);
    }
}
