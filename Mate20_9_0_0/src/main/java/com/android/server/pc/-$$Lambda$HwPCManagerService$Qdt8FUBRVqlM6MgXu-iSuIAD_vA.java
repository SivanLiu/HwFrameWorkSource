package com.android.server.pc;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwPCManagerService$Qdt8FUBRVqlM6MgXu-iSuIAD_vA implements OnDismissListener {
    private final /* synthetic */ HwPCManagerService f$0;

    public /* synthetic */ -$$Lambda$HwPCManagerService$Qdt8FUBRVqlM6MgXu-iSuIAD_vA(HwPCManagerService hwPCManagerService) {
        this.f$0 = hwPCManagerService;
    }

    public final void onDismiss(DialogInterface dialogInterface) {
        HwPCManagerService.lambda$showExitDesktopAlertDialog$7(this.f$0, dialogInterface);
    }
}
