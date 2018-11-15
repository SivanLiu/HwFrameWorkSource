package com.android.server.am;

import android.util.HwPCUtils;
import android.util.SparseIntArray;
import com.huawei.server.am.IHwActivityStackSupervisorEx;

public class HwActivityStackSupervisorEx implements IHwActivityStackSupervisorEx {
    public static final String TAG = "HwActivityStackSupervisorEx";

    public void adjustFocusDisplayOrder(SparseIntArray tmpOrderedDisplayIds, int displayIdForStack) {
        if (HwPCUtils.isPcCastModeInServer() && tmpOrderedDisplayIds != null) {
            int N = tmpOrderedDisplayIds.size();
            if (N > 1) {
                int tempElem = tmpOrderedDisplayIds.get(N - 1);
                if (tempElem != displayIdForStack) {
                    for (int i = N - 2; i >= 0; i--) {
                        int displayId = tmpOrderedDisplayIds.get(i);
                        if (displayId == displayIdForStack) {
                            tmpOrderedDisplayIds.put(N - 1, displayIdForStack);
                            tmpOrderedDisplayIds.put(i, tempElem);
                            break;
                        }
                        tmpOrderedDisplayIds.put(i, tempElem);
                        tempElem = displayId;
                    }
                }
            }
        }
    }
}
