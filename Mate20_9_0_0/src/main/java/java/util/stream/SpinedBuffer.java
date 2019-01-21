package java.util.stream;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;

public class SpinedBuffer<E> extends AbstractSpinedBuffer implements Consumer<E>, Iterable<E> {
    private static final int SPLITERATOR_CHARACTERISTICS = 16464;
    protected E[] curChunk;
    protected E[][] spine;

    /* renamed from: java.util.stream.SpinedBuffer$1Splitr */
    class AnonymousClass1Splitr implements Spliterator<E> {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        final int lastSpineElementFence;
        final int lastSpineIndex;
        E[] splChunk;
        int splElementIndex;
        int splSpineIndex;

        static {
            Class cls = SpinedBuffer.class;
        }

        AnonymousClass1Splitr(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
            this.splSpineIndex = firstSpineIndex;
            this.lastSpineIndex = lastSpineIndex;
            this.splElementIndex = firstSpineElementIndex;
            this.lastSpineElementFence = lastSpineElementFence;
            this.splChunk = SpinedBuffer.this.spine == null ? SpinedBuffer.this.curChunk : SpinedBuffer.this.spine[firstSpineIndex];
        }

        public long estimateSize() {
            if (this.splSpineIndex == this.lastSpineIndex) {
                return ((long) this.lastSpineElementFence) - ((long) this.splElementIndex);
            }
            return ((SpinedBuffer.this.priorElementCount[this.lastSpineIndex] + ((long) this.lastSpineElementFence)) - SpinedBuffer.this.priorElementCount[this.splSpineIndex]) - ((long) this.splElementIndex);
        }

        public int characteristics() {
            return SpinedBuffer.SPLITERATOR_CHARACTERISTICS;
        }

        public boolean tryAdvance(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            if (this.splSpineIndex >= this.lastSpineIndex && (this.splSpineIndex != this.lastSpineIndex || this.splElementIndex >= this.lastSpineElementFence)) {
                return false;
            }
            Object[] objArr = this.splChunk;
            int i = this.splElementIndex;
            this.splElementIndex = i + 1;
            consumer.accept(objArr[i]);
            if (this.splElementIndex == this.splChunk.length) {
                this.splElementIndex = 0;
                this.splSpineIndex++;
                if (SpinedBuffer.this.spine != null && this.splSpineIndex <= this.lastSpineIndex) {
                    this.splChunk = SpinedBuffer.this.spine[this.splSpineIndex];
                }
            }
            return true;
        }

        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            if (this.splSpineIndex < this.lastSpineIndex || (this.splSpineIndex == this.lastSpineIndex && this.splElementIndex < this.lastSpineElementFence)) {
                int i = this.splElementIndex;
                for (int sp = this.splSpineIndex; sp < this.lastSpineIndex; sp++) {
                    E[] chunk = SpinedBuffer.this.spine[sp];
                    while (i < chunk.length) {
                        consumer.accept(chunk[i]);
                        i++;
                    }
                    i = 0;
                }
                E[] chunk2 = this.splSpineIndex == this.lastSpineIndex ? this.splChunk : SpinedBuffer.this.spine[this.lastSpineIndex];
                int hElementIndex = this.lastSpineElementFence;
                while (i < hElementIndex) {
                    consumer.accept(chunk2[i]);
                    i++;
                }
                this.splSpineIndex = this.lastSpineIndex;
                this.splElementIndex = this.lastSpineElementFence;
            }
        }

        public Spliterator<E> trySplit() {
            if (this.splSpineIndex < this.lastSpineIndex) {
                Spliterator<E> anonymousClass1Splitr = new AnonymousClass1Splitr(this.splSpineIndex, this.lastSpineIndex - 1, this.splElementIndex, SpinedBuffer.this.spine[this.lastSpineIndex - 1].length);
                this.splSpineIndex = this.lastSpineIndex;
                this.splElementIndex = 0;
                this.splChunk = SpinedBuffer.this.spine[this.splSpineIndex];
                return anonymousClass1Splitr;
            } else if (this.splSpineIndex != this.lastSpineIndex) {
                return null;
            } else {
                int t = (this.lastSpineElementFence - this.splElementIndex) / 2;
                if (t == 0) {
                    return null;
                }
                Spliterator<E> ret = Arrays.spliterator(this.splChunk, this.splElementIndex, this.splElementIndex + t);
                this.splElementIndex += t;
                return ret;
            }
        }
    }

    public static abstract class OfPrimitive<E, T_ARR, T_CONS> extends AbstractSpinedBuffer implements Iterable<E> {
        T_ARR curChunk = newArray(1 << this.initialChunkPower);
        T_ARR[] spine;

        abstract class BaseSpliterator<T_SPLITR extends java.util.Spliterator.OfPrimitive<E, T_CONS, T_SPLITR>> implements java.util.Spliterator.OfPrimitive<E, T_CONS, T_SPLITR> {
            static final /* synthetic */ boolean $assertionsDisabled = false;
            final int lastSpineElementFence;
            final int lastSpineIndex;
            T_ARR splChunk;
            int splElementIndex;
            int splSpineIndex;

            abstract void arrayForOne(T_ARR t_arr, int i, T_CONS t_cons);

            abstract T_SPLITR arraySpliterator(T_ARR t_arr, int i, int i2);

            abstract T_SPLITR newSpliterator(int i, int i2, int i3, int i4);

            static {
                Class cls = SpinedBuffer.class;
            }

            BaseSpliterator(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                this.splSpineIndex = firstSpineIndex;
                this.lastSpineIndex = lastSpineIndex;
                this.splElementIndex = firstSpineElementIndex;
                this.lastSpineElementFence = lastSpineElementFence;
                this.splChunk = OfPrimitive.this.spine == null ? OfPrimitive.this.curChunk : OfPrimitive.this.spine[firstSpineIndex];
            }

            public long estimateSize() {
                if (this.splSpineIndex == this.lastSpineIndex) {
                    return ((long) this.lastSpineElementFence) - ((long) this.splElementIndex);
                }
                return ((OfPrimitive.this.priorElementCount[this.lastSpineIndex] + ((long) this.lastSpineElementFence)) - OfPrimitive.this.priorElementCount[this.splSpineIndex]) - ((long) this.splElementIndex);
            }

            public int characteristics() {
                return SpinedBuffer.SPLITERATOR_CHARACTERISTICS;
            }

            public boolean tryAdvance(T_CONS consumer) {
                Objects.requireNonNull(consumer);
                if (this.splSpineIndex >= this.lastSpineIndex && (this.splSpineIndex != this.lastSpineIndex || this.splElementIndex >= this.lastSpineElementFence)) {
                    return false;
                }
                Object obj = this.splChunk;
                int i = this.splElementIndex;
                this.splElementIndex = i + 1;
                arrayForOne(obj, i, consumer);
                if (this.splElementIndex == OfPrimitive.this.arrayLength(this.splChunk)) {
                    this.splElementIndex = 0;
                    this.splSpineIndex++;
                    if (OfPrimitive.this.spine != null && this.splSpineIndex <= this.lastSpineIndex) {
                        this.splChunk = OfPrimitive.this.spine[this.splSpineIndex];
                    }
                }
                return true;
            }

            public void forEachRemaining(T_CONS consumer) {
                Objects.requireNonNull(consumer);
                if (this.splSpineIndex < this.lastSpineIndex || (this.splSpineIndex == this.lastSpineIndex && this.splElementIndex < this.lastSpineElementFence)) {
                    int i = this.splElementIndex;
                    for (int sp = this.splSpineIndex; sp < this.lastSpineIndex; sp++) {
                        T_ARR chunk = OfPrimitive.this.spine[sp];
                        OfPrimitive.this.arrayForEach(chunk, i, OfPrimitive.this.arrayLength(chunk), consumer);
                        i = 0;
                    }
                    OfPrimitive.this.arrayForEach(this.splSpineIndex == this.lastSpineIndex ? this.splChunk : OfPrimitive.this.spine[this.lastSpineIndex], i, this.lastSpineElementFence, consumer);
                    this.splSpineIndex = this.lastSpineIndex;
                    this.splElementIndex = this.lastSpineElementFence;
                }
            }

            public T_SPLITR trySplit() {
                if (this.splSpineIndex < this.lastSpineIndex) {
                    T_SPLITR ret = newSpliterator(this.splSpineIndex, this.lastSpineIndex - 1, this.splElementIndex, OfPrimitive.this.arrayLength(OfPrimitive.this.spine[this.lastSpineIndex - 1]));
                    this.splSpineIndex = this.lastSpineIndex;
                    this.splElementIndex = 0;
                    this.splChunk = OfPrimitive.this.spine[this.splSpineIndex];
                    return ret;
                } else if (this.splSpineIndex != this.lastSpineIndex) {
                    return null;
                } else {
                    int t = (this.lastSpineElementFence - this.splElementIndex) / 2;
                    if (t == 0) {
                        return null;
                    }
                    T_SPLITR ret2 = arraySpliterator(this.splChunk, this.splElementIndex, t);
                    this.splElementIndex += t;
                    return ret2;
                }
            }
        }

        protected abstract void arrayForEach(T_ARR t_arr, int i, int i2, T_CONS t_cons);

        protected abstract int arrayLength(T_ARR t_arr);

        public abstract void forEach(Consumer<? super E> consumer);

        public abstract Iterator<E> iterator();

        public abstract T_ARR newArray(int i);

        protected abstract T_ARR[] newArrayArray(int i);

        public /* bridge */ /* synthetic */ long count() {
            return super.count();
        }

        public /* bridge */ /* synthetic */ boolean isEmpty() {
            return super.isEmpty();
        }

        OfPrimitive(int initialCapacity) {
            super(initialCapacity);
        }

        OfPrimitive() {
        }

        protected long capacity() {
            if (this.spineIndex == 0) {
                return (long) arrayLength(this.curChunk);
            }
            return this.priorElementCount[this.spineIndex] + ((long) arrayLength(this.spine[this.spineIndex]));
        }

        private void inflateSpine() {
            if (this.spine == null) {
                this.spine = newArrayArray(8);
                this.priorElementCount = new long[8];
                this.spine[0] = this.curChunk;
            }
        }

        protected final void ensureCapacity(long targetSize) {
            long capacity = capacity();
            if (targetSize > capacity) {
                inflateSpine();
                int i = this.spineIndex;
                while (true) {
                    i++;
                    if (targetSize > capacity) {
                        int newSpineSize;
                        if (i >= this.spine.length) {
                            newSpineSize = this.spine.length * 2;
                            this.spine = Arrays.copyOf(this.spine, newSpineSize);
                            this.priorElementCount = Arrays.copyOf(this.priorElementCount, newSpineSize);
                        }
                        newSpineSize = chunkSize(i);
                        this.spine[i] = newArray(newSpineSize);
                        this.priorElementCount[i] = this.priorElementCount[i - 1] + ((long) arrayLength(this.spine[i - 1]));
                        capacity += (long) newSpineSize;
                    } else {
                        return;
                    }
                }
            }
        }

        protected void increaseCapacity() {
            ensureCapacity(capacity() + 1);
        }

        protected int chunkFor(long index) {
            int j = 0;
            if (this.spineIndex == 0) {
                if (index < ((long) this.elementIndex)) {
                    return 0;
                }
                throw new IndexOutOfBoundsException(Long.toString(index));
            } else if (index < count()) {
                while (true) {
                    int j2 = j;
                    if (j2 > this.spineIndex) {
                        throw new IndexOutOfBoundsException(Long.toString(index));
                    } else if (index < this.priorElementCount[j2] + ((long) arrayLength(this.spine[j2]))) {
                        return j2;
                    } else {
                        j = j2 + 1;
                    }
                }
            } else {
                throw new IndexOutOfBoundsException(Long.toString(index));
            }
        }

        public void copyInto(T_ARR array, int offset) {
            long finalOffset = ((long) offset) + count();
            if (finalOffset > ((long) arrayLength(array)) || finalOffset < ((long) offset)) {
                throw new IndexOutOfBoundsException("does not fit");
            } else if (this.spineIndex == 0) {
                System.arraycopy(this.curChunk, 0, (Object) array, offset, this.elementIndex);
            } else {
                int offset2 = offset;
                for (offset = 0; offset < this.spineIndex; offset++) {
                    System.arraycopy(this.spine[offset], 0, (Object) array, offset2, arrayLength(this.spine[offset]));
                    offset2 += arrayLength(this.spine[offset]);
                }
                if (this.elementIndex > 0) {
                    System.arraycopy(this.curChunk, 0, (Object) array, offset2, this.elementIndex);
                }
            }
        }

        public T_ARR asPrimitiveArray() {
            long size = count();
            if (size < 2147483639) {
                T_ARR result = newArray((int) size);
                copyInto(result, 0);
                return result;
            }
            throw new IllegalArgumentException("Stream size exceeds max array size");
        }

        protected void preAccept() {
            if (this.elementIndex == arrayLength(this.curChunk)) {
                inflateSpine();
                if (this.spineIndex + 1 >= this.spine.length || this.spine[this.spineIndex + 1] == null) {
                    increaseCapacity();
                }
                this.elementIndex = 0;
                this.spineIndex++;
                this.curChunk = this.spine[this.spineIndex];
            }
        }

        public void clear() {
            if (this.spine != null) {
                this.curChunk = this.spine[0];
                this.spine = null;
                this.priorElementCount = null;
            }
            this.elementIndex = 0;
            this.spineIndex = 0;
        }

        public void forEach(T_CONS consumer) {
            for (int j = 0; j < this.spineIndex; j++) {
                arrayForEach(this.spine[j], 0, arrayLength(this.spine[j]), consumer);
            }
            arrayForEach(this.curChunk, 0, this.elementIndex, consumer);
        }
    }

    public static class OfDouble extends OfPrimitive<Double, double[], DoubleConsumer> implements DoubleConsumer {
        public OfDouble(int initialCapacity) {
            super(initialCapacity);
        }

        public void forEach(Consumer<? super Double> consumer) {
            if (consumer instanceof DoubleConsumer) {
                forEach((Object) (DoubleConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling SpinedBuffer.OfDouble.forEach(Consumer)");
            }
            spliterator().forEachRemaining((Consumer) consumer);
        }

        protected double[][] newArrayArray(int size) {
            return new double[size][];
        }

        public double[] newArray(int size) {
            return new double[size];
        }

        protected int arrayLength(double[] array) {
            return array.length;
        }

        protected void arrayForEach(double[] array, int from, int to, DoubleConsumer consumer) {
            for (int i = from; i < to; i++) {
                consumer.accept(array[i]);
            }
        }

        public void accept(double i) {
            preAccept();
            double[] dArr = (double[]) this.curChunk;
            int i2 = this.elementIndex;
            this.elementIndex = i2 + 1;
            dArr[i2] = i;
        }

        public double get(long index) {
            int ch = chunkFor(index);
            if (this.spineIndex == 0 && ch == 0) {
                return ((double[]) this.curChunk)[(int) index];
            }
            return ((double[][]) this.spine)[ch][(int) (index - this.priorElementCount[ch])];
        }

        public java.util.PrimitiveIterator.OfDouble iterator() {
            return Spliterators.iterator(spliterator());
        }

        public java.util.Spliterator.OfDouble spliterator() {
            return new java.util.Spliterator.OfDouble(0, this.spineIndex, 0, this.elementIndex) {
                public /* bridge */ /* synthetic */ void forEachRemaining(DoubleConsumer doubleConsumer) {
                    super.forEachRemaining(doubleConsumer);
                }

                public /* bridge */ /* synthetic */ boolean tryAdvance(DoubleConsumer doubleConsumer) {
                    return super.tryAdvance(doubleConsumer);
                }

                public /* bridge */ /* synthetic */ java.util.Spliterator.OfDouble trySplit() {
                    return (java.util.Spliterator.OfDouble) super.trySplit();
                }

                AnonymousClass1Splitr newSpliterator(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                    return /* anonymous class already generated */;
                }

                void arrayForOne(double[] array, int index, DoubleConsumer consumer) {
                    consumer.accept(array[index]);
                }

                java.util.Spliterator.OfDouble arraySpliterator(double[] array, int offset, int len) {
                    return Arrays.spliterator(array, offset, offset + len);
                }
            };
        }

        public String toString() {
            double[] array = (double[]) asPrimitiveArray();
            if (array.length < HttpURLConnection.HTTP_OK) {
                return String.format("%s[length=%d, chunks=%d]%s", getClass().getSimpleName(), Integer.valueOf(array.length), Integer.valueOf(this.spineIndex), Arrays.toString(array));
            }
            double[] array2 = Arrays.copyOf(array, (int) HttpURLConnection.HTTP_OK);
            return String.format("%s[length=%d, chunks=%d]%s...", getClass().getSimpleName(), Integer.valueOf(array.length), Integer.valueOf(this.spineIndex), Arrays.toString(array2));
        }
    }

    public static class OfInt extends OfPrimitive<Integer, int[], IntConsumer> implements IntConsumer {
        public OfInt(int initialCapacity) {
            super(initialCapacity);
        }

        public void forEach(Consumer<? super Integer> consumer) {
            if (consumer instanceof IntConsumer) {
                forEach((Object) (IntConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling SpinedBuffer.OfInt.forEach(Consumer)");
            }
            spliterator().forEachRemaining((Consumer) consumer);
        }

        protected int[][] newArrayArray(int size) {
            return new int[size][];
        }

        public int[] newArray(int size) {
            return new int[size];
        }

        protected int arrayLength(int[] array) {
            return array.length;
        }

        protected void arrayForEach(int[] array, int from, int to, IntConsumer consumer) {
            for (int i = from; i < to; i++) {
                consumer.accept(array[i]);
            }
        }

        public void accept(int i) {
            preAccept();
            int[] iArr = (int[]) this.curChunk;
            int i2 = this.elementIndex;
            this.elementIndex = i2 + 1;
            iArr[i2] = i;
        }

        public int get(long index) {
            int ch = chunkFor(index);
            if (this.spineIndex == 0 && ch == 0) {
                return ((int[]) this.curChunk)[(int) index];
            }
            return ((int[][]) this.spine)[ch][(int) (index - this.priorElementCount[ch])];
        }

        public java.util.PrimitiveIterator.OfInt iterator() {
            return Spliterators.iterator(spliterator());
        }

        public java.util.Spliterator.OfInt spliterator() {
            return new java.util.Spliterator.OfInt(0, this.spineIndex, 0, this.elementIndex) {
                public /* bridge */ /* synthetic */ void forEachRemaining(IntConsumer intConsumer) {
                    super.forEachRemaining(intConsumer);
                }

                public /* bridge */ /* synthetic */ boolean tryAdvance(IntConsumer intConsumer) {
                    return super.tryAdvance(intConsumer);
                }

                public /* bridge */ /* synthetic */ java.util.Spliterator.OfInt trySplit() {
                    return (java.util.Spliterator.OfInt) super.trySplit();
                }

                AnonymousClass1Splitr newSpliterator(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                    return /* anonymous class already generated */;
                }

                void arrayForOne(int[] array, int index, IntConsumer consumer) {
                    consumer.accept(array[index]);
                }

                java.util.Spliterator.OfInt arraySpliterator(int[] array, int offset, int len) {
                    return Arrays.spliterator(array, offset, offset + len);
                }
            };
        }

        public String toString() {
            int[] array = (int[]) asPrimitiveArray();
            if (array.length < HttpURLConnection.HTTP_OK) {
                return String.format("%s[length=%d, chunks=%d]%s", getClass().getSimpleName(), Integer.valueOf(array.length), Integer.valueOf(this.spineIndex), Arrays.toString(array));
            }
            int[] array2 = Arrays.copyOf(array, (int) HttpURLConnection.HTTP_OK);
            return String.format("%s[length=%d, chunks=%d]%s...", getClass().getSimpleName(), Integer.valueOf(array.length), Integer.valueOf(this.spineIndex), Arrays.toString(array2));
        }
    }

    public static class OfLong extends OfPrimitive<Long, long[], LongConsumer> implements LongConsumer {
        public OfLong(int initialCapacity) {
            super(initialCapacity);
        }

        public void forEach(Consumer<? super Long> consumer) {
            if (consumer instanceof LongConsumer) {
                forEach((Object) (LongConsumer) consumer);
                return;
            }
            if (Tripwire.ENABLED) {
                Tripwire.trip(getClass(), "{0} calling SpinedBuffer.OfLong.forEach(Consumer)");
            }
            spliterator().forEachRemaining((Consumer) consumer);
        }

        protected long[][] newArrayArray(int size) {
            return new long[size][];
        }

        public long[] newArray(int size) {
            return new long[size];
        }

        protected int arrayLength(long[] array) {
            return array.length;
        }

        protected void arrayForEach(long[] array, int from, int to, LongConsumer consumer) {
            for (int i = from; i < to; i++) {
                consumer.accept(array[i]);
            }
        }

        public void accept(long i) {
            preAccept();
            long[] jArr = (long[]) this.curChunk;
            int i2 = this.elementIndex;
            this.elementIndex = i2 + 1;
            jArr[i2] = i;
        }

        public long get(long index) {
            int ch = chunkFor(index);
            if (this.spineIndex == 0 && ch == 0) {
                return ((long[]) this.curChunk)[(int) index];
            }
            return ((long[][]) this.spine)[ch][(int) (index - this.priorElementCount[ch])];
        }

        public java.util.PrimitiveIterator.OfLong iterator() {
            return Spliterators.iterator(spliterator());
        }

        public java.util.Spliterator.OfLong spliterator() {
            return new java.util.Spliterator.OfLong(0, this.spineIndex, 0, this.elementIndex) {
                public /* bridge */ /* synthetic */ void forEachRemaining(LongConsumer longConsumer) {
                    super.forEachRemaining(longConsumer);
                }

                public /* bridge */ /* synthetic */ boolean tryAdvance(LongConsumer longConsumer) {
                    return super.tryAdvance(longConsumer);
                }

                public /* bridge */ /* synthetic */ java.util.Spliterator.OfLong trySplit() {
                    return (java.util.Spliterator.OfLong) super.trySplit();
                }

                AnonymousClass1Splitr newSpliterator(int firstSpineIndex, int lastSpineIndex, int firstSpineElementIndex, int lastSpineElementFence) {
                    return /* anonymous class already generated */;
                }

                void arrayForOne(long[] array, int index, LongConsumer consumer) {
                    consumer.accept(array[index]);
                }

                java.util.Spliterator.OfLong arraySpliterator(long[] array, int offset, int len) {
                    return Arrays.spliterator(array, offset, offset + len);
                }
            };
        }

        public String toString() {
            long[] array = (long[]) asPrimitiveArray();
            if (array.length < HttpURLConnection.HTTP_OK) {
                return String.format("%s[length=%d, chunks=%d]%s", getClass().getSimpleName(), Integer.valueOf(array.length), Integer.valueOf(this.spineIndex), Arrays.toString(array));
            }
            long[] array2 = Arrays.copyOf(array, (int) HttpURLConnection.HTTP_OK);
            return String.format("%s[length=%d, chunks=%d]%s...", getClass().getSimpleName(), Integer.valueOf(array.length), Integer.valueOf(this.spineIndex), Arrays.toString(array2));
        }
    }

    public /* bridge */ /* synthetic */ long count() {
        return super.count();
    }

    public /* bridge */ /* synthetic */ boolean isEmpty() {
        return super.isEmpty();
    }

    public SpinedBuffer(int initialCapacity) {
        super(initialCapacity);
        this.curChunk = new Object[(1 << this.initialChunkPower)];
    }

    public SpinedBuffer() {
        this.curChunk = new Object[(1 << this.initialChunkPower)];
    }

    protected long capacity() {
        if (this.spineIndex == 0) {
            return (long) this.curChunk.length;
        }
        return this.priorElementCount[this.spineIndex] + ((long) this.spine[this.spineIndex].length);
    }

    private void inflateSpine() {
        if (this.spine == null) {
            this.spine = new Object[8][];
            this.priorElementCount = new long[8];
            this.spine[0] = this.curChunk;
        }
    }

    protected final void ensureCapacity(long targetSize) {
        long capacity = capacity();
        if (targetSize > capacity) {
            inflateSpine();
            int i = this.spineIndex;
            while (true) {
                i++;
                if (targetSize > capacity) {
                    int newSpineSize;
                    if (i >= this.spine.length) {
                        newSpineSize = this.spine.length * 2;
                        this.spine = (Object[][]) Arrays.copyOf(this.spine, newSpineSize);
                        this.priorElementCount = Arrays.copyOf(this.priorElementCount, newSpineSize);
                    }
                    newSpineSize = chunkSize(i);
                    this.spine[i] = new Object[newSpineSize];
                    this.priorElementCount[i] = this.priorElementCount[i - 1] + ((long) this.spine[i - 1].length);
                    capacity += (long) newSpineSize;
                } else {
                    return;
                }
            }
        }
    }

    protected void increaseCapacity() {
        ensureCapacity(capacity() + 1);
    }

    public E get(long index) {
        if (this.spineIndex == 0) {
            if (index < ((long) this.elementIndex)) {
                return this.curChunk[(int) index];
            }
            throw new IndexOutOfBoundsException(Long.toString(index));
        } else if (index < count()) {
            for (int j = 0; j <= this.spineIndex; j++) {
                if (index < this.priorElementCount[j] + ((long) this.spine[j].length)) {
                    return this.spine[j][(int) (index - this.priorElementCount[j])];
                }
            }
            throw new IndexOutOfBoundsException(Long.toString(index));
        } else {
            throw new IndexOutOfBoundsException(Long.toString(index));
        }
    }

    public void copyInto(E[] array, int offset) {
        long finalOffset = ((long) offset) + count();
        if (finalOffset > ((long) array.length) || finalOffset < ((long) offset)) {
            throw new IndexOutOfBoundsException("does not fit");
        } else if (this.spineIndex == 0) {
            System.arraycopy(this.curChunk, 0, (Object) array, offset, this.elementIndex);
        } else {
            int offset2 = offset;
            for (offset = 0; offset < this.spineIndex; offset++) {
                System.arraycopy(this.spine[offset], 0, (Object) array, offset2, this.spine[offset].length);
                offset2 += this.spine[offset].length;
            }
            if (this.elementIndex > 0) {
                System.arraycopy(this.curChunk, 0, (Object) array, offset2, this.elementIndex);
            }
        }
    }

    public E[] asArray(IntFunction<E[]> arrayFactory) {
        long size = count();
        if (size < 2147483639) {
            Object[] result = (Object[]) arrayFactory.apply((int) size);
            copyInto(result, 0);
            return result;
        }
        throw new IllegalArgumentException("Stream size exceeds max array size");
    }

    public void clear() {
        int i;
        if (this.spine != null) {
            this.curChunk = this.spine[0];
            for (i = 0; i < this.curChunk.length; i++) {
                this.curChunk[i] = null;
            }
            this.spine = null;
            this.priorElementCount = null;
        } else {
            for (i = 0; i < this.elementIndex; i++) {
                this.curChunk[i] = null;
            }
        }
        this.elementIndex = 0;
        this.spineIndex = 0;
    }

    public Iterator<E> iterator() {
        return Spliterators.iterator(spliterator());
    }

    public void forEach(Consumer<? super E> consumer) {
        int i = 0;
        for (int j = 0; j < this.spineIndex; j++) {
            for (E t : this.spine[j]) {
                consumer.accept(t);
            }
        }
        while (i < this.elementIndex) {
            consumer.accept(this.curChunk[i]);
            i++;
        }
    }

    public void accept(E e) {
        if (this.elementIndex == this.curChunk.length) {
            inflateSpine();
            if (this.spineIndex + 1 >= this.spine.length || this.spine[this.spineIndex + 1] == null) {
                increaseCapacity();
            }
            this.elementIndex = 0;
            this.spineIndex++;
            this.curChunk = this.spine[this.spineIndex];
        }
        Object[] objArr = this.curChunk;
        int i = this.elementIndex;
        this.elementIndex = i + 1;
        objArr[i] = e;
    }

    public String toString() {
        List<E> list = new ArrayList();
        Objects.requireNonNull(list);
        forEach(new -$$Lambda$KCZEjYEMhxUzZZN6W26atKG9NZc(list));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SpinedBuffer:");
        stringBuilder.append(list.toString());
        return stringBuilder.toString();
    }

    public Spliterator<E> spliterator() {
        return new AnonymousClass1Splitr(0, this.spineIndex, 0, this.elementIndex);
    }
}
