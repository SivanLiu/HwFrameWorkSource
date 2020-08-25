package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.HwPackageParser;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ParceledListSlice;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.pm.VersionedPackage;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Log;
import android.util.Slog;
import android.util.SplitNotificationUtils;
import android.util.Xml;
import android.view.WindowManagerGlobal;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.am.HwActivityManagerService;
import com.android.server.cota.CotaInstallImpl;
import com.android.server.cota.CotaService;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import com.android.server.os.HwBootFail;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.auth.HwCertificationManager;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.huawei.cust.HwCustUtils;
import com.huawei.server.pm.antimal.AntiMalPreInstallScanner;
import huawei.com.android.server.security.fileprotect.HwAppAuthManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwPackageManagerService extends PackageManagerService {
    public static final boolean APK_INSTALL_FOREVER = SystemProperties.getBoolean("ro.config.apkinstallforever", false);
    private static final String CLONE_APP_LIST = "hw_clone_app_list.xml";
    private static final int COTA_APP_INSTALLING = -1;
    private static final int COTA_APP_INSTALL_FIAL = 0;
    private static final int COTA_APP_INSTALL_ILLEGAL = -3;
    private static final int COTA_APP_INSTALL_INIT = -2;
    private static final int COTA_APP_INSTALL_SUCCESS = 1;
    private static final String COTA_APP_UPDATE_APPWIDGET = "huawei.intent.action.UPDATE_COTA_APP_WIDGET";
    private static final String COTA_APP_UPDATE_APPWIDGET_EXTRA = "huawei.intent.extra.cota_package_list";
    private static final String COTA_PMS = "cota_pms";
    private static final String COTA_UPDATE_FLAG_FAIL = "_2";
    private static final String COTA_UPDATE_FLAG_INIT = "_0";
    private static final String COTA_UPDATE_FLAG_SUCCESS = "_1";
    private static final boolean DEBUG = SystemProperties.get("ro.dbg.pms_log", "0").equals("on");
    private static final String DESCRIPTOR = "huawei.com.android.server.IPackageManager";
    private static final String FLAG_APK_NOSYS = "nosys";
    private static final String FLAG_APK_PRIV = "priv";
    private static final String FLAG_APK_SYS = "sys";
    private static final String KEY_HWPMS_ERROR_LOG = "persist.sys.hwpms_error_log";
    private static final String KEY_HWPMS_ERROR_REBOOT_COUNT = "persist.sys.hwpms_error_reboot_count";
    private static final String[] LIMITED_PACKAGE_NAMES = {"com.huawei.android.totemweather", "com.huawei.camera", "com.android.calendar", "com.huawei.calendar", "com.android.soundrecorder", "com.huawei.soundrecorder"};
    private static final String[] LIMITED_TARGET_PACKAGE_NAMES = {"com.google.android.wearable.app.cn", "com.google.android.wearable.app"};
    private static final int MAX_HWPMS_ERROR_REBOOT_COUNT = 3;
    private static int MAX_PKG = 100;
    public static final boolean SUPPORT_HW_COTA = SystemProperties.getBoolean("ro.config.hw_cota", false);
    public static final String SYSDLL_PATH = "xml/APKInstallListEMUI5Release_732999.txt";
    private static final String TAG = "HwPackageManagerService";
    public static final int TRANSACTION_CODE_GET_APP_TYPE = 1023;
    public static final int TRANSACTION_CODE_GET_HDB_KEY = 1011;
    public static final int TRANSACTION_CODE_GET_IM_AND_VIDEO_APP_LIST = 1022;
    public static final int TRANSACTION_CODE_IS_NOTIFICATION_SPLIT = 1021;
    private static HashMap<String, HashSet<String>> mCotaDelInstallMap = null;
    private static HashMap<String, HashSet<String>> mCotaInstallMap = null;
    private static HwCustPackageManagerService mCustPackageManagerService = ((HwCustPackageManagerService) HwCustUtils.createObj(HwCustPackageManagerService.class, new Object[0]));
    static final ArrayList<String> mDexoptInBootupApps = new ArrayList<>();
    static final ArrayList<String> mForceNotDexApps = new ArrayList<>();
    private static HwPackageManagerService mHwPackageManagerService = null;
    private static final Set<String> sSupportCloneApps = new HashSet();
    public static final int transaction_pmCheckGranted = 1005;
    public static final int transaction_sendLimitedPackageBroadcast = 1006;
    public static final int transaction_setEnabledVisitorSetting = 1001;
    private HandlerThread mCommonHandlerThread = null;
    private ComponentChangeMonitor mComponentChangeMonitor = null;
    /* access modifiers changed from: private */
    public int mCotaApksInstallStatus = -2;
    private CotaInstallImpl.CotaInstallCallBack mCotaInstallCallBack = new CotaInstallImpl.CotaInstallCallBack() {
        /* class com.android.server.pm.HwPackageManagerService.AnonymousClass1 */

        @Override // com.android.server.cota.CotaInstallImpl.CotaInstallCallBack
        public void startAutoInstall(String apkInstallConfig, String removableApkInstallConfig, String strMccMnc) {
            HotInstall.getInstance().realStartAutoInstall(HwPackageManagerService.this.mContext, HwPackageManagerService.this.getHwPMSEx(), apkInstallConfig, removableApkInstallConfig, strMccMnc);
        }

        @Override // com.android.server.cota.CotaInstallImpl.CotaInstallCallBack
        public void startInstall() {
            if (HwPackageManagerService.SUPPORT_HW_COTA || HwPackageManagerService.APK_INSTALL_FOREVER) {
                Log.i(HwPackageManagerService.TAG, "startInstallCotaApks()");
                HwPackageManagerService.this.startInstallCotaApks();
            }
        }

        @Override // com.android.server.cota.CotaInstallImpl.CotaInstallCallBack
        public int getStatus() {
            if (!HwPackageManagerService.SUPPORT_HW_COTA && !HwPackageManagerService.APK_INSTALL_FOREVER) {
                return HwPackageManagerService.this.mCotaApksInstallStatus;
            }
            Log.i(HwPackageManagerService.TAG, "getStatus()");
            return HwPackageManagerService.this.getCotaStatus();
        }
    };
    private String mCotaUpdateFlag = "";
    private HashSet<String> mGrantedInstalledPkg = new HashSet<>();

    public static synchronized PackageManagerService getInstance(Context context, Installer installer, boolean factoryTest, boolean onlyCore) {
        synchronized (HwPackageManagerService.class) {
            File systemDir = new File(Environment.getDataDirectory(), "system");
            File packagesXml = new File(systemDir, "packages.xml");
            boolean isPackagesXmlExisted = packagesXml.exists();
            File packagesBackupXml = new File(systemDir, "packages-backup.xml");
            boolean isPackagesBackupXmlExisted = packagesBackupXml.exists();
            if (mHwPackageManagerService == null) {
                HwPackageManagerServiceEx.initCustStoppedApps();
                MultiWinWhiteListManager.getInstance().loadMultiWinWhiteList(context);
                initCloneAppsFromCust();
                try {
                    if (SystemProperties.getInt(KEY_HWPMS_ERROR_REBOOT_COUNT, 0) == 0 && (isPackagesXmlExisted || isPackagesBackupXmlExisted)) {
                        SystemProperties.set(KEY_HWPMS_ERROR_REBOOT_COUNT, "0");
                        SystemProperties.set(KEY_HWPMS_ERROR_LOG, "");
                    }
                    mHwPackageManagerService = new HwPackageManagerService(context, installer, factoryTest, onlyCore);
                    mHwPackageManagerService.deleteNonSupportedAppsForClone();
                    if (HwPackageParser.getIsNeedBootUpdate()) {
                        mHwPackageManagerService.getHwPMSEx().updateWhitelistByHot();
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "Error while package manager initializing! For:", e);
                    reportPmsInitError(e);
                }
            }
            if (mHwPackageManagerService == null) {
                if (!checkCanRebootForHwPmsError()) {
                    return mHwPackageManagerService;
                }
                if (isPackagesXmlExisted) {
                    boolean result = packagesXml.delete();
                    Settings.setPackageSettingsError();
                    Slog.e(TAG, "something may be missed in packages.xml, delete the file :" + result);
                }
                if (isPackagesBackupXmlExisted) {
                    boolean isSucess = packagesBackupXml.delete();
                    Settings.setPackageSettingsError();
                    Slog.e(TAG, "something may be missed in packages_backup.xml, delete the file :" + isSucess);
                }
                int times = SystemProperties.getInt(KEY_HWPMS_ERROR_REBOOT_COUNT, 0);
                StringBuilder sb = new StringBuilder();
                sb.append("repair");
                sb.append(times - 1);
                SystemProperties.set(KEY_HWPMS_ERROR_LOG, sb.toString());
                try {
                    HwBootFail.bootFailError(83886081, 0, "");
                } catch (Exception e2) {
                    Slog.e(TAG, "getInstance, try to reboot error");
                }
            }
            return mHwPackageManagerService;
        }
    }

    private static void reportPmsInitError(Exception ex) {
        int times = SystemProperties.getInt(KEY_HWPMS_ERROR_REBOOT_COUNT, 0);
        StackTraceElement[] frames = ex.getStackTrace();
        String errorLineInfo = "can't get error line info";
        if (frames != null && frames.length > 0) {
            errorLineInfo = frames[0].toString();
        }
        String exceptionName = ex.getClass().toString();
        Slog.i(TAG, "Error while package manager initializing, occur to " + exceptionName + " the error line info:" + errorLineInfo + ", reboot " + times + " times.");
        HwPackageManagerServiceUtils.reportPmsInitException((String) null, times, exceptionName, errorLineInfo);
    }

    private static boolean checkCanRebootForHwPmsError() {
        boolean isReboot = false;
        int times = SystemProperties.getInt(KEY_HWPMS_ERROR_REBOOT_COUNT, 3);
        if (times > 0 && times < 3) {
            isReboot = true;
        }
        SystemProperties.set(KEY_HWPMS_ERROR_REBOOT_COUNT, (times + 1) + "");
        return isReboot;
    }

    private static void resetHwPmsErrorRebootCount() {
        SystemProperties.set(KEY_HWPMS_ERROR_REBOOT_COUNT, "0");
    }

    public HwPackageManagerService(Context context, Installer installer, boolean factoryTest, boolean onlyCore) {
        super(context, installer, factoryTest, onlyCore);
        getHwPMSEx().recordUninstalledDelapp((String) null, (String) null);
        getHwPMSEx().getOldDataBackup().clear();
        if (!SystemProperties.getBoolean("ro.config.hwcompmonitorthread.disable", false)) {
            this.mCommonHandlerThread = new HandlerThread(TAG);
            this.mCommonHandlerThread.start();
            this.mComponentChangeMonitor = new ComponentChangeMonitor(context, this.mCommonHandlerThread.getLooper());
        }
    }

    private void setEnabledVisitorSetting(int newState, int flags, String callingPackage, int userId) {
        String callingPackage2;
        String componentName;
        boolean z;
        PackageSetting pkgSetting;
        int i = newState;
        if (i == 0 || i == 1 || i == 2 || i == 3 || i == 4) {
            int packageUid = -1;
            if (callingPackage == null) {
                callingPackage2 = Integer.toString(Binder.getCallingUid());
            } else {
                callingPackage2 = callingPackage;
            }
            HashMap<String, ArrayList<String>> componentsMap = new HashMap<>();
            HashMap<String, Integer> pkgMap = new HashMap<>();
            String pkgNameList = Settings.Secure.getString(this.mContext.getContentResolver(), "privacy_app_list");
            if (pkgNameList == null) {
                Slog.e(TAG, " pkgNameList = null ");
            } else if (pkgNameList.equals("")) {
                Slog.e(TAG, " pkgNameList is null");
            } else {
                if (DEBUG) {
                    Slog.e(TAG, " pkgNameList =   " + pkgNameList);
                }
                String[] pkgNameArray = pkgNameList.contains(";") ? pkgNameList.split(";") : new String[]{pkgNameList};
                PackageSetting packageSetting = null;
                int i2 = 0;
                boolean sendNow = false;
                while (i2 < MAX_PKG && pkgNameArray != null && i2 < pkgNameArray.length) {
                    String packageName = pkgNameArray[i2];
                    synchronized (this.mPackages) {
                        try {
                            PackageSetting pkgSetting2 = (PackageSetting) this.mSettings.mPackages.get(packageName);
                            if (pkgSetting2 == null) {
                                try {
                                    pkgMap.put(packageName, 1);
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } else {
                                try {
                                    if (pkgSetting2.getEnabled(userId) == i) {
                                        pkgMap.put(packageName, 1);
                                    } else {
                                        if (i == 0 || i == 1) {
                                            callingPackage2 = null;
                                        }
                                        pkgSetting2.setEnabled(i, userId, callingPackage2);
                                        z = false;
                                        pkgMap.put(packageName, 0);
                                        ArrayList<String> components = this.mPendingBroadcasts.get(userId, packageName);
                                        boolean newPackage = components == null;
                                        if (newPackage) {
                                            components = new ArrayList<>();
                                        }
                                        if (!components.contains(packageName)) {
                                            components.add(packageName);
                                        }
                                        componentsMap.put(packageName, components);
                                        if ((flags & 1) == 0) {
                                            sendNow = true;
                                            this.mPendingBroadcasts.remove(userId, packageName);
                                            componentName = packageName;
                                            pkgSetting = pkgSetting2;
                                        } else {
                                            if (newPackage) {
                                                this.mPendingBroadcasts.put(userId, packageName, components);
                                            }
                                            if (!this.mHandler.hasMessages(1)) {
                                                componentName = packageName;
                                                pkgSetting = pkgSetting2;
                                                this.mHandler.sendEmptyMessageDelayed(1, 1000);
                                            } else {
                                                componentName = packageName;
                                                pkgSetting = pkgSetting2;
                                            }
                                        }
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    throw th;
                                }
                            }
                            componentName = packageName;
                            pkgSetting = pkgSetting2;
                            z = false;
                            i2++;
                            i = newState;
                            packageSetting = pkgSetting;
                            packageUid = packageUid;
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                }
                this.mSettings.writePackageRestrictionsLPr(userId);
                int i3 = 0;
                while (i3 < MAX_PKG && pkgNameArray != null && i3 < pkgNameArray.length) {
                    String packageName2 = pkgNameArray[i3];
                    if (pkgMap.get(packageName2).intValue() != 1) {
                        PackageSetting pkgSetting3 = (PackageSetting) this.mSettings.mPackages.get(packageName2);
                        if (pkgSetting3 != null && componentsMap.get(packageName2) != null && sendNow) {
                            sendPackageChangedBroadcast(packageName2, (flags & 1) != 0, componentsMap.get(packageName2), UserHandle.getUid(userId, pkgSetting3.appId));
                        }
                    }
                    i3++;
                }
            }
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        Bundle extras = null;
        if (code == 1001) {
            Slog.w(TAG, "onTransact");
            data.enforceInterface(DESCRIPTOR);
            setEnabledVisitorSetting(data.readInt(), data.readInt(), null, data.readInt());
            reply.writeNoException();
            return true;
        } else if (code == 1011) {
            data.enforceInterface(DESCRIPTOR);
            reply.writeNoException();
            reply.writeString(HwAdbManager.getHdbKey());
            return true;
        } else if (code == 1005) {
            data.enforceInterface(DESCRIPTOR);
            boolean checkInstallGranted = checkInstallGranted(data.readString());
            reply.writeNoException();
            reply.writeInt(checkInstallGranted ? 1 : 0);
            return true;
        } else if (code != 1006) {
            switch (code) {
                case 1021:
                    data.enforceInterface(DESCRIPTOR);
                    boolean isNotificationAddSplitButton = isNotificationAddSplitButton(data.readString());
                    reply.writeNoException();
                    reply.writeInt(isNotificationAddSplitButton ? 1 : 0);
                    return true;
                case 1022:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> list = getSupportSplitScreenApps();
                    reply.writeNoException();
                    reply.writeStringList(list);
                    return true;
                case 1023:
                    data.enforceInterface(DESCRIPTOR);
                    int[] appType = getApplicationType(data);
                    reply.writeNoException();
                    reply.writeIntArray(appType);
                    return true;
                default:
                    return HwPackageManagerService.super.onTransact(code, data, reply, flags);
            }
        } else {
            data.enforceInterface(DESCRIPTOR);
            String action = data.readString();
            String pkg = data.readString();
            if (data.readInt() != 0) {
                extras = (Bundle) Bundle.CREATOR.createFromParcel(data);
            }
            sendLimitedPackageBroadcast(action, pkg, extras, data.readString(), data.createIntArray());
            reply.writeNoException();
            return true;
        }
    }

    private void addTempCotaPartitionApkToHashMap(ArrayList<File> apkInstallList, ArrayList<File> apkDelInstallList) {
        HashMap<String, HashSet<String>> hashMap = mCotaInstallMap;
        if (hashMap == null) {
            mCotaInstallMap = new HashMap<>();
        } else {
            hashMap.clear();
        }
        if (apkInstallList != null) {
            HashSet<String> sysInstallSet = new HashSet<>();
            HashSet<String> privInstallSet = new HashSet<>();
            mCotaInstallMap.put(FLAG_APK_SYS, sysInstallSet);
            mCotaInstallMap.put(FLAG_APK_PRIV, privInstallSet);
            getHwPMSEx().getAPKInstallListForHwPMS(apkInstallList, mCotaInstallMap);
        }
        HashMap<String, HashSet<String>> hashMap2 = mCotaDelInstallMap;
        if (hashMap2 == null) {
            mCotaDelInstallMap = new HashMap<>();
        } else {
            hashMap2.clear();
        }
        if (apkDelInstallList != null) {
            HashSet<String> sysDelInstallSet = new HashSet<>();
            HashSet<String> privDelInstallSet = new HashSet<>();
            HashSet<String> noSysDelInstallSet = new HashSet<>();
            mCotaDelInstallMap.put(FLAG_APK_SYS, sysDelInstallSet);
            mCotaDelInstallMap.put(FLAG_APK_PRIV, privDelInstallSet);
            mCotaDelInstallMap.put(FLAG_APK_NOSYS, noSysDelInstallSet);
            getHwPMSEx().getAPKInstallListForHwPMS(apkDelInstallList, mCotaDelInstallMap);
            HwPackageManagerServiceUtils.setCotaDelInstallMap(mCotaDelInstallMap);
        }
    }

    private void scanTempCotaPartitionDir(int scanMode) {
        if (!mCotaInstallMap.isEmpty()) {
            getHwPMSEx().installAPKforInstallListForHwPMS(mCotaInstallMap.get(FLAG_APK_SYS), 16, scanMode | 131072, 0, 0);
            getHwPMSEx().installAPKforInstallListForHwPMS(mCotaInstallMap.get(FLAG_APK_PRIV), 16, scanMode | 131072 | 262144, 0, 0);
        }
        if (!mCotaDelInstallMap.isEmpty()) {
            getHwPMSEx().installAPKforInstallListForHwPMS(mCotaDelInstallMap.get(FLAG_APK_SYS), 16, scanMode | 131072, 0, 33554432);
            getHwPMSEx().installAPKforInstallListForHwPMS(mCotaDelInstallMap.get(FLAG_APK_PRIV), 16, scanMode | 131072 | 262144, 0, 33554432);
            getHwPMSEx().installAPKforInstallListForHwPMS(mCotaDelInstallMap.get(FLAG_APK_NOSYS), 0, scanMode, 0, 33554432);
        }
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: private */
    public void startInstallCotaApks() {
        int[] iArr;
        this.mCotaFlag = true;
        getHwPMSEx().deleteExistsIfNeedForHwPMS();
        try {
            this.mCotaUpdateFlag = Settings.Global.getString(this.mContext.getContentResolver(), "cota_update_flag");
        } catch (Exception e) {
            Log.e(TAG, "startInstallCotaApks read cota_update_flag from global exception");
        }
        int i = 0;
        if (SUPPORT_HW_COTA) {
            ArrayList<ArrayList<File>> apkInstallList = HwPackageManagerServiceEx.getCotaApkInstallXMLPath();
            addTempCotaPartitionApkToHashMap(apkInstallList.get(0), apkInstallList.get(1));
        }
        if (APK_INSTALL_FOREVER) {
            addTempCotaPartitionApkToHashMap(HwPackageManagerServiceEx.getSysdllInstallXMLPath(), new ArrayList<>());
        }
        long beginCotaScanTime = System.currentTimeMillis();
        scanTempCotaPartitionDir(8720);
        long endCotaScanTime = System.currentTimeMillis();
        Log.i(TAG, "scanTempCotaPartitionDir take time is " + (endCotaScanTime - beginCotaScanTime));
        updateAllSharedLibrariesLocked(null, Collections.unmodifiableMap(this.mPackages));
        SystemConfig.getInstance().readCustPermissions();
        this.mPermissionManager.updateAllPermissions(StorageManager.UUID_PRIVATE_INTERNAL, false, this.mTempPkgList, this.mPermissionCallback);
        int pksSize = this.mTempPkgList.size();
        for (int i2 = 0; i2 < pksSize; i2++) {
            prepareAppDataAfterInstallLIF((PackageParser.Package) this.mTempPkgList.get(i2));
        }
        this.mSettings.writeLPr();
        Intent cotaintent = new Intent(COTA_APP_UPDATE_APPWIDGET);
        Bundle extras = new Bundle();
        String[] pkgList = new String[pksSize];
        for (int j = 0; j < pksSize; j++) {
            pkgList[j] = ((PackageParser.Package) this.mTempPkgList.get(j)).packageName;
        }
        extras.putStringArray(COTA_APP_UPDATE_APPWIDGET_EXTRA, pkgList);
        cotaintent.addFlags(268435456);
        cotaintent.putExtras(extras);
        cotaintent.putExtra("android.intent.extra.user_handle", 0);
        this.mContext.sendBroadcast(cotaintent);
        sendPreBootBroadcastToManagedProvisioning();
        long identity = Binder.clearCallingIdentity();
        try {
            int[] userIds = UserManagerService.getInstance().getUserIds();
            int length = userIds.length;
            while (i < length) {
                int userId = userIds[i];
                if (this.mDefaultPermissionPolicy != null) {
                    StringBuilder sb = new StringBuilder();
                    iArr = userIds;
                    sb.append("Cota apps have installed ,grantCustDefaultPermissions userId = ");
                    sb.append(userId);
                    Log.i(TAG, sb.toString());
                    this.mDefaultPermissionPolicy.grantCustDefaultPermissions(userId);
                } else {
                    iArr = userIds;
                }
                i++;
                userIds = iArr;
            }
            Binder.restoreCallingIdentity(identity);
            this.mCotaApksInstallStatus = 1;
            if (CotaService.getICotaCallBack() != null) {
                try {
                    Log.i(TAG, "isCotaAppsInstallFinish = " + getCotaStatus());
                    CotaService.getICotaCallBack().onAppInstallFinish(getCotaStatus());
                } catch (Exception e2) {
                    Log.w(TAG, "onAppInstallFinish error");
                }
            }
            if (SUPPORT_HW_COTA) {
                saveCotaPmsToDB(getCotaStatus());
            }
            this.mCotaFlag = false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    /* access modifiers changed from: private */
    public int getCotaStatus() {
        return this.mCotaApksInstallStatus;
    }

    private void sendPreBootBroadcastToManagedProvisioning() {
        Intent preBootBroadcastIntent = new Intent("android.intent.action.PRE_BOOT_COMPLETED");
        preBootBroadcastIntent.addFlags(268435456);
        preBootBroadcastIntent.setComponent(new ComponentName("com.android.managedprovisioning", "com.android.managedprovisioning.ota.PreBootListener"));
        this.mContext.sendBroadcast(preBootBroadcastIntent);
    }

    private void saveCotaPmsToDB(int state) {
        String cotaPMSFlag;
        if (state == -2) {
            cotaPMSFlag = COTA_UPDATE_FLAG_INIT;
        } else if (state == 1) {
            cotaPMSFlag = COTA_UPDATE_FLAG_SUCCESS;
        } else {
            cotaPMSFlag = COTA_UPDATE_FLAG_FAIL;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            Settings.Global.putString(contentResolver, COTA_PMS, this.mCotaUpdateFlag + cotaPMSFlag);
            Log.i(TAG, "startInstallCotaApks set COTA_PMS= " + this.mCotaUpdateFlag + cotaPMSFlag);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void systemReady() {
        HwPackageManagerService.super.systemReady();
        AntiMalPreInstallScanner.getInstance().systemReady();
        getHwPMSEx().writePreinstalledApkListToFile();
        getHwPMSEx().createPublicityFile();
        CotaInstallImpl.getInstance().registInstallCallBack(this.mCotaInstallCallBack);
        if (!TextUtils.isEmpty(SystemProperties.get("ro.config.hw_notch_size", ""))) {
            for (PackageSetting ps : this.mSettings.mPackages.values()) {
                if (ps.getAppUseNotchMode() > 0) {
                    HwNotchScreenWhiteConfig.getInstance().updateAppUseNotchMode(ps.pkg.packageName, ps.getAppUseNotchMode());
                }
            }
        }
        resetHwPmsErrorRebootCount();
        HwAppAuthManager.getInstance().notifyPMSReady(this.mContext);
        HwForceDarkModeConfig.getInstance().registeAppTypeRecoReceiver();
    }

    /* access modifiers changed from: protected */
    public void checkHwCertification(PackageParser.Package pkg, boolean isUpdate) {
        if (HwCertificationManager.hasFeature()) {
            if (!HwCertificationManager.isSupportHwCertification(pkg)) {
                if (isContainHwCertification(pkg)) {
                    hwCertCleanUp(pkg);
                }
            } else if (isUpdate || !isContainHwCertification(pkg) || isUpgrade()) {
                Slog.i("HwCertificationManager", "will checkCertificationInner,isUpdate = " + isUpdate + "isHotaUpGrade = " + isUpgrade());
                hwCertCleanUp(pkg);
                checkCertificationInner(pkg);
            }
        }
    }

    private void checkCertificationInner(PackageParser.Package pkg) {
        HwCertificationManager manager = HwCertificationManager.getIntance();
        if (manager != null && !manager.checkHwCertification(pkg)) {
            Slog.e("HwCertificationManager", "checkHwCertification parse error");
        }
    }

    /* access modifiers changed from: protected */
    public boolean getHwCertificationPermission(boolean allowed, PackageParser.Package pkg, String perm) {
        if (!HwCertificationManager.hasFeature()) {
            return allowed;
        }
        if (!HwCertificationManager.isInitialized()) {
            HwCertificationManager.initialize(this.mContext);
        }
        HwCertificationManager manager = HwCertificationManager.getIntance();
        if (manager == null) {
            return allowed;
        }
        return manager.getHwCertificationPermission(allowed, pkg, perm);
    }

    private void hwCertCleanUp(PackageParser.Package pkg) {
        HwCertificationManager manager = HwCertificationManager.getIntance();
        if (manager != null) {
            manager.cleanUp(pkg);
        }
    }

    /* access modifiers changed from: protected */
    public void hwCertCleanUp() {
        if (HwCertificationManager.hasFeature()) {
            if (!HwCertificationManager.isInitialized()) {
                HwCertificationManager.initialize(this.mContext);
            }
            HwCertificationManager manager = HwCertificationManager.getIntance();
            if (manager != null) {
                manager.cleanUp();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void installStage(ActiveInstallSession activeInstallSession) {
        if (activeInstallSession.mUser != null) {
            activeInstallSession.mUser = new UserHandle(redirectInstallForClone(activeInstallSession.mUser.getIdentifier()));
        }
        HwPackageManagerService.super.installStage(activeInstallSession);
    }

    /* access modifiers changed from: protected */
    public void initHwCertificationManager() {
        if (!HwCertificationManager.isInitialized()) {
            HwCertificationManager.initialize(this.mContext);
        }
        HwCertificationManager.getIntance();
    }

    /* access modifiers changed from: protected */
    public int getHwCertificateType(PackageParser.Package pkg) {
        if (!HwCertificationManager.isSupportHwCertification(pkg)) {
            return HwCertificationManager.getIntance().getHwCertificateTypeNotMDM();
        }
        return HwCertificationManager.getIntance().getHwCertificateType(pkg.packageName);
    }

    /* access modifiers changed from: protected */
    public boolean isContainHwCertification(PackageParser.Package pkg) {
        return HwCertificationManager.getIntance().isContainHwCertification(pkg.packageName);
    }

    /* access modifiers changed from: protected */
    public void addGrantedInstalledPkg(String pkgName, boolean grant) {
        if (grant) {
            synchronized (this.mGrantedInstalledPkg) {
                Slog.i(TAG, "onReceive() package added:" + pkgName);
                this.mGrantedInstalledPkg.add(pkgName);
            }
        }
    }

    private boolean checkInstallGranted(String pkgName) {
        boolean contains;
        synchronized (this.mGrantedInstalledPkg) {
            contains = this.mGrantedInstalledPkg.contains(pkgName);
        }
        return contains;
    }

    private boolean checkLimitePackageBroadcast(String action, String pkg, String targetPkg) {
        String[] callingPkgNames = getPackagesForUid(Binder.getCallingUid());
        if (callingPkgNames == null || callingPkgNames.length <= 0) {
            Flog.i(205, "Android Wear-checkLimitePackageBroadcast: callingPkgNames is empty");
            return false;
        }
        String callingPkgName = callingPkgNames[0];
        Flog.d(205, "Android Wear-checkLimitePackageBroadcast: callingPkgName = " + callingPkgName);
        if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action)) {
            boolean targetPkgExist = false;
            String[] strArr = LIMITED_TARGET_PACKAGE_NAMES;
            int length = strArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                } else if (strArr[i].equals(targetPkg)) {
                    targetPkgExist = true;
                    break;
                } else {
                    i++;
                }
            }
            if (!targetPkgExist) {
                Flog.i(205, "Android Wear-checkLimitePackageBroadcast: targetPkg is not permitted");
                return false;
            }
            boolean pkgExist = false;
            String[] strArr2 = LIMITED_PACKAGE_NAMES;
            int length2 = strArr2.length;
            int i2 = 0;
            while (true) {
                if (i2 >= length2) {
                    break;
                } else if (strArr2[i2].equals(pkg)) {
                    pkgExist = true;
                    break;
                } else {
                    i2++;
                }
            }
            if (!pkgExist) {
                Flog.i(205, "Android Wear-checkLimitePackageBroadcast: pkg is not permitted");
                return false;
            } else if (!isSystemApp(getApplicationInfo(callingPkgName, 0, this.mContext.getUserId()))) {
                Flog.i(205, "Android Wear-checkLimitePackageBroadcast: is not System App.");
                return false;
            } else {
                Flog.d(205, "Android Wear-checkLimitePackageBroadcast: success");
                return true;
            }
        } else {
            Flog.i(205, "Android Wear-checkLimitePackageBroadcast: action is not permitted");
            return false;
        }
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        boolean bSystemApp = false;
        if (appInfo != null) {
            boolean z = true;
            if ((appInfo.flags & 1) == 0) {
                z = false;
            }
            bSystemApp = z;
        }
        Flog.d(205, "Android Wear-checkLimitePackageBroadcast: bSystemApp=" + bSystemApp);
        return bSystemApp;
    }

    private void sendLimitedPackageBroadcast(final String action, final String pkg, final Bundle extras, final String targetPkg, final int[] userIds) {
        if (checkLimitePackageBroadcast(action, pkg, targetPkg)) {
            this.mHandler.post(new Runnable() {
                /* class com.android.server.pm.HwPackageManagerService.AnonymousClass2 */

                public void run() {
                    int[] resolvedUserIds;
                    try {
                        IActivityManager am = ActivityManagerNative.getDefault();
                        if (am != null) {
                            if (userIds == null) {
                                resolvedUserIds = am.getRunningUserIds();
                            } else {
                                resolvedUserIds = userIds;
                            }
                            int length = resolvedUserIds.length;
                            int i = 0;
                            while (i < length) {
                                int id = resolvedUserIds[i];
                                String str = action;
                                Uri uri = null;
                                if (pkg != null) {
                                    uri = Uri.fromParts("package", pkg, null);
                                }
                                Intent intent = new Intent(str, uri);
                                if (extras != null) {
                                    intent.putExtras(extras);
                                }
                                if (targetPkg != null) {
                                    intent.setPackage(targetPkg);
                                }
                                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                                if (uid > 0 && UserHandle.getUserId(uid) != id) {
                                    intent.putExtra("android.intent.extra.UID", UserHandle.getUid(id, UserHandle.getAppId(uid)));
                                }
                                intent.putExtra("android.intent.extra.user_handle", id);
                                am.broadcastIntent((IApplicationThread) null, intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, (String[]) null, -1, (Bundle) null, false, false, id);
                                i++;
                                length = length;
                                resolvedUserIds = resolvedUserIds;
                            }
                        }
                    } catch (RemoteException e) {
                    }
                }
            });
            return;
        }
        throw new SecurityException("sendLimitedPackageBroadcast: checkLimitePackageBroadcast failed");
    }

    public int checkPermission(String permName, String pkgName, int userId) {
        if (userId == 0 || !HwActivityManagerService.IS_SUPPORT_CLONE_APP || ((!"android.permission.INTERACT_ACROSS_USERS_FULL".equals(permName) && !"android.permission.INTERACT_ACROSS_USERS".equals(permName)) || !sUserManager.isClonedProfile(userId))) {
            return HwPackageManagerService.super.checkPermission(permName, pkgName, userId);
        }
        return 0;
    }

    public int checkUidPermission(String permName, int uid) {
        if (UserHandle.getUserId(uid) == 0 || !HwActivityManagerService.IS_SUPPORT_CLONE_APP || !sUserManager.isClonedProfile(UserHandle.getUserId(uid)) || (!"android.permission.INTERACT_ACROSS_USERS_FULL".equals(permName) && !"android.permission.INTERACT_ACROSS_USERS".equals(permName))) {
            return HwPackageManagerService.super.checkUidPermission(permName, uid);
        }
        return 0;
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:127:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:128:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x01d2, code lost:
        r0 = new android.content.Intent("android.intent.action.MAIN");
        r0.addCategory("android.intent.category.LAUNCHER");
        r12 = queryIntentActivities(r0, r0.resolveTypeIfNeeded(r21.mContext.getContentResolver()), 786432, r22);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x01ef, code lost:
        if (r12 == null) goto L_0x0239;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x01f1, code lost:
        r0 = r12.getList().iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x01fd, code lost:
        if (r0.hasNext() == false) goto L_0x0239;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:0x01ff, code lost:
        r13 = (android.content.pm.ResolveInfo) r0.next();
        r7 = r13.activityInfo.getComponentName();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:57:0x0213, code lost:
        if (r0.contains(r13.activityInfo.packageName) == false) goto L_0x01f9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x0215, code lost:
        setComponentEnabledSetting(r7, 2, 1, r22);
        r8 = true;
        android.util.Slog.i(com.android.server.pm.HwPackageManagerService.TAG, "Disable [" + r7 + "] for clone user " + r22);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x0239, code lost:
        r0 = new android.content.Intent("android.intent.action.MAIN");
        r0.addCategory("android.intent.category.HOME");
        r14 = queryIntentActivities(r0, r0.resolveTypeIfNeeded(r21.mContext.getContentResolver()), 786432, r22);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:60:0x0254, code lost:
        if (r14 == null) goto L_0x02a1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x0256, code lost:
        r0 = r14.getList().iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:0x0262, code lost:
        if (r0.hasNext() == false) goto L_0x02a1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x0264, code lost:
        r15 = (android.content.pm.ResolveInfo) r0.next();
        r7 = r15.activityInfo.getComponentName();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x0278, code lost:
        if (r0.contains(r15.activityInfo.packageName) == false) goto L_0x029d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:66:0x027a, code lost:
        setComponentEnabledSetting(r7, 2, r10, r22);
        r8 = true;
        android.util.Slog.i(com.android.server.pm.HwPackageManagerService.TAG, "Disable [" + r7 + "] for clone user " + r22);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x029d, code lost:
        r10 = 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x02a1, code lost:
        r9 = r21.mContext.getResources().getStringArray(33816595);
        r0 = null;
        r10 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x02b1, code lost:
        if (r10 >= r9.length) goto L_0x033f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x02b3, code lost:
        r15 = new android.content.Intent(r9[r10]);
        r17 = queryIntentReceivers(r15, r15.resolveTypeIfNeeded(r21.mContext.getContentResolver()), 786432, r22);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x02ce, code lost:
        if (r17 != null) goto L_0x02d3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x02d3, code lost:
        r18 = r17.getList().iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x02df, code lost:
        if (r18.hasNext() == false) goto L_0x0336;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:0x02e1, code lost:
        r3 = (android.content.pm.ResolveInfo) r18.next();
        r7 = r3.activityInfo.getComponentName();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x02ee, code lost:
        if (r7 == null) goto L_0x032b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x02f8, code lost:
        if (isSupportCloneAppInCust(r3.activityInfo.packageName) != false) goto L_0x032b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x02fa, code lost:
        r16 = r4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:?, code lost:
        setComponentEnabledSetting(r7, 2, 1, r22);
        android.util.Slog.i(com.android.server.pm.HwPackageManagerService.TAG, "disableReceiversForClone package [" + r7 + "] for user " + r22);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x0323, code lost:
        android.util.Slog.e(com.android.server.pm.HwPackageManagerService.TAG, "disableReceiversForClone Exception");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:85:0x032b, code lost:
        r16 = r4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:0x0336, code lost:
        r10 = r10 + 1;
        r0 = r15;
        r4 = r4;
        r3 = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:89:0x0345, code lost:
        if (r8 == false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:90:0x0347, code lost:
        scheduleWritePackageRestrictionsLocked(r22);
     */
    public void deleteNonRequiredAppsForClone(int clonedProfileUserId, boolean isFirstCreate) {
        String[] requiredAppsList;
        String[] disabledComponent = this.mContext.getResources().getStringArray(33816594);
        int length = disabledComponent.length;
        boolean z = false;
        boolean shouldupdate = false;
        int i = 0;
        while (true) {
            int i2 = 1;
            if (i >= length) {
                break;
            }
            String[] componentArray = disabledComponent[i].split("/");
            if (componentArray != null && componentArray.length == 2) {
                ComponentName component = new ComponentName(componentArray[0], componentArray[1]);
                try {
                    if (getComponentEnabledSetting(component, clonedProfileUserId) != 2) {
                        setComponentEnabledSetting(component, 2, 1, clonedProfileUserId);
                        shouldupdate = true;
                    }
                } catch (IllegalArgumentException | SecurityException e) {
                    Slog.d(TAG, "deleteNonRequiredComponentsForClone exception:" + e.getMessage());
                }
            }
            i++;
        }
        String[] requiredAppsList2 = this.mContext.getResources().getStringArray(33816586);
        Set<String> requiredAppsSet = new HashSet<>(Arrays.asList(requiredAppsList2));
        UserInfo ui = sUserManager.getUserInfo(clonedProfileUserId);
        synchronized (this.mPackages) {
            try {
                for (Map.Entry<String, PackageSetting> entry : this.mSettings.mPackages.entrySet()) {
                    try {
                        if (isFirstCreate) {
                            if (!requiredAppsSet.contains(entry.getKey())) {
                                entry.getValue().setInstalled(z, clonedProfileUserId);
                                shouldupdate = true;
                                Slog.i(TAG, "Deleting non supported package [" + entry.getKey() + "] for clone user " + clonedProfileUserId);
                            }
                        } else if (!isSupportCloneAppInCust(entry.getKey()) && !requiredAppsSet.contains(entry.getKey())) {
                            entry.getValue().setInstalled(z, clonedProfileUserId);
                            shouldupdate = true;
                            Slog.i(TAG, "Deleting non supported package [" + entry.getKey() + "] for clone user " + clonedProfileUserId);
                        } else if (requiredAppsSet.contains(entry.getKey()) && entry.getValue().getInstalled(ui.profileGroupId)) {
                            if (!entry.getValue().getInstalled(clonedProfileUserId)) {
                                entry.getValue().setInstalled(true, clonedProfileUserId);
                                shouldupdate = true;
                                Slog.i(TAG, "Adding required package [" + entry.getKey() + "] for clone user " + clonedProfileUserId);
                            }
                            if (!entry.getValue().isSystem()) {
                                try {
                                    setComponentEnabledSetting(new ComponentName(entry.getKey(), PackageManager.APP_DETAILS_ACTIVITY_CLASS_NAME), 2, 1, clonedProfileUserId);
                                } catch (IllegalArgumentException | SecurityException e2) {
                                    Slog.d(TAG, "Disable details activity exception:" + e2.getMessage());
                                }
                                Slog.i(TAG, "Disable details activity [" + entry.getKey() + "] for clone user " + clonedProfileUserId);
                            }
                        }
                        z = false;
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                while (true) {
                    break;
                }
                throw th;
            }
        }
        requiredAppsList2 = requiredAppsList;
    }

    public void deletePackageVersioned(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId, int deleteFlags) {
        PackageSetting pkgSetting;
        PackageParser.Package p;
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && (deleteFlags & 2) == 0 && versionedPackage != null && sSupportCloneApps.contains(versionedPackage.getPackageName()) && userId != 0 && sUserManager.isClonedProfile(userId) && (p = (PackageParser.Package) this.mPackages.get(versionedPackage.getPackageName())) != null && (p.applicationInfo.flags & 1) != 0) {
            deleteFlags |= 4;
        }
        if (versionedPackage != null) {
            HwPackageManagerService.super.deletePackageVersioned(versionedPackage, observer, userId, deleteFlags);
        }
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && (deleteFlags & 2) == 0 && versionedPackage != null && sSupportCloneApps.contains(versionedPackage.getPackageName())) {
            long ident = Binder.clearCallingIdentity();
            try {
                for (UserInfo ui : sUserManager.getProfiles(userId, false)) {
                    if (ui.isClonedProfile() && ui.id != userId && ui.profileGroupId == userId && (pkgSetting = (PackageSetting) this.mSettings.mPackages.get(versionedPackage.getPackageName())) != null && pkgSetting.getInstalled(ui.id)) {
                        HwPackageManagerService.super.deletePackageVersioned(versionedPackage, observer, ui.id, deleteFlags);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void deleteClonedProfileIfNeed(int[] removedUsers) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && removedUsers != null && removedUsers.length > 0) {
            int length = removedUsers.length;
            int i = 0;
            while (i < length) {
                int userId = removedUsers[i];
                long callingId = Binder.clearCallingIdentity();
                try {
                    UserInfo userInfo = sUserManager.getUserInfo(userId);
                    if (userInfo == null || !userInfo.isClonedProfile() || isAnyApkInstalledInClonedProfile(userId)) {
                        Binder.restoreCallingIdentity(callingId);
                        i++;
                    } else {
                        sUserManager.removeUser(userId);
                        Slog.i(TAG, "Remove cloned profile " + userId);
                        Intent clonedProfileIntent = new Intent("android.intent.action.USER_REMOVED");
                        clonedProfileIntent.setPackage("com.huawei.android.launcher");
                        clonedProfileIntent.addFlags(1342177280);
                        clonedProfileIntent.putExtra("android.intent.extra.USER", new UserHandle(userId));
                        clonedProfileIntent.putExtra("android.intent.extra.user_handle", userId);
                        this.mContext.sendBroadcastAsUser(clonedProfileIntent, new UserHandle(userInfo.profileGroupId), null);
                        return;
                    }
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }
    }

    private boolean isAnyApkInstalledInClonedProfile(int clonedProfileUserId) {
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        return queryIntentActivities(launcherIntent, launcherIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432, clonedProfileUserId).getList().size() > 0;
    }

    private int redirectInstallForClone(int userId) {
        if (userId == 0 || !HwActivityManagerService.IS_SUPPORT_CLONE_APP) {
            return userId;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            UserInfo ui = sUserManager.getUserInfo(userId);
            if (ui != null && ui.isClonedProfile()) {
                return ui.profileGroupId;
            }
            Binder.restoreCallingIdentity(ident);
            return userId;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void sendPackageBroadcast(String action, String pkg, Bundle extras, int flags, String targetPkg, IIntentReceiver finishedReceiver, int[] userIds, int[] instantUserIds) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && getUserManagerInternal().hasClonedProfile() && (("android.intent.action.PACKAGE_ADDED".equals(action) || ("android.intent.action.PACKAGE_CHANGED".equals(action) && userIds != null)) && !sSupportCloneApps.contains(pkg))) {
            long callingId = Binder.clearCallingIdentity();
            int cloneUserId = -1;
            if (userIds != null) {
                int length = userIds.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        int userId = userIds[i];
                        if (userId != 0 && sUserManager.isClonedProfile(userId)) {
                            cloneUserId = userId;
                            break;
                        }
                        i++;
                    } else {
                        break;
                    }
                }
            } else {
                try {
                    cloneUserId = getUserManagerInternal().findClonedProfile().id;
                } catch (Exception e) {
                    Slog.e(TAG, "Set required Apps' component disabled failed");
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(callingId);
                    throw th;
                }
            }
            Intent launcherIntent = new Intent("android.intent.action.MAIN");
            launcherIntent.addCategory("android.intent.category.LAUNCHER");
            launcherIntent.setPackage(pkg);
            ParceledListSlice<ResolveInfo> parceledList = queryIntentActivities(launcherIntent, launcherIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432, cloneUserId);
            if (parceledList != null) {
                for (ResolveInfo resolveInfo : parceledList.getList()) {
                    setComponentEnabledSetting(resolveInfo.activityInfo.getComponentName(), 2, 1, cloneUserId);
                }
            }
            Binder.restoreCallingIdentity(callingId);
        }
        HwPackageManagerService.super.sendPackageBroadcast(action, pkg, extras, flags, targetPkg, finishedReceiver, userIds, instantUserIds);
    }

    private static void initCloneAppsFromCust() {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP) {
            File configFile = HwPackageManagerUtils.getCustomizedFileName(CLONE_APP_LIST, 0);
            if (configFile == null || !configFile.exists()) {
                Flog.i(205, "hw_clone_app_list.xml does not exists.");
                return;
            }
            InputStream inputStream = null;
            try {
                InputStream inputStream2 = new FileInputStream(configFile);
                XmlPullParser xmlParser = Xml.newPullParser();
                xmlParser.setInput(inputStream2, null);
                while (true) {
                    int xmlEventType = xmlParser.next();
                    if (xmlEventType == 1) {
                        try {
                            inputStream2.close();
                            return;
                        } catch (IOException e) {
                            Slog.e(TAG, "initCloneAppsFromCust:- IOE while closing stream", e);
                            return;
                        }
                    } else if (xmlEventType == 2 && "package".equals(xmlParser.getName())) {
                        String packageName = xmlParser.getAttributeValue(null, "name");
                        if (!TextUtils.isEmpty(packageName)) {
                            sSupportCloneApps.add(packageName.intern());
                        }
                    }
                }
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "initCloneAppsFromCust", e2);
                if (0 != 0) {
                    inputStream.close();
                }
            } catch (XmlPullParserException e3) {
                Log.e(TAG, "initCloneAppsFromCust", e3);
                if (0 != 0) {
                    inputStream.close();
                }
            } catch (IOException e4) {
                Log.e(TAG, "initCloneAppsFromCust", e4);
                if (0 != 0) {
                    inputStream.close();
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (IOException e5) {
                        Slog.e(TAG, "initCloneAppsFromCust:- IOE while closing stream", e5);
                    }
                }
                throw th;
            }
        }
    }

    public static boolean isSupportCloneAppInCust(String packageName) {
        return sSupportCloneApps.contains(packageName);
    }

    private void deleteNonSupportedAppsForClone() {
        long callingId = Binder.clearCallingIdentity();
        try {
            Iterator<UserInfo> it = sUserManager.getUsers(false).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                UserInfo ui = it.next();
                if (ui.isClonedProfile()) {
                    deleteNonRequiredAppsForClone(ui.id, false);
                    break;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private int updateFlagsForClone(int flags, int userId) {
        if (!HwActivityManagerService.IS_SUPPORT_CLONE_APP || userId == 0 || !getUserManagerInternal().isClonedProfile(userId)) {
            return flags;
        }
        int callingUid = Binder.getCallingUid();
        if (userId != UserHandle.getUserId(callingUid) || !sSupportCloneApps.contains(getNameForUid(callingUid))) {
            return flags;
        }
        return flags | 4202496;
    }

    /* access modifiers changed from: protected */
    public List<ResolveInfo> queryIntentActivitiesInternal(Intent intent, String resolvedType, int flags, int filterCallingUid, int userId, boolean resolveForStart, boolean allowDynamicSplits) {
        boolean shouldCheckUninstall;
        int flags2;
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && userId != 0) {
            int callingUid = Binder.getCallingUid();
            UserInfo ui = getUserManagerInternal().getUserInfo(userId);
            if (ui != null && ui.isClonedProfile() && userId == UserHandle.getUserId(callingUid)) {
                boolean shouldCheckUninstall2 = (flags & 4202496) != 0 && UserHandle.getAppId(callingUid) == 1000;
                if (sSupportCloneApps.contains(getNameForUid(callingUid))) {
                    if ((flags & 4202496) == 0) {
                        shouldCheckUninstall2 = true;
                    }
                    flags2 = flags | 4202496;
                    shouldCheckUninstall = shouldCheckUninstall2;
                } else {
                    flags2 = flags;
                    shouldCheckUninstall = shouldCheckUninstall2;
                }
                List<ResolveInfo> result = HwPackageManagerService.super.queryIntentActivitiesInternal(intent, resolvedType, flags2, filterCallingUid, userId, resolveForStart, allowDynamicSplits);
                if (shouldCheckUninstall) {
                    Iterator<ResolveInfo> iterator = result.iterator();
                    while (iterator.hasNext()) {
                        ResolveInfo ri = iterator.next();
                        if (!this.mSettings.isEnabledAndMatchLPr(ri.activityInfo, 786432, ui.profileGroupId) && !this.mSettings.isEnabledAndMatchLPr(ri.activityInfo, 786432, userId)) {
                            iterator.remove();
                        }
                    }
                }
                return result;
            }
        }
        return HwPackageManagerService.super.queryIntentActivitiesInternal(intent, resolvedType, flags, filterCallingUid, userId, resolveForStart, allowDynamicSplits);
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && userId != 0) {
            int callingUid = Binder.getCallingUid();
            UserInfo ui = getUserManagerInternal().getUserInfo(userId);
            if (ui != null && ui.isClonedProfile() && userId == UserHandle.getUserId(callingUid)) {
                boolean shouldCheckUninstall = (flags & 4202496) != 0 && UserHandle.getAppId(callingUid) == 1000;
                if (sSupportCloneApps.contains(getNameForUid(callingUid))) {
                    if ((flags & 4202496) == 0) {
                        shouldCheckUninstall = true;
                    }
                    flags |= 4202496;
                }
                ActivityInfo ai = HwPackageManagerService.super.getActivityInfo(component, flags, userId);
                if (!shouldCheckUninstall || ai == null || this.mSettings.isEnabledAndMatchLPr(ai, 786432, ui.profileGroupId) || this.mSettings.isEnabledAndMatchLPr(ai, 786432, userId)) {
                    return ai;
                }
                return null;
            }
        }
        return HwPackageManagerService.super.getActivityInfo(component, flags, userId);
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        return HwPackageManagerService.super.getPackageInfo(packageName, updateFlagsForClone(flags, userId), userId);
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        return HwPackageManagerService.super.getApplicationInfo(packageName, updateFlagsForClone(flags, userId), userId);
    }

    public boolean isPackageAvailable(String packageName, int userId) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && userId != 0) {
            int callingUid = Binder.getCallingUid();
            if (userId == UserHandle.getUserId(callingUid)) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    UserInfo ui = sUserManager.getUserInfo(userId);
                    if (ui.isClonedProfile() && sSupportCloneApps.contains(getNameForUid(callingUid))) {
                        return HwPackageManagerService.super.isPackageAvailable(packageName, ui.profileGroupId);
                    }
                    Binder.restoreCallingIdentity(callingId);
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }
        return HwPackageManagerService.super.isPackageAvailable(packageName, userId);
    }

    public ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId) {
        return HwPackageManagerService.super.getInstalledPackages(updateFlagsForClone(flags, userId), userId);
    }

    public ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        return HwPackageManagerService.super.getInstalledApplications(updateFlagsForClone(flags, userId), userId);
    }

    public int installExistingPackageAsUser(String packageName, int userId, int installFlags, int installReason, List<String> whiteListedPermissions) {
        if (userId != 0 && sSupportCloneApps.contains(packageName) && getUserManagerInternal().isClonedProfile(userId)) {
            long callingId = Binder.clearCallingIdentity();
            try {
                setPackageStoppedState(packageName, true, userId);
                Slog.d(TAG, packageName + " is set stopped for user " + userId);
            } catch (IllegalArgumentException e) {
                Slog.w(TAG, "error in setPackageStoppedState for " + e.getMessage());
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(callingId);
                throw th;
            }
            Binder.restoreCallingIdentity(callingId);
        }
        return HwPackageManagerService.super.installExistingPackageAsUser(packageName, userId, installFlags, installReason, whiteListedPermissions);
    }

    public ProviderInfo resolveContentProvider(String name, int flags, int userId) {
        return HwPackageManagerService.super.resolveContentProvider(name, flags, ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).handleUserForClone(name, userId));
    }

    public List<String> getSupportSplitScreenApps() {
        List<String> list = new ArrayList<>();
        list.addAll(SplitNotificationUtils.getInstance(this.mContext).getListPkgName(2));
        list.addAll(SplitNotificationUtils.getInstance(this.mContext).getListPkgName(1));
        return list;
    }

    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags, int userId) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && userId != 0 && ((newState == 0 || newState == 1) && !sSupportCloneApps.contains(componentName.getPackageName()))) {
            long callingId = Binder.clearCallingIdentity();
            try {
                if (sUserManager.isClonedProfile(userId) && new HashSet<>(Arrays.asList(this.mContext.getResources().getStringArray(33816586))).contains(componentName.getPackageName())) {
                    Intent launcherIntent = new Intent("android.intent.action.MAIN");
                    launcherIntent.addCategory("android.intent.category.LAUNCHER");
                    launcherIntent.setPackage(componentName.getPackageName());
                    ParceledListSlice<ResolveInfo> parceledList = queryIntentActivities(launcherIntent, launcherIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786944, userId);
                    if (parceledList != null) {
                        for (ResolveInfo resolveInfo : parceledList.getList()) {
                            if (componentName.equals(resolveInfo.getComponentInfo().getComponentName())) {
                                Slog.i(TAG, "skip enable [" + resolveInfo.activityInfo.getComponentName() + "] for clone user " + userId);
                                return;
                            }
                        }
                    }
                }
                Binder.restoreCallingIdentity(callingId);
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
        ComponentChangeMonitor componentChangeMonitor = this.mComponentChangeMonitor;
        if (componentChangeMonitor != null) {
            componentChangeMonitor.writeComponetChangeLogToFile(componentName, newState, userId);
        }
        HwPackageManagerService.super.setComponentEnabledSetting(componentName, newState, flags, userId);
    }

    private int[] getApplicationType(Parcel in) {
        ArrayList<String> pkgName = in.createStringArrayList();
        if (pkgName == null) {
            Slog.i(TAG, "getApplicationType , pkgName is null");
            return null;
        }
        int N = pkgName.size();
        int[] appType = new int[N];
        PackageManager package1 = this.mContext.getPackageManager();
        int[] comparedSignatures = in.createIntArray();
        Binder.clearCallingIdentity();
        for (int i = 0; i < N; i++) {
            int j = 0;
            ApplicationInfo ai = getApplicationInfo(pkgName.get(i), 4194304, 0);
            if (isSystemApp(ai)) {
                appType[i] = appType[i] | 1;
            }
            try {
                if (!ai.sourceDir.startsWith("/data/app/")) {
                    appType[i] = appType[i] | 2;
                }
                if (comparedSignatures == null) {
                    Slog.i(TAG, "getApplicationType , comparedSignatures is null , continue");
                } else {
                    try {
                        PackageInfo pi = package1.getPackageInfo(pkgName.get(i), 64);
                        if (pi.packageName.equals(pkgName.get(i)) && pi.signatures != null && pi.signatures.length == 1) {
                            int sigHashCode = pi.signatures[0].hashCode();
                            while (true) {
                                if (j >= comparedSignatures.length) {
                                    break;
                                } else if (sigHashCode == comparedSignatures[j]) {
                                    appType[i] = appType[i] | 4;
                                    break;
                                } else {
                                    j++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Slog.i(TAG, "app:" + pkgName.get(i) + ", unmatch hwSignatures!");
                    }
                }
            } catch (Exception e2) {
                Slog.i(TAG, "app:" + pkgName.get(i) + ", not exists!");
                appType[i] = appType[i] | 128;
            }
        }
        return appType;
    }

    public HwCustPackageManagerService getCustPackageManagerService() {
        return mCustPackageManagerService;
    }

    public void setCotaApksInstallStatus(int value) {
        this.mCotaApksInstallStatus = value;
    }

    public HashMap<String, HashSet<String>> getCotaDelInstallMap() {
        return mCotaDelInstallMap;
    }

    public HashMap<String, HashSet<String>> getCotaInstallMap() {
        return mCotaInstallMap;
    }

    /* access modifiers changed from: protected */
    public boolean isNotificationAddSplitButton(String ImsPkgName) {
        if (TextUtils.isEmpty(ImsPkgName)) {
            return false;
        }
        List<String> oneSplitScreenImsListPkgNames = SplitNotificationUtils.getInstance(this.mContext).getListPkgName(2);
        if (oneSplitScreenImsListPkgNames.size() == 0 || !oneSplitScreenImsListPkgNames.contains(ImsPkgName.toLowerCase(Locale.getDefault())) || !isSupportSplitScreen(ImsPkgName)) {
            return false;
        }
        String dockableTopPkgName = getDockableTopPkgName();
        if (TextUtils.isEmpty(dockableTopPkgName)) {
            return false;
        }
        List<String> oneSplitScreenVideoListPkgNames = SplitNotificationUtils.getInstance(this.mContext).getListPkgName(1);
        if (oneSplitScreenVideoListPkgNames.size() == 0 || !oneSplitScreenVideoListPkgNames.contains(dockableTopPkgName.toLowerCase(Locale.getDefault())) || !isSupportSplitScreen(dockableTopPkgName)) {
            return false;
        }
        return true;
    }

    private String getDockableTopPkgName() {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        List<ActivityManager.RunningTaskInfo> tasks = null;
        if (am != null) {
            tasks = am.getRunningTasks(1);
        }
        ActivityManager.RunningTaskInfo runningTaskInfo = null;
        if (tasks != null && !tasks.isEmpty()) {
            runningTaskInfo = tasks.get(0);
        }
        if (runningTaskInfo == null || !runningTaskInfo.supportsSplitScreenMultiWindow) {
            return "";
        }
        try {
            if (WindowManagerGlobal.getWindowManagerService().getDockedStackSide() != -1 || ActivityManager.getService().isInLockTaskMode()) {
                return "";
            }
            return runningTaskInfo.topActivity.getPackageName();
        } catch (RemoteException e) {
            Slog.e(TAG, "get dockside failed by RemoteException");
            return "";
        }
    }

    private boolean isSupportSplitScreen(String packageName) {
        ComponentName mainComponentName;
        PackageManager packageManager = this.mContext.getPackageManager();
        int userId = ActivityManager.getCurrentUser();
        Intent mainIntent = getLaunchIntentForPackageAsUser(packageName, packageManager, userId);
        if (!(mainIntent == null || (mainComponentName = mainIntent.getComponent()) == null)) {
            try {
                ActivityInfo activityInfo = getActivityInfo(mainComponentName, 0, userId);
                if (activityInfo != null) {
                    return isResizeableMode(activityInfo.resizeMode);
                }
            } catch (RuntimeException e) {
                Slog.e(TAG, "get activityInfo failed by ComponentNameException");
            } catch (Exception e2) {
                Slog.e(TAG, "get activityInfo failed by ComponentNameException");
            }
        }
        return false;
    }

    private boolean isResizeableMode(int mode) {
        return mode == 2 || mode == 4 || mode == 1;
    }

    private Intent getLaunchIntentForPackageAsUser(String packageName, PackageManager pm, int userId) {
        Intent intentToResolve = new Intent("android.intent.action.MAIN");
        intentToResolve.addCategory("android.intent.category.INFO");
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = pm.queryIntentActivitiesAsUser(intentToResolve, 0, userId);
        if (ris == null || ris.size() <= 0) {
            intentToResolve.removeCategory("android.intent.category.INFO");
            intentToResolve.addCategory("android.intent.category.LAUNCHER");
            intentToResolve.setPackage(packageName);
            ris = pm.queryIntentActivitiesAsUser(intentToResolve, 0, userId);
        }
        if (ris == null || ris.size() <= 0) {
            return null;
        }
        Intent intent = new Intent(intentToResolve);
        intent.setFlags(268435456);
        intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
        return intent;
    }
}
