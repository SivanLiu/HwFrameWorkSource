package com.android.server.pm;

import android.content.pm.ActivityInfo;
import com.android.server.pm.PreferredComponent.Callbacks;

public class HwCustPreferredComponent {
    public boolean isSkipHwStarupGuide(Callbacks callbacks, ActivityInfo ai) {
        return false;
    }
}
