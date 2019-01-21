package android.print;

import java.util.function.IntConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PrinterCapabilitiesInfo$Builder$gsgXbNHGWpWENdPzemgHcCY8HnE implements IntConsumer {
    public static final /* synthetic */ -$$Lambda$PrinterCapabilitiesInfo$Builder$gsgXbNHGWpWENdPzemgHcCY8HnE INSTANCE = new -$$Lambda$PrinterCapabilitiesInfo$Builder$gsgXbNHGWpWENdPzemgHcCY8HnE();

    private /* synthetic */ -$$Lambda$PrinterCapabilitiesInfo$Builder$gsgXbNHGWpWENdPzemgHcCY8HnE() {
    }

    public final void accept(int i) {
        PrintAttributes.enforceValidDuplexMode(i);
    }
}
