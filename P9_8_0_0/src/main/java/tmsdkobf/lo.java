package tmsdkobf;

import tmsdk.common.exception.UnauthorizedCallerException;

public class lo {
    public static void a(Class<?>... -l_7_R) throws UnauthorizedCallerException {
        Object -l_1_R = Thread.currentThread().getStackTrace();
        int -l_2_I = 0;
        Object -l_3_R = -l_1_R;
        int -l_4_I = -l_1_R.length;
        for (int -l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
            Object -l_6_R = -l_3_R[-l_5_I];
            for (Object -l_10_R : -l_7_R) {
                if (-l_6_R.getClassName().equals(-l_10_R.getName())) {
                    -l_2_I = 1;
                    break;
                }
            }
        }
        if (-l_2_I == 0) {
            throw new UnauthorizedCallerException();
        }
    }
}
