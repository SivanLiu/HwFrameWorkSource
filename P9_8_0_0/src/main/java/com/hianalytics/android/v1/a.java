package com.hianalytics.android.v1;

import android.content.Context;
import com.hianalytics.android.a.a.c;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class a implements Runnable {
    private Context a;
    private String b;
    private String c;
    private long d;

    public a(Context context, String str, String str2, long j) {
        this.a = context;
        this.b = str.replace(",", "^");
        this.c = str2.replace(",", "^");
        this.d = j;
    }

    public final void run() {
        a -l_1_R = this;
        try {
            Object -l_2_R = c.a(this.a, "state");
            if (-l_2_R != null) {
                Object -l_3_R = -l_2_R.getString("events", "");
                if (!"".equals(-l_3_R)) {
                    -l_3_R = new StringBuilder(String.valueOf(-l_3_R)).append(";").toString();
                }
                String str = "yyyyMMddHHmmssSSS";
                -l_3_R = new StringBuilder(String.valueOf(-l_3_R)).append(this.b).append(",").append(this.c).append(",").append(new SimpleDateFormat(str, Locale.US).format(new Date(this.d))).toString();
                int -l_4_I = -l_3_R.split(";").length;
                if (-l_4_I <= com.hianalytics.android.a.a.a.d()) {
                    Object -l_5_R = -l_2_R.edit();
                    -l_5_R.remove("events");
                    -l_5_R.putString("events", -l_3_R);
                    -l_5_R.commit();
                    " current event record is：" + -l_4_I;
                    com.hianalytics.android.a.a.a.h();
                }
                if (com.hianalytics.android.a.a.a.d(this.a)) {
                    if (com.hianalytics.android.a.a.a.e()) {
                        com.hianalytics.android.a.a.a.h();
                        HiAnalytics.onReport(this.a);
                        return;
                    }
                    -l_2_R.edit().remove("events").commit();
                }
                return;
            }
            com.hianalytics.android.a.a.a.h();
        } catch (Object -l_1_R2) {
            "EventThread.run() throw exception:" + -l_1_R2.getMessage();
            com.hianalytics.android.a.a.a.h();
            -l_1_R2.printStackTrace();
        }
    }
}
