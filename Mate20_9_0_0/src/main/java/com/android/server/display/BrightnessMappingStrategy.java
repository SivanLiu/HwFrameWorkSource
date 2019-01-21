package com.android.server.display;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.BrightnessConfiguration.Builder;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.Spline;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.display.utils.Plog;
import com.android.server.os.HwBootFail;
import java.io.PrintWriter;
import java.util.Arrays;

public abstract class BrightnessMappingStrategy {
    private static final boolean DEBUG = false;
    private static final float LUX_GRAD_SMOOTHING = 0.25f;
    private static final float MAX_GRAD = 1.0f;
    private static final Plog PLOG = Plog.createSystemPlog(TAG);
    private static final String TAG = "BrightnessMappingStrategy";

    @VisibleForTesting
    static class PhysicalMappingStrategy extends BrightnessMappingStrategy {
        private float mAutoBrightnessAdjustment;
        private Spline mBacklightToNitsSpline;
        private Spline mBrightnessSpline;
        private BrightnessConfiguration mConfig;
        private final BrightnessConfiguration mDefaultConfig;
        private float mMaxGamma;
        private final Spline mNitsToBacklightSpline;
        private float mUserBrightness;
        private float mUserLux;

        public PhysicalMappingStrategy(BrightnessConfiguration config, float[] nits, int[] backlight, float maxGamma) {
            boolean z = true;
            int i = 0;
            boolean z2 = (nits.length == 0 || backlight.length == 0) ? false : true;
            Preconditions.checkArgument(z2, "Nits and backlight arrays must not be empty!");
            if (nits.length != backlight.length) {
                z = false;
            }
            Preconditions.checkArgument(z, "Nits and backlight arrays must be the same length!");
            Preconditions.checkNotNull(config);
            Preconditions.checkArrayElementsInRange(nits, 0.0f, Float.MAX_VALUE, "nits");
            Preconditions.checkArrayElementsInRange(backlight, 0, 255, "backlight");
            this.mMaxGamma = maxGamma;
            this.mAutoBrightnessAdjustment = 0.0f;
            this.mUserLux = -1.0f;
            this.mUserBrightness = -1.0f;
            int N = nits.length;
            float[] normalizedBacklight = new float[N];
            while (i < N) {
                normalizedBacklight[i] = BrightnessMappingStrategy.normalizeAbsoluteBrightness(backlight[i]);
                i++;
            }
            this.mNitsToBacklightSpline = Spline.createSpline(nits, normalizedBacklight);
            this.mBacklightToNitsSpline = Spline.createSpline(normalizedBacklight, nits);
            this.mDefaultConfig = config;
            this.mConfig = config;
            computeSpline();
        }

        public boolean setBrightnessConfiguration(BrightnessConfiguration config) {
            if (config == null) {
                config = this.mDefaultConfig;
            }
            if (config.equals(this.mConfig)) {
                return false;
            }
            this.mConfig = config;
            computeSpline();
            return true;
        }

        public float getBrightness(float lux) {
            return this.mNitsToBacklightSpline.interpolate(this.mBrightnessSpline.interpolate(lux));
        }

        public float getAutoBrightnessAdjustment() {
            return this.mAutoBrightnessAdjustment;
        }

        public boolean setAutoBrightnessAdjustment(float adjustment) {
            adjustment = MathUtils.constrain(adjustment, -1.0f, 1.0f);
            if (adjustment == this.mAutoBrightnessAdjustment) {
                return false;
            }
            this.mAutoBrightnessAdjustment = adjustment;
            computeSpline();
            return true;
        }

        public float convertToNits(int backlight) {
            return this.mBacklightToNitsSpline.interpolate(BrightnessMappingStrategy.normalizeAbsoluteBrightness(backlight));
        }

        public void addUserDataPoint(float lux, float brightness) {
            this.mAutoBrightnessAdjustment = BrightnessMappingStrategy.inferAutoBrightnessAdjustment(this.mMaxGamma, brightness, getUnadjustedBrightness(lux));
            this.mUserLux = lux;
            this.mUserBrightness = brightness;
            computeSpline();
        }

        public void clearUserDataPoints() {
            if (this.mUserLux != -1.0f) {
                this.mAutoBrightnessAdjustment = 0.0f;
                this.mUserLux = -1.0f;
                this.mUserBrightness = -1.0f;
                computeSpline();
            }
        }

        public boolean hasUserDataPoints() {
            return this.mUserLux != -1.0f;
        }

        public boolean isDefaultConfig() {
            return this.mDefaultConfig.equals(this.mConfig);
        }

        public BrightnessConfiguration getDefaultConfig() {
            return this.mDefaultConfig;
        }

        public void dump(PrintWriter pw) {
            pw.println("PhysicalMappingStrategy");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  mConfig=");
            stringBuilder.append(this.mConfig);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mBrightnessSpline=");
            stringBuilder.append(this.mBrightnessSpline);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mNitsToBacklightSpline=");
            stringBuilder.append(this.mNitsToBacklightSpline);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mMaxGamma=");
            stringBuilder.append(this.mMaxGamma);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mAutoBrightnessAdjustment=");
            stringBuilder.append(this.mAutoBrightnessAdjustment);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mUserLux=");
            stringBuilder.append(this.mUserLux);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mUserBrightness=");
            stringBuilder.append(this.mUserBrightness);
            pw.println(stringBuilder.toString());
        }

        private void computeSpline() {
            Pair<float[], float[]> defaultCurve = this.mConfig.getCurve();
            float[] defaultLux = defaultCurve.first;
            float[] defaultNits = (float[]) defaultCurve.second;
            float[] defaultBacklight = new float[defaultNits.length];
            int i = 0;
            for (int i2 = 0; i2 < defaultBacklight.length; i2++) {
                defaultBacklight[i2] = this.mNitsToBacklightSpline.interpolate(defaultNits[i2]);
            }
            Pair<float[], float[]> curve = BrightnessMappingStrategy.getAdjustedCurve(defaultLux, defaultBacklight, this.mUserLux, this.mUserBrightness, this.mAutoBrightnessAdjustment, this.mMaxGamma);
            float[] lux = curve.first;
            float[] backlight = curve.second;
            float[] nits = new float[backlight.length];
            while (true) {
                int i3 = i;
                if (i3 < nits.length) {
                    nits[i3] = this.mBacklightToNitsSpline.interpolate(backlight[i3]);
                    i = i3 + 1;
                } else {
                    this.mBrightnessSpline = Spline.createSpline(lux, nits);
                    return;
                }
            }
        }

        private float getUnadjustedBrightness(float lux) {
            Pair<float[], float[]> curve = this.mConfig.getCurve();
            return this.mNitsToBacklightSpline.interpolate(Spline.createSpline((float[]) curve.first, (float[]) curve.second).interpolate(lux));
        }
    }

    private static class SimpleMappingStrategy extends BrightnessMappingStrategy {
        private float mAutoBrightnessAdjustment;
        private final float[] mBrightness;
        private final float[] mLux;
        private float mMaxGamma;
        private Spline mSpline;
        private float mUserBrightness;
        private float mUserLux;

        public SimpleMappingStrategy(float[] lux, int[] brightness, float maxGamma) {
            boolean z = true;
            int i = 0;
            boolean z2 = (lux.length == 0 || brightness.length == 0) ? false : true;
            Preconditions.checkArgument(z2, "Lux and brightness arrays must not be empty!");
            if (lux.length != brightness.length) {
                z = false;
            }
            Preconditions.checkArgument(z, "Lux and brightness arrays must be the same length!");
            Preconditions.checkArrayElementsInRange(lux, 0.0f, Float.MAX_VALUE, "lux");
            Preconditions.checkArrayElementsInRange(brightness, 0, HwBootFail.STAGE_BOOT_SUCCESS, "brightness");
            int N = brightness.length;
            this.mLux = new float[N];
            this.mBrightness = new float[N];
            while (true) {
                int i2 = i;
                if (i2 < N) {
                    this.mLux[i2] = lux[i2];
                    this.mBrightness[i2] = BrightnessMappingStrategy.normalizeAbsoluteBrightness(brightness[i2]);
                    i = i2 + 1;
                } else {
                    this.mMaxGamma = maxGamma;
                    this.mAutoBrightnessAdjustment = 0.0f;
                    this.mUserLux = -1.0f;
                    this.mUserBrightness = -1.0f;
                    computeSpline();
                    return;
                }
            }
        }

        public boolean setBrightnessConfiguration(BrightnessConfiguration config) {
            return false;
        }

        public float getBrightness(float lux) {
            return this.mSpline.interpolate(lux);
        }

        public float getAutoBrightnessAdjustment() {
            return this.mAutoBrightnessAdjustment;
        }

        public boolean setAutoBrightnessAdjustment(float adjustment) {
            adjustment = MathUtils.constrain(adjustment, -1.0f, 1.0f);
            if (adjustment == this.mAutoBrightnessAdjustment) {
                return false;
            }
            this.mAutoBrightnessAdjustment = adjustment;
            computeSpline();
            return true;
        }

        public float convertToNits(int backlight) {
            return -1.0f;
        }

        public void addUserDataPoint(float lux, float brightness) {
            this.mAutoBrightnessAdjustment = BrightnessMappingStrategy.inferAutoBrightnessAdjustment(this.mMaxGamma, brightness, getUnadjustedBrightness(lux));
            this.mUserLux = lux;
            this.mUserBrightness = brightness;
            computeSpline();
        }

        public void clearUserDataPoints() {
            if (this.mUserLux != -1.0f) {
                this.mAutoBrightnessAdjustment = 0.0f;
                this.mUserLux = -1.0f;
                this.mUserBrightness = -1.0f;
                computeSpline();
            }
        }

        public boolean hasUserDataPoints() {
            return this.mUserLux != -1.0f;
        }

        public boolean isDefaultConfig() {
            return true;
        }

        public BrightnessConfiguration getDefaultConfig() {
            return null;
        }

        public void dump(PrintWriter pw) {
            pw.println("SimpleMappingStrategy");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  mSpline=");
            stringBuilder.append(this.mSpline);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mMaxGamma=");
            stringBuilder.append(this.mMaxGamma);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mAutoBrightnessAdjustment=");
            stringBuilder.append(this.mAutoBrightnessAdjustment);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mUserLux=");
            stringBuilder.append(this.mUserLux);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mUserBrightness=");
            stringBuilder.append(this.mUserBrightness);
            pw.println(stringBuilder.toString());
        }

        private void computeSpline() {
            Pair<float[], float[]> curve = BrightnessMappingStrategy.getAdjustedCurve(this.mLux, this.mBrightness, this.mUserLux, this.mUserBrightness, this.mAutoBrightnessAdjustment, this.mMaxGamma);
            this.mSpline = Spline.createSpline((float[]) curve.first, (float[]) curve.second);
        }

        private float getUnadjustedBrightness(float lux) {
            return Spline.createSpline(this.mLux, this.mBrightness).interpolate(lux);
        }
    }

    public abstract void addUserDataPoint(float f, float f2);

    public abstract void clearUserDataPoints();

    public abstract float convertToNits(int i);

    public abstract void dump(PrintWriter printWriter);

    public abstract float getAutoBrightnessAdjustment();

    public abstract float getBrightness(float f);

    public abstract BrightnessConfiguration getDefaultConfig();

    public abstract boolean hasUserDataPoints();

    public abstract boolean isDefaultConfig();

    public abstract boolean setAutoBrightnessAdjustment(float f);

    public abstract boolean setBrightnessConfiguration(BrightnessConfiguration brightnessConfiguration);

    public static BrightnessMappingStrategy create(Resources resources) {
        float[] luxLevels = getLuxLevels(resources.getIntArray(17235985));
        int[] brightnessLevelsBacklight = resources.getIntArray(17235984);
        float[] brightnessLevelsNits = getFloatArray(resources.obtainTypedArray(17235982));
        float autoBrightnessAdjustmentMaxGamma = resources.getFraction(2.6999636E-38f, 1, 1);
        float[] nitsRange = getFloatArray(resources.obtainTypedArray(17236032));
        int[] backlightRange = resources.getIntArray(17236031);
        if (isValidMapping(nitsRange, backlightRange) && isValidMapping(luxLevels, brightnessLevelsNits)) {
            int minimumBacklight = resources.getInteger(17694862);
            int maximumBacklight = resources.getInteger(17694861);
            if (backlightRange[0] > minimumBacklight || backlightRange[backlightRange.length - 1] < maximumBacklight) {
                Slog.w(TAG, "Screen brightness mapping does not cover whole range of available backlight values, autobrightness functionality may be impaired.");
            }
            Builder builder = new Builder();
            builder.setCurve(luxLevels, brightnessLevelsNits);
            return new PhysicalMappingStrategy(builder.build(), nitsRange, backlightRange, autoBrightnessAdjustmentMaxGamma);
        } else if (isValidMapping(luxLevels, brightnessLevelsBacklight)) {
            return new SimpleMappingStrategy(luxLevels, brightnessLevelsBacklight, autoBrightnessAdjustmentMaxGamma);
        } else {
            return null;
        }
    }

    private static float[] getLuxLevels(int[] lux) {
        float[] levels = new float[(lux.length + 1)];
        for (int i = 0; i < lux.length; i++) {
            levels[i + 1] = (float) lux[i];
        }
        return levels;
    }

    private static float[] getFloatArray(TypedArray array) {
        int N = array.length();
        float[] vals = new float[N];
        for (int i = 0; i < N; i++) {
            vals[i] = array.getFloat(i, -1.0f);
        }
        array.recycle();
        return vals;
    }

    /* JADX WARNING: Missing block: B:30:0x0058, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:33:0x005b, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isValidMapping(float[] x, float[] y) {
        if (x == null || y == null || x.length == 0 || y.length == 0 || x.length != y.length) {
            return false;
        }
        int N = x.length;
        float prevX = x[0];
        float prevY = y[0];
        if (prevX < 0.0f || prevY < 0.0f || Float.isNaN(prevX) || Float.isNaN(prevY)) {
            return false;
        }
        float prevY2 = prevY;
        prevY = prevX;
        int prevX2 = 1;
        while (prevX2 < N) {
            if (prevY >= x[prevX2] || prevY2 > y[prevX2] || Float.isNaN(x[prevX2]) || Float.isNaN(y[prevX2])) {
                return false;
            }
            prevY = x[prevX2];
            prevY2 = y[prevX2];
            prevX2++;
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:25:0x0045, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:28:0x0048, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isValidMapping(float[] x, int[] y) {
        if (x == null || y == null || x.length == 0 || y.length == 0 || x.length != y.length) {
            return false;
        }
        int N = x.length;
        float prevX = x[0];
        int prevY = y[0];
        if (prevX < 0.0f || prevY < 0 || Float.isNaN(prevX)) {
            return false;
        }
        int prevY2 = prevY;
        float prevX2 = prevX;
        int prevX3 = 1;
        while (prevX3 < N) {
            if (prevX2 >= x[prevX3] || prevY2 > y[prevX3] || Float.isNaN(x[prevX3])) {
                return false;
            }
            prevX2 = x[prevX3];
            prevY2 = y[prevX3];
            prevX3++;
        }
        return true;
    }

    private static float normalizeAbsoluteBrightness(int brightness) {
        return ((float) MathUtils.constrain(brightness, 0, 255)) / 255.0f;
    }

    private static Pair<float[], float[]> insertControlPoint(float[] luxLevels, float[] brightnessLevels, float lux, float brightness) {
        float[] newLuxLevels;
        float[] newBrightnessLevels;
        int idx = findInsertionPoint(luxLevels, lux);
        if (idx == luxLevels.length) {
            newLuxLevels = Arrays.copyOf(luxLevels, luxLevels.length + 1);
            newBrightnessLevels = Arrays.copyOf(brightnessLevels, brightnessLevels.length + 1);
            newLuxLevels[idx] = lux;
            newBrightnessLevels[idx] = brightness;
        } else if (luxLevels[idx] == lux) {
            newLuxLevels = Arrays.copyOf(luxLevels, luxLevels.length);
            newBrightnessLevels = Arrays.copyOf(brightnessLevels, brightnessLevels.length);
            newBrightnessLevels[idx] = brightness;
        } else {
            newLuxLevels = Arrays.copyOf(luxLevels, luxLevels.length + 1);
            System.arraycopy(newLuxLevels, idx, newLuxLevels, idx + 1, luxLevels.length - idx);
            newLuxLevels[idx] = lux;
            newBrightnessLevels = Arrays.copyOf(brightnessLevels, brightnessLevels.length + 1);
            System.arraycopy(newBrightnessLevels, idx, newBrightnessLevels, idx + 1, brightnessLevels.length - idx);
            newBrightnessLevels[idx] = brightness;
        }
        smoothCurve(newLuxLevels, newBrightnessLevels, idx);
        return Pair.create(newLuxLevels, newBrightnessLevels);
    }

    private static int findInsertionPoint(float[] arr, float val) {
        for (int i = 0; i < arr.length; i++) {
            if (val <= arr[i]) {
                return i;
            }
        }
        return arr.length;
    }

    private static void smoothCurve(float[] lux, float[] brightness, int idx) {
        int i;
        float currLux;
        float currBrightness;
        float newBrightness;
        float prevLux = lux[idx];
        float prevBrightness = brightness[idx];
        for (i = idx + 1; i < lux.length; i++) {
            currLux = lux[i];
            currBrightness = brightness[i];
            newBrightness = MathUtils.constrain(currBrightness, prevBrightness, permissibleRatio(currLux, prevLux) * prevBrightness);
            if (newBrightness == currBrightness) {
                break;
            }
            prevLux = currLux;
            prevBrightness = newBrightness;
            brightness[i] = newBrightness;
        }
        prevLux = lux[idx];
        prevBrightness = brightness[idx];
        i = idx - 1;
        while (i >= 0) {
            currLux = lux[i];
            currBrightness = brightness[i];
            newBrightness = MathUtils.constrain(currBrightness, permissibleRatio(currLux, prevLux) * prevBrightness, prevBrightness);
            if (newBrightness != currBrightness) {
                prevLux = currLux;
                prevBrightness = newBrightness;
                brightness[i] = newBrightness;
                i--;
            } else {
                return;
            }
        }
    }

    private static float permissibleRatio(float currLux, float prevLux) {
        return MathUtils.exp(1.0f * (MathUtils.log(currLux + LUX_GRAD_SMOOTHING) - MathUtils.log(LUX_GRAD_SMOOTHING + prevLux)));
    }

    private static float inferAutoBrightnessAdjustment(float maxGamma, float desiredBrightness, float currentBrightness) {
        float adjustment = (currentBrightness <= 0.1f || currentBrightness >= 0.9f) ? desiredBrightness - currentBrightness : desiredBrightness == 0.0f ? -1.0f : desiredBrightness == 1.0f ? 1.0f : (-MathUtils.log(MathUtils.log(desiredBrightness) / MathUtils.log(currentBrightness))) / MathUtils.log(maxGamma);
        return MathUtils.constrain(adjustment, -1.0f, 1.0f);
    }

    private static Pair<float[], float[]> getAdjustedCurve(float[] lux, float[] brightness, float userLux, float userBrightness, float adjustment, float maxGamma) {
        float[] newLux = lux;
        float[] newBrightness = Arrays.copyOf(brightness, brightness.length);
        float gamma = MathUtils.pow(maxGamma, -MathUtils.constrain(adjustment, -1.0f, 1.0f));
        if (gamma != 1.0f) {
            for (int i = 0; i < newBrightness.length; i++) {
                newBrightness[i] = MathUtils.pow(newBrightness[i], gamma);
            }
        }
        if (userLux != -1.0f) {
            Pair<float[], float[]> curve = insertControlPoint(newLux, newBrightness, userLux, userBrightness);
            newLux = (float[]) curve.first;
            newBrightness = (float[]) curve.second;
        }
        return Pair.create(newLux, newBrightness);
    }
}
