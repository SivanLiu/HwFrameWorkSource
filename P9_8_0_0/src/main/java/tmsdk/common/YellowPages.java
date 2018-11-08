package tmsdk.common;

import android.content.Context;
import android.text.TextUtils;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.module.update.UpdateManager;
import tmsdk.common.utils.f;
import tmsdk.common.utils.r;
import tmsdkobf.ko;
import tmsdkobf.lu;

public class YellowPages {
    private static YellowPages xw;
    private String xx;

    static {
        TMSDKContext.registerNatives(10, YellowPages.class);
    }

    private YellowPages() {
        init(TMSDKContext.getApplicaionContext());
    }

    private String bq(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String -l_2_R = null;
        Object -l_3_R = new AtomicInteger(0);
        Object -l_4_R = new AtomicReference();
        int -l_5_I = nQueryDataByNumberJNI(this.xx, str, -l_3_R, -l_4_R);
        if (-l_5_I == 0) {
            try {
                -l_2_R = new String((byte[]) -l_4_R.get(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
        }
        f.e("yellowPage", "error code::" + -l_5_I);
        return -l_2_R;
    }

    public static YellowPages getInstance() {
        if (xw == null) {
            Object -l_0_R = YellowPages.class;
            synchronized (YellowPages.class) {
                if (xw == null) {
                    xw = new YellowPages();
                }
            }
        }
        return xw;
    }

    private void init(Context context) {
        Object -l_2_R = UpdateConfig.YELLOW_PAGEV2_LARGE;
        if (TextUtils.isEmpty(r.k(context, -l_2_R))) {
            -l_2_R = UpdateConfig.YELLOW_PAGE;
            lu.b(context, -l_2_R, null);
        }
        this.xx = ((UpdateManager) ManagerCreatorC.getManager(UpdateManager.class)).getFileSavePath() + File.separator + -l_2_R;
    }

    private native int nQueryDataByNumberJNI(String str, String str2, AtomicInteger atomicInteger, AtomicReference<byte[]> atomicReference);

    private native int nUpdate(String str, String str2);

    public String query(String str) {
        Object -l_2_R = bq(str);
        if (!TextUtils.isEmpty(-l_2_R)) {
            return -l_2_R;
        }
        Object -l_3_R = ko.aY(str);
        return TextUtils.isEmpty(-l_3_R) ? null : bq(-l_3_R);
    }

    public int update(String str) {
        return nUpdate(this.xx, str);
    }
}
