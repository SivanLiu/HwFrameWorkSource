package tmsdk.common.utils;

import android.os.Build.VERSION;
import android.util.DisplayMetrics;
import java.io.File;
import tmsdkobf.mh;

public class k {
    public static Object a(Object obj, File file, String str, DisplayMetrics displayMetrics, int i) {
        try {
            if (VERSION.SDK_INT < 21) {
                return mh.a(obj, "parsePackage", new Object[]{file, str, displayMetrics, Integer.valueOf(i)});
            }
            return mh.a(obj, "parsePackage", new Object[]{file, Integer.valueOf(i)});
        } catch (Object -l_6_R) {
            f.b("--PackageUtil--", -l_6_R.getMessage(), -l_6_R);
            return null;
        }
    }

    public static Object bW(String str) {
        try {
            if (VERSION.SDK_INT >= 21) {
                return mh.a("android.content.pm.PackageParser", null);
            }
            return mh.a("android.content.pm.PackageParser", new Object[]{str});
        } catch (Object -l_2_R) {
            f.b("--PackageUtil--", -l_2_R.getMessage(), -l_2_R);
            return null;
        }
    }
}
