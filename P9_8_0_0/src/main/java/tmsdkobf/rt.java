package tmsdkobf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.OfflineVideo;

public class rt extends rn {
    private OfflineVideo dw(String str) {
        Object obj = null;
        Object -l_2_R = rh.di(str);
        if (-l_2_R == null) {
            return null;
        }
        long -l_3_J = rq.dq(str);
        if (-l_3_J > 0) {
            obj = 1;
        }
        if (obj == null) {
            return null;
        }
        Object -l_5_R = new OfflineVideo();
        -l_5_R.mPath = str;
        -l_5_R.mSize = -l_3_J;
        -l_5_R.mTitle = -l_2_R;
        Object -l_6_R = new File(str).list();
        if (-l_6_R != null) {
            Object -l_7_R = -l_6_R;
            for (Object -l_10_R : -l_6_R) {
                if (-l_10_R.startsWith(-l_2_R + "_")) {
                    -l_5_R.mThumnbailPath = str + "/" + -l_10_R;
                    break;
                }
            }
        }
        return -l_5_R;
    }

    public List<OfflineVideo> a(ro roVar) {
        Object<String> -l_2_R = rq.dp(roVar.Ok);
        Object -l_3_R = new ArrayList();
        for (String -l_5_R : -l_2_R) {
            Object -l_6_R = dw(-l_5_R);
            if (-l_6_R != null) {
                -l_3_R.add(-l_6_R);
            }
        }
        return -l_3_R;
    }
}
