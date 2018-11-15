package com.android.server.display;

import android.media.RemoteDisplay;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import com.android.server.display.WifiDisplayController.Listener;

public interface IWifiDisplayControllerInner {
    void disconnectInner();

    Channel getWifiP2pChannelInner();

    WifiP2pManager getWifiP2pManagerInner();

    boolean getmDiscoverPeersInProgress();

    Listener getmListener();

    RemoteDisplay getmRemoteDisplay();

    void postDelayedDiscover();

    void requestPeersEx();

    void requestStartScanInner();
}
