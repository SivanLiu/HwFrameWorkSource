package tmsdkobf;

public final class mg {
    public static boolean bX(String str) {
        if (str == null || str.trim().length() < 3) {
            return false;
        }
        Object -l_1_R = new char[]{'/', '#', ',', ';', '.', '(', ')', 'N', '*'};
        Object -l_2_R = -l_1_R;
        int -l_3_I = -l_1_R.length;
        for (int -l_4_I = 0; -l_4_I < -l_3_I; -l_4_I++) {
            if (str.indexOf(-l_2_R[-l_4_I]) >= 0) {
                return false;
            }
        }
        return true;
    }
}
