package com.android.server.connectivity;

import android.net.LinkProperties;
import android.util.Pair;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DnsManager$PrivateDnsValidationStatuses$_X4_M08nKysv-L4hDpqAsa4SBxI implements BiConsumer {
    private final /* synthetic */ LinkProperties f$0;

    public /* synthetic */ -$$Lambda$DnsManager$PrivateDnsValidationStatuses$_X4_M08nKysv-L4hDpqAsa4SBxI(LinkProperties linkProperties) {
        this.f$0 = linkProperties;
    }

    public final void accept(Object obj, Object obj2) {
        PrivateDnsValidationStatuses.lambda$fillInValidatedPrivateDns$0(this.f$0, (Pair) obj, (ValidationStatus) obj2);
    }
}
