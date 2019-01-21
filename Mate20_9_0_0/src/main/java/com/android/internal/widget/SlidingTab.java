package com.android.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.os.Vibrator;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import com.android.internal.R;

public class SlidingTab extends ViewGroup {
    private static final int ANIM_DURATION = 250;
    private static final int ANIM_TARGET_TIME = 500;
    private static final boolean DBG = false;
    private static final int HORIZONTAL = 0;
    private static final String LOG_TAG = "SlidingTab";
    private static final float THRESHOLD = 0.6666667f;
    private static final int TRACKING_MARGIN = 50;
    private static final int VERTICAL = 1;
    private static final long VIBRATE_LONG = 40;
    private static final long VIBRATE_SHORT = 30;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new Builder().setContentType(4).setUsage(13).build();
    private boolean mAnimating;
    private final AnimationListener mAnimationDoneListener;
    private Slider mCurrentSlider;
    private final float mDensity;
    private int mGrabbedState;
    private boolean mHoldLeftOnTransition;
    private boolean mHoldRightOnTransition;
    private final Slider mLeftSlider;
    private OnTriggerListener mOnTriggerListener;
    private final int mOrientation;
    private Slider mOtherSlider;
    private final Slider mRightSlider;
    private float mThreshold;
    private final Rect mTmpRect;
    private boolean mTracking;
    private boolean mTriggered;
    private Vibrator mVibrator;

    public interface OnTriggerListener {
        public static final int LEFT_HANDLE = 1;
        public static final int NO_HANDLE = 0;
        public static final int RIGHT_HANDLE = 2;

        void onGrabbedStateChange(View view, int i);

        void onTrigger(View view, int i);
    }

    private static class Slider {
        public static final int ALIGN_BOTTOM = 3;
        public static final int ALIGN_LEFT = 0;
        public static final int ALIGN_RIGHT = 1;
        public static final int ALIGN_TOP = 2;
        public static final int ALIGN_UNKNOWN = 4;
        private static final int STATE_ACTIVE = 2;
        private static final int STATE_NORMAL = 0;
        private static final int STATE_PRESSED = 1;
        private int alignment = 4;
        private int alignment_value;
        private int currentState = 0;
        private final ImageView tab;
        private final ImageView target;
        private final TextView text;

        Slider(ViewGroup parent, int tabId, int barId, int targetId) {
            this.tab = new ImageView(parent.getContext());
            this.tab.setBackgroundResource(tabId);
            this.tab.setScaleType(ScaleType.CENTER);
            this.tab.setLayoutParams(new LayoutParams(-2, -2));
            this.text = new TextView(parent.getContext());
            this.text.setLayoutParams(new LayoutParams(-2, -1));
            this.text.setBackgroundResource(barId);
            this.text.setTextAppearance(parent.getContext(), 16974759);
            this.target = new ImageView(parent.getContext());
            this.target.setImageResource(targetId);
            this.target.setScaleType(ScaleType.CENTER);
            this.target.setLayoutParams(new LayoutParams(-2, -2));
            this.target.setVisibility(4);
            parent.addView(this.target);
            parent.addView(this.tab);
            parent.addView(this.text);
        }

        void setIcon(int iconId) {
            this.tab.setImageResource(iconId);
        }

        void setTabBackgroundResource(int tabId) {
            this.tab.setBackgroundResource(tabId);
        }

        void setBarBackgroundResource(int barId) {
            this.text.setBackgroundResource(barId);
        }

        void setHintText(int resId) {
            this.text.setText(resId);
        }

        void hide() {
            int dy = 0;
            boolean horiz = this.alignment == 0 || this.alignment == 1;
            int dx = horiz ? this.alignment == 0 ? this.alignment_value - this.tab.getRight() : this.alignment_value - this.tab.getLeft() : 0;
            if (!horiz) {
                if (this.alignment == 2) {
                    dy = this.alignment_value - this.tab.getBottom();
                } else {
                    dy = this.alignment_value - this.tab.getTop();
                }
            }
            Animation trans = new TranslateAnimation(0.0f, (float) dx, 0.0f, (float) dy);
            trans.setDuration(250);
            trans.setFillAfter(true);
            this.tab.startAnimation(trans);
            this.text.startAnimation(trans);
            this.target.setVisibility(4);
        }

        void show(boolean animate) {
            int dy = 0;
            this.text.setVisibility(0);
            this.tab.setVisibility(0);
            if (animate) {
                boolean z = true;
                if (!(this.alignment == 0 || this.alignment == 1)) {
                    z = false;
                }
                boolean horiz = z;
                int dx = horiz ? this.alignment == 0 ? this.tab.getWidth() : -this.tab.getWidth() : 0;
                if (!horiz) {
                    dy = this.alignment == 2 ? this.tab.getHeight() : -this.tab.getHeight();
                }
                Animation trans = new TranslateAnimation((float) (-dx), 0.0f, (float) (-dy), 0.0f);
                trans.setDuration(250);
                this.tab.startAnimation(trans);
                this.text.startAnimation(trans);
            }
        }

        void setState(int state) {
            this.text.setPressed(state == 1);
            this.tab.setPressed(state == 1);
            if (state == 2) {
                int[] activeState = new int[]{16842914};
                if (this.text.getBackground().isStateful()) {
                    this.text.getBackground().setState(activeState);
                }
                if (this.tab.getBackground().isStateful()) {
                    this.tab.getBackground().setState(activeState);
                }
                this.text.setTextAppearance(this.text.getContext(), 16974758);
            } else {
                this.text.setTextAppearance(this.text.getContext(), 16974759);
            }
            this.currentState = state;
        }

        void showTarget() {
            AlphaAnimation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
            alphaAnim.setDuration(500);
            this.target.startAnimation(alphaAnim);
            this.target.setVisibility(0);
        }

        void reset(boolean animate) {
            setState(0);
            this.text.setVisibility(0);
            this.text.setTextAppearance(this.text.getContext(), 16974759);
            this.tab.setVisibility(0);
            this.target.setVisibility(4);
            boolean z = true;
            if (!(this.alignment == 0 || this.alignment == 1)) {
                z = false;
            }
            boolean horiz = z;
            int dx = horiz ? this.alignment == 0 ? this.alignment_value - this.tab.getLeft() : this.alignment_value - this.tab.getRight() : 0;
            int dy = horiz ? 0 : this.alignment == 2 ? this.alignment_value - this.tab.getTop() : this.alignment_value - this.tab.getBottom();
            if (animate) {
                TranslateAnimation trans = new TranslateAnimation(0.0f, (float) dx, 0.0f, (float) dy);
                trans.setDuration(250);
                trans.setFillAfter(false);
                this.text.startAnimation(trans);
                this.tab.startAnimation(trans);
                return;
            }
            if (horiz) {
                this.text.offsetLeftAndRight(dx);
                this.tab.offsetLeftAndRight(dx);
            } else {
                this.text.offsetTopAndBottom(dy);
                this.tab.offsetTopAndBottom(dy);
            }
            this.text.clearAnimation();
            this.tab.clearAnimation();
            this.target.clearAnimation();
        }

        void setTarget(int targetId) {
            this.target.setImageResource(targetId);
        }

        void layout(int l, int t, int r, int b, int alignment) {
            int handleWidth;
            int targetWidth;
            int parentWidth;
            int leftTarget;
            int rightTarget;
            int i = l;
            int i2 = t;
            int i3 = r;
            int i4 = b;
            int i5 = alignment;
            this.alignment = i5;
            Drawable tabBackground = this.tab.getBackground();
            int handleWidth2 = tabBackground.getIntrinsicWidth();
            int handleHeight = tabBackground.getIntrinsicHeight();
            Drawable targetDrawable = this.target.getDrawable();
            int targetWidth2 = targetDrawable.getIntrinsicWidth();
            int targetHeight = targetDrawable.getIntrinsicHeight();
            int parentWidth2 = i3 - i;
            int parentHeight = i4 - i2;
            int leftTarget2 = (((int) (((float) parentWidth2) * SlidingTab.THRESHOLD)) - targetWidth2) + (handleWidth2 / 2);
            int rightTarget2 = ((int) (((float) parentWidth2) * 0.3333333f)) - (handleWidth2 / 2);
            int left = (parentWidth2 - handleWidth2) / 2;
            int right = left + handleWidth2;
            if (i5 == 0) {
                handleWidth = handleWidth2;
                targetWidth = targetWidth2;
                parentWidth = parentWidth2;
                leftTarget = leftTarget2;
                rightTarget = rightTarget2;
            } else if (i5 == 1) {
                handleWidth = handleWidth2;
                targetWidth = targetWidth2;
                parentWidth = parentWidth2;
                leftTarget = leftTarget2;
                rightTarget = rightTarget2;
            } else {
                i3 = (parentWidth2 - targetWidth2) / 2;
                rightTarget = rightTarget2;
                rightTarget2 = (parentWidth2 + targetWidth2) / 2;
                i = (((int) (((float) parentHeight) * SlidingTab.THRESHOLD)) + (handleHeight / 2)) - targetHeight;
                targetWidth2 = ((int) (((float) parentHeight) * 0.3333333f)) - (handleHeight / 2);
                leftTarget = leftTarget2;
                if (i5 == 2) {
                    parentWidth = parentWidth2;
                    this.tab.layout(left, 0, right, handleHeight);
                    handleWidth = handleWidth2;
                    this.text.layout(left, 0 - parentHeight, right, 0);
                    this.target.layout(i3, i, rightTarget2, i + targetHeight);
                    this.alignment_value = i2;
                } else {
                    handleWidth = handleWidth2;
                    parentWidth = parentWidth2;
                    this.tab.layout(left, parentHeight - handleHeight, right, parentHeight);
                    this.text.layout(left, parentHeight, right, parentHeight + parentHeight);
                    this.target.layout(i3, targetWidth2, rightTarget2, targetWidth2 + targetHeight);
                    this.alignment_value = i4;
                }
                rightTarget2 = leftTarget;
                leftTarget2 = handleWidth;
                i2 = r;
                return;
            }
            i = (parentHeight - targetHeight) / 2;
            handleWidth2 = i + targetHeight;
            targetWidth2 = (parentHeight - handleHeight) / 2;
            parentWidth2 = (parentHeight + handleHeight) / 2;
            int i6;
            if (i5 == 0) {
                this.tab.layout(0, targetWidth2, handleWidth, parentWidth2);
                this.text.layout(0 - parentWidth, targetWidth2, 0, parentWidth2);
                this.text.setGravity(5);
                this.target.layout(leftTarget, i, leftTarget + targetWidth, handleWidth2);
                this.alignment_value = l;
                i4 = rightTarget;
                i6 = parentWidth;
                i2 = r;
                return;
            }
            i2 = l;
            i4 = parentWidth;
            this.tab.layout(parentWidth - handleWidth, targetWidth2, i4, parentWidth2);
            this.text.layout(i4, targetWidth2, i4 + i4, parentWidth2);
            i6 = i4;
            this.target.layout(rightTarget, i, rightTarget + targetWidth, handleWidth2);
            this.text.setGravity(48);
            this.alignment_value = r;
        }

        public void updateDrawableStates() {
            setState(this.currentState);
        }

        public void measure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            this.tab.measure(MeasureSpec.makeSafeMeasureSpec(width, 0), MeasureSpec.makeSafeMeasureSpec(height, 0));
            this.text.measure(MeasureSpec.makeSafeMeasureSpec(width, 0), MeasureSpec.makeSafeMeasureSpec(height, 0));
        }

        public int getTabWidth() {
            return this.tab.getMeasuredWidth();
        }

        public int getTabHeight() {
            return this.tab.getMeasuredHeight();
        }

        public void startAnimation(Animation anim1, Animation anim2) {
            this.tab.startAnimation(anim1);
            this.text.startAnimation(anim2);
        }

        public void hideTarget() {
            this.target.clearAnimation();
            this.target.setVisibility(4);
        }
    }

    public SlidingTab(Context context) {
        this(context, null);
    }

    public SlidingTab(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHoldLeftOnTransition = true;
        this.mHoldRightOnTransition = true;
        this.mGrabbedState = 0;
        this.mTriggered = false;
        this.mAnimationDoneListener = new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                SlidingTab.this.onAnimationDone();
            }
        };
        this.mTmpRect = new Rect();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingTab);
        this.mOrientation = a.getInt(0, 0);
        a.recycle();
        this.mDensity = getResources().getDisplayMetrics().density;
        this.mLeftSlider = new Slider(this, 17302856, 17302839, 17302870);
        this.mRightSlider = new Slider(this, 17302865, 17302848, 17302870);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        this.mLeftSlider.measure(widthMeasureSpec, heightMeasureSpec);
        this.mRightSlider.measure(widthMeasureSpec, heightMeasureSpec);
        int leftTabWidth = this.mLeftSlider.getTabWidth();
        int rightTabWidth = this.mRightSlider.getTabWidth();
        int leftTabHeight = this.mLeftSlider.getTabHeight();
        int rightTabHeight = this.mRightSlider.getTabHeight();
        if (isHorizontal()) {
            width = Math.max(widthSpecSize, leftTabWidth + rightTabWidth);
            height = Math.max(leftTabHeight, rightTabHeight);
        } else {
            width = Math.max(leftTabWidth, rightTabHeight);
            height = Math.max(heightSpecSize, leftTabHeight + rightTabHeight);
        }
        setMeasuredDimension(width, height);
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        if (this.mAnimating) {
            return false;
        }
        this.mLeftSlider.tab.getHitRect(this.mTmpRect);
        boolean leftHit = this.mTmpRect.contains((int) x, (int) y);
        this.mRightSlider.tab.getHitRect(this.mTmpRect);
        boolean rightHit = this.mTmpRect.contains((int) x, (int) y);
        if (!this.mTracking && !leftHit && !rightHit) {
            return false;
        }
        if (action == 0) {
            this.mTracking = true;
            this.mTriggered = false;
            vibrate(VIBRATE_SHORT);
            float f = 0.3333333f;
            if (leftHit) {
                this.mCurrentSlider = this.mLeftSlider;
                this.mOtherSlider = this.mRightSlider;
                if (isHorizontal()) {
                    f = THRESHOLD;
                }
                this.mThreshold = f;
                setGrabbedState(1);
            } else {
                this.mCurrentSlider = this.mRightSlider;
                this.mOtherSlider = this.mLeftSlider;
                if (!isHorizontal()) {
                    f = THRESHOLD;
                }
                this.mThreshold = f;
                setGrabbedState(2);
            }
            this.mCurrentSlider.setState(1);
            this.mCurrentSlider.showTarget();
            this.mOtherSlider.hide();
        }
        return true;
    }

    public void reset(boolean animate) {
        this.mLeftSlider.reset(animate);
        this.mRightSlider.reset(animate);
        if (!animate) {
            this.mAnimating = false;
        }
    }

    public void setVisibility(int visibility) {
        if (visibility != getVisibility() && visibility == 4) {
            reset(false);
        }
        super.setVisibility(visibility);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mTracking) {
            int action = event.getAction();
            float x = event.getX();
            float y = event.getY();
            switch (action) {
                case 2:
                    if (withinView(x, y, this)) {
                        moveHandle(x, y);
                        float position = isHorizontal() ? x : y;
                        float target = this.mThreshold * ((float) (isHorizontal() ? getWidth() : getHeight()));
                        boolean thresholdReached;
                        if (isHorizontal()) {
                            if (this.mCurrentSlider != this.mLeftSlider) {
                                thresholdReached = false;
                                break;
                            }
                            thresholdReached = false;
                            break;
                            thresholdReached = true;
                        } else {
                            if (this.mCurrentSlider != this.mLeftSlider) {
                                thresholdReached = false;
                                break;
                            }
                            thresholdReached = false;
                            break;
                            thresholdReached = true;
                        }
                        if (!this.mTriggered && thresholdReached) {
                            this.mTriggered = true;
                            this.mTracking = false;
                            int i = 2;
                            this.mCurrentSlider.setState(2);
                            boolean isLeft = this.mCurrentSlider == this.mLeftSlider;
                            if (isLeft) {
                                i = 1;
                            }
                            dispatchTriggerEvent(i);
                            startAnimating(isLeft ? this.mHoldLeftOnTransition : this.mHoldRightOnTransition);
                            setGrabbedState(0);
                            break;
                        }
                    }
                case 1:
                case 3:
                    cancelGrab();
                    break;
            }
        }
        if (this.mTracking || super.onTouchEvent(event)) {
            return true;
        }
        return false;
    }

    private void cancelGrab() {
        this.mTracking = false;
        this.mTriggered = false;
        this.mOtherSlider.show(true);
        this.mCurrentSlider.reset(false);
        this.mCurrentSlider.hideTarget();
        this.mCurrentSlider = null;
        this.mOtherSlider = null;
        setGrabbedState(0);
    }

    void startAnimating(final boolean holdAfter) {
        int right;
        int dx;
        this.mAnimating = true;
        Slider slider = this.mCurrentSlider;
        Slider other = this.mOtherSlider;
        int holdOffset = 0;
        int width;
        int left;
        int viewWidth;
        if (isHorizontal()) {
            right = slider.tab.getRight();
            width = slider.tab.getWidth();
            left = slider.tab.getLeft();
            viewWidth = getWidth();
            if (!holdAfter) {
                holdOffset = width;
            }
            if (slider == this.mRightSlider) {
                dx = -((right + viewWidth) - holdOffset);
            } else {
                dx = ((viewWidth - left) + viewWidth) - holdOffset;
            }
            right = 0;
        } else {
            int i;
            right = slider.tab.getTop();
            width = slider.tab.getBottom();
            left = slider.tab.getHeight();
            viewWidth = getHeight();
            if (!holdAfter) {
                holdOffset = left;
            }
            dx = 0;
            if (slider == this.mRightSlider) {
                i = (right + viewWidth) - holdOffset;
            } else {
                i = -(((viewWidth - width) + viewWidth) - holdOffset);
            }
            right = i;
        }
        Animation trans1 = new TranslateAnimation(0.0f, (float) dx, 0.0f, (float) right);
        trans1.setDuration(250);
        trans1.setInterpolator(new LinearInterpolator());
        trans1.setFillAfter(true);
        Animation trans2 = new TranslateAnimation(0.0f, (float) dx, 0.0f, (float) right);
        trans2.setDuration(250);
        trans2.setInterpolator(new LinearInterpolator());
        trans2.setFillAfter(true);
        trans1.setAnimationListener(new AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                Animation anim;
                if (holdAfter) {
                    anim = new TranslateAnimation((float) dx, (float) dx, (float) right, (float) right);
                    anim.setDuration(1000);
                    SlidingTab.this.mAnimating = false;
                } else {
                    anim = new AlphaAnimation(0.5f, 1.0f);
                    anim.setDuration(250);
                    SlidingTab.this.resetView();
                }
                anim.setAnimationListener(SlidingTab.this.mAnimationDoneListener);
                SlidingTab.this.mLeftSlider.startAnimation(anim, anim);
                SlidingTab.this.mRightSlider.startAnimation(anim, anim);
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });
        slider.hideTarget();
        slider.startAnimation(trans1, trans2);
    }

    private void onAnimationDone() {
        resetView();
        this.mAnimating = false;
    }

    private boolean withinView(float x, float y, View view) {
        return (isHorizontal() && y > -50.0f && y < ((float) (view.getHeight() + 50))) || (!isHorizontal() && x > -50.0f && x < ((float) (50 + view.getWidth())));
    }

    private boolean isHorizontal() {
        return this.mOrientation == 0;
    }

    private void resetView() {
        this.mLeftSlider.reset(false);
        this.mRightSlider.reset(false);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            this.mLeftSlider.layout(l, t, r, b, isHorizontal() ? 0 : 3);
            this.mRightSlider.layout(l, t, r, b, isHorizontal() ? 1 : 2);
        }
    }

    private void moveHandle(float x, float y) {
        View handle = this.mCurrentSlider.tab;
        View content = this.mCurrentSlider.text;
        int deltaX;
        if (isHorizontal()) {
            deltaX = (((int) x) - handle.getLeft()) - (handle.getWidth() / 2);
            handle.offsetLeftAndRight(deltaX);
            content.offsetLeftAndRight(deltaX);
        } else {
            deltaX = (((int) y) - handle.getTop()) - (handle.getHeight() / 2);
            handle.offsetTopAndBottom(deltaX);
            content.offsetTopAndBottom(deltaX);
        }
        invalidate();
    }

    public void setLeftTabResources(int iconId, int targetId, int barId, int tabId) {
        this.mLeftSlider.setIcon(iconId);
        this.mLeftSlider.setTarget(targetId);
        this.mLeftSlider.setBarBackgroundResource(barId);
        this.mLeftSlider.setTabBackgroundResource(tabId);
        this.mLeftSlider.updateDrawableStates();
    }

    public void setLeftHintText(int resId) {
        if (isHorizontal()) {
            this.mLeftSlider.setHintText(resId);
        }
    }

    public void setRightTabResources(int iconId, int targetId, int barId, int tabId) {
        this.mRightSlider.setIcon(iconId);
        this.mRightSlider.setTarget(targetId);
        this.mRightSlider.setBarBackgroundResource(barId);
        this.mRightSlider.setTabBackgroundResource(tabId);
        this.mRightSlider.updateDrawableStates();
    }

    public void setRightHintText(int resId) {
        if (isHorizontal()) {
            this.mRightSlider.setHintText(resId);
        }
    }

    public void setHoldAfterTrigger(boolean holdLeft, boolean holdRight) {
        this.mHoldLeftOnTransition = holdLeft;
        this.mHoldRightOnTransition = holdRight;
    }

    private synchronized void vibrate(long duration) {
        boolean z = true;
        if (System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 1, -2) == 0) {
            z = false;
        }
        if (z) {
            if (this.mVibrator == null) {
                this.mVibrator = (Vibrator) getContext().getSystemService("vibrator");
            }
            this.mVibrator.vibrate(duration, VIBRATION_ATTRIBUTES);
        }
    }

    public void setOnTriggerListener(OnTriggerListener listener) {
        this.mOnTriggerListener = listener;
    }

    private void dispatchTriggerEvent(int whichHandle) {
        vibrate(VIBRATE_LONG);
        if (this.mOnTriggerListener != null) {
            this.mOnTriggerListener.onTrigger(this, whichHandle);
        }
    }

    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView == this && visibility != 0 && this.mGrabbedState != 0) {
            cancelGrab();
        }
    }

    private void setGrabbedState(int newState) {
        if (newState != this.mGrabbedState) {
            this.mGrabbedState = newState;
            if (this.mOnTriggerListener != null) {
                this.mOnTriggerListener.onGrabbedStateChange(this, this.mGrabbedState);
            }
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
