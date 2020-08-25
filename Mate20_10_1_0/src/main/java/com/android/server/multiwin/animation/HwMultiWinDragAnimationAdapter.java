package com.android.server.multiwin.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.Slog;
import android.view.InputChannel;
import android.view.View;
import com.android.server.multiwin.HwMultiWinConstants;
import com.android.server.multiwin.animation.interpolator.SharpCurveInterpolator;
import com.huawei.android.app.HwActivityManager;
import java.lang.ref.WeakReference;

public class HwMultiWinDragAnimationAdapter {
    private static final boolean DBG = false;
    public static final int START_DRAGGING_FROM_FREE_FORM_AREA = 1;
    private static final String TAG = "HwMultiWinDragAnimationAdapter";
    private Drawable mBackground;
    private View mCaptionView;
    private Bitmap mDragBarBmp;
    private Point mDragBarDrawOffsets;
    private Point mDraggingClipSize;
    private Point mFloatingClipSize;
    private View mHostView;
    private HwDragShadowBuilder mHwDragShadowBuilder;
    private Drawable mIconDrawable;
    private Bitmap mScreenShot;

    public interface ScaleUpAnimationListener {
        void onAnimationDone();
    }

    public HwMultiWinDragAnimationAdapter(View hostView) {
        this.mHostView = hostView;
    }

    public HwMultiWinDragAnimationAdapter setCaptionView(View captionView) {
        this.mCaptionView = captionView;
        return this;
    }

    public HwMultiWinDragAnimationAdapter setIconDrawable(Drawable iconDrawable) {
        this.mIconDrawable = iconDrawable;
        return this;
    }

    public HwMultiWinDragAnimationAdapter setScreenShot(Bitmap screenShot) {
        this.mScreenShot = screenShot;
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.setScreenShot(this.mScreenShot);
        }
        return this;
    }

    public void setDragBarBmp(Bitmap dragBarBmp, Point drawOffsets) {
        this.mDragBarBmp = dragBarBmp;
        this.mDragBarDrawOffsets = drawOffsets;
    }

    public HwMultiWinDragAnimationAdapter setDragBackground(Drawable background) {
        this.mBackground = background;
        return this;
    }

    public HwMultiWinDragAnimationAdapter setDraggingClipSize(int clipWidth, int clipHeight) {
        this.mDraggingClipSize = new Point(clipWidth, clipHeight);
        return this;
    }

    public HwMultiWinDragAnimationAdapter setFloatingClipSize(int clipWidth, int clipHeight) {
        this.mFloatingClipSize = new Point(clipWidth, clipHeight);
        return this;
    }

    public boolean startDragAndDrop(Bundle info, boolean isSplitCaptionViewDragged, InputChannel inputChannel) {
        if (this.mHostView == null) {
            Log.w(TAG, "startDragAndDrop failed, cause mHostView is null!");
            return false;
        } else if (info == null) {
            Slog.w(TAG, "startDragAndDrop failed, cause info is null!");
            return false;
        } else if (!info.containsKey(HwMultiWinConstants.DRAG_TOUCH_OFFSETS_KEY)) {
            Slog.w(TAG, "startDragAndDrop failed, cause info has no DRAG_TOUCH_OFFSETS_KEY!");
            return false;
        } else {
            Point touchOffsets = (Point) info.getParcelable(HwMultiWinConstants.DRAG_TOUCH_OFFSETS_KEY);
            if (!info.containsKey(HwMultiWinConstants.DRAG_SURFACE_SIZE_KEY)) {
                Slog.w(TAG, "startDragAndDrop failed, cause info has no DRAG_SURFACE_SIZE_KEY!");
                return false;
            }
            Point dragSurfaceSize = (Point) info.getParcelable(HwMultiWinConstants.DRAG_SURFACE_SIZE_KEY);
            if (!info.containsKey(HwMultiWinConstants.INITIAL_CLIP_SIZE_KEY)) {
                Slog.w(TAG, "startDragAndDrop failed, cause info has no INITIAL_CLIP_SIZE_KEY!");
                return false;
            }
            int surfaceWidth = dragSurfaceSize.x;
            int surfaceHeight = dragSurfaceSize.y;
            Point surfaceSize = new Point(surfaceWidth, surfaceHeight);
            Point touchOffset = new Point(touchOffsets.x, touchOffsets.y);
            this.mHwDragShadowBuilder = new HwDragShadowBuilder(this.mHostView, surfaceSize, (Point) info.getParcelable(HwMultiWinConstants.INITIAL_CLIP_SIZE_KEY), touchOffset, isSplitCaptionViewDragged).setCaptionView(this.mCaptionView).setIcon(this.mIconDrawable).setScreenShot(this.mScreenShot).setDragBar(this.mDragBarBmp, this.mDragBarDrawOffsets);
            this.mHwDragShadowBuilder.setBackground(this.mBackground, new Point(surfaceWidth, surfaceHeight));
            this.mHwDragShadowBuilder.setDraggingClipSize(this.mDraggingClipSize);
            this.mHwDragShadowBuilder.setFloatingClipSize(this.mFloatingClipSize);
            Intent intent = new Intent();
            intent.putExtra(HwMultiWinConstants.HW_FREE_FROM_PRE_DRAG_INPUT_CHANNEL, (Parcelable) inputChannel);
            ClipData clipData = ClipData.newIntent(TAG, intent);
            if (!info.containsKey(HwMultiWinConstants.DRAG_TOUCH_POINT_KEY)) {
                Slog.w(TAG, "startDragAndDrop failed, cause info has no DRAG_TOUCH_POINT_KEY!");
                return false;
            }
            return this.mHostView.startDragAndDrop(clipData, this.mHwDragShadowBuilder, (Point) info.getParcelable(HwMultiWinConstants.DRAG_TOUCH_POINT_KEY), 1073743617);
        }
    }

    public void startFreeFormDraggingAnimation() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startFreeFormDraggingAnimation();
        }
    }

    public void startSplitDraggingAnimation() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startSplitDraggingAnimation();
        }
    }

    public void startReducedWidthAnimation() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startReducedWidthAnimation();
        }
    }

    public void startReducedHeightAnimation() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startReducedHeightAnimation();
        }
    }

    public void startRecoverAnimation() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startRecoverAnimation();
        }
    }

    public void startDropSplitScreenAnimation(Rect dropTargetRect, ScaleUpAnimationListener listener) {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startDropSplitAnimation(dropTargetRect, listener);
        }
    }

    public void startSplitEnterFreeFormAnimation() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startSplitEnterFreeFormAnimation();
        }
    }

    public void startSplitExitFreeFormAnimation() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startSplitExitFreeFormAnimation();
        }
    }

    public void startSplitDropSplit(ScaleUpAnimationListener listener) {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startSplitDropSplit(listener);
        }
    }

    public void startDropDisappearAnimation(Rect dropTargetRect, ScaleUpAnimationListener listener) {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startDropDisappearAnimation(dropTargetRect, listener);
        }
    }

    public void startEnterSplitAlphaAnimation() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startEnterSplitAlphaAnimation();
        }
    }

    public void startExitSplitAlphaAnimation() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startExitSplitAlphaAnimation();
        }
    }

    public void startDragBarDismissAnimation() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.startDragBarAnimation(0.0f);
        }
    }

    public void reset() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            hwDragShadowBuilder.reset();
        }
    }

    public Bitmap getLastDrawingContent() {
        HwDragShadowBuilder hwDragShadowBuilder = this.mHwDragShadowBuilder;
        if (hwDragShadowBuilder != null) {
            return hwDragShadowBuilder.getLastDrawingContent();
        }
        return null;
    }

    static class HwDragShadowBuilder extends View.DragShadowBuilder {
        private static final long ALPHA_ANIM_DURATION = 100;
        private static final float DEFAULT_SCALE_FACTOR = 1.0f;
        private static final long DRAGGING_ANIM_DURATION = 250;
        private static final long DRAGGING_ANIM_START_DELAY = 50;
        public static final int DRAGGING_ANIM_TYPE = 1;
        private static final float DRAGGING_SPLIT_SCALE_FACTOR = 0.85f;
        private static final long DRAG_BAR_ANIM_DURATION = 50;
        private static final long DROP_DISAPPEAR_DURATION = 100;
        public static final int DROP_FREE_FORM_ANIM_TYPE = 2;
        public static final int DROP_FULL_SCREEN_ANIM_TYPE = 4;
        public static final int DROP_SPLIT_SCREEN_ANIM_TYPE = 3;
        private static final long FINAL_SCALE_UP_DURATION = 350;
        private static final long ICON_DISAPPEAR_ANIMATION = 550;
        private static final float MAX_ALPHA_ONE = 1.0f;
        private static final float MAX_ROUND_CORNER_RADIUS_DP = 16.5f;
        private static final float PENDING_TO_SPLIT_ALPHA = 0.2f;
        private static final long SPLIT_ENTER_FREE_FORM_DURATION = 350;
        private static final long SPLIT_EXIT_FREE_FORM_DURATION = 350;
        /* access modifiers changed from: private */
        public float mAlpha;
        /* access modifiers changed from: private */
        public ValueAnimator mAlphaAnimator;
        private int mAnimType = 3;
        private WeakReference<Drawable> mBackground;
        private int mBackgroundHeight;
        private int mBackgroundWidth;
        private int mCaptionViewHeight;
        private int mCaptionViewWidth;
        /* access modifiers changed from: private */
        public ValueAnimator mClipAnimator;
        private int mClipHeight;
        private Path mClipPath = new Path();
        /* access modifiers changed from: private */
        public RectF mClipRect;
        private int mClipWidth;
        private WeakReference<Bitmap> mDragBar;
        /* access modifiers changed from: private */
        public float mDragBarAlpha = 1.0f;
        /* access modifiers changed from: private */
        public ValueAnimator mDragBarAnimator;
        private Point mDragBarDrawOffsets;
        private Paint mDragBarPaint = new Paint();
        private ValueAnimator mDragSurfaceDisappearAnimator;
        private int mDragSurfaceHeight;
        private int mDragSurfaceWidth;
        private int mFloatingClipHeight;
        private int mFloatingClipWidth;
        private int mFreeFormDraggingClipHeight;
        private int mFreeFormDraggingClipWidth;
        private float mFreeFormRoundCornerRadius;
        /* access modifiers changed from: private */
        public WeakReference<View> mHostView;
        private WeakReference<Drawable> mIcon;
        /* access modifiers changed from: private */
        public float mIconAlpha;
        /* access modifiers changed from: private */
        public ValueAnimator mIconDisappearAnimator;
        /* access modifiers changed from: private */
        public float mIconDrawMidX;
        /* access modifiers changed from: private */
        public float mIconDrawMidY;
        private float[] mIconDrawPos = new float[2];
        private float mInitialClipHeight;
        private float mInitialClipWidth;
        private boolean mIsIconKeepUpWidthClipRect = true;
        private boolean mIsSplitCaptionViewDragged;
        private Bitmap mLastDrawingContent;
        /* access modifiers changed from: private */
        public float mRoundCornerR;
        private float mScaleX = 1.0f;
        private float mScaleY = 1.0f;
        private WeakReference<Bitmap> mScreenShot;
        private Rect mScreenShotDstBound;
        private Rect mScreenShotSrcBound;
        private float mSplitRoundCornerRadius;
        private int mTouchPointX;
        private int mTouchPointY;

        HwDragShadowBuilder(View hostView, Point dragSurfaceSize, Point clipSize, Point touchOffset, boolean isSplitCaptionViewDragged) {
            float f;
            float f2;
            this.mHostView = new WeakReference<>(hostView);
            this.mDragSurfaceWidth = dragSurfaceSize.x;
            this.mDragSurfaceHeight = dragSurfaceSize.y;
            this.mClipWidth = clipSize.x;
            this.mClipHeight = clipSize.y;
            this.mInitialClipWidth = (float) clipSize.x;
            this.mInitialClipHeight = (float) clipSize.y;
            this.mTouchPointX = touchOffset.x + ((int) ((((float) this.mDragSurfaceWidth) - this.mInitialClipWidth) / 2.0f));
            this.mTouchPointY = touchOffset.y + ((int) ((((float) this.mDragSurfaceHeight) - this.mInitialClipHeight) / 2.0f));
            if (this.mTouchPointX < 0) {
                Slog.w(HwMultiWinDragAnimationAdapter.TAG, "HwDragShadowBuilder init: mTouchPointX is negative! mDragSurfaceWidth = " + this.mDragSurfaceWidth + ", mInitialClipWidth = " + this.mInitialClipWidth + ",  touchOffset.x = " + touchOffset.x);
                this.mTouchPointX = 0;
            }
            if (this.mTouchPointY < 0) {
                Slog.w(HwMultiWinDragAnimationAdapter.TAG, "HwDragShadowBuilder init: mTouchPointY is negative! mDragSurfaceHeight = " + this.mDragSurfaceHeight + ", mInitialClipHeight = " + this.mInitialClipHeight + ",  touchOffset.y = " + touchOffset.y);
                this.mTouchPointY = 0;
            }
            float surfaceCenterX = ((float) this.mDragSurfaceWidth) / 2.0f;
            float surfaceCenterY = ((float) this.mDragSurfaceHeight) / 2.0f;
            int i = this.mClipWidth;
            int i2 = this.mClipHeight;
            this.mClipRect = new RectF(surfaceCenterX - (((float) i) / 2.0f), surfaceCenterY - (((float) i2) / 2.0f), (((float) i) / 2.0f) + surfaceCenterX, (((float) i2) / 2.0f) + surfaceCenterY);
            Context context = hostView != null ? hostView.getContext() : null;
            float f3 = 0.0f;
            if (context != null) {
                f = (float) context.getResources().getDimensionPixelSize(34472524);
            } else {
                f = 0.0f;
            }
            this.mFreeFormRoundCornerRadius = f;
            if (context == null || HwActivityManager.IS_PHONE) {
                f2 = 0.0f;
            } else {
                f2 = (float) context.getResources().getDimensionPixelSize(34472527);
            }
            this.mSplitRoundCornerRadius = f2;
            this.mRoundCornerR = isSplitCaptionViewDragged ? this.mSplitRoundCornerRadius : this.mFreeFormRoundCornerRadius;
            this.mIsSplitCaptionViewDragged = isSplitCaptionViewDragged;
            this.mAlpha = !isSplitCaptionViewDragged ? 1.0f : f3;
        }

        private int dip2px(Context context, float dipValue) {
            return (int) ((dipValue * context.getResources().getDisplayMetrics().density) + 0.5f);
        }

        public HwDragShadowBuilder setDraggingClipSize(Point draggingClipSize) {
            this.mFreeFormDraggingClipWidth = draggingClipSize.x;
            this.mFreeFormDraggingClipHeight = draggingClipSize.y;
            return this;
        }

        public HwDragShadowBuilder setFloatingClipSize(Point floatingClipSize) {
            this.mFloatingClipWidth = floatingClipSize.x;
            this.mFloatingClipHeight = floatingClipSize.y;
            return this;
        }

        public HwDragShadowBuilder setCaptionView(View captionView) {
            this.mCaptionViewWidth = captionView.getWidth();
            this.mCaptionViewHeight = captionView.getHeight();
            return this;
        }

        public HwDragShadowBuilder setIcon(Drawable icon) {
            this.mIcon = new WeakReference<>(icon);
            this.mIconDrawMidX = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            this.mIconDrawMidY = (this.mClipRect.top + this.mClipRect.bottom) / 2.0f;
            return this;
        }

        public HwDragShadowBuilder setScreenShot(Bitmap screenShot) {
            if (screenShot != null) {
                this.mScreenShot = new WeakReference<>(screenShot);
                this.mScreenShotSrcBound = new Rect(0, 0, screenShot.getWidth(), screenShot.getHeight());
                this.mScreenShotDstBound = new Rect();
            }
            return this;
        }

        public HwDragShadowBuilder setDragBar(Bitmap dragBarBmp, Point drawOffsets) {
            this.mDragBar = new WeakReference<>(dragBarBmp);
            this.mDragBarDrawOffsets = drawOffsets;
            return this;
        }

        public HwDragShadowBuilder setBackground(Drawable background, Point backgroundSize) {
            this.mBackground = new WeakReference<>(background);
            this.mBackgroundWidth = backgroundSize.x;
            this.mBackgroundHeight = backgroundSize.y;
            return this;
        }

        public HwDragShadowBuilder animate() {
            return this;
        }

        public HwDragShadowBuilder animateAlpha(float alpha) {
            this.mAlpha = alpha;
            return this;
        }

        public HwDragShadowBuilder animateIconAlpha(float iconAlpha) {
            this.mIconAlpha = iconAlpha;
            return this;
        }

        public HwDragShadowBuilder animateDragBarAlpha(float dragBarAlpha) {
            this.mDragBarAlpha = dragBarAlpha;
            return this;
        }

        public Bitmap getLastDrawingContent() {
            return this.mLastDrawingContent;
        }

        public void startEnterSplitAlphaAnimation() {
            long duration = 100;
            ValueAnimator valueAnimator = this.mClipAnimator;
            if (valueAnimator != null && valueAnimator.isRunning()) {
                duration = 350;
            }
            this.mAnimType = 5;
            startAlphaAnimation(0.2f, duration);
        }

        public void startExitSplitAlphaAnimation() {
            this.mAnimType = 3;
            startAlphaAnimation(1.0f, 100);
        }

        public void startDropSplitAlphaAnimation(long duration) {
            Log.d(HwMultiWinDragAnimationAdapter.TAG, "startDropSplitAlphaAnimation");
            startAlphaAnimation(1.0f, duration);
        }

        @SuppressLint({"NewApi"})
        public void startDragBarAnimation(float toAlpha) {
            ValueAnimator valueAnimator = this.mDragBarAnimator;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            this.mDragBarAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("dragBarAlpha", this.mDragBarAlpha, toAlpha));
            this.mDragBarAnimator.setDuration(50L);
            this.mDragBarAnimator.setInterpolator(new SharpCurveInterpolator());
            this.mDragBarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.server.multiwin.animation.HwMultiWinDragAnimationAdapter.HwDragShadowBuilder.AnonymousClass1 */

                public void onAnimationUpdate(ValueAnimator animation) {
                    Object dragBarAlphaObj = animation.getAnimatedValue("dragBarAlpha");
                    if (dragBarAlphaObj instanceof Float) {
                        float unused = HwDragShadowBuilder.this.mDragBarAlpha = ((Float) dragBarAlphaObj).floatValue();
                    }
                    View view = (View) HwDragShadowBuilder.this.mHostView.get();
                    if (view == null) {
                        return;
                    }
                    if (HwDragShadowBuilder.this.mClipAnimator != null && HwDragShadowBuilder.this.mClipAnimator.isRunning()) {
                        return;
                    }
                    if (HwDragShadowBuilder.this.mAlphaAnimator == null || !HwDragShadowBuilder.this.mAlphaAnimator.isRunning()) {
                        HwDragShadowBuilder hwDragShadowBuilder = HwDragShadowBuilder.this;
                        view.updateDragShadow(hwDragShadowBuilder.animateDragBarAlpha(hwDragShadowBuilder.mDragBarAlpha));
                    }
                }
            });
            this.mDragBarAnimator.start();
        }

        @SuppressLint({"NewApi"})
        public void startAlphaAnimation(float toAlpha, long duration) {
            ValueAnimator valueAnimator = this.mAlphaAnimator;
            if (valueAnimator != null && valueAnimator.isRunning()) {
                this.mAlphaAnimator.cancel();
            }
            float f = this.mAlpha;
            if (f != toAlpha) {
                this.mAlphaAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("alpha", f, toAlpha));
                this.mAlphaAnimator.setDuration(duration);
                this.mAlphaAnimator.setInterpolator(new SharpCurveInterpolator());
                this.mAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    /* class com.android.server.multiwin.animation.HwMultiWinDragAnimationAdapter.HwDragShadowBuilder.AnonymousClass2 */

                    public void onAnimationUpdate(ValueAnimator animation) {
                        Object alphaValue = animation.getAnimatedValue("alpha");
                        if (alphaValue instanceof Float) {
                            float unused = HwDragShadowBuilder.this.mAlpha = ((Float) alphaValue).floatValue();
                        }
                        View view = (View) HwDragShadowBuilder.this.mHostView.get();
                        if (view != null) {
                            HwDragShadowBuilder hwDragShadowBuilder = HwDragShadowBuilder.this;
                            HwDragShadowBuilder builder = hwDragShadowBuilder.animateAlpha(hwDragShadowBuilder.mAlpha);
                            if (HwDragShadowBuilder.this.mClipAnimator == null || !HwDragShadowBuilder.this.mClipAnimator.isRunning()) {
                                view.updateDragShadow(builder);
                            }
                        }
                    }
                });
                this.mAlphaAnimator.start();
            }
        }

        public void startDropSplitAnimation(Rect dropTargetBound, ScaleUpAnimationListener listener) {
            AnimParams animParams = new AnimParams();
            animParams.setToRadius(this.mFreeFormRoundCornerRadius);
            animParams.setListener(listener);
            animParams.setClipAnimDuration(350);
            animParams.setFromIconAlpha(1.0f);
            animParams.setToIconAlpha(1.0f);
            animParams.setToDragBarAlpha(0.0f);
            animParams.setDragBarAnimDuration(100.0f);
            float dragSurfaceCenterX = ((float) this.mDragSurfaceWidth) / 2.0f;
            float dragSurfaceCenterY = ((float) this.mDragSurfaceHeight) / 2.0f;
            animParams.setToClipRect(new RectF(dragSurfaceCenterX - (((float) dropTargetBound.width()) / 2.0f), dragSurfaceCenterY - (((float) dropTargetBound.height()) / 2.0f), (((float) dropTargetBound.width()) / 2.0f) + dragSurfaceCenterX, (((float) dropTargetBound.height()) / 2.0f) + dragSurfaceCenterY));
            startAnimation(animParams);
            startDropSplitAlphaAnimation(350);
            startDragBarAnimation(0.0f);
        }

        public void startSplitEnterFreeFormAnimation() {
            float toWidth = (float) this.mFreeFormDraggingClipWidth;
            AnimParams animParams = new AnimParams();
            animParams.setToRadius(this.mFreeFormRoundCornerRadius);
            animParams.setListener(null);
            animParams.setClipAnimDuration(350);
            animParams.setFromIconAlpha(0.0f);
            animParams.setToIconAlpha(1.0f);
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            float toTop = this.mClipRect.top;
            animParams.setToClipRect(new RectF(centerxOfClipRect - (toWidth / 2.0f), toTop, (toWidth / 2.0f) + centerxOfClipRect, toTop + ((float) this.mFreeFormDraggingClipHeight)));
            this.mAnimType = 7;
            startAnimation(animParams);
            startAlphaAnimation(1.0f, 350);
            startDragBarAnimation(1.0f);
        }

        public void startSplitDropSplit(ScaleUpAnimationListener listener) {
            float toWidth = this.mInitialClipWidth;
            float toHeight = this.mInitialClipHeight;
            AnimParams animParams = new AnimParams();
            animParams.setToRadius(this.mSplitRoundCornerRadius);
            animParams.setListener(listener);
            animParams.setClipAnimDuration(350);
            animParams.setFromIconAlpha(this.mIconAlpha);
            animParams.setToIconAlpha(0.0f);
            animParams.setToDragBarAlpha(1.0f);
            animParams.setDragBarAnimDuration(100.0f);
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            float toTop = this.mClipRect.top;
            animParams.setToClipRect(new RectF(centerxOfClipRect - (toWidth / 2.0f), toTop, (toWidth / 2.0f) + centerxOfClipRect, toTop + toHeight));
            this.mAnimType = 9;
            startAnimation(animParams);
        }

        public void startSplitExitFreeFormAnimation() {
            float toWidth = this.mInitialClipWidth * 0.85f;
            AnimParams animParams = new AnimParams();
            animParams.setToRadius(this.mSplitRoundCornerRadius);
            animParams.setListener(null);
            animParams.setClipAnimDuration(350);
            animParams.setFromIconAlpha(this.mIconAlpha);
            animParams.setToIconAlpha(0.0f);
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            float toTop = this.mClipRect.top;
            animParams.setToClipRect(new RectF(centerxOfClipRect - (toWidth / 2.0f), toTop, (toWidth / 2.0f) + centerxOfClipRect, toTop + (this.mInitialClipHeight * 0.85f)));
            this.mAnimType = 8;
            if (this.mScreenShot == null) {
                this.mAlpha = 1.0f;
                animParams.setToIconAlpha(1.0f);
            } else {
                startAlphaAnimation(0.0f, 350);
            }
            startAnimation(animParams);
            startDragBarAnimation(0.0f);
        }

        public void startDropDisappearAnimation(Rect dropTargetBound, ScaleUpAnimationListener listener) {
            startAlphaAnimation(0.0f, 100);
            startDragBarAnimation(0.0f);
            startIconDisappearAnimation(dropTargetBound, listener);
        }

        @SuppressLint({"NewApi"})
        private void startIconDisappearAnimation(Rect dropTargetBound, final ScaleUpAnimationListener listener) {
            ValueAnimator valueAnimator = this.mIconDisappearAnimator;
            if (valueAnimator != null && valueAnimator.isRunning()) {
                this.mIconDisappearAnimator.cancel();
            }
            this.mIconDisappearAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("iconDrawMidX", this.mIconDrawMidX, ((float) this.mDragSurfaceWidth) / 2.0f), PropertyValuesHolder.ofFloat("iconDrawMidY", this.mIconDrawMidY, ((float) this.mDragSurfaceHeight) / 2.0f));
            this.mIconDisappearAnimator.setDuration(ICON_DISAPPEAR_ANIMATION);
            this.mIconDisappearAnimator.setInterpolator(new SharpCurveInterpolator());
            this.mIconDisappearAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.server.multiwin.animation.HwMultiWinDragAnimationAdapter.HwDragShadowBuilder.AnonymousClass3 */

                public void onAnimationUpdate(ValueAnimator animation) {
                    Object iconDrawMidXObj = animation.getAnimatedValue("iconDrawMidX");
                    if (iconDrawMidXObj instanceof Float) {
                        float unused = HwDragShadowBuilder.this.mIconDrawMidX = ((Float) iconDrawMidXObj).floatValue();
                    }
                    Object iconDrawMidYObj = animation.getAnimatedValue("iconDrawMidY");
                    if (iconDrawMidYObj instanceof Float) {
                        float unused2 = HwDragShadowBuilder.this.mIconDrawMidY = ((Float) iconDrawMidYObj).floatValue();
                    }
                    View view = (View) HwDragShadowBuilder.this.mHostView.get();
                    if (view == null) {
                        return;
                    }
                    if (HwDragShadowBuilder.this.mClipAnimator != null && HwDragShadowBuilder.this.mClipAnimator.isRunning()) {
                        return;
                    }
                    if (HwDragShadowBuilder.this.mAlphaAnimator != null && HwDragShadowBuilder.this.mAlphaAnimator.isRunning()) {
                        return;
                    }
                    if (HwDragShadowBuilder.this.mDragBarAnimator == null || !HwDragShadowBuilder.this.mDragBarAnimator.isRunning()) {
                        view.updateDragShadow(HwDragShadowBuilder.this.animate());
                    }
                }
            });
            this.mIconDisappearAnimator.addListener(new AnimatorListenerAdapter() {
                /* class com.android.server.multiwin.animation.HwMultiWinDragAnimationAdapter.HwDragShadowBuilder.AnonymousClass4 */

                public void onAnimationEnd(Animator animation) {
                    ScaleUpAnimationListener scaleUpAnimationListener = listener;
                    if (scaleUpAnimationListener != null) {
                        scaleUpAnimationListener.onAnimationDone();
                    }
                }
            });
            this.mIsIconKeepUpWidthClipRect = false;
            this.mIconDisappearAnimator.start();
        }

        @SuppressLint({"NewApi"})
        private void startDragSurfaceDisappearAnimation() {
            ValueAnimator valueAnimator = this.mDragSurfaceDisappearAnimator;
            if (valueAnimator != null && valueAnimator.isRunning()) {
                this.mDragSurfaceDisappearAnimator.cancel();
            }
            this.mDragSurfaceDisappearAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("alpha", this.mAlpha, 0.0f), PropertyValuesHolder.ofFloat("dragBarAlpha", this.mDragBarAlpha, 0.0f));
            this.mDragSurfaceDisappearAnimator.setDuration(100L);
            this.mDragSurfaceDisappearAnimator.setInterpolator(new SharpCurveInterpolator());
            this.mDragSurfaceDisappearAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.server.multiwin.animation.HwMultiWinDragAnimationAdapter.HwDragShadowBuilder.AnonymousClass5 */

                public void onAnimationUpdate(ValueAnimator animation) {
                    Object alphaObj = animation.getAnimatedValue("alpha");
                    if (alphaObj instanceof Float) {
                        float unused = HwDragShadowBuilder.this.mAlpha = ((Float) alphaObj).floatValue();
                    }
                    Object dragBarAlphaObj = animation.getAnimatedValue("dragBarAlpha");
                    if (dragBarAlphaObj instanceof Float) {
                        float unused2 = HwDragShadowBuilder.this.mDragBarAlpha = ((Float) dragBarAlphaObj).floatValue();
                    }
                    View view = (View) HwDragShadowBuilder.this.mHostView.get();
                    if (view == null) {
                        return;
                    }
                    if (HwDragShadowBuilder.this.mClipAnimator != null && HwDragShadowBuilder.this.mClipAnimator.isRunning()) {
                        return;
                    }
                    if (HwDragShadowBuilder.this.mAlphaAnimator != null && HwDragShadowBuilder.this.mAlphaAnimator.isRunning()) {
                        return;
                    }
                    if (HwDragShadowBuilder.this.mDragBarAnimator != null && HwDragShadowBuilder.this.mDragBarAnimator.isRunning()) {
                        return;
                    }
                    if (HwDragShadowBuilder.this.mIconDisappearAnimator == null || !HwDragShadowBuilder.this.mIconDisappearAnimator.isRunning()) {
                        view.updateDragShadow(HwDragShadowBuilder.this.animate());
                    }
                }
            });
            this.mDragSurfaceDisappearAnimator.start();
        }

        public void startReducedWidthAnimation() {
            if (this.mAnimType != 1) {
                float toScaleX = this.mClipWidth > 0 ? 0.7f : 1.0f;
                float toScaleY = this.mClipHeight > 0 ? 1.3f : 1.0f;
                AnimParams animParams = new AnimParams();
                animParams.setToRadius(this.mFreeFormRoundCornerRadius);
                animParams.setListener(null);
                animParams.setClipAnimDuration(DRAGGING_ANIM_DURATION);
                animParams.setFromIconAlpha(1.0f);
                animParams.setToIconAlpha(1.0f);
                animParams.setToDragBarAlpha(1.0f);
                animParams.setDragBarAnimDuration(100.0f);
                float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
                float toTop = this.mClipRect.top;
                int i = this.mFreeFormDraggingClipWidth;
                animParams.setToClipRect(new RectF(centerxOfClipRect - ((((float) i) * toScaleX) / 2.0f), toTop, ((((float) i) * toScaleX) / 2.0f) + centerxOfClipRect, (((float) this.mFreeFormDraggingClipHeight) * toScaleY) + toTop));
                this.mAnimType = 1;
                startAnimation(animParams);
            }
        }

        public void startReducedHeightAnimation() {
            if (this.mAnimType != 2) {
                float toScaleX = this.mClipWidth > 0 ? 1.3f : 1.0f;
                float toScaleY = this.mClipHeight > 0 ? 0.7f : 1.0f;
                AnimParams animParams = new AnimParams();
                animParams.setToRadius(this.mFreeFormRoundCornerRadius);
                animParams.setListener(null);
                animParams.setClipAnimDuration(DRAGGING_ANIM_DURATION);
                animParams.setFromIconAlpha(1.0f);
                animParams.setToIconAlpha(1.0f);
                animParams.setToDragBarAlpha(1.0f);
                animParams.setDragBarAnimDuration(100.0f);
                float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
                float toTop = this.mClipRect.top;
                int i = this.mFreeFormDraggingClipWidth;
                animParams.setToClipRect(new RectF(centerxOfClipRect - ((((float) i) * toScaleX) / 2.0f), toTop, ((((float) i) * toScaleX) / 2.0f) + centerxOfClipRect, (((float) this.mFreeFormDraggingClipHeight) * toScaleY) + toTop));
                this.mAnimType = 2;
                startAnimation(animParams);
            }
        }

        public void startRecoverAnimation() {
            if (this.mAnimType != 3) {
                float toScaleX = this.mClipWidth > 0 ? ((float) this.mFreeFormDraggingClipWidth) / this.mClipRect.width() : 1.0f;
                float toScaleY = this.mClipHeight > 0 ? ((float) this.mFreeFormDraggingClipHeight) / this.mClipRect.height() : 1.0f;
                AnimParams animParams = new AnimParams();
                animParams.setToRadius(this.mFreeFormRoundCornerRadius);
                animParams.setListener(null);
                animParams.setClipAnimDuration(DRAGGING_ANIM_DURATION);
                animParams.setFromIconAlpha(1.0f);
                animParams.setToIconAlpha(1.0f);
                animParams.setToDragBarAlpha(1.0f);
                animParams.setDragBarAnimDuration(100.0f);
                float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
                float toTop = this.mClipRect.top;
                animParams.setToClipRect(new RectF(centerxOfClipRect - ((this.mClipRect.width() * toScaleX) / 2.0f), toTop, ((this.mClipRect.width() * toScaleX) / 2.0f) + centerxOfClipRect, (this.mClipRect.height() * toScaleY) + toTop));
                this.mAnimType = 3;
                startAnimation(animParams);
            }
        }

        public void startFreeFormDraggingAnimation() {
            AnimParams animParams = new AnimParams();
            float f = this.mFreeFormRoundCornerRadius;
            this.mRoundCornerR = f;
            animParams.setToRadius(f);
            animParams.setListener(null);
            animParams.setClipAnimDuration(DRAGGING_ANIM_DURATION);
            animParams.setClipAnimStartDelay(50);
            animParams.setFromIconAlpha(0.0f);
            float toScaleY = 1.0f;
            animParams.setToIconAlpha(1.0f);
            this.mAlpha = 1.0f;
            animParams.setToDragBarAlpha(1.0f);
            animParams.setDragBarAnimDuration(100.0f);
            int i = this.mClipWidth;
            float toScaleX = i > 0 ? ((float) this.mFreeFormDraggingClipWidth) / ((float) i) : 1.0f;
            int i2 = this.mClipHeight;
            if (i2 > 0) {
                toScaleY = ((float) this.mFreeFormDraggingClipHeight) / ((float) i2);
            }
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            float toTop = this.mClipRect.top;
            animParams.setToClipRect(new RectF(centerxOfClipRect - ((this.mClipRect.width() * toScaleX) / 2.0f), toTop, ((this.mClipRect.width() * toScaleX) / 2.0f) + centerxOfClipRect, (this.mClipRect.height() * toScaleY) + toTop));
            WeakReference<Bitmap> weakReference = this.mScreenShot;
            if (weakReference != null) {
                weakReference.clear();
            }
            startAnimation(animParams);
        }

        public void startSplitDraggingAnimation() {
            AnimParams animParams = new AnimParams();
            animParams.setToRadius(this.mSplitRoundCornerRadius);
            animParams.setListener(null);
            animParams.setClipAnimDuration(DRAGGING_ANIM_DURATION);
            animParams.setFromIconAlpha(0.0f);
            animParams.setToIconAlpha(0.0f);
            animParams.setToDragBarAlpha(1.0f);
            animParams.setDragBarAnimDuration(100.0f);
            this.mAlpha = 0.0f;
            animParams.setToAlpha(0.0f);
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            float toTop = this.mClipRect.top;
            animParams.setToClipRect(new RectF(centerxOfClipRect - ((this.mClipRect.width() * 0.85f) / 2.0f), toTop, ((this.mClipRect.width() * 0.85f) / 2.0f) + centerxOfClipRect, (this.mClipRect.height() * 0.85f) + toTop));
            startAnimation(animParams);
        }

        public void reset() {
            this.mScaleX = 1.0f;
            this.mScaleY = 1.0f;
        }

        @SuppressLint({"NewApi"})
        private void startAnimation(final AnimParams animParams) {
            ValueAnimator valueAnimator = this.mClipAnimator;
            if (valueAnimator != null && valueAnimator.isRunning()) {
                this.mClipAnimator.cancel();
            }
            this.mClipAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("radius", this.mRoundCornerR, animParams.getToRadius()), PropertyValuesHolder.ofObject("cliprect", new RectFTypeEvaluator(), this.mClipRect, animParams.getToClipRect()), PropertyValuesHolder.ofFloat("iconAlpha", animParams.getFromIconAlpha(), animParams.getToIconAlpha()));
            this.mClipAnimator.setDuration(animParams.getClipAnimDuration());
            this.mClipAnimator.setStartDelay(animParams.getClipAnimStartDelay());
            this.mClipAnimator.setInterpolator(new SharpCurveInterpolator());
            this.mClipAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.server.multiwin.animation.HwMultiWinDragAnimationAdapter.HwDragShadowBuilder.AnonymousClass6 */

                public void onAnimationUpdate(ValueAnimator animation) {
                    Object radiusValue = animation.getAnimatedValue("radius");
                    if (radiusValue instanceof Float) {
                        float unused = HwDragShadowBuilder.this.mRoundCornerR = ((Float) radiusValue).floatValue();
                    }
                    Object clipRectObj = animation.getAnimatedValue("cliprect");
                    if (clipRectObj instanceof RectF) {
                        RectF unused2 = HwDragShadowBuilder.this.mClipRect = (RectF) clipRectObj;
                    }
                    Object iconAlphaValue = animation.getAnimatedValue("iconAlpha");
                    if (iconAlphaValue instanceof Float) {
                        float unused3 = HwDragShadowBuilder.this.mIconAlpha = ((Float) iconAlphaValue).floatValue();
                    }
                    View view = (View) HwDragShadowBuilder.this.mHostView.get();
                    if (view != null) {
                        view.updateDragShadow(HwDragShadowBuilder.this.animate());
                    }
                }
            });
            this.mClipAnimator.addListener(new AnimatorListenerAdapter() {
                /* class com.android.server.multiwin.animation.HwMultiWinDragAnimationAdapter.HwDragShadowBuilder.AnonymousClass7 */

                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (animParams.getListener() != null) {
                        animParams.getListener().onAnimationDone();
                    }
                }
            });
            this.mClipAnimator.start();
        }

        public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
            Drawable background = this.mBackground.get();
            if (background != null) {
                background.setBounds(0, 0, this.mBackgroundWidth, this.mBackgroundHeight);
            }
            outShadowSize.set(this.mDragSurfaceWidth, this.mDragSurfaceHeight);
            outShadowTouchPoint.set(this.mTouchPointX, this.mTouchPointY);
        }

        private void onDrawScreenShot(Canvas canvas, Bitmap screenShot) {
            if (this.mAlpha < 1.0f && screenShot != null) {
                this.mScreenShotDstBound.left = (int) this.mClipRect.left;
                this.mScreenShotDstBound.top = (int) this.mClipRect.top;
                this.mScreenShotDstBound.right = (int) this.mClipRect.right;
                this.mScreenShotDstBound.bottom = (int) this.mClipRect.bottom;
                canvas.drawBitmap(screenShot, this.mScreenShotSrcBound, this.mScreenShotDstBound, (Paint) null);
            }
        }

        private void onDrawBackground(Canvas canvas, Drawable background) {
            if (this.mIsSplitCaptionViewDragged && this.mScreenShot == null) {
                this.mAlpha = 1.0f;
            }
            float f = this.mAlpha;
            if (f > 0.0f && background != null) {
                background.setAlpha((int) (f * 255.0f));
                background.draw(canvas);
            }
        }

        private void onDrawDragBar(Canvas canvas, Bitmap dragBarBmp) {
            if (dragBarBmp == null) {
                Log.d(HwMultiWinDragAnimationAdapter.TAG, "debug onDrawDragBar: dragBarBmp is null!");
                return;
            }
            canvas.save();
            canvas.translate(this.mDragBarDrawOffsets != null ? ((this.mClipRect.left + this.mClipRect.right) / 2.0f) - (((float) dragBarBmp.getWidth()) / 2.0f) : 0.0f, this.mDragBarDrawOffsets != null ? this.mClipRect.top + ((float) this.mDragBarDrawOffsets.y) : 0.0f);
            this.mDragBarPaint.reset();
            this.mDragBarPaint.setAlpha((int) (this.mDragBarAlpha * 255.0f));
            canvas.drawBitmap(dragBarBmp, 0.0f, 0.0f, this.mDragBarPaint);
            canvas.restore();
        }

        private void onDrawIcon(Canvas canvas, Drawable icon) {
            if (icon != null) {
                if (this.mIsIconKeepUpWidthClipRect) {
                    getIconDrawPositionByClipRect(this.mIconDrawPos, icon);
                } else {
                    getIconDrawPositionBySelf(this.mIconDrawPos, icon);
                }
                canvas.save();
                float[] fArr = this.mIconDrawPos;
                canvas.translate(fArr[0], fArr[1]);
                icon.setAlpha((int) (this.mIconAlpha * 255.0f));
                icon.draw(canvas);
                canvas.restore();
            }
        }

        private void onDrawShadowCanvas(Canvas canvas) {
            WeakReference<Drawable> weakReference = this.mBackground;
            Drawable icon = null;
            Drawable background = weakReference != null ? weakReference.get() : null;
            WeakReference<Bitmap> weakReference2 = this.mScreenShot;
            Bitmap screenShot = weakReference2 != null ? weakReference2.get() : null;
            this.mClipPath.reset();
            Path path = this.mClipPath;
            RectF rectF = this.mClipRect;
            float f = this.mRoundCornerR;
            path.addRoundRect(rectF, f, f, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(this.mClipPath, Region.Op.INTERSECT);
            onDrawScreenShot(canvas, screenShot);
            onDrawBackground(canvas, background);
            canvas.restore();
            WeakReference<Bitmap> weakReference3 = this.mDragBar;
            onDrawDragBar(canvas, weakReference3 != null ? weakReference3.get() : null);
            WeakReference<Drawable> weakReference4 = this.mIcon;
            if (weakReference4 != null) {
                icon = weakReference4.get();
            }
            onDrawIcon(canvas, icon);
        }

        public void getIconDrawPositionBySelf(float[] outPos, Drawable icon) {
            if (icon != null) {
                Rect rect = icon.getBounds();
                int w = rect.right - rect.left;
                int h = rect.bottom - rect.top;
                outPos[0] = this.mIconDrawMidX - (((float) w) / 2.0f);
                outPos[1] = this.mIconDrawMidY - (((float) h) / 2.0f);
            }
        }

        public void getIconDrawPositionByClipRect(float[] outPos, Drawable icon) {
            if (icon != null) {
                Rect rect = icon.getBounds();
                int w = rect.right - rect.left;
                int h = rect.bottom - rect.top;
                outPos[0] = ((this.mClipRect.left + this.mClipRect.right) / 2.0f) - (((float) w) / 2.0f);
                outPos[1] = ((this.mClipRect.top + this.mClipRect.bottom) / 2.0f) - (((float) h) / 2.0f);
                this.mIconDrawMidX = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
                this.mIconDrawMidY = (this.mClipRect.top + this.mClipRect.bottom) / 2.0f;
            }
        }

        public void onDrawShadow(Canvas canvas) {
            onDrawShadowCanvas(canvas);
        }

        private static class RectFTypeEvaluator implements TypeEvaluator<RectF> {
            private RectFTypeEvaluator() {
            }

            public RectF evaluate(float fraction, RectF startValue, RectF endValue) {
                float fromLeft = startValue.left;
                float fromTop = startValue.top;
                float fromRight = startValue.right;
                float fromBottom = startValue.bottom;
                return new RectF(((endValue.left - fromLeft) * fraction) + fromLeft, ((endValue.top - fromTop) * fraction) + fromTop, ((endValue.right - fromRight) * fraction) + fromRight, ((endValue.bottom - fromBottom) * fraction) + fromBottom);
            }
        }

        public static class AnimParams {
            private int animType;
            private long clipAnimDuration;
            private long clipAnimStartDelay;
            private float dragBarAnimDuration;
            private float fromIconAlpha;
            private long iconAnimDuration;
            private long iconAnimStartDelay;
            private ScaleUpAnimationListener listener;
            private float toAlpha;
            private RectF toClipRect;
            private float toDragBarAlpha;
            private float toIconAlpha;
            private float toRadius;
            private float toScaleX;
            private float toScaleY;

            public float getToScaleX() {
                return this.toScaleX;
            }

            public void setToScaleX(float toScaleX2) {
                this.toScaleX = toScaleX2;
            }

            public float getToScaleY() {
                return this.toScaleY;
            }

            public void setToScaleY(float toScaleY2) {
                this.toScaleY = toScaleY2;
            }

            public float getToRadius() {
                return this.toRadius;
            }

            public void setToRadius(float toRadius2) {
                this.toRadius = toRadius2;
            }

            public float getToAlpha() {
                return this.toAlpha;
            }

            public void setToAlpha(float toAlpha2) {
                this.toAlpha = toAlpha2;
            }

            public long getClipAnimDuration() {
                return this.clipAnimDuration;
            }

            public void setClipAnimDuration(long duration) {
                this.clipAnimDuration = duration;
            }

            public ScaleUpAnimationListener getListener() {
                return this.listener;
            }

            public void setListener(ScaleUpAnimationListener listener2) {
                this.listener = listener2;
            }

            public int getAnimType() {
                return this.animType;
            }

            public void setAnimType(int animType2) {
                this.animType = animType2;
            }

            public long getClipAnimStartDelay() {
                return this.clipAnimStartDelay;
            }

            public void setClipAnimStartDelay(long startDelay) {
                this.clipAnimStartDelay = startDelay;
            }

            public float getFromIconAlpha() {
                return this.fromIconAlpha;
            }

            public void setFromIconAlpha(float fromIconAlpha2) {
                this.fromIconAlpha = fromIconAlpha2;
            }

            public float getToIconAlpha() {
                return this.toIconAlpha;
            }

            public void setToIconAlpha(float toIconAlpha2) {
                this.toIconAlpha = toIconAlpha2;
            }

            public long getIconAnimStartDelay() {
                return this.iconAnimStartDelay;
            }

            public void setIconAnimStartDelay(long iconAnimStartDelay2) {
                this.iconAnimStartDelay = iconAnimStartDelay2;
            }

            public long getIconAnimDuration() {
                return this.iconAnimDuration;
            }

            public void setIconAnimDuration(long iconAnimDuration2) {
                this.iconAnimDuration = iconAnimDuration2;
            }

            public RectF getToClipRect() {
                return this.toClipRect;
            }

            public void setToClipRect(RectF toClipRect2) {
                this.toClipRect = toClipRect2;
            }

            public float getToDragBarAlpha() {
                return this.toDragBarAlpha;
            }

            public void setToDragBarAlpha(float toDragBarAlpha2) {
                this.toDragBarAlpha = toDragBarAlpha2;
            }

            public float getDragBarAnimDuration() {
                return this.dragBarAnimDuration;
            }

            public void setDragBarAnimDuration(float dragBarAnimDuration2) {
                this.dragBarAnimDuration = dragBarAnimDuration2;
            }
        }
    }
}
