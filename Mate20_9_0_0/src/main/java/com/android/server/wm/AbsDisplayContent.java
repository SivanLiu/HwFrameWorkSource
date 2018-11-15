package com.android.server.wm;

public abstract class AbsDisplayContent extends WindowContainer<DisplayChildWindowContainer> {
    AbsDisplayContent(WindowManagerService service) {
        super(service);
    }

    public void setDisplayRotationFR(int rotation) {
    }

    protected void uploadOrientation(int rotation) {
    }
}
