package android.graphics.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.FloatProperty;
import android.util.MathUtils;
import android.view.DisplayListCanvas;
import android.view.RenderNodeAnimator;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import java.util.ArrayList;

class RippleForeground extends RippleComponent {
    private static final TimeInterpolator DECELERATE_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final FloatProperty<RippleForeground> OPACITY = new FloatProperty<RippleForeground>("opacity") {
        public void setValue(RippleForeground object, float value) {
            object.mOpacity = value;
            object.onAnimationPropertyChanged();
        }

        public Float get(RippleForeground object) {
            return Float.valueOf(object.mOpacity);
        }
    };
    private static final int OPACITY_ENTER_DURATION = 75;
    private static final int OPACITY_EXIT_DURATION = 150;
    private static final int OPACITY_HOLD_DURATION = 225;
    private static final int RIPPLE_ENTER_DURATION = 225;
    private static final int RIPPLE_ORIGIN_DURATION = 225;
    private static final FloatProperty<RippleForeground> TWEEN_ORIGIN = new FloatProperty<RippleForeground>("tweenOrigin") {
        public void setValue(RippleForeground object, float value) {
            object.mTweenX = value;
            object.mTweenY = value;
            object.onAnimationPropertyChanged();
        }

        public Float get(RippleForeground object) {
            return Float.valueOf(object.mTweenX);
        }
    };
    private static final FloatProperty<RippleForeground> TWEEN_RADIUS = new FloatProperty<RippleForeground>("tweenRadius") {
        public void setValue(RippleForeground object, float value) {
            object.mTweenRadius = value;
            object.onAnimationPropertyChanged();
        }

        public Float get(RippleForeground object) {
            return Float.valueOf(object.mTweenRadius);
        }
    };
    private final AnimatorListenerAdapter mAnimationListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            RippleForeground.this.mHasFinishedExit = true;
            RippleForeground.this.pruneHwFinished();
            RippleForeground.this.pruneSwFinished();
            if (RippleForeground.this.mRunningHwAnimators.isEmpty()) {
                RippleForeground.this.clearHwProps();
            }
        }
    };
    private float mClampedStartingX;
    private float mClampedStartingY;
    private long mEnterStartedAtMillis;
    private final boolean mForceSoftware;
    private boolean mHasFinishedExit;
    private float mOpacity = 0.0f;
    private ArrayList<RenderNodeAnimator> mPendingHwAnimators = new ArrayList();
    private CanvasProperty<Paint> mPropPaint;
    private CanvasProperty<Float> mPropRadius;
    private CanvasProperty<Float> mPropX;
    private CanvasProperty<Float> mPropY;
    private ArrayList<RenderNodeAnimator> mRunningHwAnimators = new ArrayList();
    private ArrayList<Animator> mRunningSwAnimators = new ArrayList();
    private float mStartRadius = 0.0f;
    private float mStartingX;
    private float mStartingY;
    private float mTargetX = 0.0f;
    private float mTargetY = 0.0f;
    private float mTweenRadius = 0.0f;
    private float mTweenX = 0.0f;
    private float mTweenY = 0.0f;
    private boolean mUsingProperties;

    public RippleForeground(RippleDrawable owner, Rect bounds, float startingX, float startingY, boolean forceSoftware) {
        super(owner, bounds);
        this.mForceSoftware = forceSoftware;
        this.mStartingX = startingX;
        this.mStartingY = startingY;
        this.mStartRadius = ((float) Math.max(bounds.width(), bounds.height())) * 0.3f;
        clampStartingPosition();
    }

    protected void onTargetRadiusChanged(float targetRadius) {
        clampStartingPosition();
        switchToUiThreadAnimation();
    }

    private void drawSoftware(Canvas c, Paint p) {
        int origAlpha = p.getAlpha();
        int alpha = (int) ((((float) origAlpha) * this.mOpacity) + 1056964608);
        float radius = getCurrentRadius();
        if (alpha > 0 && radius > 0.0f) {
            float x = getCurrentX();
            float y = getCurrentY();
            p.setAlpha(alpha);
            c.drawCircle(x, y, radius, p);
            p.setAlpha(origAlpha);
        }
    }

    private void startPending(DisplayListCanvas c) {
        if (!this.mPendingHwAnimators.isEmpty()) {
            for (int i = 0; i < this.mPendingHwAnimators.size(); i++) {
                RenderNodeAnimator animator = (RenderNodeAnimator) this.mPendingHwAnimators.get(i);
                animator.setTarget(c);
                animator.start();
                this.mRunningHwAnimators.add(animator);
            }
            this.mPendingHwAnimators.clear();
        }
    }

    private void pruneHwFinished() {
        if (!this.mRunningHwAnimators.isEmpty()) {
            for (int i = this.mRunningHwAnimators.size() - 1; i >= 0; i--) {
                if (!((RenderNodeAnimator) this.mRunningHwAnimators.get(i)).isRunning()) {
                    this.mRunningHwAnimators.remove(i);
                }
            }
        }
    }

    private void pruneSwFinished() {
        if (!this.mRunningSwAnimators.isEmpty()) {
            for (int i = this.mRunningSwAnimators.size() - 1; i >= 0; i--) {
                if (!((Animator) this.mRunningSwAnimators.get(i)).isRunning()) {
                    this.mRunningSwAnimators.remove(i);
                }
            }
        }
    }

    private void drawHardware(DisplayListCanvas c, Paint p) {
        startPending(c);
        pruneHwFinished();
        if (this.mPropPaint != null) {
            this.mUsingProperties = true;
            c.drawCircle(this.mPropX, this.mPropY, this.mPropRadius, this.mPropPaint);
            return;
        }
        this.mUsingProperties = false;
        drawSoftware(c, p);
    }

    public void getBounds(Rect bounds) {
        int outerX = (int) this.mTargetX;
        int outerY = (int) this.mTargetY;
        int r = ((int) this.mTargetRadius) + 1;
        bounds.set(outerX - r, outerY - r, outerX + r, outerY + r);
    }

    public void move(float x, float y) {
        this.mStartingX = x;
        this.mStartingY = y;
        clampStartingPosition();
    }

    public boolean hasFinishedExit() {
        return this.mHasFinishedExit;
    }

    private long computeFadeOutDelay() {
        long timeSinceEnter = AnimationUtils.currentAnimationTimeMillis() - this.mEnterStartedAtMillis;
        if (timeSinceEnter <= 0 || timeSinceEnter >= 225) {
            return 0;
        }
        return 225 - timeSinceEnter;
    }

    private void startSoftwareEnter() {
        for (int i = 0; i < this.mRunningSwAnimators.size(); i++) {
            ((Animator) this.mRunningSwAnimators.get(i)).cancel();
        }
        this.mRunningSwAnimators.clear();
        ObjectAnimator tweenRadius = ObjectAnimator.ofFloat((Object) this, TWEEN_RADIUS, 1.0f);
        tweenRadius.setDuration(225);
        tweenRadius.setInterpolator(DECELERATE_INTERPOLATOR);
        tweenRadius.start();
        this.mRunningSwAnimators.add(tweenRadius);
        ObjectAnimator tweenOrigin = ObjectAnimator.ofFloat((Object) this, TWEEN_ORIGIN, 1.0f);
        tweenOrigin.setDuration(225);
        tweenOrigin.setInterpolator(DECELERATE_INTERPOLATOR);
        tweenOrigin.start();
        this.mRunningSwAnimators.add(tweenOrigin);
        ObjectAnimator opacity = ObjectAnimator.ofFloat((Object) this, OPACITY, 1.0f);
        opacity.setDuration(75);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);
        opacity.start();
        this.mRunningSwAnimators.add(opacity);
    }

    private void startSoftwareExit() {
        ObjectAnimator opacity = ObjectAnimator.ofFloat((Object) this, OPACITY, 0.0f);
        opacity.setDuration(150);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);
        opacity.addListener(this.mAnimationListener);
        opacity.setStartDelay(computeFadeOutDelay());
        opacity.start();
        this.mRunningSwAnimators.add(opacity);
    }

    private void startHardwareEnter() {
        if (!this.mForceSoftware) {
            this.mPropX = CanvasProperty.createFloat(getCurrentX());
            this.mPropY = CanvasProperty.createFloat(getCurrentY());
            this.mPropRadius = CanvasProperty.createFloat(getCurrentRadius());
            Paint paint = this.mOwner.getRipplePaint();
            this.mPropPaint = CanvasProperty.createPaint(paint);
            RenderNodeAnimator radius = new RenderNodeAnimator(this.mPropRadius, this.mTargetRadius);
            radius.setDuration(225);
            radius.setInterpolator(DECELERATE_INTERPOLATOR);
            this.mPendingHwAnimators.add(radius);
            RenderNodeAnimator x = new RenderNodeAnimator(this.mPropX, this.mTargetX);
            x.setDuration(225);
            x.setInterpolator(DECELERATE_INTERPOLATOR);
            this.mPendingHwAnimators.add(x);
            RenderNodeAnimator y = new RenderNodeAnimator(this.mPropY, this.mTargetY);
            y.setDuration(225);
            y.setInterpolator(DECELERATE_INTERPOLATOR);
            this.mPendingHwAnimators.add(y);
            RenderNodeAnimator opacity = new RenderNodeAnimator(this.mPropPaint, 1, (float) paint.getAlpha());
            opacity.setDuration(75);
            opacity.setInterpolator(LINEAR_INTERPOLATOR);
            opacity.setStartValue(0.0f);
            this.mPendingHwAnimators.add(opacity);
            invalidateSelf();
        }
    }

    private void startHardwareExit() {
        if (!this.mForceSoftware && this.mPropPaint != null) {
            RenderNodeAnimator opacity = new RenderNodeAnimator(this.mPropPaint, 1, 0.0f);
            opacity.setDuration(150);
            opacity.setInterpolator(LINEAR_INTERPOLATOR);
            opacity.addListener(this.mAnimationListener);
            opacity.setStartDelay(computeFadeOutDelay());
            opacity.setStartValue((float) this.mOwner.getRipplePaint().getAlpha());
            this.mPendingHwAnimators.add(opacity);
            invalidateSelf();
        }
    }

    public final void enter() {
        this.mEnterStartedAtMillis = AnimationUtils.currentAnimationTimeMillis();
        startSoftwareEnter();
        startHardwareEnter();
    }

    public final void exit() {
        startSoftwareExit();
        startHardwareExit();
    }

    private float getCurrentX() {
        return MathUtils.lerp(this.mClampedStartingX - this.mBounds.exactCenterX(), this.mTargetX, this.mTweenX);
    }

    private float getCurrentY() {
        return MathUtils.lerp(this.mClampedStartingY - this.mBounds.exactCenterY(), this.mTargetY, this.mTweenY);
    }

    private float getCurrentRadius() {
        return MathUtils.lerp(this.mStartRadius, this.mTargetRadius, this.mTweenRadius);
    }

    public void draw(Canvas c, Paint p) {
        boolean hasDisplayListCanvas = !this.mForceSoftware && (c instanceof DisplayListCanvas);
        pruneSwFinished();
        if (hasDisplayListCanvas) {
            drawHardware((DisplayListCanvas) c, p);
        } else {
            drawSoftware(c, p);
        }
    }

    private void clampStartingPosition() {
        float cX = this.mBounds.exactCenterX();
        float cY = this.mBounds.exactCenterY();
        float dX = this.mStartingX - cX;
        float dY = this.mStartingY - cY;
        float r = this.mTargetRadius - this.mStartRadius;
        if ((dX * dX) + (dY * dY) > r * r) {
            double angle = Math.atan2((double) dY, (double) dX);
            this.mClampedStartingX = ((float) (Math.cos(angle) * ((double) r))) + cX;
            this.mClampedStartingY = ((float) (Math.sin(angle) * ((double) r))) + cY;
            return;
        }
        this.mClampedStartingX = this.mStartingX;
        this.mClampedStartingY = this.mStartingY;
    }

    public void end() {
        int i = 0;
        for (int i2 = 0; i2 < this.mRunningSwAnimators.size(); i2++) {
            ((Animator) this.mRunningSwAnimators.get(i2)).end();
        }
        this.mRunningSwAnimators.clear();
        while (i < this.mRunningHwAnimators.size()) {
            ((RenderNodeAnimator) this.mRunningHwAnimators.get(i)).end();
            i++;
        }
        this.mRunningHwAnimators.clear();
    }

    private void onAnimationPropertyChanged() {
        if (!this.mUsingProperties) {
            invalidateSelf();
        }
    }

    private void clearHwProps() {
        this.mPropPaint = null;
        this.mPropRadius = null;
        this.mPropX = null;
        this.mPropY = null;
        this.mUsingProperties = false;
    }

    private void switchToUiThreadAnimation() {
        for (int i = 0; i < this.mRunningHwAnimators.size(); i++) {
            Animator animator = (Animator) this.mRunningHwAnimators.get(i);
            animator.removeListener(this.mAnimationListener);
            animator.end();
        }
        this.mRunningHwAnimators.clear();
        clearHwProps();
        invalidateSelf();
    }
}
