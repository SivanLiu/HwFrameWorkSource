package com.android.server.wm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.HwFoldScreenState;
import android.os.Trace;
import android.util.CoordinationModeUtils;
import android.util.Flog;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import java.io.PrintWriter;
import java.lang.annotation.RCUnownedRef;

public class WindowStateAnimator {
    static final int COMMIT_DRAW_PENDING = 2;
    static final int DRAW_PENDING = 1;
    static final int HAS_DRAWN = 4;
    static final int NO_SURFACE = 0;
    static final int PRESERVED_SURFACE_LAYER = 1;
    static final int READY_TO_SHOW = 3;
    static final int STACK_CLIP_AFTER_ANIM = 0;
    static final int STACK_CLIP_BEFORE_ANIM = 1;
    static final int STACK_CLIP_NONE = 2;
    static final String TAG = "WindowManager";
    static final int WINDOW_FREEZE_LAYER = 2000000;
    float mAlpha = 0.0f;
    boolean mAnimationIsEntrance;
    final WindowAnimator mAnimator;
    int mAttrType;
    boolean mChildrenDetached = false;
    final Context mContext;
    private boolean mDestroyPreservedSurfaceUponRedraw;
    int mDrawState;
    float mDsDx = 1.0f;
    float mDsDy = 0.0f;
    float mDtDx = 0.0f;
    float mDtDy = 1.0f;
    boolean mEnterAnimationPending;
    boolean mEnteringAnimation;
    float mExtraHScale = 1.0f;
    float mExtraVScale = 1.0f;
    boolean mForceScaleUntilResize;
    boolean mHaveMatrix;
    public final Object mInsetSurfaceLock = new Object();
    final boolean mIsWallpaper;
    float mLastAlpha = 0.0f;
    Rect mLastClipRect = new Rect();
    private float mLastDsDx = 1.0f;
    private float mLastDsDy = 0.0f;
    private float mLastDtDx = 0.0f;
    private float mLastDtDy = 1.0f;
    Rect mLastFinalClipRect = new Rect();
    boolean mLastHidden;
    boolean mLazyIsEntering;
    boolean mLazyIsExiting;
    private boolean mOffsetPositionForStackResize;
    private WindowSurfaceController mPendingDestroySurface;
    boolean mPipAnimationStarted = false;
    final WindowManagerPolicy mPolicy;
    private final SurfaceControl.Transaction mReparentTransaction = new SurfaceControl.Transaction();
    boolean mReportSurfaceResized;
    final WindowManagerService mService;
    final Session mSession;
    float mShownAlpha = 0.0f;
    protected Point mShownPosition = new Point();
    public SplitScreenSideSurfaceBox mSplitScreenSideSurfaceBox;
    WindowSurfaceController mSurfaceController;
    boolean mSurfaceDestroyDeferred;
    int mSurfaceFormat;
    boolean mSurfaceResized;
    private final Rect mSystemDecorRect = new Rect();
    private Rect mTmpAnimatingBounds = new Rect();
    Rect mTmpClipRect = new Rect();
    private final Point mTmpPos = new Point();
    private final Rect mTmpSize = new Rect();
    private Rect mTmpSourceBounds = new Rect();
    Rect mTmpStackBounds = new Rect();
    private final SurfaceControl.Transaction mTmpTransaction = new SurfaceControl.Transaction();
    private final WallpaperController mWallpaperControllerLocked;
    @RCUnownedRef
    final WindowState mWin;
    int mXOffset = 0;
    int mYOffset = 0;

    /* access modifiers changed from: package-private */
    public String drawStateToString() {
        int i = this.mDrawState;
        if (i == 0) {
            return "NO_SURFACE";
        }
        if (i == 1) {
            return "DRAW_PENDING";
        }
        if (i == 2) {
            return "COMMIT_DRAW_PENDING";
        }
        if (i == 3) {
            return "READY_TO_SHOW";
        }
        if (i != 4) {
            return Integer.toString(i);
        }
        return "HAS_DRAWN";
    }

    WindowStateAnimator(WindowState win) {
        WindowManagerService service = win.mWmService;
        this.mService = service;
        this.mAnimator = service.mAnimator;
        this.mPolicy = service.mPolicy;
        this.mContext = service.mContext;
        this.mWin = win;
        this.mSession = win.mSession;
        this.mAttrType = win.mAttrs.type;
        this.mIsWallpaper = win.mIsWallpaper;
        this.mWallpaperControllerLocked = win.getDisplayContent().mWallpaperController;
    }

    /* access modifiers changed from: package-private */
    public void onAnimationFinished() {
        this.mWin.checkPolicyVisibilityChange();
        DisplayContent displayContent = this.mWin.getDisplayContent();
        if (this.mAttrType == 2000 && this.mWin.isVisibleByPolicy() && displayContent != null) {
            displayContent.setLayoutNeeded();
        }
        this.mWin.onExitAnimationDone();
        int displayId = this.mWin.getDisplayId();
        int pendingLayoutChanges = 8;
        if (displayContent.mWallpaperController.isWallpaperTarget(this.mWin)) {
            pendingLayoutChanges = 8 | 4;
        }
        this.mAnimator.setPendingLayoutChanges(displayId, pendingLayoutChanges);
        if (this.mWin.mAppToken != null) {
            this.mWin.mAppToken.updateReportedVisibilityLocked();
        }
    }

    /* access modifiers changed from: package-private */
    public void hide(SurfaceControl.Transaction transaction, String reason) {
        if (!this.mLastHidden) {
            this.mLastHidden = true;
            Flog.i(307, "hide " + this + " reason " + reason);
            markPreservedSurfaceForDestroy();
            WindowSurfaceController windowSurfaceController = this.mSurfaceController;
            if (windowSurfaceController != null) {
                windowSurfaceController.hide(transaction, reason);
            }
            WindowState windowState = this.mWin;
            if (windowState != null && windowState.getParentWindow() != null && !this.mWin.mAnimatingExit && this.mWin.isFocused()) {
                this.mService.updateFocusedWindowLocked(0, false);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void hide(String reason) {
        hide(this.mTmpTransaction, reason);
        SurfaceControl.mergeToGlobalTransaction(this.mTmpTransaction);
    }

    /* access modifiers changed from: package-private */
    public boolean finishDrawingLocked() {
        boolean startingWindow = this.mWin.mAttrs.type == 3;
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && startingWindow) {
            Slog.v(TAG, "Finishing drawing window " + this.mWin + ": mDrawState=" + drawStateToString());
        }
        if (this.mDrawState != 1) {
            return false;
        }
        printDrawStateChanged("DRAW_PENDING", "COMMIT_DRAW_PENDING", "finishDrawing");
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW && startingWindow) {
            Slog.v(TAG, "Draw state now committed in " + this.mWin);
        }
        this.mDrawState = 2;
        return true;
    }

    /* access modifiers changed from: package-private */
    public void printDrawStateChanged(String oldState, String newState, String reason) {
        Flog.i(307, "DrawState change: " + this.mWin + " from:" + oldState + " to:" + newState + " reason:" + reason);
    }

    /* access modifiers changed from: package-private */
    public boolean commitFinishDrawingLocked() {
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE && this.mWin.mAttrs.type == 3) {
            Slog.i(TAG, "commitFinishDrawingLocked: " + this.mWin + " cur mDrawState=" + drawStateToString());
        }
        int i = this.mDrawState;
        if (i != 2 && i != 3) {
            return false;
        }
        if (this.mDrawState == 2) {
            printDrawStateChanged("COMMIT_DRAW_PENDING", "READY_TO_SHOW", "commitFinishDrawing");
        }
        this.mDrawState = 3;
        AppWindowToken atoken = this.mWin.mAppToken;
        if (atoken == null || atoken.canShowWindows() || this.mWin.mAttrs.type == 3) {
            return this.mWin.performShowLocked();
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void preserveSurfaceLocked() {
        WindowSurfaceController windowSurfaceController;
        if (this.mDestroyPreservedSurfaceUponRedraw) {
            this.mSurfaceDestroyDeferred = false;
            WindowSurfaceController windowSurfaceController2 = this.mPendingDestroySurface;
            if (!(windowSurfaceController2 == null || windowSurfaceController2.mSurfaceControl == null || (windowSurfaceController = this.mSurfaceController) == null || windowSurfaceController.mSurfaceControl == null || !this.mSurfaceController.getShown())) {
                this.mReparentTransaction.reparentChildren(this.mSurfaceController.mSurfaceControl, this.mPendingDestroySurface.mSurfaceControl.getHandle()).apply();
            }
            destroySurfaceLocked();
            this.mSurfaceDestroyDeferred = true;
        } else if (this.mWin == null || !HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(this.mWin.getDisplayId()) || this.mSurfaceController != null) {
            WindowSurfaceController windowSurfaceController3 = this.mSurfaceController;
            if (windowSurfaceController3 != null) {
                windowSurfaceController3.mSurfaceControl.setLayer(1);
            }
            this.mDestroyPreservedSurfaceUponRedraw = true;
            this.mSurfaceDestroyDeferred = true;
            destroySurfaceLocked();
        } else {
            HwPCUtils.log(TAG, "surface is null");
        }
    }

    /* access modifiers changed from: package-private */
    public void destroyPreservedSurfaceLocked() {
        if (this.mDestroyPreservedSurfaceUponRedraw) {
            if (!(this.mSurfaceController == null || this.mPendingDestroySurface == null || (this.mWin.mAppToken != null && this.mWin.mAppToken.isRelaunching()))) {
                this.mReparentTransaction.reparentChildren(this.mPendingDestroySurface.mSurfaceControl, this.mSurfaceController.mSurfaceControl.getHandle()).apply();
            }
            destroyDeferredSurfaceLocked();
            this.mDestroyPreservedSurfaceUponRedraw = false;
        }
    }

    /* access modifiers changed from: package-private */
    public void markPreservedSurfaceForDestroy() {
        if (this.mDestroyPreservedSurfaceUponRedraw && !this.mService.mDestroyPreservedSurface.contains(this.mWin)) {
            this.mService.mDestroyPreservedSurface.add(this.mWin);
        }
    }

    private int getLayerStack() {
        return this.mWin.getDisplayContent().getDisplay().getLayerStack();
    }

    /* access modifiers changed from: package-private */
    public void resetDrawState() {
        this.mDrawState = 1;
        if (this.mWin.mAppToken != null) {
            if (!this.mWin.mAppToken.isSelfAnimating()) {
                this.mWin.mAppToken.clearAllDrawn();
            } else {
                this.mWin.mAppToken.deferClearAllDrawn = true;
            }
        }
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x01a8, code lost:
        if ("DockedStackDivider".equals(r23.mWin.getAttrs().getTitle()) == false) goto L_0x01b9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:0x01aa, code lost:
        r23.mWin.getDisplayContent().getDockedDividerController().setDockedStackDividerWindow(r23.mWin);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x01b9, code lost:
        setOffsetPositionForStackResize(false);
        r23.mSurfaceFormat = r0;
        r3 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:?, code lost:
        r12.setHasSurface(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x01c4, code lost:
        r23.mLastHidden = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x01c9, code lost:
        return r23.mSurfaceController;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:89:0x01d4, code lost:
        r0 = e;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x015c A[Catch:{ OutOfResourcesException -> 0x01d6, Exception -> 0x01d4 }] */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x015e A[Catch:{ OutOfResourcesException -> 0x01d6, Exception -> 0x01d4 }] */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x0165 A[SYNTHETIC, Splitter:B:56:0x0165] */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x01d4 A[ExcHandler: Exception (e java.lang.Exception), Splitter:B:49:0x014f] */
    public WindowSurfaceController createSurfaceLocked(int windowType, int ownerUid) {
        int windowType2;
        int flags;
        boolean z;
        int flags2;
        boolean needsSplitScreenSideSurfaceBox;
        WindowState w = this.mWin;
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            return windowSurfaceController;
        }
        this.mChildrenDetached = false;
        if ((this.mWin.mAttrs.privateFlags & 1048576) != 0) {
            windowType2 = 441731;
        } else {
            windowType2 = windowType;
        }
        w.setHasSurface(false);
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.i(TAG, "createSurface " + this + ": mDrawState=DRAW_PENDING");
        }
        printDrawStateChanged(drawStateToString(), "DRAW_PENDING", "createSurface");
        resetDrawState();
        this.mService.makeWindowFreezingScreenIfNeededLocked(w);
        WindowManager.LayoutParams attrs = w.mAttrs;
        if (this.mService.isSecureLocked(w)) {
            flags = 4 | 128;
        } else {
            flags = 4;
        }
        calculateSurfaceBounds(w, attrs, this.mTmpSize);
        int width = this.mTmpSize.width();
        int height = this.mTmpSize.height();
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG, "Creating surface in session " + this.mSession.mSurfaceSession + " window " + this + " w=" + width + " h=" + height + " x=" + this.mTmpSize.left + " y=" + this.mTmpSize.top + " format=" + attrs.format + " flags=" + flags);
        }
        this.mLastClipRect.set(0, 0, 0, 0);
        try {
            int format = (attrs.flags & 16777216) != 0 ? -3 : attrs.format;
            if (!PixelFormat.formatHasAlpha(attrs.format)) {
                try {
                    if (attrs.surfaceInsets.left == 0 && attrs.surfaceInsets.top == 0 && attrs.surfaceInsets.right == 0 && attrs.surfaceInsets.bottom == 0 && !w.isDragResizing()) {
                        flags2 = flags | 1024;
                        try {
                            this.mSurfaceController = new WindowSurfaceController(this.mSession.mSurfaceSession, attrs.getTitle().toString(), width, height, format, flags2, this, windowType2, ownerUid);
                            this.mSurfaceController.setColorSpaceAgnostic((attrs.privateFlags & 16777216) == 0);
                            synchronized (this.mInsetSurfaceLock) {
                                try {
                                    if (this.mWin.getAttrs().type != 2034) {
                                        try {
                                            if (!this.mIsWallpaper) {
                                                needsSplitScreenSideSurfaceBox = false;
                                                if (HwDisplaySizeUtil.hasSideInScreen() && needsSplitScreenSideSurfaceBox && this.mSplitScreenSideSurfaceBox == null) {
                                                    this.mSplitScreenSideSurfaceBox = new SplitScreenSideSurfaceBox(this.mService.getDefaultDisplayContentLocked());
                                                }
                                            }
                                        } catch (Throwable th) {
                                            th = th;
                                            while (true) {
                                                try {
                                                    break;
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                }
                                            }
                                            throw th;
                                        }
                                    }
                                    needsSplitScreenSideSurfaceBox = true;
                                    this.mSplitScreenSideSurfaceBox = new SplitScreenSideSurfaceBox(this.mService.getDefaultDisplayContentLocked());
                                } catch (Throwable th3) {
                                    th = th3;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            }
                        } catch (Surface.OutOfResourcesException e) {
                            z = true;
                            Slog.w(TAG, "OutOfResourcesException creating surface");
                            this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", z);
                            this.mDrawState = 0;
                            return null;
                        } catch (Exception e2) {
                        }
                    }
                } catch (Surface.OutOfResourcesException e3) {
                    z = true;
                    Slog.w(TAG, "OutOfResourcesException creating surface");
                    this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", z);
                    this.mDrawState = 0;
                    return null;
                } catch (Exception e4) {
                    e = e4;
                    Slog.e(TAG, "Exception creating surface (parent dead?)", e);
                    this.mDrawState = 0;
                    return null;
                }
            }
            flags2 = flags;
            try {
                this.mSurfaceController = new WindowSurfaceController(this.mSession.mSurfaceSession, attrs.getTitle().toString(), width, height, format, flags2, this, windowType2, ownerUid);
                this.mSurfaceController.setColorSpaceAgnostic((attrs.privateFlags & 16777216) == 0);
                synchronized (this.mInsetSurfaceLock) {
                }
            } catch (Surface.OutOfResourcesException e5) {
                z = true;
                Slog.w(TAG, "OutOfResourcesException creating surface");
                this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", z);
                this.mDrawState = 0;
                return null;
            } catch (Exception e6) {
                e = e6;
                Slog.e(TAG, "Exception creating surface (parent dead?)", e);
                this.mDrawState = 0;
                return null;
            }
        } catch (Surface.OutOfResourcesException e7) {
            z = true;
            Slog.w(TAG, "OutOfResourcesException creating surface");
            this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", z);
            this.mDrawState = 0;
            return null;
        } catch (Exception e8) {
            e = e8;
            Slog.e(TAG, "Exception creating surface (parent dead?)", e);
            this.mDrawState = 0;
            return null;
        }
    }

    private void calculateSurfaceBounds(WindowState w, WindowManager.LayoutParams attrs, Rect outSize) {
        outSize.setEmpty();
        if ((attrs.flags & 16384) != 0) {
            outSize.right = w.mRequestedWidth;
            outSize.bottom = w.mRequestedHeight;
        } else if (w.isDragResizing()) {
            DisplayInfo displayInfo = w.getDisplayInfo();
            outSize.right = displayInfo.logicalWidth;
            outSize.bottom = displayInfo.logicalHeight;
        } else {
            w.getCompatFrameSize(outSize);
        }
        if (outSize.width() < 1) {
            outSize.right = 1;
        }
        if (outSize.height() < 1) {
            outSize.bottom = 1;
        }
        outSize.inset(-attrs.surfaceInsets.left, -attrs.surfaceInsets.top, -attrs.surfaceInsets.right, -attrs.surfaceInsets.bottom);
    }

    /* access modifiers changed from: package-private */
    public boolean hasSurface() {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        return windowSurfaceController != null && windowSurfaceController.hasSurface();
    }

    /* access modifiers changed from: package-private */
    public void destroySurfaceLocked() {
        AppWindowToken wtoken = this.mWin.mAppToken;
        if (wtoken != null && this.mWin == wtoken.startingWindow) {
            wtoken.startingDisplayed = false;
        }
        if (this.mSurfaceController != null) {
            if (!this.mDestroyPreservedSurfaceUponRedraw) {
                this.mWin.mHidden = true;
            }
            try {
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                    WindowManagerService.logWithStack(TAG, "Window " + this + " destroying surface " + this.mSurfaceController + ", session " + this.mSession);
                }
                if (!this.mSurfaceDestroyDeferred) {
                    destroySurface();
                } else if (!(this.mSurfaceController == null || this.mPendingDestroySurface == this.mSurfaceController)) {
                    if (this.mPendingDestroySurface != null) {
                        this.mPendingDestroySurface.destroyNotInTransaction();
                    }
                    this.mPendingDestroySurface = this.mSurfaceController;
                }
                if (!this.mDestroyPreservedSurfaceUponRedraw) {
                    this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
                }
            } catch (RuntimeException e) {
                Slog.w(TAG, "Exception thrown when destroying Window " + this + " surface " + this.mSurfaceController + " session " + this.mSession + ": " + e.toString());
            }
            this.mWin.setHasSurface(false);
            WindowSurfaceController windowSurfaceController = this.mSurfaceController;
            if (windowSurfaceController != null) {
                windowSurfaceController.setShown(false);
            }
            this.mSurfaceController = null;
            this.mDrawState = 0;
        }
    }

    /* access modifiers changed from: package-private */
    public void destroyDeferredSurfaceLocked() {
        try {
            if (this.mPendingDestroySurface != null) {
                this.mPendingDestroySurface.destroyNotInTransaction();
                if (!this.mDestroyPreservedSurfaceUponRedraw) {
                    this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
                }
            }
        } catch (RuntimeException e) {
            Slog.w(TAG, "Exception thrown when destroying Window " + this + " surface " + this.mPendingDestroySurface + " session " + this.mSession + ": " + e.toString());
        }
        this.mSurfaceDestroyDeferred = false;
        this.mPendingDestroySurface = null;
    }

    /* access modifiers changed from: package-private */
    public void computeShownFrameLocked() {
        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(this.mWin.getDisplayId());
        boolean screenAnimation = screenRotationAnimation != null && screenRotationAnimation.isAnimating() && (this.mWin.mForceSeamlesslyRotate ^ true);
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
            }
            tmpMatrix.postScale(this.mWin.mGlobalScale, this.mWin.mGlobalScale);
            tmpMatrix.postTranslate((float) this.mWin.mAttrs.surfaceInsets.left, (float) this.mWin.mAttrs.surfaceInsets.top);
            this.mHaveMatrix = true;
            tmpMatrix.getValues(tmpFloats);
            this.mDsDx = tmpFloats[0];
            this.mDtDx = tmpFloats[3];
            this.mDtDy = tmpFloats[1];
            this.mDsDy = tmpFloats[4];
            this.mShownAlpha = this.mAlpha;
            if ((!this.mService.mLimitedAlphaCompositing || !PixelFormat.formatHasAlpha(this.mWin.mAttrs.format) || this.mWin.isIdentityMatrix(this.mDsDx, this.mDtDx, this.mDtDy, this.mDsDy)) && screenAnimation) {
                this.mShownAlpha *= screenRotationAnimation.getEnterTransformation().getAlpha();
            }
        } else if ((!this.mIsWallpaper || !this.mService.mRoot.mWallpaperActionPending) && !this.mWin.isDragResizeChanged()) {
            this.mShownAlpha = this.mAlpha;
            this.mHaveMatrix = false;
            this.mDsDx = this.mWin.mGlobalScale;
            this.mDtDx = 0.0f;
            this.mDtDy = 0.0f;
            this.mDsDy = this.mWin.mGlobalScale;
        }
    }

    private boolean calculateCrop(Rect clipRect) {
        WindowState w = this.mWin;
        DisplayContent displayContent = w.getDisplayContent();
        clipRect.setEmpty();
        if (displayContent == null || w.getWindowConfiguration().tasksAreFloating() || ((w.getWindowConfiguration().inHwSplitScreenWindowingMode() && w.getStack() != null && w.getStack().isAdjustedForIme()) || w.mForceSeamlesslyRotate)) {
            return false;
        }
        if (w.mAttrs.type == 2013 && (!HwDisplaySizeUtil.hasSideInScreen() || (displayContent.getRotation() != 1 && displayContent.getRotation() != 3))) {
            return false;
        }
        w.calculatePolicyCrop(this.mSystemDecorRect);
        clipRect.set(this.mSystemDecorRect);
        w.expandForSurfaceInsets(clipRect);
        clipRect.offset(w.mAttrs.surfaceInsets.left, w.mAttrs.surfaceInsets.top);
        w.transformClipRectFromScreenToSurfaceSpace(clipRect);
        return true;
    }

    private void applyCrop(Rect clipRect, boolean recoveringMemory) {
        if (clipRect == null) {
            if (HwDisplaySizeUtil.hasSideInScreen()) {
                this.mLastClipRect.set(0, 0, -1, -1);
            }
            this.mSurfaceController.clearCropInTransaction(recoveringMemory);
        } else if (!clipRect.equals(this.mLastClipRect)) {
            this.mLastClipRect.set(clipRect);
            this.mSurfaceController.setCropInTransaction(clipRect, recoveringMemory);
        }
    }

    /* access modifiers changed from: package-private */
    public void setSurfaceBoundariesLocked(boolean recoveringMemory) {
        boolean wasForceScaled;
        Rect clipRect;
        Rect clipRect2;
        boolean allowStretching;
        Rect clipRect3;
        if (this.mSurfaceController != null) {
            WindowState w = this.mWin;
            WindowManager.LayoutParams attrs = this.mWin.getAttrs();
            Task task = w.getTask();
            calculateSurfaceBounds(w, attrs, this.mTmpSize);
            this.mExtraHScale = 1.0f;
            this.mExtraVScale = 1.0f;
            boolean wasForceScaled2 = this.mForceScaleUntilResize;
            boolean relayout = !w.mRelayoutCalled || w.mInRelayout;
            if (relayout) {
                this.mSurfaceResized = this.mSurfaceController.setBufferSizeInTransaction(this.mTmpSize.width(), this.mTmpSize.height(), recoveringMemory);
            } else {
                this.mSurfaceResized = false;
            }
            this.mForceScaleUntilResize = this.mForceScaleUntilResize && !this.mSurfaceResized;
            Rect clipRect4 = null;
            if (calculateCrop(this.mTmpClipRect)) {
                clipRect4 = this.mTmpClipRect;
            }
            float surfaceWidth = (float) this.mSurfaceController.getWidth();
            float surfaceHeight = (float) this.mSurfaceController.getHeight();
            Rect insets = attrs.surfaceInsets;
            if (isForceScaled()) {
                float surfaceContentWidth = surfaceWidth - ((float) (insets.left + insets.right));
                float surfaceContentHeight = surfaceHeight - ((float) (insets.top + insets.bottom));
                if (!this.mForceScaleUntilResize) {
                    this.mSurfaceController.forceScaleableInTransaction(true);
                }
                int posX = 0;
                int posY = 0;
                wasForceScaled = wasForceScaled2;
                task.mStack.getDimBounds(this.mTmpStackBounds);
                task.mStack.getFinalAnimationSourceHintBounds(this.mTmpSourceBounds);
                if (!this.mTmpSourceBounds.isEmpty() || ((this.mWin.mLastRelayoutContentInsets.width() <= 0 && this.mWin.mLastRelayoutContentInsets.height() <= 0) || task.mStack.lastAnimatingBoundsWasToFullscreen())) {
                    allowStretching = false;
                } else {
                    this.mTmpSourceBounds.set(task.mStack.mPreAnimationBounds);
                    this.mTmpSourceBounds.inset(this.mWin.mLastRelayoutContentInsets);
                    allowStretching = true;
                }
                this.mTmpStackBounds.intersectUnchecked(w.getParentFrame());
                this.mTmpSourceBounds.intersectUnchecked(w.getParentFrame());
                this.mTmpAnimatingBounds.intersectUnchecked(w.getParentFrame());
                if (!this.mTmpSourceBounds.isEmpty()) {
                    task.mStack.getFinalAnimationBounds(this.mTmpAnimatingBounds);
                    float initialWidth = (float) this.mTmpSourceBounds.width();
                    float tw = (surfaceContentWidth - ((float) this.mTmpStackBounds.width())) / (surfaceContentWidth - ((float) this.mTmpAnimatingBounds.width()));
                    float th = tw;
                    this.mExtraHScale = (initialWidth + ((((float) this.mTmpAnimatingBounds.width()) - initialWidth) * tw)) / initialWidth;
                    if (allowStretching) {
                        float initialHeight = (float) this.mTmpSourceBounds.height();
                        th = (surfaceContentHeight - ((float) this.mTmpStackBounds.height())) / (surfaceContentHeight - ((float) this.mTmpAnimatingBounds.height()));
                        this.mExtraVScale = (((((float) this.mTmpAnimatingBounds.height()) - initialHeight) * tw) + initialHeight) / initialHeight;
                    } else {
                        this.mExtraVScale = this.mExtraHScale;
                    }
                    int posX2 = 0 - ((int) ((this.mExtraHScale * tw) * ((float) this.mTmpSourceBounds.left)));
                    int posY2 = 0 - ((int) ((this.mExtraVScale * th) * ((float) this.mTmpSourceBounds.top)));
                    clipRect3 = this.mTmpClipRect;
                    clipRect3.set((int) (((float) (insets.left + this.mTmpSourceBounds.left)) * tw), (int) (((float) (insets.top + this.mTmpSourceBounds.top)) * th), insets.left + ((int) (surfaceWidth - ((surfaceWidth - ((float) this.mTmpSourceBounds.right)) * tw))), insets.top + ((int) (surfaceHeight - ((surfaceHeight - ((float) this.mTmpSourceBounds.bottom)) * th))));
                    posX = posX2;
                    posY = posY2;
                } else {
                    this.mExtraHScale = ((float) this.mTmpStackBounds.width()) / surfaceContentWidth;
                    this.mExtraVScale = ((float) this.mTmpStackBounds.height()) / surfaceContentHeight;
                    clipRect3 = null;
                }
                clipRect = clipRect3;
                this.mSurfaceController.setPositionInTransaction((float) Math.floor((double) ((int) (((float) (posX - ((int) (((float) attrs.x) * (1.0f - this.mExtraHScale))))) + (((float) insets.left) * (1.0f - this.mExtraHScale))))), (float) Math.floor((double) ((int) (((float) (posY - ((int) (((float) attrs.y) * (1.0f - this.mExtraVScale))))) + (((float) insets.top) * (1.0f - this.mExtraVScale))))), recoveringMemory);
                if (!this.mPipAnimationStarted) {
                    this.mForceScaleUntilResize = true;
                    this.mPipAnimationStarted = true;
                }
            } else {
                wasForceScaled = wasForceScaled2;
                this.mPipAnimationStarted = false;
                if (!w.mSeamlesslyRotated) {
                    int xOffset = this.mXOffset;
                    int yOffset = this.mYOffset;
                    if (!this.mOffsetPositionForStackResize) {
                        clipRect2 = clipRect4;
                    } else if (relayout) {
                        setOffsetPositionForStackResize(false);
                        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
                        windowSurfaceController.deferTransactionUntil(windowSurfaceController.getHandle(), this.mWin.getFrameNumber());
                        clipRect2 = clipRect4;
                    } else {
                        TaskStack stack = this.mWin.getStack();
                        Point point = this.mTmpPos;
                        point.x = 0;
                        point.y = 0;
                        if (stack != null) {
                            stack.getRelativeDisplayedPosition(point);
                        }
                        xOffset = -this.mTmpPos.x;
                        yOffset = -this.mTmpPos.y;
                        if (clipRect4 != null) {
                            clipRect2 = clipRect4;
                            clipRect2.right += this.mTmpPos.x;
                            clipRect2.bottom += this.mTmpPos.y;
                        } else {
                            clipRect2 = clipRect4;
                        }
                    }
                    int lazyMode = this.mService.getLazyMode();
                    int displayId = this.mWin.getDisplayId();
                    if (!(displayId == -1 || displayId == 0)) {
                        lazyMode = 0;
                    }
                    if (lazyMode != 0 && this.mWin.mIsWallpaper) {
                        this.mSurfaceController.setPositionInTransaction(((float) xOffset) * this.mWin.getLazyScale(), ((float) yOffset) * this.mWin.getLazyScale(), recoveringMemory);
                    } else if (!HwFoldScreenState.isFoldScreenDevice() || !this.mService.isInSubFoldScaleMode() || !this.mWin.mIsWallpaper || HwPCUtils.isValidExtDisplayId(displayId)) {
                        this.mSurfaceController.setPositionInTransaction((float) xOffset, (float) yOffset, recoveringMemory);
                    } else {
                        this.mSurfaceController.setPositionInTransaction(((float) xOffset) * this.mService.mSubFoldModeScale, ((float) yOffset) * this.mService.mSubFoldModeScale, recoveringMemory);
                    }
                    this.mWin.updateSurfacePosition(this.mShownPosition.x, this.mShownPosition.y);
                    if (HwFoldScreenState.isFoldScreenDevice() && this.mService.isInSubFoldScaleMode() && !HwPCUtils.isValidExtDisplayId(displayId)) {
                        this.mWin.updateSurfacePositionBySubFoldMode(this.mShownPosition.x, this.mShownPosition.y);
                    }
                } else {
                    clipRect2 = clipRect4;
                }
                clipRect = clipRect2;
            }
            if (wasForceScaled && !this.mForceScaleUntilResize) {
                WindowSurfaceController windowSurfaceController2 = this.mSurfaceController;
                windowSurfaceController2.deferTransactionUntil(windowSurfaceController2.getHandle(), this.mWin.getFrameNumber());
                this.mSurfaceController.forceScaleableInTransaction(false);
            }
            if (!w.mSeamlesslyRotated) {
                applyCrop(clipRect, recoveringMemory);
                if (this.mSurfaceController.mSurfaceControl != null) {
                    this.mSurfaceController.mSurfaceControl.setLowResolutionInfo(this.mWin.mInvGlobalScale, this.mWin.getLowResolutionMode());
                }
                this.mSurfaceController.setMatrixInTransaction(this.mDsDx * w.mHScale * this.mExtraHScale, this.mDtDx * w.mVScale * this.mExtraVScale, this.mDtDy * w.mHScale * this.mExtraHScale, this.mDsDy * w.mVScale * this.mExtraVScale, recoveringMemory);
            }
            if (HwMwUtils.ENABLED && this.mWin.mAppToken != null) {
                HwMwUtils.performPolicy((int) WindowManagerService.H.GESTURE_NAVUP_TIMEOUT, new Object[]{this.mWin, this.mSurfaceController.mSurfaceControl, this.mTmpSize});
            }
            if (this.mWin.inHwFreeFormWindowingMode() && this.mWin.mAttrs.type == 3 && this.mService.getLazyMode() == 0) {
                this.mService.mAtmService.mHwATMSEx.setHwWinCornerRaduis(this.mWin, this.mSurfaceController.mSurfaceControl);
            }
            if (this.mSurfaceResized) {
                this.mReportSurfaceResized = true;
                this.mAnimator.setPendingLayoutChanges(w.getDisplayId(), 4);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void getContainerRect(Rect rect) {
        Task task = this.mWin.getTask();
        if (task != null) {
            task.getDimBounds(rect);
            return;
        }
        rect.bottom = 0;
        rect.right = 0;
        rect.top = 0;
        rect.left = 0;
    }

    /* access modifiers changed from: package-private */
    public void prepareSurfaceLocked(boolean recoveringMemory) {
        WindowState w = this.mWin;
        if (!hasSurface()) {
            if (w.getOrientationChanging() && w.isGoneForLayoutLw()) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    Slog.v(TAG, "Orientation change skips hidden " + w);
                }
                w.setOrientationChanging(false);
            }
        } else if (!w.getOrientationChanging() || !isEvilWindow(w)) {
            boolean displayed = false;
            hwPrepareSurfaceLocked();
            setSurfaceBoundariesLocked(recoveringMemory);
            if (this.mIsWallpaper && !w.mWallpaperVisible) {
                hide("prepareSurfaceLocked");
            } else if (w.isParentWindowHidden() || !w.isOnScreen()) {
                hide("prepareSurfaceLocked");
                this.mWallpaperControllerLocked.hideWallpapers(w);
                if (w.getOrientationChanging() && w.isGoneForLayoutLw()) {
                    w.setOrientationChanging(false);
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v(TAG, "Orientation change skips hidden " + w);
                    }
                }
            } else if (this.mLastAlpha == this.mShownAlpha && this.mLastDsDx == this.mDsDx && this.mLastDtDx == this.mDtDx && this.mLastDsDy == this.mDsDy && this.mLastDtDy == this.mDtDy && w.mLastHScale == w.mHScale && w.mLastVScale == w.mVScale && !this.mLastHidden) {
                displayed = true;
            } else {
                displayed = true;
                this.mLastAlpha = this.mShownAlpha;
                this.mLastDsDx = this.mDsDx;
                this.mLastDtDx = this.mDtDx;
                this.mLastDsDy = this.mDsDy;
                this.mLastDtDy = this.mDtDy;
                w.mLastHScale = w.mHScale;
                w.mLastVScale = w.mVScale;
                if (this.mSurfaceController.prepareToShowInTransaction(this.mShownAlpha, this.mDsDx * w.mHScale * this.mExtraHScale, this.mDtDx * w.mVScale * this.mExtraVScale, this.mDtDy * w.mHScale * this.mExtraHScale, this.mDsDy * w.mVScale * this.mExtraVScale, recoveringMemory) && this.mDrawState == 4 && this.mLastHidden) {
                    if (showSurfaceRobustlyLocked()) {
                        markPreservedSurfaceForDestroy();
                        this.mAnimator.requestRemovalOfReplacedWindows(w);
                        this.mLastHidden = false;
                        Flog.i(307, "showing " + this);
                        if (this.mIsWallpaper) {
                            w.dispatchWallpaperVisibility(true);
                        }
                        if (!w.getDisplayContent().getLastHasContent()) {
                            this.mAnimator.setPendingLayoutChanges(w.getDisplayId(), 8);
                        }
                    } else {
                        w.setOrientationChanging(false);
                    }
                }
                if (hasSurface()) {
                    w.mToken.hasVisible = true;
                }
            }
            if (w.getOrientationChanging()) {
                if (!w.isDrawnLw()) {
                    this.mAnimator.mBulkUpdateParams &= -5;
                    this.mAnimator.mLastWindowFreezeSource = w;
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v(TAG, "Orientation continue waiting for draw in " + w);
                    }
                } else {
                    w.setOrientationChanging(false);
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        Slog.v(TAG, "Orientation change complete in " + w);
                    }
                }
            }
            if (displayed) {
                w.mToken.hasVisible = true;
            }
        } else {
            Slog.v(TAG, "Orientation change skips evil window " + w);
            w.setOrientationChanging(false);
        }
    }

    /* access modifiers changed from: package-private */
    public void setTransparentRegionHintLocked(Region region) {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController == null) {
            Slog.w(TAG, "setTransparentRegionHint: null mSurface after mHasSurface true");
        } else {
            windowSurfaceController.setTransparentRegionHint(region);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean setWallpaperOffset(int dx, int dy) {
        if (this.mService.getLazyMode() != 0) {
            dx = (int) (((float) dx) * this.mWin.getLazyScale());
            dy = (int) (((float) dy) * this.mWin.getLazyScale());
        }
        if (this.mXOffset == dx && this.mYOffset == dy) {
            return false;
        }
        this.mXOffset = dx;
        this.mYOffset = dy;
        try {
            this.mService.openSurfaceTransaction();
            this.mSurfaceController.setPositionInTransaction((float) dx, (float) dy, false);
            applyCrop(null, false);
        } catch (RuntimeException e) {
            Slog.w(TAG, "Error positioning surface of " + this.mWin + " pos=(" + dx + "," + dy + ")", e);
        } catch (Throwable th) {
        }
        this.mService.closeSurfaceTransaction("setWallpaperOffset");
        return true;
    }

    /* access modifiers changed from: package-private */
    public boolean tryChangeFormatInPlaceLocked() {
        if (this.mSurfaceController == null) {
            return false;
        }
        WindowManager.LayoutParams attrs = this.mWin.getAttrs();
        if (((attrs.flags & 16777216) != 0 ? -3 : attrs.format) != this.mSurfaceFormat) {
            return false;
        }
        setOpaqueLocked(!PixelFormat.formatHasAlpha(attrs.format));
        return true;
    }

    /* access modifiers changed from: package-private */
    public void setOpaqueLocked(boolean isOpaque) {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            windowSurfaceController.setOpaque(isOpaque);
        }
    }

    /* access modifiers changed from: package-private */
    public void setSecureLocked(boolean isSecure) {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            windowSurfaceController.setSecure(isSecure);
        }
    }

    /* access modifiers changed from: package-private */
    public void setColorSpaceAgnosticLocked(boolean agnostic) {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            windowSurfaceController.setColorSpaceAgnostic(agnostic);
        }
    }

    private boolean showSurfaceRobustlyLocked() {
        if (this.mWin.getWindowConfiguration().windowsAreScaleable()) {
            this.mSurfaceController.forceScaleableInTransaction(true);
        }
        if (!this.mSurfaceController.showRobustlyInTransaction()) {
            return false;
        }
        WindowSurfaceController windowSurfaceController = this.mPendingDestroySurface;
        if (windowSurfaceController != null && this.mDestroyPreservedSurfaceUponRedraw) {
            windowSurfaceController.mSurfaceControl.hide();
            this.mPendingDestroySurface.reparentChildrenInTransaction(this.mSurfaceController);
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public void applyEnterAnimationLocked() {
        int transit;
        if (!this.mWin.mSkipEnterAnimationForSeamlessReplacement) {
            if (this.mEnterAnimationPending) {
                this.mEnterAnimationPending = false;
                transit = 1;
            } else {
                transit = 3;
            }
            if (this.mAttrType != 1) {
                applyAnimationLocked(transit, true);
            }
            if (this.mService.mAccessibilityController == null) {
                return;
            }
            if (!HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(this.mWin.getDisplayId()) || HwPCUtils.enabledInPad()) {
                this.mService.mAccessibilityController.onWindowTransitionLocked(this.mWin, transit);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean applyAnimationLocked(int transit, boolean isEntrance) {
        WindowState windowState;
        WindowState windowState2 = this.mWin;
        if (windowState2 != null && windowState2.getName().contains("DockedStackDivider")) {
            Slog.v(TAG, "skip DockedStackDivider Exiting Anim.");
            return false;
        } else if (HwFoldScreenState.isFoldScreenDevice() && CoordinationModeUtils.getInstance(this.mContext).isExitingCoordinationMode()) {
            return false;
        } else {
            WindowState windowState3 = this.mWin;
            if (windowState3 != null && HwPCUtils.isValidExtDisplayId(windowState3.getDisplayId()) && "com.huawei.desktop.explorer".equals(this.mWin.mAttrs.packageName) && this.mWin.getName().contains("BootWindow") && transit == 2) {
                Slog.v(TAG, "skip BootWindow Exiting Anim.");
                return false;
            } else if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && (windowState = this.mWin) != null && windowState.getName().contains("ExitDesktopAlertDialog") && transit == 2) {
                Slog.v(TAG, "skip Exiting Anim in PC mode.");
                return false;
            } else if (this.mWin.isSelfAnimating() && this.mAnimationIsEntrance == isEntrance) {
                return true;
            } else {
                if (isEntrance && this.mWin.mAttrs.type == 2011) {
                    this.mWin.getDisplayContent().adjustForImeIfNeeded();
                    this.mWin.setDisplayLayoutNeeded();
                    this.mService.mWindowPlacerLocked.requestTraversal();
                }
                Trace.traceBegin(32, "WSA#applyAnimationLocked");
                if (this.mWin.mToken.okToAnimate()) {
                    int anim = this.mWin.getDisplayContent().getDisplayPolicy().selectAnimationLw(this.mWin, transit);
                    int attr = -1;
                    Animation a = null;
                    if (anim != 0) {
                        a = anim != -1 ? AnimationUtils.loadAnimation(this.mContext, anim) : null;
                    } else {
                        if (transit == 1) {
                            attr = 0;
                        } else if (transit == 2) {
                            attr = 1;
                        } else if (transit == 3) {
                            attr = 2;
                        } else if (transit == 4) {
                            attr = 3;
                        }
                        if (attr >= 0) {
                            a = this.mWin.getDisplayContent().mAppTransition.loadAnimationAttr(this.mWin.mAttrs, attr, 0);
                        }
                    }
                    if (a != null) {
                        this.mWin.startAnimation(a);
                        this.mAnimationIsEntrance = isEntrance;
                    }
                } else {
                    this.mWin.cancelAnimation();
                }
                if (!isEntrance && this.mWin.mAttrs.type == 2011) {
                    this.mWin.getDisplayContent().adjustForImeIfNeeded();
                }
                Trace.traceEnd(32);
                return this.mWin.isAnimating();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        this.mLastClipRect.writeToProto(proto, 1146756268033L);
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            windowSurfaceController.writeToProto(proto, 1146756268034L);
        }
        proto.write(1159641169923L, this.mDrawState);
        this.mSystemDecorRect.writeToProto(proto, 1146756268036L);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        if (this.mAnimationIsEntrance) {
            pw.print(prefix);
            pw.print(" mAnimationIsEntrance=");
            pw.print(this.mAnimationIsEntrance);
        }
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            windowSurfaceController.dump(pw, prefix, dumpAll);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mDrawState=");
            pw.print(drawStateToString());
            pw.print(prefix);
            pw.print(" mLastHidden=");
            pw.println(this.mLastHidden);
            pw.print(prefix);
            pw.print("mSystemDecorRect=");
            this.mSystemDecorRect.printShortString(pw);
            pw.print(" mLastClipRect=");
            this.mLastClipRect.printShortString(pw);
            if (!this.mLastFinalClipRect.isEmpty()) {
                pw.print(" mLastFinalClipRect=");
                this.mLastFinalClipRect.printShortString(pw);
            }
            pw.println();
        }
        if (this.mPendingDestroySurface != null) {
            pw.print(prefix);
            pw.print("mPendingDestroySurface=");
            pw.println(this.mPendingDestroySurface);
        }
        if (this.mSurfaceResized || this.mSurfaceDestroyDeferred) {
            pw.print(prefix);
            pw.print("mSurfaceResized=");
            pw.print(this.mSurfaceResized);
            pw.print(" mSurfaceDestroyDeferred=");
            pw.println(this.mSurfaceDestroyDeferred);
        }
        if (!(this.mShownAlpha == 1.0f && this.mAlpha == 1.0f && this.mLastAlpha == 1.0f)) {
            pw.print(prefix);
            pw.print("mShownAlpha=");
            pw.print(this.mShownAlpha);
            pw.print(" mAlpha=");
            pw.print(this.mAlpha);
            pw.print(" mLastAlpha=");
            pw.println(this.mLastAlpha);
        }
        if (this.mHaveMatrix || this.mWin.mGlobalScale != 1.0f) {
            pw.print(prefix);
            pw.print("mGlobalScale=");
            pw.print(this.mWin.mGlobalScale);
            pw.print(" mDsDx=");
            pw.print(this.mDsDx);
            pw.print(" mDtDx=");
            pw.print(this.mDtDx);
            pw.print(" mDtDy=");
            pw.print(this.mDtDy);
            pw.print(" mDsDy=");
            pw.println(this.mDsDy);
        }
        pw.print(prefix);
        pw.print("mTmpSize=");
        this.mTmpSize.printShortString(pw);
        pw.println();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("WindowStateAnimator{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.mWin.mAttrs.getTitle());
        sb.append('}');
        return sb.toString();
    }

    /* access modifiers changed from: package-private */
    public void reclaimSomeSurfaceMemory(String operation, boolean secure) {
        this.mService.mRoot.reclaimSomeSurfaceMemory(this, operation, secure);
    }

    /* access modifiers changed from: package-private */
    public boolean getShown() {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            return windowSurfaceController.getShown();
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void destroySurface() {
        try {
            this.mWin.mHwWSEx.destoryMagicWindowDimmer();
            if (this.mSurfaceController != null) {
                this.mSurfaceController.destroyNotInTransaction();
            }
        } catch (RuntimeException e) {
            Slog.w(TAG, "Exception thrown when destroying surface " + this + " surface " + this.mSurfaceController + " session " + this.mSession + ": " + e);
        } catch (Throwable th) {
            this.mWin.setHasSurface(false);
            this.mSurfaceController = null;
            printDrawStateChanged(drawStateToString(), "NO_SURFACE", "destroySurface");
            this.mDrawState = 0;
            throw th;
        }
        this.mWin.setHasSurface(false);
        this.mSurfaceController = null;
        printDrawStateChanged(drawStateToString(), "NO_SURFACE", "destroySurface");
        this.mDrawState = 0;
    }

    /* access modifiers changed from: package-private */
    public boolean isForceScaled() {
        Task task = this.mWin.getTask();
        if (task == null || !task.mStack.isForceScaled()) {
            return this.mForceScaleUntilResize;
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public void detachChildren() {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            windowSurfaceController.detachChildren();
        }
        this.mChildrenDetached = true;
    }

    /* access modifiers changed from: package-private */
    public void setOffsetPositionForStackResize(boolean offsetPositionForStackResize) {
        this.mOffsetPositionForStackResize = offsetPositionForStackResize;
    }

    public void hwPrepareSurfaceLocked() {
        computeShownFrameLocked();
    }

    /* access modifiers changed from: package-private */
    public void computeShownFrameRightLocked() {
    }

    /* access modifiers changed from: package-private */
    public void computeShownFrameLeftLocked() {
    }

    /* access modifiers changed from: package-private */
    public boolean isEvilWindow(WindowState win) {
        return false;
    }

    /* access modifiers changed from: package-private */
    public void setWindowIconInfo(int iconType, int iconViewWidth, int iconViewHeight, Bitmap icon) {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController == null) {
            Slog.e(TAG, "mWindowSurfaceController is null!!");
        } else {
            windowSurfaceController.setWindowIconInfo(iconType, iconViewWidth, iconViewHeight, icon);
        }
    }
}
