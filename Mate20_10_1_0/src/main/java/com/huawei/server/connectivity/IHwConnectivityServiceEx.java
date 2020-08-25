package com.huawei.server.connectivity;

import android.net.RouteInfo;
import android.os.Handler;
import android.os.Message;
import com.android.server.connectivity.NetworkAgentInfo;

public interface IHwConnectivityServiceEx {
    int getCacheNetworkState(int i, String str);

    NetworkAgentInfo getIdenticalActiveNetworkAgentInfo(NetworkAgentInfo networkAgentInfo);

    void maybeHandleNetworkAgentMessageEx(Message message, NetworkAgentInfo networkAgentInfo, Handler handler);

    void removeLegacyRouteToHost(int i, RouteInfo routeInfo, int i2);

    void setCacheNetworkState(int i, String str, boolean z);

    void setupUniqueDeviceName();
}
