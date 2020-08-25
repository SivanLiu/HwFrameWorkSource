package com.android.server.gesture.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.server.gesture.GestureNavConst;
import com.android.server.gesture.anim.GLGestureBackView;
import com.android.server.gesture.anim.models.GestureBackMetaball;
import com.android.server.gesture.anim.models.GestureBackTexture;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLGestureBackRender implements GLSurfaceView.Renderer {
    private static final String ANIMATION_PROCESS = "animProcess";
    private static final long DOCK_IN_ANIM_DURATION = 150;
    private static final long DOCK_OUT_ANIM_DURATION = 150;
    private static final int FAST_SLIDING_DURATION = 150;
    public static final float FAST_SLIDING_MAX_PROCESS = 0.55f;
    private static final int FAST_SLIDING_STAY_DURATION = 180;
    private static final float MAX_SCALE_VALUE = 1.0f;
    private static final float MIN_SCALE_VALUE = 0.9f;
    private static final long SCATTER_PROCESS_DURATION = 150;
    private static final int SLIDING_STAY_DURATION = 30;
    private static final int SLOW_SLIDING_DISAPPEAR_DURATION = 300;
    private static final String TAG = "GLGestureBackRender";
    /* access modifiers changed from: private */
    public AnimatorSet dockAnimatorSet;
    private TimeInterpolator dockInInterpolator;
    private TimeInterpolator dockOutInterpolator;
    private AnimatorListenerAdapter mAnimListener = new AnimatorListenerAdapter() {
        /* class com.android.server.gesture.anim.GLGestureBackRender.AnonymousClass1 */

        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if (GLGestureBackRender.this.dockAnimatorSet != null && GLGestureBackRender.this.dockAnimatorSet.isRunning()) {
                GLGestureBackRender.this.dockAnimatorSet.end();
            }
            if (GLGestureBackRender.this.mAnimationListener == null) {
                return;
            }
            if (animation == GLGestureBackRender.this.mFastSlidingAnim) {
                GLLogUtils.logD(GLGestureBackRender.TAG, "fast sliding animation end.");
                GLGestureBackRender.this.mAnimationListener.onAnimationEnd(1);
                return;
            }
            GLLogUtils.logD(GLGestureBackRender.TAG, "slow sliding disappear animation end.");
            GLGestureBackRender.this.mAnimationListener.onAnimationEnd(2);
        }
    };
    /* access modifiers changed from: private */
    public GLGestureBackView.GestureBackAnimListener mAnimationListener;
    private Context mContext;
    /* access modifiers changed from: private */
    public AnimatorSet mFastSlidingAnim;
    private GestureBackMetaball mGestureBackMetaBall;
    private GestureBackTexture mGestureBackTexture;
    private GestureBackTexture mGestureBackTextureDock;
    private boolean mIsDockMode = false;
    private boolean mIsShowDockIcon = false;
    private ObjectAnimator mScatterProcessAnimator;
    private GLGestureBackView.ViewSizeChangeListener mViewSizeChangeListener;

    GLGestureBackRender(Context context) {
        GLLogUtils.logD(TAG, "GLGestureBackRender start");
        this.mContext = context;
        this.mGestureBackTexture = new GestureBackTexture(context, 33751903);
        this.mGestureBackMetaBall = new GestureBackMetaball();
        if (GestureNavConst.SUPPORT_DOCK_TRIGGER) {
            this.mIsDockMode = true;
            this.mGestureBackTextureDock = new GestureBackTexture(context, 33751957);
        }
        initAnimators();
    }

    private void initAnimators() {
        ObjectAnimator proAppear = ObjectAnimator.ofFloat(this, ANIMATION_PROCESS, 0.0f, 0.55f);
        proAppear.setDuration(150L);
        Interpolator interpolator = AnimationUtils.loadInterpolator(this.mContext, 17563661);
        proAppear.setInterpolator(interpolator);
        ObjectAnimator proDisappear = ObjectAnimator.ofFloat(this, ANIMATION_PROCESS, 0.55f, 0.0f);
        proDisappear.setDuration(150L);
        proDisappear.setInterpolator(interpolator);
        this.mFastSlidingAnim = new AnimatorSet();
        this.mFastSlidingAnim.play(proDisappear).after(proAppear);
        this.mFastSlidingAnim.play(proDisappear).after(180);
        this.mFastSlidingAnim.addListener(this.mAnimListener);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        this.mGestureBackMetaBall.prepare();
        this.mGestureBackTexture.prepare();
        if (this.mIsDockMode) {
            this.mGestureBackTextureDock.prepare();
        }
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLLogUtils.logD(TAG, "onSurfaceChanged width " + width + ", height " + height);
        GLGestureBackView.ViewSizeChangeListener viewSizeChangeListener = this.mViewSizeChangeListener;
        if (viewSizeChangeListener != null) {
            viewSizeChangeListener.onViewSizeChanged(width, height);
        }
        this.mGestureBackMetaBall.onSurfaceViewChanged(width, height);
        this.mGestureBackTexture.onSurfaceViewChanged(width, height);
        if (this.mIsDockMode) {
            this.mGestureBackTextureDock.onSurfaceViewChanged(width, height);
        }
    }

    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(16640);
        GLES30.glEnable(3042);
        GLES30.glBlendFunc(770, 771);
        this.mGestureBackMetaBall.drawSelf();
        this.mGestureBackTexture.drawSelf();
        if (this.mIsDockMode) {
            this.mGestureBackTextureDock.drawSelf();
        }
        GLES30.glDisable(3042);
    }

    public void setAnimProcess(float process) {
        GLLogUtils.logD(TAG, "setAnimProcess with " + process);
        this.mGestureBackMetaBall.setProcess(process);
        this.mGestureBackTexture.setProcess(process);
        if (this.mIsDockMode) {
            this.mGestureBackTextureDock.setProcess(process);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isScatterProcessAnimRunning() {
        ObjectAnimator objectAnimator = this.mScatterProcessAnimator;
        return objectAnimator != null && objectAnimator.isRunning();
    }

    @SuppressLint({"NewApi"})
    public void playScatterProcessAnim(float fromProcess, float toProcess) {
        ObjectAnimator objectAnimator = this.mScatterProcessAnimator;
        if (objectAnimator == null || !objectAnimator.isRunning()) {
            this.mScatterProcessAnimator = ObjectAnimator.ofFloat(this, ANIMATION_PROCESS, fromProcess, toProcess);
            this.mScatterProcessAnimator.setDuration((long) ((toProcess - fromProcess) * 150.0f));
            this.mScatterProcessAnimator.setInterpolator(new PathInterpolator(0.0f, 0.5f, 0.2f, 1.0f));
            GLLogUtils.logD(TAG, "start scatter process: from = " + fromProcess + ", to = " + toProcess);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(this.mScatterProcessAnimator);
            animatorSet.start();
            return;
        }
        GLLogUtils.logD(TAG, "scatter process animator isRunning = " + this.mScatterProcessAnimator.isRunning());
    }

    public void setSide(boolean isLeft) {
        GLLogUtils.logD(TAG, "setSide with " + isLeft);
        this.mGestureBackMetaBall.setSide(isLeft);
        this.mGestureBackTexture.setSide(isLeft);
        if (this.mIsDockMode) {
            this.mGestureBackTextureDock.setSide(isLeft);
        }
    }

    public void setNightMode(boolean isNightMode) {
        GLLogUtils.logD(TAG, "setNightMode with " + isNightMode);
        this.mGestureBackMetaBall.setNightMode(isNightMode);
    }

    public void setAnimPosition(float positionY) {
        GLLogUtils.logD(TAG, "setAnimPosition with " + positionY);
        this.mGestureBackMetaBall.setCenter(positionY);
        this.mGestureBackTexture.setCenter((int) positionY);
        if (this.mIsDockMode) {
            this.mGestureBackTextureDock.setCenter((int) positionY);
        }
    }

    public void setDraw(boolean isDraw) {
        GLLogUtils.logD(TAG, "setDraw with " + isDraw);
        boolean z = true;
        this.mGestureBackTexture.setDraw(isDraw && !this.mIsShowDockIcon);
        if (this.mIsDockMode) {
            GestureBackTexture gestureBackTexture = this.mGestureBackTextureDock;
            if (!isDraw || !this.mIsShowDockIcon) {
                z = false;
            }
            gestureBackTexture.setDraw(z);
        }
        this.mGestureBackMetaBall.setDraw(isDraw);
    }

    public void setDockIcon(boolean isShowDockIcon) {
        this.mIsShowDockIcon = isShowDockIcon;
    }

    public void addAnimationListener(GLGestureBackView.GestureBackAnimListener listener) {
        this.mAnimationListener = listener;
    }

    public void addViewSizeChangeListener(GLGestureBackView.ViewSizeChangeListener listener) {
        this.mViewSizeChangeListener = listener;
    }

    public void playFastSlidingAnim() {
        GLLogUtils.logD(TAG, "playFastSlidingAnim");
        this.mFastSlidingAnim.start();
    }

    public void playDisappearAnim() {
        GLLogUtils.logD(TAG, "playDisappearAnim");
        float animProcess = this.mGestureBackMetaBall.getProcess();
        ObjectAnimator disappearAnim = ObjectAnimator.ofFloat(this, ANIMATION_PROCESS, animProcess, 0.0f);
        disappearAnim.setDuration((long) (300.0f * animProcess));
        disappearAnim.setInterpolator(AnimationUtils.loadInterpolator(this.mContext, 17563661));
        disappearAnim.addListener(this.mAnimListener);
        AnimatorSet disappearAnimSet = new AnimatorSet();
        disappearAnimSet.play(disappearAnim).after(30);
        disappearAnimSet.start();
    }

    public void switchDockIcon(boolean isSlideIn) {
        GLLogUtils.logD(TAG, "start switchDockIcon method.isSlideIn:" + isSlideIn);
        AnimatorSet animatorSet = this.dockAnimatorSet;
        if (animatorSet != null && animatorSet.isRunning()) {
            this.dockAnimatorSet.end();
        }
        this.dockAnimatorSet = new AnimatorSet();
        if (this.mGestureBackTexture != null && this.mGestureBackTextureDock != null) {
            ValueAnimator showOutAlphaAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
            ValueAnimator showInAlphaAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            ValueAnimator showInScaleAnimator = ValueAnimator.ofFloat(MIN_SCALE_VALUE, 1.0f);
            if (isSlideIn) {
                this.mGestureBackTextureDock.setDockAlpha(0.0f);
                this.mGestureBackTextureDock.setScaleRate(MIN_SCALE_VALUE);
                this.mGestureBackTextureDock.setDraw(true);
                showOutAlphaAnimator.addUpdateListener(getAlphaUpdateListener(this.mGestureBackTexture));
                showInAlphaAnimator.addUpdateListener(getAlphaUpdateListener(this.mGestureBackTextureDock));
                showInScaleAnimator.addUpdateListener(getScaleUpdateListener(this.mGestureBackTextureDock));
                this.dockAnimatorSet.playTogether(showInAlphaAnimator, showInScaleAnimator, showOutAlphaAnimator);
                this.dockInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 34078724);
                this.dockAnimatorSet.setInterpolator(this.dockInInterpolator);
                this.dockAnimatorSet.setDuration(150L);
            } else {
                this.mGestureBackTexture.setDockAlpha(0.0f);
                this.mGestureBackTexture.setScaleRate(1.0f);
                this.mGestureBackTexture.setDraw(true);
                showOutAlphaAnimator.addUpdateListener(getAlphaUpdateListener(this.mGestureBackTextureDock));
                showInAlphaAnimator.addUpdateListener(getAlphaUpdateListener(this.mGestureBackTexture));
                this.dockAnimatorSet.playTogether(showInAlphaAnimator, showOutAlphaAnimator);
                this.dockOutInterpolator = AnimationUtils.loadInterpolator(this.mContext, 34078724);
                this.dockAnimatorSet.setInterpolator(this.dockOutInterpolator);
                this.dockAnimatorSet.setDuration(150L);
            }
            this.dockAnimatorSet.start();
        }
    }

    private ValueAnimator.AnimatorUpdateListener getAlphaUpdateListener(final GestureBackTexture gbTexture) {
        return new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.server.gesture.anim.GLGestureBackRender.AnonymousClass2 */

            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation.getAnimatedValue() instanceof Float) {
                    gbTexture.setDockAlpha(((Float) animation.getAnimatedValue()).floatValue());
                }
            }
        };
    }

    private ValueAnimator.AnimatorUpdateListener getScaleUpdateListener(final GestureBackTexture gbTexture) {
        return new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.server.gesture.anim.GLGestureBackRender.AnonymousClass3 */

            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation.getAnimatedValue() instanceof Float) {
                    gbTexture.setScaleRate(((Float) animation.getAnimatedValue()).floatValue());
                }
            }
        };
    }

    public void endAnimation() {
        GLLogUtils.logD(TAG, "start endAnimation method." + this.mIsDockMode);
        this.mGestureBackTexture.setDockAlpha(1.0f);
        this.mGestureBackTexture.setScaleRate(1.0f);
        if (this.mIsDockMode) {
            this.mGestureBackTextureDock.setDockAlpha(1.0f);
            this.mGestureBackTextureDock.setScaleRate(1.0f);
        }
    }
}
