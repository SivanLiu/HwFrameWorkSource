package tmsdkobf;

import java.util.ArrayList;
import java.util.Map;

public class rm {
    private static rm PT;
    private jv om = ((kf) fj.D(9)).ap("QQSecureProvider");

    private rm() {
    }

    public static rm km() {
        if (PT == null) {
            PT = new rm();
        }
        return PT;
    }

    public byte[] dm(String str) {
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
        return bArr;
    }

    public Map<String, String> j(String str, boolean z) {
        return qo.jz().j(str, z);
    }

    public ArrayList<String> kn() {
        Object -l_1_R = new ArrayList();
        Object -l_3_R = this.om.al("SELECT info1 FROM dcr_info");
        if (-l_3_R != null) {
            try {
                int -l_4_I = -l_3_R.getColumnIndex("info1");
                while (-l_3_R.moveToNext()) {
                    -l_1_R.add(-l_3_R.getString(-l_4_I));
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
}
