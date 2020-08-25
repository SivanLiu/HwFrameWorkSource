package com.android.server.multiwin.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Slog;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.HwMultiWindowManager;
import com.android.server.wm.HwMultiWindowSplitUI;
import com.android.server.wm.HwSplitBarConstants;

public abstract class HwSplitBarExitAniStrategy {
    private static final long ALPHA_DURATION = 100;
    static final long DEFAULT_ANIMATION_TIME = 300;
    static final float FLOAT_NUM_TWO = 2.0f;
    private static final String TAG = "HwSplitBarExitAniStrategy";
    static final String TRANSLATION_X = "translationX";
    static final String TRANSLATION_Y = "translationY";
    ActivityTaskManagerService mAtms;
    float mCurPos;
    View mDragBar;
    int mHeight;
    int mHeightColumns;
    View mLeft;
    View mRight;
    int mWidth;
    int mWidthColumns;

    /* access modifiers changed from: package-private */
    public abstract long getAniDuration();

    /* access modifiers changed from: package-private */
    public abstract float getDragBarTransDis();

    /* access modifiers changed from: package-private */
    public abstract void getScaleAnim(float f);

    /* access modifiers changed from: package-private */
    public abstract int getScaleUpStartLen();

    /* access modifiers changed from: package-private */
    public abstract float getTranslateDistance();

    public HwSplitBarExitAniStrategy(ActivityTaskManagerService service, View left, View dragBar, View right, Bundle bundle) {
        this.mLeft = left;
        this.mDragBar = dragBar;
        this.mRight = right;
        this.mAtms = service;
        if (bundle != null) {
            this.mCurPos = bundle.getFloat(HwSplitBarConstants.CURRENT_POSITION);
            this.mWidth = bundle.getInt(HwSplitBarConstants.DISPLAY_WIDTH);
            this.mHeight = bundle.getInt(HwSplitBarConstants.DISPLAY_HEIGHT);
            this.mWidthColumns = bundle.getInt(HwMultiWindowManager.WIDTH_COLUMNS);
            this.mHeightColumns = bundle.getInt(HwMultiWindowManager.HEIGHT_COLUMNS);
        }
    }

    public static HwSplitBarExitAniStrategy getStrategy(ActivityTaskManagerService service, View left, View dragBar, View right, Bundle bundle) {
        if (bundle == null) {
            Slog.i(TAG, " bundle is null, return null of strategy");
            return null;
        }
        int exitRegion = bundle.getInt(HwSplitBarConstants.EXIT_REGION);
        if (exitRegion == 1) {
            return new HwLeftStackExitAnim(service, left, dragBar, right, bundle);
        }
        if (exitRegion == 2) {
            return new HwRightStackExitAnim(service, left, dragBar, right, bundle);
        }
        if (exitRegion == 3) {
            return new HwTopStackExitAnim(service, left, dragBar, right, bundle);
        }
        if (exitRegion != 4) {
            return null;
        }
        return new HwBottomStackExitAnim(service, left, dragBar, right, bundle);
    }

    /* access modifiers changed from: package-private */
    public View getScaleDownView() {
        return this.mLeft;
    }

    /* access modifiers changed from: package-private */
    public View getScaleUpView() {
        return this.mRight;
    }

    /* access modifiers changed from: package-private */
    public int getScaleUpEndLen() {
        return this.mWidth;
    }

    /* access modifiers changed from: package-private */
    public String getTranslateDirect() {
        return TRANSLATION_X;
    }

    public void split2FullAnimation() {
        Interpolator standardCurve = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
        ObjectAnimator scaleDownAni = ObjectAnimator.ofFloat(getScaleDownView(), getTranslateDirect(), getTranslateDistance());
        scaleDownAni.setInterpolator(standardCurve);
        ObjectAnimator dragBarAni = ObjectAnimator.ofFloat(this.mDragBar, getTranslateDirect(), getDragBarTransDis());
        dragBarAni.setInterpolator(standardCurve);
        ValueAnimator scaleUpAni = ValueAnimator.ofInt(getScaleUpStartLen(), getScaleUpEndLen()).setDuration(Math.abs(getAniDuration()));
        scaleUpAni.setInterpolator(standardCurve);
        View view = this.mLeft;
        view.setPivotX((float) (view.getWidth() / 2));
        View view2 = this.mLeft;
        view2.setPivotY((float) (view2.getHeight() / 2));
        ScaleAnimListener listener = new ScaleAnimListener();
        scaleUpAni.addUpdateListener(listener);
        scaleUpAni.addListener(listener);
        AnimatorSet animatorSets = new AnimatorSet();
        animatorSets.setDuration(Math.abs(getAniDuration()));
        animatorSets.playTogether(scaleDownAni, dragBarAni, scaleUpAni);
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(getScaleUpView(), View.ALPHA, getScaleUpView().getAlpha(), 0.0f);
        alphaAnim.setDuration(ALPHA_DURATION);
        AnimatorSet allSets = new AnimatorSet();
        allSets.playSequentially(animatorSets, alphaAnim);
        allSets.addListener(new AnimListener());
        allSets.start();
    }

    private class ScaleAnimListener implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        private ScaleAnimListener() {
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            if (animation.getAnimatedValue() != null) {
                float value = 1.0f;
                if (animation.getAnimatedValue() instanceof Integer) {
                    value = (float) ((Integer) animation.getAnimatedValue()).intValue();
                }
                HwSplitBarExitAniStrategy.this.getScaleAnim(value);
            }
        }

        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            Slog.i(HwSplitBarExitAniStrategy.TAG, " set layout background color");
            HwMultiWindowSplitUI.getInstance(HwSplitBarExitAniStrategy.this.mAtms.getUiContext(), HwSplitBarExitAniStrategy.this.mAtms).setLayoutBackground();
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public void onAnimationCancel(Animator animation) {
            HwMultiWindowSplitUI.getInstance(HwSplitBarExitAniStrategy.this.mAtms.getUiContext(), HwSplitBarExitAniStrategy.this.mAtms).setLayoutBackground();
        }
    }

    private class AnimListener implements Animator.AnimatorListener {
        private AnimListener() {
        }

        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            HwSplitBarExitAniStrategy.this.mDragBar.setVisibility(8);
            HwMultiWindowManager.getInstance(HwSplitBarExitAniStrategy.this.mAtms).removeSplitScreenDividerBar(100, false);
            Slog.i(HwSplitBarExitAniStrategy.TAG, "on animation end");
        }

        public void onAnimationCancel(Animator animation) {
            HwMultiWindowManager.getInstance(HwSplitBarExitAniStrategy.this.mAtms).removeSplitScreenDividerBar(100, false);
            Slog.i(HwSplitBarExitAniStrategy.TAG, "on animation cancel");
        }

        public void onAnimationRepeat(Animator animation) {
        }
    }
}
