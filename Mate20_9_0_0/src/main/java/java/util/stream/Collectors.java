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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector.Characteristics;

public final class Collectors {
    static final Set<Characteristics> CH_CONCURRENT_ID = Collections.unmodifiableSet(EnumSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH));
    static final Set<Characteristics> CH_CONCURRENT_NOID = Collections.unmodifiableSet(EnumSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED));
    static final Set<Characteristics> CH_ID = Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
    static final Set<Characteristics> CH_NOID = Collections.emptySet();
    static final Set<Characteristics> CH_UNORDERED_ID = Collections.unmodifiableSet(EnumSet.of(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH));

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

    static /* synthetic */ Object lambda$throwingMerger$0(Object u, Object v) {
        throw new IllegalStateException(String.format("Duplicate key %s", u));
    }

    private static <T> BinaryOperator<T> throwingMerger() {
        return -$$Lambda$Collectors$kXZFmh6iM6xf9lJWimhd2Ef6NEs.INSTANCE;
    }

    private static <I, R> Function<I, R> castingIdentity() {
        return -$$Lambda$Collectors$f0IPpRuyw9HZC8FIP30mNjUUUhw.INSTANCE;
    }

    static /* synthetic */ Object lambda$castingIdentity$1(Object i) {
        return i;
    }

    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<C> collectionFactory) {
        return new CollectorImpl(collectionFactory, -$$Lambda$sKPaOkcQePGTRevrwmKVVhCTmTo.INSTANCE, -$$Lambda$Collectors$iab6bVO3ydceISRiUEq_MRHYzoU.INSTANCE, CH_ID);
    }

    public static <T> Collector<T, ?, List<T>> toList() {
        return new CollectorImpl(-$$Lambda$yTqQxkqu88ZhKI6fWaTTLwOLF60.INSTANCE, -$$Lambda$ihOtgw0eLCrsEBOphyN7SwoAlDg.INSTANCE, -$$Lambda$Collectors$0y_EMl863H_U7B4kxyGscB4vAag.INSTANCE, CH_ID);
    }

    public static <T> Collector<T, ?, Set<T>> toSet() {
        return new CollectorImpl(-$$Lambda$r-8H_R_mZJjp9wd0XTLoEAHMNQ0.INSTANCE, -$$Lambda$uJ6CkL42Bk73jN5EzP0Fx7o1eVA.INSTANCE, -$$Lambda$Collectors$SMVdf7W0ks2OOmS3zJw7DHc-Nhc.INSTANCE, CH_UNORDERED_ID);
    }

    public static Collector<CharSequence, ?, String> joining() {
        return new CollectorImpl(-$$Lambda$cfwqIEDg0Z3A7MGDNrQPTyjrF9M.INSTANCE, -$$Lambda$o8baRh54JSyOHAKgObeucNn1Zos.INSTANCE, -$$Lambda$Collectors$Fu6GEjokQxdzbR0jNzU39-PLUqs.INSTANCE, -$$Lambda$02PZAQlwu7SKkigJ7EI4kdTzqnI.INSTANCE, CH_NOID);
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return joining(delimiter, "", "");
    }

    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return new CollectorImpl(new -$$Lambda$Collectors$pzPeDl3rCgtNVSeZPHZk5f2se60(delimiter, prefix, suffix), -$$Lambda$Drw47GGUtPrz9CklhlT0v26u-5c.INSTANCE, -$$Lambda$i0Jl5dMkfWphZviqg6QdkkWPWRI.INSTANCE, -$$Lambda$okJigbB9kSn__oCZ5Do9uFNyF6A.INSTANCE, CH_NOID);
    }

    private static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(BinaryOperator<V> mergeFunction) {
        return new -$$Lambda$Collectors$TzSZZBK0laNSWMge_uuxANwkkMo(mergeFunction);
    }

    static /* synthetic */ Map lambda$mapMerger$7(BinaryOperator mergeFunction, Map m1, Map m2) {
        for (Entry<K, V> e : m2.entrySet()) {
            m1.merge(e.getKey(), e.getValue(), mergeFunction);
        }
        return m1;
    }

    public static <T, U, A, R> Collector<T, ?, R> mapping(Function<? super T, ? extends U> mapper, Collector<? super U, A, R> downstream) {
        return new CollectorImpl(downstream.supplier(), new -$$Lambda$Collectors$vmLceJDpkkH4HVeqPcL08DnO8yg(downstream.accumulator(), mapper), downstream.combiner(), downstream.finisher(), downstream.characteristics());
    }

    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream, Function<R, RR> finisher) {
        Set<Characteristics> characteristics = downstream.characteristics();
        if (characteristics.contains(Characteristics.IDENTITY_FINISH)) {
            if (characteristics.size() == 1) {
                characteristics = CH_NOID;
            } else {
                characteristics = EnumSet.copyOf((Collection) characteristics);
                characteristics.remove(Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
            }
        }
        return new CollectorImpl(downstream.supplier(), downstream.accumulator(), downstream.combiner(), downstream.finisher().andThen(finisher), characteristics);
    }

    public static <T> Collector<T, ?, Long> counting() {
        return reducing(Long.valueOf(0), -$$Lambda$Collectors$QJ1yoZI6qBLk2NH-gWkdN8XN8yQ.INSTANCE, -$$Lambda$R8aE88Z140TFfTli76Hdc3YzhU4.INSTANCE);
    }

    public static <T> Collector<T, ?, Optional<T>> minBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.minBy(comparator));
    }

    public static <T> Collector<T, ?, Optional<T>> maxBy(Comparator<? super T> comparator) {
        return reducing(BinaryOperator.maxBy(comparator));
    }

    public static <T> Collector<T, ?, Integer> summingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl(-$$Lambda$Collectors$LdN_EEy4nb6271v6ZFmttzZvNwg.INSTANCE, new -$$Lambda$Collectors$PkbZGUBauY6-u1ZrRakcFQjTln0(mapper), -$$Lambda$Collectors$_Q0YP_0ibAqPtQetwby3vyMbjys.INSTANCE, -$$Lambda$Collectors$dk4em1jXH8zNd7gZJwP7T1HgMsc.INSTANCE, CH_NOID);
    }

    public static <T> Collector<T, ?, Long> summingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl(-$$Lambda$Collectors$l57H1aRUHKWOcUoknXahrBTCBzM.INSTANCE, new -$$Lambda$Collectors$jDXjrt99im0xRBROpkCAqoLkqR4(mapper), -$$Lambda$Collectors$efY8VAGaEUr4IwuGz_Jkpfl5lH8.INSTANCE, -$$Lambda$Collectors$P--H40qXgNc8jDetutxHAPBu2pw.INSTANCE, CH_NOID);
    }

    public static <T> Collector<T, ?, Double> summingDouble(ToDoubleFunction<? super T> mapper) {
        return new CollectorImpl(-$$Lambda$Collectors$8qmKppCC_QJwGcQ9bUPPu3l-5Bg.INSTANCE, new -$$Lambda$Collectors$zuotCFMPpEd_pFOpcqCzvWNwmcE(mapper), -$$Lambda$Collectors$rc3oE9Q2XtMlukYSsVA6bDVPVLY.INSTANCE, -$$Lambda$Collectors$cFIm0q5Hl8z7ocxLjJsCwiKceEI.INSTANCE, CH_NOID);
    }

    static /* synthetic */ void lambda$summingDouble$19(ToDoubleFunction mapper, double[] a, Object t) {
        sumWithCompensation(a, mapper.applyAsDouble(t));
        a[2] = a[2] + mapper.applyAsDouble(t);
    }

    static /* synthetic */ double[] lambda$summingDouble$20(double[] a, double[] b) {
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
        return new CollectorImpl(-$$Lambda$Collectors$WTLZnkfYyw3lURYA1kkAz43vGhw.INSTANCE, new -$$Lambda$Collectors$5sBFPk12YoFTd83smSoPj46DB_A(mapper), -$$Lambda$Collectors$ZKsdGxiJ4anQHtwhYmSwD0W3jAc.INSTANCE, -$$Lambda$Collectors$8wf84PB5r3lvvid-SVfjAcFbHXg.INSTANCE, CH_NOID);
    }

    static /* synthetic */ void lambda$averagingInt$23(ToIntFunction mapper, long[] a, Object t) {
        a[0] = a[0] + ((long) mapper.applyAsInt(t));
        a[1] = a[1] + 1;
    }

    static /* synthetic */ long[] lambda$averagingInt$24(long[] a, long[] b) {
        a[0] = a[0] + b[0];
        a[1] = a[1] + b[1];
        return a;
    }

    static /* synthetic */ Double lambda$averagingInt$25(long[] a) {
        return Double.valueOf(a[1] == 0 ? 0.0d : ((double) a[0]) / ((double) a[1]));
    }

    public static <T> Collector<T, ?, Double> averagingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl(-$$Lambda$Collectors$bqboixg1w_Miq-JsFAc1-DEO_aI.INSTANCE, new -$$Lambda$Collectors$AI86aRAtMJ_TFLEAd-UERhgg-kI(mapper), -$$Lambda$Collectors$jubWCCHaIIJ_0UWKVLWqWh9ZDdk.INSTANCE, -$$Lambda$Collectors$J_JAZ0tPN2SF0mqc6rtXc_aK03k.INSTANCE, CH_NOID);
    }

    static /* synthetic */ void lambda$averagingLong$27(ToLongFunction mapper, long[] a, Object t) {
        a[0] = a[0] + mapper.applyAsLong(t);
        a[1] = a[1] + 1;
    }

    static /* synthetic */ long[] lambda$averagingLong$28(long[] a, long[] b) {
        a[0] = a[0] + b[0];
        a[1] = a[1] + b[1];
        return a;
    }

    static /* synthetic */ Double lambda$averagingLong$29(long[] a) {
        return Double.valueOf(a[1] == 0 ? 0.0d : ((double) a[0]) / ((double) a[1]));
    }

    public static <T> Collector<T, ?, Double> averagingDouble(ToDoubleFunction<? super T> mapper) {
        return new CollectorImpl(-$$Lambda$Collectors$bL3PuvFDr85ZCgwmUxFPHSRIrgE.INSTANCE, new -$$Lambda$Collectors$kPvxljbEEMQktNHySoxXCZ-5cu4(mapper), -$$Lambda$Collectors$KLie-yPxAsuZwFd2lh-9gJpGlzE.INSTANCE, -$$Lambda$Collectors$xmuLwoAlFtOfbPB5C1jdMenPWHE.INSTANCE, CH_NOID);
    }

    static /* synthetic */ void lambda$averagingDouble$31(ToDoubleFunction mapper, double[] a, Object t) {
        sumWithCompensation(a, mapper.applyAsDouble(t));
        a[2] = a[2] + 1.0d;
        a[3] = a[3] + mapper.applyAsDouble(t);
    }

    static /* synthetic */ double[] lambda$averagingDouble$32(double[] a, double[] b) {
        sumWithCompensation(a, b[0]);
        sumWithCompensation(a, b[1]);
        a[2] = a[2] + b[2];
        a[3] = a[3] + b[3];
        return a;
    }

    static /* synthetic */ Double lambda$averagingDouble$33(double[] a) {
        double d = 0.0d;
        if (a[2] != 0.0d) {
            d = computeFinalSum(a) / a[2];
        }
        return Double.valueOf(d);
    }

    public static <T> Collector<T, ?, T> reducing(T identity, BinaryOperator<T> op) {
        return new CollectorImpl(boxSupplier(identity), new -$$Lambda$Collectors$hPhWWwdsY4xpBbhcwNVuU8dcJDw(op), new -$$Lambda$Collectors$ad8ZNOGiSpsXDY-HtABmtE9E1UA(op), -$$Lambda$Collectors$il2gZbqqrAa81yW3okSBWeJddGk.INSTANCE, CH_NOID);
    }

    private static <T> Supplier<T[]> boxSupplier(T identity) {
        return new -$$Lambda$Collectors$EsYSkkQ7p405z0qsCjjJsUR3Wfk(identity);
    }

    public static <T> Collector<T, ?, Optional<T>> reducing(BinaryOperator<T> op) {
        return new CollectorImpl(new -$$Lambda$Collectors$BwzHl6O1mjAgxLE58ctIeFoVBAM(op), -$$Lambda$S-3JQ3-ufQOqvkvYGc3Sza-87R0.INSTANCE, -$$Lambda$Collectors$o_hw0Qu0KMLozTwiY9jr8AKG0_0.INSTANCE, -$$Lambda$Collectors$o91A-j5OVnHv9w5Hj_r9fFUMiDY.INSTANCE, CH_NOID);
    }

    static /* synthetic */ AnonymousClass1OptionalBox lambda$reducing$39(AnonymousClass1OptionalBox a, AnonymousClass1OptionalBox b) {
        if (b.present) {
            a.accept(b.value);
        }
        return a;
    }

    public static <T, U> Collector<T, ?, U> reducing(U identity, Function<? super T, ? extends U> mapper, BinaryOperator<U> op) {
        return new CollectorImpl(boxSupplier(identity), new -$$Lambda$Collectors$PTd6jsJ7t0481HRFfH8tnGifDqw(op, mapper), new -$$Lambda$Collectors$xWcI8qM0AfOYfxDSjg08D8ztBFg(op), -$$Lambda$Collectors$Nv3GWD5X6YPdKFD8ca_9SG7qCDI.INSTANCE, CH_NOID);
    }

    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> classifier) {
        return groupingBy(classifier, toList());
    }

    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return groupingBy(classifier, -$$Lambda$ry7iWszBr7beYy31SdRxibDyciQ.INSTANCE, downstream);
    }

    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
        BiConsumer<Map<K, A>, T> accumulator = new -$$Lambda$Collectors$F7-we3W7I2plNaGHqh_d2lzmvho(classifier, downstream.supplier(), downstream.accumulator());
        BinaryOperator<Map<K, A>> merger = mapMerger(downstream.combiner());
        Supplier<Map<K, A>> mangledFactory = mapFactory;
        if (downstream.characteristics().contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl(mangledFactory, accumulator, merger, CH_ID);
        }
        return new CollectorImpl(mangledFactory, accumulator, merger, new -$$Lambda$Collectors$xODDB9V1Y7uzjD3f8OY1Ck4IN4k(downstream.finisher()), CH_NOID);
    }

    public static <T, K> Collector<T, ?, ConcurrentMap<K, List<T>>> groupingByConcurrent(Function<? super T, ? extends K> classifier) {
        return groupingByConcurrent(classifier, -$$Lambda$lG52Z65fM3qwbieoOBUupMhmr2E.INSTANCE, toList());
    }

    public static <T, K, A, D> Collector<T, ?, ConcurrentMap<K, D>> groupingByConcurrent(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return groupingByConcurrent(classifier, -$$Lambda$lG52Z65fM3qwbieoOBUupMhmr2E.INSTANCE, downstream);
    }

    public static <T, K, A, D, M extends ConcurrentMap<K, D>> Collector<T, ?, M> groupingByConcurrent(Function<? super T, ? extends K> classifier, Supplier<M> mapFactory, Collector<? super T, A, D> downstream) {
        BiConsumer<ConcurrentMap<K, A>, T> accumulator;
        Function<? super T, ? extends K> function = classifier;
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BinaryOperator<ConcurrentMap<K, A>> merger = mapMerger(downstream.combiner());
        Supplier<ConcurrentMap<K, A>> mangledFactory = mapFactory;
        if (downstream.characteristics().contains(Characteristics.CONCURRENT)) {
            accumulator = new -$$Lambda$Collectors$AfO_bLozmdhHTtbBN7DysDzpfYM(function, downstreamSupplier, downstreamAccumulator);
        } else {
            accumulator = new -$$Lambda$Collectors$TRHsqgEycZfemtQqwivCY4ecHDM(function, downstreamSupplier, downstreamAccumulator);
        }
        BiConsumer<ConcurrentMap<K, A>, T> accumulator2 = accumulator;
        if (downstream.characteristics().contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl(mangledFactory, accumulator2, merger, CH_CONCURRENT_ID);
        }
        return new CollectorImpl(mangledFactory, accumulator2, merger, new -$$Lambda$Collectors$oKi5061mJjDn56eRJcmESyO7x9k(downstream.finisher()), CH_CONCURRENT_NOID);
    }

    static /* synthetic */ void lambda$groupingByConcurrent$51(Function classifier, Supplier downstreamSupplier, BiConsumer downstreamAccumulator, ConcurrentMap m, Object t) {
        A resultContainer = m.computeIfAbsent(Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key"), new -$$Lambda$Collectors$WGmHV8Rrm8a3qqTLJPLQv6fpb8o(downstreamSupplier));
        synchronized (resultContainer) {
            downstreamAccumulator.accept(resultContainer, t);
        }
    }

    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<? super T> predicate) {
        return partitioningBy(predicate, toList());
    }

    public static <T, D, A> Collector<T, ?, Map<Boolean, D>> partitioningBy(Predicate<? super T> predicate, Collector<? super T, A, D> downstream) {
        BiConsumer<Partition<A>, T> accumulator = new -$$Lambda$Collectors$DPpNNyj-GqDgBuGvO0w_46Z3Jl8(downstream.accumulator(), predicate);
        BinaryOperator<Partition<A>> merger = new -$$Lambda$Collectors$GYNZB08upNxJa3yuZVQ1065S1eU(downstream.combiner());
        Supplier<Partition<A>> supplier = new -$$Lambda$Collectors$IFopD1JhOBkpZfx3JTKDGTwaQTo(downstream);
        if (downstream.characteristics().contains(Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl(supplier, accumulator, merger, CH_ID);
        }
        return new CollectorImpl(supplier, accumulator, merger, new -$$Lambda$Collectors$chTZg9lp1htTW8rBDwbte7J-EOo(downstream), CH_NOID);
    }

    static /* synthetic */ void lambda$partitioningBy$54(BiConsumer downstreamAccumulator, Predicate predicate, Partition result, Object t) {
        downstreamAccumulator.accept(predicate.test(t) ? result.forTrue : result.forFalse, t);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return toMap(keyMapper, valueMapper, throwingMerger(), -$$Lambda$ry7iWszBr7beYy31SdRxibDyciQ.INSTANCE);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, -$$Lambda$ry7iWszBr7beYy31SdRxibDyciQ.INSTANCE);
    }

    public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction, Supplier<M> mapSupplier) {
        return new CollectorImpl(mapSupplier, new -$$Lambda$Collectors$nKlT6uFghrTzWO44dlFAJFeRr34(keyMapper, valueMapper, mergeFunction), mapMerger(mergeFunction), CH_ID);
    }

    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        return toConcurrentMap(keyMapper, valueMapper, throwingMerger(), -$$Lambda$lG52Z65fM3qwbieoOBUupMhmr2E.INSTANCE);
    }

    public static <T, K, U> Collector<T, ?, ConcurrentMap<K, U>> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        return toConcurrentMap(keyMapper, valueMapper, mergeFunction, -$$Lambda$lG52Z65fM3qwbieoOBUupMhmr2E.INSTANCE);
    }

    public static <T, K, U, M extends ConcurrentMap<K, U>> Collector<T, ?, M> toConcurrentMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction, Supplier<M> mapSupplier) {
        return new CollectorImpl(mapSupplier, new -$$Lambda$Collectors$ZHtBI7Du2F_qzRSEqDnG6y4R0Lw(keyMapper, valueMapper, mergeFunction), mapMerger(mergeFunction), CH_CONCURRENT_ID);
    }

    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingInt(ToIntFunction<? super T> mapper) {
        return new CollectorImpl(-$$Lambda$_Ea_sNpqZAwihIOCRBaP7hHgWWI.INSTANCE, new -$$Lambda$Collectors$49j2hRW8u6KMoxsVt77YSpMRb1g(mapper), -$$Lambda$Collectors$HtCSWMKsL2vCjP_AudE9j5Li4Q4.INSTANCE, CH_ID);
    }

    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLong(ToLongFunction<? super T> mapper) {
        return new CollectorImpl(-$$Lambda$kZuTETptiPwvB1J27Na7j760aLU.INSTANCE, new -$$Lambda$Collectors$xyNVxvHgGD7IIanzX9Sm9NxmODA(mapper), -$$Lambda$Collectors$GoEBVVc1WwW27RacBqhtFczthrA.INSTANCE, CH_ID);
    }

    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDouble(ToDoubleFunction<? super T> mapper) {
        return new CollectorImpl(-$$Lambda$745FUy7cYwYu7KrMQTYh2DNqh1I.INSTANCE, new -$$Lambda$Collectors$0hO4kVeN9EGHZquOI_a_qZ75htQ(mapper), -$$Lambda$Collectors$oMCfAR-_eVSty8GsYzK5sec1Kag.INSTANCE, CH_ID);
    }
}
