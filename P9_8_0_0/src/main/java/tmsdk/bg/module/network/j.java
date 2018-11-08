package tmsdk.bg.module.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import tmsdk.common.tcc.TrafficSmsParser.MatchRule;
import tmsdk.common.utils.f;
import tmsdk.common.utils.q;
import tmsdkobf.bi;
import tmsdkobf.md;

final class j {
    private final String wA = "MATCH_RULE1";
    private final String wB = "#COLUMN1#";
    private final String wC = "#ITEM1#";
    md wD = new md("traffic_correction_setting");
    int wE = 0;
    private final String wx = "MATCH_RULE0";
    private final String wy = "#COLUMN0#";
    private final String wz = "#ITEM0#";

    public j(int i) {
        this.wE = i;
    }

    public void a(String str, int i, int i2, String str2, int i3) {
        if (!q.cK(str)) {
            if (this.wE != 1) {
                this.wD.a("PROFILE_IMSI0", str, false);
                this.wD.a("PROFILE_PROVINCE0", i, false);
                this.wD.a("PROFILE_CITY0", i2, false);
                this.wD.a("PROFILE_CARRY0", str2, false);
                this.wD.a("PROFILE_BRAND0", i3, false);
            } else {
                this.wD.a("PROFILE_IMSI1", str, false);
                this.wD.a("PROFILE_PROVINCE1", i, false);
                this.wD.a("PROFILE_CITY1", i2, false);
                this.wD.a("PROFILE_CARRY1", str2, false);
                this.wD.a("PROFILE_BRAND1", i3, false);
            }
        }
    }

    public void aA(int i) {
        if (this.wE != 1) {
            this.wD.a("AUTO_CORRECTION_FREQUENCY0", i, false);
        } else {
            this.wD.a("AUTO_CORRECTION_FREQUENCY1", i, false);
        }
    }

    public List<MatchRule> aB(int i) {
        Object -l_2_R = "MATCH_RULE0";
        Object -l_3_R = "#COLUMN0#";
        Object -l_4_R = "#ITEM0#";
        if (this.wE == 1) {
            -l_2_R = "MATCH_RULE1";
            -l_3_R = "#COLUMN1#";
            -l_4_R = "#ITEM1#";
        }
        Object -l_5_R = new ArrayList();
        Object -l_6_R = this.wD.getString(-l_2_R + i, "");
        if (!(-l_6_R == null || "".equals(-l_6_R))) {
            for (Object -l_10_R : -l_6_R.split(-l_4_R)) {
                Object -l_11_R = -l_10_R.split(-l_3_R);
                if (-l_11_R != null && -l_11_R.length == 4) {
                    -l_5_R.add(new MatchRule(Integer.valueOf(-l_11_R[0]).intValue(), Integer.valueOf(-l_11_R[1]).intValue(), -l_11_R[2], -l_11_R[3]));
                }
            }
        }
        return -l_5_R;
    }

    public void ay(int i) {
        if (this.wE != 1) {
            this.wD.a("SIM_CARD_CLOSINGDAY0", i, false);
        } else {
            this.wD.a("SIM_CARD_CLOSINGDAY1", i, false);
        }
    }

    public void az(int i) {
        if (this.wE != 1) {
            this.wD.a("SMS_CORRECT_TIMEOUT0", i, false);
        } else {
            this.wD.a("SMS_CORRECT_TIMEOUT1", i, false);
        }
    }

    public void bf(String str) {
        if (str != null) {
            if (this.wE != 1) {
                this.wD.a("SIM_CARD_PROVINCE0", str, false);
            } else {
                this.wD.a("SIM_CARD_PROVINCE1", str, false);
            }
        }
    }

    public void bg(String str) {
        if (str != null) {
            if (this.wE != 1) {
                this.wD.a("SIM_CARD_CITY0", str, false);
            } else {
                this.wD.a("SIM_CARD_CITY1", str, false);
            }
        }
    }

    public void bh(String str) {
        if (str != null) {
            if (this.wE != 1) {
                this.wD.a("SIM_CARD_CARRY0", str, false);
            } else {
                this.wD.a("SIM_CARD_CARRY1", str, false);
            }
        }
    }

    public void bi(String str) {
        if (str != null) {
            if (this.wE != 1) {
                this.wD.a("SIM_CARD_BRAND0", str, false);
            } else {
                this.wD.a("SIM_CARD_BRAND1", str, false);
            }
        }
    }

    public void bj(String str) {
        if (str != null) {
            if (this.wE != 1) {
                this.wD.a("SIM_CARD_SUCCESS_UPLOAD_INFO0", str, false);
            } else {
                this.wD.a("SIM_CARD_SUCCESS_UPLOAD_INFO1", str, false);
            }
        }
    }

    public void bk(String str) {
        if (str != null) {
            if (this.wE != 1) {
                this.wD.a("QUERY_CODE0", str, false);
            } else {
                this.wD.a("QUERY_CODE1", str, false);
            }
        }
    }

    public void bl(String str) {
        if (str != null) {
            if (this.wE != 1) {
                this.wD.a("QUERY_PORT0", str, false);
            } else {
                this.wD.a("QUERY_PORT1", str, false);
            }
        }
    }

    public void bm(String str) {
        if (str != null) {
            if (this.wE != 1) {
                this.wD.a("CORRECTION_TYPE0", str, false);
            } else {
                this.wD.a("CORRECTION_TYPE1", str, false);
            }
        }
    }

    public void bn(String str) {
        if (str != null) {
            if (this.wE != 1) {
                this.wD.a("LOCAL_AUTO_CORRECTION_TIME0", str, false);
            } else {
                this.wD.a("LOCAL_AUTO_CORRECTION_TIME1", str, false);
            }
        }
    }

    public void bo(String str) {
        if (str != null) {
            if (this.wE != 1) {
                this.wD.a("SERVER_AUTO_CORRECTION_TIME0", str, false);
            } else {
                this.wD.a("SERVER_AUTO_CORRECTION_TIME1", str, false);
            }
        }
    }

    public ProfileInfo d(int i, String str) {
        Object -l_3_R = new ProfileInfo();
        f.f("TrafficCorrection", "simIndex is " + i);
        f.f("TrafficCorrection", "imsi is " + str);
        Object -l_4_R = "";
        int i2;
        if (this.wE != 1) {
            -l_4_R = this.wD.getString("PROFILE_IMSI0", "");
            f.f("TrafficCorrection", "imsiCloud is " + -l_4_R);
            if (-l_4_R.compareTo(str) == 0) {
                -l_3_R.imsi = -l_4_R;
                -l_3_R.province = this.wD.getInt("PROFILE_PROVINCE0", -1);
                -l_3_R.city = this.wD.getInt("PROFILE_CITY0", -1);
                -l_3_R.carry = this.wD.getString("PROFILE_CARRY0", "");
                i2 = this.wD.getInt("PROFILE_BRAND0", -1);
                -l_3_R.brand = i2;
            }
        } else {
            -l_4_R = this.wD.getString("PROFILE_IMSI1", "");
            f.f("TrafficCorrection", "imsiCloud is " + -l_4_R);
            if (-l_4_R.compareTo(str) == 0) {
                -l_3_R.imsi = -l_4_R;
                -l_3_R.province = this.wD.getInt("PROFILE_PROVINCE1", -1);
                -l_3_R.city = this.wD.getInt("PROFILE_CITY1", -1);
                -l_3_R.carry = this.wD.getString("PROFILE_CARRY1", "");
                i2 = this.wD.getInt("PROFILE_BRAND1", -1);
                -l_3_R.brand = i2;
            }
        }
        return -l_3_R;
    }

    public String df() {
        return this.wE != 1 ? this.wD.getString("SIM_CARD_PROVINCE0", "") : this.wD.getString("SIM_CARD_PROVINCE1", "");
    }

    public String dg() {
        return this.wE != 1 ? this.wD.getString("SIM_CARD_CITY0", "") : this.wD.getString("SIM_CARD_CITY1", "");
    }

    public String dh() {
        return this.wE != 1 ? this.wD.getString("SIM_CARD_CARRY0", "") : this.wD.getString("SIM_CARD_CARRY1", "");
    }

    public String di() {
        return this.wE != 1 ? this.wD.getString("SIM_CARD_BRAND0", "") : this.wD.getString("SIM_CARD_BRAND1", "");
    }

    public int dj() {
        return this.wE != 1 ? this.wD.getInt("SIM_CARD_CLOSINGDAY0", 1) : this.wD.getInt("SIM_CARD_CLOSINGDAY1", 1);
    }

    public String dk() {
        return this.wE != 1 ? this.wD.getString("SIM_CARD_SUCCESS_UPLOAD_INFO0", "") : this.wD.getString("SIM_CARD_SUCCESS_UPLOAD_INFO1", "");
    }

    public int dl() {
        return this.wE != 1 ? this.wD.getInt("SMS_CORRECT_TIMEOUT0", 5) : this.wD.getInt("SMS_CORRECT_TIMEOUT1", 5);
    }

    public String dm() {
        return this.wE != 1 ? this.wD.getString("QUERY_CODE0", "") : this.wD.getString("QUERY_CODE1", "");
    }

    public String dn() {
        return this.wE != 1 ? this.wD.getString("QUERY_PORT0", "") : this.wD.getString("QUERY_PORT1", "");
    }

    public boolean do() {
        return this.wE != 0 ? this.wD.getBoolean("IS_PORT_FRESHED_2", false) : this.wD.getBoolean("IS_PORT_FRESHED_1", false);
    }

    public boolean dp() {
        return this.wE != 0 ? this.wD.getBoolean("IS_CODE_FRESHED_2", false) : this.wD.getBoolean("IS_CODE_FRESHED_1", false);
    }

    public String dq() {
        return this.wE != 1 ? this.wD.getString("CORRECTION_TYPE0", "") : this.wD.getString("CORRECTION_TYPE1", "");
    }

    public String dr() {
        return this.wE != 1 ? this.wD.getString("LOCAL_AUTO_CORRECTION_TIME0", "") : this.wD.getString("LOCAL_AUTO_CORRECTION_TIME1", "");
    }

    public String ds() {
        return this.wE != 1 ? this.wD.getString("SERVER_AUTO_CORRECTION_TIME0", "") : this.wD.getString("SERVER_AUTO_CORRECTION_TIME1", "");
    }

    public int dt() {
        return this.wE != 1 ? this.wD.getInt("AUTO_CORRECTION_FREQUENCY0", 1) : this.wD.getInt("AUTO_CORRECTION_FREQUENCY1", 1);
    }

    public boolean du() {
        return this.wE != 1 ? this.wD.getBoolean("STOP_STATE0", false) : this.wD.getBoolean("STOP_STATE1", false);
    }

    public void f(List<bi> list) {
        Object -l_2_R = "MATCH_RULE0";
        Object -l_3_R = "#COLUMN0#";
        Object -l_4_R = "#ITEM0#";
        if (this.wE == 1) {
            -l_2_R = "MATCH_RULE1";
            -l_3_R = "#COLUMN1#";
            -l_4_R = "#ITEM1#";
        }
        Object -l_5_R = new HashMap();
        int -l_6_I = 1;
        for (bi -l_8_R : list) {
            String -l_9_R = (String) -l_5_R.get(-l_2_R + -l_8_R.type);
            Object -l_9_R2 = -l_9_R != null ? -l_9_R + -l_4_R + -l_8_R.unit + -l_3_R + -l_8_R.type + -l_3_R + -l_8_R.prefix + -l_3_R + -l_8_R.postfix : -l_8_R.unit + -l_3_R + -l_8_R.type + -l_3_R + -l_8_R.prefix + -l_3_R + -l_8_R.postfix;
            -l_5_R.put(-l_2_R + -l_8_R.type, -l_9_R2);
            int -l_6_I2 = -l_6_I + 1;
            f.d("TrafficCorrection", "insertMatchRule--[" + -l_6_I + "][" + -l_9_R2 + "]");
            -l_6_I = -l_6_I2;
        }
        Object<Entry> -l_7_R = -l_5_R.entrySet();
        this.wD.beginTransaction();
        for (Entry -l_9_R3 : -l_7_R) {
            this.wD.a((String) -l_9_R3.getKey(), (String) -l_9_R3.getValue(), false);
        }
        this.wD.endTransaction();
    }

    public void m(boolean z) {
        if (this.wE != 0) {
            this.wD.a("IS_PORT_FRESHED_2", z, false);
        } else {
            this.wD.a("IS_PORT_FRESHED_1", z, false);
        }
    }

    public void n(boolean z) {
        if (this.wE != 0) {
            this.wD.a("IS_CODE_FRESHED_2", z, false);
        } else {
            this.wD.a("IS_CODE_FRESHED_1", z, false);
        }
    }

    public void o(boolean z) {
        if (this.wE != 1) {
            this.wD.a("STOP_STATE0", z, false);
        } else {
            this.wD.a("STOP_STATE1", z, false);
        }
    }
}
