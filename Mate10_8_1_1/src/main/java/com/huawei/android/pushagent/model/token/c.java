package com.huawei.android.pushagent.model.token;

import com.huawei.android.pushagent.datatype.http.server.TokenDelReq;
import com.huawei.android.pushagent.datatype.http.server.TokenDelRsp;
import com.huawei.android.pushagent.model.c.e;
import com.huawei.android.pushagent.model.d.a;
import com.huawei.android.pushagent.model.prefs.b;
import com.huawei.android.pushagent.utils.f;
import java.util.List;

final class c implements Runnable {
    final /* synthetic */ a ey;

    c(a aVar) {
        this.ey = aVar;
    }

    public void run() {
        if (f.ge(this.ey.appCtx)) {
            e.tj(this.ey.appCtx);
            int km = b.kp(this.ey.appCtx).km();
            List tz = this.ey.tw();
            if (tz.size() != 0) {
                TokenDelRsp tokenDelRsp = (TokenDelRsp) new a(this.ey.appCtx, new TokenDelReq(km, tz, b.kp(this.ey.appCtx).getDeviceIdType())).tt();
                if (tokenDelRsp == null) {
                    com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "fail to apply token");
                } else {
                    this.ey.tx(tokenDelRsp);
                }
            }
        }
    }
}
