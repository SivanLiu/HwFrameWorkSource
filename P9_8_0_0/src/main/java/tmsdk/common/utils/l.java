package tmsdk.common.utils;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import tmsdkobf.lu;

public final class l {
    private static String LI = "tms_";
    private static String LJ = "[com.android.internal.telephony.ITelephonyRegistry]";
    private static Boolean LK = null;
    private static long LL = -1;
    private static String TELEPHONY_SERVICE = "[com.android.internal.telephony.ITelephony]";

    public static class a {
        public long LM;
        public long LN;
    }

    public static String L(Context context) {
        String -l_1_R = null;
        try {
            -l_1_R = ((TelephonyManager) context.getSystemService("phone")).getDeviceId();
        } catch (Object -l_2_R) {
            -l_2_R.printStackTrace();
        }
        return -l_1_R != null ? -l_1_R : "00000000000000";
    }

    public static String M(Context context) {
        String -l_1_R = null;
        try {
            -l_1_R = ((TelephonyManager) context.getSystemService("phone")).getSubscriberId();
        } catch (Object -l_2_R) {
            -l_2_R.printStackTrace();
        }
        return -l_1_R != null ? -l_1_R : "000000000000000";
    }

    public static String N(Context context) {
        return h.G(context);
    }

    public static String O(Context context) {
        Object -l_1_R = null;
        try {
            -l_1_R = ((TelephonyManager) context.getSystemService("phone")).getSimSerialNumber();
        } catch (Object -l_2_R) {
            -l_2_R.printStackTrace();
        }
        return -l_1_R;
    }

    public static String P(Context context) {
        try {
            return Secure.getString(context.getContentResolver(), "android_id");
        } catch (Throwable th) {
            return "";
        }
    }

    public static int Q(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int R(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static String R(boolean z) {
        Object -l_3_R;
        Object -l_1_R = "";
        try {
            Object -l_5_R;
            Object -l_2_R = new FileInputStream("/proc/version");
            -l_3_R = new BufferedReader(new InputStreamReader(-l_2_R), 8192);
            Object -l_4_R = "";
            Object -l_6_R = new StringBuilder("");
            while (true) {
                try {
                    -l_5_R = -l_3_R.readLine();
                    if (-l_5_R == null) {
                        break;
                    }
                    -l_6_R.append(-l_5_R);
                } catch (Object -l_7_R) {
                    f.e("PhoneInfoUtil", -l_7_R);
                }
            }
            if (-l_3_R != null) {
                try {
                    -l_3_R.close();
                } catch (Object -l_7_R2) {
                    f.e("PhoneInfoUtil", -l_7_R2);
                }
            }
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (Object -l_7_R22) {
                    f.e("PhoneInfoUtil", -l_7_R22);
                }
            }
            -l_4_R = -l_6_R.toString();
            if (!z) {
                -l_1_R = -l_4_R;
            } else if (!(-l_4_R == null || -l_4_R.equals(""))) {
                try {
                    -l_5_R = -l_4_R.substring("version ".length() + -l_4_R.indexOf("version "));
                    -l_1_R = -l_5_R.substring(0, -l_5_R.indexOf(" "));
                } catch (Object -l_7_R222) {
                    f.e("PhoneInfoUtil", -l_7_R222);
                }
            }
            return -l_1_R;
            if (-l_2_R != null) {
                -l_2_R.close();
            }
            -l_4_R = -l_6_R.toString();
            if (!z) {
                -l_1_R = -l_4_R;
            } else {
                -l_5_R = -l_4_R.substring("version ".length() + -l_4_R.indexOf("version "));
                -l_1_R = -l_5_R.substring(0, -l_5_R.indexOf(" "));
            }
            return -l_1_R;
            -l_4_R = -l_6_R.toString();
            if (!z) {
                -l_5_R = -l_4_R.substring("version ".length() + -l_4_R.indexOf("version "));
                -l_1_R = -l_5_R.substring(0, -l_5_R.indexOf(" "));
            } else {
                -l_1_R = -l_4_R;
            }
            return -l_1_R;
        } catch (Object -l_3_R2) {
            f.e("PhoneInfoUtil", -l_3_R2);
            return -l_1_R;
        }
    }

    public static int S(Context context) {
        return p.cH(M(context));
    }

    public static String S(boolean z) {
        Object -l_1_R;
        Object -l_2_R;
        Object -l_9_R;
        if (z) {
            -l_1_R = "/sys/block/mmcblk0/device/";
            -l_2_R = "MMC";
        } else {
            -l_1_R = "/sys/block/mmcblk1/device/";
            -l_2_R = "SD";
        }
        BufferedReader bufferedReader = null;
        BufferedReader bufferedReader2 = null;
        try {
            BufferedReader -l_3_R = new BufferedReader(new FileReader(-l_1_R + "type"));
            try {
                Object -l_5_R = -l_3_R.readLine();
                if (-l_5_R != null) {
                    if (-l_5_R.toUpperCase().equals(-l_2_R)) {
                        BufferedReader -l_4_R = new BufferedReader(new FileReader(-l_1_R + "cid"));
                        try {
                            Object -l_6_R = -l_4_R.readLine();
                            if (-l_6_R == null) {
                                bufferedReader2 = -l_4_R;
                            } else {
                                Object -l_7_R = -l_6_R.trim();
                                if (-l_3_R != null) {
                                    try {
                                        -l_3_R.close();
                                    } catch (IOException e) {
                                    }
                                }
                                if (-l_4_R != null) {
                                    try {
                                        -l_4_R.close();
                                    } catch (IOException e2) {
                                    }
                                }
                                return -l_7_R;
                            }
                        } catch (Throwable th) {
                            -l_9_R = th;
                            bufferedReader2 = -l_4_R;
                            bufferedReader = -l_3_R;
                            if (bufferedReader != null) {
                                try {
                                    bufferedReader.close();
                                } catch (IOException e3) {
                                }
                            }
                            if (bufferedReader2 != null) {
                                try {
                                    bufferedReader2.close();
                                } catch (IOException e4) {
                                }
                            }
                            throw -l_9_R;
                        }
                    }
                }
                if (-l_3_R != null) {
                    try {
                        -l_3_R.close();
                    } catch (IOException e5) {
                    }
                }
                if (bufferedReader2 != null) {
                    try {
                        bufferedReader2.close();
                    } catch (IOException e6) {
                    }
                }
                bufferedReader = -l_3_R;
            } catch (Throwable th2) {
                -l_9_R = th2;
                bufferedReader = -l_3_R;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (bufferedReader2 != null) {
                    bufferedReader2.close();
                }
                throw -l_9_R;
            }
        } catch (Throwable th3) {
            -l_9_R = th3;
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedReader2 != null) {
                bufferedReader2.close();
            }
            throw -l_9_R;
        }
        return null;
    }

    public static void a(File file, a aVar) {
        Object -l_2_R;
        try {
            -l_2_R = new StatFs(file.getPath());
            long -l_3_J = (long) -l_2_R.getBlockSize();
            aVar.LM = ((long) -l_2_R.getAvailableBlocks()) * -l_3_J;
            aVar.LN = ((long) -l_2_R.getBlockCount()) * -l_3_J;
        } catch (Object -l_2_R2) {
            f.b("PhoneInfoUtil", "getSizeInfo err:" + -l_2_R2.getMessage(), -l_2_R2);
        }
    }

    public static void a(a aVar) {
        if (lu.eF()) {
            a(Environment.getExternalStorageDirectory(), aVar);
            return;
        }
        aVar.LM = 0;
        aVar.LN = 0;
    }

    public static void b(a aVar) {
        a(Environment.getDataDirectory(), aVar);
    }

    public static String cE(String str) {
        Object -l_1_R = SystemProperties.get(str);
        return -l_1_R != null ? -l_1_R : "";
    }

    public static String getProductName() {
        return Build.PRODUCT;
    }

    public static String getRadioVersion() {
        Object -l_0_R = "";
        try {
            return (String) Class.forName("android.os.Build").getMethod("getRadioVersion", new Class[0]).invoke(null, new Object[0]);
        } catch (Object -l_1_R) {
            f.g("PhoneInfoUtil", -l_1_R);
            return -l_0_R;
        }
    }

    public static String iL() {
        return Build.MODEL;
    }

    public static boolean iM() {
        if (LK == null) {
            try {
                Object -l_0_R = ScriptHelper.exec("service", "list");
                int -l_1_I = 0;
                int -l_2_I = 0;
                if (-l_0_R != null) {
                    if (-l_0_R.length > 0) {
                        Object -l_3_R = -l_0_R;
                        for (Object -l_6_R : -l_0_R) {
                            if (!-l_6_R.contains(LI)) {
                                if (-l_6_R.contains(TELEPHONY_SERVICE)) {
                                    -l_1_I++;
                                } else if (-l_6_R.contains(LJ)) {
                                    -l_2_I++;
                                }
                            }
                        }
                        if (-l_1_I <= 1 && -l_2_I <= 1) {
                            LK = Boolean.valueOf(false);
                        } else {
                            LK = Boolean.valueOf(true);
                        }
                    }
                }
                LK = Boolean.valueOf(false);
            } catch (Exception e) {
                LK = Boolean.valueOf(false);
            }
        }
        if (LK == null) {
            LK = Boolean.valueOf(false);
        }
        return LK.booleanValue();
    }

    public static String iN() {
        return VERSION.INCREMENTAL;
    }

    public static String iO() {
        return VERSION.RELEASE;
    }

    public static String iP() {
        return Build.BRAND;
    }

    public static String iQ() {
        return Build.DEVICE;
    }

    public static String iR() {
        return Build.BOARD;
    }

    public static String iS() {
        return R(true);
    }

    public static String iT() {
        Object -l_0_R = Build.MANUFACTURER;
        return -l_0_R != null ? -l_0_R : "UNKNOWN";
    }

    public static String iU() {
        Object -l_0_R;
        try {
            -l_0_R = Build.MANUFACTURER;
            if (TextUtils.isEmpty(-l_0_R)) {
                return null;
            }
            -l_0_R = -l_0_R.toLowerCase(Locale.ENGLISH);
            if (-l_0_R.contains("huawei")) {
                return cE("ro.build.version.emui");
            }
            if (-l_0_R.contains("xiaomi")) {
                return cE("ro.miui.ui.version.name");
            }
            Object -l_1_R;
            if (-l_0_R.contains("gionee")) {
                -l_1_R = cE("ro.gn.extvernumber");
                if (TextUtils.isEmpty(-l_1_R)) {
                    -l_1_R = cE("ro.build.display.id");
                }
                return -l_1_R;
            } else if (-l_0_R.contains("vivo")) {
                -l_2_R = cE("ro.vivo.os.name");
                -l_3_R = cE("ro.vivo.os.version");
                -l_1_R = (TextUtils.isEmpty(-l_2_R) || TextUtils.isEmpty(-l_3_R)) ? cE("ro.vivo.os.build.display.id") : -l_2_R + "_" + -l_3_R;
                return -l_1_R;
            } else if (-l_0_R.contains("meizu")) {
                return cE("ro.build.display.id");
            } else {
                if (-l_0_R.contains("lenovo")) {
                    String str = null;
                    -l_2_R = cE("ro.lenovo.lvp.version");
                    if (!TextUtils.isEmpty(-l_2_R)) {
                        -l_3_R = -l_2_R.split("_");
                        if (-l_3_R != null && -l_3_R.length > 0) {
                            str = -l_3_R[0];
                        }
                    }
                    if (TextUtils.isEmpty(str)) {
                        str = cE("ro.build.version.incremental");
                    }
                    return str;
                }
                if (-l_0_R.contains("letv")) {
                    return cE("ro.letv.eui");
                }
                return null;
            }
        } catch (Object -l_0_R2) {
            f.g("PhoneInfoUtil", -l_0_R2);
        }
    }

    public static long iV() {
        Object -l_2_R;
        Object -l_4_R;
        Object obj = 1;
        if (LL == -1) {
            Object -l_0_R = new File("/proc/meminfo");
            DataInputStream dataInputStream = null;
            if (-l_0_R.exists()) {
                try {
                    DataInputStream -l_1_R = new DataInputStream(new FileInputStream(-l_0_R));
                    try {
                        -l_2_R = -l_1_R.readLine();
                        if (-l_2_R != null) {
                            LL = Long.parseLong(-l_2_R.trim().split("[\\s]+")[1]);
                            if (-l_1_R != null) {
                                try {
                                    -l_1_R.close();
                                } catch (Object -l_2_R2) {
                                    -l_2_R2.printStackTrace();
                                }
                            }
                        } else {
                            throw new IOException("/proc/meminfo is empty!");
                        }
                    } catch (Throwable th) {
                        -l_4_R = th;
                        dataInputStream = -l_1_R;
                        if (dataInputStream != null) {
                            try {
                                dataInputStream.close();
                            } catch (Object -l_5_R) {
                                -l_5_R.printStackTrace();
                            }
                        }
                        throw -l_4_R;
                    }
                } catch (Throwable th2) {
                    -l_4_R = th2;
                    if (dataInputStream != null) {
                        dataInputStream.close();
                    }
                    throw -l_4_R;
                }
            }
        }
        if (LL > 0) {
            obj = null;
        }
        if (obj == null) {
        }
    }
}
