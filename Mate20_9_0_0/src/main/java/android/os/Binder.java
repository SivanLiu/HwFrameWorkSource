package android.os;

import android.os.IBinder.DeathRecipient;
import android.provider.SettingsStringUtil;
import android.util.ExceptionUtils;
import android.util.Log;
import com.android.internal.os.BinderCallsStats;
import com.android.internal.os.BinderCallsStats.CallSession;
import com.android.internal.os.BinderInternal;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;
import com.android.internal.util.FunctionalUtils.ThrowingSupplier;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import libcore.io.IoUtils;
import libcore.util.NativeAllocationRegistry;

public class Binder implements IBinder {
    private static final boolean BINDER_DISPLAY_DETAILS = SystemProperties.getBoolean("persist.binder.display_details", false);
    public static final boolean CHECK_PARCEL_SIZE = false;
    private static final boolean FIND_POTENTIAL_LEAKS = false;
    public static boolean LOG_RUNTIME_EXCEPTION = false;
    private static final int NATIVE_ALLOCATION_SIZE = 500;
    static final String TAG = "Binder";
    private static volatile String sDumpDisabled = null;
    private static volatile boolean sTracingEnabled = false;
    private static volatile TransactionTracker sTransactionTracker = null;
    static volatile boolean sWarnOnBlocking = false;
    private String mDescriptor;
    private final long mObject = getNativeBBinderHolder();
    private IInterface mOwner;

    private static class NoImagePreloadHolder {
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(Binder.class.getClassLoader(), Binder.getNativeFinalizer(), 500);

        private NoImagePreloadHolder() {
        }
    }

    public static final native void blockUntilThreadAvailable();

    public static final native long clearCallingIdentity();

    public static final native void flushPendingCommands();

    public static final native int getCallingPid();

    public static final native int getCallingUid();

    private static native long getFinalizer();

    private static native long getNativeBBinderHolder();

    private static native long getNativeFinalizer();

    public static final native int getThreadStrictModePolicy();

    public static final native void restoreCallingIdentity(long j);

    public static final native void setThreadStrictModePolicy(int i);

    public static void enableTracing() {
        sTracingEnabled = true;
    }

    public static void disableTracing() {
        sTracingEnabled = false;
    }

    public static boolean isTracingEnabled() {
        return sTracingEnabled || BINDER_DISPLAY_DETAILS;
    }

    public static synchronized TransactionTracker getTransactionTracker() {
        TransactionTracker transactionTracker;
        synchronized (Binder.class) {
            if (sTransactionTracker == null) {
                sTransactionTracker = new TransactionTracker();
            }
            transactionTracker = sTransactionTracker;
        }
        return transactionTracker;
    }

    public static void setWarnOnBlocking(boolean warnOnBlocking) {
        sWarnOnBlocking = warnOnBlocking;
    }

    public static IBinder allowBlocking(IBinder binder) {
        try {
            if (binder instanceof BinderProxy) {
                ((BinderProxy) binder).mWarnOnBlocking = false;
            } else if (!(binder == null || binder.getInterfaceDescriptor() == null || binder.queryLocalInterface(binder.getInterfaceDescriptor()) != null)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to allow blocking on interface ");
                stringBuilder.append(binder);
                Log.w(str, stringBuilder.toString());
            }
        } catch (RemoteException ignored) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to allow blocking on interface ");
            stringBuilder2.append(binder);
            Log.e(str2, stringBuilder2.toString(), ignored);
        }
        return binder;
    }

    public static IBinder defaultBlocking(IBinder binder) {
        if (binder instanceof BinderProxy) {
            ((BinderProxy) binder).mWarnOnBlocking = sWarnOnBlocking;
        }
        return binder;
    }

    public static void copyAllowBlocking(IBinder fromBinder, IBinder toBinder) {
        if ((fromBinder instanceof BinderProxy) && (toBinder instanceof BinderProxy)) {
            ((BinderProxy) toBinder).mWarnOnBlocking = ((BinderProxy) fromBinder).mWarnOnBlocking;
        }
    }

    public static final UserHandle getCallingUserHandle() {
        return UserHandle.of(UserHandle.getUserId(getCallingUid()));
    }

    public static final void withCleanCallingIdentity(ThrowingRunnable action) {
        RuntimeException propagate;
        long callingIdentity = clearCallingIdentity();
        try {
            action.runOrThrow();
            restoreCallingIdentity(callingIdentity);
            if (null != null) {
                throw ExceptionUtils.propagate(null);
            }
        } catch (Throwable th) {
            restoreCallingIdentity(callingIdentity);
            if (null != null) {
                propagate = ExceptionUtils.propagate(null);
            }
        }
    }

    public static final <T> T withCleanCallingIdentity(ThrowingSupplier<T> action) {
        long callingIdentity = clearCallingIdentity();
        try {
            Object orThrow = action.getOrThrow();
            restoreCallingIdentity(callingIdentity);
            if (null == null) {
                return orThrow;
            }
            throw ExceptionUtils.propagate(null);
        } catch (Throwable th) {
            restoreCallingIdentity(callingIdentity);
            if (null != null) {
                RuntimeException propagate = ExceptionUtils.propagate(null);
            }
        }
    }

    public static final void joinThreadPool() {
        BinderInternal.joinThreadPool();
    }

    public static final boolean isProxy(IInterface iface) {
        return iface.asBinder() != iface;
    }

    public Binder() {
        NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mObject);
    }

    public void attachInterface(IInterface owner, String descriptor) {
        this.mOwner = owner;
        this.mDescriptor = descriptor;
    }

    public String getInterfaceDescriptor() {
        return this.mDescriptor;
    }

    public boolean pingBinder() {
        return true;
    }

    public boolean isBinderAlive() {
        return true;
    }

    public IInterface queryLocalInterface(String descriptor) {
        if (this.mDescriptor == null || !this.mDescriptor.equals(descriptor)) {
            return null;
        }
        return this.mOwner;
    }

    public static void setDumpDisabled(String msg) {
        sDumpDisabled = msg;
    }

    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        Throwable th;
        int i = code;
        Parcel parcel = data;
        Parcel parcel2 = reply;
        ParcelFileDescriptor fd;
        if (i == 1598968902) {
            parcel2.writeString(getInterfaceDescriptor());
            return true;
        } else if (i == 1598311760) {
            fd = data.readFileDescriptor();
            String[] args = data.readStringArray();
            if (fd != null) {
                try {
                    try {
                        dump(fd.getFileDescriptor(), args);
                        IoUtils.closeQuietly(fd);
                    } catch (Throwable th2) {
                        th = th2;
                        IoUtils.closeQuietly(fd);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    IoUtils.closeQuietly(fd);
                    throw th;
                }
            }
            if (parcel2 != null) {
                reply.writeNoException();
            } else {
                StrictMode.clearGatheredViolations();
            }
            return true;
        } else {
            if (i != 1598246212) {
                return false;
            }
            fd = data.readFileDescriptor();
            ParcelFileDescriptor out = data.readFileDescriptor();
            ParcelFileDescriptor err = data.readFileDescriptor();
            String[] args2 = data.readStringArray();
            ShellCallback shellCallback = (ShellCallback) ShellCallback.CREATOR.createFromParcel(parcel);
            ResultReceiver resultReceiver = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel);
            if (out != null) {
                FileDescriptor fileDescriptor;
                if (fd != null) {
                    try {
                        fileDescriptor = fd.getFileDescriptor();
                    } catch (Throwable th4) {
                        IoUtils.closeQuietly(fd);
                        IoUtils.closeQuietly(out);
                        IoUtils.closeQuietly(err);
                        if (parcel2 != null) {
                            reply.writeNoException();
                        } else {
                            StrictMode.clearGatheredViolations();
                        }
                    }
                } else {
                    fileDescriptor = null;
                }
                shellCommand(fileDescriptor, out.getFileDescriptor(), err != null ? err.getFileDescriptor() : out.getFileDescriptor(), args2, shellCallback, resultReceiver);
            }
            IoUtils.closeQuietly(fd);
            IoUtils.closeQuietly(out);
            IoUtils.closeQuietly(err);
            if (parcel2 != null) {
                reply.writeNoException();
            } else {
                StrictMode.clearGatheredViolations();
            }
            return true;
        }
    }

    public void dump(FileDescriptor fd, String[] args) {
        PrintWriter pw = new FastPrintWriter(new FileOutputStream(fd));
        try {
            doDump(fd, pw, args);
        } finally {
            pw.flush();
        }
    }

    void doDump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (sDumpDisabled == null) {
            try {
                dump(fd, pw, args);
                return;
            } catch (SecurityException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Security exception: ");
                stringBuilder.append(e.getMessage());
                pw.println(stringBuilder.toString());
                throw e;
            } catch (Throwable e2) {
                pw.println();
                pw.println("Exception occurred while dumping:");
                e2.printStackTrace(pw);
                return;
            }
        }
        pw.println(sDumpDisabled);
    }

    public void dumpAsync(FileDescriptor fd, String[] args) {
        final PrintWriter pw = new FastPrintWriter(new FileOutputStream(fd));
        final FileDescriptor fileDescriptor = fd;
        final String[] strArr = args;
        new Thread("Binder.dumpAsync") {
            public void run() {
                try {
                    Binder.this.dump(fileDescriptor, pw, strArr);
                } finally {
                    pw.flush();
                }
            }
        }.start();
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
    }

    public void shellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        onShellCommand(in, out, err, args, callback, resultReceiver);
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        PrintWriter pw = new FastPrintWriter(new FileOutputStream(err != null ? err : out));
        pw.println("No shell command implementation.");
        pw.flush();
        resultReceiver.send(0, null);
    }

    public final boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (data != null) {
            data.setDataPosition(0);
        }
        boolean r = onTransact(code, data, reply, flags);
        if (reply != null) {
            reply.setDataPosition(0);
        }
        return r;
    }

    public void linkToDeath(DeathRecipient recipient, int flags) {
    }

    public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
        return true;
    }

    static void checkParcel(IBinder obj, int code, Parcel parcel, String msg) {
    }

    /* JADX WARNING: Missing block: B:9:0x0045, code skipped:
            if (r8 != false) goto L_0x0047;
     */
    /* JADX WARNING: Missing block: B:10:0x0047, code skipped:
            android.os.Trace.traceEnd(1);
     */
    /* JADX WARNING: Missing block: B:23:0x0077, code skipped:
            if (r8 == false) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:24:0x007a, code skipped:
            checkParcel(r1, r2, r7, "Unreasonably large binder reply buffer");
            r7.recycle();
            r6.recycle();
            android.os.StrictMode.clearGatheredViolations();
            r4.callEnded(r5);
     */
    /* JADX WARNING: Missing block: B:25:0x008c, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean execTransact(int code, long dataObj, long replyObj, int flags) {
        boolean res;
        int i = code;
        int i2 = flags;
        BinderCallsStats binderCallsStats = BinderCallsStats.getInstance();
        CallSession callSession = binderCallsStats.callStarted(this, i);
        Parcel data = Parcel.obtain(dataObj);
        Parcel reply = Parcel.obtain(replyObj);
        boolean tracingEnabled = isTracingEnabled();
        if (tracingEnabled) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getClass().getName());
                stringBuilder.append(SettingsStringUtil.DELIMITER);
                stringBuilder.append(i);
                Trace.traceBegin(1, stringBuilder.toString());
            } catch (RemoteException | RuntimeException e) {
                if (LOG_RUNTIME_EXCEPTION) {
                    Log.w(TAG, "Caught a RuntimeException from the binder stub implementation.", e);
                }
                if ((i2 & 1) == 0) {
                    reply.setDataPosition(0);
                    reply.writeException(e);
                } else if (e instanceof RemoteException) {
                    Log.w(TAG, "Binder call failed.", e);
                } else {
                    Log.w(TAG, "Caught a RuntimeException from the binder stub implementation.", e);
                }
                res = true;
            } catch (Throwable th) {
                if (tracingEnabled) {
                    Trace.traceEnd(1);
                }
            }
        }
        res = onTransact(i, data, reply, i2);
    }
}
