package com.android.internal.os;

import com.android.internal.os.KernelUidCpuFreqTimeReader.Callback;
import java.nio.IntBuffer;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$KernelUidCpuFreqTimeReader$_LfRKir9FA4B4VL15YGHagRZaR8 implements Consumer {
    private final /* synthetic */ KernelUidCpuFreqTimeReader f$0;
    private final /* synthetic */ Callback f$1;

    public /* synthetic */ -$$Lambda$KernelUidCpuFreqTimeReader$_LfRKir9FA4B4VL15YGHagRZaR8(KernelUidCpuFreqTimeReader kernelUidCpuFreqTimeReader, Callback callback) {
        this.f$0 = kernelUidCpuFreqTimeReader;
        this.f$1 = callback;
    }

    public final void accept(Object obj) {
        KernelUidCpuFreqTimeReader.lambda$readDeltaImpl$0(this.f$0, this.f$1, (IntBuffer) obj);
    }
}
