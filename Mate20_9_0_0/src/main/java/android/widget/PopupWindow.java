package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.IBinder;
import android.os.SystemProperties;
import android.rms.AppAssociate;
import android.transition.Transition;
import android.transition.Transition.EpicenterCallback;
import android.transition.Transition.TransitionListener;
import android.transition.TransitionInflater;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.KeyEvent.DispatcherState;
import android.view.KeyboardShortcutGroup;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import com.android.internal.R;
import java.lang.ref.WeakReference;
import java.util.List;

public class PopupWindow {
    private static final int[] ABOVE_ANCHOR_STATE_SET = new int[]{16842922};
    private static final int ANIMATION_STYLE_DEFAULT = -1;
    private static final int DEFAULT_ANCHORED_GRAVITY = 8388659;
    public static final int INPUT_METHOD_FROM_FOCUSABLE = 0;
    public static final int INPUT_METHOD_NEEDED = 1;
    public static final int INPUT_METHOD_NOT_NEEDED = 2;
    private boolean mAboveAnchor;
    private Drawable mAboveAnchorBackgroundDrawable;
    private boolean mAllowScrollingAnchorParent;
    private WeakReference<View> mAnchor;
    private WeakReference<View> mAnchorRoot;
    private int mAnchorXoff;
    private int mAnchorYoff;
    private int mAnchoredGravity;
    private int mAnimationStyle;
    private boolean mAttachedInDecor;
    private boolean mAttachedInDecorSet;
    private Drawable mBackground;
    private View mBackgroundView;
    private Drawable mBelowAnchorBackgroundDrawable;
    private boolean mClipToScreen;
    private boolean mClippingEnabled;
    private View mContentView;
    private Context mContext;
    private PopupDecorView mDecorView;
    private float mDownX;
    private float mDownY;
    private float mElevation;
    private Transition mEnterTransition;
    private Rect mEpicenterBounds;
    private Transition mExitTransition;
    private boolean mFocusable;
    private int mGravity;
    private int mHeight;
    private int mHeightMode;
    private boolean mIgnoreCheekPress;
    private int mInputMethodMode;
    private boolean mIsAnchorRootAttached;
    private boolean mIsDropdown;
    private boolean mIsShowing;
    private boolean mIsTransitioningToDismiss;
    private int mLastHeight;
    private int mLastWidth;
    private boolean mLayoutInScreen;
    private boolean mLayoutInsetDecor;
    protected int mNewDisplayFrameLeft;
    protected int mNewDisplayFrameRight;
    private boolean mNotTouchModal;
    private final OnAttachStateChangeListener mOnAnchorDetachedListener;
    private final OnAttachStateChangeListener mOnAnchorRootDetachedListener;
    private OnDismissListener mOnDismissListener;
    private final OnLayoutChangeListener mOnLayoutChangeListener;
    private final OnScrollChangedListener mOnScrollChangedListener;
    private boolean mOutsideTouchable;
    private boolean mOverlapAnchor;
    private WeakReference<View> mParentRootView;
    private boolean mPopupViewInitialLayoutDirectionInherited;
    private int mSlop;
    private int mSoftInputMode;
    private int mSplitTouchEnabled;
    private final Rect mTempRect;
    private CharSequence mTitle;
    private final int[] mTmpAppLocation;
    private final int[] mTmpDrawingLocation;
    private final int[] mTmpScreenLocation;
    private OnTouchListener mTouchInterceptor;
    private boolean mTouchable;
    private int mWidth;
    private int mWidthMode;
    private int mWindowLayoutType;
    private WindowManager mWindowManager;

    public interface OnDismissListener {
        void onDismiss();
    }

    private class PopupBackgroundView extends FrameLayout {
        public PopupBackgroundView(Context context) {
            super(context);
        }

        protected int[] onCreateDrawableState(int extraSpace) {
            if (!PopupWindow.this.mAboveAnchor) {
                return super.onCreateDrawableState(extraSpace);
            }
            int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
            View.mergeDrawableStates(drawableState, PopupWindow.ABOVE_ANCHOR_STATE_SET);
            return drawableState;
        }
    }

    private class PopupDecorView extends FrameLayout {
        private Runnable mCleanupAfterExit;
        private final OnAttachStateChangeListener mOnAnchorRootDetachedListener = new OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) {
            }

            public void onViewDetachedFromWindow(View v) {
                v.removeOnAttachStateChangeListener(this);
                if (PopupDecorView.this.isAttachedToWindow()) {
                    TransitionManager.endTransitions(PopupDecorView.this);
                }
            }
        };

        public PopupDecorView(Context context) {
            super(context);
        }

        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() != 4) {
                return super.dispatchKeyEvent(event);
            }
            if (getKeyDispatcherState() == null) {
                return super.dispatchKeyEvent(event);
            }
            DispatcherState state;
            if (event.getAction() == 0 && event.getRepeatCount() == 0) {
                state = getKeyDispatcherState();
                if (state != null) {
                    state.startTracking(event, this);
                }
                return true;
            }
            if (event.getAction() == 1) {
                state = getKeyDispatcherState();
                if (!(state == null || !state.isTracking(event) || event.isCanceled())) {
                    PopupWindow.this.dismiss();
                    return true;
                }
            }
            return super.dispatchKeyEvent(event);
        }

        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (PopupWindow.this.mTouchInterceptor == null || !PopupWindow.this.mTouchInterceptor.onTouch(this, ev)) {
                return super.dispatchTouchEvent(ev);
            }
            return true;
        }

        public boolean onTouchEvent(MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (event.getAction() == 0) {
                PopupWindow.this.mDownX = (float) x;
                PopupWindow.this.mDownY = (float) y;
            }
            if (event.getAction() == 0 && (x < 0 || x >= getWidth() || y < 0 || y >= getHeight())) {
                return true;
            }
            if (event.getAction() == 1 && (x < 0 || x >= getWidth() || y < 0 || y >= getHeight())) {
                if (Math.abs(((float) x) - PopupWindow.this.mDownX) <= ((float) PopupWindow.this.mSlop) && Math.abs(((float) y) - PopupWindow.this.mDownY) <= ((float) PopupWindow.this.mSlop)) {
                    PopupWindow.this.dismiss();
                }
                return true;
            } else if (event.getAction() != 4) {
                return super.onTouchEvent(event);
            } else {
                PopupWindow.this.dismiss();
                return true;
            }
        }

        public void requestEnterTransition(Transition transition) {
            ViewTreeObserver observer = getViewTreeObserver();
            if (observer != null && transition != null) {
                final Transition enterTransition = transition.clone();
                observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        ViewTreeObserver observer = PopupDecorView.this.getViewTreeObserver();
                        if (observer != null) {
                            observer.removeOnGlobalLayoutListener(this);
                        }
                        final Rect epicenter = PopupWindow.this.getTransitionEpicenter();
                        enterTransition.setEpicenterCallback(new EpicenterCallback() {
                            public Rect onGetEpicenter(Transition transition) {
                                return epicenter;
                            }
                        });
                        PopupDecorView.this.startEnterTransition(enterTransition);
                    }
                });
            }
        }

        private void startEnterTransition(Transition enterTransition) {
            int i;
            int count = getChildCount();
            for (i = 0; i < count; i++) {
                View child = getChildAt(i);
                enterTransition.addTarget(child);
                child.setTransitionVisibility(4);
            }
            TransitionManager.beginDelayedTransition(this, enterTransition);
            for (i = 0; i < count; i++) {
                getChildAt(i).setTransitionVisibility(0);
            }
        }

        public void startExitTransition(Transition transition, View anchorRoot, final Rect epicenter, TransitionListener listener) {
            if (transition != null) {
                if (anchorRoot != null) {
                    anchorRoot.addOnAttachStateChangeListener(this.mOnAnchorRootDetachedListener);
                }
                this.mCleanupAfterExit = new -$$Lambda$PopupWindow$PopupDecorView$T99WKEnQefOCXbbKvW95WY38p_I(this, listener, transition, anchorRoot);
                Transition exitTransition = transition.clone();
                exitTransition.addListener(new TransitionListenerAdapter() {
                    public void onTransitionEnd(Transition t) {
                        t.removeListener(this);
                        if (PopupDecorView.this.mCleanupAfterExit != null) {
                            PopupDecorView.this.mCleanupAfterExit.run();
                        }
                    }
                });
                exitTransition.setEpicenterCallback(new EpicenterCallback() {
                    public Rect onGetEpicenter(Transition transition) {
                        return epicenter;
                    }
                });
                int count = getChildCount();
                int i = 0;
                for (int i2 = 0; i2 < count; i2++) {
                    exitTransition.addTarget(getChildAt(i2));
                }
                TransitionManager.beginDelayedTransition(this, exitTransition);
                while (i < count) {
                    getChildAt(i).setVisibility(4);
                    i++;
                }
            }
        }

        public static /* synthetic */ void lambda$startExitTransition$0(PopupDecorView popupDecorView, TransitionListener listener, Transition transition, View anchorRoot) {
            listener.onTransitionEnd(transition);
            if (anchorRoot != null) {
                anchorRoot.removeOnAttachStateChangeListener(popupDecorView.mOnAnchorRootDetachedListener);
            }
            popupDecorView.mCleanupAfterExit = null;
        }

        public void cancelTransitions() {
            TransitionManager.endTransitions(this);
            if (this.mCleanupAfterExit != null) {
                this.mCleanupAfterExit.run();
            }
        }

        public void requestKeyboardShortcuts(List<KeyboardShortcutGroup> list, int deviceId) {
            if (PopupWindow.this.mParentRootView != null) {
                View parentRoot = (View) PopupWindow.this.mParentRootView.get();
                if (parentRoot != null) {
                    parentRoot.requestKeyboardShortcuts(list, deviceId);
                }
            }
        }
    }

    public PopupWindow(Context context) {
        this(context, null);
    }

    public PopupWindow(Context context, AttributeSet attrs) {
        this(context, attrs, 16842870);
    }

    public PopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.mTmpDrawingLocation = new int[2];
        this.mTmpScreenLocation = new int[2];
        this.mTmpAppLocation = new int[2];
        this.mTempRect = new Rect();
        this.mInputMethodMode = 0;
        this.mSoftInputMode = 1;
        this.mTouchable = true;
        this.mOutsideTouchable = false;
        this.mClippingEnabled = true;
        this.mSplitTouchEnabled = -1;
        this.mAllowScrollingAnchorParent = true;
        this.mLayoutInsetDecor = false;
        this.mAttachedInDecor = true;
        this.mAttachedInDecorSet = false;
        this.mWidth = -2;
        this.mHeight = -2;
        this.mWindowLayoutType = 1000;
        this.mIgnoreCheekPress = false;
        this.mAnimationStyle = -1;
        this.mGravity = 0;
        this.mSlop = 32;
        this.mOnAnchorDetachedListener = new OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) {
                PopupWindow.this.alignToAnchor();
            }

            public void onViewDetachedFromWindow(View v) {
            }
        };
        this.mOnAnchorRootDetachedListener = new OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) {
            }

            public void onViewDetachedFromWindow(View v) {
                PopupWindow.this.mIsAnchorRootAttached = false;
            }
        };
        this.mOnScrollChangedListener = new -$$Lambda$PopupWindow$nV1HS3Nc6Ck5JRIbIHe3mkyHWzc(this);
        this.mOnLayoutChangeListener = new -$$Lambda$PopupWindow$8Gc2stI5cSJZbuKX7X4Qr_vU2nI(this);
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService(AppAssociate.ASSOC_WINDOW);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PopupWindow, defStyleAttr, defStyleRes);
        Drawable bg = a.getDrawable(0);
        this.mElevation = a.getDimension(3, 0.0f);
        this.mOverlapAnchor = a.getBoolean(2, false);
        if (a.hasValueOrEmpty(1)) {
            int animStyle = a.getResourceId(1, 0);
            if (animStyle == 16974588) {
                this.mAnimationStyle = -1;
            } else {
                this.mAnimationStyle = animStyle;
            }
        } else {
            this.mAnimationStyle = -1;
        }
        Transition enterTransition = getTransition(a.getResourceId(4, 0));
        Transition exitTransition = a.hasValueOrEmpty(5) ? getTransition(a.getResourceId(5, 0)) : enterTransition == null ? null : enterTransition.clone();
        a.recycle();
        setEnterTransition(enterTransition);
        setExitTransition(exitTransition);
        setBackgroundDrawable(bg);
        this.mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public PopupWindow() {
        this(null, 0, 0);
    }

    public PopupWindow(View contentView) {
        this(contentView, 0, 0);
    }

    public PopupWindow(int width, int height) {
        this(null, width, height);
    }

    public PopupWindow(View contentView, int width, int height) {
        this(contentView, width, height, false);
    }

    public PopupWindow(View contentView, int width, int height, boolean focusable) {
        this.mTmpDrawingLocation = new int[2];
        this.mTmpScreenLocation = new int[2];
        this.mTmpAppLocation = new int[2];
        this.mTempRect = new Rect();
        this.mInputMethodMode = 0;
        this.mSoftInputMode = 1;
        this.mTouchable = true;
        this.mOutsideTouchable = false;
        this.mClippingEnabled = true;
        this.mSplitTouchEnabled = -1;
        this.mAllowScrollingAnchorParent = true;
        this.mLayoutInsetDecor = false;
        this.mAttachedInDecor = true;
        this.mAttachedInDecorSet = false;
        this.mWidth = -2;
        this.mHeight = -2;
        this.mWindowLayoutType = 1000;
        this.mIgnoreCheekPress = false;
        this.mAnimationStyle = -1;
        this.mGravity = 0;
        this.mSlop = 32;
        this.mOnAnchorDetachedListener = /* anonymous class already generated */;
        this.mOnAnchorRootDetachedListener = /* anonymous class already generated */;
        this.mOnScrollChangedListener = new -$$Lambda$PopupWindow$nV1HS3Nc6Ck5JRIbIHe3mkyHWzc(this);
        this.mOnLayoutChangeListener = new -$$Lambda$PopupWindow$8Gc2stI5cSJZbuKX7X4Qr_vU2nI(this);
        if (contentView != null) {
            this.mContext = contentView.getContext();
            this.mWindowManager = (WindowManager) this.mContext.getSystemService(AppAssociate.ASSOC_WINDOW);
        }
        setContentView(contentView);
        setWidth(width);
        setHeight(height);
        setFocusable(focusable);
    }

    public void setEnterTransition(Transition enterTransition) {
        this.mEnterTransition = enterTransition;
    }

    public Transition getEnterTransition() {
        return this.mEnterTransition;
    }

    public void setExitTransition(Transition exitTransition) {
        this.mExitTransition = exitTransition;
    }

    public Transition getExitTransition() {
        return this.mExitTransition;
    }

    public void setEpicenterBounds(Rect bounds) {
        this.mEpicenterBounds = bounds;
    }

    private Transition getTransition(int resId) {
        if (!(resId == 0 || resId == 17760256)) {
            Transition transition = TransitionInflater.from(this.mContext).inflateTransition(resId);
            if (transition != null) {
                boolean isEmpty = (transition instanceof TransitionSet) && ((TransitionSet) transition).getTransitionCount() == 0;
                if (!isEmpty) {
                    return transition;
                }
            }
        }
        return null;
    }

    public Drawable getBackground() {
        return this.mBackground;
    }

    public void setBackgroundDrawable(Drawable background) {
        this.mBackground = background;
        if (this.mBackground instanceof StateListDrawable) {
            StateListDrawable stateList = this.mBackground;
            int aboveAnchorStateIndex = stateList.getStateDrawableIndex(ABOVE_ANCHOR_STATE_SET);
            int count = stateList.getStateCount();
            int belowAnchorStateIndex = -1;
            for (int i = 0; i < count; i++) {
                if (i != aboveAnchorStateIndex) {
                    belowAnchorStateIndex = i;
                    break;
                }
            }
            if (aboveAnchorStateIndex == -1 || belowAnchorStateIndex == -1) {
                this.mBelowAnchorBackgroundDrawable = null;
                this.mAboveAnchorBackgroundDrawable = null;
                return;
            }
            this.mAboveAnchorBackgroundDrawable = stateList.getStateDrawable(aboveAnchorStateIndex);
            this.mBelowAnchorBackgroundDrawable = stateList.getStateDrawable(belowAnchorStateIndex);
        }
    }

    public float getElevation() {
        return this.mElevation;
    }

    public void setElevation(float elevation) {
        this.mElevation = elevation;
    }

    public int getAnimationStyle() {
        return this.mAnimationStyle;
    }

    public void setIgnoreCheekPress() {
        this.mIgnoreCheekPress = true;
    }

    public void setAnimationStyle(int animationStyle) {
        this.mAnimationStyle = animationStyle;
    }

    public View getContentView() {
        return this.mContentView;
    }

    public void setContentView(View contentView) {
        if (!isShowing()) {
            this.mContentView = contentView;
            if (this.mContext == null && this.mContentView != null) {
                this.mContext = this.mContentView.getContext();
            }
            if (this.mWindowManager == null && this.mContentView != null) {
                this.mWindowManager = (WindowManager) this.mContext.getSystemService(AppAssociate.ASSOC_WINDOW);
            }
            if (!(this.mContext == null || this.mAttachedInDecorSet)) {
                setAttachedInDecor(this.mContext.getApplicationInfo().targetSdkVersion >= 22);
            }
        }
    }

    public void setTouchInterceptor(OnTouchListener l) {
        this.mTouchInterceptor = l;
    }

    public boolean isFocusable() {
        return this.mFocusable;
    }

    public void setFocusable(boolean focusable) {
        this.mFocusable = focusable;
    }

    public int getInputMethodMode() {
        return this.mInputMethodMode;
    }

    public void setInputMethodMode(int mode) {
        this.mInputMethodMode = mode;
    }

    public void setSoftInputMode(int mode) {
        this.mSoftInputMode = mode;
    }

    public int getSoftInputMode() {
        return this.mSoftInputMode;
    }

    public boolean isTouchable() {
        return this.mTouchable;
    }

    public void setTouchable(boolean touchable) {
        this.mTouchable = touchable;
    }

    public boolean isOutsideTouchable() {
        return this.mOutsideTouchable;
    }

    public void setOutsideTouchable(boolean touchable) {
        this.mOutsideTouchable = touchable;
    }

    public boolean isClippingEnabled() {
        return this.mClippingEnabled;
    }

    public void setClippingEnabled(boolean enabled) {
        this.mClippingEnabled = enabled;
    }

    public void setClipToScreenEnabled(boolean enabled) {
        this.mClipToScreen = enabled;
    }

    void setAllowScrollingAnchorParent(boolean enabled) {
        this.mAllowScrollingAnchorParent = enabled;
    }

    protected final boolean getAllowScrollingAnchorParent() {
        return this.mAllowScrollingAnchorParent;
    }

    public boolean isSplitTouchEnabled() {
        boolean z = false;
        if (this.mSplitTouchEnabled >= 0 || this.mContext == null) {
            if (this.mSplitTouchEnabled == 1) {
                z = true;
            }
            return z;
        }
        if (this.mContext.getApplicationInfo().targetSdkVersion >= 11) {
            z = true;
        }
        return z;
    }

    public void setSplitTouchEnabled(boolean enabled) {
        this.mSplitTouchEnabled = enabled;
    }

    public boolean isLayoutInScreenEnabled() {
        return this.mLayoutInScreen;
    }

    public void setLayoutInScreenEnabled(boolean enabled) {
        this.mLayoutInScreen = enabled;
    }

    public boolean isAttachedInDecor() {
        return this.mAttachedInDecor;
    }

    public void setAttachedInDecor(boolean enabled) {
        this.mAttachedInDecor = enabled;
        this.mAttachedInDecorSet = true;
    }

    public void setLayoutInsetDecor(boolean enabled) {
        this.mLayoutInsetDecor = enabled;
    }

    protected final boolean isLayoutInsetDecor() {
        return this.mLayoutInsetDecor;
    }

    public void setWindowLayoutType(int layoutType) {
        this.mWindowLayoutType = layoutType;
    }

    public int getWindowLayoutType() {
        return this.mWindowLayoutType;
    }

    public void setTouchModal(boolean touchModal) {
        this.mNotTouchModal = touchModal ^ 1;
    }

    @Deprecated
    public void setWindowLayoutMode(int widthSpec, int heightSpec) {
        this.mWidthMode = widthSpec;
        this.mHeightMode = heightSpec;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public void setOverlapAnchor(boolean overlapAnchor) {
        this.mOverlapAnchor = overlapAnchor;
    }

    public boolean getOverlapAnchor() {
        return this.mOverlapAnchor;
    }

    public boolean isShowing() {
        return this.mIsShowing;
    }

    protected final void setShowing(boolean isShowing) {
        this.mIsShowing = isShowing;
    }

    protected final void setDropDown(boolean isDropDown) {
        this.mIsDropdown = isDropDown;
    }

    protected final void setTransitioningToDismiss(boolean transitioningToDismiss) {
        this.mIsTransitioningToDismiss = transitioningToDismiss;
    }

    protected final boolean isTransitioningToDismiss() {
        return this.mIsTransitioningToDismiss;
    }

    public void showAtLocation(View parent, int gravity, int x, int y) {
        this.mParentRootView = new WeakReference(parent.getRootView());
        showAtLocation(parent.getWindowToken(), gravity, x, y);
    }

    public void showAtLocation(IBinder token, int gravity, int x, int y) {
        if (!isShowing() && this.mContentView != null) {
            TransitionManager.endTransitions(this.mDecorView);
            detachFromAnchor();
            this.mIsShowing = true;
            this.mIsDropdown = false;
            this.mGravity = gravity;
            LayoutParams p = createPopupLayoutParams(token);
            preparePopup(p);
            p.x = x;
            p.y = y;
            invokePopup(p);
        }
    }

    public void showAsDropDown(View anchor) {
        showAsDropDown(anchor, 0, 0);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff) {
        showAsDropDown(anchor, xoff, yoff, DEFAULT_ANCHORED_GRAVITY);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        if (!isShowing() && hasContentView()) {
            TransitionManager.endTransitions(this.mDecorView);
            attachToAnchor(anchor, xoff, yoff, gravity);
            this.mIsShowing = true;
            this.mIsDropdown = true;
            LayoutParams p = createPopupLayoutParams(anchor.getApplicationWindowToken());
            preparePopup(p);
            updateAboveAnchor(findDropDownPosition(anchor, p, xoff, yoff, p.width, p.height, gravity, this.mAllowScrollingAnchorParent));
            p.accessibilityIdOfAnchor = anchor != null ? (long) anchor.getAccessibilityViewId() : -1;
            p.windowAnimations = computeAnimationResource();
            invokePopup(p);
        }
    }

    protected final void updateAboveAnchor(boolean aboveAnchor) {
        if (aboveAnchor != this.mAboveAnchor) {
            this.mAboveAnchor = aboveAnchor;
            if (this.mBackground != null && this.mBackgroundView != null) {
                if (this.mAboveAnchorBackgroundDrawable == null) {
                    this.mBackgroundView.refreshDrawableState();
                } else if (this.mAboveAnchor) {
                    this.mBackgroundView.setBackground(this.mAboveAnchorBackgroundDrawable);
                } else {
                    this.mBackgroundView.setBackground(this.mBelowAnchorBackgroundDrawable);
                }
            }
        }
    }

    public boolean isAboveAnchor() {
        return this.mAboveAnchor;
    }

    private void preparePopup(LayoutParams p) {
        if (this.mContentView == null || this.mContext == null || this.mWindowManager == null) {
            throw new IllegalStateException("You must specify a valid content view by calling setContentView() before attempting to show the popup.");
        }
        if (p.accessibilityTitle == null) {
            p.accessibilityTitle = this.mContext.getString(17040941);
        }
        if (this.mDecorView != null) {
            this.mDecorView.cancelTransitions();
        }
        if (this.mBackground != null) {
            this.mBackgroundView = createBackgroundView(this.mContentView);
            this.mBackgroundView.setBackground(this.mBackground);
        } else {
            this.mBackgroundView = this.mContentView;
        }
        this.mDecorView = createDecorView(this.mBackgroundView);
        boolean z = true;
        this.mDecorView.setIsRootNamespace(true);
        this.mBackgroundView.setElevation(this.mElevation);
        p.setSurfaceInsets(this.mBackgroundView, true, true);
        if (this.mContentView.getRawLayoutDirection() != 2) {
            z = false;
        }
        this.mPopupViewInitialLayoutDirectionInherited = z;
    }

    private PopupBackgroundView createBackgroundView(View contentView) {
        int height;
        ViewGroup.LayoutParams layoutParams = this.mContentView.getLayoutParams();
        if (layoutParams == null || layoutParams.height != -2) {
            height = -1;
        } else {
            height = -2;
        }
        PopupBackgroundView backgroundView = new PopupBackgroundView(this.mContext);
        backgroundView.addView(contentView, (ViewGroup.LayoutParams) new FrameLayout.LayoutParams(-1, height));
        return backgroundView;
    }

    private PopupDecorView createDecorView(View contentView) {
        int height;
        ViewGroup.LayoutParams layoutParams = this.mContentView.getLayoutParams();
        if (layoutParams == null || layoutParams.height != -2) {
            height = -1;
        } else {
            height = -2;
        }
        PopupDecorView decorView = new PopupDecorView(this.mContext);
        decorView.addView(contentView, -1, height);
        decorView.setClipChildren(false);
        decorView.setClipToPadding(false);
        return decorView;
    }

    private void invokePopup(LayoutParams p) {
        if (this.mContext != null) {
            p.packageName = this.mContext.getPackageName();
        }
        if (this.mDecorView == null) {
            Log.d("mDecorView", "invokePopup mDecorView == null");
            return;
        }
        PopupDecorView decorView = this.mDecorView;
        decorView.setFitsSystemWindows(this.mLayoutInsetDecor);
        setLayoutDirectionFromAnchor();
        this.mWindowManager.addView(decorView, p);
        if (this.mEnterTransition != null) {
            decorView.requestEnterTransition(this.mEnterTransition);
        }
    }

    private void setLayoutDirectionFromAnchor() {
        if (this.mAnchor != null) {
            View anchor = (View) this.mAnchor.get();
            if (anchor != null && this.mPopupViewInitialLayoutDirectionInherited) {
                this.mDecorView.setLayoutDirection(anchor.getLayoutDirection());
            }
        }
    }

    private int computeGravity() {
        int gravity = this.mGravity == 0 ? DEFAULT_ANCHORED_GRAVITY : this.mGravity;
        if (!this.mIsDropdown) {
            return gravity;
        }
        if (this.mClipToScreen || this.mClippingEnabled) {
            return gravity | 268435456;
        }
        return gravity;
    }

    protected final LayoutParams createPopupLayoutParams(IBinder token) {
        int i;
        LayoutParams p = new LayoutParams();
        p.gravity = computeGravity();
        p.flags = computeFlags(p.flags);
        p.type = this.mWindowLayoutType;
        p.token = token;
        p.softInputMode = this.mSoftInputMode;
        p.windowAnimations = computeAnimationResource();
        if (this.mBackground != null) {
            p.format = this.mBackground.getOpacity();
        } else {
            p.format = -3;
        }
        if (this.mHeightMode < 0) {
            i = this.mHeightMode;
            this.mLastHeight = i;
            p.height = i;
        } else {
            i = this.mHeight;
            this.mLastHeight = i;
            p.height = i;
        }
        if (this.mWidthMode < 0) {
            i = this.mWidthMode;
            this.mLastWidth = i;
            p.width = i;
        } else {
            i = this.mWidth;
            this.mLastWidth = i;
            p.width = i;
        }
        p.privateFlags = 98304;
        if (this.mTitle != null) {
            p.setTitle(this.mTitle);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PopupWindow:");
            stringBuilder.append(Integer.toHexString(hashCode()));
            p.setTitle(stringBuilder.toString());
        }
        return p;
    }

    private int computeFlags(int curFlags) {
        curFlags &= -8815129;
        if (this.mIgnoreCheekPress) {
            curFlags |= 32768;
        }
        if (!this.mFocusable) {
            curFlags |= 8;
            if (this.mInputMethodMode == 1) {
                curFlags |= 131072;
            }
        } else if (this.mInputMethodMode == 2) {
            curFlags |= 131072;
        }
        if (!this.mTouchable) {
            curFlags |= 16;
        }
        if (this.mOutsideTouchable) {
            curFlags |= 262144;
        }
        if (!this.mClippingEnabled || this.mClipToScreen) {
            curFlags |= 512;
        }
        if (isSplitTouchEnabled()) {
            curFlags |= 8388608;
        }
        if (this.mLayoutInScreen) {
            curFlags |= 256;
        }
        if (this.mLayoutInsetDecor) {
            curFlags |= 65536;
        }
        if (this.mNotTouchModal) {
            curFlags |= 32;
        }
        if (this.mAttachedInDecor) {
            return curFlags | 1073741824;
        }
        return curFlags;
    }

    private int computeAnimationResource() {
        if (this.mAnimationStyle != -1) {
            return this.mAnimationStyle;
        }
        if (!this.mIsDropdown) {
            return 0;
        }
        int i;
        if (this.mAboveAnchor) {
            i = 16974576;
        } else {
            i = 16974575;
        }
        return i;
    }

    public void setPopupLocation(int start, int end) {
        this.mNewDisplayFrameLeft = start;
        this.mNewDisplayFrameRight = end;
    }

    protected boolean findDropDownPosition(View anchor, LayoutParams outParams, int xOffset, int yOffset, int width, int height, int gravity, boolean allowScroll) {
        int yOffset2;
        int height2;
        boolean z;
        View view = anchor;
        LayoutParams layoutParams = outParams;
        int anchorHeight = anchor.getHeight();
        int anchorWidth = anchor.getWidth();
        if (this.mOverlapAnchor) {
            yOffset2 = yOffset - anchorHeight;
        } else {
            yOffset2 = yOffset;
        }
        int[] appScreenLocation = this.mTmpAppLocation;
        View appRootView = getAppRootView(anchor);
        appRootView.getLocationOnScreen(appScreenLocation);
        int[] screenLocation = this.mTmpScreenLocation;
        view.getLocationOnScreen(screenLocation);
        int[] drawingLocation = this.mTmpDrawingLocation;
        drawingLocation[0] = screenLocation[0] - appScreenLocation[0];
        drawingLocation[1] = screenLocation[1] - appScreenLocation[1];
        layoutParams.x = drawingLocation[0] + xOffset;
        layoutParams.y = (drawingLocation[1] + anchorHeight) + yOffset2;
        Rect displayFrame = new Rect();
        appRootView.getWindowVisibleDisplayFrame(displayFrame);
        if (!(this.mNewDisplayFrameLeft == 0 && this.mNewDisplayFrameRight == 0)) {
            displayFrame.left = this.mNewDisplayFrameLeft;
            displayFrame.right = this.mNewDisplayFrameRight;
        }
        int i = width;
        if (i == -1) {
            i = displayFrame.right - displayFrame.left;
        }
        int width2 = i;
        i = height;
        if (i == -1) {
            height2 = displayFrame.bottom - displayFrame.top;
        } else {
            height2 = i;
        }
        layoutParams.gravity = computeGravity();
        layoutParams.width = width2;
        layoutParams.height = height2;
        int hgrav = Gravity.getAbsoluteGravity(gravity, anchor.getLayoutDirection()) & 7;
        int[] appScreenLocation2 = appScreenLocation;
        if (hgrav == 5 && !SystemProperties.getRTLFlag()) {
            layoutParams.x -= width2 - anchorWidth;
        }
        int i2 = drawingLocation[1];
        int i3 = screenLocation[1];
        int[] screenLocation2 = screenLocation;
        View appRootView2 = appRootView;
        int hgrav2 = hgrav;
        LayoutParams layoutParams2 = layoutParams;
        int height3 = height2;
        int width3 = width2;
        Rect displayFrame2 = displayFrame;
        int[] drawingLocation2 = drawingLocation;
        int i4 = i3;
        int anchorHeight2 = anchorHeight;
        int[] screenLocation3 = screenLocation2;
        boolean fitsVertical = tryFitVertical(layoutParams2, yOffset2, height3, anchorHeight, i2, i4, displayFrame.top, displayFrame.bottom, null);
        boolean fitsHorizontal = tryFitHorizontal(layoutParams2, xOffset, width3, anchorWidth, drawingLocation2[0], screenLocation3[0], displayFrame2.left, displayFrame2.right, false);
        if (fitsVertical && fitsHorizontal) {
            int i5 = hgrav2;
            z = true;
        } else {
            boolean z2;
            Rect displayFrame3 = displayFrame2;
            view = anchor;
            int scrollX = anchor.getScrollX();
            int scrollY = anchor.getScrollY();
            Rect r = new Rect(scrollX, scrollY, (scrollX + width3) + xOffset, ((scrollY + height3) + anchorHeight2) + yOffset2);
            int hgrav3;
            if (allowScroll) {
                z2 = true;
                if (view.requestRectangleOnScreen(r, true)) {
                    view.getLocationOnScreen(screenLocation3);
                    drawingLocation2[0] = screenLocation3[0] - appScreenLocation2[0];
                    drawingLocation2[1] = screenLocation3[1] - appScreenLocation2[1];
                    layoutParams.x = drawingLocation2[0] + xOffset;
                    layoutParams.y = (drawingLocation2[1] + anchorHeight2) + yOffset2;
                    hgrav3 = hgrav2;
                    if (hgrav3 == 5 && !SystemProperties.getRTLFlag()) {
                        layoutParams.x -= width3 - anchorWidth;
                    }
                } else {
                    hgrav3 = hgrav2;
                }
            } else {
                hgrav3 = hgrav2;
                z2 = true;
            }
            int i6 = drawingLocation2[z2];
            hgrav2 = screenLocation3[z2];
            width2 = displayFrame3.top;
            layoutParams2 = layoutParams;
            z = z2;
            int i7 = i6;
            i4 = hgrav2;
            scrollY = width2;
            displayFrame2 = displayFrame3;
            tryFitVertical(layoutParams2, yOffset2, height3, anchorHeight2, i7, i4, scrollY, displayFrame3.bottom, this.mClipToScreen);
            tryFitHorizontal(layoutParams2, xOffset, width3, anchorWidth, drawingLocation2[0], screenLocation3[0], displayFrame2.left, displayFrame2.right, this.mClipToScreen);
        }
        return layoutParams.y < drawingLocation2[z] ? z : false;
    }

    private boolean tryFitVertical(LayoutParams outParams, int yOffset, int height, int anchorHeight, int drawingLocationY, int screenLocationY, int displayFrameTop, int displayFrameBottom, boolean allowResize) {
        LayoutParams layoutParams = outParams;
        int i = height;
        int i2 = displayFrameBottom;
        int anchorTopInScreen = layoutParams.y + (screenLocationY - drawingLocationY);
        int spaceBelow = i2 - anchorTopInScreen;
        if (anchorTopInScreen >= 0 && i <= spaceBelow) {
            return true;
        }
        PopupWindow popupWindow;
        int spaceAbove = (anchorTopInScreen - anchorHeight) - displayFrameTop;
        if (i <= spaceAbove) {
            int yOffset2;
            popupWindow = this;
            if (popupWindow.mOverlapAnchor) {
                yOffset2 = yOffset + anchorHeight;
            } else {
                yOffset2 = yOffset;
            }
            layoutParams.y = (drawingLocationY - i) + yOffset2;
            if (layoutParams.y + i <= i2) {
                return true;
            }
            int i3 = yOffset2;
        } else {
            popupWindow = this;
        }
        if (popupWindow.positionInDisplayVertical(layoutParams, i, drawingLocationY, screenLocationY, displayFrameTop, i2, allowResize)) {
            return true;
        }
        return false;
    }

    private boolean positionInDisplayVertical(LayoutParams outParams, int height, int drawingLocationY, int screenLocationY, int displayFrameTop, int displayFrameBottom, boolean canResize) {
        boolean fitsInDisplay = true;
        int winOffsetY = screenLocationY - drawingLocationY;
        outParams.y += winOffsetY;
        outParams.height = height;
        int bottom = outParams.y + height;
        if (bottom > displayFrameBottom) {
            outParams.y -= bottom - displayFrameBottom;
        }
        if (outParams.y < displayFrameTop) {
            outParams.y = displayFrameTop;
            int displayFrameHeight = displayFrameBottom - displayFrameTop;
            if (!canResize || height <= displayFrameHeight) {
                fitsInDisplay = false;
            } else {
                outParams.height = displayFrameHeight;
            }
        }
        outParams.y -= winOffsetY;
        return fitsInDisplay;
    }

    private boolean tryFitHorizontal(LayoutParams outParams, int xOffset, int width, int anchorWidth, int drawingLocationX, int screenLocationX, int displayFrameLeft, int displayFrameRight, boolean allowResize) {
        int i;
        LayoutParams layoutParams = outParams;
        int anchorLeftInScreen = layoutParams.x + (screenLocationX - drawingLocationX);
        int spaceRight = displayFrameRight - anchorLeftInScreen;
        if (anchorLeftInScreen >= 0) {
            i = width;
            if (i <= spaceRight) {
                return true;
            }
        }
        i = width;
        if (positionInDisplayHorizontal(layoutParams, i, drawingLocationX, screenLocationX, displayFrameLeft, displayFrameRight, allowResize)) {
            return true;
        }
        return false;
    }

    private boolean positionInDisplayHorizontal(LayoutParams outParams, int width, int drawingLocationX, int screenLocationX, int displayFrameLeft, int displayFrameRight, boolean canResize) {
        boolean fitsInDisplay = true;
        int winOffsetX = screenLocationX - drawingLocationX;
        outParams.x += winOffsetX;
        int right = outParams.x + width;
        if (right > displayFrameRight) {
            outParams.x -= right - displayFrameRight;
        }
        if (outParams.x < displayFrameLeft) {
            outParams.x = displayFrameLeft;
            int displayFrameWidth = displayFrameRight - displayFrameLeft;
            if (!canResize || width <= displayFrameWidth) {
                fitsInDisplay = false;
            } else {
                outParams.width = displayFrameWidth;
            }
        }
        outParams.x -= winOffsetX;
        return fitsInDisplay;
    }

    public int getMaxAvailableHeight(View anchor) {
        return getMaxAvailableHeight(anchor, 0);
    }

    public int getMaxAvailableHeight(View anchor, int yOffset) {
        return getMaxAvailableHeight(anchor, yOffset, false);
    }

    public int getMaxAvailableHeight(View anchor, int yOffset, boolean ignoreBottomDecorations) {
        Rect displayFrame;
        int distanceToBottom;
        Rect visibleDisplayFrame = new Rect();
        getAppRootView(anchor).getWindowVisibleDisplayFrame(visibleDisplayFrame);
        if (ignoreBottomDecorations) {
            displayFrame = new Rect();
            anchor.getWindowDisplayFrame(displayFrame);
            displayFrame.top = visibleDisplayFrame.top;
            displayFrame.right = visibleDisplayFrame.right;
            displayFrame.left = visibleDisplayFrame.left;
        } else {
            displayFrame = visibleDisplayFrame;
        }
        int[] anchorPos = this.mTmpDrawingLocation;
        anchor.getLocationOnScreen(anchorPos);
        int bottomEdge = displayFrame.bottom;
        if (this.mOverlapAnchor) {
            distanceToBottom = (bottomEdge - anchorPos[1]) - yOffset;
        } else {
            distanceToBottom = (bottomEdge - (anchorPos[1] + anchor.getHeight())) - yOffset;
        }
        int returnedHeight = Math.max(distanceToBottom, (anchorPos[1] - displayFrame.top) + yOffset);
        if (this.mBackground == null) {
            return returnedHeight;
        }
        this.mBackground.getPadding(this.mTempRect);
        return returnedHeight - (this.mTempRect.top + this.mTempRect.bottom);
    }

    public void dismiss() {
        if (isShowing() && !isTransitioningToDismiss()) {
            ViewGroup contentHolder;
            final PopupDecorView decorView = this.mDecorView;
            final View contentView = this.mContentView;
            ViewParent contentParent = contentView.getParent();
            View anchorRoot = null;
            if (contentParent instanceof ViewGroup) {
                contentHolder = (ViewGroup) contentParent;
            } else {
                contentHolder = null;
            }
            decorView.cancelTransitions();
            this.mIsShowing = false;
            this.mIsTransitioningToDismiss = true;
            Transition exitTransition = this.mExitTransition;
            if (exitTransition != null && decorView.isLaidOut() && (this.mIsAnchorRootAttached || this.mAnchorRoot == null)) {
                LayoutParams p = (LayoutParams) decorView.getLayoutParams();
                p.flags |= 16;
                p.flags |= 8;
                p.flags &= -131073;
                this.mWindowManager.updateViewLayout(decorView, p);
                if (this.mAnchorRoot != null) {
                    anchorRoot = (View) this.mAnchorRoot.get();
                }
                decorView.startExitTransition(exitTransition, anchorRoot, getTransitionEpicenter(), new TransitionListenerAdapter() {
                    public void onTransitionEnd(Transition transition) {
                        PopupWindow.this.dismissImmediate(decorView, contentHolder, contentView);
                    }
                });
            } else {
                dismissImmediate(decorView, contentHolder, contentView);
            }
            detachFromAnchor();
            if (this.mOnDismissListener != null) {
                this.mOnDismissListener.onDismiss();
            }
        }
    }

    protected final Rect getTransitionEpicenter() {
        View anchor = this.mAnchor != null ? (View) this.mAnchor.get() : null;
        View decor = this.mDecorView;
        if (anchor == null || decor == null) {
            return null;
        }
        int[] anchorLocation = anchor.getLocationOnScreen();
        int[] popupLocation = this.mDecorView.getLocationOnScreen();
        Rect bounds = new Rect(0, 0, anchor.getWidth(), anchor.getHeight());
        bounds.offset(anchorLocation[0] - popupLocation[0], anchorLocation[1] - popupLocation[1]);
        if (this.mEpicenterBounds != null) {
            int offsetX = bounds.left;
            int offsetY = bounds.top;
            bounds.set(this.mEpicenterBounds);
            bounds.offset(offsetX, offsetY);
        }
        return bounds;
    }

    private void dismissImmediate(View decorView, ViewGroup contentHolder, View contentView) {
        if (decorView.getParent() != null) {
            this.mWindowManager.removeViewImmediate(decorView);
        }
        if (contentHolder != null) {
            contentHolder.removeView(contentView);
        }
        this.mDecorView = null;
        this.mBackgroundView = null;
        this.mIsTransitioningToDismiss = false;
    }

    public void setOnDismissListener(OnDismissListener onDismissListener) {
        this.mOnDismissListener = onDismissListener;
    }

    protected final OnDismissListener getOnDismissListener() {
        return this.mOnDismissListener;
    }

    public void update() {
        if (isShowing() && hasContentView()) {
            LayoutParams p = getDecorViewLayoutParams();
            boolean update = false;
            int newAnim = computeAnimationResource();
            if (newAnim != p.windowAnimations) {
                p.windowAnimations = newAnim;
                update = true;
            }
            int newFlags = computeFlags(p.flags);
            if (newFlags != p.flags) {
                p.flags = newFlags;
                update = true;
            }
            int newGravity = computeGravity();
            if (newGravity != p.gravity) {
                p.gravity = newGravity;
                update = true;
            }
            if (update) {
                update(this.mAnchor != null ? (View) this.mAnchor.get() : null, p);
            }
        }
    }

    protected void update(View anchor, LayoutParams params) {
        setLayoutDirectionFromAnchor();
        this.mWindowManager.updateViewLayout(this.mDecorView, params);
    }

    public void update(int width, int height) {
        LayoutParams p = getDecorViewLayoutParams();
        update(p.x, p.y, width, height, false);
    }

    public void update(int x, int y, int width, int height) {
        update(x, y, width, height, false);
    }

    public void update(int x, int y, int width, int height, boolean force) {
        int i = x;
        int i2 = y;
        int i3 = width;
        int i4 = height;
        if (i3 >= 0) {
            this.mLastWidth = i3;
            setWidth(i3);
        }
        if (i4 >= 0) {
            this.mLastHeight = i4;
            setHeight(i4);
        }
        if (isShowing() && hasContentView()) {
            LayoutParams p = getDecorViewLayoutParams();
            boolean update = force;
            int finalWidth = this.mWidthMode < 0 ? this.mWidthMode : this.mLastWidth;
            if (!(i3 == -1 || p.width == finalWidth)) {
                this.mLastWidth = finalWidth;
                p.width = finalWidth;
                update = true;
            }
            int finalHeight = this.mHeightMode < 0 ? this.mHeightMode : this.mLastHeight;
            if (!(i4 == -1 || p.height == finalHeight)) {
                this.mLastHeight = finalHeight;
                p.height = finalHeight;
                update = true;
            }
            if (p.x != i) {
                p.x = i;
                update = true;
            }
            if (p.y != i2) {
                p.y = i2;
                update = true;
            }
            int newAnim = computeAnimationResource();
            if (newAnim != p.windowAnimations) {
                p.windowAnimations = newAnim;
                update = true;
            }
            int newFlags = computeFlags(p.flags);
            if (newFlags != p.flags) {
                p.flags = newFlags;
                update = true;
            }
            int newGravity = computeGravity();
            if (newGravity != p.gravity) {
                p.gravity = newGravity;
                update = true;
            }
            View anchor = null;
            int newAccessibilityIdOfAnchor = -1;
            if (!(this.mAnchor == null || this.mAnchor.get() == null)) {
                anchor = (View) this.mAnchor.get();
                newAccessibilityIdOfAnchor = anchor.getAccessibilityViewId();
            }
            if (((long) newAccessibilityIdOfAnchor) != p.accessibilityIdOfAnchor) {
                p.accessibilityIdOfAnchor = (long) newAccessibilityIdOfAnchor;
                update = true;
            }
            if (update) {
                update(anchor, p);
            }
        }
    }

    protected boolean hasContentView() {
        return this.mContentView != null;
    }

    protected boolean hasDecorView() {
        return this.mDecorView != null;
    }

    protected LayoutParams getDecorViewLayoutParams() {
        return (LayoutParams) this.mDecorView.getLayoutParams();
    }

    public void update(View anchor, int width, int height) {
        update(anchor, false, 0, 0, width, height);
    }

    public void update(View anchor, int xoff, int yoff, int width, int height) {
        update(anchor, true, xoff, yoff, width, height);
    }

    private void update(View anchor, boolean updateLocation, int xoff, int yoff, int width, int height) {
        View view = anchor;
        int i = xoff;
        int i2 = yoff;
        if (isShowing() && hasContentView()) {
            int width2;
            boolean height2;
            WeakReference<View> oldAnchor = this.mAnchor;
            int gravity = this.mAnchoredGravity;
            boolean z = updateLocation && !(this.mAnchorXoff == i && this.mAnchorYoff == i2);
            boolean needsUpdate = z;
            if (oldAnchor == null || oldAnchor.get() != view || (needsUpdate && !this.mIsDropdown)) {
                attachToAnchor(view, i, i2, gravity);
            } else if (needsUpdate) {
                this.mAnchorXoff = i;
                this.mAnchorYoff = i2;
            }
            LayoutParams p = getDecorViewLayoutParams();
            int oldGravity = p.gravity;
            int oldWidth = p.width;
            int oldHeight = p.height;
            int oldX = p.x;
            int oldY = p.y;
            if (width < 0) {
                width2 = this.mWidth;
            } else {
                width2 = width;
            }
            if (height < 0) {
                height2 = this.mHeight;
            } else {
                height2 = height;
            }
            int oldY2 = oldY;
            int oldX2 = oldX;
            i = oldHeight;
            i2 = oldWidth;
            int oldGravity2 = oldGravity;
            oldGravity = gravity;
            LayoutParams p2 = p;
            updateAboveAnchor(findDropDownPosition(view, p, this.mAnchorXoff, this.mAnchorYoff, width2, height2, oldGravity, this.mAllowScrollingAnchorParent));
            boolean paramsChanged = (oldGravity2 == p2.gravity && oldX2 == p2.x && oldY2 == p2.y && i2 == p2.width && i == p2.height) ? false : true;
            update(p2.x, p2.y, width2 < 0 ? width2 : p2.width, height2 >= false ? height2 : p2.height, paramsChanged);
        }
    }

    protected void detachFromAnchor() {
        View anchor = getAnchor();
        if (anchor != null) {
            anchor.getViewTreeObserver().removeOnScrollChangedListener(this.mOnScrollChangedListener);
            anchor.removeOnAttachStateChangeListener(this.mOnAnchorDetachedListener);
        }
        View anchorRoot = this.mAnchorRoot != null ? (View) this.mAnchorRoot.get() : null;
        if (anchorRoot != null) {
            anchorRoot.removeOnAttachStateChangeListener(this.mOnAnchorRootDetachedListener);
            anchorRoot.removeOnLayoutChangeListener(this.mOnLayoutChangeListener);
        }
        this.mAnchor = null;
        this.mAnchorRoot = null;
        this.mIsAnchorRootAttached = false;
    }

    protected void attachToAnchor(View anchor, int xoff, int yoff, int gravity) {
        detachFromAnchor();
        ViewTreeObserver vto = anchor.getViewTreeObserver();
        if (vto != null) {
            vto.addOnScrollChangedListener(this.mOnScrollChangedListener);
        }
        anchor.addOnAttachStateChangeListener(this.mOnAnchorDetachedListener);
        View anchorRoot = anchor.getRootView();
        anchorRoot.addOnAttachStateChangeListener(this.mOnAnchorRootDetachedListener);
        anchorRoot.addOnLayoutChangeListener(this.mOnLayoutChangeListener);
        this.mAnchor = new WeakReference(anchor);
        this.mAnchorRoot = new WeakReference(anchorRoot);
        this.mIsAnchorRootAttached = anchorRoot.isAttachedToWindow();
        this.mParentRootView = this.mAnchorRoot;
        this.mAnchorXoff = xoff;
        this.mAnchorYoff = yoff;
        this.mAnchoredGravity = gravity;
    }

    protected View getAnchor() {
        return this.mAnchor != null ? (View) this.mAnchor.get() : null;
    }

    private void alignToAnchor() {
        View anchor = this.mAnchor != null ? (View) this.mAnchor.get() : null;
        if (anchor != null && anchor.isAttachedToWindow() && hasDecorView()) {
            LayoutParams p = getDecorViewLayoutParams();
            updateAboveAnchor(findDropDownPosition(anchor, p, this.mAnchorXoff, this.mAnchorYoff, p.width, p.height, this.mAnchoredGravity, false));
            update(p.x, p.y, -1, -1, true);
        }
    }

    private View getAppRootView(View anchor) {
        View appWindowView = WindowManagerGlobal.getInstance().getWindowView(anchor.getApplicationWindowToken());
        if (appWindowView != null) {
            return appWindowView;
        }
        return anchor.getRootView();
    }

    public void setTitle(CharSequence title) {
        this.mTitle = title;
    }
}
