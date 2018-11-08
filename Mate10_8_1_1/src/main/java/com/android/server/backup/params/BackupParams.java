package com.android.server.backup.params;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import com.android.internal.backup.IBackupTransport;
import java.util.ArrayList;

public class BackupParams {
    public String dirName;
    public ArrayList<String> fullPackages;
    public ArrayList<String> kvPackages;
    public IBackupManagerMonitor monitor;
    public boolean nonIncrementalBackup;
    public IBackupObserver observer;
    public IBackupTransport transport;
    public boolean userInitiated;

    public BackupParams(IBackupTransport transport, String dirName, ArrayList<String> kvPackages, ArrayList<String> fullPackages, IBackupObserver observer, IBackupManagerMonitor monitor, boolean userInitiated, boolean nonIncrementalBackup) {
        this.transport = transport;
        this.dirName = dirName;
        this.kvPackages = kvPackages;
        this.fullPackages = fullPackages;
        this.observer = observer;
        this.monitor = monitor;
        this.userInitiated = userInitiated;
        this.nonIncrementalBackup = nonIncrementalBackup;
    }
}
