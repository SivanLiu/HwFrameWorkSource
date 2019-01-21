package com.android.server.rms.iaware.memory.utils;

import android.rms.iaware.AwareLog;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;

public class MemoryLockFile {
    private static final String TAG = "AwareMem_MemLockFile";
    private final ArrayList<PinnedFile> mPinnedFiles = new ArrayList();

    private static class PinnedFile {
        long mAddress;
        String mFilename;
        long mLength;

        PinnedFile(long address, long length, String filename) {
            this.mAddress = address;
            this.mLength = length;
            this.mFilename = normalize(filename);
        }

        private String normalize(String path) {
            return new File(path.trim()).getName();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:44:0x00f8 A:{SYNTHETIC, Splitter:B:44:0x00f8} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00f8 A:{SYNTHETIC, Splitter:B:44:0x00f8} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00f8 A:{SYNTHETIC, Splitter:B:44:0x00f8} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00f8 A:{SYNTHETIC, Splitter:B:44:0x00f8} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00f8 A:{SYNTHETIC, Splitter:B:44:0x00f8} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private PinnedFile pinFile(String fileToPin, long offset, long length, long maxSize) {
        ErrnoException e;
        long j;
        ErrnoException e2;
        String str;
        StringBuilder stringBuilder;
        FileDescriptor fileDescriptor;
        String str2 = fileToPin;
        long j2 = maxSize;
        FileDescriptor fd = new FileDescriptor();
        FileDescriptor fd2;
        try {
            fd2 = Os.open(str2, (OsConstants.O_RDONLY | OsConstants.O_CLOEXEC) | OsConstants.O_NOFOLLOW, OsConstants.O_RDONLY);
            try {
                StructStat sb = Os.fstat(fd2);
                StringBuilder stringBuilder2;
                if (offset + length > sb.st_size) {
                    try {
                        Os.close(fd2);
                        String str3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Failed to pin file ");
                        stringBuilder2.append(str2);
                        stringBuilder2.append(", request extends beyond end of file.  offset + length =  ");
                        stringBuilder2.append(offset + length);
                        stringBuilder2.append(", file length = ");
                        stringBuilder2.append(sb.st_size);
                        AwareLog.e(str3, stringBuilder2.toString());
                        return null;
                    } catch (ErrnoException e3) {
                        e = e3;
                        j = length;
                        e2 = e;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not pin file ");
                        stringBuilder.append(str2);
                        stringBuilder.append(" with error ");
                        stringBuilder.append(e2.getMessage());
                        AwareLog.e(str, stringBuilder.toString());
                        if (fd2.valid()) {
                        }
                        return null;
                    }
                }
                long length2;
                if (length == 0) {
                    length2 = sb.st_size - offset;
                } else {
                    length2 = length;
                }
                if (j2 <= 0 || length2 <= j2) {
                    long address;
                    long length3;
                    try {
                        address = Os.mmap(0, length2, OsConstants.PROT_READ, OsConstants.MAP_PRIVATE, fd2, offset);
                        Os.close(fd2);
                        Os.mlock(address, length2);
                        PinnedFile pinnedFile = pinnedFile;
                        length3 = length2;
                    } catch (ErrnoException e4) {
                        e = e4;
                        fileDescriptor = fd2;
                        j = length2;
                        e2 = e;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not pin file ");
                        stringBuilder.append(str2);
                        stringBuilder.append(" with error ");
                        stringBuilder.append(e2.getMessage());
                        AwareLog.e(str, stringBuilder.toString());
                        if (fd2.valid()) {
                            try {
                                Os.close(fd2);
                            } catch (ErrnoException e5) {
                                ErrnoException errnoException = e5;
                                String str4 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Failed to close fd, error = ");
                                stringBuilder3.append(e5.getMessage());
                                AwareLog.e(str4, stringBuilder3.toString());
                            }
                        }
                        return null;
                    }
                    try {
                        return new PinnedFile(address, length3, str2);
                    } catch (ErrnoException e6) {
                        e5 = e6;
                        j = length3;
                        fd2 = fd2;
                        e2 = e5;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not pin file ");
                        stringBuilder.append(str2);
                        stringBuilder.append(" with error ");
                        stringBuilder.append(e2.getMessage());
                        AwareLog.e(str, stringBuilder.toString());
                        if (fd2.valid()) {
                        }
                        return null;
                    }
                }
                try {
                    String str5 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Could not pin file ");
                    stringBuilder2.append(str2);
                    stringBuilder2.append(", size = ");
                    stringBuilder2.append(length2);
                    stringBuilder2.append(", maxSize = ");
                    stringBuilder2.append(j2);
                    AwareLog.e(str5, stringBuilder2.toString());
                    Os.close(fd2);
                    return null;
                } catch (ErrnoException e7) {
                    e5 = e7;
                    j = length2;
                    e2 = e5;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Could not pin file ");
                    stringBuilder.append(str2);
                    stringBuilder.append(" with error ");
                    stringBuilder.append(e2.getMessage());
                    AwareLog.e(str, stringBuilder.toString());
                    if (fd2.valid()) {
                    }
                    return null;
                }
            } catch (ErrnoException e8) {
                e5 = e8;
                fileDescriptor = fd2;
                j = length;
                e2 = e5;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not pin file ");
                stringBuilder.append(str2);
                stringBuilder.append(" with error ");
                stringBuilder.append(e2.getMessage());
                AwareLog.e(str, stringBuilder.toString());
                if (fd2.valid()) {
                }
                return null;
            }
        } catch (ErrnoException e9) {
            e5 = e9;
            j = length;
            fd2 = fd;
            e2 = e5;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not pin file ");
            stringBuilder.append(str2);
            stringBuilder.append(" with error ");
            stringBuilder.append(e2.getMessage());
            AwareLog.e(str, stringBuilder.toString());
            if (fd2.valid()) {
            }
            return null;
        }
    }

    private void unpinFile(PinnedFile pf) {
        try {
            Os.munlock(pf.mAddress, pf.mLength);
        } catch (ErrnoException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to unpin file with error ");
            stringBuilder.append(e.getMessage());
            AwareLog.e(str, stringBuilder.toString());
        }
    }

    public void iAwareAddPinFile() {
        ArrayList<String> filesToPin = MemoryConstant.getFilesToPin();
        int fileNumSize = filesToPin.size();
        for (int i = 0; i < fileNumSize; i++) {
            PinnedFile pf = pinFile((String) filesToPin.get(i), 0, 0, 0);
            if (pf != null) {
                this.mPinnedFiles.add(pf);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Pinned file ");
                stringBuilder.append(pf.mFilename);
                stringBuilder.append("ok");
                AwareLog.d(str, stringBuilder.toString());
            } else {
                AwareLog.e(TAG, "Failed to pin file");
            }
        }
    }

    public void clearPinFile() {
        int pinnedFileNum = this.mPinnedFiles.size();
        for (int i = 0; i < pinnedFileNum; i++) {
            unpinFile((PinnedFile) this.mPinnedFiles.get(i));
        }
        this.mPinnedFiles.clear();
    }
}
