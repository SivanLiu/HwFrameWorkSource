package tmsdk.common.module.qscanner.impl;

import android.text.TextUtils;
import com.tencent.tcuser.util.a;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tmsdk.common.utils.f;
import tmsdkobf.de;
import tmsdkobf.di;
import tmsdkobf.dq;

public class c {
    public static ArrayList<b> a(ArrayList<Integer> arrayList, Map<Integer, de> map) {
        if (arrayList == null || map == null) {
            return null;
        }
        Object -l_2_R = new ArrayList();
        Object -l_4_R = arrayList.iterator();
        while (-l_4_R.hasNext()) {
            Integer -l_5_R = (Integer) -l_4_R.next();
            de -l_3_R = (de) map.get(-l_5_R);
            if (-l_3_R == null) {
                f.g("QScanInternalUtils", "[virus_scan]cannot find AD plugin info for id: " + -l_5_R);
            } else {
                Object -l_6_R = new b();
                -l_6_R.id = -l_5_R.intValue();
                -l_6_R.type = -l_3_R.gq;
                -l_6_R.BR = (((long) -l_3_R.gs) << 32) | ((long) -l_3_R.gr);
                -l_6_R.banUrls = -l_3_R.gt;
                -l_6_R.banIps = -l_3_R.gu;
                -l_6_R.name = -l_3_R.gv;
                -l_2_R.add(-l_6_R);
            }
        }
        return -l_2_R;
    }

    public static di a(e eVar, int i) {
        if (eVar == null) {
            return null;
        }
        Object -l_6_R;
        Object -l_2_R = new di();
        -l_2_R.gI = i;
        -l_2_R.gJ = null;
        -l_2_R.gK = eVar.packageName;
        -l_2_R.gL = a.at(eVar.bZ);
        Object -l_3_R = new ArrayList();
        Object -l_4_R = ca(eVar.Cb);
        int -l_5_I = 0;
        if (-l_4_R.size() > 0) {
            -l_6_R = -l_4_R.iterator();
            while (-l_6_R.hasNext()) {
                ArrayList -l_7_R = (ArrayList) -l_6_R.next();
                Object -l_8_R = new ArrayList(-l_7_R.size());
                Object -l_9_R = -l_7_R.iterator();
                while (-l_9_R.hasNext()) {
                    -l_8_R.add(a.at((String) -l_9_R.next()));
                    -l_5_I++;
                }
                -l_3_R.add(-l_8_R);
            }
        }
        if (-l_5_I > 1) {
            -l_2_R.ha = -l_3_R;
        }
        -l_2_R.gM = (long) eVar.size;
        -l_2_R.gN = eVar.softName;
        -l_2_R.gO = eVar.versionCode;
        -l_2_R.gP = eVar.version;
        -l_2_R.gQ = 0;
        if (eVar.BQ == 1) {
            -l_2_R.gQ |= 1;
        }
        if (eVar.BQ == 1 || eVar.BQ == 0) {
            -l_2_R.gQ |= 2;
            if (eVar.Cn) {
                -l_2_R.gQ |= 4;
            }
        }
        -l_2_R.gR = eVar.cc;
        -l_2_R.gS = eVar.gS;
        -l_2_R.gn = eVar.type;
        -l_2_R.gT = eVar.BU;
        -l_2_R.gU = eVar.category;
        if (eVar.plugins == null || eVar.plugins.size() == 0) {
            -l_2_R.gV = null;
        } else {
            -l_2_R.gV = new ArrayList(eVar.plugins.size());
            -l_6_R = eVar.plugins.iterator();
            while (-l_6_R.hasNext()) {
                -l_2_R.gV.add(Integer.valueOf(((b) -l_6_R.next()).id));
            }
        }
        -l_2_R.gW = 0;
        if (eVar.Cg) {
            -l_2_R.gW |= 1;
        }
        if (eVar.Ch) {
            -l_2_R.gW |= 2;
        }
        -l_2_R.gX = eVar.Cm;
        -l_2_R.gY = eVar.dp;
        -l_2_R.official = eVar.official;
        return -l_2_R;
    }

    public static void a(d dVar, e eVar) {
        if (eVar != null && dVar != null) {
            eVar.packageName = dVar.BS.nf;
            eVar.softName = dVar.BS.softName;
            eVar.version = dVar.BS.version;
            eVar.versionCode = dVar.BS.versionCode;
            eVar.path = dVar.BS.path;
            eVar.BQ = dVar.BS.BQ;
            eVar.size = dVar.BS.size;
            eVar.type = dVar.type;
            eVar.lL = dVar.lL;
            eVar.BU = dVar.BU;
            eVar.name = dVar.name;
            eVar.label = dVar.label;
            eVar.BT = dVar.BT;
            eVar.url = dVar.url;
            eVar.gS = dVar.lP;
            eVar.dp = dVar.dp;
            eVar.cc = dVar.BW;
            eVar.plugins = o(dVar.plugins);
            eVar.name = dVar.name;
            eVar.category = dVar.category;
        }
    }

    public static ArrayList<ArrayList<String>> ca(String str) {
        Object -l_2_R;
        Object -l_1_R = new ArrayList();
        if (TextUtils.isEmpty(str)) {
            return -l_1_R;
        }
        try {
            -l_2_R = str.split(";");
            if (-l_2_R != null && -l_2_R.length > 0) {
                Object -l_3_R = -l_2_R;
                for (Object -l_6_R : -l_2_R) {
                    if (!TextUtils.isEmpty(-l_6_R)) {
                        Object -l_7_R = -l_6_R.split(",");
                        if (-l_7_R != null && -l_7_R.length > 0) {
                            Object -l_8_R = new ArrayList();
                            Object -l_9_R = -l_7_R;
                            for (Object -l_12_R : -l_7_R) {
                                if (!TextUtils.isEmpty(-l_12_R)) {
                                    -l_8_R.add(-l_12_R);
                                }
                            }
                            if (-l_8_R.size() > 0) {
                                -l_1_R.add(-l_8_R);
                            }
                        }
                    }
                }
            }
        } catch (Object -l_2_R2) {
            f.b("QScanInternalUtils", "str2aalStr: " + -l_2_R2, -l_2_R2);
        }
        return -l_1_R;
    }

    private static ArrayList<b> o(List<dq> list) {
        if (list == null) {
            return null;
        }
        Object -l_1_R = new ArrayList(list.size() + 1);
        for (dq -l_3_R : list) {
            Object -l_4_R = new b();
            -l_4_R.id = -l_3_R.id;
            -l_4_R.type = -l_3_R.type;
            -l_4_R.BR = (((long) -l_3_R.hN) << 32) | ((long) -l_3_R.hM);
            -l_4_R.banUrls = -l_3_R.banUrls;
            -l_4_R.banIps = -l_3_R.banIps;
            -l_4_R.name = -l_3_R.name;
            -l_1_R.add(-l_4_R);
        }
        return -l_1_R;
    }

    public static String q(ArrayList<ArrayList<String>> arrayList) {
        Object -l_1_R = new StringBuilder();
        if (arrayList != null) {
            for (int -l_2_I = 0; -l_2_I < arrayList.size(); -l_2_I++) {
                if (-l_2_I > 0) {
                    -l_1_R.append(";");
                }
                ArrayList -l_3_R = (ArrayList) arrayList.get(-l_2_I);
                for (int -l_4_I = 0; -l_4_I < -l_3_R.size(); -l_4_I++) {
                    if (-l_4_I > 0) {
                        -l_1_R.append(",");
                    }
                    -l_1_R.append((String) -l_3_R.get(-l_4_I));
                }
            }
        }
        return -l_1_R.toString();
    }
}
