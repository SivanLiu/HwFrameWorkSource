package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

public class IOUtil {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final int IOV_MAX = iovMax();

    public static native void configureBlocking(FileDescriptor fileDescriptor, boolean z) throws IOException;

    static native boolean drain(int i) throws IOException;

    static native int fdLimit();

    public static native int fdVal(FileDescriptor fileDescriptor);

    static native int iovMax();

    static native long makePipe(boolean z);

    static native boolean randomBytes(byte[] bArr);

    static native void setfdVal(FileDescriptor fileDescriptor, int i);

    private IOUtil() {
    }

    static int write(FileDescriptor fd, ByteBuffer src, long position, NativeDispatcher nd) throws IOException {
        if (src instanceof DirectBuffer) {
            return writeFromNativeBuffer(fd, src, position, nd);
        }
        int pos = src.position();
        int lim = src.limit();
        ByteBuffer bb = Util.getTemporaryDirectBuffer(pos <= lim ? lim - pos : 0);
        try {
            bb.put(src);
            bb.flip();
            src.position(pos);
            int n = writeFromNativeBuffer(fd, bb, position, nd);
            if (n > 0) {
                src.position(pos + n);
            }
            Util.offerFirstTemporaryDirectBuffer(bb);
            return n;
        } catch (Throwable th) {
            Util.offerFirstTemporaryDirectBuffer(bb);
        }
    }

    private static int writeFromNativeBuffer(FileDescriptor fd, ByteBuffer bb, long position, NativeDispatcher nd) throws IOException {
        ByteBuffer byteBuffer = bb;
        int pos = byteBuffer.position();
        int lim = byteBuffer.limit();
        int rem = pos <= lim ? lim - pos : 0;
        if (rem == 0) {
            return 0;
        }
        int written;
        if (position != -1) {
            NativeDispatcher nativeDispatcher = nd;
            written = nd.pwrite(fd, ((long) pos) + ((DirectBuffer) byteBuffer).address(), rem, position);
            int written2 = fd;
        } else {
            written = nd.write(fd, ((DirectBuffer) byteBuffer).address() + ((long) pos), rem);
        }
        if (written > 0) {
            byteBuffer.position(pos + written);
        }
        return written;
    }

    static long write(FileDescriptor fd, ByteBuffer[] bufs, NativeDispatcher nd) throws IOException {
        return write(fd, bufs, 0, bufs.length, nd);
    }

    static long write(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length, NativeDispatcher nd) throws IOException {
        int j;
        IOVecWrapper vec = IOVecWrapper.get(length);
        int i = 0;
        int count = offset + length;
        int iov_len = 0;
        int i2 = offset;
        while (i2 < count) {
            try {
                if (iov_len >= IOV_MAX) {
                    break;
                }
                ByteBuffer buf = bufs[i2];
                int pos = buf.position();
                int lim = buf.limit();
                int rem = pos <= lim ? lim - pos : i;
                if (rem > 0) {
                    vec.setBuffer(iov_len, buf, pos, rem);
                    if (!(buf instanceof DirectBuffer)) {
                        ByteBuffer shadow = Util.getTemporaryDirectBuffer(rem);
                        shadow.put(buf);
                        shadow.flip();
                        vec.setShadow(iov_len, shadow);
                        buf.position(pos);
                        buf = shadow;
                        pos = shadow.position();
                    }
                    vec.putBase(iov_len, ((DirectBuffer) buf).address() + ((long) pos));
                    vec.putLen(iov_len, (long) rem);
                    iov_len++;
                }
                i2++;
                i = 0;
            } catch (Throwable th) {
                if (null == null) {
                    j = 0;
                    while (true) {
                        i = j;
                        if (i >= iov_len) {
                            break;
                        }
                        ByteBuffer shadow2 = vec.getShadow(i);
                        if (shadow2 != null) {
                            Util.offerLastTemporaryDirectBuffer(shadow2);
                        }
                        vec.clearRefs(i);
                        j = i + 1;
                    }
                }
            }
        }
        int j2;
        if (iov_len == 0) {
            if (null == null) {
                j2 = 0;
                while (true) {
                    int j3 = j2;
                    if (j3 >= iov_len) {
                        break;
                    }
                    ByteBuffer shadow3 = vec.getShadow(j3);
                    if (shadow3 != null) {
                        Util.offerLastTemporaryDirectBuffer(shadow3);
                    }
                    vec.clearRefs(j3);
                    j2 = j3 + 1;
                }
            }
            return 0;
        }
        int n;
        long bytesWritten = nd.writev(fd, vec.address, iov_len);
        long left = bytesWritten;
        int j4 = 0;
        while (j4 < iov_len) {
            int count2;
            if (left > 0) {
                ByteBuffer buf2 = vec.getBuffer(j4);
                int pos2 = vec.getPosition(j4);
                count2 = count;
                j2 = vec.getRemaining(j4);
                n = left > ((long) j2) ? j2 : (int) left;
                buf2.position(pos2 + n);
                left -= (long) n;
            } else {
                count2 = count;
            }
            ByteBuffer shadow4 = vec.getShadow(j4);
            if (shadow4 != null) {
                Util.offerLastTemporaryDirectBuffer(shadow4);
            }
            vec.clearRefs(j4);
            j4++;
            count = count2;
            FileDescriptor fileDescriptor = fd;
            NativeDispatcher nativeDispatcher = nd;
        }
        if (!true) {
            j = 0;
            while (true) {
                n = j;
                if (n >= iov_len) {
                    break;
                }
                ByteBuffer shadow5 = vec.getShadow(n);
                if (shadow5 != null) {
                    Util.offerLastTemporaryDirectBuffer(shadow5);
                }
                vec.clearRefs(n);
                j = n + 1;
            }
        }
        return bytesWritten;
    }

    static int read(FileDescriptor fd, ByteBuffer dst, long position, NativeDispatcher nd) throws IOException {
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        } else if (dst instanceof DirectBuffer) {
            return readIntoNativeBuffer(fd, dst, position, nd);
        } else {
            ByteBuffer bb = Util.getTemporaryDirectBuffer(dst.remaining());
            try {
                int n = readIntoNativeBuffer(fd, bb, position, nd);
                bb.flip();
                if (n > 0) {
                    dst.put(bb);
                }
                Util.offerFirstTemporaryDirectBuffer(bb);
                return n;
            } catch (Throwable th) {
                Util.offerFirstTemporaryDirectBuffer(bb);
            }
        }
    }

    private static int readIntoNativeBuffer(FileDescriptor fd, ByteBuffer bb, long position, NativeDispatcher nd) throws IOException {
        int pos = bb.position();
        int lim = bb.limit();
        int rem = pos <= lim ? lim - pos : 0;
        if (rem == 0) {
            return 0;
        }
        int n;
        if (position != -1) {
            n = nd.pread(fd, ((long) pos) + ((DirectBuffer) bb).address(), rem, position);
        } else {
            n = nd.read(fd, ((DirectBuffer) bb).address() + ((long) pos), rem);
        }
        if (n > 0) {
            bb.position(pos + n);
        }
        return n;
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, NativeDispatcher nd) throws IOException {
        return read(fd, bufs, 0, bufs.length, nd);
    }

    static long read(FileDescriptor fd, ByteBuffer[] bufs, int offset, int length, NativeDispatcher nd) throws IOException {
        boolean j;
        IOVecWrapper vec = IOVecWrapper.get(length);
        int i = 0;
        int count = offset + length;
        boolean iov_len = false;
        int i2 = offset;
        while (i2 < count) {
            try {
                if (iov_len >= IOV_MAX) {
                    break;
                }
                ByteBuffer buf = bufs[i2];
                if (buf.isReadOnly()) {
                    throw new IllegalArgumentException("Read-only buffer");
                }
                int pos = buf.position();
                int lim = buf.limit();
                int rem = pos <= lim ? lim - pos : i;
                if (rem > 0) {
                    vec.setBuffer(iov_len, buf, pos, rem);
                    if (!(buf instanceof DirectBuffer)) {
                        ByteBuffer shadow = Util.getTemporaryDirectBuffer(rem);
                        vec.setShadow(iov_len, shadow);
                        buf = shadow;
                        pos = shadow.position();
                    }
                    vec.putBase(iov_len, ((DirectBuffer) buf).address() + ((long) pos));
                    vec.putLen(iov_len, (long) rem);
                    iov_len++;
                }
                i2++;
                i = 0;
            } catch (Throwable th) {
                if (null == null) {
                    j = false;
                    while (true) {
                        boolean j2 = j;
                        if (j2 >= iov_len) {
                            break;
                        }
                        ByteBuffer shadow2 = vec.getShadow(j2);
                        if (shadow2 != null) {
                            Util.offerLastTemporaryDirectBuffer(shadow2);
                        }
                        vec.clearRefs(j2);
                        j = j2 + 1;
                    }
                }
            }
        }
        if (iov_len) {
            int count2;
            long bytesRead = nd.readv(fd, vec.address, iov_len);
            long left = bytesRead;
            boolean j3 = false;
            while (j3 < iov_len) {
                int i3;
                ByteBuffer shadow3 = vec.getShadow(j3);
                if (left > 0) {
                    ByteBuffer buf2 = vec.getBuffer(j3);
                    count2 = count;
                    int rem2 = vec.getRemaining(j3);
                    int n = left > ((long) rem2) ? rem2 : (int) left;
                    if (shadow3 == null) {
                        count = vec.getPosition(j3);
                        i3 = i2;
                        int i4 = count;
                        buf2.position(count + n);
                    } else {
                        i3 = i2;
                        count = buf2;
                        shadow3.limit(shadow3.position() + n);
                        count.put(shadow3);
                    }
                    left -= (long) n;
                } else {
                    i3 = i2;
                    count2 = count;
                }
                if (shadow3 != null) {
                    Util.offerLastTemporaryDirectBuffer(shadow3);
                }
                vec.clearRefs(j3);
                j3++;
                count = count2;
                i2 = i3;
                FileDescriptor fileDescriptor = fd;
                NativeDispatcher nativeDispatcher = nd;
            }
            count2 = count;
            if (!true) {
                j = false;
                while (true) {
                    boolean completed = j;
                    if (completed >= iov_len) {
                        break;
                    }
                    ByteBuffer shadow4 = vec.getShadow(completed);
                    if (shadow4 != null) {
                        Util.offerLastTemporaryDirectBuffer(shadow4);
                    }
                    vec.clearRefs(completed);
                    j = completed + 1;
                }
            }
            return bytesRead;
        }
        if (null == null) {
            boolean j4 = false;
            while (true) {
                boolean j5 = j4;
                if (j5 >= iov_len) {
                    break;
                }
                ByteBuffer shadow5 = vec.getShadow(j5);
                if (shadow5 != null) {
                    Util.offerLastTemporaryDirectBuffer(shadow5);
                }
                vec.clearRefs(j5);
                j4 = j5 + 1;
            }
        }
        return 0;
    }

    public static FileDescriptor newFD(int i) {
        FileDescriptor fd = new FileDescriptor();
        setfdVal(fd, i);
        return fd;
    }
}
