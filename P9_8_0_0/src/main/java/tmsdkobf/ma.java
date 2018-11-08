package tmsdkobf;

import android.content.Context;
import java.io.File;
import tmsdk.common.TMSDKContext;

public class ma {
    public static boolean f(Context context, String -l_2_R) {
        int -l_4_I;
        Object -l_3_R = TMSDKContext.getStrFromEnvMap(TMSDKContext.PRE_LIB_PATH);
        if (-l_3_R == null) {
            try {
                -l_3_R = context.getCacheDir().toString();
                if (-l_3_R.endsWith("/")) {
                    -l_3_R = -l_3_R.substring(0, -l_3_R.length() - 2);
                }
                -l_4_I = -l_3_R.lastIndexOf(47);
                if (-l_4_I == -1) {
                    -l_3_R = "";
                } else {
                    -l_3_R = -l_3_R.substring(0, -l_4_I) + "/lib/";
                }
            } catch (Exception e) {
                -l_3_R = "/data/data/" + context.getPackageName() + "/lib/";
            }
        }
        if (-l_3_R.length() == 0) {
            try {
                System.loadLibrary(-l_2_R);
            } catch (Object -l_4_R) {
                -l_4_R.printStackTrace();
                return false;
            }
        }
        String str;
        -l_4_I = 0;
        if (new File(-l_3_R + -l_2_R).exists()) {
            try {
                System.load(-l_3_R + -l_2_R);
                -l_4_I = 1;
            } catch (UnsatisfiedLinkError e2) {
            }
        }
        if (-l_4_I == 0 && !-l_2_R.endsWith(".so")) {
            str = -l_2_R + ".so";
            if (new File(-l_3_R + str).exists()) {
                try {
                    System.load(-l_3_R + str);
                    -l_4_I = 1;
                } catch (UnsatisfiedLinkError e3) {
                }
            }
        } else {
            str = -l_2_R;
        }
        if (-l_4_I == 0 && !str.startsWith("lib")) {
            str = "lib" + str;
            Object -l_5_R = new File(-l_3_R + str);
            if (!-l_5_R.exists()) {
                -l_3_R = -l_3_R.replace("/lib/", "/app_p_lib/");
                -l_5_R = new File(-l_3_R + str);
            }
            if (-l_5_R.exists()) {
                try {
                    System.load(-l_3_R + str);
                    -l_4_I = 1;
                } catch (UnsatisfiedLinkError e4) {
                }
            }
        }
        if (-l_4_I == 0) {
            try {
                System.loadLibrary(-l_2_R);
            } catch (Object -l_6_R) {
                -l_6_R.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
