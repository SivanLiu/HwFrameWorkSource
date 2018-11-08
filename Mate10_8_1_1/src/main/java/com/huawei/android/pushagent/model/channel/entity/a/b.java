package com.huawei.android.pushagent.model.channel.entity.a;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatReqMessage;
import com.huawei.android.pushagent.datatype.tcp.base.IPushMessage;
import com.huawei.android.pushagent.model.channel.a;
import com.huawei.android.pushagent.model.channel.entity.c;
import com.huawei.android.pushagent.model.prefs.d;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.f;
import java.net.Socket;

public class b extends c {
    private String ez = "";
    private int fa = 0;
    private String fb = null;
    private String fc = "";
    private boolean fd = false;
    private long fe = 7200000;
    private long ff = this.fe;
    private long fg = this.fe;
    private int fh = -1;
    private String fi = "";

    public b(Context context) {
        super(context);
    }

    public String toString() {
        String str = "=";
        String str2 = " ";
        return new StringBuffer().append("HasFindHeartBeat").append(str).append(this.fd).append(str2).append("HearBeatInterval").append(str).append(this.fe).append(str2).append("minHeartBeat").append(str).append(this.fg).append(str2).append("maxHeartBeat").append(str).append(this.ff).toString();
    }

    protected boolean ul() {
        boolean z = true;
        int fp = f.fp(this.gl);
        String gc = f.gc(this.gl);
        String ga = f.ga(this.gl);
        switch (fp) {
            case 0:
                String gb = f.gb(this.gl);
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "push apn is " + gb);
                if (fp == this.fh && (ga.equals(this.fc) ^ 1) == 0) {
                    z = gb.equals(this.ez) ^ 1;
                    break;
                }
            case 1:
                if (fp == this.fh && (this.fi.equals(gc) ^ 1) == 0) {
                    z = uh().equals(this.fb) ^ 1;
                    break;
                }
            default:
                z = false;
                break;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "is network env changed: " + z + ",from netType " + this.fh + " to " + fp + ", [-1:NONE, 0:MOBILE, 1:WIFI]");
        return z;
    }

    private String uh() {
        String str = "";
        try {
            if (a.wr() != null) {
                Socket vd = a.wr().vd();
                if (vd != null) {
                    str = vd.getLocalAddress().getHostAddress();
                }
            }
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", e.toString());
        }
        if (str == null) {
            return "";
        }
        return str;
    }

    public long uj(boolean z) {
        if (-1 == f.fp(this.gl)) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "no network, use no network heartbeat");
            return i.mj(this.gl).pd() * 1000;
        }
        if (ul()) {
            uo();
        }
        long j = this.fe;
        if (!this.fd) {
            if (z) {
                j = this.fe;
            } else {
                if (this.fe > 300000) {
                    j = i.mj(this.gl).pb();
                } else {
                    j = i.mj(this.gl).pc();
                }
                j += this.fe;
            }
            if (j <= this.fg) {
                j = this.fg;
            } else if (j >= this.ff) {
                j = this.ff;
            }
        }
        return j;
    }

    public void ue(boolean z) {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "enter adjustHeartBeat:(findHeartBeat:" + this.fd + ",isHearBeatTimeReq:" + this.gm + ",RspTimeOut:" + z + ",beatInterval:" + this.fe + ",bestHBSuccessCount:" + this.fa + ",range:[" + this.fg + "," + this.ff + "]" + ")");
        if (this.fd) {
            if (z) {
                un(z);
            } else {
                un(false);
                d.lc(this.gl).le(this.gl, this.fh);
            }
        } else if (this.gm) {
            vv(false);
            this.fe = uj(z);
            if (z || this.fe <= this.fg || this.fe >= this.ff) {
                this.fd = true;
                this.fa = 0;
                int fp = f.fp(this.gl);
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "Find the best heartBeat Interval:" + this.fe + "ms");
                ur(this.gl, this.fe, fp);
                d.lc(this.gl).le(this.gl, fp);
                com.huawei.android.pushagent.b.a.aaj(75, com.huawei.android.pushagent.b.a.aal(String.valueOf(fp), String.valueOf(this.fe)));
            } else {
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "set current heartBeatInterval " + this.fe + "ms");
            }
            up();
        } else {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "It is not hearBeatTimeReq");
        }
    }

    private void un(boolean z) {
        if (this.gm && d.lc(this.gl).lh(this.gl, this.fh) <= -1) {
            if (z) {
                this.fa--;
            } else {
                this.fa++;
            }
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "bestHBSuccessCount: " + this.fa);
            if (i.mj(this.gl).ow() == this.fa) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "Find the indeed best heartBeat:" + this.fe + "ms");
                d.lc(this.gl).lf(this.gl, this.fh, this.fe);
                return;
            }
            if (i.mj(this.gl).ow() == (-this.fa)) {
                this.fa = 0;
                this.fd = false;
                this.fe = this.fg;
                up();
            }
        }
    }

    private boolean um(long j, int i) {
        long ox;
        if (i == 0) {
            ox = i.mj(this.gl).ox();
        } else {
            ox = i.mj(this.gl).pl();
        }
        if (j > -1 && System.currentTimeMillis() - j < ox) {
            return true;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "local best heartBeat time is:" + j + "exceed " + ox + ", netType:" + i);
        return false;
    }

    private void uk() {
        i mj = i.mj(this.gl);
        switch (this.fh) {
            case 0:
                this.ez = f.gb(this.gl);
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "in loadHeartBeat apnName:" + this.ez);
                ui(mj, this.ez);
                return;
            case 1:
                this.fg = mj.getWifiMinHeartbeat() * 1000;
                this.ff = mj.getWifiMaxHeartbeat() * 1000;
                return;
            default:
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "unKnow net type");
                return;
        }
    }

    public void uo() {
        if (a.wr() == null) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "system is in start, wait net for heartBeat");
            return;
        }
        i mj = i.mj(this.gl);
        this.fh = f.fp(this.gl);
        if (-1 == this.fh) {
            this.fe = mj.pd() * 1000;
            return;
        }
        this.fa = 0;
        this.fi = f.gc(this.gl);
        this.fc = f.ga(this.gl);
        this.fb = uh();
        uk();
        long lh = d.lc(this.gl).lh(this.gl, this.fh);
        long ll = d.lc(this.gl).ll(this.gl, this.fh);
        this.fd = d.lc(this.gl).lj(this.gl, this.fh);
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "reload heartbeat info, [lastBestHBTime: " + ll + ",hasFindHeartBeat: " + this.fd + ",localBestHB: " + lh + ",minHeartBeat: " + this.fg + ",maxHeartBeat: " + this.ff + "]");
        if (this.fd && um(ll, this.fh) && lh > -1) {
            this.fe = ug(lh, this.fg, this.ff);
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "use local best heartBeat: " + this.fe);
        } else {
            this.fe = ug(d.lc(this.gl).lm(this.gl, this.fh), this.fg, this.ff);
            if (this.fd) {
                d.lc(this.gl).lp(this.gl, this.fh, false);
                d.lc(this.gl).lf(this.gl, this.fh, -1);
                this.fa = 0;
                this.fd = false;
            }
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "after load heartBeat, next heartBeatInterval:" + this.fe);
    }

    private long ug(long j, long j2, long j3) {
        long j4 = 150000;
        long j5 = j < j2 ? j2 : j;
        if (j5 >= 150000) {
            j4 = j5;
        }
        if (j4 > j3) {
            return j3;
        }
        return j4;
    }

    private void up() {
        int fp = f.fp(this.gl);
        d.lc(this.gl).lp(this.gl, fp, this.fd);
        d.lc(this.gl).lq(this.gl, fp, this.fe);
    }

    public void uq() {
        try {
            long uj = uj(false);
            if (this.gn.cu()) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "bastet started, do not need to check heartbeat timeout");
                uj = this.gn.ch();
            } else {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "set HEARTBEAT_RSP_TIMEOUT Alarm");
                com.huawei.android.pushagent.utils.tools.d.p(PushService.abd().abc(), new Intent("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT").setPackage(this.gl.getPackageName()), i.mj(this.gl).pa());
            }
            IPushMessage heartBeatReqMessage = new HeartBeatReqMessage();
            heartBeatReqMessage.zo((byte) ((int) Math.ceil((((double) uj) * 1.0d) / 60000.0d)));
            a.wr().vl(heartBeatReqMessage);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "call pushChannel.send cause Exception:" + e.toString(), e);
        }
    }

    private void ui(i iVar, String str) {
        Object obj = null;
        if (!TextUtils.isEmpty(str)) {
            try {
                Object obj2;
                Iterable<String> keySet = iVar.ov().keySet();
                if (keySet == null || keySet.size() <= 0) {
                    obj2 = null;
                } else {
                    for (String str2 : keySet) {
                        if (str2.contains(str)) {
                            String str3 = (String) iVar.ov().get(str2);
                            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "apnName is:" + str2 + ",apnHeartBeat is:" + str3);
                            String[] split = str3.split("_");
                            this.fg = Long.parseLong(split[0]) * 1000;
                            this.ff = Long.parseLong(split[1]) * 1000;
                            obj2 = 1;
                            break;
                        }
                    }
                    obj2 = null;
                }
                obj = obj2;
            } catch (Throwable e) {
                com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", e.toString(), e);
            }
        }
        if (obj == null) {
            this.fg = iVar.get3GMinHeartbeat() * 1000;
            this.ff = iVar.get3GMaxHeartbeat() * 1000;
        }
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "Data network Heartbeat range, minHeartBeat is :" + this.fg + ",maxHeartBeat is:" + this.ff);
    }

    public void uf(boolean z) {
        if (this.gn.cn()) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "support bastet, do not handle control.");
            return;
        }
        if (z) {
            int fp = f.fp(this.gl);
            if (this.gn.cl()) {
                fp = 999;
            }
            long lh = d.lc(this.gl).lh(this.gl, fp);
            if (lh > -1) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "send best heartBeat to powergenie at first heartbeat,netType:" + fp);
                ur(this.gl, lh, fp);
            } else {
                us(this.gl, true);
            }
        } else if (!this.fd) {
            us(this.gl, true);
        }
    }

    public static void us(Context context, boolean z) {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "send command to powergenie release control push: " + z);
        Intent intent = new Intent("com.huawei.android.push.controlheartbeat");
        intent.putExtra("releaseControl", z);
        intent.setPackage("com.huawei.powergenie");
        context.sendBroadcast(intent);
    }

    private void ur(Context context, long j, int i) {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "send current push bestHeartBeat to powergenie:" + j);
        Intent intent = new Intent("com.huawei.android.push.bestHB");
        intent.putExtra("networkType", d.lc(context).ld(context, i));
        intent.putExtra("heartbeat", j);
        intent.setPackage("com.huawei.powergenie");
        context.sendBroadcast(intent);
    }
}
