package java.util.stream;

import java.util.function.LongConsumer;
import java.util.function.ToLongFunction;

final /* synthetic */ class -$Lambda$zzPjrAVaRLtDBFYnKhJM_a3VmMk implements ToLongFunction {
    public static final /* synthetic */ -$Lambda$zzPjrAVaRLtDBFYnKhJM_a3VmMk $INST$0 = new -$Lambda$zzPjrAVaRLtDBFYnKhJM_a3VmMk((byte) 0);
    public static final /* synthetic */ -$Lambda$zzPjrAVaRLtDBFYnKhJM_a3VmMk $INST$1 = new -$Lambda$zzPjrAVaRLtDBFYnKhJM_a3VmMk((byte) 1);
    private final /* synthetic */ byte $id;

    /* renamed from: java.util.stream.-$Lambda$zzPjrAVaRLtDBFYnKhJM_a3VmMk$1 */
    final /* synthetic */ class AnonymousClass1 implements LongConsumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(long arg0) {
            ((java.util.stream.LongPipeline.6.AnonymousClass1) this.-$f0).lambda$-java_util_stream_LongPipeline$6$1_11125(arg0);
        }

        private final /* synthetic */ void $m$1(long arg0) {
            ((Sink) this.-$f0).accept(arg0);
        }

        private final /* synthetic */ void $m$2(long arg0) {
            ((Sink) this.-$f0).accept(arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final void accept(long j) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(j);
                    return;
                case (byte) 1:
                    $m$1(j);
                    return;
                case (byte) 2:
                    $m$2(j);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ long $m$0(Object arg0) {
        return ((Long) arg0).longValue();
    }

    private final /* synthetic */ long $m$1(Object arg0) {
        return 1;
    }

    private /* synthetic */ -$Lambda$zzPjrAVaRLtDBFYnKhJM_a3VmMk(byte b) {
        this.$id = b;
    }

    public final long applyAsLong(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj);
            case (byte) 1:
                return $m$1(obj);
            default:
                throw new AssertionError();
        }
    }
}
