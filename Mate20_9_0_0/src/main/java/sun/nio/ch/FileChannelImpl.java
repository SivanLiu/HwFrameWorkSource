package sun.nio.ch;

import android.system.ErrnoException;
import android.system.OsConstants;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DirectByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import libcore.io.Libcore;
import sun.misc.Cleaner;
import sun.security.action.GetPropertyAction;

public class FileChannelImpl extends FileChannel {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final long MAPPED_TRANSFER_SIZE = 8388608;
    private static final int MAP_PV = 2;
    private static final int MAP_RO = 0;
    private static final int MAP_RW = 1;
    private static final int TRANSFER_SIZE = 8192;
    private static final long allocationGranularity = initIDs();
    private static volatile boolean fileSupported = true;
    private static boolean isSharedFileLockTable;
    private static volatile boolean pipeSupported = true;
    private static volatile boolean propertyChecked;
    private static volatile boolean transferSupported = true;
    private final boolean append;
    public final FileDescriptor fd;
    private volatile FileLockTable fileLockTable;
    private final CloseGuard guard = CloseGuard.get();
    private final FileDispatcher nd;
    private final Object parent;
    private final String path;
    private final Object positionLock = new Object();
    private final boolean readable;
    private final NativeThreadSet threads = new NativeThreadSet(2);
    private final boolean writable;

    private static class SimpleFileLockTable extends FileLockTable {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private final List<FileLock> lockList = new ArrayList(2);

        static {
            Class cls = FileChannelImpl.class;
        }

        private void checkList(long position, long size) throws OverlappingFileLockException {
            for (FileLock fl : this.lockList) {
                if (fl.overlaps(position, size)) {
                    throw new OverlappingFileLockException();
                }
            }
        }

        public void add(FileLock fl) throws OverlappingFileLockException {
            synchronized (this.lockList) {
                checkList(fl.position(), fl.size());
                this.lockList.add(fl);
            }
        }

        public void remove(FileLock fl) {
            synchronized (this.lockList) {
                this.lockList.remove((Object) fl);
            }
        }

        public List<FileLock> removeAll() {
            List<FileLock> result;
            synchronized (this.lockList) {
                result = new ArrayList(this.lockList);
                this.lockList.clear();
            }
            return result;
        }

        public void replace(FileLock fl1, FileLock fl2) {
            synchronized (this.lockList) {
                this.lockList.remove((Object) fl1);
                this.lockList.add(fl2);
            }
        }
    }

    private static class Unmapper implements Runnable {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        static volatile int count;
        private static final NativeDispatcher nd = new FileDispatcherImpl();
        static volatile long totalCapacity;
        static volatile long totalSize;
        private volatile long address;
        private final int cap;
        private final FileDescriptor fd;
        private final long size;

        static {
            Class cls = FileChannelImpl.class;
        }

        private Unmapper(long address, long size, int cap, FileDescriptor fd) {
            this.address = address;
            this.size = size;
            this.cap = cap;
            this.fd = fd;
            synchronized (Unmapper.class) {
                count++;
                totalSize += size;
                totalCapacity += (long) cap;
            }
        }

        public void run() {
            if (this.address != 0) {
                FileChannelImpl.unmap0(this.address, this.size);
                this.address = 0;
                if (this.fd.valid()) {
                    try {
                        nd.close(this.fd);
                    } catch (IOException e) {
                    }
                }
                synchronized (Unmapper.class) {
                    count--;
                    totalSize -= this.size;
                    totalCapacity -= (long) this.cap;
                }
            }
        }
    }

    private static native long initIDs();

    private native long map0(int i, long j, long j2) throws IOException;

    private native long position0(FileDescriptor fileDescriptor, long j);

    private native long transferTo0(FileDescriptor fileDescriptor, long j, long j2, FileDescriptor fileDescriptor2);

    private static native int unmap0(long j, long j2);

    private FileChannelImpl(FileDescriptor fd, String path, boolean readable, boolean writable, boolean append, Object parent) {
        this.fd = fd;
        this.readable = readable;
        this.writable = writable;
        this.append = append;
        this.parent = parent;
        this.path = path;
        this.nd = new FileDispatcherImpl(append);
        if (fd != null && fd.valid()) {
            this.guard.open("close");
        }
    }

    public static FileChannel open(FileDescriptor fd, String path, boolean readable, boolean writable, Object parent) {
        return new FileChannelImpl(fd, path, readable, writable, $assertionsDisabled, parent);
    }

    public static FileChannel open(FileDescriptor fd, String path, boolean readable, boolean writable, boolean append, Object parent) {
        return new FileChannelImpl(fd, path, readable, writable, append, parent);
    }

    private void ensureOpen() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    protected void implCloseChannel() throws IOException {
        this.guard.close();
        if (this.fileLockTable != null) {
            for (FileLock fl : this.fileLockTable.removeAll()) {
                synchronized (fl) {
                    if (fl.isValid()) {
                        this.nd.release(this.fd, fl.position(), fl.size());
                        ((FileLockImpl) fl).invalidate();
                    }
                }
            }
        }
        this.threads.signalAndWait();
        if (this.parent != null) {
            ((Closeable) this.parent).close();
        } else {
            this.nd.close(this.fd);
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.guard != null) {
                this.guard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    public int read(ByteBuffer dst) throws IOException {
        ensureOpen();
        if (this.readable) {
            synchronized (this.positionLock) {
                int n = 0;
                int ti = -1;
                boolean z = true;
                try {
                    begin();
                    ti = this.threads.add();
                    if (isOpen()) {
                        do {
                            n = IOUtil.read(this.fd, dst, -1, this.nd);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        int normalize = IOStatus.normalize(n);
                        this.threads.remove(ti);
                        if (n <= 0) {
                            z = $assertionsDisabled;
                        }
                        end(z);
                        return normalize;
                    }
                    this.threads.remove(ti);
                    if (null <= null) {
                        z = $assertionsDisabled;
                    }
                    end(z);
                    return 0;
                } catch (Throwable th) {
                    this.threads.remove(ti);
                    if (n <= 0) {
                        z = $assertionsDisabled;
                    }
                    end(z);
                }
            }
        } else {
            throw new NonReadableChannelException();
        }
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > dsts.length - length) {
            throw new IndexOutOfBoundsException();
        }
        ensureOpen();
        if (this.readable) {
            synchronized (this.positionLock) {
                long n = 0;
                int ti = -1;
                boolean z = $assertionsDisabled;
                try {
                    begin();
                    ti = this.threads.add();
                    if (isOpen()) {
                        do {
                            n = IOUtil.read(this.fd, dsts, offset, length, this.nd);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        long normalize = IOStatus.normalize(n);
                        this.threads.remove(ti);
                        if (n > 0) {
                            z = true;
                        }
                        end(z);
                        return normalize;
                    }
                    this.threads.remove(ti);
                    if (0 > 0) {
                        z = true;
                    }
                    end(z);
                    return 0;
                } catch (Throwable th) {
                    this.threads.remove(ti);
                    if (n > 0) {
                        z = true;
                    }
                    end(z);
                }
            }
        } else {
            throw new NonReadableChannelException();
        }
    }

    public int write(ByteBuffer src) throws IOException {
        ensureOpen();
        if (this.writable) {
            synchronized (this.positionLock) {
                int n = 0;
                int ti = -1;
                boolean z = true;
                try {
                    begin();
                    ti = this.threads.add();
                    if (isOpen()) {
                        do {
                            n = IOUtil.write(this.fd, src, -1, this.nd);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        int normalize = IOStatus.normalize(n);
                        this.threads.remove(ti);
                        if (n <= 0) {
                            z = $assertionsDisabled;
                        }
                        end(z);
                        return normalize;
                    }
                    this.threads.remove(ti);
                    if (null <= null) {
                        z = $assertionsDisabled;
                    }
                    end(z);
                    return 0;
                } catch (Throwable th) {
                    this.threads.remove(ti);
                    if (n <= 0) {
                        z = $assertionsDisabled;
                    }
                    end(z);
                }
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > srcs.length - length) {
            throw new IndexOutOfBoundsException();
        }
        ensureOpen();
        if (this.writable) {
            synchronized (this.positionLock) {
                long n = 0;
                int ti = -1;
                boolean z = $assertionsDisabled;
                try {
                    begin();
                    ti = this.threads.add();
                    if (isOpen()) {
                        do {
                            n = IOUtil.write(this.fd, srcs, offset, length, this.nd);
                            if (n != -3) {
                                break;
                            }
                        } while (isOpen());
                        long normalize = IOStatus.normalize(n);
                        this.threads.remove(ti);
                        if (n > 0) {
                            z = true;
                        }
                        end(z);
                        return normalize;
                    }
                    this.threads.remove(ti);
                    if (0 > 0) {
                        z = true;
                    }
                    end(z);
                    return 0;
                } catch (Throwable th) {
                    this.threads.remove(ti);
                    if (n > 0) {
                        z = true;
                    }
                    end(z);
                }
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    public long position() throws IOException {
        ensureOpen();
        synchronized (this.positionLock) {
            int ti = -1;
            boolean z = $assertionsDisabled;
            try {
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    long p;
                    if (this.append) {
                        BlockGuard.getThreadPolicy().onWriteToDisk();
                    }
                    do {
                        p = this.append ? this.nd.size(this.fd) : position0(this.fd, -1);
                        if (p != -3) {
                            break;
                        }
                    } while (isOpen());
                    long normalize = IOStatus.normalize(p);
                    this.threads.remove(ti);
                    if (p > -1) {
                        z = true;
                    }
                    end(z);
                    return normalize;
                }
                this.threads.remove(ti);
                if (-1 > -1) {
                    z = true;
                }
                end(z);
                return 0;
            } catch (Throwable th) {
                this.threads.remove(ti);
                if (-1 > -1) {
                    z = true;
                }
                end(z);
            }
        }
    }

    public FileChannel position(long newPosition) throws IOException {
        ensureOpen();
        if (newPosition >= 0) {
            synchronized (this.positionLock) {
                int ti = -1;
                boolean z = $assertionsDisabled;
                try {
                    begin();
                    ti = this.threads.add();
                    if (isOpen()) {
                        long p;
                        BlockGuard.getThreadPolicy().onReadFromDisk();
                        do {
                            p = position0(this.fd, newPosition);
                            if (p == -3) {
                            }
                            break;
                        } while (isOpen());
                        this.threads.remove(ti);
                        if (p > -1) {
                            z = true;
                        }
                        end(z);
                        return this;
                    }
                    this.threads.remove(ti);
                    if (-1 > -1) {
                        z = true;
                    }
                    end(z);
                    return null;
                } catch (Throwable th) {
                    this.threads.remove(ti);
                    if (-1 > -1) {
                        z = true;
                    }
                    end(z);
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public long size() throws IOException {
        ensureOpen();
        synchronized (this.positionLock) {
            long s = -1;
            int ti = -1;
            boolean z = $assertionsDisabled;
            try {
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    do {
                        s = this.nd.size(this.fd);
                        if (s != -3) {
                            break;
                        }
                    } while (isOpen());
                    long normalize = IOStatus.normalize(s);
                    this.threads.remove(ti);
                    if (s > -1) {
                        z = true;
                    }
                    end(z);
                    return normalize;
                }
                this.threads.remove(ti);
                if (-1 > -1) {
                    z = true;
                }
                end(z);
                return -1;
            } catch (Throwable th) {
                this.threads.remove(ti);
                if (s > -1) {
                    z = true;
                }
                end(z);
            }
        }
    }

    public FileChannel truncate(long newSize) throws IOException {
        long j = newSize;
        ensureOpen();
        if (j < 0) {
            throw new IllegalArgumentException("Negative size");
        } else if (this.writable) {
            synchronized (this.positionLock) {
                int rv = -1;
                int ti = -1;
                begin();
                ti = this.threads.add();
                if (isOpen()) {
                    long size;
                    do {
                        try {
                            size = this.nd.size(this.fd);
                            if (size != -3) {
                                break;
                            }
                        } catch (Throwable th) {
                            this.threads.remove(ti);
                            end(rv > -1 ? true : $assertionsDisabled);
                        }
                    } while (isOpen());
                    if (isOpen()) {
                        long p;
                        do {
                            p = position0(this.fd, -1);
                            if (p != -3) {
                                break;
                            }
                        } while (isOpen());
                        if (isOpen()) {
                            if (j < size) {
                                do {
                                    rv = this.nd.truncate(this.fd, j);
                                    if (rv != -3) {
                                        break;
                                    }
                                } while (isOpen());
                                if (!isOpen()) {
                                    this.threads.remove(ti);
                                    end(rv > -1 ? true : $assertionsDisabled);
                                    return null;
                                }
                            }
                            if (p > j) {
                                p = j;
                            }
                            do {
                                rv = (int) position0(this.fd, p);
                                if (rv == -3) {
                                }
                                break;
                            } while (isOpen());
                            this.threads.remove(ti);
                            end(rv > -1 ? true : $assertionsDisabled);
                            return this;
                        }
                        this.threads.remove(ti);
                        end(-1 > -1 ? true : $assertionsDisabled);
                        return null;
                    }
                    this.threads.remove(ti);
                    end(-1 > -1 ? true : $assertionsDisabled);
                    return null;
                }
                this.threads.remove(ti);
                end(-1 > -1 ? true : $assertionsDisabled);
                return null;
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    public void force(boolean metaData) throws IOException {
        ensureOpen();
        int rv = -1;
        int ti = -1;
        boolean z = $assertionsDisabled;
        try {
            begin();
            ti = this.threads.add();
            if (isOpen()) {
                while (true) {
                    rv = this.nd.force(this.fd, metaData);
                    if (rv == -3) {
                        if (!isOpen()) {
                            break;
                        }
                    }
                    break;
                }
                this.threads.remove(ti);
                if (rv > -1) {
                    z = true;
                }
                end(z);
            }
        } finally {
            this.threads.remove(ti);
            if (rv > -1) {
                z = true;
            }
            end(z);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x00ae  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00ae  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long transferToDirectlyInternal(long position, int icount, WritableByteChannel target, FileDescriptor targetFD) throws IOException {
        int ti;
        Throwable th;
        WritableByteChannel writableByteChannel = target;
        long n = -1;
        int ti2 = -1;
        boolean z = true;
        try {
            begin();
            ti = this.threads.add();
            try {
                if (isOpen()) {
                    BlockGuard.getThreadPolicy().onWriteToDisk();
                    do {
                        try {
                            n = transferTo0(this.fd, position, (long) icount, targetFD);
                            if (n != -3) {
                                break;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            n = n;
                            this.threads.remove(ti);
                            if (n <= -1) {
                            }
                            end(z);
                            throw th;
                        }
                    } while (isOpen());
                    if (n == -6) {
                        if (writableByteChannel instanceof SinkChannelImpl) {
                            pipeSupported = $assertionsDisabled;
                        }
                        if (writableByteChannel instanceof FileChannelImpl) {
                            fileSupported = $assertionsDisabled;
                        }
                        this.threads.remove(ti);
                        if (n <= -1) {
                            z = $assertionsDisabled;
                        }
                        end(z);
                        return -6;
                    } else if (n == -4) {
                        transferSupported = $assertionsDisabled;
                        this.threads.remove(ti);
                        if (n <= -1) {
                            z = $assertionsDisabled;
                        }
                        end(z);
                        return -4;
                    } else {
                        long normalize = IOStatus.normalize(n);
                        this.threads.remove(ti);
                        if (n <= -1) {
                            z = $assertionsDisabled;
                        }
                        end(z);
                        return normalize;
                    }
                }
                this.threads.remove(ti);
                if (-1 <= -1) {
                    z = $assertionsDisabled;
                }
                end(z);
                return -1;
            } catch (Throwable th3) {
                th = th3;
                this.threads.remove(ti);
                if (n <= -1) {
                    z = $assertionsDisabled;
                }
                end(z);
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            ti = ti2;
            this.threads.remove(ti);
            if (n <= -1) {
            }
            end(z);
            throw th;
        }
    }

    private long transferToDirectly(long position, int icount, WritableByteChannel target) throws IOException {
        WritableByteChannel writableByteChannel = target;
        if (!transferSupported) {
            return -4;
        }
        FileDescriptor targetFD = null;
        if (writableByteChannel instanceof FileChannelImpl) {
            if (!fileSupported) {
                return -6;
            }
            targetFD = ((FileChannelImpl) writableByteChannel).fd;
        } else if (writableByteChannel instanceof SelChImpl) {
            if ((writableByteChannel instanceof SinkChannelImpl) && !pipeSupported) {
                return -6;
            }
            if (!this.nd.canTransferToDirectly((SelectableChannel) writableByteChannel)) {
                return -6;
            }
            targetFD = ((SelChImpl) writableByteChannel).getFD();
        }
        FileDescriptor targetFD2 = targetFD;
        if (targetFD2 == null || IOUtil.fdVal(this.fd) == IOUtil.fdVal(targetFD2)) {
            return -4;
        }
        if (!this.nd.transferToDirectlyNeedsPositionLock()) {
            return transferToDirectlyInternal(position, icount, writableByteChannel, targetFD2);
        }
        long transferToDirectlyInternal;
        synchronized (this.positionLock) {
            long pos = position();
            try {
                transferToDirectlyInternal = transferToDirectlyInternal(position, icount, writableByteChannel, targetFD2);
                position(pos);
            } catch (Throwable th) {
                position(pos);
                Throwable th2 = th;
            }
        }
        return transferToDirectlyInternal;
    }

    private long transferToTrustedChannel(long position, long count, WritableByteChannel target) throws IOException {
        boolean isSelChImpl = target instanceof SelChImpl;
        if (!(target instanceof FileChannelImpl) && !isSelChImpl) {
            return -4;
        }
        long position2 = position;
        position = count;
        while (position > 0) {
            MappedByteBuffer dbb;
            try {
                dbb = map(MapMode.READ_ONLY, position2, Math.min(position, (long) MAPPED_TRANSFER_SIZE));
                int n = target.write(dbb);
                position -= (long) n;
                if (isSelChImpl) {
                    unmap(dbb);
                    break;
                }
                position2 += (long) n;
                unmap(dbb);
            } catch (ClosedByInterruptException e) {
                try {
                    close();
                } catch (Throwable suppressed) {
                    e.addSuppressed(suppressed);
                }
                throw e;
            } catch (IOException ioe) {
                if (position == count) {
                    throw ioe;
                }
            } catch (Throwable th) {
                unmap(dbb);
            }
        }
        return count - position;
    }

    private long transferToArbitraryChannel(long position, int icount, WritableByteChannel target) throws IOException {
        ByteBuffer bb = Util.getTemporaryDirectBuffer(Math.min(icount, 8192));
        long tw = 0;
        long pos = position;
        try {
            Util.erase(bb);
            while (tw < ((long) icount)) {
                bb.limit(Math.min((int) (((long) icount) - tw), 8192));
                int nr = read(bb, pos);
                if (nr <= 0) {
                    break;
                }
                bb.flip();
                int nw = target.write(bb);
                tw += (long) nw;
                if (nw != nr) {
                    break;
                }
                pos += (long) nw;
                bb.clear();
            }
            Util.releaseTemporaryDirectBuffer(bb);
            return tw;
        } catch (IOException x) {
            if (0 > 0) {
                Util.releaseTemporaryDirectBuffer(bb);
                return 0;
            }
            throw x;
        } catch (Throwable th) {
            Util.releaseTemporaryDirectBuffer(bb);
            throw th;
        }
    }

    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        long j = position;
        long j2 = count;
        WritableByteChannel writableByteChannel = target;
        ensureOpen();
        if (!target.isOpen()) {
            throw new ClosedChannelException();
        } else if (!this.readable) {
            throw new NonReadableChannelException();
        } else if ((writableByteChannel instanceof FileChannelImpl) && !((FileChannelImpl) writableByteChannel).writable) {
            throw new NonWritableChannelException();
        } else if (j < 0 || j2 < 0) {
            throw new IllegalArgumentException();
        } else {
            long sz = size();
            if (j > sz) {
                return 0;
            }
            int icount = (int) Math.min(j2, 2147483647L);
            if (sz - j < ((long) icount)) {
                icount = (int) (sz - j);
            }
            int icount2 = icount;
            long transferToDirectly = transferToDirectly(j, icount2, writableByteChannel);
            long n = transferToDirectly;
            if (transferToDirectly >= 0) {
                return n;
            }
            int icount3 = icount2;
            transferToDirectly = transferToTrustedChannel(j, (long) icount2, writableByteChannel);
            long n2 = transferToDirectly;
            if (transferToDirectly >= 0) {
                return n2;
            }
            return transferToArbitraryChannel(j, icount3, writableByteChannel);
        }
    }

    private long transferFromFileChannel(FileChannelImpl src, long position, long count) throws IOException {
        Throwable th;
        FileChannelImpl fileChannelImpl = src;
        if (fileChannelImpl.readable) {
            synchronized (fileChannelImpl.positionLock) {
                long position2;
                try {
                    long remaining;
                    long pos = src.position();
                    long remaining2 = Math.min(count, src.size() - pos);
                    long max = remaining2;
                    long position3 = position;
                    long remaining3 = remaining2;
                    remaining2 = pos;
                    while (true) {
                        long p = remaining2;
                        if (remaining3 <= 0) {
                            break;
                        }
                        try {
                            MappedByteBuffer bb;
                            position2 = position3;
                            try {
                                remaining = remaining3;
                                bb = fileChannelImpl.map(MapMode.READ_ONLY, p, Math.min(remaining3, (long) MAPPED_TRANSFER_SIZE));
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                            try {
                                remaining3 = (long) write(bb, position2);
                                p += remaining3;
                                position2 += remaining3;
                                remaining3 = remaining - remaining3;
                                unmap(bb);
                                position3 = position2;
                                remaining2 = p;
                                position2 = count;
                            } catch (IOException ioe) {
                                IOException iOException = ioe;
                                if (remaining != max) {
                                    unmap(bb);
                                } else {
                                    throw ioe;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            position2 = position3;
                            throw th;
                        }
                    }
                    remaining = remaining3;
                    position2 = position3;
                    remaining2 = max - remaining;
                    fileChannelImpl.position(pos + remaining2);
                    return remaining2;
                } catch (Throwable th5) {
                    th = th5;
                    position2 = position;
                    throw th;
                }
            }
        }
        throw new NonReadableChannelException();
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x006a A:{SYNTHETIC, Splitter:B:35:0x006a} */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0065  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0065  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x006a A:{SYNTHETIC, Splitter:B:35:0x006a} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long transferFromArbitraryChannel(ReadableByteChannel src, long position, long count) throws IOException {
        IOException x;
        Throwable th;
        long j = count;
        ByteBuffer bb = Util.getTemporaryDirectBuffer((int) Math.min(j, 8192));
        long tw = 0;
        long pos = position;
        ReadableByteChannel readableByteChannel;
        try {
            Util.erase(bb);
            while (tw < j) {
                bb.limit((int) Math.min(j - tw, 8192));
                try {
                    int nr = src.read(bb);
                    if (nr <= 0) {
                        break;
                    }
                    bb.flip();
                    try {
                        int nw = write(bb, pos);
                        tw += (long) nw;
                        if (nw != nr) {
                            break;
                        }
                        pos += (long) nw;
                        bb.clear();
                    } catch (IOException e) {
                        x = e;
                        if (tw > 0) {
                            Util.releaseTemporaryDirectBuffer(bb);
                            return tw;
                        }
                        try {
                            throw x;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                } catch (IOException e2) {
                    x = e2;
                    if (tw > 0) {
                    }
                } catch (Throwable th3) {
                    th = th3;
                    Util.releaseTemporaryDirectBuffer(bb);
                    throw th;
                }
            }
            readableByteChannel = src;
            Util.releaseTemporaryDirectBuffer(bb);
            return tw;
        } catch (IOException e3) {
            x = e3;
            readableByteChannel = src;
            if (tw > 0) {
            }
        } catch (Throwable th4) {
            th = th4;
            readableByteChannel = src;
            Util.releaseTemporaryDirectBuffer(bb);
            throw th;
        }
    }

    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        ensureOpen();
        if (!src.isOpen()) {
            throw new ClosedChannelException();
        } else if (!this.writable) {
            throw new NonWritableChannelException();
        } else if (position < 0 || count < 0) {
            throw new IllegalArgumentException();
        } else if (position > size()) {
            return 0;
        } else {
            if (!(src instanceof FileChannelImpl)) {
                return transferFromArbitraryChannel(src, position, count);
            }
            return transferFromFileChannel((FileChannelImpl) src, position, count);
        }
    }

    public int read(ByteBuffer dst, long position) throws IOException {
        if (dst == null) {
            throw new NullPointerException();
        } else if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        } else if (this.readable) {
            ensureOpen();
            if (!this.nd.needsPositionLock()) {
                return readInternal(dst, position);
            }
            int readInternal;
            synchronized (this.positionLock) {
                readInternal = readInternal(dst, position);
            }
            return readInternal;
        } else {
            throw new NonReadableChannelException();
        }
    }

    private int readInternal(ByteBuffer dst, long position) throws IOException {
        int n = 0;
        int ti = -1;
        boolean z = $assertionsDisabled;
        try {
            begin();
            ti = this.threads.add();
            if (!isOpen()) {
                return -1;
            }
            while (true) {
                n = IOUtil.read(this.fd, dst, position, this.nd);
                if (n == -3) {
                    if (!isOpen()) {
                        break;
                    }
                }
                break;
            }
            int normalize = IOStatus.normalize(n);
            this.threads.remove(ti);
            if (n > 0) {
                z = true;
            }
            end(z);
            return normalize;
        } finally {
            this.threads.remove(ti);
            if (n > 0) {
                z = true;
            }
            end(z);
        }
    }

    public int write(ByteBuffer src, long position) throws IOException {
        if (src == null) {
            throw new NullPointerException();
        } else if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        } else if (this.writable) {
            ensureOpen();
            if (!this.nd.needsPositionLock()) {
                return writeInternal(src, position);
            }
            int writeInternal;
            synchronized (this.positionLock) {
                writeInternal = writeInternal(src, position);
            }
            return writeInternal;
        } else {
            throw new NonWritableChannelException();
        }
    }

    private int writeInternal(ByteBuffer src, long position) throws IOException {
        int n = 0;
        int ti = -1;
        boolean z = $assertionsDisabled;
        try {
            begin();
            ti = this.threads.add();
            if (!isOpen()) {
                return -1;
            }
            while (true) {
                n = IOUtil.write(this.fd, src, position, this.nd);
                if (n == -3) {
                    if (!isOpen()) {
                        break;
                    }
                }
                break;
            }
            int normalize = IOStatus.normalize(n);
            this.threads.remove(ti);
            if (n > 0) {
                z = true;
            }
            end(z);
            return normalize;
        } finally {
            this.threads.remove(ti);
            if (n > 0) {
                z = true;
            }
            end(z);
        }
    }

    private static void unmap(MappedByteBuffer bb) {
        Cleaner cl = ((DirectBuffer) bb).cleaner();
        if (cl != null) {
            cl.clean();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:123:0x018f A:{Catch:{ IOException -> 0x01b0, all -> 0x01ad }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x00dd  */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x0122 A:{SYNTHETIC, Splitter:B:92:0x0122} */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x00f3  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        Throwable th;
        int ti;
        int pagePosition;
        long addr;
        OutOfMemoryError e;
        long addr2;
        FileDescriptor mfd;
        Unmapper unmapper;
        long j;
        boolean z;
        DirectByteBuffer directByteBuffer;
        MapMode mapMode = mode;
        long j2 = size;
        ensureOpen();
        if (mapMode == null) {
            throw new NullPointerException("Mode is null");
        } else if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        } else if (j2 < 0) {
            throw new IllegalArgumentException("Negative size");
        } else if (position + j2 < 0) {
            throw new IllegalArgumentException("Position + size overflow");
        } else if (j2 <= 2147483647L) {
            int imode = -1;
            if (mapMode == MapMode.READ_ONLY) {
                imode = 0;
            } else if (mapMode == MapMode.READ_WRITE) {
                imode = 1;
            } else if (mapMode == MapMode.PRIVATE) {
                imode = 2;
            }
            int imode2 = imode;
            if (mapMode != MapMode.READ_ONLY && !this.writable) {
                throw new NonWritableChannelException();
            } else if (this.readable) {
                long addr3 = -1;
                int ti2 = -1;
                try {
                    begin();
                    int ti3 = this.threads.add();
                    if (isOpen()) {
                        long filesize;
                        do {
                            try {
                                filesize = this.nd.size(this.fd);
                                if (filesize != -3) {
                                    break;
                                }
                                try {
                                } catch (IOException e2) {
                                    IOException r = e2;
                                    if (OsConstants.S_ISREG(Libcore.os.fstat(this.fd).st_mode)) {
                                        throw r;
                                    }
                                } catch (ErrnoException e3) {
                                    e3.rethrowAsIOException();
                                } catch (Throwable th2) {
                                    th = th2;
                                    ti = ti3;
                                    this.threads.remove(ti);
                                    end(IOStatus.checkAll(addr3));
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                ti = ti3;
                                this.threads.remove(ti);
                                end(IOStatus.checkAll(addr3));
                                throw th;
                            }
                        } while (isOpen());
                        if (isOpen()) {
                            if (filesize < position + j2) {
                                imode = 0;
                                while (true) {
                                    int rv = imode;
                                    imode = this.nd.truncate(this.fd, position + j2);
                                    if (imode != -3) {
                                        break;
                                    } else if (!isOpen()) {
                                        break;
                                    }
                                }
                                if (!isOpen()) {
                                    this.threads.remove(ti3);
                                    end(IOStatus.checkAll(-1));
                                    return null;
                                }
                            }
                            if (j2 != 0) {
                                boolean z2;
                                DirectByteBuffer directByteBuffer2;
                                addr3 = 0;
                                FileDescriptor dummy = new FileDescriptor();
                                if (this.writable) {
                                    if (imode2 != 0) {
                                        z2 = $assertionsDisabled;
                                        directByteBuffer2 = new DirectByteBuffer(0, 0, dummy, null, z2);
                                        this.threads.remove(ti3);
                                        end(IOStatus.checkAll(0));
                                        return directByteBuffer2;
                                    }
                                }
                                z2 = true;
                                directByteBuffer2 = new DirectByteBuffer(0, 0, dummy, null, z2);
                                this.threads.remove(ti3);
                                end(IOStatus.checkAll(0));
                                return directByteBuffer2;
                            }
                            long mapSize;
                            int pagePosition2 = (int) (position % allocationGranularity);
                            long mapPosition = position - ((long) pagePosition2);
                            long mapSize2 = ((long) pagePosition2) + j2;
                            try {
                                BlockGuard.getThreadPolicy().onReadFromDisk();
                                mapSize = mapSize2;
                                ti = ti3;
                                pagePosition = pagePosition2;
                                try {
                                    addr = map0(imode2, mapPosition, mapSize);
                                } catch (OutOfMemoryError e4) {
                                    e = e4;
                                }
                            } catch (OutOfMemoryError e5) {
                                e = e5;
                                mapSize = mapSize2;
                                ti = ti3;
                                pagePosition = pagePosition2;
                                OutOfMemoryError x = e;
                                try {
                                    System.gc();
                                    Thread.sleep(100);
                                } catch (OutOfMemoryError e6) {
                                    ti2 = mapSize;
                                    OutOfMemoryError outOfMemoryError = e6;
                                    throw new IOException("Map failed", e6);
                                } catch (InterruptedException y) {
                                    InterruptedException interruptedException = y;
                                    Thread.currentThread().interrupt();
                                } catch (Throwable th4) {
                                    th = th4;
                                    this.threads.remove(ti);
                                    end(IOStatus.checkAll(addr3));
                                    throw th;
                                }
                                addr = map0(imode2, mapPosition, mapSize);
                                addr2 = addr;
                                mfd = this.nd.duplicateForMapping(this.fd);
                                imode = (int) j2;
                                unmapper = new Unmapper(addr2, mapSize, imode, mfd);
                                j = addr2 + ((long) pagePosition);
                                if (this.writable) {
                                }
                                z = true;
                                directByteBuffer = new DirectByteBuffer(imode, j, mfd, unmapper, z);
                                this.threads.remove(ti);
                                end(IOStatus.checkAll(addr2));
                                return directByteBuffer;
                            }
                            addr2 = addr;
                            try {
                                mfd = this.nd.duplicateForMapping(this.fd);
                                imode = (int) j2;
                                unmapper = new Unmapper(addr2, mapSize, imode, mfd);
                                j = addr2 + ((long) pagePosition);
                                if (this.writable) {
                                    if (imode2 != 0) {
                                        z = $assertionsDisabled;
                                        directByteBuffer = new DirectByteBuffer(imode, j, mfd, unmapper, z);
                                        this.threads.remove(ti);
                                        end(IOStatus.checkAll(addr2));
                                        return directByteBuffer;
                                    }
                                }
                                z = true;
                                directByteBuffer = new DirectByteBuffer(imode, j, mfd, unmapper, z);
                                this.threads.remove(ti);
                                end(IOStatus.checkAll(addr2));
                                return directByteBuffer;
                            } catch (IOException e22) {
                                unmap0(addr2, mapSize);
                                throw e22;
                            } catch (Throwable th5) {
                                th = th5;
                                addr3 = addr2;
                                this.threads.remove(ti);
                                end(IOStatus.checkAll(addr3));
                                throw th;
                            }
                        }
                        this.threads.remove(ti3);
                        end(IOStatus.checkAll(-1));
                        return null;
                    }
                    this.threads.remove(ti3);
                    end(IOStatus.checkAll(-1));
                    return null;
                } catch (Throwable th6) {
                    th = th6;
                    ti = ti2;
                    this.threads.remove(ti);
                    end(IOStatus.checkAll(addr3));
                    throw th;
                }
            } else {
                throw new NonReadableChannelException();
            }
        } else {
            throw new IllegalArgumentException("Size exceeds Integer.MAX_VALUE");
        }
        if (isOpen()) {
        }
        if (j2 != 0) {
        }
    }

    private static boolean isSharedFileLockTable() {
        if (!propertyChecked) {
            synchronized (FileChannelImpl.class) {
                if (!propertyChecked) {
                    boolean z;
                    String value = (String) AccessController.doPrivileged(new GetPropertyAction("sun.nio.ch.disableSystemWideOverlappingFileLockCheck"));
                    if (value != null) {
                        if (!value.equals("false")) {
                            z = $assertionsDisabled;
                            isSharedFileLockTable = z;
                            propertyChecked = true;
                        }
                    }
                    z = true;
                    isSharedFileLockTable = z;
                    propertyChecked = true;
                }
            }
        }
        return isSharedFileLockTable;
    }

    private FileLockTable fileLockTable() throws IOException {
        if (this.fileLockTable == null) {
            synchronized (this) {
                if (this.fileLockTable == null) {
                    if (isSharedFileLockTable()) {
                        int ti = this.threads.add();
                        try {
                            ensureOpen();
                            this.fileLockTable = FileLockTable.newSharedFileLockTable(this, this.fd);
                        } finally {
                            this.threads.remove(ti);
                        }
                    } else {
                        this.fileLockTable = new SimpleFileLockTable();
                    }
                }
            }
        }
        return this.fileLockTable;
    }

    /* JADX WARNING: Removed duplicated region for block: B:65:0x00d3  */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x00d3  */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x00d3  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        Throwable th;
        int ti;
        boolean completed;
        ClosedByInterruptException closedByInterruptException;
        ensureOpen();
        if (shared && !this.readable) {
            throw new NonReadableChannelException();
        } else if (shared || this.writable) {
            FileLockImpl fli = new FileLockImpl(this, position, size, shared);
            FileLockTable flt = fileLockTable();
            flt.add(fli);
            int ti2 = -1;
            FileLockTable flt2;
            FileLockImpl fli2;
            try {
                begin();
                int ti3 = this.threads.add();
                if (isOpen()) {
                    int n;
                    do {
                        try {
                            n = this.nd.lock(this.fd, true, position, size, shared);
                            if (n == 2) {
                                try {
                                } catch (Throwable th2) {
                                    th = th2;
                                    ti = ti3;
                                    flt2 = flt;
                                    completed = $assertionsDisabled;
                                    fli2 = fli;
                                    if (!completed) {
                                    }
                                    this.threads.remove(ti);
                                    try {
                                        end(completed);
                                        throw th;
                                    } catch (ClosedByInterruptException e) {
                                        closedByInterruptException = e;
                                        throw new FileLockInterruptionException();
                                    }
                                }
                            }
                            break;
                        } catch (Throwable th3) {
                            th = th3;
                            ti = ti3;
                            flt2 = flt;
                            completed = $assertionsDisabled;
                            fli2 = fli;
                            if (completed) {
                            }
                            this.threads.remove(ti);
                            end(completed);
                            throw th;
                        }
                    } while (isOpen());
                    if (isOpen()) {
                        if (n == 1) {
                            FileLockImpl fileLockImpl = fileLockImpl;
                            ti = ti3;
                            long j = position;
                            flt2 = flt;
                            completed = $assertionsDisabled;
                            fli2 = fli;
                            try {
                                fileLockImpl = new FileLockImpl(this, j, size, (boolean) null);
                                flt2.replace(fli2, fileLockImpl);
                                fli = fileLockImpl;
                            } catch (Throwable th4) {
                                th = th4;
                                if (completed) {
                                    flt2.remove(fli2);
                                }
                                this.threads.remove(ti);
                                end(completed);
                                throw th;
                            }
                        }
                        ti = ti3;
                        flt2 = flt;
                        completed = $assertionsDisabled;
                        fli2 = fli;
                        completed = true;
                        fli2 = fli;
                    } else {
                        ti = ti3;
                        flt2 = flt;
                        completed = $assertionsDisabled;
                        fli2 = fli;
                    }
                    if (!completed) {
                        flt2.remove(fli2);
                    }
                    this.threads.remove(ti);
                    try {
                        end(completed);
                        return fli2;
                    } catch (ClosedByInterruptException e2) {
                        closedByInterruptException = e2;
                        throw new FileLockInterruptionException();
                    }
                }
                if (null == null) {
                    flt.remove(fli);
                }
                this.threads.remove(ti3);
                try {
                    end($assertionsDisabled);
                    return null;
                } catch (ClosedByInterruptException e22) {
                    closedByInterruptException = e22;
                    throw new FileLockInterruptionException();
                }
            } catch (Throwable th5) {
                th = th5;
                flt2 = flt;
                completed = $assertionsDisabled;
                fli2 = fli;
                ti = ti2;
                if (completed) {
                }
                this.threads.remove(ti);
                end(completed);
                throw th;
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        Throwable th;
        int ti;
        ensureOpen();
        if (shared && !this.readable) {
            throw new NonReadableChannelException();
        } else if (shared || this.writable) {
            FileLockImpl fli = new FileLockImpl(this, position, size, shared);
            FileLockTable flt = fileLockTable();
            flt.add(fli);
            int ti2 = this.threads.add();
            try {
                ensureOpen();
                int result = this.nd.lock(this.fd, $assertionsDisabled, position, size, shared);
                FileLockTable fileLockTable;
                FileLockImpl fileLockImpl;
                if (result == -1) {
                    try {
                        flt.remove(fli);
                        this.threads.remove(ti2);
                        return null;
                    } catch (Throwable th2) {
                        th = th2;
                        fileLockTable = flt;
                        ti = ti2;
                        fileLockImpl = fli;
                        this.threads.remove(ti);
                        throw th;
                    }
                } else if (result == 1) {
                    try {
                        FileLockImpl fileLockImpl2 = fileLockImpl2;
                        fileLockTable = flt;
                        ti = ti2;
                        fileLockImpl = fli;
                        fileLockImpl2 = new FileLockImpl(this, position, size, (boolean) null);
                        fileLockTable.replace(fileLockImpl, fileLockImpl2);
                        this.threads.remove(ti);
                        return fileLockImpl2;
                    } catch (Throwable th3) {
                        th = th3;
                        fileLockTable = flt;
                        ti = ti2;
                        fileLockImpl = fli;
                        this.threads.remove(ti);
                        throw th;
                    }
                } else {
                    fileLockImpl = fli;
                    this.threads.remove(ti2);
                    return fileLockImpl;
                }
            } catch (IOException e) {
                ti = ti2;
                flt.remove(fli);
                throw e;
            } catch (Throwable th4) {
                th = th4;
                this.threads.remove(ti);
                throw th;
            }
        } else {
            throw new NonWritableChannelException();
        }
    }

    void release(FileLockImpl fli) throws IOException {
        int ti = this.threads.add();
        try {
            ensureOpen();
            this.nd.release(this.fd, fli.position(), fli.size());
            this.fileLockTable.remove(fli);
        } finally {
            this.threads.remove(ti);
        }
    }
}
