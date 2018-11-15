package com.android.server.wm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Trace;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.IHwShutdownThread;
import java.io.PrintWriter;

public class WindowStateAnimator {
    static final int COMMIT_DRAW_PENDING = 2;
    static final int DRAW_PENDING = 1;
    static final int HAS_DRAWN = 4;
    static final int NO_SURFACE = 0;
    static final int READY_TO_SHOW = 3;
    static final int STACK_CLIP_AFTER_ANIM = 0;
    static final int STACK_CLIP_BEFORE_ANIM = 1;
    static final int STACK_CLIP_NONE = 2;
    static final String TAG = "WindowManager";
    static final int WINDOW_FREEZE_LAYER = 2000000;
    float mAlpha = 0.0f;
    int mAnimLayer;
    boolean mAnimationIsEntrance;
    private boolean mAnimationStartDelayed;
    final WindowAnimator mAnimator;
    int mAttrType;
    private BlurParams mBlurParams;
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
    public Object mInsetSurfaceLock = new Object();
    public InsetSurfaceOverlay mInsetSurfaceOverlay;
    final boolean mIsWallpaper;
    float mLastAlpha = 0.0f;
    Rect mLastClipRect = new Rect();
    private float mLastDsDx = 1.0f;
    private float mLastDsDy = 0.0f;
    private float mLastDtDx = 0.0f;
    private float mLastDtDy = 1.0f;
    Rect mLastFinalClipRect = new Rect();
    boolean mLastHidden;
    int mLastLayer;
    boolean mLazyIsEntering;
    boolean mLazyIsExiting;
    private boolean mOffsetPositionForStackResize;
    private WindowSurfaceController mPendingDestroySurface;
    boolean mPipAnimationStarted = false;
    final WindowManagerPolicy mPolicy;
    private final Transaction mReparentTransaction = new Transaction();
    boolean mReportSurfaceResized;
    final WindowManagerService mService;
    final Session mSession;
    float mShownAlpha = 0.0f;
    protected Point mShownPosition = new Point();
    WindowSurfaceController mSurfaceController;
    boolean mSurfaceDestroyDeferred;
    int mSurfaceFormat;
    boolean mSurfaceResized;
    private final Rect mSystemDecorRect = new Rect();
    private Rect mTmpAnimatingBounds = new Rect();
    Rect mTmpClipRect = new Rect();
    Rect mTmpFinalClipRect = new Rect();
    private final Point mTmpPos = new Point();
    private final Rect mTmpSize = new Rect();
    private Rect mTmpSourceBounds = new Rect();
    Rect mTmpStackBounds = new Rect();
    private final Transaction mTmpTransaction = new Transaction();
    final WallpaperController mWallpaperControllerLocked;
    final WindowState mWin;
    int mXOffset = 0;
    int mYOffset = 0;

    private class BlurParams {
        private static final int CHANGE_ALPHA = 4;
        private static final int CHANGE_BLANK = 16;
        private static final int CHANGE_MASK = 31;
        private static final int CHANGE_NONE = 0;
        private static final int CHANGE_RADIUS = 1;
        private static final int CHANGE_REGION = 8;
        private static final int CHANGE_ROUND = 2;
        float alpha;
        Rect blank;
        private int changes = 0;
        int radius;
        Region region;
        int rx;
        int ry;
        int surfaceHashCode;

        public BlurParams(int hashcode) {
            this.surfaceHashCode = hashcode;
        }

        public BlurParams(LayoutParams lp, int hashcode) {
            this.surfaceHashCode = hashcode;
            set(lp);
        }

        public boolean updateHashCode(int hashcode) {
            if (this.surfaceHashCode == hashcode) {
                return false;
            }
            this.surfaceHashCode = hashcode;
            return true;
        }

        public void set(LayoutParams lp) {
            setRadius(lp.blurRadius);
            setRound(lp.blurRoundx, lp.blurRoundy);
            setAlpha(lp.blurAlpha);
            setRegion(lp.blurRegion);
            setBlank(lp.blurBlankLeft, lp.blurBlankTop, lp.blurBlankRight, lp.blurBlankBottom);
        }

        public void setRadius(int radius) {
            int newRadius = Math.max(0, Math.min(radius, 100));
            if (this.radius != newRadius) {
                this.radius = newRadius;
                this.changes |= 1;
            }
        }

        public void setRound(int rx, int ry) {
            if (this.rx != rx || this.ry != ry) {
                this.rx = rx;
                this.ry = ry;
                this.changes |= 2;
            }
        }

        public void setAlpha(float alpha) {
            float newAlpha = Math.max(0.0f, Math.min(alpha, 1.0f));
            if (this.alpha != newAlpha) {
                this.alpha = newAlpha;
                this.changes |= 4;
            }
        }

        public void setRegion(Region region) {
            Region newRegion = region != null ? region : new Region();
            if (!newRegion.equals(this.region)) {
                this.region = newRegion;
                this.changes |= 8;
            }
        }

        public void setBlank(int l, int t, int r, int b) {
            Rect newBlank = new Rect(l, t, r, b);
            if (!newBlank.equals(this.blank)) {
                this.blank = newBlank;
                this.changes |= 16;
            }
        }

        public void refresh(WindowSurfaceController srufacecontroller, boolean force) {
            if (force || (this.changes & 1) == 1) {
                srufacecontroller.setBlurRadius(this.radius);
            }
            if (force || (this.changes & 2) == 2) {
                srufacecontroller.setBlurRound(this.rx, this.ry);
            }
            if (force || (this.changes & 4) == 4) {
                srufacecontroller.setBlurAlpha(this.alpha);
            }
            if (force || (this.changes & 8) == 8) {
                srufacecontroller.setBlurRegion(this.region);
            }
            if (force || (this.changes & 16) == 16) {
                srufacecontroller.setBlurBlank(this.blank);
            }
            this.changes = 0;
        }
    }

    String drawStateToString() {
        switch (this.mDrawState) {
            case 0:
                return "NO_SURFACE";
            case 1:
                return "DRAW_PENDING";
            case 2:
                return "COMMIT_DRAW_PENDING";
            case 3:
                return "READY_TO_SHOW";
            case 4:
                return "HAS_DRAWN";
            default:
                return Integer.toString(this.mDrawState);
        }
    }

    WindowStateAnimator(WindowState win) {
        WindowManagerService service = win.mService;
        this.mService = service;
        this.mAnimator = service.mAnimator;
        this.mPolicy = service.mPolicy;
        this.mContext = service.mContext;
        this.mWin = win;
        this.mSession = win.mSession;
        this.mAttrType = win.mAttrs.type;
        this.mIsWallpaper = win.mIsWallpaper;
        this.mWallpaperControllerLocked = this.mService.mRoot.mWallpaperController;
    }

    boolean isAnimationSet() {
        return this.mWin.isAnimating();
    }

    void cancelExitAnimationForNextAnimationLocked() {
        this.mWin.cancelAnimation();
        this.mWin.destroySurfaceUnchecked();
    }

    void onAnimationFinished() {
        if (this.mAnimator.mWindowDetachedWallpaper == this.mWin) {
            this.mAnimator.mWindowDetachedWallpaper = null;
        }
        this.mWin.checkPolicyVisibilityChange();
        DisplayContent displayContent = this.mWin.getDisplayContent();
        if (this.mAttrType == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME && this.mWin.mPolicyVisibility && displayContent != null) {
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

    void hide(Transaction transaction, String reason) {
        if (!this.mLastHidden) {
            this.mLastHidden = true;
            markPreservedSurfaceForDestroy();
            if (this.mSurfaceController != null) {
                this.mSurfaceController.hide(transaction, reason);
            }
        }
    }

    void hide(String reason) {
        hide(this.mTmpTransaction, reason);
        SurfaceControl.mergeToGlobalTransaction(this.mTmpTransaction);
    }

    boolean finishDrawingLocked() {
        if (this.mWin.mAttrs.type == 3) {
            int i = 1;
        }
        if (this.mDrawState != 1) {
            return false;
        }
        printDrawStateChanged("DRAW_PENDING", "COMMIT_DRAW_PENDING", "finishDrawing");
        this.mDrawState = 2;
        return true;
    }

    void printDrawStateChanged(String oldState, String newState, String reason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DrawState change: ");
        stringBuilder.append(this.mWin);
        stringBuilder.append(" from:");
        stringBuilder.append(oldState);
        stringBuilder.append(" to:");
        stringBuilder.append(newState);
        stringBuilder.append(" reason:");
        stringBuilder.append(reason);
        Flog.i(307, stringBuilder.toString());
    }

    boolean commitFinishDrawingLocked() {
        if (this.mDrawState != 2 && this.mDrawState != 3) {
            return false;
        }
        if (this.mDrawState == 2) {
            printDrawStateChanged("COMMIT_DRAW_PENDING", "READY_TO_SHOW", "commitFinishDrawing");
        }
        this.mDrawState = 3;
        boolean result = false;
        AppWindowToken atoken = this.mWin.mAppToken;
        if (atoken == null || atoken.allDrawn || this.mWin.mAttrs.type == 3) {
            result = this.mWin.performShowLocked();
        }
        return result;
    }

    void preserveSurfaceLocked() {
        if (this.mDestroyPreservedSurfaceUponRedraw) {
            this.mSurfaceDestroyDeferred = false;
            destroySurfaceLocked();
            this.mSurfaceDestroyDeferred = true;
        } else if (this.mWin != null && HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(this.mWin.getDisplayId()) && this.mSurfaceController == null) {
            HwPCUtils.log(TAG, "surface is null");
        } else {
            if (this.mSurfaceController != null) {
                this.mSurfaceController.mSurfaceControl.setLayer(1);
            }
            this.mDestroyPreservedSurfaceUponRedraw = true;
            this.mSurfaceDestroyDeferred = true;
            destroySurfaceLocked();
        }
    }

    void destroyPreservedSurfaceLocked() {
        if (this.mDestroyPreservedSurfaceUponRedraw) {
            if (!(this.mSurfaceController == null || this.mPendingDestroySurface == null || (this.mWin.mAppToken != null && this.mWin.mAppToken.isRelaunching()))) {
                this.mReparentTransaction.reparentChildren(this.mPendingDestroySurface.mSurfaceControl, this.mSurfaceController.mSurfaceControl.getHandle()).apply();
            }
            destroyDeferredSurfaceLocked();
            this.mDestroyPreservedSurfaceUponRedraw = false;
        }
    }

    void markPreservedSurfaceForDestroy() {
        if (this.mDestroyPreservedSurfaceUponRedraw && !this.mService.mDestroyPreservedSurface.contains(this.mWin)) {
            this.mService.mDestroyPreservedSurface.add(this.mWin);
        }
    }

    private int getLayerStack() {
        return this.mWin.getDisplayContent().getDisplay().getLayerStack();
    }

    void resetDrawState() {
        this.mDrawState = 1;
        if (this.mWin.mAppToken != null) {
            if (this.mWin.mAppToken.isSelfAnimating()) {
                this.mWin.mAppToken.deferClearAllDrawn = true;
            } else {
                this.mWin.mAppToken.clearAllDrawn();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:88:0x01dc A:{Splitter: B:54:0x0156, ExcHandler: java.lang.Exception (e java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x01dc A:{Splitter: B:54:0x0156, ExcHandler: java.lang.Exception (e java.lang.Exception)} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:88:0x01dc, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:89:0x01dd, code:
            r1 = r18;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    WindowSurfaceController createSurfaceLocked(int windowType, int ownerUid) {
        boolean z;
        int i;
        Exception e;
        Throwable th;
        int i2;
        WindowState w = this.mWin;
        if (this.mSurfaceController != null) {
            return this.mSurfaceController;
        }
        String str;
        this.mChildrenDetached = false;
        int windowType2 = (this.mWin.mAttrs.privateFlags & DumpState.DUMP_DEXOPT) != 0 ? 441731 : windowType;
        w.setHasSurface(false);
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createSurface ");
            stringBuilder.append(this);
            stringBuilder.append(": mDrawState=DRAW_PENDING");
            Slog.i(str, stringBuilder.toString());
        }
        printDrawStateChanged(drawStateToString(), "DRAW_PENDING", "createSurface");
        resetDrawState();
        this.mService.makeWindowFreezingScreenIfNeededLocked(w);
        int flags = 4;
        LayoutParams attrs = w.mAttrs;
        if (this.mService.isSecureLocked(w)) {
            flags = 4 | 128;
        }
        int flags2 = flags;
        this.mTmpSize.set(0, 0, 0, 0);
        calculateSurfaceBounds(w, attrs);
        int width = this.mTmpSize.width();
        int height = this.mTmpSize.height();
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Creating surface in session ");
            stringBuilder2.append(this.mSession.mSurfaceSession);
            stringBuilder2.append(" window ");
            stringBuilder2.append(this);
            stringBuilder2.append(" w=");
            stringBuilder2.append(width);
            stringBuilder2.append(" h=");
            stringBuilder2.append(height);
            stringBuilder2.append(" x=");
            stringBuilder2.append(this.mTmpSize.left);
            stringBuilder2.append(" y=");
            stringBuilder2.append(this.mTmpSize.top);
            stringBuilder2.append(" format=");
            stringBuilder2.append(attrs.format);
            stringBuilder2.append(" flags=");
            stringBuilder2.append(flags2);
            Slog.v(str, stringBuilder2.toString());
        }
        this.mLastClipRect.set(0, 0, 0, 0);
        int i3;
        try {
            int flags3;
            int format = (attrs.flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0 ? -3 : attrs.format;
            if (!PixelFormat.formatHasAlpha(attrs.format)) {
                try {
                    if (attrs.surfaceInsets.left == 0 && attrs.surfaceInsets.top == 0 && attrs.surfaceInsets.right == 0 && attrs.surfaceInsets.bottom == 0 && !w.isDragResizing()) {
                        flags2 |= 1024;
                    }
                } catch (OutOfResourcesException e2) {
                    z = true;
                    i = height;
                    i3 = width;
                    Slog.w(TAG, "OutOfResourcesException creating surface");
                    this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", z);
                    this.mDrawState = 0;
                    return null;
                } catch (Exception e3) {
                    e = e3;
                    i = height;
                    i3 = width;
                    Slog.e(TAG, "Exception creating surface (parent dead?)", e);
                    this.mDrawState = 0;
                    return null;
                }
            }
            if ((attrs.flags & 4) != 0) {
                flags3 = 65536 | flags2;
            } else {
                flags3 = flags2;
            }
            try {
                WindowSurfaceController windowSurfaceController = windowSurfaceController;
                int format2 = format;
                i3 = width;
                try {
                    this.mSurfaceController = new WindowSurfaceController(this.mSession.mSurfaceSession, attrs.getTitle().toString(), width, height, format, flags3, this, windowType2, ownerUid);
                    synchronized (this.mInsetSurfaceLock) {
                        try {
                            WindowManagerService windowManagerService = this.mService;
                            if (WindowManagerService.mSupporInputMethodFilletAdaptation) {
                                try {
                                    if (this.mWin.getAttrs().type == 2011 && this.mInsetSurfaceOverlay == null) {
                                        flags = this.mService.mPolicy.getDefaultNavBarHeight() / 2;
                                        this.mInsetSurfaceOverlay = new InsetSurfaceOverlay(this.mWin.getDisplayContent(), 0, i3, flags, this.mWin.mFrame.left, (this.mWin.mFrame.bottom - flags) - this.mWin.mLastSurfacePosition.y, this.mWin.getSurfaceControl());
                                        if (this.mService.mPolicy.isInputMethodMovedUp()) {
                                            this.mInsetSurfaceOverlay.createSurface();
                                        }
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    i2 = format2;
                                    z = true;
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                    }
                                    throw th;
                                }
                            }
                            setOffsetPositionForStackResize(false);
                            this.mSurfaceFormat = format2;
                            z = true;
                            w.setHasSurface(true);
                            this.mLastHidden = true;
                            return this.mSurfaceController;
                        } catch (Throwable th4) {
                            th = th4;
                            i2 = format2;
                            z = true;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    }
                } catch (OutOfResourcesException e4) {
                    z = true;
                } catch (Exception e5) {
                }
            } catch (OutOfResourcesException e6) {
                z = true;
                i = height;
                i3 = width;
                flags2 = flags3;
                Slog.w(TAG, "OutOfResourcesException creating surface");
                this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", z);
                this.mDrawState = 0;
                return null;
            } catch (Exception e7) {
                e = e7;
                i = height;
                i3 = width;
                flags2 = flags3;
                Slog.e(TAG, "Exception creating surface (parent dead?)", e);
                this.mDrawState = 0;
                return null;
            }
        } catch (OutOfResourcesException e8) {
            z = true;
            i = height;
            i3 = width;
            Slog.w(TAG, "OutOfResourcesException creating surface");
            this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", z);
            this.mDrawState = 0;
            return null;
        } catch (Exception e9) {
            e = e9;
            i = height;
            i3 = width;
            Slog.e(TAG, "Exception creating surface (parent dead?)", e);
            this.mDrawState = 0;
            return null;
        }
        Slog.w(TAG, "OutOfResourcesException creating surface");
        this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", z);
        this.mDrawState = 0;
        return null;
    }

    private void calculateSurfaceBounds(WindowState w, LayoutParams attrs) {
        if ((attrs.flags & 16384) != 0) {
            this.mTmpSize.right = this.mTmpSize.left + w.mRequestedWidth;
            this.mTmpSize.bottom = this.mTmpSize.top + w.mRequestedHeight;
        } else if (w.isDragResizing()) {
            if (w.getResizeMode() == 0) {
                this.mTmpSize.left = 0;
                this.mTmpSize.top = 0;
            }
            DisplayInfo displayInfo = w.getDisplayInfo();
            this.mTmpSize.right = this.mTmpSize.left + displayInfo.logicalWidth;
            this.mTmpSize.bottom = this.mTmpSize.top + displayInfo.logicalHeight;
        } else {
            this.mTmpSize.right = this.mTmpSize.left + w.mCompatFrame.width();
            this.mTmpSize.bottom = this.mTmpSize.top + w.mCompatFrame.height();
        }
        if (this.mTmpSize.width() < 1) {
            this.mTmpSize.right = this.mTmpSize.left + 1;
        }
        if (this.mTmpSize.height() < 1) {
            this.mTmpSize.bottom = this.mTmpSize.top + 1;
        }
        Rect rect = this.mTmpSize;
        rect.left -= attrs.surfaceInsets.left;
        rect = this.mTmpSize;
        rect.top -= attrs.surfaceInsets.top;
        rect = this.mTmpSize;
        rect.right += attrs.surfaceInsets.right;
        rect = this.mTmpSize;
        rect.bottom += attrs.surfaceInsets.bottom;
    }

    boolean hasSurface() {
        return this.mSurfaceController != null && this.mSurfaceController.hasSurface();
    }

    void destroySurfaceLocked() {
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
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Window ");
                    stringBuilder.append(this);
                    stringBuilder.append(" destroying surface ");
                    stringBuilder.append(this.mSurfaceController);
                    stringBuilder.append(", session ");
                    stringBuilder.append(this.mSession);
                    WindowManagerService.logWithStack(str, stringBuilder.toString());
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
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exception thrown when destroying Window ");
                stringBuilder2.append(this);
                stringBuilder2.append(" surface ");
                stringBuilder2.append(this.mSurfaceController);
                stringBuilder2.append(" session ");
                stringBuilder2.append(this.mSession);
                stringBuilder2.append(": ");
                stringBuilder2.append(e.toString());
                Slog.w(str2, stringBuilder2.toString());
            }
            this.mWin.setHasSurface(false);
            if (this.mSurfaceController != null) {
                this.mSurfaceController.setShown(false);
            }
            this.mSurfaceController = null;
            this.mDrawState = 0;
        }
    }

    void destroyDeferredSurfaceLocked() {
        try {
            if (this.mPendingDestroySurface != null) {
                this.mPendingDestroySurface.destroyNotInTransaction();
                if (!this.mDestroyPreservedSurfaceUponRedraw) {
                    this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
                }
            }
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception thrown when destroying Window ");
            stringBuilder.append(this);
            stringBuilder.append(" surface ");
            stringBuilder.append(this.mPendingDestroySurface);
            stringBuilder.append(" session ");
            stringBuilder.append(this.mSession);
            stringBuilder.append(": ");
            stringBuilder.append(e.toString());
            Slog.w(str, stringBuilder.toString());
        }
        this.mSurfaceDestroyDeferred = false;
        this.mPendingDestroySurface = null;
    }

    void computeShownFrameLocked() {
        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(this.mWin.getDisplayId());
        boolean screenAnimation = screenRotationAnimation != null && screenRotationAnimation.isAnimating();
        if (screenAnimation) {
            Rect frame = this.mWin.mFrame;
            float[] tmpFloats = this.mService.mTmpFloats;
            Matrix tmpMatrix = this.mWin.mTmpMatrix;
            if (screenRotationAnimation.isRotating()) {
                float w = (float) frame.width();
                float h = (float) frame.height();
                if (w < 1.0f || h < 1.0f) {
                    tmpMatrix.reset();
                } else {
                    tmpMatrix.setScale((2.0f / w) + 1.0f, 1.0f + (2.0f / h), w / 2.0f, h / 2.0f);
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
            if (!(this.mService.mLimitedAlphaCompositing && PixelFormat.formatHasAlpha(this.mWin.mAttrs.format) && !this.mWin.isIdentityMatrix(this.mDsDx, this.mDtDx, this.mDtDy, this.mDsDy)) && screenAnimation) {
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
        if (displayContent == null || w.inPinnedWindowingMode() || w.mAttrs.type == 2013) {
            return false;
        }
        w.calculatePolicyCrop(this.mSystemDecorRect);
        Task task = w.getTask();
        boolean fullscreen;
        if (w.fillsDisplay() || (task != null && task.isFullscreen())) {
            fullscreen = true;
        } else {
            fullscreen = false;
        }
        if (w.isDragResizing() && w.getResizeMode() == 0) {
            boolean isFreeformResizing = true;
        }
        clipRect.set(this.mSystemDecorRect);
        w.expandForSurfaceInsets(clipRect);
        clipRect.offset(w.mAttrs.surfaceInsets.left, w.mAttrs.surfaceInsets.top);
        w.transformClipRectFromScreenToSurfaceSpace(clipRect);
        return true;
    }

    private void applyCrop(Rect clipRect, boolean recoveringMemory) {
        if (clipRect == null) {
            this.mSurfaceController.clearCropInTransaction(recoveringMemory);
        } else if (!clipRect.equals(this.mLastClipRect)) {
            this.mLastClipRect.set(clipRect);
            this.mSurfaceController.setCropInTransaction(clipRect, recoveringMemory);
        }
    }

    void setSurfaceBoundariesLocked(boolean recoveringMemory) {
        boolean z = recoveringMemory;
        if (this.mSurfaceController != null) {
            boolean wasForceScaled;
            float surfaceWidth;
            float initialWidth;
            Rect clipRect;
            WindowState w = this.mWin;
            LayoutParams attrs = this.mWin.getAttrs();
            Task task = w.getTask();
            this.mTmpSize.set(0, 0, 0, 0);
            calculateSurfaceBounds(w, attrs);
            float[] pos = new float[]{0.0f, 0.0f};
            this.mExtraHScale = 1.0f;
            this.mExtraVScale = 1.0f;
            boolean wasForceScaled2 = this.mForceScaleUntilResize;
            boolean wasSeamlesslyRotated = w.mSeamlesslyRotated;
            boolean z2 = !w.mRelayoutCalled || w.mInRelayout;
            boolean relayout = z2;
            if (relayout) {
                this.mService.mDeferRelayoutWindow.remove(w);
                this.mSurfaceResized = this.mSurfaceController.setSizeInTransaction(this.mTmpSize.width(), this.mTmpSize.height(), z);
            } else {
                this.mSurfaceResized = false;
            }
            z2 = this.mForceScaleUntilResize && !this.mSurfaceResized;
            this.mForceScaleUntilResize = z2;
            WindowManagerService windowManagerService = this.mService;
            boolean z3 = w.mSeamlesslyRotated && !this.mSurfaceResized;
            windowManagerService.markForSeamlessRotation(w, z3);
            Rect clipRect2 = null;
            if (calculateCrop(this.mTmpClipRect)) {
                clipRect2 = this.mTmpClipRect;
            }
            float surfaceWidth2 = (float) this.mSurfaceController.getWidth();
            float surfaceHeight = (float) this.mSurfaceController.getHeight();
            Rect insets = attrs.surfaceInsets;
            int hInsets;
            int vInsets;
            float[] fArr;
            boolean wasSeamlesslyRotated2;
            Task task2;
            boolean z4;
            if (isForceScaled()) {
                int posY;
                hInsets = insets.left + insets.right;
                vInsets = insets.top + insets.bottom;
                float surfaceContentWidth = surfaceWidth2 - ((float) hInsets);
                float surfaceContentHeight = surfaceHeight - ((float) vInsets);
                if (this.mForceScaleUntilResize == 0) {
                    fArr = pos;
                    this.mSurfaceController.forceScaleableInTransaction(1);
                }
                vInsets = 0;
                wasSeamlesslyRotated2 = wasSeamlesslyRotated;
                wasForceScaled = wasForceScaled2;
                task.mStack.getDimBounds(this.mTmpStackBounds);
                boolean allowStretching = false;
                task.mStack.getFinalAnimationSourceHintBounds(this.mTmpSourceBounds);
                if (!this.mTmpSourceBounds.isEmpty() || ((this.mWin.mLastRelayoutContentInsets.width() <= 0 && this.mWin.mLastRelayoutContentInsets.height() <= 0) || task.mStack.lastAnimatingBoundsWasToFullscreen())) {
                    wasForceScaled2 = allowStretching;
                } else {
                    this.mTmpSourceBounds.set(task.mStack.mPreAnimationBounds);
                    this.mTmpSourceBounds.inset(this.mWin.mLastRelayoutContentInsets);
                    wasForceScaled2 = true;
                }
                this.mTmpStackBounds.intersectUnchecked(w.mParentFrame);
                this.mTmpSourceBounds.intersectUnchecked(w.mParentFrame);
                this.mTmpAnimatingBounds.intersectUnchecked(w.mParentFrame);
                if (this.mTmpSourceBounds.isEmpty()) {
                    surfaceWidth = surfaceWidth2;
                    task2 = task;
                    boolean z5 = wasForceScaled2;
                    z4 = relayout;
                    if (!w.mEnforceSizeCompat) {
                        this.mExtraHScale = ((float) this.mTmpStackBounds.width()) / surfaceContentWidth;
                        this.mExtraVScale = ((float) this.mTmpStackBounds.height()) / surfaceContentHeight;
                    }
                    clipRect2 = null;
                    posY = 0;
                } else {
                    task.mStack.getFinalAnimationBounds(this.mTmpAnimatingBounds);
                    float finalWidth = (float) this.mTmpAnimatingBounds.width();
                    initialWidth = (float) this.mTmpSourceBounds.width();
                    task = (surfaceContentWidth - ((float) this.mTmpStackBounds.width())) / (surfaceContentWidth - ((float) this.mTmpAnimatingBounds.width()));
                    relayout = task;
                    this.mExtraHScale = (initialWidth + ((finalWidth - initialWidth) * task)) / initialWidth;
                    boolean th;
                    if (wasForceScaled2) {
                        wasForceScaled2 = (float) this.mTmpSourceBounds.height();
                        th = relayout;
                        relayout = (surfaceContentHeight - ((float) this.mTmpStackBounds.height())) / (surfaceContentHeight - ((float) this.mTmpAnimatingBounds.height()));
                        this.mExtraVScale = (((((float) this.mTmpAnimatingBounds.height()) - wasForceScaled2) * task) + wasForceScaled2) / wasForceScaled2;
                    } else {
                        float f = initialWidth;
                        th = relayout;
                        this.mExtraVScale = this.mExtraHScale;
                    }
                    vInsets = 0 - ((int) ((this.mExtraHScale * task) * ((float) this.mTmpSourceBounds.left)));
                    int posY2 = 0 - ((int) ((this.mExtraVScale * relayout) * ((float) this.mTmpSourceBounds.top)));
                    clipRect2 = this.mTmpClipRect;
                    int posX = vInsets;
                    posY = posY2;
                    surfaceWidth = surfaceWidth2;
                    clipRect2.set((int) (((float) (insets.left + this.mTmpSourceBounds.left)) * task), (int) (((float) (insets.top + this.mTmpSourceBounds.top)) * relayout), insets.left + ((int) (surfaceWidth2 - ((surfaceWidth2 - ((float) this.mTmpSourceBounds.right)) * task))), insets.top + ((int) (surfaceHeight - ((surfaceHeight - ((float) this.mTmpSourceBounds.bottom)) * relayout))));
                    vInsets = posX;
                }
                this.mSurfaceController.setPositionInTransaction((float) Math.floor((double) ((int) (((float) (vInsets - ((int) (((float) attrs.x) * (1.0f - this.mExtraHScale))))) + (((float) insets.left) * (1.0f - this.mExtraHScale))))), (float) Math.floor((double) ((int) (((float) (posY - ((int) (((float) attrs.y) * (1.0f - this.mExtraVScale))))) + (((float) insets.top) * (1.0f - this.mExtraVScale))))), z);
                if (!this.mPipAnimationStarted) {
                    this.mForceScaleUntilResize = true;
                    this.mPipAnimationStarted = true;
                }
                clipRect = clipRect2;
            } else {
                Rect clipRect3;
                Rect clipRect4 = clipRect2;
                surfaceWidth = surfaceWidth2;
                task2 = task;
                fArr = pos;
                wasForceScaled = wasForceScaled2;
                wasSeamlesslyRotated2 = wasSeamlesslyRotated;
                z4 = relayout;
                this.mPipAnimationStarted = false;
                if (w.mSeamlesslyRotated) {
                    clipRect3 = clipRect4;
                } else {
                    int lazyMode;
                    int displayId;
                    vInsets = this.mXOffset;
                    hInsets = this.mYOffset;
                    if (this.mOffsetPositionForStackResize) {
                        if (z4) {
                            setOffsetPositionForStackResize(false);
                            this.mSurfaceController.deferTransactionUntil(this.mSurfaceController.getHandle(), this.mWin.getFrameNumber());
                            clipRect3 = clipRect4;
                        } else {
                            TaskStack stack = this.mWin.getStack();
                            this.mTmpPos.x = 0;
                            this.mTmpPos.y = 0;
                            if (stack != null) {
                                stack.getRelativePosition(this.mTmpPos);
                            }
                            vInsets = -this.mTmpPos.x;
                            hInsets = -this.mTmpPos.y;
                            if (clipRect4 != null) {
                                clipRect3 = clipRect4;
                                clipRect3.right += this.mTmpPos.x;
                                clipRect3.bottom += this.mTmpPos.y;
                            }
                        }
                        lazyMode = this.mService.getLazyMode();
                        displayId = this.mWin.getDisplayId();
                        if (!(displayId == -1 || displayId == 0)) {
                            lazyMode = 0;
                        }
                        if (lazyMode == 0 && this.mWin.mIsWallpaper) {
                            this.mSurfaceController.setPositionInTransaction(((float) vInsets) * this.mWin.mLazyScale, ((float) hInsets) * this.mWin.mLazyScale, z);
                        } else {
                            this.mSurfaceController.setPositionInTransaction((float) vInsets, (float) hInsets, z);
                        }
                        this.mWin.updateSurfacePosition(this.mShownPosition.x, this.mShownPosition.y);
                    }
                    clipRect3 = clipRect4;
                    lazyMode = this.mService.getLazyMode();
                    displayId = this.mWin.getDisplayId();
                    lazyMode = 0;
                    if (lazyMode == 0) {
                    }
                    this.mSurfaceController.setPositionInTransaction((float) vInsets, (float) hInsets, z);
                    this.mWin.updateSurfacePosition(this.mShownPosition.x, this.mShownPosition.y);
                }
                clipRect = clipRect3;
            }
            if ((wasForceScaled && !this.mForceScaleUntilResize) || (wasSeamlesslyRotated2 && !w.mSeamlesslyRotated)) {
                this.mSurfaceController.setGeometryAppliesWithResizeInTransaction(true);
                this.mSurfaceController.forceScaleableInTransaction(false);
            }
            if (w.mSeamlesslyRotated) {
                initialWidth = surfaceHeight;
                float f2 = surfaceWidth;
            } else {
                applyCrop(clipRect, z);
                setSurfaceLowResolutionInfo();
                this.mSurfaceController.setMatrixInTransaction((this.mDsDx * w.mHScale) * this.mExtraHScale, (this.mDtDx * w.mVScale) * this.mExtraVScale, (this.mDtDy * w.mHScale) * this.mExtraHScale, (this.mDsDy * w.mVScale) * this.mExtraVScale, z);
            }
            if (this.mSurfaceResized) {
                this.mReportSurfaceResized = true;
                this.mAnimator.setPendingLayoutChanges(w.getDisplayId(), 4);
            }
        }
    }

    void getContainerRect(Rect rect) {
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

    void prepareSurfaceLocked(boolean recoveringMemory) {
        WindowState w = this.mWin;
        String str;
        StringBuilder stringBuilder;
        if (!hasSurface()) {
            if (w.getOrientationChanging() && w.isGoneForLayoutLw()) {
                if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Orientation change skips hidden ");
                    stringBuilder.append(w);
                    Slog.v(str, stringBuilder.toString());
                }
                w.setOrientationChanging(false);
            }
        } else if (w.getOrientationChanging() && isEvilWindow(w)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Orientation change skips evil window ");
            stringBuilder.append(w);
            Slog.v(str, stringBuilder.toString());
            w.setOrientationChanging(false);
        } else {
            boolean displayed = false;
            hwPrepareSurfaceLocked();
            setSurfaceBoundariesLocked(recoveringMemory);
            if (this.mIsWallpaper && !this.mWin.mWallpaperVisible) {
                hide("prepareSurfaceLocked");
            } else if (w.isParentWindowHidden() || !w.isOnScreen()) {
                hide("prepareSurfaceLocked");
                this.mWallpaperControllerLocked.hideWallpapers(w);
                if (w.getOrientationChanging() && w.isGoneForLayoutLw()) {
                    w.setOrientationChanging(false);
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Orientation change skips hidden ");
                        stringBuilder2.append(w);
                        Slog.v(str2, stringBuilder2.toString());
                    }
                }
            } else if (this.mLastLayer == this.mAnimLayer && this.mLastAlpha == this.mShownAlpha && this.mLastDsDx == this.mDsDx && this.mLastDtDx == this.mDtDx && this.mLastDsDy == this.mDsDy && this.mLastDtDy == this.mDtDy && w.mLastHScale == w.mHScale && w.mLastVScale == w.mVScale && !this.mLastHidden) {
                displayed = true;
            } else {
                displayed = true;
                this.mLastAlpha = this.mShownAlpha;
                this.mLastLayer = this.mAnimLayer;
                this.mLastDsDx = this.mDsDx;
                this.mLastDtDx = this.mDtDx;
                this.mLastDsDy = this.mDsDy;
                this.mLastDtDy = this.mDtDy;
                w.mLastHScale = w.mHScale;
                w.mLastVScale = w.mVScale;
                if (this.mSurfaceController.prepareToShowInTransaction(this.mShownAlpha, this.mExtraHScale * (this.mDsDx * w.mHScale), this.mExtraVScale * (this.mDtDx * w.mVScale), this.mExtraHScale * (this.mDtDy * w.mHScale), this.mExtraVScale * (this.mDsDy * w.mVScale), recoveringMemory) && this.mDrawState == 4 && this.mLastHidden) {
                    if (showSurfaceRobustlyLocked()) {
                        markPreservedSurfaceForDestroy();
                        this.mAnimator.requestRemovalOfReplacedWindows(w);
                        this.mLastHidden = false;
                        if (this.mIsWallpaper) {
                            w.dispatchWallpaperVisibility(true);
                        }
                        this.mAnimator.setPendingLayoutChanges(w.getDisplayId(), 8);
                    } else {
                        w.setOrientationChanging(false);
                    }
                }
                if (hasSurface()) {
                    w.mToken.hasVisible = true;
                }
            }
            if (w.getOrientationChanging()) {
                String str3;
                if (w.isDrawnLw()) {
                    w.setOrientationChanging(false);
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        str3 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Orientation change complete in ");
                        stringBuilder.append(w);
                        Slog.v(str3, stringBuilder.toString());
                    }
                } else {
                    WindowAnimator windowAnimator = this.mAnimator;
                    windowAnimator.mBulkUpdateParams &= -9;
                    this.mAnimator.mLastWindowFreezeSource = w;
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        str3 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Orientation continue waiting for draw in ");
                        stringBuilder.append(w);
                        Slog.v(str3, stringBuilder.toString());
                    }
                }
            }
            if (displayed) {
                w.mToken.hasVisible = true;
            }
        }
    }

    void setTransparentRegionHintLocked(Region region) {
        if (this.mSurfaceController == null) {
            Slog.w(TAG, "setTransparentRegionHint: null mSurface after mHasSurface true");
        } else {
            this.mSurfaceController.setTransparentRegionHint(region);
        }
    }

    boolean setWallpaperOffset(int dx, int dy) {
        if (this.mService.getLazyMode() != 0) {
            dx = (int) (((float) dx) * this.mWin.mLazyScale);
            dy = (int) (((float) dy) * this.mWin.mLazyScale);
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error positioning surface of ");
            stringBuilder.append(this.mWin);
            stringBuilder.append(" pos=(");
            stringBuilder.append(dx);
            stringBuilder.append(",");
            stringBuilder.append(dy);
            stringBuilder.append(")");
            Slog.w(str, stringBuilder.toString(), e);
        } catch (Throwable th) {
        }
        this.mService.closeSurfaceTransaction("setWallpaperOffset");
        return true;
    }

    boolean tryChangeFormatInPlaceLocked() {
        if (this.mSurfaceController == null) {
            return false;
        }
        LayoutParams attrs = this.mWin.getAttrs();
        if (((attrs.flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0 ? -3 : attrs.format) != this.mSurfaceFormat) {
            return false;
        }
        setOpaqueLocked(PixelFormat.formatHasAlpha(attrs.format) ^ true);
        return true;
    }

    void setOpaqueLocked(boolean isOpaque) {
        if (this.mSurfaceController != null) {
            this.mSurfaceController.setOpaque(isOpaque);
        }
    }

    void setSecureLocked(boolean isSecure) {
        if (this.mSurfaceController != null) {
            this.mSurfaceController.setSecure(isSecure);
        }
    }

    private boolean showSurfaceRobustlyLocked() {
        if (this.mWin.getWindowConfiguration().windowsAreScaleable()) {
            this.mSurfaceController.forceScaleableInTransaction(true);
        }
        if (!this.mSurfaceController.showRobustlyInTransaction()) {
            return false;
        }
        if (this.mPendingDestroySurface != null && this.mDestroyPreservedSurfaceUponRedraw) {
            this.mPendingDestroySurface.mSurfaceControl.hide();
            this.mPendingDestroySurface.reparentChildrenInTransaction(this.mSurfaceController);
        }
        return true;
    }

    void applyEnterAnimationLocked() {
        if (!this.mWin.mSkipEnterAnimationForSeamlessReplacement) {
            int transit;
            if (this.mEnterAnimationPending) {
                this.mEnterAnimationPending = false;
                transit = 1;
            } else {
                transit = 3;
            }
            applyAnimationLocked(transit, true);
            if (this.mService.mAccessibilityController != null && (this.mWin.getDisplayId() == 0 || (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer()))) {
                this.mService.mAccessibilityController.onWindowTransitionLocked(this.mWin, transit);
            }
        }
    }

    boolean applyAnimationLocked(int transit, boolean isEntrance) {
        if (this.mWin != null && this.mWin.getName().contains("DockedStackDivider")) {
            Slog.v(TAG, "skip DockedStackDivider Exiting Anim.");
            return false;
        } else if (this.mWin != null && HwPCUtils.isValidExtDisplayId(this.mWin.getDisplayId()) && "com.huawei.desktop.explorer".equals(this.mWin.mAttrs.packageName) && this.mWin.getName().contains("BootWindow") && transit == 2) {
            Slog.v(TAG, "skip BootWindow Exiting Anim.");
            return false;
        } else if (HwPCUtils.enabledInPad() && this.mWin != null && this.mWin.getName().contains("StatusBar") && transit == 2) {
            Slog.v(TAG, "skip StatusBar Exiting Anim.");
            return false;
        } else if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && this.mWin != null && ((this.mWin.isInputMethodWindow() || this.mWin.getName().contains("ExitDesktopAlertDialog")) && transit == 2)) {
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
                int anim = this.mPolicy.selectAnimationLw(this.mWin, transit);
                int attr = -1;
                Animation a = null;
                if (anim != 0) {
                    a = anim != -1 ? AnimationUtils.loadAnimation(this.mContext, anim) : null;
                } else {
                    switch (transit) {
                        case 1:
                            attr = 0;
                            break;
                        case 2:
                            attr = 1;
                            break;
                        case 3:
                            attr = 2;
                            break;
                        case 4:
                            attr = 3;
                            break;
                    }
                    if (attr >= 0) {
                        a = this.mService.mAppTransition.loadAnimationAttr(this.mWin.mAttrs, attr, 0);
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
            return isAnimationSet();
        }
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        this.mLastClipRect.writeToProto(proto, 1146756268033L);
        if (this.mSurfaceController != null) {
            this.mSurfaceController.writeToProto(proto, 1146756268034L);
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
        if (this.mSurfaceController != null) {
            this.mSurfaceController.dump(pw, prefix, dumpAll);
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
        if (this.mAnimationStartDelayed) {
            pw.print(prefix);
            pw.print("mAnimationStartDelayed=");
            pw.print(this.mAnimationStartDelayed);
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

    void reclaimSomeSurfaceMemory(String operation, boolean secure) {
        this.mService.mRoot.reclaimSomeSurfaceMemory(this, operation, secure);
    }

    boolean getShown() {
        if (this.mSurfaceController != null) {
            return this.mSurfaceController.getShown();
        }
        return false;
    }

    void destroySurface() {
        try {
            if (this.mSurfaceController != null) {
                this.mSurfaceController.destroyNotInTransaction();
            }
        } catch (RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception thrown when destroying surface ");
            stringBuilder.append(this);
            stringBuilder.append(" surface ");
            stringBuilder.append(this.mSurfaceController);
            stringBuilder.append(" session ");
            stringBuilder.append(this.mSession);
            stringBuilder.append(": ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
        } catch (Throwable th) {
            this.mWin.setHasSurface(false);
            this.mSurfaceController = null;
            printDrawStateChanged(drawStateToString(), "NO_SURFACE", "destroySurface");
            this.mDrawState = 0;
        }
        this.mWin.setHasSurface(false);
        this.mSurfaceController = null;
        printDrawStateChanged(drawStateToString(), "NO_SURFACE", "destroySurface");
        this.mDrawState = 0;
    }

    void seamlesslyRotateWindow(Transaction t, int oldRotation, int newRotation) {
        WindowState w = this.mWin;
        Transaction transaction;
        if (!w.isVisibleNow() || w.mIsWallpaper) {
            transaction = t;
            return;
        }
        Rect cropRect = this.mService.mTmpRect;
        Rect displayRect = this.mService.mTmpRect2;
        RectF frameRect = this.mService.mTmpRectF;
        Matrix transform = this.mService.mTmpTransform;
        float x = (float) w.mFrame.left;
        float y = (float) w.mFrame.top;
        float width = (float) w.mFrame.width();
        float height = (float) w.mFrame.height();
        this.mService.getDefaultDisplayContentLocked().getBounds(displayRect);
        float displayWidth = (float) displayRect.width();
        float height2 = (float) displayRect.height();
        float displayHeight = height2;
        DisplayContent.createRotationMatrix(DisplayContent.deltaRotation(newRotation, oldRotation), x, y, displayWidth, height2, transform);
        this.mService.markForSeamlessRotation(w, true);
        transform.getValues(this.mService.mTmpFloats);
        float DsDx = this.mService.mTmpFloats[0];
        float DtDx = this.mService.mTmpFloats[3];
        float DtDy = this.mService.mTmpFloats[1];
        height2 = this.mService.mTmpFloats[4];
        transaction = t;
        this.mSurfaceController.setPosition(transaction, this.mService.mTmpFloats[2], this.mService.mTmpFloats[5], null);
        this.mSurfaceController.setMatrix(transaction, DsDx * w.mHScale, DtDx * w.mVScale, DtDy * w.mHScale, height2 * w.mVScale, false);
    }

    boolean isForceScaled() {
        Task task = this.mWin.getTask();
        if (task == null || !task.mStack.isForceScaled()) {
            return this.mForceScaleUntilResize;
        }
        return true;
    }

    void detachChildren() {
        if (this.mSurfaceController != null) {
            this.mSurfaceController.detachChildren();
        }
        this.mChildrenDetached = true;
    }

    int getLayer() {
        return this.mLastLayer;
    }

    void setOffsetPositionForStackResize(boolean offsetPositionForStackResize) {
        this.mOffsetPositionForStackResize = offsetPositionForStackResize;
    }

    public int adjustAnimLayerIfCoverclosed(int type, int animLayer) {
        return animLayer;
    }

    public void hwPrepareSurfaceLocked() {
        computeShownFrameLocked();
    }

    void computeShownFrameRightLocked() {
    }

    void computeShownFrameLeftLocked() {
    }

    public float[] getPCDisplayModeSurfacePos(Rect tmpSize) {
        return new float[0];
    }

    public void updateBlurLayer(LayoutParams lp) {
        if (this.mSurfaceController != null) {
            boolean forceRefresh;
            int surfaceHashCode = this.mSurfaceController.hashCode();
            if (this.mBlurParams == null) {
                forceRefresh = true;
                this.mBlurParams = new BlurParams(lp, surfaceHashCode);
            } else {
                forceRefresh = this.mBlurParams.updateHashCode(surfaceHashCode);
                this.mBlurParams.set(lp);
            }
            this.mBlurParams.refresh(this.mSurfaceController, forceRefresh);
        }
    }

    void setWindowClipFlag(int flag) {
        if (this.mSurfaceController == null) {
            Slog.e(TAG, "mWindowSurfaceController is null!!");
        } else {
            this.mSurfaceController.setWindowClipFlag(flag);
        }
    }

    void setWindowClipRound(float roundx, float roundy) {
        if (this.mSurfaceController == null) {
            Slog.e(TAG, "mWindowSurfaceController is null!!");
        } else {
            this.mSurfaceController.setWindowClipRound(roundx, roundy);
        }
    }

    void setWindowClipIcon(int iconViewWidth, int iconViewHeight, Bitmap icon) {
        if (this.mSurfaceController == null) {
            Slog.e(TAG, "mWindowSurfaceController is null!!");
        } else {
            this.mSurfaceController.setWindowClipIcon(iconViewWidth, iconViewHeight, icon);
        }
    }

    private void setSurfaceLowResolutionInfo() {
        WindowState w = this.mWin;
        this.mSurfaceController.setSurfaceLowResolutionInfo(w.mGlobalScale, w.getLowResolutionMode());
    }

    boolean isEvilWindow(WindowState win) {
        return false;
    }
}
