package com.android.server.am;

import android.os.DropBoxManager;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityManagerService$w5jCshLsk1jfv4UDTmEfq_HU0OQ implements Runnable {
    private final /* synthetic */ DropBoxManager f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ String f$2;

    public /* synthetic */ -$$Lambda$ActivityManagerService$w5jCshLsk1jfv4UDTmEfq_HU0OQ(DropBoxManager dropBoxManager, String str, String str2) {
        this.f$0 = dropBoxManager;
        this.f$1 = str;
        this.f$2 = str2;
    }

    public final void run() {
        this.f$0.addText(this.f$1, this.f$2);
    }
}
