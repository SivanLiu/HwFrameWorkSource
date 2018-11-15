package com.android.server.pc;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.CheckBox;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwPCManagerService$LABOusEbz-E8gVYSSL7I5jumj_8 implements OnClickListener {
    private final /* synthetic */ HwPCManagerService f$0;
    private final /* synthetic */ CheckBox f$1;
    private final /* synthetic */ boolean f$2;

    public /* synthetic */ -$$Lambda$HwPCManagerService$LABOusEbz-E8gVYSSL7I5jumj_8(HwPCManagerService hwPCManagerService, CheckBox checkBox, boolean z) {
        this.f$0 = hwPCManagerService;
        this.f$1 = checkBox;
        this.f$2 = z;
    }

    public final void onClick(DialogInterface dialogInterface, int i) {
        HwPCManagerService.lambda$showExitDesktopAlertDialog$5(this.f$0, this.f$1, this.f$2, dialogInterface, i);
    }
}
