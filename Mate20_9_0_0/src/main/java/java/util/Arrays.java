package java.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Spliterator.OfDouble;
import java.util.Spliterator.OfInt;
import java.util.Spliterator.OfLong;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Arrays {
    private static final int INSERTIONSORT_THRESHOLD = 7;
    public static final int MIN_ARRAY_SORT_GRAN = 8192;

    static final class NaturalOrder implements Comparator<Object> {
        static final NaturalOrder INSTANCE = new NaturalOrder();

        NaturalOrder() {
        }

        public int compare(Object first, Object second) {
            return ((Comparable) first).compareTo(second);
        }
    }

    private static class ArrayList<E> extends AbstractList<E> implements RandomAccess, Serializable {
        private static final long serialVersionUID = -2764017481108945198L;
        private final E[] a;

        ArrayList(E[] array) {
            this.a = (Object[]) Objects.requireNonNull(array);
        }

        public int size() {
            return this.a.length;
        }

        public Object[] toArray() {
            return (Object[]) this.a.clone();
        }

        public <T> T[] toArray(T[] a) {
            int size = size();
            if (a.length < size) {
                return Arrays.copyOf(this.a, size, a.getClass());
            }
            System.arraycopy(this.a, 0, (Object) a, 0, size);
            if (a.length > size) {
                a[size] = null;
            }
            return a;
        }

        public E get(int index) {
            return this.a[index];
        }

        public E set(int index, E element) {
            E oldValue = this.a[index];
            this.a[index] = element;
            return oldValue;
        }

        public int indexOf(Object o) {
            E[] a = this.a;
            int i = 0;
            if (o == null) {
                while (i < a.length) {
                    if (a[i] == null) {
                        return i;
                    }
                    i++;
                }
            } else {
                while (i < a.length) {
                    if (o.equals(a[i])) {
                        return i;
                    }
                    i++;
                }
            }
            return -1;
        }

        public boolean contains(Object o) {
            return indexOf(o) != -1;
        }

        public Spliterator<E> spliterator() {
            return Spliterators.spliterator(this.a, 16);
        }

        public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            for (E e : this.a) {
                action.accept(e);
            }
        }

        public void replaceAll(UnaryOperator<E> operator) {
            Objects.requireNonNull(operator);
            E[] a = this.a;
            for (int i = 0; i < a.length; i++) {
                a[i] = operator.apply(a[i]);
            }
        }

        public void sort(Comparator<? super E> c) {
            Arrays.sort(this.a, c);
        }
    }

    private Arrays() {
    }

    private static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fromIndex(");
            stringBuilder.append(fromIndex);
            stringBuilder.append(") > toIndex(");
            stringBuilder.append(toIndex);
            stringBuilder.append(")");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        } else if (toIndex > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }

    public static void checkOffsetAndCount(int arrayLength, int offset, int count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException(arrayLength, offset, count);
        }
    }

    public static void sort(int[] a) {
        DualPivotQuicksort.sort(a, 0, a.length - 1, null, 0, 0);
    }

    public static void sort(int[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex - 1, null, 0, 0);
    }

    public static void sort(long[] a) {
        DualPivotQuicksort.sort(a, 0, a.length - 1, null, 0, 0);
    }

    public static void sort(long[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex - 1, null, 0, 0);
    }

    public static void sort(short[] a) {
        DualPivotQuicksort.sort(a, 0, a.length - 1, null, 0, 0);
    }

    public static void sort(short[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex - 1, null, 0, 0);
    }

    public static void sort(char[] a) {
        DualPivotQuicksort.sort(a, 0, a.length - 1, null, 0, 0);
    }

    public static void sort(char[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex - 1, null, 0, 0);
    }

    public static void sort(byte[] a) {
        DualPivotQuicksort.sort(a, 0, a.length - 1);
    }

    public static void sort(byte[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex - 1);
    }

    public static void sort(float[] a) {
        DualPivotQuicksort.sort(a, 0, a.length - 1, null, 0, 0);
    }

    public static void sort(float[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex - 1, null, 0, 0);
    }

    public static void sort(double[] a) {
        DualPivotQuicksort.sort(a, 0, a.length - 1, null, 0, 0);
    }

    public static void sort(double[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex - 1, null, 0, 0);
    }

    public static void parallelSort(byte[] a) {
        int n = a.length;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i = n / (p << 2);
                new Sorter(null, a, new byte[n], 0, n, 0, i <= 8192 ? 8192 : i).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(a, 0, n - 1);
    }

    public static void parallelSort(byte[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        int n = toIndex - fromIndex;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i = n / (p << 2);
                new Sorter(null, a, new byte[n], fromIndex, n, 0, i <= 8192 ? 8192 : i).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(a, fromIndex, toIndex - 1);
    }

    public static void parallelSort(char[] a) {
        int n = a.length;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i = n / (p << 2);
                new Sorter(null, a, new char[n], 0, n, 0, i <= 8192 ? 8192 : i).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(a, 0, n - 1, null, 0, 0);
    }

    public static void parallelSort(char[] a, int fromIndex, int toIndex) {
        int i = fromIndex;
        int i2 = toIndex;
        char[] cArr = a;
        rangeCheck(cArr.length, i, i2);
        int n = i2 - i;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i3 = n / (p << 2);
                new Sorter(null, cArr, new char[n], i, n, 0, i3 <= 8192 ? 8192 : i3).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(cArr, i, i2 - 1, null, 0, 0);
    }

    public static void parallelSort(short[] a) {
        int n = a.length;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i = n / (p << 2);
                new Sorter(null, a, new short[n], 0, n, 0, i <= 8192 ? 8192 : i).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(a, 0, n - 1, null, 0, 0);
    }

    public static void parallelSort(short[] a, int fromIndex, int toIndex) {
        int i = fromIndex;
        int i2 = toIndex;
        short[] sArr = a;
        rangeCheck(sArr.length, i, i2);
        int n = i2 - i;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i3 = n / (p << 2);
                new Sorter(null, sArr, new short[n], i, n, 0, i3 <= 8192 ? 8192 : i3).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(sArr, i, i2 - 1, null, 0, 0);
    }

    public static void parallelSort(int[] a) {
        int n = a.length;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i = n / (p << 2);
                new Sorter(null, a, new int[n], 0, n, 0, i <= 8192 ? 8192 : i).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(a, 0, n - 1, null, 0, 0);
    }

    public static void parallelSort(int[] a, int fromIndex, int toIndex) {
        int i = fromIndex;
        int i2 = toIndex;
        int[] iArr = a;
        rangeCheck(iArr.length, i, i2);
        int n = i2 - i;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i3 = n / (p << 2);
                new Sorter(null, iArr, new int[n], i, n, 0, i3 <= 8192 ? 8192 : i3).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(iArr, i, i2 - 1, null, 0, 0);
    }

    public static void parallelSort(long[] a) {
        int n = a.length;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i = n / (p << 2);
                new Sorter(null, a, new long[n], 0, n, 0, i <= 8192 ? 8192 : i).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(a, 0, n - 1, null, 0, 0);
    }

    public static void parallelSort(long[] a, int fromIndex, int toIndex) {
        int i = fromIndex;
        int i2 = toIndex;
        long[] jArr = a;
        rangeCheck(jArr.length, i, i2);
        int n = i2 - i;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i3 = n / (p << 2);
                new Sorter(null, jArr, new long[n], i, n, 0, i3 <= 8192 ? 8192 : i3).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(jArr, i, i2 - 1, null, 0, 0);
    }

    public static void parallelSort(float[] a) {
        int n = a.length;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i = n / (p << 2);
                new Sorter(null, a, new float[n], 0, n, 0, i <= 8192 ? 8192 : i).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(a, 0, n - 1, null, 0, 0);
    }

    public static void parallelSort(float[] a, int fromIndex, int toIndex) {
        int i = fromIndex;
        int i2 = toIndex;
        float[] fArr = a;
        rangeCheck(fArr.length, i, i2);
        int n = i2 - i;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i3 = n / (p << 2);
                new Sorter(null, fArr, new float[n], i, n, 0, i3 <= 8192 ? 8192 : i3).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(fArr, i, i2 - 1, null, 0, 0);
    }

    public static void parallelSort(double[] a) {
        int n = a.length;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i = n / (p << 2);
                new Sorter(null, a, new double[n], 0, n, 0, i <= 8192 ? 8192 : i).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(a, 0, n - 1, null, 0, 0);
    }

    public static void parallelSort(double[] a, int fromIndex, int toIndex) {
        int i = fromIndex;
        int i2 = toIndex;
        double[] dArr = a;
        rangeCheck(dArr.length, i, i2);
        int n = i2 - i;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i3 = n / (p << 2);
                new Sorter(null, dArr, new double[n], i, n, 0, i3 <= 8192 ? 8192 : i3).invoke();
                return;
            }
        }
        DualPivotQuicksort.sort(dArr, i, i2 - 1, null, 0, 0);
    }

    public static <T extends Comparable<? super T>> void parallelSort(T[] a) {
        int n = a.length;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i = n / (p << 2);
                new Sorter(null, a, (Comparable[]) Array.newInstance(a.getClass().getComponentType(), n), 0, n, 0, i <= 8192 ? 8192 : i, NaturalOrder.INSTANCE).invoke();
                return;
            }
        }
        TimSort.sort(a, 0, n, NaturalOrder.INSTANCE, null, 0, 0);
    }

    public static <T extends Comparable<? super T>> void parallelSort(T[] a, int fromIndex, int toIndex) {
        int i = fromIndex;
        int i2 = toIndex;
        T[] tArr = a;
        rangeCheck(tArr.length, i, i2);
        int n = i2 - i;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i3 = n / (p << 2);
                new Sorter(null, tArr, (Comparable[]) Array.newInstance(a.getClass().getComponentType(), n), i, n, 0, i3 <= 8192 ? 8192 : i3, NaturalOrder.INSTANCE).invoke();
                return;
            }
        }
        TimSort.sort(tArr, i, i2, NaturalOrder.INSTANCE, null, 0, 0);
    }

    public static <T> void parallelSort(T[] a, Comparator<? super T> cmp) {
        if (cmp == null) {
            cmp = NaturalOrder.INSTANCE;
        }
        int n = a.length;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i = n / (p << 2);
                new Sorter(null, a, (Object[]) Array.newInstance(a.getClass().getComponentType(), n), 0, n, 0, i <= 8192 ? 8192 : i, cmp).invoke();
                return;
            }
        }
        TimSort.sort(a, 0, n, cmp, null, 0, 0);
    }

    public static <T> void parallelSort(T[] a, int fromIndex, int toIndex, Comparator<? super T> cmp) {
        Comparator<? super T> cmp2;
        int i = fromIndex;
        int i2 = toIndex;
        T[] tArr = a;
        rangeCheck(tArr.length, i, i2);
        if (cmp == null) {
            cmp2 = NaturalOrder.INSTANCE;
        } else {
            cmp2 = cmp;
        }
        int n = i2 - i;
        if (n > 8192) {
            int commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism();
            int p = commonPoolParallelism;
            if (commonPoolParallelism != 1) {
                int i3 = n / (p << 2);
                new Sorter(null, tArr, (Object[]) Array.newInstance(a.getClass().getComponentType(), n), i, n, 0, i3 <= 8192 ? 8192 : i3, cmp2).invoke();
                return;
            }
        }
        TimSort.sort(tArr, i, i2, cmp2, null, 0, 0);
    }

    public static void sort(Object[] a) {
        ComparableTimSort.sort(a, 0, a.length, null, 0, 0);
    }

    public static void sort(Object[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        ComparableTimSort.sort(a, fromIndex, toIndex, null, 0, 0);
    }

    private static void mergeSort(Object[] src, Object[] dest, int low, int high, int off) {
        int length = high - low;
        int i;
        int j;
        if (length < 7) {
            for (i = low; i < high; i++) {
                j = i;
                while (j > low && ((Comparable) dest[j - 1]).compareTo(dest[j]) > 0) {
                    swap(dest, j, j - 1);
                    j--;
                }
            }
            return;
        }
        i = low;
        j = high;
        low += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off);
        mergeSort(dest, src, mid, high, -off);
        if (((Comparable) src[mid - 1]).compareTo(src[mid]) <= 0) {
            System.arraycopy((Object) src, low, (Object) dest, i, length);
            return;
        }
        int p = low;
        int q = mid;
        for (int i2 = i; i2 < j; i2++) {
            int p2;
            if (q >= high || (p < mid && ((Comparable) src[p]).compareTo(src[q]) <= 0)) {
                p2 = p + 1;
                dest[i2] = src[p];
                p = p2;
            } else {
                p2 = q + 1;
                dest[i2] = src[q];
                q = p2;
            }
        }
    }

    private static void swap(Object[] x, int a, int b) {
        Object t = x[a];
        x[a] = x[b];
        x[b] = t;
    }

    public static <T> void sort(T[] a, Comparator<? super T> c) {
        if (c == null) {
            sort((Object[]) a);
            return;
        }
        TimSort.sort(a, 0, a.length, c, null, 0, 0);
    }

    public static <T> void sort(T[] a, int fromIndex, int toIndex, Comparator<? super T> c) {
        if (c == null) {
            sort((Object[]) a, fromIndex, toIndex);
            return;
        }
        rangeCheck(a.length, fromIndex, toIndex);
        TimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
    }

    public static <T> void parallelPrefix(T[] array, BinaryOperator<T> op) {
        Objects.requireNonNull(op);
        if (array.length > 0) {
            new CumulateTask(null, op, array, 0, array.length).invoke();
        }
    }

    public static <T> void parallelPrefix(T[] array, int fromIndex, int toIndex, BinaryOperator<T> op) {
        Objects.requireNonNull(op);
        rangeCheck(array.length, fromIndex, toIndex);
        if (fromIndex < toIndex) {
            new CumulateTask(null, op, array, fromIndex, toIndex).invoke();
        }
    }

    public static void parallelPrefix(long[] array, LongBinaryOperator op) {
        Objects.requireNonNull(op);
        if (array.length > 0) {
            new LongCumulateTask(null, op, array, 0, array.length).invoke();
        }
    }

    public static void parallelPrefix(long[] array, int fromIndex, int toIndex, LongBinaryOperator op) {
        Objects.requireNonNull(op);
        rangeCheck(array.length, fromIndex, toIndex);
        if (fromIndex < toIndex) {
            new LongCumulateTask(null, op, array, fromIndex, toIndex).invoke();
        }
    }

    public static void parallelPrefix(double[] array, DoubleBinaryOperator op) {
        Objects.requireNonNull(op);
        if (array.length > 0) {
            new DoubleCumulateTask(null, op, array, 0, array.length).invoke();
        }
    }

    public static void parallelPrefix(double[] array, int fromIndex, int toIndex, DoubleBinaryOperator op) {
        Objects.requireNonNull(op);
        rangeCheck(array.length, fromIndex, toIndex);
        if (fromIndex < toIndex) {
            new DoubleCumulateTask(null, op, array, fromIndex, toIndex).invoke();
        }
    }

    public static void parallelPrefix(int[] array, IntBinaryOperator op) {
        Objects.requireNonNull(op);
        if (array.length > 0) {
            new IntCumulateTask(null, op, array, 0, array.length).invoke();
        }
    }

    public static void parallelPrefix(int[] array, int fromIndex, int toIndex, IntBinaryOperator op) {
        Objects.requireNonNull(op);
        rangeCheck(array.length, fromIndex, toIndex);
        if (fromIndex < toIndex) {
            new IntCumulateTask(null, op, array, fromIndex, toIndex).invoke();
        }
    }

    public static int binarySearch(long[] a, long key) {
        return binarySearch0(a, 0, a.length, key);
    }

    public static int binarySearch(long[] a, int fromIndex, int toIndex, long key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    private static int binarySearch0(long[] a, int fromIndex, int toIndex, long key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = a[mid];
            if (midVal < key) {
                low = mid + 1;
            } else if (midVal <= key) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    public static int binarySearch(int[] a, int key) {
        return binarySearch0(a, 0, a.length, key);
    }

    public static int binarySearch(int[] a, int fromIndex, int toIndex, int key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    private static int binarySearch0(int[] a, int fromIndex, int toIndex, int key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid];
            if (midVal < key) {
                low = mid + 1;
            } else if (midVal <= key) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    public static int binarySearch(short[] a, short key) {
        return binarySearch0(a, 0, a.length, key);
    }

    public static int binarySearch(short[] a, int fromIndex, int toIndex, short key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    private static int binarySearch0(short[] a, int fromIndex, int toIndex, short key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            short midVal = a[mid];
            if (midVal < key) {
                low = mid + 1;
            } else if (midVal <= key) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    public static int binarySearch(char[] a, char key) {
        return binarySearch0(a, 0, a.length, key);
    }

    public static int binarySearch(char[] a, int fromIndex, int toIndex, char key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    private static int binarySearch0(char[] a, int fromIndex, int toIndex, char key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            char midVal = a[mid];
            if (midVal < key) {
                low = mid + 1;
            } else if (midVal <= key) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    public static int binarySearch(byte[] a, byte key) {
        return binarySearch0(a, 0, a.length, key);
    }

    public static int binarySearch(byte[] a, int fromIndex, int toIndex, byte key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    private static int binarySearch0(byte[] a, int fromIndex, int toIndex, byte key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            byte midVal = a[mid];
            if (midVal < key) {
                low = mid + 1;
            } else if (midVal <= key) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    public static int binarySearch(double[] a, double key) {
        return binarySearch0(a, 0, a.length, key);
    }

    public static int binarySearch(double[] a, int fromIndex, int toIndex, double key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    private static int binarySearch0(double[] a, int fromIndex, int toIndex, double key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int high2 = (low + high) >>> 1;
            double midVal = a[high2];
            if (midVal < key) {
                low = high2 + 1;
            } else if (midVal > key) {
                high = high2 - 1;
            } else {
                long midBits = Double.doubleToLongBits(midVal);
                long keyBits = Double.doubleToLongBits(key);
                if (midBits == keyBits) {
                    return high2;
                }
                if (midBits < keyBits) {
                    low = high2 + 1;
                } else {
                    high = high2 - 1;
                }
            }
        }
        return -(low + 1);
    }

    public static int binarySearch(float[] a, float key) {
        return binarySearch0(a, 0, a.length, key);
    }

    public static int binarySearch(float[] a, int fromIndex, int toIndex, float key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    private static int binarySearch0(float[] a, int fromIndex, int toIndex, float key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int high2 = (low + high) >>> 1;
            float midVal = a[high2];
            if (midVal < key) {
                low = high2 + 1;
            } else if (midVal > key) {
                high = high2 - 1;
            } else {
                int midBits = Float.floatToIntBits(midVal);
                int keyBits = Float.floatToIntBits(key);
                if (midBits == keyBits) {
                    return high2;
                }
                if (midBits < keyBits) {
                    low = high2 + 1;
                } else {
                    high = high2 - 1;
                }
            }
        }
        return -(low + 1);
    }

    public static int binarySearch(Object[] a, Object key) {
        return binarySearch0(a, 0, a.length, key);
    }

    public static int binarySearch(Object[] a, int fromIndex, int toIndex, Object key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    private static int binarySearch0(Object[] a, int fromIndex, int toIndex, Object key) {
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = a[mid].compareTo(key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp <= 0) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    public static <T> int binarySearch(T[] a, T key, Comparator<? super T> c) {
        return binarySearch0(a, 0, a.length, key, c);
    }

    public static <T> int binarySearch(T[] a, int fromIndex, int toIndex, T key, Comparator<? super T> c) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key, c);
    }

    private static <T> int binarySearch0(T[] a, int fromIndex, int toIndex, T key, Comparator<? super T> c) {
        if (c == null) {
            return binarySearch0((Object[]) a, fromIndex, toIndex, (Object) key);
        }
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = c.compare(a[mid], key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp <= 0) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    public static boolean equals(long[] a, long[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null) {
            return false;
        }
        int length = a.length;
        if (a2.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (a[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(int[] a, int[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null) {
            return false;
        }
        int length = a.length;
        if (a2.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (a[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(short[] a, short[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null) {
            return false;
        }
        int length = a.length;
        if (a2.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (a[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(char[] a, char[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null) {
            return false;
        }
        int length = a.length;
        if (a2.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (a[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(byte[] a, byte[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null) {
            return false;
        }
        int length = a.length;
        if (a2.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (a[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(boolean[] a, boolean[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null) {
            return false;
        }
        int length = a.length;
        if (a2.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (a[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(double[] a, double[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null) {
            return false;
        }
        int length = a.length;
        if (a2.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(a2[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(float[] a, float[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null) {
            return false;
        }
        int length = a.length;
        if (a2.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (Float.floatToIntBits(a[i]) != Float.floatToIntBits(a2[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(Object[] a, Object[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null) {
            return false;
        }
        int length = a.length;
        if (a2.length != length) {
            return false;
        }
        int i = 0;
        while (i < length) {
            Object o1 = a[i];
            Object o2 = a2[i];
            if (o1 == null) {
                if (o2 == null) {
                    i++;
                }
            } else if (o1.equals(o2)) {
                i++;
            }
            return false;
        }
        return true;
    }

    public static void fill(long[] a, long val) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            a[i] = val;
        }
    }

    public static void fill(long[] a, int fromIndex, int toIndex, long val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(int[] a, int val) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            a[i] = val;
        }
    }

    public static void fill(int[] a, int fromIndex, int toIndex, int val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(short[] a, short val) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            a[i] = val;
        }
    }

    public static void fill(short[] a, int fromIndex, int toIndex, short val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(char[] a, char val) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            a[i] = val;
        }
    }

    public static void fill(char[] a, int fromIndex, int toIndex, char val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(byte[] a, byte val) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            a[i] = val;
        }
    }

    public static void fill(byte[] a, int fromIndex, int toIndex, byte val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(boolean[] a, boolean val) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            a[i] = val;
        }
    }

    public static void fill(boolean[] a, int fromIndex, int toIndex, boolean val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(double[] a, double val) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            a[i] = val;
        }
    }

    public static void fill(double[] a, int fromIndex, int toIndex, double val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(float[] a, float val) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            a[i] = val;
        }
    }

    public static void fill(float[] a, int fromIndex, int toIndex, float val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static void fill(Object[] a, Object val) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            a[i] = val;
        }
    }

    public static void fill(Object[] a, int fromIndex, int toIndex, Object val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = val;
        }
    }

    public static <T> T[] copyOf(T[] original, int newLength) {
        return copyOf(original, newLength, original.getClass());
    }

    public static <T, U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        Object copy;
        if (newType == Object[].class) {
            copy = new Object[newLength];
        } else {
            copy = (Object[]) Array.newInstance(newType.getComponentType(), newLength);
        }
        System.arraycopy((Object) original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static byte[] copyOf(byte[] original, int newLength) {
        byte[] copy = new byte[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static short[] copyOf(short[] original, int newLength) {
        Object copy = new short[newLength];
        System.arraycopy((Object) original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static int[] copyOf(int[] original, int newLength) {
        Object copy = new int[newLength];
        System.arraycopy((Object) original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static long[] copyOf(long[] original, int newLength) {
        Object copy = new long[newLength];
        System.arraycopy((Object) original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static char[] copyOf(char[] original, int newLength) {
        Object copy = new char[newLength];
        System.arraycopy((Object) original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static float[] copyOf(float[] original, int newLength) {
        Object copy = new float[newLength];
        System.arraycopy((Object) original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static double[] copyOf(double[] original, int newLength) {
        Object copy = new double[newLength];
        System.arraycopy((Object) original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static boolean[] copyOf(boolean[] original, int newLength) {
        Object copy = new boolean[newLength];
        System.arraycopy((Object) original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    public static <T> T[] copyOfRange(T[] original, int from, int to) {
        return copyOfRange(original, from, to, original.getClass());
    }

    public static <T, U> T[] copyOfRange(U[] original, int from, int to, Class<? extends T[]> newType) {
        int newLength = to - from;
        if (newLength >= 0) {
            Object copy;
            if (newType == Object[].class) {
                copy = new Object[newLength];
            } else {
                copy = (Object[]) Array.newInstance(newType.getComponentType(), newLength);
            }
            System.arraycopy((Object) original, from, copy, 0, Math.min(original.length - from, newLength));
            return copy;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(from);
        stringBuilder.append(" > ");
        stringBuilder.append(to);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength >= 0) {
            byte[] copy = new byte[newLength];
            System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
            return copy;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(from);
        stringBuilder.append(" > ");
        stringBuilder.append(to);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static short[] copyOfRange(short[] original, int from, int to) {
        int newLength = to - from;
        if (newLength >= 0) {
            Object copy = new short[newLength];
            System.arraycopy((Object) original, from, copy, 0, Math.min(original.length - from, newLength));
            return copy;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(from);
        stringBuilder.append(" > ");
        stringBuilder.append(to);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static int[] copyOfRange(int[] original, int from, int to) {
        int newLength = to - from;
        if (newLength >= 0) {
            Object copy = new int[newLength];
            System.arraycopy((Object) original, from, copy, 0, Math.min(original.length - from, newLength));
            return copy;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(from);
        stringBuilder.append(" > ");
        stringBuilder.append(to);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static long[] copyOfRange(long[] original, int from, int to) {
        int newLength = to - from;
        if (newLength >= 0) {
            Object copy = new long[newLength];
            System.arraycopy((Object) original, from, copy, 0, Math.min(original.length - from, newLength));
            return copy;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(from);
        stringBuilder.append(" > ");
        stringBuilder.append(to);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static char[] copyOfRange(char[] original, int from, int to) {
        int newLength = to - from;
        if (newLength >= 0) {
            Object copy = new char[newLength];
            System.arraycopy((Object) original, from, copy, 0, Math.min(original.length - from, newLength));
            return copy;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(from);
        stringBuilder.append(" > ");
        stringBuilder.append(to);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static float[] copyOfRange(float[] original, int from, int to) {
        int newLength = to - from;
        if (newLength >= 0) {
            Object copy = new float[newLength];
            System.arraycopy((Object) original, from, copy, 0, Math.min(original.length - from, newLength));
            return copy;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(from);
        stringBuilder.append(" > ");
        stringBuilder.append(to);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static double[] copyOfRange(double[] original, int from, int to) {
        int newLength = to - from;
        if (newLength >= 0) {
            Object copy = new double[newLength];
            System.arraycopy((Object) original, from, copy, 0, Math.min(original.length - from, newLength));
            return copy;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(from);
        stringBuilder.append(" > ");
        stringBuilder.append(to);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static boolean[] copyOfRange(boolean[] original, int from, int to) {
        int newLength = to - from;
        if (newLength >= 0) {
            Object copy = new boolean[newLength];
            System.arraycopy((Object) original, from, copy, 0, Math.min(original.length - from, newLength));
            return copy;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(from);
        stringBuilder.append(" > ");
        stringBuilder.append(to);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @SafeVarargs
    public static <T> List<T> asList(T... a) {
        return new ArrayList(a);
    }

    public static int hashCode(long[] a) {
        int i = 0;
        if (a == null) {
            return 0;
        }
        int result = 1;
        int length = a.length;
        while (i < length) {
            long element = a[i];
            result = (31 * result) + ((int) ((element >>> 32) ^ element));
            i++;
        }
        return result;
    }

    public static int hashCode(int[] a) {
        int i = 0;
        if (a == null) {
            return 0;
        }
        int result = 1;
        while (i < a.length) {
            result = (31 * result) + a[i];
            i++;
        }
        return result;
    }

    public static int hashCode(short[] a) {
        int i = 0;
        if (a == null) {
            return 0;
        }
        int result = 1;
        while (i < a.length) {
            result = (31 * result) + a[i];
            i++;
        }
        return result;
    }

    public static int hashCode(char[] a) {
        int i = 0;
        if (a == null) {
            return 0;
        }
        int result = 1;
        while (i < a.length) {
            result = (31 * result) + a[i];
            i++;
        }
        return result;
    }

    public static int hashCode(byte[] a) {
        int i = 0;
        if (a == null) {
            return 0;
        }
        int result = 1;
        while (i < a.length) {
            result = (31 * result) + a[i];
            i++;
        }
        return result;
    }

    public static int hashCode(boolean[] a) {
        int i = 0;
        if (a == null) {
            return 0;
        }
        int result = 1;
        while (i < a.length) {
            result = (31 * result) + (a[i] ? 1231 : 1237);
            i++;
        }
        return result;
    }

    public static int hashCode(float[] a) {
        int i = 0;
        if (a == null) {
            return 0;
        }
        int result = 1;
        while (i < a.length) {
            result = (31 * result) + Float.floatToIntBits(a[i]);
            i++;
        }
        return result;
    }

    public static int hashCode(double[] a) {
        int i = 0;
        if (a == null) {
            return 0;
        }
        int result = 1;
        int length = a.length;
        while (i < length) {
            long bits = Double.doubleToLongBits(a[i]);
            result = (31 * result) + ((int) ((bits >>> 32) ^ bits));
            i++;
        }
        return result;
    }

    public static int hashCode(Object[] a) {
        if (a == null) {
            return 0;
        }
        int result = 1;
        for (Object element : a) {
            result = (31 * result) + (element == null ? 0 : element.hashCode());
        }
        return result;
    }

    public static int deepHashCode(Object[] a) {
        int i = 0;
        if (a == null) {
            return 0;
        }
        int result = 1;
        int length = a.length;
        while (i < length) {
            Object element = a[i];
            int elementHash = 0;
            if (element != null) {
                Class<?> cl = element.getClass().getComponentType();
                if (cl == null) {
                    elementHash = element.hashCode();
                } else if (element instanceof Object[]) {
                    elementHash = deepHashCode((Object[]) element);
                } else if (cl == Byte.TYPE) {
                    elementHash = hashCode((byte[]) element);
                } else if (cl == Short.TYPE) {
                    elementHash = hashCode((short[]) element);
                } else if (cl == Integer.TYPE) {
                    elementHash = hashCode((int[]) element);
                } else if (cl == Long.TYPE) {
                    elementHash = hashCode((long[]) element);
                } else if (cl == Character.TYPE) {
                    elementHash = hashCode((char[]) element);
                } else if (cl == Float.TYPE) {
                    elementHash = hashCode((float[]) element);
                } else if (cl == Double.TYPE) {
                    elementHash = hashCode((double[]) element);
                } else if (cl == Boolean.TYPE) {
                    elementHash = hashCode((boolean[]) element);
                } else {
                    elementHash = element.hashCode();
                }
            }
            result = (31 * result) + elementHash;
            i++;
        }
        return result;
    }

    /* JADX WARNING: Missing block: B:19:0x0028, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean deepEquals(Object[] a1, Object[] a2) {
        if (a1 == a2) {
            return true;
        }
        if (a1 == null || a2 == null) {
            return false;
        }
        int length = a1.length;
        if (a2.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            Object e1 = a1[i];
            Object e2 = a2[i];
            if (e1 != e2 && (e1 == null || e2 == null || !deepEquals0(e1, e2))) {
                return false;
            }
        }
        return true;
    }

    static boolean deepEquals0(Object e1, Object e2) {
        Class<?> cl1 = e1.getClass().getComponentType();
        if (cl1 != e2.getClass().getComponentType()) {
            return false;
        }
        if (e1 instanceof Object[]) {
            return deepEquals((Object[]) e1, (Object[]) e2);
        }
        if (cl1 == Byte.TYPE) {
            return equals((byte[]) e1, (byte[]) e2);
        }
        if (cl1 == Short.TYPE) {
            return equals((short[]) e1, (short[]) e2);
        }
        if (cl1 == Integer.TYPE) {
            return equals((int[]) e1, (int[]) e2);
        }
        if (cl1 == Long.TYPE) {
            return equals((long[]) e1, (long[]) e2);
        }
        if (cl1 == Character.TYPE) {
            return equals((char[]) e1, (char[]) e2);
        }
        if (cl1 == Float.TYPE) {
            return equals((float[]) e1, (float[]) e2);
        }
        if (cl1 == Double.TYPE) {
            return equals((double[]) e1, (double[]) e2);
        }
        if (cl1 == Boolean.TYPE) {
            return equals((boolean[]) e1, (boolean[]) e2);
        }
        return e1.equals(e2);
    }

    public static String toString(long[] a) {
        if (a == null) {
            return "null";
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(a[i]);
            if (i == iMax) {
                b.append(']');
                return b.toString();
            }
            b.append(", ");
            i++;
        }
    }

    public static String toString(int[] a) {
        if (a == null) {
            return "null";
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(a[i]);
            if (i == iMax) {
                b.append(']');
                return b.toString();
            }
            b.append(", ");
            i++;
        }
    }

    public static String toString(short[] a) {
        if (a == null) {
            return "null";
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(a[i]);
            if (i == iMax) {
                b.append(']');
                return b.toString();
            }
            b.append(", ");
            i++;
        }
    }

    public static String toString(char[] a) {
        if (a == null) {
            return "null";
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(a[i]);
            if (i == iMax) {
                b.append(']');
                return b.toString();
            }
            b.append(", ");
            i++;
        }
    }

    public static String toString(byte[] a) {
        if (a == null) {
            return "null";
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(a[i]);
            if (i == iMax) {
                b.append(']');
                return b.toString();
            }
            b.append(", ");
            i++;
        }
    }

    public static String toString(boolean[] a) {
        if (a == null) {
            return "null";
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(a[i]);
            if (i == iMax) {
                b.append(']');
                return b.toString();
            }
            b.append(", ");
            i++;
        }
    }

    public static String toString(float[] a) {
        if (a == null) {
            return "null";
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(a[i]);
            if (i == iMax) {
                b.append(']');
                return b.toString();
            }
            b.append(", ");
            i++;
        }
    }

    public static String toString(double[] a) {
        if (a == null) {
            return "null";
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(a[i]);
            if (i == iMax) {
                b.append(']');
                return b.toString();
            }
            b.append(", ");
            i++;
        }
    }

    public static String toString(Object[] a) {
        if (a == null) {
            return "null";
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(String.valueOf(a[i]));
            if (i == iMax) {
                b.append(']');
                return b.toString();
            }
            b.append(", ");
            i++;
        }
    }

    public static String deepToString(Object[] a) {
        if (a == null) {
            return "null";
        }
        int bufLen = 20 * a.length;
        if (a.length != 0 && bufLen <= 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuilder buf = new StringBuilder(bufLen);
        deepToString(a, buf, new HashSet());
        return buf.toString();
    }

    private static void deepToString(Object[] a, StringBuilder buf, Set<Object[]> dejaVu) {
        if (a == null) {
            buf.append("null");
            return;
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            buf.append("[]");
            return;
        }
        dejaVu.add(a);
        buf.append('[');
        int i = 0;
        while (true) {
            Object element = a[i];
            if (element == null) {
                buf.append("null");
            } else {
                Class<?> eClass = element.getClass();
                if (!eClass.isArray()) {
                    buf.append(element.toString());
                } else if (eClass == byte[].class) {
                    buf.append(toString((byte[]) element));
                } else if (eClass == short[].class) {
                    buf.append(toString((short[]) element));
                } else if (eClass == int[].class) {
                    buf.append(toString((int[]) element));
                } else if (eClass == long[].class) {
                    buf.append(toString((long[]) element));
                } else if (eClass == char[].class) {
                    buf.append(toString((char[]) element));
                } else if (eClass == float[].class) {
                    buf.append(toString((float[]) element));
                } else if (eClass == double[].class) {
                    buf.append(toString((double[]) element));
                } else if (eClass == boolean[].class) {
                    buf.append(toString((boolean[]) element));
                } else if (dejaVu.contains(element)) {
                    buf.append("[...]");
                } else {
                    deepToString((Object[]) element, buf, dejaVu);
                }
            }
            if (i == iMax) {
                buf.append(']');
                dejaVu.remove(a);
                return;
            }
            buf.append(", ");
            i++;
        }
    }

    public static <T> void setAll(T[] array, IntFunction<? extends T> generator) {
        Objects.requireNonNull(generator);
        for (int i = 0; i < array.length; i++) {
            array[i] = generator.apply(i);
        }
    }

    public static <T> void parallelSetAll(T[] array, IntFunction<? extends T> generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(new -$$Lambda$Arrays$H0YqaggIxZUqId4_BJ1BLcUa93k(array, generator));
    }

    public static void setAll(int[] array, IntUnaryOperator generator) {
        Objects.requireNonNull(generator);
        for (int i = 0; i < array.length; i++) {
            array[i] = generator.applyAsInt(i);
        }
    }

    public static void parallelSetAll(int[] array, IntUnaryOperator generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(new -$$Lambda$Arrays$KFf05FUz26CqVc_cf2bKY9C927o(array, generator));
    }

    public static void setAll(long[] array, IntToLongFunction generator) {
        Objects.requireNonNull(generator);
        for (int i = 0; i < array.length; i++) {
            array[i] = generator.applyAsLong(i);
        }
    }

    public static void parallelSetAll(long[] array, IntToLongFunction generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(new -$$Lambda$Arrays$aBSX_SvA5f2Q1t8_MODHDGhokzk(array, generator));
    }

    public static void setAll(double[] array, IntToDoubleFunction generator) {
        Objects.requireNonNull(generator);
        for (int i = 0; i < array.length; i++) {
            array[i] = generator.applyAsDouble(i);
        }
    }

    public static void parallelSetAll(double[] array, IntToDoubleFunction generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(new -$$Lambda$Arrays$x0HcRDlColwoPupFWmOW7TREPtM(array, generator));
    }

    public static <T> Spliterator<T> spliterator(T[] array) {
        return Spliterators.spliterator((Object[]) array, 1040);
    }

    public static <T> Spliterator<T> spliterator(T[] array, int startInclusive, int endExclusive) {
        return Spliterators.spliterator((Object[]) array, startInclusive, endExclusive, 1040);
    }

    public static OfInt spliterator(int[] array) {
        return Spliterators.spliterator(array, 1040);
    }

    public static OfInt spliterator(int[] array, int startInclusive, int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive, 1040);
    }

    public static OfLong spliterator(long[] array) {
        return Spliterators.spliterator(array, 1040);
    }

    public static OfLong spliterator(long[] array, int startInclusive, int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive, 1040);
    }

    public static OfDouble spliterator(double[] array) {
        return Spliterators.spliterator(array, 1040);
    }

    public static OfDouble spliterator(double[] array, int startInclusive, int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive, 1040);
    }

    public static <T> Stream<T> stream(T[] array) {
        return stream((Object[]) array, 0, array.length);
    }

    public static <T> Stream<T> stream(T[] array, int startInclusive, int endExclusive) {
        return StreamSupport.stream(spliterator((Object[]) array, startInclusive, endExclusive), false);
    }

    public static IntStream stream(int[] array) {
        return stream(array, 0, array.length);
    }

    public static IntStream stream(int[] array, int startInclusive, int endExclusive) {
        return StreamSupport.intStream(spliterator(array, startInclusive, endExclusive), false);
    }

    public static LongStream stream(long[] array) {
        return stream(array, 0, array.length);
    }

    public static LongStream stream(long[] array, int startInclusive, int endExclusive) {
        return StreamSupport.longStream(spliterator(array, startInclusive, endExclusive), false);
    }

    public static DoubleStream stream(double[] array) {
        return stream(array, 0, array.length);
    }

    public static DoubleStream stream(double[] array, int startInclusive, int endExclusive) {
        return StreamSupport.doubleStream(spliterator(array, startInclusive, endExclusive), false);
    }
}
