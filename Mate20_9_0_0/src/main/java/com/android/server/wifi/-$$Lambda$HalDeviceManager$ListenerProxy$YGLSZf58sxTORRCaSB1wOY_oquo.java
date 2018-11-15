package com.android.server.wifi;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$ListenerProxy$YGLSZf58sxTORRCaSB1wOY_oquo implements Runnable {
    private final /* synthetic */ ListenerProxy f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$HalDeviceManager$ListenerProxy$YGLSZf58sxTORRCaSB1wOY_oquo(ListenerProxy listenerProxy, boolean z) {
        this.f$0 = listenerProxy;
        this.f$1 = z;
    }

    public final void run() {
        this.f$0.actionWithArg(this.f$1);
    }
}
