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
    private long mTraceTag;

    public TimingsTraceLog(String tag, long traceTag) {
        this.mStartTimes = DEBUG_BOOT_TIME ? new ArrayDeque() : null;
        this.mTag = tag;
        this.mTraceTag = traceTag;
    }

    public void traceBegin(String name) {
        Trace.traceBegin(this.mTraceTag, name);
        if (DEBUG_BOOT_TIME) {
            this.mStartTimes.push(Pair.create(name, Long.valueOf(SystemClock.elapsedRealtime())));
        }
    }

    public void traceEnd() {
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

    public void logDuration(String name, long timeMs) {
        Slog.d(this.mTag, name + " took to complete: " + timeMs + "ms");
    }
}
