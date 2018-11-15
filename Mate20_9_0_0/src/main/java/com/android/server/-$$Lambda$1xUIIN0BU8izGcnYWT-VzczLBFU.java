package com.android.server;

import com.android.server.NsdService.DaemonConnection;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$1xUIIN0BU8izGcnYWT-VzczLBFU implements DaemonConnectionSupplier {
    public static final /* synthetic */ -$$Lambda$1xUIIN0BU8izGcnYWT-VzczLBFU INSTANCE = new -$$Lambda$1xUIIN0BU8izGcnYWT-VzczLBFU();

    private /* synthetic */ -$$Lambda$1xUIIN0BU8izGcnYWT-VzczLBFU() {
    }

    public final DaemonConnection get(NativeCallbackReceiver nativeCallbackReceiver) {
        return new DaemonConnection(nativeCallbackReceiver);
    }
}
