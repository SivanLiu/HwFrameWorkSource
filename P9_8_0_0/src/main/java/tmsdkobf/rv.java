package tmsdkobf;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.update.UpdateConfig;

public class rv {
    private static ro a(dz dzVar) {
        Object -l_1_R = new ro();
        -l_1_R.Ok = dzVar.iv;
        if (!TextUtils.isEmpty(dzVar.iw)) {
            Object -l_2_R = dzVar.iw.split("&");
            if (-l_2_R != null) {
                Object -l_3_R = -l_2_R;
                for (Object -l_6_R : -l_2_R) {
                    if (-l_6_R.length() > 2) {
                        int -l_7_I = -l_6_R.charAt(0);
                        Object -l_8_R = -l_6_R.substring(2);
                        switch (-l_7_I) {
                            case 49:
                                -l_1_R.mFileName = -l_8_R;
                                break;
                            case 50:
                                -l_1_R.Ol = -l_8_R;
                                break;
                            case 51:
                                -l_1_R.Om = -l_8_R;
                                break;
                            case 52:
                                -l_1_R.On = -l_8_R;
                                break;
                            case 53:
                                -l_1_R.Oo = -l_8_R;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(dzVar.ix)) {
            -l_1_R.mPlayers = dzVar.ix.split("&");
        }
        if (!TextUtils.isEmpty(dzVar.iy)) {
            -l_1_R.mAdapter = dzVar.iy;
        }
        return -l_1_R;
    }

    private static void a(List<ro> list, ro roVar) {
        if (TextUtils.isEmpty(roVar.Ok)) {
            list.add(roVar);
            return;
        }
        int -l_2_I = 0;
        while (-l_2_I < list.size() && !TextUtils.isEmpty(((ro) list.get(-l_2_I)).Ok)) {
            -l_2_I++;
        }
        list.add(-l_2_I, roVar);
    }

    public static List<ro> kp() {
        ea -l_0_R = (ea) mk.b(TMSDKContext.getApplicaionContext(), UpdateConfig.PROCESSMANAGER_WHITE_LIST_NAME, UpdateConfig.intToString(40006), new ea(), "UTF-8");
        Object -l_1_R = new ArrayList();
        if (-l_0_R == null || -l_0_R.iC == null) {
            return -l_1_R;
        }
        Object -l_2_R = -l_0_R.iC.iterator();
        while (-l_2_R.hasNext()) {
            dz -l_3_R = (dz) -l_2_R.next();
            if (-l_3_R.iu != null) {
                try {
                    switch (Integer.valueOf(-l_3_R.iu).intValue()) {
                        case 5:
                            Object -l_5_R = a(-l_3_R);
                            if (-l_5_R == null) {
                                break;
                            }
                            a(-l_1_R, -l_5_R);
                            break;
                        default:
                            break;
                    }
                } catch (Object -l_4_R) {
                    -l_4_R.printStackTrace();
                }
            }
        }
        return -l_1_R;
    }
}
