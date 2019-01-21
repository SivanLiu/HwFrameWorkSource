package android.animation;

import android.graphics.Path;
import android.util.Log;
import java.util.Arrays;
import java.util.List;

public class KeyframeSet implements Keyframes {
    TypeEvaluator mEvaluator;
    Keyframe mFirstKeyframe;
    TimeInterpolator mInterpolator = this.mLastKeyframe.getInterpolator();
    List<Keyframe> mKeyframes;
    Keyframe mLastKeyframe;
    int mNumKeyframes;

    public KeyframeSet(Keyframe... keyframes) {
        this.mNumKeyframes = keyframes.length;
        this.mKeyframes = Arrays.asList(keyframes);
        this.mFirstKeyframe = keyframes[0];
        this.mLastKeyframe = keyframes[this.mNumKeyframes - 1];
    }

    public List<Keyframe> getKeyframes() {
        return this.mKeyframes;
    }

    public static KeyframeSet ofInt(int... values) {
        int numKeyframes = values.length;
        IntKeyframe[] keyframes = new IntKeyframe[Math.max(numKeyframes, 2)];
        int i = 1;
        if (numKeyframes != 1) {
            keyframes[0] = (IntKeyframe) Keyframe.ofInt(0.0f, values[0]);
            while (true) {
                int i2 = i;
                if (i2 >= numKeyframes) {
                    break;
                }
                keyframes[i2] = (IntKeyframe) Keyframe.ofInt(((float) i2) / ((float) (numKeyframes - 1)), values[i2]);
                i = i2 + 1;
            }
        } else {
            keyframes[0] = (IntKeyframe) Keyframe.ofInt(0.0f);
            keyframes[1] = (IntKeyframe) Keyframe.ofInt(1.0f, values[0]);
        }
        return new IntKeyframeSet(keyframes);
    }

    public static KeyframeSet ofFloat(float... values) {
        boolean badValue = false;
        int numKeyframes = values.length;
        FloatKeyframe[] keyframes = new FloatKeyframe[Math.max(numKeyframes, 2)];
        int i = 1;
        if (numKeyframes != 1) {
            keyframes[0] = (FloatKeyframe) Keyframe.ofFloat(0.0f, values[0]);
            while (true) {
                int i2 = i;
                if (i2 >= numKeyframes) {
                    break;
                }
                keyframes[i2] = (FloatKeyframe) Keyframe.ofFloat(((float) i2) / ((float) (numKeyframes - 1)), values[i2]);
                if (Float.isNaN(values[i2])) {
                    badValue = true;
                }
                i = i2 + 1;
            }
        } else {
            keyframes[0] = (FloatKeyframe) Keyframe.ofFloat(0.0f);
            keyframes[1] = (FloatKeyframe) Keyframe.ofFloat(1.0f, values[0]);
            if (Float.isNaN(values[0])) {
                badValue = true;
            }
        }
        if (badValue) {
            Log.w("Animator", "Bad value (NaN) in float animator");
        }
        return new FloatKeyframeSet(keyframes);
    }

    public static KeyframeSet ofKeyframe(Keyframe... keyframes) {
        int numKeyframes = keyframes.length;
        int i = 0;
        boolean hasOther = false;
        boolean hasInt = false;
        boolean hasFloat = false;
        for (int i2 = 0; i2 < numKeyframes; i2++) {
            if (keyframes[i2] instanceof FloatKeyframe) {
                hasFloat = true;
            } else if (keyframes[i2] instanceof IntKeyframe) {
                hasInt = true;
            } else {
                hasOther = true;
            }
        }
        if (hasFloat && !hasInt && !hasOther) {
            FloatKeyframe[] floatKeyframes = new FloatKeyframe[numKeyframes];
            while (i < numKeyframes) {
                floatKeyframes[i] = (FloatKeyframe) keyframes[i];
                i++;
            }
            return new FloatKeyframeSet(floatKeyframes);
        } else if (!hasInt || hasFloat || hasOther) {
            return new KeyframeSet(keyframes);
        } else {
            IntKeyframe[] intKeyframes = new IntKeyframe[numKeyframes];
            while (i < numKeyframes) {
                intKeyframes[i] = (IntKeyframe) keyframes[i];
                i++;
            }
            return new IntKeyframeSet(intKeyframes);
        }
    }

    public static KeyframeSet ofObject(Object... values) {
        int numKeyframes = values.length;
        ObjectKeyframe[] keyframes = new ObjectKeyframe[Math.max(numKeyframes, 2)];
        int i = 1;
        if (numKeyframes != 1) {
            keyframes[0] = (ObjectKeyframe) Keyframe.ofObject(0.0f, values[0]);
            while (true) {
                int i2 = i;
                if (i2 >= numKeyframes) {
                    break;
                }
                keyframes[i2] = (ObjectKeyframe) Keyframe.ofObject(((float) i2) / ((float) (numKeyframes - 1)), values[i2]);
                i = i2 + 1;
            }
        } else {
            keyframes[0] = (ObjectKeyframe) Keyframe.ofObject(0.0f);
            keyframes[1] = (ObjectKeyframe) Keyframe.ofObject(1.0f, values[0]);
        }
        return new KeyframeSet(keyframes);
    }

    public static PathKeyframes ofPath(Path path) {
        return new PathKeyframes(path);
    }

    public static PathKeyframes ofPath(Path path, float error) {
        return new PathKeyframes(path, error);
    }

    public void setEvaluator(TypeEvaluator evaluator) {
        this.mEvaluator = evaluator;
    }

    public Class getType() {
        return this.mFirstKeyframe.getType();
    }

    public KeyframeSet clone() {
        List<Keyframe> keyframes = this.mKeyframes;
        int numKeyframes = this.mKeyframes.size();
        Keyframe[] newKeyframes = new Keyframe[numKeyframes];
        for (int i = 0; i < numKeyframes; i++) {
            newKeyframes[i] = ((Keyframe) keyframes.get(i)).clone();
        }
        return new KeyframeSet(newKeyframes);
    }

    public Object getValue(float fraction) {
        if (this.mNumKeyframes == 2) {
            if (this.mInterpolator != null) {
                fraction = this.mInterpolator.getInterpolation(fraction);
            }
            return this.mEvaluator.evaluate(fraction, this.mFirstKeyframe.getValue(), this.mLastKeyframe.getValue());
        }
        int i = 1;
        Keyframe nextKeyframe;
        TimeInterpolator interpolator;
        float prevFraction;
        if (fraction <= 0.0f) {
            nextKeyframe = (Keyframe) this.mKeyframes.get(1);
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            prevFraction = this.mFirstKeyframe.getFraction();
            return this.mEvaluator.evaluate((fraction - prevFraction) / (nextKeyframe.getFraction() - prevFraction), this.mFirstKeyframe.getValue(), nextKeyframe.getValue());
        } else if (fraction >= 1.0f) {
            nextKeyframe = (Keyframe) this.mKeyframes.get(this.mNumKeyframes - 2);
            interpolator = this.mLastKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            prevFraction = nextKeyframe.getFraction();
            return this.mEvaluator.evaluate((fraction - prevFraction) / (this.mLastKeyframe.getFraction() - prevFraction), nextKeyframe.getValue(), this.mLastKeyframe.getValue());
        } else {
            nextKeyframe = this.mFirstKeyframe;
            while (true) {
                int i2 = i;
                if (i2 >= this.mNumKeyframes) {
                    return this.mLastKeyframe.getValue();
                }
                Keyframe nextKeyframe2 = (Keyframe) this.mKeyframes.get(i2);
                if (fraction < nextKeyframe2.getFraction()) {
                    TimeInterpolator interpolator2 = nextKeyframe2.getInterpolator();
                    float prevFraction2 = nextKeyframe.getFraction();
                    float intervalFraction = (fraction - prevFraction2) / (nextKeyframe2.getFraction() - prevFraction2);
                    if (interpolator2 != null) {
                        intervalFraction = interpolator2.getInterpolation(intervalFraction);
                    }
                    return this.mEvaluator.evaluate(intervalFraction, nextKeyframe.getValue(), nextKeyframe2.getValue());
                }
                nextKeyframe = nextKeyframe2;
                i = i2 + 1;
            }
        }
    }

    public String toString() {
        String returnVal = " ";
        for (int i = 0; i < this.mNumKeyframes; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(returnVal);
            stringBuilder.append(((Keyframe) this.mKeyframes.get(i)).getValue());
            stringBuilder.append("  ");
            returnVal = stringBuilder.toString();
        }
        return returnVal;
    }
}
