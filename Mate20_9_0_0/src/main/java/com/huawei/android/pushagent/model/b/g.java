package com.huawei.android.pushagent.model.b;

import com.huawei.android.pushagent.datatype.http.server.TrsReq;
import com.huawei.android.pushagent.datatype.http.server.TrsRsp;
import com.huawei.android.pushagent.model.c.d;
import com.huawei.android.pushagent.utils.f.c;

final class g implements Runnable {
    final /* synthetic */ f hh;

    g(f fVar) {
        this.hh = fVar;
    }

    public void run() {
        try {
            TrsRsp trsRsp = (TrsRsp) new d(this.hh.appCtx, new TrsReq(this.hh.appCtx, this.hh.hg.getBelongId(), this.hh.hg.getConnId())).yp();
            if (trsRsp.isValid()) {
                this.hh.yk(trsRsp);
                return;
            }
            c.ep("PushLog3413", "query trs error:" + this.hh.hg.getResult());
            if (trsRsp.isNotAllowedPush()) {
                this.hh.hg.uj(trsRsp.getResult());
                this.hh.hg.ui(trsRsp.getNextConnectTrsInterval());
            }
        } catch (Throwable e) {
            c.es("PushLog3413", e.toString(), e);
        }
    }
}
