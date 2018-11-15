package com.android.server.net;

import android.util.ArrayMap;
import com.android.server.net.DelayedDiskWrite.Writer;
import java.io.DataOutputStream;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpConfigStore$rFY3yG3j6RGRgrQey7yYfi0Yze0 implements Writer {
    private final /* synthetic */ ArrayMap f$0;

    public /* synthetic */ -$$Lambda$IpConfigStore$rFY3yG3j6RGRgrQey7yYfi0Yze0(ArrayMap arrayMap) {
        this.f$0 = arrayMap;
    }

    public final void onWriteCalled(DataOutputStream dataOutputStream) {
        IpConfigStore.lambda$writeIpConfigurations$1(this.f$0, dataOutputStream);
    }
}
