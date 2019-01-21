package com.android.internal.os;

import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class KernelCpuProcReader {
    private static final long DEFAULT_THROTTLE_INTERVAL = 3000;
    private static final int ERROR_THRESHOLD = 5;
    private static final int INITIAL_BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = 1048576;
    private static final String PROC_UID_ACTIVE_TIME = "/proc/uid_cpupower/concurrent_active_time";
    private static final String PROC_UID_CLUSTER_TIME = "/proc/uid_cpupower/concurrent_policy_time";
    private static final String PROC_UID_FREQ_TIME = "/proc/uid_cpupower/time_in_state";
    private static final String TAG = "KernelCpuProcReader";
    private static final KernelCpuProcReader mActiveTimeReader = new KernelCpuProcReader(PROC_UID_ACTIVE_TIME);
    private static final KernelCpuProcReader mClusterTimeReader = new KernelCpuProcReader(PROC_UID_CLUSTER_TIME);
    private static final KernelCpuProcReader mFreqTimeReader = new KernelCpuProcReader(PROC_UID_FREQ_TIME);
    private ByteBuffer mBuffer;
    private int mErrors;
    private long mLastReadTime = Long.MIN_VALUE;
    private final Path mProc;
    private long mThrottleInterval = DEFAULT_THROTTLE_INTERVAL;

    public static KernelCpuProcReader getFreqTimeReaderInstance() {
        return mFreqTimeReader;
    }

    public static KernelCpuProcReader getActiveTimeReaderInstance() {
        return mActiveTimeReader;
    }

    public static KernelCpuProcReader getClusterTimeReaderInstance() {
        return mClusterTimeReader;
    }

    @VisibleForTesting
    public KernelCpuProcReader(String procFile) {
        this.mProc = Paths.get(procFile, new String[0]);
        this.mBuffer = ByteBuffer.allocateDirect(8192);
        this.mBuffer.clear();
    }

    /* JADX WARNING: Removed duplicated region for block: B:57:0x00f4 A:{ExcHandler: FileNotFoundException | NoSuchFileException (e java.lang.Throwable), Splitter:B:13:0x004a} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:51:0x00cf, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:52:0x00d1, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:54:?, code skipped:
            r9.mErrors++;
            r2 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("Error reading: ");
            r4.append(r9.mProc);
            android.util.Slog.e(r2, r4.toString(), r3);
     */
    /* JADX WARNING: Missing block: B:55:0x00ef, code skipped:
            android.os.StrictMode.setThreadPolicyMask(r0);
     */
    /* JADX WARNING: Missing block: B:56:0x00f3, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:59:?, code skipped:
            r9.mErrors++;
            r2 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("File not exist: ");
            r4.append(r9.mProc);
            android.util.Slog.w(r2, r4.toString());
     */
    /* JADX WARNING: Missing block: B:60:0x0112, code skipped:
            android.os.StrictMode.setThreadPolicyMask(r0);
     */
    /* JADX WARNING: Missing block: B:61:0x0116, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:62:0x0117, code skipped:
            android.os.StrictMode.setThreadPolicyMask(r0);
     */
    /* JADX WARNING: Missing block: B:63:0x011a, code skipped:
            throw r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ByteBuffer readBytes() {
        FileChannel fc;
        Throwable th;
        Throwable th2;
        if (this.mErrors >= 5) {
            return null;
        }
        if (SystemClock.elapsedRealtime() >= this.mLastReadTime + this.mThrottleInterval) {
            this.mLastReadTime = SystemClock.elapsedRealtime();
            this.mBuffer.clear();
            int oldMask = StrictMode.allowThreadDiskReadsMask();
            try {
                fc = FileChannel.open(this.mProc, new OpenOption[]{StandardOpenOption.READ});
                while (fc.read(this.mBuffer) == this.mBuffer.capacity()) {
                    try {
                        if (resize()) {
                            fc.position(0);
                        } else {
                            this.mErrors++;
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Proc file is too large: ");
                            stringBuilder.append(this.mProc);
                            Slog.e(str, stringBuilder.toString());
                            if (fc != null) {
                                fc.close();
                            }
                            StrictMode.setThreadPolicyMask(oldMask);
                            return null;
                        }
                    } catch (Throwable th22) {
                        Throwable th3 = th22;
                        th22 = th;
                        th = th3;
                    }
                }
                if (fc != null) {
                    fc.close();
                }
                StrictMode.setThreadPolicyMask(oldMask);
                this.mBuffer.flip();
                return this.mBuffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder());
            } catch (FileNotFoundException | NoSuchFileException e) {
            } catch (Throwable th4) {
                th22.addSuppressed(th4);
            }
        } else if (this.mBuffer.limit() <= 0 || this.mBuffer.limit() >= this.mBuffer.capacity()) {
            return null;
        } else {
            return this.mBuffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder());
        }
        if (fc != null) {
            if (th22 != null) {
                fc.close();
            } else {
                fc.close();
            }
        }
        throw th;
        throw th;
    }

    public void setThrottleInterval(long throttleInterval) {
        if (throttleInterval >= 0) {
            this.mThrottleInterval = throttleInterval;
        }
    }

    private boolean resize() {
        if (this.mBuffer.capacity() >= MAX_BUFFER_SIZE) {
            return false;
        }
        this.mBuffer = ByteBuffer.allocateDirect(Math.min(this.mBuffer.capacity() << 1, MAX_BUFFER_SIZE));
        return true;
    }
}
