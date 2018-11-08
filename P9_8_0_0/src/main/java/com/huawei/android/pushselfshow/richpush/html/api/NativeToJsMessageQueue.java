package com.huawei.android.pushselfshow.richpush.html.api;

import android.app.Activity;
import android.webkit.WebView;
import com.huawei.android.pushagent.a.a.c;
import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NativeToJsMessageQueue {
    public WebView a;
    private final LinkedList b = new LinkedList();
    private final a c;
    private final Activity d;
    private String e;

    private interface a {
        void onNativeToJsMessageAvailable();
    }

    private class OnlineEventsBridgeMode implements a {
        boolean a = true;
        final Runnable b = new a(this);
        final /* synthetic */ NativeToJsMessageQueue c;

        OnlineEventsBridgeMode(NativeToJsMessageQueue nativeToJsMessageQueue) {
            this.c = nativeToJsMessageQueue;
            c.a("PushSelfShowLog", "OnlineEventsBridgeMode() the webview is " + nativeToJsMessageQueue.a);
            nativeToJsMessageQueue.a.setNetworkAvailable(true);
        }

        public void onNativeToJsMessageAvailable() {
            this.c.d.runOnUiThread(this.b);
        }
    }

    private static class b {
        final String a;
        final d b;

        b(d dVar, String str) {
            this.a = str;
            this.b = dVar;
        }

        JSONObject a() {
            if (this.b == null) {
                return null;
            }
            try {
                Object -l_1_R = new JSONObject();
                -l_1_R.put("type", this.b.a());
                if (this.b.b() != null) {
                    -l_1_R.put("message", this.b.b());
                }
                -l_1_R.put("callbackId", this.a);
                return -l_1_R;
            } catch (JSONException e) {
                return null;
            }
        }
    }

    public NativeToJsMessageQueue(Activity activity, WebView webView, String str) {
        c.a("PushSelfShowLog", "activity is " + activity);
        c.a("PushSelfShowLog", "webView is " + webView);
        c.a("PushSelfShowLog", "localPath is " + str);
        this.d = activity;
        this.a = webView;
        this.e = str;
        this.c = new OnlineEventsBridgeMode(this);
        b();
    }

    private boolean d() {
        boolean isEmpty;
        NativeToJsMessageQueue -l_1_R = this;
        synchronized (this) {
            isEmpty = this.b.isEmpty();
        }
        return isEmpty;
    }

    public String a() {
        return this.e;
    }

    public void a(String str, com.huawei.android.pushselfshow.richpush.html.api.d.a aVar, String str2, JSONObject jSONObject) {
        try {
            c.a("PushSelfShowLog", "addPluginResult status is " + d.c()[aVar.ordinal()]);
            if (str != null) {
                Object -l_6_R = new b(jSONObject != null ? new d(str2, aVar, jSONObject) : new d(str2, aVar), str);
                NativeToJsMessageQueue -l_7_R = this;
                synchronized (this) {
                    this.b.add(-l_6_R);
                    if (this.c != null) {
                        this.c.onNativeToJsMessageAvailable();
                    }
                }
                return;
            }
            c.e("JsMessageQueue", "Got plugin result with no callbackId");
        } catch (Object -l_5_R) {
            c.e("PushSelfShowLog", "addPluginResult failed", -l_5_R);
        }
    }

    public final void b() {
        NativeToJsMessageQueue -l_1_R = this;
        synchronized (this) {
            this.b.clear();
        }
    }

    public String c() {
        NativeToJsMessageQueue -l_1_R = this;
        synchronized (this) {
            if (this.b.isEmpty()) {
                return null;
            }
            Object -l_2_R = new JSONArray();
            int -l_3_I = this.b.size();
            for (int -l_4_I = 0; -l_4_I < -l_3_I; -l_4_I++) {
                Object -l_6_R = ((b) this.b.removeFirst()).a();
                if (-l_6_R != null) {
                    -l_2_R.put(-l_6_R);
                }
            }
            String jSONArray = -l_2_R.toString();
            return jSONArray;
        }
    }
}
