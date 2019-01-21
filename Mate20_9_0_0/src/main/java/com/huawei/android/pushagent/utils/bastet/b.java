package com.huawei.android.pushagent.utils.bastet;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.huawei.android.pushagent.b.a;

final class b extends Handler {
    final /* synthetic */ a ia;

    b(a aVar, Looper looper) {
        this.ia = aVar;
        super(looper);
    }

    public void handleMessage(Message message) {
        try {
            super.handleMessage(message);
            int i = message.what;
            switch (i) {
                case 2:
                    a.abc(71, String.valueOf(2));
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive handler message BASTET_CONNECTION_CLOSED");
                    this.ia.xn();
                    return;
                case 4:
                    this.ia.xf(message);
                    return;
                case 5:
                    a.abc(71, String.valueOf(5));
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive handler message BASTET_HB_NOT_AVAILABLE");
                    this.ia.xn();
                    return;
                case 7:
                    a.abc(71, String.valueOf(7));
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive handler message BASTET_RECONNECTION_BEST_POINT");
                    this.ia.xn();
                    return;
                case 8:
                    a.abc(71, String.valueOf(8));
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive handler message BASTET_RECONNECTION_BREAK");
                    this.ia.xn();
                    return;
                default:
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive handler message default, what is " + i);
                    return;
            }
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "handle bastetMessage error:" + e.getMessage(), e);
            this.ia.xe();
        }
        com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "handle bastetMessage error:" + e.getMessage(), e);
        this.ia.xe();
    }
}
