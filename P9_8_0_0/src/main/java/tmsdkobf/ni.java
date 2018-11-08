package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import tmsdkobf.nw.b;

public class ni {
    private boolean CX = false;
    private nw Dm;
    private volatile boolean Dn = false;
    private volatile String Do = "";
    private volatile long Dp = 0;
    private Context mContext;

    public interface a {
        void a(int i, int i2, int i3, String str);
    }

    public ni(Context context, nw nwVar, boolean z) {
        this.mContext = context;
        this.Dm = nwVar;
        this.CX = z;
        Object -l_4_R = this.Dm.gl().aF();
        boolean -l_5_I = this.Dm.gl().aP();
        if (this.CX == -l_5_I || TextUtils.isEmpty(-l_4_R)) {
            mb.n("GuidCertifier", "[cu_guid]GuidCertifier: no need to clean guid");
        } else {
            mb.n("GuidCertifier", "[cu_guid]GuidCertifier, clean guid for server change(isTest?): " + -l_5_I + " -> " + this.CX);
            this.Dm.gl().a("", false);
            this.Dm.gl().b("", false);
        }
        fC();
    }

    public static void a(Context context, int i, String str) {
        try {
            Object -l_4_R = new Intent(String.format("action.guid.got:%s", new Object[]{context.getPackageName()}));
            -l_4_R.putExtra("k.rc", i);
            -l_4_R.putExtra("k.g", str);
            context.sendBroadcast(-l_4_R);
        } catch (Object -l_3_R) {
            mb.b("GuidCertifier", "[cu_guid]sendBroadcast(): " + -l_3_R, -l_3_R);
        }
    }

    private void a(String str, br brVar, boolean z) {
        if (!TextUtils.isEmpty(str)) {
            this.Do = str == null ? "" : str;
            this.Dn = true;
            this.Dm.gl().f(this.CX);
            this.Dm.gl().a(str, true);
            this.Dm.gl().b(str, true);
            this.Dm.gl().b(brVar);
        }
    }

    private void a(final br brVar, String str) {
        mb.n("GuidCertifier", "[cu_guid]updateGuid(), for: " + this.Do);
        final int -l_3_I = ns.fW().fP();
        JceStruct -l_4_R = b(brVar, str);
        bw -l_5_R = new bw();
        -l_5_R.ey = -l_3_I;
        -l_5_R.bz = 2;
        -l_5_R.data = nh.a(this.mContext, -l_4_R, 2, -l_5_R);
        if (-l_5_R.data != null) {
            mb.n("GuidCertifier", "[cu_guid]updateGuid(), cur info: " + c(brVar));
            ArrayList -l_6_R = new ArrayList();
            -l_6_R.add(-l_5_R);
            nt.ga().a(-l_5_R.ey, -1, null);
            this.Dm.a(0, 0, false, -l_6_R, new b(this) {
                final /* synthetic */ ni Dt;

                public void a(boolean z, int i, int i2, ArrayList<ce> arrayList) {
                    mb.d("GuidCertifier", "updateGuid() retCode: " + i);
                    if (i == 0) {
                        int -l_5_I = -21250000;
                        if (arrayList != null && arrayList.size() > 0) {
                            Object -l_6_R = arrayList.iterator();
                            while (-l_6_R.hasNext()) {
                                ce -l_7_R = (ce) -l_6_R.next();
                                if (-l_7_R != null && 10002 == -l_7_R.bz) {
                                    if (-l_7_R.eB != 0) {
                                        mb.o("GuidCertifier", "[cu_guid]updateGuid(), mazu error: " + -l_7_R.eB);
                                        -l_5_I = -l_7_R.eB;
                                    } else if (-l_7_R.eC == 0) {
                                        -l_5_I = 0;
                                        mb.d("GuidCertifier", "[cu_guid]updateGuid(), succ, save info to db, mGuid: " + this.Dt.Do);
                                        this.Dt.a(this.Dt.Do, brVar, false);
                                    } else {
                                        mb.o("GuidCertifier", "[cu_guid]updateGuid(), dataRetCode: " + -l_7_R.eC);
                                        -l_5_I = -21300000;
                                    }
                                }
                            }
                        } else {
                            mb.o("GuidCertifier", "[cu_guid]updateGuid(), no sashimi, serverSashimis: " + arrayList);
                            -l_5_I = -21250000;
                        }
                        nt.ga().a("GuidCertifier", 10002, -l_3_I, null, 30, -l_5_I);
                        nt.ga().bq(-l_3_I);
                        return;
                    }
                    mb.o("GuidCertifier", "[cu_guid]updateGuid() ESharkCode.ERR_NONE != retCode, retCode: " + i);
                    nt.ga().a("GuidCertifier", 10002, -l_3_I, null, 30, i);
                    nt.ga().bq(-l_3_I);
                }
            });
            return;
        }
        mb.s("GuidCertifier", "[cu_guid]updateGuid(), jceStruct2DataForSend failed");
    }

    private br b(boolean z, String str) {
        int -l_8_I = 0;
        if (fA()) {
            mb.n("GuidCertifier", "[cu_guid]getCurInfoOfGuidIfNeed(), should register, donnot update, mGuid: " + this.Do + " fromPhone: " + this.Dn);
            return null;
        } else if (!w(z)) {
            return null;
        } else {
            br -l_3_R = fD();
            if (-l_3_R == null) {
                mb.s("GuidCertifier", "[cu_guid]getCurInfoOfGuidIfNeed(), null == realInfo");
                return null;
            } else if (TextUtils.isEmpty(str)) {
                br -l_4_R = this.Dm.gl().aL();
                if (-l_4_R != null) {
                    int -l_5_I = ((((((((((((((((((((((((((((((((((((((((((((((((((((w(-l_3_R.dl, -l_4_R.dl) | 0) | w(-l_3_R.imsi, -l_4_R.imsi)) | w(-l_3_R.dU, -l_4_R.dU)) | w(-l_3_R.dm, -l_4_R.dm)) | w(-l_3_R.dn, -l_4_R.dn)) | w(-l_3_R.do, -l_4_R.do)) | u(-l_3_R.dp, -l_4_R.dp)) | w(-l_3_R.dq, -l_4_R.dq)) | u(-l_3_R.L, -l_4_R.L)) | w(-l_3_R.dr, -l_4_R.dr)) | u(-l_3_R.ds, -l_4_R.ds)) | u(-l_3_R.dt, -l_4_R.dt)) | b(-l_3_R.du, -l_4_R.du)) | w(-l_3_R.dv, -l_4_R.dv)) | w(-l_3_R.dw, -l_4_R.dw)) | u(-l_3_R.dx, -l_4_R.dx)) | w(-l_3_R.dy, -l_4_R.dy)) | u(-l_3_R.dz, -l_4_R.dz)) | u(-l_3_R.dA, -l_4_R.dA)) | w(-l_3_R.dB, -l_4_R.dB)) | w(-l_3_R.ed, -l_4_R.ed)) | w(-l_3_R.dC, -l_4_R.dC)) | u(-l_3_R.dD, -l_4_R.dD)) | w(-l_3_R.dE, -l_4_R.dE)) | c(-l_3_R.dF, -l_4_R.dF)) | c(-l_3_R.dG, -l_4_R.dG)) | c(-l_3_R.dH, -l_4_R.dH)) | c(-l_3_R.ei, -l_4_R.ei)) | w(-l_3_R.dI, -l_4_R.dI)) | w(-l_3_R.dJ, -l_4_R.dJ)) | w(-l_3_R.dK, -l_4_R.dK)) | w(-l_3_R.version, -l_4_R.version)) | u(-l_3_R.dY, -l_4_R.dY)) | w(-l_3_R.dZ, -l_4_R.dZ)) | w(-l_3_R.dN, -l_4_R.dN)) | w(-l_3_R.ea, -l_4_R.ea)) | w(-l_3_R.eb, -l_4_R.eb)) | w(-l_3_R.ec, -l_4_R.ec)) | w(-l_3_R.ee, -l_4_R.ee)) | w(-l_3_R.ef, -l_4_R.ef)) | w(-l_3_R.eg, -l_4_R.eg)) | w(-l_3_R.eh, -l_4_R.eh)) | w(-l_3_R.dO, -l_4_R.dO)) | w(-l_3_R.ej, -l_4_R.ej)) | w(-l_3_R.dP, -l_4_R.dP)) | w(-l_3_R.dL, -l_4_R.dL)) | w(-l_3_R.dM, -l_4_R.dM)) | w(-l_3_R.ek, -l_4_R.ek)) | b(-l_3_R.dS, -l_4_R.dS)) | u(-l_3_R.el, -l_4_R.el)) | w(-l_3_R.em, -l_4_R.em)) | w(-l_3_R.en, -l_4_R.en)) | w(-l_3_R.eo, -l_4_R.eo);
                    Object -l_6_R = this.Dm.gl().aG();
                    Object -l_7_R = b();
                    if (!(TextUtils.isEmpty(-l_6_R) || -l_6_R.equals(-l_7_R))) {
                        -l_8_I = 1;
                    }
                    if ((-l_5_I | -l_8_I) != 0) {
                        mb.r("GuidCertifier", "[cu_guid]getCurInfoOfGuidIfNeed(), yes, |savedInfo|" + c(-l_4_R));
                        mb.r("GuidCertifier", "[cu_guid]getCurInfoOfGuidIfNeed(), yes, |realInfo|" + c(-l_3_R));
                        return -l_3_R;
                    }
                    mb.n("GuidCertifier", "[cu_guid]getCurInfoOfGuidIfNeed(), info not changed, no need");
                    return null;
                }
                mb.s("GuidCertifier", "[cu_guid]getCurInfoOfGuidIfNeed(), null == savedInfo");
                return null;
            } else {
                mb.s("GuidCertifier", "[cu_guid_p]getCurInfoOfGuidIfNeed(), refreshKey is not empty, server requires update guid: " + str);
                return -l_3_R;
            }
        }
    }

    private bt b(br brVar, String str) {
        Object -l_3_R = new bt();
        -l_3_R.eq = brVar;
        -l_3_R.er = b();
        -l_3_R.es = this.Dm.gl().aG();
        -l_3_R.et = str;
        mb.n("GuidCertifier", "[cu_guid_p]getCSUpdateRegist(), sdGuid: " + -l_3_R.es + " curGuid: " + -l_3_R.er + " refreshKey: " + str);
        return -l_3_R;
    }

    private boolean b(boolean z, boolean z2) {
        return z != z2;
    }

    private String c(br brVar) {
        Object -l_2_R = new StringBuilder();
        -l_2_R.append("|imei|" + brVar.dl);
        -l_2_R.append("|imsi|" + brVar.imsi);
        -l_2_R.append("|imsi_2|" + brVar.dU);
        -l_2_R.append("|mac|" + brVar.dm);
        -l_2_R.append("|qq|" + brVar.dn);
        -l_2_R.append("|phone|" + brVar.do);
        -l_2_R.append("|product|" + brVar.dp);
        -l_2_R.append("|lc|" + brVar.dq);
        -l_2_R.append("|buildno|" + brVar.L);
        -l_2_R.append("|channelid|" + brVar.dr);
        -l_2_R.append("|platform|" + brVar.ds);
        -l_2_R.append("|subplatform|" + brVar.dt);
        -l_2_R.append("|isbuildin|" + brVar.du);
        -l_2_R.append("|pkgname|" + brVar.dv);
        -l_2_R.append("|ua|" + brVar.dw);
        -l_2_R.append("|sdkver|" + brVar.dx);
        -l_2_R.append("|androidid|" + brVar.dy);
        -l_2_R.append("|lang|" + brVar.dz);
        -l_2_R.append("|simnum|" + brVar.dA);
        -l_2_R.append("|cpu|" + brVar.dB);
        -l_2_R.append("|cpu_abi2|" + brVar.ed);
        -l_2_R.append("|cpufreq|" + brVar.dC);
        -l_2_R.append("|cpunum|" + brVar.dD);
        -l_2_R.append("|resolution|" + brVar.dE);
        -l_2_R.append("|ram|" + brVar.dF);
        -l_2_R.append("|rom|" + brVar.dG);
        -l_2_R.append("|sdcard|" + brVar.dH);
        -l_2_R.append("|inner_storage|" + brVar.ei);
        -l_2_R.append("|build_brand|" + brVar.dI);
        -l_2_R.append("|build_version_incremental|" + brVar.dJ);
        -l_2_R.append("|build_version_release|" + brVar.dK);
        -l_2_R.append("|version|" + brVar.version);
        -l_2_R.append("|extSdkVer|" + brVar.dY);
        -l_2_R.append("|pkgkey|" + brVar.dZ);
        -l_2_R.append("|manufactory|" + brVar.dN);
        -l_2_R.append("|cam_pix|" + brVar.dQ);
        -l_2_R.append("|front_cam_pix|" + brVar.dR);
        -l_2_R.append("|product_device|" + brVar.ea);
        -l_2_R.append("|product_board|" + brVar.eb);
        -l_2_R.append("|build_product|" + brVar.ec);
        -l_2_R.append("|rom_fingerprint|" + brVar.ee);
        -l_2_R.append("|product_lanuage|" + brVar.ef);
        -l_2_R.append("|product_region|" + brVar.eg);
        -l_2_R.append("|build_radiover|" + brVar.eh);
        -l_2_R.append("|board_platform|" + brVar.dO);
        -l_2_R.append("|board_platform_mtk|" + brVar.ej);
        -l_2_R.append("|screen_pdi|" + brVar.dP);
        -l_2_R.append("|romname|" + brVar.dL);
        -l_2_R.append("|romversion|" + brVar.dM);
        -l_2_R.append("|kernel_ver|" + brVar.ek);
        -l_2_R.append("|isdual|" + brVar.dS);
        -l_2_R.append("|rom_manufactory_version|" + brVar.em);
        -l_2_R.append("|insideCid|" + brVar.en);
        -l_2_R.append("|outsideCid|" + brVar.eo);
        return -l_2_R.toString();
    }

    private boolean c(long j, long j2) {
        return j != j2;
    }

    private br fD() {
        Object -l_1_R = this.Dm.gl().aN();
        if (-l_1_R != null) {
            if (-l_1_R.dl == null) {
                -l_1_R.dl = "";
            }
            return -l_1_R;
        }
        throw new RuntimeException("onGetRealInfoOfGuid() return null");
    }

    private boolean u(int i, int i2) {
        return i != i2;
    }

    private boolean w(String str, String str2) {
        int -l_3_I = 0;
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        if (TextUtils.isEmpty(str2)) {
            return true;
        }
        if (!str.equals(str2)) {
            -l_3_I = 1;
        }
        return -l_3_I;
    }

    private boolean w(boolean z) {
        boolean z2 = true;
        long -l_2_J = System.currentTimeMillis();
        if (z) {
            mb.n("GuidCertifier", "[cu_guid]shouldCheckUpdate(), forceCheck, true");
            this.Dp = -l_2_J;
            this.Dm.gl().g(-l_2_J);
            return true;
        }
        int -l_4_I = 0;
        if (this.Dp > 0) {
            if (lr.a(-l_2_J, this.Dp, 60)) {
                mb.n("GuidCertifier", "[cu_guid]shouldCheckUpdate(), [mem] more than 1h, continue check...");
            }
            return -l_4_I;
        }
        mb.n("GuidCertifier", "[cu_guid]shouldCheckUpdate(), [mem] first check after boot, continue check...");
        this.Dp = -l_2_J;
        long -l_5_J = this.Dm.gl().aO();
        if (-l_5_J <= 0) {
            z2 = false;
        }
        if (!z2) {
            mb.n("GuidCertifier", "[cu_guid]shouldCheckUpdate(), [file] first check, just record the time");
            this.Dm.gl().g(-l_2_J);
        } else if (lr.a(-l_2_J, -l_5_J, 720) == 0) {
            mb.n("GuidCertifier", "[cu_guid]shouldCheckUpdate(), [file] less than 12h, donnot check");
        } else {
            mb.n("GuidCertifier", "[cu_guid]shouldCheckUpdate(), [file] more than 12h, should check");
            -l_4_I = 1;
            this.Dm.gl().g(-l_2_J);
        }
        return -l_4_I;
    }

    static void x(Context context) {
        try {
            Object -l_2_R = new Intent(String.format("action.reg.guid:%s", new Object[]{context.getPackageName()}));
            -l_2_R.setPackage(context.getPackageName());
            context.sendBroadcast(-l_2_R);
        } catch (Object -l_1_R) {
            mb.b("GuidCertifier", "[cu_guid]requestSendProcessRegisterGuid(): " + -l_1_R, -l_1_R);
        }
    }

    public void a(final a aVar) {
        mb.n("GuidCertifier", "[cu_guid]registerGuid()");
        if (fA() != 0) {
            this.Dm.gl().aT();
            final br -l_3_R = fD();
            bw -l_4_R = new bw();
            final int -l_5_I = ns.fW().fP();
            -l_4_R.ey = -l_5_I;
            -l_4_R.bz = 1;
            -l_4_R.data = nh.a(this.mContext, (JceStruct) -l_3_R, 1, -l_4_R);
            if (-l_4_R.data != null) {
                mb.n("GuidCertifier", "[cu_guid]registerGuid(), cur info: " + c(-l_3_R));
                ArrayList -l_7_R = new ArrayList();
                -l_7_R.add(-l_4_R);
                nt.ga().a(-l_4_R.ey, -1, null);
                this.Dm.b(-l_7_R, new b(this) {
                    final /* synthetic */ ni Dt;

                    public void a(boolean z, int i, int i2, ArrayList<ce> arrayList) {
                        if (i != 0) {
                            mb.o("GuidCertifier", "[cu_guid]registerGuid(), retCode: " + i);
                            aVar.a(-l_5_I, 1, i, null);
                        } else if (arrayList == null) {
                            mb.o("GuidCertifier", "[cu_guid]registerGuid(), null == serverSashimis");
                            aVar.a(-l_5_I, 1, -21250000, null);
                        } else if (arrayList.size() > 0) {
                            ce -l_5_R = (ce) arrayList.get(0);
                            if (-l_5_R == null) {
                                mb.o("GuidCertifier", "[cu_guid]registerGuid(), serverSashimi is null");
                                aVar.a(-l_5_I, 1, -21250000, null);
                            } else if (-l_5_R.eB != 0) {
                                mb.o("GuidCertifier", "[cu_guid]registerGuid(), mazu error: " + -l_5_R.eB);
                                aVar.a(-l_5_I, 1, -l_5_R.eB, null);
                            } else if (-l_5_R.eC == 0) {
                                Object -l_6_R = -l_5_R.data;
                                if (-l_6_R != null) {
                                    mb.d("GuidCertifier", "[cu_guid]registerGuid() rs.data.length: " + -l_5_R.data.length);
                                    try {
                                        Object -l_7_R = nh.a(this.Dt.mContext, this.Dt.Dm.ap().DX.getBytes(), -l_6_R, new ca(), false, -l_5_R.eE);
                                        if (-l_7_R != null) {
                                            ca -l_9_R = (ca) -l_7_R;
                                            mb.d("GuidCertifier", "[cu_guid]registerGuid(), guid got: " + -l_9_R.I);
                                            this.Dt.a(-l_9_R.I, -l_3_R, true);
                                            aVar.a(-l_5_I, 1, 0, -l_9_R.I);
                                            return;
                                        }
                                        mb.o("GuidCertifier", "[cu_guid]registerGuid(), decode jce failed: null");
                                        aVar.a(-l_5_I, 1, -21000400, null);
                                        return;
                                    } catch (Object -l_9_R2) {
                                        mb.o("GuidCertifier", "[cu_guid]registerGuid(), decode jce exception: " + -l_9_R2);
                                        aVar.a(-l_5_I, 1, -21000400, null);
                                        return;
                                    }
                                }
                                mb.o("GuidCertifier", "[cu_guid]registerGuid(), null == respData");
                                aVar.a(-l_5_I, 1, -21000005, null);
                            } else {
                                mb.o("GuidCertifier", "[cu_guid]registerGuid(), dataRetCode: " + -l_5_R.eC);
                                aVar.a(-l_5_I, 1, -21300000, null);
                            }
                        } else {
                            mb.o("GuidCertifier", "[cu_guid]registerGuid(), serverSashimis.size() <= 0");
                            aVar.a(-l_5_I, 1, -21250000, null);
                        }
                    }
                });
                return;
            }
            mb.s("GuidCertifier", "[cu_guid]registerGuid(), jceStruct2DataForSend failed");
            aVar.a(-l_5_I, 1, -20001500, null);
            return;
        }
        mb.d("GuidCertifier", "[cu_guid]registerGuid(), not necessary, mGuid: " + this.Do);
    }

    public void a(boolean z, String str) {
        if (nu.aB()) {
            br -l_3_R = b(z, str);
            if (-l_3_R != null) {
                a(-l_3_R, str);
                return;
            }
            return;
        }
        mb.n("GuidCertifier", "[cu_guid] checUpdateGuid(), not send process, ignore!");
    }

    public String b() {
        return this.Do == null ? "" : this.Do;
    }

    public boolean fA() {
        if (nu.aB()) {
            return TextUtils.isEmpty(b()) || !this.Dn;
        } else {
            return false;
        }
    }

    public boolean fB() {
        return TextUtils.isEmpty(b()) || !this.Dn;
    }

    public void fC() {
        this.Do = this.Dm.gl().aF();
        if (TextUtils.isEmpty(this.Do)) {
            this.Dn = false;
            this.Do = this.Dm.gl().aG();
            if (this.Do == null) {
                this.Do = "";
            }
        } else {
            this.Dn = true;
        }
        mb.n("GuidCertifier", "[cu_guid]refreshGuid(), mGuid: " + this.Do + " fromPhone: " + this.Dn);
    }
}
