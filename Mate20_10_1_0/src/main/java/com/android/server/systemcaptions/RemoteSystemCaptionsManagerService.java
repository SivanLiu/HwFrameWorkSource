package com.android.server.systemcaptions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;

final class RemoteSystemCaptionsManagerService {
    private static final String SERVICE_INTERFACE = "android.service.systemcaptions.SystemCaptionsManagerService";
    /* access modifiers changed from: private */
    public static final String TAG = RemoteSystemCaptionsManagerService.class.getSimpleName();
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public boolean mBinding = false;
    private final ComponentName mComponentName;
    private final Context mContext;
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public boolean mDestroyed = false;
    private final Handler mHandler;
    private final Intent mIntent;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    /* access modifiers changed from: private */
    @GuardedBy({"mLock"})
    public IBinder mService;
    private final RemoteServiceConnection mServiceConnection = new RemoteServiceConnection();
    private final int mUserId;
    /* access modifiers changed from: private */
    public final boolean mVerbose;

    RemoteSystemCaptionsManagerService(Context context, ComponentName componentName, int userId, boolean verbose) {
        this.mContext = context;
        this.mComponentName = componentName;
        this.mUserId = userId;
        this.mVerbose = verbose;
        this.mIntent = new Intent(SERVICE_INTERFACE).setComponent(componentName);
        this.mHandler = new Handler(Looper.getMainLooper());
    }

    /* access modifiers changed from: package-private */
    public void initialize() {
        if (this.mVerbose) {
            Slog.v(TAG, "initialize()");
        }
        ensureBound();
    }

    /* access modifiers changed from: package-private */
    public void destroy() {
        if (this.mVerbose) {
            Slog.v(TAG, "destroy()");
        }
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                if (this.mVerbose) {
                    Slog.v(TAG, "destroy(): Already destroyed");
                }
                return;
            }
            this.mDestroyed = true;
            ensureUnboundLocked();
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isDestroyed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDestroyed;
        }
        return z;
    }

    private void ensureBound() {
        synchronized (this.mLock) {
            if (this.mService == null) {
                if (!this.mBinding) {
                    if (this.mVerbose) {
                        Slog.v(TAG, "ensureBound(): binding");
                    }
                    this.mBinding = true;
                    if (!this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, 67108865, this.mHandler, new UserHandle(this.mUserId))) {
                        String str = TAG;
                        Slog.w(str, "Could not bind to " + this.mIntent + " with flags " + 67108865);
                        this.mBinding = false;
                        this.mService = null;
                    }
                }
            }
        }
    }

    @GuardedBy({"mLock"})
    private void ensureUnboundLocked() {
        if (this.mService != null || this.mBinding) {
            this.mBinding = false;
            this.mService = null;
            if (this.mVerbose) {
                Slog.v(TAG, "ensureUnbound(): unbinding");
            }
            this.mContext.unbindService(this.mServiceConnection);
        }
    }

    private class RemoteServiceConnection implements ServiceConnection {
        private RemoteServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (RemoteSystemCaptionsManagerService.this.mLock) {
                if (RemoteSystemCaptionsManagerService.this.mVerbose) {
                    Slog.v(RemoteSystemCaptionsManagerService.TAG, "onServiceConnected()");
                }
                if (!RemoteSystemCaptionsManagerService.this.mDestroyed) {
                    if (RemoteSystemCaptionsManagerService.this.mBinding) {
                        boolean unused = RemoteSystemCaptionsManagerService.this.mBinding = false;
                        IBinder unused2 = RemoteSystemCaptionsManagerService.this.mService = service;
                        return;
                    }
                }
                Slog.wtf(RemoteSystemCaptionsManagerService.TAG, "onServiceConnected() dispatched after unbindService");
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (RemoteSystemCaptionsManagerService.this.mLock) {
                if (RemoteSystemCaptionsManagerService.this.mVerbose) {
                    Slog.v(RemoteSystemCaptionsManagerService.TAG, "onServiceDisconnected()");
                }
                boolean unused = RemoteSystemCaptionsManagerService.this.mBinding = true;
                IBinder unused2 = RemoteSystemCaptionsManagerService.this.mService = null;
            }
        }
    }
}
