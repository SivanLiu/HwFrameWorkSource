package java.time.format;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DateTimeFormatterBuilder$ReducedPrinterParser$O7fxxUm4cHduGbldToNj0T92oIo implements Consumer {
    private final /* synthetic */ ReducedPrinterParser f$0;
    private final /* synthetic */ DateTimeParseContext f$1;
    private final /* synthetic */ long f$2;
    private final /* synthetic */ int f$3;
    private final /* synthetic */ int f$4;

    public /* synthetic */ -$$Lambda$DateTimeFormatterBuilder$ReducedPrinterParser$O7fxxUm4cHduGbldToNj0T92oIo(ReducedPrinterParser reducedPrinterParser, DateTimeParseContext dateTimeParseContext, long j, int i, int i2) {
        this.f$0 = reducedPrinterParser;
        this.f$1 = dateTimeParseContext;
        this.f$2 = j;
        this.f$3 = i;
        this.f$4 = i2;
    }

    public final void accept(Object obj) {
        this.f$0.setValue(this.f$1, this.f$2, this.f$3, this.f$4);
    }
}
