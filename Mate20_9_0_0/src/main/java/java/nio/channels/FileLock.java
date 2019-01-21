package java.nio.channels;

import java.io.IOException;

public abstract class FileLock implements AutoCloseable {
    private final Channel channel;
    private final long position;
    private final boolean shared;
    private final long size;

    public abstract boolean isValid();

    public abstract void release() throws IOException;

    protected FileLock(FileChannel channel, long position, long size, boolean shared) {
        if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        } else if (size < 0) {
            throw new IllegalArgumentException("Negative size");
        } else if (position + size >= 0) {
            this.channel = channel;
            this.position = position;
            this.size = size;
            this.shared = shared;
        } else {
            throw new IllegalArgumentException("Negative position + size");
        }
    }

    protected FileLock(AsynchronousFileChannel channel, long position, long size, boolean shared) {
        if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        } else if (size < 0) {
            throw new IllegalArgumentException("Negative size");
        } else if (position + size >= 0) {
            this.channel = channel;
            this.position = position;
            this.size = size;
            this.shared = shared;
        } else {
            throw new IllegalArgumentException("Negative position + size");
        }
    }

    public final FileChannel channel() {
        return this.channel instanceof FileChannel ? (FileChannel) this.channel : null;
    }

    public Channel acquiredBy() {
        return this.channel;
    }

    public final long position() {
        return this.position;
    }

    public final long size() {
        return this.size;
    }

    public final boolean isShared() {
        return this.shared;
    }

    public final boolean overlaps(long position, long size) {
        if (position + size > this.position && this.position + this.size > position) {
            return true;
        }
        return false;
    }

    public final void close() throws IOException {
        release();
    }

    public final String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getName());
        stringBuilder.append("[");
        stringBuilder.append(this.position);
        stringBuilder.append(":");
        stringBuilder.append(this.size);
        stringBuilder.append(" ");
        stringBuilder.append(this.shared ? "shared" : "exclusive");
        stringBuilder.append(" ");
        stringBuilder.append(isValid() ? "valid" : "invalid");
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
