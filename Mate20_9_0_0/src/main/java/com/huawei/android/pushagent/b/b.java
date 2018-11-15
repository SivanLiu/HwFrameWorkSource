package com.huawei.android.pushagent.b;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.f.a;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;

public class b {
    private Context appCtx;
    private a bx = new a(this.appCtx, "token_request_flag");

    public b(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    /* renamed from: if */
    public void m1if() {
        ig("pclient_info_v2");
        ig("push_notify_key");
        ig("pclient_request_info");
    }

    private void ig(String str) {
        for (String ih : new a(this.appCtx, str).getAll().keySet()) {
            ih(ih);
        }
    }

    private void ih(String str) {
        if (TextUtils.isEmpty(str)) {
            c.eo("PushLog3413", "pkgNameWithUid is empty");
        } else {
            this.bx.ej(g.gv(str), true);
        }
    }
}
