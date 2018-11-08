package tmsdkobf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.OfflineVideo;
import tmsdk.common.module.update.UpdateConfig;

public class rs extends rn {
    private OfflineVideo dw(String str) {
        Object obj = null;
        Object -l_2_R = rh.di(str);
        Object -l_3_R = new OfflineVideo();
        -l_3_R.mPath = str;
        -l_3_R.mSize = rq.dq(str);
        if (-l_3_R.mSize >= UpdateConfig.UPDATE_FLAG_PERMIS_MONITOR_LIST) {
            obj = 1;
        }
        if (obj == null) {
            return null;
        }
        Object<String> -l_4_R = rq.dr(str + "/" + -l_2_R + ".qiyicfg");
        if (-l_4_R == null || -l_4_R.size() == 0) {
            -l_3_R.mThumnbailPath = e(-l_3_R);
            -l_3_R.mTitle = rh.dj(-l_3_R.mThumnbailPath);
            return -l_3_R;
        }
        for (String -l_6_R : -l_4_R) {
            if (-l_6_R.startsWith("progress=")) {
                try {
                    -l_3_R.mDownProgress = (int) Float.parseFloat(-l_6_R.substring("progress=".length()));
                } catch (Exception e) {
                    -l_3_R.mDownProgress = -1;
                }
            } else if (-l_6_R.startsWith("text=")) {
                -l_3_R.mTitle = rq.ds(-l_6_R.substring("text=".length()));
            } else if (-l_6_R.startsWith("imgUrl=")) {
                String str2 = "files/.*";
                -l_3_R.mThumnbailPath = -l_3_R.mPath.replaceFirst(str2, "cache/images/default/" + rq.dt(-l_6_R.substring("imgUrl=".length()).replaceAll("\\\\", "")) + ".r");
                if (!new File(-l_3_R.mThumnbailPath).exists()) {
                    -l_3_R.mThumnbailPath = e(-l_3_R);
                }
            }
        }
        if (-l_3_R.mThumnbailPath == null) {
            -l_3_R.mThumnbailPath = e(-l_3_R);
        }
        return -l_3_R;
    }

    private String e(OfflineVideo offlineVideo) {
        Object -l_2_R = new File(offlineVideo.mPath).list();
        if (-l_2_R != null) {
            Object -l_3_R = -l_2_R;
            for (Object -l_6_R : -l_2_R) {
                if (-l_6_R.endsWith(".f4v") || -l_6_R.endsWith(".mp4")) {
                    Object -l_7_R = offlineVideo.mPath + "/" + -l_6_R;
                    if ((new File(-l_7_R).length() <= UpdateConfig.UPDATE_FLAG_PERMIS_MONITOR_LIST ? 1 : null) == null) {
                        return -l_7_R;
                    }
                }
            }
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
            Object -l_6_R = dw(-l_5_R);
            if (-l_6_R != null) {
                -l_3_R.add(-l_6_R);
            }
        }
        return -l_3_R;
    }
}
