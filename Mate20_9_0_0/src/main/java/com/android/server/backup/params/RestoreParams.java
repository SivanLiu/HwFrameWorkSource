package com.android.server.backup.params;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.content.pm.PackageInfo;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;

public class RestoreParams {
    public final String[] filterSet;
    public final boolean isSystemRestore;
    public final OnTaskFinishedListener listener;
    public final IBackupManagerMonitor monitor;
    public final IRestoreObserver observer;
    public final PackageInfo packageInfo;
    public final int pmToken;
    public final long token;
    public final TransportClient transportClient;

    public static RestoreParams createForSinglePackage(TransportClient transportClient, IRestoreObserver observer, IBackupManagerMonitor monitor, long token, PackageInfo packageInfo, OnTaskFinishedListener listener) {
        return new RestoreParams(transportClient, observer, monitor, token, packageInfo, 0, false, null, listener);
    }

    public static RestoreParams createForRestoreAtInstall(TransportClient transportClient, IRestoreObserver observer, IBackupManagerMonitor monitor, long token, String packageName, int pmToken, OnTaskFinishedListener listener) {
        return new RestoreParams(transportClient, observer, monitor, token, null, pmToken, false, new String[]{packageName}, listener);
    }

    public static RestoreParams createForRestoreAll(TransportClient transportClient, IRestoreObserver observer, IBackupManagerMonitor monitor, long token, OnTaskFinishedListener listener) {
        return new RestoreParams(transportClient, observer, monitor, token, null, 0, true, null, listener);
    }

    public static RestoreParams createForRestoreSome(TransportClient transportClient, IRestoreObserver observer, IBackupManagerMonitor monitor, long token, String[] filterSet, boolean isSystemRestore, OnTaskFinishedListener listener) {
        return new RestoreParams(transportClient, observer, monitor, token, null, 0, isSystemRestore, filterSet, listener);
    }

    private RestoreParams(TransportClient transportClient, IRestoreObserver observer, IBackupManagerMonitor monitor, long token, PackageInfo packageInfo, int pmToken, boolean isSystemRestore, String[] filterSet, OnTaskFinishedListener listener) {
        this.transportClient = transportClient;
        this.observer = observer;
        this.monitor = monitor;
        this.token = token;
        this.packageInfo = packageInfo;
        this.pmToken = pmToken;
        this.isSystemRestore = isSystemRestore;
        this.filterSet = filterSet;
        this.listener = listener;
    }
}
