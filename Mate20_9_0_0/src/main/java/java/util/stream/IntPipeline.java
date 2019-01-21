package java.util.stream;

import java.util.IntSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterator.OfInt;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.Node.Builder;
import java.util.stream.Sink.ChainedInt;

public abstract class IntPipeline<E_IN> extends AbstractPipeline<E_IN, Integer, IntStream> implements IntStream {

    public static class Head<E_IN> extends IntPipeline<E_IN> {
        public /* bridge */ /* synthetic */ IntStream parallel() {
            return (IntStream) super.parallel();
        }

        public /* bridge */ /* synthetic */ IntStream sequential() {
            return (IntStream) super.sequential();
        }

        public Head(Supplier<? extends Spliterator<Integer>> source, int sourceFlags, boolean parallel) {
            super((Supplier) source, sourceFlags, parallel);
        }

        public Head(Spliterator<Integer> source, int sourceFlags, boolean parallel) {
            super((Spliterator) source, sourceFlags, parallel);
        }

        public final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        public final Sink<E_IN> opWrapSink(int flags, Sink<Integer> sink) {
            throw new UnsupportedOperationException();
        }

        public void forEach(IntConsumer action) {
            if (isParallel()) {
                super.forEach(action);
            } else {
                IntPipeline.adapt(sourceStageSpliterator()).forEachRemaining(action);
            }
        }

        public void forEachOrdered(IntConsumer action) {
            if (isParallel()) {
                super.forEachOrdered(action);
            } else {
                IntPipeline.adapt(sourceStageSpliterator()).forEachRemaining(action);
            }
        }
    }

    public static abstract class StatefulOp<E_IN> extends IntPipeline<E_IN> {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        public abstract <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Integer[]> intFunction);

        static {
            Class cls = IntPipeline.class;
        }

        public /* bridge */ /* synthetic */ IntStream parallel() {
            return (IntStream) super.parallel();
        }

        public /* bridge */ /* synthetic */ IntStream sequential() {
            return (IntStream) super.sequential();
        }

        public StatefulOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
        }

        public final boolean opIsStateful() {
            return true;
        }
    }

    public static abstract class StatelessOp<E_IN> extends IntPipeline<E_IN> {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = IntPipeline.class;
        }

        public /* bridge */ /* synthetic */ IntStream parallel() {
            return (IntStream) super.parallel();
        }

        public /* bridge */ /* synthetic */ IntStream sequential() {
            return (IntStream) super.sequential();
        }

        public StatelessOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
        }

        public final boolean opIsStateful() {
            return false;
        }
    }

    public /* bridge */ /* synthetic */ IntStream parallel() {
        return (IntStream) super.parallel();
    }

    public /* bridge */ /* synthetic */ IntStream sequential() {
        return (IntStream) super.sequential();
    }

    IntPipeline(Supplier<? extends Spliterator<Integer>> source, int sourceFlags, boolean parallel) {
        super((Supplier) source, sourceFlags, parallel);
    }

    IntPipeline(Spliterator<Integer> source, int sourceFlags, boolean parallel) {
        super((Spliterator) source, sourceFlags, parallel);
    }

    IntPipeline(AbstractPipeline<?, E_IN, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }

    private static IntConsumer adapt(Sink<Integer> sink) {
        if (sink instanceof IntConsumer) {
            return (IntConsumer) sink;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using IntStream.adapt(Sink<Integer> s)");
        }
        Objects.requireNonNull(sink);
        return new -$$Lambda$wDsxx48ovPSGeNEb3P6H9u7YX0k(sink);
    }

    private static OfInt adapt(Spliterator<Integer> s) {
        if (s instanceof OfInt) {
            return (OfInt) s;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using IntStream.adapt(Spliterator<Integer> s)");
        }
        throw new UnsupportedOperationException("IntStream.adapt(Spliterator<Integer> s)");
    }

    public final StreamShape getOutputShape() {
        return StreamShape.INT_VALUE;
    }

    public final <P_IN> Node<Integer> evaluateToNode(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<Integer[]> intFunction) {
        return Nodes.collectInt(helper, spliterator, flattenTree);
    }

    public final <P_IN> Spliterator<Integer> wrap(PipelineHelper<Integer> ph, Supplier<Spliterator<P_IN>> supplier, boolean isParallel) {
        return new IntWrappingSpliterator((PipelineHelper) ph, (Supplier) supplier, isParallel);
    }

    public final OfInt lazySpliterator(Supplier<? extends Spliterator<Integer>> supplier) {
        return new OfInt(supplier);
    }

    public final void forEachWithCancel(Spliterator<Integer> spliterator, Sink<Integer> sink) {
        OfInt spl = adapt((Spliterator) spliterator);
        IntConsumer adaptedSink = adapt((Sink) sink);
        while (!sink.cancellationRequested()) {
            if (!spl.tryAdvance(adaptedSink)) {
                return;
            }
        }
    }

    public final Builder<Integer> makeNodeBuilder(long exactSizeIfKnown, IntFunction<Integer[]> intFunction) {
        return Nodes.intBuilder(exactSizeIfKnown);
    }

    public final PrimitiveIterator.OfInt iterator() {
        return Spliterators.iterator(spliterator());
    }

    public final OfInt spliterator() {
        return adapt(super.spliterator());
    }

    public final LongStream asLongStream() {
        return new java.util.stream.LongPipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Integer> opWrapSink(int flags, Sink<Long> sink) {
                return new ChainedInt<Long>(sink) {
                    public void accept(int t) {
                        this.downstream.accept((long) t);
                    }
                };
            }
        };
    }

    public final DoubleStream asDoubleStream() {
        return new java.util.stream.DoublePipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Integer> opWrapSink(int flags, Sink<Double> sink) {
                return new ChainedInt<Double>(sink) {
                    public void accept(int t) {
                        this.downstream.accept((double) t);
                    }
                };
            }
        };
    }

    public final Stream<Integer> boxed() {
        return mapToObj(-$$Lambda$wFoiz-RiPqYBPe0X4aSzbj2iL3g.INSTANCE);
    }

    public final IntStream map(IntUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        final IntUnaryOperator intUnaryOperator = mapper;
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new ChainedInt<Integer>(sink) {
                    public void accept(int t) {
                        this.downstream.accept(intUnaryOperator.applyAsInt(t));
                    }
                };
            }
        };
    }

    public final <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        final IntFunction<? extends U> intFunction = mapper;
        return new java.util.stream.ReferencePipeline.StatelessOp<Integer, U>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Integer> opWrapSink(int flags, Sink<U> sink) {
                return new ChainedInt<U>(sink) {
                    public void accept(int t) {
                        this.downstream.accept(intFunction.apply(t));
                    }
                };
            }
        };
    }

    public final LongStream mapToLong(IntToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        final IntToLongFunction intToLongFunction = mapper;
        return new java.util.stream.LongPipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Integer> opWrapSink(int flags, Sink<Long> sink) {
                return new ChainedInt<Long>(sink) {
                    public void accept(int t) {
                        this.downstream.accept(intToLongFunction.applyAsLong(t));
                    }
                };
            }
        };
    }

    public final DoubleStream mapToDouble(IntToDoubleFunction mapper) {
        Objects.requireNonNull(mapper);
        final IntToDoubleFunction intToDoubleFunction = mapper;
        return new java.util.stream.DoublePipeline.StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Integer> opWrapSink(int flags, Sink<Double> sink) {
                return new ChainedInt<Double>(sink) {
                    public void accept(int t) {
                        this.downstream.accept(intToDoubleFunction.applyAsDouble(t));
                    }
                };
            }
        };
    }

    public final IntStream flatMap(IntFunction<? extends IntStream> mapper) {
        final IntFunction<? extends IntStream> intFunction = mapper;
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, (StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) | StreamOpFlag.NOT_SIZED) {
            public Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new ChainedInt<Integer>(sink) {
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
                    public void accept(int t) {
                        IntStream result = (IntStream) intFunction.apply(t);
                        if (result != null) {
                            result.sequential().forEach(new -$$Lambda$IntPipeline$7$1$E2wwNE1UnVxs0E9-n47lRWmnJGM(this));
                        }
                        if (result != null) {
                            result.close();
                        }
                    }
                };
            }
        };
    }

    public IntStream unordered() {
        if (isOrdered()) {
            return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_ORDERED) {
                public Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                    return sink;
                }
            };
        }
        return this;
    }

    public final IntStream filter(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        final IntPredicate intPredicate = predicate;
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, StreamOpFlag.NOT_SIZED) {
            public Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new ChainedInt<Integer>(sink) {
                    public void begin(long size) {
                        this.downstream.begin(-1);
                    }

                    public void accept(int t) {
                        if (intPredicate.test(t)) {
                            this.downstream.accept(t);
                        }
                    }
                };
            }
        };
    }

    public final IntStream peek(IntConsumer action) {
        Objects.requireNonNull(action);
        final IntConsumer intConsumer = action;
        return new StatelessOp<Integer>(this, StreamShape.INT_VALUE, 0) {
            public Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                return new ChainedInt<Integer>(sink) {
                    public void accept(int t) {
                        intConsumer.accept(t);
                        this.downstream.accept(t);
                    }
                };
            }
        };
    }

    public final IntStream limit(long maxSize) {
        if (maxSize >= 0) {
            return SliceOps.makeInt(this, 0, maxSize);
        }
        throw new IllegalArgumentException(Long.toString(maxSize));
    }

    public final IntStream skip(long n) {
        if (n < 0) {
            throw new IllegalArgumentException(Long.toString(n));
        } else if (n == 0) {
            return this;
        } else {
            return SliceOps.makeInt(this, n, -1);
        }
    }

    public final IntStream sorted() {
        return SortedOps.makeInt(this);
    }

    public final IntStream distinct() {
        return boxed().distinct().mapToInt(-$$Lambda$IntPipeline$R-E7oGjPWog3HR9X-8MdhU1ZGRE.INSTANCE);
    }

    public void forEach(IntConsumer action) {
        evaluate(ForEachOps.makeInt(action, false));
    }

    public void forEachOrdered(IntConsumer action) {
        evaluate(ForEachOps.makeInt(action, true));
    }

    public final int sum() {
        return reduce(0, -$$Lambda$ono9Bp0lMrKbIRfAAYdycY0_qag.INSTANCE);
    }

    public final OptionalInt min() {
        return reduce(-$$Lambda$FZ2W1z3RReutoY2tFnI_NsF0lTk.INSTANCE);
    }

    public final OptionalInt max() {
        return reduce(-$$Lambda$HJTpjoyUrBGPZyR69XwKllqU1YY.INSTANCE);
    }

    public final long count() {
        return mapToLong(-$$Lambda$IntPipeline$Q_Wb7uDnZZMCasMbsGNAwSlprMo.INSTANCE).sum();
    }

    public final OptionalDouble average() {
        long[] avg = (long[]) collect(-$$Lambda$IntPipeline$MrivqBp4YhHB_ix11jxmkPQ1lbE.INSTANCE, -$$Lambda$IntPipeline$0s_rkIyKzlnj_M-bqfCTpum_W2c.INSTANCE, -$$Lambda$IntPipeline$hMFCZ84F0UujzJhdWtPfESTkN2A.INSTANCE);
        if (avg[0] > 0) {
            return OptionalDouble.of(((double) avg[1]) / ((double) avg[0]));
        }
        return OptionalDouble.empty();
    }

    static /* synthetic */ void lambda$average$3(long[] ll, int i) {
        ll[0] = ll[0] + 1;
        ll[1] = ll[1] + ((long) i);
    }

    static /* synthetic */ void lambda$average$4(long[] ll, long[] rr) {
        ll[0] = ll[0] + rr[0];
        ll[1] = ll[1] + rr[1];
    }

    public final IntSummaryStatistics summaryStatistics() {
        return (IntSummaryStatistics) collect(-$$Lambda$_Ea_sNpqZAwihIOCRBaP7hHgWWI.INSTANCE, -$$Lambda$UowTf7vzuMsu4sv1-eMs5iEeNh0.INSTANCE, -$$Lambda$YcgMAuDDScc4HC6CSMDq1R0qa40.INSTANCE);
    }

    public final int reduce(int identity, IntBinaryOperator op) {
        return ((Integer) evaluate(ReduceOps.makeInt(identity, op))).intValue();
    }

    public final OptionalInt reduce(IntBinaryOperator op) {
        return (OptionalInt) evaluate(ReduceOps.makeInt(op));
    }

    public final <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return evaluate(ReduceOps.makeInt(supplier, accumulator, new -$$Lambda$IntPipeline$gTDhYg7hsRI2br4NmAxtQnW5i6Y(combiner)));
    }

    public final boolean anyMatch(IntPredicate predicate) {
        return ((Boolean) evaluate(MatchOps.makeInt(predicate, MatchKind.ANY))).booleanValue();
    }

    public final boolean allMatch(IntPredicate predicate) {
        return ((Boolean) evaluate(MatchOps.makeInt(predicate, MatchKind.ALL))).booleanValue();
    }

    public final boolean noneMatch(IntPredicate predicate) {
        return ((Boolean) evaluate(MatchOps.makeInt(predicate, MatchKind.NONE))).booleanValue();
    }

    public final OptionalInt findFirst() {
        return (OptionalInt) evaluate(FindOps.makeInt(true));
    }

    public final OptionalInt findAny() {
        return (OptionalInt) evaluate(FindOps.makeInt(false));
    }

    public final int[] toArray() {
        return (int[]) Nodes.flattenInt((Node.OfInt) evaluateToArrayNode(-$$Lambda$IntPipeline$ozedusDMANE_B8aDthWCd1L-na4.INSTANCE)).asPrimitiveArray();
    }
}
