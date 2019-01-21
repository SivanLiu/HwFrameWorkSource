package com.android.internal.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManagerGlobal;
import android.text.TextUtils;
import android.util.Size;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.ViewTreeObserver.OnComputeInternalInsetsListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import com.android.internal.util.Preconditions;
import com.huawei.pgmng.PGAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class FloatingToolbar {
    public static final String FLOATING_TOOLBAR_TAG = "floating_toolbar";
    private static final String HW_FLOATING_TOOLBAR = "HwFloatingToolbar";
    private static final OnMenuItemClickListener NO_OP_MENUITEM_CLICK_LISTENER = -$$Lambda$FloatingToolbar$7-enOzxeypZYfdFYr1HzBLfj47k.INSTANCE;
    private static final String TAG = "FloatingToolbar";
    private static final boolean[] mHwFloatingToolbarFlag = new boolean[1];
    private static int mHwFloatingToolbarHeightBias;
    private final Rect mContentRect;
    private final Context mContext;
    private Menu mMenu;
    private OnMenuItemClickListener mMenuItemClickListener;
    private final Comparator<MenuItem> mMenuItemComparator;
    private final OnLayoutChangeListener mOrientationChangeHandler;
    private final FloatingToolbarPopup mPopup;
    private final Rect mPreviousContentRect;
    private List<MenuItem> mShowingMenuItems;
    private int mSuggestedWidth;
    private boolean mWidthChanged;
    private final Window mWindow;

    private static final class FloatingToolbarPopup {
        private static final int MAX_OVERFLOW_SIZE = 4;
        private static final int MIN_OVERFLOW_SIZE = 2;
        private final Drawable mArrow;
        private final AnimationSet mCloseOverflowAnimation;
        private final ViewGroup mContentContainer;
        private final Context mContext;
        private final Point mCoordsOnWindow = new Point();
        private final AnimatorSet mDismissAnimation;
        private boolean mDismissed = true;
        private final Interpolator mFastOutLinearInInterpolator;
        private final Interpolator mFastOutSlowInInterpolator;
        private boolean mHidden;
        private final AnimatorSet mHideAnimation;
        private final int mIconTextSpacing;
        private final OnComputeInternalInsetsListener mInsetsComputer = new -$$Lambda$FloatingToolbar$FloatingToolbarPopup$77YZy6kisO5OnjlgtKp0Zi1V8EY(this);
        private boolean mIsOverflowOpen;
        private final int mLineHeight;
        private final Interpolator mLinearOutSlowInInterpolator;
        private final Interpolator mLogAccelerateInterpolator;
        private final ViewGroup mMainPanel;
        private Size mMainPanelSize;
        private final int mMarginHorizontal;
        private final int mMarginVertical;
        private final OnClickListener mMenuItemButtonOnClickListener = new OnClickListener() {
            public void onClick(View v) {
                if ((v.getTag() instanceof MenuItem) && FloatingToolbarPopup.this.mOnMenuItemClickListener != null) {
                    FloatingToolbarPopup.this.mOnMenuItemClickListener.onMenuItemClick((MenuItem) v.getTag());
                }
            }
        };
        private OnMenuItemClickListener mOnMenuItemClickListener;
        private final AnimationSet mOpenOverflowAnimation;
        private boolean mOpenOverflowUpwards;
        private final Drawable mOverflow;
        private final AnimationListener mOverflowAnimationListener;
        private final ImageButton mOverflowButton;
        private final Size mOverflowButtonSize;
        private final OverflowPanel mOverflowPanel;
        private Size mOverflowPanelSize;
        private final OverflowPanelViewHelper mOverflowPanelViewHelper;
        private final View mParent;
        private final PopupWindow mPopupWindow;
        private final Runnable mPreparePopupContentRTLHelper = new Runnable() {
            public void run() {
                FloatingToolbarPopup.this.setPanelsStatesAtRestingPosition();
                FloatingToolbarPopup.this.setContentAreaAsTouchableSurface();
                FloatingToolbarPopup.this.mContentContainer.setAlpha(1.0f);
            }
        };
        private final AnimatorSet mShowAnimation;
        private final int[] mTmpCoords = new int[2];
        private final AnimatedVectorDrawable mToArrow;
        private final AnimatedVectorDrawable mToOverflow;
        private final Region mTouchableRegion = new Region();
        private int mTransitionDurationScale;
        private final Rect mViewPortOnScreen = new Rect();

        private static final class OverflowPanelViewHelper {
            private final View mCalculator = createMenuButton(null);
            private final Context mContext;
            private final int mIconTextSpacing;
            private final int mSidePadding;

            public OverflowPanelViewHelper(Context context, int iconTextSpacing) {
                this.mContext = (Context) Preconditions.checkNotNull(context);
                this.mIconTextSpacing = iconTextSpacing;
                this.mSidePadding = context.getResources().getDimensionPixelSize(17105059);
            }

            public View getView(MenuItem menuItem, int minimumWidth, View convertView) {
                Preconditions.checkNotNull(menuItem);
                if (convertView != null) {
                    FloatingToolbar.updateMenuItemButton(convertView, menuItem, this.mIconTextSpacing, shouldShowIcon(menuItem));
                } else {
                    convertView = createMenuButton(menuItem);
                }
                convertView.setMinimumWidth(minimumWidth);
                return convertView;
            }

            public int calculateWidth(MenuItem menuItem) {
                FloatingToolbar.updateMenuItemButton(this.mCalculator, menuItem, this.mIconTextSpacing, shouldShowIcon(menuItem));
                this.mCalculator.measure(0, 0);
                return this.mCalculator.getMeasuredWidth();
            }

            private View createMenuButton(MenuItem menuItem) {
                View button = FloatingToolbar.createMenuItemButton(this.mContext, menuItem, this.mIconTextSpacing, shouldShowIcon(menuItem));
                button.setPadding(this.mSidePadding, 0, this.mSidePadding, 0);
                return button;
            }

            private boolean shouldShowIcon(MenuItem menuItem) {
                boolean z = false;
                if (menuItem == null) {
                    return false;
                }
                if (menuItem.getGroupId() == 16908353) {
                    z = true;
                }
                return z;
            }
        }

        private static final class LogAccelerateInterpolator implements Interpolator {
            private static final int BASE = 100;
            private static final float LOGS_SCALE = (1.0f / computeLog(1.0f, 100));

            private LogAccelerateInterpolator() {
            }

            /* synthetic */ LogAccelerateInterpolator(AnonymousClass1 x0) {
                this();
            }

            private static float computeLog(float t, int base) {
                return (float) (1.0d - Math.pow((double) base, (double) (-t)));
            }

            public float getInterpolation(float t) {
                return 1.0f - (computeLog(1.0f - t, 100) * LOGS_SCALE);
            }
        }

        private static final class OverflowPanel extends ListView {
            private final FloatingToolbarPopup mPopup;

            OverflowPanel(FloatingToolbarPopup popup) {
                super(((FloatingToolbarPopup) Preconditions.checkNotNull(popup)).mContext);
                this.mPopup = popup;
                setScrollBarDefaultDelayBeforeFade(ViewConfiguration.getScrollDefaultDelay() * 3);
                setScrollIndicators(3);
            }

            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(this.mPopup.mOverflowPanelSize.getHeight() - this.mPopup.mOverflowButtonSize.getHeight(), 1073741824));
            }

            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (this.mPopup.isOverflowAnimating()) {
                    return true;
                }
                return super.dispatchTouchEvent(ev);
            }

            protected boolean awakenScrollBars() {
                return super.awakenScrollBars();
            }
        }

        public static /* synthetic */ void lambda$new$0(FloatingToolbarPopup floatingToolbarPopup, InternalInsetsInfo info) {
            info.contentInsets.setEmpty();
            info.visibleInsets.setEmpty();
            info.touchableRegion.set(floatingToolbarPopup.mTouchableRegion);
            info.setTouchableInsets(3);
        }

        public FloatingToolbarPopup(Context context, View parent) {
            this.mParent = (View) Preconditions.checkNotNull(parent);
            this.mContext = (Context) Preconditions.checkNotNull(context);
            this.mContentContainer = FloatingToolbar.createContentContainer(context);
            this.mPopupWindow = FloatingToolbar.createPopupWindow(this.mContentContainer);
            this.mMarginHorizontal = parent.getResources().getDimensionPixelSize(17105049);
            this.mMarginVertical = parent.getResources().getDimensionPixelSize(17105062);
            FloatingToolbar.mHwFloatingToolbarHeightBias = FloatingToolbar.getFloatingToolbarHeightBias(this.mContext);
            this.mLineHeight = context.getResources().getDimensionPixelSize(17105048);
            this.mIconTextSpacing = context.getResources().getDimensionPixelSize(17105050);
            this.mLogAccelerateInterpolator = new LogAccelerateInterpolator();
            this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563661);
            this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
            this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563663);
            this.mArrow = this.mContext.getResources().getDrawable(17302236, this.mContext.getTheme());
            this.mArrow.setAutoMirrored(true);
            this.mOverflow = this.mContext.getResources().getDrawable(17302234, this.mContext.getTheme());
            this.mOverflow.setAutoMirrored(true);
            this.mToArrow = (AnimatedVectorDrawable) this.mContext.getResources().getDrawable(17302235, this.mContext.getTheme());
            this.mToArrow.setAutoMirrored(true);
            this.mToOverflow = (AnimatedVectorDrawable) this.mContext.getResources().getDrawable(17302237, this.mContext.getTheme());
            this.mToOverflow.setAutoMirrored(true);
            this.mOverflowButton = createOverflowButton();
            this.mOverflowButtonSize = measure(this.mOverflowButton);
            this.mMainPanel = createMainPanel();
            this.mOverflowPanelViewHelper = new OverflowPanelViewHelper(this.mContext, this.mIconTextSpacing);
            this.mOverflowPanel = createOverflowPanel();
            this.mOverflowAnimationListener = createOverflowAnimationListener();
            this.mOpenOverflowAnimation = new AnimationSet(true);
            this.mOpenOverflowAnimation.setAnimationListener(this.mOverflowAnimationListener);
            this.mCloseOverflowAnimation = new AnimationSet(true);
            this.mCloseOverflowAnimation.setAnimationListener(this.mOverflowAnimationListener);
            this.mShowAnimation = FloatingToolbar.createEnterAnimation(this.mContentContainer);
            this.mDismissAnimation = FloatingToolbar.createExitAnimation(this.mContentContainer, 0, new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    FloatingToolbarPopup.this.mPopupWindow.dismiss();
                    FloatingToolbarPopup.this.mContentContainer.removeAllViews();
                }
            });
            this.mHideAnimation = FloatingToolbar.createExitAnimation(this.mContentContainer, 0, new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    FloatingToolbarPopup.this.mPopupWindow.dismiss();
                }
            });
        }

        public boolean setOutsideTouchable(boolean outsideTouchable, OnDismissListener onDismiss) {
            boolean ret = false;
            if ((this.mPopupWindow.isOutsideTouchable() ^ outsideTouchable) != 0) {
                this.mPopupWindow.setOutsideTouchable(outsideTouchable);
                this.mPopupWindow.setFocusable(outsideTouchable ^ 1);
                ret = true;
            }
            this.mPopupWindow.setOnDismissListener(onDismiss);
            return ret;
        }

        public void layoutMenuItems(List<MenuItem> menuItems, OnMenuItemClickListener menuItemClickListener, int suggestedWidth) {
            this.mOnMenuItemClickListener = menuItemClickListener;
            cancelOverflowAnimations();
            clearPanels();
            menuItems = layoutMainPanelItems(menuItems, getAdjustedToolbarWidth(suggestedWidth));
            if (!menuItems.isEmpty()) {
                layoutOverflowPanelItems(menuItems);
            }
            updatePopupSize();
        }

        public void show(Rect contentRectOnScreen) {
            Preconditions.checkNotNull(contentRectOnScreen);
            if (!isShowing()) {
                this.mHidden = false;
                this.mDismissed = false;
                cancelDismissAndHideAnimations();
                cancelOverflowAnimations();
                refreshCoordinatesAndOverflowDirection(contentRectOnScreen);
                preparePopupContent();
                this.mPopupWindow.showAtLocation(this.mParent, 0, this.mCoordsOnWindow.x, this.mCoordsOnWindow.y);
                setTouchableSurfaceInsetsComputer();
                runShowAnimation();
            }
        }

        public void dismiss() {
            if (!this.mDismissed) {
                this.mHidden = false;
                this.mDismissed = true;
                this.mHideAnimation.cancel();
                runDismissAnimation();
                setZeroTouchableSurface();
            }
        }

        public void hide() {
            if (isShowing()) {
                this.mHidden = true;
                runHideAnimation();
                setZeroTouchableSurface();
            }
        }

        public boolean isShowing() {
            return (this.mDismissed || this.mHidden) ? false : true;
        }

        public boolean isHidden() {
            return this.mHidden;
        }

        public void updateCoordinates(Rect contentRectOnScreen) {
            Preconditions.checkNotNull(contentRectOnScreen);
            if (isShowing() && this.mPopupWindow.isShowing()) {
                cancelOverflowAnimations();
                refreshCoordinatesAndOverflowDirection(contentRectOnScreen);
                preparePopupContent();
                this.mPopupWindow.update(this.mCoordsOnWindow.x, this.mCoordsOnWindow.y, this.mPopupWindow.getWidth(), this.mPopupWindow.getHeight());
            }
        }

        private void refreshCoordinatesAndOverflowDirection(Rect contentRectOnScreen) {
            int minimumOverflowHeightWithMargin;
            int availableHeightThroughContentDown;
            int availableHeightThroughContentUp;
            int y;
            Rect rect = contentRectOnScreen;
            refreshViewPort();
            int x = Math.min(contentRectOnScreen.centerX() - (this.mPopupWindow.getWidth() / 2), this.mViewPortOnScreen.right - this.mPopupWindow.getWidth());
            int statusBarHeight = this.mContext.getResources().getDimensionPixelSize(17105318);
            if (this.mViewPortOnScreen.top < statusBarHeight) {
                this.mViewPortOnScreen.top = statusBarHeight;
            }
            int availableHeightAboveContent = rect.top - this.mViewPortOnScreen.top;
            int availableHeightBelowContent = this.mViewPortOnScreen.bottom - rect.bottom;
            int margin = this.mMarginVertical * 2;
            int toolbarHeightWithVerticalMargin = this.mLineHeight + margin;
            if (hasOverflow()) {
                minimumOverflowHeightWithMargin = calculateOverflowHeight(2) + margin;
                availableHeightThroughContentDown = (this.mViewPortOnScreen.bottom - rect.top) + toolbarHeightWithVerticalMargin;
                availableHeightThroughContentUp = (rect.bottom - this.mViewPortOnScreen.top) + toolbarHeightWithVerticalMargin;
                if (availableHeightAboveContent >= minimumOverflowHeightWithMargin) {
                    updateOverflowHeight(availableHeightAboveContent - margin);
                    y = rect.top - this.mPopupWindow.getHeight();
                    this.mOpenOverflowUpwards = true;
                } else if (availableHeightAboveContent >= toolbarHeightWithVerticalMargin && availableHeightThroughContentDown >= minimumOverflowHeightWithMargin) {
                    updateOverflowHeight(availableHeightThroughContentDown - margin);
                    y = rect.top - toolbarHeightWithVerticalMargin;
                    this.mOpenOverflowUpwards = false;
                } else if (availableHeightBelowContent >= minimumOverflowHeightWithMargin) {
                    updateOverflowHeight(availableHeightBelowContent - margin);
                    y = rect.bottom;
                    this.mOpenOverflowUpwards = false;
                } else if (availableHeightBelowContent < toolbarHeightWithVerticalMargin || this.mViewPortOnScreen.height() < minimumOverflowHeightWithMargin) {
                    updateOverflowHeight(this.mViewPortOnScreen.height() - margin);
                    y = this.mViewPortOnScreen.top;
                    this.mOpenOverflowUpwards = false;
                } else {
                    updateOverflowHeight(availableHeightThroughContentUp - margin);
                    y = (rect.bottom + toolbarHeightWithVerticalMargin) - this.mPopupWindow.getHeight();
                    this.mOpenOverflowUpwards = true;
                }
            } else {
                if (availableHeightAboveContent >= toolbarHeightWithVerticalMargin) {
                    minimumOverflowHeightWithMargin = rect.top - toolbarHeightWithVerticalMargin;
                } else if (availableHeightBelowContent >= toolbarHeightWithVerticalMargin) {
                    minimumOverflowHeightWithMargin = rect.bottom;
                } else if (availableHeightBelowContent >= this.mLineHeight) {
                    minimumOverflowHeightWithMargin = rect.bottom - this.mMarginVertical;
                } else {
                    minimumOverflowHeightWithMargin = Math.max(this.mViewPortOnScreen.top, rect.top - toolbarHeightWithVerticalMargin);
                }
                y = minimumOverflowHeightWithMargin;
            }
            this.mParent.getRootView().getLocationOnScreen(this.mTmpCoords);
            minimumOverflowHeightWithMargin = this.mTmpCoords[0];
            availableHeightThroughContentDown = this.mTmpCoords[1];
            this.mParent.getRootView().getLocationInWindow(this.mTmpCoords);
            availableHeightThroughContentUp = this.mTmpCoords[0];
            this.mCoordsOnWindow.set(Math.max(0, x - (minimumOverflowHeightWithMargin - availableHeightThroughContentUp)), y - (availableHeightThroughContentDown - this.mTmpCoords[1]));
        }

        private void runShowAnimation() {
            this.mShowAnimation.start();
        }

        private void runDismissAnimation() {
            this.mDismissAnimation.start();
        }

        private void runHideAnimation() {
            this.mHideAnimation.start();
        }

        private void cancelDismissAndHideAnimations() {
            this.mDismissAnimation.cancel();
            this.mHideAnimation.cancel();
        }

        private void cancelOverflowAnimations() {
            this.mContentContainer.clearAnimation();
            this.mMainPanel.animate().cancel();
            this.mOverflowPanel.animate().cancel();
            this.mToArrow.stop();
            this.mToOverflow.stop();
        }

        private void openOverflow() {
            float overflowButtonTargetX;
            int targetWidth = this.mOverflowPanelSize.getWidth();
            final int targetHeight = this.mOverflowPanelSize.getHeight() + FloatingToolbar.mHwFloatingToolbarHeightBias;
            final int startWidth = this.mContentContainer.getWidth();
            final int startHeight = this.mContentContainer.getHeight();
            final float startY = this.mContentContainer.getY();
            float left = this.mContentContainer.getX();
            final int i = targetWidth;
            final int i2 = startWidth;
            final float f = left;
            final float width = left + ((float) this.mContentContainer.getWidth());
            Animation widthAnimation = new Animation() {
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    FloatingToolbarPopup.setWidth(FloatingToolbarPopup.this.mContentContainer, i2 + ((int) (((float) (i - i2)) * interpolatedTime)));
                    if (FloatingToolbarPopup.this.isInRTLMode()) {
                        FloatingToolbarPopup.this.mContentContainer.setX(f);
                        FloatingToolbarPopup.this.mMainPanel.setX(0.0f);
                        FloatingToolbarPopup.this.mOverflowPanel.setX(0.0f);
                        return;
                    }
                    FloatingToolbarPopup.this.mContentContainer.setX(width - ((float) FloatingToolbarPopup.this.mContentContainer.getWidth()));
                    FloatingToolbarPopup.this.mMainPanel.setX((float) (FloatingToolbarPopup.this.mContentContainer.getWidth() - i2));
                    FloatingToolbarPopup.this.mOverflowPanel.setX((float) (FloatingToolbarPopup.this.mContentContainer.getWidth() - i));
                }
            };
            Animation heightAnimation = new Animation() {
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    FloatingToolbarPopup.setHeight(FloatingToolbarPopup.this.mContentContainer, startHeight + ((int) (((float) (targetHeight - startHeight)) * interpolatedTime)));
                    if (FloatingToolbarPopup.this.mOpenOverflowUpwards) {
                        FloatingToolbarPopup.this.mContentContainer.setY(startY - ((float) (FloatingToolbarPopup.this.mContentContainer.getHeight() - startHeight)));
                        FloatingToolbarPopup.this.positionContentYCoordinatesIfOpeningOverflowUpwards();
                    }
                }
            };
            final float overflowButtonStartX = this.mOverflowButton.getX();
            if (isInRTLMode()) {
                overflowButtonTargetX = (((float) targetWidth) + overflowButtonStartX) - ((float) this.mOverflowButton.getWidth());
            } else {
                overflowButtonTargetX = (overflowButtonStartX - ((float) targetWidth)) + ((float) this.mOverflowButton.getWidth());
            }
            Animation overflowButtonAnimation = new Animation() {
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    float deltaContainerWidth;
                    float overflowButtonX = overflowButtonStartX + ((overflowButtonTargetX - overflowButtonStartX) * interpolatedTime);
                    if (FloatingToolbarPopup.this.isInRTLMode()) {
                        deltaContainerWidth = 0.0f;
                    } else {
                        deltaContainerWidth = (float) (FloatingToolbarPopup.this.mContentContainer.getWidth() - startWidth);
                    }
                    FloatingToolbarPopup.this.mOverflowButton.setX(overflowButtonX + deltaContainerWidth);
                }
            };
            widthAnimation.setInterpolator(this.mLogAccelerateInterpolator);
            widthAnimation.setDuration((long) getAdjustedDuration(250));
            heightAnimation.setInterpolator(this.mFastOutSlowInInterpolator);
            heightAnimation.setDuration((long) getAdjustedDuration(250));
            overflowButtonAnimation.setInterpolator(this.mFastOutSlowInInterpolator);
            overflowButtonAnimation.setDuration((long) getAdjustedDuration(250));
            this.mOpenOverflowAnimation.getAnimations().clear();
            this.mOpenOverflowAnimation.getAnimations().clear();
            this.mOpenOverflowAnimation.addAnimation(widthAnimation);
            this.mOpenOverflowAnimation.addAnimation(heightAnimation);
            this.mOpenOverflowAnimation.addAnimation(overflowButtonAnimation);
            this.mContentContainer.startAnimation(this.mOpenOverflowAnimation);
            this.mIsOverflowOpen = true;
            this.mMainPanel.animate().alpha(0.0f).withLayer().setInterpolator(this.mLinearOutSlowInInterpolator).setDuration(250).start();
            this.mOverflowPanel.setAlpha(1.0f);
        }

        private void closeOverflow() {
            float overflowButtonTargetX;
            int targetWidth = this.mMainPanelSize.getWidth();
            final int startWidth = this.mContentContainer.getWidth();
            float left = this.mContentContainer.getX();
            final int i = targetWidth;
            final int i2 = startWidth;
            final float f = left;
            final float width = left + ((float) this.mContentContainer.getWidth());
            Animation widthAnimation = new Animation() {
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    FloatingToolbarPopup.setWidth(FloatingToolbarPopup.this.mContentContainer, i2 + ((int) (((float) (i - i2)) * interpolatedTime)));
                    if (FloatingToolbarPopup.this.isInRTLMode()) {
                        FloatingToolbarPopup.this.mContentContainer.setX(f);
                        FloatingToolbarPopup.this.mMainPanel.setX(0.0f);
                        FloatingToolbarPopup.this.mOverflowPanel.setX(0.0f);
                        return;
                    }
                    FloatingToolbarPopup.this.mContentContainer.setX(width - ((float) FloatingToolbarPopup.this.mContentContainer.getWidth()));
                    FloatingToolbarPopup.this.mMainPanel.setX((float) (FloatingToolbarPopup.this.mContentContainer.getWidth() - i));
                    FloatingToolbarPopup.this.mOverflowPanel.setX((float) (FloatingToolbarPopup.this.mContentContainer.getWidth() - i2));
                }
            };
            final int targetHeight = this.mMainPanelSize.getHeight() + FloatingToolbar.mHwFloatingToolbarHeightBias;
            i = this.mContentContainer.getHeight();
            final float bottom = this.mContentContainer.getY() + ((float) this.mContentContainer.getHeight());
            Animation heightAnimation = new Animation() {
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    FloatingToolbarPopup.setHeight(FloatingToolbarPopup.this.mContentContainer, i + ((int) (((float) (targetHeight - i)) * interpolatedTime)));
                    if (FloatingToolbarPopup.this.mOpenOverflowUpwards) {
                        FloatingToolbarPopup.this.mContentContainer.setY(bottom - ((float) FloatingToolbarPopup.this.mContentContainer.getHeight()));
                        FloatingToolbarPopup.this.positionContentYCoordinatesIfOpeningOverflowUpwards();
                    }
                }
            };
            width = this.mOverflowButton.getX();
            if (isInRTLMode()) {
                overflowButtonTargetX = (width - ((float) startWidth)) + ((float) this.mOverflowButton.getWidth());
            } else {
                overflowButtonTargetX = (((float) startWidth) + width) - ((float) this.mOverflowButton.getWidth());
            }
            Animation overflowButtonAnimation = new Animation() {
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    float deltaContainerWidth;
                    float overflowButtonX = width + ((overflowButtonTargetX - width) * interpolatedTime);
                    if (FloatingToolbarPopup.this.isInRTLMode()) {
                        deltaContainerWidth = 0.0f;
                    } else {
                        deltaContainerWidth = (float) (FloatingToolbarPopup.this.mContentContainer.getWidth() - startWidth);
                    }
                    FloatingToolbarPopup.this.mOverflowButton.setX(overflowButtonX + deltaContainerWidth);
                }
            };
            widthAnimation.setInterpolator(this.mFastOutSlowInInterpolator);
            widthAnimation.setDuration((long) getAdjustedDuration(250));
            heightAnimation.setInterpolator(this.mLogAccelerateInterpolator);
            heightAnimation.setDuration((long) getAdjustedDuration(250));
            overflowButtonAnimation.setInterpolator(this.mFastOutSlowInInterpolator);
            overflowButtonAnimation.setDuration((long) getAdjustedDuration(250));
            this.mCloseOverflowAnimation.getAnimations().clear();
            this.mCloseOverflowAnimation.addAnimation(widthAnimation);
            this.mCloseOverflowAnimation.addAnimation(heightAnimation);
            this.mCloseOverflowAnimation.addAnimation(overflowButtonAnimation);
            this.mContentContainer.startAnimation(this.mCloseOverflowAnimation);
            this.mIsOverflowOpen = false;
            this.mMainPanel.animate().alpha(1.0f).withLayer().setInterpolator(this.mFastOutLinearInInterpolator).setDuration(100).start();
            this.mOverflowPanel.animate().alpha(0.0f).withLayer().setInterpolator(this.mLinearOutSlowInInterpolator).setDuration(150).start();
        }

        private void setPanelsStatesAtRestingPosition() {
            this.mOverflowButton.setEnabled(true);
            this.mOverflowPanel.awakenScrollBars();
            Size containerSize;
            if (this.mIsOverflowOpen) {
                containerSize = new Size(this.mOverflowPanelSize.getWidth(), this.mOverflowPanelSize.getHeight() + FloatingToolbar.mHwFloatingToolbarHeightBias);
                setSize(this.mContentContainer, containerSize);
                this.mMainPanel.setAlpha(0.0f);
                this.mMainPanel.setVisibility(4);
                this.mOverflowPanel.setAlpha(1.0f);
                this.mOverflowPanel.setVisibility(0);
                this.mOverflowButton.setImageDrawable(this.mArrow);
                this.mOverflowButton.setContentDescription(this.mContext.getString(17040086));
                if (isInRTLMode()) {
                    this.mContentContainer.setX((float) this.mMarginHorizontal);
                    this.mMainPanel.setX(0.0f);
                    this.mOverflowButton.setX((float) (containerSize.getWidth() - this.mOverflowButtonSize.getWidth()));
                    this.mOverflowPanel.setX(0.0f);
                } else {
                    this.mContentContainer.setX((float) ((this.mPopupWindow.getWidth() - containerSize.getWidth()) - this.mMarginHorizontal));
                    this.mMainPanel.setX(-this.mContentContainer.getX());
                    this.mOverflowButton.setX(0.0f);
                    this.mOverflowPanel.setX(0.0f);
                }
                if (this.mOpenOverflowUpwards) {
                    this.mContentContainer.setY((float) this.mMarginVertical);
                    this.mMainPanel.setY((float) (containerSize.getHeight() - this.mContentContainer.getHeight()));
                    this.mOverflowButton.setY((float) ((containerSize.getHeight() - this.mOverflowButtonSize.getHeight()) - FloatingToolbar.mHwFloatingToolbarHeightBias));
                    this.mOverflowPanel.setY(0.0f);
                    return;
                }
                this.mContentContainer.setY((float) this.mMarginVertical);
                this.mMainPanel.setY(0.0f);
                this.mOverflowButton.setY(0.0f);
                this.mOverflowPanel.setY((float) this.mOverflowButtonSize.getHeight());
                return;
            }
            containerSize = new Size(this.mMainPanelSize.getWidth(), this.mMainPanelSize.getHeight() + FloatingToolbar.mHwFloatingToolbarHeightBias);
            setSize(this.mContentContainer, containerSize);
            this.mMainPanel.setAlpha(1.0f);
            this.mMainPanel.setVisibility(0);
            this.mOverflowPanel.setAlpha(0.0f);
            this.mOverflowPanel.setVisibility(4);
            this.mOverflowButton.setImageDrawable(this.mOverflow);
            this.mOverflowButton.setContentDescription(this.mContext.getString(17040087));
            if (hasOverflow()) {
                if (isInRTLMode()) {
                    this.mContentContainer.setX((float) this.mMarginHorizontal);
                    this.mMainPanel.setX(0.0f);
                    this.mOverflowButton.setX(0.0f);
                    this.mOverflowPanel.setX(0.0f);
                } else {
                    this.mContentContainer.setX((float) ((this.mPopupWindow.getWidth() - containerSize.getWidth()) - this.mMarginHorizontal));
                    this.mMainPanel.setX(0.0f);
                    this.mOverflowButton.setX((float) (containerSize.getWidth() - this.mOverflowButtonSize.getWidth()));
                    this.mOverflowPanel.setX((float) (containerSize.getWidth() - this.mOverflowPanelSize.getWidth()));
                }
                if (this.mOpenOverflowUpwards) {
                    this.mContentContainer.setY((float) (((this.mMarginVertical + this.mOverflowPanelSize.getHeight()) - containerSize.getHeight()) + FloatingToolbar.mHwFloatingToolbarHeightBias));
                    this.mMainPanel.setY(0.0f);
                    this.mOverflowButton.setY(0.0f);
                    this.mOverflowPanel.setY((float) (containerSize.getHeight() - this.mOverflowPanelSize.getHeight()));
                    return;
                }
                this.mContentContainer.setY((float) this.mMarginVertical);
                this.mMainPanel.setY(0.0f);
                this.mOverflowButton.setY(0.0f);
                this.mOverflowPanel.setY((float) this.mOverflowButtonSize.getHeight());
                return;
            }
            this.mContentContainer.setX((float) this.mMarginHorizontal);
            this.mContentContainer.setY((float) this.mMarginVertical);
            this.mMainPanel.setX(0.0f);
            this.mMainPanel.setY(0.0f);
        }

        private void updateOverflowHeight(int suggestedHeight) {
            if (hasOverflow()) {
                int newHeight = calculateOverflowHeight((suggestedHeight - this.mOverflowButtonSize.getHeight()) / this.mLineHeight);
                if (this.mOverflowPanelSize.getHeight() != newHeight) {
                    this.mOverflowPanelSize = new Size(this.mOverflowPanelSize.getWidth(), newHeight);
                }
                setSize(this.mOverflowPanel, this.mOverflowPanelSize);
                if (this.mIsOverflowOpen) {
                    setSize(this.mContentContainer, this.mOverflowPanelSize);
                    if (this.mOpenOverflowUpwards) {
                        int deltaHeight = this.mOverflowPanelSize.getHeight() - newHeight;
                        this.mContentContainer.setY(this.mContentContainer.getY() + ((float) deltaHeight));
                        this.mOverflowButton.setY(this.mOverflowButton.getY() - ((float) deltaHeight));
                    }
                } else {
                    setSize(this.mContentContainer, this.mMainPanelSize);
                }
                updatePopupSize();
            }
        }

        private void updatePopupSize() {
            int width = 0;
            int height = 0;
            if (this.mMainPanelSize != null) {
                width = Math.max(0, this.mMainPanelSize.getWidth());
                height = Math.max(0, this.mMainPanelSize.getHeight());
            }
            if (this.mOverflowPanelSize != null) {
                width = Math.max(width, this.mOverflowPanelSize.getWidth());
                height = Math.max(height, this.mOverflowPanelSize.getHeight());
            }
            this.mPopupWindow.setWidth((this.mMarginHorizontal * 2) + width);
            this.mPopupWindow.setHeight((this.mMarginVertical * 2) + height);
            maybeComputeTransitionDurationScale();
        }

        private void refreshViewPort() {
            this.mParent.getWindowVisibleDisplayFrame(this.mViewPortOnScreen);
            FloatingToolbar.adjustWindowVisibleDisplayFrame(this.mViewPortOnScreen);
        }

        private int getAdjustedToolbarWidth(int suggestedWidth) {
            int width = suggestedWidth;
            refreshViewPort();
            int maximumWidth = this.mViewPortOnScreen.width() - (2 * this.mParent.getResources().getDimensionPixelSize(17105049));
            if (width <= 0) {
                width = this.mParent.getResources().getDimensionPixelSize(17105060);
            }
            return Math.min(width, maximumWidth);
        }

        private void setZeroTouchableSurface() {
            this.mTouchableRegion.setEmpty();
        }

        private void setContentAreaAsTouchableSurface() {
            int width;
            int height;
            Preconditions.checkNotNull(this.mMainPanelSize);
            if (this.mIsOverflowOpen) {
                Preconditions.checkNotNull(this.mOverflowPanelSize);
                width = this.mOverflowPanelSize.getWidth();
                height = this.mOverflowPanelSize.getHeight();
            } else {
                width = this.mMainPanelSize.getWidth();
                height = this.mMainPanelSize.getHeight();
            }
            this.mTouchableRegion.set((int) this.mContentContainer.getX(), (int) this.mContentContainer.getY(), ((int) this.mContentContainer.getX()) + width, ((int) this.mContentContainer.getY()) + height);
        }

        private void setTouchableSurfaceInsetsComputer() {
            ViewTreeObserver viewTreeObserver = this.mPopupWindow.getContentView().getRootView().getViewTreeObserver();
            viewTreeObserver.removeOnComputeInternalInsetsListener(this.mInsetsComputer);
            viewTreeObserver.addOnComputeInternalInsetsListener(this.mInsetsComputer);
        }

        private boolean isInRTLMode() {
            if (this.mContext.getApplicationInfo().hasRtlSupport() && this.mContext.getResources().getConfiguration().getLayoutDirection() == 1) {
                return true;
            }
            return false;
        }

        private boolean hasOverflow() {
            return this.mOverflowPanelSize != null;
        }

        public List<MenuItem> layoutMainPanelItems(List<MenuItem> menuItems, int toolbarWidth) {
            int i;
            Preconditions.checkNotNull(menuItems);
            int availableWidth = toolbarWidth;
            LinkedList<MenuItem> remainingMenuItems = new LinkedList();
            LinkedList<MenuItem> overflowMenuItems = new LinkedList();
            Iterator it = menuItems.iterator();
            while (true) {
                i = 16908353;
                if (!it.hasNext()) {
                    break;
                }
                MenuItem menuItem = (MenuItem) it.next();
                if (menuItem.getItemId() == 16908353 || !menuItem.requiresOverflow()) {
                    remainingMenuItems.add(menuItem);
                } else {
                    overflowMenuItems.add(menuItem);
                }
            }
            remainingMenuItems.addAll(overflowMenuItems);
            this.mMainPanel.removeAllViews();
            boolean z = false;
            this.mMainPanel.setPaddingRelative(0, 0, 0, 0);
            boolean isFirstItem = true;
            Rect paddingRect = new Rect();
            while (!remainingMenuItems.isEmpty()) {
                MenuItem menuItem2 = (MenuItem) remainingMenuItems.peek();
                if (!isFirstItem && menuItem2.requiresOverflow()) {
                    break;
                }
                boolean canFitNoOverflow = true;
                boolean showIcon = (isFirstItem && menuItem2.getItemId() == i) ? true : z;
                View menuItemButton = FloatingToolbar.createMenuItemButton(this.mContext, menuItem2, this.mIconTextSpacing, showIcon);
                paddingRect.set(menuItemButton.getPaddingStart(), menuItemButton.getPaddingTop(), menuItemButton.getPaddingEnd(), menuItemButton.getPaddingBottom());
                if (!showIcon && (menuItemButton instanceof LinearLayout)) {
                    ((LinearLayout) menuItemButton).setGravity(17);
                }
                if (isFirstItem) {
                    if (remainingMenuItems.size() == 1) {
                        menuItemButton.setBackgroundResource(33751246);
                    } else if (isInRTLMode()) {
                        menuItemButton.setBackgroundResource(33751245);
                    } else {
                        menuItemButton.setBackgroundResource(33751244);
                    }
                } else if (remainingMenuItems.size() != 1) {
                    menuItemButton.setBackgroundResource(33751786);
                } else if (isInRTLMode()) {
                    menuItemButton.setBackgroundResource(33751244);
                } else {
                    menuItemButton.setBackgroundResource(33751245);
                }
                menuItemButton.setPaddingRelative(paddingRect.left, paddingRect.top, paddingRect.right, paddingRect.bottom);
                if (isFirstItem) {
                    menuItemButton.setPaddingRelative((int) (((double) menuItemButton.getPaddingStart()) * 1.5d), menuItemButton.getPaddingTop(), menuItemButton.getPaddingEnd(), menuItemButton.getPaddingBottom());
                }
                z = remainingMenuItems.size() == 1;
                if (z) {
                    menuItemButton.setPaddingRelative(menuItemButton.getPaddingStart(), menuItemButton.getPaddingTop(), (int) (((double) menuItemButton.getPaddingEnd()) * 1.5d), menuItemButton.getPaddingBottom());
                }
                menuItemButton.measure(0, 0);
                i = Math.min(menuItemButton.getMeasuredWidth(), toolbarWidth);
                boolean canFitWithOverflow = i <= availableWidth - this.mOverflowButtonSize.getWidth();
                if (!z || i > availableWidth) {
                    canFitNoOverflow = false;
                }
                if (!canFitWithOverflow && !canFitNoOverflow) {
                    break;
                }
                setButtonTagAndClickListener(menuItemButton, menuItem2);
                menuItemButton.setTooltipText(menuItem2.getTooltipText());
                this.mMainPanel.addView(menuItemButton);
                LayoutParams params = menuItemButton.getLayoutParams();
                params.width = i;
                menuItemButton.setLayoutParams(params);
                availableWidth -= i;
                remainingMenuItems.pop();
                int lastGroupId = menuItem2.getGroupId();
                isFirstItem = false;
                z = false;
                i = 16908353;
            }
            int i2 = toolbarWidth;
            if (!remainingMenuItems.isEmpty()) {
                this.mMainPanel.setPaddingRelative(0, 0, this.mOverflowButtonSize.getWidth(), 0);
            }
            this.mMainPanelSize = measure(this.mMainPanel);
            return remainingMenuItems;
        }

        private void layoutOverflowPanelItems(List<MenuItem> menuItems) {
            ArrayAdapter<MenuItem> overflowPanelAdapter = (ArrayAdapter) this.mOverflowPanel.getAdapter();
            overflowPanelAdapter.clear();
            int size = menuItems.size();
            for (int i = 0; i < size; i++) {
                overflowPanelAdapter.add((MenuItem) menuItems.get(i));
            }
            this.mOverflowPanel.setAdapter(overflowPanelAdapter);
            if (this.mOpenOverflowUpwards) {
                this.mOverflowPanel.setY(0.0f);
            } else {
                this.mOverflowPanel.setY((float) this.mOverflowButtonSize.getHeight());
            }
            this.mOverflowPanelSize = new Size(Math.max(getOverflowWidth(), this.mOverflowButtonSize.getWidth()), calculateOverflowHeight(4));
            setSize(this.mOverflowPanel, this.mOverflowPanelSize);
        }

        private void preparePopupContent() {
            this.mContentContainer.removeAllViews();
            if (hasOverflow()) {
                this.mContentContainer.addView(this.mOverflowPanel);
            }
            this.mContentContainer.addView(this.mMainPanel);
            if (hasOverflow()) {
                this.mContentContainer.addView(this.mOverflowButton);
            }
            setPanelsStatesAtRestingPosition();
            setContentAreaAsTouchableSurface();
            if (isInRTLMode()) {
                this.mContentContainer.setAlpha(0.0f);
                this.mContentContainer.post(this.mPreparePopupContentRTLHelper);
            }
        }

        private void clearPanels() {
            this.mOverflowPanelSize = null;
            this.mMainPanelSize = null;
            this.mIsOverflowOpen = false;
            this.mMainPanel.removeAllViews();
            ArrayAdapter<MenuItem> overflowPanelAdapter = (ArrayAdapter) this.mOverflowPanel.getAdapter();
            overflowPanelAdapter.clear();
            this.mOverflowPanel.setAdapter(overflowPanelAdapter);
            this.mContentContainer.removeAllViews();
        }

        private void positionContentYCoordinatesIfOpeningOverflowUpwards() {
            if (this.mOpenOverflowUpwards) {
                this.mMainPanel.setY((float) ((this.mContentContainer.getHeight() - this.mMainPanelSize.getHeight()) - FloatingToolbar.mHwFloatingToolbarHeightBias));
                this.mOverflowButton.setY((float) ((this.mContentContainer.getHeight() - this.mOverflowButton.getHeight()) - FloatingToolbar.mHwFloatingToolbarHeightBias));
                this.mOverflowPanel.setY((float) ((this.mContentContainer.getHeight() - this.mOverflowPanelSize.getHeight()) - FloatingToolbar.mHwFloatingToolbarHeightBias));
            }
        }

        private int getOverflowWidth() {
            int overflowWidth = 0;
            int count = this.mOverflowPanel.getAdapter().getCount();
            for (int i = 0; i < count; i++) {
                overflowWidth = Math.max(this.mOverflowPanelViewHelper.calculateWidth((MenuItem) this.mOverflowPanel.getAdapter().getItem(i)), overflowWidth);
            }
            return overflowWidth;
        }

        private int calculateOverflowHeight(int maxItemSize) {
            int actualSize = Math.min(4, Math.min(Math.max(2, maxItemSize), this.mOverflowPanel.getCount()));
            int extension = 0;
            if (actualSize < this.mOverflowPanel.getCount()) {
                extension = (int) (((float) this.mLineHeight) * 0.5f);
            }
            return ((this.mLineHeight * actualSize) + this.mOverflowButtonSize.getHeight()) + extension;
        }

        private void setButtonTagAndClickListener(View menuItemButton, MenuItem menuItem) {
            menuItemButton.setTag(menuItem);
            menuItemButton.setOnClickListener(this.mMenuItemButtonOnClickListener);
        }

        private int getAdjustedDuration(int originalDuration) {
            if (this.mTransitionDurationScale < 150) {
                return Math.max(originalDuration - 50, 0);
            }
            if (this.mTransitionDurationScale > 300) {
                return originalDuration + 50;
            }
            return (int) (((float) originalDuration) * ValueAnimator.getDurationScale());
        }

        private void maybeComputeTransitionDurationScale() {
            if (this.mMainPanelSize != null && this.mOverflowPanelSize != null) {
                int w = this.mMainPanelSize.getWidth() - this.mOverflowPanelSize.getWidth();
                int h = this.mOverflowPanelSize.getHeight() - this.mMainPanelSize.getHeight();
                this.mTransitionDurationScale = (int) (Math.sqrt((double) ((w * w) + (h * h))) / ((double) this.mContentContainer.getContext().getResources().getDisplayMetrics().density));
            }
        }

        private ViewGroup createMainPanel() {
            return new LinearLayout(this.mContext) {
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    if (FloatingToolbarPopup.this.isOverflowAnimating()) {
                        widthMeasureSpec = MeasureSpec.makeMeasureSpec(FloatingToolbarPopup.this.mMainPanelSize.getWidth(), 1073741824);
                    }
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }

                public boolean onInterceptTouchEvent(MotionEvent ev) {
                    return FloatingToolbarPopup.this.isOverflowAnimating();
                }
            };
        }

        private ImageButton createOverflowButton() {
            ImageButton overflowButton = (ImageButton) LayoutInflater.from(this.mContext).inflate(17367143, null);
            overflowButton.setImageDrawable(this.mOverflow);
            overflowButton.setOnClickListener(new -$$Lambda$FloatingToolbar$FloatingToolbarPopup$-uEfRwR-_1oHxMvRVdmbNRdukDM(this, overflowButton));
            return overflowButton;
        }

        public static /* synthetic */ void lambda$createOverflowButton$1(FloatingToolbarPopup floatingToolbarPopup, ImageButton overflowButton, View v) {
            if (floatingToolbarPopup.mIsOverflowOpen) {
                overflowButton.setImageDrawable(floatingToolbarPopup.mToOverflow);
                floatingToolbarPopup.mToOverflow.start();
                floatingToolbarPopup.closeOverflow();
                return;
            }
            overflowButton.setImageDrawable(floatingToolbarPopup.mToArrow);
            floatingToolbarPopup.mToArrow.start();
            floatingToolbarPopup.openOverflow();
        }

        private OverflowPanel createOverflowPanel() {
            OverflowPanel overflowPanel = new OverflowPanel(this);
            overflowPanel.setLayoutParams(new LayoutParams(-1, -1));
            overflowPanel.setDivider(null);
            overflowPanel.setDividerHeight(0);
            overflowPanel.setAdapter(new ArrayAdapter<MenuItem>(this.mContext, 0) {
                public View getView(int position, View convertView, ViewGroup parent) {
                    View itemView = FloatingToolbarPopup.this.mOverflowPanelViewHelper.getView((MenuItem) getItem(position), FloatingToolbarPopup.this.mOverflowPanelSize.getWidth(), convertView);
                    Rect paddingRect = new Rect();
                    paddingRect.set(itemView.getPaddingStart(), itemView.getPaddingTop(), itemView.getPaddingEnd(), itemView.getPaddingBottom());
                    if (FloatingToolbarPopup.this.mOpenOverflowUpwards) {
                        if (position == 0) {
                            itemView.setBackgroundResource(33751787);
                        } else {
                            itemView.setBackgroundResource(33751788);
                        }
                    } else if (position == getCount() - 1) {
                        itemView.setBackgroundResource(33751789);
                    } else {
                        itemView.setBackgroundResource(33751788);
                    }
                    itemView.setPaddingRelative(paddingRect.left, paddingRect.top, paddingRect.right, paddingRect.bottom);
                    return itemView;
                }
            });
            overflowPanel.setOnItemClickListener(new -$$Lambda$FloatingToolbar$FloatingToolbarPopup$E8FwnPCl7gZpcTlX_UaRPIBRnT0(this, overflowPanel));
            return overflowPanel;
        }

        public static /* synthetic */ void lambda$createOverflowPanel$2(FloatingToolbarPopup floatingToolbarPopup, OverflowPanel overflowPanel, AdapterView parent, View view, int position, long id) {
            MenuItem menuItem = (MenuItem) overflowPanel.getAdapter().getItem(position);
            if (floatingToolbarPopup.mOnMenuItemClickListener != null) {
                floatingToolbarPopup.mOnMenuItemClickListener.onMenuItemClick(menuItem);
            }
        }

        private boolean isOverflowAnimating() {
            boolean overflowOpening = this.mOpenOverflowAnimation.hasStarted() && !this.mOpenOverflowAnimation.hasEnded();
            boolean overflowClosing = this.mCloseOverflowAnimation.hasStarted() && !this.mCloseOverflowAnimation.hasEnded();
            if (overflowOpening || overflowClosing) {
                return true;
            }
            return false;
        }

        private AnimationListener createOverflowAnimationListener() {
            return new AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    FloatingToolbarPopup.this.mOverflowButton.setEnabled(false);
                    FloatingToolbarPopup.this.mMainPanel.setVisibility(0);
                    FloatingToolbarPopup.this.mOverflowPanel.setVisibility(0);
                }

                public void onAnimationEnd(Animation animation) {
                    FloatingToolbarPopup.this.mContentContainer.post(new -$$Lambda$FloatingToolbar$FloatingToolbarPopup$13$7WTSUuAWkzil48e0QxuKTn0YOXI(this));
                }

                public static /* synthetic */ void lambda$onAnimationEnd$0(AnonymousClass13 anonymousClass13) {
                    FloatingToolbarPopup.this.setPanelsStatesAtRestingPosition();
                    FloatingToolbarPopup.this.setContentAreaAsTouchableSurface();
                }

                public void onAnimationRepeat(Animation animation) {
                }
            };
        }

        private static Size measure(View view) {
            Preconditions.checkState(view.getParent() == null);
            view.measure(0, 0);
            return new Size(view.getMeasuredWidth(), view.getMeasuredHeight());
        }

        private static void setSize(View view, int width, int height) {
            view.setMinimumWidth(width);
            view.setMinimumHeight(height);
            LayoutParams params = view.getLayoutParams();
            params = params == null ? new LayoutParams(0, 0) : params;
            params.width = width;
            params.height = height;
            view.setLayoutParams(params);
        }

        private static void setSize(View view, Size size) {
            setSize(view, size.getWidth(), size.getHeight());
        }

        private static void setWidth(View view, int width) {
            setSize(view, width, view.getLayoutParams().height);
        }

        private static void setHeight(View view, int height) {
            setSize(view, view.getLayoutParams().width, height);
        }
    }

    static /* synthetic */ int lambda$new$1(MenuItem menuItem1, MenuItem menuItem2) {
        int i = 0;
        if (menuItem1.getItemId() == 16908353) {
            if (menuItem2.getItemId() != 16908353) {
                i = -1;
            }
            return i;
        } else if (menuItem2.getItemId() == 16908353) {
            return 1;
        } else {
            if (menuItem1.getItemId() == 16908319) {
                return -1;
            }
            if (menuItem2.getItemId() == 16908319) {
                return 1;
            }
            if (menuItem1.requiresActionButton()) {
                if (!menuItem2.requiresActionButton()) {
                    i = -1;
                }
                return i;
            } else if (menuItem2.requiresActionButton()) {
                return 1;
            } else {
                if (menuItem1.requiresOverflow()) {
                    return menuItem2.requiresOverflow() ^ 1;
                }
                if (menuItem2.requiresOverflow()) {
                    return -1;
                }
                return menuItem1.getOrder() - menuItem2.getOrder();
            }
        }
    }

    public FloatingToolbar(Window window) {
        this(window, false);
    }

    public FloatingToolbar(Window window, boolean flag) {
        this.mContentRect = new Rect();
        this.mPreviousContentRect = new Rect();
        this.mShowingMenuItems = new ArrayList();
        this.mMenuItemClickListener = NO_OP_MENUITEM_CLICK_LISTENER;
        this.mWidthChanged = true;
        this.mOrientationChangeHandler = new OnLayoutChangeListener() {
            private final Rect mNewRect = new Rect();
            private final Rect mOldRect = new Rect();

            public void onLayoutChange(View view, int newLeft, int newRight, int newTop, int newBottom, int oldLeft, int oldRight, int oldTop, int oldBottom) {
                this.mNewRect.set(newLeft, newRight, newTop, newBottom);
                this.mOldRect.set(oldLeft, oldRight, oldTop, oldBottom);
                if (FloatingToolbar.this.mPopup.isShowing() && !this.mNewRect.equals(this.mOldRect)) {
                    FloatingToolbar.this.mWidthChanged = true;
                    FloatingToolbar.this.updateLayout();
                }
            }
        };
        this.mMenuItemComparator = -$$Lambda$FloatingToolbar$LutnsyBKrZiroTBekgIjhIyrl40.INSTANCE;
        mHwFloatingToolbarFlag[0] = flag;
        this.mContext = applyDefaultTheme(window.getContext());
        this.mWindow = (Window) Preconditions.checkNotNull(window);
        this.mPopup = new FloatingToolbarPopup(this.mContext, window.getDecorView());
    }

    public FloatingToolbar setMenu(Menu menu) {
        this.mMenu = (Menu) Preconditions.checkNotNull(menu);
        return this;
    }

    public FloatingToolbar setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
        if (menuItemClickListener != null) {
            this.mMenuItemClickListener = menuItemClickListener;
        } else {
            this.mMenuItemClickListener = NO_OP_MENUITEM_CLICK_LISTENER;
        }
        return this;
    }

    public FloatingToolbar setContentRect(Rect rect) {
        this.mContentRect.set((Rect) Preconditions.checkNotNull(rect));
        return this;
    }

    public FloatingToolbar setSuggestedWidth(int suggestedWidth) {
        this.mWidthChanged = ((double) Math.abs(suggestedWidth - this.mSuggestedWidth)) > ((double) this.mSuggestedWidth) * 0.2d;
        this.mSuggestedWidth = suggestedWidth;
        return this;
    }

    public FloatingToolbar show() {
        registerOrientationHandler();
        doShow();
        return this;
    }

    public FloatingToolbar updateLayout() {
        if (this.mPopup.isShowing()) {
            doShow();
        }
        return this;
    }

    public void dismiss() {
        unregisterOrientationHandler();
        this.mPopup.dismiss();
    }

    public void hide() {
        this.mPopup.hide();
    }

    public boolean isShowing() {
        return this.mPopup.isShowing();
    }

    public boolean isHidden() {
        return this.mPopup.isHidden();
    }

    public void setOutsideTouchable(boolean outsideTouchable, OnDismissListener onDismiss) {
        if (this.mPopup.setOutsideTouchable(outsideTouchable, onDismiss) && isShowing()) {
            dismiss();
            doShow();
        }
    }

    private void doShow() {
        List<MenuItem> menuItems = getVisibleAndEnabledMenuItems(this.mMenu);
        menuItems.sort(this.mMenuItemComparator);
        if (!isCurrentlyShowing(menuItems) || this.mWidthChanged) {
            this.mPopup.dismiss();
            this.mPopup.layoutMenuItems(menuItems, this.mMenuItemClickListener, this.mSuggestedWidth);
            this.mShowingMenuItems = menuItems;
        }
        if (!this.mPopup.isShowing()) {
            this.mPopup.show(this.mContentRect);
        } else if (!this.mPreviousContentRect.equals(this.mContentRect)) {
            this.mPopup.updateCoordinates(this.mContentRect);
        }
        this.mWidthChanged = false;
        this.mPreviousContentRect.set(this.mContentRect);
    }

    private boolean isCurrentlyShowing(List<MenuItem> menuItems) {
        if (this.mShowingMenuItems == null || menuItems.size() != this.mShowingMenuItems.size()) {
            return false;
        }
        int size = menuItems.size();
        for (int i = 0; i < size; i++) {
            MenuItem menuItem = (MenuItem) menuItems.get(i);
            MenuItem showingItem = (MenuItem) this.mShowingMenuItems.get(i);
            if (menuItem.getItemId() != showingItem.getItemId() || !TextUtils.equals(menuItem.getTitle(), showingItem.getTitle()) || !Objects.equals(menuItem.getIcon(), showingItem.getIcon()) || menuItem.getGroupId() != showingItem.getGroupId()) {
                return false;
            }
        }
        return true;
    }

    private List<MenuItem> getVisibleAndEnabledMenuItems(Menu menu) {
        List<MenuItem> menuItems = new ArrayList();
        int i = 0;
        while (menu != null && i < menu.size()) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.isVisible() && menuItem.isEnabled()) {
                Menu subMenu = menuItem.getSubMenu();
                if (subMenu != null) {
                    menuItems.addAll(getVisibleAndEnabledMenuItems(subMenu));
                } else {
                    menuItems.add(menuItem);
                }
            }
            i++;
        }
        return menuItems;
    }

    private void registerOrientationHandler() {
        unregisterOrientationHandler();
        this.mWindow.getDecorView().addOnLayoutChangeListener(this.mOrientationChangeHandler);
    }

    private void unregisterOrientationHandler() {
        this.mWindow.getDecorView().removeOnLayoutChangeListener(this.mOrientationChangeHandler);
    }

    private static View createMenuItemButton(Context context, MenuItem menuItem, int iconTextSpacing, boolean showIcon) {
        View menuItemButton = LayoutInflater.from(context).inflate(17367141, null);
        if (menuItem != null) {
            updateMenuItemButton(menuItemButton, menuItem, iconTextSpacing, showIcon);
        }
        return menuItemButton;
    }

    private static void updateMenuItemButton(View menuItemButton, MenuItem menuItem, int iconTextSpacing, boolean showIcon) {
        TextView buttonText = (TextView) menuItemButton.findViewById(16908931);
        buttonText.setEllipsize(null);
        if (TextUtils.isEmpty(menuItem.getTitle())) {
            buttonText.setVisibility(8);
        } else {
            buttonText.setVisibility(0);
            buttonText.setText(menuItem.getTitle());
            setMenuItemButtonInputType(buttonText);
        }
        ImageView buttonIcon = (ImageView) menuItemButton.findViewById(16908929);
        if (menuItem.getIcon() == null || !showIcon) {
            buttonIcon.setVisibility(8);
            if (buttonText != null) {
                buttonText.setPaddingRelative(0, 0, 0, 0);
            }
        } else {
            buttonIcon.setVisibility(0);
            buttonIcon.setImageDrawable(menuItem.getIcon());
            if (buttonText != null) {
                buttonText.setPaddingRelative(iconTextSpacing, 0, 0, 0);
            }
        }
        CharSequence contentDescription = menuItem.getContentDescription();
        if (TextUtils.isEmpty(contentDescription)) {
            menuItemButton.setContentDescription(menuItem.getTitle());
        } else {
            menuItemButton.setContentDescription(contentDescription);
        }
    }

    private static ViewGroup createContentContainer(Context context) {
        ViewGroup contentContainer = (ViewGroup) LayoutInflater.from(context).inflate(17367140, null);
        if (mHwFloatingToolbarFlag[0]) {
            contentContainer.setElevation(0.0f);
        }
        contentContainer.setLayoutParams(new LayoutParams(-2, -2));
        contentContainer.setTag(FLOATING_TOOLBAR_TAG);
        return contentContainer;
    }

    private static PopupWindow createPopupWindow(ViewGroup content) {
        ViewGroup popupContentHolder = new LinearLayout(content.getContext());
        PopupWindow popupWindow = new PopupWindow(popupContentHolder);
        popupWindow.setClippingEnabled(false);
        popupWindow.setWindowLayoutType(1005);
        popupWindow.setAnimationStyle(0);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));
        popupWindow.setTitle(HW_FLOATING_TOOLBAR);
        content.setLayoutParams(new LayoutParams(-2, -2));
        popupContentHolder.addView(content);
        return popupWindow;
    }

    private static AnimatorSet createEnterAnimation(View view) {
        AnimatorSet animation = new AnimatorSet();
        animation.playTogether(new Animator[]{ObjectAnimator.ofFloat(view, View.ALPHA, new float[]{0.0f, 1.0f}).setDuration(150)});
        return animation;
    }

    private static AnimatorSet createExitAnimation(View view, int startDelay, AnimatorListener listener) {
        AnimatorSet animation = new AnimatorSet();
        animation.playTogether(new Animator[]{ObjectAnimator.ofFloat(view, View.ALPHA, new float[]{1.0f, 0.0f}).setDuration(0)});
        animation.setStartDelay((long) startDelay);
        animation.addListener(listener);
        return animation;
    }

    private static Context applyDefaultTheme(Context originalContext) {
        return new ContextThemeWrapper(originalContext, getThemeId(originalContext));
    }

    protected static void setMenuItemButtonInputType(TextView textview) {
        if (mHwFloatingToolbarFlag[0]) {
            textview.setAllCaps(false);
            textview.setInputType(16384);
        }
    }

    protected static int getThemeId(Context originalContext) {
        if (mHwFloatingToolbarFlag[0]) {
            return originalContext.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null);
        }
        TypedArray a = originalContext.obtainStyledAttributes(new int[]{17891413});
        int themeId = a.getBoolean(0, true) ? 16974123 : 16974120;
        a.recycle();
        return themeId;
    }

    protected static void adjustWindowVisibleDisplayFrame(Rect rect) {
        if (mHwFloatingToolbarFlag[0] && rect.equals(new Rect(-10000, -10000, PGAction.PG_ID_DEFAULT_FRONT, PGAction.PG_ID_DEFAULT_FRONT))) {
            Display d = DisplayManagerGlobal.getInstance().getRealDisplay(0);
            if (d != null) {
                d.getRectSize(rect);
            }
        }
    }

    protected static int getFloatingToolbarHeightBias(Context context) {
        return context.getResources().getDimensionPixelSize(34472163);
    }
}
