package com.huawei.android.pushselfshow.b;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.URLUtil;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.util.ArrayList;

public class a {
    private static final String[] a = new String[]{"phone", CheckVersionField.CHECK_VERSION_SERVER_URL, "email", "app", "cosa", "rp"};
    private Context b;
    private com.huawei.android.pushselfshow.c.a c;

    public a(Context context, com.huawei.android.pushselfshow.c.a aVar) {
        this.b = context;
        this.c = aVar;
    }

    public static boolean a(String str) {
        for (String equals : a) {
            if (equals.equals(str)) {
                return true;
            }
        }
        return false;
    }

    private String b(String str) {
        try {
            int -l_2_I = str.indexOf(63);
            if (-l_2_I == -1) {
                return str;
            }
            int -l_6_I;
            Object -l_3_R = str.substring(-l_2_I + 1).split("&");
            Object -l_4_R = new ArrayList();
            Object -l_5_R = -l_3_R;
            for (Object -l_8_R : -l_3_R) {
                if (!(-l_8_R.startsWith("h_w_hiapp_referrer") || -l_8_R.startsWith("h_w_gp_referrer"))) {
                    -l_4_R.add(-l_8_R);
                }
            }
            -l_5_R = new StringBuilder();
            for (-l_6_I = 0; -l_6_I < -l_4_R.size(); -l_6_I++) {
                -l_5_R.append((String) -l_4_R.get(-l_6_I));
                if (-l_6_I < -l_4_R.size() - 1) {
                    -l_5_R.append("&");
                }
            }
            Object -l_6_R = "";
            -l_6_R = -l_4_R.size() != 0 ? str.substring(0, -l_2_I + 1) + -l_5_R.toString() : str.substring(0, -l_2_I);
            c.a("PushSelfShowLog", "after delete referrer, the new IntentUri is:" + -l_6_R);
            return -l_6_R;
        } catch (Object -l_2_R) {
            c.c("PushSelfShowLog", "delete referrer exception", -l_2_R);
            return str;
        }
    }

    private void b() {
        c.a("PushSelfShowLog", "enter launchUrl");
        try {
            String -l_1_R = this.c.w();
            String -l_2_R = this.c.K();
            Object -l_3_R = this.c.C();
            if (!(this.c.B() == 0 || -l_3_R == null || -l_3_R.length() <= 0)) {
                if (-l_1_R.indexOf("?") == -1) {
                    this.c.c(-l_1_R + "?" + -l_3_R + "=" + com.huawei.android.pushselfshow.utils.a.a(-l_2_R));
                } else {
                    this.c.c(-l_1_R + "&" + -l_3_R + "=" + com.huawei.android.pushselfshow.utils.a.a(-l_2_R));
                }
            }
            c.a("PushSelfShowLog", "url =" + -l_1_R);
            if (this.c.A() != 0) {
                this.c.d(-l_1_R);
                this.c.f("text/html");
                this.c.e("html");
                i();
                return;
            }
            Object -l_4_R = new Intent();
            -l_4_R.setAction("android.intent.action.VIEW").setFlags(268435456).setData(Uri.parse(-l_1_R));
            this.b.startActivity(-l_4_R);
        } catch (Object -l_1_R2) {
            c.d("PushSelfShowLog", -l_1_R2.toString(), -l_1_R2);
        }
    }

    private void c() {
        c.a("PushSelfShowLog", "enter launchCall");
        Object -l_1_R;
        try {
            -l_1_R = new Intent();
            -l_1_R.setAction("android.intent.action.DIAL").setData(Uri.parse("tel:" + this.c.q())).setFlags(268435456);
            this.b.startActivity(-l_1_R);
        } catch (Object -l_1_R2) {
            c.d("PushSelfShowLog", -l_1_R2.toString(), -l_1_R2);
        }
    }

    private void d() {
        Object -l_1_R;
        c.a("PushSelfShowLog", "enter launchMail");
        try {
            if (this.c.r() != null) {
                -l_1_R = new Intent();
                String str = "android.intent.extra.SUBJECT";
                str = "android.intent.extra.TEXT";
                -l_1_R.setAction("android.intent.action.SENDTO").setFlags(268435456).setData(Uri.fromParts("mailto", this.c.r(), null)).putExtra(str, this.c.s()).putExtra(str, this.c.t()).setPackage("com.android.email");
                this.b.startActivity(-l_1_R);
            }
        } catch (Object -l_1_R2) {
            c.d("PushSelfShowLog", -l_1_R2.toString(), -l_1_R2);
        }
    }

    private void e() {
        try {
            c.b("PushSelfShowLog", "enter launchApp, appPackageName =" + this.c.u() + ",and msg.intentUri is " + this.c.g());
            if (com.huawei.android.pushselfshow.utils.a.c(this.b, this.c.u())) {
                h();
                return;
            }
            try {
                c.e("PushSelfShowLog", "insert into db message.getMsgId() is " + this.c.a() + ",message.appPackageName is " + this.c.u());
                com.huawei.android.pushselfshow.utils.a.a.a(this.b, this.c.a(), this.c.u());
            } catch (Object -l_1_R) {
                c.e("PushSelfShowLog", "launchApp not exist ,insertAppinfo error", -l_1_R);
            }
            c.b("PushSelfShowLog", "enter launch app, appPackageName =" + this.c.u() + ",and msg.intentUri is " + this.c.g());
            f();
        } catch (Object -l_1_R2) {
            c.d("PushSelfShowLog", "launchApp error:" + -l_1_R2.toString());
        }
    }

    private void f() {
        Object -l_1_R;
        try {
            -l_1_R = new StringBuilder();
            if (!TextUtils.isEmpty(this.c.g())) {
                -l_1_R.append("&referrer=").append(Uri.encode(b(this.c.g())));
            }
            Object -l_2_R = "market://details?id=" + this.c.u() + -l_1_R;
            Intent -l_3_R = new Intent("android.intent.action.VIEW");
            -l_3_R.setData(Uri.parse(-l_2_R));
            -l_3_R.setPackage("com.huawei.appmarket");
            Intent -l_4_R = new Intent("android.intent.action.VIEW");
            -l_4_R.setData(Uri.parse(-l_2_R));
            -l_4_R.setPackage("com.android.vending");
            if (com.huawei.android.pushselfshow.utils.a.a(this.b, "com.android.vending", -l_4_R).booleanValue()) {
                -l_4_R.setFlags(402653184);
                c.b("PushSelfShowLog", "open google play store's app detail, IntentUrl is:" + -l_4_R.toURI());
                this.b.startActivity(-l_4_R);
            } else if (com.huawei.android.pushselfshow.utils.a.a(this.b, "com.huawei.appmarket", -l_3_R).booleanValue()) {
                com.huawei.android.pushselfshow.utils.a.a(this.b, "7", this.c, -1);
                -l_3_R.setFlags(402653184);
                c.b("PushSelfShowLog", "open HiApp's app detail, IntentUrl is:" + -l_3_R.toURI());
                this.b.startActivity(-l_3_R);
            } else {
                c.b("PushSelfShowLog", "open app detail by browser.");
                g();
            }
        } catch (Object -l_1_R2) {
            c.d("PushSelfShowLog", "open market app detail failed,exception:" + -l_1_R2);
        }
    }

    private void g() {
        Object -l_4_R;
        Object -l_1_R = "";
        Object -l_2_R = "";
        try {
            Object -l_3_R = Uri.parse(Uri.decode(this.c.g()));
            try {
                -l_1_R = -l_3_R.getQueryParameter("h_w_hiapp_referrer");
            } catch (Exception e) {
                c.b("PushSelfShowLog", "parse h_w_hiapp_referrer faied");
            }
            try {
                -l_2_R = -l_3_R.getQueryParameter("h_w_gp_referrer");
            } catch (Exception e2) {
                c.b("PushSelfShowLog", "parse h_w_hiapp_referrer faied");
            }
        } catch (Throwable -l_4_R2) {
            c.b("PushSelfShowLog", "parse intentUri error,", -l_4_R2);
        }
        Object -l_5_R;
        if (com.huawei.android.pushagent.a.a.a.c() && com.huawei.android.pushagent.a.a.a.d()) {
            c.b("PushSelfShowLog", "It is China device, open Huawei market web, referrer: " + -l_1_R);
            -l_5_R = Uri.decode(-l_1_R);
            if (!URLUtil.isValidUrl(-l_5_R)) {
                -l_4_R = "http://a.vmall.com/";
            }
            -l_4_R = -l_5_R;
        } else {
            c.b("PushSelfShowLog", "not EMUI system or not in China, open google play web, referrer: " + -l_2_R);
            -l_5_R = Uri.decode(-l_2_R);
            if (!URLUtil.isValidUrl(-l_5_R)) {
                -l_4_R = "https://play.google.com/store/apps/details?id=" + this.c.u();
            }
            -l_4_R = -l_5_R;
        }
        c.b("PushSelfShowLog", "open the URL by browser: " + -l_4_R);
        com.huawei.android.pushselfshow.utils.a.e(this.b, -l_4_R);
    }

    private void h() {
        c.e("PushSelfShowLog", "run into launchCosaApp ");
        try {
            c.b("PushSelfShowLog", "enter launchExistApp cosa, appPackageName =" + this.c.u() + ",and msg.intentUri is " + this.c.g());
            Intent -l_1_R = com.huawei.android.pushselfshow.utils.a.b(this.b, this.c.u());
            int -l_2_I = 0;
            Intent -l_3_R;
            if (this.c.g() == null) {
                if (this.c.v() != null) {
                    -l_3_R = new Intent(this.c.v());
                    if (com.huawei.android.pushselfshow.utils.a.a(this.b, this.c.u(), -l_3_R).booleanValue()) {
                        -l_1_R = -l_3_R;
                    }
                }
                -l_1_R.setPackage(this.c.u());
            } else {
                try {
                    -l_3_R = Intent.parseUri(this.c.g(), 0);
                    c.b("PushSelfShowLog", "Intent.parseUri(msg.intentUri, 0)," + -l_3_R.toURI());
                    if (com.huawei.android.pushselfshow.utils.a.a(this.b, this.c.u(), -l_3_R).booleanValue()) {
                        -l_1_R = -l_3_R;
                        -l_2_I = 1;
                    }
                } catch (Throwable -l_3_R2) {
                    c.b("PushSelfShowLog", "intentUri error ", -l_3_R2);
                }
            }
            if (-l_1_R == null) {
                c.b("PushSelfShowLog", "launchCosaApp,intent == null");
            } else if (com.huawei.android.pushselfshow.utils.a.a(this.b, -l_1_R)) {
                if (-l_2_I == 0) {
                    -l_1_R.setFlags(805437440);
                } else {
                    -l_1_R.addFlags(268435456);
                }
                c.b("PushSelfShowLog", "start " + -l_1_R.toURI());
                this.b.startActivity(-l_1_R);
            } else {
                c.c("PushSelfShowLog", "no permission to start Activity");
            }
        } catch (Object -l_1_R2) {
            c.d("PushSelfShowLog", -l_1_R2.toString(), -l_1_R2);
        }
    }

    private void i() {
        Object -l_1_R;
        try {
            c.e("PushSelfShowLog", "run into launchRichPush ");
            -l_1_R = new Intent();
            -l_1_R.setComponent(new ComponentName(this.b.getPackageName(), "com.huawei.android.pushselfshow.richpush.RichPushHtmlActivity"));
            -l_1_R.putExtra("type", this.c.y());
            -l_1_R.putExtra("selfshow_info", this.c.c());
            -l_1_R.putExtra("selfshow_token", this.c.d());
            -l_1_R.setFlags(268468240);
            -l_1_R.setPackage(this.b.getPackageName());
            this.b.startActivity(-l_1_R);
        } catch (Object -l_1_R2) {
            c.d("PushSelfShowLog", "launchRichPush failed", -l_1_R2);
        }
    }

    public void a() {
        c.a("PushSelfShowLog", "enter launchNotify()");
        if (this.b == null || this.c == null) {
            c.a("PushSelfShowLog", "launchNotify  context or msg is null");
            return;
        }
        if ("app".equals(this.c.m())) {
            e();
        } else {
            if ("cosa".equals(this.c.m())) {
                h();
            } else {
                if ("email".equals(this.c.m())) {
                    d();
                } else {
                    if ("phone".equals(this.c.m())) {
                        c();
                    } else {
                        if ("rp".equals(this.c.m())) {
                            i();
                        } else {
                            if (CheckVersionField.CHECK_VERSION_SERVER_URL.equals(this.c.m())) {
                                b();
                            } else {
                                c.a("PushSelfShowLog", this.c.m() + " is not exist in hShowType");
                            }
                        }
                    }
                }
            }
        }
    }
}
