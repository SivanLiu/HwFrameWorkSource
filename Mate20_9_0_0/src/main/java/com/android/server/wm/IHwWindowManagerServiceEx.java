package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.util.MergedConfiguration;
import com.huawei.android.view.HwTaskSnapshotWrapper;
import java.util.List;

public interface IHwWindowManagerServiceEx {
    public static final int NOTCH_MODE_ALWAYS = 1;
    public static final int NOTCH_MODE_NEVER = 2;

    void addWindowReport(WindowState windowState, int i);

    void adjustWindowPosForPadPC(Rect rect, Rect rect2, WindowState windowState, WindowState windowState2, WindowState windowState3);

    void checkSingleHandMode(AppWindowToken appWindowToken, AppWindowToken appWindowToken2);

    void computeShownFrameLockedByPCScreenDpMode(int i);

    void getAppDisplayRect(float f, Rect rect, int i, int i2);

    int getAppUseNotchMode(String str);

    void getCurrFocusedWinInExtDisplay(Bundle bundle);

    float getDefaultNonFullMaxRatio();

    float getDeviceMaxRatio();

    float getExclusionNavBarMaxRatio();

    int getFocusWindowWidth(WindowState windowState, WindowState windowState2);

    HwTaskSnapshotWrapper getForegroundTaskSnapshotWrapper(TaskSnapshotController taskSnapshotController, WindowState windowState, boolean z);

    List<String> getNotchSystemApps();

    int getPCScreenDisplayMode();

    float getPCScreenScale();

    Rect getTopAppDisplayBounds(float f, int i, int i2);

    List<Bundle> getVisibleWindows(int i);

    void handleNewDisplayConfiguration(Configuration configuration, int i);

    boolean hasLighterViewInPCCastMode();

    boolean isFullScreenDevice();

    boolean isInNotchAppWhitelist(WindowState windowState);

    void layoutWindowForPadPCMode(WindowState windowState, WindowState windowState2, WindowState windowState3, Rect rect, Rect rect2, Rect rect3, Rect rect4, int i);

    void notifyFingerWinCovered(boolean z, Rect rect);

    void onChangeConfiguration(MergedConfiguration mergedConfiguration, WindowState windowState);

    void removeWindowReport(WindowState windowState);

    void reportLazyModeToIAware(int i);

    void sendUpdateAppOpsState();

    void setAppOpHideHook(WindowState windowState, boolean z);

    void setNotchHeight(int i);

    void setVisibleFromParent(WindowState windowState);

    boolean shouldDropMotionEventForTouchPad(float f, float f2);

    void takeTaskSnapshot(IBinder iBinder);

    void updateAppOpsStateReport(int i, String str);

    void updateDimPositionForPCMode(WindowContainer windowContainer, Rect rect);

    void updateHwStartWindowRecord(String str);

    void updateSurfacePositionForPCMode(WindowState windowState, Point point);

    void updateWindowReport(WindowState windowState, int i, int i2);
}
