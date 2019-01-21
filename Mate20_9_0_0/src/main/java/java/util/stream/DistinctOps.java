package java.util.stream;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.ReferencePipeline.StatefulOp;
import java.util.stream.Sink.ChainedReference;

final class DistinctOps {
    private DistinctOps() {
    }

    static <T> ReferencePipeline<T, T> makeRef(AbstractPipeline<?, T, ?> upstream) {
        return new StatefulOp<T, T>(upstream, StreamShape.REFERENCE, StreamOpFlag.IS_DISTINCT | StreamOpFlag.NOT_SIZED) {
            <P_IN> Node<T> reduce(PipelineHelper<T> helper, Spliterator<P_IN> spliterator) {
                return Nodes.node((Collection) ReduceOps.makeRef(-$$Lambda$VQnU3Jki1-RCSS5B-Yg_Kf6hQAY.INSTANCE, -$$Lambda$zcFI7bYCRDtB1UMy72aPExbc6R4.INSTANCE, -$$Lambda$r6LgDiay3Ow5w51ifJiV4dn8S84.INSTANCE).evaluateParallel(helper, spliterator));
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
                ForEachOps.makeRef(new -$$Lambda$DistinctOps$1$y8chmSlaKpIKb_VPPBaXdCNT51c(seenNull, map), false).evaluateParallel(helper, spliterator);
                Collection keys = map.keySet();
                if (seenNull.get()) {
                    keys = new HashSet(keys);
                    keys.add(null);
                }
                return Nodes.node(keys);
            }

            static /* synthetic */ void lambda$opEvaluateParallel$0(AtomicBoolean seenNull, ConcurrentHashMap map, Object t) {
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
                        } else if (this.lastSeen == null || !t.equals(this.lastSeen)) {
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
