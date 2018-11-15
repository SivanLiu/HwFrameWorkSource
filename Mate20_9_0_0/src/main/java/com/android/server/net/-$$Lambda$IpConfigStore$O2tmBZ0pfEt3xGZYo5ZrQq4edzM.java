package com.android.server.net;

import android.util.SparseArray;
import com.android.server.net.DelayedDiskWrite.Writer;
import java.io.DataOutputStream;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpConfigStore$O2tmBZ0pfEt3xGZYo5ZrQq4edzM implements Writer {
    private final /* synthetic */ SparseArray f$0;

    public /* synthetic */ -$$Lambda$IpConfigStore$O2tmBZ0pfEt3xGZYo5ZrQq4edzM(SparseArray sparseArray) {
        this.f$0 = sparseArray;
    }

    public final void onWriteCalled(DataOutputStream dataOutputStream) {
        IpConfigStore.lambda$writeIpAndProxyConfigurationsToFile$0(this.f$0, dataOutputStream);
    }
}
