package com.android.server.wm;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.HwMwUtils;
import android.util.Log;
import android.util.Slog;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.view.animation.ScaleAnimation;
import com.android.server.AttributeCache;
import com.android.server.wm.utils.HwDisplaySizeUtil;

public class HwAppTransitionImpl implements IHwAppTransition {
    private static final TimeInterpolator[] ALPHA_INTERPOLATORS = {CONST_INTERPOLATOR, new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f), CONST_INTERPOLATOR};
    private static final float ALPHA_KEY_1_FRACTION = 0.16f;
    private static final float ALPHA_KEY_2_FRACTION = 0.32f;
    private static final TimeInterpolator CONST_INTERPOLATOR = new TimeInterpolator() {
        /* class com.android.server.wm.HwAppTransitionImpl.AnonymousClass1 */

        public float getInterpolation(float input) {
            return 1.0f;
        }
    };
    private static final boolean DBG = false;
    private static final long EXIT_ANIM_FOR_LOWPERM_DURATION = 300;
    private static final long FIND_ICON_ANIM_DURATION = 350;
    private static final long FIND_NO_ICON_ANIM_DURATION = 200;
    private static final float FOLDER_ICON_FINAL_SCALE_RATIO = 0.4f;
    public static final boolean IS_EMUI_LITE = SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);
    public static final boolean IS_NOVA_PERF = SystemProperties.getBoolean("ro.config.hw_nova_performance", false);
    private static final float LAUNCHER_ENTER_ALPHA_TIME_RATIO = 0.2f;
    private static final float LAUNCHER_ENTER_HIDE_TIME_RATIO = 0.3f;
    private static final float LAUNCHER_ENTER_HIDE_TIME_RATIO_LITE = 0.16f;
    private static final float LAUNCHER_ENTER_SCALE_TIME_RATIO = 0.7f;
    private static final float LAUNCHER_FROM_SCALE = 0.93f;
    private static final PathInterpolator LAUNCHER_SCALE_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.1f, 1.0f);
    private static final float LAZY_MODE_COMP_FACTOR = 0.125f;
    private static final float LAZY_MODE_WIN_SCALE_FACTOR = 0.75f;
    private static final int LEFT_SINGLE_HAND_MODE = 1;
    private static final float MAX_ALPHA = 1.0f;
    private static final float MAX_FRACTION = 1.0f;
    private static final float MAX_SCALE = 1.0f;
    private static final float MID_SCALE_X_RATIO_HORIZANTAL = 0.54f;
    private static final float MIN_ALPHA = 0.0f;
    private static final float MIN_FRACTION = 0.0f;
    private static final float MIN_SCALE = 0.0f;
    private static final float PIVOT_COMPENSATION = 0.5f;
    private static final int RES_ID_FLAG_MASK = -16777216;
    private static final int RES_ID_FLAG_SYSTEM = 16777216;
    private static final int RIGHT_SINGLE_HAND_MODE = 2;
    private static final float SCALE_KEY_1_FRACTION = 0.16f;
    private static final TimeInterpolator[] SIZE_BIG_INTERPOLATORS = {new PathInterpolator(0.44f, 0.43f, 0.7f, 0.75f), new PathInterpolator(0.13f, 0.79f, LAUNCHER_ENTER_HIDE_TIME_RATIO, 1.0f)};
    private static final TimeInterpolator[] SIZE_SMALL_INTERPOLATORS = {new PathInterpolator(0.41f, 0.38f, 0.7f, 0.71f), new PathInterpolator(0.16f, 0.64f, 0.33f, 1.0f)};
    private static final String TAG = "HwAppTransitionImpl";
    private static final float TWO_CONST = 2.0f;
    private Context mHwextContext = null;

    public AttributeCache.Entry overrideAnimation(WindowManager.LayoutParams lp, int animAttr, Context mContext, AttributeCache.Entry mEnt, AppTransition appTransition) {
        int anim;
        int hwAnimResId;
        int hwAnimResId2;
        if (lp != null) {
            int windowAnimations = lp.windowAnimations;
            if ((RES_ID_FLAG_MASK & windowAnimations) != RES_ID_FLAG_SYSTEM) {
                Slog.d(TAG, "windowAnimations = " + Integer.toHexString(windowAnimations) + " dose not come from system, not to override it.");
                return mEnt;
            }
        }
        AttributeCache.Entry ent = null;
        if (mEnt == null) {
            return null;
        }
        Context context = mEnt.context;
        if (mEnt.array.getResourceId(animAttr, 0) != 0) {
            if (this.mHwextContext == null) {
                try {
                    this.mHwextContext = mContext.createPackageContext("androidhwext", 0);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "overrideAnimation : no hwext package");
                }
            }
            if (this.mHwextContext != null) {
                int anim2 = 0;
                String title = lp != null ? lp.getTitle().toString() : "";
                if (title != null && !title.equals("")) {
                    if (IS_EMUI_LITE || IS_NOVA_PERF) {
                        Resources resources = this.mHwextContext.getResources();
                        hwAnimResId2 = resources.getIdentifier("HwAnimation_lite." + title, "style", "androidhwext");
                    } else {
                        Resources resources2 = this.mHwextContext.getResources();
                        hwAnimResId2 = resources2.getIdentifier("HwAnimation." + title, "style", "androidhwext");
                    }
                    if (!(hwAnimResId2 == 0 || (ent = appTransition.getCachedAnimations("androidhwext", hwAnimResId2)) == null)) {
                        Context context2 = ent.context;
                        anim2 = ent.array.getResourceId(animAttr, 0);
                    }
                }
                if ((IS_EMUI_LITE || IS_NOVA_PERF) && anim2 == 0 && (hwAnimResId = this.mHwextContext.getResources().getIdentifier("HwAnimation_lite", "style", "androidhwext")) != 0 && (ent = appTransition.getCachedAnimations("androidhwext", hwAnimResId)) != null) {
                    Context context3 = ent.context;
                    anim2 = ent.array.getResourceId(animAttr, 0);
                }
                if (anim2 == 0) {
                    int hwAnimResId3 = this.mHwextContext.getResources().getIdentifier("HwAnimation", "style", "androidhwext");
                    if (hwAnimResId3 != 0) {
                        ent = appTransition.getCachedAnimations("androidhwext", hwAnimResId3);
                        if (ent != null) {
                            Context context4 = ent.context;
                            anim = ent.array.getResourceId(animAttr, 0);
                        } else {
                            anim = anim2;
                        }
                    } else {
                        anim = anim2;
                    }
                } else {
                    anim = anim2;
                }
                if (anim == 0) {
                    return null;
                }
            }
        }
        return ent;
    }

    private static float[] adjustPivotsInLazyMode(float originPivotX, float originPivotY, int iconWidth, int iconHeight, WindowState win, int lazyMode) {
        float[] pivots = {originPivotX, originPivotY};
        if (lazyMode != 0) {
            Rect bounds = win.getBounds();
            if (bounds == null) {
                Slog.w(TAG, "bounds is null! return.");
                return pivots;
            }
            Slog.d(TAG, "app exit to launcher, lazymode bounds = " + bounds);
            float winStartX = 0.0f;
            float winStartY = ((float) bounds.height()) * 0.25f;
            if (lazyMode == 2) {
                winStartX = ((float) bounds.width()) * 0.25f;
            }
            float compensationX = ((float) iconWidth) * LAZY_MODE_COMP_FACTOR;
            float compensationY = ((float) iconHeight) * LAZY_MODE_COMP_FACTOR;
            int safeWidth = 0;
            if (win.mContext != null) {
                safeWidth = HwDisplaySizeUtil.getInstance(win.mWmService).getSafeSideWidth();
            }
            float safeWidthCompensation = ((float) safeWidth) + (((float) safeWidth) * LAZY_MODE_COMP_FACTOR);
            if (lazyMode == 1) {
                compensationX = -compensationX;
                safeWidthCompensation = -safeWidthCompensation;
            }
            pivots[0] = (((originPivotX - safeWidthCompensation) * 0.75f) + winStartX) - compensationX;
            pivots[1] = ((originPivotY * 0.75f) + winStartY) - compensationY;
        }
        return pivots;
    }

    /* JADX INFO: Multiple debug info for r4v6 float[]: [D('iconLeft' float), D('scaleOutValuesX' float[])] */
    static Animation createAppExitToIconAnimation(AppWindowToken atoken, int containingHeight, int iconWidth, int iconHeight, float originPivotX, float originPivotY, Bitmap icon, int exitFlag, int lazyMode) {
        int finalIconWidth;
        long duration;
        float pivotY;
        TimeInterpolator[] sizeXInterpolators;
        TimeInterpolator[] sizeYInterpolators;
        if (atoken == null) {
            Slog.w(TAG, "create app exit animation find no app window token!");
            return null;
        }
        WindowState window = atoken.findMainWindow(false);
        if (window == null) {
            Slog.w(TAG, "create app exit animation find no app main window!");
            return null;
        }
        Rect winAnimFrame = window.getDisplayFrameLw();
        if (winAnimFrame == null) {
            Slog.w(TAG, "create app exit animation find no winDisplayFrame!");
            return null;
        }
        Rect winDecorFrame = window.getDecorFrame();
        if (winDecorFrame == null) {
            Slog.w(TAG, "create app exit animation find no winDecorFrame!");
            return null;
        }
        winAnimFrame.intersect(winDecorFrame);
        int winWidth = winAnimFrame.width();
        int winHeight = winAnimFrame.height();
        if (winWidth > 0) {
            if (winHeight > 0) {
                boolean isHorizontal = winWidth > winHeight;
                float middleYRatio = MID_SCALE_X_RATIO_HORIZANTAL;
                float middleXRatio = isHorizontal ? 0.54f : 0.45999998f;
                if (isHorizontal) {
                    middleYRatio = 0.45999998f;
                }
                float middleX = 1.0f - ((((float) (winWidth - iconWidth)) * middleXRatio) / ((float) winWidth));
                float middleY = 1.0f - ((((float) (winHeight - iconHeight)) * middleYRatio) / ((float) winHeight));
                int finalIconHeight = iconHeight;
                if (exitFlag == 1) {
                    finalIconHeight = (int) (((float) iconHeight) * 0.4f);
                    finalIconWidth = (int) (((float) iconWidth) * 0.4f);
                } else {
                    finalIconWidth = iconWidth;
                }
                float toY = ((float) finalIconHeight) / ((float) winHeight);
                float toX = ((float) finalIconWidth) / ((float) winWidth);
                float[] pivots = adjustPivotsInLazyMode(originPivotX, originPivotY, iconWidth, iconHeight, window, lazyMode);
                float pivotXAdj = pivots[0];
                float pivotYAdj = pivots[1];
                float iconLeft = pivotXAdj - (((float) finalIconWidth) / 2.0f);
                float iconTop = pivotYAdj - (((float) finalIconHeight) / 2.0f);
                float iconRight = (((float) finalIconWidth) / 2.0f) + pivotXAdj;
                float iconBottom = (((float) finalIconHeight) / 2.0f) + pivotYAdj;
                float pivotX = ((((float) winAnimFrame.right) * iconLeft) - (((float) winAnimFrame.left) * iconRight)) / (((((float) winAnimFrame.right) - iconRight) + iconLeft) - ((float) winAnimFrame.left));
                float pivotY2 = ((((float) winAnimFrame.bottom) * iconTop) - (((float) winAnimFrame.top) * iconBottom)) / (((((float) winAnimFrame.bottom) - iconBottom) + iconTop) - ((float) winAnimFrame.top));
                if (window.mWmService.getFoldDisplayMode() == 3) {
                    pivotY2 *= window.mWmService.mSubFoldModeScale;
                    pivotX *= window.mWmService.mSubFoldModeScale;
                }
                if (window.getFrameLw() == null) {
                    Slog.w(TAG, "create app exit animation find no app window frame!");
                    return null;
                }
                if (HwWmConstants.IS_APP_LOW_PERF_ANIM) {
                    duration = 300;
                } else {
                    duration = 350;
                }
                AnimationSet appExitToIconAnimation = new AnimationSet(false);
                appExitToIconAnimation.addAnimation(createAppAlphaAnimation(duration));
                if (!HwMwUtils.ENABLED || !window.inHwMagicWindowingMode()) {
                    pivotY = pivotY2;
                } else {
                    Bundle bundle = HwMwUtils.performPolicy(105, new Object[]{window, new float[]{originPivotX, originPivotY}, Integer.valueOf(iconWidth), Integer.valueOf(iconHeight)});
                    toX = bundle.getFloat("BUNDLE_EXITANIM_SCALETOX", 0.0f);
                    toY = bundle.getFloat("BUNDLE_EXITANIM_SCALETOY", 0.0f);
                    pivotX = bundle.getFloat("BUNDLE_EXITANIM_PIVOTX", 0.0f);
                    pivotY = bundle.getFloat("BUNDLE_EXITANIM_PIVOTY", 0.0f);
                }
                float[] scaleInValues = {0.0f, 0.16f, 1.0f};
                float[] scaleOutValuesX = {1.0f, middleX, toX};
                float[] scaleOutValuesY = {1.0f, middleY, toY};
                if (isHorizontal) {
                    sizeXInterpolators = SIZE_BIG_INTERPOLATORS;
                } else {
                    sizeXInterpolators = SIZE_SMALL_INTERPOLATORS;
                }
                if (isHorizontal) {
                    sizeYInterpolators = SIZE_SMALL_INTERPOLATORS;
                } else {
                    sizeYInterpolators = SIZE_BIG_INTERPOLATORS;
                }
                PhaseInterpolator interpolatorX = new PhaseInterpolator(scaleInValues, scaleOutValuesX, sizeXInterpolators);
                ScaleAnimation scaleXAnim = new ScaleAnimation(0.0f, 1.0f, 1.0f, 1.0f, pivotX, pivotY);
                scaleXAnim.setFillEnabled(true);
                scaleXAnim.setFillBefore(true);
                scaleXAnim.setFillAfter(true);
                scaleXAnim.setDuration(duration);
                scaleXAnim.setInterpolator(interpolatorX);
                appExitToIconAnimation.addAnimation(scaleXAnim);
                PhaseInterpolator interpolatorY = new PhaseInterpolator(scaleInValues, scaleOutValuesY, sizeYInterpolators);
                ScaleAnimation scaleYAnim = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f, pivotX, pivotY);
                scaleYAnim.setFillEnabled(true);
                scaleYAnim.setFillBefore(true);
                scaleYAnim.setFillAfter(true);
                scaleYAnim.setDuration(duration);
                scaleYAnim.setInterpolator(interpolatorY);
                appExitToIconAnimation.addAnimation(scaleYAnim);
                appExitToIconAnimation.setZAdjustment(1);
                if (atoken.mShouldDrawIcon && icon != null) {
                    window.mWinAnimator.setWindowIconInfo(0 | 1, iconWidth, iconHeight, icon);
                }
                return appExitToIconAnimation;
            }
        }
        return null;
    }

    private static Animation createAppAlphaAnimation(long duration) {
        AlphaAnimation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
        alphaAnim.setDuration(duration);
        alphaAnim.setFillEnabled(true);
        alphaAnim.setFillBefore(true);
        alphaAnim.setFillAfter(true);
        alphaAnim.setInterpolator(new PhaseInterpolator(new float[]{0.0f, 0.16f, ALPHA_KEY_2_FRACTION, 1.0f}, new float[]{1.0f, 1.0f, 0.0f, 0.0f}, ALPHA_INTERPOLATORS));
        return alphaAnim;
    }

    static Animation createLauncherEnterAnimation(AppWindowToken atoken, int containingHeight, int iconWidth, int iconHeight, float originPivotX, float originPivotY, Bitmap iconBitmap) {
        float pivotX;
        long duration;
        if (atoken == null) {
            Slog.w(TAG, "create launcher enter animation find no app window token!");
            return null;
        }
        WindowState window = atoken.findMainWindow();
        if (window == null) {
            Slog.w(TAG, "create launcher enter animation find no app main window!");
            return null;
        }
        Rect winDisplayFrame = window.getDisplayFrameLw();
        if (winDisplayFrame == null) {
            Slog.w(TAG, "create launcher enter animation find no winDisplayFrame!");
            return null;
        }
        int winWidth = winDisplayFrame.width();
        int winHeight = winDisplayFrame.height();
        if (winWidth <= 0 || winHeight <= 0) {
            return null;
        }
        float[] pivots = adjustPivotsInLazyMode(originPivotX, originPivotY, iconWidth, iconHeight, window, window.mWmService.getLazyMode());
        float pivotXAdj = pivots[0];
        float pivotYAdj = pivots[1];
        float iconLeft = pivotXAdj - (((float) iconWidth) / 2.0f);
        float iconTop = pivotYAdj - (((float) iconHeight) / 2.0f);
        float iconRight = (((float) iconWidth) / 2.0f) + pivotXAdj;
        float iconBottom = (((float) iconHeight) / 2.0f) + pivotYAdj;
        float pivotX2 = ((((float) winDisplayFrame.right) * iconLeft) - (((float) winDisplayFrame.left) * iconRight)) / (((((float) winDisplayFrame.right) - iconRight) + iconLeft) - ((float) winDisplayFrame.left));
        float pivotY = ((((float) winDisplayFrame.bottom) * iconTop) - (((float) winDisplayFrame.top) * iconBottom)) / (((((float) winDisplayFrame.bottom) - iconBottom) + iconTop) - ((float) winDisplayFrame.top));
        if (originPivotX < 0.0f || originPivotY < 0.0f) {
            pivotX = ((float) winWidth) / 2.0f;
            pivotY = ((float) winHeight) / 2.0f;
        } else {
            pivotX = pivotX2;
        }
        if (window.mWmService.getFoldDisplayMode() == 3) {
            pivotY *= window.mWmService.mSubFoldModeScale;
            pivotX *= window.mWmService.mSubFoldModeScale;
        }
        if (iconWidth < 0 || iconHeight < 0 || iconBitmap == null) {
            duration = 200;
        } else {
            duration = 350;
        }
        AnimationSet launcherEnterAnimation = new AnimationSet(false);
        launcherEnterAnimation.addAnimation(createLauncherScaleAnimation(duration, pivotX, pivotY));
        launcherEnterAnimation.addAnimation(createLauncherAlphaAnimation(duration));
        launcherEnterAnimation.setDetachWallpaper(true);
        launcherEnterAnimation.setZAdjustment(0);
        return launcherEnterAnimation;
    }

    private static Animation createLauncherScaleAnimation(long duration, float pivotX, float pivotY) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(LAUNCHER_FROM_SCALE, 1.0f, LAUNCHER_FROM_SCALE, 1.0f, pivotX, pivotY);
        scaleAnimation.setFillEnabled(true);
        scaleAnimation.setFillBefore(true);
        scaleAnimation.setFillAfter(true);
        scaleAnimation.setStartOffset((long) (((float) duration) * (IS_EMUI_LITE ? 0.16f : LAUNCHER_ENTER_HIDE_TIME_RATIO)));
        scaleAnimation.setDuration((long) (((float) duration) * 0.7f));
        scaleAnimation.setInterpolator(LAUNCHER_SCALE_INTERPOLATOR);
        return scaleAnimation;
    }

    private static Animation createLauncherAlphaAnimation(long duration) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setFillEnabled(true);
        alphaAnimation.setFillBefore(true);
        alphaAnimation.setFillAfter(true);
        alphaAnimation.setDuration((long) (((float) duration) * 0.2f));
        alphaAnimation.setStartOffset((long) (((float) duration) * (IS_EMUI_LITE ? 0.16f : LAUNCHER_ENTER_HIDE_TIME_RATIO)));
        alphaAnimation.setInterpolator(new LinearInterpolator());
        return alphaAnimation;
    }
}
