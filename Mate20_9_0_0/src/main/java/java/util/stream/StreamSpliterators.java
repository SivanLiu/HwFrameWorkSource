package java.util.stream;

import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.SpinedBuffer.OfDouble;
import java.util.stream.SpinedBuffer.OfInt;
import java.util.stream.SpinedBuffer.OfLong;

class StreamSpliterators {

    static abstract class ArrayBuffer {
        int index;

        static abstract class OfPrimitive<T_CONS> extends ArrayBuffer {
            int index;

            abstract void forEach(T_CONS t_cons, long j);

            OfPrimitive() {
            }

            void reset() {
                this.index = 0;
            }
        }

        static final class OfRef<T> extends ArrayBuffer implements Consumer<T> {
            final Object[] array;

            OfRef(int size) {
                this.array = new Object[size];
            }

            public void accept(T t) {
                Object[] objArr = this.array;
                int i = this.index;
                this.index = i + 1;
                objArr[i] = t;
            }

            public void forEach(Consumer<? super T> action, long fence) {
                for (int i = 0; ((long) i) < fence; i++) {
                    action.accept(this.array[i]);
                }
            }
        }

        static final class OfDouble extends OfPrimitive<DoubleConsumer> implements DoubleConsumer {
            final double[] array;

            OfDouble(int size) {
                this.array = new double[size];
            }

            public void accept(double t) {
                double[] dArr = this.array;
                int i = this.index;
                this.index = i + 1;
                dArr[i] = t;
            }

            void forEach(DoubleConsumer action, long fence) {
                for (int i = 0; ((long) i) < fence; i++) {
                    action.accept(this.array[i]);
                }
            }
        }

        static final class OfInt extends OfPrimitive<IntConsumer> implements IntConsumer {
            final int[] array;

            OfInt(int size) {
                this.array = new int[size];
            }

            public void accept(int t) {
                int[] iArr = this.array;
                int i = this.index;
                this.index = i + 1;
                iArr[i] = t;
            }

            public void forEach(IntConsumer action, long fence) {
                for (int i = 0; ((long) i) < fence; i++) {
                    action.accept(this.array[i]);
                }
            }
        }

        static final class OfLong extends OfPrimitive<LongConsumer> implements LongConsumer {
            final long[] array;

            OfLong(int size) {
                this.array = new long[size];
            }

            public void accept(long t) {
                long[] jArr = this.array;
                int i = this.index;
                this.index = i + 1;
                jArr[i] = t;
            }

            public void forEach(LongConsumer action, long fence) {
                for (int i = 0; ((long) i) < fence; i++) {
                    action.accept(this.array[i]);
                }
            }
        }

        ArrayBuffer() {
        }

        void reset() {
            this.index = 0;
        }
    }

    static abstract class SliceSpliterator<T, T_SPLITR extends Spliterator<T>> {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        long fence;
        long index;
        T_SPLITR s;
        final long sliceFence;
        final long sliceOrigin;

        static final class OfRef<T> extends SliceSpliterator<T, Spliterator<T>> implements Spliterator<T> {
            OfRef(Spliterator<T> s, long sliceOrigin, long sliceFence) {
                long j = sliceFence;
                this(s, sliceOrigin, j, 0, Math.min(s.estimateSize(), j));
            }

            private OfRef(Spliterator<T> s, long sliceOrigin, long sliceFence, long origin, long fence) {
                super(s, sliceOrigin, sliceFence, origin, fence);
            }

            protected Spliterator<T> makeSpliterator(Spliterator<T> s, long sliceOrigin, long sliceFence, long origin, long fence) {
                return new OfRef(s, sliceOrigin, sliceFence, origin, fence);
            }

            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                if (this.sliceOrigin >= this.fence) {
                    return false;
                }
                while (this.sliceOrigin > this.index) {
                    this.s.tryAdvance(-$$Lambda$StreamSpliterators$SliceSpliterator$OfRef$WQsOrB6TN5sHvsPJU2O20DZGElU.INSTANCE);
                    this.index++;
                }
                if (this.index >= this.fence) {
                    return false;
                }
                this.index++;
                return this.s.tryAdvance(action);
            }

            static /* synthetic */ void lambda$tryAdvance$0(Object e) {
            }

            public void forEachRemaining(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                if (this.sliceOrigin < this.fence && this.index < this.fence) {
                    if (this.index < this.sliceOrigin || this.index + this.s.estimateSize() > this.sliceFence) {
                        while (this.sliceOrigin > this.index) {
                            this.s.tryAdvance(-$$Lambda$StreamSpliterators$SliceSpliterator$OfRef$NUGTWbZg9cfpPm623I8CORYtfns.INSTANCE);
                            this.index++;
                        }
                        while (this.index < this.fence) {
                            this.s.tryAdvance(action);
                            this.index++;
                        }
                    } else {
                        this.s.forEachRemaining(action);
                        this.index = this.fence;
                    }
                }
            }

            static /* synthetic */ void lambda$forEachRemaining$1(Object e) {
            }
        }

        static abstract class OfPrimitive<T, T_SPLITR extends java.util.Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_CONS> extends SliceSpliterator<T, T_SPLITR> implements java.util.Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            protected abstract T_CONS emptyConsumer();

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfPrimitive trySplit() {
                return (java.util.Spliterator.OfPrimitive) super.trySplit();
            }

            OfPrimitive(T_SPLITR s, long sliceOrigin, long sliceFence) {
                long j = sliceFence;
                this(s, sliceOrigin, j, 0, Math.min(s.estimateSize(), j));
            }

            private OfPrimitive(T_SPLITR s, long sliceOrigin, long sliceFence, long origin, long fence) {
                super(s, sliceOrigin, sliceFence, origin, fence);
            }

            public boolean tryAdvance(T_CONS action) {
                Objects.requireNonNull(action);
                if (this.sliceOrigin >= this.fence) {
                    return false;
                }
                while (this.sliceOrigin > this.index) {
                    ((java.util.Spliterator.OfPrimitive) this.s).tryAdvance(emptyConsumer());
                    this.index++;
                }
                if (this.index >= this.fence) {
                    return false;
                }
                this.index++;
                return ((java.util.Spliterator.OfPrimitive) this.s).tryAdvance(action);
            }

            public void forEachRemaining(T_CONS action) {
                Objects.requireNonNull(action);
                if (this.sliceOrigin < this.fence && this.index < this.fence) {
                    if (this.index < this.sliceOrigin || this.index + ((java.util.Spliterator.OfPrimitive) this.s).estimateSize() > this.sliceFence) {
                        while (this.sliceOrigin > this.index) {
                            ((java.util.Spliterator.OfPrimitive) this.s).tryAdvance(emptyConsumer());
                            this.index++;
                        }
                        while (this.index < this.fence) {
                            ((java.util.Spliterator.OfPrimitive) this.s).tryAdvance(action);
                            this.index++;
                        }
                    } else {
                        ((java.util.Spliterator.OfPrimitive) this.s).forEachRemaining(action);
                        this.index = this.fence;
                    }
                }
            }
        }

        static final class OfDouble extends OfPrimitive<Double, java.util.Spliterator.OfDouble, DoubleConsumer> implements java.util.Spliterator.OfDouble {
            public /* bridge */ /* synthetic */ void forEachRemaining(DoubleConsumer doubleConsumer) {
                super.forEachRemaining(doubleConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(DoubleConsumer doubleConsumer) {
                return super.tryAdvance(doubleConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfDouble trySplit() {
                return (java.util.Spliterator.OfDouble) super.trySplit();
            }

            OfDouble(java.util.Spliterator.OfDouble s, long sliceOrigin, long sliceFence) {
                super(s, sliceOrigin, sliceFence);
            }

            OfDouble(java.util.Spliterator.OfDouble s, long sliceOrigin, long sliceFence, long origin, long fence) {
                super(s, sliceOrigin, sliceFence, origin, fence);
            }

            protected java.util.Spliterator.OfDouble makeSpliterator(java.util.Spliterator.OfDouble s, long sliceOrigin, long sliceFence, long origin, long fence) {
                return new OfDouble(s, sliceOrigin, sliceFence, origin, fence);
            }

            static /* synthetic */ void lambda$emptyConsumer$0(double e) {
            }

            protected DoubleConsumer emptyConsumer() {
                return -$$Lambda$StreamSpliterators$SliceSpliterator$OfDouble$F1bBlpqcoM_HwaVPMQ3Q9zUwTCw.INSTANCE;
            }
        }

        static final class OfInt extends OfPrimitive<Integer, java.util.Spliterator.OfInt, IntConsumer> implements java.util.Spliterator.OfInt {
            public /* bridge */ /* synthetic */ void forEachRemaining(IntConsumer intConsumer) {
                super.forEachRemaining(intConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(IntConsumer intConsumer) {
                return super.tryAdvance(intConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfInt trySplit() {
                return (java.util.Spliterator.OfInt) super.trySplit();
            }

            OfInt(java.util.Spliterator.OfInt s, long sliceOrigin, long sliceFence) {
                super(s, sliceOrigin, sliceFence);
            }

            OfInt(java.util.Spliterator.OfInt s, long sliceOrigin, long sliceFence, long origin, long fence) {
                super(s, sliceOrigin, sliceFence, origin, fence);
            }

            protected java.util.Spliterator.OfInt makeSpliterator(java.util.Spliterator.OfInt s, long sliceOrigin, long sliceFence, long origin, long fence) {
                return new OfInt(s, sliceOrigin, sliceFence, origin, fence);
            }

            static /* synthetic */ void lambda$emptyConsumer$0(int e) {
            }

            protected IntConsumer emptyConsumer() {
                return -$$Lambda$StreamSpliterators$SliceSpliterator$OfInt$GDCU9wlqIN8f-np3lkzlBdIGmvc.INSTANCE;
            }
        }

        static final class OfLong extends OfPrimitive<Long, java.util.Spliterator.OfLong, LongConsumer> implements java.util.Spliterator.OfLong {
            public /* bridge */ /* synthetic */ void forEachRemaining(LongConsumer longConsumer) {
                super.forEachRemaining(longConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(LongConsumer longConsumer) {
                return super.tryAdvance(longConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfLong trySplit() {
                return (java.util.Spliterator.OfLong) super.trySplit();
            }

            OfLong(java.util.Spliterator.OfLong s, long sliceOrigin, long sliceFence) {
                super(s, sliceOrigin, sliceFence);
            }

            OfLong(java.util.Spliterator.OfLong s, long sliceOrigin, long sliceFence, long origin, long fence) {
                super(s, sliceOrigin, sliceFence, origin, fence);
            }

            protected java.util.Spliterator.OfLong makeSpliterator(java.util.Spliterator.OfLong s, long sliceOrigin, long sliceFence, long origin, long fence) {
                return new OfLong(s, sliceOrigin, sliceFence, origin, fence);
            }

            static /* synthetic */ void lambda$emptyConsumer$0(long e) {
            }

            protected LongConsumer emptyConsumer() {
                return -$$Lambda$StreamSpliterators$SliceSpliterator$OfLong$gbTno_el7bKUjUiBqsBq7RYjcY8.INSTANCE;
            }
        }

        protected abstract T_SPLITR makeSpliterator(T_SPLITR t_splitr, long j, long j2, long j3, long j4);

        static {
            Class cls = StreamSpliterators.class;
        }

        SliceSpliterator(T_SPLITR s, long sliceOrigin, long sliceFence, long origin, long fence) {
            this.s = s;
            this.sliceOrigin = sliceOrigin;
            this.sliceFence = sliceFence;
            this.index = origin;
            this.fence = fence;
        }

        public T_SPLITR trySplit() {
            if (this.sliceOrigin >= this.fence || this.index >= this.fence) {
                return null;
            }
            while (true) {
                T_SPLITR leftSplit = this.s.trySplit();
                if (leftSplit == null) {
                    return null;
                }
                long leftSplitFenceUnbounded = this.index + leftSplit.estimateSize();
                long leftSplitFence = Math.min(leftSplitFenceUnbounded, this.sliceFence);
                if (this.sliceOrigin >= leftSplitFence) {
                    this.index = leftSplitFence;
                } else if (leftSplitFence >= this.sliceFence) {
                    this.s = leftSplit;
                    this.fence = leftSplitFence;
                } else if (this.index < this.sliceOrigin || leftSplitFenceUnbounded > this.sliceFence) {
                    long j = this.sliceOrigin;
                    long j2 = this.sliceFence;
                    long j3 = this.index;
                    this.index = leftSplitFence;
                    return makeSpliterator(leftSplit, j, j2, j3, leftSplitFence);
                } else {
                    this.index = leftSplitFence;
                    return leftSplit;
                }
            }
        }

        public long estimateSize() {
            return this.sliceOrigin < this.fence ? this.fence - Math.max(this.sliceOrigin, this.index) : 0;
        }

        public int characteristics() {
            return this.s.characteristics();
        }
    }

    static abstract class UnorderedSliceSpliterator<T, T_SPLITR extends Spliterator<T>> {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        static final int CHUNK_SIZE = 128;
        private final AtomicLong permits;
        protected final T_SPLITR s;
        private final long skipThreshold;
        protected final boolean unlimited;

        static final class OfRef<T> extends UnorderedSliceSpliterator<T, Spliterator<T>> implements Spliterator<T>, Consumer<T> {
            T tmpSlot;

            OfRef(Spliterator<T> s, long skip, long limit) {
                super(s, skip, limit);
            }

            OfRef(Spliterator<T> s, OfRef<T> parent) {
                super(s, parent);
            }

            public final void accept(T t) {
                this.tmpSlot = t;
            }

            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                while (permitStatus() != PermitStatus.NO_MORE && this.s.tryAdvance(this)) {
                    if (acquirePermits(1) == 1) {
                        action.accept(this.tmpSlot);
                        this.tmpSlot = null;
                        return true;
                    }
                }
                return UnorderedSliceSpliterator.$assertionsDisabled;
            }

            public void forEachRemaining(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                OfRef<T> sb = null;
                while (true) {
                    PermitStatus permitStatus = permitStatus();
                    PermitStatus permitStatus2 = permitStatus;
                    if (permitStatus == PermitStatus.NO_MORE) {
                        return;
                    }
                    if (permitStatus2 == PermitStatus.MAYBE_MORE) {
                        if (sb == null) {
                            sb = new OfRef(128);
                        } else {
                            sb.reset();
                        }
                        long permitsRequested = 0;
                        while (this.s.tryAdvance(sb)) {
                            long j = 1 + permitsRequested;
                            permitsRequested = j;
                            if (j >= 128) {
                                break;
                            }
                        }
                        if (permitsRequested != 0) {
                            sb.forEach(action, acquirePermits(permitsRequested));
                        } else {
                            return;
                        }
                    }
                    this.s.forEachRemaining(action);
                    return;
                }
            }

            protected Spliterator<T> makeSpliterator(Spliterator<T> s) {
                return new OfRef(s, this);
            }
        }

        static abstract class OfPrimitive<T, T_CONS, T_BUFF extends OfPrimitive<T_CONS>, T_SPLITR extends java.util.Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends UnorderedSliceSpliterator<T, T_SPLITR> implements java.util.Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            protected abstract void acceptConsumed(T_CONS t_cons);

            protected abstract T_BUFF bufferCreate(int i);

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfPrimitive trySplit() {
                return (java.util.Spliterator.OfPrimitive) super.trySplit();
            }

            OfPrimitive(T_SPLITR s, long skip, long limit) {
                super(s, skip, limit);
            }

            OfPrimitive(T_SPLITR s, OfPrimitive<T, T_CONS, T_BUFF, T_SPLITR> parent) {
                super(s, parent);
            }

            public boolean tryAdvance(T_CONS action) {
                Objects.requireNonNull(action);
                while (permitStatus() != PermitStatus.NO_MORE && ((java.util.Spliterator.OfPrimitive) this.s).tryAdvance(this)) {
                    if (acquirePermits(1) == 1) {
                        acceptConsumed(action);
                        return true;
                    }
                }
                return UnorderedSliceSpliterator.$assertionsDisabled;
            }

            public void forEachRemaining(T_CONS action) {
                Objects.requireNonNull(action);
                T_BUFF sb = null;
                while (true) {
                    PermitStatus permitStatus = permitStatus();
                    PermitStatus permitStatus2 = permitStatus;
                    if (permitStatus == PermitStatus.NO_MORE) {
                        return;
                    }
                    if (permitStatus2 == PermitStatus.MAYBE_MORE) {
                        if (sb == null) {
                            sb = bufferCreate(128);
                        } else {
                            sb.reset();
                        }
                        T_CONS sbc = sb;
                        long permitsRequested = 0;
                        while (((java.util.Spliterator.OfPrimitive) this.s).tryAdvance(sbc)) {
                            long j = 1 + permitsRequested;
                            permitsRequested = j;
                            if (j >= 128) {
                                break;
                            }
                        }
                        if (permitsRequested != 0) {
                            sb.forEach(action, acquirePermits(permitsRequested));
                        } else {
                            return;
                        }
                    }
                    ((java.util.Spliterator.OfPrimitive) this.s).forEachRemaining(action);
                    return;
                }
            }
        }

        enum PermitStatus {
            NO_MORE,
            MAYBE_MORE,
            UNLIMITED
        }

        static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, OfDouble, java.util.Spliterator.OfDouble> implements java.util.Spliterator.OfDouble, DoubleConsumer {
            double tmpValue;

            public /* bridge */ /* synthetic */ void forEachRemaining(DoubleConsumer doubleConsumer) {
                super.forEachRemaining(doubleConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(DoubleConsumer doubleConsumer) {
                return super.tryAdvance(doubleConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfDouble trySplit() {
                return (java.util.Spliterator.OfDouble) super.trySplit();
            }

            OfDouble(java.util.Spliterator.OfDouble s, long skip, long limit) {
                super(s, skip, limit);
            }

            OfDouble(java.util.Spliterator.OfDouble s, OfDouble parent) {
                super(s, parent);
            }

            public void accept(double value) {
                this.tmpValue = value;
            }

            protected void acceptConsumed(DoubleConsumer action) {
                action.accept(this.tmpValue);
            }

            protected OfDouble bufferCreate(int initialCapacity) {
                return new OfDouble(initialCapacity);
            }

            protected java.util.Spliterator.OfDouble makeSpliterator(java.util.Spliterator.OfDouble s) {
                return new OfDouble(s, this);
            }
        }

        static final class OfInt extends OfPrimitive<Integer, IntConsumer, OfInt, java.util.Spliterator.OfInt> implements java.util.Spliterator.OfInt, IntConsumer {
            int tmpValue;

            public /* bridge */ /* synthetic */ void forEachRemaining(IntConsumer intConsumer) {
                super.forEachRemaining(intConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(IntConsumer intConsumer) {
                return super.tryAdvance(intConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfInt trySplit() {
                return (java.util.Spliterator.OfInt) super.trySplit();
            }

            OfInt(java.util.Spliterator.OfInt s, long skip, long limit) {
                super(s, skip, limit);
            }

            OfInt(java.util.Spliterator.OfInt s, OfInt parent) {
                super(s, parent);
            }

            public void accept(int value) {
                this.tmpValue = value;
            }

            protected void acceptConsumed(IntConsumer action) {
                action.accept(this.tmpValue);
            }

            protected OfInt bufferCreate(int initialCapacity) {
                return new OfInt(initialCapacity);
            }

            protected java.util.Spliterator.OfInt makeSpliterator(java.util.Spliterator.OfInt s) {
                return new OfInt(s, this);
            }
        }

        static final class OfLong extends OfPrimitive<Long, LongConsumer, OfLong, java.util.Spliterator.OfLong> implements java.util.Spliterator.OfLong, LongConsumer {
            long tmpValue;

            public /* bridge */ /* synthetic */ void forEachRemaining(LongConsumer longConsumer) {
                super.forEachRemaining(longConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(LongConsumer longConsumer) {
                return super.tryAdvance(longConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfLong trySplit() {
                return (java.util.Spliterator.OfLong) super.trySplit();
            }

            OfLong(java.util.Spliterator.OfLong s, long skip, long limit) {
                super(s, skip, limit);
            }

            OfLong(java.util.Spliterator.OfLong s, OfLong parent) {
                super(s, parent);
            }

            public void accept(long value) {
                this.tmpValue = value;
            }

            protected void acceptConsumed(LongConsumer action) {
                action.accept(this.tmpValue);
            }

            protected OfLong bufferCreate(int initialCapacity) {
                return new OfLong(initialCapacity);
            }

            protected java.util.Spliterator.OfLong makeSpliterator(java.util.Spliterator.OfLong s) {
                return new OfLong(s, this);
            }
        }

        protected abstract T_SPLITR makeSpliterator(T_SPLITR t_splitr);

        static {
            Class cls = StreamSpliterators.class;
        }

        UnorderedSliceSpliterator(T_SPLITR s, long skip, long limit) {
            this.s = s;
            this.unlimited = limit < 0 ? true : $assertionsDisabled;
            this.skipThreshold = limit >= 0 ? limit : 0;
            this.permits = new AtomicLong(limit >= 0 ? skip + limit : skip);
        }

        UnorderedSliceSpliterator(T_SPLITR s, UnorderedSliceSpliterator<T, T_SPLITR> parent) {
            this.s = s;
            this.unlimited = parent.unlimited;
            this.permits = parent.permits;
            this.skipThreshold = parent.skipThreshold;
        }

        protected final long acquirePermits(long numElements) {
            long remainingPermits;
            long grabbing;
            do {
                remainingPermits = this.permits.get();
                long j = 0;
                if (remainingPermits != 0) {
                    grabbing = Math.min(remainingPermits, numElements);
                    if (grabbing <= 0) {
                        break;
                    }
                } else {
                    if (this.unlimited) {
                        j = numElements;
                    }
                    return j;
                }
            } while (!this.permits.compareAndSet(remainingPermits, remainingPermits - grabbing));
            if (this.unlimited) {
                return Math.max(numElements - grabbing, 0);
            }
            if (remainingPermits > this.skipThreshold) {
                return Math.max(grabbing - (remainingPermits - this.skipThreshold), 0);
            }
            return grabbing;
        }

        protected final PermitStatus permitStatus() {
            if (this.permits.get() > 0) {
                return PermitStatus.MAYBE_MORE;
            }
            return this.unlimited ? PermitStatus.UNLIMITED : PermitStatus.NO_MORE;
        }

        public final T_SPLITR trySplit() {
            T_SPLITR t_splitr = null;
            if (this.permits.get() == 0) {
                return null;
            }
            T_SPLITR split = this.s.trySplit();
            if (split != null) {
                t_splitr = makeSpliterator(split);
            }
            return t_splitr;
        }

        public final long estimateSize() {
            return this.s.estimateSize();
        }

        public final int characteristics() {
            return this.s.characteristics() & -16465;
        }
    }

    private static abstract class AbstractWrappingSpliterator<P_IN, P_OUT, T_BUFFER extends AbstractSpinedBuffer> implements Spliterator<P_OUT> {
        T_BUFFER buffer;
        Sink<P_IN> bufferSink;
        boolean finished;
        final boolean isParallel;
        long nextToConsume;
        final PipelineHelper<P_OUT> ph;
        BooleanSupplier pusher;
        Spliterator<P_IN> spliterator;
        private Supplier<Spliterator<P_IN>> spliteratorSupplier;

        abstract void initPartialTraversalState();

        abstract AbstractWrappingSpliterator<P_IN, P_OUT, ?> wrap(Spliterator<P_IN> spliterator);

        AbstractWrappingSpliterator(PipelineHelper<P_OUT> ph, Supplier<Spliterator<P_IN>> spliteratorSupplier, boolean parallel) {
            this.ph = ph;
            this.spliteratorSupplier = spliteratorSupplier;
            this.spliterator = null;
            this.isParallel = parallel;
        }

        AbstractWrappingSpliterator(PipelineHelper<P_OUT> ph, Spliterator<P_IN> spliterator, boolean parallel) {
            this.ph = ph;
            this.spliteratorSupplier = null;
            this.spliterator = spliterator;
            this.isParallel = parallel;
        }

        final void init() {
            if (this.spliterator == null) {
                this.spliterator = (Spliterator) this.spliteratorSupplier.get();
                this.spliteratorSupplier = null;
            }
        }

        final boolean doAdvance() {
            boolean z = false;
            if (this.buffer != null) {
                this.nextToConsume++;
                if (this.nextToConsume < this.buffer.count()) {
                    z = true;
                }
                boolean hasNext = z;
                if (!hasNext) {
                    this.nextToConsume = 0;
                    this.buffer.clear();
                    hasNext = fillBuffer();
                }
                return hasNext;
            } else if (this.finished) {
                return false;
            } else {
                init();
                initPartialTraversalState();
                this.nextToConsume = 0;
                this.bufferSink.begin(this.spliterator.getExactSizeIfKnown());
                return fillBuffer();
            }
        }

        public Spliterator<P_OUT> trySplit() {
            Spliterator<P_OUT> spliterator = null;
            if (!this.isParallel || this.finished) {
                return null;
            }
            init();
            Spliterator<P_IN> split = this.spliterator.trySplit();
            if (split != null) {
                spliterator = wrap(split);
            }
            return spliterator;
        }

        private boolean fillBuffer() {
            while (this.buffer.count() == 0) {
                if (this.bufferSink.cancellationRequested() || !this.pusher.getAsBoolean()) {
                    if (this.finished) {
                        return false;
                    }
                    this.bufferSink.end();
                    this.finished = true;
                }
            }
            return true;
        }

        public final long estimateSize() {
            init();
            return this.spliterator.estimateSize();
        }

        public final long getExactSizeIfKnown() {
            init();
            if (StreamOpFlag.SIZED.isKnown(this.ph.getStreamAndOpFlags())) {
                return this.spliterator.getExactSizeIfKnown();
            }
            return -1;
        }

        public final int characteristics() {
            init();
            int c = StreamOpFlag.toCharacteristics(StreamOpFlag.toStreamFlags(this.ph.getStreamAndOpFlags()));
            if ((c & 64) != 0) {
                return (c & -16449) | (this.spliterator.characteristics() & 16448);
            }
            return c;
        }

        public Comparator<? super P_OUT> getComparator() {
            if (hasCharacteristics(4)) {
                return null;
            }
            throw new IllegalStateException();
        }

        public final String toString() {
            return String.format("%s[%s]", getClass().getName(), this.spliterator);
        }
    }

    static class DelegatingSpliterator<T, T_SPLITR extends Spliterator<T>> implements Spliterator<T> {
        private T_SPLITR s;
        private final Supplier<? extends T_SPLITR> supplier;

        static class OfPrimitive<T, T_CONS, T_SPLITR extends java.util.Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends DelegatingSpliterator<T, T_SPLITR> implements java.util.Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            public /* bridge */ /* synthetic */ java.util.Spliterator.OfPrimitive trySplit() {
                return (java.util.Spliterator.OfPrimitive) super.trySplit();
            }

            OfPrimitive(Supplier<? extends T_SPLITR> supplier) {
                super(supplier);
            }

            public boolean tryAdvance(T_CONS consumer) {
                return ((java.util.Spliterator.OfPrimitive) get()).tryAdvance(consumer);
            }

            public void forEachRemaining(T_CONS consumer) {
                ((java.util.Spliterator.OfPrimitive) get()).forEachRemaining(consumer);
            }
        }

        static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, java.util.Spliterator.OfDouble> implements java.util.Spliterator.OfDouble {
            public /* bridge */ /* synthetic */ void forEachRemaining(DoubleConsumer doubleConsumer) {
                super.forEachRemaining(doubleConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(DoubleConsumer doubleConsumer) {
                return super.tryAdvance(doubleConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfDouble trySplit() {
                return (java.util.Spliterator.OfDouble) super.trySplit();
            }

            OfDouble(Supplier<java.util.Spliterator.OfDouble> supplier) {
                super(supplier);
            }
        }

        static final class OfInt extends OfPrimitive<Integer, IntConsumer, java.util.Spliterator.OfInt> implements java.util.Spliterator.OfInt {
            public /* bridge */ /* synthetic */ void forEachRemaining(IntConsumer intConsumer) {
                super.forEachRemaining(intConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(IntConsumer intConsumer) {
                return super.tryAdvance(intConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfInt trySplit() {
                return (java.util.Spliterator.OfInt) super.trySplit();
            }

            OfInt(Supplier<java.util.Spliterator.OfInt> supplier) {
                super(supplier);
            }
        }

        static final class OfLong extends OfPrimitive<Long, LongConsumer, java.util.Spliterator.OfLong> implements java.util.Spliterator.OfLong {
            public /* bridge */ /* synthetic */ void forEachRemaining(LongConsumer longConsumer) {
                super.forEachRemaining(longConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(LongConsumer longConsumer) {
                return super.tryAdvance(longConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfLong trySplit() {
                return (java.util.Spliterator.OfLong) super.trySplit();
            }

            OfLong(Supplier<java.util.Spliterator.OfLong> supplier) {
                super(supplier);
            }
        }

        DelegatingSpliterator(Supplier<? extends T_SPLITR> supplier) {
            this.supplier = supplier;
        }

        T_SPLITR get() {
            if (this.s == null) {
                this.s = (Spliterator) this.supplier.get();
            }
            return this.s;
        }

        public T_SPLITR trySplit() {
            return get().trySplit();
        }

        public boolean tryAdvance(Consumer<? super T> consumer) {
            return get().tryAdvance(consumer);
        }

        public void forEachRemaining(Consumer<? super T> consumer) {
            get().forEachRemaining(consumer);
        }

        public long estimateSize() {
            return get().estimateSize();
        }

        public int characteristics() {
            return get().characteristics();
        }

        public Comparator<? super T> getComparator() {
            return get().getComparator();
        }

        public long getExactSizeIfKnown() {
            return get().getExactSizeIfKnown();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getClass().getName());
            stringBuilder.append("[");
            stringBuilder.append(get());
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    static final class DistinctSpliterator<T> implements Spliterator<T>, Consumer<T> {
        private static final Object NULL_VALUE = new Object();
        private final Spliterator<T> s;
        private final ConcurrentHashMap<T, Boolean> seen;
        private T tmpSlot;

        DistinctSpliterator(Spliterator<T> s) {
            this(s, new ConcurrentHashMap());
        }

        private DistinctSpliterator(Spliterator<T> s, ConcurrentHashMap<T, Boolean> seen) {
            this.s = s;
            this.seen = seen;
        }

        public void accept(T t) {
            this.tmpSlot = t;
        }

        private T mapNull(T t) {
            return t != null ? t : NULL_VALUE;
        }

        public boolean tryAdvance(Consumer<? super T> action) {
            while (this.s.tryAdvance(this)) {
                if (this.seen.putIfAbsent(mapNull(this.tmpSlot), Boolean.TRUE) == null) {
                    action.accept(this.tmpSlot);
                    this.tmpSlot = null;
                    return true;
                }
            }
            return false;
        }

        public void forEachRemaining(Consumer<? super T> action) {
            this.s.forEachRemaining(new -$$Lambda$StreamSpliterators$DistinctSpliterator$ojM-Hxa6O4-MX3G2cGvIRG3GI58(this, action));
        }

        public static /* synthetic */ void lambda$forEachRemaining$0(DistinctSpliterator distinctSpliterator, Consumer action, Object t) {
            if (distinctSpliterator.seen.putIfAbsent(distinctSpliterator.mapNull(t), Boolean.TRUE) == null) {
                action.accept(t);
            }
        }

        public Spliterator<T> trySplit() {
            Spliterator<T> split = this.s.trySplit();
            return split != null ? new DistinctSpliterator(split, this.seen) : null;
        }

        public long estimateSize() {
            return this.s.estimateSize();
        }

        public int characteristics() {
            return (this.s.characteristics() & -16469) | 1;
        }

        public Comparator<? super T> getComparator() {
            return this.s.getComparator();
        }
    }

    static abstract class InfiniteSupplyingSpliterator<T> implements Spliterator<T> {
        long estimate;

        static final class OfRef<T> extends InfiniteSupplyingSpliterator<T> {
            final Supplier<T> s;

            OfRef(long size, Supplier<T> s) {
                super(size);
                this.s = s;
            }

            public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action);
                action.accept(this.s.get());
                return true;
            }

            public Spliterator<T> trySplit() {
                if (this.estimate == 0) {
                    return null;
                }
                long j = this.estimate >>> 1;
                this.estimate = j;
                return new OfRef(j, this.s);
            }
        }

        static final class OfDouble extends InfiniteSupplyingSpliterator<Double> implements java.util.Spliterator.OfDouble {
            final DoubleSupplier s;

            OfDouble(long size, DoubleSupplier s) {
                super(size);
                this.s = s;
            }

            public boolean tryAdvance(DoubleConsumer action) {
                Objects.requireNonNull(action);
                action.accept(this.s.getAsDouble());
                return true;
            }

            public java.util.Spliterator.OfDouble trySplit() {
                if (this.estimate == 0) {
                    return null;
                }
                long j = this.estimate >>> 1;
                this.estimate = j;
                return new OfDouble(j, this.s);
            }
        }

        static final class OfInt extends InfiniteSupplyingSpliterator<Integer> implements java.util.Spliterator.OfInt {
            final IntSupplier s;

            OfInt(long size, IntSupplier s) {
                super(size);
                this.s = s;
            }

            public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action);
                action.accept(this.s.getAsInt());
                return true;
            }

            public java.util.Spliterator.OfInt trySplit() {
                if (this.estimate == 0) {
                    return null;
                }
                long j = this.estimate >>> 1;
                this.estimate = j;
                return new OfInt(j, this.s);
            }
        }

        static final class OfLong extends InfiniteSupplyingSpliterator<Long> implements java.util.Spliterator.OfLong {
            final LongSupplier s;

            OfLong(long size, LongSupplier s) {
                super(size);
                this.s = s;
            }

            public boolean tryAdvance(LongConsumer action) {
                Objects.requireNonNull(action);
                action.accept(this.s.getAsLong());
                return true;
            }

            public java.util.Spliterator.OfLong trySplit() {
                if (this.estimate == 0) {
                    return null;
                }
                long j = this.estimate >>> 1;
                this.estimate = j;
                return new OfLong(j, this.s);
            }
        }

        protected InfiniteSupplyingSpliterator(long estimate) {
            this.estimate = estimate;
        }

        public long estimateSize() {
            return this.estimate;
        }

        public int characteristics() {
            return 1024;
        }
    }

    static final class WrappingSpliterator<P_IN, P_OUT> extends AbstractWrappingSpliterator<P_IN, P_OUT, SpinedBuffer<P_OUT>> {
        WrappingSpliterator(PipelineHelper<P_OUT> ph, Supplier<Spliterator<P_IN>> supplier, boolean parallel) {
            super((PipelineHelper) ph, (Supplier) supplier, parallel);
        }

        WrappingSpliterator(PipelineHelper<P_OUT> ph, Spliterator<P_IN> spliterator, boolean parallel) {
            super((PipelineHelper) ph, (Spliterator) spliterator, parallel);
        }

        WrappingSpliterator<P_IN, P_OUT> wrap(Spliterator<P_IN> s) {
            return new WrappingSpliterator(this.ph, (Spliterator) s, this.isParallel);
        }

        void initPartialTraversalState() {
            SpinedBuffer<P_OUT> b = new SpinedBuffer();
            this.buffer = b;
            PipelineHelper pipelineHelper = this.ph;
            Objects.requireNonNull(b);
            this.bufferSink = pipelineHelper.wrapSink(new -$$Lambda$GF-s38TgrG6hfxe__ZFdhGp-wPw(b));
            this.pusher = new -$$Lambda$StreamSpliterators$WrappingSpliterator$Ky6g3CKkCccuRWAvbAL1cAsdkNk(this);
        }

        public boolean tryAdvance(Consumer<? super P_OUT> consumer) {
            Objects.requireNonNull(consumer);
            boolean hasNext = doAdvance();
            if (hasNext) {
                consumer.accept(((SpinedBuffer) this.buffer).get(this.nextToConsume));
            }
            return hasNext;
        }

        public void forEachRemaining(Consumer<? super P_OUT> consumer) {
            if (this.buffer != null || this.finished) {
                do {
                } while (tryAdvance(consumer));
                return;
            }
            Objects.requireNonNull(consumer);
            init();
            PipelineHelper pipelineHelper = this.ph;
            Objects.requireNonNull(consumer);
            pipelineHelper.wrapAndCopyInto(new -$$Lambda$btpzqYSQDsLykCcQbI2_g5D3-zs(consumer), this.spliterator);
            this.finished = true;
        }
    }

    static final class DoubleWrappingSpliterator<P_IN> extends AbstractWrappingSpliterator<P_IN, Double, OfDouble> implements Spliterator.OfDouble {
        DoubleWrappingSpliterator(PipelineHelper<Double> ph, Supplier<Spliterator<P_IN>> supplier, boolean parallel) {
            super((PipelineHelper) ph, (Supplier) supplier, parallel);
        }

        DoubleWrappingSpliterator(PipelineHelper<Double> ph, Spliterator<P_IN> spliterator, boolean parallel) {
            super((PipelineHelper) ph, (Spliterator) spliterator, parallel);
        }

        AbstractWrappingSpliterator<P_IN, Double, ?> wrap(Spliterator<P_IN> s) {
            return new DoubleWrappingSpliterator(this.ph, (Spliterator) s, this.isParallel);
        }

        void initPartialTraversalState() {
            OfDouble b = new OfDouble();
            this.buffer = b;
            PipelineHelper pipelineHelper = this.ph;
            Objects.requireNonNull(b);
            this.bufferSink = pipelineHelper.wrapSink(new -$$Lambda$xWqUKn-t_aBWo9sD9bohYsGFiXg(b));
            this.pusher = new -$$Lambda$StreamSpliterators$DoubleWrappingSpliterator$vGvekEV3XchaSAEI93tmYCeVG9A(this);
        }

        public Spliterator.OfDouble trySplit() {
            return (Spliterator.OfDouble) super.trySplit();
        }

        public boolean tryAdvance(DoubleConsumer consumer) {
            Objects.requireNonNull(consumer);
            boolean hasNext = doAdvance();
            if (hasNext) {
                consumer.accept(((OfDouble) this.buffer).get(this.nextToConsume));
            }
            return hasNext;
        }

        public void forEachRemaining(DoubleConsumer consumer) {
            if (this.buffer != null || this.finished) {
                do {
                } while (tryAdvance(consumer));
                return;
            }
            Objects.requireNonNull(consumer);
            init();
            PipelineHelper pipelineHelper = this.ph;
            Objects.requireNonNull(consumer);
            pipelineHelper.wrapAndCopyInto(new -$$Lambda$fgFAI1gk0hw2h3IP9CmHWlY3YkM(consumer), this.spliterator);
            this.finished = true;
        }
    }

    static final class IntWrappingSpliterator<P_IN> extends AbstractWrappingSpliterator<P_IN, Integer, OfInt> implements Spliterator.OfInt {
        IntWrappingSpliterator(PipelineHelper<Integer> ph, Supplier<Spliterator<P_IN>> supplier, boolean parallel) {
            super((PipelineHelper) ph, (Supplier) supplier, parallel);
        }

        IntWrappingSpliterator(PipelineHelper<Integer> ph, Spliterator<P_IN> spliterator, boolean parallel) {
            super((PipelineHelper) ph, (Spliterator) spliterator, parallel);
        }

        AbstractWrappingSpliterator<P_IN, Integer, ?> wrap(Spliterator<P_IN> s) {
            return new IntWrappingSpliterator(this.ph, (Spliterator) s, this.isParallel);
        }

        void initPartialTraversalState() {
            OfInt b = new OfInt();
            this.buffer = b;
            PipelineHelper pipelineHelper = this.ph;
            Objects.requireNonNull(b);
            this.bufferSink = pipelineHelper.wrapSink(new -$$Lambda$ZgCkHA78fnu8poGzKYmvya-ev3U(b));
            this.pusher = new -$$Lambda$StreamSpliterators$IntWrappingSpliterator$js67IRBzuEwtfp5Z3OTF-GfmUTw(this);
        }

        public Spliterator.OfInt trySplit() {
            return (Spliterator.OfInt) super.trySplit();
        }

        public boolean tryAdvance(IntConsumer consumer) {
            Objects.requireNonNull(consumer);
            boolean hasNext = doAdvance();
            if (hasNext) {
                consumer.accept(((OfInt) this.buffer).get(this.nextToConsume));
            }
            return hasNext;
        }

        public void forEachRemaining(IntConsumer consumer) {
            if (this.buffer != null || this.finished) {
                do {
                } while (tryAdvance(consumer));
                return;
            }
            Objects.requireNonNull(consumer);
            init();
            PipelineHelper pipelineHelper = this.ph;
            Objects.requireNonNull(consumer);
            pipelineHelper.wrapAndCopyInto(new -$$Lambda$C9lt_0Cg-SARhdNFJsMyHSsCsGA(consumer), this.spliterator);
            this.finished = true;
        }
    }

    static final class LongWrappingSpliterator<P_IN> extends AbstractWrappingSpliterator<P_IN, Long, OfLong> implements Spliterator.OfLong {
        LongWrappingSpliterator(PipelineHelper<Long> ph, Supplier<Spliterator<P_IN>> supplier, boolean parallel) {
            super((PipelineHelper) ph, (Supplier) supplier, parallel);
        }

        LongWrappingSpliterator(PipelineHelper<Long> ph, Spliterator<P_IN> spliterator, boolean parallel) {
            super((PipelineHelper) ph, (Spliterator) spliterator, parallel);
        }

        AbstractWrappingSpliterator<P_IN, Long, ?> wrap(Spliterator<P_IN> s) {
            return new LongWrappingSpliterator(this.ph, (Spliterator) s, this.isParallel);
        }

        void initPartialTraversalState() {
            OfLong b = new OfLong();
            this.buffer = b;
            PipelineHelper pipelineHelper = this.ph;
            Objects.requireNonNull(b);
            this.bufferSink = pipelineHelper.wrapSink(new -$$Lambda$6BdNjvJJOqgXMfHsEogzyrab-60(b));
            this.pusher = new -$$Lambda$StreamSpliterators$LongWrappingSpliterator$sXmxiR9mZHUX9mr52PfuVCxTtPw(this);
        }

        public Spliterator.OfLong trySplit() {
            return (Spliterator.OfLong) super.trySplit();
        }

        public boolean tryAdvance(LongConsumer consumer) {
            Objects.requireNonNull(consumer);
            boolean hasNext = doAdvance();
            if (hasNext) {
                consumer.accept(((OfLong) this.buffer).get(this.nextToConsume));
            }
            return hasNext;
        }

        public void forEachRemaining(LongConsumer consumer) {
            if (this.buffer != null || this.finished) {
                do {
                } while (tryAdvance(consumer));
                return;
            }
            Objects.requireNonNull(consumer);
            init();
            PipelineHelper pipelineHelper = this.ph;
            Objects.requireNonNull(consumer);
            pipelineHelper.wrapAndCopyInto(new -$$Lambda$G3FiaNZPcIIAnGkHVY7Mdu42X5g(consumer), this.spliterator);
            this.finished = true;
        }
    }

    StreamSpliterators() {
    }
}
