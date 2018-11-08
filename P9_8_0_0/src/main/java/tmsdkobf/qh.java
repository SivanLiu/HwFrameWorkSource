package tmsdkobf;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;
import com.qq.taf.jce.JceInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.utils.m;

@SuppressLint({"DefaultLocale"})
public class qh {
    boolean MA;
    jv om;

    static class a {
        public String MB;
        public String mAppName;

        a(String str, String str2) {
            this.MB = str;
            this.mAppName = str2;
        }
    }

    public qh(boolean z) {
        this.om = null;
        this.MA = false;
        this.om = ((kf) fj.D(9)).ap("QQSecureProvider");
        this.MA = z;
    }

    private static void a(String str, qj qjVar) {
        int -l_2_I = str.indexOf(44);
        qjVar.Nh = Long.valueOf(str.substring(0, -l_2_I)).longValue() << 10;
        -l_2_I++;
        if (str.charAt(-l_2_I) != '-') {
            qjVar.Ni = Long.valueOf(str.substring(-l_2_I)).longValue() << 10;
        } else {
            qjVar.Ni = Long.MAX_VALUE;
        }
    }

    private static void b(String str, qj qjVar) {
        long -l_2_J = System.currentTimeMillis();
        int -l_4_I = str.indexOf(44);
        long -l_5_J = Long.valueOf(str.substring(0, -l_4_I)).longValue();
        -l_4_I++;
        qjVar.Nk = -l_2_J - TimeUnit.DAYS.toMillis(-l_5_J);
        if (str.charAt(-l_4_I) != '-') {
            qjVar.Nj = -l_2_J - TimeUnit.DAYS.toMillis(Long.valueOf(str.substring(-l_4_I)).longValue());
        } else {
            qjVar.Nj = 0;
        }
    }

    private byte[] cR(String str) {
        byte[] bArr = null;
        Object -l_4_R = this.om.al("SELECT info2 FROM dcr_info WHERE info1='" + str + "'");
        if (-l_4_R != null) {
            try {
                int -l_5_I = -l_4_R.getColumnIndex("info2");
                while (-l_4_R.moveToNext()) {
                    bArr = -l_4_R.getBlob(-l_5_I);
                }
                if (-l_4_R != null) {
                    -l_4_R.close();
                }
            } catch (Object -l_5_R) {
                -l_5_R.printStackTrace();
                if (-l_4_R != null) {
                    -l_4_R.close();
                }
            } catch (Throwable th) {
                if (-l_4_R != null) {
                    -l_4_R.close();
                }
            }
        }
        this.om.close();
        return bArr != null ? bArr : null;
    }

    private static String v(byte[] bArr) {
        if (bArr != null) {
            Object -l_1_R = TccCryptor.decrypt(bArr, null);
            if (-l_1_R != null) {
                return new String(-l_1_R);
            }
        }
        return null;
    }

    private byte[] w(byte[] bArr) {
        Object -l_3_R = ((am) nn.a(bArr, new am(), false)).bw;
        return -l_3_R != null ? -l_3_R : null;
    }

    void U(boolean z) {
        this.MA = z;
    }

    public ql a(String str, ql -l_3_R) {
        Object -l_4_R = cR(str);
        if (-l_4_R == null) {
            return null;
        }
        Object -l_5_R = w(-l_4_R);
        if (-l_5_R == null) {
            return null;
        }
        JceInputStream jceInputStream = new JceInputStream(TccCryptor.decrypt(-l_5_R, null));
        jceInputStream.setServerEncoding("UTF-8");
        ak -l_7_R = new ak();
        -l_7_R.readFrom(jceInputStream);
        Object -l_8_R = -l_7_R.br.iterator();
        while (-l_8_R.hasNext()) {
            try {
                Map -l_9_R = (Map) -l_8_R.next();
                int -l_10_I = Integer.parseInt((String) -l_9_R.get(Integer.valueOf(9)));
                int -l_11_I = rg.dg((String) -l_9_R.get(Integer.valueOf(19)));
                String -l_12_R = (String) -l_9_R.get(Integer.valueOf(20));
                String -l_13_R = (String) -l_9_R.get(Integer.valueOf(3));
                String -l_14_R = (String) -l_9_R.get(Integer.valueOf(23));
                String -l_15_R;
                Object -l_15_R2;
                String -l_17_R;
                if (TextUtils.isEmpty(-l_13_R)) {
                    -l_13_R = (String) -l_9_R.get(Integer.valueOf(4));
                    if (!TextUtils.isEmpty(-l_13_R)) {
                        if (this.MA) {
                            -l_15_R = (String) -l_9_R.get(Integer.valueOf(18));
                        } else {
                            -l_15_R2 = (String) -l_9_R.get(Integer.valueOf(8));
                        }
                        qj -l_16_R = new qj(-l_15_R2, -l_13_R);
                        -l_16_R.Nl = 4;
                        -l_16_R.Ng = (String) -l_9_R.get(Integer.valueOf(11));
                        -l_16_R.Nt = -l_10_I;
                        -l_16_R.Nu = -l_11_I;
                        -l_16_R.Ne = -l_12_R;
                        -l_16_R.Nv = -l_14_R;
                        if (!TextUtils.isEmpty(-l_16_R.Nv)) {
                            -l_16_R.Nw = rg.dh(-l_14_R);
                        }
                        if (!(-l_16_R.Ng == null || -l_16_R.Ng.isEmpty())) {
                            -l_16_R.Nf++;
                            -l_16_R.Ng = -l_16_R.Ng.toLowerCase();
                        }
                        -l_17_R = (String) -l_9_R.get(Integer.valueOf(12));
                        if (!(-l_17_R == null || -l_17_R.isEmpty())) {
                            a(-l_17_R, -l_16_R);
                            -l_16_R.Nf++;
                        }
                        String -l_18_R = (String) -l_9_R.get(Integer.valueOf(13));
                        String -l_19_R = (String) -l_9_R.get(Integer.valueOf(14));
                        String -l_20_R = (String) -l_9_R.get(Integer.valueOf(15));
                        if (-l_19_R != null && !-l_19_R.isEmpty()) {
                            b(-l_19_R, -l_16_R);
                            -l_16_R.Nf++;
                        } else if (-l_20_R != null && !-l_20_R.isEmpty()) {
                            b(-l_20_R, -l_16_R);
                            -l_16_R.Nf++;
                        } else if (!(-l_18_R == null || -l_18_R.isEmpty())) {
                            b(-l_18_R, -l_16_R);
                            -l_16_R.Nf++;
                        }
                        -l_3_R.b(-l_16_R);
                    }
                } else {
                    if (this.MA) {
                        -l_15_R = (String) -l_9_R.get(Integer.valueOf(18));
                    } else {
                        -l_15_R2 = (String) -l_9_R.get(Integer.valueOf(8));
                    }
                    Object -l_16_R2 = new qj(-l_15_R2, -l_13_R);
                    -l_16_R2.Nt = -l_10_I;
                    -l_16_R2.Nl = 3;
                    -l_16_R2.Nu = -l_11_I;
                    -l_16_R2.Ne = -l_12_R;
                    -l_16_R2.Nv = -l_14_R;
                    if (!TextUtils.isEmpty(-l_16_R2.Nv)) {
                        -l_16_R2.Nw = rg.dh(-l_14_R);
                    }
                    -l_17_R = (String) -l_9_R.get(Integer.valueOf(10));
                    if (!TextUtils.isEmpty(-l_17_R)) {
                        int -l_18_I = Integer.valueOf(-l_17_R).intValue();
                        if (-l_18_I > 0) {
                            -l_16_R2.Np = -l_16_R2.mDescription + "(" + String.format(m.cF("days_ago"), new Object[]{Integer.valueOf(-l_18_I)}) + ")";
                            -l_16_R2.mDescription += "(" + String.format(m.cF("in_recent_days"), new Object[]{Integer.valueOf(-l_18_I)}) + ")";
                            -l_16_R2.Ns = (long) -l_18_I;
                            -l_16_R2.Nf++;
                        }
                    }
                    -l_3_R.b(-l_16_R2);
                }
            } catch (Object -l_6_R) {
                -l_6_R.printStackTrace();
                return null;
            }
        }
        if (!(-l_3_R.jr() || -l_3_R.js())) {
            qj qjVar = new qj(m.cF("deep_clean_other_rubbish"), "/");
            qjVar.Nt = 1;
            -l_3_R.b(qjVar);
        }
        -l_3_R.jt();
        return -l_3_R;
    }

    public List<a> cQ(String str) {
        Object -l_7_R;
        Object -l_12_R;
        List<a> list = null;
        Object -l_5_R = "SELECT * FROM dcp_info WHERE info2=(x'" + com.qq.taf.jce.a.c(TccCryptor.encrypt(str.getBytes(), null)) + "')";
        Log.d("fgtDatabaseParse", getClass().getSimpleName() + " getRootPaths pkg:" + str);
        Object -l_6_R = this.om.al(-l_5_R);
        if (-l_6_R != null) {
            try {
                int -l_7_I = -l_6_R.getColumnIndex("info1");
                int -l_8_I = -l_6_R.getColumnIndex("info3");
                int -l_9_I = -l_6_R.getColumnIndex("info4");
                List<a> -l_4_R = new ArrayList();
                while (-l_6_R.moveToNext()) {
                    try {
                        Object -l_10_R = -l_6_R.getString(-l_7_I);
                        Object -l_11_R = !this.MA ? v(-l_6_R.getBlob(-l_8_I)) : v(-l_6_R.getBlob(-l_9_I));
                        if (-l_10_R != null) {
                            Log.d("fgtDatabaseParse", "add root path:" + -l_10_R);
                            try {
                                -l_4_R.add(new a(-l_10_R, -l_11_R));
                            } catch (Exception e) {
                                -l_7_R = e;
                                list = -l_4_R;
                            } catch (Throwable th) {
                                -l_12_R = th;
                                list = -l_4_R;
                            }
                        }
                    } catch (Exception e2) {
                        -l_7_R = e2;
                        list = -l_4_R;
                    } catch (Throwable th2) {
                        -l_12_R = th2;
                        list = -l_4_R;
                    }
                }
                if (-l_6_R != null) {
                    -l_6_R.close();
                }
                list = -l_4_R;
            } catch (Exception e3) {
                -l_7_R = e3;
                try {
                    -l_7_R.printStackTrace();
                    if (-l_6_R != null) {
                        -l_6_R.close();
                    }
                    this.om.close();
                    return list;
                } catch (Throwable th3) {
                    -l_12_R = th3;
                    if (-l_6_R != null) {
                        -l_6_R.close();
                    }
                    throw -l_12_R;
                }
            }
        }
        this.om.close();
        return list;
    }
}
