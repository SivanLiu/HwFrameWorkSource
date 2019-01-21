package android.telecom;

import android.telecom.RemoteConnection.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteConnection$yp1cNJ53RzQGFz3RZRlC3urzQv4 implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ RemoteConnection f$1;

    public /* synthetic */ -$$Lambda$RemoteConnection$yp1cNJ53RzQGFz3RZRlC3urzQv4(Callback callback, RemoteConnection remoteConnection) {
        this.f$0 = callback;
        this.f$1 = remoteConnection;
    }

    public final void run() {
        this.f$0.onRemoteRttRequest(this.f$1);
    }
}
