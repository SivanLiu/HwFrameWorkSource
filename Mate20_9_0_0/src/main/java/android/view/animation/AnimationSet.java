package android.view.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.telephony.SubscriptionPlan;
import android.util.AttributeSet;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.List;

public class AnimationSet extends Animation {
    private static final int PROPERTY_CHANGE_BOUNDS_MASK = 128;
    private static final int PROPERTY_DURATION_MASK = 32;
    private static final int PROPERTY_FILL_AFTER_MASK = 1;
    private static final int PROPERTY_FILL_BEFORE_MASK = 2;
    private static final int PROPERTY_MORPH_MATRIX_MASK = 64;
    private static final int PROPERTY_REPEAT_MODE_MASK = 4;
    private static final int PROPERTY_SHARE_INTERPOLATOR_MASK = 16;
    private static final int PROPERTY_START_OFFSET_MASK = 8;
    private ArrayList<Animation> mAnimations = new ArrayList();
    private boolean mDirty;
    private int mFlags = 0;
    private boolean mHasAlpha;
    private long mLastEnd;
    private long[] mStoredOffsets;
    private Transformation mTempTransformation = new Transformation();

    public AnimationSet(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AnimationSet);
        setFlag(16, a.getBoolean(1, true));
        init();
        if (context.getApplicationInfo().targetSdkVersion >= 14) {
            if (a.hasValue(0)) {
                this.mFlags |= 32;
            }
            if (a.hasValue(2)) {
                this.mFlags = 2 | this.mFlags;
            }
            if (a.hasValue(3)) {
                this.mFlags |= 1;
            }
            if (a.hasValue(5)) {
                this.mFlags |= 4;
            }
            if (a.hasValue(4)) {
                this.mFlags |= 8;
            }
        }
        a.recycle();
    }

    public AnimationSet(boolean shareInterpolator) {
        setFlag(16, shareInterpolator);
        init();
    }

    protected AnimationSet clone() throws CloneNotSupportedException {
        AnimationSet animation = (AnimationSet) super.clone();
        animation.mTempTransformation = new Transformation();
        animation.mAnimations = new ArrayList();
        int count = this.mAnimations.size();
        ArrayList<Animation> animations = this.mAnimations;
        for (int i = 0; i < count; i++) {
            animation.mAnimations.add(((Animation) animations.get(i)).clone());
        }
        return animation;
    }

    private void setFlag(int mask, boolean value) {
        if (value) {
            this.mFlags |= mask;
        } else {
            this.mFlags &= ~mask;
        }
    }

    private void init() {
        this.mStartTime = 0;
    }

    public void setFillAfter(boolean fillAfter) {
        this.mFlags |= 1;
        super.setFillAfter(fillAfter);
    }

    public void setFillBefore(boolean fillBefore) {
        this.mFlags |= 2;
        super.setFillBefore(fillBefore);
    }

    public void setRepeatMode(int repeatMode) {
        this.mFlags |= 4;
        super.setRepeatMode(repeatMode);
    }

    public void setStartOffset(long startOffset) {
        this.mFlags |= 8;
        super.setStartOffset(startOffset);
    }

    public boolean hasAlpha() {
        if (this.mDirty) {
            int i = 0;
            this.mHasAlpha = false;
            this.mDirty = false;
            int count = this.mAnimations.size();
            ArrayList<Animation> animations = this.mAnimations;
            while (i < count) {
                if (((Animation) animations.get(i)).hasAlpha()) {
                    this.mHasAlpha = true;
                    break;
                }
                i++;
            }
        }
        return this.mHasAlpha;
    }

    public void setDuration(long durationMillis) {
        this.mFlags |= 32;
        super.setDuration(durationMillis);
        this.mLastEnd = this.mStartOffset + this.mDuration;
    }

    public void addAnimation(Animation a) {
        this.mAnimations.add(a);
        boolean changeBounds = false;
        if (((this.mFlags & 64) == 0) && a.willChangeTransformationMatrix()) {
            this.mFlags |= 64;
        }
        if ((this.mFlags & 128) == 0) {
            changeBounds = true;
        }
        if (changeBounds && a.willChangeBounds()) {
            this.mFlags |= 128;
        }
        if ((this.mFlags & 32) == 32) {
            this.mLastEnd = this.mStartOffset + this.mDuration;
        } else if (this.mAnimations.size() == 1) {
            this.mDuration = a.getStartOffset() + a.getDuration();
            this.mLastEnd = this.mStartOffset + this.mDuration;
        } else {
            this.mLastEnd = Math.max(this.mLastEnd, (this.mStartOffset + a.getStartOffset()) + a.getDuration());
            this.mDuration = this.mLastEnd - this.mStartOffset;
        }
        this.mDirty = true;
    }

    public void setStartTime(long startTimeMillis) {
        super.setStartTime(startTimeMillis);
        int count = this.mAnimations.size();
        ArrayList<Animation> animations = this.mAnimations;
        for (int i = 0; i < count; i++) {
            ((Animation) animations.get(i)).setStartTime(startTimeMillis);
        }
    }

    public long getStartTime() {
        long startTime = SubscriptionPlan.BYTES_UNLIMITED;
        int count = this.mAnimations.size();
        ArrayList<Animation> animations = this.mAnimations;
        for (int i = 0; i < count; i++) {
            startTime = Math.min(startTime, ((Animation) animations.get(i)).getStartTime());
        }
        return startTime;
    }

    public void restrictDuration(long durationMillis) {
        super.restrictDuration(durationMillis);
        ArrayList<Animation> animations = this.mAnimations;
        int count = animations.size();
        for (int i = 0; i < count; i++) {
            ((Animation) animations.get(i)).restrictDuration(durationMillis);
        }
    }

    public long getDuration() {
        ArrayList<Animation> animations = this.mAnimations;
        int count = animations.size();
        long duration = 0;
        int i = 0;
        if ((this.mFlags & 32) == 32) {
            return this.mDuration;
        }
        while (true) {
            int i2 = i;
            if (i2 >= count) {
                return duration;
            }
            duration = Math.max(duration, ((Animation) animations.get(i2)).getDuration());
            i = i2 + 1;
        }
    }

    public long computeDurationHint() {
        long duration = 0;
        int count = this.mAnimations.size();
        ArrayList<Animation> animations = this.mAnimations;
        for (int i = count - 1; i >= 0; i--) {
            long d = ((Animation) animations.get(i)).computeDurationHint();
            if (d > duration) {
                duration = d;
            }
        }
        return duration;
    }

    public void initializeInvalidateRegion(int left, int top, int right, int bottom) {
        RectF region = this.mPreviousRegion;
        region.set((float) left, (float) top, (float) right, (float) bottom);
        region.inset(-1.0f, -1.0f);
        if (this.mFillBefore) {
            int count = this.mAnimations.size();
            ArrayList<Animation> animations = this.mAnimations;
            Transformation temp = this.mTempTransformation;
            Transformation previousTransformation = this.mPreviousTransformation;
            for (int i = count - 1; i >= 0; i--) {
                Animation a = (Animation) animations.get(i);
                if (!a.isFillEnabled() || a.getFillBefore() || a.getStartOffset() == 0) {
                    temp.clear();
                    Interpolator interpolator = a.mInterpolator;
                    float f = 0.0f;
                    if (interpolator != null) {
                        f = interpolator.getInterpolation(0.0f);
                    }
                    a.applyTransformation(f, temp);
                    previousTransformation.compose(temp);
                }
            }
        }
    }

    public boolean getTransformation(long currentTime, Transformation t) {
        int count = this.mAnimations.size();
        ArrayList<Animation> animations = this.mAnimations;
        Transformation temp = this.mTempTransformation;
        boolean more = false;
        boolean started = false;
        boolean ended = true;
        t.clear();
        for (int i = count - 1; i >= 0; i--) {
            Animation a = (Animation) animations.get(i);
            temp.clear();
            boolean z = false;
            boolean z2 = a.getTransformation(currentTime, temp, getScaleFactor()) || more;
            more = z2;
            t.compose(temp);
            z2 = started || a.hasStarted();
            started = z2;
            if (a.hasEnded() && ended) {
                z = true;
            }
            ended = z;
        }
        if (started && !this.mStarted) {
            if (this.mListener != null) {
                this.mListener.onAnimationStart(this);
            }
            this.mStarted = true;
        }
        if (ended != this.mEnded) {
            if (this.mListener != null) {
                this.mListener.onAnimationEnd(this);
            }
            this.mEnded = ended;
        }
        return more;
    }

    public void scaleCurrentDuration(float scale) {
        ArrayList<Animation> animations = this.mAnimations;
        int count = animations.size();
        for (int i = 0; i < count; i++) {
            ((Animation) animations.get(i)).scaleCurrentDuration(scale);
        }
    }

    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        boolean startOffsetSet = true;
        boolean durationSet = (this.mFlags & 32) == 32;
        boolean fillAfterSet = (this.mFlags & 1) == 1;
        boolean fillBeforeSet = (this.mFlags & 2) == 2;
        boolean repeatModeSet = (this.mFlags & 4) == 4;
        boolean shareInterpolator = (this.mFlags & 16) == 16;
        if ((this.mFlags & 8) != 8) {
            startOffsetSet = false;
        }
        if (shareInterpolator) {
            ensureInterpolator();
        }
        ArrayList<Animation> children = this.mAnimations;
        int count = children.size();
        long duration = this.mDuration;
        boolean fillAfter = this.mFillAfter;
        boolean fillBefore = this.mFillBefore;
        int repeatMode = this.mRepeatMode;
        Interpolator interpolator = this.mInterpolator;
        boolean startOffsetSet2 = startOffsetSet;
        long startOffset = this.mStartOffset;
        long[] storedOffsets = this.mStoredOffsets;
        if (startOffsetSet2) {
            if (storedOffsets == null || storedOffsets.length != count) {
                long[] jArr = new long[count];
                this.mStoredOffsets = jArr;
                storedOffsets = jArr;
            }
        } else if (storedOffsets != null) {
            this.mStoredOffsets = null;
            storedOffsets = null;
        }
        int i = 0;
        while (true) {
            int i2 = i;
            boolean fillAfterSet2;
            long[] storedOffsets2;
            boolean fillBeforeSet2;
            if (i2 < count) {
                boolean durationSet2;
                Animation a = (Animation) children.get(i2);
                if (durationSet) {
                    a.setDuration(duration);
                }
                if (fillAfterSet) {
                    a.setFillAfter(fillAfter);
                }
                if (fillBeforeSet) {
                    a.setFillBefore(fillBefore);
                }
                if (repeatModeSet) {
                    a.setRepeatMode(repeatMode);
                }
                if (shareInterpolator) {
                    a.setInterpolator(interpolator);
                }
                if (startOffsetSet2) {
                    long offset = a.getStartOffset();
                    durationSet2 = durationSet;
                    fillAfterSet2 = fillAfterSet;
                    a.setStartOffset(offset + startOffset);
                    storedOffsets[i2] = offset;
                } else {
                    durationSet2 = durationSet;
                    fillAfterSet2 = fillAfterSet;
                }
                storedOffsets2 = storedOffsets;
                fillBeforeSet2 = fillBeforeSet;
                a.initialize(width, height, parentWidth, parentHeight);
                i = i2 + 1;
                durationSet = durationSet2;
                fillAfterSet = fillAfterSet2;
                storedOffsets = storedOffsets2;
                fillBeforeSet = fillBeforeSet2;
            } else {
                fillAfterSet2 = fillAfterSet;
                storedOffsets2 = storedOffsets;
                fillBeforeSet2 = fillBeforeSet;
                durationSet = width;
                fillAfterSet = height;
                storedOffsets = parentWidth;
                fillBeforeSet = parentHeight;
                return;
            }
        }
    }

    public void reset() {
        super.reset();
        restoreChildrenStartOffset();
    }

    void restoreChildrenStartOffset() {
        long[] offsets = this.mStoredOffsets;
        if (offsets != null) {
            ArrayList<Animation> children = this.mAnimations;
            int count = children.size();
            for (int i = 0; i < count; i++) {
                ((Animation) children.get(i)).setStartOffset(offsets[i]);
            }
        }
    }

    public List<Animation> getAnimations() {
        return this.mAnimations;
    }

    public boolean willChangeTransformationMatrix() {
        return (this.mFlags & 64) == 64;
    }

    public boolean willChangeBounds() {
        return (this.mFlags & 128) == 128;
    }
}
