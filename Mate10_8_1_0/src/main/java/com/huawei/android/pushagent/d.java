package com.huawei.android.pushagent;

import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import com.huawei.android.pushagent.b.a;
import com.huawei.android.pushagent.utils.a.b;

final class d extends NetworkCallback {
    final /* synthetic */ PushService jo;

    d(PushService pushService) {
        this.jo = pushService;
    }

    public void onAvailable(Network network) {
        super.onAvailable(network);
        b.z(PushService.TAG, "onAvailable");
        a.aak(10);
        this.jo.aba(this.jo.iw, network, true);
    }

    public void onLost(Network network) {
        super.onLost(network);
        b.z(PushService.TAG, "onLost");
        a.aak(11);
        this.jo.aba(this.jo.iw, network, false);
    }

    public void onNetworkResumed(Network network) {
        super.onNetworkResumed(network);
        b.z(PushService.TAG, "onNetworkResumed");
        a.aak(12);
        this.jo.aba(this.jo.iw, network, true);
    }
}
