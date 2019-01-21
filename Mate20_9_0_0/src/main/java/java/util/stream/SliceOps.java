package java.util.stream;

import java.util.Spliterator;
import java.util.Spliterator.OfDouble;
import java.util.Spliterator.OfInt;
import java.util.Spliterator.OfLong;
import java.util.concurrent.CountedCompleter;
import java.util.function.IntFunction;
import java.util.stream.Node.Builder;
import java.util.stream.ReferencePipeline.StatefulOp;
import java.util.stream.Sink.ChainedDouble;
import java.util.stream.Sink.ChainedInt;
import java.util.stream.Sink.ChainedLong;
import java.util.stream.Sink.ChainedReference;

final class SliceOps {
    static final /* synthetic */ boolean $assertionsDisabled = false;

    private static final class SliceTask<P_IN, P_OUT> extends AbstractShortCircuitTask<P_IN, P_OUT, Node<P_OUT>, SliceTask<P_IN, P_OUT>> {
        private volatile boolean completed;
        private final IntFunction<P_OUT[]> generator;
        private final AbstractPipeline<P_OUT, P_OUT, ?> op;
        private final long targetOffset;
        private final long targetSize;
        private long thisNodeSize;

        SliceTask(AbstractPipeline<P_OUT, P_OUT, ?> op, PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, IntFunction<P_OUT[]> generator, long offset, long size) {
            super((PipelineHelper) helper, (Spliterator) spliterator);
            this.op = op;
            this.generator = generator;
            this.targetOffset = offset;
            this.targetSize = size;
        }

        SliceTask(SliceTask<P_IN, P_OUT> parent, Spliterator<P_IN> spliterator) {
            super((AbstractShortCircuitTask) parent, (Spliterator) spliterator);
            this.op = parent.op;
            this.generator = parent.generator;
            this.targetOffset = parent.targetOffset;
            this.targetSize = parent.targetSize;
        }

        protected SliceTask<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator) {
            return new SliceTask(this, spliterator);
        }

        protected final Node<P_OUT> getEmptyResult() {
            return Nodes.emptyNode(this.op.getOutputShape());
        }

        protected final Node<P_OUT> doLeaf() {
            long j = -1;
            if (isRoot()) {
                if (StreamOpFlag.SIZED.isPreserved(this.op.sourceOrOpFlags)) {
                    j = this.op.exactOutputSizeIfKnown(this.spliterator);
                }
                Builder<P_OUT> nb = this.op.makeNodeBuilder(j, this.generator);
                this.helper.copyIntoWithCancel(this.helper.wrapSink(this.op.opWrapSink(this.helper.getStreamAndOpFlags(), nb)), this.spliterator);
                return nb.build();
            }
            Node<P_OUT> node = ((Builder) this.helper.wrapAndCopyInto(this.helper.makeNodeBuilder(-1, this.generator), this.spliterator)).build();
            this.thisNodeSize = node.count();
            this.completed = true;
            this.spliterator = null;
            return node;
        }

        public final void onCompletion(CountedCompleter<?> caller) {
            if (!isLeaf()) {
                Node<P_OUT> result;
                this.thisNodeSize = ((SliceTask) this.leftChild).thisNodeSize + ((SliceTask) this.rightChild).thisNodeSize;
                if (this.canceled) {
                    this.thisNodeSize = 0;
                    result = getEmptyResult();
                } else if (this.thisNodeSize == 0) {
                    result = getEmptyResult();
                } else if (((SliceTask) this.leftChild).thisNodeSize == 0) {
                    result = (Node) ((SliceTask) this.rightChild).getLocalResult();
                } else {
                    result = Nodes.conc(this.op.getOutputShape(), (Node) ((SliceTask) this.leftChild).getLocalResult(), (Node) ((SliceTask) this.rightChild).getLocalResult());
                }
                setLocalResult(isRoot() ? doTruncate(result) : result);
                this.completed = true;
            }
            if (this.targetSize >= 0 && !isRoot() && isLeftCompleted(this.targetOffset + this.targetSize)) {
                cancelLaterNodes();
            }
            super.onCompletion(caller);
        }

        protected void cancel() {
            super.cancel();
            if (this.completed) {
                setLocalResult(getEmptyResult());
            }
        }

        private Node<P_OUT> doTruncate(Node<P_OUT> input) {
            return input.truncate(this.targetOffset, this.targetSize >= 0 ? Math.min(input.count(), this.targetOffset + this.targetSize) : this.thisNodeSize, this.generator);
        }

        private boolean isLeftCompleted(long target) {
            long size = this.completed ? this.thisNodeSize : completedSize(target);
            boolean z = true;
            if (size >= target) {
                return true;
            }
            long size2 = size;
            AbstractTask node = this;
            for (AbstractTask parent = (SliceTask) getParent(); parent != null; SliceTask parent2 = (SliceTask) parent2.getParent()) {
                if (node == parent2.rightChild) {
                    SliceTask<P_IN, P_OUT> left = parent2.leftChild;
                    if (left != null) {
                        size2 += left.completedSize(target);
                        if (size2 >= target) {
                            return true;
                        }
                    } else {
                        continue;
                    }
                }
                node = parent2;
            }
            if (size2 < target) {
                z = false;
            }
            return z;
        }

        private long completedSize(long target) {
            if (this.completed) {
                return this.thisNodeSize;
            }
            SliceTask<P_IN, P_OUT> left = this.leftChild;
            SliceTask<P_IN, P_OUT> right = this.rightChild;
            if (left == null || right == null) {
                return this.thisNodeSize;
            }
            long leftSize = left.completedSize(target);
            return leftSize >= target ? leftSize : right.completedSize(target) + leftSize;
        }
    }

    private SliceOps() {
    }

    private static long calcSize(long size, long skip, long limit) {
        return size >= 0 ? Math.max(-1, Math.min(size - skip, limit)) : -1;
    }

    private static long calcSliceFence(long skip, long limit) {
        long sliceFence = limit >= 0 ? skip + limit : Long.MAX_VALUE;
        if (sliceFence >= 0) {
            return sliceFence;
        }
        return Long.MAX_VALUE;
    }

    private static <P_IN> Spliterator<P_IN> sliceSpliterator(StreamShape shape, Spliterator<P_IN> s, long skip, long limit) {
        long sliceFence = calcSliceFence(skip, limit);
        switch (shape) {
            case REFERENCE:
                return new OfRef(s, skip, sliceFence);
            case INT_VALUE:
                return new OfInt((OfInt) s, skip, sliceFence);
            case LONG_VALUE:
                return new OfLong((OfLong) s, skip, sliceFence);
            case DOUBLE_VALUE:
                return new OfDouble((OfDouble) s, skip, sliceFence);
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown shape ");
                stringBuilder.append((Object) shape);
                throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private static <T> IntFunction<T[]> castingArray() {
        return -$$Lambda$SliceOps$T0eS2B9nWeCpmA7G2QlMnW3G2UA.INSTANCE;
    }

    public static <T> Stream<T> makeRef(AbstractPipeline<?, T, ?> upstream, long skip, long limit) {
        if (skip >= 0) {
            final long j = skip;
            final long j2 = limit;
            return new StatefulOp<T, T>(upstream, StreamShape.REFERENCE, flags(limit)) {
                Spliterator<T> unorderedSkipLimitSpliterator(Spliterator<T> s, long skip, long limit, long sizeIfKnown) {
                    if (skip <= sizeIfKnown) {
                        limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                        skip = 0;
                    }
                    return new OfRef(s, skip, limit);
                }

                public <P_IN> Spliterator<T> opEvaluateParallelLazy(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
                    Spliterator spliterator2;
                    long size = helper.exactOutputSizeIfKnown(spliterator);
                    if (size > 0) {
                        spliterator2 = spliterator;
                        if (spliterator2.hasCharacteristics(16384)) {
                            return new OfRef(helper.wrapSpliterator(spliterator), j, SliceOps.calcSliceFence(j, j2));
                        }
                    }
                    spliterator2 = spliterator;
                    if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                        return ((Node) new SliceTask(this, helper, spliterator2, SliceOps.castingArray(), j, j2).invoke()).spliterator();
                    }
                    return unorderedSkipLimitSpliterator(helper.wrapSpliterator(spliterator), j, j2, size);
                }

                public <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> helper, Spliterator<P_IN> spliterator, IntFunction<T[]> generator) {
                    Spliterator spliterator2;
                    PipelineHelper<T> pipelineHelper;
                    IntFunction<T[]> intFunction = generator;
                    long size = helper.exactOutputSizeIfKnown(spliterator);
                    if (size > 0) {
                        spliterator2 = spliterator;
                        if (spliterator2.hasCharacteristics(16384)) {
                            return Nodes.collect(helper, SliceOps.sliceSpliterator(helper.getSourceShape(), spliterator2, j, j2), true, intFunction);
                        }
                        pipelineHelper = helper;
                    } else {
                        pipelineHelper = helper;
                        spliterator2 = spliterator;
                    }
                    if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                        return (Node) new SliceTask(this, pipelineHelper, spliterator2, intFunction, j, j2).invoke();
                    }
                    return Nodes.collect(this, unorderedSkipLimitSpliterator(helper.wrapSpliterator(spliterator), j, j2, size), true, intFunction);
                }

                public Sink<T> opWrapSink(int flags, Sink<T> sink) {
                    return new ChainedReference<T, T>(sink) {
                        long m;
                        long n = j;

                        public void begin(long size) {
                            this.downstream.begin(SliceOps.calcSize(size, j, this.m));
                        }

                        public void accept(T t) {
                            if (this.n != 0) {
                                this.n--;
                            } else if (this.m > 0) {
                                this.m--;
                                this.downstream.accept(t);
                            }
                        }

                        public boolean cancellationRequested() {
                            return this.m == 0 || this.downstream.cancellationRequested();
                        }
                    };
                }
            };
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Skip must be non-negative: ");
        stringBuilder.append(skip);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static IntStream makeInt(AbstractPipeline<?, Integer, ?> upstream, long skip, long limit) {
        if (skip >= 0) {
            final long j = skip;
            final long j2 = limit;
            return new IntPipeline.StatefulOp<Integer>(upstream, StreamShape.INT_VALUE, flags(limit)) {
                OfInt unorderedSkipLimitSpliterator(OfInt s, long skip, long limit, long sizeIfKnown) {
                    if (skip <= sizeIfKnown) {
                        limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                        skip = 0;
                    }
                    return new OfInt(s, skip, limit);
                }

                public <P_IN> Spliterator<Integer> opEvaluateParallelLazy(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator) {
                    Spliterator spliterator2;
                    long size = helper.exactOutputSizeIfKnown(spliterator);
                    if (size > 0) {
                        spliterator2 = spliterator;
                        if (spliterator2.hasCharacteristics(16384)) {
                            return new OfInt((OfInt) helper.wrapSpliterator(spliterator), j, SliceOps.calcSliceFence(j, j2));
                        }
                    }
                    spliterator2 = spliterator;
                    if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                        return ((Node) new SliceTask(this, helper, spliterator2, -$$Lambda$SliceOps$2$pJKvYyBs7HGPiOPTm_fxpciSsG8.INSTANCE, j, j2).invoke()).spliterator();
                    }
                    return unorderedSkipLimitSpliterator((OfInt) helper.wrapSpliterator(spliterator), j, j2, size);
                }

                public <P_IN> Node<Integer> opEvaluateParallel(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator, IntFunction<Integer[]> generator) {
                    Spliterator spliterator2;
                    PipelineHelper<Integer> pipelineHelper;
                    long size = helper.exactOutputSizeIfKnown(spliterator);
                    if (size > 0) {
                        spliterator2 = spliterator;
                        if (spliterator2.hasCharacteristics(16384)) {
                            return Nodes.collectInt(helper, SliceOps.sliceSpliterator(helper.getSourceShape(), spliterator2, j, j2), true);
                        }
                        pipelineHelper = helper;
                    } else {
                        pipelineHelper = helper;
                        spliterator2 = spliterator;
                    }
                    if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                        return (Node) new SliceTask(this, pipelineHelper, spliterator2, generator, j, j2).invoke();
                    }
                    return Nodes.collectInt(this, unorderedSkipLimitSpliterator((OfInt) helper.wrapSpliterator(spliterator), j, j2, size), true);
                }

                public Sink<Integer> opWrapSink(int flags, Sink<Integer> sink) {
                    return new ChainedInt<Integer>(sink) {
                        long m;
                        long n = j;

                        public void begin(long size) {
                            this.downstream.begin(SliceOps.calcSize(size, j, this.m));
                        }

                        public void accept(int t) {
                            if (this.n != 0) {
                                this.n--;
                            } else if (this.m > 0) {
                                this.m--;
                                this.downstream.accept(t);
                            }
                        }

                        public boolean cancellationRequested() {
                            return this.m == 0 || this.downstream.cancellationRequested();
                        }
                    };
                }
            };
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Skip must be non-negative: ");
        stringBuilder.append(skip);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static LongStream makeLong(AbstractPipeline<?, Long, ?> upstream, long skip, long limit) {
        if (skip >= 0) {
            final long j = skip;
            final long j2 = limit;
            return new LongPipeline.StatefulOp<Long>(upstream, StreamShape.LONG_VALUE, flags(limit)) {
                OfLong unorderedSkipLimitSpliterator(OfLong s, long skip, long limit, long sizeIfKnown) {
                    if (skip <= sizeIfKnown) {
                        limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                        skip = 0;
                    }
                    return new OfLong(s, skip, limit);
                }

                public <P_IN> Spliterator<Long> opEvaluateParallelLazy(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator) {
                    Spliterator spliterator2;
                    long size = helper.exactOutputSizeIfKnown(spliterator);
                    if (size > 0) {
                        spliterator2 = spliterator;
                        if (spliterator2.hasCharacteristics(16384)) {
                            return new OfLong((OfLong) helper.wrapSpliterator(spliterator), j, SliceOps.calcSliceFence(j, j2));
                        }
                    }
                    spliterator2 = spliterator;
                    if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                        return ((Node) new SliceTask(this, helper, spliterator2, -$$Lambda$SliceOps$3$iKJ8R9VMhJpW3rzcr1q-11o2TH4.INSTANCE, j, j2).invoke()).spliterator();
                    }
                    return unorderedSkipLimitSpliterator((OfLong) helper.wrapSpliterator(spliterator), j, j2, size);
                }

                public <P_IN> Node<Long> opEvaluateParallel(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator, IntFunction<Long[]> generator) {
                    Spliterator spliterator2;
                    PipelineHelper<Long> pipelineHelper;
                    long size = helper.exactOutputSizeIfKnown(spliterator);
                    if (size > 0) {
                        spliterator2 = spliterator;
                        if (spliterator2.hasCharacteristics(16384)) {
                            return Nodes.collectLong(helper, SliceOps.sliceSpliterator(helper.getSourceShape(), spliterator2, j, j2), true);
                        }
                        pipelineHelper = helper;
                    } else {
                        pipelineHelper = helper;
                        spliterator2 = spliterator;
                    }
                    if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                        return (Node) new SliceTask(this, pipelineHelper, spliterator2, generator, j, j2).invoke();
                    }
                    return Nodes.collectLong(this, unorderedSkipLimitSpliterator((OfLong) helper.wrapSpliterator(spliterator), j, j2, size), true);
                }

                public Sink<Long> opWrapSink(int flags, Sink<Long> sink) {
                    return new ChainedLong<Long>(sink) {
                        long m;
                        long n = j;

                        public void begin(long size) {
                            this.downstream.begin(SliceOps.calcSize(size, j, this.m));
                        }

                        public void accept(long t) {
                            if (this.n != 0) {
                                this.n--;
                            } else if (this.m > 0) {
                                this.m--;
                                this.downstream.accept(t);
                            }
                        }

                        public boolean cancellationRequested() {
                            return this.m == 0 || this.downstream.cancellationRequested();
                        }
                    };
                }
            };
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Skip must be non-negative: ");
        stringBuilder.append(skip);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static DoubleStream makeDouble(AbstractPipeline<?, Double, ?> upstream, long skip, long limit) {
        if (skip >= 0) {
            final long j = skip;
            final long j2 = limit;
            return new DoublePipeline.StatefulOp<Double>(upstream, StreamShape.DOUBLE_VALUE, flags(limit)) {
                OfDouble unorderedSkipLimitSpliterator(OfDouble s, long skip, long limit, long sizeIfKnown) {
                    if (skip <= sizeIfKnown) {
                        limit = limit >= 0 ? Math.min(limit, sizeIfKnown - skip) : sizeIfKnown - skip;
                        skip = 0;
                    }
                    return new OfDouble(s, skip, limit);
                }

                public <P_IN> Spliterator<Double> opEvaluateParallelLazy(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator) {
                    Spliterator spliterator2;
                    long size = helper.exactOutputSizeIfKnown(spliterator);
                    if (size > 0) {
                        spliterator2 = spliterator;
                        if (spliterator2.hasCharacteristics(16384)) {
                            return new OfDouble((OfDouble) helper.wrapSpliterator(spliterator), j, SliceOps.calcSliceFence(j, j2));
                        }
                    }
                    spliterator2 = spliterator;
                    if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                        return ((Node) new SliceTask(this, helper, spliterator2, -$$Lambda$SliceOps$4$JdMLhF4N5dBS3gGxMct4lK2SQ04.INSTANCE, j, j2).invoke()).spliterator();
                    }
                    return unorderedSkipLimitSpliterator((OfDouble) helper.wrapSpliterator(spliterator), j, j2, size);
                }

                public <P_IN> Node<Double> opEvaluateParallel(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator, IntFunction<Double[]> generator) {
                    Spliterator spliterator2;
                    PipelineHelper<Double> pipelineHelper;
                    long size = helper.exactOutputSizeIfKnown(spliterator);
                    if (size > 0) {
                        spliterator2 = spliterator;
                        if (spliterator2.hasCharacteristics(16384)) {
                            return Nodes.collectDouble(helper, SliceOps.sliceSpliterator(helper.getSourceShape(), spliterator2, j, j2), true);
                        }
                        pipelineHelper = helper;
                    } else {
                        pipelineHelper = helper;
                        spliterator2 = spliterator;
                    }
                    if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                        return (Node) new SliceTask(this, pipelineHelper, spliterator2, generator, j, j2).invoke();
                    }
                    return Nodes.collectDouble(this, unorderedSkipLimitSpliterator((OfDouble) helper.wrapSpliterator(spliterator), j, j2, size), true);
                }

                public Sink<Double> opWrapSink(int flags, Sink<Double> sink) {
                    return new ChainedDouble<Double>(sink) {
                        long m;
                        long n = j;

                        public void begin(long size) {
                            this.downstream.begin(SliceOps.calcSize(size, j, this.m));
                        }

                        public void accept(double t) {
                            if (this.n != 0) {
                                this.n--;
                            } else if (this.m > 0) {
                                this.m--;
                                this.downstream.accept(t);
                            }
                        }

                        public boolean cancellationRequested() {
                            return this.m == 0 || this.downstream.cancellationRequested();
                        }
                    };
                }
            };
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Skip must be non-negative: ");
        stringBuilder.append(skip);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static int flags(long limit) {
        return StreamOpFlag.NOT_SIZED | (limit != -1 ? StreamOpFlag.IS_SHORT_CIRCUIT : 0);
    }
}
