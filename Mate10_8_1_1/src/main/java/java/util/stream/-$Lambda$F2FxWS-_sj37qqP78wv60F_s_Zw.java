package java.util.stream;

import java.util.Spliterator;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors.AnonymousClass1OptionalBox;

final /* synthetic */ class -$Lambda$F2FxWS-_sj37qqP78wv60F_s_Zw implements Supplier {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ Object $m$0() {
        return ((AbstractPipeline) this.-$f0).lambda$-java_util_stream_AbstractPipeline_14339();
    }

    private final /* synthetic */ Object $m$1() {
        return AbstractPipeline.lambda$-java_util_stream_AbstractPipeline_20439((Spliterator) this.-$f0);
    }

    private final /* synthetic */ Object $m$2() {
        return new Object[]{this.-$f0};
    }

    private final /* synthetic */ Object $m$3() {
        return new Partition(((Collector) this.-$f0).supplier().get(), ((Collector) this.-$f0).supplier().get());
    }

    private final /* synthetic */ Object $m$4() {
        return new AnonymousClass1OptionalBox((BinaryOperator) this.-$f0);
    }

    public /* synthetic */ -$Lambda$F2FxWS-_sj37qqP78wv60F_s_Zw(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final Object get() {
        switch (this.$id) {
            case (byte) 0:
                return $m$0();
            case (byte) 1:
                return $m$1();
            case (byte) 2:
                return $m$2();
            case (byte) 3:
                return $m$3();
            case (byte) 4:
                return $m$4();
            default:
                throw new AssertionError();
        }
    }
}
