package com.huawei.android.pushagent.model.channel.entity.a;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatReqMessage;
import com.huawei.android.pushagent.model.prefs.f;
import com.huawei.android.pushagent.model.prefs.m;
import com.huawei.android.pushagent.utils.d;
import com.huawei.android.pushagent.utils.e.c;
import java.util.Set;

public class a extends com.huawei.android.pushagent.model.channel.entity.a {
    private int aa = 0;
    private String ab = "";
    private boolean ac = false;
    private long ad = 7200000;
    private long ae = this.ad;
    private long af = 180000;
    private int ag = -1;
    private String ah = "";
    private String y = "";
    private int z = 0;

    public a(Context context) {
        super(context);
    }

    public boolean bn() {
        boolean z = true;
        int yh = d.yh(this.ak);
        String yi = d.yi(this.ak);
        String yj = d.yj(this.ak);
        switch (yh) {
            case 0:
                String yk = d.yk(this.ak);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "push apn is " + yk);
                if (yh == this.ag && (yj.equals(this.ab) ^ 1) == 0) {
                    z = yk.equals(this.y) ^ 1;
                    break;
                }
            case 1:
                if (yh == this.ag) {
                    z = this.ah.equals(yi) ^ 1;
                    break;
                }
                break;
            default:
                z = false;
                break;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "is network env changed: " + z + ",from netType " + this.ag + " to " + yh + ", [-1:NONE, 0:MOBILE, 1:WIFI]");
        return z;
    }

    public long bk(boolean z) {
        if (-1 == d.yh(this.ak)) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "no network, use no network heartbeat");
            return com.huawei.android.pushagent.model.prefs.a.ff(this.ak).fg() * 1000;
        }
        long j = this.ad;
        if (!this.ac) {
            if (z) {
                j = this.ad;
            } else {
                if (this.ad > 300000) {
                    j = com.huawei.android.pushagent.model.prefs.a.ff(this.ak).fh();
                } else {
                    j = com.huawei.android.pushagent.model.prefs.a.ff(this.ak).fi();
                }
                j += this.ad;
            }
            if (j <= this.af) {
                j = this.af;
            } else if (j >= this.ae) {
                j = this.ae;
            }
        }
        return j;
    }

    private void bl(boolean z) {
        if (this.al) {
            cj(false);
            if (z) {
                f.ke(this.ak).kf();
            } else {
                f.ke(this.ak).kg();
            }
        }
    }

    public void bf(boolean z, boolean z2) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "enter adjustHeartBeat:(findHeartBeat:" + this.ac + ",isHearBeatTimeReq:" + this.am + ",RspTimeOut:" + z + ",beatInterval:" + this.ad + ",bestHBSuccessCount:" + this.aa + ",range:[" + this.af + "," + this.ae + "]" + ")");
        bl(z);
        if (this.ac) {
            if (z) {
                bp(z, z2);
            } else {
                bp(false, z2);
            }
        } else if (this.am) {
            ck(false);
            this.ad = bk(z);
            if (z || this.ad <= this.af || this.ad >= this.ae) {
                this.ac = true;
                this.aa = 0;
                int yh = d.yh(this.ak);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "Find the best heartBeat Interval:" + this.ad + "ms");
                bu(this.ak, this.ad, yh);
                m.mc(this.ak).md(this.ak, yh);
                com.huawei.android.pushagent.b.a.abc(75, com.huawei.android.pushagent.b.a.abb(String.valueOf(yh), String.valueOf(this.ad)));
            } else {
                com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "set current heartBeatInterval " + this.ad + "ms");
            }
            bs();
        } else {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "It is not hearBeatTimeReq");
        }
    }

    private void bp(boolean z, boolean z2) {
        if (this.am) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "judgeFindBestHB and isBestHBTimeOut: " + z + "isOffset: " + z2);
            ck(false);
            if (m.mc(this.ak).me(this.ak, this.ag) > -1) {
                if (!z) {
                    this.z = 0;
                } else if (!z2) {
                    this.z++;
                    br();
                }
            } else if (z) {
                this.aa--;
                this.z++;
                br();
            } else {
                this.aa++;
                this.z = 0;
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "bestHBSuccessCount:" + this.aa);
                if (com.huawei.android.pushagent.model.prefs.a.ff(this.ak).fj() == this.aa) {
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "bestHBSuccessCount: " + this.aa + "and make sure the indeed BestHBTime: " + this.ad + "ms,reset bestHBSuccessCount");
                    m.mc(this.ak).mf(this.ak, this.ag, this.ad);
                    bu(this.ak, this.ad, this.ag);
                }
            }
        }
    }

    private void br() {
        if (m.mc(this.ak).mg(this.ak, this.ag)) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "has already back, no need back.");
            return;
        }
        if ((this.ad >= 300000 && this.z >= 2) || (this.ad < 300000 && this.z >= 3)) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "CurrentBestHBFailureCount:" + this.z + ",BestHBTime need go back one step and reset two counts");
            this.z = 0;
            this.aa = 0;
            this.ad -= 30000;
            this.ad = bh(this.ad, this.af, this.ae);
            m.mc(this.ak).mf(this.ak, this.ag, this.ad);
            m.mc(this.ak).md(this.ak, this.ag);
            m.mc(this.ak).mh(this.ak, this.ag, true);
            bu(this.ak, this.ad, this.ag);
            bs();
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "CurrentBestHBFailureCount:" + this.z + ",heartBeatInterval: " + this.ad);
    }

    private boolean bo(long j, int i) {
        long fk;
        if (i == 0) {
            fk = com.huawei.android.pushagent.model.prefs.a.ff(this.ak).fk();
        } else {
            fk = com.huawei.android.pushagent.model.prefs.a.ff(this.ak).fl();
        }
        if (j > -1 && System.currentTimeMillis() - j < fk) {
            return true;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "local best heartBeat time is:" + j + "exceed " + fk + ", netType:" + i);
        return false;
    }

    private void bm() {
        com.huawei.android.pushagent.model.prefs.a ff = com.huawei.android.pushagent.model.prefs.a.ff(this.ak);
        switch (this.ag) {
            case 0:
                this.y = d.yk(this.ak);
                com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "in loadHeartBeat apnName:" + this.y);
                bj(ff, this.y);
                break;
            case 1:
                this.af = ff.getWifiMinHeartbeat() * 1000;
                this.ae = ff.getWifiMaxHeartbeat() * 1000;
                break;
            default:
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "unKnow net type");
                break;
        }
        if (this.af < 180000) {
            this.af = 180000;
        }
    }

    public synchronized void bq() {
        if (com.huawei.android.pushagent.model.channel.a.dz() == null) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "system is in start, wait net for heartBeat");
            return;
        }
        com.huawei.android.pushagent.model.prefs.a ff = com.huawei.android.pushagent.model.prefs.a.ff(this.ak);
        this.ag = d.yh(this.ak);
        if (-1 == this.ag) {
            this.ad = ff.fg() * 1000;
            return;
        }
        this.aa = 0;
        this.z = 0;
        this.ah = d.yi(this.ak);
        this.ab = d.yj(this.ak);
        bm();
        long me = m.mc(this.ak).me(this.ak, this.ag);
        long mi = m.mc(this.ak).mi(this.ak, this.ag);
        this.ac = m.mc(this.ak).mj(this.ak, this.ag);
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "reload heartbeat info, [lastBestHBTime: " + mi + ",hasFindHeartBeat: " + this.ac + ",localBestHB: " + me + ",minHeartBeat: " + this.af + ",maxHeartBeat: " + this.ae + "wifi:" + c.wh(this.ah) + "]");
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "reload network info:" + c.wh(m.mc(this.ak).mk(this.ak, this.ag)));
        if (this.ac && bo(mi, this.ag) && me > -1) {
            this.ad = bh(me, this.af, this.ae);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "use local best heartBeat: " + this.ad);
        } else {
            this.ad = bh(m.mc(this.ak).ml(this.ak, this.ag), this.af, this.ae);
            if (this.ac) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "BestHB is invalid because of time exceed,re-find BestHB begin from " + this.ad);
                m.mc(this.ak).mm(this.ak, this.ag, false);
                m.mc(this.ak).mf(this.ak, this.ag, -1);
                m.mc(this.ak).mh(this.ak, this.ag, false);
                this.aa = 0;
                this.z = 0;
                this.ac = false;
            }
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "after load heartBeat, next heartBeatInterval:" + this.ad);
    }

    private long bh(long j, long j2, long j3) {
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

    private void bs() {
        int yh = d.yh(this.ak);
        m.mc(this.ak).mm(this.ak, yh, this.ac);
        m.mc(this.ak).mn(this.ak, yh, this.ad);
    }

    public void bt() {
        long j = 300000;
        try {
            bk(false);
            if (this.an.wz()) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "bastet started, do not need to check heartbeat timeout");
                j = this.an.xa();
            } else {
                long bk = bk(false);
                if (d.yl() || bk >= 300000) {
                    j = bk;
                }
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "set HEARTBEAT_RSP_TIMEOUT Alarm");
                Intent intent = new Intent("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
                intent.setPackage(this.ak.getPackageName());
                com.huawei.android.pushagent.utils.tools.a.sa(PushService.abt().abu(), intent, com.huawei.android.pushagent.model.prefs.a.ff(this.ak).fm());
            }
            HeartBeatReqMessage heartBeatReqMessage = new HeartBeatReqMessage();
            double ceil = Math.ceil((((double) j) * 1.0d) / 60000.0d);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "next Heartbeat timeout for push server is:" + ceil);
            heartBeatReqMessage.u((byte) ((int) ceil));
            com.huawei.android.pushagent.model.channel.a.dz().co(heartBeatReqMessage);
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "call pushChannel.send cause Exception:" + e.toString(), e);
        }
    }

    private void bj(com.huawei.android.pushagent.model.prefs.a aVar, String str) {
        Object obj = null;
        if (!TextUtils.isEmpty(str)) {
            try {
                Object obj2;
                Set<String> keySet = aVar.fn().keySet();
                if (keySet == null || keySet.size() <= 0) {
                    obj2 = null;
                } else {
                    for (String str2 : keySet) {
                        if (str2.contains(str)) {
                            String str3 = (String) aVar.fn().get(str2);
                            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "apnName is:" + str2 + ",apnHeartBeat is:" + str3);
                            String[] split = str3.split("_");
                            this.af = Long.parseLong(split[0]) * 1000;
                            this.ae = Long.parseLong(split[1]) * 1000;
                            obj2 = 1;
                            break;
                        }
                    }
                    obj2 = null;
                }
                obj = obj2;
            } catch (Exception e) {
                com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
            }
        }
        if (obj == null) {
            this.af = aVar.get3GMinHeartbeat() * 1000;
            this.ae = aVar.get3GMaxHeartbeat() * 1000;
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "Data network Heartbeat range, minHeartBeat is :" + this.af + ",maxHeartBeat is:" + this.ae);
    }

    public void bg(boolean z) {
        if (this.an.xb()) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "support bastet, do not handle control.");
            return;
        }
        if (z) {
            int yh = d.yh(this.ak);
            if (this.an.xc()) {
                yh = 999;
            }
            long me = m.mc(this.ak).me(this.ak, yh);
            if (me > -1) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "send best heartBeat to powergenie at first heartbeat,netType:" + yh);
                bu(this.ak, me, yh);
            } else {
                bv(this.ak, true);
            }
        } else if (!this.ac) {
            bv(this.ak, true);
        }
    }

    public static void bv(Context context, boolean z) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "send command to powergenie release control push: " + z);
        Intent intent = new Intent("com.huawei.android.push.controlheartbeat");
        intent.putExtra("releaseControl", z);
        intent.setPackage("com.huawei.powergenie");
        context.sendBroadcast(intent);
    }

    private void bu(Context context, long j, int i) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "send current push bestHeartBeat to powergenie:" + j);
        Intent intent = new Intent("com.huawei.android.push.bestHB");
        intent.putExtra("networkType", m.mc(context).mk(context, i));
        intent.putExtra("heartbeat", j);
        intent.setPackage("com.huawei.powergenie");
        context.sendBroadcast(intent);
    }

    public void bi() {
        long j = 300000;
        long j2 = 0;
        if (!this.ac || m.mc(this.ak).me(this.ak, this.ag) <= -1) {
            j2 = bk(false);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "after delayHeartBeatReq, nextHeartBeatTime, will be " + j2 + "ms later");
            cm(j2, true);
            return;
        }
        if (this.ad < 300000) {
            j = this.ad;
        } else {
            j2 = this.ad - 300000;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "after delayHeartBeatReq, nextHeartBeatTime, will be [" + j + "," + (j + j2) + "] ms later");
        cl(j, j2);
    }
}
