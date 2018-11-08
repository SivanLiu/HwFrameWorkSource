package tmsdkobf;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;

public class fy {
    private static int W(String str) {
        Object -l_4_R;
        Object -l_8_R;
        int -l_1_I = -1;
        if (!TextUtils.isEmpty(str)) {
            InputStreamReader -l_2_R = null;
            BufferedReader bufferedReader = null;
            try {
                -l_4_R = Runtime.getRuntime().exec("ps");
                -l_4_R.waitFor();
                InputStreamReader -l_2_R2 = new InputStreamReader(-l_4_R.getInputStream());
                try {
                    try {
                        Object -l_5_R;
                        BufferedReader -l_3_R = new BufferedReader(-l_2_R2);
                        while (true) {
                            try {
                                Object -l_6_R = -l_3_R.readLine();
                                if (!TextUtils.isEmpty(-l_6_R)) {
                                    -l_5_R = -l_6_R.split("[\\s]+");
                                    if (-l_5_R.length == 9 && -l_5_R[8].equals(str)) {
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            } catch (Throwable th) {
                                -l_8_R = th;
                                bufferedReader = -l_3_R;
                                -l_2_R = -l_2_R2;
                            }
                        }
                        -l_1_I = Integer.parseInt(-l_5_R[1]);
                        if (-l_2_R2 != null) {
                            -l_2_R2.close();
                        }
                        if (-l_3_R != null) {
                            try {
                                -l_3_R.close();
                            } catch (Object -l_4_R2) {
                                -l_4_R2.printStackTrace();
                            }
                        }
                    } catch (Throwable th2) {
                        -l_8_R = th2;
                        -l_2_R = -l_2_R2;
                        if (-l_2_R != null) {
                            -l_2_R.close();
                        }
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                        throw -l_8_R;
                    }
                } catch (Throwable th3) {
                    -l_8_R = th3;
                    -l_2_R = -l_2_R2;
                    if (-l_2_R != null) {
                        -l_2_R.close();
                    }
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    throw -l_8_R;
                }
            } catch (Throwable th4) {
                -l_4_R2 = th4;
                -l_4_R2.printStackTrace();
                if (-l_2_R != null) {
                    -l_2_R.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                return -l_1_I;
            }
        }
        return -l_1_I;
    }

    public static PackageInfo a(Context context, String str, int i) {
        Object -l_3_R = null;
        try {
            -l_3_R = context.getPackageManager().getPackageInfo(str, i);
        } catch (Throwable th) {
        }
        return -l_3_R;
    }

    public static boolean a(Context context, String str) {
        int -l_2_I = 0;
        if (context == null || str == null) {
            return false;
        }
        try {
            Object<RunningAppProcessInfo> -l_4_R = ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses();
            if (-l_4_R == null || -l_4_R.size() <= 5) {
                kt.saveActionData(1320010);
            }
            if (-l_4_R != null) {
                for (RunningAppProcessInfo -l_6_R : -l_4_R) {
                    if (str.equalsIgnoreCase(-l_6_R.processName)) {
                        -l_2_I = 1;
                        break;
                    }
                }
            }
            if (-l_2_I == 0) {
                -l_2_I = W(str) <= 0 ? 0 : 1;
            }
            if (-l_2_I != 0) {
                return -l_2_I;
            }
            for (RunningServiceInfo -l_8_R : ((ActivityManager) context.getSystemService("activity")).getRunningServices(Integer.MAX_VALUE)) {
                if (-l_8_R != null && str.equalsIgnoreCase(-l_8_R.process)) {
                    return true;
                }
            }
            return -l_2_I;
        } catch (Throwable th) {
            return false;
        }
    }

    public static boolean a(Context context, String str, String str2) {
        return context.getPackageManager().getComponentEnabledSetting(new ComponentName(str, str2)) == 2;
    }

    public static final boolean a(String str, String str2) {
        if (str2.equals("0")) {
            return true;
        }
        if (!str2.startsWith(">=")) {
            return str2.equalsIgnoreCase(str);
        } else {
            Object -l_2_R = str.split("[\\._]");
            Object -l_3_R = str2.substring(2).split("[\\._]");
            if (-l_2_R.length >= 2 && -l_3_R.length >= 3) {
                int -l_4_I = Integer.valueOf(-l_2_R[0]).intValue();
                int -l_5_I = Integer.valueOf(-l_2_R[1]).intValue();
                int -l_6_I = 0;
                if (-l_2_R.length >= 3) {
                    -l_6_I = Integer.valueOf(-l_2_R[2]).intValue();
                }
                int -l_7_I = Integer.valueOf(-l_3_R[0]).intValue();
                int -l_8_I = Integer.valueOf(-l_3_R[1]).intValue();
                int -l_9_I = Integer.valueOf(-l_3_R[2]).intValue();
                if (-l_4_I > -l_7_I) {
                    return true;
                }
                if (-l_4_I == -l_7_I) {
                    if (-l_5_I > -l_8_I) {
                        return true;
                    }
                    if (-l_5_I == -l_8_I && -l_6_I >= -l_9_I) {
                        return true;
                    }
                }
            }
        }
    }

    public static boolean c(Context context, ft ftVar) {
        if (ftVar.y() == null) {
            return false;
        }
        try {
            Object -l_4_R = a(context, ftVar.y(), 64).signatures;
            Object -l_5_R = MessageDigest.getInstance("MD5");
            if (-l_4_R != null && -l_4_R.length > 0) {
                -l_5_R.update(-l_4_R[0].toByteArray());
            }
            Object -l_6_R = -l_5_R.digest();
            Object -l_7_R = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
            Object -l_8_R = new StringBuilder(-l_6_R.length * 2);
            for (int -l_9_I = 0; -l_9_I < -l_6_R.length; -l_9_I++) {
                -l_8_R.append(-l_7_R[(-l_6_R[-l_9_I] & 240) >>> 4]);
                -l_8_R.append(-l_7_R[-l_6_R[-l_9_I] & 15]);
            }
            return -l_8_R.toString().equalsIgnoreCase(ftVar.z());
        } catch (Exception e) {
            return false;
        }
    }
}
