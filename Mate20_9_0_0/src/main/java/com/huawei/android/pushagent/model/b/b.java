package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.constant.HttpMethod;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.d;

public abstract class b<T> {
    private Context appCtx;

    protected abstract int mv();

    protected abstract String mw();

    protected abstract String mx();

    protected abstract T my(String str);

    public b(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    protected Context mz() {
        return this.appCtx;
    }

    public T nb() {
        if (!d.yp(this.appCtx)) {
            return my(null);
        }
        String mx = mx();
        a.st("PushLog3414", "http query start");
        com.huawei.android.pushagent.utils.c.a aVar = new com.huawei.android.pushagent.utils.c.a(this.appCtx, mx, null);
        aVar.tx(((int) com.huawei.android.pushagent.model.prefs.a.ff(this.appCtx).hr()) * 1000);
        aVar.tz(((int) com.huawei.android.pushagent.model.prefs.a.ff(this.appCtx).hs()) * 1000);
        mx = mw();
        String nc = nc(aVar, mx, false, false);
        if (!TextUtils.isEmpty(nc)) {
            return my(nc);
        }
        nc = nc(aVar, mx, true, false);
        if (!TextUtils.isEmpty(nc)) {
            return my(nc);
        }
        nc = nc(aVar, mx, false, true);
        if (!TextUtils.isEmpty(nc)) {
            return my(nc);
        }
        mx = nc(aVar, mx, true, true);
        if (!TextUtils.isEmpty(mx)) {
            return my(mx);
        }
        a.st("PushLog3414", "query https failed");
        return my(null);
    }

    private String nc(com.huawei.android.pushagent.utils.c.a aVar, String str, boolean z, boolean z2) {
        aVar.ua(z);
        if (z2) {
            aVar.ty(mv());
        } else {
            aVar.ty(0);
        }
        return aVar.tv(str, HttpMethod.POST);
    }

    public static String na(String str, String str2) {
        if (TextUtils.isEmpty(str2) || TextUtils.isEmpty(str)) {
            a.sx("PushLog3414", "belongId is null or trsAddress is null");
            return str;
        }
        try {
            int parseInt = Integer.parseInt(str2.trim());
            if (parseInt <= 0) {
                a.sx("PushLog3414", "belongId is invalid:" + parseInt);
                return str;
            }
            int indexOf = str.indexOf(".");
            if (indexOf > -1) {
                return new StringBuffer().append(str.substring(0, indexOf)).append(parseInt).append(str.substring(indexOf)).toString();
            }
            return str;
        } catch (NumberFormatException e) {
            a.sw("PushLog3414", "belongId parseInt error " + str2, e);
            return str;
        } catch (Exception e2) {
            a.sw("PushLog3414", e2.getMessage(), e2);
            return str;
        }
    }
}
