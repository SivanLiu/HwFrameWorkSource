package com.android.server.wm;

import com.android.server.policy.WindowManagerPolicy.StartingSurface;

public abstract class StartingData {
    protected final WindowManagerService mService;

    abstract StartingSurface createStartingSurface(AppWindowToken appWindowToken);

    protected StartingData(WindowManagerService service) {
        this.mService = service;
    }
}
