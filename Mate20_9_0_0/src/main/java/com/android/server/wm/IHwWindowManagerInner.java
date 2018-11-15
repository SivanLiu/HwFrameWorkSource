package com.android.server.wm;

import android.app.AppOpsManager;
import com.android.server.policy.WindowManagerPolicy;

public interface IHwWindowManagerInner {
    AppOpsManager getAppOps();

    InputMonitor getInputMonitor();

    WindowManagerPolicy getPolicy();

    RootWindowContainer getRoot();

    TaskSnapshotController getTaskSnapshotController();

    HwWMDAMonitorProxy getWMMonitor();

    WindowHashMap getWindowMap();

    void updateAppOpsState();
}
