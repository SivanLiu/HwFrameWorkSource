package tmsdk.bg.module.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import tmsdk.bg.creator.BaseManagerB;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.tcc.TrafficSmsParser;
import tmsdk.common.tcc.TrafficSmsParser.MatchRule;
import tmsdk.common.utils.f;
import tmsdk.common.utils.l;
import tmsdk.common.utils.q;
import tmsdk.common.utils.s;
import tmsdkobf.ay;
import tmsdkobf.az;
import tmsdkobf.ba;
import tmsdkobf.bb;
import tmsdkobf.bc;
import tmsdkobf.bd;
import tmsdkobf.be;
import tmsdkobf.bf;
import tmsdkobf.bg;
import tmsdkobf.bh;
import tmsdkobf.bj;
import tmsdkobf.bk;
import tmsdkobf.bl;
import tmsdkobf.bm;
import tmsdkobf.bn;
import tmsdkobf.bo;
import tmsdkobf.bq;
import tmsdkobf.eb;
import tmsdkobf.im;
import tmsdkobf.jk;
import tmsdkobf.jl;
import tmsdkobf.jn;
import tmsdkobf.jy;
import tmsdkobf.ka;
import tmsdkobf.kp;
import tmsdkobf.kq;
import tmsdkobf.lw;
import tmsdkobf.oa;
import tmsdkobf.oh;

class i extends BaseManagerB {
    private Context mContext;
    private int mRetryCount = 0;
    public final int vO = 4097;
    public final int vP = 4098;
    public final int vQ = 4099;
    public final int vR = 4100;
    public final int vS = 4101;
    public final int vT = 4102;
    public final int vU = 4103;
    public final int vV = 4104;
    Handler vW;
    private oa vX;
    private ITrafficCorrectionListener vY = null;
    private int vZ = 2;
    private int wa = 3;
    ka wb = new ka(this) {
        final /* synthetic */ i wc;

        {
            this.wc = r1;
        }

        public oh<Long, Integer, JceStruct> a(int -l_6_I, long -l_7_J, int -l_9_I, JceStruct -l_10_R) {
            f.d("TrafficCorrection", "【push listener收到push消息】--cmdId:[" + -l_9_I + "]pushId:[" + -l_7_J + "]seqNo:[" + -l_6_I + "]guid[" + this.wc.vX.b() + "]");
            switch (-l_9_I) {
                case 11006:
                    if (-l_10_R != null) {
                        f.d("TrafficCorrection", "[流量push消息]--启动worker线程跑执行");
                        final int i = -l_9_I;
                        final int i2 = -l_6_I;
                        final long j = -l_7_J;
                        final JceStruct jceStruct = -l_10_R;
                        Object -l_11_R = new Thread(new Runnable(this) {
                            final /* synthetic */ AnonymousClass2 wh;

                            public void run() {
                                Object -l_1_R = new kp();
                                -l_1_R.Y = i;
                                -l_1_R.ey = i2;
                                -l_1_R.ex = j;
                                -l_1_R.wL = jceStruct;
                                bh -l_2_R = (bh) -l_1_R.wL;
                                String -l_3_R = -l_2_R.imsi;
                                int -l_4_I = -l_2_R.cj;
                                f.d("TrafficCorrection", "[执行push消息]--个数:[" + -l_2_R.cD.size() + "]--cloudimsi:[" + -l_3_R + "]卡槽:[" + -l_4_I + "]seqNo:[" + -l_1_R.ey + "]pushId:[" + -l_1_R.ex + "]");
                                if (!(-l_4_I == 0 || -l_4_I == 1)) {
                                    -l_4_I = 0;
                                }
                                boolean -l_5_I = false;
                                String -l_6_R = this.wh.wc.at(-l_4_I);
                                if (-l_3_R == null) {
                                    -l_3_R = "";
                                }
                                if (-l_6_R == null) {
                                    -l_6_R = "";
                                }
                                if (!("".equals(-l_3_R) || "".equals(-l_6_R) || !-l_6_R.equals(-l_3_R))) {
                                    -l_5_I = true;
                                }
                                if ("".equals(-l_3_R) && "".equals(-l_6_R)) {
                                    -l_5_I = true;
                                }
                                f.d("TrafficCorrection", "isImsiOK:[" + -l_5_I + "]");
                                Object -l_7_R = new bc();
                                -l_7_R.cs = new ArrayList();
                                -l_7_R.imsi = -l_6_R;
                                -l_7_R.cj = -l_4_I;
                                int -l_9_I = 0;
                                for (int -l_10_I = 0; -l_10_I < -l_2_R.cD.size(); -l_10_I++) {
                                    boolean -l_8_I = false;
                                    be -l_11_R = (be) -l_2_R.cD.get(-l_10_I);
                                    f.d("TrafficCorrection", "[" + -l_1_R.ey + "]开始执行第[" + (-l_10_I + 1) + "]条push指令:[" + -l_11_R + "]");
                                    if (-l_5_I && -l_11_R != null) {
                                        -l_8_I = this.wh.wc.a(-l_4_I, -l_11_R, -l_3_R);
                                        if (-l_11_R.cu == 14 || -l_11_R.cu == 10 || -l_11_R.cu == 13) {
                                            if (!(-l_11_R.cw == null || -l_11_R.cw.equals(""))) {
                                                -l_9_I = 1;
                                            }
                                        }
                                    }
                                    f.d("TrafficCorrection", "]" + -l_1_R.ey + "]指令执行结果:[" + -l_8_I + "]");
                                    Object -l_12_R = new bf();
                                    -l_12_R.cz = -l_11_R;
                                    -l_12_R.cA = -l_8_I;
                                    -l_7_R.cs.add(-l_12_R);
                                }
                                f.d("TrafficCorrection", "【push消息处理完毕】全部指令执行结束--[upload]业务回包imsi:[" + -l_7_R.imsi + "]卡槽:[" + -l_7_R.cj + "]");
                                this.wh.wc.vX.b(-l_1_R.ey, -l_1_R.ex, 11006, -l_7_R);
                                if (-l_9_I != 0 && this.wh.wc.vY != null) {
                                    this.wh.wc.vW.sendMessage(this.wh.wc.vW.obtainMessage(4103, -l_4_I, 0));
                                }
                            }
                        });
                        -l_11_R.setName("pushImpl");
                        -l_11_R.start();
                        break;
                    }
                    f.d("TrafficCorrection", "push == null结束");
                    return null;
            }
            return null;
        }
    };

    class a {
        final /* synthetic */ i wc;
        int wt;
        int wu;
        int wv;
        int ww;

        a(i iVar) {
            this.wc = iVar;
        }
    }

    i() {
    }

    private int a(int i, List<MatchRule> list, String str, String str2, boolean z) {
        int -l_9_I;
        int -l_6_I = 9;
        f.d("TrafficCorrection", "[开始模块匹配] body：[ " + str2 + "]isUsed:[" + z + "]matchRules:[" + list + "]");
        MatchRule -l_7_R = (MatchRule) list.get(0);
        Object -l_8_R = new MatchRule(-l_7_R.unit, -l_7_R.type, -l_7_R.prefix, -l_7_R.postfix);
        if (list.size() > 1) {
            -l_8_R.prefix += "&#" + -l_8_R.unit + "&#" + -l_7_R.type;
        }
        for (-l_9_I = 1; -l_9_I < list.size(); -l_9_I++) {
            -l_8_R.prefix += ("&#" + ((MatchRule) list.get(-l_9_I)).prefix + "&#" + ((MatchRule) list.get(-l_9_I)).unit + "&#" + ((MatchRule) list.get(-l_9_I)).type);
            -l_8_R.postfix += ("&#" + ((MatchRule) list.get(-l_9_I)).postfix);
        }
        f.d("TrafficCorrection", "prefix: " + -l_8_R.prefix);
        f.d("TrafficCorrection", "postfix: " + -l_8_R.postfix);
        -l_9_I = 0;
        int -l_10_I = 0;
        Object -l_11_R = new AtomicInteger();
        if (TrafficSmsParser.getNumberEntrance(str, str2, -l_8_R, -l_11_R) == 0) {
            -l_9_I = 1;
            -l_10_I = -l_11_R.get() + 0;
        }
        if (-l_9_I == 0) {
            f.d("TrafficCorrection", "[匹配不成功]");
        } else {
            f.d("TrafficCorrection", "[匹配成功]isUsed:[" + z + "]数据为：[" + -l_10_I + "]");
            Object -l_13_R = new a(this);
            -l_13_R.wt = i;
            -l_13_R.wu = 1;
            -l_13_R.ww = -l_10_I;
            if (z) {
                -l_6_I = 6;
                -l_13_R.wv = 258;
            } else {
                -l_6_I = 7;
                -l_13_R.wv = 257;
            }
            this.vW.sendMessage(this.vW.obtainMessage(4097, -l_13_R));
        }
        return -l_6_I;
    }

    private String a(int i, JceStruct jceStruct) {
        Object -l_3_R;
        switch (i) {
            case 1001:
                bd -l_4_R = (bd) jceStruct;
                -l_3_R = "\nsimcard:" + -l_4_R.cj + " imsi:" + -l_4_R.imsi + "\n sms:" + -l_4_R.sms + "\n startType:" + -l_4_R.cp + "\n time:" + -l_4_R.time + "\n code:" + -l_4_R.cm + "\n vecTraffic:" + -l_4_R.ck;
                break;
            case 1002:
                bb -l_5_R = (bb) jceStruct;
                -l_3_R = "\nsimcard:" + -l_5_R.cj + " imsi:" + -l_5_R.imsi + "\n method:" + -l_5_R.cn + "\n tplate:" + -l_5_R.co + "\n sms:" + -l_5_R.sms + "\n startType:" + -l_5_R.cp + "\n time:" + -l_5_R.time + "\n type:" + -l_5_R.type + "\n code:" + -l_5_R.cm + "\n vecTraffic:" + -l_5_R.ck;
                break;
            case 1003:
                az -l_6_R = (az) jceStruct;
                -l_3_R = "\n simcard:" + -l_6_R.cj + " imsi:" + -l_6_R.imsi + "\n vecTraffic:" + -l_6_R.ck;
                break;
            case 1004:
                ay -l_7_R = (ay) jceStruct;
                -l_3_R = "\n simcard:" + -l_7_R.cj + " imsi:" + -l_7_R.imsi + "\n authenResult:" + -l_7_R.ci + "\n skey:" + -l_7_R.ch;
                break;
            case 1007:
                ba -l_8_R = (ba) jceStruct;
                -l_3_R = "\n simcard:" + -l_8_R.cj + " imsi:" + -l_8_R.imsi + "getType:" + -l_8_R.aH;
                break;
            case 1008:
                bq -l_9_R = (bq) jceStruct;
                -l_3_R = "\nsimcard:" + -l_9_R.cj + "\n getParamType:" + -l_9_R.dj + " fixMethod:" + -l_9_R.de + "\n fixTimeLocal:" + -l_9_R.di + "\n fixTimes:" + -l_9_R.dd + "\n frequence:" + -l_9_R.dh + "\n imsi:" + -l_9_R.imsi + "\n status:" + -l_9_R.status + "\n timeOutNum:" + -l_9_R.df + "\n queryCode:" + -l_9_R.dg;
                if (-l_9_R.dg != null) {
                    -l_3_R = -l_3_R + "\n port:" + -l_9_R.dg.port + ", code:" + -l_9_R.dg.cC;
                    break;
                }
                return -l_3_R;
            default:
                return "";
        }
        return -l_3_R;
    }

    private void a(int i, int i2, int i3, int i4) {
        f.d("TrafficCorrection", "[开始短信校正]");
        if (as(i)) {
            this.vZ = i2;
            this.wa = i3;
            if (this.vY != null) {
                f.d("TrafficCorrection", "[通知使用者去发生查询短信]");
                this.vW.sendMessage(this.vW.obtainMessage(4098, i, 0));
            }
            return;
        }
        m(i, i4);
    }

    private void a(final int -l_10_I, int i, String str) {
        f.d("TrafficCorrection", "uploadLocalCorrectionState-simIndex:[" + -l_10_I + "]fixType:[" + i + "]smsBody:[" + str + "]");
        Object -l_4_R = new j(-l_10_I);
        Object -l_5_R = new ArrayList();
        Object -l_6_R = new bm();
        -l_6_R.cU = -l_5_R;
        Object -l_7_R = new ArrayList();
        Object -l_8_R = new bg();
        if (-l_8_R != null) {
            -l_8_R.cC = -l_4_R.dm();
            -l_8_R.port = -l_4_R.dn();
        }
        JceStruct -l_9_R = new bb();
        -l_9_R.imsi = at(-l_10_I);
        -l_9_R.cm = -l_8_R;
        -l_9_R.cn = this.wa;
        -l_9_R.sms = str;
        -l_9_R.cp = this.vZ;
        -l_9_R.co = -l_6_R;
        -l_9_R.type = i;
        -l_9_R.ck = -l_7_R;
        -l_9_R.cj = -l_10_I;
        f.d("TrafficCorrection", "[upload]-[" + av(1002) + "],内容：[" + a(1002, -l_9_R) + "]");
        this.vX.a(1002, -l_9_R, null, 2, new jy(this) {
            final /* synthetic */ i wc;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                if (i3 != 0) {
                    this.wc.o(-l_10_I, TrafficErrorCode.ERR_CORRECTION_FEEDBACK_UPLOAD_FAIL);
                }
            }
        });
    }

    private void a(int i, String -l_5_R, String str) {
        int -l_4_I;
        int -l_7_I = 0;
        String -l_5_R2 = "";
        eb -l_6_R = tmsdk.common.utils.i.iG();
        if (-l_6_R == eb.iL || -l_6_R == eb.iK) {
            -l_7_I = 1;
        }
        f.d("TrafficCorrection", "doValify--skey:[" + -l_5_R + "]url:[" + str + "]isGPRS:[" + -l_7_I + "]");
        if (-l_7_I == 0) {
            -l_4_I = 1;
            -l_5_R = -l_5_R2;
        } else {
            Object -l_8_R;
            try {
                -l_8_R = lw.bO(str);
                -l_8_R.eK();
                if (-l_8_R.getResponseCode() == SmsCheckResult.ESCT_200) {
                    -l_4_I = 0;
                }
            } catch (Object -l_8_R2) {
                f.d("TrafficCorrection", "doValify--networkException:" + -l_8_R2.getMessage());
            }
            -l_4_I = 2;
            -l_5_R = -l_5_R2;
        }
        f.d("TrafficCorrection", "doValify--resultSkey:[" + -l_5_R + "]errorcode:[" + -l_4_I + "]");
        if (-l_5_R == null) {
            -l_5_R = "";
        }
        JceStruct -l_8_R3 = new ay();
        -l_8_R3.imsi = at(i);
        -l_8_R3.ci = -l_4_I;
        -l_8_R3.ch = -l_5_R;
        -l_8_R3.cj = i;
        f.d("TrafficCorrection", "[upload]-[" + av(1004) + "]内容:[" + a(1004, -l_8_R3) + "]");
        this.vX.a(1004, -l_8_R3, null, 2, null);
    }

    private void a(int i, be beVar, String str, int i2) {
        if (this.vY != null) {
            a -l_5_R = null;
            if (beVar.cv == 4) {
                -l_5_R = new a(this);
                -l_5_R.wv = 257;
            } else if (beVar.cv == 3) {
                -l_5_R = new a(this);
                -l_5_R.wv = 258;
            } else if (beVar.cv == 6) {
                -l_5_R = new a(this);
                -l_5_R.wv = 259;
            }
            if (!(-l_5_R == null || this.vW == null)) {
                -l_5_R.wt = i;
                -l_5_R.wu = i2;
                -l_5_R.ww = Integer.valueOf(str).intValue();
                this.vW.sendMessage(this.vW.obtainMessage(4097, -l_5_R));
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean a(int i, be beVar, String str) {
        int -l_4_I = 1;
        String -l_5_R = beVar.cw;
        f.d("TrafficCorrection", "处理push卡槽:[" + i + "] order.orderType:[" + beVar.cu + "](" + aw(beVar.cu) + ") content:[" + -l_5_R + "]");
        Object -l_6_R = new j(i);
        Object -l_12_R;
        int -l_10_I;
        Object -l_11_R;
        Object -l_14_R;
        switch (beVar.cu) {
            case 1:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    f.f("jiejieT", "simIndex is " + i);
                    f.f("jiejieT", "correctionType is " + -l_5_R);
                    -l_6_R.bm(-l_5_R);
                    break;
                }
            case 2:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    try {
                        -l_6_R.aA(Integer.valueOf(-l_5_R).intValue());
                    } catch (Object -l_13_R) {
                        f.e("TrafficCorrection", "[Error]EOrder.EO_ChangeFrequncy" + -l_13_R.getMessage());
                        -l_4_I = 0;
                        return -l_4_I;
                    }
                }
            case 3:
                -l_6_R.o(false);
                break;
            case 4:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    -l_6_R.bk(-l_5_R);
                    -l_6_R.n(true);
                    f.f("New_PortCode", "EOrder.EO_DownCode, simIndex is " + i);
                    break;
                }
            case 5:
                -l_6_R.o(true);
                break;
            case 6:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    JceStruct -l_12_R2 = new bm();
                    if (!(a(-l_5_R.getBytes(), -l_12_R2) == 0 || -l_12_R2.cU == null)) {
                        -l_6_R.f(-l_12_R2.cU);
                        break;
                    }
                }
            case 7:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    -l_6_R.bn(-l_5_R);
                    break;
                }
            case 8:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    try {
                        -l_6_R.az(Integer.valueOf(-l_5_R).intValue());
                    } catch (Object -l_12_R3) {
                        f.e("TrafficCorrection", "[Error]EO_ChangeTimeOut: " + -l_12_R3.getMessage());
                        -l_4_I = 0;
                        return -l_4_I;
                    }
                }
            case 9:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    -l_6_R.bl(-l_5_R);
                    -l_6_R.m(true);
                    f.f("TrafficCorrection", "EOrder.EO_DownPort, simIndex is " + i);
                    break;
                }
            case 10:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    a(i, beVar, -l_5_R, 1);
                    break;
                }
            case 11:
                a(i, 1, 0, 7);
                break;
            case 12:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    final JceStruct -l_10_R = new kq();
                    if (a(-l_5_R.getBytes(), -l_10_R) != 0) {
                        final int i2 = i;
                        im.bJ().addTask(new Runnable(this) {
                            final /* synthetic */ i wc;

                            public void run() {
                                this.wc.a(i2, -l_10_R.ch, -l_10_R.url);
                            }
                        }, "AuthenticationInfo_Check");
                        break;
                    }
                }
            case 14:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    a(i, beVar, -l_5_R, 2);
                    break;
                }
            case 16:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    -l_6_R.bo(-l_5_R);
                    break;
                }
            case 19:
                if (!(-l_5_R == null || "".equals(-l_5_R))) {
                    a(i, beVar, -l_5_R, 3);
                    break;
                }
            case 20:
                if (!(beVar.cx == null || beVar.cx.length == 0)) {
                    if (this.vW != null) {
                        JceStruct -l_9_R = new bn();
                        if (a(beVar.cx, -l_9_R)) {
                            f.d("TrafficCorrection", "push详情信息timeNow:[" + -l_9_R.cW + "]");
                            -l_10_I = 0;
                            if (-l_9_R.cX != null) {
                                -l_11_R = new ArrayList();
                                -l_12_R3 = -l_9_R.cX.iterator();
                                while (-l_12_R3.hasNext()) {
                                    bk -l_13_R2 = (bk) -l_12_R3.next();
                                    DetailCategoryInfo -l_15_R = new DetailCategoryInfo();
                                    if (-l_13_R2.cK != null) {
                                        -l_14_R = -l_13_R2.cK;
                                        f.d("TrafficCorrection", "[" + -l_10_I + "]father.parDesc[" + j(-l_14_R.cF) + "]father.useNum[" + j(-l_14_R.cG) + "]father.usePer[" + -l_14_R.cH + "]");
                                        DetailItemInfo -l_16_R = new DetailItemInfo();
                                        -l_16_R.mDescription = j(-l_14_R.cF);
                                        -l_16_R.mLeft = j(-l_14_R.cG);
                                        -l_16_R.mUsed = -l_14_R.cH;
                                        -l_15_R.mFatherInfo = -l_16_R;
                                    }
                                    if (-l_13_R2.cL != null) {
                                        int -l_16_I = 0;
                                        Object -l_17_R = -l_13_R2.cL.iterator();
                                        while (-l_17_R.hasNext()) {
                                            bj -l_14_R2 = (bj) -l_17_R.next();
                                            f.d("TrafficCorrection", "[" + -l_10_I + "][" + -l_16_I + "]son.parDesc[" + j(-l_14_R2.cF) + "]son.useNum[" + j(-l_14_R2.cG) + "]son.usePer[" + -l_14_R2.cH + "]");
                                            -l_16_I++;
                                            DetailItemInfo -l_19_R = new DetailItemInfo();
                                            -l_19_R.mDescription = j(-l_14_R2.cF);
                                            -l_19_R.mLeft = j(-l_14_R2.cG);
                                            -l_19_R.mUsed = -l_14_R2.cH;
                                            -l_15_R.mSonInfoList.add(-l_19_R);
                                        }
                                    }
                                    -l_10_I++;
                                    -l_11_R.add(-l_15_R);
                                }
                                this.vW.sendMessage(this.vW.obtainMessage(4104, -l_11_R));
                                break;
                            }
                        }
                    }
                }
                break;
            case 21:
                f.d("TrafficCorrection", "下发profile");
                if (!(beVar.cx == null || beVar.cx.length == 0)) {
                    JceStruct -l_8_R = new bl();
                    if (a(beVar.cx, -l_8_R)) {
                        int -l_9_I = -l_8_R.province;
                        -l_10_I = -l_8_R.city;
                        -l_11_R = au(-l_8_R.cO);
                        int -l_12_I = -l_8_R.cP;
                        f.d("TrafficCorrection", "province:[" + -l_9_I + "]city[" + -l_10_I + "]carry[" + -l_11_R + "]brand[" + -l_12_I + "]payDay[" + -l_8_R.cR + "]");
                        -l_6_R.a(str, -l_9_I, -l_10_I, -l_11_R, -l_12_I);
                        if (this.vW != null) {
                            -l_14_R = new ProfileInfo();
                            -l_14_R.imsi = str;
                            -l_14_R.province = -l_9_I;
                            -l_14_R.city = -l_10_I;
                            -l_14_R.carry = -l_11_R;
                            -l_14_R.brand = -l_12_I;
                            Message -l_15_R2 = this.vW.obtainMessage(4102, i, 0);
                            -l_15_R2.obj = -l_14_R;
                            this.vW.sendMessage(-l_15_R2);
                            break;
                        }
                    }
                }
                break;
            case 49:
                f.d("TrafficCorrection", "校正失败");
                JceStruct -l_7_R = new bo();
                if (a(beVar.cx, -l_7_R)) {
                    String -l_9_R2;
                    int -l_8_I = -l_7_R.a();
                    Object -l_9_R3 = "";
                    if (-l_8_I == 0) {
                        -l_9_R2 = "短信校正失败";
                    } else if (-l_8_I == 1 && !n(i, 0)) {
                        -l_9_R3 = "不支持运营商接口，强制拉取profile回调";
                        return true;
                    } else {
                        -l_9_R2 = -l_8_I != 1 ? "服务器返回未知类型" : "运营商合作校正失败";
                    }
                    f.d("TrafficCorrection", "type of error is " + -l_9_R2);
                }
                if (this.vW != null) {
                    this.vW.sendMessage(this.vW.obtainMessage(4103, i, -1));
                    break;
                }
                break;
        }
        return -l_4_I;
    }

    private boolean a(byte[] bArr, JceStruct jceStruct) {
        if (bArr == null || jceStruct == null) {
            return false;
        }
        int -l_4_I;
        Object -l_3_R = new JceInputStream(bArr);
        -l_3_R.setServerEncoding("UTF-8");
        try {
            jceStruct.readFrom(-l_3_R);
            -l_4_I = 1;
        } catch (Object -l_5_R) {
            -l_5_R.printStackTrace();
            -l_4_I = 0;
        }
        return -l_4_I;
    }

    private int ap(int -l_22_I) {
        f.d("TrafficCorrection", "[profile上报][Beg]");
        Object -l_11_R = new j(-l_22_I);
        final Object -l_12_R = -l_11_R.df();
        final Object -l_13_R = -l_11_R.dg();
        final Object -l_14_R = -l_11_R.dh();
        final Object -l_15_R = -l_11_R.di();
        final Object -l_16_R = at(-l_22_I);
        final int -l_17_I = -l_11_R.dj();
        try {
            int -l_18_I = Integer.valueOf(-l_12_R).intValue();
            int -l_19_I = Integer.valueOf(-l_13_R).intValue();
            int -l_21_I = Integer.valueOf(-l_15_R).intValue();
            int -l_20_I = be(-l_14_R);
            if (-l_20_I != -1) {
                int -l_3_I;
                int -l_4_I;
                int -l_5_I;
                int -l_6_I;
                int -l_8_I;
                int -l_9_I;
                final int i = -l_22_I;
                jn.cx().a(new jl(this) {
                    final /* synthetic */ i wc;

                    public void a(ArrayList<JceStruct> arrayList, int i) {
                        this.wc.vW.sendMessage(this.wc.vW.obtainMessage(4099, i, 0, this));
                        Object -l_4_R = "";
                        if (i == 0) {
                            -l_4_R = this.wc.vX.b() + "$" + -l_16_R + "$" + -l_12_R + -l_13_R + -l_14_R + -l_15_R + "$" + -l_17_I;
                        }
                        this.wc.vW.sendMessage(this.wc.vW.obtainMessage(4100, i, 0, -l_4_R));
                        f.d("TrafficCorrection", "profile上报结果[" + i + "]guid:[" + this.wc.vX.b() + "]");
                    }
                });
                if (1 != -l_22_I) {
                    -l_3_I = 2003;
                    -l_4_I = 2002;
                    -l_5_I = 2004;
                    -l_6_I = 2005;
                    -l_8_I = 2007;
                    -l_9_I = 2008;
                } else {
                    -l_3_I = 2011;
                    -l_4_I = 2010;
                    -l_5_I = 2012;
                    -l_6_I = 2013;
                    -l_8_I = 2015;
                    -l_9_I = 2016;
                }
                jn.cx().l(-l_4_I, Integer.valueOf(-l_18_I).intValue());
                jn.cx().l(-l_3_I, Integer.valueOf(-l_19_I).intValue());
                jn.cx().l(-l_5_I, -l_20_I);
                jn.cx().l(-l_6_I, Integer.valueOf(-l_21_I).intValue());
                jn.cx().l(-l_8_I, -l_17_I);
                jn.cx().a(-l_9_I, true);
                f.d("TrafficCorrection", "[profile上报][End]");
                return 0;
            }
            f.d("TrafficCorrection", "[error] upload profile Operator error");
            return TrafficErrorCode.ERR_CORRECTION_PROFILE_ILLEGAL;
        } catch (Object -l_22_R) {
            f.d("TrafficCorrection", "[error] upload profile NumberFormatException:" + -l_22_R.getMessage());
            return TrafficErrorCode.ERR_CORRECTION_PROFILE_ILLEGAL;
        }
    }

    private boolean aq(int i) {
        Object -l_2_R = new j(i);
        Object -l_3_R = -l_2_R.df();
        Object -l_4_R = -l_2_R.dg();
        Object -l_5_R = -l_2_R.dh();
        Object -l_6_R = -l_2_R.di();
        f.d("TrafficCorrection", "[检查省、市、运营商、品牌代码]province:[" + -l_3_R + "]city:[" + -l_4_R + "]carry:[" + -l_5_R + "]brand:[" + -l_6_R + "]");
        if (!q.cK(-l_3_R) && !q.cK(-l_4_R) && !q.cK(-l_5_R) && !q.cK(-l_6_R)) {
            return true;
        }
        f.d("TrafficCorrection", "[error]省、市、运营商、品牌代码存在为空");
        return false;
    }

    private boolean ar(int i) {
        Object -l_2_R = new j(i);
        Object -l_3_R = this.vX.b() + "$" + at(i) + "$" + -l_2_R.df() + -l_2_R.dg() + -l_2_R.dh() + -l_2_R.di() + "$" + -l_2_R.dj();
        Object -l_4_R = -l_2_R.dk();
        f.h("TrafficCorrection", "currentInfo:[" + -l_3_R + "]lastSuccessInfo:[" + -l_4_R + "]");
        return -l_3_R.compareTo(-l_4_R) != 0;
    }

    private boolean as(int i) {
        Object -l_2_R = new j(i);
        Object -l_3_R = -l_2_R.dm();
        Object -l_4_R = -l_2_R.dn();
        f.d("TrafficCorrection", "[检查查询码与端口号]queryCode:[" + -l_3_R + "]queryPort:[" + -l_4_R + "]");
        if (!TextUtils.isEmpty(-l_3_R) && !TextUtils.isEmpty(-l_4_R)) {
            return true;
        }
        f.d("TrafficCorrection", "[error]查询码或端口号不合法");
        return false;
    }

    private String at(int i) {
        Object -l_2_R = "";
        if (im.bO() != null) {
            -l_2_R = im.bO().getIMSI(i);
        } else if (i == 0) {
            -l_2_R = l.M(TMSDKContext.getApplicaionContext());
        }
        f.d("TrafficCorrection", "getIMSIBySimSlot:[" + i + "][" + -l_2_R + "");
        return -l_2_R;
    }

    private String au(int i) {
        Object -l_2_R = "";
        if (i == 2) {
            return "CMCC";
        }
        if (i != 1) {
            return i != 3 ? -l_2_R : "TELECOM";
        } else {
            return "UNICOM";
        }
    }

    private String av(int i) {
        switch (i) {
            case 1001:
                return "通过查询码获取到流量短信处理";
            case 1002:
                return "本地校正后上报";
            case 1003:
                return "手动修改上报";
            case 1004:
                return "身份验证";
            case 1007:
                return "手动获取云端数据";
            case 1008:
                return "纠错上报";
            default:
                return "";
        }
    }

    private String aw(int i) {
        switch (i) {
            case 1:
                return "校正类型";
            case 2:
                return "调整校正频率：例如一天校正一次调整为3天校正一次";
            case 3:
                return "复活指令：关闭校正的用户复活。";
            case 4:
                return "直接替换终端当前使用的查询码：换查询码时使用";
            case 5:
                return "暂停校正";
            case 6:
                return "下发模板";
            case 7:
                return "调整校正时机:允许server校正的时间段调整";
            case 8:
                return "替换超时时间";
            case 9:
                return "更换监听运营商端口。";
            case 10:
                return "下发GPRS流量值";
            case 11:
                return "立即执行一次校正";
            case 12:
                return "下发身份认证信息(url+sky)，终端收到该信息后进行省份认证";
            case 13:
                return "下发TD流量值";
            case 14:
                return "下发闲时流量值";
            case 15:
                return "下发一串内容，这串内容需要终端展示给用户看；";
            case 16:
                return "调整校正时机:允许Local校正的时间段调整";
            case 17:
                return "下发推广链接";
            case 20:
                return "流量详情";
            case 21:
                return "下发profile";
            case 49:
                return "校正失败";
            default:
                return "";
        }
    }

    private void ax(int i) {
        f.f("TrafficCorrection", "setQueryInfo, simIndex is " + i);
        Object -l_2_R = new j(i);
        -l_2_R.n(false);
        -l_2_R.m(false);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int b(int i, String str, String str2) {
        f.d("TrafficCorrection", "本地模板分析短信");
        if (TrafficSmsParser.getWrongSmsType(str, str2) == 0) {
            Object -l_4_R = new j(i);
            List -l_5_R = -l_4_R.aB(2);
            List -l_6_R = -l_4_R.aB(1);
            if (-l_5_R.isEmpty() && -l_6_R.isEmpty()) {
                f.d("TrafficCorrection", "模板为空");
                o(i, TrafficErrorCode.ERR_CORRECTION_LOCAL_NO_TEMPLATE);
                a(i, 3, str2);
                return 0;
            }
            int -l_7_I = 0;
            int i2 = 9;
            if (-l_6_R.isEmpty()) {
                f.d("TrafficCorrection", "剩余模板为空");
                -l_7_I = 1;
            } else {
                i2 = a(i, -l_6_R, str, str2, false);
            }
            if (!(-l_7_I == 0 || -l_5_R.isEmpty())) {
                i2 = a(i, -l_5_R, str, str2, true);
            }
            if (i2 == 6 || i2 == 7) {
                f.d("TrafficCorrection", "匹配成功");
                o(i, 0);
            } else {
                o(i, TrafficErrorCode.ERR_CORRECTION_LOCAL_TEMPLATE_UNMATCH);
                a(i, i2, str2);
                f.d("TrafficCorrection", "匹配失败");
            }
            return 0;
        }
        f.d("TrafficCorrection", "[error]TrafficSmsParser.getWrongSmsType异常");
        o(i, TrafficErrorCode.ERR_CORRECTION_BAD_SMS);
        return TrafficErrorCode.ERR_CORRECTION_BAD_SMS;
    }

    private int be(String str) {
        if ("CMCC".equals(str)) {
            return 2;
        }
        if ("UNICOM".equals(str)) {
            return 1;
        }
        return !"TELECOM".equals(str) ? -1 : 3;
    }

    private synchronized void de() {
        f.d("TrafficCorrection", "[注册push listener]");
        this.vX.v(11006, 2);
        this.vX.a(11006, new bh(), 2, this.wb);
    }

    private String j(byte[] bArr) {
        return bArr != null ? new String(bArr) : "";
    }

    private void m(final int -l_6_I, int i) {
        int i2 = 0;
        f.d("TrafficCorrection", "[uploadParam]simIndex:[" + -l_6_I + "]");
        Object -l_3_R = new j(-l_6_I);
        Object -l_4_R = new bg();
        if (-l_3_R.do() && -l_3_R.dp()) {
            -l_4_R.cC = -l_3_R.dm();
            -l_4_R.port = -l_3_R.dn();
        } else {
            -l_4_R.cC = "";
            -l_4_R.port = "";
        }
        JceStruct -l_5_R = new bq();
        -l_5_R.imsi = at(-l_6_I);
        -l_5_R.de = -l_3_R.dq();
        -l_5_R.di = -l_3_R.dr();
        -l_5_R.dd = -l_3_R.ds();
        -l_5_R.dh = -l_3_R.dt();
        -l_5_R.dg = -l_4_R;
        if (-l_3_R.du()) {
            i2 = 2;
        }
        -l_5_R.status = i2;
        -l_5_R.dj = i;
        -l_5_R.df = -l_3_R.dl();
        -l_5_R.cj = -l_6_I;
        f.d("TrafficCorrection", "[upload]-[" + av(1008) + "],内容：[" + a(1008, -l_5_R) + "]");
        this.vX.a(1008, -l_5_R, null, 2, new jy(this) {
            final /* synthetic */ i wc;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                if (i3 != 0) {
                    this.wc.o(-l_6_I, TrafficErrorCode.ERR_CORRECTION_FEEDBACK_UPLOAD_FAIL);
                }
            }
        });
    }

    private void o(int i, int i2) {
        if (i2 != 0) {
            this.vW.sendMessage(this.vW.obtainMessage(4101, i, i2));
        }
        f.d("TrafficCorrection", "[本次校正流程结束]--重置状态");
    }

    public int a(final int i, String str, final String str2, final String -l_9_R, int i2) {
        f.d("TrafficCorrection", "[分析短信]analysisSMS--simIndex:[" + i + "]queryCode:[" + str + "]queryPort:" + str2 + "]smsBody:[" + -l_9_R + "]");
        if (i == 0 || i == 1) {
            if (!(q.cK(str) || q.cK(str2) || q.cK(-l_9_R))) {
                Object -l_6_R = new bg();
                if (-l_6_R != null) {
                    -l_6_R.cC = str;
                    -l_6_R.port = str2;
                }
                if (tmsdk.common.utils.i.hm() == 0) {
                    return b(i, str2, -l_9_R);
                }
                f.d("TrafficCorrection", "有网络，走云短信");
                JceStruct -l_8_R = new bd();
                -l_8_R.imsi = at(i);
                -l_8_R.cm = -l_6_R;
                -l_8_R.sms = -l_9_R;
                -l_8_R.cp = this.vZ;
                -l_8_R.time = i2;
                -l_8_R.ck = new ArrayList();
                -l_8_R.cj = i;
                f.d("TrafficCorrection", "[upload]-[" + av(1001) + "]内容:[" + a(1001, -l_8_R) + "]");
                this.vX.a(1001, -l_8_R, null, 2, new jy(this) {
                    final /* synthetic */ i wc;

                    public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                        if (i3 != 0) {
                            f.d("TrafficCorrection", "有网络，上报短信流程失败，走本地短信分析流程");
                            this.wc.b(i, str2, -l_9_R);
                        }
                    }
                });
                return 0;
            }
        }
        o(i, -6);
        f.d("TrafficCorrection", "参数错误");
        return -6;
    }

    public int getSingletonType() {
        return 1;
    }

    public boolean n(int i, int i2) {
        Object -l_4_R = new j(i).dq();
        if (TextUtils.isEmpty(-l_4_R)) {
            return false;
        }
        Object -l_6_R = -l_4_R.replace("||", "*").split("\\*");
        int -l_7_I = 0;
        while (-l_7_I < -l_6_R.length) {
            try {
                if (Integer.valueOf(-l_6_R[-l_7_I]).intValue() == i2) {
                    return true;
                }
                -l_7_I++;
            } catch (NumberFormatException e) {
            }
        }
        return false;
    }

    public void onCreate(Context context) {
        f.d("TrafficCorrection", "TrafficCorrectionManagerImpl-OnCreate-context:[" + context + "]");
        this.mContext = context;
        this.vX = im.bK();
        de();
        jk.cv().a(jn.cx());
        jk.cv().ah(jn.cx().cs());
        this.vW = new Handler(this, Looper.getMainLooper()) {
            final /* synthetic */ i wc;

            public void handleMessage(Message message) {
                if (message.what == 4097) {
                    a -l_2_R = (a) message.obj;
                    if (this.wc.vY != null && -l_2_R != null) {
                        f.d("TrafficCorrection", "onTrafficInfoNotify--simIndex:[" + -l_2_R.wt + "]trafficClass:[" + -l_2_R.wu + "]" + "]subClass:[" + -l_2_R.wv + "]" + "]kBytes:[" + -l_2_R.ww + "]");
                        this.wc.vY.onTrafficInfoNotify(-l_2_R.wt, -l_2_R.wu, -l_2_R.wv, -l_2_R.ww);
                    }
                } else if (message.what != 4098) {
                    if (message.what == 4099) {
                        jn.cx().b((jl) message.obj);
                    } else if (message.what == 4100) {
                        if (TextUtils.isEmpty((String) message.obj)) {
                            f.d("TrafficCorrection", "onError--simIndex:[" + message.arg1 + "]ERR_CORRECTION_PROFILE_UPLOAD_FAIL");
                            if (this.wc.vY != null) {
                                this.wc.vY.onError(message.arg1, TrafficErrorCode.ERR_CORRECTION_PROFILE_UPLOAD_FAIL);
                            }
                        }
                        new j(message.arg1).bj((String) message.obj);
                    } else if (message.what != 4101) {
                        if (message.what != 4102) {
                            if (message.what != 4103) {
                                if (message.what == 4104) {
                                    ArrayList -l_2_R2 = (ArrayList) message.obj;
                                    if (this.wc.vY != null && -l_2_R2 != null) {
                                        f.d("TrafficCorrection", "onDetailInfoNotify--");
                                        this.wc.vY.onDetailInfoNotify(-l_2_R2);
                                    }
                                }
                            } else if (this.wc.vY != null) {
                                f.d("TrafficCorrection", "onCorrectionResult--simIndex:[" + message.arg1 + "]retCode:[" + message.arg2 + "]");
                                this.wc.vY.onCorrectionResult(message.arg1, message.arg2);
                            }
                        } else if (this.wc.vY != null) {
                            f.d("TrafficCorrection", "onProfileNotify--simIndex:[" + message.arg1 + "]ProfileInfo:[" + ((ProfileInfo) message.obj).toString() + "]");
                            this.wc.vY.onProfileNotify(message.arg1, (ProfileInfo) message.obj);
                        }
                    } else if (this.wc.vY != null) {
                        f.d("TrafficCorrection", "onError--simIndex:[" + message.arg1 + "]errorCode:[" + message.arg2 + "]");
                        this.wc.vY.onError(message.arg1, message.arg2);
                    }
                } else if (this.wc.vY != null) {
                    Object -l_2_R3 = new j(message.arg1);
                    Object -l_3_R = -l_2_R3.dm();
                    Object -l_4_R = -l_2_R3.dn();
                    f.d("TrafficCorrection", "onNeedSmsCorrection--simIndex:[" + message.arg1 + "]queryCode:[" + -l_3_R + "]queryPort:[" + -l_4_R + "]");
                    this.wc.vY.onNeedSmsCorrection(message.arg1, -l_3_R, -l_4_R);
                }
            }
        };
    }

    public void onImsiChanged() {
        f.d("TrafficCorrection", "onImsiChanged");
        TMSDKContext.onImsiChanged();
        if (tmsdk.common.utils.i.hm()) {
            this.vX.gm();
        }
        if (aq(0) && ar(0)) {
            ax(0);
            ap(0);
        }
        if (aq(1) && ar(1)) {
            ax(1);
            ap(1);
        }
    }

    public int requestProfile(int i) {
        f.d("TrafficCorrection", "requestProfile--simIndex:[" + i + "]");
        Object -l_3_R = new j(i).d(i, at(i));
        if (-l_3_R.province != -1) {
            Object -l_4_R = this.vW.obtainMessage(4102, i, 0);
            -l_4_R.obj = -l_3_R;
            this.vW.sendMessage(-l_4_R);
        } else {
            f.d("TrafficCorrection", "本地没有profile信息");
            m(i, 5);
        }
        return 0;
    }

    public int setConfig(int i, String str, String str2, String str3, String str4, int i2) {
        f.d("TrafficCorrection", "[设置省、市、运营商、品牌代码]simIndex:[" + i + "]provinceId:[" + str + "]cityId:[" + str2 + "]carryId:[" + str3 + "]brandId:[" + str4 + "]closingDay:[" + i2 + "]");
        if (i == 0 || i == 1) {
            if (!(q.cK(str) || q.cK(str2) || q.cK(str3) || q.cK(str4))) {
                Object -l_7_R = new j(i);
                -l_7_R.bf(str);
                -l_7_R.bg(str2);
                -l_7_R.bh(str3);
                -l_7_R.bi(str4);
                -l_7_R.ay(i2);
                return 0;
            }
        }
        f.d("TrafficCorrection", "[error]设置信息有的为空");
        return -6;
    }

    public int setTrafficCorrectionListener(ITrafficCorrectionListener iTrafficCorrectionListener) {
        f.d("TrafficCorrection", "[设置流量校正监听]listener:[" + iTrafficCorrectionListener + "]");
        if (iTrafficCorrectionListener == null) {
            return -6;
        }
        this.vY = iTrafficCorrectionListener;
        return 0;
    }

    public int startCorrection(int i) {
        Object -l_3_R;
        f.f("TrafficCorrection", "先检查vid");
        Object -l_2_R = this.vX.gl();
        if (-l_2_R.aH()) {
            -l_3_R = -l_2_R.aJ();
            if (-l_3_R.equals("")) {
                f.f("TrafficCorrection", "支持vid, 没拿到vid：isSupportVid is " + -l_2_R.aH() + ", onGetVidFromPhone is " + -l_3_R);
                if (this.mRetryCount < 10) {
                    this.mRetryCount++;
                    this.vX.gB();
                    return TrafficErrorCode.ERR_CORRECTION_NEED_RETRY;
                } else if (-l_2_R.aF().equals("")) {
                    this.mRetryCount = 0;
                    return TrafficErrorCode.ERR_CORRECTION_NEED_RETRY;
                }
            }
        }
        f.f("TrafficCorrection", "不支持vid");
        this.mRetryCount = 0;
        f.d("TrafficCorrection", "[开始校正]simIndex:[ " + i + "]");
        s.bW(128);
        if (at(i) != null) {
            if (tmsdk.common.utils.i.hm()) {
                this.vX.gm();
            }
            if (i != 0 && i != 1) {
                f.d("TrafficCorrection", "[error]simIndex 不合法");
                return -6;
            } else if (aq(i)) {
                -l_3_R = new j(i);
                if (ar(i)) {
                    f.d("TrafficCorrection", "[需要上报profile][上报profile触发后续校正流程]");
                    if (tmsdk.common.utils.i.hm() == 0) {
                        f.d("TrafficCorrection", "没有网络-[profile上报]结束");
                        return TrafficErrorCode.ERR_CORRECTION_FEEDBACK_UPLOAD_FAIL;
                    } else if (ap(i) != 0) {
                        return TrafficErrorCode.ERR_CORRECTION_PROFILE_UPLOAD_FAIL;
                    }
                }
                int -l_4_I = tmsdk.common.utils.i.hm();
                if (-l_4_I != 0) {
                    if (-l_4_I != 0) {
                        if (ap(i) != 0) {
                            f.d("TrafficCorrection", "[无校正方式，上报profile]simIndex:[" + i + "]");
                        }
                        if (q.cK(-l_3_R.dq())) {
                            f.d("TrafficCorrection", "[无校正方式，纠错上报]simIndex:[" + i + "]");
                            m(i, 5);
                            return TrafficErrorCode.ERR_CORRECTION_NEED_RETRY;
                        }
                        f.f("TrafficCorrection", "simIndex is " + i);
                        f.f("TrafficCorrection", "IsPortFreshed is " + -l_3_R.do());
                        f.f("TrafficCorrection", "IsCodeFreshed is " + -l_3_R.dp());
                        if (!-l_3_R.do() || !-l_3_R.dp()) {
                            f.d("TrafficCorrection", "查询码和端口号没有更新");
                            JceStruct -l_6_R = new ba();
                            -l_6_R.aH = 0;
                            -l_6_R.cj = i;
                            -l_6_R.imsi = at(i);
                            f.d("TrafficCorrection", "[upload]-[" + av(1007) + "]内容:[" + a(1007, -l_6_R));
                            this.vX.a(1007, -l_6_R, null, 2, null);
                            return 0;
                        }
                    }
                    if (n(i, 0)) {
                        f.d("TrafficCorrection", "[运营商云端合作校正]simIndex:[" + i + "]");
                        JceStruct -l_5_R = new ba();
                        -l_5_R.aH = 0;
                        -l_5_R.cj = i;
                        -l_5_R.imsi = at(i);
                        f.d("TrafficCorrection", "[upload]-[" + av(1007) + "]内容:[" + a(1007, -l_5_R));
                        this.vX.a(1007, -l_5_R, null, 2, null);
                    } else if (n(i, 3)) {
                        f.d("TrafficCorrection", "[短信云端校正]simIndex:[" + i + "]");
                        a(i, 2, 3, 5);
                    }
                } else {
                    f.d("TrafficCorrection", "[本地模板匹配]simIndex:[" + i + "]");
                    a(i, 2, 2, 5);
                    return 0;
                }
                return 0;
            } else {
                f.d("TrafficCorrection", "[error]ErrorCode.ERR_CORRECTION_PROFILE_ILLEGAL");
                return TrafficErrorCode.ERR_CORRECTION_PROFILE_ILLEGAL;
            }
        }
        f.d("TrafficCorrection", "imsi为null, 直接返回");
        return -7;
    }
}
