package huawei.android.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hwcontrol.HwWidgetFactory;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import com.android.internal.R;

public class HwToolBarMenuContainer extends FrameLayout {
    private static final int ANIMEXTDURATION = 350;
    private static final int ANIMUPDURATION = 150;
    private static final boolean DEBUG = true;
    private static final boolean IS_EMUI_LITE = SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", IS_EMUI_LITE);
    private static final String TAG = "HwToolBarMenuContainer";
    private ValueAnimator mAnimExtend;
    private ObjectAnimator mAnimUp;
    private boolean mAnimationEnabled;
    private int mEndLocation;
    private boolean mForcedSplitBackground;
    private LayoutParams mLayoutParams;
    private View mMenu;
    private int mOldOrientation;
    private int mOrigWidth;
    private Drawable mSplitBackground;
    private int mStartLocation;
    private int mWidthPixels;

    public HwToolBarMenuContainer(Context context) {
        this(context, null);
    }

    public HwToolBarMenuContainer(Context context, AttributeSet attrs) {
        boolean z = DEBUG;
        super(context, attrs);
        this.mOrigWidth = 0;
        this.mAnimationEnabled = DEBUG;
        Log.d(TAG, "new HwToolBarMenuContainer");
        DisplayMetrics dp = context.getResources().getDisplayMetrics();
        this.mWidthPixels = dp.widthPixels <= dp.heightPixels ? dp.widthPixels : dp.heightPixels;
        this.mOldOrientation = getResources().getConfiguration().orientation;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ActionBar);
        this.mSplitBackground = a.getDrawable(19);
        a.recycle();
        if (this.mSplitBackground != null) {
            z = IS_EMUI_LITE;
        }
        setWillNotDraw(z);
    }

    public void setAnimationEnable(boolean enableAnim) {
        this.mAnimationEnabled = enableAnim;
    }

    public boolean isAnimationEnabled() {
        return this.mAnimationEnabled;
    }

    public void setForcedSplitBackground(boolean forced) {
        this.mForcedSplitBackground = forced;
    }

    public void setSplitBackground(Drawable bg) {
        boolean z = IS_EMUI_LITE;
        if (this.mSplitBackground != null) {
            this.mSplitBackground.setCallback(null);
            unscheduleDrawable(this.mSplitBackground);
        }
        this.mSplitBackground = bg;
        if (bg != null) {
            bg.setCallback(this);
            if (this.mSplitBackground != null) {
                this.mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }
        }
        if (this.mSplitBackground == null) {
            z = DEBUG;
        }
        setWillNotDraw(z);
        invalidate();
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        boolean isVisible = visibility == 0 ? DEBUG : IS_EMUI_LITE;
        if (this.mSplitBackground != null) {
            this.mSplitBackground.setVisible(isVisible, IS_EMUI_LITE);
        }
    }

    protected boolean verifyDrawable(Drawable who) {
        return who != this.mSplitBackground ? super.verifyDrawable(who) : DEBUG;
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int[] state = getDrawableState();
        boolean changed = IS_EMUI_LITE;
        Drawable splitBackground = this.mSplitBackground;
        if (splitBackground != null && splitBackground.isStateful()) {
            changed = splitBackground.setState(state);
        }
        if (changed) {
            invalidate();
        }
    }

    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mSplitBackground != null) {
            this.mSplitBackground.jumpToCurrentState();
        }
    }

    public void onResolveDrawables(int layoutDirection) {
        super.onResolveDrawables(layoutDirection);
        if (this.mSplitBackground != null) {
            this.mSplitBackground.setLayoutDirection(layoutDirection);
        }
    }

    private Drawable getSplitBackground() {
        return this.mSplitBackground;
    }

    private void initBackgroundResource() {
        if (this.mForcedSplitBackground) {
            if (getSplitBackground() == null) {
                setPadding(0, 0, 0, 0);
            }
            setBackground(getSplitBackground());
        } else if (HwWidgetFactory.isHwDarkTheme(getContext()) || HwWidgetFactory.isHwEmphasizeTheme(getContext())) {
            setBackgroundResource(33751610);
        } else {
            setBackgroundResource(33751609);
        }
        Drawable d = getBackground();
        if (d != null) {
            Rect padding = new Rect();
            d.getPadding(padding);
            setPadding(padding.left, padding.top, padding.right, padding.bottom);
        }
    }

    private View getVisibleMenuView() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (getChildAt(i).getVisibility() == 0) {
                return getChildAt(i);
            }
        }
        return null;
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        View menuView = getVisibleMenuView();
        if (menuView == null || menuView.getMeasuredHeight() <= 0) {
            setMeasuredDimension(0, 0);
            setPadding(0, 0, 0, 0);
            setBackgroundResource(0);
        } else {
            Drawable old = getBackground();
            initBackgroundResource();
            if (old != getBackground()) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
        final int dst_width = getMeasuredWidth();
        if (isLandscape()) {
            this.mOrigWidth = 0;
        }
        if (isAnimatable()) {
            if (!(dst_width == this.mOrigWidth || this.mOrigWidth == 0 || dst_width == 0)) {
                this.mLayoutParams = getLayoutParams();
                setMeasuredDimension(this.mOrigWidth, getMeasuredHeight());
                final int start = this.mOrigWidth;
                int end = dst_width;
                this.mOrigWidth = dst_width;
                this.mAnimExtend = ValueAnimator.ofFloat(new float[]{1.0f});
                this.mAnimExtend.setDuration(350);
                this.mAnimExtend.setInterpolator(new PathInterpolator(0.3f, 0.15f, 0.1f, 0.85f));
                this.mAnimExtend.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int width = (int) (((float) start) + (((float) (dst_width - start)) * animation.getAnimatedFraction()));
                        if (width % 2 != 0) {
                            width++;
                        }
                        HwToolBarMenuContainer.this.mLayoutParams.width = width;
                        HwToolBarMenuContainer.this.mMenu = HwToolBarMenuContainer.this.getChildAt(0);
                        if (HwToolBarMenuContainer.this.mMenu != null) {
                            HwToolBarMenuContainer.this.mMenu.setTranslationX((float) ((width / 2) - (dst_width / 2)));
                        }
                        HwToolBarMenuContainer.this.setLayoutParams(HwToolBarMenuContainer.this.mLayoutParams);
                    }
                });
                this.mAnimExtend.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        HwToolBarMenuContainer.this.mAnimExtend = null;
                        HwToolBarMenuContainer.this.mMenu = HwToolBarMenuContainer.this.getChildAt(0);
                        if (HwToolBarMenuContainer.this.mMenu != null) {
                            HwToolBarMenuContainer.this.mMenu.setTranslationX(0.0f);
                        }
                        HwToolBarMenuContainer.this.mLayoutParams.width = -2;
                        HwToolBarMenuContainer.this.setLayoutParams(HwToolBarMenuContainer.this.mLayoutParams);
                    }
                });
                this.mAnimExtend.start();
            }
            if (this.mOrigWidth == 0 && dst_width != 0) {
                this.mOrigWidth = dst_width;
                this.mAnimUp = ObjectAnimator.ofFloat(this, "alpha", new float[]{0.0f, 1.0f});
                this.mAnimUp.setInterpolator(new PathInterpolator(0.3f, 0.15f, 0.1f, 0.85f));
                this.mAnimUp.setDuration(150);
                this.mAnimUp.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        HwToolBarMenuContainer.this.mAnimUp = null;
                    }
                });
                this.mAnimUp.start();
            }
            if (this.mOrigWidth != 0 && dst_width == 0) {
                disappearNoAnimation();
            }
        } else if (!isLandscape()) {
            this.mOrigWidth = dst_width;
        }
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (this.mSplitBackground != null) {
            this.mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            invalidate();
        }
        int w = getMeasuredWidth();
        int containerW = this.mWidthPixels;
        if (getParent() != null) {
            int pw = ((View) getParent()).getWidth();
            if (pw > 0) {
                containerW = pw;
            }
        }
        if (this.mEndLocation > 0) {
            containerW = this.mEndLocation - this.mStartLocation;
        }
        int realLeft = (left + ((containerW - w) / 2)) + this.mStartLocation;
        setLeft(realLeft);
        setRight(realLeft + w);
        resetLocationX();
    }

    public void setSplitViewLocation(int start, int end) {
        this.mStartLocation = start;
        this.mEndLocation = end;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation != this.mOldOrientation) {
            this.mOldOrientation = newConfig.orientation;
            this.mOrigWidth = 0;
            if (this.mAnimExtend != null && this.mAnimExtend.isRunning()) {
                this.mAnimExtend.end();
            }
            if (this.mAnimUp != null && this.mAnimUp.isRunning()) {
                this.mAnimUp.end();
            }
        }
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == 2 ? DEBUG : IS_EMUI_LITE;
    }

    private boolean isAnimatable() {
        int i = 0;
        if (IS_EMUI_LITE || !isAnimationEnabled()) {
            return IS_EMUI_LITE;
        }
        if (this.mAnimUp != null && this.mAnimUp.isRunning()) {
            i = 1;
        } else if (this.mAnimExtend != null) {
            i = this.mAnimExtend.isRunning();
        }
        return i ^ 1;
    }

    private void disappearNoAnimation() {
        this.mOrigWidth = 0;
        setMeasuredDimension(0, 0);
        this.mLayoutParams = getLayoutParams();
        this.mLayoutParams.width = -2;
        setLayoutParams(this.mLayoutParams);
    }

    private void resetLocationX() {
        this.mMenu = getChildAt(0);
        if (this.mMenu != null) {
            this.mMenu.setTranslationX(0.0f);
        }
    }
}
