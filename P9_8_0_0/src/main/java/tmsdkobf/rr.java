package tmsdkobf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.OfflineVideo;

public class rr extends rn {
    private OfflineVideo du(String str) {
        long -l_2_J = rq.dq(str);
        if (-l_2_J == 0) {
            return null;
        }
        Object -l_4_R = new OfflineVideo();
        -l_4_R.mPath = str;
        -l_4_R.mSize = -l_2_J;
        -l_4_R.mTitle = rh.di(str);
        -l_4_R.mThumnbailPath = dv(str);
        return -l_4_R;
    }

    private String dv(String str) {
        try {
            Object -l_2_R = new File(str).list();
            if (-l_2_R != null) {
                Object -l_3_R = -l_2_R;
                for (Object -l_6_R : -l_2_R) {
                    Object -l_7_R = new File(str + "/" + -l_6_R);
                    if (-l_7_R.isDirectory()) {
                        Object -l_8_R = dv(-l_7_R.getAbsolutePath());
                        if (-l_8_R != null) {
                            return -l_8_R;
                        }
                    } else if (-l_6_R.endsWith(".db") || -l_6_R.endsWith("tmv")) {
                        return -l_7_R.getAbsolutePath();
                    }
                }
            }
        } catch (Error e) {
        }
        return null;
    }

    public List<OfflineVideo> a(ro roVar) {
        Object<String> -l_2_R = rq.dp(roVar.Ok);
        if (-l_2_R == null || -l_2_R.size() == 0) {
            return null;
        }
        Object -l_3_R = new ArrayList();
        for (String -l_5_R : -l_2_R) {
            Object -l_6_R = du(-l_5_R);
            if (-l_6_R != null) {
                -l_3_R.add(-l_6_R);
            }
        }
        if (-l_3_R.size() == 0) {
            -l_3_R = null;
        }
        return -l_3_R;
    }
}
