package com.android.server.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class PermissionMonitor {
    private static final boolean DBG = true;
    private static final Boolean NETWORK = Boolean.FALSE;
    private static final Boolean SYSTEM = Boolean.TRUE;
    private static final String TAG = "PermissionMonitor";
    private final Map<Integer, Boolean> mApps = new HashMap();
    private final Context mContext;
    private final BroadcastReceiver mIntentReceiver;
    private final INetworkManagementService mNetd;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private final Set<Integer> mUsers = new HashSet();

    public PermissionMonitor(Context context, INetworkManagementService netd) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mUserManager = UserManager.get(context);
        this.mNetd = netd;
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int user = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                int appUid = intent.getIntExtra("android.intent.extra.UID", -1);
                Uri appData = intent.getData();
                String appName = appData != null ? appData.getSchemeSpecificPart() : null;
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    PermissionMonitor.this.onUserAdded(user);
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    PermissionMonitor.this.onUserRemoved(user);
                } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    PermissionMonitor.this.onAppAdded(appName, appUid);
                } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                    PermissionMonitor.this.onAppRemoved(appUid);
                }
            }
        };
    }

    public synchronized void startMonitoring() {
        log("Monitoring");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter, null, null);
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter, null, null);
        List<PackageInfo> apps = this.mPackageManager.getInstalledPackages(4096);
        if (apps == null) {
            loge("No apps");
            return;
        }
        for (PackageInfo app : apps) {
            int uid = app.applicationInfo != null ? app.applicationInfo.uid : -1;
            if (uid >= 0) {
                boolean isNetwork = hasNetworkPermission(app);
                boolean hasRestrictedPermission = hasRestrictedNetworkPermission(app);
                if (isNetwork || hasRestrictedPermission) {
                    Boolean permission = (Boolean) this.mApps.get(Integer.valueOf(uid));
                    if (permission == null || permission == NETWORK) {
                        this.mApps.put(Integer.valueOf(uid), Boolean.valueOf(hasRestrictedPermission));
                    }
                }
            }
        }
        List<UserInfo> users = this.mUserManager.getUsers(true);
        if (users != null) {
            for (UserInfo user : users) {
                this.mUsers.add(Integer.valueOf(user.id));
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Users: ");
        stringBuilder.append(this.mUsers.size());
        stringBuilder.append(", Apps: ");
        stringBuilder.append(this.mApps.size());
        log(stringBuilder.toString());
        update(this.mUsers, this.mApps, true);
    }

    @VisibleForTesting
    boolean isPreinstalledSystemApp(PackageInfo app) {
        if (((app.applicationInfo != null ? app.applicationInfo.flags : 0) & NetworkConstants.ICMPV6_ECHO_REPLY_TYPE) != 0) {
            return true;
        }
        return false;
    }

    @VisibleForTesting
    boolean hasPermission(PackageInfo app, String permission) {
        if (app.requestedPermissions != null) {
            for (String p : app.requestedPermissions) {
                if (permission.equals(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasNetworkPermission(PackageInfo app) {
        return hasPermission(app, "android.permission.CHANGE_NETWORK_STATE");
    }

    private boolean hasRestrictedNetworkPermission(PackageInfo app) {
        boolean z = true;
        if (isPreinstalledSystemApp(app)) {
            return true;
        }
        if (!(hasPermission(app, "android.permission.CONNECTIVITY_INTERNAL") || hasPermission(app, "android.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS"))) {
            z = false;
        }
        return z;
    }

    private boolean hasUseBackgroundNetworksPermission(PackageInfo app) {
        return hasPermission(app, "android.permission.CHANGE_NETWORK_STATE") || hasPermission(app, "android.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS") || hasPermission(app, "android.permission.CONNECTIVITY_INTERNAL") || hasPermission(app, "android.permission.NETWORK_STACK") || isPreinstalledSystemApp(app);
    }

    public boolean hasUseBackgroundNetworksPermission(int uid) {
        String[] names = this.mPackageManager.getPackagesForUid(uid);
        if (names == null || names.length == 0) {
            return false;
        }
        try {
            return hasUseBackgroundNetworksPermission(this.mPackageManager.getPackageInfoAsUser(names[0], 4096, UserHandle.getUserId(uid)));
        } catch (NameNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NameNotFoundException ");
            stringBuilder.append(names[0]);
            loge(stringBuilder.toString(), e);
            return false;
        }
    }

    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = ((Integer) list.get(i)).intValue();
        }
        return array;
    }

    private void update(Set<Integer> users, Map<Integer, Boolean> apps, boolean add) {
        List<Integer> network = new ArrayList();
        List<Integer> system = new ArrayList();
        for (Entry<Integer, Boolean> app : apps.entrySet()) {
            List<Integer> list = ((Boolean) app.getValue()).booleanValue() ? system : network;
            for (Integer user : users) {
                list.add(Integer.valueOf(UserHandle.getUid(user.intValue(), ((Integer) app.getKey()).intValue())));
            }
        }
        if (add) {
            try {
                this.mNetd.setPermission("NETWORK", toIntArray(network));
                this.mNetd.setPermission("SYSTEM", toIntArray(system));
                return;
            } catch (RemoteException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception when updating permissions: ");
                stringBuilder.append(e);
                loge(stringBuilder.toString());
                return;
            }
        }
        this.mNetd.clearPermission(toIntArray(network));
        this.mNetd.clearPermission(toIntArray(system));
    }

    private synchronized void onUserAdded(int user) {
        if (user < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid user in onUserAdded: ");
            stringBuilder.append(user);
            loge(stringBuilder.toString());
            return;
        }
        this.mUsers.add(Integer.valueOf(user));
        Set<Integer> users = new HashSet();
        users.add(Integer.valueOf(user));
        update(users, this.mApps, true);
    }

    private synchronized void onUserRemoved(int user) {
        if (user < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid user in onUserRemoved: ");
            stringBuilder.append(user);
            loge(stringBuilder.toString());
            return;
        }
        this.mUsers.remove(Integer.valueOf(user));
        Set<Integer> users = new HashSet();
        users.add(Integer.valueOf(user));
        update(users, this.mApps, false);
    }

    private Boolean highestPermissionForUid(Boolean currentPermission, String name) {
        if (currentPermission == SYSTEM) {
            return currentPermission;
        }
        try {
            PackageInfo app = this.mPackageManager.getPackageInfo(name, 4096);
            boolean isNetwork = hasNetworkPermission(app);
            boolean hasRestrictedPermission = hasRestrictedNetworkPermission(app);
            if (isNetwork || hasRestrictedPermission) {
                currentPermission = Boolean.valueOf(hasRestrictedPermission);
            }
        } catch (NameNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NameNotFoundException ");
            stringBuilder.append(name);
            loge(stringBuilder.toString());
        }
        return currentPermission;
    }

    /* JADX WARNING: Missing block: B:9:0x0042, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void onAppAdded(String appName, int appUid) {
        if (TextUtils.isEmpty(appName) || appUid < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid app in onAppAdded: ");
            stringBuilder.append(appName);
            stringBuilder.append(" | ");
            stringBuilder.append(appUid);
            loge(stringBuilder.toString());
            return;
        }
        Boolean permission = highestPermissionForUid((Boolean) this.mApps.get(Integer.valueOf(appUid)), appName);
        if (permission != this.mApps.get(Integer.valueOf(appUid))) {
            this.mApps.put(Integer.valueOf(appUid), permission);
            Map<Integer, Boolean> apps = new HashMap();
            apps.put(Integer.valueOf(appUid), permission);
            update(this.mUsers, apps, true);
        }
    }

    /* JADX WARNING: Missing block: B:30:0x0082, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void onAppRemoved(int appUid) {
        if (appUid < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid app in onAppRemoved: ");
            stringBuilder.append(appUid);
            loge(stringBuilder.toString());
            return;
        }
        Map<Integer, Boolean> apps = new HashMap();
        Boolean permission = null;
        String[] packages = this.mPackageManager.getPackagesForUid(appUid);
        if (packages != null && packages.length > 0) {
            int length = packages.length;
            Boolean permission2 = null;
            int permission3 = 0;
            while (permission3 < length) {
                permission2 = highestPermissionForUid(permission2, packages[permission3]);
                if (permission2 != SYSTEM) {
                    permission3++;
                } else {
                    return;
                }
            }
            permission = permission2;
        }
        if (permission != this.mApps.get(Integer.valueOf(appUid))) {
            if (permission != null) {
                this.mApps.put(Integer.valueOf(appUid), permission);
                apps.put(Integer.valueOf(appUid), permission);
                update(this.mUsers, apps, true);
            } else {
                this.mApps.remove(Integer.valueOf(appUid));
                apps.put(Integer.valueOf(appUid), NETWORK);
                update(this.mUsers, apps, false);
            }
        }
    }

    private static void log(String s) {
        Log.d(TAG, s);
    }

    private static void loge(String s) {
        Log.e(TAG, s);
    }

    private static void loge(String s, Throwable e) {
        Log.e(TAG, s, e);
    }
}
