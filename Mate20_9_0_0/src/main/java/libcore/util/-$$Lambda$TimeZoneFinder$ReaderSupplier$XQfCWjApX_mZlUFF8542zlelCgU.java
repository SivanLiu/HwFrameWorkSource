package libcore.util;

import java.io.Reader;
import java.io.StringReader;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TimeZoneFinder$ReaderSupplier$XQfCWjApX_mZlUFF8542zlelCgU implements ReaderSupplier {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$TimeZoneFinder$ReaderSupplier$XQfCWjApX_mZlUFF8542zlelCgU(String str) {
        this.f$0 = str;
    }

    public final Reader get() {
        return new StringReader(this.f$0);
    }
}
