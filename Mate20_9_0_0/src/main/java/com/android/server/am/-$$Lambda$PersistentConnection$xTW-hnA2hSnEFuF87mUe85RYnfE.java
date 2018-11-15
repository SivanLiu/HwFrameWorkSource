package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PersistentConnection$xTW-hnA2hSnEFuF87mUe85RYnfE implements Runnable {
    private final /* synthetic */ PersistentConnection f$0;

    public /* synthetic */ -$$Lambda$PersistentConnection$xTW-hnA2hSnEFuF87mUe85RYnfE(PersistentConnection persistentConnection) {
        this.f$0 = persistentConnection;
    }

    public final void run() {
        this.f$0.bindForBackoff();
    }
}
