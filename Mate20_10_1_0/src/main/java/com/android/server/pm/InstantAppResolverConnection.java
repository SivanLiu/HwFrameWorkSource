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
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.TimedRemoteCaller;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import com.android.server.pm.InstantAppResolverConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

final class InstantAppResolverConnection implements IBinder.DeathRecipient {
    private static final long BIND_SERVICE_TIMEOUT_MS = (Build.IS_ENG ? 500 : 300);
    /* access modifiers changed from: private */
    public static final long CALL_SERVICE_TIMEOUT_MS = (Build.IS_ENG ? 200 : 100);
    /* access modifiers changed from: private */
    public static final boolean DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    private static final int STATE_BINDING = 1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PENDING = 2;
    private static final String TAG = "PackageManager";
    private final Handler mBgHandler;
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public int mBindState = 0;
    private final Context mContext;
    private final GetInstantAppResolveInfoCaller mGetInstantAppResolveInfoCaller = new GetInstantAppResolveInfoCaller();
    private final Intent mIntent;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public IInstantAppResolver mRemoteInstance;
    private final ServiceConnection mServiceConnection = new MyServiceConnection();

    public static abstract class PhaseTwoCallback {
        /* access modifiers changed from: package-private */
        public abstract void onPhaseTwoResolved(List<InstantAppResolveInfo> list, long j);
    }

    public InstantAppResolverConnection(Context context, ComponentName componentName, String action) {
        this.mContext = context;
        this.mIntent = new Intent(action).setComponent(componentName);
        this.mBgHandler = BackgroundThread.getHandler();
    }

    /* JADX WARNING: Removed duplicated region for block: B:40:0x0051 A[SYNTHETIC] */
    public List<InstantAppResolveInfo> getInstantAppResolveInfoList(Intent sanitizedIntent, int[] hashPrefix, int userId, String token) throws ConnectionException {
        throwIfCalledOnMainThread();
        try {
            try {
                List<InstantAppResolveInfo> instantAppResolveInfoList = this.mGetInstantAppResolveInfoCaller.getInstantAppResolveInfoList(getRemoteInstanceLazy(token), sanitizedIntent, hashPrefix, userId, token);
                synchronized (this.mLock) {
                    this.mLock.notifyAll();
                }
                return instantAppResolveInfoList;
            } catch (TimeoutException e) {
                throw new ConnectionException(2);
            } catch (RemoteException e2) {
                synchronized (this.mLock) {
                    this.mLock.notifyAll();
                    return null;
                }
            } catch (Throwable th) {
                e = th;
                synchronized (this.mLock) {
                }
            }
        } catch (TimeoutException e3) {
            throw new ConnectionException(1);
        } catch (InterruptedException e4) {
            throw new ConnectionException(3);
        } catch (Throwable th2) {
            e = th2;
            synchronized (this.mLock) {
                this.mLock.notifyAll();
            }
            throw e;
        }
    }

    public void getInstantAppIntentFilterList(Intent sanitizedIntent, int[] hashPrefix, int userId, String token, final PhaseTwoCallback callback, final Handler callbackHandler, final long startTime) throws ConnectionException {
        try {
            getRemoteInstanceLazy(token).getInstantAppIntentFilterList(sanitizedIntent, hashPrefix, userId, token, new IRemoteCallback.Stub() {
                /* class com.android.server.pm.InstantAppResolverConnection.AnonymousClass1 */

                public void sendResult(Bundle data) throws RemoteException {
                    callbackHandler.post(new Runnable(data.getParcelableArrayList("android.app.extra.RESOLVE_INFO"), startTime) {
                        /* class com.android.server.pm.$$Lambda$InstantAppResolverConnection$1$eWvILRylTGnW4MEpM1wMNc5IMnY */
                        private final /* synthetic */ ArrayList f$1;
                        private final /* synthetic */ long f$2;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        public final void run() {
                            PhaseTwoCallback.this.onPhaseTwoResolved(this.f$1, this.f$2);
                        }
                    });
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
            return bind(token);
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    @GuardedBy({"mLock"})
    private void waitForBindLocked(String token) throws TimeoutException, InterruptedException {
        long startMillis = SystemClock.uptimeMillis();
        while (this.mBindState != 0 && this.mRemoteInstance == null) {
            long remainingMillis = BIND_SERVICE_TIMEOUT_MS - (SystemClock.uptimeMillis() - startMillis);
            if (remainingMillis > 0) {
                this.mLock.wait(remainingMillis);
            } else {
                throw new TimeoutException("[" + token + "] Didn't bind to resolver in time!");
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:36:0x0079, code lost:
        if (r0 == false) goto L_0x00a1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x007d, code lost:
        if (com.android.server.pm.InstantAppResolverConnection.DEBUG_INSTANT == false) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x007f, code lost:
        android.util.Slog.i(com.android.server.pm.InstantAppResolverConnection.TAG, "[" + r13 + "] Previous connection never established; rebinding");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x009a, code lost:
        r12.mContext.unbindService(r12.mServiceConnection);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00a3, code lost:
        if (com.android.server.pm.InstantAppResolverConnection.DEBUG_INSTANT == false) goto L_0x00c0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x00a5, code lost:
        android.util.Slog.v(com.android.server.pm.InstantAppResolverConnection.TAG, "[" + r13 + "] Binding to instant app resolver");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00c0, code lost:
        r7 = r12.mContext.bindServiceAsUser(r12.mIntent, r12.mServiceConnection, 67108865, android.os.UserHandle.SYSTEM);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00d3, code lost:
        if (r7 == false) goto L_0x00f8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x00d5, code lost:
        r4 = r12.mLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00d7, code lost:
        monitor-enter(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:?, code lost:
        waitForBindLocked(r13);
        r7 = r12.mRemoteInstance;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x00de, code lost:
        monitor-exit(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x00df, code lost:
        r7 = r12.mLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x00e1, code lost:
        monitor-enter(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x00e2, code lost:
        if (r7 == false) goto L_0x00e9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x00e4, code lost:
        if (r7 != null) goto L_0x00e9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:57:?, code lost:
        r12.mBindState = 2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x00e9, code lost:
        r12.mBindState = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x00eb, code lost:
        r12.mLock.notifyAll();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:60:0x00f0, code lost:
        monitor-exit(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x00f1, code lost:
        return r7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x00f8, code lost:
        android.util.Slog.w(com.android.server.pm.InstantAppResolverConnection.TAG, "[" + r13 + "] Failed to bind to: " + r12.mIntent);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:0x011d, code lost:
        throw new com.android.server.pm.InstantAppResolverConnection.ConnectionException(1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x011e, code lost:
        r4 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x0123, code lost:
        monitor-enter(r12.mLock);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x0124, code lost:
        if (0 == 0) goto L_0x012b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:?, code lost:
        r12.mBindState = 2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x012b, code lost:
        r12.mBindState = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:0x012d, code lost:
        r12.mLock.notifyAll();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x0133, code lost:
        throw r4;
     */
    private IInstantAppResolver bind(String token) throws ConnectionException, TimeoutException, InterruptedException {
        boolean doUnbind = false;
        synchronized (this.mLock) {
            if (this.mRemoteInstance != null) {
                return this.mRemoteInstance;
            }
            if (this.mBindState == 2) {
                if (DEBUG_INSTANT) {
                    Slog.i(TAG, "[" + token + "] Previous bind timed out; waiting for connection");
                }
                try {
                    waitForBindLocked(token);
                    if (this.mRemoteInstance != null) {
                        return this.mRemoteInstance;
                    }
                } catch (TimeoutException e) {
                    doUnbind = true;
                }
            }
            if (this.mBindState == 1) {
                if (DEBUG_INSTANT) {
                    Slog.i(TAG, "[" + token + "] Another thread is binding; waiting for connection");
                }
                waitForBindLocked(token);
                if (this.mRemoteInstance != null) {
                    return this.mRemoteInstance;
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

    /* access modifiers changed from: package-private */
    public void optimisticBind() {
        this.mBgHandler.post(new Runnable() {
            /* class com.android.server.pm.$$Lambda$InstantAppResolverConnection$DJKXi4qrYjnPQMOwj8UtfZenps */

            public final void run() {
                InstantAppResolverConnection.this.lambda$optimisticBind$0$InstantAppResolverConnection();
            }
        });
    }

    public /* synthetic */ void lambda$optimisticBind$0$InstantAppResolverConnection() {
        try {
            if (bind("Optimistic Bind") != null && DEBUG_INSTANT) {
                Slog.i(TAG, "Optimistic bind succeeded.");
            }
        } catch (ConnectionException | InterruptedException | TimeoutException e) {
            Slog.e(TAG, "Optimistic bind failed.", e);
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

    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public void handleBinderDiedLocked() {
        IInstantAppResolver iInstantAppResolver = this.mRemoteInstance;
        if (iInstantAppResolver != null) {
            try {
                iInstantAppResolver.asBinder().unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
            }
        }
        this.mRemoteInstance = null;
    }

    public static class ConnectionException extends Exception {
        public static final int FAILURE_BIND = 1;
        public static final int FAILURE_CALL = 2;
        public static final int FAILURE_INTERRUPTED = 3;
        public final int failure;

        public ConnectionException(int _failure) {
            this.failure = _failure;
        }
    }

    private final class MyServiceConnection implements ServiceConnection {
        private MyServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (InstantAppResolverConnection.DEBUG_INSTANT) {
                Slog.d(InstantAppResolverConnection.TAG, "Connected to instant app resolver");
            }
            synchronized (InstantAppResolverConnection.this.mLock) {
                IInstantAppResolver unused = InstantAppResolverConnection.this.mRemoteInstance = IInstantAppResolver.Stub.asInterface(service);
                if (InstantAppResolverConnection.this.mBindState == 2) {
                    int unused2 = InstantAppResolverConnection.this.mBindState = 0;
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

    private static final class GetInstantAppResolveInfoCaller extends TimedRemoteCaller<List<InstantAppResolveInfo>> {
        private final IRemoteCallback mCallback = new IRemoteCallback.Stub() {
            /* class com.android.server.pm.InstantAppResolverConnection.GetInstantAppResolveInfoCaller.AnonymousClass1 */

            public void sendResult(Bundle data) throws RemoteException {
                GetInstantAppResolveInfoCaller.this.onRemoteMethodResult(data.getParcelableArrayList("android.app.extra.RESOLVE_INFO"), data.getInt("android.app.extra.SEQUENCE", -1));
            }
        };

        public GetInstantAppResolveInfoCaller() {
            super(InstantAppResolverConnection.CALL_SERVICE_TIMEOUT_MS);
        }

        public List<InstantAppResolveInfo> getInstantAppResolveInfoList(IInstantAppResolver target, Intent sanitizedIntent, int[] hashPrefix, int userId, String token) throws RemoteException, TimeoutException {
            int sequence = onBeforeRemoteCall();
            target.getInstantAppResolveInfoList(sanitizedIntent, hashPrefix, userId, token, sequence, this.mCallback);
            return (List) getResultTimed(sequence);
        }
    }
}
