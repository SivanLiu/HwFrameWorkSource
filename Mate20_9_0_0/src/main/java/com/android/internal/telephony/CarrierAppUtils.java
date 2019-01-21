package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.SystemConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class CarrierAppUtils {
    private static final boolean DEBUG = false;
    private static final String TAG = "CarrierAppUtils";

    private CarrierAppUtils() {
    }

    public static synchronized void disableCarrierAppsUntilPrivileged(String callingPackage, IPackageManager packageManager, TelephonyManager telephonyManager, ContentResolver contentResolver, int userId) {
        synchronized (CarrierAppUtils.class) {
            SystemConfig config = SystemConfig.getInstance();
            disableCarrierAppsUntilPrivileged(callingPackage, packageManager, telephonyManager, contentResolver, userId, config.getDisabledUntilUsedPreinstalledCarrierApps(), config.getDisabledUntilUsedPreinstalledCarrierAssociatedApps());
        }
    }

    public static synchronized void disableCarrierAppsUntilPrivileged(String callingPackage, IPackageManager packageManager, ContentResolver contentResolver, int userId) {
        synchronized (CarrierAppUtils.class) {
            SystemConfig config = SystemConfig.getInstance();
            disableCarrierAppsUntilPrivileged(callingPackage, packageManager, null, contentResolver, userId, config.getDisabledUntilUsedPreinstalledCarrierApps(), config.getDisabledUntilUsedPreinstalledCarrierAssociatedApps());
        }
    }

    @VisibleForTesting
    public static void disableCarrierAppsUntilPrivileged(String callingPackage, IPackageManager packageManager, TelephonyManager telephonyManager, ContentResolver contentResolver, int userId, ArraySet<String> systemCarrierAppsDisabledUntilUsed, ArrayMap<String, List<String>> systemCarrierAssociatedAppsDisabledUntilUsed) {
        IPackageManager iPackageManager = packageManager;
        TelephonyManager telephonyManager2 = telephonyManager;
        ContentResolver contentResolver2 = contentResolver;
        int i = userId;
        List<ApplicationInfo> candidates = getDefaultCarrierAppCandidatesHelper(iPackageManager, i, systemCarrierAppsDisabledUntilUsed);
        if (candidates == null || candidates.isEmpty()) {
            ArrayMap<String, List<String>> arrayMap = systemCarrierAssociatedAppsDisabledUntilUsed;
            return;
        }
        Map<String, List<ApplicationInfo>> associatedApps = getDefaultCarrierAssociatedAppsHelper(iPackageManager, i, systemCarrierAssociatedAppsDisabledUntilUsed);
        ArrayList enabledCarrierPackages = new ArrayList();
        boolean z = false;
        boolean z2 = true;
        boolean hasRunOnce = Secure.getIntForUser(contentResolver2, "carrier_apps_handled", 0, i) == 1;
        try {
            String packageName = candidates.iterator();
            while (packageName.hasNext()) {
                String str;
                boolean z3;
                ApplicationInfo ai = (ApplicationInfo) packageName.next();
                String packageName2 = ai.packageName;
                boolean z4 = (telephonyManager2 == null || telephonyManager2.checkCarrierPrivilegesForPackageAnyPhone(packageName2) != z2) ? z : z2;
                String str2;
                StringBuilder stringBuilder;
                List<ApplicationInfo> associatedAppList;
                Iterator it;
                if (z4) {
                    ApplicationInfo ai2;
                    if (ai.isUpdatedSystemApp()) {
                        str = packageName;
                        packageName = packageName2;
                        ai2 = ai;
                        z3 = z;
                    } else {
                        if (ai.enabledSetting != 0) {
                            if (ai.enabledSetting != 4) {
                                str = packageName;
                                packageName = packageName2;
                                ai2 = ai;
                                z3 = z;
                            }
                        }
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Update state(");
                        stringBuilder.append(packageName2);
                        stringBuilder.append("): ENABLED for user ");
                        stringBuilder.append(i);
                        Slog.i(str2, stringBuilder.toString());
                        str = packageName;
                        packageName = packageName2;
                        ai2 = ai;
                        z3 = z;
                        iPackageManager.setApplicationEnabledSetting(packageName2, 1, 1, i, callingPackage);
                    }
                    associatedAppList = (List) associatedApps.get(packageName);
                    if (associatedAppList != null) {
                        it = associatedAppList.iterator();
                        while (it.hasNext()) {
                            Iterator it2;
                            List<ApplicationInfo> associatedAppList2;
                            ai = (ApplicationInfo) it.next();
                            if (ai.enabledSetting != 0) {
                                if (ai.enabledSetting != 4) {
                                    int i2 = 4;
                                    it2 = it;
                                    associatedAppList2 = associatedAppList;
                                    it = it2;
                                    associatedAppList = associatedAppList2;
                                }
                            }
                            str2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Update associated state(");
                            stringBuilder.append(ai.packageName);
                            stringBuilder.append("): ENABLED for user ");
                            stringBuilder.append(i);
                            Slog.i(str2, stringBuilder.toString());
                            it2 = it;
                            associatedAppList2 = associatedAppList;
                            iPackageManager.setApplicationEnabledSetting(ai.packageName, 1, 1, i, callingPackage);
                            it = it2;
                            associatedAppList = associatedAppList2;
                        }
                    }
                    enabledCarrierPackages.add(ai2.packageName);
                } else {
                    str = packageName;
                    packageName = packageName2;
                    z3 = z;
                    ApplicationInfo ai3 = ai;
                    if (ai3.isUpdatedSystemApp() || ai3.enabledSetting != 0) {
                    } else {
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Update state(");
                        stringBuilder.append(packageName);
                        stringBuilder.append("): DISABLED_UNTIL_USED for user ");
                        stringBuilder.append(i);
                        Slog.i(str2, stringBuilder.toString());
                        iPackageManager.setApplicationEnabledSetting(packageName, 4, 0, i, callingPackage);
                    }
                    if (!hasRunOnce) {
                        associatedAppList = (List) associatedApps.get(packageName);
                        if (associatedAppList != null) {
                            it = associatedAppList.iterator();
                            while (it.hasNext()) {
                                Iterator it3;
                                List<ApplicationInfo> associatedAppList3;
                                ai = (ApplicationInfo) it.next();
                                if (ai.enabledSetting == 0) {
                                    str2 = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Update associated state(");
                                    stringBuilder.append(ai.packageName);
                                    stringBuilder.append("): DISABLED_UNTIL_USED for user ");
                                    stringBuilder.append(i);
                                    Slog.i(str2, stringBuilder.toString());
                                    ApplicationInfo applicationInfo = ai;
                                    it3 = it;
                                    associatedAppList3 = associatedAppList;
                                    iPackageManager.setApplicationEnabledSetting(ai.packageName, 4, 0, i, callingPackage);
                                } else {
                                    it3 = it;
                                    associatedAppList3 = associatedAppList;
                                }
                                it = it3;
                                associatedAppList = associatedAppList3;
                            }
                        }
                    }
                }
                z = z3;
                packageName = str;
                z2 = true;
            }
            if (!hasRunOnce) {
                Secure.putIntForUser(contentResolver2, "carrier_apps_handled", 1, i);
            }
            if (!enabledCarrierPackages.isEmpty()) {
                String[] packageNames = new String[enabledCarrierPackages.size()];
                enabledCarrierPackages.toArray(packageNames);
                iPackageManager.grantDefaultPermissionsToEnabledCarrierApps(packageNames, i);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not reach PackageManager", e);
        }
    }

    public static List<ApplicationInfo> getDefaultCarrierApps(IPackageManager packageManager, TelephonyManager telephonyManager, int userId) {
        List<ApplicationInfo> candidates = getDefaultCarrierAppCandidates(packageManager, userId);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (int i = candidates.size() - 1; i >= 0; i--) {
            if (!(telephonyManager.checkCarrierPrivilegesForPackageAnyPhone(((ApplicationInfo) candidates.get(i)).packageName) == 1)) {
                candidates.remove(i);
            }
        }
        return candidates;
    }

    public static List<ApplicationInfo> getDefaultCarrierAppCandidates(IPackageManager packageManager, int userId) {
        return getDefaultCarrierAppCandidatesHelper(packageManager, userId, SystemConfig.getInstance().getDisabledUntilUsedPreinstalledCarrierApps());
    }

    private static List<ApplicationInfo> getDefaultCarrierAppCandidatesHelper(IPackageManager packageManager, int userId, ArraySet<String> systemCarrierAppsDisabledUntilUsed) {
        if (systemCarrierAppsDisabledUntilUsed == null) {
            return null;
        }
        int size = systemCarrierAppsDisabledUntilUsed.size();
        if (size == 0) {
            return null;
        }
        List<ApplicationInfo> apps = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            ApplicationInfo ai = getApplicationInfoIfSystemApp(packageManager, userId, (String) systemCarrierAppsDisabledUntilUsed.valueAt(i));
            if (ai != null) {
                apps.add(ai);
            }
        }
        return apps;
    }

    private static Map<String, List<ApplicationInfo>> getDefaultCarrierAssociatedAppsHelper(IPackageManager packageManager, int userId, ArrayMap<String, List<String>> systemCarrierAssociatedAppsDisabledUntilUsed) {
        int size = systemCarrierAssociatedAppsDisabledUntilUsed.size();
        Map<String, List<ApplicationInfo>> associatedApps = new ArrayMap(size);
        for (int i = 0; i < size; i++) {
            String carrierAppPackage = (String) systemCarrierAssociatedAppsDisabledUntilUsed.keyAt(i);
            List<String> associatedAppPackages = (List) systemCarrierAssociatedAppsDisabledUntilUsed.valueAt(i);
            for (int j = 0; j < associatedAppPackages.size(); j++) {
                ApplicationInfo ai = getApplicationInfoIfSystemApp(packageManager, userId, (String) associatedAppPackages.get(j));
                if (!(ai == null || ai.isUpdatedSystemApp())) {
                    List<ApplicationInfo> appList = (List) associatedApps.get(carrierAppPackage);
                    if (appList == null) {
                        appList = new ArrayList();
                        associatedApps.put(carrierAppPackage, appList);
                    }
                    appList.add(ai);
                }
            }
        }
        return associatedApps;
    }

    private static ApplicationInfo getApplicationInfoIfSystemApp(IPackageManager packageManager, int userId, String packageName) {
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(packageName, 32768, userId);
            if (ai != null && ai.isSystemApp()) {
                return ai;
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not reach PackageManager", e);
        }
        return null;
    }
}
