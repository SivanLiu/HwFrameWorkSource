package com.huawei.android.pushagent.model.d;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.constant.HttpMethod;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.e.a;
import com.huawei.android.pushagent.utils.f;

public abstract class d<T> {
    private Context appCtx;

    protected abstract int tn();

    protected abstract String to();

    protected abstract String tp();

    protected abstract T tq(String str);

    public d(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    protected Context tr() {
        return this.appCtx;
    }

    public T tt() {
        if (!f.ge(this.appCtx)) {
            return tq(null);
        }
        String tp = tp();
        b.x("PushLog2976", "http query start");
        a aVar = new a(this.appCtx, tp, null);
        aVar.df(((int) i.mj(this.appCtx).om()) * 1000);
        aVar.dh(((int) i.mj(this.appCtx).on()) * 1000);
        tp = to();
        Object tu = tu(aVar, tp, false, false);
        if (!TextUtils.isEmpty(tu)) {
            return tq(tu);
        }
        tu = tu(aVar, tp, true, false);
        if (!TextUtils.isEmpty(tu)) {
            return tq(tu);
        }
        tu = tu(aVar, tp, false, true);
        if (!TextUtils.isEmpty(tu)) {
            return tq(tu);
        }
        Object tu2 = tu(aVar, tp, true, true);
        if (!TextUtils.isEmpty(tu2)) {
            return tq(tu2);
        }
        b.x("PushLog2976", "query https failed");
        return tq(null);
    }

    private String tu(a aVar, String str, boolean z, boolean z2) {
        aVar.di(z);
        if (z2) {
            aVar.dg(tn());
        } else {
            aVar.dg(0);
        }
        return aVar.dd(str, HttpMethod.POST);
    }

    public static String ts(String str, String str2) {
        if (TextUtils.isEmpty(str2) || TextUtils.isEmpty(str)) {
            b.ab("PushLog2976", "belongId is null or trsAddress is null");
            return str;
        }
        try {
            int parseInt = Integer.parseInt(str2.trim());
            if (parseInt <= 0) {
                b.ab("PushLog2976", "belongId is invalid:" + parseInt);
                return str;
            }
            int indexOf = str.indexOf(".");
            if (indexOf > -1) {
                return new StringBuffer().append(str.substring(0, indexOf)).append(parseInt).append(str.substring(indexOf)).toString();
            }
            return str;
        } catch (Throwable e) {
            b.aa("PushLog2976", "belongId parseInt error " + str2, e);
            return str;
        } catch (Throwable e2) {
            b.aa("PushLog2976", e2.getMessage(), e2);
            return str;
        }
    }
}
