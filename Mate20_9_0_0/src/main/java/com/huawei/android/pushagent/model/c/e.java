package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.http.server.TrsRsp;
import com.huawei.android.pushagent.model.flowcontrol.ReconnectMgr$RECONNECTEVENT;
import com.huawei.android.pushagent.model.flowcontrol.b;
import com.huawei.android.pushagent.model.prefs.a;
import com.huawei.android.pushagent.model.prefs.g;
import com.huawei.android.pushagent.utils.d;
import com.huawei.android.pushagent.utils.threadpool.c;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Date;

public class e {
    private static final byte[] fs = new byte[0];
    private static e ft = null;
    private Context appCtx;
    private boolean fu = false;
    private a fv = null;

    public static e pw(Context context) {
        e eVar;
        synchronized (fs) {
            if (ft == null) {
                ft = new e(context);
            }
            eVar = ft;
        }
        return eVar;
    }

    private e(Context context) {
        this.appCtx = context.getApplicationContext();
        this.fv = a.ff(context);
    }

    public InetSocketAddress px() {
        boolean py = py();
        if (!this.fv.isValid() || py) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "in getPushSrvAddr, have no invalid addr");
            return null;
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "return valid PushSrvAddr");
        return new InetSocketAddress(this.fv.getServerIP(), this.fv.getServerPort());
    }

    public void qd() {
        a.ff(this.appCtx).ii(0);
        this.fu = true;
    }

    /* JADX WARNING: Missing block: B:19:0x0071, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean py() {
        if (-1 == d.yh(this.appCtx)) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "in queryTRSInfo no network");
            return false;
        }
        long currentTimeMillis = System.currentTimeMillis() - com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).jq();
        if (this.fv.isNotAllowedPush() && currentTimeMillis > 0 && currentTimeMillis < this.fv.getNextConnectTrsInterval()) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "Not allow to use push in this area, and not pass interval,result code: " + this.fv.getResult());
            return false;
        } else if (!qb() || !pz() || (qa() ^ 1) != 0 || !com.huawei.android.pushagent.model.flowcontrol.a.nk(this.appCtx)) {
            return false;
        } else {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "need Connect TRS");
            return qc();
        }
    }

    private synchronized boolean qc() {
        c.sp(new g(this));
        com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).jz(System.currentTimeMillis());
        com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).ka(com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).js() + 1);
        return true;
    }

    private boolean qe(TrsRsp trsRsp) {
        if (trsRsp == null || (trsRsp.isValid() ^ 1) != 0) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "in PushSrvInfo:trsConfig, trsConfig is null or invalid");
            return false;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "queryTrs success!");
        qf(trsRsp.getAnalyticUrl());
        trsRsp.encryptRsaPubKey();
        trsRsp.encryptConnectionId();
        trsRsp.encryptDeviceId();
        String deviceId = trsRsp.getDeviceId();
        trsRsp.removeDeviceId();
        if (!TextUtils.isEmpty(deviceId)) {
            g.kp(this.appCtx).kz(deviceId);
        }
        this.fv.lf(trsRsp.getAll());
        this.fv.ii((this.fv.hv() * 1000) + System.currentTimeMillis());
        com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).ka(0);
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "write the lastQueryTRSsucc_time to the pushConfig.xml file ");
        com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).jy(System.currentTimeMillis());
        this.fu = false;
        b.nz(this.appCtx).ob(this.appCtx, ReconnectMgr$RECONNECTEVENT.TRS_QUERIED, new Bundle());
        PushService.abv(new Intent("com.huawei.android.push.intent.TRS_QUERY_SUCCESS"));
        return true;
    }

    private void qf(String str) {
        if (TextUtils.isEmpty(str)) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "analytic url is empty");
            return;
        }
        String analyticUrl = this.fv.getAnalyticUrl();
        long ju = com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).ju();
        long currentTimeMillis = System.currentTimeMillis();
        if (!str.equals(analyticUrl) || currentTimeMillis - ju >= this.fv.fv()) {
            com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).kc(currentTimeMillis);
            Intent intent = new Intent("com.huawei.android.push.intent.NC_CONTROL_INFO");
            intent.putExtra("control_type", 1);
            intent.setPackage("com.huawei.android.pushagent");
            intent.putExtra("report_url", str);
            try {
                intent.putExtra("device_id", com.huawei.android.pushagent.utils.e.a.vv(d.yq(this.appCtx), d.zd().getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "fail to encrypt deviceId");
            }
            d.zq(this.appCtx, intent, com.huawei.android.pushagent.utils.a.xy());
            return;
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "analytic url not changed and not pass interval");
    }

    private boolean qb() {
        long hy = this.fv.hy();
        long jp = com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).jp();
        long jq = com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).jq();
        if (this.fv.isValid() && (this.fu ^ 1) != 0 && hy >= System.currentTimeMillis() && System.currentTimeMillis() > jp) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "config still valid, need not query TRS");
            return false;
        } else if (!this.fv.isValid() || !this.fu || System.currentTimeMillis() - jq >= this.fv.hw() * 1000 || System.currentTimeMillis() <= jq) {
            return true;
        } else {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", " cannot query TRS in trsValid_min");
            return false;
        }
    }

    private boolean pz() {
        long jp = com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).jp();
        if (System.currentTimeMillis() - jp >= this.fv.hw() * 1000 || System.currentTimeMillis() <= jp) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "can not contect TRS Service when  the connect more than " + this.fv.hw() + " sec last contected success time," + "lastQueryTRSsucc_time = " + new Date(jp));
        return false;
    }

    private boolean qa() {
        long jq = com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).jq();
        long gd = this.fv.gd() * 1000;
        if (com.huawei.android.pushagent.model.prefs.e.jj(this.appCtx).js() > this.fv.gq()) {
            gd = this.fv.gc() * 1000;
        }
        if (System.currentTimeMillis() - jq >= gd || System.currentTimeMillis() <= jq) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "can't connect TRS, not exceed retry interval, " + (gd / 1000) + "sec than  last contectting time,lastQueryTRSTime =" + new Date(jq));
        return false;
    }
}
