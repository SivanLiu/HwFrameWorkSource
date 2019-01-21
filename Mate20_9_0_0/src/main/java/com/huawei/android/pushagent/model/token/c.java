package com.huawei.android.pushagent.model.token;

import com.huawei.android.pushagent.datatype.http.server.TokenApplyReq;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyRsp;
import com.huawei.android.pushagent.model.prefs.a;
import com.huawei.android.pushagent.model.prefs.g;
import com.huawei.android.pushagent.utils.d;
import java.util.List;

final class c implements Runnable {
    final /* synthetic */ TokenApply gb;

    c(TokenApply tokenApply) {
        this.gb = tokenApply;
    }

    public void run() {
        if (d.yp(this.gb.appCtx)) {
            String connId = a.ff(this.gb.appCtx).getConnId();
            int kq = g.kp(this.gb.appCtx).kq();
            List -wrap0 = this.gb.getTokenReqs();
            int deviceIdType = g.kp(this.gb.appCtx).getDeviceIdType();
            if (-wrap0.size() != 0) {
                TokenApplyRsp tokenApplyRsp = (TokenApplyRsp) new com.huawei.android.pushagent.model.b.a(this.gb.appCtx, new TokenApplyReq(connId, kq, -wrap0, deviceIdType)).nb();
                if (tokenApplyRsp == null) {
                    com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "fail to apply token, http level failed");
                } else {
                    this.gb.responseToken(tokenApplyRsp);
                }
            }
        }
    }
}
