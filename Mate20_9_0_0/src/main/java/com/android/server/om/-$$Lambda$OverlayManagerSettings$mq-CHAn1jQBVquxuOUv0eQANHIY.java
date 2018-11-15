package com.android.server.om;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$OverlayManagerSettings$mq-CHAn1jQBVquxuOUv0eQANHIY implements Predicate {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$OverlayManagerSettings$mq-CHAn1jQBVquxuOUv0eQANHIY(String str) {
        this.f$0 = str;
    }

    public final boolean test(Object obj) {
        return ((SettingsItem) obj).getTargetPackageName().equals(this.f$0);
    }
}
