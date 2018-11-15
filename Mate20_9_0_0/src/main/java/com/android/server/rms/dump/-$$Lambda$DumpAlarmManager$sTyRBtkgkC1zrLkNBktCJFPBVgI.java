package com.android.server.rms.dump;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DumpAlarmManager$sTyRBtkgkC1zrLkNBktCJFPBVgI implements Consumer {
    public static final /* synthetic */ -$$Lambda$DumpAlarmManager$sTyRBtkgkC1zrLkNBktCJFPBVgI INSTANCE = new -$$Lambda$DumpAlarmManager$sTyRBtkgkC1zrLkNBktCJFPBVgI();

    private /* synthetic */ -$$Lambda$DumpAlarmManager$sTyRBtkgkC1zrLkNBktCJFPBVgI() {
    }

    public final void accept(Object obj) {
        DumpAlarmManager.delay(((Params) obj).context, ((Params) obj).pw, ((Params) obj).args);
    }
}
