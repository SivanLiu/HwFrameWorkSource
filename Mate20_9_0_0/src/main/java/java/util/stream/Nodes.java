package java.util.stream;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CountedCompleter;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.stream.Node.Builder;
import java.util.stream.Node.OfDouble;
import java.util.stream.Node.OfInt;
import java.util.stream.Node.OfLong;

final class Nodes {
    static final String BAD_SIZE = "Stream size exceeds max array size";
    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    private static final OfDouble EMPTY_DOUBLE_NODE = new OfDouble();
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final OfInt EMPTY_INT_NODE = new OfInt();
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final OfLong EMPTY_LONG_NODE = new OfLong();
    private static final Node EMPTY_NODE = new OfRef();
    static final long MAX_ARRAY_SIZE = 2147483639;

    private static abstract class AbstractConcNode<T, T_NODE extends Node<T>> implements Node<T> {
        protected final T_NODE left;
        protected final T_NODE right;
        private final long size;

        AbstractConcNode(T_NODE left, T_NODE right) {
            this.left = left;
            this.right = right;
            this.size = left.count() + right.count();
        }

        public int getChildCount() {
            return 2;
        }

        public T_NODE getChild(int i) {
            if (i == 0) {
                return this.left;
            }
            if (i == 1) {
                return this.right;
            }
            throw new IndexOutOfBoundsException();
        }

        public long count() {
            return this.size;
        }
    }

    private static class ArrayNode<T> implements Node<T> {
        final T[] array;
        int curSize;

        ArrayNode(long size, IntFunction<T[]> generator) {
            if (size < Nodes.MAX_ARRAY_SIZE) {
                this.array = (Object[]) generator.apply((int) size);
                this.curSize = 0;
                return;
            }
            throw new IllegalArgumentException(Nodes.BAD_SIZE);
        }

        ArrayNode(T[] array) {
            this.array = array;
            this.curSize = array.length;
        }

        public Spliterator<T> spliterator() {
            return Arrays.spliterator(this.array, 0, this.curSize);
        }

        public void copyInto(T[] dest, int destOffset) {
            System.arraycopy(this.array, 0, (Object) dest, destOffset, this.curSize);
        }

        public T[] asArray(IntFunction<T[]> intFunction) {
            if (this.array.length == this.curSize) {
                return this.array;
            }
            throw new IllegalStateException();
        }

        public long count() {
            return (long) this.curSize;
        }

        public void forEach(Consumer<? super T> consumer) {
            for (int i = 0; i < this.curSize; i++) {
                consumer.accept(this.array[i]);
            }
        }

        public String toString() {
            return String.format("ArrayNode[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class CollectionNode<T> implements Node<T> {
        private final Collection<T> c;

        CollectionNode(Collection<T> c) {
            this.c = c;
        }

        public Spliterator<T> spliterator() {
            return this.c.stream().spliterator();
        }

        public void copyInto(T[] array, int offset) {
            for (T t : this.c) {
                int offset2 = offset + 1;
                array[offset] = t;
                offset = offset2;
            }
        }

        public T[] asArray(IntFunction<T[]> generator) {
            return this.c.toArray((Object[]) generator.apply(this.c.size()));
        }

        public long count() {
            return (long) this.c.size();
        }

        public void forEach(Consumer<? super T> consumer) {
            this.c.forEach(consumer);
        }

        public String toString() {
            return String.format("CollectionNode[%d][%s]", Integer.valueOf(this.c.size()), this.c);
        }
    }

    private static abstract class EmptyNode<T, T_ARR, T_CONS> implements Node<T> {

        private static class OfRef<T> extends EmptyNode<T, T[], Consumer<? super T>> {
            public /* bridge */ /* synthetic */ void copyInto(Object[] objArr, int i) {
                super.copyInto(objArr, i);
            }

            public /* bridge */ /* synthetic */ void forEach(Consumer consumer) {
                super.forEach(consumer);
            }

            private OfRef() {
            }

            public Spliterator<T> spliterator() {
                return Spliterators.emptySpliterator();
            }
        }

        private static final class OfDouble extends EmptyNode<Double, double[], DoubleConsumer> implements java.util.stream.Node.OfDouble {
            OfDouble() {
            }

            public java.util.Spliterator.OfDouble spliterator() {
                return Spliterators.emptyDoubleSpliterator();
            }

            public double[] asPrimitiveArray() {
                return Nodes.EMPTY_DOUBLE_ARRAY;
            }
        }

        private static final class OfInt extends EmptyNode<Integer, int[], IntConsumer> implements java.util.stream.Node.OfInt {
            OfInt() {
            }

            public java.util.Spliterator.OfInt spliterator() {
                return Spliterators.emptyIntSpliterator();
            }

            public int[] asPrimitiveArray() {
                return Nodes.EMPTY_INT_ARRAY;
            }
        }

        private static final class OfLong extends EmptyNode<Long, long[], LongConsumer> implements java.util.stream.Node.OfLong {
            OfLong() {
            }

            public java.util.Spliterator.OfLong spliterator() {
                return Spliterators.emptyLongSpliterator();
            }

            public long[] asPrimitiveArray() {
                return Nodes.EMPTY_LONG_ARRAY;
            }
        }

        EmptyNode() {
        }

        public T[] asArray(IntFunction<T[]> generator) {
            return (Object[]) generator.apply(0);
        }

        public void copyInto(T_ARR t_arr, int offset) {
        }

        public long count() {
            return 0;
        }

        public void forEach(T_CONS t_cons) {
        }
    }

    private static abstract class InternalNodeSpliterator<T, S extends Spliterator<T>, N extends Node<T>> implements Spliterator<T> {
        int curChildIndex;
        N curNode;
        S lastNodeSpliterator;
        S tryAdvanceSpliterator;
        Deque<N> tryAdvanceStack;

        private static abstract class OfPrimitive<T, T_CONS, T_ARR, T_SPLITR extends java.util.Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, N extends java.util.stream.Node.OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, N>> extends InternalNodeSpliterator<T, T_SPLITR, N> implements java.util.Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            public /* bridge */ /* synthetic */ java.util.Spliterator.OfPrimitive trySplit() {
                return (java.util.Spliterator.OfPrimitive) super.trySplit();
            }

            OfPrimitive(N cur) {
                super(cur);
            }

            public boolean tryAdvance(T_CONS consumer) {
                if (!initTryAdvance()) {
                    return false;
                }
                boolean hasNext = ((java.util.Spliterator.OfPrimitive) this.tryAdvanceSpliterator).tryAdvance(consumer);
                if (!hasNext) {
                    if (this.lastNodeSpliterator == null) {
                        java.util.stream.Node.OfPrimitive leaf = (java.util.stream.Node.OfPrimitive) findNextLeafNode(this.tryAdvanceStack);
                        if (leaf != null) {
                            this.tryAdvanceSpliterator = leaf.spliterator();
                            return ((java.util.Spliterator.OfPrimitive) this.tryAdvanceSpliterator).tryAdvance(consumer);
                        }
                    }
                    this.curNode = null;
                }
                return hasNext;
            }

            public void forEachRemaining(T_CONS consumer) {
                if (this.curNode != null) {
                    if (this.tryAdvanceSpliterator != null) {
                        while (tryAdvance(consumer)) {
                        }
                    } else if (this.lastNodeSpliterator == null) {
                        Deque<N> stack = initStack();
                        while (true) {
                            N n = (java.util.stream.Node.OfPrimitive) findNextLeafNode(stack);
                            N leaf = n;
                            if (n == null) {
                                break;
                            }
                            leaf.forEach(consumer);
                        }
                        this.curNode = null;
                    } else {
                        ((java.util.Spliterator.OfPrimitive) this.lastNodeSpliterator).forEachRemaining(consumer);
                    }
                }
            }
        }

        private static final class OfRef<T> extends InternalNodeSpliterator<T, Spliterator<T>, Node<T>> {
            OfRef(Node<T> curNode) {
                super(curNode);
            }

            public boolean tryAdvance(Consumer<? super T> consumer) {
                if (!initTryAdvance()) {
                    return false;
                }
                boolean hasNext = this.tryAdvanceSpliterator.tryAdvance(consumer);
                if (!hasNext) {
                    if (this.lastNodeSpliterator == null) {
                        Node<T> leaf = findNextLeafNode(this.tryAdvanceStack);
                        if (leaf != null) {
                            this.tryAdvanceSpliterator = leaf.spliterator();
                            return this.tryAdvanceSpliterator.tryAdvance(consumer);
                        }
                    }
                    this.curNode = null;
                }
                return hasNext;
            }

            public void forEachRemaining(Consumer<? super T> consumer) {
                if (this.curNode != null) {
                    if (this.tryAdvanceSpliterator != null) {
                        while (tryAdvance(consumer)) {
                        }
                    } else if (this.lastNodeSpliterator == null) {
                        Deque<Node<T>> stack = initStack();
                        while (true) {
                            Node<T> findNextLeafNode = findNextLeafNode(stack);
                            Node<T> leaf = findNextLeafNode;
                            if (findNextLeafNode == null) {
                                break;
                            }
                            leaf.forEach(consumer);
                        }
                        this.curNode = null;
                    } else {
                        this.lastNodeSpliterator.forEachRemaining(consumer);
                    }
                }
            }
        }

        private static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, double[], java.util.Spliterator.OfDouble, java.util.stream.Node.OfDouble> implements java.util.Spliterator.OfDouble {
            public /* bridge */ /* synthetic */ void forEachRemaining(DoubleConsumer doubleConsumer) {
                super.forEachRemaining(doubleConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(DoubleConsumer doubleConsumer) {
                return super.tryAdvance(doubleConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfDouble trySplit() {
                return (java.util.Spliterator.OfDouble) super.trySplit();
            }

            OfDouble(java.util.stream.Node.OfDouble cur) {
                super(cur);
            }
        }

        private static final class OfInt extends OfPrimitive<Integer, IntConsumer, int[], java.util.Spliterator.OfInt, java.util.stream.Node.OfInt> implements java.util.Spliterator.OfInt {
            public /* bridge */ /* synthetic */ void forEachRemaining(IntConsumer intConsumer) {
                super.forEachRemaining(intConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(IntConsumer intConsumer) {
                return super.tryAdvance(intConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfInt trySplit() {
                return (java.util.Spliterator.OfInt) super.trySplit();
            }

            OfInt(java.util.stream.Node.OfInt cur) {
                super(cur);
            }
        }

        private static final class OfLong extends OfPrimitive<Long, LongConsumer, long[], java.util.Spliterator.OfLong, java.util.stream.Node.OfLong> implements java.util.Spliterator.OfLong {
            public /* bridge */ /* synthetic */ void forEachRemaining(LongConsumer longConsumer) {
                super.forEachRemaining(longConsumer);
            }

            public /* bridge */ /* synthetic */ boolean tryAdvance(LongConsumer longConsumer) {
                return super.tryAdvance(longConsumer);
            }

            public /* bridge */ /* synthetic */ java.util.Spliterator.OfLong trySplit() {
                return (java.util.Spliterator.OfLong) super.trySplit();
            }

            OfLong(java.util.stream.Node.OfLong cur) {
                super(cur);
            }
        }

        InternalNodeSpliterator(N curNode) {
            this.curNode = curNode;
        }

        protected final Deque<N> initStack() {
            Deque<N> stack = new ArrayDeque(8);
            int i = this.curNode.getChildCount();
            while (true) {
                i--;
                if (i < this.curChildIndex) {
                    return stack;
                }
                stack.addFirst(this.curNode.getChild(i));
            }
        }

        protected final N findNextLeafNode(Deque<N> stack) {
            N n = null;
            while (true) {
                N n2 = (Node) stack.pollFirst();
                n = n2;
                if (n2 == null) {
                    return null;
                }
                if (n.getChildCount() != 0) {
                    for (int i = n.getChildCount() - 1; i >= 0; i--) {
                        stack.addFirst(n.getChild(i));
                    }
                } else if (n.count() > 0) {
                    return n;
                }
            }
        }

        protected final boolean initTryAdvance() {
            if (this.curNode == null) {
                return false;
            }
            if (this.tryAdvanceSpliterator == null) {
                if (this.lastNodeSpliterator == null) {
                    this.tryAdvanceStack = initStack();
                    N leaf = findNextLeafNode(this.tryAdvanceStack);
                    if (leaf != null) {
                        this.tryAdvanceSpliterator = leaf.spliterator();
                    } else {
                        this.curNode = null;
                        return false;
                    }
                }
                this.tryAdvanceSpliterator = this.lastNodeSpliterator;
            }
            return true;
        }

        public final S trySplit() {
            if (this.curNode == null || this.tryAdvanceSpliterator != null) {
                return null;
            }
            if (this.lastNodeSpliterator != null) {
                return this.lastNodeSpliterator.trySplit();
            }
            Node node;
            int i;
            if (this.curChildIndex < this.curNode.getChildCount() - 1) {
                node = this.curNode;
                i = this.curChildIndex;
                this.curChildIndex = i + 1;
                return node.getChild(i).spliterator();
            }
            this.curNode = this.curNode.getChild(this.curChildIndex);
            if (this.curNode.getChildCount() == 0) {
                this.lastNodeSpliterator = this.curNode.spliterator();
                return this.lastNodeSpliterator.trySplit();
            }
            this.curChildIndex = 0;
            node = this.curNode;
            i = this.curChildIndex;
            this.curChildIndex = i + 1;
            return node.getChild(i).spliterator();
        }

        public final long estimateSize() {
            if (this.curNode == null) {
                return 0;
            }
            if (this.lastNodeSpliterator != null) {
                return this.lastNodeSpliterator.estimateSize();
            }
            long size = 0;
            for (int i = this.curChildIndex; i < this.curNode.getChildCount(); i++) {
                size += this.curNode.getChild(i).count();
            }
            return size;
        }

        public final int characteristics() {
            return 64;
        }
    }

    static final class ConcNode<T> extends AbstractConcNode<T, Node<T>> implements Node<T> {

        private static abstract class OfPrimitive<E, T_CONS, T_ARR, T_SPLITR extends java.util.Spliterator.OfPrimitive<E, T_CONS, T_SPLITR>, T_NODE extends java.util.stream.Node.OfPrimitive<E, T_CONS, T_ARR, T_SPLITR, T_NODE>> extends AbstractConcNode<E, T_NODE> implements java.util.stream.Node.OfPrimitive<E, T_CONS, T_ARR, T_SPLITR, T_NODE> {
            public /* bridge */ /* synthetic */ java.util.stream.Node.OfPrimitive getChild(int i) {
                return (java.util.stream.Node.OfPrimitive) super.getChild(i);
            }

            OfPrimitive(T_NODE left, T_NODE right) {
                super(left, right);
            }

            public void forEach(T_CONS consumer) {
                ((java.util.stream.Node.OfPrimitive) this.left).forEach(consumer);
                ((java.util.stream.Node.OfPrimitive) this.right).forEach(consumer);
            }

            public void copyInto(T_ARR array, int offset) {
                ((java.util.stream.Node.OfPrimitive) this.left).copyInto(array, offset);
                ((java.util.stream.Node.OfPrimitive) this.right).copyInto(array, ((int) ((java.util.stream.Node.OfPrimitive) this.left).count()) + offset);
            }

            public T_ARR asPrimitiveArray() {
                long size = count();
                if (size < Nodes.MAX_ARRAY_SIZE) {
                    T_ARR array = newArray((int) size);
                    copyInto(array, 0);
                    return array;
                }
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }

            public String toString() {
                if (count() < 32) {
                    return String.format("%s[%s.%s]", getClass().getName(), this.left, this.right);
                }
                return String.format("%s[size=%d]", getClass().getName(), Long.valueOf(count()));
            }
        }

        static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, double[], java.util.Spliterator.OfDouble, java.util.stream.Node.OfDouble> implements java.util.stream.Node.OfDouble {
            OfDouble(java.util.stream.Node.OfDouble left, java.util.stream.Node.OfDouble right) {
                super(left, right);
            }

            public java.util.Spliterator.OfDouble spliterator() {
                return new OfDouble(this);
            }
        }

        static final class OfInt extends OfPrimitive<Integer, IntConsumer, int[], java.util.Spliterator.OfInt, java.util.stream.Node.OfInt> implements java.util.stream.Node.OfInt {
            OfInt(java.util.stream.Node.OfInt left, java.util.stream.Node.OfInt right) {
                super(left, right);
            }

            public java.util.Spliterator.OfInt spliterator() {
                return new OfInt(this);
            }
        }

        static final class OfLong extends OfPrimitive<Long, LongConsumer, long[], java.util.Spliterator.OfLong, java.util.stream.Node.OfLong> implements java.util.stream.Node.OfLong {
            OfLong(java.util.stream.Node.OfLong left, java.util.stream.Node.OfLong right) {
                super(left, right);
            }

            public java.util.Spliterator.OfLong spliterator() {
                return new OfLong(this);
            }
        }

        ConcNode(Node<T> left, Node<T> right) {
            super(left, right);
        }

        public Spliterator<T> spliterator() {
            return new OfRef(this);
        }

        public void copyInto(T[] array, int offset) {
            Objects.requireNonNull(array);
            this.left.copyInto(array, offset);
            this.right.copyInto(array, ((int) this.left.count()) + offset);
        }

        public T[] asArray(IntFunction<T[]> generator) {
            long size = count();
            if (size < Nodes.MAX_ARRAY_SIZE) {
                Object[] array = (Object[]) generator.apply((int) size);
                copyInto(array, 0);
                return array;
            }
            throw new IllegalArgumentException(Nodes.BAD_SIZE);
        }

        public void forEach(Consumer<? super T> consumer) {
            this.left.forEach(consumer);
            this.right.forEach(consumer);
        }

        public Node<T> truncate(long from, long to, IntFunction<T[]> generator) {
            if (from == 0 && to == count()) {
                return this;
            }
            long leftCount = this.left.count();
            if (from >= leftCount) {
                return this.right.truncate(from - leftCount, to - leftCount, generator);
            }
            if (to <= leftCount) {
                return this.left.truncate(from, to, generator);
            }
            return Nodes.conc(getShape(), this.left.truncate(from, leftCount, generator), this.right.truncate(0, to - leftCount, generator));
        }

        public String toString() {
            if (count() < 32) {
                return String.format("ConcNode[%s.%s]", this.left, this.right);
            }
            return String.format("ConcNode[size=%d]", Long.valueOf(count()));
        }
    }

    private static class DoubleArrayNode implements OfDouble {
        final double[] array;
        int curSize;

        DoubleArrayNode(long size) {
            if (size < Nodes.MAX_ARRAY_SIZE) {
                this.array = new double[((int) size)];
                this.curSize = 0;
                return;
            }
            throw new IllegalArgumentException(Nodes.BAD_SIZE);
        }

        DoubleArrayNode(double[] array) {
            this.array = array;
            this.curSize = array.length;
        }

        public Spliterator.OfDouble spliterator() {
            return Arrays.spliterator(this.array, 0, this.curSize);
        }

        public double[] asPrimitiveArray() {
            if (this.array.length == this.curSize) {
                return this.array;
            }
            return Arrays.copyOf(this.array, this.curSize);
        }

        public void copyInto(double[] dest, int destOffset) {
            System.arraycopy(this.array, 0, (Object) dest, destOffset, this.curSize);
        }

        public long count() {
            return (long) this.curSize;
        }

        public void forEach(DoubleConsumer consumer) {
            for (int i = 0; i < this.curSize; i++) {
                consumer.accept(this.array[i]);
            }
        }

        public String toString() {
            return String.format("DoubleArrayNode[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class FixedNodeBuilder<T> extends ArrayNode<T> implements Builder<T> {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = Nodes.class;
        }

        FixedNodeBuilder(long size, IntFunction<T[]> generator) {
            super(size, generator);
        }

        public Node<T> build() {
            if (this.curSize >= this.array.length) {
                return this;
            }
            throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
        }

        public void begin(long size) {
            if (size == ((long) this.array.length)) {
                this.curSize = 0;
            } else {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", Long.valueOf(size), Integer.valueOf(this.array.length)));
            }
        }

        public void accept(T t) {
            if (this.curSize < this.array.length) {
                Object[] objArr = this.array;
                int i = this.curSize;
                this.curSize = i + 1;
                objArr[i] = t;
                return;
            }
            throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", Integer.valueOf(this.array.length)));
        }

        public void end() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
        }

        public String toString() {
            return String.format("FixedNodeBuilder[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static class IntArrayNode implements OfInt {
        final int[] array;
        int curSize;

        IntArrayNode(long size) {
            if (size < Nodes.MAX_ARRAY_SIZE) {
                this.array = new int[((int) size)];
                this.curSize = 0;
                return;
            }
            throw new IllegalArgumentException(Nodes.BAD_SIZE);
        }

        IntArrayNode(int[] array) {
            this.array = array;
            this.curSize = array.length;
        }

        public Spliterator.OfInt spliterator() {
            return Arrays.spliterator(this.array, 0, this.curSize);
        }

        public int[] asPrimitiveArray() {
            if (this.array.length == this.curSize) {
                return this.array;
            }
            return Arrays.copyOf(this.array, this.curSize);
        }

        public void copyInto(int[] dest, int destOffset) {
            System.arraycopy(this.array, 0, (Object) dest, destOffset, this.curSize);
        }

        public long count() {
            return (long) this.curSize;
        }

        public void forEach(IntConsumer consumer) {
            for (int i = 0; i < this.curSize; i++) {
                consumer.accept(this.array[i]);
            }
        }

        public String toString() {
            return String.format("IntArrayNode[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static class LongArrayNode implements OfLong {
        final long[] array;
        int curSize;

        LongArrayNode(long size) {
            if (size < Nodes.MAX_ARRAY_SIZE) {
                this.array = new long[((int) size)];
                this.curSize = 0;
                return;
            }
            throw new IllegalArgumentException(Nodes.BAD_SIZE);
        }

        LongArrayNode(long[] array) {
            this.array = array;
            this.curSize = array.length;
        }

        public Spliterator.OfLong spliterator() {
            return Arrays.spliterator(this.array, 0, this.curSize);
        }

        public long[] asPrimitiveArray() {
            if (this.array.length == this.curSize) {
                return this.array;
            }
            return Arrays.copyOf(this.array, this.curSize);
        }

        public void copyInto(long[] dest, int destOffset) {
            System.arraycopy(this.array, 0, (Object) dest, destOffset, this.curSize);
        }

        public long count() {
            return (long) this.curSize;
        }

        public void forEach(LongConsumer consumer) {
            for (int i = 0; i < this.curSize; i++) {
                consumer.accept(this.array[i]);
            }
        }

        public String toString() {
            return String.format("LongArrayNode[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static abstract class SizedCollectorTask<P_IN, P_OUT, T_SINK extends Sink<P_OUT>, K extends SizedCollectorTask<P_IN, P_OUT, T_SINK, K>> extends CountedCompleter<Void> implements Sink<P_OUT> {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        protected int fence;
        protected final PipelineHelper<P_OUT> helper;
        protected int index;
        protected long length;
        protected long offset;
        protected final Spliterator<P_IN> spliterator;
        protected final long targetSize;

        static final class OfDouble<P_IN> extends SizedCollectorTask<P_IN, Double, java.util.stream.Sink.OfDouble, OfDouble<P_IN>> implements java.util.stream.Sink.OfDouble {
            private final double[] array;

            OfDouble(Spliterator<P_IN> spliterator, PipelineHelper<Double> helper, double[] array) {
                super(spliterator, helper, array.length);
                this.array = array;
            }

            OfDouble(OfDouble<P_IN> parent, Spliterator<P_IN> spliterator, long offset, long length) {
                super(parent, spliterator, offset, length, parent.array.length);
                this.array = parent.array;
            }

            OfDouble<P_IN> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new OfDouble(this, spliterator, offset, size);
            }

            public void accept(double value) {
                if (this.index < this.fence) {
                    double[] dArr = this.array;
                    int i = this.index;
                    this.index = i + 1;
                    dArr[i] = value;
                    return;
                }
                throw new IndexOutOfBoundsException(Integer.toString(this.index));
            }
        }

        static final class OfInt<P_IN> extends SizedCollectorTask<P_IN, Integer, java.util.stream.Sink.OfInt, OfInt<P_IN>> implements java.util.stream.Sink.OfInt {
            private final int[] array;

            OfInt(Spliterator<P_IN> spliterator, PipelineHelper<Integer> helper, int[] array) {
                super(spliterator, helper, array.length);
                this.array = array;
            }

            OfInt(OfInt<P_IN> parent, Spliterator<P_IN> spliterator, long offset, long length) {
                super(parent, spliterator, offset, length, parent.array.length);
                this.array = parent.array;
            }

            OfInt<P_IN> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new OfInt(this, spliterator, offset, size);
            }

            public void accept(int value) {
                if (this.index < this.fence) {
                    int[] iArr = this.array;
                    int i = this.index;
                    this.index = i + 1;
                    iArr[i] = value;
                    return;
                }
                throw new IndexOutOfBoundsException(Integer.toString(this.index));
            }
        }

        static final class OfLong<P_IN> extends SizedCollectorTask<P_IN, Long, java.util.stream.Sink.OfLong, OfLong<P_IN>> implements java.util.stream.Sink.OfLong {
            private final long[] array;

            OfLong(Spliterator<P_IN> spliterator, PipelineHelper<Long> helper, long[] array) {
                super(spliterator, helper, array.length);
                this.array = array;
            }

            OfLong(OfLong<P_IN> parent, Spliterator<P_IN> spliterator, long offset, long length) {
                super(parent, spliterator, offset, length, parent.array.length);
                this.array = parent.array;
            }

            OfLong<P_IN> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new OfLong(this, spliterator, offset, size);
            }

            public void accept(long value) {
                if (this.index < this.fence) {
                    long[] jArr = this.array;
                    int i = this.index;
                    this.index = i + 1;
                    jArr[i] = value;
                    return;
                }
                throw new IndexOutOfBoundsException(Integer.toString(this.index));
            }
        }

        static final class OfRef<P_IN, P_OUT> extends SizedCollectorTask<P_IN, P_OUT, Sink<P_OUT>, OfRef<P_IN, P_OUT>> implements Sink<P_OUT> {
            private final P_OUT[] array;

            OfRef(Spliterator<P_IN> spliterator, PipelineHelper<P_OUT> helper, P_OUT[] array) {
                super(spliterator, helper, array.length);
                this.array = array;
            }

            OfRef(OfRef<P_IN, P_OUT> parent, Spliterator<P_IN> spliterator, long offset, long length) {
                super(parent, spliterator, offset, length, parent.array.length);
                this.array = parent.array;
            }

            OfRef<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator, long offset, long size) {
                return new OfRef(this, spliterator, offset, size);
            }

            public void accept(P_OUT value) {
                if (this.index < this.fence) {
                    Object[] objArr = this.array;
                    int i = this.index;
                    this.index = i + 1;
                    objArr[i] = value;
                    return;
                }
                throw new IndexOutOfBoundsException(Integer.toString(this.index));
            }
        }

        abstract K makeChild(Spliterator<P_IN> spliterator, long j, long j2);

        static {
            Class cls = Nodes.class;
        }

        SizedCollectorTask(Spliterator<P_IN> spliterator, PipelineHelper<P_OUT> helper, int arrayLength) {
            this.spliterator = spliterator;
            this.helper = helper;
            this.targetSize = AbstractTask.suggestTargetSize(spliterator.estimateSize());
            this.offset = 0;
            this.length = (long) arrayLength;
        }

        SizedCollectorTask(K parent, Spliterator<P_IN> spliterator, long offset, long length, int arrayLength) {
            super(parent);
            this.spliterator = spliterator;
            this.helper = parent.helper;
            this.targetSize = parent.targetSize;
            this.offset = offset;
            this.length = length;
            if (offset < 0 || length < 0 || (offset + length) - 1 >= ((long) arrayLength)) {
                throw new IllegalArgumentException(String.format("offset and length interval [%d, %d + %d) is not within array size interval [0, %d)", Long.valueOf(offset), Long.valueOf(offset), Long.valueOf(length), Integer.valueOf(arrayLength)));
            }
        }

        public void compute() {
            T_SINK task = this;
            Spliterator<P_IN> rightSplit = this.spliterator;
            while (rightSplit.estimateSize() > task.targetSize) {
                Spliterator<P_IN> trySplit = rightSplit.trySplit();
                Spliterator<P_IN> leftSplit = trySplit;
                if (trySplit == null) {
                    break;
                }
                task.setPendingCount(1);
                long leftSplitSize = leftSplit.estimateSize();
                task.makeChild(leftSplit, task.offset, leftSplitSize).fork();
                task = task.makeChild(rightSplit, task.offset + leftSplitSize, task.length - leftSplitSize);
            }
            task.helper.wrapAndCopyInto(task, rightSplit);
            task.propagateCompletion();
        }

        public void begin(long size) {
            if (size <= this.length) {
                this.index = (int) this.offset;
                this.fence = this.index + ((int) this.length);
                return;
            }
            throw new IllegalStateException("size passed to Sink.begin exceeds array length");
        }
    }

    private static final class SpinedNodeBuilder<T> extends SpinedBuffer<T> implements Node<T>, Builder<T> {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private boolean building = false;

        static {
            Class cls = Nodes.class;
        }

        SpinedNodeBuilder() {
        }

        public Spliterator<T> spliterator() {
            return super.spliterator();
        }

        public void forEach(Consumer<? super T> consumer) {
            super.forEach(consumer);
        }

        public void begin(long size) {
            this.building = true;
            clear();
            ensureCapacity(size);
        }

        public void accept(T t) {
            super.accept(t);
        }

        public void end() {
            this.building = false;
        }

        public void copyInto(T[] array, int offset) {
            super.copyInto(array, offset);
        }

        public T[] asArray(IntFunction<T[]> arrayFactory) {
            return super.asArray(arrayFactory);
        }

        public Node<T> build() {
            return this;
        }
    }

    private static abstract class ToArrayTask<T, T_NODE extends Node<T>, K extends ToArrayTask<T, T_NODE, K>> extends CountedCompleter<Void> {
        protected final T_NODE node;
        protected final int offset;

        private static class OfPrimitive<T, T_CONS, T_ARR, T_SPLITR extends java.util.Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_NODE extends java.util.stream.Node.OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE>> extends ToArrayTask<T, T_NODE, OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE>> {
            private final T_ARR array;

            private OfPrimitive(T_NODE node, T_ARR array, int offset) {
                super(node, offset);
                this.array = array;
            }

            private OfPrimitive(OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE> parent, T_NODE node, int offset) {
                super(parent, node, offset);
                this.array = parent.array;
            }

            OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE> makeChild(int childIndex, int offset) {
                return new OfPrimitive(this, ((java.util.stream.Node.OfPrimitive) this.node).getChild(childIndex), offset);
            }

            void copyNodeToArray() {
                ((java.util.stream.Node.OfPrimitive) this.node).copyInto(this.array, this.offset);
            }
        }

        private static final class OfRef<T> extends ToArrayTask<T, Node<T>, OfRef<T>> {
            private final T[] array;

            private OfRef(Node<T> node, T[] array, int offset) {
                super(node, offset);
                this.array = array;
            }

            private OfRef(OfRef<T> parent, Node<T> node, int offset) {
                super(parent, node, offset);
                this.array = parent.array;
            }

            OfRef<T> makeChild(int childIndex, int offset) {
                return new OfRef(this, this.node.getChild(childIndex), offset);
            }

            void copyNodeToArray() {
                this.node.copyInto(this.array, this.offset);
            }
        }

        private static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, double[], java.util.Spliterator.OfDouble, java.util.stream.Node.OfDouble> {
            private OfDouble(java.util.stream.Node.OfDouble node, double[] array, int offset) {
                super(node, array, offset);
            }
        }

        private static final class OfInt extends OfPrimitive<Integer, IntConsumer, int[], java.util.Spliterator.OfInt, java.util.stream.Node.OfInt> {
            private OfInt(java.util.stream.Node.OfInt node, int[] array, int offset) {
                super(node, array, offset);
            }
        }

        private static final class OfLong extends OfPrimitive<Long, LongConsumer, long[], java.util.Spliterator.OfLong, java.util.stream.Node.OfLong> {
            private OfLong(java.util.stream.Node.OfLong node, long[] array, int offset) {
                super(node, array, offset);
            }
        }

        abstract void copyNodeToArray();

        abstract K makeChild(int i, int i2);

        ToArrayTask(T_NODE node, int offset) {
            this.node = node;
            this.offset = offset;
        }

        ToArrayTask(K parent, T_NODE node, int offset) {
            super(parent);
            this.node = node;
            this.offset = offset;
        }

        public void compute() {
            ToArrayTask<T, T_NODE, K> task = this;
            while (task.node.getChildCount() != 0) {
                task.setPendingCount(task.node.getChildCount() - 1);
                int size = 0;
                int i = 0;
                while (i < task.node.getChildCount() - 1) {
                    K leftTask = task.makeChild(i, task.offset + size);
                    size = (int) (((long) size) + leftTask.node.count());
                    leftTask.fork();
                    i++;
                }
                task = task.makeChild(i, task.offset + size);
            }
            task.copyNodeToArray();
            task.propagateCompletion();
        }
    }

    private static class CollectorTask<P_IN, P_OUT, T_NODE extends Node<P_OUT>, T_BUILDER extends Builder<P_OUT>> extends AbstractTask<P_IN, P_OUT, T_NODE, CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER>> {
        protected final LongFunction<T_BUILDER> builderFactory;
        protected final BinaryOperator<T_NODE> concFactory;
        protected final PipelineHelper<P_OUT> helper;

        private static final class OfDouble<P_IN> extends CollectorTask<P_IN, Double, java.util.stream.Node.OfDouble, java.util.stream.Node.Builder.OfDouble> {
            OfDouble(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, -$$Lambda$LfPL0444L8HcP6gPtdKqQiCTSfM.INSTANCE, -$$Lambda$KTexUmxMdHIv08L4oU8j9HXK_go.INSTANCE);
            }
        }

        private static final class OfInt<P_IN> extends CollectorTask<P_IN, Integer, java.util.stream.Node.OfInt, java.util.stream.Node.Builder.OfInt> {
            OfInt(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, -$$Lambda$B6rBjxAejI5kqKK9J3AHwY_L9ag.INSTANCE, -$$Lambda$O4iFzVwtlyKFZkWcnfXHIHbxaTY.INSTANCE);
            }
        }

        private static final class OfLong<P_IN> extends CollectorTask<P_IN, Long, java.util.stream.Node.OfLong, java.util.stream.Node.Builder.OfLong> {
            OfLong(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, -$$Lambda$8ABiL5PN53c8rr14_yI2_4o5Zlo.INSTANCE, -$$Lambda$eeRvX3cGN3C3qCAoKtOxCHIW8Lo.INSTANCE);
            }
        }

        private static final class OfRef<P_IN, P_OUT> extends CollectorTask<P_IN, P_OUT, Node<P_OUT>, Builder<P_OUT>> {
            OfRef(PipelineHelper<P_OUT> helper, IntFunction<P_OUT[]> generator, Spliterator<P_IN> spliterator) {
                super(helper, spliterator, new -$$Lambda$Nodes$CollectorTask$OfRef$Zd2fdoB-mZW0DbPHybIpYjf-Pyo(generator), -$$Lambda$Mo9-ryI3XUGyoHfpnRL3BoFhaqY.INSTANCE);
            }
        }

        CollectorTask(PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, LongFunction<T_BUILDER> builderFactory, BinaryOperator<T_NODE> concFactory) {
            super((PipelineHelper) helper, (Spliterator) spliterator);
            this.helper = helper;
            this.builderFactory = builderFactory;
            this.concFactory = concFactory;
        }

        CollectorTask(CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER> parent, Spliterator<P_IN> spliterator) {
            super((AbstractTask) parent, (Spliterator) spliterator);
            this.helper = parent.helper;
            this.builderFactory = parent.builderFactory;
            this.concFactory = parent.concFactory;
        }

        protected CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER> makeChild(Spliterator<P_IN> spliterator) {
            return new CollectorTask(this, spliterator);
        }

        protected T_NODE doLeaf() {
            return ((Builder) this.helper.wrapAndCopyInto((Builder) this.builderFactory.apply(this.helper.exactOutputSizeIfKnown(this.spliterator)), this.spliterator)).build();
        }

        public void onCompletion(CountedCompleter<?> caller) {
            if (!isLeaf()) {
                setLocalResult((Node) this.concFactory.apply((Node) ((CollectorTask) this.leftChild).getLocalResult(), (Node) ((CollectorTask) this.rightChild).getLocalResult()));
            }
            super.onCompletion(caller);
        }
    }

    private static final class DoubleFixedNodeBuilder extends DoubleArrayNode implements Builder.OfDouble {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = Nodes.class;
        }

        DoubleFixedNodeBuilder(long size) {
            super(size);
        }

        public OfDouble build() {
            if (this.curSize >= this.array.length) {
                return this;
            }
            throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
        }

        public void begin(long size) {
            if (size == ((long) this.array.length)) {
                this.curSize = 0;
            } else {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", Long.valueOf(size), Integer.valueOf(this.array.length)));
            }
        }

        public void accept(double i) {
            if (this.curSize < this.array.length) {
                double[] dArr = this.array;
                int i2 = this.curSize;
                this.curSize = i2 + 1;
                dArr[i2] = i;
                return;
            }
            throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", Integer.valueOf(this.array.length)));
        }

        public void end() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
        }

        public String toString() {
            return String.format("DoubleFixedNodeBuilder[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class DoubleSpinedNodeBuilder extends SpinedBuffer.OfDouble implements OfDouble, Builder.OfDouble {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private boolean building = false;

        static {
            Class cls = Nodes.class;
        }

        DoubleSpinedNodeBuilder() {
        }

        public Spliterator.OfDouble spliterator() {
            return super.spliterator();
        }

        public void forEach(DoubleConsumer consumer) {
            super.forEach((Object) consumer);
        }

        public void begin(long size) {
            this.building = true;
            clear();
            ensureCapacity(size);
        }

        public void accept(double i) {
            super.accept(i);
        }

        public void end() {
            this.building = false;
        }

        public void copyInto(double[] array, int offset) {
            super.copyInto(array, offset);
        }

        public double[] asPrimitiveArray() {
            return (double[]) super.asPrimitiveArray();
        }

        public OfDouble build() {
            return this;
        }
    }

    private static final class IntFixedNodeBuilder extends IntArrayNode implements Builder.OfInt {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = Nodes.class;
        }

        IntFixedNodeBuilder(long size) {
            super(size);
        }

        public OfInt build() {
            if (this.curSize >= this.array.length) {
                return this;
            }
            throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
        }

        public void begin(long size) {
            if (size == ((long) this.array.length)) {
                this.curSize = 0;
            } else {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", Long.valueOf(size), Integer.valueOf(this.array.length)));
            }
        }

        public void accept(int i) {
            if (this.curSize < this.array.length) {
                int[] iArr = this.array;
                int i2 = this.curSize;
                this.curSize = i2 + 1;
                iArr[i2] = i;
                return;
            }
            throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", Integer.valueOf(this.array.length)));
        }

        public void end() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
        }

        public String toString() {
            return String.format("IntFixedNodeBuilder[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class IntSpinedNodeBuilder extends SpinedBuffer.OfInt implements OfInt, Builder.OfInt {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private boolean building = false;

        static {
            Class cls = Nodes.class;
        }

        IntSpinedNodeBuilder() {
        }

        public Spliterator.OfInt spliterator() {
            return super.spliterator();
        }

        public void forEach(IntConsumer consumer) {
            super.forEach((Object) consumer);
        }

        public void begin(long size) {
            this.building = true;
            clear();
            ensureCapacity(size);
        }

        public void accept(int i) {
            super.accept(i);
        }

        public void end() {
            this.building = false;
        }

        public void copyInto(int[] array, int offset) throws IndexOutOfBoundsException {
            super.copyInto(array, offset);
        }

        public int[] asPrimitiveArray() {
            return (int[]) super.asPrimitiveArray();
        }

        public OfInt build() {
            return this;
        }
    }

    private static final class LongFixedNodeBuilder extends LongArrayNode implements Builder.OfLong {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = Nodes.class;
        }

        LongFixedNodeBuilder(long size) {
            super(size);
        }

        public OfLong build() {
            if (this.curSize >= this.array.length) {
                return this;
            }
            throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
        }

        public void begin(long size) {
            if (size == ((long) this.array.length)) {
                this.curSize = 0;
            } else {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", Long.valueOf(size), Integer.valueOf(this.array.length)));
            }
        }

        public void accept(long i) {
            if (this.curSize < this.array.length) {
                long[] jArr = this.array;
                int i2 = this.curSize;
                this.curSize = i2 + 1;
                jArr[i2] = i;
                return;
            }
            throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", Integer.valueOf(this.array.length)));
        }

        public void end() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
        }

        public String toString() {
            return String.format("LongFixedNodeBuilder[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class LongSpinedNodeBuilder extends SpinedBuffer.OfLong implements OfLong, Builder.OfLong {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private boolean building = false;

        static {
            Class cls = Nodes.class;
        }

        LongSpinedNodeBuilder() {
        }

        public Spliterator.OfLong spliterator() {
            return super.spliterator();
        }

        public void forEach(LongConsumer consumer) {
            super.forEach((Object) consumer);
        }

        public void begin(long size) {
            this.building = true;
            clear();
            ensureCapacity(size);
        }

        public void accept(long i) {
            super.accept(i);
        }

        public void end() {
            this.building = false;
        }

        public void copyInto(long[] array, int offset) {
            super.copyInto(array, offset);
        }

        public long[] asPrimitiveArray() {
            return (long[]) super.asPrimitiveArray();
        }

        public OfLong build() {
            return this;
        }
    }

    private Nodes() {
        throw new Error("no instances");
    }

    static <T> Node<T> emptyNode(StreamShape shape) {
        switch (shape) {
            case REFERENCE:
                return EMPTY_NODE;
            case INT_VALUE:
                return EMPTY_INT_NODE;
            case LONG_VALUE:
                return EMPTY_LONG_NODE;
            case DOUBLE_VALUE:
                return EMPTY_DOUBLE_NODE;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown shape ");
                stringBuilder.append((Object) shape);
                throw new IllegalStateException(stringBuilder.toString());
        }
    }

    static <T> Node<T> conc(StreamShape shape, Node<T> left, Node<T> right) {
        switch (shape) {
            case REFERENCE:
                return new ConcNode(left, right);
            case INT_VALUE:
                return new OfInt((OfInt) left, (OfInt) right);
            case LONG_VALUE:
                return new OfLong((OfLong) left, (OfLong) right);
            case DOUBLE_VALUE:
                return new OfDouble((OfDouble) left, (OfDouble) right);
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown shape ");
                stringBuilder.append((Object) shape);
                throw new IllegalStateException(stringBuilder.toString());
        }
    }

    static <T> Node<T> node(T[] array) {
        return new ArrayNode(array);
    }

    static <T> Node<T> node(Collection<T> c) {
        return new CollectionNode(c);
    }

    static <T> Builder<T> builder(long exactSizeIfKnown, IntFunction<T[]> generator) {
        if (exactSizeIfKnown < 0 || exactSizeIfKnown >= MAX_ARRAY_SIZE) {
            return builder();
        }
        return new FixedNodeBuilder(exactSizeIfKnown, generator);
    }

    static <T> Builder<T> builder() {
        return new SpinedNodeBuilder();
    }

    static OfInt node(int[] array) {
        return new IntArrayNode(array);
    }

    static Builder.OfInt intBuilder(long exactSizeIfKnown) {
        if (exactSizeIfKnown < 0 || exactSizeIfKnown >= MAX_ARRAY_SIZE) {
            return intBuilder();
        }
        return new IntFixedNodeBuilder(exactSizeIfKnown);
    }

    static Builder.OfInt intBuilder() {
        return new IntSpinedNodeBuilder();
    }

    static OfLong node(long[] array) {
        return new LongArrayNode(array);
    }

    static Builder.OfLong longBuilder(long exactSizeIfKnown) {
        if (exactSizeIfKnown < 0 || exactSizeIfKnown >= MAX_ARRAY_SIZE) {
            return longBuilder();
        }
        return new LongFixedNodeBuilder(exactSizeIfKnown);
    }

    static Builder.OfLong longBuilder() {
        return new LongSpinedNodeBuilder();
    }

    static OfDouble node(double[] array) {
        return new DoubleArrayNode(array);
    }

    static Builder.OfDouble doubleBuilder(long exactSizeIfKnown) {
        if (exactSizeIfKnown < 0 || exactSizeIfKnown >= MAX_ARRAY_SIZE) {
            return doubleBuilder();
        }
        return new DoubleFixedNodeBuilder(exactSizeIfKnown);
    }

    static Builder.OfDouble doubleBuilder() {
        return new DoubleSpinedNodeBuilder();
    }

    public static <P_IN, P_OUT> Node<P_OUT> collect(PipelineHelper<P_OUT> helper, Spliterator<P_IN> spliterator, boolean flattenTree, IntFunction<P_OUT[]> generator) {
        long size = helper.exactOutputSizeIfKnown(spliterator);
        if (size < 0 || !spliterator.hasCharacteristics(16384)) {
            Node<P_OUT> node = (Node) new OfRef(helper, generator, spliterator).invoke();
            return flattenTree ? flatten(node, generator) : node;
        } else if (size < MAX_ARRAY_SIZE) {
            Object[] array = (Object[]) generator.apply((int) size);
            new OfRef(spliterator, helper, array).invoke();
            return node(array);
        } else {
            throw new IllegalArgumentException(BAD_SIZE);
        }
    }

    public static <P_IN> OfInt collectInt(PipelineHelper<Integer> helper, Spliterator<P_IN> spliterator, boolean flattenTree) {
        long size = helper.exactOutputSizeIfKnown(spliterator);
        if (size < 0 || !spliterator.hasCharacteristics(16384)) {
            OfInt node = (OfInt) new OfInt(helper, spliterator).invoke();
            return flattenTree ? flattenInt(node) : node;
        } else if (size < MAX_ARRAY_SIZE) {
            int[] array = new int[((int) size)];
            new OfInt(spliterator, helper, array).invoke();
            return node(array);
        } else {
            throw new IllegalArgumentException(BAD_SIZE);
        }
    }

    public static <P_IN> OfLong collectLong(PipelineHelper<Long> helper, Spliterator<P_IN> spliterator, boolean flattenTree) {
        long size = helper.exactOutputSizeIfKnown(spliterator);
        if (size < 0 || !spliterator.hasCharacteristics(16384)) {
            OfLong node = (OfLong) new OfLong(helper, spliterator).invoke();
            return flattenTree ? flattenLong(node) : node;
        } else if (size < MAX_ARRAY_SIZE) {
            long[] array = new long[((int) size)];
            new OfLong(spliterator, helper, array).invoke();
            return node(array);
        } else {
            throw new IllegalArgumentException(BAD_SIZE);
        }
    }

    public static <P_IN> OfDouble collectDouble(PipelineHelper<Double> helper, Spliterator<P_IN> spliterator, boolean flattenTree) {
        long size = helper.exactOutputSizeIfKnown(spliterator);
        if (size < 0 || !spliterator.hasCharacteristics(16384)) {
            OfDouble node = (OfDouble) new OfDouble(helper, spliterator).invoke();
            return flattenTree ? flattenDouble(node) : node;
        } else if (size < MAX_ARRAY_SIZE) {
            double[] array = new double[((int) size)];
            new OfDouble(spliterator, helper, array).invoke();
            return node(array);
        } else {
            throw new IllegalArgumentException(BAD_SIZE);
        }
    }

    public static <T> Node<T> flatten(Node<T> node, IntFunction<T[]> generator) {
        if (node.getChildCount() <= 0) {
            return node;
        }
        long size = node.count();
        if (size < MAX_ARRAY_SIZE) {
            Object[] array = (Object[]) generator.apply((int) size);
            new OfRef(node, array, 0).invoke();
            return node(array);
        }
        throw new IllegalArgumentException(BAD_SIZE);
    }

    public static OfInt flattenInt(OfInt node) {
        if (node.getChildCount() <= 0) {
            return node;
        }
        long size = node.count();
        if (size < MAX_ARRAY_SIZE) {
            int[] array = new int[((int) size)];
            new OfInt(node, array, 0).invoke();
            return node(array);
        }
        throw new IllegalArgumentException(BAD_SIZE);
    }

    public static OfLong flattenLong(OfLong node) {
        if (node.getChildCount() <= 0) {
            return node;
        }
        long size = node.count();
        if (size < MAX_ARRAY_SIZE) {
            long[] array = new long[((int) size)];
            new OfLong(node, array, 0).invoke();
            return node(array);
        }
        throw new IllegalArgumentException(BAD_SIZE);
    }

    public static OfDouble flattenDouble(OfDouble node) {
        if (node.getChildCount() <= 0) {
            return node;
        }
        long size = node.count();
        if (size < MAX_ARRAY_SIZE) {
            double[] array = new double[((int) size)];
            new OfDouble(node, array, 0).invoke();
            return node(array);
        }
        throw new IllegalArgumentException(BAD_SIZE);
    }
}
