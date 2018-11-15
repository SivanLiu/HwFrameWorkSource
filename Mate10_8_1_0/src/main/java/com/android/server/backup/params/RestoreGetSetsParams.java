package com.android.server.backup.params;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import com.android.internal.backup.IBackupTransport;
import com.android.server.backup.restore.ActiveRestoreSession;

public class RestoreGetSetsParams {
    public IBackupManagerMonitor monitor;
    public IRestoreObserver observer;
    public ActiveRestoreSession session;
    public IBackupTransport transport;

    public RestoreGetSetsParams(IBackupTransport _transport, ActiveRestoreSession _session, IRestoreObserver _observer, IBackupManagerMonitor _monitor) {
        this.transport = _transport;
        this.session = _session;
        this.observer = _observer;
        this.monitor = _monitor;
    }
}
