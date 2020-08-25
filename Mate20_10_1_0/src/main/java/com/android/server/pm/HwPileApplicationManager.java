package com.android.server.pm;

import android.app.PackageInstallObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManagerInternal;
import android.os.BadParcelableException;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.LocalServices;
import com.android.server.pm.HwPileApplicationManager;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class HwPileApplicationManager {
    /* access modifiers changed from: private */
    public static final boolean IS_DEBUG = SystemProperties.get("ro.dbg.pms_log", "0").equals("on");
    private static final int MAX_SCAN_DIR_DEP = 2;
    private static final String[] PILE_APP_DIRS = {"/data/update/cloud_rom/language", "/data/update/cloud_rom/lfapp", "/data/update/cloud_rom/rom_feature"};
    private static final String[] PILE_BUNDLE_DIRS = {"/data/update/cloud_rom/app_bundle"};
    private static final String PILE_INSTALL_RESULT_BROADCAST_RECEIVER = "com.huawei.android.hwouc";
    private static final String TAG = "HwPileApplicationManager";
    private PackageManagerInternal hwPmsEx;
    @GuardedBy({"this"})
    private Set<String> mBundleApkFeatureDirs;
    @GuardedBy({"this"})
    private Map<String, List<String>> mBundleApkMaps;
    /* access modifiers changed from: private */
    public Context mContext;
    @GuardedBy({"this"})
    private Set<String> mFailedPackageNames;
    private final HandlerThread mHandlerThread;
    private boolean mHasApkToInstall;
    @GuardedBy({"this"})
    private boolean mIsInstallFinish;
    @GuardedBy({"this"})
    private List<String> mPileApkList;
    private BroadcastReceiver mReceiver;
    @GuardedBy({"this"})
    private Set<String> mSuccessPackageNames;

    public HwPileApplicationManager(Context context) {
        this.mPileApkList = null;
        this.mBundleApkMaps = null;
        this.mBundleApkFeatureDirs = null;
        this.mSuccessPackageNames = null;
        this.mFailedPackageNames = null;
        this.mIsInstallFinish = false;
        this.mHasApkToInstall = false;
        this.hwPmsEx = null;
        this.mReceiver = new BroadcastReceiver() {
            /* class com.android.server.pm.HwPileApplicationManager.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                if (HwPileApplicationManager.IS_DEBUG) {
                    Slog.i(HwPileApplicationManager.TAG, "onReceive:ACTION_USER_UNLOCKED");
                }
                HwPileApplicationManager.this.getHandler().post(new Runnable() {
                    /* class com.android.server.pm.$$Lambda$HwPileApplicationManager$1$mcwbenlx2nNSgpruSiLvaEmVbg */

                    public final void run() {
                        HwPileApplicationManager.AnonymousClass1.this.lambda$onReceive$0$HwPileApplicationManager$1();
                    }
                });
                HwPileApplicationManager.this.mContext.unregisterReceiver(this);
            }

            public /* synthetic */ void lambda$onReceive$0$HwPileApplicationManager$1() {
                HwPileApplicationManager.this.sendResultBroadcast();
            }
        };
        this.hwPmsEx = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mContext = context;
        this.mPileApkList = new ArrayList();
        this.mBundleApkMaps = new HashMap();
        this.mBundleApkFeatureDirs = new HashSet();
        this.mSuccessPackageNames = new HashSet();
        this.mFailedPackageNames = new HashSet();
        registerUserUnlockReceiver();
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
    }

    private void registerUserUnlockReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    public void startInstallPileApk() {
        this.mSuccessPackageNames.clear();
        this.mFailedPackageNames.clear();
        getHandler().post(new Runnable() {
            /* class com.android.server.pm.$$Lambda$HwPileApplicationManager$6SjkOHCcE1BGFwvmKOO104O20 */

            public final void run() {
                HwPileApplicationManager.this.lambda$startInstallPileApk$0$HwPileApplicationManager();
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: initPileApkListAndStartInstll */
    public void lambda$startInstallPileApk$0$HwPileApplicationManager() {
        scanApkFile();
        scanBundleApkFile();
        if (!this.mPileApkList.isEmpty() || !this.mBundleApkMaps.isEmpty()) {
            this.mHasApkToInstall = true;
        }
        lambda$installApk$1$HwPileApplicationManager();
    }

    /* access modifiers changed from: private */
    public Handler getHandler() {
        return this.mHandlerThread.getThreadHandler();
    }

    /* access modifiers changed from: private */
    /* renamed from: startInstallPileApkInner */
    public void lambda$installApk$1$HwPileApplicationManager() {
        synchronized (this) {
            if (this.mPileApkList != null) {
                if (!this.mPileApkList.isEmpty()) {
                    this.mPileApkList.remove(0);
                    installApk(this.mPileApkList.get(0));
                    return;
                }
            }
            lambda$installBundleApks$3$HwPileApplicationManager();
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: startInstallBundleApk */
    public void lambda$installBundleApks$3$HwPileApplicationManager() {
        synchronized (this) {
            if (this.mBundleApkMaps != null) {
                if (!this.mBundleApkMaps.isEmpty()) {
                    String installBundleDirName = this.mBundleApkMaps.keySet().iterator().next();
                    installBundleApks(getPackageName(installBundleDirName), this.mBundleApkMaps.get(installBundleDirName));
                    this.mBundleApkMaps.remove(installBundleDirName);
                    return;
                }
            }
            finishInstall();
        }
    }

    private void finishInstall() {
        Slog.d(TAG, "finish install");
        deleteBundleApkDirs();
        synchronized (this) {
            this.mIsInstallFinish = true;
        }
        sendResultBroadcast();
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0016, code lost:
        if (com.android.server.pm.HwPileApplicationManager.IS_DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0018, code lost:
        android.util.Slog.i(com.android.server.pm.HwPileApplicationManager.TAG, "No applications need to install,do not send result broadcast");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0020, code lost:
        r0 = new android.content.Intent();
        r0.setAction("com.huawei.server.action.PILE_APP_INSTALL_FINISH");
        r0.addFlags(16777216);
        r0.setPackage(com.android.server.pm.HwPileApplicationManager.PILE_INSTALL_RESULT_BROADCAST_RECEIVER);
        r1 = java.lang.String.join(",", r5.mSuccessPackageNames);
        r2 = java.lang.String.join(",", r5.mFailedPackageNames);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0046, code lost:
        if (com.android.server.pm.HwPileApplicationManager.IS_DEBUG == false) goto L_0x0074;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0048, code lost:
        android.util.Slog.i(com.android.server.pm.HwPileApplicationManager.TAG, "finishInstall success:" + r1);
        android.util.Slog.i(com.android.server.pm.HwPileApplicationManager.TAG, "finishInstall failed:" + r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0074, code lost:
        r0.putExtra("install_successed_packages", r1);
        r0.putExtra("install_failed_packages", r2);
        r5.mContext.sendBroadcast(r0);
        r5.mSuccessPackageNames.clear();
        r5.mFailedPackageNames.clear();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x008d, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0012, code lost:
        if (r5.mHasApkToInstall != false) goto L_0x0020;
     */
    public void sendResultBroadcast() {
        synchronized (this) {
            if (this.mIsInstallFinish) {
                if (!HwPackageManagerUtils.isUserUnlocked(this.mContext)) {
                }
            }
            if (IS_DEBUG) {
                Slog.i(TAG, "The installation is not finish or user is not unlock,do not send result broadcast");
            }
        }
    }

    private void deleteBundleApkDirs() {
        for (String bundleApkDir : this.mBundleApkFeatureDirs) {
            deleteApkFile(bundleApkDir);
        }
    }

    /* access modifiers changed from: private */
    public void deleteApkFile(String apkFile) {
        try {
            File tempFile = new File(apkFile.trim()).getCanonicalFile();
            if (tempFile.exists()) {
                boolean isDeleteSuccess = tempFile.delete();
                Slog.d(TAG, "delete " + apkFile + " result:" + isDeleteSuccess);
            }
        } catch (IOException e) {
            Slog.e(TAG, "delete apk file failed:" + apkFile);
        }
    }

    private void installApk(final String apkFile) {
        final String packageName = HwPackageManagerUtils.getPackageNameFromApk(apkFile);
        Slog.w(TAG, "installApk: " + apkFile + ",packageName:" + packageName);
        if (TextUtils.isEmpty(packageName)) {
            Slog.i(TAG, "Illegal apk file:" + apkFile);
            deleteApkFile(apkFile);
            getHandler().post(new Runnable() {
                /* class com.android.server.pm.$$Lambda$HwPileApplicationManager$Tf7H0h8v8ZIPGO6yH6LfPLsAZ0 */

                public final void run() {
                    HwPileApplicationManager.this.lambda$installApk$1$HwPileApplicationManager();
                }
            });
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.hwPmsEx.installPackageAsUser(apkFile, new PackageInstallObserver() {
                /* class com.android.server.pm.HwPileApplicationManager.AnonymousClass2 */

                public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
                    boolean isInstallSuccess = false;
                    if (returnCode == 1) {
                        HwPileApplicationManager.this.addInstallFinishPackageName(packageName, true);
                        isInstallSuccess = true;
                    } else {
                        HwPileApplicationManager.this.addInstallFinishPackageName(packageName, false);
                    }
                    Slog.i(HwPileApplicationManager.TAG, "The package " + apkFile + ",code:" + returnCode + ",result:" + isInstallSuccess);
                    HwPileApplicationManager.this.deleteApkFile(apkFile);
                    HwPileApplicationManager.this.getHandler().post(new Runnable() {
                        /* class com.android.server.pm.$$Lambda$HwPileApplicationManager$2$M6DV1_yegpNGl8XzWdE8YA8qTCM */

                        public final void run() {
                            HwPileApplicationManager.AnonymousClass2.this.lambda$onPackageInstalled$0$HwPileApplicationManager$2();
                        }
                    });
                }

                public /* synthetic */ void lambda$onPackageInstalled$0$HwPileApplicationManager$2() {
                    HwPileApplicationManager.this.lambda$installApk$1$HwPileApplicationManager();
                }
            }.getBinder(), 2, this.mContext.getPackageName(), 0);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void scanApkFile() {
        synchronized (this) {
            this.mPileApkList.clear();
            for (String scanDir : PILE_APP_DIRS) {
                scanApkFileInDir(new File(scanDir), 0);
            }
        }
        if (IS_DEBUG) {
            Slog.d(TAG, "scanApkFile BasePileApkList:" + this.mPileApkList);
        }
    }

    private void scanBundleApkFile() {
        synchronized (this) {
            this.mBundleApkMaps.clear();
            this.mBundleApkFeatureDirs.clear();
            for (String scanDir : PILE_BUNDLE_DIRS) {
                scanBundleApkFileInDir(new File(scanDir));
            }
        }
        if (IS_DEBUG) {
            Slog.d(TAG, "scanApkFile BundleApks:" + this.mBundleApkMaps);
        }
    }

    @GuardedBy({"this"})
    private void scanApkFileInDir(File dirFile, int dep) {
        if (dirFile != null && dirFile.exists() && dirFile.isDirectory() && dep <= 2) {
            File[] files = dirFile.listFiles();
            for (File getFile : files) {
                if (getFile.isFile() && HwPackageManagerUtils.isPackageFilename(getFile.getName())) {
                    this.mPileApkList.add(getFile.getPath());
                }
                if (getFile.isDirectory()) {
                    scanApkFileInDir(getFile, dep + 1);
                }
            }
        }
    }

    @GuardedBy({"this"})
    private void scanBundleApkFileInDir(File dirFile) {
        if (dirFile != null && dirFile.exists() && dirFile.isDirectory()) {
            File[] files = dirFile.listFiles();
            for (File getFile : files) {
                if (getFile.isDirectory()) {
                    List<String> bundleApks = getBundleApksInDir(getFile);
                    if (!bundleApks.isEmpty()) {
                        this.mBundleApkMaps.put(getFile.getName(), bundleApks);
                    }
                    this.mBundleApkFeatureDirs.add(getFile.getPath());
                }
            }
        }
    }

    @GuardedBy({"this"})
    private List<String> getBundleApksInDir(File dirFile) {
        File[] files = dirFile.listFiles();
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        List<String> bundleApks = new ArrayList<>(files.length);
        for (File getFile : files) {
            if (getFile.isFile() && HwPackageManagerUtils.isPackageFilename(getFile.getName())) {
                bundleApks.add(getFile.getPath());
            }
        }
        return bundleApks;
    }

    /* access modifiers changed from: private */
    @GuardedBy({"this"})
    public void addInstallFinishPackageName(String packageName, boolean isSuccess) {
        if (!TextUtils.isEmpty(packageName)) {
            synchronized (this) {
                if (isSuccess) {
                    this.mSuccessPackageNames.add(packageName);
                } else {
                    this.mFailedPackageNames.add(packageName);
                }
            }
        }
    }

    private String getPackageName(String dirFileName) {
        if (TextUtils.isEmpty(dirFileName)) {
            return null;
        }
        String[] fileSplitNames = dirFileName.split("_");
        if (fileSplitNames.length >= 2) {
            return fileSplitNames[0];
        }
        Slog.i(TAG, dirFileName + " illegal!");
        return null;
    }

    private void installBundleApks(String packageName, List<String> bundleApks) {
        Slog.i(TAG, "installBundleApks:" + packageName + ", bundleApks:" + bundleApks);
        if (TextUtils.isEmpty(packageName) || bundleApks == null || bundleApks.isEmpty()) {
            getHandler().post(new Runnable() {
                /* class com.android.server.pm.$$Lambda$HwPileApplicationManager$5Idu7rGT5UMJylcOTF8WzxVnuXo */

                public final void run() {
                    HwPileApplicationManager.this.lambda$installBundleApks$2$HwPileApplicationManager();
                }
            });
            return;
        }
        try {
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(2);
            params.setAppPackageName(packageName);
            PackageInstaller packageInstaller = this.mContext.getPackageManager().getPackageInstaller();
            PackageInstaller.Session session = packageInstaller.openSession(packageInstaller.createSession(params));
            for (String bundleApkFile : bundleApks) {
                writeApk2Session(session, bundleApkFile);
            }
            session.commit(new LocalInstallIntentReceiver(new Consumer(packageName) {
                /* class com.android.server.pm.$$Lambda$HwPileApplicationManager$ZDL_JKKnHA5W1qkDgXG4EiJ7RGc */
                private final /* synthetic */ String f$1;

                {
                    this.f$1 = r2;
                }

                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    HwPileApplicationManager.this.lambda$installBundleApks$5$HwPileApplicationManager(this.f$1, (Intent) obj);
                }
            }).getIntentSender());
        } catch (IOException e) {
            Slog.e(TAG, "installBundleApks IOException");
        }
    }

    public /* synthetic */ void lambda$installBundleApks$5$HwPileApplicationManager(String packageName, Intent result) {
        getHandler().post(new Runnable(result, packageName) {
            /* class com.android.server.pm.$$Lambda$HwPileApplicationManager$EaJ_TjBulB5dg9pZhQnSXRguE */
            private final /* synthetic */ Intent f$1;
            private final /* synthetic */ String f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                HwPileApplicationManager.this.lambda$installBundleApks$4$HwPileApplicationManager(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$installBundleApks$4$HwPileApplicationManager(Intent result, String packageName) {
        $$Lambda$HwPileApplicationManager$Iy_Zh2Ll0cfL1gJtNWHxyGWk1rs r1;
        Handler handler;
        try {
            int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
            boolean isInstallSuccess = false;
            if (status == 0) {
                addInstallFinishPackageName(packageName, true);
                isInstallSuccess = true;
            } else {
                addInstallFinishPackageName(packageName, false);
            }
            Slog.i(TAG, "LocalInstallIntentReceiver status:" + status + ",result:" + isInstallSuccess);
            handler = getHandler();
            r1 = new Runnable() {
                /* class com.android.server.pm.$$Lambda$HwPileApplicationManager$Iy_Zh2Ll0cfL1gJtNWHxyGWk1rs */

                public final void run() {
                    HwPileApplicationManager.this.lambda$installBundleApks$3$HwPileApplicationManager();
                }
            };
        } catch (BadParcelableException e) {
            Slog.e(TAG, "LocalInstallIntentReceiver BadParcelableException!");
            handler = getHandler();
            r1 = new Runnable() {
                /* class com.android.server.pm.$$Lambda$HwPileApplicationManager$Iy_Zh2Ll0cfL1gJtNWHxyGWk1rs */

                public final void run() {
                    HwPileApplicationManager.this.lambda$installBundleApks$3$HwPileApplicationManager();
                }
            };
        } catch (Throwable th) {
            getHandler().post(new Runnable() {
                /* class com.android.server.pm.$$Lambda$HwPileApplicationManager$Iy_Zh2Ll0cfL1gJtNWHxyGWk1rs */

                public final void run() {
                    HwPileApplicationManager.this.lambda$installBundleApks$3$HwPileApplicationManager();
                }
            });
            throw th;
        }
        handler.post(r1);
    }

    /* JADX INFO: finally extract failed */
    private void writeApk2Session(PackageInstaller.Session session, String apkFilePath) throws IOException {
        if (IS_DEBUG) {
            Slog.d(TAG, "writeApk2Session:" + apkFilePath);
        }
        File apkFile = new File(apkFilePath.trim()).getCanonicalFile();
        InputStream in = new FileInputStream(apkFile);
        OutputStream out = session.openWrite(apkFile.getName(), 0, -1);
        try {
            FileUtils.copy(new BufferedInputStream(in), out);
            session.fsync(out);
            closeStream(in, out);
            if (IS_DEBUG) {
                Slog.d(TAG, "writeApk2Session:" + apkFilePath + ",Finish");
            }
        } catch (Throwable th) {
            closeStream(in, out);
            throw th;
        }
    }

    private static class LocalInstallIntentReceiver {
        final Consumer<Intent> mConsumer;
        private IIntentSender.Stub mLocalSender = new IIntentSender.Stub() {
            /* class com.android.server.pm.HwPileApplicationManager.LocalInstallIntentReceiver.AnonymousClass1 */

            public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                LocalInstallIntentReceiver.this.mConsumer.accept(intent);
            }
        };

        LocalInstallIntentReceiver(Consumer<Intent> consumer) {
            this.mConsumer = consumer;
        }

        public IntentSender getIntentSender() {
            return new IntentSender(this.mLocalSender);
        }
    }

    private void closeStream(InputStream in, OutputStream out) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                Slog.e(TAG, "close InputStream IOException!");
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e2) {
                Slog.e(TAG, "close OutputStream IOException!");
            }
        }
    }
}
