package com.android.internal.net;

import android.net.NetworkStats;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ProcFileReader;
import com.android.server.NetworkManagementSocketTagger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ProtocolException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import libcore.io.IoUtils;

public class NetworkStatsFactory {
    private static final boolean SANITY_CHECK_NATIVE = false;
    private static final String TAG = "NetworkStatsFactory";
    private static final boolean USE_NATIVE_PARSING = true;
    private static final ConcurrentHashMap<String, String> sStackedIfaces = new ConcurrentHashMap();
    private final File mStatsXtIfaceAll;
    private final File mStatsXtIfaceFmt;
    private final File mStatsXtProcAndUid;
    private final File mStatsXtProcUid;
    private final File mStatsXtUid;
    private boolean mUseBpfStats;

    @VisibleForTesting
    public static native int nativeReadNetworkStatsDetail(NetworkStats networkStats, String str, int i, String[] strArr, int i2, boolean z);

    @VisibleForTesting
    public static native int nativeReadNetworkStatsDev(NetworkStats networkStats);

    public static void noteStackedIface(String stackedIface, String baseIface) {
        if (stackedIface != null && baseIface != null) {
            sStackedIfaces.put(stackedIface, baseIface);
        }
    }

    public static String[] augmentWithStackedInterfaces(String[] requiredIfaces) {
        if (requiredIfaces == NetworkStats.INTERFACES_ALL) {
            return null;
        }
        HashSet<String> relatedIfaces = new HashSet(Arrays.asList(requiredIfaces));
        for (Entry<String, String> entry : sStackedIfaces.entrySet()) {
            if (relatedIfaces.contains(entry.getKey())) {
                relatedIfaces.add((String) entry.getValue());
            } else if (relatedIfaces.contains(entry.getValue())) {
                relatedIfaces.add((String) entry.getKey());
            }
        }
        return (String[]) relatedIfaces.toArray(new String[relatedIfaces.size()]);
    }

    public static void apply464xlatAdjustments(NetworkStats baseTraffic, NetworkStats stackedTraffic) {
        NetworkStats.apply464xlatAdjustments(baseTraffic, stackedTraffic, sStackedIfaces);
    }

    @VisibleForTesting
    public static void clearStackedIfaces() {
        sStackedIfaces.clear();
    }

    public NetworkStatsFactory() {
        this(new File("/proc/"), new File("/sys/fs/bpf/traffic_uid_stats_map").exists());
    }

    @VisibleForTesting
    public NetworkStatsFactory(File procRoot, boolean useBpfStats) {
        this.mStatsXtIfaceAll = new File(procRoot, "net/xt_qtaguid/iface_stat_all");
        this.mStatsXtIfaceFmt = new File(procRoot, "net/xt_qtaguid/iface_stat_fmt");
        this.mStatsXtUid = new File(procRoot, "net/xt_qtaguid/stats");
        this.mUseBpfStats = useBpfStats;
        this.mStatsXtProcUid = new File(procRoot, "net/comm/stats");
        this.mStatsXtProcAndUid = new File(procRoot, "net/xt_qtaguid/stats_pid");
    }

    public NetworkStats readBpfNetworkStatsDev() throws IOException {
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        if (nativeReadNetworkStatsDev(stats) == 0) {
            return stats;
        }
        throw new IOException("Failed to parse bpf iface stats");
    }

    public NetworkStats readNetworkStatsSummaryDev() throws IOException {
        if (this.mUseBpfStats) {
            return readBpfNetworkStatsDev();
        }
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        try {
            ProcFileReader reader = new ProcFileReader(new FileInputStream(this.mStatsXtIfaceAll));
            while (reader.hasMoreData()) {
                entry.iface = reader.nextString();
                entry.uid = -1;
                entry.set = -1;
                boolean active = false;
                entry.tag = 0;
                if (reader.nextInt() != 0) {
                    active = true;
                }
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
            }
            IoUtils.closeQuietly(reader);
            StrictMode.setThreadPolicy(savedPolicy);
            return stats;
        } catch (NullPointerException | NumberFormatException e) {
            throw new ProtocolException("problem parsing stats", e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    public NetworkStats readNetworkStatsSummaryXt() throws IOException {
        if (this.mUseBpfStats) {
            return readBpfNetworkStatsDev();
        }
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        if (!this.mStatsXtIfaceFmt.exists()) {
            return null;
        }
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        try {
            ProcFileReader reader = new ProcFileReader(new FileInputStream(this.mStatsXtIfaceFmt));
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
        } catch (NullPointerException | NumberFormatException e) {
            throw new ProtocolException("problem parsing stats", e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    public NetworkStats readNetworkStatsDetail() throws IOException {
        return readNetworkStatsDetail(-1, null, -1, null);
    }

    public NetworkStats readNetworkStatsDetail(int limitUid, String[] limitIfaces, int limitTag, NetworkStats lastStats) throws IOException {
        NetworkStats stats = readNetworkStatsDetailInternal(limitUid, limitIfaces, limitTag, lastStats);
        stats.apply464xlatAdjustments(sStackedIfaces);
        return stats;
    }

    private NetworkStats readNetworkStatsDetailInternal(int limitUid, String[] limitIfaces, int limitTag, NetworkStats lastStats) throws IOException {
        NetworkStats stats;
        if (lastStats != null) {
            stats = lastStats;
            stats.setElapsedRealtime(SystemClock.elapsedRealtime());
        } else {
            stats = new NetworkStats(SystemClock.elapsedRealtime(), -1);
        }
        if (nativeReadNetworkStatsDetail(stats, this.mStatsXtUid.getAbsolutePath(), limitUid, limitIfaces, limitTag, this.mUseBpfStats) == 0) {
            return stats;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to parse network stats! stats: ");
        stringBuilder.append(stats.toString());
        stringBuilder.append("path: ");
        stringBuilder.append(this.mStatsXtUid.getAbsolutePath());
        Slog.w(TAG, stringBuilder.toString());
        throw new IOException("Failed to parse network stats");
    }

    @VisibleForTesting
    public static NetworkStats javaReadNetworkStatsDetail(File detailPath, int limitUid, String[] limitIfaces, int limitTag) throws IOException {
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 24);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        int lastIdx = 1;
        try {
            ProcFileReader reader = new ProcFileReader(new FileInputStream(detailPath));
            reader.finishLine();
            while (reader.hasMoreData()) {
                int idx = reader.nextInt();
                if (idx == lastIdx + 1) {
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
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("inconsistent idx=");
                    stringBuilder.append(idx);
                    stringBuilder.append(" after lastIdx=");
                    stringBuilder.append(lastIdx);
                    throw new ProtocolException(stringBuilder.toString());
                }
            }
            IoUtils.closeQuietly(reader);
            StrictMode.setThreadPolicy(savedPolicy);
            return stats;
        } catch (NullPointerException | NumberFormatException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("problem parsing idx ");
            stringBuilder2.append(1);
            throw new ProtocolException(stringBuilder2.toString(), e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    public NetworkStats readNetworkStatsProcDetail(String iface) {
        NetworkStats stats;
        NumberFormatException e;
        IOException ex;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        BufferedReader reader = null;
        if (this.mStatsXtProcUid.exists()) {
            stats = new NetworkStats(SystemClock.elapsedRealtime(), 24);
            NetworkStats.Entry entry = new NetworkStats.Entry();
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.mStatsXtProcUid), "UTF-8"));
                while (true) {
                    String readLine = reader.readLine();
                    String line = readLine;
                    if (readLine != null) {
                        String[] procArr = line.trim().split("\\s+");
                        if (procArr.length >= 4) {
                            StringBuffer procName = new StringBuffer();
                            int i = 0;
                            while (i < procArr.length - 3) {
                                procName.append(procArr[i]);
                                procName.append(" ");
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
                                } catch (NumberFormatException e2) {
                                    e = e2;
                                }
                            } catch (NumberFormatException e3) {
                                NumberFormatException numberFormatException = e3;
                                i = i2;
                                e = numberFormatException;
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("problem parsing line:");
                                stringBuilder.append(line);
                                Slog.e(str, stringBuilder.toString(), e);
                            }
                        }
                    } else {
                        try {
                            break;
                        } catch (IOException e4) {
                            ex = e4;
                        }
                    }
                }
                reader.close();
            } catch (RuntimeException e5) {
                Slog.e(TAG, "problem parsing proc stats", e5);
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e6) {
                        ex = e6;
                    }
                }
            } catch (IOException ex2) {
                Slog.e(TAG, "problem parsing proc stats", ex2);
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e7) {
                        ex2 = e7;
                    }
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex3) {
                        Slog.e(TAG, "close reader exception", ex3);
                    }
                }
                StrictMode.setThreadPolicy(savedPolicy);
            }
            StrictMode.setThreadPolicy(savedPolicy);
            return stats;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mStatsXtProcUid: ");
        stringBuilder2.append(this.mStatsXtProcUid.getAbsolutePath());
        stringBuilder2.append(" does not exist!");
        Slog.w(str2, stringBuilder2.toString());
        return null;
        Slog.e(TAG, "close reader exception", ex2);
        StrictMode.setThreadPolicy(savedPolicy);
        return stats;
    }

    public NetworkStats javaReadNetworkStatsUidAndProcDetail(int limitUid, String[] limitIfaces, int limitTag) throws IOException {
        int i = limitUid;
        Object[] objArr = limitIfaces;
        int i2 = limitTag;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        String str;
        if (this.mStatsXtProcAndUid.exists()) {
            long begin = SystemClock.elapsedRealtime();
            NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 24);
            NetworkStats.Entry entry = new NetworkStats.Entry();
            int lastIdx = 1;
            try {
                ProcFileReader reader = new ProcFileReader(new FileInputStream(this.mStatsXtProcAndUid));
                reader.finishLine();
                while (reader.hasMoreData()) {
                    int idx = reader.nextInt();
                    if (idx == lastIdx + 1) {
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
                        if ((objArr == null || ArrayUtils.contains(objArr, entry.iface)) && ((i == -1 || i == entry.uid) && (i2 == -1 || i2 == entry.tag))) {
                            stats.addValues(entry);
                        }
                        reader.finishLine();
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("inconsistent idx=");
                        stringBuilder.append(idx);
                        stringBuilder.append(" after lastIdx=");
                        stringBuilder.append(lastIdx);
                        throw new ProtocolException(stringBuilder.toString());
                    }
                }
                IoUtils.closeQuietly(reader);
                StrictMode.setThreadPolicy(savedPolicy);
                long end = SystemClock.elapsedRealtime();
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("javaReadNetworkStatsUidAndProcDetail. cost time = ");
                stringBuilder2.append(end - begin);
                Slog.i(str, stringBuilder2.toString());
                return stats;
            } catch (NullPointerException e) {
                throw new ProtocolException("problem parsing xt_qprocuid stats", e);
            } catch (NumberFormatException e2) {
                throw new ProtocolException("problem parsing xt_qprocuid stats", e2);
            } catch (Throwable th) {
                IoUtils.closeQuietly(null);
                StrictMode.setThreadPolicy(savedPolicy);
            }
        } else {
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("mStatsXtProcAndUid: ");
            stringBuilder3.append(this.mStatsXtProcAndUid.getAbsolutePath());
            stringBuilder3.append(" does not exist!");
            Slog.w(str, stringBuilder3.toString());
            return null;
        }
    }

    public void assertEquals(NetworkStats expected, NetworkStats actual) {
        if (expected.size() == actual.size()) {
            NetworkStats.Entry expectedRow = null;
            NetworkStats.Entry actualRow = null;
            int i = 0;
            while (i < expected.size()) {
                expectedRow = expected.getValues(i, expectedRow);
                actualRow = actual.getValues(i, actualRow);
                if (expectedRow.equals(actualRow)) {
                    i++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected row ");
                    stringBuilder.append(i);
                    stringBuilder.append(": ");
                    stringBuilder.append(expectedRow);
                    stringBuilder.append(", actual row ");
                    stringBuilder.append(actualRow);
                    throw new AssertionError(stringBuilder.toString());
                }
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Expected size ");
        stringBuilder2.append(expected.size());
        stringBuilder2.append(", actual size ");
        stringBuilder2.append(actual.size());
        throw new AssertionError(stringBuilder2.toString());
    }
}
