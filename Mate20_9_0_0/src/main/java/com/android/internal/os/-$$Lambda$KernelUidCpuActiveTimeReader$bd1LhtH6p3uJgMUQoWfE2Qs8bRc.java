package com.android.internal.os;

import com.android.internal.os.KernelUidCpuActiveTimeReader.Callback;
import java.nio.IntBuffer;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$KernelUidCpuActiveTimeReader$bd1LhtH6p3uJgMUQoWfE2Qs8bRc implements Consumer {
    private final /* synthetic */ KernelUidCpuActiveTimeReader f$0;
    private final /* synthetic */ Callback f$1;

    public /* synthetic */ -$$Lambda$KernelUidCpuActiveTimeReader$bd1LhtH6p3uJgMUQoWfE2Qs8bRc(KernelUidCpuActiveTimeReader kernelUidCpuActiveTimeReader, Callback callback) {
        this.f$0 = kernelUidCpuActiveTimeReader;
        this.f$1 = callback;
    }

    public final void accept(Object obj) {
        KernelUidCpuActiveTimeReader.lambda$readDeltaImpl$0(this.f$0, this.f$1, (IntBuffer) obj);
    }
}
