package com.android.server.wm;

import com.android.server.LocalServices;

public class WindowManagerInternalEx {
    private WindowManagerInternal mWindowManagerInternal = ((WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class));

    public void setFocusedDisplay(int displayId, boolean findTopTask, String reason) {
        WindowManagerInternal windowManagerInternal = this.mWindowManagerInternal;
        if (windowManagerInternal != null) {
            windowManagerInternal.setFocusedDisplay(displayId, findTopTask, reason);
        }
    }

    public int getFocusedDisplayId() {
        WindowManagerInternal windowManagerInternal = this.mWindowManagerInternal;
        if (windowManagerInternal == null) {
            return 0;
        }
        return windowManagerInternal.getFocusedDisplayId();
    }
}
