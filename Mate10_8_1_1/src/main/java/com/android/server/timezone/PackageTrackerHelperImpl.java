package com.android.server.timezone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.util.Slog;
import java.util.List;

final class PackageTrackerHelperImpl implements ClockHelper, ConfigHelper, PackageManagerHelper {
    private static final String TAG = "PackageTrackerHelperImpl";
    private final Context mContext;
    private final PackageManager mPackageManager;

    PackageTrackerHelperImpl(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }

    public boolean isTrackingEnabled() {
        return this.mContext.getResources().getBoolean(17957044);
    }

    public String getUpdateAppPackageName() {
        return this.mContext.getResources().getString(17039817);
    }

    public String getDataAppPackageName() {
        return this.mContext.getResources().getString(17039816);
    }

    public int getCheckTimeAllowedMillis() {
        return this.mContext.getResources().getInteger(17694861);
    }

    public int getFailedCheckRetryCount() {
        return this.mContext.getResources().getInteger(17694860);
    }

    public long currentTimestamp() {
        return SystemClock.elapsedRealtime();
    }

    public int getInstalledPackageVersion(String packageName) throws NameNotFoundException {
        return this.mPackageManager.getPackageInfo(packageName, 32768).versionCode;
    }

    public boolean isPrivilegedApp(String packageName) throws NameNotFoundException {
        return this.mPackageManager.getPackageInfo(packageName, 32768).applicationInfo.isPrivilegedApp();
    }

    public boolean usesPermission(String packageName, String requiredPermissionName) throws NameNotFoundException {
        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 36864);
        if (packageInfo.requestedPermissions == null) {
            return false;
        }
        for (String requestedPermission : packageInfo.requestedPermissions) {
            if (requiredPermissionName.equals(requestedPermission)) {
                return true;
            }
        }
        return false;
    }

    public boolean contentProviderRegistered(String authority, String requiredPackageName) {
        ProviderInfo providerInfo = this.mPackageManager.resolveContentProvider(authority, 32768);
        if (providerInfo == null) {
            Slog.i(TAG, "contentProviderRegistered: No content provider registered with authority=" + authority);
            return false;
        } else if (requiredPackageName.equals(providerInfo.applicationInfo.packageName)) {
            return true;
        } else {
            Slog.i(TAG, "contentProviderRegistered: App with packageName=" + requiredPackageName + " does not expose the a content provider with authority=" + authority);
            return false;
        }
    }

    public boolean receiverRegistered(Intent intent, String requiredPermissionName) throws NameNotFoundException {
        List<ResolveInfo> resolveInfo = this.mPackageManager.queryBroadcastReceivers(intent, 32768);
        if (resolveInfo.size() != 1) {
            Slog.i(TAG, "receiverRegistered: Zero or multiple broadcast receiver registered for intent=" + intent + ", found=" + resolveInfo);
            return false;
        }
        boolean requiresPermission = requiredPermissionName.equals(((ResolveInfo) resolveInfo.get(0)).activityInfo.permission);
        if (!requiresPermission) {
            Slog.i(TAG, "receiverRegistered: Broadcast receiver registered for intent=" + intent + " must require permission " + requiredPermissionName);
        }
        return requiresPermission;
    }
}
