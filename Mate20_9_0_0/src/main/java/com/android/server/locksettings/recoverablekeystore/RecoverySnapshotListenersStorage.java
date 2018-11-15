package com.android.server.locksettings.recoverablekeystore;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;

public class RecoverySnapshotListenersStorage {
    private static final String TAG = "RecoverySnapshotLstnrs";
    @GuardedBy("this")
    private SparseArray<PendingIntent> mAgentIntents = new SparseArray();
    @GuardedBy("this")
    private ArraySet<Integer> mAgentsWithPendingSnapshots = new ArraySet();

    public synchronized void setSnapshotListener(int recoveryAgentUid, PendingIntent intent) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Registered listener for agent with uid ");
        stringBuilder.append(recoveryAgentUid);
        Log.i(str, stringBuilder.toString());
        this.mAgentIntents.put(recoveryAgentUid, intent);
        if (this.mAgentsWithPendingSnapshots.contains(Integer.valueOf(recoveryAgentUid))) {
            Log.i(TAG, "Snapshot already created for agent. Immediately triggering intent.");
            tryToSendIntent(recoveryAgentUid, intent);
        }
    }

    public synchronized boolean hasListener(int recoveryAgentUid) {
        return this.mAgentIntents.get(recoveryAgentUid) != null;
    }

    public synchronized void recoverySnapshotAvailable(int recoveryAgentUid) {
        PendingIntent intent = (PendingIntent) this.mAgentIntents.get(recoveryAgentUid);
        if (intent == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Snapshot available for agent ");
            stringBuilder.append(recoveryAgentUid);
            stringBuilder.append(" but agent has not yet initialized. Will notify agent when it does.");
            Log.i(str, stringBuilder.toString());
            this.mAgentsWithPendingSnapshots.add(Integer.valueOf(recoveryAgentUid));
            return;
        }
        tryToSendIntent(recoveryAgentUid, intent);
    }

    private synchronized void tryToSendIntent(int recoveryAgentUid, PendingIntent intent) {
        try {
            intent.send();
            this.mAgentsWithPendingSnapshots.remove(Integer.valueOf(recoveryAgentUid));
            Log.d(TAG, "Successfully notified listener.");
        } catch (CanceledException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to trigger PendingIntent for ");
            stringBuilder.append(recoveryAgentUid);
            Log.e(str, stringBuilder.toString(), e);
            this.mAgentsWithPendingSnapshots.add(Integer.valueOf(recoveryAgentUid));
        }
        return;
    }
}
