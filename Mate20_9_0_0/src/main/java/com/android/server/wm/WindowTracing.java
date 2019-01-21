package com.android.server.wm;

import android.content.Context;
import android.os.Build;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.display.DisplayTransformManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class WindowTracing {
    private static final long MAGIC_NUMBER_VALUE = 4990904633914181975L;
    private static final String TAG = "WindowTracing";
    private boolean mEnabled;
    private volatile boolean mEnabledLockFree;
    private final Object mLock = new Object();
    private final File mTraceFile;
    private final BlockingQueue<ProtoOutputStream> mWriteQueue = new ArrayBlockingQueue(DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE);

    WindowTracing(File file) {
        this.mTraceFile = file;
    }

    /* JADX WARNING: Missing block: B:20:?, code skipped:
            $closeResource(r2, r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void startTrace(PrintWriter pw) throws IOException {
        if (Build.IS_USER) {
            logAndPrintln(pw, "Error: Tracing is not supported on user builds.");
            return;
        }
        synchronized (this.mLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Start tracing to ");
            stringBuilder.append(this.mTraceFile);
            stringBuilder.append(".");
            logAndPrintln(pw, stringBuilder.toString());
            this.mWriteQueue.clear();
            this.mTraceFile.delete();
            OutputStream os = new FileOutputStream(this.mTraceFile);
            this.mTraceFile.setReadable(true, false);
            ProtoOutputStream proto = new ProtoOutputStream(os);
            proto.write(1125281431553L, MAGIC_NUMBER_VALUE);
            proto.flush();
            $closeResource(null, os);
            this.mEnabledLockFree = true;
            this.mEnabled = true;
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private void logAndPrintln(PrintWriter pw, String msg) {
        Log.i(TAG, msg);
        if (pw != null) {
            pw.println(msg);
            pw.flush();
        }
    }

    void stopTrace(PrintWriter pw) {
        if (Build.IS_USER) {
            logAndPrintln(pw, "Error: Tracing is not supported on user builds.");
            return;
        }
        synchronized (this.mLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stop tracing to ");
            stringBuilder.append(this.mTraceFile);
            stringBuilder.append(". Waiting for traces to flush.");
            logAndPrintln(pw, stringBuilder.toString());
            this.mEnabledLockFree = false;
            this.mEnabled = false;
            while (!this.mWriteQueue.isEmpty()) {
                if (this.mEnabled) {
                    logAndPrintln(pw, "ERROR: tracing was re-enabled while waiting for flush.");
                    throw new IllegalStateException("tracing enabled while waiting for flush.");
                }
                try {
                    this.mLock.wait();
                    this.mLock.notify();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Trace written to ");
            stringBuilder.append(this.mTraceFile);
            stringBuilder.append(".");
            logAndPrintln(pw, stringBuilder.toString());
        }
    }

    void appendTraceEntry(ProtoOutputStream proto) {
        if (this.mEnabledLockFree && !this.mWriteQueue.offer(proto)) {
            Log.e(TAG, "Dropping window trace entry, queue full");
        }
    }

    void loop() {
        while (true) {
            loopOnce();
        }
    }

    @VisibleForTesting
    void loopOnce() {
        try {
            ProtoOutputStream proto = (ProtoOutputStream) this.mWriteQueue.take();
            synchronized (this.mLock) {
                OutputStream os;
                try {
                    Trace.traceBegin(32, "writeToFile");
                    os = new FileOutputStream(this.mTraceFile, true);
                    os.write(proto.getBytes());
                    $closeResource(null, os);
                    Trace.traceEnd(32);
                } catch (IOException e) {
                    try {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to write file ");
                        stringBuilder.append(this.mTraceFile);
                        Log.e(str, stringBuilder.toString(), e);
                        Trace.traceEnd(32);
                    } catch (Throwable th) {
                        Trace.traceEnd(32);
                    }
                } catch (Throwable th2) {
                    $closeResource(r5, os);
                }
                this.mLock.notify();
            }
        } catch (InterruptedException e2) {
            Thread.currentThread().interrupt();
        }
    }

    boolean isEnabled() {
        return this.mEnabledLockFree;
    }

    static WindowTracing createDefaultAndStartLooper(Context context) {
        WindowTracing windowTracing = new WindowTracing(new File("/data/misc/wmtrace/wm_trace.pb"));
        if (!Build.IS_USER) {
            Objects.requireNonNull(windowTracing);
            new Thread(new -$$Lambda$8kACnZAYfDhQTXwuOd2shUPmkTE(windowTracing), "window_tracing").start();
        }
        return windowTracing;
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x002f A:{Catch:{ IOException -> 0x004d }} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0036 A:{Catch:{ IOException -> 0x004d }} */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0032 A:{Catch:{ IOException -> 0x004d }} */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x002f A:{Catch:{ IOException -> 0x004d }} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0036 A:{Catch:{ IOException -> 0x004d }} */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0032 A:{Catch:{ IOException -> 0x004d }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int onShellCommand(ShellCommand shell, String cmd) {
        PrintWriter pw = shell.getOutPrintWriter();
        try {
            int hashCode = cmd.hashCode();
            if (hashCode == 3540994) {
                if (cmd.equals("stop")) {
                    hashCode = 1;
                    switch (hashCode) {
                        case 0:
                            break;
                        case 1:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == 109757538) {
                if (cmd.equals("start")) {
                    hashCode = 0;
                    switch (hashCode) {
                        case 0:
                            startTrace(pw);
                            return 0;
                        case 1:
                            stopTrace(pw);
                            return 0;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown command: ");
                            stringBuilder.append(cmd);
                            pw.println(stringBuilder.toString());
                            return -1;
                    }
                }
            }
            hashCode = -1;
            switch (hashCode) {
                case 0:
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            logAndPrintln(pw, e.toString());
            throw new RuntimeException(e);
        }
    }

    void traceStateLocked(String where, WindowManagerService service) {
        if (isEnabled()) {
            ProtoOutputStream os = new ProtoOutputStream();
            long tokenOuter = os.start(2246267895810L);
            os.write(1125281431553L, SystemClock.elapsedRealtimeNanos());
            os.write(1138166333442L, where);
            Trace.traceBegin(32, "writeToProtoLocked");
            try {
                long tokenInner = os.start(1146756268035L);
                service.writeToProtoLocked(os, true);
                os.end(tokenInner);
                Trace.traceEnd(32);
                os.end(tokenOuter);
                appendTraceEntry(os);
            } finally {
                Trace.traceEnd(32);
            }
        }
    }
}
