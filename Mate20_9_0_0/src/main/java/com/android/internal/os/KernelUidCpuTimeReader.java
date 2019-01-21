package com.android.internal.os;

import android.os.StrictMode;
import android.os.SystemClock;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Slog;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import com.android.internal.content.NativeLibraryHelper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class KernelUidCpuTimeReader extends KernelUidCpuTimeReaderBase<Callback> {
    private static final String TAG = KernelUidCpuTimeReader.class.getSimpleName();
    private static final String sProcFile = "/proc/uid_cputime/show_uid_stat";
    private static final String sRemoveUidProcFile = "/proc/uid_cputime/remove_uid_range";
    private SparseLongArray mLastSystemTimeUs = new SparseLongArray();
    private long mLastTimeReadUs = 0;
    private SparseLongArray mLastUserTimeUs = new SparseLongArray();

    public interface Callback extends com.android.internal.os.KernelUidCpuTimeReaderBase.Callback {
        void onUidCpuTime(int i, long j, long j2);
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x00a9  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0099 A:{SYNTHETIC, Splitter:B:30:0x0099} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x01bc A:{Catch:{ Throwable -> 0x01f2, all -> 0x01eb }} */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00b4 A:{SYNTHETIC, Splitter:B:38:0x00b4} */
    /* JADX WARNING: Removed duplicated region for block: B:127:0x01e1 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x01d6 A:{Catch:{ Throwable -> 0x01f2, all -> 0x01eb }} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0065 A:{ExcHandler: Throwable (r0_4 'splitter' android.text.TextUtils$SimpleStringSplitter), Splitter:B:12:0x0054} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:16:0x0060, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:17:0x0061, code skipped:
            r4 = r3;
            r3 = r7;
     */
    /* JADX WARNING: Missing block: B:18:0x0065, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:19:0x0066, code skipped:
            r7 = r0;
            r4 = r3;
            r35 = r8;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void readDeltaImpl(Callback callback) {
        Throwable splitter;
        long nowUs;
        Throwable th;
        IOException e;
        String str;
        StringBuilder stringBuilder;
        NumberFormatException e2;
        StringIndexOutOfBoundsException e3;
        long nowUs2;
        String line;
        SimpleStringSplitter simpleStringSplitter;
        int oldMask = StrictMode.allowThreadDiskReadsMask();
        long index = 1000;
        long nowUs3 = SystemClock.elapsedRealtime() * 1000;
        Throwable th2 = null;
        String line2 = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(sProcFile));
            try {
                SimpleStringSplitter splitter2 = new SimpleStringSplitter(' ');
                while (true) {
                    String readLine = reader.readLine();
                    line2 = readLine;
                    if (readLine != null) {
                        try {
                            long systemTimeUs;
                            long powerMaUs;
                            long systemTimeDeltaUs;
                            SimpleStringSplitter splitter3;
                            boolean notifyCallback;
                            long userTimeDeltaUs;
                            splitter2.setString(line2);
                            readLine = splitter2.next();
                            int uid = Integer.parseInt(readLine.substring(0, readLine.length() - 1), 10);
                            long systemTimeDeltaUs2 = Long.parseLong(splitter2.next(), 10);
                            long systemTimeUs2 = 0;
                            if (splitter2.hasNext()) {
                                try {
                                    systemTimeUs2 = Long.parseLong(splitter2.next(), 10);
                                } catch (Throwable th3) {
                                    splitter = th3;
                                    nowUs = nowUs3;
                                    th = null;
                                    try {
                                        $closeResource(th, reader);
                                        throw splitter;
                                    } catch (IOException e4) {
                                        e = e4;
                                        str = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Failed to read uid_cputime: ");
                                        stringBuilder.append(e.getMessage());
                                        Slog.e(str, stringBuilder.toString());
                                        StrictMode.setThreadPolicyMask(oldMask);
                                        this.mLastTimeReadUs = nowUs;
                                    } catch (NumberFormatException e5) {
                                        e2 = e5;
                                        str = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("read uid_cputime has NumberFormatException, line:");
                                        stringBuilder.append(line2);
                                        Slog.e(str, stringBuilder.toString());
                                        Slog.e(TAG, "Failed to read uid_cputime", e2);
                                        StrictMode.setThreadPolicyMask(oldMask);
                                        this.mLastTimeReadUs = nowUs;
                                    } catch (StringIndexOutOfBoundsException e6) {
                                        e3 = e6;
                                        str = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("read uid_cputime has StringIndexOutOfBoundsException, line:");
                                        stringBuilder.append(line2);
                                        Slog.e(str, stringBuilder.toString());
                                        Slog.e(TAG, "Failed to read uid_cputime", e3);
                                        StrictMode.setThreadPolicyMask(oldMask);
                                        this.mLastTimeReadUs = nowUs;
                                    }
                                }
                            }
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Read uid_cputime has system time format exception when split line:");
                            stringBuilder2.append(line2);
                            Slog.w(str2, stringBuilder2.toString());
                            int uIdIndex = this.mLastUserTimeUs.indexOfKey(uid);
                            if (uIdIndex >= 0) {
                                systemTimeUs = this.mLastSystemTimeUs.valueAt(uIdIndex);
                                if (splitter2.hasNext()) {
                                    powerMaUs = 0;
                                } else {
                                    powerMaUs = Long.parseLong(splitter2.next(), 10) / index;
                                }
                                powerMaUs = systemTimeDeltaUs2;
                                systemTimeDeltaUs = systemTimeUs;
                                if (callback != null) {
                                    splitter3 = splitter2;
                                    nowUs2 = nowUs3;
                                    notifyCallback = false;
                                    line = line2;
                                    nowUs3 = systemTimeDeltaUs2;
                                } else if (this.mLastTimeReadUs != 0) {
                                    boolean z;
                                    int index2 = this.mLastUserTimeUs.indexOfKey(uid);
                                    int i;
                                    if (index2 >= 0) {
                                        powerMaUs -= this.mLastUserTimeUs.valueAt(index2);
                                        systemTimeDeltaUs -= this.mLastSystemTimeUs.valueAt(index2);
                                        notifyCallback = false;
                                        long timeDiffUs = nowUs3 - this.mLastTimeReadUs;
                                        if (powerMaUs >= 0) {
                                            if (systemTimeDeltaUs >= 0) {
                                                splitter3 = splitter2;
                                                nowUs2 = nowUs3;
                                                i = index2;
                                                line = line2;
                                                nowUs3 = systemTimeDeltaUs2;
                                                index = 1000;
                                            }
                                        }
                                        splitter3 = splitter2;
                                        StringBuilder splitter4 = new StringBuilder("Malformed cpu data for UID=");
                                        splitter4.append(uid);
                                        splitter4.append("!\n");
                                        splitter4.append("Time between reads: ");
                                        nowUs2 = nowUs3;
                                        try {
                                            TimeUtils.formatDuration(timeDiffUs / 1000, splitter4);
                                            splitter4.append("\n");
                                            splitter4.append("Previous times: u=");
                                            TimeUtils.formatDuration(this.mLastUserTimeUs.valueAt(index2) / 1000, splitter4);
                                            splitter4.append(" s=");
                                            TimeUtils.formatDuration(this.mLastSystemTimeUs.valueAt(index2) / 1000, splitter4);
                                            splitter4.append("\nCurrent times: u=");
                                            nowUs3 = systemTimeDeltaUs2;
                                            TimeUtils.formatDuration(nowUs3 / 1000, splitter4);
                                            splitter4.append(" s=");
                                            line = line2;
                                        } catch (Throwable th4) {
                                            splitter = th4;
                                            line = line2;
                                            nowUs = nowUs2;
                                            th = null;
                                            $closeResource(th, reader);
                                            throw splitter;
                                        }
                                        try {
                                            TimeUtils.formatDuration(systemTimeUs / 1000, splitter4);
                                            splitter4.append("\nDelta: u=");
                                            TimeUtils.formatDuration(powerMaUs / 1000, splitter4);
                                            splitter4.append(" s=");
                                            index = 1000;
                                            TimeUtils.formatDuration(systemTimeDeltaUs / 1000, splitter4);
                                            Slog.e(TAG, splitter4.toString());
                                            powerMaUs = 0;
                                            systemTimeDeltaUs = 0;
                                        } catch (Throwable th5) {
                                            splitter = th5;
                                            nowUs = nowUs2;
                                            line2 = line;
                                            th = null;
                                            $closeResource(th, reader);
                                            throw splitter;
                                        }
                                    }
                                    splitter3 = splitter2;
                                    nowUs2 = nowUs3;
                                    i = index2;
                                    notifyCallback = false;
                                    line = line2;
                                    nowUs3 = systemTimeDeltaUs2;
                                    index = 1000;
                                    if (powerMaUs == 0) {
                                        if (systemTimeDeltaUs == 0) {
                                            z = false;
                                            notifyCallback = z;
                                        }
                                    }
                                    z = true;
                                    notifyCallback = z;
                                } else {
                                    splitter3 = splitter2;
                                    nowUs2 = nowUs3;
                                    notifyCallback = false;
                                    line = line2;
                                    nowUs3 = systemTimeDeltaUs2;
                                    index = 1000;
                                }
                                userTimeDeltaUs = powerMaUs;
                                systemTimeDeltaUs2 = systemTimeDeltaUs;
                                this.mLastUserTimeUs.put(uid, nowUs3);
                                this.mLastSystemTimeUs.put(uid, systemTimeUs);
                                if (!notifyCallback) {
                                    callback.onUidCpuTime(uid, userTimeDeltaUs, systemTimeDeltaUs2);
                                }
                                splitter2 = splitter3;
                                nowUs3 = nowUs2;
                                line2 = line;
                                th2 = null;
                            }
                            systemTimeUs = systemTimeUs2;
                            if (splitter2.hasNext()) {
                            }
                            powerMaUs = systemTimeDeltaUs2;
                            systemTimeDeltaUs = systemTimeUs;
                            if (callback != null) {
                            }
                            userTimeDeltaUs = powerMaUs;
                            systemTimeDeltaUs2 = systemTimeDeltaUs;
                            this.mLastUserTimeUs.put(uid, nowUs3);
                            this.mLastSystemTimeUs.put(uid, systemTimeUs);
                            if (!notifyCallback) {
                            }
                            splitter2 = splitter3;
                            nowUs3 = nowUs2;
                            line2 = line;
                            th2 = null;
                        } catch (Throwable th6) {
                            splitter = th6;
                            line = line2;
                            nowUs = nowUs3;
                            th = null;
                            $closeResource(th, reader);
                            throw splitter;
                        }
                    }
                    nowUs2 = nowUs3;
                    line = line2;
                    try {
                        $closeResource(null, reader);
                        StrictMode.setThreadPolicyMask(oldMask);
                        this.mLastTimeReadUs = nowUs2;
                        line2 = line;
                        return;
                    } catch (IOException e7) {
                        e = e7;
                        nowUs = nowUs2;
                        line2 = line;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to read uid_cputime: ");
                        stringBuilder.append(e.getMessage());
                        Slog.e(str, stringBuilder.toString());
                        StrictMode.setThreadPolicyMask(oldMask);
                        this.mLastTimeReadUs = nowUs;
                    } catch (NumberFormatException e8) {
                        e2 = e8;
                        nowUs = nowUs2;
                        line2 = line;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("read uid_cputime has NumberFormatException, line:");
                        stringBuilder.append(line2);
                        Slog.e(str, stringBuilder.toString());
                        Slog.e(TAG, "Failed to read uid_cputime", e2);
                        StrictMode.setThreadPolicyMask(oldMask);
                        this.mLastTimeReadUs = nowUs;
                    } catch (StringIndexOutOfBoundsException e9) {
                        e3 = e9;
                        nowUs = nowUs2;
                        line2 = line;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("read uid_cputime has StringIndexOutOfBoundsException, line:");
                        stringBuilder.append(line2);
                        Slog.e(str, stringBuilder.toString());
                        Slog.e(TAG, "Failed to read uid_cputime", e3);
                        StrictMode.setThreadPolicyMask(oldMask);
                        this.mLastTimeReadUs = nowUs;
                    } catch (Throwable th7) {
                        splitter = th7;
                        nowUs = nowUs2;
                        line2 = line;
                        StrictMode.setThreadPolicyMask(oldMask);
                        this.mLastTimeReadUs = nowUs;
                        throw splitter;
                    }
                }
            } catch (Throwable th8) {
                splitter = th8;
                nowUs = nowUs3;
                th = null;
                $closeResource(th, reader);
                throw splitter;
            }
        } catch (IOException e10) {
            e = e10;
            nowUs = nowUs3;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read uid_cputime: ");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            StrictMode.setThreadPolicyMask(oldMask);
            this.mLastTimeReadUs = nowUs;
        } catch (NumberFormatException e11) {
            e2 = e11;
            nowUs = nowUs3;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("read uid_cputime has NumberFormatException, line:");
            stringBuilder.append(line2);
            Slog.e(str, stringBuilder.toString());
            Slog.e(TAG, "Failed to read uid_cputime", e2);
            StrictMode.setThreadPolicyMask(oldMask);
            this.mLastTimeReadUs = nowUs;
        } catch (StringIndexOutOfBoundsException e12) {
            e3 = e12;
            nowUs = nowUs3;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("read uid_cputime has StringIndexOutOfBoundsException, line:");
            stringBuilder.append(line2);
            Slog.e(str, stringBuilder.toString());
            Slog.e(TAG, "Failed to read uid_cputime", e3);
            StrictMode.setThreadPolicyMask(oldMask);
            this.mLastTimeReadUs = nowUs;
        } catch (Throwable th9) {
            splitter = th9;
            StrictMode.setThreadPolicyMask(oldMask);
            this.mLastTimeReadUs = nowUs;
            throw splitter;
        }
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

    public void readAbsolute(Callback callback) {
        int oldMask = StrictMode.allowThreadDiskReadsMask();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(sProcFile));
            SimpleStringSplitter splitter = new SimpleStringSplitter(' ');
            while (true) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine == null) {
                    break;
                }
                splitter.setString(line);
                readLine = splitter.next();
                callback.onUidCpuTime(Integer.parseInt(readLine.substring(0, readLine.length() - 1), 10), Long.parseLong(splitter.next(), 10), Long.parseLong(splitter.next(), 10));
            }
            $closeResource(null, reader);
        } catch (IOException e) {
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to read uid_cputime: ");
                stringBuilder.append(e.getMessage());
                Slog.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                StrictMode.setThreadPolicyMask(oldMask);
            }
        } catch (Throwable th2) {
            $closeResource(r2, reader);
        }
        StrictMode.setThreadPolicyMask(oldMask);
    }

    public void removeUid(int uid) {
        int index = this.mLastSystemTimeUs.indexOfKey(uid);
        if (index >= 0) {
            this.mLastSystemTimeUs.removeAt(index);
            this.mLastUserTimeUs.removeAt(index);
        }
        removeUidsFromKernelModule(uid, uid);
    }

    public void removeUidsInRange(int startUid, int endUid) {
        if (endUid >= startUid) {
            this.mLastSystemTimeUs.put(startUid, 0);
            this.mLastUserTimeUs.put(startUid, 0);
            this.mLastSystemTimeUs.put(endUid, 0);
            this.mLastUserTimeUs.put(endUid, 0);
            int startIndex = this.mLastSystemTimeUs.indexOfKey(startUid);
            int endIndex = this.mLastSystemTimeUs.indexOfKey(endUid);
            this.mLastSystemTimeUs.removeAtRange(startIndex, (endIndex - startIndex) + 1);
            this.mLastUserTimeUs.removeAtRange(startIndex, (endIndex - startIndex) + 1);
            removeUidsFromKernelModule(startUid, endUid);
        }
    }

    private void removeUidsFromKernelModule(int startUid, int endUid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Removing uids ");
        stringBuilder.append(startUid);
        stringBuilder.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
        stringBuilder.append(endUid);
        Slog.d(str, stringBuilder.toString());
        int oldMask = StrictMode.allowThreadDiskWritesMask();
        FileWriter writer;
        StringBuilder stringBuilder2;
        try {
            writer = new FileWriter(sRemoveUidProcFile);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(startUid);
            stringBuilder2.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
            stringBuilder2.append(endUid);
            writer.write(stringBuilder2.toString());
            writer.flush();
            $closeResource(null, writer);
        } catch (IOException e) {
            try {
                String str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("failed to remove uids ");
                stringBuilder2.append(startUid);
                stringBuilder2.append(" - ");
                stringBuilder2.append(endUid);
                stringBuilder2.append(" from uid_cputime module");
                Slog.e(str2, stringBuilder2.toString(), e);
            } catch (Throwable th) {
                StrictMode.setThreadPolicyMask(oldMask);
            }
        } catch (Throwable th2) {
            $closeResource(r2, writer);
        }
        StrictMode.setThreadPolicyMask(oldMask);
    }
}
