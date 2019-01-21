package java.util.stream;

import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterator.OfLong;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.Node.Builder;
import java.util.stream.Sink.ChainedLong;

public abstract class LongPipeline<E_IN> extends AbstractPipeline<E_IN, Long, LongStream> implements LongStream {

    public static class Head<E_IN> extends LongPipeline<E_IN> {
        public /* bridge */ /* synthetic */ LongStream parallel() {
            return (LongStream) super.parallel();
        }

        public /* bridge */ /* synthetic */ LongStream sequential() {
            return (LongStream) super.sequential();
        }

        public Head(Supplier<? extends Spliterator<Long>> source, int sourceFlags, boolean parallel) {
            super((Supplier) source, sourceFlags, parallel);
        }

        public Head(Spliterator<Long> source, int sourceFlags, boolean parallel) {
            super((Spliterator) source, sourceFlags, parallel);
        }

        public final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        public final Sink<E_IN> opWrapSink(int flags, Sink<Long> sink) {
            throw new UnsupportedOperationException();
        }

        public void forEach(LongConsumer action) {
            if (isParallel()) {
                super.forEach(action);
            } else {
                LongPipeline.adapt(sourceStageSpliterator()).forEachRemaining(action);
            }
        }

        public void forEachOrdered(LongConsumer action) {
            if (isParallel()) {
                super.forEachOrdered(action);
            } else {
                LongPipeline.adapt(sourceStageSpliterator()).forEachRemaining(action);
            }
        }
    }

    public static abstract class StatefulOp<E_IN> extends LongPipeline<E_IN> {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        public abstract <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Long[]> intFunction);

        static {
            Class cls = LongPipeline.class;
        }

        public /* bridge */ /* synthetic */ LongStream parallel() {
            return (LongStream) super.parallel();
        }

        public /* bridge */ /* synthetic */ LongStream sequential() {
            return (LongStream) super.sequential();
        }

        public StatefulOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
        }

        public final boolean opIsStateful() {
            return true;
        }
    }

    public static abstract class StatelessOp<E_IN> extends LongPipeline<E_IN> {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = LongPipeline.class;
        }

        public /* bridge */ /* synthetic */ LongStream parallel() {
            return (LongStream) super.parallel();
        }

        public /* bridge */ /* synthetic */ LongStream sequential() {
            return (LongStream) super.sequential();
        }

        public StatelessOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
        }

        public final boolean opIsStateful() {
            return false;
        }
    }

    public /* bridge */ /* synthetic */ LongStream parallel() {
        return (LongStream) super.parallel();
    }

    public /* bridge */ /* synthetic */ LongStream sequential() {
        return (LongStream) super.sequential();
    }

    LongPipeline(Supplier<? extends Spliterator<Long>> source, int sourceFlags, boolean parallel) {
        super((Supplier) source, sourceFlags, parallel);
    }

    LongPipeline(Spliterator<Long> source, int sourceFlags, boolean parallel) {
        super((Spliterator) source, sourceFlags, parallel);
    }

    LongPipeline(AbstractPipeline<?, E_IN, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }

    private static LongConsumer adapt(Sink<Long> sink) {
        if (sink instanceof LongConsumer) {
            return (LongConsumer) sink;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using LongStream.adapt(Sink<Long> s)");
        }
        Objects.requireNonNull(sink);
        return new -$$Lambda$zQ-9PoG-PFOA3MjNNbaERnRB6ik(sink);
    }

    private static OfLong adapt(Spliterator<Long> s) {
        if (s instanceof OfLong) {
            return (OfLong) s;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using LongStream.adapt(Spliterator<Long> s)");
        }
        throw new UnsupportedOperationException("LongStream.adapt(Spliterator<Long> s)");
    }

    public final StreamShape getOutputShape() {
        return StreamShape.LONG_VALUE;
    }

    public final <P_IN> Node<Long> evaluateToNode(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<Long[]> intFunction) {
        return Nodes.collectLong(helper, spliterator, flattenTree);
    }

    public final <P_IN> Spliterator<Long> wrap(PipelineHelper<Long> ph, Supplier<Spliterator<P_IN>> supplier, boolean isParallel) {
        return new LongWrappingSpliterator((PipelineHelper) ph, (Supplier) supplier, isParallel);
    }

    public final OfLong lazySpliterator(Supplier<? extends Spliterator<Long>> supplier) {
        return new OfLong(supplier);
    }

    public final void forEachWithCancel(Spliterator<Long> spliterator, Sink<Long> sink) {
        OfLong spl = adapt((Spliterator) spliterator);
        LongConsumer adaptedSink = adapt((Sink) sink);
        while (!sink.cancellationRequested()) {
            if (!spl.tryAdvance(adaptedSink)) {
                return;
            }
        }
    }

    public final Builder<Long> makeNodeBuilder(long exactSizeIfKnown, IntFunction<Long[]> intFunction) {
        return Nodes.longBuilder(exactSizeIfKnown);
    }

    public final PrimitiveIterator.OfLong iterator() {
        return Spliterators.iterator(spliterator());
    }

    public final OfLong spliterator() {
        return adapt(super.spliterator());
    }

    public final DoubleStream asDoubleStream() {
        return new java.util.stream.DoublePipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Long> opWrapSink(int flags, Sink<Double> sink) {
                return new ChainedLong<Double>(sink) {
                    public void accept(long t) {
                        this.downstream.accept((double) t);
                    }
                };
            }
        };
    }

    public final Stream<Long> boxed() {
        return mapToObj(-$$Lambda$w4zz3RuWVbX94KiVllUNB6u_ygA.INSTANCE);
    }

    public final LongStream map(LongUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        final LongUnaryOperator longUnaryOperator = mapper;
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return new ChainedLong<Long>(sink) {
                    public void accept(long t) {
                        this.downstream.accept(longUnaryOperator.applyAsLong(t));
                    }
                };
            }
        };
    }

    public final <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        final LongFunction<? extends U> longFunction = mapper;
        return new java.util.stream.ReferencePipeline.StatelessOp<Long, U>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Long> opWrapSink(int flags, Sink<U> sink) {
                return new ChainedLong<U>(sink) {
                    public void accept(long t) {
                        this.downstream.accept(longFunction.apply(t));
                    }
                };
            }
        };
    }

    public final IntStream mapToInt(LongToIntFunction mapper) {
        Objects.requireNonNull(mapper);
        final LongToIntFunction longToIntFunction = mapper;
        return new java.util.stream.IntPipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Long> opWrapSink(int flags, Sink<Integer> sink) {
                return new ChainedLong<Integer>(sink) {
                    public void accept(long t) {
                        this.downstream.accept(longToIntFunction.applyAsInt(t));
                    }
                };
            }
        };
    }

    public final DoubleStream mapToDouble(LongToDoubleFunction mapper) {
        Objects.requireNonNull(mapper);
        final LongToDoubleFunction longToDoubleFunction = mapper;
        return new java.util.stream.DoublePipeline.StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Long> opWrapSink(int flags, Sink<Double> sink) {
                return new ChainedLong<Double>(sink) {
                    public void accept(long t) {
                        this.downstream.accept(longToDoubleFunction.applyAsDouble(t));
                    }
                };
            }
        };
    }

    public final LongStream flatMap(LongFunction<? extends LongStream> mapper) {
        final LongFunction<? extends LongStream> longFunction = mapper;
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, (StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) | StreamOpFlag.NOT_SIZED) {
            public Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return new ChainedLong<Long>(sink) {
                    public void begin(long size) {
                        this.downstream.begin(-1);
                    }

                    /* JADX WARNING: Missing block: B:9:0x001e, code skipped:
            if (r0 != null) goto L_0x0020;
     */
                    /* JADX WARNING: Missing block: B:10:0x0020, code skipped:
            if (r1 != null) goto L_0x0022;
     */
                    /* JADX WARNING: Missing block: B:12:?, code skipped:
            r0.close();
     */
                    /* JADX WARNING: Missing block: B:13:0x0026, code skipped:
            r3 = move-exception;
     */
                    /* JADX WARNING: Missing block: B:14:0x0027, code skipped:
            r1.addSuppressed(r3);
     */
                    /* JADX WARNING: Missing block: B:15:0x002b, code skipped:
            r0.close();
     */
                    /* Code decompiled incorrectly, please refer to instructions dump. */
                    public void accept(long t) {
                        LongStream result = (LongStream) longFunction.apply(t);
                        if (result != null) {
                            result.sequential().forEach(new -$$Lambda$LongPipeline$6$1$fLvJH_Wq0Kv-MEJSFU3IOaEtvxk(this));
                        }
                        if (result != null) {
                            result.close();
                        }
                    }
                };
            }
        };
    }

    public LongStream unordered() {
        if (isOrdered()) {
            return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_ORDERED) {
                public Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                    return sink;
                }
            };
        }
        return this;
    }

    public final LongStream filter(LongPredicate predicate) {
        Objects.requireNonNull(predicate);
        final LongPredicate longPredicate = predicate;
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, StreamOpFlag.NOT_SIZED) {
            public Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return new ChainedLong<Long>(sink) {
                    public void begin(long size) {
                        this.downstream.begin(-1);
                    }

                    public void accept(long t) {
                        if (longPredicate.test(t)) {
                            this.downstream.accept(t);
                        }
                    }
                };
            }
        };
    }

    public final LongStream peek(LongConsumer action) {
        Objects.requireNonNull(action);
        final LongConsumer longConsumer = action;
        return new StatelessOp<Long>(this, StreamShape.LONG_VALUE, 0) {
            public Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                return new ChainedLong<Long>(sink) {
                    public void accept(long t) {
                        longConsumer.accept(t);
                        this.downstream.accept(t);
                    }
                };
            }
        };
    }

    public final LongStream limit(long maxSize) {
        if (maxSize >= 0) {
            return SliceOps.makeLong(this, 0, maxSize);
        }
        throw new IllegalArgumentException(Long.toString(maxSize));
    }

    public final LongStream skip(long n) {
        if (n < 0) {
            throw new IllegalArgumentException(Long.toString(n));
        } else if (n == 0) {
            return this;
        } else {
            return SliceOps.makeLong(this, n, -1);
        }
    }

    public final LongStream sorted() {
        return SortedOps.makeLong(this);
    }

    public final LongStream distinct() {
        return boxed().distinct().mapToLong(-$$Lambda$LongPipeline$doop4YO9hzEFGaLnLB3xKA404M4.INSTANCE);
    }

    public void forEach(LongConsumer action) {
        evaluate(ForEachOps.makeLong(action, false));
    }

    public void forEachOrdered(LongConsumer action) {
        evaluate(ForEachOps.makeLong(action, true));
    }

    public final long sum() {
        return reduce(0, -$$Lambda$dplkPhACWDPIy18ogwdupEQaN40.INSTANCE);
    }

    public final OptionalLong min() {
        return reduce(-$$Lambda$OExyAlU04fvFLvnsXWOUeFS6K6Y.INSTANCE);
    }

    public final OptionalLong max() {
        return reduce(-$$Lambda$6eeAyFpmvaed9kw3uuEs0ErN7sg.INSTANCE);
    }

    public final OptionalDouble average() {
        long[] avg = (long[]) collect(-$$Lambda$LongPipeline$C2qxkG-7ctBwIL2ufjYSA46AbOM.INSTANCE, -$$Lambda$LongPipeline$sfTgyfHS4klE7h4z5M-NXsSIFcQ.INSTANCE, -$$Lambda$LongPipeline$unkecqyY0oPqnMvfYdq_wAGb9pY.INSTANCE);
        if (avg[0] > 0) {
            return OptionalDouble.of(((double) avg[1]) / ((double) avg[0]));
        }
        return OptionalDouble.empty();
    }

    static /* synthetic */ void lambda$average$2(long[] ll, long i) {
        ll[0] = ll[0] + 1;
        ll[1] = ll[1] + i;
    }

    static /* synthetic */ void lambda$average$3(long[] ll, long[] rr) {
        ll[0] = ll[0] + rr[0];
        ll[1] = ll[1] + rr[1];
    }

    public final long count() {
        return map(-$$Lambda$LongPipeline$HjmjwoQcQfPYnTF2E4GrQONBjyM.INSTANCE).sum();
    }

    public final LongSummaryStatistics summaryStatistics() {
        return (LongSummaryStatistics) collect(-$$Lambda$kZuTETptiPwvB1J27Na7j760aLU.INSTANCE, -$$Lambda$Y_fORtDI6zkwP_Z_VGSwO2GcnS0.INSTANCE, -$$Lambda$JNjUhnscc8mcsjlQNaAi4qIfRDQ.INSTANCE);
    }

    public final long reduce(long identity, LongBinaryOperator op) {
        return ((Long) evaluate(ReduceOps.makeLong(identity, op))).longValue();
    }

    public final OptionalLong reduce(LongBinaryOperator op) {
        return (OptionalLong) evaluate(ReduceOps.makeLong(op));
    }

    public final <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return evaluate(ReduceOps.makeLong(supplier, accumulator, new -$$Lambda$LongPipeline$-BxZA1c1Y79VaVw54W8s5K5ji_0(combiner)));
    }

    public final boolean anyMatch(LongPredicate predicate) {
        return ((Boolean) evaluate(MatchOps.makeLong(predicate, MatchKind.ANY))).booleanValue();
    }

    public final boolean allMatch(LongPredicate predicate) {
        return ((Boolean) evaluate(MatchOps.makeLong(predicate, MatchKind.ALL))).booleanValue();
    }

    public final boolean noneMatch(LongPredicate predicate) {
        return ((Boolean) evaluate(MatchOps.makeLong(predicate, MatchKind.NONE))).booleanValue();
    }

    public final OptionalLong findFirst() {
        return (OptionalLong) evaluate(FindOps.makeLong(true));
    }

    public final OptionalLong findAny() {
        return (OptionalLong) evaluate(FindOps.makeLong(false));
    }

    public final long[] toArray() {
        return (long[]) Nodes.flattenLong((Node.OfLong) evaluateToArrayNode(-$$Lambda$LongPipeline$LTFlNC6dzl63DE63FJGC-sG7H_c.INSTANCE)).asPrimitiveArray();
    }
}
