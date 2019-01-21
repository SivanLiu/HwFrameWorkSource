package android.util.apk;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.DirectByteBuffer;
import java.security.DigestException;

class MemoryMappedFileDataSource implements DataSource {
    private static final long MEMORY_PAGE_SIZE_BYTES = Os.sysconf(OsConstants._SC_PAGESIZE);
    private final FileDescriptor mFd;
    private final long mFilePosition;
    private final long mSize;

    MemoryMappedFileDataSource(FileDescriptor fd, long position, long size) {
        this.mFd = fd;
        this.mFilePosition = position;
        this.mSize = size;
    }

    public long size() {
        return this.mSize;
    }

    /* JADX WARNING: Removed duplicated region for block: B:49:0x00c8 A:{SYNTHETIC, Splitter:B:49:0x00c8} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00c8 A:{SYNTHETIC, Splitter:B:49:0x00c8} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00c8 A:{SYNTHETIC, Splitter:B:49:0x00c8} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00c8 A:{SYNTHETIC, Splitter:B:49:0x00c8} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00c8 A:{SYNTHETIC, Splitter:B:49:0x00c8} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void feedIntoDataDigester(DataDigester md, long offset, int size) throws IOException, DigestException {
        long mmapPtr;
        long j;
        ErrnoException e;
        DataDigester dataDigester;
        long mmapRegionSize;
        StringBuilder stringBuilder;
        Throwable th;
        Throwable th2;
        long j2;
        long filePosition = this.mFilePosition + offset;
        long mmapFilePosition = (filePosition / MEMORY_PAGE_SIZE_BYTES) * MEMORY_PAGE_SIZE_BYTES;
        int dataStartOffsetInMmapRegion = (int) (filePosition - mmapFilePosition);
        long mmapRegionSize2 = (long) (size + dataStartOffsetInMmapRegion);
        long mmapPtr2 = 0;
        try {
            FileDescriptor fileDescriptor;
            long mmapRegionSize3 = mmapRegionSize2;
            try {
                mmapRegionSize2 = Os.mmap(0, mmapRegionSize2, OsConstants.PROT_READ, OsConstants.MAP_SHARED | OsConstants.MAP_POPULATE, this.mFd, mmapFilePosition);
                try {
                    mmapPtr = mmapRegionSize2 + ((long) dataStartOffsetInMmapRegion);
                    fileDescriptor = this.mFd;
                    DirectByteBuffer directByteBuffer = directByteBuffer;
                    j = mmapRegionSize2;
                    try {
                    } catch (ErrnoException e2) {
                        e = e2;
                        dataDigester = md;
                        mmapRegionSize = mmapRegionSize3;
                        mmapPtr2 = j;
                        try {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to mmap ");
                            stringBuilder.append(mmapRegionSize);
                            stringBuilder.append(" bytes");
                            throw new IOException(stringBuilder.toString(), e);
                        } catch (Throwable th22) {
                            th = th22;
                            mmapPtr = mmapPtr2;
                            if (mmapPtr != 0) {
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th22 = th3;
                        dataDigester = md;
                        mmapRegionSize = mmapRegionSize3;
                        mmapPtr = j;
                        th = th22;
                        if (mmapPtr != 0) {
                        }
                        throw th;
                    }
                } catch (ErrnoException e3) {
                    e = e3;
                    j2 = filePosition;
                    mmapRegionSize = mmapRegionSize3;
                    dataDigester = md;
                    mmapPtr2 = mmapRegionSize2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to mmap ");
                    stringBuilder.append(mmapRegionSize);
                    stringBuilder.append(" bytes");
                    throw new IOException(stringBuilder.toString(), e);
                } catch (Throwable th222) {
                    j2 = filePosition;
                    mmapRegionSize = mmapRegionSize3;
                    dataDigester = md;
                    mmapPtr = mmapRegionSize2;
                    th = th222;
                    if (mmapPtr != 0) {
                    }
                    throw th;
                }
            } catch (ErrnoException e4) {
                e = e4;
                j2 = filePosition;
                mmapRegionSize = mmapRegionSize3;
                dataDigester = md;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to mmap ");
                stringBuilder.append(mmapRegionSize);
                stringBuilder.append(" bytes");
                throw new IOException(stringBuilder.toString(), e);
            } catch (Throwable th2222) {
                j2 = filePosition;
                mmapRegionSize = mmapRegionSize3;
                dataDigester = md;
                th = th2222;
                mmapPtr = mmapPtr2;
                if (mmapPtr != 0) {
                }
                throw th;
            }
            try {
                md.consume(new DirectByteBuffer(size, mmapPtr, fileDescriptor, null, true));
                if (j != 0) {
                    try {
                        Os.munmap(j, mmapRegionSize3);
                        return;
                    } catch (ErrnoException e5) {
                        return;
                    }
                }
            } catch (ErrnoException e6) {
                e = e6;
                mmapRegionSize = mmapRegionSize3;
                mmapPtr2 = j;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to mmap ");
                stringBuilder.append(mmapRegionSize);
                stringBuilder.append(" bytes");
                throw new IOException(stringBuilder.toString(), e);
            } catch (Throwable th4) {
                th2222 = th4;
                mmapRegionSize = mmapRegionSize3;
                mmapPtr = j;
                th = th2222;
                if (mmapPtr != 0) {
                    try {
                        Os.munmap(mmapPtr, mmapRegionSize);
                    } catch (ErrnoException e7) {
                    }
                }
                throw th;
            }
        } catch (ErrnoException e8) {
            e = e8;
            j2 = filePosition;
            mmapRegionSize = mmapRegionSize2;
            dataDigester = md;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to mmap ");
            stringBuilder.append(mmapRegionSize);
            stringBuilder.append(" bytes");
            throw new IOException(stringBuilder.toString(), e);
        } catch (Throwable th22222) {
            j2 = filePosition;
            mmapRegionSize = mmapRegionSize2;
            dataDigester = md;
            th = th22222;
            mmapPtr = mmapPtr2;
            if (mmapPtr != 0) {
            }
            throw th;
        }
    }
}
