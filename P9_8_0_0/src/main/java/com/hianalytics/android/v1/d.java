package com.hianalytics.android.v1;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.hianalytics.android.a.a.a;
import com.hianalytics.android.a.a.c;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public final class d implements Runnable {
    private Context a;
    private int b;
    private long c;

    public d(Context context, int i, long j) {
        this.a = context;
        this.b = i;
        this.c = j;
    }

    private static void a(Context context, SharedPreferences sharedPreferences, long j) {
        Editor edit = sharedPreferences.edit();
        String valueOf = String.valueOf(new StringBuilder(String.valueOf(j)).append(a.b(((TelephonyManager) context.getSystemService("phone")).getDeviceId())).toString());
        edit.remove("session_id");
        edit.remove("refer_id");
        edit.putString("session_id", valueOf);
        edit.putString("refer_id", "");
        edit.putLong("end_millis", j);
        edit.commit();
    }

    private void a(SharedPreferences sharedPreferences) {
        Editor edit = sharedPreferences.edit();
        edit.putLong("last_millis", this.c);
        edit.commit();
    }

    private void b(SharedPreferences -l_3_R) {
        Object -l_10_R;
        Object -l_6_R;
        d -l_2_R = this;
        int -l_4_I = 1;
        Object -l_5_R = new JSONObject();
        Context -l_6_R2 = this.a;
        Object -l_7_R = new StringBuffer("");
        SharedPreferences -l_8_R = c.a(-l_6_R2, "sessioncontext");
        String -l_9_R = -l_8_R.getString("session_id", "");
        if ("".equals(-l_9_R)) {
            Object -l_11_R = a.b(((TelephonyManager) -l_6_R2.getSystemService("phone")).getDeviceId());
            long -l_12_J = System.currentTimeMillis();
            -l_9_R = String.valueOf(new StringBuilder(String.valueOf(-l_12_J)).append(-l_11_R).toString());
            -l_10_R = -l_8_R.edit();
            -l_10_R.putString("session_id", -l_9_R);
            -l_10_R.putLong("end_millis", -l_12_J);
            -l_10_R.commit();
        }
        -l_10_R = -l_8_R.getString("refer_id", "");
        TelephonyManager -l_11_R2 = (TelephonyManager) -l_6_R2.getSystemService("phone");
        if (-l_11_R2 != null) {
            -l_7_R.append(a.c(-l_6_R2)[0]).append(",").append(-l_11_R2.getNetworkOperatorName().replace(',', '&')).append(",").append(-l_9_R).append(",").append(-l_10_R);
            -l_6_R = -l_7_R.toString();
        } else {
            a.h();
            -l_6_R = null;
        }
        if (-l_6_R != null) {
            try {
                Object -l_4_R;
                if (-l_3_R.getString("activities", "").trim().length() > 0) {
                    -l_7_R = -l_3_R.getString("activities", "").split(";");
                    -l_4_R = new JSONArray();
                    for (Object put : -l_7_R) {
                        -l_4_R.put(put);
                    }
                    -l_5_R.put("b", -l_4_R);
                    -l_4_I = 0;
                }
                if (-l_3_R.getString("events", "").trim().length() > 0) {
                    -l_7_R = -l_3_R.getString("events", "").split(";");
                    -l_4_R = new JSONArray();
                    for (Object put2 : -l_7_R) {
                        -l_4_R.put(put2);
                    }
                    -l_5_R.put("e", -l_4_R);
                    -l_4_I = 0;
                }
                -l_5_R.put("h", -l_6_R);
                -l_5_R.put("type", "termination");
                -l_7_R = a.f();
                if (-l_7_R != null) {
                    -l_7_R.post(new c(this.a, -l_5_R, -l_4_I));
                }
                a.h();
            } catch (Object -l_7_R2) {
                Log.e("HiAnalytics", "onTerminate: JSONException.", -l_7_R2);
                -l_7_R2.printStackTrace();
                -l_7_R = -l_7_R2;
            }
        }
        Object -l_2_R2 = -l_3_R.edit();
        -l_2_R2.putString("activities", "");
        -l_2_R2.remove("events");
        -l_2_R2.commit();
    }

    private boolean c(SharedPreferences sharedPreferences) {
        return !(((this.c - sharedPreferences.getLong("last_millis", -1)) > (a.a().longValue() * 1000) ? 1 : ((this.c - sharedPreferences.getLong("last_millis", -1)) == (a.a().longValue() * 1000) ? 0 : -1)) <= 0);
    }

    public final void run() {
        try {
            Object -l_2_R = this.a;
            long -l_4_J = this.c;
            SharedPreferences -l_6_R = c.a(-l_2_R, "sessioncontext");
            if ("".equals(-l_6_R.getString("session_id", ""))) {
                a(-l_2_R, -l_6_R, -l_4_J);
            } else {
                if ((-l_4_J - -l_6_R.getLong("end_millis", 0) <= a.c().longValue() * 1000 ? 1 : null) == null) {
                    a(-l_2_R, -l_6_R, -l_4_J);
                } else {
                    Object -l_14_R = -l_6_R.edit();
                    -l_14_R.putLong("end_millis", -l_4_J);
                    -l_14_R.commit();
                }
            }
            d -l_1_R;
            Object -l_4_R;
            if (this.b == 0) {
                Context -l_2_R2 = this.a;
                -l_1_R = this;
                if (this.a == -l_2_R2) {
                    this.a = -l_2_R2;
                    -l_4_R = c.a(-l_2_R2, "state");
                    if (-l_4_R != null) {
                        long -l_5_J = -l_4_R.getLong("last_millis", -1);
                        if (-l_5_J == -1) {
                            a.h();
                        } else {
                            long -l_7_J = this.c - -l_5_J;
                            long -l_9_J = -l_4_R.getLong("duration", 0);
                            Object -l_11_R = -l_4_R.edit();
                            Object -l_12_R = -l_4_R.getString("activities", "");
                            Object -l_3_R = -l_2_R2.getClass().getName();
                            if (!"".equals(-l_12_R)) {
                                -l_12_R = new StringBuilder(String.valueOf(-l_12_R)).append(";").toString();
                            }
                            long -l_13_J = -l_5_J;
                            -l_12_R = new StringBuilder(String.valueOf(-l_12_R)).append(-l_3_R).append(",").append(new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US).format(new Date(-l_5_J))).append(",").append(-l_7_J / 1000).toString();
                            -l_11_R.remove("activities");
                            -l_11_R.putString("activities", -l_12_R);
                            -l_11_R.putLong("duration", -l_9_J + -l_7_J);
                            -l_11_R.commit();
                        }
                        if (c(-l_4_R)) {
                            b(-l_4_R);
                            a(-l_4_R);
                            return;
                        } else if (a.d(-l_2_R2)) {
                            b(-l_4_R);
                            a(-l_4_R);
                        }
                    }
                    return;
                }
                a.h();
            } else if (this.b != 1) {
                if (this.b == 2) {
                    -l_1_R = this;
                    -l_4_R = c.a(this.a, "state");
                    if (-l_4_R != null) {
                        b(-l_4_R);
                    }
                }
            } else {
                -l_2_R = this.a;
                -l_1_R = this;
                this.a = -l_2_R;
                -l_4_R = c.a(-l_2_R, "state");
                if (-l_4_R != null && c(-l_4_R)) {
                    b(-l_4_R);
                    a(-l_4_R);
                }
            }
        } catch (Object -l_1_R2) {
            "SessionThread.run() throw exception:" + -l_1_R2.getMessage();
            a.h();
            -l_1_R2.printStackTrace();
        }
    }
}
