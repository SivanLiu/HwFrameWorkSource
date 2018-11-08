package tmsdkobf;

import java.util.List;
import tmsdk.common.TMServiceFactory;

public class rj {
    private boolean Ac = false;
    private pa Pp = TMServiceFactory.getSystemInfoService();

    public String J(List<String> list) {
        if (list == null) {
            return null;
        }
        for (String -l_3_R : list) {
            if (dk(-l_3_R)) {
                return -l_3_R;
            }
        }
        return null;
    }

    public int K(List<String> list) {
        if (list == null) {
            return -1;
        }
        int -l_2_I = 0;
        int -l_3_I = -1;
        long -l_4_J = -1;
        Object -l_6_R = qo.jz().jB();
        for (String -l_8_R : list) {
            Long -l_9_R = (Long) -l_6_R.get(-l_8_R);
            long -l_10_J = 0;
            if (-l_9_R != null) {
                -l_10_J = -l_9_R.longValue();
            }
            if ((-l_10_J <= -l_4_J ? 1 : null) == null) {
                -l_4_J = -l_10_J;
                -l_3_I = -l_2_I;
            }
            -l_2_I++;
        }
        return -l_3_I;
    }

    public String cS(String str) {
        Object -l_2_R = this.Pp.a(str, 1);
        return -l_2_R == null ? null : -l_2_R.getAppName();
    }

    public boolean dk(String str) {
        return str != null ? this.Pp.ai(str) : false;
    }

    public void init() {
        this.Ac = true;
    }
}
