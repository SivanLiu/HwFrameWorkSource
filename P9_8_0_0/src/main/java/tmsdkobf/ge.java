package tmsdkobf;

import tmsdk.common.TMSDKContext;
import tmsdk.common.utils.f;
import tmsdk.common.utils.l;

public class ge {
    public boolean R() {
        String -l_2_R;
        f.d("ImsiChecker", "isImsiChanged [Beg]");
        Object -l_1_R = new md("imsiInfo");
        String -l_3_R = null;
        int -l_4_I = 0;
        Object -l_5_R = im.bO();
        if (-l_5_R != null) {
            -l_4_I = 1;
            -l_2_R = -l_5_R.getIMSI(0);
            -l_3_R = -l_5_R.getIMSI(1);
        } else {
            -l_2_R = l.M(TMSDKContext.getApplicaionContext());
        }
        if (-l_2_R == null) {
            -l_2_R = "";
        }
        if (-l_3_R == null) {
            -l_3_R = "";
        }
        Object -l_6_R = -l_1_R.getString("IMSI1", "");
        Object -l_7_R = -l_1_R.getString("IMSI2", "");
        -l_1_R.a("IMSI1", -l_2_R, false);
        if (-l_4_I != 0) {
            -l_1_R.a("IMSI2", -l_3_R, false);
        }
        if (-l_1_R.getBoolean("IS_FIRST", true)) {
            -l_1_R.a("IS_FIRST", false, false);
            f.d("ImsiChecker", "isImsiChanged [End][First--ture]");
            return false;
        }
        f.d("ImsiChecker", "isImsiChanged-lastImsi:[" + -l_6_R + "][" + -l_7_R + "]");
        f.d("ImsiChecker", "isImsiChanged-currImsi:[" + -l_2_R + "][" + -l_3_R + "]");
        if (-l_6_R.compareTo(-l_2_R) == 0) {
            if (-l_4_I != 0) {
                if (-l_7_R.compareTo(-l_3_R) == 0) {
                }
            }
            f.d("ImsiChecker", "isImsiChanged [End][false]");
            return false;
        }
        f.d("ImsiChecker", "isImsiChanged [End][true]");
        return true;
    }
}
