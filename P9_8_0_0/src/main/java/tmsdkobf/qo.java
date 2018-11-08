package tmsdkobf;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.text.TextUtils;
import com.qq.taf.jce.a;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.utils.m;

public class qo {
    private static qo NY;
    private jv om = ((kf) fj.D(9)).ap("QQSecureProvider");

    private qo() {
    }

    public static qo jz() {
        if (NY == null) {
            NY = new qo();
        }
        return NY;
    }

    private String v(byte[] bArr) {
        if (bArr != null) {
            Object -l_2_R = TccCryptor.decrypt(bArr, null);
            if (-l_2_R != null) {
                return new String(-l_2_R);
            }
        }
        return null;
    }

    public boolean addUninstallPkg(String str) {
        if (str == null) {
            return false;
        }
        Object -l_2_R = new ArrayList();
        Object -l_4_R = new String[]{str};
        -l_2_R.add(ContentProviderOperation.newDelete(this.om.an("up")).withSelection("info1 = ?", -l_4_R).build());
        Object -l_6_R = new ContentValues();
        -l_6_R.put("info1", str);
        -l_6_R.put("info2", Long.valueOf(System.currentTimeMillis()));
        -l_2_R.add(ContentProviderOperation.newInsert(this.om.am("up")).withValues(-l_6_R).build());
        this.om.applyBatch(-l_2_R);
        this.om.close();
        return true;
    }

    public List<qu> cV(String str) {
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
        return bArr != null ? qs.b(str, bArr) : null;
    }

    public Map<String, qv> cW(String str) {
        Object -l_8_R;
        Object -l_14_R;
        int -l_2_I = m.iW();
        Map<String, qv> map = null;
        Object -l_7_R = this.om.al("SELECT * FROM dcp_info WHERE info2=(x'" + a.c(TccCryptor.encrypt(str.getBytes(), null)) + "')");
        if (-l_7_R != null) {
            try {
                int -l_8_I = -l_7_R.getColumnIndex("info1");
                int -l_9_I = -l_7_R.getColumnIndex("info3");
                int -l_10_I = -l_7_R.getColumnIndex("info4");
                Map<String, qv> -l_5_R = new HashMap();
                while (-l_7_R.moveToNext()) {
                    try {
                        Object -l_12_R = -l_7_R.getString(-l_8_I);
                        try {
                            Object -l_11_R = (qv) -l_5_R.get(-l_12_R);
                            if (-l_11_R == null) {
                                -l_11_R = new qv();
                                -l_11_R.MB = -l_12_R;
                                -l_5_R.put(-l_11_R.MB, -l_11_R);
                                qv -l_11_R2 = (qv) -l_5_R.get(-l_12_R);
                            }
                            if (-l_11_R.Oz == null) {
                                -l_11_R.Oz = new HashMap();
                            }
                            Object -l_13_R = -l_7_R.getBlob(-l_10_I);
                            if (-l_2_I == 0 || -l_13_R == null) {
                                -l_11_R.Oz.put(str, v(-l_7_R.getBlob(-l_9_I)));
                            } else {
                                -l_11_R.Oz.put(str, v(-l_7_R.getBlob(-l_10_I)));
                            }
                        } catch (Exception e) {
                            -l_8_R = e;
                            map = -l_5_R;
                        } catch (Throwable th) {
                            -l_14_R = th;
                            map = -l_5_R;
                        }
                    } catch (Exception e2) {
                        -l_8_R = e2;
                        map = -l_5_R;
                    } catch (Throwable th2) {
                        -l_14_R = th2;
                        map = -l_5_R;
                    }
                }
                if (-l_7_R != null) {
                    -l_7_R.close();
                }
                map = -l_5_R;
            } catch (Exception e3) {
                -l_8_R = e3;
                try {
                    -l_8_R.printStackTrace();
                    if (-l_7_R != null) {
                        -l_7_R.close();
                    }
                    this.om.close();
                    return map;
                } catch (Throwable th3) {
                    -l_14_R = th3;
                    if (-l_7_R != null) {
                        -l_7_R.close();
                    }
                    throw -l_14_R;
                }
            }
        }
        this.om.close();
        return map;
    }

    public boolean delUninstallPkg(String str) {
        if (str == null) {
            return false;
        }
        Object -l_2_R = new ArrayList();
        Object -l_4_R = new String[]{str};
        -l_2_R.add(ContentProviderOperation.newDelete(this.om.an("up")).withSelection("info1 = ?", -l_4_R).build());
        this.om.applyBatch(-l_2_R);
        this.om.close();
        return true;
    }

    public Map<String, String> j(String str, boolean z) {
        Object -l_3_R = new HashMap();
        Object -l_5_R = this.om.al("SELECT info2,info3,info4 FROM dcp_info WHERE info1='" + str + "'");
        if (-l_5_R != null) {
            int -l_6_I = -l_5_R.getColumnIndex("info2");
            int -l_7_I = -l_5_R.getColumnIndex("info3");
            int -l_8_I = -l_5_R.getColumnIndex("info4");
            while (-l_5_R.moveToNext()) {
                Object -l_9_R = -l_5_R.getBlob(-l_8_I);
                if (z && -l_9_R != null) {
                    try {
                        -l_3_R.put(v(-l_5_R.getBlob(-l_6_I)), v(-l_5_R.getBlob(-l_8_I)));
                    } catch (Object -l_6_R) {
                        -l_6_R.printStackTrace();
                        if (-l_5_R != null) {
                            -l_5_R.close();
                        }
                    } catch (Throwable th) {
                        if (-l_5_R != null) {
                            -l_5_R.close();
                        }
                    }
                } else {
                    -l_3_R.put(v(-l_5_R.getBlob(-l_6_I)), v(-l_5_R.getBlob(-l_7_I)));
                }
            }
            if (-l_5_R != null) {
                -l_5_R.close();
            }
        }
        this.om.close();
        return -l_3_R;
    }

    public HashMap<String, qv> jA() {
        Object -l_1_R = new HashMap();
        Object -l_3_R = this.om.al("SELECT info1,info2 FROM dcr_info");
        if (-l_3_R != null) {
            try {
                int -l_4_I = -l_3_R.getColumnIndex("info1");
                int -l_5_I = -l_3_R.getColumnIndex("info2");
                while (-l_3_R.moveToNext()) {
                    Object -l_6_R = -l_3_R.getString(-l_4_I);
                    Object -l_7_R = -l_3_R.getBlob(-l_5_I);
                    Object -l_8_R = new qv();
                    -l_8_R.MB = -l_6_R;
                    Object -l_9_R = qs.b(-l_6_R, -l_7_R);
                    if (-l_9_R != null) {
                        -l_8_R.C(-l_9_R);
                        -l_1_R.put(-l_6_R, -l_8_R);
                    }
                }
                if (-l_3_R != null) {
                    -l_3_R.close();
                }
            } catch (Object -l_4_R) {
                -l_4_R.printStackTrace();
                if (-l_3_R != null) {
                    -l_3_R.close();
                }
            } catch (Throwable th) {
                if (-l_3_R != null) {
                    -l_3_R.close();
                }
            }
        }
        this.om.close();
        return -l_1_R;
    }

    public Map<String, Long> jB() {
        Object -l_1_R = new HashMap();
        Object -l_3_R = this.om.al("SELECT info1,info2 FROM up");
        if (-l_3_R != null) {
            try {
                int -l_4_I = -l_3_R.getColumnIndex("info1");
                int -l_5_I = -l_3_R.getColumnIndex("info2");
                while (-l_3_R.moveToNext()) {
                    if (-l_3_R.getString(-l_4_I) != null) {
                        -l_1_R.put(-l_3_R.getString(-l_4_I), Long.valueOf(-l_3_R.getLong(-l_5_I)));
                    }
                }
                if (-l_3_R != null) {
                    -l_3_R.close();
                }
            } catch (Object -l_4_R) {
                -l_4_R.printStackTrace();
                if (-l_3_R != null) {
                    -l_3_R.close();
                }
            } catch (Throwable th) {
                if (-l_3_R != null) {
                    -l_3_R.close();
                }
            }
        }
        this.om.close();
        return -l_1_R;
    }

    public boolean jC() {
        int -l_1_I = 1;
        Object -l_3_R = this.om.al("SELECT * FROM dcp_info");
        if (-l_3_R != null && -l_3_R.getCount() < 10) {
            -l_1_I = 0;
        }
        if (-l_3_R != null) {
            -l_3_R.close();
        }
        this.om.close();
        return -l_1_I;
    }

    public List<String> y(List<am> list) {
        if (list == null) {
            return null;
        }
        Object -l_2_R = new ArrayList();
        Object -l_3_R = new ArrayList();
        for (am -l_8_R : list) {
            Object -l_13_R;
            int -l_4_I = 0;
            String -l_5_R = null;
            Object -l_6_R = new TreeMap();
            Object -l_9_R = -l_8_R.bv.iterator();
            while (-l_9_R.hasNext()) {
                Map -l_10_R = (Map) -l_9_R.next();
                if (-l_10_R.get(Integer.valueOf(2)) != null) {
                    -l_5_R = ((String) -l_10_R.get(Integer.valueOf(2))).toLowerCase();
                    -l_4_I = ((String) -l_10_R.get(Integer.valueOf(16))).equals("1");
                    if (-l_4_I != 0) {
                        break;
                    }
                } else if (-l_10_R.get(Integer.valueOf(5)) != null) {
                    String -l_11_R = (String) -l_10_R.get(Integer.valueOf(6));
                    String -l_12_R = (String) -l_10_R.get(Integer.valueOf(5));
                    -l_13_R = (String) -l_10_R.get(Integer.valueOf(17));
                    if (TextUtils.isEmpty(-l_13_R)) {
                        String -l_13_R2 = -l_11_R;
                    }
                    if (!(-l_11_R == null || -l_12_R == null)) {
                        -l_6_R.put(-l_12_R, new String[]{-l_11_R, -l_13_R});
                    }
                }
            }
            if (-l_5_R != null) {
                String -l_9_R2 = "info1 = ?";
                Object -l_10_R2 = new String[]{-l_5_R};
                -l_3_R.add(ContentProviderOperation.newDelete(this.om.an("dcr_info")).withSelection(-l_9_R2, -l_10_R2).build());
                -l_3_R.add(ContentProviderOperation.newDelete(this.om.an("dcp_info")).withSelection(-l_9_R2, -l_10_R2).build());
                if (-l_4_I == 0) {
                    -l_13_R = new ContentValues();
                    -l_13_R.put("info1", -l_5_R);
                    -l_13_R.put("info2", -l_8_R.toByteArray());
                    -l_3_R.add(ContentProviderOperation.newInsert(this.om.am("dcr_info")).withValues(-l_13_R).build());
                    for (Entry -l_16_R : -l_6_R.entrySet()) {
                        -l_13_R = new ContentValues();
                        -l_13_R.put("info2", lq.at(((String) -l_16_R.getKey()).toUpperCase()));
                        String[] -l_17_R = (String[]) -l_16_R.getValue();
                        if (-l_17_R.length > 0) {
                            -l_13_R.put("info3", lq.at(-l_17_R[0].toUpperCase()));
                        }
                        if (-l_17_R.length > 1 && !TextUtils.isEmpty(-l_17_R[1])) {
                            -l_13_R.put("info4", lq.at(-l_17_R[1].toUpperCase()));
                        }
                        -l_13_R.put("info1", -l_5_R);
                        -l_3_R.add(ContentProviderOperation.newInsert(this.om.am("dcp_info")).withValues(-l_13_R).build());
                    }
                }
                -l_2_R.add(-l_5_R);
            }
        }
        this.om.applyBatch(-l_3_R);
        this.om.close();
        return -l_2_R;
    }
}
