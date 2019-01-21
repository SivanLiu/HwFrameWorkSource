package com.huawei.android.pushagent.model.token;

import com.huawei.android.pushagent.datatype.http.server.TokenDelReq;
import com.huawei.android.pushagent.datatype.http.server.TokenDelRsp;
import com.huawei.android.pushagent.model.c.f;
import com.huawei.android.pushagent.model.prefs.g;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.d;
import java.util.List;

final class b implements Runnable {
    final /* synthetic */ a ga;

    b(a aVar) {
        this.ga = aVar;
    }

    public void run() {
        if (d.yp(this.ga.appCtx)) {
            f.qk(this.ga.appCtx);
            int kq = g.kp(this.ga.appCtx).kq();
            List rv = this.ga.rs();
            if (rv.size() != 0) {
                TokenDelRsp tokenDelRsp = (TokenDelRsp) new com.huawei.android.pushagent.model.b.d(this.ga.appCtx, new TokenDelReq(kq, rv, g.kp(this.ga.appCtx).getDeviceIdType())).nb();
                if (tokenDelRsp == null) {
                    a.su("PushLog3414", "fail to apply token");
                } else {
                    this.ga.rt(tokenDelRsp);
                }
            }
        }
    }
}
