package com.android.internal.app.procstats;

import android.os.Parcel;
import android.os.SystemClock;
import android.util.Slog;
import android.util.TimeUtils;
import java.io.PrintWriter;

public final class ServiceState {
    private static final boolean DEBUG = false;
    public static final int SERVICE_BOUND = 2;
    public static final int SERVICE_COUNT = 4;
    public static final int SERVICE_EXEC = 3;
    public static final int SERVICE_RUN = 0;
    public static final int SERVICE_STARTED = 1;
    private static final String TAG = "ProcessStats";
    private int mBoundCount;
    private long mBoundStartTime;
    private int mBoundState = -1;
    private final DurationsTable mDurations;
    private int mExecCount;
    private long mExecStartTime;
    private int mExecState = -1;
    private final String mName;
    private Object mOwner;
    private final String mPackage;
    private ProcessState mProc;
    private final String mProcessName;
    private boolean mRestarting;
    private int mRunCount;
    private long mRunStartTime;
    private int mRunState = -1;
    private boolean mStarted;
    private int mStartedCount;
    private long mStartedStartTime;
    private int mStartedState = -1;

    public ServiceState(ProcessStats processStats, String pkg, String name, String processName, ProcessState proc) {
        this.mPackage = pkg;
        this.mName = name;
        this.mProcessName = processName;
        this.mProc = proc;
        this.mDurations = new DurationsTable(processStats.mTableData);
    }

    public String getPackage() {
        return this.mPackage;
    }

    public String getProcessName() {
        return this.mProcessName;
    }

    public String getName() {
        return this.mName;
    }

    public ProcessState getProcess() {
        return this.mProc;
    }

    public void setProcess(ProcessState proc) {
        this.mProc = proc;
    }

    public void setMemFactor(int memFactor, long now) {
        if (isRestarting()) {
            setRestarting(true, memFactor, now);
        } else if (isInUse()) {
            if (this.mStartedState != -1) {
                setStarted(true, memFactor, now);
            }
            if (this.mBoundState != -1) {
                setBound(true, memFactor, now);
            }
            if (this.mExecState != -1) {
                setExecuting(true, memFactor, now);
            }
        }
    }

    public void applyNewOwner(Object newOwner) {
        if (this.mOwner == newOwner) {
            return;
        }
        if (this.mOwner == null) {
            this.mOwner = newOwner;
            this.mProc.incActiveServices(this.mName);
            return;
        }
        this.mOwner = newOwner;
        if (this.mStarted || this.mBoundState != -1 || this.mExecState != -1) {
            long now = SystemClock.uptimeMillis();
            if (this.mStarted) {
                setStarted(false, 0, now);
            }
            if (this.mBoundState != -1) {
                setBound(false, 0, now);
            }
            if (this.mExecState != -1) {
                setExecuting(false, 0, now);
            }
        }
    }

    public void clearCurrentOwner(Object owner, boolean silently) {
        if (this.mOwner == owner) {
            this.mProc.decActiveServices(this.mName);
            if (!(!this.mStarted && this.mBoundState == -1 && this.mExecState == -1)) {
                StringBuilder stringBuilder;
                long now = SystemClock.uptimeMillis();
                if (this.mStarted) {
                    if (!silently) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Service owner ");
                        stringBuilder.append(owner);
                        stringBuilder.append(" cleared while started: pkg=");
                        stringBuilder.append(this.mPackage);
                        stringBuilder.append(" service=");
                        stringBuilder.append(this.mName);
                        stringBuilder.append(" proc=");
                        stringBuilder.append(this.mProc);
                        Slog.wtfStack("ProcessStats", stringBuilder.toString());
                    }
                    setStarted(false, 0, now);
                }
                if (this.mBoundState != -1) {
                    if (!silently) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Service owner ");
                        stringBuilder.append(owner);
                        stringBuilder.append(" cleared while bound: pkg=");
                        stringBuilder.append(this.mPackage);
                        stringBuilder.append(" service=");
                        stringBuilder.append(this.mName);
                        stringBuilder.append(" proc=");
                        stringBuilder.append(this.mProc);
                        Slog.wtfStack("ProcessStats", stringBuilder.toString());
                    }
                    setBound(false, 0, now);
                }
                if (this.mExecState != -1) {
                    if (!silently) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Service owner ");
                        stringBuilder2.append(owner);
                        stringBuilder2.append(" cleared while exec: pkg=");
                        stringBuilder2.append(this.mPackage);
                        stringBuilder2.append(" service=");
                        stringBuilder2.append(this.mName);
                        stringBuilder2.append(" proc=");
                        stringBuilder2.append(this.mProc);
                        Slog.wtfStack("ProcessStats", stringBuilder2.toString());
                    }
                    setExecuting(false, 0, now);
                }
            }
            this.mOwner = null;
        }
    }

    public boolean isInUse() {
        return this.mOwner != null || this.mRestarting;
    }

    public boolean isRestarting() {
        return this.mRestarting;
    }

    public void add(ServiceState other) {
        this.mDurations.addDurations(other.mDurations);
        this.mRunCount += other.mRunCount;
        this.mStartedCount += other.mStartedCount;
        this.mBoundCount += other.mBoundCount;
        this.mExecCount += other.mExecCount;
    }

    public void resetSafely(long now) {
        this.mDurations.resetTable();
        int i = 0;
        this.mRunCount = this.mRunState != -1 ? 1 : 0;
        this.mStartedCount = this.mStartedState != -1 ? 1 : 0;
        this.mBoundCount = this.mBoundState != -1 ? 1 : 0;
        if (this.mExecState != -1) {
            i = 1;
        }
        this.mExecCount = i;
        this.mExecStartTime = now;
        this.mBoundStartTime = now;
        this.mStartedStartTime = now;
        this.mRunStartTime = now;
    }

    public void writeToParcel(Parcel out, long now) {
        this.mDurations.writeToParcel(out);
        out.writeInt(this.mRunCount);
        out.writeInt(this.mStartedCount);
        out.writeInt(this.mBoundCount);
        out.writeInt(this.mExecCount);
    }

    public boolean readFromParcel(Parcel in) {
        if (!this.mDurations.readFromParcel(in)) {
            return false;
        }
        this.mRunCount = in.readInt();
        this.mStartedCount = in.readInt();
        this.mBoundCount = in.readInt();
        this.mExecCount = in.readInt();
        return true;
    }

    public void commitStateTime(long now) {
        if (this.mRunState != -1) {
            this.mDurations.addDuration(0 + (this.mRunState * 4), now - this.mRunStartTime);
            this.mRunStartTime = now;
        }
        if (this.mStartedState != -1) {
            this.mDurations.addDuration(1 + (this.mStartedState * 4), now - this.mStartedStartTime);
            this.mStartedStartTime = now;
        }
        if (this.mBoundState != -1) {
            this.mDurations.addDuration(2 + (this.mBoundState * 4), now - this.mBoundStartTime);
            this.mBoundStartTime = now;
        }
        if (this.mExecState != -1) {
            this.mDurations.addDuration(3 + (this.mExecState * 4), now - this.mExecStartTime);
            this.mExecStartTime = now;
        }
    }

    private void updateRunning(int memFactor, long now) {
        int state = (this.mStartedState == -1 && this.mBoundState == -1 && this.mExecState == -1) ? -1 : memFactor;
        if (this.mRunState != state) {
            if (this.mRunState != -1) {
                this.mDurations.addDuration(0 + (this.mRunState * 4), now - this.mRunStartTime);
            } else if (state != -1) {
                this.mRunCount++;
            }
            this.mRunState = state;
            this.mRunStartTime = now;
        }
    }

    public void setStarted(boolean started, int memFactor, long now) {
        if (this.mOwner == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Starting service ");
            stringBuilder.append(this);
            stringBuilder.append(" without owner");
            Slog.wtf("ProcessStats", stringBuilder.toString());
        }
        this.mStarted = started;
        updateStartedState(memFactor, now);
    }

    public void setRestarting(boolean restarting, int memFactor, long now) {
        this.mRestarting = restarting;
        updateStartedState(memFactor, now);
    }

    public void updateStartedState(int memFactor, long now) {
        boolean started = false;
        boolean wasStarted = this.mStartedState != -1;
        if (this.mStarted || this.mRestarting) {
            started = true;
        }
        int state = started ? memFactor : -1;
        if (this.mStartedState != state) {
            if (this.mStartedState != -1) {
                this.mDurations.addDuration(1 + (this.mStartedState * 4), now - this.mStartedStartTime);
            } else if (started) {
                this.mStartedCount++;
            }
            this.mStartedState = state;
            this.mStartedStartTime = now;
            this.mProc = this.mProc.pullFixedProc(this.mPackage);
            if (wasStarted != started) {
                if (started) {
                    this.mProc.incStartedServices(memFactor, now, this.mName);
                } else {
                    this.mProc.decStartedServices(memFactor, now, this.mName);
                }
            }
            updateRunning(memFactor, now);
        }
    }

    public void setBound(boolean bound, int memFactor, long now) {
        if (this.mOwner == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Binding service ");
            stringBuilder.append(this);
            stringBuilder.append(" without owner");
            Slog.wtf("ProcessStats", stringBuilder.toString());
        }
        int state = bound ? memFactor : -1;
        if (this.mBoundState != state) {
            if (this.mBoundState != -1) {
                this.mDurations.addDuration(2 + (this.mBoundState * 4), now - this.mBoundStartTime);
            } else if (bound) {
                this.mBoundCount++;
            }
            this.mBoundState = state;
            this.mBoundStartTime = now;
            updateRunning(memFactor, now);
        }
    }

    public void setExecuting(boolean executing, int memFactor, long now) {
        if (this.mOwner == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Executing service ");
            stringBuilder.append(this);
            stringBuilder.append(" without owner");
            Slog.wtf("ProcessStats", stringBuilder.toString());
        }
        int state = executing ? memFactor : -1;
        if (this.mExecState != state) {
            if (this.mExecState != -1) {
                this.mDurations.addDuration(3 + (this.mExecState * 4), now - this.mExecStartTime);
            } else if (executing) {
                this.mExecCount++;
            }
            this.mExecState = state;
            this.mExecStartTime = now;
            updateRunning(memFactor, now);
        }
    }

    public long getDuration(int opType, int curState, long startTime, int memFactor, long now) {
        long time = this.mDurations.getValueForId((byte) ((memFactor * 4) + opType));
        if (curState == memFactor) {
            return time + (now - startTime);
        }
        return time;
    }

    public void dumpStats(PrintWriter pw, String prefix, String prefixInner, String headerPrefix, long now, long totalTime, boolean dumpSummary, boolean dumpAll) {
        PrintWriter printWriter = pw;
        String str = "Running";
        int i = this.mRunCount;
        int i2 = this.mRunState;
        long j = this.mRunStartTime;
        boolean z = false;
        boolean z2 = !dumpSummary || dumpAll;
        dumpStats(printWriter, prefix, prefixInner, headerPrefix, str, i, 0, i2, j, now, totalTime, z2);
        str = "Started";
        i = this.mStartedCount;
        i2 = this.mStartedState;
        j = this.mStartedStartTime;
        z2 = !dumpSummary || dumpAll;
        dumpStats(pw, prefix, prefixInner, headerPrefix, str, i, 1, i2, j, now, totalTime, z2);
        str = "Bound";
        i = this.mBoundCount;
        i2 = this.mBoundState;
        j = this.mBoundStartTime;
        z2 = !dumpSummary || dumpAll;
        dumpStats(pw, prefix, prefixInner, headerPrefix, str, i, 2, i2, j, now, totalTime, z2);
        str = "Executing";
        i = this.mExecCount;
        i2 = this.mExecState;
        j = this.mExecStartTime;
        if (!dumpSummary || dumpAll) {
            z = true;
        }
        dumpStats(pw, prefix, prefixInner, headerPrefix, str, i, 3, i2, j, now, totalTime, z);
        PrintWriter printWriter2;
        if (dumpAll) {
            if (this.mOwner != null) {
                printWriter2 = pw;
                printWriter2.print("        mOwner=");
                printWriter2.println(this.mOwner);
            } else {
                printWriter2 = pw;
            }
            if (this.mStarted || this.mRestarting) {
                printWriter2.print("        mStarted=");
                printWriter2.print(this.mStarted);
                printWriter2.print(" mRestarting=");
                printWriter2.println(this.mRestarting);
                return;
            }
            return;
        }
        printWriter2 = pw;
    }

    private void dumpStats(PrintWriter pw, String prefix, String prefixInner, String headerPrefix, String header, int count, int serviceType, int state, long startTime, long now, long totalTime, boolean dumpAll) {
        PrintWriter printWriter = pw;
        String str = header;
        int i = count;
        if (i != 0) {
            if (dumpAll) {
                pw.print(prefix);
                printWriter.print(str);
                printWriter.print(" op count ");
                printWriter.print(i);
                printWriter.println(":");
                dumpTime(printWriter, prefixInner, serviceType, state, startTime, now);
            } else {
                long myTime = dumpTime(null, null, serviceType, state, startTime, now);
                pw.print(prefix);
                printWriter.print(headerPrefix);
                printWriter.print(str);
                printWriter.print(" count ");
                printWriter.print(i);
                printWriter.print(" / time ");
                DumpUtils.printPercent(printWriter, ((double) myTime) / ((double) totalTime));
                pw.println();
                return;
            }
        }
        String str2 = headerPrefix;
        long j = totalTime;
    }

    public long dumpTime(PrintWriter pw, String prefix, int serviceType, int curState, long curStartTime, long now) {
        int i;
        PrintWriter printWriter = pw;
        int printedScreen = -1;
        long totalTime = 0;
        int iscreen = 0;
        while (iscreen < 8) {
            int printedMem = -1;
            long totalTime2 = totalTime;
            int imem = 0;
            while (imem < 4) {
                int state = imem + iscreen;
                long time = getDuration(serviceType, curState, curStartTime, state, now);
                String running = "";
                if (curState == state && printWriter != null) {
                    running = " (running)";
                }
                if (time != 0) {
                    if (printWriter != null) {
                        pw.print(prefix);
                        DumpUtils.printScreenLabel(printWriter, printedScreen != iscreen ? iscreen : -1);
                        printedScreen = iscreen;
                        DumpUtils.printMemLabel(printWriter, printedMem != imem ? imem : -1, 0);
                        printedMem = imem;
                        printWriter.print(": ");
                        TimeUtils.formatDuration(time, printWriter);
                        printWriter.println(running);
                    }
                    totalTime2 += time;
                }
                imem++;
            }
            i = curState;
            iscreen += 4;
            totalTime = totalTime2;
        }
        i = curState;
        if (!(totalTime == 0 || printWriter == null)) {
            pw.print(prefix);
            printWriter.print("    TOTAL: ");
            TimeUtils.formatDuration(totalTime, printWriter);
            pw.println();
        }
        return totalTime;
    }

    public void dumpTimesCheckin(PrintWriter pw, String pkgName, int uid, long vers, String serviceName, long now) {
        PrintWriter printWriter = pw;
        String str = pkgName;
        int i = uid;
        long j = vers;
        String str2 = serviceName;
        long j2 = now;
        dumpTimeCheckin(printWriter, "pkgsvc-run", str, i, j, str2, 0, this.mRunCount, this.mRunState, this.mRunStartTime, j2);
        dumpTimeCheckin(printWriter, "pkgsvc-start", str, i, j, str2, 1, this.mStartedCount, this.mStartedState, this.mStartedStartTime, j2);
        dumpTimeCheckin(printWriter, "pkgsvc-bound", str, i, j, str2, 2, this.mBoundCount, this.mBoundState, this.mBoundStartTime, j2);
        dumpTimeCheckin(printWriter, "pkgsvc-exec", str, i, j, str2, 3, this.mExecCount, this.mExecState, this.mExecStartTime, j2);
    }

    private void dumpTimeCheckin(PrintWriter pw, String label, String packageName, int uid, long vers, String serviceName, int serviceType, int opCount, int curState, long curStartTime, long now) {
        ServiceState serviceState = this;
        PrintWriter printWriter = pw;
        int i = opCount;
        int i2 = curState;
        if (i > 0) {
            pw.print(label);
            printWriter.print(",");
            printWriter.print(packageName);
            printWriter.print(",");
            printWriter.print(uid);
            printWriter.print(",");
            printWriter.print(vers);
            printWriter.print(",");
            printWriter.print(serviceName);
            printWriter.print(",");
            printWriter.print(i);
            boolean didCurState = false;
            int N = serviceState.mDurations.getKeyCount();
            int i3 = 0;
            while (i3 < N) {
                i = serviceState.mDurations.getKeyAt(i3);
                long time = serviceState.mDurations.getValue(i);
                long time2 = SparseMappingTable.getIdFromKey(i);
                int memFactor = time2 / 4;
                time2 %= 4;
                if (time2 == serviceType) {
                    if (i2 == memFactor) {
                        didCurState = true;
                        time += now - curStartTime;
                    }
                    int type = time2;
                    DumpUtils.printAdjTagAndValue(printWriter, memFactor, time);
                }
                i3++;
                serviceState = this;
                i = opCount;
                String str = packageName;
                int i4 = uid;
            }
            i = serviceType;
            if (!(didCurState || i2 == -1)) {
                DumpUtils.printAdjTagAndValue(printWriter, i2, now - curStartTime);
            }
            pw.println();
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ServiceState{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" ");
        stringBuilder.append(this.mName);
        stringBuilder.append(" pkg=");
        stringBuilder.append(this.mPackage);
        stringBuilder.append(" proc=");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
