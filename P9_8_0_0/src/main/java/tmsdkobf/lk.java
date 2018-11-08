package tmsdkobf;

import android.net.Uri;
import android.os.Build.VERSION;
import tmsdk.common.TMSDKContext;

public class lk {
    public static final Uri yx = Uri.parse("content://sms");
    public static final Uri yy = Uri.parse("content://mms");

    public static int a(boolean z, String str, String str2) {
        if (!z) {
            return 0;
        }
        if (VERSION.SDK_INT < 19) {
            return 0;
        }
        Object -l_4_R = TMSDKContext.getApplicaionContext();
        try {
            int -l_3_I = -l_4_R.getPackageManager().getApplicationInfo(str, 1).uid;
            try {
                Object -l_5_R = -l_4_R.getSystemService("appops");
                Object -l_6_R = Class.forName(-l_5_R.getClass().getName());
                Object -l_7_R = -l_6_R.getDeclaredField("mService");
                -l_7_R.setAccessible(true);
                -l_6_R.getDeclaredField(str2).setAccessible(true);
                -l_6_R.getDeclaredField("MODE_ALLOWED").setAccessible(true);
                Object -l_10_R = -l_7_R.get(-l_5_R);
                Object -l_12_R = Class.forName(-l_10_R.getClass().getName()).getDeclaredMethod("setMode", new Class[]{Integer.TYPE, Integer.TYPE, String.class, Integer.TYPE});
                -l_12_R.setAccessible(true);
                -l_12_R.invoke(-l_10_R, new Object[]{Integer.valueOf(-l_8_R.getInt(null)), Integer.valueOf(-l_3_I), str, Integer.valueOf(-l_9_R.getInt(null))});
                return 1;
            } catch (Throwable th) {
                return 2;
            }
        } catch (Throwable th2) {
            return 2;
        }
    }
}
