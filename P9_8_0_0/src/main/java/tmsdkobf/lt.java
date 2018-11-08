package tmsdkobf;

import java.io.InputStream;

public class lt {
    public static ls c(InputStream inputStream) {
        Object -l_1_R = new ls();
        Object -l_2_R = new byte[4];
        try {
            inputStream.read(-l_2_R);
            -l_1_R.sW = lq.k(-l_2_R);
            inputStream.read(-l_2_R);
            -l_1_R.yT = lq.k(-l_2_R);
            -l_2_R = new byte[16];
            inputStream.read(-l_2_R);
            -l_1_R.yU = -l_2_R;
        } catch (Object -l_3_R) {
            -l_3_R.printStackTrace();
        }
        return -l_1_R;
    }
}
