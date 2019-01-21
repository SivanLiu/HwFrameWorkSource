package com.android.server;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.os.Build;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.LocalLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.Watchdog.Monitor;
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

final class NativeDaemonConnector implements Runnable, Callback, Monitor {
    private static boolean DEBUG_NETD = false;
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
    private final WakeLock mWakeLock;
    private volatile Object mWarnIfHeld;
    private HwNativeDaemonConnector mhwNativeDaemonConnector;

    public static class Command {
        private ArrayList<Object> mArguments = Lists.newArrayList();
        private String mCmd;

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

    private static class ResponseQueue {
        private int mMaxCount;
        private final LinkedList<PendingCmd> mPendingCmds = new LinkedList();

        private static class PendingCmd {
            public int availableResponseCount;
            public final int cmdNum;
            public final String logCmd;
            public BlockingQueue<NativeDaemonEvent> responses = new ArrayBlockingQueue(20);

            public PendingCmd(int cmdNum, String logCmd) {
                this.cmdNum = cmdNum;
                this.logCmd = logCmd;
            }
        }

        ResponseQueue(int maxCount) {
            this.mMaxCount = maxCount;
        }

        public void add(int cmdNum, NativeDaemonEvent response) {
            PendingCmd found = null;
            synchronized (this.mPendingCmds) {
                StringBuilder stringBuilder;
                Iterator it = this.mPendingCmds.iterator();
                while (it.hasNext()) {
                    PendingCmd pendingCmd = (PendingCmd) it.next();
                    if (pendingCmd.cmdNum == cmdNum) {
                        found = pendingCmd;
                        break;
                    }
                }
                if (this.mPendingCmds.size() == 11) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mPendingCmds.size = ");
                    stringBuilder.append(this.mPendingCmds.size());
                    Slog.d("NativeDaemonConnector.ResponseQueue", stringBuilder.toString());
                }
                if (found == null) {
                    while (this.mPendingCmds.size() >= this.mMaxCount) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("more buffered than allowed: ");
                        stringBuilder.append(this.mPendingCmds.size());
                        stringBuilder.append(" >= ");
                        stringBuilder.append(this.mMaxCount);
                        Slog.e("NativeDaemonConnector.ResponseQueue", stringBuilder.toString());
                        PendingCmd pendingCmd2 = (PendingCmd) this.mPendingCmds.remove();
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Removing request: ");
                        stringBuilder2.append(pendingCmd2.logCmd);
                        stringBuilder2.append(" (");
                        stringBuilder2.append(pendingCmd2.cmdNum);
                        stringBuilder2.append(")");
                        Slog.e("NativeDaemonConnector.ResponseQueue", stringBuilder2.toString());
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
                Iterator it = this.mPendingCmds.iterator();
                while (it.hasNext()) {
                    PendingCmd pendingCmd = (PendingCmd) it.next();
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
                result = (NativeDaemonEvent) found.responses.poll(timeoutMs, TimeUnit.MILLISECONDS);
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
                Iterator it = this.mPendingCmds.iterator();
                while (it.hasNext()) {
                    PendingCmd pendingCmd = (PendingCmd) it.next();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  Cmd ");
                    stringBuilder.append(pendingCmd.cmdNum);
                    stringBuilder.append(" - ");
                    stringBuilder.append(pendingCmd.logCmd);
                    pw.println(stringBuilder.toString());
                }
            }
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

    private static class NativeDaemonArgumentException extends NativeDaemonConnectorException {
        public NativeDaemonArgumentException(String command, NativeDaemonEvent event) {
            super(command, event);
        }

        public IllegalArgumentException rethrowAsParcelableException() {
            throw new IllegalArgumentException(getMessage(), this);
        }
    }

    private static class NativeDaemonFailureException extends NativeDaemonConnectorException {
        public NativeDaemonFailureException(String command, NativeDaemonEvent event) {
            super(command, event);
        }
    }

    static {
        boolean z = false;
        if (Log.HWModuleLog || DEBUG_ON) {
            z = true;
        }
        DEBUG_NETD = z;
    }

    NativeDaemonConnector(INativeDaemonConnectorCallbacks callbacks, String socket, int responseQueueSize, String logTag, int maxLogSize, WakeLock wl) {
        this(callbacks, socket, responseQueueSize, logTag, maxLogSize, wl, FgThread.get().getLooper());
    }

    NativeDaemonConnector(INativeDaemonConnectorCallbacks callbacks, String socket, int responseQueueSize, String logTag, int maxLogSize, WakeLock wl, Looper looper) {
        this.mDebug = false;
        this.mDaemonLock = new Object();
        this.BUFFER_SIZE = 4096;
        this.mCallbacks = callbacks;
        this.mSocket = socket;
        this.mResponseQueue = new ResponseQueue(responseQueueSize);
        this.mWakeLock = wl;
        if (this.mWakeLock != null) {
            this.mWakeLock.setReferenceCounted(true);
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
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error in NativeDaemonConnector: ");
                stringBuilder.append(e);
                loge(stringBuilder.toString());
                if (!isShuttingDown()) {
                    SystemClock.sleep(5000);
                } else {
                    return;
                }
            }
        }
    }

    private static boolean isShuttingDown() {
        String shutdownAct = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        return shutdownAct != null && shutdownAct.length() > 0;
    }

    public boolean handleMessage(Message msg) {
        String event = msg.obj;
        String encryption_event = event.replaceAll(encryption_ip, " ******** ");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RCV unsolicited event from native daemon, event = ");
        stringBuilder.append(msg.what);
        log(stringBuilder.toString());
        int start = uptimeMillisInt();
        int sent = msg.arg1;
        int end;
        String str;
        Object[] objArr;
        try {
            if (!this.mCallbacks.onEvent(msg.what, event, NativeDaemonEvent.unescapeArgs(event))) {
                log(String.format("Unhandled event '%s'", new Object[]{event}));
            }
            if (this.mCallbacks.onCheckHoldWakeLock(msg.what) && this.mWakeLock != null) {
                this.mWakeLock.release();
            }
            end = uptimeMillisInt();
            if (start > sent && ((long) (start - sent)) > 500) {
                loge(String.format("NDC event {%s} processed too late: %dms", new Object[]{event, Integer.valueOf(start - sent)}));
            }
            if (end > start && ((long) (end - start)) > 500) {
                str = "NDC event {%s} took too long: %dms";
                objArr = new Object[]{event, Integer.valueOf(end - start)};
                loge(String.format(str, objArr));
            }
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error handling '");
            stringBuilder2.append(event);
            stringBuilder2.append("': ");
            stringBuilder2.append(e);
            loge(stringBuilder2.toString());
            if (this.mCallbacks.onCheckHoldWakeLock(msg.what) && this.mWakeLock != null) {
                this.mWakeLock.release();
            }
            end = uptimeMillisInt();
            if (start > sent && ((long) (start - sent)) > 500) {
                loge(String.format("NDC event {%s} processed too late: %dms", new Object[]{event, Integer.valueOf(start - sent)}));
            }
            if (end > start && ((long) (end - start)) > 500) {
                str = "NDC event {%s} took too long: %dms";
                objArr = new Object[]{event, Integer.valueOf(end - start)};
            }
        } catch (Throwable th) {
            if (this.mCallbacks.onCheckHoldWakeLock(msg.what) && this.mWakeLock != null) {
                this.mWakeLock.release();
            }
            int end2 = uptimeMillisInt();
            if (start > sent && ((long) (start - sent)) > 500) {
                loge(String.format("NDC event {%s} processed too late: %dms", new Object[]{event, Integer.valueOf(start - sent)}));
            }
            if (end2 > start && ((long) (end2 - start)) > 500) {
                loge(String.format("NDC event {%s} took too long: %dms", new Object[]{event, Integer.valueOf(end2 - start)}));
            }
        }
        return true;
    }

    private LocalSocketAddress determineSocketAddress() {
        if (this.mSocket.startsWith("__test__") && Build.IS_DEBUGGABLE) {
            return new LocalSocketAddress(this.mSocket);
        }
        return new LocalSocketAddress(this.mSocket, Namespace.RESERVED);
    }

    /* JADX WARNING: Removed duplicated region for block: B:160:0x02fd A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:171:0x0338 A:{SYNTHETIC, Splitter:B:171:0x0338} */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x02fd A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:171:0x0338 A:{SYNTHETIC, Splitter:B:171:0x0338} */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x0276 A:{SYNTHETIC, Splitter:B:119:0x0276} */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x0280 A:{Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }} */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x0276 A:{SYNTHETIC, Splitter:B:119:0x0276} */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x0276 A:{SYNTHETIC, Splitter:B:119:0x0276} */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x0280 A:{Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }} */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x0280 A:{Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }} */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x0276 A:{SYNTHETIC, Splitter:B:119:0x0276} */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x0280 A:{Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }} */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x0280 A:{Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void listenToSocket() throws IOException {
        LocalSocket socket;
        LocalSocketAddress localSocketAddress;
        InputStream inputStream;
        IOException ex;
        Throwable th;
        LocalSocket socket2;
        IllegalArgumentException e;
        Pattern pattern;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        LocalSocket socket3 = null;
        log("listenToSocket enter");
        OutputStream outputStream = null;
        try {
            socket3 = new LocalSocket();
            StringBuilder stringBuilder3;
            StringBuilder stringBuilder4;
            try {
                int count;
                StringBuilder stringBuilder5;
                LocalSocketAddress address = determineSocketAddress();
                socket3.connect(address);
                InputStream inputStream2 = socket3.getInputStream();
                synchronized (this.mDaemonLock) {
                    try {
                        this.mOutputStream = socket3.getOutputStream();
                    } catch (IOException e2) {
                        ex = e2;
                        socket3 = socket;
                    } catch (Throwable th2) {
                        socket3 = th2;
                        socket2 = socket;
                        synchronized (this.mDaemonLock) {
                        }
                        if (socket2 != null) {
                        }
                        throw socket3;
                    }
                }
                this.mCallbacks.onDaemonConnected();
                byte[] buffer = new byte[4096];
                FileDescriptor[] fdList = null;
                int start = 0;
                while (true) {
                    count = inputStream2.read(buffer, start, 4096 - start);
                    if (count < 0) {
                        break;
                    }
                    Object obj;
                    fdList = socket3.getAncillaryFileDescriptors();
                    count += start;
                    int start2 = 0;
                    start = 0;
                    while (true) {
                        int i = start;
                        if (i >= count) {
                            break;
                        }
                        if (buffer[i] == (byte) 0) {
                            String rawEvent = new String(buffer, start2, i - start2, StandardCharsets.UTF_8);
                            String r = "([0-9a-zA-Z]{2}:){4}[0-9a-zA-Z]{2}";
                            String rv = "[A-Fa-f0-9]{2,}:{1,}";
                            Pattern pattern2 = Pattern.compile(r);
                            String rawEventLog = rawEvent.replaceAll(encryption_ip, " ******** ");
                            Pattern pattern3 = pattern2;
                            if (pattern3.matcher(rawEventLog).find()) {
                                rawEventLog = rawEventLog.replaceAll(r, " ******** ");
                                stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("RCV <- {");
                                stringBuilder5.append(rawEventLog);
                                stringBuilder5.append("}");
                                log(stringBuilder5.toString());
                            } else if (!shouldPrintEvent()) {
                                rawEventLog = rawEventLog.replaceAll(rv, "****");
                                stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("RCV <- {");
                                stringBuilder5.append(rawEventLog);
                                stringBuilder5.append("}");
                                log(stringBuilder5.toString());
                            }
                            boolean releaseWl = false;
                            WakeLock wakeLock;
                            try {
                                NativeDaemonEvent event = NativeDaemonEvent.parseRawEvent(rawEvent, fdList);
                                socket = socket3;
                                try {
                                    localSocketAddress = address;
                                    try {
                                        String eventLog = event.toString().replaceAll(encryption_ip, " ******** ");
                                        if (shouldPrintEvent()) {
                                            try {
                                                stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("RCV <- {");
                                                stringBuilder3.append(event);
                                                stringBuilder3.append("}");
                                                log(stringBuilder3.toString());
                                            } catch (IllegalArgumentException e3) {
                                                e = e3;
                                                pattern = pattern3;
                                                inputStream = inputStream2;
                                            } catch (Throwable th3) {
                                                th2 = th3;
                                                pattern = pattern3;
                                                inputStream = inputStream2;
                                                if (releaseWl) {
                                                }
                                                throw th2;
                                            }
                                        } else if (pattern3.matcher(eventLog).find()) {
                                            eventLog = eventLog.replaceAll(r, " ******** ");
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("RCV <- {");
                                            stringBuilder3.append(eventLog);
                                            stringBuilder3.append("}");
                                            log(stringBuilder3.toString());
                                        } else {
                                            eventLog = eventLog.replaceAll(rv, "****");
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("RCV <- {");
                                            stringBuilder3.append(eventLog);
                                            stringBuilder3.append("}");
                                            log(stringBuilder3.toString());
                                        }
                                        if (event.isClassUnsolicited()) {
                                            if (this.mCallbacks.onCheckHoldWakeLock(event.getCode())) {
                                                if (this.mWakeLock != null) {
                                                    this.mWakeLock.acquire();
                                                    releaseWl = true;
                                                }
                                            }
                                            try {
                                                inputStream = inputStream2;
                                                try {
                                                    if (this.mCallbackHandler.sendMessage(this.mCallbackHandler.obtainMessage(event.getCode(), uptimeMillisInt(), null, event.getRawEvent()))) {
                                                        releaseWl = null;
                                                    }
                                                } catch (IllegalArgumentException e4) {
                                                    e = e4;
                                                    try {
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("Problem parsing message ");
                                                        stringBuilder.append(e);
                                                        log(stringBuilder.toString());
                                                        if (releaseWl) {
                                                        }
                                                        start2 = i + 1;
                                                        start = i + 1;
                                                        socket3 = socket;
                                                        address = localSocketAddress;
                                                        inputStream2 = inputStream;
                                                    } catch (Throwable th4) {
                                                        th2 = th4;
                                                        if (releaseWl) {
                                                        }
                                                        throw th2;
                                                    }
                                                }
                                            } catch (IllegalArgumentException e5) {
                                                e = e5;
                                                inputStream = inputStream2;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Problem parsing message ");
                                                stringBuilder.append(e);
                                                log(stringBuilder.toString());
                                                if (releaseWl) {
                                                }
                                                start2 = i + 1;
                                                start = i + 1;
                                                socket3 = socket;
                                                address = localSocketAddress;
                                                inputStream2 = inputStream;
                                            } catch (Throwable th5) {
                                                th2 = th5;
                                                inputStream = inputStream2;
                                                if (releaseWl) {
                                                }
                                                throw th2;
                                            }
                                        }
                                        String str = eventLog;
                                        pattern = pattern3;
                                        inputStream = inputStream2;
                                        this.mResponseQueue.add(event.getCmdNumber(), event);
                                        if (releaseWl) {
                                            wakeLock = this.mWakeLock;
                                            wakeLock.release();
                                        }
                                    } catch (IllegalArgumentException e6) {
                                        e = e6;
                                        pattern = pattern3;
                                        inputStream = inputStream2;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Problem parsing message ");
                                        stringBuilder.append(e);
                                        log(stringBuilder.toString());
                                        if (releaseWl) {
                                        }
                                        start2 = i + 1;
                                        start = i + 1;
                                        socket3 = socket;
                                        address = localSocketAddress;
                                        inputStream2 = inputStream;
                                    } catch (Throwable th6) {
                                        th2 = th6;
                                        pattern = pattern3;
                                        inputStream = inputStream2;
                                        if (releaseWl) {
                                        }
                                        throw th2;
                                    }
                                } catch (IllegalArgumentException e7) {
                                    e = e7;
                                    pattern = pattern3;
                                    localSocketAddress = address;
                                    inputStream = inputStream2;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Problem parsing message ");
                                    stringBuilder.append(e);
                                    log(stringBuilder.toString());
                                    if (releaseWl) {
                                    }
                                    start2 = i + 1;
                                    start = i + 1;
                                    socket3 = socket;
                                    address = localSocketAddress;
                                    inputStream2 = inputStream;
                                } catch (Throwable th7) {
                                    th2 = th7;
                                    pattern = pattern3;
                                    localSocketAddress = address;
                                    inputStream = inputStream2;
                                    if (releaseWl) {
                                    }
                                    throw th2;
                                }
                            } catch (IllegalArgumentException e8) {
                                e = e8;
                                socket = socket3;
                                pattern = pattern3;
                                localSocketAddress = address;
                                inputStream = inputStream2;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Problem parsing message ");
                                stringBuilder.append(e);
                                log(stringBuilder.toString());
                                if (releaseWl) {
                                    wakeLock = this.mWakeLock;
                                    wakeLock.release();
                                }
                                start2 = i + 1;
                                start = i + 1;
                                socket3 = socket;
                                address = localSocketAddress;
                                inputStream2 = inputStream;
                            } catch (Throwable th8) {
                                th2 = th8;
                                socket = socket3;
                                pattern = pattern3;
                                localSocketAddress = address;
                                inputStream = inputStream2;
                                if (releaseWl) {
                                    this.mWakeLock.release();
                                }
                                throw th2;
                            }
                            start2 = i + 1;
                        } else {
                            socket = socket3;
                            localSocketAddress = address;
                            inputStream = inputStream2;
                        }
                        start = i + 1;
                        socket3 = socket;
                        address = localSocketAddress;
                        inputStream2 = inputStream;
                    }
                    socket = socket3;
                    localSocketAddress = address;
                    inputStream = inputStream2;
                    if (start2 == 0) {
                        log("RCV incomplete");
                    }
                    if (start2 != count) {
                        obj = 4096;
                        int remaining = 4096 - start2;
                        System.arraycopy(buffer, start2, buffer, 0, remaining);
                        start = remaining;
                    } else {
                        obj = 4096;
                        start = 0;
                    }
                    Object obj2 = obj;
                    socket3 = socket;
                    address = localSocketAddress;
                    inputStream2 = inputStream;
                    outputStream = null;
                }
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("got ");
                stringBuilder5.append(count);
                stringBuilder5.append(" reading with start = ");
                stringBuilder5.append(start);
                loge(stringBuilder5.toString());
                synchronized (this.mDaemonLock) {
                    if (this.mOutputStream != null) {
                        try {
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("closing stream for ");
                            stringBuilder4.append(this.mSocket);
                            loge(stringBuilder4.toString());
                            this.mOutputStream.close();
                        } catch (IOException ex2) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Failed closing output stream: ");
                            stringBuilder2.append(ex2);
                            loge(stringBuilder2.toString());
                        }
                        this.mOutputStream = outputStream;
                    }
                }
                try {
                    socket3.close();
                } catch (IOException ex22) {
                    IOException iOException = ex22;
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append("Failed closing socket: ");
                    stringBuilder6.append(ex22);
                    loge(stringBuilder6.toString());
                }
            } catch (IOException e9) {
                ex22 = e9;
                socket = socket3;
                try {
                    socket2 = new StringBuilder();
                    socket2.append("Communications error: ");
                    socket2.append(ex22);
                    loge(socket2.toString());
                    throw ex22;
                } catch (Throwable th22) {
                    socket2 = socket3;
                    socket3 = th22;
                    synchronized (this.mDaemonLock) {
                        if (this.mOutputStream != null) {
                            try {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("closing stream for ");
                                stringBuilder4.append(this.mSocket);
                                loge(stringBuilder4.toString());
                                this.mOutputStream.close();
                            } catch (IOException ex222) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Failed closing output stream: ");
                                stringBuilder2.append(ex222);
                                loge(stringBuilder2.toString());
                            }
                            this.mOutputStream = null;
                        }
                    }
                    if (socket2 != null) {
                        try {
                            socket2.close();
                        } catch (IOException ex2222) {
                            IOException iOException2 = ex2222;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Failed closing socket: ");
                            stringBuilder3.append(ex2222);
                            loge(stringBuilder3.toString());
                        }
                    }
                    throw socket3;
                }
            } catch (Throwable th222) {
                socket = socket3;
                socket3 = th222;
                socket2 = socket;
                synchronized (this.mDaemonLock) {
                }
                if (socket2 != null) {
                }
                throw socket3;
            }
        } catch (IOException e10) {
            ex2222 = e10;
        }
    }

    @VisibleForTesting
    static void makeCommand(StringBuilder rawBuilder, StringBuilder logBuilder, int sequenceNumber, String cmd, Object... args) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        if (cmd.indexOf(0) >= 0) {
            if (DEBUG_NETD) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected command, cmd = ");
                stringBuilder.append(cmd);
                Slog.e("NDC makeCommand", stringBuilder.toString());
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unexpected command: ");
            stringBuilder2.append(cmd);
            throw new IllegalArgumentException(stringBuilder2.toString());
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
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected argument: ");
                    stringBuilder.append(arg);
                    Slog.e("NDC makeCommand", stringBuilder.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unexpected argument: ");
                    stringBuilder2.append(arg);
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
            }
            rawBuilder.append(0);
        }
    }

    public void waitForCallbacks() {
        if (Thread.currentThread() != this.mLooper.getThread()) {
            final CountDownLatch latch = new CountDownLatch(1);
            this.mCallbackHandler.post(new Runnable() {
                public void run() {
                    latch.countDown();
                }
            });
            try {
                latch.await();
                return;
            } catch (InterruptedException e) {
                Slog.wtf(this.TAG, "Interrupted while waiting for unsolicited response handling", e);
                return;
            }
        }
        throw new IllegalStateException("Must not call this method on callback thread");
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected exactly one response, but received ");
        stringBuilder.append(events.length);
        throw new NativeDaemonConnectorException(stringBuilder.toString());
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

    /* JADX WARNING: Missing block: B:19:0x00d4, code skipped:
            r16 = r5;
            r17 = r6;
            r14 = r1.mResponseQueue.remove(r7, r26, r11);
     */
    /* JADX WARNING: Missing block: B:20:0x00e0, code skipped:
            if (r14 != null) goto L_0x0128;
     */
    /* JADX WARNING: Missing block: B:21:0x00e2, code skipped:
            r15 = new java.lang.StringBuilder();
            r15.append("timed-out waiting for response to ");
            r15.append(r11);
            loge(r15.toString());
     */
    /* JADX WARNING: Missing block: B:22:0x00f9, code skipped:
            if (r1.mhwNativeDaemonConnector == null) goto L_0x0101;
     */
    /* JADX WARNING: Missing block: B:23:0x00fb, code skipped:
            r1.mhwNativeDaemonConnector.reportChrForAddRouteFail(r11, null);
     */
    /* JADX WARNING: Missing block: B:24:0x0101, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append("timed-out waiting for response mOutputStream = ");
            r0.append(r1.mOutputStream);
            r0.append(", mSocket = ");
            r0.append(r1.mSocket);
            loge(r0.toString());
     */
    /* JADX WARNING: Missing block: B:25:0x0127, code skipped:
            throw new com.android.server.NativeDaemonTimeoutException(r11, r14);
     */
    /* JADX WARNING: Missing block: B:26:0x0128, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append("RMV <- {");
            r0.append(r14);
            r0.append("}");
            r0 = r0.toString();
     */
    /* JADX WARNING: Missing block: B:27:0x0142, code skipped:
            if (r1.mhwNativeDaemonConnector == null) goto L_0x0149;
     */
    /* JADX WARNING: Missing block: B:28:0x0144, code skipped:
            r1.mhwNativeDaemonConnector.reportChrForAddRouteFail(r11, r0);
     */
    /* JADX WARNING: Missing block: B:29:0x0149, code skipped:
            r4.add(r14);
     */
    /* JADX WARNING: Missing block: B:30:0x0150, code skipped:
            if (r14.isClassContinue() != false) goto L_0x01b2;
     */
    /* JADX WARNING: Missing block: B:31:0x0152, code skipped:
            r18 = android.os.SystemClock.elapsedRealtime();
     */
    /* JADX WARNING: Missing block: B:32:0x015c, code skipped:
            if ((r18 - r2) <= 500) goto L_0x0183;
     */
    /* JADX WARNING: Missing block: B:33:0x015e, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append("NDC Command {");
            r0.append(r11);
            r0.append("} took too long (");
            r0.append(r18 - r2);
            r0.append("ms)");
            loge(r0.toString());
     */
    /* JADX WARNING: Missing block: B:35:0x0187, code skipped:
            if (r14.isClassClientError() != false) goto L_0x01a7;
     */
    /* JADX WARNING: Missing block: B:37:0x018d, code skipped:
            if (r14.isClassServerError() != false) goto L_0x019c;
     */
    /* JADX WARNING: Missing block: B:39:0x019b, code skipped:
            return (com.android.server.NativeDaemonEvent[]) r4.toArray(new com.android.server.NativeDaemonEvent[r4.size()]);
     */
    /* JADX WARNING: Missing block: B:40:0x019c, code skipped:
            loge("NDC server error throw NativeDaemonFailureException");
     */
    /* JADX WARNING: Missing block: B:41:0x01a6, code skipped:
            throw new com.android.server.NativeDaemonConnector.NativeDaemonFailureException(r11, r14);
     */
    /* JADX WARNING: Missing block: B:42:0x01a7, code skipped:
            loge("NDC client error throw NativeDaemonArgumentException");
     */
    /* JADX WARNING: Missing block: B:43:0x01b1, code skipped:
            throw new com.android.server.NativeDaemonConnector.NativeDaemonArgumentException(r11, r14);
     */
    /* JADX WARNING: Missing block: B:44:0x01b2, code skipped:
            r5 = r16;
            r6 = r17;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public NativeDaemonEvent[] executeForList(long timeoutMs, String cmd, Object... args) throws NativeDaemonConnectorException {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        Throwable e;
        if (this.mWarnIfHeld != null && Thread.holdsLock(this.mWarnIfHeld)) {
            String str = this.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Calling thread ");
            stringBuilder3.append(Thread.currentThread().getName());
            stringBuilder3.append(" is holding 0x");
            stringBuilder3.append(Integer.toHexString(System.identityHashCode(this.mWarnIfHeld)));
            Slog.wtf(str, stringBuilder3.toString(), new Throwable());
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
        String r_ipv6 = "([A-Fa-f0-9]{2,}:{1,}){2,}";
        String encryption_logCmd = logCmd.replaceAll(encryption_ip, " ******** ").replaceAll("([A-Fa-f0-9]{2,}:{1,}){2,}", " **** ");
        StringBuilder stringBuilder4;
        if (shouldPrintEvent()) {
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("SND -> {");
            stringBuilder4.append(logCmd);
            stringBuilder4.append("}");
            log(stringBuilder4.toString());
        } else {
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("SND -> {");
            stringBuilder4.append(encryption_logCmd);
            stringBuilder4.append("}");
            log(stringBuilder4.toString());
        }
        synchronized (this.mDaemonLock) {
            try {
                if (this.mOutputStream != null) {
                    try {
                        this.mOutputStream.write(rawCmd.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e2) {
                        stringBuilder = rawBuilder;
                        stringBuilder2 = logBuilder;
                        loge("NDC problem sending command throw NativeDaemonConnectorException");
                        throw new NativeDaemonConnectorException("problem sending command", e2);
                    } catch (Throwable th) {
                        e2 = th;
                        throw e2;
                    }
                }
                stringBuilder2 = logBuilder;
                loge("NDC missing output stream throw NativeDaemonConnectorException");
                throw new NativeDaemonConnectorException("missing output stream");
            } catch (Throwable th2) {
                e2 = th2;
                stringBuilder = rawBuilder;
                stringBuilder2 = logBuilder;
                throw e2;
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
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected list response ");
                stringBuilder.append(code);
                stringBuilder.append(" instead of ");
                stringBuilder.append(expectedCode);
                throw new NativeDaemonConnectorException(stringBuilder.toString());
            }
        }
        NativeDaemonEvent finalEvent = events[events.length - 1];
        if (finalEvent.isClassOk()) {
            return (String[]) list.toArray(new String[list.size()]);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("unexpected final event: ");
        stringBuilder2.append(finalEvent);
        throw new NativeDaemonConnectorException(stringBuilder2.toString());
    }

    @VisibleForTesting
    static void appendEscaped(StringBuilder builder, String arg) {
        int i = 0;
        boolean hasSpaces = arg.indexOf(32) >= 0;
        if (hasSpaces) {
            builder.append('\"');
        }
        int length = arg.length();
        while (i < length) {
            char c = arg.charAt(i);
            if (c == '\"') {
                builder.append("\\\"");
            } else if (c == '\\') {
                builder.append("\\\\");
            } else {
                builder.append(c);
            }
            i++;
        }
        if (hasSpaces) {
            builder.append('\"');
        }
    }

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

    /* JADX WARNING: Removed duplicated region for block: B:17:0x003a A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x003b A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x003a A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x003b A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x003a A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x003b A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean shouldPrintEvent() {
        boolean z;
        String str = this.TAG;
        int hashCode = str.hashCode();
        if (hashCode == -1339953786) {
            if (str.equals("SdCryptdConnector")) {
                z = false;
                switch (z) {
                    case false:
                    case true:
                    case true:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 136483132) {
            if (str.equals("VoldConnector")) {
                z = true;
                switch (z) {
                    case false:
                    case true:
                    case true:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 1813268503 && str.equals("CryptdConnector")) {
            z = true;
            switch (z) {
                case false:
                case true:
                case true:
                    return true;
                default:
                    return false;
            }
        }
        z = true;
        switch (z) {
            case false:
            case true:
            case true:
                break;
            default:
                break;
        }
    }
}
