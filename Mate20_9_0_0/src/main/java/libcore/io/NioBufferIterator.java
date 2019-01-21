package libcore.io;

public final class NioBufferIterator extends BufferIterator {
    private final long address;
    private final MemoryMappedFile file;
    private final int length;
    private int position;
    private final boolean swap;

    NioBufferIterator(MemoryMappedFile file, long address, int length, boolean swap) {
        file.checkNotClosed();
        this.file = file;
        this.address = address;
        if (length < 0) {
            throw new IllegalArgumentException("length < 0");
        } else if (Long.compareUnsigned(address, -1 - ((long) length)) <= 0) {
            this.length = length;
            this.swap = swap;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("length ");
            stringBuilder.append(length);
            stringBuilder.append(" would overflow 64-bit address space");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public void seek(int offset) {
        this.position = offset;
    }

    public void skip(int byteCount) {
        this.position += byteCount;
    }

    public int pos() {
        return this.position;
    }

    public void readByteArray(byte[] dst, int dstOffset, int byteCount) {
        checkDstBounds(dstOffset, dst.length, byteCount);
        this.file.checkNotClosed();
        checkReadBounds(this.position, this.length, byteCount);
        Memory.peekByteArray(this.address + ((long) this.position), dst, dstOffset, byteCount);
        this.position += byteCount;
    }

    public byte readByte() {
        this.file.checkNotClosed();
        checkReadBounds(this.position, this.length, 1);
        byte result = Memory.peekByte(this.address + ((long) this.position));
        this.position++;
        return result;
    }

    public int readInt() {
        this.file.checkNotClosed();
        checkReadBounds(this.position, this.length, 4);
        int result = Memory.peekInt(this.address + ((long) this.position), this.swap);
        this.position += 4;
        return result;
    }

    public void readIntArray(int[] dst, int dstOffset, int intCount) {
        checkDstBounds(dstOffset, dst.length, intCount);
        this.file.checkNotClosed();
        int byteCount = 4 * intCount;
        checkReadBounds(this.position, this.length, byteCount);
        Memory.peekIntArray(this.address + ((long) this.position), dst, dstOffset, intCount, this.swap);
        this.position += byteCount;
    }

    public short readShort() {
        this.file.checkNotClosed();
        checkReadBounds(this.position, this.length, 2);
        short result = Memory.peekShort(this.address + ((long) this.position), this.swap);
        this.position += 2;
        return result;
    }

    private static void checkReadBounds(int position, int length, int byteCount) {
        if (position < 0 || byteCount < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid read args: position=");
            stringBuilder.append(position);
            stringBuilder.append(", byteCount=");
            stringBuilder.append(byteCount);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
        int finalReadPos = position + byteCount;
        if (finalReadPos < 0 || finalReadPos > length) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Read outside range: position=");
            stringBuilder2.append(position);
            stringBuilder2.append(", byteCount=");
            stringBuilder2.append(byteCount);
            stringBuilder2.append(", length=");
            stringBuilder2.append(length);
            throw new IndexOutOfBoundsException(stringBuilder2.toString());
        }
    }

    private static void checkDstBounds(int dstOffset, int dstLength, int count) {
        if (dstOffset < 0 || count < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid dst args: offset=");
            stringBuilder.append(dstLength);
            stringBuilder.append(", count=");
            stringBuilder.append(count);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
        int targetPos = dstOffset + count;
        if (targetPos < 0 || targetPos > dstLength) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Write outside range: dst.length=");
            stringBuilder2.append(dstLength);
            stringBuilder2.append(", offset=");
            stringBuilder2.append(dstOffset);
            stringBuilder2.append(", count=");
            stringBuilder2.append(count);
            throw new IndexOutOfBoundsException(stringBuilder2.toString());
        }
    }
}
