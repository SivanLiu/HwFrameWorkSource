package tmsdkobf;

import android.content.Context;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import tmsdk.common.ErrorCode;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.exception.NetWorkException;
import tmsdk.common.exception.NetworkOnMainThreadException;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.utils.e;
import tmsdk.common.utils.i;
import tmsdk.common.utils.l;
import tmsdk.common.utils.n;
import tmsdk.common.utils.q;

class po implements pl {
    private static String IV = null;
    private static String TAG = "WupSessionHelperImpl";
    private ek IS;
    private fc IT;
    private final String Kh = "tid";
    private final String Ki = "mdtids";
    private String Kj = null;
    private ep Kk;
    private eq Kl;
    private ec Km;
    private eh Kn;
    volatile boolean Ko;
    private Object Kp = new Object();
    private volatile long Kq = 0;
    private volatile long Kr = 0;
    private boolean Ks = false;
    private Context mContext;
    private String mImei = null;
    private md vu;

    public po(Context context) {
        this.mContext = context;
        this.vu = new md("wup");
        if (IV == null) {
            IV = this.vu.getString("guid", null);
        }
        this.mImei = q.cI(l.L(this.mContext));
        this.Kj = q.cI(l.N(this.mContext));
        lw.eI();
        hM();
    }

    private void F(long j) {
        this.Kq = j;
        this.vu.a("last_ft", this.Kq, true);
    }

    private void G(long j) {
        this.Kr = j;
        this.vu.a("cts_td", this.Kr, true);
    }

    private void a(fo foVar, int i, String str, String str2, HashMap<String, Object> hashMap) {
        foVar.E(i);
        foVar.C(str);
        foVar.D(str2);
        foVar.B("UTF-8");
        if (hashMap != null && hashMap.size() > 0) {
            for (Entry -l_8_R : hashMap.entrySet()) {
                foVar.put((String) -l_8_R.getKey(), -l_8_R.getValue());
            }
        }
    }

    private void hM() {
        this.Kq = this.vu.getLong("last_ft", 0);
        this.Kr = this.vu.getLong("cts_td", 0);
    }

    private ec hQ() {
        if (this.Km != null) {
            this.Km.l(IV);
            this.Km.d(this.mImei);
            this.Km.f(this.Kj);
        } else {
            this.Km = new ec();
            this.Km.d(this.mImei);
            this.Km.e(q.cI(l.M(this.mContext)));
            this.Km.f(this.Kj);
            this.Km.g(q.cI(l.O(this.mContext)));
            this.Km.h(q.cI(l.P(this.mContext)));
            this.Km.e(n.iX());
            this.Km.i(q.cI(l.iL()));
            this.Km.j(q.cI(l.getProductName()));
            this.Km.k(q.cI(e.E(this.mContext)));
            this.Km.l(IV);
        }
        return this.Km;
    }

    private int hR() {
        Object -l_2_R = hQ();
        Object -l_3_R = new AtomicReference();
        int -l_1_I = ((pq) ManagerCreatorC.getManager(pq.class)).a(-l_2_R, -l_3_R);
        if (-l_1_I != 0) {
            return -l_1_I;
        }
        eg -l_4_R = (eg) -l_3_R.get();
        if (-l_4_R == null) {
            return -l_1_I;
        }
        IV = -l_4_R.b();
        return (IV == null || IV.equals("")) ? -2001 : -l_1_I;
    }

    private boolean hS() {
        Object -l_1_R = q.cI(l.L(this.mContext));
        Object -l_2_R = q.cI(l.N(this.mContext));
        this.mImei = this.vu.getString("imei", -l_1_R);
        this.Kj = this.vu.getString("mac", -l_2_R);
        if (-l_1_R.equals(this.mImei) && -l_2_R.equals(this.Kj)) {
            return false;
        }
        this.mImei = -l_1_R;
        this.Kj = -l_2_R;
        return true;
    }

    public int a(pp ppVar) {
        return a(ppVar, TMSDKContext.getStrFromEnvMap(TMSDKContext.PRE_HTTP_SERVER_URL));
    }

    public int a(pp ppVar, String str) {
        return a(ppVar, false, str);
    }

    public int a(pp ppVar, boolean z) {
        return a(ppVar, z, TMSDKContext.getStrFromEnvMap(TMSDKContext.PRE_HTTP_SERVER_URL));
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int a(pp ppVar, boolean z, String str) {
        Object -l_12_R;
        Object -l_25_R;
        kv.d(TAG, " runHttpSession() url:" + str);
        boolean -l_4_I = eJ();
        kv.d(TAG, " runHttpSession() couldNotConnect:" + -l_4_I);
        if (-l_4_I) {
            return -64;
        }
        int -l_5_I = ErrorCode.ERR_WUP;
        if (ppVar == null) {
            return -6057;
        }
        Object -l_13_R;
        byte[] -l_12_R2;
        if (!z) {
            -l_5_I = hT();
            if (-l_5_I != 0) {
                return -l_5_I - 60;
            }
        }
        Object -l_6_R = ppVar.Kw;
        fo -l_7_R = ppVar.Kx;
        a(-l_6_R, ppVar.Kt, ppVar.Ku.Kf, ppVar.Ku.Kg, ppVar.Kv);
        int -l_8_I = -l_6_R.p();
        fo -l_10_R = null;
        if (-l_8_I != 999 && hN()) {
            pn -l_11_R = (pn) pm.Ke.get(999);
            Object -l_12_R3 = new HashMap(3);
            -l_12_R3.put("phonetype", ht());
            -l_12_R3.put("userinfo", hu());
            -l_12_R3.put("reqinfo", hU());
            -l_10_R = new fo(true);
            a(-l_10_R, 999, -l_11_R.Kf, -l_11_R.Kg, -l_12_R3);
            kv.n("Chord", "WupSessionHelperImpl runHttpSession() getMainTips");
        }
        Object obj = null;
        if (-l_10_R != null) {
            try {
                -l_13_R = -l_6_R.l();
                Object -l_14_R = -l_10_R.l();
                -l_12_R2 = new byte[(-l_13_R.length + -l_14_R.length)];
                System.arraycopy(-l_13_R, 0, -l_12_R2, 0, -l_13_R.length);
                System.arraycopy(-l_14_R, 0, -l_12_R2, -l_13_R.length, -l_14_R.length);
            } catch (NetWorkException e) {
                -l_12_R = e;
                -l_5_I = -l_12_R.getErrCode();
                kv.o(TAG, "NetWorkException:" + -l_12_R.getMessage());
                -l_12_R.printStackTrace();
                int -l_13_I = -l_5_I;
                if (obj != null) {
                    obj.close();
                }
                if (!this.Ko && -l_5_I == 0) {
                    TMSDKContext.reportChannelInfo();
                }
                return -l_13_I;
            } catch (IllegalArgumentException e2) {
                -l_12_R = e2;
                -l_5_I = -6057;
                kv.o(TAG, "wup agrs error:" + -l_12_R.getMessage());
                -l_12_R.printStackTrace();
                if (obj != null) {
                    obj.close();
                }
            } catch (NetworkOnMainThreadException e3) {
                -l_12_R = e3;
                throw -l_12_R;
            } catch (Throwable th) {
                -l_25_R = th;
                -l_12_R3 = -l_12_R;
            }
        } else {
            -l_12_R2 = -l_6_R.l();
        }
        -l_13_R = TccCryptor.encrypt(-l_12_R2, null);
        obj = lw.e(str, ppVar.Ky);
        obj.setRequestMethod("POST");
        obj.setPostData(-l_13_R);
        int -l_14_I = obj.eK();
        AtomicReference -l_15_R = new AtomicReference();
        -l_5_I = obj.a(false, -l_15_R);
        if (-l_5_I == 0) {
            byte[] -l_16_R = (byte[]) -l_15_R.get();
            if (-l_16_R != null) {
                if (-l_16_R.length > 0) {
                    Object -l_17_R = TccCryptor.decrypt(-l_16_R, null);
                    int -l_18_I = ((((-l_17_R[0] & 255) << 24) | ((-l_17_R[1] & 255) << 16)) | ((-l_17_R[2] & 255) << 8)) | (-l_17_R[3] & 255);
                    if (-l_18_I == -l_17_R.length) {
                        -l_7_R.b(-l_17_R);
                    } else if (-l_18_I > 0 && -l_17_R.length > -l_18_I + 4) {
                        int -l_19_I = ((((-l_17_R[-l_18_I] & 255) << 24) | ((-l_17_R[-l_18_I + 1] & 255) << 16)) | ((-l_17_R[-l_18_I + 2] & 255) << 8)) | (-l_17_R[-l_18_I + 3] & 255);
                        if (-l_18_I + -l_19_I == -l_17_R.length) {
                            fo -l_22_R;
                            Object -l_20_R = new byte[-l_18_I];
                            Object -l_21_R = new byte[-l_19_I];
                            System.arraycopy(-l_17_R, 0, -l_20_R, 0, -l_18_I);
                            System.arraycopy(-l_17_R, -l_18_I, -l_21_R, 0, -l_19_I);
                            fo foVar = new fo(true);
                            foVar.B("UTF-8");
                            foVar.b(-l_21_R);
                            -l_7_R.b(-l_20_R);
                            if (foVar.p() == -l_8_I) {
                                -l_22_R = -l_7_R;
                                -l_7_R = foVar;
                            }
                            ppVar.Kx = -l_7_R;
                            foVar = -l_22_R;
                            er -l_24_R = (er) foVar.a("cmdinfo", (Object) new er());
                            if (-l_24_R != null) {
                                cq(-l_24_R.c());
                            } else {
                                new er().s("");
                            }
                        }
                    }
                }
            }
            -l_5_I = 0;
            if (obj != null) {
                obj.close();
            }
            if (!this.Ko) {
                TMSDKContext.reportChannelInfo();
            }
            return -l_5_I;
        }
        int -l_16_I = -l_5_I;
        if (obj != null) {
            obj.close();
        }
        if (!this.Ko && -l_5_I == 0) {
            TMSDKContext.reportChannelInfo();
        }
        return -l_16_I;
        if (obj != null) {
            obj.close();
        }
        if (!this.Ko && -l_5_I == 0) {
            TMSDKContext.reportChannelInfo();
        }
        throw -l_25_R;
    }

    public Object a(fo foVar, String str, Object obj) {
        if (str == null) {
            str = "";
        }
        return foVar.a(str, obj);
    }

    public void cq(String str) {
        kv.n("Chord", "setNewTipsId() newTipsId:" + str);
        this.vu.a("tid", str, true);
    }

    public boolean eJ() {
        return lw.eJ();
    }

    public boolean hN() {
        synchronized (this.Kp) {
            if (this.Ks) {
                int -l_9_I;
                long -l_2_J = System.currentTimeMillis();
                int -l_6_I = lr.a(new Date(-l_2_J), new Date(this.Kq));
                kv.d("Chord", "couldFetchCloud() nowIsTheDayOfLastFetch: " + -l_6_I);
                if (-l_6_I != 0) {
                    if ((this.Kr < 72 ? 1 : null) == null) {
                        kv.n("Chord", "couldFetchCloud() couldFetchCloud: false");
                        return false;
                    }
                }
                G(0);
                long -l_7_J = -l_2_J - this.Kq;
                if ((-l_7_J <= 0 ? 1 : null) == null) {
                    if ((-l_7_J <= 1200000 ? 1 : null) != null) {
                        -l_9_I = 0;
                        kv.d("Chord", "couldFetchCloud() moreThanBlank: " + -l_9_I);
                        if (-l_9_I == 0) {
                            F(-l_2_J);
                            G(this.Kr + 1);
                            kv.n("Chord", "couldFetchCloud() couldFetchCloud: true");
                            return true;
                        }
                        kv.n("Chord", "couldFetchCloud() couldFetchCloud: false");
                        return false;
                    }
                }
                -l_9_I = 1;
                kv.d("Chord", "couldFetchCloud() moreThanBlank: " + -l_9_I);
                if (-l_9_I == 0) {
                    kv.n("Chord", "couldFetchCloud() couldFetchCloud: false");
                    return false;
                }
                F(-l_2_J);
                G(this.Kr + 1);
                kv.n("Chord", "couldFetchCloud() couldFetchCloud: true");
                return true;
            }
            kv.n("Chord", "couldFetchCloud() mIsCloudReady: false");
            return false;
        }
    }

    public ep hO() {
        if (this.Kk == null) {
            this.Kk = new ep();
            this.Kk.f(2);
        }
        return this.Kk;
    }

    public eq hP() {
        if (this.Kl != null) {
            this.Kl.d(this.mImei);
        } else {
            this.Kl = new eq();
            this.Kl.e(q.cI(l.M(this.mContext)));
            this.Kl.d(this.mImei);
            this.Kl.q(q.cI("19B7C7417A1AB190"));
            this.Kl.r(q.cI("6.1.0"));
            this.Kl.m(3059);
            this.Kl.n(13);
        }
        return this.Kl;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    synchronized int hT() {
        if (IV != null) {
            if (!(IV.equals("") || hS())) {
            }
        }
        int -l_1_I = hR();
        if (-l_1_I != 0) {
            return -l_1_I;
        }
        this.vu.a("imei", this.mImei, false);
        this.vu.a("mac", this.Kj, false);
        this.vu.a("guid", IV, false);
    }

    eh hU() {
        Object -l_1_R = q.cI(this.vu.getString("tid", null));
        kv.n("Chord", "getMainReqInfo() oldTipsId:" + -l_1_R);
        if (this.Kn != null) {
            this.Kn.n(-l_1_R);
        } else {
            this.Kn = new eh();
            this.Kn.n(-l_1_R);
            this.Kn.a(new ex(this.mContext.getPackageName()));
        }
        return this.Kn;
    }

    public ek ht() {
        if (this.IS == null) {
            this.IS = new ek();
            this.IS.f(2);
            this.IS.g(SmsCheckResult.ESCT_201);
        }
        return this.IS;
    }

    public fc hu() {
        int i = 2;
        int i2 = 1;
        if (this.IT != null) {
            this.IT.I = IV;
            this.IT.dl = this.mImei;
            this.IT.lz = e.iB();
            fc fcVar = this.IT;
            if (i.iG() == eb.iJ) {
                i2 = 2;
            }
            fcVar.lx = i2;
        } else {
            this.IT = new fc();
            this.IT.dl = this.mImei;
            this.IT.dq = q.cI("19B7C7417A1AB190");
            this.IT.dr = q.cI(im.bQ());
            this.IT.dw = q.cI(l.iL());
            this.IT.dp = 13;
            int -l_1_I = 0;
            int -l_2_I = 0;
            int -l_3_I = 0;
            Object -l_5_R = "6.1.0".trim().split("[\\.]");
            if (-l_5_R.length >= 3) {
                -l_1_I = Integer.parseInt(-l_5_R[0]);
                -l_2_I = Integer.parseInt(-l_5_R[1]);
                -l_3_I = Integer.parseInt(-l_5_R[2]);
            }
            this.IT.ly = new el(-l_1_I, -l_2_I, -l_3_I);
            this.IT.I = IV;
            this.IT.imsi = q.cI(l.M(this.mContext));
            fc fcVar2 = this.IT;
            if (i.iG() != eb.iJ) {
                i = 1;
            }
            fcVar2.lx = i;
            fc fcVar3 = this.IT;
            if (!e.F(this.mContext)) {
                i2 = 0;
            }
            fcVar3.ib = i2;
            this.IT.iN = n.iX();
            this.IT.L = 3059;
        }
        nz -l_1_R = (nz) ManagerCreatorC.getManager(nz.class);
        if (-l_1_R != null) {
            this.IT.lD = -l_1_R.b();
        }
        kv.n(TAG, "getUserInfo() product: " + this.IT.dp);
        kv.n(TAG, "getUserInfo() new guid: " + this.IT.lD);
        return this.IT;
    }
}
