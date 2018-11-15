package com.android.server.wm;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.HwPCUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.MagnificationSpec;
import android.view.Surface;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceControl;
import android.view.ViewConfiguration;
import android.view.WindowInfo;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import com.android.internal.os.SomeArgs;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.pm.DumpState;
import com.android.server.wm.WindowManagerInternal.MagnificationCallbacks;
import com.android.server.wm.WindowManagerInternal.WindowsForAccessibilityCallback;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

final class AccessibilityController {
    private static final float[] sTempFloats = new float[9];
    private DisplayMagnifier mDisplayMagnifier;
    private final WindowManagerService mService;
    private WindowsForAccessibilityObserver mWindowsForAccessibilityObserver;

    private static final class DisplayMagnifier {
        private static final boolean DEBUG_LAYERS = false;
        private static final boolean DEBUG_RECTANGLE_REQUESTED = false;
        private static final boolean DEBUG_ROTATION = false;
        private static final boolean DEBUG_VIEWPORT_WINDOW = false;
        private static final boolean DEBUG_WINDOW_TRANSITIONS = false;
        private static final String LOG_TAG = "WindowManager";
        private final MagnificationCallbacks mCallbacks;
        private final Context mContext;
        private boolean mForceShowMagnifiableBounds = false;
        private final Handler mHandler;
        private final long mLongAnimationDuration;
        private final MagnifiedViewport mMagnifedViewport;
        private final WindowManagerService mService;
        private final Rect mTempRect1 = new Rect();
        private final Rect mTempRect2 = new Rect();
        private final Region mTempRegion1 = new Region();
        private final Region mTempRegion2 = new Region();
        private final Region mTempRegion3 = new Region();
        private final Region mTempRegion4 = new Region();

        private final class MagnifiedViewport {
            private final float mBorderWidth;
            private final Path mCircularPath;
            private final int mDrawBorderInset;
            private boolean mFullRedrawNeeded;
            private final int mHalfBorderWidth;
            private final Region mMagnificationRegion = new Region();
            private final MagnificationSpec mMagnificationSpec = MagnificationSpec.obtain();
            private final Region mOldMagnificationRegion = new Region();
            private int mTempLayer = 0;
            private final Matrix mTempMatrix = new Matrix();
            private final Point mTempPoint = new Point();
            private final RectF mTempRectF = new RectF();
            private final SparseArray<WindowState> mTempWindowStates = new SparseArray();
            private final ViewportWindow mWindow;
            private final WindowManager mWindowManager;

            private final class ViewportWindow {
                private static final String SURFACE_TITLE = "Magnification Overlay";
                private int mAlpha;
                private final AnimationController mAnimationController;
                private final Region mBounds = new Region();
                private Context mContext;
                private final Rect mDirtyRect = new Rect();
                private boolean mInvalidated;
                private boolean mLastInPCMode = false;
                private final Paint mPaint = new Paint();
                private boolean mShown;
                private final Surface mSurface = new Surface();
                private final SurfaceControl mSurfaceControl;

                private final class AnimationController extends Handler {
                    private static final int MAX_ALPHA = 255;
                    private static final int MIN_ALPHA = 0;
                    private static final int MSG_FRAME_SHOWN_STATE_CHANGED = 1;
                    private static final String PROPERTY_NAME_ALPHA = "alpha";
                    private final ValueAnimator mShowHideFrameAnimator;

                    public AnimationController(Context context, Looper looper) {
                        super(looper);
                        this.mShowHideFrameAnimator = ObjectAnimator.ofInt(ViewportWindow.this, PROPERTY_NAME_ALPHA, new int[]{0, 255});
                        long longAnimationDuration = (long) context.getResources().getInteger(17694722);
                        this.mShowHideFrameAnimator.setInterpolator(new DecelerateInterpolator(2.5f));
                        this.mShowHideFrameAnimator.setDuration(longAnimationDuration);
                    }

                    public void onFrameShownStateChanged(boolean shown, boolean animate) {
                        obtainMessage(1, shown, animate).sendToTarget();
                    }

                    public void handleMessage(Message message) {
                        boolean animate = true;
                        if (message.what == 1) {
                            boolean shown = message.arg1 == 1;
                            if (message.arg2 != 1) {
                                animate = false;
                            }
                            if (!animate) {
                                this.mShowHideFrameAnimator.cancel();
                                if (shown) {
                                    ViewportWindow.this.setAlpha(255);
                                } else {
                                    ViewportWindow.this.setAlpha(0);
                                }
                            } else if (this.mShowHideFrameAnimator.isRunning()) {
                                this.mShowHideFrameAnimator.reverse();
                            } else if (shown) {
                                this.mShowHideFrameAnimator.start();
                            } else {
                                this.mShowHideFrameAnimator.reverse();
                            }
                        }
                    }
                }

                public ViewportWindow(Context context) {
                    SurfaceControl surfaceControl = null;
                    this.mContext = null;
                    try {
                        MagnifiedViewport.this.mWindowManager.getDefaultDisplay().getRealSize(MagnifiedViewport.this.mTempPoint);
                        surfaceControl = DisplayMagnifier.this.mService.getDefaultDisplayContentLocked().makeOverlay().setName(SURFACE_TITLE).setSize(MagnifiedViewport.this.mTempPoint.x, MagnifiedViewport.this.mTempPoint.y).setFormat(-3).build();
                    } catch (OutOfResourcesException e) {
                    }
                    this.mSurfaceControl = surfaceControl;
                    this.mSurfaceControl.setLayer(DisplayMagnifier.this.mService.mPolicy.getWindowLayerFromTypeLw(2027) * 10000);
                    this.mSurfaceControl.setPosition(0.0f, 0.0f);
                    this.mSurface.copyFrom(this.mSurfaceControl);
                    this.mAnimationController = new AnimationController(context, DisplayMagnifier.this.mService.mH.getLooper());
                    TypedValue typedValue = new TypedValue();
                    context.getTheme().resolveAttribute(16843664, typedValue, true);
                    int borderColor = context.getColor(typedValue.resourceId);
                    this.mPaint.setStyle(Style.STROKE);
                    this.mPaint.setStrokeWidth(MagnifiedViewport.this.mBorderWidth);
                    this.mPaint.setColor(borderColor);
                    this.mInvalidated = true;
                    this.mContext = context;
                }

                public void setShown(boolean shown, boolean animate) {
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (this.mShown == shown) {
                            } else {
                                this.mShown = shown;
                                this.mAnimationController.onFrameShownStateChanged(shown, animate);
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        } finally {
                            while (true) {
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }

                public int getAlpha() {
                    int i;
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            i = this.mAlpha;
                        } finally {
                            while (true) {
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    return i;
                }

                public void setAlpha(int alpha) {
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (this.mAlpha == alpha) {
                            } else {
                                this.mAlpha = alpha;
                                invalidate(null);
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        } finally {
                            while (true) {
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }

                public void setBounds(Region bounds) {
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (this.mBounds.equals(bounds)) {
                            } else {
                                this.mBounds.set(bounds);
                                invalidate(this.mDirtyRect);
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        } finally {
                            while (true) {
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }

                public void updateSize() {
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            MagnifiedViewport.this.mWindowManager.getDefaultDisplay().getRealSize(MagnifiedViewport.this.mTempPoint);
                            this.mSurfaceControl.setSize(MagnifiedViewport.this.mTempPoint.x, MagnifiedViewport.this.mTempPoint.y);
                            invalidate(this.mDirtyRect);
                        } finally {
                            while (true) {
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }

                public void invalidate(Rect dirtyRect) {
                    if (dirtyRect != null) {
                        this.mDirtyRect.set(dirtyRect);
                    } else {
                        this.mDirtyRect.setEmpty();
                    }
                    this.mInvalidated = true;
                    DisplayMagnifier.this.mService.scheduleAnimationLocked();
                }

                private void setLayerStackForPC() {
                    if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() != this.mLastInPCMode) {
                        if (HwPCUtils.isPcCastModeInServer()) {
                            DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
                            Display display = null;
                            if (displayManager != null) {
                                display = displayManager.getDisplay(HwPCUtils.getPCDisplayID());
                            }
                            if (display != null) {
                                String name = AccessibilityController.class.getName();
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("set pc display layerstack:");
                                stringBuilder.append(display);
                                HwPCUtils.log(name, stringBuilder.toString());
                                this.mSurfaceControl.setLayerStack(display.getLayerStack());
                            } else {
                                this.mSurfaceControl.setLayerStack(MagnifiedViewport.this.mWindowManager.getDefaultDisplay().getLayerStack());
                            }
                        } else {
                            this.mSurfaceControl.setLayerStack(MagnifiedViewport.this.mWindowManager.getDefaultDisplay().getLayerStack());
                        }
                        this.mLastInPCMode = HwPCUtils.isPcCastModeInServer();
                    }
                }

                /* JADX WARNING: Missing block: B:28:0x0081, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
                /* JADX WARNING: Missing block: B:29:0x0084, code:
            return;
     */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void drawIfNeeded() {
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (this.mInvalidated) {
                                setLayerStackForPC();
                                this.mInvalidated = false;
                                if (this.mAlpha > 0) {
                                    Canvas canvas = null;
                                    try {
                                        if (this.mDirtyRect.isEmpty()) {
                                            this.mBounds.getBounds(this.mDirtyRect);
                                        }
                                        this.mDirtyRect.inset(-MagnifiedViewport.this.mHalfBorderWidth, -MagnifiedViewport.this.mHalfBorderWidth);
                                        canvas = this.mSurface.lockCanvas(this.mDirtyRect);
                                    } catch (IllegalArgumentException e) {
                                    } catch (OutOfResourcesException e2) {
                                    }
                                    if (canvas == null) {
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        return;
                                    }
                                    canvas.drawColor(0, Mode.CLEAR);
                                    this.mPaint.setAlpha(this.mAlpha);
                                    canvas.drawPath(this.mBounds.getBoundaryPath(), this.mPaint);
                                    this.mSurface.unlockCanvasAndPost(canvas);
                                    this.mSurfaceControl.show();
                                } else {
                                    this.mSurfaceControl.hide();
                                }
                            }
                        } finally {
                            while (true) {
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }

                public void releaseSurface() {
                    this.mSurfaceControl.release();
                    this.mSurface.release();
                }
            }

            public MagnifiedViewport() {
                this.mWindowManager = (WindowManager) DisplayMagnifier.this.mContext.getSystemService("window");
                this.mBorderWidth = DisplayMagnifier.this.mContext.getResources().getDimension(17104903);
                this.mHalfBorderWidth = (int) Math.ceil((double) (this.mBorderWidth / 2.0f));
                this.mDrawBorderInset = ((int) this.mBorderWidth) / 2;
                this.mWindow = new ViewportWindow(DisplayMagnifier.this.mContext);
                if (DisplayMagnifier.this.mContext.getResources().getConfiguration().isScreenRound()) {
                    this.mCircularPath = new Path();
                    this.mWindowManager.getDefaultDisplay().getRealSize(this.mTempPoint);
                    int centerXY = this.mTempPoint.x / 2;
                    this.mCircularPath.addCircle((float) centerXY, (float) centerXY, (float) centerXY, Direction.CW);
                } else {
                    this.mCircularPath = null;
                }
                recomputeBoundsLocked();
            }

            public void getMagnificationRegionLocked(Region outMagnificationRegion) {
                outMagnificationRegion.set(this.mMagnificationRegion);
            }

            public void updateMagnificationSpecLocked(MagnificationSpec spec) {
                if (spec != null) {
                    this.mMagnificationSpec.initialize(spec.scale, spec.offsetX, spec.offsetY);
                } else {
                    this.mMagnificationSpec.clear();
                }
                if (!DisplayMagnifier.this.mHandler.hasMessages(5)) {
                    boolean z = isMagnifyingLocked() || DisplayMagnifier.this.isForceShowingMagnifiableBoundsLocked();
                    setMagnifiedRegionBorderShownLocked(z, true);
                }
            }

            public void recomputeBoundsLocked() {
                Region accountedBounds;
                this.mWindowManager.getDefaultDisplay().getRealSize(this.mTempPoint);
                int screenWidth = this.mTempPoint.x;
                int screenHeight = this.mTempPoint.y;
                this.mMagnificationRegion.set(0, 0, 0, 0);
                Region availableBounds = DisplayMagnifier.this.mTempRegion1;
                availableBounds.set(0, 0, screenWidth, screenHeight);
                if (this.mCircularPath != null) {
                    availableBounds.setPath(this.mCircularPath, availableBounds);
                }
                Region nonMagnifiedBounds = DisplayMagnifier.this.mTempRegion4;
                nonMagnifiedBounds.set(0, 0, 0, 0);
                SparseArray<WindowState> visibleWindows = this.mTempWindowStates;
                visibleWindows.clear();
                populateWindowsOnScreenLocked(visibleWindows);
                int i = visibleWindows.size() - 1;
                while (true) {
                    int i2 = i;
                    if (i2 < 0) {
                        break;
                    }
                    WindowState windowState = (WindowState) visibleWindows.valueAt(i2);
                    if (windowState.mAttrs.type != 2027 && (windowState.mAttrs.privateFlags & DumpState.DUMP_DEXOPT) == 0) {
                        Matrix matrix = this.mTempMatrix;
                        AccessibilityController.populateTransformationMatrixLocked(windowState, matrix);
                        Region touchableRegion = DisplayMagnifier.this.mTempRegion3;
                        windowState.getTouchableRegion(touchableRegion);
                        Rect touchableFrame = DisplayMagnifier.this.mTempRect1;
                        touchableRegion.getBounds(touchableFrame);
                        RectF windowFrame = this.mTempRectF;
                        windowFrame.set(touchableFrame);
                        windowFrame.offset((float) (-windowState.mFrame.left), (float) (-windowState.mFrame.top));
                        matrix.mapRect(windowFrame);
                        Region windowBounds = DisplayMagnifier.this.mTempRegion2;
                        windowBounds.set((int) windowFrame.left, (int) windowFrame.top, (int) windowFrame.right, (int) windowFrame.bottom);
                        Region portionOfWindowAlreadyAccountedFor = DisplayMagnifier.this.mTempRegion3;
                        portionOfWindowAlreadyAccountedFor.set(this.mMagnificationRegion);
                        portionOfWindowAlreadyAccountedFor.op(nonMagnifiedBounds, Op.UNION);
                        windowBounds.op(portionOfWindowAlreadyAccountedFor, Op.DIFFERENCE);
                        if (windowState.shouldMagnify()) {
                            this.mMagnificationRegion.op(windowBounds, Op.UNION);
                            this.mMagnificationRegion.op(availableBounds, Op.INTERSECT);
                        } else {
                            nonMagnifiedBounds.op(windowBounds, Op.UNION);
                            availableBounds.op(windowBounds, Op.DIFFERENCE);
                        }
                        touchableRegion = DisplayMagnifier.this.mTempRegion2;
                        touchableRegion.set(this.mMagnificationRegion);
                        touchableRegion.op(nonMagnifiedBounds, Op.UNION);
                        Region accountedBounds2 = touchableRegion;
                        touchableRegion.op(0, 0, screenWidth, screenHeight, Op.INTERSECT);
                        accountedBounds = accountedBounds2;
                        if (accountedBounds.isRect()) {
                            Rect accountedFrame = DisplayMagnifier.this.mTempRect1;
                            accountedBounds.getBounds(accountedFrame);
                            if (accountedFrame.width() == screenWidth && accountedFrame.height() == screenHeight) {
                                break;
                            }
                        }
                        continue;
                    }
                    i = i2 - 1;
                }
                visibleWindows.clear();
                accountedBounds = this.mMagnificationRegion;
                int i3 = this.mDrawBorderInset;
                accountedBounds.op(i3, this.mDrawBorderInset, screenWidth - this.mDrawBorderInset, screenHeight - this.mDrawBorderInset, Op.INTERSECT);
                if (this.mOldMagnificationRegion.equals(this.mMagnificationRegion) ^ true) {
                    this.mWindow.setBounds(this.mMagnificationRegion);
                    Rect dirtyRect = DisplayMagnifier.this.mTempRect1;
                    if (this.mFullRedrawNeeded) {
                        this.mFullRedrawNeeded = false;
                        dirtyRect.set(this.mDrawBorderInset, this.mDrawBorderInset, screenWidth - this.mDrawBorderInset, screenHeight - this.mDrawBorderInset);
                        this.mWindow.invalidate(dirtyRect);
                    } else {
                        Region dirtyRegion = DisplayMagnifier.this.mTempRegion3;
                        dirtyRegion.set(this.mMagnificationRegion);
                        dirtyRegion.op(this.mOldMagnificationRegion, Op.UNION);
                        dirtyRegion.op(nonMagnifiedBounds, Op.INTERSECT);
                        dirtyRegion.getBounds(dirtyRect);
                        this.mWindow.invalidate(dirtyRect);
                    }
                    this.mOldMagnificationRegion.set(this.mMagnificationRegion);
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = Region.obtain(this.mMagnificationRegion);
                    DisplayMagnifier.this.mHandler.obtainMessage(1, args).sendToTarget();
                }
            }

            public void onRotationChangedLocked() {
                if (isMagnifyingLocked() || DisplayMagnifier.this.isForceShowingMagnifiableBoundsLocked()) {
                    setMagnifiedRegionBorderShownLocked(false, false);
                    long delay = (long) (((float) DisplayMagnifier.this.mLongAnimationDuration) * DisplayMagnifier.this.mService.getWindowAnimationScaleLocked());
                    DisplayMagnifier.this.mHandler.sendMessageDelayed(DisplayMagnifier.this.mHandler.obtainMessage(5), delay);
                }
                recomputeBoundsLocked();
                this.mWindow.updateSize();
            }

            public void setMagnifiedRegionBorderShownLocked(boolean shown, boolean animate) {
                if (shown) {
                    this.mFullRedrawNeeded = true;
                    this.mOldMagnificationRegion.set(0, 0, 0, 0);
                }
                this.mWindow.setShown(shown, animate);
            }

            public void getMagnifiedFrameInContentCoordsLocked(Rect rect) {
                MagnificationSpec spec = this.mMagnificationSpec;
                this.mMagnificationRegion.getBounds(rect);
                rect.offset((int) (-spec.offsetX), (int) (-spec.offsetY));
                rect.scale(1.0f / spec.scale);
            }

            public boolean isMagnifyingLocked() {
                return this.mMagnificationSpec.scale > 1.0f;
            }

            public MagnificationSpec getMagnificationSpecLocked() {
                return this.mMagnificationSpec;
            }

            public void drawWindowIfNeededLocked() {
                recomputeBoundsLocked();
                this.mWindow.drawIfNeeded();
            }

            public void destroyWindow() {
                this.mWindow.releaseSurface();
            }

            private void populateWindowsOnScreenLocked(SparseArray<WindowState> outWindows) {
                DisplayContent dc = DisplayMagnifier.this.mService.getDefaultDisplayContentLocked();
                if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && DisplayMagnifier.this.mService.mRoot != null) {
                    dc = DisplayMagnifier.this.mService.mRoot.getDisplayContent(HwPCUtils.getPCDisplayID());
                }
                this.mTempLayer = 0;
                dc.forAllWindows((Consumer) new -$$Lambda$AccessibilityController$DisplayMagnifier$MagnifiedViewport$ZNyFGy-UXiWV1D2yZGvH-9qN0AA(this, outWindows), false);
            }

            public static /* synthetic */ void lambda$populateWindowsOnScreenLocked$0(MagnifiedViewport magnifiedViewport, SparseArray outWindows, WindowState w) {
                if (w.isOnScreen() && w.isVisibleLw() && w.mAttrs.alpha != 0.0f && !w.mWinAnimator.mEnterAnimationPending) {
                    magnifiedViewport.mTempLayer++;
                    outWindows.put(magnifiedViewport.mTempLayer, w);
                }
            }
        }

        private class MyHandler extends Handler {
            public static final int MESSAGE_NOTIFY_MAGNIFICATION_REGION_CHANGED = 1;
            public static final int MESSAGE_NOTIFY_RECTANGLE_ON_SCREEN_REQUESTED = 2;
            public static final int MESSAGE_NOTIFY_ROTATION_CHANGED = 4;
            public static final int MESSAGE_NOTIFY_USER_CONTEXT_CHANGED = 3;
            public static final int MESSAGE_SHOW_MAGNIFIED_REGION_BOUNDS_IF_NEEDED = 5;

            public MyHandler(Looper looper) {
                super(looper);
            }

            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        Region magnifiedBounds = ((SomeArgs) message.obj).arg1;
                        DisplayMagnifier.this.mCallbacks.onMagnificationRegionChanged(magnifiedBounds);
                        magnifiedBounds.recycle();
                        return;
                    case 2:
                        SomeArgs args = message.obj;
                        DisplayMagnifier.this.mCallbacks.onRectangleOnScreenRequested(args.argi1, args.argi2, args.argi3, args.argi4);
                        args.recycle();
                        return;
                    case 3:
                        DisplayMagnifier.this.mCallbacks.onUserContextChanged();
                        return;
                    case 4:
                        DisplayMagnifier.this.mCallbacks.onRotationChanged(message.arg1);
                        return;
                    case 5:
                        synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                if (DisplayMagnifier.this.mMagnifedViewport.isMagnifyingLocked() || DisplayMagnifier.this.isForceShowingMagnifiableBoundsLocked()) {
                                    DisplayMagnifier.this.mMagnifedViewport.setMagnifiedRegionBorderShownLocked(true, true);
                                    DisplayMagnifier.this.mService.scheduleAnimationLocked();
                                }
                            } finally {
                                while (true) {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    break;
                                }
                            }
                        }
                        return;
                    default:
                        return;
                }
            }
        }

        public DisplayMagnifier(WindowManagerService windowManagerService, MagnificationCallbacks callbacks) {
            this.mContext = windowManagerService.mContext;
            this.mService = windowManagerService;
            this.mCallbacks = callbacks;
            this.mHandler = new MyHandler(this.mService.mH.getLooper());
            this.mMagnifedViewport = new MagnifiedViewport();
            this.mLongAnimationDuration = (long) this.mContext.getResources().getInteger(17694722);
        }

        public void setMagnificationSpecLocked(MagnificationSpec spec) {
            this.mMagnifedViewport.updateMagnificationSpecLocked(spec);
            this.mMagnifedViewport.recomputeBoundsLocked();
            this.mService.applyMagnificationSpec(spec);
            this.mService.scheduleAnimationLocked();
        }

        public void setForceShowMagnifiableBoundsLocked(boolean show) {
            this.mForceShowMagnifiableBounds = show;
            this.mMagnifedViewport.setMagnifiedRegionBorderShownLocked(show, true);
        }

        public boolean isForceShowingMagnifiableBoundsLocked() {
            return this.mForceShowMagnifiableBounds;
        }

        public void onRectangleOnScreenRequestedLocked(Rect rectangle) {
            if (this.mMagnifedViewport.isMagnifyingLocked()) {
                Rect magnifiedRegionBounds = this.mTempRect2;
                this.mMagnifedViewport.getMagnifiedFrameInContentCoordsLocked(magnifiedRegionBounds);
                if (!magnifiedRegionBounds.contains(rectangle)) {
                    SomeArgs args = SomeArgs.obtain();
                    args.argi1 = rectangle.left;
                    args.argi2 = rectangle.top;
                    args.argi3 = rectangle.right;
                    args.argi4 = rectangle.bottom;
                    this.mHandler.obtainMessage(2, args).sendToTarget();
                }
            }
        }

        public void onWindowLayersChangedLocked() {
            this.mMagnifedViewport.recomputeBoundsLocked();
            this.mService.scheduleAnimationLocked();
        }

        public void onRotationChangedLocked(DisplayContent displayContent) {
            this.mMagnifedViewport.onRotationChangedLocked();
            this.mHandler.sendEmptyMessage(4);
        }

        public void onAppWindowTransitionLocked(WindowState windowState, int transition) {
            if (this.mMagnifedViewport.isMagnifyingLocked()) {
                if (!(transition == 6 || transition == 8 || transition == 10)) {
                    switch (transition) {
                        case 12:
                        case 13:
                        case 14:
                            break;
                        default:
                            return;
                    }
                }
                this.mHandler.sendEmptyMessage(3);
            }
        }

        public void onWindowTransitionLocked(WindowState windowState, int transition) {
            boolean magnifying = this.mMagnifedViewport.isMagnifyingLocked();
            int type = windowState.mAttrs.type;
            if ((transition == 1 || transition == 3) && magnifying) {
                if (!(type == 2 || type == 4 || type == 1005 || type == 2020 || type == 2024 || type == 2035 || type == 2038)) {
                    switch (type) {
                        case 1000:
                        case NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE /*1001*/:
                        case 1002:
                        case 1003:
                            break;
                        default:
                            switch (type) {
                                case 2001:
                                case 2002:
                                case 2003:
                                    break;
                                default:
                                    switch (type) {
                                        case 2005:
                                        case 2006:
                                        case 2007:
                                        case 2008:
                                        case 2009:
                                        case 2010:
                                            break;
                                        default:
                                            return;
                                    }
                            }
                    }
                }
                Rect magnifiedRegionBounds = this.mTempRect2;
                this.mMagnifedViewport.getMagnifiedFrameInContentCoordsLocked(magnifiedRegionBounds);
                Rect touchableRegionBounds = this.mTempRect1;
                windowState.getTouchableRegion(this.mTempRegion1);
                this.mTempRegion1.getBounds(touchableRegionBounds);
                if (!magnifiedRegionBounds.intersect(touchableRegionBounds)) {
                    this.mCallbacks.onRectangleOnScreenRequested(touchableRegionBounds.left, touchableRegionBounds.top, touchableRegionBounds.right, touchableRegionBounds.bottom);
                }
            }
        }

        public MagnificationSpec getMagnificationSpecForWindowLocked(WindowState windowState) {
            MagnificationSpec spec = this.mMagnifedViewport.getMagnificationSpecLocked();
            if (spec == null || spec.isNop() || windowState.shouldMagnify()) {
                return spec;
            }
            return null;
        }

        public void getMagnificationRegionLocked(Region outMagnificationRegion) {
            this.mMagnifedViewport.recomputeBoundsLocked();
            this.mMagnifedViewport.getMagnificationRegionLocked(outMagnificationRegion);
        }

        public void destroyLocked() {
            this.mMagnifedViewport.destroyWindow();
        }

        public void showMagnificationBoundsIfNeeded() {
            this.mHandler.obtainMessage(5).sendToTarget();
        }

        public void drawMagnifiedRegionBorderIfNeededLocked() {
            this.mMagnifedViewport.drawWindowIfNeededLocked();
        }
    }

    private static final class WindowsForAccessibilityObserver {
        private static final boolean DEBUG = false;
        private static final String LOG_TAG = "WindowManager";
        private final WindowsForAccessibilityCallback mCallback;
        private final Context mContext;
        private final Handler mHandler;
        private final List<WindowInfo> mOldWindows = new ArrayList();
        private final long mRecurringAccessibilityEventsIntervalMillis;
        private final WindowManagerService mService;
        private final Set<IBinder> mTempBinderSet = new ArraySet();
        private int mTempLayer = 0;
        private final Matrix mTempMatrix = new Matrix();
        private final Point mTempPoint = new Point();
        private final Rect mTempRect = new Rect();
        private final RectF mTempRectF = new RectF();
        private final Region mTempRegion = new Region();
        private final Region mTempRegion1 = new Region();
        private final SparseArray<WindowState> mTempWindowStates = new SparseArray();

        private class MyHandler extends Handler {
            public static final int MESSAGE_COMPUTE_CHANGED_WINDOWS = 1;

            public MyHandler(Looper looper) {
                super(looper, null, false);
            }

            public void handleMessage(Message message) {
                if (message.what == 1) {
                    WindowsForAccessibilityObserver.this.computeChangedWindows();
                }
            }
        }

        public WindowsForAccessibilityObserver(WindowManagerService windowManagerService, WindowsForAccessibilityCallback callback) {
            this.mContext = windowManagerService.mContext;
            this.mService = windowManagerService;
            this.mCallback = callback;
            this.mHandler = new MyHandler(this.mService.mH.getLooper());
            this.mRecurringAccessibilityEventsIntervalMillis = ViewConfiguration.getSendRecurringAccessibilityEventsInterval();
            computeChangedWindows();
        }

        public void performComputeChangedWindowsNotLocked() {
            this.mHandler.removeMessages(1);
            computeChangedWindows();
        }

        public void scheduleComputeChangedWindowsLocked() {
            if (!this.mHandler.hasMessages(1)) {
                this.mHandler.sendEmptyMessageDelayed(1, this.mRecurringAccessibilityEventsIntervalMillis);
            }
        }

        /* JADX WARNING: Missing block: B:24:0x008c, code:
            if (r15.mAttrs.type != 2034) goto L_0x009d;
     */
        /* JADX WARNING: Missing block: B:85:0x019e, code:
            com.android.server.wm.WindowManagerService.resetPriorityAfterLockedSection();
     */
        /* JADX WARNING: Missing block: B:86:0x01a1, code:
            if (r2 == false) goto L_0x01a8;
     */
        /* JADX WARNING: Missing block: B:87:0x01a3, code:
            r1.mCallback.onWindowsForAccessibilityChanged(r3);
     */
        /* JADX WARNING: Missing block: B:88:0x01a8, code:
            clearAndRecycleWindows(r3);
     */
        /* JADX WARNING: Missing block: B:89:0x01ab, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void computeChangedWindows() {
            Throwable th;
            boolean windowsChanged = false;
            ArrayList windows = new ArrayList();
            synchronized (this.mService.mWindowMap) {
                boolean windowsChanged2;
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (this.mService.mCurrentFocus == null) {
                        try {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    int flags;
                    int screenWidth;
                    int screenHeight;
                    int i;
                    int i2;
                    WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
                    windowManager.getDefaultDisplay().getRealSize(this.mTempPoint);
                    int screenWidth2 = this.mTempPoint.x;
                    int screenHeight2 = this.mTempPoint.y;
                    Region unaccountedSpace = this.mTempRegion;
                    unaccountedSpace.set(0, 0, screenWidth2, screenHeight2);
                    SparseArray<WindowState> visibleWindows = this.mTempWindowStates;
                    populateVisibleWindowsOnScreenLocked(visibleWindows);
                    Set<IBinder> addedWindows = this.mTempBinderSet;
                    addedWindows.clear();
                    boolean focusedWindowAdded = false;
                    int visibleWindowCount = visibleWindows.size();
                    HashSet<Integer> skipRemainingWindowsForTasks = new HashSet();
                    int i3 = visibleWindowCount - 1;
                    while (i3 >= 0) {
                        WindowState windowState = (WindowState) visibleWindows.valueAt(i3);
                        flags = windowState.mAttrs.flags;
                        WindowManager windowManager2 = windowManager;
                        Task windowManager3 = windowState.getTask();
                        if (windowManager3 != null) {
                            windowsChanged2 = windowsChanged;
                            try {
                                if (skipRemainingWindowsForTasks.contains(Integer.valueOf(windowManager3.mTaskId))) {
                                    screenWidth = screenWidth2;
                                    screenHeight = screenHeight2;
                                    i3--;
                                    windowManager = windowManager2;
                                    windowsChanged = windowsChanged2;
                                    screenWidth2 = screenWidth;
                                    screenHeight2 = screenHeight;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                windowsChanged = windowsChanged2;
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        windowsChanged2 = windowsChanged;
                        if ((flags & 16) != 0) {
                            screenWidth = screenWidth2;
                        } else {
                            screenWidth = screenWidth2;
                        }
                        Rect boundsInScreen = this.mTempRect;
                        computeWindowBoundsInScreen(windowState, boundsInScreen);
                        if (!unaccountedSpace.quickReject(boundsInScreen)) {
                            if (isReportedWindowType(windowState.mAttrs.type)) {
                                addPopulatedWindowInfo(windowState, boundsInScreen, windows, addedWindows);
                                if (windowState.isFocused()) {
                                    focusedWindowAdded = true;
                                }
                            }
                            screenHeight = screenHeight2;
                            if (windowState.mAttrs.type != 2032) {
                                unaccountedSpace.op(boundsInScreen, unaccountedSpace, Op.REVERSE_DIFFERENCE);
                                if ((flags & 40) == 0) {
                                    unaccountedSpace.op(windowState.getDisplayFrameLw(), unaccountedSpace, Op.REVERSE_DIFFERENCE);
                                    if (windowManager3 == null) {
                                        break;
                                    }
                                    skipRemainingWindowsForTasks.add(Integer.valueOf(windowManager3.mTaskId));
                                    i3--;
                                    windowManager = windowManager2;
                                    windowsChanged = windowsChanged2;
                                    screenWidth2 = screenWidth;
                                    screenHeight2 = screenHeight;
                                }
                            }
                            if (unaccountedSpace.isEmpty()) {
                                break;
                            }
                            i3--;
                            windowManager = windowManager2;
                            windowsChanged = windowsChanged2;
                            screenWidth2 = screenWidth;
                            screenHeight2 = screenHeight;
                        }
                        screenHeight = screenHeight2;
                        i3--;
                        windowManager = windowManager2;
                        windowsChanged = windowsChanged2;
                        screenWidth2 = screenWidth;
                        screenHeight2 = screenHeight;
                    }
                    windowsChanged2 = windowsChanged;
                    screenWidth = screenWidth2;
                    screenHeight = screenHeight2;
                    if (!focusedWindowAdded) {
                        for (i = visibleWindowCount - 1; i >= 0; i--) {
                            WindowState windowState2 = (WindowState) visibleWindows.valueAt(i);
                            if (windowState2.isFocused()) {
                                Rect boundsInScreen2 = this.mTempRect;
                                computeWindowBoundsInScreen(windowState2, boundsInScreen2);
                                addPopulatedWindowInfo(windowState2, boundsInScreen2, windows, addedWindows);
                                break;
                            }
                        }
                    }
                    i = windows.size();
                    for (i2 = 0; i2 < i; i2++) {
                        WindowInfo window = (WindowInfo) windows.get(i2);
                        if (!addedWindows.contains(window.parentToken)) {
                            window.parentToken = null;
                        }
                        if (window.childTokens != null) {
                            for (flags = window.childTokens.size() - 1; flags >= 0; flags--) {
                                if (!addedWindows.contains(window.childTokens.get(flags))) {
                                    window.childTokens.remove(flags);
                                }
                            }
                        }
                    }
                    visibleWindows.clear();
                    addedWindows.clear();
                    if (this.mOldWindows.size() != windows.size()) {
                        windowsChanged = true;
                    } else {
                        if (!this.mOldWindows.isEmpty() || !windows.isEmpty()) {
                            int i4 = 0;
                            while (true) {
                                i2 = i4;
                                if (i2 >= i) {
                                    break;
                                } else if (windowChangedNoLayer((WindowInfo) this.mOldWindows.get(i2), (WindowInfo) windows.get(i2))) {
                                    windowsChanged = true;
                                    break;
                                } else {
                                    i4 = i2 + 1;
                                }
                            }
                        }
                        windowsChanged = windowsChanged2;
                    }
                    if (windowsChanged) {
                        cacheWindows(windows);
                    }
                } catch (Throwable th4) {
                    th = th4;
                    windowsChanged2 = false;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        private void computeWindowBoundsInScreen(WindowState windowState, Rect outBounds) {
            Region touchableRegion = this.mTempRegion1;
            windowState.getTouchableRegion(touchableRegion);
            Rect touchableFrame = this.mTempRect;
            touchableRegion.getBounds(touchableFrame);
            RectF windowFrame = this.mTempRectF;
            windowFrame.set(touchableFrame);
            windowFrame.offset((float) (-windowState.mFrame.left), (float) (-windowState.mFrame.top));
            Matrix matrix = this.mTempMatrix;
            AccessibilityController.populateTransformationMatrixLocked(windowState, matrix);
            matrix.mapRect(windowFrame);
            outBounds.set((int) windowFrame.left, (int) windowFrame.top, (int) windowFrame.right, (int) windowFrame.bottom);
        }

        private static void addPopulatedWindowInfo(WindowState windowState, Rect boundsInScreen, List<WindowInfo> out, Set<IBinder> tokenOut) {
            WindowInfo window = windowState.getWindowInfo();
            window.boundsInScreen.set(boundsInScreen);
            window.layer = tokenOut.size();
            out.add(window);
            tokenOut.add(window.token);
        }

        private void cacheWindows(List<WindowInfo> windows) {
            int i;
            for (i = this.mOldWindows.size() - 1; i >= 0; i--) {
                ((WindowInfo) this.mOldWindows.remove(i)).recycle();
            }
            i = windows.size();
            for (int i2 = 0; i2 < i; i2++) {
                this.mOldWindows.add(WindowInfo.obtain((WindowInfo) windows.get(i2)));
            }
        }

        private boolean windowChangedNoLayer(WindowInfo oldWindow, WindowInfo newWindow) {
            if (oldWindow == newWindow) {
                return false;
            }
            if (oldWindow == null || newWindow == null || oldWindow.type != newWindow.type || oldWindow.focused != newWindow.focused) {
                return true;
            }
            if (oldWindow.token == null) {
                if (newWindow.token != null) {
                    return true;
                }
            } else if (!oldWindow.token.equals(newWindow.token)) {
                return true;
            }
            if (oldWindow.parentToken == null) {
                if (newWindow.parentToken != null) {
                    return true;
                }
            } else if (!oldWindow.parentToken.equals(newWindow.parentToken)) {
                return true;
            }
            if (!oldWindow.boundsInScreen.equals(newWindow.boundsInScreen)) {
                return true;
            }
            if ((oldWindow.childTokens == null || newWindow.childTokens == null || oldWindow.childTokens.equals(newWindow.childTokens)) && TextUtils.equals(oldWindow.title, newWindow.title) && oldWindow.accessibilityIdOfAnchor == newWindow.accessibilityIdOfAnchor) {
                return false;
            }
            return true;
        }

        private static void clearAndRecycleWindows(List<WindowInfo> windows) {
            for (int i = windows.size() - 1; i >= 0; i--) {
                ((WindowInfo) windows.remove(i)).recycle();
            }
        }

        private static boolean isReportedWindowType(int windowType) {
            return (windowType == 2013 || windowType == 2021 || windowType == 2026 || windowType == 2016 || windowType == 2022 || windowType == 2018 || windowType == 2027 || windowType == 1004 || windowType == 2015 || windowType == 2030) ? false : true;
        }

        private void populateVisibleWindowsOnScreenLocked(SparseArray<WindowState> outWindows) {
            DisplayContent dc = this.mService.getDefaultDisplayContentLocked();
            if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && this.mService.mRoot != null) {
                dc = this.mService.mRoot.getDisplayContent(HwPCUtils.getPCDisplayID());
            }
            this.mTempLayer = 0;
            dc.forAllWindows((Consumer) new -$$Lambda$AccessibilityController$WindowsForAccessibilityObserver$vRhBz0DqTZWNemKfoIyId7HacTk(this, outWindows), false);
        }

        public static /* synthetic */ void lambda$populateVisibleWindowsOnScreenLocked$0(WindowsForAccessibilityObserver windowsForAccessibilityObserver, SparseArray outWindows, WindowState w) {
            if (w.isVisibleLw()) {
                int i = windowsForAccessibilityObserver.mTempLayer;
                windowsForAccessibilityObserver.mTempLayer = i + 1;
                outWindows.put(i, w);
            }
        }
    }

    public AccessibilityController(WindowManagerService service) {
        this.mService = service;
    }

    public void setMagnificationCallbacksLocked(MagnificationCallbacks callbacks) {
        if (callbacks != null) {
            if (this.mDisplayMagnifier == null) {
                this.mDisplayMagnifier = new DisplayMagnifier(this.mService, callbacks);
                return;
            }
            throw new IllegalStateException("Magnification callbacks already set!");
        } else if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.destroyLocked();
            this.mDisplayMagnifier = null;
        } else {
            throw new IllegalStateException("Magnification callbacks already cleared!");
        }
    }

    public void setWindowsForAccessibilityCallback(WindowsForAccessibilityCallback callback) {
        if (callback != null) {
            if (this.mWindowsForAccessibilityObserver == null) {
                this.mWindowsForAccessibilityObserver = new WindowsForAccessibilityObserver(this.mService, callback);
                return;
            }
            throw new IllegalStateException("Windows for accessibility callback already set!");
        } else if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver = null;
        } else {
            throw new IllegalStateException("Windows for accessibility callback already cleared!");
        }
    }

    public void performComputeChangedWindowsNotLocked() {
        WindowsForAccessibilityObserver observer;
        synchronized (this.mService) {
            observer = this.mWindowsForAccessibilityObserver;
        }
        if (observer != null) {
            observer.performComputeChangedWindowsNotLocked();
        }
    }

    public void setMagnificationSpecLocked(MagnificationSpec spec) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.setMagnificationSpecLocked(spec);
        }
        if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
        }
    }

    public void getMagnificationRegionLocked(Region outMagnificationRegion) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.getMagnificationRegionLocked(outMagnificationRegion);
        }
    }

    public void onRectangleOnScreenRequestedLocked(Rect rectangle) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onRectangleOnScreenRequestedLocked(rectangle);
        }
    }

    public void onWindowLayersChangedLocked() {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onWindowLayersChangedLocked();
        }
        if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
        }
    }

    public void onRotationChangedLocked(DisplayContent displayContent) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onRotationChangedLocked(displayContent);
        }
        if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
        }
    }

    public void onAppWindowTransitionLocked(WindowState windowState, int transition) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onAppWindowTransitionLocked(windowState, transition);
        }
    }

    public void onWindowTransitionLocked(WindowState windowState, int transition) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onWindowTransitionLocked(windowState, transition);
        }
        if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
        }
    }

    public void onWindowFocusChangedNotLocked() {
        WindowsForAccessibilityObserver observer;
        synchronized (this.mService) {
            observer = this.mWindowsForAccessibilityObserver;
        }
        if (observer != null) {
            observer.performComputeChangedWindowsNotLocked();
        }
    }

    public void onSomeWindowResizedOrMovedLocked() {
        if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
        }
    }

    public void drawMagnifiedRegionBorderIfNeededLocked() {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.drawMagnifiedRegionBorderIfNeededLocked();
        }
    }

    public MagnificationSpec getMagnificationSpecForWindowLocked(WindowState windowState) {
        if (this.mDisplayMagnifier != null) {
            return this.mDisplayMagnifier.getMagnificationSpecForWindowLocked(windowState);
        }
        return null;
    }

    public boolean hasCallbacksLocked() {
        return (this.mDisplayMagnifier == null && this.mWindowsForAccessibilityObserver == null) ? false : true;
    }

    public void setForceShowMagnifiableBoundsLocked(boolean show) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.setForceShowMagnifiableBoundsLocked(show);
            this.mDisplayMagnifier.showMagnificationBoundsIfNeeded();
        }
    }

    private static void populateTransformationMatrixLocked(WindowState windowState, Matrix outMatrix) {
        windowState.getTransformationMatrix(sTempFloats, outMatrix);
    }
}
