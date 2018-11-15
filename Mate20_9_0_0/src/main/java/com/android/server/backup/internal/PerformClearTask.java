package com.android.server.backup.internal;

import android.content.pm.PackageInfo;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.TransportManager;
import com.android.server.backup.transport.TransportClient;
import java.io.File;

public class PerformClearTask implements Runnable {
    private final BackupManagerService mBackupManagerService;
    private final OnTaskFinishedListener mListener;
    private final PackageInfo mPackage;
    private final TransportClient mTransportClient;
    private final TransportManager mTransportManager;

    PerformClearTask(BackupManagerService backupManagerService, TransportClient transportClient, PackageInfo packageInfo, OnTaskFinishedListener listener) {
        this.mBackupManagerService = backupManagerService;
        this.mTransportManager = backupManagerService.getTransportManager();
        this.mTransportClient = transportClient;
        this.mPackage = packageInfo;
        this.mListener = listener;
    }

    public void run() {
        Exception e;
        String str;
        StringBuilder stringBuilder;
        String callerLogString = "PerformClearTask.run()";
        IBackupTransport transport = null;
        try {
            new File(new File(this.mBackupManagerService.getBaseStateDir(), this.mTransportManager.getTransportDirName(this.mTransportClient.getTransportComponent())), this.mPackage.packageName).delete();
            transport = this.mTransportClient.connectOrThrow(callerLogString);
            transport.clearBackupData(this.mPackage);
            if (transport != null) {
                try {
                    transport.finishBackup();
                } catch (Exception e2) {
                    e = e2;
                    str = BackupManagerService.TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Exception e3) {
            str = BackupManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Transport threw clearing data for ");
            stringBuilder.append(this.mPackage);
            stringBuilder.append(": ");
            stringBuilder.append(e3.getMessage());
            Slog.e(str, stringBuilder.toString());
            if (transport != null) {
                try {
                    transport.finishBackup();
                } catch (Exception e4) {
                    e3 = e4;
                    str = BackupManagerService.TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (transport != null) {
                try {
                    transport.finishBackup();
                } catch (Exception e5) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to mark clear operation finished: ");
                    stringBuilder.append(e5.getMessage());
                    Slog.e(BackupManagerService.TAG, stringBuilder.toString());
                }
            }
            this.mListener.onFinished(callerLogString);
            this.mBackupManagerService.getWakelock().release();
        }
        this.mListener.onFinished(callerLogString);
        this.mBackupManagerService.getWakelock().release();
        stringBuilder.append("Unable to mark clear operation finished: ");
        stringBuilder.append(e3.getMessage());
        Slog.e(str, stringBuilder.toString());
        this.mListener.onFinished(callerLogString);
        this.mBackupManagerService.getWakelock().release();
    }
}
