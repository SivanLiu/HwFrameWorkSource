package java.nio;

import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CharBuffer$xI2EDDPiF8RqmYmH7GJjSUKqvSY implements Supplier {
    private final /* synthetic */ CharBuffer f$0;

    public /* synthetic */ -$$Lambda$CharBuffer$xI2EDDPiF8RqmYmH7GJjSUKqvSY(CharBuffer charBuffer) {
        this.f$0 = charBuffer;
    }

    public final Object get() {
        return new CharBufferSpliterator(this.f$0);
    }
}
