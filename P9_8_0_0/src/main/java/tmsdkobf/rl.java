package tmsdkobf;

import android.os.Environment;
import android.text.TextUtils;
import com.qq.taf.jce.JceInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.lang.MultiLangManager;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.tcc.DeepCleanEngine;
import tmsdk.common.tcc.QFile;
import tmsdk.common.tcc.SdcardScannerFactory;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.utils.f;
import tmsdk.common.utils.m;

public class rl {
    static DeepCleanEngine PB = SdcardScannerFactory.getDeepCleanEngine(null);
    public static final List<String> Ps = rk.jZ();
    Map<String, ov> OK;
    private b PA = null;
    private String PC = null;
    private TreeMap<Integer, List<a>> PD = null;
    private HashMap<String, b> PE = new HashMap();
    private boolean Pt = false;
    public long Pu = 0;
    public long Pv = 0;
    public long Pw = 0;
    public long Px = 0;
    public long Py = 0;
    public long Pz = 0;

    static class a {
        public String Ow = "";
        public String Ox = "";
        public String PF = "";
        public String PG = "";
        public String PH = "";
        public String PI = "";
        public String PJ = "";
        public String PK = "";
        private String PL = null;
        public String[] PM = null;
        public String mDesc = "";
        public String mFileName = "";
        public String mPath = "";

        a() {
        }
    }

    static class b {
        public String MB;
        private String PL = null;
        public List<a> PN;
        public HashMap<String, List<a>> PO = new HashMap();
        public TreeMap<String, List<a>> PP = new TreeMap();
        public TreeMap<String, List<a>> PQ = new TreeMap();
        boolean PR = false;
        boolean PS = false;
        public String mAppName;
        public String mPkg;

        b() {
        }

        public void kl() {
            if (!this.PR && this != null && this.PN != null) {
                for (a -l_2_R : this.PN) {
                    Object -l_3_R;
                    if (!(this.PS || -l_2_R.Ow == null || !-l_2_R.Ow.equals("4"))) {
                        this.PS = true;
                    }
                    if (-l_2_R.mPath.contains("*")) {
                        -l_3_R = this.PP;
                        -l_2_R.PM = -l_2_R.mPath.split("/");
                    } else {
                        -l_3_R = !-l_2_R.mPath.contains("//") ? this.PO : this.PQ;
                    }
                    if (-l_3_R != null) {
                        Object -l_4_R = (List) -l_3_R.get(-l_2_R.mPath);
                        if (-l_4_R == null) {
                            -l_4_R = new ArrayList();
                        }
                        -l_4_R.add(-l_2_R);
                        -l_3_R.put(-l_2_R.mPath, -l_4_R);
                    }
                }
                this.PR = true;
            }
        }
    }

    private void Y(boolean z) {
        al -l_2_R = (al) mk.b(TMSDKContext.getApplicaionContext(), UpdateConfig.intToString(40415) + ".dat", UpdateConfig.intToString(40415), new al(), "UTF-8");
        if (-l_2_R != null && -l_2_R.bt != null) {
            Object -l_3_R = -l_2_R.bt.iterator();
            while (-l_3_R.hasNext()) {
                Object -l_10_R;
                am -l_4_R = (am) -l_3_R.next();
                Object -l_5_R = new qv();
                Object -l_6_R = -l_4_R.bv.iterator();
                while (-l_6_R.hasNext()) {
                    Map -l_7_R = (Map) -l_6_R.next();
                    if (-l_7_R.get(Integer.valueOf(2)) != null) {
                        -l_5_R.MB = ((String) -l_7_R.get(Integer.valueOf(2))).toLowerCase();
                    } else if (-l_7_R.get(Integer.valueOf(5)) != null) {
                        String -l_8_R = (String) -l_7_R.get(Integer.valueOf(6));
                        -l_10_R = new String(TccCryptor.decrypt(lq.at(((String) -l_7_R.get(Integer.valueOf(5))).toUpperCase()), null));
                        String -l_11_R = (String) -l_7_R.get(Integer.valueOf(17));
                        if (-l_5_R.Oz == null) {
                            -l_5_R.Oz = new HashMap();
                        }
                        if (z && !TextUtils.isEmpty(-l_11_R)) {
                            -l_5_R.Oz.put(-l_10_R, new String(TccCryptor.decrypt(lq.at(-l_11_R.toUpperCase()), null)));
                        } else {
                            -l_5_R.Oz.put(-l_10_R, new String(TccCryptor.decrypt(lq.at(-l_8_R.toUpperCase()), null)));
                        }
                        if (-l_5_R.Oz == null) {
                            -l_5_R.Oz = new HashMap();
                        }
                        if (TextUtils.isEmpty(-l_8_R)) {
                            -l_5_R.Oz.put(-l_10_R, "NoAppName");
                        } else {
                            -l_5_R.Oz.put(-l_10_R, new String(TccCryptor.decrypt(lq.at(-l_8_R.toUpperCase()), null)));
                        }
                    }
                }
                -l_5_R.Ot = a(-l_5_R.MB, -l_4_R.bw, z);
                if (!(-l_5_R.Ot == null || -l_5_R.Ot.size() == 0 || -l_5_R.Oz == null || -l_5_R.Oz.size() == 0)) {
                    Object -l_7_R2 = (b) this.PE.get(-l_5_R.MB);
                    if (-l_7_R2 == null) {
                        -l_7_R2 = new b();
                        -l_7_R2.MB = -l_5_R.MB;
                        -l_7_R2.PN = new ArrayList();
                        this.PE.put(-l_5_R.MB, -l_7_R2);
                    }
                    for (Entry -l_9_R : -l_5_R.Oz.entrySet()) {
                        ov -l_10_R2 = (ov) this.OK.get(-l_9_R.getKey());
                        if (-l_10_R2 != null) {
                            -l_7_R2.mPkg = -l_10_R2.getPackageName();
                            -l_7_R2.mAppName = -l_10_R2.getAppName();
                            break;
                        }
                    }
                    if (-l_7_R2.mPkg == null) {
                        Entry -l_8_R2 = (Entry) -l_5_R.Oz.entrySet().iterator().next();
                        -l_7_R2.mPkg = (String) -l_8_R2.getKey();
                        -l_7_R2.mAppName = (String) -l_8_R2.getValue();
                    }
                    for (qu -l_9_R2 : -l_5_R.Ot) {
                        -l_10_R = new a();
                        -l_10_R.mPath = (String) -l_9_R2.Ot.get(0);
                        -l_10_R.PF = -l_9_R2.MB;
                        -l_10_R.Ow = -l_9_R2.Ow;
                        -l_10_R.mDesc = -l_9_R2.mDescription;
                        -l_10_R.PG = Integer.toString(-l_9_R2.Nt);
                        if (!TextUtils.isEmpty(-l_10_R.Ow)) {
                            -l_7_R2.PN.add(-l_10_R);
                        }
                    }
                }
            }
        }
    }

    private int a(a aVar) {
        if (aVar == null) {
            return 0;
        }
        int -l_2_I = 0;
        if (!aVar.mFileName.equals("")) {
            -l_2_I = 1;
        }
        if (!aVar.PH.equals("")) {
            -l_2_I++;
        }
        if (!aVar.PI.equals("")) {
            -l_2_I++;
        }
        if (!aVar.PJ.equals("")) {
            -l_2_I++;
        }
        if (!aVar.PK.equals("")) {
            -l_2_I++;
        }
        return -l_2_I;
    }

    public static List<qu> a(String str, byte[] bArr, boolean z) {
        Object obj;
        Object -l_4_R;
        try {
            -l_4_R = new JceInputStream(TccCryptor.decrypt(bArr, null));
            -l_4_R.setServerEncoding("UTF-8");
            Object -l_5_R = new ak();
            -l_5_R.readFrom(-l_4_R);
            Object -l_3_R = new ArrayList();
            Object -l_6_R = -l_5_R.br.iterator();
            while (-l_6_R.hasNext()) {
                Map -l_7_R = (Map) -l_6_R.next();
                String -l_8_R = (String) -l_7_R.get(Integer.valueOf(3));
                Object -l_9_R;
                String -l_10_R;
                if (TextUtils.isEmpty(-l_8_R)) {
                    -l_8_R = (String) -l_7_R.get(Integer.valueOf(4));
                    if (TextUtils.isEmpty(-l_8_R)) {
                        continue;
                    } else {
                        -l_9_R = a(str, z, -l_7_R, -l_8_R);
                        -l_9_R.Ow = "4";
                        -l_9_R.mFileName = (String) -l_7_R.get(Integer.valueOf(11));
                        -l_9_R.Ol = (String) -l_7_R.get(Integer.valueOf(12));
                        -l_9_R.Om = (String) -l_7_R.get(Integer.valueOf(13));
                        -l_9_R.On = (String) -l_7_R.get(Integer.valueOf(14));
                        -l_9_R.Oo = (String) -l_7_R.get(Integer.valueOf(15));
                        -l_10_R = (String) -l_7_R.get(Integer.valueOf(23));
                        if (-l_10_R != null) {
                            -l_9_R.Nw = rg.dh(-l_10_R);
                        }
                        if (-l_9_R.Ov > 100) {
                            -l_9_R.Ov = 0;
                        }
                        -l_3_R.add(-l_9_R);
                    }
                } else {
                    -l_9_R = a(str, z, -l_7_R, -l_8_R);
                    -l_9_R.Ow = "3";
                    -l_10_R = (String) -l_7_R.get(Integer.valueOf(23));
                    if (-l_10_R != null) {
                        try {
                            -l_9_R.Nw = rg.dh(-l_10_R);
                        } catch (Exception e) {
                            -l_4_R = e;
                            obj = -l_3_R;
                        }
                    }
                    if (-l_9_R.Ov > 100) {
                        -l_9_R.Ov = 0;
                    }
                    String -l_11_R = (String) -l_7_R.get(Integer.valueOf(10));
                    if (!TextUtils.isEmpty(-l_11_R)) {
                        int -l_12_I = Integer.valueOf(-l_11_R).intValue();
                        if (-l_12_I > 0) {
                            -l_9_R.Ox = -l_11_R;
                            -l_9_R.mDescription += "(" + String.format(m.cF("in_recent_days"), new Object[]{Integer.valueOf(-l_12_I)}) + ")";
                            -l_9_R.Oo = "0," + -l_12_I;
                            Object -l_13_R = a(str, z, -l_7_R, -l_8_R);
                            -l_13_R.Ow = "3";
                            -l_13_R.Nt = 1;
                            -l_13_R.Oo = "" + -l_12_I + ",-";
                            -l_13_R.Ox = -l_11_R;
                            -l_13_R.mDescription += "(" + String.format(m.cF("days_ago"), new Object[]{Integer.valueOf(-l_12_I)}) + ")";
                            -l_13_R.Nw = -l_9_R.Nw;
                            -l_3_R.add(-l_13_R);
                        }
                    }
                    try {
                        -l_3_R.add(-l_9_R);
                    } catch (Exception e2) {
                        -l_4_R = e2;
                        obj = -l_3_R;
                    }
                }
            }
            return -l_3_R;
        } catch (Exception e3) {
            -l_4_R = e3;
            -l_4_R.printStackTrace();
            return null;
        }
    }

    private static qu a(String str, boolean z, Map<Integer, String> map, String str2) {
        Object -l_4_R = new qu();
        -l_4_R.MB = str;
        -l_4_R.Nt = Integer.parseInt((String) map.get(Integer.valueOf(9)));
        -l_4_R.Ot = new ArrayList();
        -l_4_R.Ot.add(str2.toLowerCase());
        -l_4_R.mDescription = !z ? (String) map.get(Integer.valueOf(8)) : (String) map.get(Integer.valueOf(18));
        if (TextUtils.isEmpty(-l_4_R.mDescription)) {
            -l_4_R.mDescription = "Data Cache";
        }
        -l_4_R.Nu = rg.dg((String) map.get(Integer.valueOf(19)));
        -l_4_R.Ne = (String) map.get(Integer.valueOf(20));
        -l_4_R.Ou = (String) map.get(Integer.valueOf(21));
        -l_4_R.Ov = (int) rg.df((String) map.get(Integer.valueOf(22)));
        return -l_4_R;
    }

    private a a(b bVar, String str, String[] -l_9_R, QFile qFile) {
        if (bVar == null || bVar.PN == null || bVar.PN.size() == 0 || -l_9_R == null) {
            return null;
        }
        TreeMap -l_5_R = null;
        int -l_6_I = 0;
        String -l_7_R = "";
        if (-l_9_R.length > 2) {
            int -l_8_I = 0;
            for (Object -l_12_R : -l_9_R) {
                -l_8_I++;
                if (-l_8_I == -l_9_R.length) {
                    break;
                }
                if (!(-l_12_R == null || -l_12_R.equals(""))) {
                    -l_7_R = -l_7_R + "/" + -l_12_R;
                }
            }
        }
        if (this.PC == null) {
            this.PC = -l_7_R;
        } else {
            if (-l_7_R.equals(this.PC)) {
                -l_5_R = this.PD;
                -l_6_I = 1;
            } else {
                this.PC = -l_7_R;
                this.PD = null;
            }
        }
        if (this.PC == null) {
            return null;
        }
        int -l_9_I;
        if (-l_5_R == null && -l_6_I == 0) {
            -l_5_R = new TreeMap();
            String -l_8_R = "";
            -l_9_I = 0;
            Object -l_10_R = -l_9_R;
            for (Object -l_13_R : -l_9_R) {
                -l_9_I++;
                if (-l_9_I == -l_9_R.length) {
                    break;
                }
                if (!(-l_13_R == null || -l_13_R.equals(""))) {
                    -l_8_R = -l_8_R + "/" + -l_13_R;
                    List -l_14_R = (List) bVar.PO.get(-l_8_R);
                    if (-l_14_R == null) {
                        continue;
                    } else if (bVar.PS) {
                        -l_15_R = (List) -l_5_R.get(Integer.valueOf(-l_9_I));
                        if (-l_15_R == null) {
                            -l_15_R = new ArrayList();
                            -l_5_R.put(Integer.valueOf(-l_9_I), -l_15_R);
                        }
                        -l_15_R.addAll(-l_14_R);
                    } else {
                        -l_15_R = new ArrayList();
                        -l_15_R.add(-l_14_R.get(0));
                        -l_5_R.put(Integer.valueOf(-l_9_I), -l_15_R);
                        this.PD = -l_5_R;
                        return (a) -l_14_R.get(0);
                    }
                }
            }
            long -l_10_J = System.currentTimeMillis();
            for (Entry -l_13_R2 : bVar.PQ.entrySet()) {
                if (PB != null) {
                    if (PB.isMatchPath(this.PC, (String) -l_13_R2.getKey())) {
                        Object -l_14_R2 = (List) -l_5_R.get(Integer.valueOf(-l_9_R.length - 1));
                        if (-l_14_R2 == null) {
                            -l_14_R2 = new ArrayList();
                            -l_5_R.put(Integer.valueOf(-l_9_R.length - 1), -l_14_R2);
                        }
                        -l_14_R2.addAll((Collection) -l_13_R2.getValue());
                    }
                }
            }
            this.Pu += System.currentTimeMillis() - -l_10_J;
            long -l_14_J = System.currentTimeMillis();
            for (Entry -l_17_R : bVar.PP.entrySet()) {
                Object -l_18_R = ((a) ((List) -l_17_R.getValue()).get(0)).PM;
                if (a(-l_18_R, -l_9_R)) {
                    List -l_19_R;
                    if (bVar.PS) {
                        -l_19_R = (List) -l_5_R.get(Integer.valueOf(-l_18_R.length));
                        if (-l_19_R == null) {
                            -l_19_R = new ArrayList();
                            -l_5_R.put(Integer.valueOf(-l_18_R.length), -l_19_R);
                        }
                        -l_19_R.addAll((Collection) -l_17_R.getValue());
                    } else {
                        -l_19_R = new ArrayList();
                        -l_19_R.add(((List) -l_17_R.getValue()).get(0));
                        -l_5_R.put(Integer.valueOf(-l_18_R.length), -l_19_R);
                        this.PD = -l_5_R;
                        return (a) ((List) -l_17_R.getValue()).get(0);
                    }
                }
            }
            this.Pw += System.currentTimeMillis() - -l_14_J;
        }
        if (-l_5_R == null || -l_5_R.size() == 0) {
            return null;
        }
        if (-l_6_I == 0) {
            this.PD = -l_5_R;
        }
        QFile qFile2 = qFile;
        for (-l_9_I = ((Integer) -l_5_R.lastKey()).intValue(); -l_9_I > 0; -l_9_I--) {
            List<a> -l_10_R2 = (List) -l_5_R.get(Integer.valueOf(-l_9_I));
            if (-l_10_R2 != null) {
                a -l_11_R = null;
                for (a -l_13_R3 : -l_10_R2) {
                    if (-l_13_R3.Ow.equals("4")) {
                        if (qFile2 == null) {
                            qFile2 = new QFile(Environment.getExternalStorageDirectory().getAbsolutePath() + str);
                            qFile2.fillExtraInfo();
                        }
                        if (PB != null) {
                            if (-l_13_R3.mFileName.equals("") || PB.isMatchFile(-l_9_R[-l_9_R.length - 1], -l_13_R3.mFileName)) {
                                if (!-l_13_R3.PH.equals("")) {
                                    if (!PB.isMatchFileSize(qFile2.size, -l_13_R3.PH)) {
                                    }
                                }
                                if (!-l_13_R3.PI.equals("")) {
                                    if (!PB.isMatchTime(qFile2.createTime, -l_13_R3.PI)) {
                                    }
                                }
                                if (!-l_13_R3.PJ.equals("")) {
                                    if (!PB.isMatchTime(qFile2.modifyTime, -l_13_R3.PJ)) {
                                    }
                                }
                                if (!-l_13_R3.PK.equals("")) {
                                    if (PB.isMatchTime(qFile2.accessTime, -l_13_R3.PK)) {
                                    }
                                }
                            }
                        }
                    }
                    if (-l_11_R != null) {
                        if (-l_11_R.Ow.equals("3")) {
                            if (!-l_13_R3.Ow.equals("4")) {
                            }
                        }
                        if (!-l_13_R3.Ow.equals("3")) {
                            if (a(-l_11_R) >= a(-l_13_R3)) {
                            }
                        }
                    }
                    -l_11_R = -l_13_R3;
                }
                if (-l_11_R != null) {
                    return -l_11_R;
                }
            }
        }
        return null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean a(String[] strArr, String[] strArr2) {
        if (strArr == null || strArr2 == null || strArr.length >= strArr2.length) {
            return false;
        }
        int -l_3_I = 0;
        while (-l_3_I < strArr.length) {
            if (!strArr[-l_3_I].equals("*") && !strArr[-l_3_I].equals(strArr2[-l_3_I])) {
                return false;
            }
            -l_3_I++;
        }
        return true;
    }

    private b b(String str, String[] strArr) {
        if (str == null || strArr == null) {
            return null;
        }
        Object -l_3_R;
        if (this.PA == null || this.PA.MB == null || !str.startsWith(this.PA.MB)) {
            long -l_4_J = System.currentTimeMillis();
            -l_3_R = c(str, strArr);
            this.Pz += System.currentTimeMillis() - -l_4_J;
            this.PA = -l_3_R;
        } else {
            -l_3_R = this.PA;
        }
        return -l_3_R;
    }

    private b c(String str, String[] -l_4_R) {
        if (str == null || -l_4_R == null) {
            return null;
        }
        Object -l_3_R = "";
        for (Object -l_7_R : -l_4_R) {
            if (!(-l_7_R == null || -l_7_R.equals(""))) {
                -l_3_R = -l_3_R + "/" + -l_7_R;
                b -l_8_R = (b) this.PE.get(-l_3_R);
                if (-l_8_R != null) {
                    return -l_8_R;
                }
            }
        }
        return null;
    }

    private void kk() {
        Object -l_1_R = rm.km().kn();
        if (-l_1_R != null) {
            Object -l_2_R = -l_1_R.iterator();
            while (-l_2_R.hasNext()) {
                String -l_3_R = (String) -l_2_R.next();
                Object -l_4_R = new b();
                -l_4_R.MB = -l_3_R;
                this.PE.put(-l_3_R, -l_4_R);
            }
        }
    }

    private String m(String str, boolean z) {
        Object -l_3_R = o(str, z);
        return -l_3_R != null ? -l_3_R[1] : null;
    }

    private String n(String str, boolean z) {
        Object -l_3_R = o(str, z);
        return -l_3_R != null ? -l_3_R[0] : null;
    }

    private String[] o(String str, boolean z) {
        return p(str, z);
    }

    private String[] p(String str, boolean z) {
        Object -l_3_R = rm.km().j(str, z);
        if (-l_3_R == null || -l_3_R.size() == 0) {
            return null;
        }
        String -l_4_R = null;
        String -l_5_R = null;
        Object -l_6_R = new ArrayList();
        Object -l_7_R = new ArrayList();
        for (String -l_9_R : -l_3_R.keySet()) {
            try {
                -l_6_R.add(-l_9_R);
                -l_7_R.add(-l_3_R.get(-l_9_R));
            } catch (Exception e) {
            }
        }
        Object -l_8_R = new rj();
        -l_8_R.init();
        String -l_9_R2 = -l_8_R.J(-l_6_R);
        if (-l_9_R2 != null) {
            Object -l_10_R = TMServiceFactory.getSystemInfoService().a(-l_9_R2, 2048);
            if (-l_10_R != null) {
                -l_5_R = -l_9_R2;
                -l_4_R = -l_10_R.getAppName();
                if (-l_4_R == null) {
                    -l_4_R = -l_8_R.cS(-l_9_R2);
                }
            }
        } else {
            int -l_10_I = -l_8_R.K(-l_6_R);
            if (-l_10_I == -1) {
                -l_10_I = 0;
            }
            -l_5_R = (String) -l_6_R.get(-l_10_I);
            -l_4_R = (String) -l_7_R.get(-l_10_I);
        }
        if (-l_4_R == null || -l_5_R == null) {
            return new String[]{null, null};
        }
        f.e("xx", -l_5_R + "  " + -l_4_R);
        return new String[]{-l_5_R.trim(), -l_4_R.trim()};
    }

    private List<a> q(String str, boolean z) {
        if (str == null) {
            return null;
        }
        byte[] -l_3_R = null;
        try {
            Object -l_5_R;
            Object -l_6_R;
            byte[] -l_4_R = rm.km().dm(str);
            if (-l_4_R != null) {
                -l_5_R = new JceInputStream(-l_4_R);
                -l_5_R.setServerEncoding("UTF-8");
                -l_6_R = new am();
                -l_6_R.readFrom(-l_5_R);
                -l_3_R = -l_6_R.bw;
            }
            if (-l_3_R != null) {
                -l_5_R = new JceInputStream(TccCryptor.decrypt(-l_3_R, null));
                -l_5_R.setServerEncoding("UTF-8");
                -l_6_R = new ak();
                -l_6_R.readFrom(-l_5_R);
                Object -l_7_R = new ArrayList();
                Object -l_8_R = -l_6_R.br.iterator();
                while (-l_8_R.hasNext()) {
                    Map -l_9_R = (Map) -l_8_R.next();
                    Object -l_10_R;
                    if (-l_9_R.get(Integer.valueOf(3)) != null) {
                        -l_10_R = new a();
                        -l_10_R.Ow = "3";
                        -l_10_R.PG = (String) -l_9_R.get(Integer.valueOf(9));
                        -l_10_R.mPath = ((String) -l_9_R.get(Integer.valueOf(3))).toLowerCase();
                        -l_10_R.PF = str;
                        if (-l_10_R.mPath.equals("/")) {
                            -l_10_R.mPath = str;
                        } else {
                            -l_10_R.mPath = str + -l_10_R.mPath.toLowerCase();
                        }
                        -l_10_R.mDesc = !z ? (String) -l_9_R.get(Integer.valueOf(8)) : (String) -l_9_R.get(Integer.valueOf(18));
                        if (-l_10_R.mDesc == null) {
                            -l_10_R.mDesc = (String) -l_9_R.get(Integer.valueOf(8));
                        }
                        -l_10_R.Ox = (String) -l_9_R.get(Integer.valueOf(10));
                        -l_7_R.add(-l_10_R);
                    } else if (-l_9_R.get(Integer.valueOf(4)) != null) {
                        -l_10_R = new a();
                        -l_10_R.Ow = "4";
                        -l_10_R.PG = (String) -l_9_R.get(Integer.valueOf(9));
                        -l_10_R.mPath = ((String) -l_9_R.get(Integer.valueOf(4))).toLowerCase();
                        -l_10_R.PF = str;
                        if (-l_10_R.mPath.equals("/")) {
                            -l_10_R.mPath = str;
                        } else {
                            -l_10_R.mPath = str + -l_10_R.mPath.toLowerCase();
                        }
                        -l_10_R.mDesc = !z ? (String) -l_9_R.get(Integer.valueOf(8)) : (String) -l_9_R.get(Integer.valueOf(18));
                        if (-l_10_R.mDesc == null) {
                            -l_10_R.mDesc = (String) -l_9_R.get(Integer.valueOf(8));
                        }
                        -l_10_R.mFileName = (String) -l_9_R.get(Integer.valueOf(11));
                        -l_10_R.PH = (String) -l_9_R.get(Integer.valueOf(12));
                        -l_10_R.PI = (String) -l_9_R.get(Integer.valueOf(13));
                        -l_10_R.PJ = (String) -l_9_R.get(Integer.valueOf(14));
                        -l_10_R.PK = (String) -l_9_R.get(Integer.valueOf(15));
                        -l_7_R.add(-l_10_R);
                    }
                }
                return -l_7_R;
            }
            f.e("xx", "null:" + str);
            return null;
        } catch (Object -l_3_R2) {
            f.e("xx", -l_3_R2.getMessage());
            return null;
        }
    }

    public String a(String str, QFile qFile, boolean z) {
        if (str == null) {
            return null;
        }
        String[] -l_4_R = str.split("/");
        b -l_5_R = b(str, -l_4_R);
        if (-l_5_R == null) {
            return null;
        }
        if (this.Pt) {
            if (-l_5_R.PN == null || -l_5_R.PN.size() == 0) {
                -l_5_R.PN = q(-l_5_R.MB, z);
            }
            if (-l_5_R.mAppName == null) {
                -l_5_R.mAppName = m(-l_5_R.MB, z);
            }
            if (-l_5_R.mPkg == null) {
                -l_5_R.mPkg = n(-l_5_R.MB, z);
            }
        }
        if (-l_5_R.PN == null || -l_5_R.PN.size() == 0) {
            return -l_5_R.mAppName;
        }
        -l_5_R.kl();
        long -l_6_J = System.currentTimeMillis();
        Object -l_8_R = a(-l_5_R, str, -l_4_R, qFile);
        this.Py += System.currentTimeMillis() - -l_6_J;
        return -l_8_R != null ? -l_5_R.mAppName + " " + -l_8_R.mDesc : -l_5_R.mAppName;
    }

    protected Map<String, ov> jY() {
        Object -l_3_R = TMServiceFactory.getSystemInfoService().f(73, 2);
        int -l_4_I = -l_3_R.size();
        Object -l_5_R = new HashMap();
        for (int -l_6_I = 0; -l_6_I < -l_4_I; -l_6_I++) {
            -l_5_R.put(((ov) -l_3_R.get(-l_6_I)).getPackageName(), -l_3_R.get(-l_6_I));
        }
        return -l_5_R;
    }

    public String k(String str, boolean z) {
        if (str == null) {
            return null;
        }
        Object -l_3_R = b(str, str.split("/"));
        if (-l_3_R == null) {
            return null;
        }
        if (-l_3_R.mPkg == null) {
            Object -l_4_R = o(-l_3_R.MB, z);
            if (-l_4_R != null) {
                -l_3_R.mPkg = -l_4_R[0];
                -l_3_R.mAppName = -l_4_R[1];
            }
        }
        return -l_3_R.mPkg;
    }

    public void kj() {
        int -l_2_I = ((MultiLangManager) ManagerCreatorC.getManager(MultiLangManager.class)).isENG();
        m.T(-l_2_I);
        Object -l_3_R = new qn();
        this.OK = jY();
        this.Pt = -l_3_R.jv();
        if (this.Pt) {
            kk();
        } else {
            Y(-l_2_I);
        }
    }

    public String l(String str, boolean z) {
        if (str == null) {
            return null;
        }
        Object -l_3_R = b(str, str.split("/"));
        if (-l_3_R == null) {
            return null;
        }
        if (-l_3_R.mAppName == null) {
            Object -l_4_R = o(-l_3_R.MB, z);
            if (-l_4_R != null) {
                -l_3_R.mPkg = -l_4_R[0];
                -l_3_R.mAppName = -l_4_R[1];
            }
        }
        return -l_3_R.mAppName;
    }
}
