package android.telecom;

import android.telecom.RemoteConnection.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteConnection$C4t0J6QK31Ef1UFsdPVwkew1VaQ implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ RemoteConnection f$1;

    public /* synthetic */ -$$Lambda$RemoteConnection$C4t0J6QK31Ef1UFsdPVwkew1VaQ(Callback callback, RemoteConnection remoteConnection) {
        this.f$0 = callback;
        this.f$1 = remoteConnection;
    }

    public final void run() {
        this.f$0.onRttInitiationSuccess(this.f$1);
    }
}
