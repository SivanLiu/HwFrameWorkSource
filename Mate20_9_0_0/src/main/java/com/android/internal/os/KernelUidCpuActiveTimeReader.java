package com.android.internal.os;

import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

public class KernelUidCpuActiveTimeReader extends KernelUidCpuTimeReaderBase<Callback> {
    private static final String TAG = KernelUidCpuActiveTimeReader.class.getSimpleName();
    private int mCores;
    private SparseArray<Double> mLastUidCpuActiveTimeMs;
    private final KernelCpuProcReader mProcReader;

    public interface Callback extends com.android.internal.os.KernelUidCpuTimeReaderBase.Callback {
        void onUidCpuActiveTime(int i, long j);
    }

    public KernelUidCpuActiveTimeReader() {
        this.mLastUidCpuActiveTimeMs = new SparseArray();
        this.mProcReader = KernelCpuProcReader.getActiveTimeReaderInstance();
    }

    @VisibleForTesting
    public KernelUidCpuActiveTimeReader(KernelCpuProcReader procReader) {
        this.mLastUidCpuActiveTimeMs = new SparseArray();
        this.mProcReader = procReader;
    }

    protected void readDeltaImpl(Callback callback) {
        readImpl(new -$$Lambda$KernelUidCpuActiveTimeReader$bd1LhtH6p3uJgMUQoWfE2Qs8bRc(this, callback));
    }

    public static /* synthetic */ void lambda$readDeltaImpl$0(KernelUidCpuActiveTimeReader kernelUidCpuActiveTimeReader, Callback callback, IntBuffer buf) {
        int uid = buf.get();
        double activeTime = kernelUidCpuActiveTimeReader.sumActiveTime(buf);
        if (activeTime > 0.0d) {
            double delta = activeTime - ((Double) kernelUidCpuActiveTimeReader.mLastUidCpuActiveTimeMs.get(uid, Double.valueOf(0.0d))).doubleValue();
            if (delta > 0.0d) {
                kernelUidCpuActiveTimeReader.mLastUidCpuActiveTimeMs.put(uid, Double.valueOf(activeTime));
                if (callback != null) {
                    callback.onUidCpuActiveTime(uid, (long) delta);
                }
            } else if (delta < 0.0d) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Negative delta from active time proc: ");
                stringBuilder.append(delta);
                Slog.e(str, stringBuilder.toString());
            }
        }
    }

    public void readAbsolute(Callback callback) {
        readImpl(new -$$Lambda$KernelUidCpuActiveTimeReader$uXm3GBhF7PBpo0hLrva14EQYjPA(this, callback));
    }

    public static /* synthetic */ void lambda$readAbsolute$1(KernelUidCpuActiveTimeReader kernelUidCpuActiveTimeReader, Callback callback, IntBuffer buf) {
        int uid = buf.get();
        double activeTime = kernelUidCpuActiveTimeReader.sumActiveTime(buf);
        if (activeTime > 0.0d) {
            callback.onUidCpuActiveTime(uid, (long) activeTime);
        }
    }

    private double sumActiveTime(IntBuffer buffer) {
        double sum = 0.0d;
        boolean corrupted = false;
        for (int j = 1; j <= this.mCores; j++) {
            int time = buffer.get();
            if (time < 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Negative time from active time proc: ");
                stringBuilder.append(time);
                Slog.e(str, stringBuilder.toString());
                corrupted = true;
            } else {
                sum += (((double) time) * 10.0d) / ((double) j);
            }
        }
        return corrupted ? -1.0d : sum;
    }

    /* JADX WARNING: Missing block: B:34:0x00a7, code skipped:
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
                        stringBuilder.append("Cannot parse active time proc bytes to int: ");
                        stringBuilder.append(bytes.remaining());
                        Slog.wtf(str, stringBuilder.toString());
                        return;
                    }
                    IntBuffer buf = bytes.asIntBuffer();
                    int cores = buf.get();
                    String str2;
                    StringBuilder stringBuilder2;
                    if (this.mCores == 0 || cores == this.mCores) {
                        this.mCores = cores;
                        if (cores > 0) {
                            if (buf.remaining() % (cores + 1) == 0) {
                                int numUids = buf.remaining() / (cores + 1);
                                for (int i = 0; i < numUids; i++) {
                                    processUid.accept(buf);
                                }
                                return;
                            }
                        }
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Cpu active time format error: ");
                        stringBuilder2.append(buf.remaining());
                        stringBuilder2.append(" / ");
                        stringBuilder2.append(cores + 1);
                        Slog.wtf(str2, stringBuilder2.toString());
                        return;
                    }
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Cpu active time wrong # cores: ");
                    stringBuilder2.append(cores);
                    Slog.wtf(str2, stringBuilder2.toString());
                }
            }
        }
    }

    public void removeUid(int uid) {
        this.mLastUidCpuActiveTimeMs.delete(uid);
    }

    public void removeUidsInRange(int startUid, int endUid) {
        this.mLastUidCpuActiveTimeMs.put(startUid, null);
        this.mLastUidCpuActiveTimeMs.put(endUid, null);
        int firstIndex = this.mLastUidCpuActiveTimeMs.indexOfKey(startUid);
        this.mLastUidCpuActiveTimeMs.removeAtRange(firstIndex, (this.mLastUidCpuActiveTimeMs.indexOfKey(endUid) - firstIndex) + 1);
    }
}
