package com.android.server;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SystemServerInitThreadPool$7wfLGkZF7FvYZv7xj3ghvuiJJGk implements Runnable {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ Runnable f$1;

    public /* synthetic */ -$$Lambda$SystemServerInitThreadPool$7wfLGkZF7FvYZv7xj3ghvuiJJGk(String str, Runnable runnable) {
        this.f$0 = str;
        this.f$1 = runnable;
    }

    public final void run() {
        SystemServerInitThreadPool.lambda$submit$0(this.f$0, this.f$1);
    }
}
