package java.util.stream;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.-$Lambda$9RlwRKfo_sNL8-VA-0-rDqK5gEM.AnonymousClass3;
import java.util.stream.ReferencePipeline.StatefulOp;
import java.util.stream.Sink.ChainedReference;

final class DistinctOps {
    private DistinctOps() {
    }

    static <T> ReferencePipeline<T, T> makeRef(AbstractPipeline<?, T, ?> upstream) {
        return new StatefulOp<T, T>(upstream, StreamShape.REFERENCE, StreamOpFlag.IS_DISTINCT | StreamOpFlag.NOT_SIZED) {
            <P_IN> Node<T> reduce(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
                return Nodes.node((Collection) ReduceOps.makeRef(new Supplier() {
                    private final /* synthetic */ Object $m$0() {
                        return new LinkedHashSet();
                    }

                    public final Object get() {
                        return $m$0();
                    }
                }, new -$Lambda$9RlwRKfo_sNL8-VA-0-rDqK5gEM(), new BiConsumer() {
                    private final /* synthetic */ void $m$0(Object arg0, Object arg1) {
                        ((LinkedHashSet) arg0).addAll((LinkedHashSet) arg1);
                    }

                    public final void accept(Object obj, Object obj2) {
                        $m$0(obj, obj2);
                    }
                }).evaluateParallel(helper, spliterator));
            }

            public <P_IN> Node<T> opEvaluateParallel(PipelineHelper<T> helper, Spliterator<P_IN> spliterator, IntFunction<T[]> generator) {
                if (StreamOpFlag.DISTINCT.isKnown(helper.getStreamAndOpFlags())) {
                    return helper.evaluate(spliterator, false, generator);
                }
                if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return reduce(helper, spliterator);
                }
                AtomicBoolean seenNull = new AtomicBoolean(false);
                ConcurrentHashMap<T, Boolean> map = new ConcurrentHashMap();
                ForEachOps.makeRef(new AnonymousClass3(seenNull, map), false).evaluateParallel(helper, spliterator);
                Collection keys = map.keySet();
                if (seenNull.get()) {
                    Set<T> keys2 = new HashSet(keys);
                    keys2.add(null);
                    keys = keys2;
                }
                return Nodes.node(keys);
            }

            static /* synthetic */ void lambda$-java_util_stream_DistinctOps$1_3835(AtomicBoolean seenNull, ConcurrentHashMap map, Object t) {
                if (t == null) {
                    seenNull.set(true);
                } else {
                    map.putIfAbsent(t, Boolean.TRUE);
                }
            }

            public <P_IN> Spliterator<T> opEvaluateParallelLazy(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
                if (StreamOpFlag.DISTINCT.isKnown(helper.getStreamAndOpFlags())) {
                    return helper.wrapSpliterator(spliterator);
                }
                if (StreamOpFlag.ORDERED.isKnown(helper.getStreamAndOpFlags())) {
                    return reduce(helper, spliterator).spliterator();
                }
                return new DistinctSpliterator(helper.wrapSpliterator(spliterator));
            }

            public Sink<T> opWrapSink(int flags, Sink<T> sink) {
                Objects.requireNonNull(sink);
                if (StreamOpFlag.DISTINCT.isKnown(flags)) {
                    return sink;
                }
                return StreamOpFlag.SORTED.isKnown(flags) ? new ChainedReference<T, T>(sink) {
                    T lastSeen;
                    boolean seenNull;

                    public void begin(long size) {
                        this.seenNull = false;
                        this.lastSeen = null;
                        this.downstream.begin(-1);
                    }

                    public void end() {
                        this.seenNull = false;
                        this.lastSeen = null;
                        this.downstream.end();
                    }

                    public void accept(T t) {
                        Sink sink;
                        if (t == null) {
                            if (!this.seenNull) {
                                this.seenNull = true;
                                sink = this.downstream;
                                this.lastSeen = null;
                                sink.accept(null);
                            }
                        } else if (this.lastSeen == null || (t.equals(this.lastSeen) ^ 1) != 0) {
                            sink = this.downstream;
                            this.lastSeen = t;
                            sink.accept(t);
                        }
                    }
                } : new ChainedReference<T, T>(sink) {
                    Set<T> seen;

                    public void begin(long size) {
                        this.seen = new HashSet();
                        this.downstream.begin(-1);
                    }

                    public void end() {
                        this.seen = null;
                        this.downstream.end();
                    }

                    public void accept(T t) {
                        if (!this.seen.contains(t)) {
                            this.seen.add(t);
                            this.downstream.accept(t);
                        }
                    }
                };
            }
        };
    }
}
