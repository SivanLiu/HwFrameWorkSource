package com.android.server.backup.params;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.content.pm.PackageInfo;
import com.android.internal.backup.IBackupTransport;

public class RestoreParams {
    public String dirName;
    public String[] filterSet;
    public boolean isSystemRestore;
    public IBackupManagerMonitor monitor;
    public IRestoreObserver observer;
    public PackageInfo pkgInfo;
    public int pmToken;
    public long token;
    public IBackupTransport transport;

    public RestoreParams(IBackupTransport _transport, String _dirName, IRestoreObserver _obs, IBackupManagerMonitor _monitor, long _token, PackageInfo _pkg) {
        this.transport = _transport;
        this.dirName = _dirName;
        this.observer = _obs;
        this.monitor = _monitor;
        this.token = _token;
        this.pkgInfo = _pkg;
        this.pmToken = 0;
        this.isSystemRestore = false;
        this.filterSet = null;
    }

    public RestoreParams(IBackupTransport _transport, String _dirName, IRestoreObserver _obs, IBackupManagerMonitor _monitor, long _token, String _pkgName, int _pmToken) {
        this.transport = _transport;
        this.dirName = _dirName;
        this.observer = _obs;
        this.monitor = _monitor;
        this.token = _token;
        this.pkgInfo = null;
        this.pmToken = _pmToken;
        this.isSystemRestore = false;
        this.filterSet = new String[]{_pkgName};
    }

    public RestoreParams(IBackupTransport _transport, String _dirName, IRestoreObserver _obs, IBackupManagerMonitor _monitor, long _token) {
        this.transport = _transport;
        this.dirName = _dirName;
        this.observer = _obs;
        this.monitor = _monitor;
        this.token = _token;
        this.pkgInfo = null;
        this.pmToken = 0;
        this.isSystemRestore = true;
        this.filterSet = null;
    }

    public RestoreParams(IBackupTransport _transport, String _dirName, IRestoreObserver _obs, IBackupManagerMonitor _monitor, long _token, String[] _filterSet, boolean _isSystemRestore) {
        this.transport = _transport;
        this.dirName = _dirName;
        this.observer = _obs;
        this.monitor = _monitor;
        this.token = _token;
        this.pkgInfo = null;
        this.pmToken = 0;
        this.isSystemRestore = _isSystemRestore;
        this.filterSet = _filterSet;
    }
}
