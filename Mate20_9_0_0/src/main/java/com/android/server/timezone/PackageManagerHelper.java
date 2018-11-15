package com.android.server.timezone;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;

interface PackageManagerHelper {
    boolean contentProviderRegistered(String str, String str2);

    long getInstalledPackageVersion(String str) throws NameNotFoundException;

    boolean isPrivilegedApp(String str) throws NameNotFoundException;

    boolean receiverRegistered(Intent intent, String str) throws NameNotFoundException;

    boolean usesPermission(String str, String str2) throws NameNotFoundException;
}
