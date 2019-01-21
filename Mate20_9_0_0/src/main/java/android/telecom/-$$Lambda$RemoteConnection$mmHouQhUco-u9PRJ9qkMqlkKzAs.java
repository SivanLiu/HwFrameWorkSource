package android.telecom;

import android.telecom.RemoteConnection.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteConnection$mmHouQhUco-u9PRJ9qkMqlkKzAs implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ RemoteConnection f$1;

    public /* synthetic */ -$$Lambda$RemoteConnection$mmHouQhUco-u9PRJ9qkMqlkKzAs(Callback callback, RemoteConnection remoteConnection) {
        this.f$0 = callback;
        this.f$1 = remoteConnection;
    }

    public final void run() {
        this.f$0.onRttSessionRemotelyTerminated(this.f$1);
    }
}
