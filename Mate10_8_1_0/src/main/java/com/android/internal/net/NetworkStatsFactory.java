package com.android.internal.net;

import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ProcFileReader;
import com.android.server.NetworkManagementSocketTagger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ProtocolException;
import libcore.io.IoUtils;

public class NetworkStatsFactory {
    private static final String CLATD_INTERFACE_PREFIX = "v4-";
    private static final int IPV4V6_HEADER_DELTA = 20;
    private static final boolean SANITY_CHECK_NATIVE = false;
    private static final String TAG = "NetworkStatsFactory";
    private static final boolean USE_NATIVE_PARSING = true;
    @GuardedBy("sStackedIfaces")
    private static final ArrayMap<String, String> sStackedIfaces = new ArrayMap();
    private final File mStatsXtIfaceAll;
    private final File mStatsXtIfaceFmt;
    private final File mStatsXtProcAndUid;
    private final File mStatsXtProcUid;
    private final File mStatsXtUid;

    public static native int nativeReadNetworkStatsDetail(NetworkStats networkStats, String str, int i, String[] strArr, int i2);

    public static void noteStackedIface(String stackedIface, String baseIface) {
        synchronized (sStackedIfaces) {
            if (baseIface != null) {
                sStackedIfaces.put(stackedIface, baseIface);
            } else {
                sStackedIfaces.remove(stackedIface);
            }
        }
    }

    public NetworkStatsFactory() {
        this(new File("/proc/"));
    }

    public NetworkStatsFactory(File procRoot) {
        this.mStatsXtIfaceAll = new File(procRoot, "net/xt_qtaguid/iface_stat_all");
        this.mStatsXtIfaceFmt = new File(procRoot, "net/xt_qtaguid/iface_stat_fmt");
        this.mStatsXtUid = new File(procRoot, "net/xt_qtaguid/stats");
        this.mStatsXtProcUid = new File(procRoot, "net/comm/stats");
        this.mStatsXtProcAndUid = new File(procRoot, "net/xt_qtaguid/stats_pid");
    }

    public NetworkStats readNetworkStatsSummaryDev() throws IOException {
        RuntimeException e;
        Throwable th;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        Entry entry = new Entry();
        AutoCloseable autoCloseable = null;
        try {
            ProcFileReader reader = new ProcFileReader(new FileInputStream(this.mStatsXtIfaceAll));
            while (reader.hasMoreData()) {
                try {
                    entry.iface = reader.nextString();
                    entry.uid = -1;
                    entry.set = -1;
                    entry.tag = 0;
                    boolean active = reader.nextInt() != 0;
                    entry.rxBytes = reader.nextLong();
                    entry.rxPackets = reader.nextLong();
                    entry.txBytes = reader.nextLong();
                    entry.txPackets = reader.nextLong();
                    if (active) {
                        entry.rxBytes += reader.nextLong();
                        entry.rxPackets += reader.nextLong();
                        entry.txBytes += reader.nextLong();
                        entry.txPackets += reader.nextLong();
                    }
                    stats.addValues(entry);
                    reader.finishLine();
                } catch (NullPointerException e2) {
                    e = e2;
                    autoCloseable = reader;
                } catch (Throwable th2) {
                    th = th2;
                    Object reader2 = reader;
                }
            }
            IoUtils.closeQuietly(reader);
            StrictMode.setThreadPolicy(savedPolicy);
            return stats;
        } catch (NullPointerException e3) {
            e = e3;
            try {
                throw new ProtocolException("problem parsing stats", e);
            } catch (Throwable th3) {
                th = th3;
                IoUtils.closeQuietly(autoCloseable);
                StrictMode.setThreadPolicy(savedPolicy);
                throw th;
            }
        }
    }

    public NetworkStats readNetworkStatsSummaryXt() throws IOException {
        RuntimeException e;
        Throwable th;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        if (!this.mStatsXtIfaceFmt.exists()) {
            return null;
        }
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        Entry entry = new Entry();
        AutoCloseable autoCloseable = null;
        try {
            ProcFileReader reader = new ProcFileReader(new FileInputStream(this.mStatsXtIfaceFmt));
            try {
                reader.finishLine();
                while (reader.hasMoreData()) {
                    entry.iface = reader.nextString();
                    entry.uid = -1;
                    entry.set = -1;
                    entry.tag = 0;
                    entry.rxBytes = reader.nextLong();
                    entry.rxPackets = reader.nextLong();
                    entry.txBytes = reader.nextLong();
                    entry.txPackets = reader.nextLong();
                    stats.addValues(entry);
                    reader.finishLine();
                }
                IoUtils.closeQuietly(reader);
                StrictMode.setThreadPolicy(savedPolicy);
                return stats;
            } catch (NullPointerException e2) {
                e = e2;
                autoCloseable = reader;
                try {
                    throw new ProtocolException("problem parsing stats", e);
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(autoCloseable);
                    StrictMode.setThreadPolicy(savedPolicy);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                Object reader2 = reader;
                IoUtils.closeQuietly(autoCloseable);
                StrictMode.setThreadPolicy(savedPolicy);
                throw th;
            }
        } catch (NullPointerException e3) {
            e = e3;
            throw new ProtocolException("problem parsing stats", e);
        }
    }

    public NetworkStats readNetworkStatsDetail() throws IOException {
        return readNetworkStatsDetail(-1, null, -1, null);
    }

    public NetworkStats readNetworkStatsDetail(int limitUid, String[] limitIfaces, int limitTag, NetworkStats lastStats) throws IOException {
        ArrayMap<String, String> arrayMap;
        NetworkStats stats = readNetworkStatsDetailInternal(limitUid, limitIfaces, limitTag, lastStats);
        synchronized (sStackedIfaces) {
            arrayMap = new ArrayMap(sStackedIfaces);
        }
        NetworkStats adjustments = new NetworkStats(0, arrayMap.size());
        Entry entry = null;
        for (int i = 0; i < stats.size(); i++) {
            entry = stats.getValues(i, entry);
            if (entry.iface != null && (entry.iface.startsWith(CLATD_INTERFACE_PREFIX) ^ 1) == 0) {
                String baseIface = (String) arrayMap.get(entry.iface);
                if (baseIface != null) {
                    Entry adjust = new Entry(baseIface, 0, 0, 0, 0, 0, 0, 0, 0);
                    adjust.rxBytes -= entry.rxBytes + (entry.rxPackets * 20);
                    adjust.txBytes -= entry.txBytes + (entry.txPackets * 20);
                    adjust.rxPackets -= entry.rxPackets;
                    adjust.txPackets -= entry.txPackets;
                    adjustments.combineValues(adjust);
                    entry.rxBytes = entry.rxPackets * 20;
                    entry.txBytes = entry.txPackets * 20;
                    entry.rxPackets = 0;
                    entry.txPackets = 0;
                    stats.combineValues(entry);
                }
            }
        }
        stats.combineAllValues(adjustments);
        return stats;
    }

    private NetworkStats readNetworkStatsDetailInternal(int limitUid, String[] limitIfaces, int limitTag, NetworkStats lastStats) throws IOException {
        NetworkStats stats;
        if (lastStats != null) {
            stats = lastStats;
            lastStats.setElapsedRealtime(SystemClock.elapsedRealtime());
        } else {
            stats = new NetworkStats(SystemClock.elapsedRealtime(), -1);
        }
        if (nativeReadNetworkStatsDetail(stats, this.mStatsXtUid.getAbsolutePath(), limitUid, limitIfaces, limitTag) == 0) {
            return stats;
        }
        Slog.w(TAG, "Failed to parse network stats! stats: " + stats.toString() + "path: " + this.mStatsXtUid.getAbsolutePath());
        throw new IOException("Failed to parse network stats");
    }

    public static NetworkStats javaReadNetworkStatsDetail(File detailPath, int limitUid, String[] limitIfaces, int limitTag) throws IOException {
        RuntimeException e;
        Throwable th;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 24);
        Entry entry = new Entry();
        int idx = 1;
        int lastIdx = 1;
        AutoCloseable autoCloseable = null;
        try {
            ProcFileReader reader = new ProcFileReader(new FileInputStream(detailPath));
            try {
                reader.finishLine();
                while (reader.hasMoreData()) {
                    idx = reader.nextInt();
                    if (idx != lastIdx + 1) {
                        throw new ProtocolException("inconsistent idx=" + idx + " after lastIdx=" + lastIdx);
                    }
                    lastIdx = idx;
                    entry.iface = reader.nextString();
                    entry.tag = NetworkManagementSocketTagger.kernelToTag(reader.nextString());
                    entry.uid = reader.nextInt();
                    entry.set = reader.nextInt();
                    entry.rxBytes = reader.nextLong();
                    entry.rxPackets = reader.nextLong();
                    entry.txBytes = reader.nextLong();
                    entry.txPackets = reader.nextLong();
                    if ((limitIfaces == null || ArrayUtils.contains((Object[]) limitIfaces, entry.iface)) && ((limitUid == -1 || limitUid == entry.uid) && (limitTag == -1 || limitTag == entry.tag))) {
                        stats.addValues(entry);
                    }
                    reader.finishLine();
                }
                IoUtils.closeQuietly(reader);
                StrictMode.setThreadPolicy(savedPolicy);
                return stats;
            } catch (NullPointerException e2) {
                e = e2;
                autoCloseable = reader;
            } catch (Throwable th2) {
                th = th2;
                Object reader2 = reader;
            }
        } catch (NullPointerException e3) {
            e = e3;
            try {
                throw new ProtocolException("problem parsing idx " + idx, e);
            } catch (Throwable th3) {
                th = th3;
                IoUtils.closeQuietly(autoCloseable);
                StrictMode.setThreadPolicy(savedPolicy);
                throw th;
            }
        }
    }

    public NetworkStats readNetworkStatsProcDetail(String iface) {
        NumberFormatException e;
        RuntimeException e2;
        IOException e3;
        Throwable th;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        if (this.mStatsXtProcUid.exists()) {
            NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 24);
            Entry entry = new Entry();
            BufferedReader bufferedReader = null;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.mStatsXtProcUid), "UTF-8"));
                while (true) {
                    try {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        String[] procArr = line.trim().split("\\s+");
                        if (procArr.length >= 4) {
                            StringBuffer procName = new StringBuffer();
                            int i = 0;
                            while (i < procArr.length - 3) {
                                procName.append(procArr[i]).append(" ");
                                i++;
                            }
                            entry.iface = iface;
                            entry.proc = procName.toString().trim();
                            entry.set = -1;
                            entry.tag = 0;
                            entry.rxPackets = 0;
                            entry.txPackets = 0;
                            int i2 = i + 1;
                            try {
                                entry.uid = Integer.parseInt(procArr[i]);
                                i = i2 + 1;
                                try {
                                    entry.rxBytes = Long.parseLong(procArr[i2]);
                                    entry.txBytes = Long.parseLong(procArr[i]);
                                    stats.addValues(entry);
                                } catch (NumberFormatException e4) {
                                    e = e4;
                                    i2 = i;
                                    Slog.e(TAG, "problem parsing line:" + line, e);
                                }
                            } catch (NumberFormatException e5) {
                                e = e5;
                                Slog.e(TAG, "problem parsing line:" + line, e);
                            }
                        }
                    } catch (RuntimeException e6) {
                        e2 = e6;
                        bufferedReader = reader;
                    } catch (IOException e7) {
                        e3 = e7;
                        bufferedReader = reader;
                    } catch (Throwable th2) {
                        th = th2;
                        bufferedReader = reader;
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                        Slog.e(TAG, "close reader exception", ex);
                    }
                }
                StrictMode.setThreadPolicy(savedPolicy);
            } catch (RuntimeException e8) {
                e2 = e8;
                try {
                    Slog.e(TAG, "problem parsing proc stats", e2);
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException ex2) {
                            Slog.e(TAG, "close reader exception", ex2);
                        }
                    }
                    StrictMode.setThreadPolicy(savedPolicy);
                    return stats;
                } catch (Throwable th3) {
                    th = th3;
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException ex22) {
                            Slog.e(TAG, "close reader exception", ex22);
                        }
                    }
                    StrictMode.setThreadPolicy(savedPolicy);
                    throw th;
                }
            } catch (IOException e9) {
                e3 = e9;
                Slog.e(TAG, "problem parsing proc stats", e3);
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException ex222) {
                        Slog.e(TAG, "close reader exception", ex222);
                    }
                }
                StrictMode.setThreadPolicy(savedPolicy);
                return stats;
            }
            return stats;
        }
        Slog.w(TAG, "mStatsXtProcUid: " + this.mStatsXtProcUid.getAbsolutePath() + " does not exist!");
        return null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public NetworkStats javaReadNetworkStatsUidAndProcDetail(int limitUid, String[] limitIfaces, int limitTag) throws IOException {
        NullPointerException e;
        NumberFormatException e2;
        Throwable th;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        if (this.mStatsXtProcAndUid.exists()) {
            long begin = SystemClock.elapsedRealtime();
            NetworkStats networkStats = new NetworkStats(SystemClock.elapsedRealtime(), 24);
            Entry entry = new Entry();
            int lastIdx = 1;
            AutoCloseable autoCloseable = null;
            try {
                ProcFileReader reader = new ProcFileReader(new FileInputStream(this.mStatsXtProcAndUid));
                try {
                    reader.finishLine();
                    while (reader.hasMoreData()) {
                        int idx = reader.nextInt();
                        if (idx != lastIdx + 1) {
                            throw new ProtocolException("inconsistent idx=" + idx + " after lastIdx=" + lastIdx);
                        }
                        lastIdx = idx;
                        entry.iface = reader.nextString();
                        entry.proc = reader.nextString();
                        entry.actUid = reader.nextInt();
                        entry.tag = NetworkManagementSocketTagger.kernelToTag(reader.nextString());
                        entry.uid = reader.nextInt();
                        entry.set = reader.nextInt();
                        entry.rxBytes = reader.nextLong();
                        entry.rxPackets = reader.nextLong();
                        entry.txBytes = reader.nextLong();
                        entry.txPackets = reader.nextLong();
                        if (limitIfaces != null) {
                        }
                        if ((limitUid == -1 || limitUid == entry.uid) && (limitTag == -1 || limitTag == entry.tag)) {
                            networkStats.addValues(entry);
                        }
                        reader.finishLine();
                    }
                    IoUtils.closeQuietly(reader);
                    StrictMode.setThreadPolicy(savedPolicy);
                    Slog.i(TAG, "javaReadNetworkStatsUidAndProcDetail. cost time = " + (SystemClock.elapsedRealtime() - begin));
                    return networkStats;
                } catch (NullPointerException e3) {
                    e = e3;
                    autoCloseable = reader;
                } catch (NumberFormatException e4) {
                    e2 = e4;
                    autoCloseable = reader;
                } catch (Throwable th2) {
                    th = th2;
                    Object reader2 = reader;
                }
            } catch (NullPointerException e5) {
                e = e5;
                try {
                    throw new ProtocolException("problem parsing xt_qprocuid stats", e);
                } catch (Throwable th3) {
                    th = th3;
                    IoUtils.closeQuietly(autoCloseable);
                    StrictMode.setThreadPolicy(savedPolicy);
                    throw th;
                }
            } catch (NumberFormatException e6) {
                e2 = e6;
                throw new ProtocolException("problem parsing xt_qprocuid stats", e2);
            }
        }
        Slog.w(TAG, "mStatsXtProcAndUid: " + this.mStatsXtProcAndUid.getAbsolutePath() + " does not exist!");
        return null;
    }

    public void assertEquals(NetworkStats expected, NetworkStats actual) {
        if (expected.size() != actual.size()) {
            throw new AssertionError("Expected size " + expected.size() + ", actual size " + actual.size());
        }
        Entry expectedRow = null;
        Entry actualRow = null;
        int i = 0;
        while (i < expected.size()) {
            expectedRow = expected.getValues(i, expectedRow);
            actualRow = actual.getValues(i, actualRow);
            if (expectedRow.equals(actualRow)) {
                i++;
            } else {
                throw new AssertionError("Expected row " + i + ": " + expectedRow + ", actual row " + actualRow);
            }
        }
    }
}
