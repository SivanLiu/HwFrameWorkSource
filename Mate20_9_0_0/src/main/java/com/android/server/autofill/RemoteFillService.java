package com.android.server.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.ICancellationSignal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.IAutoFillService;
import android.service.autofill.IAutoFillService.Stub;
import android.service.autofill.IFillCallback;
import android.service.autofill.ISaveCallback;
import android.service.autofill.SaveRequest;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.FgThread;
import com.android.server.pm.DumpState;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

final class RemoteFillService implements DeathRecipient {
    private static final String LOG_TAG = "RemoteFillService";
    private static final int MSG_UNBIND = 3;
    private static final long TIMEOUT_IDLE_BIND_MILLIS = 5000;
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 5000;
    private IAutoFillService mAutoFillService;
    private final boolean mBindInstantServiceAllowed;
    private boolean mBinding;
    private final FillServiceCallbacks mCallbacks;
    private boolean mCompleted;
    private final ComponentName mComponentName;
    private final Context mContext;
    private boolean mDestroyed;
    private final Handler mHandler;
    private final Intent mIntent;
    private PendingRequest mPendingRequest;
    private final ServiceConnection mServiceConnection = new RemoteServiceConnection();
    private boolean mServiceDied;
    private final int mUserId;

    public interface FillServiceCallbacks {
        void onFillRequestFailure(int i, CharSequence charSequence, String str);

        void onFillRequestSuccess(int i, FillResponse fillResponse, String str, int i2);

        void onFillRequestTimeout(int i, String str);

        void onSaveRequestFailure(CharSequence charSequence, String str);

        void onSaveRequestSuccess(String str, IntentSender intentSender);

        void onServiceDied(RemoteFillService remoteFillService);
    }

    private static abstract class PendingRequest implements Runnable {
        @GuardedBy("mLock")
        private boolean mCancelled;
        @GuardedBy("mLock")
        private boolean mCompleted;
        protected final Object mLock = new Object();
        private final Handler mServiceHandler;
        private final Runnable mTimeoutTrigger;
        private final WeakReference<RemoteFillService> mWeakService;

        abstract void onTimeout(RemoteFillService remoteFillService);

        PendingRequest(RemoteFillService service) {
            this.mWeakService = new WeakReference(service);
            this.mServiceHandler = service.mHandler;
            this.mTimeoutTrigger = new -$$Lambda$RemoteFillService$PendingRequest$Wzl5nwSdboq2CuUeWvFraQLBZk8(this);
            this.mServiceHandler.postAtTime(this.mTimeoutTrigger, SystemClock.uptimeMillis() + 5000);
        }

        /* JADX WARNING: Missing block: B:9:0x000d, code skipped:
            r0 = com.android.server.autofill.RemoteFillService.LOG_TAG;
            r1 = new java.lang.StringBuilder();
            r1.append(r5.getClass().getSimpleName());
            r1.append(" timed out");
            android.util.Slog.w(r0, r1.toString());
            r0 = (com.android.server.autofill.RemoteFillService) r5.mWeakService.get();
     */
        /* JADX WARNING: Missing block: B:10:0x0033, code skipped:
            if (r0 == null) goto L_0x0060;
     */
        /* JADX WARNING: Missing block: B:11:0x0035, code skipped:
            r1 = com.android.server.autofill.RemoteFillService.LOG_TAG;
            r2 = new java.lang.StringBuilder();
            r2.append(r5.getClass().getSimpleName());
            r2.append(" timed out after ");
            r2.append(5000);
            r2.append(" ms");
            android.util.Slog.w(r1, r2.toString());
            r5.onTimeout(r0);
     */
        /* JADX WARNING: Missing block: B:12:0x0060, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static /* synthetic */ void lambda$new$0(PendingRequest pendingRequest) {
            synchronized (pendingRequest.mLock) {
                if (pendingRequest.mCancelled) {
                    return;
                }
                pendingRequest.mCompleted = true;
            }
        }

        protected RemoteFillService getService() {
            return (RemoteFillService) this.mWeakService.get();
        }

        protected final boolean finish() {
            synchronized (this.mLock) {
                if (!this.mCompleted) {
                    if (!this.mCancelled) {
                        this.mCompleted = true;
                        this.mServiceHandler.removeCallbacks(this.mTimeoutTrigger);
                        return true;
                    }
                }
                return false;
            }
        }

        @GuardedBy("mLock")
        protected boolean isCancelledLocked() {
            return this.mCancelled;
        }

        boolean cancel() {
            synchronized (this.mLock) {
                if (!this.mCancelled) {
                    if (!this.mCompleted) {
                        this.mCancelled = true;
                        this.mServiceHandler.removeCallbacks(this.mTimeoutTrigger);
                        return true;
                    }
                }
                return false;
            }
        }

        boolean isFinal() {
            return false;
        }
    }

    private class RemoteServiceConnection implements ServiceConnection {
        private RemoteServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (RemoteFillService.this.mDestroyed || !RemoteFillService.this.mBinding) {
                Slog.wtf(RemoteFillService.LOG_TAG, "onServiceConnected was dispatched after unbindService.");
                return;
            }
            RemoteFillService.this.mBinding = false;
            RemoteFillService.this.mAutoFillService = Stub.asInterface(service);
            try {
                service.linkToDeath(RemoteFillService.this, 0);
                try {
                    RemoteFillService.this.mAutoFillService.onConnectedStateChanged(true);
                } catch (RemoteException e) {
                    String str = RemoteFillService.LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception calling onConnected(): ");
                    stringBuilder.append(e);
                    Slog.w(str, stringBuilder.toString());
                }
                if (RemoteFillService.this.mPendingRequest != null) {
                    PendingRequest pendingRequest = RemoteFillService.this.mPendingRequest;
                    RemoteFillService.this.mPendingRequest = null;
                    RemoteFillService.this.handlePendingRequest(pendingRequest);
                }
                RemoteFillService.this.mServiceDied = false;
            } catch (RemoteException e2) {
                RemoteFillService.this.handleBinderDied();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            RemoteFillService.this.mBinding = true;
            RemoteFillService.this.mAutoFillService = null;
        }
    }

    private static final class PendingFillRequest extends PendingRequest {
        private final IFillCallback mCallback;
        private ICancellationSignal mCancellation;
        private final FillRequest mRequest;

        public PendingFillRequest(final FillRequest request, RemoteFillService service) {
            super(service);
            this.mRequest = request;
            this.mCallback = new IFillCallback.Stub() {
                public void onCancellable(ICancellationSignal cancellation) {
                    synchronized (PendingFillRequest.this.mLock) {
                        boolean cancelled;
                        synchronized (PendingFillRequest.this.mLock) {
                            PendingFillRequest.this.mCancellation = cancellation;
                            cancelled = PendingFillRequest.this.isCancelledLocked();
                        }
                        if (cancelled) {
                            try {
                                cancellation.cancel();
                            } catch (RemoteException e) {
                                Slog.e(RemoteFillService.LOG_TAG, "Error requesting a cancellation", e);
                            }
                        }
                    }
                }

                public void onSuccess(FillResponse response) {
                    if (PendingFillRequest.this.finish()) {
                        RemoteFillService remoteService = PendingFillRequest.this.getService();
                        if (remoteService != null) {
                            remoteService.dispatchOnFillRequestSuccess(PendingFillRequest.this, response, request.getFlags());
                        }
                    }
                }

                public void onFailure(int requestId, CharSequence message) {
                    if (PendingFillRequest.this.finish()) {
                        RemoteFillService remoteService = PendingFillRequest.this.getService();
                        if (remoteService != null) {
                            remoteService.dispatchOnFillRequestFailure(PendingFillRequest.this, message);
                        }
                    }
                }
            };
        }

        void onTimeout(RemoteFillService remoteService) {
            ICancellationSignal cancellation;
            synchronized (this.mLock) {
                cancellation = this.mCancellation;
            }
            if (cancellation != null) {
                remoteService.dispatchOnFillTimeout(cancellation);
            }
            remoteService.dispatchOnFillRequestTimeout(this);
        }

        /* JADX WARNING: Missing block: B:9:0x0027, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:11:0x0029, code skipped:
            r0 = getService();
     */
        /* JADX WARNING: Missing block: B:12:0x002d, code skipped:
            if (r0 == null) goto L_0x0058;
     */
        /* JADX WARNING: Missing block: B:14:0x0033, code skipped:
            if (com.android.server.autofill.RemoteFillService.access$400(r0) == null) goto L_0x0058;
     */
        /* JADX WARNING: Missing block: B:16:?, code skipped:
            com.android.server.autofill.RemoteFillService.access$400(r0).onFillRequest(r4.mRequest, r4.mCallback);
     */
        /* JADX WARNING: Missing block: B:18:0x0042, code skipped:
            android.util.Slog.e(com.android.server.autofill.RemoteFillService.LOG_TAG, "onFillRequest has Exception : IndexOutOfBoundsException");
     */
        /* JADX WARNING: Missing block: B:19:0x004b, code skipped:
            r1 = move-exception;
     */
        /* JADX WARNING: Missing block: B:20:0x004c, code skipped:
            android.util.Slog.e(com.android.server.autofill.RemoteFillService.LOG_TAG, "Error calling on fill request", r1);
            com.android.server.autofill.RemoteFillService.access$1200(r0, r4, null);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (this.mLock) {
                if (isCancelledLocked()) {
                    if (Helper.sDebug) {
                        String str = RemoteFillService.LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("run() called after canceled: ");
                        stringBuilder.append(this.mRequest);
                        Slog.d(str, stringBuilder.toString());
                    }
                }
            }
        }

        public boolean cancel() {
            if (!super.cancel()) {
                return false;
            }
            ICancellationSignal cancellation;
            synchronized (this.mLock) {
                cancellation = this.mCancellation;
            }
            if (cancellation != null) {
                try {
                    cancellation.cancel();
                } catch (RemoteException e) {
                    Slog.e(RemoteFillService.LOG_TAG, "Error cancelling a fill request", e);
                }
            }
            return true;
        }
    }

    private static final class PendingSaveRequest extends PendingRequest {
        private final ISaveCallback mCallback = new ISaveCallback.Stub() {
            public void onSuccess(IntentSender intentSender) {
                if (PendingSaveRequest.this.finish()) {
                    RemoteFillService remoteService = PendingSaveRequest.this.getService();
                    if (remoteService != null) {
                        remoteService.dispatchOnSaveRequestSuccess(PendingSaveRequest.this, intentSender);
                    }
                }
            }

            public void onFailure(CharSequence message) {
                if (PendingSaveRequest.this.finish()) {
                    RemoteFillService remoteService = PendingSaveRequest.this.getService();
                    if (remoteService != null) {
                        remoteService.dispatchOnSaveRequestFailure(PendingSaveRequest.this, message);
                    }
                }
            }
        };
        private final SaveRequest mRequest;

        public PendingSaveRequest(SaveRequest request, RemoteFillService service) {
            super(service);
            this.mRequest = request;
        }

        void onTimeout(RemoteFillService remoteService) {
            remoteService.dispatchOnSaveRequestFailure(this, null);
        }

        public void run() {
            RemoteFillService remoteService = getService();
            if (remoteService != null) {
                try {
                    remoteService.mAutoFillService.onSaveRequest(this.mRequest, this.mCallback);
                } catch (RemoteException e) {
                    Slog.e(RemoteFillService.LOG_TAG, "Error calling on save request", e);
                    remoteService.dispatchOnSaveRequestFailure(this, null);
                }
            }
        }

        public boolean isFinal() {
            return true;
        }
    }

    public RemoteFillService(Context context, ComponentName componentName, int userId, FillServiceCallbacks callbacks, boolean bindInstantServiceAllowed) {
        this.mContext = context;
        this.mCallbacks = callbacks;
        this.mComponentName = componentName;
        this.mIntent = new Intent("android.service.autofill.AutofillService").setComponent(this.mComponentName);
        this.mUserId = userId;
        this.mHandler = new Handler(FgThread.getHandler().getLooper());
        this.mBindInstantServiceAllowed = bindInstantServiceAllowed;
    }

    public void destroy() {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemoteFillService$KN9CcjjmJTg_PJcamzzLgVvQt9M.INSTANCE, this));
    }

    private void handleDestroy() {
        if (!checkIfDestroyed()) {
            if (this.mPendingRequest != null) {
                this.mPendingRequest.cancel();
                this.mPendingRequest = null;
            }
            ensureUnbound();
            this.mDestroyed = true;
        }
    }

    public void binderDied() {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemoteFillService$1sGSxm1GNkRnOTqlIJFPKrlV6Bk.INSTANCE, this));
    }

    private void handleBinderDied() {
        if (!checkIfDestroyed()) {
            if (this.mAutoFillService != null) {
                this.mAutoFillService.asBinder().unlinkToDeath(this, 0);
            }
            this.mAutoFillService = null;
            this.mServiceDied = true;
            this.mCallbacks.onServiceDied(this);
        }
    }

    public int cancelCurrentRequest() {
        if (this.mDestroyed) {
            return Integer.MIN_VALUE;
        }
        int requestId = Integer.MIN_VALUE;
        if (this.mPendingRequest != null) {
            if (this.mPendingRequest instanceof PendingFillRequest) {
                requestId = ((PendingFillRequest) this.mPendingRequest).mRequest.getId();
            }
            try {
                this.mPendingRequest.cancel();
            } catch (NullPointerException e) {
                Slog.e(LOG_TAG, "Error calling cancle", e);
            }
            this.mPendingRequest = null;
        }
        return requestId;
    }

    public void onFillRequest(FillRequest request) {
        cancelScheduledUnbind();
        scheduleRequest(new PendingFillRequest(request, this));
    }

    public void onSaveRequest(SaveRequest request) {
        cancelScheduledUnbind();
        scheduleRequest(new PendingSaveRequest(request, this));
    }

    private void scheduleRequest(PendingRequest pendingRequest) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemoteFillService$h6FPsdmILphrDZs953cJIyumyqg.INSTANCE, this, pendingRequest));
    }

    public void dump(String prefix, PrintWriter pw) {
        String tab = "  ";
        pw.append(prefix).append("service:").println();
        pw.append(prefix).append(tab).append("userId=").append(String.valueOf(this.mUserId)).println();
        pw.append(prefix).append(tab).append("componentName=").append(this.mComponentName.flattenToString()).println();
        pw.append(prefix).append(tab).append("destroyed=").append(String.valueOf(this.mDestroyed)).println();
        pw.append(prefix).append(tab).append("bound=").append(String.valueOf(isBound())).println();
        pw.append(prefix).append(tab).append("hasPendingRequest=").append(String.valueOf(this.mPendingRequest != null)).println();
        pw.append(prefix).append("mBindInstantServiceAllowed=").println(this.mBindInstantServiceAllowed);
        pw.println();
    }

    private void cancelScheduledUnbind() {
        this.mHandler.removeMessages(3);
    }

    private void scheduleUnbind() {
        cancelScheduledUnbind();
        this.mHandler.sendMessageDelayed(PooledLambda.obtainMessage(-$$Lambda$RemoteFillService$YjPsINV7QuCehWwsB0GTTg1hvr4.INSTANCE, this).setWhat(3), 5000);
    }

    private void handleUnbind() {
        if (!checkIfDestroyed()) {
            ensureUnbound();
        }
    }

    private void handlePendingRequest(PendingRequest pendingRequest) {
        if (!checkIfDestroyed() && !this.mCompleted) {
            if (isBound()) {
                if (Helper.sVerbose) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[user: ");
                    stringBuilder.append(this.mUserId);
                    stringBuilder.append("] handlePendingRequest()");
                    Slog.v(str, stringBuilder.toString());
                }
                if (pendingRequest != null) {
                    pendingRequest.run();
                }
                if (pendingRequest.isFinal()) {
                    this.mCompleted = true;
                }
            } else {
                if (this.mPendingRequest != null) {
                    this.mPendingRequest.cancel();
                }
                this.mPendingRequest = pendingRequest;
                ensureBound();
            }
        }
    }

    private boolean isBound() {
        return this.mAutoFillService != null;
    }

    private void ensureBound() {
        if (!isBound() && !this.mBinding) {
            if (Helper.sVerbose) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserId);
                stringBuilder.append("] ensureBound()");
                Slog.v(str, stringBuilder.toString());
            }
            this.mBinding = true;
            int flags = 67108865;
            if (this.mBindInstantServiceAllowed) {
                flags = 67108865 | DumpState.DUMP_CHANGES;
            }
            if (!this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, flags, new UserHandle(this.mUserId))) {
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[user: ");
                stringBuilder2.append(this.mUserId);
                stringBuilder2.append("] could not bind to ");
                stringBuilder2.append(this.mIntent);
                stringBuilder2.append(" using flags ");
                stringBuilder2.append(flags);
                Slog.w(str2, stringBuilder2.toString());
                this.mBinding = false;
                if (!this.mServiceDied) {
                    handleBinderDied();
                }
            }
        }
    }

    private void ensureUnbound() {
        if (isBound() || this.mBinding) {
            if (Helper.sVerbose) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[user: ");
                stringBuilder.append(this.mUserId);
                stringBuilder.append("] ensureUnbound()");
                Slog.v(str, stringBuilder.toString());
            }
            this.mBinding = false;
            if (isBound()) {
                try {
                    this.mAutoFillService.onConnectedStateChanged(false);
                } catch (Exception e) {
                    String str2 = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Exception calling onDisconnected(): ");
                    stringBuilder2.append(e);
                    Slog.w(str2, stringBuilder2.toString());
                }
                if (this.mAutoFillService != null) {
                    this.mAutoFillService.asBinder().unlinkToDeath(this, 0);
                    this.mAutoFillService = null;
                }
            }
            try {
                this.mContext.unbindService(this.mServiceConnection);
            } catch (IllegalArgumentException e2) {
                String str3 = LOG_TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Service not registered: ");
                stringBuilder3.append(e2);
                Slog.e(str3, stringBuilder3.toString());
            }
        }
    }

    private void dispatchOnFillRequestSuccess(PendingFillRequest pendingRequest, FillResponse response, int requestFlags) {
        this.mHandler.post(new -$$Lambda$RemoteFillService$_5v43Gwb-Yar1uuVIqDgfleCP_4(this, pendingRequest, response, requestFlags));
    }

    public static /* synthetic */ void lambda$dispatchOnFillRequestSuccess$0(RemoteFillService remoteFillService, PendingFillRequest pendingRequest, FillResponse response, int requestFlags) {
        if (remoteFillService.handleResponseCallbackCommon(pendingRequest)) {
            remoteFillService.mCallbacks.onFillRequestSuccess(pendingRequest.mRequest.getId(), response, remoteFillService.mComponentName.getPackageName(), requestFlags);
        }
    }

    private void dispatchOnFillRequestFailure(PendingFillRequest pendingRequest, CharSequence message) {
        this.mHandler.post(new -$$Lambda$RemoteFillService$17MhbU6HKTSEi1dUKhwTRwYg2xA(this, pendingRequest, message));
    }

    public static /* synthetic */ void lambda$dispatchOnFillRequestFailure$1(RemoteFillService remoteFillService, PendingFillRequest pendingRequest, CharSequence message) {
        if (remoteFillService.handleResponseCallbackCommon(pendingRequest)) {
            remoteFillService.mCallbacks.onFillRequestFailure(pendingRequest.mRequest.getId(), message, remoteFillService.mComponentName.getPackageName());
        }
    }

    private void dispatchOnFillRequestTimeout(PendingFillRequest pendingRequest) {
        this.mHandler.post(new -$$Lambda$RemoteFillService$PKEfnjx72TG33VenAsL_32TGLPg(this, pendingRequest));
    }

    public static /* synthetic */ void lambda$dispatchOnFillRequestTimeout$2(RemoteFillService remoteFillService, PendingFillRequest pendingRequest) {
        if (remoteFillService.handleResponseCallbackCommon(pendingRequest)) {
            remoteFillService.mCallbacks.onFillRequestTimeout(pendingRequest.mRequest.getId(), remoteFillService.mComponentName.getPackageName());
        }
    }

    private void dispatchOnFillTimeout(ICancellationSignal cancellationSignal) {
        this.mHandler.post(new -$$Lambda$RemoteFillService$lfx4anCMpwM99MhvsITDjU9sFRA(cancellationSignal));
    }

    static /* synthetic */ void lambda$dispatchOnFillTimeout$3(ICancellationSignal cancellationSignal) {
        try {
            cancellationSignal.cancel();
        } catch (RemoteException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error calling cancellation signal: ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
        }
    }

    private void dispatchOnSaveRequestSuccess(PendingRequest pendingRequest, IntentSender intentSender) {
        this.mHandler.post(new -$$Lambda$RemoteFillService$XMU-2wAMieOoEHWM96VKmbAYfUo(this, pendingRequest, intentSender));
    }

    public static /* synthetic */ void lambda$dispatchOnSaveRequestSuccess$4(RemoteFillService remoteFillService, PendingRequest pendingRequest, IntentSender intentSender) {
        if (remoteFillService.handleResponseCallbackCommon(pendingRequest)) {
            remoteFillService.mCallbacks.onSaveRequestSuccess(remoteFillService.mComponentName.getPackageName(), intentSender);
        }
    }

    private void dispatchOnSaveRequestFailure(PendingRequest pendingRequest, CharSequence message) {
        this.mHandler.post(new -$$Lambda$RemoteFillService$-MTWVawYUlWYzdF5tucVgNj4nNY(this, pendingRequest, message));
    }

    public static /* synthetic */ void lambda$dispatchOnSaveRequestFailure$5(RemoteFillService remoteFillService, PendingRequest pendingRequest, CharSequence message) {
        if (remoteFillService.handleResponseCallbackCommon(pendingRequest)) {
            remoteFillService.mCallbacks.onSaveRequestFailure(message, remoteFillService.mComponentName.getPackageName());
        }
    }

    private boolean handleResponseCallbackCommon(PendingRequest pendingRequest) {
        if (this.mDestroyed) {
            return false;
        }
        if (this.mPendingRequest == pendingRequest) {
            this.mPendingRequest = null;
        }
        if (this.mPendingRequest == null) {
            scheduleUnbind();
        }
        return true;
    }

    private boolean checkIfDestroyed() {
        if (this.mDestroyed && Helper.sVerbose) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not handling operation as service for ");
            stringBuilder.append(this.mComponentName);
            stringBuilder.append(" is already destroyed");
            Slog.v(str, stringBuilder.toString());
        }
        return this.mDestroyed;
    }
}
