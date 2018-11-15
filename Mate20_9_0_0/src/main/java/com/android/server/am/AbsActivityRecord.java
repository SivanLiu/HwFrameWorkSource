package com.android.server.am;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import com.android.server.wm.ConfigurationContainer;

public abstract class AbsActivityRecord extends ConfigurationContainer {
    protected void initSplitMode(Intent intent) {
    }

    protected boolean isSplitMode() {
        return false;
    }

    protected boolean isForceRotationMode(String packageName, Intent intent) {
        return false;
    }

    protected int overrideRealConfigChanged(ActivityInfo info) {
        return info.getRealConfigChanged();
    }

    protected boolean isSplitBaseActivity() {
        return false;
    }
}
