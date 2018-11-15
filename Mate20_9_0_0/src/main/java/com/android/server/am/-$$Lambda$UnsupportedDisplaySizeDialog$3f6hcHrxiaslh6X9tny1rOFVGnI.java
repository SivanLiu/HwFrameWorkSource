package com.android.server.am;

import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UnsupportedDisplaySizeDialog$3f6hcHrxiaslh6X9tny1rOFVGnI implements OnCheckedChangeListener {
    private final /* synthetic */ UnsupportedDisplaySizeDialog f$0;
    private final /* synthetic */ AppWarnings f$1;

    public /* synthetic */ -$$Lambda$UnsupportedDisplaySizeDialog$3f6hcHrxiaslh6X9tny1rOFVGnI(UnsupportedDisplaySizeDialog unsupportedDisplaySizeDialog, AppWarnings appWarnings) {
        this.f$0 = unsupportedDisplaySizeDialog;
        this.f$1 = appWarnings;
    }

    public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        this.f$1.setPackageFlag(this.f$0.mPackageName, 1, z ^ 1);
    }
}
