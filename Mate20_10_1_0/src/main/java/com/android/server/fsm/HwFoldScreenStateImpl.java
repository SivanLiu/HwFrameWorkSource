package com.android.server.fsm;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.DisplayViewport;
import android.hardware.display.HwFoldScreenState;
import android.hardware.input.InputManagerInternal;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.wm.WindowManagerInternal;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;
import huawei.android.hwutil.HwFullScreenDisplay;

public class HwFoldScreenStateImpl extends HwFoldScreenState {
    public static final int DISPLAY_REGION_MODE_FULL = 7;
    public static final int DISPLAY_REGION_MODE_MAIN = 1;
    public static final int DISPLAY_REGION_MODE_SUB = 6;
    public static final int DISPLAY_REGION_MODE_UNKNOWN = 0;
    public static final int NAV_BAR_HEIGHT = 122;
    public static final int REGION_TYPE_EDGE = 2;
    public static final int REGION_TYPE_MAIN = 1;
    public static final int REGION_TYPE_SUB = 4;
    public static final int REGION_TYPE_UNKNOWN = 0;
    private static final String TAG = "Fsm_FoldScreenStateImpl";
    private static WakeupManager mWakeupManager = null;
    private Context mContext;
    private int mDisplayMode = 0;
    private DisplayManagerInternal mDm;
    private HwFoldScreenManagerInternal mFsm;
    private Rect mFullDispRect = new Rect();
    private InputManagerInternal mInputManager;
    private Rect mMainDispRect = new Rect();
    private Rect mSubDispRect = new Rect();
    private Rect mTmpDispRect = new Rect();
    private WindowManagerInternal mWm;

    public HwFoldScreenStateImpl(Context context) {
        this.mContext = context;
        initDisplayRect();
    }

    public static int getDisplayRect(Point point) {
        if (point == null) {
            return 0;
        }
        int x = SCREEN_FOLD_FULL_WIDTH - point.y;
        int y = point.x;
        Slog.i("Fsm_FoldScreenStateImpl", "getDisplayRect x:" + x + ", y:" + y);
        if (y > SCREEN_FOLD_FULL_HEIGHT) {
            return 0;
        }
        if (x <= SCREEN_FOLD_SUB_WIDTH) {
            return 4;
        }
        if (x < SCREEN_FOLD_SUB_WIDTH + SCREEN_FOLD_EDGE_WIDTH) {
            return 2;
        }
        if (x <= SCREEN_FOLD_FULL_WIDTH) {
            return 1;
        }
        return 0;
    }

    public static void setWakeUpManager(WakeupManager wm) {
        mWakeupManager = wm;
        Slog.d("Fsm_FoldScreenStateImpl", "setWakeUpManager done:" + mWakeupManager);
    }

    public Rect getScreenDispRect(int orientation) {
        if (orientation < 0 || orientation > 3) {
            return null;
        }
        Rect tmpDispRect = getCurrentDispRect();
        if (tmpDispRect == null) {
            return tmpDispRect;
        }
        if (orientation == 0) {
            this.mTmpDispRect.set(tmpDispRect);
        } else if (orientation == 1) {
            this.mTmpDispRect.left = tmpDispRect.top;
            this.mTmpDispRect.right = tmpDispRect.bottom;
            this.mTmpDispRect.top = SCREEN_FOLD_FULL_WIDTH - tmpDispRect.right;
            this.mTmpDispRect.bottom = SCREEN_FOLD_FULL_WIDTH - tmpDispRect.left;
        } else if (orientation == 2) {
            this.mTmpDispRect.left = SCREEN_FOLD_FULL_WIDTH - tmpDispRect.right;
            this.mTmpDispRect.right = SCREEN_FOLD_FULL_WIDTH - tmpDispRect.left;
            this.mTmpDispRect.top = SCREEN_FOLD_REAL_FULL_HEIGHT - tmpDispRect.bottom;
            this.mTmpDispRect.bottom = SCREEN_FOLD_REAL_FULL_HEIGHT - tmpDispRect.top;
        } else if (orientation == 3) {
            this.mTmpDispRect.left = SCREEN_FOLD_REAL_FULL_HEIGHT - tmpDispRect.bottom;
            this.mTmpDispRect.right = SCREEN_FOLD_REAL_FULL_HEIGHT - tmpDispRect.top;
            this.mTmpDispRect.top = tmpDispRect.left;
            this.mTmpDispRect.bottom = tmpDispRect.right;
        }
        Slog.d("Fsm_FoldScreenStateImpl", "getScreenDispRect=" + tmpDispRect);
        return this.mTmpDispRect;
    }

    public int getDisplayMode() {
        return this.mDisplayMode;
    }

    public int setDisplayMode(int mode) {
        boolean isScreenOn = ((PowerManager) this.mContext.getSystemService("power")).isScreenOn();
        int oldDisplayMode = this.mDisplayMode;
        Rect dispRegion = getDispRect(mode);
        if (dispRegion != null) {
            int h = dispRegion.height();
            HwFullScreenDisplay.setFullScreenData(h, h - 122, dispRegion.width());
            setRealDisplayMode(mode);
        }
        if (this.mFsm == null) {
            this.mFsm = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
        }
        HwFoldScreenManagerInternal hwFoldScreenManagerInternal = this.mFsm;
        if (hwFoldScreenManagerInternal != null) {
            hwFoldScreenManagerInternal.onSetFoldDisplayModeFinished(this.mDisplayMode, oldDisplayMode);
        }
        if (isScreenOn) {
            return 0;
        }
        if (this.mDm == null) {
            this.mDm = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        }
        HwFoldScreenManagerInternal hwFoldScreenManagerInternal2 = this.mFsm;
        if (hwFoldScreenManagerInternal2 != null && hwFoldScreenManagerInternal2.getInfoDrawWindow()) {
            Slog.d("Fsm_FoldScreenStateImpl", "requestScreenState");
            this.mDm.requestScreenState();
            this.mFsm.resetInfoDrawWindow();
            return 0;
        } else if (mWakeupManager == null) {
            return 0;
        } else {
            handleResumeDispModeChange();
            return 0;
        }
    }

    public void adjustViewportFrame(DisplayViewport viewport, Rect currentLayerRect, Rect currentDisplayRect) {
        int tmpOffsetHeight;
        int orientation = viewport.orientation;
        boolean isRealFoldDevice = mIsFoldScreenDevice && !mIsSimulated;
        Rect currentDispRect = getScreenDispRect(orientation);
        if (currentDispRect != null) {
            if (isRealFoldDevice) {
                if (currentLayerRect != null) {
                    viewport.logicalFrame.set(currentDisplayRect);
                }
            } else if (!viewport.logicalFrame.isEmpty()) {
                viewport.logicalFrame.set(currentDispRect);
            }
            if (!viewport.physicalFrame.isEmpty()) {
                if (isRealFoldDevice) {
                    viewport.physicalFrame.set(currentDisplayRect);
                } else {
                    viewport.physicalFrame.set(currentDispRect);
                }
                if (orientation != 0) {
                    if (orientation == 1) {
                        if (!isRealFoldDevice && (tmpOffsetHeight = SCREEN_FOLD_FULL_WIDTH - currentDispRect.height()) > 0) {
                            if (currentDispRect.top == 0) {
                                viewport.physicalFrame.top += tmpOffsetHeight;
                                viewport.physicalFrame.bottom += tmpOffsetHeight;
                                return;
                            }
                            viewport.physicalFrame.top -= tmpOffsetHeight;
                            viewport.physicalFrame.bottom -= tmpOffsetHeight;
                        }
                    } else if (orientation == 3) {
                        if (isRealFoldDevice) {
                            int i = this.mDisplayMode;
                            if (i == 3) {
                                int tmpOffsetWidth = SCREEN_FOLD_FULL_WIDTH - SCREEN_FOLD_MAIN_WIDTH;
                                viewport.physicalFrame.left += tmpOffsetWidth;
                                viewport.physicalFrame.right += tmpOffsetWidth;
                            } else if (i == 2) {
                                int tmpOffsetWidth2 = SCREEN_FOLD_FULL_WIDTH - SCREEN_FOLD_MAIN_WIDTH;
                                viewport.physicalFrame.left -= tmpOffsetWidth2;
                                viewport.physicalFrame.right -= tmpOffsetWidth2;
                            }
                        } else {
                            int tmpOffsetWidth3 = SCREEN_FOLD_REAL_FULL_HEIGHT - currentDispRect.width();
                            if (tmpOffsetWidth3 > 0) {
                                viewport.physicalFrame.left -= tmpOffsetWidth3;
                                viewport.physicalFrame.right -= tmpOffsetWidth3;
                            }
                        }
                    } else if (orientation != 2) {
                    } else {
                        if (isRealFoldDevice) {
                            int i2 = this.mDisplayMode;
                            if (i2 == 3) {
                                int tmpOffsetHeight2 = SCREEN_FOLD_FULL_WIDTH - SCREEN_FOLD_MAIN_WIDTH;
                                viewport.physicalFrame.top += tmpOffsetHeight2;
                                viewport.physicalFrame.bottom += tmpOffsetHeight2;
                            } else if (i2 == 2) {
                                int tmpOffsetHeight3 = SCREEN_FOLD_FULL_WIDTH - SCREEN_FOLD_MAIN_WIDTH;
                                viewport.physicalFrame.top -= tmpOffsetHeight3;
                                viewport.physicalFrame.bottom -= tmpOffsetHeight3;
                            }
                        } else {
                            int tmpOffsetHeight4 = SCREEN_FOLD_REAL_FULL_HEIGHT - currentDispRect.height();
                            int tmpOffsetWidth4 = SCREEN_FOLD_FULL_WIDTH - currentDispRect.width();
                            if (tmpOffsetHeight4 > 0) {
                                viewport.physicalFrame.top -= tmpOffsetHeight4;
                                viewport.physicalFrame.bottom -= tmpOffsetHeight4;
                            }
                            if (tmpOffsetWidth4 <= 0) {
                                return;
                            }
                            if (currentDispRect.left == 0) {
                                viewport.physicalFrame.left += tmpOffsetWidth4;
                                viewport.physicalFrame.right += tmpOffsetWidth4;
                                return;
                            }
                            viewport.physicalFrame.left -= tmpOffsetWidth4;
                            viewport.physicalFrame.right -= tmpOffsetWidth4;
                        }
                    }
                }
            }
        }
    }

    public int rotateScreen() {
        if (!mIsFoldScreenDevice || mIsSimulated) {
            return 0;
        }
        return 3;
    }

    private void initDisplayRect() {
        this.mFullDispRect.set(0, 0, SCREEN_FOLD_FULL_WIDTH, SCREEN_FOLD_FULL_HEIGHT);
        this.mMainDispRect.set(SCREEN_FOLD_FULL_WIDTH - SCREEN_FOLD_MAIN_WIDTH, 0, SCREEN_FOLD_FULL_WIDTH, SCREEN_FOLD_FULL_HEIGHT);
        this.mSubDispRect.set(0, 0, SCREEN_FOLD_MAIN_WIDTH, SCREEN_FOLD_FULL_HEIGHT);
    }

    private Rect getCurrentDispRect() {
        return getDispRect(this.mDisplayMode);
    }

    private Rect getDispRect(int mode) {
        Rect screenRect;
        if (mode == 1) {
            screenRect = this.mFullDispRect;
        } else if (mode == 2) {
            screenRect = this.mMainDispRect;
        } else if (mode == 3) {
            screenRect = this.mSubDispRect;
        } else if (mode == 4) {
            screenRect = this.mFullDispRect;
        } else {
            screenRect = this.mFullDispRect;
        }
        Slog.d("Fsm_FoldScreenStateImpl", "getCurrentDispRect = " + screenRect);
        return screenRect;
    }

    private boolean setRealDisplayMode(int mode) {
        if (mode == this.mDisplayMode) {
            Slog.d("Fsm_FoldScreenStateImpl", "Current mode don't change, return!");
            if (this.mDm == null) {
                this.mDm = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
            }
            this.mDm.resetDisplayDelay();
            handleResumeDispModeChange();
            return false;
        }
        Rect screenRect = getDispRect(mode);
        if (this.mWm == null) {
            this.mWm = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        }
        if (!(this.mWm == null || screenRect == null || screenRect.isEmpty())) {
            Slog.d("Fsm_FoldScreenStateImpl", "setRealDisplayMode new mode:" + mode + ", old mode:" + this.mDisplayMode + " screenRect=" + screenRect);
            Bundle foldScreenInfo = new Bundle();
            foldScreenInfo.putBoolean("isFold", true);
            foldScreenInfo.putInt("fromFoldMode", this.mDisplayMode);
            foldScreenInfo.putInt("toFoldMode", mode);
            this.mDisplayMode = mode;
            this.mWm.setForcedDisplaySize(0, screenRect.width(), screenRect.height(), foldScreenInfo);
            if (this.mInputManager == null) {
                this.mInputManager = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
            }
            InputManagerInternal inputManagerInternal = this.mInputManager;
            if (inputManagerInternal != null) {
                inputManagerInternal.setDisplayMode(mode, SCREEN_FOLD_SUB_WIDTH, SCREEN_FOLD_MAIN_WIDTH, SCREEN_FOLD_FULL_HEIGHT);
                Slog.d("Fsm_FoldScreenStateImpl", "mIM.setDisplayMode !");
            }
        }
        return true;
    }

    private void handleResumeDispModeChange() {
        if (this.mFsm == null) {
            this.mFsm = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
        }
        if (this.mFsm != null) {
            Slog.d("Fsm_FoldScreenStateImpl", "handleResumeDispModeChange from FSSImpl");
            this.mFsm.resumeDispModeChange();
            WindowManagerInternal windowManagerInternal = this.mWm;
            if (windowManagerInternal != null) {
                windowManagerInternal.unFreezeFoldRotation();
            }
        }
    }
}
