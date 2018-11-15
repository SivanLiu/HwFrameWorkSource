package com.android.server.mtm.iaware.appmng.appstart.comm;

import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.content.pm.ApplicationInfo;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class AppStartupUtil {
    private static final Pattern CTS_PATTERN = Pattern.compile(".*android.*cts.*");
    private static final String TAG = "AppStartupUtil";
    private static Set<String> mCtsFilterPkgs = new HashSet();
    private static Set<String> mCtsSpecPkgs = new HashSet();
    private static AtomicBoolean mIsCtsInitialized = new AtomicBoolean(false);

    public static void initCtsPkgList() {
        if (!mIsCtsInitialized.get()) {
            Set<String> ctsSpecPkgs;
            mIsCtsInitialized.set(true);
            ArrayList<String> pkgList = DecisionMaker.getInstance().getRawConfig(AppMngFeature.APP_START.getDesc(), "ctspkglist");
            if (pkgList != null) {
                ctsSpecPkgs = new HashSet();
                ctsSpecPkgs.addAll(pkgList);
                mCtsSpecPkgs = ctsSpecPkgs;
            }
            pkgList = DecisionMaker.getInstance().getRawConfig(AppMngFeature.APP_START.getDesc(), "ctsfilterpkglist");
            if (pkgList != null) {
                ctsSpecPkgs = new HashSet();
                ctsSpecPkgs.addAll(pkgList);
                mCtsFilterPkgs = ctsSpecPkgs;
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
        if (pkgName == null || ((!CTS_PATTERN.matcher(pkgName).matches() || mCtsFilterPkgs.contains(pkgName)) && ((!pkgName.startsWith("com.google.android") || !pkgName.contains(".gts")) && !mCtsSpecPkgs.contains(pkgName)))) {
            return false;
        }
        return true;
    }

    public static String getDumpCtsPackages() {
        StringBuilder stringBuilder;
        StringBuilder cacheStr = new StringBuilder();
        Set<String> ctsSpecPkgs = mCtsSpecPkgs;
        Set<String> ctsFilterPkgs = mCtsFilterPkgs;
        cacheStr.append("ctsSpecPkgs:\n");
        for (String pkg : ctsSpecPkgs) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  ");
            stringBuilder.append(pkg);
            stringBuilder.append("\n");
            cacheStr.append(stringBuilder.toString());
        }
        cacheStr.append("ctsFilterPkgs:\n");
        for (String pkg2 : ctsFilterPkgs) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  ");
            stringBuilder.append(pkg2);
            stringBuilder.append("\n");
            cacheStr.append(stringBuilder.toString());
        }
        return cacheStr.toString();
    }
}
