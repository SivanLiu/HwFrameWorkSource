package java.util;

import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Arrays$KFf05FUz26CqVc_cf2bKY9C927o implements IntConsumer {
    private final /* synthetic */ int[] f$0;
    private final /* synthetic */ IntUnaryOperator f$1;

    public /* synthetic */ -$$Lambda$Arrays$KFf05FUz26CqVc_cf2bKY9C927o(int[] iArr, IntUnaryOperator intUnaryOperator) {
        this.f$0 = iArr;
        this.f$1 = intUnaryOperator;
    }

    public final void accept(int i) {
        this.f$0[i] = this.f$1.applyAsInt(i);
    }
}
