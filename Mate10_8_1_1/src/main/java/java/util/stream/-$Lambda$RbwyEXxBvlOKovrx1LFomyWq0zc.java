package java.util.stream;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors.AnonymousClass1OptionalBox;
import java.util.stream.Node.OfDouble;
import java.util.stream.Node.OfInt;
import java.util.stream.Node.OfLong;

final /* synthetic */ class -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc implements BinaryOperator {
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$0 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 0);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$1 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 1);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$10 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 10);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$11 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 11);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$12 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 12);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$13 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 13);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$14 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 14);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$15 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 15);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$16 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 16);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$17 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc(Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$18 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 18);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$19 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 19);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$2 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 2);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$20 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 20);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$3 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 3);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$4 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 4);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$5 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 5);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$6 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 6);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$7 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 7);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$8 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 8);
    public static final /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc $INST$9 = new -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc((byte) 9);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ Object $m$0(Object arg0, Object arg1) {
        return Collectors.lambda$-java_util_stream_Collectors_27185((double[]) arg0, (double[]) arg1);
    }

    private final /* synthetic */ Object $m$1(Object arg0, Object arg1) {
        return Collectors.lambda$-java_util_stream_Collectors_24486((long[]) arg0, (long[]) arg1);
    }

    private final /* synthetic */ Object $m$10(Object arg0, Object arg1) {
        return Collectors.lambda$-java_util_stream_Collectors_22206((double[]) arg0, (double[]) arg1);
    }

    private final /* synthetic */ Object $m$11(Object arg0, Object arg1) {
        return ((int[]) arg0)[0] = ((int[]) arg0)[0] + ((int[]) arg1)[0];
    }

    private final /* synthetic */ Object $m$12(Object arg0, Object arg1) {
        return ((long[]) arg0)[0] = ((long[]) arg0)[0] + ((long[]) arg1)[0];
    }

    private final /* synthetic */ Object $m$13(Object arg0, Object arg1) {
        return Collectors.lambda$-java_util_stream_Collectors_5845(arg0, arg1);
    }

    private final /* synthetic */ Object $m$14(Object arg0, Object arg1) {
        return ((Collection) arg0).addAll((Collection) arg1);
    }

    private final /* synthetic */ Object $m$15(Object arg0, Object arg1) {
        return ((List) arg0).addAll((List) arg1);
    }

    private final /* synthetic */ Object $m$16(Object arg0, Object arg1) {
        return ((Set) arg0).addAll((Set) arg1);
    }

    private final /* synthetic */ Object $m$17(Object arg0, Object arg1) {
        return new OfDouble((OfDouble) arg0, (OfDouble) arg1);
    }

    private final /* synthetic */ Object $m$18(Object arg0, Object arg1) {
        return new OfInt((OfInt) arg0, (OfInt) arg1);
    }

    private final /* synthetic */ Object $m$19(Object arg0, Object arg1) {
        return new OfLong((OfLong) arg0, (OfLong) arg1);
    }

    private final /* synthetic */ Object $m$2(Object arg0, Object arg1) {
        return Collectors.lambda$-java_util_stream_Collectors_25283((long[]) arg0, (long[]) arg1);
    }

    private final /* synthetic */ Object $m$20(Object arg0, Object arg1) {
        return new ConcNode((Node) arg0, (Node) arg1);
    }

    private final /* synthetic */ Object $m$3(Object arg0, Object arg1) {
        return Long.valueOf(Long.sum(((Long) arg0).longValue(), ((Long) arg1).longValue()));
    }

    private final /* synthetic */ Object $m$4(Object arg0, Object arg1) {
        return ((StringBuilder) arg0).append((CharSequence) (StringBuilder) arg1);
    }

    private final /* synthetic */ Object $m$5(Object arg0, Object arg1) {
        return ((StringJoiner) arg0).merge((StringJoiner) arg1);
    }

    private final /* synthetic */ Object $m$6(Object arg0, Object arg1) {
        return Collectors.lambda$-java_util_stream_Collectors_30747((AnonymousClass1OptionalBox) arg0, (AnonymousClass1OptionalBox) arg1);
    }

    private final /* synthetic */ Object $m$7(Object arg0, Object arg1) {
        return ((DoubleSummaryStatistics) arg0).combine((DoubleSummaryStatistics) arg1);
    }

    private final /* synthetic */ Object $m$8(Object arg0, Object arg1) {
        return ((IntSummaryStatistics) arg0).combine((IntSummaryStatistics) arg1);
    }

    private final /* synthetic */ Object $m$9(Object arg0, Object arg1) {
        return ((LongSummaryStatistics) arg0).combine((LongSummaryStatistics) arg1);
    }

    private /* synthetic */ -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc(byte b) {
        this.$id = b;
    }

    public final Object apply(Object obj, Object obj2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj, obj2);
            case (byte) 1:
                return $m$1(obj, obj2);
            case (byte) 2:
                return $m$2(obj, obj2);
            case (byte) 3:
                return $m$3(obj, obj2);
            case (byte) 4:
                return $m$4(obj, obj2);
            case (byte) 5:
                return $m$5(obj, obj2);
            case (byte) 6:
                return $m$6(obj, obj2);
            case (byte) 7:
                return $m$7(obj, obj2);
            case (byte) 8:
                return $m$8(obj, obj2);
            case (byte) 9:
                return $m$9(obj, obj2);
            case (byte) 10:
                return $m$10(obj, obj2);
            case (byte) 11:
                return $m$11(obj, obj2);
            case (byte) 12:
                return $m$12(obj, obj2);
            case (byte) 13:
                return $m$13(obj, obj2);
            case (byte) 14:
                return $m$14(obj, obj2);
            case (byte) 15:
                return $m$15(obj, obj2);
            case (byte) 16:
                return $m$16(obj, obj2);
            case (byte) 17:
                return $m$17(obj, obj2);
            case (byte) 18:
                return $m$18(obj, obj2);
            case (byte) 19:
                return $m$19(obj, obj2);
            case (byte) 20:
                return $m$20(obj, obj2);
            default:
                throw new AssertionError();
        }
    }
}
