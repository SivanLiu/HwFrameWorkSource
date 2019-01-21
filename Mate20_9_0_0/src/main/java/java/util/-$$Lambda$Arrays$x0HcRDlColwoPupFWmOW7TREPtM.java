package java.util;

import java.util.function.IntConsumer;
import java.util.function.IntToDoubleFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Arrays$x0HcRDlColwoPupFWmOW7TREPtM implements IntConsumer {
    private final /* synthetic */ double[] f$0;
    private final /* synthetic */ IntToDoubleFunction f$1;

    public /* synthetic */ -$$Lambda$Arrays$x0HcRDlColwoPupFWmOW7TREPtM(double[] dArr, IntToDoubleFunction intToDoubleFunction) {
        this.f$0 = dArr;
        this.f$1 = intToDoubleFunction;
    }

    public final void accept(int i) {
        this.f$0[i] = this.f$1.applyAsDouble(i);
    }
}
