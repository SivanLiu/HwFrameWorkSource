package com.android.server.wm;

import android.content.ClipData;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.util.ArraySet;
import android.util.MergedConfiguration;
import android.view.DragEvent;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.widget.RemoteViews;
import com.android.server.policy.WindowManagerPolicy;
import com.huawei.android.view.HwTaskSnapshotWrapper;
import com.huawei.android.view.IHwMultiDisplayBitmapDragStartListener;
import com.huawei.android.view.IHwMultiDisplayDragStartListener;
import com.huawei.android.view.IHwMultiDisplayDragStateListener;
import com.huawei.android.view.IHwMultiDisplayDropStartListener;
import com.huawei.android.view.IHwMultiDisplayDroppableListener;
import com.huawei.android.view.IHwMultiDisplayPhoneOperateListener;
import java.util.ArrayList;
import java.util.List;

public interface IHwWindowManagerServiceEx {
    public static final int NOTCH_MODE_ALWAYS = 1;
    public static final int NOTCH_MODE_NEVER = 2;

    void addWindowReport(WindowState windowState, int i);

    void adjustWindowPosForPadPC(Rect rect, Rect rect2, WindowState windowState, WindowState windowState2, WindowState windowState3);

    void checkSingleHandMode(AppWindowToken appWindowToken, AppWindowToken appWindowToken2);

    void clearAppWindowIconInfo(WindowState windowState, int i);

    boolean detectSafeMode();

    boolean dragStartForMultiDisplay(ClipData clipData);

    boolean dropStartForMultiDisplay(DragEvent dragEvent);

    AppWindowToken findExitToLauncherMaxWindowSizeToken(ArraySet<AppWindowToken> arraySet, int i, int i2);

    void freezeOrThawRotation(int i);

    void getAppDisplayRect(float f, Rect rect, int i, int i2);

    int getAppUseNotchMode(String str);

    List<Rect> getBounds(int i);

    Configuration getCurNaviConfiguration();

    void getCurrFocusedWinInExtDisplay(Bundle bundle);

    float getDefaultNonFullMaxRatio();

    float getDeviceMaxRatio();

    Bitmap getDisplayBitmap(int i, int i2, int i3);

    String getDragSrcPkgName();

    float getExclusionNavBarMaxRatio();

    int getFocusWindowWidth(WindowState windowState, WindowState windowState2);

    Rect getFocuseWindowVisibleFrame(WindowManagerService windowManagerService);

    HwTaskSnapshotWrapper getForegroundTaskSnapshotWrapper(TaskSnapshotController taskSnapshotController, WindowState windowState, boolean z);

    boolean getIgnoreFrozen();

    int getLazyModeEx();

    float getLazyModeScale();

    Animation getMagicWindowAnimation(Animation animation, boolean z, int i, AppWindowToken appWindowToken, Rect rect);

    Interpolator getMagicWindowMoveInterpolator();

    List<String> getNotchSystemApps();

    Rect getSafeInsets(int i);

    boolean getSafeMode();

    ArrayList<WindowState> getSecureScreenWindow();

    int getTopActivityAdaptNotchState(String str);

    Rect getTopAppDisplayBounds(float f, int i, int i2);

    String getTopAppPackageByWindowMode(int i, RootWindowContainer rootWindowContainer);

    String getTouchedWinPackageName(float f, float f2, int i);

    List<Bundle> getVisibleWindows(int i);

    void handleNewDisplayConfiguration(Configuration configuration, int i);

    boolean hasLighterViewInPCCastMode();

    void hwSystemReady();

    boolean isCoverOpen();

    boolean isFullScreenDevice();

    boolean isInNotchAppWhitelist(WindowState windowState);

    boolean isLastOneApp(DisplayContent displayContent);

    boolean isRightInMagicWindow(WindowState windowState);

    boolean isSecureForPCDisplay(WindowState windowState);

    boolean isShowDimForPCMode(WindowContainer windowContainer, Rect rect);

    boolean isSkipComputeImeTargetForHwMultiDisplay(WindowManagerPolicy.WindowState windowState, DisplayContent displayContent);

    boolean isSupportSingleHand();

    boolean isSwitched();

    void layoutWindowForPadPCMode(WindowState windowState, WindowState windowState2, WindowState windowState3, Rect rect, Rect rect2, Rect rect3, Rect rect4, int i);

    Animation loadAppWindowExitToLauncherAnimation(Animation animation, int i, Rect rect, AppWindowToken appWindowToken);

    boolean notifyDragAndDropForMultiDisplay(float f, float f2, int i, DragEvent dragEvent);

    void notifyFingerWinCovered(boolean z, Rect rect);

    void notifySwingRotation(int i);

    void onChangeConfiguration(MergedConfiguration mergedConfiguration, WindowState windowState);

    void onOperateOnPhone();

    void performDisplayTraversalLocked();

    void performhwLayoutAndPlaceSurfacesLocked();

    void preAddWindow(WindowManager.LayoutParams layoutParams);

    void reevaluateStatusBarSize(boolean z);

    void registerBitmapDragListenerForMultiDisplay(IHwMultiDisplayBitmapDragStartListener iHwMultiDisplayBitmapDragStartListener);

    void registerDragListenerForMultiDisplay(IHwMultiDisplayDragStartListener iHwMultiDisplayDragStartListener);

    void registerDropListenerForMultiDisplay(IHwMultiDisplayDropStartListener iHwMultiDisplayDropStartListener);

    void registerHwMultiDisplayDragStateListener(IHwMultiDisplayDragStateListener iHwMultiDisplayDragStateListener);

    void registerIsDroppableForMultiDisplay(IHwMultiDisplayDroppableListener iHwMultiDisplayDroppableListener);

    void registerPhoneOperateListenerForHwMultiDisplay(IHwMultiDisplayPhoneOperateListener iHwMultiDisplayPhoneOperateListener);

    void relaunchIMEProcess();

    Animation reloadHwSplitScreenOpeningAnimation(Animation animation, AppWindowToken appWindowToken, ArraySet<AppWindowToken> arraySet, boolean z);

    void removeAppView();

    void removeAppView(boolean z);

    void removeSecureScreenWindow(WindowState windowState);

    void removeWindowReport(WindowState windowState);

    void resetAppWindowExitInfo(int i, AppWindowToken appWindowToken);

    void restoreShadow();

    void sendFocusProcessToRMS(WindowState windowState, WindowState windowState2);

    void sendUpdateAppOpsState();

    void setAppOpHideHook(WindowState windowState, boolean z);

    void setAppWindowExitInfo(Bundle bundle, Bitmap bitmap, int i);

    void setCoverManagerState(boolean z);

    void setCurrentUser(int i, int[] iArr);

    boolean setDragStartBitmap(Bitmap bitmap);

    void setDragWinState(WindowState windowState);

    void setDroppableForMultiDisplay(float f, float f2, boolean z);

    void setForcedDisplayDensityAndSize(int i, int i2, int i3, int i4);

    void setGestureNavMode(String str, int i, int i2, int i3, int i4);

    void setHwSecureScreenShot(WindowState windowState);

    void setIgnoreFrozen(boolean z);

    void setLazyModeEx(int i, boolean z, String str);

    void setMagicWindowAnimation(boolean z, Animation animation, Animation animation2);

    void setMagicWindowMoveInterpolator(Interpolator interpolator);

    void setNotchFlags(WindowState windowState, WindowManager.LayoutParams layoutParams, DisplayPolicy displayPolicy, int i);

    void setNotchHeight(int i);

    void setOriginalDropPoint(float f, float f2);

    void setRtgThreadForAnimation(boolean z);

    void setStartWindowTransitionReady(WindowState windowState);

    void setTouchWinState(WindowState windowState);

    void setVisibleFromParent(WindowState windowState);

    boolean shouldDropMotionEventForTouchPad(float f, float f2);

    void switchDragShadow(boolean z);

    void takeTaskSnapshot(IBinder iBinder, boolean z);

    void togglePCMode(boolean z, int i);

    void unregisterBitmapDragListenerForMultiDisplay();

    void unregisterDragListenerForMultiDisplay();

    void unregisterDropListenerForMultiDisplay();

    void unregisterHwMultiDisplayDragStateListener();

    void unregisterPhoneOperateListenerForHwMultiDisplay();

    void updateAppOpsStateReport(int i, String str);

    void updateAppView(RemoteViews remoteViews);

    void updateDragState(int i);

    void updateFocusWindowFreezed(boolean z);

    void updateHwStartWindowRecord(int i);

    Point updateLazyModePoint(int i, Point point);

    void updateResourceConfiguration(int i, int i2, int i3, int i4);

    void updateStatusBarInMagicWindow(int i, WindowManager.LayoutParams layoutParams);

    void updateWindowReport(WindowState windowState, int i, int i2);
}
