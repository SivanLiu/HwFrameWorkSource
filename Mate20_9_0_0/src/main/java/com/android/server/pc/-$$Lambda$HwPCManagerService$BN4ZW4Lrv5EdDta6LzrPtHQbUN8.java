package com.android.server.pc;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwPCManagerService$BN4ZW4Lrv5EdDta6LzrPtHQbUN8 implements OnDismissListener {
    private final /* synthetic */ HwPCManagerService f$0;

    public /* synthetic */ -$$Lambda$HwPCManagerService$BN4ZW4Lrv5EdDta6LzrPtHQbUN8(HwPCManagerService hwPCManagerService) {
        this.f$0 = hwPCManagerService;
    }

    public final void onDismiss(DialogInterface dialogInterface) {
        HwPCManagerService.lambda$showEnterDesktopAlertDialog$3(this.f$0, dialogInterface);
    }
}
