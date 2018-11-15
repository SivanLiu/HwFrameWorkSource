package com.android.server;

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

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.fixSplitterBlock(BlockFinish.java:45)
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:29)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private void listenToSocket() throws java.io.IOException {
        /*
        r22 = this;
        r1 = r22;
        r2 = 0;
        r0 = "listenToSocket enter";
        r1.log(r0);
        r3 = 0;
        r0 = new android.net.LocalSocket;	 Catch:{ IOException -> 0x02e4 }
        r0.<init>();	 Catch:{ IOException -> 0x02e4 }
        r2 = r0;
        r0 = r22.determineSocketAddress();	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r4 = r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r2.connect(r4);	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = r2.getInputStream();	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r5 = r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r6 = r1.mDaemonLock;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        monitor-enter(r6);	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = r2.getOutputStream();	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r1.mOutputStream = r0;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        monitor-exit(r6);	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r0 = r1.mCallbacks;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0.onDaemonConnected();	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = 0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r6 = 4096; // 0x1000 float:5.74E-42 double:2.0237E-320;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r7 = new byte[r6];	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r9 = r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = 0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
    L_0x0033:
        r10 = 4096 - r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r10 = r5.read(r7, r0, r10);	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        if (r10 >= 0) goto L_0x00b4;
    L_0x003b:
        r6 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x02e4 }
        r6.<init>();	 Catch:{ IOException -> 0x02e4 }
        r8 = "got ";	 Catch:{ IOException -> 0x02e4 }
        r6.append(r8);	 Catch:{ IOException -> 0x02e4 }
        r6.append(r10);	 Catch:{ IOException -> 0x02e4 }
        r8 = " reading with start = ";	 Catch:{ IOException -> 0x02e4 }
        r6.append(r8);	 Catch:{ IOException -> 0x02e4 }
        r6.append(r0);	 Catch:{ IOException -> 0x02e4 }
        r6 = r6.toString();	 Catch:{ IOException -> 0x02e4 }
        r1.loge(r6);	 Catch:{ IOException -> 0x02e4 }
        r4 = r1.mDaemonLock;
        monitor-enter(r4);
        r0 = r1.mOutputStream;	 Catch:{ IOException -> 0x007b }
        if (r0 == 0) goto L_0x0092;
    L_0x005f:
        r0 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x007b }
        r0.<init>();	 Catch:{ IOException -> 0x007b }
        r5 = "closing stream for ";	 Catch:{ IOException -> 0x007b }
        r0.append(r5);	 Catch:{ IOException -> 0x007b }
        r5 = r1.mSocket;	 Catch:{ IOException -> 0x007b }
        r0.append(r5);	 Catch:{ IOException -> 0x007b }
        r0 = r0.toString();	 Catch:{ IOException -> 0x007b }
        r1.loge(r0);	 Catch:{ IOException -> 0x007b }
        r0 = r1.mOutputStream;	 Catch:{ IOException -> 0x007b }
        r0.close();	 Catch:{ IOException -> 0x007b }
        goto L_0x0090;
    L_0x007b:
        r0 = move-exception;
        r5 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x007b }
        r5.<init>();	 Catch:{ IOException -> 0x007b }
        r6 = "Failed closing output stream: ";	 Catch:{ IOException -> 0x007b }
        r5.append(r6);	 Catch:{ IOException -> 0x007b }
        r5.append(r0);	 Catch:{ IOException -> 0x007b }
        r5 = r5.toString();	 Catch:{ IOException -> 0x007b }
        r1.loge(r5);	 Catch:{ IOException -> 0x007b }
    L_0x0090:
        r1.mOutputStream = r3;	 Catch:{ IOException -> 0x007b }
    L_0x0092:
        monitor-exit(r4);	 Catch:{ IOException -> 0x007b }
        r2.close();	 Catch:{ IOException -> 0x0098 }
        goto L_0x00af;
    L_0x0098:
        r0 = move-exception;
        r3 = r0;
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "Failed closing socket: ";
        r3.append(r4);
        r3.append(r0);
        r3 = r3.toString();
        r1.loge(r3);
        goto L_0x00b0;
    L_0x00b0:
        return;
    L_0x00b1:
        r0 = move-exception;
        monitor-exit(r4);	 Catch:{ IOException -> 0x007b }
        throw r0;
    L_0x00b4:
        r11 = r2.getAncillaryFileDescriptors();	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r9 = r11;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r10 = r10 + r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = 0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r11 = r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = 0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
    L_0x00bd:
        r12 = r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        if (r12 >= r10) goto L_0x0299;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
    L_0x00c0:
        r0 = r7[r12];	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        if (r0 != 0) goto L_0x0286;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
    L_0x00c4:
        r0 = new java.lang.String;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r13 = r12 - r11;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r14 = java.nio.charset.StandardCharsets.UTF_8;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0.<init>(r7, r11, r13, r14);	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r13 = r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = "([0-9a-zA-Z]{2}:){4}[0-9a-zA-Z]{2}";	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r14 = r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = "[A-Fa-f0-9]{2,}:{1,}";	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r15 = r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = java.util.regex.Pattern.compile(r14);	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r16 = r0;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r3 = " ******** ";	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r0 = r13.replaceAll(r0, r3);	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r3 = r16;	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r6 = r3.matcher(r0);	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        r6 = r6.find();	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        if (r6 == 0) goto L_0x0110;
    L_0x00ee:
        r6 = " ******** ";	 Catch:{ IOException -> 0x02e4 }
        r6 = r0.replaceAll(r14, r6);	 Catch:{ IOException -> 0x02e4 }
        r0 = r6;	 Catch:{ IOException -> 0x02e4 }
        r6 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x02e4 }
        r6.<init>();	 Catch:{ IOException -> 0x02e4 }
        r8 = "RCV <- {";	 Catch:{ IOException -> 0x02e4 }
        r6.append(r8);	 Catch:{ IOException -> 0x02e4 }
        r6.append(r0);	 Catch:{ IOException -> 0x02e4 }
        r8 = "}";	 Catch:{ IOException -> 0x02e4 }
        r6.append(r8);	 Catch:{ IOException -> 0x02e4 }
        r6 = r6.toString();	 Catch:{ IOException -> 0x02e4 }
        r1.log(r6);	 Catch:{ IOException -> 0x02e4 }
        goto L_0x0137;
    L_0x0110:
        r6 = r22.shouldPrintEvent();	 Catch:{ IOException -> 0x02dc, all -> 0x02d5 }
        if (r6 != 0) goto L_0x0137;
    L_0x0116:
        r6 = "****";	 Catch:{ IOException -> 0x02e4 }
        r6 = r0.replaceAll(r15, r6);	 Catch:{ IOException -> 0x02e4 }
        r0 = r6;	 Catch:{ IOException -> 0x02e4 }
        r6 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x02e4 }
        r6.<init>();	 Catch:{ IOException -> 0x02e4 }
        r8 = "RCV <- {";	 Catch:{ IOException -> 0x02e4 }
        r6.append(r8);	 Catch:{ IOException -> 0x02e4 }
        r6.append(r0);	 Catch:{ IOException -> 0x02e4 }
        r8 = "}";	 Catch:{ IOException -> 0x02e4 }
        r6.append(r8);	 Catch:{ IOException -> 0x02e4 }
        r6 = r6.toString();	 Catch:{ IOException -> 0x02e4 }
        r1.log(r6);	 Catch:{ IOException -> 0x02e4 }
    L_0x0137:
        r6 = r0;
        r8 = 0;
        r16 = r8;
        r0 = com.android.server.NativeDaemonEvent.parseRawEvent(r13, r9);	 Catch:{ IllegalArgumentException -> 0x0257, all -> 0x024d }
        r8 = r0.toString();	 Catch:{ IllegalArgumentException -> 0x0257, all -> 0x024d }
        r17 = r2;
        r2 = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";	 Catch:{ IllegalArgumentException -> 0x0245, all -> 0x023d }
        r18 = r4;
        r4 = " ******** ";	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r2 = r8.replaceAll(r2, r4);	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r4 = r22.shouldPrintEvent();	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        if (r4 == 0) goto L_0x017f;
    L_0x0156:
        r4 = new java.lang.StringBuilder;	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4.<init>();	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r8 = "RCV <- {";	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4.append(r8);	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4.append(r0);	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r8 = "}";	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4.append(r8);	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4 = r4.toString();	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r1.log(r4);	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        goto L_0x01cc;
    L_0x0171:
        r0 = move-exception;
        r20 = r3;
        r21 = r5;
        goto L_0x027e;
    L_0x0178:
        r0 = move-exception;
        r20 = r3;
        r21 = r5;
        goto L_0x0260;
    L_0x017f:
        r4 = r3.matcher(r2);	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r4 = r4.find();	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        if (r4 == 0) goto L_0x01ab;
    L_0x0189:
        r4 = " ******** ";	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4 = r2.replaceAll(r14, r4);	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r2 = r4;	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4 = new java.lang.StringBuilder;	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4.<init>();	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r8 = "RCV <- {";	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4.append(r8);	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4.append(r2);	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r8 = "}";	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4.append(r8);	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4 = r4.toString();	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r1.log(r4);	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        goto L_0x01cc;
    L_0x01ab:
        r4 = "****";	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r4 = r2.replaceAll(r15, r4);	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r2 = r4;	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r4 = new java.lang.StringBuilder;	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r4.<init>();	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r8 = "RCV <- {";	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r4.append(r8);	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r4.append(r2);	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r8 = "}";	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r4.append(r8);	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r4 = r4.toString();	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r1.log(r4);	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
    L_0x01cc:
        r4 = r0.isClassUnsolicited();	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        if (r4 == 0) goto L_0x0218;	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
    L_0x01d2:
        r4 = r1.mCallbacks;	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r8 = r0.getCode();	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r4 = r4.onCheckHoldWakeLock(r8);	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        if (r4 == 0) goto L_0x01ea;
    L_0x01de:
        r4 = r1.mWakeLock;	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        if (r4 == 0) goto L_0x01ea;	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
    L_0x01e2:
        r4 = r1.mWakeLock;	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4.acquire();	 Catch:{ IllegalArgumentException -> 0x0178, all -> 0x0171 }
        r4 = 1;
        r16 = r4;
    L_0x01ea:
        r4 = r1.mCallbackHandler;	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r8 = r0.getCode();	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r19 = r2;	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r2 = r22.uptimeMillisInt();	 Catch:{ IllegalArgumentException -> 0x0237, all -> 0x0231 }
        r20 = r3;
        r3 = r0.getRawEvent();	 Catch:{ IllegalArgumentException -> 0x0214, all -> 0x020f }
        r21 = r5;
        r5 = 0;
        r2 = r4.obtainMessage(r8, r2, r5, r3);	 Catch:{ IllegalArgumentException -> 0x022f }
        r3 = r1.mCallbackHandler;	 Catch:{ IllegalArgumentException -> 0x022f }
        r3 = r3.sendMessage(r2);	 Catch:{ IllegalArgumentException -> 0x022f }
        if (r3 == 0) goto L_0x0227;	 Catch:{ IllegalArgumentException -> 0x022f }
    L_0x020b:
        r2 = 0;	 Catch:{ IllegalArgumentException -> 0x022f }
        r16 = r2;	 Catch:{ IllegalArgumentException -> 0x022f }
    L_0x020e:
        goto L_0x0227;	 Catch:{ IllegalArgumentException -> 0x022f }
    L_0x020f:
        r0 = move-exception;	 Catch:{ IllegalArgumentException -> 0x022f }
        r21 = r5;	 Catch:{ IllegalArgumentException -> 0x022f }
        goto L_0x027e;	 Catch:{ IllegalArgumentException -> 0x022f }
    L_0x0214:
        r0 = move-exception;	 Catch:{ IllegalArgumentException -> 0x022f }
        r21 = r5;	 Catch:{ IllegalArgumentException -> 0x022f }
        goto L_0x0260;	 Catch:{ IllegalArgumentException -> 0x022f }
    L_0x0218:
        r19 = r2;	 Catch:{ IllegalArgumentException -> 0x022f }
        r20 = r3;	 Catch:{ IllegalArgumentException -> 0x022f }
        r21 = r5;	 Catch:{ IllegalArgumentException -> 0x022f }
        r2 = r1.mResponseQueue;	 Catch:{ IllegalArgumentException -> 0x022f }
        r3 = r0.getCmdNumber();	 Catch:{ IllegalArgumentException -> 0x022f }
        r2.add(r3, r0);	 Catch:{ IllegalArgumentException -> 0x022f }
    L_0x0227:
        if (r16 == 0) goto L_0x0279;
    L_0x0229:
        r0 = r1.mWakeLock;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x022b:
        r0.release();	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        goto L_0x0279;
    L_0x022f:
        r0 = move-exception;
        goto L_0x0260;
    L_0x0231:
        r0 = move-exception;
        r20 = r3;
        r21 = r5;
        goto L_0x027e;
    L_0x0237:
        r0 = move-exception;
        r20 = r3;
        r21 = r5;
        goto L_0x0260;
    L_0x023d:
        r0 = move-exception;
        r20 = r3;
        r18 = r4;
        r21 = r5;
        goto L_0x027e;
    L_0x0245:
        r0 = move-exception;
        r20 = r3;
        r18 = r4;
        r21 = r5;
        goto L_0x0260;
    L_0x024d:
        r0 = move-exception;
        r17 = r2;
        r20 = r3;
        r18 = r4;
        r21 = r5;
        goto L_0x027e;
    L_0x0257:
        r0 = move-exception;
        r17 = r2;
        r20 = r3;
        r18 = r4;
        r21 = r5;
    L_0x0260:
        r2 = new java.lang.StringBuilder;	 Catch:{ all -> 0x027d }
        r2.<init>();	 Catch:{ all -> 0x027d }
        r3 = "Problem parsing message ";	 Catch:{ all -> 0x027d }
        r2.append(r3);	 Catch:{ all -> 0x027d }
        r2.append(r0);	 Catch:{ all -> 0x027d }
        r2 = r2.toString();	 Catch:{ all -> 0x027d }
        r1.log(r2);	 Catch:{ all -> 0x027d }
        if (r16 == 0) goto L_0x0279;
    L_0x0276:
        r0 = r1.mWakeLock;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        goto L_0x022b;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x0279:
        r0 = r12 + 1;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r11 = r0;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        goto L_0x028c;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x027d:
        r0 = move-exception;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x027e:
        if (r16 == 0) goto L_0x0285;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x0280:
        r2 = r1.mWakeLock;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r2.release();	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x0285:
        throw r0;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x0286:
        r17 = r2;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r18 = r4;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r21 = r5;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x028c:
        r0 = r12 + 1;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r2 = r17;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r4 = r18;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r5 = r21;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r3 = 0;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r6 = 4096; // 0x1000 float:5.74E-42 double:2.0237E-320;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        goto L_0x00bd;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x0299:
        r17 = r2;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r18 = r4;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r21 = r5;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        if (r11 != 0) goto L_0x02a6;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x02a1:
        r0 = "RCV incomplete";	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r1.log(r0);	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x02a6:
        if (r11 == r10) goto L_0x02b2;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x02a8:
        r2 = 4096; // 0x1000 float:5.74E-42 double:2.0237E-320;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r6 = 4096 - r11;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r3 = 0;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        java.lang.System.arraycopy(r7, r11, r7, r3, r6);	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
        r0 = r6;
        goto L_0x02b6;
    L_0x02b2:
        r2 = 4096; // 0x1000 float:5.74E-42 double:2.0237E-320;
        r3 = 0;
        r0 = 0;
        r6 = r2;
        r2 = r17;
        r4 = r18;
        r5 = r21;
        r3 = 0;
        goto L_0x0033;
    L_0x02c1:
        r0 = move-exception;
        r17 = r2;
        r18 = r4;
        r21 = r5;
    L_0x02c8:
        monitor-exit(r6);	 Catch:{ all -> 0x02d3 }
        throw r0;	 Catch:{ all -> 0x02c1, IOException -> 0x02cf, all -> 0x02ca }
    L_0x02ca:
        r0 = move-exception;
        r2 = r0;
        r3 = r17;
        goto L_0x02fa;
    L_0x02cf:
        r0 = move-exception;
        r2 = r17;
        goto L_0x02e5;
    L_0x02d3:
        r0 = move-exception;
        goto L_0x02c8;
    L_0x02d5:
        r0 = move-exception;
        r17 = r2;
        r2 = r0;
        r3 = r17;
        goto L_0x02fa;
    L_0x02dc:
        r0 = move-exception;
        r17 = r2;
        goto L_0x02e5;
    L_0x02e0:
        r0 = move-exception;
        r3 = r2;
        r2 = r0;
        goto L_0x02fa;
    L_0x02e4:
        r0 = move-exception;
    L_0x02e5:
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x02e0 }
        r3.<init>();	 Catch:{ all -> 0x02e0 }
        r4 = "Communications error: ";	 Catch:{ all -> 0x02e0 }
        r3.append(r4);	 Catch:{ all -> 0x02e0 }
        r3.append(r0);	 Catch:{ all -> 0x02e0 }
        r3 = r3.toString();	 Catch:{ all -> 0x02e0 }
        r1.loge(r3);	 Catch:{ all -> 0x02e0 }
        throw r0;	 Catch:{ all -> 0x02e0 }
    L_0x02fa:
        r4 = r1.mDaemonLock;
        monitor-enter(r4);
        r0 = r1.mOutputStream;	 Catch:{ IOException -> 0x031d }
        if (r0 == 0) goto L_0x0335;
    L_0x0301:
        r0 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x031d }
        r0.<init>();	 Catch:{ IOException -> 0x031d }
        r5 = "closing stream for ";	 Catch:{ IOException -> 0x031d }
        r0.append(r5);	 Catch:{ IOException -> 0x031d }
        r5 = r1.mSocket;	 Catch:{ IOException -> 0x031d }
        r0.append(r5);	 Catch:{ IOException -> 0x031d }
        r0 = r0.toString();	 Catch:{ IOException -> 0x031d }
        r1.loge(r0);	 Catch:{ IOException -> 0x031d }
        r0 = r1.mOutputStream;	 Catch:{ IOException -> 0x031d }
        r0.close();	 Catch:{ IOException -> 0x031d }
        goto L_0x0332;
    L_0x031d:
        r0 = move-exception;
        r5 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x031d }
        r5.<init>();	 Catch:{ IOException -> 0x031d }
        r6 = "Failed closing output stream: ";	 Catch:{ IOException -> 0x031d }
        r5.append(r6);	 Catch:{ IOException -> 0x031d }
        r5.append(r0);	 Catch:{ IOException -> 0x031d }
        r5 = r5.toString();	 Catch:{ IOException -> 0x031d }
        r1.loge(r5);	 Catch:{ IOException -> 0x031d }
    L_0x0332:
        r5 = 0;	 Catch:{ IOException -> 0x031d }
        r1.mOutputStream = r5;	 Catch:{ IOException -> 0x031d }
    L_0x0335:
        monitor-exit(r4);	 Catch:{ IOException -> 0x031d }
        if (r3 == 0) goto L_0x0353;
    L_0x0338:
        r3.close();	 Catch:{ IOException -> 0x033c }
        goto L_0x0353;
    L_0x033c:
        r0 = move-exception;
        r4 = r0;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "Failed closing socket: ";
        r4.append(r5);
        r4.append(r0);
        r4 = r4.toString();
        r1.loge(r4);
    L_0x0353:
        throw r2;
    L_0x0354:
        r0 = move-exception;
        monitor-exit(r4);	 Catch:{ IOException -> 0x031d }
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.NativeDaemonConnector.listenToSocket():void");
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

    /* JADX WARNING: Missing block: B:19:0x00d4, code:
            r16 = r5;
            r17 = r6;
            r14 = r1.mResponseQueue.remove(r7, r26, r11);
     */
    /* JADX WARNING: Missing block: B:20:0x00e0, code:
            if (r14 != null) goto L_0x0128;
     */
    /* JADX WARNING: Missing block: B:21:0x00e2, code:
            r15 = new java.lang.StringBuilder();
            r15.append("timed-out waiting for response to ");
            r15.append(r11);
            loge(r15.toString());
     */
    /* JADX WARNING: Missing block: B:22:0x00f9, code:
            if (r1.mhwNativeDaemonConnector == null) goto L_0x0101;
     */
    /* JADX WARNING: Missing block: B:23:0x00fb, code:
            r1.mhwNativeDaemonConnector.reportChrForAddRouteFail(r11, null);
     */
    /* JADX WARNING: Missing block: B:24:0x0101, code:
            r0 = new java.lang.StringBuilder();
            r0.append("timed-out waiting for response mOutputStream = ");
            r0.append(r1.mOutputStream);
            r0.append(", mSocket = ");
            r0.append(r1.mSocket);
            loge(r0.toString());
     */
    /* JADX WARNING: Missing block: B:25:0x0127, code:
            throw new com.android.server.NativeDaemonTimeoutException(r11, r14);
     */
    /* JADX WARNING: Missing block: B:26:0x0128, code:
            r0 = new java.lang.StringBuilder();
            r0.append("RMV <- {");
            r0.append(r14);
            r0.append("}");
            r0 = r0.toString();
     */
    /* JADX WARNING: Missing block: B:27:0x0142, code:
            if (r1.mhwNativeDaemonConnector == null) goto L_0x0149;
     */
    /* JADX WARNING: Missing block: B:28:0x0144, code:
            r1.mhwNativeDaemonConnector.reportChrForAddRouteFail(r11, r0);
     */
    /* JADX WARNING: Missing block: B:29:0x0149, code:
            r4.add(r14);
     */
    /* JADX WARNING: Missing block: B:30:0x0150, code:
            if (r14.isClassContinue() != false) goto L_0x01b2;
     */
    /* JADX WARNING: Missing block: B:31:0x0152, code:
            r18 = android.os.SystemClock.elapsedRealtime();
     */
    /* JADX WARNING: Missing block: B:32:0x015c, code:
            if ((r18 - r2) <= 500) goto L_0x0183;
     */
    /* JADX WARNING: Missing block: B:33:0x015e, code:
            r0 = new java.lang.StringBuilder();
            r0.append("NDC Command {");
            r0.append(r11);
            r0.append("} took too long (");
            r0.append(r18 - r2);
            r0.append("ms)");
            loge(r0.toString());
     */
    /* JADX WARNING: Missing block: B:35:0x0187, code:
            if (r14.isClassClientError() != false) goto L_0x01a7;
     */
    /* JADX WARNING: Missing block: B:37:0x018d, code:
            if (r14.isClassServerError() != false) goto L_0x019c;
     */
    /* JADX WARNING: Missing block: B:39:0x019b, code:
            return (com.android.server.NativeDaemonEvent[]) r4.toArray(new com.android.server.NativeDaemonEvent[r4.size()]);
     */
    /* JADX WARNING: Missing block: B:40:0x019c, code:
            loge("NDC server error throw NativeDaemonFailureException");
     */
    /* JADX WARNING: Missing block: B:41:0x01a6, code:
            throw new com.android.server.NativeDaemonConnector.NativeDaemonFailureException(r11, r14);
     */
    /* JADX WARNING: Missing block: B:42:0x01a7, code:
            loge("NDC client error throw NativeDaemonArgumentException");
     */
    /* JADX WARNING: Missing block: B:43:0x01b1, code:
            throw new com.android.server.NativeDaemonConnector.NativeDaemonArgumentException(r11, r14);
     */
    /* JADX WARNING: Missing block: B:44:0x01b2, code:
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
                    } catch (Throwable e2) {
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
