package com.huawei.android.pushagent.a;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.b;
import com.huawei.android.pushagent.utils.d;

public class a {
    private Context appCtx;
    /* renamed from: if */
    private b f0if = new b(this.appCtx, "token_request_flag");

    public a(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public void aaa() {
        aab("pclient_info_v2");
        aab("push_notify_key");
        aab("pclient_request_info");
    }

    private void aab(String str) {
        for (String aac : new b(this.appCtx, str).getAll().keySet()) {
            aac(aac);
        }
    }

    private void aac(String str) {
        if (TextUtils.isEmpty(str)) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "pkgNameWithUid is empty");
        } else {
            this.f0if.tl(d.yx(str), true);
        }
    }
}
