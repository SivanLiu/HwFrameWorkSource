package huawei.com.android.internal.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hwcontrol.HwWidgetFactory;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class HwToolBarMenuContainer extends HwActionBarContainer {
    private int mEndLocation;
    private View mMenu;
    private int mOldOrientation;
    private int mStartLocation;
    private int mWidthPixels;

    public HwToolBarMenuContainer(Context context) {
        this(context, null);
    }

    public HwToolBarMenuContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        DisplayMetrics dp = context.getResources().getDisplayMetrics();
        this.mWidthPixels = dp.widthPixels < dp.heightPixels ? dp.widthPixels : dp.heightPixels;
        setHeight(-1);
        this.mOldOrientation = getResources().getConfiguration().orientation;
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
            return;
        }
        Drawable old = getBackground();
        initBackgroundResource();
        if (old != getBackground()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
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
        }
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == 2;
    }

    private void resetLocationX() {
        this.mMenu = getChildAt(0);
        if (this.mMenu != null) {
            this.mMenu.setTranslationX(0.0f);
        }
    }
}
