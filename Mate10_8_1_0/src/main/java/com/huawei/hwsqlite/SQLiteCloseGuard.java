package com.huawei.hwsqlite;

import android.util.Log;

public final class SQLiteCloseGuard {
    private static volatile boolean ENABLED = true;
    private static final SQLiteCloseGuard NOOP = new SQLiteCloseGuard();
    private static volatile Reporter REPORTER = new DefaultReporter();
    private Throwable allocationSite;
    private String detailMessage;

    public interface Reporter {
        void report(String str, String str2);

        void report(String str, Throwable th);
    }

    private static final class DefaultReporter implements Reporter {
        private DefaultReporter() {
        }

        public void report(String message, Throwable allocationSite) {
            Log.w(message, allocationSite);
        }

        public void report(String message, String detail) {
            Log.w(message, detail);
        }
    }

    public static SQLiteCloseGuard get() {
        if (ENABLED) {
            return new SQLiteCloseGuard();
        }
        return NOOP;
    }

    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    public static void setReporter(Reporter reporter) {
        if (reporter == null) {
            throw new NullPointerException("reporter == null");
        }
        REPORTER = reporter;
    }

    public static Reporter getReporter() {
        return REPORTER;
    }

    private SQLiteCloseGuard() {
    }

    public void open(String closer) {
        if (closer == null) {
            throw new NullPointerException("closer == null");
        } else if (this != NOOP && (ENABLED ^ 1) == 0) {
            this.allocationSite = new Throwable("Explicit termination method '" + closer + "' not called");
        }
    }

    public void open(String detail, boolean fillStack) {
        if (fillStack) {
            open(detail);
        } else if (detail == null) {
            throw new NullPointerException("detail == null");
        } else if (this != NOOP && (ENABLED ^ 1) == 0) {
            this.detailMessage = detail;
        }
    }

    public void close() {
        this.allocationSite = null;
        this.detailMessage = null;
    }

    public void warnIfOpen() {
        if (!(this.allocationSite == null && this.detailMessage == null) && (ENABLED ^ 1) == 0) {
            if (this.detailMessage != null) {
                REPORTER.report("A resource was acquired but never released", this.detailMessage);
            } else {
                REPORTER.report("A resource was acquired at attached stack trace but never released. See java.io.Closeable for information on avoiding resource leaks.", this.allocationSite);
            }
        }
    }
}
