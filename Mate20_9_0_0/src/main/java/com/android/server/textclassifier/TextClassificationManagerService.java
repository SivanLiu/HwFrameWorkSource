package com.android.server.textclassifier;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.textclassifier.ITextClassificationCallback;
import android.service.textclassifier.ITextClassifierService;
import android.service.textclassifier.ITextClassifierService.Stub;
import android.service.textclassifier.ITextLinksCallback;
import android.service.textclassifier.ITextSelectionCallback;
import android.service.textclassifier.TextClassifierService;
import android.util.Slog;
import android.util.SparseArray;
import android.view.textclassifier.SelectionEvent;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationContext;
import android.view.textclassifier.TextClassificationSessionId;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection.Request;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;
import com.android.internal.util.Preconditions;
import com.android.server.SystemService;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public final class TextClassificationManagerService extends Stub {
    private static final String LOG_TAG = "TextClassificationManagerService";
    private final Context mContext;
    private final Object mLock;
    @GuardedBy("mLock")
    final SparseArray<UserState> mUserStates;

    private static final class PendingRequest implements DeathRecipient {
        private final IBinder mBinder;
        private final Runnable mOnServiceFailure;
        @GuardedBy("mLock")
        private final UserState mOwningUser;
        private final Runnable mRequest;
        private final TextClassificationManagerService mService;

        PendingRequest(ThrowingRunnable request, ThrowingRunnable onServiceFailure, IBinder binder, TextClassificationManagerService service, UserState owningUser) {
            this.mRequest = TextClassificationManagerService.logOnFailure((ThrowingRunnable) Preconditions.checkNotNull(request), "handling pending request");
            this.mOnServiceFailure = TextClassificationManagerService.logOnFailure(onServiceFailure, "notifying callback of service failure");
            this.mBinder = binder;
            this.mService = service;
            this.mOwningUser = owningUser;
            if (this.mBinder != null) {
                try {
                    this.mBinder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        public void binderDied() {
            synchronized (this.mService.mLock) {
                removeLocked();
            }
        }

        @GuardedBy("mLock")
        private void removeLocked() {
            this.mOwningUser.mPendingRequests.remove(this);
            if (this.mBinder != null) {
                this.mBinder.unlinkToDeath(this, 0);
            }
        }
    }

    private static final class UserState {
        @GuardedBy("mLock")
        boolean mBinding;
        final TextClassifierServiceConnection mConnection;
        private final Context mContext;
        private final Object mLock;
        @GuardedBy("mLock")
        final Queue<PendingRequest> mPendingRequests;
        @GuardedBy("mLock")
        ITextClassifierService mService;
        final int mUserId;

        private final class TextClassifierServiceConnection implements ServiceConnection {
            private TextClassifierServiceConnection() {
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                init(Stub.asInterface(service));
            }

            public void onServiceDisconnected(ComponentName name) {
                cleanupService();
            }

            public void onBindingDied(ComponentName name) {
                cleanupService();
            }

            public void onNullBinding(ComponentName name) {
                cleanupService();
            }

            void cleanupService() {
                init(null);
            }

            private void init(ITextClassifierService service) {
                synchronized (UserState.this.mLock) {
                    UserState.this.mService = service;
                    UserState.this.mBinding = false;
                    UserState.this.handlePendingRequestsLocked();
                }
            }
        }

        private UserState(int userId, Context context, Object lock) {
            this.mConnection = new TextClassifierServiceConnection();
            this.mPendingRequests = new ArrayDeque();
            this.mUserId = userId;
            this.mContext = (Context) Preconditions.checkNotNull(context);
            this.mLock = Preconditions.checkNotNull(lock);
        }

        @GuardedBy("mLock")
        boolean isBoundLocked() {
            return this.mService != null;
        }

        @GuardedBy("mLock")
        private void handlePendingRequestsLocked() {
            while (true) {
                PendingRequest pendingRequest = (PendingRequest) this.mPendingRequests.poll();
                PendingRequest request = pendingRequest;
                if (pendingRequest != null) {
                    if (isBoundLocked()) {
                        request.mRequest.run();
                    } else if (request.mOnServiceFailure != null) {
                        request.mOnServiceFailure.run();
                    }
                    if (request.mBinder != null) {
                        request.mBinder.unlinkToDeath(request, 0);
                    }
                } else {
                    return;
                }
            }
        }

        private boolean bindIfHasPendingRequestsLocked() {
            return !this.mPendingRequests.isEmpty() && bindLocked();
        }

        private boolean bindLocked() {
            if (isBoundLocked() || this.mBinding) {
                return true;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                ComponentName componentName = TextClassifierService.getServiceComponentName(this.mContext);
                boolean z;
                if (componentName == null) {
                    z = false;
                    return z;
                }
                z = new Intent("android.service.textclassifier.TextClassifierService").setComponent(componentName);
                String str = TextClassificationManagerService.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Binding to ");
                stringBuilder.append(z.getComponent());
                Slog.d(str, stringBuilder.toString());
                boolean willBind = this.mContext.bindServiceAsUser(z, this.mConnection, 67108865, UserHandle.of(this.mUserId));
                this.mBinding = willBind;
                Binder.restoreCallingIdentity(identity);
                return willBind;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public static final class Lifecycle extends SystemService {
        private final TextClassificationManagerService mManagerService;

        public Lifecycle(Context context) {
            super(context);
            this.mManagerService = new TextClassificationManagerService(context);
        }

        public void onStart() {
            try {
                publishBinderService("textclassification", this.mManagerService);
            } catch (Throwable t) {
                Slog.e(TextClassificationManagerService.LOG_TAG, "Could not start the TextClassificationManagerService.", t);
            }
        }

        public void onStartUser(int userId) {
            processAnyPendingWork(userId);
        }

        public void onUnlockUser(int userId) {
            processAnyPendingWork(userId);
        }

        private void processAnyPendingWork(int userId) {
            synchronized (this.mManagerService.mLock) {
                this.mManagerService.getUserStateLocked(userId).bindIfHasPendingRequestsLocked();
            }
        }

        public void onStopUser(int userId) {
            synchronized (this.mManagerService.mLock) {
                UserState userState = this.mManagerService.peekUserStateLocked(userId);
                if (userState != null) {
                    userState.mConnection.cleanupService();
                    this.mManagerService.mUserStates.remove(userId);
                }
            }
        }
    }

    private TextClassificationManagerService(Context context) {
        this.mUserStates = new SparseArray();
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mLock = new Object();
    }

    public void onSuggestSelection(TextClassificationSessionId sessionId, Request request, ITextSelectionCallback callback) throws RemoteException {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(callback);
        synchronized (this.mLock) {
            UserState userState = getCallingUserStateLocked();
            if (!userState.bindLocked()) {
                callback.onFailure();
            } else if (userState.isBoundLocked()) {
                userState.mService.onSuggestSelection(sessionId, request, callback);
            } else {
                Queue queue = userState.mPendingRequests;
                -$$Lambda$TextClassificationManagerService$Oay4QGGKO1MM7dDcB0KN_1JmqZA -__lambda_textclassificationmanagerservice_oay4qggko1mm7ddcb0kn_1jmqza = new -$$Lambda$TextClassificationManagerService$Oay4QGGKO1MM7dDcB0KN_1JmqZA(this, sessionId, request, callback);
                Objects.requireNonNull(callback);
                queue.add(new PendingRequest(-__lambda_textclassificationmanagerservice_oay4qggko1mm7ddcb0kn_1jmqza, new -$$Lambda$m88mc8F7odBzfaVb5UMVTJhRQps(callback), callback.asBinder(), this, userState));
            }
        }
    }

    public void onClassifyText(TextClassificationSessionId sessionId, TextClassification.Request request, ITextClassificationCallback callback) throws RemoteException {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(callback);
        synchronized (this.mLock) {
            UserState userState = getCallingUserStateLocked();
            if (!userState.bindLocked()) {
                callback.onFailure();
            } else if (userState.isBoundLocked()) {
                userState.mService.onClassifyText(sessionId, request, callback);
            } else {
                Queue queue = userState.mPendingRequests;
                -$$Lambda$TextClassificationManagerService$0ahBOnx4jsgbPYQhVmIdEMzPn5Q -__lambda_textclassificationmanagerservice_0ahbonx4jsgbpyqhvmidemzpn5q = new -$$Lambda$TextClassificationManagerService$0ahBOnx4jsgbPYQhVmIdEMzPn5Q(this, sessionId, request, callback);
                Objects.requireNonNull(callback);
                queue.add(new PendingRequest(-__lambda_textclassificationmanagerservice_0ahbonx4jsgbpyqhvmidemzpn5q, new -$$Lambda$6tTWS9-rW6CtxVP0xKRcg3Q5kmI(callback), callback.asBinder(), this, userState));
            }
        }
    }

    public void onGenerateLinks(TextClassificationSessionId sessionId, TextLinks.Request request, ITextLinksCallback callback) throws RemoteException {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(callback);
        synchronized (this.mLock) {
            UserState userState = getCallingUserStateLocked();
            if (!userState.bindLocked()) {
                callback.onFailure();
            } else if (userState.isBoundLocked()) {
                userState.mService.onGenerateLinks(sessionId, request, callback);
            } else {
                Queue queue = userState.mPendingRequests;
                -$$Lambda$TextClassificationManagerService$-O5SqJ3O93lhUbxb9PI9hMy-SaM -__lambda_textclassificationmanagerservice_-o5sqj3o93lhubxb9pi9hmy-sam = new -$$Lambda$TextClassificationManagerService$-O5SqJ3O93lhUbxb9PI9hMy-SaM(this, sessionId, request, callback);
                Objects.requireNonNull(callback);
                queue.add(new PendingRequest(-__lambda_textclassificationmanagerservice_-o5sqj3o93lhubxb9pi9hmy-sam, new -$$Lambda$WxMu2h-uKYpQBik6LDmBRWb9Y00(callback), callback.asBinder(), this, userState));
            }
        }
    }

    public void onSelectionEvent(TextClassificationSessionId sessionId, SelectionEvent event) throws RemoteException {
        Preconditions.checkNotNull(event);
        validateInput(event.getPackageName(), this.mContext);
        synchronized (this.mLock) {
            UserState userState = getCallingUserStateLocked();
            if (userState.isBoundLocked()) {
                userState.mService.onSelectionEvent(sessionId, event);
            } else {
                userState.mPendingRequests.add(new PendingRequest(new -$$Lambda$TextClassificationManagerService$Xo8FJ3LmQoamgJ2foxZOcS-n70c(this, sessionId, event), null, null, this, userState));
            }
        }
    }

    public void onCreateTextClassificationSession(TextClassificationContext classificationContext, TextClassificationSessionId sessionId) throws RemoteException {
        Preconditions.checkNotNull(sessionId);
        Preconditions.checkNotNull(classificationContext);
        validateInput(classificationContext.getPackageName(), this.mContext);
        synchronized (this.mLock) {
            UserState userState = getCallingUserStateLocked();
            if (userState.isBoundLocked()) {
                userState.mService.onCreateTextClassificationSession(classificationContext, sessionId);
            } else {
                userState.mPendingRequests.add(new PendingRequest(new -$$Lambda$TextClassificationManagerService$BPyCtyAA7AehDOdMNqubn2TPsH0(this, classificationContext, sessionId), null, null, this, userState));
            }
        }
    }

    public void onDestroyTextClassificationSession(TextClassificationSessionId sessionId) throws RemoteException {
        Preconditions.checkNotNull(sessionId);
        synchronized (this.mLock) {
            UserState userState = getCallingUserStateLocked();
            if (userState.isBoundLocked()) {
                userState.mService.onDestroyTextClassificationSession(sessionId);
            } else {
                userState.mPendingRequests.add(new PendingRequest(new -$$Lambda$TextClassificationManagerService$pc4ryoobAsvmL5rAUjkRjLM3K84(this, sessionId), null, null, this, userState));
            }
        }
    }

    private UserState getCallingUserStateLocked() {
        return getUserStateLocked(UserHandle.getCallingUserId());
    }

    private UserState getUserStateLocked(int userId) {
        UserState result = (UserState) this.mUserStates.get(userId);
        if (result != null) {
            return result;
        }
        result = new UserState(userId, this.mContext, this.mLock);
        this.mUserStates.put(userId, result);
        return result;
    }

    UserState peekUserStateLocked(int userId) {
        return (UserState) this.mUserStates.get(userId);
    }

    private static Runnable logOnFailure(ThrowingRunnable r, String opDesc) {
        if (r == null) {
            return null;
        }
        return FunctionalUtils.handleExceptions(r, new -$$Lambda$TextClassificationManagerService$AlzZLOTDy6ySI7ijsc3zdoY2qPo(opDesc));
    }

    static /* synthetic */ void lambda$logOnFailure$6(String opDesc, Throwable e) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error ");
        stringBuilder.append(opDesc);
        stringBuilder.append(": ");
        stringBuilder.append(e.getMessage());
        Slog.d(str, stringBuilder.toString());
    }

    private static void validateInput(String packageName, Context context) throws RemoteException {
        try {
            boolean z = false;
            if (Binder.getCallingUid() == context.getPackageManager().getPackageUid(packageName, 0)) {
                z = true;
            }
            Preconditions.checkArgument(z);
        } catch (NameNotFoundException | IllegalArgumentException | NullPointerException e) {
            throw new RemoteException(e.getMessage());
        }
    }
}
