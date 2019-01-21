package android.print;

import java.util.function.IntConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PrinterCapabilitiesInfo$Builder$dbsSt8pZfd6hqZ6hGCnpzhPK6Uk implements IntConsumer {
    public static final /* synthetic */ -$$Lambda$PrinterCapabilitiesInfo$Builder$dbsSt8pZfd6hqZ6hGCnpzhPK6Uk INSTANCE = new -$$Lambda$PrinterCapabilitiesInfo$Builder$dbsSt8pZfd6hqZ6hGCnpzhPK6Uk();

    private /* synthetic */ -$$Lambda$PrinterCapabilitiesInfo$Builder$dbsSt8pZfd6hqZ6hGCnpzhPK6Uk() {
    }

    public final void accept(int i) {
        PrintAttributes.enforceValidColorMode(i);
    }
}
