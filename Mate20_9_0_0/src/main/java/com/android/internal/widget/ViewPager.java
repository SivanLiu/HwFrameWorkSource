package com.android.internal.widget;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.ClassLoaderCreator;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.AbsSavedState;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.Scroller;
import com.android.internal.telephony.gsm.SmsCbConstants;
import com.android.internal.util.Protocol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ViewPager extends ViewGroup {
    private static final int CLOSE_ENOUGH = 2;
    private static final Comparator<ItemInfo> COMPARATOR = new Comparator<ItemInfo>() {
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            return lhs.position - rhs.position;
        }
    };
    private static final boolean DEBUG = false;
    private static final int DEFAULT_GUTTER_SIZE = 16;
    private static final int DEFAULT_OFFSCREEN_PAGES = 1;
    private static final int DRAW_ORDER_DEFAULT = 0;
    private static final int DRAW_ORDER_FORWARD = 1;
    private static final int DRAW_ORDER_REVERSE = 2;
    private static final int INVALID_POINTER = -1;
    private static final int[] LAYOUT_ATTRS = new int[]{16842931};
    private static final int MAX_SCROLL_X = 16777216;
    private static final int MAX_SETTLE_DURATION = 600;
    private static final int MIN_DISTANCE_FOR_FLING = 25;
    private static final int MIN_FLING_VELOCITY = 400;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_SETTLING = 2;
    private static final String TAG = "ViewPager";
    private static final boolean USE_CACHE = false;
    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return ((((t * t) * t) * t) * t) + 1.0f;
        }
    };
    private static final ViewPositionComparator sPositionComparator = new ViewPositionComparator();
    private int mActivePointerId;
    private PagerAdapter mAdapter;
    private OnAdapterChangeListener mAdapterChangeListener;
    private int mBottomPageBounds;
    private boolean mCalledSuper;
    private int mChildHeightMeasureSpec;
    private int mChildWidthMeasureSpec;
    private final int mCloseEnough;
    private int mCurItem;
    private int mDecorChildCount;
    private final int mDefaultGutterSize;
    private int mDrawingOrder;
    private ArrayList<View> mDrawingOrderedChildren;
    private final Runnable mEndScrollRunnable;
    private int mExpectedAdapterCount;
    private boolean mFirstLayout;
    private float mFirstOffset;
    private final int mFlingDistance;
    private int mGutterSize;
    private boolean mInLayout;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private OnPageChangeListener mInternalPageChangeListener;
    private boolean mIsBeingDragged;
    private boolean mIsUnableToDrag;
    private final ArrayList<ItemInfo> mItems;
    private float mLastMotionX;
    private float mLastMotionY;
    private float mLastOffset;
    private final EdgeEffect mLeftEdge;
    private int mLeftIncr;
    private Drawable mMarginDrawable;
    private final int mMaximumVelocity;
    private final int mMinimumVelocity;
    private PagerObserver mObserver;
    private int mOffscreenPageLimit;
    private OnPageChangeListener mOnPageChangeListener;
    private int mPageMargin;
    private PageTransformer mPageTransformer;
    private boolean mPopulatePending;
    private Parcelable mRestoredAdapterState;
    private ClassLoader mRestoredClassLoader;
    private int mRestoredCurItem;
    private final EdgeEffect mRightEdge;
    private int mScrollState;
    private final Scroller mScroller;
    private boolean mScrollingCacheEnabled;
    private final ItemInfo mTempItem;
    private final Rect mTempRect;
    private int mTopPageBounds;
    private final int mTouchSlop;
    private VelocityTracker mVelocityTracker;

    interface Decor {
    }

    static class ItemInfo {
        Object object;
        float offset;
        int position;
        boolean scrolling;
        float widthFactor;

        ItemInfo() {
        }
    }

    interface OnAdapterChangeListener {
        void onAdapterChanged(PagerAdapter pagerAdapter, PagerAdapter pagerAdapter2);
    }

    public interface OnPageChangeListener {
        void onPageScrollStateChanged(int i);

        void onPageScrolled(int i, float f, int i2);

        void onPageSelected(int i);
    }

    public interface PageTransformer {
        void transformPage(View view, float f);
    }

    static class ViewPositionComparator implements Comparator<View> {
        ViewPositionComparator() {
        }

        public int compare(View lhs, View rhs) {
            LayoutParams llp = (LayoutParams) lhs.getLayoutParams();
            LayoutParams rlp = (LayoutParams) rhs.getLayoutParams();
            if (llp.isDecor == rlp.isDecor) {
                return llp.position - rlp.position;
            }
            return llp.isDecor ? 1 : -1;
        }
    }

    public static class LayoutParams extends android.view.ViewGroup.LayoutParams {
        int childIndex;
        public int gravity;
        public boolean isDecor;
        boolean needsMeasure;
        int position;
        float widthFactor = 0.0f;

        public LayoutParams() {
            super(-1, -1);
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, ViewPager.LAYOUT_ATTRS);
            this.gravity = a.getInteger(0, 48);
            a.recycle();
        }
    }

    private class PagerObserver extends DataSetObserver {
        private PagerObserver() {
        }

        /* synthetic */ PagerObserver(ViewPager x0, AnonymousClass1 x1) {
            this();
        }

        public void onChanged() {
            ViewPager.this.dataSetChanged();
        }

        public void onInvalidated() {
            ViewPager.this.dataSetChanged();
        }
    }

    public static class SimpleOnPageChangeListener implements OnPageChangeListener {
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
        }

        public void onPageScrollStateChanged(int state) {
        }
    }

    public static class SavedState extends AbsSavedState {
        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        Parcelable adapterState;
        ClassLoader loader;
        int position;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.position);
            out.writeParcelable(this.adapterState, flags);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FragmentPager.SavedState{");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
            stringBuilder.append(" position=");
            stringBuilder.append(this.position);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            if (loader == null) {
                loader = getClass().getClassLoader();
            }
            this.position = in.readInt();
            this.adapterState = in.readParcelable(loader);
            this.loader = loader;
        }
    }

    public ViewPager(Context context) {
        this(context, null);
    }

    public ViewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ViewPager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mItems = new ArrayList();
        this.mTempItem = new ItemInfo();
        this.mTempRect = new Rect();
        this.mRestoredCurItem = -1;
        this.mRestoredAdapterState = null;
        this.mRestoredClassLoader = null;
        this.mLeftIncr = -1;
        this.mFirstOffset = -3.4028235E38f;
        this.mLastOffset = Float.MAX_VALUE;
        this.mOffscreenPageLimit = 1;
        this.mActivePointerId = -1;
        this.mFirstLayout = true;
        this.mEndScrollRunnable = new Runnable() {
            public void run() {
                ViewPager.this.setScrollState(0);
                ViewPager.this.populate();
            }
        };
        this.mScrollState = 0;
        setWillNotDraw(false);
        setDescendantFocusability(262144);
        setFocusable(true);
        this.mScroller = new Scroller(context, sInterpolator);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        float density = context.getResources().getDisplayMetrics().density;
        this.mTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mMinimumVelocity = (int) (400.0f * density);
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mLeftEdge = new EdgeEffect(context);
        this.mRightEdge = new EdgeEffect(context);
        this.mFlingDistance = (int) (25.0f * density);
        this.mCloseEnough = (int) (2.0f * density);
        this.mDefaultGutterSize = (int) (16.0f * density);
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
    }

    protected void onDetachedFromWindow() {
        removeCallbacks(this.mEndScrollRunnable);
        super.onDetachedFromWindow();
    }

    private void setScrollState(int newState) {
        if (this.mScrollState != newState) {
            this.mScrollState = newState;
            if (this.mPageTransformer != null) {
                enableLayers(newState != 0);
            }
            if (this.mOnPageChangeListener != null) {
                this.mOnPageChangeListener.onPageScrollStateChanged(newState);
            }
        }
    }

    public void setAdapter(PagerAdapter adapter) {
        if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(this.mObserver);
            this.mAdapter.startUpdate((ViewGroup) this);
            for (int i = 0; i < this.mItems.size(); i++) {
                ItemInfo ii = (ItemInfo) this.mItems.get(i);
                this.mAdapter.destroyItem((ViewGroup) this, ii.position, ii.object);
            }
            this.mAdapter.finishUpdate((ViewGroup) this);
            this.mItems.clear();
            removeNonDecorViews();
            this.mCurItem = 0;
            scrollTo(0, 0);
        }
        PagerAdapter oldAdapter = this.mAdapter;
        this.mAdapter = adapter;
        this.mExpectedAdapterCount = 0;
        if (this.mAdapter != null) {
            if (this.mObserver == null) {
                this.mObserver = new PagerObserver(this, null);
            }
            this.mAdapter.registerDataSetObserver(this.mObserver);
            this.mPopulatePending = false;
            boolean wasFirstLayout = this.mFirstLayout;
            this.mFirstLayout = true;
            this.mExpectedAdapterCount = this.mAdapter.getCount();
            if (this.mRestoredCurItem >= 0) {
                this.mAdapter.restoreState(this.mRestoredAdapterState, this.mRestoredClassLoader);
                setCurrentItemInternal(this.mRestoredCurItem, false, true);
                this.mRestoredCurItem = -1;
                this.mRestoredAdapterState = null;
                this.mRestoredClassLoader = null;
            } else if (wasFirstLayout) {
                requestLayout();
            } else {
                populate();
            }
        }
        if (this.mAdapterChangeListener != null && oldAdapter != adapter) {
            this.mAdapterChangeListener.onAdapterChanged(oldAdapter, adapter);
        }
    }

    private void removeNonDecorViews() {
        int i = 0;
        while (i < getChildCount()) {
            if (!((LayoutParams) getChildAt(i).getLayoutParams()).isDecor) {
                removeViewAt(i);
                i--;
            }
            i++;
        }
    }

    public PagerAdapter getAdapter() {
        return this.mAdapter;
    }

    void setOnAdapterChangeListener(OnAdapterChangeListener listener) {
        this.mAdapterChangeListener = listener;
    }

    private int getPaddedWidth() {
        return (getMeasuredWidth() - getPaddingLeft()) - getPaddingRight();
    }

    public void setCurrentItem(int item) {
        this.mPopulatePending = false;
        setCurrentItemInternal(item, this.mFirstLayout ^ 1, false);
    }

    public void setCurrentItem(int item, boolean smoothScroll) {
        this.mPopulatePending = false;
        setCurrentItemInternal(item, smoothScroll, false);
    }

    public int getCurrentItem() {
        return this.mCurItem;
    }

    boolean setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
        return setCurrentItemInternal(item, smoothScroll, always, 0);
    }

    boolean setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
        boolean dispatchSelected = false;
        if (this.mAdapter == null || this.mAdapter.getCount() <= 0) {
            setScrollingCacheEnabled(false);
            return false;
        }
        item = MathUtils.constrain(item, 0, this.mAdapter.getCount() - 1);
        if (always || this.mCurItem != item || this.mItems.size() == 0) {
            int pageLimit = this.mOffscreenPageLimit;
            if (item > this.mCurItem + pageLimit || item < this.mCurItem - pageLimit) {
                for (int i = 0; i < this.mItems.size(); i++) {
                    ((ItemInfo) this.mItems.get(i)).scrolling = true;
                }
            }
            if (this.mCurItem != item) {
                dispatchSelected = true;
            }
            if (this.mFirstLayout) {
                this.mCurItem = item;
                if (dispatchSelected && this.mOnPageChangeListener != null) {
                    this.mOnPageChangeListener.onPageSelected(item);
                }
                if (dispatchSelected && this.mInternalPageChangeListener != null) {
                    this.mInternalPageChangeListener.onPageSelected(item);
                }
                requestLayout();
            } else {
                populate(item);
                scrollToItem(item, smoothScroll, velocity, dispatchSelected);
            }
            return true;
        }
        setScrollingCacheEnabled(false);
        return false;
    }

    private void scrollToItem(int position, boolean smoothScroll, int velocity, boolean dispatchSelected) {
        int destX = getLeftEdgeForItem(position);
        if (smoothScroll) {
            smoothScrollTo(destX, 0, velocity);
            if (dispatchSelected && this.mOnPageChangeListener != null) {
                this.mOnPageChangeListener.onPageSelected(position);
            }
            if (dispatchSelected && this.mInternalPageChangeListener != null) {
                this.mInternalPageChangeListener.onPageSelected(position);
                return;
            }
            return;
        }
        if (dispatchSelected && this.mOnPageChangeListener != null) {
            this.mOnPageChangeListener.onPageSelected(position);
        }
        if (dispatchSelected && this.mInternalPageChangeListener != null) {
            this.mInternalPageChangeListener.onPageSelected(position);
        }
        completeScroll(false);
        scrollTo(destX, 0);
        pageScrolled(destX);
    }

    private int getLeftEdgeForItem(int position) {
        ItemInfo info = infoForPosition(position);
        if (info == null) {
            return 0;
        }
        int width = getPaddedWidth();
        int scaledOffset = (int) (((float) width) * MathUtils.constrain(info.offset, this.mFirstOffset, this.mLastOffset));
        if (isLayoutRtl()) {
            return (16777216 - ((int) ((((float) width) * info.widthFactor) + 1056964608))) - scaledOffset;
        }
        return scaledOffset;
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.mOnPageChangeListener = listener;
    }

    public void setPageTransformer(boolean reverseDrawingOrder, PageTransformer transformer) {
        int i = 1;
        boolean hasTransformer = transformer != null;
        boolean needsPopulate = hasTransformer != (this.mPageTransformer != null);
        this.mPageTransformer = transformer;
        setChildrenDrawingOrderEnabled(hasTransformer);
        if (hasTransformer) {
            if (reverseDrawingOrder) {
                i = 2;
            }
            this.mDrawingOrder = i;
        } else {
            this.mDrawingOrder = 0;
        }
        if (needsPopulate) {
            populate();
        }
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        return ((LayoutParams) ((View) this.mDrawingOrderedChildren.get(this.mDrawingOrder == 2 ? (childCount - 1) - i : i)).getLayoutParams()).childIndex;
    }

    OnPageChangeListener setInternalPageChangeListener(OnPageChangeListener listener) {
        OnPageChangeListener oldListener = this.mInternalPageChangeListener;
        this.mInternalPageChangeListener = listener;
        return oldListener;
    }

    public int getOffscreenPageLimit() {
        return this.mOffscreenPageLimit;
    }

    public void setOffscreenPageLimit(int limit) {
        if (limit < 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Requested offscreen page limit ");
            stringBuilder.append(limit);
            stringBuilder.append(" too small; defaulting to ");
            stringBuilder.append(1);
            Log.w(str, stringBuilder.toString());
            limit = 1;
        }
        if (limit != this.mOffscreenPageLimit) {
            this.mOffscreenPageLimit = limit;
            populate();
        }
    }

    public void setPageMargin(int marginPixels) {
        int oldMargin = this.mPageMargin;
        this.mPageMargin = marginPixels;
        int width = getWidth();
        recomputeScrollPosition(width, width, marginPixels, oldMargin);
        requestLayout();
    }

    public int getPageMargin() {
        return this.mPageMargin;
    }

    public void setPageMarginDrawable(Drawable d) {
        this.mMarginDrawable = d;
        if (d != null) {
            refreshDrawableState();
        }
        setWillNotDraw(d == null);
        invalidate();
    }

    public void setPageMarginDrawable(int resId) {
        setPageMarginDrawable(getContext().getDrawable(resId));
    }

    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mMarginDrawable;
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable marginDrawable = this.mMarginDrawable;
        if (marginDrawable != null && marginDrawable.isStateful() && marginDrawable.setState(getDrawableState())) {
            invalidateDrawable(marginDrawable);
        }
    }

    float distanceInfluenceForSnapDuration(float f) {
        return (float) Math.sin((double) ((float) (((double) (f - 0.5f)) * 0.4712389167638204d)));
    }

    void smoothScrollTo(int x, int y) {
        smoothScrollTo(x, y, 0);
    }

    void smoothScrollTo(int x, int y, int velocity) {
        if (getChildCount() == 0) {
            setScrollingCacheEnabled(false);
            return;
        }
        int sx = getScrollX();
        int sy = getScrollY();
        int dx = x - sx;
        int dy = y - sy;
        if (dx == 0 && dy == 0) {
            completeScroll(false);
            populate();
            setScrollState(0);
            return;
        }
        int duration;
        setScrollingCacheEnabled(true);
        setScrollState(2);
        int width = getPaddedWidth();
        int halfWidth = width / 2;
        float distance = ((float) halfWidth) + (((float) halfWidth) * distanceInfluenceForSnapDuration(Math.min(1.0f, (((float) Math.abs(dx)) * 1.0f) / ((float) width))));
        int velocity2 = Math.abs(velocity);
        if (velocity2 > 0) {
            duration = 4 * Math.round(1000.0f * Math.abs(distance / ((float) velocity2)));
        } else {
            duration = (int) ((1.0f + (((float) Math.abs(dx)) / (((float) this.mPageMargin) + (((float) width) * this.mAdapter.getPageWidth(this.mCurItem))))) * 100.0f);
        }
        this.mScroller.startScroll(sx, sy, dx, dy, Math.min(duration, 600));
        postInvalidateOnAnimation();
    }

    ItemInfo addNewItem(int position, int index) {
        ItemInfo ii = new ItemInfo();
        ii.position = position;
        ii.object = this.mAdapter.instantiateItem((ViewGroup) this, position);
        ii.widthFactor = this.mAdapter.getPageWidth(position);
        if (index < 0 || index >= this.mItems.size()) {
            this.mItems.add(ii);
        } else {
            this.mItems.add(index, ii);
        }
        return ii;
    }

    void dataSetChanged() {
        int adapterCount = this.mAdapter.getCount();
        this.mExpectedAdapterCount = adapterCount;
        boolean needPopulate = this.mItems.size() < (this.mOffscreenPageLimit * 2) + 1 && this.mItems.size() < adapterCount;
        boolean isUpdating = false;
        int newCurrItem = this.mCurItem;
        boolean needPopulate2 = needPopulate;
        int i = 0;
        while (i < this.mItems.size()) {
            ItemInfo ii = (ItemInfo) this.mItems.get(i);
            int newPos = this.mAdapter.getItemPosition(ii.object);
            if (newPos != -1) {
                if (newPos == -2) {
                    this.mItems.remove(i);
                    i--;
                    if (!isUpdating) {
                        this.mAdapter.startUpdate((ViewGroup) this);
                        isUpdating = true;
                    }
                    this.mAdapter.destroyItem((ViewGroup) this, ii.position, ii.object);
                    needPopulate2 = true;
                    if (this.mCurItem == ii.position) {
                        newCurrItem = Math.max(0, Math.min(this.mCurItem, adapterCount - 1));
                        needPopulate2 = true;
                    }
                } else if (ii.position != newPos) {
                    if (ii.position == this.mCurItem) {
                        newCurrItem = newPos;
                    }
                    ii.position = newPos;
                    needPopulate2 = true;
                }
            }
            i++;
        }
        if (isUpdating) {
            this.mAdapter.finishUpdate((ViewGroup) this);
        }
        Collections.sort(this.mItems, COMPARATOR);
        if (needPopulate2) {
            i = getChildCount();
            for (int i2 = 0; i2 < i; i2++) {
                LayoutParams lp = (LayoutParams) getChildAt(i2).getLayoutParams();
                if (!lp.isDecor) {
                    lp.widthFactor = 0.0f;
                }
            }
            setCurrentItemInternal(newCurrItem, false, true);
            requestLayout();
        }
    }

    public void populate() {
        populate(this.mCurItem);
    }

    /* JADX WARNING: Removed duplicated region for block: B:117:0x01e9  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0087  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x01f8  */
    /* JADX WARNING: Removed duplicated region for block: B:120:0x01f5  */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x0208  */
    /* JADX WARNING: Removed duplicated region for block: B:135:0x023b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void populate(int newCurrentItem) {
        int i = newCurrentItem;
        ItemInfo oldCurInfo = null;
        ItemInfo oldCurInfo2 = 2;
        if (this.mCurItem != i) {
            oldCurInfo2 = this.mCurItem < i ? 66 : 17;
            oldCurInfo = infoForPosition(this.mCurItem);
            this.mCurItem = i;
        }
        int focusDirection = oldCurInfo2;
        oldCurInfo2 = oldCurInfo;
        if (this.mAdapter == null) {
            sortChildDrawingOrder();
        } else if (this.mPopulatePending) {
            sortChildDrawingOrder();
        } else if (getWindowToken() != null) {
            this.mAdapter.startUpdate(this);
            int pageLimit = this.mOffscreenPageLimit;
            int startPos = Math.max(0, this.mCurItem - pageLimit);
            int N = this.mAdapter.getCount();
            int endPos = Math.min(N - 1, this.mCurItem + pageLimit);
            if (N == this.mExpectedAdapterCount) {
                int curIndex;
                ItemInfo curItem = null;
                for (curIndex = 0; curIndex < this.mItems.size(); curIndex++) {
                    ItemInfo ii = (ItemInfo) this.mItems.get(curIndex);
                    if (ii.position >= this.mCurItem) {
                        int clientWidth;
                        if (ii.position == this.mCurItem) {
                            curItem = ii;
                        }
                        if (curItem == null && N > 0) {
                            curItem = addNewItem(this.mCurItem, curIndex);
                        }
                        int curIndex2;
                        int pageLimit2;
                        if (curItem == null) {
                            int curIndex3;
                            int i2;
                            int leftWidthNeeded;
                            float extraWidthLeft = 0.0f;
                            int itemIndex = curIndex - 1;
                            ItemInfo ii2 = itemIndex >= 0 ? (ItemInfo) this.mItems.get(itemIndex) : null;
                            clientWidth = getPaddedWidth();
                            if (clientWidth <= 0) {
                                curIndex3 = curIndex;
                                i2 = 0;
                            } else {
                                curIndex3 = curIndex;
                                i2 = (((float) getPaddingLeft()) / ((float) clientWidth)) + (2.0f - curItem.widthFactor);
                            }
                            curIndex = i2;
                            i2 = this.mCurItem - 1;
                            curIndex2 = curIndex3;
                            while (i2 >= 0) {
                                if (extraWidthLeft < curIndex || i2 >= startPos) {
                                    leftWidthNeeded = curIndex;
                                    if (ii2 == null || i2 != ii2.position) {
                                        extraWidthLeft += addNewItem(i2, itemIndex + 1).widthFactor;
                                        curIndex2++;
                                        curIndex = itemIndex >= 0 ? (ItemInfo) this.mItems.get(itemIndex) : null;
                                    } else {
                                        extraWidthLeft += ii2.widthFactor;
                                        itemIndex--;
                                        curIndex = itemIndex >= 0 ? (ItemInfo) this.mItems.get(itemIndex) : 0;
                                    }
                                } else if (ii2 == null) {
                                    leftWidthNeeded = curIndex;
                                    break;
                                } else {
                                    leftWidthNeeded = curIndex;
                                    if (i2 == ii2.position && ii2.scrolling == 0) {
                                        this.mItems.remove(itemIndex);
                                        this.mAdapter.destroyItem(this, i2, ii2.object);
                                        itemIndex--;
                                        curIndex2--;
                                        curIndex = itemIndex >= 0 ? (ItemInfo) this.mItems.get(itemIndex) : 0;
                                    }
                                    i2--;
                                    curIndex = leftWidthNeeded;
                                    i = newCurrentItem;
                                }
                                ii2 = curIndex;
                                i2--;
                                curIndex = leftWidthNeeded;
                                i = newCurrentItem;
                            }
                            leftWidthNeeded = curIndex;
                            curIndex = curItem.widthFactor;
                            i = curIndex2 + 1;
                            int clientWidth2;
                            if (curIndex < 1073741824) {
                                ii = i < this.mItems.size() ? (ItemInfo) this.mItems.get(i) : null;
                                float rightWidthNeeded = clientWidth <= 0 ? 0.0f : (((float) getPaddingRight()) / ((float) clientWidth)) + 2.0f;
                                int pos = this.mCurItem + 1;
                                while (pos < N) {
                                    if (curIndex < rightWidthNeeded || pos <= endPos) {
                                        pageLimit2 = pageLimit;
                                        clientWidth2 = clientWidth;
                                        if (ii == null || pos != ii.position) {
                                            pageLimit = addNewItem(pos, i);
                                            i++;
                                            curIndex += pageLimit.widthFactor;
                                            ii = i < this.mItems.size() ? (ItemInfo) this.mItems.get(i) : null;
                                        } else {
                                            curIndex += ii.widthFactor;
                                            i++;
                                            ii = i < this.mItems.size() ? (ItemInfo) this.mItems.get(i) : 0;
                                        }
                                    } else if (ii == null) {
                                        pageLimit2 = pageLimit;
                                        clientWidth2 = clientWidth;
                                        break;
                                    } else {
                                        pageLimit2 = pageLimit;
                                        if (pos == ii.position && ii.scrolling == 0) {
                                            this.mItems.remove(i);
                                            clientWidth2 = clientWidth;
                                            this.mAdapter.destroyItem(this, pos, ii.object);
                                            ii = i < this.mItems.size() ? (ItemInfo) this.mItems.get(i) : 0;
                                        } else {
                                            clientWidth2 = clientWidth;
                                        }
                                    }
                                    pos++;
                                    pageLimit = pageLimit2;
                                    clientWidth = clientWidth2;
                                }
                                clientWidth2 = clientWidth;
                            } else {
                                clientWidth2 = clientWidth;
                            }
                            calculatePageOffsets(curItem, curIndex2, oldCurInfo2);
                        } else {
                            pageLimit2 = pageLimit;
                            curIndex2 = curIndex;
                        }
                        this.mAdapter.setPrimaryItem(this, this.mCurItem, curItem == null ? curItem.object : 0);
                        this.mAdapter.finishUpdate(this);
                        curIndex = getChildCount();
                        for (i = 0; i < curIndex; i++) {
                            pageLimit = getChildAt(i);
                            LayoutParams lp = (LayoutParams) pageLimit.getLayoutParams();
                            lp.childIndex = i;
                            if (!lp.isDecor) {
                                if (lp.widthFactor == 0.0f) {
                                    ii = infoForChild(pageLimit);
                                    if (ii != null) {
                                        lp.widthFactor = ii.widthFactor;
                                        lp.position = ii.position;
                                    }
                                }
                            }
                        }
                        sortChildDrawingOrder();
                        if (hasFocus()) {
                            View currentFocused = findFocus();
                            pageLimit = currentFocused != null ? infoForAnyChild(currentFocused) : null;
                            if (pageLimit == 0 || pageLimit.position != this.mCurItem) {
                                int i3 = 0;
                                while (true) {
                                    clientWidth = i3;
                                    if (clientWidth >= getChildCount()) {
                                        break;
                                    }
                                    View child = getChildAt(clientWidth);
                                    pageLimit = infoForChild(child);
                                    if (pageLimit != 0 && pageLimit.position == this.mCurItem) {
                                        Rect focusRect;
                                        if (currentFocused == null) {
                                            focusRect = null;
                                        } else {
                                            focusRect = this.mTempRect;
                                            currentFocused.getFocusedRect(this.mTempRect);
                                            offsetDescendantRectToMyCoords(currentFocused, this.mTempRect);
                                            offsetRectIntoDescendantCoords(child, this.mTempRect);
                                        }
                                        if (child.requestFocus(focusDirection, focusRect)) {
                                            break;
                                        }
                                    }
                                    i3 = clientWidth + 1;
                                }
                            }
                        }
                        return;
                    }
                }
                curItem = addNewItem(this.mCurItem, curIndex);
                if (curItem == null) {
                }
                if (curItem == null) {
                }
                this.mAdapter.setPrimaryItem(this, this.mCurItem, curItem == null ? curItem.object : 0);
                this.mAdapter.finishUpdate(this);
                curIndex = getChildCount();
                while (i < curIndex) {
                }
                sortChildDrawingOrder();
                if (hasFocus()) {
                }
                return;
            }
            String resName;
            try {
                resName = getResources().getResourceName(getId());
            } catch (NotFoundException e) {
                resName = Integer.toHexString(getId());
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("The application's PagerAdapter changed the adapter's contents without calling PagerAdapter#notifyDataSetChanged! Expected adapter item count: ");
            stringBuilder.append(this.mExpectedAdapterCount);
            stringBuilder.append(", found: ");
            stringBuilder.append(N);
            stringBuilder.append(" Pager id: ");
            stringBuilder.append(resName);
            stringBuilder.append(" Pager class: ");
            stringBuilder.append(getClass());
            stringBuilder.append(" Problematic adapter: ");
            stringBuilder.append(this.mAdapter.getClass());
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private void sortChildDrawingOrder() {
        if (this.mDrawingOrder != 0) {
            if (this.mDrawingOrderedChildren == null) {
                this.mDrawingOrderedChildren = new ArrayList();
            } else {
                this.mDrawingOrderedChildren.clear();
            }
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                this.mDrawingOrderedChildren.add(getChildAt(i));
            }
            Collections.sort(this.mDrawingOrderedChildren, sPositionComparator);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x0056 A:{LOOP_END, LOOP:2: B:18:0x0052->B:20:0x0056} */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x009f A:{LOOP_END, LOOP:5: B:33:0x009b->B:35:0x009f} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void calculatePageOffsets(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
        int oldCurPosition;
        int itemIndex;
        int pos;
        ItemInfo ii;
        int N = this.mAdapter.getCount();
        int width = getPaddedWidth();
        float marginOffset = width > 0 ? ((float) this.mPageMargin) / ((float) width) : 0.0f;
        if (oldCurInfo != null) {
            oldCurPosition = oldCurInfo.position;
            float offset;
            if (oldCurPosition < curItem.position) {
                itemIndex = 0;
                offset = (oldCurInfo.offset + oldCurInfo.widthFactor) + marginOffset;
                pos = oldCurPosition + 1;
                while (pos <= curItem.position && itemIndex < this.mItems.size()) {
                    ii = this.mItems.get(itemIndex);
                    while (true) {
                        ii = ii;
                        if (pos <= ii.position || itemIndex >= this.mItems.size() - 1) {
                            while (pos < ii.position) {
                                offset += this.mAdapter.getPageWidth(pos) + marginOffset;
                                pos++;
                            }
                        } else {
                            itemIndex++;
                            ii = this.mItems.get(itemIndex);
                        }
                    }
                    while (pos < ii.position) {
                    }
                    ii.offset = offset;
                    offset += ii.widthFactor + marginOffset;
                    pos++;
                }
            } else if (oldCurPosition > curItem.position) {
                itemIndex = this.mItems.size() - 1;
                offset = oldCurInfo.offset;
                pos = oldCurPosition - 1;
                while (pos >= curItem.position && itemIndex >= 0) {
                    Object ii2 = this.mItems.get(itemIndex);
                    while (true) {
                        ii = (ItemInfo) ii2;
                        if (pos >= ii.position || itemIndex <= 0) {
                            while (pos > ii.position) {
                                offset -= this.mAdapter.getPageWidth(pos) + marginOffset;
                                pos--;
                            }
                        } else {
                            itemIndex--;
                            ii2 = this.mItems.get(itemIndex);
                        }
                    }
                    while (pos > ii.position) {
                    }
                    offset -= ii.widthFactor + marginOffset;
                    ii.offset = offset;
                    pos--;
                }
            }
        }
        oldCurPosition = this.mItems.size();
        float offset2 = curItem.offset;
        int pos2 = curItem.position - 1;
        this.mFirstOffset = curItem.position == 0 ? curItem.offset : -3.4028235E38f;
        this.mLastOffset = curItem.position == N + -1 ? (curItem.offset + curItem.widthFactor) - 1.0f : Float.MAX_VALUE;
        pos = curIndex - 1;
        while (pos >= 0) {
            ii = (ItemInfo) this.mItems.get(pos);
            while (pos2 > ii.position) {
                offset2 -= this.mAdapter.getPageWidth(pos2) + marginOffset;
                pos2--;
            }
            offset2 -= ii.widthFactor + marginOffset;
            ii.offset = offset2;
            if (ii.position == 0) {
                this.mFirstOffset = offset2;
            }
            pos--;
            pos2--;
        }
        float offset3 = (curItem.offset + curItem.widthFactor) + marginOffset;
        itemIndex = curItem.position + 1;
        pos2 = curIndex + 1;
        while (pos2 < oldCurPosition) {
            ii = (ItemInfo) this.mItems.get(pos2);
            while (itemIndex < ii.position) {
                offset3 += this.mAdapter.getPageWidth(itemIndex) + marginOffset;
                itemIndex++;
            }
            if (ii.position == N - 1) {
                this.mLastOffset = (ii.widthFactor + offset3) - 1.0f;
            }
            ii.offset = offset3;
            offset3 += ii.widthFactor + marginOffset;
            pos2++;
            itemIndex++;
        }
    }

    public Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.position = this.mCurItem;
        if (this.mAdapter != null) {
            ss.adapterState = this.mAdapter.saveState();
        }
        return ss;
    }

    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            if (this.mAdapter != null) {
                this.mAdapter.restoreState(ss.adapterState, ss.loader);
                setCurrentItemInternal(ss.position, false, true);
            } else {
                this.mRestoredCurItem = ss.position;
                this.mRestoredAdapterState = ss.adapterState;
                this.mRestoredClassLoader = ss.loader;
            }
            return;
        }
        super.onRestoreInstanceState(state);
    }

    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }
        LayoutParams lp = (LayoutParams) params;
        lp.isDecor |= child instanceof Decor;
        if (!this.mInLayout) {
            super.addView(child, index, params);
        } else if (lp == null || !lp.isDecor) {
            lp.needsMeasure = true;
            addViewInLayout(child, index, params);
        } else {
            throw new IllegalStateException("Cannot add pager decor view during layout");
        }
    }

    public Object getCurrent() {
        ItemInfo itemInfo = infoForPosition(getCurrentItem());
        return itemInfo == null ? null : itemInfo.object;
    }

    public void removeView(View view) {
        if (this.mInLayout) {
            removeViewInLayout(view);
        } else {
            super.removeView(view);
        }
    }

    ItemInfo infoForChild(View child) {
        for (int i = 0; i < this.mItems.size(); i++) {
            ItemInfo ii = (ItemInfo) this.mItems.get(i);
            if (this.mAdapter.isViewFromObject(child, ii.object)) {
                return ii;
            }
        }
        return null;
    }

    ItemInfo infoForAnyChild(View child) {
        while (true) {
            Object parent = child.getParent();
            ViewParent parent2 = parent;
            if (parent == this) {
                return infoForChild(child);
            }
            if (parent2 != null && (parent2 instanceof View)) {
                child = (View) parent2;
            }
        }
        return null;
    }

    ItemInfo infoForPosition(int position) {
        for (int i = 0; i < this.mItems.size(); i++) {
            ItemInfo ii = (ItemInfo) this.mItems.get(i);
            if (ii.position == position) {
                return ii;
            }
        }
        return null;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mFirstLayout = true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:41:0x00cd  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00a5  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00cd  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize;
        int maxGutterSize;
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec), getDefaultSize(0, heightMeasureSpec));
        int measuredWidth = getMeasuredWidth();
        int maxGutterSize2 = measuredWidth / 10;
        this.mGutterSize = Math.min(maxGutterSize2, this.mDefaultGutterSize);
        int childWidthSize = (measuredWidth - getPaddingLeft()) - getPaddingRight();
        int childHeightSize = (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom();
        int size = getChildCount();
        int childHeightSize2 = childHeightSize;
        childHeightSize = childWidthSize;
        childWidthSize = 0;
        while (childWidthSize < size) {
            int measuredWidth2;
            int heightMode;
            View child = getChildAt(childWidthSize);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp != null && lp.isDecor) {
                    int hgrav = lp.gravity & 7;
                    int vgrav = lp.gravity & 112;
                    int widthMode = Integer.MIN_VALUE;
                    int heightMode2 = Integer.MIN_VALUE;
                    boolean consumeVertical = vgrav == 48 || vgrav == 80;
                    boolean z = hgrav == 3 || hgrav == 5;
                    boolean consumeHorizontal = z;
                    if (consumeVertical) {
                        widthMode = 1073741824;
                    } else if (consumeHorizontal) {
                        heightMode2 = 1073741824;
                    }
                    int widthSize2 = childHeightSize;
                    int heightSize = childHeightSize2;
                    measuredWidth2 = measuredWidth;
                    if (lp.width != -2) {
                        widthMode = 1073741824;
                        if (lp.width != -1) {
                            widthSize = lp.width;
                            if (lp.height != -2) {
                                heightMode2 = 1073741824;
                                if (lp.height != -1) {
                                    measuredWidth = lp.height;
                                    heightMode = 1073741824;
                                    maxGutterSize = maxGutterSize2;
                                    child.measure(MeasureSpec.makeMeasureSpec(widthSize, widthMode), MeasureSpec.makeMeasureSpec(measuredWidth, heightMode));
                                    if (consumeVertical) {
                                        childHeightSize2 -= child.getMeasuredHeight();
                                    } else if (consumeHorizontal) {
                                        childHeightSize -= child.getMeasuredWidth();
                                    }
                                    childWidthSize++;
                                    measuredWidth = measuredWidth2;
                                    maxGutterSize2 = maxGutterSize;
                                    widthSize = widthMeasureSpec;
                                    heightMode = heightMeasureSpec;
                                }
                            }
                            heightMode = heightMode2;
                            measuredWidth = heightSize;
                            maxGutterSize = maxGutterSize2;
                            child.measure(MeasureSpec.makeMeasureSpec(widthSize, widthMode), MeasureSpec.makeMeasureSpec(measuredWidth, heightMode));
                            if (consumeVertical) {
                            }
                            childWidthSize++;
                            measuredWidth = measuredWidth2;
                            maxGutterSize2 = maxGutterSize;
                            widthSize = widthMeasureSpec;
                            heightMode = heightMeasureSpec;
                        }
                    }
                    widthSize = widthSize2;
                    if (lp.height != -2) {
                    }
                    heightMode = heightMode2;
                    measuredWidth = heightSize;
                    maxGutterSize = maxGutterSize2;
                    child.measure(MeasureSpec.makeMeasureSpec(widthSize, widthMode), MeasureSpec.makeMeasureSpec(measuredWidth, heightMode));
                    if (consumeVertical) {
                    }
                    childWidthSize++;
                    measuredWidth = measuredWidth2;
                    maxGutterSize2 = maxGutterSize;
                    widthSize = widthMeasureSpec;
                    heightMode = heightMeasureSpec;
                }
            }
            measuredWidth2 = measuredWidth;
            maxGutterSize = maxGutterSize2;
            childWidthSize++;
            measuredWidth = measuredWidth2;
            maxGutterSize2 = maxGutterSize;
            widthSize = widthMeasureSpec;
            heightMode = heightMeasureSpec;
        }
        maxGutterSize = maxGutterSize2;
        this.mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, 1073741824);
        this.mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize2, 1073741824);
        this.mInLayout = true;
        populate();
        widthSize = 0;
        this.mInLayout = false;
        measuredWidth = getChildCount();
        while (widthSize < measuredWidth) {
            View child2 = getChildAt(widthSize);
            if (child2.getVisibility() != 8) {
                LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                if (lp2 == null || !lp2.isDecor) {
                    child2.measure(MeasureSpec.makeMeasureSpec((int) (((float) childHeightSize) * lp2.widthFactor), 1073741824), this.mChildHeightMeasureSpec);
                }
            }
            widthSize++;
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw) {
            recomputeScrollPosition(w, oldw, this.mPageMargin, this.mPageMargin);
        }
    }

    private void recomputeScrollPosition(int width, int oldWidth, int margin, int oldMargin) {
        int i = width;
        if (oldWidth <= 0 || this.mItems.isEmpty()) {
            ItemInfo ii = infoForPosition(this.mCurItem);
            int scrollPos = (int) (((float) ((i - getPaddingLeft()) - getPaddingRight())) * (ii != null ? Math.min(ii.offset, this.mLastOffset) : 0.0f));
            if (scrollPos != getScrollX()) {
                completeScroll(false);
                scrollTo(scrollPos, getScrollY());
                return;
            }
            return;
        }
        int newOffsetPixels = (int) (((float) (((i - getPaddingLeft()) - getPaddingRight()) + margin)) * (((float) getScrollX()) / ((float) (((oldWidth - getPaddingLeft()) - getPaddingRight()) + oldMargin))));
        scrollTo(newOffsetPixels, getScrollY());
        if (!this.mScroller.isFinished()) {
            int newDuration = this.mScroller.getDuration() - this.mScroller.timePassed();
            ItemInfo targetInfo = infoForPosition(this.mCurItem);
            this.mScroller.startScroll(newOffsetPixels, 0, (int) (targetInfo.offset * ((float) i)), 0, newDuration);
        }
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int hgrav;
        int childLeft;
        int width;
        int childWidth;
        boolean z;
        int count = getChildCount();
        int width2 = r - l;
        int height = b - t;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int scrollX = getScrollX();
        int decorCount = 0;
        int paddingBottom2 = paddingBottom;
        paddingBottom = paddingTop;
        paddingTop = paddingLeft;
        for (paddingLeft = 0; paddingLeft < count; paddingLeft++) {
            View child = getChildAt(paddingLeft);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int childLeft2 = 0;
                if (lp.isDecor != 0) {
                    hgrav = lp.gravity & 7;
                    int vgrav = lp.gravity & 112;
                    if (hgrav == 1) {
                        childLeft = Math.max((width2 - child.getMeasuredWidth()) / 2, paddingTop);
                    } else if (hgrav == 3) {
                        childLeft = paddingTop;
                        paddingTop += child.getMeasuredWidth();
                    } else if (hgrav != 5) {
                        childLeft = paddingTop;
                    } else {
                        childLeft = (width2 - paddingRight) - child.getMeasuredWidth();
                        paddingRight += child.getMeasuredWidth();
                    }
                    if (vgrav == 16) {
                        hgrav = Math.max((height - child.getMeasuredHeight()) / 2, paddingBottom);
                    } else if (vgrav == 48) {
                        hgrav = paddingBottom;
                        paddingBottom += child.getMeasuredHeight();
                    } else if (vgrav != 80) {
                        hgrav = paddingBottom;
                    } else {
                        hgrav = (height - paddingBottom2) - child.getMeasuredHeight();
                        paddingBottom2 += child.getMeasuredHeight();
                    }
                    childLeft += scrollX;
                    child.layout(childLeft, hgrav, childLeft + child.getMeasuredWidth(), hgrav + child.getMeasuredHeight());
                    decorCount++;
                }
            }
        }
        childLeft = (width2 - paddingTop) - paddingRight;
        hgrav = 0;
        while (hgrav < count) {
            int count2;
            View child2 = getChildAt(hgrav);
            if (child2.getVisibility() != 8) {
                LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                if (!lp2.isDecor) {
                    ItemInfo ii = infoForChild(child2);
                    if (ii != null) {
                        if (lp2.needsMeasure) {
                            lp2.needsMeasure = false;
                            count2 = count;
                            width = width2;
                            child2.measure(MeasureSpec.makeMeasureSpec((int) (((float) childLeft) * lp2.widthFactor), 1073741824), MeasureSpec.makeMeasureSpec((height - paddingBottom) - paddingBottom2, 1073741824));
                        } else {
                            count2 = count;
                            width = width2;
                        }
                        count = child2.getMeasuredWidth();
                        width2 = (int) (((float) childLeft) * ii.offset);
                        if (isLayoutRtl()) {
                            paddingLeft = ((16777216 - paddingRight) - width2) - count;
                        } else {
                            paddingLeft = paddingTop + width2;
                        }
                        childWidth = childLeft;
                        childLeft = paddingLeft + count;
                        int childMeasuredWidth = count;
                        count = paddingBottom;
                        child2.layout(paddingLeft, count, childLeft, count + child2.getMeasuredHeight());
                        hgrav++;
                        count = count2;
                        width2 = width;
                        childLeft = childWidth;
                    }
                }
            }
            count2 = count;
            childWidth = childLeft;
            width = width2;
            hgrav++;
            count = count2;
            width2 = width;
            childLeft = childWidth;
        }
        childWidth = childLeft;
        width = width2;
        this.mTopPageBounds = paddingBottom;
        this.mBottomPageBounds = height - paddingBottom2;
        this.mDecorChildCount = decorCount;
        if (this.mFirstLayout) {
            z = false;
            scrollToItem(this.mCurItem, false, 0, false);
        } else {
            z = false;
        }
        this.mFirstLayout = z;
    }

    public void computeScroll() {
        if (this.mScroller.isFinished() || !this.mScroller.computeScrollOffset()) {
            completeScroll(true);
            return;
        }
        int oldX = getScrollX();
        int oldY = getScrollY();
        int x = this.mScroller.getCurrX();
        int y = this.mScroller.getCurrY();
        if (!(oldX == x && oldY == y)) {
            scrollTo(x, y);
            if (!pageScrolled(x)) {
                this.mScroller.abortAnimation();
                scrollTo(0, y);
            }
        }
        postInvalidateOnAnimation();
    }

    private boolean pageScrolled(int scrollX) {
        if (this.mItems.size() == 0) {
            this.mCalledSuper = false;
            onPageScrolled(0, 0.0f, 0);
            if (this.mCalledSuper) {
                return false;
            }
            throw new IllegalStateException("onPageScrolled did not call superclass implementation");
        }
        int scrollStart;
        if (isLayoutRtl()) {
            scrollStart = 16777216 - scrollX;
        } else {
            scrollStart = scrollX;
        }
        ItemInfo ii = infoForFirstVisiblePage();
        int width = getPaddedWidth();
        int widthWithMargin = this.mPageMargin + width;
        float marginOffset = ((float) this.mPageMargin) / ((float) width);
        int currentPage = ii.position;
        float pageOffset = ((((float) scrollStart) / ((float) width)) - ii.offset) / (ii.widthFactor + marginOffset);
        int offsetPixels = (int) (((float) widthWithMargin) * pageOffset);
        this.mCalledSuper = false;
        onPageScrolled(currentPage, pageOffset, offsetPixels);
        if (this.mCalledSuper) {
            return true;
        }
        throw new IllegalStateException("onPageScrolled did not call superclass implementation");
    }

    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        int scrollX;
        int paddingLeft;
        int childLeft;
        int i = position;
        float f = offset;
        int i2 = offsetPixels;
        if (this.mDecorChildCount > 0) {
            scrollX = getScrollX();
            paddingLeft = getPaddingLeft();
            int paddingRight = getPaddingRight();
            int width = getWidth();
            int childCount = getChildCount();
            int paddingRight2 = paddingRight;
            paddingRight = paddingLeft;
            for (paddingLeft = 0; paddingLeft < childCount; paddingLeft++) {
                View child = getChildAt(paddingLeft);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.isDecor) {
                    int hgrav = lp.gravity & 7;
                    if (hgrav == 1) {
                        childLeft = Math.max((width - child.getMeasuredWidth()) / 2, paddingRight);
                    } else if (hgrav == 3) {
                        childLeft = paddingRight;
                        paddingRight += child.getWidth();
                    } else if (hgrav != 5) {
                        childLeft = paddingRight;
                    } else {
                        childLeft = (width - paddingRight2) - child.getMeasuredWidth();
                        paddingRight2 += child.getMeasuredWidth();
                    }
                    int childOffset = (childLeft + scrollX) - child.getLeft();
                    if (childOffset != 0) {
                        child.offsetLeftAndRight(childOffset);
                    }
                }
            }
        }
        if (this.mOnPageChangeListener != null) {
            this.mOnPageChangeListener.onPageScrolled(i, f, i2);
        }
        if (this.mInternalPageChangeListener != null) {
            this.mInternalPageChangeListener.onPageScrolled(i, f, i2);
        }
        if (this.mPageTransformer != null) {
            scrollX = getScrollX();
            childLeft = getChildCount();
            int i3 = 0;
            while (true) {
                paddingLeft = i3;
                if (paddingLeft >= childLeft) {
                    break;
                }
                View child2 = getChildAt(paddingLeft);
                if (!((LayoutParams) child2.getLayoutParams()).isDecor) {
                    this.mPageTransformer.transformPage(child2, ((float) (child2.getLeft() - scrollX)) / ((float) getPaddedWidth()));
                }
                i3 = paddingLeft + 1;
            }
        }
        this.mCalledSuper = true;
    }

    private void completeScroll(boolean postEvents) {
        boolean needPopulate = this.mScrollState == 2;
        if (needPopulate) {
            setScrollingCacheEnabled(false);
            this.mScroller.abortAnimation();
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = this.mScroller.getCurrX();
            int y = this.mScroller.getCurrY();
            if (!(oldX == x && oldY == y)) {
                scrollTo(x, y);
            }
        }
        this.mPopulatePending = false;
        boolean needPopulate2 = needPopulate;
        for (int i = 0; i < this.mItems.size(); i++) {
            ItemInfo ii = (ItemInfo) this.mItems.get(i);
            if (ii.scrolling) {
                needPopulate2 = true;
                ii.scrolling = false;
            }
        }
        if (!needPopulate2) {
            return;
        }
        if (postEvents) {
            postOnAnimation(this.mEndScrollRunnable);
        } else {
            this.mEndScrollRunnable.run();
        }
    }

    private boolean isGutterDrag(float x, float dx) {
        return (x < ((float) this.mGutterSize) && dx > 0.0f) || (x > ((float) (getWidth() - this.mGutterSize)) && dx < 0.0f);
    }

    private void enableLayers(boolean enable) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).setLayerType(enable ? 2 : 0, null);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        MotionEvent motionEvent = ev;
        int action = ev.getAction() & 255;
        if (action == 3 || action == 1) {
            this.mIsBeingDragged = false;
            this.mIsUnableToDrag = false;
            this.mActivePointerId = -1;
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.recycle();
                this.mVelocityTracker = null;
            }
            return false;
        }
        if (action != 0) {
            if (this.mIsBeingDragged) {
                return true;
            }
            if (this.mIsUnableToDrag) {
                return false;
            }
        }
        float x;
        if (action == 0) {
            x = ev.getX();
            this.mInitialMotionX = x;
            this.mLastMotionX = x;
            x = ev.getY();
            this.mInitialMotionY = x;
            this.mLastMotionY = x;
            this.mActivePointerId = motionEvent.getPointerId(0);
            this.mIsUnableToDrag = false;
            this.mScroller.computeScrollOffset();
            if (this.mScrollState != 2 || Math.abs(this.mScroller.getFinalX() - this.mScroller.getCurrX()) <= this.mCloseEnough) {
                completeScroll(false);
                this.mIsBeingDragged = false;
            } else {
                this.mScroller.abortAnimation();
                this.mPopulatePending = false;
                populate();
                this.mIsBeingDragged = true;
                requestParentDisallowInterceptTouchEvent(true);
                setScrollState(1);
            }
        } else if (action == 2) {
            int activePointerId = this.mActivePointerId;
            if (activePointerId != -1) {
                float y;
                int pointerIndex = motionEvent.findPointerIndex(activePointerId);
                float x2 = motionEvent.getX(pointerIndex);
                float dx = x2 - this.mLastMotionX;
                float xDiff = Math.abs(dx);
                float y2 = motionEvent.getY(pointerIndex);
                float yDiff = Math.abs(y2 - this.mInitialMotionY);
                if (dx == 0.0f || isGutterDrag(this.mLastMotionX, dx)) {
                    y = y2;
                } else {
                    y = y2;
                    if (canScroll(this, false, (int) dx, (int) x2, (int) y2)) {
                        this.mLastMotionX = x2;
                        this.mLastMotionY = y;
                        this.mIsUnableToDrag = true;
                        return false;
                    }
                }
                if (xDiff > ((float) this.mTouchSlop) && 0.5f * xDiff > yDiff) {
                    this.mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(1);
                    if (dx > 0.0f) {
                        x = this.mInitialMotionX + ((float) this.mTouchSlop);
                    } else {
                        x = this.mInitialMotionX - ((float) this.mTouchSlop);
                    }
                    this.mLastMotionX = x;
                    this.mLastMotionY = y;
                    setScrollingCacheEnabled(true);
                } else if (yDiff > ((float) this.mTouchSlop)) {
                    this.mIsUnableToDrag = true;
                }
                if (this.mIsBeingDragged && performDrag(x2)) {
                    postInvalidateOnAnimation();
                }
            }
        } else if (action == 6) {
            onSecondaryPointerUp(ev);
        }
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        return this.mIsBeingDragged;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        MotionEvent motionEvent = ev;
        if ((ev.getAction() == 0 && ev.getEdgeFlags() != 0) || this.mAdapter == null || this.mAdapter.getCount() == 0) {
            return false;
        }
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        boolean needsInvalidate = false;
        float x;
        float scrolledPages;
        int pointerIndex;
        switch (ev.getAction() & 255) {
            case 0:
                this.mScroller.abortAnimation();
                this.mPopulatePending = false;
                populate();
                x = ev.getX();
                this.mInitialMotionX = x;
                this.mLastMotionX = x;
                x = ev.getY();
                this.mInitialMotionY = x;
                this.mLastMotionY = x;
                this.mActivePointerId = motionEvent.getPointerId(0);
                break;
            case 1:
                if (this.mIsBeingDragged) {
                    float nextPageOffset;
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getXVelocity(this.mActivePointerId);
                    this.mPopulatePending = true;
                    scrolledPages = ((float) getScrollStart()) / ((float) getPaddedWidth());
                    ItemInfo ii = infoForFirstVisiblePage();
                    int currentPage = ii.position;
                    if (isLayoutRtl()) {
                        nextPageOffset = (ii.offset - scrolledPages) / ii.widthFactor;
                    } else {
                        nextPageOffset = (scrolledPages - ii.offset) / ii.widthFactor;
                    }
                    setCurrentItemInternal(determineTargetPage(currentPage, nextPageOffset, initialVelocity, (int) (motionEvent.getX(motionEvent.findPointerIndex(this.mActivePointerId)) - this.mInitialMotionX)), true, true, initialVelocity);
                    this.mActivePointerId = -1;
                    endDrag();
                    this.mLeftEdge.onRelease();
                    this.mRightEdge.onRelease();
                    needsInvalidate = true;
                    break;
                }
                break;
            case 2:
                if (!this.mIsBeingDragged) {
                    pointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                    x = motionEvent.getX(pointerIndex);
                    float xDiff = Math.abs(x - this.mLastMotionX);
                    float y = motionEvent.getY(pointerIndex);
                    scrolledPages = Math.abs(y - this.mLastMotionY);
                    if (xDiff > ((float) this.mTouchSlop) && xDiff > scrolledPages) {
                        float f;
                        this.mIsBeingDragged = true;
                        requestParentDisallowInterceptTouchEvent(true);
                        if (x - this.mInitialMotionX > 0.0f) {
                            f = this.mInitialMotionX + ((float) this.mTouchSlop);
                        } else {
                            f = this.mInitialMotionX - ((float) this.mTouchSlop);
                        }
                        this.mLastMotionX = f;
                        this.mLastMotionY = y;
                        setScrollState(1);
                        setScrollingCacheEnabled(true);
                        ViewParent parent = getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                if (this.mIsBeingDragged) {
                    needsInvalidate = false | performDrag(motionEvent.getX(motionEvent.findPointerIndex(this.mActivePointerId)));
                    break;
                }
                break;
            case 3:
                if (this.mIsBeingDragged) {
                    scrollToItem(this.mCurItem, true, 0, false);
                    this.mActivePointerId = -1;
                    endDrag();
                    this.mLeftEdge.onRelease();
                    this.mRightEdge.onRelease();
                    needsInvalidate = true;
                    break;
                }
                break;
            case 5:
                pointerIndex = ev.getActionIndex();
                this.mLastMotionX = motionEvent.getX(pointerIndex);
                this.mActivePointerId = motionEvent.getPointerId(pointerIndex);
                break;
            case 6:
                onSecondaryPointerUp(ev);
                this.mLastMotionX = motionEvent.getX(motionEvent.findPointerIndex(this.mActivePointerId));
                break;
        }
        if (needsInvalidate) {
            postInvalidateOnAnimation();
        }
        return true;
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    private boolean performDrag(float x) {
        EdgeEffect startEdge;
        EdgeEffect endEdge;
        float scrollStart;
        float startBound;
        float endBound;
        float targetScrollX;
        float f = x;
        boolean needsInvalidate = false;
        int width = getPaddedWidth();
        float deltaX = this.mLastMotionX - f;
        this.mLastMotionX = f;
        if (isLayoutRtl()) {
            startEdge = this.mRightEdge;
            endEdge = this.mLeftEdge;
        } else {
            startEdge = this.mLeftEdge;
            endEdge = this.mRightEdge;
        }
        float nextScrollX = ((float) getScrollX()) + deltaX;
        if (isLayoutRtl()) {
            scrollStart = 1.6777216E7f - nextScrollX;
        } else {
            scrollStart = nextScrollX;
        }
        ItemInfo startItem = (ItemInfo) this.mItems.get(0);
        boolean z = true;
        boolean startAbsolute = startItem.position == 0;
        if (startAbsolute) {
            startBound = startItem.offset * ((float) width);
        } else {
            startBound = ((float) width) * this.mFirstOffset;
        }
        ItemInfo endItem = (ItemInfo) this.mItems.get(this.mItems.size() - 1);
        if (endItem.position != this.mAdapter.getCount() - 1) {
            z = false;
        }
        boolean endAbsolute = z;
        if (endAbsolute) {
            endBound = endItem.offset * ((float) width);
        } else {
            endBound = ((float) width) * this.mLastOffset;
        }
        if (scrollStart < startBound) {
            if (startAbsolute) {
                startEdge.onPull(Math.abs(startBound - scrollStart) / ((float) width));
                needsInvalidate = true;
            }
            f = startBound;
        } else if (scrollStart > endBound) {
            if (endAbsolute) {
                f = scrollStart - endBound;
                endEdge.onPull(Math.abs(f) / ((float) width));
                needsInvalidate = true;
            }
            f = endBound;
        } else {
            f = scrollStart;
        }
        if (isLayoutRtl()) {
            targetScrollX = 1.6777216E7f - f;
        } else {
            targetScrollX = f;
        }
        this.mLastMotionX += targetScrollX - ((float) ((int) targetScrollX));
        scrollTo((int) targetScrollX, getScrollY());
        pageScrolled((int) targetScrollX);
        return needsInvalidate;
    }

    private ItemInfo infoForFirstVisiblePage() {
        int startOffset = getScrollStart();
        int width = getPaddedWidth();
        float marginOffset = 0.0f;
        float scrollOffset = width > 0 ? ((float) startOffset) / ((float) width) : 0.0f;
        if (width > 0) {
            marginOffset = ((float) this.mPageMargin) / ((float) width);
        }
        int lastPos = -1;
        float lastOffset = 0.0f;
        float lastWidth = 0.0f;
        boolean first = true;
        ItemInfo lastItem = null;
        int N = this.mItems.size();
        int i = 0;
        while (i < N) {
            ItemInfo ii = (ItemInfo) this.mItems.get(i);
            if (!(first || ii.position == lastPos + 1)) {
                ii = this.mTempItem;
                ii.offset = (lastOffset + lastWidth) + marginOffset;
                ii.position = lastPos + 1;
                ii.widthFactor = this.mAdapter.getPageWidth(ii.position);
                i--;
            }
            float offset = ii.offset;
            float startBound = offset;
            if (!first && scrollOffset < startBound) {
                return lastItem;
            }
            if (scrollOffset >= (ii.widthFactor + offset) + marginOffset) {
                int startOffset2 = startOffset;
                if (i != this.mItems.size() - 1) {
                    first = false;
                    lastPos = ii.position;
                    lastOffset = offset;
                    lastWidth = ii.widthFactor;
                    lastItem = ii;
                    i++;
                    startOffset = startOffset2;
                }
            }
            return ii;
        }
        return lastItem;
    }

    private int getScrollStart() {
        if (isLayoutRtl()) {
            return 16777216 - getScrollX();
        }
        return getScrollX();
    }

    private int determineTargetPage(int currentPage, float pageOffset, int velocity, int deltaX) {
        int targetPage;
        if (Math.abs(deltaX) <= this.mFlingDistance || Math.abs(velocity) <= this.mMinimumVelocity) {
            targetPage = (int) (((float) currentPage) - (((float) this.mLeftIncr) * (pageOffset + (currentPage >= this.mCurItem ? 0.4f : 0.6f))));
        } else {
            targetPage = currentPage - (velocity < 0 ? this.mLeftIncr : 0);
        }
        if (this.mItems.size() <= 0) {
            return targetPage;
        }
        return MathUtils.constrain(targetPage, ((ItemInfo) this.mItems.get(0)).position, ((ItemInfo) this.mItems.get(this.mItems.size() - 1)).position);
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        boolean needsInvalidate = false;
        int overScrollMode = getOverScrollMode();
        if (overScrollMode == 0 || (overScrollMode == 1 && this.mAdapter != null && this.mAdapter.getCount() > 1)) {
            int restoreCount;
            int height;
            int width;
            if (!this.mLeftEdge.isFinished()) {
                restoreCount = canvas.save();
                height = (getHeight() - getPaddingTop()) - getPaddingBottom();
                width = getWidth();
                canvas.rotate(270.0f);
                canvas.translate((float) ((-height) + getPaddingTop()), this.mFirstOffset * ((float) width));
                this.mLeftEdge.setSize(height, width);
                needsInvalidate = false | this.mLeftEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }
            if (!this.mRightEdge.isFinished()) {
                restoreCount = canvas.save();
                height = getWidth();
                width = (getHeight() - getPaddingTop()) - getPaddingBottom();
                canvas.rotate(90.0f);
                canvas.translate((float) (-getPaddingTop()), (-(this.mLastOffset + 1.0f)) * ((float) height));
                this.mRightEdge.setSize(width, height);
                needsInvalidate |= this.mRightEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }
        } else {
            this.mLeftEdge.finish();
            this.mRightEdge.finish();
        }
        if (needsInvalidate) {
            postInvalidateOnAnimation();
        }
    }

    protected void onDraw(Canvas canvas) {
        Canvas ii;
        super.onDraw(canvas);
        if (this.mPageMargin > 0 && this.mMarginDrawable != null && this.mItems.size() > 0 && this.mAdapter != null) {
            int scrollX = getScrollX();
            int width = getWidth();
            float marginOffset = ((float) this.mPageMargin) / ((float) width);
            ItemInfo ii2 = (ItemInfo) this.mItems.get(0);
            float offset = ii2.offset;
            int itemCount = this.mItems.size();
            int firstPos = ii2.position;
            int lastPos = ((ItemInfo) this.mItems.get(itemCount - 1)).position;
            float offset2 = offset;
            int itemIndex = 0;
            int pos = firstPos;
            while (pos < lastPos) {
                float itemOffset;
                float widthFactor;
                float left;
                ItemInfo ii3;
                int itemIndex2;
                int itemCount2;
                while (pos > ii2.position && itemIndex < itemCount) {
                    itemIndex++;
                    ii2 = (ItemInfo) this.mItems.get(itemIndex);
                }
                if (pos == ii2.position) {
                    itemOffset = ii2.offset;
                    widthFactor = ii2.widthFactor;
                } else {
                    itemOffset = offset2;
                    widthFactor = this.mAdapter.getPageWidth(pos);
                }
                float scaledOffset = ((float) width) * itemOffset;
                if (isLayoutRtl()) {
                    left = 1.6777216E7f - scaledOffset;
                } else {
                    left = (((float) width) * widthFactor) + scaledOffset;
                }
                offset2 = (itemOffset + widthFactor) + marginOffset;
                float marginOffset2 = marginOffset;
                if (((float) this.mPageMargin) + left > ((float) scrollX)) {
                    ii3 = ii2;
                    itemIndex2 = itemIndex;
                    itemCount2 = itemCount;
                    this.mMarginDrawable.setBounds((int) left, this.mTopPageBounds, (int) ((((float) this.mPageMargin) + left) + 0.5f), this.mBottomPageBounds);
                    this.mMarginDrawable.draw(canvas);
                } else {
                    ii3 = ii2;
                    itemIndex2 = itemIndex;
                    itemCount2 = itemCount;
                    ii = canvas;
                }
                if (left <= ((float) (scrollX + width))) {
                    pos++;
                    marginOffset = marginOffset2;
                    ii2 = ii3;
                    itemIndex = itemIndex2;
                    itemCount = itemCount2;
                } else {
                    return;
                }
            }
        }
        ii = canvas;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = ev.getActionIndex();
        if (ev.getPointerId(pointerIndex) == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            this.mLastMotionX = ev.getX(newPointerIndex);
            this.mActivePointerId = ev.getPointerId(newPointerIndex);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    private void endDrag() {
        this.mIsBeingDragged = false;
        this.mIsUnableToDrag = false;
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void setScrollingCacheEnabled(boolean enabled) {
        if (this.mScrollingCacheEnabled != enabled) {
            this.mScrollingCacheEnabled = enabled;
        }
    }

    public boolean canScrollHorizontally(int direction) {
        boolean z = false;
        if (this.mAdapter == null) {
            return false;
        }
        int width = getPaddedWidth();
        int scrollX = getScrollX();
        if (direction < 0) {
            if (scrollX > ((int) (((float) width) * this.mFirstOffset))) {
                z = true;
            }
            return z;
        } else if (direction <= 0) {
            return false;
        } else {
            if (scrollX < ((int) (((float) width) * this.mLastOffset))) {
                z = true;
            }
            return z;
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0065, code skipped:
            if (r0.canScrollHorizontally(-r17) != false) goto L_0x006b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        int scrollX;
        View view = v;
        boolean z = true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            scrollX = view.getScrollX();
            int scrollY = view.getScrollY();
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()) {
                    if (canScroll(child, true, dx, (x + scrollX) - child.getLeft(), (y + scrollY) - child.getTop())) {
                        return true;
                    }
                }
            }
        }
        if (!checkV) {
            scrollX = dx;
        }
        z = false;
        return z;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    public boolean executeKeyEvent(KeyEvent event) {
        if (event.getAction() != 0) {
            return false;
        }
        int keyCode = event.getKeyCode();
        if (keyCode != 61) {
            switch (keyCode) {
                case 21:
                    return arrowScroll(17);
                case 22:
                    return arrowScroll(66);
                default:
                    return false;
            }
        } else if (event.hasNoModifiers()) {
            return arrowScroll(2);
        } else {
            if (event.hasModifiers(1)) {
                return arrowScroll(1);
            }
            return false;
        }
    }

    public boolean arrowScroll(int direction) {
        boolean isChild;
        View currentFocused = findFocus();
        if (currentFocused == this) {
            currentFocused = null;
        } else if (currentFocused != null) {
            isChild = false;
            for (ViewPager parent = currentFocused.getParent(); parent instanceof ViewGroup; parent = parent.getParent()) {
                if (parent == this) {
                    isChild = true;
                    break;
                }
            }
            if (!isChild) {
                StringBuilder sb = new StringBuilder();
                sb.append(currentFocused.getClass().getSimpleName());
                for (ViewParent parent2 = currentFocused.getParent(); parent2 instanceof ViewGroup; parent2 = parent2.getParent()) {
                    sb.append(" => ");
                    sb.append(parent2.getClass().getSimpleName());
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("arrowScroll tried to find focus based on non-child current focused view ");
                stringBuilder.append(sb.toString());
                Log.e(str, stringBuilder.toString());
                currentFocused = null;
            }
        }
        isChild = false;
        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);
        if (nextFocused == null || nextFocused == currentFocused) {
            if (direction == 17 || direction == 1) {
                isChild = pageLeft();
            } else if (direction == 66 || direction == 2) {
                isChild = pageRight();
            }
        } else if (direction == 17) {
            isChild = (currentFocused == null || getChildRectInPagerCoordinates(this.mTempRect, nextFocused).left < getChildRectInPagerCoordinates(this.mTempRect, currentFocused).left) ? nextFocused.requestFocus() : pageLeft();
        } else if (direction == 66) {
            isChild = (currentFocused == null || getChildRectInPagerCoordinates(this.mTempRect, nextFocused).left > getChildRectInPagerCoordinates(this.mTempRect, currentFocused).left) ? nextFocused.requestFocus() : pageRight();
        }
        if (isChild) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
        }
        return isChild;
    }

    private Rect getChildRectInPagerCoordinates(Rect outRect, View child) {
        if (outRect == null) {
            outRect = new Rect();
        }
        if (child == null) {
            outRect.set(0, 0, 0, 0);
            return outRect;
        }
        outRect.left = child.getLeft();
        outRect.right = child.getRight();
        outRect.top = child.getTop();
        outRect.bottom = child.getBottom();
        ViewGroup parent = child.getParent();
        while ((parent instanceof ViewGroup) && parent != this) {
            ViewGroup group = parent;
            outRect.left += group.getLeft();
            outRect.right += group.getRight();
            outRect.top += group.getTop();
            outRect.bottom += group.getBottom();
            parent = group.getParent();
        }
        return outRect;
    }

    boolean pageLeft() {
        return setCurrentItemInternal(this.mCurItem + this.mLeftIncr, true, false);
    }

    boolean pageRight() {
        return setCurrentItemInternal(this.mCurItem - this.mLeftIncr, true, false);
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        if (layoutDirection == 0) {
            this.mLeftIncr = -1;
        } else {
            this.mLeftIncr = 1;
        }
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        int focusableCount = views.size();
        int descendantFocusability = getDescendantFocusability();
        if (descendantFocusability != Protocol.BASE_NSD_MANAGER) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == 0) {
                    ItemInfo ii = infoForChild(child);
                    if (ii != null && ii.position == this.mCurItem) {
                        child.addFocusables(views, direction, focusableMode);
                    }
                }
            }
        }
        if ((descendantFocusability == 262144 && focusableCount != views.size()) || !isFocusable()) {
            return;
        }
        if (!(((focusableMode & 1) == 1 && isInTouchMode() && !isFocusableInTouchMode()) || views == null)) {
            views.add(this);
        }
    }

    public void addTouchables(ArrayList<View> views) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == 0) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == this.mCurItem) {
                    child.addTouchables(views);
                }
            }
        }
    }

    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        int index;
        int increment;
        int end;
        int count = getChildCount();
        if ((direction & 2) != 0) {
            index = 0;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        }
        for (int i = index; i != end; i += increment) {
            View child = getChildAt(i);
            if (child.getVisibility() == 0) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == this.mCurItem && child.requestFocus(direction, previouslyFocusedRect)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected android.view.ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    protected android.view.ViewGroup.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return (p instanceof LayoutParams) && super.checkLayoutParams(p);
    }

    public android.view.ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(ViewPager.class.getName());
        event.setScrollable(canScroll());
        if (event.getEventType() == 4096 && this.mAdapter != null) {
            event.setItemCount(this.mAdapter.getCount());
            event.setFromIndex(this.mCurItem);
            event.setToIndex(this.mCurItem);
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(ViewPager.class.getName());
        info.setScrollable(canScroll());
        if (canScrollHorizontally(1)) {
            info.addAction(AccessibilityAction.ACTION_SCROLL_FORWARD);
            info.addAction(AccessibilityAction.ACTION_SCROLL_RIGHT);
        }
        if (canScrollHorizontally(-1)) {
            info.addAction(AccessibilityAction.ACTION_SCROLL_BACKWARD);
            info.addAction(AccessibilityAction.ACTION_SCROLL_LEFT);
        }
    }

    public boolean performAccessibilityAction(int action, Bundle args) {
        if (super.performAccessibilityAction(action, args)) {
            return true;
        }
        if (action != 4096) {
            if (action == SmsCbConstants.SERIAL_NUMBER_ETWS_EMERGENCY_USER_ALERT || action == 16908345) {
                if (!canScrollHorizontally(-1)) {
                    return false;
                }
                setCurrentItem(this.mCurItem - 1);
                return true;
            } else if (action != 16908347) {
                return false;
            }
        }
        if (!canScrollHorizontally(1)) {
            return false;
        }
        setCurrentItem(this.mCurItem + 1);
        return true;
    }

    private boolean canScroll() {
        return this.mAdapter != null && this.mAdapter.getCount() > 1;
    }
}
