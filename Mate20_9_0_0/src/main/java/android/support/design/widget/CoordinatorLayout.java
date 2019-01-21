package android.support.design.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.ClassLoaderCreator;
import android.os.Parcelable.Creator;
import android.os.SystemClock;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.annotation.VisibleForTesting;
import android.support.coordinatorlayout.R;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.ObjectsCompat;
import android.support.v4.util.Pools.Pool;
import android.support.v4.util.Pools.SynchronizedPool;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.NestedScrollingParent2;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.DirectedAcyclicGraph;
import android.support.v4.widget.ViewGroupUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewGroup.OnHierarchyChangeListener;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoordinatorLayout extends ViewGroup implements NestedScrollingParent2 {
    static final Class<?>[] CONSTRUCTOR_PARAMS = new Class[]{Context.class, AttributeSet.class};
    static final int EVENT_NESTED_SCROLL = 1;
    static final int EVENT_PRE_DRAW = 0;
    static final int EVENT_VIEW_REMOVED = 2;
    static final String TAG = "CoordinatorLayout";
    static final Comparator<View> TOP_SORTED_CHILDREN_COMPARATOR;
    private static final int TYPE_ON_INTERCEPT = 0;
    private static final int TYPE_ON_TOUCH = 1;
    static final String WIDGET_PACKAGE_NAME;
    static final ThreadLocal<Map<String, Constructor<Behavior>>> sConstructors = new ThreadLocal();
    private static final Pool<Rect> sRectPool = new SynchronizedPool(12);
    private OnApplyWindowInsetsListener mApplyWindowInsetsListener;
    private View mBehaviorTouchView;
    private final DirectedAcyclicGraph<View> mChildDag;
    private final List<View> mDependencySortedChildren;
    private boolean mDisallowInterceptReset;
    private boolean mDrawStatusBarBackground;
    private boolean mIsAttachedToWindow;
    private int[] mKeylines;
    private WindowInsetsCompat mLastInsets;
    private boolean mNeedsPreDrawListener;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private View mNestedScrollingTarget;
    OnHierarchyChangeListener mOnHierarchyChangeListener;
    private OnPreDrawListener mOnPreDrawListener;
    private Paint mScrimPaint;
    private Drawable mStatusBarBackground;
    private final List<View> mTempDependenciesList;
    private final int[] mTempIntPair;
    private final List<View> mTempList1;

    public interface AttachedBehavior {
        @NonNull
        Behavior getBehavior();
    }

    public static abstract class Behavior<V extends View> {
        public Behavior(Context context, AttributeSet attrs) {
        }

        public void onAttachedToLayoutParams(@NonNull LayoutParams params) {
        }

        public void onDetachedFromLayoutParams() {
        }

        public boolean onInterceptTouchEvent(@NonNull CoordinatorLayout parent, @NonNull V v, @NonNull MotionEvent ev) {
            return false;
        }

        public boolean onTouchEvent(@NonNull CoordinatorLayout parent, @NonNull V v, @NonNull MotionEvent ev) {
            return false;
        }

        @ColorInt
        public int getScrimColor(@NonNull CoordinatorLayout parent, @NonNull V v) {
            return ViewCompat.MEASURED_STATE_MASK;
        }

        @FloatRange(from = 0.0d, to = 1.0d)
        public float getScrimOpacity(@NonNull CoordinatorLayout parent, @NonNull V v) {
            return 0.0f;
        }

        public boolean blocksInteractionBelow(@NonNull CoordinatorLayout parent, @NonNull V child) {
            return getScrimOpacity(parent, child) > 0.0f;
        }

        public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull V v, @NonNull View dependency) {
            return false;
        }

        public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull V v, @NonNull View dependency) {
            return false;
        }

        public void onDependentViewRemoved(@NonNull CoordinatorLayout parent, @NonNull V v, @NonNull View dependency) {
        }

        public boolean onMeasureChild(@NonNull CoordinatorLayout parent, @NonNull V v, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
            return false;
        }

        public boolean onLayoutChild(@NonNull CoordinatorLayout parent, @NonNull V v, int layoutDirection) {
            return false;
        }

        public static void setTag(@NonNull View child, @Nullable Object tag) {
            ((LayoutParams) child.getLayoutParams()).mBehaviorTag = tag;
        }

        @Nullable
        public static Object getTag(@NonNull View child) {
            return ((LayoutParams) child.getLayoutParams()).mBehaviorTag;
        }

        @Deprecated
        public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V v, @NonNull View directTargetChild, @NonNull View target, int axes) {
            return false;
        }

        public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
            if (type == 0) {
                return onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes);
            }
            return false;
        }

        @Deprecated
        public void onNestedScrollAccepted(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V v, @NonNull View directTargetChild, @NonNull View target, int axes) {
        }

        public void onNestedScrollAccepted(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
            if (type == 0) {
                onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, axes);
            }
        }

        @Deprecated
        public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V v, @NonNull View target) {
        }

        public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int type) {
            if (type == 0) {
                onStopNestedScroll(coordinatorLayout, child, target);
            }
        }

        @Deprecated
        public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V v, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        }

        public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
            if (type == 0) {
                onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
            }
        }

        @Deprecated
        public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V v, @NonNull View target, int dx, int dy, @NonNull int[] consumed) {
        }

        public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
            if (type == 0) {
                onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
            }
        }

        public boolean onNestedFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V v, @NonNull View target, float velocityX, float velocityY, boolean consumed) {
            return false;
        }

        public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V v, @NonNull View target, float velocityX, float velocityY) {
            return false;
        }

        @NonNull
        public WindowInsetsCompat onApplyWindowInsets(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V v, @NonNull WindowInsetsCompat insets) {
            return insets;
        }

        public boolean onRequestChildRectangleOnScreen(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V v, @NonNull Rect rectangle, boolean immediate) {
            return false;
        }

        public void onRestoreInstanceState(@NonNull CoordinatorLayout parent, @NonNull V v, @NonNull Parcelable state) {
        }

        @Nullable
        public Parcelable onSaveInstanceState(@NonNull CoordinatorLayout parent, @NonNull V v) {
            return BaseSavedState.EMPTY_STATE;
        }

        public boolean getInsetDodgeRect(@NonNull CoordinatorLayout parent, @NonNull V v, @NonNull Rect rect) {
            return false;
        }
    }

    @Deprecated
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultBehavior {
        Class<? extends Behavior> value();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DispatchChangeEvent {
    }

    private class HierarchyChangeListener implements OnHierarchyChangeListener {
        HierarchyChangeListener() {
        }

        public void onChildViewAdded(View parent, View child) {
            if (CoordinatorLayout.this.mOnHierarchyChangeListener != null) {
                CoordinatorLayout.this.mOnHierarchyChangeListener.onChildViewAdded(parent, child);
            }
        }

        public void onChildViewRemoved(View parent, View child) {
            CoordinatorLayout.this.onChildViewsChanged(2);
            if (CoordinatorLayout.this.mOnHierarchyChangeListener != null) {
                CoordinatorLayout.this.mOnHierarchyChangeListener.onChildViewRemoved(parent, child);
            }
        }
    }

    public static class LayoutParams extends MarginLayoutParams {
        public int anchorGravity = 0;
        public int dodgeInsetEdges = 0;
        public int gravity = 0;
        public int insetEdge = 0;
        public int keyline = -1;
        View mAnchorDirectChild;
        int mAnchorId = -1;
        View mAnchorView;
        Behavior mBehavior;
        boolean mBehaviorResolved = false;
        Object mBehaviorTag;
        private boolean mDidAcceptNestedScrollNonTouch;
        private boolean mDidAcceptNestedScrollTouch;
        private boolean mDidBlockInteraction;
        private boolean mDidChangeAfterNestedScroll;
        int mInsetOffsetX;
        int mInsetOffsetY;
        final Rect mLastChildRect = new Rect();

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        LayoutParams(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CoordinatorLayout_Layout);
            this.gravity = a.getInteger(R.styleable.CoordinatorLayout_Layout_android_layout_gravity, 0);
            this.mAnchorId = a.getResourceId(R.styleable.CoordinatorLayout_Layout_layout_anchor, -1);
            this.anchorGravity = a.getInteger(R.styleable.CoordinatorLayout_Layout_layout_anchorGravity, 0);
            this.keyline = a.getInteger(R.styleable.CoordinatorLayout_Layout_layout_keyline, -1);
            this.insetEdge = a.getInt(R.styleable.CoordinatorLayout_Layout_layout_insetEdge, 0);
            this.dodgeInsetEdges = a.getInt(R.styleable.CoordinatorLayout_Layout_layout_dodgeInsetEdges, 0);
            this.mBehaviorResolved = a.hasValue(R.styleable.CoordinatorLayout_Layout_layout_behavior);
            if (this.mBehaviorResolved) {
                this.mBehavior = CoordinatorLayout.parseBehavior(context, attrs, a.getString(R.styleable.CoordinatorLayout_Layout_layout_behavior));
            }
            a.recycle();
            if (this.mBehavior != null) {
                this.mBehavior.onAttachedToLayoutParams(this);
            }
        }

        public LayoutParams(LayoutParams p) {
            super(p);
        }

        public LayoutParams(MarginLayoutParams p) {
            super(p);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams p) {
            super(p);
        }

        @IdRes
        public int getAnchorId() {
            return this.mAnchorId;
        }

        public void setAnchorId(@IdRes int id) {
            invalidateAnchor();
            this.mAnchorId = id;
        }

        @Nullable
        public Behavior getBehavior() {
            return this.mBehavior;
        }

        public void setBehavior(@Nullable Behavior behavior) {
            if (this.mBehavior != behavior) {
                if (this.mBehavior != null) {
                    this.mBehavior.onDetachedFromLayoutParams();
                }
                this.mBehavior = behavior;
                this.mBehaviorTag = null;
                this.mBehaviorResolved = true;
                if (behavior != null) {
                    behavior.onAttachedToLayoutParams(this);
                }
            }
        }

        void setLastChildRect(Rect r) {
            this.mLastChildRect.set(r);
        }

        Rect getLastChildRect() {
            return this.mLastChildRect;
        }

        boolean checkAnchorChanged() {
            return this.mAnchorView == null && this.mAnchorId != -1;
        }

        boolean didBlockInteraction() {
            if (this.mBehavior == null) {
                this.mDidBlockInteraction = false;
            }
            return this.mDidBlockInteraction;
        }

        boolean isBlockingInteractionBelow(CoordinatorLayout parent, View child) {
            if (this.mDidBlockInteraction) {
                return true;
            }
            int blocksInteractionBelow = this.mDidBlockInteraction | (this.mBehavior != null ? this.mBehavior.blocksInteractionBelow(parent, child) : 0);
            this.mDidBlockInteraction = blocksInteractionBelow;
            return blocksInteractionBelow;
        }

        void resetTouchBehaviorTracking() {
            this.mDidBlockInteraction = false;
        }

        void resetNestedScroll(int type) {
            setNestedScrollAccepted(type, false);
        }

        void setNestedScrollAccepted(int type, boolean accept) {
            switch (type) {
                case 0:
                    this.mDidAcceptNestedScrollTouch = accept;
                    return;
                case 1:
                    this.mDidAcceptNestedScrollNonTouch = accept;
                    return;
                default:
                    return;
            }
        }

        boolean isNestedScrollAccepted(int type) {
            switch (type) {
                case 0:
                    return this.mDidAcceptNestedScrollTouch;
                case 1:
                    return this.mDidAcceptNestedScrollNonTouch;
                default:
                    return false;
            }
        }

        boolean getChangedAfterNestedScroll() {
            return this.mDidChangeAfterNestedScroll;
        }

        void setChangedAfterNestedScroll(boolean changed) {
            this.mDidChangeAfterNestedScroll = changed;
        }

        void resetChangedAfterNestedScroll() {
            this.mDidChangeAfterNestedScroll = false;
        }

        boolean dependsOn(CoordinatorLayout parent, View child, View dependency) {
            return dependency == this.mAnchorDirectChild || shouldDodge(dependency, ViewCompat.getLayoutDirection(parent)) || (this.mBehavior != null && this.mBehavior.layoutDependsOn(parent, child, dependency));
        }

        void invalidateAnchor() {
            this.mAnchorDirectChild = null;
            this.mAnchorView = null;
        }

        View findAnchorView(CoordinatorLayout parent, View forChild) {
            if (this.mAnchorId == -1) {
                this.mAnchorDirectChild = null;
                this.mAnchorView = null;
                return null;
            }
            if (this.mAnchorView == null || !verifyAnchorView(forChild, parent)) {
                resolveAnchorView(forChild, parent);
            }
            return this.mAnchorView;
        }

        private void resolveAnchorView(View forChild, CoordinatorLayout parent) {
            this.mAnchorView = parent.findViewById(this.mAnchorId);
            if (this.mAnchorView != null) {
                if (this.mAnchorView != parent) {
                    View directChild = this.mAnchorView;
                    View p = this.mAnchorView.getParent();
                    while (p != parent && p != null) {
                        if (p != forChild) {
                            if (p instanceof View) {
                                directChild = p;
                            }
                            p = p.getParent();
                        } else if (parent.isInEditMode()) {
                            this.mAnchorDirectChild = null;
                            this.mAnchorView = null;
                            return;
                        } else {
                            throw new IllegalStateException("Anchor must not be a descendant of the anchored view");
                        }
                    }
                    this.mAnchorDirectChild = directChild;
                } else if (parent.isInEditMode()) {
                    this.mAnchorDirectChild = null;
                    this.mAnchorView = null;
                } else {
                    throw new IllegalStateException("View can not be anchored to the the parent CoordinatorLayout");
                }
            } else if (parent.isInEditMode()) {
                this.mAnchorDirectChild = null;
                this.mAnchorView = null;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not find CoordinatorLayout descendant view with id ");
                stringBuilder.append(parent.getResources().getResourceName(this.mAnchorId));
                stringBuilder.append(" to anchor view ");
                stringBuilder.append(forChild);
                throw new IllegalStateException(stringBuilder.toString());
            }
        }

        private boolean verifyAnchorView(View forChild, CoordinatorLayout parent) {
            if (this.mAnchorView.getId() != this.mAnchorId) {
                return false;
            }
            View directChild = this.mAnchorView;
            View p = this.mAnchorView.getParent();
            while (p != parent) {
                if (p == null || p == forChild) {
                    this.mAnchorDirectChild = null;
                    this.mAnchorView = null;
                    return false;
                }
                if (p instanceof View) {
                    directChild = p;
                }
                p = p.getParent();
            }
            this.mAnchorDirectChild = directChild;
            return true;
        }

        private boolean shouldDodge(View other, int layoutDirection) {
            int absInset = GravityCompat.getAbsoluteGravity(((LayoutParams) other.getLayoutParams()).insetEdge, layoutDirection);
            return absInset != 0 && (GravityCompat.getAbsoluteGravity(this.dodgeInsetEdges, layoutDirection) & absInset) == absInset;
        }
    }

    class OnPreDrawListener implements android.view.ViewTreeObserver.OnPreDrawListener {
        OnPreDrawListener() {
        }

        public boolean onPreDraw() {
            CoordinatorLayout.this.onChildViewsChanged(0);
            return true;
        }
    }

    static class ViewElevationComparator implements Comparator<View> {
        ViewElevationComparator() {
        }

        public int compare(View lhs, View rhs) {
            float lz = ViewCompat.getZ(lhs);
            float rz = ViewCompat.getZ(rhs);
            if (lz > rz) {
                return -1;
            }
            if (lz < rz) {
                return 1;
            }
            return 0;
        }
    }

    protected static class SavedState extends AbsSavedState {
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
        SparseArray<Parcelable> behaviorStates;

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            int size = source.readInt();
            int[] ids = new int[size];
            source.readIntArray(ids);
            Parcelable[] states = source.readParcelableArray(loader);
            this.behaviorStates = new SparseArray(size);
            for (int i = 0; i < size; i++) {
                this.behaviorStates.append(ids[i], states[i]);
            }
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            int i = 0;
            int size = this.behaviorStates != null ? this.behaviorStates.size() : 0;
            dest.writeInt(size);
            int[] ids = new int[size];
            Parcelable[] states = new Parcelable[size];
            while (i < size) {
                ids[i] = this.behaviorStates.keyAt(i);
                states[i] = (Parcelable) this.behaviorStates.valueAt(i);
                i++;
            }
            dest.writeIntArray(ids);
            dest.writeParcelableArray(states, flags);
        }
    }

    static {
        Package pkg = CoordinatorLayout.class.getPackage();
        WIDGET_PACKAGE_NAME = pkg != null ? pkg.getName() : null;
        if (VERSION.SDK_INT >= 21) {
            TOP_SORTED_CHILDREN_COMPARATOR = new ViewElevationComparator();
        } else {
            TOP_SORTED_CHILDREN_COMPARATOR = null;
        }
    }

    @NonNull
    private static Rect acquireTempRect() {
        Rect rect = (Rect) sRectPool.acquire();
        if (rect == null) {
            return new Rect();
        }
        return rect;
    }

    private static void releaseTempRect(@NonNull Rect rect) {
        rect.setEmpty();
        sRectPool.release(rect);
    }

    public CoordinatorLayout(@NonNull Context context) {
        this(context, null);
    }

    public CoordinatorLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.coordinatorLayoutStyle);
    }

    public CoordinatorLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        TypedArray a;
        super(context, attrs, defStyleAttr);
        this.mDependencySortedChildren = new ArrayList();
        this.mChildDag = new DirectedAcyclicGraph();
        this.mTempList1 = new ArrayList();
        this.mTempDependenciesList = new ArrayList();
        this.mTempIntPair = new int[2];
        this.mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        int i = 0;
        if (defStyleAttr == 0) {
            a = context.obtainStyledAttributes(attrs, R.styleable.CoordinatorLayout, 0, R.style.Widget_Support_CoordinatorLayout);
        } else {
            a = context.obtainStyledAttributes(attrs, R.styleable.CoordinatorLayout, defStyleAttr, 0);
        }
        int keylineArrayRes = a.getResourceId(R.styleable.CoordinatorLayout_keylines, 0);
        if (keylineArrayRes != 0) {
            Resources res = context.getResources();
            this.mKeylines = res.getIntArray(keylineArrayRes);
            float density = res.getDisplayMetrics().density;
            int count = this.mKeylines.length;
            while (i < count) {
                this.mKeylines[i] = (int) (((float) this.mKeylines[i]) * density);
                i++;
            }
        }
        this.mStatusBarBackground = a.getDrawable(R.styleable.CoordinatorLayout_statusBarBackground);
        a.recycle();
        setupForInsets();
        super.setOnHierarchyChangeListener(new HierarchyChangeListener());
    }

    public void setOnHierarchyChangeListener(OnHierarchyChangeListener onHierarchyChangeListener) {
        this.mOnHierarchyChangeListener = onHierarchyChangeListener;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        resetTouchBehaviors(false);
        if (this.mNeedsPreDrawListener) {
            if (this.mOnPreDrawListener == null) {
                this.mOnPreDrawListener = new OnPreDrawListener();
            }
            getViewTreeObserver().addOnPreDrawListener(this.mOnPreDrawListener);
        }
        if (this.mLastInsets == null && ViewCompat.getFitsSystemWindows(this)) {
            ViewCompat.requestApplyInsets(this);
        }
        this.mIsAttachedToWindow = true;
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        resetTouchBehaviors(false);
        if (this.mNeedsPreDrawListener && this.mOnPreDrawListener != null) {
            getViewTreeObserver().removeOnPreDrawListener(this.mOnPreDrawListener);
        }
        if (this.mNestedScrollingTarget != null) {
            onStopNestedScroll(this.mNestedScrollingTarget);
        }
        this.mIsAttachedToWindow = false;
    }

    public void setStatusBarBackground(@Nullable Drawable bg) {
        if (this.mStatusBarBackground != bg) {
            Drawable drawable = null;
            if (this.mStatusBarBackground != null) {
                this.mStatusBarBackground.setCallback(null);
            }
            if (bg != null) {
                drawable = bg.mutate();
            }
            this.mStatusBarBackground = drawable;
            if (this.mStatusBarBackground != null) {
                if (this.mStatusBarBackground.isStateful()) {
                    this.mStatusBarBackground.setState(getDrawableState());
                }
                DrawableCompat.setLayoutDirection(this.mStatusBarBackground, ViewCompat.getLayoutDirection(this));
                this.mStatusBarBackground.setVisible(getVisibility() == 0, false);
                this.mStatusBarBackground.setCallback(this);
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Nullable
    public Drawable getStatusBarBackground() {
        return this.mStatusBarBackground;
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int[] state = getDrawableState();
        boolean changed = false;
        Drawable d = this.mStatusBarBackground;
        if (d != null && d.isStateful()) {
            changed = false | d.setState(state);
        }
        if (changed) {
            invalidate();
        }
    }

    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mStatusBarBackground;
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        boolean visible = visibility == 0;
        if (this.mStatusBarBackground != null && this.mStatusBarBackground.isVisible() != visible) {
            this.mStatusBarBackground.setVisible(visible, false);
        }
    }

    public void setStatusBarBackgroundResource(@DrawableRes int resId) {
        setStatusBarBackground(resId != 0 ? ContextCompat.getDrawable(getContext(), resId) : null);
    }

    public void setStatusBarBackgroundColor(@ColorInt int color) {
        setStatusBarBackground(new ColorDrawable(color));
    }

    final WindowInsetsCompat setWindowInsets(WindowInsetsCompat insets) {
        if (ObjectsCompat.equals(this.mLastInsets, insets)) {
            return insets;
        }
        this.mLastInsets = insets;
        boolean z = false;
        boolean z2 = insets != null && insets.getSystemWindowInsetTop() > 0;
        this.mDrawStatusBarBackground = z2;
        if (!this.mDrawStatusBarBackground && getBackground() == null) {
            z = true;
        }
        setWillNotDraw(z);
        insets = dispatchApplyWindowInsetsToBehaviors(insets);
        requestLayout();
        return insets;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public final WindowInsetsCompat getLastWindowInsets() {
        return this.mLastInsets;
    }

    private void resetTouchBehaviors(boolean notifyOnInterceptTouchEvent) {
        int i;
        int childCount = getChildCount();
        for (i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            Behavior b = ((LayoutParams) child.getLayoutParams()).getBehavior();
            if (b != null) {
                long now = SystemClock.uptimeMillis();
                MotionEvent cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
                if (notifyOnInterceptTouchEvent) {
                    b.onInterceptTouchEvent(this, child, cancelEvent);
                } else {
                    b.onTouchEvent(this, child, cancelEvent);
                }
                cancelEvent.recycle();
            }
        }
        for (i = 0; i < childCount; i++) {
            ((LayoutParams) getChildAt(i).getLayoutParams()).resetTouchBehaviorTracking();
        }
        this.mBehaviorTouchView = null;
        this.mDisallowInterceptReset = false;
    }

    private void getTopSortedChildren(List<View> out) {
        out.clear();
        boolean useCustomOrder = isChildrenDrawingOrderEnabled();
        int childCount = getChildCount();
        int i = childCount - 1;
        while (i >= 0) {
            out.add(getChildAt(useCustomOrder ? getChildDrawingOrder(childCount, i) : i));
            i--;
        }
        if (TOP_SORTED_CHILDREN_COMPARATOR != null) {
            Collections.sort(out, TOP_SORTED_CHILDREN_COMPARATOR);
        }
    }

    private boolean performIntercept(MotionEvent ev, int type) {
        MotionEvent motionEvent = ev;
        int action = ev.getActionMasked();
        List<View> topmostChildList = this.mTempList1;
        getTopSortedChildren(topmostChildList);
        int childCount = topmostChildList.size();
        MotionEvent cancelEvent = null;
        boolean newBlock = false;
        boolean intercepted = false;
        for (int i = 0; i < childCount; i++) {
            View child = (View) topmostChildList.get(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior b = lp.getBehavior();
            if ((intercepted || newBlock) && action != 0) {
                if (b != null) {
                    if (cancelEvent == null) {
                        long now = SystemClock.uptimeMillis();
                        cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
                    }
                    switch (type) {
                        case 0:
                            b.onInterceptTouchEvent(this, child, cancelEvent);
                            break;
                        case 1:
                            b.onTouchEvent(this, child, cancelEvent);
                            break;
                        default:
                            break;
                    }
                }
            } else {
                if (!(intercepted || b == null)) {
                    switch (type) {
                        case 0:
                            intercepted = b.onInterceptTouchEvent(this, child, motionEvent);
                            break;
                        case 1:
                            intercepted = b.onTouchEvent(this, child, motionEvent);
                            break;
                    }
                    if (intercepted) {
                        this.mBehaviorTouchView = child;
                    }
                }
                boolean wasBlocking = lp.didBlockInteraction();
                boolean isBlocking = lp.isBlockingInteractionBelow(this, child);
                boolean z = isBlocking && !wasBlocking;
                newBlock = z;
                if (isBlocking && !newBlock) {
                    topmostChildList.clear();
                    return intercepted;
                }
            }
        }
        topmostChildList.clear();
        return intercepted;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == 0) {
            resetTouchBehaviors(true);
        }
        boolean intercepted = performIntercept(ev, false);
        if (action == 1 || action == 3) {
            resetTouchBehaviors(true);
        }
        return intercepted;
    }

    /* JADX WARNING: Missing block: B:3:0x0015, code skipped:
            if (r6 != false) goto L_0x0017;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTouchEvent(MotionEvent ev) {
        MotionEvent motionEvent = ev;
        boolean handled = false;
        boolean cancelSuper = false;
        MotionEvent cancelEvent = null;
        int action = ev.getActionMasked();
        if (this.mBehaviorTouchView == null) {
            boolean performIntercept = performIntercept(motionEvent, 1);
            cancelSuper = performIntercept;
        }
        Behavior b = ((LayoutParams) this.mBehaviorTouchView.getLayoutParams()).getBehavior();
        if (b != null) {
            handled = b.onTouchEvent(this, this.mBehaviorTouchView, motionEvent);
        }
        if (this.mBehaviorTouchView == null) {
            handled |= super.onTouchEvent(ev);
        } else if (cancelSuper) {
            if (null == null) {
                long now = SystemClock.uptimeMillis();
                cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
            }
            super.onTouchEvent(cancelEvent);
        }
        if (cancelEvent != null) {
            cancelEvent.recycle();
        }
        if (action == 1 || action == 3) {
            resetTouchBehaviors(false);
        }
        return handled;
    }

    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        if (disallowIntercept && !this.mDisallowInterceptReset) {
            resetTouchBehaviors(false);
            this.mDisallowInterceptReset = true;
        }
    }

    private int getKeyline(int index) {
        String str;
        StringBuilder stringBuilder;
        if (this.mKeylines == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("No keylines defined for ");
            stringBuilder.append(this);
            stringBuilder.append(" - attempted index lookup ");
            stringBuilder.append(index);
            Log.e(str, stringBuilder.toString());
            return 0;
        } else if (index >= 0 && index < this.mKeylines.length) {
            return this.mKeylines[index];
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Keyline index ");
            stringBuilder.append(index);
            stringBuilder.append(" out of range for ");
            stringBuilder.append(this);
            Log.e(str, stringBuilder.toString());
            return 0;
        }
    }

    static Behavior parseBehavior(Context context, AttributeSet attrs, String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        String fullName;
        if (name.startsWith(".")) {
            fullName = new StringBuilder();
            fullName.append(context.getPackageName());
            fullName.append(name);
            fullName = fullName.toString();
        } else if (name.indexOf(46) >= 0) {
            fullName = name;
        } else if (TextUtils.isEmpty(WIDGET_PACKAGE_NAME)) {
            fullName = name;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(WIDGET_PACKAGE_NAME);
            stringBuilder.append('.');
            stringBuilder.append(name);
            fullName = stringBuilder.toString();
        }
        try {
            Map<String, Constructor<Behavior>> constructors = (Map) sConstructors.get();
            if (constructors == null) {
                constructors = new HashMap();
                sConstructors.set(constructors);
            }
            Constructor<Behavior> c = (Constructor) constructors.get(fullName);
            if (c == null) {
                c = context.getClassLoader().loadClass(fullName).getConstructor(CONSTRUCTOR_PARAMS);
                c.setAccessible(true);
                constructors.put(fullName, c);
            }
            return (Behavior) c.newInstance(new Object[]{context, attrs});
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Could not inflate Behavior subclass ");
            stringBuilder2.append(fullName);
            throw new RuntimeException(stringBuilder2.toString(), e);
        }
    }

    LayoutParams getResolvedLayoutParams(View child) {
        LayoutParams result = (LayoutParams) child.getLayoutParams();
        if (!result.mBehaviorResolved) {
            if (child instanceof AttachedBehavior) {
                Behavior attachedBehavior = ((AttachedBehavior) child).getBehavior();
                if (attachedBehavior == null) {
                    Log.e(TAG, "Attached behavior class is null");
                }
                result.setBehavior(attachedBehavior);
                result.mBehaviorResolved = true;
            } else {
                DefaultBehavior defaultBehavior = null;
                for (Class<?> childClass = child.getClass(); childClass != null; childClass = childClass.getSuperclass()) {
                    DefaultBehavior defaultBehavior2 = (DefaultBehavior) childClass.getAnnotation(DefaultBehavior.class);
                    defaultBehavior = defaultBehavior2;
                    if (defaultBehavior2 != null) {
                        break;
                    }
                }
                if (defaultBehavior != null) {
                    try {
                        result.setBehavior((Behavior) defaultBehavior.value().getDeclaredConstructor(new Class[0]).newInstance(new Object[0]));
                    } catch (Exception e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Default behavior class ");
                        stringBuilder.append(defaultBehavior.value().getName());
                        stringBuilder.append(" could not be instantiated. Did you forget");
                        stringBuilder.append(" a default constructor?");
                        Log.e(str, stringBuilder.toString(), e);
                    }
                }
                result.mBehaviorResolved = true;
            }
        }
        return result;
    }

    private void prepareChildren() {
        this.mDependencySortedChildren.clear();
        this.mChildDag.clear();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            LayoutParams lp = getResolvedLayoutParams(view);
            lp.findAnchorView(this, view);
            this.mChildDag.addNode(view);
            for (int j = 0; j < count; j++) {
                if (j != i) {
                    View other = getChildAt(j);
                    if (lp.dependsOn(this, view, other)) {
                        if (!this.mChildDag.contains(other)) {
                            this.mChildDag.addNode(other);
                        }
                        this.mChildDag.addEdge(other, view);
                    }
                }
            }
        }
        this.mDependencySortedChildren.addAll(this.mChildDag.getSortedList());
        Collections.reverse(this.mDependencySortedChildren);
    }

    void getDescendantRect(View descendant, Rect out) {
        ViewGroupUtils.getDescendantRect(this, descendant, out);
    }

    protected int getSuggestedMinimumWidth() {
        return Math.max(super.getSuggestedMinimumWidth(), getPaddingLeft() + getPaddingRight());
    }

    protected int getSuggestedMinimumHeight() {
        return Math.max(super.getSuggestedMinimumHeight(), getPaddingTop() + getPaddingBottom());
    }

    public void onMeasureChild(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
    }

    /* JADX WARNING: Missing block: B:43:0x0138, code skipped:
            if (r29.onMeasureChild(r7, r4, r21, r23, r26, 0) == false) goto L_0x0149;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        prepareChildren();
        ensurePreDrawListener();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        boolean z = true;
        boolean isRtl = layoutDirection == 1;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthPadding = paddingLeft + paddingRight;
        int heightPadding = paddingTop + paddingBottom;
        int widthUsed = getSuggestedMinimumWidth();
        int heightUsed = getSuggestedMinimumHeight();
        int childState = 0;
        if (this.mLastInsets == null || !ViewCompat.getFitsSystemWindows(this)) {
            z = false;
        }
        boolean applyInsets = z;
        int childCount = this.mDependencySortedChildren.size();
        int widthUsed2 = widthUsed;
        int i = 0;
        while (true) {
            widthUsed = i;
            int heightMode2;
            if (widthUsed < childCount) {
                int childCount2;
                View child = (View) this.mDependencySortedChildren.get(widthUsed);
                int i2 = widthUsed;
                if (child.getVisibility() == 8) {
                    childCount2 = childCount;
                    heightMode2 = heightMode;
                } else {
                    int heightUsed2;
                    View child2;
                    int childWidthMeasureSpec;
                    int childHeightMeasureSpec;
                    int heightUsed3;
                    int childState2;
                    View child3;
                    int widthUsed3;
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    int keylineWidthUsed = 0;
                    if (lp.keyline < 0 || widthMode == 0) {
                        heightUsed2 = heightUsed;
                        childCount2 = childState;
                        heightMode2 = widthUsed2;
                    } else {
                        widthUsed = getKeyline(lp.keyline);
                        heightUsed2 = heightUsed;
                        heightUsed = GravityCompat.getAbsoluteGravity(resolveKeylineGravity(lp.gravity), layoutDirection) & 7;
                        childCount2 = childState;
                        if ((heightUsed != 3 || isRtl) && !(heightUsed == 5 && isRtl)) {
                            heightMode2 = widthUsed2;
                            if ((heightUsed == 5 && !isRtl) || (heightUsed == 3 && isRtl)) {
                                keylineWidthUsed = Math.max(0, widthUsed - paddingLeft);
                            }
                        } else {
                            heightMode2 = widthUsed2;
                            keylineWidthUsed = Math.max(0, (widthSize - paddingRight) - widthUsed);
                        }
                    }
                    widthUsed = widthMeasureSpec;
                    heightUsed = heightMeasureSpec;
                    if (!applyInsets || ViewCompat.getFitsSystemWindows(child)) {
                        child2 = child;
                        childWidthMeasureSpec = widthUsed;
                        childHeightMeasureSpec = heightUsed;
                    } else {
                        child2 = child;
                        widthUsed2 = this.mLastInsets.getSystemWindowInsetTop() + this.mLastInsets.getSystemWindowInsetBottom();
                        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize - (this.mLastInsets.getSystemWindowInsetLeft() + this.mLastInsets.getSystemWindowInsetRight()), widthMode);
                        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize - widthUsed2, heightMode);
                    }
                    Behavior b = lp.getBehavior();
                    if (b != null) {
                        View child4 = child2;
                        heightUsed3 = heightUsed2;
                        childState2 = childCount2;
                        child3 = child4;
                        widthUsed3 = heightMode2;
                        childCount2 = childCount;
                        heightMode2 = heightMode;
                    } else {
                        heightUsed3 = heightUsed2;
                        childState2 = childCount2;
                        widthUsed3 = heightMode2;
                        child3 = child2;
                        childCount2 = childCount;
                        heightMode2 = heightMode;
                    }
                    onMeasureChild(child3, childWidthMeasureSpec, keylineWidthUsed, childHeightMeasureSpec, 0);
                    child = child3;
                    widthUsed = Math.max(widthUsed3, ((widthPadding + child.getMeasuredWidth()) + lp.leftMargin) + lp.rightMargin);
                    heightUsed = Math.max(heightUsed3, ((heightPadding + child.getMeasuredHeight()) + lp.topMargin) + lp.bottomMargin);
                    childState = View.combineMeasuredStates(childState2, child.getMeasuredState());
                    widthUsed2 = widthUsed;
                }
                i = i2 + 1;
                childCount = childCount2;
                heightMode = heightMode2;
            } else {
                heightMode2 = heightMode;
                int i3 = childState;
                childState = heightUsed;
                heightUsed = widthUsed2;
                widthUsed2 = i3;
                setMeasuredDimension(View.resolveSizeAndState(heightUsed, widthMeasureSpec, ViewCompat.MEASURED_STATE_MASK & widthUsed2), View.resolveSizeAndState(childState, heightMeasureSpec, widthUsed2 << 16));
                return;
            }
        }
    }

    private WindowInsetsCompat dispatchApplyWindowInsetsToBehaviors(WindowInsetsCompat insets) {
        if (insets.isConsumed()) {
            return insets;
        }
        int z = getChildCount();
        for (int i = 0; i < z; i++) {
            View child = getChildAt(i);
            if (ViewCompat.getFitsSystemWindows(child)) {
                Behavior b = ((LayoutParams) child.getLayoutParams()).getBehavior();
                if (b != null) {
                    insets = b.onApplyWindowInsets(this, child, insets);
                    if (insets.isConsumed()) {
                        break;
                    }
                } else {
                    continue;
                }
            }
        }
        return insets;
    }

    public void onLayoutChild(@NonNull View child, int layoutDirection) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.checkAnchorChanged()) {
            throw new IllegalStateException("An anchor may not be changed after CoordinatorLayout measurement begins before layout is complete.");
        } else if (lp.mAnchorView != null) {
            layoutChildWithAnchor(child, lp.mAnchorView, layoutDirection);
        } else if (lp.keyline >= 0) {
            layoutChildWithKeyline(child, lp.keyline, layoutDirection);
        } else {
            layoutChild(child, layoutDirection);
        }
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        int childCount = this.mDependencySortedChildren.size();
        for (int i = 0; i < childCount; i++) {
            View child = (View) this.mDependencySortedChildren.get(i);
            if (child.getVisibility() != 8) {
                Behavior behavior = ((LayoutParams) child.getLayoutParams()).getBehavior();
                if (behavior == null || !behavior.onLayoutChild(this, child, layoutDirection)) {
                    onLayoutChild(child, layoutDirection);
                }
            }
        }
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (this.mDrawStatusBarBackground && this.mStatusBarBackground != null) {
            int inset = this.mLastInsets != null ? this.mLastInsets.getSystemWindowInsetTop() : 0;
            if (inset > 0) {
                this.mStatusBarBackground.setBounds(0, 0, getWidth(), inset);
                this.mStatusBarBackground.draw(c);
            }
        }
    }

    public void setFitsSystemWindows(boolean fitSystemWindows) {
        super.setFitsSystemWindows(fitSystemWindows);
        setupForInsets();
    }

    void recordLastChildRect(View child, Rect r) {
        ((LayoutParams) child.getLayoutParams()).setLastChildRect(r);
    }

    void getLastChildRect(View child, Rect out) {
        out.set(((LayoutParams) child.getLayoutParams()).getLastChildRect());
    }

    void getChildRect(View child, boolean transform, Rect out) {
        if (child.isLayoutRequested() || child.getVisibility() == 8) {
            out.setEmpty();
            return;
        }
        if (transform) {
            getDescendantRect(child, out);
        } else {
            out.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        }
    }

    private void getDesiredAnchoredChildRectWithoutConstraints(View child, int layoutDirection, Rect anchorRect, Rect out, LayoutParams lp, int childWidth, int childHeight) {
        int left;
        int top;
        int i = layoutDirection;
        Rect rect = anchorRect;
        LayoutParams layoutParams = lp;
        int absGravity = GravityCompat.getAbsoluteGravity(resolveAnchoredChildGravity(layoutParams.gravity), i);
        int absAnchorGravity = GravityCompat.getAbsoluteGravity(resolveGravity(layoutParams.anchorGravity), i);
        int hgrav = absGravity & 7;
        int vgrav = absGravity & 112;
        int anchorHgrav = absAnchorGravity & 7;
        int anchorVgrav = absAnchorGravity & 112;
        if (anchorHgrav == 1) {
            left = rect.left + (anchorRect.width() / 2);
        } else if (anchorHgrav != 5) {
            left = rect.left;
        } else {
            left = rect.right;
        }
        if (anchorVgrav == 16) {
            top = rect.top + (anchorRect.height() / 2);
        } else if (anchorVgrav != 80) {
            top = rect.top;
        } else {
            top = rect.bottom;
        }
        if (hgrav == 1) {
            left -= childWidth / 2;
        } else if (hgrav != 5) {
            left -= childWidth;
        }
        if (vgrav == 16) {
            top -= childHeight / 2;
        } else if (vgrav != 80) {
            top -= childHeight;
        }
        out.set(left, top, left + childWidth, top + childHeight);
    }

    private void constrainChildRect(LayoutParams lp, Rect out, int childWidth, int childHeight) {
        int width = getWidth();
        int height = getHeight();
        int left = Math.max(getPaddingLeft() + lp.leftMargin, Math.min(out.left, ((width - getPaddingRight()) - childWidth) - lp.rightMargin));
        int top = Math.max(getPaddingTop() + lp.topMargin, Math.min(out.top, ((height - getPaddingBottom()) - childHeight) - lp.bottomMargin));
        out.set(left, top, left + childWidth, top + childHeight);
    }

    void getDesiredAnchoredChildRect(View child, int layoutDirection, Rect anchorRect, Rect out) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        getDesiredAnchoredChildRectWithoutConstraints(child, layoutDirection, anchorRect, out, lp, childWidth, childHeight);
        constrainChildRect(lp, out, childWidth, childHeight);
    }

    private void layoutChildWithAnchor(View child, View anchor, int layoutDirection) {
        Rect anchorRect = acquireTempRect();
        Rect childRect = acquireTempRect();
        try {
            getDescendantRect(anchor, anchorRect);
            getDesiredAnchoredChildRect(child, layoutDirection, anchorRect, childRect);
            child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
        } finally {
            releaseTempRect(anchorRect);
            releaseTempRect(childRect);
        }
    }

    private void layoutChildWithKeyline(View child, int keyline, int layoutDirection) {
        int keyline2;
        int i = layoutDirection;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int absGravity = GravityCompat.getAbsoluteGravity(resolveKeylineGravity(lp.gravity), i);
        int hgrav = absGravity & 7;
        int vgrav = absGravity & 112;
        int width = getWidth();
        int height = getHeight();
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        if (i == 1) {
            keyline2 = width - keyline;
        } else {
            keyline2 = keyline;
        }
        int left = getKeyline(keyline2) - childWidth;
        int top = 0;
        if (hgrav == 1) {
            left += childWidth / 2;
        } else if (hgrav == 5) {
            left += childWidth;
        }
        if (vgrav == 16) {
            top = 0 + (childHeight / 2);
        } else if (vgrav == 80) {
            top = 0 + childHeight;
        }
        int left2 = Math.max(getPaddingLeft() + lp.leftMargin, Math.min(left, ((width - getPaddingRight()) - childWidth) - lp.rightMargin));
        left = Math.max(getPaddingTop() + lp.topMargin, Math.min(top, ((height - getPaddingBottom()) - childHeight) - lp.bottomMargin));
        child.layout(left2, left, left2 + childWidth, left + childHeight);
    }

    private void layoutChild(View child, int layoutDirection) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        Rect parent = acquireTempRect();
        parent.set(getPaddingLeft() + lp.leftMargin, getPaddingTop() + lp.topMargin, (getWidth() - getPaddingRight()) - lp.rightMargin, (getHeight() - getPaddingBottom()) - lp.bottomMargin);
        if (!(this.mLastInsets == null || !ViewCompat.getFitsSystemWindows(this) || ViewCompat.getFitsSystemWindows(child))) {
            parent.left += this.mLastInsets.getSystemWindowInsetLeft();
            parent.top += this.mLastInsets.getSystemWindowInsetTop();
            parent.right -= this.mLastInsets.getSystemWindowInsetRight();
            parent.bottom -= this.mLastInsets.getSystemWindowInsetBottom();
        }
        Rect out = acquireTempRect();
        GravityCompat.apply(resolveGravity(lp.gravity), child.getMeasuredWidth(), child.getMeasuredHeight(), parent, out, layoutDirection);
        child.layout(out.left, out.top, out.right, out.bottom);
        releaseTempRect(parent);
        releaseTempRect(out);
    }

    private static int resolveGravity(int gravity) {
        if ((gravity & 7) == 0) {
            gravity |= GravityCompat.START;
        }
        if ((gravity & 112) == 0) {
            return gravity | 48;
        }
        return gravity;
    }

    private static int resolveKeylineGravity(int gravity) {
        return gravity == 0 ? 8388661 : gravity;
    }

    private static int resolveAnchoredChildGravity(int gravity) {
        return gravity == 0 ? 17 : gravity;
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mBehavior != null) {
            float scrimAlpha = lp.mBehavior.getScrimOpacity(this, child);
            if (scrimAlpha > 0.0f) {
                if (this.mScrimPaint == null) {
                    this.mScrimPaint = new Paint();
                }
                this.mScrimPaint.setColor(lp.mBehavior.getScrimColor(this, child));
                this.mScrimPaint.setAlpha(clamp(Math.round(255.0f * scrimAlpha), 0, 255));
                int saved = canvas.save();
                if (child.isOpaque()) {
                    canvas.clipRect((float) child.getLeft(), (float) child.getTop(), (float) child.getRight(), (float) child.getBottom(), Op.DIFFERENCE);
                }
                canvas.drawRect((float) getPaddingLeft(), (float) getPaddingTop(), (float) (getWidth() - getPaddingRight()), (float) (getHeight() - getPaddingBottom()), this.mScrimPaint);
                canvas.restoreToCount(saved);
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    final void onChildViewsChanged(int type) {
        boolean z = type;
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        int childCount = this.mDependencySortedChildren.size();
        Rect inset = acquireTempRect();
        Rect drawRect = acquireTempRect();
        Rect lastDrawRect = acquireTempRect();
        for (int i = 0; i < childCount; i++) {
            View child = (View) this.mDependencySortedChildren.get(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (z || child.getVisibility() != 8) {
                int i2;
                for (int j = 0; j < i; j++) {
                    if (lp.mAnchorDirectChild == ((View) this.mDependencySortedChildren.get(j))) {
                        offsetChildToAnchor(child, layoutDirection);
                    }
                }
                boolean z2 = true;
                getChildRect(child, true, drawRect);
                if (!(lp.insetEdge == 0 || drawRect.isEmpty())) {
                    int absInsetEdge = GravityCompat.getAbsoluteGravity(lp.insetEdge, layoutDirection);
                    i2 = absInsetEdge & 112;
                    if (i2 == 48) {
                        inset.top = Math.max(inset.top, drawRect.bottom);
                    } else if (i2 == 80) {
                        inset.bottom = Math.max(inset.bottom, getHeight() - drawRect.top);
                    }
                    i2 = absInsetEdge & 7;
                    if (i2 == 3) {
                        inset.left = Math.max(inset.left, drawRect.right);
                    } else if (i2 == 5) {
                        inset.right = Math.max(inset.right, getWidth() - drawRect.left);
                    }
                }
                if (lp.dodgeInsetEdges != 0 && child.getVisibility() == 0) {
                    offsetChildByInset(child, inset, layoutDirection);
                }
                if (!z) {
                    getLastChildRect(child, lastDrawRect);
                    if (!lastDrawRect.equals(drawRect)) {
                        recordLastChildRect(child, drawRect);
                    }
                }
                i2 = i + 1;
                while (i2 < childCount) {
                    View checkChild = (View) this.mDependencySortedChildren.get(i2);
                    LayoutParams checkLp = (LayoutParams) checkChild.getLayoutParams();
                    Behavior b = checkLp.getBehavior();
                    if (b != null && b.layoutDependsOn(this, checkChild, child)) {
                        if (z || !checkLp.getChangedAfterNestedScroll()) {
                            boolean handled;
                            if (!z) {
                                handled = b.onDependentViewChanged(this, checkChild, child);
                            } else {
                                b.onDependentViewRemoved(this, checkChild, child);
                                handled = true;
                            }
                            boolean handled2 = handled;
                            if (z == z2) {
                                checkLp.setChangedAfterNestedScroll(handled2);
                            }
                        } else {
                            checkLp.resetChangedAfterNestedScroll();
                        }
                    }
                    i2++;
                    z2 = true;
                }
            }
        }
        releaseTempRect(inset);
        releaseTempRect(drawRect);
        releaseTempRect(lastDrawRect);
    }

    private void offsetChildByInset(View child, Rect inset, int layoutDirection) {
        if (ViewCompat.isLaidOut(child) && child.getWidth() > 0 && child.getHeight() > 0) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior behavior = lp.getBehavior();
            Rect dodgeRect = acquireTempRect();
            Rect bounds = acquireTempRect();
            bounds.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            if (behavior == null || !behavior.getInsetDodgeRect(this, child, dodgeRect)) {
                dodgeRect.set(bounds);
            } else if (!bounds.contains(dodgeRect)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Rect should be within the child's bounds. Rect:");
                stringBuilder.append(dodgeRect.toShortString());
                stringBuilder.append(" | Bounds:");
                stringBuilder.append(bounds.toShortString());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            releaseTempRect(bounds);
            if (dodgeRect.isEmpty()) {
                releaseTempRect(dodgeRect);
                return;
            }
            int distance;
            int distance2;
            int absDodgeInsetEdges = GravityCompat.getAbsoluteGravity(lp.dodgeInsetEdges, layoutDirection);
            boolean offsetY = false;
            if ((absDodgeInsetEdges & 48) == 48) {
                distance = (dodgeRect.top - lp.topMargin) - lp.mInsetOffsetY;
                if (distance < inset.top) {
                    setInsetOffsetY(child, inset.top - distance);
                    offsetY = true;
                }
            }
            if ((absDodgeInsetEdges & 80) == 80) {
                distance = ((getHeight() - dodgeRect.bottom) - lp.bottomMargin) + lp.mInsetOffsetY;
                if (distance < inset.bottom) {
                    setInsetOffsetY(child, distance - inset.bottom);
                    offsetY = true;
                }
            }
            if (!offsetY) {
                setInsetOffsetY(child, 0);
            }
            boolean offsetX = false;
            if ((absDodgeInsetEdges & 3) == 3) {
                distance2 = (dodgeRect.left - lp.leftMargin) - lp.mInsetOffsetX;
                if (distance2 < inset.left) {
                    setInsetOffsetX(child, inset.left - distance2);
                    offsetX = true;
                }
            }
            if ((absDodgeInsetEdges & 5) == 5) {
                distance2 = ((getWidth() - dodgeRect.right) - lp.rightMargin) + lp.mInsetOffsetX;
                if (distance2 < inset.right) {
                    setInsetOffsetX(child, distance2 - inset.right);
                    offsetX = true;
                }
            }
            if (!offsetX) {
                setInsetOffsetX(child, 0);
            }
            releaseTempRect(dodgeRect);
        }
    }

    private void setInsetOffsetX(View child, int offsetX) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mInsetOffsetX != offsetX) {
            ViewCompat.offsetLeftAndRight(child, offsetX - lp.mInsetOffsetX);
            lp.mInsetOffsetX = offsetX;
        }
    }

    private void setInsetOffsetY(View child, int offsetY) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mInsetOffsetY != offsetY) {
            ViewCompat.offsetTopAndBottom(child, offsetY - lp.mInsetOffsetY);
            lp.mInsetOffsetY = offsetY;
        }
    }

    public void dispatchDependentViewsChanged(@NonNull View view) {
        List<View> dependents = this.mChildDag.getIncomingEdges(view);
        if (dependents != null && !dependents.isEmpty()) {
            for (int i = 0; i < dependents.size(); i++) {
                View child = (View) dependents.get(i);
                Behavior b = ((LayoutParams) child.getLayoutParams()).getBehavior();
                if (b != null) {
                    b.onDependentViewChanged(this, child, view);
                }
            }
        }
    }

    @NonNull
    public List<View> getDependencies(@NonNull View child) {
        List<View> dependencies = this.mChildDag.getOutgoingEdges(child);
        this.mTempDependenciesList.clear();
        if (dependencies != null) {
            this.mTempDependenciesList.addAll(dependencies);
        }
        return this.mTempDependenciesList;
    }

    @NonNull
    public List<View> getDependents(@NonNull View child) {
        List<View> edges = this.mChildDag.getIncomingEdges(child);
        this.mTempDependenciesList.clear();
        if (edges != null) {
            this.mTempDependenciesList.addAll(edges);
        }
        return this.mTempDependenciesList;
    }

    @VisibleForTesting
    final List<View> getDependencySortedChildren() {
        prepareChildren();
        return Collections.unmodifiableList(this.mDependencySortedChildren);
    }

    void ensurePreDrawListener() {
        boolean hasDependencies = false;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (hasDependencies(getChildAt(i))) {
                hasDependencies = true;
                break;
            }
        }
        if (hasDependencies == this.mNeedsPreDrawListener) {
            return;
        }
        if (hasDependencies) {
            addPreDrawListener();
        } else {
            removePreDrawListener();
        }
    }

    private boolean hasDependencies(View child) {
        return this.mChildDag.hasOutgoingEdges(child);
    }

    void addPreDrawListener() {
        if (this.mIsAttachedToWindow) {
            if (this.mOnPreDrawListener == null) {
                this.mOnPreDrawListener = new OnPreDrawListener();
            }
            getViewTreeObserver().addOnPreDrawListener(this.mOnPreDrawListener);
        }
        this.mNeedsPreDrawListener = true;
    }

    void removePreDrawListener() {
        if (this.mIsAttachedToWindow && this.mOnPreDrawListener != null) {
            getViewTreeObserver().removeOnPreDrawListener(this.mOnPreDrawListener);
        }
        this.mNeedsPreDrawListener = false;
    }

    void offsetChildToAnchor(View child, int layoutDirection) {
        View view = child;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mAnchorView != null) {
            Rect anchorRect = acquireTempRect();
            Rect childRect = acquireTempRect();
            Rect desiredChildRect = acquireTempRect();
            getDescendantRect(lp.mAnchorView, anchorRect);
            boolean z = false;
            getChildRect(view, false, childRect);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            int childHeight2 = childHeight;
            getDesiredAnchoredChildRectWithoutConstraints(view, layoutDirection, anchorRect, desiredChildRect, lp, childWidth, childHeight);
            if (!(desiredChildRect.left == childRect.left && desiredChildRect.top == childRect.top)) {
                z = true;
            }
            boolean changed = z;
            constrainChildRect(lp, desiredChildRect, childWidth, childHeight2);
            int dx = desiredChildRect.left - childRect.left;
            int dy = desiredChildRect.top - childRect.top;
            if (dx != 0) {
                ViewCompat.offsetLeftAndRight(view, dx);
            }
            if (dy != 0) {
                ViewCompat.offsetTopAndBottom(view, dy);
            }
            if (changed) {
                Behavior b = lp.getBehavior();
                if (b != null) {
                    b.onDependentViewChanged(this, view, lp.mAnchorView);
                }
            }
            releaseTempRect(anchorRect);
            releaseTempRect(childRect);
            releaseTempRect(desiredChildRect);
        }
    }

    public boolean isPointInChildBounds(@NonNull View child, int x, int y) {
        Rect r = acquireTempRect();
        getDescendantRect(child, r);
        try {
            boolean contains = r.contains(x, y);
            return contains;
        } finally {
            releaseTempRect(r);
        }
    }

    public boolean doViewsOverlap(@NonNull View first, @NonNull View second) {
        boolean z = false;
        if (first.getVisibility() != 0 || second.getVisibility() != 0) {
            return false;
        }
        Rect firstRect = acquireTempRect();
        getChildRect(first, first.getParent() != this, firstRect);
        Rect secondRect = acquireTempRect();
        getChildRect(second, second.getParent() != this, secondRect);
        try {
            if (firstRect.left <= secondRect.right && firstRect.top <= secondRect.bottom && firstRect.right >= secondRect.left && firstRect.bottom >= secondRect.top) {
                z = true;
            }
            releaseTempRect(firstRect);
            releaseTempRect(secondRect);
            return z;
        } catch (Throwable th) {
            releaseTempRect(firstRect);
            releaseTempRect(secondRect);
        }
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    protected LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) p);
        }
        if (p instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) p);
        }
        return new LayoutParams(p);
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return (p instanceof LayoutParams) && super.checkLayoutParams(p);
    }

    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return onStartNestedScroll(child, target, nestedScrollAxes, 0);
    }

    public boolean onStartNestedScroll(View child, View target, int axes, int type) {
        int i = type;
        int childCount = getChildCount();
        boolean handled = false;
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 < childCount) {
                View view = getChildAt(i3);
                if (view.getVisibility() != 8) {
                    LayoutParams lp = (LayoutParams) view.getLayoutParams();
                    Behavior viewBehavior = lp.getBehavior();
                    if (viewBehavior != null) {
                        boolean accepted = viewBehavior.onStartNestedScroll(this, view, child, target, axes, i);
                        boolean handled2 = handled | accepted;
                        lp.setNestedScrollAccepted(i, accepted);
                        handled = handled2;
                    } else {
                        lp.setNestedScrollAccepted(i, false);
                    }
                }
                i2 = i3 + 1;
            } else {
                return handled;
            }
        }
    }

    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        onNestedScrollAccepted(child, target, nestedScrollAxes, 0);
    }

    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes, int type) {
        View view = target;
        int i = type;
        View view2 = child;
        int i2 = nestedScrollAxes;
        this.mNestedScrollingParentHelper.onNestedScrollAccepted(view2, view, i2, i);
        this.mNestedScrollingTarget = view;
        int childCount = getChildCount();
        int i3 = 0;
        while (true) {
            int i4 = i3;
            if (i4 < childCount) {
                View view3 = getChildAt(i4);
                LayoutParams lp = (LayoutParams) view3.getLayoutParams();
                if (lp.isNestedScrollAccepted(i)) {
                    Behavior viewBehavior = lp.getBehavior();
                    if (viewBehavior != null) {
                        viewBehavior.onNestedScrollAccepted(this, view3, view2, view, i2, i);
                    }
                }
                i3 = i4 + 1;
            } else {
                return;
            }
        }
    }

    public void onStopNestedScroll(View target) {
        onStopNestedScroll(target, 0);
    }

    public void onStopNestedScroll(View target, int type) {
        this.mNestedScrollingParentHelper.onStopNestedScroll(target, type);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollAccepted(type)) {
                Behavior viewBehavior = lp.getBehavior();
                if (viewBehavior != null) {
                    viewBehavior.onStopNestedScroll(this, view, target, type);
                }
                lp.resetNestedScroll(type);
                lp.resetChangedAfterNestedScroll();
            }
        }
        this.mNestedScrollingTarget = null;
    }

    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, 0);
    }

    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        int i;
        int childCount = getChildCount();
        int i2 = 0;
        boolean accepted = false;
        while (true) {
            int i3 = i2;
            if (i3 >= childCount) {
                break;
            }
            View view = getChildAt(i3);
            if (view.getVisibility() == 8) {
                i = type;
            } else {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                i = type;
                if (lp.isNestedScrollAccepted(i)) {
                    Behavior viewBehavior = lp.getBehavior();
                    if (viewBehavior != null) {
                        viewBehavior.onNestedScroll(this, view, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, i);
                        accepted = true;
                    }
                }
            }
            i2 = i3 + 1;
        }
        i = type;
        if (accepted) {
            onChildViewsChanged(1);
        }
    }

    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        onNestedPreScroll(target, dx, dy, consumed, 0);
    }

    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
        int childCount = getChildCount();
        int xConsumed = 0;
        int yConsumed = 0;
        boolean accepted = false;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= childCount) {
                break;
            }
            View view = getChildAt(i2);
            if (view.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.isNestedScrollAccepted(type)) {
                    Behavior viewBehavior = lp.getBehavior();
                    if (viewBehavior != null) {
                        int xConsumed2;
                        int yConsumed2;
                        int[] iArr = this.mTempIntPair;
                        this.mTempIntPair[1] = 0;
                        iArr[0] = 0;
                        viewBehavior.onNestedPreScroll(this, view, target, dx, dy, this.mTempIntPair, type);
                        if (dx > 0) {
                            xConsumed2 = Math.max(xConsumed, this.mTempIntPair[0]);
                        } else {
                            xConsumed2 = Math.min(xConsumed, this.mTempIntPair[0]);
                        }
                        if (dy > 0) {
                            yConsumed2 = Math.max(yConsumed, this.mTempIntPair[1]);
                        } else {
                            yConsumed2 = Math.min(yConsumed, this.mTempIntPair[1]);
                        }
                        xConsumed = xConsumed2;
                        yConsumed = yConsumed2;
                        accepted = true;
                    }
                }
            }
            i = i2 + 1;
        }
        int i3 = 1;
        consumed[0] = xConsumed;
        consumed[i3] = yConsumed;
        if (accepted) {
            onChildViewsChanged(i3);
        }
    }

    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        int childCount = getChildCount();
        boolean handled = false;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= childCount) {
                break;
            }
            View view = getChildAt(i2);
            if (view.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.isNestedScrollAccepted(0)) {
                    Behavior viewBehavior = lp.getBehavior();
                    if (viewBehavior != null) {
                        handled = viewBehavior.onNestedFling(this, view, target, velocityX, velocityY, consumed) | handled;
                    }
                }
            }
            i = i2 + 1;
        }
        if (handled) {
            onChildViewsChanged(1);
        }
        return handled;
    }

    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        int childCount = getChildCount();
        boolean handled = false;
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            if (view.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.isNestedScrollAccepted(0)) {
                    Behavior viewBehavior = lp.getBehavior();
                    if (viewBehavior != null) {
                        handled |= viewBehavior.onNestedPreFling(this, view, target, velocityX, velocityY);
                    }
                }
            }
        }
        return handled;
    }

    public int getNestedScrollAxes() {
        return this.mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            SparseArray<Parcelable> behaviorStates = ss.behaviorStates;
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                int childId = child.getId();
                Behavior b = getResolvedLayoutParams(child).getBehavior();
                if (!(childId == -1 || b == null)) {
                    Parcelable savedState = (Parcelable) behaviorStates.get(childId);
                    if (savedState != null) {
                        b.onRestoreInstanceState(this, child, savedState);
                    }
                }
            }
            return;
        }
        super.onRestoreInstanceState(state);
    }

    protected Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        SparseArray<Parcelable> behaviorStates = new SparseArray();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childId = child.getId();
            Behavior b = ((LayoutParams) child.getLayoutParams()).getBehavior();
            if (!(childId == -1 || b == null)) {
                Parcelable state = b.onSaveInstanceState(this, child);
                if (state != null) {
                    behaviorStates.append(childId, state);
                }
            }
        }
        ss.behaviorStates = behaviorStates;
        return ss;
    }

    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        Behavior behavior = ((LayoutParams) child.getLayoutParams()).getBehavior();
        if (behavior == null || !behavior.onRequestChildRectangleOnScreen(this, child, rectangle, immediate)) {
            return super.requestChildRectangleOnScreen(child, rectangle, immediate);
        }
        return true;
    }

    private void setupForInsets() {
        if (VERSION.SDK_INT >= 21) {
            if (ViewCompat.getFitsSystemWindows(this)) {
                if (this.mApplyWindowInsetsListener == null) {
                    this.mApplyWindowInsetsListener = new OnApplyWindowInsetsListener() {
                        public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                            return CoordinatorLayout.this.setWindowInsets(insets);
                        }
                    };
                }
                ViewCompat.setOnApplyWindowInsetsListener(this, this.mApplyWindowInsetsListener);
                setSystemUiVisibility(1280);
            } else {
                ViewCompat.setOnApplyWindowInsetsListener(this, null);
            }
        }
    }
}
