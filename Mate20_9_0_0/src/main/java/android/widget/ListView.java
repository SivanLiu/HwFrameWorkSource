package android.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.iawareperf.UniPerf;
import android.os.Bundle;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.util.SparseBooleanArray;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.RemotableViewMethod;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewDebug.ExportedProperty;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewHierarchyEncoder;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.accessibility.AccessibilityNodeInfo.CollectionInfo;
import android.view.accessibility.AccessibilityNodeInfo.CollectionItemInfo;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.R;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@RemoteView
public class ListView extends HwAbsListView {
    private static final float MAX_SCROLL_FACTOR = 0.33f;
    private static final int MIN_SCROLL_PREVIEW_PIXELS = 2;
    static final int NO_POSITION = -1;
    static final String TAG = "ListView";
    private boolean mAreAllItemsSelectable;
    private final ArrowScrollFocusResult mArrowScrollFocusResult;
    Drawable mDivider;
    int mDividerHeight;
    private boolean mDividerIsOpaque;
    private Paint mDividerPaint;
    private FocusSelector mFocusSelector;
    private boolean mFooterDividersEnabled;
    ArrayList<FixedViewInfo> mFooterViewInfos;
    private boolean mHeaderDividersEnabled;
    ArrayList<FixedViewInfo> mHeaderViewInfos;
    private boolean mIsCacheColorOpaque;
    private boolean mItemsCanFocus;
    Drawable mOverScrollFooter;
    Drawable mOverScrollHeader;
    private final Rect mTempRect;

    private static class ArrowScrollFocusResult {
        private int mAmountToScroll;
        private int mSelectedPosition;

        private ArrowScrollFocusResult() {
        }

        void populate(int selectedPosition, int amountToScroll) {
            this.mSelectedPosition = selectedPosition;
            this.mAmountToScroll = amountToScroll;
        }

        public int getSelectedPosition() {
            return this.mSelectedPosition;
        }

        public int getAmountToScroll() {
            return this.mAmountToScroll;
        }
    }

    public class FixedViewInfo {
        public Object data;
        public boolean isSelectable;
        public View view;
    }

    private class FocusSelector implements Runnable {
        private static final int STATE_REQUEST_FOCUS = 3;
        private static final int STATE_SET_SELECTION = 1;
        private static final int STATE_WAIT_FOR_LAYOUT = 2;
        private int mAction;
        private int mPosition;
        private int mPositionTop;

        private FocusSelector() {
        }

        FocusSelector setupForSetSelection(int position, int top) {
            this.mPosition = position;
            this.mPositionTop = top;
            this.mAction = 1;
            return this;
        }

        public void run() {
            if (this.mAction == 1) {
                ListView.this.setSelectionFromTop(this.mPosition, this.mPositionTop);
                this.mAction = 2;
            } else if (this.mAction == 3) {
                View child = ListView.this.getChildAt(this.mPosition - ListView.this.mFirstPosition);
                if (child != null) {
                    child.requestFocus();
                }
                this.mAction = -1;
            }
        }

        Runnable setupFocusIfValid(int position) {
            if (this.mAction != 2 || position != this.mPosition) {
                return null;
            }
            this.mAction = 3;
            return this;
        }

        void onLayoutComplete() {
            if (this.mAction == 2) {
                this.mAction = -1;
            }
        }
    }

    public ListView(Context context) {
        this(context, null);
    }

    public ListView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842868);
    }

    public ListView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mHeaderViewInfos = Lists.newArrayList();
        this.mFooterViewInfos = Lists.newArrayList();
        this.mAreAllItemsSelectable = true;
        this.mItemsCanFocus = false;
        this.mTempRect = new Rect();
        this.mArrowScrollFocusResult = new ArrowScrollFocusResult();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ListView, defStyleAttr, defStyleRes);
        Object[] entries = a.getTextArray(0);
        if (entries != null) {
            setAdapter(new ArrayAdapter(context, 17367043, entries));
        }
        Drawable d = a.getDrawable(1);
        if (d != null) {
            setDivider(d);
        }
        Drawable osHeader = a.getDrawable(5);
        if (osHeader != null) {
            setOverscrollHeader(osHeader);
        }
        Drawable osFooter = a.getDrawable(6);
        if (osFooter != null) {
            setOverscrollFooter(osFooter);
        }
        if (a.hasValueOrEmpty(2)) {
            int dividerHeight = a.getDimensionPixelSize(2, 0);
            if (dividerHeight != 0) {
                setDividerHeight(dividerHeight);
            }
        }
        this.mHeaderDividersEnabled = a.getBoolean(3, true);
        this.mFooterDividersEnabled = a.getBoolean(4, true);
        a.recycle();
    }

    public int getMaxScrollAmount() {
        return (int) (MAX_SCROLL_FACTOR * ((float) (this.mBottom - this.mTop)));
    }

    private void adjustViewsUpOrDown() {
        int childCount = getChildCount();
        if (childCount > 0) {
            int delta;
            if (this.mStackFromBottom) {
                delta = getChildAt(childCount - 1).getBottom() - (getHeight() - this.mListPadding.bottom);
                if (this.mFirstPosition + childCount < this.mItemCount) {
                    delta += this.mDividerHeight;
                }
                if (delta > 0) {
                    delta = 0;
                }
            } else {
                delta = getChildAt(null).getTop() - this.mListPadding.top;
                if (this.mFirstPosition != 0) {
                    delta -= this.mDividerHeight;
                }
                if (delta < 0) {
                    delta = 0;
                }
            }
            if (delta != 0) {
                offsetChildrenTopAndBottom(-delta);
            }
        }
    }

    public void addHeaderView(View v, Object data, boolean isSelectable) {
        if (!(v.getParent() == null || v.getParent() == this || !Log.isLoggable(TAG, 5))) {
            Log.w(TAG, "The specified child already has a parent. You must call removeView() on the child's parent first.");
        }
        FixedViewInfo info = new FixedViewInfo();
        info.view = v;
        info.data = data;
        info.isSelectable = isSelectable;
        this.mHeaderViewInfos.add(info);
        this.mAreAllItemsSelectable &= isSelectable;
        if (this.mAdapter != null) {
            if (!(this.mAdapter instanceof HeaderViewListAdapter)) {
                wrapHeaderListAdapterInternal();
            }
            if (this.mDataSetObserver != null) {
                this.mDataSetObserver.onChanged();
            }
        }
    }

    public void addHeaderView(View v) {
        addHeaderView(v, null, true);
    }

    public int getHeaderViewsCount() {
        return this.mHeaderViewInfos.size();
    }

    public boolean removeHeaderView(View v) {
        if (this.mHeaderViewInfos.size() <= 0) {
            return false;
        }
        boolean result = false;
        if (this.mAdapter != null && ((HeaderViewListAdapter) this.mAdapter).removeHeader(v)) {
            if (this.mDataSetObserver != null) {
                this.mDataSetObserver.onChanged();
            }
            result = true;
        }
        removeFixedViewInfo(v, this.mHeaderViewInfos);
        return result;
    }

    private void removeFixedViewInfo(View v, ArrayList<FixedViewInfo> where) {
        int len = where.size();
        for (int i = 0; i < len; i++) {
            if (((FixedViewInfo) where.get(i)).view == v) {
                where.remove(i);
                return;
            }
        }
    }

    public void addFooterView(View v, Object data, boolean isSelectable) {
        if (!(v.getParent() == null || v.getParent() == this || !Log.isLoggable(TAG, 5))) {
            Log.w(TAG, "The specified child already has a parent. You must call removeView() on the child's parent first.");
        }
        FixedViewInfo info = new FixedViewInfo();
        info.view = v;
        info.data = data;
        info.isSelectable = isSelectable;
        this.mFooterViewInfos.add(info);
        this.mAreAllItemsSelectable &= isSelectable;
        if (this.mAdapter != null) {
            if (!(this.mAdapter instanceof HeaderViewListAdapter)) {
                wrapHeaderListAdapterInternal();
            }
            if (this.mDataSetObserver != null) {
                this.mDataSetObserver.onChanged();
            }
        }
    }

    public void addFooterView(View v) {
        addFooterView(v, null, true);
    }

    public int getFooterViewsCount() {
        return this.mFooterViewInfos.size();
    }

    public boolean removeFooterView(View v) {
        if (this.mFooterViewInfos.size() <= 0) {
            return false;
        }
        boolean result = false;
        if (this.mAdapter != null && ((HeaderViewListAdapter) this.mAdapter).removeFooter(v)) {
            if (this.mDataSetObserver != null) {
                this.mDataSetObserver.onChanged();
            }
            result = true;
        }
        removeFixedViewInfo(v, this.mFooterViewInfos);
        return result;
    }

    public ListAdapter getAdapter() {
        return this.mAdapter;
    }

    @RemotableViewMethod(asyncImpl = "setRemoteViewsAdapterAsync")
    public void setRemoteViewsAdapter(Intent intent) {
        super.setRemoteViewsAdapter(intent);
    }

    public void setAdapter(ListAdapter adapter) {
        if (!(this.mAdapter == null || this.mDataSetObserver == null)) {
            this.mAdapter.unregisterDataSetObserver(this.mDataSetObserver);
        }
        resetList();
        this.mRecycler.clear();
        if (this.mHeaderViewInfos.size() > 0 || this.mFooterViewInfos.size() > 0) {
            this.mAdapter = wrapHeaderListAdapterInternal(this.mHeaderViewInfos, this.mFooterViewInfos, adapter);
        } else {
            this.mAdapter = adapter;
        }
        this.mOldSelectedPosition = -1;
        this.mOldSelectedRowId = Long.MIN_VALUE;
        super.setAdapter(adapter);
        if (this.mAdapter != null) {
            int position;
            this.mAreAllItemsSelectable = this.mAdapter.areAllItemsEnabled();
            this.mOldItemCount = this.mItemCount;
            this.mItemCount = this.mAdapter.getCount();
            checkFocus();
            this.mDataSetObserver = new AdapterDataSetObserver();
            this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
            wrapObserver();
            this.mRecycler.setViewTypeCount(this.mAdapter.getViewTypeCount());
            if (this.mStackFromBottom) {
                position = lookForSelectablePosition(this.mItemCount - 1, false);
            } else {
                position = lookForSelectablePosition(0, true);
            }
            setSelectedPositionInt(position);
            setNextSelectedPositionInt(position);
            if (this.mItemCount == 0) {
                checkSelectionChanged();
            }
        } else {
            this.mAreAllItemsSelectable = true;
            checkFocus();
            checkSelectionChanged();
        }
        requestLayout();
    }

    void resetList() {
        clearRecycledState(this.mHeaderViewInfos);
        clearRecycledState(this.mFooterViewInfos);
        super.resetList();
        this.mLayoutMode = 0;
    }

    private void clearRecycledState(ArrayList<FixedViewInfo> infos) {
        if (infos != null) {
            int count = infos.size();
            for (int i = 0; i < count; i++) {
                LayoutParams params = ((FixedViewInfo) infos.get(i)).view.getLayoutParams();
                if (checkLayoutParams(params)) {
                    ((AbsListView.LayoutParams) params).recycledHeaderFooter = false;
                }
            }
        }
    }

    private boolean showingTopFadingEdge() {
        return this.mFirstPosition > 0 || getChildAt(0).getTop() > this.mScrollY + this.mListPadding.top;
    }

    private boolean showingBottomFadingEdge() {
        int childCount = getChildCount();
        int bottomOfBottomChild = getChildAt(childCount - 1).getBottom();
        int listBottom = (this.mScrollY + getHeight()) - this.mListPadding.bottom;
        if ((this.mFirstPosition + childCount) - 1 < this.mItemCount - 1 || bottomOfBottomChild < listBottom) {
            return true;
        }
        return false;
    }

    public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
        int rectTopWithinChild = rect.top;
        rect.offset(child.getLeft(), child.getTop());
        rect.offset(-child.getScrollX(), -child.getScrollY());
        int height = getHeight();
        int listUnfadedTop = getScrollY();
        int listUnfadedBottom = listUnfadedTop + height;
        int fadingEdge = getVerticalFadingEdgeLength();
        if (showingTopFadingEdge() && (this.mSelectedPosition > 0 || rectTopWithinChild > fadingEdge)) {
            listUnfadedTop += fadingEdge;
        }
        int bottomOfBottomChild = getChildAt(getChildCount() - 1).getBottom();
        if (showingBottomFadingEdge() && (this.mSelectedPosition < this.mItemCount - 1 || rect.bottom < bottomOfBottomChild - fadingEdge)) {
            listUnfadedBottom -= fadingEdge;
        }
        int scrollYDelta = 0;
        boolean z = false;
        if (rect.bottom > listUnfadedBottom && rect.top > listUnfadedTop) {
            if (rect.height() > height) {
                scrollYDelta = 0 + (rect.top - listUnfadedTop);
            } else {
                scrollYDelta = 0 + (rect.bottom - listUnfadedBottom);
            }
            scrollYDelta = Math.min(scrollYDelta, bottomOfBottomChild - listUnfadedBottom);
        } else if (rect.top < listUnfadedTop && rect.bottom < listUnfadedBottom) {
            if (rect.height() > height) {
                scrollYDelta = 0 - (listUnfadedBottom - rect.bottom);
            } else {
                scrollYDelta = 0 - (listUnfadedTop - rect.top);
            }
            scrollYDelta = Math.max(scrollYDelta, getChildAt(0).getTop() - listUnfadedTop);
        }
        if (scrollYDelta != 0) {
            z = true;
        }
        boolean scroll = z;
        if (scroll) {
            scrollListItemsBy(-scrollYDelta);
            positionSelector(-1, child);
            this.mSelectedTop = child.getTop();
            invalidate();
        }
        return scroll;
    }

    void fillGap(boolean down) {
        int count = getChildCount();
        int paddingTop;
        int startOffset;
        if (down) {
            paddingTop = 0;
            if ((this.mGroupFlags & 34) == 34) {
                paddingTop = getListPaddingTop();
            }
            if (count > 0) {
                startOffset = getChildAt(count - 1).getBottom() + this.mDividerHeight;
            } else {
                startOffset = paddingTop;
            }
            fillDown(this.mFirstPosition + count, startOffset);
            correctTooHigh(getChildCount());
            return;
        }
        paddingTop = 0;
        if ((this.mGroupFlags & 34) == 34) {
            paddingTop = getListPaddingBottom();
        }
        if (count > 0) {
            startOffset = getChildAt(0).getTop() - this.mDividerHeight;
        } else {
            startOffset = getHeight() - paddingTop;
        }
        fillUp(this.mFirstPosition - 1, startOffset);
        correctTooLow(getChildCount());
    }

    private View fillDown(int pos, int nextTop) {
        View selectedView = null;
        int end = this.mBottom - this.mTop;
        if ((this.mGroupFlags & 34) == 34) {
            end -= this.mListPadding.bottom;
        }
        while (true) {
            boolean z = true;
            if (nextTop >= end || pos >= this.mItemCount - this.mAnimOffset) {
                setVisibleRangeHint(this.mFirstPosition, (this.mFirstPosition + getChildCount()) - 1);
                onFirstPositionChange();
            } else {
                if (pos != this.mSelectedPosition) {
                    z = false;
                }
                boolean selected = z;
                View child = makeAndAddView(pos, nextTop, true, this.mListPadding.left, selected);
                nextTop = child.getBottom() + this.mDividerHeight;
                if (selected) {
                    selectedView = child;
                }
                pos++;
            }
        }
        setVisibleRangeHint(this.mFirstPosition, (this.mFirstPosition + getChildCount()) - 1);
        onFirstPositionChange();
        return selectedView;
    }

    private View fillUp(int pos, int nextBottom) {
        View selectedView = null;
        int end = 0;
        if ((this.mGroupFlags & 34) == 34) {
            end = this.mListPadding.top;
        }
        while (true) {
            boolean z = true;
            if (nextBottom <= end || pos < 0) {
                this.mFirstPosition = pos + 1;
                setVisibleRangeHint(this.mFirstPosition, (this.mFirstPosition + getChildCount()) - 1);
                onFirstPositionChange();
            } else {
                if (pos != this.mSelectedPosition) {
                    z = false;
                }
                boolean selected = z;
                View child = makeAndAddView(pos, nextBottom, false, this.mListPadding.left, selected);
                nextBottom = child.getTop() - this.mDividerHeight;
                if (selected) {
                    selectedView = child;
                }
                pos--;
            }
        }
        this.mFirstPosition = pos + 1;
        setVisibleRangeHint(this.mFirstPosition, (this.mFirstPosition + getChildCount()) - 1);
        onFirstPositionChange();
        return selectedView;
    }

    private View fillFromTop(int nextTop) {
        this.mFirstPosition = Math.min(this.mFirstPosition, this.mSelectedPosition);
        this.mFirstPosition = Math.min(this.mFirstPosition, this.mItemCount - 1);
        if (this.mFirstPosition < 0) {
            this.mFirstPosition = 0;
        }
        return fillDown(this.mFirstPosition, nextTop);
    }

    private View fillFromMiddle(int childrenTop, int childrenBottom) {
        int height = childrenBottom - childrenTop;
        int position = reconcileSelectedPosition();
        View sel = makeAndAddView(position, childrenTop, true, this.mListPadding.left, true);
        this.mFirstPosition = position;
        int selHeight = sel.getMeasuredHeight();
        if (selHeight <= height) {
            sel.offsetTopAndBottom((height - selHeight) / 2);
        }
        fillAboveAndBelow(sel, position);
        if (this.mStackFromBottom) {
            correctTooLow(getChildCount());
        } else {
            correctTooHigh(getChildCount());
        }
        return sel;
    }

    private void fillAboveAndBelow(View sel, int position) {
        int dividerHeight = this.mDividerHeight;
        if (this.mStackFromBottom) {
            fillDown(position + 1, sel.getBottom() + dividerHeight);
            adjustViewsUpOrDown();
            fillUp(position - 1, sel.getTop() - dividerHeight);
            return;
        }
        fillUp(position - 1, sel.getTop() - dividerHeight);
        adjustViewsUpOrDown();
        fillDown(position + 1, sel.getBottom() + dividerHeight);
    }

    private View fillFromSelection(int selectedTop, int childrenTop, int childrenBottom) {
        int fadingEdgeLength = getVerticalFadingEdgeLength();
        int selectedPosition = this.mSelectedPosition;
        int topSelectionPixel = getTopSelectionPixel(childrenTop, fadingEdgeLength, selectedPosition);
        int bottomSelectionPixel = getBottomSelectionPixel(childrenBottom, fadingEdgeLength, selectedPosition);
        View sel = makeAndAddView(selectedPosition, selectedTop, true, this.mListPadding.left, true);
        if (sel.getBottom() > bottomSelectionPixel) {
            sel.offsetTopAndBottom(-Math.min(sel.getTop() - topSelectionPixel, sel.getBottom() - bottomSelectionPixel));
        } else if (sel.getTop() < topSelectionPixel) {
            sel.offsetTopAndBottom(Math.min(topSelectionPixel - sel.getTop(), bottomSelectionPixel - sel.getBottom()));
        }
        fillAboveAndBelow(sel, selectedPosition);
        if (this.mStackFromBottom) {
            correctTooLow(getChildCount());
        } else {
            correctTooHigh(getChildCount());
        }
        return sel;
    }

    private int getBottomSelectionPixel(int childrenBottom, int fadingEdgeLength, int selectedPosition) {
        int bottomSelectionPixel = childrenBottom;
        if (selectedPosition != this.mItemCount - 1) {
            return bottomSelectionPixel - fadingEdgeLength;
        }
        return bottomSelectionPixel;
    }

    private int getTopSelectionPixel(int childrenTop, int fadingEdgeLength, int selectedPosition) {
        int topSelectionPixel = childrenTop;
        if (selectedPosition > 0) {
            return topSelectionPixel + fadingEdgeLength;
        }
        return topSelectionPixel;
    }

    @RemotableViewMethod
    public void smoothScrollToPosition(int position) {
        super.smoothScrollToPosition(position);
    }

    @RemotableViewMethod
    public void smoothScrollByOffset(int offset) {
        super.smoothScrollByOffset(offset);
    }

    private View moveSelection(View oldSel, View newSel, int delta, int childrenTop, int childrenBottom) {
        View sel;
        int i = childrenTop;
        int fadingEdgeLength = getVerticalFadingEdgeLength();
        int selectedPosition = this.mSelectedPosition;
        int topSelectionPixel = getTopSelectionPixel(i, fadingEdgeLength, selectedPosition);
        int bottomSelectionPixel = getBottomSelectionPixel(i, fadingEdgeLength, selectedPosition);
        int dividerHeight;
        if (delta > 0) {
            View oldSel2 = makeAndAddView(selectedPosition - 1, oldSel.getTop(), true, this.mListPadding.left, false);
            dividerHeight = this.mDividerHeight;
            sel = makeAndAddView(selectedPosition, oldSel2.getBottom() + dividerHeight, true, this.mListPadding.left, true);
            if (sel.getBottom() > bottomSelectionPixel) {
                int offset = Math.min(Math.min(sel.getTop() - topSelectionPixel, sel.getBottom() - bottomSelectionPixel), (childrenBottom - i) / 2);
                oldSel2.offsetTopAndBottom(-offset);
                sel.offsetTopAndBottom(-offset);
            }
            if (this.mStackFromBottom) {
                fillDown(this.mSelectedPosition + 1, sel.getBottom() + dividerHeight);
                adjustViewsUpOrDown();
                fillUp(this.mSelectedPosition - 2, sel.getTop() - dividerHeight);
            } else {
                fillUp(this.mSelectedPosition - 2, sel.getTop() - dividerHeight);
                adjustViewsUpOrDown();
                fillDown(this.mSelectedPosition + 1, sel.getBottom() + dividerHeight);
            }
            View view = oldSel2;
        } else {
            if (delta < 0) {
                if (newSel != null) {
                    sel = makeAndAddView(selectedPosition, newSel.getTop(), true, this.mListPadding.left, true);
                } else {
                    sel = makeAndAddView(selectedPosition, oldSel.getTop(), false, this.mListPadding.left, true);
                }
                if (sel.getTop() < topSelectionPixel) {
                    sel.offsetTopAndBottom(Math.min(Math.min(topSelectionPixel - sel.getTop(), bottomSelectionPixel - sel.getBottom()), (childrenBottom - i) / 2));
                }
                fillAboveAndBelow(sel, selectedPosition);
            } else {
                dividerHeight = oldSel.getTop();
                sel = makeAndAddView(selectedPosition, dividerHeight, true, this.mListPadding.left, true);
                if (dividerHeight < i && sel.getBottom() < i + 20) {
                    sel.offsetTopAndBottom(i - sel.getTop());
                }
                fillAboveAndBelow(sel, selectedPosition);
            }
        }
        return sel;
    }

    protected void onDetachedFromWindow() {
        if (this.mFocusSelector != null) {
            removeCallbacks(this.mFocusSelector);
            this.mFocusSelector = null;
        }
        super.onDetachedFromWindow();
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (getChildCount() > 0) {
            View focusedChild = getFocusedChild();
            if (focusedChild != null) {
                int childPosition = this.mFirstPosition + indexOfChild(focusedChild);
                int top = focusedChild.getTop() - Math.max(0, focusedChild.getBottom() - (h - this.mPaddingTop));
                if (this.mFocusSelector == null) {
                    this.mFocusSelector = new FocusSelector();
                }
                post(this.mFocusSelector.setupForSetSelection(childPosition, top));
            }
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSize;
        int i = widthMeasureSpec;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize2 = MeasureSpec.getSize(heightMeasureSpec);
        int childWidth = 0;
        int childHeight = 0;
        int childState = 0;
        this.mItemCount = this.mAdapter == null ? 0 : this.mAdapter.getCount();
        if (this.mItemCount > 0 && (widthMode == 0 || heightMode == 0)) {
            View child = obtainView(0, this.mIsScrap);
            measureScrapChild(child, 0, i, heightSize2);
            childWidth = child.getMeasuredWidth();
            childHeight = child.getMeasuredHeight();
            childState = View.combineMeasuredStates(0, child.getMeasuredState());
            if (recycleOnMeasure() && this.mRecycler.shouldRecycleViewType(((AbsListView.LayoutParams) child.getLayoutParams()).viewType)) {
                this.mRecycler.addScrapView(child, 0);
            }
        }
        int childWidth2 = childWidth;
        int childHeight2 = childHeight;
        int childState2 = childState;
        if (widthMode == 0) {
            childWidth = ((this.mListPadding.left + this.mListPadding.right) + childWidth2) + getVerticalScrollbarWidth();
        } else {
            childWidth = (-16777216 & childState2) | widthSize;
        }
        int widthSize2 = childWidth;
        if (heightMode == 0) {
            heightSize = ((this.mListPadding.top + this.mListPadding.bottom) + childHeight2) + (getVerticalFadingEdgeLength() * 2);
        } else {
            heightSize = heightSize2;
        }
        if (heightMode == Integer.MIN_VALUE) {
            heightSize = measureHeightOfChildren(i, 0, -1, heightSize, -1);
        }
        setMeasuredDimension(widthSize2, heightSize);
        this.mWidthMeasureSpec = i;
    }

    private void measureScrapChild(View child, int position, int widthMeasureSpec, int heightHint) {
        int childHeightSpec;
        AbsListView.LayoutParams p = (AbsListView.LayoutParams) child.getLayoutParams();
        if (p == null) {
            p = (AbsListView.LayoutParams) generateDefaultLayoutParams();
            child.setLayoutParams(p);
        }
        p.viewType = this.mAdapter.getItemViewType(position);
        p.isEnabled = this.mAdapter.isEnabled(position);
        p.forceAdd = true;
        int childWidthSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec, this.mListPadding.left + this.mListPadding.right, p.width);
        int lpHeight = p.height;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, 1073741824);
        } else {
            childHeightSpec = MeasureSpec.makeSafeMeasureSpec(heightHint, 0);
        }
        child.measure(childWidthSpec, childHeightSpec);
        child.forceLayout();
    }

    @ExportedProperty(category = "list")
    protected boolean recycleOnMeasure() {
        return true;
    }

    final int measureHeightOfChildren(int widthMeasureSpec, int startPosition, int endPosition, int maxHeight, int disallowPartialChildPosition) {
        int i = maxHeight;
        int i2 = disallowPartialChildPosition;
        ListAdapter adapter = this.mAdapter;
        if (adapter == null) {
            return this.mListPadding.top + this.mListPadding.bottom;
        }
        int returnedHeight = this.mListPadding.top + this.mListPadding.bottom;
        int dividerHeight = this.mDividerHeight;
        int i3 = endPosition;
        i3 = i3 == -1 ? adapter.getCount() - 1 : i3;
        RecycleBin recycleBin = this.mRecycler;
        boolean recyle = recycleOnMeasure();
        boolean[] isScrap = this.mIsScrap;
        int prevHeightWithoutPartialChild = 0;
        int returnedHeight2 = returnedHeight;
        returnedHeight = startPosition;
        while (returnedHeight <= i3) {
            View child = obtainView(returnedHeight, isScrap);
            measureScrapChild(child, returnedHeight, widthMeasureSpec, i);
            if (returnedHeight > 0) {
                returnedHeight2 += dividerHeight;
            }
            if (recyle && recycleBin.shouldRecycleViewType(((AbsListView.LayoutParams) child.getLayoutParams()).viewType)) {
                recycleBin.addScrapView(child, -1);
            }
            returnedHeight2 += child.getMeasuredHeight();
            if (returnedHeight2 >= i) {
                int i4 = (i2 < 0 || returnedHeight <= i2 || prevHeightWithoutPartialChild <= 0 || returnedHeight2 == i) ? i : prevHeightWithoutPartialChild;
                return i4;
            }
            if (i2 >= 0 && returnedHeight >= i2) {
                prevHeightWithoutPartialChild = returnedHeight2;
            }
            returnedHeight++;
        }
        int i5 = widthMeasureSpec;
        return returnedHeight2;
    }

    int findMotionRow(int y) {
        int childCount = getChildCount();
        if (childCount > 0) {
            int i;
            if (this.mStackFromBottom) {
                for (i = childCount - 1; i >= 0; i--) {
                    if (y >= getChildAt(i).getTop()) {
                        return this.mFirstPosition + i;
                    }
                }
            } else {
                for (i = 0; i < childCount; i++) {
                    if (y <= getChildAt(i).getBottom()) {
                        return this.mFirstPosition + i;
                    }
                }
            }
        }
        return -1;
    }

    private View fillSpecific(int position, int top) {
        View below;
        View above;
        boolean tempIsSelected = position == this.mSelectedPosition;
        View temp = makeAndAddView(position, top, true, this.mListPadding.left, tempIsSelected);
        this.mFirstPosition = position;
        int dividerHeight = this.mDividerHeight;
        int childCount;
        if (this.mStackFromBottom) {
            below = fillDown(position + 1, temp.getBottom() + dividerHeight);
            adjustViewsUpOrDown();
            above = fillUp(position - 1, temp.getTop() - dividerHeight);
            childCount = getChildCount();
            if (childCount > 0) {
                correctTooLow(childCount);
            }
        } else {
            above = fillUp(position - 1, temp.getTop() - dividerHeight);
            adjustViewsUpOrDown();
            below = fillDown(position + 1, temp.getBottom() + dividerHeight);
            childCount = getChildCount();
            if (childCount > 0) {
                correctTooHigh(childCount);
            }
        }
        if (tempIsSelected) {
            return temp;
        }
        if (above != null) {
            return above;
        }
        return below;
    }

    private void correctTooHigh(int childCount) {
        if ((this.mFirstPosition + childCount) - 1 == this.mItemCount - 1 && childCount > 0) {
            int bottomOffset = ((this.mBottom - this.mTop) - this.mListPadding.bottom) - getChildAt(childCount - 1).getBottom();
            View firstChild = getChildAt(null);
            int firstTop = firstChild.getTop();
            if (bottomOffset <= 0) {
                return;
            }
            if (this.mFirstPosition > 0 || firstTop < this.mListPadding.top) {
                if (this.mFirstPosition == 0) {
                    bottomOffset = Math.min(bottomOffset, this.mListPadding.top - firstTop);
                }
                offsetChildrenTopAndBottom(bottomOffset);
                if (this.mFirstPosition > 0) {
                    fillUp(this.mFirstPosition - 1, firstChild.getTop() - this.mDividerHeight);
                    adjustViewsUpOrDown();
                }
            }
        }
    }

    private void correctTooLow(int childCount) {
        if (this.mFirstPosition == 0 && childCount > 0) {
            int end = (this.mBottom - this.mTop) - this.mListPadding.bottom;
            int topOffset = getChildAt(null).getTop() - this.mListPadding.top;
            View lastChild = getChildAt(childCount - 1);
            int lastBottom = lastChild.getBottom();
            int lastPosition = (this.mFirstPosition + childCount) - 1;
            if (topOffset <= 0) {
                return;
            }
            if (lastPosition < this.mItemCount - 1 || lastBottom > end) {
                if (lastPosition == this.mItemCount - 1) {
                    topOffset = Math.min(topOffset, lastBottom - end);
                }
                offsetChildrenTopAndBottom(-topOffset);
                if (lastPosition < this.mItemCount - 1) {
                    fillDown(lastPosition + 1, lastChild.getBottom() + this.mDividerHeight);
                    adjustViewsUpOrDown();
                }
            } else if (lastPosition == this.mItemCount - 1) {
                adjustViewsUpOrDown();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:48:0x0093 A:{SYNTHETIC, Splitter:B:48:0x0093} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00ae A:{SYNTHETIC, Splitter:B:61:0x00ae} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x009a A:{SYNTHETIC, Splitter:B:53:0x009a} */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x0437  */
    /* JADX WARNING: Removed duplicated region for block: B:255:0x043e  */
    /* JADX WARNING: Removed duplicated region for block: B:253:0x0437  */
    /* JADX WARNING: Removed duplicated region for block: B:255:0x043e  */
    /* JADX WARNING: Missing block: B:93:0x010c, code skipped:
            if (r7.mAdapterHasStableIds == false) goto L_0x011a;
     */
    /* JADX WARNING: Missing block: B:130:0x01de, code skipped:
            r1 = r2;
     */
    /* JADX WARNING: Missing block: B:158:0x029b, code skipped:
            r9.scrapActiveViews();
            removeUnusedFixedViews(r7.mHeaderViewInfos);
            removeUnusedFixedViews(r7.mFooterViewInfos);
     */
    /* JADX WARNING: Missing block: B:159:0x02a9, code skipped:
            if (r1 == null) goto L_0x02f1;
     */
    /* JADX WARNING: Missing block: B:161:0x02ad, code skipped:
            if (r7.mItemsCanFocus == false) goto L_0x02e7;
     */
    /* JADX WARNING: Missing block: B:163:0x02b3, code skipped:
            if (hasFocus() == false) goto L_0x02e7;
     */
    /* JADX WARNING: Missing block: B:165:0x02b9, code skipped:
            if (r1.hasFocus() != false) goto L_0x02e7;
     */
    /* JADX WARNING: Missing block: B:166:0x02bb, code skipped:
            if (r1 != r0) goto L_0x02c5;
     */
    /* JADX WARNING: Missing block: B:167:0x02bd, code skipped:
            if (r8 == false) goto L_0x02c5;
     */
    /* JADX WARNING: Missing block: B:169:0x02c3, code skipped:
            if (r8.requestFocus() != false) goto L_0x02cb;
     */
    /* JADX WARNING: Missing block: B:171:0x02c9, code skipped:
            if (r1.requestFocus() == false) goto L_0x02cd;
     */
    /* JADX WARNING: Missing block: B:172:0x02cb, code skipped:
            r3 = true;
     */
    /* JADX WARNING: Missing block: B:173:0x02cd, code skipped:
            r3 = false;
     */
    /* JADX WARNING: Missing block: B:174:0x02ce, code skipped:
            if (r3 != false) goto L_0x02dd;
     */
    /* JADX WARNING: Missing block: B:175:0x02d0, code skipped:
            r4 = getFocusedChild();
     */
    /* JADX WARNING: Missing block: B:176:0x02d4, code skipped:
            if (r4 == null) goto L_0x02d9;
     */
    /* JADX WARNING: Missing block: B:177:0x02d6, code skipped:
            r4.clearFocus();
     */
    /* JADX WARNING: Missing block: B:178:0x02d9, code skipped:
            positionSelector(-1, r1);
     */
    /* JADX WARNING: Missing block: B:179:0x02dd, code skipped:
            r1.setSelected(false);
            r7.mSelectorRect.setEmpty();
     */
    /* JADX WARNING: Missing block: B:181:0x02e7, code skipped:
            positionSelector(-1, r1);
     */
    /* JADX WARNING: Missing block: B:182:0x02ea, code skipped:
            r7.mSelectedTop = r1.getTop();
     */
    /* JADX WARNING: Missing block: B:184:0x02f4, code skipped:
            if (r7.mTouchMode == 1) goto L_0x02fe;
     */
    /* JADX WARNING: Missing block: B:186:0x02f9, code skipped:
            if (r7.mTouchMode != 2) goto L_0x02fc;
     */
    /* JADX WARNING: Missing block: B:188:0x02fc, code skipped:
            r3 = false;
     */
    /* JADX WARNING: Missing block: B:189:0x02fe, code skipped:
            r3 = true;
     */
    /* JADX WARNING: Missing block: B:190:0x02ff, code skipped:
            if (r3 == false) goto L_0x0312;
     */
    /* JADX WARNING: Missing block: B:191:0x0301, code skipped:
            r4 = getChildAt(r7.mMotionPosition - r7.mFirstPosition);
     */
    /* JADX WARNING: Missing block: B:192:0x030a, code skipped:
            if (r4 == null) goto L_0x0311;
     */
    /* JADX WARNING: Missing block: B:193:0x030c, code skipped:
            positionSelector(r7.mMotionPosition, r4);
     */
    /* JADX WARNING: Missing block: B:196:0x0314, code skipped:
            if (r7.mSelectorPosition == -1) goto L_0x0327;
     */
    /* JADX WARNING: Missing block: B:197:0x0316, code skipped:
            r4 = getChildAt(r7.mSelectorPosition - r7.mFirstPosition);
     */
    /* JADX WARNING: Missing block: B:198:0x031f, code skipped:
            if (r4 == null) goto L_0x0326;
     */
    /* JADX WARNING: Missing block: B:199:0x0321, code skipped:
            positionSelector(r7.mSelectorPosition, r4);
     */
    /* JADX WARNING: Missing block: B:201:0x0327, code skipped:
            r7.mSelectedTop = 0;
            r7.mSelectorRect.setEmpty();
     */
    /* JADX WARNING: Missing block: B:203:0x0333, code skipped:
            if (hasFocus() == false) goto L_0x033a;
     */
    /* JADX WARNING: Missing block: B:204:0x0335, code skipped:
            if (r8 == false) goto L_0x033a;
     */
    /* JADX WARNING: Missing block: B:205:0x0337, code skipped:
            r8.requestFocus();
     */
    /* JADX WARNING: Missing block: B:206:0x033a, code skipped:
            r4 = r29;
     */
    /* JADX WARNING: Missing block: B:207:0x033c, code skipped:
            if (r4 == null) goto L_0x03a8;
     */
    /* JADX WARNING: Missing block: B:209:0x0342, code skipped:
            if (r4.getAccessibilityFocusedHost() != null) goto L_0x03a8;
     */
    /* JADX WARNING: Missing block: B:210:0x0344, code skipped:
            if (r28 == null) goto L_0x0383;
     */
    /* JADX WARNING: Missing block: B:211:0x0346, code skipped:
            r6 = r28;
     */
    /* JADX WARNING: Missing block: B:212:0x034c, code skipped:
            if (r6.isAttachedToWindow() == false) goto L_0x037c;
     */
    /* JADX WARNING: Missing block: B:213:0x034e, code skipped:
            r2 = r6.getAccessibilityNodeProvider();
     */
    /* JADX WARNING: Missing block: B:214:0x0353, code skipped:
            if (r27 == null) goto L_0x036f;
     */
    /* JADX WARNING: Missing block: B:215:0x0355, code skipped:
            if (r2 == null) goto L_0x036f;
     */
    /* JADX WARNING: Missing block: B:216:0x0357, code skipped:
            r30 = r0;
            r31 = r1;
            r3 = r27;
            r32 = r3;
            r2.performAction(android.view.accessibility.AccessibilityNodeInfo.getVirtualDescendantId(r3.getSourceNodeId()), 64, null);
     */
    /* JADX WARNING: Missing block: B:217:0x036f, code skipped:
            r30 = r0;
            r31 = r1;
            r32 = r27;
            r6.requestAccessibilityFocus();
     */
    /* JADX WARNING: Missing block: B:218:0x0378, code skipped:
            r3 = r26;
     */
    /* JADX WARNING: Missing block: B:219:0x037c, code skipped:
            r30 = r0;
            r31 = r1;
            r32 = r27;
     */
    /* JADX WARNING: Missing block: B:220:0x0383, code skipped:
            r30 = r0;
            r31 = r1;
            r32 = r27;
            r6 = r28;
     */
    /* JADX WARNING: Missing block: B:221:0x038b, code skipped:
            r3 = r26;
     */
    /* JADX WARNING: Missing block: B:222:0x038d, code skipped:
            if (r3 == -1) goto L_0x03b2;
     */
    /* JADX WARNING: Missing block: B:223:0x038f, code skipped:
            r1 = getChildAt(android.util.MathUtils.constrain(r3 - r7.mFirstPosition, 0, getChildCount() - 1));
     */
    /* JADX WARNING: Missing block: B:224:0x03a2, code skipped:
            if (r1 == null) goto L_0x03b2;
     */
    /* JADX WARNING: Missing block: B:225:0x03a4, code skipped:
            r1.requestAccessibilityFocus();
     */
    /* JADX WARNING: Missing block: B:226:0x03a8, code skipped:
            r30 = r0;
            r31 = r1;
            r3 = r26;
            r32 = r27;
            r6 = r28;
     */
    /* JADX WARNING: Missing block: B:227:0x03b2, code skipped:
            if (r8 == false) goto L_0x03bd;
     */
    /* JADX WARNING: Missing block: B:229:0x03b8, code skipped:
            if (r8.getWindowToken() == null) goto L_0x03bd;
     */
    /* JADX WARNING: Missing block: B:230:0x03ba, code skipped:
            r8.dispatchFinishTemporaryDetach();
     */
    /* JADX WARNING: Missing block: B:231:0x03bd, code skipped:
            r7.mLayoutMode = 0;
            r7.mDataChanged = false;
     */
    /* JADX WARNING: Missing block: B:232:0x03c4, code skipped:
            if (r7.mPositionScrollAfterLayout == null) goto L_0x03ce;
     */
    /* JADX WARNING: Missing block: B:233:0x03c6, code skipped:
            post(r7.mPositionScrollAfterLayout);
            r7.mPositionScrollAfterLayout = null;
     */
    /* JADX WARNING: Missing block: B:234:0x03ce, code skipped:
            r7.mNeedSync = false;
            setNextSelectedPositionInt(r7.mSelectedPosition);
            updateScrollIndicators();
     */
    /* JADX WARNING: Missing block: B:235:0x03db, code skipped:
            if (r7.mItemCount <= 0) goto L_0x03e0;
     */
    /* JADX WARNING: Missing block: B:236:0x03dd, code skipped:
            checkSelectionChanged();
     */
    /* JADX WARNING: Missing block: B:237:0x03e0, code skipped:
            invokeOnItemScrollListener();
     */
    /* JADX WARNING: Missing block: B:239:0x03e5, code skipped:
            if (r7.mFocusSelector == null) goto L_0x03ec;
     */
    /* JADX WARNING: Missing block: B:240:0x03e7, code skipped:
            r7.mFocusSelector.onLayoutComplete();
     */
    /* JADX WARNING: Missing block: B:241:0x03ec, code skipped:
            if (r25 != false) goto L_0x03f1;
     */
    /* JADX WARNING: Missing block: B:242:0x03ee, code skipped:
            r7.mBlockLayoutRequests = false;
     */
    /* JADX WARNING: Missing block: B:243:0x03f1, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void layoutChildren() {
        Throwable th;
        boolean blockLayoutRequests;
        boolean blockLayoutRequests2 = this.mBlockLayoutRequests;
        if (!blockLayoutRequests2) {
            this.mBlockLayoutRequests = true;
            super.layoutChildren();
            invalidate();
            if (this.mAdapter == null) {
                try {
                    resetList();
                    invokeOnItemScrollListener();
                    if (this.mFocusSelector != null) {
                        this.mFocusSelector.onLayoutComplete();
                    }
                    if (!blockLayoutRequests2) {
                        this.mBlockLayoutRequests = false;
                    }
                    return;
                } catch (Throwable th2) {
                    th = th2;
                    blockLayoutRequests = blockLayoutRequests2;
                    if (this.mFocusSelector != null) {
                    }
                    if (!blockLayoutRequests) {
                    }
                    throw th;
                }
            }
            int delta;
            View oldSel;
            View oldFirst;
            View newSel;
            boolean dataChanged;
            int childrenTop = this.mListPadding.top;
            int childrenBottom = (this.mBottom - this.mTop) - this.mListPadding.bottom;
            int childCount = getChildCount();
            int index = 0;
            int delta2 = 0;
            View oldSel2 = null;
            View oldFirst2 = null;
            View newSel2 = null;
            switch (this.mLayoutMode) {
                case 2:
                    index = this.mNextSelectedPosition - this.mFirstPosition;
                    if (index >= 0 && index < childCount) {
                        newSel2 = getChildAt(index);
                    }
                case 1:
                case 3:
                case 4:
                case 5:
                    delta = delta2;
                    oldSel = oldSel2;
                    oldFirst = oldFirst2;
                    newSel = newSel2;
                    dataChanged = this.mDataChanged;
                    if (dataChanged) {
                        handleDataChanged();
                    }
                    if (this.mItemCount != 0) {
                        resetList();
                        invokeOnItemScrollListener();
                        if (this.mFocusSelector != null) {
                            this.mFocusSelector.onLayoutComplete();
                        }
                        if (!blockLayoutRequests2) {
                            this.mBlockLayoutRequests = false;
                        }
                        return;
                    } else if (this.mItemCount == this.mAdapter.getCount()) {
                        int accessibilityFocusPosition;
                        View focusLayoutRestoreDirectChild;
                        View focusedChild;
                        int accessibilityFocusPosition2;
                        View accessibilityFocusLayoutRestoreView;
                        setSelectedPositionInt(this.mNextSelectedPosition);
                        AccessibilityNodeInfo accessibilityFocusLayoutRestoreNode = null;
                        View accessibilityFocusLayoutRestoreView2 = null;
                        int accessibilityFocusPosition3 = -1;
                        ViewRootImpl viewRootImpl = getViewRootImpl();
                        if (viewRootImpl != null) {
                            accessibilityFocusPosition = viewRootImpl.getAccessibilityFocusedHost();
                            if (accessibilityFocusPosition != 0) {
                                newSel2 = getAccessibilityFocusedChild(accessibilityFocusPosition);
                                if (newSel2 != null) {
                                    if (!dataChanged || isDirectChildHeaderOrFooter(newSel2) || (newSel2.hasTransientState() && this.mAdapterHasStableIds)) {
                                        accessibilityFocusLayoutRestoreView2 = accessibilityFocusPosition;
                                        accessibilityFocusLayoutRestoreNode = viewRootImpl.getAccessibilityFocusedVirtualView();
                                    }
                                    accessibilityFocusPosition3 = getPositionForView(newSel2);
                                }
                            }
                        }
                        AccessibilityNodeInfo accessibilityFocusLayoutRestoreNode2 = accessibilityFocusLayoutRestoreNode;
                        newSel2 = accessibilityFocusLayoutRestoreView2;
                        accessibilityFocusPosition = accessibilityFocusPosition3;
                        View focusLayoutRestoreDirectChild2 = null;
                        accessibilityFocusLayoutRestoreView2 = null;
                        oldSel2 = getFocusedChild();
                        if (oldSel2 != null) {
                            if (dataChanged) {
                                if (!isDirectChildHeaderOrFooter(oldSel2)) {
                                    if (!oldSel2.hasTransientState()) {
                                        break;
                                    }
                                }
                            }
                            focusLayoutRestoreDirectChild = oldSel2;
                            focusLayoutRestoreDirectChild2 = findFocus();
                            if (focusLayoutRestoreDirectChild2 != null) {
                                focusLayoutRestoreDirectChild2.dispatchStartTemporaryDetach();
                            }
                            accessibilityFocusLayoutRestoreView2 = focusLayoutRestoreDirectChild2;
                            focusLayoutRestoreDirectChild2 = focusLayoutRestoreDirectChild;
                            requestFocus();
                        }
                        focusLayoutRestoreDirectChild = focusLayoutRestoreDirectChild2;
                        index = this.mFirstPosition;
                        View focusLayoutRestoreView = accessibilityFocusLayoutRestoreView2;
                        RecycleBin recycleBin = this.mRecycler;
                        if (dataChanged) {
                            int i = 0;
                            while (true) {
                                focusedChild = oldSel2;
                                oldSel2 = i;
                                if (oldSel2 < childCount) {
                                    accessibilityFocusPosition2 = accessibilityFocusPosition;
                                    accessibilityFocusLayoutRestoreView = newSel2;
                                    recycleBin.addScrapView(getChildAt(oldSel2), index + oldSel2);
                                    i = oldSel2 + 1;
                                    oldSel2 = focusedChild;
                                    accessibilityFocusPosition = accessibilityFocusPosition2;
                                    newSel2 = accessibilityFocusLayoutRestoreView;
                                } else {
                                    accessibilityFocusPosition2 = accessibilityFocusPosition;
                                    accessibilityFocusLayoutRestoreView = newSel2;
                                }
                            }
                        } else {
                            focusedChild = oldSel2;
                            accessibilityFocusPosition2 = accessibilityFocusPosition;
                            accessibilityFocusLayoutRestoreView = newSel2;
                            recycleBin.fillActiveViews(childCount, index);
                        }
                        detachAllViewsFromParent();
                        recycleBin.removeSkippedScrap();
                        ViewRootImpl viewRootImpl2;
                        AccessibilityNodeInfo accessibilityFocusLayoutRestoreNode3;
                        int accessibilityFocusPosition4;
                        View accessibilityFocusLayoutRestoreView3;
                        RecycleBin recycleBin2;
                        switch (this.mLayoutMode) {
                            case 1:
                                viewRootImpl2 = viewRootImpl;
                                blockLayoutRequests = blockLayoutRequests2;
                                accessibilityFocusLayoutRestoreNode3 = accessibilityFocusLayoutRestoreNode2;
                                blockLayoutRequests2 = focusLayoutRestoreView;
                                accessibilityFocusPosition4 = accessibilityFocusPosition2;
                                accessibilityFocusLayoutRestoreView3 = accessibilityFocusLayoutRestoreView;
                                recycleBin2 = recycleBin;
                                this.mFirstPosition = 0;
                                focusLayoutRestoreDirectChild2 = fillFromTop(childrenTop);
                                adjustViewsUpOrDown();
                                break;
                            case 2:
                                viewRootImpl2 = viewRootImpl;
                                blockLayoutRequests = blockLayoutRequests2;
                                accessibilityFocusLayoutRestoreNode3 = accessibilityFocusLayoutRestoreNode2;
                                blockLayoutRequests2 = focusLayoutRestoreView;
                                focusLayoutRestoreView = focusedChild;
                                accessibilityFocusPosition4 = accessibilityFocusPosition2;
                                accessibilityFocusLayoutRestoreView3 = accessibilityFocusLayoutRestoreView;
                                recycleBin2 = recycleBin;
                                if (newSel == null) {
                                    focusLayoutRestoreDirectChild2 = fillFromMiddle(childrenTop, childrenBottom);
                                    break;
                                }
                                focusLayoutRestoreDirectChild2 = fillFromSelection(newSel.getTop(), childrenTop, childrenBottom);
                            case 3:
                                viewRootImpl2 = viewRootImpl;
                                blockLayoutRequests = blockLayoutRequests2;
                                accessibilityFocusLayoutRestoreNode3 = accessibilityFocusLayoutRestoreNode2;
                                blockLayoutRequests2 = focusLayoutRestoreView;
                                focusLayoutRestoreView = focusedChild;
                                accessibilityFocusPosition4 = accessibilityFocusPosition2;
                                accessibilityFocusLayoutRestoreView3 = accessibilityFocusLayoutRestoreView;
                                recycleBin2 = recycleBin;
                                focusLayoutRestoreDirectChild2 = fillUp(this.mItemCount - 1, childrenBottom);
                                adjustViewsUpOrDown();
                                break;
                            case 4:
                                viewRootImpl2 = viewRootImpl;
                                blockLayoutRequests = blockLayoutRequests2;
                                accessibilityFocusLayoutRestoreNode3 = accessibilityFocusLayoutRestoreNode2;
                                blockLayoutRequests2 = focusLayoutRestoreView;
                                focusLayoutRestoreView = focusedChild;
                                accessibilityFocusPosition4 = accessibilityFocusPosition2;
                                accessibilityFocusLayoutRestoreView3 = accessibilityFocusLayoutRestoreView;
                                recycleBin2 = recycleBin;
                                index = reconcileSelectedPosition();
                                recycleBin = fillSpecific(index, this.mSpecificTop);
                                if (recycleBin == null && this.mFocusSelector != null) {
                                    Runnable focusRunnable = this.mFocusSelector.setupFocusIfValid(index);
                                    if (focusRunnable != null) {
                                        post(focusRunnable);
                                    }
                                    break;
                                }
                            case 5:
                                viewRootImpl2 = viewRootImpl;
                                blockLayoutRequests = blockLayoutRequests2;
                                accessibilityFocusLayoutRestoreNode3 = accessibilityFocusLayoutRestoreNode2;
                                blockLayoutRequests2 = focusLayoutRestoreView;
                                focusLayoutRestoreView = focusedChild;
                                accessibilityFocusPosition4 = accessibilityFocusPosition2;
                                accessibilityFocusLayoutRestoreView3 = accessibilityFocusLayoutRestoreView;
                                recycleBin2 = recycleBin;
                                focusLayoutRestoreDirectChild2 = fillSpecific(this.mSyncPosition, this.mSpecificTop);
                                break;
                            case 6:
                                oldFirst2 = focusLayoutRestoreView;
                                focusLayoutRestoreView = focusedChild;
                                blockLayoutRequests = blockLayoutRequests2;
                                accessibilityFocusPosition4 = accessibilityFocusPosition2;
                                blockLayoutRequests2 = oldFirst2;
                                accessibilityFocusLayoutRestoreNode3 = accessibilityFocusLayoutRestoreNode2;
                                accessibilityFocusLayoutRestoreView3 = accessibilityFocusLayoutRestoreView;
                                recycleBin2 = recycleBin;
                                viewRootImpl2 = viewRootImpl;
                                focusLayoutRestoreDirectChild2 = moveSelection(oldSel, newSel, delta, childrenTop, childrenBottom);
                                break;
                            default:
                                viewRootImpl2 = viewRootImpl;
                                blockLayoutRequests = blockLayoutRequests2;
                                accessibilityFocusLayoutRestoreNode3 = accessibilityFocusLayoutRestoreNode2;
                                blockLayoutRequests2 = focusLayoutRestoreView;
                                focusLayoutRestoreView = focusedChild;
                                accessibilityFocusPosition4 = accessibilityFocusPosition2;
                                accessibilityFocusLayoutRestoreView3 = accessibilityFocusLayoutRestoreView;
                                recycleBin2 = recycleBin;
                                if (childCount == 0) {
                                    try {
                                        if (this.mStackFromBottom != null) {
                                            setSelectedPositionInt(lookForSelectablePosition(this.mItemCount - 1, false));
                                            focusLayoutRestoreDirectChild2 = fillUp(this.mItemCount - 1, childrenBottom);
                                            break;
                                        }
                                        setSelectedPositionInt(lookForSelectablePosition(0, true));
                                        focusLayoutRestoreDirectChild2 = fillFromTop(childrenTop);
                                        break;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        if (this.mFocusSelector != null) {
                                        }
                                        if (blockLayoutRequests) {
                                        }
                                        throw th;
                                    }
                                } else if (this.mSelectedPosition < 0 || this.mSelectedPosition >= this.mItemCount) {
                                    if (this.mFirstPosition >= this.mItemCount) {
                                        recycleBin = fillSpecific(0, childrenTop);
                                        break;
                                    }
                                    index = this.mFirstPosition;
                                    if (oldFirst == null) {
                                        delta2 = childrenTop;
                                    } else {
                                        delta2 = oldFirst.getTop();
                                    }
                                    focusLayoutRestoreDirectChild2 = fillSpecific(index, delta2);
                                } else {
                                    index = this.mSelectedPosition;
                                    if (oldSel == null) {
                                        delta2 = childrenTop;
                                    } else {
                                        delta2 = oldSel.getTop();
                                    }
                                    focusLayoutRestoreDirectChild2 = fillSpecific(index, delta2);
                                }
                                break;
                        }
                    } else {
                        blockLayoutRequests = blockLayoutRequests2;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("The content of the adapter has changed but ListView did not receive a notification. Make sure the content of your adapter is not modified from a background thread, but only from the UI thread. Make sure your adapter calls notifyDataSetChanged() when its content changes. [in ListView(");
                        stringBuilder.append(getId());
                        stringBuilder.append(", ");
                        stringBuilder.append(getClass());
                        stringBuilder.append(") with Adapter(");
                        stringBuilder.append(this.mAdapter.getClass());
                        stringBuilder.append(")]");
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                    break;
                default:
                    index = this.mSelectedPosition - this.mFirstPosition;
                    if (index >= 0 && index < childCount) {
                        oldSel2 = getChildAt(index);
                    }
                    try {
                        oldFirst2 = getChildAt(0);
                        if (this.mNextSelectedPosition >= 0) {
                            delta2 = this.mNextSelectedPosition - this.mSelectedPosition;
                        }
                        newSel2 = getChildAt(index + delta2);
                    } catch (Throwable th4) {
                        th = th4;
                        blockLayoutRequests = blockLayoutRequests2;
                        if (this.mFocusSelector != null) {
                            this.mFocusSelector.onLayoutComplete();
                        }
                        if (blockLayoutRequests) {
                            this.mBlockLayoutRequests = false;
                        }
                        throw th;
                    }
            }
            delta = delta2;
            oldSel = oldSel2;
            oldFirst = oldFirst2;
            newSel = newSel2;
            dataChanged = this.mDataChanged;
            if (dataChanged) {
            }
            if (this.mItemCount != 0) {
            }
        }
    }

    boolean trackMotionScroll(int deltaY, int incrementalDeltaY) {
        boolean result = super.trackMotionScroll(deltaY, incrementalDeltaY);
        removeUnusedFixedViews(this.mHeaderViewInfos);
        removeUnusedFixedViews(this.mFooterViewInfos);
        return result;
    }

    private void removeUnusedFixedViews(List<FixedViewInfo> infoList) {
        if (infoList != null) {
            for (int i = infoList.size() - 1; i >= 0; i--) {
                View view = ((FixedViewInfo) infoList.get(i)).view;
                AbsListView.LayoutParams lp = (AbsListView.LayoutParams) view.getLayoutParams();
                if (view.getParent() == null && lp != null && lp.recycledHeaderFooter) {
                    removeDetachedView(view, false);
                    lp.recycledHeaderFooter = false;
                }
            }
        }
    }

    private boolean isDirectChildHeaderOrFooter(View child) {
        ArrayList<FixedViewInfo> headers = this.mHeaderViewInfos;
        int numHeaders = headers.size();
        for (int i = 0; i < numHeaders; i++) {
            if (child == ((FixedViewInfo) headers.get(i)).view) {
                return true;
            }
        }
        ArrayList<FixedViewInfo> footers = this.mFooterViewInfos;
        int numFooters = footers.size();
        for (int i2 = 0; i2 < numFooters; i2++) {
            if (child == ((FixedViewInfo) footers.get(i2)).view) {
                return true;
            }
        }
        return false;
    }

    private View makeAndAddView(int position, int y, boolean flow, int childrenLeft, boolean selected) {
        View activeView;
        int i = position;
        if (!this.mDataChanged) {
            activeView = this.mRecycler.getActiveView(i);
            if (activeView != null) {
                setupChild(activeView, i, y, flow, childrenLeft, selected, true);
                return activeView;
            }
        }
        UniPerf.getInstance().uniPerfEvent(4116, "", new int[]{0});
        this.mAddItemViewType = this.mAdapter.getItemViewType(i);
        this.mAddItemViewPosition = i;
        activeView = obtainView(i, this.mIsScrap);
        UniPerf.getInstance().uniPerfEvent(4116, "", new int[]{-1});
        setupChild(activeView, i, y, flow, childrenLeft, selected, this.mIsScrap[0]);
        this.mAddItemViewType = -10000;
        this.mAddItemViewPosition = -1;
        return activeView;
    }

    private void setupChild(View child, int position, int y, boolean flowDown, int childrenLeft, boolean selected, boolean isAttachedToWindow) {
        int childWidthSpec;
        int childHeightSpec;
        View view = child;
        int i = position;
        int i2 = childrenLeft;
        Trace.traceBegin(8, "setupListItem");
        boolean isSelected = selected && shouldShowSelector();
        boolean updateChildSelected = isSelected != child.isSelected();
        int mode = this.mTouchMode;
        boolean isPressed = mode > 0 && mode < 3 && this.mMotionPosition == i;
        boolean updateChildPressed = isPressed != child.isPressed();
        boolean needToMeasure = !isAttachedToWindow || updateChildSelected || child.isLayoutRequested();
        AbsListView.LayoutParams p = (AbsListView.LayoutParams) child.getLayoutParams();
        if (p == null) {
            p = (AbsListView.LayoutParams) generateDefaultLayoutParams();
        }
        AbsListView.LayoutParams p2 = p;
        p2.viewType = getHwItemViewType(i);
        p2.isEnabled = this.mAdapter.isEnabled(i);
        if (updateChildSelected) {
            view.setSelected(isSelected);
        }
        if (updateChildPressed) {
            view.setPressed(isPressed);
        }
        if (!(this.mChoiceMode == 0 || this.mCheckStates == null)) {
            if (view instanceof Checkable) {
                ((Checkable) view).setChecked(this.mCheckStates.get(i));
            } else if (getContext().getApplicationInfo().targetSdkVersion >= 11) {
                view.setActivated(this.mCheckStates.get(i));
            }
        }
        if ((!isAttachedToWindow || p2.forceAdd) && !(p2.recycledHeaderFooter && p2.viewType == -2)) {
            boolean z;
            p2.forceAdd = false;
            if (p2.viewType == -2) {
                z = true;
                p2.recycledHeaderFooter = true;
            } else {
                z = true;
            }
            addViewInLayout(view, flowDown ? -1 : 0, p2, z);
            child.resolveRtlPropertiesIfNeeded();
        } else {
            attachViewToParent(view, flowDown ? -1 : 0, p2);
            if (isAttachedToWindow && ((AbsListView.LayoutParams) child.getLayoutParams()).scrappedFromPosition != i) {
                child.jumpDrawablesToCurrentState();
            }
        }
        if (needToMeasure) {
            childWidthSpec = ViewGroup.getChildMeasureSpec(this.mWidthMeasureSpec, this.mListPadding.left + this.mListPadding.right, p2.width);
            int lpHeight = p2.height;
            if (lpHeight > 0) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, 1073741824);
            } else {
                childHeightSpec = MeasureSpec.makeSafeMeasureSpec(getMeasuredHeight(), 0);
            }
            view.measure(childWidthSpec, childHeightSpec);
        } else {
            cleanupLayoutState(child);
        }
        i = child.getMeasuredWidth();
        childWidthSpec = child.getMeasuredHeight();
        childHeightSpec = flowDown ? y : y - childWidthSpec;
        if (needToMeasure) {
            view.layout(i2, childHeightSpec, i2 + i, childHeightSpec + childWidthSpec);
        } else {
            view.offsetLeftAndRight(i2 - child.getLeft());
            view.offsetTopAndBottom(childHeightSpec - child.getTop());
        }
        if (this.mCachingStarted && !child.isDrawingCacheEnabled()) {
            view.setDrawingCacheEnabled(true);
        }
        Trace.traceEnd(8);
    }

    protected boolean canAnimate() {
        return super.canAnimate() && this.mItemCount > 0;
    }

    public void setSelection(int position) {
        setSelectionFromTop(position, 0);
    }

    void setSelectionInt(int position) {
        setNextSelectedPositionInt(position);
        boolean awakeScrollbars = false;
        int selectedPosition = this.mSelectedPosition;
        if (selectedPosition >= 0) {
            if (position == selectedPosition - 1) {
                awakeScrollbars = true;
            } else if (position == selectedPosition + 1) {
                awakeScrollbars = true;
            }
        }
        if (this.mPositionScroller != null) {
            this.mPositionScroller.stop();
        }
        layoutChildren();
        if (awakeScrollbars) {
            awakenScrollBars();
        }
    }

    int lookForSelectablePosition(int position, boolean lookDown) {
        ListAdapter adapter = this.mAdapter;
        if (adapter == null || isInTouchMode()) {
            return -1;
        }
        int count = adapter.getCount();
        if (!this.mAreAllItemsSelectable) {
            if (lookDown) {
                position = Math.max(0, position);
                while (position < count && !adapter.isEnabled(position)) {
                    position++;
                }
            } else {
                position = Math.min(position, count - 1);
                while (position >= 0 && !adapter.isEnabled(position)) {
                    position--;
                }
            }
        }
        if (position < 0 || position >= count) {
            return -1;
        }
        return position;
    }

    int lookForSelectablePositionAfter(int current, int position, boolean lookDown) {
        ListAdapter adapter = this.mAdapter;
        if (adapter == null || isInTouchMode()) {
            return -1;
        }
        int after = lookForSelectablePosition(position, lookDown);
        if (after != -1) {
            return after;
        }
        int count = adapter.getCount();
        current = MathUtils.constrain(current, -1, count - 1);
        if (lookDown) {
            position = Math.min(position - 1, count - 1);
            while (position > current && !adapter.isEnabled(position)) {
                position--;
            }
            if (position <= current) {
                return -1;
            }
        }
        position = Math.max(0, position + 1);
        while (position < current && !adapter.isEnabled(position)) {
            position++;
        }
        if (position >= current) {
            return -1;
        }
        return position;
    }

    public void setSelectionAfterHeaderView() {
        int count = getHeaderViewsCount();
        if (count > 0) {
            this.mNextSelectedPosition = 0;
            return;
        }
        if (this.mAdapter != null) {
            setSelection(count);
        } else {
            this.mNextSelectedPosition = count;
            this.mLayoutMode = 2;
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = super.dispatchKeyEvent(event);
        if (handled || getFocusedChild() == null || event.getAction() != 0) {
            return handled;
        }
        return onKeyDown(event.getKeyCode(), event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return commonKey(keyCode, 1, event);
    }

    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return commonKey(keyCode, repeatCount, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return commonKey(keyCode, 1, event);
    }

    /* JADX WARNING: Missing block: B:128:0x017a, code skipped:
            r9 = r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean commonKey(int keyCode, int count, KeyEvent event) {
        if (this.mAdapter == null || !isAttachedToWindow()) {
            return false;
        }
        if (this.mDataChanged) {
            layoutChildren();
        }
        boolean handled = false;
        int action = event.getAction();
        if (KeyEvent.isConfirmKey(keyCode) && event.hasNoModifiers() && action != 1) {
            handled = resurrectSelectionIfNeeded();
            if (!handled && event.getRepeatCount() == 0 && getChildCount() > 0) {
                keyPressed();
                handled = true;
            }
        }
        if (!(handled || action == 1)) {
            boolean z;
            int count2;
            switch (keyCode) {
                case 19:
                    if (!event.hasNoModifiers()) {
                        if (event.hasModifiers(2)) {
                            z = resurrectSelectionIfNeeded() || fullScroll(33);
                            handled = z;
                            break;
                        }
                    }
                    handled = resurrectSelectionIfNeeded();
                    if (!handled) {
                        while (true) {
                            count2 = count - 1;
                            if (count > 0 && arrowScroll(33)) {
                                handled = true;
                                count = count2;
                            }
                        }
                    }
                    break;
                case 20:
                    if (!event.hasNoModifiers()) {
                        if (event.hasModifiers(2)) {
                            z = resurrectSelectionIfNeeded() || fullScroll(130);
                            handled = z;
                            break;
                        }
                    }
                    handled = resurrectSelectionIfNeeded();
                    if (!handled) {
                        while (true) {
                            count2 = count - 1;
                            if (count > 0 && arrowScroll(130)) {
                                handled = true;
                                count = count2;
                            }
                        }
                    }
                    break;
                case 21:
                    if (event.hasNoModifiers()) {
                        handled = handleHorizontalFocusWithinListItem(17);
                        break;
                    }
                    break;
                case 22:
                    if (event.hasNoModifiers()) {
                        handled = handleHorizontalFocusWithinListItem(66);
                        break;
                    }
                    break;
                case 61:
                    if (!event.hasNoModifiers()) {
                        if (event.hasModifiers(1)) {
                            z = resurrectSelectionIfNeeded() || arrowScroll(33);
                            handled = z;
                            break;
                        }
                    }
                    z = resurrectSelectionIfNeeded() || arrowScroll(130);
                    handled = z;
                    break;
                    break;
                case 92:
                    if (!event.hasNoModifiers()) {
                        if (event.hasModifiers(2)) {
                            z = resurrectSelectionIfNeeded() || fullScroll(33);
                            handled = z;
                            break;
                        }
                    }
                    z = resurrectSelectionIfNeeded() || pageScroll(33);
                    handled = z;
                    break;
                    break;
                case 93:
                    if (!event.hasNoModifiers()) {
                        if (event.hasModifiers(2)) {
                            z = resurrectSelectionIfNeeded() || fullScroll(130);
                            handled = z;
                            break;
                        }
                    }
                    z = resurrectSelectionIfNeeded() || pageScroll(130);
                    handled = z;
                    break;
                    break;
                case 122:
                    if (event.hasNoModifiers()) {
                        z = resurrectSelectionIfNeeded() || fullScroll(33);
                        handled = z;
                        break;
                    }
                    break;
                case 123:
                    if (event.hasNoModifiers()) {
                        z = resurrectSelectionIfNeeded() || fullScroll(130);
                        handled = z;
                        break;
                    }
                    break;
            }
        }
        if (handled || sendToTextFilter(keyCode, count, event)) {
            return true;
        }
        switch (action) {
            case 0:
                return super.onKeyDown(keyCode, event);
            case 1:
                return super.onKeyUp(keyCode, event);
            case 2:
                return super.onKeyMultiple(keyCode, count, event);
            default:
                return false;
        }
    }

    boolean pageScroll(int direction) {
        int nextPage;
        boolean down;
        if (direction == 33) {
            nextPage = Math.max(0, (this.mSelectedPosition - getChildCount()) - 1);
            down = false;
        } else if (direction != 130) {
            return false;
        } else {
            nextPage = Math.min(this.mItemCount - 1, (this.mSelectedPosition + getChildCount()) - 1);
            down = true;
        }
        if (nextPage >= 0) {
            int position = lookForSelectablePositionAfter(this.mSelectedPosition, nextPage, down);
            if (position >= 0) {
                this.mLayoutMode = 4;
                this.mSpecificTop = this.mPaddingTop + getVerticalFadingEdgeLength();
                if (down && position > this.mItemCount - getChildCount()) {
                    this.mLayoutMode = 3;
                }
                if (!down && position < getChildCount()) {
                    this.mLayoutMode = 1;
                }
                setSelectionInt(position);
                invokeOnItemScrollListener();
                if (!awakenScrollBars()) {
                    invalidate();
                }
                return true;
            }
        }
        return false;
    }

    boolean fullScroll(int direction) {
        boolean moved = false;
        int position;
        if (direction == 33) {
            if (this.mSelectedPosition != 0) {
                position = lookForSelectablePositionAfter(this.mSelectedPosition, 0, true);
                if (position >= 0) {
                    this.mLayoutMode = 1;
                    setSelectionInt(position);
                    invokeOnItemScrollListener();
                }
                moved = true;
            }
        } else if (direction == 130) {
            int lastItem = this.mItemCount - 1;
            if (this.mSelectedPosition < lastItem) {
                position = lookForSelectablePositionAfter(this.mSelectedPosition, lastItem, false);
                if (position >= 0) {
                    this.mLayoutMode = 3;
                    setSelectionInt(position);
                    invokeOnItemScrollListener();
                }
                moved = true;
            }
        }
        if (moved && !awakenScrollBars()) {
            awakenScrollBars();
            invalidate();
        }
        return moved;
    }

    private boolean handleHorizontalFocusWithinListItem(int direction) {
        if (direction == 17 || direction == 66) {
            int numChildren = getChildCount();
            if (this.mItemsCanFocus && numChildren > 0 && this.mSelectedPosition != -1) {
                View selectedView = getSelectedView();
                if (selectedView != null && selectedView.hasFocus() && (selectedView instanceof ViewGroup)) {
                    View currentFocus = selectedView.findFocus();
                    View nextFocus = FocusFinder.getInstance().findNextFocus((ViewGroup) selectedView, currentFocus, direction);
                    if (nextFocus != null) {
                        Rect focusedRect = this.mTempRect;
                        if (currentFocus != null) {
                            currentFocus.getFocusedRect(focusedRect);
                            offsetDescendantRectToMyCoords(currentFocus, focusedRect);
                            offsetRectIntoDescendantCoords(nextFocus, focusedRect);
                        } else {
                            focusedRect = null;
                        }
                        if (nextFocus.requestFocus(direction, focusedRect)) {
                            return true;
                        }
                    }
                    View globalNextFocus = FocusFinder.getInstance().findNextFocus((ViewGroup) getRootView(), currentFocus, direction);
                    if (globalNextFocus != null) {
                        return isViewAncestorOf(globalNextFocus, this);
                    }
                }
            }
            return false;
        }
        throw new IllegalArgumentException("direction must be one of {View.FOCUS_LEFT, View.FOCUS_RIGHT}");
    }

    boolean arrowScroll(int direction) {
        try {
            this.mInLayout = true;
            boolean handled = arrowScrollImpl(direction);
            if (handled) {
                playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
            }
            this.mInLayout = false;
            return handled;
        } catch (Throwable th) {
            this.mInLayout = false;
        }
    }

    private final int nextSelectedPositionForDirection(View selectedView, int selectedPos, int direction) {
        int listBottom;
        int nextSelected;
        boolean z = true;
        if (direction == 130) {
            listBottom = getHeight() - this.mListPadding.bottom;
            if (selectedView == null || selectedView.getBottom() > listBottom) {
                return -1;
            }
            if (selectedPos == -1 || selectedPos < this.mFirstPosition) {
                nextSelected = this.mFirstPosition;
            } else {
                nextSelected = selectedPos + 1;
            }
        } else {
            listBottom = this.mListPadding.top;
            if (selectedView == null || selectedView.getTop() < listBottom) {
                return -1;
            }
            int i;
            nextSelected = (this.mFirstPosition + getChildCount()) - 1;
            if (selectedPos == -1 || selectedPos > nextSelected) {
                i = nextSelected;
            } else {
                i = selectedPos - 1;
            }
            nextSelected = i;
        }
        listBottom = nextSelected;
        if (listBottom < 0 || listBottom >= this.mAdapter.getCount()) {
            return -1;
        }
        if (direction != 130) {
            z = false;
        }
        return lookForSelectablePosition(listBottom, z);
    }

    private boolean arrowScrollImpl(int direction) {
        if (getChildCount() <= 0) {
            return false;
        }
        View focused;
        View selectedView = getSelectedView();
        int selectedPos = this.mSelectedPosition;
        int nextSelectedPosition = nextSelectedPositionForDirection(selectedView, selectedPos, direction);
        int amountToScroll = amountToScroll(direction, nextSelectedPosition);
        ArrowScrollFocusResult focusResult = this.mItemsCanFocus ? arrowScrollFocused(direction) : null;
        if (focusResult != null) {
            nextSelectedPosition = focusResult.getSelectedPosition();
            amountToScroll = focusResult.getAmountToScroll();
        }
        boolean needToRedraw = focusResult != null;
        if (nextSelectedPosition != -1) {
            handleNewSelectionChange(selectedView, direction, nextSelectedPosition, focusResult != null);
            setSelectedPositionInt(nextSelectedPosition);
            setNextSelectedPositionInt(nextSelectedPosition);
            selectedView = getSelectedView();
            selectedPos = nextSelectedPosition;
            if (this.mItemsCanFocus && focusResult == null) {
                focused = getFocusedChild();
                if (focused != null) {
                    focused.clearFocus();
                }
            }
            needToRedraw = true;
            checkSelectionChanged();
        }
        if (amountToScroll > 0) {
            scrollListItemsBy(direction == 33 ? amountToScroll : -amountToScroll);
            needToRedraw = true;
        }
        if (this.mItemsCanFocus && focusResult == null && selectedView != null && selectedView.hasFocus()) {
            focused = selectedView.findFocus();
            if (focused != null && (!isViewAncestorOf(focused, this) || distanceToView(focused) > 0)) {
                focused.clearFocus();
            }
        }
        if (!(nextSelectedPosition != -1 || selectedView == null || isViewAncestorOf(selectedView, this))) {
            selectedView = null;
            hideSelector();
            this.mResurrectToPosition = -1;
        }
        if (!needToRedraw) {
            return false;
        }
        if (selectedView != null) {
            positionSelectorLikeFocus(selectedPos, selectedView);
            this.mSelectedTop = selectedView.getTop();
        }
        if (!awakenScrollBars()) {
            invalidate();
        }
        invokeOnItemScrollListener();
        return true;
    }

    private void handleNewSelectionChange(View selectedView, int direction, int newSelectedPosition, boolean newFocusAssigned) {
        if (newSelectedPosition != -1) {
            int topViewIndex;
            int bottomViewIndex;
            View topView;
            View bottomView;
            boolean topSelected = false;
            int selectedIndex = this.mSelectedPosition - this.mFirstPosition;
            int nextSelectedIndex = newSelectedPosition - this.mFirstPosition;
            if (direction == 33) {
                topViewIndex = nextSelectedIndex;
                bottomViewIndex = selectedIndex;
                topView = getChildAt(topViewIndex);
                bottomView = selectedView;
                topSelected = true;
            } else {
                topViewIndex = selectedIndex;
                bottomViewIndex = nextSelectedIndex;
                topView = selectedView;
                bottomView = getChildAt(bottomViewIndex);
            }
            int numChildren = getChildCount();
            boolean z = true;
            if (topView != null) {
                boolean z2 = !newFocusAssigned && topSelected;
                topView.setSelected(z2);
                measureAndAdjustDown(topView, topViewIndex, numChildren);
            }
            if (bottomView != null) {
                if (newFocusAssigned || topSelected) {
                    z = false;
                }
                bottomView.setSelected(z);
                measureAndAdjustDown(bottomView, bottomViewIndex, numChildren);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("newSelectedPosition needs to be valid");
    }

    private void measureAndAdjustDown(View child, int childIndex, int numChildren) {
        int oldHeight = child.getHeight();
        measureItem(child);
        if (child.getMeasuredHeight() != oldHeight) {
            relayoutMeasuredItem(child);
            int heightDelta = child.getMeasuredHeight() - oldHeight;
            for (int i = childIndex + 1; i < numChildren; i++) {
                getChildAt(i).offsetTopAndBottom(heightDelta);
            }
        }
    }

    private void measureItem(View child) {
        int childHeightSpec;
        LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new LayoutParams(-1, -2);
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(this.mWidthMeasureSpec, this.mListPadding.left + this.mListPadding.right, p.width);
        int lpHeight = p.height;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, 1073741824);
        } else {
            childHeightSpec = MeasureSpec.makeSafeMeasureSpec(getMeasuredHeight(), 0);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    private void relayoutMeasuredItem(View child) {
        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();
        int childLeft = this.mListPadding.left;
        int childRight = childLeft + w;
        int childTop = child.getTop();
        child.layout(childLeft, childTop, childRight, childTop + h);
    }

    private int getArrowScrollPreviewLength() {
        return Math.max(2, getVerticalFadingEdgeLength());
    }

    private int amountToScroll(int direction, int nextSelectedPosition) {
        int listBottom = getHeight() - this.mListPadding.bottom;
        int listTop = this.mListPadding.top;
        int numChildren = getChildCount();
        int indexToMakeVisible;
        int positionToMakeVisible;
        View viewToMakeVisible;
        int goalBottom;
        int amountToScroll;
        if (direction == 130) {
            indexToMakeVisible = numChildren - 1;
            if (nextSelectedPosition != -1) {
                indexToMakeVisible = nextSelectedPosition - this.mFirstPosition;
            }
            while (numChildren <= indexToMakeVisible) {
                addViewBelow(getChildAt(numChildren - 1), (this.mFirstPosition + numChildren) - 1);
                numChildren++;
            }
            positionToMakeVisible = this.mFirstPosition + indexToMakeVisible;
            viewToMakeVisible = getChildAt(indexToMakeVisible);
            goalBottom = listBottom;
            if (positionToMakeVisible < this.mItemCount - 1) {
                goalBottom -= getArrowScrollPreviewLength();
            }
            if (viewToMakeVisible.getBottom() <= goalBottom) {
                return 0;
            }
            if (nextSelectedPosition != -1 && goalBottom - viewToMakeVisible.getTop() >= getMaxScrollAmount()) {
                return 0;
            }
            amountToScroll = viewToMakeVisible.getBottom() - goalBottom;
            if (this.mFirstPosition + numChildren == this.mItemCount) {
                amountToScroll = Math.min(amountToScroll, getChildAt(numChildren - 1).getBottom() - listBottom);
            }
            return Math.min(amountToScroll, getMaxScrollAmount());
        }
        indexToMakeVisible = 0;
        if (nextSelectedPosition != -1) {
            indexToMakeVisible = nextSelectedPosition - this.mFirstPosition;
        }
        while (indexToMakeVisible < 0) {
            addViewAbove(getChildAt(0), this.mFirstPosition);
            this.mFirstPosition--;
            indexToMakeVisible = nextSelectedPosition - this.mFirstPosition;
        }
        positionToMakeVisible = this.mFirstPosition + indexToMakeVisible;
        viewToMakeVisible = getChildAt(indexToMakeVisible);
        goalBottom = listTop;
        if (positionToMakeVisible > 0) {
            goalBottom += getArrowScrollPreviewLength();
        }
        if (viewToMakeVisible.getTop() >= goalBottom) {
            return 0;
        }
        if (nextSelectedPosition != -1 && viewToMakeVisible.getBottom() - goalBottom >= getMaxScrollAmount()) {
            return 0;
        }
        amountToScroll = goalBottom - viewToMakeVisible.getTop();
        if (this.mFirstPosition == 0) {
            amountToScroll = Math.min(amountToScroll, listTop - getChildAt(0).getTop());
        }
        return Math.min(amountToScroll, getMaxScrollAmount());
    }

    private int lookForSelectablePositionOnScreen(int direction) {
        int firstPosition = this.mFirstPosition;
        int startPos;
        int lastVisiblePos;
        ListAdapter adapter;
        int pos;
        if (direction == 130) {
            if (this.mSelectedPosition != -1) {
                startPos = this.mSelectedPosition + 1;
            } else {
                startPos = firstPosition;
            }
            if (startPos >= this.mAdapter.getCount()) {
                return -1;
            }
            if (startPos < firstPosition) {
                startPos = firstPosition;
            }
            lastVisiblePos = getLastVisiblePosition();
            adapter = getAdapter();
            pos = startPos;
            while (pos <= lastVisiblePos) {
                if (adapter.isEnabled(pos) && getChildAt(pos - firstPosition).getVisibility() == 0) {
                    return pos;
                }
                pos++;
            }
        } else {
            startPos = (getChildCount() + firstPosition) - 1;
            if (this.mSelectedPosition != -1) {
                lastVisiblePos = this.mSelectedPosition - 1;
            } else {
                lastVisiblePos = (getChildCount() + firstPosition) - 1;
            }
            if (lastVisiblePos < 0 || lastVisiblePos >= this.mAdapter.getCount()) {
                return -1;
            }
            if (lastVisiblePos > startPos) {
                lastVisiblePos = startPos;
            }
            adapter = getAdapter();
            pos = lastVisiblePos;
            while (pos >= firstPosition) {
                if (adapter.isEnabled(pos) && getChildAt(pos - firstPosition).getVisibility() == 0) {
                    return pos;
                }
                pos--;
            }
        }
        return -1;
    }

    private ArrowScrollFocusResult arrowScrollFocused(int direction) {
        int listTop;
        int ySearchPoint;
        View newFocus;
        View selectedView = getSelectedView();
        if (selectedView == null || !selectedView.hasFocus()) {
            boolean topFadingEdgeShowing = true;
            if (direction == 130) {
                if (this.mFirstPosition <= 0) {
                    topFadingEdgeShowing = false;
                }
                listTop = this.mListPadding.top + (topFadingEdgeShowing ? getArrowScrollPreviewLength() : 0);
                if (selectedView == null || selectedView.getTop() <= listTop) {
                    ySearchPoint = listTop;
                } else {
                    ySearchPoint = selectedView.getTop();
                }
                this.mTempRect.set(0, ySearchPoint, 0, ySearchPoint);
            } else {
                if ((this.mFirstPosition + getChildCount()) - 1 >= this.mItemCount) {
                    topFadingEdgeShowing = false;
                }
                listTop = (getHeight() - this.mListPadding.bottom) - (topFadingEdgeShowing ? getArrowScrollPreviewLength() : 0);
                if (selectedView == null || selectedView.getBottom() >= listTop) {
                    ySearchPoint = listTop;
                } else {
                    ySearchPoint = selectedView.getBottom();
                }
                this.mTempRect.set(0, ySearchPoint, 0, ySearchPoint);
            }
            newFocus = FocusFinder.getInstance().findNextFocusFromRect(this, this.mTempRect, direction);
        } else {
            newFocus = FocusFinder.getInstance().findNextFocus(this, selectedView.findFocus(), direction);
        }
        if (newFocus != null) {
            listTop = positionOfNewFocus(newFocus);
            if (!(this.mSelectedPosition == -1 || listTop == this.mSelectedPosition)) {
                ySearchPoint = lookForSelectablePositionOnScreen(direction);
                if (ySearchPoint != -1 && ((direction == 130 && ySearchPoint < listTop) || (direction == 33 && ySearchPoint > listTop))) {
                    return null;
                }
            }
            int focusScroll = amountToScrollToNewFocus(direction, newFocus, listTop);
            ySearchPoint = getMaxScrollAmount();
            if (focusScroll < ySearchPoint) {
                newFocus.requestFocus(direction);
                this.mArrowScrollFocusResult.populate(listTop, focusScroll);
                return this.mArrowScrollFocusResult;
            } else if (distanceToView(newFocus) < ySearchPoint) {
                newFocus.requestFocus(direction);
                this.mArrowScrollFocusResult.populate(listTop, ySearchPoint);
                return this.mArrowScrollFocusResult;
            }
        }
        return null;
    }

    private int positionOfNewFocus(View newFocus) {
        int numChildren = getChildCount();
        for (int i = 0; i < numChildren; i++) {
            if (isViewAncestorOf(newFocus, getChildAt(i))) {
                return this.mFirstPosition + i;
            }
        }
        throw new IllegalArgumentException("newFocus is not a child of any of the children of the list!");
    }

    private boolean isViewAncestorOf(View child, View parent) {
        boolean z = true;
        if (child == parent) {
            return true;
        }
        ViewParent theParent = child.getParent();
        if (!((theParent instanceof ViewGroup) && isViewAncestorOf((View) theParent, parent))) {
            z = false;
        }
        return z;
    }

    private int amountToScrollToNewFocus(int direction, View newFocus, int positionOfNewFocus) {
        newFocus.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(newFocus, this.mTempRect);
        int amountToScroll;
        if (direction != 33) {
            int listBottom = getHeight() - this.mListPadding.bottom;
            if (this.mTempRect.bottom <= listBottom) {
                return 0;
            }
            amountToScroll = this.mTempRect.bottom - listBottom;
            if (positionOfNewFocus < this.mItemCount - 1) {
                return amountToScroll + getArrowScrollPreviewLength();
            }
            return amountToScroll;
        } else if (this.mTempRect.top >= this.mListPadding.top) {
            return 0;
        } else {
            amountToScroll = this.mListPadding.top - this.mTempRect.top;
            if (positionOfNewFocus > 0) {
                return amountToScroll + getArrowScrollPreviewLength();
            }
            return amountToScroll;
        }
    }

    private int distanceToView(View descendant) {
        descendant.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(descendant, this.mTempRect);
        int listBottom = (this.mBottom - this.mTop) - this.mListPadding.bottom;
        if (this.mTempRect.bottom < this.mListPadding.top) {
            return this.mListPadding.top - this.mTempRect.bottom;
        }
        if (this.mTempRect.top > listBottom) {
            return this.mTempRect.top - listBottom;
        }
        return 0;
    }

    private void scrollListItemsBy(int amount) {
        offsetChildrenTopAndBottom(amount);
        int listBottom = getHeight() - this.mListPadding.bottom;
        int listTop = this.mListPadding.top;
        RecycleBin recycleBin = this.mRecycler;
        int numChildren;
        View last;
        if (amount < 0) {
            numChildren = getChildCount();
            last = getChildAt(numChildren - 1);
            while (last.getBottom() < listBottom) {
                int lastVisiblePosition = (this.mFirstPosition + numChildren) - 1;
                if (lastVisiblePosition >= this.mItemCount - 1) {
                    break;
                }
                last = addViewBelow(last, lastVisiblePosition);
                numChildren++;
            }
            if (last.getBottom() < listBottom) {
                offsetChildrenTopAndBottom(listBottom - last.getBottom());
            }
            View first = getChildAt(0);
            while (first != null && first.getBottom() < listTop) {
                if (recycleBin.shouldRecycleViewType(((AbsListView.LayoutParams) first.getLayoutParams()).viewType)) {
                    recycleBin.addScrapView(first, this.mFirstPosition);
                }
                detachViewFromParent(first);
                first = getChildAt(0);
                this.mFirstPosition++;
            }
        } else {
            View first2 = getChildAt(0);
            while (first2.getTop() > listTop && this.mFirstPosition > 0) {
                first2 = addViewAbove(first2, this.mFirstPosition);
                this.mFirstPosition--;
            }
            if (first2.getTop() > listTop) {
                offsetChildrenTopAndBottom(listTop - first2.getTop());
            }
            numChildren = getChildCount() - 1;
            last = getChildAt(numChildren);
            while (last != null && last.getTop() > listBottom) {
                if (recycleBin.shouldRecycleViewType(((AbsListView.LayoutParams) last.getLayoutParams()).viewType)) {
                    recycleBin.addScrapView(last, this.mFirstPosition + numChildren);
                }
                detachViewFromParent(last);
                numChildren--;
                last = getChildAt(numChildren);
            }
        }
        recycleBin.fullyDetachScrapViews();
        removeUnusedFixedViews(this.mHeaderViewInfos);
        removeUnusedFixedViews(this.mFooterViewInfos);
    }

    private View addViewAbove(View theView, int position) {
        int abovePosition = position - 1;
        View view = obtainView(abovePosition, this.mIsScrap);
        setupChild(view, abovePosition, theView.getTop() - this.mDividerHeight, false, this.mListPadding.left, false, this.mIsScrap[0]);
        return view;
    }

    private View addViewBelow(View theView, int position) {
        int belowPosition = position + 1;
        View view = obtainView(belowPosition, this.mIsScrap);
        setupChild(view, belowPosition, theView.getBottom() + this.mDividerHeight, true, this.mListPadding.left, false, this.mIsScrap[0]);
        return view;
    }

    public void setItemsCanFocus(boolean itemsCanFocus) {
        this.mItemsCanFocus = itemsCanFocus;
        if (!itemsCanFocus) {
            setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
    }

    public boolean getItemsCanFocus() {
        return this.mItemsCanFocus;
    }

    public boolean isOpaque() {
        boolean retValue = (this.mCachingActive && this.mIsCacheColorOpaque && this.mDividerIsOpaque && hasOpaqueScrollbars()) || super.isOpaque();
        if (retValue) {
            int listTop = this.mListPadding != null ? this.mListPadding.top : this.mPaddingTop;
            View first = getChildAt(0);
            if (first == null || first.getTop() > listTop) {
                return false;
            }
            int listBottom = getHeight() - (this.mListPadding != null ? this.mListPadding.bottom : this.mPaddingBottom);
            View last = getChildAt(getChildCount() - 1);
            if (last == null || last.getBottom() < listBottom) {
                return false;
            }
        }
        return retValue;
    }

    public void setCacheColorHint(int color) {
        boolean opaque = (color >>> 24) == 255;
        this.mIsCacheColorOpaque = opaque;
        if (opaque) {
            if (this.mDividerPaint == null) {
                this.mDividerPaint = new Paint();
            }
            this.mDividerPaint.setColor(color);
        }
        super.setCacheColorHint(color);
    }

    void drawOverscrollHeader(Canvas canvas, Drawable drawable, Rect bounds) {
        int height = drawable.getMinimumHeight();
        canvas.save();
        canvas.clipRect(bounds);
        if (bounds.bottom - bounds.top < height) {
            bounds.top = bounds.bottom - height;
        }
        drawable.setBounds(bounds);
        drawable.draw(canvas);
        canvas.restore();
    }

    void drawOverscrollFooter(Canvas canvas, Drawable drawable, Rect bounds) {
        int height = drawable.getMinimumHeight();
        canvas.save();
        canvas.clipRect(bounds);
        if (bounds.bottom - bounds.top < height) {
            bounds.bottom = bounds.top + height;
        }
        drawable.setBounds(bounds);
        drawable.draw(canvas);
        canvas.restore();
    }

    protected void dispatchDraw(Canvas canvas) {
        Canvas canvas2 = canvas;
        if (this.mCachingStarted) {
            this.mCachingActive = true;
        }
        int dividerHeight = this.mDividerHeight;
        Drawable overscrollHeader = this.mOverScrollHeader;
        Drawable overscrollFooter = this.mOverScrollFooter;
        boolean drawOverscrollHeader = overscrollHeader != null;
        boolean drawOverscrollFooter = overscrollFooter != null;
        boolean drawDividers = dividerHeight > 0 && this.mDivider != null;
        boolean drawOverscrollFooter2;
        boolean drawOverscrollHeader2;
        boolean drawDividers2;
        Drawable overscrollHeader2;
        if (drawDividers || drawOverscrollHeader || drawOverscrollFooter) {
            int itemCount;
            ListAdapter adapter;
            int effectivePaddingBottom;
            Rect bounds = this.mTempRect;
            bounds.left = this.mPaddingLeft;
            bounds.right = (this.mRight - this.mLeft) - this.mPaddingRight;
            int count = getChildCount();
            int headerCount = getHeaderViewsCount();
            int itemCount2 = this.mItemCount;
            int footerLimit = itemCount2 - this.mFooterViewInfos.size();
            boolean headerDividers = this.mHeaderDividersEnabled;
            boolean footerDividers = this.mFooterDividersEnabled;
            int first = this.mFirstPosition;
            Drawable overscrollFooter2 = overscrollFooter;
            boolean areAllItemsSelectable = this.mAreAllItemsSelectable;
            ListAdapter adapter2 = this.mAdapter;
            boolean fillForMissingDividers = isOpaque() && !super.isOpaque();
            if (fillForMissingDividers) {
                itemCount = itemCount2;
                if (this.mDividerPaint == 0 && this.mIsCacheColorOpaque) {
                    this.mDividerPaint = new Paint();
                    adapter = adapter2;
                    this.mDividerPaint.setColor(getCacheColorHint());
                } else {
                    adapter = adapter2;
                }
            } else {
                adapter = adapter2;
                itemCount = itemCount2;
            }
            int effectivePaddingTop = 0;
            Paint paint = this.mDividerPaint;
            if ((this.mGroupFlags & 34) == 34) {
                itemCount2 = this.mListPadding.top;
                effectivePaddingBottom = this.mListPadding.bottom;
            } else {
                effectivePaddingBottom = 0;
                itemCount2 = effectivePaddingTop;
            }
            int effectivePaddingTop2 = itemCount2;
            drawOverscrollFooter2 = drawOverscrollFooter;
            boolean listBottom = ((this.mBottom - this.mTop) - effectivePaddingBottom) + this.mScrollY;
            Drawable overscrollHeader3;
            boolean listBottom2;
            Drawable overscrollFooter3;
            ListAdapter adapter3;
            Paint paint2;
            int top;
            if (this.mStackFromBottom) {
                Drawable overscrollHeader4;
                int i;
                int first2;
                Drawable overscrollFooter4;
                int effectivePaddingTop3;
                int start;
                overscrollHeader3 = overscrollHeader;
                drawOverscrollHeader2 = drawOverscrollHeader;
                drawDividers2 = drawDividers;
                listBottom2 = listBottom;
                overscrollFooter3 = overscrollFooter2;
                itemCount2 = itemCount;
                adapter3 = adapter;
                paint2 = paint;
                effectivePaddingBottom = this.mScrollY;
                if (count <= 0 || !drawOverscrollHeader2) {
                    overscrollHeader4 = overscrollHeader3;
                    i = 0;
                } else {
                    bounds.top = effectivePaddingBottom;
                    i = 0;
                    bounds.bottom = getChildAt(0).getTop();
                    overscrollHeader4 = overscrollHeader3;
                    drawOverscrollHeader(canvas2, overscrollHeader4, bounds);
                }
                int i2 = drawOverscrollHeader2 ? 1 : i;
                int start2 = i2;
                while (true) {
                    i = i2;
                    if (i >= count) {
                        break;
                    }
                    boolean footerDividers2;
                    overscrollHeader2 = overscrollHeader4;
                    itemCount2 = first + i;
                    boolean isHeader = itemCount2 < headerCount;
                    boolean isFooter = itemCount2 >= footerLimit;
                    if ((headerDividers || !isHeader) && (footerDividers || !isFooter)) {
                        first2 = first;
                        View child = getChildAt(i);
                        overscrollFooter4 = overscrollFooter3;
                        top = child.getTop();
                        if (drawDividers2) {
                            first = effectivePaddingTop2;
                            if (top > first) {
                                effectivePaddingTop3 = first;
                                first = start2;
                                boolean isFirstItem = i == first;
                                start = first;
                                first = itemCount2 - 1;
                                if (!checkIsEnabled(adapter3, itemCount2)) {
                                    footerDividers2 = footerDividers;
                                } else if ((headerDividers || (!isHeader && first >= headerCount)) && (isFirstItem || (checkIsEnabled(adapter3, first) && (footerDividers || (!isFooter && first < footerLimit))))) {
                                    footerDividers2 = footerDividers;
                                    bounds.top = top - dividerHeight;
                                    bounds.bottom = top;
                                    drawDivider(canvas2, bounds, i - 1);
                                } else {
                                    footerDividers2 = footerDividers;
                                }
                                if (fillForMissingDividers) {
                                    bounds.top = top - dividerHeight;
                                    bounds.bottom = top;
                                    canvas2.drawRect(bounds, paint2);
                                }
                            } else {
                                footerDividers2 = footerDividers;
                                effectivePaddingTop3 = first;
                                start = start2;
                            }
                        } else {
                            footerDividers2 = footerDividers;
                            effectivePaddingTop3 = effectivePaddingTop2;
                            start = start2;
                        }
                    } else {
                        footerDividers2 = footerDividers;
                        first2 = first;
                        overscrollFooter4 = overscrollFooter3;
                        effectivePaddingTop3 = effectivePaddingTop2;
                        start = start2;
                    }
                    i2 = i + 1;
                    overscrollHeader4 = overscrollHeader2;
                    first = first2;
                    overscrollFooter3 = overscrollFooter4;
                    effectivePaddingTop2 = effectivePaddingTop3;
                    start2 = start;
                    footerDividers = footerDividers2;
                }
                first2 = first;
                overscrollFooter4 = overscrollFooter3;
                overscrollHeader2 = overscrollHeader4;
                effectivePaddingTop3 = effectivePaddingTop2;
                start = start2;
                if (count <= 0 || effectivePaddingBottom <= 0) {
                } else if (drawOverscrollFooter2) {
                    int absListBottom = this.mBottom;
                    bounds.top = absListBottom;
                    bounds.bottom = absListBottom + effectivePaddingBottom;
                    drawOverscrollFooter(canvas2, overscrollFooter4, bounds);
                } else {
                    if (drawDividers2) {
                        listBottom = listBottom2;
                        bounds.top = listBottom;
                        bounds.bottom = listBottom + dividerHeight;
                        drawDivider(canvas2, bounds, -1);
                    }
                }
            } else {
                boolean bottom;
                effectivePaddingBottom = this.mScrollY;
                if (count <= 0 || effectivePaddingBottom >= 0) {
                    bottom = false;
                } else if (drawOverscrollHeader) {
                    bottom = false;
                    bounds.bottom = 0;
                    bounds.top = effectivePaddingBottom;
                    drawOverscrollHeader(canvas2, overscrollHeader, bounds);
                } else {
                    bottom = false;
                    if (drawDividers) {
                        bounds.bottom = 0;
                        bounds.top = -dividerHeight;
                        drawDivider(canvas2, bounds, -1);
                    }
                }
                boolean bottom2 = bottom;
                top = 0;
                while (top < count) {
                    Object paint3;
                    overscrollHeader3 = overscrollHeader;
                    overscrollHeader = first + top;
                    boolean isHeader2 = overscrollHeader < headerCount;
                    boolean isFooter2 = overscrollHeader >= footerLimit;
                    if ((headerDividers || !isHeader2) && (footerDividers || !isFooter2)) {
                        drawOverscrollHeader2 = drawOverscrollHeader;
                        View child2 = getChildAt(top);
                        bottom2 = child2.getBottom();
                        drawOverscrollHeader = top == count + -1;
                        if (!drawDividers || bottom2 >= listBottom) {
                            drawDividers2 = drawDividers;
                            listBottom2 = listBottom;
                            adapter3 = adapter;
                            overscrollHeader = paint3;
                            top++;
                            paint3 = overscrollHeader;
                            adapter = adapter3;
                            overscrollHeader = overscrollHeader3;
                            drawOverscrollHeader = drawOverscrollHeader2;
                            listBottom = listBottom2;
                            drawDividers = drawDividers2;
                        } else if (!(drawOverscrollFooter2 && drawOverscrollHeader)) {
                            listBottom2 = listBottom;
                            itemCount2 = overscrollHeader + 1;
                            drawDividers2 = drawDividers;
                            adapter3 = adapter;
                            int itemIndex;
                            if (checkIsEnabled(adapter3, overscrollHeader) == null) {
                                itemIndex = overscrollHeader;
                            } else if ((headerDividers || (!isHeader2 && itemCount2 >= headerCount)) && (drawOverscrollHeader || (checkIsEnabled(adapter3, itemCount2) && (footerDividers || (!isFooter2 && itemCount2 < footerLimit))))) {
                                bounds.top = bottom2;
                                itemIndex = overscrollHeader;
                                bounds.bottom = bottom2 + dividerHeight;
                                drawDivider(canvas2, bounds, top);
                                overscrollHeader = paint3;
                                top++;
                                paint3 = overscrollHeader;
                                adapter = adapter3;
                                overscrollHeader = overscrollHeader3;
                                drawOverscrollHeader = drawOverscrollHeader2;
                                listBottom = listBottom2;
                                drawDividers = drawDividers2;
                            } else {
                                Drawable drawable = overscrollHeader;
                            }
                            if (fillForMissingDividers) {
                                bounds.top = bottom2;
                                bounds.bottom = bottom2 + dividerHeight;
                                overscrollHeader = paint3;
                                canvas2.drawRect(bounds, overscrollHeader);
                            } else {
                                overscrollHeader = paint3;
                            }
                            top++;
                            paint3 = overscrollHeader;
                            adapter = adapter3;
                            overscrollHeader = overscrollHeader3;
                            drawOverscrollHeader = drawOverscrollHeader2;
                            listBottom = listBottom2;
                            drawDividers = drawDividers2;
                        }
                    } else {
                        drawOverscrollHeader2 = drawOverscrollHeader;
                    }
                    drawDividers2 = drawDividers;
                    listBottom2 = listBottom;
                    adapter3 = adapter;
                    overscrollHeader = paint3;
                    top++;
                    paint3 = overscrollHeader;
                    adapter = adapter3;
                    overscrollHeader = overscrollHeader3;
                    drawOverscrollHeader = drawOverscrollHeader2;
                    listBottom = listBottom2;
                    drawDividers = drawDividers2;
                }
                overscrollHeader3 = overscrollHeader;
                drawOverscrollHeader2 = drawOverscrollHeader;
                drawDividers2 = drawDividers;
                listBottom2 = listBottom;
                drawDividers = adapter;
                paint2 = paint3;
                drawOverscrollHeader = this.mBottom + this.mScrollY;
                if (!drawOverscrollFooter2) {
                    overscrollFooter3 = overscrollFooter2;
                    itemCount2 = itemCount;
                } else if (first + count != itemCount || drawOverscrollHeader <= bottom2) {
                    overscrollFooter3 = overscrollFooter2;
                } else {
                    bounds.top = bottom2;
                    bounds.bottom = drawOverscrollHeader;
                    overscrollFooter3 = overscrollFooter2;
                    drawOverscrollFooter(canvas2, overscrollFooter3, bounds);
                }
                first = overscrollFooter3;
                overscrollHeader2 = overscrollHeader3;
            }
        } else {
            overscrollHeader2 = overscrollHeader;
            Drawable drawable2 = overscrollFooter;
            drawOverscrollHeader2 = drawOverscrollHeader;
            drawOverscrollFooter2 = drawOverscrollFooter;
            drawDividers2 = drawDividers;
        }
        super.dispatchDraw(canvas);
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean more = super.drawChild(canvas, child, drawingTime);
        if (this.mCachingActive && child.mCachingFailed) {
            this.mCachingActive = false;
        }
        return more;
    }

    void drawDivider(Canvas canvas, Rect bounds, int childIndex) {
        Drawable divider = this.mDivider;
        divider.setBounds(bounds);
        divider.draw(canvas);
    }

    public Drawable getDivider() {
        return this.mDivider;
    }

    public void setDivider(Drawable divider) {
        boolean z = false;
        if (divider != null) {
            this.mDividerHeight = divider.getIntrinsicHeight();
        } else {
            this.mDividerHeight = 0;
        }
        this.mDivider = divider;
        if (divider == null || divider.getOpacity() == -1) {
            z = true;
        }
        this.mDividerIsOpaque = z;
        requestLayout();
        invalidate();
    }

    public int getDividerHeight() {
        return this.mDividerHeight;
    }

    public void setDividerHeight(int height) {
        this.mDividerHeight = height;
        requestLayout();
        invalidate();
    }

    public void setHeaderDividersEnabled(boolean headerDividersEnabled) {
        this.mHeaderDividersEnabled = headerDividersEnabled;
        invalidate();
    }

    public boolean areHeaderDividersEnabled() {
        return this.mHeaderDividersEnabled;
    }

    public void setFooterDividersEnabled(boolean footerDividersEnabled) {
        this.mFooterDividersEnabled = footerDividersEnabled;
        invalidate();
    }

    public boolean areFooterDividersEnabled() {
        return this.mFooterDividersEnabled;
    }

    public void setOverscrollHeader(Drawable header) {
        this.mOverScrollHeader = header;
        if (this.mScrollY < 0) {
            invalidate();
        }
    }

    public Drawable getOverscrollHeader() {
        return this.mOverScrollHeader;
    }

    public void setOverscrollFooter(Drawable footer) {
        this.mOverScrollFooter = footer;
        invalidate();
    }

    public Drawable getOverscrollFooter() {
        return this.mOverScrollFooter;
    }

    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        ListAdapter adapter = this.mAdapter;
        int closetChildIndex = -1;
        int closestChildTop = 0;
        if (!(adapter == null || !gainFocus || previouslyFocusedRect == null)) {
            previouslyFocusedRect.offset(this.mScrollX, this.mScrollY);
            int i = 0;
            if (adapter.getCount() < getChildCount() + this.mFirstPosition) {
                this.mLayoutMode = 0;
                layoutChildren();
            }
            Rect otherRect = this.mTempRect;
            int minDistance = Integer.MAX_VALUE;
            int childCount = getChildCount();
            int firstPosition = this.mFirstPosition;
            while (i < childCount) {
                if (adapter.isEnabled(firstPosition + i)) {
                    View other = getChildAt(i);
                    other.getDrawingRect(otherRect);
                    offsetDescendantRectToMyCoords(other, otherRect);
                    int distance = AbsListView.getDistance(previouslyFocusedRect, otherRect, direction);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closetChildIndex = i;
                        closestChildTop = other.getTop();
                    }
                }
                i++;
            }
        }
        if (closetChildIndex >= 0) {
            setSelectionFromTop(this.mFirstPosition + closetChildIndex, closestChildTop);
        } else {
            requestLayout();
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        int count = getChildCount();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                addHeaderView(getChildAt(i));
            }
            removeAllViews();
        }
    }

    protected <T extends View> T findViewTraversal(int id) {
        View v = super.findViewTraversal(id);
        if (v == null) {
            v = findViewInHeadersOrFooters(this.mHeaderViewInfos, id);
            if (v != null) {
                return v;
            }
            v = findViewInHeadersOrFooters(this.mFooterViewInfos, id);
            if (v != null) {
                return v;
            }
        }
        return v;
    }

    View findViewInHeadersOrFooters(ArrayList<FixedViewInfo> where, int id) {
        if (where != null) {
            int len = where.size();
            for (int i = 0; i < len; i++) {
                View v = ((FixedViewInfo) where.get(i)).view;
                if (!v.isRootNamespace()) {
                    v = v.findViewById(id);
                    if (v != null) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    protected <T extends View> T findViewWithTagTraversal(Object tag) {
        View v = super.findViewWithTagTraversal(tag);
        if (v == null) {
            v = findViewWithTagInHeadersOrFooters(this.mHeaderViewInfos, tag);
            if (v != null) {
                return v;
            }
            v = findViewWithTagInHeadersOrFooters(this.mFooterViewInfos, tag);
            if (v != null) {
                return v;
            }
        }
        return v;
    }

    View findViewWithTagInHeadersOrFooters(ArrayList<FixedViewInfo> where, Object tag) {
        if (where != null) {
            int len = where.size();
            for (int i = 0; i < len; i++) {
                View v = ((FixedViewInfo) where.get(i)).view;
                if (!v.isRootNamespace()) {
                    v = v.findViewWithTag(tag);
                    if (v != null) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    protected <T extends View> T findViewByPredicateTraversal(Predicate<View> predicate, View childToSkip) {
        View v = super.findViewByPredicateTraversal(predicate, childToSkip);
        if (v == null) {
            v = findViewByPredicateInHeadersOrFooters(this.mHeaderViewInfos, predicate, childToSkip);
            if (v != null) {
                return v;
            }
            v = findViewByPredicateInHeadersOrFooters(this.mFooterViewInfos, predicate, childToSkip);
            if (v != null) {
                return v;
            }
        }
        return v;
    }

    View findViewByPredicateInHeadersOrFooters(ArrayList<FixedViewInfo> where, Predicate<View> predicate, View childToSkip) {
        if (where != null) {
            int len = where.size();
            for (int i = 0; i < len; i++) {
                View v = ((FixedViewInfo) where.get(i)).view;
                if (!(v == childToSkip || v.isRootNamespace())) {
                    v = v.findViewByPredicate(predicate);
                    if (v != null) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    @Deprecated
    public long[] getCheckItemIds() {
        if (this.mAdapter != null && this.mAdapter.hasStableIds()) {
            return getCheckedItemIds();
        }
        if (this.mChoiceMode == 0 || this.mCheckStates == null || this.mAdapter == null) {
            return new long[0];
        }
        SparseBooleanArray states = this.mCheckStates;
        int count = states.size();
        long[] ids = new long[count];
        ListAdapter adapter = this.mAdapter;
        int checkedCount = 0;
        for (int i = 0; i < count; i++) {
            if (states.valueAt(i)) {
                int checkedCount2 = checkedCount + 1;
                ids[checkedCount] = adapter.getItemId(states.keyAt(i));
                checkedCount = checkedCount2;
            }
        }
        if (checkedCount == count) {
            return ids;
        }
        long[] result = new long[checkedCount];
        System.arraycopy(ids, 0, result, 0, checkedCount);
        return result;
    }

    int getHeightForPosition(int position) {
        int height = super.getHeightForPosition(position);
        if (shouldAdjustHeightForDivider(position)) {
            return this.mDividerHeight + height;
        }
        return height;
    }

    private boolean shouldAdjustHeightForDivider(int itemIndex) {
        int i = itemIndex;
        int dividerHeight = this.mDividerHeight;
        Drawable overscrollHeader = this.mOverScrollHeader;
        Drawable overscrollFooter = this.mOverScrollFooter;
        boolean drawOverscrollHeader = overscrollHeader != null;
        boolean drawOverscrollFooter = overscrollFooter != null;
        boolean drawDividers = dividerHeight > 0 && this.mDivider != null;
        Drawable drawable;
        Drawable drawable2;
        if (drawDividers) {
            boolean fillForMissingDividers = isOpaque() && !super.isOpaque();
            int itemCount = this.mItemCount;
            int headerCount = getHeaderViewsCount();
            int footerLimit = itemCount - this.mFooterViewInfos.size();
            boolean isHeader = i < headerCount;
            boolean isFooter = i >= footerLimit;
            boolean headerDividers = this.mHeaderDividersEnabled;
            boolean footerDividers = this.mFooterDividersEnabled;
            if ((headerDividers || !isHeader) && (footerDividers || !isFooter)) {
                dividerHeight = this.mAdapter;
                if (this.mStackFromBottom == null) {
                    boolean isLastItem = i == itemCount + -1;
                    if (drawOverscrollFooter && isLastItem) {
                        drawable = overscrollFooter;
                    } else {
                        overscrollFooter = i + 1;
                        if (checkIsEnabled(dividerHeight, i) && ((headerDividers || (!isHeader && overscrollFooter >= headerCount)) && (isLastItem || (checkIsEnabled(dividerHeight, overscrollFooter) && (footerDividers || (!isFooter && overscrollFooter < footerLimit)))))) {
                            return true;
                        }
                        if (fillForMissingDividers) {
                            return true;
                        }
                    }
                }
                int start = drawOverscrollHeader ? 1 : 0;
                overscrollFooter = i == start ? true : null;
                if (overscrollFooter == null) {
                    overscrollHeader = i - 1;
                    if (checkIsEnabled(dividerHeight, i) && ((headerDividers || (!isHeader && overscrollHeader >= headerCount)) && (overscrollFooter != null || (checkIsEnabled(dividerHeight, overscrollHeader) && (footerDividers || (!isFooter && overscrollHeader < footerLimit)))))) {
                        return true;
                    }
                    if (fillForMissingDividers) {
                        return true;
                    }
                }
            }
            int i2 = dividerHeight;
            drawable2 = overscrollHeader;
            drawable = overscrollFooter;
        } else {
            drawable2 = overscrollHeader;
            drawable = overscrollFooter;
        }
        return false;
    }

    public CharSequence getAccessibilityClassName() {
        return ListView.class.getName();
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        int rowsCount = getCount();
        info.setCollectionInfo(CollectionInfo.obtain(rowsCount, 1, false, getSelectionModeForAccessibility()));
        if (rowsCount > 0) {
            info.addAction(AccessibilityAction.ACTION_SCROLL_TO_POSITION);
        }
    }

    public boolean performAccessibilityActionInternal(int action, Bundle arguments) {
        if (super.performAccessibilityActionInternal(action, arguments)) {
            return true;
        }
        if (action == 16908343) {
            int row = arguments.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT, -1);
            int position = Math.min(row, getCount() - 1);
            if (row >= 0) {
                smoothScrollToPosition(position);
                return true;
            }
        }
        return false;
    }

    public void onInitializeAccessibilityNodeInfoForItem(View view, int position, AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoForItem(view, position, info);
        AbsListView.LayoutParams lp = (AbsListView.LayoutParams) view.getLayoutParams();
        boolean z = lp != null && lp.viewType == -2;
        info.setCollectionItemInfo(CollectionItemInfo.obtain(position, 1, 0, 1, z, isItemChecked(position)));
    }

    protected void encodeProperties(ViewHierarchyEncoder encoder) {
        super.encodeProperties(encoder);
        encoder.addProperty("recycleOnMeasure", recycleOnMeasure());
    }

    protected HeaderViewListAdapter wrapHeaderListAdapterInternal(ArrayList<FixedViewInfo> headerViewInfos, ArrayList<FixedViewInfo> footerViewInfos, ListAdapter adapter) {
        return new HeaderViewListAdapter(headerViewInfos, footerViewInfos, adapter);
    }

    protected void wrapHeaderListAdapterInternal() {
        this.mAdapter = wrapHeaderListAdapterInternal(this.mHeaderViewInfos, this.mFooterViewInfos, this.mAdapter);
    }

    protected void dispatchDataSetObserverOnChangedInternal() {
        if (this.mDataSetObserver != null) {
            this.mDataSetObserver.onChanged();
        }
    }
}
