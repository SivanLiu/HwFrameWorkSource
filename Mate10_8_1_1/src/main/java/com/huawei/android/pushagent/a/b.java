package com.huawei.android.pushagent.a;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.c;
import com.huawei.android.pushagent.utils.f;

public class b {
    private Context appCtx;
    private c hi = new c(this.appCtx, "token_request_flag");

    public b(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public void xj() {
        xk("pclient_info_v2");
        xk("push_notify_key");
        xk("pclient_request_info");
    }

    private void xk(String str) {
        for (String xl : new c(this.appCtx, str).getAll().keySet()) {
            xl(xl);
        }
    }

    private void xl(String str) {
        if (TextUtils.isEmpty(str)) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "pkgNameWithUid is empty");
        } else {
            this.hi.as(f.gl(str), true);
        }
    }
}
