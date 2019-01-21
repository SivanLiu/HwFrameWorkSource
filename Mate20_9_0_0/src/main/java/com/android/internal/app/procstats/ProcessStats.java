package com.android.internal.app.procstats;

import android.os.Debug;
import android.os.Debug.MemoryInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.ProcessMap;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.telephony.PhoneConstants;
import com.android.server.job.JobStatusDumpProto;
import dalvik.system.VMRuntime;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProcessStats implements Parcelable {
    public static final int ADD_PSS_EXTERNAL = 3;
    public static final int ADD_PSS_EXTERNAL_SLOW = 4;
    public static final int ADD_PSS_INTERNAL_ALL_MEM = 1;
    public static final int ADD_PSS_INTERNAL_ALL_POLL = 2;
    public static final int ADD_PSS_INTERNAL_SINGLE = 0;
    public static final int ADJ_COUNT = 8;
    public static final int ADJ_MEM_FACTOR_COUNT = 4;
    public static final int ADJ_MEM_FACTOR_CRITICAL = 3;
    public static final int ADJ_MEM_FACTOR_LOW = 2;
    public static final int ADJ_MEM_FACTOR_MODERATE = 1;
    public static final int ADJ_MEM_FACTOR_NORMAL = 0;
    public static final int ADJ_NOTHING = -1;
    public static final int ADJ_SCREEN_MOD = 4;
    public static final int ADJ_SCREEN_OFF = 0;
    public static final int ADJ_SCREEN_ON = 4;
    public static final int[] ALL_MEM_ADJ = new int[]{0, 1, 2, 3};
    public static final int[] ALL_PROC_STATES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
    public static final int[] ALL_SCREEN_ADJ = new int[]{0, 4};
    public static final int[] BACKGROUND_PROC_STATES = new int[]{2, 3, 4, 8, 5, 6, 7};
    static final int[] BAD_TABLE = new int[0];
    public static long COMMIT_PERIOD = 10800000;
    public static long COMMIT_UPTIME_PERIOD = 3600000;
    public static final Creator<ProcessStats> CREATOR = new Creator<ProcessStats>() {
        public ProcessStats createFromParcel(Parcel in) {
            return new ProcessStats(in);
        }

        public ProcessStats[] newArray(int size) {
            return new ProcessStats[size];
        }
    };
    static final boolean DEBUG = false;
    static final boolean DEBUG_PARCEL = false;
    public static final int FLAG_COMPLETE = 1;
    public static final int FLAG_SHUTDOWN = 2;
    public static final int FLAG_SYSPROPS = 4;
    private static final int MAGIC = 1347638356;
    public static final int[] NON_CACHED_PROC_STATES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int PARCEL_VERSION = 27;
    public static final int PSS_AVERAGE = 2;
    public static final int PSS_COUNT = 10;
    public static final int PSS_MAXIMUM = 3;
    public static final int PSS_MINIMUM = 1;
    public static final int PSS_RSS_AVERAGE = 8;
    public static final int PSS_RSS_MAXIMUM = 9;
    public static final int PSS_RSS_MINIMUM = 7;
    public static final int PSS_SAMPLE_COUNT = 0;
    public static final int PSS_USS_AVERAGE = 5;
    public static final int PSS_USS_MAXIMUM = 6;
    public static final int PSS_USS_MINIMUM = 4;
    public static final String SERVICE_NAME = "procstats";
    public static final int STATE_BACKUP = 4;
    public static final int STATE_CACHED_ACTIVITY = 11;
    public static final int STATE_CACHED_ACTIVITY_CLIENT = 12;
    public static final int STATE_CACHED_EMPTY = 13;
    public static final int STATE_COUNT = 14;
    public static final int STATE_HEAVY_WEIGHT = 8;
    public static final int STATE_HOME = 9;
    public static final int STATE_IMPORTANT_BACKGROUND = 3;
    public static final int STATE_IMPORTANT_FOREGROUND = 2;
    public static final int STATE_LAST_ACTIVITY = 10;
    public static final int STATE_NOTHING = -1;
    public static final int STATE_PERSISTENT = 0;
    public static final int STATE_RECEIVER = 7;
    public static final int STATE_SERVICE = 5;
    public static final int STATE_SERVICE_RESTARTING = 6;
    public static final int STATE_TOP = 1;
    public static final int SYS_MEM_USAGE_CACHED_AVERAGE = 2;
    public static final int SYS_MEM_USAGE_CACHED_MAXIMUM = 3;
    public static final int SYS_MEM_USAGE_CACHED_MINIMUM = 1;
    public static final int SYS_MEM_USAGE_COUNT = 16;
    public static final int SYS_MEM_USAGE_FREE_AVERAGE = 5;
    public static final int SYS_MEM_USAGE_FREE_MAXIMUM = 6;
    public static final int SYS_MEM_USAGE_FREE_MINIMUM = 4;
    public static final int SYS_MEM_USAGE_KERNEL_AVERAGE = 11;
    public static final int SYS_MEM_USAGE_KERNEL_MAXIMUM = 12;
    public static final int SYS_MEM_USAGE_KERNEL_MINIMUM = 10;
    public static final int SYS_MEM_USAGE_NATIVE_AVERAGE = 14;
    public static final int SYS_MEM_USAGE_NATIVE_MAXIMUM = 15;
    public static final int SYS_MEM_USAGE_NATIVE_MINIMUM = 13;
    public static final int SYS_MEM_USAGE_SAMPLE_COUNT = 0;
    public static final int SYS_MEM_USAGE_ZRAM_AVERAGE = 8;
    public static final int SYS_MEM_USAGE_ZRAM_MAXIMUM = 9;
    public static final int SYS_MEM_USAGE_ZRAM_MINIMUM = 7;
    public static final String TAG = "ProcessStats";
    private static final Pattern sPageTypeRegex = Pattern.compile("^Node\\s+(\\d+),.*. type\\s+(\\w+)\\s+([\\s\\d]+?)\\s*$");
    ArrayMap<String, Integer> mCommonStringToIndex;
    public long mExternalPssCount;
    public long mExternalPssTime;
    public long mExternalSlowPssCount;
    public long mExternalSlowPssTime;
    public int mFlags;
    boolean mHasSwappedOutPss;
    ArrayList<String> mIndexToCommonString;
    public long mInternalAllMemPssCount;
    public long mInternalAllMemPssTime;
    public long mInternalAllPollPssCount;
    public long mInternalAllPollPssTime;
    public long mInternalSinglePssCount;
    public long mInternalSinglePssTime;
    public int mMemFactor;
    public final long[] mMemFactorDurations;
    public final ProcessMap<LongSparseArray<PackageState>> mPackages;
    private final ArrayList<String> mPageTypeLabels;
    private final ArrayList<int[]> mPageTypeSizes;
    private final ArrayList<Integer> mPageTypeZones;
    public final ProcessMap<ProcessState> mProcesses;
    public String mReadError;
    boolean mRunning;
    String mRuntime;
    public long mStartTime;
    public final SysMemUsageTable mSysMemUsage;
    public final long[] mSysMemUsageArgs;
    public final SparseMappingTable mTableData;
    public long mTimePeriodEndRealtime;
    public long mTimePeriodEndUptime;
    public long mTimePeriodStartClock;
    public String mTimePeriodStartClockStr;
    public long mTimePeriodStartRealtime;
    public long mTimePeriodStartUptime;

    public static final class PackageState {
        public final String mPackageName;
        public final ArrayMap<String, ProcessState> mProcesses = new ArrayMap();
        public final ArrayMap<String, ServiceState> mServices = new ArrayMap();
        public final int mUid;

        public PackageState(String packageName, int uid) {
            this.mUid = uid;
            this.mPackageName = packageName;
        }
    }

    public static final class ProcessDataCollection {
        public long avgPss;
        public long avgRss;
        public long avgUss;
        public long maxPss;
        public long maxRss;
        public long maxUss;
        final int[] memStates;
        public long minPss;
        public long minRss;
        public long minUss;
        public long numPss;
        final int[] procStates;
        final int[] screenStates;
        public long totalTime;

        public ProcessDataCollection(int[] _screenStates, int[] _memStates, int[] _procStates) {
            this.screenStates = _screenStates;
            this.memStates = _memStates;
            this.procStates = _procStates;
        }

        void print(PrintWriter pw, long overallTime, boolean full) {
            if (this.totalTime > overallTime) {
                pw.print(PhoneConstants.APN_TYPE_ALL);
            }
            DumpUtils.printPercent(pw, ((double) this.totalTime) / ((double) overallTime));
            if (this.numPss > 0) {
                pw.print(" (");
                DebugUtils.printSizeValue(pw, this.minPss * 1024);
                pw.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                DebugUtils.printSizeValue(pw, this.avgPss * 1024);
                pw.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                DebugUtils.printSizeValue(pw, this.maxPss * 1024);
                pw.print("/");
                DebugUtils.printSizeValue(pw, this.minUss * 1024);
                pw.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                DebugUtils.printSizeValue(pw, this.avgUss * 1024);
                pw.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                DebugUtils.printSizeValue(pw, this.maxUss * 1024);
                pw.print("/");
                DebugUtils.printSizeValue(pw, this.minRss * 1024);
                pw.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                DebugUtils.printSizeValue(pw, this.avgRss * 1024);
                pw.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                DebugUtils.printSizeValue(pw, this.maxRss * 1024);
                if (full) {
                    pw.print(" over ");
                    pw.print(this.numPss);
                }
                pw.print(")");
            }
        }
    }

    public static final class ProcessStateHolder {
        public final long appVersion;
        public ProcessState state;

        public ProcessStateHolder(long _appVersion) {
            this.appVersion = _appVersion;
        }
    }

    public static class TotalMemoryUseCollection {
        public boolean hasSwappedOutPss;
        final int[] memStates;
        public long[] processStatePss = new long[14];
        public int[] processStateSamples = new int[14];
        public long[] processStateTime = new long[14];
        public double[] processStateWeight = new double[14];
        final int[] screenStates;
        public double sysMemCachedWeight;
        public double sysMemFreeWeight;
        public double sysMemKernelWeight;
        public double sysMemNativeWeight;
        public int sysMemSamples;
        public long[] sysMemUsage = new long[16];
        public double sysMemZRamWeight;
        public long totalTime;

        public TotalMemoryUseCollection(int[] _screenStates, int[] _memStates) {
            this.screenStates = _screenStates;
            this.memStates = _memStates;
        }
    }

    public ProcessStats(boolean running) {
        this.mPackages = new ProcessMap();
        this.mProcesses = new ProcessMap();
        this.mMemFactorDurations = new long[8];
        this.mMemFactor = -1;
        this.mTableData = new SparseMappingTable();
        this.mSysMemUsageArgs = new long[16];
        this.mSysMemUsage = new SysMemUsageTable(this.mTableData);
        this.mPageTypeZones = new ArrayList();
        this.mPageTypeLabels = new ArrayList();
        this.mPageTypeSizes = new ArrayList();
        this.mRunning = running;
        reset();
        if (running) {
            MemoryInfo info = new MemoryInfo();
            Debug.getMemoryInfo(Process.myPid(), info);
            this.mHasSwappedOutPss = info.hasSwappedOutPss();
        }
    }

    public ProcessStats(Parcel in) {
        this.mPackages = new ProcessMap();
        this.mProcesses = new ProcessMap();
        this.mMemFactorDurations = new long[8];
        this.mMemFactor = -1;
        this.mTableData = new SparseMappingTable();
        this.mSysMemUsageArgs = new long[16];
        this.mSysMemUsage = new SysMemUsageTable(this.mTableData);
        this.mPageTypeZones = new ArrayList();
        this.mPageTypeLabels = new ArrayList();
        this.mPageTypeSizes = new ArrayList();
        reset();
        readFromParcel(in);
    }

    public void add(ProcessStats other) {
        int iproc;
        ProcessState otherProc;
        ProcessStats processStats = other;
        ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap = processStats.mPackages.getMap();
        int ip = 0;
        while (true) {
            int ip2 = ip;
            if (ip2 >= pkgMap.size()) {
                break;
            }
            String pkgName = (String) pkgMap.keyAt(ip2);
            SparseArray<LongSparseArray<PackageState>> uids = (SparseArray) pkgMap.valueAt(ip2);
            ip = 0;
            while (true) {
                int iu = ip;
                if (iu >= uids.size()) {
                    break;
                }
                SparseArray<LongSparseArray<PackageState>> uids2;
                int uid = uids.keyAt(iu);
                LongSparseArray<PackageState> versions = (LongSparseArray) uids.valueAt(iu);
                ip = 0;
                while (true) {
                    int iv = ip;
                    if (iv >= versions.size()) {
                        break;
                    }
                    int NSRVS;
                    LongSparseArray<PackageState> versions2;
                    ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap2;
                    PackageState otherState;
                    long vers;
                    long vers2 = versions.keyAt(iv);
                    PackageState otherState2 = (PackageState) versions.valueAt(iv);
                    int NPROCS = otherState2.mProcesses.size();
                    ip = otherState2.mServices.size();
                    int iproc2 = 0;
                    while (true) {
                        iproc = iproc2;
                        if (iproc >= NPROCS) {
                            break;
                        }
                        int NPROCS2;
                        NSRVS = ip;
                        otherProc = (ProcessState) otherState2.mProcesses.valueAt(iproc);
                        int NPROCS3 = NPROCS;
                        if (otherProc.getCommonProcess() != otherProc) {
                            versions2 = versions;
                            ProcessState versions3 = otherProc;
                            pkgMap2 = pkgMap;
                            NPROCS2 = NPROCS3;
                            pkgMap = NSRVS;
                            uids2 = uids;
                            otherState = otherState2;
                            vers = vers2;
                            NSRVS = iv;
                            otherProc = getProcessStateLocked(pkgName, uid, vers2, otherProc.getName());
                            if (otherProc.getCommonProcess() == otherProc) {
                                otherProc.setMultiPackage(1);
                                NPROCS = SystemClock.uptimeMillis();
                                vers2 = vers;
                                iv = getPackageStateLocked(pkgName, uid, vers2);
                                otherProc = otherProc.clone(NPROCS);
                                long now = NPROCS;
                                iv.mProcesses.put(otherProc.getName(), otherProc);
                            } else {
                                vers2 = vers;
                            }
                            otherProc.add(versions3);
                        } else {
                            versions2 = versions;
                            pkgMap2 = pkgMap;
                            uids2 = uids;
                            pkgMap = NSRVS;
                            NPROCS2 = NPROCS3;
                            otherState = otherState2;
                            NSRVS = iv;
                        }
                        iproc2 = iproc + 1;
                        ip = pkgMap;
                        otherState2 = otherState;
                        NPROCS = NPROCS2;
                        iv = NSRVS;
                        versions = versions2;
                        pkgMap = pkgMap2;
                        uids = uids2;
                    }
                    NSRVS = iv;
                    versions2 = versions;
                    pkgMap2 = pkgMap;
                    uids2 = uids;
                    pkgMap = ip;
                    otherState = otherState2;
                    ip = 0;
                    while (true) {
                        iproc = ip;
                        if (iproc >= pkgMap) {
                            break;
                        }
                        ServiceState otherSvc = (ServiceState) otherState.mServices.valueAt(iproc);
                        vers = vers2;
                        int NSRVS2 = pkgMap;
                        ServiceState pkgMap3 = otherSvc;
                        getServiceStateLocked(pkgName, uid, vers2, otherSvc.getProcessName(), otherSvc.getName()).add(pkgMap3);
                        ip = iproc + 1;
                        pkgMap = NSRVS2;
                    }
                    ip = NSRVS + 1;
                    versions = versions2;
                    pkgMap = pkgMap2;
                    uids = uids2;
                }
                uids2 = uids;
                ip = iu + 1;
            }
            ip = ip2 + 1;
        }
        ArrayMap<String, SparseArray<ProcessState>> procMap = processStats.mProcesses.getMap();
        ip = 0;
        while (true) {
            iproc = ip;
            if (iproc >= procMap.size()) {
                break;
            }
            SparseArray<ProcessState> uids3 = (SparseArray) procMap.valueAt(iproc);
            ip = 0;
            while (true) {
                int iu2 = ip;
                if (iu2 >= uids3.size()) {
                    break;
                }
                ArrayMap<String, SparseArray<ProcessState>> procMap2;
                int uid2 = uids3.keyAt(iu2);
                ProcessState otherProc2 = (ProcessState) uids3.valueAt(iu2);
                String name = otherProc2.getName();
                String pkg = otherProc2.getPackage();
                long vers3 = otherProc2.getVersion();
                ProcessState thisProc = (ProcessState) this.mProcesses.get(name, uid2);
                String procMap3;
                if (thisProc == null) {
                    long vers4 = vers3;
                    procMap2 = procMap;
                    procMap3 = pkg;
                    otherProc = new ProcessState(this, pkg, uid2, vers3, name);
                    this.mProcesses.put(name, uid2, otherProc);
                    PackageState thisState = getPackageStateLocked(procMap3, uid2, vers4);
                    if (!thisState.mProcesses.containsKey(name)) {
                        thisState.mProcesses.put(name, otherProc);
                    }
                } else {
                    procMap2 = procMap;
                    procMap3 = pkg;
                    otherProc = thisProc;
                }
                otherProc.add(otherProc2);
                ip = iu2 + 1;
                procMap = procMap2;
            }
            ip = iproc + 1;
        }
        int i = 0;
        while (true) {
            ip = i;
            if (ip >= 8) {
                break;
            }
            long[] jArr = this.mMemFactorDurations;
            jArr[ip] = jArr[ip] + processStats.mMemFactorDurations[ip];
            i = ip + 1;
        }
        this.mSysMemUsage.mergeStats(processStats.mSysMemUsage);
        if (processStats.mTimePeriodStartClock < this.mTimePeriodStartClock) {
            this.mTimePeriodStartClock = processStats.mTimePeriodStartClock;
            this.mTimePeriodStartClockStr = processStats.mTimePeriodStartClockStr;
        }
        this.mTimePeriodEndRealtime += processStats.mTimePeriodEndRealtime - processStats.mTimePeriodStartRealtime;
        this.mTimePeriodEndUptime += processStats.mTimePeriodEndUptime - processStats.mTimePeriodStartUptime;
        this.mInternalSinglePssCount += processStats.mInternalSinglePssCount;
        this.mInternalSinglePssTime += processStats.mInternalSinglePssTime;
        this.mInternalAllMemPssCount += processStats.mInternalAllMemPssCount;
        this.mInternalAllMemPssTime += processStats.mInternalAllMemPssTime;
        this.mInternalAllPollPssCount += processStats.mInternalAllPollPssCount;
        this.mInternalAllPollPssTime += processStats.mInternalAllPollPssTime;
        this.mExternalPssCount += processStats.mExternalPssCount;
        this.mExternalPssTime += processStats.mExternalPssTime;
        this.mExternalSlowPssCount += processStats.mExternalSlowPssCount;
        this.mExternalSlowPssTime += processStats.mExternalSlowPssTime;
        this.mHasSwappedOutPss |= processStats.mHasSwappedOutPss;
    }

    public void addSysMemUsage(long cachedMem, long freeMem, long zramMem, long kernelMem, long nativeMem) {
        if (this.mMemFactor != -1) {
            int state = this.mMemFactor * 14;
            this.mSysMemUsageArgs[0] = 1;
            for (int i = 0; i < 3; i++) {
                this.mSysMemUsageArgs[1 + i] = cachedMem;
                this.mSysMemUsageArgs[4 + i] = freeMem;
                this.mSysMemUsageArgs[7 + i] = zramMem;
                this.mSysMemUsageArgs[10 + i] = kernelMem;
                this.mSysMemUsageArgs[13 + i] = nativeMem;
            }
            this.mSysMemUsage.mergeStats(state, this.mSysMemUsageArgs, 0);
        }
    }

    public void computeTotalMemoryUse(TotalMemoryUseCollection data, long now) {
        int i;
        long j;
        TotalMemoryUseCollection totalMemoryUseCollection = data;
        long j2 = now;
        totalMemoryUseCollection.totalTime = 0;
        int i2 = 0;
        for (i = 0; i < 14; i++) {
            totalMemoryUseCollection.processStateWeight[i] = 0.0d;
            totalMemoryUseCollection.processStatePss[i] = 0;
            totalMemoryUseCollection.processStateTime[i] = 0;
            totalMemoryUseCollection.processStateSamples[i] = 0;
        }
        for (i = 0; i < 16; i++) {
            totalMemoryUseCollection.sysMemUsage[i] = 0;
        }
        totalMemoryUseCollection.sysMemCachedWeight = 0.0d;
        totalMemoryUseCollection.sysMemFreeWeight = 0.0d;
        totalMemoryUseCollection.sysMemZRamWeight = 0.0d;
        totalMemoryUseCollection.sysMemKernelWeight = 0.0d;
        totalMemoryUseCollection.sysMemNativeWeight = 0.0d;
        totalMemoryUseCollection.sysMemSamples = 0;
        long[] totalMemUsage = this.mSysMemUsage.getTotalMemUsage();
        int is = 0;
        while (is < totalMemoryUseCollection.screenStates.length) {
            i = i2;
            while (i < totalMemoryUseCollection.memStates.length) {
                long[] totalMemUsage2;
                int im;
                int memBucket = totalMemoryUseCollection.screenStates[is] + totalMemoryUseCollection.memStates[i];
                int stateBucket = memBucket * 14;
                long memTime = this.mMemFactorDurations[memBucket];
                if (this.mMemFactor == memBucket) {
                    memTime += j2 - this.mStartTime;
                }
                totalMemoryUseCollection.totalTime += memTime;
                int sysKey = this.mSysMemUsage.getKey((byte) stateBucket);
                long[] longs = totalMemUsage;
                int idx = 0;
                if (sysKey != -1) {
                    long[] tmpLongs = this.mSysMemUsage.getArrayForKey(sysKey);
                    int tmpIndex = SparseMappingTable.getIndexFromKey(sysKey);
                    if (tmpLongs[tmpIndex + 0] >= 3) {
                        totalMemUsage2 = totalMemUsage;
                        SysMemUsageTable.mergeSysMemUsage(totalMemoryUseCollection.sysMemUsage, i2, longs, 0);
                        longs = tmpLongs;
                        idx = tmpIndex;
                        im = i;
                        totalMemoryUseCollection.sysMemCachedWeight += ((double) longs[idx + 2]) * ((double) memTime);
                        totalMemoryUseCollection.sysMemFreeWeight += ((double) longs[idx + 5]) * ((double) memTime);
                        totalMemoryUseCollection.sysMemZRamWeight += ((double) longs[idx + 8]) * ((double) memTime);
                        totalMemoryUseCollection.sysMemKernelWeight += ((double) longs[idx + 11]) * ((double) memTime);
                        totalMemoryUseCollection.sysMemNativeWeight += ((double) longs[idx + 14]) * ((double) memTime);
                        totalMemoryUseCollection.sysMemSamples = (int) (((long) totalMemoryUseCollection.sysMemSamples) + longs[idx + 0]);
                        i = im + 1;
                        totalMemUsage = totalMemUsage2;
                        j2 = now;
                        i2 = 0;
                    }
                }
                totalMemUsage2 = totalMemUsage;
                im = i;
                totalMemoryUseCollection.sysMemCachedWeight += ((double) longs[idx + 2]) * ((double) memTime);
                totalMemoryUseCollection.sysMemFreeWeight += ((double) longs[idx + 5]) * ((double) memTime);
                totalMemoryUseCollection.sysMemZRamWeight += ((double) longs[idx + 8]) * ((double) memTime);
                totalMemoryUseCollection.sysMemKernelWeight += ((double) longs[idx + 11]) * ((double) memTime);
                totalMemoryUseCollection.sysMemNativeWeight += ((double) longs[idx + 14]) * ((double) memTime);
                totalMemoryUseCollection.sysMemSamples = (int) (((long) totalMemoryUseCollection.sysMemSamples) + longs[idx + 0]);
                i = im + 1;
                totalMemUsage = totalMemUsage2;
                j2 = now;
                i2 = 0;
            }
            is++;
            j2 = now;
            i2 = 0;
        }
        totalMemoryUseCollection.hasSwappedOutPss = this.mHasSwappedOutPss;
        ArrayMap<String, SparseArray<ProcessState>> procMap = this.mProcesses.getMap();
        for (int iproc = 0; iproc < procMap.size(); iproc++) {
            SparseArray<ProcessState> uids = (SparseArray) procMap.valueAt(iproc);
            for (is = 0; is < uids.size(); is++) {
                ((ProcessState) uids.valueAt(is)).aggregatePss(totalMemoryUseCollection, now);
            }
            j = now;
        }
        j = now;
    }

    public void reset() {
        resetCommon();
        this.mPackages.getMap().clear();
        this.mProcesses.getMap().clear();
        this.mMemFactor = -1;
        this.mStartTime = 0;
    }

    public void resetSafely() {
        int ip;
        int iu;
        resetCommon();
        long now = SystemClock.uptimeMillis();
        ArrayMap<String, SparseArray<ProcessState>> procMap = this.mProcesses.getMap();
        for (int ip2 = procMap.size() - 1; ip2 >= 0; ip2--) {
            SparseArray<ProcessState> uids = (SparseArray) procMap.valueAt(ip2);
            for (int iu2 = uids.size() - 1; iu2 >= 0; iu2--) {
                ((ProcessState) uids.valueAt(iu2)).tmpNumInUse = 0;
            }
        }
        ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap = this.mPackages.getMap();
        for (ip = pkgMap.size() - 1; ip >= 0; ip--) {
            SparseArray<LongSparseArray<PackageState>> uids2 = (SparseArray) pkgMap.valueAt(ip);
            for (iu = uids2.size() - 1; iu >= 0; iu--) {
                LongSparseArray<PackageState> vpkgs = (LongSparseArray) uids2.valueAt(iu);
                for (int iv = vpkgs.size() - 1; iv >= 0; iv--) {
                    int iproc;
                    PackageState pkgState = (PackageState) vpkgs.valueAt(iv);
                    for (iproc = pkgState.mProcesses.size() - 1; iproc >= 0; iproc--) {
                        ProcessState ps = (ProcessState) pkgState.mProcesses.valueAt(iproc);
                        if (ps.isInUse()) {
                            ps.resetSafely(now);
                            ProcessState commonProcess = ps.getCommonProcess();
                            commonProcess.tmpNumInUse++;
                            ps.getCommonProcess().tmpFoundSubProc = ps;
                        } else {
                            ((ProcessState) pkgState.mProcesses.valueAt(iproc)).makeDead();
                            pkgState.mProcesses.removeAt(iproc);
                        }
                    }
                    for (iproc = pkgState.mServices.size() - 1; iproc >= 0; iproc--) {
                        ServiceState ss = (ServiceState) pkgState.mServices.valueAt(iproc);
                        if (ss.isInUse()) {
                            ss.resetSafely(now);
                        } else {
                            pkgState.mServices.removeAt(iproc);
                        }
                    }
                    if (pkgState.mProcesses.size() <= 0 && pkgState.mServices.size() <= 0) {
                        vpkgs.removeAt(iv);
                    }
                }
                if (vpkgs.size() <= 0) {
                    uids2.removeAt(iu);
                }
            }
            if (uids2.size() <= 0) {
                pkgMap.removeAt(ip);
            }
        }
        for (ip = procMap.size() - 1; ip >= 0; ip--) {
            SparseArray<ProcessState> uids3 = (SparseArray) procMap.valueAt(ip);
            for (iu = uids3.size() - 1; iu >= 0; iu--) {
                ProcessState ps2 = (ProcessState) uids3.valueAt(iu);
                if (!ps2.isInUse() && ps2.tmpNumInUse <= 0) {
                    ps2.makeDead();
                    uids3.removeAt(iu);
                } else if (!ps2.isActive() && ps2.isMultiPackage() && ps2.tmpNumInUse == 1) {
                    ps2 = ps2.tmpFoundSubProc;
                    ps2.makeStandalone();
                    uids3.setValueAt(iu, ps2);
                } else {
                    ps2.resetSafely(now);
                }
            }
            if (uids3.size() <= 0) {
                procMap.removeAt(ip);
            }
        }
        this.mStartTime = now;
    }

    private void resetCommon() {
        this.mTimePeriodStartClock = System.currentTimeMillis();
        buildTimePeriodStartClockStr();
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.mTimePeriodEndRealtime = elapsedRealtime;
        this.mTimePeriodStartRealtime = elapsedRealtime;
        elapsedRealtime = SystemClock.uptimeMillis();
        this.mTimePeriodEndUptime = elapsedRealtime;
        this.mTimePeriodStartUptime = elapsedRealtime;
        this.mInternalSinglePssCount = 0;
        this.mInternalSinglePssTime = 0;
        this.mInternalAllMemPssCount = 0;
        this.mInternalAllMemPssTime = 0;
        this.mInternalAllPollPssCount = 0;
        this.mInternalAllPollPssTime = 0;
        this.mExternalPssCount = 0;
        this.mExternalPssTime = 0;
        this.mExternalSlowPssCount = 0;
        this.mExternalSlowPssTime = 0;
        this.mTableData.reset();
        Arrays.fill(this.mMemFactorDurations, 0);
        this.mSysMemUsage.resetTable();
        this.mStartTime = 0;
        this.mReadError = null;
        this.mFlags = 0;
        evaluateSystemProperties(true);
        updateFragmentation();
    }

    public boolean evaluateSystemProperties(boolean update) {
        boolean changed = false;
        String runtime = SystemProperties.get("persist.sys.dalvik.vm.lib.2", VMRuntime.getRuntime().vmLibrary());
        if (!Objects.equals(runtime, this.mRuntime)) {
            changed = true;
            if (update) {
                this.mRuntime = runtime;
            }
        }
        return changed;
    }

    private void buildTimePeriodStartClockStr() {
        this.mTimePeriodStartClockStr = DateFormat.format("yyyy-MM-dd-HH-mm-ss", this.mTimePeriodStartClock).toString();
    }

    public void updateFragmentation() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/pagetypeinfo"));
            Matcher matcher = sPageTypeRegex.matcher("");
            this.mPageTypeZones.clear();
            this.mPageTypeLabels.clear();
            this.mPageTypeSizes.clear();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    try {
                        break;
                    } catch (IOException e) {
                    }
                } else {
                    matcher.reset(line);
                    if (matcher.matches()) {
                        Integer zone = Integer.valueOf(matcher.group(1), 10);
                        if (zone != null) {
                            this.mPageTypeZones.add(zone);
                            this.mPageTypeLabels.add(matcher.group(2));
                            this.mPageTypeSizes.add(splitAndParseNumbers(matcher.group(3)));
                        }
                    }
                }
            }
            reader.close();
        } catch (IOException e2) {
            this.mPageTypeZones.clear();
            this.mPageTypeLabels.clear();
            this.mPageTypeSizes.clear();
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                }
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    private static int[] splitAndParseNumbers(String s) {
        int count = 0;
        int N = s.length();
        int i = 0;
        boolean digit = false;
        for (int i2 = 0; i2 < N; i2++) {
            char c = s.charAt(i2);
            if (c < '0' || c > '9') {
                digit = false;
            } else if (!digit) {
                digit = true;
                count++;
            }
        }
        int[] result = new int[count];
        int p = 0;
        int val = 0;
        while (i < N) {
            char c2 = s.charAt(i);
            if (c2 < '0' || c2 > '9') {
                if (digit) {
                    digit = false;
                    int p2 = p + 1;
                    result[p] = val;
                    p = p2;
                }
            } else if (digit) {
                val = (val * 10) + (c2 - 48);
            } else {
                digit = true;
                val = c2 - 48;
            }
            i++;
        }
        if (count > 0) {
            result[count - 1] = val;
        }
        return result;
    }

    private void writeCompactedLongArray(Parcel out, long[] array, int num) {
        for (int i = 0; i < num; i++) {
            long val = array[i];
            if (val < 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Time val negative: ");
                stringBuilder.append(val);
                Slog.w(str, stringBuilder.toString());
                val = 0;
            }
            if (val <= 2147483647L) {
                out.writeInt((int) val);
            } else {
                int bottom = (int) (4294967295L & val);
                out.writeInt(~((int) (2147483647L & (val >> 32))));
                out.writeInt(bottom);
            }
        }
    }

    private void readCompactedLongArray(Parcel in, int version, long[] array, int num) {
        if (version <= 10) {
            in.readLongArray(array);
            return;
        }
        int alen = array.length;
        if (num <= alen) {
            int i = 0;
            while (i < num) {
                int val = in.readInt();
                if (val >= 0) {
                    array[i] = (long) val;
                } else {
                    array[i] = (((long) (~val)) << 32) | ((long) in.readInt());
                }
                i++;
            }
            while (i < alen) {
                array[i] = 0;
                i++;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bad array lengths: got ");
        stringBuilder.append(num);
        stringBuilder.append(" array is ");
        stringBuilder.append(alen);
        throw new RuntimeException(stringBuilder.toString());
    }

    private void writeCommonString(Parcel out, String name) {
        Integer index = (Integer) this.mCommonStringToIndex.get(name);
        if (index != null) {
            out.writeInt(index.intValue());
            return;
        }
        index = Integer.valueOf(this.mCommonStringToIndex.size());
        this.mCommonStringToIndex.put(name, index);
        out.writeInt(~index.intValue());
        out.writeString(name);
    }

    private String readCommonString(Parcel in, int version) {
        if (version <= 9) {
            return in.readString();
        }
        int index = in.readInt();
        if (index >= 0) {
            return (String) this.mIndexToCommonString.get(index);
        }
        index = ~index;
        String name = in.readString();
        while (this.mIndexToCommonString.size() <= index) {
            this.mIndexToCommonString.add(null);
        }
        this.mIndexToCommonString.set(index, name);
        return name;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        writeToParcel(out, SystemClock.uptimeMillis(), flags);
    }

    public void writeToParcel(Parcel out, long now, int flags) {
        int ip;
        int NUID;
        int iu;
        int NUID2;
        int NVERS;
        int iv;
        int ip2;
        int NPROC;
        ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap;
        Parcel parcel = out;
        long j = now;
        parcel.writeInt(MAGIC);
        parcel.writeInt(27);
        parcel.writeInt(14);
        parcel.writeInt(8);
        parcel.writeInt(10);
        parcel.writeInt(16);
        parcel.writeInt(4096);
        this.mCommonStringToIndex = new ArrayMap(this.mProcesses.size());
        ArrayMap<String, SparseArray<ProcessState>> procMap = this.mProcesses.getMap();
        int NPROC2 = procMap.size();
        for (ip = 0; ip < NPROC2; ip++) {
            SparseArray<ProcessState> uids = (SparseArray) procMap.valueAt(ip);
            NUID = uids.size();
            for (iu = 0; iu < NUID; iu++) {
                ((ProcessState) uids.valueAt(iu)).commitStateTime(j);
            }
        }
        ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap2 = this.mPackages.getMap();
        int NPKG = pkgMap2.size();
        for (NUID = 0; NUID < NPKG; NUID++) {
            SparseArray<LongSparseArray<PackageState>> uids2 = (SparseArray) pkgMap2.valueAt(NUID);
            NUID2 = uids2.size();
            for (int iu2 = 0; iu2 < NUID2; iu2++) {
                int NUID3;
                LongSparseArray<PackageState> vpkgs = (LongSparseArray) uids2.valueAt(iu2);
                NVERS = vpkgs.size();
                iv = 0;
                while (iv < NVERS) {
                    int NPROCS;
                    LongSparseArray<PackageState> vpkgs2;
                    PackageState pkgState = (PackageState) vpkgs.valueAt(iv);
                    SparseArray<LongSparseArray<PackageState>> uids3 = uids2;
                    uids2 = pkgState.mProcesses.size();
                    int iproc = 0;
                    while (true) {
                        NUID3 = NUID2;
                        NUID2 = iproc;
                        if (NUID2 >= uids2) {
                            break;
                        }
                        NPROCS = uids2;
                        Object uids4 = (ProcessState) pkgState.mProcesses.valueAt(NUID2);
                        vpkgs2 = vpkgs;
                        if (uids4.getCommonProcess() != uids4) {
                            uids4.commitStateTime(j);
                        }
                        iproc = NUID2 + 1;
                        NUID2 = NUID3;
                        uids2 = NPROCS;
                        vpkgs = vpkgs2;
                    }
                    NPROCS = uids2;
                    vpkgs2 = vpkgs;
                    uids2 = pkgState.mServices.size();
                    for (NUID2 = 0; NUID2 < uids2; NUID2++) {
                        ((ServiceState) pkgState.mServices.valueAt(NUID2)).commitStateTime(j);
                    }
                    iv++;
                    uids2 = uids3;
                    NUID2 = NUID3;
                    vpkgs = vpkgs2;
                }
                NUID3 = NUID2;
            }
        }
        parcel.writeLong(this.mTimePeriodStartClock);
        parcel.writeLong(this.mTimePeriodStartRealtime);
        parcel.writeLong(this.mTimePeriodEndRealtime);
        parcel.writeLong(this.mTimePeriodStartUptime);
        parcel.writeLong(this.mTimePeriodEndUptime);
        parcel.writeLong(this.mInternalSinglePssCount);
        parcel.writeLong(this.mInternalSinglePssTime);
        parcel.writeLong(this.mInternalAllMemPssCount);
        parcel.writeLong(this.mInternalAllMemPssTime);
        parcel.writeLong(this.mInternalAllPollPssCount);
        parcel.writeLong(this.mInternalAllPollPssTime);
        parcel.writeLong(this.mExternalPssCount);
        parcel.writeLong(this.mExternalPssTime);
        parcel.writeLong(this.mExternalSlowPssCount);
        parcel.writeLong(this.mExternalSlowPssTime);
        parcel.writeString(this.mRuntime);
        parcel.writeInt(this.mHasSwappedOutPss);
        parcel.writeInt(this.mFlags);
        this.mTableData.writeToParcel(parcel);
        if (this.mMemFactor != -1) {
            long[] jArr = this.mMemFactorDurations;
            NUID = this.mMemFactor;
            jArr[NUID] = jArr[NUID] + (j - this.mStartTime);
            this.mStartTime = j;
        }
        writeCompactedLongArray(parcel, this.mMemFactorDurations, this.mMemFactorDurations.length);
        this.mSysMemUsage.writeToParcel(parcel);
        parcel.writeInt(NPROC2);
        for (ip2 = 0; ip2 < NPROC2; ip2++) {
            writeCommonString(parcel, (String) procMap.keyAt(ip2));
            SparseArray<ProcessState> uids5 = (SparseArray) procMap.valueAt(ip2);
            iu = uids5.size();
            parcel.writeInt(iu);
            for (NUID2 = 0; NUID2 < iu; NUID2++) {
                parcel.writeInt(uids5.keyAt(NUID2));
                ProcessState proc = (ProcessState) uids5.valueAt(NUID2);
                writeCommonString(parcel, proc.getPackage());
                parcel.writeLong(proc.getVersion());
                proc.writeToParcel(parcel, j);
            }
        }
        parcel.writeInt(NPKG);
        for (ip2 = 0; ip2 < NPKG; ip2++) {
            writeCommonString(parcel, (String) pkgMap2.keyAt(ip2));
            SparseArray<LongSparseArray<PackageState>> uids6 = (SparseArray) pkgMap2.valueAt(ip2);
            iu = uids6.size();
            parcel.writeInt(iu);
            for (NUID2 = 0; NUID2 < iu; NUID2++) {
                parcel.writeInt(uids6.keyAt(NUID2));
                LongSparseArray<PackageState> vpkgs3 = (LongSparseArray) uids6.valueAt(NUID2);
                int NVERS2 = vpkgs3.size();
                parcel.writeInt(NVERS2);
                NVERS = 0;
                while (NVERS < NVERS2) {
                    ArrayMap<String, SparseArray<ProcessState>> procMap2 = procMap;
                    NPROC = NPROC2;
                    parcel.writeLong(vpkgs3.keyAt(NVERS));
                    procMap = (PackageState) vpkgs3.valueAt(NVERS);
                    NPROC2 = procMap.mProcesses.size();
                    parcel.writeInt(NPROC2);
                    iv = 0;
                    while (iv < NPROC2) {
                        int NPROCS2 = NPROC2;
                        writeCommonString(parcel, (String) procMap.mProcesses.keyAt(iv));
                        Object NPROCS3 = (ProcessState) procMap.mProcesses.valueAt(iv);
                        pkgMap = pkgMap2;
                        if (NPROCS3.getCommonProcess() == NPROCS3) {
                            parcel.writeInt(0);
                        } else {
                            parcel.writeInt(1);
                            NPROCS3.writeToParcel(parcel, j);
                        }
                        iv++;
                        NPROC2 = NPROCS2;
                        pkgMap2 = pkgMap;
                    }
                    pkgMap = pkgMap2;
                    NPROC2 = procMap.mServices.size();
                    parcel.writeInt(NPROC2);
                    ip = 0;
                    while (ip < NPROC2) {
                        parcel.writeString((String) procMap.mServices.keyAt(ip));
                        ServiceState svc = (ServiceState) procMap.mServices.valueAt(ip);
                        ArrayMap<String, SparseArray<ProcessState>> pkgState2 = procMap;
                        writeCommonString(parcel, svc.getProcessName());
                        svc.writeToParcel(parcel, j);
                        ip++;
                        procMap = pkgState2;
                    }
                    NVERS++;
                    procMap = procMap2;
                    NPROC2 = NPROC;
                    pkgMap2 = pkgMap;
                }
                NPROC = NPROC2;
                pkgMap = pkgMap2;
            }
            NPROC = NPROC2;
            pkgMap = pkgMap2;
        }
        NPROC = NPROC2;
        pkgMap = pkgMap2;
        int NPAGETYPES = this.mPageTypeLabels.size();
        parcel.writeInt(NPAGETYPES);
        int i = 0;
        while (true) {
            NPROC2 = i;
            if (NPROC2 < NPAGETYPES) {
                parcel.writeInt(((Integer) this.mPageTypeZones.get(NPROC2)).intValue());
                parcel.writeString((String) this.mPageTypeLabels.get(NPROC2));
                parcel.writeIntArray((int[]) this.mPageTypeSizes.get(NPROC2));
                i = NPROC2 + 1;
            } else {
                this.mCommonStringToIndex = null;
                return;
            }
        }
    }

    private boolean readCheckedInt(Parcel in, int val, String what) {
        int readInt = in.readInt();
        int got = readInt;
        if (readInt == val) {
            return true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bad ");
        stringBuilder.append(what);
        stringBuilder.append(": ");
        stringBuilder.append(got);
        this.mReadError = stringBuilder.toString();
        return false;
    }

    static byte[] readFully(InputStream stream, int[] outLen) throws IOException {
        int pos = 0;
        int initialAvail = stream.available();
        byte[] data = new byte[(initialAvail > 0 ? initialAvail + 1 : 16384)];
        while (true) {
            int amt = stream.read(data, pos, data.length - pos);
            if (amt < 0) {
                outLen[0] = pos;
                return data;
            }
            pos += amt;
            if (pos >= data.length) {
                byte[] newData = new byte[(pos + 16384)];
                System.arraycopy(data, 0, newData, 0, pos);
                data = newData;
            }
        }
    }

    public void read(InputStream stream) {
        try {
            int[] len = new int[1];
            byte[] raw = readFully(stream, len);
            Parcel in = Parcel.obtain();
            in.unmarshall(raw, 0, len[0]);
            in.setDataPosition(0);
            stream.close();
            readFromParcel(in);
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("caught exception: ");
            stringBuilder.append(e);
            this.mReadError = stringBuilder.toString();
        }
    }

    public void readFromParcel(Parcel in) {
        Parcel parcel = in;
        boolean z = false;
        boolean z2 = this.mPackages.getMap().size() > 0 || this.mProcesses.getMap().size() > 0;
        boolean hadData = z2;
        if (hadData) {
            resetSafely();
        }
        if (readCheckedInt(parcel, MAGIC, "magic number")) {
            int version = in.readInt();
            StringBuilder stringBuilder;
            if (version != 27) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("bad version: ");
                stringBuilder.append(version);
                this.mReadError = stringBuilder.toString();
            } else if (readCheckedInt(parcel, 14, "state count") && readCheckedInt(parcel, 8, "adj count") && readCheckedInt(parcel, 10, "pss count") && readCheckedInt(parcel, 16, "sys mem usage count") && readCheckedInt(parcel, 4096, "longs size")) {
                this.mIndexToCommonString = new ArrayList();
                this.mTimePeriodStartClock = in.readLong();
                buildTimePeriodStartClockStr();
                this.mTimePeriodStartRealtime = in.readLong();
                this.mTimePeriodEndRealtime = in.readLong();
                this.mTimePeriodStartUptime = in.readLong();
                this.mTimePeriodEndUptime = in.readLong();
                this.mInternalSinglePssCount = in.readLong();
                this.mInternalSinglePssTime = in.readLong();
                this.mInternalAllMemPssCount = in.readLong();
                this.mInternalAllMemPssTime = in.readLong();
                this.mInternalAllPollPssCount = in.readLong();
                this.mInternalAllPollPssTime = in.readLong();
                this.mExternalPssCount = in.readLong();
                this.mExternalPssTime = in.readLong();
                this.mExternalSlowPssCount = in.readLong();
                this.mExternalSlowPssTime = in.readLong();
                this.mRuntime = in.readString();
                this.mHasSwappedOutPss = in.readInt() != 0;
                this.mFlags = in.readInt();
                this.mTableData.readFromParcel(parcel);
                readCompactedLongArray(parcel, version, this.mMemFactorDurations, this.mMemFactorDurations.length);
                if (this.mSysMemUsage.readFromParcel(parcel)) {
                    int NPROC = in.readInt();
                    StringBuilder stringBuilder2;
                    if (NPROC < 0) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("bad process count: ");
                        stringBuilder2.append(NPROC);
                        this.mReadError = stringBuilder2.toString();
                        return;
                    }
                    int NUID;
                    int uid;
                    int NPROC2 = NPROC;
                    while (NPROC2 > 0) {
                        int NPROC3 = NPROC2 - 1;
                        String procName = readCommonString(parcel, version);
                        if (procName == null) {
                            this.mReadError = "bad process name";
                            return;
                        }
                        NPROC = in.readInt();
                        if (NPROC < 0) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("bad uid count: ");
                            stringBuilder2.append(NPROC);
                            this.mReadError = stringBuilder2.toString();
                            return;
                        }
                        while (NPROC > 0) {
                            NUID = NPROC - 1;
                            NPROC2 = in.readInt();
                            if (NPROC2 < 0) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("bad uid: ");
                                stringBuilder.append(NPROC2);
                                this.mReadError = stringBuilder.toString();
                                return;
                            }
                            String pkgName = readCommonString(parcel, version);
                            if (pkgName == null) {
                                this.mReadError = "bad process package name";
                                return;
                            }
                            long vers = in.readLong();
                            ProcessState proc = hadData ? (ProcessState) this.mProcesses.get(procName, NPROC2) : null;
                            if (proc == null) {
                                uid = NPROC2;
                                proc = new ProcessState(this, pkgName, NPROC2, vers, procName);
                                if (!proc.readFromParcel(parcel, true)) {
                                    return;
                                }
                            } else if (proc.readFromParcel(parcel, false)) {
                                uid = NPROC2;
                            } else {
                                return;
                            }
                            this.mProcesses.put(procName, uid, proc);
                            NPROC = NUID;
                        }
                        NPROC2 = NPROC3;
                    }
                    NPROC = in.readInt();
                    if (NPROC < 0) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("bad package count: ");
                        stringBuilder2.append(NPROC);
                        this.mReadError = stringBuilder2.toString();
                        return;
                    }
                    while (NPROC > 0) {
                        uid = NPROC - 1;
                        String pkgName2 = readCommonString(parcel, version);
                        if (pkgName2 == null) {
                            this.mReadError = "bad package name";
                            return;
                        }
                        NPROC = in.readInt();
                        if (NPROC < 0) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("bad uid count: ");
                            stringBuilder2.append(NPROC);
                            this.mReadError = stringBuilder2.toString();
                            return;
                        }
                        while (NPROC > 0) {
                            int NUID2 = NPROC - 1;
                            int uid2 = in.readInt();
                            if (uid2 < 0) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("bad uid: ");
                                stringBuilder.append(uid2);
                                this.mReadError = stringBuilder.toString();
                                return;
                            }
                            NPROC = in.readInt();
                            if (NPROC < 0) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("bad versions count: ");
                                stringBuilder2.append(NPROC);
                                this.mReadError = stringBuilder2.toString();
                                return;
                            }
                            while (NPROC > 0) {
                                NUID = NPROC - 1;
                                long vers2 = in.readLong();
                                PackageState pkgState = new PackageState(pkgName2, uid2);
                                LongSparseArray<PackageState> vpkg = (LongSparseArray) this.mPackages.get(pkgName2, uid2);
                                if (vpkg == null) {
                                    vpkg = new LongSparseArray();
                                    this.mPackages.put(pkgName2, uid2, vpkg);
                                }
                                vpkg.put(vers2, pkgState);
                                int NPROCS = in.readInt();
                                if (NPROCS < 0) {
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("bad package process count: ");
                                    stringBuilder3.append(NPROCS);
                                    this.mReadError = stringBuilder3.toString();
                                    return;
                                }
                                String procName2;
                                LongSparseArray<PackageState> vpkg2;
                                int NPROCS2 = NPROCS;
                                while (NPROCS2 > 0) {
                                    NPROCS2--;
                                    procName2 = readCommonString(parcel, version);
                                    if (procName2 == null) {
                                        this.mReadError = "bad package process name";
                                        return;
                                    }
                                    int hasProc = in.readInt();
                                    ProcessState commonProc = (ProcessState) this.mProcesses.get(procName2, uid2);
                                    if (commonProc == null) {
                                        StringBuilder stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("no common proc: ");
                                        stringBuilder4.append(procName2);
                                        this.mReadError = stringBuilder4.toString();
                                        return;
                                    }
                                    vpkg2 = vpkg;
                                    if (hasProc != 0) {
                                        vpkg = hadData ? (ProcessState) pkgState.mProcesses.get(procName2) : null;
                                        if (vpkg == null) {
                                            vpkg = new ProcessState(commonProc, pkgName2, uid2, vers2, procName2, 0);
                                            if (!vpkg.readFromParcel(parcel, true)) {
                                                return;
                                            }
                                        } else if (!vpkg.readFromParcel(parcel, z)) {
                                            return;
                                        }
                                        pkgState.mProcesses.put(procName2, vpkg);
                                    } else {
                                        pkgState.mProcesses.put(procName2, commonProc);
                                    }
                                    vpkg = vpkg2;
                                    z = false;
                                }
                                vpkg2 = vpkg;
                                NPROC = in.readInt();
                                if (NPROC < 0) {
                                    StringBuilder stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("bad package service count: ");
                                    stringBuilder5.append(NPROC);
                                    this.mReadError = stringBuilder5.toString();
                                    return;
                                }
                                while (NPROC > 0) {
                                    int NSRVS = NPROC - 1;
                                    String serviceName = in.readString();
                                    if (serviceName == null) {
                                        this.mReadError = "bad package service name";
                                        return;
                                    }
                                    LongSparseArray<PackageState> vpkg3;
                                    PackageState pkgState2;
                                    long vers3;
                                    int uid3;
                                    ServiceState serv;
                                    procName2 = version > 9 ? readCommonString(parcel, version) : null;
                                    ServiceState serv2 = hadData ? (ServiceState) pkgState.mServices.get(serviceName) : null;
                                    if (serv2 == null) {
                                        vpkg3 = vpkg2;
                                        pkgState2 = pkgState;
                                        vers3 = vers2;
                                        uid3 = uid2;
                                        serv = new ServiceState(this, pkgName2, serviceName, procName2, null);
                                    } else {
                                        pkgState2 = pkgState;
                                        vers3 = vers2;
                                        uid3 = uid2;
                                        vpkg3 = vpkg2;
                                        serv = serv2;
                                    }
                                    if (serv.readFromParcel(parcel)) {
                                        pkgState = pkgState2;
                                        pkgState.mServices.put(serviceName, serv);
                                        NPROC = NSRVS;
                                        vpkg2 = vpkg3;
                                        vers2 = vers3;
                                        uid2 = uid3;
                                    } else {
                                        return;
                                    }
                                }
                                NPROC = NUID;
                                z = false;
                            }
                            NPROC = NUID2;
                            z = false;
                        }
                        NPROC = uid;
                        z = false;
                    }
                    int NPAGETYPES = in.readInt();
                    this.mPageTypeZones.clear();
                    this.mPageTypeZones.ensureCapacity(NPAGETYPES);
                    this.mPageTypeLabels.clear();
                    this.mPageTypeLabels.ensureCapacity(NPAGETYPES);
                    this.mPageTypeSizes.clear();
                    this.mPageTypeSizes.ensureCapacity(NPAGETYPES);
                    int i = 0;
                    while (true) {
                        int i2 = i;
                        if (i2 < NPAGETYPES) {
                            this.mPageTypeZones.add(Integer.valueOf(in.readInt()));
                            this.mPageTypeLabels.add(in.readString());
                            this.mPageTypeSizes.add(in.createIntArray());
                            i = i2 + 1;
                        } else {
                            this.mIndexToCommonString = null;
                            return;
                        }
                    }
                }
            }
        }
    }

    public PackageState getPackageStateLocked(String packageName, int uid, long vers) {
        LongSparseArray<PackageState> vpkg = (LongSparseArray) this.mPackages.get(packageName, uid);
        if (vpkg == null) {
            vpkg = new LongSparseArray();
            this.mPackages.put(packageName, uid, vpkg);
        }
        PackageState as = (PackageState) vpkg.get(vers);
        if (as != null) {
            return as;
        }
        as = new PackageState(packageName, uid);
        vpkg.put(vers, as);
        return as;
    }

    public ProcessState getProcessStateLocked(String packageName, int uid, long vers, String processName) {
        int i = uid;
        String str = processName;
        PackageState pkgState = getPackageStateLocked(packageName, uid, vers);
        ProcessState ps = (ProcessState) pkgState.mProcesses.get(str);
        if (ps != null) {
            return ps;
        }
        ProcessState commonProc;
        PackageState pkgState2;
        ProcessState ps2;
        ProcessState commonProc2 = (ProcessState) this.mProcesses.get(str, i);
        if (commonProc2 == null) {
            commonProc = new ProcessState(this, packageName, i, vers, str);
            this.mProcesses.put(str, i, commonProc);
        } else {
            commonProc = commonProc2;
        }
        if (commonProc.isMultiPackage()) {
            pkgState2 = pkgState;
            commonProc2 = new ProcessState(commonProc, packageName, uid, vers, processName, SystemClock.uptimeMillis());
        } else {
            String str2 = packageName;
            if (str2.equals(commonProc.getPackage()) && vers == commonProc.getVersion()) {
                ps2 = commonProc;
                pkgState2 = pkgState;
            } else {
                commonProc.setMultiPackage(true);
                long now = SystemClock.uptimeMillis();
                PackageState commonPkgState = getPackageStateLocked(commonProc.getPackage(), i, commonProc.getVersion());
                if (commonPkgState != null) {
                    commonProc2 = commonProc.clone(now);
                    commonPkgState.mProcesses.put(commonProc.getName(), commonProc2);
                    int i2 = commonPkgState.mServices.size() - 1;
                    while (true) {
                        int i3 = i2;
                        if (i3 < 0) {
                            break;
                        }
                        ServiceState ss = (ServiceState) commonPkgState.mServices.valueAt(i3);
                        if (ss.getProcess() == commonProc) {
                            ss.setProcess(commonProc2);
                        }
                        i2 = i3 - 1;
                    }
                } else {
                    String str3 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Cloning proc state: no package state ");
                    stringBuilder.append(commonProc.getPackage());
                    stringBuilder.append("/");
                    stringBuilder.append(i);
                    stringBuilder.append(" for proc ");
                    stringBuilder.append(commonProc.getName());
                    Slog.w(str3, stringBuilder.toString());
                }
                pkgState2 = pkgState;
                commonProc2 = new ProcessState(commonProc, str2, i, vers, processName, now);
            }
        }
        pkgState2.mProcesses.put(processName, ps2);
        return ps2;
    }

    public ServiceState getServiceStateLocked(String packageName, int uid, long vers, String processName, String className) {
        PackageState as = getPackageStateLocked(packageName, uid, vers);
        ServiceState ss = (ServiceState) as.mServices.get(className);
        if (ss != null) {
            return ss;
        }
        ss = new ServiceState(this, packageName, className, processName, processName != null ? getProcessStateLocked(packageName, uid, vers, processName) : null);
        as.mServices.put(className, ss);
        return ss;
    }

    /* JADX WARNING: Removed duplicated region for block: B:76:0x023f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dumpLocked(PrintWriter pw, String reqPackage, long now, boolean dumpSummary, boolean dumpAll, boolean activeOnly) {
        int uid;
        boolean sepNeeded;
        int iv;
        boolean iv2;
        PrintWriter printWriter;
        PrintWriter printWriter2 = pw;
        String str = reqPackage;
        boolean z = dumpAll;
        long totalTime = DumpUtils.dumpSingleTime(null, null, this.mMemFactorDurations, this.mMemFactor, this.mStartTime, now);
        boolean sepNeeded2 = false;
        if (this.mSysMemUsage.getKeyCount() > 0) {
            printWriter2.println("System memory usage:");
            this.mSysMemUsage.dump(printWriter2, "  ", ALL_SCREEN_ADJ, ALL_MEM_ADJ);
            sepNeeded2 = true;
        }
        ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap = this.mPackages.getMap();
        boolean printedHeader = false;
        boolean sepNeeded3 = sepNeeded2;
        int ip = 0;
        while (ip < pkgMap.size()) {
            ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap2;
            String pkgName = (String) pkgMap.keyAt(ip);
            SparseArray<LongSparseArray<PackageState>> uids = (SparseArray) pkgMap.valueAt(ip);
            boolean sepNeeded4 = sepNeeded3;
            sepNeeded3 = printedHeader;
            int iu = 0;
            while (iu < uids.size()) {
                SparseArray<LongSparseArray<PackageState>> uids2;
                String pkgName2;
                uid = uids.keyAt(iu);
                LongSparseArray<PackageState> vpkgs = (LongSparseArray) uids.valueAt(iu);
                sepNeeded = sepNeeded4;
                sepNeeded4 = sepNeeded3;
                iv = 0;
                while (true) {
                    int iv3 = iv;
                    if (iv3 >= vpkgs.size()) {
                        break;
                    }
                    int iv4;
                    LongSparseArray<PackageState> vpkgs2;
                    int uid2;
                    int NPROCS;
                    PackageState pkgState;
                    PackageState pkgState2;
                    String str2;
                    PrintWriter printWriter3;
                    boolean z2;
                    long vers;
                    int iu2 = iu;
                    pkgMap2 = pkgMap;
                    long vers2 = vpkgs.keyAt(iv3);
                    PackageState pkgState3 = (PackageState) vpkgs.valueAt(iv3);
                    sepNeeded3 = pkgState3.mProcesses.size();
                    int ip2 = ip;
                    ip = pkgState3.mServices.size();
                    boolean z3 = str == null || str.equals(pkgName);
                    boolean pkgMatch = z3;
                    if (pkgMatch) {
                        iv4 = iv3;
                        vpkgs2 = vpkgs;
                        uids2 = uids;
                    } else {
                        z3 = false;
                        boolean iproc = false;
                        while (true) {
                            iv4 = iv3;
                            iv2 = iproc;
                            if (iv2 >= sepNeeded3) {
                                vpkgs2 = vpkgs;
                                uids2 = uids;
                                break;
                            }
                            vpkgs2 = vpkgs;
                            uids2 = uids;
                            if (str.equals(((ProcessState) pkgState3.mProcesses.valueAt(iv2)).getName())) {
                                z3 = true;
                                break;
                            }
                            iproc = iv2 + 1;
                            iv3 = iv4;
                            vpkgs = vpkgs2;
                            uids = uids2;
                        }
                        if (!z3) {
                            uid2 = uid;
                            pkgName2 = pkgName;
                            iv = iv4 + 1;
                            pkgMap = pkgMap2;
                            iu = iu2;
                            ip = ip2;
                            vpkgs = vpkgs2;
                            uids = uids2;
                            uid = uid2;
                            pkgName = pkgName2;
                        }
                    }
                    if (sepNeeded3 <= false || ip > 0) {
                        if (!sepNeeded4) {
                            if (sepNeeded) {
                                pw.println();
                            }
                            printWriter2.println("Per-Package Stats:");
                            sepNeeded4 = true;
                            sepNeeded = true;
                        }
                        printWriter2.print("  * ");
                        printWriter2.print(pkgName);
                        printWriter2.print(" / ");
                        UserHandle.formatUid(printWriter2, uid);
                        printWriter2.print(" / v");
                        printWriter2.print(vers2);
                        printWriter2.println(":");
                    }
                    boolean printedHeader2 = sepNeeded4;
                    boolean sepNeeded5 = sepNeeded;
                    if (!dumpSummary) {
                        NPROCS = sepNeeded3;
                        pkgState = pkgState3;
                        uid2 = uid;
                        pkgName2 = pkgName;
                    } else if (z) {
                        NPROCS = sepNeeded3;
                        pkgState = pkgState3;
                        uid2 = uid;
                        pkgName2 = pkgName;
                    } else {
                        ArrayList<ProcessState> procs = new ArrayList();
                        for (sepNeeded4 = false; sepNeeded4 < sepNeeded3; sepNeeded4++) {
                            ProcessState proc = (ProcessState) pkgState3.mProcesses.valueAt(sepNeeded4);
                            if ((pkgMatch || str.equals(proc.getName())) && (!activeOnly || proc.isInUse())) {
                                procs.add(proc);
                            }
                        }
                        sepNeeded = sepNeeded3;
                        pkgState = pkgState3;
                        uid2 = uid;
                        pkgName2 = pkgName;
                        DumpUtils.dumpProcessSummaryLocked(printWriter2, "      ", procs, ALL_SCREEN_ADJ, ALL_MEM_ADJ, NON_CACHED_PROC_STATES, now, totalTime);
                        uid = ip;
                        pkgState2 = pkgState;
                        str2 = str;
                        printWriter3 = printWriter2;
                        z2 = dumpAll;
                        ip = 0;
                        while (ip < uid) {
                            PackageState pkgState4;
                            int NSRVS;
                            ServiceState svc = (ServiceState) pkgState2.mServices.valueAt(ip);
                            if (pkgMatch || str2.equals(svc.getProcessName()) != null) {
                                if (!activeOnly || svc.isInUse()) {
                                    if (z2) {
                                        printWriter3.print("      Service ");
                                    } else {
                                        printWriter3.print("      * ");
                                    }
                                    printWriter3.print((String) pkgState2.mServices.keyAt(ip));
                                    printWriter3.println(":");
                                    printWriter3.print("        Process: ");
                                    printWriter3.println(svc.getProcessName());
                                    printWriter2 = printWriter3;
                                    str = str2;
                                    z = z2;
                                    pkgState4 = pkgState2;
                                    NSRVS = uid;
                                    svc.dumpStats(printWriter3, "        ", "          ", "    ", now, totalTime, dumpSummary, z);
                                    ip++;
                                    pkgState2 = pkgState4;
                                    z2 = z;
                                    str2 = str;
                                    printWriter3 = printWriter2;
                                    uid = NSRVS;
                                } else {
                                    printWriter3.print("      (Not active: ");
                                    printWriter3.print((String) pkgState2.mServices.keyAt(ip));
                                    printWriter3.println(")");
                                }
                            }
                            printWriter2 = printWriter3;
                            str = str2;
                            z = z2;
                            pkgState4 = pkgState2;
                            NSRVS = uid;
                            ip++;
                            pkgState2 = pkgState4;
                            z2 = z;
                            str2 = str;
                            printWriter3 = printWriter2;
                            uid = NSRVS;
                        }
                        printWriter2 = printWriter3;
                        str = str2;
                        z = z2;
                        sepNeeded = sepNeeded5;
                        sepNeeded4 = printedHeader2;
                        iv = iv4 + 1;
                        pkgMap = pkgMap2;
                        iu = iu2;
                        ip = ip2;
                        vpkgs = vpkgs2;
                        uids = uids2;
                        uid = uid2;
                        pkgName = pkgName2;
                    }
                    iv = 0;
                    while (true) {
                        int iproc2 = iv;
                        if (iproc2 >= NPROCS) {
                            break;
                        }
                        ProcessState proc2 = (ProcessState) pkgState.mProcesses.valueAt(iproc2);
                        if (pkgMatch || str.equals(proc2.getName())) {
                            if (!activeOnly || proc2.isInUse()) {
                                printWriter2.print("      Process ");
                                printWriter2.print((String) pkgState.mProcesses.keyAt(iproc2));
                                if (proc2.getCommonProcess().isMultiPackage()) {
                                    printWriter2.print(" (multi, ");
                                } else {
                                    printWriter2.print(" (unique, ");
                                }
                                printWriter2.print(proc2.getDurationsBucketCount());
                                printWriter2.print(" entries)");
                                printWriter2.println(":");
                                ProcessState proc3 = proc2;
                                proc2.dumpProcessState(printWriter2, "        ", ALL_SCREEN_ADJ, ALL_MEM_ADJ, ALL_PROC_STATES, now);
                                uid = ip;
                                vers = vers2;
                                pkgState2 = pkgState;
                                z2 = dumpAll;
                                str2 = str;
                                printWriter3 = printWriter2;
                                proc3.dumpPss(printWriter2, "        ", ALL_SCREEN_ADJ, ALL_MEM_ADJ, ALL_PROC_STATES);
                                proc3.dumpInternalLocked(printWriter3, "        ", z2);
                                iv = iproc2 + 1;
                                printWriter2 = printWriter3;
                                str = str2;
                                pkgState = pkgState2;
                                ip = uid;
                                vers2 = vers;
                            } else {
                                printWriter2.print("      (Not active: ");
                                printWriter2.print((String) pkgState.mProcesses.keyAt(iproc2));
                                printWriter2.println(")");
                            }
                        }
                        uid = ip;
                        vers = vers2;
                        pkgState2 = pkgState;
                        str2 = str;
                        printWriter3 = printWriter2;
                        z2 = dumpAll;
                        iv = iproc2 + 1;
                        printWriter2 = printWriter3;
                        str = str2;
                        pkgState = pkgState2;
                        ip = uid;
                        vers2 = vers;
                    }
                    uid = ip;
                    vers = vers2;
                    pkgState2 = pkgState;
                    str2 = str;
                    printWriter3 = printWriter2;
                    z2 = dumpAll;
                    ip = 0;
                    while (ip < uid) {
                    }
                    printWriter2 = printWriter3;
                    str = str2;
                    z = z2;
                    sepNeeded = sepNeeded5;
                    sepNeeded4 = printedHeader2;
                    iv = iv4 + 1;
                    pkgMap = pkgMap2;
                    iu = iu2;
                    ip = ip2;
                    vpkgs = vpkgs2;
                    uids = uids2;
                    uid = uid2;
                    pkgName = pkgName2;
                }
                pkgMap2 = pkgMap;
                uids2 = uids;
                pkgName2 = pkgName;
                iu++;
                sepNeeded3 = sepNeeded4;
                sepNeeded4 = sepNeeded;
            }
            pkgMap2 = pkgMap;
            ip++;
            printedHeader = sepNeeded3;
            sepNeeded3 = sepNeeded4;
        }
        ArrayMap<String, SparseArray<ProcessState>> procMap = this.mProcesses.getMap();
        boolean printedHeader3 = false;
        int numTotalProcs = 0;
        sepNeeded = sepNeeded3;
        ip = 0;
        iv = 0;
        while (true) {
            int ip3 = ip;
            if (ip3 >= procMap.size()) {
                break;
            }
            String procName = (String) procMap.keyAt(ip3);
            SparseArray<ProcessState> uids3 = (SparseArray) procMap.valueAt(ip3);
            iv2 = printedHeader3;
            int numShownProcs = iv;
            iv = 0;
            while (true) {
                int iu3 = iv;
                if (iu3 >= uids3.size()) {
                    break;
                }
                int iu4;
                SparseArray<ProcessState> uids4;
                String procName2;
                PrintWriter printWriter4;
                uid = uids3.keyAt(iu3);
                int numTotalProcs2 = numTotalProcs + 1;
                ProcessState proc4 = (ProcessState) uids3.valueAt(iu3);
                if (!proc4.hasAnyData() && proc4.isMultiPackage() && (r4 == null || r4.equals(procName) || r4.equals(proc4.getPackage()))) {
                    boolean printedHeader4;
                    int numShownProcs2 = numShownProcs + 1;
                    if (sepNeeded) {
                        pw.println();
                    }
                    if (iv2) {
                        printedHeader4 = iv2;
                    } else {
                        printWriter2.println("Multi-Package Common Processes:");
                        printedHeader4 = true;
                    }
                    if (!activeOnly || proc4.isInUse()) {
                        printWriter2.print("  * ");
                        printWriter2.print(procName);
                        printWriter2.print(" / ");
                        UserHandle.formatUid(printWriter2, uid);
                        printWriter2.print(" (");
                        printWriter2.print(proc4.getDurationsBucketCount());
                        printWriter2.print(" entries)");
                        printWriter2.println(":");
                        ProcessState proc5 = proc4;
                        iu4 = iu3;
                        proc4.dumpProcessState(printWriter2, "        ", ALL_SCREEN_ADJ, ALL_MEM_ADJ, ALL_PROC_STATES, now);
                        uids4 = uids3;
                        procName2 = procName;
                        uid = ip3;
                        sepNeeded3 = z;
                        printWriter4 = printWriter2;
                        proc5.dumpPss(printWriter2, "        ", ALL_SCREEN_ADJ, ALL_MEM_ADJ, ALL_PROC_STATES);
                        proc5.dumpInternalLocked(printWriter4, "        ", sepNeeded3);
                    } else {
                        printWriter2.print("      (Not active: ");
                        printWriter2.print(procName);
                        printWriter2.println(")");
                        uids4 = uids3;
                        uid = ip3;
                        sepNeeded3 = z;
                        printWriter4 = printWriter2;
                        iu4 = iu3;
                        procName2 = procName;
                    }
                    numShownProcs = numShownProcs2;
                    sepNeeded = true;
                    iv2 = printedHeader4;
                } else {
                    uids4 = uids3;
                    uid = ip3;
                    sepNeeded3 = z;
                    printWriter4 = printWriter2;
                    iu4 = iu3;
                    procName2 = procName;
                }
                str = reqPackage;
                z = sepNeeded3;
                printWriter2 = printWriter4;
                procName = procName2;
                ip3 = uid;
                numTotalProcs = numTotalProcs2;
                iv = iu4 + 1;
                uids3 = uids4;
            }
            sepNeeded3 = z;
            ip = ip3 + 1;
            str = reqPackage;
            iv = numShownProcs;
            printedHeader3 = iv2;
        }
        iv2 = z;
        if (iv2) {
            pw.println();
            printWriter2.print("  Total procs: ");
            printWriter2.print(iv);
            printWriter2.print(" shown of ");
            printWriter2.print(numTotalProcs);
            printWriter2.println(" total");
        }
        if (sepNeeded) {
            pw.println();
        }
        if (dumpSummary) {
            printWriter2.println("Summary:");
            printWriter = printWriter2;
            dumpSummaryLocked(printWriter2, reqPackage, now, activeOnly);
            long j = now;
        } else {
            printWriter = printWriter2;
            dumpTotalsLocked(printWriter, now);
        }
        if (iv2) {
            pw.println();
            printWriter.println("Internal state:");
            printWriter.print("  mRunning=");
            printWriter.println(this.mRunning);
        }
        dumpFragmentationLocked(pw);
    }

    public void dumpSummaryLocked(PrintWriter pw, String reqPackage, long now, boolean activeOnly) {
        PrintWriter printWriter = pw;
        dumpFilteredSummaryLocked(printWriter, null, "  ", ALL_SCREEN_ADJ, ALL_MEM_ADJ, ALL_PROC_STATES, NON_CACHED_PROC_STATES, now, DumpUtils.dumpSingleTime(null, null, this.mMemFactorDurations, this.mMemFactor, this.mStartTime, now), reqPackage, activeOnly);
        pw.println();
        dumpTotalsLocked(pw, now);
    }

    private void dumpFragmentationLocked(PrintWriter pw) {
        pw.println();
        pw.println("Available pages by page size:");
        int NPAGETYPES = this.mPageTypeLabels.size();
        for (int i = 0; i < NPAGETYPES; i++) {
            pw.format("Zone %3d  %14s ", new Object[]{this.mPageTypeZones.get(i), this.mPageTypeLabels.get(i)});
            int[] sizes = (int[]) this.mPageTypeSizes.get(i);
            int N = sizes == null ? 0 : sizes.length;
            for (int j = 0; j < N; j++) {
                pw.format("%6d", new Object[]{Integer.valueOf(sizes[j])});
            }
            pw.println();
        }
    }

    long printMemoryCategory(PrintWriter pw, String prefix, String label, double memWeight, long totalTime, long curTotalMem, int samples) {
        if (memWeight == 0.0d) {
            return curTotalMem;
        }
        long mem = (long) ((1024.0d * memWeight) / ((double) totalTime));
        pw.print(prefix);
        pw.print(label);
        pw.print(": ");
        DebugUtils.printSizeValue(pw, mem);
        pw.print(" (");
        pw.print(samples);
        pw.print(" samples)");
        pw.println();
        return curTotalMem + mem;
    }

    void dumpTotalsLocked(PrintWriter pw, long now) {
        PrintWriter printWriter = pw;
        printWriter.println("Run time Stats:");
        DumpUtils.dumpSingleTime(printWriter, "  ", this.mMemFactorDurations, this.mMemFactor, this.mStartTime, now);
        pw.println();
        printWriter.println("Memory usage:");
        TotalMemoryUseCollection totalMem = new TotalMemoryUseCollection(ALL_SCREEN_ADJ, ALL_MEM_ADJ);
        computeTotalMemoryUse(totalMem, now);
        PrintWriter printWriter2 = printWriter;
        int i = 0;
        long totalPss = printMemoryCategory(printWriter2, "  ", "Native ", totalMem.sysMemNativeWeight, totalMem.totalTime, printMemoryCategory(printWriter2, "  ", "Kernel ", totalMem.sysMemKernelWeight, totalMem.totalTime, 0, totalMem.sysMemSamples), totalMem.sysMemSamples);
        while (true) {
            int i2 = i;
            if (i2 >= 14) {
                break;
            }
            int i3;
            if (i2 != 6) {
                i3 = i2;
                totalPss = printMemoryCategory(printWriter, "  ", DumpUtils.STATE_NAMES[i2], totalMem.processStateWeight[i2], totalMem.totalTime, totalPss, totalMem.processStateSamples[i2]);
            } else {
                i3 = i2;
            }
            i = i3 + 1;
        }
        printWriter2 = printWriter;
        int i4 = 6;
        long totalPss2 = printMemoryCategory(printWriter2, "  ", "Z-Ram  ", totalMem.sysMemZRamWeight, totalMem.totalTime, printMemoryCategory(printWriter2, "  ", "Free   ", totalMem.sysMemFreeWeight, totalMem.totalTime, printMemoryCategory(printWriter2, "  ", "Cached ", totalMem.sysMemCachedWeight, totalMem.totalTime, totalPss, totalMem.sysMemSamples), totalMem.sysMemSamples), totalMem.sysMemSamples);
        printWriter.print("  TOTAL  : ");
        DebugUtils.printSizeValue(printWriter, totalPss2);
        pw.println();
        totalPss = totalPss2;
        printMemoryCategory(printWriter2, "  ", DumpUtils.STATE_NAMES[i4], totalMem.processStateWeight[i4], totalMem.totalTime, totalPss2, totalMem.processStateSamples[i4]);
        pw.println();
        printWriter.println("PSS collection stats:");
        printWriter.print("  Internal Single: ");
        printWriter.print(this.mInternalSinglePssCount);
        printWriter.print("x over ");
        TimeUtils.formatDuration(this.mInternalSinglePssTime, printWriter);
        pw.println();
        printWriter.print("  Internal All Procs (Memory Change): ");
        printWriter.print(this.mInternalAllMemPssCount);
        printWriter.print("x over ");
        TimeUtils.formatDuration(this.mInternalAllMemPssTime, printWriter);
        pw.println();
        printWriter.print("  Internal All Procs (Polling): ");
        printWriter.print(this.mInternalAllPollPssCount);
        printWriter.print("x over ");
        TimeUtils.formatDuration(this.mInternalAllPollPssTime, printWriter);
        pw.println();
        printWriter.print("  External: ");
        printWriter.print(this.mExternalPssCount);
        printWriter.print("x over ");
        TimeUtils.formatDuration(this.mExternalPssTime, printWriter);
        pw.println();
        printWriter.print("  External Slow: ");
        printWriter.print(this.mExternalSlowPssCount);
        printWriter.print("x over ");
        TimeUtils.formatDuration(this.mExternalSlowPssTime, printWriter);
        pw.println();
        pw.println();
        printWriter.print("          Start time: ");
        printWriter.print(DateFormat.format("yyyy-MM-dd HH:mm:ss", this.mTimePeriodStartClock));
        pw.println();
        printWriter.print("        Total uptime: ");
        TimeUtils.formatDuration((this.mRunning ? SystemClock.uptimeMillis() : this.mTimePeriodEndUptime) - this.mTimePeriodStartUptime, printWriter);
        pw.println();
        printWriter.print("  Total elapsed time: ");
        TimeUtils.formatDuration((this.mRunning ? SystemClock.elapsedRealtime() : this.mTimePeriodEndRealtime) - this.mTimePeriodStartRealtime, printWriter);
        boolean partial = true;
        if ((this.mFlags & 2) != 0) {
            printWriter.print(" (shutdown)");
            partial = false;
        }
        if ((this.mFlags & 4) != 0) {
            printWriter.print(" (sysprops)");
            partial = false;
        }
        if ((this.mFlags & 1) != 0) {
            printWriter.print(" (complete)");
            partial = false;
        }
        if (partial) {
            printWriter.print(" (partial)");
        }
        if (this.mHasSwappedOutPss) {
            printWriter.print(" (swapped-out-pss)");
        }
        printWriter.print(' ');
        printWriter.print(this.mRuntime);
        pw.println();
    }

    void dumpFilteredSummaryLocked(PrintWriter pw, String header, String prefix, int[] screenStates, int[] memStates, int[] procStates, int[] sortProcStates, long now, long totalTime, String reqPackage, boolean activeOnly) {
        ArrayList<ProcessState> procs = collectProcessesLocked(screenStates, memStates, procStates, sortProcStates, now, reqPackage, activeOnly);
        if (procs.size() > 0) {
            if (header != null) {
                pw.println();
                pw.println(header);
            }
            DumpUtils.dumpProcessSummaryLocked(pw, prefix, procs, screenStates, memStates, sortProcStates, now, totalTime);
        }
    }

    public ArrayList<ProcessState> collectProcessesLocked(int[] screenStates, int[] memStates, int[] procStates, int[] sortProcStates, long now, String reqPackage, boolean activeOnly) {
        String str = reqPackage;
        ArraySet<ProcessState> foundProcs = new ArraySet();
        ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap = this.mPackages.getMap();
        int ip = 0;
        while (ip < pkgMap.size()) {
            String pkgName = (String) pkgMap.keyAt(ip);
            SparseArray<LongSparseArray<PackageState>> procs = (SparseArray) pkgMap.valueAt(ip);
            int iu = 0;
            while (iu < procs.size()) {
                LongSparseArray<PackageState> vpkgs = (LongSparseArray) procs.valueAt(iu);
                int NVERS = vpkgs.size();
                int iv = 0;
                while (iv < NVERS) {
                    PackageState state = (PackageState) vpkgs.valueAt(iv);
                    int NPROCS = state.mProcesses.size();
                    boolean pkgMatch = str == null || str.equals(pkgName);
                    int iproc = 0;
                    while (iproc < NPROCS) {
                        ProcessState proc = (ProcessState) state.mProcesses.valueAt(iproc);
                        if ((pkgMatch || r0.equals(proc.getName())) && (!activeOnly || proc.isInUse())) {
                            foundProcs.add(proc.getCommonProcess());
                        }
                        iproc++;
                        str = reqPackage;
                    }
                    iv++;
                    str = reqPackage;
                }
                iu++;
                str = reqPackage;
            }
            ip++;
            str = reqPackage;
        }
        ArrayList<ProcessState> outProcs = new ArrayList(foundProcs.size());
        int i = 0;
        while (true) {
            int i2 = i;
            int[] iArr;
            int[] iArr2;
            if (i2 < foundProcs.size()) {
                ProcessState proc2 = (ProcessState) foundProcs.valueAt(i2);
                if (proc2.computeProcessTimeLocked(screenStates, memStates, procStates, now) > 0) {
                    outProcs.add(proc2);
                    iArr = sortProcStates;
                    if (procStates != iArr) {
                        proc2.computeProcessTimeLocked(screenStates, memStates, iArr, now);
                    }
                } else {
                    iArr2 = procStates;
                    iArr = sortProcStates;
                }
                i = i2 + 1;
            } else {
                iArr2 = procStates;
                iArr = sortProcStates;
                Collections.sort(outProcs, ProcessState.COMPARATOR);
                return outProcs;
            }
        }
    }

    public void dumpCheckinLocked(PrintWriter pw, String reqPackage) {
        int iu;
        int iv;
        int NPROCS;
        int ip;
        int key;
        PrintWriter printWriter = pw;
        String str = reqPackage;
        long now = SystemClock.uptimeMillis();
        ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap = this.mPackages.getMap();
        printWriter.println("vers,5");
        printWriter.print("period,");
        printWriter.print(this.mTimePeriodStartClockStr);
        printWriter.print(",");
        printWriter.print(this.mTimePeriodStartRealtime);
        printWriter.print(",");
        printWriter.print(this.mRunning ? SystemClock.elapsedRealtime() : this.mTimePeriodEndRealtime);
        boolean partial = true;
        if ((this.mFlags & 2) != 0) {
            printWriter.print(",shutdown");
            partial = false;
        }
        if ((this.mFlags & 4) != 0) {
            printWriter.print(",sysprops");
            partial = false;
        }
        if ((this.mFlags & 1) != 0) {
            printWriter.print(",complete");
            partial = false;
        }
        if (partial) {
            printWriter.print(",partial");
        }
        if (this.mHasSwappedOutPss) {
            printWriter.print(",swapped-out-pss");
        }
        pw.println();
        printWriter.print("config,");
        printWriter.println(this.mRuntime);
        int ip2 = 0;
        while (true) {
            int ip3 = ip2;
            if (ip3 >= pkgMap.size()) {
                break;
            }
            String pkgName = (String) pkgMap.keyAt(ip3);
            if (str == null || str.equals(pkgName)) {
                SparseArray<LongSparseArray<PackageState>> uids = (SparseArray) pkgMap.valueAt(ip3);
                ip2 = 0;
                while (true) {
                    iu = ip2;
                    if (iu >= uids.size()) {
                        break;
                    }
                    ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap2;
                    SparseArray<LongSparseArray<PackageState>> uids2;
                    int ip4;
                    String pkgName2;
                    int uid = uids.keyAt(iu);
                    LongSparseArray<PackageState> vpkgs = (LongSparseArray) uids.valueAt(iu);
                    ip2 = 0;
                    while (true) {
                        iv = ip2;
                        if (iv >= vpkgs.size()) {
                            break;
                        }
                        int iproc;
                        int NSRVS;
                        int NPROCS2;
                        PackageState pkgState;
                        int iv2;
                        int iu2;
                        LongSparseArray<PackageState> vpkgs2;
                        long vers = vpkgs.keyAt(iv);
                        PackageState pkgState2 = (PackageState) vpkgs.valueAt(iv);
                        NPROCS = pkgState2.mProcesses.size();
                        ip2 = pkgState2.mServices.size();
                        int iproc2 = 0;
                        while (true) {
                            iproc = iproc2;
                            if (iproc >= NPROCS) {
                                break;
                            }
                            NSRVS = ip2;
                            NPROCS2 = NPROCS;
                            pkgMap2 = pkgMap;
                            pkgState = pkgState2;
                            iv2 = iv;
                            iu2 = iu;
                            vpkgs2 = vpkgs;
                            uids2 = uids;
                            ip4 = ip3;
                            pkgName2 = pkgName;
                            ((ProcessState) pkgState2.mProcesses.valueAt(iproc)).dumpPackageProcCheckin(printWriter, pkgName, uid, vers, (String) pkgState2.mProcesses.keyAt(iproc), now);
                            iproc2 = iproc + 1;
                            pkgName = pkgName2;
                            ip2 = NSRVS;
                            pkgState2 = pkgState;
                            ip3 = ip4;
                            NPROCS = NPROCS2;
                            pkgMap = pkgMap2;
                            iv = iv2;
                            iu = iu2;
                            vpkgs = vpkgs2;
                            uids = uids2;
                            NSRVS = reqPackage;
                        }
                        NSRVS = ip2;
                        NPROCS2 = NPROCS;
                        iv2 = iv;
                        iu2 = iu;
                        vpkgs2 = vpkgs;
                        uids2 = uids;
                        ip4 = ip3;
                        pkgName2 = pkgName;
                        pkgMap2 = pkgMap;
                        pkgState = pkgState2;
                        ip2 = 0;
                        while (true) {
                            iproc = ip2;
                            if (iproc >= NSRVS) {
                                break;
                            }
                            ((ServiceState) pkgState.mServices.valueAt(iproc)).dumpTimesCheckin(printWriter, pkgName2, uid, vers, DumpUtils.collapseString(pkgName2, (String) pkgState.mServices.keyAt(iproc)), now);
                            ip2 = iproc + 1;
                        }
                        ip2 = iv2 + 1;
                        pkgName = pkgName2;
                        ip3 = ip4;
                        pkgMap = pkgMap2;
                        iu = iu2;
                        vpkgs = vpkgs2;
                        uids = uids2;
                        str = reqPackage;
                    }
                    uids2 = uids;
                    ip4 = ip3;
                    pkgName2 = pkgName;
                    pkgMap2 = pkgMap;
                    ip2 = iu + 1;
                    str = reqPackage;
                }
            }
            ip2 = ip3 + 1;
            pkgMap = pkgMap;
            str = reqPackage;
        }
        ArrayMap<String, SparseArray<ProcessState>> procMap = this.mProcesses.getMap();
        ip2 = 0;
        while (true) {
            ip = ip2;
            if (ip >= procMap.size()) {
                break;
            }
            String procName = (String) procMap.keyAt(ip);
            SparseArray<ProcessState> uids3 = (SparseArray) procMap.valueAt(ip);
            ip2 = 0;
            while (true) {
                int iu3 = ip2;
                if (iu3 >= uids3.size()) {
                    break;
                }
                ((ProcessState) uids3.valueAt(iu3)).dumpProcCheckin(printWriter, procName, uids3.keyAt(iu3), now);
                ip2 = iu3 + 1;
            }
            ip2 = ip + 1;
        }
        printWriter.print("total");
        DumpUtils.dumpAdjTimesCheckin(printWriter, ",", this.mMemFactorDurations, this.mMemFactor, this.mStartTime, now);
        pw.println();
        ip2 = this.mSysMemUsage.getKeyCount();
        if (ip2 > 0) {
            printWriter.print("sysmemusage");
            for (NPROCS = 0; NPROCS < ip2; NPROCS++) {
                key = this.mSysMemUsage.getKeyAt(NPROCS);
                iv = SparseMappingTable.getIdFromKey(key);
                printWriter.print(",");
                DumpUtils.printProcStateTag(printWriter, iv);
                for (iu = 0; iu < 16; iu++) {
                    if (iu > 1) {
                        printWriter.print(":");
                    }
                    printWriter.print(this.mSysMemUsage.getValue(key, iu));
                }
            }
        }
        pw.println();
        TotalMemoryUseCollection totalMem = new TotalMemoryUseCollection(ALL_SCREEN_ADJ, ALL_MEM_ADJ);
        computeTotalMemoryUse(totalMem, now);
        printWriter.print("weights,");
        printWriter.print(totalMem.totalTime);
        printWriter.print(",");
        printWriter.print(totalMem.sysMemCachedWeight);
        printWriter.print(":");
        printWriter.print(totalMem.sysMemSamples);
        printWriter.print(",");
        printWriter.print(totalMem.sysMemFreeWeight);
        printWriter.print(":");
        printWriter.print(totalMem.sysMemSamples);
        printWriter.print(",");
        printWriter.print(totalMem.sysMemZRamWeight);
        printWriter.print(":");
        printWriter.print(totalMem.sysMemSamples);
        printWriter.print(",");
        printWriter.print(totalMem.sysMemKernelWeight);
        printWriter.print(":");
        printWriter.print(totalMem.sysMemSamples);
        printWriter.print(",");
        printWriter.print(totalMem.sysMemNativeWeight);
        printWriter.print(":");
        printWriter.print(totalMem.sysMemSamples);
        for (key = 0; key < 14; key++) {
            printWriter.print(",");
            printWriter.print(totalMem.processStateWeight[key]);
            printWriter.print(":");
            printWriter.print(totalMem.processStateSamples[key]);
        }
        pw.println();
        key = this.mPageTypeLabels.size();
        for (iv = 0; iv < key; iv++) {
            printWriter.print("availablepages,");
            printWriter.print((String) this.mPageTypeLabels.get(iv));
            printWriter.print(",");
            printWriter.print(this.mPageTypeZones.get(iv));
            printWriter.print(",");
            int[] sizes = (int[]) this.mPageTypeSizes.get(iv);
            int N = sizes == null ? 0 : sizes.length;
            for (ip = 0; ip < N; ip++) {
                if (ip != 0) {
                    printWriter.print(",");
                }
                printWriter.print(sizes[ip]);
            }
            pw.println();
        }
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, long now) {
        ProtoOutputStream protoOutputStream = proto;
        ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pkgMap = this.mPackages.getMap();
        long token = proto.start(fieldId);
        protoOutputStream.write(1112396529665L, this.mTimePeriodStartRealtime);
        protoOutputStream.write(1112396529666L, this.mRunning ? SystemClock.elapsedRealtime() : this.mTimePeriodEndRealtime);
        protoOutputStream.write(1112396529667L, this.mTimePeriodStartUptime);
        protoOutputStream.write(1112396529668L, this.mTimePeriodEndUptime);
        protoOutputStream.write(1138166333445L, this.mRuntime);
        protoOutputStream.write(1133871366150L, this.mHasSwappedOutPss);
        boolean partial = true;
        if ((this.mFlags & 2) != 0) {
            protoOutputStream.write(JobStatusDumpProto.REQUIRED_CONSTRAINTS, 3);
            partial = false;
        }
        if ((this.mFlags & 4) != 0) {
            protoOutputStream.write(JobStatusDumpProto.REQUIRED_CONSTRAINTS, 4);
            partial = false;
        }
        if ((this.mFlags & 1) != 0) {
            protoOutputStream.write(JobStatusDumpProto.REQUIRED_CONSTRAINTS, 1);
            partial = false;
        }
        if (partial) {
            protoOutputStream.write(JobStatusDumpProto.REQUIRED_CONSTRAINTS, 2);
        }
        ArrayMap<String, SparseArray<ProcessState>> procMap = this.mProcesses.getMap();
        int ip = 0;
        while (true) {
            int ip2 = ip;
            if (ip2 < procMap.size()) {
                String procName = (String) procMap.keyAt(ip2);
                SparseArray<ProcessState> uids = (SparseArray) procMap.valueAt(ip2);
                ip = 0;
                while (true) {
                    int iu = ip;
                    if (iu >= uids.size()) {
                        break;
                    }
                    int iu2 = iu;
                    int ip3 = ip2;
                    SparseArray<ProcessState> uids2 = uids;
                    ((ProcessState) uids.valueAt(iu)).writeToProto(protoOutputStream, 2246267895816L, procName, uids.keyAt(iu), now);
                    ip = iu2 + 1;
                    ip2 = ip3;
                    uids = uids2;
                }
                ip = ip2 + 1;
            } else {
                protoOutputStream.end(token);
                return;
            }
        }
    }
}
