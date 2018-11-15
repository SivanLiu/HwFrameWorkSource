package com.android.server.rms.io;

import android.os.FileUtils;
import android.rms.utils.Utils;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import libcore.io.IoUtils;

public class IOFileRotator {
    private static final int MAX_FILE_SIZE = 262144;
    private static long MAX_SIZE_BASE_PATH = 2097152;
    private static final String SUFFIX_BACKUP = ".backup";
    private static final String SUFFIX_NO_BACKUP = ".no_backup";
    private static final String TAG = "RMS.IO.FileRotator";
    private static long mMaxSizeForBasePath = MAX_SIZE_BASE_PATH;
    private final File mBasePath;
    private final long mDeleteAgeMillis;
    private long mMaxFileSize;
    private final String mPrefix;
    private final long mRotateAgeMillis;

    private static class FileInfo {
        public long endMillis;
        public final String prefix;
        public long startMillis;

        public FileInfo(String prefix) {
            this.prefix = (String) Preconditions.checkNotNull(prefix);
        }

        public boolean parse(String name) {
            this.endMillis = -1;
            this.startMillis = -1;
            int dotIndex = name.lastIndexOf(46);
            int dashIndex = name.lastIndexOf(45);
            String str;
            StringBuilder stringBuilder;
            if (dotIndex == -1 || dashIndex == -1) {
                if (Utils.DEBUG) {
                    str = IOFileRotator.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("FileInfo.parse,name:");
                    stringBuilder.append(name);
                    stringBuilder.append(" missing time section");
                    Log.d(str, stringBuilder.toString());
                }
                return false;
            } else if (this.prefix.equals(name.substring(0, dotIndex))) {
                try {
                    this.startMillis = Long.parseLong(name.substring(dotIndex + 1, dashIndex));
                    if (name.length() - dashIndex == 1) {
                        this.endMillis = Long.MAX_VALUE;
                    } else {
                        this.endMillis = Long.parseLong(name.substring(dashIndex + 1));
                    }
                    return true;
                } catch (NumberFormatException e) {
                    String str2 = IOFileRotator.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("FileInfo.parse,name:");
                    stringBuilder2.append(name);
                    stringBuilder2.append(" NumberFormatException");
                    Slog.e(str2, stringBuilder2.toString());
                    return false;
                }
            } else {
                if (Utils.DEBUG) {
                    str = IOFileRotator.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("FileInfo.parse,name:");
                    stringBuilder.append(name);
                    stringBuilder.append(" prefix doesn't match");
                    Log.d(str, stringBuilder.toString());
                }
                return false;
            }
        }

        public String build() {
            StringBuilder name = new StringBuilder();
            name.append(this.prefix);
            name.append('.');
            name.append(this.startMillis);
            name.append('-');
            if (this.endMillis != Long.MAX_VALUE) {
                name.append(this.endMillis);
            }
            return name.toString();
        }

        public boolean isActive() {
            return this.endMillis == Long.MAX_VALUE;
        }
    }

    public interface Reader {
        void read(InputStream inputStream) throws IOException;
    }

    public interface Writer {
        void write(OutputStream outputStream) throws IOException;
    }

    public interface Rewriter extends Reader, Writer {
        void reset();

        boolean shouldWrite();
    }

    private static class RewriterDef implements Rewriter {
        private Reader mReader = null;
        private Writer mWriter = null;

        public RewriterDef(Reader reader, Writer writer) {
            this.mReader = reader;
            this.mWriter = writer;
        }

        public void reset() {
        }

        public void read(InputStream in) throws IOException {
            if (this.mReader == null) {
                Log.e(IOFileRotator.TAG, "RewriterDef,the reader is null");
            } else {
                this.mReader.read(in);
            }
        }

        public boolean shouldWrite() {
            return true;
        }

        public void write(OutputStream out) throws IOException {
            if (this.mWriter == null) {
                Log.e(IOFileRotator.TAG, "RewriterDef,the Writer is null");
            } else {
                this.mWriter.write(out);
            }
        }
    }

    public IOFileRotator(File basePath, String prefix, long rotateAgeMillis, long deleteAgeMillis, long maxFileSize) {
        this(basePath, prefix, rotateAgeMillis, deleteAgeMillis);
        this.mMaxFileSize = maxFileSize;
    }

    public IOFileRotator(File basePath, String prefix, long rotateAgeMillis, long deleteAgeMillis) {
        this.mMaxFileSize = 262144;
        this.mBasePath = (File) Preconditions.checkNotNull(basePath);
        this.mPrefix = (String) Preconditions.checkNotNull(prefix);
        this.mRotateAgeMillis = rotateAgeMillis;
        this.mDeleteAgeMillis = deleteAgeMillis;
        if (!(this.mBasePath.exists() || this.mBasePath.mkdirs())) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IOFileRotator,fail to create the directory:");
            stringBuilder.append(this.mBasePath);
            Log.e(str, stringBuilder.toString());
        }
        for (String name : getBasePathFileList()) {
            if (name.startsWith(this.mPrefix)) {
                String str2;
                StringBuilder stringBuilder2;
                File backupFile;
                String str3;
                StringBuilder stringBuilder3;
                if (name.endsWith(SUFFIX_BACKUP)) {
                    if (Utils.DEBUG) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("recovering ");
                        stringBuilder2.append(name);
                        Log.d(str2, stringBuilder2.toString());
                    }
                    backupFile = new File(this.mBasePath, name);
                    if (!backupFile.renameTo(new File(this.mBasePath, name.substring(0, name.length() - SUFFIX_BACKUP.length())))) {
                        str3 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("IOFileRotator,fail to renameTo,file:");
                        stringBuilder3.append(backupFile.getName());
                        Log.e(str3, stringBuilder3.toString());
                    }
                } else if (name.endsWith(SUFFIX_NO_BACKUP)) {
                    if (Utils.DEBUG) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("recovering ");
                        stringBuilder2.append(name);
                        Log.d(str2, stringBuilder2.toString());
                    }
                    backupFile = new File(this.mBasePath, name);
                    File file = new File(this.mBasePath, name.substring(0, name.length() - SUFFIX_NO_BACKUP.length()));
                    if (!backupFile.delete()) {
                        str3 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("IOFileRotator,fail to delete,file:");
                        stringBuilder3.append(backupFile.getName());
                        Log.e(str3, stringBuilder3.toString());
                    }
                    if (!file.delete()) {
                        str3 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("IOFileRotator,fail to delete,file:");
                        stringBuilder3.append(file.getName());
                        Log.e(str3, stringBuilder3.toString());
                    }
                }
            }
        }
    }

    public void deleteAll() {
        FileInfo info = new FileInfo(this.mPrefix);
        for (String name : getBasePathFileList()) {
            if (info.parse(name) && !new File(this.mBasePath, name).delete()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("deleteAll,fail to delete the file:");
                stringBuilder.append(name);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: EliminatePhiNodes
        jadx.core.utils.exceptions.JadxRuntimeException: Assign predecessor not found for B:6:0x001c from B:25:?
        	at jadx.core.dex.visitors.ssa.EliminatePhiNodes.replaceMerge(EliminatePhiNodes.java:102)
        	at jadx.core.dex.visitors.ssa.EliminatePhiNodes.replaceMergeInstructions(EliminatePhiNodes.java:68)
        	at jadx.core.dex.visitors.ssa.EliminatePhiNodes.visit(EliminatePhiNodes.java:31)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public void dumpAll(java.io.OutputStream r10) throws java.io.IOException {
        /*
        r9 = this;
        r0 = new java.util.zip.ZipOutputStream;
        r0.<init>(r10);
        r1 = new com.android.server.rms.io.IOFileRotator$FileInfo;	 Catch:{ all -> 0x0048 }
        r2 = r9.mPrefix;	 Catch:{ all -> 0x0048 }
        r1.<init>(r2);	 Catch:{ all -> 0x0048 }
        r2 = r9.getBasePathFileList();	 Catch:{ all -> 0x0048 }
        r3 = r2.length;	 Catch:{ all -> 0x0048 }
        r4 = 0;	 Catch:{ all -> 0x0048 }
    L_0x0012:
        if (r4 >= r3) goto L_0x0043;	 Catch:{ all -> 0x0048 }
    L_0x0014:
        r5 = r2[r4];	 Catch:{ all -> 0x0048 }
        r6 = r1.parse(r5);	 Catch:{ all -> 0x0048 }
        if (r6 == 0) goto L_0x0040;	 Catch:{ all -> 0x0048 }
    L_0x001c:
        r6 = new java.util.zip.ZipEntry;	 Catch:{ all -> 0x0048 }
        r6.<init>(r5);	 Catch:{ all -> 0x0048 }
        r0.putNextEntry(r6);	 Catch:{ all -> 0x0048 }
        r7 = new java.io.File;	 Catch:{ all -> 0x0048 }
        r8 = r9.mBasePath;	 Catch:{ all -> 0x0048 }
        r7.<init>(r8, r5);	 Catch:{ all -> 0x0048 }
        r8 = new java.io.FileInputStream;	 Catch:{ all -> 0x0048 }
        r8.<init>(r7);	 Catch:{ all -> 0x0048 }
        libcore.io.Streams.copy(r8, r0);	 Catch:{ all -> 0x003b }
        libcore.io.IoUtils.closeQuietly(r8);	 Catch:{ all -> 0x0048 }
        r0.closeEntry();	 Catch:{ all -> 0x0048 }
        goto L_0x0040;	 Catch:{ all -> 0x0048 }
    L_0x003b:
        r3 = move-exception;	 Catch:{ all -> 0x0048 }
        libcore.io.IoUtils.closeQuietly(r8);	 Catch:{ all -> 0x0048 }
        throw r3;	 Catch:{ all -> 0x0048 }
    L_0x0040:
        r4 = r4 + 1;
        goto L_0x0012;
    L_0x0043:
        libcore.io.IoUtils.closeQuietly(r0);
        return;
    L_0x0048:
        r1 = move-exception;
        libcore.io.IoUtils.closeQuietly(r0);
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.rms.io.IOFileRotator.dumpAll(java.io.OutputStream):void");
    }

    public long getAvailableBytesInActiveFile(long currentTimeMillis) {
        long j = 0;
        long availableBytes = 0;
        try {
            availableBytes = this.mMaxFileSize - new File(this.mBasePath, getActiveName(currentTimeMillis)).length();
            if (availableBytes > 0) {
                j = availableBytes;
            }
            availableBytes = j;
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkIfActiveFileFull,RuntimeException:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            Log.e(TAG, "checkIfActiveFileFull,fail to read the file's size");
        } catch (Throwable th) {
        }
        return availableBytes;
    }

    public boolean removeFilesWhenOverFlow() {
        int index = 0;
        boolean isHandle = false;
        try {
            long directorySize = Utils.getSizeOfDirectory(this.mBasePath);
            if (directorySize < mMaxSizeForBasePath) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeFilesWhenOverFlow,the total size is ok,current size:");
                stringBuilder.append(directorySize);
                Log.i(str, stringBuilder.toString());
                return false;
            }
            String[] baseFiles = getBasePathFileList();
            int totalSize = 0;
            int deleteEndIndex = 0;
            for (int index2 = baseFiles.length - 1; index2 >= 0; index2--) {
                totalSize = (int) (((long) totalSize) + new File(this.mBasePath, baseFiles[index2]).length());
                if (((long) totalSize) >= mMaxSizeForBasePath) {
                    deleteEndIndex = index2;
                    break;
                }
            }
            while (index <= deleteEndIndex) {
                if (!new File(this.mBasePath, baseFiles[index]).delete()) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("removeFilesWhenOverFlow,fail to delete the ");
                    stringBuilder2.append(baseFiles[index]);
                    Log.e(str2, stringBuilder2.toString());
                }
                index++;
            }
            isHandle = true;
            return isHandle;
        } catch (RuntimeException e) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("removeFilesWhenOverFlow,RuntimeException:");
            stringBuilder3.append(e.getMessage());
            Log.e(str3, stringBuilder3.toString());
        } catch (Exception e2) {
            Log.e(TAG, "removeFilesWhenOverFlow,fail to read the file's size");
        }
    }

    public void forceFile(long currentTimeMillis, long endTimeMills) {
        String activeFileName = getActiveName(currentTimeMillis);
        File currentFile = new File(this.mBasePath, activeFileName);
        if (currentFile.exists()) {
            FileInfo info = new FileInfo(this.mPrefix);
            if (info.parse(activeFileName) && info.isActive()) {
                info.endMillis = endTimeMills;
                File destFile = new File(this.mBasePath, info.build());
                if (!currentFile.renameTo(destFile)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("forceFile,fail to renameTo:destFile");
                    stringBuilder.append(destFile.getName());
                    Log.e(str, stringBuilder.toString());
                }
            }
        }
    }

    public void rewriteActive(Rewriter rewriter, long currentTimeMillis) throws IOException {
        if (rewriter == null) {
            Log.e(TAG, "rewriteActive,the rewriter is null");
        } else {
            rewriteSingle(rewriter, getActiveName(currentTimeMillis));
        }
    }

    @Deprecated
    public void combineActive(Reader reader, Writer writer, long currentTimeMillis) throws IOException {
        rewriteActive(new RewriterDef(reader, writer), currentTimeMillis);
    }

    private void rewriteSingle(Rewriter rewriter, String name) throws IOException {
        String str;
        StringBuilder stringBuilder;
        IOException rethrowAsIoException;
        if (Utils.DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("rewriting ");
            stringBuilder2.append(name);
            Log.d(str2, stringBuilder2.toString());
        }
        File file = new File(this.mBasePath, name);
        rewriter.reset();
        File file2;
        StringBuilder stringBuilder3;
        File backupFile;
        String str3;
        if (file.exists()) {
            readFile(file, rewriter);
            if (rewriter.shouldWrite()) {
                file2 = this.mBasePath;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(name);
                stringBuilder3.append(SUFFIX_BACKUP);
                backupFile = new File(file2, stringBuilder3.toString());
                if (!file.renameTo(backupFile)) {
                    str3 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("rewriteSingle,fail to renameTo:");
                    stringBuilder3.append(backupFile.getName());
                    Log.e(str3, stringBuilder3.toString());
                }
                try {
                    writeFile(file, rewriter);
                    if (!backupFile.delete()) {
                        str3 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("rewriteSingle,fail to delete the file:");
                        stringBuilder3.append(backupFile.getName());
                        Log.e(str3, stringBuilder3.toString());
                    }
                } catch (Throwable t) {
                    if (!file.delete()) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("rewriteSingle,fail to delete the file:");
                        stringBuilder.append(file.getName());
                        Log.e(str, stringBuilder.toString());
                    }
                    if (!backupFile.renameTo(file)) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("rewriteSingle,fail to renameTo:");
                        stringBuilder3.append(backupFile.getName());
                        Log.e(TAG, stringBuilder3.toString());
                    }
                    rethrowAsIoException = rethrowAsIoException(t);
                }
            } else {
                return;
            }
        }
        file2 = this.mBasePath;
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(name);
        stringBuilder3.append(SUFFIX_NO_BACKUP);
        backupFile = new File(file2, stringBuilder3.toString());
        if (!backupFile.createNewFile()) {
            str3 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("rewriteSingle,fail to createNewFile,");
            stringBuilder3.append(backupFile.getName());
            Log.e(str3, stringBuilder3.toString());
        }
        try {
            writeFile(file, rewriter);
            if (!backupFile.delete()) {
                str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("rewriteSingle,fail to delete the file:");
                stringBuilder3.append(backupFile.getName());
                Log.e(str3, stringBuilder3.toString());
            }
        } catch (Throwable t2) {
            if (!file.delete()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("rewriteSingle,fail to delete the file:");
                stringBuilder.append(file.getName());
                Log.e(str, stringBuilder.toString());
            }
            if (!backupFile.delete()) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("rewriteSingle,fail to delete the file:");
                stringBuilder3.append(backupFile.getName());
                Log.e(TAG, stringBuilder3.toString());
            }
            rethrowAsIoException = rethrowAsIoException(t2);
        }
    }

    public void readMatching(Reader reader, long matchStartMillis, long matchEndMillis) throws IOException {
        FileInfo info = new FileInfo(this.mPrefix);
        for (String name : getBasePathFileList()) {
            if (info.parse(name) && info.startMillis <= matchEndMillis && matchStartMillis <= info.endMillis) {
                if (Utils.DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("reading matching ");
                    stringBuilder.append(name);
                    Log.d(str, stringBuilder.toString());
                }
                readFile(new File(this.mBasePath, name), reader);
            }
        }
    }

    private String getActiveName(long currentTimeMillis) {
        String oldestActiveName = null;
        long oldestActiveStart = Long.MAX_VALUE;
        FileInfo info = new FileInfo(this.mPrefix);
        for (String name : getBasePathFileList()) {
            if (info.parse(name) && info.isActive() && info.startMillis < currentTimeMillis && info.startMillis < oldestActiveStart) {
                oldestActiveName = name;
                oldestActiveStart = info.startMillis;
            }
        }
        if (oldestActiveName != null) {
            return oldestActiveName;
        }
        info.startMillis = currentTimeMillis;
        info.endMillis = Long.MAX_VALUE;
        return info.build();
    }

    private String[] getBasePathFileList() {
        String[] baseFiles = this.mBasePath.list();
        if (baseFiles != null && baseFiles.length != 0) {
            return baseFiles;
        }
        Log.e(TAG, "getBasePathFileList,the baseFiles is empty");
        return new String[0];
    }

    public void maybeRotate(long currentTimeMillis) {
        long j = currentTimeMillis;
        long rotateBefore = j - this.mRotateAgeMillis;
        long deleteBefore = j - this.mDeleteAgeMillis;
        FileInfo info = new FileInfo(this.mPrefix);
        String[] baseFileList = getBasePathFileList();
        int length = baseFileList.length;
        int i = 0;
        while (i < length) {
            String name = baseFileList[i];
            if (info.parse(name)) {
                if (info.isActive()) {
                    if (info.startMillis <= rotateBefore) {
                        if (Utils.DEBUG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("rotating ");
                            stringBuilder.append(name);
                            Log.d(str, stringBuilder.toString());
                        }
                        info.endMillis = j;
                        File file = new File(this.mBasePath, name);
                        File destFile = new File(this.mBasePath, info.build());
                        if (!file.renameTo(destFile)) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("maybeRotate,fail to renameTo:");
                            stringBuilder2.append(destFile.getName());
                            Log.e(str2, stringBuilder2.toString());
                        }
                    }
                } else if (info.endMillis <= deleteBefore) {
                    if (Utils.DEBUG) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("deleting ");
                        stringBuilder3.append(name);
                        Log.d(str3, stringBuilder3.toString());
                    }
                    File file2 = new File(this.mBasePath, name);
                    if (!file2.delete()) {
                        String str4 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("maybeRotate,fail to delete the file:");
                        stringBuilder4.append(file2.getName());
                        Log.e(str4, stringBuilder4.toString());
                    }
                }
            }
            i++;
            j = currentTimeMillis;
        }
    }

    private static void readFile(File file, Reader reader) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        try {
            reader.read(bis);
        } finally {
            IoUtils.closeQuietly(bis);
        }
    }

    private static void writeFile(File file, Writer writer) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        try {
            writer.write(bos);
            bos.flush();
        } finally {
            FileUtils.sync(fos);
            IoUtils.closeQuietly(bos);
        }
    }

    private static IOException rethrowAsIoException(Throwable t) throws IOException {
        if (t instanceof IOException) {
            throw ((IOException) t);
        }
        throw new IOException(t.getMessage(), t);
    }
}
