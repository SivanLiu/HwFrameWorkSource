package huawei.android.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.PathInterpolator;

public class HwViewAnimationUtils {
    private static final long FAB_ANIM_DURATION = 100;
    private static final float FAB_ANIM_SCALE_BIG = 2.0f;
    private static final long MASK_ANIM_ENTER_DURATION = 300;
    private static final long MASK_ANIM_EXIT_DURATION = 300;
    private static final long MASK_ANIM_EXIT_START_OFFSET = 50;
    private static final long REVEAL_ANIM_DURATION = 300;
    private static final String TAG = "HwViewAnimationUtils";

    public static Animator createCircularRevealAnimator(boolean isEnter, View fab, View content, View mask) {
        return createCircularRevealAnimator(isEnter, fab, content, mask, -1);
    }

    public static Animator createCircularRevealAnimator(boolean isEnter, View fab, View content, View mask, int maskColor) {
        final View view = fab;
        final View view2 = content;
        final View view3 = mask;
        if (view == null || view2 == null) {
            Log.w(TAG, "createCircularRevealAnimator:  fab and content should not be null.");
            return null;
        }
        int i;
        float startRadius;
        Animator maskAlphaAnimator;
        TimeInterpolator frictionInterpolator = new PathInterpolator(0.2f, 0.0f, 0.2f, 1.0f);
        AnimatorSet animatorSet = new AnimatorSet();
        AnimatorSet fabAnimatorSet = new AnimatorSet();
        Animator fabAlphaAnimator = "alpha";
        float[] fArr = new float[2];
        fArr[0] = isEnter ? 1.0f : 0.0f;
        fArr[1] = isEnter ? 0.0f : 1.0f;
        fabAlphaAnimator = ObjectAnimator.ofFloat(view, fabAlphaAnimator, fArr);
        Animator fabScaleXAnimator = "scaleX";
        float[] fArr2 = new float[2];
        fArr2[0] = isEnter ? 1.0f : FAB_ANIM_SCALE_BIG;
        fArr2[1] = isEnter ? FAB_ANIM_SCALE_BIG : 1.0f;
        fabScaleXAnimator = ObjectAnimator.ofFloat(view, fabScaleXAnimator, fArr2);
        String str = "scaleY";
        Animator fabScaleYAnimator = new float[2];
        fabScaleYAnimator[0] = isEnter ? 1.0f : FAB_ANIM_SCALE_BIG;
        fabScaleYAnimator[1] = isEnter ? FAB_ANIM_SCALE_BIG : 1.0f;
        fabScaleYAnimator = ObjectAnimator.ofFloat(view, str, fabScaleYAnimator);
        fabAnimatorSet.playTogether(new Animator[]{fabAlphaAnimator, fabScaleXAnimator, fabScaleYAnimator});
        fabAlphaAnimator.setDuration(FAB_ANIM_DURATION);
        fabScaleXAnimator.setDuration(FAB_ANIM_DURATION);
        fabScaleYAnimator.setDuration(FAB_ANIM_DURATION);
        if (!isEnter) {
            fabAnimatorSet.setStartDelay(200);
        }
        if (isEnter) {
            fabAnimatorSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(4);
                }
            });
        }
        int[] contentLocation = new int[2];
        int[] fabLocation = new int[2];
        view2.getLocationInWindow(contentLocation);
        view.getLocationInWindow(fabLocation);
        int centerX = (fabLocation[0] - contentLocation[0]) + ((int) ((((float) fab.getWidth()) * fab.getScaleX()) / FAB_ANIM_SCALE_BIG));
        int centerY = ((int) ((((float) fab.getHeight()) * fab.getScaleY()) / FAB_ANIM_SCALE_BIG)) + (fabLocation[1] - contentLocation[1]);
        float hypot2 = (float) Math.hypot((double) centerX, (double) (centerY - content.getWidth()));
        float hypot3 = (float) Math.hypot((double) (centerX - content.getWidth()), (double) centerY);
        float hypot4 = (float) Math.hypot((double) (centerX - content.getWidth()), (double) (centerY - content.getHeight()));
        float hypot = Math.max(Math.max(Math.max((float) Math.hypot((double) centerX, (double) centerY), hypot2), hypot3), hypot4);
        if (isEnter) {
            i = 2;
            startRadius = (float) (fab.getWidth() / 2);
        } else {
            i = 2;
            startRadius = hypot;
        }
        Animator revealAnimator = ViewAnimationUtils.createCircularReveal(view2, centerX, centerY, startRadius, isEnter ? hypot : (float) (fab.getWidth() / i));
        revealAnimator.setDuration(4.2E-43f);
        revealAnimator.setInterpolator(frictionInterpolator);
        if (!isEnter) {
            revealAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    view2.setVisibility(4);
                }
            });
        }
        if (view3 != null) {
            if (maskColor >= 0) {
                mask.setBackgroundColor(maskColor);
            }
            maskAlphaAnimator = null;
            String str2 = "alpha";
            float[] fArr3 = new float[2];
            fArr3[0] = isEnter ? 1.0f : 0.0f;
            fArr3[1] = isEnter ? 0.0f : 1.0f;
            fabScaleYAnimator = ObjectAnimator.ofFloat(view3, str2, fArr3);
            if (isEnter) {
                fabScaleYAnimator.setDuration(300);
                fabScaleYAnimator.setInterpolator(frictionInterpolator);
            } else {
                fabScaleYAnimator.setDuration(300);
                fabScaleYAnimator.setStartDelay(MASK_ANIM_EXIT_START_OFFSET);
                fabScaleYAnimator.setInterpolator(frictionInterpolator);
            }
            if (isEnter) {
                fabScaleYAnimator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        view3.setVisibility(4);
                    }
                });
            }
            maskAlphaAnimator = fabScaleYAnimator;
        } else {
            maskAlphaAnimator = null;
            float f = hypot4;
        }
        if (maskAlphaAnimator != null) {
            animatorSet.playTogether(new Animator[]{fabAnimatorSet, revealAnimator, maskAlphaAnimator});
        } else {
            animatorSet.playTogether(new Animator[]{fabAnimatorSet, revealAnimator});
        }
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                view.setVisibility(0);
                view2.setVisibility(0);
                if (view3 != null) {
                    view3.setVisibility(0);
                }
            }
        });
        return animatorSet;
    }
}
