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
    private final Runnable mRetryRunnable = new -$$Lambda$ManagedApplicationService$TUtdiUHqGW7Fae8jX7ATvPxzdeM(this);
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

    public interface PendingEvent {
        void runEvent(IInterface iInterface) throws RemoteException;
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dateFormat.format(new Date(this.timestamp)));
            stringBuilder.append("   ");
            stringBuilder.append(eventToString(this.event));
            stringBuilder.append(" Managed Service: ");
            stringBuilder.append(this.component == null ? "None" : this.component.flattenToString());
            return stringBuilder.toString();
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
        IInterface iface;
        synchronized (this.mLock) {
            iface = this.mBoundInterface;
            if (iface == null) {
                this.mPendingEvent = event;
            }
        }
        if (iface != null) {
            try {
                event.runEvent(iface);
            } catch (RemoteException | RuntimeException ex) {
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
                    String access$000 = ManagedApplicationService.this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Service binding died: ");
                    stringBuilder.append(componentName);
                    Slog.w(access$000, stringBuilder.toString());
                    synchronized (ManagedApplicationService.this.mLock) {
                        if (ManagedApplicationService.this.mConnection != this) {
                            return;
                        }
                        ManagedApplicationService.this.mHandler.post(new -$$Lambda$ManagedApplicationService$1$u8NdnzWjrb-KhRpDHf8fTyh3KVU(this, timestamp));
                        ManagedApplicationService.this.mBoundInterface = null;
                        ManagedApplicationService.this.startRetriesLocked();
                    }
                }

                /* JADX WARNING: Missing block: B:16:0x00ad, code skipped:
            if (r2 == null) goto L_0x00c6;
     */
                /* JADX WARNING: Missing block: B:17:0x00af, code skipped:
            if (r3 == null) goto L_0x00c6;
     */
                /* JADX WARNING: Missing block: B:19:?, code skipped:
            r3.runEvent(r2);
     */
                /* JADX WARNING: Missing block: B:20:0x00b5, code skipped:
            r4 = move-exception;
     */
                /* JADX WARNING: Missing block: B:21:0x00b6, code skipped:
            android.util.Slog.e(com.android.server.utils.ManagedApplicationService.access$000(r8.this$0), "Received exception from user service: ", r4);
            com.android.server.utils.ManagedApplicationService.access$500(r8.this$0);
     */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    long timestamp = System.currentTimeMillis();
                    String access$000 = ManagedApplicationService.this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Service connected: ");
                    stringBuilder.append(componentName);
                    Slog.i(access$000, stringBuilder.toString());
                    IInterface iface = null;
                    PendingEvent pendingEvent = null;
                    synchronized (ManagedApplicationService.this.mLock) {
                        if (ManagedApplicationService.this.mConnection != this) {
                            return;
                        }
                        ManagedApplicationService.this.mHandler.post(new -$$Lambda$ManagedApplicationService$1$IyJ0KZQns9OXjnHsop6Gzx7uhvA(this, timestamp));
                        ManagedApplicationService.this.stopRetriesLocked();
                        ManagedApplicationService.this.mBoundInterface = null;
                        if (ManagedApplicationService.this.mChecker != null) {
                            ManagedApplicationService.this.mBoundInterface = ManagedApplicationService.this.mChecker.asInterface(iBinder);
                            if (ManagedApplicationService.this.mChecker.checkType(ManagedApplicationService.this.mBoundInterface)) {
                                iface = ManagedApplicationService.this.mBoundInterface;
                                pendingEvent = ManagedApplicationService.this.mPendingEvent;
                                ManagedApplicationService.this.mPendingEvent = null;
                            } else {
                                ManagedApplicationService.this.mBoundInterface = null;
                                String access$0002 = ManagedApplicationService.this.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Invalid binder from ");
                                stringBuilder2.append(componentName);
                                Slog.w(access$0002, stringBuilder2.toString());
                                ManagedApplicationService.this.startRetriesLocked();
                            }
                        }
                    }
                }

                public void onServiceDisconnected(ComponentName componentName) {
                    long timestamp = System.currentTimeMillis();
                    String access$000 = ManagedApplicationService.this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Service disconnected: ");
                    stringBuilder.append(componentName);
                    Slog.w(access$000, stringBuilder.toString());
                    synchronized (ManagedApplicationService.this.mLock) {
                        if (ManagedApplicationService.this.mConnection != this) {
                            return;
                        }
                        ManagedApplicationService.this.mHandler.post(new -$$Lambda$ManagedApplicationService$1$iBg5-L6PAieAfuWNXxIPqvSlAAg(this, timestamp));
                        ManagedApplicationService.this.mBoundInterface = null;
                        ManagedApplicationService.this.startRetriesLocked();
                    }
                }
            };
            int flags = 67108865;
            if (this.mIsImportant) {
                flags = 67108865 | 64;
            }
            try {
                if (!this.mContext.bindServiceAsUser(intent, this.mConnection, flags, new UserHandle(this.mUserId))) {
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to bind service: ");
                    stringBuilder.append(intent);
                    Slog.w(str, stringBuilder.toString());
                    startRetriesLocked();
                }
            } catch (SecurityException e) {
                String str2 = this.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to bind service: ");
                stringBuilder2.append(intent);
                Slog.w(str2, stringBuilder2.toString(), e);
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
        this.mNextRetryDurationMs = Math.min(2 * this.mNextRetryDurationMs, MAX_RETRY_DURATION_MS);
        this.mRetryCount++;
    }

    private boolean checkAndDeliverServiceDiedCbLocked() {
        if (this.mRetryType != 2 && (this.mRetryType != 3 || this.mRetryCount < 4)) {
            return false;
        }
        String str = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Service ");
        stringBuilder.append(this.mComponent);
        stringBuilder.append(" has died too much, not retrying.");
        Slog.e(str, stringBuilder.toString());
        if (this.mEventCb != null) {
            this.mHandler.post(new -$$Lambda$ManagedApplicationService$7a-sAFwcUuC9yt8nXYlr0jScFcs(this, System.currentTimeMillis()));
        }
        return true;
    }

    private void doRetry() {
        synchronized (this.mLock) {
            if (this.mConnection == null) {
            } else if (this.mRetrying) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Attempting to reconnect ");
                stringBuilder.append(this.mComponent);
                stringBuilder.append("...");
                Slog.i(str, stringBuilder.toString());
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
