package com.android.server;

import android.os.IBinder;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TextServicesManagerService$SpellCheckerBindGroup$WPb2Qavn5gWhsY_rCdz_4UGBTAw implements Predicate {
    private final /* synthetic */ IBinder f$0;

    public /* synthetic */ -$$Lambda$TextServicesManagerService$SpellCheckerBindGroup$WPb2Qavn5gWhsY_rCdz_4UGBTAw(IBinder iBinder) {
        this.f$0 = iBinder;
    }

    public final boolean test(Object obj) {
        return SpellCheckerBindGroup.lambda$removeListener$0(this.f$0, (SessionRequest) obj);
    }
}
