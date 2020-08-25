package huawei.com.android.server.fingerprint.fingerprintAnimation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

public class BreathImageDrawable extends BitmapDrawable {
    public static final int BREATH = 1;
    private static final int BREATH_AINM_DURATION = 2000;
    public static final int DEFAULT = 0;
    private static final long FRAME_DURATION = 34;
    private static final int MSG_DO_INVALITE = 10;
    public static final int POST_BREATH = 2;
    private static final String TAG = "BreathImageDrawable";
    private static final int TOUCH_AINM_DURATION = 200;
    /* access modifiers changed from: private */
    public AnimatorSet mBreathAnim;
    private ObjectAnimator mBreathCircleAnim;
    private float mBreathCircleScale = 1.0f;
    private float mBreathImageScale = 1.0f;
    private ObjectAnimator mBreathTouchAnim;
    private Drawable mCircleDrawable;
    private float mDefaultBreathCircleScale;
    private float mDefaultBreathImageScale;
    private Handler mHandler = new Handler() {
        /* class huawei.com.android.server.fingerprint.fingerprintAnimation.BreathImageDrawable.AnonymousClass1 */

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 10) {
                BreathImageDrawable.this.invalidateSelf();
            }
        }
    };
    private Drawable mImageDrawable;
    private long mLastTime = 0;
    private int mStatus = 0;
    private float mTouchBreathScaleMax;
    private float mTouchBreathScaleMin;
    /* access modifiers changed from: private */
    public boolean mTouchUp = false;

    public BreathImageDrawable(Context context) {
    }

    public void setDrawableStatus(int status) {
        this.mStatus = status;
        resetBreathAnim();
    }

    public int getDrawableStatus() {
        return this.mStatus;
    }

    public float getBreathCircleScale() {
        return this.mBreathCircleScale;
    }

    public void setBreathCircleScale(float breathCircleScale) {
        this.mBreathCircleScale = breathCircleScale;
        invalidateSelf();
    }

    public float getBreathImageScale() {
        return this.mBreathImageScale;
    }

    public void invalidateSelf() {
        long currTime = System.currentTimeMillis();
        if (currTime - this.mLastTime > FRAME_DURATION) {
            super.invalidateSelf();
            this.mHandler.removeMessages(10);
        } else if (!this.mHandler.hasMessages(10)) {
            this.mHandler.sendEmptyMessageDelayed(10, (FRAME_DURATION + currTime) - this.mLastTime);
        }
    }

    public void setBreathImageScale(float breathImageScale) {
        this.mBreathImageScale = breathImageScale;
        invalidateSelf();
    }

    public void setBreathImageDrawable(Drawable image, Drawable circle) {
        this.mImageDrawable = image;
        this.mCircleDrawable = circle;
        initDrawable();
        initBreathAnim();
        initBreathTouchAnim();
        setAlpha(255);
    }

    private void initDrawable() {
        Drawable drawable = this.mImageDrawable;
        if (drawable != null) {
            drawable.setBounds(0, 0, getIntrinsicWidth(), getIntrinsicHeight());
        }
        Drawable drawable2 = this.mCircleDrawable;
        if (drawable2 != null) {
            drawable2.setBounds(0, 0, getIntrinsicWidth(), getIntrinsicHeight());
        }
    }

    private void initBreathAnim() {
        this.mBreathAnim = new AnimatorSet();
        this.mDefaultBreathImageScale = 1.0f;
        this.mDefaultBreathCircleScale = 1.0f;
        this.mBreathCircleScale = this.mDefaultBreathCircleScale;
        float f = this.mDefaultBreathImageScale;
        this.mBreathImageScale = f;
        ObjectAnimator fingerScaleAnim = ObjectAnimator.ofFloat(this, "BreathImageScale", f, 1.1f);
        fingerScaleAnim.setRepeatMode(2);
        fingerScaleAnim.setRepeatCount(-1);
        fingerScaleAnim.setInterpolator(new PathInterpolator(0.2f, 0.5f, 0.8f, 0.5f));
        this.mBreathCircleAnim = ObjectAnimator.ofFloat(this, "BreathCircleScale", this.mDefaultBreathCircleScale, 1.15f);
        this.mBreathCircleAnim.setRepeatMode(2);
        this.mBreathCircleAnim.setRepeatCount(-1);
        this.mBreathCircleAnim.setInterpolator(new FastOutSlowInInterpolator());
        this.mBreathAnim.play(fingerScaleAnim).with(this.mBreathCircleAnim);
        this.mBreathAnim.setStartDelay(0);
        this.mBreathAnim.setDuration(2000L);
    }

    private void initBreathTouchAnim() {
        this.mTouchBreathScaleMax = 1.3f;
        this.mTouchBreathScaleMin = 1.0f;
        this.mBreathTouchAnim = ObjectAnimator.ofFloat(this, "BreathCircleScale", this.mTouchBreathScaleMin, this.mTouchBreathScaleMax);
        this.mBreathTouchAnim.setDuration(200L);
        this.mBreathTouchAnim.setInterpolator(new FastOutSlowInInterpolator());
        this.mBreathTouchAnim.addListener(new AnimatorListenerAdapter() {
            /* class huawei.com.android.server.fingerprint.fingerprintAnimation.BreathImageDrawable.AnonymousClass2 */

            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (BreathImageDrawable.this.mTouchUp) {
                    BreathImageDrawable.this.mBreathAnim.setStartDelay(0);
                    BreathImageDrawable.this.mBreathAnim.start();
                }
            }
        });
    }

    private void resetBreathAnim() {
        if (this.mBreathAnim.isRunning()) {
            this.mBreathAnim.cancel();
        }
        setBreathImageScale(this.mDefaultBreathImageScale);
        setBreathCircleScale(this.mDefaultBreathCircleScale);
    }

    private void startBreathAnimImpl() {
        if (this.mBreathAnim == null) {
            initBreathAnim();
        }
        setDrawableStatus(1);
        this.mBreathAnim.start();
    }

    public void startBreathAnim() {
        if (this.mStatus == 0) {
            this.mStatus = 2;
            invalidateSelf();
            return;
        }
        startBreathAnimImpl();
    }

    public void startTouchDownBreathAnim() {
        setBreathImageScale(this.mDefaultBreathImageScale);
        this.mTouchUp = false;
        if (this.mBreathAnim.isRunning()) {
            this.mBreathAnim.cancel();
        }
        if (this.mBreathTouchAnim.isRunning()) {
            this.mBreathTouchAnim.reverse();
            return;
        }
        float currentBreathScale = 0.0f;
        try {
            currentBreathScale = Float.parseFloat(this.mBreathCircleAnim.getAnimatedValue("BreathCircleScale").toString());
        } catch (NumberFormatException e) {
            Log.w(TAG, "startTouchDownBreathAnim NumberFormatException");
        }
        this.mBreathTouchAnim.setFloatValues(currentBreathScale, this.mTouchBreathScaleMax);
        this.mBreathTouchAnim.start();
    }

    public void startTouchUpBreathAnim() {
        this.mTouchUp = true;
        if (this.mBreathTouchAnim == null) {
            initBreathTouchAnim();
        }
        if (this.mBreathTouchAnim.isRunning()) {
            this.mBreathTouchAnim.reverse();
            return;
        }
        this.mBreathTouchAnim.setFloatValues(this.mTouchBreathScaleMax, this.mTouchBreathScaleMin);
        this.mBreathTouchAnim.start();
    }

    public void draw(Canvas canvas) {
        if (this.mImageDrawable != null || this.mCircleDrawable != null) {
            float pivotX = ((float) getIntrinsicWidth()) / 2.0f;
            float pivotY = ((float) getIntrinsicHeight()) / 2.0f;
            drawLayer(canvas, this.mImageDrawable, this.mBreathImageScale, pivotX, pivotY);
            drawLayer(canvas, this.mCircleDrawable, this.mBreathCircleScale, pivotX, pivotY);
            if (this.mStatus == 2) {
                startBreathAnimImpl();
            }
            this.mLastTime = System.currentTimeMillis();
        }
    }

    private void drawLayer(Canvas canvas, Drawable able, float scale, float pivotX, float pivotY) {
        if (able != null) {
            if (Math.abs(1.0f - scale) > 1.0E-4f) {
                canvas.save();
                canvas.scale(scale, scale, pivotX, pivotY);
                able.draw(canvas);
                canvas.restore();
                return;
            }
            able.draw(canvas);
        }
    }

    public void setAlpha(int i) {
        Drawable drawable = this.mImageDrawable;
        if (drawable != null) {
            drawable.setAlpha(i);
        }
        Drawable drawable2 = this.mCircleDrawable;
        if (drawable2 != null) {
            drawable2.setAlpha(i);
        }
    }

    public void setColorFilter(ColorFilter colorFilter) {
    }

    public int getOpacity() {
        return -3;
    }

    public int getIntrinsicWidth() {
        Drawable drawable = this.mImageDrawable;
        int secondWidth = 0;
        int maxWidth = drawable == null ? 0 : drawable.getIntrinsicWidth();
        Drawable drawable2 = this.mCircleDrawable;
        if (drawable2 != null) {
            secondWidth = drawable2.getIntrinsicWidth();
        }
        return maxWidth > secondWidth ? maxWidth : secondWidth;
    }

    public int getIntrinsicHeight() {
        Drawable drawable = this.mImageDrawable;
        int secondHeight = 0;
        int maxHeight = drawable == null ? 0 : drawable.getIntrinsicHeight();
        Drawable drawable2 = this.mCircleDrawable;
        if (drawable2 != null) {
            secondHeight = drawable2.getIntrinsicHeight();
        }
        return maxHeight > secondHeight ? maxHeight : secondHeight;
    }

    /* access modifiers changed from: protected */
    public void onBoundsChange(Rect bounds) {
    }

    public static class FastOutSlowInInterpolator implements Interpolator {
        private static final float[] VALUES = {0.0f, 1.0E-4f, 2.0E-4f, 5.0E-4f, 9.0E-4f, 0.0014f, 0.002f, 0.0027f, 0.0036f, 0.0046f, 0.0058f, 0.0071f, 0.0085f, 0.0101f, 0.0118f, 0.0137f, 0.0158f, 0.018f, 0.0205f, 0.0231f, 0.0259f, 0.0289f, 0.0321f, 0.0355f, 0.0391f, 0.043f, 0.0471f, 0.0514f, 0.056f, 0.0608f, 0.066f, 0.0714f, 0.0771f, 0.083f, 0.0893f, 0.0959f, 0.1029f, 0.1101f, 0.1177f, 0.1257f, 0.1339f, 0.1426f, 0.1516f, 0.161f, 0.1707f, 0.1808f, 0.1913f, 0.2021f, 0.2133f, 0.2248f, 0.2366f, 0.2487f, 0.2611f, 0.2738f, 0.2867f, 0.2998f, 0.3131f, 0.3265f, 0.34f, 0.3536f, 0.3673f, 0.381f, 0.3946f, 0.4082f, 0.4217f, 0.4352f, 0.4485f, 0.4616f, 0.4746f, 0.4874f, 0.5f, 0.5124f, 0.5246f, 0.5365f, 0.5482f, 0.5597f, 0.571f, 0.582f, 0.5928f, 0.6033f, 0.6136f, 0.6237f, 0.6335f, 0.6431f, 0.6525f, 0.6616f, 0.6706f, 0.6793f, 0.6878f, 0.6961f, 0.7043f, 0.7122f, 0.7199f, 0.7275f, 0.7349f, 0.7421f, 0.7491f, 0.7559f, 0.7626f, 0.7692f, 0.7756f, 0.7818f, 0.7879f, 0.7938f, 0.7996f, 0.8053f, 0.8108f, 0.8162f, 0.8215f, 0.8266f, 0.8317f, 0.8366f, 0.8414f, 0.8461f, 0.8507f, 0.8551f, 0.8595f, 0.8638f, 0.8679f, 0.872f, 0.876f, 0.8798f, 0.8836f, 0.8873f, 0.8909f, 0.8945f, 0.8979f, 0.9013f, 0.9046f, 0.9078f, 0.9109f, 0.9139f, 0.9169f, 0.9198f, 0.9227f, 0.9254f, 0.9281f, 0.9307f, 0.9333f, 0.9358f, 0.9382f, 0.9406f, 0.9429f, 0.9452f, 0.9474f, 0.9495f, 0.9516f, 0.9536f, 0.9556f, 0.9575f, 0.9594f, 0.9612f, 0.9629f, 0.9646f, 0.9663f, 0.9679f, 0.9695f, 0.971f, 0.9725f, 0.9739f, 0.9753f, 0.9766f, 0.9779f, 0.9791f, 0.9803f, 0.9815f, 0.9826f, 0.9837f, 0.9848f, 0.9858f, 0.9867f, 0.9877f, 0.9885f, 0.9894f, 0.9902f, 0.991f, 0.9917f, 0.9924f, 0.9931f, 0.9937f, 0.9944f, 0.9949f, 0.9955f, 0.996f, 0.9964f, 0.9969f, 0.9973f, 0.9977f, 0.998f, 0.9984f, 0.9986f, 0.9989f, 0.9991f, 0.9993f, 0.9995f, 0.9997f, 0.9998f, 0.9999f, 0.9999f, 1.0f, 1.0f};
        private final float mStepSize;
        private final float[] mValues;

        public FastOutSlowInInterpolator() {
            this.mValues = VALUES;
            this.mStepSize = 1.0f / ((float) (this.mValues.length - 1));
        }

        public FastOutSlowInInterpolator(float[] values) {
            this.mValues = values;
            this.mStepSize = 1.0f / ((float) (this.mValues.length - 1));
        }

        public float getInterpolation(float input) {
            if (input >= 1.0f) {
                return 1.0f;
            }
            if (input <= 0.0f) {
                return 0.0f;
            }
            float[] fArr = this.mValues;
            int length = (int) (((float) (fArr.length - 1)) * input);
            int length2 = fArr.length - 2;
            int length3 = fArr.length;
            int position = length < length2 ? (int) (((float) (length3 - 1)) * input) : length3 - 2;
            float f = this.mStepSize;
            float[] fArr2 = this.mValues;
            return fArr2[position] + ((fArr2[position + 1] - fArr2[position]) * ((input - (((float) position) * f)) / f));
        }
    }
}
