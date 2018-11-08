package com.huawei.android.pushselfshow.c;

import android.text.TextUtils;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.io.Serializable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class a implements Serializable {
    private String A;
    private String B = "";
    private String C;
    private String D;
    private String E;
    private String F;
    private String G;
    private String H = "";
    private int I = 1;
    private int J = 0;
    private String K;
    private String L;
    private String M;
    private int N = com.huawei.android.pushselfshow.d.a.STYLE_1.ordinal();
    private int O = 0;
    private String[] P = null;
    private String[] Q = null;
    private String[] R = null;
    private int S = 0;
    private String[] T = null;
    private String U = "";
    private String V = "";
    public int a = 1;
    public String b = "";
    private String c = "";
    private String d;
    private String e;
    private String f;
    private String g;
    private int h;
    private String i;
    private int j;
    private String k;
    private int l;
    private int m;
    private String n;
    private String o = "";
    private String p = "";
    private String q;
    private String r = "";
    private String s = "";
    private String t = "";
    private String u = "";
    private String v;
    private String w;
    private String x;
    private String y;
    private String z;

    public a(byte[] bArr, byte[] bArr2) {
        try {
            this.L = new String(bArr, "UTF-8");
            this.M = new String(bArr2, "UTF-8");
        } catch (Exception e) {
            c.d("PushSelfShowLog", "get msg byte arr error");
        }
    }

    private boolean a(JSONObject jSONObject) {
        try {
            JSONObject -l_2_R = jSONObject.getJSONObject("param");
            if (-l_2_R.has("autoClear")) {
                this.h = -l_2_R.getInt("autoClear");
            } else {
                this.h = 0;
            }
            if (!"app".equals(this.r)) {
                if (!"cosa".equals(this.r)) {
                    if ("email".equals(this.r)) {
                        c(-l_2_R);
                    } else {
                        if (!"phone".equals(this.r)) {
                            if (CheckVersionField.CHECK_VERSION_SERVER_URL.equals(this.r)) {
                                d(-l_2_R);
                            } else {
                                if ("rp".equals(this.r)) {
                                    e(-l_2_R);
                                }
                            }
                        } else if (-l_2_R.has("phoneNum")) {
                            this.y = -l_2_R.getString("phoneNum");
                        } else {
                            c.a("PushSelfShowLog", "phoneNum is null");
                            return false;
                        }
                    }
                    return true;
                }
            }
            b(-l_2_R);
            return true;
        } catch (Object -l_2_R2) {
            c.d("PushSelfShowLog", "ParseParam error ", -l_2_R2);
            return false;
        }
    }

    private boolean b(JSONObject jSONObject) throws JSONException {
        if (jSONObject == null) {
            return false;
        }
        if (jSONObject.has("acn")) {
            this.D = jSONObject.getString("acn");
            this.i = this.D;
        }
        if (jSONObject.has("intentUri")) {
            this.i = jSONObject.getString("intentUri");
        }
        if (jSONObject.has("appPackageName")) {
            this.C = jSONObject.getString("appPackageName");
            return true;
        }
        c.a("PushSelfShowLog", "appPackageName is null");
        return false;
    }

    private boolean c(JSONObject jSONObject) throws JSONException {
        if (jSONObject == null) {
            return false;
        }
        if (jSONObject.has("emailAddr") && jSONObject.has("emailSubject")) {
            this.z = jSONObject.getString("emailAddr");
            this.A = jSONObject.getString("emailSubject");
            if (jSONObject.has("emailContent")) {
                this.B = jSONObject.getString("emailContent");
            }
            return true;
        }
        c.a("PushSelfShowLog", "emailAddr or emailSubject is null");
        return false;
    }

    private boolean d(JSONObject jSONObject) throws JSONException {
        if (jSONObject == null) {
            return false;
        }
        if (jSONObject.has(CheckVersionField.CHECK_VERSION_SERVER_URL)) {
            this.E = jSONObject.getString(CheckVersionField.CHECK_VERSION_SERVER_URL);
            if (jSONObject.has("inBrowser")) {
                this.I = jSONObject.getInt("inBrowser");
            }
            if (jSONObject.has("needUserId")) {
                this.J = jSONObject.getInt("needUserId");
            }
            if (jSONObject.has("sign")) {
                this.K = jSONObject.getString("sign");
            }
            if (jSONObject.has("rpt") && jSONObject.has("rpl")) {
                this.F = jSONObject.getString("rpl");
                this.G = jSONObject.getString("rpt");
                if (jSONObject.has("rpct")) {
                    this.H = jSONObject.getString("rpct");
                }
            }
            return true;
        }
        c.a("PushSelfShowLog", "url is null");
        return false;
    }

    private boolean e(JSONObject jSONObject) throws JSONException {
        if (jSONObject == null) {
            return false;
        }
        if (jSONObject.has("rpt") && jSONObject.has("rpl")) {
            this.F = jSONObject.getString("rpl");
            this.G = jSONObject.getString("rpt");
            if (jSONObject.has("rpct")) {
                this.H = jSONObject.getString("rpct");
            }
            if (jSONObject.has("needUserId")) {
                this.J = jSONObject.getInt("needUserId");
            }
            return true;
        }
        c.a("PushSelfShowLog", "rpl or rpt is null");
        return false;
    }

    private boolean f(JSONObject jSONObject) {
        c.a("PushSelfShowLog", "enter parseNotifyParam");
        Object -l_2_R;
        try {
            -l_2_R = jSONObject.getJSONObject("notifyParam");
            if (!-l_2_R.has("style")) {
                return false;
            }
            Object -l_5_R;
            this.N = -l_2_R.getInt("style");
            c.a("PushSelfShowLog", "style:" + this.N);
            if (-l_2_R.has("btnCount")) {
                this.O = -l_2_R.getInt("btnCount");
            }
            if (this.O > 0) {
                if (this.O > 3) {
                    this.O = 3;
                }
                c.a("PushSelfShowLog", "btnCount:" + this.O);
                this.P = new String[this.O];
                this.Q = new String[this.O];
                this.R = new String[this.O];
                for (int -l_3_I = 0; -l_3_I < this.O; -l_3_I++) {
                    Object -l_4_R = "btn" + (-l_3_I + 1) + "Text";
                    -l_5_R = "btn" + (-l_3_I + 1) + "Image";
                    Object -l_6_R = "btn" + (-l_3_I + 1) + "Event";
                    if (-l_2_R.has(-l_4_R)) {
                        this.P[-l_3_I] = -l_2_R.getString(-l_4_R);
                    }
                    if (-l_2_R.has(-l_5_R)) {
                        this.Q[-l_3_I] = -l_2_R.getString(-l_5_R);
                    }
                    if (-l_2_R.has(-l_6_R)) {
                        this.R[-l_3_I] = -l_2_R.getString(-l_6_R);
                    }
                }
            }
            Object -l_3_R = com.huawei.android.pushselfshow.d.a.STYLE_1;
            if (this.N >= 0 && this.N < com.huawei.android.pushselfshow.d.a.values().length) {
                -l_3_R = com.huawei.android.pushselfshow.d.a.values()[this.N];
            }
            switch (-l_3_R) {
                case STYLE_4:
                    if (-l_2_R.has("iconCount")) {
                        this.S = -l_2_R.getInt("iconCount");
                    }
                    if (this.S > 0) {
                        if (this.S > 6) {
                            this.S = 6;
                        }
                        c.a("PushSelfShowLog", "iconCount:" + this.S);
                        this.T = new String[this.S];
                        for (int -l_4_I = 0; -l_4_I < this.S; -l_4_I++) {
                            -l_5_R = "icon" + (-l_4_I + 1);
                            if (-l_2_R.has(-l_5_R)) {
                                this.T[-l_4_I] = -l_2_R.getString(-l_5_R);
                            }
                        }
                        break;
                    }
                    break;
                case STYLE_5:
                    if (-l_2_R.has("subTitle")) {
                        this.U = -l_2_R.getString("subTitle");
                        c.a("PushSelfShowLog", "subTitle:" + this.U);
                        break;
                    }
                    break;
                case STYLE_6:
                case STYLE_8:
                    if (-l_2_R.has("bigPic")) {
                        this.V = -l_2_R.getString("bigPic");
                        c.a("PushSelfShowLog", "bigPicUrl:" + this.V);
                        break;
                    }
                    break;
            }
            return true;
        } catch (Object -l_2_R2) {
            c.b("PushSelfShowLog", -l_2_R2.toString());
            return false;
        }
    }

    public int A() {
        return this.I;
    }

    public int B() {
        return this.J;
    }

    public String C() {
        return this.K;
    }

    public int D() {
        return this.N;
    }

    public String[] E() {
        return this.P;
    }

    public String[] F() {
        return this.Q;
    }

    public String[] G() {
        return this.R;
    }

    public String[] H() {
        return this.T;
    }

    public String I() {
        return this.U;
    }

    public String J() {
        return this.V;
    }

    public String K() {
        return this.M;
    }

    public int L() {
        return this.a;
    }

    public String M() {
        return this.b;
    }

    public String a() {
        c.a("PushSelfShowLog", "msgId =" + this.o);
        return this.o;
    }

    public void a(String str) {
        this.p = str;
    }

    public void b(String str) {
        this.r = str;
    }

    public boolean b() {
        try {
            if (this.M == null || this.M.length() == 0) {
                c.a("PushSelfShowLog", "token is null");
                return false;
            }
            this.k = this.M;
            if (this.L == null || this.L.length() == 0) {
                c.a("PushSelfShowLog", "msg is null");
                return false;
            }
            Object -l_1_R = new JSONObject(this.L);
            this.j = -l_1_R.getInt("msgType");
            if (this.j == 1) {
                if (-l_1_R.has("group")) {
                    this.c = -l_1_R.getString("group");
                    c.a("PushSelfShowLog", "NOTIFY_GROUP:" + this.c);
                }
                if (-l_1_R.has("badgeClass")) {
                    this.b = -l_1_R.getString("badgeClass");
                    c.a("PushSelfShowLog", "BADGE_CLASS:" + this.b);
                }
                if (-l_1_R.has("badgeAddNum")) {
                    this.a = -l_1_R.getInt("badgeAddNum");
                    c.a("PushSelfShowLog", "BADGE_ADD_NUM:" + this.a);
                }
                Object -l_2_R = -l_1_R.getJSONObject("msgContent");
                if (-l_2_R == null) {
                    c.b("PushSelfShowLog", "msgObj == null");
                    return false;
                } else if (-l_2_R.has("msgId")) {
                    Object -l_3_R = -l_2_R.get("msgId");
                    if (-l_3_R instanceof String) {
                        this.o = (String) -l_3_R;
                    } else if (-l_3_R instanceof Integer) {
                        this.o = String.valueOf(((Integer) -l_3_R).intValue());
                    }
                    if (-l_2_R.has("dispPkgName")) {
                        this.p = -l_2_R.getString("dispPkgName");
                    }
                    if (-l_2_R.has("rtn")) {
                        this.m = -l_2_R.getInt("rtn");
                    } else {
                        this.m = 1;
                    }
                    if (-l_2_R.has("fm")) {
                        this.l = -l_2_R.getInt("fm");
                    } else {
                        this.l = 1;
                    }
                    if (-l_2_R.has("ap")) {
                        -l_3_R = -l_2_R.getString("ap");
                        Object -l_4_R = new StringBuilder();
                        if (!TextUtils.isEmpty(-l_3_R) && -l_3_R.length() < 48) {
                            for (int -l_5_I = 0; -l_5_I < 48 - -l_3_R.length(); -l_5_I++) {
                                -l_4_R.append("0");
                            }
                            -l_4_R.append(-l_3_R);
                            this.n = -l_4_R.toString();
                        } else {
                            this.n = -l_3_R.substring(0, 48);
                        }
                    }
                    if (-l_2_R.has("extras")) {
                        this.q = -l_2_R.getJSONArray("extras").toString();
                    }
                    if (!-l_2_R.has("psContent")) {
                        return false;
                    }
                    JSONObject -l_3_R2 = -l_2_R.getJSONObject("psContent");
                    if (-l_3_R2 == null) {
                        return false;
                    }
                    this.r = -l_3_R2.getString("cmd");
                    if (-l_3_R2.has("content")) {
                        this.s = -l_3_R2.getString("content");
                    } else {
                        this.s = "";
                    }
                    if (-l_3_R2.has("notifyIcon")) {
                        this.t = -l_3_R2.getString("notifyIcon");
                    } else {
                        this.t = "" + this.o;
                    }
                    if (-l_3_R2.has("statusIcon")) {
                        this.v = -l_3_R2.getString("statusIcon");
                    }
                    if (-l_3_R2.has("notifyTitle")) {
                        this.u = -l_3_R2.getString("notifyTitle");
                    }
                    if (-l_3_R2.has("notifyParam")) {
                        f(-l_3_R2);
                    }
                    return !-l_3_R2.has("param") ? false : a(-l_3_R2);
                } else {
                    c.b("PushSelfShowLog", "msgId == null");
                    return false;
                }
            }
            c.a("PushSelfShowLog", "not a selefShowMsg");
            return false;
        } catch (Throwable -l_1_R2) {
            c.a("PushSelfShowLog", -l_1_R2.toString(), -l_1_R2);
            return false;
        }
    }

    public void c(String str) {
        this.E = str;
    }

    public byte[] c() {
        try {
            Object -l_1_R = "";
            Object -l_2_R = new JSONObject();
            Object -l_3_R = new JSONObject();
            Object -l_4_R = new JSONObject();
            Object -l_5_R = new JSONObject();
            -l_5_R.put("autoClear", this.h);
            -l_5_R.put("s", this.d);
            -l_5_R.put("r", this.e);
            -l_5_R.put("smsC", this.f);
            -l_5_R.put("mmsUrl", this.g);
            -l_5_R.put(CheckVersionField.CHECK_VERSION_SERVER_URL, this.E);
            -l_5_R.put("inBrowser", this.I);
            -l_5_R.put("needUserId", this.J);
            -l_5_R.put("sign", this.K);
            -l_5_R.put("rpl", this.F);
            -l_5_R.put("rpt", this.G);
            -l_5_R.put("rpct", this.H);
            -l_5_R.put("appPackageName", this.C);
            -l_5_R.put("acn", this.D);
            -l_5_R.put("intentUri", this.i);
            -l_5_R.put("emailAddr", this.z);
            -l_5_R.put("emailSubject", this.A);
            -l_5_R.put("emailContent", this.B);
            -l_5_R.put("phoneNum", this.y);
            -l_5_R.put("replyToSms", this.x);
            -l_5_R.put("smsNum", this.w);
            -l_4_R.put("cmd", this.r);
            -l_4_R.put("content", this.s);
            -l_4_R.put("notifyIcon", this.t);
            -l_4_R.put("notifyTitle", this.u);
            -l_4_R.put("statusIcon", this.v);
            -l_4_R.put("param", -l_5_R);
            -l_3_R.put("dispPkgName", this.p);
            -l_3_R.put("msgId", this.o);
            -l_3_R.put("fm", this.l);
            -l_3_R.put("ap", this.n);
            -l_3_R.put("rtn", this.m);
            -l_3_R.put("psContent", -l_4_R);
            if (this.q != null && this.q.length() > 0) {
                -l_3_R.put("extras", new JSONArray(this.q));
            }
            -l_2_R.put("msgType", this.j);
            -l_2_R.put("msgContent", -l_3_R);
            -l_2_R.put("group", this.c);
            -l_2_R.put("badgeClass", this.b);
            -l_2_R.put("badgeAddNum", this.a);
            return -l_2_R.toString().getBytes("UTF-8");
        } catch (Throwable -l_1_R2) {
            c.a("PushSelfShowLog", "getMsgData failed JSONException:", -l_1_R2);
            return new byte[0];
        } catch (Throwable -l_1_R22) {
            c.a("PushSelfShowLog", "getMsgData failed UnsupportedEncodingException:", -l_1_R22);
            return new byte[0];
        }
    }

    public void d(String str) {
        this.F = str;
    }

    public byte[] d() {
        try {
            if (this.k != null) {
                if (this.k.length() > 0) {
                    return this.k.getBytes("UTF-8");
                }
            }
        } catch (Throwable -l_1_R) {
            c.a("PushSelfShowLog", "getToken getByte failed ", -l_1_R);
        }
        return new byte[0];
    }

    public String e() {
        return this.c;
    }

    public void e(String str) {
        this.G = str;
    }

    public int f() {
        return this.h;
    }

    public void f(String str) {
        this.H = str;
    }

    public String g() {
        return this.i;
    }

    public void g(String str) {
        this.o = str;
    }

    public int h() {
        return this.l;
    }

    public int i() {
        return this.m;
    }

    public String j() {
        return this.n;
    }

    public String k() {
        return this.p;
    }

    public String l() {
        return this.q;
    }

    public String m() {
        return this.r;
    }

    public String n() {
        return this.s;
    }

    public String o() {
        return this.t;
    }

    public String p() {
        return this.u;
    }

    public String q() {
        return this.y;
    }

    public String r() {
        return this.z;
    }

    public String s() {
        return this.A;
    }

    public String t() {
        return this.B;
    }

    public String u() {
        return this.C;
    }

    public String v() {
        return this.D;
    }

    public String w() {
        return this.E;
    }

    public String x() {
        return this.F;
    }

    public String y() {
        return this.G;
    }

    public String z() {
        return this.H;
    }
}
