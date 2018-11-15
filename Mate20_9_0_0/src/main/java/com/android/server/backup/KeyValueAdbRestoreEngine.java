package com.android.server.backup;

import android.app.IBackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackup;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.backup.restore.PerformAdbRestoreTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;

public class KeyValueAdbRestoreEngine implements Runnable {
    private static final boolean DEBUG = false;
    private static final String TAG = "KeyValueAdbRestoreEngine";
    IBackupAgent mAgent;
    private final BackupManagerServiceInterface mBackupManagerService;
    private final File mDataDir;
    ParcelFileDescriptor mInFD;
    FileMetadata mInfo;
    PerformAdbRestoreTask mRestoreTask;
    int mToken;

    public KeyValueAdbRestoreEngine(BackupManagerServiceInterface backupManagerService, File dataDir, FileMetadata info, ParcelFileDescriptor inFD, IBackupAgent agent, int token) {
        this.mBackupManagerService = backupManagerService;
        this.mDataDir = dataDir;
        this.mInfo = info;
        this.mInFD = inFD;
        this.mAgent = agent;
        this.mToken = token;
    }

    public void run() {
        try {
            invokeAgentForAdbRestore(this.mAgent, this.mInfo, prepareRestoreData(this.mInfo, this.mInFD));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File prepareRestoreData(FileMetadata info, ParcelFileDescriptor inFD) throws IOException {
        String pkg = info.packageName;
        File file = this.mDataDir;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pkg);
        stringBuilder.append(".restore");
        File restoreDataName = new File(file, stringBuilder.toString());
        File file2 = this.mDataDir;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(pkg);
        stringBuilder2.append(".sorted");
        file = new File(file2, stringBuilder2.toString());
        FullBackup.restoreFile(inFD, info.size, info.type, info.mode, info.mtime, restoreDataName);
        sortKeyValueData(restoreDataName, file);
        return file;
    }

    private void invokeAgentForAdbRestore(IBackupAgent agent, FileMetadata info, File restoreData) throws IOException {
        String str;
        StringBuilder stringBuilder;
        String pkg = info.packageName;
        File file = this.mDataDir;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(pkg);
        stringBuilder2.append(".new");
        File newStateName = new File(file, stringBuilder2.toString());
        try {
            IBackupAgent iBackupAgent = agent;
            iBackupAgent.doRestore(ParcelFileDescriptor.open(restoreData, 268435456), info.version, ParcelFileDescriptor.open(newStateName, 1006632960), this.mToken, this.mBackupManagerService.getBackupManagerBinder());
        } catch (IOException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception opening file. ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        } catch (RemoteException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception calling doRestore on agent: ");
            stringBuilder.append(e2);
            Slog.e(str, stringBuilder.toString());
        }
    }

    private void sortKeyValueData(File restoreData, File sortedData) throws IOException {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(restoreData);
            outputStream = new FileOutputStream(sortedData);
            copyKeysInLexicalOrder(new BackupDataInput(inputStream.getFD()), new BackupDataOutput(outputStream.getFD()));
            IoUtils.closeQuietly(inputStream);
            IoUtils.closeQuietly(outputStream);
        } catch (Throwable th) {
            if (inputStream != null) {
                IoUtils.closeQuietly(inputStream);
            }
            if (outputStream != null) {
                IoUtils.closeQuietly(outputStream);
            }
        }
    }

    private void copyKeysInLexicalOrder(BackupDataInput in, BackupDataOutput out) throws IOException {
        Map<String, byte[]> data = new HashMap();
        while (in.readNextHeader()) {
            String key = in.getKey();
            int size = in.getDataSize();
            if (size < 0) {
                in.skipEntityData();
            } else {
                byte[] value = new byte[size];
                in.readEntityData(value, 0, size);
                data.put(key, value);
            }
        }
        List<String> keys = new ArrayList(data.keySet());
        Collections.sort(keys);
        for (String key2 : keys) {
            byte[] value2 = (byte[]) data.get(key2);
            out.writeEntityHeader(key2, value2.length);
            out.writeEntityData(value2, value2.length);
        }
    }
}
