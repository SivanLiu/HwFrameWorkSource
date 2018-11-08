package tmsdkobf;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.telephony.TelephonyManager;
import tmsdk.common.TMServiceFactory;

public class ln {
    public static int yI = 4;
    public static String yJ = null;
    public static int yK = 80;
    public static byte yL = (byte) 0;
    public static boolean yM = false;
    public static boolean yN = false;
    public static byte yO = (byte) 4;
    public static String yP = "unknown";
    public static byte yQ = (byte) 9;
    public static int yR = 17;

    private static int a(Context context, NetworkInfo networkInfo) {
        int -l_2_I = 0;
        if (networkInfo == null) {
            return 0;
        }
        try {
            if (1 != networkInfo.getType()) {
                if (networkInfo.getType() == 0) {
                    TelephonyManager -l_3_R = (TelephonyManager) context.getSystemService("phone");
                    if (-l_3_R != null) {
                        switch (-l_3_R.getNetworkType()) {
                            case 1:
                                -l_2_I = 2;
                                break;
                            case 2:
                                -l_2_I = 3;
                                break;
                            case 3:
                                -l_2_I = 4;
                                break;
                            case 4:
                                -l_2_I = 8;
                                break;
                            case 5:
                                -l_2_I = 9;
                                break;
                            case 6:
                                -l_2_I = 10;
                                break;
                            case 7:
                                -l_2_I = 11;
                                break;
                            case 8:
                                -l_2_I = 5;
                                break;
                            case 9:
                                -l_2_I = 6;
                                break;
                            case 10:
                                -l_2_I = 7;
                                break;
                            default:
                                -l_2_I = 17;
                                break;
                        }
                    }
                }
            }
            -l_2_I = 1;
        } catch (Object -l_3_R2) {
            -l_3_R2.printStackTrace();
        }
        return -l_2_I;
    }

    private static boolean aN(int i) {
        return i == 2 || i == 0;
    }

    private static void bI(String str) {
        if (str != null) {
            if (str.contains("cmwap")) {
                yP = "cmwap";
                yQ = (byte) 0;
            } else if (str.contains("cmnet")) {
                yP = "cmnet";
                yQ = (byte) 1;
            } else if (str.contains("3gwap")) {
                yP = "3gwap";
                yQ = (byte) 2;
            } else if (str.contains("3gnet")) {
                yP = "3gnet";
                yQ = (byte) 3;
            } else if (str.contains("uniwap")) {
                yP = "uniwap";
                yQ = (byte) 4;
            } else if (str.contains("uninet")) {
                yP = "uninet";
                yQ = (byte) 5;
            } else if (str.contains("ctwap")) {
                yP = "ctwap";
                yQ = (byte) 6;
            } else if (str.contains("ctnet")) {
                yP = "ctnet";
                yQ = (byte) 7;
            } else if (str.contains("#777")) {
                yP = "#777";
                yQ = (byte) 8;
            }
        }
    }

    public static void init(Context context) {
        NetworkInfo -l_1_R = null;
        try {
            -l_1_R = TMServiceFactory.getSystemInfoService().getActiveNetworkInfo();
        } catch (Object -l_2_R) {
            mb.s("getActiveNetworkInfo", " getActiveNetworkInfo NullPointerException--- \n" + -l_2_R.getMessage());
        }
        mb.d("Apn", "networkInfo : " + -l_1_R);
        int -l_2_I = -1;
        try {
            yI = 0;
            yO = (byte) 4;
            String -l_3_R = null;
            if (-l_1_R != null) {
                -l_2_I = -l_1_R.getType();
                mb.d("Apn", "type: " + -l_1_R.getType());
                mb.d("Apn", "typeName: " + -l_1_R.getTypeName());
                -l_3_R = -l_1_R.getExtraInfo();
                if (-l_3_R != null) {
                    -l_3_R = -l_3_R.trim().toLowerCase();
                } else {
                    yI = 0;
                }
            }
            mb.d("Apn", "extraInfo : " + -l_3_R);
            if (-l_2_I != 1) {
                bI(-l_3_R);
                if (-l_3_R == null) {
                    yI = 0;
                } else if (-l_3_R.contains("cmwap") || -l_3_R.contains("uniwap") || -l_3_R.contains("3gwap") || -l_3_R.contains("ctwap")) {
                    yO = (byte) 1;
                    if (-l_3_R.contains("3gwap")) {
                        yO = (byte) 2;
                    }
                    yI = 2;
                } else if (-l_3_R.contains("cmnet") || -l_3_R.contains("uninet") || -l_3_R.contains("3gnet") || -l_3_R.contains("ctnet")) {
                    yO = (byte) 1;
                    if (-l_3_R.contains("3gnet") || -l_3_R.contains("ctnet")) {
                        yO = (byte) 2;
                    }
                    yI = 1;
                } else if (-l_3_R.contains("#777")) {
                    yO = (byte) 2;
                    yI = 0;
                } else {
                    yI = 0;
                }
                yM = false;
                if (aN(yI)) {
                    yJ = Proxy.getDefaultHost();
                    yK = Proxy.getDefaultPort();
                    if (yJ != null) {
                        yJ = yJ.trim();
                    }
                    if (yJ != null) {
                        if (!"".equals(yJ)) {
                            yM = true;
                            yI = 2;
                            if ("10.0.0.200".equals(yJ)) {
                                yL = (byte) 1;
                            } else {
                                yL = (byte) 0;
                            }
                        }
                    }
                    yM = false;
                    yI = 1;
                }
            } else {
                yI = 4;
                yM = false;
                yO = (byte) 3;
                yP = "unknown";
                yQ = (byte) 9;
            }
            mb.d("Apn", "NETWORK_TYPE : " + yO);
            mb.d("Apn", "M_APN_TYPE : " + yI);
            mb.d("Apn", "M_USE_PROXY : " + yM);
            mb.d("Apn", "M_APN_PROXY : " + yJ);
            mb.d("Apn", "M_APN_PORT : " + yK);
        } catch (Object -l_2_R2) {
            -l_2_R2.printStackTrace();
        }
        yR = a(context, -l_1_R);
        mb.n("Apn", "init() Apn.APN_NAME_VALUE: " + yQ + " APN_NAME_DRI: " + yP + " NETWORK_TYPE: " + yO + " ENT_VALUE: " + yR);
    }

    public static void q(Context context) {
        if (!yN) {
            Object -l_1_R = ln.class;
            synchronized (ln.class) {
                if (yN) {
                    return;
                }
                init(context);
                yN = true;
            }
        }
    }
}
