package com.android.server.locksettings.recoverablekeystore.storage;

import com.android.server.locksettings.recoverablekeystore.storage.RecoverySessionStorage.Entry;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RecoverySessionStorage$1ayqf2qqdJH00fvbhBUKWso4cdc implements Predicate {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$RecoverySessionStorage$1ayqf2qqdJH00fvbhBUKWso4cdc(String str) {
        this.f$0 = str;
    }

    public final boolean test(Object obj) {
        return ((Entry) obj).mSessionId.equals(this.f$0);
    }
}
