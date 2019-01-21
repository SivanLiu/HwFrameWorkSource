package com.huawei.android.pushagent.model.c;

import com.huawei.android.pushagent.datatype.http.server.TrsReq;
import com.huawei.android.pushagent.datatype.http.server.TrsRsp;
import com.huawei.android.pushagent.model.b.c;
import com.huawei.android.pushagent.utils.b.a;

final class g implements Runnable {
    final /* synthetic */ e fy;

    g(e eVar) {
        this.fy = eVar;
    }

    public void run() {
        try {
            TrsRsp trsRsp = (TrsRsp) new c(this.fy.appCtx, new TrsReq(this.fy.appCtx, this.fy.fv.getBelongId(), this.fy.fv.getConnId())).nb();
            if (trsRsp.isValid()) {
                this.fy.qe(trsRsp);
                return;
            }
            a.sv("PushLog3414", "query trs error:" + this.fy.fv.getResult());
            if (trsRsp.isNotAllowedPush()) {
                this.fy.fv.ih(trsRsp.getResult());
                this.fy.fv.ig(trsRsp.getNextConnectTrsInterval());
            }
        } catch (Exception e) {
            a.sw("PushLog3414", e.toString(), e);
        }
    }
}
