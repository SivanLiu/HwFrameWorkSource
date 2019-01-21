package com.android.server.om;

import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.content.pm.UserInfo;
import android.hwtheme.HwThemeManager;
import android.os.IUserManager.Stub;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class OverlayManagerServiceImpl {
    private static final String AMOLED = "AMOLED";
    private static final String DARK = "dark";
    private static final String[] EXT_WHILTLIST_APP = new String[]{OverlayManagerSettings.FWK_DARK_TAG, OverlayManagerSettings.FWK_HONOR_TAG, OverlayManagerSettings.FWK_NOVA_TAG};
    private static final String FILEPATH = "/sys/class/graphics/fb0/panel_info";
    private static final int FLAG_OVERLAY_IS_UPGRADING = 2;
    private static final int FLAG_TARGET_IS_UPGRADING = 1;
    private static final String NO_DARK = "noDark";
    private static final String PREX = "lcdtype:";
    private static final String UN_KNOWN = "Unknown";
    private static String sThemetype = null;
    private final String[] mDefaultOverlays;
    private final IdmapManager mIdmapManager;
    private final OverlayChangeListener mListener;
    private final PackageManagerHelper mPackageManager;
    private final OverlayManagerSettings mSettings;
    private UserManagerService mUserManager;

    interface OverlayChangeListener {
        void onOverlaysChanged(String str, int i);
    }

    interface PackageManagerHelper {
        List<PackageInfo> getOverlayPackages(int i);

        PackageInfo getPackageInfo(String str, int i);

        boolean signaturesMatching(String str, String str2, int i);
    }

    private static boolean mustReinitializeOverlay(PackageInfo theTruth, OverlayInfo oldSettings) {
        if (oldSettings == null || !Objects.equals(theTruth.overlayTarget, oldSettings.targetPackageName) || theTruth.isStaticOverlayPackage() != oldSettings.isStatic) {
            return true;
        }
        if (!theTruth.isStaticOverlayPackage() || theTruth.overlayPriority == oldSettings.priority) {
            return false;
        }
        return true;
    }

    OverlayManagerServiceImpl(PackageManagerHelper packageManager, IdmapManager idmapManager, OverlayManagerSettings settings, String[] defaultOverlays, OverlayChangeListener listener) {
        this.mPackageManager = packageManager;
        this.mIdmapManager = idmapManager;
        this.mSettings = settings;
        this.mDefaultOverlays = defaultOverlays;
        this.mListener = listener;
    }

    ArrayList<String> updateOverlaysForUser(int newUserId) {
        StringBuilder stringBuilder;
        int i;
        int chunkSize;
        int j;
        PackageInfo overlayPackage;
        ArrayMap<String, OverlayInfo> storedOverlayInfos;
        boolean isHiddenSpace;
        UserInfo newUserInfo;
        boolean isDarkTheme;
        ArrayMap<String, List<OverlayInfo>> tmp;
        int tmpSize;
        List<PackageInfo> overlayPackages;
        OverlayInfo oi;
        int i2;
        ArrayMap<String, OverlayInfo> storedOverlayInfos2;
        boolean isDarkTheme2;
        boolean isHiddenSpace2;
        BadKeyException e;
        Iterator<String> iter;
        Iterator<String> iter2;
        boolean isDarkTheme3;
        String[] strArr;
        int i3;
        ArraySet<String> enabledCategories;
        int storedOverlayInfos3 = newUserId;
        if (sThemetype == null || UN_KNOWN.equals(sThemetype)) {
            boolean isAmoledPanel = isAmoledPanel();
            if (sThemetype == null) {
                setThemetype(isAmoledPanel ? DARK : NO_DARK);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateOverlaysForUser newUserId=");
            stringBuilder.append(storedOverlayInfos3);
            stringBuilder.append(",sThemetype=");
            stringBuilder.append(sThemetype);
            Slog.d("OverlayManager", stringBuilder.toString());
        }
        boolean isDarkTheme4 = DARK.equals(sThemetype);
        boolean isHonorType = HwThemeManager.isHonorProduct();
        ArraySet packagesToUpdateAssets = new ArraySet();
        ArrayMap<String, List<OverlayInfo>> tmp2 = this.mSettings.getOverlaysForUser(storedOverlayInfos3);
        int tmpSize2 = tmp2.size();
        UserInfo newUserInfo2 = getUserManager().getUserInfo(storedOverlayInfos3);
        boolean isHiddenSpace3 = newUserInfo2 == null ? false : newUserInfo2.isHwHiddenSpace();
        ArrayMap<String, OverlayInfo> storedOverlayInfos4 = new ArrayMap(tmpSize2);
        for (i = 0; i < tmpSize2; i++) {
            List<OverlayInfo> chunk = (List) tmp2.valueAt(i);
            chunkSize = chunk.size();
            for (j = 0; j < chunkSize; j++) {
                OverlayInfo oi2 = (OverlayInfo) chunk.get(j);
                if (!filterOverlayinfos(isHonorType, isDarkTheme4, oi2.packageName, isHiddenSpace3)) {
                    storedOverlayInfos4.put(oi2.packageName, oi2);
                }
            }
        }
        List<PackageInfo> overlayPackages2 = this.mPackageManager.getOverlayPackages(storedOverlayInfos3);
        int overlayPackagesSize = overlayPackages2.size();
        i = 0;
        while (i < overlayPackagesSize) {
            overlayPackage = (PackageInfo) overlayPackages2.get(i);
            if (filterOverlayinfos(isHonorType, isDarkTheme4, overlayPackage.packageName, isHiddenSpace3)) {
                storedOverlayInfos = storedOverlayInfos4;
                isHiddenSpace = isHiddenSpace3;
                newUserInfo = newUserInfo2;
                isDarkTheme = isDarkTheme4;
                tmp = tmp2;
                tmpSize = tmpSize2;
                tmpSize2 = overlayPackagesSize;
                overlayPackages = overlayPackages2;
            } else {
                PackageInfo overlayPackage2;
                oi = (OverlayInfo) storedOverlayInfos4.get(overlayPackage.packageName);
                if (mustReinitializeOverlay(overlayPackage, oi)) {
                    if (oi != null) {
                        packagesToUpdateAssets.add(oi.targetPackageName);
                    }
                    OverlayManagerSettings overlayManagerSettings = this.mSettings;
                    String str = overlayPackage.packageName;
                    int overlayPackagesSize2 = overlayPackagesSize;
                    String str2 = overlayPackage.overlayTarget;
                    ArrayMap<String, OverlayInfo> storedOverlayInfos5 = storedOverlayInfos4;
                    String baseCodePath = overlayPackage.applicationInfo.getBaseCodePath();
                    boolean isStaticOverlayPackage = overlayPackage.isStaticOverlayPackage();
                    List<PackageInfo> overlayPackages3 = overlayPackages2;
                    i2 = overlayPackage.overlayPriority;
                    UserInfo newUserInfo3 = newUserInfo2;
                    String str3 = overlayPackage.overlayCategory;
                    tmp = tmp2;
                    overlayPackage2 = overlayPackage;
                    j = storedOverlayInfos3;
                    tmpSize = tmpSize2;
                    tmpSize2 = overlayPackagesSize2;
                    storedOverlayInfos = storedOverlayInfos5;
                    isDarkTheme = isDarkTheme4;
                    isHiddenSpace = isHiddenSpace3;
                    overlayPackages = overlayPackages3;
                    newUserInfo = newUserInfo3;
                    overlayManagerSettings.init(str, j, str2, baseCodePath, isStaticOverlayPackage, i2, str3);
                } else {
                    storedOverlayInfos = storedOverlayInfos4;
                    isHiddenSpace = isHiddenSpace3;
                    newUserInfo = newUserInfo2;
                    isDarkTheme = isDarkTheme4;
                    tmp = tmp2;
                    tmpSize = tmpSize2;
                    overlayPackage2 = overlayPackage;
                    tmpSize2 = overlayPackagesSize;
                    overlayPackages = overlayPackages2;
                }
                storedOverlayInfos.remove(overlayPackage2.packageName);
            }
            i++;
            storedOverlayInfos4 = storedOverlayInfos;
            overlayPackages2 = overlayPackages;
            overlayPackagesSize = tmpSize2;
            newUserInfo2 = newUserInfo;
            tmp2 = tmp;
            tmpSize2 = tmpSize;
            isDarkTheme4 = isDarkTheme;
            isHiddenSpace3 = isHiddenSpace;
            storedOverlayInfos3 = newUserId;
        }
        storedOverlayInfos = storedOverlayInfos4;
        isHiddenSpace = isHiddenSpace3;
        newUserInfo = newUserInfo2;
        isDarkTheme = isDarkTheme4;
        tmp = tmp2;
        tmpSize = tmpSize2;
        tmpSize2 = overlayPackagesSize;
        overlayPackages = overlayPackages2;
        int storedOverlayInfosSize = storedOverlayInfos.size();
        for (i = 0; i < storedOverlayInfosSize; i++) {
            oi = (OverlayInfo) storedOverlayInfos.valueAt(i);
            this.mSettings.remove(oi.packageName, oi.userId);
            removeIdmapIfPossible(oi);
            packagesToUpdateAssets.add(oi.targetPackageName);
        }
        i = 0;
        while (true) {
            chunkSize = i;
            if (chunkSize >= tmpSize2) {
                break;
            }
            overlayPackage = (PackageInfo) overlayPackages.get(chunkSize);
            if (overlayPackage.isStaticOverlayPackage() && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(overlayPackage.overlayTarget)) {
                storedOverlayInfos2 = storedOverlayInfos;
                isDarkTheme2 = isDarkTheme;
                isHiddenSpace2 = isHiddenSpace;
            } else {
                isDarkTheme2 = isDarkTheme;
                isHiddenSpace2 = isHiddenSpace;
                if (filterOverlayinfos(isHonorType, isDarkTheme2, overlayPackage.packageName, isHiddenSpace2)) {
                    storedOverlayInfos2 = storedOverlayInfos;
                } else {
                    try {
                        storedOverlayInfos2 = storedOverlayInfos;
                        i2 = newUserId;
                        try {
                            updateState(overlayPackage.overlayTarget, overlayPackage.packageName, i2, null);
                        } catch (BadKeyException e2) {
                            e = e2;
                        }
                    } catch (BadKeyException e3) {
                        e = e3;
                        storedOverlayInfos2 = storedOverlayInfos;
                        i2 = newUserId;
                        Slog.e("OverlayManager", "failed to update settings", e);
                        this.mSettings.remove(overlayPackage.packageName, i2);
                        packagesToUpdateAssets.add(overlayPackage.overlayTarget);
                        i = chunkSize + 1;
                        isDarkTheme = isDarkTheme2;
                        isHiddenSpace = isHiddenSpace2;
                        storedOverlayInfos = storedOverlayInfos2;
                    }
                    packagesToUpdateAssets.add(overlayPackage.overlayTarget);
                    i = chunkSize + 1;
                    isDarkTheme = isDarkTheme2;
                    isHiddenSpace = isHiddenSpace2;
                    storedOverlayInfos = storedOverlayInfos2;
                }
            }
            i2 = newUserId;
            i = chunkSize + 1;
            isDarkTheme = isDarkTheme2;
            isHiddenSpace = isHiddenSpace2;
            storedOverlayInfos = storedOverlayInfos2;
        }
        isDarkTheme2 = isDarkTheme;
        isHiddenSpace2 = isHiddenSpace;
        i2 = newUserId;
        Iterator<String> iter3 = packagesToUpdateAssets.iterator();
        while (true) {
            iter = iter3;
            if (!iter.hasNext()) {
                break;
            }
            if (this.mPackageManager.getPackageInfo((String) iter.next(), i2) == null) {
                iter.remove();
            }
            iter3 = iter;
        }
        ArraySet<String> enabledCategories2 = new ArraySet();
        ArrayMap<String, List<OverlayInfo>> userOverlays = this.mSettings.getOverlaysForUser(i2);
        int userOverlayTargetCount = userOverlays.size();
        i = 0;
        while (i < userOverlayTargetCount) {
            int storedOverlayInfosSize2;
            List<OverlayInfo> overlayList = (List) userOverlays.valueAt(i);
            int overlayCount = overlayList != null ? overlayList.size() : 0;
            int j2 = 0;
            while (true) {
                storedOverlayInfosSize2 = storedOverlayInfosSize;
                iter2 = iter;
                storedOverlayInfosSize = overlayCount;
                chunkSize = j2;
                if (chunkSize >= storedOverlayInfosSize) {
                    break;
                }
                int overlayCount2 = storedOverlayInfosSize;
                OverlayInfo storedOverlayInfosSize3 = (OverlayInfo) overlayList.get(chunkSize);
                if (storedOverlayInfosSize3.isEnabled()) {
                    isDarkTheme3 = isDarkTheme2;
                    enabledCategories2.add(storedOverlayInfosSize3.category);
                } else {
                    isDarkTheme3 = isDarkTheme2;
                }
                j2 = chunkSize + 1;
                storedOverlayInfosSize = storedOverlayInfosSize2;
                iter = iter2;
                overlayCount = overlayCount2;
                isDarkTheme2 = isDarkTheme3;
            }
            i++;
            storedOverlayInfosSize = storedOverlayInfosSize2;
            iter = iter2;
        }
        iter2 = iter;
        isDarkTheme3 = isDarkTheme2;
        String[] strArr2 = this.mDefaultOverlays;
        chunkSize = strArr2.length;
        overlayPackagesSize = 0;
        while (overlayPackagesSize < chunkSize) {
            String defaultOverlay = strArr2[overlayPackagesSize];
            try {
                OverlayInfo oi3 = this.mSettings.getOverlayInfo(defaultOverlay, i2);
                strArr = strArr2;
                try {
                    if (enabledCategories2.contains(oi3.category)) {
                        i3 = chunkSize;
                        enabledCategories = enabledCategories2;
                    } else {
                        String str4 = "OverlayManager";
                        i3 = chunkSize;
                        try {
                            stringBuilder = new StringBuilder();
                            enabledCategories = enabledCategories2;
                            try {
                                stringBuilder.append("Enabling default overlay '");
                                stringBuilder.append(defaultOverlay);
                                stringBuilder.append("' for target '");
                                stringBuilder.append(oi3.targetPackageName);
                                stringBuilder.append("' in category '");
                                stringBuilder.append(oi3.category);
                                stringBuilder.append("' for user ");
                                stringBuilder.append(i2);
                                Slog.w(str4, stringBuilder.toString());
                                this.mSettings.setEnabled(oi3.packageName, i2, true);
                            } catch (BadKeyException e4) {
                                e = e4;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Failed to set default overlay '");
                                stringBuilder.append(defaultOverlay);
                                stringBuilder.append("' for user ");
                                stringBuilder.append(i2);
                                Slog.e("OverlayManager", stringBuilder.toString(), e);
                                overlayPackagesSize++;
                                strArr2 = strArr;
                                chunkSize = i3;
                                enabledCategories2 = enabledCategories;
                            }
                        } catch (BadKeyException e5) {
                            e = e5;
                            enabledCategories = enabledCategories2;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to set default overlay '");
                            stringBuilder.append(defaultOverlay);
                            stringBuilder.append("' for user ");
                            stringBuilder.append(i2);
                            Slog.e("OverlayManager", stringBuilder.toString(), e);
                            overlayPackagesSize++;
                            strArr2 = strArr;
                            chunkSize = i3;
                            enabledCategories2 = enabledCategories;
                        }
                        try {
                            if (updateState(oi3.targetPackageName, oi3.packageName, i2, 0)) {
                                packagesToUpdateAssets.add(oi3.targetPackageName);
                            }
                        } catch (BadKeyException e6) {
                            e = e6;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to set default overlay '");
                            stringBuilder.append(defaultOverlay);
                            stringBuilder.append("' for user ");
                            stringBuilder.append(i2);
                            Slog.e("OverlayManager", stringBuilder.toString(), e);
                            overlayPackagesSize++;
                            strArr2 = strArr;
                            chunkSize = i3;
                            enabledCategories2 = enabledCategories;
                        }
                    }
                } catch (BadKeyException e7) {
                    e = e7;
                    i3 = chunkSize;
                    enabledCategories = enabledCategories2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to set default overlay '");
                    stringBuilder.append(defaultOverlay);
                    stringBuilder.append("' for user ");
                    stringBuilder.append(i2);
                    Slog.e("OverlayManager", stringBuilder.toString(), e);
                    overlayPackagesSize++;
                    strArr2 = strArr;
                    chunkSize = i3;
                    enabledCategories2 = enabledCategories;
                }
            } catch (BadKeyException e8) {
                e = e8;
                strArr = strArr2;
                i3 = chunkSize;
                enabledCategories = enabledCategories2;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to set default overlay '");
                stringBuilder.append(defaultOverlay);
                stringBuilder.append("' for user ");
                stringBuilder.append(i2);
                Slog.e("OverlayManager", stringBuilder.toString(), e);
                overlayPackagesSize++;
                strArr2 = strArr;
                chunkSize = i3;
                enabledCategories2 = enabledCategories;
            }
            overlayPackagesSize++;
            strArr2 = strArr;
            chunkSize = i3;
            enabledCategories2 = enabledCategories;
        }
        return new ArrayList(packagesToUpdateAssets);
    }

    private UserManagerService getUserManager() {
        if (this.mUserManager == null) {
            this.mUserManager = (UserManagerService) Stub.asInterface(ServiceManager.getService("user"));
        }
        return this.mUserManager;
    }

    private boolean filterOverlayinfos(boolean isHonorType, boolean isAmoledPanel, String packageName, boolean isHiddenSpaceUser) {
        boolean z = true;
        if (isHonorType) {
            if (!(OverlayManagerSettings.FWK_DARK_TAG.equals(packageName) || OverlayManagerSettings.FWK_NOVA_TAG.equals(packageName) || OverlayManagerSettings.FWK_DARK_OVERLAY_TAG.equals(packageName))) {
                z = false;
            }
            return z;
        } else if (HwThemeManager.isNovaProduct()) {
            if (!(OverlayManagerSettings.FWK_DARK_TAG.equals(packageName) || OverlayManagerSettings.FWK_DARK_OVERLAY_TAG.equals(packageName) || OverlayManagerSettings.FWK_HONOR_TAG.equals(packageName))) {
                z = false;
            }
            return z;
        } else if (isAmoledPanel) {
            if (!(OverlayManagerSettings.FWK_HONOR_TAG.equals(packageName) || OverlayManagerSettings.FWK_NOVA_TAG.equals(packageName))) {
                z = false;
            }
            return z;
        } else {
            boolean isHiddenSpaceEmulation = false;
            if (isHiddenSpaceUser) {
                boolean z2 = OverlayManagerSettings.FWK_EMULATION_NARROW_TAG.equals(packageName) || OverlayManagerSettings.FWK_EMULATION_TALL_TAG.equals(packageName) || OverlayManagerSettings.FWK_EMULATION_WIDE_TAG.equals(packageName);
                isHiddenSpaceEmulation = z2;
            }
            if (!(OverlayManagerSettings.FWK_HONOR_TAG.equals(packageName) || OverlayManagerSettings.FWK_NOVA_TAG.equals(packageName) || OverlayManagerSettings.FWK_DARK_TAG.equals(packageName) || OverlayManagerSettings.FWK_DARK_OVERLAY_TAG.equals(packageName) || isHiddenSpaceEmulation)) {
                z = false;
            }
            return z;
        }
    }

    private boolean isAmoledPanel() {
        String lineValue = readFileByChars(FILEPATH);
        if (TextUtils.isEmpty(lineValue)) {
            return false;
        }
        for (String temp : lineValue.trim().split(",")) {
            if (temp.startsWith(PREX) && temp.split(":")[1].equalsIgnoreCase(AMOLED)) {
                return true;
            }
        }
        return false;
    }

    private static void setThemetype(String type) {
        sThemetype = type;
    }

    private String readFileByChars(String fileName) {
        StringBuilder stringBuilder;
        File file = new File(fileName);
        if (!file.exists() || !file.canRead()) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        Reader reader = null;
        char[] tempChars = new char[512];
        StringBuilder sb = new StringBuilder();
        try {
            reader = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
            while (true) {
                int read = reader.read(tempChars, 0, tempChars.length);
                int charRead = read;
                if (read != -1) {
                    sb.append(tempChars, 0, charRead);
                } else {
                    try {
                        break;
                    } catch (IOException e) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to close ");
                        stringBuilder.append(fileName);
                        Slog.e("OverlayManager", stringBuilder.toString());
                    }
                }
            }
            reader.close();
            setThemetype(null);
            return sb.toString();
        } catch (IOException e2) {
            setThemetype(UN_KNOWN);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read ");
            stringBuilder.append(fileName);
            Slog.e("OverlayManager", stringBuilder.toString());
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to close ");
                    stringBuilder2.append(fileName);
                    Slog.e("OverlayManager", stringBuilder2.toString());
                }
            }
            return null;
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e4) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Failed to close ");
                    stringBuilder3.append(fileName);
                    Slog.e("OverlayManager", stringBuilder3.toString());
                }
            }
        }
    }

    void onUserRemoved(int userId) {
        this.mSettings.removeUser(userId);
    }

    void onTargetPackageAdded(String packageName, int userId) {
        if (updateAllOverlaysForTarget(packageName, userId, 0)) {
            this.mListener.onOverlaysChanged(packageName, userId);
        }
    }

    void onTargetPackageChanged(String packageName, int userId) {
        updateAllOverlaysForTarget(packageName, userId, 0);
    }

    void onTargetPackageUpgrading(String packageName, int userId) {
        updateAllOverlaysForTarget(packageName, userId, 1);
    }

    void onTargetPackageUpgraded(String packageName, int userId) {
        updateAllOverlaysForTarget(packageName, userId, 0);
    }

    void onTargetPackageRemoved(String packageName, int userId) {
        if (updateAllOverlaysForTarget(packageName, userId, 0)) {
            this.mListener.onOverlaysChanged(packageName, userId);
        }
    }

    private boolean updateAllOverlaysForTarget(String targetPackageName, int userId, int flags) {
        List<OverlayInfo> ois = this.mSettings.getOverlaysForTarget(targetPackageName, userId);
        int N = ois.size();
        boolean z = false;
        boolean modified = false;
        for (int i = 0; i < N; i++) {
            OverlayInfo oi = (OverlayInfo) ois.get(i);
            if (this.mPackageManager.getPackageInfo(oi.packageName, userId) == null) {
                modified |= this.mSettings.remove(oi.packageName, oi.userId);
                removeIdmapIfPossible(oi);
            } else {
                try {
                    modified |= updateState(targetPackageName, oi.packageName, userId, flags);
                } catch (BadKeyException e) {
                    Slog.e("OverlayManager", "failed to update settings", e);
                    modified |= this.mSettings.remove(oi.packageName, userId);
                }
            }
        }
        if (modified || !getEnabledOverlayPackageNames(PackageManagerService.PLATFORM_PACKAGE_NAME, userId).isEmpty()) {
            z = true;
        }
        return z;
    }

    void onOverlayPackageAdded(String packageName, int userId) {
        PackageInfo overlayPackage = this.mPackageManager.getPackageInfo(packageName, userId);
        if (overlayPackage == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("overlay package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" was added, but couldn't be found");
            Slog.w("OverlayManager", stringBuilder.toString());
            onOverlayPackageRemoved(packageName, userId);
            return;
        }
        this.mSettings.init(packageName, userId, overlayPackage.overlayTarget, overlayPackage.applicationInfo.getBaseCodePath(), overlayPackage.isStaticOverlayPackage(), overlayPackage.overlayPriority, overlayPackage.overlayCategory);
        try {
            if (updateState(overlayPackage.overlayTarget, packageName, userId, 0)) {
                this.mListener.onOverlaysChanged(overlayPackage.overlayTarget, userId);
            }
        } catch (BadKeyException e) {
            Slog.e("OverlayManager", "failed to update settings", e);
            this.mSettings.remove(packageName, userId);
        }
    }

    void onOverlayPackageChanged(String packageName, int userId) {
        try {
            OverlayInfo oi = this.mSettings.getOverlayInfo(packageName, userId);
            if (updateState(oi.targetPackageName, packageName, userId, 0)) {
                this.mListener.onOverlaysChanged(oi.targetPackageName, userId);
            }
        } catch (BadKeyException e) {
            Slog.e("OverlayManager", "failed to update settings", e);
        }
    }

    void onOverlayPackageUpgrading(String packageName, int userId) {
        try {
            OverlayInfo oi = this.mSettings.getOverlayInfo(packageName, userId);
            if (updateState(oi.targetPackageName, packageName, userId, 2)) {
                removeIdmapIfPossible(oi);
                this.mListener.onOverlaysChanged(oi.targetPackageName, userId);
            }
        } catch (BadKeyException e) {
            Slog.e("OverlayManager", "failed to update settings", e);
        }
    }

    void onOverlayPackageUpgraded(String packageName, int userId) {
        PackageInfo pkg = this.mPackageManager.getPackageInfo(packageName, userId);
        if (pkg == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("overlay package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" was upgraded, but couldn't be found");
            Slog.w("OverlayManager", stringBuilder.toString());
            onOverlayPackageRemoved(packageName, userId);
            return;
        }
        try {
            OverlayInfo oldOi = this.mSettings.getOverlayInfo(packageName, userId);
            if (mustReinitializeOverlay(pkg, oldOi)) {
                if (!(oldOi == null || oldOi.targetPackageName.equals(pkg.overlayTarget))) {
                    this.mListener.onOverlaysChanged(pkg.overlayTarget, userId);
                }
                this.mSettings.init(packageName, userId, pkg.overlayTarget, pkg.applicationInfo.getBaseCodePath(), pkg.isStaticOverlayPackage(), pkg.overlayPriority, pkg.overlayCategory);
            }
            if (updateState(pkg.overlayTarget, packageName, userId, 0)) {
                this.mListener.onOverlaysChanged(pkg.overlayTarget, userId);
            }
        } catch (BadKeyException e) {
            Slog.e("OverlayManager", "failed to update settings", e);
        }
    }

    void onOverlayPackageRemoved(String packageName, int userId) {
        try {
            OverlayInfo overlayInfo = this.mSettings.getOverlayInfo(packageName, userId);
            if (this.mSettings.remove(packageName, userId)) {
                removeIdmapIfPossible(overlayInfo);
                if (overlayInfo.isEnabled()) {
                    this.mListener.onOverlaysChanged(overlayInfo.targetPackageName, userId);
                }
            }
        } catch (BadKeyException e) {
            Slog.e("OverlayManager", "failed to remove overlay", e);
        }
    }

    OverlayInfo getOverlayInfo(String packageName, int userId) {
        try {
            return this.mSettings.getOverlayInfo(packageName, userId);
        } catch (BadKeyException e) {
            return null;
        }
    }

    List<OverlayInfo> getOverlayInfosForTarget(String targetPackageName, int userId) {
        return this.mSettings.getOverlaysForTarget(targetPackageName, userId);
    }

    Map<String, List<OverlayInfo>> getOverlaysForUser(int userId) {
        return this.mSettings.getOverlaysForUser(userId);
    }

    boolean setEnabled(String packageName, boolean enable, int userId) {
        PackageInfo overlayPackage = this.mPackageManager.getPackageInfo(packageName, userId);
        if (overlayPackage == null || overlayPackage.isStaticOverlayPackage()) {
            return false;
        }
        try {
            OverlayInfo oi = this.mSettings.getOverlayInfo(packageName, userId);
            if ((this.mSettings.setEnabled(packageName, userId, enable) | updateState(oi.targetPackageName, oi.packageName, userId, 0)) | checkWhiteExtList(packageName)) {
                this.mListener.onOverlaysChanged(oi.targetPackageName, userId);
            }
            return true;
        } catch (BadKeyException e) {
            return false;
        }
    }

    boolean setEnabledExclusive(String packageName, boolean withinCategory, int userId) {
        if (this.mPackageManager.getPackageInfo(packageName, userId) == null) {
            return false;
        }
        try {
            OverlayInfo oi = this.mSettings.getOverlayInfo(packageName, userId);
            String targetPackageName = oi.targetPackageName;
            List<OverlayInfo> allOverlays = getOverlayInfosForTarget(targetPackageName, userId);
            allOverlays.remove(oi);
            boolean modified = false;
            for (int i = 0; i < allOverlays.size(); i++) {
                String disabledOverlayPackageName = ((OverlayInfo) allOverlays.get(i)).packageName;
                PackageInfo disabledOverlayPackageInfo = this.mPackageManager.getPackageInfo(disabledOverlayPackageName, userId);
                if (disabledOverlayPackageInfo == null) {
                    modified |= this.mSettings.remove(disabledOverlayPackageName, userId);
                } else if (!disabledOverlayPackageInfo.isStaticOverlayPackage()) {
                    if (!withinCategory || Objects.equals(disabledOverlayPackageInfo.overlayCategory, oi.category)) {
                        modified = (modified | this.mSettings.setEnabled(disabledOverlayPackageName, userId, false)) | updateState(targetPackageName, disabledOverlayPackageName, userId, 0);
                    }
                }
            }
            if (((this.mSettings.setEnabled(packageName, userId, true) | modified) | updateState(targetPackageName, packageName, userId, 0)) | checkWhiteExtList(packageName)) {
                this.mListener.onOverlaysChanged(targetPackageName, userId);
            }
            return true;
        } catch (BadKeyException e) {
            return false;
        }
    }

    private boolean checkWhiteExtList(String packageName) {
        int length = EXT_WHILTLIST_APP.length;
        int i = 0;
        while (i < length) {
            if (packageName != null && packageName.equals(EXT_WHILTLIST_APP[i])) {
                return true;
            }
            i++;
        }
        return false;
    }

    private boolean isPackageUpdatableOverlay(String packageName, int userId) {
        PackageInfo overlayPackage = this.mPackageManager.getPackageInfo(packageName, userId);
        if (overlayPackage == null || overlayPackage.isStaticOverlayPackage()) {
            return false;
        }
        return true;
    }

    boolean setPriority(String packageName, String newParentPackageName, int userId) {
        if (!isPackageUpdatableOverlay(packageName, userId)) {
            return false;
        }
        PackageInfo overlayPackage = this.mPackageManager.getPackageInfo(packageName, userId);
        if (overlayPackage == null) {
            return false;
        }
        if (this.mSettings.setPriority(packageName, newParentPackageName, userId)) {
            this.mListener.onOverlaysChanged(overlayPackage.overlayTarget, userId);
        }
        return true;
    }

    boolean setHighestPriority(String packageName, int userId) {
        if (!isPackageUpdatableOverlay(packageName, userId)) {
            return false;
        }
        PackageInfo overlayPackage = this.mPackageManager.getPackageInfo(packageName, userId);
        if (overlayPackage == null) {
            return false;
        }
        if (this.mSettings.setHighestPriority(packageName, userId)) {
            this.mListener.onOverlaysChanged(overlayPackage.overlayTarget, userId);
        }
        return true;
    }

    boolean setLowestPriority(String packageName, int userId) {
        if (!isPackageUpdatableOverlay(packageName, userId)) {
            return false;
        }
        PackageInfo overlayPackage = this.mPackageManager.getPackageInfo(packageName, userId);
        if (overlayPackage == null) {
            return false;
        }
        if (this.mSettings.setLowestPriority(packageName, userId)) {
            this.mListener.onOverlaysChanged(overlayPackage.overlayTarget, userId);
        }
        return true;
    }

    void onDump(PrintWriter pw) {
        this.mSettings.dump(pw);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Default overlays: ");
        stringBuilder.append(TextUtils.join(";", this.mDefaultOverlays));
        pw.println(stringBuilder.toString());
    }

    List<String> getEnabledOverlayPackageNames(String targetPackageName, int userId) {
        List<OverlayInfo> overlays = this.mSettings.getOverlaysForTarget(targetPackageName, userId);
        List<String> paths = new ArrayList(overlays.size());
        int N = overlays.size();
        for (int i = 0; i < N; i++) {
            OverlayInfo oi = (OverlayInfo) overlays.get(i);
            if (oi.isEnabled()) {
                paths.add(oi.packageName);
            }
        }
        return paths;
    }

    private boolean updateState(String targetPackageName, String overlayPackageName, int userId, int flags) throws BadKeyException {
        PackageInfo targetPackage = this.mPackageManager.getPackageInfo(targetPackageName, userId);
        PackageInfo overlayPackage = this.mPackageManager.getPackageInfo(overlayPackageName, userId);
        if (!(targetPackage == null || overlayPackage == null || (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(targetPackageName) && overlayPackage.isStaticOverlayPackage()))) {
            this.mIdmapManager.createIdmap(targetPackage, overlayPackage, userId);
        }
        boolean modified = false;
        if (overlayPackage != null) {
            modified = (false | this.mSettings.setBaseCodePath(overlayPackageName, userId, overlayPackage.applicationInfo.getBaseCodePath())) | this.mSettings.setCategory(overlayPackageName, userId, overlayPackage.overlayCategory);
        }
        int currentState = this.mSettings.getState(overlayPackageName, userId);
        int newState = calculateNewState(targetPackage, overlayPackage, userId, flags);
        if (currentState != newState) {
            return modified | this.mSettings.setState(overlayPackageName, userId, newState);
        }
        return modified;
    }

    private int calculateNewState(PackageInfo targetPackage, PackageInfo overlayPackage, int userId, int flags) throws BadKeyException {
        if ((flags & 1) != 0) {
            return 4;
        }
        if ((flags & 2) != 0) {
            return 5;
        }
        if (targetPackage == null) {
            return 0;
        }
        if (!this.mIdmapManager.idmapExists(overlayPackage, userId)) {
            return 1;
        }
        if (overlayPackage.isStaticOverlayPackage()) {
            return 6;
        }
        return this.mSettings.getEnabled(overlayPackage.packageName, userId) ? 3 : 2;
    }

    private void removeIdmapIfPossible(OverlayInfo oi) {
        if (this.mIdmapManager.idmapExists(oi)) {
            for (int userId : this.mSettings.getUsers()) {
                try {
                    OverlayInfo tmp = this.mSettings.getOverlayInfo(oi.packageName, userId);
                    if (tmp != null && tmp.isEnabled()) {
                        return;
                    }
                } catch (BadKeyException e) {
                }
            }
            this.mIdmapManager.removeIdmap(oi, oi.userId);
        }
    }
}
