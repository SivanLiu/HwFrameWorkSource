package com.android.server.wm;

import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.HwFoldScreenState;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.WindowManager;
import com.android.server.HwServiceFactory;
import com.android.server.mtm.iaware.appmng.appfreeze.AwareAppFreezeMng;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import com.huawei.displayengine.DisplayEngineManager;

public class HwWindowStateAnimator extends WindowStateAnimator {
    private static final int SCENE_POS_EXIT = -1;
    private static final int SCENE_POS_START = 1;
    private static final String TAG = "HwWindowStateAnimator";
    private static final int TYPE_LEFT = 1;
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_RIGHT = 2;
    private DisplayEngineManager mDisplayEngineManager;
    private boolean mIsDefaultDisplay = true;
    private boolean mIsLeftLazyMode;
    private boolean mIsRightLazyMode;
    private int mLastLazyMode;
    private float mLazyScale;
    private int mScreenHeight;
    private int mScreenWidth;
    private final boolean mSkipScalingDownSurface;
    private final WindowManager mWindowManager;

    public HwWindowStateAnimator(WindowState win) {
        super(win);
        this.mSkipScalingDownSurface = win.toString().contains("hwSingleMode_window");
        int displayId = this.mWin.getDisplayId();
        if (!(displayId == -1 || displayId == 0)) {
            this.mIsDefaultDisplay = false;
        }
        this.mLastLazyMode = this.mService.getLazyMode();
        if (!this.mIsDefaultDisplay) {
            this.mLastLazyMode = 0;
        }
        this.mLazyScale = 1.0f;
        this.mLazyIsExiting = false;
        this.mLazyIsEntering = false;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mDisplayEngineManager = new DisplayEngineManager();
    }

    private void updatedisplayinfo() {
        Display defaultDisplay = this.mWindowManager.getDefaultDisplay();
        DisplayInfo defaultDisplayInfo = new DisplayInfo();
        defaultDisplay.getDisplayInfo(defaultDisplayInfo);
        boolean isPortrait = defaultDisplayInfo.logicalHeight > defaultDisplayInfo.logicalWidth;
        this.mScreenWidth = isPortrait ? defaultDisplayInfo.logicalWidth : defaultDisplayInfo.logicalHeight;
        this.mScreenHeight = isPortrait ? defaultDisplayInfo.logicalHeight : defaultDisplayInfo.logicalWidth;
    }

    public int adjustAnimLayerIfCoverclosed(int type, int animLayer) {
        if (type != 2000 || animLayer >= 400000 || (!HwServiceFactory.isCoverClosed())) {
            return animLayer;
        }
        return 400000;
    }

    /* access modifiers changed from: package-private */
    public void computeShownFrameLocked(int type) {
        if (type == 1 || type == 2) {
            updatedisplayinfo();
        }
        int displayId = this.mWin.getDisplayId();
        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(displayId);
        boolean screenAnimation = screenRotationAnimation != null && screenRotationAnimation.isAnimating() && (this.mWin.mForceSeamlesslyRotate ^ true);
        float ratio = 1.0f;
        float pendingX = 0.0f;
        float pendingY = 0.0f;
        int rotation = 0;
        if (HwFoldScreenState.isFoldScreenDevice() && this.mService.isInSubFoldScaleMode() && !this.mWin.toString().contains("FoldScreen_SubScreenViewEntry") && !this.mWin.toString().contains("GestureNavSubScreenLeft") && !this.mWin.toString().contains("GestureNavSubScreenRight") && !this.mWin.toString().contains("GestureNavSubScreenBottom") && !HwPCUtils.isValidExtDisplayId(displayId)) {
            updatedisplayinfo();
            ratio = this.mService.mSubFoldModeScale;
            rotation = this.mWin.getDisplayContent().getRotation();
            if (rotation == 1) {
                pendingY = ((float) this.mScreenWidth) * (1.0f - ratio);
            } else if (rotation == 2) {
                pendingX = ((float) this.mScreenWidth) * (1.0f - ratio);
                pendingY = ((float) this.mScreenHeight) * (1.0f - ratio);
            } else if (rotation == 3) {
                pendingX = ((float) this.mScreenHeight) * (1.0f - ratio);
            }
        }
        if (type == 1) {
            ratio = this.mLazyScale;
            pendingX = 0.0f;
            pendingY = ((float) this.mScreenHeight) * (1.0f - this.mLazyScale);
        } else if (type == 2) {
            ratio = this.mLazyScale;
            float f = this.mLazyScale;
            pendingX = ((float) this.mScreenWidth) * (1.0f - f);
            pendingY = ((float) this.mScreenHeight) * (1.0f - f);
        }
        if (screenAnimation) {
            Rect frame = this.mWin.getFrameLw();
            float[] tmpFloats = this.mService.mTmpFloats;
            Matrix tmpMatrix = this.mWin.mTmpMatrix;
            if (screenRotationAnimation.isRotating()) {
                float w = (float) frame.width();
                float h = (float) frame.height();
                if (w < 1.0f || h < 1.0f) {
                    tmpMatrix.reset();
                } else {
                    tmpMatrix.setScale((2.0f / w) + 1.0f, (2.0f / h) + 1.0f, w / 2.0f, h / 2.0f);
                }
            } else {
                tmpMatrix.reset();
                float x = (((float) frame.left) * ratio) + pendingX;
                float y = (((float) frame.top) * ratio) + pendingY;
                if (HwFoldScreenState.isFoldScreenDevice() && this.mWin.toString().contains("DockedStackDivider")) {
                    if (rotation == 1 || rotation == 3) {
                        x -= 1.0f;
                    } else {
                        y -= 1.0f;
                    }
                }
                if (HwDisplaySizeUtil.hasSideInScreen()) {
                    if (type == 2) {
                        x -= (float) HwDisplaySizeUtil.getInstance(this.mService).getSafeSideWidth();
                    } else {
                        x += (float) HwDisplaySizeUtil.getInstance(this.mService).getSafeSideWidth();
                    }
                }
                this.mShownPosition.set((int) x, (int) y);
            }
            tmpMatrix.postScale(this.mWin.mGlobalScale, this.mWin.mGlobalScale);
            tmpMatrix.postTranslate((float) this.mWin.mAttrs.surfaceInsets.left, (float) this.mWin.mAttrs.surfaceInsets.top);
            this.mHaveMatrix = true;
            tmpMatrix.getValues(tmpFloats);
            this.mDsDx = tmpFloats[0] * ratio;
            this.mDtDx = tmpFloats[3] * ratio;
            this.mDtDy = tmpFloats[1] * ratio;
            this.mDsDy = tmpFloats[4] * ratio;
            scaleForMagicWindow();
            this.mShownAlpha = this.mAlpha;
            if ((!this.mService.mLimitedAlphaCompositing || !PixelFormat.formatHasAlpha(this.mWin.mAttrs.format) || this.mWin.isIdentityMatrix(this.mDsDx, this.mDtDx, this.mDtDy, this.mDsDy)) && screenAnimation) {
                this.mShownAlpha *= screenRotationAnimation.getEnterTransformation().getAlpha();
            }
        } else if ((!this.mIsWallpaper || !this.mService.mRoot.mWallpaperActionPending) && !this.mWin.isDragResizeChanged()) {
            float x2 = (((float) this.mWin.getFrameLw().left) * ratio) + pendingX;
            float y2 = (((float) this.mWin.getFrameLw().top) * ratio) + pendingY;
            if (HwDisplaySizeUtil.hasSideInScreen()) {
                if ((type == 0 && this.mService.mPolicy.isKeyguardLockedAndOccluded()) || type == 1) {
                    x2 += (float) HwDisplaySizeUtil.getInstance(this.mService).getSafeSideWidth();
                } else if (type == 2) {
                    x2 -= (float) HwDisplaySizeUtil.getInstance(this.mService).getSafeSideWidth();
                }
            }
            this.mShownPosition.set((int) x2, (int) y2);
            this.mShownAlpha = this.mAlpha;
            this.mHaveMatrix = false;
            this.mDsDx = this.mWin.mGlobalScale * ratio;
            this.mDtDx = 0.0f;
            this.mDtDy = 0.0f;
            this.mDsDy = this.mWin.mGlobalScale * ratio;
            scaleForMagicWindow();
        }
    }

    private void scaleForMagicWindow() {
        if (this.mWin.inHwMagicWindowingMode()) {
            DisplayMetrics dm = new DisplayMetrics();
            this.mWindowManager.getDefaultDisplay().getRealMetrics(dm);
            WindowContainer parentWindowContainer = this.mWin.getParent();
            Rect parentBounds = null;
            if (parentWindowContainer != null) {
                parentBounds = parentWindowContainer.getBounds();
            }
            if (this.mWin.getBounds().width() == dm.widthPixels && this.mWin.getBounds().height() == dm.heightPixels) {
                WindowState windowState = this.mWin;
                float f = this.mWin.mGlobalScale;
                windowState.mMwUsedScaleFactor = f;
                this.mDsDy = f;
                this.mDsDx = f;
            } else if (parentBounds == null || this.mWin.mToken.toString().contains("com.android.packageinstaller")) {
                WindowState windowState2 = this.mWin;
                float f2 = this.mWin.mGlobalScale;
                windowState2.mMwUsedScaleFactor = f2;
                this.mDsDy = f2;
                this.mDsDx = f2;
            } else {
                this.mWin.mMwUsedScaleFactor = this.mWin.mMwScaleRatioConfig;
                float f3 = this.mWin.mMwUsedScaleFactor * this.mWin.mGlobalScale;
                this.mDsDy = f3;
                this.mDsDx = f3;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void computeShownFrameLocked() {
        hwPrepareSurfaceLocked();
    }

    public void hwPrepareSurfaceLocked() {
        int currentLazyMode = this.mService.getLazyMode();
        if (!this.mIsDefaultDisplay) {
            currentLazyMode = 0;
        }
        int requestedOrientation = -1;
        if (this.mWin.mAppToken != null) {
            requestedOrientation = this.mWin.mAppToken.mOrientation;
        }
        if (this.mSkipScalingDownSurface) {
            computeShownFrameNormalLocked();
        } else if (isOrientationLandscape(requestedOrientation)) {
            computeShownFrameNormalLocked();
            this.mLastLazyMode = 0;
        } else {
            computeShownFrameLockedByLazyMode(currentLazyMode);
        }
        if (this.mWin.mAttrs.type == 2000 && this.mLastLazyMode != 0 && currentLazyMode != 0 && (this.mIsLeftLazyMode || this.mIsRightLazyMode)) {
            this.mDsDy += 2.0f / ((float) this.mWin.getFrameLw().height());
        }
        if (isMultiWindowInSingleHandMode()) {
            this.mWin.getDisplayContent().mDividerControllerLocked.adjustBoundsForSingleHand();
        }
        traceLogForLazyMode(currentLazyMode);
    }

    /* access modifiers changed from: package-private */
    public void computeShownFrameNormalLocked() {
        computeShownFrameLocked(0);
    }

    /* access modifiers changed from: package-private */
    public void computeShownFrameRightLocked() {
        computeShownFrameLocked(2);
    }

    /* access modifiers changed from: package-private */
    public void computeShownFrameLeftLocked() {
        computeShownFrameLocked(1);
    }

    private void computeShownFrameLockedByLazyMode(int currentLazyMode) {
        if (currentLazyMode == 0 && this.mLastLazyMode == 1) {
            this.mLastLazyMode = 0;
            this.mLazyIsExiting = true;
            this.mLazyIsEntering = false;
            this.mAnimator.offsetLayer = 0;
            this.mLazyScale = 0.8f;
            computeShownFrameLeftLocked();
        } else if (currentLazyMode == 0 && this.mLastLazyMode == 2) {
            this.mLastLazyMode = 0;
            this.mLazyIsExiting = true;
            this.mLazyIsEntering = false;
            this.mLazyScale = 0.8f;
            this.mAnimator.offsetLayer = 0;
            computeShownFrameRightLocked();
        } else if (currentLazyMode == 0 && this.mLazyIsExiting && this.mLastLazyMode == 0) {
            if (this.mIsLeftLazyMode) {
                setLeftLazyScale();
            } else if (this.mIsRightLazyMode) {
                setRightLazyScale();
            } else {
                this.mLazyScale = 1.0f;
                this.mLazyIsExiting = false;
                this.mLazyIsEntering = false;
                this.mIsRightLazyMode = false;
                this.mIsLeftLazyMode = false;
                this.mLastLazyMode = 0;
                computeShownFrameNormalLocked();
            }
        } else if (currentLazyMode == 1) {
            handleLeftScale();
        } else if (currentLazyMode == 2) {
            handleRightScale();
        } else {
            computeShownFrameNormalLocked();
        }
    }

    private void setLeftLazyScale() {
        if (floatEqualCompare(0.8f)) {
            this.mLazyScale = 0.85f;
            computeShownFrameLeftLocked();
        } else if (floatEqualCompare(0.85f)) {
            this.mLazyScale = 0.9f;
            computeShownFrameLeftLocked();
        } else if (floatEqualCompare(0.9f)) {
            this.mLazyScale = 0.95f;
            computeShownFrameLeftLocked();
        } else if (floatEqualCompare(0.95f)) {
            this.mLazyScale = 1.0f;
            this.mLazyIsExiting = false;
            this.mIsLeftLazyMode = false;
            computeShownFrameNormalLocked();
        }
    }

    private void setRightLazyScale() {
        if (floatEqualCompare(0.8f)) {
            this.mLazyScale = 0.85f;
            computeShownFrameRightLocked();
        } else if (floatEqualCompare(0.85f)) {
            this.mLazyScale = 0.9f;
            computeShownFrameRightLocked();
        } else if (floatEqualCompare(0.9f)) {
            this.mLazyScale = 0.95f;
            computeShownFrameRightLocked();
        } else if (floatEqualCompare(0.95f)) {
            this.mLazyScale = 1.0f;
            this.mLazyIsExiting = false;
            this.mIsRightLazyMode = false;
            computeShownFrameNormalLocked();
        }
    }

    private void handleLeftScale() {
        int i = this.mLastLazyMode;
        if (i == 0) {
            this.mLastLazyMode = 1;
            this.mLazyIsEntering = true;
            this.mLazyIsExiting = false;
            this.mLazyScale = 0.95f;
            computeShownFrameLeftLocked();
        } else if (i != 1 || !this.mLazyIsEntering) {
            this.mLazyScale = 0.75f;
            this.mIsLeftLazyMode = true;
            this.mAnimator.offsetLayer = 800000;
            computeShownFrameLeftLocked();
        } else {
            if (floatEqualCompare(0.95f)) {
                this.mLazyScale = 0.9f;
            } else if (floatEqualCompare(0.9f)) {
                this.mLazyScale = 0.85f;
            } else if (floatEqualCompare(0.85f)) {
                this.mLazyScale = 0.8f;
            } else if (floatEqualCompare(0.8f)) {
                this.mLazyScale = 0.75f;
                this.mLazyIsEntering = false;
                this.mIsLeftLazyMode = true;
            }
            computeShownFrameLeftLocked();
        }
    }

    private void handleRightScale() {
        int i = this.mLastLazyMode;
        if (i == 0) {
            this.mLastLazyMode = 2;
            this.mLazyIsEntering = true;
            this.mLazyIsExiting = false;
            this.mLazyScale = 0.95f;
            computeShownFrameRightLocked();
        } else if (i != 2 || !this.mLazyIsEntering) {
            this.mLazyScale = 0.75f;
            this.mIsRightLazyMode = true;
            this.mAnimator.offsetLayer = 800000;
            computeShownFrameRightLocked();
        } else {
            if (floatEqualCompare(0.95f)) {
                this.mLazyScale = 0.9f;
            } else if (floatEqualCompare(0.9f)) {
                this.mLazyScale = 0.85f;
            } else if (floatEqualCompare(0.85f)) {
                this.mLazyScale = 0.8f;
            } else if (floatEqualCompare(0.8f)) {
                this.mLazyScale = 0.75f;
                this.mLazyIsEntering = false;
                this.mIsRightLazyMode = true;
            }
            computeShownFrameRightLocked();
        }
    }

    private boolean floatEqualCompare(float f) {
        return ((double) Math.abs(this.mLazyScale - f)) < 1.0E-6d;
    }

    private boolean isOrientationLandscape(int requestedOrientation) {
        return requestedOrientation == 0 || requestedOrientation == 6 || requestedOrientation == 8 || requestedOrientation == 11;
    }

    private boolean isMultiWindowInSingleHandMode() {
        return (this.mWin.mAttrs.type == 2034 && this.mLazyIsEntering && floatEqualCompare(0.8f)) || (this.mLazyIsExiting && floatEqualCompare(0.95f));
    }

    private void traceLogForLazyMode(int currentLazyMode) {
    }

    private boolean ignoreParentClipRect(WindowManager.LayoutParams lp) {
        return (lp.privateFlags & 1073741824) != 0;
    }

    /* access modifiers changed from: package-private */
    public WindowSurfaceController createSurfaceLocked(int windowType, int ownerUid) {
        WindowSurfaceController surfaceController = HwWindowStateAnimator.super.createSurfaceLocked(windowType, ownerUid);
        sendMessageToDESceneHandler(1);
        return surfaceController;
    }

    /* access modifiers changed from: package-private */
    public void destroySurfaceLocked() {
        sendMessageToDESceneHandler(-1);
        HwWindowStateAnimator.super.destroySurfaceLocked();
    }

    private void sendMessageToDESceneHandler(int pos) {
        WindowState ws = this.mWin;
        WindowManager.LayoutParams attrs = ws.mAttrs;
        WindowManagerService service = ws.mWmService;
        String SurName = attrs.getTitle().toString();
        DisplayContent displayContent = service.getDefaultDisplayContentLocked();
        int initScreenWidth = displayContent.mInitialDisplayWidth;
        int initScreenHeight = displayContent.mInitialDisplayHeight;
        Bundle data = new Bundle();
        data.putInt("Position", pos);
        data.putString("SurfaceName", SurName);
        data.putInt("FrameLeft", ws.getFrameLw().left);
        data.putInt("FrameRight", ws.getFrameLw().right);
        data.putInt("FrameTop", ws.getFrameLw().top);
        data.putInt("FrameBottom", ws.getFrameLw().bottom);
        data.putInt("SourceWidth", ws.mRequestedWidth);
        data.putInt("SourceHeight", ws.mRequestedHeight);
        data.putInt("DisplayWidth", initScreenWidth);
        data.putInt("DisplayHeight", initScreenHeight);
        data.putInt("Layer", ws.mLayer);
        data.putInt("BaseLayer", ws.mBaseLayer);
        data.putInt("SubLayer", ws.mSubLayer);
        data.putInt("SurfaceFormat", this.mSurfaceFormat);
        data.putString("AttachWinName", null);
        this.mDisplayEngineManager.sendMessage(2, data);
    }

    private int checkWindowType(WindowState win) {
        if (24 == win.mAppOp) {
            return 1;
        }
        if (2005 == win.getAttrs().type) {
            return 2;
        }
        return -1;
    }

    public boolean isEvilWindow(WindowState win) {
        if (win == null) {
            return false;
        }
        return AwareAppFreezeMng.getInstance().isEvilWindow(win.mSession.mPid, System.identityHashCode(win), checkWindowType(win));
    }
}
