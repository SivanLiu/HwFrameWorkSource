package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.http.server.TrsRsp;
import com.huawei.android.pushagent.model.flowcontrol.ReconnectMgr$RECONNECTEVENT;
import com.huawei.android.pushagent.model.prefs.b;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.threadpool.a;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Date;

public class f {
    private static final byte[] hd = new byte[0];
    private static f he = null;
    private Context appCtx;
    private boolean hf = false;
    private k hg = null;

    public static f yc(Context context) {
        f fVar;
        synchronized (hd) {
            if (he == null) {
                he = new f(context);
            }
            fVar = he;
        }
        return fVar;
    }

    private f(Context context) {
        this.appCtx = context.getApplicationContext();
        this.hg = k.rh(context);
    }

    public InetSocketAddress yi() {
        boolean yd = yd();
        if (!this.hg.isValid() || yd) {
            c.ep("PushLog3413", "in getPushSrvAddr, have no invalid addr");
            return null;
        }
        c.er("PushLog3413", "return valid PushSrvAddr");
        return new InetSocketAddress(this.hg.getServerIP(), this.hg.getServerPort());
    }

    public void ye() {
        k.rh(this.appCtx).uk(0);
        this.hf = true;
    }

    /* JADX WARNING: Missing block: B:19:0x0071, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean yd() {
        if (-1 == g.fw(this.appCtx)) {
            c.ep("PushLog3413", "in queryTRSInfo no network");
            return false;
        }
        long currentTimeMillis = System.currentTimeMillis() - l.ul(this.appCtx).ux();
        if (this.hg.isNotAllowedPush() && currentTimeMillis > 0 && currentTimeMillis < this.hg.getNextConnectTrsInterval()) {
            c.ep("PushLog3413", "Not allow to use push in this area, and not pass interval,result code: " + this.hg.getResult());
            return false;
        } else if (!yh() || !yf() || (yg() ^ 1) != 0 || !com.huawei.android.pushagent.model.flowcontrol.c.aba(this.appCtx)) {
            return false;
        } else {
            c.ep("PushLog3413", "need Connect TRS");
            return yj();
        }
    }

    private synchronized boolean yj() {
        a.ck(new g(this));
        l.ul(this.appCtx).ut(System.currentTimeMillis());
        l.ul(this.appCtx).vd(l.ul(this.appCtx).uz() + 1);
        return true;
    }

    private boolean yk(TrsRsp trsRsp) {
        if (trsRsp == null || (trsRsp.isValid() ^ 1) != 0) {
            c.eq("PushLog3413", "in PushSrvInfo:trsConfig, trsConfig is null or invalid");
            return false;
        }
        c.ep("PushLog3413", "queryTrs success!");
        yl(trsRsp.getAnalyticUrl());
        trsRsp.encryptRsaPubKey();
        trsRsp.encryptConnectionId();
        trsRsp.encryptDeviceId();
        Object deviceId = trsRsp.getDeviceId();
        trsRsp.removeDeviceId();
        if (!TextUtils.isEmpty(deviceId)) {
            b.oq(this.appCtx).pa(deviceId);
        }
        this.hg.vu(trsRsp.getAll());
        this.hg.uk((this.hg.uf() * 1000) + System.currentTimeMillis());
        l.ul(this.appCtx).vd(0);
        c.er("PushLog3413", "write the lastQueryTRSsucc_time to the pushConfig.xml file ");
        l.ul(this.appCtx).uu(System.currentTimeMillis());
        this.hf = false;
        com.huawei.android.pushagent.model.flowcontrol.a.zx(this.appCtx).aag(this.appCtx, ReconnectMgr$RECONNECTEVENT.TRS_QUERIED, new Bundle());
        PushService.abr(new Intent("com.huawei.android.push.intent.TRS_QUERY_SUCCESS"));
        return true;
    }

    private void yl(String str) {
        if (TextUtils.isEmpty(str)) {
            c.eq("PushLog3413", "analytic url is empty");
            return;
        }
        String analyticUrl = this.hg.getAnalyticUrl();
        long vb = l.ul(this.appCtx).vb();
        long currentTimeMillis = System.currentTimeMillis();
        if (!str.equals(analyticUrl) || currentTimeMillis - vb >= this.hg.tq()) {
            l.ul(this.appCtx).vf(currentTimeMillis);
            Intent intent = new Intent("com.huawei.android.push.intent.NC_CONTROL_INFO");
            intent.putExtra("control_type", 1);
            intent.setPackage("com.huawei.android.pushagent");
            intent.putExtra("report_url", str);
            try {
                intent.putExtra("device_id", com.huawei.android.pushagent.utils.a.c.o(g.gi(this.appCtx), g.go().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                c.eq("PushLog3413", "fail to encrypt deviceId");
            }
            g.gm(this.appCtx, intent, com.huawei.android.pushagent.utils.a.fb());
            return;
        }
        c.er("PushLog3413", "analytic url not changed and not pass interval");
    }

    private boolean yh() {
        long uh = this.hg.uh();
        long uv = l.ul(this.appCtx).uv();
        long ux = l.ul(this.appCtx).ux();
        if (this.hg.isValid() && (this.hf ^ 1) != 0 && uh >= System.currentTimeMillis() && System.currentTimeMillis() > uv) {
            c.ep("PushLog3413", "config still valid, need not query TRS");
            return false;
        } else if (!this.hg.isValid() || !this.hf || System.currentTimeMillis() - ux >= this.hg.ug() * 1000 || System.currentTimeMillis() <= ux) {
            return true;
        } else {
            c.ep("PushLog3413", " cannot query TRS in trsValid_min");
            return false;
        }
    }

    private boolean yf() {
        long uv = l.ul(this.appCtx).uv();
        if (System.currentTimeMillis() - uv >= this.hg.ug() * 1000 || System.currentTimeMillis() <= uv) {
            return true;
        }
        c.ep("PushLog3413", "can not contect TRS Service when  the connect more than " + this.hg.ug() + " sec last contected success time," + "lastQueryTRSsucc_time = " + new Date(uv));
        return false;
    }

    private boolean yg() {
        long ux = l.ul(this.appCtx).ux();
        long tt = this.hg.tt() * 1000;
        if (l.ul(this.appCtx).uz() > this.hg.tw()) {
            tt = this.hg.ts() * 1000;
        }
        if (System.currentTimeMillis() - ux >= tt || System.currentTimeMillis() <= ux) {
            return true;
        }
        c.ep("PushLog3413", "can't connect TRS, not exceed retry interval, " + (tt / 1000) + "sec than  last contectting time,lastQueryTRSTime =" + new Date(ux));
        return false;
    }
}
