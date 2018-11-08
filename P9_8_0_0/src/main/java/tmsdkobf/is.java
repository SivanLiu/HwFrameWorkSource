package tmsdkobf;

import android.text.TextUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;
import tmsdk.common.utils.f;

public class is {
    private il rX = new il(IncomingSmsFilterConsts.PAY_SMS);
    private Map<String, String> rY = new HashMap(16);
    private Map<String, String> rZ = new HashMap(16);
    private Map<String, String> sa = new HashMap(16);
    private boolean sb;

    private static boolean aI(String str) {
        return !TextUtils.isEmpty(str) && str.indexOf(42) >= 0;
    }

    private static boolean aJ(String str) {
        return str.length() > 0 && str.indexOf(42) == str.length() - 1;
    }

    private static String aK(String str) {
        return str.replace("\\", "\\\\").replace(".", "\\.").replace("+", "\\+").replace("*", ".*");
    }

    public void clear() {
        this.rX.clear();
        this.rY.clear();
        this.rZ.clear();
        this.sa.clear();
    }

    public String getName(String str) {
        String -l_3_R;
        Object -l_2_R = qe.cz(str);
        if (qe.cy(-l_2_R)) {
            try {
                -l_3_R = (String) this.rX.get(Integer.parseInt(-l_2_R));
            } catch (Object -l_4_R) {
                Object -l_4_R2;
                f.b("ContactsMap", "minMatch to int", -l_4_R2);
                -l_3_R = (String) this.rY.get(-l_2_R);
            }
        } else {
            Object -l_3_R2 = (String) this.rY.get(-l_2_R);
        }
        if (-l_3_R2 != null) {
            return -l_3_R2;
        }
        CharSequence cx = qe.cx(str);
        -l_4_R2 = qe.cv(cx);
        for (Entry -l_6_R : this.rZ.entrySet()) {
            String -l_7_R = (String) -l_6_R.getKey();
            if (qe.cw(-l_7_R)) {
                if (cx.startsWith(-l_7_R)) {
                    return (String) -l_6_R.getValue();
                }
            } else if (-l_4_R2.startsWith(-l_7_R)) {
                return (String) -l_6_R.getValue();
            }
        }
        for (Entry -l_6_R2 : this.sa.entrySet()) {
            Object -l_7_R2 = Pattern.compile((String) -l_6_R2.getKey());
            if (-l_7_R2.matcher(cx).matches() || -l_7_R2.matcher(-l_4_R2).matches()) {
                return (String) -l_6_R2.getValue();
            }
        }
        return null;
    }

    public void i(String str, String str2) {
        if (this.sb && aI(str)) {
            j(str, str2);
        } else if (!TextUtils.isEmpty(str)) {
            if (str2 == null) {
                str2 = "";
            }
            Object -l_3_R = qe.cz(str);
            if (qe.cy(-l_3_R)) {
                try {
                    this.rX.put(Integer.parseInt(-l_3_R), str2);
                } catch (Object -l_4_R) {
                    f.e("ContactsMap", "Exception in parseInt(minMatch): " + -l_4_R.getMessage());
                    this.rY.put(-l_3_R, str2);
                }
            } else {
                this.rY.put(-l_3_R, str2);
            }
        }
    }

    public void j(String str, String str2) {
        if (!TextUtils.isEmpty(str)) {
            if (str2 == null) {
                str2 = "";
            }
            if (aJ(str)) {
                this.rZ.put(str.substring(0, str.length() - 1), str2);
            } else {
                this.sa.put(aK(str), str2);
            }
        }
    }
}
