package com.android.server.am;

import android.os.PowerSaveState;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityManagerService$eFxS8Z-_MXzP9a8ro45rBMHy3bk implements Consumer {
    private final /* synthetic */ ActivityManagerService f$0;

    public /* synthetic */ -$$Lambda$ActivityManagerService$eFxS8Z-_MXzP9a8ro45rBMHy3bk(ActivityManagerService activityManagerService) {
        this.f$0 = activityManagerService;
    }

    public final void accept(Object obj) {
        this.f$0.updateForceBackgroundCheck(((PowerSaveState) obj).batterySaverEnabled);
    }
}
