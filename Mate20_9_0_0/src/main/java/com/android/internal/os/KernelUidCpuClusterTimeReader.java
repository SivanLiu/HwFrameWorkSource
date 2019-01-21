package com.android.internal.os;

import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

public class KernelUidCpuClusterTimeReader extends KernelUidCpuTimeReaderBase<Callback> {
    private static final String TAG = KernelUidCpuClusterTimeReader.class.getSimpleName();
    private double[] mCurTime;
    private long[] mCurTimeRounded;
    private long[] mDeltaTime;
    private SparseArray<double[]> mLastUidPolicyTimeMs;
    private int mNumClusters;
    private int mNumCores;
    private int[] mNumCoresOnCluster;
    private final KernelCpuProcReader mProcReader;

    public interface Callback extends com.android.internal.os.KernelUidCpuTimeReaderBase.Callback {
        void onUidCpuPolicyTime(int i, long[] jArr);
    }

    public KernelUidCpuClusterTimeReader() {
        this.mLastUidPolicyTimeMs = new SparseArray();
        this.mNumClusters = -1;
        this.mProcReader = KernelCpuProcReader.getClusterTimeReaderInstance();
    }

    @VisibleForTesting
    public KernelUidCpuClusterTimeReader(KernelCpuProcReader procReader) {
        this.mLastUidPolicyTimeMs = new SparseArray();
        this.mNumClusters = -1;
        this.mProcReader = procReader;
    }

    protected void readDeltaImpl(Callback cb) {
        readImpl(new -$$Lambda$KernelUidCpuClusterTimeReader$j4vHMa0qvl5KRBiWr-LkFJbasC8(this, cb));
    }

    public static /* synthetic */ void lambda$readDeltaImpl$0(KernelUidCpuClusterTimeReader kernelUidCpuClusterTimeReader, Callback cb, IntBuffer buf) {
        int uid = buf.get();
        double[] lastTimes = (double[]) kernelUidCpuClusterTimeReader.mLastUidPolicyTimeMs.get(uid);
        if (lastTimes == null) {
            lastTimes = new double[kernelUidCpuClusterTimeReader.mNumClusters];
            kernelUidCpuClusterTimeReader.mLastUidPolicyTimeMs.put(uid, lastTimes);
        }
        if (kernelUidCpuClusterTimeReader.sumClusterTime(buf, kernelUidCpuClusterTimeReader.mCurTime)) {
            boolean notify = false;
            boolean valid = true;
            for (int i = 0; i < kernelUidCpuClusterTimeReader.mNumClusters; i++) {
                kernelUidCpuClusterTimeReader.mDeltaTime[i] = (long) (kernelUidCpuClusterTimeReader.mCurTime[i] - lastTimes[i]);
                if (kernelUidCpuClusterTimeReader.mDeltaTime[i] < 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Negative delta from cluster time proc: ");
                    stringBuilder.append(kernelUidCpuClusterTimeReader.mDeltaTime[i]);
                    Slog.e(str, stringBuilder.toString());
                    valid = false;
                }
                notify |= kernelUidCpuClusterTimeReader.mDeltaTime[i] > 0 ? 1 : 0;
            }
            if (notify && valid) {
                System.arraycopy(kernelUidCpuClusterTimeReader.mCurTime, 0, lastTimes, 0, kernelUidCpuClusterTimeReader.mNumClusters);
                if (cb != null) {
                    cb.onUidCpuPolicyTime(uid, kernelUidCpuClusterTimeReader.mDeltaTime);
                }
            }
        }
    }

    public void readAbsolute(Callback callback) {
        readImpl(new -$$Lambda$KernelUidCpuClusterTimeReader$SvNbuRWT162Eb4ur1GVE0r4GiDo(this, callback));
    }

    public static /* synthetic */ void lambda$readAbsolute$1(KernelUidCpuClusterTimeReader kernelUidCpuClusterTimeReader, Callback callback, IntBuffer buf) {
        int uid = buf.get();
        if (kernelUidCpuClusterTimeReader.sumClusterTime(buf, kernelUidCpuClusterTimeReader.mCurTime)) {
            for (int i = 0; i < kernelUidCpuClusterTimeReader.mNumClusters; i++) {
                kernelUidCpuClusterTimeReader.mCurTimeRounded[i] = (long) kernelUidCpuClusterTimeReader.mCurTime[i];
            }
            callback.onUidCpuPolicyTime(uid, kernelUidCpuClusterTimeReader.mCurTimeRounded);
        }
    }

    private boolean sumClusterTime(IntBuffer buffer, double[] clusterTime) {
        boolean valid = true;
        for (int i = 0; i < this.mNumClusters; i++) {
            clusterTime[i] = 0.0d;
            for (int j = 1; j <= this.mNumCoresOnCluster[i]; j++) {
                int time = buffer.get();
                if (time < 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Negative time from cluster time proc: ");
                    stringBuilder.append(time);
                    Slog.e(str, stringBuilder.toString());
                    valid = false;
                }
                clusterTime[i] = clusterTime[i] + ((((double) time) * 10.0d) / ((double) j));
            }
        }
        return valid;
    }

    /* JADX WARNING: Missing block: B:44:0x00df, code skipped:
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
                        stringBuilder.append("Cannot parse cluster time proc bytes to int: ");
                        stringBuilder.append(bytes.remaining());
                        Slog.wtf(str, stringBuilder.toString());
                        return;
                    }
                    IntBuffer buf = bytes.asIntBuffer();
                    int numClusters = buf.get();
                    String str2;
                    StringBuilder stringBuilder2;
                    if (numClusters <= 0) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Cluster time format error: ");
                        stringBuilder2.append(numClusters);
                        Slog.wtf(str2, stringBuilder2.toString());
                        return;
                    }
                    if (this.mNumClusters == -1) {
                        this.mNumClusters = numClusters;
                    }
                    if (buf.remaining() < numClusters) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Too few data left in the buffer: ");
                        stringBuilder2.append(buf.remaining());
                        Slog.wtf(str2, stringBuilder2.toString());
                        return;
                    }
                    if (this.mNumCores > 0) {
                        buf.position(buf.position() + numClusters);
                    } else if (!readCoreInfo(buf, numClusters)) {
                        return;
                    }
                    if (buf.remaining() % (this.mNumCores + 1) != 0) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Cluster time format error: ");
                        stringBuilder2.append(buf.remaining());
                        stringBuilder2.append(" / ");
                        stringBuilder2.append(this.mNumCores + 1);
                        Slog.wtf(str2, stringBuilder2.toString());
                        return;
                    }
                    int numUids = buf.remaining() / (this.mNumCores + 1);
                    for (int i = 0; i < numUids; i++) {
                        processUid.accept(buf);
                    }
                }
            }
        }
    }

    private boolean readCoreInfo(IntBuffer buf, int numClusters) {
        int[] numCoresOnCluster = new int[numClusters];
        int numCores = 0;
        for (int i = 0; i < numClusters; i++) {
            numCoresOnCluster[i] = buf.get();
            numCores += numCoresOnCluster[i];
        }
        if (numCores <= 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid # cores from cluster time proc file: ");
            stringBuilder.append(numCores);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
        this.mNumCores = numCores;
        this.mNumCoresOnCluster = numCoresOnCluster;
        this.mCurTime = new double[numClusters];
        this.mDeltaTime = new long[numClusters];
        this.mCurTimeRounded = new long[numClusters];
        return true;
    }

    public void removeUid(int uid) {
        this.mLastUidPolicyTimeMs.delete(uid);
    }

    public void removeUidsInRange(int startUid, int endUid) {
        this.mLastUidPolicyTimeMs.put(startUid, null);
        this.mLastUidPolicyTimeMs.put(endUid, null);
        int firstIndex = this.mLastUidPolicyTimeMs.indexOfKey(startUid);
        this.mLastUidPolicyTimeMs.removeAtRange(firstIndex, (this.mLastUidPolicyTimeMs.indexOfKey(endUid) - firstIndex) + 1);
    }
}
