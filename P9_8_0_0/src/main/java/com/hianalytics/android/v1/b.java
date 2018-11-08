package com.hianalytics.android.v1;

import com.hianalytics.android.a.a.a;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import tmsdk.common.module.intelli_sms.SmsCheckResult;

public final class b {
    private static HttpsURLConnection a = null;

    public static boolean a(String str, byte[] bArr) {
        Object -l_2_R = null;
        Object -l_3_R = null;
        try {
            -l_3_R = (HttpURLConnection) new URL(str).openConnection();
            a.h();
            -l_3_R.setRequestMethod("POST");
            -l_3_R.setConnectTimeout(5000);
            -l_3_R.setDoOutput(true);
            -l_3_R.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            -l_3_R.setRequestProperty("Content-Length", String.valueOf(bArr.length));
            -l_2_R = -l_3_R.getOutputStream();
            -l_2_R.write(bArr);
            -l_2_R.flush();
            int -l_0_I = -l_3_R.getResponseCode();
            "connHttp.getResponseCode() = " + -l_0_I;
            a.h();
            -l_0_I = -l_0_I != SmsCheckResult.ESCT_200 ? 0 : 1;
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (-l_3_R != null) {
                -l_3_R.disconnect();
                a.h();
            }
            return -l_0_I;
        } catch (IOException e2) {
            "connHttp error:" + e2.getMessage();
            a.h();
            e2.printStackTrace();
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (IOException e22) {
                    e22.printStackTrace();
                }
            }
            if (-l_3_R != null) {
                -l_3_R.disconnect();
                a.h();
            }
            return false;
        } catch (Throwable th) {
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            if (-l_3_R != null) {
                -l_3_R.disconnect();
                a.h();
            }
        }
    }
}
