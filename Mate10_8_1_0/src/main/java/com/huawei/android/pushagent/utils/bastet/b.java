package com.huawei.android.pushagent.utils.bastet;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.huawei.android.pushagent.b.a;

final class b extends Handler {
    final /* synthetic */ a ae;

    b(a aVar, Looper looper) {
        this.ae = aVar;
        super(looper);
    }

    public void handleMessage(Message message) {
        try {
            super.handleMessage(message);
            int i = message.what;
            switch (i) {
                case 2:
                    a.aaj(71, String.valueOf(2));
                    com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "receive handler message BASTET_CONNECTION_CLOSED");
                    this.ae.cq();
                    return;
                case 4:
                    this.ae.ce(message);
                    return;
                case 5:
                    a.aaj(71, String.valueOf(5));
                    com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "receive handler message BASTET_HB_NOT_AVAILABLE");
                    this.ae.cq();
                    return;
                case 7:
                    a.aaj(71, String.valueOf(7));
                    com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "receive handler message BASTET_RECONNECTION_BEST_POINT");
                    this.ae.cq();
                    return;
                case 8:
                    a.aaj(71, String.valueOf(8));
                    com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "receive handler message BASTET_RECONNECTION_BREAK");
                    this.ae.cq();
                    return;
                default:
                    com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "receive handler message default, what is " + i);
                    return;
            }
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "handle bastetMessage error:" + e.getMessage(), e);
            this.ae.cr();
        }
        com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "handle bastetMessage error:" + e.getMessage(), e);
        this.ae.cr();
    }
}
