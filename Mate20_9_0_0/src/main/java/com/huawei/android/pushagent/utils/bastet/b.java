package com.huawei.android.pushagent.utils.bastet;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.huawei.android.pushagent.a.a;
import com.huawei.android.pushagent.utils.f.c;

final class b extends Handler {
    final /* synthetic */ a ay;

    b(a aVar, Looper looper) {
        this.ay = aVar;
        super(looper);
    }

    public void handleMessage(Message message) {
        try {
            super.handleMessage(message);
            int i = message.what;
            switch (i) {
                case 2:
                    a.hq(71, String.valueOf(2));
                    c.ep("PushLog3413", "receive handler message BASTET_CONNECTION_CLOSED");
                    this.ay.du();
                    return;
                case 4:
                    this.ay.dm(message);
                    return;
                case 5:
                    a.hq(71, String.valueOf(5));
                    c.ep("PushLog3413", "receive handler message BASTET_HB_NOT_AVAILABLE");
                    this.ay.du();
                    return;
                case 7:
                    a.hq(71, String.valueOf(7));
                    c.ep("PushLog3413", "receive handler message BASTET_RECONNECTION_BEST_POINT");
                    this.ay.du();
                    return;
                case 8:
                    a.hq(71, String.valueOf(8));
                    c.ep("PushLog3413", "receive handler message BASTET_RECONNECTION_BREAK");
                    this.ay.du();
                    return;
                default:
                    c.ep("PushLog3413", "receive handler message default, what is " + i);
                    return;
            }
        } catch (Throwable e) {
            c.es("PushLog3413", "handle bastetMessage error:" + e.getMessage(), e);
            this.ay.dl();
        }
        c.es("PushLog3413", "handle bastetMessage error:" + e.getMessage(), e);
        this.ay.dl();
    }
}
