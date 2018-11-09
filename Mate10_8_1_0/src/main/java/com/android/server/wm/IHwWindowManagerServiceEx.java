package com.android.server.wm;

import android.graphics.Rect;
import android.util.MergedConfiguration;
import java.util.List;

public interface IHwWindowManagerServiceEx {
    public static final int NOTCH_MODE_ALWAYS = 1;
    public static final int NOTCH_MODE_NEVER = 2;

    void adjustWindowPosForPadPC(Rect rect, Rect rect2, WindowState windowState, WindowState windowState2, WindowState windowState3);

    int getAppUseNotchMode(String str);

    int getFocusWindowWidth(WindowState windowState, WindowState windowState2);

    boolean getGestureState();

    List<String> getNotchSystemApps();

    boolean isAppNotchSupport(String str);

    void layoutWindowForPadPCMode(WindowState windowState, WindowState windowState2, WindowState windowState3, Rect rect, Rect rect2, Rect rect3, Rect rect4, int i);

    void onChangeConfiguration(MergedConfiguration mergedConfiguration, WindowState windowState);

    void setViewAlpha(float f, RootWindowContainer rootWindowContainer);
}
