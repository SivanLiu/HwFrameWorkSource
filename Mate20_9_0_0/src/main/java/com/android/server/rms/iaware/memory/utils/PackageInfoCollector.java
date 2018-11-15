package com.android.server.rms.iaware.memory.utils;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.rms.iaware.AwareLog;
import android.util.ArraySet;
import java.io.File;
import java.util.Iterator;

public class PackageInfoCollector {
    private static final String TAG = "AwareMem_PkgInfo";

    public static ArraySet<String> getLibFilesFromPackage(Context mContext, ArraySet<String> pkgSet) {
        if (mContext == null || pkgSet == null) {
            return null;
        }
        ArraySet<String> libFileSet = new ArraySet();
        Iterator it = pkgSet.iterator();
        while (it.hasNext()) {
            String pkg = (String) it.next();
            try {
                Context context = mContext.createPackageContext(pkg, 1152);
                String sourceDir = context.getApplicationInfo().sourceDir;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pkg: ");
                stringBuilder.append(pkg);
                stringBuilder.append(", sourceDir: ");
                stringBuilder.append(sourceDir);
                AwareLog.i(str, stringBuilder.toString());
                if (sourceDir != null) {
                    if (sourceDir.lastIndexOf(File.separator) <= 0) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("source dir name error, sourceDir.lastIndexOf(File.separator)=");
                        stringBuilder.append(sourceDir.lastIndexOf(File.separator));
                        AwareLog.w(str, stringBuilder.toString());
                    } else {
                        sourceDir = sourceDir.substring(0, sourceDir.lastIndexOf(File.separator));
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("sourceDir: ");
                        stringBuilder.append(sourceDir);
                        AwareLog.i(str, stringBuilder.toString());
                        libFileSet.add(sourceDir);
                    }
                }
                str = context.getApplicationInfo().dataDir;
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("pkg: ");
                stringBuilder2.append(pkg);
                stringBuilder2.append(", dataDir: ");
                stringBuilder2.append(str);
                AwareLog.i(str2, stringBuilder2.toString());
                if (str != null) {
                    libFileSet.add(str);
                }
            } catch (NameNotFoundException e) {
                AwareLog.w(TAG, "Unable to create context for heavy notification");
            }
        }
        return libFileSet;
    }
}
