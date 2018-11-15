package com.android.server.display;

import com.android.server.display.DisplayManagerService.SyncRoot;

public interface IHwDisplayManagerInner {
    SyncRoot getLock();

    WifiDisplayAdapter getWifiDisplayAdapter();

    void startWifiDisplayScanInner(int i, int i2);
}
