package com.huawei.android.pushselfshow.richpush.html.api;

import android.app.Activity;
import android.content.Intent;
import com.huawei.android.pushselfshow.richpush.html.a.a;
import com.huawei.android.pushselfshow.richpush.html.a.d;
import com.huawei.android.pushselfshow.richpush.html.a.e;
import com.huawei.android.pushselfshow.richpush.html.a.g;
import com.huawei.android.pushselfshow.richpush.html.a.h;
import com.huawei.android.pushselfshow.richpush.html.a.i;
import com.huawei.android.pushselfshow.richpush.html.a.j;
import java.util.HashMap;
import java.util.Map.Entry;
import org.json.JSONException;
import org.json.JSONObject;

public class c {
    public HashMap a = new HashMap();

    public c(Activity activity, boolean z, String str) {
        try {
            this.a.clear();
            this.a.put("Audio", new e(activity));
            this.a.put("Video", new j(activity));
            this.a.put("App", new d(activity));
            this.a.put("Geo", new i(activity));
            this.a.put("Accelerometer", new a(activity));
            this.a.put("Device", new h(activity, z, str));
        } catch (Object -l_4_R) {
            com.huawei.android.pushagent.a.a.c.d("PluginManager", -l_4_R.toString(), -l_4_R);
        }
    }

    public String a(String str, String str2) throws JSONException {
        Object -l_4_R = new JSONObject();
        try {
            Object -l_5_R = new JSONObject(str2);
            if (-l_5_R.has("method")) {
                Object -l_3_R = -l_5_R.getString("method");
                com.huawei.android.pushagent.a.a.c.a("PluginManager", "method is " + -l_3_R);
                if (-l_5_R.has("options")) {
                    -l_4_R = -l_5_R.getJSONObject("options");
                }
                if (!this.a.containsKey(str)) {
                    return d.a(d.a.SERVICE_NOT_FOUND_EXCEPTION).toString();
                }
                com.huawei.android.pushagent.a.a.c.a("PluginManager", "plugins.containsKey(" + str + ") ");
                return ((g) this.a.get(str)).a(-l_3_R, -l_4_R);
            }
            com.huawei.android.pushagent.a.a.c.a("PluginManager", "method is null");
            return d.a(d.a.METHOD_NOT_FOUND_EXCEPTION).toString();
        } catch (JSONException e) {
            return d.a(d.a.JSON_EXCEPTION).toString();
        }
    }

    public void a() {
        for (Entry -l_2_R : this.a.entrySet()) {
            g -l_4_R = (g) -l_2_R.getValue();
            String str = "PluginManager";
            com.huawei.android.pushagent.a.a.c.e(str, "call plugin: " + ((String) -l_2_R.getKey()) + " reset");
            -l_4_R.d();
        }
    }

    public void a(int i, int i2, Intent intent) {
        for (Entry -l_5_R : this.a.entrySet()) {
            g -l_7_R = (g) -l_5_R.getValue();
            String str = "PluginManager";
            com.huawei.android.pushagent.a.a.c.e(str, "call plugin: " + ((String) -l_5_R.getKey()) + " reset");
            -l_7_R.a(i, i2, intent);
        }
    }

    public void a(String str, String str2, NativeToJsMessageQueue nativeToJsMessageQueue) {
        if (nativeToJsMessageQueue != null) {
            String str3 = null;
            Object -l_6_R = new JSONObject();
            try {
                Object -l_7_R = new JSONObject(str2);
                if (-l_7_R.has("callbackId")) {
                    str3 = -l_7_R.getString("callbackId");
                    com.huawei.android.pushagent.a.a.c.a("PluginManager", "callbackId is " + str3);
                }
                if (-l_7_R.has("method")) {
                    Object -l_5_R = -l_7_R.getString("method");
                    com.huawei.android.pushagent.a.a.c.a("PluginManager", "method is " + -l_5_R);
                    if (-l_7_R.has("options")) {
                        -l_6_R = -l_7_R.getJSONObject("options");
                    }
                    if (this.a.containsKey(str)) {
                        com.huawei.android.pushagent.a.a.c.a("PluginManager", "plugins.containsKey(" + str + ") ");
                        ((g) this.a.get(str)).a(nativeToJsMessageQueue, -l_5_R, str3, -l_6_R);
                    } else {
                        nativeToJsMessageQueue.a(str3, d.a.SERVICE_NOT_FOUND_EXCEPTION, "error", null);
                    }
                    return;
                }
                com.huawei.android.pushagent.a.a.c.a("PluginManager", "method is null");
                nativeToJsMessageQueue.a(str3, d.a.METHOD_NOT_FOUND_EXCEPTION, "error", null);
                return;
            } catch (JSONException e) {
                nativeToJsMessageQueue.a(str3, d.a.JSON_EXCEPTION, "error", null);
                return;
            }
        }
        com.huawei.android.pushagent.a.a.c.a("PluginManager", "plugin.exec,jsMessageQueue is null");
    }

    public void b() {
        for (Entry -l_2_R : this.a.entrySet()) {
            g -l_4_R = (g) -l_2_R.getValue();
            String str = "PluginManager";
            com.huawei.android.pushagent.a.a.c.e(str, "call plugin: " + ((String) -l_2_R.getKey()) + " reset");
            -l_4_R.b();
        }
    }

    public void c() {
        for (Entry -l_2_R : this.a.entrySet()) {
            g -l_4_R = (g) -l_2_R.getValue();
            String str = "PluginManager";
            com.huawei.android.pushagent.a.a.c.e(str, "call plugin: " + ((String) -l_2_R.getKey()) + " reset");
            -l_4_R.c();
        }
    }
}
