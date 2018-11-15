package com.android.server.gesture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SlideOutCircleView extends FrameLayout {
    public static final Interpolator ALPHA_OUT = new PathInterpolator(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.8f, 1.0f);
    private final Paint mBackgroundPaint;
    private final int mBaseMargin;
    private float mCircleAnimationEndValue;
    private ValueAnimator mCircleAnimator;
    private int mCircleCenterStartPos;
    private boolean mCircleHidden;
    private final int mCircleMaxSize;
    private final int mCircleMinSize;
    private final Rect mCircleRect;
    private final int mCircleSideMargin;
    private float mCircleSize;
    private AnimatorUpdateListener mCircleUpdateListener;
    private AnimatorListenerAdapter mClearAnimatorListener;
    private boolean mClipToOutline;
    private final Interpolator mDisappearInterpolator;
    private boolean mDraggedFarEnough;
    private ValueAnimator mFadeOutAnimator;
    private final Interpolator mFastOutSlowInInterpolator;
    private ImageView mLeftLogo;
    private final int mMaxElevation;
    private float mOffset;
    private boolean mOffsetAnimatingIn;
    private ValueAnimator mOffsetAnimator;
    private AnimatorUpdateListener mOffsetUpdateListener;
    private float mOutlineAlpha;
    private ImageView mRightLogo;
    private boolean mSlidingOnLeft;
    private final int mStaticOffset;
    private final Rect mStaticRect;

    private static class MyAnimatorListenerAdapter extends AnimatorListenerAdapter {
        Runnable mRunnable;

        public MyAnimatorListenerAdapter(Runnable runnable) {
            this.mRunnable = runnable;
        }

        public void onAnimationEnd(Animator animation) {
            if (this.mRunnable != null) {
                this.mRunnable.run();
            }
        }
    }

    public SlideOutCircleView(Context context) {
        this(context, null);
    }

    public SlideOutCircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideOutCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SlideOutCircleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mBackgroundPaint = new Paint();
        this.mCircleRect = new Rect();
        this.mStaticRect = new Rect();
        this.mSlidingOnLeft = true;
        this.mCircleUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                SlideOutCircleView.this.applyCircleSize(((Float) animation.getAnimatedValue()).floatValue());
                SlideOutCircleView.this.updateElevation();
            }
        };
        this.mClearAnimatorListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                SlideOutCircleView.this.mCircleAnimator = null;
            }
        };
        this.mOffsetUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                SlideOutCircleView.this.setOffset(((Float) animation.getAnimatedValue()).floatValue());
            }
        };
        setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                if (SlideOutCircleView.this.mCircleSize > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                    outline.setOval(SlideOutCircleView.this.mCircleRect);
                } else {
                    outline.setEmpty();
                }
                outline.setAlpha(SlideOutCircleView.this.mOutlineAlpha);
            }
        });
        setWillNotDraw(false);
        this.mCircleSideMargin = context.getResources().getDimensionPixelSize(34472401);
        this.mCircleMinSize = context.getResources().getDimensionPixelSize(34472400);
        this.mCircleMaxSize = context.getResources().getDimensionPixelSize(34472399);
        this.mCircleCenterStartPos = context.getResources().getDimensionPixelSize(34472398);
        this.mBaseMargin = context.getResources().getDimensionPixelSize(34472397);
        this.mStaticOffset = context.getResources().getDimensionPixelSize(34472403);
        this.mMaxElevation = context.getResources().getDimensionPixelSize(34472146);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563661);
        this.mDisappearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563663);
        this.mBackgroundPaint.setAntiAlias(true);
        this.mBackgroundPaint.setColor(getResources().getColor(33882315));
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawCircle((float) this.mCircleRect.centerX(), (float) this.mCircleRect.centerY(), this.mCircleSize / 2.0f, this.mBackgroundPaint);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLeftLogo = (ImageView) findViewById(34603223);
        this.mRightLogo = (ImageView) findViewById(34603249);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.mLeftLogo.layout(0, 0, this.mLeftLogo.getMeasuredWidth(), this.mLeftLogo.getMeasuredHeight());
        this.mRightLogo.layout(0, 0, this.mRightLogo.getMeasuredWidth(), this.mRightLogo.getMeasuredHeight());
        if (changed) {
            updateCircleRect(this.mStaticRect, (float) this.mStaticOffset, true);
        }
    }

    public void setSlidingSide(boolean onLeft) {
        this.mSlidingOnLeft = onLeft;
        if (this.mSlidingOnLeft) {
            this.mLeftLogo.setVisibility(0);
            this.mRightLogo.setVisibility(4);
        } else {
            this.mLeftLogo.setVisibility(4);
            this.mRightLogo.setVisibility(0);
        }
        updateCircleRect(this.mStaticRect, (float) this.mStaticOffset, true);
    }

    public ImageView getLogo() {
        return this.mSlidingOnLeft ? this.mLeftLogo : this.mRightLogo;
    }

    public void setCircleSize(float circleSize) {
        setCircleSize(circleSize, false, null, 0, null);
    }

    public void setCircleSize(float circleSize, boolean animated, Runnable endRunnable, int startDelay, Interpolator interpolator) {
        boolean isAnimating = this.mCircleAnimator != null;
        boolean animationPending = isAnimating && !this.mCircleAnimator.isRunning();
        boolean animatingOut = isAnimating && this.mCircleAnimationEndValue == GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        if (animated || animationPending || animatingOut) {
            if (isAnimating) {
                this.mCircleAnimator.cancel();
            }
            this.mCircleAnimator = ValueAnimator.ofFloat(new float[]{this.mCircleSize, circleSize});
            this.mCircleAnimator.addUpdateListener(this.mCircleUpdateListener);
            this.mCircleAnimator.addListener(this.mClearAnimatorListener);
            this.mCircleAnimator.addListener(new MyAnimatorListenerAdapter(endRunnable));
            this.mCircleAnimator.setInterpolator(interpolator != null ? interpolator : this.mDisappearInterpolator);
            this.mCircleAnimator.setDuration(300);
            this.mCircleAnimator.setStartDelay((long) startDelay);
            this.mCircleAnimator.start();
            this.mCircleAnimationEndValue = circleSize;
        } else if (isAnimating) {
            float diff = circleSize - this.mCircleAnimationEndValue;
            this.mCircleAnimator.getValues()[0].setFloatValues(new float[]{diff, circleSize});
            this.mCircleAnimator.setCurrentPlayTime(this.mCircleAnimator.getCurrentPlayTime());
            this.mCircleAnimationEndValue = circleSize;
        } else {
            applyCircleSize(circleSize);
            updateElevation();
        }
    }

    private void applyCircleSize(float circleSize) {
        this.mCircleSize = circleSize;
        updateLayout();
    }

    private void updateElevation() {
        float t = (((float) this.mStaticOffset) - this.mOffset) / ((float) this.mStaticOffset);
        float f = 1.0f - t;
        float f2 = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        if (f > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            f2 = t;
        }
        setElevation(((float) this.mMaxElevation) * f2);
    }

    public void setOffset(float offset) {
        setOffset(offset, false, 0, null, null);
    }

    private void setOffset(float offset, boolean animate, int startDelay, Interpolator interpolator, final Runnable endRunnable) {
        if (animate) {
            if (this.mOffsetAnimator != null) {
                this.mOffsetAnimator.removeAllListeners();
                this.mOffsetAnimator.cancel();
            }
            float[] fArr = new float[2];
            fArr[0] = this.mOffset;
            boolean z = true;
            fArr[1] = offset;
            this.mOffsetAnimator = ValueAnimator.ofFloat(fArr);
            this.mOffsetAnimator.addUpdateListener(this.mOffsetUpdateListener);
            this.mOffsetAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    SlideOutCircleView.this.mOffsetAnimator = null;
                    if (endRunnable != null) {
                        endRunnable.run();
                    }
                }
            });
            this.mOffsetAnimator.setInterpolator(interpolator != null ? interpolator : this.mDisappearInterpolator);
            this.mOffsetAnimator.setStartDelay((long) startDelay);
            this.mOffsetAnimator.setDuration(300);
            this.mOffsetAnimator.start();
            if (offset == GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                z = false;
            }
            this.mOffsetAnimatingIn = z;
            return;
        }
        this.mOffset = offset;
        updateLayout();
        if (endRunnable != null) {
            endRunnable.run();
        }
    }

    private void updateLayout() {
        updateCircleRect();
        updateLogo();
        invalidateOutline();
        invalidate();
        updateClipping();
    }

    private void updateClipping() {
        boolean clip = this.mCircleSize < ((float) this.mCircleMinSize);
        if (clip != this.mClipToOutline) {
            setClipToOutline(clip);
            this.mClipToOutline = clip;
        }
    }

    private void updateLogo() {
        ImageView mLogo = getLogo();
        boolean exitAnimationRunning = this.mFadeOutAnimator != null;
        Rect rect = exitAnimationRunning ? this.mCircleRect : this.mStaticRect;
        float translationX = (((float) (rect.left + rect.right)) / 2.0f) - (((float) mLogo.getWidth()) / 2.0f);
        float translationY = (((float) (rect.top + rect.bottom)) / 2.0f) - (((float) mLogo.getHeight()) / 2.0f);
        float t = (((float) this.mStaticOffset) - this.mOffset) / ((float) this.mStaticOffset);
        if (exitAnimationRunning) {
            translationY += (this.mOffset - ((float) this.mStaticOffset)) / 2.0f;
        } else {
            translationY += (((float) this.mStaticOffset) * t) * 0.3f;
            float alphaTmp = ((1.0f - t) - 0.5f) * 2.0f;
            float alpha = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            if (alphaTmp > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                alpha = alphaTmp;
            }
            mLogo.setAlpha(alpha);
        }
        mLogo.setTranslationX(translationX);
        mLogo.setTranslationY(translationY);
    }

    private void updateCircleRect() {
        updateCircleRect(this.mCircleRect, this.mOffset, false);
    }

    private void updateCircleRect(Rect rect, float offset, boolean useStaticSize) {
        int left;
        int top;
        float circleSize = useStaticSize ? (float) this.mCircleMinSize : this.mCircleSize;
        if (this.mSlidingOnLeft) {
            left = (int) (((float) (this.mCircleSideMargin + this.mCircleCenterStartPos)) - (circleSize / 2.0f));
            top = (int) (((((float) getHeight()) - (circleSize / 2.0f)) - ((float) this.mBaseMargin)) - offset);
        } else {
            left = (int) (((float) ((getWidth() - this.mCircleSideMargin) - this.mCircleCenterStartPos)) - (circleSize / 2.0f));
            top = (int) (((((float) getHeight()) - (circleSize / 2.0f)) - ((float) this.mBaseMargin)) - offset);
        }
        rect.set(left, top, (int) (((float) left) + circleSize), (int) (((float) top) + circleSize));
    }

    public void setDragDistance(float distance) {
        if (!this.mCircleHidden || this.mDraggedFarEnough) {
            float circleSize = ((float) this.mCircleMinSize) + rubberband(distance);
            if (circleSize > ((float) this.mCircleMaxSize)) {
                circleSize = (float) this.mCircleMaxSize;
            }
            setCircleSize(circleSize);
        }
    }

    private float rubberband(float diff) {
        return (float) Math.pow((double) Math.abs(diff), 0.6000000238418579d);
    }

    public void startAbortAnimation(Runnable endRunnable, boolean animAlpha) {
        setCircleSize(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, true, null, 0, null);
        setOffset(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, true, 0, null, endRunnable);
        if (animAlpha) {
            animate().alpha(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO).setDuration(300).setStartDelay(0).setInterpolator(ALPHA_OUT).start();
        } else {
            setAlpha(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
        }
        this.mCircleHidden = true;
    }

    public void startEnterAnimation() {
        setAlpha(1.0f);
        applyCircleSize(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
        setOffset(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
        setCircleSize((float) this.mCircleMinSize, true, null, 50, null);
        setOffset((float) this.mStaticOffset, true, 50, null, null);
        this.mCircleHidden = false;
    }

    public void startExitAnimation(Runnable endRunnable, boolean isFastSlide) {
        setOffset((((float) getHeight()) / 2.0f) - ((float) this.mBaseMargin), true, 50, this.mFastOutSlowInInterpolator, null);
        setCircleSize((float) Math.ceil(Math.hypot((double) (((float) getWidth()) / 2.0f), (double) (((float) getHeight()) / 2.0f)) * 2.0d), true, null, 50, this.mFastOutSlowInInterpolator);
        performExitFadeOutAnimation(50, 300, endRunnable);
    }

    private void performExitFadeOutAnimation(int startDelay, int duration, final Runnable endRunnable) {
        this.mFadeOutAnimator = ValueAnimator.ofFloat(new float[]{((float) this.mBackgroundPaint.getAlpha()) / 255.0f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        this.mFadeOutAnimator.setInterpolator(new LinearInterpolator());
        this.mFadeOutAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float backgroundValue;
                float animatedFraction = animation.getAnimatedFraction();
                float logoValue = SlideOutCircleView.ALPHA_OUT.getInterpolation(1.0f - (animatedFraction > 0.5f ? 1.0f : animatedFraction / 0.5f));
                if (animatedFraction < 0.2f) {
                    backgroundValue = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                } else {
                    backgroundValue = SlideOutCircleView.ALPHA_OUT.getInterpolation((animatedFraction - 0.2f) / 0.8f);
                }
                float backgroundValue2 = 1.0f - backgroundValue;
                SlideOutCircleView.this.mBackgroundPaint.setAlpha((int) (255.0f * backgroundValue2));
                SlideOutCircleView.this.mOutlineAlpha = backgroundValue2;
                SlideOutCircleView.this.getLogo().setAlpha(logoValue);
                SlideOutCircleView.this.invalidateOutline();
                SlideOutCircleView.this.invalidate();
            }
        });
        this.mFadeOutAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (endRunnable != null) {
                    endRunnable.run();
                }
                SlideOutCircleView.this.getLogo().setAlpha(1.0f);
                SlideOutCircleView.this.mBackgroundPaint.setAlpha(255);
                SlideOutCircleView.this.mOutlineAlpha = 1.0f;
                SlideOutCircleView.this.mFadeOutAnimator = null;
            }
        });
        this.mFadeOutAnimator.setStartDelay((long) startDelay);
        this.mFadeOutAnimator.setDuration((long) duration);
        this.mFadeOutAnimator.start();
    }

    public void setDraggedFarEnough(boolean farEnough) {
        if (farEnough != this.mDraggedFarEnough) {
            if (!farEnough) {
                startAbortAnimation(null, false);
            } else if (this.mCircleHidden) {
                startEnterAnimation();
            }
            this.mDraggedFarEnough = farEnough;
        }
    }

    public void reset() {
        this.mDraggedFarEnough = false;
        this.mCircleHidden = true;
        this.mClipToOutline = false;
        if (this.mFadeOutAnimator != null) {
            this.mFadeOutAnimator.cancel();
        }
        this.mBackgroundPaint.setAlpha(255);
        this.mOutlineAlpha = 1.0f;
    }

    public boolean isAnimationRunning(boolean enterAnimation) {
        return this.mOffsetAnimator != null && enterAnimation == this.mOffsetAnimatingIn;
    }

    public void performOnAnimationFinished(Runnable runnable) {
        if (this.mOffsetAnimator != null) {
            this.mOffsetAnimator.addListener(new MyAnimatorListenerAdapter(runnable));
        } else if (runnable != null) {
            runnable.run();
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
