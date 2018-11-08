package com.huawei.android.pushagent.a.a;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.text.TextUtils;
import java.lang.reflect.InvocationTargetException;

public class a {
    private static final char[] a = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static int a() {
        int -l_0_I = 0;
        Object -l_1_R = new Class[]{String.class, Integer.TYPE};
        Object -l_2_R = new Object[]{"ro.build.hw_emui_api_level", Integer.valueOf(0)};
        try {
            Object -l_3_R = Class.forName("android.os.SystemProperties");
            -l_0_I = ((Integer) -l_3_R.getDeclaredMethod("getInt", -l_1_R).invoke(-l_3_R, -l_2_R)).intValue();
            c.b("PushLogSC2907", "getEmuiLevel:" + -l_0_I);
            return -l_0_I;
        } catch (ClassNotFoundException e) {
            c.d("PushLogSC2907", " getEmuiLevel wrong, ClassNotFoundException");
            return -l_0_I;
        } catch (ExceptionInInitializerError e2) {
            c.d("PushLogSC2907", " getEmuiLevel wrong, ExceptionInInitializerError");
            return -l_0_I;
        } catch (LinkageError e3) {
            c.d("PushLogSC2907", " getEmuiLevel wrong, LinkageError");
            return -l_0_I;
        } catch (NoSuchMethodException e4) {
            c.d("PushLogSC2907", " getEmuiLevel wrong, NoSuchMethodException");
            return -l_0_I;
        } catch (NullPointerException e5) {
            c.d("PushLogSC2907", " getEmuiLevel wrong, NullPointerException");
            return -l_0_I;
        } catch (IllegalAccessException e6) {
            c.d("PushLogSC2907", " getEmuiLevel wrong, IllegalAccessException");
            return -l_0_I;
        } catch (IllegalArgumentException e7) {
            c.d("PushLogSC2907", " getEmuiLevel wrong, IllegalArgumentException");
            return -l_0_I;
        } catch (InvocationTargetException e8) {
            c.d("PushLogSC2907", " getEmuiLevel wrong, InvocationTargetException");
            return -l_0_I;
        }
    }

    public static int a(Context context) {
        ConnectivityManager -l_1_R = (ConnectivityManager) context.getSystemService("connectivity");
        if (-l_1_R == null) {
            return -1;
        }
        Object -l_2_R = -l_1_R.getAllNetworkInfo();
        if (-l_2_R == null) {
            return -1;
        }
        for (int -l_3_I = 0; -l_3_I < -l_2_R.length; -l_3_I++) {
            if (-l_2_R[-l_3_I].getState() == State.CONNECTED) {
                return -l_2_R[-l_3_I].getType();
            }
        }
        return -1;
    }

    public static Object a(Class cls, String str, Class[] clsArr, Object[] objArr) throws Exception {
        if (cls != null) {
            if (clsArr != null) {
                if (objArr == null) {
                    throw new Exception("paramsType or params should be same");
                } else if (clsArr.length != objArr.length) {
                    throw new Exception("paramsType len:" + clsArr.length + " should equal params.len:" + objArr.length);
                }
            } else if (objArr != null) {
                throw new Exception("paramsType is null, but params is not null");
            }
            try {
                return cls.getMethod(str, clsArr).invoke(null, objArr);
            } catch (Object -l_5_R) {
                c.d("PushLogSC2907", "invoke emui method exception:" + -l_5_R.getMessage());
                return null;
            }
        }
        throw new Exception("class is null in staticFun");
    }

    public static Object a(String str, String str2, Class[] clsArr, Object[] objArr) {
        try {
            return a(Class.forName(str), str2, clsArr, objArr);
        } catch (Object -l_5_R) {
            c.b("PushLogSC2907", -l_5_R.getMessage());
            return null;
        } catch (Object -l_5_R2) {
            c.b("PushLogSC2907", -l_5_R2.getMessage());
            return null;
        } catch (Object -l_5_R22) {
            c.b("PushLogSC2907", -l_5_R22.getMessage());
            return null;
        }
    }

    public static String a(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        if (bArr.length == 0) {
            return "";
        }
        Object -l_1_R = new char[(bArr.length * 2)];
        for (int -l_2_I = 0; -l_2_I < bArr.length; -l_2_I++) {
            int -l_3_I = bArr[-l_2_I];
            -l_1_R[-l_2_I * 2] = (char) a[(-l_3_I & 240) >> 4];
            -l_1_R[(-l_2_I * 2) + 1] = (char) a[-l_3_I & 15];
        }
        return new String(-l_1_R);
    }

    public static boolean a(Context context, String str) {
        try {
            if ((context.getPackageManager().getApplicationInfo(str, 0).flags & 1) == 0) {
                c.b("PushLogSC2907", "not SystemApp");
                return false;
            }
            c.b("PushLogSC2907", "isSystemApp");
            return true;
        } catch (NameNotFoundException e) {
            c.c("PushLogSC2907", " isSystemApp NameNotFoundException");
        }
    }

    public static byte[] a(String str) {
        Object -l_2_R = new byte[(str.length() / 2)];
        try {
            Object -l_3_R = str.getBytes("UTF-8");
            for (int -l_4_I = 0; -l_4_I < -l_2_R.length; -l_4_I++) {
                -l_2_R[-l_4_I] = (byte) ((byte) (((byte) (Byte.decode("0x" + new String(new byte[]{(byte) -l_3_R[-l_4_I * 2]}, "UTF-8")).byteValue() << 4)) ^ Byte.decode("0x" + new String(new byte[]{(byte) -l_3_R[(-l_4_I * 2) + 1]}, "UTF-8")).byteValue()));
            }
        } catch (Object -l_4_R) {
            c.d("PushLogSC2907", -l_4_R.toString());
        }
        return -l_2_R;
    }

    public static String b() {
        Object -l_0_R = "";
        Object -l_1_R;
        try {
            -l_1_R = a("android.os.SystemProperties", "get", new Class[]{String.class, String.class}, new Object[]{"ro.build.version.emui", ""});
            if (-l_1_R != null) {
                String -l_0_R2 = (String) -l_1_R;
            }
        } catch (Object -l_1_R2) {
            c.b("PushLogSC2907", -l_1_R2.getMessage());
        }
        return -l_0_R;
    }

    public static boolean c() {
        if (!TextUtils.isEmpty(b())) {
            return true;
        }
        c.a("PushLogSC2907", "it is not EMUI system.");
        return false;
    }

    public static boolean d() {
        int -l_0_I = 0;
        try {
            Object -l_1_R = Class.forName("android.os.SystemProperties");
            Object -l_3_R = -l_1_R.getMethod("get", new Class[]{String.class, String.class}).invoke(-l_1_R, new Object[]{"ro.product.locale.region", ""});
            c.a("PushLogSC2907", "regionCode : " + -l_3_R.toString());
            -l_0_I = "CN".equals(-l_3_R.toString());
        } catch (Exception e) {
            c.c("PushLogSC2907", "get region invocation target exception");
        }
        return -l_0_I;
    }

    public static boolean e() {
        boolean z = false;
        Object -l_1_R;
        try {
            Object -l_0_R = Class.forName("android.telephony.HwTelephonyManager");
            -l_1_R = -l_0_R.getDeclaredField("SUPPORT_SYSTEMAPP_GET_DEVICEID");
            if (-l_1_R != null) {
                -l_1_R.setAccessible(true);
                if (1 == -l_1_R.getInt(-l_0_R)) {
                    z = true;
                }
                return z;
            }
        } catch (Object -l_1_R2) {
            c.d("PushLogSC2907", -l_1_R2.toString());
        } catch (Object -l_1_R22) {
            c.d("PushLogSC2907", -l_1_R22.toString());
        } catch (Object -l_1_R222) {
            c.d("PushLogSC2907", -l_1_R222.toString());
        } catch (Object -l_1_R2222) {
            c.d("PushLogSC2907", -l_1_R2222.toString());
        }
        return false;
    }
}
