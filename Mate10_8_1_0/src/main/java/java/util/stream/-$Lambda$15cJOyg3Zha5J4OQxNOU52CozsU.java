package java.util.stream;

import java.util.DoubleSummaryStatistics;
import java.util.function.DoubleBinaryOperator;
import java.util.function.ObjDoubleConsumer;

final /* synthetic */ class -$Lambda$15cJOyg3Zha5J4OQxNOU52CozsU implements DoubleBinaryOperator {
    public static final /* synthetic */ -$Lambda$15cJOyg3Zha5J4OQxNOU52CozsU $INST$0 = new -$Lambda$15cJOyg3Zha5J4OQxNOU52CozsU((byte) 0);
    public static final /* synthetic */ -$Lambda$15cJOyg3Zha5J4OQxNOU52CozsU $INST$1 = new -$Lambda$15cJOyg3Zha5J4OQxNOU52CozsU((byte) 1);
    private final /* synthetic */ byte $id;

    /* renamed from: java.util.stream.-$Lambda$15cJOyg3Zha5J4OQxNOU52CozsU$3 */
    final /* synthetic */ class AnonymousClass3 implements ObjDoubleConsumer {
        public static final /* synthetic */ AnonymousClass3 $INST$0 = new AnonymousClass3((byte) 0);
        public static final /* synthetic */ AnonymousClass3 $INST$1 = new AnonymousClass3((byte) 1);
        public static final /* synthetic */ AnonymousClass3 $INST$2 = new AnonymousClass3((byte) 2);
        private final /* synthetic */ byte $id;

        private final /* synthetic */ void $m$0(Object arg0, double arg1) {
            DoublePipeline.lambda$-java_util_stream_DoublePipeline_15880((double[]) arg0, arg1);
        }

        private final /* synthetic */ void $m$1(Object arg0, double arg1) {
            DoublePipeline.lambda$-java_util_stream_DoublePipeline_14331((double[]) arg0, arg1);
        }

        private final /* synthetic */ void $m$2(Object arg0, double arg1) {
            ((DoubleSummaryStatistics) arg0).accept(arg1);
        }

        private /* synthetic */ AnonymousClass3(byte b) {
            this.$id = b;
        }

        public final void accept(Object obj, double d) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(obj, d);
                    return;
                case (byte) 1:
                    $m$1(obj, d);
                    return;
                case (byte) 2:
                    $m$2(obj, d);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ double $m$0(double arg0, double arg1) {
        return Math.max(arg0, arg1);
    }

    private final /* synthetic */ double $m$1(double arg0, double arg1) {
        return Math.min(arg0, arg1);
    }

    private /* synthetic */ -$Lambda$15cJOyg3Zha5J4OQxNOU52CozsU(byte b) {
        this.$id = b;
    }

    public final double applyAsDouble(double d, double d2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(d, d2);
            case (byte) 1:
                return $m$1(d, d2);
            default:
                throw new AssertionError();
        }
    }
}
