package com.android.server.backup.params;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;
import java.util.ArrayList;

public class BackupParams {
    public String dirName;
    public ArrayList<String> fullPackages;
    public ArrayList<String> kvPackages;
    public OnTaskFinishedListener listener;
    public IBackupManagerMonitor monitor;
    public boolean nonIncrementalBackup;
    public IBackupObserver observer;
    public TransportClient transportClient;
    public boolean userInitiated;

    public BackupParams(TransportClient transportClient, String dirName, ArrayList<String> kvPackages, ArrayList<String> fullPackages, IBackupObserver observer, IBackupManagerMonitor monitor, OnTaskFinishedListener listener, boolean userInitiated, boolean nonIncrementalBackup) {
        this.transportClient = transportClient;
        this.dirName = dirName;
        this.kvPackages = kvPackages;
        this.fullPackages = fullPackages;
        this.observer = observer;
        this.monitor = monitor;
        this.listener = listener;
        this.userInitiated = userInitiated;
        this.nonIncrementalBackup = nonIncrementalBackup;
    }
}
