package com.android.server.wm;

import android.graphics.Rect;
import android.view.WindowManager;

public class WindowStateEx {
    private WindowState mWindowState;

    public void setWindowState(WindowState windowState) {
        this.mWindowState = windowState;
    }

    public WindowState getWindowState() {
        return this.mWindowState;
    }

    public int getDisplayId() {
        WindowState windowState = this.mWindowState;
        if (windowState != null) {
            return windowState.getDisplayId();
        }
        return 0;
    }

    public WindowManager.LayoutParams getAttrs() {
        WindowState windowState = this.mWindowState;
        if (windowState != null) {
            return windowState.getAttrs();
        }
        return null;
    }

    public Rect getVisibleFrameLw() {
        WindowState windowState = this.mWindowState;
        if (windowState != null) {
            return windowState.getVisibleFrameLw();
        }
        return null;
    }

    public Rect getDisplayFrameLw() {
        WindowState windowState = this.mWindowState;
        if (windowState != null) {
            return windowState.getDisplayFrameLw();
        }
        return null;
    }
}
