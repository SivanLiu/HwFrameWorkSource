package com.android.server.gesture.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.android.server.gesture.GestureNavConst;

public class HwGestureSideLayout extends RelativeLayout {
    public static final int HIVISION = 1;
    public static final int HIVOICE = 0;
    private static final int ICON_ALPHA_DURATION = 200;
    private static final float ICON_INIT_SCALE = 0.2f;
    private static final int ICON_MAX_TRANSLATE_DISTANCE_DP = 55;
    private static final int ICON_SCALE_DURATION = 350;
    private static final int ICON_SIDE_MARGIN_DP = 16;
    private static final int ICON_SIZE_DP = 56;
    private static final int ICON_TRANSLATION_DURATION = 350;
    private static final int INNER_MASK_DURATION = 350;
    private static final int INNER_MASK_INIT_X_DP = 316;
    private static final int INNER_MASK_INIT_Y_DP = 55;
    private static final float INNER_MASK_MAX_SCALE = 8.0f;
    private static final int INNER_MASK_TRANSLATE_X_DP = -56;
    private static final int INNER_MASK_TRANSLATE_Y_DP = -158;
    public static final int LEFT = 0;
    private static final int OUTER_MASK_ALPHA_DURATION1 = 350;
    private static final int OUTER_MASK_ALPHA_DURATION2 = 100;
    private static final int OUTER_MASK_SCALE_DURATION = 300;
    private static final int OUTER_MASK_START_DELAY_IN_QUICK_START = 450;
    private static final int OUTER_MASK_START_DELAY_IN_SLOW_START = 250;
    private static final float OUT_MASK_FINAL_SCALE = 25.0f;
    private static final float OUT_MASK_INIT_ALPHA = 0.3f;
    private static final float OUT_MASK_INIT_SCALE = 20.0f;
    private static final float RECT_MASK_ALPHA_WHEN_ICON_ANIMATION_END = 0.3f;
    private static final int RECT_MASK_ALPHA_WITH_CIRCLE_MASK_DURATION = 450;
    private static final int RECT_MASK_ALPHA_WITH_ICON_DURATION = 350;
    public static final int RIGHT = 1;
    private static final String TAG = "HwGestureSideLayout";
    private TimeInterpolator mAccelerationInterpolator;
    private float mIconAlphaAnimationEndDistance;
    private float mIconMaxTranslateDistance;
    private ImageView mInnerCircleMask;
    private ImageView mLeftIcon;
    private ImageView mOuterCircleMask;
    private RelativeLayout mRectMask;
    private ImageView mRightIcon;
    private boolean mSlidingOnLeft;
    private TimeInterpolator mStandardInterpolator;

    public HwGestureSideLayout(Context context) {
        this(context, null);
    }

    public HwGestureSideLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HwGestureSideLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HwGestureSideLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mSlidingOnLeft = true;
        float density = context.getResources().getDisplayMetrics().density;
        this.mIconMaxTranslateDistance = 55.0f * density;
        this.mIconAlphaAnimationEndDistance = (this.mIconMaxTranslateDistance * 200.0f) / 350.0f;
        this.mRectMask = new RelativeLayout(context);
        this.mRectMask.setBackgroundColor(getResources().getColor(17170444, context.getTheme()));
        this.mRectMask.setVisibility(4);
        addView(this.mRectMask, new LayoutParams(-1, -1));
        this.mLeftIcon = new ImageView(context);
        this.mLeftIcon.setImageResource(33751800);
        this.mLeftIcon.setVisibility(4);
        LayoutParams leftIconLayoutParams = new LayoutParams((int) (56.0f * density), (int) (56.0f * density));
        leftIconLayoutParams.addRule(9, -1);
        leftIconLayoutParams.addRule(12, -1);
        leftIconLayoutParams.leftMargin = ((int) density) * 16;
        addView(this.mLeftIcon, leftIconLayoutParams);
        this.mRightIcon = new ImageView(context);
        this.mRightIcon.setImageResource(33751800);
        this.mRightIcon.setVisibility(4);
        LayoutParams rightIconLayoutParams = new LayoutParams((int) (56.0f * density), (int) (56.0f * density));
        rightIconLayoutParams.addRule(11, -1);
        rightIconLayoutParams.addRule(12, -1);
        rightIconLayoutParams.rightMargin = ((int) density) * 16;
        addView(this.mRightIcon, rightIconLayoutParams);
        this.mInnerCircleMask = new ImageView(context);
        this.mInnerCircleMask.setImageResource(33751811);
        this.mInnerCircleMask.setVisibility(4);
        this.mInnerCircleMask.setAlpha(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
        LayoutParams innerMaskLayoutParams = new LayoutParams((int) (56.0f * density), (int) (56.0f * density));
        innerMaskLayoutParams.addRule(11, -1);
        innerMaskLayoutParams.addRule(12, -1);
        innerMaskLayoutParams.rightMargin = ((int) density) * 16;
        this.mInnerCircleMask.setTranslationY((-density) * 55.0f);
        addView(this.mInnerCircleMask, innerMaskLayoutParams);
        this.mOuterCircleMask = new ImageView(context);
        this.mOuterCircleMask.setImageResource(33751812);
        this.mOuterCircleMask.setVisibility(4);
        this.mOuterCircleMask.setAlpha(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
        LayoutParams outerMaskLayoutParams = new LayoutParams((int) (56.0f * density), (int) (56.0f * density));
        outerMaskLayoutParams.addRule(11, -1);
        outerMaskLayoutParams.addRule(12, -1);
        outerMaskLayoutParams.rightMargin = ((int) density) * 16;
        this.mOuterCircleMask.setTranslationY((-density) * 55.0f);
        addView(this.mOuterCircleMask, outerMaskLayoutParams);
        this.mStandardInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.mAccelerationInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
    }

    public void applySlowStartAnimation(float progress, int animationType, int animationTarget) {
        Log.d(TAG, "applySlowStartAnimation invoked,  progress: " + progress + " animationType: " + animationType + " animationTarget: " + animationTarget);
        ImageView icon = getTargetIcon(animationTarget);
        if (icon != null) {
            icon.setVisibility(0);
            this.mRectMask.setVisibility(0);
            switch (animationType) {
                case 0:
                    applySlowSwapAnimationHivoiceStyle(progress, animationTarget);
                    break;
                case 1:
                    applySlowSwapAnimationHivisionStyle(progress, animationTarget);
                    break;
            }
        }
    }

    public void onSlowStartAnimationEnd(int animationType, int animationTarget, boolean startMaskAnimation, final Runnable runnable) {
        final ImageView icon = getTargetIcon(animationTarget);
        switch (animationType) {
            case 0:
                this.mRectMask.setVisibility(4);
                if (icon != null) {
                    icon.setVisibility(4);
                    if (runnable != null) {
                        runnable.run();
                        break;
                    }
                }
                return;
                break;
            case 1:
                if (!startMaskAnimation) {
                    this.mRectMask.setVisibility(4);
                    icon.setVisibility(4);
                    if (runnable != null) {
                        runnable.run();
                        break;
                    }
                }
                AnimatorSet maskAnim = getMaskAnimationInSlowStart();
                maskAnim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (runnable != null) {
                            runnable.run();
                        }
                    }

                    public void onAnimationStart(Animator animation) {
                        if (icon != null) {
                            icon.setVisibility(4);
                        }
                        super.onAnimationStart(animation);
                    }
                });
                maskAnim.start();
                break;
                break;
        }
    }

    public void applyQuickStartAnimation(int animationType, int animationTarget, Runnable runnable) {
        Log.d(TAG, "applyQuickStartAnimation begin");
        ImageView icon = getTargetIcon(animationTarget);
        if (icon != null) {
            switch (animationType) {
                case 0:
                    Log.d(TAG, "applyQuickStartAnimation HIVOICE");
                    quickStartAnimationHivoiceStyle(icon, runnable);
                    break;
                case 1:
                    Log.d(TAG, "applyQuickStartAnimation HIVISION");
                    quickStartAnimationHivisonStyle(icon, runnable);
                    break;
            }
        }
    }

    private void quickStartAnimationHivoiceStyle(ImageView icon, final Runnable runnable) {
        AnimatorSet iconAnim = getIconAnimatorSet(icon);
        AnimatorSet rectMaskAnim = getRectMaskAnimatorSet(0);
        AnimatorSet quickStartAnim = new AnimatorSet();
        quickStartAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        quickStartAnim.playTogether(new Animator[]{iconAnim, rectMaskAnim});
        quickStartAnim.start();
    }

    private void quickStartAnimationHivisonStyle(ImageView icon, final Runnable runnable) {
        AnimatorSet iconAnim = getIconAnimatorSet(icon);
        getInnerCircleMaskAnimatorSet(this.mInnerCircleMask).setStartDelay(350);
        getOuterCircleMaskAnimatorSet(this.mOuterCircleMask).setStartDelay(450);
        AnimatorSet rectMaskAnim = getRectMaskAnimatorSet(1);
        AnimatorSet quickStartAnim = new AnimatorSet();
        quickStartAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        quickStartAnim.playTogether(new Animator[]{iconAnim, innerMaskAnimator, outerMaskAnimator, rectMaskAnim});
        quickStartAnim.start();
    }

    private AnimatorSet getIconAnimatorSet(final ImageView icon) {
        ObjectAnimator iconTransitionAnim = ObjectAnimator.ofFloat(icon, "translationY", new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, -this.mIconMaxTranslateDistance});
        iconTransitionAnim.setInterpolator(this.mStandardInterpolator);
        iconTransitionAnim.setDuration(350);
        ObjectAnimator iconScaleXAnim = ObjectAnimator.ofFloat(icon, "ScaleX", new float[]{0.2f, 1.0f});
        iconScaleXAnim.setInterpolator(this.mStandardInterpolator);
        iconScaleXAnim.setDuration(350);
        ObjectAnimator iconScaleYAnim = ObjectAnimator.ofFloat(icon, "ScaleY", new float[]{0.2f, 1.0f});
        iconScaleYAnim.setInterpolator(this.mStandardInterpolator);
        iconScaleYAnim.setDuration(350);
        ObjectAnimator iconAlphaAnim = ObjectAnimator.ofFloat(icon, "Alpha", new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 1.0f});
        iconAlphaAnim.setInterpolator(this.mStandardInterpolator);
        iconAlphaAnim.setDuration(200);
        AnimatorSet iconQuickSwapAnim = new AnimatorSet();
        iconQuickSwapAnim.playTogether(new Animator[]{iconTransitionAnim, iconScaleXAnim, iconScaleYAnim, iconAlphaAnim});
        iconQuickSwapAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                icon.setVisibility(0);
            }

            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                icon.setVisibility(4);
            }
        });
        return iconQuickSwapAnim;
    }

    private void applySlowSwapAnimationHivoiceStyle(float progress, int animationTarget) {
        ImageView icon = getTargetIcon(animationTarget);
        if (icon != null) {
            float dy = progress * this.mIconMaxTranslateDistance;
            float scale = 0.2f + (0.8f * progress);
            float alpha = dy / this.mIconAlphaAnimationEndDistance;
            icon.setTranslationY(-dy);
            icon.setScaleX(scale);
            icon.setScaleY(scale);
            icon.setAlpha(alpha);
            this.mRectMask.setAlpha(progress * 0.3f);
        }
    }

    private void applySlowSwapAnimationHivisionStyle(float progress, int animationTarget) {
        applySlowSwapAnimationHivoiceStyle(progress, animationTarget);
    }

    private ImageView getTargetIcon(int animationTarget) {
        if (animationTarget == 0) {
            return this.mLeftIcon;
        }
        if (animationTarget == 1) {
            return this.mRightIcon;
        }
        return null;
    }

    private AnimatorSet getMaskAnimationInSlowStart() {
        AnimatorSet innerMaskAnimator = getInnerCircleMaskAnimatorSet(this.mInnerCircleMask);
        getOuterCircleMaskAnimatorSet(this.mOuterCircleMask).setStartDelay(250);
        ObjectAnimator rectMaskAlpha = ObjectAnimator.ofFloat(this.mRectMask, "Alpha", new float[]{0.3f, 1.0f});
        rectMaskAlpha.setDuration(450);
        rectMaskAlpha.setInterpolator(this.mStandardInterpolator);
        rectMaskAlpha.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                HwGestureSideLayout.this.mRectMask.setVisibility(0);
            }

            public void onAnimationEnd(Animator animation, boolean isReverse) {
                super.onAnimationEnd(animation);
                HwGestureSideLayout.this.mRectMask.setVisibility(4);
            }
        });
        AnimatorSet maskAnimator = new AnimatorSet();
        maskAnimator.playTogether(new Animator[]{innerMaskAnimator, outerMaskAnimator, rectMaskAlpha});
        return maskAnimator;
    }

    private AnimatorSet getRectMaskAnimatorSet(int animationType) {
        ObjectAnimator rectMaskAlphaAnimWithIcon = ObjectAnimator.ofFloat(this.mRectMask, "Alpha", new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.3f});
        rectMaskAlphaAnimWithIcon.setDuration(350);
        rectMaskAlphaAnimWithIcon.setInterpolator(this.mStandardInterpolator);
        ObjectAnimator rectMaskAlphaAnimWithCircleMask = ObjectAnimator.ofFloat(this.mRectMask, "Alpha", new float[]{0.3f, 1.0f});
        rectMaskAlphaAnimWithCircleMask.setDuration(450);
        rectMaskAlphaAnimWithCircleMask.setInterpolator(this.mAccelerationInterpolator);
        rectMaskAlphaAnimWithCircleMask.setStartDelay(350);
        AnimatorSet rectMaskAnimator = new AnimatorSet();
        switch (animationType) {
            case 0:
                rectMaskAnimator.play(rectMaskAlphaAnimWithIcon);
                break;
            case 1:
                rectMaskAnimator.playTogether(new Animator[]{rectMaskAlphaAnimWithIcon, rectMaskAlphaAnimWithCircleMask});
                break;
        }
        rectMaskAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                HwGestureSideLayout.this.mRectMask.setVisibility(0);
            }

            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                HwGestureSideLayout.this.mRectMask.setVisibility(4);
            }
        });
        return rectMaskAnimator;
    }

    private AnimatorSet getOuterCircleMaskAnimatorSet(final ImageView outerMask) {
        ObjectAnimator outerMaskAlpha1 = ObjectAnimator.ofFloat(outerMask, "Alpha", new float[]{0.3f, 1.0f});
        outerMaskAlpha1.setDuration(350);
        outerMaskAlpha1.setInterpolator(this.mStandardInterpolator);
        ObjectAnimator outerMaskAlpha2 = ObjectAnimator.ofFloat(outerMask, "Alpha", new float[]{1.0f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        outerMaskAlpha2.setDuration(100);
        outerMaskAlpha2.setInterpolator(this.mStandardInterpolator);
        outerMaskAlpha2.setStartDelay(350);
        ObjectAnimator outerMaskScaleX = ObjectAnimator.ofFloat(outerMask, "ScaleX", new float[]{OUT_MASK_INIT_SCALE, OUT_MASK_FINAL_SCALE});
        outerMaskScaleX.setDuration(300);
        outerMaskScaleX.setInterpolator(this.mStandardInterpolator);
        ObjectAnimator outerMaskScaleY = ObjectAnimator.ofFloat(outerMask, "ScaleY", new float[]{OUT_MASK_INIT_SCALE, OUT_MASK_FINAL_SCALE});
        outerMaskScaleY.setDuration(300);
        outerMaskScaleY.setInterpolator(this.mStandardInterpolator);
        AnimatorSet outerMaskAnimator = new AnimatorSet();
        outerMaskAnimator.playTogether(new Animator[]{outerMaskAlpha1, outerMaskAlpha2, outerMaskScaleX, outerMaskScaleY});
        outerMaskAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                outerMask.setVisibility(0);
            }

            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                outerMask.setVisibility(4);
            }
        });
        return outerMaskAnimator;
    }

    private AnimatorSet getInnerCircleMaskAnimatorSet(final ImageView innerMask) {
        float density = getContext().getResources().getDisplayMetrics().density;
        ObjectAnimator innerMaskAlpha = ObjectAnimator.ofFloat(innerMask, "Alpha", new float[]{1.0f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        innerMaskAlpha.setInterpolator(this.mStandardInterpolator);
        innerMaskAlpha.setDuration(350);
        ObjectAnimator innerMaskScaleX = ObjectAnimator.ofFloat(innerMask, "ScaleX", new float[]{1.0f, INNER_MASK_MAX_SCALE});
        innerMaskScaleX.setInterpolator(this.mStandardInterpolator);
        innerMaskScaleX.setDuration(350);
        ObjectAnimator innerMaskScaleY = ObjectAnimator.ofFloat(innerMask, "ScaleY", new float[]{1.0f, INNER_MASK_MAX_SCALE});
        innerMaskScaleY.setInterpolator(this.mStandardInterpolator);
        innerMaskScaleY.setDuration(350);
        ObjectAnimator innerMaskTransitionX = ObjectAnimator.ofFloat(innerMask, "translationX", new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, -56.0f * density});
        innerMaskTransitionX.setInterpolator(this.mStandardInterpolator);
        innerMaskTransitionX.setDuration(350);
        ObjectAnimator innerMaskTransitionY = ObjectAnimator.ofFloat(innerMask, "translationY", new float[]{(-density) * 55.0f, -158.0f * density});
        innerMaskTransitionY.setInterpolator(this.mStandardInterpolator);
        innerMaskTransitionY.setDuration(350);
        AnimatorSet innerMaskAnimator = new AnimatorSet();
        innerMaskAnimator.playTogether(new Animator[]{innerMaskAlpha, innerMaskScaleX, innerMaskScaleY, innerMaskTransitionX, innerMaskTransitionY});
        innerMaskAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                innerMask.setVisibility(0);
            }

            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                innerMask.setVisibility(4);
            }
        });
        return innerMaskAnimator;
    }

    public void setSlidingSide(boolean onLeft) {
        this.mSlidingOnLeft = onLeft;
    }

    public ImageView getLogo() {
        return this.mSlidingOnLeft ? this.mLeftIcon : this.mRightIcon;
    }

    public void setSlidingProcess(float process) {
        applySlowStartAnimation(process, this.mSlidingOnLeft ? 0 : 1, this.mSlidingOnLeft ? 0 : 1);
    }

    public void startEnterAnimation() {
        applySlowStartAnimation(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, this.mSlidingOnLeft ? 0 : 1, this.mSlidingOnLeft ? 0 : 1);
    }

    public void startExitAnimation(Runnable runnable, boolean isFastSlide, boolean startMaskAnimation) {
        Log.d(TAG, "startExitAnimation isFastSlide: " + isFastSlide);
        int target = this.mSlidingOnLeft ? 0 : 1;
        int type = this.mSlidingOnLeft ? 0 : 1;
        if (isFastSlide) {
            applyQuickStartAnimation(type, target, runnable);
        } else {
            onSlowStartAnimationEnd(type, target, startMaskAnimation, runnable);
        }
    }
}
