package com.android.server.mtm.iaware.appmng.appstart.comm;

import android.app.mtm.iaware.appmng.AppMngConstant;
import android.content.pm.ApplicationInfo;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class AppStartupUtil {
    private static final String PREFIX = "com.google.android";
    private static final String TAG = "AppStartupUtil";
    private static Set<String> sCtsSpecPkgs = new HashSet();
    private static AtomicBoolean sIsCtsInitialized = new AtomicBoolean(false);

    public static void initCtsPkgList() {
        if (!sIsCtsInitialized.get()) {
            sIsCtsInitialized.set(true);
            Collection<? extends String> pkgList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.APP_START.getDesc(), "ctspkglist");
            if (pkgList != null) {
                Set<String> ctsSpecPkgs = new HashSet<>();
                ctsSpecPkgs.addAll(pkgList);
                sCtsSpecPkgs = ctsSpecPkgs;
            }
        }
    }

    public static boolean isSystemUnRemovablePkg(ApplicationInfo applicationInfo) {
        return (applicationInfo.flags & 1) != 0 && (applicationInfo.hwFlags & 100663296) == 0;
    }

    public static boolean isAppStopped(ApplicationInfo applicationInfo) {
        return (applicationInfo.flags & HighBitsCompModeID.MODE_EYE_PROTECT) != 0;
    }

    public static boolean isCtsPackage(String pkgName) {
        int androidIndex;
        if (pkgName == null) {
            return false;
        }
        int ctsIndex = pkgName.indexOf(".cts");
        if (ctsIndex > 0 && (androidIndex = pkgName.indexOf(AppStartupDataMgr.HWPUSH_PKGNAME)) > -1 && ctsIndex > androidIndex) {
            return true;
        }
        if (pkgName.indexOf(".gts", PREFIX.length()) <= -1 || !pkgName.startsWith(PREFIX)) {
            return sCtsSpecPkgs.contains(pkgName);
        }
        return true;
    }

    public static String getDumpCtsPackages() {
        StringBuilder cacheStr = new StringBuilder();
        Set<String> ctsSpecPkgs = sCtsSpecPkgs;
        cacheStr.append("ctsSpecPkgs:\n");
        for (String pkg : ctsSpecPkgs) {
            cacheStr.append("  ");
            cacheStr.append(pkg);
            cacheStr.append("\n");
        }
        return cacheStr.toString();
    }
}
