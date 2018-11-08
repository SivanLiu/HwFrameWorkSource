package com.android.server.pm;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser.Package;
import android.content.pm.Signature;
import android.os.Binder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import com.android.server.security.antimal.HwAntiMalStatus;

public final class HwPackageManagerServiceEx implements IHwPackageManagerServiceEx {
    private static final boolean IS_NOTCH_PROP = (!SystemProperties.get("ro.config.hw_notch_size", "").equals(""));
    private static final String MIDDLEWARE_LIMITED_DPC_PKGS = "com.huawei.mdm.dpc";
    private static final String SYSTEM_SIGN_STR = "30820405308202eda00302010202090083309550b47e0583300d06092a864886f70d0101050500308198310b300906035504061302434e3112301006035504080c094775616e67646f6e673112301006035504070c095368656e677a68656e310f300d060355040a0c0648756177656931183016060355040b0c0f5465726d696e616c436f6d70616e793114301206035504030c0b416e64726f69645465616d3120301e06092a864886f70d01090116116d6f62696c65406875617765692e636f6d301e170d3136303530353037333531345a170d3433303932313037333531345a308198310b300906035504061302434e3112301006035504080c094775616e67646f6e673112301006035504070c095368656e677a68656e310f300d060355040a0c0648756177656931183016060355040b0c0f5465726d696e616c436f6d70616e793114301206035504030c0b416e64726f69645465616d3120301e06092a864886f70d01090116116d6f62696c65406875617765692e636f6d30820122300d06092a864886f70d01010105000382010f003082010a0282010100c9fe1b699203091cb3944030cb1ba7996567182c1ce8be5535d673bc2025f37958e5bb1f4ed870dc229ffc2ed7d16f6cf10c08bc63f53624abe49db543518ef0069686ea5b3f129188652e87eca4b794df591828dd94de14b91ddbf2af156426453b8e739b12625a44b0895bfa1db3cdcce7db52f4d5af7c9918c325475c8273a5e4fe002e0f68082e9ec61d100913618982928ab5767701a8f576113c0810a4850a606233fd654531562bf8a74ac81bf8bacd66ca8a5ca9751f08e9575b402221e48e474f7f2dc91d02cfd87ceeaeb39ccf754cff5f1e8dfe23587955481bf0b8a386993edadc0f725e124f1ecedbef8d3cfbd6ddc783cde4b193f79fae05ed0203010001a350304e301d0603551d0e041604148d42132bfdc2ed970e25f5677cedd26f32527bc8301f0603551d230418301680148d42132bfdc2ed970e25f5677cedd26f32527bc8300c0603551d13040530030101ff300d06092a864886f70d010105050003820101003bc6e2ba8703a211222da8ed350e12cf31ac4d91290c5524da44626c382c8186f8238860b7ebddebba996f204802d72246d1326332ca85aff4a10cdaaa0d886016e26075c9b98799bf4767663d8c1097dccbc609dd3946f6431a35a71ee9ff3731c5b2715c158fe8d64c700b7e3e387e63a62e80ecdd4d007af242abed4b694d5a70d12dbde433fd18e1a7d033142f44cbe9ca187134830b86ecfa78ae2ff6d201014e4cf1d1655f40f4e4f4dd04af3c0416709dd159845d25515ff12f2854180e2ccbc1b05dffce93f9487839c126fa39f1453468a41eb7872b84c736dcb0d90a29775cd863707044f28bce4d05edcce4699605b27ae11e981590f87384726d";
    static final String TAG = "HwPackageManagerServiceEx";
    final Context mContext;
    private HwAntiMalStatus mHwAntiMalStatus = null;
    IHwPackageManagerInner mIPmsInner = null;

    public HwPackageManagerServiceEx(IHwPackageManagerInner pms, Context context) {
        this.mIPmsInner = pms;
        this.mContext = context;
        this.mHwAntiMalStatus = new HwAntiMalStatus(this.mContext);
    }

    public boolean isAllowedSetHomeActivityForAntiMal(PackageInfo pi, int userId) {
        if (this.mHwAntiMalStatus == null) {
            this.mHwAntiMalStatus = new HwAntiMalStatus(this.mContext);
        }
        return this.mHwAntiMalStatus.isAllowedSetHomeActivityForAntiMal(pi, userId);
    }

    public void updateNochScreenWhite(String packageName, String flag, int versionCode) {
        if (IS_NOTCH_PROP) {
            HwNotchScreenWhiteConfig.getInstance().updateVersionCodeInNoch(packageName, flag, versionCode);
            if ("removed".equals(flag)) {
                HwNotchScreenWhiteConfig.getInstance().removeAppUseNotchMode(packageName);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getAppUseNotchMode(String packageName) {
        if (!IS_NOTCH_PROP) {
            return -1;
        }
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) == 1000 || uid == 0) {
            long callingId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mIPmsInner.getPackagesLock()) {
                    PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                    if (pkgSetting != null) {
                        int appUseNotchMode = pkgSetting.getAppUseNotchMode();
                        Binder.restoreCallingIdentity(callingId);
                        return appUseNotchMode;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        } else {
            throw new SecurityException("Only the system can get app use notch mode");
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setAppUseNotchMode(String packageName, int mode) {
        if (IS_NOTCH_PROP) {
            int uid = Binder.getCallingUid();
            if (UserHandle.getAppId(uid) == 1000 || uid == 0) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    synchronized (this.mIPmsInner.getPackagesLock()) {
                        PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                        if (pkgSetting != null) {
                            if (pkgSetting.getAppUseNotchMode() != mode) {
                                pkgSetting.setAppUseNotchMode(mode);
                                this.mIPmsInner.getSettings().writeLPr();
                                HwNotchScreenWhiteConfig.getInstance().updateAppUseNotchMode(packageName, mode);
                            }
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            } else {
                throw new SecurityException("Only the system can set app use notch mode");
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isDisallowedInstallApk(Package pkg) {
        boolean z = true;
        if (pkg == null || TextUtils.isEmpty(pkg.packageName) || !MIDDLEWARE_LIMITED_DPC_PKGS.equals(pkg.packageName)) {
            return false;
        }
        if (compareSignatures(new Signature[]{new Signature(SYSTEM_SIGN_STR)}, pkg.mSignatures) == 0) {
            z = false;
        }
        return z;
    }

    private int compareSignatures(Signature[] s1, Signature[] s2) {
        int i = 1;
        if (s1 == null) {
            if (s2 != null) {
                i = -1;
            }
            return i;
        } else if (s2 == null) {
            return -2;
        } else {
            if (s1.length != s2.length) {
                return -3;
            }
            if (s1.length == 1) {
                return s1[0].equals(s2[0]) ? 0 : -3;
            }
            ArraySet<Signature> set1 = new ArraySet();
            for (Signature sig : s1) {
                set1.add(sig);
            }
            ArraySet<Signature> set2 = new ArraySet();
            for (Signature sig2 : s2) {
                set2.add(sig2);
            }
            return set1.equals(set2) ? 0 : -3;
        }
    }
}
