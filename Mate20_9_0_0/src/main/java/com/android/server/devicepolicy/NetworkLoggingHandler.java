package com.android.server.devicepolicy;

import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.app.admin.NetworkEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.LongSparseArray;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class NetworkLoggingHandler extends Handler {
    private static final long BATCH_FINALIZATION_TIMEOUT_ALARM_INTERVAL_MS = 1800000;
    private static final long BATCH_FINALIZATION_TIMEOUT_MS = 5400000;
    @VisibleForTesting
    static final int LOG_NETWORK_EVENT_MSG = 1;
    private static final int MAX_BATCHES = 5;
    private static final int MAX_EVENTS_PER_BATCH = 1200;
    static final String NETWORK_EVENT_KEY = "network_event";
    private static final String NETWORK_LOGGING_TIMEOUT_ALARM_TAG = "NetworkLogging.batchTimeout";
    private static final long RETRIEVED_BATCH_DISCARD_DELAY_MS = 300000;
    private static final String TAG = NetworkLoggingHandler.class.getSimpleName();
    private final AlarmManager mAlarmManager;
    private final OnAlarmListener mBatchTimeoutAlarmListener;
    @GuardedBy("this")
    private final LongSparseArray<ArrayList<NetworkEvent>> mBatches;
    @GuardedBy("this")
    private long mCurrentBatchToken;
    private final DevicePolicyManagerService mDpm;
    private long mId;
    @GuardedBy("this")
    private long mLastRetrievedBatchToken;
    @GuardedBy("this")
    private ArrayList<NetworkEvent> mNetworkEvents;
    @GuardedBy("this")
    private boolean mPaused;

    NetworkLoggingHandler(Looper looper, DevicePolicyManagerService dpm) {
        this(looper, dpm, 0);
    }

    @VisibleForTesting
    NetworkLoggingHandler(Looper looper, DevicePolicyManagerService dpm, long id) {
        super(looper);
        this.mBatchTimeoutAlarmListener = new OnAlarmListener() {
            public void onAlarm() {
                Bundle notificationExtras;
                String access$000 = NetworkLoggingHandler.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Received a batch finalization timeout alarm, finalizing ");
                stringBuilder.append(NetworkLoggingHandler.this.mNetworkEvents.size());
                stringBuilder.append(" pending events.");
                Slog.d(access$000, stringBuilder.toString());
                synchronized (NetworkLoggingHandler.this) {
                    notificationExtras = NetworkLoggingHandler.this.finalizeBatchAndBuildDeviceOwnerMessageLocked();
                }
                if (notificationExtras != null) {
                    NetworkLoggingHandler.this.notifyDeviceOwner(notificationExtras);
                }
            }
        };
        this.mNetworkEvents = new ArrayList();
        this.mBatches = new LongSparseArray(5);
        this.mPaused = false;
        this.mDpm = dpm;
        this.mAlarmManager = this.mDpm.mInjector.getAlarmManager();
        this.mId = id;
    }

    public void handleMessage(Message msg) {
        if (msg.what != 1) {
            Slog.d(TAG, "NetworkLoggingHandler received an unknown of message.");
            return;
        }
        NetworkEvent networkEvent = (NetworkEvent) msg.getData().getParcelable(NETWORK_EVENT_KEY);
        if (networkEvent != null) {
            Bundle notificationExtras = null;
            synchronized (this) {
                this.mNetworkEvents.add(networkEvent);
                if (this.mNetworkEvents.size() >= MAX_EVENTS_PER_BATCH) {
                    notificationExtras = finalizeBatchAndBuildDeviceOwnerMessageLocked();
                }
            }
            if (notificationExtras != null) {
                notifyDeviceOwner(notificationExtras);
            }
        }
    }

    void scheduleBatchFinalization() {
        this.mAlarmManager.setWindow(2, SystemClock.elapsedRealtime() + BATCH_FINALIZATION_TIMEOUT_MS, 1800000, NETWORK_LOGGING_TIMEOUT_ALARM_TAG, this.mBatchTimeoutAlarmListener, this);
        Slog.d(TAG, "Scheduled a new batch finalization alarm 5400000ms from now.");
    }

    synchronized void pause() {
        Slog.d(TAG, "Paused network logging");
        this.mPaused = true;
    }

    /* JADX WARNING: Missing block: B:14:0x004d, code:
            if (r0 == null) goto L_0x0052;
     */
    /* JADX WARNING: Missing block: B:15:0x004f, code:
            notifyDeviceOwner(r0);
     */
    /* JADX WARNING: Missing block: B:16:0x0052, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void resume() {
        Bundle notificationExtras = null;
        synchronized (this) {
            if (this.mPaused) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Resumed network logging. Current batch=");
                stringBuilder.append(this.mCurrentBatchToken);
                stringBuilder.append(", LastRetrievedBatch=");
                stringBuilder.append(this.mLastRetrievedBatchToken);
                Slog.d(str, stringBuilder.toString());
                this.mPaused = false;
                if (this.mBatches.size() > 0 && this.mLastRetrievedBatchToken != this.mCurrentBatchToken) {
                    scheduleBatchFinalization();
                    notificationExtras = buildDeviceOwnerMessageLocked();
                }
            } else {
                Slog.d(TAG, "Attempted to resume network logging, but logging is not paused.");
            }
        }
    }

    synchronized void discardLogs() {
        this.mBatches.clear();
        this.mNetworkEvents = new ArrayList();
        Slog.d(TAG, "Discarded all network logs");
    }

    @GuardedBy("this")
    private Bundle finalizeBatchAndBuildDeviceOwnerMessageLocked() {
        Bundle notificationExtras = null;
        if (this.mNetworkEvents.size() > 0) {
            Iterator it = this.mNetworkEvents.iterator();
            while (it.hasNext()) {
                ((NetworkEvent) it.next()).setId(this.mId);
                if (this.mId == JobStatus.NO_LATEST_RUNTIME) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Reached maximum id value; wrapping around .");
                    stringBuilder.append(this.mCurrentBatchToken);
                    Slog.i(str, stringBuilder.toString());
                    this.mId = 0;
                } else {
                    this.mId++;
                }
            }
            if (this.mBatches.size() >= 5) {
                this.mBatches.removeAt(0);
            }
            this.mCurrentBatchToken++;
            this.mBatches.append(this.mCurrentBatchToken, this.mNetworkEvents);
            this.mNetworkEvents = new ArrayList();
            if (!this.mPaused) {
                notificationExtras = buildDeviceOwnerMessageLocked();
            }
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Was about to finalize the batch, but there were no events to send to the DPC, the batchToken of last available batch: ");
            stringBuilder2.append(this.mCurrentBatchToken);
            Slog.d(str2, stringBuilder2.toString());
        }
        scheduleBatchFinalization();
        return notificationExtras;
    }

    @GuardedBy("this")
    private Bundle buildDeviceOwnerMessageLocked() {
        Bundle extras = new Bundle();
        int lastBatchSize = ((ArrayList) this.mBatches.valueAt(this.mBatches.size() - 1)).size();
        extras.putLong("android.app.extra.EXTRA_NETWORK_LOGS_TOKEN", this.mCurrentBatchToken);
        extras.putInt("android.app.extra.EXTRA_NETWORK_LOGS_COUNT", lastBatchSize);
        return extras;
    }

    private void notifyDeviceOwner(Bundle extras) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Sending network logging batch broadcast to device owner, batchToken: ");
        stringBuilder.append(extras.getLong("android.app.extra.EXTRA_NETWORK_LOGS_TOKEN", -1));
        Slog.d(str, stringBuilder.toString());
        if (Thread.holdsLock(this)) {
            Slog.wtfStack(TAG, "Shouldn't be called with NetworkLoggingHandler lock held");
        } else {
            this.mDpm.sendDeviceOwnerCommand("android.app.action.NETWORK_LOGS_AVAILABLE", extras);
        }
    }

    synchronized List<NetworkEvent> retrieveFullLogBatch(long batchToken) {
        int index = this.mBatches.indexOfKey(batchToken);
        if (index < 0) {
            return null;
        }
        postDelayed(new -$$Lambda$NetworkLoggingHandler$VKC_fB9Ws13yQKJ8zNkiF3Wp0Jk(this, batchToken), 300000);
        this.mLastRetrievedBatchToken = batchToken;
        return (List) this.mBatches.valueAt(index);
    }

    public static /* synthetic */ void lambda$retrieveFullLogBatch$0(NetworkLoggingHandler networkLoggingHandler, long batchToken) {
        synchronized (networkLoggingHandler) {
            while (networkLoggingHandler.mBatches.size() > 0 && networkLoggingHandler.mBatches.keyAt(0) <= batchToken) {
                networkLoggingHandler.mBatches.removeAt(0);
            }
        }
    }
}
