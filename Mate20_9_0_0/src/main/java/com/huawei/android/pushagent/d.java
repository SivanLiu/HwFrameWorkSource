package com.huawei.android.pushagent;

import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import com.huawei.android.pushagent.a.a;
import com.huawei.android.pushagent.utils.f.c;

final class d extends NetworkCallback {
    final /* synthetic */ PushService jt;

    d(PushService pushService) {
        this.jt = pushService;
    }

    public void onAvailable(Network network) {
        super.onAvailable(network);
        c.ep(PushService.TAG, "onAvailable");
        a.hx(10);
        this.jt.abu(this.jt.jb, network, true);
    }

    public void onLost(Network network) {
        super.onLost(network);
        c.ep(PushService.TAG, "onLost");
        a.hx(11);
        this.jt.abu(this.jt.jb, network, false);
    }

    public void onNetworkResumed(Network network) {
        super.onNetworkResumed(network);
        c.ep(PushService.TAG, "onNetworkResumed");
        a.hx(12);
        this.jt.abu(this.jt.jb, network, true);
    }
}
