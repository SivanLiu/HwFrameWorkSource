package java.util.stream;

import java.util.function.IntConsumer;
import java.util.stream.IntPipeline.7.AnonymousClass1;

final /* synthetic */ class -$Lambda$nO1MaU0vQHo4iVZemtT1k9gUtrc implements IntConsumer {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0(int arg0) {
        ((AnonymousClass1) this.-$f0).lambda$-java_util_stream_IntPipeline$7$1_11907(arg0);
    }

    private final /* synthetic */ void $m$1(int arg0) {
        ((Sink) this.-$f0).accept(arg0);
    }

    private final /* synthetic */ void $m$2(int arg0) {
        ((Sink) this.-$f0).accept(arg0);
    }

    public /* synthetic */ -$Lambda$nO1MaU0vQHo4iVZemtT1k9gUtrc(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final void accept(int i) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(i);
                return;
            case (byte) 1:
                $m$1(i);
                return;
            case (byte) 2:
                $m$2(i);
                return;
            default:
                throw new AssertionError();
        }
    }
}
