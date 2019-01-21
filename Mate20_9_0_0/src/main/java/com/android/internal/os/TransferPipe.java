package com.android.internal.os;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import libcore.io.IoUtils;

public class TransferPipe implements Runnable, Closeable {
    static final boolean DEBUG = false;
    static final long DEFAULT_TIMEOUT = 5000;
    static final String TAG = "TransferPipe";
    String mBufferPrefix;
    boolean mComplete;
    long mEndTime;
    String mFailure;
    final ParcelFileDescriptor[] mFds;
    FileDescriptor mOutFd;
    final Thread mThread;

    interface Caller {
        void go(IInterface iInterface, FileDescriptor fileDescriptor, String str, String[] strArr) throws RemoteException;
    }

    public TransferPipe() throws IOException {
        this(null);
    }

    public TransferPipe(String bufferPrefix) throws IOException {
        this(bufferPrefix, TAG);
    }

    protected TransferPipe(String bufferPrefix, String threadName) throws IOException {
        this.mThread = new Thread(this, threadName);
        this.mFds = ParcelFileDescriptor.createPipe();
        this.mBufferPrefix = bufferPrefix;
    }

    ParcelFileDescriptor getReadFd() {
        return this.mFds[0];
    }

    public ParcelFileDescriptor getWriteFd() {
        return this.mFds[1];
    }

    public void setBufferPrefix(String prefix) {
        this.mBufferPrefix = prefix;
    }

    public static void dumpAsync(IBinder binder, FileDescriptor out, String[] args) throws IOException, RemoteException {
        goDump(binder, out, args);
    }

    public static byte[] dumpAsync(IBinder binder, String... args) throws IOException, RemoteException {
        Throwable th;
        Throwable th2;
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ByteArrayOutputStream combinedBuffer;
        try {
            dumpAsync(binder, pipe[1].getFileDescriptor(), args);
            pipe[1].close();
            pipe[1] = null;
            byte[] buffer = new byte[4096];
            combinedBuffer = new ByteArrayOutputStream();
            FileInputStream is = new FileInputStream(pipe[0].getFileDescriptor());
            while (true) {
                try {
                    int numRead = is.read(buffer);
                    if (numRead == -1) {
                        $closeResource(null, is);
                        byte[] toByteArray = combinedBuffer.toByteArray();
                        $closeResource(null, combinedBuffer);
                        pipe[0].close();
                        IoUtils.closeQuietly(pipe[1]);
                        return toByteArray;
                    }
                    combinedBuffer.write(buffer, 0, numRead);
                } catch (Throwable th22) {
                    Throwable th3 = th22;
                    th22 = th;
                    th = th3;
                }
            }
            $closeResource(th22, is);
            throw th;
        } catch (Throwable th4) {
            pipe[0].close();
            IoUtils.closeQuietly(pipe[1]);
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

    static void go(Caller caller, IInterface iface, FileDescriptor out, String prefix, String[] args) throws IOException, RemoteException {
        go(caller, iface, out, prefix, args, DEFAULT_TIMEOUT);
    }

    /* JADX WARNING: Missing block: B:15:0x002a, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static void go(Caller caller, IInterface iface, FileDescriptor out, String prefix, String[] args, long timeout) throws IOException, RemoteException {
        if (iface.asBinder() instanceof Binder) {
            try {
                caller.go(iface, out, prefix, args);
            } catch (RemoteException e) {
            }
            return;
        }
        TransferPipe tp = new TransferPipe();
        caller.go(iface, tp.getWriteFd().getFileDescriptor(), prefix, args);
        tp.go(out, timeout);
        $closeResource(null, tp);
    }

    static void goDump(IBinder binder, FileDescriptor out, String[] args) throws IOException, RemoteException {
        goDump(binder, out, args, DEFAULT_TIMEOUT);
    }

    /* JADX WARNING: Missing block: B:15:0x0026, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static void goDump(IBinder binder, FileDescriptor out, String[] args, long timeout) throws IOException, RemoteException {
        if (binder instanceof Binder) {
            try {
                binder.dump(out, args);
            } catch (RemoteException e) {
            }
            return;
        }
        TransferPipe tp = new TransferPipe();
        binder.dumpAsync(tp.getWriteFd().getFileDescriptor(), args);
        tp.go(out, timeout);
        $closeResource(null, tp);
    }

    public void go(FileDescriptor out) throws IOException {
        go(out, DEFAULT_TIMEOUT);
    }

    public void go(FileDescriptor out, long timeout) throws IOException {
        try {
            synchronized (this) {
                this.mOutFd = out;
                this.mEndTime = SystemClock.uptimeMillis() + timeout;
                closeFd(1);
                this.mThread.start();
                while (this.mFailure == null && !this.mComplete) {
                    long waitTime = this.mEndTime - SystemClock.uptimeMillis();
                    if (waitTime > 0) {
                        try {
                            wait(waitTime);
                        } catch (InterruptedException e) {
                        }
                    } else {
                        this.mThread.interrupt();
                        throw new IOException("Timeout");
                    }
                }
                if (this.mFailure == null) {
                } else {
                    throw new IOException(this.mFailure);
                }
            }
            kill();
        } catch (Throwable th) {
            kill();
        }
    }

    void closeFd(int num) {
        if (this.mFds[num] != null) {
            try {
                this.mFds[num].close();
            } catch (IOException e) {
            }
            this.mFds[num] = null;
        }
    }

    public void close() {
        kill();
    }

    public void kill() {
        synchronized (this) {
            closeFd(0);
            closeFd(1);
        }
    }

    protected OutputStream getNewOutputStream() {
        return new FileOutputStream(this.mOutFd);
    }

    /* JADX WARNING: Missing block: B:10:0x0023, code skipped:
            r3 = null;
            r4 = true;
     */
    /* JADX WARNING: Missing block: B:11:0x0027, code skipped:
            if (r11.mBufferPrefix == null) goto L_0x002f;
     */
    /* JADX WARNING: Missing block: B:12:0x0029, code skipped:
            r3 = r11.mBufferPrefix.getBytes();
     */
    /* JADX WARNING: Missing block: B:14:?, code skipped:
            r5 = r2.read(r0);
            r6 = r5;
     */
    /* JADX WARNING: Missing block: B:15:0x0035, code skipped:
            if (r5 <= 0) goto L_0x006a;
     */
    /* JADX WARNING: Missing block: B:16:0x0037, code skipped:
            r5 = 0;
     */
    /* JADX WARNING: Missing block: B:17:0x0038, code skipped:
            if (r3 != null) goto L_0x003e;
     */
    /* JADX WARNING: Missing block: B:18:0x003a, code skipped:
            r1.write(r0, 0, r6);
     */
    /* JADX WARNING: Missing block: B:19:0x003e, code skipped:
            r8 = 0;
     */
    /* JADX WARNING: Missing block: B:20:0x0040, code skipped:
            if (r5 >= r6) goto L_0x0062;
     */
    /* JADX WARNING: Missing block: B:22:0x0046, code skipped:
            if (r0[r5] == (byte) 10) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:23:0x0048, code skipped:
            if (r5 <= r8) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:24:0x004a, code skipped:
            r1.write(r0, r8, r5 - r8);
     */
    /* JADX WARNING: Missing block: B:25:0x004f, code skipped:
            r8 = r5;
     */
    /* JADX WARNING: Missing block: B:26:0x0050, code skipped:
            if (r4 == false) goto L_0x0056;
     */
    /* JADX WARNING: Missing block: B:27:0x0052, code skipped:
            r1.write(r3);
            r4 = false;
     */
    /* JADX WARNING: Missing block: B:28:0x0056, code skipped:
            r5 = r5 + 1;
     */
    /* JADX WARNING: Missing block: B:29:0x0057, code skipped:
            if (r5 >= r6) goto L_0x005d;
     */
    /* JADX WARNING: Missing block: B:31:0x005b, code skipped:
            if (r0[r5] != (byte) 10) goto L_0x0056;
     */
    /* JADX WARNING: Missing block: B:32:0x005d, code skipped:
            if (r5 >= r6) goto L_0x0060;
     */
    /* JADX WARNING: Missing block: B:33:0x005f, code skipped:
            r4 = true;
     */
    /* JADX WARNING: Missing block: B:34:0x0060, code skipped:
            r5 = r5 + 1;
     */
    /* JADX WARNING: Missing block: B:35:0x0062, code skipped:
            if (r6 <= r8) goto L_0x0069;
     */
    /* JADX WARNING: Missing block: B:36:0x0064, code skipped:
            r1.write(r0, r8, r6 - r8);
     */
    /* JADX WARNING: Missing block: B:38:0x006a, code skipped:
            r11.mThread.isInterrupted();
     */
    /* JADX WARNING: Missing block: B:39:0x0071, code skipped:
            monitor-enter(r11);
     */
    /* JADX WARNING: Missing block: B:41:?, code skipped:
            r11.mComplete = true;
            notifyAll();
     */
    /* JADX WARNING: Missing block: B:42:0x0077, code skipped:
            monitor-exit(r11);
     */
    /* JADX WARNING: Missing block: B:43:0x0078, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:47:0x007c, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:48:0x007d, code skipped:
            monitor-enter(r11);
     */
    /* JADX WARNING: Missing block: B:50:?, code skipped:
            r11.mFailure = r5.toString();
            notifyAll();
     */
    /* JADX WARNING: Missing block: B:52:0x0088, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        byte[] buffer = new byte[1024];
        synchronized (this) {
            ParcelFileDescriptor readFd = getReadFd();
            if (readFd == null) {
                Slog.w(TAG, "Pipe has been closed...");
                return;
            }
            FileInputStream fis = new FileInputStream(readFd.getFileDescriptor());
            OutputStream fos = getNewOutputStream();
        }
    }
}
