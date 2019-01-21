package com.huawei.android.pushagent;

import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import com.huawei.android.pushagent.utils.b.a;

final class e extends NetworkCallback {
    final /* synthetic */ PushService ju;

    e(PushService pushService) {
        this.ju = pushService;
    }

    public void onAvailable(Network network) {
        super.onAvailable(network);
        a.sv(PushService.TAG, "onAvailable");
        com.huawei.android.pushagent.b.a.abd(10);
        this.ju.abz(this.ju.jj, network, true);
    }

    public void onLost(Network network) {
        super.onLost(network);
        a.sv(PushService.TAG, "onLost");
        com.huawei.android.pushagent.b.a.abd(11);
        this.ju.abz(this.ju.jj, network, false);
    }

    public void onNetworkResumed(Network network) {
        super.onNetworkResumed(network);
        a.sv(PushService.TAG, "onNetworkResumed");
        com.huawei.android.pushagent.b.a.abd(12);
        this.ju.abz(this.ju.jj, network, true);
    }
}
