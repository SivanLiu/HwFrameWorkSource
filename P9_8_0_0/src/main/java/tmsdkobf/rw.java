package tmsdkobf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import tmsdk.common.OfflineVideo;

public class rw extends rn {
    Pattern PZ = Pattern.compile("\"title\":\"([^\"]*)\"");
    Pattern Qa = Pattern.compile("\"progress\":([0-9]{1,3})");
    Pattern Qb = Pattern.compile("\"seconds\":([0-9]{1,3})");
    Pattern Qc = Pattern.compile("\"playTime\":([0-9]{1,3})");

    private OfflineVideo dw(String str) {
        Object -l_2_R = new OfflineVideo();
        -l_2_R.mPath = str;
        if (new File(str + "/1.png").exists()) {
            -l_2_R.mThumnbailPath = str + "/1.png";
        }
        Object -l_3_R = rq.dr(str + "/info");
        if (-l_3_R == null || -l_3_R.size() == 0) {
            return null;
        }
        String -l_4_R = (String) -l_3_R.get(0);
        -l_2_R.mTitle = rq.b(-l_4_R, this.PZ);
        -l_2_R.mDownProgress = rq.a(-l_4_R, this.Qa);
        int -l_5_I = rq.a(-l_4_R, this.Qc);
        int -l_6_I = rq.a(-l_4_R, this.Qb);
        -l_2_R.mPlayProgress = -l_6_I <= 0 ? -1 : (-l_5_I * 100) / -l_6_I;
        -l_2_R.mSize = rq.dq(str);
        return -l_2_R;
    }

    public List<OfflineVideo> a(ro roVar) {
        if (roVar.Ok == null) {
            return null;
        }
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
