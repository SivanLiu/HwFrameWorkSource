package com.huawei.android.pushagent.model.token;

import com.huawei.android.pushagent.datatype.http.server.TokenApplyReq;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyRsp;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.f;
import java.util.List;

final class b implements Runnable {
    final /* synthetic */ TokenApply ex;

    b(TokenApply tokenApply) {
        this.ex = tokenApply;
    }

    public void run() {
        if (f.ge(this.ex.appCtx)) {
            String connId = i.mj(this.ex.appCtx).getConnId();
            int km = com.huawei.android.pushagent.model.prefs.b.kp(this.ex.appCtx).km();
            List -wrap0 = this.ex.getTokenReqs();
            int deviceIdType = com.huawei.android.pushagent.model.prefs.b.kp(this.ex.appCtx).getDeviceIdType();
            if (-wrap0.size() != 0) {
                TokenApplyRsp tokenApplyRsp = (TokenApplyRsp) new com.huawei.android.pushagent.model.d.b(this.ex.appCtx, new TokenApplyReq(connId, km, -wrap0, deviceIdType)).tt();
                if (tokenApplyRsp == null) {
                    com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "fail to apply token, http level failed");
                } else {
                    this.ex.responseToken(tokenApplyRsp);
                }
            }
        }
    }
}
