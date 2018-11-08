package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import android.util.ArrayMap;

public interface IHwPackageManagerInner {
    ArrayMap<String, Package> getPackagesLock();

    Settings getSettings();
}
