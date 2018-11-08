package java.util.stream;

import java.util.IntSummaryStatistics;
import java.util.function.IntBinaryOperator;
import java.util.function.ObjIntConsumer;

final /* synthetic */ class -$Lambda$QgGTJrv63_zzBbeGjswm_UMqEbo implements IntBinaryOperator {
    public static final /* synthetic */ -$Lambda$QgGTJrv63_zzBbeGjswm_UMqEbo $INST$0 = new -$Lambda$QgGTJrv63_zzBbeGjswm_UMqEbo((byte) 0);
    public static final /* synthetic */ -$Lambda$QgGTJrv63_zzBbeGjswm_UMqEbo $INST$1 = new -$Lambda$QgGTJrv63_zzBbeGjswm_UMqEbo((byte) 1);
    public static final /* synthetic */ -$Lambda$QgGTJrv63_zzBbeGjswm_UMqEbo $INST$2 = new -$Lambda$QgGTJrv63_zzBbeGjswm_UMqEbo((byte) 2);
    private final /* synthetic */ byte $id;

    /* renamed from: java.util.stream.-$Lambda$QgGTJrv63_zzBbeGjswm_UMqEbo$2 */
    final /* synthetic */ class AnonymousClass2 implements ObjIntConsumer {
        public static final /* synthetic */ AnonymousClass2 $INST$0 = new AnonymousClass2((byte) 0);
        public static final /* synthetic */ AnonymousClass2 $INST$1 = new AnonymousClass2((byte) 1);
        private final /* synthetic */ byte $id;

        private final /* synthetic */ void $m$0(Object arg0, int arg1) {
            IntPipeline.lambda$-java_util_stream_IntPipeline_15510((long[]) arg0, arg1);
        }

        private final /* synthetic */ void $m$1(Object arg0, int arg1) {
            ((IntSummaryStatistics) arg0).accept(arg1);
        }

        private /* synthetic */ AnonymousClass2(byte b) {
            this.$id = b;
        }

        public final void accept(Object obj, int i) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(obj, i);
                    return;
                case (byte) 1:
                    $m$1(obj, i);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ int $m$0(int arg0, int arg1) {
        return Math.max(arg0, arg1);
    }

    private final /* synthetic */ int $m$1(int arg0, int arg1) {
        return Math.min(arg0, arg1);
    }

    private final /* synthetic */ int $m$2(int arg0, int arg1) {
        return Integer.sum(arg0, arg1);
    }

    private /* synthetic */ -$Lambda$QgGTJrv63_zzBbeGjswm_UMqEbo(byte b) {
        this.$id = b;
    }

    public final int applyAsInt(int i, int i2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(i, i2);
            case (byte) 1:
                return $m$1(i, i2);
            case (byte) 2:
                return $m$2(i, i2);
            default:
                throw new AssertionError();
        }
    }
}
