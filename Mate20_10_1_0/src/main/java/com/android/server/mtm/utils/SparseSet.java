package com.android.server.mtm.utils;

import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.util.Arrays;
import java.util.Collection;
import libcore.util.EmptyArray;

public class SparseSet implements Cloneable {
    private int[] mKeys;
    private int mSize;

    public SparseSet() {
        this(10);
    }

    public SparseSet(int initialCapacity) {
        if (initialCapacity == 0) {
            this.mKeys = EmptyArray.INT;
        } else {
            this.mKeys = ArrayUtils.newUnpaddedIntArray(initialCapacity);
        }
        this.mSize = 0;
    }

    @Override // java.lang.Object
    public SparseSet clone() {
        SparseSet clone = null;
        try {
            clone = (SparseSet) super.clone();
            clone.mKeys = (int[]) this.mKeys.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            return clone;
        }
    }

    public boolean contains(int key) {
        if (ContainerHelpers.binarySearch(this.mKeys, this.mSize, key) < 0) {
            return false;
        }
        return true;
    }

    public void remove(int key) {
        int i = ContainerHelpers.binarySearch(this.mKeys, this.mSize, key);
        if (i >= 0) {
            removeAt(i);
        }
    }

    public void removeAt(int index) {
        int[] iArr = this.mKeys;
        System.arraycopy(iArr, index + 1, iArr, index, this.mSize - (index + 1));
        this.mSize--;
    }

    public void add(int key) {
        int i = ContainerHelpers.binarySearch(this.mKeys, this.mSize, key);
        if (i < 0) {
            this.mKeys = GrowingArrayUtils.insert(this.mKeys, this.mSize, ~i, key);
            this.mSize++;
        }
    }

    public void addAll(SparseSet other) {
        for (int i = other.size() - 1; i >= 0; i--) {
            add(other.keyAt(i));
        }
    }

    public void addAll(Collection<Integer> other) {
        for (Integer item : other) {
            add(item.intValue());
        }
    }

    public int size() {
        return this.mSize;
    }

    public boolean isEmpty() {
        return this.mSize == 0;
    }

    public int keyAt(int index) {
        if (index < this.mSize) {
            return this.mKeys[index];
        }
        throw new ArrayIndexOutOfBoundsException(index);
    }

    public int indexOfKey(int key) {
        return ContainerHelpers.binarySearch(this.mKeys, this.mSize, key);
    }

    public void clear() {
        this.mSize = 0;
    }

    public void append(int key) {
        int i = this.mSize;
        if (i == 0 || key > this.mKeys[i - 1]) {
            this.mKeys = GrowingArrayUtils.append(this.mKeys, this.mSize, key);
            this.mSize++;
            return;
        }
        add(key);
    }

    public int[] copyKeys() {
        if (size() == 0) {
            return null;
        }
        return Arrays.copyOf(this.mKeys, size());
    }

    public String toString() {
        if (size() <= 0) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder(this.mSize * 28);
        buffer.append('{');
        for (int i = 0; i < this.mSize; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(keyAt(i));
        }
        buffer.append('}');
        return buffer.toString();
    }

    public static final class ContainerHelpers {
        static int binarySearch(int[] array, int size, int value) {
            int lo = 0;
            int hi = size - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int midVal = array[mid];
                if (midVal < value) {
                    lo = mid + 1;
                } else if (midVal <= value) {
                    return mid;
                } else {
                    hi = mid - 1;
                }
            }
            return ~lo;
        }

        static int binarySearch(long[] array, int size, long value) {
            int lo = 0;
            int hi = size - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                long midVal = array[mid];
                if (midVal < value) {
                    lo = mid + 1;
                } else if (midVal <= value) {
                    return mid;
                } else {
                    hi = mid - 1;
                }
            }
            return ~lo;
        }
    }
}
