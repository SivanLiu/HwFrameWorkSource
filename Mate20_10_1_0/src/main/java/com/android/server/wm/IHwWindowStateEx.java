package com.android.server.wm;

import android.graphics.Rect;

public interface IHwWindowStateEx {
    Rect adjustImePosForFreeform(Rect rect, Rect rect2);

    int adjustTopForFreeform(Rect rect, Rect rect2, int i);

    void createMagicWindowDimmer();

    void destoryMagicWindowDimmer();

    boolean isInHideCaptionList();

    boolean isInHwFreeFormWorkspace();

    boolean isNeedMoveAnimation(WindowState windowState);

    void stopMagicWindowDimmer();

    boolean updateMagicWindowDimmer();
}
