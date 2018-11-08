package tmsdkobf;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import tmsdk.common.TMSDKContext;

public class le {
    public static void ep() {
        try {
            long -l_0_J = System.currentTimeMillis() / 1000;
            long -l_2_J = kz.eb();
            if ((-l_0_J >= -l_2_J ? 1 : null) != null) {
                Object<UsageStats> -l_5_R = ((UsageStatsManager) TMSDKContext.getApplicaionContext().getSystemService("usagestats")).queryUsageStats(0, -l_2_J, -l_0_J);
                kz.m(-l_0_J);
                if (-l_5_R != null && -l_5_R.size() > 0) {
                    Object -l_6_R = new JSONArray();
                    for (UsageStats -l_9_R : -l_5_R) {
                        try {
                            if (!-l_9_R.getPackageName().startsWith("com.android")) {
                                if ((-l_9_R.getTotalTimeInForeground() <= 0 ? 1 : null) == null) {
                                    if ((-l_9_R.getLastTimeUsed() > 0 ? 1 : null) != null) {
                                        Object -l_10_R = new JSONObject();
                                        -l_10_R.put("pkgName", -l_9_R.getPackageName());
                                        -l_10_R.put("firstTimeStamp", String.valueOf(-l_9_R.getFirstTimeStamp() / 1000));
                                        -l_10_R.put("lastTimeStamp", String.valueOf(-l_9_R.getLastTimeStamp() / 1000));
                                        -l_10_R.put("lastTimeUsed", String.valueOf(-l_9_R.getLastTimeUsed() / 1000));
                                        -l_10_R.put("totalTimeInForeground", String.valueOf(-l_9_R.getTotalTimeInForeground() / 1000));
                                        -l_6_R.put(-l_10_R);
                                    }
                                }
                            }
                        } catch (Throwable th) {
                        }
                    }
                    if (-l_6_R.length() > 0) {
                        la.a(-l_6_R.toString(), getPath(), 150);
                    }
                }
            }
        } catch (Throwable th2) {
        }
    }

    public static void eq() {
        try {
            final Object -l_0_R = getPath();
            Object -l_1_R = la.bD(-l_0_R);
            if (-l_1_R == null || -l_1_R.isEmpty()) {
                la.b(150, 1001, "");
                return;
            }
            JceStruct -l_2_R = new ao(150, new ArrayList());
            Object -l_3_R = -l_1_R.iterator();
            while (-l_3_R.hasNext()) {
                try {
                    Object -l_5_R = new JSONArray((String) -l_3_R.next());
                    int -l_6_I = -l_5_R.length();
                    for (int -l_7_I = 0; -l_7_I < -l_6_I; -l_7_I++) {
                        JSONObject -l_8_R = (JSONObject) -l_5_R.get(-l_7_I);
                        Object -l_9_R = new ap(new HashMap());
                        -l_9_R.bG.put(Integer.valueOf(6), -l_8_R.getString("pkgName"));
                        -l_9_R.bG.put(Integer.valueOf(7), -l_8_R.getString("firstTimeStamp"));
                        -l_9_R.bG.put(Integer.valueOf(8), -l_8_R.getString("lastTimeStamp"));
                        -l_9_R.bG.put(Integer.valueOf(9), -l_8_R.getString("lastTimeUsed"));
                        -l_9_R.bG.put(Integer.valueOf(10), -l_8_R.getString("totalTimeInForeground"));
                        -l_2_R.bD.add(-l_9_R);
                    }
                } catch (Throwable th) {
                }
            }
            -l_3_R = im.bK();
            if (-l_2_R.bD.size() > 0 && -l_3_R != null) {
                -l_3_R.a(4060, -l_2_R, null, 0, new jy() {
                    public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                        if (i3 == 0 && i4 == 0) {
                            la.bF(-l_0_R);
                            kz.n(System.currentTimeMillis() / 1000);
                        }
                    }
                });
            }
        } catch (Throwable th2) {
        }
    }

    public static String getPath() {
        return TMSDKContext.getApplicaionContext().getFilesDir().getAbsolutePath() + File.separator + "d_" + 150;
    }
}
