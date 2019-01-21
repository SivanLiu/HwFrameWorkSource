package android.print;

import java.util.function.IntConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PrinterCapabilitiesInfo$2mJhwjGC7Dgi0vwDsnG83V2s6sE implements IntConsumer {
    public static final /* synthetic */ -$$Lambda$PrinterCapabilitiesInfo$2mJhwjGC7Dgi0vwDsnG83V2s6sE INSTANCE = new -$$Lambda$PrinterCapabilitiesInfo$2mJhwjGC7Dgi0vwDsnG83V2s6sE();

    private /* synthetic */ -$$Lambda$PrinterCapabilitiesInfo$2mJhwjGC7Dgi0vwDsnG83V2s6sE() {
    }

    public final void accept(int i) {
        PrintAttributes.enforceValidColorMode(i);
    }
}
