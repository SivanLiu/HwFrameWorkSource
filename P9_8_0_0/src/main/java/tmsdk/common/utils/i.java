package tmsdk.common.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Proxy;
import android.os.Build.VERSION;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;
import tmsdkobf.eb;
import tmsdkobf.ln;

public class i {
    private static int LH;

    public static ConnectivityManager I(Context context) {
        Object -l_1_R = null;
        try {
            return (ConnectivityManager) context.getSystemService("connectivity");
        } catch (Object -l_2_R) {
            -l_2_R.printStackTrace();
            return -l_1_R;
        }
    }

    public static int J(Context context) {
        if (!ln.yN) {
            ln.yN = false;
            ln.q(context);
        }
        switch (ln.yR) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            case 8:
                return 8;
            case 9:
                return 9;
            case 10:
                return 10;
            case 11:
                return 11;
            case 12:
                return 12;
            case 13:
                return 13;
            case 14:
                return 14;
            case 15:
                return 15;
            case 16:
                return 16;
            case 17:
                return 0;
            default:
                return 0;
        }
    }

    public static boolean K(Context context) {
        try {
            Object -l_2_R = ((ConnectivityManager) context.getSystemService("connectivity")).getNetworkInfo(1);
            return -l_2_R != null && -l_2_R.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    public static String getNetworkName() {
        Object -l_0_R = "";
        NetworkInfo -l_1_R = null;
        try {
            -l_1_R = TMServiceFactory.getSystemInfoService().getActiveNetworkInfo();
        } catch (Object -l_2_R) {
            f.g("getActiveNetworkInfo", " getActiveNetworkInfo NullPointerException--- \n" + -l_2_R.getMessage());
        }
        if (-l_1_R == null) {
            return -l_0_R;
        }
        -l_0_R = -l_1_R.getType() != 1 ? -l_1_R.getExtraInfo() : u.getSSID();
        if (-l_0_R == null) {
            -l_0_R = "";
        }
        return -l_0_R;
    }

    public static boolean hm() {
        Object -l_0_R = hn();
        return -l_0_R != null ? -l_0_R.isConnected() : false;
    }

    public static NetworkInfo hn() {
        Object -l_0_R = null;
        try {
            -l_0_R = TMServiceFactory.getSystemInfoService().getActiveNetworkInfo();
        } catch (Object -l_1_R) {
            f.g("getActiveNetworkInfo", " getActiveNetworkInfo NullPointerException--- \n" + -l_1_R.getMessage());
        }
        return -l_0_R;
    }

    public static boolean iE() {
        try {
            Object -l_1_R = I(TMSDKContext.getApplicaionContext());
            if (-l_1_R != null) {
                Object -l_2_R = -l_1_R.getAllNetworkInfo();
                if (-l_2_R != null) {
                    for (NetworkInfo state : -l_2_R) {
                        if (state.getState() == State.CONNECTED) {
                            return true;
                        }
                    }
                }
            }
        } catch (Object -l_0_R) {
            f.e("NetworkUtil", -l_0_R);
        }
        return false;
    }

    public static boolean iF() {
        NetworkInfo -l_0_R = null;
        try {
            -l_0_R = TMServiceFactory.getSystemInfoService().getActiveNetworkInfo();
        } catch (Object -l_1_R) {
            f.g("getActiveNetworkInfo", " getActiveNetworkInfo NullPointerException--- \n" + -l_1_R.getMessage());
        }
        if (-l_0_R != null && -l_0_R.getType() == 0) {
            if (-l_0_R.getSubtype() == 1 || -l_0_R.getSubtype() == 4 || -l_0_R.getSubtype() == 2) {
                return true;
            }
        }
        return false;
    }

    public static eb iG() {
        NetworkInfo -l_0_R = null;
        try {
            -l_0_R = TMServiceFactory.getSystemInfoService().getActiveNetworkInfo();
        } catch (Object -l_1_R) {
            Object -l_1_R2;
            f.g("getActiveNetworkInfo", " getActiveNetworkInfo NullPointerException--- \n" + -l_1_R2.getMessage());
        }
        if (-l_0_R == null) {
            return eb.iH;
        }
        if (-l_0_R.getType() == 1) {
            return eb.iJ;
        }
        if (-l_0_R.getType() != 0) {
            return eb.iL;
        }
        -l_1_R2 = iI();
        return (-l_1_R2 != null && -l_1_R2.length() > 0 && iJ() > 0) ? eb.iK : eb.iL;
    }

    public static boolean iH() {
        return VERSION.SDK_INT >= 14;
    }

    public static String iI() {
        return !iH() ? Proxy.getHost(TMSDKContext.getApplicaionContext()) : System.getProperty("http.proxyHost");
    }

    public static int iJ() {
        int -l_0_I;
        if (iH()) {
            try {
                -l_0_I = Integer.parseInt(System.getProperty("http.proxyPort"));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        -l_0_I = Proxy.getPort(TMSDKContext.getApplicaionContext());
        return -l_0_I;
    }

    public static boolean iK() {
        if (n.iX() < 11) {
            return true;
        }
        if (LH < 1) {
            LH = TMSDKContext.getApplicaionContext().getApplicationInfo().targetSdkVersion;
        }
        return LH < 10;
    }
}
