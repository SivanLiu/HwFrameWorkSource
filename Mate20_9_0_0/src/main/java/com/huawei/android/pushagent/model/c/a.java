package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.constant.HttpMethod;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;

public abstract class a<T> {
    private Context appCtx;

    protected abstract int yr();

    protected abstract String ys();

    protected abstract String yt();

    protected abstract T yu(String str);

    public a(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    protected Context yq() {
        return this.appCtx;
    }

    public T yp() {
        if (!g.fq(this.appCtx)) {
            return yu(null);
        }
        String yt = yt();
        c.er("PushLog3413", "http query start");
        com.huawei.android.pushagent.utils.b.c cVar = new com.huawei.android.pushagent.utils.b.c(this.appCtx, yt, null);
        cVar.am(((int) k.rh(this.appCtx).rw()) * 1000);
        cVar.an(((int) k.rh(this.appCtx).rx()) * 1000);
        yt = ys();
        Object yv = yv(cVar, yt, false, false);
        if (!TextUtils.isEmpty(yv)) {
            return yu(yv);
        }
        yv = yv(cVar, yt, true, false);
        if (!TextUtils.isEmpty(yv)) {
            return yu(yv);
        }
        yv = yv(cVar, yt, false, true);
        if (!TextUtils.isEmpty(yv)) {
            return yu(yv);
        }
        Object yv2 = yv(cVar, yt, true, true);
        if (!TextUtils.isEmpty(yv2)) {
            return yu(yv2);
        }
        c.er("PushLog3413", "query https failed");
        return yu(null);
    }

    private String yv(com.huawei.android.pushagent.utils.b.c cVar, String str, boolean z, boolean z2) {
        cVar.ao(z);
        if (z2) {
            cVar.ap(yr());
        } else {
            cVar.ap(0);
        }
        return cVar.aq(str, HttpMethod.POST);
    }

    public static String yw(String str, String str2) {
        if (TextUtils.isEmpty(str2) || TextUtils.isEmpty(str)) {
            c.eo("PushLog3413", "belongId is null or trsAddress is null");
            return str;
        }
        try {
            int parseInt = Integer.parseInt(str2.trim());
            if (parseInt <= 0) {
                c.eo("PushLog3413", "belongId is invalid:" + parseInt);
                return str;
            }
            int indexOf = str.indexOf(".");
            if (indexOf > -1) {
                return new StringBuffer().append(str.substring(0, indexOf)).append(parseInt).append(str.substring(indexOf)).toString();
            }
            return str;
        } catch (Throwable e) {
            c.es("PushLog3413", "belongId parseInt error " + str2, e);
            return str;
        } catch (Throwable e2) {
            c.es("PushLog3413", e2.getMessage(), e2);
            return str;
        }
    }
}
