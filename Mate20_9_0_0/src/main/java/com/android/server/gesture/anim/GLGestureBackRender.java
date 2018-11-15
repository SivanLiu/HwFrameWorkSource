package com.android.server.gesture.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView.Renderer;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.server.gesture.GestureNavConst;
import com.android.server.gesture.anim.GLGestureBackView.GestureBackAnimListener;
import com.android.server.gesture.anim.GLGestureBackView.ViewSizeChangeListener;
import com.android.server.gesture.anim.models.GestureBackMetaball;
import com.android.server.gesture.anim.models.GestureBackTexture;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLGestureBackRender implements Renderer {
    private static final int FAST_SLIDING_DURATION = 150;
    public static final float FAST_SLIDING_MAX_PROCESS = 0.55f;
    private static final int FAST_SLIDING_STAY_DURATION = 180;
    private static final int SLIDING_STAY_DURATION = 30;
    private static final int SLOW_SLIDING_DISAPPEAR_DURATION = 300;
    private static final String TAG = "GLGestureBackRender";
    private AnimatorListenerAdapter mAnimListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
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
    private GestureBackAnimListener mAnimationListener;
    private Context mContext;
    private AnimatorSet mFastSlidingAnim;
    private GestureBackMetaball mGestureBackMetaBall;
    private GestureBackTexture mGestureBackTexture;
    private ViewSizeChangeListener mViewSizeChangeListener;

    GLGestureBackRender(Context context) {
        this.mContext = context;
        this.mGestureBackTexture = new GestureBackTexture(context);
        this.mGestureBackMetaBall = new GestureBackMetaball();
        initAnimators();
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0021, element type: float, insn element type: null */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void initAnimators() {
        ObjectAnimator proAppear = ObjectAnimator.ofFloat(this, "animProcess", new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 0.55f});
        proAppear.setDuration(150);
        Interpolator interpolator = AnimationUtils.loadInterpolator(this.mContext, 17563661);
        proAppear.setInterpolator(interpolator);
        ObjectAnimator proDisappear = ObjectAnimator.ofFloat(this, "animProcess", new float[]{0.55f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        proDisappear.setDuration(150);
        proDisappear.setInterpolator(interpolator);
        this.mFastSlidingAnim = new AnimatorSet();
        this.mFastSlidingAnim.play(proDisappear).after(proAppear);
        this.mFastSlidingAnim.play(proDisappear).after(180);
        this.mFastSlidingAnim.addListener(this.mAnimListener);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
        this.mGestureBackMetaBall.prepare();
        this.mGestureBackTexture.prepare();
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSurfaceChanged width ");
        stringBuilder.append(width);
        stringBuilder.append(", height ");
        stringBuilder.append(height);
        GLLogUtils.logD(str, stringBuilder.toString());
        if (this.mViewSizeChangeListener != null) {
            this.mViewSizeChangeListener.onViewSizeChanged(width, height);
        }
        this.mGestureBackMetaBall.onSurfaceViewChanged(width, height);
        this.mGestureBackTexture.onSurfaceViewChanged(width, height);
    }

    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(16640);
        GLES30.glEnable(3042);
        GLES30.glBlendFunc(770, 771);
        this.mGestureBackMetaBall.drawSelf();
        this.mGestureBackTexture.drawSelf();
        GLES30.glDisable(3042);
    }

    public void setAnimProcess(float process) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAnimProcess with ");
        stringBuilder.append(process);
        GLLogUtils.logD(str, stringBuilder.toString());
        this.mGestureBackMetaBall.setProcess(process);
        this.mGestureBackTexture.setProcess(process);
    }

    public void setSide(boolean isLeft) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setSide with ");
        stringBuilder.append(isLeft);
        GLLogUtils.logD(str, stringBuilder.toString());
        this.mGestureBackMetaBall.setSide(isLeft);
        this.mGestureBackTexture.setSide(isLeft);
    }

    public void setAnimPosition(float y) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAnimPosition with ");
        stringBuilder.append(y);
        GLLogUtils.logD(str, stringBuilder.toString());
        this.mGestureBackMetaBall.setCenter(y);
        this.mGestureBackTexture.setCenter((int) y);
    }

    public void setDraw(boolean draw) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDraw with ");
        stringBuilder.append(draw);
        GLLogUtils.logD(str, stringBuilder.toString());
        this.mGestureBackTexture.setDraw(draw);
        this.mGestureBackMetaBall.setDraw(draw);
    }

    public void addAnimationListener(GestureBackAnimListener listener) {
        this.mAnimationListener = listener;
    }

    public void addViewSizeChangeListener(ViewSizeChangeListener listener) {
        this.mViewSizeChangeListener = listener;
    }

    public void playFastSlidingAnim() {
        GLLogUtils.logD(TAG, "playFastSlidingAnim");
        this.mFastSlidingAnim.start();
    }

    public void playDisappearAnim() {
        GLLogUtils.logD(TAG, "playDisappearAnim");
        float animProcess = this.mGestureBackMetaBall.getProcess();
        AnimatorSet disappearAnimSet = new AnimatorSet();
        ObjectAnimator disappearAnim = ObjectAnimator.ofFloat(this, "animProcess", new float[]{animProcess, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO});
        disappearAnim.setDuration((long) (300.0f * animProcess));
        disappearAnim.setInterpolator(AnimationUtils.loadInterpolator(this.mContext, 17563661));
        disappearAnim.addListener(this.mAnimListener);
        disappearAnimSet.play(disappearAnim).after(30);
        disappearAnimSet.start();
    }
}
