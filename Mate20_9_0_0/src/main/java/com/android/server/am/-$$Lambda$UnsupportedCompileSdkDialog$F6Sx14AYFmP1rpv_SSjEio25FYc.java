package com.android.server.am;

import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UnsupportedCompileSdkDialog$F6Sx14AYFmP1rpv_SSjEio25FYc implements OnCheckedChangeListener {
    private final /* synthetic */ UnsupportedCompileSdkDialog f$0;
    private final /* synthetic */ AppWarnings f$1;

    public /* synthetic */ -$$Lambda$UnsupportedCompileSdkDialog$F6Sx14AYFmP1rpv_SSjEio25FYc(UnsupportedCompileSdkDialog unsupportedCompileSdkDialog, AppWarnings appWarnings) {
        this.f$0 = unsupportedCompileSdkDialog;
        this.f$1 = appWarnings;
    }

    public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        this.f$1.setPackageFlag(this.f$0.mPackageName, 2, z ^ 1);
    }
}
