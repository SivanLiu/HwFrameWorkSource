package com.huawei.android.pushselfshow.richpush.html.api;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.huawei.android.pushagent.a.a.c;

public class ExposedJsApi {
    private static final String TAG = "PushSelfShowLog";
    private NativeToJsMessageQueue jsMessageQueue;
    public c pluginManager;

    public ExposedJsApi(Activity activity, WebView webView, String str, boolean z, String str2) {
        c.e("PushSelfShowLog", "init ExposedJsApi");
        this.pluginManager = new c(activity, z, str2);
        this.jsMessageQueue = new NativeToJsMessageQueue(activity, webView, str);
    }

    @JavascriptInterface
    public void exec(String str, String str2) {
        try {
            c.a("PushSelfShowLog", "ExposedJsApi exec and serviceName is " + str + ",jsonMsgObject is " + str2);
            this.pluginManager.a(str, str2, this.jsMessageQueue);
        } catch (Throwable -l_3_R) {
            c.a("PushSelfShowLog", "ExposedJsApi exec error", -l_3_R);
        }
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        this.pluginManager.a(i, i2, intent);
    }

    public void onDestroy() {
        this.pluginManager.a();
        this.jsMessageQueue.b();
    }

    public void onPause() {
        this.pluginManager.c();
    }

    public void onResume() {
        this.pluginManager.b();
    }

    @JavascriptInterface
    public String retrieveJsMessages() {
        Object -l_1_R = "";
        try {
            -l_1_R = this.jsMessageQueue.c();
        } catch (Exception e) {
            c.a("PushSelfShowLog", "retrieveJsMessages error");
        }
        return -l_1_R;
    }

    @JavascriptInterface
    public String synExec(String str, String str2) {
        try {
            c.a("PushSelfShowLog", "ExposedJsApi exec and serviceName is " + str + ",jsonMsgObject is " + str2);
            return this.pluginManager.a(str, str2);
        } catch (Throwable -l_3_R) {
            c.a("PushSelfShowLog", "ExposedJsApi exec error", -l_3_R);
            return null;
        }
    }
}
