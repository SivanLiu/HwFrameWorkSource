package android.service.autofill;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$eWz26esczusoIA84WEwFlxQuDGQ implements Consumer {
    public static final /* synthetic */ -$$Lambda$eWz26esczusoIA84WEwFlxQuDGQ INSTANCE = new -$$Lambda$eWz26esczusoIA84WEwFlxQuDGQ();

    private /* synthetic */ -$$Lambda$eWz26esczusoIA84WEwFlxQuDGQ() {
    }

    public final void accept(Object obj) {
        ((AutofillService) obj).onDisconnected();
    }
}
