package tmsdkobf;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class ls {
    public int sW = 0;
    public int yT = 0;
    public byte[] yU;

    public ls() {
        Object -l_1_R = new byte[16];
        for (int -l_2_I = 0; -l_2_I < 15; -l_2_I++) {
            -l_1_R[-l_2_I] = null;
        }
        this.yU = -l_1_R;
    }

    public byte[] eD() {
        Object -l_1_R;
        try {
            -l_1_R = new ByteArrayOutputStream();
            Object -l_2_R = new DataOutputStream(-l_1_R);
            -l_2_R.write(lq.aO(this.sW));
            -l_2_R.write(lq.aO(this.yT));
            -l_2_R.write(this.yU);
            -l_2_R.flush();
            return -l_1_R.toByteArray();
        } catch (Object -l_1_R2) {
            -l_1_R2.printStackTrace();
            return null;
        }
    }
}
