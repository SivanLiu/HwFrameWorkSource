package android.animation;

import android.animation.Keyframes.FloatKeyframes;
import java.util.List;

class FloatKeyframeSet extends KeyframeSet implements FloatKeyframes {
    public FloatKeyframeSet(FloatKeyframe... keyframes) {
        super(keyframes);
    }

    public Object getValue(float fraction) {
        return Float.valueOf(getFloatValue(fraction));
    }

    public FloatKeyframeSet clone() {
        List<Keyframe> keyframes = this.mKeyframes;
        int numKeyframes = this.mKeyframes.size();
        FloatKeyframe[] newKeyframes = new FloatKeyframe[numKeyframes];
        for (int i = 0; i < numKeyframes; i++) {
            newKeyframes[i] = (FloatKeyframe) ((Keyframe) keyframes.get(i)).clone();
        }
        return new FloatKeyframeSet(newKeyframes);
    }

    public float getFloatValue(float fraction) {
        FloatKeyframe prevKeyframe;
        FloatKeyframe nextKeyframe;
        float prevValue;
        float nextValue;
        float prevFraction;
        float nextFraction;
        TimeInterpolator interpolator;
        float intervalFraction;
        float f;
        if (fraction <= 0.0f) {
            prevKeyframe = (FloatKeyframe) this.mKeyframes.get(0);
            nextKeyframe = (FloatKeyframe) this.mKeyframes.get(1);
            prevValue = prevKeyframe.getFloatValue();
            nextValue = nextKeyframe.getFloatValue();
            prevFraction = prevKeyframe.getFraction();
            nextFraction = nextKeyframe.getFraction();
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            intervalFraction = (fraction - prevFraction) / (nextFraction - prevFraction);
            if (this.mEvaluator == null) {
                f = ((nextValue - prevValue) * intervalFraction) + prevValue;
            } else {
                f = ((Number) this.mEvaluator.evaluate(intervalFraction, Float.valueOf(prevValue), Float.valueOf(nextValue))).floatValue();
            }
            return f;
        } else if (fraction >= 1.0f) {
            prevKeyframe = (FloatKeyframe) this.mKeyframes.get(this.mNumKeyframes - 2);
            nextKeyframe = (FloatKeyframe) this.mKeyframes.get(this.mNumKeyframes - 1);
            prevValue = prevKeyframe.getFloatValue();
            nextValue = nextKeyframe.getFloatValue();
            prevFraction = prevKeyframe.getFraction();
            nextFraction = nextKeyframe.getFraction();
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            intervalFraction = (fraction - prevFraction) / (nextFraction - prevFraction);
            if (this.mEvaluator == null) {
                f = ((nextValue - prevValue) * intervalFraction) + prevValue;
            } else {
                f = ((Number) this.mEvaluator.evaluate(intervalFraction, Float.valueOf(prevValue), Float.valueOf(nextValue))).floatValue();
            }
            return f;
        } else {
            nextKeyframe = (FloatKeyframe) this.mKeyframes.get(0);
            for (int i = 1; i < this.mNumKeyframes; i++) {
                FloatKeyframe nextKeyframe2 = (FloatKeyframe) this.mKeyframes.get(i);
                if (fraction < nextKeyframe2.getFraction()) {
                    TimeInterpolator interpolator2 = nextKeyframe2.getInterpolator();
                    prevFraction = (fraction - nextKeyframe.getFraction()) / (nextKeyframe2.getFraction() - nextKeyframe.getFraction());
                    nextFraction = nextKeyframe.getFloatValue();
                    float nextValue2 = nextKeyframe2.getFloatValue();
                    if (interpolator2 != null) {
                        prevFraction = interpolator2.getInterpolation(prevFraction);
                    }
                    if (this.mEvaluator == null) {
                        intervalFraction = ((nextValue2 - nextFraction) * prevFraction) + nextFraction;
                    } else {
                        intervalFraction = ((Number) this.mEvaluator.evaluate(prevFraction, Float.valueOf(nextFraction), Float.valueOf(nextValue2))).floatValue();
                    }
                    return intervalFraction;
                }
                nextKeyframe = nextKeyframe2;
            }
            return ((Number) ((Keyframe) this.mKeyframes.get(this.mNumKeyframes - 1)).getValue()).floatValue();
        }
    }

    public Class getType() {
        return Float.class;
    }
}
