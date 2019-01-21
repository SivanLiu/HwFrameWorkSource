package java.util.stream;

import java.util.DoubleSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterator.OfDouble;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.Node.Builder;
import java.util.stream.Sink.ChainedDouble;

public abstract class DoublePipeline<E_IN> extends AbstractPipeline<E_IN, Double, DoubleStream> implements DoubleStream {

    public static class Head<E_IN> extends DoublePipeline<E_IN> {
        public /* bridge */ /* synthetic */ DoubleStream parallel() {
            return (DoubleStream) super.parallel();
        }

        public /* bridge */ /* synthetic */ DoubleStream sequential() {
            return (DoubleStream) super.sequential();
        }

        public Head(Supplier<? extends Spliterator<Double>> source, int sourceFlags, boolean parallel) {
            super((Supplier) source, sourceFlags, parallel);
        }

        public Head(Spliterator<Double> source, int sourceFlags, boolean parallel) {
            super((Spliterator) source, sourceFlags, parallel);
        }

        public final boolean opIsStateful() {
            throw new UnsupportedOperationException();
        }

        public final Sink<E_IN> opWrapSink(int flags, Sink<Double> sink) {
            throw new UnsupportedOperationException();
        }

        public void forEach(DoubleConsumer consumer) {
            if (isParallel()) {
                super.forEach(consumer);
            } else {
                DoublePipeline.adapt(sourceStageSpliterator()).forEachRemaining(consumer);
            }
        }

        public void forEachOrdered(DoubleConsumer consumer) {
            if (isParallel()) {
                super.forEachOrdered(consumer);
            } else {
                DoublePipeline.adapt(sourceStageSpliterator()).forEachRemaining(consumer);
            }
        }
    }

    public static abstract class StatefulOp<E_IN> extends DoublePipeline<E_IN> {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        public abstract <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> pipelineHelper, Spliterator<P_IN> spliterator, IntFunction<Double[]> intFunction);

        static {
            Class cls = DoublePipeline.class;
        }

        public /* bridge */ /* synthetic */ DoubleStream parallel() {
            return (DoubleStream) super.parallel();
        }

        public /* bridge */ /* synthetic */ DoubleStream sequential() {
            return (DoubleStream) super.sequential();
        }

        public StatefulOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
        }

        public final boolean opIsStateful() {
            return true;
        }
    }

    public static abstract class StatelessOp<E_IN> extends DoublePipeline<E_IN> {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = DoublePipeline.class;
        }

        public /* bridge */ /* synthetic */ DoubleStream parallel() {
            return (DoubleStream) super.parallel();
        }

        public /* bridge */ /* synthetic */ DoubleStream sequential() {
            return (DoubleStream) super.sequential();
        }

        public StatelessOp(AbstractPipeline<?, E_IN, ?> upstream, StreamShape inputShape, int opFlags) {
            super(upstream, opFlags);
        }

        public final boolean opIsStateful() {
            return false;
        }
    }

    public /* bridge */ /* synthetic */ DoubleStream parallel() {
        return (DoubleStream) super.parallel();
    }

    public /* bridge */ /* synthetic */ DoubleStream sequential() {
        return (DoubleStream) super.sequential();
    }

    DoublePipeline(Supplier<? extends Spliterator<Double>> source, int sourceFlags, boolean parallel) {
        super((Supplier) source, sourceFlags, parallel);
    }

    DoublePipeline(Spliterator<Double> source, int sourceFlags, boolean parallel) {
        super((Spliterator) source, sourceFlags, parallel);
    }

    DoublePipeline(AbstractPipeline<?, E_IN, ?> upstream, int opFlags) {
        super(upstream, opFlags);
    }

    private static DoubleConsumer adapt(Sink<Double> sink) {
        if (sink instanceof DoubleConsumer) {
            return (DoubleConsumer) sink;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using DoubleStream.adapt(Sink<Double> s)");
        }
        Objects.requireNonNull(sink);
        return new -$$Lambda$G0LLxk8pWitjFgsOx2bYtRO-rGg(sink);
    }

    private static OfDouble adapt(Spliterator<Double> s) {
        if (s instanceof OfDouble) {
            return (OfDouble) s;
        }
        if (Tripwire.ENABLED) {
            Tripwire.trip(AbstractPipeline.class, "using DoubleStream.adapt(Spliterator<Double> s)");
        }
        throw new UnsupportedOperationException("DoubleStream.adapt(Spliterator<Double> s)");
    }

    public final StreamShape getOutputShape() {
        return StreamShape.DOUBLE_VALUE;
    }

    public final <P_IN> Node<Double> evaluateToNode(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<Double[]> intFunction) {
        return Nodes.collectDouble(helper, spliterator, flattenTree);
    }

    public final <P_IN> Spliterator<Double> wrap(PipelineHelper<Double> ph, Supplier<Spliterator<P_IN>> supplier, boolean isParallel) {
        return new DoubleWrappingSpliterator((PipelineHelper) ph, (Supplier) supplier, isParallel);
    }

    public final OfDouble lazySpliterator(Supplier<? extends Spliterator<Double>> supplier) {
        return new OfDouble(supplier);
    }

    public final void forEachWithCancel(Spliterator<Double> spliterator, Sink<Double> sink) {
        OfDouble spl = adapt((Spliterator) spliterator);
        DoubleConsumer adaptedSink = adapt((Sink) sink);
        while (!sink.cancellationRequested()) {
            if (!spl.tryAdvance(adaptedSink)) {
                return;
            }
        }
    }

    public final Builder<Double> makeNodeBuilder(long exactSizeIfKnown, IntFunction<Double[]> intFunction) {
        return Nodes.doubleBuilder(exactSizeIfKnown);
    }

    public final PrimitiveIterator.OfDouble iterator() {
        return Spliterators.iterator(spliterator());
    }

    public final OfDouble spliterator() {
        return adapt(super.spliterator());
    }

    public final Stream<Double> boxed() {
        return mapToObj(-$$Lambda$0HimmAYr5h1pFdNckEhxJ9y9Zqk.INSTANCE);
    }

    public final DoubleStream map(DoubleUnaryOperator mapper) {
        Objects.requireNonNull(mapper);
        final DoubleUnaryOperator doubleUnaryOperator = mapper;
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new ChainedDouble<Double>(sink) {
                    public void accept(double t) {
                        this.downstream.accept(doubleUnaryOperator.applyAsDouble(t));
                    }
                };
            }
        };
    }

    public final <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        Objects.requireNonNull(mapper);
        final DoubleFunction<? extends U> doubleFunction = mapper;
        return new java.util.stream.ReferencePipeline.StatelessOp<Double, U>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Double> opWrapSink(int flags, Sink<U> sink) {
                return new ChainedDouble<U>(sink) {
                    public void accept(double t) {
                        this.downstream.accept(doubleFunction.apply(t));
                    }
                };
            }
        };
    }

    public final IntStream mapToInt(DoubleToIntFunction mapper) {
        Objects.requireNonNull(mapper);
        final DoubleToIntFunction doubleToIntFunction = mapper;
        return new java.util.stream.IntPipeline.StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Double> opWrapSink(int flags, Sink<Integer> sink) {
                return new ChainedDouble<Integer>(sink) {
                    public void accept(double t) {
                        this.downstream.accept(doubleToIntFunction.applyAsInt(t));
                    }
                };
            }
        };
    }

    public final LongStream mapToLong(DoubleToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        final DoubleToLongFunction doubleToLongFunction = mapper;
        return new java.util.stream.LongPipeline.StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) {
            public Sink<Double> opWrapSink(int flags, Sink<Long> sink) {
                return new ChainedDouble<Long>(sink) {
                    public void accept(double t) {
                        this.downstream.accept(doubleToLongFunction.applyAsLong(t));
                    }
                };
            }
        };
    }

    public final DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        final DoubleFunction<? extends DoubleStream> doubleFunction = mapper;
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, (StreamOpFlag.NOT_SORTED | StreamOpFlag.NOT_DISTINCT) | StreamOpFlag.NOT_SIZED) {
            public Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new ChainedDouble<Double>(sink) {
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
                    public void accept(double t) {
                        DoubleStream result = (DoubleStream) doubleFunction.apply(t);
                        if (result != null) {
                            result.sequential().forEach(new -$$Lambda$DoublePipeline$5$1$kqJiVK7sQB3kKvPk9DB9gInHJq4(this));
                        }
                        if (result != null) {
                            result.close();
                        }
                    }
                };
            }
        };
    }

    public DoubleStream unordered() {
        if (isOrdered()) {
            return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_ORDERED) {
                public Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                    return sink;
                }
            };
        }
        return this;
    }

    public final DoubleStream filter(DoublePredicate predicate) {
        Objects.requireNonNull(predicate);
        final DoublePredicate doublePredicate = predicate;
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, StreamOpFlag.NOT_SIZED) {
            public Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new ChainedDouble<Double>(sink) {
                    public void begin(long size) {
                        this.downstream.begin(-1);
                    }

                    public void accept(double t) {
                        if (doublePredicate.test(t)) {
                            this.downstream.accept(t);
                        }
                    }
                };
            }
        };
    }

    public final DoubleStream peek(DoubleConsumer action) {
        Objects.requireNonNull(action);
        final DoubleConsumer doubleConsumer = action;
        return new StatelessOp<Double>(this, StreamShape.DOUBLE_VALUE, 0) {
            public Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                return new ChainedDouble<Double>(sink) {
                    public void accept(double t) {
                        doubleConsumer.accept(t);
                        this.downstream.accept(t);
                    }
                };
            }
        };
    }

    public final DoubleStream limit(long maxSize) {
        if (maxSize >= 0) {
            return SliceOps.makeDouble(this, 0, maxSize);
        }
        throw new IllegalArgumentException(Long.toString(maxSize));
    }

    public final DoubleStream skip(long n) {
        if (n < 0) {
            throw new IllegalArgumentException(Long.toString(n));
        } else if (n == 0) {
            return this;
        } else {
            return SliceOps.makeDouble(this, n, -1);
        }
    }

    public final DoubleStream sorted() {
        return SortedOps.makeDouble(this);
    }

    public final DoubleStream distinct() {
        return boxed().distinct().mapToDouble(-$$Lambda$DoublePipeline$gq0fD9NZ938fl5Zgm1Lwm9G2tpI.INSTANCE);
    }

    public void forEach(DoubleConsumer consumer) {
        evaluate(ForEachOps.makeDouble(consumer, false));
    }

    public void forEachOrdered(DoubleConsumer consumer) {
        evaluate(ForEachOps.makeDouble(consumer, true));
    }

    public final double sum() {
        return Collectors.computeFinalSum((double[]) collect(-$$Lambda$DoublePipeline$jsM76ecD5K_oP4TaArM1RdmdjOw.INSTANCE, -$$Lambda$DoublePipeline$btJQIF5a5bk658mbj9AIl0UV19Q.INSTANCE, -$$Lambda$DoublePipeline$KYIKJiRuFnKlAv02sN6Y0G5US7E.INSTANCE));
    }

    static /* synthetic */ void lambda$sum$2(double[] ll, double d) {
        Collectors.sumWithCompensation(ll, d);
        ll[2] = ll[2] + d;
    }

    static /* synthetic */ void lambda$sum$3(double[] ll, double[] rr) {
        Collectors.sumWithCompensation(ll, rr[0]);
        Collectors.sumWithCompensation(ll, rr[1]);
        ll[2] = ll[2] + rr[2];
    }

    public final OptionalDouble min() {
        return reduce(-$$Lambda$Xsl4nKeYydTETtdRjTtEXmjJItE.INSTANCE);
    }

    public final OptionalDouble max() {
        return reduce(-$$Lambda$xi7ZBZfKmkbt5CSsaL8qlNeHupc.INSTANCE);
    }

    public final OptionalDouble average() {
        double[] avg = (double[]) collect(-$$Lambda$DoublePipeline$O7F4ENrC3oYj9E0vblCKW9Dec60.INSTANCE, -$$Lambda$DoublePipeline$lWQTyY6EPN0Xvhyjp5Lr5ZKBDCA.INSTANCE, -$$Lambda$DoublePipeline$8lpXAdS4oGMq6Yo_dNhNdoP-gg0.INSTANCE);
        if (avg[2] > 0.0d) {
            return OptionalDouble.of(Collectors.computeFinalSum(avg) / avg[2]);
        }
        return OptionalDouble.empty();
    }

    static /* synthetic */ void lambda$average$5(double[] ll, double d) {
        ll[2] = ll[2] + 1.0d;
        Collectors.sumWithCompensation(ll, d);
        ll[3] = ll[3] + d;
    }

    static /* synthetic */ void lambda$average$6(double[] ll, double[] rr) {
        Collectors.sumWithCompensation(ll, rr[0]);
        Collectors.sumWithCompensation(ll, rr[1]);
        ll[2] = ll[2] + rr[2];
        ll[3] = ll[3] + rr[3];
    }

    public final long count() {
        return mapToLong(-$$Lambda$DoublePipeline$V2mM4_kocaa0EZ7g04Qc6_Yd13E.INSTANCE).sum();
    }

    public final DoubleSummaryStatistics summaryStatistics() {
        return (DoubleSummaryStatistics) collect(-$$Lambda$745FUy7cYwYu7KrMQTYh2DNqh1I.INSTANCE, -$$Lambda$9-clh6DyAY2rGfAxuH1sO9aEBuU.INSTANCE, -$$Lambda$BZcmU4lh1MU8ke57orLk6ELdvT4.INSTANCE);
    }

    public final double reduce(double identity, DoubleBinaryOperator op) {
        return ((Double) evaluate(ReduceOps.makeDouble(identity, op))).doubleValue();
    }

    public final OptionalDouble reduce(DoubleBinaryOperator op) {
        return (OptionalDouble) evaluate(ReduceOps.makeDouble(op));
    }

    public final <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return evaluate(ReduceOps.makeDouble(supplier, accumulator, new -$$Lambda$DoublePipeline$IBZGhEgRy1ddKsqLtAJ-JIbQPE8(combiner)));
    }

    public final boolean anyMatch(DoublePredicate predicate) {
        return ((Boolean) evaluate(MatchOps.makeDouble(predicate, MatchKind.ANY))).booleanValue();
    }

    public final boolean allMatch(DoublePredicate predicate) {
        return ((Boolean) evaluate(MatchOps.makeDouble(predicate, MatchKind.ALL))).booleanValue();
    }

    public final boolean noneMatch(DoublePredicate predicate) {
        return ((Boolean) evaluate(MatchOps.makeDouble(predicate, MatchKind.NONE))).booleanValue();
    }

    public final OptionalDouble findFirst() {
        return (OptionalDouble) evaluate(FindOps.makeDouble(true));
    }

    public final OptionalDouble findAny() {
        return (OptionalDouble) evaluate(FindOps.makeDouble(false));
    }

    public final double[] toArray() {
        return (double[]) Nodes.flattenDouble((Node.OfDouble) evaluateToArrayNode(-$$Lambda$DoublePipeline$VwL6T93St4bY9lzEXgl24N_DcA4.INSTANCE)).asPrimitiveArray();
    }
}
