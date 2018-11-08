package com.android.internal.colorextraction.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;
import com.android.internal.colorextraction.ColorExtractor.GradientColors;
import com.android.internal.graphics.ColorUtils;

public class GradientDrawable extends Drawable {
    private static final float CENTRALIZED_CIRCLE_1 = -2.0f;
    private static final long COLOR_ANIMATION_DURATION = 2000;
    private static final int GRADIENT_RADIUS = 480;
    private static final String TAG = "GradientDrawable";
    private int mAlpha = 255;
    private ValueAnimator mColorAnimation;
    private float mDensity;
    private int mMainColor;
    private final Paint mPaint;
    private int mSecondaryColor;
    private final Splat mSplat;
    private final Rect mWindowBounds;

    static final class Splat {
        final float colorIndex;
        final float radius;
        final float x;
        final float y;

        Splat(float x, float y, float radius, float colorIndex) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.colorIndex = colorIndex;
        }
    }

    public GradientDrawable(Context context) {
        this.mDensity = context.getResources().getDisplayMetrics().density;
        this.mSplat = new Splat(0.5f, 1.0f, 480.0f, CENTRALIZED_CIRCLE_1);
        this.mWindowBounds = new Rect();
        this.mPaint = new Paint();
        this.mPaint.setStyle(Style.FILL);
    }

    public void setColors(GradientColors colors) {
        setColors(colors.getMainColor(), colors.getSecondaryColor(), true);
    }

    public void setColors(GradientColors colors, boolean animated) {
        setColors(colors.getMainColor(), colors.getSecondaryColor(), animated);
    }

    public void setColors(int mainColor, int secondaryColor, boolean animated) {
        if (mainColor != this.mMainColor || secondaryColor != this.mSecondaryColor) {
            if (this.mColorAnimation != null && this.mColorAnimation.isRunning()) {
                this.mColorAnimation.cancel();
            }
            if (animated) {
                int mainFrom = this.mMainColor;
                int secFrom = this.mSecondaryColor;
                ValueAnimator anim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
                anim.setDuration(COLOR_ANIMATION_DURATION);
                anim.addUpdateListener(new -$Lambda$D0plBYSeplKHUImgLxjOl14-7Rw(mainFrom, mainColor, secFrom, secondaryColor, this));
                anim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation, boolean isReverse) {
                        if (GradientDrawable.this.mColorAnimation == animation) {
                            GradientDrawable.this.mColorAnimation = null;
                        }
                    }
                });
                anim.setInterpolator(new DecelerateInterpolator());
                anim.start();
                this.mColorAnimation = anim;
            } else {
                this.mMainColor = mainColor;
                this.mSecondaryColor = secondaryColor;
                buildPaints();
                invalidateSelf();
            }
        }
    }

    /* synthetic */ void lambda$-com_android_internal_colorextraction_drawable_GradientDrawable_3291(int mainFrom, int mainColor, int secFrom, int secondaryColor, ValueAnimator animation) {
        float ratio = ((Float) animation.getAnimatedValue()).floatValue();
        this.mMainColor = ColorUtils.blendARGB(mainFrom, mainColor, ratio);
        this.mSecondaryColor = ColorUtils.blendARGB(secFrom, secondaryColor, ratio);
        buildPaints();
        invalidateSelf();
    }

    public void setAlpha(int alpha) {
        if (alpha != this.mAlpha) {
            this.mAlpha = alpha;
            this.mPaint.setAlpha(this.mAlpha);
            invalidateSelf();
        }
    }

    public int getAlpha() {
        return this.mAlpha;
    }

    public void setXfermode(Xfermode mode) {
        this.mPaint.setXfermode(mode);
        invalidateSelf();
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
    }

    public ColorFilter getColorFilter() {
        return this.mPaint.getColorFilter();
    }

    public int getOpacity() {
        return -3;
    }

    public void setScreenSize(int width, int height) {
        this.mWindowBounds.set(0, 0, width, height);
        setBounds(0, 0, width, height);
        buildPaints();
    }

    private void buildPaints() {
        Rect bounds = this.mWindowBounds;
        if (bounds.width() != 0) {
            this.mPaint.setShader(new RadialGradient(this.mSplat.x * ((float) bounds.width()), this.mSplat.y * ((float) bounds.height()), this.mSplat.radius * this.mDensity, this.mSecondaryColor, this.mMainColor, TileMode.CLAMP));
        }
    }

    public void draw(Canvas canvas) {
        Rect bounds = this.mWindowBounds;
        if (bounds.width() == 0) {
            throw new IllegalStateException("You need to call setScreenSize before drawing.");
        }
        float w = (float) bounds.width();
        float h = (float) bounds.height();
        float x = this.mSplat.x * w;
        float y = this.mSplat.y * h;
        float radius = Math.max(w, h);
        canvas.drawRect(x - radius, y - radius, x + radius, y + radius, this.mPaint);
    }

    public int getMainColor() {
        return this.mMainColor;
    }

    public int getSecondaryColor() {
        return this.mSecondaryColor;
    }
}
