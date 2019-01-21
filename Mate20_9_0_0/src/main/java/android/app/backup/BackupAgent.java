package android.app.backup;

import android.app.IBackupAgent.Stub;
import android.app.QueuedWork;
import android.app.backup.FullBackup.BackupScheme;
import android.app.backup.FullBackup.BackupScheme.PathWithRequiredFlags;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.util.ArraySet;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;

public abstract class BackupAgent extends ContextWrapper {
    private static final boolean DEBUG = false;
    public static final int FLAG_CLIENT_SIDE_ENCRYPTION_ENABLED = 1;
    public static final int FLAG_DEVICE_TO_DEVICE_TRANSFER = 2;
    public static final int FLAG_FAKE_CLIENT_SIDE_ENCRYPTION_ENABLED = Integer.MIN_VALUE;
    private static final String TAG = "BackupAgent";
    public static final int TYPE_DIRECTORY = 2;
    public static final int TYPE_EOF = 0;
    public static final int TYPE_FILE = 1;
    public static final int TYPE_SYMLINK = 3;
    private final IBinder mBinder = new BackupServiceBinder().asBinder();
    Handler mHandler = null;

    static class FailRunnable implements Runnable {
        private String mMessage;

        FailRunnable(String message) {
            this.mMessage = message;
        }

        public void run() {
            throw new IllegalStateException(this.mMessage);
        }
    }

    class SharedPrefsSynchronizer implements Runnable {
        public final CountDownLatch mLatch = new CountDownLatch(1);

        SharedPrefsSynchronizer() {
        }

        public void run() {
            QueuedWork.waitToFinish();
            this.mLatch.countDown();
        }
    }

    private class BackupServiceBinder extends Stub {
        private static final String TAG = "BackupServiceBinder";

        private BackupServiceBinder() {
        }

        /* JADX WARNING: Removed duplicated region for block: B:34:0x00c7  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void doBackup(ParcelFileDescriptor oldState, ParcelFileDescriptor data, ParcelFileDescriptor newState, long quotaBytes, int token, IBackupManager callbackBinder, int transportFlags) throws RemoteException {
            IOException ex;
            String str;
            StringBuilder stringBuilder;
            RuntimeException ex2;
            String str2;
            StringBuilder stringBuilder2;
            ParcelFileDescriptor parcelFileDescriptor;
            ParcelFileDescriptor parcelFileDescriptor2;
            Throwable th;
            Throwable th2;
            int i = token;
            IBackupManager iBackupManager = callbackBinder;
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    BackupAgent.this.onBackup(oldState, new BackupDataOutput(data.getFileDescriptor(), quotaBytes, transportFlags), newState);
                    BackupAgent.this.waitForSharedPrefs();
                    Binder.restoreCallingIdentity(ident);
                    try {
                        iBackupManager.opComplete(i, 0);
                    } catch (RemoteException e) {
                    }
                    if (Binder.getCallingPid() != Process.myPid()) {
                        IoUtils.closeQuietly(oldState);
                        IoUtils.closeQuietly(data);
                        IoUtils.closeQuietly(newState);
                    }
                } catch (IOException e2) {
                    ex = e2;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onBackup (");
                    stringBuilder.append(BackupAgent.this.getClass().getName());
                    stringBuilder.append(") threw");
                    Log.d(str, stringBuilder.toString(), ex);
                    throw new RuntimeException(ex);
                } catch (RuntimeException e3) {
                    ex2 = e3;
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onBackup (");
                    stringBuilder2.append(BackupAgent.this.getClass().getName());
                    stringBuilder2.append(") threw");
                    Log.d(str2, stringBuilder2.toString(), ex2);
                    throw ex2;
                }
            } catch (IOException e4) {
                ex = e4;
                parcelFileDescriptor = oldState;
                parcelFileDescriptor2 = newState;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onBackup (");
                stringBuilder.append(BackupAgent.this.getClass().getName());
                stringBuilder.append(") threw");
                Log.d(str, stringBuilder.toString(), ex);
                throw new RuntimeException(ex);
            } catch (RuntimeException e5) {
                ex2 = e5;
                parcelFileDescriptor = oldState;
                parcelFileDescriptor2 = newState;
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onBackup (");
                stringBuilder2.append(BackupAgent.this.getClass().getName());
                stringBuilder2.append(") threw");
                Log.d(str2, stringBuilder2.toString(), ex2);
                throw ex2;
            } catch (Throwable th3) {
                th = th3;
                th2 = th;
                BackupAgent.this.waitForSharedPrefs();
                Binder.restoreCallingIdentity(ident);
                iBackupManager.opComplete(i, 0);
                if (Binder.getCallingPid() != Process.myPid()) {
                }
                throw th2;
            }
        }

        public void doRestore(ParcelFileDescriptor data, long appVersionCode, ParcelFileDescriptor newState, int token, IBackupManager callbackBinder) throws RemoteException {
            String str;
            StringBuilder stringBuilder;
            long ident = Binder.clearCallingIdentity();
            BackupAgent.this.waitForSharedPrefs();
            try {
                BackupAgent.this.onRestore(new BackupDataInput(data.getFileDescriptor()), appVersionCode, newState);
                BackupAgent.this.reloadSharedPreferences();
                Binder.restoreCallingIdentity(ident);
                try {
                    callbackBinder.opComplete(token, 0);
                } catch (RemoteException e) {
                }
                if (Binder.getCallingPid() != Process.myPid()) {
                    IoUtils.closeQuietly(data);
                    IoUtils.closeQuietly(newState);
                }
            } catch (IOException ex) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onRestore (");
                stringBuilder.append(BackupAgent.this.getClass().getName());
                stringBuilder.append(") threw");
                Log.d(str, stringBuilder.toString(), ex);
                throw new RuntimeException(ex);
            } catch (RuntimeException ex2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onRestore (");
                stringBuilder.append(BackupAgent.this.getClass().getName());
                stringBuilder.append(") threw");
                Log.d(str, stringBuilder.toString(), ex2);
                throw ex2;
            } catch (Throwable th) {
                BackupAgent.this.reloadSharedPreferences();
                Binder.restoreCallingIdentity(ident);
                try {
                    callbackBinder.opComplete(token, 0);
                } catch (RemoteException e2) {
                }
                if (Binder.getCallingPid() != Process.myPid()) {
                    IoUtils.closeQuietly(data);
                    IoUtils.closeQuietly(newState);
                }
            }
        }

        public void doFullBackup(ParcelFileDescriptor data, long quotaBytes, int token, IBackupManager callbackBinder, int transportFlags) {
            String str;
            StringBuilder stringBuilder;
            long ident = Binder.clearCallingIdentity();
            BackupAgent.this.waitForSharedPrefs();
            try {
                BackupAgent.this.onFullBackup(new FullBackupDataOutput(data, quotaBytes, transportFlags));
                BackupAgent.this.waitForSharedPrefs();
                try {
                    new FileOutputStream(data.getFileDescriptor()).write(new byte[4]);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to finalize backup stream!");
                }
                Binder.restoreCallingIdentity(ident);
                try {
                    callbackBinder.opComplete(token, 0);
                } catch (RemoteException e2) {
                }
                if (Binder.getCallingPid() != Process.myPid()) {
                    IoUtils.closeQuietly(data);
                }
            } catch (IOException ex) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onFullBackup (");
                stringBuilder.append(BackupAgent.this.getClass().getName());
                stringBuilder.append(") threw");
                Log.d(str, stringBuilder.toString(), ex);
                throw new RuntimeException(ex);
            } catch (RuntimeException ex2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onFullBackup (");
                stringBuilder.append(BackupAgent.this.getClass().getName());
                stringBuilder.append(") threw");
                Log.d(str, stringBuilder.toString(), ex2);
                throw ex2;
            } catch (Throwable th) {
                BackupAgent.this.waitForSharedPrefs();
                try {
                    new FileOutputStream(data.getFileDescriptor()).write(new byte[4]);
                } catch (IOException e3) {
                    Log.e(TAG, "Unable to finalize backup stream!");
                }
                Binder.restoreCallingIdentity(ident);
                try {
                    callbackBinder.opComplete(token, 0);
                } catch (RemoteException e4) {
                }
                if (Binder.getCallingPid() != Process.myPid()) {
                    IoUtils.closeQuietly(data);
                }
            }
        }

        public void doMeasureFullBackup(long quotaBytes, int token, IBackupManager callbackBinder, int transportFlags) {
            String str;
            StringBuilder stringBuilder;
            long ident = Binder.clearCallingIdentity();
            FullBackupDataOutput measureOutput = new FullBackupDataOutput(quotaBytes, transportFlags);
            BackupAgent.this.waitForSharedPrefs();
            try {
                BackupAgent.this.onFullBackup(measureOutput);
                Binder.restoreCallingIdentity(ident);
                try {
                    callbackBinder.opComplete(token, measureOutput.getSize());
                } catch (RemoteException e) {
                }
            } catch (IOException ex) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onFullBackup[M] (");
                stringBuilder.append(BackupAgent.this.getClass().getName());
                stringBuilder.append(") threw");
                Log.d(str, stringBuilder.toString(), ex);
                throw new RuntimeException(ex);
            } catch (RuntimeException ex2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onFullBackup[M] (");
                stringBuilder.append(BackupAgent.this.getClass().getName());
                stringBuilder.append(") threw");
                Log.d(str, stringBuilder.toString(), ex2);
                throw ex2;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                try {
                    callbackBinder.opComplete(token, measureOutput.getSize());
                } catch (RemoteException e2) {
                }
            }
        }

        public void doRestoreFile(ParcelFileDescriptor data, long size, int type, String domain, String path, long mode, long mtime, int token, IBackupManager callbackBinder) throws RemoteException {
            int i = token;
            IBackupManager iBackupManager = callbackBinder;
            long ident = Binder.clearCallingIdentity();
            try {
                BackupAgent.this.onRestoreFile(data, size, type, domain, path, mode, mtime);
                BackupAgent.this.waitForSharedPrefs();
                BackupAgent.this.reloadSharedPreferences();
                Binder.restoreCallingIdentity(ident);
                try {
                    iBackupManager.opComplete(i, 0);
                } catch (RemoteException e) {
                }
                if (Binder.getCallingPid() != Process.myPid()) {
                    IoUtils.closeQuietly(data);
                }
            } catch (IOException e2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onRestoreFile (");
                stringBuilder.append(BackupAgent.this.getClass().getName());
                stringBuilder.append(") threw");
                Log.d(str, stringBuilder.toString(), e2);
                throw new RuntimeException(e2);
            } catch (Throwable th) {
                Throwable th2 = th;
                BackupAgent.this.waitForSharedPrefs();
                BackupAgent.this.reloadSharedPreferences();
                Binder.restoreCallingIdentity(ident);
                try {
                    iBackupManager.opComplete(i, 0);
                } catch (RemoteException e3) {
                }
                if (Binder.getCallingPid() != Process.myPid()) {
                    IoUtils.closeQuietly(data);
                }
            }
        }

        public void doRestoreFinished(int token, IBackupManager callbackBinder) {
            long ident = Binder.clearCallingIdentity();
            try {
                BackupAgent.this.onRestoreFinished();
                BackupAgent.this.waitForSharedPrefs();
                Binder.restoreCallingIdentity(ident);
                try {
                    callbackBinder.opComplete(token, 0);
                } catch (RemoteException e) {
                }
            } catch (Exception e2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onRestoreFinished (");
                stringBuilder.append(BackupAgent.this.getClass().getName());
                stringBuilder.append(") threw");
                Log.d(str, stringBuilder.toString(), e2);
                throw e2;
            } catch (Throwable th) {
                BackupAgent.this.waitForSharedPrefs();
                Binder.restoreCallingIdentity(ident);
                try {
                    callbackBinder.opComplete(token, 0);
                } catch (RemoteException e3) {
                }
            }
        }

        public void fail(String message) {
            BackupAgent.this.getHandler().post(new FailRunnable(message));
        }

        public void doQuotaExceeded(long backupDataBytes, long quotaBytes) {
            long ident = Binder.clearCallingIdentity();
            try {
                BackupAgent.this.onQuotaExceeded(backupDataBytes, quotaBytes);
                BackupAgent.this.waitForSharedPrefs();
                Binder.restoreCallingIdentity(ident);
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onQuotaExceeded(");
                stringBuilder.append(BackupAgent.this.getClass().getName());
                stringBuilder.append(") threw");
                Log.d(str, stringBuilder.toString(), e);
                throw e;
            } catch (Throwable th) {
                BackupAgent.this.waitForSharedPrefs();
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public abstract void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException;

    public abstract void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException;

    Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler(Looper.getMainLooper());
        }
        return this.mHandler;
    }

    private void waitForSharedPrefs() {
        Handler h = getHandler();
        SharedPrefsSynchronizer s = new SharedPrefsSynchronizer();
        h.postAtFrontOfQueue(s);
        try {
            s.mLatch.await();
        } catch (InterruptedException e) {
        }
    }

    public BackupAgent() {
        super(null);
    }

    public void onCreate() {
    }

    public void onDestroy() {
    }

    public void onRestore(BackupDataInput data, long appVersionCode, ParcelFileDescriptor newState) throws IOException {
        onRestore(data, (int) appVersionCode, newState);
    }

    public void onFullBackup(FullBackupDataOutput data) throws IOException {
        BackupScheme backupScheme = FullBackup.getBackupScheme(this);
        if (backupScheme.isFullBackupContentEnabled()) {
            try {
                String libDir;
                Map<String, Set<PathWithRequiredFlags>> manifestIncludeMap = backupScheme.maybeParseAndGetCanonicalIncludePaths();
                ArraySet<PathWithRequiredFlags> manifestExcludeSet = backupScheme.maybeParseAndGetCanonicalExcludePaths();
                String packageName = getPackageName();
                ApplicationInfo appInfo = getApplicationInfo();
                Context ceContext = createCredentialProtectedStorageContext();
                String rootDir = ceContext.getDataDir().getCanonicalPath();
                String filesDir = ceContext.getFilesDir().getCanonicalPath();
                String noBackupDir = ceContext.getNoBackupFilesDir().getCanonicalPath();
                String databaseDir = ceContext.getDatabasePath("foo").getParentFile().getCanonicalPath();
                String sharedPrefsDir = ceContext.getSharedPreferencesPath("foo").getParentFile().getCanonicalPath();
                String cacheDir = ceContext.getCacheDir().getCanonicalPath();
                String codeCacheDir = ceContext.getCodeCacheDir().getCanonicalPath();
                Context deContext = createDeviceProtectedStorageContext();
                String deviceRootDir = deContext.getDataDir().getCanonicalPath();
                String deviceFilesDir = deContext.getFilesDir().getCanonicalPath();
                String deviceNoBackupDir = deContext.getNoBackupFilesDir().getCanonicalPath();
                String deviceRootDir2 = deviceRootDir;
                deviceRootDir = deContext.getDatabasePath("foo").getParentFile().getCanonicalPath();
                String deviceSharedPrefsDir = deContext.getSharedPreferencesPath("foo").getParentFile().getCanonicalPath();
                String rootDir2 = rootDir;
                rootDir = deContext.getCacheDir().getCanonicalPath();
                Map<String, Set<PathWithRequiredFlags>> manifestIncludeMap2 = manifestIncludeMap;
                String deviceCodeCacheDir = deContext.getCodeCacheDir().getCanonicalPath();
                Context deContext2 = deContext;
                ArraySet<PathWithRequiredFlags> manifestExcludeSet2 = manifestExcludeSet;
                if (appInfo.nativeLibraryDir != null) {
                    libDir = new File(appInfo.nativeLibraryDir).getCanonicalPath();
                } else {
                    libDir = null;
                }
                ArraySet<String> traversalExcludeSet = new ArraySet();
                traversalExcludeSet.add(filesDir);
                traversalExcludeSet.add(noBackupDir);
                traversalExcludeSet.add(databaseDir);
                traversalExcludeSet.add(sharedPrefsDir);
                traversalExcludeSet.add(cacheDir);
                traversalExcludeSet.add(codeCacheDir);
                traversalExcludeSet.add(deviceFilesDir);
                traversalExcludeSet.add(deviceNoBackupDir);
                traversalExcludeSet.add(deviceRootDir);
                traversalExcludeSet.add(deviceSharedPrefsDir);
                traversalExcludeSet.add(rootDir);
                traversalExcludeSet.add(deviceCodeCacheDir);
                if (libDir != null) {
                    traversalExcludeSet.add(libDir);
                }
                deviceNoBackupDir = deviceFilesDir;
                String deviceSharedPrefsDir2 = deviceSharedPrefsDir;
                String deviceRootDir3 = deviceRootDir2;
                deviceSharedPrefsDir = deviceRootDir;
                ArraySet<String> traversalExcludeSet2 = traversalExcludeSet;
                Map<String, Set<PathWithRequiredFlags>> map = manifestIncludeMap2;
                manifestIncludeMap = map;
                ArraySet<PathWithRequiredFlags> arraySet = manifestExcludeSet2;
                manifestExcludeSet = arraySet;
                applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.ROOT_TREE_TOKEN, manifestIncludeMap, manifestExcludeSet, traversalExcludeSet2, data);
                deviceFilesDir = rootDir2;
                ArraySet<String> traversalExcludeSet3 = traversalExcludeSet2;
                traversalExcludeSet3.add(deviceFilesDir);
                cacheDir = packageName;
                String rootDir3 = deviceFilesDir;
                deviceFilesDir = sharedPrefsDir;
                String databaseDir2 = databaseDir;
                Map<String, Set<PathWithRequiredFlags>> map2 = manifestIncludeMap;
                ArraySet<PathWithRequiredFlags> arraySet2 = manifestExcludeSet;
                String sharedPrefsDir2 = deviceFilesDir;
                deviceFilesDir = filesDir;
                ArraySet<String> arraySet3 = traversalExcludeSet3;
                FullBackupDataOutput fullBackupDataOutput = data;
                applyXmlFiltersAndDoFullBackupForDomain(cacheDir, FullBackup.DEVICE_ROOT_TREE_TOKEN, map2, arraySet2, arraySet3, fullBackupDataOutput);
                traversalExcludeSet3.add(deviceRootDir3);
                traversalExcludeSet3.remove(deviceFilesDir);
                applyXmlFiltersAndDoFullBackupForDomain(cacheDir, FullBackup.FILES_TREE_TOKEN, map2, arraySet2, arraySet3, fullBackupDataOutput);
                traversalExcludeSet3.add(deviceFilesDir);
                traversalExcludeSet3.remove(deviceNoBackupDir);
                applyXmlFiltersAndDoFullBackupForDomain(cacheDir, FullBackup.DEVICE_FILES_TREE_TOKEN, map2, arraySet2, arraySet3, fullBackupDataOutput);
                traversalExcludeSet3.add(deviceNoBackupDir);
                traversalExcludeSet3.remove(databaseDir2);
                applyXmlFiltersAndDoFullBackupForDomain(cacheDir, FullBackup.DATABASE_TREE_TOKEN, map2, arraySet2, arraySet3, fullBackupDataOutput);
                traversalExcludeSet3.add(databaseDir2);
                traversalExcludeSet3.remove(deviceSharedPrefsDir);
                applyXmlFiltersAndDoFullBackupForDomain(cacheDir, FullBackup.DEVICE_DATABASE_TREE_TOKEN, map2, arraySet2, arraySet3, fullBackupDataOutput);
                traversalExcludeSet3.add(deviceSharedPrefsDir);
                rootDir = sharedPrefsDir2;
                traversalExcludeSet3.remove(rootDir);
                deviceNoBackupDir = rootDir;
                applyXmlFiltersAndDoFullBackupForDomain(cacheDir, FullBackup.SHAREDPREFS_TREE_TOKEN, map2, arraySet2, arraySet3, data);
                traversalExcludeSet3.add(deviceNoBackupDir);
                rootDir = deviceSharedPrefsDir2;
                traversalExcludeSet3.remove(rootDir);
                deviceNoBackupDir = rootDir;
                applyXmlFiltersAndDoFullBackupForDomain(cacheDir, FullBackup.DEVICE_SHAREDPREFS_TREE_TOKEN, map2, arraySet2, arraySet3, data);
                traversalExcludeSet3.add(deviceNoBackupDir);
                if (Process.myUid() != 1000) {
                    if (getExternalFilesDir(null) != null) {
                        applyXmlFiltersAndDoFullBackupForDomain(packageName, FullBackup.MANAGED_EXTERNAL_TREE_TOKEN, manifestIncludeMap, manifestExcludeSet, traversalExcludeSet3, data);
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                BackupScheme backupScheme2 = backupScheme;
                if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                    Log.v("BackupXmlParserLogging", "Exception trying to parse fullBackupContent xml file! Aborting full backup.", e);
                }
            }
        }
    }

    public void onQuotaExceeded(long backupDataBytes, long quotaBytes) {
    }

    private void applyXmlFiltersAndDoFullBackupForDomain(String packageName, String domainToken, Map<String, Set<PathWithRequiredFlags>> includeMap, ArraySet<PathWithRequiredFlags> filterSet, ArraySet<String> traversalExcludeSet, FullBackupDataOutput data) throws IOException {
        String str = domainToken;
        Map<String, Set<PathWithRequiredFlags>> map = includeMap;
        if (map == null || map.size() == 0) {
            fullBackupFileTree(packageName, str, FullBackup.getBackupScheme(this).tokenToDirectoryPath(str), filterSet, traversalExcludeSet, data);
            return;
        }
        if (map.get(str) != null) {
            for (PathWithRequiredFlags includeFile : (Set) map.get(str)) {
                if (areIncludeRequiredTransportFlagsSatisfied(includeFile.getRequiredFlags(), data.getTransportFlags())) {
                    fullBackupFileTree(packageName, str, includeFile.getPath(), filterSet, traversalExcludeSet, data);
                }
            }
        }
    }

    private boolean areIncludeRequiredTransportFlagsSatisfied(int includeFlags, int transportFlags) {
        return (transportFlags & includeFlags) == includeFlags;
    }

    public final void fullBackupFile(File file, FullBackupDataOutput output) {
        ApplicationInfo applicationInfo;
        ApplicationInfo appInfo = getApplicationInfo();
        String efDir;
        try {
            String deviceCodeCacheDir;
            String libDir;
            String deviceCodeCacheDir2;
            String appInfo2;
            String str;
            String str2;
            String str3;
            Context ceContext = createCredentialProtectedStorageContext();
            String rootDir = ceContext.getDataDir().getCanonicalPath();
            String filesDir = ceContext.getFilesDir().getCanonicalPath();
            String nbFilesDir = ceContext.getNoBackupFilesDir().getCanonicalPath();
            String dbDir = ceContext.getDatabasePath("foo").getParentFile().getCanonicalPath();
            String spDir = ceContext.getSharedPreferencesPath("foo").getParentFile().getCanonicalPath();
            String cacheDir = ceContext.getCacheDir().getCanonicalPath();
            String codeCacheDir = ceContext.getCodeCacheDir().getCanonicalPath();
            Context deContext = createDeviceProtectedStorageContext();
            String deviceRootDir = deContext.getDataDir().getCanonicalPath();
            String deviceFilesDir = deContext.getFilesDir().getCanonicalPath();
            String deviceNbFilesDir = deContext.getNoBackupFilesDir().getCanonicalPath();
            String deviceDbDir = deContext.getDatabasePath("foo").getParentFile().getCanonicalPath();
            String deviceSpDir = deContext.getSharedPreferencesPath("foo").getParentFile().getCanonicalPath();
            String deviceCacheDir = deContext.getCacheDir().getCanonicalPath();
            efDir = null;
            try {
                String efDir2;
                deviceCodeCacheDir = deContext.getCodeCacheDir().getCanonicalPath();
                libDir = appInfo.nativeLibraryDir == null ? null : new File(appInfo.nativeLibraryDir).getCanonicalPath();
                if (Process.myUid() != 1000) {
                    try {
                        File efLocation = getExternalFilesDir(null);
                        if (efLocation != null) {
                            efDir2 = efLocation.getCanonicalPath();
                            deviceCodeCacheDir2 = deviceCodeCacheDir;
                            appInfo2 = file.getCanonicalPath();
                            if (!appInfo2.startsWith(cacheDir) || appInfo2.startsWith(codeCacheDir) || appInfo2.startsWith(nbFilesDir) || appInfo2.startsWith(deviceCacheDir)) {
                                str = libDir;
                                str2 = rootDir;
                                str3 = deviceCodeCacheDir2;
                            } else {
                                deviceCacheDir = deviceCodeCacheDir2;
                                if (appInfo2.startsWith(deviceCacheDir) || appInfo2.startsWith(deviceNbFilesDir)) {
                                    str3 = deviceCacheDir;
                                    str = libDir;
                                    str2 = rootDir;
                                } else if (appInfo2.startsWith(libDir)) {
                                    str3 = deviceCacheDir;
                                    str = libDir;
                                    str2 = rootDir;
                                } else {
                                    String rootpath;
                                    if (appInfo2.startsWith(dbDir)) {
                                        efDir = FullBackup.DATABASE_TREE_TOKEN;
                                        rootpath = dbDir;
                                    } else if (appInfo2.startsWith(spDir)) {
                                        efDir = FullBackup.SHAREDPREFS_TREE_TOKEN;
                                        rootpath = spDir;
                                    } else if (appInfo2.startsWith(filesDir)) {
                                        efDir = FullBackup.FILES_TREE_TOKEN;
                                        rootpath = filesDir;
                                    } else if (appInfo2.startsWith(rootDir)) {
                                        efDir = FullBackup.ROOT_TREE_TOKEN;
                                        rootpath = rootDir;
                                    } else if (appInfo2.startsWith(deviceDbDir)) {
                                        efDir = FullBackup.DEVICE_DATABASE_TREE_TOKEN;
                                        rootpath = deviceDbDir;
                                    } else if (appInfo2.startsWith(deviceSpDir)) {
                                        efDir = FullBackup.DEVICE_SHAREDPREFS_TREE_TOKEN;
                                        rootpath = deviceSpDir;
                                    } else if (appInfo2.startsWith(deviceFilesDir)) {
                                        efDir = FullBackup.DEVICE_FILES_TREE_TOKEN;
                                        rootpath = deviceFilesDir;
                                    } else if (appInfo2.startsWith(deviceRootDir)) {
                                        efDir = FullBackup.DEVICE_ROOT_TREE_TOKEN;
                                        rootpath = deviceRootDir;
                                    } else if (efDir2 == null || !appInfo2.startsWith(efDir2)) {
                                        str3 = deviceCacheDir;
                                        deviceCacheDir = TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("File ");
                                        stringBuilder.append(appInfo2);
                                        stringBuilder.append(" is in an unsupported location; skipping");
                                        Log.w(deviceCacheDir, stringBuilder.toString());
                                        return;
                                    } else {
                                        efDir = FullBackup.MANAGED_EXTERNAL_TREE_TOKEN;
                                        rootpath = efDir2;
                                    }
                                    FullBackup.backupToTar(getPackageName(), efDir, null, rootpath, appInfo2, output);
                                    return;
                                }
                            }
                            Log.w(TAG, "lib, cache, code_cache, and no_backup files are not backed up");
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "Unable to obtain canonical paths");
                    }
                }
                efDir2 = efDir;
            } catch (IOException e2) {
                applicationInfo = appInfo;
                Log.w(TAG, "Unable to obtain canonical paths");
            }
            try {
                deviceCodeCacheDir2 = deviceCodeCacheDir;
                appInfo2 = file.getCanonicalPath();
                if (appInfo2.startsWith(cacheDir)) {
                }
                str = libDir;
                str2 = rootDir;
                str3 = deviceCodeCacheDir2;
                Log.w(TAG, "lib, cache, code_cache, and no_backup files are not backed up");
            } catch (IOException e3) {
                Log.w(TAG, "Unable to obtain canonical paths");
            }
        } catch (IOException e4) {
            efDir = null;
            applicationInfo = appInfo;
            Log.w(TAG, "Unable to obtain canonical paths");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:51:0x00d2  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00a6  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected final void fullBackupFileTree(String packageName, String domain, String startingPath, ArraySet<PathWithRequiredFlags> manifestExcludes, ArraySet<String> systemExcludes, FullBackupDataOutput output) {
        ErrnoException e;
        StringBuilder stringBuilder;
        ArraySet<PathWithRequiredFlags> arraySet = manifestExcludes;
        ArraySet<String> arraySet2 = systemExcludes;
        String str = domain;
        String domainPath = FullBackup.getBackupScheme(this).tokenToDirectoryPath(str);
        if (domainPath != null) {
            File rootFile = new File(startingPath);
            if (rootFile.exists()) {
                LinkedList<File> scanQueue = new LinkedList();
                scanQueue.add(rootFile);
                while (scanQueue.size() > 0) {
                    File file = (File) scanQueue.remove(0);
                    try {
                        StructStat stat = Os.lstat(file.getPath());
                        if (OsConstants.S_ISREG(stat.st_mode) || OsConstants.S_ISDIR(stat.st_mode)) {
                            String filePath = file.getCanonicalPath();
                            if (arraySet != null) {
                                try {
                                    if (manifestExcludesContainFilePath(arraySet, filePath)) {
                                    }
                                } catch (IOException e2) {
                                    if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                                    }
                                } catch (ErrnoException e3) {
                                    e = e3;
                                    if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                                    }
                                }
                            }
                            if (arraySet2 == null || !arraySet2.contains(filePath)) {
                                if (OsConstants.S_ISDIR(stat.st_mode)) {
                                    File[] contents = file.listFiles();
                                    if (contents != null) {
                                        for (File add : contents) {
                                            scanQueue.add(0, add);
                                        }
                                    }
                                }
                                FullBackup.backupToTar(packageName, str, null, domainPath, filePath, output);
                            }
                        }
                    } catch (IOException e4) {
                        if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error canonicalizing path of ");
                            stringBuilder.append(file);
                            Log.v("BackupXmlParserLogging", stringBuilder.toString());
                        }
                    } catch (ErrnoException e5) {
                        e = e5;
                        if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error scanning file ");
                            stringBuilder.append(file);
                            stringBuilder.append(" : ");
                            stringBuilder.append(e);
                            Log.v("BackupXmlParserLogging", stringBuilder.toString());
                        }
                    }
                }
            }
        }
    }

    private boolean manifestExcludesContainFilePath(ArraySet<PathWithRequiredFlags> manifestExcludes, String filePath) {
        Iterator it = manifestExcludes.iterator();
        while (it.hasNext()) {
            String excludePath = ((PathWithRequiredFlags) it.next()).getPath();
            if (excludePath != null && excludePath.equals(filePath)) {
                return true;
            }
        }
        return false;
    }

    public void onRestoreFile(ParcelFileDescriptor data, long size, File destination, int type, long mode, long mtime) throws IOException {
        File file = destination;
        FullBackup.restoreFile(data, size, type, mode, mtime, isFileEligibleForRestore(file) ? file : null);
    }

    private boolean isFileEligibleForRestore(File destination) throws IOException {
        BackupScheme bs = FullBackup.getBackupScheme(this);
        if (bs.isFullBackupContentEnabled()) {
            String destinationCanonicalPath = destination.getCanonicalPath();
            StringBuilder stringBuilder;
            try {
                Map<String, Set<PathWithRequiredFlags>> includes = bs.maybeParseAndGetCanonicalIncludePaths();
                ArraySet<PathWithRequiredFlags> excludes = bs.maybeParseAndGetCanonicalExcludePaths();
                if (excludes == null || !isFileSpecifiedInPathList(destination, excludes)) {
                    if (!(includes == null || includes.isEmpty())) {
                        boolean explicitlyIncluded = false;
                        for (Set<PathWithRequiredFlags> domainIncludes : includes.values()) {
                            explicitlyIncluded |= isFileSpecifiedInPathList(destination, domainIncludes);
                            if (explicitlyIncluded) {
                                break;
                            }
                        }
                        if (!explicitlyIncluded) {
                            if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("onRestoreFile: Trying to restore \"");
                                stringBuilder.append(destinationCanonicalPath);
                                stringBuilder.append("\" but it isn't specified in the included files; skipping.");
                                Log.v("BackupXmlParserLogging", stringBuilder.toString());
                            }
                            return false;
                        }
                    }
                    return true;
                }
                if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onRestoreFile: \"");
                    stringBuilder2.append(destinationCanonicalPath);
                    stringBuilder2.append("\": listed in excludes; skipping.");
                    Log.v("BackupXmlParserLogging", stringBuilder2.toString());
                }
                return false;
            } catch (XmlPullParserException e) {
                if (Log.isLoggable("BackupXmlParserLogging", 2)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onRestoreFile \"");
                    stringBuilder.append(destinationCanonicalPath);
                    stringBuilder.append("\" : Exception trying to parse fullBackupContent xml file! Aborting onRestoreFile.");
                    Log.v("BackupXmlParserLogging", stringBuilder.toString(), e);
                }
                return false;
            }
        }
        if (Log.isLoggable("BackupXmlParserLogging", 2)) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onRestoreFile \"");
            stringBuilder3.append(destination.getCanonicalPath());
            stringBuilder3.append("\" : fullBackupContent not enabled for ");
            stringBuilder3.append(getPackageName());
            Log.v("BackupXmlParserLogging", stringBuilder3.toString());
        }
        return false;
    }

    private boolean isFileSpecifiedInPathList(File file, Collection<PathWithRequiredFlags> canonicalPathList) throws IOException {
        for (PathWithRequiredFlags canonical : canonicalPathList) {
            String canonicalPath = canonical.getPath();
            File fileFromList = new File(canonicalPath);
            if (fileFromList.isDirectory()) {
                if (file.isDirectory()) {
                    return file.equals(fileFromList);
                }
                return file.getCanonicalPath().startsWith(canonicalPath);
            } else if (file.equals(fileFromList)) {
                return true;
            }
        }
        return false;
    }

    protected void onRestoreFile(ParcelFileDescriptor data, long size, int type, String domain, String path, long mode, long mtime) throws IOException {
        long mode2;
        String str = domain;
        String basePath = FullBackup.getBackupScheme(this).tokenToDirectoryPath(str);
        if (str.equals(FullBackup.MANAGED_EXTERNAL_TREE_TOKEN)) {
            mode2 = -1;
        } else {
            mode2 = mode;
        }
        if (basePath != null) {
            File outFile = new File(basePath, path);
            String outPath = outFile.getCanonicalPath();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(basePath);
            stringBuilder.append(File.separatorChar);
            if (outPath.startsWith(stringBuilder.toString())) {
                onRestoreFile(data, size, outFile, type, mode2, mtime);
                return;
            }
        }
        String str2 = path;
        FullBackup.restoreFile(data, size, type, mode2, mtime, null);
    }

    public void onRestoreFinished() {
    }

    public final IBinder onBind() {
        return this.mBinder;
    }

    public void attach(Context context) {
        attachBaseContext(context);
    }
}
