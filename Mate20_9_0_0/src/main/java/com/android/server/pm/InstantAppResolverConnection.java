package com.android.server.pm;

import android.app.IInstantAppResolver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.InstantAppResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IRemoteCallback;
import android.os.IRemoteCallback.Stub;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.TimedRemoteCaller;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

final class InstantAppResolverConnection implements DeathRecipient {
    private static final long BIND_SERVICE_TIMEOUT_MS = (Build.IS_ENG ? 500 : 300);
    private static final long CALL_SERVICE_TIMEOUT_MS = (Build.IS_ENG ? 200 : 100);
    private static final boolean DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    private static final int STATE_BINDING = 1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PENDING = 2;
    private static final String TAG = "PackageManager";
    private final Handler mBgHandler;
    @GuardedBy("mLock")
    private int mBindState = 0;
    private final Context mContext;
    private final GetInstantAppResolveInfoCaller mGetInstantAppResolveInfoCaller = new GetInstantAppResolveInfoCaller();
    private final Intent mIntent;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private IInstantAppResolver mRemoteInstance;
    private final ServiceConnection mServiceConnection = new MyServiceConnection(this, null);

    public static class ConnectionException extends Exception {
        public static final int FAILURE_BIND = 1;
        public static final int FAILURE_CALL = 2;
        public static final int FAILURE_INTERRUPTED = 3;
        public final int failure;

        public ConnectionException(int _failure) {
            this.failure = _failure;
        }
    }

    private static final class GetInstantAppResolveInfoCaller extends TimedRemoteCaller<List<InstantAppResolveInfo>> {
        private final IRemoteCallback mCallback = new Stub() {
            public void sendResult(Bundle data) throws RemoteException {
                GetInstantAppResolveInfoCaller.this.onRemoteMethodResult(data.getParcelableArrayList("android.app.extra.RESOLVE_INFO"), data.getInt("android.app.extra.SEQUENCE", -1));
            }
        };

        public GetInstantAppResolveInfoCaller() {
            super(InstantAppResolverConnection.CALL_SERVICE_TIMEOUT_MS);
        }

        public List<InstantAppResolveInfo> getInstantAppResolveInfoList(IInstantAppResolver target, Intent sanitizedIntent, int[] hashPrefix, String token) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.getInstantAppResolveInfoList(sanitizedIntent, hashPrefix, token, sequence, this.mCallback);
            return (List) getResultTimed(sequence);
        }
    }

    private final class MyServiceConnection implements ServiceConnection {
        private MyServiceConnection() {
        }

        /* synthetic */ MyServiceConnection(InstantAppResolverConnection x0, AnonymousClass1 x1) {
            this();
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (InstantAppResolverConnection.DEBUG_INSTANT) {
                Slog.d(InstantAppResolverConnection.TAG, "Connected to instant app resolver");
            }
            synchronized (InstantAppResolverConnection.this.mLock) {
                InstantAppResolverConnection.this.mRemoteInstance = IInstantAppResolver.Stub.asInterface(service);
                if (InstantAppResolverConnection.this.mBindState == 2) {
                    InstantAppResolverConnection.this.mBindState = 0;
                }
                try {
                    service.linkToDeath(InstantAppResolverConnection.this, 0);
                } catch (RemoteException e) {
                    InstantAppResolverConnection.this.handleBinderDiedLocked();
                }
                InstantAppResolverConnection.this.mLock.notifyAll();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            if (InstantAppResolverConnection.DEBUG_INSTANT) {
                Slog.d(InstantAppResolverConnection.TAG, "Disconnected from instant app resolver");
            }
            synchronized (InstantAppResolverConnection.this.mLock) {
                InstantAppResolverConnection.this.handleBinderDiedLocked();
            }
        }
    }

    public static abstract class PhaseTwoCallback {
        abstract void onPhaseTwoResolved(List<InstantAppResolveInfo> list, long j);
    }

    public InstantAppResolverConnection(Context context, ComponentName componentName, String action) {
        this.mContext = context;
        this.mIntent = new Intent(action).setComponent(componentName);
        this.mBgHandler = BackgroundThread.getHandler();
    }

    public final List<InstantAppResolveInfo> getInstantAppResolveInfoList(Intent sanitizedIntent, int[] hashPrefix, String token) throws ConnectionException {
        throwIfCalledOnMainThread();
        IInstantAppResolver target = null;
        try {
            List<InstantAppResolveInfo> instantAppResolveInfoList = this.mGetInstantAppResolveInfoCaller.getInstantAppResolveInfoList(getRemoteInstanceLazy(token), sanitizedIntent, hashPrefix, token);
            synchronized (this.mLock) {
                this.mLock.notifyAll();
            }
            return instantAppResolveInfoList;
        } catch (TimeoutException e) {
            throw new ConnectionException(1);
        } catch (InterruptedException e2) {
            throw new ConnectionException(3);
        } catch (TimeoutException e3) {
            throw new ConnectionException(2);
        } catch (RemoteException e4) {
            synchronized (this.mLock) {
                this.mLock.notifyAll();
                return null;
            }
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mLock.notifyAll();
            }
        }
    }

    public final void getInstantAppIntentFilterList(Intent sanitizedIntent, int[] hashPrefix, String token, PhaseTwoCallback callback, Handler callbackHandler, long startTime) throws ConnectionException {
        final Handler handler = callbackHandler;
        final PhaseTwoCallback phaseTwoCallback = callback;
        final long j = startTime;
        try {
            getRemoteInstanceLazy(token).getInstantAppIntentFilterList(sanitizedIntent, hashPrefix, token, new Stub() {
                public void sendResult(Bundle data) throws RemoteException {
                    handler.post(new -$$Lambda$InstantAppResolverConnection$1$eWvILRylTGnW4MEpM1wMNc5IMnY(phaseTwoCallback, data.getParcelableArrayList("android.app.extra.RESOLVE_INFO"), j));
                }
            });
        } catch (TimeoutException e) {
            throw new ConnectionException(1);
        } catch (InterruptedException e2) {
            throw new ConnectionException(3);
        } catch (RemoteException e3) {
        }
    }

    private IInstantAppResolver getRemoteInstanceLazy(String token) throws ConnectionException, TimeoutException, InterruptedException {
        long binderToken = Binder.clearCallingIdentity();
        try {
            IInstantAppResolver bind = bind(token);
            return bind;
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    @GuardedBy("mLock")
    private void waitForBindLocked(String token) throws TimeoutException, InterruptedException {
        long startMillis = SystemClock.uptimeMillis();
        while (this.mBindState != 0 && this.mRemoteInstance == null) {
            long remainingMillis = BIND_SERVICE_TIMEOUT_MS - (SystemClock.uptimeMillis() - startMillis);
            if (remainingMillis > 0) {
                this.mLock.wait(remainingMillis);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                stringBuilder.append(token);
                stringBuilder.append("] Didn't bind to resolver in time!");
                throw new TimeoutException(stringBuilder.toString());
            }
        }
    }

    /* JADX WARNING: Missing block: B:36:0x0076, code:
            r1 = false;
     */
    /* JADX WARNING: Missing block: B:37:0x0079, code:
            if (r0 == false) goto L_0x00a7;
     */
    /* JADX WARNING: Missing block: B:40:0x007d, code:
            if (DEBUG_INSTANT == false) goto L_0x009a;
     */
    /* JADX WARNING: Missing block: B:41:0x007f, code:
            r6 = TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("[");
            r7.append(r13);
            r7.append("] Previous connection never established; rebinding");
            android.util.Slog.i(r6, r7.toString());
     */
    /* JADX WARNING: Missing block: B:42:0x009a, code:
            r12.mContext.unbindService(r12.mServiceConnection);
     */
    /* JADX WARNING: Missing block: B:44:0x00a3, code:
            r6 = null;
            r2 = r1;
     */
    /* JADX WARNING: Missing block: B:46:0x00a9, code:
            if (DEBUG_INSTANT == false) goto L_0x00c6;
     */
    /* JADX WARNING: Missing block: B:47:0x00ab, code:
            r6 = TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("[");
            r7.append(r13);
            r7.append("] Binding to instant app resolver");
            android.util.Slog.v(r6, r7.toString());
     */
    /* JADX WARNING: Missing block: B:48:0x00c6, code:
            r1 = r12.mContext.bindServiceAsUser(r12.mIntent, r12.mServiceConnection, 67108865, android.os.UserHandle.SYSTEM);
     */
    /* JADX WARNING: Missing block: B:49:0x00d9, code:
            if (r1 == false) goto L_0x00fe;
     */
    /* JADX WARNING: Missing block: B:50:0x00db, code:
            r4 = r12.mLock;
     */
    /* JADX WARNING: Missing block: B:51:0x00dd, code:
            monitor-enter(r4);
     */
    /* JADX WARNING: Missing block: B:53:?, code:
            waitForBindLocked(r13);
            r2 = r12.mRemoteInstance;
     */
    /* JADX WARNING: Missing block: B:54:0x00e4, code:
            monitor-exit(r4);
     */
    /* JADX WARNING: Missing block: B:55:0x00e5, code:
            r7 = r12.mLock;
     */
    /* JADX WARNING: Missing block: B:56:0x00e7, code:
            monitor-enter(r7);
     */
    /* JADX WARNING: Missing block: B:57:0x00e8, code:
            if (r1 == false) goto L_0x00ef;
     */
    /* JADX WARNING: Missing block: B:58:0x00ea, code:
            if (r2 != false) goto L_0x00ef;
     */
    /* JADX WARNING: Missing block: B:60:?, code:
            r12.mBindState = 2;
     */
    /* JADX WARNING: Missing block: B:61:0x00ef, code:
            r12.mBindState = 0;
     */
    /* JADX WARNING: Missing block: B:62:0x00f1, code:
            r12.mLock.notifyAll();
     */
    /* JADX WARNING: Missing block: B:63:0x00f6, code:
            monitor-exit(r7);
     */
    /* JADX WARNING: Missing block: B:64:0x00f7, code:
            return r2;
     */
    /* JADX WARNING: Missing block: B:73:0x00fe, code:
            r7 = TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("[");
            r8.append(r13);
            r8.append("] Failed to bind to: ");
            r8.append(r12.mIntent);
            android.util.Slog.w(r7, r8.toString());
     */
    /* JADX WARNING: Missing block: B:74:0x0123, code:
            throw new com.android.server.pm.InstantAppResolverConnection.ConnectionException(1);
     */
    /* JADX WARNING: Missing block: B:76:0x0126, code:
            monitor-enter(r12.mLock);
     */
    /* JADX WARNING: Missing block: B:77:0x0127, code:
            if (r2 == false) goto L_0x012e;
     */
    /* JADX WARNING: Missing block: B:80:?, code:
            r12.mBindState = 2;
     */
    /* JADX WARNING: Missing block: B:81:0x012e, code:
            r12.mBindState = 0;
     */
    /* JADX WARNING: Missing block: B:82:0x0130, code:
            r12.mLock.notifyAll();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private IInstantAppResolver bind(String token) throws ConnectionException, TimeoutException, InterruptedException {
        boolean doUnbind = false;
        synchronized (this.mLock) {
            IInstantAppResolver iInstantAppResolver;
            if (this.mRemoteInstance != null) {
                iInstantAppResolver = this.mRemoteInstance;
                return iInstantAppResolver;
            }
            String str;
            if (this.mBindState == 2) {
                if (DEBUG_INSTANT) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[");
                    stringBuilder.append(token);
                    stringBuilder.append("] Previous bind timed out; waiting for connection");
                    Slog.i(str, stringBuilder.toString());
                }
                try {
                    waitForBindLocked(token);
                    if (this.mRemoteInstance != null) {
                        iInstantAppResolver = this.mRemoteInstance;
                        return iInstantAppResolver;
                    }
                } catch (TimeoutException e) {
                    doUnbind = true;
                }
            }
            if (this.mBindState == 1) {
                if (DEBUG_INSTANT) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[");
                    stringBuilder2.append(token);
                    stringBuilder2.append("] Another thread is binding; waiting for connection");
                    Slog.i(str, stringBuilder2.toString());
                }
                waitForBindLocked(token);
                if (this.mRemoteInstance != null) {
                    iInstantAppResolver = this.mRemoteInstance;
                    return iInstantAppResolver;
                }
                throw new ConnectionException(1);
            }
            this.mBindState = 1;
        }
    }

    private void throwIfCalledOnMainThread() {
        if (Thread.currentThread() == this.mContext.getMainLooper().getThread()) {
            throw new RuntimeException("Cannot invoke on the main thread");
        }
    }

    void optimisticBind() {
        this.mBgHandler.post(new -$$Lambda$InstantAppResolverConnection$D-JKXi4qrYjnPQMOwj8UtfZenps(this));
    }

    /* JADX WARNING: Removed duplicated region for block: B:6:0x0014 A:{Splitter: B:0:0x0000, ExcHandler: com.android.server.pm.InstantAppResolverConnection.ConnectionException (r0_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:6:0x0014 A:{Splitter: B:0:0x0000, ExcHandler: com.android.server.pm.InstantAppResolverConnection.ConnectionException (r0_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:6:0x0014, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:7:0x0015, code:
            android.util.Slog.e(TAG, "Optimistic bind failed.", r0);
     */
    /* JADX WARNING: Missing block: B:8:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$optimisticBind$0(InstantAppResolverConnection instantAppResolverConnection) {
        try {
            if (instantAppResolverConnection.bind("Optimistic Bind") != null && DEBUG_INSTANT) {
                Slog.i(TAG, "Optimistic bind succeeded.");
            }
        } catch (Exception e) {
        }
    }

    public void binderDied() {
        if (DEBUG_INSTANT) {
            Slog.d(TAG, "Binder to instant app resolver died");
        }
        synchronized (this.mLock) {
            handleBinderDiedLocked();
        }
        optimisticBind();
    }

    @GuardedBy("mLock")
    private void handleBinderDiedLocked() {
        if (this.mRemoteInstance != null) {
            try {
                this.mRemoteInstance.asBinder().unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
            }
        }
        this.mRemoteInstance = null;
    }
}
