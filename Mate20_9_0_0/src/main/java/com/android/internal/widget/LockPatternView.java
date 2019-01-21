package com.android.internal.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hwcontrol.HwWidgetFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.IntArray;
import android.util.SparseArray;
import android.view.DisplayListCanvas;
import android.view.MotionEvent;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.View.MeasureSpec;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.internal.R;
import com.android.internal.os.PowerProfile;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class LockPatternView extends View {
    private static final long ANIM_DURATION = 100;
    private static final int ASPECT_LOCK_HEIGHT = 2;
    private static final int ASPECT_LOCK_WIDTH = 1;
    private static final int ASPECT_SQUARE = 0;
    public static final boolean DEBUG_A11Y = false;
    private static final float DRAG_THRESHHOLD = 0.0f;
    private static final int MILLIS_PER_CIRCLE_ANIMATING = 700;
    private static final boolean PROFILE_DRAWING = false;
    private static final String TAG = "LockPatternView";
    public static final int VIRTUAL_BASE_VIEW_ID = 1;
    private int mAlphaTransparent;
    private long mAnimatingPeriodStart;
    private int mAspect;
    private AudioManager mAudioManager;
    private final CellState[][] mCellStates;
    private final Path mCurrentPath;
    private final int mDotCircleRadius;
    private final int mDotRadius;
    private final int mDotRadiusActivated;
    private boolean mDrawingProfilingStarted;
    private boolean mEnableHapticFeedback;
    private int mErrorColor;
    private PatternExploreByTouchHelper mExploreByTouchHelper;
    private boolean mFadePattern;
    private final Interpolator mFastOutSlowInInterpolator;
    private float mHeight;
    private float mHitFactor;
    private float mInProgressX;
    private float mInProgressY;
    private boolean mInStealthMode;
    private boolean mInputEnabled;
    private final Interpolator mInterpolator;
    private final Rect mInvalidate;
    private boolean mIsHwTheme;
    private boolean mIsInKeyguard;
    private float mLastCellCenterX;
    private float mLastCellCenterY;
    private long[] mLineFadeStart;
    private final int mLineRadius;
    private final Interpolator mLinearOutSlowInInterpolator;
    private Drawable mNotSelectedDrawable;
    private OnPatternListener mOnPatternListener;
    private final Paint mPaint;
    private int mPathColor;
    private final Paint mPathPaint;
    private final ArrayList<Cell> mPattern;
    private DisplayMode mPatternDisplayMode;
    private final boolean[][] mPatternDrawLookup;
    private boolean mPatternInProgress;
    private int mPickedColor;
    private int mRegularColor;
    private Drawable mSelectedDrawable;
    private float mSquareHeight;
    private float mSquareWidth;
    private int mSuccessColor;
    private final Rect mTmpInvalidateRect;
    private boolean mUseLockPatternDrawable;
    private float mWidth;

    public static final class Cell {
        private static final Cell[][] sCells = createCells();
        final int column;
        final int row;

        private static Cell[][] createCells() {
            Cell[][] res = (Cell[][]) Array.newInstance(Cell.class, new int[]{3, 3});
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    res[i][j] = new Cell(i, j);
                }
            }
            return res;
        }

        private Cell(int row, int column) {
            checkRange(row, column);
            this.row = row;
            this.column = column;
        }

        public int getRow() {
            return this.row;
        }

        public int getColumn() {
            return this.column;
        }

        public static Cell of(int row, int column) {
            checkRange(row, column);
            return sCells[row][column];
        }

        private static void checkRange(int row, int column) {
            if (row < 0 || row > 2) {
                throw new IllegalArgumentException("row must be in range 0-2");
            } else if (column < 0 || column > 2) {
                throw new IllegalArgumentException("column must be in range 0-2");
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("(row=");
            stringBuilder.append(this.row);
            stringBuilder.append(",clmn=");
            stringBuilder.append(this.column);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    public static class CellState {
        float alpha = 1.0f;
        int col;
        boolean hwAnimating;
        CanvasProperty<Float> hwCenterX;
        CanvasProperty<Float> hwCenterY;
        CanvasProperty<Paint> hwPaint;
        CanvasProperty<Float> hwRadius;
        public ValueAnimator lineAnimator;
        public float lineEndX = Float.MIN_VALUE;
        public float lineEndY = Float.MIN_VALUE;
        public ValueAnimator moveAnimator;
        float radius;
        int row;
        float translationY;
    }

    public enum DisplayMode {
        Correct,
        Animate,
        Wrong
    }

    public interface OnPatternListener {
        void onPatternCellAdded(List<Cell> list);

        void onPatternCleared();

        void onPatternDetected(List<Cell> list);

        void onPatternStart();
    }

    private final class PatternExploreByTouchHelper extends ExploreByTouchHelper {
        private final SparseArray<VirtualViewContainer> mItems = new SparseArray();
        private Rect mTempRect = new Rect();

        class VirtualViewContainer {
            CharSequence description;

            public VirtualViewContainer(CharSequence description) {
                this.description = description;
            }
        }

        public PatternExploreByTouchHelper(View forView) {
            super(forView);
            for (int i = 1; i < 10; i++) {
                this.mItems.put(i, new VirtualViewContainer(getTextForVirtualView(i)));
            }
        }

        protected int getVirtualViewAt(float x, float y) {
            return getVirtualViewIdForHit(x, y);
        }

        protected void getVisibleVirtualViews(IntArray virtualViewIds) {
            if (LockPatternView.this.mPatternInProgress) {
                for (int i = 1; i < 10; i++) {
                    virtualViewIds.add(i);
                }
            }
        }

        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            VirtualViewContainer container = (VirtualViewContainer) this.mItems.get(virtualViewId);
            if (container != null) {
                event.getText().add(container.description);
            }
        }

        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onPopulateAccessibilityEvent(host, event);
            if (!LockPatternView.this.mPatternInProgress) {
                event.setContentDescription(LockPatternView.this.getContext().getText(17040344));
            }
        }

        protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfo node) {
            node.setText(getTextForVirtualView(virtualViewId));
            node.setContentDescription(getTextForVirtualView(virtualViewId));
            if (LockPatternView.this.mPatternInProgress) {
                node.setFocusable(true);
                if (isClickable(virtualViewId)) {
                    node.addAction(AccessibilityAction.ACTION_CLICK);
                    node.setClickable(isClickable(virtualViewId));
                }
            }
            node.setBoundsInParent(getBoundsForVirtualView(virtualViewId));
        }

        private boolean isClickable(int virtualViewId) {
            if (virtualViewId == Integer.MIN_VALUE) {
                return false;
            }
            return LockPatternView.this.mPatternDrawLookup[(virtualViewId - 1) / 3][(virtualViewId - 1) % 3] ^ 1;
        }

        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            if (action != 16) {
                return false;
            }
            return onItemClicked(virtualViewId);
        }

        boolean onItemClicked(int index) {
            invalidateVirtualView(index);
            sendEventForVirtualView(index, 1);
            return true;
        }

        private Rect getBoundsForVirtualView(int virtualViewId) {
            int ordinal = virtualViewId - 1;
            Rect bounds = this.mTempRect;
            int row = ordinal / 3;
            int col = ordinal % 3;
            CellState cell = LockPatternView.this.mCellStates[row][col];
            float centerX = LockPatternView.this.getCenterXForColumn(col);
            float centerY = LockPatternView.this.getCenterYForRow(row);
            float cellheight = (LockPatternView.this.mSquareHeight * LockPatternView.this.mHitFactor) * 0.5f;
            float cellwidth = (LockPatternView.this.mSquareWidth * LockPatternView.this.mHitFactor) * 0.5f;
            bounds.left = (int) (centerX - cellwidth);
            bounds.right = (int) (centerX + cellwidth);
            bounds.top = (int) (centerY - cellheight);
            bounds.bottom = (int) (centerY + cellheight);
            return bounds;
        }

        private CharSequence getTextForVirtualView(int virtualViewId) {
            return LockPatternView.this.getResources().getString(17040346, new Object[]{Integer.valueOf(virtualViewId)});
        }

        private int getVirtualViewIdForHit(float x, float y) {
            int rowHit = LockPatternView.this.getRowHit(y);
            int view = Integer.MIN_VALUE;
            if (rowHit < 0) {
                return Integer.MIN_VALUE;
            }
            int columnHit = LockPatternView.this.getColumnHit(x);
            if (columnHit < 0) {
                return Integer.MIN_VALUE;
            }
            int dotId = ((rowHit * 3) + columnHit) + 1;
            if (LockPatternView.this.mPatternDrawLookup[rowHit][columnHit]) {
                view = dotId;
            }
            return view;
        }
    }

    private static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        private final int mDisplayMode;
        private final boolean mInStealthMode;
        private final boolean mInputEnabled;
        private final String mSerializedPattern;
        private final boolean mTactileFeedbackEnabled;

        /* synthetic */ SavedState(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        /* synthetic */ SavedState(Parcelable x0, String x1, int x2, boolean x3, boolean x4, boolean x5, AnonymousClass1 x6) {
            this(x0, x1, x2, x3, x4, x5);
        }

        private SavedState(Parcelable superState, String serializedPattern, int displayMode, boolean inputEnabled, boolean inStealthMode, boolean tactileFeedbackEnabled) {
            super(superState);
            this.mSerializedPattern = serializedPattern;
            this.mDisplayMode = displayMode;
            this.mInputEnabled = inputEnabled;
            this.mInStealthMode = inStealthMode;
            this.mTactileFeedbackEnabled = tactileFeedbackEnabled;
        }

        private SavedState(Parcel in) {
            super(in);
            this.mSerializedPattern = in.readString();
            this.mDisplayMode = in.readInt();
            this.mInputEnabled = ((Boolean) in.readValue(null)).booleanValue();
            this.mInStealthMode = ((Boolean) in.readValue(null)).booleanValue();
            this.mTactileFeedbackEnabled = ((Boolean) in.readValue(null)).booleanValue();
        }

        public String getSerializedPattern() {
            return this.mSerializedPattern;
        }

        public int getDisplayMode() {
            return this.mDisplayMode;
        }

        public boolean isInputEnabled() {
            return this.mInputEnabled;
        }

        public boolean isInStealthMode() {
            return this.mInStealthMode;
        }

        public boolean isTactileFeedbackEnabled() {
            return this.mTactileFeedbackEnabled;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(this.mSerializedPattern);
            dest.writeInt(this.mDisplayMode);
            dest.writeValue(Boolean.valueOf(this.mInputEnabled));
            dest.writeValue(Boolean.valueOf(this.mInStealthMode));
            dest.writeValue(Boolean.valueOf(this.mTactileFeedbackEnabled));
        }
    }

    public LockPatternView(Context context) {
        this(context, null);
    }

    public LockPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDrawingProfilingStarted = false;
        this.mPaint = new Paint();
        this.mPathPaint = new Paint();
        this.mPattern = new ArrayList(9);
        this.mPatternDrawLookup = (boolean[][]) Array.newInstance(boolean.class, new int[]{3, 3});
        this.mInProgressX = -1.0f;
        this.mInProgressY = -1.0f;
        this.mLineFadeStart = new long[9];
        this.mPatternDisplayMode = DisplayMode.Correct;
        this.mInputEnabled = true;
        this.mInStealthMode = false;
        this.mEnableHapticFeedback = true;
        this.mPatternInProgress = false;
        this.mFadePattern = true;
        this.mHitFactor = 0.6f;
        this.mCurrentPath = new Path();
        this.mInvalidate = new Rect();
        this.mTmpInvalidateRect = new Rect();
        this.mInterpolator = new AccelerateInterpolator();
        this.mIsHwTheme = false;
        this.mLastCellCenterX = -1.0f;
        this.mLastCellCenterY = -1.0f;
        this.mAlphaTransparent = 128;
        this.mPathColor = -3355444;
        this.mIsInKeyguard = false;
        this.mPickedColor = 0;
        this.mIsHwTheme = HwWidgetFactory.checkIsHwTheme(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LockPatternView, 17891431, 16974999);
        String aspect = a.getString(0);
        if ("square".equals(aspect)) {
            this.mAspect = 0;
        } else if ("lock_width".equals(aspect)) {
            this.mAspect = 1;
        } else if ("lock_height".equals(aspect)) {
            this.mAspect = 2;
        } else {
            this.mAspect = 0;
        }
        setClickable(true);
        this.mPathPaint.setAntiAlias(true);
        this.mPathPaint.setDither(true);
        if (!this.mIsInKeyguard || this.mPickedColor == 0) {
            this.mRegularColor = context.getColor(33882269);
            this.mSuccessColor = context.getColor(33882270);
            this.mRegularColor = a.getColor(3, this.mRegularColor);
            this.mSuccessColor = a.getColor(4, this.mSuccessColor);
        } else {
            this.mRegularColor = this.mPickedColor;
            this.mSuccessColor = this.mPickedColor;
        }
        this.mErrorColor = a.getColor(1, 0);
        this.mPathPaint.setColor(a.getColor(2, this.mRegularColor));
        this.mPathPaint.setStyle(Style.FILL);
        this.mPathPaint.setStrokeJoin(Join.ROUND);
        this.mPathPaint.setStrokeCap(Cap.ROUND);
        this.mPathPaint.setStrokeWidth(2.0f);
        this.mLineRadius = getResources().getDimensionPixelSize(34472102);
        this.mDotCircleRadius = getResources().getDimensionPixelSize(34472101);
        this.mDotRadius = getResources().getDimensionPixelSize(34472099);
        this.mDotRadiusActivated = getResources().getDimensionPixelSize(34472100);
        this.mUseLockPatternDrawable = getResources().getBoolean(17957114);
        if (this.mUseLockPatternDrawable) {
            this.mSelectedDrawable = getResources().getDrawable(17302944);
            this.mNotSelectedDrawable = getResources().getDrawable(17302942);
        }
        this.mPaint.setAntiAlias(true);
        this.mPaint.setDither(true);
        this.mCellStates = (CellState[][]) Array.newInstance(CellState.class, new int[]{3, 3});
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.mCellStates[i][j] = new CellState();
                this.mCellStates[i][j].radius = (float) this.mDotRadius;
                this.mCellStates[i][j].row = i;
                this.mCellStates[i][j].col = j;
            }
        }
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mExploreByTouchHelper = new PatternExploreByTouchHelper(this);
        setAccessibilityDelegate(this.mExploreByTouchHelper);
        this.mAudioManager = (AudioManager) this.mContext.getSystemService(PowerProfile.POWER_AUDIO);
        a.recycle();
    }

    public CellState[][] getCellStates() {
        return this.mCellStates;
    }

    public boolean isInStealthMode() {
        return this.mInStealthMode;
    }

    public boolean isTactileFeedbackEnabled() {
        return this.mEnableHapticFeedback;
    }

    public void setInStealthMode(boolean inStealthMode) {
        this.mInStealthMode = inStealthMode;
    }

    public void setFadePattern(boolean fadePattern) {
        this.mFadePattern = fadePattern;
    }

    public void setTactileFeedbackEnabled(boolean tactileFeedbackEnabled) {
        this.mEnableHapticFeedback = tactileFeedbackEnabled;
    }

    public void setOnPatternListener(OnPatternListener onPatternListener) {
        this.mOnPatternListener = onPatternListener;
    }

    public void setPattern(DisplayMode displayMode, List<Cell> pattern) {
        this.mPattern.clear();
        this.mPattern.addAll(pattern);
        clearPatternDrawLookup();
        for (Cell cell : pattern) {
            this.mPatternDrawLookup[cell.getRow()][cell.getColumn()] = true;
        }
        setDisplayMode(displayMode);
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.mPatternDisplayMode = displayMode;
        if (displayMode == DisplayMode.Animate) {
            if (this.mPattern.size() != 0) {
                this.mAnimatingPeriodStart = SystemClock.elapsedRealtime();
                Cell first = (Cell) this.mPattern.get(0);
                this.mInProgressX = getCenterXForColumn(first.getColumn());
                this.mInProgressY = getCenterYForRow(first.getRow());
                clearPatternDrawLookup();
            } else {
                throw new IllegalStateException("you must have a pattern to animate if you want to set the display mode to animate");
            }
        }
        invalidate();
    }

    public void startCellStateAnimation(CellState cellState, float startAlpha, float endAlpha, float startTranslationY, float endTranslationY, float startScale, float endScale, long delay, long duration, Interpolator interpolator, Runnable finishRunnable) {
        if (isHardwareAccelerated()) {
            startCellStateAnimationHw(cellState, startAlpha, endAlpha, startTranslationY, endTranslationY, startScale, endScale, delay, duration, interpolator, finishRunnable);
        } else {
            startCellStateAnimationSw(cellState, startAlpha, endAlpha, startTranslationY, endTranslationY, startScale, endScale, delay, duration, interpolator, finishRunnable);
        }
    }

    private void startCellStateAnimationSw(CellState cellState, float startAlpha, float endAlpha, float startTranslationY, float endTranslationY, float startScale, float endScale, long delay, long duration, Interpolator interpolator, Runnable finishRunnable) {
        CellState cellState2 = cellState;
        float f = startAlpha;
        cellState2.alpha = f;
        float f2 = startTranslationY;
        cellState2.translationY = f2;
        cellState2.radius = ((float) this.mDotRadius) * startScale;
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.setInterpolator(interpolator);
        final CellState cellState3 = cellState2;
        final float f3 = f;
        AnonymousClass1 anonymousClass1 = r0;
        final float f4 = endAlpha;
        final float f5 = f2;
        final float f6 = endTranslationY;
        final float f7 = startScale;
        final float f8 = endScale;
        AnonymousClass1 anonymousClass12 = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                cellState3.alpha = ((1.0f - t) * f3) + (f4 * t);
                cellState3.translationY = ((1.0f - t) * f5) + (f6 * t);
                cellState3.radius = ((float) LockPatternView.this.mDotRadius) * (((1.0f - t) * f7) + (f8 * t));
                LockPatternView.this.invalidate();
            }
        };
        animator.addUpdateListener(anonymousClass1);
        final Runnable runnable = finishRunnable;
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        animator.start();
    }

    private void startCellStateAnimationHw(CellState cellState, float startAlpha, float endAlpha, float startTranslationY, float endTranslationY, float startScale, float endScale, long delay, long duration, Interpolator interpolator, Runnable finishRunnable) {
        final CellState cellState2 = cellState;
        float f = endTranslationY;
        float f2 = endAlpha;
        cellState2.alpha = f2;
        cellState2.translationY = f;
        cellState2.radius = ((float) this.mDotRadius) * endScale;
        cellState2.hwAnimating = true;
        cellState2.hwCenterY = CanvasProperty.createFloat(getCenterYForRow(cellState2.row) + startTranslationY);
        cellState2.hwCenterX = CanvasProperty.createFloat(getCenterXForColumn(cellState2.col));
        cellState2.hwRadius = CanvasProperty.createFloat(((float) this.mDotRadius) * startScale);
        this.mPaint.setColor(getCurrentColor(false));
        this.mPaint.setAlpha((int) (255.0f * startAlpha));
        cellState2.hwPaint = CanvasProperty.createPaint(new Paint(this.mPaint));
        long j = delay;
        long j2 = duration;
        Interpolator interpolator2 = interpolator;
        startRtFloatAnimation(cellState2.hwCenterY, getCenterYForRow(cellState2.row) + f, j, j2, interpolator2);
        startRtFloatAnimation(cellState2.hwRadius, ((float) this.mDotRadius) * endScale, j, j2, interpolator2);
        final Runnable runnable = finishRunnable;
        startRtAlphaAnimation(cellState2, f2, j, j2, interpolator, new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                cellState2.hwAnimating = false;
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        invalidate();
    }

    private void startRtAlphaAnimation(CellState cellState, float endAlpha, long delay, long duration, Interpolator interpolator, AnimatorListener listener) {
        RenderNodeAnimator animator = new RenderNodeAnimator(cellState.hwPaint, 1, (float) ((int) (255.0f * endAlpha)));
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.setInterpolator(interpolator);
        animator.setTarget(this);
        animator.addListener(listener);
        animator.start();
    }

    private void startRtFloatAnimation(CanvasProperty<Float> property, float endValue, long delay, long duration, Interpolator interpolator) {
        RenderNodeAnimator animator = new RenderNodeAnimator(property, endValue);
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.setInterpolator(interpolator);
        animator.setTarget(this);
        animator.start();
    }

    private void notifyCellAdded() {
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternCellAdded(this.mPattern);
        }
        this.mExploreByTouchHelper.invalidateRoot();
    }

    private void notifyPatternStarted() {
        sendAccessEvent(17040349);
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternStart();
        }
    }

    private void notifyPatternDetected() {
        sendAccessEvent(17040348);
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternDetected(this.mPattern);
        }
    }

    private void notifyPatternCleared() {
        sendAccessEvent(17040347);
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternCleared();
        }
    }

    public void clearPattern() {
        resetPattern();
    }

    protected boolean dispatchHoverEvent(MotionEvent event) {
        return super.dispatchHoverEvent(event) | this.mExploreByTouchHelper.dispatchHoverEvent(event);
    }

    private void resetPattern() {
        this.mPattern.clear();
        clearPatternDrawLookup();
        resetCellRadius();
        this.mPatternDisplayMode = DisplayMode.Correct;
        invalidate();
    }

    private void clearPatternDrawLookup() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.mPatternDrawLookup[i][j] = false;
                this.mLineFadeStart[i + j] = 0;
            }
        }
    }

    public void disableInput() {
        this.mInputEnabled = false;
    }

    public void enableInput() {
        this.mInputEnabled = true;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int width = (w - this.mPaddingLeft) - this.mPaddingRight;
        this.mSquareWidth = ((float) width) / 3.0f;
        int height = (h - this.mPaddingTop) - this.mPaddingBottom;
        this.mSquareHeight = ((float) height) / 3.0f;
        this.mExploreByTouchHelper.invalidateRoot();
        if (this.mUseLockPatternDrawable) {
            this.mNotSelectedDrawable.setBounds(this.mPaddingLeft, this.mPaddingTop, width, height);
            this.mSelectedDrawable.setBounds(this.mPaddingLeft, this.mPaddingTop, width, height);
        }
        this.mWidth = (float) w;
        this.mHeight = (float) h;
        if (!this.mPattern.isEmpty()) {
            Cell lastCell = (Cell) this.mPattern.get(this.mPattern.size() - 1);
            if (lastCell != null) {
                this.mLastCellCenterX = getCenterXForColumn(lastCell.getColumn());
                this.mLastCellCenterY = getCenterYForRow(lastCell.getRow());
            }
        }
    }

    private int resolveMeasured(int measureSpec, int desired) {
        int specSize = MeasureSpec.getSize(measureSpec);
        int mode = MeasureSpec.getMode(measureSpec);
        if (mode == Integer.MIN_VALUE) {
            return Math.max(specSize, desired);
        }
        if (mode != 0) {
            return specSize;
        }
        return desired;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minimumWidth = getSuggestedMinimumWidth();
        int minimumHeight = getSuggestedMinimumHeight();
        int viewWidth = resolveMeasured(widthMeasureSpec, minimumWidth);
        int viewHeight = resolveMeasured(heightMeasureSpec, minimumHeight);
        switch (this.mAspect) {
            case 0:
                int min = Math.min(viewWidth, viewHeight);
                viewHeight = min;
                viewWidth = min;
                break;
            case 1:
                viewHeight = Math.min(viewWidth, viewHeight);
                break;
            case 2:
                viewWidth = Math.min(viewWidth, viewHeight);
                break;
        }
        setMeasuredDimension(viewWidth, viewHeight);
    }

    private Cell detectAndAddHit(float x, float y) {
        Cell cell = checkForNewHit(x, y);
        if (cell == null) {
            return null;
        }
        Cell fillInGapCell = null;
        ArrayList<Cell> pattern = this.mPattern;
        if (!pattern.isEmpty()) {
            Cell lastCell = (Cell) pattern.get(pattern.size() - 1);
            int dRow = cell.row - lastCell.row;
            int dColumn = cell.column - lastCell.column;
            int fillInRow = lastCell.row;
            int fillInColumn = lastCell.column;
            int i = -1;
            if (Math.abs(dRow) == 2 && Math.abs(dColumn) != 1) {
                fillInRow = lastCell.row + (dRow > 0 ? 1 : -1);
            }
            if (Math.abs(dColumn) == 2 && Math.abs(dRow) != 1) {
                int i2 = lastCell.column;
                if (dColumn > 0) {
                    i = 1;
                }
                fillInColumn = i2 + i;
            }
            fillInGapCell = Cell.of(fillInRow, fillInColumn);
        }
        Cell fillInGapCell2 = fillInGapCell;
        if (!(fillInGapCell2 == null || this.mPatternDrawLookup[fillInGapCell2.row][fillInGapCell2.column])) {
            addCellToPattern(fillInGapCell2);
        }
        if (this.mIsHwTheme && !this.mInStealthMode) {
            moveToTouchArea(cell, getCenterXForColumn(cell.getColumn()), getCenterYForRow(cell.getRow()), x, y, true);
        }
        addCellToPattern(cell);
        if (this.mEnableHapticFeedback) {
            performHapticFeedback(1, 3);
        }
        return cell;
    }

    private void addCellToPattern(Cell newCell) {
        this.mPatternDrawLookup[newCell.getRow()][newCell.getColumn()] = true;
        this.mPattern.add(newCell);
        if (!this.mInStealthMode) {
            startCellActivatedAnimation(newCell);
        }
        notifyCellAdded();
    }

    private void startCellActivatedAnimation(Cell cell) {
        final CellState cellState = this.mCellStates[cell.row][cell.column];
        if (this.mIsHwTheme) {
            enlargeCellAnimation((float) this.mDotRadius, (float) this.mDotRadiusActivated, this.mLinearOutSlowInInterpolator, cellState);
            return;
        }
        startRadiusAnimation((float) this.mDotRadius, (float) this.mDotRadiusActivated, 96, this.mLinearOutSlowInInterpolator, cellState, new Runnable() {
            public void run() {
                LockPatternView.this.startRadiusAnimation((float) LockPatternView.this.mDotRadiusActivated, (float) LockPatternView.this.mDotRadius, 192, LockPatternView.this.mFastOutSlowInInterpolator, cellState, null);
            }
        });
        startLineEndAnimation(cellState, this.mInProgressX, this.mInProgressY, getCenterXForColumn(cell.column), getCenterYForRow(cell.row));
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0003, element type: float, insn element type: null */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void startLineEndAnimation(final CellState state, float startX, float startY, float targetX, float targetY) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        final CellState cellState = state;
        final float f = startX;
        final float f2 = targetX;
        final float f3 = startY;
        final float f4 = targetY;
        valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                cellState.lineEndX = ((1.0f - t) * f) + (f2 * t);
                cellState.lineEndY = ((1.0f - t) * f3) + (f4 * t);
                LockPatternView.this.invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                state.lineAnimator = null;
            }
        });
        valueAnimator.setInterpolator(this.mFastOutSlowInInterpolator);
        valueAnimator.setDuration(ANIM_DURATION);
        valueAnimator.start();
        state.lineAnimator = valueAnimator;
    }

    private void startRadiusAnimation(float start, float end, long duration, Interpolator interpolator, final CellState state, final Runnable endRunnable) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(new float[]{start, end});
        valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                state.radius = ((Float) animation.getAnimatedValue()).floatValue();
                LockPatternView.this.invalidate();
            }
        });
        if (endRunnable != null) {
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    endRunnable.run();
                }
            });
        }
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    private Cell checkForNewHit(float x, float y) {
        int rowHit = getRowHit(y);
        if (rowHit < 0) {
            return null;
        }
        int columnHit = getColumnHit(x);
        if (columnHit >= 0 && !this.mPatternDrawLookup[rowHit][columnHit]) {
            return Cell.of(rowHit, columnHit);
        }
        return null;
    }

    private int getRowHit(float y) {
        float squareHeight = this.mSquareHeight;
        float hitSize = this.mHitFactor * squareHeight;
        float offset = ((float) this.mPaddingTop) + ((squareHeight - hitSize) / 2.0f);
        for (int i = 0; i < 3; i++) {
            float hitTop = (((float) i) * squareHeight) + offset;
            if (y >= hitTop && y <= hitTop + hitSize) {
                return i;
            }
        }
        return -1;
    }

    private int getColumnHit(float x) {
        float squareWidth = this.mSquareWidth;
        float hitSize = this.mHitFactor * squareWidth;
        float offset = ((float) this.mPaddingLeft) + ((squareWidth - hitSize) / 2.0f);
        for (int i = 0; i < 3; i++) {
            float hitLeft = (((float) i) * squareWidth) + offset;
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i;
            }
        }
        return -1;
    }

    public boolean onHoverEvent(MotionEvent event) {
        if (AccessibilityManager.getInstance(this.mContext).isTouchExplorationEnabled()) {
            int action = event.getAction();
            if (action != 7) {
                switch (action) {
                    case 9:
                        event.setAction(0);
                        break;
                    case 10:
                        event.setAction(1);
                        break;
                }
            }
            event.setAction(2);
            onTouchEvent(event);
            event.setAction(action);
        }
        return super.onHoverEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mInputEnabled || !isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case 0:
                handleActionDown(event);
                return true;
            case 1:
                handleActionUp();
                return true;
            case 2:
                handleActionMove(event);
                return true;
            case 3:
                if (this.mPatternInProgress) {
                    setPatternInProgress(false);
                    resetPattern();
                    notifyPatternCleared();
                }
                return true;
            default:
                return false;
        }
    }

    private void setPatternInProgress(boolean progress) {
        this.mPatternInProgress = progress;
        this.mExploreByTouchHelper.invalidateRoot();
    }

    private void handleActionMove(MotionEvent event) {
        MotionEvent motionEvent = event;
        int historySize = event.getHistorySize();
        this.mTmpInvalidateRect.setEmpty();
        boolean invalidateNow = false;
        int i = 0;
        while (i < historySize + 1) {
            int historySize2;
            boolean invalidateNow2;
            float x = i < historySize ? motionEvent.getHistoricalX(i) : event.getX();
            float y = i < historySize ? motionEvent.getHistoricalY(i) : event.getY();
            Cell hitCell = detectAndAddHit(x, y);
            int patternSize = this.mPattern.size();
            if (hitCell != null && patternSize == 1) {
                setPatternInProgress(true);
                notifyPatternStarted();
            }
            float dx = Math.abs(x - this.mInProgressX);
            float dy = Math.abs(y - this.mInProgressY);
            if (dx > 0.0f || dy > 0.0f) {
                invalidateNow = true;
            }
            if (!this.mPatternInProgress || patternSize <= 0) {
                historySize2 = historySize;
                invalidateNow2 = invalidateNow;
            } else {
                ArrayList<Cell> pattern = this.mPattern;
                Cell lastCell = (Cell) pattern.get(patternSize - 1);
                float lastCellCenterX = getCenterXForColumn(lastCell.column);
                float lastCellCenterY = getCenterYForRow(lastCell.row);
                float left = Math.min(lastCellCenterX, x);
                historySize2 = historySize;
                historySize = Math.max(lastCellCenterX, x);
                invalidateNow2 = invalidateNow;
                invalidateNow = Math.min(lastCellCenterY, y);
                float bottom = Math.max(lastCellCenterY, y);
                if (hitCell != null) {
                    dx = this.mSquareWidth * 0.5f;
                    dy = this.mSquareHeight * 0.5f;
                    pattern = getCenterXForColumn(hitCell.column);
                    lastCellCenterX = getCenterYForRow(hitCell.row);
                    left = Math.min(pattern - dx, left);
                    historySize = Math.max(pattern + dx, historySize);
                    invalidateNow = Math.min(lastCellCenterX - dy, invalidateNow);
                    bottom = Math.max(lastCellCenterX + dy, bottom);
                } else {
                    float f = dy;
                    ArrayList<Cell> arrayList = pattern;
                    float f2 = lastCellCenterX;
                    float f3 = lastCellCenterY;
                }
                if (hitCell == null && this.mIsHwTheme && !this.mInStealthMode) {
                    lastCellAnimation(lastCell, x, y);
                }
                this.mTmpInvalidateRect.union(Math.round(left), Math.round(invalidateNow), Math.round(historySize), Math.round(bottom));
            }
            i++;
            historySize = historySize2;
            invalidateNow = invalidateNow2;
        }
        this.mInProgressX = event.getX();
        this.mInProgressY = event.getY();
        if (invalidateNow) {
            this.mInvalidate.union(this.mTmpInvalidateRect);
            invalidate(this.mInvalidate);
            this.mInvalidate.set(this.mTmpInvalidateRect);
        }
    }

    private void sendAccessEvent(int resId) {
        announceForAccessibility(this.mContext.getString(resId));
    }

    private void handleActionUp() {
        if (!this.mPattern.isEmpty()) {
            backToCenterAfterActionUp();
            setPatternInProgress(false);
            cancelLineAnimations();
            notifyPatternDetected();
            if (this.mFadePattern) {
                clearPatternDrawLookup();
                this.mPatternDisplayMode = DisplayMode.Correct;
            }
            invalidate();
        }
    }

    private void cancelLineAnimations() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                CellState state = this.mCellStates[i][j];
                if (state.lineAnimator != null) {
                    state.lineAnimator.cancel();
                    state.lineEndX = Float.MIN_VALUE;
                    state.lineEndY = Float.MIN_VALUE;
                }
                if (state.moveAnimator != null) {
                    state.moveAnimator.cancel();
                }
            }
        }
    }

    private void handleActionDown(MotionEvent event) {
        resetPattern();
        float x = event.getX();
        float y = event.getY();
        Cell hitCell = detectAndAddHit(x, y);
        if (hitCell != null) {
            setPatternInProgress(true);
            this.mPatternDisplayMode = DisplayMode.Correct;
            notifyPatternStarted();
        } else if (this.mPatternInProgress) {
            setPatternInProgress(false);
            notifyPatternCleared();
        }
        if (hitCell != null) {
            float startX = getCenterXForColumn(hitCell.column);
            float startY = getCenterYForRow(hitCell.row);
            float widthOffset = this.mSquareWidth / 2.0f;
            float heightOffset = this.mSquareHeight / 2.0f;
            invalidate((int) (startX - widthOffset), (int) (startY - heightOffset), (int) (startX + widthOffset), (int) (startY + heightOffset));
        }
        this.mInProgressX = x;
        this.mInProgressY = y;
    }

    private float getCenterXForColumn(int column) {
        return (((float) this.mPaddingLeft) + (((float) column) * this.mSquareWidth)) + (this.mSquareWidth / 2.0f);
    }

    private float getCenterYForRow(int row) {
        return (((float) this.mPaddingTop) + (((float) row) * this.mSquareHeight)) + (this.mSquareHeight / 2.0f);
    }

    protected void onDraw(Canvas canvas) {
        int oneCycle;
        float centerX;
        float centerY;
        Cell nextCell;
        float dy;
        int i;
        float translationY;
        int count;
        Canvas canvas2 = canvas;
        if (this.mIsInKeyguard && this.mPickedColor != 0) {
            this.mRegularColor = this.mPickedColor;
            this.mSuccessColor = this.mPickedColor;
            this.mPathColor = this.mPickedColor;
        }
        ArrayList<Cell> pattern = this.mPattern;
        int count2 = pattern.size();
        boolean[][] drawLookup = this.mPatternDrawLookup;
        if (this.mPatternDisplayMode == DisplayMode.Animate) {
            oneCycle = (count2 + 1) * 700;
            int spotInCycle = ((int) (SystemClock.elapsedRealtime() - this.mAnimatingPeriodStart)) % oneCycle;
            int numCircles = spotInCycle / 700;
            clearPatternDrawLookup();
            for (int i2 = 0; i2 < numCircles; i2++) {
                Cell cell = (Cell) pattern.get(i2);
                drawLookup[cell.getRow()][cell.getColumn()] = true;
            }
            boolean needToUpdateInProgressPoint = numCircles > 0 && numCircles < count2;
            if (needToUpdateInProgressPoint) {
                centerX = ((float) (spotInCycle % 700)) / 700.0f;
                Cell currentCell = (Cell) pattern.get(numCircles - 1);
                float centerX2 = getCenterXForColumn(currentCell.column);
                centerY = getCenterYForRow(currentCell.row);
                nextCell = (Cell) pattern.get(numCircles);
                dy = (getCenterYForRow(nextCell.row) - centerY) * centerX;
                this.mInProgressX = centerX2 + ((getCenterXForColumn(nextCell.column) - centerX2) * centerX);
                this.mInProgressY = centerY + dy;
            }
            invalidate();
        }
        Path currentPath = this.mCurrentPath;
        currentPath.rewind();
        oneCycle = 0;
        while (true) {
            int i3 = oneCycle;
            i = 3;
            if (i3 >= 3) {
                break;
            }
            oneCycle = 0;
            while (true) {
                int j = oneCycle;
                if (j >= i) {
                    break;
                }
                CellState cellState = this.mCellStates[i3][j];
                oneCycle = getCenterYForRow(i3);
                float centerX3 = getCenterXForColumn(j);
                if (!this.mInStealthMode && this.mIsHwTheme && isLastCell(Cell.of(i3, j))) {
                    centerX3 = this.mLastCellCenterX;
                    oneCycle = this.mLastCellCenterY;
                }
                int centerY2 = oneCycle;
                centerX = centerX3;
                translationY = cellState.translationY;
                if (this.mUseLockPatternDrawable) {
                    count = count2;
                    count2 = centerY2;
                    drawCellDrawable(canvas2, i3, j, cellState.radius, drawLookup[i3][j]);
                } else {
                    float translationY2 = translationY;
                    centerY = centerX;
                    count = count2;
                    count2 = centerY2;
                    if (isHardwareAccelerated() && cellState.hwAnimating) {
                        ((DisplayListCanvas) canvas2).drawCircle(cellState.hwCenterX, cellState.hwCenterY, cellState.hwRadius, cellState.hwPaint);
                    } else {
                        drawCircle(canvas2, (float) ((int) centerY), ((float) ((int) count2)) + translationY2, cellState.radius, drawLookup[i3][j], cellState.alpha);
                    }
                }
                oneCycle = j + 1;
                count2 = count;
                i = 3;
            }
            oneCycle = i3 + 1;
        }
        count = count2;
        boolean drawPath = this.mInStealthMode ^ true;
        boolean z;
        if (!drawPath) {
            i = count;
        } else if (this.mIsHwTheme) {
            this.mPathPaint.setStyle(Style.FILL);
            this.mPathPaint.setColor(this.mPathColor);
            this.mPathPaint.setAlpha(this.mAlphaTransparent);
            this.mPathPaint.setStrokeWidth(2.0f);
            drawHwPath(pattern, drawLookup, currentPath, canvas2);
            z = drawPath;
            i = count;
        } else {
            this.mPathPaint.setColor(getCurrentColor(true));
            boolean anyCircles = false;
            translationY = 0.0f;
            centerX = 0.0f;
            long elapsedRealtime = SystemClock.elapsedRealtime();
            int i4 = 0;
            while (true) {
                count2 = i4;
                i = count;
                long j2;
                if (count2 >= i) {
                    j2 = elapsedRealtime;
                    break;
                }
                nextCell = (Cell) pattern.get(count2);
                if (!drawLookup[nextCell.row][nextCell.column]) {
                    z = drawPath;
                    j2 = elapsedRealtime;
                    break;
                }
                boolean anyCircles2;
                float f;
                if (this.mLineFadeStart[count2] == 0) {
                    this.mLineFadeStart[count2] = SystemClock.elapsedRealtime();
                }
                float centerX4 = getCenterXForColumn(nextCell.column);
                dy = getCenterYForRow(nextCell.row);
                if (count2 != 0) {
                    z = drawPath;
                    anyCircles2 = true;
                    f = 2.0f;
                    drawPath = (int) Math.min(((float) (elapsedRealtime - this.mLineFadeStart[count2])) / true, 255.0f);
                    j2 = elapsedRealtime;
                    anyCircles = this.mCellStates[nextCell.row][nextCell.column];
                    currentPath.rewind();
                    currentPath.moveTo(translationY, centerX);
                    if (anyCircles.lineEndX == Float.MIN_VALUE || anyCircles.lineEndY == Float.MIN_VALUE) {
                        currentPath.lineTo(centerX4, dy);
                        if (this.mFadePattern) {
                            this.mPathPaint.setAlpha(255 - drawPath);
                        } else {
                            this.mPathPaint.setAlpha(255);
                        }
                    } else {
                        currentPath.lineTo(anyCircles.lineEndX, anyCircles.lineEndY);
                        if (this.mFadePattern) {
                            this.mPathPaint.setAlpha(255 - drawPath);
                        } else {
                            this.mPathPaint.setAlpha(255);
                        }
                    }
                    canvas2.drawPath(currentPath, this.mPathPaint);
                } else {
                    z = drawPath;
                    anyCircles2 = true;
                    j2 = elapsedRealtime;
                    f = 2.0f;
                }
                translationY = centerX4;
                centerX = dy;
                count = i;
                centerX4 = f;
                anyCircles = anyCircles2;
                elapsedRealtime = j2;
                boolean i42 = count2 + 1;
                drawPath = z;
            }
            if ((this.mPatternInProgress || this.mPatternDisplayMode == DisplayMode.Animate) && anyCircles) {
                currentPath.rewind();
                currentPath.moveTo(translationY, centerX);
                currentPath.lineTo(this.mInProgressX, this.mInProgressY);
                this.mPathPaint.setAlpha((int) (calculateLastSegmentAlpha(this.mInProgressX, this.mInProgressY, translationY, centerX) * 255.0f));
                canvas2.drawPath(currentPath, this.mPathPaint);
            }
        }
    }

    private float calculateLastSegmentAlpha(float x, float y, float lastX, float lastY) {
        float diffX = x - lastX;
        float diffY = y - lastY;
        return Math.min(1.0f, Math.max(0.0f, ((((float) Math.sqrt((double) ((diffX * diffX) + (diffY * diffY)))) / this.mSquareWidth) - 0.3f) * 4.0f));
    }

    private int getCurrentColor(boolean partOfPattern) {
        if (!partOfPattern || this.mInStealthMode || this.mPatternInProgress) {
            return this.mRegularColor;
        }
        if (this.mPatternDisplayMode == DisplayMode.Wrong) {
            return this.mErrorColor;
        }
        if (this.mPatternDisplayMode == DisplayMode.Correct || this.mPatternDisplayMode == DisplayMode.Animate) {
            return this.mSuccessColor;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown display mode ");
        stringBuilder.append(this.mPatternDisplayMode);
        throw new IllegalStateException(stringBuilder.toString());
    }

    private void drawCircle(Canvas canvas, float centerX, float centerY, float radius, boolean partOfPattern, float alpha) {
        this.mPaint.setColor(getCurrentColor(partOfPattern));
        this.mPaint.setAlpha((int) (255.0f * alpha));
        canvas.drawCircle(centerX, centerY, radius, this.mPaint);
    }

    private void drawCellDrawable(Canvas canvas, int i, int j, float radius, boolean partOfPattern) {
        Rect dst = new Rect((int) (((float) this.mPaddingLeft) + (((float) j) * this.mSquareWidth)), (int) (((float) this.mPaddingTop) + (((float) i) * this.mSquareHeight)), (int) (((float) this.mPaddingLeft) + (((float) (j + 1)) * this.mSquareWidth)), (int) (((float) this.mPaddingTop) + (((float) (i + 1)) * this.mSquareHeight)));
        float scale = radius / ((float) this.mDotRadius);
        canvas.save();
        canvas.clipRect(dst);
        canvas.scale(scale, scale, (float) dst.centerX(), (float) dst.centerY());
        if (!partOfPattern || scale > 1.0f) {
            this.mNotSelectedDrawable.draw(canvas);
        } else {
            this.mSelectedDrawable.draw(canvas);
        }
        canvas.restore();
    }

    protected Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), LockPatternUtils.patternToString(this.mPattern), this.mPatternDisplayMode.ordinal(), this.mInputEnabled, this.mInStealthMode, this.mEnableHapticFeedback, null);
    }

    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setPattern(DisplayMode.Correct, LockPatternUtils.stringToPattern(ss.getSerializedPattern()));
        this.mPatternDisplayMode = DisplayMode.values()[ss.getDisplayMode()];
        this.mInputEnabled = ss.isInputEnabled();
        this.mInStealthMode = ss.isInStealthMode();
        this.mEnableHapticFeedback = ss.isTactileFeedbackEnabled();
    }

    private boolean isLastCell(Cell cell) {
        if (this.mPattern.isEmpty() || cell != this.mPattern.get(this.mPattern.size() - 1)) {
            return false;
        }
        return true;
    }

    private Cell touchACell(float x, float y) {
        int rowHit = getRowHit(y);
        if (rowHit < 0) {
            return null;
        }
        int columnHit = getColumnHit(x);
        if (columnHit < 0) {
            return null;
        }
        return Cell.of(rowHit, columnHit);
    }

    private void moveToTouchArea(Cell cell, float currCenterX, float currCenterY, float touchX, float touchY, boolean hasAnimation) {
        int row = cell.getRow();
        int column = cell.getColumn();
        if (hasAnimation) {
            final CellState cellState = this.mCellStates[row][column];
            if (cellState.moveAnimator != null) {
                cellState.moveAnimator.cancel();
            }
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
            final float f = currCenterX;
            final float f2 = touchX;
            final float f3 = currCenterY;
            final float f4 = touchY;
            final int i = row;
            final int i2 = column;
            valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = ((Float) animation.getAnimatedValue()).floatValue();
                    LockPatternView.this.mLastCellCenterX = ((1.0f - t) * f) + (f2 * t);
                    LockPatternView.this.mLastCellCenterY = ((1.0f - t) * f3) + (f4 * t);
                    LockPatternView.this.mCellStates[i][i2].hwCenterX = CanvasProperty.createFloat(LockPatternView.this.mLastCellCenterX);
                    LockPatternView.this.mCellStates[i][i2].hwCenterY = CanvasProperty.createFloat(LockPatternView.this.mLastCellCenterY);
                    LockPatternView.this.invalidate();
                }
            });
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    cellState.moveAnimator = null;
                }
            });
            valueAnimator.setDuration(ANIM_DURATION);
            valueAnimator.setInterpolator(this.mInterpolator);
            if (cellState.moveAnimator == null) {
                valueAnimator.start();
                cellState.moveAnimator = valueAnimator;
            }
            float f5 = touchX;
            float f6 = touchY;
            return;
        }
        this.mLastCellCenterX = touchX;
        this.mLastCellCenterY = touchY;
        this.mCellStates[row][column].hwCenterX = CanvasProperty.createFloat(touchX);
        this.mCellStates[row][column].hwCenterY = CanvasProperty.createFloat(touchY);
    }

    private void cellBackToCenter(Cell cell, float currentX, float currentY) {
        int row = cell.getRow();
        int column = cell.getColumn();
        float centerX = getCenterXForColumn(column);
        float centerY = getCenterYForRow(row);
        final CellState cellState = this.mCellStates[row][column];
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        final float f = currentX;
        final float f2 = centerX;
        final float f3 = currentY;
        final float f4 = centerY;
        final CellState cellState2 = cellState;
        valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                LockPatternView.this.mLastCellCenterX = ((1.0f - t) * f) + (f2 * t);
                LockPatternView.this.mLastCellCenterY = ((1.0f - t) * f3) + (f4 * t);
                cellState2.hwCenterX = CanvasProperty.createFloat(LockPatternView.this.mLastCellCenterX);
                cellState2.hwCenterY = CanvasProperty.createFloat(LockPatternView.this.mLastCellCenterY);
                LockPatternView.this.invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                cellState.moveAnimator = null;
            }
        });
        valueAnimator.setInterpolator(this.mInterpolator);
        valueAnimator.setDuration(ANIM_DURATION);
        if (cellState.moveAnimator == null) {
            valueAnimator.start();
            this.mCellStates[row][column].moveAnimator = valueAnimator;
        }
    }

    private void enlargeCellAnimation(float start, float end, Interpolator interpolator, final CellState cellState) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(new float[]{start, end});
        valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                cellState.radius = ((Float) animation.getAnimatedValue()).floatValue();
                LockPatternView.this.invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                cellState.lineAnimator = null;
            }
        });
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.setDuration(ANIM_DURATION);
        valueAnimator.start();
        cellState.lineAnimator = valueAnimator;
    }

    private void drawHwPath(ArrayList<Cell> pattern, boolean[][] drawLookup, Path currentPath, Canvas canvas) {
        float centerX;
        float centerY;
        float centerX2;
        float centerY2;
        Path path = currentPath;
        int count = pattern.size();
        int i = 0;
        boolean anyCircles = false;
        float lastX = 0.0f;
        float lastY = 0.0f;
        while (true) {
            int i2 = i;
            if (i2 >= count) {
                ArrayList<Cell> arrayList = pattern;
                break;
            }
            Cell cell = (Cell) pattern.get(i2);
            if (!drawLookup[cell.row][cell.column]) {
                break;
            }
            anyCircles = true;
            centerX = getCenterXForColumn(cell.column);
            centerY = getCenterYForRow(cell.row);
            if (i2 != 0) {
                if (isLastCell(cell)) {
                    centerX = this.mLastCellCenterX;
                    centerY = this.mLastCellCenterY;
                }
                centerX2 = centerX;
                centerY2 = centerY;
                connectTwoCells(path, (float) this.mDotCircleRadius, lastX, lastY, centerX2, centerY2);
                centerX = centerX2;
                centerY = centerY2;
            }
            lastX = centerX;
            lastY = centerY;
            i = i2 + 1;
        }
        if ((this.mPatternInProgress || this.mPatternDisplayMode == DisplayMode.Animate) && anyCircles) {
            Cell cell2 = touchACell(this.mInProgressX, this.mInProgressY);
            if (cell2 == null || !isLastCell(cell2)) {
                centerX = this.mInProgressX;
                centerY = this.mInProgressY;
                float margin = (float) this.mLineRadius;
                if (this.mInProgressX < margin) {
                    centerX = margin;
                }
                if (this.mInProgressX > this.mWidth - margin) {
                    centerX2 = this.mWidth - margin;
                } else {
                    centerX2 = centerX;
                }
                if (this.mInProgressY < margin) {
                    centerY = margin;
                }
                if (this.mInProgressY > this.mHeight - margin) {
                    centerY2 = this.mHeight - margin;
                } else {
                    centerY2 = centerY;
                }
                connectCellToPoint(path, (float) this.mDotCircleRadius, this.mLastCellCenterX, this.mLastCellCenterY, centerX2, centerY2);
            }
        }
        if (count == 1) {
            path.addCircle(this.mLastCellCenterX, this.mLastCellCenterY, (float) this.mDotCircleRadius, Direction.CCW);
        }
        canvas.drawPath(path, this.mPathPaint);
    }

    private void connectTwoCells(Path currentPath, float radius, float startX, float startY, float endX, float endY) {
        Path path = currentPath;
        float f = radius;
        float f2 = startX;
        float f3 = startY;
        float f4 = endX;
        float f5 = endY;
        double midAngle = Math.atan(0.2d);
        double midRadius = Math.sqrt(((double) (this.mLineRadius * this.mLineRadius)) + ((((double) f) * 1.6d) * (((double) f) * 1.6d)));
        double baseAngle = Math.abs(f2 - f4) < 1.0f ? f5 < f3 ? 1.5707963267948966d : -1.5707963267948966d : -Math.atan2((double) (f5 - f3), (double) (f4 - f2));
        double baseAngle2 = baseAngle;
        path.moveTo((float) (((double) f2) + (((double) f) * Math.cos(baseAngle2 - 0.7853981633974483d))), (float) (((double) f3) - (((double) f) * Math.sin(baseAngle2 - 0.7853981633974483d))));
        double baseAngle3 = baseAngle2;
        path.arcTo(f2 - f, f3 - f, f2 + f, f3 + f, (float) Math.toDegrees((-baseAngle2) + 0.7853981633974483d), 270.0f, false);
        path.quadTo((float) (((double) f2) + (((double) (1.05f * f)) * Math.cos(baseAngle3 + (0.7853981633974483d / 2.0d)))), (float) (((double) f3) - (((double) (1.05f * f)) * Math.sin(baseAngle3 + (0.7853981633974483d / 2.0d)))), (float) (((double) f2) + (Math.cos(baseAngle3 + midAngle) * midRadius)), (float) (((double) f3) - (Math.sin(baseAngle3 + midAngle) * midRadius)));
        float f6 = endX;
        float f7 = endY;
        path.lineTo((float) (((double) f6) + (Math.cos((baseAngle3 - midAngle) + 3.141592653589793d) * midRadius)), (float) (((double) f7) - (Math.sin((baseAngle3 - midAngle) + 3.141592653589793d) * midRadius)));
        path.lineTo((float) (((double) f6) + (Math.cos((3.141592653589793d + baseAngle3) + midAngle) * midRadius)), (float) (((double) f7) - (Math.sin((3.141592653589793d + baseAngle3) + midAngle) * midRadius)));
        path.lineTo((float) (((double) f2) + (Math.cos(baseAngle3 - midAngle) * midRadius)), (float) (((double) f3) - (Math.sin(baseAngle3 - midAngle) * midRadius)));
        path.quadTo((float) (((double) f2) + (((double) (1.05f * f)) * Math.cos(baseAngle3 - (0.7853981633974483d / 2.0d)))), (float) (((double) f3) - (((double) (1.05f * f)) * Math.sin(baseAngle3 - (0.7853981633974483d / 2.0d)))), (float) (((double) f2) + (((double) f) * Math.cos(baseAngle3 - 0.7853981633974483d))), (float) (((double) f3) - (((double) f) * Math.sin(baseAngle3 - 0.7853981633974483d))));
        path.moveTo((float) (((double) f6) + (Math.cos((3.141592653589793d + baseAngle3) + midAngle) * midRadius)), (float) (((double) f7) - (Math.sin((3.141592653589793d + baseAngle3) + midAngle) * midRadius)));
        f4 = f7;
        f2 = f6;
        path.arcTo(f6 - f, f7 - f, f6 + f, f7 + f, (float) Math.toDegrees((3.141592653589793d - baseAngle3) + 0.7853981633974483d), 270.0f, false);
        path.quadTo((float) (((double) f2) + (((double) (1.05f * f)) * Math.cos((3.141592653589793d + baseAngle3) + (0.7853981633974483d / 2.0d)))), (float) (((double) f4) - (((double) (1.05f * f)) * Math.sin((3.141592653589793d + baseAngle3) + (0.7853981633974483d / 2.0d)))), (float) (((double) f2) + (Math.cos((3.141592653589793d + baseAngle3) + midAngle) * midRadius)), (float) (((double) f4) - (Math.sin((3.141592653589793d + baseAngle3) + midAngle) * midRadius)));
        path.lineTo((float) (((double) f2) + (Math.cos((baseAngle3 - midAngle) + 3.141592653589793d) * midRadius)), (float) (((double) f4) - (Math.sin((baseAngle3 - midAngle) + 3.141592653589793d) * midRadius)));
        path.quadTo((float) (((double) f2) + (((double) (1.05f * f)) * Math.cos((3.141592653589793d + baseAngle3) - (0.7853981633974483d / 2.0d)))), (float) (((double) f4) - (((double) (1.05f * f)) * Math.sin((3.141592653589793d + baseAngle3) - (0.7853981633974483d / 2.0d)))), (float) (((double) f2) + (((double) f) * Math.cos((3.141592653589793d + baseAngle3) - 0.7853981633974483d))), (float) (((double) f4) - (((double) f) * Math.sin((3.141592653589793d + baseAngle3) - 0.7853981633974483d))));
        invalidate();
    }

    private void connectCellToPoint(Path currentPath, float radius, float startX, float startY, float endX, float endY) {
        Path path = currentPath;
        float f = radius;
        float f2 = startX;
        float f3 = startY;
        float f4 = endX;
        float f5 = endY;
        double midAngle = Math.atan(0.2d);
        double midRadius = Math.sqrt(((double) (this.mLineRadius * this.mLineRadius)) + ((((double) f) * 1.6d) * (((double) f) * 1.6d)));
        float distance = (float) Math.hypot((double) (f4 - f2), (double) (f5 - f3));
        double baseAngle = Math.abs(f2 - f4) < 1.0f ? f5 < f3 ? 1.5707963267948966d : -1.5707963267948966d : -Math.atan2((double) (f5 - f3), (double) (f4 - f2));
        double baseAngle2 = baseAngle;
        float endCenterX = (float) (((double) f4) + (((((double) (f / 2.0f)) * Math.cos(baseAngle2 + 1.5707963267948966d)) + (((double) (f / 2.0f)) * Math.cos(baseAngle2 - 1.5707963267948966d))) / 2.0d));
        double shiftAngle = 0.7853981633974483d;
        f3 = (float) (((double) f5) - (((((double) (f / 2.0f)) * Math.sin(baseAngle2 + 1.5707963267948966d)) + (((double) (f / 2.0f)) * Math.sin(baseAngle2 - 1.5707963267948966d))) / 2.0d));
        float endCenterX2;
        float f6;
        float endCenterY;
        if (distance > 2.0f * f) {
            f4 = startY;
            float endCenterX3 = endCenterX;
            path.moveTo((float) (((double) f2) + (((double) f) * Math.cos(baseAngle2 - shiftAngle))), (float) (((double) f4) - (((double) f) * Math.sin(baseAngle2 - shiftAngle))));
            endCenterX2 = endCenterX3;
            double baseAngle3 = baseAngle2;
            f5 = distance;
            path.arcTo(f2 - f, f4 - f, f2 + f, f4 + f, (float) Math.toDegrees((-baseAngle2) + shiftAngle), -90.0f, 0.0f);
            double baseAngle4 = baseAngle3;
            path.quadTo((float) (((double) f2) + (((double) (1.05f * f)) * Math.cos(baseAngle4 + (shiftAngle / 2.0d)))), (float) (((double) f4) - (((double) (1.05f * f)) * Math.sin(baseAngle4 + (shiftAngle / 2.0d)))), (float) (((double) f2) + (Math.cos(baseAngle4 + midAngle) * midRadius)), (float) (((double) f4) - (Math.sin(baseAngle4 + midAngle) * midRadius)));
            f5 = endX;
            f6 = endY;
            endCenterY = f3;
            path.lineTo((float) (((double) f5) + (((double) this.mLineRadius) * Math.cos(baseAngle4 + 1.5707963267948966d))), (float) (((double) f6) - (((double) this.mLineRadius) * Math.sin(baseAngle4 + 3.37028055E12f))));
            path.lineTo((float) (((double) f5) + (((double) this.mLineRadius) * Math.cos(baseAngle4 - 1.5707963267948966d))), (float) (((double) f6) - (((double) this.mLineRadius) * Math.sin(baseAngle4 - 1.5707963267948966d))));
            f3 = startY;
            path.lineTo((float) (((double) f2) + (Math.cos(baseAngle4 - midAngle) * midRadius)), (float) (((double) f3) - (Math.sin(baseAngle4 - midAngle) * midRadius)));
            path.quadTo((float) (((double) f2) + (((double) (1.05f * f)) * Math.cos(baseAngle4 - (shiftAngle / 2.0d)))), (float) (((double) f3) - (((double) (1.05f * f)) * Math.sin(baseAngle4 - (shiftAngle / 2.0d)))), (float) (((double) f2) + (((double) f) * Math.cos(baseAngle4 - shiftAngle))), (float) (((double) f3) - (((double) f) * Math.sin(baseAngle4 - shiftAngle))));
            f3 = baseAngle4;
            path.addArc(endCenterX2 - ((float) this.mLineRadius), endCenterY - ((float) this.mLineRadius), endCenterX2 + ((float) this.mLineRadius), endCenterY + ((float) this.mLineRadius), (float) Math.toDegrees((-baseAngle4) - 1.5707963267948966d), 1127481344);
        } else {
            endCenterX2 = endCenterX;
            endCenterY = f3;
            f6 = endY;
            double baseAngle5 = baseAngle2;
            if (distance > f) {
                f2 = startX;
                f5 = startY;
                path.moveTo((float) (((double) f2) + (((double) f) * Math.cos(baseAngle5 - shiftAngle))), (float) (((double) f5) - (((double) f) * Math.sin(baseAngle5 - shiftAngle))));
                path.arcTo(f2 - f, f5 - f, f2 + f, f5 + f, (float) Math.toDegrees((-baseAngle5) + shiftAngle), -90.0f, false);
                distance = endX;
                path.quadTo((float) (((double) f2) + (((double) f) * Math.cos((shiftAngle / 2.0d) + baseAngle5))), (float) (((double) f5) - (((double) f) * Math.sin(baseAngle5 + (shiftAngle / 2.0d)))), (float) (((double) distance) + (((double) this.mLineRadius) * Math.cos(baseAngle5 + 1.5707963267948966d))), (float) (((double) f6) - (((double) this.mLineRadius) * Math.sin(baseAngle5 + 1.5707963267948966d))));
                path.lineTo((float) (((double) distance) + (((double) this.mLineRadius) * Math.cos(baseAngle5 - 1.5707963267948966d))), (float) (((double) f6) - (((double) this.mLineRadius) * Math.sin(baseAngle5 - 1.5707963267948966d))));
                f = startX;
                f2 = radius;
                path.quadTo((float) (((double) f) + (((double) f2) * Math.cos(baseAngle5 - (shiftAngle / 2.0d)))), (float) (((double) f5) - (((double) f2) * Math.sin(baseAngle5 - (shiftAngle / 2.0d)))), (float) (((double) f) + (((double) f2) * Math.cos(baseAngle5 - shiftAngle))), (float) (((double) f5) - (((double) f2) * Math.sin(baseAngle5 - shiftAngle))));
                path.addArc(endCenterX2 - ((float) this.mLineRadius), endCenterY - ((float) this.mLineRadius), endCenterX2 + ((float) this.mLineRadius), endCenterY + ((float) this.mLineRadius), (float) Math.toDegrees((-baseAngle5) - 1.5707963267948966d), 180.0f);
                invalidate();
            }
        }
        f5 = startY;
        invalidate();
    }

    private void resetCellRadius() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.mCellStates[i][j].radius = (float) this.mDotRadius;
            }
        }
    }

    private void lastCellAnimation(Cell lastCell, float x, float y) {
        Cell touchedCell = touchACell(x, y);
        float currCenterX = this.mLastCellCenterX;
        float curreCenterY = this.mLastCellCenterY;
        CellState cellState = this.mCellStates[lastCell.getRow()][lastCell.getColumn()];
        boolean z = Math.abs(this.mLastCellCenterX - getCenterXForColumn(lastCell.getColumn())) < 0.01f && Math.abs(this.mLastCellCenterY - getCenterYForRow(lastCell.getRow())) < 0.01f;
        boolean isAtCenter = z;
        if (touchedCell != null && isLastCell(touchedCell)) {
            moveToTouchArea(touchedCell, currCenterX, curreCenterY, x, y, false);
        } else if (!isAtCenter && cellState.moveAnimator == null) {
            cellBackToCenter(lastCell, currCenterX, curreCenterY);
        }
    }

    private void backToCenterAfterActionUp() {
        Cell lastCell = (Cell) this.mPattern.get(this.mPattern.size() - 1);
        CellState cellState = this.mCellStates[lastCell.getRow()][lastCell.getColumn()];
        this.mLastCellCenterX = getCenterXForColumn(lastCell.getColumn());
        this.mLastCellCenterY = getCenterYForRow(lastCell.getRow());
        cellState.hwCenterX = CanvasProperty.createFloat(this.mLastCellCenterX);
        cellState.hwCenterY = CanvasProperty.createFloat(this.mLastCellCenterY);
        invalidate();
    }

    public void setRegularColor(boolean isInKeyguard, int color) {
        this.mIsInKeyguard = isInKeyguard;
        this.mPickedColor = color;
    }
}
