package tmsdk.common.utils;

public class t {
    static final char[] mE = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String I(long j) {
        Object -l_2_R = new int[]{(int) ((j >> 24) & 255), (int) ((j >> 16) & 255), (int) ((j >> 8) & 255), (int) (j & 255)};
        return Integer.toString(-l_2_R[3]) + "." + Integer.toString(-l_2_R[2]) + "." + Integer.toString(-l_2_R[1]) + "." + Integer.toString(-l_2_R[0]);
    }
}
