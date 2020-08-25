package com.android.server.magicwin;

import android.content.Context;
import android.graphics.Rect;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import com.android.server.wm.WindowManagerService;

public class HwMagicWinAnimation {
    private static final int ALPHA_OFFSET = 0;
    private static final float CENTER = 0.5f;
    private static final float CENTER_AXIS_X = 0.5f;
    private static final float CENTER_SCALE_X = 0.75f;
    private static final float DEFAULT_AXIS_Y = 0.5f;
    private static final float DEFAULT_MIN = 0.01f;
    private static final float DEFAULT_SCALE = 1.0f;
    private static final int DURATION_ALPHA_ENTER = 350;
    private static final int DURATION_RIGHT_ENTER_A1AN = 350;
    private static final int DURATION_RIGHT_EXIT = 350;
    private static final int DURATION_ROTATION_ANIMATION = 350;
    private static final int DURATION_TRANSLATION_ENTER = 350;
    private static final int ENTER_INDEX = 0;
    private static final float EXIT_FROM_SCALE = 1.0f;
    private static final int EXIT_INDEX = 1;
    private static final float EXIT_TO_SCALE = 0.8f;
    private static final float FROM_ALPHA = 1.0f;
    private static final float HALF_FACTOR = 2.0f;
    public static final float INVALID_THRESHOLD = 0.0f;
    private static final float LAUCHER_EXIT_SCALE = 0.9f;
    private static final float LEFT_AXIS_X = 0.25f;
    private static final float MAX_ALPHA = 1.0f;
    private static final float MAX_SCALE = 1.0f;
    private static final float MIN_ALPHA = 0.0f;
    private static final float MIN_ALPHA_EXIT = 0.85f;
    private static final float MIN_SCALE_ENTER = 0.85f;
    private static final int NUM_ANIMATIONSET = 2;
    private static final int PARAM_INDEX_ONE = 1;
    private static final int PARAM_INDEX_THREE = 3;
    private static final int PARAM_INDEX_TWO = 2;
    private static final int PARAM_INDEX_ZERO = 0;
    private static final float PIVOT_COMPENSATION = 0.5f;
    private static final float RIGHT_AXIS_X = 0.75f;
    private static final int ROTATION_FOCUS_CENTER = 0;
    private static final int ROTATION_FOCUS_INVALID = -999;
    private static final int ROTATION_FOCUS_LEFT = -1;
    private static final int ROTATION_FOCUS_RIGHT = 1;
    private static final float SCALE_HALF = 0.5f;
    private static final int SCENE_ANAN_RIGHT_TO_LEFT = 200;
    private static final int SCENE_DEFAULT = -1;
    private static final int SCENE_EXIT = 100;
    private static final int SCENE_EXIT_RIGHT_TO_RIGHT = 103;
    private static final int SCENE_LEFT_TO_RIGHT = 0;
    private static final int SCENE_MIDDILE_TO_RIGHT = 2;
    private static final int SCENE_MIDDLE = 101;
    private static final int SCENE_MIDDLE_OPEN_APP = 102;
    private static final int SCENE_RIGHT_TO_RIGHT = 1;
    private static final float SPLITSCREEN_ALPHA_FRACTION = 0.9f;
    private static final int SPLITSCREEN_ANIMATION_DURATION = 1000;
    private static final float SPLITSCREEN_FRACTION = 0.55f;
    private static final String TAG = "HwMagicWinAnimation";
    private static final float TO_ALPHA = 0.0f;
    private static final int WALLPAPER_CLOSE_ANIMATION_DURATION = 200;
    private static final float WALLPAPER_CLOSE_ANIMATION_FRACTION = 0.5f;
    private static final float ZERO_POINT = 0.0f;
    /* access modifiers changed from: private */
    public boolean isAnimationRunning = false;
    private Interpolator mAlphaInterpolator;
    /* access modifiers changed from: private */
    public AnimationScene mAnimationScene;
    private Context mContext;
    private int mDurationAlphaEnter = 350;
    private int mDurationRightEnterA1An = 350;
    private int mDurationRightExit = 350;
    private int mDurationRotation = 350;
    private int mDurationTranslationEnter = 350;
    private int mFocus;
    private Rect mFocusBounds;
    private int mFocusMode;
    /* access modifiers changed from: private */
    public Interpolator mInterpolator;
    /* access modifiers changed from: private */
    public HwMagicWindowService mService;
    private Interpolator mSplitAnimInterpolator;
    private int mTopActivityWidth;
    private WindowManagerService mWms;

    public HwMagicWinAnimation(Context context, HwMagicWindowService hwMagicWindowService, WindowManagerService windowManagerService) {
        this.mContext = context;
        this.mService = hwMagicWindowService;
        this.mWms = windowManagerService;
        this.mAnimationScene = new AnimationScene(this.mService);
        this.mInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563661);
        this.mAlphaInterpolator = new Interpolator() {
            /* class com.android.server.magicwin.HwMagicWinAnimation.AnonymousClass1 */

            public float getInterpolation(float input) {
                if (input < 0.9f) {
                    return 0.0f;
                }
                return HwMagicWinAnimation.this.mInterpolator.getInterpolation((input - 0.9f) / 0.100000024f);
            }
        };
        this.mSplitAnimInterpolator = new Interpolator() {
            /* class com.android.server.magicwin.HwMagicWinAnimation.AnonymousClass2 */

            public float getInterpolation(float input) {
                if (input >= 0.55f) {
                    return 1.0f;
                }
                return HwMagicWinAnimation.this.mInterpolator.getInterpolation(input / 0.55f);
            }
        };
        this.mWms.getWindowManagerServiceEx().setMagicWindowMoveInterpolator(new HwMagicWinMoveInterpolator(this));
    }

    public void setFocusBound(Rect focus) {
        Slog.d(TAG, "setFocusBound " + focus);
        Rect unused = this.mAnimationScene.mFocusBound = focus;
    }

    public void setIsMiddleOnClickBefore(boolean isMiddle) {
        boolean unused = this.mAnimationScene.mIsMiddle = isMiddle;
    }

    public void overrideStartActivityAnimation(Rect next, boolean isRightToLeft, boolean isTransition, boolean allDrawn) {
        Slog.d(TAG, "overrideStartActivityAnimation, params is  next = " + next + ", isRightToLeft = " + isRightToLeft + ", isTransition = " + isTransition + ", allDrawn = " + allDrawn);
        int startScene = this.mAnimationScene.getSceneForStart(isRightToLeft, allDrawn, isTransition, next);
        int unused = this.mAnimationScene.mLastStartScene = startScene;
        overrideTranslationAnimations(true, startScene);
    }

    public void overrideFinishActivityAnimation(Rect finishBound, boolean isMoveToMiddleOrRight, boolean isTransition, boolean isRightEmpty) {
        int finishScene = this.mAnimationScene.getSceneForFinish(isRightEmpty, finishBound, isMoveToMiddleOrRight);
        if (isTransition) {
            finishScene = -1;
        }
        overrideTranslationAnimations(false, finishScene);
    }

    /* JADX INFO: Multiple debug info for r3v1 android.view.animation.AnimationSet: [D('enterSet' android.view.animation.AnimationSet), D('exitSet' android.view.animation.AnimationSet)] */
    private void overrideTranslationAnimations(boolean isStart, int scene) {
        float animationScale = getTransitionAnimationScale();
        this.mDurationTranslationEnter = (int) (350.0f / animationScale);
        this.mDurationAlphaEnter = (int) (350.0f / animationScale);
        this.mDurationRightExit = (int) (350.0f / animationScale);
        this.mDurationRightEnterA1An = (int) (350.0f / animationScale);
        AnimationSet[] animationSets = new AnimationSet[2];
        AnimationSet enterSet = new AnimationSet(false);
        AnimationSet exitSet = new AnimationSet(false);
        Slog.d(TAG, "overrideTranslationAnimations, scene = " + scene + "isStart" + isStart);
        if (scene != -1) {
            if (scene == 0) {
                animationSets = setLeftToRightAnimation(animationSets, enterSet, exitSet);
            } else if (scene == 1) {
                animationSets = setRightToRightAnimation(animationSets, enterSet, exitSet);
            } else if (scene == 2) {
                animationSets = setMiddleToRightAnimation(animationSets, enterSet, exitSet);
            } else if (scene != 200) {
                switch (scene) {
                    case 100:
                        animationSets = setExitAnimation(animationSets, enterSet, exitSet);
                        break;
                    case 101:
                        animationSets = setMiddleAnimation(animationSets, enterSet, exitSet);
                        break;
                    case 102:
                        animationSets = setOpenAppAnimation(animationSets, enterSet, exitSet);
                        break;
                    case 103:
                        animationSets = setExitRightToRightAnimation(animationSets, enterSet, exitSet);
                        break;
                    default:
                        return;
                }
            } else {
                animationSets = setAnAnRightToLeftAnimation(animationSets, enterSet, exitSet);
            }
        }
        AnimationSet enterSet2 = animationSets[0];
        AnimationSet exitSet2 = animationSets[1];
        if ((isStart ? enterSet2 : exitSet2) != null) {
            (isStart ? enterSet2 : exitSet2).setAnimationListener(new MoveAnimationListener());
        }
        this.mWms.getWindowManagerServiceEx().setMagicWindowAnimation(isStart, enterSet2, exitSet2);
    }

    private AnimationSet[] setOpenAppAnimation(AnimationSet[] animationSets, AnimationSet enterSet, AnimationSet exitSet) {
        animationSets[0] = null;
        animationSets[1] = getLauncherExitAnim();
        return animationSets;
    }

    public void setOpenAppAnimation() {
        this.mWms.getWindowManagerServiceEx().setMagicWindowAnimation(true, (Animation) null, getLauncherExitAnim());
    }

    private AnimationSet getLauncherExitAnim() {
        AnimationSet as = new AnimationSet(true);
        ScaleAnimation scale = new ScaleAnimation(1.0f, 0.9f, 1.0f, 0.9f, 1, 0.5f, 1, 0.5f);
        as.addAnimation(new AlphaAnimation(1.0f, 0.0f));
        as.addAnimation(scale);
        as.setDuration((long) this.mDurationAlphaEnter);
        as.setInterpolator(this.mInterpolator);
        return as;
    }

    private AnimationSet[] setExitRightToRightAnimation(AnimationSet[] animationSets, AnimationSet enterSet, AnimationSet exitSet) {
        animationSets[0] = (AnimationSet) AnimationUtils.loadAnimation(this.mContext, 34209803);
        animationSets[1] = (AnimationSet) AnimationUtils.loadAnimation(this.mContext, 34209805);
        return animationSets;
    }

    private AnimationSet[] setLeftToRightAnimation(AnimationSet[] animationSets, AnimationSet enterSet, AnimationSet exitSet) {
        animationSets[0] = (AnimationSet) AnimationUtils.loadAnimation(this.mContext, 34209800);
        animationSets[1] = (AnimationSet) AnimationUtils.loadAnimation(this.mContext, 34209808);
        return animationSets;
    }

    private AnimationSet[] setMiddleToRightAnimation(AnimationSet[] animationSets, AnimationSet enterSet, AnimationSet exitSet) {
        enterSet.addAnimation(getScaleAnimation(0.75f, 0.85f, 1.0f));
        enterSet.addAnimation(getAlphaAnimationEnter());
        exitSet.addAnimation(getAlphaAnimationExit());
        exitSet.addAnimation(getScaleAnimation(0.75f, 1.0f, 0.85f));
        animationSets[0] = enterSet;
        animationSets[1] = exitSet;
        enterSet.setInterpolator(this.mInterpolator);
        exitSet.setInterpolator(this.mInterpolator);
        return animationSets;
    }

    private AnimationSet[] setRightToRightAnimation(AnimationSet[] animationSets, AnimationSet enterSet, AnimationSet exitSet) {
        animationSets[0] = (AnimationSet) AnimationUtils.loadAnimation(this.mContext, 34209800);
        animationSets[1] = (AnimationSet) AnimationUtils.loadAnimation(this.mContext, 34209808);
        return animationSets;
    }

    private AnimationSet[] setExitAnimation(AnimationSet[] animationSets, AnimationSet enterSet, AnimationSet exitSet) {
        exitSet.addAnimation(getAlphaAnimationExit());
        exitSet.addAnimation(getScaleAnimation(0.75f, 1.0f, 0.85f));
        exitSet.setInterpolator(this.mInterpolator);
        animationSets[0] = enterSet;
        animationSets[1] = exitSet;
        return animationSets;
    }

    private AnimationSet[] setAnAnRightToLeftAnimation(AnimationSet[] animationSets, AnimationSet enterSet, AnimationSet exitSet) {
        enterSet.addAnimation(getAlphaAnimationEnter());
        ScaleAnimation scaleEnter = new ScaleAnimation(0.85f, 1.0f, 0.85f, 1.0f, 2, 0.75f, 2, 0.5f);
        scaleEnter.setDuration((long) this.mDurationTranslationEnter);
        enterSet.addAnimation(scaleEnter);
        enterSet.setInterpolator(this.mInterpolator);
        animationSets[0] = enterSet;
        animationSets[1] = null;
        return animationSets;
    }

    private AnimationSet[] setMiddleAnimation(AnimationSet[] animationSets, AnimationSet enterSet, AnimationSet exitSet) {
        enterSet.addAnimation(getAlphaAnimationEnter());
        enterSet.addAnimation(getScaleAnimation(0.5f, 0.85f, 1.0f));
        animationSets[0] = enterSet;
        exitSet.addAnimation(getAlphaAnimationExit());
        exitSet.addAnimation(getScaleAnimation(0.5f, 1.0f, 0.85f));
        animationSets[1] = exitSet;
        return animationSets;
    }

    private AlphaAnimation getAlphaAnimationEnter() {
        AlphaAnimation alphaEnter = new AlphaAnimation(0.0f, 1.0f);
        alphaEnter.setDuration((long) this.mDurationAlphaEnter);
        alphaEnter.setStartOffset(0);
        return alphaEnter;
    }

    private AlphaAnimation getAlphaAnimationExit() {
        AlphaAnimation alphaExit = new AlphaAnimation(1.0f, 0.0f);
        alphaExit.setDuration((long) this.mDurationRightExit);
        return alphaExit;
    }

    private ScaleAnimation getScaleAnimation(float centerX, float from, float to) {
        ScaleAnimation scaleEnter = new ScaleAnimation(from, to, from, to, 2, centerX, 2, 0.5f);
        scaleEnter.setDuration((long) this.mDurationTranslationEnter);
        return scaleEnter;
    }

    public static float[] computePivotForAppExit(Rect vRect, int iconWidth, int iconHeight, float[] centerCoordinate) {
        float iconFrameLeft = centerCoordinate[0] - (((float) iconWidth) / 2.0f);
        float iconFrameRight = centerCoordinate[0] + (((float) iconWidth) / 2.0f);
        float iconFrameTop = centerCoordinate[1] - (((float) iconHeight) / 2.0f);
        float iconFrameBottom = centerCoordinate[1] + (((float) iconHeight) / 2.0f);
        return new float[]{(((((float) vRect.right) * iconFrameLeft) - (((float) vRect.left) * iconFrameRight)) / (((((float) vRect.right) - iconFrameRight) + iconFrameLeft) - ((float) vRect.left))) + 0.5f, (((((float) vRect.bottom) * iconFrameTop) - (((float) vRect.top) * iconFrameBottom)) / (((((float) vRect.bottom) - iconFrameBottom) + iconFrameTop) - ((float) vRect.top))) + 0.5f};
    }

    public static float[] computeScaleToForAppExit(Rect vRect, int iconWidth, int iconHeight) {
        return new float[]{((float) iconWidth) / ((float) (vRect.right - vRect.left)), ((float) iconHeight) / ((float) (vRect.bottom - vRect.top))};
    }

    public void setParamsForRotation(Rect focusBounds, int focusMode) {
        this.mFocusBounds = focusBounds;
        this.mFocusMode = focusMode;
        this.mFocus = getFocusForRotation();
    }

    public void resetParamsForRotation() {
        this.mFocusBounds = null;
        this.mFocus = ROTATION_FOCUS_INVALID;
    }

    public boolean getRotationAnim(Integer[] params, Animation[] results) {
        if (this.mFocus == ROTATION_FOCUS_INVALID || this.mFocusBounds == null || 103 != this.mFocusMode) {
            Slog.e(TAG, "get rotation animation focus not right!");
            return false;
        }
        int oldR = params[0].intValue();
        int delta = params[1].intValue();
        int oldH = params[2].intValue();
        int newH = params[3].intValue();
        if (oldR == 0 || oldR == 2) {
            results[0] = getExitAnimaFormVert(delta, oldH, newH);
            results[1] = getEnterAnimaFormVert(delta, oldH, newH);
            return true;
        } else if (oldR == 1 || oldR == 3) {
            results[0] = getExitAnimaFormHoriz(delta, oldH);
            results[1] = getEnterAnimaFormHoriz(delta, oldH);
            return true;
        } else {
            Slog.e(TAG, "getRotationAnimation failed ! oldR wrong !");
            return false;
        }
    }

    private int getFocusForRotation() {
        Rect rect = this.mFocusBounds;
        if (rect != null) {
            this.mTopActivityWidth = rect.width();
        }
        if (this.mService.checkPosition(this.mFocusBounds, 2)) {
            return this.mService.getConfig().isRtl() ? -1 : 1;
        }
        if (this.mService.checkPosition(this.mFocusBounds, 1)) {
            return this.mService.getConfig().isRtl() ? -1 : 1;
        }
        if (this.mService.checkPosition(this.mFocusBounds, 3)) {
            return 0;
        }
        return ROTATION_FOCUS_INVALID;
    }

    private Animation getEnterAnimaFormHoriz(int delta, int oldH) {
        float scaleToVertical = ((float) oldH) / ((float) this.mTopActivityWidth);
        int direction = delta - 2;
        float displacement = 0.0f;
        int i = this.mFocus;
        if (i != 0) {
            displacement = i * direction > 0 ? (scaleToVertical - 1.0f) / 2.0f : (scaleToVertical / 2.0f) - 1.0f;
        }
        AnimationSet enterAnimation = new AnimationSet(true);
        enterAnimation.setInterpolator(this.mInterpolator);
        setDurationAndAddAnimation(enterAnimation, new ScaleAnimation(1.0f / (scaleToVertical * 2.0f), 1.0f, 1.0f / (2.0f * scaleToVertical), 1.0f, 1, 0.5f, 1, 0.5f));
        setDurationAndAddAnimation(enterAnimation, new RotateAnimation(90.0f * ((float) (-direction)), 0.0f, 1, 0.5f, 1, 0.5f));
        setDurationAndAddAnimation(enterAnimation, new TranslateAnimation(1, 0.0f, 1, 0.0f, 1, -displacement, 1, 0.0f));
        setDurationAndAddAnimation(enterAnimation, new AlphaAnimation(0.0f, 1.0f));
        return enterAnimation;
    }

    private Animation getExitAnimaFormHoriz(int delta, int oldH) {
        float displacement;
        float axisY;
        float scaleToVertical = ((float) oldH) / ((float) this.mTopActivityWidth);
        int direction = delta - 2;
        float axisY2 = LEFT_AXIS_X;
        float displacement2 = this.mFocus * direction > 0 ? (scaleToVertical - 1.0f) / 2.0f : (scaleToVertical / 2.0f) - 1.0f;
        if (this.mFocus * direction < 0) {
            axisY2 = 0.75f;
        }
        if (this.mFocus == 0) {
            axisY = 0.5f;
            displacement = 0.0f;
        } else {
            axisY = axisY2;
            displacement = displacement2;
        }
        AnimationSet exitAnimation = new AnimationSet(true);
        exitAnimation.setInterpolator(this.mInterpolator);
        setDurationAndAddAnimation(exitAnimation, new ScaleAnimation(1.0f, scaleToVertical, 1.0f, scaleToVertical, 1, 0.5f, 1, axisY));
        setDurationAndAddAnimation(exitAnimation, new RotateAnimation(0.0f, 90.0f * ((float) direction), 1, 0.5f, 1, axisY));
        setDurationAndAddAnimation(exitAnimation, new TranslateAnimation(1, 0.0f, 1, 0.0f, 1, 0.0f, 1, displacement));
        setDurationAndAddAnimation(exitAnimation, new AlphaAnimation(1.0f, 0.0f));
        return exitAnimation;
    }

    private Animation getEnterAnimaFormVert(int delta, int oldH, int newH) {
        float axisX;
        AnimationSet enterAnimation = new AnimationSet(true);
        enterAnimation.setInterpolator(this.mInterpolator);
        float scaleToHorizontal = ((float) oldH) / ((float) newH);
        int direction = delta - 2;
        float axisX2 = LEFT_AXIS_X;
        if (this.mFocus * direction < 0) {
            axisX2 = 0.75f;
        }
        if (this.mFocus == 0) {
            axisX = 0.5f;
        } else {
            axisX = axisX2;
        }
        float displacement = ((float) (this.mFocus * this.mTopActivityWidth)) / 2.0f;
        setDurationAndAddAnimation(enterAnimation, new ScaleAnimation(scaleToHorizontal, 1.0f, scaleToHorizontal, 1.0f, 1, 0.5f, 1, 0.5f));
        setDurationAndAddAnimation(enterAnimation, new RotateAnimation(90.0f * ((float) (-direction)), 0.0f, 1, axisX, 1, 0.5f));
        setDurationAndAddAnimation(enterAnimation, new TranslateAnimation(0, -displacement, 0, 0.0f, 0, 0.0f, 0, 0.0f));
        setDurationAndAddAnimation(enterAnimation, new AlphaAnimation(0.0f, 1.0f));
        return enterAnimation;
    }

    private Animation getExitAnimaFormVert(int delta, int oldH, int newH) {
        float scaleToHorizontal = ((float) newH) / ((float) oldH);
        float displacement = ((float) (this.mFocus * this.mTopActivityWidth)) / 2.0f;
        AnimationSet exitAnimation = new AnimationSet(true);
        exitAnimation.setInterpolator(this.mInterpolator);
        setDurationAndAddAnimation(exitAnimation, new ScaleAnimation(1.0f, scaleToHorizontal, 1.0f, scaleToHorizontal, 1, 0.5f, 1, 0.5f));
        setDurationAndAddAnimation(exitAnimation, new RotateAnimation(0.0f, ((float) (delta - 2)) * 90.0f, 1, 0.5f, 1, 0.5f));
        setDurationAndAddAnimation(exitAnimation, new TranslateAnimation(0, 0.0f, 0, displacement, 0, 0.0f, 0, 0.0f));
        setDurationAndAddAnimation(exitAnimation, new AlphaAnimation(1.0f, 0.0f));
        return exitAnimation;
    }

    private void setDurationAndAddAnimation(AnimationSet animationSet, Animation animation) {
        this.mDurationRotation = (int) (350.0f / getTransitionAnimationScale());
        animation.setDuration((long) this.mDurationRotation);
        animationSet.addAnimation(animation);
    }

    private float getTransitionAnimationScale() {
        return Math.abs(this.mWms.getTransitionAnimationScaleLocked() - 0.5f) < DEFAULT_MIN ? 0.5f : 1.0f;
    }

    private Animation getAlpahAnimation(float fromAlpha, float toAlpha, Interpolator interpolator) {
        Animation alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
        alphaAnimation.setInterpolator(interpolator);
        return alphaAnimation;
    }

    private Animation getTranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta) {
        return new TranslateAnimation(fromXDelta, toXDelta, fromYDelta, toYDelta);
    }

    private Animation getScaleAnimation(int fromWidth, float toWidth, int fromHeight, float toHeight) {
        float startXScale = (((float) fromWidth) + 0.5f) / toWidth;
        float endXScale = 1.0f;
        float startYScale = (((float) fromHeight) + 0.5f) / toHeight;
        float endYScale = 1.0f;
        if (((float) fromWidth) < toWidth) {
            startXScale = 1.0f;
            endXScale = (toWidth + 0.5f) / ((float) fromWidth);
        }
        if (((float) fromHeight) < toHeight) {
            startYScale = 1.0f;
            endYScale = (0.5f + toHeight) / ((float) fromHeight);
        }
        return new ScaleAnimation(startXScale, endXScale, startYScale, endYScale);
    }

    public AnimationParams getSplitAnimation(Rect startBounds, Rect endBounds, DisplayInfo displayInfo) {
        AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(getAlpahAnimation(1.0f, 0.0f, this.mAlphaInterpolator));
        AnimationSet combineSet = new AnimationSet(true);
        combineSet.addAnimation(getScaleAnimation(startBounds.width(), (float) endBounds.width(), startBounds.height(), (float) endBounds.height()));
        combineSet.addAnimation(getTranslateAnimation((float) startBounds.left, (float) endBounds.left, (float) startBounds.top, (float) endBounds.top));
        combineSet.setInterpolator(this.mSplitAnimInterpolator);
        animationSet.addAnimation(combineSet);
        animationSet.setDuration(1000);
        animationSet.setFillAfter(true);
        animationSet.initialize(startBounds.width(), startBounds.height(), displayInfo.logicalWidth, displayInfo.logicalHeight);
        return new AnimationParams(animationSet, 0.55f);
    }

    public AnimationParams getExitTaskAnimation(Rect startBounds, DisplayInfo displayInfo) {
        AnimationSet animationSet = new AnimationSet(true);
        Animation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        Animation scaleAnimation = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f);
        animationSet.addAnimation(alphaAnimation);
        animationSet.addAnimation(scaleAnimation);
        animationSet.setInterpolator(this.mInterpolator);
        animationSet.setDuration(550);
        animationSet.setFillAfter(true);
        animationSet.initialize(startBounds.width(), startBounds.height(), displayInfo.logicalWidth, displayInfo.logicalHeight);
        return new AnimationParams(animationSet, 0.0f);
    }

    private class MoveAnimationListener implements Animation.AnimationListener {
        private MoveAnimationListener() {
        }

        public void onAnimationStart(Animation animation) {
            boolean z = true;
            boolean unused = HwMagicWinAnimation.this.isAnimationRunning = true;
            boolean emptySlave = HwMagicWinAnimation.this.mService.getAmsPolicy().getActvityByPosition(2) == null;
            boolean emptyMaster = HwMagicWinAnimation.this.mAnimationScene.mIsMoveToMiddleOrRight || HwMagicWinAnimation.this.mService.getAmsPolicy().getActvityByPosition(1) == null;
            HwMagicWindowService access$800 = HwMagicWinAnimation.this.mService;
            if (!emptySlave || !emptyMaster) {
                z = false;
            }
            access$800.changeWallpaper(z);
        }

        public void onAnimationEnd(Animation animation) {
            boolean unused = HwMagicWinAnimation.this.isAnimationRunning = false;
        }

        public void onAnimationRepeat(Animation animation) {
        }
    }

    private class HwMagicWinMoveInterpolator implements Interpolator {
        private static final int DURATION_MOVE_WINDOW_REAL = 250;
        private HwMagicWinAnimation mRightAnimation = null;
        private boolean running = false;
        private long startTime;
        private long waitTime;

        public HwMagicWinMoveInterpolator(HwMagicWinAnimation rightAnimation) {
            this.mRightAnimation = rightAnimation;
        }

        public float getInterpolation(float t) {
            if (Math.abs(t - 0.0f) < 1.0E-6f) {
                this.startTime = System.currentTimeMillis();
                this.running = false;
            }
            long timeNow = System.currentTimeMillis();
            if (this.mRightAnimation.isAnimationRunning && !this.running) {
                this.running = true;
                this.waitTime = timeNow - this.startTime;
            }
            if (!this.running && ((float) (timeNow - this.startTime)) > 500.0f) {
                this.running = true;
                this.waitTime = 500;
            }
            boolean z = this.running;
            if (!z) {
                return 0.0f;
            }
            if (!z || t >= ((float) (this.waitTime + 250)) / 750.0f) {
                return 1.0f;
            }
            return HwMagicWinAnimation.this.mInterpolator.getInterpolation(((750.0f * t) - ((float) this.waitTime)) / 250.0f);
        }
    }

    public static Animation getMwWallpaperCloseAnimation() {
        AlphaAnimation alphaExit = new AlphaAnimation(1.0f, 0.0f);
        alphaExit.setDuration(200);
        alphaExit.setInterpolator(new Interpolator() {
            /* class com.android.server.magicwin.HwMagicWinAnimation.AnonymousClass3 */

            public float getInterpolation(float input) {
                if (input < 0.5f) {
                    return input / 0.5f;
                }
                return 1.0f;
            }
        });
        return alphaExit;
    }

    public static class AnimationParams {
        private final Animation mAnimation;
        private final float mHideThreshold;

        public AnimationParams(Animation animation, float hideThreshold) {
            this.mAnimation = animation;
            this.mHideThreshold = hideThreshold;
        }

        public Animation getAnimation() {
            return this.mAnimation;
        }

        public float getHideThreshold() {
            return this.mHideThreshold;
        }
    }

    /* access modifiers changed from: private */
    public class AnimationScene {
        /* access modifiers changed from: private */
        public Rect mFocusBound = null;
        /* access modifiers changed from: private */
        public boolean mIsMiddle = false;
        /* access modifiers changed from: private */
        public boolean mIsMoveToMiddleOrRight = true;
        /* access modifiers changed from: private */
        public int mLastStartScene = -1;
        private HwMagicWindowService mMWService;

        AnimationScene(HwMagicWindowService service) {
            this.mMWService = service;
        }

        /* access modifiers changed from: private */
        public int getSceneForStart(boolean isRightToLeft, boolean allDrawn, boolean mIsTransitionActivity, Rect next) {
            int i;
            if (mIsTransitionActivity) {
                Slog.e(HwMagicWinAnimation.TAG, "mIsTransitionActivity ");
                if (allDrawn || ((i = this.mLastStartScene) != 0 && i != 200)) {
                    return -1;
                }
                return this.mLastStartScene;
            }
            int boundsPosition = this.mMWService.getBoundsPosition(next);
            if (boundsPosition == 2) {
                int boundsPosition2 = this.mMWService.getBoundsPosition(this.mFocusBound);
                if (boundsPosition2 != 1) {
                    if (boundsPosition2 != 2) {
                        return -1;
                    }
                    if (isRightToLeft) {
                        return 200;
                    }
                    return 1;
                } else if (isRightToLeft) {
                    return 200;
                } else {
                    if (!this.mIsMiddle) {
                        return 0;
                    }
                    this.mIsMiddle = false;
                    return 2;
                }
            } else if (boundsPosition != 3) {
                return -1;
            } else {
                Rect rect = this.mFocusBound;
                if (rect == null || rect.isEmpty()) {
                    return 102;
                }
                if (3 == this.mMWService.getBoundsPosition(this.mFocusBound)) {
                    return 101;
                }
                return -1;
            }
        }

        /* access modifiers changed from: private */
        public int getSceneForFinish(boolean isRightEmpty, Rect finishBound, boolean isMoveToMiddleOrRight) {
            this.mIsMoveToMiddleOrRight = isMoveToMiddleOrRight;
            if (2 != this.mMWService.getBoundsPosition(finishBound) || isMoveToMiddleOrRight || isRightEmpty) {
                return 100;
            }
            return 103;
        }
    }
}
