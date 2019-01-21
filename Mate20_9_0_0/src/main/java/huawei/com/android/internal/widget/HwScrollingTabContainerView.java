package huawei.com.android.internal.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hwcontrol.HwWidgetFactory;
import android.os.FreezeScreenScene;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.widget.ScrollingTabContainerView;
import com.android.internal.widget.ScrollingTabContainerView.TabView;
import huawei.com.android.internal.app.HwActionBarImpl.HwTabImpl;

public class HwScrollingTabContainerView extends ScrollingTabContainerView {
    private static final float DOUBLE = 2.0f;
    private static final boolean IS_EMUI_LITE = SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);
    private static float SCALESIZE = (IS_EMUI_LITE ? 1.0f : 1.2f);
    private static final float SINGLE = 1.0f;
    private String TAG = "HwSrollingTabContainerView";
    private boolean isWidthPixelsChanged = false;
    private int mAvailableSpace = 0;
    private float mDensity;
    private int mIndicatorHeight;
    private boolean mIsHwDarkTheme;
    private boolean mIsHwEmphasizeTheme;
    private int mLastContainerWidth;
    private int mMinTabTextSize;
    private boolean mRTLFlag;
    private int mRealWidthPixels;
    private Typeface mRegular;
    private Typeface mRegularCondensed;
    private int mScrollDirection;
    private int mScrollerLastPos;
    private boolean mTabChanged = false;
    private int mTabDefaultSize;
    private ImageView mTabIndicator;
    private TabIndicatorAnimation mTabIndicatorAnimation;
    private int mTabLayoutWidth;
    private int mTabMinSize;
    private int mTabStepSize;
    private int mTabTitleSelectedColor;
    private int mWidthPixels = 0;
    private WindowManager mWindowManager = ((WindowManager) getContext().getSystemService(FreezeScreenScene.WINDOW_PARAM));

    class TabIndicatorAnimation {
        private static final float SCALE_DELTA_ERROR = 0.01f;
        private ValueAnimator mAnimator;
        private boolean mIsAnimEnd = true;
        private View mView;
        private int mWidth;

        TabIndicatorAnimation(View view) {
            this.mView = view;
        }

        public void setViewWidth(int width) {
            this.mWidth = width;
        }

        public void startAnim(int from, int to) {
            this.mIsAnimEnd = false;
            if (from == to) {
                this.mIsAnimEnd = true;
                HwScrollingTabContainerView.this.mTabClicked = false;
                return;
            }
            int l2r;
            if (from < to) {
                cancelAnim();
                l2r = 1;
            } else {
                cancelAnim();
                l2r = 0;
            }
            anim(from, to, l2r);
        }

        public void cancelAnim() {
            if (this.mAnimator != null) {
                this.mAnimator.cancel();
            }
        }

        private int getX1() {
            LayoutParams lp = (LayoutParams) this.mView.getLayoutParams();
            if (HwScrollingTabContainerView.this.mRTLFlag) {
                return lp.rightMargin;
            }
            return lp.leftMargin;
        }

        private int getX2() {
            LayoutParams lp = (LayoutParams) this.mView.getLayoutParams();
            if (HwScrollingTabContainerView.this.mRTLFlag) {
                return lp.rightMargin + lp.width;
            }
            return lp.leftMargin + lp.width;
        }

        private float getScaleX() {
            return ((float) ((LayoutParams) this.mView.getLayoutParams()).width) / ((float) this.mWidth);
        }

        private void setLine(int x1, int x2) {
            LayoutParams lp = (LayoutParams) this.mView.getLayoutParams();
            if (HwScrollingTabContainerView.this.mRTLFlag) {
                lp.rightMargin = x1;
            } else {
                lp.leftMargin = x1;
            }
            lp.width = x2 - x1;
            if (lp.width > ((int) (((float) this.mWidth) * HwScrollingTabContainerView.SCALESIZE))) {
                lp.width = (int) (((float) this.mWidth) * HwScrollingTabContainerView.SCALESIZE);
            }
            if (lp.width < this.mWidth) {
                lp.width = this.mWidth;
            }
            this.mView.setLayoutParams(lp);
            this.mView.setMinimumWidth(lp.width);
        }

        private void setTranslationX(int x) {
            LayoutParams lp = (LayoutParams) this.mView.getLayoutParams();
            if (HwScrollingTabContainerView.this.mRTLFlag) {
                lp.rightMargin = x;
            } else {
                lp.leftMargin = x;
            }
            this.mView.setLayoutParams(lp);
        }

        public boolean isAnimEnd() {
            return this.mIsAnimEnd;
        }

        private void anim(int from, int to, int l2r) {
            int totalLength;
            int duration;
            int i = from;
            int i2 = to;
            final int toX1 = i2 * this.mWidth;
            final int toX2 = (i2 + 1) * this.mWidth;
            int x1 = getX1();
            int x2 = getX2();
            if (i < i2) {
                totalLength = (int) (((float) Math.abs(toX2 - x2)) + ((HwScrollingTabContainerView.SCALESIZE - 1.0f) * ((float) this.mWidth)));
            } else {
                totalLength = (int) (((float) Math.abs(toX1 - x1)) + ((HwScrollingTabContainerView.SCALESIZE - 1.0f) * ((float) this.mWidth)));
            }
            int totalLength2 = totalLength;
            if (Math.abs(i2 - i) > 1) {
                duration = 250;
            } else {
                duration = 200;
            }
            int duration2 = duration;
            int stepSize = (int) (((float) totalLength2) / (((float) duration2) / 16.0f));
            this.mAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
            this.mAnimator.setTarget(this.mView);
            this.mAnimator.setDuration((long) duration2).start();
            this.mAnimator.setInterpolator(new PathInterpolator(0.2f, 0.5f, 0.8f, 0.5f));
            final int i3 = toX1;
            AnonymousClass1 anonymousClass1 = r0;
            final int i4 = toX2;
            ValueAnimator valueAnimator = this.mAnimator;
            final int i5 = stepSize;
            stepSize = l2r;
            AnonymousClass1 anonymousClass12 = new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    int x1 = TabIndicatorAnimation.this.getX1();
                    int x2 = TabIndicatorAnimation.this.getX2();
                    float scale = TabIndicatorAnimation.this.getScaleX();
                    if (x1 == i3 && x2 == i4) {
                        TabIndicatorAnimation.this.mAnimator.cancel();
                    }
                    int step = i5;
                    if (stepSize == 1) {
                        if (x2 < i4) {
                            if (scale >= 1.0f && scale < HwScrollingTabContainerView.SCALESIZE - TabIndicatorAnimation.SCALE_DELTA_ERROR) {
                                if (i5 + x2 > i4) {
                                    step = i4 - x2;
                                }
                                TabIndicatorAnimation.this.setLine(x1, x2 + step);
                            } else if (Math.abs(scale - HwScrollingTabContainerView.SCALESIZE) <= TabIndicatorAnimation.SCALE_DELTA_ERROR) {
                                if (i5 + x2 > i4) {
                                    step = i4 - x2;
                                }
                                TabIndicatorAnimation.this.setTranslationX(x1 + step);
                            }
                        } else if (x2 == i4) {
                            if (x1 < i3) {
                                if (i5 + x1 > i3) {
                                    step = i3 - x1;
                                }
                                TabIndicatorAnimation.this.setLine(x1 + step, x2);
                            }
                        } else if (x2 > i4) {
                            if (x2 - i5 < i4) {
                                step = x2 - i4;
                            }
                            TabIndicatorAnimation.this.setLine(x1, x2 - step);
                        }
                    } else if (x1 > i3) {
                        if (scale >= 1.0f && scale < HwScrollingTabContainerView.SCALESIZE - TabIndicatorAnimation.SCALE_DELTA_ERROR) {
                            if (x1 - i5 < i3) {
                                step = x1 - i3;
                            }
                            TabIndicatorAnimation.this.setLine(x1 - step, x2);
                        } else if (Math.abs(scale - HwScrollingTabContainerView.SCALESIZE) <= TabIndicatorAnimation.SCALE_DELTA_ERROR) {
                            if (x1 - i5 < i3) {
                                step = x1 - i3;
                            }
                            TabIndicatorAnimation.this.setTranslationX(x1 - step);
                        } else {
                            int i = (scale > 1.0f ? 1 : (scale == 1.0f ? 0 : -1));
                        }
                    } else if (x1 == i3) {
                        if (x2 > i4) {
                            if (x2 - i5 < i4) {
                                step = x2 - i4;
                            }
                            TabIndicatorAnimation.this.setLine(x1, x2 - step);
                        }
                    } else if (x1 < i3) {
                        if (i5 + x1 > i3) {
                            step = i3 - x1;
                        }
                        TabIndicatorAnimation.this.setLine(x1 + step, x2);
                    }
                }
            };
            valueAnimator.addUpdateListener(anonymousClass1);
            this.mAnimator.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    TabIndicatorAnimation.this.mIsAnimEnd = true;
                    HwScrollingTabContainerView.this.mTabClicked = false;
                    TabIndicatorAnimation.this.setLine(toX1, toX2);
                }

                public void onAnimationCancel(Animator animation) {
                    HwScrollingTabContainerView.this.mTabClicked = false;
                }
            });
        }
    }

    public HwScrollingTabContainerView(Context context) {
        super(context);
        this.mDensity = context.getResources().getDisplayMetrics().density;
        this.mIsHwDarkTheme = HwWidgetFactory.isHwDarkTheme(context);
        this.mIsHwEmphasizeTheme = HwWidgetFactory.isHwEmphasizeTheme(context);
        updateTabViewContainerWidth(context);
        this.mRegular = Typeface.create("sans-serif", 0);
        this.mRegularCondensed = Typeface.create("sans-serif-condensed-regular", 0);
        this.mRTLFlag = SystemProperties.getRTLFlag();
        this.mTabDefaultSize = getContext().getResources().getDimensionPixelSize(34472417);
        this.mTabMinSize = getContext().getResources().getDimensionPixelSize(34472418);
        this.mTabStepSize = getContext().getResources().getDimensionPixelSize(34472419);
        this.mMinTabTextSize = this.mTabDefaultSize;
        createTabIndicator();
        setShouldAnimToTab(this.mShouldAnimToTab);
    }

    public void setShouldAnimToTab(boolean shouldAnim) {
        boolean z = !IS_EMUI_LITE && shouldAnim;
        this.mShouldAnimToTab = z;
    }

    private void updateOrigPos() {
        LinearLayout tabLayout = getTabLayout();
        this.mTabLayoutWidth = tabLayout.getWidth();
        TabView tabView = (TabView) tabLayout.getChildAt(getSelectedTabIndex());
        int translationX = tabView.getLeft();
        if (this.mRTLFlag) {
            translationX = this.mRealWidthPixels - tabView.getRight();
        }
        int translationY = tabView.getBottom() - this.mIndicatorHeight;
        if (this.mTabIndicatorAnimation != null) {
            this.mTabIndicatorAnimation.cancelAnim();
        }
        int count = tabLayout.getChildCount();
        this.mTabIndicator.setTranslationY((float) translationY);
        LayoutParams lp = (LayoutParams) this.mTabIndicator.getLayoutParams();
        if (this.mRTLFlag) {
            lp.rightMargin = translationX;
        } else {
            lp.leftMargin = translationX;
        }
        lp.width = this.mRealWidthPixels / count;
        this.mTabIndicator.setLayoutParams(lp);
        this.mTabIndicator.setMinimumWidth(lp.width);
        if (this.mTabIndicatorAnimation != null) {
            this.mTabIndicatorAnimation.setViewWidth(lp.width);
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTabViewContainerWidth(this.mContext);
        destroyTabIndicator();
        createTabIndicator();
        getTabLayout().requestLayout();
    }

    public void updateTabViewContainerWidth(Context context) {
        int tabContainerViewWidth = context.getResources().getDimensionPixelOffset(34472070);
        boolean isLandscape = context.getResources().getConfiguration().orientation == 2;
        if (tabContainerViewWidth != 0) {
            this.mWidthPixels = tabContainerViewWidth;
            return;
        }
        int sw = (int) (this.mDensity * ((float) context.getResources().getConfiguration().screenWidthDp));
        Point point = new Point();
        this.mWindowManager.getDefaultDisplay().getRealSize(point);
        if (point.x < point.y) {
            this.mWidthPixels = point.x;
        } else if (isLandscape) {
            this.mWidthPixels = (sw * 8) / 12;
        } else {
            this.mWidthPixels = sw;
        }
    }

    private void createTabIndicator() {
        this.mIndicatorHeight = getResources().getDimensionPixelSize(34472180);
        if (!HwWidgetUtils.isActionbarBackgroundThemed(getContext())) {
            int titleColorRes;
            if (this.mTabIndicator == null) {
                this.mTabIndicator = new ImageView(this.mContext);
            }
            if (this.mIsHwDarkTheme) {
                titleColorRes = 33882247;
            } else if (this.mIsHwEmphasizeTheme) {
                titleColorRes = 33882248;
            } else {
                titleColorRes = 33882241;
            }
            this.mTabTitleSelectedColor = this.mContext.getResources().getColor(titleColorRes);
            this.mTabIndicator.setBackgroundColor(this.mTabTitleSelectedColor);
            this.mTabIndicator.setScaleType(ScaleType.FIT_XY);
            LayoutParams lp = new LayoutParams(-1, -2);
            lp.width = this.mWidthPixels;
            lp.height = this.mIndicatorHeight;
            this.mTabIndicator.setLayoutParams(lp);
            if (this.mTabIndicatorAnimation == null) {
                this.mTabIndicatorAnimation = new TabIndicatorAnimation(this.mTabIndicator);
            }
        }
    }

    private void destroyTabIndicator() {
        if (this.mTabIndicator != null) {
            removeView(this.mTabIndicator);
            this.mTabIndicator = null;
            this.mTabIndicatorAnimation = null;
        }
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mTabChanged = true;
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (this.mTabIndicatorAnimation != null && this.mTabIndicatorAnimation.isAnimEnd()) {
            LinearLayout tabLayout = getTabLayout();
            if (tabLayout.getChildCount() > 0) {
                if (tabLayout.getChildCount() <= 1) {
                    if (this.mTabIndicator != null) {
                        this.mTabIndicator.setVisibility(8);
                    }
                } else if (this.mTabIndicator != null) {
                    this.mTabIndicator.setVisibility(0);
                }
                if (!(((TabView) tabLayout.getChildAt(getSelectedTabIndex())) == null || HwWidgetUtils.isActionbarBackgroundThemed(getContext()) || this.mTabIndicator == null)) {
                    if (this.isWidthPixelsChanged) {
                        setTabScrollingOffsets(this.mLastPos, 0.0f);
                        this.isWidthPixelsChanged = false;
                    }
                    if (this.mTabIndicator.getParent() != this) {
                        addViewInLayout(this.mTabIndicator, 1, this.mTabIndicator.getLayoutParams());
                        updateOrigPos();
                    } else if (this.mTabChanged) {
                        updateTabViewContainerWidth(this.mContext);
                        updateOrigPos();
                        this.mTabChanged = false;
                    }
                    if (this.mTabLayoutWidth != tabLayout.getWidth()) {
                        updateOrigPos();
                        this.mTabLayoutWidth = tabLayout.getWidth();
                    }
                }
                this.mLastPos = getSelectedTabIndex();
            }
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LinearLayout tabLayout = getTabLayout();
        int childCount = tabLayout.getChildCount();
        int j = 0;
        setAllowCollapse(false);
        tabLayout.setMeasureWithLargestChildEnabled(false);
        if (childCount > 0) {
            int totalW = this.mWidthPixels;
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            if (widthSize < totalW) {
                this.mAvailableSpace = 0;
                totalW = widthSize;
            } else {
                this.mAvailableSpace = widthSize - this.mWidthPixels;
            }
            this.mRealWidthPixels = totalW;
            int w = totalW / childCount;
            if (this.mAvailableSpace > 0) {
                this.mAvailableSpace += totalW % childCount;
            }
            while (j < childCount) {
                TabView child = (TabView) tabLayout.getChildAt(j);
                if (child != null) {
                    measureTabView(child, w, heightMeasureSpec);
                }
                j++;
            }
            this.mMinTabTextSize = this.mTabDefaultSize;
            if (!(this.mLastContainerWidth == 0 || this.mTabIndicator == null || this.mLastContainerWidth == totalW || this.mTabIndicator.getParent() != this)) {
                this.isWidthPixelsChanged = true;
            }
            this.mLastContainerWidth = totalW;
            if (this.mTabIndicatorAnimation != null) {
                this.mTabIndicatorAnimation.setViewWidth(w);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void handlePressed(View view, boolean pressed) {
        if (!HwWidgetUtils.isActionbarBackgroundThemed(getContext()) && this.mTabIndicator != null) {
            if (((TabView) getTabLayout().getChildAt(getSelectedTabIndex())) == view || !pressed) {
                this.mTabIndicator.setPressed(pressed);
            }
        }
    }

    private void measureTabView(TabView child, int w, int heightMeasureSpec) {
        TabView tabView = child;
        int i = w;
        int i2 = heightMeasureSpec;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(i, -1);
        tabView.setLayoutParams(lp);
        TextView tv = child.getTextView();
        ImageView iv = child.getIconView();
        int tabPadding = child.getPaddingStart() + child.getPaddingEnd();
        if (child.getCustomView() == null && tv != null) {
            tv.setSingleLine(true);
            tv.setMaxLines(1);
            int iconSize = iv == null ? 0 : iv.getMeasuredWidth();
            if (this.mMinTabTextSize == this.mTabDefaultSize) {
                tv.setTextSize(0, (float) this.mTabDefaultSize);
            }
            tv.setTypeface(this.mRegular);
            tv.measure(0, i2);
            int requiredWidth = (tv.getMeasuredWidth() + iconSize) + tabPadding;
            if (requiredWidth > i) {
                tv.setTypeface(this.mRegularCondensed);
                tv.measure(0, i2);
                requiredWidth = (tv.getMeasuredWidth() + iconSize) + tabPadding;
            }
            if (requiredWidth > i) {
                int diff;
                int requiredWidthSmallFont;
                if (this.mAvailableSpace > 0) {
                    diff = requiredWidth - i;
                    if (diff <= this.mAvailableSpace) {
                        this.mAvailableSpace -= diff;
                        lp.width = i + diff;
                        tabView.setLayoutParams(lp);
                        return;
                    }
                    tv.measure(0, i2);
                    requiredWidthSmallFont = (tv.getMeasuredWidth() + iconSize) + tabPadding;
                    if (requiredWidthSmallFont > i) {
                        int diffSmallFont = requiredWidthSmallFont - i;
                        if (diffSmallFont <= this.mAvailableSpace) {
                            this.mAvailableSpace -= diffSmallFont;
                            lp.width = i + diffSmallFont;
                            tabView.setLayoutParams(lp);
                            return;
                        }
                        lp.width = this.mAvailableSpace + i;
                        this.mAvailableSpace = 0;
                        tabView.setLayoutParams(lp);
                    } else {
                        return;
                    }
                }
                diff = this.mMinTabTextSize;
                tv.measure(0, i2);
                requiredWidthSmallFont = (tv.getMeasuredWidth() + iconSize) + tabPadding;
                while (requiredWidthSmallFont > lp.width && diff > this.mTabMinSize) {
                    diff -= this.mTabStepSize;
                    tv.setTextSize(0, (float) diff);
                    tv.measure(0, i2);
                    requiredWidthSmallFont = (tv.getMeasuredWidth() + iconSize) + tabPadding;
                }
                if (diff < this.mMinTabTextSize) {
                    this.mMinTabTextSize = diff;
                }
                LinearLayout tabLayout = getTabLayout();
                int tabCount = tabLayout.getChildCount();
                int i3 = 0;
                while (true) {
                    int i4 = i3;
                    if (i4 >= tabCount) {
                        break;
                    }
                    TabView tabView2 = (TabView) tabLayout.getChildAt(i4);
                    if (!(tabView2 == null || tabView2 == tabView)) {
                        TextView tabTv = tabView2.getTextView();
                        if (tabTv != null) {
                            tabTv.setTextSize(0, (float) diff);
                        }
                    }
                    i3 = i4 + 1;
                    tabView = child;
                    i = w;
                }
                if (requiredWidthSmallFont > lp.width) {
                    tv.setSingleLine(false);
                    tv.setMaxLines(2);
                }
            }
        }
    }

    private void updateTabIndicatorPos(int position) {
        float tabOffset = (float) getTabLayout().getChildAt(position).getLeft();
        if (this.mTabIndicator == null) {
            Log.w(this.TAG, "mTabIndicator is null!");
            return;
        }
        LayoutParams lp = (LayoutParams) this.mTabIndicator.getLayoutParams();
        lp.leftMargin = (int) tabOffset;
        this.mTabIndicator.setLayoutParams(lp);
        this.mLastPos = position;
    }

    public void setTabScrollingOffsets(int position, float x) {
        int i = position;
        if (this.mTabIndicatorAnimation != null && this.mTabIndicatorAnimation.isAnimEnd()) {
            if (!HwWidgetUtils.isActionbarBackgroundThemed(getContext()) && this.mTabIndicator != null) {
                LinearLayout mTabLayout = getTabLayout();
                int count = mTabLayout.getChildCount();
                if (count <= 1 || ((i == count - 1 && x > 0.0f) || this.mTabClicked)) {
                    Log.w(this.TAG, "Do not scroll tab point");
                    return;
                }
                if (this.mScrollerLastPos > i) {
                    this.mScrollDirection = 1;
                }
                if (this.mScrollerLastPos < i) {
                    this.mScrollDirection = 0;
                }
                int tabWidth = mTabLayout.getChildAt(0).getWidth();
                float tabOffset = (float) mTabLayout.getChildAt(i).getLeft();
                if (this.mRTLFlag) {
                    tabOffset = (float) (this.mRealWidthPixels - mTabLayout.getChildAt(i).getRight());
                }
                float scrollerOffset = Math.abs(((float) (mTabLayout.getChildAt(1).getLeft() - mTabLayout.getChildAt(0).getLeft())) * x);
                LayoutParams lp = (LayoutParams) this.mTabIndicator.getLayoutParams();
                if (this.mScrollDirection == 0) {
                    if (x < SCALESIZE - 1.0f) {
                        if (this.mRTLFlag) {
                            lp.rightMargin = (int) tabOffset;
                        } else {
                            lp.leftMargin = (int) tabOffset;
                        }
                        this.mTabIndicator.setMinimumWidth((int) (((float) tabWidth) + scrollerOffset));
                    } else {
                        float scale = (x - (SCALESIZE - 1.0f)) / (DOUBLE - SCALESIZE);
                        if (this.mRTLFlag) {
                            lp.rightMargin = (int) ((((float) tabWidth) * scale) + tabOffset);
                        } else {
                            lp.leftMargin = (int) ((((float) tabWidth) * scale) + tabOffset);
                        }
                        this.mTabIndicator.setMinimumWidth((int) ((((float) tabWidth) * SCALESIZE) - ((((float) tabWidth) * (SCALESIZE - 1.0f)) * scale)));
                    }
                } else if (x < DOUBLE - SCALESIZE) {
                    float scale2 = ((DOUBLE - SCALESIZE) - x) / (DOUBLE - SCALESIZE);
                    if (this.mRTLFlag) {
                        lp.rightMargin = (int) (tabOffset + scrollerOffset);
                    } else {
                        lp.leftMargin = (int) (tabOffset + scrollerOffset);
                    }
                    this.mTabIndicator.setMinimumWidth((int) ((((float) tabWidth) * SCALESIZE) - ((((float) tabWidth) * (SCALESIZE - 1.0f)) * scale2)));
                    if (x == 0.0f) {
                        this.mScrollDirection = 0;
                    }
                } else {
                    if (this.mRTLFlag) {
                        lp.rightMargin = (int) (tabOffset + scrollerOffset);
                    } else {
                        lp.leftMargin = (int) (tabOffset + scrollerOffset);
                    }
                    this.mTabIndicator.setMinimumWidth((int) ((((float) tabWidth) * DOUBLE) - scrollerOffset));
                }
                this.mTabIndicator.setLayoutParams(lp);
            } else {
                return;
            }
        }
        this.mScrollerLastPos = i;
    }

    public void removeTabAt(int position) {
        super.removeTabAt(position);
        this.mTabChanged = true;
    }

    public void removeAllTabs() {
        super.removeAllTabs();
        this.mTabChanged = true;
    }

    public void addTab(Tab tab, boolean setSelected) {
        super.addTab(tab, setSelected);
        this.mTabChanged = true;
    }

    public void addTab(Tab tab, int position, boolean setSelected) {
        super.addTab(tab, position, setSelected);
        this.mTabChanged = true;
    }

    protected boolean disableMaxTabWidth() {
        return true;
    }

    protected void handleTabViewCreated(TabView tabView) {
        if (tabView != null) {
            Tab tab = tabView.getTab();
            if (tab instanceof HwTabImpl) {
                tabView.setId(((HwTabImpl) tab).getTabViewId());
            }
            if (HwWidgetUtils.isActionbarBackgroundThemed(getContext())) {
                tabView.setBackgroundResource(33751382);
                return;
            }
            int resBackground = HwWidgetFactory.getImmersionResource(this.mContext, 33751384, 0, 33751385, false);
            if (this.mIsHwEmphasizeTheme) {
                resBackground = 33751386;
            }
            tabView.setBackgroundResource(resBackground);
        }
    }

    protected void handleTabClicked(int position) {
        if (this.mShouldAnimToTab) {
            if (this.mTabIndicatorAnimation != null) {
                this.mTabIndicatorAnimation.startAnim(this.mLastPos, position);
            }
            this.mLastPos = position;
            this.mScrollerLastPos = position;
            return;
        }
        updateTabIndicatorPos(position);
        this.mTabClicked = false;
    }

    protected void initTitleAppearance(TextView textView) {
        int colorRes = 33882440;
        if (this.mIsHwDarkTheme) {
            colorRes = 33882441;
        } else if (this.mIsHwEmphasizeTheme) {
            colorRes = 33882442;
        }
        textView.setTextColor(this.mContext.getResources().getColorStateList(colorRes));
    }

    protected int adjustPadding(int availableWidth, int itemPaddingSize) {
        if (availableWidth == this.mWidthPixels) {
            return 0;
        }
        return 2 * itemPaddingSize;
    }

    public void setTabSelected(int position) {
        super.setTabSelected(position);
        requestLayout();
    }
}
