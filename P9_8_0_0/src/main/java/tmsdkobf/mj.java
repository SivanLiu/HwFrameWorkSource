package tmsdkobf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import tmsdkobf.im.a;

public final class mj implements a {
    private static volatile mj zQ;
    private static final String[] zR = new String[]{"phone", "phone1", "phone2", "phoneEX"};
    private List<ly> zS = new ArrayList();
    private List zT = new ArrayList(2);

    private mj() {
        eP();
    }

    public static synchronized mj eO() {
        mj mjVar;
        synchronized (mj.class) {
            if (zQ == null) {
                zQ = new mj();
            }
            mjVar = zQ;
        }
        return mjVar;
    }

    private boolean eP() {
        if (this.zS.size() == 0) {
            synchronized (this.zS) {
                if (this.zS.size() == 0) {
                    for (String -l_4_R : eQ()) {
                        this.zS.add(new lz(-l_4_R));
                    }
                }
            }
        }
        return this.zS.size() > 0;
    }

    public static final List<String> eQ() {
        Object -l_1_R = null;
        Object -l_0_R = im.rE;
        if (-l_0_R != null) {
            -l_1_R = -l_0_R.ip();
        }
        if (-l_1_R == null) {
            -l_1_R = Arrays.asList(zR);
        }
        Object -l_2_R = new ArrayList();
        for (String -l_4_R : -l_1_R) {
            if (mi.checkService(-l_4_R) != null) {
                -l_2_R.add(-l_4_R);
            }
        }
        return -l_2_R;
    }

    public boolean endCall() {
        Object -l_1_R = im.rE;
        if (!eP()) {
            return false;
        }
        int -l_2_I = 0;
        if (-l_1_R != null && -l_1_R.iu()) {
            for (ly -l_4_R : this.zS) {
                if (-l_4_R.aP(0)) {
                    -l_2_I = 1;
                }
                if (-l_4_R.aP(1)) {
                    -l_2_I = 1;
                }
            }
        } else {
            for (ly endCall : this.zS) {
                if (endCall.endCall()) {
                    -l_2_I = 1;
                }
            }
        }
        return -l_2_I;
    }
}
