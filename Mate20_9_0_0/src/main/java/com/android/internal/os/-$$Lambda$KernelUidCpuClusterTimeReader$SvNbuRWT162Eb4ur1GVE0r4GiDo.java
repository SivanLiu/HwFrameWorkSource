package com.android.internal.os;

import com.android.internal.os.KernelUidCpuClusterTimeReader.Callback;
import java.nio.IntBuffer;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$KernelUidCpuClusterTimeReader$SvNbuRWT162Eb4ur1GVE0r4GiDo implements Consumer {
    private final /* synthetic */ KernelUidCpuClusterTimeReader f$0;
    private final /* synthetic */ Callback f$1;

    public /* synthetic */ -$$Lambda$KernelUidCpuClusterTimeReader$SvNbuRWT162Eb4ur1GVE0r4GiDo(KernelUidCpuClusterTimeReader kernelUidCpuClusterTimeReader, Callback callback) {
        this.f$0 = kernelUidCpuClusterTimeReader;
        this.f$1 = callback;
    }

    public final void accept(Object obj) {
        KernelUidCpuClusterTimeReader.lambda$readAbsolute$1(this.f$0, this.f$1, (IntBuffer) obj);
    }
}
