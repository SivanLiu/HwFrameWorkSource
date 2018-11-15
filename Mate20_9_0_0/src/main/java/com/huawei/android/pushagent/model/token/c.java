package com.huawei.android.pushagent.model.token;

import com.huawei.android.pushagent.datatype.http.server.TokenDelReq;
import com.huawei.android.pushagent.datatype.http.server.TokenDelRsp;
import com.huawei.android.pushagent.model.b.a;
import com.huawei.android.pushagent.model.prefs.b;
import com.huawei.android.pushagent.utils.g;
import java.util.List;

final class c implements Runnable {
    final /* synthetic */ a cs;

    c(a aVar) {
        this.cs = aVar;
    }

    public void run() {
        if (g.fq(this.cs.appCtx)) {
            a.xc(this.cs.appCtx);
            int or = b.oq(this.cs.appCtx).or();
            List kw = this.cs.kt();
            if (kw.size() != 0) {
                TokenDelRsp tokenDelRsp = (TokenDelRsp) new com.huawei.android.pushagent.model.c.b(this.cs.appCtx, new TokenDelReq(or, kw, b.oq(this.cs.appCtx).getDeviceIdType())).yp();
                if (tokenDelRsp == null) {
                    com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "fail to apply token");
                } else {
                    this.cs.ku(tokenDelRsp);
                }
            }
        }
    }
}
