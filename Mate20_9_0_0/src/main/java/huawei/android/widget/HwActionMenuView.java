package huawei.android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewParent;
import android.widget.ActionMenuPresenter;
import android.widget.ActionMenuView;
import android.widget.ActionMenuView.LayoutParams;
import android.widget.TextView;
import huawei.com.android.internal.view.menu.HwActionMenuItemView;

public class HwActionMenuView extends ActionMenuView implements HwSmartColorListener {
    private static final boolean DEBUG = true;
    private static final String TAG = "HwActionMenuView";
    private int mActionBarMenuItemPadding;
    private int mActionBarMenuItemSize;
    private int mActionBarMenuPadding;
    private int mActionBarOverFlowBtnEndPadding;
    private int mActionBarOverFlowBtnSize;
    private int mActionBarOverFlowBtnStartPadding;
    private Typeface mCondensed;
    private boolean mContain2Lines;
    private boolean mHasOverFlowBtnAtActionBar;
    private boolean mHasVisibleChildAtActionBar;
    private int mMaxSize;
    private int mMinHeight;
    private ActionMenuPresenter mPresenter;
    private Parcelable mSavedState;
    private Typeface mSerif;
    private ColorStateList mSmartIconColor;
    private ColorStateList mSmartTitleColor;
    private int mTextSize;
    private int[] mToolBarMenuItemMaxSize3;
    private int[] mToolBarMenuItemMaxSize4;
    private int[] mToolBarMenuItemMaxSize5;
    private int mToolBarMenuItemPadding;
    private int mToolBarMenuPadding;

    private static class ItemSpec {
        int itemSize;
        int maxSize;
        int measuredSize;
        int usingSmallFont;
        boolean wasTooLong;

        private ItemSpec() {
        }
    }

    public HwActionMenuView(Context context) {
        this(context, null);
    }

    public HwActionMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMinHeight = 0;
        this.mContain2Lines = false;
        this.mHasVisibleChildAtActionBar = false;
        this.mHasOverFlowBtnAtActionBar = false;
        Log.d(TAG, "new HwActionMenuView");
        this.mMinHeight = context.getResources().getDimensionPixelSize(34471968);
        initWidths(getContext());
        this.mSerif = Typeface.create("dinnext-serif", 0);
        this.mCondensed = Typeface.create("dinnext-condensed", 0);
    }

    public void setPresenter(ActionMenuPresenter presenter) {
        super.setPresenter(presenter);
        this.mPresenter = presenter;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (((View) getParent()).getId() == 16909357) {
            onMeasureAtSplitView(widthMeasureSpec, heightMeasureSpec);
            setPadding(0, 0, 0, 0);
            return;
        }
        onMeasureAtActionBar(widthMeasureSpec, heightMeasureSpec);
        if (!this.mHasVisibleChildAtActionBar) {
            setPadding(0, 0, 0, 0);
        } else if (isLayoutRtl()) {
            if (this.mHasOverFlowBtnAtActionBar) {
                setPadding(this.mActionBarOverFlowBtnEndPadding, 0, 0, 0);
            } else {
                setPadding(this.mActionBarMenuPadding, 0, 0, 0);
            }
        } else if (this.mHasOverFlowBtnAtActionBar) {
            setPadding(0, 0, this.mActionBarOverFlowBtnEndPadding, 0);
        } else {
            setPadding(0, 0, this.mActionBarMenuPadding, 0);
        }
    }

    protected void onMeasureAtActionBar(int widthMeasureSpec, int heightMeasureSpec) {
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

    private boolean isNextItemOverFlowBtn(int itemnum) {
        int childCount = getChildCount();
        if (childCount >= 2 && itemnum < childCount - 1 && ((LayoutParams) getChildAt(itemnum + 1).getLayoutParams()).isOverflowButton) {
            return DEBUG;
        }
        return false;
    }

    protected void onMeasureAtSplitView(int widthMeasureSpec, int heightMeasureSpec) {
        int widthPadding;
        int containerWidth;
        int containerHeight;
        int parentWidth;
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int itemHeightSpec = getChildMeasureSpec(heightMeasureSpec, heightPadding, -2);
        int childTotalWidth = widthSize;
        View actionbarOverlayLayout = (View) ((View) getParent()).getParent();
        View container = (View) actionbarOverlayLayout.getParent();
        if (container != null) {
            widthPadding = this.mToolBarMenuPadding * 2;
            containerWidth = container.getWidth();
            containerHeight = container.getHeight();
            parentWidth = (containerWidth < containerHeight ? containerWidth : containerHeight) - widthPadding;
            childTotalWidth = parentWidth > widthSize ? parentWidth : widthSize;
        }
        if (this.mMaxSize != 0) {
            childTotalWidth = this.mMaxSize - (this.mToolBarMenuPadding * 2);
        }
        containerWidth = 0;
        containerHeight = 0;
        parentWidth = getChildCount();
        int visibleItemCount = 0;
        int i = 0;
        while (i < parentWidth) {
            int heightPadding2 = heightPadding;
            if (getChildAt(i).getVisibility() != 8) {
                visibleItemCount++;
            }
            i++;
            heightPadding = heightPadding2;
        }
        int i2;
        int i3;
        View view;
        View view2;
        int i4;
        int i5;
        if (visibleItemCount <= 0) {
            setMeasuredDimension(widthSize, 0);
            i2 = widthSize;
            i3 = childTotalWidth;
            view = actionbarOverlayLayout;
            view2 = container;
            i4 = 0;
            i5 = visibleItemCount;
        } else {
            TextView child;
            int requiredWidth;
            int avgCellSizeRemaining;
            if (visibleItemCount < 4) {
                heightPadding = childTotalWidth / 4;
                containerWidth = (childTotalWidth - (heightPadding * visibleItemCount)) / visibleItemCount;
            } else {
                heightPadding = childTotalWidth / visibleItemCount;
            }
            ItemSpec[] items = new ItemSpec[visibleItemCount];
            int maxDiff = 0;
            int gone = 0;
            widthPadding = 0;
            while (widthPadding < parentWidth) {
                child = (TextView) getChildAt(widthPadding);
                i2 = widthSize;
                widthSize = widthPadding - gone;
                i3 = childTotalWidth;
                int i6;
                if (child.getVisibility() != 8) {
                    items[widthSize] = new ItemSpec();
                    items[widthSize].usingSmallFont = -1;
                    items[widthSize].itemSize = heightPadding;
                    items[widthSize].maxSize = getItemMaxSize(widthSize, visibleItemCount);
                    child.setSingleLine(DEBUG);
                    child.setMaxLines(1);
                    child.setTypeface(this.mSerif);
                    child.setTranslationX(0.0f);
                    child.setHwCompoundPadding(0, 0, 0, 0);
                    requiredWidth = ((int) child.getPaint().measureText(child.getText().toString())) + (this.mToolBarMenuItemPadding * 2);
                    items[widthSize].measuredSize = requiredWidth;
                    if (requiredWidth > heightPadding) {
                        view = actionbarOverlayLayout;
                        items[widthSize].wasTooLong = DEBUG;
                        actionbarOverlayLayout = maxDiff;
                        maxDiff = requiredWidth - heightPadding > actionbarOverlayLayout ? requiredWidth - heightPadding : actionbarOverlayLayout;
                        i6 = widthSize;
                    } else {
                        view = actionbarOverlayLayout;
                        actionbarOverlayLayout = maxDiff;
                        i6 = widthSize;
                        items[widthSize].wasTooLong = false;
                    }
                } else {
                    i6 = widthSize;
                    view = actionbarOverlayLayout;
                    gone++;
                }
                widthPadding++;
                widthSize = i2;
                childTotalWidth = i3;
                actionbarOverlayLayout = view;
                requiredWidth = heightMeasureSpec;
            }
            i3 = childTotalWidth;
            view = actionbarOverlayLayout;
            int maxDiff2 = maxDiff;
            if (maxDiff2 == 0 || maxDiff2 < containerWidth) {
                avgCellSizeRemaining = containerWidth;
                i5 = visibleItemCount;
            } else if (visibleItemCount == 1) {
                view2 = container;
                avgCellSizeRemaining = containerWidth;
                i5 = visibleItemCount;
            } else {
                TextView container2;
                int index;
                int[] standardIconPos = new int[visibleItemCount];
                widthPadding = heightPadding + containerWidth;
                for (heightPadding = 0; heightPadding < items.length; heightPadding++) {
                    items[heightPadding].itemSize = widthPadding;
                    standardIconPos[heightPadding] = (widthPadding * heightPadding) + ((widthPadding - this.mActionBarMenuItemSize) / 2);
                    if (items[heightPadding].measuredSize > widthPadding) {
                        items[heightPadding].wasTooLong = DEBUG;
                    } else {
                        items[heightPadding].wasTooLong = false;
                    }
                }
                childTotalWidth = 0;
                heightPadding = 0;
                while (heightPadding < parentWidth) {
                    child = (TextView) getChildAt(heightPadding);
                    gone = heightPadding - childTotalWidth;
                    view2 = container;
                    if (child.getVisibility() == 8) {
                        childTotalWidth++;
                    } else if (items[gone].wasTooLong) {
                        adjustSizeFromsiblings(child, itemHeightSpec, items, gone);
                        if (items[gone].usingSmallFont == 0 && items[gone].wasTooLong) {
                            adjustSizeFromsiblings(child, itemHeightSpec, items, gone);
                        }
                    }
                    heightPadding++;
                    container = view2;
                }
                childTotalWidth = 0;
                heightPadding = 0;
                widthSize = 0;
                while (widthSize < parentWidth) {
                    container2 = (TextView) getChildAt(widthSize);
                    index = widthSize - heightPadding;
                    i4 = widthPadding;
                    if (container2.getVisibility() != 8) {
                        boolean textTrans;
                        i5 = visibleItemCount;
                        avgCellSizeRemaining = containerWidth;
                        container2.setPadding(this.mToolBarMenuItemPadding, container2.getPaddingTop(), this.mToolBarMenuItemPadding, container2.getPaddingBottom());
                        widthPadding = ((items[index].itemSize - this.mActionBarMenuItemSize) / 2) + childTotalWidth;
                        containerWidth = standardIconPos[index] - widthPadding;
                        int iconPosition;
                        boolean textTrans2;
                        if (containerWidth < 0) {
                            iconPosition = widthPadding;
                            textTrans2 = false;
                            if ((this.mToolBarMenuItemPadding + childTotalWidth) + ((items[index].itemSize - items[index].measuredSize) / 2) > standardIconPos[index]) {
                                container2.setTranslationX((float) containerWidth);
                                textTrans = DEBUG;
                            } else {
                                textTrans = textTrans2;
                            }
                        } else {
                            iconPosition = widthPadding;
                            textTrans2 = false;
                            if ((childTotalWidth - this.mToolBarMenuItemPadding) + ((items[index].itemSize + items[index].measuredSize) / 2) < standardIconPos[index] + this.mActionBarMenuItemSize) {
                                container2.setTranslationX((float) containerWidth);
                                textTrans = DEBUG;
                            } else {
                                textTrans = textTrans2;
                            }
                        }
                        if (!textTrans) {
                            container2.setHwCompoundPadding(containerWidth, 0, 0, 0);
                        }
                        childTotalWidth += items[index].itemSize;
                        container2.measure(MeasureSpec.makeMeasureSpec(items[index].itemSize, 1073741824), itemHeightSpec);
                        containerHeight += container2.getMeasuredWidth();
                    } else {
                        avgCellSizeRemaining = containerWidth;
                        i5 = visibleItemCount;
                        heightPadding++;
                    }
                    widthSize++;
                    widthPadding = i4;
                    visibleItemCount = i5;
                    containerWidth = avgCellSizeRemaining;
                }
                avgCellSizeRemaining = containerWidth;
                i5 = visibleItemCount;
                this.mContain2Lines = false;
                heightPadding = 0;
                for (widthSize = 0; widthSize < parentWidth; widthSize++) {
                    container2 = (TextView) getChildAt(widthSize);
                    index = widthSize - heightPadding;
                    if (container2.getVisibility() == 8) {
                        heightPadding++;
                    } else if (items[index].wasTooLong) {
                        container2.setTypeface(this.mCondensed);
                        if (((int) container2.getPaint().measureText(container2.getText().toString())) + (this.mToolBarMenuItemPadding * 2) > items[index].itemSize) {
                            container2.setSingleLine(false);
                            container2.setMaxLines(2);
                            this.mContain2Lines = DEBUG;
                        }
                        container2.measure(MeasureSpec.makeMeasureSpec(items[index].itemSize, 1073741824), itemHeightSpec);
                    }
                }
                if (this.mContain2Lines != null) {
                    container = this.mMinHeight + this.mTextSize;
                } else {
                    container = this.mMinHeight;
                }
                setMeasuredDimension(containerHeight, container);
                containerWidth = avgCellSizeRemaining;
            }
            widthSize = 0;
            if (maxDiff2 <= 0) {
            } else if (maxDiff2 < avgCellSizeRemaining) {
                widthSize = maxDiff2;
            }
            requiredWidth = 0;
            while (requiredWidth < parentWidth) {
                TextView child2 = (TextView) getChildAt(requiredWidth);
                if (child2.getVisibility() != 8) {
                    child2.setPadding(this.mToolBarMenuItemPadding, child2.getPaddingTop(), this.mToolBarMenuItemPadding, child2.getPaddingBottom());
                    visibleItemCount = i5;
                    if (visibleItemCount == 1) {
                        child2.measure(MeasureSpec.makeMeasureSpec(heightPadding > items[0].measuredSize ? heightPadding : items[0].measuredSize, 1073741824), itemHeightSpec);
                    } else {
                        child2.measure(MeasureSpec.makeMeasureSpec(heightPadding + widthSize, 1073741824), itemHeightSpec);
                    }
                    containerHeight += child2.getMeasuredWidth();
                } else {
                    visibleItemCount = i5;
                }
                requiredWidth++;
                i5 = visibleItemCount;
            }
            setMeasuredDimension(containerHeight, this.mMinHeight);
        }
    }

    private void adjustSizeFromsiblings(TextView child, int itemHeightSpec, ItemSpec[] items, int index) {
        if (index == 0 && items.length > 1) {
            adjustSize(child, itemHeightSpec, items, index, 1);
        } else if (index == items.length - 1 && items.length > 1) {
            adjustSize(child, itemHeightSpec, items, index, -1);
        } else if (items.length <= 2) {
        } else {
            if (!items[index - 1].wasTooLong && items[index + 1].wasTooLong) {
                adjustSize(child, itemHeightSpec, items, index, -1);
            } else if (!items[index + 1].wasTooLong && items[index - 1].wasTooLong) {
                adjustSize(child, itemHeightSpec, items, index, 1);
            } else if (!items[index - 1].wasTooLong && !items[index + 1].wasTooLong) {
                adjustSize(child, itemHeightSpec, items, index, -1, 1);
            }
        }
    }

    private void adjustSize(TextView child, int itemHeightSpec, ItemSpec[] items, int index, int diff) {
        TextView textView;
        int need = index;
        int brother = index + diff;
        if (!items[brother].wasTooLong) {
            int needMeasuredSize = items[need].measuredSize;
            boolean stillTooLong = false;
            if (items[need].measuredSize > items[need].maxSize && items[need].maxSize > 0) {
                needMeasuredSize = items[need].maxSize;
                stillTooLong = DEBUG;
            }
            int needDiff = needMeasuredSize - items[need].itemSize;
            int overflowDiff = items[brother].itemSize - items[brother].measuredSize;
            ItemSpec itemSpec;
            if (needDiff < overflowDiff) {
                itemSpec = items[need];
                itemSpec.itemSize += needDiff;
                items[need].wasTooLong = stillTooLong;
                itemSpec = items[brother];
                itemSpec.itemSize -= needDiff;
            } else if (overflowDiff > 0) {
                int i = items[need].usingSmallFont;
                boolean z = DEBUG;
                if (i == -1) {
                    textView = child;
                    textView.setTypeface(this.mCondensed);
                    i = ((int) textView.getPaint().measureText(textView.getText().toString())) + (2 * this.mToolBarMenuItemPadding);
                    items[need].measuredSize = i;
                    ItemSpec itemSpec2 = items[need];
                    if (items[need].itemSize >= i) {
                        z = false;
                    }
                    itemSpec2.wasTooLong = z;
                    items[need].usingSmallFont = 0;
                    return;
                }
                textView = child;
                if (items[need].usingSmallFont == 0) {
                    items[need].usingSmallFont = 1;
                    itemSpec = items[need];
                    itemSpec.itemSize += overflowDiff;
                    items[need].wasTooLong = DEBUG;
                    itemSpec = items[brother];
                    itemSpec.itemSize -= overflowDiff;
                    return;
                }
                return;
            }
        }
        textView = child;
    }

    private void adjustSize(TextView child, int itemHeightSpec, ItemSpec[] items, int index, int diff1, int diff2) {
        int need = index;
        int brother1 = index + diff1;
        int brother2 = index + diff2;
        int overflowDiff1 = items[brother1].itemSize - items[brother1].measuredSize;
        int overflowDiff2 = items[brother2].itemSize - items[brother2].measuredSize;
        int min = brother1;
        int minOverflowDiff = overflowDiff1;
        int max = brother2;
        int maxOverflowDiff = overflowDiff2;
        if (overflowDiff1 > overflowDiff2) {
            max = brother1;
            maxOverflowDiff = overflowDiff1;
            min = brother2;
            minOverflowDiff = overflowDiff2;
        }
        int needMeasuredSize = items[need].measuredSize;
        boolean stillTooLong = false;
        if (items[need].measuredSize > items[need].maxSize && items[need].maxSize > 0) {
            needMeasuredSize = items[need].maxSize;
            stillTooLong = DEBUG;
        }
        int needDiff = needMeasuredSize - items[need].itemSize;
        int midNeedDiff;
        TextView textView;
        int i;
        if (needDiff <= maxOverflowDiff + minOverflowDiff) {
            midNeedDiff = needDiff / 2;
            ItemSpec itemSpec;
            if (midNeedDiff <= minOverflowDiff) {
                itemSpec = items[need];
                itemSpec.itemSize += needDiff;
                items[need].wasTooLong = stillTooLong;
                itemSpec = items[min];
                itemSpec.itemSize -= midNeedDiff;
                itemSpec = items[max];
                itemSpec.itemSize -= needDiff - midNeedDiff;
            } else {
                if (midNeedDiff <= maxOverflowDiff) {
                    itemSpec = items[need];
                    itemSpec.itemSize += needDiff;
                    items[need].wasTooLong = stillTooLong;
                    itemSpec = items[min];
                    itemSpec.itemSize -= minOverflowDiff;
                    itemSpec = items[max];
                    itemSpec.itemSize -= needDiff - minOverflowDiff;
                }
            }
            textView = child;
            i = needDiff;
            return;
        }
        if (items[need].usingSmallFont == -1) {
            child.setTypeface(this.mCondensed);
            midNeedDiff = ((int) child.getPaint().measureText(child.getText().toString())) + (2 * this.mToolBarMenuItemPadding);
            items[need].measuredSize = midNeedDiff;
            items[need].wasTooLong = items[need].itemSize < midNeedDiff ? DEBUG : false;
            items[need].usingSmallFont = 0;
            return;
        }
        textView = child;
        i = needDiff;
        if (items[need].usingSmallFont == 0) {
            items[need].usingSmallFont = 1;
            ItemSpec itemSpec2 = items[need];
            itemSpec2.itemSize += minOverflowDiff + maxOverflowDiff;
            items[need].wasTooLong = DEBUG;
            itemSpec2 = items[min];
            itemSpec2.itemSize -= minOverflowDiff;
            itemSpec2 = items[max];
            itemSpec2.itemSize -= maxOverflowDiff;
        }
    }

    private int getItemMaxSize(int index, int visibleItemCount) {
        if (visibleItemCount == 3) {
            return this.mToolBarMenuItemMaxSize3[index];
        }
        if (visibleItemCount == 4) {
            return this.mToolBarMenuItemMaxSize4[index];
        }
        if (visibleItemCount == 5) {
            return this.mToolBarMenuItemMaxSize5[index];
        }
        return 0;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        boolean isToolBar;
        int i = top;
        int i2 = bottom;
        int childCount = getChildCount();
        boolean isLayoutRtl = isLayoutRtl();
        ViewParent parent = getParent();
        int i3 = 0;
        boolean z = DEBUG;
        if (parent != null) {
            if (((View) getParent()).getId() != 16909357) {
                z = false;
            }
            isToolBar = z;
        } else {
            if (getContext().getResources().getConfiguration().orientation != 1) {
                z = false;
            }
            isToolBar = z;
        }
        int startRight;
        View v;
        LayoutParams lp;
        int width;
        int height;
        int t;
        if (isLayoutRtl) {
            startRight = getWidth() - getStartRight(isToolBar);
            while (i3 < childCount) {
                v = getChildAt(i3);
                lp = (LayoutParams) v.getLayoutParams();
                if (v.getVisibility() != 8) {
                    startRight -= lp.rightMargin;
                    width = v.getMeasuredWidth();
                    height = v.getMeasuredHeight();
                    t = getLayoutTop(isToolBar, i, i2, v);
                    v.layout(startRight - width, t, startRight, t + height);
                    startRight -= (lp.leftMargin + width) + getItemPadding(isToolBar, i3);
                }
                i3++;
            }
            return;
        }
        startRight = getStartLeft(isToolBar);
        while (i3 < childCount) {
            v = getChildAt(i3);
            lp = (LayoutParams) v.getLayoutParams();
            if (v.getVisibility() != 8) {
                startRight += lp.leftMargin;
                width = v.getMeasuredWidth();
                height = v.getMeasuredHeight();
                t = getLayoutTop(isToolBar, i, i2, v);
                v.layout(startRight, t, startRight + width, t + height);
                startRight += (lp.rightMargin + width) + getItemPadding(isToolBar, i3);
            }
            i3++;
        }
    }

    private int getStartLeft(boolean isToolBar) {
        if (isToolBar) {
            return getPaddingLeft();
        }
        return 0;
    }

    private int getStartRight(boolean isToolBar) {
        if (isToolBar) {
            return getPaddingRight();
        }
        return 0;
    }

    private int getLayoutTop(boolean isToolBar, int top, int bottom, View child) {
        if (isToolBar) {
            return getPaddingTop();
        }
        return ((bottom - top) / 2) - (child.getMeasuredHeight() / 2);
    }

    private int getItemPadding(boolean isToolBar, int itemNum) {
        if (isToolBar) {
            return 0;
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
        super.onConfigurationChanged(newConfig);
        this.mMinHeight = getContext().getResources().getDimensionPixelSize(34471968);
    }

    private void initWidths(Context context) {
        int i;
        DisplayMetrics dp = context.getResources().getDisplayMetrics();
        Resources res = context.getResources();
        this.mTextSize = res.getDimensionPixelSize(34471965) + res.getDimensionPixelSize(34471967);
        this.mActionBarMenuItemPadding = res.getDimensionPixelSize(34471972);
        this.mActionBarMenuItemSize = res.getDimensionPixelSize(34471973);
        this.mActionBarOverFlowBtnSize = res.getDimensionPixelSize(34472084);
        this.mActionBarMenuPadding = res.getDimensionPixelSize(34471971);
        this.mToolBarMenuItemPadding = res.getDimensionPixelSize(34471970);
        this.mToolBarMenuPadding = res.getDimensionPixelSize(34471969);
        this.mActionBarOverFlowBtnStartPadding = res.getDimensionPixelSize(34471974);
        this.mActionBarOverFlowBtnEndPadding = res.getDimensionPixelSize(34471975);
        float density = dp.density;
        this.mToolBarMenuItemMaxSize3 = res.getIntArray(33816578);
        int i2 = 0;
        for (i = 0; i < this.mToolBarMenuItemMaxSize3.length; i++) {
            this.mToolBarMenuItemMaxSize3[i] = (int) (((float) this.mToolBarMenuItemMaxSize3[i]) * density);
        }
        this.mToolBarMenuItemMaxSize4 = res.getIntArray(33816579);
        for (i = 0; i < this.mToolBarMenuItemMaxSize4.length; i++) {
            this.mToolBarMenuItemMaxSize4[i] = (int) (((float) this.mToolBarMenuItemMaxSize4[i]) * density);
        }
        this.mToolBarMenuItemMaxSize5 = res.getIntArray(33816580);
        while (i2 < this.mToolBarMenuItemMaxSize5.length) {
            this.mToolBarMenuItemMaxSize5[i2] = (int) (((float) this.mToolBarMenuItemMaxSize5[i2]) * density);
            i2++;
        }
    }

    public void setSplitViewMaxSize(int maxSize) {
        this.mMaxSize = maxSize;
    }

    public void onSetSmartColor(ColorStateList iconColor, ColorStateList titleColor) {
        this.mSmartIconColor = iconColor;
        this.mSmartTitleColor = titleColor;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof HwActionMenuItemView) {
                ((HwActionMenuItemView) child).updateTextAndIcon();
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
