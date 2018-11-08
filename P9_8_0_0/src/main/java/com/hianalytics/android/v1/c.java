package com.hianalytics.android.v1;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Build.VERSION;
import android.telephony.TelephonyManager;
import android.util.Base64;
import com.hianalytics.android.a.a.a;
import com.hianalytics.android.a.a.b;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class c implements Runnable {
    boolean a;
    private Context b;
    private JSONObject c;

    public c(Context context, JSONObject jSONObject, boolean z) {
        this.b = context;
        this.c = jSONObject;
        this.a = z;
    }

    private String a(byte[] bArr) {
        Object -l_3_R;
        Exception e;
        Object -l_2_R = new SecureRandom();
        Object -l_3_R2 = String.valueOf(System.currentTimeMillis());
        int -l_4_I = -l_3_R2.length();
        if (-l_4_I < 13) {
            -l_3_R = new StringBuffer(-l_3_R2);
            for (int -l_5_I = 0; -l_5_I < 13 - -l_4_I; -l_5_I++) {
                -l_3_R.append("0");
            }
            -l_3_R2 = -l_3_R.toString();
        } else if (-l_4_I > 13) {
            -l_3_R2 = -l_3_R2.substring(0, 13);
        }
        -l_2_R = new StringBuilder(String.valueOf(-l_3_R2)).append(String.format("%03d", new Object[]{Integer.valueOf(-l_2_R.nextInt(999))})).toString();
        try {
            byte[] a = b.a(-l_2_R, bArr);
            -l_2_R = -l_2_R.getBytes("UTF-8");
            -l_3_R2 = Base64.decode("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDf5raDExuuXbsVNCWl48yuB89W\rfNOuuhPuS2Mptii/0UorpzypBkNTTGt11E7aorCc1lFwlB+4KDMIpFyQsdChSk+A\rt9UfhFKa95uiDpMe5rMfU+DAhoXGER6WQ2qGtrHmBWVv33i3lc76u9IgEfYuLwC6\r1mhQDHzAKPiViY6oeQIDAQAB\r", 0);
            Object -l_4_R = KeyFactory.getInstance("RSA");
            -l_3_R = new X509EncodedKeySpec(-l_3_R2);
            try {
                RSAPublicKey -l_3_R3 = (RSAPublicKey) -l_4_R.generatePublic(-l_3_R);
                -l_4_R = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                -l_4_R.init(1, -l_3_R3);
                byte[] -l_2_R2 = -l_4_R.doFinal(-l_2_R);
                return "{\"vs\":\"" + a.e(this.b) + "\",\"ed\":\"" + a.b(a) + "\",\"ek\":\"" + a.b(-l_2_R2) + "\"}";
            } catch (Exception e2) {
                e = e2;
                -l_3_R2 = -l_3_R;
                e.printStackTrace();
                return null;
            }
        } catch (Exception e3) {
            e = e3;
            e.printStackTrace();
            return null;
        }
    }

    private boolean a(JSONObject jSONObject, String str) {
        Object -l_3_R = str.toLowerCase();
        try {
            byte[] a = a.a(jSONObject.toString().getBytes("UTF-8"));
            if (a == null) {
                return false;
            }
            String a2 = a(a);
            if (a2 == null) {
                return false;
            }
            try {
                a = a2.getBytes("UTF-8");
                if (-l_3_R.indexOf("https") >= 0) {
                    return false;
                }
                a.h();
                return b.a(str, a);
            } catch (UnsupportedEncodingException e) {
                "UnsupportedEncodingException:" + e.getMessage();
                a.h();
                return false;
            }
        } catch (UnsupportedEncodingException e2) {
            "UnsupportedEncodingException:" + e2.getMessage();
            a.h();
            return false;
        }
    }

    public final void run() {
        JSONException e;
        try {
            if (this.c.getString("type") != null) {
                String -l_8_R;
                Object -l_5_R;
                Context -l_2_R = this.b;
                Object -l_3_R = this.c;
                int -l_4_I = this.a;
                c -l_1_R = this;
                Context -l_5_R2 = -l_2_R;
                Object -l_6_R = new StringBuffer("1.0");
                String -l_7_R = a.a(-l_5_R2);
                TelephonyManager -l_8_R2 = (TelephonyManager) -l_5_R2.getSystemService("phone");
                if (-l_8_R2 != null) {
                    Configuration -l_9_R = -l_5_R2.getResources().getConfiguration();
                    Object -l_10_R = "";
                    if (!(-l_9_R == null || -l_9_R.locale == null)) {
                        -l_10_R = -l_9_R.locale.toString();
                    }
                    String -l_9_R2 = "";
                    if (a.a(-l_5_R2, "android.permission.READ_PHONE_STATE")) {
                        -l_9_R2 = a.b(-l_8_R2.getDeviceId());
                    }
                    -l_8_R = a.e(-l_5_R2);
                    if (a.f(-l_5_R2)) {
                        -l_6_R.append(",").append("Android" + VERSION.RELEASE).append(",").append(-l_10_R).append(",").append(Build.MODEL).append(",").append(Build.DISPLAY).append(",").append(-l_8_R).append(",").append(-l_9_R2).append(",").append(-l_7_R).append(",").append(a.b(-l_5_R2));
                        a.h();
                    } else {
                        -l_6_R.append(",,,,,").append(-l_8_R).append(",").append(-l_9_R2).append(",").append(-l_7_R).append(",");
                        a.h();
                    }
                    -l_5_R = -l_6_R.toString();
                } else {
                    a.h();
                    -l_5_R = null;
                }
                if (-l_5_R != null) {
                    -l_6_R = com.hianalytics.android.a.a.c.b(-l_2_R, "cached");
                    JSONObject -l_7_R2 = new JSONObject();
                    try {
                        -l_8_R = -l_3_R.getString("type");
                        if (-l_8_R != null) {
                            JSONArray -l_9_R3;
                            -l_3_R.remove("type");
                            int -l_10_I = 1;
                            if (-l_6_R == null) {
                                Object -l_6_R2 = new JSONObject();
                                try {
                                    -l_9_R3 = new JSONArray();
                                    -l_6_R = -l_6_R2;
                                } catch (JSONException e2) {
                                    e = e2;
                                    -l_6_R = -l_6_R2;
                                    e.printStackTrace();
                                    com.hianalytics.android.a.a.c.c(-l_2_R, "cached");
                                    return;
                                }
                            } else if (-l_6_R.isNull(-l_8_R)) {
                                -l_9_R3 = new JSONArray();
                            } else {
                                -l_10_I = 0;
                                -l_9_R3 = -l_6_R.getJSONArray(-l_8_R);
                            }
                            if (-l_4_I == 0 || -l_10_I == 0) {
                                if (-l_4_I == 0) {
                                    -l_9_R3.put(-l_3_R);
                                }
                                Object -l_3_R2 = new JSONArray();
                                try {
                                    -l_4_I = -l_9_R3.length();
                                    for (-l_10_I = 0; -l_10_I <= -l_4_I - 1; -l_10_I++) {
                                        Object -l_11_R = -l_9_R3.getJSONObject(-l_10_I);
                                        Object -l_12_R;
                                        if (-l_11_R.has("b")) {
                                            -l_12_R = -l_11_R.getJSONArray("b");
                                            if (-l_12_R != null && -l_12_R.length() > 0) {
                                                -l_12_R = -l_12_R.getString(-l_12_R.length() - 1).split(",");
                                                if ((((System.currentTimeMillis() / 1000) - a.a(-l_12_R[1])) - Long.parseLong(-l_12_R[2]) >= a.b().longValue() ? 1 : null) == null) {
                                                    -l_3_R2.put(-l_11_R);
                                                } else {
                                                    a.h();
                                                }
                                            }
                                        } else if (-l_11_R.has("e")) {
                                            -l_12_R = -l_11_R.getJSONArray("e");
                                            if (-l_12_R != null && -l_12_R.length() > 0) {
                                                if (((System.currentTimeMillis() / 1000) - a.a(-l_12_R.getString(-l_12_R.length() + -1).split(",")[2]) >= a.b().longValue() ? 1 : null) == null) {
                                                    -l_3_R2.put(-l_11_R);
                                                } else {
                                                    a.h();
                                                }
                                            }
                                        }
                                    }
                                    if (-l_3_R2.length() > 0) {
                                        -l_6_R.remove(-l_8_R);
                                        -l_6_R.put(-l_8_R, -l_3_R2);
                                        -l_7_R2.put("g", -l_5_R);
                                        -l_7_R2.put("s", -l_3_R2);
                                        "message=" + -l_7_R2.toString();
                                        a.h();
                                        if (a(-l_7_R2, a.i())) {
                                            Object -l_8_R3 = com.hianalytics.android.a.a.c.a(-l_2_R, "flag");
                                            if (a.f(-l_2_R)) {
                                                Editor -l_9_R4 = -l_8_R3.edit();
                                                -l_9_R4.putString("rom_version", Build.DISPLAY);
                                                -l_9_R4.commit();
                                            }
                                            com.hianalytics.android.a.a.c.c(-l_2_R, "cached");
                                            a.h();
                                            return;
                                        }
                                        com.hianalytics.android.a.a.c.a(-l_2_R, -l_6_R, "cached");
                                        a.h();
                                        return;
                                    }
                                    a.h();
                                    return;
                                } catch (JSONException e3) {
                                    e = e3;
                                    -l_3_R = -l_3_R2;
                                }
                            } else {
                                a.h();
                                return;
                            }
                        }
                        return;
                    } catch (JSONException e4) {
                        e = e4;
                        e.printStackTrace();
                        com.hianalytics.android.a.a.c.c(-l_2_R, "cached");
                        return;
                    }
                }
                a.h();
            }
        } catch (Object -l_1_R2) {
            "MessageThread.run() throw exception:" + -l_1_R2.getMessage();
            a.h();
            -l_1_R2.printStackTrace();
            com.hianalytics.android.a.a.c.c(this.b, "cached");
        }
    }
}
