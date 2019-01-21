package com.android.internal.os;

import android.os.Binder;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BinderCallsStats {
    private static final int CALL_SESSIONS_POOL_SIZE = 100;
    private static final BinderCallsStats sInstance = new BinderCallsStats();
    private final Queue<CallSession> mCallSessionsPool = new ConcurrentLinkedQueue();
    private volatile boolean mDetailedTracking = false;
    private final Object mLock = new Object();
    private long mStartTime = System.currentTimeMillis();
    @GuardedBy("mLock")
    private final SparseArray<UidEntry> mUidEntries = new SparseArray();

    public static class CallSession {
        CallStat mCallStat = new CallStat();
        int mCallingUId;
        long mStarted;
    }

    private static class CallStat {
        long callCount;
        String className;
        int msg;
        long time;

        CallStat() {
        }

        CallStat(String className, int msg) {
            this.className = className;
            this.msg = msg;
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            CallStat callStat = (CallStat) o;
            if (!(this.msg == callStat.msg && this.className.equals(callStat.className))) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return (31 * this.className.hashCode()) + this.msg;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.className);
            stringBuilder.append("/");
            stringBuilder.append(this.msg);
            return stringBuilder.toString();
        }
    }

    private static class UidEntry {
        long callCount;
        Map<CallStat, CallStat> mCallStats = new ArrayMap();
        long time;
        int uid;

        UidEntry(int uid) {
            this.uid = uid;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UidEntry{time=");
            stringBuilder.append(this.time);
            stringBuilder.append(", callCount=");
            stringBuilder.append(this.callCount);
            stringBuilder.append(", mCallStats=");
            stringBuilder.append(this.mCallStats);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (this.uid != ((UidEntry) o).uid) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return this.uid;
        }
    }

    private BinderCallsStats() {
    }

    @VisibleForTesting
    public BinderCallsStats(boolean detailedTracking) {
        this.mDetailedTracking = detailedTracking;
    }

    public CallSession callStarted(Binder binder, int code) {
        return callStarted(binder.getClass().getName(), code);
    }

    private CallSession callStarted(String className, int code) {
        CallSession s = (CallSession) this.mCallSessionsPool.poll();
        if (s == null) {
            s = new CallSession();
        }
        s.mCallStat.className = className;
        s.mCallStat.msg = code;
        s.mStarted = getThreadTimeMicro();
        return s;
    }

    public void callEnded(CallSession s) {
        Preconditions.checkNotNull(s);
        long duration = this.mDetailedTracking ? getThreadTimeMicro() - s.mStarted : 1;
        s.mCallingUId = Binder.getCallingUid();
        synchronized (this.mLock) {
            UidEntry uidEntry = (UidEntry) this.mUidEntries.get(s.mCallingUId);
            if (uidEntry == null) {
                uidEntry = new UidEntry(s.mCallingUId);
                this.mUidEntries.put(s.mCallingUId, uidEntry);
            }
            if (this.mDetailedTracking) {
                CallStat callStat = (CallStat) uidEntry.mCallStats.get(s.mCallStat);
                if (callStat == null) {
                    callStat = new CallStat(s.mCallStat.className, s.mCallStat.msg);
                    uidEntry.mCallStats.put(callStat, callStat);
                }
                callStat.callCount++;
                callStat.time += duration;
            }
            uidEntry.time += duration;
            uidEntry.callCount++;
        }
        if (this.mCallSessionsPool.size() < 100) {
            this.mCallSessionsPool.add(s);
        }
    }

    /* JADX WARNING: Missing block: B:33:0x00bf, code skipped:
            if (r31.mDetailedTracking == false) goto L_0x01e2;
     */
    /* JADX WARNING: Missing block: B:34:0x00c1, code skipped:
            r2.println("Raw data (uid,call_desc,time):");
            r10.sort(com.android.internal.os.-$$Lambda$BinderCallsStats$JdIS98lVGLAIfkEkC976rVyBc_U.INSTANCE);
            r0 = new java.lang.StringBuilder();
            r11 = r10.iterator();
     */
    /* JADX WARNING: Missing block: B:36:0x00d8, code skipped:
            if (r11.hasNext() == false) goto L_0x0137;
     */
    /* JADX WARNING: Missing block: B:37:0x00da, code skipped:
            r14 = (com.android.internal.os.BinderCallsStats.UidEntry) r11.next();
            r5 = new java.util.ArrayList(r14.mCallStats.keySet());
            r5.sort(com.android.internal.os.-$$Lambda$BinderCallsStats$8JB19VSNkNr7RqU7ZTJ6NGkFXVU.INSTANCE);
            r6 = r5.iterator();
     */
    /* JADX WARNING: Missing block: B:39:0x00fa, code skipped:
            if (r6.hasNext() == false) goto L_0x0133;
     */
    /* JADX WARNING: Missing block: B:40:0x00fc, code skipped:
            r15 = (com.android.internal.os.BinderCallsStats.CallStat) r6.next();
            r0.setLength(0);
            r0.append("    ");
            r0.append(r14.uid);
            r0.append(",");
            r0.append(r15);
            r0.append(',');
            r21 = r5;
            r22 = r6;
            r0.append(r15.time);
            r2.println(r0);
            r5 = r21;
            r6 = r22;
            r1 = r31;
     */
    /* JADX WARNING: Missing block: B:41:0x0133, code skipped:
            r1 = r31;
     */
    /* JADX WARNING: Missing block: B:42:0x0137, code skipped:
            r32.println();
            r2.println("Per UID Summary(UID: time, % of total_time, calls_count):");
            r1 = new java.util.ArrayList(r3.entrySet());
            r1.sort(com.android.internal.os.-$$Lambda$BinderCallsStats$BeSOWJ8AoyB7S9CtX-6IPAXHyNQ.INSTANCE);
            r5 = r1.iterator();
     */
    /* JADX WARNING: Missing block: B:44:0x0155, code skipped:
            if (r5.hasNext() == false) goto L_0x01b0;
     */
    /* JADX WARNING: Missing block: B:45:0x0157, code skipped:
            r6 = (java.util.Map.Entry) r5.next();
            r11 = (java.lang.Long) r4.get(r6.getKey());
            r15 = new java.lang.Object[4];
            r15[0] = r6.getKey();
            r15[1] = r6.getValue();
            r23 = r0;
            r24 = r1;
            r25 = r5;
            r26 = r6;
            r15[2] = java.lang.Double.valueOf((((double) ((java.lang.Long) r6.getValue()).longValue()) * 100.0d) / ((double) r7));
            r15[3] = r11;
            r2.println(java.lang.String.format("  %7d: %11d %3.0f%% %8d", r15));
            r0 = r23;
            r1 = r24;
            r5 = r25;
     */
    /* JADX WARNING: Missing block: B:46:0x01b0, code skipped:
            r23 = r0;
            r24 = r1;
            r32.println();
            r2.println(java.lang.String.format("  Summary: total_time=%d, calls_count=%d, avg_call_time=%.0f", new java.lang.Object[]{java.lang.Long.valueOf(r7), java.lang.Long.valueOf(r12), java.lang.Double.valueOf(((double) r7) / ((double) r12))}));
            r29 = r3;
            r30 = r4;
     */
    /* JADX WARNING: Missing block: B:47:0x01e2, code skipped:
            r2.println("Per UID Summary(UID: calls_count, % of total calls_count):");
            r0 = new java.util.ArrayList(r3.entrySet());
            r0.sort(com.android.internal.os.-$$Lambda$BinderCallsStats$jhdszMKzG9FSuIQ4Vz9B0exXKPk.INSTANCE);
            r1 = r0.iterator();
     */
    /* JADX WARNING: Missing block: B:49:0x01fd, code skipped:
            if (r1.hasNext() == false) goto L_0x0253;
     */
    /* JADX WARNING: Missing block: B:50:0x01ff, code skipped:
            r5 = (java.util.Map.Entry) r1.next();
            r6 = (java.lang.Long) r4.get(r5.getKey());
            r15 = new java.lang.Object[3];
            r15[0] = r5.getKey();
            r15[1] = r6;
            r27 = r0;
            r28 = r1;
            r29 = r3;
            r30 = r4;
            r15[2] = java.lang.Double.valueOf((((double) ((java.lang.Long) r5.getValue()).longValue()) * 100.0d) / ((double) r7));
            r2.println(java.lang.String.format("    %7d: %8d %3.0f%%", r15));
            r0 = r27;
            r1 = r28;
            r3 = r29;
            r4 = r30;
     */
    /* JADX WARNING: Missing block: B:51:0x0253, code skipped:
            r29 = r3;
            r30 = r4;
     */
    /* JADX WARNING: Missing block: B:64:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:65:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dump(PrintWriter pw) {
        Throwable th;
        Object obj;
        Object obj2;
        long totalCallsTime;
        BinderCallsStats binderCallsStats = this;
        PrintWriter printWriter = pw;
        Map<Integer, Long> uidTimeMap = new HashMap();
        Map<Integer, Long> uidCallCountMap = new HashMap();
        long totalCallsTime2 = 0;
        printWriter.print("Start time: ");
        printWriter.println(DateFormat.format("yyyy-MM-dd HH:mm:ss", binderCallsStats.mStartTime));
        int uidEntriesSize = binderCallsStats.mUidEntries.size();
        ArrayList entries = new ArrayList();
        synchronized (binderCallsStats.mLock) {
            long totalCallsCount = 0;
            int i = 0;
            while (i < uidEntriesSize) {
                UidEntry e;
                try {
                    e = (UidEntry) binderCallsStats.mUidEntries.valueAt(i);
                    entries.add(e);
                    totalCallsTime2 += e.time;
                } catch (Throwable th2) {
                    th = th2;
                    obj = uidTimeMap;
                    obj2 = uidCallCountMap;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    throw th;
                }
                try {
                    long j;
                    Long totalTimePerUid = (Long) uidTimeMap.get(Integer.valueOf(e.uid));
                    Integer valueOf = Integer.valueOf(e.uid);
                    if (totalTimePerUid == null) {
                        j = e.time;
                        totalCallsTime = totalCallsTime2;
                    } else {
                        totalCallsTime = totalCallsTime2;
                        try {
                            j = totalTimePerUid.longValue() + e.time;
                        } catch (Throwable th4) {
                            th = th4;
                            obj = uidTimeMap;
                            obj2 = uidCallCountMap;
                            totalCallsTime2 = totalCallsTime;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    }
                    uidTimeMap.put(valueOf, Long.valueOf(j));
                    Long totalCallsPerUid = (Long) uidCallCountMap.get(Integer.valueOf(e.uid));
                    Integer valueOf2 = Integer.valueOf(e.uid);
                    if (totalCallsPerUid == null) {
                        totalCallsTime2 = e.callCount;
                        Long l = totalTimePerUid;
                    } else {
                        totalCallsTime2 = totalCallsPerUid.longValue() + e.callCount;
                    }
                    uidCallCountMap.put(valueOf2, Long.valueOf(totalCallsTime2));
                    totalCallsCount += e.callCount;
                    i++;
                    totalCallsTime2 = totalCallsTime;
                    binderCallsStats = this;
                } catch (Throwable th5) {
                    th = th5;
                    totalCallsTime = totalCallsTime2;
                    obj = uidTimeMap;
                    obj2 = uidCallCountMap;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
            try {
            } catch (Throwable th6) {
                th = th6;
                Map<Integer, Long> map = uidTimeMap;
                Map<Integer, Long> map2 = uidCallCountMap;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    static /* synthetic */ int lambda$dump$0(UidEntry o1, UidEntry o2) {
        if (o1.time < o2.time) {
            return 1;
        }
        if (o1.time > o2.time) {
            return -1;
        }
        return 0;
    }

    static /* synthetic */ int lambda$dump$1(CallStat o1, CallStat o2) {
        if (o1.time < o2.time) {
            return 1;
        }
        if (o1.time > o2.time) {
            return -1;
        }
        return 0;
    }

    private long getThreadTimeMicro() {
        return this.mDetailedTracking ? SystemClock.currentThreadTimeMicro() : 0;
    }

    public static BinderCallsStats getInstance() {
        return sInstance;
    }

    public void setDetailedTracking(boolean enabled) {
        if (enabled != this.mDetailedTracking) {
            reset();
            this.mDetailedTracking = enabled;
        }
    }

    public void reset() {
        synchronized (this.mLock) {
            this.mUidEntries.clear();
            this.mStartTime = System.currentTimeMillis();
        }
    }
}
