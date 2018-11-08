package tmsdkobf;

import android.os.Environment;
import android.text.TextUtils;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import com.qq.taf.jce.JceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import tmsdk.common.TMSDKContext;
import tmsdkobf.nj.a;
import tmsdkobf.nq.b;

public class gg {
    private static int VERSION = 3;
    public static String nY;
    private static gg ob = null;
    private jx nZ = gf.S().U();
    private gm oa = new gm();

    static {
        nY = null;
        try {
            nY = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/.tmfs/" + "sk_v" + (TMSDKContext.getStrFromEnvMap(TMSDKContext.PRE_IS_TEST).equals("true") == 0 ? "" : "_test") + ".dat";
        } catch (Throwable th) {
        }
    }

    private gg() {
        am();
    }

    private long ac(String str) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean ad(String str) {
        try {
            return Boolean.parseBoolean(str);
        } catch (Exception e) {
            return false;
        }
    }

    private int ae(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean af(String str) {
        try {
            return Boolean.parseBoolean(str);
        } catch (Exception e) {
            return false;
        }
    }

    public static gg al() {
        if (ob == null) {
            Object -l_0_R = gg.class;
            synchronized (gg.class) {
                if (ob == null) {
                    ob = new gg();
                }
            }
        }
        return ob;
    }

    private synchronized void am() {
        if (this.nZ != null) {
            int -l_1_I = this.nZ.getInt("key_shark_dao_ver", -1);
            if (-l_1_I < 1) {
                a(aw());
            }
            if (-l_1_I < 2) {
                Object -l_2_R = an();
                Object -l_3_R = ao();
                if (!(TextUtils.isEmpty(-l_2_R) || TextUtils.isEmpty(-l_3_R))) {
                    mb.n("SharkDao", "translate rsakey...");
                    b -l_4_R = new b();
                    -l_4_R.DX = -l_2_R;
                    -l_4_R.DW = -l_3_R;
                    a(-l_4_R);
                }
            }
            this.nZ.putInt("key_shark_dao_ver", VERSION);
        }
    }

    private String an() {
        return kk.d(TMSDKContext.getApplicaionContext(), this.nZ.getString("key_ek", ""));
    }

    private String ao() {
        return kk.d(TMSDKContext.getApplicaionContext(), this.nZ.getString("key_sid", ""));
    }

    private LinkedHashMap<String, a> ay() {
        Object -l_10_R;
        Object -l_1_R = new LinkedHashMap();
        Object -l_3_R = kk.d(TMSDKContext.getApplicaionContext(), this.nZ.getString("key_hips", ""));
        if (TextUtils.isEmpty(-l_3_R)) {
            mb.n("SharkDao", "[ip_list]getAllHIPListInfos(), none is saved");
            return -l_1_R;
        }
        Object -l_4_R = -l_3_R.split("\\|");
        if (-l_4_R == null || -l_4_R.length == 0) {
            mb.s("SharkDao", "[ip_list]getAllHIPListInfos(), item number is 0!");
            return -l_1_R;
        }
        Object -l_5_R = -l_4_R;
        for (Object -l_8_R : -l_4_R) {
            if (!TextUtils.isEmpty(-l_8_R)) {
                Object -l_9_R = -l_8_R.split(",");
                if (-l_9_R != null && -l_9_R.length > 0) {
                    try {
                        -l_10_R = -l_9_R[0];
                        long -l_11_J = Long.parseLong(-l_9_R[1]);
                        Object -l_13_R = -l_9_R[2].split("#");
                        if (-l_13_R != null) {
                            -l_1_R.put(-l_10_R, new a(-l_11_J, nj.a(Arrays.asList(-l_13_R), false), false));
                        }
                    } catch (Object -l_10_R2) {
                        mb.o("SharkDao", "[ip_list]getAllHIPListInfos() exception: " + -l_10_R2);
                    }
                }
            }
        }
        mb.n("SharkDao", "[ip_list]getAllHIPListInfos(), size: " + -l_1_R.size());
        return -l_1_R;
    }

    public void Z(String str) {
        Object -l_2_R = kk.c(TMSDKContext.getApplicaionContext(), str);
        if (-l_2_R != null) {
            this.nZ.putString("key_gd", -l_2_R);
        }
    }

    public void a(String str, long j, List<String> list) {
        if (str != null) {
            int -l_5_I = (((j > 0 ? 1 : (j == 0 ? 0 : -1)) <= 0 ? 1 : null) != null || list == null) ? 1 : 0;
            mb.d("SharkDao", "[ip_list]setHIPListInfo(), op=" + (-l_5_I == 0 ? "[set] " : "[delete] ") + "|key=" + str);
            Object -l_6_R = ay();
            LinkedHashMap -l_7_R = new LinkedHashMap();
            for (Entry -l_9_R : -l_6_R.entrySet()) {
                String -l_10_R = (String) -l_9_R.getKey();
                a -l_11_R = (a) -l_9_R.getValue();
                if (!(-l_10_R == null || -l_11_R == null)) {
                    if (-l_11_R.isValid()) {
                        -l_7_R.put(-l_10_R, -l_11_R);
                    } else {
                        mb.o("SharkDao", "[ip_list]setHIPListInfo(), remove expired:　" + -l_10_R);
                    }
                }
            }
            if (-l_5_I == 0) {
                a aVar = new a(j, list, false);
                if (aVar.isValid()) {
                    -l_7_R.put(str, aVar);
                }
            } else {
                -l_7_R.remove(str);
            }
            if (-l_7_R.size() > 10) {
                ArrayList arrayList = new ArrayList(-l_7_R.keySet());
                mb.n("SharkDao", "[ip_list]setHIPListInfo(), too manay, keyList: " + arrayList);
                String -l_9_R2 = (String) arrayList.get(0);
                -l_7_R.remove(-l_9_R2);
                mb.n("SharkDao", "[ip_list]setHIPListInfo(), too manay, remove firstKey: " + -l_9_R2);
            }
            StringBuilder -l_8_R = new StringBuilder();
            int -l_9_I = 0;
            for (Entry -l_11_R2 : -l_7_R.entrySet()) {
                String -l_12_R = (String) -l_11_R2.getKey();
                a -l_13_R = (a) -l_11_R2.getValue();
                if (!(-l_12_R == null || -l_13_R == null)) {
                    long -l_14_J = -l_13_R.DG;
                    if ((-l_14_J > System.currentTimeMillis() ? 1 : null) != null) {
                        Object -l_16_R = new StringBuilder();
                        int -l_17_I = 0;
                        for (String -l_19_R : -l_13_R.DH) {
                            if (-l_17_I > 0) {
                                -l_16_R.append("#");
                            }
                            -l_16_R.append(-l_19_R);
                            -l_17_I++;
                        }
                        Object -l_18_R = new StringBuilder();
                        -l_18_R.append(-l_12_R).append(",").append(-l_14_J).append(",").append(-l_16_R.toString());
                        if (-l_9_I > 0) {
                            -l_8_R.append("|");
                        }
                        -l_8_R.append(-l_18_R.toString());
                        -l_9_I++;
                    }
                }
            }
            mb.n("SharkDao", "[ip_list]setHIPListInfo(), new size: " + -l_9_I + ", before encode: " + -l_8_R.toString());
            Object -l_10_R2 = kk.c(TMSDKContext.getApplicaionContext(), -l_8_R.toString());
            if (-l_10_R2 != null) {
                this.nZ.putString("key_hips", -l_10_R2);
                return;
            } else {
                mb.o("SharkDao", "[ip_list]getEncodeString for HIPLists failed");
                return;
            }
        }
        mb.o("SharkDao", "[ip_list]setHIPListInfo(), bad arg, key == null");
    }

    public void a(br brVar) {
        try {
            this.oa.a(10000, brVar.toByteArray("UTF-8"));
        } catch (Object -l_2_R) {
            mb.e("SharkDao", -l_2_R);
        }
    }

    public void a(h hVar) {
        if (hVar != null) {
            Object -l_2_R = nn.d(hVar);
            if (-l_2_R != null) {
                Object -l_3_R = com.tencent.tcuser.util.a.bytesToHexString(-l_2_R);
                if (-l_3_R != null) {
                    Object -l_4_R = kk.c(TMSDKContext.getApplicaionContext(), -l_3_R);
                    if (-l_4_R != null) {
                        this.nZ.putString("key_s_c", -l_4_R);
                        mb.n("SharkDao", "[shark_conf]setSharkConf() succ");
                    }
                }
            }
        }
    }

    public void a(b bVar) {
        Object -l_2_R = "" + bVar.DX + "|" + bVar.DW;
        mb.n("SharkDao", "[rsa_key]setRsaKey(), str: " + -l_2_R);
        Object -l_3_R = kk.c(TMSDKContext.getApplicaionContext(), -l_2_R);
        if (-l_3_R != null) {
            this.nZ.putString("key_rsa", -l_3_R);
        }
    }

    public void aa(String str) {
        Object -l_2_R = kk.c(TMSDKContext.getApplicaionContext(), str);
        if (-l_2_R != null) {
            this.nZ.putString("key_vd", -l_2_R);
            mb.n("SharkDao", "[cu_vid] setVidInPhone() vid: " + str);
        }
    }

    public void ab(String str) {
        Object -l_2_R = kk.c(TMSDKContext.getApplicaionContext(), str);
        if (-l_2_R != null) {
            int -l_3_I = 0;
            if (nY != null) {
                -l_3_I = kl.a(-l_2_R.getBytes(), nY);
            }
            mb.n("SharkDao", "[cu_vid] setVidInSD(), vid: " + str + " isSaved: " + -l_3_I);
        }
    }

    public a ag(String str) {
        return (a) ay().get(str);
    }

    public b ap() {
        Object -l_2_R = kk.d(TMSDKContext.getApplicaionContext(), this.nZ.getString("key_rsa", ""));
        if (TextUtils.isEmpty(-l_2_R)) {
            return null;
        }
        int -l_4_I = -l_2_R.indexOf("|");
        if (-l_4_I <= 0 || -l_4_I >= -l_2_R.length() - 1) {
            return null;
        }
        Object -l_3_R = new b();
        -l_3_R.DX = -l_2_R.substring(0, -l_4_I);
        -l_3_R.DW = -l_2_R.substring(-l_4_I + 1);
        return -l_3_R;
    }

    public String aq() {
        Object -l_2_R = kk.d(TMSDKContext.getApplicaionContext(), this.nZ.getString("key_vd", ""));
        mb.n("SharkDao", "[cu_vid] getVidInPhone() vid: " + -l_2_R);
        return -l_2_R;
    }

    public String ar() {
        String -l_1_R = null;
        Object -l_2_R = kl.aV(nY);
        if (-l_2_R != null) {
            -l_1_R = kk.d(TMSDKContext.getApplicaionContext(), new String(-l_2_R));
        }
        mb.n("SharkDao", "[cu_vid] getVidInSD(), vid: " + -l_1_R);
        return -l_1_R;
    }

    public String as() {
        return kk.d(TMSDKContext.getApplicaionContext(), this.nZ.getString("key_gd", ""));
    }

    public String at() {
        return kk.d(TMSDKContext.getApplicaionContext(), this.nZ.getString("key_ws_gd", null));
    }

    public long au() {
        try {
            return Long.parseLong(kk.d(TMSDKContext.getApplicaionContext(), this.nZ.getString("key_gd_ck_tm", "")));
        } catch (Exception e) {
            return 0;
        }
    }

    public br av() {
        Object -l_1_R = new br();
        try {
            byte[] -l_2_R = this.oa.M(10000);
            if (-l_2_R != null) {
                Object -l_3_R = new JceInputStream(-l_2_R);
                -l_3_R.setServerEncoding("UTF-8");
                -l_1_R.readFrom(-l_3_R);
            }
        } catch (Object -l_2_R2) {
            mb.e("SharkDao", -l_2_R2);
        }
        return -l_1_R;
    }

    @Deprecated
    public br aw() {
        Object -l_1_R = new br();
        -l_1_R.dl = this.oa.L(1);
        if (-l_1_R.dl == null) {
            -l_1_R.dl = "";
        }
        -l_1_R.imsi = this.oa.L(2);
        -l_1_R.dU = this.oa.L(32);
        -l_1_R.dm = this.oa.L(3);
        -l_1_R.dn = this.oa.L(4);
        -l_1_R.do = this.oa.L(5);
        -l_1_R.dp = ae(this.oa.L(6));
        -l_1_R.dq = this.oa.L(7);
        -l_1_R.L = ae(this.oa.L(8));
        -l_1_R.dr = this.oa.L(9);
        -l_1_R.ds = ae(this.oa.L(10));
        -l_1_R.dt = ae(this.oa.L(11));
        -l_1_R.du = af(this.oa.L(12));
        -l_1_R.dv = this.oa.L(13);
        -l_1_R.dw = this.oa.L(14);
        -l_1_R.dx = ae(this.oa.L(15));
        -l_1_R.dy = this.oa.L(16);
        -l_1_R.dz = (short) ((short) ae(this.oa.L(17)));
        -l_1_R.dA = ae(this.oa.L(18));
        -l_1_R.dB = this.oa.L(19);
        -l_1_R.ed = this.oa.L(36);
        -l_1_R.dC = this.oa.L(20);
        -l_1_R.dD = ae(this.oa.L(21));
        -l_1_R.dE = this.oa.L(22);
        -l_1_R.dF = ac(this.oa.L(23));
        -l_1_R.dG = ac(this.oa.L(24));
        -l_1_R.dH = ac(this.oa.L(25));
        -l_1_R.ei = ac(this.oa.L(41));
        -l_1_R.dI = this.oa.L(26);
        -l_1_R.dJ = this.oa.L(27);
        -l_1_R.dK = this.oa.L(28);
        -l_1_R.version = this.oa.L(29);
        -l_1_R.dY = ae(this.oa.L(30));
        -l_1_R.dZ = this.oa.L(31);
        -l_1_R.dN = this.oa.L(44);
        -l_1_R.dQ = this.oa.g(45, -1);
        -l_1_R.dR = this.oa.g(46, -1);
        -l_1_R.ea = this.oa.L(33);
        -l_1_R.eb = this.oa.L(34);
        -l_1_R.ec = this.oa.L(35);
        -l_1_R.ee = this.oa.L(37);
        -l_1_R.ef = this.oa.L(38);
        -l_1_R.eg = this.oa.L(39);
        -l_1_R.eh = this.oa.L(40);
        -l_1_R.dO = this.oa.L(50);
        -l_1_R.ej = this.oa.L(42);
        -l_1_R.dP = this.oa.L(47);
        -l_1_R.dL = this.oa.L(48);
        -l_1_R.dM = this.oa.L(49);
        -l_1_R.ek = this.oa.L(43);
        -l_1_R.dS = ad(this.oa.L(51));
        -l_1_R.el = ae(this.oa.L(52));
        return -l_1_R;
    }

    public boolean ax() {
        return af(this.oa.L(CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY));
    }

    public h az() {
        Object -l_3_R = kk.d(TMSDKContext.getApplicaionContext(), this.nZ.getString("key_s_c", ""));
        return TextUtils.isEmpty(-l_3_R) ? null : (h) nn.a(com.tencent.tcuser.util.a.at(-l_3_R), new h(), false);
    }

    public void e(boolean z) {
        this.oa.b(CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY, Boolean.toString(z));
    }

    public void f(long j) {
        Object -l_4_R = kk.c(TMSDKContext.getApplicaionContext(), Long.toString(j));
        if (-l_4_R != null) {
            this.nZ.putString("key_gd_ck_tm", -l_4_R);
        }
    }
}
