package com.android.server.ethernet;

import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EthernetTracker$rMvwcG7iXM6tWTHAeE-h6PlZCT8 implements Runnable {
    private final /* synthetic */ EthernetTracker f$0;
    private final /* synthetic */ IndentingPrintWriter f$1;
    private final /* synthetic */ FileDescriptor f$2;
    private final /* synthetic */ String[] f$3;

    public /* synthetic */ -$$Lambda$EthernetTracker$rMvwcG7iXM6tWTHAeE-h6PlZCT8(EthernetTracker ethernetTracker, IndentingPrintWriter indentingPrintWriter, FileDescriptor fileDescriptor, String[] strArr) {
        this.f$0 = ethernetTracker;
        this.f$1 = indentingPrintWriter;
        this.f$2 = fileDescriptor;
        this.f$3 = strArr;
    }

    public final void run() {
        EthernetTracker.lambda$dump$1(this.f$0, this.f$1, this.f$2, this.f$3);
    }
}
