package android.util;

import android.os.Build;
import android.os.SystemClock;
import android.os.Trace;
import java.util.ArrayDeque;
import java.util.Deque;

public class TimingsTraceLog {
    private static final boolean DEBUG_BOOT_TIME = (Build.IS_USER ^ 1);
    private final Deque<Pair<String, Long>> mStartTimes;
    private final String mTag;
    private long mThreadId;
    private long mTraceTag;

    public TimingsTraceLog(String tag, long traceTag) {
        this.mStartTimes = DEBUG_BOOT_TIME ? new ArrayDeque() : null;
        this.mTag = tag;
        this.mTraceTag = traceTag;
        this.mThreadId = Thread.currentThread().getId();
    }

    public void traceBegin(String name) {
        assertSameThread();
        Trace.traceBegin(this.mTraceTag, name);
        if (DEBUG_BOOT_TIME) {
            this.mStartTimes.push(Pair.create(name, Long.valueOf(SystemClock.elapsedRealtime())));
        }
    }

    public void traceEnd() {
        assertSameThread();
        Trace.traceEnd(this.mTraceTag);
        if (!DEBUG_BOOT_TIME) {
            return;
        }
        if (this.mStartTimes.peek() == null) {
            Slog.w(this.mTag, "traceEnd called more times than traceBegin");
            return;
        }
        Pair<String, Long> event = (Pair) this.mStartTimes.pop();
        logDuration((String) event.first, SystemClock.elapsedRealtime() - ((Long) event.second).longValue());
    }

    private void assertSameThread() {
        Thread currentThread = Thread.currentThread();
        if (currentThread.getId() != this.mThreadId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Instance of TimingsTraceLog can only be called from the thread it was created on (tid: ");
            stringBuilder.append(this.mThreadId);
            stringBuilder.append("), but was from ");
            stringBuilder.append(currentThread.getName());
            stringBuilder.append(" (tid: ");
            stringBuilder.append(currentThread.getId());
            stringBuilder.append(")");
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public void logDuration(String name, long timeMs) {
        String str = this.mTag;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append(" took to complete: ");
        stringBuilder.append(timeMs);
        stringBuilder.append("ms");
        Slog.d(str, stringBuilder.toString());
    }
}
