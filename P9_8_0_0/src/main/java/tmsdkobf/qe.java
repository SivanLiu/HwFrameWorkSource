package tmsdkobf;

import android.telephony.PhoneNumberUtils;

public class qe extends PhoneNumberUtils {
    public static final String[] Lh = new String[]{"-", "+86", "12593", "17909", "17951", "17911", "10193", "12583", "12520", "96688"};

    public static String cA(String str) {
        if (str == null) {
            return null;
        }
        int -l_1_I = str.length();
        Object -l_2_R = new StringBuilder(-l_1_I);
        int -l_3_I = 0;
        for (int -l_4_I = 0; -l_4_I < -l_1_I; -l_4_I++) {
            int -l_5_I = str.charAt(-l_4_I);
            if (-l_5_I == 43) {
                if (-l_3_I == 0) {
                    -l_3_I = 1;
                } else {
                    continue;
                }
            }
            if (!isDialable(-l_5_I)) {
                if (isStartsPostDial(-l_5_I)) {
                    break;
                }
            } else {
                -l_2_R.append(-l_5_I);
            }
        }
        return -l_2_R.toString();
    }

    public static String cv(String str) {
        if (str == null || str.length() <= 2) {
            return str;
        }
        for (Object -l_4_R : Lh) {
            if (str.startsWith(-l_4_R)) {
                return str.substring(-l_4_R.length());
            }
        }
        return str;
    }

    public static boolean cw(String str) {
        for (Object -l_5_R : Lh) {
            if (str.startsWith(-l_5_R)) {
                return true;
            }
        }
        return false;
    }

    public static String cx(String str) {
        return str == null ? str : stripSeparators(str).replace("-", "").replace(" ", "").trim();
    }

    public static boolean cy(String str) {
        for (int -l_1_I = str.length() - 1; -l_1_I >= 0; -l_1_I--) {
            if (!isISODigit(str.charAt(-l_1_I))) {
                return false;
            }
        }
        return true;
    }

    public static String cz(String str) {
        return i(cA(cv(str)), 8);
    }

    private static String i(String str, int i) {
        if (str == null) {
            return null;
        }
        Object -l_2_R = new StringBuilder(i);
        int -l_3_I = str.length();
        int -l_4_I = -l_3_I - 1;
        int -l_5_I = -l_3_I;
        while (-l_4_I >= 0 && -l_3_I - -l_4_I <= i) {
            -l_2_R.append(str.charAt(-l_4_I));
            -l_4_I--;
        }
        return -l_2_R.toString();
    }
}
