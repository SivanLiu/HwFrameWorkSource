package tmsdkobf;

import android.content.Context;
import android.text.TextUtils;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import tmsdk.common.TMSDKContext;
import tmsdk.common.utils.l;
import tmsdk.common.utils.u;
import tmsdkobf.on.b;

public class nj implements om {
    private static a DE = null;
    private static a DF = null;
    public static final boolean Dw = TMSDKContext.getStrFromEnvMap(TMSDKContext.PRE_USE_IP_LIST).equals("true");
    private static String Dx = "mazu.3g.qq.com";
    private static nj Dy = null;
    private boolean CX = false;
    private final Object DA = new Object();
    private String DB = "key_notset";
    private a DC;
    private a DD;
    private nl Dz;
    private Context mContext;

    public static class a {
        public long DG;
        public List<String> DH = new ArrayList();
        public boolean DI = false;
        private int DJ = 0;

        public a(long j, List<String> list, boolean z) {
            this.DG = j;
            if (list != null) {
                this.DH.addAll(list);
            }
            this.DI = z;
        }

        private static String cf(String str) {
            if (TextUtils.isEmpty(str)) {
                return null;
            }
            Object -l_1_R;
            int -l_2_I = str.lastIndexOf(":");
            if (-l_2_I < 0) {
                -l_1_R = str + ":80";
                mb.o("HIPList", "conv2HttpIPPort(): invalid ipPort(missing port): " + str);
            } else {
                -l_1_R = str.substring(0, -l_2_I) + ":80";
            }
            if (-l_1_R.length() < "http://".length() || !-l_1_R.substring(0, "http://".length()).equalsIgnoreCase("http://")) {
                -l_1_R = "http://" + -l_1_R;
            }
            return -l_1_R;
        }

        private a fL() {
            Object -l_1_R = new LinkedHashSet();
            for (String -l_3_R : this.DH) {
                Object -l_4_R = cf(-l_3_R);
                if (-l_4_R != null) {
                    -l_1_R.add(-l_4_R);
                }
            }
            return new a(this.DG, new ArrayList(-l_1_R), this.DI);
        }

        private b fM() {
            if (this.DJ >= this.DH.size()) {
                this.DJ = 0;
            }
            return nj.cc((String) this.DH.get(this.DJ));
        }

        private void fN() {
            this.DJ++;
            if (this.DJ >= this.DH.size()) {
                this.DJ = 0;
            }
        }

        private void fO() {
            this.DJ = 0;
        }

        private void s(List<String> list) {
            int -l_2_I = this.DH.size();
            if (-l_2_I < 2) {
                this.DH.addAll(nj.a(list, true));
            } else {
                this.DH.addAll(-l_2_I - 1, nj.a(list, true));
            }
        }

        public boolean isValid() {
            if (!this.DI) {
                if (!(System.currentTimeMillis() > this.DG)) {
                }
                return false;
            }
            if (this.DH.size() > 0) {
                return true;
            }
            return false;
        }

        public String toString() {
            Object -l_1_R = new StringBuilder();
            -l_1_R.append("|mValidTimeMills=").append(this.DG).append("|mIsDefault=").append(this.DI).append("|mIPPortList=").append(this.DH);
            return -l_1_R.toString();
        }
    }

    public nj(Context context, boolean z, nl nlVar, String str) {
        mb.d("HIPList", "[ip_list]HIPList() isTest: " + z);
        this.mContext = context;
        this.CX = z;
        this.Dz = nlVar;
        if (TextUtils.isEmpty(str)) {
            String str2 = !this.CX ? this.Dz.fQ() != 1 ? "mazu.3g.qq.com" : "mazu-hk.3g.qq.com" : "mazutest.3g.qq.com";
            Dx = str2;
        } else {
            Dx = str;
        }
        if (Dw) {
            fG();
        } else {
            mb.s("HIPList", "[ip_list]HIPList(), not enable, use default");
            fH();
        }
        a(this);
    }

    private void A(boolean z) {
        synchronized (this.DA) {
            Object -l_2_R = !z ? this.DD : this.DC;
        }
        if (-l_2_R == null) {
            fG();
        } else if (!-l_2_R.isValid()) {
            fH();
        }
    }

    public static String a(nl nlVar) {
        return nlVar.fQ() != 1 ? "mazuburst.3g.qq.com" : "mazuburst-hk.3g.qq.com";
    }

    public static List<String> a(List<String> list, boolean z) {
        Object -l_2_R = new ArrayList();
        if (list != null && list.size() > 0) {
            for (String -l_4_R : list) {
                if (g(-l_4_R, z)) {
                    -l_2_R.add(-l_4_R);
                } else {
                    mb.o("HIPList", "[ip_list]drop invalid ipport: " + -l_4_R);
                }
            }
        }
        return -l_2_R;
    }

    private void a(String str, a aVar, boolean z) {
        if (str == null || aVar == null || !aVar.isValid()) {
            mb.o("HIPList", "[ip_list]setWorkingHIPList(), bad arg or invalid, ignore");
            return;
        }
        Object -l_4_R = new a(aVar.DG, aVar.DH, aVar.DI);
        if (z) {
            -l_4_R.s(y(true));
            mb.n("HIPList", "[ip_list]setWorkingHIPList for " + (!this.CX ? " [release server]" : " [test server]") + ": " + -l_4_R.DH);
        }
        synchronized (this.DA) {
            this.DC = -l_4_R;
            this.DD = this.DC.fL();
            mb.n("HIPList", "[ip_list]setWorkingHIPList(), key changed: " + this.DB + " -> " + str);
            this.DB = str;
        }
    }

    public static void a(nj njVar) {
        Dy = njVar;
    }

    private String bm(int i) {
        Object -l_2_R = "" + (!this.CX ? "r_" : "t_");
        Object -l_3_R = "unknow";
        -l_3_R = i != 1 ? "apn_" + i : !u.jh() ? "wifi_nonessid" : "wifi_" + u.getSSID();
        return -l_2_R + -l_3_R;
    }

    private static b cc(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        int -l_1_I = str.lastIndexOf(":");
        if (-l_1_I <= 0 || -l_1_I == str.length() - 1) {
            return null;
        }
        Object -l_2_R = str.substring(0, -l_1_I);
        Object -l_3_R = str.substring(-l_1_I + 1);
        if (TextUtils.isDigitsOnly(-l_3_R)) {
            mb.n("HIPList", "[ip_list]getIPEndPointByStr(), ip: " + -l_2_R + " port: " + Integer.parseInt(-l_3_R));
            return new b(-l_2_R, Integer.parseInt(-l_3_R));
        }
        mb.n("HIPList", "[ip_list]getIPEndPointByStr(), invalid: " + str);
        return null;
    }

    private static boolean cd(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        try {
            if (str.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                Object -l_1_R = str.split("\\.");
                return -l_1_R.length >= 4 && Integer.parseInt(-l_1_R[0]) <= 255 && Integer.parseInt(-l_1_R[1]) <= 255 && Integer.parseInt(-l_1_R[2]) <= 255 && Integer.parseInt(-l_1_R[3]) <= 255;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private a f(String str, boolean z) {
        mb.n("HIPList", "[ip_list]loadSavedIPPortListInfo(), key: " + str);
        Object -l_4_R = this.Dz.ah(str);
        if (-l_4_R == null) {
            mb.s("HIPList", "[ip_list]loadSavedIPPortListInfo(), no saved info for: " + str);
            return null;
        } else if (-l_4_R.isValid()) {
            Object -l_3_R = -l_4_R;
            mb.n("HIPList", "[ip_list]loadSavedIPPortListInfo(), saved info for: " + str + ": " + -l_4_R.toString());
            return -l_3_R;
        } else {
            mb.s("HIPList", "[ip_list]loadSavedIPPortListInfo(), not valid");
            if (!z) {
                return null;
            }
            mb.s("HIPList", "[ip_list]loadSavedIPPortListInfo(), delete not valid info: " + str);
            this.Dz.b(str, 0, null);
            return null;
        }
    }

    public static nj fE() {
        return Dy;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void fG() {
        String -l_1_R = fI();
        synchronized (this.DA) {
            if (this.DB != null) {
                if (this.DB.equals(-l_1_R) && this.DC != null && this.DC.isValid()) {
                    mb.n("HIPList", "[ip_list]refreshWorkingIPList(), not necessary, key unchanged: " + -l_1_R);
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void fH() {
        mb.d("HIPList", "[ip_list]reset2Default()");
        synchronized (this.DA) {
            if (this.DB != null) {
                if (this.DB.equals("key_default") && this.DC != null && this.DC.isValid()) {
                    mb.n("HIPList", "[ip_list]reset2Default(), not necessary, key unchanged");
                }
            }
        }
    }

    private String fI() {
        Object -l_1_R = "" + (!this.CX ? "r_" : "t_");
        Object -l_2_R = "unknow";
        int -l_3_I = nh.w(this.mContext);
        return -l_1_R + (-l_3_I != 1 ? "apn_" + -l_3_I : "wifi_" + u.getSSID());
    }

    private int fK() {
        int -l_1_I;
        if (4 != ln.yI) {
            -l_1_I = l.S(this.mContext);
            if (-1 == -l_1_I) {
                mb.d("HIPList", "[ip_list]getOperator(), unknow as china telecom");
                -l_1_I = 2;
            }
        } else {
            mb.d("HIPList", "[ip_list]getOperator(), wifi as china telecom");
            -l_1_I = 2;
        }
        mb.d("HIPList", "[ip_list]getOperator(), 0-mobile, 1-unicom, 2-telecom: " + -l_1_I);
        return -l_1_I;
    }

    private static boolean g(String str, boolean z) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        int -l_2_I = str.lastIndexOf(":");
        if (-l_2_I <= 0 || -l_2_I == str.length() - 1) {
            return false;
        }
        Object -l_3_R = str.substring(0, -l_2_I);
        Object -l_4_R = str.substring(-l_2_I + 1);
        if (z || cd(-l_3_R)) {
            if (TextUtils.isDigitsOnly(-l_4_R)) {
                return true;
            }
        }
        return false;
    }

    private a x(boolean z) {
        if (z && DE != null) {
            return DE;
        }
        if (!z && DF != null) {
            return DF;
        }
        Object -l_2_R = y(z);
        Object -l_3_R = z(z);
        Object -l_4_R = new ArrayList();
        -l_4_R.addAll(-l_2_R);
        if (Dw) {
            -l_4_R.addAll(-l_3_R);
        }
        mb.n("HIPList", "[ip_list]getDefaultHIPListInfo for " + (!z ? "http" : "tcp") + (!this.CX ? " [release server]" : " [test server]") + ": " + -l_4_R);
        Object -l_5_R = new a(0, -l_4_R, true);
        if (z) {
            DE = -l_5_R;
        } else {
            DF = -l_5_R;
        }
        return -l_5_R;
    }

    private List<String> y(boolean z) {
        Object -l_2_R = new ArrayList();
        Object<Integer> -l_3_R = new ArrayList();
        if (z) {
            -l_3_R.add(Integer.valueOf(443));
        } else {
            -l_3_R.add(Integer.valueOf(80));
        }
        Object -l_4_R = Dx;
        for (Integer intValue : -l_3_R) {
            int -l_6_I = intValue.intValue();
            -l_2_R.add(String.format("%s:%d", new Object[]{-l_4_R, Integer.valueOf(-l_6_I)}));
        }
        return -l_2_R;
    }

    private List<String> z(boolean z) {
        Object -l_2_R = new ArrayList();
        if (this.CX) {
            return -l_2_R;
        }
        Object<Integer> -l_3_R = new ArrayList();
        if (z) {
            -l_3_R.add(Integer.valueOf(443));
        } else {
            -l_3_R.add(Integer.valueOf(80));
        }
        Object -l_4_R;
        if (this.Dz.fQ() != 1) {
            switch (fK()) {
                case 0:
                    -l_4_R = "183.232.125.162";
                    break;
                case 1:
                    -l_4_R = "163.177.71.153";
                    break;
                default:
                    -l_4_R = "120.198.203.156";
                    break;
            }
            for (Integer intValue : -l_3_R) {
                int -l_7_I = intValue.intValue();
                -l_2_R.add(String.format("%s:%d", new Object[]{-l_4_R, Integer.valueOf(-l_7_I)}));
            }
        } else {
            for (Integer intValue2 : -l_3_R) {
                int -l_5_I = intValue2.intValue();
                -l_2_R.add(String.format("%s:%d", new Object[]{"203.205.143.147", Integer.valueOf(-l_5_I)}));
                -l_2_R.add(String.format("%s:%d", new Object[]{"203.205.146.46", Integer.valueOf(-l_5_I)}));
                -l_2_R.add(String.format("%s:%d", new Object[]{"203.205.146.45", Integer.valueOf(-l_5_I)}));
            }
        }
        return -l_2_R;
    }

    public b B(boolean z) {
        A(true);
        synchronized (this.DA) {
            Object -l_2_R = !z ? this.DD : this.DC;
            if (-l_2_R == null) {
                return null;
            }
            b b = -l_2_R.fM();
            return b;
        }
    }

    public void C(boolean z) {
        A(true);
        synchronized (this.DA) {
            Object -l_2_R = !z ? this.DD : this.DC;
            if (-l_2_R != null) {
                -l_2_R.fN();
            }
        }
    }

    public void D(boolean z) {
        A(true);
        synchronized (this.DA) {
            Object -l_2_R = !z ? this.DD : this.DC;
            if (-l_2_R != null) {
                -l_2_R.fO();
            }
        }
    }

    public void E(boolean z) {
    }

    public ArrayList<String> F(boolean z) {
        A(true);
        synchronized (this.DA) {
            Object -l_2_R = !z ? this.DD : this.DC;
            if (-l_2_R == null) {
                return null;
            }
            ArrayList<String> arrayList = (ArrayList) -l_2_R.DH;
            return arrayList;
        }
    }

    public int G(boolean z) {
        Object -l_2_R = F(z);
        return -l_2_R == null ? 0 : -l_2_R.size();
    }

    public void a(long j, int i, JceStruct jceStruct) {
        mb.r("HIPList", "[ip_list]onIPListPush(), |pushId=" + j + "|seqNo=" + i);
        if (!Dw) {
            mb.s("HIPList", "[ip_list]onIPListPush(), not enable, use default");
        } else if (jceStruct == null) {
            mb.o("HIPList", "[ip_list]onIPListPush(), bad arg: jceStruct == null");
        } else if (jceStruct instanceof g) {
            g -l_5_R = (g) jceStruct;
            a -l_8_R = new a(System.currentTimeMillis() + (((long) -l_5_R.q) * 1000), a(-l_5_R.p, false), false);
            if (-l_8_R.isValid()) {
                int -l_9_I = nh.w(this.mContext);
                int -l_10_I = -l_5_R.s;
                if (-l_10_I != -l_9_I) {
                    mb.o("HIPList", "[ip_list]onIPListPush(), apn not match， just save, curApn: " + -l_9_I + " pushedApn: " + -l_10_I);
                    this.Dz.b(bm(-l_10_I), -l_8_R.DG, -l_8_R.DH);
                } else {
                    String -l_11_R = fI();
                    this.Dz.b(-l_11_R, -l_8_R.DG, -l_8_R.DH);
                    a(-l_11_R, -l_8_R, true);
                    mb.n("HIPList", "[ip_list]onIPListPush(), saved, key: " + -l_11_R);
                }
            } else {
                mb.s("HIPList", "[ip_list]onIPListPush(), not valid");
            }
        } else {
            mb.o("HIPList", "[ip_list]onIPListPush(), bad type, should be SCHIPList: " + jceStruct.getClass());
        }
    }

    public boolean ax() {
        return this.CX;
    }

    public void fF() {
        if (Dw) {
            mb.d("HIPList", "[ip_list]handleNetworkChange(), refreshWorkingHIPList, isTest: " + this.CX);
            fG();
        }
    }

    public String fJ() {
        String -l_1_R = null;
        Object -l_2_R = B(false);
        if (-l_2_R != null) {
            -l_1_R = -l_2_R.hd();
            if (-l_1_R != null) {
                if (-l_1_R.length() < "http://".length() || !-l_1_R.substring(0, "http://".length()).equalsIgnoreCase("http://")) {
                    -l_1_R = "http://" + -l_1_R;
                }
            }
            mb.n("HIPList", "[ip_list]getHttpIp(), httpIp: " + -l_1_R);
        }
        if (-l_1_R != null) {
            return -l_1_R;
        }
        -l_1_R = "http://" + Dx;
        mb.s("HIPList", "[ip_list]getHttpIp(), use default: " + -l_1_R);
        return -l_1_R;
    }
}
