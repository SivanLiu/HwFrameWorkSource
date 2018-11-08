package tmsdkobf;

import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.intelli_sms.SmsCheckResult;

public class lh {
    public static void c(String str, int i) {
        try {
            Object -l_2_R = new JSONObject();
            -l_2_R.put("number", str);
            -l_2_R.put("time", String.valueOf(System.currentTimeMillis()));
            -l_2_R.put("tag", String.valueOf(i));
            la.a(-l_2_R.toString(), getPath(), SmsCheckResult.ESCT_146);
        } catch (Throwable th) {
        }
    }

    public static void eq() {
        try {
            final Object -l_0_R = getPath();
            Object -l_1_R = la.bD(-l_0_R);
            if (-l_1_R != null && !-l_1_R.isEmpty()) {
                JceStruct -l_2_R = new ao(SmsCheckResult.ESCT_146, new ArrayList());
                Object -l_4_R = -l_1_R.iterator();
                while (-l_4_R.hasNext()) {
                    Object -l_6_R = new JSONObject((String) -l_4_R.next());
                    Object -l_7_R = new ap(new HashMap());
                    -l_7_R.bG.put(Integer.valueOf(6), -l_6_R.getString("number"));
                    -l_7_R.bG.put(Integer.valueOf(7), -l_6_R.getString("time"));
                    -l_7_R.bG.put(Integer.valueOf(8), -l_6_R.getString("tag"));
                    -l_2_R.bD.add(-l_7_R);
                }
                -l_4_R = im.bK();
                if (-l_2_R.bD.size() > 0 && -l_4_R != null) {
                    -l_4_R.a(4060, -l_2_R, null, 0, new jy() {
                        public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                            if (i3 == 0 && i4 == 0) {
                                la.bF(-l_0_R);
                                kz.l(System.currentTimeMillis() / 1000);
                            }
                        }
                    });
                }
            }
        } catch (Throwable th) {
        }
    }

    public static String getPath() {
        return TMSDKContext.getApplicaionContext().getFilesDir().getAbsolutePath() + File.separator + "d_" + SmsCheckResult.ESCT_146;
    }
}
