package com.huawei.android.pushselfshow.richpush.html.a;

import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.richpush.html.a.e.a;
import com.huawei.android.pushselfshow.richpush.html.api.d;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.MessageSafeConfigJsonField;
import org.json.JSONObject;

class f implements Runnable {
    final /* synthetic */ e a;

    f(e eVar) {
        this.a = eVar;
    }

    public void run() {
        try {
            c.e("PushSelfShowLog", "getPlayingStatusRb getPlayingStatus this.state = " + this.a.e);
            if (a.MEDIA_RUNNING.ordinal() == this.a.e.ordinal()) {
                long -l_1_J = this.a.i();
                float -l_3_F = this.a.k();
                Object -l_4_R = new JSONObject();
                try {
                    -l_4_R.put("current_postion", -l_1_J);
                    -l_4_R.put("duration", (double) -l_3_F);
                    -l_4_R.put(CheckVersionField.CHECK_VERSION_SERVER_URL, this.a.f);
                    this.a.j.a(this.a.a, d.a.OK, MessageSafeConfigJsonField.UPDATE_STATUS, -l_4_R);
                } catch (Object -l_5_R) {
                    c.e("PushSelfShowLog", "getPlayingStatus error", -l_5_R);
                }
            }
        } catch (Object -l_1_R) {
            c.e("PushSelfShowLog", "getPlayingStatusRb run error", -l_1_R);
        }
        if (a.MEDIA_NONE.ordinal() != this.a.e.ordinal() && a.MEDIA_STOPPED.ordinal() != this.a.e.ordinal()) {
            this.a.b.postDelayed(this, (long) this.a.g);
        }
    }
}
