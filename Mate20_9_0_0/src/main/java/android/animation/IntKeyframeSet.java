package android.animation;

import android.animation.Keyframes.IntKeyframes;
import java.util.List;

class IntKeyframeSet extends KeyframeSet implements IntKeyframes {
    public IntKeyframeSet(IntKeyframe... keyframes) {
        super(keyframes);
    }

    public Object getValue(float fraction) {
        return Integer.valueOf(getIntValue(fraction));
    }

    public IntKeyframeSet clone() {
        List<Keyframe> keyframes = this.mKeyframes;
        int numKeyframes = this.mKeyframes.size();
        IntKeyframe[] newKeyframes = new IntKeyframe[numKeyframes];
        for (int i = 0; i < numKeyframes; i++) {
            newKeyframes[i] = (IntKeyframe) ((Keyframe) keyframes.get(i)).clone();
        }
        return new IntKeyframeSet(newKeyframes);
    }

    public int getIntValue(float fraction) {
        IntKeyframe prevKeyframe;
        IntKeyframe nextKeyframe;
        int prevValue;
        int nextValue;
        float prevFraction;
        float nextFraction;
        TimeInterpolator interpolator;
        float intervalFraction;
        int i;
        if (fraction <= 0.0f) {
            prevKeyframe = (IntKeyframe) this.mKeyframes.get(0);
            nextKeyframe = (IntKeyframe) this.mKeyframes.get(1);
            prevValue = prevKeyframe.getIntValue();
            nextValue = nextKeyframe.getIntValue();
            prevFraction = prevKeyframe.getFraction();
            nextFraction = nextKeyframe.getFraction();
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            intervalFraction = (fraction - prevFraction) / (nextFraction - prevFraction);
            if (this.mEvaluator == null) {
                i = ((int) (((float) (nextValue - prevValue)) * intervalFraction)) + prevValue;
            } else {
                i = ((Number) this.mEvaluator.evaluate(intervalFraction, Integer.valueOf(prevValue), Integer.valueOf(nextValue))).intValue();
            }
            return i;
        } else if (fraction >= 1.0f) {
            prevKeyframe = (IntKeyframe) this.mKeyframes.get(this.mNumKeyframes - 2);
            nextKeyframe = (IntKeyframe) this.mKeyframes.get(this.mNumKeyframes - 1);
            prevValue = prevKeyframe.getIntValue();
            nextValue = nextKeyframe.getIntValue();
            prevFraction = prevKeyframe.getFraction();
            nextFraction = nextKeyframe.getFraction();
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            intervalFraction = (fraction - prevFraction) / (nextFraction - prevFraction);
            if (this.mEvaluator == null) {
                i = ((int) (((float) (nextValue - prevValue)) * intervalFraction)) + prevValue;
            } else {
                i = ((Number) this.mEvaluator.evaluate(intervalFraction, Integer.valueOf(prevValue), Integer.valueOf(nextValue))).intValue();
            }
            return i;
        } else {
            nextKeyframe = (IntKeyframe) this.mKeyframes.get(0);
            for (int i2 = 1; i2 < this.mNumKeyframes; i2++) {
                IntKeyframe nextKeyframe2 = (IntKeyframe) this.mKeyframes.get(i2);
                if (fraction < nextKeyframe2.getFraction()) {
                    int i3;
                    TimeInterpolator interpolator2 = nextKeyframe2.getInterpolator();
                    prevFraction = (fraction - nextKeyframe.getFraction()) / (nextKeyframe2.getFraction() - nextKeyframe.getFraction());
                    int prevValue2 = nextKeyframe.getIntValue();
                    int nextValue2 = nextKeyframe2.getIntValue();
                    if (interpolator2 != null) {
                        prevFraction = interpolator2.getInterpolation(prevFraction);
                    }
                    if (this.mEvaluator == null) {
                        i3 = ((int) (((float) (nextValue2 - prevValue2)) * prevFraction)) + prevValue2;
                    } else {
                        i3 = ((Number) this.mEvaluator.evaluate(prevFraction, Integer.valueOf(prevValue2), Integer.valueOf(nextValue2))).intValue();
                    }
                    return i3;
                }
                nextKeyframe = nextKeyframe2;
            }
            return ((Number) ((Keyframe) this.mKeyframes.get(this.mNumKeyframes - 1)).getValue()).intValue();
        }
    }

    public Class getType() {
        return Integer.class;
    }
}
