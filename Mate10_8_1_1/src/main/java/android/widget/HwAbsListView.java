package android.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.hwcontrol.HwWidgetFactory;
import android.os.SystemProperties;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnTouchModeChangeListener;
import android.view.animation.PathInterpolator;
import android.widget.AbsListView.FlingRunnable;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Filter.FilterListener;
import android.widget.RemoteViewsAdapter.RemoteAdapterConnectionCallback;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

public abstract class HwAbsListView extends AbsListView implements TextWatcher, OnGlobalLayoutListener, FilterListener, OnTouchModeChangeListener, RemoteAdapterConnectionCallback {
    protected static final String ANIM_TAG = "listDeleteAnimation";
    private static final boolean DEBUG = false;
    public static final int LISTVIEW_STUB_MASK_HIGH_SPEED_STABLE_ANIMATOR = 4;
    public static final int LISTVIEW_STUB_MASK_SCROLL_MULTI_SELECT = 1;
    public static final int LISTVIEW_STUB_MASK_SPRING_ANIMATOR = 2;
    private static final float MAX_OVER_SCROLLY_DISTANCE_FACTOR = 0.45f;
    private static final float MAX_OVER_SCROLL_SCALE = 0.04f;
    private static final float OVER_SCROLL_SCALE_DELTA = 0.01f;
    private static final int PRESSED_STATE_HWDURATION = 16;
    private static final int SCALE_ITEM_NUM = 4;
    private static final String SWIPELAYOUT = "huawei.android.widget.SwipeLayout";
    private static final String TAG = "AbsListView";
    public static final int TOUCH_MODE_SCROLL_MULTI_SELECT = 7;
    protected int mAnimOffset;
    protected int mAnimatingFirstPos;
    protected int mAnimindex;
    private HashSet<Integer> mCheckedIdOnMove;
    private int[] mChildPositionOnLevel;
    private CheckBox mCurItemView;
    private boolean mFirstChecked;
    private boolean mFirstHasChangedOnMove;
    private boolean mIsHwTheme;
    protected boolean mIsSupportAnim;
    private int mItemHeight;
    private int mLevel;
    protected ValueAnimator mListDeleteAnimator;
    private int mMarkWidthOfCheckedTextView;
    private int mMask;
    protected ArrayList<Object> mVisibleItems;
    private float sX;
    private float sY;

    protected class AnimateAdapterDataSetObserver extends AdapterDataSetObserver {
        protected AnimateAdapterDataSetObserver() {
            super();
        }

        public void onChanged() {
            if (!HwAbsListView.this.startDataChangeAnimation()) {
                onChangedAnimDone();
            }
        }

        public void onChangedAnimDone() {
            HwAbsListView.this.mListDeleteAnimator = null;
            HwAbsListView.this.mAnimOffset = 0;
            super.onChanged();
        }
    }

    private static class LeveledView {
        public int level;
        public String path;
        public View view;

        public LeveledView(View view, int level, String path) {
            this.view = view;
            this.level = level;
            this.path = path;
        }

        public String toString() {
            return "(" + this.path + "):" + this.view;
        }
    }

    public HwAbsListView(Context context) {
        super(context);
        this.mChildPositionOnLevel = new int[20];
        this.mLevel = 0;
        this.mCheckedIdOnMove = new HashSet();
        this.sX = -1.0f;
        this.sY = -1.0f;
        this.mIsSupportAnim = false;
        this.mAnimOffset = 0;
        this.mVisibleItems = new ArrayList();
        this.mIsHwTheme = checkIsHwTheme(context, null);
        initMask(this.mIsHwTheme);
        if (this.mIsHwTheme) {
            setOverScrollMode(0);
        }
    }

    public HwAbsListView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.absListViewStyle);
    }

    public HwAbsListView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HwAbsListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mChildPositionOnLevel = new int[20];
        this.mLevel = 0;
        this.mCheckedIdOnMove = new HashSet();
        this.sX = -1.0f;
        this.sY = -1.0f;
        this.mIsSupportAnim = false;
        this.mAnimOffset = 0;
        this.mVisibleItems = new ArrayList();
        this.mIsHwTheme = checkIsHwTheme(context, attrs);
        initMask(this.mIsHwTheme);
        if (this.mIsHwTheme) {
            setOverScrollMode(0);
        }
        initSpringBackEffect();
    }

    private boolean checkIsHwTheme(Context context, AttributeSet attrs) {
        return HwWidgetFactory.checkIsHwTheme(context, attrs);
    }

    private void initMask(boolean isHwTheme) {
        if (isHwTheme) {
            this.mMask = 7;
        } else {
            this.mMask = 4;
        }
    }

    private void initSpringBackEffect() {
        if (hasSpringAnimatorMask()) {
            setEdgeGlowTopBottom(null, null);
            this.mOverflingDistance = 0;
        }
    }

    public void setOverScrollMode(int mode) {
        super.setOverScrollMode(mode);
        if (hasSpringAnimatorMask()) {
            setEdgeGlowTopBottom(null, null);
        }
    }

    protected boolean getCheckedStateForMultiSelect(boolean curState) {
        if (hasScrollMultiSelectMask()) {
            return getItemCheckedState(curState);
        }
        return curState;
    }

    private boolean getItemCheckedState(boolean curChecked) {
        if (getTouchMode() == 7) {
            return this.mFirstChecked;
        }
        return curChecked;
    }

    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        if (!hasSpringAnimatorMask()) {
            return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
        }
        int newDeltaY = deltaY;
        if (isTouchEvent) {
            newDeltaY = getElasticInterpolation(deltaY, scrollY);
        }
        int maxOverScrollYDistance = (int) (((float) getHeight()) * MAX_OVER_SCROLLY_DISTANCE_FACTOR);
        int absY = Math.abs(scrollY);
        int itemNum = Math.min(4, getChildCount());
        int i;
        float deltaScaleY;
        if (scrollY < 0) {
            for (i = 0; i < itemNum; i++) {
                deltaScaleY = ((MAX_OVER_SCROLL_SCALE - (((float) i) * OVER_SCROLL_SCALE_DELTA)) * ((float) absY)) / ((float) maxOverScrollYDistance);
                getChildAt(i).setScaleX(1.0f - deltaScaleY);
                getChildAt(i).setScaleY(1.0f - deltaScaleY);
            }
        } else {
            for (i = getChildCount() - 1; i >= getChildCount() - itemNum; i--) {
                deltaScaleY = ((MAX_OVER_SCROLL_SCALE - (((float) ((getChildCount() - 1) - i)) * OVER_SCROLL_SCALE_DELTA)) * ((float) absY)) / ((float) maxOverScrollYDistance);
                getChildAt(i).setScaleX(1.0f - deltaScaleY);
                getChildAt(i).setScaleY(1.0f - deltaScaleY);
            }
        }
        invalidate();
        return super.overScrollBy(deltaX, newDeltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollYDistance, isTouchEvent);
    }

    protected void layoutChildren() {
        super.layoutChildren();
        if (getScrollY() == 0) {
            int itemNum = Math.min(4, getChildCount());
            for (int i = 0; i < itemNum; i++) {
                if (getChildAt(i).getScaleX() != 1.0f) {
                    getChildAt(i).setScaleX(1.0f);
                    getChildAt(i).setScaleY(1.0f);
                }
                int index = (getChildCount() - 1) - i;
                if (getChildAt(index).getScaleX() != 1.0f) {
                    getChildAt(index).setScaleX(1.0f);
                    getChildAt(index).setScaleY(1.0f);
                }
            }
        }
    }

    private int getElasticInterpolation(int delta, int currentPos) {
        float len = (float) Math.abs(currentPos);
        int newDeltaY = (int) ((Math.sqrt(((double) (250.0f * ((float) Math.abs(delta)))) + Math.pow((double) len, 2.0d)) - ((double) len)) * ((double) Math.signum((float) delta)));
        if (Math.abs(newDeltaY) > Math.abs(delta)) {
            return delta;
        }
        return newDeltaY;
    }

    protected void dismissCurrentPressed() {
        View child = getChildAt(getMotionPosition() - getFirstVisiblePosition());
        if (child != null) {
            child.setPressed(false);
        }
    }

    protected void enterMultiSelectModeIfNeeded(int motionPosition, int x) {
        if (hasScrollMultiSelectMask() && getTouchMode() != 7 && inCheckableViewOnDown(getChildAt(motionPosition - getFirstVisiblePosition()), x)) {
            setTouchMode(7);
            if (this.mCurItemView != null) {
                this.mFirstChecked = this.mCurItemView.isChecked() ^ 1;
            } else if (getChoiceMode() != 0) {
                this.mFirstChecked = getCheckStates().get(motionPosition, false) ^ 1;
            }
            this.mFirstHasChangedOnMove = false;
            this.mCheckedIdOnMove.clear();
        }
    }

    private boolean inCheckableViewOnDown(View view, int x) {
        boolean z = false;
        if (view instanceof CheckedTextView) {
            if (view.getVisibility() != 0) {
                return false;
            }
            CheckedTextView ctView = (CheckedTextView) view;
            if (ctView.getCheckMarkDrawable() != null) {
                this.mMarkWidthOfCheckedTextView = ctView.getCheckMarkDrawable().getIntrinsicWidth();
                if (x > view.getRight() - this.mMarkWidthOfCheckedTextView && x < view.getRight()) {
                    z = true;
                }
                return z;
            }
        } else if (view instanceof ViewGroup) {
            return searchCheckableView((ViewGroup) view, x);
        }
        return false;
    }

    private boolean searchCheckableView(ViewGroup root, int x) {
        Stack<LeveledView> stack = new Stack();
        String path = "";
        String childPath = "";
        boolean found = false;
        stack.push(new LeveledView(root, 0, "0"));
        while (!stack.empty()) {
            int i;
            LeveledView curView = (LeveledView) stack.pop();
            if (curView.view instanceof ViewGroup) {
                ViewGroup vg = curView.view;
                int level = curView.level + 1;
                path = curView.path;
                if (level > this.mChildPositionOnLevel.length) {
                    break;
                }
                i = 0;
                while (i < vg.getChildCount()) {
                    View childView = vg.getChildAt(i);
                    childPath = path + "," + i;
                    if ((childView instanceof CheckBox) && childView.getVisibility() == 0) {
                        stack.push(new LeveledView(childView, level, path + "," + i));
                        if (inCheckBox(childView, x)) {
                            found = true;
                        }
                    } else {
                        if (childView instanceof ViewGroup) {
                            stack.push(new LeveledView(childView, level, childPath));
                        }
                        i++;
                    }
                }
                continue;
            }
        }
        if (found && (((LeveledView) stack.peek()).view instanceof CheckBox)) {
            LeveledView check = (LeveledView) stack.pop();
            if (!(check.path == null || (check.path.isEmpty() ^ 1) == 0)) {
                String[] leveledPath = check.path.split(",");
                if (leveledPath != null && leveledPath.length <= this.mChildPositionOnLevel.length) {
                    this.mLevel = leveledPath.length;
                    this.mCurItemView = (CheckBox) check.view;
                    i = 0;
                    while (i < leveledPath.length) {
                        try {
                            this.mChildPositionOnLevel[i] = Integer.parseInt(leveledPath[i]);
                            i++;
                        } catch (Exception e) {
                            this.mLevel = 0;
                            this.mCurItemView = null;
                            return false;
                        }
                    }
                }
            }
        }
        boolean z = found && this.mLevel > 0;
        return z;
    }

    protected void onMultiSelectMove(MotionEvent ev, int pointerIndex) {
        if (getTouchMode() == 7 && hasScrollMultiSelectMask()) {
            clickItemIfNeeded(findClosestMotionRow((int) ev.getY(pointerIndex)), (int) ev.getX(pointerIndex));
        }
    }

    private void clickItemIfNeeded(int motionPosition, int x) {
        ListAdapter adapter = getAdapter();
        if (adapter != null && getCount() > 0 && motionPosition != -1 && motionPosition < adapter.getCount()) {
            View view = getChildAt(motionPosition - getFirstVisiblePosition());
            if (view != null && (this.mCheckedIdOnMove.contains(Integer.valueOf(motionPosition)) ^ 1) != 0 && findCheckbleView(view, x)) {
                performItemClickInner(view, motionPosition, adapter.getItemId(motionPosition));
                this.mCheckedIdOnMove.add(Integer.valueOf(motionPosition));
            }
        }
    }

    private boolean performItemClickInner(View view, int position, long id) {
        if (position == getMotionPosition()) {
            this.mFirstHasChangedOnMove = true;
        }
        if (getChoiceMode() != 0) {
            if (this.mFirstChecked != getCheckStates().get(position, false)) {
                return performItemClick(view, position, id);
            }
            return true;
        } else if (this.mCurItemView == null || this.mFirstChecked == this.mCurItemView.isChecked()) {
            return false;
        } else {
            if (this.mCurItemView == null) {
                return true;
            }
            if (this.mCurItemView.getOnCheckedChangeListener() == null) {
                OnItemClickListener listener = getOnItemClickListener();
                if (listener == null) {
                    return true;
                }
                listener.onItemClick(this, view, position, id);
                return true;
            }
            this.mCurItemView.setChecked(this.mFirstChecked);
            return true;
        }
    }

    private boolean findCheckbleView(View view, int x) {
        boolean needResearch = false;
        if (view.getClass().getName().indexOf("ContactListItemView") >= 0) {
            needResearch = true;
        }
        if (needResearch && inCheckableViewOnDown(view, x)) {
            return true;
        }
        if (needResearch) {
            return false;
        }
        return inCheckableViewOnMove(view, x);
    }

    private boolean inCheckableViewOnMove(View view, int x) {
        if (view instanceof CheckedTextView) {
            if (((CheckedTextView) view).getCheckMarkDrawable() != null) {
                boolean z = x > view.getRight() - this.mMarkWidthOfCheckedTextView && x < view.getRight();
                return z;
            }
        } else if (view instanceof ViewGroup) {
            View cbView = getCheckableView(view);
            if (cbView != null) {
                try {
                    this.mCurItemView = (CheckBox) cbView;
                    return inCheckBox(cbView, x);
                } catch (ClassCastException e) {
                    Log.w(TAG, "Judge in checkbox view on move cast fialue.");
                }
            }
        }
        return false;
    }

    private boolean inCheckBox(View cb, int x) {
        int[] location = new int[2];
        cb.getLocationOnScreen(location);
        if (x <= location[0] || x >= location[0] + cb.getWidth()) {
            return false;
        }
        return true;
    }

    private View getCheckableView(View view) {
        if (this.mLevel <= 0) {
            return null;
        }
        View check = view;
        int i = 1;
        while (i < this.mLevel && i < this.mChildPositionOnLevel.length) {
            try {
                check = ((ViewGroup) check).getChildAt(this.mChildPositionOnLevel[i]);
                i++;
            } catch (Exception e) {
                return null;
            }
        }
        return check;
    }

    protected void onTouchUp(MotionEvent ev) {
        if (hasScrollMultiSelectMask() && getTouchMode() == 7 && (eatTouchUpForMultiSelect(ev) ^ 1) != 0) {
            setTouchMode(0);
        }
        super.onTouchUp(ev);
        onMultiSelectUp();
    }

    private boolean eatTouchUpForMultiSelect(MotionEvent ev) {
        if (getTouchMode() != 7 || (pointToPosition((int) ev.getX(), (int) ev.getY()) == getMotionPosition() && !this.mFirstHasChangedOnMove)) {
            return false;
        }
        return true;
    }

    private void onMultiSelectUp() {
        if (hasScrollMultiSelectMask() && getTouchMode() == 7) {
            setTouchMode(-1);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isSwipeLayoutItem() && isWidgetScollHorizontal(ev)) {
            return false;
        }
        if (inMultiSelectMoveMode(ev)) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean isSwipeLayoutItem() {
        int itemPositon = getMotionPosition();
        if (itemPositon <= -1 || itemPositon >= getChildCount() || !getChildAt(itemPositon).getClass().getName().equals(SWIPELAYOUT)) {
            return false;
        }
        return true;
    }

    private boolean isWidgetScollHorizontal(MotionEvent ev) {
        switch (ev.getAction()) {
            case 0:
                this.sX = ev.getRawX();
                this.sY = ev.getRawY();
                break;
            case 2:
                if (((float) Math.toDegrees(Math.atan((double) Math.abs((ev.getRawY() - this.sY) / (ev.getRawX() - this.sX))))) < 30.0f) {
                    return true;
                }
                break;
        }
        return false;
    }

    private boolean inMultiSelectMoveMode(MotionEvent ev) {
        if (hasScrollMultiSelectMask() && getTouchMode() == 7 && ev.getActionMasked() == 2) {
            return true;
        }
        return false;
    }

    protected void setIgnoreScrollMultiSelectStub() {
        this.mMask &= -2;
    }

    protected void enableScrollMultiSelectStub() {
        if ((this.mMask & 1) == 0) {
            this.mMask |= 1;
        }
    }

    protected void setStableItemHeight(OverScroller scroller, FlingRunnable fr) {
        if (hasHighSpeedStableMask()) {
            if (getChildCount() > 2) {
                this.mItemHeight = getChildAt(1).getTop() - getChildAt(0).getTop();
                scroller.getIHwSplineOverScroller().setStableItemHeight(this.mItemHeight);
            }
            if (!scroller.isFinished()) {
                removeCallbacks(fr);
            }
        }
    }

    protected int adjustFlingDistance(int delta) {
        boolean isMore;
        if (delta > 0) {
            int screen = ((getHeight() - this.mPaddingBottom) - this.mPaddingTop) - 1;
            isMore = Math.abs(delta) > Math.abs(screen);
            delta = Math.min(screen, delta);
            if (!isMore || this.mItemHeight <= 0) {
                return delta;
            }
            return (delta / this.mItemHeight) * this.mItemHeight;
        }
        screen = -(((getHeight() - this.mPaddingBottom) - this.mPaddingTop) - 1);
        isMore = Math.abs(delta) > Math.abs(screen);
        delta = Math.max(screen, delta);
        if (!isMore || this.mItemHeight <= 0) {
            return delta;
        }
        return (delta / this.mItemHeight) * this.mItemHeight;
    }

    protected boolean hasScrollMultiSelectMask() {
        return (this.mMask & 1) != 0;
    }

    protected boolean hasSpringAnimatorMask() {
        return (this.mMask & 2) != 0;
    }

    protected boolean hasHighSpeedStableMask() {
        return (this.mMask & 4) != 0;
    }

    protected boolean checkIsEnabled(ListAdapter adapter, int position) {
        return adapter.isEnabled(position);
    }

    protected int getPressedStateDuration() {
        return 16;
    }

    protected void wrapObserver() {
        if (this.mDataSetObserver != null && this.mIsSupportAnim) {
            this.mAdapter.unregisterDataSetObserver(this.mDataSetObserver);
            this.mDataSetObserver = new AnimateAdapterDataSetObserver();
            this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
        }
    }

    protected void onFirstPositionChange() {
        if (this.mListDeleteAnimator == null && (this.mIsSupportAnim ^ 1) == 0) {
            int start = this.mFirstPosition;
            int end = (getChildCount() + start) - 1;
            this.mVisibleItems.clear();
            for (int i = start; i <= end; i++) {
                Object id;
                if (this.mAdapter instanceof ArrayAdapter) {
                    id = this.mAdapter.getItem(i);
                } else {
                    id = Long.valueOf(this.mAdapter.getItemId(i));
                }
                this.mVisibleItems.add(id);
            }
        }
    }

    protected boolean startDataChangeAnimation() {
        if (this.mListDeleteAnimator != null) {
            this.mListDeleteAnimator.cancel();
            this.mListDeleteAnimator = null;
        }
        int olditemcount = this.mItemCount;
        int newItemCount = this.mAdapter.getCount();
        if (olditemcount - newItemCount != 1) {
            Log.w(ANIM_TAG, "ListView::startDataChangeAnimation: not delete 1");
            return false;
        }
        this.mAnimindex = -1;
        this.mAnimOffset = 0;
        this.mAnimatingFirstPos = this.mFirstPosition;
        int start = this.mFirstPosition;
        int childCount = getChildCount();
        int dataCount = this.mVisibleItems.size();
        if (childCount != dataCount) {
            Log.w(ANIM_TAG, "ListView::startDataChangeAnimation count not sync: " + childCount + ", " + dataCount);
            return false;
        }
        int end = (start + childCount) - 1;
        Object id = Integer.valueOf(0);
        int i = start;
        while (i <= end) {
            if (this.mAnimOffset + i < newItemCount) {
                if (this.mAdapter instanceof ArrayAdapter) {
                    id = this.mAdapter.getItem(this.mAnimOffset + i);
                } else {
                    id = Long.valueOf(this.mAdapter.getItemId(this.mAnimOffset + i));
                }
            }
            if (!id.equals(this.mVisibleItems.get(i - this.mFirstPosition)) || this.mAnimOffset + i == newItemCount) {
                if (this.mAnimindex != -1) {
                    Log.w(ANIM_TAG, "ListView::startDataChangeAnimation: error, list view is changed without onFirstPositionChange.");
                    return false;
                }
                this.mAnimindex = i - this.mFirstPosition;
                this.mAnimOffset--;
            }
            i++;
        }
        if (this.mAnimindex == -1) {
            Log.w(ANIM_TAG, "ListView::startDataChangeAnimation no visible item is deleted ");
            return false;
        }
        this.mListDeleteAnimator = ValueAnimator.ofInt(new int[]{100, 0});
        this.mListDeleteAnimator.setInterpolator(new PathInterpolator(0.3f, 0.15f, 0.1f, 0.85f));
        int duration = SystemProperties.getInt("durationList", 200);
        this.mListDeleteAnimator.setDuration((long) duration);
        View v = getChildAt(this.mAnimindex);
        final int height0 = v.getHeight();
        final View view = v;
        this.mListDeleteAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                if (HwAbsListView.this.mFirstPosition != HwAbsListView.this.mAnimatingFirstPos) {
                    Log.w(HwAbsListView.ANIM_TAG, "ListView::onAnimationUpdate: cancel pos is changed" + HwAbsListView.this.mAnimatingFirstPos + ", " + HwAbsListView.this.mFirstPosition);
                    animation.cancel();
                    return;
                }
                Integer value = (Integer) animation.getAnimatedValue();
                LayoutParams lp = view.getLayoutParams();
                lp.height = (int) (((float) height0) * (((float) value.intValue()) / 100.0f));
                if (lp.height == 0) {
                    lp.height = 1;
                }
                view.setLayoutParams(lp);
            }
        });
        view = v;
        this.mListDeleteAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                HwAbsListView.this.mAnimOffset = 0;
                HwAbsListView.this.mListDeleteAnimator = null;
                HwAbsListView.this.mAnimindex = 0;
                LayoutParams lp = view.getLayoutParams();
                lp.height = 0;
                view.setLayoutParams(lp);
                HwAbsListView.this.mAnimatingFirstPos = 0;
                if (HwAbsListView.this.mDataSetObserver instanceof AnimateAdapterDataSetObserver) {
                    ((AnimateAdapterDataSetObserver) HwAbsListView.this.mDataSetObserver).onChangedAnimDone();
                }
            }
        });
        this.mListDeleteAnimator.start();
        return true;
    }

    View obtainView(int position, boolean[] outMetadata) {
        if (this.mIsSupportAnim && this.mAnimOffset != 0 && position > this.mAnimindex + this.mAnimatingFirstPos) {
            position += this.mAnimOffset;
        }
        return super.obtainView(position, outMetadata);
    }
}
