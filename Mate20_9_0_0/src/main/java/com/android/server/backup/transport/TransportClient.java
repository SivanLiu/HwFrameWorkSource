package com.android.server.backup.transport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.EventLog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.backup.IBackupTransport.Stub;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import dalvik.system.CloseGuard;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

public class TransportClient {
    private static final int LOG_BUFFER_SIZE = 5;
    @VisibleForTesting
    static final String TAG = "TransportClient";
    private final Intent mBindIntent;
    private final CloseGuard mCloseGuard;
    private final ServiceConnection mConnection;
    private final Context mContext;
    private final String mCreatorLogString;
    private final String mIdentifier;
    private final Handler mListenerHandler;
    @GuardedBy("mStateLock")
    private final Map<TransportConnectionListener, String> mListeners;
    @GuardedBy("mLogBufferLock")
    private final List<String> mLogBuffer;
    private final Object mLogBufferLock;
    private final String mPrefixForLog;
    @GuardedBy("mStateLock")
    private int mState;
    private final Object mStateLock;
    @GuardedBy("mStateLock")
    private volatile IBackupTransport mTransport;
    private final ComponentName mTransportComponent;
    private final TransportStats mTransportStats;

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
        public static final int BOUND_AND_CONNECTING = 2;
        public static final int CONNECTED = 3;
        public static final int IDLE = 1;
        public static final int UNUSABLE = 0;
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface Transition {
        public static final int DOWN = -1;
        public static final int NO_TRANSITION = 0;
        public static final int UP = 1;
    }

    private static class TransportConnection implements ServiceConnection {
        private final Context mContext;
        private final WeakReference<TransportClient> mTransportClientRef;

        private TransportConnection(Context context, TransportClient transportClient) {
            this.mContext = context;
            this.mTransportClientRef = new WeakReference(transportClient);
        }

        public void onServiceConnected(ComponentName transportComponent, IBinder binder) {
            TransportClient transportClient = (TransportClient) this.mTransportClientRef.get();
            if (transportClient == null) {
                referenceLost("TransportConnection.onServiceConnected()");
            } else {
                transportClient.onServiceConnected(binder);
            }
        }

        public void onServiceDisconnected(ComponentName transportComponent) {
            TransportClient transportClient = (TransportClient) this.mTransportClientRef.get();
            if (transportClient == null) {
                referenceLost("TransportConnection.onServiceDisconnected()");
            } else {
                transportClient.onServiceDisconnected();
            }
        }

        public void onBindingDied(ComponentName transportComponent) {
            TransportClient transportClient = (TransportClient) this.mTransportClientRef.get();
            if (transportClient == null) {
                referenceLost("TransportConnection.onBindingDied()");
            } else {
                transportClient.onBindingDied();
            }
        }

        private void referenceLost(String caller) {
            this.mContext.unbindService(this);
            String str = TransportClient.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(caller);
            stringBuilder.append(" called but TransportClient reference has been GC'ed");
            TransportUtils.log(4, str, stringBuilder.toString());
        }
    }

    TransportClient(Context context, TransportStats transportStats, Intent bindIntent, ComponentName transportComponent, String identifier, String caller) {
        this(context, transportStats, bindIntent, transportComponent, identifier, caller, new Handler(Looper.getMainLooper()));
    }

    @VisibleForTesting
    TransportClient(Context context, TransportStats transportStats, Intent bindIntent, ComponentName transportComponent, String identifier, String caller, Handler listenerHandler) {
        this.mStateLock = new Object();
        this.mLogBufferLock = new Object();
        this.mCloseGuard = CloseGuard.get();
        this.mLogBuffer = new LinkedList();
        this.mListeners = new ArrayMap();
        this.mState = 1;
        this.mContext = context;
        this.mTransportStats = transportStats;
        this.mTransportComponent = transportComponent;
        this.mBindIntent = bindIntent;
        this.mIdentifier = identifier;
        this.mCreatorLogString = caller;
        this.mListenerHandler = listenerHandler;
        this.mConnection = new TransportConnection(context, this);
        String classNameForLog = this.mTransportComponent.getShortClassName().replaceFirst(".*\\.", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(classNameForLog);
        stringBuilder.append("#");
        stringBuilder.append(this.mIdentifier);
        stringBuilder.append(":");
        this.mPrefixForLog = stringBuilder.toString();
        this.mCloseGuard.open("markAsDisposed");
    }

    public ComponentName getTransportComponent() {
        return this.mTransportComponent;
    }

    public void connectAsync(TransportConnectionListener listener, String caller) {
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            switch (this.mState) {
                case 0:
                    log(5, caller, "Async connect: UNUSABLE client");
                    notifyListener(listener, null, caller);
                    break;
                case 1:
                    if (!this.mContext.bindServiceAsUser(this.mBindIntent, this.mConnection, 1, UserHandle.SYSTEM)) {
                        log(6, "Async connect: bindService returned false");
                        this.mContext.unbindService(this.mConnection);
                        notifyListener(listener, null, caller);
                        break;
                    }
                    log(3, caller, "Async connect: service bound, connecting");
                    setStateLocked(2, null);
                    this.mListeners.put(listener, caller);
                    break;
                case 2:
                    log(3, caller, "Async connect: already connecting, adding listener");
                    this.mListeners.put(listener, caller);
                    break;
                case 3:
                    log(3, caller, "Async connect: reusing transport");
                    notifyListener(listener, this.mTransport, caller);
                    break;
            }
        }
    }

    public void unbind(String caller) {
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unbind requested (was ");
            stringBuilder.append(stateToString(this.mState));
            stringBuilder.append(")");
            log(3, caller, stringBuilder.toString());
            switch (this.mState) {
                case 2:
                    setStateLocked(1, null);
                    this.mContext.unbindService(this.mConnection);
                    notifyListenersAndClearLocked(null);
                    break;
                case 3:
                    setStateLocked(1, null);
                    this.mContext.unbindService(this.mConnection);
                    break;
            }
        }
    }

    public void markAsDisposed() {
        synchronized (this.mStateLock) {
            Preconditions.checkState(this.mState < 2, "Can't mark as disposed if still bound");
            this.mCloseGuard.close();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0069 A:{Splitter: B:14:0x0041, ExcHandler: java.lang.InterruptedException (r1_5 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:17:0x0069, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:18:0x006a, code:
            r2 = r1.getClass().getSimpleName();
            r9 = new java.lang.StringBuilder();
            r9.append(r2);
            r9.append(" while waiting for transport: ");
            r9.append(r1.getMessage());
            log(6, r15, r9.toString());
     */
    /* JADX WARNING: Missing block: B:19:0x008e, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public IBackupTransport connect(String caller) {
        Preconditions.checkState(Looper.getMainLooper().isCurrentThread() ^ true, "Can't call connect() on main thread");
        IBackupTransport transport = this.mTransport;
        if (transport != null) {
            log(3, caller, "Sync connect: reusing transport");
            return transport;
        }
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                log(5, caller, "Sync connect: UNUSABLE client");
                return null;
            }
            CompletableFuture<IBackupTransport> transportFuture = new CompletableFuture();
            TransportConnectionListener requestListener = new -$$Lambda$TransportClient$uc3fygwQjQIS_JT7mlt-yMBfJcE(transportFuture);
            long requestTime = SystemClock.elapsedRealtime();
            log(3, caller, "Sync connect: calling async");
            connectAsync(requestListener, caller);
            try {
                transport = (IBackupTransport) transportFuture.get();
                this.mTransportStats.registerConnectionTime(this.mTransportComponent, SystemClock.elapsedRealtime() - requestTime);
                log(3, caller, String.format(Locale.US, "Connect took %d ms", new Object[]{Long.valueOf(time)}));
                return transport;
            } catch (Exception e) {
            }
        }
    }

    public IBackupTransport connectOrThrow(String caller) throws TransportNotAvailableException {
        IBackupTransport transport = connect(caller);
        if (transport != null) {
            return transport;
        }
        log(6, caller, "Transport connection failed");
        throw new TransportNotAvailableException();
    }

    public IBackupTransport getConnectedTransport(String caller) throws TransportNotAvailableException {
        IBackupTransport transport = this.mTransport;
        if (transport != null) {
            return transport;
        }
        log(6, caller, "Transport not connected");
        throw new TransportNotAvailableException();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("TransportClient{");
        stringBuilder.append(this.mTransportComponent.flattenToShortString());
        stringBuilder.append("#");
        stringBuilder.append(this.mIdentifier);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    protected void finalize() throws Throwable {
        synchronized (this.mStateLock) {
            this.mCloseGuard.warnIfOpen();
            if (this.mState >= 2) {
                String callerLogString = "TransportClient.finalize()";
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Dangling TransportClient created in [");
                stringBuilder.append(this.mCreatorLogString);
                stringBuilder.append("] being GC'ed. Left bound, unbinding...");
                log(6, callerLogString, stringBuilder.toString());
                try {
                    unbind(callerLogString);
                } catch (IllegalStateException e) {
                }
            }
        }
    }

    private void onServiceConnected(IBinder binder) {
        IBackupTransport transport = Stub.asInterface(binder);
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            if (this.mState != 0) {
                log(3, "Transport connected");
                setStateLocked(3, transport);
                notifyListenersAndClearLocked(transport);
            }
        }
    }

    private void onServiceDisconnected() {
        synchronized (this.mStateLock) {
            log(6, "Service disconnected: client UNUSABLE");
            setStateLocked(0, null);
            try {
                this.mContext.unbindService(this.mConnection);
            } catch (IllegalArgumentException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception trying to unbind onServiceDisconnected(): ");
                stringBuilder.append(e.getMessage());
                log(5, stringBuilder.toString());
            }
        }
    }

    private void onBindingDied() {
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            log(6, "Binding died: client UNUSABLE");
            switch (this.mState) {
                case 1:
                    log(6, "Unexpected state transition IDLE => UNUSABLE");
                    setStateLocked(0, null);
                    break;
                case 2:
                    setStateLocked(0, null);
                    this.mContext.unbindService(this.mConnection);
                    notifyListenersAndClearLocked(null);
                    break;
                case 3:
                    setStateLocked(0, null);
                    this.mContext.unbindService(this.mConnection);
                    break;
            }
        }
    }

    private void notifyListener(TransportConnectionListener listener, IBackupTransport transport, String caller) {
        String transportString = transport != null ? "IBackupTransport" : "null";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Notifying [");
        stringBuilder.append(caller);
        stringBuilder.append("] transport = ");
        stringBuilder.append(transportString);
        log(4, stringBuilder.toString());
        this.mListenerHandler.post(new -$$Lambda$TransportClient$ciIUj0x0CRg93UETUpy2FB5aqCQ(this, listener, transport));
    }

    @GuardedBy("mStateLock")
    private void notifyListenersAndClearLocked(IBackupTransport transport) {
        for (Entry<TransportConnectionListener, String> entry : this.mListeners.entrySet()) {
            notifyListener((TransportConnectionListener) entry.getKey(), transport, (String) entry.getValue());
        }
        this.mListeners.clear();
    }

    @GuardedBy("mStateLock")
    private void setStateLocked(int state, IBackupTransport transport) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("State: ");
        stringBuilder.append(stateToString(this.mState));
        stringBuilder.append(" => ");
        stringBuilder.append(stateToString(state));
        log(2, stringBuilder.toString());
        onStateTransition(this.mState, state);
        this.mState = state;
        this.mTransport = transport;
    }

    private void onStateTransition(int oldState, int newState) {
        int value;
        String transport = this.mTransportComponent.flattenToShortString();
        int bound = transitionThroughState(oldState, newState, 2);
        int connected = transitionThroughState(oldState, newState, 3);
        if (bound != 0) {
            value = bound == 1 ? 1 : 0;
            EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_LIFECYCLE, new Object[]{transport, Integer.valueOf(value)});
        }
        if (connected != 0) {
            value = connected == 1 ? 1 : 0;
            EventLog.writeEvent(EventLogTags.BACKUP_TRANSPORT_CONNECTION, new Object[]{transport, Integer.valueOf(value)});
        }
    }

    private int transitionThroughState(int oldState, int newState, int stateReference) {
        if (oldState < stateReference && stateReference <= newState) {
            return 1;
        }
        if (oldState < stateReference || stateReference <= newState) {
            return 0;
        }
        return -1;
    }

    @GuardedBy("mStateLock")
    private void checkStateIntegrityLocked() {
        boolean z = true;
        switch (this.mState) {
            case 0:
                checkState(this.mListeners.isEmpty(), "Unexpected listeners when state = UNUSABLE");
                checkState(this.mTransport == null, "Transport expected to be null when state = UNUSABLE");
                break;
            case 1:
                break;
            case 2:
                if (this.mTransport != null) {
                    z = false;
                }
                checkState(z, "Transport expected to be null when state = BOUND_AND_CONNECTING");
                return;
            case 3:
                checkState(this.mListeners.isEmpty(), "Unexpected listeners when state = CONNECTED");
                if (this.mTransport == null) {
                    z = false;
                }
                checkState(z, "Transport expected to be non-null when state = CONNECTED");
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected state = ");
                stringBuilder.append(stateToString(this.mState));
                checkState(false, stringBuilder.toString());
                return;
        }
        checkState(this.mListeners.isEmpty(), "Unexpected listeners when state = IDLE");
        if (this.mTransport != null) {
            z = false;
        }
        checkState(z, "Transport expected to be null when state = IDLE");
    }

    private void checkState(boolean assertion, String message) {
        if (!assertion) {
            log(6, message);
        }
    }

    private String stateToString(int state) {
        switch (state) {
            case 0:
                return "UNUSABLE";
            case 1:
                return "IDLE";
            case 2:
                return "BOUND_AND_CONNECTING";
            case 3:
                return "CONNECTED";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<UNKNOWN = ");
                stringBuilder.append(state);
                stringBuilder.append(">");
                return stringBuilder.toString();
        }
    }

    private void log(int priority, String message) {
        TransportUtils.log(priority, TAG, TransportUtils.formatMessage(this.mPrefixForLog, null, message));
        saveLogEntry(TransportUtils.formatMessage(null, null, message));
    }

    private void log(int priority, String caller, String message) {
        TransportUtils.log(priority, TAG, TransportUtils.formatMessage(this.mPrefixForLog, caller, message));
        saveLogEntry(TransportUtils.formatMessage(null, caller, message));
    }

    private void saveLogEntry(String message) {
        CharSequence time = DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(time);
        stringBuilder.append(" ");
        stringBuilder.append(message);
        message = stringBuilder.toString();
        synchronized (this.mLogBufferLock) {
            if (this.mLogBuffer.size() == 5) {
                this.mLogBuffer.remove(this.mLogBuffer.size() - 1);
            }
            this.mLogBuffer.add(0, message);
        }
    }

    List<String> getLogBuffer() {
        List<String> unmodifiableList;
        synchronized (this.mLogBufferLock) {
            unmodifiableList = Collections.unmodifiableList(this.mLogBuffer);
        }
        return unmodifiableList;
    }
}
