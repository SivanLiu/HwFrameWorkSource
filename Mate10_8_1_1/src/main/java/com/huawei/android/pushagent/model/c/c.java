package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.http.server.TrsRsp;
import com.huawei.android.pushagent.model.flowcontrol.ReconnectMgr$RECONNECTEVENT;
import com.huawei.android.pushagent.model.flowcontrol.a;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Date;

public class c {
    private static final byte[] ei = new byte[0];
    private static c ej = null;
    private Context appCtx;
    private boolean ek = false;
    private i el = null;

    public static c sp(Context context) {
        c cVar;
        synchronized (ei) {
            if (ej == null) {
                ej = new c(context);
            }
            cVar = ej;
        }
        return cVar;
    }

    private c(Context context) {
        this.appCtx = context.getApplicationContext();
        this.el = i.mj(context);
    }

    public InetSocketAddress sv(boolean z) {
        boolean sq = sq(z);
        if (!this.el.isValid() || sq) {
            b.z("PushLog2976", "in getPushSrvAddr, have no invalid addr");
            return null;
        }
        b.x("PushLog2976", "return valid PushSrvAddr");
        return new InetSocketAddress(this.el.getServerIP(), this.el.getServerPort());
    }

    public void sr() {
        i.mj(this.appCtx).nt(0);
        this.ek = true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean sq(boolean z) {
        if (-1 == f.fp(this.appCtx)) {
            b.z("PushLog2976", "in queryTRSInfo no network");
            return false;
        }
        long currentTimeMillis = System.currentTimeMillis() - k.pt(this.appCtx).qb();
        if (this.el.isNotAllowedPush() && currentTimeMillis > 0 && currentTimeMillis < this.el.getNextConnectTrsInterval()) {
            b.z("PushLog2976", "Not allow to use push in this area, and not pass interval,result code: " + this.el.getResult());
            return false;
        } else if (!su()) {
            return false;
        } else {
            if ((!z && (!ss() || (st() ^ 1) != 0)) || !a.hp(this.appCtx)) {
                return false;
            }
            b.z("PushLog2976", "need Connect TRS");
            return sw();
        }
    }

    private synchronized boolean sw() {
        com.huawei.android.pushagent.utils.threadpool.b.d(new g(this));
        k.pt(this.appCtx).qc(System.currentTimeMillis());
        k.pt(this.appCtx).qe(k.pt(this.appCtx).qd() + 1);
        return true;
    }

    private boolean sx(TrsRsp trsRsp) {
        if (trsRsp == null || (trsRsp.isValid() ^ 1) != 0) {
            b.y("PushLog2976", "in PushSrvInfo:trsConfig, trsConfig is null or invalid");
            return false;
        }
        b.z("PushLog2976", "queryTrs success!");
        sy(trsRsp.getAnalyticUrl());
        trsRsp.encryptRsaPubKey();
        trsRsp.encryptConnectionId();
        trsRsp.encryptDeviceId();
        Object deviceId = trsRsp.getDeviceId();
        trsRsp.removeDeviceId();
        if (!TextUtils.isEmpty(deviceId)) {
            com.huawei.android.pushagent.model.prefs.b.kp(this.appCtx).kv(deviceId);
        }
        this.el.lx(trsRsp.getAll());
        this.el.nt((this.el.nw() * 1000) + System.currentTimeMillis());
        k.pt(this.appCtx).qe(0);
        b.x("PushLog2976", "write the lastQueryTRSsucc_time to the pushConfig.xml file ");
        k.pt(this.appCtx).qf(System.currentTimeMillis());
        this.ek = false;
        com.huawei.android.pushagent.model.flowcontrol.b.ib(this.appCtx).ik(this.appCtx, ReconnectMgr$RECONNECTEVENT.TRS_QUERIED, new Bundle());
        PushService.aax(new Intent("com.huawei.android.push.intent.TRS_QUERY_SUCCESS"));
        return true;
    }

    private void sy(String str) {
        if (TextUtils.isEmpty(str)) {
            b.y("PushLog2976", "analytic url is empty");
            return;
        }
        String analyticUrl = this.el.getAnalyticUrl();
        long qg = k.pt(this.appCtx).qg();
        long currentTimeMillis = System.currentTimeMillis();
        if (!str.equals(analyticUrl) || currentTimeMillis - qg >= this.el.nx()) {
            k.pt(this.appCtx).qh(currentTimeMillis);
            Intent intent = new Intent("com.huawei.android.push.intent.NC_CONTROL_INFO");
            intent.putExtra("control_type", 1);
            intent.setPackage("com.huawei.android.pushagent");
            intent.putExtra("report_url", str);
            try {
                intent.putExtra("device_id", com.huawei.android.pushagent.utils.c.c.bd(f.gk(this.appCtx), f.fy().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                b.y("PushLog2976", "fail to encrypt deviceId");
            }
            f.fw(this.appCtx, intent, com.huawei.android.pushagent.utils.a.fc());
            return;
        }
        b.x("PushLog2976", "analytic url not changed and not pass interval");
    }

    private boolean su() {
        long ny = this.el.ny();
        long pu = k.pt(this.appCtx).pu();
        long qb = k.pt(this.appCtx).qb();
        if (this.el.isValid() && (this.ek ^ 1) != 0 && ny >= System.currentTimeMillis() && System.currentTimeMillis() > pu) {
            b.z("PushLog2976", "config still valid, need not query TRS");
            return false;
        } else if (!this.el.isValid() || !this.ek || System.currentTimeMillis() - qb >= this.el.nz() * 1000 || System.currentTimeMillis() <= qb) {
            return true;
        } else {
            b.z("PushLog2976", " cannot query TRS in trsValid_min");
            return false;
        }
    }

    private boolean ss() {
        long pu = k.pt(this.appCtx).pu();
        if (System.currentTimeMillis() - pu >= this.el.nz() * 1000 || System.currentTimeMillis() <= pu) {
            return true;
        }
        b.z("PushLog2976", "can not contect TRS Service when  the connect more than " + this.el.nz() + " sec last contected success time," + "lastQueryTRSsucc_time = " + new Date(pu));
        return false;
    }

    private boolean st() {
        long qb = k.pt(this.appCtx).qb();
        long oa = this.el.oa() * 1000;
        if (k.pt(this.appCtx).qd() > this.el.ob()) {
            oa = this.el.oc() * 1000;
        }
        if (System.currentTimeMillis() - qb >= oa || System.currentTimeMillis() <= qb) {
            return true;
        }
        b.z("PushLog2976", "can't connect TRS, not exceed retry interval, " + (oa / 1000) + "sec than  last contectting time,lastQueryTRSTime =" + new Date(qb));
        return false;
    }
}
