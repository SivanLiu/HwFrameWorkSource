package java.nio;

import libcore.io.Memory;

class ByteBufferAsCharBuffer extends CharBuffer {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    protected final ByteBuffer bb;
    protected final int offset;
    private final ByteOrder order;

    ByteBufferAsCharBuffer(ByteBuffer bb, int mark, int pos, int lim, int cap, int off, ByteOrder order) {
        super(mark, pos, lim, cap);
        this.bb = bb.duplicate();
        this.isReadOnly = bb.isReadOnly;
        if (bb instanceof DirectByteBuffer) {
            this.address = bb.address + ((long) off);
        }
        this.bb.order(order);
        this.order = order;
        this.offset = off;
    }

    public CharBuffer slice() {
        int pos = position();
        int lim = limit();
        int rem = pos <= lim ? lim - pos : 0;
        return new ByteBufferAsCharBuffer(this.bb, -1, 0, rem, rem, (pos << 1) + this.offset, this.order);
    }

    public CharBuffer duplicate() {
        return new ByteBufferAsCharBuffer(this.bb, markValue(), position(), limit(), capacity(), this.offset, this.order);
    }

    public CharBuffer asReadOnlyBuffer() {
        return new ByteBufferAsCharBuffer(this.bb.asReadOnlyBuffer(), markValue(), position(), limit(), capacity(), this.offset, this.order);
    }

    protected int ix(int i) {
        return (i << 1) + this.offset;
    }

    public char get() {
        return get(nextGetIndex());
    }

    public char get(int i) {
        return this.bb.getCharUnchecked(ix(checkIndex(i)));
    }

    public CharBuffer get(char[] dst, int offset, int length) {
        Buffer.checkBounds(offset, length, dst.length);
        if (length <= remaining()) {
            this.bb.getUnchecked(ix(this.position), dst, offset, length);
            this.position += length;
            return this;
        }
        throw new BufferUnderflowException();
    }

    char getUnchecked(int i) {
        return this.bb.getCharUnchecked(ix(i));
    }

    public CharBuffer put(char x) {
        put(nextPutIndex(), x);
        return this;
    }

    public CharBuffer put(int i, char x) {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        this.bb.putCharUnchecked(ix(checkIndex(i)), x);
        return this;
    }

    public CharBuffer put(char[] src, int offset, int length) {
        Buffer.checkBounds(offset, length, src.length);
        if (length <= remaining()) {
            this.bb.putUnchecked(ix(this.position), src, offset, length);
            this.position += length;
            return this;
        }
        throw new BufferOverflowException();
    }

    public CharBuffer compact() {
        if (this.isReadOnly) {
            throw new ReadOnlyBufferException();
        }
        int pos = position();
        int lim = limit();
        int rem = pos <= lim ? lim - pos : 0;
        if (this.bb instanceof DirectByteBuffer) {
            Memory.memmove(this, ix(0), this, ix(pos), (long) (rem << 1));
        } else {
            System.arraycopy(this.bb.array(), ix(pos), this.bb.array(), ix(0), rem << 1);
        }
        position(rem);
        limit(capacity());
        discardMark();
        return this;
    }

    public boolean isDirect() {
        return this.bb.isDirect();
    }

    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    public String toString(int start, int end) {
        if (end > limit() || start > end) {
            throw new IndexOutOfBoundsException();
        }
        try {
            char[] ca = new char[(end - start)];
            CharBuffer cb = CharBuffer.wrap(ca);
            CharBuffer db = duplicate();
            db.position(start);
            db.limit(end);
            cb.put(db);
            return new String(ca);
        } catch (StringIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    public CharBuffer subSequence(int start, int end) {
        int pos = position();
        int lim = limit();
        pos = pos <= lim ? pos : lim;
        int len = lim - pos;
        if (start >= 0 && end <= len && start <= end) {
            return new ByteBufferAsCharBuffer(this.bb, -1, pos + start, pos + end, capacity(), this.offset, this.order);
        }
        throw new IndexOutOfBoundsException();
    }

    public ByteOrder order() {
        return this.order;
    }
}
