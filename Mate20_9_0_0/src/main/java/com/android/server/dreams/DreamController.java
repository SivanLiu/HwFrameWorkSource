package com.android.server.dreams;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IRemoteCallback;
import android.os.IRemoteCallback.Stub;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.service.dreams.IDreamService;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.logging.MetricsLogger;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.policy.PhoneWindowManager;
import java.io.PrintWriter;
import java.util.NoSuchElementException;

final class DreamController {
    private static final int DREAM_CONNECTION_TIMEOUT = 5000;
    private static final int DREAM_FINISH_TIMEOUT = 5000;
    private static final String TAG = "DreamController";
    private final Intent mCloseNotificationShadeIntent;
    private final Context mContext;
    DreamRecord mCurrentDream;
    private HwCustDreamController mCust = null;
    private long mDreamStartTime;
    private final Intent mDreamingStartedIntent = new Intent("android.intent.action.DREAMING_STARTED").addFlags(1073741824);
    private final Intent mDreamingStoppedIntent = new Intent("android.intent.action.DREAMING_STOPPED").addFlags(1073741824);
    private final Handler mHandler;
    private final IWindowManager mIWindowManager;
    private final Listener mListener;
    private final Runnable mStopStubbornDreamRunnable = new Runnable() {
        public void run() {
            Slog.w(DreamController.TAG, "Stubborn dream did not finish itself in the time allotted");
            DreamController.this.stopDream(true);
        }
    };
    final Runnable mStopUnconnectedDreamRunnable = new Runnable() {
        public void run() {
            if (DreamController.this.mCurrentDream != null && DreamController.this.mCurrentDream.mBound && !DreamController.this.mCurrentDream.mConnected) {
                Slog.w(DreamController.TAG, "Bound dream did not connect in the time allotted");
                DreamController.this.stopDream(true);
            }
        }
    };

    final class DreamRecord implements DeathRecipient, ServiceConnection {
        public boolean mBound;
        public final boolean mCanDoze;
        public boolean mConnected;
        final IRemoteCallback mDreamingStartedCallback = new Stub() {
            public void sendResult(Bundle data) throws RemoteException {
                DreamController.this.mHandler.post(DreamRecord.this.mReleaseWakeLockIfNeeded);
            }
        };
        public final boolean mIsTest;
        public final ComponentName mName;
        final Runnable mReleaseWakeLockIfNeeded = new -$$Lambda$gXC4nM2f5GMCBX0ED45DCQQjqv0(this);
        public boolean mSentStartBroadcast;
        public IDreamService mService;
        public final Binder mToken;
        public final int mUserId;
        public WakeLock mWakeLock;
        public boolean mWakingGently;

        public DreamRecord(Binder token, ComponentName name, boolean isTest, boolean canDoze, int userId, WakeLock wakeLock) {
            this.mToken = token;
            this.mName = name;
            this.mIsTest = isTest;
            this.mCanDoze = canDoze;
            this.mUserId = userId;
            this.mWakeLock = wakeLock;
            this.mWakeLock.acquire();
            DreamController.this.mHandler.postDelayed(this.mReleaseWakeLockIfNeeded, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }

        public void binderDied() {
            DreamController.this.mHandler.post(new Runnable() {
                public void run() {
                    DreamRecord.this.mService = null;
                    if (DreamController.this.mCurrentDream == DreamRecord.this) {
                        DreamController.this.stopDream(true);
                    }
                }
            });
        }

        public void onServiceConnected(ComponentName name, final IBinder service) {
            DreamController.this.mHandler.post(new Runnable() {
                public void run() {
                    DreamRecord.this.mConnected = true;
                    if (DreamController.this.mCurrentDream == DreamRecord.this && DreamRecord.this.mService == null) {
                        DreamController.this.attach(IDreamService.Stub.asInterface(service));
                    } else {
                        DreamRecord.this.releaseWakeLockIfNeeded();
                    }
                }
            });
        }

        public void onServiceDisconnected(ComponentName name) {
            DreamController.this.mHandler.post(new Runnable() {
                public void run() {
                    DreamRecord.this.mService = null;
                    if (DreamController.this.mCurrentDream == DreamRecord.this) {
                        DreamController.this.stopDream(true);
                    }
                }
            });
        }

        void releaseWakeLockIfNeeded() {
            if (this.mWakeLock != null) {
                this.mWakeLock.release();
                this.mWakeLock = null;
                DreamController.this.mHandler.removeCallbacks(this.mReleaseWakeLockIfNeeded);
            }
        }
    }

    public interface Listener {
        void onDreamStopped(Binder binder);
    }

    public HwCustDreamController getCust() {
        return this.mCust;
    }

    public DreamController(Context context, Handler handler, Listener listener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        this.mIWindowManager = WindowManagerGlobal.getWindowManagerService();
        this.mCloseNotificationShadeIntent = new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        this.mCloseNotificationShadeIntent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, "dream");
    }

    public void dump(PrintWriter pw) {
        pw.println("Dreamland:");
        if (this.mCurrentDream != null) {
            pw.println("  mCurrentDream:");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    mToken=");
            stringBuilder.append(this.mCurrentDream.mToken);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mName=");
            stringBuilder.append(this.mCurrentDream.mName);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mIsTest=");
            stringBuilder.append(this.mCurrentDream.mIsTest);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mCanDoze=");
            stringBuilder.append(this.mCurrentDream.mCanDoze);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mUserId=");
            stringBuilder.append(this.mCurrentDream.mUserId);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mBound=");
            stringBuilder.append(this.mCurrentDream.mBound);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mService=");
            stringBuilder.append(this.mCurrentDream.mService);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mSentStartBroadcast=");
            stringBuilder.append(this.mCurrentDream.mSentStartBroadcast);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mWakingGently=");
            stringBuilder.append(this.mCurrentDream.mWakingGently);
            pw.println(stringBuilder.toString());
            return;
        }
        pw.println("  mCurrentDream: null");
    }

    public void startDream(Binder token, ComponentName name, boolean isTest, boolean canDoze, int userId, WakeLock wakeLock) {
        RemoteException ex;
        Binder binder;
        Throwable th;
        ComponentName componentName = name;
        int i = userId;
        stopDream(true);
        Trace.traceBegin(131072, "startDream");
        boolean z;
        try {
            this.mContext.sendBroadcastAsUser(this.mCloseNotificationShadeIntent, UserHandle.ALL);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Starting dream: name=");
            stringBuilder.append(componentName);
            stringBuilder.append(", isTest=");
            z = isTest;
            try {
                stringBuilder.append(z);
                stringBuilder.append(", canDoze=");
                boolean z2 = canDoze;
                stringBuilder.append(z2);
                stringBuilder.append(", userId=");
                stringBuilder.append(i);
                Slog.i(str, stringBuilder.toString());
                this.mCurrentDream = new DreamRecord(token, componentName, z, z2, i, wakeLock);
                this.mDreamStartTime = SystemClock.elapsedRealtime();
                MetricsLogger.visible(this.mContext, this.mCurrentDream.mCanDoze ? NetdResponseCode.ClatdStatusResult : NetdResponseCode.DnsProxyQueryResult);
                try {
                    try {
                        this.mIWindowManager.addWindowToken(token, 2023, 0);
                    } catch (RemoteException e) {
                        ex = e;
                    }
                } catch (RemoteException e2) {
                    ex = e2;
                    binder = token;
                    Slog.e(TAG, "Unable to add window token for dream.", ex);
                    stopDream(true);
                    Trace.traceEnd(131072);
                    return;
                }
            } catch (Throwable th2) {
                th = th2;
                binder = token;
                Trace.traceEnd(131072);
                throw th;
            }
            Intent intent;
            try {
                intent = new Intent("android.service.dreams.DreamService");
                intent.setComponent(componentName);
                intent.addFlags(DumpState.DUMP_VOLUMES);
                if (this.mContext.bindServiceAsUser(intent, this.mCurrentDream, 67108865, new UserHandle(i))) {
                    this.mCurrentDream.mBound = true;
                    this.mHandler.postDelayed(this.mStopUnconnectedDreamRunnable, 5000);
                    Trace.traceEnd(131072);
                    return;
                }
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to bind dream service: ");
                stringBuilder2.append(intent);
                Slog.e(str, stringBuilder2.toString());
                stopDream(true);
                Trace.traceEnd(131072);
            } catch (SecurityException ex2) {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unable to bind dream service: ");
                stringBuilder3.append(intent);
                Slog.e(str2, stringBuilder3.toString(), ex2);
                stopDream(true);
                Trace.traceEnd(131072);
            } catch (Throwable th3) {
                th = th3;
                Trace.traceEnd(131072);
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            binder = token;
            z = isTest;
            Trace.traceEnd(131072);
            throw th;
        }
    }

    public void stopDream(boolean immediate) {
        if (this.mCurrentDream != null) {
            final DreamRecord oldDream;
            Trace.traceBegin(131072, "stopDream");
            if (!immediate) {
                if (this.mCurrentDream.mWakingGently) {
                    Trace.traceEnd(131072);
                    return;
                } else if (this.mCurrentDream.mService != null) {
                    this.mCurrentDream.mWakingGently = true;
                    try {
                        this.mCurrentDream.mService.wakeUp();
                        this.mHandler.postDelayed(this.mStopStubbornDreamRunnable, 5000);
                        Trace.traceEnd(131072);
                        return;
                    } catch (RemoteException e) {
                    }
                }
            }
            try {
                oldDream = this.mCurrentDream;
                this.mCurrentDream = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Stopping dream: name=");
                stringBuilder.append(oldDream.mName);
                stringBuilder.append(", isTest=");
                stringBuilder.append(oldDream.mIsTest);
                stringBuilder.append(", canDoze=");
                stringBuilder.append(oldDream.mCanDoze);
                stringBuilder.append(", userId=");
                stringBuilder.append(oldDream.mUserId);
                Slog.i(str, stringBuilder.toString());
                MetricsLogger.hidden(this.mContext, oldDream.mCanDoze ? NetdResponseCode.ClatdStatusResult : NetdResponseCode.DnsProxyQueryResult);
                MetricsLogger.histogram(this.mContext, oldDream.mCanDoze ? "dozing_minutes" : "dreaming_minutes", (int) ((SystemClock.elapsedRealtime() - this.mDreamStartTime) / 60000));
                this.mHandler.removeCallbacks(this.mStopUnconnectedDreamRunnable);
                this.mHandler.removeCallbacks(this.mStopStubbornDreamRunnable);
                if (oldDream.mSentStartBroadcast) {
                    this.mContext.sendBroadcastAsUser(this.mDreamingStoppedIntent, UserHandle.ALL);
                }
                if (oldDream.mService != null) {
                    try {
                        oldDream.mService.detach();
                    } catch (RemoteException e2) {
                    }
                    try {
                        oldDream.mService.asBinder().unlinkToDeath(oldDream, 0);
                    } catch (NoSuchElementException e3) {
                    }
                    oldDream.mService = null;
                }
                if (oldDream.mBound) {
                    this.mContext.unbindService(oldDream);
                }
                oldDream.releaseWakeLockIfNeeded();
                this.mIWindowManager.removeWindowToken(oldDream.mToken, 0);
            } catch (RemoteException ex) {
                Slog.w(TAG, "Error removing window token for dream.", ex);
            } catch (Throwable th) {
                Trace.traceEnd(131072);
            }
            this.mHandler.post(new Runnable() {
                public void run() {
                    DreamController.this.mListener.onDreamStopped(oldDream.mToken);
                }
            });
            Trace.traceEnd(131072);
        }
    }

    private void attach(IDreamService service) {
        try {
            service.asBinder().linkToDeath(this.mCurrentDream, 0);
            service.attach(this.mCurrentDream.mToken, this.mCurrentDream.mCanDoze, this.mCurrentDream.mDreamingStartedCallback);
            this.mCurrentDream.mService = service;
            if (!this.mCurrentDream.mIsTest) {
                this.mContext.sendBroadcastAsUser(this.mDreamingStartedIntent, UserHandle.ALL);
                this.mCurrentDream.mSentStartBroadcast = true;
            }
        } catch (RemoteException ex) {
            Slog.e(TAG, "The dream service died unexpectedly.", ex);
            stopDream(true);
        }
    }
}
