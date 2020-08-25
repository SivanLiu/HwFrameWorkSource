package com.android.server.pm;

import android.content.pm.SharedLibraryInfo;
import java.util.function.BiConsumer;

/* renamed from: com.android.server.pm.-$$Lambda$PackageManagerService$GRQMseHE8dORyaqj3rZB-aYJdvk  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$PackageManagerService$GRQMseHE8dORyaqj3rZBaYJdvk implements BiConsumer {
    public static final /* synthetic */ $$Lambda$PackageManagerService$GRQMseHE8dORyaqj3rZBaYJdvk INSTANCE = new $$Lambda$PackageManagerService$GRQMseHE8dORyaqj3rZBaYJdvk();

    private /* synthetic */ $$Lambda$PackageManagerService$GRQMseHE8dORyaqj3rZBaYJdvk() {
    }

    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ((SharedLibraryInfo) obj).clearDependencies();
    }
}
