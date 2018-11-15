package com.huawei.android.pushagent.model.channel.entity.a;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatReqMessage;
import com.huawei.android.pushagent.datatype.tcp.base.IPushMessage;
import com.huawei.android.pushagent.model.channel.entity.c;
import com.huawei.android.pushagent.model.prefs.f;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.d;

public class a extends c {
    private String ct = "";
    private int cu = 0;
    private int cv = 0;
    private String cw = "";
    private boolean cx = false;
    private long cy = 7200000;
    private long cz = this.cy;
    private long da = 180000;
    private int db = -1;
    private String dc = "";

    public a(Context context) {
        super(context);
    }

    public boolean lg() {
        boolean z = true;
        int fw = g.fw(this.ef);
        String fx = g.fx(this.ef);
        String fy = g.fy(this.ef);
        switch (fw) {
            case 0:
                String fz = g.fz(this.ef);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "push apn is " + fz);
                if (fw == this.db && (fy.equals(this.cw) ^ 1) == 0) {
                    z = fz.equals(this.ct) ^ 1;
                    break;
                }
            case 1:
                if (fw == this.db) {
                    z = this.dc.equals(fx) ^ 1;
                    break;
                }
                break;
            default:
                z = false;
                break;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "is network env changed: " + z + ",from netType " + this.db + " to " + fw + ", [-1:NONE, 0:MOBILE, 1:WIFI]");
        return z;
    }

    public long ld(boolean z) {
        if (-1 == g.fw(this.ef)) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "no network, use no network heartbeat");
            return k.rh(this.ef).ro() * 1000;
        }
        long j = this.cy;
        if (!this.cx) {
            if (z) {
                j = this.cy;
            } else {
                if (this.cy > 300000) {
                    j = k.rh(this.ef).rp();
                } else {
                    j = k.rh(this.ef).rq();
                }
                j += this.cy;
            }
            if (j <= this.da) {
                j = this.da;
            } else if (j >= this.cz) {
                j = this.cz;
            }
        }
        return j;
    }

    private void le(boolean z) {
        if (this.eg) {
            mv(false);
            if (z) {
                com.huawei.android.pushagent.model.prefs.a.of(this.ef).og();
            } else {
                com.huawei.android.pushagent.model.prefs.a.of(this.ef).oh();
            }
        }
    }

    public void ky(boolean z, boolean z2) {
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "enter adjustHeartBeat:(findHeartBeat:" + this.cx + ",isHearBeatTimeReq:" + this.eh + ",RspTimeOut:" + z + ",beatInterval:" + this.cy + ",bestHBSuccessCount:" + this.cv + ",range:[" + this.da + "," + this.cz + "]" + ")");
        le(z);
        if (this.cx) {
            if (z) {
                li(z, z2);
            } else {
                li(false, z2);
            }
        } else if (this.eh) {
            mw(false);
            this.cy = ld(z);
            if (z || this.cy <= this.da || this.cy >= this.cz) {
                this.cx = true;
                this.cv = 0;
                int fw = g.fw(this.ef);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "Find the best heartBeat Interval:" + this.cy + "ms");
                ln(this.ef, this.cy, fw);
                f.py(this.ef).pz(this.ef, fw);
                com.huawei.android.pushagent.a.a.hq(75, com.huawei.android.pushagent.a.a.hr(String.valueOf(fw), String.valueOf(this.cy)));
            } else {
                com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "set current heartBeatInterval " + this.cy + "ms");
            }
            ll();
        } else {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "It is not hearBeatTimeReq");
        }
    }

    private void li(boolean z, boolean z2) {
        if (this.eh) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "judgeFindBestHB and isBestHBTimeOut: " + z + "isOffset: " + z2);
            mw(false);
            if (f.py(this.ef).qa(this.ef, this.db) > -1) {
                if (!z) {
                    this.cu = 0;
                } else if (!z2) {
                    this.cu++;
                    lk();
                }
            } else if (z) {
                this.cv--;
                this.cu++;
                lk();
            } else {
                this.cv++;
                this.cu = 0;
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "bestHBSuccessCount:" + this.cv);
                if (k.rh(this.ef).rr() == this.cv) {
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "bestHBSuccessCount: " + this.cv + "and make sure the indeed BestHBTime: " + this.cy + "ms,reset bestHBSuccessCount");
                    f.py(this.ef).qb(this.ef, this.db, this.cy);
                    ln(this.ef, this.cy, this.db);
                }
            }
        }
    }

    private void lk() {
        if (f.py(this.ef).qc(this.ef, this.db)) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "has already back, no need back.");
            return;
        }
        if ((this.cy >= 300000 && this.cu >= 2) || (this.cy < 300000 && this.cu >= 3)) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "CurrentBestHBFailureCount:" + this.cu + ",BestHBTime need go back one step and reset two counts");
            this.cu = 0;
            this.cv = 0;
            this.cy -= 30000;
            this.cy = la(this.cy, this.da, this.cz);
            f.py(this.ef).qb(this.ef, this.db, this.cy);
            f.py(this.ef).pz(this.ef, this.db);
            f.py(this.ef).qd(this.ef, this.db, true);
            ln(this.ef, this.cy, this.db);
            ll();
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "CurrentBestHBFailureCount:" + this.cu + ",heartBeatInterval: " + this.cy);
    }

    private boolean lh(long j, int i) {
        long rs;
        if (i == 0) {
            rs = k.rh(this.ef).rs();
        } else {
            rs = k.rh(this.ef).rt();
        }
        if (j > -1 && System.currentTimeMillis() - j < rs) {
            return true;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "local best heartBeat time is:" + j + "exceed " + rs + ", netType:" + i);
        return false;
    }

    private void lf() {
        k rh = k.rh(this.ef);
        switch (this.db) {
            case 0:
                this.ct = g.fz(this.ef);
                com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "in loadHeartBeat apnName:" + this.ct);
                lc(rh, this.ct);
                break;
            case 1:
                this.da = rh.getWifiMinHeartbeat() * 1000;
                this.cz = rh.getWifiMaxHeartbeat() * 1000;
                break;
            default:
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "unKnow net type");
                break;
        }
        if (this.da < 180000) {
            this.da = 180000;
        }
    }

    public synchronized void lj() {
        if (com.huawei.android.pushagent.model.channel.a.ns() == null) {
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "system is in start, wait net for heartBeat");
            return;
        }
        k rh = k.rh(this.ef);
        this.db = g.fw(this.ef);
        if (-1 == this.db) {
            this.cy = rh.ro() * 1000;
            return;
        }
        this.cv = 0;
        this.cu = 0;
        this.dc = g.fx(this.ef);
        this.cw = g.fy(this.ef);
        lf();
        long qa = f.py(this.ef).qa(this.ef, this.db);
        long qe = f.py(this.ef).qe(this.ef, this.db);
        this.cx = f.py(this.ef).qf(this.ef, this.db);
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "reload heartbeat info, [lastBestHBTime: " + qe + ",hasFindHeartBeat: " + this.cx + ",localBestHB: " + qa + ",minHeartBeat: " + this.da + ",maxHeartBeat: " + this.cz + "wifi:" + com.huawei.android.pushagent.utils.a.f.t(this.dc) + "]");
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "reload network info:" + com.huawei.android.pushagent.utils.a.f.t(f.py(this.ef).qg(this.ef, this.db)));
        if (this.cx && lh(qe, this.db) && qa > -1) {
            this.cy = la(qa, this.da, this.cz);
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "use local best heartBeat: " + this.cy);
        } else {
            this.cy = la(f.py(this.ef).qh(this.ef, this.db), this.da, this.cz);
            if (this.cx) {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "BestHB is invalid because of time exceed,re-find BestHB begin from " + this.cy);
                f.py(this.ef).qi(this.ef, this.db, false);
                f.py(this.ef).qb(this.ef, this.db, -1);
                f.py(this.ef).qd(this.ef, this.db, false);
                this.cv = 0;
                this.cu = 0;
                this.cx = false;
            }
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "after load heartBeat, next heartBeatInterval:" + this.cy);
    }

    private long la(long j, long j2, long j3) {
        long j4;
        if (j < j2) {
            j4 = j2;
        } else {
            j4 = j;
        }
        if (j4 > j3) {
            return j3;
        }
        return j4;
    }

    private void ll() {
        int fw = g.fw(this.ef);
        f.py(this.ef).qi(this.ef, fw, this.cx);
        f.py(this.ef).qj(this.ef, fw, this.cy);
    }

    public void lm() {
        long j = 300000;
        try {
            ld(false);
            if (this.ei.dg()) {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "bastet started, do not need to check heartbeat timeout");
                j = this.ei.dh();
            } else {
                long ld = ld(false);
                if (g.ga() || ld >= 300000) {
                    j = ld;
                }
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "set HEARTBEAT_RSP_TIMEOUT Alarm");
                Intent intent = new Intent("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
                intent.setPackage(this.ef.getPackageName());
                d.cw(PushService.abp().abq(), intent, k.rh(this.ef).ru());
            }
            IPushMessage heartBeatReqMessage = new HeartBeatReqMessage();
            double ceil = Math.ceil((((double) j) * 1.0d) / 60000.0d);
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "next Heartbeat timeout for push server is:" + ceil);
            heartBeatReqMessage.jj((byte) ((int) ceil));
            com.huawei.android.pushagent.model.channel.a.ns().mc(heartBeatReqMessage);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "call pushChannel.send cause Exception:" + e.toString(), e);
        }
    }

    private void lc(k kVar, String str) {
        Object obj = null;
        if (!TextUtils.isEmpty(str)) {
            try {
                Object obj2;
                Iterable<String> keySet = kVar.rv().keySet();
                if (keySet == null || keySet.size() <= 0) {
                    obj2 = null;
                } else {
                    for (String str2 : keySet) {
                        if (str2.contains(str)) {
                            String str3 = (String) kVar.rv().get(str2);
                            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "apnName is:" + str2 + ",apnHeartBeat is:" + str3);
                            String[] split = str3.split("_");
                            this.da = Long.parseLong(split[0]) * 1000;
                            this.cz = Long.parseLong(split[1]) * 1000;
                            obj2 = 1;
                            break;
                        }
                    }
                    obj2 = null;
                }
                obj = obj2;
            } catch (Throwable e) {
                com.huawei.android.pushagent.utils.f.c.es("PushLog3413", e.toString(), e);
            }
        }
        if (obj == null) {
            this.da = kVar.get3GMinHeartbeat() * 1000;
            this.cz = kVar.get3GMaxHeartbeat() * 1000;
        }
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "Data network Heartbeat range, minHeartBeat is :" + this.da + ",maxHeartBeat is:" + this.cz);
    }

    public void kz(boolean z) {
        if (this.ei.di()) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "support bastet, do not handle control.");
            return;
        }
        if (z) {
            int fw = g.fw(this.ef);
            if (this.ei.dj()) {
                fw = 999;
            }
            long qa = f.py(this.ef).qa(this.ef, fw);
            if (qa > -1) {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "send best heartBeat to powergenie at first heartbeat,netType:" + fw);
                ln(this.ef, qa, fw);
            } else {
                lo(this.ef, true);
            }
        } else if (!this.cx) {
            lo(this.ef, true);
        }
    }

    public static void lo(Context context, boolean z) {
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "send command to powergenie release control push: " + z);
        Intent intent = new Intent("com.huawei.android.push.controlheartbeat");
        intent.putExtra("releaseControl", z);
        intent.setPackage("com.huawei.powergenie");
        context.sendBroadcast(intent);
    }

    private void ln(Context context, long j, int i) {
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "send current push bestHeartBeat to powergenie:" + j);
        Intent intent = new Intent("com.huawei.android.push.bestHB");
        intent.putExtra("networkType", f.py(context).qg(context, i));
        intent.putExtra("heartbeat", j);
        intent.setPackage("com.huawei.powergenie");
        context.sendBroadcast(intent);
    }

    public void lb() {
        long j = 300000;
        long j2 = 0;
        if (!this.cx || f.py(this.ef).qa(this.ef, this.db) <= -1) {
            j2 = ld(false);
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "after delayHeartBeatReq, nextHeartBeatTime, will be " + j2 + "ms later");
            my(j2, true);
            return;
        }
        if (this.cy < 300000) {
            j = this.cy;
        } else {
            j2 = this.cy - 300000;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "after delayHeartBeatReq, nextHeartBeatTime, will be [" + j + "," + (j + j2) + "] ms later");
        mx(j, j2);
    }
}
