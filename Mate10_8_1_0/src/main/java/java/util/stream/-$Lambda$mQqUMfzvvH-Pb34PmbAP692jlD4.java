package java.util.stream;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.DistinctOps.AnonymousClass1;

final /* synthetic */ class -$Lambda$mQqUMfzvvH-Pb34PmbAP692jlD4 implements Consumer {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0(Object arg0) {
        AnonymousClass1.lambda$-java_util_stream_DistinctOps$1_3835((AtomicBoolean) this.-$f0, (ConcurrentHashMap) this.-$f1, arg0);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        ((BiConsumer) this.-$f0).accept(this.-$f1, arg0);
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        ((DistinctSpliterator) this.-$f0).lambda$-java_util_stream_StreamSpliterators$DistinctSpliterator_46149((Consumer) this.-$f1, arg0);
    }

    public /* synthetic */ -$Lambda$mQqUMfzvvH-Pb34PmbAP692jlD4(byte b, Object obj, Object obj2) {
        this.$id = b;
        this.-$f0 = obj;
        this.-$f1 = obj2;
    }

    public final void accept(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(obj);
                return;
            case (byte) 1:
                $m$1(obj);
                return;
            case (byte) 2:
                $m$2(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
