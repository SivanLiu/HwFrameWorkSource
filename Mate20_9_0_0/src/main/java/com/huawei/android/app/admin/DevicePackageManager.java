package com.huawei.android.app.admin;

import android.content.ComponentName;
import android.os.Bundle;
import huawei.android.app.admin.HwDevicePolicyManagerEx;
import java.util.ArrayList;
import java.util.List;

public class DevicePackageManager {
    public static final String APP_NOTIFY_WHITE_LIST = "notification-app-bt-white-list";
    private static final String TAG = "DevicePackageManager";
    private final HwDevicePolicyManagerEx mDpm = new HwDevicePolicyManagerEx();

    public void installPackage(ComponentName admin, String packagePath) {
        this.mDpm.installPackage(admin, packagePath);
    }

    public void uninstallPackage(ComponentName admin, String packageName, boolean keepData) {
        this.mDpm.uninstallPackage(admin, packageName, keepData);
    }

    public void clearPackageData(ComponentName admin, String packageName) {
        this.mDpm.clearPackageData(admin, packageName);
    }

    public void enableInstallPackage(ComponentName admin) {
        this.mDpm.enableInstallPackage(admin);
    }

    public void disableInstallSource(ComponentName admin, List<String> whitelist) {
        this.mDpm.disableInstallSource(admin, whitelist);
    }

    public boolean isInstallSourceDisabled(ComponentName admin) {
        return this.mDpm.isInstallSourceDisabled(admin);
    }

    public List<String> getInstallPackageSourceWhiteList(ComponentName admin) {
        return this.mDpm.getInstallPackageSourceWhiteList(admin);
    }

    public void addInstallPackageWhiteList(ComponentName admin, List<String> packageNames) {
        this.mDpm.addInstallPackageWhiteList(admin, packageNames);
    }

    public void removeInstallPackageWhiteList(ComponentName admin, List<String> packageNames) {
        this.mDpm.removeInstallPackageWhiteList(admin, packageNames);
    }

    public List<String> getInstallPackageWhiteList(ComponentName admin) {
        return this.mDpm.getInstallPackageWhiteList(admin);
    }

    public void addDisallowedUninstallPackages(ComponentName admin, List<String> packageNames) {
        this.mDpm.addDisallowedUninstallPackages(admin, packageNames);
    }

    public void removeDisallowedUninstallPackages(ComponentName admin, List<String> packageNames) {
        this.mDpm.removeDisallowedUninstallPackages(admin, packageNames);
    }

    public List<String> getDisallowedUninstallPackageList(ComponentName admin) {
        return this.mDpm.getDisallowedUninstallPackageList(admin);
    }

    public void addDisabledDeactivateMdmPackages(ComponentName admin, List<String> packageNames) {
        this.mDpm.addDisabledDeactivateMdmPackages(admin, packageNames);
    }

    public void removeDisabledDeactivateMdmPackages(ComponentName admin, List<String> packageNames) {
        this.mDpm.removeDisabledDeactivateMdmPackages(admin, packageNames);
    }

    public List<String> getDisabledDeactivateMdmPackageList(ComponentName admin) {
        return this.mDpm.getDisabledDeactivateMdmPackageList(admin);
    }

    public boolean addAllowNotificationApps(ComponentName admin, ArrayList<String> packageNames) {
        if (packageNames == null || packageNames.isEmpty()) {
            return false;
        }
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("value", filterListPkg(admin, packageNames));
        return this.mDpm.setPolicy(admin, APP_NOTIFY_WHITE_LIST, bundle);
    }

    public boolean removeAllowNotificationApps(ComponentName admin, ArrayList<String> packageNames) {
        if (packageNames == null || packageNames.isEmpty()) {
            return false;
        }
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("value", packageNames);
        return this.mDpm.removePolicy(admin, APP_NOTIFY_WHITE_LIST, bundle);
    }

    public ArrayList<String> getAllowNotificationApps(ComponentName admin) {
        Bundle bundle = this.mDpm.getPolicy(admin, APP_NOTIFY_WHITE_LIST);
        if (bundle == null) {
            return null;
        }
        return bundle.getStringArrayList("value");
    }

    private ArrayList<String> filterListPkg(ComponentName admin, ArrayList<String> addListPkg) {
        List<String> listPkg = getAllowNotificationApps(admin);
        ArrayList<String> tempList = new ArrayList();
        if (listPkg == null || listPkg.isEmpty()) {
            return addListPkg;
        }
        int size = addListPkg.size();
        for (int i = 0; i < size; i++) {
            String pkgName = (String) addListPkg.get(i);
            if (!listPkg.contains(pkgName)) {
                tempList.add(pkgName);
            }
        }
        return tempList;
    }
}
