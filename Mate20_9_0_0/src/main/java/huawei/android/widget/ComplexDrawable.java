package huawei.android.widget;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import huawei.android.widget.loader.ResLoader;
import huawei.android.widget.loader.ResLoaderUtil;

public class ComplexDrawable extends Drawable {
    private static final float MAX_SCALE = 1.42f;
    private int mAnimationDuration;
    private AnimatorUpdateListener mAnimatorListener;
    private ValueAnimator mCheckAnim;
    private Path mClipPath;
    private Context mContext;
    private Rect mDrawableRect;
    private Drawable mDstDrawable;
    private int mIconActiveColor;
    private int mIconBounds;
    private int mIconDefaultColor;
    private int mRadius = 0;
    private Drawable mSrcDrawable;
    private ValueAnimator mUnCheckAnim;

    public ComplexDrawable(Context context, Drawable srcDrawable) {
        this.mContext = context;
        this.mAnimationDuration = ResLoaderUtil.getResources(context).getInteger(ResLoader.getInstance().getIdentifier(context, "integer", "bottomnav_icon_anim_duration"));
        this.mIconBounds = ResLoaderUtil.getDimensionPixelSize(context, "bottomnav_item_icon_size");
        this.mDrawableRect = new Rect(0, 0, this.mIconBounds, this.mIconBounds);
        setSrcDrawable(srcDrawable);
        this.mAnimatorListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ComplexDrawable.this.setRadius(((Integer) valueAnimator.getAnimatedValue()).intValue());
            }
        };
        this.mClipPath = new Path();
        initAnim();
    }

    private void initAnim() {
        Interpolator INTERPOLATOR_40_80 = AnimationUtils.loadInterpolator(this.mContext, this.mContext.getResources().getIdentifier("fast_out_slow_in", "interpolator", "android"));
        Interpolator INTERPOLATOR_20_80 = AnimationUtils.loadInterpolator(this.mContext, ResLoader.getInstance().getIdentifier(this.mContext, "interpolator", "cubic_bezier_interpolator_type_20_80"));
        this.mCheckAnim = ValueAnimator.ofInt(new int[]{0, (int) (((float) this.mIconBounds) * MAX_SCALE)});
        this.mCheckAnim.setDuration((long) this.mAnimationDuration);
        this.mCheckAnim.addUpdateListener(this.mAnimatorListener);
        this.mCheckAnim.setInterpolator(INTERPOLATOR_20_80);
        this.mUnCheckAnim = ValueAnimator.ofInt(new int[]{(int) (((float) this.mIconBounds) * MAX_SCALE), 0});
        this.mUnCheckAnim.setDuration((long) this.mAnimationDuration);
        this.mUnCheckAnim.addUpdateListener(this.mAnimatorListener);
        this.mUnCheckAnim.setInterpolator(INTERPOLATOR_40_80);
    }

    public void draw(Canvas canvas) {
        this.mClipPath.reset();
        this.mClipPath.addCircle((float) (getLayoutDirection() == 1 ? this.mIconBounds : this.mDrawableRect.left), (float) this.mDrawableRect.bottom, (float) this.mRadius, Direction.CCW);
        canvas.save();
        canvas.clipOutPath(this.mClipPath);
        this.mSrcDrawable.draw(canvas);
        canvas.restore();
        canvas.save();
        canvas.clipPath(this.mClipPath);
        this.mDstDrawable.draw(canvas);
        canvas.restore();
    }

    public void setAlpha(int alpha) {
        if (this.mSrcDrawable != null) {
            this.mSrcDrawable.setAlpha(alpha);
        }
    }

    public void setColorFilter(ColorFilter colorFilter) {
        if (this.mSrcDrawable != null) {
            this.mSrcDrawable.setColorFilter(colorFilter);
        }
    }

    public int getOpacity() {
        if (this.mSrcDrawable != null) {
            return this.mSrcDrawable.getOpacity();
        }
        return -3;
    }

    void setSrcDrawable(int drawableRes) {
        setSrcDrawable(this.mContext.getResources().getDrawable(drawableRes));
    }

    void setSrcDrawable(Drawable drawable) {
        if (drawable instanceof StateListDrawable) {
            StateListDrawable sld = (StateListDrawable) drawable;
            int attr = this.mContext.getResources().getIdentifier("state_selected", "attr", "android");
            int[] empty_set = new int[0];
            int[] selected_set = new int[]{attr};
            int[] unselected_set = new int[]{~attr};
            Drawable srcDrawable = null;
            Drawable dstDrawable = null;
            int stateDrawableIndex = sld.getStateDrawableIndex(unselected_set);
            int index = stateDrawableIndex;
            if (stateDrawableIndex != -1) {
                srcDrawable = sld.getStateDrawable(index);
            }
            stateDrawableIndex = sld.getStateDrawableIndex(selected_set);
            index = stateDrawableIndex;
            if (stateDrawableIndex != -1) {
                dstDrawable = sld.getStateDrawable(index);
            }
            if (srcDrawable == null && dstDrawable == null) {
                setSrcAndDst(drawable, drawable.getConstantState().newDrawable().mutate());
                return;
            } else if (srcDrawable == null || dstDrawable == null) {
                stateDrawableIndex = sld.getStateDrawableIndex(empty_set);
                index = stateDrawableIndex;
                if (stateDrawableIndex != -1) {
                    Drawable stateDrawable;
                    Drawable stateDrawable2;
                    if (srcDrawable == null) {
                        stateDrawable = sld.getStateDrawable(index);
                    } else {
                        stateDrawable = srcDrawable;
                    }
                    if (dstDrawable == null) {
                        stateDrawable2 = sld.getStateDrawable(index);
                    } else {
                        stateDrawable2 = dstDrawable;
                    }
                    setSrcAndDst(stateDrawable, stateDrawable2);
                    return;
                }
                throw new IllegalArgumentException("no resource available to provide");
            } else {
                setSrcAndDst(srcDrawable, dstDrawable);
                return;
            }
        }
        setSrcAndDst(drawable, drawable.getConstantState().newDrawable().mutate());
    }

    private void setSrcAndDst(Drawable srcDrawable, Drawable dstDrawable) {
        if (srcDrawable != null && dstDrawable != null) {
            this.mSrcDrawable = srcDrawable;
            this.mSrcDrawable.setTint(this.mIconDefaultColor);
            this.mSrcDrawable.setBounds(this.mDrawableRect);
            this.mDstDrawable = dstDrawable;
            this.mDstDrawable.setTint(this.mIconActiveColor);
            this.mDstDrawable.setBounds(this.mDrawableRect);
        }
    }

    private void setRadius(int mRadius) {
        this.mRadius = mRadius;
        invalidateSelf();
    }

    void setActiveColor(int iconActiveColor) {
        if (this.mIconActiveColor != iconActiveColor) {
            this.mIconActiveColor = iconActiveColor;
            if (this.mDstDrawable != null) {
                this.mDstDrawable.setTint(this.mIconActiveColor);
            }
            invalidateSelf();
        }
    }

    void setDefaultColor(int iconDefaultColor) {
        if (this.mIconDefaultColor != iconDefaultColor) {
            this.mIconDefaultColor = iconDefaultColor;
            if (this.mSrcDrawable != null) {
                this.mSrcDrawable.setTint(this.mIconDefaultColor);
            }
            invalidateSelf();
        }
    }

    private void startAnim(boolean checkedState) {
        ValueAnimator outdatedAnim = checkedState ? this.mUnCheckAnim : this.mCheckAnim;
        ValueAnimator currentAnim = checkedState ? this.mCheckAnim : this.mUnCheckAnim;
        if (outdatedAnim.isRunning()) {
            outdatedAnim.reverse();
        } else {
            currentAnim.start();
        }
    }

    void setState(boolean checked, boolean useAnim) {
        if (useAnim) {
            startAnim(checked);
        } else {
            setRadius(checked ? (int) (((float) this.mIconBounds) * MAX_SCALE) : 0);
        }
    }
}
