package com.android.internal.os;

import android.os.Process;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.os.KernelWakelockStats.Entry;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

public class KernelWakelockReader {
    private static final int[] PROC_WAKELOCKS_FORMAT = new int[]{5129, 8201, 9, 9, 9, 8201};
    private static final String TAG = "KernelWakelockReader";
    private static final int[] WAKEUP_SOURCES_FORMAT = new int[]{4105, 8457, MetricsEvent.NOTIFICATION_TOPIC_NOTIFICATION, MetricsEvent.NOTIFICATION_TOPIC_NOTIFICATION, MetricsEvent.NOTIFICATION_TOPIC_NOTIFICATION, MetricsEvent.NOTIFICATION_TOPIC_NOTIFICATION, 8457};
    private static int sKernelWakelockUpdateVersion = 0;
    private static final String sWakelockFile = "/proc/wakelocks";
    private static final String sWakeupSourceFile = "/d/wakeup_sources";
    private final long[] mProcWakelocksData = new long[3];
    private final String[] mProcWakelocksName = new String[3];

    public final KernelWakelockStats readKernelWakelockStats(KernelWakelockStats staleStats) {
        byte[] buffer = new byte[32768];
        long startTime = SystemClock.uptimeMillis();
        int oldMask = StrictMode.allowThreadDiskReadsMask();
        IOException e;
        boolean wakeup_sources;
        int wakeup_sources2;
        try {
            e = new FileInputStream(sWakelockFile);
            wakeup_sources = null;
            try {
                String str;
                StringBuilder stringBuilder;
                wakeup_sources2 = e.read(buffer);
                e.close();
                long readTime = SystemClock.uptimeMillis() - startTime;
                if (readTime > 100) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Reading wakelock stats took ");
                    stringBuilder.append(readTime);
                    stringBuilder.append("ms");
                    Slog.w(str, stringBuilder.toString());
                }
                if (wakeup_sources2 > 0) {
                    if (wakeup_sources2 >= buffer.length) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Kernel wake locks exceeded buffer size ");
                        stringBuilder.append(buffer.length);
                        Slog.wtf(str, stringBuilder.toString());
                    }
                    for (int i = 0; i < wakeup_sources2; i++) {
                        if (buffer[i] == (byte) 0) {
                            wakeup_sources2 = i;
                            break;
                        }
                    }
                }
                return parseProcWakelocks(buffer, wakeup_sources2, wakeup_sources, staleStats);
            } catch (IOException e2) {
                wakeup_sources = TAG;
                wakeup_sources2 = "failed to read kernel wakelocks";
                Slog.wtf(wakeup_sources, wakeup_sources2, e2);
                return null;
            } finally {
                StrictMode.setThreadPolicyMask(oldMask);
            }
        } catch (FileNotFoundException e3) {
            try {
                wakeup_sources2 = sWakeupSourceFile;
                FileInputStream is = new FileInputStream(wakeup_sources2);
                wakeup_sources2 = true;
                e2 = is;
                wakeup_sources = 1;
            } catch (FileNotFoundException e4) {
                wakeup_sources = e4;
                wakeup_sources2 = TAG;
                Slog.wtf(wakeup_sources2, "neither /proc/wakelocks nor /d/wakeup_sources exists");
                StrictMode.setThreadPolicyMask(oldMask);
                return null;
            }
        }
    }

    @VisibleForTesting
    public KernelWakelockStats parseProcWakelocks(byte[] wlBuffer, int len, boolean wakeup_sources, KernelWakelockStats staleStats) {
        int i;
        byte b;
        int endIndex;
        int startIndex;
        Throwable th;
        byte[] bArr = wlBuffer;
        int i2 = len;
        KernelWakelockStats kernelWakelockStats = staleStats;
        int i3 = 0;
        int i4 = 0;
        while (true) {
            i = i4;
            b = (byte) 10;
            if (i >= i2 || bArr[i] == (byte) 10 || bArr[i] == (byte) 0) {
                i4 = i + 1;
                endIndex = i4;
                startIndex = i4;
            } else {
                i4 = i + 1;
            }
        }
        i4 = i + 1;
        endIndex = i4;
        startIndex = i4;
        synchronized (this) {
            try {
                int startIndex2;
                sKernelWakelockUpdateVersion++;
                int startIndex3 = startIndex;
                while (endIndex < i2) {
                    int endIndex2 = startIndex3;
                    while (endIndex2 < i2) {
                        try {
                            if (bArr[endIndex2] == b || bArr[endIndex2] == (byte) 0) {
                                break;
                            }
                            endIndex2++;
                        } catch (Throwable th2) {
                            th = th2;
                            endIndex = endIndex2;
                            startIndex = startIndex3;
                            throw th;
                        }
                    }
                    if (endIndex2 > i2 - 1) {
                        endIndex = endIndex2;
                        startIndex2 = startIndex3;
                        break;
                    }
                    try {
                        int[] iArr;
                        String[] nameStringArray = this.mProcWakelocksName;
                        long[] wlData = this.mProcWakelocksData;
                        for (i4 = startIndex3; i4 < endIndex2; i4++) {
                            if ((bArr[i4] & 128) != 0) {
                                bArr[i4] = (byte) 63;
                            }
                        }
                        if (wakeup_sources) {
                            iArr = WAKEUP_SOURCES_FORMAT;
                        } else {
                            iArr = PROC_WAKELOCKS_FORMAT;
                        }
                        int endIndex3 = endIndex2;
                        startIndex2 = startIndex3;
                        try {
                            long totalTime;
                            boolean parsed = Process.parseProcLine(bArr, startIndex3, endIndex2, iArr, nameStringArray, wlData, null);
                            String name = nameStringArray[i3];
                            int count = (int) wlData[1];
                            if (wakeup_sources) {
                                totalTime = wlData[2] * 1000;
                            } else {
                                totalTime = (wlData[2] + 500) / 1000;
                            }
                            long totalTime2 = totalTime;
                            if (!parsed || name.length() <= 0) {
                                if (!parsed) {
                                    String str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Failed to parse proc line: ");
                                    stringBuilder.append(new String(bArr, startIndex2, endIndex3 - startIndex2));
                                    Slog.wtf(str, stringBuilder.toString());
                                }
                            } else if (kernelWakelockStats.containsKey(name)) {
                                Entry kwlStats = (Entry) kernelWakelockStats.get(name);
                                if (kwlStats.mVersion == sKernelWakelockUpdateVersion) {
                                    kwlStats.mCount += count;
                                    kwlStats.mTotalTime += totalTime2;
                                } else {
                                    kwlStats.mCount = count;
                                    kwlStats.mTotalTime = totalTime2;
                                    kwlStats.mVersion = sKernelWakelockUpdateVersion;
                                }
                            } else {
                                kernelWakelockStats.put(name, new Entry(count, totalTime2, sKernelWakelockUpdateVersion));
                            }
                        } catch (Exception e) {
                            Slog.wtf(TAG, "Failed to parse proc line!");
                        } catch (Throwable th3) {
                            th = th3;
                            startIndex = startIndex2;
                            endIndex = endIndex3;
                        }
                        startIndex3 = endIndex3 + 1;
                        endIndex = endIndex3;
                        i3 = 0;
                        b = (byte) 10;
                    } catch (Throwable th4) {
                        th = th4;
                        startIndex = startIndex3;
                        throw th;
                    }
                }
                startIndex2 = startIndex3;
                try {
                    Iterator<Entry> itr = staleStats.values().iterator();
                    while (itr.hasNext()) {
                        if (((Entry) itr.next()).mVersion != sKernelWakelockUpdateVersion) {
                            itr.remove();
                        }
                    }
                    kernelWakelockStats.kernelWakelockVersion = sKernelWakelockUpdateVersion;
                    return kernelWakelockStats;
                } catch (Throwable th5) {
                    th = th5;
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                throw th;
            }
        }
    }
}
