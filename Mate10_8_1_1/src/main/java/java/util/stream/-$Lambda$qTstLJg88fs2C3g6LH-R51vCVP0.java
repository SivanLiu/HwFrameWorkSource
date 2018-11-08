package java.util.stream;

import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors.AnonymousClass1OptionalBox;

final /* synthetic */ class -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 implements Function {
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$0 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 0);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$1 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 1);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$10 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 10);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$11 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 11);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$12 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 12);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$2 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 2);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$3 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 3);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$4 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 4);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$5 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 5);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$6 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 6);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$7 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 7);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$8 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 8);
    public static final /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0 $INST$9 = new -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0((byte) 9);
    private final /* synthetic */ byte $id;

    /* renamed from: java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0$1 */
    final /* synthetic */ class AnonymousClass1 implements BiConsumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(Object arg0, Object arg1) {
            Collectors.lambda$-java_util_stream_Collectors_27066((ToDoubleFunction) this.-$f0, (double[]) arg0, arg1);
        }

        private final /* synthetic */ void $m$1(Object arg0, Object arg1) {
            Collectors.lambda$-java_util_stream_Collectors_24417((ToIntFunction) this.-$f0, (long[]) arg0, arg1);
        }

        private final /* synthetic */ void $m$2(Object arg0, Object arg1) {
            Collectors.lambda$-java_util_stream_Collectors_25213((ToLongFunction) this.-$f0, (long[]) arg0, arg1);
        }

        private final /* synthetic */ void $m$3(Object arg0, Object arg1) {
            ((Object[]) arg0)[0] = ((BinaryOperator) this.-$f0).apply(((Object[]) arg0)[0], arg1);
        }

        private final /* synthetic */ void $m$4(Object arg0, Object arg1) {
            ((DoubleSummaryStatistics) arg0).accept(((ToDoubleFunction) this.-$f0).applyAsDouble(arg1));
        }

        private final /* synthetic */ void $m$5(Object arg0, Object arg1) {
            ((IntSummaryStatistics) arg0).accept(((ToIntFunction) this.-$f0).applyAsInt(arg1));
        }

        private final /* synthetic */ void $m$6(Object arg0, Object arg1) {
            ((LongSummaryStatistics) arg0).accept(((ToLongFunction) this.-$f0).applyAsLong(arg1));
        }

        private final /* synthetic */ void $m$7(Object arg0, Object arg1) {
            Collectors.lambda$-java_util_stream_Collectors_22066((ToDoubleFunction) this.-$f0, (double[]) arg0, arg1);
        }

        private final /* synthetic */ void $m$8(Object arg0, Object arg1) {
            ((int[]) arg0)[0] = ((int[]) arg0)[0] + ((ToIntFunction) this.-$f0).applyAsInt(arg1);
        }

        private final /* synthetic */ void $m$9(Object arg0, Object arg1) {
            ((long[]) arg0)[0] = ((long[]) arg0)[0] + ((ToLongFunction) this.-$f0).applyAsLong(arg1);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
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
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0$2 */
    final /* synthetic */ class AnonymousClass2 implements BiFunction {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ Object $m$0(Object arg0, Object arg1) {
            return ((Function) this.-$f0).apply(arg1);
        }

        private final /* synthetic */ Object $m$1(Object arg0, Object arg1) {
            return ((Function) this.-$f0).apply(arg1);
        }

        public /* synthetic */ AnonymousClass2(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final Object apply(Object obj, Object obj2) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(obj, obj2);
                case (byte) 1:
                    return $m$1(obj, obj2);
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0$3 */
    final /* synthetic */ class AnonymousClass3 implements Function {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ Object $m$0(Object arg0) {
            return Collectors.lambda$-java_util_stream_Collectors_41242((Function) this.-$f0, (Map) arg0);
        }

        private final /* synthetic */ Object $m$1(Object arg0) {
            return Collectors.lambda$-java_util_stream_Collectors_49796((Function) this.-$f0, (ConcurrentMap) arg0);
        }

        private final /* synthetic */ Object $m$2(Object arg0) {
            return ((Supplier) this.-$f0).get();
        }

        private final /* synthetic */ Object $m$3(Object arg0) {
            return ((Supplier) this.-$f0).get();
        }

        private final /* synthetic */ Object $m$4(Object arg0) {
            return ((Supplier) this.-$f0).get();
        }

        private final /* synthetic */ Object $m$5(Object arg0) {
            return new Partition(((Collector) this.-$f0).finisher().apply(((Partition) arg0).forTrue), ((Collector) this.-$f0).finisher().apply(((Partition) arg0).forFalse));
        }

        public /* synthetic */ AnonymousClass3(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final Object apply(Object obj) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(obj);
                case (byte) 1:
                    return $m$1(obj);
                case (byte) 2:
                    return $m$2(obj);
                case (byte) 3:
                    return $m$3(obj);
                case (byte) 4:
                    return $m$4(obj);
                case (byte) 5:
                    return $m$5(obj);
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0$4 */
    final /* synthetic */ class AnonymousClass4 implements BiConsumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(Object arg0, Object arg1) {
            ((BiConsumer) this.-$f0).accept(arg0, ((Function) this.-$f1).apply(arg1));
        }

        private final /* synthetic */ void $m$1(Object arg0, Object arg1) {
            Collectors.lambda$-java_util_stream_Collectors_52253((BiConsumer) this.-$f0, (Predicate) this.-$f1, (Partition) arg0, arg1);
        }

        private final /* synthetic */ void $m$2(Object arg0, Object arg1) {
            ((Object[]) arg0)[0] = ((BinaryOperator) this.-$f0).apply(((Object[]) arg0)[0], ((Function) this.-$f1).apply(arg1));
        }

        public /* synthetic */ AnonymousClass4(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
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
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0$5 */
    final /* synthetic */ class AnonymousClass5 implements BiConsumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0(Object arg0, Object arg1) {
            ((BiConsumer) this.-$f2).accept(((Map) arg0).computeIfAbsent(Objects.requireNonNull(((Function) this.-$f0).apply(arg1), "element cannot be mapped to a null key"), new AnonymousClass3((byte) 2, (Supplier) this.-$f1)), arg1);
        }

        private final /* synthetic */ void $m$1(Object arg0, Object arg1) {
            ((BiConsumer) this.-$f2).accept(((ConcurrentMap) arg0).computeIfAbsent(Objects.requireNonNull(((Function) this.-$f0).apply(arg1), "element cannot be mapped to a null key"), new AnonymousClass3((byte) 3, (Supplier) this.-$f1)), arg1);
        }

        private final /* synthetic */ void $m$2(Object arg0, Object arg1) {
            Collectors.lambda$-java_util_stream_Collectors_49016((Function) this.-$f0, (Supplier) this.-$f1, (BiConsumer) this.-$f2, (ConcurrentMap) arg0, arg1);
        }

        private final /* synthetic */ void $m$3(Object arg0, Object arg1) {
            ((ConcurrentMap) arg0).merge(((Function) this.-$f0).apply(arg1), ((Function) this.-$f1).apply(arg1), (BinaryOperator) this.-$f2);
        }

        private final /* synthetic */ void $m$4(Object arg0, Object arg1) {
            ((Map) arg0).merge(((Function) this.-$f0).apply(arg1), ((Function) this.-$f1).apply(arg1), (BinaryOperator) this.-$f2);
        }

        public /* synthetic */ AnonymousClass5(byte b, Object obj, Object obj2, Object obj3) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
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
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0$6 */
    final /* synthetic */ class AnonymousClass6 implements Supplier {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ Object $m$0() {
            return new StringJoiner((CharSequence) this.-$f0, (CharSequence) this.-$f1, (CharSequence) this.-$f2);
        }

        public /* synthetic */ AnonymousClass6(Object obj, Object obj2, Object obj3) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
        }

        public final Object get() {
            return $m$0();
        }
    }

    private final /* synthetic */ Object $m$0(Object arg0) {
        return Collectors.lambda$-java_util_stream_Collectors_27314((double[]) arg0);
    }

    private final /* synthetic */ Object $m$1(Object arg0) {
        return Collectors.lambda$-java_util_stream_Collectors_24555((long[]) arg0);
    }

    private final /* synthetic */ Object $m$10(Object arg0) {
        return Double.valueOf(Collectors.computeFinalSum((double[]) arg0));
    }

    private final /* synthetic */ Object $m$11(Object arg0) {
        return Integer.valueOf(((int[]) arg0)[0]);
    }

    private final /* synthetic */ Object $m$12(Object arg0) {
        return Long.valueOf(((long[]) arg0)[0]);
    }

    private final /* synthetic */ Object $m$2(Object arg0) {
        return Collectors.lambda$-java_util_stream_Collectors_25352((long[]) arg0);
    }

    private final /* synthetic */ Object $m$3(Object arg0) {
        return Collectors.lambda$-java_util_stream_Collectors_6048(arg0);
    }

    private final /* synthetic */ Object $m$4(Object arg0) {
        return Long.valueOf(1);
    }

    private final /* synthetic */ Object $m$5(Object arg0) {
        return ((StringBuilder) arg0).toString();
    }

    private final /* synthetic */ Object $m$6(Object arg0) {
        return ((StringJoiner) arg0).toString();
    }

    private final /* synthetic */ Object $m$7(Object arg0) {
        return ((Object[]) arg0)[0];
    }

    private final /* synthetic */ Object $m$8(Object arg0) {
        return ((Object[]) arg0)[0];
    }

    private final /* synthetic */ Object $m$9(Object arg0) {
        return Optional.ofNullable(((AnonymousClass1OptionalBox) arg0).value);
    }

    private /* synthetic */ -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0(byte b) {
        this.$id = b;
    }

    public final Object apply(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj);
            case (byte) 1:
                return $m$1(obj);
            case (byte) 2:
                return $m$2(obj);
            case (byte) 3:
                return $m$3(obj);
            case (byte) 4:
                return $m$4(obj);
            case (byte) 5:
                return $m$5(obj);
            case (byte) 6:
                return $m$6(obj);
            case (byte) 7:
                return $m$7(obj);
            case (byte) 8:
                return $m$8(obj);
            case (byte) 9:
                return $m$9(obj);
            case (byte) 10:
                return $m$10(obj);
            case (byte) 11:
                return $m$11(obj);
            case (byte) 12:
                return $m$12(obj);
            default:
                throw new AssertionError();
        }
    }
}
