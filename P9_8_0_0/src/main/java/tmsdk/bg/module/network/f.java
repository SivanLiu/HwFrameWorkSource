package tmsdk.bg.module.network;

import android.content.Context;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.utils.ScriptHelper;
import tmsdkobf.ei;
import tmsdkobf.ej;
import tmsdkobf.md;
import tmsdkobf.mk;

final class f {
    private static String vq = "upload_config_des";
    private Context mContext = TMSDKContext.getApplicaionContext();
    private String vc;
    private final String vn = "MOBILE";
    private final String vo = "WIFI";
    private final String vp = "EXCLUDE";
    private final List<String> vr = new ArrayList();
    private final List<String> vs = new ArrayList();
    private final ArrayList<String> vt = new ArrayList();
    private md vu;
    private int vv = 0;
    private String vw;

    public f(String str) {
        this.vc = str;
        this.vu = new md("NetInterfaceManager");
    }

    private void a(ej ejVar) {
        if (ejVar != null && ejVar.kl != null) {
            Object -l_2_R = ejVar.kl.iterator();
            while (-l_2_R.hasNext()) {
                ei -l_3_R = (ei) -l_2_R.next();
                if ("MOBILE".equalsIgnoreCase(-l_3_R.ki)) {
                    this.vr.clear();
                    this.vr.addAll(-l_3_R.kj);
                } else {
                    if ("WIFI".equalsIgnoreCase(-l_3_R.ki)) {
                        this.vs.clear();
                        this.vs.addAll(-l_3_R.kj);
                    } else {
                        if ("EXCLUDE".equalsIgnoreCase(-l_3_R.ki)) {
                            this.vt.clear();
                            this.vt.addAll(-l_3_R.kj);
                        }
                    }
                }
            }
        }
    }

    private boolean a(List<String> list, String str) {
        for (String -l_4_R : list) {
            if (str.startsWith(-l_4_R)) {
                return true;
            }
        }
        return false;
    }

    private boolean bd(String str) {
        if (!str.startsWith("ppp")) {
            return false;
        }
        if (this.vw != null && this.vw.equals(str)) {
            return true;
        }
        this.vw = da();
        return this.vw != null && this.vw.equals(str);
    }

    private ej cY() {
        return (ej) mk.a(this.mContext, UpdateConfig.TRAFFIC_MONITOR_CONFIG_NAME, UpdateConfig.intToString(20001), new ej());
    }

    private void d(List<String> list) {
        Object -l_2_R = e(list).replaceAll("\n", ",");
        Object -l_3_R = new StringBuilder("IpAddr: ");
        -l_3_R.append(-l_2_R).append(";");
        if (this.vu != null) {
            this.vu.a(vq, -l_3_R.toString(), true);
        }
    }

    private String da() {
        Object<String> -l_2_R = db();
        if (-l_2_R.size() <= 1) {
            return null;
        }
        Object -l_3_R = new ArrayList(1);
        for (String -l_5_R : -l_2_R) {
            if (-l_5_R.startsWith("ppp")) {
                -l_3_R.add(-l_5_R);
            }
        }
        if (-l_3_R == null || -l_3_R.size() <= 0) {
            return (String) -l_2_R.get(0);
        }
        String -l_1_R = (String) -l_3_R.get(0);
        if (-l_3_R.size() <= 1) {
            return -l_1_R;
        }
        d(-l_3_R);
        return -l_1_R;
    }

    private List<String> db() {
        Object -l_1_R = new ArrayList(1);
        Object -l_2_R = ScriptHelper.runScript((int) CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY, "ip route");
        if (-l_2_R != null) {
            Object -l_4_R = Pattern.compile("dev\\s+([\\w]+)").matcher(-l_2_R);
            while (-l_4_R.find()) {
                Object -l_5_R = -l_4_R.group(1);
                if (!-l_1_R.contains(-l_5_R)) {
                    -l_1_R.add(-l_5_R);
                }
            }
        }
        return -l_1_R;
    }

    private String e(List<String> list) {
        Object -l_2_R = new StringBuilder();
        Object -l_3_R = ScriptHelper.runScript((int) CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY, "ip addr");
        if (-l_3_R != null) {
            Object -l_4_R = new StringBuilder("(");
            for (String -l_6_R : list) {
                -l_4_R.append("(?:" + -l_6_R + ")|");
            }
            -l_4_R.deleteCharAt(-l_4_R.length() - 1);
            -l_4_R.append(")");
            Object -l_6_R2 = Pattern.compile("^\\d+:\\s+" + -l_4_R.toString() + ".*$\n*" + "(^[^\\d].*$\n*)*", 8).matcher(-l_3_R);
            while (-l_6_R2.find()) {
                Object -l_7_R = -l_6_R2.group(0);
                if (-l_7_R != null) {
                    -l_2_R.append(-l_7_R);
                }
            }
        }
        return -l_2_R.toString();
    }

    public boolean bb(String str) {
        return !bd(str) && a(this.vr, str);
    }

    public boolean bc(String str) {
        return !bd(str) && a(this.vs, str);
    }

    public void cZ() {
        a(cY());
    }
}
