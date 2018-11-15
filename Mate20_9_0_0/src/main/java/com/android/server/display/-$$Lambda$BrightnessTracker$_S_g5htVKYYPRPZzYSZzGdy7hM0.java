package com.android.server.display;

import java.io.PrintWriter;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BrightnessTracker$_S_g5htVKYYPRPZzYSZzGdy7hM0 implements Runnable {
    private final /* synthetic */ BrightnessTracker f$0;
    private final /* synthetic */ PrintWriter f$1;

    public /* synthetic */ -$$Lambda$BrightnessTracker$_S_g5htVKYYPRPZzYSZzGdy7hM0(BrightnessTracker brightnessTracker, PrintWriter printWriter) {
        this.f$0 = brightnessTracker;
        this.f$1 = printWriter;
    }

    public final void run() {
        this.f$0.dumpLocal(this.f$1);
    }
}
