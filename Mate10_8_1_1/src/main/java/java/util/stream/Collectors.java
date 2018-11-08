package java.util.stream;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.AnonymousClass1;
import java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.AnonymousClass2;
import java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.AnonymousClass3;
import java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.AnonymousClass4;
import java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.AnonymousClass5;
import java.util.stream.-$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.AnonymousClass6;
import java.util.stream.Collector.Characteristics;

public final class Collectors {
    static final Set<Characteristics> CH_CONCURRENT_ID = Collections.unmodifiableSet(EnumSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH));
    static final Set<Characteristics> CH_CONCURRENT_NOID = Collections.unmodifiableSet(EnumSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED));
    static final Set<Characteristics> CH_ID = Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
    static final Set<Characteristics> CH_NOID = Collections.emptySet();
    static final Set<Characteristics> CH_UNORDERED_ID = Collections.unmodifiableSet(EnumSet.of(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH));

    /* renamed from: java.util.stream.Collectors$1OptionalBox */
    class AnonymousClass1OptionalBox implements Consumer<T> {
        boolean present = false;
        final /* synthetic */ BinaryOperator val$op;
        T value = null;

        AnonymousClass1OptionalBox(BinaryOperator binaryOperator) {
            this.val$op = binaryOperator;
        }

        public void accept(T t) {
            if (this.present) {
                this.value = this.val$op.apply(this.value, t);
                return;
            }
            this.value = t;
            this.present = true;
        }
    }

    static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
        private final BiConsumer<A, T> accumulator;
        private final Set<Characteristics> characteristics;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Supplier<A> supplier;

        CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Function<A, R> finisher, Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Set<Characteristics> characteristics) {
            this(supplier, accumulator, combiner, Collectors.castingIdentity(), characteristics);
        }

        public BiConsumer<A, T> accumulator() {
            return this.accumulator;
        }

        public Supplier<A> supplier() {
            return this.supplier;
        }

        public BinaryOperator<A> combiner() {
            return this.combiner;
        }

        public Function<A, R> finisher() {
            return this.finisher;
        }

        public Set<Characteristics> characteristics() {
            return this.characteristics;
        }
    }

    private static final class Partition<T> extends AbstractMap<Boolean, T> implements Map<Boolean, T> {
        final T forFalse;
        final T forTrue;

        Partition(T forTrue, T forFalse) {
            this.forTrue = forTrue;
            this.forFalse = forFalse;
        }

        public Set<Entry<Boolean, T>> entrySet() {
            return new AbstractSet<Entry<Boolean, T>>() {
                public Iterator<Entry<Boolean, T>> iterator() {
                    Entry<Boolean, T> falseEntry = new SimpleImmutableEntry(Boolean.valueOf(false), Partition.this.forFalse);
                    Entry<Boolean, T> trueEntry = new SimpleImmutableEntry(Boolean.valueOf(true), Partition.this.forTrue);
                    return Arrays.asList(falseEntry, trueEntry).iterator();
                }

                public int size() {
                    return 2;
                }
            };
        }
    }

    private Collectors() {
    }

    static /* synthetic */ Object lambda$-java_util_stream_Collectors_5845(Object u, Object obj) {
        throw new IllegalStateException(String.format("Duplicate key %s", u));
    }

    private static <T> BinaryOperator<T> throwingMerger() {
        return -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$13;
    }

    private static <I, R> Function<I, R> castingIdentity() {
        return -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$3;
    }

    static /* synthetic */ Object lambda$-java_util_stream_Collectors_6048(Object i) {
        return i;
    }

    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<C> collectionFactory) {
        return new CollectorImpl(collectionFactory, -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU.$INST$3, -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$14, CH_ID);
    }

    public static <T> Collector<T, ?, List<T>> toList() {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$15, -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU.$INST$4, -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$15, CH_ID);
    }

    public static <T> Collector<T, ?, Set<T>> toSet() {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$18, -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU.$INST$5, -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$16, CH_UNORDERED_ID);
    }

    public static Collector<CharSequence, ?, String> joining() {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$6, -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU.$INST$0, -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$4, -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$5, CH_NOID);
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return new CollectorImpl(new AnonymousClass6(delimiter, prefix, suffix), -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU.$INST$1, -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$5, -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$6, CH_NOID);
    }

    private static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(BinaryOperator<V> mergeFunction) {
        return new -$Lambda$s-muF8cTY6kf2DcLR-Ys2NMV7bA((byte) 0, mergeFunction);
    }

    static /* synthetic */ Map lambda$-java_util_stream_Collectors_13304(BinaryOperator mergeFunction, Map m1, Map m2) {
        for (Entry<K, V> e : m2.entrySet()) {
            m1.merge(e.getKey(), e.getValue(), mergeFunction);
        }
        return m1;
    }

    public static <T, U, A, R> Collector<T, ?, R> mapping(Function<? super T, ? extends U> mapper, Collector<? super U, A, R> downstream) {
        return new CollectorImpl(downstream.supplier(), new AnonymousClass4((byte) 0, downstream.accumulator(), mapper), downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream, Function<R, RR> finisher) {
        Set<Characteristics> characteristics;
        Collection characteristics2 = downstream.characteristics();
        if (characteristics2.contains(Characteristics.IDENTITY_FINISH)) {
            if (characteristics2.size() == 1) {
                characteristics = CH_NOID;
            } else {
                characteristics = EnumSet.copyOf(characteristics2);
                characteristics.remove(Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }
        return new CollectorImpl(downstream.supplier(), downstream.accumulator(), downstream.combiner(), downstream.finisher().andThen(finisher), characteristics);
    }

    public static <T> Collector<T, ?, Long> counting() {
        return reducing(Long.valueOf(0), -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$4, -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$3);
    }

    public static <T> Collector<T, ?, Optional<T>> minBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.minBy(comparator));
    }

    public static <T> Collector<T, ?, Optional<T>> maxBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.maxBy(comparator));
    }

    public static <T> Collector<T, ?, Integer> summingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$11, new AnonymousClass1((byte) 8, mapper), -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$11, -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$11, CH_NOID);
    }

    public static <T> Collector<T, ?, Long> summingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$12, new AnonymousClass1((byte) 9, mapper), -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$12, -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$12, CH_NOID);
    }

    public static <T> Collector<T, ?, Double> summingDouble(ToDoubleFunction<? super T> mapper) {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$10, new AnonymousClass1((byte) 7, mapper), -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$10, -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$10, CH_NOID);
    }

    static /* synthetic */ void lambda$-java_util_stream_Collectors_22066(ToDoubleFunction mapper, double[] a, Object t) {
        sumWithCompensation(a, mapper.applyAsDouble(t));
        a[2] = a[2] + mapper.applyAsDouble(t);
    }

    static /* synthetic */ double[] lambda$-java_util_stream_Collectors_22206(double[] a, double[] b) {
        sumWithCompensation(a, b[0]);
        a[2] = a[2] + b[2];
        return sumWithCompensation(a, b[1]);
    }

    static double[] sumWithCompensation(double[] intermediateSum, double value) {
        double tmp = value - intermediateSum[1];
        double sum = intermediateSum[0];
        double velvel = sum + tmp;
        intermediateSum[1] = (velvel - sum) - tmp;
        intermediateSum[0] = velvel;
        return intermediateSum;
    }

    static double computeFinalSum(double[] summands) {
        double tmp = summands[0] + summands[1];
        double simpleSum = summands[summands.length - 1];
        if (Double.isNaN(tmp) && Double.isInfinite(simpleSum)) {
            return simpleSum;
        }
        return tmp;
    }

    public static <T> Collector<T, ?, Double> averagingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$1, new AnonymousClass1((byte) 1, mapper), -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$1, -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$1, CH_NOID);
    }

    static /* synthetic */ void lambda$-java_util_stream_Collectors_24417(ToIntFunction mapper, long[] a, Object t) {
        a[0] = a[0] + ((long) mapper.applyAsInt(t));
        a[1] = a[1] + 1;
    }

    static /* synthetic */ long[] lambda$-java_util_stream_Collectors_24486(long[] a, long[] b) {
        a[0] = a[0] + b[0];
        a[1] = a[1] + b[1];
        return a;
    }

    static /* synthetic */ Double lambda$-java_util_stream_Collectors_24555(long[] a) {
        return Double.valueOf(a[1] == 0 ? 0.0d : ((double) a[0]) / ((double) a[1]));
    }

    public static <T> Collector<T, ?, Double> averagingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$2, new AnonymousClass1((byte) 2, mapper), -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$2, -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$2, CH_NOID);
    }

    static /* synthetic */ void lambda$-java_util_stream_Collectors_25213(ToLongFunction mapper, long[] a, Object t) {
        a[0] = a[0] + mapper.applyAsLong(t);
        a[1] = a[1] + 1;
    }

    static /* synthetic */ long[] lambda$-java_util_stream_Collectors_25283(long[] a, long[] b) {
        a[0] = a[0] + b[0];
        a[1] = a[1] + b[1];
        return a;
    }

    static /* synthetic */ Double lambda$-java_util_stream_Collectors_25352(long[] a) {
        return Double.valueOf(a[1] == 0 ? 0.0d : ((double) a[0]) / ((double) a[1]));
    }

    public static <T> Collector<T, ?, Double> averagingDouble(ToDoubleFunction<? super T> mapper) {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$0, new AnonymousClass1((byte) 0, mapper), -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$0, -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$0, CH_NOID);
    }

    static /* synthetic */ void lambda$-java_util_stream_Collectors_27066(ToDoubleFunction mapper, double[] a, Object t) {
        sumWithCompensation(a, mapper.applyAsDouble(t));
        a[2] = a[2] + 1.0d;
        a[3] = a[3] + mapper.applyAsDouble(t);
    }

    static /* synthetic */ double[] lambda$-java_util_stream_Collectors_27185(double[] a, double[] b) {
        sumWithCompensation(a, b[0]);
        sumWithCompensation(a, b[1]);
        a[2] = a[2] + b[2];
        a[3] = a[3] + b[3];
        return a;
    }

    static /* synthetic */ Double lambda$-java_util_stream_Collectors_27314(double[] a) {
        double d = 0.0d;
        if (a[2] != 0.0d) {
            d = computeFinalSum(a) / a[2];
        }
        return Double.valueOf(d);
    }

    public static <T> Collector<T, ?, T> reducing(T identity, BinaryOperator<T> op) {
        return new CollectorImpl(boxSupplier(identity), new AnonymousClass1((byte) 3, op), new -$Lambda$s-muF8cTY6kf2DcLR-Ys2NMV7bA((byte) 2, op), -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$7, CH_NOID);
    }

    private static <T> Supplier<T[]> boxSupplier(T identity) {
        return new -$Lambda$F2FxWS-_sj37qqP78wv60F_s_Zw((byte) 2, identity);
    }

    public static <T> Collector<T, ?, Optional<T>> reducing(BinaryOperator<T> op) {
        return new CollectorImpl(new -$Lambda$F2FxWS-_sj37qqP78wv60F_s_Zw((byte) 4, op), -$Lambda$Y1nWb7oHyESmWtTUR-RlHqf5IfU.$INST$2, -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$6, -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$9, CH_NOID);
    }

    static /* synthetic */ AnonymousClass1OptionalBox lambda$-java_util_stream_Collectors_30747(AnonymousClass1OptionalBox a, AnonymousClass1OptionalBox b) {
        if (b.present) {
            a.accept(b.value);
        }
        return a;
    }

    public static <T, U> Collector<T, ?, U> reducing(U identity, Function<? super T, ? extends U> mapper, BinaryOperator<U> op) {
        return new CollectorImpl(boxSupplier(identity), new AnonymousClass4((byte) 2, op, mapper), new -$Lambda$s-muF8cTY6kf2DcLR-Ys2NMV7bA((byte) 3, op), -$Lambda$qTstLJg88fs2C3g6LH-R51vCVP0.$INST$8, CH_NOID);
    }

    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> classifier) {
        return groupingBy(classifier, toList());
    }

    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return groupingBy(classifier, -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$3, downstream);
    }

    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
        BiConsumer<Map<K, A>, T> accumulator = new AnonymousClass5((byte) 0, classifier, downstream.supplier(), downstream.accumulator());
        BinaryOperator<Map<K, A>> merger = mapMerger(downstream.combiner());
        Supplier<Map<K, A>> mangledFactory = mapFactory;
        if (downstream.characteristics().contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl(mapFactory, accumulator, merger, CH_ID);
        }
        return new CollectorImpl(mapFactory, accumulator, merger, new AnonymousClass3((byte) 0, downstream.finisher()), CH_NOID);
    }

    static /* synthetic */ Map lambda$-java_util_stream_Collectors_41242(Function downstreamFinisher, Map intermediate) {
        intermediate.replaceAll(new AnonymousClass2((byte) 0, downstreamFinisher));
        M castResult = intermediate;
        return intermediate;
    }

    public static <T, K> Collector<T, ?, ConcurrentMap<K, List<T>>> groupingByConcurrent(Function<? super T, ? extends K> classifier) {
        return groupingByConcurrent(classifier, -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$4, toList());
    }

    public static <T, K, A, D> Collector<T, ?, ConcurrentMap<K, D>> groupingByConcurrent(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return groupingByConcurrent(classifier, -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$5, downstream);
    }

    public static <T, K, A, D, M extends ConcurrentMap<K, D>> Collector<T, ?, M> groupingByConcurrent(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
        BiConsumer<ConcurrentMap<K, A>, T> accumulator;
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BinaryOperator<ConcurrentMap<K, A>> merger = mapMerger(downstream.combiner());
        Supplier<ConcurrentMap<K, A>> mangledFactory = mapFactory;
        if (downstream.characteristics().contains(Characteristics.CONCURRENT)) {
            accumulator = new AnonymousClass5((byte) 1, classifier, downstreamSupplier, downstreamAccumulator);
        } else {
            accumulator = new AnonymousClass5((byte) 2, classifier, downstreamSupplier, downstreamAccumulator);
        }
        if (downstream.characteristics().contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl(mapFactory, accumulator, merger, CH_CONCURRENT_ID);
        }
        return new CollectorImpl(mapFactory, accumulator, merger, new AnonymousClass3((byte) 1, downstream.finisher()), CH_CONCURRENT_NOID);
    }

    static /* synthetic */ void lambda$-java_util_stream_Collectors_49016(Function classifier, Supplier downstreamSupplier, BiConsumer downstreamAccumulator, ConcurrentMap m, Object t) {
        A resultContainer = m.computeIfAbsent(Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key"), new AnonymousClass3((byte) 4, downstreamSupplier));
        synchronized (resultContainer) {
            downstreamAccumulator.accept(resultContainer, t);
        }
    }

    static /* synthetic */ ConcurrentMap lambda$-java_util_stream_Collectors_49796(Function downstreamFinisher, ConcurrentMap intermediate) {
        intermediate.replaceAll(new AnonymousClass2((byte) 1, downstreamFinisher));
        M castResult = intermediate;
        return intermediate;
    }

    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<? super T> predicate) {
        return partitioningBy(predicate, toList());
    }

    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(Predicate<? super T> predicate, Collector<? super T, A, D> downstream) {
        BiConsumer<Partition<A>, T> accumulator = new AnonymousClass4((byte) 1, downstream.accumulator(), predicate);
        BinaryOperator<Partition<A>> merger = new -$Lambda$s-muF8cTY6kf2DcLR-Ys2NMV7bA((byte) 1, downstream.combiner());
        Supplier<Partition<A>> supplier = new -$Lambda$F2FxWS-_sj37qqP78wv60F_s_Zw((byte) 3, downstream);
        if (downstream.characteristics().contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl(supplier, accumulator, merger, CH_ID);
        }
        return new CollectorImpl(supplier, accumulator, merger, new AnonymousClass3((byte) 5, downstream), CH_NOID);
    }

    static /* synthetic */ void lambda$-java_util_stream_Collectors_52253(BiConsumer downstreamAccumulator, Predicate predicate, Partition result, Object t) {
        downstreamAccumulator.accept(predicate.test(t) ? result.forTrue : result.forFalse, t);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return toMap(keyMapper, valueMapper, throwingMerger(), -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$16);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$17);
    }

    public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction, Supplier<M> mapSupplier) {
        return new CollectorImpl(mapSupplier, new AnonymousClass5((byte) 4, keyMapper, valueMapper, mergeFunction), mapMerger(mergeFunction), CH_ID);
    }

    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return toConcurrentMap(keyMapper, valueMapper, throwingMerger(), -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$13);
    }

    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$14);
    }

    public static <T, K, U, M extends ConcurrentMap<K, U>> Collector<T, ?, M> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction, Supplier<M> mapSupplier) {
        return new CollectorImpl(mapSupplier, new AnonymousClass5((byte) 3, keyMapper, valueMapper, mergeFunction), mapMerger(mergeFunction), CH_CONCURRENT_ID);
    }

    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$8, new AnonymousClass1((byte) 5, mapper), -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$8, CH_ID);
    }

    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$9, new AnonymousClass1((byte) 6, mapper), -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$9, CH_ID);
    }

    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(ToDoubleFunction<? super T> mapper) {
        return new CollectorImpl(-$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8.$INST$7, new AnonymousClass1((byte) 4, mapper), -$Lambda$RbwyEXxBvlOKovrx1LFomyWq0zc.$INST$7, CH_ID);
    }
}
