package com.android.internal.os;

import android.os.StrictMode;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

public class KernelUidCpuFreqTimeReader extends KernelUidCpuTimeReaderBase<Callback> {
    private static final String TAG = KernelUidCpuFreqTimeReader.class.getSimpleName();
    private static final int TOTAL_READ_ERROR_COUNT = 5;
    static final String UID_TIMES_PROC_FILE = "/proc/uid_time_in_state";
    private boolean mAllUidTimesAvailable;
    private long[] mCpuFreqs;
    private int mCpuFreqsCount;
    private long[] mCurTimes;
    private long[] mDeltaTimes;
    private SparseArray<long[]> mLastUidCpuFreqTimeMs;
    private boolean mPerClusterTimesAvailable;
    private final KernelCpuProcReader mProcReader;
    private int mReadErrorCounter;

    public interface Callback extends com.android.internal.os.KernelUidCpuTimeReaderBase.Callback {
        void onUidCpuFreqTime(int i, long[] jArr);
    }

    public KernelUidCpuFreqTimeReader() {
        this.mLastUidCpuFreqTimeMs = new SparseArray();
        this.mAllUidTimesAvailable = true;
        this.mProcReader = KernelCpuProcReader.getFreqTimeReaderInstance();
    }

    @VisibleForTesting
    public KernelUidCpuFreqTimeReader(KernelCpuProcReader procReader) {
        this.mLastUidCpuFreqTimeMs = new SparseArray();
        this.mAllUidTimesAvailable = true;
        this.mProcReader = procReader;
    }

    public boolean perClusterTimesAvailable() {
        return this.mPerClusterTimesAvailable;
    }

    public boolean allUidTimesAvailable() {
        return this.mAllUidTimesAvailable;
    }

    public SparseArray<long[]> getAllUidCpuFreqTimeMs() {
        return this.mLastUidCpuFreqTimeMs;
    }

    public long[] readFreqs(PowerProfile powerProfile) {
        long[] fileReader;
        BufferedReader reader;
        Throwable th;
        Preconditions.checkNotNull(powerProfile);
        if (this.mCpuFreqs != null) {
            return this.mCpuFreqs;
        }
        if (!this.mAllUidTimesAvailable) {
            return null;
        }
        int oldMask = StrictMode.allowThreadDiskReadsMask();
        try {
            fileReader = new FileReader(UID_TIMES_PROC_FILE);
            reader = new BufferedReader(fileReader);
            try {
                fileReader = readFreqs(reader, powerProfile);
                reader.close();
                return fileReader;
            } catch (Throwable th2) {
                Throwable th3 = th2;
                th2 = r3;
                fileReader = th3;
            }
        } catch (IOException e) {
            int i = this.mReadErrorCounter + 1;
            this.mReadErrorCounter = i;
            if (i >= 5) {
                this.mAllUidTimesAvailable = false;
            }
            fileReader = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read /proc/uid_time_in_state: ");
            stringBuilder.append(e);
            Slog.e(fileReader, stringBuilder.toString());
            return null;
        } finally {
            StrictMode.setThreadPolicyMask(oldMask);
        }
        throw fileReader;
        if (th2 != null) {
            try {
                reader.close();
            } catch (Throwable th4) {
                th2.addSuppressed(th4);
            }
        } else {
            reader.close();
        }
        throw fileReader;
    }

    @VisibleForTesting
    public long[] readFreqs(BufferedReader reader, PowerProfile powerProfile) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        String[] freqStr = line.split(" ");
        this.mCpuFreqsCount = freqStr.length - 1;
        this.mCpuFreqs = new long[this.mCpuFreqsCount];
        this.mCurTimes = new long[this.mCpuFreqsCount];
        this.mDeltaTimes = new long[this.mCpuFreqsCount];
        for (int i = 0; i < this.mCpuFreqsCount; i++) {
            this.mCpuFreqs[i] = Long.parseLong(freqStr[i + 1], 10);
        }
        IntArray numClusterFreqs = extractClusterInfoFromProcFileFreqs();
        int numClusters = powerProfile.getNumCpuClusters();
        if (numClusterFreqs.size() == numClusters) {
            this.mPerClusterTimesAvailable = true;
            for (int i2 = 0; i2 < numClusters; i2++) {
                if (numClusterFreqs.get(i2) != powerProfile.getNumSpeedStepsInCpuCluster(i2)) {
                    this.mPerClusterTimesAvailable = false;
                    break;
                }
            }
        } else {
            this.mPerClusterTimesAvailable = false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mPerClusterTimesAvailable=");
        stringBuilder.append(this.mPerClusterTimesAvailable);
        Slog.i(str, stringBuilder.toString());
        return this.mCpuFreqs;
    }

    @VisibleForTesting
    public void readDeltaImpl(Callback callback) {
        if (this.mCpuFreqs != null) {
            readImpl(new -$$Lambda$KernelUidCpuFreqTimeReader$_LfRKir9FA4B4VL15YGHagRZaR8(this, callback));
        }
    }

    public static /* synthetic */ void lambda$readDeltaImpl$0(KernelUidCpuFreqTimeReader kernelUidCpuFreqTimeReader, Callback callback, IntBuffer buf) {
        int uid = buf.get();
        long[] lastTimes = (long[]) kernelUidCpuFreqTimeReader.mLastUidCpuFreqTimeMs.get(uid);
        if (lastTimes == null) {
            lastTimes = new long[kernelUidCpuFreqTimeReader.mCpuFreqsCount];
            kernelUidCpuFreqTimeReader.mLastUidCpuFreqTimeMs.put(uid, lastTimes);
        }
        if (kernelUidCpuFreqTimeReader.getFreqTimeForUid(buf, kernelUidCpuFreqTimeReader.mCurTimes)) {
            boolean valid = true;
            boolean notify = false;
            for (int i = 0; i < kernelUidCpuFreqTimeReader.mCpuFreqsCount; i++) {
                kernelUidCpuFreqTimeReader.mDeltaTimes[i] = kernelUidCpuFreqTimeReader.mCurTimes[i] - lastTimes[i];
                if (kernelUidCpuFreqTimeReader.mDeltaTimes[i] < 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Negative delta from freq time proc: ");
                    stringBuilder.append(kernelUidCpuFreqTimeReader.mDeltaTimes[i]);
                    Slog.e(str, stringBuilder.toString());
                    valid = false;
                }
                notify |= kernelUidCpuFreqTimeReader.mDeltaTimes[i] > 0 ? 1 : 0;
            }
            if (notify && valid) {
                System.arraycopy(kernelUidCpuFreqTimeReader.mCurTimes, 0, lastTimes, 0, kernelUidCpuFreqTimeReader.mCpuFreqsCount);
                if (callback != null) {
                    callback.onUidCpuFreqTime(uid, kernelUidCpuFreqTimeReader.mDeltaTimes);
                }
            }
        }
    }

    public void readAbsolute(Callback callback) {
        readImpl(new -$$Lambda$KernelUidCpuFreqTimeReader$s7iJKg0yjXXtqM4hsU8GS_gavIY(this, callback));
    }

    public static /* synthetic */ void lambda$readAbsolute$1(KernelUidCpuFreqTimeReader kernelUidCpuFreqTimeReader, Callback callback, IntBuffer buf) {
        int uid = buf.get();
        if (kernelUidCpuFreqTimeReader.getFreqTimeForUid(buf, kernelUidCpuFreqTimeReader.mCurTimes)) {
            callback.onUidCpuFreqTime(uid, kernelUidCpuFreqTimeReader.mCurTimes);
        }
    }

    private boolean getFreqTimeForUid(IntBuffer buffer, long[] freqTime) {
        boolean valid = true;
        for (int i = 0; i < this.mCpuFreqsCount; i++) {
            freqTime[i] = ((long) buffer.get()) * 10;
            if (freqTime[i] < 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Negative time from freq time proc: ");
                stringBuilder.append(freqTime[i]);
                Slog.e(str, stringBuilder.toString());
                valid = false;
            }
        }
        return valid;
    }

    /* JADX WARNING: Missing block: B:29:0x00a8, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readImpl(Consumer<IntBuffer> processUid) {
        synchronized (this.mProcReader) {
            ByteBuffer bytes = this.mProcReader.readBytes();
            if (bytes != null) {
                if (bytes.remaining() > 4) {
                    if ((bytes.remaining() & 3) != 0) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot parse freq time proc bytes to int: ");
                        stringBuilder.append(bytes.remaining());
                        Slog.wtf(str, stringBuilder.toString());
                        return;
                    }
                    IntBuffer buf = bytes.asIntBuffer();
                    int freqs = buf.get();
                    String str2;
                    StringBuilder stringBuilder2;
                    if (freqs != this.mCpuFreqsCount) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Cpu freqs expect ");
                        stringBuilder2.append(this.mCpuFreqsCount);
                        stringBuilder2.append(" , got ");
                        stringBuilder2.append(freqs);
                        Slog.wtf(str2, stringBuilder2.toString());
                    } else if (buf.remaining() % (freqs + 1) != 0) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Freq time format error: ");
                        stringBuilder2.append(buf.remaining());
                        stringBuilder2.append(" / ");
                        stringBuilder2.append(freqs + 1);
                        Slog.wtf(str2, stringBuilder2.toString());
                    } else {
                        int numUids = buf.remaining() / (freqs + 1);
                        for (int i = 0; i < numUids; i++) {
                            processUid.accept(buf);
                        }
                    }
                }
            }
        }
    }

    public void removeUid(int uid) {
        this.mLastUidCpuFreqTimeMs.delete(uid);
    }

    public void removeUidsInRange(int startUid, int endUid) {
        this.mLastUidCpuFreqTimeMs.put(startUid, null);
        this.mLastUidCpuFreqTimeMs.put(endUid, null);
        int firstIndex = this.mLastUidCpuFreqTimeMs.indexOfKey(startUid);
        this.mLastUidCpuFreqTimeMs.removeAtRange(firstIndex, (this.mLastUidCpuFreqTimeMs.indexOfKey(endUid) - firstIndex) + 1);
    }

    private IntArray extractClusterInfoFromProcFileFreqs() {
        IntArray numClusterFreqs = new IntArray();
        int freqsFound = 0;
        int i = 0;
        while (i < this.mCpuFreqsCount) {
            freqsFound++;
            if (i + 1 == this.mCpuFreqsCount || this.mCpuFreqs[i + 1] <= this.mCpuFreqs[i]) {
                numClusterFreqs.add(freqsFound);
                freqsFound = 0;
            }
            i++;
        }
        return numClusterFreqs;
    }
}
