package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.model.prefs.b;
import com.huawei.android.pushagent.model.prefs.c;
import com.huawei.android.pushagent.model.prefs.g;
import com.huawei.android.pushagent.model.prefs.j;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.d;
import java.io.UnsupportedEncodingException;
import java.util.Set;

public class f {
    private static final byte[] fw = new byte[0];
    private static f fx = null;

    public static void qk(Context context) {
        Set<String> ip = b.il(context).ip();
        if (ip.size() > 0) {
            for (String str : ip) {
                if (!d.yn(context, d.yw(str), d.zh(str))) {
                    com.huawei.android.pushagent.model.prefs.d.ja(context).ji(b.il(context).ik(str), str);
                    b.il(context).ir(str);
                }
            }
        }
    }

    public static boolean qj(Context context, String str, String str2) {
        if (TextUtils.isEmpty(b.il(context).ik(d.ys(str, str2)))) {
            return false;
        }
        return true;
    }

    public static boolean ql(Context context, String str, String str2) {
        int kq = g.kp(context).kq();
        String ys = d.ys(str, str2);
        if (1 == kq) {
            if (c.iu(context).ix(ys) || (TextUtils.isEmpty(b.il(context).ik(ys)) ^ 1) != 0) {
                return true;
            }
        } else if (!TextUtils.isEmpty(b.il(context).in(ys))) {
            return true;
        }
        return false;
    }

    public static boolean qm(Context context, String str, String str2, byte[] bArr) {
        int kq = g.kp(context).kq();
        String ys = d.ys(str, str2);
        if (1 == kq) {
            return true;
        }
        if (bArr != null) {
            try {
                if (new String(bArr, "UTF-8").equals(b.il(context).in(ys))) {
                    return true;
                }
            } catch (UnsupportedEncodingException e) {
                a.su("PushLog3414", "server token parse string failed");
            }
        }
        return false;
    }

    public static boolean qn(Context context) {
        if (b.il(context).io() || (com.huawei.android.pushagent.model.prefs.d.ja(context).jg() ^ 1) == 0 || (j.lm(context).lq() ^ 1) == 0 || (c.iu(context).iy() ^ 1) == 0) {
            return true;
        }
        return false;
    }
}
