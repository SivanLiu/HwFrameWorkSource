package tmsdkobf;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import tmsdk.common.TMSDKContext;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.utils.f;

public class mk {
    public static <T> T a(Context context, String str, String str2, T t) {
        return a(context, str, str2, t, null);
    }

    public static <T> T a(Context context, String str, String str2, T -l_5_R, String str3) {
        if (str == null || str2 == null) {
            return -l_5_R;
        }
        Object -l_5_R2;
        try {
            -l_5_R2 = d(context, str, str3).a(str2, (Object) -l_5_R);
        } catch (Object -l_7_R) {
            -l_7_R.printStackTrace();
            f.e("deepclean", "head error::" + -l_7_R.toString());
            T -l_5_R3 = -l_5_R;
        }
        if (-l_5_R2 == null) {
            -l_5_R3 = -l_5_R;
        }
        return -l_5_R2;
    }

    public static <T> T b(Context context, String str, String str2, T -l_5_R, String str3) {
        if (str == null || str2 == null) {
            return -l_5_R;
        }
        Object -l_5_R2;
        try {
            -l_5_R2 = c(context, str, str3).a(str2, (Object) -l_5_R);
        } catch (Object -l_7_R) {
            -l_7_R.printStackTrace();
            T -l_5_R3 = -l_5_R;
        }
        if (-l_5_R2 == null) {
            -l_5_R3 = -l_5_R;
        }
        return -l_5_R2;
    }

    private static fn c(Context -l_4_R, String str, String str2) {
        Object -l_5_R;
        Object -l_3_R = new fn();
        Context -l_4_R2 = TMSDKContext.getCurrentContext();
        if (-l_4_R2 != null) {
            -l_4_R = -l_4_R2;
        }
        try {
            -l_5_R = -l_4_R.getResources().getAssets().open(str, 1);
            Object -l_6_R = lt.c(-l_5_R);
            Object -l_7_R = new byte[-l_5_R.available()];
            -l_5_R.read(-l_7_R);
            if (!lq.bytesToHexString(mc.l(-l_7_R)).equals(lq.bytesToHexString(-l_6_R.yU))) {
                return -l_3_R;
            }
            Object -l_8_R = TccCryptor.decrypt(-l_7_R, null);
            if (str2 != null && str2.length() > 0) {
                -l_3_R.B(str2);
            }
            -l_3_R.b(-l_8_R);
            return -l_3_R;
        } catch (Object -l_5_R2) {
            -l_5_R2.printStackTrace();
            return null;
        }
    }

    private static fn d(Context context, String str, String str2) {
        Object -l_11_R;
        Object -l_3_R = new fn();
        Object -l_4_R = lu.b(context, str, null);
        if (-l_4_R == null || -l_4_R.length() == 0) {
            return -l_3_R;
        }
        Object -l_5_R = new File(-l_4_R);
        if (!-l_5_R.exists()) {
            return -l_3_R;
        }
        FileInputStream fileInputStream = null;
        Object -l_7_R;
        try {
            FileInputStream -l_6_R = new FileInputStream(-l_5_R);
            try {
                -l_7_R = lt.c(-l_6_R);
                Object -l_8_R = new byte[-l_6_R.available()];
                -l_6_R.read(-l_8_R);
                Object -l_9_R;
                if (lq.bytesToHexString(mc.l(-l_8_R)).equals(lq.bytesToHexString(-l_7_R.yU))) {
                    -l_9_R = TccCryptor.decrypt(-l_8_R, null);
                    if (str2 != null) {
                        if (str2.length() > 0) {
                            -l_3_R.B(str2);
                        }
                    }
                    -l_3_R.b(-l_9_R);
                    if (-l_6_R == null) {
                        fileInputStream = -l_6_R;
                        return -l_3_R;
                    }
                    try {
                        -l_6_R.close();
                    } catch (Object -l_7_R2) {
                        -l_7_R2.printStackTrace();
                    }
                    return -l_3_R;
                }
                -l_9_R = -l_3_R;
                if (-l_6_R == null) {
                    fileInputStream = -l_6_R;
                } else {
                    try {
                        -l_6_R.close();
                    } catch (Object -l_10_R) {
                        -l_10_R.printStackTrace();
                    }
                }
                return -l_3_R;
            } catch (Exception e) {
                -l_7_R2 = e;
                fileInputStream = -l_6_R;
                try {
                    -l_7_R2.printStackTrace();
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Object -l_7_R22) {
                            -l_7_R22.printStackTrace();
                        }
                    }
                    return -l_3_R;
                } catch (Throwable th) {
                    -l_11_R = th;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Object -l_12_R) {
                            -l_12_R.printStackTrace();
                        }
                    }
                    throw -l_11_R;
                }
            } catch (Throwable th2) {
                -l_11_R = th2;
                fileInputStream = -l_6_R;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw -l_11_R;
            }
        } catch (Exception e2) {
            -l_7_R22 = e2;
            -l_7_R22.printStackTrace();
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return -l_3_R;
        }
    }
}
