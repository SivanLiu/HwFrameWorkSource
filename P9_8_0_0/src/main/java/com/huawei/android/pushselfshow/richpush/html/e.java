package com.huawei.android.pushselfshow.richpush.html;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.utils.d;
import java.io.File;

class e extends WebViewClient {
    final /* synthetic */ HtmlViewer a;

    e(HtmlViewer htmlViewer) {
        this.a = htmlViewer;
    }

    public void onLoadResource(WebView webView, String str) {
        super.onLoadResource(webView, str);
    }

    public void onPageFinished(WebView webView, String str) {
        super.onPageFinished(webView, str);
        c.a("PushSelfShowLog", "onPageFinished:" + str + ",title:" + webView.getTitle());
        Object -l_3_R = webView.getTitle();
        if (-l_3_R != null && -l_3_R.endsWith(".html")) {
            this.a.a(this.a.d.getString(d.a(this.a.d, "hwpush_richmedia")));
        }
        try {
            if (this.a.e != null && !str.equals(this.a.d.getFilesDir().getPath() + File.separator + "PushService" + File.separator + "richpush" + File.separator + "error.html")) {
                Object -l_5_R = "";
                if ("text/html_local".equals(this.a.g.z())) {
                    -l_5_R = (("var newscript = document.createElement(\"script\");" + "newscript.src=\"" + this.a.prepareJS(str) + "\";") + "newscript.onload=function(){ try {onDeviceReady();}catch(err){}};") + "document.body.appendChild(newscript);";
                } else {
                    -l_5_R = (("var newscript = document.createElement(\"script\");" + "newscript.src=\"http://open.hicloud.com/android/push1.0.js\";") + "newscript.onload=function(){ try { onDeviceReady();}catch(err){}};") + "document.body.appendChild(newscript);";
                }
                c.a("PushSelfShowLog", "load js " + -l_5_R);
                this.a.e.loadUrl("javascript:" + -l_5_R);
            }
        } catch (Throwable -l_4_R) {
            c.a("PushSelfShowLog", "onPageFinished load err " + -l_4_R.toString(), -l_4_R);
        }
    }

    public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
        super.onPageStarted(webView, str, bitmap);
        c.a("PushSelfShowLog", "onPageStarted:" + str);
        this.a.setProgress(5);
        this.a.a(this.a.d.getString(d.a(this.a.d, "hwpush_richmedia")));
    }

    public boolean shouldOverrideUrlLoading(WebView webView, String str) {
        try {
            if (!str.startsWith("mailto:") && !str.startsWith("tel:") && !str.startsWith("smsto:") && !str.startsWith("sms:") && !str.startsWith("geo:")) {
                return false;
            }
            this.a.d.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(str)));
            return true;
        } catch (Throwable -l_3_R) {
            c.a("PushSelfShowLog", "", -l_3_R);
        }
    }
}
