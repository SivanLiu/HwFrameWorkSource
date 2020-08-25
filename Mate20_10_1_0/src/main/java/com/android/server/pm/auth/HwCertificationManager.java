package com.android.server.pm.auth;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageParser;
import android.os.Process;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Flog;
import com.android.server.pm.HwMdmDFTUtilImpl;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.processor.HwCertificationProcessor;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.json.JSONException;
import org.json.JSONObject;

public class HwCertificationManager {
    private static final String BDREPORT_MDM_KEY_APINAME = "apiName";
    private static final String BDREPORT_MDM_KEY_PACKAGE = "package";
    private static final String BDREPORT_MDM_VALUE_APINAME = "check_hwcert";
    private static final int CORE_POOL_SIZE = 0;
    private static final int DEFAULT_SIZE = 10;
    public static final int GET_SIGNATURE_OF_CERT = 0;
    public static final boolean IS_HAS_FEATUREH = true;
    private static final int KEEP_ALIVE = 5;
    private static final Object LOCK = new Object();
    private static final int MAXIMUM_POOL_SIZE = 1;
    private static final int QUEUE_SIZE = 10;
    public static final String TAG = "HwCertificationManager";
    private static Context mContext;
    private static HwCertificationManager mInstance;
    private ConcurrentHashMap<String, HwCertification> mCertMap = new ConcurrentHashMap<>();
    private ExecutorService mHwCertExecutor = new ThreadPoolExecutor(0, 1, 5, TimeUnit.SECONDS, new LinkedBlockingQueue(10));
    private final HwCertXmlHandler mHwCertXmlHandler = new HwCertXmlHandler();
    private AtomicBoolean mIsSystemReady = new AtomicBoolean(false);
    private List<String> mMdmCertBlackList = new ArrayList(10);
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.server.pm.auth.HwCertificationManager.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                HwCertificationManager.this.handlePackagesChanged(intent);
            }
        }
    };

    private HwCertificationManager() {
        readHwCertXml();
        readMdmCertBlacklist();
    }

    private void scheduleWriteStateLocked(boolean isRemoveCache) {
        this.mHwCertExecutor.execute(new Runnable(isRemoveCache) {
            /* class com.android.server.pm.auth.$$Lambda$HwCertificationManager$Dc_b9iE8kRfz7K84umfKA9nlf0 */
            private final /* synthetic */ boolean f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwCertificationManager.this.lambda$scheduleWriteStateLocked$0$HwCertificationManager(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$scheduleWriteStateLocked$0$HwCertificationManager(boolean isRemoveCache) {
        this.mHwCertXmlHandler.updateHwCert(new ArrayList(this.mCertMap.values()));
        if (isRemoveCache) {
            removeCertCache();
        }
    }

    private void readHwCertXml() {
        syncHwCertCache();
    }

    private void readMdmCertBlacklist() {
        this.mMdmCertBlackList.clear();
        this.mHwCertXmlHandler.readMdmCertBlacklist(this.mMdmCertBlackList);
        if (HwAuthLogger.getHWFLOW()) {
            HwAuthLogger.i("HwCertificationManager", "readMdmCertBlacklist, mMdmCertBlackList size:" + this.mMdmCertBlackList.size());
        }
    }

    private HwCertification parseAndVerify(PackageParser.Package pkg) {
        HwCertification cert = new HwCertification();
        HwCertificationProcessor hwCertificationProcessor = new HwCertificationProcessor();
        try {
            hwCertificationProcessor.createZipFile(pkg.baseCodePath);
            if (!hwCertificationProcessor.readCert(pkg.baseCodePath, cert)) {
                HwAuthLogger.e("HwCertificationManager", "read cert failed");
                return null;
            } else if (!hwCertificationProcessor.parserCert(cert)) {
                HwAuthLogger.e("HwCertificationManager", "parse cert failed");
                cert.resetZipFile();
                hwCertificationProcessor.releaseZipFileResource();
                return null;
            } else if (!hwCertificationProcessor.verifyCert(pkg, cert)) {
                HwAuthLogger.e("HwCertificationManager", "verify cert failed");
                cert.resetZipFile();
                hwCertificationProcessor.releaseZipFileResource();
                return null;
            } else {
                cert.resetZipFile();
                hwCertificationProcessor.releaseZipFileResource();
                return cert;
            }
        } finally {
            cert.resetZipFile();
            hwCertificationProcessor.releaseZipFileResource();
        }
    }

    private void addHwPermission(PackageParser.Package pkg, HwCertification cert) {
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
                    return mInstance;
                }
                HwAuthLogger.e("HwCertificationManager", "getIntance from uid:" + uid + ",not system.return null");
                return null;
            }
            throw new IllegalArgumentException("Impossible to get the instance. This class must be initialized before");
        }
    }

    public Context getContext() {
        return mContext;
    }

    public static boolean isSupportHwCertification(PackageParser.Package pkg) {
        if (pkg == null || pkg.requestedPermissions == null) {
            return false;
        }
        if (pkg.requestedPermissions.contains("com.huawei.permission.sec.MDM") || pkg.requestedPermissions.contains("com.huawei.permission.sec.MDM.v2")) {
            return true;
        }
        return false;
    }

    public boolean checkHwCertification(PackageParser.Package pkg) {
        synchronized (LOCK) {
            HwCertification cert = null;
            try {
                HwCertification cert2 = parseAndVerify(pkg);
                if (cert2 != null) {
                    syncHwCertCache();
                    this.mCertMap.put(cert2.getPackageName().trim(), cert2);
                    addHwPermission(pkg, cert2);
                    scheduleWriteStateLocked(false);
                    if (getHwCertificateType(pkg.packageName) == 6) {
                        try {
                            JSONObject obj = new JSONObject();
                            obj.put(BDREPORT_MDM_KEY_PACKAGE, pkg.packageName);
                            obj.put(BDREPORT_MDM_KEY_APINAME, BDREPORT_MDM_VALUE_APINAME);
                            Flog.bdReport(mContext, 127, obj.toString());
                        } catch (JSONException e) {
                            HwAuthLogger.e("HwCertificationManager", "JSONException can not put on obj");
                        }
                    }
                    if (isContainHwCertification(pkg.packageName)) {
                        HwMdmDFTUtilImpl.getMdmInstallInfoDft(mContext, pkg);
                    }
                    return true;
                }
                if (HwAuthLogger.getHWFLOW()) {
                    HwAuthLogger.e("HwCertificationManager", "check HwCertification error, cert is null!");
                }
                removeExistedCert(pkg);
                return false;
            } catch (Exception e2) {
                if (0 != 0) {
                    cert.setPermissionList(new ArrayList(0));
                }
                HwAuthLogger.e("HwCertificationManager", "check HwCertification error!");
                return false;
            }
        }
    }

    public boolean isSystemReady() {
        return this.mIsSystemReady.get();
    }

    public static boolean hasFeature() {
        return true;
    }

    public void systemReady() {
        this.mIsSystemReady.set(true);
        try {
            removeNotExist();
        } catch (Exception e) {
            HwAuthLogger.e("HwCertificationManager", "remove invalid package list error!");
        }
        resigterBroadcastReceiver();
    }

    @SuppressLint({"AvoidMethodInForLoop"})
    private void removeNotExist() {
        syncHwCertCache();
        Iterator<String> it = this.mCertMap.keySet().iterator();
        while (it.hasNext()) {
            String pkgName = it.next();
            if (!Utils.isPackageInstalled(pkgName, mContext) && this.mCertMap.get(pkgName) != null) {
                it.remove();
            }
        }
        scheduleWriteStateLocked(true);
    }

    private void removeExistedCert(PackageParser.Package pkg) {
        syncHwCertCache();
        if (this.mCertMap.get(pkg.packageName) != null) {
            this.mCertMap.remove(pkg.packageName);
        }
        scheduleWriteStateLocked(false);
    }

    public void cleanUp(PackageParser.Package pkg) {
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
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addDataScheme(BDREPORT_MDM_KEY_PACKAGE);
        mContext.registerReceiver(this.mReceiver, filter);
    }

    /* access modifiers changed from: private */
    public void handlePackagesChanged(Intent intent) {
        String action;
        if (intent != null && intent.getData() != null && (action = intent.getAction()) != null) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                onPackageRemoved(packageName);
            }
            if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                removeCertCache();
            }
        }
    }

    private void onPackageRemoved(String packageName) {
        syncHwCertCache();
        if (packageName != null && this.mCertMap.containsKey(packageName)) {
            if (Utils.isPackageInstalled(packageName, mContext)) {
                HwAuthLogger.w("HwCertificationManager", "package:" + packageName + " is exist in the package");
                return;
            }
            this.mCertMap.remove(packageName);
            scheduleWriteStateLocked(false);
            if (HwAuthLogger.getHWFLOW()) {
                HwAuthLogger.i("HwCertificationManager", "package:" + packageName + ",remove from the cert list xml");
            }
        }
    }

    public boolean getHwCertificationPermission(boolean isAllowed, PackageParser.Package pkg, String perm) {
        List<String> permissions;
        if (isAllowed || pkg == null || !this.mCertMap.containsKey(pkg.packageName) || (permissions = this.mCertMap.get(pkg.packageName).getPermissionList()) == null || !permissions.contains(perm) || pkg.requestedPermissions == null || !pkg.requestedPermissions.contains(perm)) {
            return isAllowed;
        }
        if (!HwAuthLogger.getHWDEBUG()) {
            return true;
        }
        HwAuthLogger.i("HwCertificationManager", "[package]:" + pkg.packageName + ",perm:" + perm);
        return true;
    }

    public int getHwCertificateType(String packageName) {
        HwCertification cert = this.mCertMap.get(packageName);
        if (cert != null) {
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
        } else if (!HwAuthLogger.getHWDEBUG()) {
            return 5;
        } else {
            HwAuthLogger.i("HwCertificationManager", "getHwCertificateType: cert is null, and pkg name is " + packageName);
            return 5;
        }
    }

    public int getHwCertSignatureVersion(String packageName) {
        HwCertification cert = this.mCertMap.get(packageName);
        if (cert == null) {
            if (!HwAuthLogger.getHWDEBUG()) {
                return -1;
            }
            HwAuthLogger.i("HwCertificationManager", "getSignatureVersion: cert is null, pkg name is " + packageName);
            return -1;
        } else if (TextUtils.isEmpty(cert.getSignature2())) {
            return 1;
        } else {
            return 2;
        }
    }

    public boolean isContainHwCertification(String packageName) {
        return this.mCertMap.get(packageName) != null;
    }

    public int getHwCertificateTypeNotMDM() {
        return 5;
    }

    public void updateMdmCertBlacklist() {
        readMdmCertBlacklist();
        Context context = mContext;
        if (context != null) {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
            PackageManagerService packageManager = (PackageManagerService) ServiceManager.getService(BDREPORT_MDM_KEY_PACKAGE);
            syncHwCertCache();
            HashMap<String, String> certMap = new HashMap<>();
            for (String packageName : this.mCertMap.keySet()) {
                String signature = getSignatureOfCert(packageName, 0);
                if (!TextUtils.isEmpty(signature)) {
                    certMap.put(signature, packageName);
                }
            }
            synchronized (LOCK) {
                boolean isUpdated = false;
                for (String blackItem : this.mMdmCertBlackList) {
                    if (removeBlackCertPackageName(blackItem, devicePolicyManager, packageManager, certMap)) {
                        isUpdated = true;
                    }
                }
                if (isUpdated) {
                    scheduleWriteStateLocked(true);
                }
            }
        }
    }

    private boolean removeBlackCertPackageName(String blackItem, DevicePolicyManager devicePolicyManager, PackageManagerService packageManager, HashMap<String, String> certMap) {
        String packageName;
        HwCertification certification;
        if (TextUtils.isEmpty(blackItem) || packageManager == null || certMap == null || !certMap.containsKey(blackItem) || (certification = this.mCertMap.get((packageName = certMap.get(blackItem)))) == null) {
            return false;
        }
        List<String> permissionList = certification.getPermissionList();
        if (!(permissionList == null || permissionList.size() == 0)) {
            packageManager.getHwPMSEx().revokePermissionsFromApp(packageName, permissionList);
        }
        removeActiveAdmin(packageName, devicePolicyManager);
        this.mCertMap.remove(packageName);
        certMap.remove(blackItem);
        HwAuthLogger.i("HwCertificationManager", packageName + " removed from the cert list xml");
        return true;
    }

    private void removeActiveAdmin(String packageName, DevicePolicyManager devicePolicyManager) {
        List<ComponentName> activeAdmins;
        if (!TextUtils.isEmpty(packageName) && devicePolicyManager != null && (activeAdmins = devicePolicyManager.getActiveAdmins()) != null) {
            for (ComponentName component : activeAdmins) {
                if (packageName.equals(component.getPackageName())) {
                    devicePolicyManager.removeActiveAdmin(component);
                    HwAuthLogger.i("HwCertificationManager", component.toString() + " removed from activeAdmins");
                    return;
                }
            }
        }
    }

    public String getSignatureOfCert(String packageName, int flag) {
        HwCertification certification;
        if (flag == 0 && !TextUtils.isEmpty(packageName) && (certification = this.mCertMap.get(packageName)) != null) {
            return certification.getSignature2();
        }
        return "";
    }

    public boolean checkMdmCertBlacklist(String signature) {
        if (!TextUtils.isEmpty(signature) && this.mMdmCertBlackList.contains(signature)) {
            return true;
        }
        return false;
    }

    private void removeCertCache() {
        if (this.mIsSystemReady.get()) {
            synchronized (LOCK) {
                this.mCertMap.forEach(new BiConsumer() {
                    /* class com.android.server.pm.auth.$$Lambda$HwCertificationManager$pjuD7lF0Hg1FfZoRonbSSNgk6aE */

                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        HwCertificationManager.this.lambda$removeCertCache$1$HwCertificationManager((String) obj, (HwCertification) obj2);
                    }
                });
            }
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: removeCertCacheOfPackage */
    public void lambda$removeCertCache$1$HwCertificationManager(String packageName, HwCertification cert) {
        if (cert != null) {
            cert.setSignature("");
            cert.setDelveoperKey("");
            cert.setApkHash("");
            HwCertification.CertificationData certificationData = cert.mCertificationData;
            if (certificationData != null) {
                certificationData.mSignature = "";
                certificationData.mDelveoperKey = "";
                certificationData.mApkHash = "";
                if (!cert.isContainSpecialPermissions()) {
                    cert.setSignature2("");
                    certificationData.mSignature2 = "";
                }
            }
        }
    }

    private void syncHwCertCache() {
        synchronized (LOCK) {
            this.mCertMap.clear();
            this.mHwCertXmlHandler.readHwCertXml(this.mCertMap);
        }
    }
}
