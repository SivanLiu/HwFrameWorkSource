package com.android.server.pm;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutDumpFiles$rwmVVp6PnQCcurF7D6VzrdNqEdk implements Consumer {
    private final /* synthetic */ byte[] f$0;

    public /* synthetic */ -$$Lambda$ShortcutDumpFiles$rwmVVp6PnQCcurF7D6VzrdNqEdk(byte[] bArr) {
        this.f$0 = bArr;
    }

    public final void accept(Object obj) {
        ((PrintWriter) obj).println(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(this.f$0)).toString());
    }
}
