package com.android.server.pm;

import android.app.AppGlobals;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.Permission;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IStorageManager.Stub;
import android.os.storage.VolumeInfo;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HwCustPackageManagerServiceImpl extends HwCustPackageManagerService {
    private static String BOOT_DELETE_CONFIG = "/product/etc/boot_delete_apps.cfg";
    private static final String CUST_DEFAULT_LAUNCHER = SystemProperties.get("ro.config.def_launcher_pkg", "");
    private static final boolean FILT_REQ_PERM = SystemProperties.getBoolean("ro.config.hw_filt_req_perm", false);
    protected static final boolean HWDBG;
    protected static final boolean HWFLOW;
    protected static final boolean HWLOGW_E = true;
    private static final boolean IS_DOCOMO = SystemProperties.get("ro.product.custom", "NULL").contains("docomo");
    public static final boolean SECURITY_PACKAGE_ENABLE = SystemProperties.getBoolean("ro.config.hw_security_pkg", false);
    private static final String SIMPLE_LAUNCHER_PACKAGE_NAME = "com.huawei.android.simplelauncher";
    private static final int SYSTEMUI_DEFAULT_UID = -100;
    private static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "HwCustPackageManager";
    private static final String TAG_FLOW = "HwCustPackageManager_FLOW";
    private static final String TAG_INIT = "HwCustPackageManager_INIT";
    Context mContext;
    private ArrayList<DelPackage> mListedApps = new ArrayList();
    private Object mLock = new Object();
    boolean mSdInstallEnable = SystemProperties.getBoolean("ro.config.hw_sdInstall_enable", false);
    private int mSystemUIUid = SYSTEMUI_DEFAULT_UID;

    private class DelPackage {
        private int delFlag;
        private String delPackageName;

        private DelPackage() {
        }

        public DelPackage(String name, int flag) {
            this.delPackageName = name;
            this.delFlag = flag;
        }
    }

    static {
        boolean z = Log.HWLog;
        boolean z2 = HWLOGW_E;
        z = (z || (Log.HWModuleLog && Log.isLoggable(TAG, 3))) ? HWLOGW_E : false;
        HWDBG = z;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z2 = false;
        }
        HWFLOW = z2;
    }

    public HwCustPackageManagerServiceImpl() {
        if (HWFLOW) {
            Log.d(TAG_FLOW, "HwCustPackageManagerServiceImpl");
        }
        if (SECURITY_PACKAGE_ENABLE) {
            this.mSystemUIUid = getUidByPackageName(SYSTEMUI_PACKAGE_NAME);
        }
    }

    public void scanCustPrivDir(int scanMode, AbsPackageManagerService service) {
        if (HWFLOW) {
            Log.d(TAG_FLOW, "scanCustPrivDir");
        }
        File custPrivAppDir = new File("/data/cust/", "priv-app");
        if (custPrivAppDir.exists()) {
            service.custScanPrivDir(custPrivAppDir, 16, (scanMode | 131072) | 262144, 0, 0);
        }
        File custDelPrivAppDir = new File("/data/cust/", "priv-delapp");
        if (custDelPrivAppDir.exists()) {
            service.custScanPrivDir(custDelPrivAppDir, 16, (scanMode | 131072) | 262144, 0, 33554432);
        }
    }

    public boolean isPrivAppInCust(File file) {
        if (HWFLOW) {
            String str = TAG_FLOW;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isPrivAppInCust,file=");
            stringBuilder.append(file);
            Log.d(str, stringBuilder.toString());
        }
        String filePath = file.getAbsolutePath();
        boolean z = (filePath.startsWith("/data/cust/priv-app") || filePath.startsWith("/data/cust/priv-delapp") || filePath.startsWith("/data/cota/atl/priv-app") || filePath.startsWith("/data/cota/atl/priv-delapp")) ? HWLOGW_E : false;
        return z;
    }

    public void handleCustInitailizations(Object settings) {
        if (SystemProperties.getBoolean("ro.config.hw_DMHFA", false)) {
            enableApplication(settings, "com.huawei.android.DMHFA", 1);
            enableComponent(settings, "com.huawei.sprint.setupwizard", "com.huawei.sprint.setupwizard.controller.ControllerActivity");
        }
    }

    public void enableApplication(Object settings, String packageName, int newState) {
        PackageSetting pkgSetting = null;
        if (settings instanceof Settings) {
            pkgSetting = (PackageSetting) ((Settings) settings).mPackages.get(packageName);
        }
        if (pkgSetting == null) {
            String str = TAG_FLOW;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableApplication Unknown package: ");
            stringBuilder.append(packageName);
            Log.w(str, stringBuilder.toString());
            return;
        }
        int userId = UserHandle.getCallingUserId();
        if (pkgSetting.getEnabled(userId) == newState) {
            if (HWFLOW) {
                Log.d(TAG, "**** Nothing to do!");
            }
            return;
        }
        pkgSetting.setEnabled(newState, userId, null);
    }

    private void enableComponent(Object settings, String packageName, String componentName) {
        PackageSetting pkgSetting = null;
        if (settings instanceof Settings) {
            pkgSetting = (PackageSetting) ((Settings) settings).mPackages.get(packageName);
        }
        String str;
        if (pkgSetting == null) {
            str = TAG_FLOW;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableComponent Unknown package: ");
            stringBuilder.append(packageName);
            Log.w(str, stringBuilder.toString());
            return;
        }
        str = componentName;
        Package pkg = pkgSetting.pkg;
        if (pkg == null || !pkg.hasComponentClassName(str)) {
            String str2 = TAG_FLOW;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed setComponentEnabledSetting: component class ");
            stringBuilder2.append(str);
            stringBuilder2.append(" does not exist");
            Log.w(str2, stringBuilder2.toString());
            return;
        }
        pkgSetting.enableComponentLPw(str, UserHandle.getCallingUserId());
    }

    public File customizeUninstallApk(File file) {
        String apkListFilePath = SystemProperties.get("ro.config.huawei.apklistpath", "");
        if (!TextUtils.isEmpty(apkListFilePath)) {
            File customFile = new File(apkListFilePath);
            if (customFile.exists()) {
                return customFile;
            }
        }
        return file;
    }

    public boolean isMccMncMatch() {
        String mccmnc = SystemProperties.get("persist.sys.mccmnc", "");
        if (mccmnc == null || "".equals(mccmnc)) {
            return false;
        }
        return HWLOGW_E;
    }

    public String joinCustomizeFile(String fileName) {
        String joinFileName = fileName;
        String mccmnc = SystemProperties.get("persist.sys.mccmnc", "");
        if (fileName == null) {
            return joinFileName;
        }
        String[] splitArray = fileName.split("\\.");
        if (splitArray.length != 2) {
            return joinFileName;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(splitArray[0]);
        stringBuilder.append("_");
        stringBuilder.append(mccmnc);
        stringBuilder.append(".");
        stringBuilder.append(splitArray[1]);
        return stringBuilder.toString();
    }

    public String getCustomizeAPKListFile(String apkListFile, String mAPKInstallList_FILE, String mDelAPKInstallList_FILE, String mAPKInstallList_DIR) {
        if (apkListFile == null) {
            return apkListFile;
        }
        if (!apkListFile.equals(mAPKInstallList_FILE) && !apkListFile.equals(mDelAPKInstallList_FILE)) {
            return apkListFile;
        }
        String tmpAPKInstallFile = joinCustomizeFile(mAPKInstallList_FILE);
        String tmpDelAPKInstallFile = joinCustomizeFile(mDelAPKInstallList_FILE);
        if (!new File(mAPKInstallList_DIR, tmpAPKInstallFile).exists() && !new File(mAPKInstallList_DIR, tmpDelAPKInstallFile).exists()) {
            return apkListFile;
        }
        return apkListFile.equals(mAPKInstallList_FILE) ? tmpAPKInstallFile : tmpDelAPKInstallFile;
    }

    public String getCustomizeAPKInstallFile(String APKInstallFile, String DelAPKInstallFile) {
        String tmpAPKInstallFile = joinCustomizeFile(APKInstallFile);
        String tmpDelAPKInstallFile = joinCustomizeFile(DelAPKInstallFile);
        try {
            if (HwCfgFilePolicy.getCfgFileList(tmpAPKInstallFile, 0).size() > 0 || HwCfgFilePolicy.getCfgFileList(tmpDelAPKInstallFile, 0).size() > 0) {
                return tmpAPKInstallFile;
            }
            return APKInstallFile;
        } catch (NoClassDefFoundError e) {
            if (HWDBG) {
                Log.d(TAG_FLOW, "getCustomizeAPKInstallFile: NoClassDefFound");
            }
        }
    }

    public String getCustomizeDelAPKInstallFile(String APKInstallFile, String DelAPKInstallFile) {
        String tmpAPKInstallFile = joinCustomizeFile(APKInstallFile);
        String tmpDelAPKInstallFile = joinCustomizeFile(DelAPKInstallFile);
        try {
            if (HwCfgFilePolicy.getCfgFileList(tmpAPKInstallFile, 0).size() > 0 || HwCfgFilePolicy.getCfgFileList(tmpDelAPKInstallFile, 0).size() > 0) {
                return tmpDelAPKInstallFile;
            }
            return DelAPKInstallFile;
        } catch (NoClassDefFoundError e) {
            if (HWDBG) {
                Log.d(TAG_FLOW, "getCustomizeDelAPKInstallFile: NoClassDefFound");
            }
        }
    }

    public boolean isSdInstallEnabled() {
        return this.mSdInstallEnable;
    }

    private boolean isFirstSdVolume(VolumeInfo vol) {
        String CurrentDiskID = vol.getDisk().getId();
        String CurrentVolumeID = vol.getId();
        String[] CurrentDiskIDSplitstr = CurrentDiskID.split(":");
        String[] CurrentVolumeIDSplitstr = CurrentVolumeID.split(":");
        if (CurrentDiskIDSplitstr.length != 3 || CurrentVolumeIDSplitstr.length != 3) {
            return false;
        }
        try {
            int DiskID = Integer.valueOf(CurrentDiskIDSplitstr[2]).intValue();
            int VolumeID = Integer.valueOf(CurrentVolumeIDSplitstr[2]).intValue();
            if (VolumeID == DiskID + 1 || VolumeID == DiskID) {
                return HWLOGW_E;
            }
            return false;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isSDCardMounted() {
        try {
            for (VolumeInfo vol : Stub.asInterface(ServiceManager.getService("mount")).getVolumes(0)) {
                if (vol.getDisk() != null) {
                    if (vol.isMountedWritable() && vol.getDisk().isSd() && isFirstSdVolume(vol)) {
                        return HWLOGW_E;
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean needDerivePkgAbi(Package pkg) {
        return (isSdInstallEnabled() && pkg.applicationInfo.isExternalAsec() && isSDCardMounted()) ? HWLOGW_E : false;
    }

    public boolean canAppMoveToPublicSd(VolumeInfo volume) {
        boolean z = false;
        if (!isSdInstallEnabled() || volume == null || volume.getDisk() == null) {
            return false;
        }
        if (volume.getDisk().isSd() && volume.isMountedWritable() && isFirstSdVolume(volume)) {
            z = HWLOGW_E;
        }
        return z;
    }

    public boolean isHwCustHiddenInfoPackage(Package pkgInfo) {
        return (SECURITY_PACKAGE_ENABLE && isRestrictedPackage(pkgInfo)) ? HWLOGW_E : false;
    }

    private boolean isRestrictedPackage(Package pkgInfo) {
        if (pkgInfo != null && hasPermission(pkgInfo, "android.permission.SECURITY_PACKAGE")) {
            int uid = Binder.getCallingUid();
            if (uid >= 10000 && uid <= 19999 && uid != this.mSystemUIUid) {
                return HWLOGW_E;
            }
        }
        return false;
    }

    private int getUidByPackageName(String packageName) {
        try {
            return AppGlobals.getPackageManager().getPackageUid(packageName, 1048576, UserHandle.getCallingUserId());
        } catch (Exception e) {
            Log.w(TAG_FLOW, "Exception happend, when get package uid");
            return SYSTEMUI_DEFAULT_UID;
        }
    }

    private static boolean hasPermission(Package pkgInfo, String perm) {
        for (int i = pkgInfo.permissions.size() - 1; i >= 0; i--) {
            if (((Permission) pkgInfo.permissions.get(i)).info.name.equals(perm)) {
                return HWLOGW_E;
            }
        }
        return false;
    }

    public boolean isSdVol(VolumeInfo vol) {
        boolean z = false;
        if (!this.mSdInstallEnable || vol.getDisk() == null) {
            return false;
        }
        if (vol.getDisk().isSd() && isFirstSdVolume(vol)) {
            z = HWLOGW_E;
        }
        return z;
    }

    public int isListedApp(String packageName) {
        synchronized (this.mLock) {
            if (this.mListedApps.size() == 0) {
                readDelAppsList();
            }
        }
        Iterator it = this.mListedApps.iterator();
        while (it.hasNext()) {
            DelPackage app = (DelPackage) it.next();
            if (packageName.equals(app.delPackageName)) {
                return app.delFlag;
            }
        }
        return -1;
    }

    public boolean isHwFiltReqInstallPerm(String pkgName, String permission) {
        if (FILT_REQ_PERM && "com.huawei.hidisk".equals(pkgName) && "android.permission.REQUEST_INSTALL_PACKAGES".equals(permission)) {
            return HWLOGW_E;
        }
        return false;
    }

    private void readDelAppsList() {
        BufferedReader reader = null;
        try {
            File confFile = new File(BOOT_DELETE_CONFIG);
            File cfg = HwCfgFilePolicy.getCfgFile("boot_delete_apps.cfg", 0);
            if (cfg != null) {
                confFile = cfg;
            }
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(confFile), "UTF-8"));
            while (true) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine != null) {
                    String[] apps = line.trim().split(",");
                    this.mListedApps.add(new DelPackage(apps[0], Integer.parseInt(apps[1])));
                } else {
                    try {
                        reader.close();
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        } catch (NoClassDefFoundError e2) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            if (reader != null) {
                reader.close();
            }
        } catch (FileNotFoundException e3) {
            Log.i(TAG, "boot_delete_apps.cfg Not Found.");
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e4) {
            Log.i(TAG, "boot_delete_apps.cfg IOException");
            e4.printStackTrace();
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e5) {
                    e5.printStackTrace();
                }
            }
        }
    }

    public boolean isUnAppInstallAllowed(String originPath, Context context) {
        if (context == null || (!isUnInstallerCheck(context) && !isUnInstallerValid(originPath, context))) {
            return false;
        }
        return HWLOGW_E;
    }

    private boolean isUnInstallerCheck(Context context) {
        return Secure.getInt(context.getContentResolver(), "hw_uninstall_status", 0) != 0 ? HWLOGW_E : false;
    }

    private boolean isUnInstallerValid(String originPath, Context context) {
        String whiteInstallerPackages = Secure.getString(context.getContentResolver(), "hw_installer_whitelist");
        if (whiteInstallerPackages == null || "".equals(whiteInstallerPackages.trim()) || originPath == null) {
            return false;
        }
        for (String pkg : whiteInstallerPackages.split(";")) {
            if (originPath.contains(pkg)) {
                return false;
            }
        }
        return HWLOGW_E;
    }

    private boolean isDocomo() {
        return SystemProperties.get("ro.product.custom", "NULL").contains("docomo");
    }

    public boolean isSkipMmsSendImageAction() {
        if (!isDocomo()) {
            return false;
        }
        Log.d(TAG, "Not support MMS , skip MMS SEND action !");
        return HWLOGW_E;
    }

    public List<ResolveInfo> filterResolveInfos(List<ResolveInfo> rInfos, Intent intent, String resolvedType) {
        if (isDocomo() && rInfos != null && intent != null && rInfos.size() > 0) {
            String action = intent.getAction();
            Iterator<ResolveInfo> rIter = rInfos.iterator();
            boolean justText = isJustText(intent);
            while (rIter.hasNext()) {
                ResolveInfo rInfo = (ResolveInfo) rIter.next();
                if (rInfo.activityInfo != null) {
                    String pkgName = rInfo.activityInfo.packageName;
                    if (resolvedType != null && (!(resolvedType.contains("text/") && justText) && (("android.intent.action.SEND".equals(action) || "android.intent.action.SEND_MULTIPLE".equals(action)) && "com.android.mms".equals(pkgName)))) {
                        rIter.remove();
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("skip Mms for send image :");
                        stringBuilder.append(action);
                        stringBuilder.append(", pkg:");
                        stringBuilder.append(pkgName);
                        Log.d(str, stringBuilder.toString());
                    }
                }
            }
        }
        return rInfos;
    }

    private boolean isJustText(Intent intent) {
        ClipData clipData = intent.getClipData();
        if (clipData == null) {
            return false;
        }
        for (int i = 0; i < clipData.getItemCount(); i++) {
            Item item = clipData.getItemAt(i);
            if (item != null && (item.getIntent() != null || item.getUri() != null)) {
                return false;
            }
        }
        return HWLOGW_E;
    }

    public String getCustDefaultLauncher(Context context, String pkg) {
        if (TextUtils.isEmpty(CUST_DEFAULT_LAUNCHER)) {
            return null;
        }
        try {
            context.getPackageManager().getPackageInfoAsUser(CUST_DEFAULT_LAUNCHER, 128, UserHandle.getCallingUserId());
            if (IS_DOCOMO && SIMPLE_LAUNCHER_PACKAGE_NAME.equals(pkg)) {
                return pkg;
            }
            return CUST_DEFAULT_LAUNCHER;
        } catch (NameNotFoundException e) {
            Log.d(TAG, "there is no this cust launcher in system");
            return null;
        }
    }
}
