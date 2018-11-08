package tmsdkobf;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import tmsdk.common.TMSDKContext;

public class lf {
    static long yg = -1;

    private static int a(ScanResult scanResult) {
        return (scanResult == null || scanResult.capabilities == null) ? -1 : !scanResult.capabilities.contains("WEP") ? !scanResult.capabilities.contains("PSK") ? !scanResult.capabilities.contains("EAP") ? 0 : 3 : 2 : 1;
    }

    public static synchronized void bG(String str) {
        synchronized (lf.class) {
            if (TextUtils.isEmpty(str)) {
                return;
            }
            try {
                Object -l_2_R;
                int -l_3_I;
                JSONObject -l_4_R;
                Object -l_5_R;
                long -l_6_J;
                Object -l_4_R2;
                JceStruct -l_6_R;
                Object -l_7_R;
                Object -l_8_R;
                Object -l_1_R = la.bD(getPath());
                if (-l_1_R != null) {
                    if (-l_1_R.size() > 0) {
                        -l_2_R = new JSONArray((String) -l_1_R.get(0));
                        while (-l_3_I < -l_2_R.length()) {
                            -l_4_R = (JSONObject) -l_2_R.get(-l_3_I);
                            -l_5_R = -l_4_R.getString("bssid");
                            -l_6_J = -l_4_R.getLong("curr_time");
                            if (-l_5_R.compareToIgnoreCase(str) == 0) {
                            } else {
                                if ((System.currentTimeMillis() - -l_6_J < 86400000 ? 1 : null) != null) {
                                    return;
                                }
                                -l_4_R.put("curr_time", System.currentTimeMillis());
                                if (-l_3_I >= -l_2_R.length()) {
                                    -l_4_R2 = new JSONObject();
                                    -l_4_R2.put("bssid", str);
                                    -l_4_R2.put("curr_time", System.currentTimeMillis());
                                    -l_2_R.put(-l_4_R2);
                                }
                                -l_4_R2 = -l_2_R;
                                -l_5_R = eu();
                                if (TextUtils.isEmpty(-l_5_R)) {
                                    -l_6_R = new ao(90, new ArrayList());
                                    -l_7_R = new ap(new HashMap());
                                    -l_7_R.bG.put(Integer.valueOf(1), String.valueOf(7));
                                    -l_7_R.bG.put(Integer.valueOf(2), String.valueOf(System.currentTimeMillis()));
                                    -l_7_R.bG.put(Integer.valueOf(3), String.valueOf(-l_5_R));
                                    -l_7_R.bG.put(Integer.valueOf(9), String.valueOf(yg));
                                    -l_6_R.bD.add(-l_7_R);
                                    -l_8_R = im.bK();
                                    if (-l_6_R.bD.size() > 0 && -l_8_R != null) {
                                        -l_8_R.a(4060, -l_6_R, null, 0, new jy() {
                                            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                                                if (i3 == 0 && i4 == 0) {
                                                    try {
                                                        la.a(-l_4_R2.toString(), lf.getPath(), 90);
                                                    } catch (Throwable th) {
                                                    }
                                                }
                                            }
                                        });
                                    }
                                } else {
                                    return;
                                }
                            }
                        }
                        if (-l_3_I >= -l_2_R.length()) {
                            -l_4_R2 = new JSONObject();
                            -l_4_R2.put("bssid", str);
                            -l_4_R2.put("curr_time", System.currentTimeMillis());
                            -l_2_R.put(-l_4_R2);
                        }
                        -l_4_R2 = -l_2_R;
                        -l_5_R = eu();
                        if (TextUtils.isEmpty(-l_5_R)) {
                            return;
                        }
                        -l_6_R = new ao(90, new ArrayList());
                        -l_7_R = new ap(new HashMap());
                        -l_7_R.bG.put(Integer.valueOf(1), String.valueOf(7));
                        -l_7_R.bG.put(Integer.valueOf(2), String.valueOf(System.currentTimeMillis()));
                        -l_7_R.bG.put(Integer.valueOf(3), String.valueOf(-l_5_R));
                        -l_7_R.bG.put(Integer.valueOf(9), String.valueOf(yg));
                        -l_6_R.bD.add(-l_7_R);
                        -l_8_R = im.bK();
                        -l_8_R.a(4060, -l_6_R, null, 0, /* anonymous class already generated */);
                    }
                }
                -l_2_R = new JSONArray();
                for (-l_3_I = 0; -l_3_I < -l_2_R.length(); -l_3_I++) {
                    -l_4_R = (JSONObject) -l_2_R.get(-l_3_I);
                    -l_5_R = -l_4_R.getString("bssid");
                    -l_6_J = -l_4_R.getLong("curr_time");
                    if (-l_5_R.compareToIgnoreCase(str) == 0) {
                        if (System.currentTimeMillis() - -l_6_J < 86400000) {
                        }
                        if ((System.currentTimeMillis() - -l_6_J < 86400000 ? 1 : null) != null) {
                            -l_4_R.put("curr_time", System.currentTimeMillis());
                            if (-l_3_I >= -l_2_R.length()) {
                                -l_4_R2 = new JSONObject();
                                -l_4_R2.put("bssid", str);
                                -l_4_R2.put("curr_time", System.currentTimeMillis());
                                -l_2_R.put(-l_4_R2);
                            }
                            -l_4_R2 = -l_2_R;
                            -l_5_R = eu();
                            if (TextUtils.isEmpty(-l_5_R)) {
                                -l_6_R = new ao(90, new ArrayList());
                                -l_7_R = new ap(new HashMap());
                                -l_7_R.bG.put(Integer.valueOf(1), String.valueOf(7));
                                -l_7_R.bG.put(Integer.valueOf(2), String.valueOf(System.currentTimeMillis()));
                                -l_7_R.bG.put(Integer.valueOf(3), String.valueOf(-l_5_R));
                                -l_7_R.bG.put(Integer.valueOf(9), String.valueOf(yg));
                                -l_6_R.bD.add(-l_7_R);
                                -l_8_R = im.bK();
                                -l_8_R.a(4060, -l_6_R, null, 0, /* anonymous class already generated */);
                            } else {
                                return;
                            }
                        }
                        return;
                    }
                }
                if (-l_3_I >= -l_2_R.length()) {
                    -l_4_R2 = new JSONObject();
                    -l_4_R2.put("bssid", str);
                    -l_4_R2.put("curr_time", System.currentTimeMillis());
                    -l_2_R.put(-l_4_R2);
                }
                -l_4_R2 = -l_2_R;
                -l_5_R = eu();
                if (TextUtils.isEmpty(-l_5_R)) {
                    return;
                }
                -l_6_R = new ao(90, new ArrayList());
                -l_7_R = new ap(new HashMap());
                -l_7_R.bG.put(Integer.valueOf(1), String.valueOf(7));
                -l_7_R.bG.put(Integer.valueOf(2), String.valueOf(System.currentTimeMillis()));
                -l_7_R.bG.put(Integer.valueOf(3), String.valueOf(-l_5_R));
                -l_7_R.bG.put(Integer.valueOf(9), String.valueOf(yg));
                -l_6_R.bD.add(-l_7_R);
                -l_8_R = im.bK();
                -l_8_R.a(4060, -l_6_R, null, 0, /* anonymous class already generated */);
            } catch (Throwable th) {
            }
        }
    }

    private static String eu() {
        try {
            Object<ScanResult> -l_1_R = ((WifiManager) TMSDKContext.getApplicaionContext().getApplicationContext().getSystemService("wifi")).getScanResults();
            if (-l_1_R == null || -l_1_R.size() <= 0) {
                return null;
            }
            Object -l_2_R = new JSONArray();
            int -l_3_I = 0;
            for (ScanResult -l_5_R : -l_1_R) {
                if (-l_3_I > 50) {
                    break;
                }
                Object -l_6_R = new JSONObject();
                try {
                    -l_6_R.putOpt("bssid", -l_5_R.BSSID);
                    -l_6_R.putOpt("ssid", -l_5_R.SSID);
                    -l_6_R.putOpt("secureType", Integer.valueOf(a(-l_5_R)));
                } catch (Throwable th) {
                }
                -l_2_R.put(-l_6_R);
                -l_3_I++;
            }
            return -l_2_R.toString();
        } catch (Throwable th2) {
            return null;
        }
    }

    public static String getPath() {
        return TMSDKContext.getApplicaionContext().getFilesDir().getAbsolutePath() + File.separator + "d_" + String.valueOf(90);
    }
}
