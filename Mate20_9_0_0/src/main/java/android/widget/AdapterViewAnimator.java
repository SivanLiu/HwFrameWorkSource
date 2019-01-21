package android.widget;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.FilterComparison;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.rms.AppAssociate;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RemoteViews.OnClickHandler;
import android.widget.RemoteViewsAdapter.AsyncRemoteAdapterAction;
import android.widget.RemoteViewsAdapter.RemoteAdapterConnectionCallback;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class AdapterViewAnimator extends AdapterView<Adapter> implements RemoteAdapterConnectionCallback, Advanceable {
    private static final int DEFAULT_ANIMATION_DURATION = 200;
    private static final String TAG = "RemoteViewAnimator";
    static final int TOUCH_MODE_DOWN_IN_CURRENT_VIEW = 1;
    static final int TOUCH_MODE_HANDLED = 2;
    static final int TOUCH_MODE_NONE = 0;
    int mActiveOffset;
    Adapter mAdapter;
    boolean mAnimateFirstTime;
    int mCurrentWindowEnd;
    int mCurrentWindowStart;
    int mCurrentWindowStartUnbounded;
    AdapterDataSetObserver mDataSetObserver;
    boolean mDeferNotifyDataSetChanged;
    boolean mFirstTime;
    ObjectAnimator mInAnimation;
    boolean mLoopViews;
    int mMaxNumActiveViews;
    ObjectAnimator mOutAnimation;
    private Runnable mPendingCheckForTap;
    ArrayList<Integer> mPreviousViews;
    int mReferenceChildHeight;
    int mReferenceChildWidth;
    RemoteViewsAdapter mRemoteViewsAdapter;
    private int mRestoreWhichChild;
    private int mTouchMode;
    HashMap<Integer, ViewAndMetaData> mViewsMap;
    int mWhichChild;

    final class CheckForTap implements Runnable {
        CheckForTap() {
        }

        public void run() {
            if (AdapterViewAnimator.this.mTouchMode == 1) {
                AdapterViewAnimator.this.showTapFeedback(AdapterViewAnimator.this.getCurrentView());
            }
        }
    }

    class ViewAndMetaData {
        int adapterPosition;
        long itemId;
        int relativeIndex;
        View view;

        ViewAndMetaData(View view, int relativeIndex, int adapterPosition, long itemId) {
            this.view = view;
            this.relativeIndex = relativeIndex;
            this.adapterPosition = adapterPosition;
            this.itemId = itemId;
        }
    }

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int whichChild;

        SavedState(Parcelable superState, int whichChild) {
            super(superState);
            this.whichChild = whichChild;
        }

        private SavedState(Parcel in) {
            super(in);
            this.whichChild = in.readInt();
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.whichChild);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AdapterViewAnimator.SavedState{ whichChild = ");
            stringBuilder.append(this.whichChild);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }
    }

    public AdapterViewAnimator(Context context) {
        this(context, null);
    }

    public AdapterViewAnimator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdapterViewAnimator(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AdapterViewAnimator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mWhichChild = 0;
        this.mRestoreWhichChild = -1;
        this.mAnimateFirstTime = true;
        this.mActiveOffset = 0;
        this.mMaxNumActiveViews = 1;
        this.mViewsMap = new HashMap();
        this.mCurrentWindowStart = 0;
        this.mCurrentWindowEnd = -1;
        this.mCurrentWindowStartUnbounded = 0;
        this.mDeferNotifyDataSetChanged = false;
        this.mFirstTime = true;
        this.mLoopViews = true;
        this.mReferenceChildWidth = -1;
        this.mReferenceChildHeight = -1;
        this.mTouchMode = 0;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AdapterViewAnimator, defStyleAttr, defStyleRes);
        int resource = a.getResourceId(0, 0);
        if (resource > 0) {
            setInAnimation(context, resource);
        } else {
            setInAnimation(getDefaultInAnimation());
        }
        resource = a.getResourceId(1, 0);
        if (resource > 0) {
            setOutAnimation(context, resource);
        } else {
            setOutAnimation(getDefaultOutAnimation());
        }
        setAnimateFirstView(a.getBoolean(2, true));
        this.mLoopViews = a.getBoolean(3, false);
        a.recycle();
        initViewAnimator();
    }

    private void initViewAnimator() {
        this.mPreviousViews = new ArrayList();
    }

    void configureViewAnimator(int numVisibleViews, int activeOffset) {
        this.mMaxNumActiveViews = numVisibleViews;
        this.mActiveOffset = activeOffset;
        this.mPreviousViews.clear();
        this.mViewsMap.clear();
        removeAllViewsInLayout();
        this.mCurrentWindowStart = 0;
        this.mCurrentWindowEnd = -1;
    }

    void transformViewForTransition(int fromIndex, int toIndex, View view, boolean animate) {
        if (fromIndex == -1) {
            this.mInAnimation.setTarget(view);
            this.mInAnimation.start();
        } else if (toIndex == -1) {
            this.mOutAnimation.setTarget(view);
            this.mOutAnimation.start();
        }
    }

    ObjectAnimator getDefaultInAnimation() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(null, AppAssociate.ASSOC_WINDOW_ALPHA, new float[]{0.0f, 1.0f});
        anim.setDuration(200);
        return anim;
    }

    ObjectAnimator getDefaultOutAnimation() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(null, AppAssociate.ASSOC_WINDOW_ALPHA, new float[]{1.0f, 0.0f});
        anim.setDuration(200);
        return anim;
    }

    @RemotableViewMethod
    public void setDisplayedChild(int whichChild) {
        setDisplayedChild(whichChild, true);
    }

    private void setDisplayedChild(int whichChild, boolean animate) {
        if (this.mAdapter != null) {
            this.mWhichChild = whichChild;
            boolean z = true;
            if (whichChild >= getWindowSize()) {
                this.mWhichChild = this.mLoopViews ? 0 : getWindowSize() - 1;
            } else if (whichChild < 0) {
                this.mWhichChild = this.mLoopViews ? getWindowSize() - 1 : 0;
            }
            if (getFocusedChild() == null) {
                z = false;
            }
            boolean hasFocus = z;
            showOnly(this.mWhichChild, animate);
            if (hasFocus) {
                requestFocus(2);
            }
        }
    }

    void applyTransformForChildAtIndex(View child, int relativeIndex) {
    }

    public int getDisplayedChild() {
        return this.mWhichChild;
    }

    public void showNext() {
        setDisplayedChild(this.mWhichChild + 1);
    }

    public void showPrevious() {
        setDisplayedChild(this.mWhichChild - 1);
    }

    int modulo(int pos, int size) {
        if (size > 0) {
            return ((pos % size) + size) % size;
        }
        return 0;
    }

    View getViewAtRelativeIndex(int relativeIndex) {
        if (relativeIndex >= 0 && relativeIndex <= getNumActiveViews() - 1 && this.mAdapter != null) {
            int i = modulo(this.mCurrentWindowStartUnbounded + relativeIndex, getWindowSize());
            if (this.mViewsMap.get(Integer.valueOf(i)) != null) {
                return ((ViewAndMetaData) this.mViewsMap.get(Integer.valueOf(i))).view;
            }
        }
        return null;
    }

    int getNumActiveViews() {
        if (this.mAdapter != null) {
            return Math.min(getCount() + 1, this.mMaxNumActiveViews);
        }
        return this.mMaxNumActiveViews;
    }

    int getWindowSize() {
        if (this.mAdapter == null) {
            return 0;
        }
        int adapterCount = getCount();
        if (adapterCount > getNumActiveViews() || !this.mLoopViews) {
            return adapterCount;
        }
        return this.mMaxNumActiveViews * adapterCount;
    }

    private ViewAndMetaData getMetaDataForChild(View child) {
        for (ViewAndMetaData vm : this.mViewsMap.values()) {
            if (vm.view == child) {
                return vm;
            }
        }
        return null;
    }

    LayoutParams createOrReuseLayoutParams(View v) {
        LayoutParams currentLp = v.getLayoutParams();
        if (currentLp != null) {
            return currentLp;
        }
        return new LayoutParams(0, 0);
    }

    void refreshChildren() {
        if (this.mAdapter != null) {
            for (int i = this.mCurrentWindowStart; i <= this.mCurrentWindowEnd; i++) {
                int index = modulo(i, getWindowSize());
                View updatedChild = this.mAdapter.getView(modulo(i, getCount()), null, this);
                if (updatedChild.getImportantForAccessibility() == 0) {
                    updatedChild.setImportantForAccessibility(1);
                }
                if (this.mViewsMap.containsKey(Integer.valueOf(index))) {
                    FrameLayout fl = ((ViewAndMetaData) this.mViewsMap.get(Integer.valueOf(index))).view;
                    if (updatedChild != null) {
                        fl.removeAllViewsInLayout();
                        fl.addView(updatedChild);
                    }
                }
            }
        }
    }

    FrameLayout getFrameForChild() {
        return new FrameLayout(this.mContext);
    }

    void showOnly(int childIndex, boolean animate) {
        boolean z = animate;
        if (this.mAdapter != null) {
            int adapterCount = getCount();
            if (adapterCount != 0) {
                int i;
                View viewToRemove;
                int oldRelativeIndex;
                for (i = 0; i < this.mPreviousViews.size(); i++) {
                    viewToRemove = ((ViewAndMetaData) this.mViewsMap.get(this.mPreviousViews.get(i))).view;
                    this.mViewsMap.remove(this.mPreviousViews.get(i));
                    viewToRemove.clearAnimation();
                    if (viewToRemove instanceof ViewGroup) {
                        ((ViewGroup) viewToRemove).removeAllViewsInLayout();
                    }
                    applyTransformForChildAtIndex(viewToRemove, -1);
                    removeViewInLayout(viewToRemove);
                }
                this.mPreviousViews.clear();
                int newWindowStartUnbounded = childIndex - this.mActiveOffset;
                int newWindowEndUnbounded = (getNumActiveViews() + newWindowStartUnbounded) - 1;
                i = Math.max(0, newWindowStartUnbounded);
                int newWindowEnd = Math.min(adapterCount - 1, newWindowEndUnbounded);
                if (this.mLoopViews) {
                    i = newWindowStartUnbounded;
                    newWindowEnd = newWindowEndUnbounded;
                }
                int newWindowStart = i;
                int newWindowEnd2 = newWindowEnd;
                int rangeStart = modulo(newWindowStart, getWindowSize());
                int rangeEnd = modulo(newWindowEnd2, getWindowSize());
                boolean wrap = false;
                if (rangeStart > rangeEnd) {
                    wrap = true;
                }
                boolean wrap2 = wrap;
                for (Integer index : this.mViewsMap.keySet()) {
                    boolean remove = false;
                    if (!wrap2 && (index.intValue() < rangeStart || index.intValue() > rangeEnd)) {
                        remove = true;
                    } else if (wrap2 && index.intValue() > rangeEnd && index.intValue() < rangeStart) {
                        remove = true;
                    }
                    if (remove) {
                        View previousView = ((ViewAndMetaData) this.mViewsMap.get(index)).view;
                        oldRelativeIndex = ((ViewAndMetaData) this.mViewsMap.get(index)).relativeIndex;
                        this.mPreviousViews.add(index);
                        transformViewForTransition(oldRelativeIndex, -1, previousView, z);
                    }
                }
                int i2;
                int i3;
                int i4;
                int i5;
                if (newWindowStart == this.mCurrentWindowStart && newWindowEnd2 == this.mCurrentWindowEnd && newWindowStartUnbounded == this.mCurrentWindowStartUnbounded) {
                    i2 = rangeEnd;
                    i3 = rangeStart;
                    i4 = adapterCount;
                    i = newWindowStartUnbounded;
                    i5 = newWindowEndUnbounded;
                    adapterCount = newWindowStart;
                    newWindowStartUnbounded = newWindowEnd2;
                } else {
                    int adapterCount2;
                    int newWindowStartUnbounded2;
                    i = newWindowStart;
                    while (true) {
                        int i6 = i;
                        if (i6 > newWindowEnd2) {
                            break;
                        }
                        int index2 = modulo(i6, getWindowSize());
                        if (this.mViewsMap.containsKey(Integer.valueOf(index2))) {
                            i = ((ViewAndMetaData) this.mViewsMap.get(Integer.valueOf(index2))).relativeIndex;
                        } else {
                            i = -1;
                        }
                        oldRelativeIndex = i;
                        i4 = i6 - newWindowStartUnbounded;
                        wrap = this.mViewsMap.containsKey(Integer.valueOf(index2)) && !this.mPreviousViews.contains(Integer.valueOf(index2));
                        View view;
                        if (wrap) {
                            view = ((ViewAndMetaData) this.mViewsMap.get(Integer.valueOf(index2))).view;
                            i2 = rangeEnd;
                            ((ViewAndMetaData) this.mViewsMap.get(Integer.valueOf(index2))).relativeIndex = i4;
                            applyTransformForChildAtIndex(view, i4);
                            transformViewForTransition(oldRelativeIndex, i4, view, z);
                            newWindowEnd = i4;
                            i3 = rangeStart;
                            adapterCount2 = adapterCount;
                            newWindowStartUnbounded2 = newWindowStartUnbounded;
                            int i7 = oldRelativeIndex;
                            i5 = newWindowEndUnbounded;
                            adapterCount = newWindowStart;
                            newWindowStartUnbounded = newWindowEnd2;
                        } else {
                            i2 = rangeEnd;
                            rangeEnd = modulo(i6, adapterCount);
                            viewToRemove = this.mAdapter.getView(rangeEnd, null, this);
                            long itemId = this.mAdapter.getItemId(rangeEnd);
                            view = getFrameForChild();
                            if (viewToRemove != null) {
                                view.addView(viewToRemove);
                            }
                            i5 = newWindowEndUnbounded;
                            View fl = view;
                            int newRelativeIndex = i4;
                            i3 = rangeStart;
                            adapterCount2 = adapterCount;
                            newWindowStartUnbounded2 = newWindowStartUnbounded;
                            adapterCount = newWindowStart;
                            newWindowStartUnbounded = newWindowEnd2;
                            this.mViewsMap.put(Integer.valueOf(index2), new ViewAndMetaData(fl, newRelativeIndex, rangeEnd, itemId));
                            view = fl;
                            addChild(view);
                            newWindowEnd = newRelativeIndex;
                            applyTransformForChildAtIndex(view, newWindowEnd);
                            transformViewForTransition(-1, newWindowEnd, view, z);
                        }
                        ((ViewAndMetaData) this.mViewsMap.get(Integer.valueOf(index2))).view.bringToFront();
                        i = i6 + 1;
                        newWindowStart = adapterCount;
                        newWindowEnd2 = newWindowStartUnbounded;
                        rangeEnd = i2;
                        newWindowEndUnbounded = i5;
                        rangeStart = i3;
                        adapterCount = adapterCount2;
                        newWindowStartUnbounded = newWindowStartUnbounded2;
                    }
                    i3 = rangeStart;
                    adapterCount2 = adapterCount;
                    newWindowStartUnbounded2 = newWindowStartUnbounded;
                    i5 = newWindowEndUnbounded;
                    newWindowStartUnbounded = newWindowEnd2;
                    this.mCurrentWindowStart = newWindowStart;
                    this.mCurrentWindowEnd = newWindowStartUnbounded;
                    this.mCurrentWindowStartUnbounded = newWindowStartUnbounded2;
                    if (this.mRemoteViewsAdapter != null) {
                        i4 = adapterCount2;
                        this.mRemoteViewsAdapter.setVisibleRangeHint(modulo(this.mCurrentWindowStart, i4), modulo(this.mCurrentWindowEnd, i4));
                    }
                }
                requestLayout();
                invalidate();
            }
        }
    }

    private void addChild(View child) {
        addViewInLayout(child, -1, createOrReuseLayoutParams(child));
        if (this.mReferenceChildWidth == -1 || this.mReferenceChildHeight == -1) {
            int measureSpec = MeasureSpec.makeMeasureSpec(0, 0);
            child.measure(measureSpec, measureSpec);
            this.mReferenceChildWidth = child.getMeasuredWidth();
            this.mReferenceChildHeight = child.getMeasuredHeight();
        }
    }

    void showTapFeedback(View v) {
        v.setPressed(true);
    }

    void hideTapFeedback(View v) {
        v.setPressed(false);
    }

    void cancelHandleClick() {
        View v = getCurrentView();
        if (v != null) {
            hideTapFeedback(v);
        }
        this.mTouchMode = 0;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        boolean handled = false;
        if (action != 6) {
            switch (action) {
                case 0:
                    View v = getCurrentView();
                    if (v != null && isTransformedTouchPointInView(ev.getX(), ev.getY(), v, null)) {
                        if (this.mPendingCheckForTap == null) {
                            this.mPendingCheckForTap = new CheckForTap();
                        }
                        this.mTouchMode = 1;
                        postDelayed(this.mPendingCheckForTap, (long) ViewConfiguration.getTapTimeout());
                        break;
                    }
                case 1:
                    if (this.mTouchMode == 1) {
                        final View v2 = getCurrentView();
                        final ViewAndMetaData viewData = getMetaDataForChild(v2);
                        if (v2 != null && isTransformedTouchPointInView(ev.getX(), ev.getY(), v2, null)) {
                            Handler handler = getHandler();
                            if (handler != null) {
                                handler.removeCallbacks(this.mPendingCheckForTap);
                            }
                            showTapFeedback(v2);
                            postDelayed(new Runnable() {
                                public void run() {
                                    AdapterViewAnimator.this.hideTapFeedback(v2);
                                    AdapterViewAnimator.this.post(new Runnable() {
                                        public void run() {
                                            if (viewData != null) {
                                                AdapterViewAnimator.this.performItemClick(v2, viewData.adapterPosition, viewData.itemId);
                                            } else {
                                                AdapterViewAnimator.this.performItemClick(v2, 0, 0);
                                            }
                                        }
                                    });
                                }
                            }, (long) ViewConfiguration.getPressedStateDuration());
                            handled = true;
                        }
                    }
                    this.mTouchMode = 0;
                    break;
                case 3:
                    View v3 = getCurrentView();
                    if (v3 != null) {
                        hideTapFeedback(v3);
                    }
                    this.mTouchMode = 0;
                    break;
            }
        }
        return handled;
    }

    private void measureChildren() {
        int count = getChildCount();
        int childWidth = (getMeasuredWidth() - this.mPaddingLeft) - this.mPaddingRight;
        int childHeight = (getMeasuredHeight() - this.mPaddingTop) - this.mPaddingBottom;
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(childWidth, 1073741824), MeasureSpec.makeMeasureSpec(childHeight, 1073741824));
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int i = 0;
        boolean haveChildRefSize = (this.mReferenceChildWidth == -1 || this.mReferenceChildHeight == -1) ? false : true;
        int i2;
        if (heightSpecMode == 0) {
            if (haveChildRefSize) {
                i2 = (this.mReferenceChildHeight + this.mPaddingTop) + this.mPaddingBottom;
            } else {
                i2 = 0;
            }
            heightSpecSize = i2;
        } else if (heightSpecMode == Integer.MIN_VALUE && haveChildRefSize) {
            i2 = (this.mReferenceChildHeight + this.mPaddingTop) + this.mPaddingBottom;
            heightSpecSize = i2 > heightSpecSize ? heightSpecSize | 16777216 : i2;
        }
        if (widthSpecMode == 0) {
            if (haveChildRefSize) {
                i = this.mPaddingRight + (this.mReferenceChildWidth + this.mPaddingLeft);
            }
            widthSpecSize = i;
        } else if (heightSpecMode == Integer.MIN_VALUE && haveChildRefSize) {
            i = (this.mReferenceChildWidth + this.mPaddingLeft) + this.mPaddingRight;
            widthSpecSize = i > widthSpecSize ? widthSpecSize | 16777216 : i;
        }
        setMeasuredDimension(widthSpecSize, heightSpecSize);
        measureChildren();
    }

    void checkForAndHandleDataChanged() {
        if (this.mDataChanged) {
            post(new Runnable() {
                public void run() {
                    AdapterViewAnimator.this.handleDataChanged();
                    if (AdapterViewAnimator.this.mWhichChild >= AdapterViewAnimator.this.getWindowSize()) {
                        AdapterViewAnimator.this.mWhichChild = 0;
                        AdapterViewAnimator.this.showOnly(AdapterViewAnimator.this.mWhichChild, false);
                    } else if (AdapterViewAnimator.this.mOldItemCount != AdapterViewAnimator.this.getCount()) {
                        AdapterViewAnimator.this.showOnly(AdapterViewAnimator.this.mWhichChild, false);
                    }
                    AdapterViewAnimator.this.refreshChildren();
                    AdapterViewAnimator.this.requestLayout();
                }
            });
        }
        this.mDataChanged = false;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        checkForAndHandleDataChanged();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.layout(this.mPaddingLeft, this.mPaddingTop, this.mPaddingLeft + child.getMeasuredWidth(), this.mPaddingTop + child.getMeasuredHeight());
        }
    }

    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (this.mRemoteViewsAdapter != null) {
            this.mRemoteViewsAdapter.saveRemoteViewsCache();
        }
        return new SavedState(superState, this.mWhichChild);
    }

    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mWhichChild = ss.whichChild;
        if (this.mRemoteViewsAdapter == null || this.mAdapter != null) {
            setDisplayedChild(this.mWhichChild, false);
        } else {
            this.mRestoreWhichChild = this.mWhichChild;
        }
    }

    public View getCurrentView() {
        return getViewAtRelativeIndex(this.mActiveOffset);
    }

    public ObjectAnimator getInAnimation() {
        return this.mInAnimation;
    }

    public void setInAnimation(ObjectAnimator inAnimation) {
        this.mInAnimation = inAnimation;
    }

    public ObjectAnimator getOutAnimation() {
        return this.mOutAnimation;
    }

    public void setOutAnimation(ObjectAnimator outAnimation) {
        this.mOutAnimation = outAnimation;
    }

    public void setInAnimation(Context context, int resourceID) {
        setInAnimation((ObjectAnimator) AnimatorInflater.loadAnimator(context, resourceID));
    }

    public void setOutAnimation(Context context, int resourceID) {
        setOutAnimation((ObjectAnimator) AnimatorInflater.loadAnimator(context, resourceID));
    }

    public void setAnimateFirstView(boolean animate) {
        this.mAnimateFirstTime = animate;
    }

    public int getBaseline() {
        return getCurrentView() != null ? getCurrentView().getBaseline() : super.getBaseline();
    }

    public Adapter getAdapter() {
        return this.mAdapter;
    }

    public void setAdapter(Adapter adapter) {
        if (!(this.mAdapter == null || this.mDataSetObserver == null)) {
            this.mAdapter.unregisterDataSetObserver(this.mDataSetObserver);
        }
        this.mAdapter = adapter;
        checkFocus();
        if (this.mAdapter != null) {
            this.mDataSetObserver = new AdapterDataSetObserver();
            this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
            this.mItemCount = this.mAdapter.getCount();
        }
        setFocusable(true);
        this.mWhichChild = 0;
        showOnly(this.mWhichChild, false);
    }

    @RemotableViewMethod(asyncImpl = "setRemoteViewsAdapterAsync")
    public void setRemoteViewsAdapter(Intent intent) {
        setRemoteViewsAdapter(intent, false);
    }

    public Runnable setRemoteViewsAdapterAsync(Intent intent) {
        return new AsyncRemoteAdapterAction(this, intent);
    }

    public void setRemoteViewsAdapter(Intent intent, boolean isAsync) {
        if (this.mRemoteViewsAdapter == null || !new FilterComparison(intent).equals(new FilterComparison(this.mRemoteViewsAdapter.getRemoteViewsServiceIntent()))) {
            this.mDeferNotifyDataSetChanged = false;
            this.mRemoteViewsAdapter = new RemoteViewsAdapter(getContext(), intent, this, isAsync);
            if (this.mRemoteViewsAdapter.isDataReady()) {
                setAdapter(this.mRemoteViewsAdapter);
            }
        }
    }

    public void setRemoteViewsOnClickHandler(OnClickHandler handler) {
        if (this.mRemoteViewsAdapter != null) {
            this.mRemoteViewsAdapter.setRemoteViewsOnClickHandler(handler);
        }
    }

    public void setSelection(int position) {
        setDisplayedChild(position);
    }

    public View getSelectedView() {
        return getViewAtRelativeIndex(this.mActiveOffset);
    }

    public void deferNotifyDataSetChanged() {
        this.mDeferNotifyDataSetChanged = true;
    }

    public boolean onRemoteAdapterConnected() {
        if (this.mRemoteViewsAdapter != this.mAdapter) {
            setAdapter(this.mRemoteViewsAdapter);
            if (this.mDeferNotifyDataSetChanged) {
                this.mRemoteViewsAdapter.notifyDataSetChanged();
                this.mDeferNotifyDataSetChanged = false;
            }
            if (this.mRestoreWhichChild > -1) {
                setDisplayedChild(this.mRestoreWhichChild, false);
                this.mRestoreWhichChild = -1;
            }
            return false;
        } else if (this.mRemoteViewsAdapter == null) {
            return false;
        } else {
            this.mRemoteViewsAdapter.superNotifyDataSetChanged();
            return true;
        }
    }

    public void onRemoteAdapterDisconnected() {
    }

    public void advance() {
        showNext();
    }

    public void fyiWillBeAdvancedByHostKThx() {
    }

    public CharSequence getAccessibilityClassName() {
        return AdapterViewAnimator.class.getName();
    }
}
