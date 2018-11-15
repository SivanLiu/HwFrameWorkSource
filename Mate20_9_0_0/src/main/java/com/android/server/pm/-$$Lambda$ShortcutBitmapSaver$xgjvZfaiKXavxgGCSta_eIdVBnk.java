package com.android.server.pm;

import java.util.concurrent.CountDownLatch;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutBitmapSaver$xgjvZfaiKXavxgGCSta_eIdVBnk implements Runnable {
    private final /* synthetic */ CountDownLatch f$0;

    public /* synthetic */ -$$Lambda$ShortcutBitmapSaver$xgjvZfaiKXavxgGCSta_eIdVBnk(CountDownLatch countDownLatch) {
        this.f$0 = countDownLatch;
    }

    public final void run() {
        this.f$0.countDown();
    }
}
