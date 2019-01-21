package com.android.internal.os;

import com.android.internal.os.KernelUidCpuClusterTimeReader.Callback;
import java.nio.IntBuffer;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$KernelUidCpuClusterTimeReader$j4vHMa0qvl5KRBiWr-LkFJbasC8 implements Consumer {
    private final /* synthetic */ KernelUidCpuClusterTimeReader f$0;
    private final /* synthetic */ Callback f$1;

    public /* synthetic */ -$$Lambda$KernelUidCpuClusterTimeReader$j4vHMa0qvl5KRBiWr-LkFJbasC8(KernelUidCpuClusterTimeReader kernelUidCpuClusterTimeReader, Callback callback) {
        this.f$0 = kernelUidCpuClusterTimeReader;
        this.f$1 = callback;
    }

    public final void accept(Object obj) {
        KernelUidCpuClusterTimeReader.lambda$readDeltaImpl$0(this.f$0, this.f$1, (IntBuffer) obj);
    }
}
