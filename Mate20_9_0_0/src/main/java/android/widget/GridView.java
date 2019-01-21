package android.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.RemotableViewMethod;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewDebug.ExportedProperty;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.view.ViewRootImpl;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.accessibility.AccessibilityNodeInfo.CollectionInfo;
import android.view.accessibility.AccessibilityNodeInfo.CollectionItemInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.GridLayoutAnimationController.AnimationParameters;
import android.widget.AbsListView.LayoutParams;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RemoteView
public class GridView extends AbsListView {
    public static final int AUTO_FIT = -1;
    public static final int NO_STRETCH = 0;
    public static final int STRETCH_COLUMN_WIDTH = 2;
    public static final int STRETCH_SPACING = 1;
    public static final int STRETCH_SPACING_UNIFORM = 3;
    private int mColumnWidth;
    private int mGravity;
    private int mHorizontalSpacing;
    private int mNumColumns;
    private View mReferenceView;
    private View mReferenceViewInSelectedRow;
    private int mRequestedColumnWidth;
    private int mRequestedHorizontalSpacing;
    private int mRequestedNumColumns;
    private int mStretchMode;
    private final Rect mTempRect;
    private int mVerticalSpacing;

    @Retention(RetentionPolicy.SOURCE)
    public @interface StretchMode {
    }

    public GridView(Context context) {
        this(context, null);
    }

    public GridView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842865);
    }

    public GridView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mNumColumns = -1;
        this.mHorizontalSpacing = 0;
        this.mVerticalSpacing = 0;
        this.mStretchMode = 2;
        this.mReferenceView = null;
        this.mReferenceViewInSelectedRow = null;
        this.mGravity = Gravity.START;
        this.mTempRect = new Rect();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GridView, defStyleAttr, defStyleRes);
        setHorizontalSpacing(a.getDimensionPixelOffset(1, 0));
        setVerticalSpacing(a.getDimensionPixelOffset(2, 0));
        int index = a.getInt(3, 2);
        if (index >= 0) {
            setStretchMode(index);
        }
        int columnWidth = a.getDimensionPixelOffset(4, -1);
        if (columnWidth > 0) {
            setColumnWidth(columnWidth);
        }
        setNumColumns(a.getInt(5, 1));
        int index2 = a.getInt(0, -1);
        if (index2 >= 0) {
            setGravity(index2);
        }
        a.recycle();
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
        this.mAdapter = adapter;
        this.mOldSelectedPosition = -1;
        this.mOldSelectedRowId = Long.MIN_VALUE;
        super.setAdapter(adapter);
        if (this.mAdapter != null) {
            int position;
            this.mOldItemCount = this.mItemCount;
            this.mItemCount = this.mAdapter.getCount();
            this.mDataChanged = true;
            checkFocus();
            this.mDataSetObserver = new AdapterDataSetObserver();
            this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
            this.mRecycler.setViewTypeCount(this.mAdapter.getViewTypeCount());
            if (this.mStackFromBottom) {
                position = lookForSelectablePosition(this.mItemCount - 1, false);
            } else {
                position = lookForSelectablePosition(0, true);
            }
            setSelectedPositionInt(position);
            setNextSelectedPositionInt(position);
            checkSelectionChanged();
        } else {
            checkFocus();
            checkSelectionChanged();
        }
        requestLayout();
    }

    /* JADX WARNING: Missing block: B:9:0x0015, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int lookForSelectablePosition(int position, boolean lookDown) {
        if (this.mAdapter == null || isInTouchMode() || position < 0 || position >= this.mItemCount) {
            return -1;
        }
        return position;
    }

    void fillGap(boolean down) {
        int numColumns = this.mNumColumns;
        int verticalSpacing = this.mVerticalSpacing;
        int count = getChildCount();
        int paddingTop;
        int startOffset;
        int position;
        if (down) {
            paddingTop = 0;
            if ((this.mGroupFlags & 34) == 34) {
                paddingTop = getListPaddingTop();
            }
            startOffset = count > 0 ? getChildAt(count - 1).getBottom() + verticalSpacing : paddingTop;
            position = this.mFirstPosition + count;
            if (this.mStackFromBottom) {
                position += numColumns - 1;
            }
            fillDown(position, startOffset);
            correctTooHigh(numColumns, verticalSpacing, getChildCount());
            return;
        }
        paddingTop = 0;
        if ((this.mGroupFlags & 34) == 34) {
            paddingTop = getListPaddingBottom();
        }
        startOffset = count > 0 ? getChildAt(0).getTop() - verticalSpacing : getHeight() - paddingTop;
        position = this.mFirstPosition;
        if (this.mStackFromBottom) {
            position--;
        } else {
            position -= numColumns;
        }
        fillUp(position, startOffset);
        correctTooLow(numColumns, verticalSpacing, getChildCount());
    }

    private View fillDown(int pos, int nextTop) {
        View selectedView = null;
        int end = this.mBottom - this.mTop;
        if ((this.mGroupFlags & 34) == 34) {
            end -= this.mListPadding.bottom;
        }
        while (nextTop < end && pos < this.mItemCount) {
            View temp = makeRow(pos, nextTop, true);
            if (temp != null) {
                selectedView = temp;
            }
            nextTop = this.mReferenceView.getBottom() + this.mVerticalSpacing;
            pos += this.mNumColumns;
        }
        setVisibleRangeHint(this.mFirstPosition, (this.mFirstPosition + getChildCount()) - 1);
        return selectedView;
    }

    private View makeRow(int startPos, int y, boolean flow) {
        int nextLeft;
        int startPos2;
        int startPos3;
        int selectedPosition;
        int columnWidth = this.mColumnWidth;
        int horizontalSpacing = this.mHorizontalSpacing;
        boolean isLayoutRtl = isRtlLocale();
        boolean z = false;
        if (isLayoutRtl) {
            nextLeft = ((getWidth() - this.mListPadding.right) - columnWidth) - (this.mStretchMode == 3 ? horizontalSpacing : 0);
        } else {
            nextLeft = this.mListPadding.left + (this.mStretchMode == 3 ? horizontalSpacing : 0);
        }
        int nextLeft2 = nextLeft;
        if (this.mStackFromBottom) {
            nextLeft = startPos + 1;
            startPos2 = Math.max(0, (startPos - this.mNumColumns) + 1);
            if (nextLeft - startPos2 < this.mNumColumns) {
                nextLeft2 += (isLayoutRtl ? -1 : 1) * ((this.mNumColumns - (nextLeft - startPos2)) * (columnWidth + horizontalSpacing));
            }
            startPos3 = startPos2;
        } else {
            nextLeft = Math.min(startPos + this.mNumColumns, this.mItemCount);
            startPos3 = startPos;
        }
        int last = nextLeft;
        boolean hasFocus = shouldShowSelector();
        boolean inClick = touchModeDrawsInPressedState();
        int selectedPosition2 = this.mSelectedPosition;
        int nextChildDir = isLayoutRtl ? -1 : 1;
        View selectedView = null;
        int nextLeft3 = nextLeft2;
        View child = null;
        startPos2 = startPos3;
        while (true) {
            int pos = startPos2;
            if (pos >= last) {
                break;
            }
            boolean selected = pos == selectedPosition2 ? true : z;
            int pos2 = pos;
            selectedPosition = selectedPosition2;
            child = makeAndAddView(pos, y, flow, nextLeft3, selected, flow ? -1 : pos - startPos3);
            nextLeft3 += nextChildDir * columnWidth;
            if (pos2 < last - 1) {
                nextLeft3 += nextChildDir * horizontalSpacing;
            }
            if (selected && (hasFocus || inClick)) {
                selectedView = child;
            }
            startPos2 = pos2 + 1;
            selectedPosition2 = selectedPosition;
            z = false;
        }
        selectedPosition = selectedPosition2;
        this.mReferenceView = child;
        if (selectedView != null) {
            this.mReferenceViewInSelectedRow = this.mReferenceView;
        }
        return selectedView;
    }

    private View fillUp(int pos, int nextBottom) {
        View selectedView = null;
        int end = 0;
        if ((this.mGroupFlags & 34) == 34) {
            end = this.mListPadding.top;
        }
        while (nextBottom > end && pos >= 0) {
            View temp = makeRow(pos, nextBottom, false);
            if (temp != null) {
                selectedView = temp;
            }
            nextBottom = this.mReferenceView.getTop() - this.mVerticalSpacing;
            this.mFirstPosition = pos;
            pos -= this.mNumColumns;
        }
        if (this.mStackFromBottom) {
            this.mFirstPosition = Math.max(0, pos + 1);
        }
        setVisibleRangeHint(this.mFirstPosition, (this.mFirstPosition + getChildCount()) - 1);
        return selectedView;
    }

    private View fillFromTop(int nextTop) {
        this.mFirstPosition = Math.min(this.mFirstPosition, this.mSelectedPosition);
        this.mFirstPosition = Math.min(this.mFirstPosition, this.mItemCount - 1);
        if (this.mFirstPosition < 0) {
            this.mFirstPosition = 0;
        }
        this.mFirstPosition -= this.mFirstPosition % this.mNumColumns;
        return fillDown(this.mFirstPosition, nextTop);
    }

    private View fillFromBottom(int lastPosition, int nextBottom) {
        int invertedPosition = (this.mItemCount - 1) - Math.min(Math.max(lastPosition, this.mSelectedPosition), this.mItemCount - 1);
        return fillUp((this.mItemCount - 1) - (invertedPosition - (invertedPosition % this.mNumColumns)), nextBottom);
    }

    private View fillSelection(int childrenTop, int childrenBottom) {
        int invertedSelection;
        int selectedPosition = reconcileSelectedPosition();
        int numColumns = this.mNumColumns;
        int verticalSpacing = this.mVerticalSpacing;
        int rowEnd = -1;
        if (this.mStackFromBottom) {
            invertedSelection = (this.mItemCount - 1) - selectedPosition;
            rowEnd = (this.mItemCount - 1) - (invertedSelection - (invertedSelection % numColumns));
            invertedSelection = Math.max(0, (rowEnd - numColumns) + 1);
        } else {
            invertedSelection = selectedPosition - (selectedPosition % numColumns);
        }
        int fadingEdgeLength = getVerticalFadingEdgeLength();
        View sel = makeRow(this.mStackFromBottom ? rowEnd : invertedSelection, getTopSelectionPixel(childrenTop, fadingEdgeLength, invertedSelection), true);
        this.mFirstPosition = invertedSelection;
        View referenceView = this.mReferenceView;
        if (this.mStackFromBottom) {
            offsetChildrenTopAndBottom(getBottomSelectionPixel(childrenBottom, fadingEdgeLength, numColumns, invertedSelection) - referenceView.getBottom());
            fillUp(invertedSelection - 1, referenceView.getTop() - verticalSpacing);
            pinToTop(childrenTop);
            fillDown(rowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
            adjustViewsUpOrDown();
        } else {
            fillDown(invertedSelection + numColumns, referenceView.getBottom() + verticalSpacing);
            pinToBottom(childrenBottom);
            fillUp(invertedSelection - numColumns, referenceView.getTop() - verticalSpacing);
            adjustViewsUpOrDown();
        }
        return sel;
    }

    private void pinToTop(int childrenTop) {
        if (this.mFirstPosition == 0) {
            int offset = childrenTop - getChildAt(0).getTop();
            if (offset < 0) {
                offsetChildrenTopAndBottom(offset);
            }
        }
    }

    private void pinToBottom(int childrenBottom) {
        int count = getChildCount();
        if (this.mFirstPosition + count == this.mItemCount) {
            int offset = childrenBottom - getChildAt(count - 1).getBottom();
            if (offset > 0) {
                offsetChildrenTopAndBottom(offset);
            }
        }
    }

    int findMotionRow(int y) {
        int childCount = getChildCount();
        if (childCount > 0) {
            int numColumns = this.mNumColumns;
            int i;
            if (this.mStackFromBottom) {
                for (i = childCount - 1; i >= 0; i -= numColumns) {
                    if (y >= getChildAt(i).getTop()) {
                        return this.mFirstPosition + i;
                    }
                }
            } else {
                for (i = 0; i < childCount; i += numColumns) {
                    if (y <= getChildAt(i).getBottom()) {
                        return this.mFirstPosition + i;
                    }
                }
            }
        }
        return -1;
    }

    private View fillSpecific(int position, int top) {
        int invertedSelection;
        int numColumns = this.mNumColumns;
        int motionRowEnd = -1;
        if (this.mStackFromBottom) {
            invertedSelection = (this.mItemCount - 1) - position;
            motionRowEnd = (this.mItemCount - 1) - (invertedSelection - (invertedSelection % numColumns));
            invertedSelection = Math.max(0, (motionRowEnd - numColumns) + 1);
        } else {
            invertedSelection = position - (position % numColumns);
        }
        View temp = makeRow(this.mStackFromBottom ? motionRowEnd : invertedSelection, top, true);
        this.mFirstPosition = invertedSelection;
        View referenceView = this.mReferenceView;
        if (referenceView == null) {
            return null;
        }
        View below;
        View above;
        int verticalSpacing = this.mVerticalSpacing;
        int childCount;
        if (this.mStackFromBottom) {
            below = fillDown(motionRowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
            adjustViewsUpOrDown();
            above = fillUp(invertedSelection - 1, referenceView.getTop() - verticalSpacing);
            childCount = getChildCount();
            if (childCount > 0) {
                correctTooLow(numColumns, verticalSpacing, childCount);
            }
        } else {
            above = fillUp(invertedSelection - numColumns, referenceView.getTop() - verticalSpacing);
            adjustViewsUpOrDown();
            below = fillDown(invertedSelection + numColumns, referenceView.getBottom() + verticalSpacing);
            childCount = getChildCount();
            if (childCount > 0) {
                correctTooHigh(numColumns, verticalSpacing, childCount);
            }
        }
        if (temp != null) {
            return temp;
        }
        if (above != null) {
            return above;
        }
        return below;
    }

    private void correctTooHigh(int numColumns, int verticalSpacing, int childCount) {
        int i = 1;
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
                    int i2 = this.mFirstPosition;
                    if (!this.mStackFromBottom) {
                        i = numColumns;
                    }
                    fillUp(i2 - i, firstChild.getTop() - verticalSpacing);
                    adjustViewsUpOrDown();
                }
            }
        }
    }

    private void correctTooLow(int numColumns, int verticalSpacing, int childCount) {
        if (this.mFirstPosition == 0 && childCount > 0) {
            int end = (this.mBottom - this.mTop) - this.mListPadding.bottom;
            int topOffset = getChildAt(null).getTop() - this.mListPadding.top;
            View lastChild = getChildAt(childCount - 1);
            int lastBottom = lastChild.getBottom();
            int i = 1;
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
                    if (this.mStackFromBottom) {
                        i = numColumns;
                    }
                    fillDown(i + lastPosition, lastChild.getBottom() + verticalSpacing);
                    adjustViewsUpOrDown();
                }
            }
        }
    }

    private View fillFromSelection(int selectedTop, int childrenTop, int childrenBottom) {
        int invertedSelection;
        int fadingEdgeLength = getVerticalFadingEdgeLength();
        int selectedPosition = this.mSelectedPosition;
        int numColumns = this.mNumColumns;
        int verticalSpacing = this.mVerticalSpacing;
        int rowEnd = -1;
        if (this.mStackFromBottom) {
            invertedSelection = (this.mItemCount - 1) - selectedPosition;
            rowEnd = (this.mItemCount - 1) - (invertedSelection - (invertedSelection % numColumns));
            invertedSelection = Math.max(0, (rowEnd - numColumns) + 1);
        } else {
            invertedSelection = selectedPosition - (selectedPosition % numColumns);
        }
        int topSelectionPixel = getTopSelectionPixel(childrenTop, fadingEdgeLength, invertedSelection);
        int bottomSelectionPixel = getBottomSelectionPixel(childrenBottom, fadingEdgeLength, numColumns, invertedSelection);
        View sel = makeRow(this.mStackFromBottom ? rowEnd : invertedSelection, selectedTop, true);
        this.mFirstPosition = invertedSelection;
        View referenceView = this.mReferenceView;
        adjustForTopFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);
        adjustForBottomFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);
        if (this.mStackFromBottom) {
            fillDown(rowEnd + numColumns, referenceView.getBottom() + verticalSpacing);
            adjustViewsUpOrDown();
            fillUp(invertedSelection - 1, referenceView.getTop() - verticalSpacing);
        } else {
            fillUp(invertedSelection - numColumns, referenceView.getTop() - verticalSpacing);
            adjustViewsUpOrDown();
            fillDown(invertedSelection + numColumns, referenceView.getBottom() + verticalSpacing);
        }
        return sel;
    }

    private int getBottomSelectionPixel(int childrenBottom, int fadingEdgeLength, int numColumns, int rowStart) {
        int bottomSelectionPixel = childrenBottom;
        if ((rowStart + numColumns) - 1 < this.mItemCount - 1) {
            return bottomSelectionPixel - fadingEdgeLength;
        }
        return bottomSelectionPixel;
    }

    private int getTopSelectionPixel(int childrenTop, int fadingEdgeLength, int rowStart) {
        int topSelectionPixel = childrenTop;
        if (rowStart > 0) {
            return topSelectionPixel + fadingEdgeLength;
        }
        return topSelectionPixel;
    }

    private void adjustForBottomFadingEdge(View childInSelectedRow, int topSelectionPixel, int bottomSelectionPixel) {
        if (childInSelectedRow.getBottom() > bottomSelectionPixel) {
            offsetChildrenTopAndBottom(-Math.min(childInSelectedRow.getTop() - topSelectionPixel, childInSelectedRow.getBottom() - bottomSelectionPixel));
        }
    }

    private void adjustForTopFadingEdge(View childInSelectedRow, int topSelectionPixel, int bottomSelectionPixel) {
        if (childInSelectedRow.getTop() < topSelectionPixel) {
            offsetChildrenTopAndBottom(Math.min(topSelectionPixel - childInSelectedRow.getTop(), bottomSelectionPixel - childInSelectedRow.getBottom()));
        }
    }

    @RemotableViewMethod
    public void smoothScrollToPosition(int position) {
        super.smoothScrollToPosition(position);
    }

    @RemotableViewMethod
    public void smoothScrollByOffset(int offset) {
        super.smoothScrollByOffset(offset);
    }

    private View moveSelection(int delta, int childrenTop, int childrenBottom) {
        int invertedSelection;
        int rowStart;
        int invertedSelection2;
        View sel;
        View sel2;
        int fadingEdgeLength = getVerticalFadingEdgeLength();
        int selectedPosition = this.mSelectedPosition;
        int numColumns = this.mNumColumns;
        int verticalSpacing = this.mVerticalSpacing;
        int rowEnd = -1;
        if (this.mStackFromBottom) {
            invertedSelection = (this.mItemCount - 1) - selectedPosition;
            rowEnd = (this.mItemCount - 1) - (invertedSelection - (invertedSelection % numColumns));
            rowStart = Math.max(0, (rowEnd - numColumns) + 1);
            invertedSelection2 = (this.mItemCount - 1) - (selectedPosition - delta);
            invertedSelection = Math.max(0, (((this.mItemCount - 1) - (invertedSelection2 - (invertedSelection2 % numColumns))) - numColumns) + 1);
        } else {
            invertedSelection = (selectedPosition - delta) - ((selectedPosition - delta) % numColumns);
            rowStart = selectedPosition - (selectedPosition % numColumns);
        }
        invertedSelection2 = rowStart - invertedSelection;
        int topSelectionPixel = getTopSelectionPixel(childrenTop, fadingEdgeLength, rowStart);
        int bottomSelectionPixel = getBottomSelectionPixel(childrenBottom, fadingEdgeLength, numColumns, rowStart);
        this.mFirstPosition = rowStart;
        if (invertedSelection2 > 0) {
            sel = makeRow(this.mStackFromBottom ? rowEnd : rowStart, (this.mReferenceViewInSelectedRow == null ? 0 : this.mReferenceViewInSelectedRow.getBottom()) + verticalSpacing, 1);
            fadingEdgeLength = this.mReferenceView;
            adjustForBottomFadingEdge(fadingEdgeLength, topSelectionPixel, bottomSelectionPixel);
            View view = fadingEdgeLength;
            sel2 = sel;
            sel = view;
        } else {
            if (invertedSelection2 < 0) {
                sel2 = makeRow(this.mStackFromBottom ? rowEnd : rowStart, (this.mReferenceViewInSelectedRow == null ? 0 : this.mReferenceViewInSelectedRow.getTop()) - verticalSpacing, false);
                View referenceView = this.mReferenceView;
                adjustForTopFadingEdge(referenceView, topSelectionPixel, bottomSelectionPixel);
                sel = referenceView;
            } else {
                sel2 = makeRow(this.mStackFromBottom ? rowEnd : rowStart, this.mReferenceViewInSelectedRow == null ? 0 : this.mReferenceViewInSelectedRow.getTop(), true);
                sel = this.mReferenceView;
            }
        }
        if (this.mStackFromBottom) {
            fillDown(rowEnd + numColumns, sel.getBottom() + verticalSpacing);
            adjustViewsUpOrDown();
            fillUp(rowStart - 1, sel.getTop() - verticalSpacing);
        } else {
            fillUp(rowStart - numColumns, sel.getTop() - verticalSpacing);
            adjustViewsUpOrDown();
            fillDown(rowStart + numColumns, sel.getBottom() + verticalSpacing);
        }
        return sel2;
    }

    private boolean determineColumns(int availableSpace) {
        int requestedHorizontalSpacing = this.mRequestedHorizontalSpacing;
        int stretchMode = this.mStretchMode;
        int requestedColumnWidth = this.mRequestedColumnWidth;
        boolean didNotInitiallyFit = false;
        if (this.mRequestedNumColumns != -1) {
            this.mNumColumns = this.mRequestedNumColumns;
        } else if (requestedColumnWidth > 0) {
            this.mNumColumns = (availableSpace + requestedHorizontalSpacing) / (requestedColumnWidth + requestedHorizontalSpacing);
        } else {
            this.mNumColumns = 2;
        }
        if (this.mNumColumns <= 0) {
            this.mNumColumns = 1;
        }
        if (stretchMode != 0) {
            int spaceLeftOver = (availableSpace - (this.mNumColumns * requestedColumnWidth)) - ((this.mNumColumns - 1) * requestedHorizontalSpacing);
            if (spaceLeftOver < 0) {
                didNotInitiallyFit = true;
            }
            switch (stretchMode) {
                case 1:
                    this.mColumnWidth = requestedColumnWidth;
                    if (this.mNumColumns <= 1) {
                        this.mHorizontalSpacing = requestedHorizontalSpacing + spaceLeftOver;
                        break;
                    }
                    this.mHorizontalSpacing = (spaceLeftOver / (this.mNumColumns - 1)) + requestedHorizontalSpacing;
                    break;
                case 2:
                    this.mColumnWidth = (spaceLeftOver / this.mNumColumns) + requestedColumnWidth;
                    this.mHorizontalSpacing = requestedHorizontalSpacing;
                    break;
                case 3:
                    this.mColumnWidth = requestedColumnWidth;
                    if (this.mNumColumns <= 1) {
                        this.mHorizontalSpacing = requestedHorizontalSpacing + spaceLeftOver;
                        break;
                    }
                    this.mHorizontalSpacing = (spaceLeftOver / (this.mNumColumns + 1)) + requestedHorizontalSpacing;
                    break;
            }
        }
        this.mColumnWidth = requestedColumnWidth;
        this.mHorizontalSpacing = requestedHorizontalSpacing;
        return didNotInitiallyFit;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == 0) {
            int widthSize2;
            if (this.mColumnWidth > 0) {
                widthSize2 = (this.mColumnWidth + this.mListPadding.left) + this.mListPadding.right;
            } else {
                widthSize2 = this.mListPadding.left + this.mListPadding.right;
            }
            widthSize = getVerticalScrollbarWidth() + widthSize2;
        }
        boolean didNotInitiallyFit = determineColumns((widthSize - this.mListPadding.left) - this.mListPadding.right);
        int childHeight = 0;
        this.mItemCount = this.mAdapter == null ? 0 : this.mAdapter.getCount();
        int count = this.mItemCount;
        if (count > 0) {
            View child = obtainView(0, this.mIsScrap);
            LayoutParams p = (LayoutParams) child.getLayoutParams();
            if (p == null) {
                p = (LayoutParams) generateDefaultLayoutParams();
                child.setLayoutParams(p);
            }
            p.viewType = this.mAdapter.getItemViewType(0);
            p.isEnabled = this.mAdapter.isEnabled(0);
            p.forceAdd = true;
            child.measure(ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(this.mColumnWidth, 1073741824), 0, p.width), ViewGroup.getChildMeasureSpec(MeasureSpec.makeSafeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), 0), 0, p.height));
            childHeight = child.getMeasuredHeight();
            int childState = View.combineMeasuredStates(0, child.getMeasuredState());
            if (this.mRecycler.shouldRecycleViewType(p.viewType)) {
                this.mRecycler.addScrapView(child, -1);
            }
        }
        if (heightMode == 0) {
            heightSize = ((this.mListPadding.top + this.mListPadding.bottom) + childHeight) + (getVerticalFadingEdgeLength() * 2);
        }
        if (heightMode == Integer.MIN_VALUE) {
            int ourSize = this.mListPadding.top + this.mListPadding.bottom;
            int numColumns = this.mNumColumns;
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= count) {
                    break;
                }
                ourSize += childHeight;
                if (i2 + numColumns < count) {
                    ourSize += this.mVerticalSpacing;
                }
                if (ourSize >= heightSize) {
                    ourSize = heightSize;
                    break;
                }
                i = i2 + numColumns;
            }
            heightSize = ourSize;
        }
        if (widthMode == Integer.MIN_VALUE && this.mRequestedNumColumns != -1 && ((((this.mRequestedNumColumns * this.mColumnWidth) + ((this.mRequestedNumColumns - 1) * this.mHorizontalSpacing)) + this.mListPadding.left) + this.mListPadding.right > widthSize || didNotInitiallyFit)) {
            widthSize |= 16777216;
        }
        setMeasuredDimension(widthSize, heightSize);
        this.mWidthMeasureSpec = widthMeasureSpec;
    }

    protected void attachLayoutAnimationParameters(View child, ViewGroup.LayoutParams params, int index, int count) {
        AnimationParameters animationParams = params.layoutAnimationParameters;
        if (animationParams == null) {
            animationParams = new AnimationParameters();
            params.layoutAnimationParameters = animationParams;
        }
        animationParams.count = count;
        animationParams.index = index;
        animationParams.columnsCount = this.mNumColumns;
        animationParams.rowsCount = count / this.mNumColumns;
        if (this.mStackFromBottom) {
            int invertedIndex = (count - 1) - index;
            animationParams.column = (this.mNumColumns - 1) - (invertedIndex % this.mNumColumns);
            animationParams.row = (animationParams.rowsCount - 1) - (invertedIndex / this.mNumColumns);
            return;
        }
        animationParams.column = index % this.mNumColumns;
        animationParams.row = index / this.mNumColumns;
    }

    /* JADX WARNING: Removed duplicated region for block: B:79:0x00fb A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00d6  */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0110 A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x014e A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x013c A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x012f A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x0125 A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x011b A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0115 A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x01d7 A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:129:0x01cb A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x028e A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:177:0x02a5 A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x02ad  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x00d6  */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x00fb A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0110 A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x014e A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x013c A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x012f A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x0125 A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x011b A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0115 A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:129:0x01cb A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x01d7 A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x021a A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x028e A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:177:0x02a5 A:{Catch:{ all -> 0x02b1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x02ad  */
    /* JADX WARNING: Removed duplicated region for block: B:186:0x02b8  */
    /* JADX WARNING: Removed duplicated region for block: B:186:0x02b8  */
    /* JADX WARNING: Removed duplicated region for block: B:186:0x02b8  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void layoutChildren() {
        Throwable th;
        boolean blockLayoutRequests;
        GridView gridView = this;
        boolean blockLayoutRequests2 = gridView.mBlockLayoutRequests;
        if (!blockLayoutRequests2) {
            gridView.mBlockLayoutRequests = true;
        }
        super.layoutChildren();
        invalidate();
        if (gridView.mAdapter == null) {
            try {
                resetList();
                invokeOnItemScrollListener();
                if (!blockLayoutRequests2) {
                    gridView.mBlockLayoutRequests = false;
                }
                return;
            } catch (Throwable th2) {
                th = th2;
                blockLayoutRequests = blockLayoutRequests2;
                if (!blockLayoutRequests) {
                }
                throw th;
            }
        }
        int index;
        int childrenTop = gridView.mListPadding.top;
        int childrenBottom = (gridView.mBottom - gridView.mTop) - gridView.mListPadding.bottom;
        int childCount = getChildCount();
        int delta = 0;
        View oldSel = null;
        View oldFirst = null;
        View newSel = null;
        switch (gridView.mLayoutMode) {
            case 1:
            case 3:
            case 4:
            case 5:
                break;
            case 2:
                index = gridView.mNextSelectedPosition - gridView.mFirstPosition;
                if (index >= 0 && index < childCount) {
                    newSel = gridView.getChildAt(index);
                }
            case 6:
                if (gridView.mNextSelectedPosition >= 0) {
                    delta = gridView.mNextSelectedPosition - gridView.mSelectedPosition;
                }
            default:
                index = gridView.mSelectedPosition - gridView.mFirstPosition;
                if (index >= 0 && index < childCount) {
                    oldSel = gridView.getChildAt(index);
                }
                try {
                    oldFirst = gridView.getChildAt(0);
                } catch (Throwable th3) {
                    th = th3;
                    blockLayoutRequests = blockLayoutRequests2;
                    if (blockLayoutRequests) {
                    }
                    throw th;
                }
                break;
        }
        boolean dataChanged = gridView.mDataChanged;
        if (dataChanged) {
            handleDataChanged();
        }
        if (gridView.mItemCount == 0) {
            resetList();
            invokeOnItemScrollListener();
            if (!blockLayoutRequests2) {
                gridView.mBlockLayoutRequests = false;
            }
            return;
        }
        AccessibilityNodeInfo accessibilityFocusLayoutRestoreNode;
        int firstPosition;
        RecycleBin recycleBin;
        int accessibilityFocusPosition;
        View sel;
        RecycleBin recycleBin2;
        gridView.setSelectedPositionInt(gridView.mNextSelectedPosition);
        View accessibilityFocusLayoutRestoreView = null;
        int accessibilityFocusPosition2 = -1;
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null) {
            View focusHost = viewRootImpl.getAccessibilityFocusedHost();
            if (focusHost != null) {
                View focusChild = gridView.getAccessibilityFocusedChild(focusHost);
                if (focusChild != null) {
                    AccessibilityNodeInfo accessibilityFocusLayoutRestoreNode2;
                    if (!dataChanged || focusChild.hasTransientState()) {
                        accessibilityFocusLayoutRestoreNode2 = null;
                    } else {
                        accessibilityFocusLayoutRestoreNode2 = null;
                        if (gridView.mAdapterHasStableIds == null) {
                            accessibilityFocusLayoutRestoreNode = accessibilityFocusLayoutRestoreNode2;
                            accessibilityFocusPosition2 = gridView.getPositionForView(focusChild);
                            firstPosition = gridView.mFirstPosition;
                            recycleBin = gridView.mRecycler;
                            boolean dataChanged2;
                            if (dataChanged) {
                                int i = 0;
                                while (true) {
                                    dataChanged2 = dataChanged;
                                    index = i;
                                    if (index < childCount) {
                                        blockLayoutRequests = blockLayoutRequests2;
                                        try {
                                            accessibilityFocusPosition = accessibilityFocusPosition2;
                                            recycleBin.addScrapView(gridView.getChildAt(index), firstPosition + index);
                                            i = index + 1;
                                            dataChanged = dataChanged2;
                                            blockLayoutRequests2 = blockLayoutRequests;
                                            accessibilityFocusPosition2 = accessibilityFocusPosition;
                                        } catch (Throwable th4) {
                                            th = th4;
                                            if (blockLayoutRequests) {
                                            }
                                            throw th;
                                        }
                                    }
                                    blockLayoutRequests = blockLayoutRequests2;
                                    accessibilityFocusPosition = accessibilityFocusPosition2;
                                }
                            } else {
                                blockLayoutRequests = blockLayoutRequests2;
                                dataChanged2 = dataChanged;
                                accessibilityFocusPosition = accessibilityFocusPosition2;
                                recycleBin.fillActiveViews(childCount, firstPosition);
                            }
                            detachAllViewsFromParent();
                            recycleBin.removeSkippedScrap();
                            switch (gridView.mLayoutMode) {
                                case 1:
                                    gridView.mFirstPosition = 0;
                                    sel = gridView.fillFromTop(childrenTop);
                                    adjustViewsUpOrDown();
                                    break;
                                case 2:
                                    if (newSel == null) {
                                        sel = gridView.fillSelection(childrenTop, childrenBottom);
                                        break;
                                    }
                                    sel = gridView.fillFromSelection(newSel.getTop(), childrenTop, childrenBottom);
                                case 3:
                                    sel = gridView.fillUp(gridView.mItemCount - 1, childrenBottom);
                                    adjustViewsUpOrDown();
                                    break;
                                case 4:
                                    sel = gridView.fillSpecific(gridView.mSelectedPosition, gridView.mSpecificTop);
                                    break;
                                case 5:
                                    sel = gridView.fillSpecific(gridView.mSyncPosition, gridView.mSpecificTop);
                                    break;
                                case 6:
                                    sel = gridView.moveSelection(delta, childrenTop, childrenBottom);
                                    break;
                                default:
                                    int i2;
                                    if (childCount == 0) {
                                        if (gridView.mStackFromBottom == null) {
                                            if (gridView.mAdapter != null) {
                                                if (!isInTouchMode()) {
                                                    i2 = 0;
                                                    gridView.setSelectedPositionInt(i2);
                                                    sel = gridView.fillFromTop(childrenTop);
                                                    break;
                                                }
                                            }
                                            i2 = -1;
                                            gridView.setSelectedPositionInt(i2);
                                            sel = gridView.fillFromTop(childrenTop);
                                        } else {
                                            i2 = gridView.mItemCount - 1;
                                            if (gridView.mAdapter != null) {
                                                if (!isInTouchMode()) {
                                                    accessibilityFocusPosition2 = i2;
                                                    gridView.setSelectedPositionInt(accessibilityFocusPosition2);
                                                    sel = gridView.fillFromBottom(i2, childrenBottom);
                                                    break;
                                                }
                                            }
                                            accessibilityFocusPosition2 = -1;
                                            gridView.setSelectedPositionInt(accessibilityFocusPosition2);
                                            sel = gridView.fillFromBottom(i2, childrenBottom);
                                        }
                                    } else if (gridView.mSelectedPosition < 0 || gridView.mSelectedPosition >= gridView.mItemCount) {
                                        if (gridView.mFirstPosition >= gridView.mItemCount) {
                                            sel = gridView.fillSpecific(0, childrenTop);
                                            break;
                                        }
                                        i2 = gridView.mFirstPosition;
                                        if (oldFirst == null) {
                                            accessibilityFocusPosition2 = childrenTop;
                                        } else {
                                            accessibilityFocusPosition2 = oldFirst.getTop();
                                        }
                                        sel = gridView.fillSpecific(i2, accessibilityFocusPosition2);
                                    } else {
                                        i2 = gridView.mSelectedPosition;
                                        if (oldSel == null) {
                                            accessibilityFocusPosition2 = childrenTop;
                                        } else {
                                            accessibilityFocusPosition2 = oldSel.getTop();
                                        }
                                        sel = gridView.fillSpecific(i2, accessibilityFocusPosition2);
                                    }
                                    break;
                            }
                            recycleBin.scrapActiveViews();
                            if (sel != null) {
                                gridView.positionSelector(-1, sel);
                                gridView.mSelectedTop = sel.getTop();
                                int i3 = firstPosition;
                            } else {
                                dataChanged = gridView.mTouchMode > 0 && gridView.mTouchMode < 3;
                                if (dataChanged) {
                                    firstPosition = gridView.getChildAt(gridView.mMotionPosition - gridView.mFirstPosition);
                                    if (firstPosition != 0) {
                                        gridView.positionSelector(gridView.mMotionPosition, firstPosition);
                                    }
                                } else {
                                    if (gridView.mSelectedPosition != -1) {
                                        focusChild = gridView.getChildAt(gridView.mSelectorPosition - gridView.mFirstPosition);
                                        if (focusChild != null) {
                                            gridView.positionSelector(gridView.mSelectorPosition, focusChild);
                                        }
                                    } else {
                                        gridView.mSelectedTop = 0;
                                        gridView.mSelectorRect.setEmpty();
                                    }
                                }
                            }
                            if (viewRootImpl != null || viewRootImpl.getAccessibilityFocusedHost() != null) {
                                recycleBin2 = recycleBin;
                                accessibilityFocusPosition2 = accessibilityFocusPosition;
                            } else if (accessibilityFocusLayoutRestoreView == null || !accessibilityFocusLayoutRestoreView.isAttachedToWindow()) {
                                View view = sel;
                                accessibilityFocusPosition2 = accessibilityFocusPosition;
                                if (accessibilityFocusPosition2 != -1) {
                                    gridView = this;
                                    sel = gridView.getChildAt(MathUtils.constrain(accessibilityFocusPosition2 - gridView.mFirstPosition, (int) null, getChildCount() - 1));
                                    if (sel != null) {
                                        sel.requestAccessibilityFocus();
                                    }
                                } else {
                                    gridView = this;
                                }
                            } else {
                                AccessibilityNodeProvider provider = accessibilityFocusLayoutRestoreView.getAccessibilityNodeProvider();
                                if (accessibilityFocusLayoutRestoreNode == null || provider == null) {
                                    accessibilityFocusLayoutRestoreView.requestAccessibilityFocus();
                                } else {
                                    try {
                                        provider.performAction(AccessibilityNodeInfo.getVirtualDescendantId(accessibilityFocusLayoutRestoreNode.getSourceNodeId()), 64, null);
                                    } catch (Throwable th5) {
                                        th = th5;
                                        gridView = this;
                                        if (blockLayoutRequests) {
                                            gridView.mBlockLayoutRequests = false;
                                        }
                                        throw th;
                                    }
                                }
                                recycleBin2 = recycleBin;
                                accessibilityFocusPosition2 = accessibilityFocusPosition;
                                gridView = this;
                            }
                            gridView.mLayoutMode = 0;
                            gridView.mDataChanged = false;
                            if (gridView.mPositionScrollAfterLayout != null) {
                                gridView.post(gridView.mPositionScrollAfterLayout);
                                gridView.mPositionScrollAfterLayout = null;
                            }
                            gridView.mNeedSync = false;
                            gridView.setNextSelectedPositionInt(gridView.mSelectedPosition);
                            updateScrollIndicators();
                            if (gridView.mItemCount > 0) {
                                checkSelectionChanged();
                            }
                            invokeOnItemScrollListener();
                            if (!blockLayoutRequests) {
                                gridView.mBlockLayoutRequests = false;
                            }
                        }
                    }
                    accessibilityFocusLayoutRestoreView = focusHost;
                    accessibilityFocusLayoutRestoreNode = viewRootImpl.getAccessibilityFocusedVirtualView();
                    accessibilityFocusPosition2 = gridView.getPositionForView(focusChild);
                    firstPosition = gridView.mFirstPosition;
                    recycleBin = gridView.mRecycler;
                    if (dataChanged) {
                    }
                    detachAllViewsFromParent();
                    recycleBin.removeSkippedScrap();
                    switch (gridView.mLayoutMode) {
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            break;
                        case 4:
                            break;
                        case 5:
                            break;
                        case 6:
                            break;
                        default:
                            break;
                    }
                    recycleBin.scrapActiveViews();
                    if (sel != null) {
                    }
                    if (viewRootImpl != null) {
                    }
                    recycleBin2 = recycleBin;
                    accessibilityFocusPosition2 = accessibilityFocusPosition;
                    gridView.mLayoutMode = 0;
                    gridView.mDataChanged = false;
                    if (gridView.mPositionScrollAfterLayout != null) {
                    }
                    gridView.mNeedSync = false;
                    gridView.setNextSelectedPositionInt(gridView.mSelectedPosition);
                    updateScrollIndicators();
                    if (gridView.mItemCount > 0) {
                    }
                    invokeOnItemScrollListener();
                    if (blockLayoutRequests) {
                    }
                }
            }
        }
        accessibilityFocusLayoutRestoreNode = null;
        firstPosition = gridView.mFirstPosition;
        recycleBin = gridView.mRecycler;
        if (dataChanged) {
        }
        detachAllViewsFromParent();
        recycleBin.removeSkippedScrap();
        switch (gridView.mLayoutMode) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            default:
                break;
        }
        recycleBin.scrapActiveViews();
        if (sel != null) {
        }
        if (viewRootImpl != null) {
        }
        recycleBin2 = recycleBin;
        accessibilityFocusPosition2 = accessibilityFocusPosition;
        gridView.mLayoutMode = 0;
        gridView.mDataChanged = false;
        if (gridView.mPositionScrollAfterLayout != null) {
        }
        gridView.mNeedSync = false;
        gridView.setNextSelectedPositionInt(gridView.mSelectedPosition);
        updateScrollIndicators();
        if (gridView.mItemCount > 0) {
        }
        invokeOnItemScrollListener();
        if (blockLayoutRequests) {
        }
    }

    private View makeAndAddView(int position, int y, boolean flow, int childrenLeft, boolean selected, int where) {
        View activeView;
        int i = position;
        if (!this.mDataChanged) {
            activeView = this.mRecycler.getActiveView(i);
            if (activeView != null) {
                setupChild(activeView, i, y, flow, childrenLeft, selected, true, where);
                return activeView;
            }
        }
        activeView = obtainView(i, this.mIsScrap);
        setupChild(activeView, i, y, flow, childrenLeft, selected, this.mIsScrap[0], where);
        return activeView;
    }

    private void setupChild(View child, int position, int y, boolean flowDown, int childrenLeft, boolean selected, boolean isAttachedToWindow, int where) {
        int i;
        View view = child;
        int i2 = position;
        int i3 = where;
        Trace.traceBegin(8, "setupGridItem");
        boolean isSelected = selected && shouldShowSelector();
        boolean updateChildSelected = isSelected != child.isSelected();
        int mode = this.mTouchMode;
        boolean isPressed = mode > 0 && mode < 3 && this.mMotionPosition == i2;
        boolean updateChildPressed = isPressed != child.isPressed();
        boolean needToMeasure = !isAttachedToWindow || updateChildSelected || child.isLayoutRequested();
        LayoutParams p = (LayoutParams) child.getLayoutParams();
        if (p == null) {
            p = (LayoutParams) generateDefaultLayoutParams();
        }
        LayoutParams p2 = p;
        p2.viewType = this.mAdapter.getItemViewType(i2);
        p2.isEnabled = this.mAdapter.isEnabled(i2);
        if (updateChildSelected) {
            view.setSelected(isSelected);
            if (isSelected) {
                requestFocus();
            }
        }
        if (updateChildPressed) {
            view.setPressed(isPressed);
        }
        if (!(this.mChoiceMode == 0 || this.mCheckStates == null)) {
            if (view instanceof Checkable) {
                ((Checkable) view).setChecked(this.mCheckStates.get(i2));
            } else if (getContext().getApplicationInfo().targetSdkVersion >= 11) {
                view.setActivated(this.mCheckStates.get(i2));
            }
        }
        if (!isAttachedToWindow || p2.forceAdd) {
            i = 0;
            p2.forceAdd = false;
            addViewInLayout(view, i3, p2, true);
        } else {
            attachViewToParent(view, i3, p2);
            if (!(isAttachedToWindow && ((LayoutParams) child.getLayoutParams()).scrappedFromPosition == i2)) {
                child.jumpDrawablesToCurrentState();
            }
            i = 0;
        }
        if (needToMeasure) {
            view.measure(ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(this.mColumnWidth, 1073741824), 0, p2.width), ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(i, i), i, p2.height));
        } else {
            cleanupLayoutState(child);
        }
        i2 = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();
        int childTop = flowDown ? y : y - h;
        i3 = getLayoutDirection();
        i = Gravity.getAbsoluteGravity(this.mGravity, i3);
        i3 = i & 7;
        if (i3 == 1) {
            i3 = childrenLeft + ((this.mColumnWidth - i2) / 2);
        } else if (i3 == 3) {
            i3 = childrenLeft;
        } else if (i3 != 5) {
            i3 = childrenLeft;
        } else {
            i3 = (childrenLeft + this.mColumnWidth) - i2;
        }
        if (needToMeasure) {
            i = i3 + i2;
            i2 = childTop;
            view.layout(i3, i2, i, i2 + h);
        } else {
            i2 = childTop;
            view.offsetLeftAndRight(i3 - child.getLeft());
            view.offsetTopAndBottom(i2 - child.getTop());
        }
        if (this.mCachingStarted && !child.isDrawingCacheEnabled()) {
            view.setDrawingCacheEnabled(true);
        }
        Trace.traceEnd(8);
    }

    public void setSelection(int position) {
        if (isInTouchMode()) {
            this.mResurrectToPosition = position;
        } else {
            setNextSelectedPositionInt(position);
        }
        this.mLayoutMode = 2;
        if (this.mPositionScroller != null) {
            this.mPositionScroller.stop();
        }
        requestLayout();
    }

    void setSelectionInt(int position) {
        int next;
        int previousSelectedPosition = this.mNextSelectedPosition;
        if (this.mPositionScroller != null) {
            this.mPositionScroller.stop();
        }
        setNextSelectedPositionInt(position);
        layoutChildren();
        if (this.mStackFromBottom) {
            next = (this.mItemCount - 1) - this.mNextSelectedPosition;
        } else {
            next = this.mNextSelectedPosition;
        }
        if (next / this.mNumColumns != (this.mStackFromBottom ? (this.mItemCount - 1) - previousSelectedPosition : previousSelectedPosition) / this.mNumColumns) {
            awakenScrollBars();
        }
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

    private boolean commonKey(int keyCode, int count, KeyEvent event) {
        if (this.mAdapter == null) {
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
            switch (keyCode) {
                case 19:
                    if (!event.hasNoModifiers()) {
                        if (event.hasModifiers(2)) {
                            z = resurrectSelectionIfNeeded() || fullScroll(33);
                            handled = z;
                            break;
                        }
                    }
                    z = resurrectSelectionIfNeeded() || arrowScroll(33);
                    handled = z;
                    break;
                    break;
                case 20:
                    if (!event.hasNoModifiers()) {
                        if (event.hasModifiers(2)) {
                            z = resurrectSelectionIfNeeded() || fullScroll(130);
                            handled = z;
                            break;
                        }
                    }
                    z = resurrectSelectionIfNeeded() || arrowScroll(130);
                    handled = z;
                    break;
                    break;
                case 21:
                    if (event.hasNoModifiers()) {
                        z = resurrectSelectionIfNeeded() || arrowScroll(17);
                        handled = z;
                        break;
                    }
                    break;
                case 22:
                    if (event.hasNoModifiers()) {
                        z = resurrectSelectionIfNeeded() || arrowScroll(66);
                        handled = z;
                        break;
                    }
                    break;
                case 61:
                    if (!event.hasNoModifiers()) {
                        if (event.hasModifiers(1)) {
                            z = resurrectSelectionIfNeeded() || sequenceScroll(1);
                            handled = z;
                            break;
                        }
                    }
                    z = resurrectSelectionIfNeeded() || sequenceScroll(2);
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
        int nextPage = -1;
        if (direction == 33) {
            nextPage = Math.max(0, this.mSelectedPosition - getChildCount());
        } else if (direction == 130) {
            nextPage = Math.min(this.mItemCount - 1, this.mSelectedPosition + getChildCount());
        }
        if (nextPage < 0) {
            return false;
        }
        setSelectionInt(nextPage);
        invokeOnItemScrollListener();
        awakenScrollBars();
        return true;
    }

    boolean fullScroll(int direction) {
        boolean moved = false;
        if (direction == 33) {
            this.mLayoutMode = 2;
            setSelectionInt(0);
            invokeOnItemScrollListener();
            moved = true;
        } else if (direction == 130) {
            this.mLayoutMode = 2;
            setSelectionInt(this.mItemCount - 1);
            invokeOnItemScrollListener();
            moved = true;
        }
        if (moved) {
            awakenScrollBars();
        }
        return moved;
    }

    boolean arrowScroll(int direction) {
        int endOfRowPos;
        int startOfRowPos;
        int selectedPosition = this.mSelectedPosition;
        int numColumns = this.mNumColumns;
        boolean moved = false;
        if (this.mStackFromBottom) {
            endOfRowPos = (this.mItemCount - 1) - ((((this.mItemCount - 1) - selectedPosition) / numColumns) * numColumns);
            startOfRowPos = Math.max(0, (endOfRowPos - numColumns) + 1);
        } else {
            startOfRowPos = (selectedPosition / numColumns) * numColumns;
            endOfRowPos = Math.min((startOfRowPos + numColumns) - 1, this.mItemCount - 1);
        }
        if (direction != 33) {
            if (direction == 130 && endOfRowPos < this.mItemCount - 1) {
                this.mLayoutMode = 6;
                setSelectionInt(Math.min(selectedPosition + numColumns, this.mItemCount - 1));
                moved = true;
            }
        } else if (startOfRowPos > 0) {
            this.mLayoutMode = 6;
            setSelectionInt(Math.max(0, selectedPosition - numColumns));
            moved = true;
        }
        boolean isLayoutRtl = isRtlLocale();
        if (selectedPosition > startOfRowPos && ((direction == 17 && !isLayoutRtl) || (direction == 66 && isLayoutRtl))) {
            this.mLayoutMode = 6;
            setSelectionInt(Math.max(0, selectedPosition - 1));
            moved = true;
        } else if (selectedPosition < endOfRowPos && ((direction == 17 && isLayoutRtl) || (direction == 66 && !isLayoutRtl))) {
            this.mLayoutMode = 6;
            setSelectionInt(Math.min(selectedPosition + 1, this.mItemCount - 1));
            moved = true;
        }
        if (moved) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
            invokeOnItemScrollListener();
        }
        if (moved) {
            awakenScrollBars();
        }
        return moved;
    }

    boolean sequenceScroll(int direction) {
        int endOfRow;
        int startOfRow;
        int selectedPosition = this.mSelectedPosition;
        int numColumns = this.mNumColumns;
        int count = this.mItemCount;
        boolean z = false;
        if (this.mStackFromBottom) {
            endOfRow = (count - 1) - ((((count - 1) - selectedPosition) / numColumns) * numColumns);
            startOfRow = Math.max(0, (endOfRow - numColumns) + 1);
        } else {
            startOfRow = (selectedPosition / numColumns) * numColumns;
            endOfRow = Math.min((startOfRow + numColumns) - 1, count - 1);
        }
        boolean moved = false;
        boolean showScroll = false;
        switch (direction) {
            case 1:
                if (selectedPosition > 0) {
                    this.mLayoutMode = 6;
                    setSelectionInt(selectedPosition - 1);
                    moved = true;
                    if (selectedPosition == startOfRow) {
                        z = true;
                    }
                    showScroll = z;
                    break;
                }
                break;
            case 2:
                if (selectedPosition < count - 1) {
                    this.mLayoutMode = 6;
                    setSelectionInt(selectedPosition + 1);
                    moved = true;
                    if (selectedPosition == endOfRow) {
                        z = true;
                    }
                    showScroll = z;
                    break;
                }
                break;
        }
        if (moved) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
            invokeOnItemScrollListener();
        }
        if (showScroll) {
            awakenScrollBars();
        }
        return moved;
    }

    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        int closestChildIndex = -1;
        if (gainFocus && previouslyFocusedRect != null) {
            previouslyFocusedRect.offset(this.mScrollX, this.mScrollY);
            Rect otherRect = this.mTempRect;
            int minDistance = Integer.MAX_VALUE;
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (isCandidateSelection(i, direction)) {
                    View other = getChildAt(i);
                    other.getDrawingRect(otherRect);
                    offsetDescendantRectToMyCoords(other, otherRect);
                    int distance = AbsListView.getDistance(previouslyFocusedRect, otherRect, direction);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestChildIndex = i;
                    }
                }
            }
        }
        if (closestChildIndex >= 0) {
            setSelection(this.mFirstPosition + closestChildIndex);
        } else {
            requestLayout();
        }
    }

    private boolean isCandidateSelection(int childIndex, int direction) {
        int rowEnd;
        int rowStart;
        int count = getChildCount();
        int invertedIndex = (count - 1) - childIndex;
        boolean z = false;
        if (this.mStackFromBottom) {
            rowEnd = (count - 1) - (invertedIndex - (invertedIndex % this.mNumColumns));
            rowStart = Math.max(0, (rowEnd - this.mNumColumns) + 1);
        } else {
            rowStart = childIndex - (childIndex % this.mNumColumns);
            rowEnd = Math.min((this.mNumColumns + rowStart) - 1, count);
        }
        if (direction == 17) {
            if (childIndex == rowEnd) {
                z = true;
            }
            return z;
        } else if (direction == 33) {
            if (rowEnd == count - 1) {
                z = true;
            }
            return z;
        } else if (direction == 66) {
            if (childIndex == rowStart) {
                z = true;
            }
            return z;
        } else if (direction != 130) {
            switch (direction) {
                case 1:
                    if (childIndex == rowEnd && rowEnd == count - 1) {
                        z = true;
                    }
                    return z;
                case 2:
                    if (childIndex == rowStart && rowStart == 0) {
                        z = true;
                    }
                    return z;
                default:
                    throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD, FOCUS_BACKWARD}.");
            }
        } else {
            if (rowStart == 0) {
                z = true;
            }
            return z;
        }
    }

    public void setGravity(int gravity) {
        if (this.mGravity != gravity) {
            this.mGravity = gravity;
            requestLayoutIfNecessary();
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    public void setHorizontalSpacing(int horizontalSpacing) {
        if (horizontalSpacing != this.mRequestedHorizontalSpacing) {
            this.mRequestedHorizontalSpacing = horizontalSpacing;
            requestLayoutIfNecessary();
        }
    }

    public int getHorizontalSpacing() {
        return this.mHorizontalSpacing;
    }

    public int getRequestedHorizontalSpacing() {
        return this.mRequestedHorizontalSpacing;
    }

    public void setVerticalSpacing(int verticalSpacing) {
        if (verticalSpacing != this.mVerticalSpacing) {
            this.mVerticalSpacing = verticalSpacing;
            requestLayoutIfNecessary();
        }
    }

    public int getVerticalSpacing() {
        return this.mVerticalSpacing;
    }

    public void setStretchMode(int stretchMode) {
        if (stretchMode != this.mStretchMode) {
            this.mStretchMode = stretchMode;
            requestLayoutIfNecessary();
        }
    }

    public int getStretchMode() {
        return this.mStretchMode;
    }

    public void setColumnWidth(int columnWidth) {
        if (columnWidth != this.mRequestedColumnWidth) {
            this.mRequestedColumnWidth = columnWidth;
            requestLayoutIfNecessary();
        }
    }

    public int getColumnWidth() {
        return this.mColumnWidth;
    }

    public int getRequestedColumnWidth() {
        return this.mRequestedColumnWidth;
    }

    public void setNumColumns(int numColumns) {
        if (numColumns != this.mRequestedNumColumns) {
            this.mRequestedNumColumns = numColumns;
            requestLayoutIfNecessary();
        }
    }

    @ExportedProperty
    public int getNumColumns() {
        return this.mNumColumns;
    }

    private void adjustViewsUpOrDown() {
        int childCount = getChildCount();
        if (childCount > 0) {
            int delta;
            if (this.mStackFromBottom) {
                delta = getChildAt(childCount - 1).getBottom() - (getHeight() - this.mListPadding.bottom);
                if (this.mFirstPosition + childCount < this.mItemCount) {
                    delta += this.mVerticalSpacing;
                }
                if (delta > 0) {
                    delta = 0;
                }
            } else {
                delta = getChildAt(null).getTop() - this.mListPadding.top;
                if (this.mFirstPosition != 0) {
                    delta -= this.mVerticalSpacing;
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

    protected int computeVerticalScrollExtent() {
        int count = getChildCount();
        if (count <= 0) {
            return 0;
        }
        int numColumns = this.mNumColumns;
        int extent = (((count + numColumns) - 1) / numColumns) * 100;
        View view = getChildAt(0);
        int top = view.getTop();
        int height = view.getHeight();
        if (height > 0) {
            extent += (top * 100) / height;
        }
        view = getChildAt(count - 1);
        int bottom = view.getBottom();
        height = view.getHeight();
        if (height > 0) {
            extent -= ((bottom - getHeight()) * 100) / height;
        }
        return extent;
    }

    protected int computeVerticalScrollOffset() {
        if (this.mFirstPosition >= 0 && getChildCount() > 0) {
            View view = getChildAt(0);
            int top = view.getTop();
            int height = view.getHeight();
            if (height > 0) {
                int oddItemsOnFirstRow;
                int numColumns = this.mNumColumns;
                int rowCount = ((this.mItemCount + numColumns) - 1) / numColumns;
                if (isStackFromBottom()) {
                    oddItemsOnFirstRow = (rowCount * numColumns) - this.mItemCount;
                } else {
                    oddItemsOnFirstRow = 0;
                }
                return Math.max(((((this.mFirstPosition + oddItemsOnFirstRow) / numColumns) * 100) - ((top * 100) / height)) + ((int) (((((float) this.mScrollY) / ((float) getHeight())) * ((float) rowCount)) * 100.0f)), 0);
            }
        }
        return 0;
    }

    protected int computeVerticalScrollRange() {
        int numColumns = this.mNumColumns;
        int rowCount = ((this.mItemCount + numColumns) - 1) / numColumns;
        int result = Math.max(rowCount * 100, 0);
        if (this.mScrollY != 0) {
            return result + Math.abs((int) (((((float) this.mScrollY) / ((float) getHeight())) * ((float) rowCount)) * 100.0f));
        }
        return result;
    }

    public CharSequence getAccessibilityClassName() {
        return GridView.class.getName();
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        int columnsCount = getNumColumns();
        int rowsCount = getCount() / columnsCount;
        info.setCollectionInfo(CollectionInfo.obtain(rowsCount, columnsCount, null, getSelectionModeForAccessibility()));
        if (columnsCount > 0 || rowsCount > 0) {
            info.addAction(AccessibilityAction.ACTION_SCROLL_TO_POSITION);
        }
    }

    public boolean performAccessibilityActionInternal(int action, Bundle arguments) {
        if (super.performAccessibilityActionInternal(action, arguments)) {
            return true;
        }
        if (action == 16908343) {
            int numColumns = getNumColumns();
            int row = arguments.getInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT, -1);
            int position = Math.min(row * numColumns, getCount() - 1);
            if (row >= 0) {
                smoothScrollToPosition(position);
                return true;
            }
        }
        return false;
    }

    public void onInitializeAccessibilityNodeInfoForItem(View view, int position, AccessibilityNodeInfo info) {
        int invertedIndex;
        int row;
        super.onInitializeAccessibilityNodeInfoForItem(view, position, info);
        int count = getCount();
        int columnsCount = getNumColumns();
        int rowsCount = count / columnsCount;
        if (this.mStackFromBottom) {
            invertedIndex = (count - 1) - position;
            row = (rowsCount - 1) - (invertedIndex / columnsCount);
            invertedIndex = (columnsCount - 1) - (invertedIndex % columnsCount);
        } else {
            invertedIndex = position % columnsCount;
            row = position / columnsCount;
        }
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        boolean z = lp != null && lp.viewType == -2;
        info.setCollectionItemInfo(CollectionItemInfo.obtain(row, 1, invertedIndex, 1, z, isItemChecked(position)));
    }

    protected void encodeProperties(ViewHierarchyEncoder encoder) {
        super.encodeProperties(encoder);
        encoder.addProperty("numColumns", getNumColumns());
    }
}
