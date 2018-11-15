package com.android.server.am;

import android.content.Intent;
import android.content.res.Configuration;
import com.android.server.wm.ConfigurationContainer;

public abstract class AbsActivityStack extends ConfigurationContainer {
    public int getInvalidFlag(int changes, Configuration newConfig, Configuration naviConfig) {
        return 0;
    }

    protected boolean isSplitActivity(Intent intent) {
        return false;
    }

    protected void resumeCustomActivity(ActivityRecord next) {
    }

    protected void setKeepPortraitFR() {
    }

    public void makeStackVisible(boolean visible) {
    }
}
