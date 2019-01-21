package com.android.internal.os;

import com.android.internal.os.KernelUidCpuFreqTimeReader.Callback;
import java.nio.IntBuffer;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$KernelUidCpuFreqTimeReader$s7iJKg0yjXXtqM4hsU8GS_gavIY implements Consumer {
    private final /* synthetic */ KernelUidCpuFreqTimeReader f$0;
    private final /* synthetic */ Callback f$1;

    public /* synthetic */ -$$Lambda$KernelUidCpuFreqTimeReader$s7iJKg0yjXXtqM4hsU8GS_gavIY(KernelUidCpuFreqTimeReader kernelUidCpuFreqTimeReader, Callback callback) {
        this.f$0 = kernelUidCpuFreqTimeReader;
        this.f$1 = callback;
    }

    public final void accept(Object obj) {
        KernelUidCpuFreqTimeReader.lambda$readAbsolute$1(this.f$0, this.f$1, (IntBuffer) obj);
    }
}
