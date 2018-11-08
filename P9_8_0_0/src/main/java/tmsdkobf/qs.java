package tmsdkobf;

import android.content.Context;
import android.text.TextUtils;
import com.qq.taf.jce.JceInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.utils.m;

public class qs {
    public Map<String, qv> Od;

    private static qu a(String str, Map<Integer, String> map, String str2) {
        Object -l_3_R = new qu();
        -l_3_R.MB = str;
        -l_3_R.Nt = Integer.parseInt((String) map.get(Integer.valueOf(9)));
        -l_3_R.Ot = new ArrayList();
        -l_3_R.Ot.add(str2.toLowerCase());
        -l_3_R.mDescription = !m.iW() ? (String) map.get(Integer.valueOf(8)) : (String) map.get(Integer.valueOf(18));
        if (TextUtils.isEmpty(-l_3_R.mDescription)) {
            -l_3_R.mDescription = "Data Cache";
        }
        -l_3_R.Nu = rg.dg((String) map.get(Integer.valueOf(19)));
        -l_3_R.Ne = (String) map.get(Integer.valueOf(20));
        -l_3_R.Ou = (String) map.get(Integer.valueOf(21));
        -l_3_R.Ov = (int) rg.df((String) map.get(Integer.valueOf(22)));
        return -l_3_R;
    }

    public static List<qu> b(String str, byte[] bArr) {
        Object -l_4_R = ((am) nn.a(bArr, new am(), false)).bw;
        return -l_4_R != null ? c(str, -l_4_R) : null;
    }

    private static List<qu> c(String str, byte[] bArr) {
        Object obj;
        Object -l_3_R;
        try {
            -l_3_R = new JceInputStream(TccCryptor.decrypt(bArr, null));
            -l_3_R.setServerEncoding("UTF-8");
            Object -l_4_R = new ak();
            -l_4_R.readFrom(-l_3_R);
            Object -l_2_R = new ArrayList();
            Object -l_5_R = -l_4_R.br.iterator();
            while (-l_5_R.hasNext()) {
                Map -l_6_R = (Map) -l_5_R.next();
                String -l_7_R = (String) -l_6_R.get(Integer.valueOf(3));
                Object -l_8_R;
                String -l_9_R;
                if (TextUtils.isEmpty(-l_7_R)) {
                    -l_7_R = (String) -l_6_R.get(Integer.valueOf(4));
                    if (TextUtils.isEmpty(-l_7_R)) {
                        continue;
                    } else {
                        -l_8_R = a(str, -l_6_R, -l_7_R);
                        -l_8_R.Ow = "4";
                        -l_8_R.mFileName = (String) -l_6_R.get(Integer.valueOf(11));
                        -l_8_R.Ol = (String) -l_6_R.get(Integer.valueOf(12));
                        -l_8_R.Om = (String) -l_6_R.get(Integer.valueOf(13));
                        -l_8_R.On = (String) -l_6_R.get(Integer.valueOf(14));
                        -l_8_R.Oo = (String) -l_6_R.get(Integer.valueOf(15));
                        -l_9_R = (String) -l_6_R.get(Integer.valueOf(23));
                        if (-l_9_R != null) {
                            try {
                                -l_8_R.Nw = rg.dh(-l_9_R);
                            } catch (Exception e) {
                                -l_3_R = e;
                                obj = -l_2_R;
                            }
                        }
                        if (-l_8_R.Ov > 100) {
                            -l_8_R.Ov = 0;
                        }
                        -l_2_R.add(-l_8_R);
                    }
                } else {
                    -l_8_R = a(str, -l_6_R, -l_7_R);
                    -l_8_R.Ow = "3";
                    -l_9_R = (String) -l_6_R.get(Integer.valueOf(23));
                    if (-l_9_R != null) {
                        -l_8_R.Nw = rg.dh(-l_9_R);
                    }
                    if (-l_8_R.Ov > 100) {
                        -l_8_R.Ov = 0;
                    }
                    String -l_10_R = (String) -l_6_R.get(Integer.valueOf(10));
                    if (!TextUtils.isEmpty(-l_10_R)) {
                        int -l_11_I = Integer.valueOf(-l_10_R).intValue();
                        if (-l_11_I > 0) {
                            -l_8_R.mDescription += "(" + String.format(m.cF("in_recent_days"), new Object[]{Integer.valueOf(-l_11_I)}) + ")";
                            -l_8_R.Oo = "0," + -l_11_I;
                            Object -l_12_R = a(str, -l_6_R, -l_7_R);
                            -l_12_R.Nt = 1;
                            -l_12_R.Oo = "" + -l_11_I + ",-";
                            -l_12_R.mDescription += "(" + String.format(m.cF("days_ago"), new Object[]{Integer.valueOf(-l_11_I)}) + ")";
                            -l_12_R.Ow = "3";
                            -l_12_R.Nw = -l_8_R.Nw;
                            -l_2_R.add(-l_12_R);
                        }
                    }
                    try {
                        -l_2_R.add(-l_8_R);
                    } catch (Exception e2) {
                        -l_3_R = e2;
                        obj = -l_2_R;
                    }
                }
            }
            return -l_2_R;
        } catch (Exception e3) {
            -l_3_R = e3;
            -l_3_R.printStackTrace();
            return null;
        }
    }

    private HashMap<String, String> jK() {
        ea -l_1_R = (ea) mk.b(TMSDKContext.getApplicaionContext(), UpdateConfig.intToString(40291) + ".dat", UpdateConfig.intToString(40291), new ea(), "UTF-8");
        if (-l_1_R == null || -l_1_R.iC == null) {
            return null;
        }
        Object -l_2_R = new HashMap();
        Object -l_3_R = -l_1_R.iC.iterator();
        while (-l_3_R.hasNext()) {
            dz -l_4_R = (dz) -l_3_R.next();
            if (!(-l_4_R.iu == null || -l_4_R.iv == null || -l_4_R.iw == null || !-l_4_R.iu.equals("1"))) {
                -l_2_R.put(-l_4_R.iv, -l_4_R.iw);
            }
        }
        return -l_2_R;
    }

    protected boolean T(Context context) {
        this.Od = new HashMap();
        al -l_2_R = (al) mk.a(TMSDKContext.getApplicaionContext(), UpdateConfig.DEEPCLEAN_SDCARD_SCAN_RULE_NAME_V2_SDK, UpdateConfig.intToString(40415), new al(), "UTF-8");
        if (-l_2_R == null || -l_2_R.bt == null) {
            return false;
        }
        Object -l_3_R = -l_2_R.bt.iterator();
        while (-l_3_R.hasNext()) {
            am -l_4_R = (am) -l_3_R.next();
            Object -l_5_R = new qv();
            Object -l_6_R = -l_4_R.bv.iterator();
            while (-l_6_R.hasNext()) {
                Map -l_7_R = (Map) -l_6_R.next();
                if (-l_7_R.get(Integer.valueOf(2)) != null) {
                    -l_5_R.MB = ((String) -l_7_R.get(Integer.valueOf(2))).toLowerCase();
                } else if (-l_7_R.get(Integer.valueOf(5)) != null) {
                    String -l_8_R = (String) -l_7_R.get(Integer.valueOf(6));
                    String -l_10_R = (String) -l_7_R.get(Integer.valueOf(17));
                    Object -l_11_R = new String(TccCryptor.decrypt(lq.at(((String) -l_7_R.get(Integer.valueOf(5))).toUpperCase()), null));
                    if (-l_5_R.Oz == null) {
                        -l_5_R.Oz = new HashMap();
                    }
                    if (m.iW() && !TextUtils.isEmpty(-l_10_R)) {
                        -l_5_R.Oz.put(-l_11_R, new String(TccCryptor.decrypt(lq.at(-l_10_R.toUpperCase()), null)));
                    } else {
                        -l_5_R.Oz.put(-l_11_R, new String(TccCryptor.decrypt(lq.at(-l_8_R.toUpperCase()), null)));
                    }
                }
            }
            -l_5_R.Ot = c(-l_5_R.MB, -l_4_R.bw);
            if (!(-l_5_R.Ot == null || -l_5_R.Ot.size() == 0 || -l_5_R.Oz == null || -l_5_R.Oz.size() == 0)) {
                this.Od.put(-l_5_R.MB, -l_5_R);
            }
        }
        return true;
    }

    protected boolean a(boolean z, qq qqVar) {
        if (qqVar == null) {
            return false;
        }
        ea -l_3_R = (ea) mk.b(TMSDKContext.getApplicaionContext(), UpdateConfig.intToString(40248) + ".dat", UpdateConfig.intToString(40248), new ea(), "UTF-8");
        if (-l_3_R == null || -l_3_R.iC == null) {
            return false;
        }
        HashMap -l_4_R = null;
        if (z) {
            -l_4_R = jK();
        }
        Object -l_5_R = new ArrayList();
        Object -l_6_R = new ArrayList();
        Object -l_7_R = new ArrayList();
        Object -l_8_R = -l_3_R.iC.iterator();
        while (-l_8_R.hasNext()) {
            dz -l_9_R = (dz) -l_8_R.next();
            Object -l_10_R = new qt();
            -l_10_R.Ok = -l_9_R.iv;
            if (-l_4_R != null) {
                -l_10_R.mDescription = (String) -l_4_R.get(-l_9_R.iw);
                -l_10_R.Oq = !TextUtils.isEmpty(-l_9_R.ix) ? (String) -l_4_R.get(-l_9_R.ix) : null;
            }
            if (-l_10_R.mDescription == null) {
                -l_10_R.mDescription = -l_9_R.iw;
            }
            if (-l_10_R.Oq == null) {
                -l_10_R.Oq = -l_9_R.ix;
            }
            -l_10_R.Or = !"1".equals(-l_9_R.iy);
            -l_10_R.Op = !TextUtils.isEmpty(-l_9_R.iA) ? -l_9_R.iA : "0";
            if (!TextUtils.isEmpty(-l_9_R.iz)) {
                Object -l_11_R = -l_9_R.iz.split("&");
                if (-l_11_R != null) {
                    Object -l_12_R = -l_11_R;
                    for (Object -l_15_R : -l_11_R) {
                        if (-l_15_R.length() > 2) {
                            int -l_16_I = -l_15_R.charAt(0);
                            Object -l_17_R = -l_15_R.substring(2);
                            switch (-l_16_I) {
                                case 49:
                                    -l_10_R.mFileName = -l_17_R;
                                    break;
                                case 50:
                                    -l_10_R.Ol = -l_17_R;
                                    break;
                                case 51:
                                    -l_10_R.Om = -l_17_R;
                                    break;
                                case 52:
                                    -l_10_R.On = -l_17_R;
                                    break;
                                case 53:
                                    -l_10_R.Oo = -l_17_R;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
            if ("1".equals(-l_9_R.iu)) {
                -l_5_R.add(-l_10_R);
            } else {
                if ("2".equals(-l_9_R.iu)) {
                    -l_6_R.add(-l_10_R);
                } else {
                    if ("3".equals(-l_9_R.iu)) {
                        -l_7_R.add(-l_10_R);
                    }
                }
            }
        }
        -l_8_R = new qt();
        -l_8_R.mFileName = ".apk";
        -l_8_R.Op = "0";
        -l_5_R.add(-l_8_R);
        if (-l_6_R != null && -l_6_R.size() > 0) {
            -l_8_R.Op = ((qt) -l_6_R.get(0)).Op;
        }
        qqVar.A(-l_7_R);
        qqVar.z(-l_5_R);
        qqVar.B(-l_6_R);
        qqVar.a(-l_8_R);
        return true;
    }
}
