package com.android.server.wm;

public interface IHwDisplayContentEx {
    void focusWinZrHung(WindowState windowState, AppWindowToken appWindowToken, int i);

    boolean isPointOutsideMagicWindow(WindowState windowState, int i, int i2);
}
