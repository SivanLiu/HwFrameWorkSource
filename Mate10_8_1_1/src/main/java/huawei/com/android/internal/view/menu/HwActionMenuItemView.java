package huawei.com.android.internal.view.menu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.hwcontrol.HwWidgetFactory;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import com.android.internal.view.menu.ActionMenuItemView;
import huawei.android.widget.DecouplingUtil.ReflectUtil;
import huawei.android.widget.HwSmartColorListener;

public class HwActionMenuItemView extends ActionMenuItemView {
    private static final int DEFAULT_MENU_ICON_SIZE = -1;
    private static final int MAX_ICON_SIZE = 32;
    static final int MENU_ITEM_TOUCHABLE_X_RANGE = 58;
    private static final String TAG = "HwActionMenuItemView";
    private final int[] mActionMenuTextColorAttr;
    private float mDensity;
    private boolean mExpandedFormat;
    private Drawable mIcon;
    private boolean mIsSmartColored;
    private int mMaxIconSize;
    private ColorStateList mTintColor;
    private int mTintRes;
    private CharSequence mTitle;
    private boolean mToolbarAttachOverlay;
    private int mTouchXRange;
    private boolean shouldBeProcessed;

    public HwActionMenuItemView(Context context) {
        this(context, null);
    }

    public HwActionMenuItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HwActionMenuItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mActionMenuTextColorAttr = new int[]{16843617};
        this.mToolbarAttachOverlay = false;
        Log.i(TAG, TAG);
        this.mDensity = context.getResources().getDisplayMetrics().density;
        this.mTouchXRange = (int) (this.mDensity * 58.0f);
        this.mMaxIconSize = (int) ((32.0f * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public void updateTextButtonVisibility() {
        int i = 1;
        super.updateTextButtonVisibility();
        if (getParent() != null) {
            CharSequence charSequence;
            boolean visible = TextUtils.isEmpty(this.mTitle) ^ 1;
            if (this.mIcon != null) {
                if (!getItemData().showsTextAsAction()) {
                    i = 0;
                } else if (!getAllowTextWithIcon1()) {
                    i = this.mExpandedFormat;
                }
            }
            if (((visible & i) | showHwTextWithAction()) | this.mToolbarAttachOverlay) {
                charSequence = this.mTitle;
            } else {
                charSequence = null;
            }
            setText(charSequence);
        }
    }

    public void setToolBarAttachOverlay(boolean isAttach) {
        this.mToolbarAttachOverlay = isAttach;
    }

    public void setTitle(CharSequence title) {
        super.setTitle(title);
        this.mTitle = title;
    }

    private boolean showHwTextWithAction() {
        ViewParent menuView = getParent();
        if (menuView != null) {
            ViewParent container = menuView.getParent();
            if (container != null && ((View) container).getId() == 16909317) {
                return true;
            }
        }
        return false;
    }

    public void setIcon(Drawable icon) {
        setIconDirect1(icon);
        if (icon != null) {
            float scale;
            int width = icon.getIntrinsicWidth();
            int height = icon.getIntrinsicHeight();
            if (width > getMaxIconSize1()) {
                scale = ((float) getMaxIconSize1()) / ((float) width);
                width = getMaxIconSize1();
                height = (int) (((float) height) * scale);
            }
            if (height > getMaxIconSize1()) {
                scale = ((float) getMaxIconSize1()) / ((float) height);
                height = getMaxIconSize1();
                width = (int) (((float) width) * scale);
            }
            icon.setBounds(0, 0, width, height);
        }
        updateTextAndIcon();
    }

    public void setExpandedFormat(boolean expandedFormat) {
        if (this.mExpandedFormat != expandedFormat) {
            this.mExpandedFormat = expandedFormat;
        }
    }

    public void updateTextAndIcon() {
        if (getParent() != null) {
            updateTextButtonVisibility();
            Drawable icon = this.mIcon;
            ColorStateList smartIconColor = getSmartIconColor();
            ColorStateList smartTitleColor = getSmartTitleColor();
            if (smartIconColor == null || smartTitleColor == null) {
                int resTint;
                this.mIsSmartColored = false;
                if (hasText()) {
                    if (HwWidgetFactory.isHwDarkTheme(this.mContext)) {
                        resTint = 33882141;
                    } else {
                        resTint = 33882140;
                    }
                    setCompoundDrawables(null, this.mIcon, null, null);
                } else {
                    resTint = HwWidgetFactory.getImmersionResource(this.mContext, 33882140, 0, 33882141, false);
                    setCompoundDrawables(this.mIcon, null, null, null);
                }
                if (HwWidgetFactory.isHwEmphasizeTheme(this.mContext)) {
                    resTint = 33882402;
                }
                if (!(this.mTintRes == resTint || icon == null || !(icon instanceof VectorDrawable))) {
                    this.mTintRes = resTint;
                    this.mTintColor = getContext().getColorStateList(resTint);
                    icon.setTintList(this.mTintColor);
                }
                if (getItemData().isChecked()) {
                    HwWidgetFactory.setImmersionStyle(getContext(), this, 33882410, 33882409, 0, false);
                } else {
                    TypedArray ta = getContext().getTheme().obtainStyledAttributes(this.mActionMenuTextColorAttr);
                    setTextColor(ta.getColorStateList(0));
                    ta.recycle();
                }
                return;
            }
            if (hasText()) {
                setCompoundDrawables(null, icon, null, null);
            } else {
                setCompoundDrawables(icon, null, null, null);
            }
            if (icon != null) {
                icon.setTintList(smartIconColor);
            }
            setTextColor(smartTitleColor);
            this.mIsSmartColored = true;
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateTextAndIcon();
        requestLayout();
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            updateTextAndIcon();
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == 0 && event.getX() > ((float) (getWidth() - this.mTouchXRange)) / 2.0f && event.getX() < (((float) (getWidth() - this.mTouchXRange)) / 2.0f) + ((float) this.mTouchXRange)) {
            this.shouldBeProcessed = true;
            return super.onTouchEvent(event);
        } else if (!this.shouldBeProcessed) {
            return false;
        } else {
            if (event.getAction() == 1) {
                this.shouldBeProcessed = false;
            }
            return super.onTouchEvent(event);
        }
    }

    protected void onDraw(Canvas canvas) {
        Drawable icon = this.mIcon;
        if (!(icon == null || !(icon instanceof VectorDrawable) || (this.mIsSmartColored ^ 1) == 0)) {
            icon.setTintList(this.mTintColor);
        }
        super.onDraw(canvas);
    }

    private int getMaxIconSize1() {
        int maxIconSize = getContext().getResources().getDimensionPixelSize(34472086);
        return maxIconSize <= 0 ? this.mMaxIconSize : maxIconSize;
    }

    private void setIconDirect1(Drawable msetIcon) {
        this.mIcon = msetIcon;
        ReflectUtil.setObject("mIcon", this, msetIcon, ActionMenuItemView.class);
    }

    private boolean getAllowTextWithIcon1() {
        return ((Boolean) ReflectUtil.getObject(this, "mAllowTextWithIcon", ActionMenuItemView.class)).booleanValue();
    }

    protected boolean forceMeasureForMinWidth() {
        return true;
    }

    public boolean needsDividerBefore() {
        return false;
    }

    public boolean needsDividerAfter() {
        return false;
    }

    private ColorStateList getSmartIconColor() {
        ViewParent parent = getParent();
        if (parent instanceof HwSmartColorListener) {
            return ((HwSmartColorListener) parent).getSmartIconColor();
        }
        return null;
    }

    private ColorStateList getSmartTitleColor() {
        ViewParent parent = getParent();
        if (parent instanceof HwSmartColorListener) {
            return ((HwSmartColorListener) parent).getSmartTitleColor();
        }
        return null;
    }
}
