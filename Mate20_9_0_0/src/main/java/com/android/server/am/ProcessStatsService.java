package com.android.server.am;

import android.os.Binder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.procstats.DumpUtils;
import com.android.internal.app.procstats.IProcessStats.Stub;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.app.procstats.ProcessStats.PackageState;
import com.android.internal.app.procstats.ServiceState;
import com.android.internal.os.BackgroundThread;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public final class ProcessStatsService extends Stub {
    static final boolean DEBUG = false;
    static final int MAX_HISTORIC_STATES = 8;
    static final String STATE_FILE_CHECKIN_SUFFIX = ".ci";
    static final String STATE_FILE_PREFIX = "state-";
    static final String STATE_FILE_SUFFIX = ".bin";
    static final String TAG = "ProcessStatsService";
    static long WRITE_PERIOD = 1800000;
    final ActivityManagerService mAm;
    final File mBaseDir;
    boolean mCommitPending;
    AtomicFile mFile;
    @GuardedBy("mAm")
    Boolean mInjectedScreenState;
    int mLastMemOnlyState = -1;
    long mLastWriteTime;
    boolean mMemFactorLowered;
    Parcel mPendingWrite;
    boolean mPendingWriteCommitted;
    AtomicFile mPendingWriteFile;
    final Object mPendingWriteLock = new Object();
    ProcessStats mProcessStats;
    boolean mShuttingDown;
    final ReentrantLock mWriteLock = new ReentrantLock();

    public ProcessStatsService(ActivityManagerService am, File file) {
        this.mAm = am;
        this.mBaseDir = file;
        this.mBaseDir.mkdirs();
        this.mProcessStats = new ProcessStats(true);
        updateFile();
        SystemProperties.addChangeCallback(new Runnable() {
            public void run() {
                synchronized (ProcessStatsService.this.mAm) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (ProcessStatsService.this.mProcessStats.evaluateSystemProperties(false)) {
                            ProcessStats processStats = ProcessStatsService.this.mProcessStats;
                            processStats.mFlags |= 4;
                            ProcessStatsService.this.writeStateLocked(true, true);
                            ProcessStatsService.this.mProcessStats.evaluateSystemProperties(true);
                        }
                    } finally {
                        while (true) {
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                }
            }
        });
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Process Stats Crash", e);
            }
            throw e;
        }
    }

    public ProcessState getProcessStateLocked(String packageName, int uid, long versionCode, String processName) {
        return this.mProcessStats.getProcessStateLocked(packageName, uid, versionCode, processName);
    }

    public ServiceState getServiceStateLocked(String packageName, int uid, long versionCode, String processName, String className) {
        return this.mProcessStats.getServiceStateLocked(packageName, uid, versionCode, processName, className);
    }

    public boolean isMemFactorLowered() {
        return this.mMemFactorLowered;
    }

    @GuardedBy("mAm")
    public boolean setMemFactorLocked(int memFactor, boolean screenOn, long now) {
        this.mMemFactorLowered = memFactor < this.mLastMemOnlyState;
        this.mLastMemOnlyState = memFactor;
        if (this.mInjectedScreenState != null) {
            screenOn = this.mInjectedScreenState.booleanValue();
        }
        if (screenOn) {
            memFactor += 4;
        }
        if (memFactor == this.mProcessStats.mMemFactor) {
            return false;
        }
        int i;
        if (this.mProcessStats.mMemFactor != -1) {
            long[] jArr = this.mProcessStats.mMemFactorDurations;
            i = this.mProcessStats.mMemFactor;
            jArr[i] = jArr[i] + (now - this.mProcessStats.mStartTime);
        }
        this.mProcessStats.mMemFactor = memFactor;
        this.mProcessStats.mStartTime = now;
        ArrayMap<String, SparseArray<LongSparseArray<PackageState>>> pmap = this.mProcessStats.mPackages.getMap();
        for (i = pmap.size() - 1; i >= 0; i--) {
            SparseArray<LongSparseArray<PackageState>> uids = (SparseArray) pmap.valueAt(i);
            for (int iuid = uids.size() - 1; iuid >= 0; iuid--) {
                LongSparseArray<PackageState> vers = (LongSparseArray) uids.valueAt(iuid);
                for (int iver = vers.size() - 1; iver >= 0; iver--) {
                    ArrayMap<String, ServiceState> services = ((PackageState) vers.valueAt(iver)).mServices;
                    for (int isvc = services.size() - 1; isvc >= 0; isvc--) {
                        ((ServiceState) services.valueAt(isvc)).setMemFactor(memFactor, now);
                    }
                }
            }
        }
        return true;
    }

    public int getMemFactorLocked() {
        return this.mProcessStats.mMemFactor != -1 ? this.mProcessStats.mMemFactor : 0;
    }

    public void addSysMemUsageLocked(long cachedMem, long freeMem, long zramMem, long kernelMem, long nativeMem) {
        this.mProcessStats.addSysMemUsage(cachedMem, freeMem, zramMem, kernelMem, nativeMem);
    }

    public boolean shouldWriteNowLocked(long now) {
        if (now <= this.mLastWriteTime + WRITE_PERIOD) {
            return false;
        }
        if (SystemClock.elapsedRealtime() > this.mProcessStats.mTimePeriodStartRealtime + ProcessStats.COMMIT_PERIOD && SystemClock.uptimeMillis() > this.mProcessStats.mTimePeriodStartUptime + ProcessStats.COMMIT_UPTIME_PERIOD) {
            this.mCommitPending = true;
        }
        return true;
    }

    public void shutdownLocked() {
        Slog.w(TAG, "Writing process stats before shutdown...");
        ProcessStats processStats = this.mProcessStats;
        processStats.mFlags |= 2;
        writeStateSyncLocked();
        this.mShuttingDown = true;
    }

    public void writeStateAsyncLocked() {
        writeStateLocked(false);
    }

    public void writeStateSyncLocked() {
        writeStateLocked(true);
    }

    private void writeStateLocked(boolean sync) {
        if (!this.mShuttingDown) {
            boolean commitPending = this.mCommitPending;
            this.mCommitPending = false;
            writeStateLocked(sync, commitPending);
        }
    }

    public void writeStateLocked(boolean sync, boolean commit) {
        synchronized (this.mPendingWriteLock) {
            long now = SystemClock.uptimeMillis();
            if (this.mPendingWrite == null || !this.mPendingWriteCommitted) {
                this.mPendingWrite = Parcel.obtain();
                this.mProcessStats.mTimePeriodEndRealtime = SystemClock.elapsedRealtime();
                this.mProcessStats.mTimePeriodEndUptime = now;
                if (commit) {
                    ProcessStats processStats = this.mProcessStats;
                    processStats.mFlags |= 1;
                }
                this.mProcessStats.writeToParcel(this.mPendingWrite, 0);
                this.mPendingWriteFile = new AtomicFile(this.mFile.getBaseFile());
                this.mPendingWriteCommitted = commit;
            }
            if (commit) {
                this.mProcessStats.resetSafely();
                updateFile();
                this.mAm.requestPssAllProcsLocked(SystemClock.uptimeMillis(), true, false);
            }
            this.mLastWriteTime = SystemClock.uptimeMillis();
            final long totalTime = SystemClock.uptimeMillis() - now;
            if (sync) {
                performWriteState(totalTime);
                return;
            }
            BackgroundThread.getHandler().post(new Runnable() {
                public void run() {
                    ProcessStatsService.this.performWriteState(totalTime);
                }
            });
        }
    }

    private void updateFile() {
        File file = this.mBaseDir;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(STATE_FILE_PREFIX);
        stringBuilder.append(this.mProcessStats.mTimePeriodStartClockStr);
        stringBuilder.append(STATE_FILE_SUFFIX);
        this.mFile = new AtomicFile(new File(file, stringBuilder.toString()));
        this.mLastWriteTime = SystemClock.uptimeMillis();
    }

    /* JADX WARNING: Missing block: B:9:0x0019, code:
            r4 = android.os.SystemClock.uptimeMillis();
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:11:?, code:
            r0 = r2.startWrite();
            r0.write(r1.marshall());
            r0.flush();
            r2.finishWrite(r0);
            com.android.internal.logging.EventLogTags.writeCommitSysConfigFile("procstats", (android.os.SystemClock.uptimeMillis() - r4) + r9);
     */
    /* JADX WARNING: Missing block: B:13:0x003f, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:15:?, code:
            android.util.Slog.w(TAG, "Error writing process statistics", r3);
            r2.failWrite(r0);
     */
    /* JADX WARNING: Missing block: B:18:0x0057, code:
            r1.recycle();
            trimHistoricStatesWriteLocked();
            r8.mWriteLock.unlock();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void performWriteState(long initialTime) {
        Parcel data;
        synchronized (this.mPendingWriteLock) {
            data = this.mPendingWrite;
            AtomicFile file = this.mPendingWriteFile;
            this.mPendingWriteCommitted = false;
            if (data == null) {
                return;
            }
            this.mPendingWrite = null;
            this.mPendingWriteFile = null;
            this.mWriteLock.lock();
        }
        data.recycle();
        trimHistoricStatesWriteLocked();
        this.mWriteLock.unlock();
    }

    boolean readLocked(ProcessStats stats, AtomicFile file) {
        try {
            FileInputStream stream = file.openRead();
            stats.read(stream);
            stream.close();
            if (stats.mReadError == null) {
                return true;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring existing stats; ");
            stringBuilder.append(stats.mReadError);
            Slog.w(str, stringBuilder.toString());
            return false;
        } catch (Throwable e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("caught exception: ");
            stringBuilder2.append(e);
            stats.mReadError = stringBuilder2.toString();
            Slog.e(TAG, "Error reading process statistics", e);
            return false;
        }
    }

    private ArrayList<String> getCommittedFiles(int minNum, boolean inclCurrent, boolean inclCheckedIn) {
        File[] files = this.mBaseDir.listFiles();
        if (files == null || files.length <= minNum) {
            return null;
        }
        ArrayList<String> filesArray = new ArrayList(files.length);
        String currentFile = this.mFile.getBaseFile().getPath();
        for (File file : files) {
            String fileStr = file.getPath();
            if ((inclCheckedIn || !fileStr.endsWith(STATE_FILE_CHECKIN_SUFFIX)) && (inclCurrent || !fileStr.equals(currentFile))) {
                filesArray.add(fileStr);
            }
        }
        Collections.sort(filesArray);
        return filesArray;
    }

    public void trimHistoricStatesWriteLocked() {
        ArrayList<String> filesArray = getCommittedFiles(8, false, true);
        if (filesArray != null) {
            while (filesArray.size() > 8) {
                String file = (String) filesArray.remove(0);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Pruning old procstats: ");
                stringBuilder.append(file);
                Slog.i(str, stringBuilder.toString());
                new File(file).delete();
            }
        }
    }

    boolean dumpFilteredProcessesCsvLocked(PrintWriter pw, String header, boolean sepScreenStates, int[] screenStates, boolean sepMemStates, int[] memStates, boolean sepProcStates, int[] procStates, long now, String reqPackage) {
        ArrayList<ProcessState> procs = this.mProcessStats.collectProcessesLocked(screenStates, memStates, procStates, procStates, now, reqPackage, false);
        if (procs.size() <= 0) {
            return false;
        }
        if (header != null) {
            pw.println(header);
        }
        DumpUtils.dumpProcessListCsv(pw, procs, sepScreenStates, screenStates, sepMemStates, memStates, sepProcStates, procStates, now);
        return true;
    }

    static int[] parseStateList(String[] states, int mult, String arg, boolean[] outSep, String[] outError) {
        ArrayList<Integer> res = new ArrayList();
        int i = 0;
        int lastPos = 0;
        int i2 = 0;
        while (i2 <= arg.length()) {
            char c = i2 < arg.length() ? arg.charAt(i2) : 0;
            if (c == ',' || c == '+' || c == ' ' || c == 0) {
                boolean isSep = c == ',';
                if (lastPos == 0) {
                    outSep[0] = isSep;
                } else if (!(c == 0 || outSep[0] == isSep)) {
                    outError[0] = "inconsistent separators (can't mix ',' with '+')";
                    return null;
                }
                if (lastPos < i2 - 1) {
                    String str = arg.substring(lastPos, i2);
                    for (int j = 0; j < states.length; j++) {
                        if (str.equals(states[j])) {
                            res.add(Integer.valueOf(j));
                            str = null;
                            break;
                        }
                    }
                    if (str != null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("invalid word \"");
                        stringBuilder.append(str);
                        stringBuilder.append("\"");
                        outError[0] = stringBuilder.toString();
                        return null;
                    }
                }
                lastPos = i2 + 1;
            }
            i2++;
        }
        int[] finalRes = new int[res.size()];
        while (i < res.size()) {
            finalRes[i] = ((Integer) res.get(i)).intValue() * mult;
            i++;
        }
        return finalRes;
    }

    public byte[] getCurrentStats(List<ParcelFileDescriptor> historic) {
        this.mAm.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", null);
        Parcel current = Parcel.obtain();
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long now = SystemClock.uptimeMillis();
                this.mProcessStats.mTimePeriodEndRealtime = SystemClock.elapsedRealtime();
                this.mProcessStats.mTimePeriodEndUptime = now;
                this.mProcessStats.writeToParcel(current, now, 0);
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        this.mWriteLock.lock();
        if (historic != null) {
            ArrayList<String> files;
            int i;
            try {
                files = getCommittedFiles(0, false, true);
                if (files != null) {
                    int i2 = files.size() - 1;
                    while (true) {
                        i = i2;
                        if (i < 0) {
                            break;
                        }
                        historic.add(ParcelFileDescriptor.open(new File((String) files.get(i)), 268435456));
                        i2 = i - 1;
                    }
                }
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failure opening procstat file ");
                stringBuilder.append((String) files.get(i));
                Slog.w(str, stringBuilder.toString(), e);
            } catch (Throwable th) {
                this.mWriteLock.unlock();
            }
        }
        this.mWriteLock.unlock();
        return current.marshall();
    }

    public ParcelFileDescriptor getStatsOverTime(long minTime) {
        boolean z;
        long curTime;
        IOException e;
        Throwable th;
        final byte[] outData;
        final ParcelFileDescriptor[] fds;
        ParcelFileDescriptor parcelFileDescriptor;
        this.mAm.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", null);
        Parcel current = Parcel.obtain();
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long now = SystemClock.uptimeMillis();
                this.mProcessStats.mTimePeriodEndRealtime = SystemClock.elapsedRealtime();
                this.mProcessStats.mTimePeriodEndUptime = now;
                z = false;
                this.mProcessStats.writeToParcel(current, now, 0);
                curTime = this.mProcessStats.mTimePeriodEndRealtime - this.mProcessStats.mTimePeriodStartRealtime;
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        this.mWriteLock.lock();
        if (curTime < minTime) {
            long curTime2;
            try {
                ArrayList<String> files = getCommittedFiles(0, false, true);
                if (files != null && files.size() > 0) {
                    current.setDataPosition(0);
                    ProcessStats stats = (ProcessStats) ProcessStats.CREATOR.createFromParcel(current);
                    current.recycle();
                    int i = files.size() - 1;
                    while (true) {
                        int i2 = i;
                        if (i2 < 0 || stats.mTimePeriodEndRealtime - stats.mTimePeriodStartRealtime >= minTime) {
                            current = Parcel.obtain();
                            stats.writeToParcel(current, 0);
                        } else {
                            AtomicFile file = new AtomicFile(new File((String) files.get(i2)));
                            i2--;
                            ProcessStats moreStats = new ProcessStats(z);
                            readLocked(moreStats, file);
                            if (moreStats.mReadError == null) {
                                stats.add(moreStats);
                                StringBuilder sb = new StringBuilder();
                                sb.append("Added stats: ");
                                sb.append(moreStats.mTimePeriodStartClockStr);
                                sb.append(", over ");
                                curTime2 = curTime;
                                try {
                                    TimeUtils.formatDuration(moreStats.mTimePeriodEndRealtime - moreStats.mTimePeriodStartRealtime, sb);
                                    Slog.i(TAG, sb.toString());
                                } catch (IOException e2) {
                                    e = e2;
                                    try {
                                        Slog.w(TAG, "Failed building output pipe", e);
                                        this.mWriteLock.unlock();
                                        return null;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        this.mWriteLock.unlock();
                                        throw th;
                                    }
                                }
                            }
                            curTime2 = curTime;
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Failure reading ");
                            stringBuilder.append((String) files.get(i2 + 1));
                            stringBuilder.append("; ");
                            stringBuilder.append(moreStats.mReadError);
                            Slog.w(str, stringBuilder.toString());
                            i = i2;
                            curTime = curTime2;
                            z = false;
                        }
                    }
                    current = Parcel.obtain();
                    stats.writeToParcel(current, 0);
                    outData = current.marshall();
                    current.recycle();
                    fds = ParcelFileDescriptor.createPipe();
                    new Thread("ProcessStats pipe output") {
                        public void run() {
                            FileOutputStream fout = new AutoCloseOutputStream(fds[1]);
                            try {
                                fout.write(outData);
                                fout.close();
                            } catch (IOException e) {
                                Slog.w(ProcessStatsService.TAG, "Failure writing pipe", e);
                            }
                        }
                    }.start();
                    parcelFileDescriptor = fds[0];
                    this.mWriteLock.unlock();
                    return parcelFileDescriptor;
                }
            } catch (IOException e3) {
                e = e3;
                curTime2 = curTime;
                Slog.w(TAG, "Failed building output pipe", e);
                this.mWriteLock.unlock();
                return null;
            } catch (Throwable th3) {
                th = th3;
                curTime2 = curTime;
                this.mWriteLock.unlock();
                throw th;
            }
        }
        outData = current.marshall();
        current.recycle();
        fds = ParcelFileDescriptor.createPipe();
        /* anonymous class already generated */.start();
        parcelFileDescriptor = fds[0];
        this.mWriteLock.unlock();
        return parcelFileDescriptor;
    }

    public int getCurrentMemoryState() {
        int i;
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                i = this.mLastMemOnlyState;
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        return i;
    }

    private void dumpAggregatedStats(PrintWriter pw, long aggregateHours, long now, String reqPackage, boolean isCompact, boolean dumpDetails, boolean dumpFullDetails, boolean dumpAll, boolean activeOnly) {
        PrintWriter printWriter = pw;
        ParcelFileDescriptor pfd = getStatsOverTime((((aggregateHours * 60) * 60) * 1000) - (ProcessStats.COMMIT_PERIOD / 2));
        if (pfd == null) {
            printWriter.println("Unable to build stats!");
            return;
        }
        ProcessStats stats = new ProcessStats(false);
        stats.read(new AutoCloseInputStream(pfd));
        if (stats.mReadError != null) {
            printWriter.print("Failure reading: ");
            printWriter.println(stats.mReadError);
            return;
        }
        if (isCompact) {
            stats.dumpCheckinLocked(printWriter, reqPackage);
        } else {
            String str = reqPackage;
            if (dumpDetails || dumpFullDetails) {
                stats.dumpLocked(printWriter, str, now, dumpFullDetails ^ 1, dumpAll, activeOnly);
            } else {
                stats.dumpSummaryLocked(printWriter, str, now, activeOnly);
            }
        }
    }

    private static void dumpHelp(PrintWriter pw) {
        pw.println("Process stats (procstats) dump options:");
        pw.println("    [--checkin|-c|--csv] [--csv-screen] [--csv-proc] [--csv-mem]");
        pw.println("    [--details] [--full-details] [--current] [--hours N] [--last N]");
        pw.println("    [--max N] --active] [--commit] [--reset] [--clear] [--write] [-h]");
        pw.println("    [--start-testing] [--stop-testing] ");
        pw.println("    [--pretend-screen-on] [--pretend-screen-off] [--stop-pretend-screen]");
        pw.println("    [<package.name>]");
        pw.println("  --checkin: perform a checkin: print and delete old committed states.");
        pw.println("  -c: print only state in checkin format.");
        pw.println("  --csv: output data suitable for putting in a spreadsheet.");
        pw.println("  --csv-screen: on, off.");
        pw.println("  --csv-mem: norm, mod, low, crit.");
        pw.println("  --csv-proc: pers, top, fore, vis, precept, backup,");
        pw.println("    service, home, prev, cached");
        pw.println("  --details: dump per-package details, not just summary.");
        pw.println("  --full-details: dump all timing and active state details.");
        pw.println("  --current: only dump current state.");
        pw.println("  --hours: aggregate over about N last hours.");
        pw.println("  --last: only show the last committed stats at index N (starting at 1).");
        pw.println("  --max: for -a, max num of historical batches to print.");
        pw.println("  --active: only show currently active processes/services.");
        pw.println("  --commit: commit current stats to disk and reset to start new stats.");
        pw.println("  --reset: reset current stats, without committing.");
        pw.println("  --clear: clear all stats; does both --reset and deletes old stats.");
        pw.println("  --write: write current in-memory stats to disk.");
        pw.println("  --read: replace current stats with last-written stats.");
        pw.println("  --start-testing: clear all stats and starting high frequency pss sampling.");
        pw.println("  --stop-testing: stop high frequency pss sampling.");
        pw.println("  --pretend-screen-on: pretend screen is on.");
        pw.println("  --pretend-screen-off: pretend screen is off.");
        pw.println("  --stop-pretend-screen: forget \"pretend screen\" and use the real state.");
        pw.println("  -a: print everything.");
        pw.println("  -h: print this help text.");
        pw.println("  <package.name>: optional name of package to filter output by.");
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (com.android.internal.util.DumpUtils.checkDumpAndUsageStatsPermission(this.mAm.mContext, TAG, pw)) {
            long ident = Binder.clearCallingIdentity();
            try {
                if (args.length <= 0 || !PriorityDump.PROTO_ARG.equals(args[0])) {
                    dumpInner(pw, args);
                } else {
                    dumpProto(fd);
                }
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:515:0x07d2 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:383:0x0794 A:{Catch:{ Throwable -> 0x07b4 }} */
    /* JADX WARNING: Missing block: B:120:0x02c3, code:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:430:0x084f, code:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:431:0x0852, code:
            if (r33 != false) goto L_0x0883;
     */
    /* JADX WARNING: Missing block: B:432:0x0854, code:
            if (r29 == false) goto L_0x0859;
     */
    /* JADX WARNING: Missing block: B:433:0x0856, code:
            r44.println();
     */
    /* JADX WARNING: Missing block: B:434:0x0859, code:
            r14.println("AGGREGATED OVER LAST 24 HOURS:");
            r1 = r13;
            r2 = r14;
            r5 = r16;
            r7 = r28;
            r8 = r31;
            r9 = r34;
            r10 = r18;
            r11 = r23;
            r35 = r12;
            r12 = r22;
            dumpAggregatedStats(r2, 24, r5, r7, r8, r9, r10, r11, r12);
            r44.println();
            r14.println("AGGREGATED OVER LAST 3 HOURS:");
            dumpAggregatedStats(r2, 3, r5, r7, r8, r9, r10, r11, r12);
     */
    /* JADX WARNING: Missing block: B:435:0x0883, code:
            r35 = r12;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void dumpInner(PrintWriter pw, String[] args) {
        boolean csvSepProcStats;
        boolean csvSepMemStats;
        boolean activeOnly;
        int maxNum;
        boolean isCompact;
        int i;
        int i2;
        boolean isCompact2;
        boolean isCsv;
        boolean dumpDetails;
        int lastIndex;
        int[] csvScreenStats;
        boolean activeOnly2;
        int[] csvProcStats;
        boolean csvSepMemStats2;
        int maxNum2;
        int[] csvMemStats;
        int[] csvMemStats2;
        Throwable th;
        ArrayList<String> files;
        int[] iArr;
        int[] iArr2;
        String str;
        boolean z;
        String reqPackage;
        PrintWriter printWriter = pw;
        String[] strArr = args;
        long now = SystemClock.uptimeMillis();
        int aggregateHours = 0;
        String reqPackage2 = null;
        boolean isCheckin = false;
        boolean quit = false;
        int[] csvScreenStats2 = new int[]{0, 4};
        int[] csvMemStats3 = new int[1];
        boolean dumpAll = false;
        csvMemStats3[0] = 3;
        int[] csvProcStats2 = ProcessStats.ALL_PROC_STATES;
        boolean currentOnly;
        if (strArr != null) {
            csvSepProcStats = true;
            csvSepMemStats = false;
            boolean csvSepScreenStats = false;
            activeOnly = false;
            maxNum = 2;
            int lastIndex2 = 0;
            int aggregateHours2 = 0;
            boolean dumpFullDetails = false;
            boolean dumpFullDetails2 = false;
            boolean dumpDetails2 = false;
            boolean currentOnly2 = false;
            boolean isCsv2 = false;
            isCompact = csvMemStats3;
            i = 0;
            loop16:
            while (true) {
                i2 = i;
                if (i2 >= strArr.length) {
                    isCompact2 = isCsv2;
                    isCsv = currentOnly2;
                    currentOnly = dumpDetails2;
                    dumpDetails = dumpFullDetails2;
                    lastIndex = lastIndex2;
                    csvScreenStats = csvScreenStats2;
                    activeOnly2 = activeOnly;
                    activeOnly = dumpFullDetails;
                    aggregateHours = aggregateHours2;
                    csvProcStats = csvProcStats2;
                    csvSepMemStats2 = csvSepMemStats;
                    csvSepMemStats = csvSepScreenStats;
                    maxNum2 = maxNum;
                    csvMemStats = isCompact;
                    break loop16;
                }
                String arg = strArr[i2];
                if ("--checkin".equals(arg)) {
                    isCheckin = true;
                } else if ("-c".equals(arg)) {
                    isCsv2 = true;
                } else if ("--csv".equals(arg)) {
                    currentOnly2 = true;
                } else {
                    if ("--csv-screen".equals(arg)) {
                        i = i2 + 1;
                        if (i >= strArr.length) {
                            printWriter.println("Error: argument required for --csv-screen");
                            dumpHelp(pw);
                            return;
                        }
                        csvMemStats2 = isCompact;
                        isCompact = new boolean[1];
                        isCompact2 = isCsv2;
                        isCsv2 = new String[1];
                        isCsv = currentOnly2;
                        currentOnly = dumpDetails2;
                        currentOnly2 = parseStateList(DumpUtils.ADJ_SCREEN_NAMES_CSV, true, strArr[i], isCompact, isCsv2);
                        if (currentOnly2) {
                            i2 = i;
                            csvSepScreenStats = isCompact[0];
                            csvScreenStats2 = currentOnly2;
                        } else {
                            dumpDetails2 = new StringBuilder();
                            dumpDetails2.append("Error in \"");
                            dumpDetails2.append(strArr[i]);
                            dumpDetails2.append("\": ");
                            dumpDetails2.append(isCsv2[0]);
                            printWriter.println(dumpDetails2.toString());
                            dumpHelp(pw);
                            return;
                        }
                    }
                    csvMemStats2 = isCompact;
                    isCompact2 = isCsv2;
                    isCsv = currentOnly2;
                    currentOnly = dumpDetails2;
                    boolean i3;
                    boolean i4;
                    if ("--csv-mem".equals(arg)) {
                        i3 = i2 + 1;
                        if (i3 >= strArr.length) {
                            printWriter.println("Error: argument required for --csv-mem");
                            dumpHelp(pw);
                            return;
                        }
                        isCsv2 = new boolean[1];
                        currentOnly2 = new String[1];
                        dumpDetails2 = parseStateList(DumpUtils.ADJ_MEM_NAMES_CSV, 1, strArr[i3], isCsv2, currentOnly2);
                        if (dumpDetails2) {
                            i4 = i3;
                            csvSepMemStats = isCsv2[0];
                            isCompact = dumpDetails2;
                            isCsv2 = isCompact2;
                            currentOnly2 = isCsv;
                            dumpDetails2 = currentOnly;
                        } else {
                            isCompact = new StringBuilder();
                            isCompact.append("Error in \"");
                            isCompact.append(strArr[i3]);
                            isCompact.append("\": ");
                            isCompact.append(currentOnly2[0]);
                            printWriter.println(isCompact.toString());
                            dumpHelp(pw);
                            return;
                        }
                    } else if ("--csv-proc".equals(arg)) {
                        i3 = i2 + 1;
                        if (i3 >= strArr.length) {
                            printWriter.println("Error: argument required for --csv-proc");
                            dumpHelp(pw);
                            return;
                        }
                        isCsv2 = new boolean[1];
                        currentOnly2 = new String[1];
                        dumpDetails2 = parseStateList(DumpUtils.STATE_NAMES_CSV, 1, strArr[i3], isCsv2, currentOnly2);
                        if (dumpDetails2) {
                            i4 = i3;
                            csvSepProcStats = isCsv2[0];
                            csvProcStats2 = dumpDetails2;
                        } else {
                            isCompact = new StringBuilder();
                            isCompact.append("Error in \"");
                            isCompact.append(strArr[i3]);
                            isCompact.append("\": ");
                            isCompact.append(currentOnly2[0]);
                            printWriter.println(isCompact.toString());
                            dumpHelp(pw);
                            return;
                        }
                    } else if ("--details".equals(arg)) {
                        dumpFullDetails2 = true;
                    } else if ("--full-details".equals(arg)) {
                        dumpFullDetails = true;
                    } else {
                        if ("--hours".equals(arg)) {
                            isCompact = i2 + 1;
                            if (isCompact >= strArr.length) {
                                printWriter.println("Error: argument required for --hours");
                                dumpHelp(pw);
                                return;
                            }
                            try {
                                aggregateHours2 = Integer.parseInt(strArr[isCompact]);
                            } catch (NumberFormatException e) {
                                isCsv2 = new StringBuilder();
                                isCsv2.append("Error: --hours argument not an int -- ");
                                isCsv2.append(strArr[isCompact]);
                                printWriter.println(isCsv2.toString());
                                dumpHelp(pw);
                                return;
                            }
                        } else if ("--last".equals(arg)) {
                            isCompact = i2 + 1;
                            if (isCompact >= strArr.length) {
                                printWriter.println("Error: argument required for --last");
                                dumpHelp(pw);
                                return;
                            }
                            try {
                                lastIndex2 = Integer.parseInt(strArr[isCompact]);
                            } catch (NumberFormatException e2) {
                                isCsv2 = new StringBuilder();
                                isCsv2.append("Error: --last argument not an int -- ");
                                isCsv2.append(strArr[isCompact]);
                                printWriter.println(isCsv2.toString());
                                dumpHelp(pw);
                                return;
                            }
                        } else if ("--max".equals(arg)) {
                            isCompact = i2 + 1;
                            if (isCompact >= strArr.length) {
                                printWriter.println("Error: argument required for --max");
                                dumpHelp(pw);
                                return;
                            }
                            try {
                                maxNum = Integer.parseInt(strArr[isCompact]);
                            } catch (NumberFormatException e3) {
                                isCsv2 = new StringBuilder();
                                isCsv2.append("Error: --max argument not an int -- ");
                                isCsv2.append(strArr[isCompact]);
                                printWriter.println(isCsv2.toString());
                                dumpHelp(pw);
                                return;
                            }
                        } else {
                            if ("--active".equals(arg)) {
                                dumpDetails2 = true;
                                activeOnly = true;
                            } else if ("--current".equals(arg)) {
                                dumpDetails2 = true;
                            } else if ("--commit".equals(arg)) {
                                synchronized (this.mAm) {
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        ProcessStats processStats = this.mProcessStats;
                                        processStats.mFlags |= true;
                                        writeStateLocked(true, true);
                                        printWriter.println("Process stats committed.");
                                        quit = true;
                                    } finally {
                                        while (true) {
                                        }
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                    }
                                }
                            } else {
                                if ("--reset".equals(arg)) {
                                    synchronized (this.mAm) {
                                        try {
                                            ActivityManagerService.boostPriorityForLockedSection();
                                            this.mProcessStats.resetSafely();
                                            dumpDetails = dumpFullDetails2;
                                            try {
                                                this.mAm.requestPssAllProcsLocked(SystemClock.uptimeMillis(), true, false);
                                                printWriter.println("Process stats reset.");
                                                quit = true;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                                throw th;
                                            }
                                        } catch (Throwable th3) {
                                            th = th3;
                                            dumpDetails = dumpFullDetails2;
                                        }
                                    }
                                } else {
                                    dumpDetails = dumpFullDetails2;
                                    if ("--clear".equals(arg)) {
                                        synchronized (this.mAm) {
                                            try {
                                                ActivityManagerService.boostPriorityForLockedSection();
                                                this.mProcessStats.resetSafely();
                                                this.mAm.requestPssAllProcsLocked(SystemClock.uptimeMillis(), true, false);
                                                files = getCommittedFiles(0, true, true);
                                                if (files != null) {
                                                    for (isCsv2 = false; isCsv2 < files.size(); isCsv2++) {
                                                        new File((String) files.get(isCsv2)).delete();
                                                    }
                                                }
                                                printWriter.println("All process stats cleared.");
                                                quit = true;
                                            } finally {
                                                while (true) {
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                    } else if ("--write".equals(arg)) {
                                        synchronized (this.mAm) {
                                            try {
                                                ActivityManagerService.boostPriorityForLockedSection();
                                                writeStateSyncLocked();
                                                printWriter.println("Process stats written.");
                                                quit = true;
                                            } finally {
                                                while (true) {
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                    } else if ("--read".equals(arg)) {
                                        synchronized (this.mAm) {
                                            try {
                                                ActivityManagerService.boostPriorityForLockedSection();
                                                readLocked(this.mProcessStats, this.mFile);
                                                printWriter.println("Process stats read.");
                                                quit = true;
                                            } finally {
                                                while (true) {
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                    } else if ("--start-testing".equals(arg)) {
                                        synchronized (this.mAm) {
                                            try {
                                                ActivityManagerService.boostPriorityForLockedSection();
                                                this.mAm.setTestPssMode(true);
                                                printWriter.println("Started high frequency sampling.");
                                                quit = true;
                                            } finally {
                                                while (true) {
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                    } else if ("--stop-testing".equals(arg)) {
                                        synchronized (this.mAm) {
                                            try {
                                                ActivityManagerService.boostPriorityForLockedSection();
                                                this.mAm.setTestPssMode(false);
                                                printWriter.println("Stopped high frequency sampling.");
                                                quit = true;
                                            } finally {
                                                while (true) {
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                    } else if ("--pretend-screen-on".equals(arg)) {
                                        synchronized (this.mAm) {
                                            try {
                                                ActivityManagerService.boostPriorityForLockedSection();
                                                this.mInjectedScreenState = Boolean.valueOf(true);
                                            } finally {
                                                while (true) {
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                        quit = true;
                                    } else if ("--pretend-screen-off".equals(arg)) {
                                        synchronized (this.mAm) {
                                            try {
                                                ActivityManagerService.boostPriorityForLockedSection();
                                                this.mInjectedScreenState = Boolean.valueOf(false);
                                            } finally {
                                                while (true) {
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                        quit = true;
                                    } else if ("--stop-pretend-screen".equals(arg)) {
                                        synchronized (this.mAm) {
                                            try {
                                                ActivityManagerService.boostPriorityForLockedSection();
                                                this.mInjectedScreenState = null;
                                            } finally {
                                                while (true) {
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            }
                                        }
                                        quit = true;
                                    } else if ("-h".equals(arg)) {
                                        dumpHelp(pw);
                                        return;
                                    } else if ("-a".equals(arg)) {
                                        dumpFullDetails2 = true;
                                        dumpAll = true;
                                    } else if (arg.length() <= 0 || !arg.charAt(false)) {
                                        reqPackage2 = arg;
                                        dumpFullDetails2 = true;
                                    } else {
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Unknown option: ");
                                        stringBuilder.append(arg);
                                        printWriter.println(stringBuilder.toString());
                                        dumpHelp(pw);
                                        return;
                                    }
                                }
                                isCompact = csvMemStats2;
                                isCsv2 = isCompact2;
                                currentOnly2 = isCsv;
                                dumpDetails2 = currentOnly;
                                dumpFullDetails2 = dumpDetails;
                            }
                            isCompact = csvMemStats2;
                            isCsv2 = isCompact2;
                            currentOnly2 = isCsv;
                        }
                        i4 = isCompact;
                    }
                    isCompact = csvMemStats2;
                    isCsv2 = isCompact2;
                    currentOnly2 = isCsv;
                    dumpDetails2 = currentOnly;
                }
                i = i2 + 1;
            }
        } else {
            isCompact2 = false;
            isCsv = false;
            currentOnly = false;
            dumpDetails = false;
            lastIndex = 0;
            csvSepProcStats = true;
            csvProcStats = csvProcStats2;
            csvSepMemStats = false;
            csvSepMemStats2 = false;
            activeOnly = false;
            maxNum2 = 2;
            csvScreenStats = csvScreenStats2;
            activeOnly2 = false;
            csvMemStats = csvMemStats3;
        }
        if (!quit) {
            int i5;
            String str2;
            if (isCsv) {
                printWriter.print("Processes running summed over");
                if (!csvSepMemStats) {
                    for (int printScreenLabelCsv : csvScreenStats) {
                        printWriter.print(" ");
                        DumpUtils.printScreenLabelCsv(printWriter, printScreenLabelCsv);
                    }
                }
                if (!csvSepMemStats2) {
                    for (int printScreenLabelCsv2 : csvMemStats) {
                        printWriter.print(" ");
                        DumpUtils.printMemLabelCsv(printWriter, printScreenLabelCsv2);
                    }
                }
                if (!csvSepProcStats) {
                    int i6 = 0;
                    while (true) {
                        i = i6;
                        if (i >= csvProcStats.length) {
                            break;
                        }
                        printWriter.print(" ");
                        printWriter.print(DumpUtils.STATE_NAMES_CSV[csvProcStats[i]]);
                        i6 = i + 1;
                    }
                }
                pw.println();
                ActivityManagerService activityManagerService = this.mAm;
                synchronized (activityManagerService) {
                    ActivityManagerService activityManagerService2;
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        activityManagerService2 = activityManagerService;
                        try {
                            dumpFilteredProcessesCsvLocked(printWriter, null, csvSepMemStats, csvScreenStats, csvSepMemStats2, csvMemStats, csvSepProcStats, csvProcStats, now, reqPackage2);
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        } catch (Throwable th4) {
                            th = th4;
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        activityManagerService2 = activityManagerService;
                        i5 = lastIndex;
                        int i7 = aggregateHours;
                        csvMemStats2 = csvProcStats;
                        iArr = csvScreenStats;
                        iArr2 = csvMemStats;
                        str2 = reqPackage2;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            }
            i5 = lastIndex;
            csvMemStats2 = csvProcStats;
            iArr = csvScreenStats;
            iArr2 = csvMemStats;
            str2 = reqPackage2;
            int aggregateHours3 = aggregateHours;
            if (aggregateHours3 != 0) {
                printWriter.print("AGGREGATED OVER LAST ");
                printWriter.print(aggregateHours3);
                printWriter.println(" HOURS:");
                dumpAggregatedStats(printWriter, (long) aggregateHours3, now, str2, isCompact2, dumpDetails, activeOnly, dumpAll, activeOnly2);
                return;
            }
            aggregateHours3 = i5;
            boolean checkedIn;
            if (aggregateHours3 > 0) {
                printWriter.print("LAST STATS AT INDEX ");
                printWriter.print(aggregateHours3);
                printWriter.println(":");
                files = getCommittedFiles(0, false, true);
                if (aggregateHours3 >= files.size()) {
                    printWriter.print("Only have ");
                    printWriter.print(files.size());
                    printWriter.println(" data sets");
                    return;
                }
                AtomicFile file = new AtomicFile(new File((String) files.get(aggregateHours3)));
                ProcessStats processStats2 = new ProcessStats(false);
                readLocked(processStats2, file);
                if (processStats2.mReadError != null) {
                    if (isCheckin || isCompact2) {
                        printWriter.print("err,");
                    }
                    printWriter.print("Failure reading ");
                    printWriter.print((String) files.get(aggregateHours3));
                    printWriter.print("; ");
                    printWriter.println(processStats2.mReadError);
                    return;
                }
                checkedIn = file.getBaseFile().getPath().endsWith(STATE_FILE_CHECKIN_SUFFIX);
                if (isCheckin || isCompact2) {
                    processStats2.dumpCheckinLocked(printWriter, str2);
                } else {
                    printWriter.print("COMMITTED STATS FROM ");
                    printWriter.print(processStats2.mTimePeriodStartClockStr);
                    if (checkedIn) {
                        printWriter.print(" (checked in)");
                    }
                    printWriter.println(":");
                    if (dumpDetails || activeOnly) {
                        processStats2.dumpLocked(printWriter, str2, now, !activeOnly, dumpAll, activeOnly2);
                        if (dumpAll) {
                            printWriter.print("  mFile=");
                            printWriter.println(this.mFile.getBaseFile());
                        }
                    } else {
                        processStats2.dumpSummaryLocked(printWriter, str2, now, activeOnly2);
                    }
                }
                return;
            }
            boolean z2;
            boolean sepNeeded;
            String reqPackage3 = str2;
            boolean z3 = true;
            isCompact = false;
            if (dumpAll || isCheckin) {
                this.mWriteLock.lock();
                try {
                    ArrayList<String> files2 = getCommittedFiles(0, false, !isCheckin);
                    if (files2 != null) {
                        if (isCheckin) {
                            i2 = 0;
                        } else {
                            try {
                                i2 = files2.size() - maxNum2;
                            } catch (Throwable th6) {
                                th = th6;
                                str = reqPackage3;
                                i5 = aggregateHours3;
                            }
                        }
                        i = i2;
                        if (i < 0) {
                            i = 0;
                        }
                        i = i;
                        while (true) {
                            maxNum = i;
                            if (maxNum >= files2.size()) {
                                break;
                            }
                            try {
                                AtomicFile file2 = new AtomicFile(new File((String) files2.get(maxNum)));
                                try {
                                    ProcessStats processStats3 = new ProcessStats(false);
                                    readLocked(processStats3, file2);
                                    if (processStats3.mReadError != null) {
                                        if (isCheckin || isCompact2) {
                                            printWriter.print("err,");
                                        }
                                        printWriter.print("Failure reading ");
                                        printWriter.print((String) files2.get(maxNum));
                                        printWriter.print("; ");
                                        printWriter.println(processStats3.mReadError);
                                        new File((String) files2.get(maxNum)).delete();
                                        z2 = false;
                                        z = z3;
                                    } else {
                                        ProcessStats processStats4;
                                        String fileStr = file2.getBaseFile().getPath();
                                        boolean checkedIn2 = fileStr.endsWith(STATE_FILE_CHECKIN_SUFFIX);
                                        String fileStr2;
                                        if (isCheckin) {
                                            processStats4 = processStats3;
                                            fileStr2 = fileStr;
                                            z2 = false;
                                            z = z3;
                                            reqPackage = reqPackage3;
                                        } else if (isCompact2) {
                                            processStats4 = processStats3;
                                            fileStr2 = fileStr;
                                            z2 = false;
                                            z = z3;
                                            reqPackage = reqPackage3;
                                        } else {
                                            if (isCompact) {
                                                pw.println();
                                            } else {
                                                isCompact = true;
                                            }
                                            checkedIn = isCompact;
                                            try {
                                                printWriter.print("COMMITTED STATS FROM ");
                                                printWriter.print(processStats3.mTimePeriodStartClockStr);
                                                if (checkedIn2) {
                                                    try {
                                                        printWriter.print(" (checked in)");
                                                    } catch (Throwable th7) {
                                                        th = th7;
                                                        str = reqPackage3;
                                                        i5 = aggregateHours3;
                                                        isCompact = checkedIn;
                                                    }
                                                }
                                                printWriter.println(":");
                                                if (activeOnly) {
                                                    processStats4 = processStats3;
                                                    fileStr2 = fileStr;
                                                    z2 = false;
                                                    z = z3;
                                                    reqPackage = reqPackage3;
                                                    try {
                                                        processStats3.dumpLocked(printWriter, reqPackage3, now, false, false, activeOnly2);
                                                    } catch (Throwable th8) {
                                                        th = th8;
                                                        i5 = aggregateHours3;
                                                        isCompact = checkedIn;
                                                        str = reqPackage;
                                                        this.mWriteLock.unlock();
                                                        throw th;
                                                    }
                                                }
                                                processStats4 = processStats3;
                                                fileStr2 = fileStr;
                                                z2 = false;
                                                z = z3;
                                                reqPackage = reqPackage3;
                                                processStats4.dumpSummaryLocked(printWriter, reqPackage, now, activeOnly2);
                                                isCompact = checkedIn;
                                                ProcessStats processStats5 = processStats4;
                                                reqPackage3 = reqPackage;
                                                if (!isCheckin) {
                                                    File baseFile = file2.getBaseFile();
                                                    StringBuilder stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append(fileStr2);
                                                    stringBuilder2.append(STATE_FILE_CHECKIN_SUFFIX);
                                                    baseFile.renameTo(new File(stringBuilder2.toString()));
                                                }
                                            } catch (Throwable th9) {
                                                th = th9;
                                                str = reqPackage3;
                                                i5 = aggregateHours3;
                                                isCompact = checkedIn;
                                            }
                                        }
                                        reqPackage3 = reqPackage;
                                        try {
                                            processStats4.dumpCheckinLocked(printWriter, reqPackage3);
                                            if (!isCheckin) {
                                            }
                                        } catch (Throwable th10) {
                                            th = th10;
                                            printWriter.print("**** FAILURE DUMPING STATE: ");
                                            printWriter.println((String) files2.get(maxNum));
                                            th.printStackTrace(printWriter);
                                            i = maxNum + 1;
                                            z3 = z;
                                        }
                                    }
                                } catch (Throwable th11) {
                                    th = th11;
                                    z2 = false;
                                    z = z3;
                                    printWriter.print("**** FAILURE DUMPING STATE: ");
                                    printWriter.println((String) files2.get(maxNum));
                                    th.printStackTrace(printWriter);
                                    i = maxNum + 1;
                                    z3 = z;
                                }
                            } catch (Throwable th12) {
                                th = th12;
                                z = z3;
                                printWriter.print("**** FAILURE DUMPING STATE: ");
                                printWriter.println((String) files2.get(maxNum));
                                th.printStackTrace(printWriter);
                                i = maxNum + 1;
                                z3 = z;
                            }
                            i = maxNum + 1;
                            z3 = z;
                        }
                    }
                    z = z3;
                    z2 = false;
                    this.mWriteLock.unlock();
                    sepNeeded = isCompact;
                } catch (Throwable th13) {
                    th = th13;
                    str = reqPackage3;
                    i5 = aggregateHours3;
                    this.mWriteLock.unlock();
                    throw th;
                }
            }
            sepNeeded = false;
            z = true;
            z2 = false;
            if (isCheckin) {
                i5 = aggregateHours3;
                checkedIn = sepNeeded;
            } else {
                synchronized (this.mAm) {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (isCompact2) {
                        try {
                            this.mProcessStats.dumpCheckinLocked(printWriter, reqPackage3);
                            str = reqPackage3;
                            checkedIn = sepNeeded;
                        } catch (Throwable th14) {
                            th = th14;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th15) {
                                    th = th15;
                                }
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    if (sepNeeded) {
                        pw.println();
                    }
                    try {
                        printWriter.println("CURRENT STATS:");
                        if (dumpDetails || activeOnly) {
                            str = reqPackage3;
                            try {
                                this.mProcessStats.dumpLocked(printWriter, reqPackage3, now, !activeOnly ? z : z2, dumpAll, activeOnly2);
                                if (dumpAll) {
                                    try {
                                        printWriter.print("  mFile=");
                                        printWriter.println(this.mFile.getBaseFile());
                                    } catch (Throwable th16) {
                                        th = th16;
                                    }
                                }
                            } catch (Throwable th17) {
                                th = th17;
                                i5 = aggregateHours3;
                                while (true) {
                                    break;
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        this.mProcessStats.dumpSummaryLocked(printWriter, reqPackage3, now, activeOnly2);
                        str = reqPackage3;
                        checkedIn = true;
                    } catch (Throwable th18) {
                        th = th18;
                        str = reqPackage3;
                        i5 = aggregateHours3;
                        while (true) {
                            break;
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                    try {
                    } catch (Throwable th19) {
                        th = th19;
                        i5 = aggregateHours3;
                        sepNeeded = checkedIn;
                        while (true) {
                            break;
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            }
        }
    }

    private void dumpAggregatedStats(ProtoOutputStream proto, long fieldId, int aggregateHours, long now) {
        ParcelFileDescriptor pfd = getStatsOverTime(((long) (((aggregateHours * 60) * 60) * 1000)) - (ProcessStats.COMMIT_PERIOD / 2));
        if (pfd != null) {
            ProcessStats stats = new ProcessStats(false);
            stats.read(new AutoCloseInputStream(pfd));
            if (stats.mReadError == null) {
                stats.writeToProto(proto, fieldId, now);
            }
        }
    }

    private void dumpProto(FileDescriptor fd) {
        long now;
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                now = SystemClock.uptimeMillis();
                this.mProcessStats.writeToProto(proto, 1146756268033L, now);
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        ProtoOutputStream protoOutputStream = proto;
        long j = now;
        dumpAggregatedStats(protoOutputStream, 1146756268034L, 3, j);
        dumpAggregatedStats(protoOutputStream, 1146756268035L, 24, j);
        proto.flush();
    }
}
