package libcore.util;

import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TimeZoneFinder$ReaderSupplier$IAVNuAYizGfcsPtGXEBkDPhlBF0 implements ReaderSupplier {
    private final /* synthetic */ Path f$0;
    private final /* synthetic */ Charset f$1;

    public /* synthetic */ -$$Lambda$TimeZoneFinder$ReaderSupplier$IAVNuAYizGfcsPtGXEBkDPhlBF0(Path path, Charset charset) {
        this.f$0 = path;
        this.f$1 = charset;
    }

    public final Reader get() {
        return Files.newBufferedReader(this.f$0, this.f$1);
    }
}
