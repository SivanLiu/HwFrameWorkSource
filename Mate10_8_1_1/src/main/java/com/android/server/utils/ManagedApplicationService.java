package com.android.server.utils;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ManagedApplicationService {
    private static final int MAX_RETRY_COUNT = 4;
    private static final long MAX_RETRY_DURATION_MS = 16000;
    private static final long MIN_RETRY_DURATION_MS = 2000;
    public static final int RETRY_BEST_EFFORT = 3;
    public static final int RETRY_FOREVER = 1;
    public static final int RETRY_NEVER = 2;
    private static final long RETRY_RESET_TIME_MS = 64000;
    private final String TAG = getClass().getSimpleName();
    private IInterface mBoundInterface;
    private final BinderChecker mChecker;
    private final int mClientLabel;
    private final ComponentName mComponent;
    private ServiceConnection mConnection;
    private final Context mContext;
    private final EventCallback mEventCb;
    private final Handler mHandler;
    private final boolean mIsImportant;
    private long mLastRetryTimeMs;
    private final Object mLock = new Object();
    private long mNextRetryDurationMs = MIN_RETRY_DURATION_MS;
    private PendingEvent mPendingEvent;
    private int mRetryCount;
    private final Runnable mRetryRunnable = new -$Lambda$luWxpSWBY1-S73qs-S0xFqWHvIs(this);
    private final int mRetryType;
    private boolean mRetrying;
    private final String mSettingsAction;
    private final int mUserId;

    public interface BinderChecker {
        IInterface asInterface(IBinder iBinder);

        boolean checkType(IInterface iInterface);
    }

    public interface EventCallback {
        void onServiceEvent(LogEvent logEvent);
    }

    public interface LogFormattable {
        String toLogString(SimpleDateFormat simpleDateFormat);
    }

    public static class LogEvent implements LogFormattable {
        public static final int EVENT_BINDING_DIED = 3;
        public static final int EVENT_CONNECTED = 1;
        public static final int EVENT_DISCONNECTED = 2;
        public static final int EVENT_STOPPED_PERMANENTLY = 4;
        public final ComponentName component;
        public final int event;
        public final long timestamp;

        public LogEvent(long timestamp, ComponentName component, int event) {
            this.timestamp = timestamp;
            this.component = component;
            this.event = event;
        }

        public String toLogString(SimpleDateFormat dateFormat) {
            return dateFormat.format(new Date(this.timestamp)) + "   " + eventToString(this.event) + " Managed Service: " + (this.component == null ? "None" : this.component.flattenToString());
        }

        public static String eventToString(int event) {
            switch (event) {
                case 1:
                    return "Connected";
                case 2:
                    return "Disconnected";
                case 3:
                    return "Binding Died For";
                case 4:
                    return "Permanently Stopped";
                default:
                    return "Unknown Event Occurred";
            }
        }
    }

    public interface PendingEvent {
        void runEvent(IInterface iInterface) throws RemoteException;
    }

    /* synthetic */ void -com_android_server_utils_ManagedApplicationService-mthref-0() {
        doRetry();
    }

    private ManagedApplicationService(Context context, ComponentName component, int userId, int clientLabel, String settingsAction, BinderChecker binderChecker, boolean isImportant, int retryType, Handler handler, EventCallback eventCallback) {
        this.mContext = context;
        this.mComponent = component;
        this.mUserId = userId;
        this.mClientLabel = clientLabel;
        this.mSettingsAction = settingsAction;
        this.mChecker = binderChecker;
        this.mIsImportant = isImportant;
        this.mRetryType = retryType;
        this.mHandler = handler;
        this.mEventCb = eventCallback;
    }

    public static ManagedApplicationService build(Context context, ComponentName component, int userId, int clientLabel, String settingsAction, BinderChecker binderChecker, boolean isImportant, int retryType, Handler handler, EventCallback eventCallback) {
        return new ManagedApplicationService(context, component, userId, clientLabel, settingsAction, binderChecker, isImportant, retryType, handler, eventCallback);
    }

    public int getUserId() {
        return this.mUserId;
    }

    public ComponentName getComponent() {
        return this.mComponent;
    }

    public boolean disconnectIfNotMatching(ComponentName componentName, int userId) {
        if (matches(componentName, userId)) {
            return false;
        }
        disconnect();
        return true;
    }

    public void sendEvent(PendingEvent event) {
        synchronized (this.mLock) {
            IInterface iface = this.mBoundInterface;
            if (iface == null) {
                this.mPendingEvent = event;
            }
        }
        if (iface != null) {
            try {
                event.runEvent(iface);
            } catch (Exception ex) {
                Slog.e(this.TAG, "Received exception from user service: ", ex);
            }
        }
    }

    public void disconnect() {
        synchronized (this.mLock) {
            if (this.mConnection == null) {
                return;
            }
            this.mContext.unbindService(this.mConnection);
            this.mConnection = null;
            this.mBoundInterface = null;
        }
    }

    public void connect() {
        synchronized (this.mLock) {
            if (this.mConnection != null) {
                return;
            }
            Intent intent = new Intent().setComponent(this.mComponent);
            if (this.mClientLabel != 0) {
                intent.putExtra("android.intent.extra.client_label", this.mClientLabel);
            }
            if (this.mSettingsAction != null) {
                intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(this.mContext, 0, new Intent(this.mSettingsAction), 0));
            }
            this.mConnection = new ServiceConnection() {
                public void onBindingDied(ComponentName componentName) {
                    long timestamp = System.currentTimeMillis();
                    Slog.w(ManagedApplicationService.this.TAG, "Service binding died: " + componentName);
                    synchronized (ManagedApplicationService.this.mLock) {
                        if (ManagedApplicationService.this.mConnection != this) {
                            return;
                        }
                        ManagedApplicationService.this.mHandler.post(new com.android.server.utils.-$Lambda$luWxpSWBY1-S73qs-S0xFqWHvIs.AnonymousClass1((byte) 0, timestamp, this));
                        ManagedApplicationService.this.mBoundInterface = null;
                        ManagedApplicationService.this.startRetriesLocked();
                    }
                }

                /* synthetic */ void lambda$-com_android_server_utils_ManagedApplicationService$1_11806(long timestamp) {
                    ManagedApplicationService.this.mEventCb.onServiceEvent(new LogEvent(timestamp, ManagedApplicationService.this.mComponent, 3));
                }

                /* JADX WARNING: inconsistent code. */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    long timestamp = System.currentTimeMillis();
                    Slog.i(ManagedApplicationService.this.TAG, "Service connected: " + componentName);
                    IInterface iInterface = null;
                    PendingEvent pendingEvent = null;
                    synchronized (ManagedApplicationService.this.mLock) {
                        if (ManagedApplicationService.this.mConnection != this) {
                            return;
                        }
                        ManagedApplicationService.this.mHandler.post(new com.android.server.utils.-$Lambda$luWxpSWBY1-S73qs-S0xFqWHvIs.AnonymousClass1((byte) 1, timestamp, this));
                        ManagedApplicationService.this.stopRetriesLocked();
                        ManagedApplicationService.this.mBoundInterface = null;
                        if (ManagedApplicationService.this.mChecker != null) {
                            ManagedApplicationService.this.mBoundInterface = ManagedApplicationService.this.mChecker.asInterface(iBinder);
                            if (ManagedApplicationService.this.mChecker.checkType(ManagedApplicationService.this.mBoundInterface)) {
                                iInterface = ManagedApplicationService.this.mBoundInterface;
                                pendingEvent = ManagedApplicationService.this.mPendingEvent;
                                ManagedApplicationService.this.mPendingEvent = null;
                            } else {
                                ManagedApplicationService.this.mBoundInterface = null;
                                Slog.w(ManagedApplicationService.this.TAG, "Invalid binder from " + componentName);
                                ManagedApplicationService.this.startRetriesLocked();
                            }
                        }
                    }
                }

                /* synthetic */ void lambda$-com_android_server_utils_ManagedApplicationService$1_12741(long timestamp) {
                    ManagedApplicationService.this.mEventCb.onServiceEvent(new LogEvent(timestamp, ManagedApplicationService.this.mComponent, 1));
                }

                public void onServiceDisconnected(ComponentName componentName) {
                    long timestamp = System.currentTimeMillis();
                    Slog.w(ManagedApplicationService.this.TAG, "Service disconnected: " + componentName);
                    synchronized (ManagedApplicationService.this.mLock) {
                        if (ManagedApplicationService.this.mConnection != this) {
                            return;
                        }
                        ManagedApplicationService.this.mHandler.post(new com.android.server.utils.-$Lambda$luWxpSWBY1-S73qs-S0xFqWHvIs.AnonymousClass1((byte) 2, timestamp, this));
                        ManagedApplicationService.this.mBoundInterface = null;
                        ManagedApplicationService.this.startRetriesLocked();
                    }
                }

                /* synthetic */ void lambda$-com_android_server_utils_ManagedApplicationService$1_14647(long timestamp) {
                    ManagedApplicationService.this.mEventCb.onServiceEvent(new LogEvent(timestamp, ManagedApplicationService.this.mComponent, 2));
                }
            };
            int flags = 67108865;
            if (this.mIsImportant) {
                flags = 67108929;
            }
            try {
                if (!this.mContext.bindServiceAsUser(intent, this.mConnection, flags, new UserHandle(this.mUserId))) {
                    Slog.w(this.TAG, "Unable to bind service: " + intent);
                    startRetriesLocked();
                }
            } catch (SecurityException e) {
                Slog.w(this.TAG, "Unable to bind service: " + intent, e);
                startRetriesLocked();
            }
        }
    }

    private boolean matches(ComponentName component, int userId) {
        return Objects.equals(this.mComponent, component) && this.mUserId == userId;
    }

    private void startRetriesLocked() {
        if (checkAndDeliverServiceDiedCbLocked()) {
            disconnect();
        } else if (!this.mRetrying) {
            this.mRetrying = true;
            queueRetryLocked();
        }
    }

    private void stopRetriesLocked() {
        this.mRetrying = false;
        this.mHandler.removeCallbacks(this.mRetryRunnable);
    }

    private void queueRetryLocked() {
        long now = SystemClock.uptimeMillis();
        if (now - this.mLastRetryTimeMs > RETRY_RESET_TIME_MS) {
            this.mNextRetryDurationMs = MIN_RETRY_DURATION_MS;
            this.mRetryCount = 0;
        }
        this.mLastRetryTimeMs = now;
        this.mHandler.postDelayed(this.mRetryRunnable, this.mNextRetryDurationMs);
        this.mNextRetryDurationMs = Math.min(this.mNextRetryDurationMs * 2, MAX_RETRY_DURATION_MS);
        this.mRetryCount++;
    }

    private boolean checkAndDeliverServiceDiedCbLocked() {
        if (this.mRetryType != 2 && (this.mRetryType != 3 || this.mRetryCount < 4)) {
            return false;
        }
        Slog.e(this.TAG, "Service " + this.mComponent + " has died too much, not retrying.");
        if (this.mEventCb != null) {
            this.mHandler.post(new com.android.server.utils.-$Lambda$luWxpSWBY1-S73qs-S0xFqWHvIs.AnonymousClass1((byte) 3, System.currentTimeMillis(), this));
        }
        return true;
    }

    /* synthetic */ void lambda$-com_android_server_utils_ManagedApplicationService_17383(long timestamp) {
        this.mEventCb.onServiceEvent(new LogEvent(timestamp, this.mComponent, 4));
    }

    private void doRetry() {
        synchronized (this.mLock) {
            if (this.mConnection == null) {
            } else if (this.mRetrying) {
                Slog.i(this.TAG, "Attempting to reconnect " + this.mComponent + "...");
                disconnect();
                if (checkAndDeliverServiceDiedCbLocked()) {
                    return;
                }
                queueRetryLocked();
                connect();
            }
        }
    }
}
