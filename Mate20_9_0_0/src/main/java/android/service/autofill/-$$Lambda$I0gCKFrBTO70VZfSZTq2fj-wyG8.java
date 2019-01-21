package android.service.autofill;

import android.os.CancellationSignal;
import com.android.internal.util.function.QuadConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$I0gCKFrBTO70VZfSZTq2fj-wyG8 implements QuadConsumer {
    public static final /* synthetic */ -$$Lambda$I0gCKFrBTO70VZfSZTq2fj-wyG8 INSTANCE = new -$$Lambda$I0gCKFrBTO70VZfSZTq2fj-wyG8();

    private /* synthetic */ -$$Lambda$I0gCKFrBTO70VZfSZTq2fj-wyG8() {
    }

    public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
        ((AutofillService) obj).onFillRequest((FillRequest) obj2, (CancellationSignal) obj3, (FillCallback) obj4);
    }
}
