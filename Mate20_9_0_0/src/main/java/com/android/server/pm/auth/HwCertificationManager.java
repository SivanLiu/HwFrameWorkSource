package com.android.server.pm.auth;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageParser.Package;
import android.os.Process;
import android.util.Flog;
import com.android.server.pm.auth.processor.HwCertificationProcessor;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HwCertificationManager {
    public static final String TAG = "HwCertificationManager";
    private static Context mContext = null;
    public static final boolean mHasFeature = true;
    private static HwCertificationManager mInstance;
    private ConcurrentHashMap<String, HwCertification> mCertMap = new ConcurrentHashMap();
    private Object mLock = new Object();
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                HwCertificationManager.this.handlePackagesChanged(intent);
            }
        }
    };
    private boolean mSystemReady = false;
    private final Runnable mWriteStateRunnable = new Runnable() {
        public void run() {
            List<HwCertification> data = new ArrayList();
            data.addAll(HwCertificationManager.this.mCertMap.values());
            new HwCertXmlHandler().updateHwCert(data);
        }
    };
    private boolean mloaded = false;

    private HwCertificationManager() {
        readHwCertXml();
    }

    private synchronized void readHwCertXml() {
        if (!this.mloaded) {
            StringBuilder stringBuilder;
            long start = System.currentTimeMillis();
            HwCertXmlHandler handler = new HwCertXmlHandler();
            this.mCertMap.clear();
            handler.readHwCertXml(this.mCertMap);
            this.mloaded = true;
            long end = System.currentTimeMillis();
            if (HwAuthLogger.getHWFLOW()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("readHwCertXml  spend time:");
                stringBuilder.append(end - start);
                stringBuilder.append(" ms");
                HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
            }
            if (HwAuthLogger.getHWFLOW()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("readHwCertXml  mCertMap size:");
                stringBuilder.append(this.mCertMap.size());
                HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
            }
        }
    }

    private void scheduleWriteStateLocked() {
        new Thread(this.mWriteStateRunnable).start();
    }

    /* JADX WARNING: Missing block: B:9:0x0028, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized HwCertification parseAndVerify(Package pkg) {
        HwCertification cert = new HwCertification();
        HwCertificationProcessor hwCertificationProcessor = new HwCertificationProcessor();
        try {
            hwCertificationProcessor.createZipFile(pkg.baseCodePath);
            HwCertification hwCertification = null;
            if (!hwCertificationProcessor.readCert(pkg.baseCodePath, cert)) {
                HwAuthLogger.e("HwCertificationManager", "read cert failed");
            } else if (!hwCertificationProcessor.parserCert(cert)) {
                HwAuthLogger.e("HwCertificationManager", "parse cert failed");
                cert.resetZipFile();
                hwCertificationProcessor.releaseZipFileResource();
                return hwCertification;
            } else if (hwCertificationProcessor.verifyCert(pkg, cert)) {
                cert.resetZipFile();
                hwCertificationProcessor.releaseZipFileResource();
                return cert;
            } else {
                HwAuthLogger.e("HwCertificationManager", "verify cert failed");
                cert.resetZipFile();
                hwCertificationProcessor.releaseZipFileResource();
                return hwCertification;
            }
        } finally {
            cert.resetZipFile();
            hwCertificationProcessor.releaseZipFileResource();
        }
    }

    private void addHwPermission(Package pkg, HwCertification cert) {
        if (pkg.requestedPermissions != null) {
            for (String perm : cert.getPermissionList()) {
                if (!pkg.requestedPermissions.contains(perm)) {
                    pkg.requestedPermissions.add(perm);
                }
            }
        }
    }

    public static void initialize(Context ctx) {
        mContext = ctx;
    }

    public static boolean isInitialized() {
        return mContext != null;
    }

    public static synchronized HwCertificationManager getIntance() {
        synchronized (HwCertificationManager.class) {
            if (mContext != null) {
                int uid = Process.myUid();
                if (uid == 1000 || uid == 2000 || uid == 0) {
                    if (mInstance == null) {
                        mInstance = new HwCertificationManager();
                    }
                    HwCertificationManager hwCertificationManager = mInstance;
                    return hwCertificationManager;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getIntance from uid:");
                stringBuilder.append(uid);
                stringBuilder.append(",not system.return null");
                HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
                return null;
            }
            throw new IllegalArgumentException("Impossible to get the instance. This class must be initialized before");
        }
    }

    public Context getContext() {
        return mContext;
    }

    public static boolean isSupportHwCertification(Package pkg) {
        boolean z = false;
        if (pkg == null || pkg.requestedPermissions == null) {
            return false;
        }
        if (pkg.requestedPermissions.contains("com.huawei.permission.sec.MDM") || pkg.requestedPermissions.contains("com.huawei.permission.sec.MDM.v2")) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:20:0x006a, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:31:0x00ac, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean checkHwCertification(Package pkg) {
        synchronized (this.mLock) {
            long start = System.currentTimeMillis();
            HwCertification cert = null;
            long end;
            StringBuilder stringBuilder;
            try {
                cert = parseAndVerify(pkg);
                if (cert != null) {
                    this.mCertMap.put(cert.getPackageName().trim(), cert);
                    addHwPermission(pkg, cert);
                    scheduleWriteStateLocked();
                    if (getHwCertificateType(pkg.packageName) == 6) {
                        Flog.bdReport(mContext, 127, pkg.packageName);
                    }
                    end = System.currentTimeMillis();
                    if (HwAuthLogger.getHWFLOW()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("check");
                        stringBuilder.append(pkg.packageName);
                        stringBuilder.append("HwCertification spend time:");
                        stringBuilder.append(end - start);
                        stringBuilder.append(" ms");
                        HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                    }
                } else {
                    if (HwAuthLogger.getHWFLOW()) {
                        HwAuthLogger.e("HwCertificationManager", "check HwCertification error, cert is null!");
                    }
                    end = System.currentTimeMillis();
                    if (HwAuthLogger.getHWFLOW()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("check");
                        stringBuilder.append(pkg.packageName);
                        stringBuilder.append("HwCertification spend time:");
                        stringBuilder.append(end - start);
                        stringBuilder.append(" ms");
                        HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                    }
                }
            } catch (RuntimeException e) {
                HwAuthLogger.e("HwCertificationManager", "check HwCertification error: RuntimeException!");
                long end2 = System.currentTimeMillis();
                if (HwAuthLogger.getHWFLOW()) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("check");
                    stringBuilder2.append(pkg.packageName);
                    stringBuilder2.append("HwCertification spend time:");
                    stringBuilder2.append(end2 - start);
                    stringBuilder2.append(" ms");
                    HwAuthLogger.i("HwCertificationManager", stringBuilder2.toString());
                }
                return false;
            } catch (Exception e2) {
                try {
                    cert.setPermissionList(new ArrayList());
                    HwAuthLogger.e("HwCertificationManager", "check HwCertification error!");
                } finally {
                    end = System.currentTimeMillis();
                    if (HwAuthLogger.getHWFLOW()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("check");
                        stringBuilder.append(pkg.packageName);
                        stringBuilder.append("HwCertification spend time:");
                        stringBuilder.append(end - start);
                        stringBuilder.append(" ms");
                        HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                    }
                }
                return false;
            }
        }
    }

    public boolean isSystemReady() {
        return this.mSystemReady;
    }

    public static boolean hasFeature() {
        return true;
    }

    public void systemReady() {
        this.mSystemReady = true;
        try {
            removeNotExist();
        } catch (Exception e) {
            HwAuthLogger.e("HwCertificationManager", "remove invalid package list error!");
        }
        resigterBroadcastReceiver();
    }

    @SuppressLint({"AvoidMethodInForLoop"})
    private void removeNotExist() {
        List<String> pkgNameList = new ArrayList();
        for (String pkgName : this.mCertMap.keySet()) {
            if (!Utils.isPackageInstalled(pkgName, mContext)) {
                pkgNameList.add(pkgName);
            }
        }
        for (int i = 0; i < pkgNameList.size(); i++) {
            if (this.mCertMap.get(pkgNameList.get(i)) != null) {
                this.mCertMap.remove(pkgNameList.get(i));
                if (HwAuthLogger.getHWDEBUG()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("package:");
                    stringBuilder.append((String) pkgNameList.get(i));
                    stringBuilder.append(" not installed,removed from the cert list xml");
                    HwAuthLogger.d("HwCertificationManager", stringBuilder.toString());
                }
            }
        }
        scheduleWriteStateLocked();
    }

    private void removeExistedCert(Package pkg) {
        StringBuilder stringBuilder;
        if (HwAuthLogger.getHWDEBUG()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeExistedCert");
            stringBuilder.append(pkg.packageName);
            HwAuthLogger.d("HwCertificationManager", stringBuilder.toString());
        }
        if (this.mCertMap.get(pkg.packageName) != null) {
            this.mCertMap.remove(pkg.packageName);
            if (HwAuthLogger.getHWDEBUG()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("package:");
                stringBuilder.append(pkg.packageName);
                stringBuilder.append(" installed,removed from the cert list xml");
                HwAuthLogger.d("HwCertificationManager", stringBuilder.toString());
            }
            scheduleWriteStateLocked();
        }
    }

    public void cleanUp(Package pkg) {
        if (HwAuthLogger.getHWFLOW()) {
            HwAuthLogger.i("HwCertificationManager", "clean up the cert list xml");
        }
        removeExistedCert(pkg);
    }

    public void cleanUp() {
        if (HwAuthLogger.getHWFLOW()) {
            HwAuthLogger.i("HwCertificationManager", "removeNotExist,clean up the cert list xml");
        }
        removeNotExist();
    }

    private void resigterBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        mContext.registerReceiver(this.mReceiver, filter);
    }

    private void handlePackagesChanged(Intent intent) {
        if (intent.getData() != null && intent.getAction() != null) {
            String action = intent.getAction();
            String packageName = intent.getData().getSchemeSpecificPart();
            if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                onPackageRemoved(packageName);
            }
        }
    }

    private void onPackageRemoved(String packageName) {
        synchronized (this.mLock) {
            if (packageName != null) {
                if (this.mCertMap.containsKey(packageName)) {
                    try {
                        StringBuilder stringBuilder;
                        if (Utils.isPackageInstalled(packageName, mContext)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("[package]:");
                            stringBuilder.append(packageName);
                            stringBuilder.append(" is exist in the package");
                            HwAuthLogger.w("HwCertificationManager", stringBuilder.toString());
                            return;
                        }
                        this.mCertMap.remove(packageName);
                        scheduleWriteStateLocked();
                        if (HwAuthLogger.getHWFLOW()) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("[package]:");
                            stringBuilder.append(packageName);
                            stringBuilder.append(",remove from the cert list xml");
                            HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
                        }
                    } catch (Exception ex) {
                        HwAuthLogger.e("HwCertificationManager", "onPackageRemoved error!", ex);
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x005b, code:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getHwCertificationPermission(boolean allowed, Package pkg, String perm) {
        if (allowed || pkg == null || !this.mCertMap.containsKey(pkg.packageName)) {
            return allowed;
        }
        List<String> permissions = ((HwCertification) this.mCertMap.get(pkg.packageName)).getPermissionList();
        if (permissions == null || !permissions.contains(perm) || pkg.requestedPermissions == null || !pkg.requestedPermissions.contains(perm)) {
            return allowed;
        }
        if (HwAuthLogger.getHWDEBUG()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[package]:");
            stringBuilder.append(pkg.packageName);
            stringBuilder.append(",perm:");
            stringBuilder.append(perm);
            HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
        }
        return true;
    }

    public int getHwCertificateType(String packageName) {
        HwCertification cert = (HwCertification) this.mCertMap.get(packageName);
        if (cert == null) {
            if (HwAuthLogger.getHWDEBUG()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getHwCertificateType: cert is null, and pkg name is ");
                stringBuilder.append(packageName);
                HwAuthLogger.i("HwCertificationManager", stringBuilder.toString());
            }
            return 5;
        }
        String certificate = cert.getCertificate();
        if (certificate == null) {
            return 6;
        }
        if (certificate.equals(HwCertification.SIGNATURE_PLATFORM)) {
            return 1;
        }
        if (certificate.equals(HwCertification.SIGNATURE_TESTKEY)) {
            return 2;
        }
        if (certificate.equals(HwCertification.SIGNATURE_SHARED)) {
            return 3;
        }
        if (certificate.equals(HwCertification.SIGNATURE_MEDIA)) {
            return 4;
        }
        if (certificate.equals("null")) {
            return 0;
        }
        return -1;
    }

    public boolean isContainHwCertification(String packageName) {
        return this.mCertMap.get(packageName) != null;
    }

    public int getHwCertificateTypeNotMDM() {
        return 5;
    }
}
