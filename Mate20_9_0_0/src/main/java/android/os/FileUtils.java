package android.os;

import android.provider.DocumentsContract.Document;
import android.rms.iaware.AwareConstant.Database;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.telecom.Logging.Session;
import android.telephony.SubscriptionPlan;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.webkit.MimeTypeMap;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.SizedInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;

public class FileUtils {
    private static final long COPY_CHECKPOINT_BYTES = 524288;
    private static final File[] EMPTY = new File[0];
    private static final boolean ENABLE_COPY_OPTIMIZATIONS = true;
    public static final int S_IRGRP = 32;
    public static final int S_IROTH = 4;
    public static final int S_IRUSR = 256;
    public static final int S_IRWXG = 56;
    public static final int S_IRWXO = 7;
    public static final int S_IRWXU = 448;
    public static final int S_IWGRP = 16;
    public static final int S_IWOTH = 2;
    public static final int S_IWUSR = 128;
    public static final int S_IXGRP = 8;
    public static final int S_IXOTH = 1;
    public static final int S_IXUSR = 64;
    private static final String TAG = "FileUtils";

    @VisibleForTesting
    public static class MemoryPipe extends Thread implements AutoCloseable {
        private final byte[] data;
        private final FileDescriptor[] pipe;
        private final boolean sink;

        private MemoryPipe(byte[] data, boolean sink) throws IOException {
            try {
                this.pipe = Os.pipe();
                this.data = data;
                this.sink = sink;
            } catch (ErrnoException e) {
                throw e.rethrowAsIOException();
            }
        }

        private MemoryPipe startInternal() {
            super.start();
            return this;
        }

        public static MemoryPipe createSource(byte[] data) throws IOException {
            return new MemoryPipe(data, false).startInternal();
        }

        public static MemoryPipe createSink(byte[] data) throws IOException {
            return new MemoryPipe(data, true).startInternal();
        }

        public FileDescriptor getFD() {
            return this.sink ? this.pipe[1] : this.pipe[0];
        }

        public FileDescriptor getInternalFD() {
            return this.sink ? this.pipe[0] : this.pipe[1];
        }

        /* JADX WARNING: Missing block: B:11:0x002a, code skipped:
            if (r6.sink != false) goto L_0x002c;
     */
        /* JADX WARNING: Missing block: B:12:0x002c, code skipped:
            android.os.SystemClock.sleep(java.util.concurrent.TimeUnit.SECONDS.toMillis(1));
     */
        /* JADX WARNING: Missing block: B:13:0x0035, code skipped:
            libcore.io.IoUtils.closeQuietly(r0);
     */
        /* JADX WARNING: Missing block: B:19:0x0045, code skipped:
            if (r6.sink == false) goto L_0x0035;
     */
        /* JADX WARNING: Missing block: B:20:0x0048, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            FileDescriptor fd = getInternalFD();
            int i = 0;
            while (i < this.data.length) {
                try {
                    if (this.sink) {
                        i += Os.read(fd, this.data, i, this.data.length - i);
                    } else {
                        i += Os.write(fd, this.data, i, this.data.length - i);
                    }
                } catch (ErrnoException | IOException e) {
                    Slog.w(FileUtils.TAG, "IOException | ErrnoException: sink");
                } catch (Throwable th) {
                    if (this.sink) {
                        SystemClock.sleep(TimeUnit.SECONDS.toMillis(1));
                    }
                    IoUtils.closeQuietly(fd);
                }
            }
        }

        public void close() throws Exception {
            IoUtils.closeQuietly(getFD());
        }
    }

    private static class NoImagePreloadHolder {
        public static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("[\\w%+,./=_-]+");

        private NoImagePreloadHolder() {
        }
    }

    public interface ProgressListener {
        void onProgress(long j);
    }

    public static int setPermissions(File path, int mode, int uid, int gid) {
        return setPermissions(path.getAbsolutePath(), mode, uid, gid);
    }

    public static int setPermissions(String path, int mode, int uid, int gid) {
        String str;
        StringBuilder stringBuilder;
        try {
            Os.chmod(path, mode);
            if (uid >= 0 || gid >= 0) {
                try {
                    Os.chown(path, uid, gid);
                } catch (ErrnoException e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to chown(");
                    stringBuilder.append(path);
                    stringBuilder.append("): ");
                    stringBuilder.append(e);
                    Slog.w(str, stringBuilder.toString());
                    return e.errno;
                }
            }
            return 0;
        } catch (ErrnoException e2) {
            if (uid >= 0 || gid >= 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to chmod(");
                stringBuilder.append(path);
                stringBuilder.append("): ");
                stringBuilder.append(e2);
                Slog.w(str, stringBuilder.toString());
            }
            return e2.errno;
        }
    }

    public static int setPermissions(FileDescriptor fd, int mode, int uid, int gid) {
        String str;
        StringBuilder stringBuilder;
        try {
            Os.fchmod(fd, mode);
            if (uid >= 0 || gid >= 0) {
                try {
                    Os.fchown(fd, uid, gid);
                } catch (ErrnoException e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to fchown(): ");
                    stringBuilder.append(e);
                    Slog.w(str, stringBuilder.toString());
                    return e.errno;
                }
            }
            return 0;
        } catch (ErrnoException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to fchmod(): ");
            stringBuilder.append(e2);
            Slog.w(str, stringBuilder.toString());
            return e2.errno;
        }
    }

    public static void copyPermissions(File from, File to) throws IOException {
        try {
            StructStat stat = Os.stat(from.getAbsolutePath());
            Os.chmod(to.getAbsolutePath(), stat.st_mode);
            Os.chown(to.getAbsolutePath(), stat.st_uid, stat.st_gid);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public static int getUid(String path) {
        try {
            return Os.stat(path).st_uid;
        } catch (ErrnoException e) {
            return -1;
        }
    }

    public static boolean sync(FileOutputStream stream) {
        if (stream != null) {
            try {
                stream.getFD().sync();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    @Deprecated
    public static boolean copyFile(File srcFile, File destFile) {
        try {
            copyFileOrThrow(srcFile, destFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0011, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Deprecated
    public static void copyFileOrThrow(File srcFile, File destFile) throws IOException {
        InputStream in = new FileInputStream(srcFile);
        copyToFileOrThrow(in, destFile);
        $closeResource(null, in);
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    @Deprecated
    public static boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            copyToFileOrThrow(inputStream, destFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0028, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @Deprecated
    public static void copyToFileOrThrow(InputStream in, File destFile) throws IOException {
        if (destFile.exists()) {
            destFile.delete();
        }
        OutputStream out = new FileOutputStream(destFile);
        copy(in, out);
        try {
            Os.fsync(out.getFD());
            $closeResource(null, out);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public static long copy(File from, File to) throws IOException {
        return copy(from, to, null, null);
    }

    /* JADX WARNING: Missing block: B:23:0x0028, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static long copy(File from, File to, ProgressListener listener, CancellationSignal signal) throws IOException {
        Throwable th;
        Throwable th2;
        InputStream in = new FileInputStream(from);
        OutputStream out = new FileOutputStream(to);
        try {
            long copy = copy(in, out, listener, signal);
            $closeResource(null, out);
            $closeResource(null, in);
            return copy;
        } catch (Throwable th22) {
            Throwable th3 = th22;
            th22 = th;
            th = th3;
        }
        $closeResource(th22, out);
        throw th;
    }

    public static long copy(InputStream in, OutputStream out) throws IOException {
        return copy(in, out, null, null);
    }

    public static long copy(InputStream in, OutputStream out, ProgressListener listener, CancellationSignal signal) throws IOException {
        if ((in instanceof FileInputStream) && (out instanceof FileOutputStream)) {
            return copy(((FileInputStream) in).getFD(), ((FileOutputStream) out).getFD(), listener, signal);
        }
        return copyInternalUserspace(in, out, listener, signal);
    }

    public static long copy(FileDescriptor in, FileDescriptor out) throws IOException {
        return copy(in, out, null, null);
    }

    public static long copy(FileDescriptor in, FileDescriptor out, ProgressListener listener, CancellationSignal signal) throws IOException {
        return copy(in, out, listener, signal, SubscriptionPlan.BYTES_UNLIMITED);
    }

    public static long copy(FileDescriptor in, FileDescriptor out, ProgressListener listener, CancellationSignal signal, long count) throws IOException {
        try {
            StructStat st_in = Os.fstat(in);
            StructStat st_out = Os.fstat(out);
            if (OsConstants.S_ISREG(st_in.st_mode) && OsConstants.S_ISREG(st_out.st_mode)) {
                return copyInternalSendfile(in, out, listener, signal, count);
            }
            if (OsConstants.S_ISFIFO(st_in.st_mode) || OsConstants.S_ISFIFO(st_out.st_mode)) {
                return copyInternalSplice(in, out, listener, signal, count);
            }
            return copyInternalUserspace(in, out, listener, signal, count);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    @VisibleForTesting
    public static long copyInternalSplice(FileDescriptor in, FileDescriptor out, ProgressListener listener, CancellationSignal signal, long count) throws ErrnoException {
        ProgressListener progressListener = listener;
        long count2 = count;
        long progress = 0;
        long checkpoint = 0;
        while (true) {
            long j = 524288;
            long splice = Os.splice(in, null, out, null, Math.min(count2, 524288), OsConstants.SPLICE_F_MOVE | OsConstants.SPLICE_F_MORE);
            long t = splice;
            if (splice == 0) {
                break;
            }
            progress += t;
            checkpoint += t;
            count2 -= t;
            if (checkpoint >= j) {
                if (signal != null) {
                    signal.throwIfCanceled();
                }
                if (progressListener != null) {
                    progressListener.onProgress(progress);
                }
                checkpoint = 0;
            }
        }
        if (progressListener != null) {
            progressListener.onProgress(progress);
        }
        return progress;
    }

    @VisibleForTesting
    public static long copyInternalSendfile(FileDescriptor in, FileDescriptor out, ProgressListener listener, CancellationSignal signal, long count) throws ErrnoException {
        ProgressListener progressListener = listener;
        long count2 = count;
        long progress = 0;
        long checkpoint = 0;
        while (true) {
            FileDescriptor fileDescriptor = out;
            long sendfile = Os.sendfile(fileDescriptor, in, null, Math.min(count2, 524288));
            long t = sendfile;
            if (sendfile == 0) {
                break;
            }
            progress += t;
            checkpoint += t;
            count2 -= t;
            if (checkpoint >= 524288) {
                if (signal != null) {
                    signal.throwIfCanceled();
                }
                if (progressListener != null) {
                    progressListener.onProgress(progress);
                }
                checkpoint = 0;
            }
        }
        if (progressListener != null) {
            progressListener.onProgress(progress);
        }
        return progress;
    }

    @VisibleForTesting
    public static long copyInternalUserspace(FileDescriptor in, FileDescriptor out, ProgressListener listener, CancellationSignal signal, long count) throws IOException {
        if (count != SubscriptionPlan.BYTES_UNLIMITED) {
            return copyInternalUserspace(new SizedInputStream(new FileInputStream(in), count), new FileOutputStream(out), listener, signal);
        }
        return copyInternalUserspace(new FileInputStream(in), new FileOutputStream(out), listener, signal);
    }

    @VisibleForTesting
    public static long copyInternalUserspace(InputStream in, OutputStream out, ProgressListener listener, CancellationSignal signal) throws IOException {
        long progress = 0;
        long checkpoint = 0;
        byte[] buffer = new byte[8192];
        while (true) {
            int read = in.read(buffer);
            int t = read;
            if (read == -1) {
                break;
            }
            out.write(buffer, 0, t);
            progress += (long) t;
            checkpoint += (long) t;
            if (checkpoint >= 524288) {
                if (signal != null) {
                    signal.throwIfCanceled();
                }
                if (listener != null) {
                    listener.onProgress(progress);
                }
                checkpoint = 0;
            }
        }
        if (listener != null) {
            listener.onProgress(progress);
        }
        return progress;
    }

    public static boolean isFilenameSafe(File file) {
        return NoImagePreloadHolder.SAFE_FILENAME_PATTERN.matcher(file.getPath()).matches();
    }

    public static String readTextFile(File file, int max, String ellipsis) throws IOException {
        InputStream input = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(input);
        try {
            long size = file.length();
            String str = 0;
            if (max <= 0) {
                if (size <= 0 || max != 0) {
                    byte[] last;
                    if (max < 0) {
                        int len;
                        boolean rolled = false;
                        last = null;
                        byte[] data = null;
                        while (true) {
                            if (last != null) {
                                rolled = true;
                            }
                            byte[] tmp = last;
                            last = data;
                            data = tmp;
                            if (data == null) {
                                data = new byte[(-max)];
                            }
                            len = bis.read(data);
                            if (len != data.length) {
                                break;
                            }
                        }
                        if (last == null && len <= 0) {
                            str = "";
                            bis.close();
                            input.close();
                            return str;
                        } else if (last == null) {
                            String str2 = new String(data, 0, len);
                            bis.close();
                            input.close();
                            return str2;
                        } else {
                            if (len > 0) {
                                rolled = true;
                                System.arraycopy(last, len, last, 0, last.length - len);
                                System.arraycopy(data, 0, last, last.length - len, len);
                            }
                            if (ellipsis != null) {
                                if (rolled) {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(ellipsis);
                                    stringBuilder.append(new String(last));
                                    str = stringBuilder.toString();
                                    bis.close();
                                    input.close();
                                    return str;
                                }
                            }
                            str = new String(last);
                            bis.close();
                            input.close();
                            return str;
                        }
                    }
                    ByteArrayOutputStream contents = new ByteArrayOutputStream();
                    last = new byte[1024];
                    while (true) {
                        int len2 = bis.read(last);
                        if (len2 > 0) {
                            contents.write(last, 0, len2);
                        }
                        if (len2 != last.length) {
                            str = contents.toString();
                            bis.close();
                            input.close();
                            return str;
                        }
                    }
                }
            }
            if (size > 0 && (max == 0 || size < ((long) max))) {
                max = (int) size;
            }
            byte[] data2 = new byte[(max + 1)];
            int length = bis.read(data2);
            String str3;
            if (length <= 0) {
                str = "";
                return str;
            } else if (length <= max) {
                str3 = new String(data2, 0, length);
                bis.close();
                input.close();
                return str3;
            } else if (ellipsis == null) {
                str3 = new String(data2, 0, max);
                bis.close();
                input.close();
                return str3;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(new String(data2, 0, max));
                stringBuilder2.append(ellipsis);
                str = stringBuilder2.toString();
                bis.close();
                input.close();
                return str;
            }
        } finally {
            bis.close();
            input.close();
        }
    }

    public static void stringToFile(File file, String string) throws IOException {
        stringToFile(file.getAbsolutePath(), string);
    }

    /* JADX WARNING: Missing block: B:28:0x003a, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void bytesToFile(String filename, byte[] content) throws IOException {
        if (filename.startsWith("/proc/")) {
            int oldMask = StrictMode.allowThreadDiskWritesMask();
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(filename);
                fos.write(content);
                $closeResource(null, fos);
                StrictMode.setThreadPolicyMask(oldMask);
            } catch (Throwable th) {
                StrictMode.setThreadPolicyMask(oldMask);
            }
        } else {
            FileOutputStream fos2 = new FileOutputStream(filename);
            fos2.write(content);
            $closeResource(null, fos2);
        }
    }

    public static void stringToFile(String filename, String string) throws IOException {
        bytesToFile(filename, string.getBytes(StandardCharsets.UTF_8));
    }

    public static long checksumCrc32(File file) throws FileNotFoundException, IOException {
        CRC32 checkSummer = new CRC32();
        CheckedInputStream cis = null;
        try {
            cis = new CheckedInputStream(new FileInputStream(file), checkSummer);
            while (cis.read(new byte[128]) >= 0) {
            }
            long value = checkSummer.getValue();
            try {
                cis.close();
            } catch (IOException e) {
            }
            return value;
        } catch (Throwable th) {
            if (cis != null) {
                try {
                    cis.close();
                } catch (IOException e2) {
                }
            }
        }
    }

    public static boolean deleteOlderFiles(File dir, int minCount, long minAgeMs) {
        if (minCount < 0 || minAgeMs < 0) {
            throw new IllegalArgumentException("Constraints must be positive or 0");
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File lhs, File rhs) {
                return Long.compare(rhs.lastModified(), lhs.lastModified());
            }
        });
        boolean deleted = false;
        for (int i = minCount; i < files.length; i++) {
            File file = files[i];
            if (System.currentTimeMillis() - file.lastModified() > minAgeMs && file.delete()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Deleted old file ");
                stringBuilder.append(file);
                Log.d(str, stringBuilder.toString());
                deleted = true;
            }
        }
        return deleted;
    }

    public static boolean contains(File[] dirs, File file) {
        for (File dir : dirs) {
            if (contains(dir, file)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(File dir, File file) {
        if (dir == null || file == null) {
            return false;
        }
        return contains(dir.getAbsolutePath(), file.getAbsolutePath());
    }

    public static boolean contains(String dirPath, String filePath) {
        if (dirPath.equals(filePath)) {
            return true;
        }
        if (!dirPath.endsWith("/")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dirPath);
            stringBuilder.append("/");
            dirPath = stringBuilder.toString();
        }
        return filePath.startsWith(dirPath);
    }

    public static boolean deleteContentsAndDir(File dir) {
        if (deleteContents(dir)) {
            return dir.delete();
        }
        return false;
    }

    public static boolean deleteContents(File dir) {
        File[] files = dir.listFiles();
        boolean success = true;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    success &= deleteContents(file);
                }
                if (!file.delete()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to delete ");
                    stringBuilder.append(file);
                    Log.w(str, stringBuilder.toString());
                    success = false;
                }
            }
        }
        return success;
    }

    private static boolean isValidExtFilenameChar(char c) {
        if (c == 0 || c == '/') {
            return false;
        }
        return true;
    }

    public static boolean isValidExtFilename(String name) {
        return name != null && name.equals(buildValidExtFilename(name));
    }

    public static String buildValidExtFilename(String name) {
        if (TextUtils.isEmpty(name) || ".".equals(name) || "..".equals(name)) {
            return "(invalid)";
        }
        StringBuilder res = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (isValidExtFilenameChar(c)) {
                res.append(c);
            } else {
                res.append('_');
            }
        }
        trimFilename(res, 255);
        return res.toString();
    }

    private static boolean isValidFatFilenameChar(char c) {
        if (!((c >= 0 && c <= 31) || c == '\"' || c == '*' || c == '/' || c == ':' || c == '<' || c == '\\' || c == '|' || c == 127)) {
            switch (c) {
                case '>':
                case '?':
                    break;
                default:
                    return true;
            }
        }
        return false;
    }

    public static boolean isValidFatFilename(String name) {
        return name != null && name.equals(buildValidFatFilename(name));
    }

    public static String buildValidFatFilename(String name) {
        if (TextUtils.isEmpty(name) || ".".equals(name) || "..".equals(name)) {
            return "(invalid)";
        }
        StringBuilder res = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (isValidFatFilenameChar(c)) {
                res.append(c);
            } else {
                res.append('_');
            }
        }
        trimFilename(res, 255);
        return res.toString();
    }

    @VisibleForTesting
    public static String trimFilename(String str, int maxBytes) {
        StringBuilder res = new StringBuilder(str);
        trimFilename(res, maxBytes);
        return res.toString();
    }

    private static void trimFilename(StringBuilder res, int maxBytes) {
        byte[] raw = res.toString().getBytes(StandardCharsets.UTF_8);
        if (raw.length > maxBytes) {
            maxBytes -= 3;
            while (raw.length > maxBytes) {
                res.deleteCharAt(res.length() / 2);
                raw = res.toString().getBytes(StandardCharsets.UTF_8);
            }
            res.insert(res.length() / 2, Session.TRUNCATE_STRING);
        }
    }

    public static String rewriteAfterRename(File beforeDir, File afterDir, String path) {
        String str = null;
        if (path == null) {
            return null;
        }
        File result = rewriteAfterRename(beforeDir, afterDir, new File(path));
        if (result != null) {
            str = result.getAbsolutePath();
        }
        return str;
    }

    public static String[] rewriteAfterRename(File beforeDir, File afterDir, String[] paths) {
        if (paths == null) {
            return null;
        }
        String[] result = new String[paths.length];
        for (int i = 0; i < paths.length; i++) {
            result[i] = rewriteAfterRename(beforeDir, afterDir, paths[i]);
        }
        return result;
    }

    /* JADX WARNING: Missing block: B:9:0x0025, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static File rewriteAfterRename(File beforeDir, File afterDir, File file) {
        if (file == null || beforeDir == null || afterDir == null || !contains(beforeDir, file)) {
            return null;
        }
        return new File(afterDir, file.getAbsolutePath().substring(beforeDir.getAbsolutePath().length()));
    }

    private static File buildUniqueFileWithExtension(File parent, String name, String ext) throws FileNotFoundException {
        File file = buildFile(parent, name, ext);
        int n = 0;
        while (file.exists()) {
            int n2 = n + 1;
            if (n < 32) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(name);
                stringBuilder.append(" (");
                stringBuilder.append(n2);
                stringBuilder.append(")");
                file = buildFile(parent, stringBuilder.toString(), ext);
                n = n2;
            } else {
                throw new FileNotFoundException("Failed to create unique file");
            }
        }
        return file;
    }

    public static File buildUniqueFile(File parent, String mimeType, String displayName) throws FileNotFoundException {
        String[] parts = splitFileName(mimeType, displayName);
        return buildUniqueFileWithExtension(parent, parts[0], parts[1]);
    }

    public static File buildUniqueFile(File parent, String displayName) throws FileNotFoundException {
        String name;
        String ext;
        int lastDot = displayName.lastIndexOf(46);
        if (lastDot >= 0) {
            name = displayName.substring(null, lastDot);
            ext = displayName.substring(lastDot + 1);
        } else {
            name = displayName;
            ext = null;
        }
        return buildUniqueFileWithExtension(parent, name, ext);
    }

    public static String[] splitFileName(String mimeType, String displayName) {
        String ext;
        String name;
        if (Document.MIME_TYPE_DIR.equals(mimeType)) {
            ext = null;
            name = displayName;
        } else {
            String mimeTypeFromExt;
            int lastDot = displayName.lastIndexOf(46);
            if (lastDot >= 0) {
                ext = displayName.substring(0, lastDot);
                name = displayName.substring(lastDot + 1);
                mimeTypeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.toLowerCase());
            } else {
                ext = displayName;
                name = null;
                mimeTypeFromExt = null;
            }
            String str = name;
            name = ext;
            ext = str;
            if (mimeTypeFromExt == null) {
                mimeTypeFromExt = Database.UNKNOWN_MIME_TYPE;
            }
            String extFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (!(Objects.equals(mimeType, mimeTypeFromExt) || Objects.equals(ext, extFromMimeType))) {
                name = displayName;
                ext = extFromMimeType;
            }
        }
        if (ext == null) {
            ext = "";
        }
        return new String[]{name, ext};
    }

    private static File buildFile(File parent, String name, String ext) {
        if (TextUtils.isEmpty(ext)) {
            return new File(parent, name);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append(".");
        stringBuilder.append(ext);
        return new File(parent, stringBuilder.toString());
    }

    public static String[] listOrEmpty(File dir) {
        if (dir == null) {
            return EmptyArray.STRING;
        }
        String[] res = dir.list();
        if (res != null) {
            return res;
        }
        return EmptyArray.STRING;
    }

    public static File[] listFilesOrEmpty(File dir) {
        if (dir == null) {
            return EMPTY;
        }
        File[] res = dir.listFiles();
        if (res != null) {
            return res;
        }
        return EMPTY;
    }

    public static File[] listFilesOrEmpty(File dir, FilenameFilter filter) {
        if (dir == null) {
            return EMPTY;
        }
        File[] res = dir.listFiles(filter);
        if (res != null) {
            return res;
        }
        return EMPTY;
    }

    public static File newFileOrNull(String path) {
        return path != null ? new File(path) : null;
    }

    public static File createDir(File baseDir, String name) {
        File dir = new File(baseDir, name);
        File file = null;
        if (dir.exists()) {
            if (dir.isDirectory()) {
                file = dir;
            }
            return file;
        }
        if (dir.mkdir()) {
            file = dir;
        }
        return file;
    }

    public static long roundStorageSize(long size) {
        long val = 1;
        long pow = 1;
        while (val * pow < size) {
            val <<= 1;
            if (val > 512) {
                val = 1;
                pow *= 1000;
            }
        }
        return val * pow;
    }
}
