package com.android.internal.util;

import java.lang.reflect.Array;
import java.util.Arrays;

public class RingBuffer<T> {
    private final T[] mBuffer;
    private long mCursor = 0;

    public RingBuffer(Class<T> c, int capacity) {
        Preconditions.checkArgumentPositive(capacity, "A RingBuffer cannot have 0 capacity");
        this.mBuffer = (Object[]) Array.newInstance(c, capacity);
    }

    public int size() {
        return (int) Math.min((long) this.mBuffer.length, this.mCursor);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void clear() {
        for (int i = 0; i < size(); i++) {
            this.mBuffer[i] = null;
        }
        this.mCursor = 0;
    }

    public void append(T t) {
        Object[] objArr = this.mBuffer;
        long j = this.mCursor;
        this.mCursor = 1 + j;
        objArr[indexOf(j)] = t;
    }

    public T getNextSlot() {
        int nextSlotIdx = this.mCursor;
        this.mCursor = 1 + nextSlotIdx;
        nextSlotIdx = indexOf(nextSlotIdx);
        if (this.mBuffer[nextSlotIdx] == null) {
            this.mBuffer[nextSlotIdx] = createNewItem();
        }
        return this.mBuffer[nextSlotIdx];
    }

    protected T createNewItem() {
        try {
            return this.mBuffer.getClass().getComponentType().newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            return null;
        }
    }

    public T[] toArray() {
        T[] out = Arrays.copyOf(this.mBuffer, size(), this.mBuffer.getClass());
        long inCursor = this.mCursor - 1;
        int outIdx = out.length - 1;
        while (outIdx >= 0) {
            int outIdx2 = outIdx - 1;
            long inCursor2 = inCursor - 1;
            out[outIdx] = this.mBuffer[indexOf(inCursor)];
            outIdx = outIdx2;
            inCursor = inCursor2;
        }
        return out;
    }

    private int indexOf(long cursor) {
        return (int) Math.abs(cursor % ((long) this.mBuffer.length));
    }
}
