package android.support.v4.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.OverScroller;
import java.util.Arrays;

public class ViewDragHelper {
    private static final int BASE_SETTLE_DURATION = 256;
    public static final int DIRECTION_ALL = 3;
    public static final int DIRECTION_HORIZONTAL = 1;
    public static final int DIRECTION_VERTICAL = 2;
    public static final int EDGE_ALL = 15;
    public static final int EDGE_BOTTOM = 8;
    public static final int EDGE_LEFT = 1;
    public static final int EDGE_RIGHT = 2;
    private static final int EDGE_SIZE = 20;
    public static final int EDGE_TOP = 4;
    public static final int INVALID_POINTER = -1;
    private static final int MAX_SETTLE_DURATION = 600;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_SETTLING = 2;
    private static final String TAG = "ViewDragHelper";
    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return ((((t * t) * t) * t) * t) + 1.0f;
        }
    };
    private int mActivePointerId = -1;
    private final Callback mCallback;
    private View mCapturedView;
    private int mDragState;
    private int[] mEdgeDragsInProgress;
    private int[] mEdgeDragsLocked;
    private int mEdgeSize;
    private int[] mInitialEdgesTouched;
    private float[] mInitialMotionX;
    private float[] mInitialMotionY;
    private float[] mLastMotionX;
    private float[] mLastMotionY;
    private float mMaxVelocity;
    private float mMinVelocity;
    private final ViewGroup mParentView;
    private int mPointersDown;
    private boolean mReleaseInProgress;
    private OverScroller mScroller;
    private final Runnable mSetIdleRunnable = new Runnable() {
        public void run() {
            ViewDragHelper.this.setDragState(0);
        }
    };
    private int mTouchSlop;
    private int mTrackingEdges;
    private VelocityTracker mVelocityTracker;

    public static abstract class Callback {
        public abstract boolean tryCaptureView(@NonNull View view, int i);

        public void onViewDragStateChanged(int state) {
        }

        public void onViewPositionChanged(@NonNull View changedView, int left, int top, @Px int dx, @Px int dy) {
        }

        public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
        }

        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
        }

        public void onEdgeTouched(int edgeFlags, int pointerId) {
        }

        public boolean onEdgeLock(int edgeFlags) {
            return false;
        }

        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
        }

        public int getOrderedChildIndex(int index) {
            return index;
        }

        public int getViewHorizontalDragRange(@NonNull View child) {
            return 0;
        }

        public int getViewVerticalDragRange(@NonNull View child) {
            return 0;
        }

        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            return 0;
        }

        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return 0;
        }
    }

    public static ViewDragHelper create(@NonNull ViewGroup forParent, @NonNull Callback cb) {
        return new ViewDragHelper(forParent.getContext(), forParent, cb);
    }

    public static ViewDragHelper create(@NonNull ViewGroup forParent, float sensitivity, @NonNull Callback cb) {
        ViewDragHelper helper = create(forParent, cb);
        helper.mTouchSlop = (int) (((float) helper.mTouchSlop) * (1.0f / sensitivity));
        return helper;
    }

    private ViewDragHelper(@NonNull Context context, @NonNull ViewGroup forParent, @NonNull Callback cb) {
        if (forParent == null) {
            throw new IllegalArgumentException("Parent view may not be null");
        } else if (cb != null) {
            this.mParentView = forParent;
            this.mCallback = cb;
            ViewConfiguration vc = ViewConfiguration.get(context);
            this.mEdgeSize = (int) ((20.0f * context.getResources().getDisplayMetrics().density) + 0.5f);
            this.mTouchSlop = vc.getScaledTouchSlop();
            this.mMaxVelocity = (float) vc.getScaledMaximumFlingVelocity();
            this.mMinVelocity = (float) vc.getScaledMinimumFlingVelocity();
            this.mScroller = new OverScroller(context, sInterpolator);
        } else {
            throw new IllegalArgumentException("Callback may not be null");
        }
    }

    public void setMinVelocity(float minVel) {
        this.mMinVelocity = minVel;
    }

    public float getMinVelocity() {
        return this.mMinVelocity;
    }

    public int getViewDragState() {
        return this.mDragState;
    }

    public void setEdgeTrackingEnabled(int edgeFlags) {
        this.mTrackingEdges = edgeFlags;
    }

    @Px
    public int getEdgeSize() {
        return this.mEdgeSize;
    }

    public void captureChildView(@NonNull View childView, int activePointerId) {
        if (childView.getParent() == this.mParentView) {
            this.mCapturedView = childView;
            this.mActivePointerId = activePointerId;
            this.mCallback.onViewCaptured(childView, activePointerId);
            setDragState(1);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("captureChildView: parameter must be a descendant of the ViewDragHelper's tracked parent view (");
        stringBuilder.append(this.mParentView);
        stringBuilder.append(")");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @Nullable
    public View getCapturedView() {
        return this.mCapturedView;
    }

    public int getActivePointerId() {
        return this.mActivePointerId;
    }

    @Px
    public int getTouchSlop() {
        return this.mTouchSlop;
    }

    public void cancel() {
        this.mActivePointerId = -1;
        clearMotionHistory();
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public void abort() {
        cancel();
        if (this.mDragState == 2) {
            int oldX = this.mScroller.getCurrX();
            int oldY = this.mScroller.getCurrY();
            this.mScroller.abortAnimation();
            int newX = this.mScroller.getCurrX();
            int newY = this.mScroller.getCurrY();
            this.mCallback.onViewPositionChanged(this.mCapturedView, newX, newY, newX - oldX, newY - oldY);
        }
        setDragState(0);
    }

    public boolean smoothSlideViewTo(@NonNull View child, int finalLeft, int finalTop) {
        this.mCapturedView = child;
        this.mActivePointerId = -1;
        boolean continueSliding = forceSettleCapturedViewAt(finalLeft, finalTop, 0, 0);
        if (!(continueSliding || this.mDragState != 0 || this.mCapturedView == null)) {
            this.mCapturedView = null;
        }
        return continueSliding;
    }

    public boolean settleCapturedViewAt(int finalLeft, int finalTop) {
        if (this.mReleaseInProgress) {
            return forceSettleCapturedViewAt(finalLeft, finalTop, (int) this.mVelocityTracker.getXVelocity(this.mActivePointerId), (int) this.mVelocityTracker.getYVelocity(this.mActivePointerId));
        }
        throw new IllegalStateException("Cannot settleCapturedViewAt outside of a call to Callback#onViewReleased");
    }

    private boolean forceSettleCapturedViewAt(int finalLeft, int finalTop, int xvel, int yvel) {
        int startLeft = this.mCapturedView.getLeft();
        int startTop = this.mCapturedView.getTop();
        int dx = finalLeft - startLeft;
        int dy = finalTop - startTop;
        if (dx == 0 && dy == 0) {
            this.mScroller.abortAnimation();
            setDragState(0);
            return false;
        }
        this.mScroller.startScroll(startLeft, startTop, dx, dy, computeSettleDuration(this.mCapturedView, dx, dy, xvel, yvel));
        setDragState(2);
        return true;
    }

    private int computeSettleDuration(View child, int dx, int dy, int xvel, int yvel) {
        float xweight;
        float f;
        float f2;
        View view = child;
        int xvel2 = clampMag(xvel, (int) this.mMinVelocity, (int) this.mMaxVelocity);
        int yvel2 = clampMag(yvel, (int) this.mMinVelocity, (int) this.mMaxVelocity);
        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);
        int absXVel = Math.abs(xvel2);
        int absYVel = Math.abs(yvel2);
        int addedVel = absXVel + absYVel;
        int addedDistance = absDx + absDy;
        if (xvel2 != 0) {
            xweight = (float) absXVel;
            f = (float) addedVel;
        } else {
            xweight = (float) absDx;
            f = (float) addedDistance;
        }
        xweight /= f;
        if (yvel2 != 0) {
            f = (float) absYVel;
            f2 = (float) addedVel;
        } else {
            f = (float) absDy;
            f2 = (float) addedDistance;
        }
        f /= f2;
        return (int) ((((float) computeAxisDuration(dx, xvel2, this.mCallback.getViewHorizontalDragRange(view))) * xweight) + (((float) computeAxisDuration(dy, yvel2, this.mCallback.getViewVerticalDragRange(view))) * f));
    }

    private int computeAxisDuration(int delta, int velocity, int motionRange) {
        if (delta == 0) {
            return 0;
        }
        int duration;
        int width = this.mParentView.getWidth();
        int halfWidth = width / 2;
        float distance = ((float) halfWidth) + (((float) halfWidth) * distanceInfluenceForSnapDuration(Math.min(1.0f, ((float) Math.abs(delta)) / ((float) width))));
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000.0f * Math.abs(distance / ((float) velocity)));
        } else {
            duration = (int) ((1.0f + (((float) Math.abs(delta)) / ((float) motionRange))) * 256.0f);
        }
        return Math.min(duration, MAX_SETTLE_DURATION);
    }

    private int clampMag(int value, int absMin, int absMax) {
        int absValue = Math.abs(value);
        if (absValue < absMin) {
            return 0;
        }
        if (absValue <= absMax) {
            return value;
        }
        return value > 0 ? absMax : -absMax;
    }

    private float clampMag(float value, float absMin, float absMax) {
        float absValue = Math.abs(value);
        if (absValue < absMin) {
            return 0.0f;
        }
        if (absValue <= absMax) {
            return value;
        }
        return value > 0.0f ? absMax : -absMax;
    }

    private float distanceInfluenceForSnapDuration(float f) {
        return (float) Math.sin((double) ((f - 0.5f) * 0.47123894f));
    }

    public void flingCapturedView(int minLeft, int minTop, int maxLeft, int maxTop) {
        if (this.mReleaseInProgress) {
            this.mScroller.fling(this.mCapturedView.getLeft(), this.mCapturedView.getTop(), (int) this.mVelocityTracker.getXVelocity(this.mActivePointerId), (int) this.mVelocityTracker.getYVelocity(this.mActivePointerId), minLeft, maxLeft, minTop, maxTop);
            setDragState(2);
            return;
        }
        throw new IllegalStateException("Cannot flingCapturedView outside of a call to Callback#onViewReleased");
    }

    public boolean continueSettling(boolean deferCallbacks) {
        if (this.mDragState == 2) {
            boolean keepGoing = this.mScroller.computeScrollOffset();
            int x = this.mScroller.getCurrX();
            int y = this.mScroller.getCurrY();
            int dx = x - this.mCapturedView.getLeft();
            int dy = y - this.mCapturedView.getTop();
            if (dx != 0) {
                ViewCompat.offsetLeftAndRight(this.mCapturedView, dx);
            }
            if (dy != 0) {
                ViewCompat.offsetTopAndBottom(this.mCapturedView, dy);
            }
            if (!(dx == 0 && dy == 0)) {
                this.mCallback.onViewPositionChanged(this.mCapturedView, x, y, dx, dy);
            }
            if (keepGoing && x == this.mScroller.getFinalX() && y == this.mScroller.getFinalY()) {
                this.mScroller.abortAnimation();
                keepGoing = false;
            }
            if (!keepGoing) {
                if (deferCallbacks) {
                    this.mParentView.post(this.mSetIdleRunnable);
                } else {
                    setDragState(0);
                }
            }
        }
        if (this.mDragState == 2) {
            return true;
        }
        return false;
    }

    private void dispatchViewReleased(float xvel, float yvel) {
        this.mReleaseInProgress = true;
        this.mCallback.onViewReleased(this.mCapturedView, xvel, yvel);
        this.mReleaseInProgress = false;
        if (this.mDragState == 1) {
            setDragState(0);
        }
    }

    private void clearMotionHistory() {
        if (this.mInitialMotionX != null) {
            Arrays.fill(this.mInitialMotionX, 0.0f);
            Arrays.fill(this.mInitialMotionY, 0.0f);
            Arrays.fill(this.mLastMotionX, 0.0f);
            Arrays.fill(this.mLastMotionY, 0.0f);
            Arrays.fill(this.mInitialEdgesTouched, 0);
            Arrays.fill(this.mEdgeDragsInProgress, 0);
            Arrays.fill(this.mEdgeDragsLocked, 0);
            this.mPointersDown = 0;
        }
    }

    private void clearMotionHistory(int pointerId) {
        if (this.mInitialMotionX != null && isPointerDown(pointerId)) {
            this.mInitialMotionX[pointerId] = 0.0f;
            this.mInitialMotionY[pointerId] = 0.0f;
            this.mLastMotionX[pointerId] = 0.0f;
            this.mLastMotionY[pointerId] = 0.0f;
            this.mInitialEdgesTouched[pointerId] = 0;
            this.mEdgeDragsInProgress[pointerId] = 0;
            this.mEdgeDragsLocked[pointerId] = 0;
            this.mPointersDown &= ~(1 << pointerId);
        }
    }

    private void ensureMotionHistorySizeForId(int pointerId) {
        if (this.mInitialMotionX == null || this.mInitialMotionX.length <= pointerId) {
            float[] imx = new float[(pointerId + 1)];
            float[] imy = new float[(pointerId + 1)];
            float[] lmx = new float[(pointerId + 1)];
            float[] lmy = new float[(pointerId + 1)];
            int[] iit = new int[(pointerId + 1)];
            int[] edip = new int[(pointerId + 1)];
            int[] edl = new int[(pointerId + 1)];
            if (this.mInitialMotionX != null) {
                System.arraycopy(this.mInitialMotionX, 0, imx, 0, this.mInitialMotionX.length);
                System.arraycopy(this.mInitialMotionY, 0, imy, 0, this.mInitialMotionY.length);
                System.arraycopy(this.mLastMotionX, 0, lmx, 0, this.mLastMotionX.length);
                System.arraycopy(this.mLastMotionY, 0, lmy, 0, this.mLastMotionY.length);
                System.arraycopy(this.mInitialEdgesTouched, 0, iit, 0, this.mInitialEdgesTouched.length);
                System.arraycopy(this.mEdgeDragsInProgress, 0, edip, 0, this.mEdgeDragsInProgress.length);
                System.arraycopy(this.mEdgeDragsLocked, 0, edl, 0, this.mEdgeDragsLocked.length);
            }
            this.mInitialMotionX = imx;
            this.mInitialMotionY = imy;
            this.mLastMotionX = lmx;
            this.mLastMotionY = lmy;
            this.mInitialEdgesTouched = iit;
            this.mEdgeDragsInProgress = edip;
            this.mEdgeDragsLocked = edl;
        }
    }

    private void saveInitialMotion(float x, float y, int pointerId) {
        ensureMotionHistorySizeForId(pointerId);
        float[] fArr = this.mInitialMotionX;
        this.mLastMotionX[pointerId] = x;
        fArr[pointerId] = x;
        fArr = this.mInitialMotionY;
        this.mLastMotionY[pointerId] = y;
        fArr[pointerId] = y;
        this.mInitialEdgesTouched[pointerId] = getEdgesTouched((int) x, (int) y);
        this.mPointersDown |= 1 << pointerId;
    }

    private void saveLastMotion(MotionEvent ev) {
        int pointerCount = ev.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            int pointerId = ev.getPointerId(i);
            if (isValidPointerForActionMove(pointerId)) {
                float x = ev.getX(i);
                float y = ev.getY(i);
                this.mLastMotionX[pointerId] = x;
                this.mLastMotionY[pointerId] = y;
            }
        }
    }

    public boolean isPointerDown(int pointerId) {
        return (this.mPointersDown & (1 << pointerId)) != 0;
    }

    void setDragState(int state) {
        this.mParentView.removeCallbacks(this.mSetIdleRunnable);
        if (this.mDragState != state) {
            this.mDragState = state;
            this.mCallback.onViewDragStateChanged(state);
            if (this.mDragState == 0) {
                this.mCapturedView = null;
            }
        }
    }

    boolean tryCaptureViewForDrag(View toCapture, int pointerId) {
        if (toCapture == this.mCapturedView && this.mActivePointerId == pointerId) {
            return true;
        }
        if (toCapture == null || !this.mCallback.tryCaptureView(toCapture, pointerId)) {
            return false;
        }
        this.mActivePointerId = pointerId;
        captureChildView(toCapture, pointerId);
        return true;
    }

    /* JADX WARNING: Missing block: B:20:0x0074, code skipped:
            if (r0.canScrollVertically(-r20) != false) goto L_0x007f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean canScroll(@NonNull View v, boolean checkV, int dx, int dy, int x, int y) {
        int scrollX;
        int scrollY;
        View view = v;
        boolean z = true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            scrollX = v.getScrollX();
            scrollY = v.getScrollY();
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()) {
                    if (canScroll(child, true, dx, dy, (x + scrollX) - child.getLeft(), (y + scrollY) - child.getTop())) {
                        return true;
                    }
                }
            }
        }
        if (checkV) {
            if (view.canScrollHorizontally(-dx)) {
                scrollY = dy;
            }
            return z;
        }
        scrollX = dx;
        scrollY = dy;
        z = false;
        return z;
    }

    /* JADX WARNING: Missing block: B:8:0x0024, code skipped:
            r16 = r2;
            r17 = r3;
            r5 = false;
     */
    /* JADX WARNING: Missing block: B:10:0x0033, code skipped:
            r16 = r2;
            r17 = r3;
     */
    /* JADX WARNING: Missing block: B:40:0x00ed, code skipped:
            if (r2 != r15) goto L_0x00fc;
     */
    /* JADX WARNING: Missing block: B:54:0x0129, code skipped:
            r5 = false;
     */
    /* JADX WARNING: Missing block: B:64:0x0164, code skipped:
            if (r0.mDragState != 1) goto L_0x0167;
     */
    /* JADX WARNING: Missing block: B:74:?, code skipped:
            return r5;
     */
    /* JADX WARNING: Missing block: B:75:?, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean shouldInterceptTouchEvent(@NonNull MotionEvent ev) {
        MotionEvent motionEvent = ev;
        int action = ev.getActionMasked();
        int actionIndex = ev.getActionIndex();
        if (action == 0) {
            cancel();
        }
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        int i;
        int pointerId;
        int edgesTouched;
        int i2;
        float x;
        switch (action) {
            case 0:
                i = actionIndex;
                float x2 = ev.getX();
                float y = ev.getY();
                boolean z = false;
                pointerId = motionEvent.getPointerId(0);
                saveInitialMotion(x2, y, pointerId);
                View toCapture = findTopChildUnder((int) x2, (int) y);
                if (toCapture == this.mCapturedView && this.mDragState == 2) {
                    tryCaptureViewForDrag(toCapture, pointerId);
                }
                edgesTouched = this.mInitialEdgesTouched[pointerId];
                if ((this.mTrackingEdges & edgesTouched) != 0) {
                    this.mCallback.onEdgeTouched(this.mTrackingEdges & edgesTouched, pointerId);
                    break;
                }
                break;
            case 1:
            case 3:
                i = actionIndex;
                cancel();
                break;
            case 2:
                if (this.mInitialMotionX != null) {
                    if (this.mInitialMotionY != null) {
                        int pointerCount;
                        edgesTouched = ev.getPointerCount();
                        i2 = 0;
                        while (i2 < edgesTouched) {
                            int action2;
                            int pointerId2 = motionEvent.getPointerId(i2);
                            if (isValidPointerForActionMove(pointerId2)) {
                                x = motionEvent.getX(i2);
                                float y2 = motionEvent.getY(i2);
                                float dx = x - this.mInitialMotionX[pointerId2];
                                float dy = y2 - this.mInitialMotionY[pointerId2];
                                View toCapture2 = findTopChildUnder((int) x, (int) y2);
                                boolean pastSlop = toCapture2 != null && checkTouchSlop(toCapture2, dx, dy);
                                if (pastSlop) {
                                    int oldLeft = toCapture2.getLeft();
                                    int targetLeft = ((int) dx) + oldLeft;
                                    action2 = action;
                                    action = this.mCallback.clampViewPositionHorizontal(toCapture2, targetLeft, (int) dx);
                                    pointerId = toCapture2.getTop();
                                    i = actionIndex;
                                    actionIndex = ((int) dy) + pointerId;
                                    pointerCount = edgesTouched;
                                    edgesTouched = this.mCallback.clampViewPositionVertical(toCapture2, actionIndex, (int) dy);
                                    targetLeft = this.mCallback.getViewHorizontalDragRange(toCapture2);
                                    actionIndex = this.mCallback.getViewVerticalDragRange(toCapture2);
                                    if (targetLeft != 0) {
                                        if (targetLeft > 0) {
                                        }
                                    }
                                    if (actionIndex != 0) {
                                        if (actionIndex > 0 && edgesTouched == pointerId) {
                                        }
                                    }
                                    saveLastMotion(ev);
                                    break;
                                }
                                action2 = action;
                                i = actionIndex;
                                pointerCount = edgesTouched;
                                reportNewEdgeDrags(dx, dy, pointerId2);
                                if (this.mDragState != 1) {
                                    if (pastSlop && tryCaptureViewForDrag(toCapture2, pointerId2)) {
                                    }
                                }
                                saveLastMotion(ev);
                            } else {
                                action2 = action;
                                i = actionIndex;
                                pointerCount = edgesTouched;
                            }
                            i2++;
                            action = action2;
                            actionIndex = i;
                            edgesTouched = pointerCount;
                        }
                        i = actionIndex;
                        pointerCount = edgesTouched;
                        saveLastMotion(ev);
                    }
                }
                break;
            case 5:
                i2 = motionEvent.getPointerId(actionIndex);
                float x3 = motionEvent.getX(actionIndex);
                x = motionEvent.getY(actionIndex);
                saveInitialMotion(x3, x, i2);
                if (this.mDragState != 0) {
                    if (this.mDragState == 2) {
                        View toCapture3 = findTopChildUnder((int) x3, (int) x);
                        if (toCapture3 == this.mCapturedView) {
                            tryCaptureViewForDrag(toCapture3, i2);
                            break;
                        }
                    }
                }
                edgesTouched = this.mInitialEdgesTouched[i2];
                if ((this.mTrackingEdges & edgesTouched) != 0) {
                    this.mCallback.onEdgeTouched(this.mTrackingEdges & edgesTouched, i2);
                    break;
                }
                break;
            case 6:
                clearMotionHistory(motionEvent.getPointerId(actionIndex));
                break;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void processTouchEvent(@NonNull MotionEvent ev) {
        int action = ev.getActionMasked();
        int actionIndex = ev.getActionIndex();
        if (action == 0) {
            cancel();
        }
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(ev);
        int i = 0;
        float x;
        float y;
        int edgesTouched;
        int pointerCount;
        int pointerId;
        switch (action) {
            case 0:
                x = ev.getX();
                y = ev.getY();
                i = ev.getPointerId(0);
                View toCapture = findTopChildUnder((int) x, (int) y);
                saveInitialMotion(x, y, i);
                tryCaptureViewForDrag(toCapture, i);
                edgesTouched = this.mInitialEdgesTouched[i];
                if ((this.mTrackingEdges & edgesTouched) != 0) {
                    this.mCallback.onEdgeTouched(this.mTrackingEdges & edgesTouched, i);
                    return;
                }
                return;
            case 1:
                if (this.mDragState == 1) {
                    releaseViewForPointerUp();
                }
                cancel();
                return;
            case 2:
                if (this.mDragState != 1) {
                    pointerCount = ev.getPointerCount();
                    while (i < pointerCount) {
                        pointerId = ev.getPointerId(i);
                        if (isValidPointerForActionMove(pointerId)) {
                            float x2 = ev.getX(i);
                            float y2 = ev.getY(i);
                            float dx = x2 - this.mInitialMotionX[pointerId];
                            float dy = y2 - this.mInitialMotionY[pointerId];
                            reportNewEdgeDrags(dx, dy, pointerId);
                            if (this.mDragState != 1) {
                                View toCapture2 = findTopChildUnder((int) x2, (int) y2);
                                if (checkTouchSlop(toCapture2, dx, dy) && tryCaptureViewForDrag(toCapture2, pointerId)) {
                                }
                            }
                            saveLastMotion(ev);
                            return;
                        }
                        i++;
                    }
                    saveLastMotion(ev);
                    return;
                } else if (isValidPointerForActionMove(this.mActivePointerId)) {
                    i = ev.findPointerIndex(this.mActivePointerId);
                    x = ev.getX(i);
                    pointerId = (int) (x - this.mLastMotionX[this.mActivePointerId]);
                    edgesTouched = (int) (ev.getY(i) - this.mLastMotionY[this.mActivePointerId]);
                    dragTo(this.mCapturedView.getLeft() + pointerId, this.mCapturedView.getTop() + edgesTouched, pointerId, edgesTouched);
                    saveLastMotion(ev);
                    return;
                } else {
                    return;
                }
            case 3:
                if (this.mDragState == 1) {
                    dispatchViewReleased(0.0f, 0.0f);
                }
                cancel();
                return;
            case 5:
                i = ev.getPointerId(actionIndex);
                x = ev.getX(actionIndex);
                y = ev.getY(actionIndex);
                saveInitialMotion(x, y, i);
                if (this.mDragState == 0) {
                    tryCaptureViewForDrag(findTopChildUnder((int) x, (int) y), i);
                    edgesTouched = this.mInitialEdgesTouched[i];
                    if ((this.mTrackingEdges & edgesTouched) != 0) {
                        this.mCallback.onEdgeTouched(this.mTrackingEdges & edgesTouched, i);
                        return;
                    }
                    return;
                } else if (isCapturedViewUnder((int) x, (int) y)) {
                    tryCaptureViewForDrag(this.mCapturedView, i);
                    return;
                } else {
                    return;
                }
            case 6:
                pointerCount = ev.getPointerId(actionIndex);
                if (this.mDragState == 1 && pointerCount == this.mActivePointerId) {
                    int newActivePointer = -1;
                    pointerId = ev.getPointerCount();
                    while (i < pointerId) {
                        edgesTouched = ev.getPointerId(i);
                        if (edgesTouched != this.mActivePointerId) {
                            if (findTopChildUnder((int) ev.getX(i), (int) ev.getY(i)) == this.mCapturedView && tryCaptureViewForDrag(this.mCapturedView, edgesTouched)) {
                                newActivePointer = this.mActivePointerId;
                                if (newActivePointer == -1) {
                                    releaseViewForPointerUp();
                                }
                            }
                        }
                        i++;
                    }
                    if (newActivePointer == -1) {
                    }
                }
                clearMotionHistory(pointerCount);
                return;
            default:
                return;
        }
    }

    private void reportNewEdgeDrags(float dx, float dy, int pointerId) {
        int dragsStarted = 0;
        if (checkNewEdgeDrag(dx, dy, pointerId, 1)) {
            dragsStarted = 0 | 1;
        }
        if (checkNewEdgeDrag(dy, dx, pointerId, 4)) {
            dragsStarted |= 4;
        }
        if (checkNewEdgeDrag(dx, dy, pointerId, 2)) {
            dragsStarted |= 2;
        }
        if (checkNewEdgeDrag(dy, dx, pointerId, 8)) {
            dragsStarted |= 8;
        }
        if (dragsStarted != 0) {
            int[] iArr = this.mEdgeDragsInProgress;
            iArr[pointerId] = iArr[pointerId] | dragsStarted;
            this.mCallback.onEdgeDragStarted(dragsStarted, pointerId);
        }
    }

    private boolean checkNewEdgeDrag(float delta, float odelta, int pointerId, int edge) {
        float absDelta = Math.abs(delta);
        float absODelta = Math.abs(odelta);
        boolean z = false;
        if ((this.mInitialEdgesTouched[pointerId] & edge) != edge || (this.mTrackingEdges & edge) == 0 || (this.mEdgeDragsLocked[pointerId] & edge) == edge || (this.mEdgeDragsInProgress[pointerId] & edge) == edge || (absDelta <= ((float) this.mTouchSlop) && absODelta <= ((float) this.mTouchSlop))) {
            return false;
        }
        if (absDelta >= 0.5f * absODelta || !this.mCallback.onEdgeLock(edge)) {
            if ((this.mEdgeDragsInProgress[pointerId] & edge) == 0 && absDelta > ((float) this.mTouchSlop)) {
                z = true;
            }
            return z;
        }
        int[] iArr = this.mEdgeDragsLocked;
        iArr[pointerId] = iArr[pointerId] | edge;
        return false;
    }

    private boolean checkTouchSlop(View child, float dx, float dy) {
        boolean z = false;
        if (child == null) {
            return false;
        }
        boolean checkHorizontal = this.mCallback.getViewHorizontalDragRange(child) > 0;
        boolean checkVertical = this.mCallback.getViewVerticalDragRange(child) > 0;
        if (checkHorizontal && checkVertical) {
            if ((dx * dx) + (dy * dy) > ((float) (this.mTouchSlop * this.mTouchSlop))) {
                z = true;
            }
            return z;
        } else if (checkHorizontal) {
            if (Math.abs(dx) > ((float) this.mTouchSlop)) {
                z = true;
            }
            return z;
        } else if (!checkVertical) {
            return false;
        } else {
            if (Math.abs(dy) > ((float) this.mTouchSlop)) {
                z = true;
            }
            return z;
        }
    }

    public boolean checkTouchSlop(int directions) {
        int count = this.mInitialMotionX.length;
        for (int i = 0; i < count; i++) {
            if (checkTouchSlop(directions, i)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkTouchSlop(int directions, int pointerId) {
        boolean z = false;
        if (!isPointerDown(pointerId)) {
            return false;
        }
        boolean checkHorizontal = (directions & 1) == 1;
        boolean checkVertical = (directions & 2) == 2;
        float dx = this.mLastMotionX[pointerId] - this.mInitialMotionX[pointerId];
        float dy = this.mLastMotionY[pointerId] - this.mInitialMotionY[pointerId];
        if (checkHorizontal && checkVertical) {
            if ((dx * dx) + (dy * dy) > ((float) (this.mTouchSlop * this.mTouchSlop))) {
                z = true;
            }
            return z;
        } else if (checkHorizontal) {
            if (Math.abs(dx) > ((float) this.mTouchSlop)) {
                z = true;
            }
            return z;
        } else if (!checkVertical) {
            return false;
        } else {
            if (Math.abs(dy) > ((float) this.mTouchSlop)) {
                z = true;
            }
            return z;
        }
    }

    public boolean isEdgeTouched(int edges) {
        int count = this.mInitialEdgesTouched.length;
        for (int i = 0; i < count; i++) {
            if (isEdgeTouched(edges, i)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEdgeTouched(int edges, int pointerId) {
        return isPointerDown(pointerId) && (this.mInitialEdgesTouched[pointerId] & edges) != 0;
    }

    private void releaseViewForPointerUp() {
        this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaxVelocity);
        dispatchViewReleased(clampMag(this.mVelocityTracker.getXVelocity(this.mActivePointerId), this.mMinVelocity, this.mMaxVelocity), clampMag(this.mVelocityTracker.getYVelocity(this.mActivePointerId), this.mMinVelocity, this.mMaxVelocity));
    }

    private void dragTo(int left, int top, int dx, int dy) {
        int i = dx;
        int i2 = dy;
        int clampedX = left;
        int clampedY = top;
        int oldLeft = this.mCapturedView.getLeft();
        int oldTop = this.mCapturedView.getTop();
        if (i != 0) {
            clampedX = this.mCallback.clampViewPositionHorizontal(this.mCapturedView, left, i);
            ViewCompat.offsetLeftAndRight(this.mCapturedView, clampedX - oldLeft);
        } else {
            int i3 = left;
        }
        if (i2 != 0) {
            clampedY = this.mCallback.clampViewPositionVertical(this.mCapturedView, top, i2);
            ViewCompat.offsetTopAndBottom(this.mCapturedView, clampedY - oldTop);
        } else {
            int i4 = top;
        }
        if (i != 0 || i2 != 0) {
            this.mCallback.onViewPositionChanged(this.mCapturedView, clampedX, clampedY, clampedX - oldLeft, clampedY - oldTop);
        }
    }

    public boolean isCapturedViewUnder(int x, int y) {
        return isViewUnder(this.mCapturedView, x, y);
    }

    public boolean isViewUnder(@Nullable View view, int x, int y) {
        boolean z = false;
        if (view == null) {
            return false;
        }
        if (x >= view.getLeft() && x < view.getRight() && y >= view.getTop() && y < view.getBottom()) {
            z = true;
        }
        return z;
    }

    @Nullable
    public View findTopChildUnder(int x, int y) {
        for (int i = this.mParentView.getChildCount() - 1; i >= 0; i--) {
            View child = this.mParentView.getChildAt(this.mCallback.getOrderedChildIndex(i));
            if (x >= child.getLeft() && x < child.getRight() && y >= child.getTop() && y < child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    private int getEdgesTouched(int x, int y) {
        int result = 0;
        if (x < this.mParentView.getLeft() + this.mEdgeSize) {
            result = 0 | 1;
        }
        if (y < this.mParentView.getTop() + this.mEdgeSize) {
            result |= 4;
        }
        if (x > this.mParentView.getRight() - this.mEdgeSize) {
            result |= 2;
        }
        if (y > this.mParentView.getBottom() - this.mEdgeSize) {
            return result | 8;
        }
        return result;
    }

    private boolean isValidPointerForActionMove(int pointerId) {
        if (isPointerDown(pointerId)) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ignoring pointerId=");
        stringBuilder.append(pointerId);
        stringBuilder.append(" because ACTION_DOWN was not received ");
        stringBuilder.append("for this pointer before ACTION_MOVE. It likely happened because ");
        stringBuilder.append(" ViewDragHelper did not receive all the events in the event stream.");
        Log.e(str, stringBuilder.toString());
        return false;
    }
}
