package huawei.android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.WindowInsets;
import android.widget.ActionMenuPresenter;
import android.widget.ActionMenuView;
import android.widget.ActionMenuView.LayoutParams;
import android.widget.TextView;
import huawei.android.widget.loader.ResLoader;
import huawei.android.widget.loader.ResLoaderUtil;
import huawei.com.android.internal.view.menu.HwToolbarMenuItemView;
import java.util.ArrayList;

public class HwToolbarMenuView extends ActionMenuView implements HwSmartColorListener {
    private static final boolean DEBUG = true;
    private static final int SPLIT_MENU_AVERAGE_ITEM_NUM = 4;
    private static final String TAG = "HwToolbarMenuView";
    private int mActionBarMenuItemPadding;
    private int mActionBarMenuItemSize;
    private int mActionBarMenuPadding;
    private int mActionBarOverFlowBtnEndPadding;
    private int mActionBarOverFlowBtnSize;
    private int mActionBarOverFlowBtnStartPadding;
    private boolean mHasOverFlowBtnAtActionBar;
    private boolean mHasVisibleChildAtActionBar;
    private HwCutoutUtil mHwCutoutUtil;
    private int mMinHeight;
    private ActionMenuPresenter mPresenter;
    private ResLoader mResLoader;
    private Parcelable mSavedState;
    private ColorStateList mSmartIconColor;
    private ColorStateList mSmartTitleColor;
    private int mSplitMenuDrawablePadding;
    private int mSplitMenuHeightPadding;
    private int mSplitMenuMinTextSize;
    private int mSplitMenuNormalTextSize;
    private int mSplitMenuTextStep;
    private int mSplitMenuTopMargin;
    private int mSplitMenuTotalWidth;
    private int mSplitToolbarMenuItemPadding;
    private int mSplitToolbarMenuItemPaddingLess;

    private static class ItemSpec {
        int endPadding;
        int itemSize;
        int measuredSize;
        int startPadding;
        boolean wasTooLong;

        private ItemSpec() {
        }
    }

    public HwToolbarMenuView(Context context) {
        this(context, null);
    }

    public HwToolbarMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMinHeight = 0;
        this.mHasVisibleChildAtActionBar = false;
        this.mHasOverFlowBtnAtActionBar = false;
        this.mHwCutoutUtil = null;
        Log.d(TAG, "new HwToolbarMenuView");
        this.mResLoader = ResLoader.getInstance();
        this.mMinHeight = this.mResLoader.getResources(context).getDimensionPixelSize(this.mResLoader.getIdentifier(context, ResLoaderUtil.DIMEN, "hwtoolbar_split_menu_height"));
        initSize(context);
        this.mHwCutoutUtil = new HwCutoutUtil();
    }

    public void setPresenter(ActionMenuPresenter presenter) {
        super.setPresenter(presenter);
        this.mPresenter = presenter;
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mHwCutoutUtil.checkCutoutStatus(insets, this, this.mContext);
        return super.onApplyWindowInsets(insets);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (((View) getParent()).getId() == 16909357) {
            onMeasureAtSplitView(widthMeasureSpec, heightMeasureSpec);
            setPadding(0, 0, 0, 0);
            return;
        }
        onMeasureAtActionBar(heightMeasureSpec);
        if (this.mHasVisibleChildAtActionBar) {
            int padding = this.mHasOverFlowBtnAtActionBar ? this.mActionBarOverFlowBtnEndPadding : this.mActionBarMenuPadding;
            if (isLayoutRtl()) {
                setPadding(padding, 0, 0, 0);
                return;
            } else {
                setPadding(0, 0, padding, 0);
                return;
            }
        }
        setPadding(0, 0, 0, 0);
    }

    protected void onMeasureAtActionBar(int heightMeasureSpec) {
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthPadding = this.mActionBarMenuPadding;
        int itemWidthSpec = MeasureSpec.makeMeasureSpec(this.mActionBarMenuItemSize, 1073741824);
        int itemHeightSpec = MeasureSpec.makeMeasureSpec(this.mActionBarMenuItemSize, 1073741824);
        int childCount = getChildCount();
        int totalWidth = 0;
        int i = 0;
        this.mHasVisibleChildAtActionBar = false;
        int itemWidthSpec2 = itemWidthSpec;
        for (itemWidthSpec = 0; itemWidthSpec < childCount; itemWidthSpec++) {
            View child = getChildAt(itemWidthSpec);
            if (child.getVisibility() != 8) {
                child.setPadding(0, child.getPaddingTop(), 0, child.getPaddingBottom());
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.isOverflowButton) {
                    itemWidthSpec2 = MeasureSpec.makeMeasureSpec(this.mActionBarOverFlowBtnSize, 1073741824);
                }
                child.measure(itemWidthSpec2, itemHeightSpec);
                totalWidth += child.getMeasuredWidth();
                if (itemWidthSpec < childCount - 1) {
                    if (isNextItemOverFlowBtn(itemWidthSpec)) {
                        totalWidth += this.mActionBarOverFlowBtnStartPadding;
                    } else {
                        totalWidth += this.mActionBarMenuItemPadding;
                    }
                }
                if (itemWidthSpec == childCount - 1 && lp.isOverflowButton) {
                    this.mHasOverFlowBtnAtActionBar = DEBUG;
                }
                this.mHasVisibleChildAtActionBar = DEBUG;
            }
        }
        itemWidthSpec = (this.mHasOverFlowBtnAtActionBar ? this.mActionBarOverFlowBtnEndPadding : widthPadding) + totalWidth;
        if (this.mHasVisibleChildAtActionBar) {
            i = itemWidthSpec;
        }
        setMeasuredDimension(i, heightSize);
    }

    protected void onMeasureAtSplitView(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int widthPadding = this.mSplitToolbarMenuItemPadding * 2;
        int itemHeightSpec = getChildMeasureSpec(heightMeasureSpec, heightPadding, -1);
        int menuHeightPadding = this.mSplitMenuHeightPadding * 2;
        int childCount = getChildCount();
        int childTotalWidth = widthSize - (childCount * widthPadding);
        int visibleItemCount = 0;
        for (i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            if (v.getVisibility() != 8) {
                v.setPadding(0, 0, 0, 0);
                visibleItemCount++;
            }
        }
        if (visibleItemCount <= 0) {
            setMeasuredDimension(widthSize, 0);
            return;
        }
        TextView textChild;
        int requiredWidth;
        int childCount2;
        ItemSpec itemSpec;
        int requireCellSize;
        int maxCellSize;
        ArrayList<ItemSpec> itemSpecs;
        ArrayList<TextView> textChilds;
        int visibleItemCount2;
        ArrayList<TextView> textChilds2 = new ArrayList();
        ArrayList<ItemSpec> itemSpecs2 = new ArrayList();
        int maxCellSize2 = childTotalWidth / visibleItemCount;
        childTotalWidth -= this.mHwCutoutUtil.getCutoutPadding();
        if (visibleItemCount > 4) {
            i = childTotalWidth / visibleItemCount;
        } else {
            childTotalWidth -= this.mSplitToolbarMenuItemPaddingLess * 2;
            i = childTotalWidth / 4;
        }
        childTotalWidth = i;
        int requireCellSize2 = i;
        i = 0;
        while (i < childCount) {
            int heightPadding2;
            View child = getChildAt(i);
            if (child instanceof TextView) {
                heightPadding2 = heightPadding;
                if (child.getVisibility() != 8) {
                    textChild = (TextView) child;
                    textChilds2.add(textChild);
                    heightPadding = new ItemSpec();
                    heightPadding.itemSize = childTotalWidth;
                    textChild.measure(0, itemHeightSpec);
                    requiredWidth = textChild.getMeasuredWidth();
                    childCount2 = childCount;
                    childCount = requireCellSize2;
                    if (requiredWidth > childCount) {
                        childCount = requiredWidth > maxCellSize2 ? maxCellSize2 : requiredWidth;
                    }
                    heightPadding.measuredSize = requiredWidth;
                    heightPadding.endPadding = 0;
                    heightPadding.startPadding = 0;
                    heightPadding.wasTooLong = requiredWidth > heightPadding.itemSize ? DEBUG : false;
                    itemSpecs2.add(heightPadding);
                } else {
                    childCount2 = childCount;
                    childCount = requireCellSize2;
                }
            } else {
                heightPadding2 = heightPadding;
                childCount2 = childCount;
                childCount = requireCellSize2;
            }
            requireCellSize2 = childCount;
            i++;
            heightPadding = heightPadding2;
            childCount = childCount2;
            requiredWidth = heightMeasureSpec;
        }
        childCount2 = childCount;
        childCount = requireCellSize2;
        if (childCount > childTotalWidth) {
            heightPadding = itemSpecs2.size();
            for (requiredWidth = 0; requiredWidth < heightPadding; requiredWidth++) {
                itemSpec = (ItemSpec) itemSpecs2.get(requiredWidth);
                itemSpec.itemSize = childCount;
                itemSpec.wasTooLong = itemSpec.measuredSize > childCount ? DEBUG : false;
            }
        }
        heightPadding = textChilds2.size();
        requiredWidth = this.mMinHeight;
        i = 0;
        while (i < heightPadding) {
            if (itemSpecs2.get(i) != null && ((ItemSpec) itemSpecs2.get(i)).wasTooLong) {
                adjustSize(i, itemSpecs2);
            }
            i++;
        }
        this.mSplitMenuTotalWidth = 0;
        int maxItemHeight = requiredWidth;
        requiredWidth = 0;
        while (requiredWidth < heightPadding) {
            int textChildCount;
            int cellSize;
            int maxCellSize3;
            textChild = (TextView) textChilds2.get(requiredWidth);
            itemSpec = (ItemSpec) itemSpecs2.get(requiredWidth);
            if (isLayoutRtl()) {
                textChildCount = heightPadding;
                cellSize = childTotalWidth;
                maxCellSize3 = maxCellSize2;
                textChild.setPadding(itemSpec.endPadding, 0, itemSpec.startPadding, 0);
            } else {
                textChildCount = heightPadding;
                cellSize = childTotalWidth;
                maxCellSize3 = maxCellSize2;
                textChild.setPadding(itemSpec.startPadding, 0, itemSpec.endPadding, 0);
            }
            textChild.setCompoundDrawablePadding(this.mSplitMenuDrawablePadding);
            heightPadding = this.mSplitMenuNormalTextSize;
            childTotalWidth = this.mSplitMenuMinTextSize;
            maxCellSize2 = this.mSplitMenuTextStep;
            ArrayList<ItemSpec> itemSpecs3 = itemSpecs2;
            int i2 = itemSpec.itemSize;
            requireCellSize = childCount;
            ItemSpec itemSpec2 = itemSpec;
            i = heightPadding;
            heightPadding = cellSize;
            TextView textItemView = textChild;
            maxCellSize = maxCellSize3;
            itemSpecs = itemSpecs3;
            textChilds = textChilds2;
            visibleItemCount2 = visibleItemCount;
            AutoTextUtils.autoText(i, childTotalWidth, maxCellSize2, 1, i2, itemHeightSpec, textItemView);
            TextView textItemView2 = textItemView;
            textItemView2.measure(MeasureSpec.makeMeasureSpec(itemSpec2.itemSize, 1073741824), itemHeightSpec);
            this.mSplitMenuTotalWidth += itemSpec2.itemSize + widthPadding;
            i = (textItemView2.getMeasuredHeight() + menuHeightPadding) + this.mSplitMenuTopMargin;
            maxItemHeight = maxItemHeight > i ? maxItemHeight : i;
            requiredWidth++;
            childTotalWidth = heightPadding;
            itemSpecs2 = itemSpecs;
            maxCellSize2 = maxCellSize;
            textChilds2 = textChilds;
            visibleItemCount = visibleItemCount2;
            heightPadding = textChildCount;
            childCount = requireCellSize;
        }
        maxCellSize = maxCellSize2;
        itemSpecs = itemSpecs2;
        textChilds = textChilds2;
        visibleItemCount2 = visibleItemCount;
        requireCellSize = childCount;
        setMeasuredDimension(widthSize, maxItemHeight);
    }

    private boolean isNextItemOverFlowBtn(int itemnum) {
        int childCount = getChildCount();
        if (childCount >= 2 && itemnum < childCount - 1 && ((LayoutParams) getChildAt(itemnum + 1).getLayoutParams()).isOverflowButton) {
            return DEBUG;
        }
        return false;
    }

    private void adjustSize(int index, ArrayList<ItemSpec> items) {
        String str;
        StringBuilder stringBuilder;
        if (index >= items.size() - 1 || index <= 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("adjustSize: the item index >= count - 1 or <= 0 and index is ");
            stringBuilder.append(index);
            Log.i(str, stringBuilder.toString());
        } else if (((ItemSpec) items.get(index - 1)).wasTooLong || ((ItemSpec) items.get(index + 1)).wasTooLong) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("adjustSize: ");
            stringBuilder.append(index);
            stringBuilder.append(" the pre item was too long or the next item was too long");
            Log.i(str, stringBuilder.toString());
        } else {
            ItemSpec preItem = (ItemSpec) items.get(index - 1);
            ItemSpec nextItem = (ItemSpec) items.get(index + 1);
            ItemSpec currentItem = (ItemSpec) items.get(index);
            int diff = currentItem.measuredSize - currentItem.itemSize;
            int preRemainderEnd = (((preItem.itemSize - preItem.measuredSize) - preItem.endPadding) / 2) + preItem.endPadding;
            int nextRemainderStart = (nextItem.itemSize - nextItem.measuredSize) / 2;
            int minRemainder = preRemainderEnd < nextRemainderStart ? preRemainderEnd : nextRemainderStart;
            int harfDiff = diff / 2;
            harfDiff = harfDiff < minRemainder ? harfDiff : minRemainder;
            preItem.itemSize -= harfDiff;
            if (harfDiff < preItem.endPadding) {
                preItem.endPadding -= harfDiff;
            } else {
                preItem.startPadding = harfDiff - preItem.endPadding;
                preItem.endPadding = 0;
            }
            currentItem.itemSize += harfDiff * 2;
            nextItem.itemSize -= harfDiff;
            nextItem.endPadding = harfDiff;
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int i;
        int childCount;
        boolean isLayoutRtl;
        int cutoutRightPadding;
        int cutoutLeftPadding;
        int cutoutPadding;
        int childCount2 = getChildCount();
        boolean isLayoutRtl2 = isLayoutRtl();
        int cutoutRightPadding2 = 0;
        int cutoutLeftPadding2 = 0;
        int cutoutPadding2 = 0;
        boolean isToolBar = getParent() != null ? ((View) getParent()).getId() == 16909357 ? DEBUG : false : this.mContext.getResources().getConfiguration().orientation == 1 ? DEBUG : false;
        if (this.mHwCutoutUtil != null && this.mHwCutoutUtil.getNeedFitCutout()) {
            if (1 == this.mHwCutoutUtil.getDisplayRotate()) {
                cutoutLeftPadding2 = this.mHwCutoutUtil.getCutoutPadding();
                cutoutPadding2 = this.mHwCutoutUtil.getCutoutPadding();
            } else if (3 == this.mHwCutoutUtil.getDisplayRotate()) {
                cutoutRightPadding2 = this.mHwCutoutUtil.getCutoutPadding();
                cutoutPadding2 = this.mHwCutoutUtil.getCutoutPadding();
            }
        }
        int menuViewWidth = getMeasuredWidth();
        int start = ((menuViewWidth - this.mSplitMenuTotalWidth) - cutoutPadding2) / 2;
        int startLeft = isToolBar ? start + cutoutLeftPadding2 : 0;
        int startRight = isToolBar ? (menuViewWidth - start) - cutoutRightPadding2 : menuViewWidth;
        int i2 = 0;
        while (i2 < childCount2) {
            View v = getChildAt(i2);
            if (v.getVisibility() == 8) {
                i = top;
                childCount = childCount2;
                isLayoutRtl = isLayoutRtl2;
                cutoutRightPadding = cutoutRightPadding2;
                cutoutLeftPadding = cutoutLeftPadding2;
                cutoutPadding = cutoutPadding2;
            } else {
                int itemPadding = getItemPadding(isToolBar, i2);
                int itemPaddingStart = isToolBar ? this.mSplitToolbarMenuItemPadding : 0;
                int width = v.getMeasuredWidth();
                int height = v.getMeasuredHeight();
                childCount = childCount2;
                cutoutRightPadding = cutoutRightPadding2;
                cutoutRightPadding2 = getLayoutTop(isToolBar, top, bottom, v);
                childCount2 = v.getPaddingLeft();
                cutoutLeftPadding = cutoutLeftPadding2;
                cutoutPadding = cutoutPadding2;
                v.setPadding(childCount2, cutoutRightPadding2, v.getPaddingRight(), v.getPaddingBottom());
                if (isLayoutRtl2) {
                    startRight -= itemPaddingStart;
                    isLayoutRtl = isLayoutRtl2;
                    v.layout(startRight - width, 0, startRight, getMeasuredHeight());
                    startRight -= width + itemPadding;
                } else {
                    isLayoutRtl = isLayoutRtl2;
                    int i3 = cutoutRightPadding2;
                    startLeft += itemPaddingStart;
                    v.layout(startLeft, 0, startLeft + width, getMeasuredHeight());
                    startLeft += width + itemPadding;
                }
            }
            i2++;
            childCount2 = childCount;
            cutoutRightPadding2 = cutoutRightPadding;
            cutoutLeftPadding2 = cutoutLeftPadding;
            cutoutPadding2 = cutoutPadding;
            isLayoutRtl2 = isLayoutRtl;
        }
        i = top;
        childCount = childCount2;
        isLayoutRtl = isLayoutRtl2;
        cutoutRightPadding = cutoutRightPadding2;
        cutoutLeftPadding = cutoutLeftPadding2;
        cutoutPadding = cutoutPadding2;
    }

    private int getLayoutTop(boolean isToolbar, int top, int bottom, View child) {
        int toolbarHeight = bottom - top;
        if (isToolbar && toolbarHeight > this.mMinHeight) {
            return this.mSplitMenuHeightPadding + this.mSplitMenuTopMargin;
        }
        if (isToolbar) {
            toolbarHeight -= this.mSplitMenuTopMargin;
        }
        int midVertical = (toolbarHeight / 2) - (child.getMeasuredHeight() / 2);
        if (isToolbar) {
            midVertical += this.mSplitMenuTopMargin;
        }
        return midVertical;
    }

    private int getItemPadding(boolean isToolBar, int itemNum) {
        if (isToolBar) {
            return this.mSplitToolbarMenuItemPadding;
        }
        if (isNextItemOverFlowBtn(itemNum)) {
            return this.mActionBarOverFlowBtnStartPadding;
        }
        return this.mActionBarMenuItemPadding;
    }

    protected boolean hasDividerBeforeChildAt(int childIndex) {
        return false;
    }

    public void onDetachedFromWindow() {
        if ((this.mPresenter instanceof HwActionMenuPresenter) && (((HwActionMenuPresenter) this.mPresenter).isPopupMenuShowing() || this.mPresenter.isOverflowMenuShowing())) {
            this.mSavedState = this.mPresenter.onSaveInstanceState();
        }
        if (this.mPresenter != null) {
            this.mPresenter.dismissPopupMenus();
        }
        super.onDetachedFromWindow();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if ((this.mPresenter instanceof HwActionMenuPresenter) && this.mSavedState != null) {
            if (!((HwActionMenuPresenter) this.mPresenter).isPopupMenuShowing() || !this.mPresenter.isOverflowMenuShowing()) {
                this.mPresenter.onRestoreInstanceState(this.mSavedState);
                this.mSavedState = null;
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (this.mPresenter != null) {
            this.mPresenter.updateMenuView(false);
            if (this.mPresenter.isOverflowMenuShowing()) {
                this.mPresenter.hideOverflowMenu();
                this.mPresenter.showOverflowMenuPending();
            }
        }
        this.mMinHeight = this.mResLoader.getResources(this.mContext).getDimensionPixelSize(this.mResLoader.getIdentifier(this.mContext, ResLoaderUtil.DIMEN, "hwtoolbar_split_menu_height"));
    }

    private void initSize(Context context) {
        Context context2 = context;
        Resources res = this.mResLoader.getResources(context2);
        int menuItemPaddingId = this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_menuitem_padding");
        this.mActionBarMenuItemPadding = res.getDimensionPixelSize(menuItemPaddingId);
        this.mActionBarMenuItemSize = res.getDimensionPixelSize(this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_menuitem_size"));
        this.mActionBarOverFlowBtnSize = res.getDimensionPixelSize(this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_overflowbtn_size"));
        this.mActionBarMenuPadding = res.getDimensionPixelSize(this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_menu_padding"));
        this.mActionBarOverFlowBtnStartPadding = res.getDimensionPixelSize(this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_overflowbtn_start_padding"));
        this.mActionBarOverFlowBtnEndPadding = res.getDimensionPixelSize(this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_overflowbtn_end_padding"));
        this.mSplitToolbarMenuItemPadding = res.getDimensionPixelSize(this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_split_menuitem_padding"));
        this.mSplitToolbarMenuItemPaddingLess = res.getDimensionPixelSize(this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_split_menuitem_padding_less"));
        this.mSplitMenuMinTextSize = res.getInteger(this.mResLoader.getIdentifier(context2, "integer", "hwtoolbar_split_menu_min_textsize"));
        this.mSplitMenuTextStep = res.getInteger(this.mResLoader.getIdentifier(context2, "integer", "hwtoolbar_split_menu_textsize_step"));
        this.mSplitMenuNormalTextSize = res.getInteger(this.mResLoader.getIdentifier(context2, "integer", "hwtoolbar_split_menu_normal_textsize"));
        menuItemPaddingId = this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_split_menu_height_padding");
        this.mSplitMenuHeightPadding = res.getDimensionPixelSize(menuItemPaddingId);
        menuItemPaddingId = this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_split_menu_drawable_padding");
        this.mSplitMenuDrawablePadding = res.getDimensionPixelSize(menuItemPaddingId);
        this.mSplitMenuTopMargin = res.getDimensionPixelSize(this.mResLoader.getIdentifier(context2, ResLoaderUtil.DIMEN, "hwtoolbar_split_menuitem_top_margin"));
    }

    public void onSetSmartColor(ColorStateList iconColor, ColorStateList titleColor) {
        this.mSmartIconColor = iconColor;
        this.mSmartTitleColor = titleColor;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof HwToolbarMenuItemView) {
                ((HwToolbarMenuItemView) child).updateTextAndIcon();
            }
            if (child instanceof HwOverflowMenuButton) {
                ((HwOverflowMenuButton) child).updateTextAndIcon();
            }
        }
    }

    public ColorStateList getSmartIconColor() {
        return this.mSmartIconColor;
    }

    public ColorStateList getSmartTitleColor() {
        return this.mSmartTitleColor;
    }
}
