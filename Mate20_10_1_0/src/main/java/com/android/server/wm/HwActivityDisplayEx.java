package com.android.server.wm;

import android.util.HwMwUtils;
import android.util.HwPCUtils;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;

public class HwActivityDisplayEx implements IHwActivityDisplayEx {
    public boolean launchMagicOnSplitScreenDismissed(ActivityStack top) {
        return top != null && top.inHwMagicWindowingMode() && !HwPCUtils.isPcCastModeInServer();
    }

    public boolean keepStackResumed(ActivityStack stack) {
        if (stack != null && stack.inHwMagicWindowingMode()) {
            if (HwMwUtils.performPolicy((int) BigMemoryConstant.ACTIVITY_NAME_MAX_LEN, new Object[]{Integer.valueOf(stack.getStackId()), false}).getBoolean("RESULT_IN_APP_SPLIT", false)) {
                return true;
            }
        }
        return false;
    }
}
