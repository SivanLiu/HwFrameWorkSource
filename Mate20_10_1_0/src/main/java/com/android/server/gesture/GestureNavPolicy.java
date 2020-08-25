package com.android.server.gesture;

import android.graphics.Point;
import com.android.server.policy.WindowManagerPolicy;
import java.io.PrintWriter;

public interface GestureNavPolicy {
    void bringTopSubScreenNavView();

    void destroySubScreenNavView();

    void dump(String str, PrintWriter printWriter, String[] strArr);

    void initSubScreenNavView();

    boolean isGestureNavStartedNotLocked();

    boolean isKeyNavEnabled();

    boolean isPointInExcludedRegion(Point point);

    void onConfigurationChanged();

    boolean onFocusWindowChanged(WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2);

    void onKeyguardShowingChanged(boolean z);

    void onLayoutInDisplayCutoutModeChanged(WindowManagerPolicy.WindowState windowState, boolean z, boolean z2);

    void onLockTaskStateChanged(int i);

    void onMultiWindowChanged(int i);

    void onRotationChanged(int i);

    void onUserChanged(int i);

    void setGestureNavMode(String str, int i, int i2, int i3, int i4);

    void systemReady();

    void updateGestureNavRegion(boolean z, int i);
}
