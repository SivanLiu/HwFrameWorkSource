package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.KeyValueAdbRestoreEngine;
import com.android.server.backup.fullbackup.FullBackupObbConnection;
import com.android.server.backup.utils.BytesReadListener;
import com.android.server.backup.utils.FullBackupRestoreObserverUtils;
import com.android.server.backup.utils.RestoreUtils;
import com.android.server.backup.utils.TarBackupReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class FullRestoreEngine extends RestoreEngine {
    private IBackupAgent mAgent;
    private String mAgentPackage;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    final boolean mAllowApks;
    private final boolean mAllowObbs;
    private final BackupManagerService mBackupManagerService;
    final byte[] mBuffer;
    private long mBytes;
    private final HashSet<String> mClearedPackages = new HashSet();
    private final RestoreDeleteObserver mDeleteObserver = new RestoreDeleteObserver();
    final int mEphemeralOpToken;
    private final HashMap<String, Signature[]> mManifestSignatures = new HashMap();
    final IBackupManagerMonitor mMonitor;
    private final BackupRestoreTask mMonitorTask;
    private FullBackupObbConnection mObbConnection = null;
    private IFullBackupRestoreObserver mObserver;
    final PackageInfo mOnlyPackage;
    private final HashMap<String, String> mPackageInstallers = new HashMap();
    private final HashMap<String, RestorePolicy> mPackagePolicies = new HashMap();
    private ParcelFileDescriptor[] mPipes = null;
    private ApplicationInfo mTargetApp;
    private byte[] mWidgetData = null;

    static /* synthetic */ long access$014(FullRestoreEngine x0, long x1) {
        long j = x0.mBytes + x1;
        x0.mBytes = j;
        return j;
    }

    public FullRestoreEngine(BackupManagerService backupManagerService, BackupRestoreTask monitorTask, IFullBackupRestoreObserver observer, IBackupManagerMonitor monitor, PackageInfo onlyPackage, boolean allowApks, boolean allowObbs, int ephemeralOpToken) {
        this.mBackupManagerService = backupManagerService;
        this.mEphemeralOpToken = ephemeralOpToken;
        this.mMonitorTask = monitorTask;
        this.mObserver = observer;
        this.mMonitor = monitor;
        this.mOnlyPackage = onlyPackage;
        this.mAllowApks = allowApks;
        this.mAllowObbs = allowObbs;
        this.mBuffer = new byte[32768];
        this.mBytes = 0;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    public IBackupAgent getAgent() {
        return this.mAgent;
    }

    public byte[] getWidgetData() {
        return this.mWidgetData;
    }

    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x04ca A:{Catch:{ IOException -> 0x059f }} */
    /* JADX WARNING: Removed duplicated region for block: B:238:0x0545 A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x054d A:{Catch:{ IOException -> 0x0594 }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:0x0597 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x05b6 A:{Catch:{ IOException -> 0x05f0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:282:0x0644  */
    /* JADX WARNING: Removed duplicated region for block: B:279:0x0635  */
    /* JADX WARNING: Removed duplicated region for block: B:285:0x064a  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0647  */
    /* JADX WARNING: Missing block: B:81:0x01ea, code:
            if (isCanonicalFilePath(r3.path) == false) goto L_0x01f6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean restoreOneFile(InputStream instream, boolean mustKillAgent, byte[] buffer, PackageInfo onlyPackage, boolean allowApks, int token, IBackupManagerMonitor monitor) {
        IOException e;
        int i;
        PackageInfo packageInfo;
        Object obj;
        IBackupManagerMonitor iBackupManagerMonitor;
        String str;
        StringBuilder stringBuilder;
        FileMetadata info;
        FileMetadata info2;
        boolean z;
        boolean okay;
        boolean z2;
        TarBackupReader tarBackupReader;
        boolean agentSuccess;
        InputStream inputStream = instream;
        byte[] bArr = buffer;
        PackageInfo packageInfo2 = onlyPackage;
        if (isRunning()) {
            BytesReadListener bytesReadListener = new BytesReadListener() {
                public void onBytesRead(long bytesRead) {
                    FullRestoreEngine.access$014(FullRestoreEngine.this, bytesRead);
                }
            };
            IBackupManagerMonitor iBackupManagerMonitor2 = monitor;
            TarBackupReader tarBackupReader2 = new TarBackupReader(inputStream, bytesReadListener, iBackupManagerMonitor2);
            BytesReadListener bytesReadListener2;
            try {
                FileMetadata info3;
                FileMetadata info4 = tarBackupReader2.readTarHeaders();
                if (info4 != null) {
                    String str2;
                    StringBuilder stringBuilder2;
                    String pkg = info4.packageName;
                    if (!pkg.equals(this.mAgentPackage)) {
                        if (packageInfo2 != null) {
                            try {
                                if (!pkg.equals(packageInfo2.packageName)) {
                                    str2 = BackupManagerService.TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Expected data for ");
                                    stringBuilder2.append(packageInfo2);
                                    stringBuilder2.append(" but saw ");
                                    stringBuilder2.append(pkg);
                                    Slog.w(str2, stringBuilder2.toString());
                                    setResult(-3);
                                    setRunning(false);
                                    return false;
                                }
                            } catch (IOException e2) {
                                e = e2;
                                i = token;
                                packageInfo = packageInfo2;
                                obj = bytesReadListener;
                                iBackupManagerMonitor = iBackupManagerMonitor2;
                                str = BackupManagerService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("io exception on restore socket read: ");
                                stringBuilder.append(e.getMessage());
                                Slog.w(str, stringBuilder.toString());
                                setResult(-3);
                                info = null;
                                info2 = info;
                                if (info2 != null) {
                                }
                                if (info2 == null) {
                                }
                                return info2 == null ? true : z;
                            }
                        }
                        if (!this.mPackagePolicies.containsKey(pkg)) {
                            this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                        }
                        if (this.mAgent != null) {
                            Slog.d(BackupManagerService.TAG, "Saw new package; finalizing old one");
                            tearDownPipes();
                            tearDownAgent(this.mTargetApp);
                            this.mTargetApp = null;
                            this.mAgentPackage = null;
                        }
                    }
                    String pkg2;
                    int i2;
                    FileMetadata info5;
                    if (info4.path.equals(BackupManagerService.BACKUP_MANIFEST_FILENAME)) {
                        try {
                            Signature[] signatures = tarBackupReader2.readAppManifestAndReturnSignatures(info4);
                            pkg2 = pkg;
                            FileMetadata info6 = info4;
                            Object obj2 = null;
                            i2 = 1;
                            RestorePolicy restorePolicy = tarBackupReader2.chooseRestorePolicy(this.mBackupManagerService.getPackageManager(), allowApks, info6, signatures, (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
                            info5 = info6;
                            this.mManifestSignatures.put(info5.packageName, signatures);
                            this.mPackagePolicies.put(pkg2, restorePolicy);
                            this.mPackageInstallers.put(pkg2, info5.installerPackageName);
                            tarBackupReader2.skipTarPadding(info5.size);
                            this.mObserver = FullBackupRestoreObserverUtils.sendOnRestorePackage(this.mObserver, pkg2);
                            i = token;
                            info3 = info5;
                            packageInfo = packageInfo2;
                            obj = bytesReadListener;
                        } catch (IOException e3) {
                            e = e3;
                            i = token;
                            packageInfo = packageInfo2;
                            str = BackupManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("io exception on restore socket read: ");
                            stringBuilder.append(e.getMessage());
                            Slog.w(str, stringBuilder.toString());
                            setResult(-3);
                            info = null;
                            info2 = info;
                            if (info2 != null) {
                            }
                            if (info2 == null) {
                            }
                            return info2 == null ? true : z;
                        }
                    }
                    pkg2 = pkg;
                    i2 = 1;
                    info5 = info4;
                    if (info5.path.equals(BackupManagerService.BACKUP_METADATA_FILENAME)) {
                        tarBackupReader2.readMetadata(info5);
                        this.mWidgetData = tarBackupReader2.getWidgetData();
                        IBackupManagerMonitor monitor2 = tarBackupReader2.getMonitor();
                        try {
                            tarBackupReader2.skipTarPadding(info5.size);
                            i = token;
                            iBackupManagerMonitor = monitor2;
                            info3 = info5;
                            packageInfo = packageInfo2;
                            bytesReadListener2 = bytesReadListener;
                            info = info3;
                        } catch (IOException e4) {
                            e = e4;
                            i = token;
                            iBackupManagerMonitor = monitor2;
                            packageInfo = packageInfo2;
                            bytesReadListener2 = bytesReadListener;
                            str = BackupManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("io exception on restore socket read: ");
                            stringBuilder.append(e.getMessage());
                            Slog.w(str, stringBuilder.toString());
                            setResult(-3);
                            info = null;
                            info2 = info;
                            if (info2 != null) {
                            }
                            if (info2 == null) {
                            }
                            return info2 == null ? true : z;
                        }
                        info2 = info;
                        if (info2 != null) {
                            tearDownPipes();
                            z = false;
                            setRunning(false);
                            if (mustKillAgent) {
                                tearDownAgent(this.mTargetApp);
                            }
                        } else {
                            z = false;
                        }
                        return info2 == null ? true : z;
                    }
                    TarBackupReader tarBackupReader3;
                    boolean isSuccessfullyInstalled;
                    StringBuilder stringBuilder3;
                    String pkg3;
                    boolean okay2 = true;
                    RestorePolicy policy = (RestorePolicy) this.mPackagePolicies.get(pkg2);
                    RestorePolicy restorePolicy2;
                    switch (policy) {
                        case IGNORE:
                            info3 = info5;
                            restorePolicy2 = policy;
                            tarBackupReader3 = tarBackupReader2;
                            okay2 = false;
                            break;
                        case ACCEPT_IF_APK:
                            try {
                                if (!info5.domain.equals("a")) {
                                    info3 = info5;
                                    restorePolicy2 = policy;
                                    tarBackupReader3 = tarBackupReader2;
                                    this.mPackagePolicies.put(pkg2, RestorePolicy.IGNORE);
                                    okay2 = false;
                                    break;
                                }
                                Slog.d(BackupManagerService.TAG, "APK file; installing");
                                int i3 = -3;
                                FileMetadata info7 = info5;
                                restorePolicy2 = policy;
                                tarBackupReader3 = tarBackupReader2;
                                try {
                                    Object obj3;
                                    isSuccessfullyInstalled = RestoreUtils.installApk(inputStream, this.mBackupManagerService.getContext(), this.mDeleteObserver, this.mManifestSignatures, this.mPackagePolicies, info7, (String) this.mPackageInstallers.get(pkg2), bytesReadListener);
                                    HashMap hashMap = this.mPackagePolicies;
                                    if (isSuccessfullyInstalled) {
                                        obj3 = RestorePolicy.ACCEPT;
                                    } else {
                                        obj3 = RestorePolicy.IGNORE;
                                    }
                                    hashMap.put(pkg2, obj3);
                                    tarBackupReader3.skipTarPadding(info7.size);
                                    return true;
                                } catch (IOException e5) {
                                    e = e5;
                                    i = token;
                                    packageInfo = packageInfo2;
                                    bytesReadListener2 = bytesReadListener;
                                    tarBackupReader2 = tarBackupReader3;
                                    break;
                                }
                            } catch (IOException e6) {
                                e = e6;
                                i = token;
                                packageInfo = packageInfo2;
                                bytesReadListener2 = bytesReadListener;
                                iBackupManagerMonitor = monitor;
                                str = BackupManagerService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("io exception on restore socket read: ");
                                stringBuilder.append(e.getMessage());
                                Slog.w(str, stringBuilder.toString());
                                setResult(-3);
                                info = null;
                                info2 = info;
                                if (info2 != null) {
                                }
                                if (info2 == null) {
                                }
                                return info2 == null ? true : z;
                            }
                            break;
                        case ACCEPT:
                            if (info5.domain.equals("a")) {
                                Slog.d(BackupManagerService.TAG, "apk present but ACCEPT");
                                okay2 = false;
                            }
                            info3 = info5;
                            tarBackupReader3 = tarBackupReader2;
                            break;
                        default:
                            info3 = info5;
                            tarBackupReader3 = tarBackupReader2;
                            try {
                                Slog.e(BackupManagerService.TAG, "Invalid policy from manifest");
                                okay2 = false;
                                this.mPackagePolicies.put(pkg2, RestorePolicy.IGNORE);
                                break;
                            } catch (IOException e7) {
                                e = e7;
                                i = token;
                                packageInfo = packageInfo2;
                                bytesReadListener2 = bytesReadListener;
                                tarBackupReader2 = tarBackupReader3;
                                iBackupManagerMonitor = monitor;
                                break;
                            }
                    }
                    if (isRestorableFile(info3)) {
                    }
                    okay2 = false;
                    isSuccessfullyInstalled = okay2;
                    if (isSuccessfullyInstalled && this.mAgent == null) {
                        try {
                            this.mTargetApp = this.mBackupManagerService.getPackageManager().getApplicationInfo(pkg2, 0);
                            if (!this.mClearedPackages.contains(pkg2)) {
                                okay2 = shouldForceClearAppDataOnFullRestore(this.mTargetApp.packageName);
                                if (this.mTargetApp.backupAgentName == null || okay2) {
                                    Slog.d(BackupManagerService.TAG, "Clearing app data preparatory to full restore");
                                    this.mBackupManagerService.clearApplicationDataSynchronous(pkg2, true);
                                }
                                this.mClearedPackages.add(pkg2);
                            }
                            setUpPipes();
                            this.mAgent = this.mBackupManagerService.bindToAgentSynchronous(this.mTargetApp, 3);
                            this.mAgentPackage = pkg2;
                        } catch (IOException e8) {
                        } catch (NameNotFoundException e9) {
                        }
                        if (this.mAgent == null) {
                            str2 = BackupManagerService.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Unable to create agent for ");
                            stringBuilder3.append(pkg2);
                            Slog.e(str2, stringBuilder3.toString());
                            isSuccessfullyInstalled = false;
                            tearDownPipes();
                            this.mPackagePolicies.put(pkg2, RestorePolicy.IGNORE);
                        }
                    }
                    if (isSuccessfullyInstalled && !pkg2.equals(this.mAgentPackage)) {
                        str2 = BackupManagerService.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Restoring data for ");
                        stringBuilder3.append(pkg2);
                        stringBuilder3.append(" but agent is for ");
                        stringBuilder3.append(this.mAgentPackage);
                        Slog.e(str2, stringBuilder3.toString());
                        isSuccessfullyInstalled = false;
                    }
                    if (isSuccessfullyInstalled) {
                        long sharedBackupAgentTimeoutMillis;
                        long toCopy = info3.size;
                        boolean isSharedStorage = pkg2.equals(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
                        if (isSharedStorage) {
                            sharedBackupAgentTimeoutMillis = this.mAgentTimeoutParameters.getSharedBackupAgentTimeoutMillis();
                        } else {
                            sharedBackupAgentTimeoutMillis = this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis();
                        }
                        boolean agentSuccess2;
                        long toCopy2;
                        String pkg4;
                        try {
                            this.mBackupManagerService.prepareOperationTimeout(token, sharedBackupAgentTimeoutMillis, this.mMonitorTask, 1);
                            if ("obb".equals(info3.domain)) {
                                try {
                                    str2 = BackupManagerService.TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Restoring OBB file for ");
                                    stringBuilder3.append(pkg2);
                                    stringBuilder3.append(" : ");
                                    stringBuilder3.append(info3.path);
                                    Slog.d(str2, stringBuilder3.toString());
                                    okay = isSuccessfullyInstalled;
                                    try {
                                        agentSuccess2 = true;
                                    } catch (IOException e10) {
                                        agentSuccess2 = true;
                                        toCopy2 = toCopy;
                                        z2 = isSharedStorage;
                                        bytesReadListener2 = bytesReadListener;
                                        tarBackupReader = tarBackupReader3;
                                        pkg4 = pkg2;
                                        try {
                                            Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                                            agentSuccess = false;
                                            okay2 = false;
                                            isSuccessfullyInstalled = okay2;
                                            if (isSuccessfullyInstalled) {
                                            }
                                            if (agentSuccess) {
                                            }
                                            okay = isSuccessfullyInstalled;
                                            if (!okay) {
                                            }
                                            iBackupManagerMonitor = monitor;
                                            info = info3;
                                        } catch (IOException e11) {
                                            e = e11;
                                            i = token;
                                            tarBackupReader2 = tarBackupReader;
                                            packageInfo = onlyPackage;
                                            iBackupManagerMonitor = monitor;
                                            str = BackupManagerService.TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("io exception on restore socket read: ");
                                            stringBuilder.append(e.getMessage());
                                            Slog.w(str, stringBuilder.toString());
                                            setResult(-3);
                                            info = null;
                                            info2 = info;
                                            if (info2 != null) {
                                            }
                                            if (info2 == null) {
                                            }
                                            return info2 == null ? true : z;
                                        }
                                        info2 = info;
                                        if (info2 != null) {
                                        }
                                        if (info2 == null) {
                                        }
                                        return info2 == null ? true : z;
                                    } catch (RemoteException e12) {
                                        agentSuccess2 = true;
                                        toCopy2 = toCopy;
                                        z2 = isSharedStorage;
                                        bytesReadListener2 = bytesReadListener;
                                        tarBackupReader = tarBackupReader3;
                                        pkg4 = pkg2;
                                        try {
                                            Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                            agentSuccess = false;
                                            okay2 = false;
                                            isSuccessfullyInstalled = okay2;
                                            if (isSuccessfullyInstalled) {
                                            }
                                            if (agentSuccess) {
                                            }
                                            okay = isSuccessfullyInstalled;
                                            if (okay) {
                                            }
                                            iBackupManagerMonitor = monitor;
                                            info = info3;
                                        } catch (IOException e13) {
                                            e = e13;
                                            i = token;
                                            isSharedStorage = tarBackupReader;
                                        }
                                        info2 = info;
                                        if (info2 != null) {
                                        }
                                        if (info2 == null) {
                                        }
                                        return info2 == null ? true : z;
                                    }
                                } catch (IOException e14) {
                                    okay = isSuccessfullyInstalled;
                                    agentSuccess2 = true;
                                    toCopy2 = toCopy;
                                    z2 = isSharedStorage;
                                    bytesReadListener2 = bytesReadListener;
                                    tarBackupReader = tarBackupReader3;
                                    pkg4 = pkg2;
                                    Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                                    agentSuccess = false;
                                    okay2 = false;
                                    isSuccessfullyInstalled = okay2;
                                    if (isSuccessfullyInstalled) {
                                    }
                                    if (agentSuccess) {
                                    }
                                    okay = isSuccessfullyInstalled;
                                    if (okay) {
                                    }
                                    iBackupManagerMonitor = monitor;
                                    info = info3;
                                    info2 = info;
                                    if (info2 != null) {
                                    }
                                    if (info2 == null) {
                                    }
                                    return info2 == null ? true : z;
                                } catch (RemoteException e15) {
                                    okay = isSuccessfullyInstalled;
                                    agentSuccess2 = true;
                                    toCopy2 = toCopy;
                                    z2 = isSharedStorage;
                                    bytesReadListener2 = bytesReadListener;
                                    tarBackupReader = tarBackupReader3;
                                    pkg4 = pkg2;
                                    Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                    agentSuccess = false;
                                    okay2 = false;
                                    isSuccessfullyInstalled = okay2;
                                    if (isSuccessfullyInstalled) {
                                    }
                                    if (agentSuccess) {
                                    }
                                    okay = isSuccessfullyInstalled;
                                    if (okay) {
                                    }
                                    iBackupManagerMonitor = monitor;
                                    info = info3;
                                    info2 = info;
                                    if (info2 != null) {
                                    }
                                    if (info2 == null) {
                                    }
                                    return info2 == null ? true : z;
                                }
                                try {
                                    toCopy2 = toCopy;
                                    try {
                                        bytesReadListener2 = bytesReadListener;
                                        tarBackupReader = tarBackupReader3;
                                        try {
                                            try {
                                                this.mObbConnection.restoreObbFile(pkg2, this.mPipes[0], info3.size, info3.type, info3.path, info3.mode, info3.mtime, token, this.mBackupManagerService.getBackupManagerBinder());
                                            } catch (IOException e16) {
                                                pkg4 = pkg2;
                                            } catch (RemoteException e17) {
                                                pkg4 = pkg2;
                                                Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                                agentSuccess = false;
                                                okay2 = false;
                                                isSuccessfullyInstalled = okay2;
                                                if (isSuccessfullyInstalled) {
                                                }
                                                if (agentSuccess) {
                                                }
                                                okay = isSuccessfullyInstalled;
                                                if (okay) {
                                                }
                                                iBackupManagerMonitor = monitor;
                                                info = info3;
                                                info2 = info;
                                                if (info2 != null) {
                                                }
                                                if (info2 == null) {
                                                }
                                                return info2 == null ? true : z;
                                            }
                                        } catch (IOException e18) {
                                            z2 = isSharedStorage;
                                            pkg4 = pkg2;
                                            Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                                            agentSuccess = false;
                                            okay2 = false;
                                            isSuccessfullyInstalled = okay2;
                                            if (isSuccessfullyInstalled) {
                                            }
                                            if (agentSuccess) {
                                            }
                                            okay = isSuccessfullyInstalled;
                                            if (okay) {
                                            }
                                            iBackupManagerMonitor = monitor;
                                            info = info3;
                                            info2 = info;
                                            if (info2 != null) {
                                            }
                                            if (info2 == null) {
                                            }
                                            return info2 == null ? true : z;
                                        } catch (RemoteException e19) {
                                            z2 = isSharedStorage;
                                            pkg4 = pkg2;
                                            Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                            agentSuccess = false;
                                            okay2 = false;
                                            isSuccessfullyInstalled = okay2;
                                            if (isSuccessfullyInstalled) {
                                            }
                                            if (agentSuccess) {
                                            }
                                            okay = isSuccessfullyInstalled;
                                            if (okay) {
                                            }
                                            iBackupManagerMonitor = monitor;
                                            info = info3;
                                            info2 = info;
                                            if (info2 != null) {
                                            }
                                            if (info2 == null) {
                                            }
                                            return info2 == null ? true : z;
                                        }
                                    } catch (IOException e20) {
                                        z2 = isSharedStorage;
                                        bytesReadListener2 = bytesReadListener;
                                        tarBackupReader = tarBackupReader3;
                                        pkg4 = pkg2;
                                        Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                                        agentSuccess = false;
                                        okay2 = false;
                                        isSuccessfullyInstalled = okay2;
                                        if (isSuccessfullyInstalled) {
                                        }
                                        if (agentSuccess) {
                                        }
                                        okay = isSuccessfullyInstalled;
                                        if (okay) {
                                        }
                                        iBackupManagerMonitor = monitor;
                                        info = info3;
                                        info2 = info;
                                        if (info2 != null) {
                                        }
                                        if (info2 == null) {
                                        }
                                        return info2 == null ? true : z;
                                    } catch (RemoteException e21) {
                                        z2 = isSharedStorage;
                                        bytesReadListener2 = bytesReadListener;
                                        tarBackupReader = tarBackupReader3;
                                        pkg4 = pkg2;
                                        Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                        agentSuccess = false;
                                        okay2 = false;
                                        isSuccessfullyInstalled = okay2;
                                        if (isSuccessfullyInstalled) {
                                        }
                                        if (agentSuccess) {
                                        }
                                        okay = isSuccessfullyInstalled;
                                        if (okay) {
                                        }
                                        iBackupManagerMonitor = monitor;
                                        info = info3;
                                        info2 = info;
                                        if (info2 != null) {
                                        }
                                        if (info2 == null) {
                                        }
                                        return info2 == null ? true : z;
                                    }
                                } catch (IOException e22) {
                                    toCopy2 = toCopy;
                                    z2 = isSharedStorage;
                                    bytesReadListener2 = bytesReadListener;
                                    tarBackupReader = tarBackupReader3;
                                    pkg4 = pkg2;
                                    Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                                    agentSuccess = false;
                                    okay2 = false;
                                    isSuccessfullyInstalled = okay2;
                                    if (isSuccessfullyInstalled) {
                                    }
                                    if (agentSuccess) {
                                    }
                                    okay = isSuccessfullyInstalled;
                                    if (okay) {
                                    }
                                    iBackupManagerMonitor = monitor;
                                    info = info3;
                                    info2 = info;
                                    if (info2 != null) {
                                    }
                                    if (info2 == null) {
                                    }
                                    return info2 == null ? true : z;
                                } catch (RemoteException e23) {
                                    toCopy2 = toCopy;
                                    z2 = isSharedStorage;
                                    bytesReadListener2 = bytesReadListener;
                                    tarBackupReader = tarBackupReader3;
                                    pkg4 = pkg2;
                                    Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                    agentSuccess = false;
                                    okay2 = false;
                                    isSuccessfullyInstalled = okay2;
                                    if (isSuccessfullyInstalled) {
                                    }
                                    if (agentSuccess) {
                                    }
                                    okay = isSuccessfullyInstalled;
                                    if (okay) {
                                    }
                                    iBackupManagerMonitor = monitor;
                                    info = info3;
                                    info2 = info;
                                    if (info2 != null) {
                                    }
                                    if (info2 == null) {
                                    }
                                    return info2 == null ? true : z;
                                }
                            }
                            okay = isSuccessfullyInstalled;
                            agentSuccess2 = true;
                            toCopy2 = toCopy;
                            z2 = isSharedStorage;
                            bytesReadListener2 = bytesReadListener;
                            tarBackupReader = tarBackupReader3;
                            try {
                                if ("k".equals(info3.domain)) {
                                    str2 = BackupManagerService.TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Restoring key-value file for ");
                                    stringBuilder2.append(pkg2);
                                    stringBuilder2.append(" : ");
                                    stringBuilder2.append(info3.path);
                                    Slog.d(str2, stringBuilder2.toString());
                                    new Thread(new KeyValueAdbRestoreEngine(this.mBackupManagerService, this.mBackupManagerService.getDataDir(), info3, this.mPipes[0], this.mAgent, token), "restore-key-value-runner").start();
                                } else if (this.mTargetApp.processName.equals("system")) {
                                    Slog.d(BackupManagerService.TAG, "system process agent - spinning a thread");
                                    new Thread(new RestoreFileRunnable(this.mBackupManagerService, this.mAgent, info3, this.mPipes[0], token), "restore-sys-runner").start();
                                } else {
                                    pkg4 = pkg2;
                                    try {
                                        this.mAgent.doRestoreFile(this.mPipes[0], info3.size, info3.type, info3.domain, info3.path, info3.mode, info3.mtime, token, this.mBackupManagerService.getBackupManagerBinder());
                                        isSuccessfullyInstalled = okay;
                                        agentSuccess = agentSuccess2;
                                    } catch (IOException e24) {
                                        Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                                        agentSuccess = false;
                                        okay2 = false;
                                        isSuccessfullyInstalled = okay2;
                                        if (isSuccessfullyInstalled) {
                                        }
                                        if (agentSuccess) {
                                        }
                                        okay = isSuccessfullyInstalled;
                                        if (okay) {
                                        }
                                        iBackupManagerMonitor = monitor;
                                        info = info3;
                                        info2 = info;
                                        if (info2 != null) {
                                        }
                                        if (info2 == null) {
                                        }
                                        return info2 == null ? true : z;
                                    } catch (RemoteException e25) {
                                        Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                        agentSuccess = false;
                                        okay2 = false;
                                        isSuccessfullyInstalled = okay2;
                                        if (isSuccessfullyInstalled) {
                                        }
                                        if (agentSuccess) {
                                        }
                                        okay = isSuccessfullyInstalled;
                                        if (okay) {
                                        }
                                        iBackupManagerMonitor = monitor;
                                        info = info3;
                                        info2 = info;
                                        if (info2 != null) {
                                        }
                                        if (info2 == null) {
                                        }
                                        return info2 == null ? true : z;
                                    }
                                    long toCopy3;
                                    if (isSuccessfullyInstalled) {
                                        FileOutputStream pipe = new FileOutputStream(this.mPipes[1].getFileDescriptor());
                                        boolean pipeOkay = true;
                                        long toCopy4 = toCopy2;
                                        while (toCopy4 > 0) {
                                            pkg2 = inputStream.read(bArr, 0, toCopy4 > ((long) bArr.length) ? bArr.length : (int) toCopy4);
                                            if (pkg2 >= null) {
                                                toCopy3 = toCopy4;
                                                this.mBytes += (long) pkg2;
                                            } else {
                                                toCopy3 = toCopy4;
                                            }
                                            if (pkg2 > null) {
                                                toCopy = toCopy3 - ((long) pkg2);
                                                if (pipeOkay) {
                                                    try {
                                                        pipe.write(bArr, 0, pkg2);
                                                    } catch (IOException e26) {
                                                        IOException iOException = e26;
                                                        String str3 = BackupManagerService.TAG;
                                                        StringBuilder stringBuilder4 = new StringBuilder();
                                                        stringBuilder4.append("Failed to write to restore pipe: ");
                                                        stringBuilder4.append(e26.getMessage());
                                                        Slog.e(str3, stringBuilder4.toString());
                                                        pipeOkay = false;
                                                    }
                                                }
                                                toCopy4 = toCopy;
                                            }
                                            tarBackupReader.skipTarPadding(info3.size);
                                            try {
                                                agentSuccess = this.mBackupManagerService.waitUntilOperationComplete(token);
                                            } catch (IOException e27) {
                                                e26 = e27;
                                                packageInfo = onlyPackage;
                                                str = BackupManagerService.TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("io exception on restore socket read: ");
                                                stringBuilder.append(e26.getMessage());
                                                Slog.w(str, stringBuilder.toString());
                                                setResult(-3);
                                                info = null;
                                                info2 = info;
                                                if (info2 != null) {
                                                }
                                                if (info2 == null) {
                                                }
                                                return info2 == null ? true : z;
                                            }
                                        }
                                        toCopy3 = toCopy4;
                                        try {
                                            tarBackupReader.skipTarPadding(info3.size);
                                            agentSuccess = this.mBackupManagerService.waitUntilOperationComplete(token);
                                        } catch (IOException e28) {
                                            e26 = e28;
                                            i = token;
                                            packageInfo = onlyPackage;
                                            str = BackupManagerService.TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("io exception on restore socket read: ");
                                            stringBuilder.append(e26.getMessage());
                                            Slog.w(str, stringBuilder.toString());
                                            setResult(-3);
                                            info = null;
                                            info2 = info;
                                            if (info2 != null) {
                                            }
                                            if (info2 == null) {
                                            }
                                            return info2 == null ? true : z;
                                        }
                                    }
                                    i = token;
                                    tarBackupReader2 = tarBackupReader;
                                    toCopy3 = toCopy2;
                                    if (agentSuccess) {
                                        str2 = BackupManagerService.TAG;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("Agent failure restoring ");
                                        pkg3 = pkg4;
                                        stringBuilder3.append(pkg3);
                                        stringBuilder3.append("; ending restore");
                                        Slog.w(str2, stringBuilder3.toString());
                                        this.mBackupManagerService.getBackupHandler().removeMessages(18);
                                        tearDownPipes();
                                        tearDownAgent(this.mTargetApp);
                                        this.mAgent = null;
                                        this.mPackagePolicies.put(pkg3, RestorePolicy.IGNORE);
                                        if (onlyPackage != null) {
                                            try {
                                                setResult(-2);
                                                setRunning(false);
                                                return false;
                                            } catch (IOException e29) {
                                                e26 = e29;
                                                str = BackupManagerService.TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("io exception on restore socket read: ");
                                                stringBuilder.append(e26.getMessage());
                                                Slog.w(str, stringBuilder.toString());
                                                setResult(-3);
                                                info = null;
                                                info2 = info;
                                                if (info2 != null) {
                                                }
                                                if (info2 == null) {
                                                }
                                                return info2 == null ? true : z;
                                            }
                                        }
                                    }
                                    pkg3 = pkg4;
                                    packageInfo = onlyPackage;
                                    okay = isSuccessfullyInstalled;
                                }
                            } catch (IOException e30) {
                                pkg4 = pkg2;
                                Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                                agentSuccess = false;
                                okay2 = false;
                                isSuccessfullyInstalled = okay2;
                                if (isSuccessfullyInstalled) {
                                }
                                if (agentSuccess) {
                                }
                                okay = isSuccessfullyInstalled;
                                if (okay) {
                                }
                                iBackupManagerMonitor = monitor;
                                info = info3;
                                info2 = info;
                                if (info2 != null) {
                                }
                                if (info2 == null) {
                                }
                                return info2 == null ? true : z;
                            } catch (RemoteException e31) {
                                pkg4 = pkg2;
                                Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                agentSuccess = false;
                                okay2 = false;
                                isSuccessfullyInstalled = okay2;
                                if (isSuccessfullyInstalled) {
                                }
                                if (agentSuccess) {
                                }
                                okay = isSuccessfullyInstalled;
                                if (okay) {
                                }
                                iBackupManagerMonitor = monitor;
                                info = info3;
                                info2 = info;
                                if (info2 != null) {
                                }
                                if (info2 == null) {
                                }
                                return info2 == null ? true : z;
                            }
                            pkg4 = pkg2;
                            isSuccessfullyInstalled = okay;
                            agentSuccess = agentSuccess2;
                        } catch (IOException e32) {
                            okay = isSuccessfullyInstalled;
                            agentSuccess2 = true;
                            toCopy2 = toCopy;
                            z2 = isSharedStorage;
                            pkg4 = pkg2;
                            bytesReadListener2 = bytesReadListener;
                            tarBackupReader = tarBackupReader3;
                            Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                            agentSuccess = false;
                            okay2 = false;
                            isSuccessfullyInstalled = okay2;
                            if (isSuccessfullyInstalled) {
                            }
                            if (agentSuccess) {
                            }
                            okay = isSuccessfullyInstalled;
                            if (okay) {
                            }
                            iBackupManagerMonitor = monitor;
                            info = info3;
                            info2 = info;
                            if (info2 != null) {
                            }
                            if (info2 == null) {
                            }
                            return info2 == null ? true : z;
                        } catch (RemoteException e33) {
                            okay = isSuccessfullyInstalled;
                            agentSuccess2 = true;
                            toCopy2 = toCopy;
                            z2 = isSharedStorage;
                            pkg4 = pkg2;
                            bytesReadListener2 = bytesReadListener;
                            tarBackupReader = tarBackupReader3;
                            Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                            agentSuccess = false;
                            okay2 = false;
                            isSuccessfullyInstalled = okay2;
                            if (isSuccessfullyInstalled) {
                            }
                            if (agentSuccess) {
                            }
                            okay = isSuccessfullyInstalled;
                            if (okay) {
                            }
                            iBackupManagerMonitor = monitor;
                            info = info3;
                            info2 = info;
                            if (info2 != null) {
                            }
                            if (info2 == null) {
                            }
                            return info2 == null ? true : z;
                        }
                        if (isSuccessfullyInstalled) {
                        }
                        if (agentSuccess) {
                        }
                        okay = isSuccessfullyInstalled;
                    } else {
                        i = token;
                        okay = isSuccessfullyInstalled;
                        packageInfo = packageInfo2;
                        pkg3 = pkg2;
                        bytesReadListener2 = bytesReadListener;
                        tarBackupReader2 = tarBackupReader3;
                    }
                    if (okay) {
                        long bytesToConsume = (info3.size + 511) & -512;
                        while (bytesToConsume > 0) {
                            String pkg5;
                            bytesReadListener = (long) inputStream.read(bArr, 0, bytesToConsume > ((long) bArr.length) ? bArr.length : (int) bytesToConsume);
                            if (bytesReadListener >= 0) {
                                pkg5 = pkg3;
                                this.mBytes += bytesReadListener;
                            } else {
                                pkg5 = pkg3;
                            }
                            if (bytesReadListener > 0) {
                                bytesToConsume -= bytesReadListener;
                                pkg3 = pkg5;
                            }
                        }
                    }
                } else {
                    i = token;
                    info3 = info4;
                    packageInfo = packageInfo2;
                    bytesReadListener2 = bytesReadListener;
                }
                iBackupManagerMonitor = monitor;
                info = info3;
            } catch (IOException e34) {
                e26 = e34;
                i = token;
                packageInfo = packageInfo2;
                bytesReadListener2 = bytesReadListener;
                iBackupManagerMonitor = monitor;
                str = BackupManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("io exception on restore socket read: ");
                stringBuilder.append(e26.getMessage());
                Slog.w(str, stringBuilder.toString());
                setResult(-3);
                info = null;
                info2 = info;
                if (info2 != null) {
                }
                if (info2 == null) {
                }
                return info2 == null ? true : z;
            }
            info2 = info;
            if (info2 != null) {
            }
            if (info2 == null) {
            }
            return info2 == null ? true : z;
        }
        Slog.w(BackupManagerService.TAG, "Restore engine used after halting");
        return false;
    }

    private void setUpPipes() throws IOException {
        this.mPipes = ParcelFileDescriptor.createPipe();
    }

    private void tearDownPipes() {
        synchronized (this) {
            if (this.mPipes != null) {
                try {
                    this.mPipes[0].close();
                    this.mPipes[0] = null;
                    this.mPipes[1].close();
                    this.mPipes[1] = null;
                } catch (IOException e) {
                    Slog.w(BackupManagerService.TAG, "Couldn't close agent pipes", e);
                }
                this.mPipes = null;
            }
        }
    }

    private void tearDownAgent(ApplicationInfo app) {
        if (this.mAgent != null) {
            this.mBackupManagerService.tearDownAgentAndKill(app);
            this.mAgent = null;
        }
    }

    void handleTimeout() {
        tearDownPipes();
        setResult(-2);
        setRunning(false);
    }

    private static boolean isRestorableFile(FileMetadata info) {
        if ("c".equals(info.domain)) {
            return false;
        }
        if ("r".equals(info.domain) && info.path.startsWith("no_backup/")) {
            return false;
        }
        return true;
    }

    private static boolean isCanonicalFilePath(String path) {
        if (path.contains("..") || path.contains("//")) {
            return false;
        }
        return true;
    }

    private boolean shouldForceClearAppDataOnFullRestore(String packageName) {
        String packageListString = Secure.getString(this.mBackupManagerService.getContext().getContentResolver(), "packages_to_clear_data_before_full_restore");
        if (TextUtils.isEmpty(packageListString)) {
            return false;
        }
        return Arrays.asList(packageListString.split(";")).contains(packageName);
    }

    void sendOnRestorePackage(String name) {
        if (this.mObserver != null) {
            try {
                this.mObserver.onRestorePackage(name);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: restorePackage");
                this.mObserver = null;
            }
        }
    }
}
