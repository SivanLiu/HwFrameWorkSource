package com.android.internal.policy;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.WindowConfiguration;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.hwcontrol.HwWidgetFactory;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.util.HwSlog;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ActionMode.Callback2;
import android.view.ContextThemeWrapper;
import android.view.DisplayListCanvas;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewOutlineProvider;
import android.view.ViewStub;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.Window;
import android.view.Window.WindowControllerCallback;
import android.view.WindowCallbacks;
import android.view.WindowInsets;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import com.android.internal.colorextraction.types.Tonal;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.policy.PhoneWindow.PhoneWindowMenuCallback;
import com.android.internal.telephony.AbstractRILConstants;
import com.android.internal.util.Protocol;
import com.android.internal.view.FloatingActionMode;
import com.android.internal.view.RootViewSurfaceTaker;
import com.android.internal.view.StandaloneActionMode;
import com.android.internal.view.menu.ContextMenuBuilder;
import com.android.internal.view.menu.MenuHelper;
import com.android.internal.widget.AbsHwDecorCaptionView;
import com.android.internal.widget.ActionBarContextView;
import com.android.internal.widget.BackgroundFallback;
import com.android.internal.widget.DecorCaptionView;
import com.android.internal.widget.FloatingToolbar;
import java.util.List;

public class DecorView extends FrameLayout implements RootViewSurfaceTaker, WindowCallbacks {
    static final boolean DEBUG_IMMERSION = false;
    private static final boolean DEBUG_MEASURE = false;
    private static final int DECOR_SHADOW_FOCUSED_HEIGHT_IN_DIP = 20;
    private static final int DECOR_SHADOW_UNFOCUSED_HEIGHT_IN_DIP = 5;
    public static final ColorViewAttributes NAVIGATION_BAR_COLOR_VIEW_ATTRIBUTES = new ColorViewAttributes(2, 134217728, 80, 5, 3, "android:navigation:background", 16908336, 0, null);
    private static final String PERMISSION_USE_SMARTKEY = "huawei.permission.USE_SMARTKEY";
    private static final ViewOutlineProvider PIP_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
            outline.setAlpha(1.0f);
        }
    };
    public static final ColorViewAttributes STATUS_BAR_COLOR_VIEW_ATTRIBUTES = new ColorViewAttributes(4, 67108864, 48, 3, 5, "android:status:background", 16908335, 1024, null);
    private static final boolean SWEEP_OPEN_MENU = false;
    private static final String TAG = "DecorView";
    private boolean mAllowUpdateElevation = false;
    private boolean mApplyFloatingHorizontalInsets = false;
    private boolean mApplyFloatingVerticalInsets = false;
    private float mAvailableWidth;
    private BackdropFrameRenderer mBackdropFrameRenderer = null;
    private final BackgroundFallback mBackgroundFallback = new BackgroundFallback();
    private final Rect mBackgroundPadding = new Rect();
    private final int mBarEnterExitDuration;
    private Drawable mCaptionBackgroundDrawable;
    private boolean mChanging;
    ViewGroup mContentRoot;
    DecorCaptionView mDecorCaptionView;
    int mDefaultOpacity = -1;
    private int mDownY;
    private final Rect mDrawingBounds = new Rect();
    private boolean mElevationAdjustedForStack = false;
    private ObjectAnimator mFadeAnim;
    private final int mFeatureId;
    private ActionMode mFloatingActionMode;
    private View mFloatingActionModeOriginatingView;
    private final Rect mFloatingInsets = new Rect();
    private FloatingToolbar mFloatingToolbar;
    private OnPreDrawListener mFloatingToolbarPreDrawListener;
    final boolean mForceWindowDrawsStatusBarBackground;
    private boolean mForcedDrawSysBarBackground = false;
    private final Rect mFrameOffsets = new Rect();
    private final Rect mFramePadding = new Rect();
    private boolean mHasCaption = false;
    private final Interpolator mHideInterpolator;
    private final Paint mHorizontalResizeShadowPaint = new Paint();
    private boolean mIsHandlingTouchEvent = false;
    private boolean mIsInPictureInPictureMode;
    private Callback mLastBackgroundDrawableCb = null;
    private int mLastBottomInset = 0;
    private boolean mLastHasBottomStableInset = false;
    private boolean mLastHasLeftStableInset = false;
    private boolean mLastHasRightStableInset = false;
    private boolean mLastHasTopStableInset = false;
    private int mLastLeftInset = 0;
    private ViewOutlineProvider mLastOutlineProvider;
    private int mLastRightInset = 0;
    private boolean mLastShouldAlwaysConsumeNavBar = false;
    private int mLastTopInset = 0;
    private int mLastWindowFlags = 0;
    String mLogTag = TAG;
    private Drawable mMenuBackground;
    private final ColorViewState mNavigationColorViewState = new ColorViewState(NAVIGATION_BAR_COLOR_VIEW_ATTRIBUTES);
    private Rect mOutsets = new Rect();
    private final IPressGestureDetector mPressGestureDetector;
    ActionMode mPrimaryActionMode;
    private PopupWindow mPrimaryActionModePopup;
    private ActionBarContextView mPrimaryActionModeView;
    private int mResizeMode = -1;
    private final int mResizeShadowSize;
    private Drawable mResizingBackgroundDrawable;
    private int mRootScrollY = 0;
    private final int mSemiTransparentStatusBarColor;
    private final Interpolator mShowInterpolator;
    private Runnable mShowPrimaryActionModePopup;
    private final ColorViewState mStatusColorViewState = new ColorViewState(STATUS_BAR_COLOR_VIEW_ATTRIBUTES);
    private View mStatusGuard;
    private Rect mTempRect;
    private Drawable mUserCaptionBackgroundDrawable;
    private final Paint mVerticalResizeShadowPaint = new Paint();
    private boolean mWatchingForMenu;
    private PhoneWindow mWindow;
    private boolean mWindowResizeCallbacksAdded = false;

    public static class ColorViewAttributes {
        final int hideWindowFlag;
        final int horizontalGravity;
        final int id;
        final int seascapeGravity;
        final int systemUiHideFlag;
        final String transitionName;
        final int translucentFlag;
        final int verticalGravity;

        /* synthetic */ ColorViewAttributes(int x0, int x1, int x2, int x3, int x4, String x5, int x6, int x7, AnonymousClass1 x8) {
            this(x0, x1, x2, x3, x4, x5, x6, x7);
        }

        private ColorViewAttributes(int systemUiHideFlag, int translucentFlag, int verticalGravity, int horizontalGravity, int seascapeGravity, String transitionName, int id, int hideWindowFlag) {
            this.id = id;
            this.systemUiHideFlag = systemUiHideFlag;
            this.translucentFlag = translucentFlag;
            this.verticalGravity = verticalGravity;
            this.horizontalGravity = horizontalGravity;
            this.seascapeGravity = seascapeGravity;
            this.transitionName = transitionName;
            this.hideWindowFlag = hideWindowFlag;
        }

        public boolean isPresentHW(int sysUiVis, int windowFlags, boolean force, boolean isEmui) {
            if (!isEmui) {
                return isPresent(sysUiVis, windowFlags, force);
            }
            boolean z = (this.systemUiHideFlag & sysUiVis) == 0 && (this.hideWindowFlag & windowFlags) == 0;
            return z;
        }

        public boolean isPresent(int sysUiVis, int windowFlags, boolean force) {
            return (this.systemUiHideFlag & sysUiVis) == 0 && (this.hideWindowFlag & windowFlags) == 0 && ((Integer.MIN_VALUE & windowFlags) != 0 || force);
        }

        public boolean isVisible(boolean present, int color, int windowFlags, boolean force) {
            return present && (Tonal.MAIN_COLOR_DARK & color) != 0 && ((this.translucentFlag & windowFlags) == 0 || force);
        }

        public boolean isVisible(int sysUiVis, int color, int windowFlags, boolean force) {
            return isVisible(isPresent(sysUiVis, windowFlags, force), color, windowFlags, force);
        }
    }

    private static class ColorViewState {
        final ColorViewAttributes attributes;
        int color;
        boolean present = false;
        int targetVisibility = 4;
        View view = null;
        boolean visible;

        ColorViewState(ColorViewAttributes attributes) {
            this.attributes = attributes;
        }
    }

    static class WindowManagerHolder {
        static final IWindowManager sWindowManager = Stub.asInterface(ServiceManager.getService("window"));

        WindowManagerHolder() {
        }
    }

    private class ActionModeCallback2Wrapper extends Callback2 {
        private final ActionMode.Callback mWrapped;

        public ActionModeCallback2Wrapper(ActionMode.Callback wrapped) {
            this.mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return this.mWrapped.onCreateActionMode(mode, menu);
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            DecorView.this.requestFitSystemWindows();
            return this.mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return this.mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            boolean isPrimary;
            this.mWrapped.onDestroyActionMode(mode);
            boolean isFloating = false;
            if (DecorView.this.mContext.getApplicationInfo().targetSdkVersion >= 23) {
                isPrimary = mode == DecorView.this.mPrimaryActionMode;
                if (mode == DecorView.this.mFloatingActionMode) {
                    isFloating = true;
                }
                if (!isPrimary && mode.getType() == 0) {
                    String str = DecorView.this.mLogTag;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Destroying unexpected ActionMode instance of TYPE_PRIMARY; ");
                    stringBuilder.append(mode);
                    stringBuilder.append(" was not the current primary action mode! Expected ");
                    stringBuilder.append(DecorView.this.mPrimaryActionMode);
                    Log.e(str, stringBuilder.toString());
                }
                if (!isFloating && mode.getType() == 1) {
                    String str2 = DecorView.this.mLogTag;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Destroying unexpected ActionMode instance of TYPE_FLOATING; ");
                    stringBuilder2.append(mode);
                    stringBuilder2.append(" was not the current floating action mode! Expected ");
                    stringBuilder2.append(DecorView.this.mFloatingActionMode);
                    Log.e(str2, stringBuilder2.toString());
                }
            } else {
                isPrimary = mode.getType() == 0;
                if (mode.getType() == 1) {
                    isFloating = true;
                }
            }
            if (isPrimary) {
                if (DecorView.this.mPrimaryActionModePopup != null) {
                    DecorView.this.removeCallbacks(DecorView.this.mShowPrimaryActionModePopup);
                }
                if (DecorView.this.mPrimaryActionModeView != null) {
                    DecorView.this.endOnGoingFadeAnimation();
                    final ActionBarContextView lastActionModeView = DecorView.this.mPrimaryActionModeView;
                    DecorView.this.mFadeAnim = ObjectAnimator.ofFloat(DecorView.this.mPrimaryActionModeView, View.ALPHA, new float[]{1.0f, 0.0f});
                    DecorView.this.mFadeAnim.addListener(new AnimatorListener() {
                        public void onAnimationStart(Animator animation) {
                        }

                        public void onAnimationEnd(Animator animation) {
                            if (lastActionModeView == DecorView.this.mPrimaryActionModeView) {
                                lastActionModeView.setVisibility(8);
                                if (DecorView.this.mPrimaryActionModePopup != null) {
                                    DecorView.this.mPrimaryActionModePopup.dismiss();
                                }
                                lastActionModeView.killMode();
                                DecorView.this.mFadeAnim = null;
                            }
                        }

                        public void onAnimationCancel(Animator animation) {
                        }

                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
                    DecorView.this.mFadeAnim.start();
                }
                DecorView.this.mPrimaryActionMode = null;
            } else if (isFloating) {
                DecorView.this.cleanupFloatingActionModeViews();
                DecorView.this.mFloatingActionMode = null;
            }
            if (!(DecorView.this.mWindow.getCallback() == null || DecorView.this.mWindow.isDestroyed())) {
                try {
                    DecorView.this.mWindow.getCallback().onActionModeFinished(mode);
                } catch (AbstractMethodError e) {
                }
            }
            DecorView.this.requestFitSystemWindows();
        }

        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if (this.mWrapped instanceof Callback2) {
                ((Callback2) this.mWrapped).onGetContentRect(mode, view, outRect);
            } else {
                super.onGetContentRect(mode, view, outRect);
            }
        }
    }

    DecorView(Context context, int featureId, PhoneWindow window, LayoutParams params) {
        super(context);
        boolean z = false;
        this.mFeatureId = featureId;
        this.mShowInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mHideInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
        this.mBarEnterExitDuration = context.getResources().getInteger(17694941);
        if (context.getResources().getBoolean(17956976) && context.getApplicationInfo().targetSdkVersion >= 24 && !HwWidgetFactory.isHwTheme(context)) {
            z = true;
        }
        this.mForceWindowDrawsStatusBarBackground = z;
        this.mSemiTransparentStatusBarColor = context.getResources().getColor(17170783, null);
        updateAvailableWidth();
        setWindow(window);
        updateLogTag(params);
        this.mResizeShadowSize = context.getResources().getDimensionPixelSize(17105296);
        initResizingPaints();
        this.mPressGestureDetector = HwFrameworkFactory.getPressGestureDetector(context, this, this.mWindow.getContext());
    }

    void setBackgroundFallback(int resId) {
        this.mBackgroundFallback.setDrawable(resId != 0 ? getContext().getDrawable(resId) : null);
        boolean z = getBackground() == null && !this.mBackgroundFallback.hasFallback();
        setWillNotDraw(z);
    }

    public boolean gatherTransparentRegion(Region region) {
        return gatherTransparentRegion(this.mStatusColorViewState, region) || gatherTransparentRegion(this.mNavigationColorViewState, region) || super.gatherTransparentRegion(region);
    }

    boolean gatherTransparentRegion(ColorViewState colorViewState, Region region) {
        if (colorViewState.view != null && colorViewState.visible && isResizing()) {
            return colorViewState.view.gatherTransparentRegion(region);
        }
        return false;
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
        this.mBackgroundFallback.draw(this, this.mContentRoot, c, this.mWindow.mContentParent, this.mStatusColorViewState.view, this.mNavigationColorViewState.view);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mDecorCaptionView != null && (this.mDecorCaptionView instanceof AbsHwDecorCaptionView) && ((AbsHwDecorCaptionView) this.mDecorCaptionView).processKeyEvent(event)) {
            return true;
        }
        boolean onKeyDown;
        int keyCode = event.getKeyCode();
        boolean isDown = event.getAction() == 0;
        if (HwSlog.HW_DEBUG && keyCode == 1000) {
            Log.i(TAG, "View hierachy: ");
            debug();
        }
        if (isDown && event.getRepeatCount() == 0) {
            if (this.mWindow.mPanelChordingKey > 0 && this.mWindow.mPanelChordingKey != keyCode && dispatchKeyShortcutEvent(event)) {
                return true;
            }
            if (this.mWindow.mPreparedPanel != null && this.mWindow.mPreparedPanel.isOpen && this.mWindow.performPanelShortcut(this.mWindow.mPreparedPanel, keyCode, event, 0)) {
                return true;
            }
        }
        if (!this.mWindow.isDestroyed()) {
            boolean handled;
            if (keyCode == MetricsEvent.TUNER_NIGHT_MODE) {
                try {
                    if (!(this.mContext.getPackageManager().checkPermission(PERMISSION_USE_SMARTKEY, this.mContext.getPackageName()) == 0)) {
                        return false;
                    }
                } catch (Exception e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("checkPermission error");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    return false;
                }
            }
            Window.Callback cb = this.mWindow.getCallback();
            if (cb == null || this.mFeatureId >= 0) {
                handled = super.dispatchKeyEvent(event);
            } else {
                handled = cb.dispatchKeyEvent(event);
            }
            if (handled) {
                return true;
            }
        }
        if (isDown) {
            onKeyDown = this.mWindow.onKeyDown(this.mFeatureId, event.getKeyCode(), event);
        } else {
            onKeyDown = this.mWindow.onKeyUp(this.mFeatureId, event.getKeyCode(), event);
        }
        return onKeyDown;
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent ev) {
        if (this.mWindow.mPreparedPanel == null || !this.mWindow.performPanelShortcut(this.mWindow.mPreparedPanel, ev.getKeyCode(), ev, 1)) {
            Window.Callback cb = this.mWindow.getCallback();
            boolean handled = (cb == null || this.mWindow.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchKeyShortcutEvent(ev) : cb.dispatchKeyShortcutEvent(ev);
            if (handled) {
                return true;
            }
            PanelFeatureState st = this.mWindow.getPanelState(0, false);
            if (st != null && this.mWindow.mPreparedPanel == null) {
                this.mWindow.preparePanel(st, ev);
                handled = this.mWindow.performPanelShortcut(st, ev.getKeyCode(), ev, 1);
                st.isPrepared = false;
                if (handled) {
                    return true;
                }
            }
            return false;
        }
        if (this.mWindow.mPreparedPanel != null) {
            this.mWindow.mPreparedPanel.isHandled = true;
        }
        return true;
    }

    private void setHandlingTouchEvent(boolean b) {
        this.mIsHandlingTouchEvent = b;
    }

    protected boolean isHandlingTouchEvent() {
        return this.mIsHandlingTouchEvent;
    }

    public boolean isLongPressSwipe() {
        return this.mPressGestureDetector.isLongPressSwipe();
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        int oldAction = ev.getAction();
        if (isHandlingTouchEvent()) {
            Log.i("PressGestureDetector", "DecorView.dispatchTouchEvent, isHandlingTouchEvent() is TRUE.");
        }
        if (this.mPressGestureDetector.dispatchTouchEvent(ev, isHandlingTouchEvent())) {
            ev.setAction(3);
        }
        setHandlingTouchEvent(true);
        Window.Callback cb = this.mWindow.getCallback();
        boolean ret = (cb == null || this.mWindow.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchTouchEvent(ev) : cb.dispatchTouchEvent(ev);
        setHandlingTouchEvent(false);
        ev.setAction(oldAction);
        return ret;
    }

    public boolean dispatchTrackballEvent(MotionEvent ev) {
        Window.Callback cb = this.mWindow.getCallback();
        return (cb == null || this.mWindow.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchTrackballEvent(ev) : cb.dispatchTrackballEvent(ev);
    }

    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        Window.Callback cb = this.mWindow.getCallback();
        return (cb == null || this.mWindow.isDestroyed() || this.mFeatureId >= 0) ? super.dispatchGenericMotionEvent(ev) : cb.dispatchGenericMotionEvent(ev);
    }

    public boolean superDispatchKeyEvent(KeyEvent event) {
        boolean z = true;
        if (event.getKeyCode() == 4) {
            int action = event.getAction();
            if (this.mPrimaryActionMode != null) {
                if (action == 1) {
                    this.mPrimaryActionMode.finish();
                }
                return true;
            } else if (action == 1) {
                this.mPressGestureDetector.handleBackKey();
            }
        }
        if (super.dispatchKeyEvent(event)) {
            return true;
        }
        if (getViewRootImpl() == null || !getViewRootImpl().dispatchUnhandledKeyEvent(event)) {
            z = false;
        }
        return z;
    }

    public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
        return super.dispatchKeyShortcutEvent(event);
    }

    public boolean superDispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    public boolean superDispatchTrackballEvent(MotionEvent event) {
        return super.dispatchTrackballEvent(event);
    }

    public boolean superDispatchGenericMotionEvent(MotionEvent event) {
        return super.dispatchGenericMotionEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event) {
        return onInterceptTouchEvent(event);
    }

    private boolean isOutOfInnerBounds(int x, int y) {
        return x < 0 || y < 0 || x > getWidth() || y > getHeight();
    }

    private boolean isOutOfBounds(int x, int y) {
        return x < -5 || y < -5 || x > getWidth() + 5 || y > getHeight() + 5;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (this.mHasCaption && isShowingCaption() && action == 0 && isOutOfInnerBounds((int) event.getX(), (int) event.getY())) {
            return true;
        }
        if (this.mFeatureId < 0 || action != 0 || !isOutOfBounds((int) event.getX(), (int) event.getY())) {
            return false;
        }
        this.mWindow.closePanel(this.mFeatureId);
        return true;
    }

    public void sendAccessibilityEvent(int eventType) {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            if ((this.mFeatureId == 0 || this.mFeatureId == 6 || this.mFeatureId == 2 || this.mFeatureId == 5) && getChildCount() == 1) {
                getChildAt(0).sendAccessibilityEvent(eventType);
            } else {
                super.sendAccessibilityEvent(eventType);
            }
        }
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        Window.Callback cb = this.mWindow.getCallback();
        if (cb == null || this.mWindow.isDestroyed() || !cb.dispatchPopulateAccessibilityEvent(event)) {
            return super.dispatchPopulateAccessibilityEventInternal(event);
        }
        return true;
    }

    protected boolean setFrame(int l, int t, int r, int b) {
        boolean changed = super.setFrame(l, t, r, b);
        if (changed) {
            Rect drawingBounds = this.mDrawingBounds;
            getDrawingRect(drawingBounds);
            Drawable fg = getForeground();
            if (fg != null) {
                Rect frameOffsets = this.mFrameOffsets;
                drawingBounds.left += frameOffsets.left;
                drawingBounds.top += frameOffsets.top;
                drawingBounds.right -= frameOffsets.right;
                drawingBounds.bottom -= frameOffsets.bottom;
                fg.setBounds(drawingBounds);
                Rect framePadding = this.mFramePadding;
                drawingBounds.left += framePadding.left - frameOffsets.left;
                drawingBounds.top += framePadding.top - frameOffsets.top;
                drawingBounds.right -= framePadding.right - frameOffsets.right;
                drawingBounds.bottom -= framePadding.bottom - frameOffsets.bottom;
            }
            Drawable bg = getBackground();
            if (bg != null) {
                bg.setBounds(drawingBounds);
            }
        }
        return changed;
    }

    /* JADX WARNING: Removed duplicated region for block: B:52:0x0105  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x0129  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x0172  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0188  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0183  */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x0190  */
    /* JADX WARNING: Removed duplicated region for block: B:92:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x01b7  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0089  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x0105  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x0129  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x0172  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0183  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0188  */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x0190  */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x01b7  */
    /* JADX WARNING: Removed duplicated region for block: B:92:? A:{SYNTHETIC, RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w;
        int widthMeasureSpec2;
        int h;
        int heightMeasureSpec2;
        int heightMeasureSpec3;
        boolean measure;
        TypedValue tv;
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        boolean isPortrait = getResources().getConfiguration().orientation == 1;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        boolean fixedWidth = false;
        this.mApplyFloatingHorizontalInsets = false;
        if (widthMode == Integer.MIN_VALUE) {
            TypedValue tvw = isPortrait ? this.mWindow.mFixedWidthMinor : this.mWindow.mFixedWidthMajor;
            if (!(tvw == null || tvw.type == 0)) {
                if (tvw.type == 5) {
                    w = (int) tvw.getDimension(metrics);
                } else if (tvw.type == 6) {
                    w = (int) tvw.getFraction((float) metrics.widthPixels, (float) metrics.widthPixels);
                } else {
                    w = 0;
                }
                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                if (w > 0) {
                    widthMeasureSpec2 = MeasureSpec.makeMeasureSpec(Math.min(w, widthSize), 1073741824);
                    fixedWidth = true;
                } else {
                    int widthMeasureSpec3 = MeasureSpec.makeMeasureSpec((widthSize - this.mFloatingInsets.left) - this.mFloatingInsets.right, Integer.MIN_VALUE);
                    this.mApplyFloatingHorizontalInsets = true;
                    widthMeasureSpec2 = widthMeasureSpec3;
                }
                this.mApplyFloatingVerticalInsets = false;
                if (heightMode == Integer.MIN_VALUE) {
                    TypedValue tvh;
                    if (isPortrait) {
                        tvh = this.mWindow.mFixedHeightMajor;
                    } else {
                        tvh = this.mWindow.mFixedHeightMinor;
                    }
                    if (!(tvh == null || tvh.type == 0)) {
                        if (tvh.type == 5) {
                            h = (int) tvh.getDimension(metrics);
                        } else if (tvh.type == 6) {
                            h = (int) tvh.getFraction((float) metrics.heightPixels, (float) metrics.heightPixels);
                        } else {
                            h = 0;
                        }
                        w = MeasureSpec.getSize(heightMeasureSpec);
                        if (h > 0) {
                            heightMeasureSpec2 = this.mWindow.getHeightMeasureSpec(h, w, MeasureSpec.makeMeasureSpec(Math.min(h, w), 1073741824));
                        } else if ((this.mWindow.getAttributes().flags & 256) == 0) {
                            heightMeasureSpec3 = MeasureSpec.makeMeasureSpec((w - this.mFloatingInsets.top) - this.mFloatingInsets.bottom, Integer.MIN_VALUE);
                            this.mApplyFloatingVerticalInsets = true;
                            heightMeasureSpec2 = heightMeasureSpec3;
                        }
                        getOutsets(this.mOutsets);
                        if (this.mOutsets.top > 0 || this.mOutsets.bottom > 0) {
                            heightMeasureSpec3 = MeasureSpec.getMode(heightMeasureSpec2);
                            if (heightMeasureSpec3 != 0) {
                                heightMeasureSpec2 = MeasureSpec.makeMeasureSpec((this.mOutsets.top + MeasureSpec.getSize(heightMeasureSpec2)) + this.mOutsets.bottom, heightMeasureSpec3);
                            }
                        }
                        if (this.mOutsets.left > 0 || this.mOutsets.right > 0) {
                            heightMeasureSpec3 = MeasureSpec.getMode(widthMeasureSpec2);
                            if (heightMeasureSpec3 != 0) {
                                widthMeasureSpec2 = MeasureSpec.makeMeasureSpec((this.mOutsets.left + MeasureSpec.getSize(widthMeasureSpec2)) + this.mOutsets.right, heightMeasureSpec3);
                            }
                        }
                        super.onMeasure(widthMeasureSpec2, heightMeasureSpec2);
                        heightMeasureSpec3 = getMeasuredWidth();
                        measure = false;
                        if (!(HwPCUtils.isValidExtDisplayId(this.mWindow.getContext()) || ((HwPCUtils.enabledInPad() && (this.mDecorCaptionView instanceof AbsHwDecorCaptionView)) || this.mWindow.getAttributes().type != AbstractRILConstants.RIL_REQUEST_HW_DATA_CONNECTION_DETACH || isPortrait))) {
                            h = this.mWindow.getScreenWidth();
                            if (h > 0) {
                                heightMeasureSpec3 = Math.min(h, heightMeasureSpec3);
                                measure = true;
                            }
                        }
                        w = MeasureSpec.makeMeasureSpec(heightMeasureSpec3, 1073741824);
                        if (!fixedWidth && widthMode == Integer.MIN_VALUE) {
                            tv = isPortrait ? this.mWindow.mMinWidthMinor : this.mWindow.mMinWidthMajor;
                            if (tv.type != 0) {
                                int min;
                                if (tv.type == 5) {
                                    min = (int) tv.getDimension(metrics);
                                } else if (tv.type == 6) {
                                    updateAvailableWidth();
                                    min = (int) tv.getFraction(this.mAvailableWidth, this.mAvailableWidth);
                                } else {
                                    min = 0;
                                }
                                if (heightMeasureSpec3 < min) {
                                    w = MeasureSpec.makeMeasureSpec(min, 1073741824);
                                    measure = true;
                                }
                            }
                        }
                        if (measure) {
                            super.onMeasure(w, heightMeasureSpec2);
                            return;
                        }
                        return;
                    }
                }
                heightMeasureSpec2 = heightMeasureSpec;
                getOutsets(this.mOutsets);
                heightMeasureSpec3 = MeasureSpec.getMode(heightMeasureSpec2);
                if (heightMeasureSpec3 != 0) {
                }
                heightMeasureSpec3 = MeasureSpec.getMode(widthMeasureSpec2);
                if (heightMeasureSpec3 != 0) {
                }
                super.onMeasure(widthMeasureSpec2, heightMeasureSpec2);
                heightMeasureSpec3 = getMeasuredWidth();
                measure = false;
                h = this.mWindow.getScreenWidth();
                if (h > 0) {
                }
                w = MeasureSpec.makeMeasureSpec(heightMeasureSpec3, 1073741824);
                if (isPortrait) {
                }
                if (tv.type != 0) {
                }
                if (measure) {
                }
            }
        }
        widthMeasureSpec2 = widthMeasureSpec;
        this.mApplyFloatingVerticalInsets = false;
        if (heightMode == Integer.MIN_VALUE) {
        }
        heightMeasureSpec2 = heightMeasureSpec;
        getOutsets(this.mOutsets);
        heightMeasureSpec3 = MeasureSpec.getMode(heightMeasureSpec2);
        if (heightMeasureSpec3 != 0) {
        }
        heightMeasureSpec3 = MeasureSpec.getMode(widthMeasureSpec2);
        if (heightMeasureSpec3 != 0) {
        }
        super.onMeasure(widthMeasureSpec2, heightMeasureSpec2);
        heightMeasureSpec3 = getMeasuredWidth();
        measure = false;
        h = this.mWindow.getScreenWidth();
        if (h > 0) {
        }
        w = MeasureSpec.makeMeasureSpec(heightMeasureSpec3, 1073741824);
        if (isPortrait) {
        }
        if (tv.type != 0) {
        }
        if (measure) {
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        getOutsets(this.mOutsets);
        if (this.mOutsets.left > 0) {
            offsetLeftAndRight(-this.mOutsets.left);
        }
        if (this.mOutsets.top > 0) {
            offsetTopAndBottom(-this.mOutsets.top);
        }
        if (this.mApplyFloatingVerticalInsets) {
            offsetTopAndBottom(this.mFloatingInsets.top);
        }
        if (this.mApplyFloatingHorizontalInsets) {
            offsetLeftAndRight(this.mFloatingInsets.left);
        }
        updateElevation();
        this.mAllowUpdateElevation = true;
        if (changed && this.mResizeMode == 1) {
            getViewRootImpl().requestInvalidateRootRenderNode();
        }
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.mMenuBackground != null) {
            this.mMenuBackground.draw(canvas);
        }
    }

    public boolean showContextMenuForChild(View originalView) {
        return showContextMenuForChildInternal(originalView, Float.NaN, Float.NaN);
    }

    public boolean showContextMenuForChild(View originalView, float x, float y) {
        return showContextMenuForChildInternal(originalView, x, y);
    }

    private boolean showContextMenuForChildInternal(View originalView, float x, float y) {
        MenuHelper helper;
        if (this.mWindow.mContextMenuHelper != null) {
            this.mWindow.mContextMenuHelper.dismiss();
            this.mWindow.mContextMenuHelper = null;
        }
        PhoneWindowMenuCallback callback = this.mWindow.mContextMenuCallback;
        if (this.mWindow.mContextMenu == null) {
            this.mWindow.mContextMenu = new ContextMenuBuilder(getContext());
            this.mWindow.mContextMenu.setCallback(callback);
        } else {
            this.mWindow.mContextMenu.clearAll();
        }
        boolean isPopup = (Float.isNaN(x) || Float.isNaN(y)) ? false : true;
        if (isPopup) {
            helper = this.mWindow.mContextMenu.showPopup(getContext(), originalView, x, y);
        } else {
            helper = this.mWindow.mContextMenu.showDialog(originalView, originalView.getWindowToken());
        }
        if (helper != null) {
            callback.setShowDialogForSubmenu(!isPopup);
            helper.setPresenterCallback(callback);
        }
        this.mWindow.mContextMenuHelper = helper;
        if (helper != null) {
            return true;
        }
        return false;
    }

    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback) {
        return startActionModeForChild(originalView, callback, 0);
    }

    public ActionMode startActionModeForChild(View child, ActionMode.Callback callback, int type) {
        return startActionMode(child, callback, type);
    }

    public ActionMode startActionMode(ActionMode.Callback callback) {
        return startActionMode(callback, 0);
    }

    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        return startActionMode(this, callback, type);
    }

    private ActionMode startActionMode(View originatingView, ActionMode.Callback callback, int type) {
        Callback2 wrappedCallback = new ActionModeCallback2Wrapper(callback);
        ActionMode mode = null;
        if (!(this.mWindow.getCallback() == null || this.mWindow.isDestroyed())) {
            try {
                mode = this.mWindow.getCallback().onWindowStartingActionMode(wrappedCallback, type);
            } catch (AbstractMethodError e) {
                if (type == 0) {
                    try {
                        mode = this.mWindow.getCallback().onWindowStartingActionMode(wrappedCallback);
                    } catch (AbstractMethodError e2) {
                    }
                }
            }
        }
        if (mode == null) {
            mode = createActionMode(type, wrappedCallback, originatingView);
            if (mode == null || !wrappedCallback.onCreateActionMode(mode, mode.getMenu())) {
                mode = null;
            } else {
                setHandledActionMode(mode);
            }
        } else if (mode.getType() == 0) {
            cleanupPrimaryActionMode();
            this.mPrimaryActionMode = mode;
        } else if (mode.getType() == 1) {
            if (this.mFloatingActionMode != null) {
                this.mFloatingActionMode.finish();
            }
            this.mFloatingActionMode = mode;
        }
        if (!(mode == null || this.mWindow.getCallback() == null || this.mWindow.isDestroyed())) {
            try {
                this.mWindow.getCallback().onActionModeStarted(mode);
            } catch (AbstractMethodError e3) {
            }
        }
        return mode;
    }

    private void cleanupPrimaryActionMode() {
        if (this.mPrimaryActionMode != null) {
            this.mPrimaryActionMode.finish();
            this.mPrimaryActionMode = null;
        }
        if (this.mPrimaryActionModeView != null) {
            this.mPrimaryActionModeView.killMode();
        }
    }

    private void cleanupFloatingActionModeViews() {
        if (this.mFloatingToolbar != null) {
            this.mFloatingToolbar.dismiss();
            this.mFloatingToolbar = null;
        }
        if (this.mFloatingActionModeOriginatingView != null) {
            if (this.mFloatingToolbarPreDrawListener != null) {
                this.mFloatingActionModeOriginatingView.getViewTreeObserver().removeOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
                this.mFloatingToolbarPreDrawListener = null;
            }
            this.mFloatingActionModeOriginatingView = null;
        }
    }

    void startChanging() {
        this.mChanging = true;
    }

    void finishChanging() {
        this.mChanging = false;
        drawableChanged();
    }

    public void setWindowBackground(Drawable drawable) {
        if (getBackground() != drawable) {
            setBackgroundDrawable(drawable);
            boolean z = true;
            if (drawable != null) {
                if (!(this.mWindow.isTranslucent() || this.mWindow.isShowingWallpaper())) {
                    z = false;
                }
                this.mResizingBackgroundDrawable = enforceNonTranslucentBackground(drawable, z);
            } else {
                Context context = getContext();
                int i = this.mWindow.mBackgroundFallbackResource;
                if (!(this.mWindow.isTranslucent() || this.mWindow.isShowingWallpaper())) {
                    z = false;
                }
                this.mResizingBackgroundDrawable = getResizingBackgroundDrawable(context, 0, i, z);
            }
            if (this.mResizingBackgroundDrawable != null) {
                this.mResizingBackgroundDrawable.getPadding(this.mBackgroundPadding);
            } else {
                this.mBackgroundPadding.setEmpty();
            }
            drawableChanged();
        }
    }

    public void setWindowFrame(Drawable drawable) {
        if (getForeground() != drawable) {
            setForeground(drawable);
            if (drawable != null) {
                drawable.getPadding(this.mFramePadding);
            } else {
                this.mFramePadding.setEmpty();
            }
            drawableChanged();
        }
    }

    public void onWindowSystemUiVisibilityChanged(int visible) {
        updateColorViews(null, true);
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        LayoutParams attrs = this.mWindow.getAttributes();
        this.mFloatingInsets.setEmpty();
        if ((attrs.flags & 256) == 0) {
            if (attrs.height == -2) {
                this.mFloatingInsets.top = insets.getSystemWindowInsetTop();
                this.mFloatingInsets.bottom = insets.getSystemWindowInsetBottom();
                insets = insets.inset(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            }
            if (this.mWindow.getAttributes().width == -2) {
                this.mFloatingInsets.left = insets.getSystemWindowInsetTop();
                this.mFloatingInsets.right = insets.getSystemWindowInsetBottom();
                insets = insets.inset(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
            }
        }
        this.mFrameOffsets.set(insets.getSystemWindowInsets());
        insets = updateStatusGuard(updateColorViews(insets, true));
        if (getForeground() != null) {
            drawableChanged();
        }
        return insets;
    }

    public boolean isTransitionGroup() {
        return false;
    }

    public static int getColorViewTopInset(int stableTop, int systemTop) {
        return Math.min(stableTop, systemTop);
    }

    public static int getColorViewBottomInset(int stableBottom, int systemBottom) {
        return Math.min(stableBottom, systemBottom);
    }

    public static int getColorViewRightInset(int stableRight, int systemRight) {
        return Math.min(stableRight, systemRight);
    }

    public static int getColorViewLeftInset(int stableLeft, int systemLeft) {
        return Math.min(stableLeft, systemLeft);
    }

    public static boolean isNavBarToRightEdge(int bottomInset, int rightInset) {
        return bottomInset == 0 && rightInset > 0;
    }

    public static boolean isNavBarToLeftEdge(int bottomInset, int leftInset) {
        return bottomInset == 0 && leftInset > 0;
    }

    public static int getNavBarSize(int bottomInset, int rightInset, int leftInset) {
        if (isNavBarToRightEdge(bottomInset, rightInset)) {
            return rightInset;
        }
        return isNavBarToLeftEdge(bottomInset, leftInset) ? leftInset : bottomInset;
    }

    public static void getNavigationBarRect(int canvasWidth, int canvasHeight, Rect stableInsets, Rect contentInsets, Rect outRect) {
        int bottomInset = getColorViewBottomInset(stableInsets.bottom, contentInsets.bottom);
        int leftInset = getColorViewLeftInset(stableInsets.left, contentInsets.left);
        int rightInset = getColorViewLeftInset(stableInsets.right, contentInsets.right);
        int size = getNavBarSize(bottomInset, rightInset, leftInset);
        if (isNavBarToRightEdge(bottomInset, rightInset)) {
            outRect.set(canvasWidth - size, 0, canvasWidth, canvasHeight);
        } else if (isNavBarToLeftEdge(bottomInset, leftInset)) {
            outRect.set(0, 0, size, canvasHeight);
        } else {
            outRect.set(0, canvasHeight - size, canvasWidth, canvasHeight);
        }
    }

    WindowInsets updateColorViews(WindowInsets insets, boolean animate) {
        boolean disallowAnimate;
        int i;
        int i2;
        int i3;
        WindowInsets insets2 = insets;
        LayoutParams attrs = this.mWindow.getAttributes();
        int sysUiVisibility = attrs.systemUiVisibility | getWindowSystemUiVisibility();
        boolean z = true;
        int i4 = 0;
        boolean isImeWindow = this.mWindow.getAttributes().type == AbstractRILConstants.RIL_REQUEST_HW_DATA_CONNECTION_DETACH;
        if (!this.mWindow.mIsFloating || isImeWindow) {
            int i5;
            int statusBarSideInset;
            disallowAnimate = (isLaidOut() ^ true) | (((this.mLastWindowFlags ^ attrs.flags) & Integer.MIN_VALUE) != 0 ? 1 : 0);
            this.mLastWindowFlags = attrs.flags;
            if (insets2 != null) {
                this.mLastTopInset = getColorViewTopInset(insets.getStableInsetTop(), insets.getSystemWindowInsetTop());
                this.mLastBottomInset = getColorViewBottomInset(insets.getStableInsetBottom(), insets.getSystemWindowInsetBottom());
                this.mLastRightInset = getColorViewRightInset(insets.getStableInsetRight(), insets.getSystemWindowInsetRight());
                this.mLastLeftInset = getColorViewRightInset(insets.getStableInsetLeft(), insets.getSystemWindowInsetLeft());
                boolean hasTopStableInset = insets.getStableInsetTop() != 0;
                disallowAnimate |= hasTopStableInset != this.mLastHasTopStableInset ? 1 : 0;
                this.mLastHasTopStableInset = hasTopStableInset;
                boolean hasBottomStableInset = insets.getStableInsetBottom() != 0;
                disallowAnimate |= hasBottomStableInset != this.mLastHasBottomStableInset ? 1 : 0;
                this.mLastHasBottomStableInset = hasBottomStableInset;
                boolean hasRightStableInset = insets.getStableInsetRight() != 0;
                disallowAnimate |= hasRightStableInset != this.mLastHasRightStableInset ? 1 : 0;
                this.mLastHasRightStableInset = hasRightStableInset;
                boolean hasLeftStableInset = insets.getStableInsetLeft() != 0;
                disallowAnimate |= hasLeftStableInset != this.mLastHasLeftStableInset ? 1 : 0;
                this.mLastHasLeftStableInset = hasLeftStableInset;
                this.mLastShouldAlwaysConsumeNavBar = insets.shouldAlwaysConsumeNavBar();
            }
            boolean disallowAnimate2 = disallowAnimate;
            boolean navBarToRightEdge = isNavBarToRightEdge(this.mLastBottomInset, this.mLastRightInset);
            boolean navBarToLeftEdge = isNavBarToLeftEdge(this.mLastBottomInset, this.mLastLeftInset);
            int navBarSize = getNavBarSize(this.mLastBottomInset, this.mLastRightInset, this.mLastLeftInset);
            ColorViewState colorViewState = this.mNavigationColorViewState;
            i = this.mWindow.mNavigationBarColor;
            i2 = this.mWindow.mNavigationBarDividerColor;
            boolean z2 = navBarToRightEdge || navBarToLeftEdge;
            boolean z3 = animate && !disallowAnimate2;
            updateColorViewInt(colorViewState, sysUiVisibility, i, i2, navBarSize, z2, navBarToLeftEdge, 0, z3, false);
            disallowAnimate = navBarToRightEdge && this.mNavigationColorViewState.present;
            boolean statusBarNeedsRightInset = disallowAnimate;
            disallowAnimate = navBarToLeftEdge && this.mNavigationColorViewState.present;
            boolean statusBarNeedsLeftInset = disallowAnimate;
            if (statusBarNeedsRightInset) {
                i5 = this.mLastRightInset;
            } else if (statusBarNeedsLeftInset) {
                i5 = this.mLastLeftInset;
            } else {
                statusBarSideInset = 0;
                colorViewState = this.mStatusColorViewState;
                i = calculateStatusBarColor();
                i3 = this.mLastTopInset;
                z3 = animate && !disallowAnimate2;
                updateColorViewInt(colorViewState, sysUiVisibility, i, 0, i3, false, statusBarNeedsLeftInset, statusBarSideInset, z3, this.mForceWindowDrawsStatusBarBackground);
            }
            statusBarSideInset = i5;
            colorViewState = this.mStatusColorViewState;
            i = calculateStatusBarColor();
            i3 = this.mLastTopInset;
            if (!animate) {
            }
            updateColorViewInt(colorViewState, sysUiVisibility, i, 0, i3, false, statusBarNeedsLeftInset, statusBarSideInset, z3, this.mForceWindowDrawsStatusBarBackground);
        }
        disallowAnimate = ((attrs.flags & Integer.MIN_VALUE) != 0 && (sysUiVisibility & 512) == 0 && (sysUiVisibility & 2) == 0) || this.mLastShouldAlwaysConsumeNavBar;
        if (!((sysUiVisibility & 1024) == 0 && (sysUiVisibility & Integer.MIN_VALUE) == 0 && (attrs.flags & 256) == 0 && (attrs.flags & Protocol.BASE_SYSTEM_RESERVED) == 0 && this.mForceWindowDrawsStatusBarBackground && this.mLastTopInset != 0)) {
            z = false;
        }
        int consumedTop = z ? this.mLastTopInset : 0;
        i = disallowAnimate ? this.mLastRightInset : 0;
        i2 = disallowAnimate ? this.mLastBottomInset : 0;
        if (disallowAnimate) {
            i4 = this.mLastLeftInset;
        }
        i3 = i4;
        if (this.mContentRoot != null && (this.mContentRoot.getLayoutParams() instanceof MarginLayoutParams)) {
            MarginLayoutParams lp = (MarginLayoutParams) this.mContentRoot.getLayoutParams();
            if (!(lp.topMargin == consumedTop && lp.rightMargin == i && lp.bottomMargin == i2 && lp.leftMargin == i3)) {
                lp.topMargin = consumedTop;
                lp.rightMargin = i;
                lp.bottomMargin = i2;
                lp.leftMargin = i3;
                this.mContentRoot.setLayoutParams(lp);
                if (insets2 == null) {
                    requestApplyInsets();
                }
            }
            if (insets2 != null) {
                insets2 = insets2.inset(i3, consumedTop, i, i2);
            }
        }
        if (insets2 != null) {
            return insets2.consumeStableInsets();
        }
        return insets2;
    }

    private int calculateStatusBarColor() {
        return calculateStatusBarColor(this.mWindow.getAttributes().flags, this.mSemiTransparentStatusBarColor, this.mWindow.getStatusBarColor());
    }

    public static int calculateStatusBarColor(int flags, int semiTransparentStatusBarColor, int statusBarColor) {
        if ((67108864 & flags) != 0) {
            return semiTransparentStatusBarColor;
        }
        if ((Integer.MIN_VALUE & flags) != 0) {
            return statusBarColor;
        }
        return Tonal.MAIN_COLOR_DARK;
    }

    private int getCurrentColor(ColorViewState state) {
        if (state.visible) {
            return state.color;
        }
        return 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:62:0x014f  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x014c  */
    /* JADX WARNING: Missing block: B:90:0x01b2, code skipped:
            if (r6.leftMargin != r10) goto L_0x01c0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateColorViewInt(ColorViewState state, int sysUiVis, int color, int dividerColor, int size, boolean verticalBar, boolean seascape, int sideMargin, boolean animate, boolean force) {
        int resolvedGravity;
        boolean visibilityChanged;
        boolean visibilityChanged2;
        int rightMargin;
        boolean show;
        final ColorViewState colorViewState = state;
        int i = color;
        int i2 = dividerColor;
        boolean z = verticalBar;
        boolean z2 = seascape;
        int i3 = sideMargin;
        boolean z3 = animate;
        boolean z4 = force;
        boolean drawBackSet = (this.mWindow.getAttributes().flags & Integer.MIN_VALUE) != 0 || z4;
        boolean isEmui = HwWidgetFactory.isHwTheme(getContext());
        colorViewState.present = colorViewState.attributes.isPresent(sysUiVis, this.mWindow.getAttributes().flags, z4);
        boolean show2 = colorViewState.attributes.isVisible(colorViewState.present, i, this.mWindow.getAttributes().flags, z4);
        boolean showView = show2 && !isResizing() && size > 0;
        z4 = this.mWindow.getAttributes().type == AbstractRILConstants.RIL_REQUEST_HW_DATA_CONNECTION_DETACH;
        boolean showView2 = !this.mWindow.getHwFloating() && (!isEmui || !this.mWindow.windowIsTranslucent() || this.mWindow.isTranslucentImmersion() || z4);
        showView2 = (showView2 & showView) | this.mWindow.isSplitMode();
        View view = colorViewState.view;
        boolean resolvedHeight = z ? true : size;
        int resolvedDynStatusBarWidth = -1;
        drawBackSet = getContext().getResources().getConfiguration().orientation;
        if (this.mForcedDrawSysBarBackground && !z && drawBackSet) {
            resolvedDynStatusBarWidth = this.mWindow.getFullScreenWidth();
        }
        int resolvedWidth = z ? size : resolvedDynStatusBarWidth;
        if (!z) {
            resolvedGravity = colorViewState.attributes.verticalGravity;
        } else if (z2) {
            resolvedGravity = colorViewState.attributes.seascapeGravity;
        } else {
            resolvedGravity = colorViewState.attributes.horizontalGravity;
        }
        if (view != null) {
            int leftMargin;
            visibilityChanged = false;
            showView = resolvedHeight;
            int vis = showView2 ? 0 : 4;
            visibilityChanged2 = colorViewState.targetVisibility != vis;
            colorViewState.targetVisibility = vis;
            boolean visibilityChanged3 = visibilityChanged2;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
            int rightMargin2 = z2 ? 0 : sideMargin;
            int leftMargin2 = z2 ? sideMargin : 0;
            if (lp.height == showView && lp.width == resolvedWidth && lp.gravity == resolvedGravity) {
                rightMargin = rightMargin2;
                if (lp.rightMargin == rightMargin) {
                    show = show2;
                    leftMargin = leftMargin2;
                } else {
                    show = show2;
                    leftMargin = leftMargin2;
                }
            } else {
                show = show2;
                rightMargin = rightMargin2;
                leftMargin = leftMargin2;
            }
            lp.height = showView;
            lp.width = resolvedWidth;
            lp.gravity = resolvedGravity;
            lp.rightMargin = rightMargin;
            lp.leftMargin = leftMargin;
            view.setLayoutParams(lp);
            if (showView2) {
                setColor(view, i, i2, z, z2);
            }
            visibilityChanged = visibilityChanged3;
        } else if (showView2) {
            View hwNavigationBarColorView;
            boolean visibilityChanged4;
            FrameLayout.LayoutParams lp2;
            boolean isHwLightTheme = HwWidgetFactory.isHwLightTheme(getContext());
            if (isHwLightTheme) {
                if (colorViewState.attributes.transitionName != null) {
                    visibilityChanged = false;
                    if (colorViewState.attributes.transitionName.equals("android:navigation:background")) {
                        hwNavigationBarColorView = HwPolicyFactory.getHwNavigationBarColorView(this.mContext);
                        view = hwNavigationBarColorView;
                        colorViewState.view = hwNavigationBarColorView;
                        setColor(view, i, i2, z, z2);
                        view.setTransitionName(colorViewState.attributes.transitionName);
                        view.setId(colorViewState.attributes.id);
                        view.setVisibility(4);
                        colorViewState.targetVisibility = 0;
                        visibilityChanged4 = true;
                        lp2 = new FrameLayout.LayoutParams(resolvedWidth, resolvedHeight, resolvedGravity);
                        if (z2) {
                            lp2.rightMargin = i3;
                        } else {
                            lp2.leftMargin = i3;
                        }
                        addView(view, lp2);
                        updateColorViewTranslations();
                        show = show2;
                        visibilityChanged = visibilityChanged4;
                    }
                } else {
                    visibilityChanged = false;
                }
            } else {
                visibilityChanged = false;
            }
            hwNavigationBarColorView = new View(this.mContext);
            view = hwNavigationBarColorView;
            colorViewState.view = hwNavigationBarColorView;
            setColor(view, i, i2, z, z2);
            view.setTransitionName(colorViewState.attributes.transitionName);
            view.setId(colorViewState.attributes.id);
            view.setVisibility(4);
            colorViewState.targetVisibility = 0;
            visibilityChanged4 = true;
            lp2 = new FrameLayout.LayoutParams(resolvedWidth, resolvedHeight, resolvedGravity);
            if (z2) {
            }
            addView(view, lp2);
            updateColorViewTranslations();
            show = show2;
            visibilityChanged = visibilityChanged4;
        } else {
            visibilityChanged = false;
            showView = resolvedHeight;
            show = show2;
        }
        if (visibilityChanged) {
            view.animate().cancel();
            visibilityChanged2 = this.mWindow.getTryForcedCloseAnimation(WindowManagerHolder.sWindowManager, z3, getTag());
            if (!z3 || visibilityChanged2 || this.mWindow.getAttributes().isEmuiStyle == -1 || isResizing()) {
                rightMargin = 0;
                view.setAlpha(1.0f);
                if (!showView2) {
                    rightMargin = 4;
                }
                view.setVisibility(rightMargin);
            } else if (showView2) {
                if (view.getVisibility() != 0) {
                    view.setVisibility(0);
                    view.setAlpha(0.0f);
                }
                view.animate().alpha(1.0f).setInterpolator(this.mShowInterpolator).setDuration((long) this.mBarEnterExitDuration);
            } else {
                view.animate().alpha(0.0f).setInterpolator(this.mHideInterpolator).setDuration(0).withEndAction(new Runnable() {
                    public void run() {
                        colorViewState.view.setAlpha(1.0f);
                        colorViewState.view.setVisibility(4);
                    }
                });
            }
        }
        colorViewState.visible = show;
        colorViewState.color = i;
    }

    private static void setColor(View v, int color, int dividerColor, boolean verticalBar, boolean seascape) {
        if (dividerColor != 0) {
            Pair<Boolean, Boolean> dir = (Pair) v.getTag();
            if (dir != null && ((Boolean) dir.first).booleanValue() == verticalBar && ((Boolean) dir.second).booleanValue() == seascape) {
                LayerDrawable d = (LayerDrawable) v.getBackground();
                ((ColorDrawable) ((InsetDrawable) d.getDrawable(1)).getDrawable()).setColor(color);
                ((ColorDrawable) d.getDrawable(0)).setColor(dividerColor);
                return;
            }
            int size = Math.round(TypedValue.applyDimension(1, 1.0f, v.getContext().getResources().getDisplayMetrics()));
            ColorDrawable colorDrawable = new ColorDrawable(color);
            int i = (!verticalBar || seascape) ? 0 : size;
            int i2 = !verticalBar ? size : 0;
            int i3 = (verticalBar && seascape) ? size : 0;
            InsetDrawable d2 = new InsetDrawable(colorDrawable, i, i2, i3, 0);
            v.setBackground(new LayerDrawable(new Drawable[]{new ColorDrawable(dividerColor), d2}));
            v.setTag(new Pair(Boolean.valueOf(verticalBar), Boolean.valueOf(seascape)));
            return;
        }
        v.setTag(null);
        v.setBackgroundColor(color);
    }

    private void updateColorViewTranslations() {
        int rootScrollY = this.mRootScrollY;
        float f = 0.0f;
        if (this.mStatusColorViewState.view != null) {
            this.mStatusColorViewState.view.setTranslationY(rootScrollY > 0 ? (float) rootScrollY : 0.0f);
        }
        if (this.mNavigationColorViewState.view != null) {
            View view = this.mNavigationColorViewState.view;
            if (rootScrollY < 0) {
                f = (float) rootScrollY;
            }
            view.setTranslationY(f);
        }
    }

    private WindowInsets updateStatusGuard(WindowInsets insets) {
        boolean showStatusGuard = false;
        int i = 0;
        if (this.mPrimaryActionModeView != null && (this.mPrimaryActionModeView.getLayoutParams() instanceof MarginLayoutParams)) {
            MarginLayoutParams mlp = (MarginLayoutParams) this.mPrimaryActionModeView.getLayoutParams();
            boolean mlpChanged = false;
            if (this.mPrimaryActionModeView.isShown()) {
                if (this.mTempRect == null) {
                    this.mTempRect = new Rect();
                }
                Rect rect = this.mTempRect;
                this.mWindow.mContentParent.computeSystemWindowInsets(insets, rect);
                if (mlp.topMargin != (rect.top == 0 ? insets.getSystemWindowInsetTop() : 0)) {
                    mlpChanged = true;
                    mlp.topMargin = insets.getSystemWindowInsetTop();
                    if (this.mStatusGuard == null) {
                        this.mStatusGuard = new View(this.mContext);
                        this.mStatusGuard.setBackgroundColor(this.mContext.getColor(17170556));
                        addView(this.mStatusGuard, indexOfChild(this.mStatusColorViewState.view), new FrameLayout.LayoutParams(-1, mlp.topMargin, 8388659));
                    } else {
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mStatusGuard.getLayoutParams();
                        if (lp.height != mlp.topMargin) {
                            lp.height = mlp.topMargin;
                            this.mStatusGuard.setLayoutParams(lp);
                        }
                    }
                }
                boolean z = true;
                showStatusGuard = this.mStatusGuard != null;
                if ((this.mWindow.getLocalFeaturesPrivate() & 1024) != 0) {
                    z = false;
                }
                if (z && showStatusGuard) {
                    insets = insets.inset(0, insets.getSystemWindowInsetTop(), 0, 0);
                }
            } else if (mlp.topMargin != 0) {
                mlpChanged = true;
                mlp.topMargin = 0;
            }
            if (mlpChanged) {
                this.mPrimaryActionModeView.setLayoutParams(mlp);
            }
        }
        if (this.mStatusGuard != null) {
            View view = this.mStatusGuard;
            if (!showStatusGuard) {
                i = 8;
            }
            view.setVisibility(i);
        }
        return insets;
    }

    public void updatePictureInPictureOutlineProvider(boolean isInPictureInPictureMode) {
        if (this.mIsInPictureInPictureMode != isInPictureInPictureMode) {
            if (isInPictureInPictureMode) {
                WindowControllerCallback callback = this.mWindow.getWindowControllerCallback();
                if (callback != null && callback.isTaskRoot()) {
                    super.setOutlineProvider(PIP_OUTLINE_PROVIDER);
                }
            } else if (getOutlineProvider() != this.mLastOutlineProvider) {
                setOutlineProvider(this.mLastOutlineProvider);
            }
            this.mIsInPictureInPictureMode = isInPictureInPictureMode;
        }
    }

    public void setOutlineProvider(ViewOutlineProvider provider) {
        super.setOutlineProvider(provider);
        this.mLastOutlineProvider = provider;
    }

    private void drawableChanged() {
        if (!this.mChanging) {
            setPadding(this.mFramePadding.left + this.mBackgroundPadding.left, this.mFramePadding.top + this.mBackgroundPadding.top, this.mFramePadding.right + this.mBackgroundPadding.right, this.mFramePadding.bottom + this.mBackgroundPadding.bottom);
            requestLayout();
            invalidate();
            int opacity = -1;
            if (getResources().getConfiguration().windowConfiguration.hasWindowShadow()) {
                opacity = -3;
            } else {
                Drawable bg = getBackground();
                Drawable fg = getForeground();
                if (bg != null) {
                    if (fg == null) {
                        opacity = bg.getOpacity();
                    } else if (this.mFramePadding.left > 0 || this.mFramePadding.top > 0 || this.mFramePadding.right > 0 || this.mFramePadding.bottom > 0) {
                        opacity = -3;
                    } else {
                        int fop = fg.getOpacity();
                        int bop = bg.getOpacity();
                        if (fop == -1 || bop == -1) {
                            opacity = -1;
                        } else if (fop == 0) {
                            opacity = bop;
                        } else if (bop == 0) {
                            opacity = fop;
                        } else {
                            opacity = Drawable.resolveOpacity(fop, bop);
                        }
                    }
                }
            }
            this.mDefaultOpacity = opacity;
            if (this.mFeatureId < 0) {
                this.mWindow.setDefaultWindowFormat(opacity);
            }
        }
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!(!this.mWindow.hasFeature(0) || hasWindowFocus || this.mWindow.mPanelChordingKey == 0)) {
            this.mWindow.closePanel(0);
        }
        Window.Callback cb = this.mWindow.getCallback();
        if (!(cb == null || this.mWindow.isDestroyed() || this.mFeatureId >= 0)) {
            cb.onWindowFocusChanged(hasWindowFocus);
        }
        if (this.mPrimaryActionMode != null) {
            this.mPrimaryActionMode.onWindowFocusChanged(hasWindowFocus);
        }
        if (this.mFloatingActionMode != null) {
            this.mFloatingActionMode.onWindowFocusChanged(hasWindowFocus);
        }
        updateElevation();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window.Callback cb = this.mWindow.getCallback();
        if (!(cb == null || this.mWindow.isDestroyed() || this.mFeatureId >= 0)) {
            cb.onAttachedToWindow();
        }
        if (this.mFeatureId == -1) {
            this.mWindow.openPanelsAfterRestore();
        }
        if (!this.mWindowResizeCallbacksAdded) {
            getViewRootImpl().addWindowCallbacks(this);
            this.mWindowResizeCallbacksAdded = true;
        } else if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.onConfigurationChange();
        }
        this.mWindow.onViewRootImplSet(getViewRootImpl());
        if (this.mPressGestureDetector != null) {
            this.mPressGestureDetector.onAttached(this.mWindow.getAttributes().type);
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Window.Callback cb = this.mWindow.getCallback();
        if (cb != null && this.mFeatureId < 0) {
            cb.onDetachedFromWindow();
        }
        if (this.mWindow.mDecorContentParent != null) {
            this.mWindow.mDecorContentParent.dismissPopups();
        }
        if (this.mPrimaryActionModePopup != null) {
            removeCallbacks(this.mShowPrimaryActionModePopup);
            if (this.mPrimaryActionModePopup.isShowing()) {
                this.mPrimaryActionModePopup.dismiss();
            }
            this.mPrimaryActionModePopup = null;
        }
        if (this.mFloatingToolbar != null) {
            this.mFloatingToolbar.dismiss();
            this.mFloatingToolbar = null;
        }
        this.mPressGestureDetector.onDetached();
        PanelFeatureState st = this.mWindow.getPanelState(0, false);
        if (!(st == null || st.menu == null || this.mFeatureId >= 0)) {
            st.menu.close();
        }
        releaseThreadedRenderer();
        if (this.mWindowResizeCallbacksAdded) {
            getViewRootImpl().removeWindowCallbacks(this);
            this.mWindowResizeCallbacksAdded = false;
        }
    }

    public void onCloseSystemDialogs(String reason) {
        if (this.mFeatureId >= 0) {
            this.mWindow.closeAllPanels();
        }
    }

    public SurfaceHolder.Callback2 willYouTakeTheSurface() {
        return this.mFeatureId < 0 ? this.mWindow.mTakeSurfaceCallback : null;
    }

    public InputQueue.Callback willYouTakeTheInputQueue() {
        return this.mFeatureId < 0 ? this.mWindow.mTakeInputQueueCallback : null;
    }

    public void setSurfaceType(int type) {
        this.mWindow.setType(type);
    }

    public void setSurfaceFormat(int format) {
        this.mWindow.setFormat(format);
    }

    public void setSurfaceKeepScreenOn(boolean keepOn) {
        if (keepOn) {
            this.mWindow.addFlags(128);
        } else {
            this.mWindow.clearFlags(128);
        }
    }

    public void onRootViewScrollYChanged(int rootScrollY) {
        this.mRootScrollY = rootScrollY;
        updateColorViewTranslations();
    }

    private ActionMode createActionMode(int type, Callback2 callback, View originatingView) {
        if (type != 1) {
            return createStandaloneActionMode(callback);
        }
        return createFloatingActionMode(originatingView, callback);
    }

    private void setHandledActionMode(ActionMode mode) {
        if (mode.getType() == 0) {
            setHandledPrimaryActionMode(mode);
        } else if (mode.getType() == 1) {
            setHandledFloatingActionMode(mode);
        }
    }

    private ActionMode createStandaloneActionMode(ActionMode.Callback callback) {
        endOnGoingFadeAnimation();
        cleanupPrimaryActionMode();
        boolean z = false;
        if (this.mPrimaryActionModeView == null || !this.mPrimaryActionModeView.isAttachedToWindow()) {
            if (this.mWindow.isFloating()) {
                Context actionBarContext;
                TypedValue outValue = new TypedValue();
                Theme baseTheme = this.mContext.getTheme();
                baseTheme.resolveAttribute(16843825, outValue, true);
                if (outValue.resourceId != 0) {
                    Theme actionBarTheme = this.mContext.getResources().newTheme();
                    actionBarTheme.setTo(baseTheme);
                    actionBarTheme.applyStyle(outValue.resourceId, true);
                    actionBarContext = new ContextThemeWrapper(this.mContext, 0);
                    actionBarContext.getTheme().setTo(actionBarTheme);
                } else {
                    actionBarContext = this.mContext;
                }
                Context actionBarContext2 = actionBarContext;
                this.mPrimaryActionModeView = new ActionBarContextView(actionBarContext2);
                this.mPrimaryActionModePopup = new PopupWindow(actionBarContext2, null, 17891333);
                this.mPrimaryActionModePopup.setWindowLayoutType(2);
                this.mPrimaryActionModePopup.setContentView(this.mPrimaryActionModeView);
                this.mPrimaryActionModePopup.setWidth(-1);
                actionBarContext2.getTheme().resolveAttribute(16843499, outValue, true);
                this.mPrimaryActionModeView.setContentHeight(TypedValue.complexToDimensionPixelSize(outValue.data, actionBarContext2.getResources().getDisplayMetrics()));
                this.mPrimaryActionModePopup.setHeight(-2);
                this.mShowPrimaryActionModePopup = new Runnable() {
                    public void run() {
                        DecorView.this.mPrimaryActionModePopup.showAtLocation(DecorView.this.mPrimaryActionModeView.getApplicationWindowToken(), 55, 0, 0);
                        DecorView.this.endOnGoingFadeAnimation();
                        if (DecorView.this.shouldAnimatePrimaryActionModeView()) {
                            DecorView.this.mFadeAnim = ObjectAnimator.ofFloat(DecorView.this.mPrimaryActionModeView, View.ALPHA, new float[]{0.0f, 1.0f});
                            DecorView.this.mFadeAnim.addListener(new AnimatorListenerAdapter() {
                                public void onAnimationStart(Animator animation) {
                                    DecorView.this.mPrimaryActionModeView.setVisibility(0);
                                }

                                public void onAnimationEnd(Animator animation) {
                                    DecorView.this.mPrimaryActionModeView.setAlpha(1.0f);
                                    DecorView.this.mFadeAnim = null;
                                }
                            });
                            DecorView.this.mFadeAnim.start();
                            return;
                        }
                        DecorView.this.mPrimaryActionModeView.setAlpha(1.0f);
                        DecorView.this.mPrimaryActionModeView.setVisibility(0);
                    }
                };
            } else {
                ViewStub stub = (ViewStub) findViewById(16908697);
                this.mWindow.setEmuiActionModeBar(stub);
                if (stub != null) {
                    this.mPrimaryActionModeView = (ActionBarContextView) stub.inflate();
                    this.mPrimaryActionModePopup = null;
                }
            }
        }
        if (this.mPrimaryActionModeView == null) {
            return null;
        }
        this.mPrimaryActionModeView.killMode();
        Context context = this.mPrimaryActionModeView.getContext();
        ActionBarContextView actionBarContextView = this.mPrimaryActionModeView;
        if (this.mPrimaryActionModePopup == null) {
            z = true;
        }
        return new StandaloneActionMode(context, actionBarContextView, callback, z);
    }

    private void endOnGoingFadeAnimation() {
        if (this.mFadeAnim != null) {
            this.mFadeAnim.end();
        }
    }

    private void setHandledPrimaryActionMode(ActionMode mode) {
        endOnGoingFadeAnimation();
        this.mPrimaryActionMode = mode;
        this.mPrimaryActionMode.invalidate();
        this.mPrimaryActionModeView.initForMode(this.mPrimaryActionMode);
        if (this.mPrimaryActionModePopup != null) {
            post(this.mShowPrimaryActionModePopup);
        } else if (shouldAnimatePrimaryActionModeView()) {
            this.mFadeAnim = ObjectAnimator.ofFloat(this.mPrimaryActionModeView, View.ALPHA, new float[]{0.0f, 1.0f});
            this.mFadeAnim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    DecorView.this.mPrimaryActionModeView.setVisibility(0);
                }

                public void onAnimationEnd(Animator animation) {
                    DecorView.this.mPrimaryActionModeView.setAlpha(1.0f);
                    DecorView.this.mFadeAnim = null;
                }
            });
            this.mFadeAnim.start();
        } else {
            this.mPrimaryActionModeView.setAlpha(1.0f);
            this.mPrimaryActionModeView.setVisibility(0);
        }
        this.mPrimaryActionModeView.sendAccessibilityEvent(32);
    }

    boolean shouldAnimatePrimaryActionModeView() {
        return isLaidOut();
    }

    private ActionMode createFloatingActionMode(View originatingView, Callback2 callback) {
        if (this.mFloatingActionMode != null) {
            this.mFloatingActionMode.finish();
        }
        cleanupFloatingActionModeViews();
        this.mFloatingToolbar = this.mWindow.getFloatingToolbar(this.mWindow);
        final FloatingActionMode mode = new FloatingActionMode(this.mContext, callback, originatingView, this.mFloatingToolbar);
        this.mFloatingActionModeOriginatingView = originatingView;
        this.mFloatingToolbarPreDrawListener = new OnPreDrawListener() {
            public boolean onPreDraw() {
                mode.updateViewLocationInWindow();
                return true;
            }
        };
        return mode;
    }

    private void setHandledFloatingActionMode(ActionMode mode) {
        this.mFloatingActionMode = mode;
        this.mFloatingActionMode.invalidate();
        this.mFloatingActionModeOriginatingView.getViewTreeObserver().addOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
    }

    void enableCaption(boolean attachedAndVisible) {
        if (this.mHasCaption != attachedAndVisible) {
            this.mHasCaption = attachedAndVisible;
            if (getForeground() != null) {
                drawableChanged();
            }
        }
    }

    void setWindow(PhoneWindow phoneWindow) {
        this.mWindow = phoneWindow;
        Context context = getContext();
        if (context instanceof DecorContext) {
            ((DecorContext) context).setPhoneWindow(this.mWindow);
        }
    }

    public Resources getResources() {
        return getContext().getResources();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean displayWindowDecor = newConfig.windowConfiguration.hasWindowDecorCaption();
        if (this.mDecorCaptionView == null && displayWindowDecor) {
            this.mDecorCaptionView = createDecorCaptionView(this.mWindow.getLayoutInflater());
            if (this.mDecorCaptionView != null) {
                if (this.mDecorCaptionView.getParent() == null) {
                    addView(this.mDecorCaptionView, 0, new ViewGroup.LayoutParams(-1, -1));
                }
                removeView(this.mContentRoot);
                this.mDecorCaptionView.addView(this.mContentRoot, new MarginLayoutParams(-1, -1));
            }
        } else if (this.mDecorCaptionView != null) {
            this.mDecorCaptionView.onConfigurationChanged(displayWindowDecor);
            enableCaption(displayWindowDecor);
        }
        updateAvailableWidth();
        initializeElevation();
        this.mPressGestureDetector.handleConfigurationChanged(newConfig);
    }

    void onResourcesLoaded(LayoutInflater inflater, int layoutResource) {
        if (this.mBackdropFrameRenderer != null) {
            loadBackgroundDrawablesIfNeeded();
            this.mBackdropFrameRenderer.onResourcesLoaded(this, this.mResizingBackgroundDrawable, this.mCaptionBackgroundDrawable, this.mUserCaptionBackgroundDrawable, getCurrentColor(this.mStatusColorViewState), getCurrentColor(this.mNavigationColorViewState));
        }
        this.mDecorCaptionView = createDecorCaptionView(inflater);
        View root = inflater.inflate(layoutResource, null);
        if (this.mDecorCaptionView != null) {
            if (this.mDecorCaptionView.getParent() == null) {
                addView(this.mDecorCaptionView, new ViewGroup.LayoutParams(-1, -1));
            }
            this.mDecorCaptionView.addView(root, new MarginLayoutParams(-1, -1));
        } else {
            addView(root, 0, new ViewGroup.LayoutParams(-1, -1));
        }
        this.mContentRoot = (ViewGroup) root;
        initializeElevation();
    }

    private void loadBackgroundDrawablesIfNeeded() {
        if (this.mResizingBackgroundDrawable == null) {
            Context context = getContext();
            int i = this.mWindow.mBackgroundResource;
            int i2 = this.mWindow.mBackgroundFallbackResource;
            boolean z = this.mWindow.isTranslucent() || this.mWindow.isShowingWallpaper();
            this.mResizingBackgroundDrawable = getResizingBackgroundDrawable(context, i, i2, z);
            if (this.mResizingBackgroundDrawable == null) {
                String str = this.mLogTag;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to find background drawable for PhoneWindow=");
                stringBuilder.append(this.mWindow);
                Log.w(str, stringBuilder.toString());
            }
        }
        if (this.mCaptionBackgroundDrawable == null) {
            this.mCaptionBackgroundDrawable = getContext().getDrawable(17302109);
        }
        if (this.mResizingBackgroundDrawable != null) {
            this.mLastBackgroundDrawableCb = this.mResizingBackgroundDrawable.getCallback();
            this.mResizingBackgroundDrawable.setCallback(null);
        }
    }

    private DecorCaptionView createDecorCaptionView(LayoutInflater inflater) {
        DecorCaptionView decorCaptionView = null;
        boolean z = true;
        for (int i = getChildCount() - 1; i >= 0 && decorCaptionView == null; i--) {
            View view = getChildAt(i);
            if (view instanceof DecorCaptionView) {
                decorCaptionView = (DecorCaptionView) view;
                removeViewAt(i);
            }
        }
        LayoutParams attrs = this.mWindow.getAttributes();
        boolean isApplication = attrs.type == 1 || attrs.type == 2 || attrs.type == 4;
        WindowConfiguration winConfig = getResources().getConfiguration().windowConfiguration;
        if (!this.mWindow.isFloating() && isApplication && winConfig.hasWindowDecorCaption()) {
            if (decorCaptionView == null) {
                decorCaptionView = inflateDecorCaptionView(inflater);
            }
            decorCaptionView.setPhoneWindow(this.mWindow, true);
        } else {
            decorCaptionView = null;
        }
        if (decorCaptionView == null) {
            z = false;
        }
        enableCaption(z);
        return decorCaptionView;
    }

    private DecorCaptionView inflateDecorCaptionView(LayoutInflater inflater) {
        Context context = getContext();
        DecorCaptionView view = getHwDecorCaptionView(LayoutInflater.from(context), context);
        setDecorCaptionShade(context, view);
        return view;
    }

    private DecorCaptionView getHwDecorCaptionView(LayoutInflater inflater, Context context) {
        if (!HwPCUtils.isValidExtDisplayId(this.mWindow.getContext()) && (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastMode())) {
            return (DecorCaptionView) inflater.inflate(17367127, null);
        }
        DecorCaptionView view = HwWidgetFactory.getHwDecorCaptionView(inflater);
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(16843827, value, true);
        if (Color.alpha(value.data) != 0) {
            return view;
        }
        view.setBackgroundResource(33751727);
        this.mWindow.setDecorCaptionShade(2);
        return view;
    }

    private void setDecorCaptionShade(Context context, DecorCaptionView view) {
        switch (this.mWindow.getDecorCaptionShade()) {
            case 1:
                setLightDecorCaptionShade(view);
                return;
            case 2:
                setDarkDecorCaptionShade(view);
                return;
            default:
                TypedValue value = new TypedValue();
                context.getTheme().resolveAttribute(16843827, value, true);
                if (((double) Color.luminance(value.data)) < 0.5d) {
                    setLightDecorCaptionShade(view);
                    return;
                } else {
                    setDarkDecorCaptionShade(view);
                    return;
                }
        }
    }

    void updateDecorCaptionShade() {
        if (this.mDecorCaptionView != null) {
            setDecorCaptionShade(getContext(), this.mDecorCaptionView);
        }
    }

    private void setLightDecorCaptionShade(DecorCaptionView view) {
        if (view instanceof AbsHwDecorCaptionView) {
            ((AbsHwDecorCaptionView) view).updateShade(true);
            return;
        }
        view.findViewById(16909065).setBackgroundResource(17302114);
        view.findViewById(16908818).setBackgroundResource(17302112);
    }

    private void setDarkDecorCaptionShade(DecorCaptionView view) {
        if (view instanceof AbsHwDecorCaptionView) {
            ((AbsHwDecorCaptionView) view).updateShade(false);
            return;
        }
        view.findViewById(16909065).setBackgroundResource(17302113);
        view.findViewById(16908818).setBackgroundResource(17302111);
    }

    public static Drawable getResizingBackgroundDrawable(Context context, int backgroundRes, int backgroundFallbackRes, boolean windowTranslucent) {
        Drawable drawable;
        if (backgroundRes != 0) {
            drawable = context.getDrawable(backgroundRes);
            if (drawable != null) {
                return enforceNonTranslucentBackground(drawable, windowTranslucent);
            }
        }
        if (backgroundFallbackRes != 0) {
            drawable = context.getDrawable(backgroundFallbackRes);
            if (drawable != null) {
                return enforceNonTranslucentBackground(drawable, windowTranslucent);
            }
        }
        return new ColorDrawable(Tonal.MAIN_COLOR_DARK);
    }

    private static Drawable enforceNonTranslucentBackground(Drawable drawable, boolean windowTranslucent) {
        if (!windowTranslucent && (drawable instanceof ColorDrawable)) {
            ColorDrawable colorDrawable = (ColorDrawable) drawable;
            int color = colorDrawable.getColor();
            if (Color.alpha(color) != 255) {
                ColorDrawable copy = (ColorDrawable) colorDrawable.getConstantState().newDrawable().mutate();
                copy.setColor(Color.argb(255, Color.red(color), Color.green(color), Color.blue(color)));
                return copy;
            }
        }
        return drawable;
    }

    void clearContentView() {
        if (this.mDecorCaptionView != null) {
            this.mDecorCaptionView.removeContentView();
            return;
        }
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View v = getChildAt(i);
            if (!(v == this.mStatusColorViewState.view || v == this.mNavigationColorViewState.view || v == this.mStatusGuard)) {
                removeViewAt(i);
            }
        }
    }

    public void onWindowSizeIsChanging(Rect newBounds, boolean fullscreen, Rect systemInsets, Rect stableInsets) {
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.setTargetRect(newBounds, fullscreen, systemInsets, stableInsets);
        }
    }

    public void onWindowDragResizeStart(Rect initialBounds, boolean fullscreen, Rect systemInsets, Rect stableInsets, int resizeMode) {
        if (this.mWindow.isDestroyed()) {
            releaseThreadedRenderer();
        } else if (this.mBackdropFrameRenderer == null) {
            ThreadedRenderer renderer = getThreadedRenderer();
            if (renderer != null) {
                loadBackgroundDrawablesIfNeeded();
                this.mBackdropFrameRenderer = new BackdropFrameRenderer(this, renderer, initialBounds, this.mResizingBackgroundDrawable, this.mCaptionBackgroundDrawable, this.mUserCaptionBackgroundDrawable, getCurrentColor(this.mStatusColorViewState), getCurrentColor(this.mNavigationColorViewState), fullscreen, systemInsets, stableInsets, resizeMode);
                updateElevation();
                updateColorViews(null, false);
            }
            this.mResizeMode = resizeMode;
            getViewRootImpl().requestInvalidateRootRenderNode();
        }
    }

    public void onWindowDragResizeEnd() {
        releaseThreadedRenderer();
        updateColorViews(null, false);
        this.mResizeMode = -1;
        getViewRootImpl().requestInvalidateRootRenderNode();
    }

    public boolean onContentDrawn(int offsetX, int offsetY, int sizeX, int sizeY) {
        if (this.mBackdropFrameRenderer == null) {
            return false;
        }
        return this.mBackdropFrameRenderer.onContentDrawn(offsetX, offsetY, sizeX, sizeY);
    }

    public void onRequestDraw(boolean reportNextDraw) {
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.onRequestDraw(reportNextDraw);
        } else if (reportNextDraw && isAttachedToWindow()) {
            getViewRootImpl().reportDrawFinish();
        }
    }

    public void onPostDraw(DisplayListCanvas canvas) {
        drawResizingShadowIfNeeded(canvas);
    }

    private void initResizingPaints() {
        int middleColor = (this.mContext.getResources().getColor(17170741, null) + this.mContext.getResources().getColor(17170740, null)) / 2;
        this.mHorizontalResizeShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, (float) this.mResizeShadowSize, new int[]{startColor, middleColor, endColor}, new float[]{0.0f, 0.3f, 1.0f}, TileMode.CLAMP));
        this.mVerticalResizeShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, (float) this.mResizeShadowSize, 0.0f, new int[]{startColor, middleColor, endColor}, new float[]{0.0f, 0.3f, 1.0f}, TileMode.CLAMP));
    }

    private void drawResizingShadowIfNeeded(DisplayListCanvas canvas) {
        if (this.mResizeMode == 1 && !this.mWindow.mIsFloating && !this.mWindow.isTranslucent() && !this.mWindow.isShowingWallpaper()) {
            canvas.save();
            canvas.translate(0.0f, (float) (getHeight() - this.mFrameOffsets.bottom));
            canvas.drawRect(0.0f, 0.0f, (float) getWidth(), (float) this.mResizeShadowSize, this.mHorizontalResizeShadowPaint);
            canvas.restore();
            canvas.save();
            canvas.translate((float) (getWidth() - this.mFrameOffsets.right), 0.0f);
            canvas.drawRect(0.0f, 0.0f, (float) this.mResizeShadowSize, (float) getHeight(), this.mVerticalResizeShadowPaint);
            canvas.restore();
        }
    }

    private void releaseThreadedRenderer() {
        if (!(this.mResizingBackgroundDrawable == null || this.mLastBackgroundDrawableCb == null)) {
            this.mResizingBackgroundDrawable.setCallback(this.mLastBackgroundDrawableCb);
            this.mLastBackgroundDrawableCb = null;
        }
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.releaseRenderer();
            this.mBackdropFrameRenderer = null;
            updateElevation();
        }
    }

    private boolean isResizing() {
        return this.mBackdropFrameRenderer != null;
    }

    private void initializeElevation() {
        this.mAllowUpdateElevation = false;
        updateElevation();
    }

    private void updateElevation() {
        float elevation = 0.0f;
        boolean wasAdjustedForStack = this.mElevationAdjustedForStack;
        int windowingMode = getResources().getConfiguration().windowConfiguration.getWindowingMode();
        float f = 5.0f;
        if (windowingMode == 5 && !isResizing()) {
            if (hasWindowFocus()) {
                f = 20.0f;
            }
            elevation = f;
            if (!this.mAllowUpdateElevation) {
                elevation = 20.0f;
            }
            elevation = dipToPx(elevation);
            this.mElevationAdjustedForStack = true;
        } else if (windowingMode == 2) {
            elevation = dipToPx(5.0f);
            this.mElevationAdjustedForStack = true;
        } else {
            this.mElevationAdjustedForStack = false;
        }
        if ((wasAdjustedForStack || this.mElevationAdjustedForStack) && getElevation() != elevation) {
            this.mWindow.setElevation(elevation);
        }
    }

    boolean isShowingCaption() {
        return this.mDecorCaptionView != null && this.mDecorCaptionView.isCaptionShowing();
    }

    int getCaptionHeight() {
        return isShowingCaption() ? this.mDecorCaptionView.getCaptionHeight() : 0;
    }

    private float dipToPx(float dip) {
        return TypedValue.applyDimension(1, dip, getResources().getDisplayMetrics());
    }

    void setUserCaptionBackgroundDrawable(Drawable drawable) {
        this.mUserCaptionBackgroundDrawable = drawable;
        if (this.mBackdropFrameRenderer != null) {
            this.mBackdropFrameRenderer.setUserCaptionBackgroundDrawable(drawable);
        }
    }

    private static String getTitleSuffix(LayoutParams params) {
        if (params == null) {
            return "";
        }
        String[] split = params.getTitle().toString().split("\\.");
        if (split.length > 0) {
            return split[split.length - 1];
        }
        return "";
    }

    void updateLogTag(LayoutParams params) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DecorView[");
        stringBuilder.append(getTitleSuffix(params));
        stringBuilder.append("]");
        this.mLogTag = stringBuilder.toString();
    }

    public void updateAvailableWidth() {
        Resources res = getResources();
        this.mAvailableWidth = TypedValue.applyDimension(1, (float) res.getConfiguration().screenWidthDp, res.getDisplayMetrics());
    }

    public void requestKeyboardShortcuts(List<KeyboardShortcutGroup> list, int deviceId) {
        PanelFeatureState st = this.mWindow.getPanelState(0, false);
        Menu menu = st != null ? st.menu : null;
        if (!this.mWindow.isDestroyed() && this.mWindow.getCallback() != null) {
            this.mWindow.getCallback().onProvideKeyboardShortcuts(list, menu, deviceId);
        }
    }

    public void dispatchPointerCaptureChanged(boolean hasCapture) {
        super.dispatchPointerCaptureChanged(hasCapture);
        if (!this.mWindow.isDestroyed() && this.mWindow.getCallback() != null) {
            this.mWindow.getCallback().onPointerCaptureChanged(hasCapture);
        }
    }

    public int getAccessibilityViewId() {
        return 2147483646;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DecorView@");
        stringBuilder.append(Integer.toHexString(hashCode()));
        stringBuilder.append("[");
        stringBuilder.append(getTitleSuffix(this.mWindow.getAttributes()));
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
