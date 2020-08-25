package com.android.server.wm;

import android.common.HwFrameworkFactory;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;
import com.android.server.rms.iaware.feature.SceneRecogFeature;

public final class HwDisplayContentEx implements IHwDisplayContentEx {
    private static final int DEFALUT_CAPACITY = 0;
    private static final String TAG = "HwDisplayContentEx";
    private static IZrHung focusWindowZrHung = HwFrameworkFactory.getZrHung("appeye_nofocuswindow");
    private static IZrHung transWindowZrHung = HwFrameworkFactory.getZrHung("appeye_transparentwindow");

    public void focusWinZrHung(WindowState currentFocus, AppWindowToken focusedApp, int displayId) {
        ArrayMap<String, Object> params = new ArrayMap<>(0);
        if (currentFocus != null) {
            params.put("focusedWindowName", currentFocus.toString());
            params.put("layoutParams", currentFocus.getAttrs());
            if (currentFocus.mSession != null) {
                params.put(SceneRecogFeature.DATA_PID, Integer.valueOf(currentFocus.mSession.mPid));
            }
            params.put("focusedWinPackageName", currentFocus.getOwningPackage());
        } else {
            params.put("focusedWindowName", "null");
            params.put("layoutParams", "null");
            params.put(SceneRecogFeature.DATA_PID, 0);
            params.put("focusedWinPackageName", "null");
        }
        if (focusedApp != null) {
            params.put("focusedAppPackageName", focusedApp.appPackageName);
            params.put("focusedActivityName", focusedApp.appComponentName);
        } else {
            params.put("focusedAppPackageName", "null");
            params.put("focusedActivityName", "null");
        }
        params.put("displayId", Integer.valueOf(displayId));
        ZrHungData arg = new ZrHungData();
        arg.putAll(params);
        IZrHung iZrHung = focusWindowZrHung;
        if (iZrHung != null) {
            if (currentFocus == null) {
                iZrHung.check(arg);
            } else {
                iZrHung.cancelCheck(arg);
            }
        }
        IZrHung iZrHung2 = transWindowZrHung;
        if (iZrHung2 != null) {
            iZrHung2.cancelCheck(arg);
            if (currentFocus != null) {
                transWindowZrHung.check(arg);
            }
        }
    }

    public boolean isPointOutsideMagicWindow(WindowState win, int x, int y) {
        if (win == null || !win.inHwMagicWindowingMode() || Math.abs(1.0f - win.mMwUsedScaleFactor) < 1.0E-6f) {
            return false;
        }
        Rect tmpBound = new Rect();
        tmpBound.set(win.getVisibleFrameLw());
        if (tmpBound.isEmpty()) {
            tmpBound.set(win.getFrameLw());
        }
        tmpBound.right = (int) (((float) tmpBound.left) + (((float) tmpBound.width()) * win.mMwUsedScaleFactor) + 0.5f);
        tmpBound.bottom = (int) (((float) tmpBound.top) + (((float) tmpBound.height()) * win.mMwUsedScaleFactor) + 0.5f);
        return !tmpBound.contains(x, y);
    }
}
