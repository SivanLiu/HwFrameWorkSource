package com.huawei.android.pushselfshow.utils;

import android.content.Context;
import com.huawei.android.pushagent.a.a.c;

class b implements Runnable {
    final /* synthetic */ Context a;
    final /* synthetic */ String b;
    final /* synthetic */ String c;
    final /* synthetic */ String d;
    final /* synthetic */ String e;
    final /* synthetic */ int f;

    b(Context context, String str, String str2, String str3, String str4, int i) {
        this.a = context;
        this.b = str;
        this.c = str2;
        this.d = str3;
        this.e = str4;
        this.f = i;
    }

    public void run() {
        Object -l_1_R;
        try {
            if (a.o(this.a)) {
                -l_1_R = "PUSH_PS";
                Object -l_2_R = new StringBuffer(String.valueOf(a.a())).append("|").append("PS").append("|").append(a.b(this.a)).append("|").append(this.b).append("|").append(this.c).append("|").append(a.a(this.a)).append("|").append(this.d).append("|").append(this.e).append("|").append(this.f).toString();
                if (this.a != null) {
                    Object -l_3_R = Class.forName("com.hianalytics.android.v1.HiAnalytics");
                    -l_3_R.getMethod("onEvent", new Class[]{Context.class, String.class, String.class}).invoke(-l_3_R, new Object[]{this.a, -l_1_R, -l_2_R});
                    -l_3_R.getMethod("onReport", new Class[]{Context.class}).invoke(-l_3_R, new Object[]{this.a});
                    c.b("PushSelfShowLog", "send HiAnalytics msg, report cmd =" + this.d + ", msgid = " + this.b + ", eventId = " + this.c);
                }
                return;
            }
            c.b("PushSelfShowLog", "not allowed to sendHiAnalytics!");
        } catch (Object -l_1_R2) {
            c.e("PushSelfShowLog", "sendHiAnalytics IllegalAccessException ", -l_1_R2);
        } catch (Object -l_1_R22) {
            c.e("PushSelfShowLog", "sendHiAnalytics IllegalArgumentException ", -l_1_R22);
        } catch (Object -l_1_R222) {
            c.e("PushSelfShowLog", "sendHiAnalytics InvocationTargetException", -l_1_R222);
        } catch (Object -l_1_R2222) {
            c.e("PushSelfShowLog", "sendHiAnalytics NoSuchMethodException", -l_1_R2222);
        } catch (ClassNotFoundException e) {
            c.e("PushSelfShowLog", "sendHiAnalytics ClassNotFoundException");
        }
    }
}
