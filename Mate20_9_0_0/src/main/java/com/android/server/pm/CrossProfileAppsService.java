package com.android.server.pm;

import android.content.Context;
import com.android.server.SystemService;

public class CrossProfileAppsService extends SystemService {
    private CrossProfileAppsServiceImpl mServiceImpl;

    public CrossProfileAppsService(Context context) {
        super(context);
        this.mServiceImpl = new CrossProfileAppsServiceImpl(context);
    }

    public void onStart() {
        publishBinderService("crossprofileapps", this.mServiceImpl);
    }
}
