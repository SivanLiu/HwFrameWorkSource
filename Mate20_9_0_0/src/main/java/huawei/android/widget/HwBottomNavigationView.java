package huawei.android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.internal.view.menu.MenuBuilder;
import huawei.android.widget.effect.engine.HwBlurEngine;
import huawei.android.widget.effect.engine.HwBlurEngine.BlurType;
import huawei.android.widget.loader.ResLoader;
import huawei.android.widget.loader.ResLoaderUtil;
import java.util.ArrayList;
import java.util.List;

public class HwBottomNavigationView extends LinearLayout {
    private static final int MENU_MAX_SIZE = 5;
    private String TAG;
    private boolean isBlurEnable;
    private int mActiveColor;
    private HwBlurEngine mBlurEngine;
    private BottomNavItemClickListenr mBottomNavItemClickListenr;
    private ClickEffectEntry mClickEffectEntry;
    private Context mContext;
    private int mCutoutPadding;
    private int mDefaultColor;
    private DisplayCutout mDisplayCutout;
    private int mDisplayRotate;
    private int mIconBounds;
    private int mLastSelectItem;
    private BottemNavListener mListener;
    private Menu mMenu;
    private MenuInflater mMenuInflater;
    private int mMenuSize;
    private int mMessageBgColor;
    private boolean mNeedFitCutout;
    private boolean mPortLayout;
    private int mTextPadding;

    public interface BottemNavListener {
        void onBottemNavItemReselected(MenuItem menuItem, int i);

        void onBottemNavItemSelected(MenuItem menuItem, int i);

        void onBottemNavItemUnselected(MenuItem menuItem, int i);
    }

    private class BottomNavItemClickListenr implements OnClickListener {
        private BottomNavigationItemView itemView;

        private BottomNavItemClickListenr() {
        }

        public void onClick(View view) {
            if (view instanceof BottomNavigationItemView) {
                this.itemView = (BottomNavigationItemView) view;
                HwBottomNavigationView.this.changeItem(this.itemView, true);
            }
        }
    }

    private class BottomNavigationItemView extends LinearLayout {
        private int mActiveColor;
        private boolean mChecked;
        private LinearLayout mContainer;
        private HwTextView mContent;
        private ComplexDrawable mCurrentDrawable;
        private int mDefaultColor;
        private boolean mDirectionLand;
        float mEndRent;
        boolean mHasMeasured;
        private int mHorizontalPadding;
        private int mIndex;
        private boolean mIsHasMessage;
        private MenuItem mItem;
        private ComplexDrawable mLandComplexDrawable;
        private int mLandMinHeight;
        private int mLandTextSize;
        private int mMinTextSize;
        private int mMsgBgColor;
        private Paint mPaint = new Paint();
        private ComplexDrawable mPortComplexDrawable;
        private int mPortMinHeight;
        private int mPortTextSize;
        private int mRedDotRadius;
        private ImageView mStartImage;
        float mStartRent;
        private int mStepGranularity;
        private ImageView mTopImage;
        private int mVerticalAddedPadding;
        private int mVerticalPadding;

        public BottomNavigationItemView(Context context, MenuItem MenuItem, boolean mIsLand, int index) {
            super(context);
            this.mItem = MenuItem;
            inflate(context, ResLoaderUtil.getLayoutId(context, "bottomnav_item_layout"), this);
            this.mContent = (HwTextView) findViewById(ResLoaderUtil.getViewId(context, "content"));
            this.mTopImage = (ImageView) findViewById(ResLoaderUtil.getViewId(context, "topIcon"));
            this.mStartImage = (ImageView) findViewById(ResLoaderUtil.getViewId(context, "startIcon"));
            this.mContainer = (LinearLayout) findViewById(ResLoaderUtil.getViewId(context, "container"));
            this.mLandComplexDrawable = new ComplexDrawable(context, this.mItem.getIcon());
            this.mPortComplexDrawable = new ComplexDrawable(context, this.mItem.getIcon());
            this.mLandMinHeight = ResLoaderUtil.getDimensionPixelSize(context, "bottomnav_item_land_minheight");
            this.mPortMinHeight = ResLoaderUtil.getDimensionPixelSize(context, "bottomnav_item_port_minheight");
            this.mPortTextSize = ResLoaderUtil.getResources(context).getInteger(ResLoader.getInstance().getIdentifier(context, "integer", "bottomnav_item_port_textsize"));
            this.mLandTextSize = ResLoaderUtil.getResources(context).getInteger(ResLoader.getInstance().getIdentifier(context, "integer", "bottomnav_item_land_textsize"));
            this.mStepGranularity = ResLoaderUtil.getResources(context).getInteger(ResLoader.getInstance().getIdentifier(context, "integer", "bottomnav_text_stepgranularity"));
            this.mMinTextSize = ResLoaderUtil.getResources(context).getInteger(ResLoader.getInstance().getIdentifier(context, "integer", "bottomnav_item_min_textsize"));
            this.mVerticalPadding = ResLoaderUtil.getDimensionPixelSize(context, "bottomnav_item_vertical_padding");
            this.mHorizontalPadding = ResLoaderUtil.getDimensionPixelSize(context, "bottomnav_item_horizontal_padding");
            this.mVerticalAddedPadding = ResLoaderUtil.getDimensionPixelSize(context, "bottomnav_item_top_margin");
            this.mRedDotRadius = ResLoaderUtil.getDimensionPixelSize(context, "bottomnav_item_red_dot_radius");
            this.mContent.setAutoTextInfo(this.mMinTextSize, this.mStepGranularity, 1);
            this.mDirectionLand = mIsLand;
            this.mIndex = index;
            this.mStartImage.setImageDrawable(this.mLandComplexDrawable);
            this.mTopImage.setImageDrawable(this.mPortComplexDrawable);
            this.mPaint.setAntiAlias(true);
            setOrientation(1);
            updateTextAndIcon(true, true);
        }

        public BottomNavigationItemView setActiveColor(int color) {
            this.mActiveColor = color;
            updateTextAndIcon(false, true);
            return this;
        }

        public BottomNavigationItemView setDefaultColor(int color) {
            this.mDefaultColor = color;
            updateTextAndIcon(false, true);
            return this;
        }

        public int getItemIndex() {
            return this.mIndex;
        }

        private boolean isChecked() {
            return this.mChecked;
        }

        public void setChecked(boolean checked, boolean hasAnim) {
            if (checked != this.mChecked) {
                this.mChecked = checked;
                this.mCurrentDrawable = this.mDirectionLand ? this.mLandComplexDrawable : this.mPortComplexDrawable;
                this.mCurrentDrawable.setState(this.mChecked, hasAnim);
                this.mContent.setTextColor(this.mChecked ? this.mActiveColor : this.mDefaultColor);
            }
        }

        public void setDirection(boolean directionLand) {
            if (directionLand != this.mDirectionLand) {
                this.mDirectionLand = directionLand;
            }
            updateTextAndIcon(true, false);
        }

        private void updateTextAndIcon(boolean directionchanged, boolean colorChanged) {
            if (directionchanged) {
                MarginLayoutParams lp;
                if (this.mDirectionLand) {
                    setGravity(17);
                    setMinimumHeight(this.mLandMinHeight);
                    setPaddingRelative(this.mHorizontalPadding, 0, this.mHorizontalPadding, 0);
                    this.mTopImage.setVisibility(8);
                    this.mStartImage.setVisibility(0);
                    lp = (MarginLayoutParams) this.mContent.getLayoutParams();
                    lp.setMarginsRelative(0, 0, 0, 0);
                    this.mContent.setLayoutParams(lp);
                    this.mContent.setAutoTextSize(1, (float) this.mLandTextSize);
                    this.mContent.setGravity(8388611);
                    this.mCurrentDrawable = this.mLandComplexDrawable;
                } else {
                    setGravity(0);
                    setMinimumHeight(this.mPortMinHeight);
                    setPaddingRelative(0, this.mVerticalPadding + this.mVerticalAddedPadding, 0, this.mVerticalPadding);
                    this.mTopImage.setVisibility(0);
                    this.mStartImage.setVisibility(8);
                    lp = (MarginLayoutParams) this.mContent.getLayoutParams();
                    lp.setMarginsRelative(HwBottomNavigationView.this.mTextPadding, 0, HwBottomNavigationView.this.mTextPadding, 0);
                    this.mContent.setLayoutParams(lp);
                    this.mContent.setAutoTextSize(1, (float) this.mPortTextSize);
                    this.mContent.setGravity(1);
                    this.mCurrentDrawable = this.mPortComplexDrawable;
                }
                this.mContent.setText(this.mItem.getTitle());
                this.mCurrentDrawable.setState(this.mChecked, false);
            }
            if (colorChanged) {
                this.mLandComplexDrawable.setActiveColor(this.mActiveColor);
                this.mLandComplexDrawable.setDefaultColor(this.mDefaultColor);
                this.mPortComplexDrawable.setActiveColor(this.mActiveColor);
                this.mPortComplexDrawable.setDefaultColor(this.mDefaultColor);
                this.mContent.setTextColor(this.mChecked ? this.mActiveColor : this.mDefaultColor);
            }
        }

        public TextView getContent() {
            return this.mContent;
        }

        public ImageView getIcon() {
            return this.mDirectionLand ? this.mStartImage : this.mTopImage;
        }

        public LinearLayout getContainer() {
            return this.mContainer;
        }

        boolean isHasMessage() {
            return this.mIsHasMessage;
        }

        void setHasMessage(boolean hasMessage) {
            this.mIsHasMessage = hasMessage;
            invalidate();
        }

        void setMsgBgColor(int color) {
            this.mMsgBgColor = color;
            this.mPaint.setColor(this.mMsgBgColor);
            invalidate();
        }

        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (this.mIsHasMessage) {
                int cx;
                ImageView icon = getIcon();
                Rect itemRect = new Rect();
                Rect iconRect = new Rect();
                getGlobalVisibleRect(itemRect);
                icon.getGlobalVisibleRect(iconRect);
                if (isLayoutRtl()) {
                    cx = (iconRect.left - itemRect.left) + this.mRedDotRadius;
                } else {
                    cx = (iconRect.right - itemRect.left) - this.mRedDotRadius;
                }
                canvas.drawCircle((float) cx, (float) ((iconRect.top - itemRect.top) + this.mRedDotRadius), (float) this.mRedDotRadius, this.mPaint);
            }
        }
    }

    public HwBottomNavigationView(Context context) {
        this(context, null);
    }

    public HwBottomNavigationView(Context context, AttributeSet attrs) {
        this(context, attrs, ResLoader.getInstance().getIdentifier(context, "attr", "bottomNavStyle"));
    }

    public HwBottomNavigationView(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public HwBottomNavigationView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        Context context2 = context;
        int i = defStyleAttr;
        super(context, attrs, defStyleAttr, defStyleRes);
        this.TAG = "HwBottomNavigationView";
        this.mLastSelectItem = -1;
        this.mCutoutPadding = 0;
        this.mDisplayRotate = 0;
        this.mNeedFitCutout = false;
        this.mBlurEngine = HwBlurEngine.getInstance();
        this.isBlurEnable = false;
        this.mClickEffectEntry = null;
        this.mContext = ResLoader.getInstance().getContext(context2);
        this.mMenu = new MenuBuilder(this.mContext);
        this.mMenuInflater = new MenuInflater(this.mContext);
        TypedArray ta = ResLoader.getInstance().getTheme(context2).obtainStyledAttributes(attrs, ResLoader.getInstance().getIdentifierArray(context2, ResLoaderUtil.STAYLEABLE, "bottomNavigation"), i, defStyleRes);
        int menuResId = ta.getResourceId(ResLoader.getInstance().getIdentifier(context2, ResLoaderUtil.STAYLEABLE, "bottomNavigation_bottomNavMenu"), 0);
        int defatultColor = ta.getResourceId(ResLoader.getInstance().getIdentifier(context2, ResLoaderUtil.STAYLEABLE, "bottomNavigation_bottomNavItemDefaultColor"), ResLoader.getInstance().getIdentifier(getContext(), ResLoaderUtil.COLOR, "emui_color_gray_7"));
        int activeColor = ta.getResourceId(ResLoader.getInstance().getIdentifier(context2, ResLoaderUtil.STAYLEABLE, "bottomNavigation_bottomNavItemActiveColor"), ResLoader.getInstance().getIdentifier(getContext(), ResLoaderUtil.COLOR, "emui_accent"));
        int messageColor = ta.getResourceId(ResLoader.getInstance().getIdentifier(context2, ResLoaderUtil.STAYLEABLE, "bottomNavigation_bottomMessageBgColor"), ResLoader.getInstance().getIdentifier(getContext(), ResLoaderUtil.COLOR, "bottom_nav_message_bg"));
        ta.recycle();
        this.mClickEffectEntry = HwWidgetUtils.getCleckEffectEntry(context2, i);
        this.mDefaultColor = ResLoaderUtil.getResources(context).getColor(defatultColor);
        this.mActiveColor = ResLoaderUtil.getResources(context).getColor(activeColor);
        this.mMessageBgColor = ResLoaderUtil.getResources(context).getColor(messageColor);
        this.mTextPadding = ResLoaderUtil.getDimensionPixelSize(context2, "bottomnav_item_text_margin");
        this.mIconBounds = ResLoaderUtil.getDimensionPixelSize(context2, "bottomnav_item_icon_size");
        if (menuResId > 0) {
            this.mMenuInflater.inflate(menuResId, this.mMenu);
        }
        this.mBottomNavItemClickListenr = new BottomNavItemClickListenr();
        correctSize(this.mMenu);
        createNewItem(this.mMenu);
        this.mCutoutPadding = 0;
        this.mDisplayRotate = 0;
        this.mNeedFitCutout = false;
    }

    private boolean correctSize(Menu menu) {
        if (menu.size() > 5) {
            Log.w(this.TAG, "too big size");
            this.mMenuSize = 5;
            return false;
        }
        this.mMenuSize = menu.size();
        return true;
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int size = this.mMenuSize;
        for (int i = 0; i < size; i++) {
            ((BottomNavigationItemView) getChildAt(i)).setDirection(isLand());
        }
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mDisplayCutout = insets.getDisplayCutout();
        this.mCutoutPadding = 0;
        this.mDisplayRotate = 0;
        this.mNeedFitCutout = false;
        if (this.mDisplayCutout != null && HwCutoutUtil.needDoCutoutFit(this, this.mContext)) {
            int rotate = HwCutoutUtil.getDisplayRotate(this.mContext);
            boolean noNavigationBar = HwCutoutUtil.isNavigationBarExist(this.mContext) ^ true;
            if (1 == rotate) {
                this.mNeedFitCutout = true;
                this.mDisplayRotate = 1;
                this.mCutoutPadding = this.mDisplayCutout.getSafeInsetLeft();
            } else if (3 == rotate && noNavigationBar) {
                this.mNeedFitCutout = true;
                this.mDisplayRotate = 3;
                this.mCutoutPadding = this.mDisplayCutout.getSafeInsetRight();
            }
        }
        return super.onApplyWindowInsets(insets);
    }

    private boolean isLand() {
        return !this.mPortLayout && this.mContext.getResources().getConfiguration().orientation == 2;
    }

    public void setPortLayout(boolean forcePort) {
        if (this.mPortLayout != forcePort) {
            this.mPortLayout = forcePort;
            requestLayout();
        }
    }

    private boolean addMenu(int group, int id, int categoryOrder, CharSequence title, Drawable iconRes) {
        MenuItem item = this.mMenu.add(group, id, categoryOrder, title).setIcon(iconRes);
        if (!correctSize(this.mMenu)) {
            return false;
        }
        createNewItem(item, this.mMenuSize - 1);
        return true;
    }

    public boolean addMenu(CharSequence title, Drawable iconRes) {
        return addMenu(0, 0, 0, title, iconRes);
    }

    public boolean addMenu(int titleRes, Drawable iconRes) {
        return addMenu(0, 0, 0, titleRes, iconRes);
    }

    private boolean addMenu(int group, int id, int categoryOrder, int titleRes, Drawable iconRes) {
        MenuItem item = this.mMenu.add(0, 0, 0, titleRes).setIcon(iconRes);
        if (!correctSize(this.mMenu)) {
            return false;
        }
        createNewItem(item, this.mMenuSize - 1);
        return true;
    }

    public void addView(View child, int index, LayoutParams params) {
        if (child instanceof BottomNavigationItemView) {
            super.addView(child, index, params);
        } else {
            Log.w(this.TAG, "illegal to addView by this method");
        }
    }

    public void notifyDotMessage(int index, boolean isHasMessage) {
        if (index < this.mMenuSize) {
            ((BottomNavigationItemView) getChildAt(index)).setHasMessage(isHasMessage);
        }
    }

    public boolean isHasMessage(int index) {
        if (index < this.mMenuSize) {
            return ((BottomNavigationItemView) getChildAt(index)).isHasMessage();
        }
        return false;
    }

    public void setMessageBgColor(int color) {
        this.mMessageBgColor = color;
        for (int i = 0; i < this.mMenuSize; i++) {
            ((BottomNavigationItemView) getChildAt(i)).setMsgBgColor(color);
        }
    }

    private void createNewItem(Menu menu) {
        int size = this.mMenuSize;
        for (int i = 0; i < size; i++) {
            createNewItem(menu.getItem(i), i);
        }
    }

    private void createNewItem(MenuItem menuItem, int index) {
        BottomNavigationItemView itemView = new BottomNavigationItemView(this.mContext, menuItem, isLand(), index);
        itemView.setClickable(true);
        itemView.setBackground(HwWidgetUtils.getHwAnimatedGradientDrawable(this.mContext, this.mClickEffectEntry));
        itemView.setActiveColor(this.mActiveColor);
        itemView.setDefaultColor(this.mDefaultColor);
        itemView.setMsgBgColor(this.mMessageBgColor);
        itemView.setOnClickListener(this.mBottomNavItemClickListenr);
        addView(itemView);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isLand()) {
            onMeasureLand(widthMeasureSpec, heightMeasureSpec);
        } else {
            onMeasurePort(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void onMeasureLand(int widthMeasureSpec, int heightMeasureSpec) {
        int totalWidth = MeasureSpec.getSize(widthMeasureSpec);
        boolean isLayoutRtl = isLayoutRtl();
        int itemHeightSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(), -2);
        int childCount = getChildCount();
        float averageWidth = (float) totalWidth;
        if (childCount != 0) {
            averageWidth = (float) ((totalWidth - this.mCutoutPadding) / childCount);
        }
        int theight = 0;
        int i = 0;
        while (i < childCount) {
            BottomNavigationItemView itemView = (BottomNavigationItemView) getChildAt(i);
            if (this.mNeedFitCutout && 0 == i && !isLayoutRtl && 1 == this.mDisplayRotate) {
                setStableWidth(itemView, ((int) averageWidth) + this.mCutoutPadding);
                itemView.setPadding(itemView.mHorizontalPadding + this.mCutoutPadding, itemView.getPaddingTop(), itemView.getPaddingRight(), itemView.getPaddingBottom());
                itemView.measure(MeasureSpec.makeMeasureSpec(((int) averageWidth) + this.mCutoutPadding, 1073741824), itemHeightSpec);
            } else if (this.mNeedFitCutout && 0 == i && isLayoutRtl && 3 == this.mDisplayRotate) {
                setStableWidth(itemView, ((int) averageWidth) + this.mCutoutPadding);
                itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.mHorizontalPadding + this.mCutoutPadding, itemView.getPaddingBottom());
                itemView.measure(MeasureSpec.makeMeasureSpec(((int) averageWidth) + this.mCutoutPadding, 1073741824), itemHeightSpec);
            } else {
                setStableWidth(itemView, (int) averageWidth);
                itemView.setPadding(itemView.mHorizontalPadding, itemView.getPaddingTop(), itemView.mHorizontalPadding, itemView.getPaddingBottom());
                itemView.measure(MeasureSpec.makeMeasureSpec((int) averageWidth, 1073741824), itemHeightSpec);
            }
            setMarginHorizontal(itemView.getContainer(), 0, 0);
            int height = itemView.getMeasuredHeight();
            if (height > theight) {
                theight = height;
            }
            i++;
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(totalWidth, 1073741824), MeasureSpec.makeMeasureSpec(theight, 1073741824));
    }

    /* JADX WARNING: Removed duplicated region for block: B:79:0x015e A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x0159  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x0159  */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x015e A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onMeasurePort(int widthMeasureSpec, int heightMeasureSpec) {
        List<Float> dataList = new ArrayList();
        int theight = 0;
        int totalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int childCount = getChildCount();
        if (childCount == 0) {
            setMeasuredDimension(totalWidth, 0);
            return;
        }
        int i;
        BottomNavigationItemView itemView;
        float averageWidth = (float) (totalWidth / childCount);
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int itemHeightSpec = getChildMeasureSpec(heightMeasureSpec, heightPadding, -2);
        int i2;
        List<Float> list;
        if (childCount == 2) {
            i2 = heightPadding;
        } else if (childCount == 1) {
            list = dataList;
            i2 = heightPadding;
        } else {
            for (i = 0; i < childCount; i++) {
                generateOffsetList(dataList, i, averageWidth);
            }
            i = 0;
            theight = 0;
            int i3 = 0;
            while (i3 < childCount) {
                BottomNavigationItemView itemView2 = (BottomNavigationItemView) getChildAt(i3);
                float current = ((Float) dataList.get(i3)).floatValue();
                if (current < 0.0f) {
                    int layoutParams;
                    int correctWidth;
                    ImageView icon = itemView2.getIcon();
                    ViewGroup.LayoutParams layoutParams2 = icon.getLayoutParams();
                    if (layoutParams2 instanceof LayoutParams) {
                        LayoutParams iconLayoutParams = (LayoutParams) layoutParams2;
                        iconLayoutParams.gravity = 1;
                        layoutParams = 0;
                        setMarginHorizontal(icon, 0, 0, iconLayoutParams);
                    } else {
                        layoutParams = 0;
                    }
                    setMarginHorizontal(itemView2.getContainer(), layoutParams, layoutParams);
                    ImageView imageView;
                    if (i3 == 0) {
                        i2 = heightPadding;
                        imageView = icon;
                        heightPadding = 1073741824;
                    } else if (i3 == childCount - 1) {
                        i2 = heightPadding;
                        imageView = icon;
                        heightPadding = 1073741824;
                    } else {
                        float startData = ((Float) dataList.get(i3 - 1)).floatValue();
                        float endData = ((Float) dataList.get(i3 + 1)).floatValue();
                        float f;
                        float f2;
                        if (startData < 0.0f) {
                            f = startData;
                            i2 = heightPadding;
                            f2 = endData;
                            imageView = icon;
                        } else if (endData < 0.0f) {
                            f = startData;
                            i2 = heightPadding;
                            f2 = endData;
                            imageView = icon;
                        } else {
                            startData = startData > endData ? endData : startData;
                            i2 = heightPadding;
                            BottomNavigationItemView heightPadding2 = (BottomNavigationItemView) getChildAt(i3 - 1);
                            BottomNavigationItemView rightItem = (BottomNavigationItemView) getChildAt(i3 + 1);
                            if (startData + (current / 2.0f) > 0.0f) {
                                itemView2.measure(MeasureSpec.makeMeasureSpec((int) (averageWidth - current), 1073741824), itemHeightSpec);
                                heightPadding2.mEndRent = (-current) / 2.0f;
                                rightItem.mStartRent = (-current) / 2.0f;
                                correctWidth = (int) (averageWidth - current);
                            } else {
                                imageView = icon;
                                itemView2.measure(MeasureSpec.makeMeasureSpec((int) ((2.0f * startData) + averageWidth), 1073741824), itemHeightSpec);
                                heightPadding2.mEndRent = startData;
                                rightItem.mStartRent = startData;
                                correctWidth = (int) ((2.0f * startData) + averageWidth);
                            }
                            itemView2.mHasMeasured = true;
                            setStableWidth(itemView2, correctWidth);
                            theight += itemView2.getMeasuredWidth();
                            layoutParams = itemView2.getMeasuredHeight();
                            if (layoutParams > i) {
                                i = layoutParams;
                            }
                        }
                        itemView2.measure(MeasureSpec.makeMeasureSpec((int) averageWidth, 1073741824), itemHeightSpec);
                        correctWidth = (int) averageWidth;
                        itemView2.mHasMeasured = true;
                        setStableWidth(itemView2, correctWidth);
                        theight += itemView2.getMeasuredWidth();
                        layoutParams = itemView2.getMeasuredHeight();
                        if (layoutParams > i) {
                        }
                    }
                    itemView2.measure(MeasureSpec.makeMeasureSpec((int) averageWidth, heightPadding), itemHeightSpec);
                    correctWidth = (int) averageWidth;
                    itemView2.mHasMeasured = true;
                    setStableWidth(itemView2, correctWidth);
                    theight += itemView2.getMeasuredWidth();
                    layoutParams = itemView2.getMeasuredHeight();
                    if (layoutParams > i) {
                    }
                } else {
                    i2 = heightPadding;
                }
                i3++;
                heightPadding = i2;
                int i4 = heightMeasureSpec;
            }
            i3 = 0;
            while (i3 < childCount) {
                itemView = (BottomNavigationItemView) getChildAt(i3);
                if (itemView.mHasMeasured) {
                    itemView.mHasMeasured = false;
                    list = dataList;
                } else {
                    float current2 = ((Float) dataList.get(i3)).floatValue();
                    setMarginHorizontal(itemView.getContainer(), (int) (current2 - itemView.mStartRent), (int) (current2 - itemView.mEndRent));
                    ImageView icon2 = itemView.getIcon();
                    ViewGroup.LayoutParams layoutParams3 = icon2.getLayoutParams();
                    if (layoutParams3 instanceof LayoutParams) {
                        LayoutParams iconLayoutParams2 = (LayoutParams) layoutParams3;
                        iconLayoutParams2.gravity = 0;
                        list = dataList;
                        setMarginHorizontal(icon2, (int) (((averageWidth - ((float) this.mIconBounds)) / 2.0f) - itemView.mStartRent), (int) (((averageWidth - ((float) this.mIconBounds)) / 2.0f) - itemView.mEndRent), iconLayoutParams2);
                    } else {
                        list = dataList;
                        float f3 = current2;
                    }
                    itemView.measure(MeasureSpec.makeMeasureSpec((int) ((averageWidth - itemView.mStartRent) - itemView.mEndRent), 1073741824), itemHeightSpec);
                    setStableWidth(itemView, (int) ((averageWidth - itemView.mStartRent) - itemView.mEndRent));
                    itemView.mStartRent = 0.0f;
                    itemView.mEndRent = 0.0f;
                    theight += itemView.getMeasuredWidth();
                    heightPadding = itemView.getMeasuredHeight();
                    if (heightPadding > i) {
                        i = heightPadding;
                    }
                }
                i3++;
                dataList = list;
            }
            super.onMeasure(MeasureSpec.makeMeasureSpec(theight, 1073741824), MeasureSpec.makeMeasureSpec(i, 1073741824));
            return;
        }
        int i5 = 0;
        while (true) {
            int i6 = i5;
            if (i6 < childCount) {
                itemView = (BottomNavigationItemView) getChildAt(i6);
                itemView.measure(MeasureSpec.makeMeasureSpec((int) averageWidth, 1073741824), itemHeightSpec);
                setStableWidth(itemView, (int) averageWidth);
                LinearLayout container = itemView.getContainer();
                ViewGroup.LayoutParams layoutParams4 = container.getLayoutParams();
                if (layoutParams4 instanceof LayoutParams) {
                    LayoutParams containerLayoutParams = (LayoutParams) layoutParams4;
                    containerLayoutParams.gravity = 1;
                    container.setLayoutParams(containerLayoutParams);
                }
                i = itemView.getMeasuredHeight();
                if (i > theight) {
                    theight = i;
                }
                i5 = i6 + 1;
            } else {
                super.onMeasure(MeasureSpec.makeMeasureSpec(totalWidth, 1073741824), MeasureSpec.makeMeasureSpec(theight, 1073741824));
                return;
            }
        }
    }

    private void generateOffsetList(List<Float> dataList, int index, float standardData) {
        float size = standardData - (Layout.getDesiredWidth(this.mMenu.getItem(index).getTitle(), ((BottomNavigationItemView) getChildAt(index)).getContent().getPaint()) + ((float) (2 * this.mTextPadding)));
        if (size > 0.0f) {
            dataList.add(Float.valueOf(size / 2.0f));
        } else {
            dataList.add(Float.valueOf(size));
        }
    }

    private void setMarginHorizontal(View view, int marginStart, int marginEnd) {
        setMarginHorizontal(view, marginStart, marginEnd, (MarginLayoutParams) view.getLayoutParams());
    }

    private void setMarginHorizontal(View view, int marginStart, int marginEnd, MarginLayoutParams layoutParams) {
        layoutParams.setMarginStart(marginStart);
        layoutParams.setMarginEnd(marginEnd);
        view.setLayoutParams(layoutParams);
    }

    private void setStableWidth(View view, int width) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = width;
        view.setLayoutParams(layoutParams);
    }

    public void setBottemNavListener(BottemNavListener listener) {
        this.mListener = listener;
    }

    public void setItemChecked(int index) {
        int childCount = getChildCount();
        if (index >= 0 && index < childCount) {
            BottomNavigationItemView view = (BottomNavigationItemView) getChildAt(index);
            view.setChecked(true, this.mLastSelectItem != -1);
            changeItem(view, false);
        }
    }

    public void removeMenuItems() {
        this.mLastSelectItem = -1;
        this.mMenu.clear();
        this.mMenuSize = 0;
        removeAllViews();
    }

    private void changeItem(BottomNavigationItemView itemView, boolean isClick) {
        int index = itemView.getItemIndex();
        if (index == this.mLastSelectItem && this.mListener != null) {
            this.mListener.onBottemNavItemReselected(this.mMenu.getItem(index), index);
        } else if (index != this.mLastSelectItem) {
            if (this.mLastSelectItem < this.mMenuSize && this.mLastSelectItem >= 0) {
                ((BottomNavigationItemView) getChildAt(this.mLastSelectItem)).setChecked(false, true);
                if (this.mListener != null) {
                    this.mListener.onBottemNavItemUnselected(this.mMenu.getItem(this.mLastSelectItem), this.mLastSelectItem);
                }
            }
            this.mLastSelectItem = index;
            if (isClick) {
                itemView.setChecked(true, true);
            }
            if (this.mListener != null) {
                this.mListener.onBottemNavItemSelected(this.mMenu.getItem(this.mLastSelectItem), this.mLastSelectItem);
            }
        }
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
