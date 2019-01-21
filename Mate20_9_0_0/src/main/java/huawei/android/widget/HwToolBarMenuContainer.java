package huawei.android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import com.android.internal.widget.ActionBarContainer;
import huawei.android.widget.effect.engine.HwBlurEngine;
import huawei.android.widget.effect.engine.HwBlurEngine.BlurType;
import huawei.android.widget.loader.ResLoader;
import huawei.android.widget.loader.ResLoaderUtil;

public class HwToolBarMenuContainer extends ActionBarContainer {
    private static final boolean DEBUG = true;
    private static final String TAG = "HwToolBarMenuContainer";
    private boolean isBlurEnable;
    private AttributeSet mAttrs;
    private HwBlurEngine mBlurEngine;
    private int mEndLocation;
    private boolean mForcedSplitBackground;
    private Drawable mSplitBackground;
    private int mStartLocation;
    private int mWidthPixels;

    public HwToolBarMenuContainer(Context context) {
        this(context, null);
    }

    public HwToolBarMenuContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBlurEngine = HwBlurEngine.getInstance();
        this.isBlurEnable = false;
        Log.d(TAG, "new HwToolBarMenuContainer");
        this.mAttrs = attrs;
        DisplayMetrics dp = context.getResources().getDisplayMetrics();
        this.mWidthPixels = dp.widthPixels <= dp.heightPixels ? dp.widthPixels : dp.heightPixels;
    }

    public void setForcedSplitBackground(boolean forced) {
        this.mForcedSplitBackground = forced;
    }

    public void setSplitBackground(Drawable bg) {
        this.mSplitBackground = bg;
        this.mForcedSplitBackground = DEBUG;
    }

    private void initBackgroundResource() {
        if (this.mForcedSplitBackground) {
            if (this.mSplitBackground == null) {
                setPadding(0, 0, 0, 0);
            }
            setBackground(this.mSplitBackground);
        } else {
            ResLoader resLoader = ResLoader.getInstance();
            Resources res = resLoader.getResources(this.mContext);
            Theme theme = resLoader.getTheme(this.mContext);
            if (theme != null) {
                TypedArray a = theme.obtainStyledAttributes(this.mAttrs, resLoader.getIdentifierArray(getContext(), ResLoaderUtil.STAYLEABLE, "HwToolbar"), 16843946, 0);
                int backgroundId = a.getResourceId(resLoader.getIdentifier(getContext(), ResLoaderUtil.STAYLEABLE, "HwToolbar_hwToolbarSplitBarBackground"), -1);
                if (backgroundId != -1) {
                    setBackgroundResource(backgroundId);
                }
                a.recycle();
            }
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
    }

    public void setSplitViewLocation(int start, int end) {
        this.mStartLocation = start;
        this.mEndLocation = end;
    }

    public void draw(Canvas canvas) {
        if (this.mBlurEngine.isShowHwBlur(this)) {
            this.mBlurEngine.draw(canvas, this);
            super.dispatchDraw(canvas);
            return;
        }
        super.draw(canvas);
    }

    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == 0) {
            this.mBlurEngine.addBlurTargetView(this, BlurType.LightBlurWithGray);
            this.mBlurEngine.setTargetViewBlurEnable(this, isBlurEnable());
            return;
        }
        this.mBlurEngine.removeBlurTargetView(this);
    }

    public boolean isBlurEnable() {
        return this.isBlurEnable;
    }

    public void setBlurEnable(boolean blurEnable) {
        this.isBlurEnable = blurEnable;
        this.mBlurEngine.setTargetViewBlurEnable(this, isBlurEnable());
    }
}
