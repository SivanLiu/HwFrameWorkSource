package android.telecom;

import android.telecom.RemoteConnection.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteConnection$AwagQDJDcNDplrFif6DlYZldL5E implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ RemoteConnection f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$RemoteConnection$AwagQDJDcNDplrFif6DlYZldL5E(Callback callback, RemoteConnection remoteConnection, int i) {
        this.f$0 = callback;
        this.f$1 = remoteConnection;
        this.f$2 = i;
    }

    public final void run() {
        this.f$0.onRttInitiationFailure(this.f$1, this.f$2);
    }
}
