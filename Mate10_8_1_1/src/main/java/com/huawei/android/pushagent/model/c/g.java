package com.huawei.android.pushagent.model.c;

import com.huawei.android.pushagent.datatype.http.server.TrsReq;
import com.huawei.android.pushagent.datatype.http.server.TrsRsp;
import com.huawei.android.pushagent.model.d.c;
import com.huawei.android.pushagent.utils.a.b;

final class g implements Runnable {
    final /* synthetic */ c et;

    g(c cVar) {
        this.et = cVar;
    }

    public void run() {
        try {
            TrsRsp trsRsp = (TrsRsp) new c(this.et.appCtx, new TrsReq(this.et.appCtx, this.et.el.getBelongId(), this.et.el.getConnId())).tt();
            if (trsRsp.isValid()) {
                this.et.sx(trsRsp);
                return;
            }
            b.z("PushLog2976", "query trs error:" + this.et.el.getResult());
            if (trsRsp.isNotAllowedPush()) {
                this.et.el.nu(trsRsp.getResult());
                this.et.el.nv(trsRsp.getNextConnectTrsInterval());
            }
        } catch (Throwable e) {
            b.aa("PushLog2976", e.toString(), e);
        }
    }
}
