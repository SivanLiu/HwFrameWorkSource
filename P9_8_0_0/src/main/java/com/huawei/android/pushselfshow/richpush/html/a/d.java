package com.huawei.android.pushselfshow.richpush.html.a;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.richpush.html.api.NativeToJsMessageQueue;
import com.huawei.android.pushselfshow.richpush.html.api.d.a;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.BasicCloudField;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.MessageSafeConfigJsonField;
import org.json.JSONException;
import org.json.JSONObject;

public class d implements g {
    public boolean a = false;
    public int b;
    public int c;
    private NativeToJsMessageQueue d;
    private String e;
    private Activity f;

    public d(Activity activity) {
        c.e("PushSelfShowLog", "init App");
        this.f = activity;
    }

    private void a(String str, String str2, boolean z) {
        try {
            c.a("PushSelfShowLog", "enter launchApp , appPackageName =" + str + ",and msg.intentUri is " + str2 + " boolean appmarket is " + z);
            if (str == null || str.trim().length() == 0) {
                this.d.a(this.e, a.JSON_EXCEPTION, "error", null);
                return;
            }
            Intent -l_4_R = com.huawei.android.pushselfshow.utils.a.b(this.f, str);
            if (-l_4_R != null) {
                if (str2 != null) {
                    try {
                        Intent -l_5_R = Intent.parseUri(str2, 0);
                        c.e("PushSelfShowLog", "Intent.parseUri(intentUri, 0)，" + -l_4_R.toURI());
                        if (com.huawei.android.pushselfshow.utils.a.a(this.f, str, -l_5_R).booleanValue()) {
                            -l_4_R = -l_5_R;
                        }
                    } catch (Throwable -l_5_R2) {
                        c.a("PushSelfShowLog", "intentUri error ", -l_5_R2);
                    }
                }
                if (com.huawei.android.pushselfshow.utils.a.a(this.f, -l_4_R)) {
                    if (this.a) {
                        c.e("PushSelfShowLog", " APP_OPEN startActivityForResult " + -l_4_R.toURI());
                        this.f.startActivityForResult(-l_4_R, this.b);
                    } else {
                        c.e("PushSelfShowLog", " APP_OPEN start " + -l_4_R.toURI());
                        this.f.startActivity(-l_4_R);
                    }
                    this.d.a(this.e, a.OK, "success", null);
                }
                c.c("PushSelfShowLog", "no permission to start Activity");
                this.d.a(this.e, a.ILLEGAL_ACCESS_EXCEPTION, "error", null);
                return;
            }
            if (z) {
                a(str);
            } else {
                c.e("PushSelfShowLog", "APP_NOT_EXIST and appmaeket is false");
                this.d.a(this.e, a.APP_NOT_EXIST, "error", null);
            }
        } catch (Object -l_4_R2) {
            c.d("PushSelfShowLog", -l_4_R2.toString(), -l_4_R2);
        }
    }

    private void a(JSONObject jSONObject) {
        String str = null;
        String str2 = null;
        boolean -l_4_I = false;
        if (jSONObject != null && jSONObject.has("package-name")) {
            try {
                str = jSONObject.getString("package-name");
                if (jSONObject.has("intent-uri")) {
                    str2 = jSONObject.getString("intent-uri");
                }
                if (jSONObject.has("appmarket")) {
                    -l_4_I = jSONObject.getBoolean("appmarket");
                }
                if (jSONObject.has("requestCode") && jSONObject.has(BasicCloudField.CONFIG_SERVRE_RESULT_CODE)) {
                    this.b = jSONObject.getInt("requestCode");
                    this.c = jSONObject.getInt(BasicCloudField.CONFIG_SERVRE_RESULT_CODE);
                    this.a = true;
                }
            } catch (Object -l_5_R) {
                c.e("PushSelfShowLog", "openApp param failed ", -l_5_R);
            }
            c.b("PushSelfShowLog", "packageName is %s , appmarket is %s ,bResult is %s ", str, Boolean.valueOf(-l_4_I), Boolean.valueOf(this.a));
            a(str, str2, -l_4_I);
            return;
        }
        this.d.a(this.e, a.JSON_EXCEPTION, "error", null);
    }

    private void b(JSONObject jSONObject) {
        Object -l_2_R;
        if (jSONObject != null && jSONObject.has("package-name")) {
            try {
                -l_2_R = jSONObject.getString("package-name");
                Object -l_3_R = new JSONObject();
                Object -l_4_R = this.f.getPackageManager();
                Object -l_5_R = -l_4_R.getPackageInfo(-l_2_R, 0);
                Object -l_6_R = -l_5_R.applicationInfo.loadLabel(-l_4_R).toString();
                Object -l_7_R = -l_5_R.versionName;
                int -l_8_I = -l_5_R.versionCode;
                -l_3_R.put("appName", -l_6_R);
                -l_3_R.put("versionCode", -l_8_I);
                -l_3_R.put("versionName", -l_7_R);
                this.d.a(this.e, a.OK, "success", -l_3_R);
                return;
            } catch (Object -l_2_R2) {
                c.e("PushSelfShowLog", "getAppInfo param failed ", -l_2_R2);
                this.d.a(this.e, a.APP_NOT_EXIST, "error", null);
                return;
            }
        }
        this.d.a(this.e, a.JSON_EXCEPTION, "error", null);
    }

    private String c(JSONObject jSONObject) throws JSONException {
        Object -l_2_R;
        if (jSONObject != null && jSONObject.has("package-name")) {
            Object -l_3_R;
            try {
                -l_3_R = jSONObject.getString("package-name");
                Object -l_4_R = this.f.getPackageManager();
                Object -l_5_R = -l_4_R.getPackageInfo(-l_3_R, 0);
                Object -l_6_R = -l_5_R.applicationInfo.loadLabel(-l_4_R).toString();
                Object -l_7_R = -l_5_R.versionName;
                int -l_8_I = -l_5_R.versionCode;
                -l_2_R = com.huawei.android.pushselfshow.richpush.html.api.d.a(a.OK);
                -l_2_R.put("appName", -l_6_R);
                -l_2_R.put("versionCode", -l_8_I);
                -l_2_R.put("versionName", -l_7_R);
            } catch (Object -l_3_R2) {
                c.e("PushSelfShowLog", "getAppInfo param failed ", -l_3_R2);
                -l_2_R = com.huawei.android.pushselfshow.richpush.html.api.d.a(a.APP_NOT_EXIST);
            }
        } else {
            -l_2_R = com.huawei.android.pushselfshow.richpush.html.api.d.a(a.JSON_EXCEPTION);
        }
        return -l_2_R.toString();
    }

    public String a(String str, JSONObject jSONObject) {
        try {
            return !"getAppInfo".equals(str) ? com.huawei.android.pushselfshow.richpush.html.api.d.a(a.METHOD_NOT_FOUND_EXCEPTION).toString() : c(jSONObject);
        } catch (Object -l_3_R) {
            c.d("PushSelfShowLog", -l_3_R.toString());
            return null;
        }
    }

    public void a(int i, int i2, Intent intent) {
        c.e("PushSelfShowLog", "onActivityResult and requestCode is " + i + " resultCode is " + i2 + " intent data is " + intent);
        try {
            if (this.a && i2 == this.c && intent != null) {
                Object -l_4_R = new JSONObject();
                Object -l_5_R = new JSONObject();
                Object -l_6_R = intent.getExtras();
                if (-l_6_R != null) {
                    for (String -l_9_R : -l_6_R.keySet()) {
                        -l_5_R.put(-l_9_R, -l_6_R.get(-l_9_R));
                    }
                    if (-l_5_R.length() > 0) {
                        -l_4_R.put("extra", -l_5_R);
                        this.d.a(this.e, a.OK, MessageSafeConfigJsonField.UPDATE_STATUS, -l_4_R);
                    }
                }
            }
        } catch (JSONException e) {
            c.e("PushSelfShowLog", "onActivityResult error");
        } catch (Exception e2) {
            c.e("PushSelfShowLog", "onActivityResult error");
        }
    }

    public void a(NativeToJsMessageQueue nativeToJsMessageQueue, String str, String str2, JSONObject jSONObject) {
        if (nativeToJsMessageQueue != null) {
            this.d = nativeToJsMessageQueue;
            if ("openApp".equals(str)) {
                d();
                if (str2 == null) {
                    c.a("PushSelfShowLog", "Audio exec callback is null ");
                } else {
                    this.e = str2;
                    a(jSONObject);
                }
            } else if (!"getAppInfo".equals(str)) {
                nativeToJsMessageQueue.a(str2, a.METHOD_NOT_FOUND_EXCEPTION, "error", null);
            } else if (str2 == null) {
                c.a("PushSelfShowLog", "Audio exec callback is null ");
            } else {
                this.e = str2;
                b(jSONObject);
            }
            return;
        }
        c.a("PushSelfShowLog", "jsMessageQueue is null while run into App exec");
    }

    public void a(String str) {
        Intent -l_2_R = null;
        if (com.huawei.android.pushselfshow.utils.a.a(this.f, "com.huawei.appmarket", new Intent("com.huawei.appmarket.intent.action.AppDetail")).booleanValue()) {
            c.a("PushSelfShowLog", "app not exist && appmarkt exist ,so open appmarket");
            -l_2_R = new Intent("com.huawei.appmarket.intent.action.AppDetail");
            -l_2_R.putExtra("APP_PACKAGENAME", str);
            -l_2_R.setPackage("com.huawei.appmarket");
            -l_2_R.setFlags(402653184);
            c.a("PushSelfShowLog", "hwAppmarket only support com.huawei.appmarket.intent.action.AppDetail!");
        } else if (com.huawei.android.pushselfshow.utils.a.c(this.f).size() > 0) {
            c.a("PushSelfShowLog", "app not exist && other appmarkt exist ,so open appmarket");
            -l_2_R = new Intent();
            -l_2_R.setAction("android.intent.action.VIEW");
            -l_2_R.setData(Uri.parse("market://details?id=" + str));
            -l_2_R.setFlags(402653184);
        }
        if (-l_2_R == null) {
            c.a("PushSelfShowLog", "intent is null ");
            c.e("PushSelfShowLog", "APP_OPEN_APPMARKET and not find any  appmaeket");
            this.d.a(this.e, a.APP_NOT_APPMARKET, "error", null);
            return;
        }
        c.e("PushSelfShowLog", "intent is not null " + -l_2_R.toURI());
        this.f.startActivity(-l_2_R);
        c.e("PushSelfShowLog", "APP_OPEN_APPMARKET and open with appmaeket");
        this.d.a(this.e, a.APP_OPEN_APPMARKET, "success", null);
    }

    public void b() {
    }

    public void c() {
        d();
    }

    public void d() {
        this.e = null;
        this.a = false;
        this.b = 0;
        this.c = 0;
    }
}
