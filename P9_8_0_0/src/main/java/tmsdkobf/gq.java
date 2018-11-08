package tmsdkobf;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

public class gq {
    private static Object lock = new Object();
    private static gq oF;
    private jx nZ = ((kf) fj.D(9)).getPreferenceService("prfle_cnfg_dao");

    private gq() {
    }

    private String U(int i) {
        return "profile_quantity_" + i;
    }

    private String V(int i) {
        return "profile_last_enqueue_key_" + i;
    }

    public static gq aZ() {
        if (oF == null) {
            synchronized (lock) {
                if (oF == null) {
                    oF = new gq();
                }
            }
        }
        return oF;
    }

    private ar aj(String str) {
        if (str == null || str.equals("")) {
            return null;
        }
        try {
            Object -l_3_R = new JceInputStream(lq.at(str));
            -l_3_R.setServerEncoding("UTF-8");
            return (ar) -l_3_R.read(new ar(), 0, false);
        } catch (Throwable th) {
            return null;
        }
    }

    private String c(ar arVar) {
        if (arVar == null) {
            return "";
        }
        Object -l_2_R = new JceOutputStream();
        -l_2_R.setServerEncoding("UTF-8");
        -l_2_R.write((JceStruct) arVar, 0);
        return lq.bytesToHexString(-l_2_R.toByteArray());
    }

    public ar R(int i) {
        Object -l_2_R = this.nZ.getString(V(i), null);
        return -l_2_R != null ? aj(-l_2_R) : null;
    }

    public int S(int i) {
        return this.nZ.getInt(U(i), 0);
    }

    public void T(int i) {
        this.nZ.putInt(U(i), 0);
    }

    public void a(ar arVar) {
        if (arVar != null) {
            this.nZ.putString(V(arVar.bK), c(arVar));
        }
    }

    public boolean b(ar arVar) {
        return gr.a(R(arVar.bK), arVar);
    }

    public int ba() {
        return this.nZ.getInt("profile_task_id", 0);
    }

    public void bb() {
        int -l_1_I = ba();
        if (-l_1_I < 0) {
            -l_1_I = 0;
        }
        this.nZ.putInt("profile_task_id", -l_1_I + 1);
    }

    public void g(boolean z) {
        this.nZ.putBoolean("profile_soft_list_upload_opened", z);
    }

    public void h(int i, int i2) {
        this.nZ.putInt(U(i), S(i) + i2);
    }

    public void i(int i, int i2) {
        this.nZ.putInt(U(i), S(i) - i2);
    }
}
