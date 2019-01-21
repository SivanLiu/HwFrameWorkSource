package java.nio;

public abstract class Buffer {
    static final int SPLITERATOR_CHARACTERISTICS = 16464;
    final int _elementSizeShift;
    long address;
    private int capacity;
    private int limit;
    private int mark = -1;
    int position = 0;

    public abstract Object array();

    public abstract int arrayOffset();

    public abstract boolean hasArray();

    public abstract boolean isDirect();

    public abstract boolean isReadOnly();

    Buffer(int mark, int pos, int lim, int cap, int elementSizeShift) {
        StringBuilder stringBuilder;
        if (cap >= 0) {
            this.capacity = cap;
            limit(lim);
            position(pos);
            if (mark >= 0) {
                if (mark <= pos) {
                    this.mark = mark;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mark > position: (");
                    stringBuilder.append(mark);
                    stringBuilder.append(" > ");
                    stringBuilder.append(pos);
                    stringBuilder.append(")");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            this._elementSizeShift = elementSizeShift;
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Negative capacity: ");
        stringBuilder.append(cap);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public final int capacity() {
        return this.capacity;
    }

    public final int position() {
        return this.position;
    }

    public final Buffer position(int newPosition) {
        if (newPosition > this.limit || newPosition < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad position ");
            stringBuilder.append(newPosition);
            stringBuilder.append("/");
            stringBuilder.append(this.limit);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.position = newPosition;
        if (this.mark > this.position) {
            this.mark = -1;
        }
        return this;
    }

    public final int limit() {
        return this.limit;
    }

    public final Buffer limit(int newLimit) {
        if (newLimit > this.capacity || newLimit < 0) {
            throw new IllegalArgumentException();
        }
        this.limit = newLimit;
        if (this.position > this.limit) {
            this.position = this.limit;
        }
        if (this.mark > this.limit) {
            this.mark = -1;
        }
        return this;
    }

    public final Buffer mark() {
        this.mark = this.position;
        return this;
    }

    public final Buffer reset() {
        int m = this.mark;
        if (m >= 0) {
            this.position = m;
            return this;
        }
        throw new InvalidMarkException();
    }

    public final Buffer clear() {
        this.position = 0;
        this.limit = this.capacity;
        this.mark = -1;
        return this;
    }

    public final Buffer flip() {
        this.limit = this.position;
        this.position = 0;
        this.mark = -1;
        return this;
    }

    public final Buffer rewind() {
        this.position = 0;
        this.mark = -1;
        return this;
    }

    public final int remaining() {
        return this.limit - this.position;
    }

    public final boolean hasRemaining() {
        return this.position < this.limit;
    }

    final int nextGetIndex() {
        if (this.position < this.limit) {
            int i = this.position;
            this.position = i + 1;
            return i;
        }
        throw new BufferUnderflowException();
    }

    final int nextGetIndex(int nb) {
        if (this.limit - this.position >= nb) {
            int p = this.position;
            this.position += nb;
            return p;
        }
        throw new BufferUnderflowException();
    }

    final int nextPutIndex() {
        if (this.position < this.limit) {
            int i = this.position;
            this.position = i + 1;
            return i;
        }
        throw new BufferOverflowException();
    }

    final int nextPutIndex(int nb) {
        if (this.limit - this.position >= nb) {
            int p = this.position;
            this.position += nb;
            return p;
        }
        throw new BufferOverflowException();
    }

    final int checkIndex(int i) {
        if (i >= 0 && i < this.limit) {
            return i;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("index=");
        stringBuilder.append(i);
        stringBuilder.append(" out of bounds (limit=");
        stringBuilder.append(this.limit);
        stringBuilder.append(")");
        throw new IndexOutOfBoundsException(stringBuilder.toString());
    }

    final int checkIndex(int i, int nb) {
        if (i >= 0 && nb <= this.limit - i) {
            return i;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("index=");
        stringBuilder.append(i);
        stringBuilder.append(" out of bounds (limit=");
        stringBuilder.append(this.limit);
        stringBuilder.append(", nb=");
        stringBuilder.append(nb);
        stringBuilder.append(")");
        throw new IndexOutOfBoundsException(stringBuilder.toString());
    }

    final int markValue() {
        return this.mark;
    }

    final void truncate() {
        this.mark = -1;
        this.position = 0;
        this.limit = 0;
        this.capacity = 0;
    }

    final void discardMark() {
        this.mark = -1;
    }

    static void checkBounds(int off, int len, int size) {
        if ((((off | len) | (off + len)) | (size - (off + len))) < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("off=");
            stringBuilder.append(off);
            stringBuilder.append(", len=");
            stringBuilder.append(len);
            stringBuilder.append(" out of bounds (size=");
            stringBuilder.append(size);
            stringBuilder.append(")");
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public int getElementSizeShift() {
        return this._elementSizeShift;
    }
}
