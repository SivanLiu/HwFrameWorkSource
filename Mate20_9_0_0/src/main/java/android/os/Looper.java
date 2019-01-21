package android.os;

import android.common.HwFrameworkFactory;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import android.util.Printer;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.zrhung.IAppEyeUiProbe;

public final class Looper {
    private static final String TAG = "Looper";
    private static IAppEyeUiProbe mZrHungAppEyeUiProbe;
    private static Looper sMainLooper;
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal();
    private Printer mLogging;
    final MessageQueue mQueue;
    private long mSlowDeliveryThresholdMs;
    private long mSlowDispatchThresholdMs;
    final Thread mThread = Thread.currentThread();
    private long mTraceTag;

    public static void prepare() {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() == null) {
            sThreadLocal.set(new Looper(quitAllowed));
            return;
        }
        throw new RuntimeException("Only one Looper may be created per thread");
    }

    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper == null) {
                sMainLooper = myLooper();
            } else {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
        }
    }

    public static Looper getMainLooper() {
        Looper looper;
        synchronized (Looper.class) {
            looper = sMainLooper;
        }
        return looper;
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x00a5  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x00a3  */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x00cc  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00c7  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00d5 A:{SYNTHETIC, Splitter:B:41:0x00d5} */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x00fa A:{SYNTHETIC, Splitter:B:50:0x00fa} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x010c  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x0105 A:{Catch:{ all -> 0x00e3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x0112  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x0153  */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0117  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x015f  */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x018f  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x016c  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x01e5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0199  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0200  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void loop() {
        Throwable th;
        Looper me = myLooper();
        if (me != null) {
            MessageQueue queue = me.mQueue;
            Binder.clearCallingIdentity();
            long ident = Binder.clearCallingIdentity();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("log.looper.");
            stringBuilder.append(Process.myUid());
            stringBuilder.append(".");
            stringBuilder.append(Thread.currentThread().getName());
            stringBuilder.append(".slow");
            int thresholdOverride = SystemProperties.getInt(stringBuilder.toString(), 0);
            Looper me2;
            MessageQueue queue2;
            int i;
            if (BlockMonitor.isNeedMonitor()) {
                me2 = me;
                queue2 = queue;
                i = thresholdOverride;
                while (true) {
                    MessageQueue me3 = queue2;
                    Message msg = me3.next();
                    if (msg != null) {
                        Looper me4 = me2;
                        Printer logging = me4.mLogging;
                        if (logging != null) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(">>>>> Dispatching to ");
                            stringBuilder2.append(msg.target);
                            stringBuilder2.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                            stringBuilder2.append(msg.callback);
                            stringBuilder2.append(": ");
                            stringBuilder2.append(msg.what);
                            logging.println(stringBuilder2.toString());
                        }
                        long dispatchTime = SystemClock.uptimeMillis();
                        if (mZrHungAppEyeUiProbe != null) {
                            mZrHungAppEyeUiProbe.beginDispatching(msg, msg.target, msg.callback);
                        }
                        msg.target.dispatchMessage(msg);
                        if (mZrHungAppEyeUiProbe != null) {
                            mZrHungAppEyeUiProbe.endDispatching();
                        }
                        BlockMonitor.checkMessageDelayTime(dispatchTime, msg, me3);
                        if (logging != null) {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("<<<<< Finished to ");
                            stringBuilder3.append(msg.target);
                            stringBuilder3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                            stringBuilder3.append(msg.callback);
                            logging.println(stringBuilder3.toString());
                        }
                        long newIdent = Binder.clearCallingIdentity();
                        if (ident != newIdent) {
                            String str = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Thread identity changed from 0x");
                            stringBuilder4.append(Long.toHexString(ident));
                            stringBuilder4.append(" to 0x");
                            stringBuilder4.append(Long.toHexString(newIdent));
                            stringBuilder4.append(" while dispatching to ");
                            stringBuilder4.append(msg.target.getClass().getName());
                            stringBuilder4.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                            stringBuilder4.append(msg.callback);
                            stringBuilder4.append(" what=");
                            stringBuilder4.append(msg.what);
                            Log.wtf(str, stringBuilder4.toString());
                        }
                        msg.recycleUnchecked();
                        queue2 = me3;
                        me2 = me4;
                    } else {
                        return;
                    }
                }
            }
            boolean slowDeliveryDetected = false;
            while (true) {
                Message msg2 = queue.next();
                if (msg2 != null) {
                    boolean z;
                    boolean logSlowDelivery;
                    boolean logSlowDispatch;
                    boolean needStartTime;
                    boolean needEndTime;
                    long dispatchStart;
                    Message message;
                    long dispatchEnd;
                    boolean z2;
                    Printer logging2;
                    Printer logging3 = me.mLogging;
                    if (logging3 != null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(">>>>> Dispatching to ");
                        stringBuilder.append(msg2.target);
                        stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        stringBuilder.append(msg2.callback);
                        stringBuilder.append(": ");
                        stringBuilder.append(msg2.what);
                        logging3.println(stringBuilder.toString());
                    }
                    long traceTag = me.mTraceTag;
                    long slowDispatchThresholdMs = me.mSlowDispatchThresholdMs;
                    long slowDeliveryThresholdMs = me.mSlowDeliveryThresholdMs;
                    if (thresholdOverride > 0) {
                        slowDispatchThresholdMs = (long) thresholdOverride;
                        slowDeliveryThresholdMs = (long) thresholdOverride;
                    }
                    long slowDispatchThresholdMs2 = slowDispatchThresholdMs;
                    long slowDeliveryThresholdMs2 = slowDeliveryThresholdMs;
                    boolean z3 = true;
                    if (slowDeliveryThresholdMs2 > 0) {
                        i = thresholdOverride;
                        if (msg2.when > 0) {
                            Message msg3;
                            z = true;
                            logSlowDelivery = z;
                            logSlowDispatch = slowDispatchThresholdMs2 <= 0;
                            if (!(logSlowDelivery || logSlowDispatch)) {
                                z3 = false;
                            }
                            needStartTime = z3;
                            needEndTime = logSlowDispatch;
                            if (traceTag != 0 && Trace.isTagEnabled(traceTag)) {
                                Trace.traceBegin(traceTag, msg2.target.getTraceName(msg2));
                            }
                            dispatchStart = needStartTime ? SystemClock.uptimeMillis() : 0;
                            if (BlockMonitor.isInMainThread()) {
                                try {
                                    if (mZrHungAppEyeUiProbe != null) {
                                        mZrHungAppEyeUiProbe.beginDispatching(msg2, msg2.target, msg2.callback);
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    me2 = me;
                                    me = traceTag;
                                    message = msg2;
                                    if (me != null) {
                                    }
                                    throw th;
                                }
                            }
                            msg2.target.dispatchMessage(msg2);
                            if (BlockMonitor.isInMainThread()) {
                                if (mZrHungAppEyeUiProbe != null) {
                                    mZrHungAppEyeUiProbe.endDispatching();
                                }
                            }
                            dispatchEnd = needEndTime ? SystemClock.uptimeMillis() : 0;
                            if (traceTag != 0) {
                                Trace.traceEnd(traceTag);
                            }
                            if (logSlowDelivery) {
                                me2 = me;
                                queue2 = queue;
                                z2 = logSlowDelivery;
                                me = traceTag;
                                logging2 = logging3;
                                msg3 = msg2;
                            } else if (slowDeliveryDetected) {
                                if (dispatchStart - msg2.when <= 10) {
                                    Slog.w(TAG, "Drained");
                                    slowDeliveryDetected = false;
                                }
                                me2 = me;
                                queue2 = queue;
                                me = traceTag;
                                logging2 = logging3;
                                msg3 = msg2;
                            } else {
                                me2 = me;
                                queue2 = queue;
                                me = traceTag;
                                z2 = logSlowDelivery;
                                logging2 = logging3;
                                msg3 = msg2;
                                if (showSlowLog(slowDeliveryThresholdMs2, msg2.when, dispatchStart, "delivery", msg2)) {
                                    slowDeliveryDetected = true;
                                }
                            }
                            if (logSlowDispatch) {
                                showSlowLog(slowDispatchThresholdMs2, dispatchStart, dispatchEnd, "dispatch", msg3);
                            }
                            if (logging2 == null) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("<<<<< Finished to ");
                                message = msg3;
                                stringBuilder.append(message.target);
                                stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                stringBuilder.append(message.callback);
                                logging2.println(stringBuilder.toString());
                            } else {
                                message = msg3;
                            }
                            slowDispatchThresholdMs = Binder.clearCallingIdentity();
                            if (ident == slowDispatchThresholdMs) {
                                String str2 = TAG;
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("Thread identity changed from 0x");
                                stringBuilder5.append(Long.toHexString(ident));
                                stringBuilder5.append(" to 0x");
                                stringBuilder5.append(Long.toHexString(slowDispatchThresholdMs));
                                stringBuilder5.append(" while dispatching to ");
                                stringBuilder5.append(message.target.getClass().getName());
                                stringBuilder5.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                stringBuilder5.append(message.callback);
                                stringBuilder5.append(" what=");
                                stringBuilder5.append(message.what);
                                Log.wtf(str2, stringBuilder5.toString());
                            }
                            message.recycleUnchecked();
                            thresholdOverride = i;
                            me = me2;
                            queue = queue2;
                        }
                    } else {
                        i = thresholdOverride;
                    }
                    z = false;
                    logSlowDelivery = z;
                    if (slowDispatchThresholdMs2 <= 0) {
                    }
                    logSlowDispatch = slowDispatchThresholdMs2 <= 0;
                    z3 = false;
                    needStartTime = z3;
                    needEndTime = logSlowDispatch;
                    Trace.traceBegin(traceTag, msg2.target.getTraceName(msg2));
                    if (needStartTime) {
                    }
                    dispatchStart = needStartTime ? SystemClock.uptimeMillis() : 0;
                    try {
                        if (BlockMonitor.isInMainThread()) {
                        }
                        msg2.target.dispatchMessage(msg2);
                        if (BlockMonitor.isInMainThread()) {
                        }
                        if (needEndTime) {
                        }
                        if (traceTag != 0) {
                        }
                        if (logSlowDelivery) {
                        }
                        if (logSlowDispatch) {
                        }
                        if (logging2 == null) {
                        }
                        slowDispatchThresholdMs = Binder.clearCallingIdentity();
                        if (ident == slowDispatchThresholdMs) {
                        }
                        message.recycleUnchecked();
                        thresholdOverride = i;
                        me = me2;
                        queue = queue2;
                    } catch (Throwable th3) {
                        th = th3;
                        me2 = me;
                        queue2 = queue;
                        z2 = logSlowDelivery;
                        me = traceTag;
                        message = msg2;
                        if (me != null) {
                            Trace.traceEnd(me);
                        }
                        throw th;
                    }
                }
                return;
            }
        }
        throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
    }

    private static boolean showSlowLog(long threshold, long measureStart, long measureEnd, String what, Message msg) {
        long actualTime = measureEnd - measureStart;
        if (actualTime < threshold) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Slow ");
        stringBuilder.append(what);
        stringBuilder.append(" took ");
        stringBuilder.append(actualTime);
        stringBuilder.append("ms ");
        stringBuilder.append(Thread.currentThread().getName());
        stringBuilder.append(" h=");
        stringBuilder.append(msg.target.getClass().getName());
        stringBuilder.append(" c=");
        stringBuilder.append(msg.callback);
        stringBuilder.append(" m=");
        stringBuilder.append(msg.what);
        Slog.w(str, stringBuilder.toString());
        return true;
    }

    public static Looper myLooper() {
        return (Looper) sThreadLocal.get();
    }

    public static MessageQueue myQueue() {
        return myLooper().mQueue;
    }

    private Looper(boolean quitAllowed) {
        this.mQueue = new MessageQueue(quitAllowed);
        mZrHungAppEyeUiProbe = HwFrameworkFactory.getAppEyeUiProbe();
    }

    public boolean isCurrentThread() {
        return Thread.currentThread() == this.mThread;
    }

    public void setMessageLogging(Printer printer) {
        this.mLogging = printer;
    }

    public void setTraceTag(long traceTag) {
        this.mTraceTag = traceTag;
    }

    public void setSlowLogThresholdMs(long slowDispatchThresholdMs, long slowDeliveryThresholdMs) {
        this.mSlowDispatchThresholdMs = slowDispatchThresholdMs;
        this.mSlowDeliveryThresholdMs = slowDeliveryThresholdMs;
    }

    public void quit() {
        this.mQueue.quit(false);
    }

    public void quitSafely() {
        this.mQueue.quit(true);
    }

    public Thread getThread() {
        return this.mThread;
    }

    public MessageQueue getQueue() {
        return this.mQueue;
    }

    public void dump(Printer pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(toString());
        pw.println(stringBuilder.toString());
        MessageQueue messageQueue = this.mQueue;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append("  ");
        messageQueue.dump(pw, stringBuilder2.toString(), null);
    }

    public void dump(Printer pw, String prefix, Handler handler) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(toString());
        pw.println(stringBuilder.toString());
        MessageQueue messageQueue = this.mQueue;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append("  ");
        messageQueue.dump(pw, stringBuilder2.toString(), handler);
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long looperToken = proto.start(fieldId);
        proto.write(1138166333441L, this.mThread.getName());
        proto.write(1112396529666L, this.mThread.getId());
        this.mQueue.writeToProto(proto, 1146756268035L);
        proto.end(looperToken);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Looper (");
        stringBuilder.append(this.mThread.getName());
        stringBuilder.append(", tid ");
        stringBuilder.append(this.mThread.getId());
        stringBuilder.append(") {");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
