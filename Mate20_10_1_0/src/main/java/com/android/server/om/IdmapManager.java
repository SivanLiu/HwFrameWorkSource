package com.android.server.om;

import android.content.om.OverlayInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.IIdmap2;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.om.OverlayManagerServiceImpl;
import com.android.server.pm.Installer;
import java.io.File;

class IdmapManager {
    private static final boolean FEATURE_FLAG_IDMAP2 = true;
    private static final boolean VENDOR_IS_Q_OR_LATER;
    private IIdmap2 mIdmap2Service;
    private final Installer mInstaller;
    private final OverlayManagerServiceImpl.PackageManagerHelper mPackageManager;

    static {
        boolean isQOrLater;
        try {
            isQOrLater = Integer.parseInt(SystemProperties.get("ro.vndk.version", "29")) >= 29;
        } catch (NumberFormatException e) {
            isQOrLater = true;
        }
        VENDOR_IS_Q_OR_LATER = isQOrLater;
    }

    IdmapManager(Installer installer, OverlayManagerServiceImpl.PackageManagerHelper packageManager) {
        this.mInstaller = installer;
        this.mPackageManager = packageManager;
        lambda$connectToIdmap2d$0$IdmapManager();
    }

    /* access modifiers changed from: package-private */
    public boolean createIdmap(PackageInfo targetPackage, PackageInfo overlayPackage, int userId) {
        UserHandle.getSharedAppGid(targetPackage.applicationInfo.uid);
        String targetPath = targetPackage.applicationInfo.getBaseCodePath();
        String overlayPath = overlayPackage.applicationInfo.getBaseCodePath();
        try {
            int policies = calculateFulfilledPolicies(targetPackage, overlayPackage, userId);
            boolean enforce = enforceOverlayable(overlayPackage);
            try {
                if (this.mIdmap2Service.verifyIdmap(overlayPath, policies, enforce, userId)) {
                    return true;
                }
                if (this.mIdmap2Service.createIdmap(targetPath, overlayPath, policies, enforce, userId) != null) {
                    return true;
                }
                return false;
            } catch (Exception e) {
                e = e;
                Slog.w("OverlayManager", "failed to generate idmap for " + targetPath + " and " + overlayPath + ": " + e.getMessage());
                return false;
            }
        } catch (Exception e2) {
            e = e2;
            Slog.w("OverlayManager", "failed to generate idmap for " + targetPath + " and " + overlayPath + ": " + e.getMessage());
            return false;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean removeIdmap(OverlayInfo oi, int userId) {
        try {
            return this.mIdmap2Service.removeIdmap(oi.baseCodePath, userId);
        } catch (Exception e) {
            Slog.w("OverlayManager", "failed to remove idmap for " + oi.baseCodePath + ": " + e.getMessage());
            return false;
        }
    }

    /* access modifiers changed from: package-private */
    public boolean idmapExists(OverlayInfo oi) {
        return new File(getIdmapPath(oi.baseCodePath, oi.userId)).isFile();
    }

    /* access modifiers changed from: package-private */
    public boolean idmapExists(PackageInfo overlayPackage, int userId) {
        return new File(getIdmapPath(overlayPackage.applicationInfo.getBaseCodePath(), userId)).isFile();
    }

    private String getIdmapPath(String overlayPackagePath, int userId) {
        try {
            return this.mIdmap2Service.getIdmapPath(overlayPackagePath, userId);
        } catch (Exception e) {
            Slog.w("OverlayManager", "failed to get idmap path for " + overlayPackagePath + ": " + e.getMessage());
            return "";
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: connectToIdmap2d */
    public void lambda$connectToIdmap2d$0$IdmapManager() {
        IBinder binder = ServiceManager.getService("idmap");
        if (binder != null) {
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() {
                    /* class com.android.server.om.IdmapManager.AnonymousClass1 */

                    public void binderDied() {
                        Slog.w("OverlayManager", "service 'idmap' died; reconnecting...");
                        IdmapManager.this.lambda$connectToIdmap2d$0$IdmapManager();
                    }
                }, 0);
            } catch (RemoteException e) {
                binder = null;
            }
        }
        if (binder != null) {
            this.mIdmap2Service = IIdmap2.Stub.asInterface(binder);
            return;
        }
        Slog.w("OverlayManager", "service 'idmap' not found; trying again...");
        BackgroundThread.getHandler().postDelayed(new Runnable() {
            /* class com.android.server.om.$$Lambda$IdmapManager$CK7wBONETFX3KTlO4L5BDA9DNJk */

            public final void run() {
                IdmapManager.this.lambda$connectToIdmap2d$0$IdmapManager();
            }
        }, 1000);
    }

    private boolean enforceOverlayable(PackageInfo overlayPackage) {
        ApplicationInfo ai = overlayPackage.applicationInfo;
        if (ai.targetSdkVersion >= 29) {
            return true;
        }
        if (ai.isVendor()) {
            return VENDOR_IS_Q_OR_LATER;
        }
        if (ai.isSystemApp() || ai.isSignedWithPlatformKey()) {
            return false;
        }
        return true;
    }

    private int calculateFulfilledPolicies(PackageInfo targetPackage, PackageInfo overlayPackage, int userId) {
        ApplicationInfo ai = overlayPackage.applicationInfo;
        int fulfilledPolicies = 1;
        if (this.mPackageManager.signaturesMatching(targetPackage.packageName, overlayPackage.packageName, userId)) {
            fulfilledPolicies = 1 | 16;
        }
        if (ai.isVendor()) {
            return fulfilledPolicies | 4;
        }
        if (ai.isProduct()) {
            return fulfilledPolicies | 8;
        }
        if (ai.isOdm()) {
            return fulfilledPolicies | 32;
        }
        if (ai.isOem()) {
            return fulfilledPolicies | 64;
        }
        if (!ai.isProductServices() && ai.isSystemApp()) {
            return fulfilledPolicies | 2;
        }
        return fulfilledPolicies;
    }
}
