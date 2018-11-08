package tmsdk.common.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import tmsdkobf.mb;

public class h {
    public static String G(Context context) {
        Object -l_1_R = "";
        Object -l_2_R;
        try {
            -l_1_R = H(context);
            mb.n("MacUtil", "getMacOld, mac: " + -l_1_R);
            if (!TextUtils.isEmpty(-l_1_R) && !"02:00:00:00:00:00".equals(-l_1_R)) {
                return -l_1_R;
            }
            -l_2_R = cD("wifi.interface");
            mb.n("MacUtil", "interfaceName: " + -l_2_R);
            if (TextUtils.isEmpty(-l_2_R)) {
                -l_2_R = "wlan0";
            }
            -l_1_R = cB(-l_2_R);
            mb.n("MacUtil", "getMacByAPI, mac: " + -l_1_R);
            if (!TextUtils.isEmpty(-l_1_R) && !"02:00:00:00:00:00".equals(-l_1_R)) {
                return -l_1_R;
            }
            -l_1_R = cC(-l_2_R);
            mb.n("MacUtil", "getMacFromFile, mac: " + -l_1_R);
            return -l_1_R;
        } catch (Object -l_2_R2) {
            mb.b("MacUtil", "getMac: " + -l_2_R2, -l_2_R2);
            return -l_1_R;
        }
    }

    private static String H(Context context) {
        Object -l_1_R = "";
        try {
            Object -l_3_R = ((WifiManager) context.getSystemService("wifi")).getConnectionInfo();
            if (-l_3_R != null) {
                -l_1_R = -l_3_R.getMacAddress();
            }
        } catch (Object -l_2_R) {
            mb.b("MacUtil", "getMac exception: " + -l_2_R, -l_2_R);
        }
        return -l_1_R == null ? "" : -l_1_R;
    }

    private static String cB(String str) {
        Object -l_1_R = "";
        try {
            Object -l_3_R = NetworkInterface.getByName(str).getHardwareAddress();
            if (-l_3_R == null) {
                return -l_1_R;
            }
            Object -l_4_R = new StringBuilder();
            Object -l_5_R = -l_3_R;
            int -l_6_I = -l_3_R.length;
            for (int -l_7_I = 0; -l_7_I < -l_6_I; -l_7_I++) {
                -l_4_R.append(String.format("%02x:", new Object[]{Byte.valueOf(-l_3_R[-l_7_I])}));
            }
            if (-l_4_R.length() > 0) {
                -l_4_R.deleteCharAt(-l_4_R.length() - 1);
            }
            -l_1_R = -l_4_R.toString();
            return -l_1_R;
        } catch (Object -l_2_R) {
            mb.b("MacUtil", "getMacByAPI: " + -l_2_R, -l_2_R);
        }
    }

    private static String cC(String str) {
        Object -l_1_R = "";
        try {
            Object -l_3_R = g(new File(String.format("/sys/class/net/%s/address", new Object[]{str})));
            if (-l_3_R != null && -l_3_R.size() == 1) {
                String -l_1_R2 = (String) -l_3_R.get(0);
                if (!TextUtils.isEmpty(-l_1_R2)) {
                    -l_1_R = -l_1_R2.trim();
                }
            }
        } catch (Object -l_2_R) {
            mb.b("MacUtil", "getMacFromFile: " + -l_2_R, -l_2_R);
        }
        return -l_1_R;
    }

    public static String cD(String str) {
        Object -l_1_R = "";
        try {
            Object -l_3_R = Class.forName("android.os.SystemProperties").getMethod("get", new Class[]{String.class});
            -l_3_R.setAccessible(true);
            -l_1_R = (String) -l_3_R.invoke(null, new Object[]{str});
            if (-l_1_R == null) {
                -l_1_R = "";
            }
        } catch (Object -l_2_R) {
            mb.s("MacUtil", " getSysPropByReflect: " + -l_2_R);
        }
        return -l_1_R;
    }

    public static List<String> g(File file) {
        Object -l_5_R;
        Object -l_6_R;
        Object -l_1_R = new ArrayList();
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            InputStreamReader -l_3_R;
            FileInputStream -l_2_R = new FileInputStream(file);
            try {
                try {
                    -l_3_R = new InputStreamReader(-l_2_R);
                } catch (Throwable th) {
                    -l_6_R = th;
                    fileInputStream = -l_2_R;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    throw -l_6_R;
                }
            } catch (Throwable th2) {
                -l_6_R = th2;
                fileInputStream = -l_2_R;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                throw -l_6_R;
            }
            try {
                try {
                    BufferedReader -l_4_R = new BufferedReader(-l_3_R);
                    while (true) {
                        try {
                            -l_5_R = -l_4_R.readLine();
                            if (-l_5_R == null) {
                                break;
                            }
                            -l_1_R.add(-l_5_R);
                        } catch (Throwable th3) {
                            -l_6_R = th3;
                            bufferedReader = -l_4_R;
                            inputStreamReader = -l_3_R;
                            fileInputStream = -l_2_R;
                        }
                    }
                    if (-l_2_R != null) {
                        try {
                            -l_2_R.close();
                        } catch (Object -l_5_R2) {
                            -l_5_R2.printStackTrace();
                        }
                    }
                    if (-l_3_R != null) {
                        -l_3_R.close();
                    }
                    if (-l_4_R != null) {
                        -l_4_R.close();
                    }
                    bufferedReader = -l_4_R;
                    inputStreamReader = -l_3_R;
                    fileInputStream = -l_2_R;
                } catch (Throwable th4) {
                    -l_6_R = th4;
                    inputStreamReader = -l_3_R;
                    fileInputStream = -l_2_R;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    throw -l_6_R;
                }
            } catch (Throwable th5) {
                -l_6_R = th5;
                inputStreamReader = -l_3_R;
                fileInputStream = -l_2_R;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                throw -l_6_R;
            }
        } catch (Throwable th6) {
            -l_5_R2 = th6;
            mb.b("MacUtil", "readLines：" + -l_5_R2, -l_5_R2);
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (inputStreamReader != null) {
                inputStreamReader.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            return -l_1_R;
        }
        return -l_1_R;
    }
}
