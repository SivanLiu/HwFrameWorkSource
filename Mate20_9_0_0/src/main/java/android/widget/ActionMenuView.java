package android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewDebug.ExportedProperty;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.view.menu.ActionMenuItemView;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuBuilder.ItemInvoker;
import com.android.internal.view.menu.MenuItemImpl;
import com.android.internal.view.menu.MenuPresenter.Callback;
import com.android.internal.view.menu.MenuView;

public class ActionMenuView extends LinearLayout implements ItemInvoker, MenuView {
    static final int GENERATED_ITEM_PADDING = 4;
    static final int MIN_CELL_SIZE = 56;
    private static final String TAG = "ActionMenuView";
    private Callback mActionMenuPresenterCallback;
    private boolean mFormatItems;
    private int mFormatItemsWidth;
    private int mGeneratedItemPadding;
    private MenuBuilder mMenu;
    private MenuBuilder.Callback mMenuBuilderCallback;
    private int mMinCellSize;
    private OnMenuItemClickListener mOnMenuItemClickListener;
    private Context mPopupContext;
    private int mPopupTheme;
    private ActionMenuPresenter mPresenter;
    private boolean mReserveOverflow;

    public interface ActionMenuChildView {
        boolean needsDividerAfter();

        boolean needsDividerBefore();
    }

    public interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem menuItem);
    }

    private class ActionMenuPresenterCallback implements Callback {
        private ActionMenuPresenterCallback() {
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            return false;
        }
    }

    private class MenuBuilderCallback implements MenuBuilder.Callback {
        private MenuBuilderCallback() {
        }

        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            return ActionMenuView.this.mOnMenuItemClickListener != null && ActionMenuView.this.mOnMenuItemClickListener.onMenuItemClick(item);
        }

        public void onMenuModeChange(MenuBuilder menu) {
            if (ActionMenuView.this.mMenuBuilderCallback != null) {
                ActionMenuView.this.mMenuBuilderCallback.onMenuModeChange(menu);
            }
        }
    }

    public static class LayoutParams extends android.widget.LinearLayout.LayoutParams {
        @ExportedProperty(category = "layout")
        public int cellsUsed;
        @ExportedProperty(category = "layout")
        public boolean expandable;
        public boolean expanded;
        @ExportedProperty(category = "layout")
        public int extraPixels;
        @ExportedProperty(category = "layout")
        public boolean isOverflowButton;
        @ExportedProperty(category = "layout")
        public boolean preventEdgeOffset;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams other) {
            super(other);
        }

        public LayoutParams(LayoutParams other) {
            super((android.widget.LinearLayout.LayoutParams) other);
            this.isOverflowButton = other.isOverflowButton;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.isOverflowButton = false;
        }

        public LayoutParams(int width, int height, boolean isOverflowButton) {
            super(width, height);
            this.isOverflowButton = isOverflowButton;
        }

        protected void encodeProperties(ViewHierarchyEncoder encoder) {
            super.encodeProperties(encoder);
            encoder.addProperty("layout:overFlowButton", this.isOverflowButton);
            encoder.addProperty("layout:cellsUsed", this.cellsUsed);
            encoder.addProperty("layout:extraPixels", this.extraPixels);
            encoder.addProperty("layout:expandable", this.expandable);
            encoder.addProperty("layout:preventEdgeOffset", this.preventEdgeOffset);
        }
    }

    public ActionMenuView(Context context) {
        this(context, null);
    }

    public ActionMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBaselineAligned(false);
        float density = context.getResources().getDisplayMetrics().density;
        this.mMinCellSize = (int) (56.0f * density);
        this.mGeneratedItemPadding = (int) (4.0f * density);
        this.mPopupContext = context;
        this.mPopupTheme = 0;
    }

    public void setPopupTheme(int resId) {
        if (this.mPopupTheme != resId) {
            this.mPopupTheme = resId;
            if (resId == 0) {
                this.mPopupContext = this.mContext;
            } else {
                this.mPopupContext = new ContextThemeWrapper(this.mContext, resId);
            }
        }
    }

    public int getPopupTheme() {
        return this.mPopupTheme;
    }

    public void setPresenter(ActionMenuPresenter presenter) {
        this.mPresenter = presenter;
        this.mPresenter.setMenuView(this);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mPresenter != null) {
            this.mPresenter.updateMenuView(false);
            if (this.mPresenter.isOverflowMenuShowing()) {
                this.mPresenter.hideOverflowMenu();
                this.mPresenter.showOverflowMenu();
            }
        }
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.mOnMenuItemClickListener = listener;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean wasFormatted = this.mFormatItems;
        this.mFormatItems = MeasureSpec.getMode(widthMeasureSpec) == 1073741824;
        if (wasFormatted != this.mFormatItems) {
            this.mFormatItemsWidth = 0;
        }
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (!(!this.mFormatItems || this.mMenu == null || widthSize == this.mFormatItemsWidth)) {
            this.mFormatItemsWidth = widthSize;
            this.mMenu.onItemsChanged(true);
        }
        int childCount = getChildCount();
        if (!this.mFormatItems || childCount <= 0) {
            for (int i = 0; i < childCount; i++) {
                LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
                lp.rightMargin = 0;
                lp.leftMargin = 0;
            }
            if (childCount == 0) {
                setMeasuredDimension(0, 0);
                return;
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
        }
        onMeasureExactFormat(widthMeasureSpec, heightMeasureSpec);
    }

    /* JADX WARNING: Removed duplicated region for block: B:139:0x02b5  */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x02ec  */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x02ea  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onMeasureExactFormat(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthPadding = getPaddingLeft() + getPaddingRight();
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int itemHeightSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, heightPadding, -2);
        widthSize -= widthPadding;
        int cellCount = widthSize / this.mMinCellSize;
        int cellSizeRemaining = widthSize % this.mMinCellSize;
        if (cellCount == 0) {
            setMeasuredDimension(widthSize, 0);
            return;
        }
        int cellCount2;
        int cellSizeRemaining2;
        boolean isGeneratedItem;
        LayoutParams lp;
        int i;
        boolean needsExpansion;
        int visibleItemCount;
        View child;
        LayoutParams lp2;
        int i2;
        int cellSize = this.mMinCellSize + (cellSizeRemaining / cellCount);
        int cellsRemaining = cellCount;
        boolean hasOverflow = false;
        long smallestItemsAt = 0;
        int childCount = getChildCount();
        int heightSize2 = heightSize;
        heightSize = 0;
        int visibleItemCount2 = 0;
        int expandableItemCount = 0;
        int maxCellsUsed = 0;
        int cellsRemaining2 = cellsRemaining;
        cellsRemaining = 0;
        while (true) {
            int widthPadding2 = widthPadding;
            if (cellsRemaining >= childCount) {
                break;
            }
            int heightPadding2;
            View child2 = getChildAt(cellsRemaining);
            cellCount2 = cellCount;
            if (child2.getVisibility() == 8) {
                heightPadding2 = heightPadding;
                cellSizeRemaining2 = cellSizeRemaining;
            } else {
                int visibleItemCount3;
                boolean visibleItemCount4;
                isGeneratedItem = child2 instanceof ActionMenuItemView;
                visibleItemCount2++;
                if (isGeneratedItem) {
                    cellSizeRemaining2 = cellSizeRemaining;
                    visibleItemCount3 = visibleItemCount2;
                    visibleItemCount4 = false;
                    child2.setPadding(this.mGeneratedItemPadding, 0, this.mGeneratedItemPadding, 0);
                } else {
                    cellSizeRemaining2 = cellSizeRemaining;
                    visibleItemCount3 = visibleItemCount2;
                    visibleItemCount4 = false;
                }
                lp = (LayoutParams) child2.getLayoutParams();
                lp.expanded = visibleItemCount4;
                lp.extraPixels = visibleItemCount4;
                lp.cellsUsed = visibleItemCount4;
                lp.expandable = visibleItemCount4;
                lp.leftMargin = visibleItemCount4;
                lp.rightMargin = visibleItemCount4;
                boolean z = isGeneratedItem && ((ActionMenuItemView) child2).hasText();
                lp.preventEdgeOffset = z;
                visibleItemCount2 = measureChildForCells(child2, cellSize, lp.isOverflowButton ? 1 : cellsRemaining2, itemHeightSpec, heightPadding);
                maxCellsUsed = Math.max(maxCellsUsed, visibleItemCount2);
                heightPadding2 = heightPadding;
                if (lp.expandable != 0) {
                    expandableItemCount++;
                }
                if (lp.isOverflowButton) {
                    hasOverflow = true;
                }
                cellsRemaining2 -= visibleItemCount2;
                heightSize = Math.max(heightSize, child2.getMeasuredHeight());
                if (visibleItemCount2 == 1) {
                    smallestItemsAt |= (long) (1 << cellsRemaining);
                    visibleItemCount2 = visibleItemCount3;
                    heightSize = heightSize;
                } else {
                    visibleItemCount2 = visibleItemCount3;
                }
            }
            cellsRemaining++;
            widthPadding = widthPadding2;
            cellCount = cellCount2;
            cellSizeRemaining = cellSizeRemaining2;
            heightPadding = heightPadding2;
            i = heightMeasureSpec;
        }
        cellCount2 = cellCount;
        cellSizeRemaining2 = cellSizeRemaining;
        boolean centerSingleExpandedItem = hasOverflow && visibleItemCount2 == 2;
        isGeneratedItem = false;
        while (expandableItemCount > 0 && cellsRemaining2 > 0) {
            long minCellsAt = 0;
            cellCount = Integer.MAX_VALUE;
            widthPadding = 0;
            cellsRemaining = 0;
            while (true) {
                cellSizeRemaining = cellsRemaining;
                if (cellSizeRemaining >= childCount) {
                    break;
                }
                View child3 = getChildAt(cellSizeRemaining);
                needsExpansion = isGeneratedItem;
                LayoutParams needsExpansion2 = (LayoutParams) child3.getLayoutParams();
                if (needsExpansion2.expandable == null) {
                    visibleItemCount = visibleItemCount2;
                } else if (needsExpansion2.cellsUsed < cellCount) {
                    visibleItemCount = visibleItemCount2;
                    widthPadding = 1;
                    minCellsAt = (long) (1 << cellSizeRemaining);
                    cellCount = needsExpansion2.cellsUsed;
                } else {
                    visibleItemCount = visibleItemCount2;
                    if (needsExpansion2.cellsUsed == cellCount) {
                        widthPadding++;
                        minCellsAt |= (long) (1 << cellSizeRemaining);
                    }
                }
                cellsRemaining = cellSizeRemaining + 1;
                isGeneratedItem = needsExpansion;
                visibleItemCount2 = visibleItemCount;
            }
            needsExpansion = isGeneratedItem;
            visibleItemCount = visibleItemCount2;
            smallestItemsAt |= minCellsAt;
            boolean z2;
            if (widthPadding > cellsRemaining2) {
                z2 = centerSingleExpandedItem;
                break;
            }
            cellCount++;
            i = 0;
            while (i < childCount) {
                child = getChildAt(i);
                lp2 = (LayoutParams) child.getLayoutParams();
                int minCellsItemCount = widthPadding;
                int cellsRemaining3 = cellsRemaining2;
                if ((minCellsAt & ((long) (1 << i))) == 0) {
                    if (lp2.cellsUsed == cellCount) {
                        smallestItemsAt |= (long) (1 << i);
                    }
                    z2 = centerSingleExpandedItem;
                    cellsRemaining2 = cellsRemaining3;
                } else {
                    if (centerSingleExpandedItem && lp2.preventEdgeOffset) {
                        cellsRemaining2 = cellsRemaining3;
                        if (cellsRemaining2 == 1) {
                            z2 = centerSingleExpandedItem;
                            child.setPadding(this.mGeneratedItemPadding + cellSize, 0, this.mGeneratedItemPadding, 0);
                        } else {
                            z2 = centerSingleExpandedItem;
                        }
                    } else {
                        z2 = centerSingleExpandedItem;
                        cellsRemaining2 = cellsRemaining3;
                    }
                    lp2.cellsUsed++;
                    lp2.expanded = true;
                    cellsRemaining2--;
                }
                i++;
                widthPadding = minCellsItemCount;
                centerSingleExpandedItem = z2;
            }
            z2 = centerSingleExpandedItem;
            isGeneratedItem = true;
            visibleItemCount2 = visibleItemCount;
        }
        needsExpansion = isGeneratedItem;
        visibleItemCount = visibleItemCount2;
        long smallestItemsAt2 = smallestItemsAt;
        if (hasOverflow) {
            visibleItemCount2 = visibleItemCount;
        } else {
            visibleItemCount2 = visibleItemCount;
            if (visibleItemCount2 == 1) {
                isGeneratedItem = true;
                boolean singleItem;
                if (cellsRemaining2 > 0 || smallestItemsAt2 == 0) {
                    i2 = visibleItemCount2;
                } else if (cellsRemaining2 < visibleItemCount2 - 1 || isGeneratedItem || maxCellsUsed > 1) {
                    float expandCount;
                    float expandCount2 = (float) Long.bitCount(smallestItemsAt2);
                    if (isGeneratedItem) {
                    } else {
                        if (!((smallestItemsAt2 & 1) == 0 || ((LayoutParams) getChildAt(0).getLayoutParams()).preventEdgeOffset)) {
                            expandCount2 -= 0.5f;
                        }
                        if (!((((long) (1 << (childCount - 1))) & smallestItemsAt2) == 0 || ((LayoutParams) getChildAt(childCount - 1).getLayoutParams()).preventEdgeOffset)) {
                            expandCount2 -= 0.5f;
                        }
                    }
                    cellSizeRemaining = expandCount2 > 0.0f ? (int) (((float) (cellsRemaining2 * cellSize)) / expandCount2) : 0;
                    cellsRemaining = 0;
                    while (cellsRemaining < childCount) {
                        singleItem = isGeneratedItem;
                        expandCount = expandCount2;
                        if ((((long) (true << cellsRemaining)) & smallestItemsAt2) != 0) {
                            isGeneratedItem = getChildAt(cellsRemaining);
                            lp = (LayoutParams) isGeneratedItem.getLayoutParams();
                            if (isGeneratedItem instanceof ActionMenuItemView) {
                                lp.extraPixels = cellSizeRemaining;
                                lp.expanded = true;
                                if (cellsRemaining == 0 && !lp.preventEdgeOffset) {
                                    lp.leftMargin = (-cellSizeRemaining) / 2;
                                }
                                needsExpansion = true;
                            } else {
                                if (lp.isOverflowButton) {
                                    lp.extraPixels = cellSizeRemaining;
                                    lp.expanded = true;
                                    lp.rightMargin = (-cellSizeRemaining) / 2;
                                    needsExpansion = true;
                                } else {
                                    if (cellsRemaining != 0) {
                                        lp.leftMargin = cellSizeRemaining / 2;
                                    }
                                    if (cellsRemaining != childCount - 1) {
                                        lp.rightMargin = cellSizeRemaining / 2;
                                    }
                                }
                                cellsRemaining++;
                                isGeneratedItem = singleItem;
                                expandCount2 = expandCount;
                            }
                        }
                        cellsRemaining++;
                        isGeneratedItem = singleItem;
                        expandCount2 = expandCount;
                    }
                    expandCount = expandCount2;
                } else {
                    singleItem = isGeneratedItem;
                    i2 = visibleItemCount2;
                }
                if (needsExpansion) {
                    int i3 = 0;
                    while (true) {
                        cellCount = i3;
                        if (cellCount >= childCount) {
                            break;
                        }
                        long smallestItemsAt3;
                        child = getChildAt(cellCount);
                        lp2 = (LayoutParams) child.getLayoutParams();
                        if (lp2.expanded) {
                            smallestItemsAt3 = smallestItemsAt2;
                            child.measure(MeasureSpec.makeMeasureSpec((lp2.cellsUsed * cellSize) + lp2.extraPixels, 1073741824), itemHeightSpec);
                        } else {
                            smallestItemsAt3 = smallestItemsAt2;
                        }
                        i3 = cellCount + 1;
                        smallestItemsAt2 = smallestItemsAt3;
                    }
                }
                if (heightMode == 1073741824) {
                    widthPadding = heightSize;
                } else {
                    widthPadding = heightSize2;
                }
                setMeasuredDimension(widthSize, widthPadding);
            }
        }
        isGeneratedItem = false;
        if (cellsRemaining2 > 0) {
        }
        i2 = visibleItemCount2;
        if (needsExpansion) {
        }
        if (heightMode == 1073741824) {
        }
        setMeasuredDimension(widthSize, widthPadding);
    }

    static int measureChildForCells(View child, int cellSize, int cellsRemaining, int parentHeightMeasureSpec, int parentHeightPadding) {
        View view = child;
        int i = cellsRemaining;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        int childHeightSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(parentHeightMeasureSpec) - parentHeightPadding, MeasureSpec.getMode(parentHeightMeasureSpec));
        ActionMenuItemView itemView = view instanceof ActionMenuItemView ? (ActionMenuItemView) view : null;
        boolean expandable = false;
        boolean hasText = itemView != null && itemView.hasText();
        int cellsUsed = 0;
        if (i > 0 && (!hasText || i >= 2)) {
            view.measure(MeasureSpec.makeMeasureSpec(cellSize * i, Integer.MIN_VALUE), childHeightSpec);
            int measuredWidth = view.getMeasuredWidth();
            cellsUsed = measuredWidth / cellSize;
            if (measuredWidth % cellSize != 0) {
                cellsUsed++;
            }
            if (hasText && cellsUsed < 2) {
                cellsUsed = 2;
            }
        }
        if (!lp.isOverflowButton && hasText) {
            expandable = true;
        }
        lp.expandable = expandable;
        lp.cellsUsed = cellsUsed;
        view.measure(MeasureSpec.makeMeasureSpec(cellsUsed * cellSize, 1073741824), childHeightSpec);
        return cellsUsed;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mFormatItems) {
            int midVertical;
            boolean isLayoutRtl;
            int overflowWidth;
            int t;
            int size;
            int childCount = getChildCount();
            int midVertical2 = (bottom - top) / 2;
            int dividerWidth = getDividerWidth();
            int nonOverflowCount = 0;
            int widthRemaining = ((right - left) - getPaddingRight()) - getPaddingLeft();
            boolean hasOverflow = false;
            boolean isLayoutRtl2 = isLayoutRtl();
            int widthRemaining2 = widthRemaining;
            widthRemaining = 0;
            int overflowWidth2 = 0;
            int i = 0;
            while (i < childCount) {
                View v = getChildAt(i);
                if (v.getVisibility() == 8) {
                    midVertical = midVertical2;
                    isLayoutRtl = isLayoutRtl2;
                } else {
                    LayoutParams p = (LayoutParams) v.getLayoutParams();
                    if (p.isOverflowButton) {
                        int l;
                        overflowWidth = v.getMeasuredWidth();
                        if (hasDividerBeforeChildAt(i)) {
                            overflowWidth += dividerWidth;
                        }
                        overflowWidth2 = v.getMeasuredHeight();
                        if (isLayoutRtl2) {
                            isLayoutRtl = isLayoutRtl2;
                            l = getPaddingLeft() + p.leftMargin;
                            isLayoutRtl2 = l + overflowWidth;
                        } else {
                            isLayoutRtl = isLayoutRtl2;
                            isLayoutRtl2 = (getWidth() - getPaddingRight()) - p.rightMargin;
                            l = isLayoutRtl2 - overflowWidth;
                        }
                        t = midVertical2 - (overflowWidth2 / 2);
                        midVertical = midVertical2;
                        v.layout(l, t, isLayoutRtl2, t + overflowWidth2);
                        widthRemaining2 -= overflowWidth;
                        hasOverflow = true;
                        overflowWidth2 = overflowWidth;
                    } else {
                        midVertical = midVertical2;
                        isLayoutRtl = isLayoutRtl2;
                        size = (v.getMeasuredWidth() + p.leftMargin) + p.rightMargin;
                        widthRemaining += size;
                        widthRemaining2 -= size;
                        if (hasDividerBeforeChildAt(i)) {
                            widthRemaining += dividerWidth;
                        }
                        nonOverflowCount++;
                    }
                }
                i++;
                isLayoutRtl2 = isLayoutRtl;
                midVertical2 = midVertical;
            }
            midVertical = midVertical2;
            isLayoutRtl = isLayoutRtl2;
            int i2 = 1;
            int spacerSize;
            int t2;
            if (childCount != 1 || hasOverflow) {
                if (hasOverflow) {
                    i2 = 0;
                }
                size = nonOverflowCount - i2;
                t = 0;
                spacerSize = Math.max(0, size > 0 ? widthRemaining2 / size : 0);
                int dividerWidth2;
                int overflowWidth3;
                if (isLayoutRtl) {
                    overflowWidth = getWidth() - getPaddingRight();
                    while (t < childCount) {
                        View v2 = getChildAt(t);
                        LayoutParams lp = (LayoutParams) v2.getLayoutParams();
                        int spacerCount = size;
                        if (v2.getVisibility() == 8) {
                            dividerWidth2 = dividerWidth;
                            overflowWidth3 = overflowWidth2;
                        } else if (lp.isOverflowButton != 0) {
                            dividerWidth2 = dividerWidth;
                            overflowWidth3 = overflowWidth2;
                        } else {
                            overflowWidth -= lp.rightMargin;
                            size = v2.getMeasuredWidth();
                            i2 = v2.getMeasuredHeight();
                            midVertical2 = midVertical - (i2 / 2);
                            dividerWidth2 = dividerWidth;
                            overflowWidth3 = overflowWidth2;
                            v2.layout(overflowWidth - size, midVertical2, overflowWidth, midVertical2 + i2);
                            overflowWidth -= (lp.leftMargin + size) + spacerSize;
                        }
                        t++;
                        size = spacerCount;
                        dividerWidth = dividerWidth2;
                        overflowWidth2 = overflowWidth3;
                    }
                    dividerWidth2 = dividerWidth;
                    overflowWidth3 = overflowWidth2;
                } else {
                    dividerWidth2 = dividerWidth;
                    overflowWidth3 = overflowWidth2;
                    size = getPaddingLeft();
                    while (t < childCount) {
                        View v3 = getChildAt(t);
                        LayoutParams lp2 = (LayoutParams) v3.getLayoutParams();
                        if (!(v3.getVisibility() == 8 || lp2.isOverflowButton)) {
                            size += lp2.leftMargin;
                            dividerWidth = v3.getMeasuredWidth();
                            overflowWidth2 = v3.getMeasuredHeight();
                            t2 = midVertical - (overflowWidth2 / 2);
                            v3.layout(size, t2, size + dividerWidth, t2 + overflowWidth2);
                            size += (lp2.rightMargin + dividerWidth) + spacerSize;
                        }
                        t++;
                    }
                }
                return;
            }
            View v4 = getChildAt(null);
            t = v4.getMeasuredWidth();
            spacerSize = v4.getMeasuredHeight();
            t2 = ((right - left) / 2) - (t / 2);
            i2 = midVertical - (spacerSize / 2);
            v4.layout(t2, i2, t2 + t, i2 + spacerSize);
            return;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dismissPopupMenus();
    }

    public void setOverflowIcon(Drawable icon) {
        getMenu();
        this.mPresenter.setOverflowIcon(icon);
    }

    public Drawable getOverflowIcon() {
        getMenu();
        return this.mPresenter.getOverflowIcon();
    }

    public boolean isOverflowReserved() {
        return this.mReserveOverflow;
    }

    public void setOverflowReserved(boolean reserveOverflow) {
        this.mReserveOverflow = reserveOverflow;
    }

    protected LayoutParams generateDefaultLayoutParams() {
        LayoutParams params = new LayoutParams(-2, -2);
        params.gravity = 16;
        return params;
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    protected LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        if (p == null) {
            return generateDefaultLayoutParams();
        }
        LayoutParams result;
        if (p instanceof LayoutParams) {
            result = new LayoutParams((LayoutParams) p);
        } else {
            result = new LayoutParams(p);
        }
        if (result.gravity <= 0) {
            result.gravity = 16;
        }
        return result;
    }

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p != null && (p instanceof LayoutParams);
    }

    public LayoutParams generateOverflowButtonLayoutParams() {
        LayoutParams result = generateDefaultLayoutParams();
        result.isOverflowButton = true;
        return result;
    }

    public boolean invokeItem(MenuItemImpl item) {
        return this.mMenu.performItemAction(item, 0);
    }

    public int getWindowAnimations() {
        return 0;
    }

    public void initialize(MenuBuilder menu) {
        this.mMenu = menu;
    }

    public Menu getMenu() {
        if (this.mMenu == null) {
            Context context = getContext();
            this.mMenu = new MenuBuilder(context);
            this.mMenu.setCallback(new MenuBuilderCallback());
            this.mPresenter = new ActionMenuPresenter(context);
            this.mPresenter.setReserveOverflow(true);
            this.mPresenter.setCallback(this.mActionMenuPresenterCallback != null ? this.mActionMenuPresenterCallback : new ActionMenuPresenterCallback());
            this.mMenu.addMenuPresenter(this.mPresenter, this.mPopupContext);
            this.mPresenter.setMenuView(this);
        }
        return this.mMenu;
    }

    public void setMenuCallbacks(Callback pcb, MenuBuilder.Callback mcb) {
        this.mActionMenuPresenterCallback = pcb;
        this.mMenuBuilderCallback = mcb;
    }

    public MenuBuilder peekMenu() {
        return this.mMenu;
    }

    public boolean showOverflowMenu() {
        return this.mPresenter != null && this.mPresenter.showOverflowMenu();
    }

    public boolean hideOverflowMenu() {
        return this.mPresenter != null && this.mPresenter.hideOverflowMenu();
    }

    public boolean isOverflowMenuShowing() {
        return this.mPresenter != null && this.mPresenter.isOverflowMenuShowing();
    }

    public boolean isOverflowMenuShowPending() {
        return this.mPresenter != null && this.mPresenter.isOverflowMenuShowPending();
    }

    public void dismissPopupMenus() {
        if (this.mPresenter != null) {
            this.mPresenter.dismissPopupMenus();
        }
    }

    protected boolean hasDividerBeforeChildAt(int childIndex) {
        if (childIndex == 0) {
            return false;
        }
        View childBefore = getChildAt(childIndex - 1);
        View child = getChildAt(childIndex);
        boolean result = false;
        if (childIndex < getChildCount() && (childBefore instanceof ActionMenuChildView)) {
            result = false | ((ActionMenuChildView) childBefore).needsDividerAfter();
        }
        if (childIndex > 0 && (child instanceof ActionMenuChildView)) {
            result |= ((ActionMenuChildView) child).needsDividerBefore();
        }
        return result;
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        return false;
    }

    public void setExpandedActionViewsExclusive(boolean exclusive) {
        this.mPresenter.setExpandedActionViewsExclusive(exclusive);
    }

    protected ActionMenuPresenter getPresenter() {
        return this.mPresenter;
    }

    protected boolean getFormatItems() {
        return this.mFormatItems;
    }

    protected int getGeneratedItemPadding() {
        return this.mGeneratedItemPadding;
    }

    public void setSplitViewMaxSize(int maxSize) {
    }
}
