package com.huawei.android.pushagent.model.token;

import com.huawei.android.pushagent.datatype.http.server.TokenApplyReq;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyRsp;
import com.huawei.android.pushagent.model.c.c;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.g;
import java.util.List;

final class b implements Runnable {
    final /* synthetic */ TokenApply cr;

    b(TokenApply tokenApply) {
        this.cr = tokenApply;
    }

    public void run() {
        if (g.fq(this.cr.appCtx)) {
            String connId = k.rh(this.cr.appCtx).getConnId();
            int or = com.huawei.android.pushagent.model.prefs.b.oq(this.cr.appCtx).or();
            List -wrap0 = this.cr.getTokenReqs();
            int deviceIdType = com.huawei.android.pushagent.model.prefs.b.oq(this.cr.appCtx).getDeviceIdType();
            if (-wrap0.size() != 0) {
                TokenApplyRsp tokenApplyRsp = (TokenApplyRsp) new c(this.cr.appCtx, new TokenApplyReq(connId, or, -wrap0, deviceIdType)).yp();
                if (tokenApplyRsp == null) {
                    com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "fail to apply token, http level failed");
                } else {
                    this.cr.responseToken(tokenApplyRsp);
                }
            }
        }
    }
}
