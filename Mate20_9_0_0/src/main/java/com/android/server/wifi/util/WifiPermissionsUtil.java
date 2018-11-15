package com.android.server.wifi.util;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiLog;
import com.android.server.wifi.WifiSettingsStore;

public class WifiPermissionsUtil {
    private static final String TAG = "WifiPermissionsUtil";
    private final AppOpsManager mAppOps = ((AppOpsManager) this.mContext.getSystemService("appops"));
    private final Context mContext;
    private WifiLog mLog;
    private final WifiSettingsStore mSettingsStore;
    private final UserManager mUserManager;
    private final WifiPermissionsWrapper mWifiPermissionsWrapper;

    public WifiPermissionsUtil(WifiPermissionsWrapper wifiPermissionsWrapper, Context context, WifiSettingsStore settingsStore, UserManager userManager, WifiInjector wifiInjector) {
        this.mWifiPermissionsWrapper = wifiPermissionsWrapper;
        this.mContext = context;
        this.mUserManager = userManager;
        this.mSettingsStore = settingsStore;
        this.mLog = wifiInjector.makeLog(TAG);
    }

    public boolean checkConfigOverridePermission(int uid) {
        boolean z = false;
        try {
            if (this.mWifiPermissionsWrapper.getOverrideWifiConfigPermission(uid) == 0) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            this.mLog.err("Error checking for permission: %").r(e.getMessage()).flush();
            return false;
        }
    }

    public boolean checkChangePermission(int uid) {
        boolean z = false;
        try {
            if (this.mWifiPermissionsWrapper.getChangeWifiConfigPermission(uid) == 0) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            this.mLog.err("Error checking for permission: %").r(e.getMessage()).flush();
            return false;
        }
    }

    public boolean checkWifiAccessPermission(int uid) {
        boolean z = false;
        try {
            if (this.mWifiPermissionsWrapper.getAccessWifiStatePermission(uid) == 0) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            this.mLog.err("Error checking for permission: %").r(e.getMessage()).flush();
            return false;
        }
    }

    public void enforceLocationPermission(String pkgName, int uid) {
        if (!checkCallersLocationPermission(pkgName, uid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UID ");
            stringBuilder.append(uid);
            stringBuilder.append(" does not have Coarse Location permission");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public boolean checkCallersLocationPermission(String pkgName, int uid) {
        if (this.mWifiPermissionsWrapper.getUidPermission("android.permission.ACCESS_COARSE_LOCATION", uid) == 0 && checkAppOpAllowed(0, pkgName, uid)) {
            return true;
        }
        return false;
    }

    public void enforceFineLocationPermission(String pkgName, int uid) {
        if (!checkCallersFineLocationPermission(pkgName, uid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UID ");
            stringBuilder.append(uid);
            stringBuilder.append(" does not have Fine Location permission");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public boolean checkCallersFineLocationPermission(String pkgName, int uid) {
        if (this.mWifiPermissionsWrapper.getUidPermission("android.permission.ACCESS_FINE_LOCATION", uid) == 0 && checkAppOpAllowed(1, pkgName, uid)) {
            return true;
        }
        return false;
    }

    public void enforceCanAccessScanResults(String pkgName, int uid) throws SecurityException {
        this.mAppOps.checkPackage(uid, pkgName);
        if (!checkNetworkSettingsPermission(uid) && !checkNetworkSetupWizardPermission(uid)) {
            if (isLocationModeEnabled(pkgName, this.mWifiPermissionsWrapper.getCallingUserId(uid))) {
                boolean canCallingUidAccessLocation = checkCallerHasPeersMacAddressPermission(uid);
                boolean canAppPackageUseLocation = checkCallersLocationPermission(pkgName, uid);
                StringBuilder stringBuilder;
                if (!canCallingUidAccessLocation && !canAppPackageUseLocation) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("UID ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" has no location permission");
                    throw new SecurityException(stringBuilder.toString());
                } else if (!isScanAllowedbyApps(pkgName, uid)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("UID ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" has no wifi scan permission");
                    throw new SecurityException(stringBuilder.toString());
                } else if (!isCurrentProfile(uid) && !checkInteractAcrossUsersFull(uid)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("UID ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" profile not permitted");
                    throw new SecurityException(stringBuilder.toString());
                } else {
                    return;
                }
            }
            throw new SecurityException("Location mode is disabled for the device");
        }
    }

    private boolean checkCallerHasPeersMacAddressPermission(int uid) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.PEERS_MAC_ADDRESS", uid) == 0;
    }

    private boolean isScanAllowedbyApps(String pkgName, int uid) {
        return checkAppOpAllowed(10, pkgName, uid);
    }

    private boolean checkInteractAcrossUsersFull(int uid) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.INTERACT_ACROSS_USERS_FULL", uid) == 0;
    }

    private boolean isCurrentProfile(int uid) {
        int currentUser = this.mWifiPermissionsWrapper.getCurrentUser();
        int callingUserId = this.mWifiPermissionsWrapper.getCallingUserId(uid);
        if (callingUserId == currentUser) {
            return true;
        }
        for (UserInfo user : this.mUserManager.getProfiles(currentUser)) {
            if (user.id == callingUserId) {
                return true;
            }
        }
        return false;
    }

    public boolean isLegacyVersion(String pkgName, int minVersion) {
        try {
            if (this.mContext.getPackageManager().getApplicationInfoAsUser(pkgName, 0, UserHandle.getUserId(Binder.getCallingUid())).targetSdkVersion < minVersion) {
                return true;
            }
        } catch (NameNotFoundException e) {
        }
        return false;
    }

    private boolean checkAppOpAllowed(int op, String pkgName, int uid) {
        return this.mAppOps.noteOp(op, uid, pkgName) == 0;
    }

    private boolean isLocationModeEnabled(String pkgName, int userId) {
        return this.mSettingsStore.getLocationModeSetting(this.mContext, userId) != 0;
    }

    public boolean checkNetworkSettingsPermission(int uid) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.NETWORK_SETTINGS", uid) == 0;
    }

    public boolean checkNetworkSetupWizardPermission(int uid) {
        return this.mWifiPermissionsWrapper.getUidPermission("android.permission.NETWORK_SETUP_WIZARD", uid) == 0;
    }
}
