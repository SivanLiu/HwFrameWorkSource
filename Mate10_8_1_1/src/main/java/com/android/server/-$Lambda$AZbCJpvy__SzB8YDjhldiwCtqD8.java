package com.android.server;

import com.android.server.NsdService.DaemonConnection;

final /* synthetic */ class -$Lambda$AZbCJpvy__SzB8YDjhldiwCtqD8 implements DaemonConnectionSupplier {
    public static final /* synthetic */ -$Lambda$AZbCJpvy__SzB8YDjhldiwCtqD8 $INST$0 = new -$Lambda$AZbCJpvy__SzB8YDjhldiwCtqD8();

    private final /* synthetic */ DaemonConnection $m$0(NativeCallbackReceiver arg0) {
        return new DaemonConnection(arg0);
    }

    private /* synthetic */ -$Lambda$AZbCJpvy__SzB8YDjhldiwCtqD8() {
    }

    public final DaemonConnection get(NativeCallbackReceiver nativeCallbackReceiver) {
        return $m$0(nativeCallbackReceiver);
    }
}
