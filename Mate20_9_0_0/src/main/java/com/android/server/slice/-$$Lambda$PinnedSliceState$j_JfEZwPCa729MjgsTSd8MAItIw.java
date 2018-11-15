package com.android.server.slice;

import android.app.slice.SliceSpec;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PinnedSliceState$j_JfEZwPCa729MjgsTSd8MAItIw implements Function {
    private final /* synthetic */ PinnedSliceState f$0;
    private final /* synthetic */ SliceSpec[] f$1;

    public /* synthetic */ -$$Lambda$PinnedSliceState$j_JfEZwPCa729MjgsTSd8MAItIw(PinnedSliceState pinnedSliceState, SliceSpec[] sliceSpecArr) {
        this.f$0 = pinnedSliceState;
        this.f$1 = sliceSpecArr;
    }

    public final Object apply(Object obj) {
        return PinnedSliceState.lambda$mergeSpecs$0(this.f$0, this.f$1, (SliceSpec) obj);
    }
}
