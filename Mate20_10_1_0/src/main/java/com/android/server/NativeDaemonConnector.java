package com.android.server;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.LocalLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.Watchdog;
import com.android.server.os.HwBootFail;
import com.android.server.power.ShutdownThread;
import com.google.android.collect.Lists;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

final class NativeDaemonConnector implements Runnable, Handler.Callback, Watchdog.Monitor {
    /* access modifiers changed from: private */
    public static boolean DEBUG_NETD = false;
    private static boolean DEBUG_ON = SystemProperties.getBoolean("persist.sys.huawei.debug.on", false);
    private static final long DEFAULT_TIMEOUT = 60000;
    private static final boolean VDBG = false;
    private static final long WARN_EXECUTE_DELAY_MS = 500;
    private static final String encryption_ip = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
    private final int BUFFER_SIZE;
    private final String TAG;
    private Handler mCallbackHandler;
    private INativeDaemonConnectorCallbacks mCallbacks;
    private final Object mDaemonLock;
    private volatile boolean mDebug;
    private LocalLog mLocalLog;
    private final Looper mLooper;
    private OutputStream mOutputStream;
    private final ResponseQueue mResponseQueue;
    private AtomicInteger mSequenceNumber;
    private String mSocket;
    private final PowerManager.WakeLock mWakeLock;
    private volatile Object mWarnIfHeld;
    private HwNativeDaemonConnector mhwNativeDaemonConnector;

    static {
        boolean z = false;
        if (Log.HWModuleLog || DEBUG_ON) {
            z = true;
        }
        DEBUG_NETD = z;
    }

    NativeDaemonConnector(INativeDaemonConnectorCallbacks callbacks, String socket, int responseQueueSize, String logTag, int maxLogSize, PowerManager.WakeLock wl) {
        this(callbacks, socket, responseQueueSize, logTag, maxLogSize, wl, FgThread.get().getLooper());
    }

    NativeDaemonConnector(INativeDaemonConnectorCallbacks callbacks, String socket, int responseQueueSize, String logTag, int maxLogSize, PowerManager.WakeLock wl, Looper looper) {
        this.mDebug = false;
        this.mDaemonLock = new Object();
        this.BUFFER_SIZE = 4096;
        this.mCallbacks = callbacks;
        this.mSocket = socket;
        this.mResponseQueue = new ResponseQueue(responseQueueSize);
        this.mWakeLock = wl;
        PowerManager.WakeLock wakeLock = this.mWakeLock;
        if (wakeLock != null) {
            wakeLock.setReferenceCounted(true);
        }
        this.mLooper = looper;
        this.mSequenceNumber = new AtomicInteger(0);
        this.TAG = logTag != null ? logTag : "NativeDaemonConnector";
        this.mLocalLog = new LocalLog(maxLogSize);
    }

    public void setDebug(boolean debug) {
        this.mDebug = debug;
    }

    private int uptimeMillisInt() {
        return ((int) SystemClock.uptimeMillis()) & HwBootFail.STAGE_BOOT_SUCCESS;
    }

    public void setWarnIfHeld(Object warnIfHeld) {
        Preconditions.checkState(this.mWarnIfHeld == null);
        this.mWarnIfHeld = Preconditions.checkNotNull(warnIfHeld);
    }

    public void run() {
        this.mCallbackHandler = new Handler(this.mLooper, this);
        while (!isShuttingDown()) {
            try {
                listenToSocket();
            } catch (Exception e) {
                loge("Error in NativeDaemonConnector: " + e);
                if (!isShuttingDown()) {
                    SystemClock.sleep(5000);
                } else {
                    return;
                }
            }
        }
    }

    private static boolean isShuttingDown() {
        String shutdownAct = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, "");
        return shutdownAct != null && shutdownAct.length() > 0;
    }

    public boolean handleMessage(Message msg) {
        Object[] objArr;
        PowerManager.WakeLock wakeLock;
        int end;
        PowerManager.WakeLock wakeLock2;
        PowerManager.WakeLock wakeLock3;
        String event = (String) msg.obj;
        event.replaceAll(encryption_ip, " ******** ");
        log("RCV unsolicited event from native daemon, event = " + msg.what);
        int start = uptimeMillisInt();
        int sent = msg.arg1;
        try {
            if (!this.mCallbacks.onEvent(msg.what, event, NativeDaemonEvent.unescapeArgs(event))) {
                log(String.format("Unhandled event '%s'", event));
            }
            if (this.mCallbacks.onCheckHoldWakeLock(msg.what) && (wakeLock3 = this.mWakeLock) != null) {
                wakeLock3.release();
            }
            int end2 = uptimeMillisInt();
            if (start > sent && ((long) (start - sent)) > 500) {
                loge(String.format("NDC event {%s} processed too late: %dms", event, Integer.valueOf(start - sent)));
            }
            if (end2 > start && ((long) (end2 - start)) > 500) {
                objArr = new Object[]{event, Integer.valueOf(end2 - start)};
                loge(String.format("NDC event {%s} took too long: %dms", objArr));
            }
        } catch (Exception e) {
            loge("Error handling '" + event + "': " + e);
            if (this.mCallbacks.onCheckHoldWakeLock(msg.what) && (wakeLock = this.mWakeLock) != null) {
                wakeLock.release();
            }
            int end3 = uptimeMillisInt();
            if (start > sent && ((long) (start - sent)) > 500) {
                loge(String.format("NDC event {%s} processed too late: %dms", event, Integer.valueOf(start - sent)));
            }
            if (end3 > start && ((long) (end3 - start)) > 500) {
                objArr = new Object[]{event, Integer.valueOf(end3 - start)};
            }
        } catch (Throwable th) {
            if (this.mCallbacks.onCheckHoldWakeLock(msg.what) && (wakeLock2 = this.mWakeLock) != null) {
                wakeLock2.release();
            }
            int end4 = uptimeMillisInt();
            if (start > sent) {
                end = end4;
                if (((long) (start - sent)) > 500) {
                    loge(String.format("NDC event {%s} processed too late: %dms", event, Integer.valueOf(start - sent)));
                }
            } else {
                end = end4;
            }
            if (end > start && ((long) (end - start)) > 500) {
                loge(String.format("NDC event {%s} took too long: %dms", event, Integer.valueOf(end - start)));
            }
            throw th;
        }
        return true;
    }

    private LocalSocketAddress determineSocketAddress() {
        if (!this.mSocket.startsWith("__test__") || !Build.IS_DEBUGGABLE) {
            return new LocalSocketAddress(this.mSocket, LocalSocketAddress.Namespace.RESERVED);
        }
        return new LocalSocketAddress(this.mSocket);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:127:0x02ef, code lost:
        r0 = th;
     */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x028f  */
    /* JADX WARNING: Removed duplicated region for block: B:107:0x0299  */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x0319 A[SYNTHETIC] */
    private void listenToSocket() throws IOException {
        LocalSocket socket;
        Throwable th;
        int count;
        boolean z;
        char c;
        int i;
        InputStream inputStream;
        LocalSocketAddress address;
        String rawEventLog;
        PowerManager.WakeLock wakeLock;
        LocalSocket socket2 = null;
        log("listenToSocket enter");
        OutputStream outputStream = null;
        try {
            socket2 = new LocalSocket();
            try {
                LocalSocketAddress address2 = determineSocketAddress();
                socket2.connect(address2);
                InputStream inputStream2 = socket2.getInputStream();
                synchronized (this.mDaemonLock) {
                    this.mOutputStream = socket2.getOutputStream();
                }
                this.mCallbacks.onDaemonConnected();
                byte[] buffer = new byte[4096];
                int start = 0;
                while (true) {
                    count = inputStream2.read(buffer, start, 4096 - start);
                    if (count < 0) {
                        break;
                    }
                    FileDescriptor[] fdList = socket2.getAncillaryFileDescriptors();
                    int count2 = count + start;
                    int i2 = 0;
                    int start2 = 0;
                    while (i2 < count2) {
                        if (buffer[i2] == 0) {
                            String rawEvent = new String(buffer, start2, i2 - start2, StandardCharsets.UTF_8);
                            Pattern pattern = Pattern.compile("([0-9a-zA-Z]{2}:){4}[0-9a-zA-Z]{2}");
                            String rawEventLog2 = rawEvent.replaceAll(encryption_ip, " ******** ");
                            if (pattern.matcher(rawEventLog2).find()) {
                                String rawEventLog3 = rawEventLog2.replaceAll("([0-9a-zA-Z]{2}:){4}[0-9a-zA-Z]{2}", " ******** ");
                                log("RCV <- {" + rawEventLog3 + "}");
                                rawEventLog = rawEventLog3;
                            } else if (!shouldPrintEvent()) {
                                String rawEventLog4 = rawEventLog2.replaceAll("[A-Fa-f0-9]{2,}:{1,}", "****");
                                log("RCV <- {" + rawEventLog4 + "}");
                                rawEventLog = rawEventLog4;
                            } else {
                                rawEventLog = rawEventLog2;
                            }
                            boolean releaseWl = false;
                            try {
                                NativeDaemonEvent event = NativeDaemonEvent.parseRawEvent(rawEvent, fdList);
                                socket = socket2;
                                try {
                                    address = address2;
                                    inputStream = inputStream2;
                                    try {
                                        String eventLog = event.toString().replaceAll(encryption_ip, " ******** ");
                                        if (shouldPrintEvent()) {
                                            try {
                                                log("RCV <- {" + event + "}");
                                            } catch (IllegalArgumentException e) {
                                                e = e;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                if (releaseWl) {
                                                }
                                                throw th;
                                            }
                                        } else if (pattern.matcher(eventLog).find()) {
                                            eventLog = eventLog.replaceAll("([0-9a-zA-Z]{2}:){4}[0-9a-zA-Z]{2}", " ******** ");
                                            log("RCV <- {" + eventLog + "}");
                                        } else {
                                            eventLog = eventLog.replaceAll("[A-Fa-f0-9]{2,}:{1,}", "****");
                                            log("RCV <- {" + eventLog + "}");
                                        }
                                        if (event.isClassUnsolicited()) {
                                            if (this.mCallbacks.onCheckHoldWakeLock(event.getCode()) && this.mWakeLock != null) {
                                                this.mWakeLock.acquire();
                                                releaseWl = true;
                                            }
                                            try {
                                                try {
                                                    if (this.mCallbackHandler.sendMessage(this.mCallbackHandler.obtainMessage(event.getCode(), uptimeMillisInt(), 0, event.getRawEvent()))) {
                                                        releaseWl = false;
                                                    }
                                                } catch (IllegalArgumentException e2) {
                                                    e = e2;
                                                    try {
                                                        log("Problem parsing message " + e);
                                                        if (releaseWl) {
                                                        }
                                                        start2 = i2 + 1;
                                                        i2++;
                                                        socket2 = socket;
                                                        address2 = address;
                                                        inputStream2 = inputStream;
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                        if (releaseWl) {
                                                        }
                                                        throw th;
                                                    }
                                                }
                                            } catch (IllegalArgumentException e3) {
                                                e = e3;
                                                log("Problem parsing message " + e);
                                                if (releaseWl) {
                                                }
                                                start2 = i2 + 1;
                                                i2++;
                                                socket2 = socket;
                                                address2 = address;
                                                inputStream2 = inputStream;
                                            } catch (Throwable th4) {
                                                th = th4;
                                                if (releaseWl) {
                                                }
                                                throw th;
                                            }
                                        } else {
                                            this.mResponseQueue.add(event.getCmdNumber(), event);
                                        }
                                        if (releaseWl) {
                                            try {
                                                wakeLock = this.mWakeLock;
                                                wakeLock.release();
                                            } catch (IOException e4) {
                                                ex = e4;
                                                socket2 = socket;
                                                try {
                                                    loge("Communications error: " + ex);
                                                    throw ex;
                                                } catch (Throwable th5) {
                                                    socket = socket2;
                                                    th = th5;
                                                    synchronized (this.mDaemonLock) {
                                                    }
                                                }
                                            } catch (Throwable th6) {
                                                th = th6;
                                                synchronized (this.mDaemonLock) {
                                                }
                                            }
                                        }
                                    } catch (IllegalArgumentException e5) {
                                        e = e5;
                                        log("Problem parsing message " + e);
                                        if (releaseWl) {
                                        }
                                        start2 = i2 + 1;
                                        i2++;
                                        socket2 = socket;
                                        address2 = address;
                                        inputStream2 = inputStream;
                                    } catch (Throwable th7) {
                                        th = th7;
                                        if (releaseWl) {
                                        }
                                        throw th;
                                    }
                                } catch (IllegalArgumentException e6) {
                                    e = e6;
                                    address = address2;
                                    inputStream = inputStream2;
                                    log("Problem parsing message " + e);
                                    if (releaseWl) {
                                    }
                                    start2 = i2 + 1;
                                    i2++;
                                    socket2 = socket;
                                    address2 = address;
                                    inputStream2 = inputStream;
                                } catch (Throwable th8) {
                                    th = th8;
                                    if (releaseWl) {
                                    }
                                    throw th;
                                }
                            } catch (IllegalArgumentException e7) {
                                e = e7;
                                socket = socket2;
                                address = address2;
                                inputStream = inputStream2;
                                log("Problem parsing message " + e);
                                if (releaseWl) {
                                    wakeLock = this.mWakeLock;
                                    wakeLock.release();
                                }
                                start2 = i2 + 1;
                                i2++;
                                socket2 = socket;
                                address2 = address;
                                inputStream2 = inputStream;
                            } catch (Throwable th9) {
                                th = th9;
                                if (releaseWl) {
                                    this.mWakeLock.release();
                                }
                                throw th;
                            }
                            start2 = i2 + 1;
                        } else {
                            socket = socket2;
                            address = address2;
                            inputStream = inputStream2;
                        }
                        i2++;
                        socket2 = socket;
                        address2 = address;
                        inputStream2 = inputStream;
                    }
                    if (start2 == 0) {
                        log("RCV incomplete");
                    }
                    if (start2 != count2) {
                        c = 4096;
                        int remaining = 4096 - start2;
                        z = false;
                        System.arraycopy(buffer, start2, buffer, 0, remaining);
                        i = remaining;
                    } else {
                        c = 4096;
                        z = false;
                        i = 0;
                    }
                    start = i;
                    socket2 = socket2;
                    address2 = address2;
                    inputStream2 = inputStream2;
                    outputStream = null;
                }
                loge("got " + count + " reading with start = " + start);
                synchronized (this.mDaemonLock) {
                    if (this.mOutputStream != null) {
                        try {
                            loge("closing stream for " + this.mSocket);
                            this.mOutputStream.close();
                        } catch (IOException e8) {
                            loge("Failed closing output stream: " + e8);
                        }
                        this.mOutputStream = outputStream;
                    }
                }
                try {
                    socket2.close();
                    return;
                } catch (IOException ex) {
                    loge("Failed closing socket: " + ex);
                    return;
                }
                while (true) {
                }
            } catch (IOException e9) {
                ex = e9;
                loge("Communications error: " + ex);
                throw ex;
            } catch (Throwable th10) {
                socket = socket2;
                th = th10;
                synchronized (this.mDaemonLock) {
                    if (this.mOutputStream != null) {
                        try {
                            loge("closing stream for " + this.mSocket);
                            this.mOutputStream.close();
                        } catch (IOException e10) {
                            loge("Failed closing output stream: " + e10);
                        }
                        this.mOutputStream = null;
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ex2) {
                        loge("Failed closing socket: " + ex2);
                    }
                }
                throw th;
            }
        } catch (IOException e11) {
            ex = e11;
            loge("Communications error: " + ex);
            throw ex;
        }
    }

    public static class SensitiveArg {
        private final Object mArg;

        public SensitiveArg(Object arg) {
            this.mArg = arg;
        }

        public String toString() {
            return String.valueOf(this.mArg);
        }
    }

    @VisibleForTesting
    static void makeCommand(StringBuilder rawBuilder, StringBuilder logBuilder, int sequenceNumber, String cmd, Object... args) {
        if (cmd.indexOf(0) >= 0) {
            if (DEBUG_NETD) {
                Slog.e("NDC makeCommand", "Unexpected command, cmd = " + cmd);
            }
            throw new IllegalArgumentException("Unexpected command: " + cmd);
        } else if (cmd.indexOf(32) >= 0) {
            if (DEBUG_NETD) {
                Slog.e("NDC makeCommand", "Error, arguments must be separate from command");
            }
            throw new IllegalArgumentException("Arguments must be separate from command");
        } else {
            rawBuilder.append(sequenceNumber);
            rawBuilder.append(' ');
            rawBuilder.append(cmd);
            logBuilder.append(sequenceNumber);
            logBuilder.append(' ');
            logBuilder.append(cmd);
            int length = args.length;
            int i = 0;
            while (i < length) {
                Object arg = args[i];
                String argString = String.valueOf(arg);
                if (argString.indexOf(0) < 0) {
                    rawBuilder.append(' ');
                    logBuilder.append(' ');
                    appendEscaped(rawBuilder, argString);
                    if (arg instanceof SensitiveArg) {
                        logBuilder.append("[scrubbed]");
                    } else {
                        appendEscaped(logBuilder, argString);
                    }
                    i++;
                } else {
                    Slog.e("NDC makeCommand", "Unexpected argument: " + arg);
                    throw new IllegalArgumentException("Unexpected argument: " + arg);
                }
            }
            rawBuilder.append((char) 0);
        }
    }

    public void waitForCallbacks() {
        if (Thread.currentThread() != this.mLooper.getThread()) {
            final CountDownLatch latch = new CountDownLatch(1);
            this.mCallbackHandler.post(new Runnable() {
                /* class com.android.server.NativeDaemonConnector.AnonymousClass1 */

                public void run() {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Slog.wtf(this.TAG, "Interrupted while waiting for unsolicited response handling", e);
            }
        } else {
            throw new IllegalStateException("Must not call this method on callback thread");
        }
    }

    public NativeDaemonEvent execute(Command cmd) throws NativeDaemonConnectorException {
        return execute(cmd.mCmd, cmd.mArguments.toArray());
    }

    public NativeDaemonEvent execute(String cmd, Object... args) throws NativeDaemonConnectorException {
        return execute(60000, cmd, args);
    }

    public NativeDaemonEvent execute(long timeoutMs, String cmd, Object... args) throws NativeDaemonConnectorException {
        NativeDaemonEvent[] events = executeForList(timeoutMs, cmd, args);
        if (events.length == 1) {
            return events[0];
        }
        loge("Expected exactly one response, but receive more, throw NativeDaemonConnectorException");
        throw new NativeDaemonConnectorException("Expected exactly one response, but received " + events.length);
    }

    public NativeDaemonEvent[] executeForList(Command cmd) throws NativeDaemonConnectorException {
        return executeForList(cmd.mCmd, cmd.mArguments.toArray());
    }

    public NativeDaemonEvent[] executeForList(String cmd, Object... args) throws NativeDaemonConnectorException {
        return executeForList(60000, cmd, args);
    }

    private int countCharacterAppearTimes(String str, String character) {
        int x = 0;
        for (int i = 0; i <= str.length() - 1; i++) {
            if (str.substring(i, i + 1).equals(character)) {
                x++;
            }
        }
        return x;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x00d4, code lost:
        r0 = r21.mResponseQueue.remove(r7, r22, r11);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x00df, code lost:
        if (r0 != null) goto L_0x0104;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x00e1, code lost:
        loge("timed-out waiting for response to " + r11);
        r5 = r21.mhwNativeDaemonConnector;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x00f8, code lost:
        if (r5 == null) goto L_0x00fe;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x00fa, code lost:
        r5.reportChrForAddRouteFail(r11, null);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0103, code lost:
        throw new com.android.server.NativeDaemonTimeoutException(r11, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0104, code lost:
        r5 = "RMV <- {" + r0 + "}";
        r6 = r21.mhwNativeDaemonConnector;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x011d, code lost:
        if (r6 == null) goto L_0x0122;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x011f, code lost:
        r6.reportChrForAddRouteFail(r11, r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0122, code lost:
        r4.add(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0129, code lost:
        if (r0.isClassContinue() != false) goto L_0x0190;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x012b, code lost:
        r5 = android.os.SystemClock.elapsedRealtime();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0135, code lost:
        if ((r5 - r2) <= 500) goto L_0x015f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0137, code lost:
        loge("NDC Command {" + r11 + "} took too long (" + (r5 - r2) + "ms)");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x0165, code lost:
        if (r0.isClassClientError() != false) goto L_0x0185;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x016b, code lost:
        if (r0.isClassServerError() != false) goto L_0x017a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x0179, code lost:
        return (com.android.server.NativeDaemonEvent[]) r4.toArray(new com.android.server.NativeDaemonEvent[r4.size()]);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x017a, code lost:
        loge("NDC server error throw NativeDaemonFailureException");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x0184, code lost:
        throw new com.android.server.NativeDaemonConnector.NativeDaemonFailureException(r11, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x0185, code lost:
        loge("NDC client error throw NativeDaemonArgumentException");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x018f, code lost:
        throw new com.android.server.NativeDaemonConnector.NativeDaemonArgumentException(r11, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x0190, code lost:
        r5 = r5;
        r6 = r6;
     */
    public NativeDaemonEvent[] executeForList(long timeoutMs, String cmd, Object... args) throws NativeDaemonConnectorException {
        if (this.mWarnIfHeld != null && Thread.holdsLock(this.mWarnIfHeld)) {
            Slog.wtf(this.TAG, "Calling thread " + Thread.currentThread().getName() + " is holding 0x" + Integer.toHexString(System.identityHashCode(this.mWarnIfHeld)), new Throwable());
        }
        long startTime = SystemClock.elapsedRealtime();
        ArrayList<NativeDaemonEvent> events = Lists.newArrayList();
        StringBuilder rawBuilder = new StringBuilder();
        StringBuilder logBuilder = new StringBuilder();
        int sequenceNumber = this.mSequenceNumber.incrementAndGet();
        this.mhwNativeDaemonConnector = HwServiceFactory.getHwNativeDaemonConnector();
        makeCommand(rawBuilder, logBuilder, sequenceNumber, cmd, args);
        String rawCmd = rawBuilder.toString();
        String logCmd = logBuilder.toString();
        String encryption_logCmd = logCmd.replaceAll(encryption_ip, " ******** ").replaceAll("([A-Fa-f0-9]{2,}:{1,}){2,}", " **** ");
        if (shouldPrintEvent()) {
            log("SND -> {" + logCmd + "}");
        } else {
            log("SND -> {" + encryption_logCmd + "}");
        }
        synchronized (this.mDaemonLock) {
            try {
                if (this.mOutputStream != null) {
                    try {
                        this.mOutputStream.write(rawCmd.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        loge("NDC problem sending command throw NativeDaemonConnectorException");
                        throw new NativeDaemonConnectorException("problem sending command", e);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } else {
                    loge("NDC missing output stream throw NativeDaemonConnectorException");
                    throw new NativeDaemonConnectorException("missing output stream");
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    @Deprecated
    public ArrayList<String> doCommand(String cmd, Object... args) throws NativeDaemonConnectorException {
        ArrayList<String> rawEvents = Lists.newArrayList();
        for (NativeDaemonEvent event : executeForList(cmd, args)) {
            rawEvents.add(event.getRawEvent());
        }
        return rawEvents;
    }

    @Deprecated
    public String[] doListCommand(String cmd, int expectedCode, Object... args) throws NativeDaemonConnectorException {
        ArrayList<String> list = Lists.newArrayList();
        NativeDaemonEvent[] events = executeForList(cmd, args);
        int i = 0;
        while (i < events.length - 1) {
            NativeDaemonEvent event = events[i];
            int code = event.getCode();
            if (code == expectedCode) {
                list.add(event.getMessage());
                i++;
            } else {
                throw new NativeDaemonConnectorException("unexpected list response " + code + " instead of " + expectedCode);
            }
        }
        NativeDaemonEvent finalEvent = events[events.length - 1];
        if (finalEvent.isClassOk()) {
            return (String[]) list.toArray(new String[list.size()]);
        }
        throw new NativeDaemonConnectorException("unexpected final event: " + finalEvent);
    }

    @VisibleForTesting
    static void appendEscaped(StringBuilder builder, String arg) {
        boolean hasSpaces = arg.indexOf(32) >= 0;
        if (hasSpaces) {
            builder.append('\"');
        }
        int length = arg.length();
        for (int i = 0; i < length; i++) {
            char c = arg.charAt(i);
            if (c == '\"') {
                builder.append("\\\"");
            } else if (c == '\\') {
                builder.append("\\\\");
            } else {
                builder.append(c);
            }
        }
        if (hasSpaces) {
            builder.append('\"');
        }
    }

    private static class NativeDaemonArgumentException extends NativeDaemonConnectorException {
        public NativeDaemonArgumentException(String command, NativeDaemonEvent event) {
            super(command, event);
        }

        @Override // com.android.server.NativeDaemonConnectorException
        public IllegalArgumentException rethrowAsParcelableException() {
            throw new IllegalArgumentException(getMessage(), this);
        }
    }

    private static class NativeDaemonFailureException extends NativeDaemonConnectorException {
        public NativeDaemonFailureException(String command, NativeDaemonEvent event) {
            super(command, event);
        }
    }

    public static class Command {
        /* access modifiers changed from: private */
        public ArrayList<Object> mArguments = Lists.newArrayList();
        /* access modifiers changed from: private */
        public String mCmd;

        public Command(String cmd, Object... args) {
            this.mCmd = cmd;
            for (Object arg : args) {
                appendArg(arg);
            }
        }

        public Command appendArg(Object arg) {
            this.mArguments.add(arg);
            return this;
        }
    }

    @Override // com.android.server.Watchdog.Monitor
    public void monitor() {
        synchronized (this.mDaemonLock) {
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mLocalLog.dump(fd, pw, args);
        pw.println();
        this.mResponseQueue.dump(fd, pw, args);
    }

    private void log(String logstring) {
        if (this.mDebug) {
            Slog.d(this.TAG, logstring);
        }
        if (DEBUG_NETD) {
            this.mLocalLog.log(logstring);
        }
    }

    private void loge(String logstring) {
        if (DEBUG_NETD) {
            Slog.e(this.TAG, logstring);
            this.mLocalLog.log(logstring);
        }
    }

    private static class ResponseQueue {
        private int mMaxCount;
        private final LinkedList<PendingCmd> mPendingCmds = new LinkedList<>();

        private static class PendingCmd {
            public int availableResponseCount;
            public final int cmdNum;
            public final String logCmd;
            public BlockingQueue<NativeDaemonEvent> responses = new ArrayBlockingQueue(20);

            public PendingCmd(int cmdNum2, String logCmd2) {
                this.cmdNum = cmdNum2;
                this.logCmd = logCmd2;
            }
        }

        ResponseQueue(int maxCount) {
            this.mMaxCount = maxCount;
        }

        public void add(int cmdNum, NativeDaemonEvent response) {
            PendingCmd found = null;
            synchronized (this.mPendingCmds) {
                Iterator<PendingCmd> it = this.mPendingCmds.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    PendingCmd pendingCmd = it.next();
                    if (pendingCmd.cmdNum == cmdNum) {
                        found = pendingCmd;
                        break;
                    }
                }
                if (this.mPendingCmds.size() == 11) {
                    Slog.d("NativeDaemonConnector.ResponseQueue", "mPendingCmds.size = " + this.mPendingCmds.size());
                }
                if (found == null) {
                    while (this.mPendingCmds.size() >= this.mMaxCount) {
                        Slog.e("NativeDaemonConnector.ResponseQueue", "more buffered than allowed: " + this.mPendingCmds.size() + " >= " + this.mMaxCount);
                        PendingCmd pendingCmd2 = this.mPendingCmds.remove();
                        Slog.e("NativeDaemonConnector.ResponseQueue", "Removing request: " + pendingCmd2.logCmd + " (" + pendingCmd2.cmdNum + ")");
                    }
                    found = new PendingCmd(cmdNum, null);
                    this.mPendingCmds.add(found);
                }
                found.availableResponseCount++;
                if (found.availableResponseCount == 0) {
                    this.mPendingCmds.remove(found);
                }
            }
            try {
                found.responses.put(response);
            } catch (InterruptedException e) {
                if (NativeDaemonConnector.DEBUG_NETD) {
                    Slog.e("NDC put", "InterruptedException happen");
                }
            }
        }

        public NativeDaemonEvent remove(int cmdNum, long timeoutMs, String logCmd) {
            PendingCmd found = null;
            synchronized (this.mPendingCmds) {
                Iterator<PendingCmd> it = this.mPendingCmds.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    PendingCmd pendingCmd = it.next();
                    if (pendingCmd.cmdNum == cmdNum) {
                        found = pendingCmd;
                        break;
                    }
                }
                if (found == null) {
                    found = new PendingCmd(cmdNum, logCmd);
                    this.mPendingCmds.add(found);
                }
                found.availableResponseCount--;
                if (found.availableResponseCount == 0) {
                    this.mPendingCmds.remove(found);
                }
            }
            NativeDaemonEvent result = null;
            try {
                result = found.responses.poll(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                if (NativeDaemonConnector.DEBUG_NETD) {
                    Slog.e("NDC poll", "InterruptedException happen");
                }
            }
            if (result == null) {
                Slog.e("NativeDaemonConnector.ResponseQueue", "Timeout waiting for response");
            }
            return result;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println("Pending requests:");
            synchronized (this.mPendingCmds) {
                Iterator<PendingCmd> it = this.mPendingCmds.iterator();
                while (it.hasNext()) {
                    PendingCmd pendingCmd = it.next();
                    pw.println("  Cmd " + pendingCmd.cmdNum + " - " + pendingCmd.logCmd);
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x003a A[ADDED_TO_REGION] */
    private boolean shouldPrintEvent() {
        char c;
        String str = this.TAG;
        int hashCode = str.hashCode();
        if (hashCode != -1339953786) {
            if (hashCode != 136483132) {
                if (hashCode == 1813268503 && str.equals("CryptdConnector")) {
                    c = 2;
                    return c != 0 || c == 1 || c == 2;
                }
            } else if (str.equals("VoldConnector")) {
                c = 1;
                if (c != 0) {
                }
            }
        } else if (str.equals("SdCryptdConnector")) {
            c = 0;
            if (c != 0) {
            }
        }
        c = 65535;
        if (c != 0) {
        }
    }
}
