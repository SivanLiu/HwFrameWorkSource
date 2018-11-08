package tmsdk.common.utils;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.utils.l.a;
import tmsdkobf.lu;
import tmsdkobf.md;

public class e {
    private static Integer LE = null;
    private static String TAG = "EnvUtil";

    public static String[] D(Context context) {
        Object -l_1_R = new String[4];
        -l_1_R[0] = Build.MODEL;
        -l_1_R[1] = VERSION.RELEASE;
        Object -l_4_R = "";
        try {
            -l_4_R = lu.bN("/proc/cpuinfo").split("\\n")[0];
        } catch (Object -l_6_R) {
            -l_6_R.printStackTrace();
        }
        -l_1_R[2] = -l_4_R;
        -l_1_R[3] = Integer.toString(l.Q(context)) + "*" + Integer.toString(l.R(context));
        return -l_1_R;
    }

    public static String E(Context context) {
        int i = 1;
        Object -l_1_R = new String();
        String[] strArr = new String[4];
        Object -l_2_R = D(context);
        -l_1_R = (((((((-l_1_R + "MODEL " + -l_2_R[0] + ";") + "ANDROID " + -l_2_R[1] + ";") + "CPU " + -l_2_R[2] + ";") + "CPUFreq " + iz() + ";") + "CPUNum " + Runtime.getRuntime().availableProcessors() + ";") + "resolution " + -l_2_R[3] + ";") + "ram " + l.iV() + ";") + "rom " + iA() + ";";
        Object -l_3_R = new a();
        l.a(-l_3_R);
        -l_1_R = -l_1_R + "sdcard " + -l_3_R.LN + ";";
        int -l_4_I = l.iM();
        StringBuilder append = new StringBuilder().append(-l_1_R).append("simNum ");
        if (-l_4_I != 0) {
            i = 2;
        }
        -l_1_R = (append.append(i).append(";").toString() + "baseband " + SystemProperties.get("gsm.version.baseband", "") + ";") + "inversion " + Build.DISPLAY + ";";
        Object -l_5_R = new md("NetInterfaceManager").getString("upload_config_des", null);
        return (-l_5_R == null || -l_5_R.length() == 0) ? -l_1_R : -l_1_R + -l_5_R;
    }

    public static boolean F(Context context) {
        Object -l_2_R = TMServiceFactory.getSystemInfoService().a(context.getPackageName(), 1);
        return -l_2_R != null && -l_2_R.hx();
    }

    public static long iA() {
        Object -l_1_R = new StatFs(Environment.getDataDirectory().getPath());
        return ((long) -l_1_R.getBlockSize()) * ((long) -l_1_R.getBlockCount());
    }

    public static int iB() {
        return !ScriptHelper.isSuExist ? 0 : ScriptHelper.getRootState() != 0 ? 2 : 1;
    }

    public static int iC() {
        if (LE == null) {
            try {
                Object -l_2_R = new File("/sys/devices/system/cpu/").listFiles(new FileFilter() {
                    public boolean accept(File file) {
                        return Pattern.matches("cpu[0-9]", file.getName());
                    }
                });
                if (-l_2_R == null) {
                    return 1;
                }
                f.d(TAG, "CPU Count: " + -l_2_R.length);
                LE = Integer.valueOf(-l_2_R.length);
            } catch (Object -l_0_R) {
                f.g(TAG, -l_0_R);
                return 1;
            }
        }
        return LE.intValue();
    }

    public static long iD() {
        Object -l_1_R = new StatFs(Environment.getRootDirectory().getPath());
        return (((long) -l_1_R.getBlockSize()) * ((long) -l_1_R.getBlockCount())) + iA();
    }

    public static String iz() {
        Object -l_0_R = new StringBuilder();
        try {
            Object -l_4_R = new ProcessBuilder(new String[]{"/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"}).start().getInputStream();
            Object -l_5_R = new byte[24];
            while (-l_4_R.read(-l_5_R) != -1) {
                -l_0_R.append(new String(-l_5_R));
            }
            -l_4_R.close();
        } catch (Object -l_2_R) {
            -l_2_R.printStackTrace();
            -l_0_R = new StringBuilder("N/A");
        }
        return -l_0_R.toString().trim();
    }
}
