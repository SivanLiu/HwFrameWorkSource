package com.android.server.wm;

import android.util.proto.ProtoOutputStream;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RootWindowContainer$WK0a_BR42j4A-e0Xx1wj4BL8rUk implements Consumer {
    private final /* synthetic */ ProtoOutputStream f$0;

    public /* synthetic */ -$$Lambda$RootWindowContainer$WK0a_BR42j4A-e0Xx1wj4BL8rUk(ProtoOutputStream protoOutputStream) {
        this.f$0 = protoOutputStream;
    }

    public final void accept(Object obj) {
        ((WindowState) obj).writeIdentifierToProto(this.f$0, 2246267895811L);
    }
}
