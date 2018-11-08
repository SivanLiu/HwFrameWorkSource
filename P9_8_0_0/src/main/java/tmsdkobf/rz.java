package tmsdkobf;

import android.media.ExifInterface;
import android.os.Environment;
import android.text.TextUtils;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import tmsdk.common.utils.f;

public class rz {
    private static SimpleDateFormat Qe;
    public static final TimeZone Qf = TimeZone.getDefault();
    public static final String Qg = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String[] Qh = new String[]{"screenshot", "截屏"};
    private static final String[] Qi = new String[]{"xj", "androidgeek", "logo", "pt", "MYXJ", "C360"};
    private static String TAG = "MediaFileUtil";

    public static class a {
        public static final String[] Qj = new String[]{"mp4", "avi", "3gpp", "mkv", "wmv", "3gpp2", "mp2ts", "3gp", "mov", "flv", "rmvb", "flv"};
        public static final String[] Qk = new String[]{"jpg", "jpeg", "png", "gif", "bmp"};
        public static final String[] Ql = new String[]{"mp3", "wma", "flac", "wav", "mid", "m4a", "aac"};
        public static final String[] Qm = new String[]{"jpg", "jpeg"};
    }

    private static long a(ExifInterface exifInterface) {
        Object -l_1_R = exifInterface.getAttribute("DateTime");
        if (-l_1_R == null) {
            return 0;
        }
        f.e(TAG, "exif time:" + -l_1_R);
        Object -l_2_R = new ParsePosition(0);
        Object -l_3_R;
        try {
            if (Qe == null) {
                Qe = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                Qe.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
            -l_3_R = Qe.parse(-l_1_R, -l_2_R);
            if (-l_3_R == null) {
                return 0;
            }
            long -l_4_J = -l_3_R.getTime();
            return -l_4_J - ((long) Qf.getOffset(-l_4_J));
        } catch (Object -l_3_R2) {
            f.b(TAG, "exifDateTime", -l_3_R2);
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean dA(String str) {
        Object -l_1_R = str.toLowerCase();
        for (Object -l_5_R : Qh) {
            if (-l_1_R.contains(-l_5_R)) {
                return true;
            }
        }
        return false;
    }

    public static boolean dB(String str) {
        Object -l_2_R = dC(str).toLowerCase();
        for (Object -l_6_R : a.Qk) {
            if (-l_6_R.equals(-l_2_R)) {
                return true;
            }
        }
        return false;
    }

    public static String dC(String str) {
        if (str == null) {
            return null;
        }
        Object -l_1_R = null;
        int -l_2_I = str.lastIndexOf(".");
        if (-l_2_I >= 0 && -l_2_I < str.length() - 1) {
            -l_1_R = str.substring(-l_2_I + 1);
        }
        return -l_1_R;
    }

    public static boolean dD(String str) {
        if (str == null) {
            return false;
        }
        Object -l_1_R = dC(str).toLowerCase();
        for (Object -l_5_R : a.Qm) {
            if (-l_5_R.equals(-l_1_R)) {
                return true;
            }
        }
        return false;
    }

    public static boolean dE(String str) {
        return !TextUtils.isEmpty(str) ? str.startsWith(Qg) : false;
    }

    public static boolean dF(String str) {
        if (str != null) {
            for (Object -l_4_R : Qi) {
                if (str.startsWith(-l_4_R)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static long dG(String str) {
        Object -l_1_R = null;
        try {
            -l_1_R = new ExifInterface(str);
        } catch (Object -l_2_R) {
            f.b(TAG, "getImageTakenTime", -l_2_R);
        }
        return -l_1_R == null ? 0 : a(-l_1_R);
    }

    public static String di(String str) {
        if (str == null) {
            return str;
        }
        int -l_1_I = str.lastIndexOf("/");
        return (-l_1_I >= 0 && -l_1_I < str.length() - 1) ? str.substring(-l_1_I + 1, str.length()) : str;
    }
}
