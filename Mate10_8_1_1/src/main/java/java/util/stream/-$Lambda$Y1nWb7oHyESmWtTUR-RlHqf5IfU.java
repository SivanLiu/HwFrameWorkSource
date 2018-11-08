package java.util.stream;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.stream.Collectors.AnonymousClass1OptionalBox;

final /* synthetic */ class -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU implements BiConsumer {
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$0 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 0);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$1 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 1);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$10 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 10);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$11 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 11);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$12 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 12);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$13 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 13);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$14 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 14);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$2 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 2);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$3 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 3);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$4 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 4);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$5 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 5);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$6 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 6);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$7 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 7);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$8 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 8);
    public static final /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU $INST$9 = new -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU((byte) 9);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ void $m$0(Object arg0, Object arg1) {
        ((StringBuilder) arg0).append((CharSequence) arg1);
    }

    private final /* synthetic */ void $m$1(Object arg0, Object arg1) {
        ((StringJoiner) arg0).add((CharSequence) arg1);
    }

    private final /* synthetic */ void $m$10(Object arg0, Object arg1) {
        ((DoubleSummaryStatistics) arg0).combine((DoubleSummaryStatistics) arg1);
    }

    private final /* synthetic */ void $m$11(Object arg0, Object arg1) {
        IntPipeline.lambda$-java_util_stream_IntPipeline_15671((long[]) arg0, (long[]) arg1);
    }

    private final /* synthetic */ void $m$12(Object arg0, Object arg1) {
        ((IntSummaryStatistics) arg0).combine((IntSummaryStatistics) arg1);
    }

    private final /* synthetic */ void $m$13(Object arg0, Object arg1) {
        LongPipeline.lambda$-java_util_stream_LongPipeline_14862((long[]) arg0, (long[]) arg1);
    }

    private final /* synthetic */ void $m$14(Object arg0, Object arg1) {
        ((LongSummaryStatistics) arg0).combine((LongSummaryStatistics) arg1);
    }

    private final /* synthetic */ void $m$2(Object arg0, Object arg1) {
        ((AnonymousClass1OptionalBox) arg0).accept(arg1);
    }

    private final /* synthetic */ void $m$3(Object arg0, Object arg1) {
        ((Collection) arg0).add(arg1);
    }

    private final /* synthetic */ void $m$4(Object arg0, Object arg1) {
        ((List) arg0).add(arg1);
    }

    private final /* synthetic */ void $m$5(Object arg0, Object arg1) {
        ((Set) arg0).add(arg1);
    }

    private final /* synthetic */ void $m$6(Object arg0, Object arg1) {
        ((LinkedHashSet) arg0).add(arg1);
    }

    private final /* synthetic */ void $m$7(Object arg0, Object arg1) {
        ((LinkedHashSet) arg0).addAll((LinkedHashSet) arg1);
    }

    private final /* synthetic */ void $m$8(Object arg0, Object arg1) {
        DoublePipeline.lambda$-java_util_stream_DoublePipeline_16123((double[]) arg0, (double[]) arg1);
    }

    private final /* synthetic */ void $m$9(Object arg0, Object arg1) {
        DoublePipeline.lambda$-java_util_stream_DoublePipeline_14530((double[]) arg0, (double[]) arg1);
    }

    private /* synthetic */ -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU(byte b) {
        this.$id = b;
    }

    public final void accept(Object obj, Object obj2) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(obj, obj2);
                return;
            case (byte) 1:
                $m$1(obj, obj2);
                return;
            case (byte) 2:
                $m$2(obj, obj2);
                return;
            case (byte) 3:
                $m$3(obj, obj2);
                return;
            case (byte) 4:
                $m$4(obj, obj2);
                return;
            case (byte) 5:
                $m$5(obj, obj2);
                return;
            case (byte) 6:
                $m$6(obj, obj2);
                return;
            case (byte) 7:
                $m$7(obj, obj2);
                return;
            case (byte) 8:
                $m$8(obj, obj2);
                return;
            case (byte) 9:
                $m$9(obj, obj2);
                return;
            case (byte) 10:
                $m$10(obj, obj2);
                return;
            case (byte) 11:
                $m$11(obj, obj2);
                return;
            case (byte) 12:
                $m$12(obj, obj2);
                return;
            case (byte) 13:
                $m$13(obj, obj2);
                return;
            case (byte) 14:
                $m$14(obj, obj2);
                return;
            default:
                throw new AssertionError();
        }
    }
}
