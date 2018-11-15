package com.android.server.timezone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.util.Slog;
import java.util.List;

final class PackageTrackerHelperImpl implements ConfigHelper, PackageManagerHelper {
    private static final String TAG = "PackageTrackerHelperImpl";
    private final Context mContext;
    private final PackageManager mPackageManager;

    PackageTrackerHelperImpl(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }

    public boolean isTrackingEnabled() {
        return this.mContext.getResources().getBoolean(17957051);
    }

    public String getUpdateAppPackageName() {
        return this.mContext.getResources().getString(17039841);
    }

    public String getDataAppPackageName() {
        return this.mContext.getResources().getString(17039840);
    }

    public int getCheckTimeAllowedMillis() {
        return this.mContext.getResources().getInteger(17694872);
    }

    public int getFailedCheckRetryCount() {
        return this.mContext.getResources().getInteger(17694871);
    }

    public long getInstalledPackageVersion(String packageName) throws NameNotFoundException {
        return this.mPackageManager.getPackageInfo(packageName, 32768).getLongVersionCode();
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
        ProviderInfo providerInfo = this.mPackageManager.resolveContentProviderAsUser(authority, 32768, UserHandle.SYSTEM.getIdentifier());
        if (providerInfo == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("contentProviderRegistered: No content provider registered with authority=");
            stringBuilder.append(authority);
            Slog.i(str, stringBuilder.toString());
            return false;
        } else if (requiredPackageName.equals(providerInfo.applicationInfo.packageName)) {
            return true;
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("contentProviderRegistered: App with packageName=");
            stringBuilder2.append(requiredPackageName);
            stringBuilder2.append(" does not expose the a content provider with authority=");
            stringBuilder2.append(authority);
            Slog.i(str2, stringBuilder2.toString());
            return false;
        }
    }

    public boolean receiverRegistered(Intent intent, String requiredPermissionName) throws NameNotFoundException {
        List<ResolveInfo> resolveInfo = this.mPackageManager.queryBroadcastReceiversAsUser(intent, 32768, UserHandle.SYSTEM);
        if (resolveInfo.size() != 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("receiverRegistered: Zero or multiple broadcast receiver registered for intent=");
            stringBuilder.append(intent);
            stringBuilder.append(", found=");
            stringBuilder.append(resolveInfo);
            Slog.i(str, stringBuilder.toString());
            return false;
        }
        boolean requiresPermission = requiredPermissionName.equals(((ResolveInfo) resolveInfo.get(0)).activityInfo.permission);
        if (!requiresPermission) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("receiverRegistered: Broadcast receiver registered for intent=");
            stringBuilder2.append(intent);
            stringBuilder2.append(" must require permission ");
            stringBuilder2.append(requiredPermissionName);
            Slog.i(str2, stringBuilder2.toString());
        }
        return requiresPermission;
    }
}
