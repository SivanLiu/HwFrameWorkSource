package com.huawei.android.pushselfshow.richpush.html.a;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION;
import android.text.TextUtils;
import com.huawei.android.pushagent.a.a.a.f;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushagent.a.a.d;
import com.huawei.android.pushselfshow.richpush.html.api.NativeToJsMessageQueue;
import com.huawei.android.pushselfshow.utils.a;
import org.json.JSONObject;

public class h implements g {
    private Activity a;
    private NativeToJsMessageQueue b;
    private String c;
    private boolean d = false;
    private String e = "";

    public h(Activity activity, boolean z, String str) {
        c.e("PushSelfShowLog", "init App");
        this.a = activity;
        this.d = z;
        this.e = str;
    }

    private String e() {
        Object -l_1_R = new JSONObject();
        try {
            -l_1_R.put("manufacturer", Build.MANUFACTURER);
            -l_1_R.put("model", Build.MODEL);
            -l_1_R.put("version", Build.DISPLAY);
            -l_1_R.put("os", "Android");
            -l_1_R.put("osVersion", VERSION.RELEASE);
            -l_1_R.put("uuid", a());
            -l_1_R.put("sdkVersion", "2907");
            if (this.d) {
                -l_1_R.put("imei", a.a(this.e));
            }
        } catch (Object -l_2_R) {
            c.e("PushSelfShowLog", "onError error", -l_2_R);
        }
        return -l_1_R.toString();
    }

    public String a() {
        Object -l_1_R;
        try {
            -l_1_R = d.a(this.a, "push_client_self_info", "token_info");
            if (TextUtils.isEmpty(-l_1_R)) {
                -l_1_R = this.e;
            }
            return f.a(-l_1_R);
        } catch (Object -l_1_R2) {
            c.d("PushSelfShowLog", -l_1_R2.toString());
            return "";
        }
    }

    public String a(String str, JSONObject jSONObject) {
        return !"getDeviceInfo".equals(str) ? com.huawei.android.pushselfshow.richpush.html.api.d.a(com.huawei.android.pushselfshow.richpush.html.api.d.a.ERROR).toString() : e();
    }

    public void a(int i, int i2, Intent intent) {
    }

    public void a(NativeToJsMessageQueue nativeToJsMessageQueue, String str, String str2, JSONObject jSONObject) {
        if (nativeToJsMessageQueue != null) {
            this.b = nativeToJsMessageQueue;
            if (str2 == null) {
                c.a("PushSelfShowLog", "get DeviceInfo exec callback is null ");
            } else {
                this.c = str2;
            }
            this.b.a(this.c, com.huawei.android.pushselfshow.richpush.html.api.d.a.METHOD_NOT_FOUND_EXCEPTION, "error", null);
            return;
        }
        c.a("PushSelfShowLog", "jsMessageQueue is null while run into App exec");
    }

    public void b() {
    }

    public void c() {
    }

    public void d() {
    }
}
