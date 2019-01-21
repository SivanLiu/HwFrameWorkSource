package android.os;

import android.content.Context;
import android.net.Uri;
import android.os.Parcelable.Creator;
import android.telephony.SubscriptionPlan;
import android.util.MathUtils;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Arrays;

public abstract class VibrationEffect implements Parcelable {
    public static final Creator<VibrationEffect> CREATOR = new Creator<VibrationEffect>() {
        public VibrationEffect createFromParcel(Parcel in) {
            int token = in.readInt();
            if (token == 1) {
                return new OneShot(in);
            }
            if (token == 2) {
                return new Waveform(in);
            }
            if (token == 3) {
                return new Prebaked(in);
            }
            throw new IllegalStateException("Unexpected vibration event type token in parcel.");
        }

        public VibrationEffect[] newArray(int size) {
            return new VibrationEffect[size];
        }
    };
    public static final int DEFAULT_AMPLITUDE = -1;
    public static final int EFFECT_CLICK = 0;
    public static final int EFFECT_DOUBLE_CLICK = 1;
    public static final int EFFECT_HEAVY_CLICK = 5;
    public static final int EFFECT_POP = 4;
    public static final int EFFECT_THUD = 3;
    public static final int EFFECT_TICK = 2;
    public static final int MAX_AMPLITUDE = 255;
    private static final int PARCEL_TOKEN_EFFECT = 3;
    private static final int PARCEL_TOKEN_ONE_SHOT = 1;
    private static final int PARCEL_TOKEN_WAVEFORM = 2;
    @VisibleForTesting
    public static final int[] RINGTONES = new int[]{6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};

    public static class OneShot extends VibrationEffect implements Parcelable {
        public static final Creator<OneShot> CREATOR = new Creator<OneShot>() {
            public OneShot createFromParcel(Parcel in) {
                in.readInt();
                return new OneShot(in);
            }

            public OneShot[] newArray(int size) {
                return new OneShot[size];
            }
        };
        private final int mAmplitude;
        private final long mDuration;

        public OneShot(Parcel in) {
            this.mDuration = in.readLong();
            this.mAmplitude = in.readInt();
        }

        public OneShot(long milliseconds, int amplitude) {
            this.mDuration = milliseconds;
            this.mAmplitude = amplitude;
        }

        public long getDuration() {
            return this.mDuration;
        }

        public int getAmplitude() {
            return this.mAmplitude;
        }

        public VibrationEffect scale(float gamma, int maxAmplitude) {
            return new OneShot(this.mDuration, VibrationEffect.scale(this.mAmplitude, gamma, maxAmplitude));
        }

        public OneShot resolve(int defaultAmplitude) {
            if (defaultAmplitude > 255 || defaultAmplitude < 0) {
                throw new IllegalArgumentException("Amplitude is negative or greater than MAX_AMPLITUDE");
            } else if (this.mAmplitude == -1) {
                return new OneShot(this.mDuration, defaultAmplitude);
            } else {
                return this;
            }
        }

        public void validate() {
            StringBuilder stringBuilder;
            if (this.mAmplitude < -1 || this.mAmplitude == 0 || this.mAmplitude > 255) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("amplitude must either be DEFAULT_AMPLITUDE, or between 1 and 255 inclusive (amplitude=");
                stringBuilder.append(this.mAmplitude);
                stringBuilder.append(")");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (this.mDuration <= 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("duration must be positive (duration=");
                stringBuilder.append(this.mDuration);
                stringBuilder.append(")");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof OneShot)) {
                return false;
            }
            OneShot other = (OneShot) o;
            if (other.mDuration == this.mDuration && other.mAmplitude == this.mAmplitude) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (17 + (((int) this.mDuration) * 37)) + (37 * this.mAmplitude);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("OneShot{mDuration=");
            stringBuilder.append(this.mDuration);
            stringBuilder.append(", mAmplitude=");
            stringBuilder.append(this.mAmplitude);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(1);
            out.writeLong(this.mDuration);
            out.writeInt(this.mAmplitude);
        }
    }

    public static class Prebaked extends VibrationEffect implements Parcelable {
        public static final Creator<Prebaked> CREATOR = new Creator<Prebaked>() {
            public Prebaked createFromParcel(Parcel in) {
                in.readInt();
                return new Prebaked(in);
            }

            public Prebaked[] newArray(int size) {
                return new Prebaked[size];
            }
        };
        private final int mEffectId;
        private int mEffectStrength;
        private final boolean mFallback;

        public Prebaked(Parcel in) {
            this(in.readInt(), in.readByte() != (byte) 0);
            this.mEffectStrength = in.readInt();
        }

        public Prebaked(int effectId, boolean fallback) {
            this.mEffectId = effectId;
            this.mFallback = fallback;
            this.mEffectStrength = 1;
        }

        public int getId() {
            return this.mEffectId;
        }

        public boolean shouldFallback() {
            return this.mFallback;
        }

        public long getDuration() {
            return -1;
        }

        public void setEffectStrength(int strength) {
            if (isValidEffectStrength(strength)) {
                this.mEffectStrength = strength;
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid effect strength: ");
            stringBuilder.append(strength);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public int getEffectStrength() {
            return this.mEffectStrength;
        }

        private static boolean isValidEffectStrength(int strength) {
            switch (strength) {
                case 0:
                case 1:
                case 2:
                    return true;
                default:
                    return false;
            }
        }

        public void validate() {
            StringBuilder stringBuilder;
            switch (this.mEffectId) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    break;
                default:
                    if (this.mEffectId < RINGTONES[0] || this.mEffectId > RINGTONES[RINGTONES.length - 1]) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown prebaked effect type (value=");
                        stringBuilder.append(this.mEffectId);
                        stringBuilder.append(")");
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
            }
            if (!isValidEffectStrength(this.mEffectStrength)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown prebaked effect strength (value=");
                stringBuilder.append(this.mEffectStrength);
                stringBuilder.append(")");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof Prebaked)) {
                return false;
            }
            Prebaked other = (Prebaked) o;
            if (this.mEffectId == other.mEffectId && this.mFallback == other.mFallback && this.mEffectStrength == other.mEffectStrength) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (17 + (this.mEffectId * 37)) + (37 * this.mEffectStrength);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Prebaked{mEffectId=");
            stringBuilder.append(this.mEffectId);
            stringBuilder.append(", mEffectStrength=");
            stringBuilder.append(this.mEffectStrength);
            stringBuilder.append(", mFallback=");
            stringBuilder.append(this.mFallback);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(3);
            out.writeInt(this.mEffectId);
            out.writeByte((byte) this.mFallback);
            out.writeInt(this.mEffectStrength);
        }
    }

    public static class Waveform extends VibrationEffect implements Parcelable {
        public static final Creator<Waveform> CREATOR = new Creator<Waveform>() {
            public Waveform createFromParcel(Parcel in) {
                in.readInt();
                return new Waveform(in);
            }

            public Waveform[] newArray(int size) {
                return new Waveform[size];
            }
        };
        private final int[] mAmplitudes;
        private final int mRepeat;
        private final long[] mTimings;

        public Waveform(Parcel in) {
            this(in.createLongArray(), in.createIntArray(), in.readInt());
        }

        public Waveform(long[] timings, int[] amplitudes, int repeat) {
            this.mTimings = new long[timings.length];
            System.arraycopy(timings, 0, this.mTimings, 0, timings.length);
            this.mAmplitudes = new int[amplitudes.length];
            System.arraycopy(amplitudes, 0, this.mAmplitudes, 0, amplitudes.length);
            this.mRepeat = repeat;
        }

        public long[] getTimings() {
            return this.mTimings;
        }

        public int[] getAmplitudes() {
            return this.mAmplitudes;
        }

        public int getRepeatIndex() {
            return this.mRepeat;
        }

        public long getDuration() {
            if (this.mRepeat >= 0) {
                return SubscriptionPlan.BYTES_UNLIMITED;
            }
            long duration = 0;
            for (long d : this.mTimings) {
                duration += d;
            }
            return duration;
        }

        public VibrationEffect scale(float gamma, int maxAmplitude) {
            if (gamma == 1.0f && maxAmplitude == 255) {
                return new Waveform(this.mTimings, this.mAmplitudes, this.mRepeat);
            }
            int[] scaledAmplitudes = Arrays.copyOf(this.mAmplitudes, this.mAmplitudes.length);
            for (int i = 0; i < scaledAmplitudes.length; i++) {
                scaledAmplitudes[i] = VibrationEffect.scale(scaledAmplitudes[i], gamma, maxAmplitude);
            }
            return new Waveform(this.mTimings, scaledAmplitudes, this.mRepeat);
        }

        public Waveform resolve(int defaultAmplitude) {
            if (defaultAmplitude > 255 || defaultAmplitude < 0) {
                throw new IllegalArgumentException("Amplitude is negative or greater than MAX_AMPLITUDE");
            }
            int[] resolvedAmplitudes = Arrays.copyOf(this.mAmplitudes, this.mAmplitudes.length);
            for (int i = 0; i < resolvedAmplitudes.length; i++) {
                if (resolvedAmplitudes[i] == -1) {
                    resolvedAmplitudes[i] = defaultAmplitude;
                }
            }
            return new Waveform(this.mTimings, resolvedAmplitudes, this.mRepeat);
        }

        public void validate() {
            StringBuilder stringBuilder;
            if (this.mTimings.length != this.mAmplitudes.length) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("timing and amplitude arrays must be of equal length (timings.length=");
                stringBuilder.append(this.mTimings.length);
                stringBuilder.append(", amplitudes.length=");
                stringBuilder.append(this.mAmplitudes.length);
                stringBuilder.append(")");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (hasNonZeroEntry(this.mTimings)) {
                long[] jArr = this.mTimings;
                int length = jArr.length;
                int i = 0;
                int i2 = 0;
                while (i2 < length) {
                    if (jArr[i2] >= 0) {
                        i2++;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("timings must all be >= 0 (timings=");
                        stringBuilder.append(Arrays.toString(this.mTimings));
                        stringBuilder.append(")");
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                int[] iArr = this.mAmplitudes;
                length = iArr.length;
                while (i < length) {
                    int amplitude = iArr[i];
                    if (amplitude < -1 || amplitude > 255) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("amplitudes must all be DEFAULT_AMPLITUDE or between 0 and 255 (amplitudes=");
                        stringBuilder.append(Arrays.toString(this.mAmplitudes));
                        stringBuilder.append(")");
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                    i++;
                }
                if (this.mRepeat < -1 || this.mRepeat >= this.mTimings.length) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("repeat index must be within the bounds of the timings array (timings.length=");
                    stringBuilder.append(this.mTimings.length);
                    stringBuilder.append(", index=");
                    stringBuilder.append(this.mRepeat);
                    stringBuilder.append(")");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("at least one timing must be non-zero (timings=");
                stringBuilder.append(Arrays.toString(this.mTimings));
                stringBuilder.append(")");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof Waveform)) {
                return false;
            }
            Waveform other = (Waveform) o;
            if (Arrays.equals(this.mTimings, other.mTimings) && Arrays.equals(this.mAmplitudes, other.mAmplitudes) && this.mRepeat == other.mRepeat) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return ((17 + (Arrays.hashCode(this.mTimings) * 37)) + (Arrays.hashCode(this.mAmplitudes) * 37)) + (37 * this.mRepeat);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Waveform{mTimings=");
            stringBuilder.append(Arrays.toString(this.mTimings));
            stringBuilder.append(", mAmplitudes=");
            stringBuilder.append(Arrays.toString(this.mAmplitudes));
            stringBuilder.append(", mRepeat=");
            stringBuilder.append(this.mRepeat);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(2);
            out.writeLongArray(this.mTimings);
            out.writeIntArray(this.mAmplitudes);
            out.writeInt(this.mRepeat);
        }

        private static boolean hasNonZeroEntry(long[] vals) {
            for (long val : vals) {
                if (val != 0) {
                    return true;
                }
            }
            return false;
        }
    }

    public abstract long getDuration();

    public abstract void validate();

    public static VibrationEffect createOneShot(long milliseconds, int amplitude) {
        VibrationEffect effect = new OneShot(milliseconds, amplitude);
        effect.validate();
        return effect;
    }

    public static VibrationEffect createWaveform(long[] timings, int repeat) {
        int[] amplitudes = new int[timings.length];
        for (int i = 0; i < timings.length / 2; i++) {
            amplitudes[(i * 2) + 1] = -1;
        }
        return createWaveform(timings, amplitudes, repeat);
    }

    public static VibrationEffect createWaveform(long[] timings, int[] amplitudes, int repeat) {
        VibrationEffect effect = new Waveform(timings, amplitudes, repeat);
        effect.validate();
        return effect;
    }

    public static VibrationEffect get(int effectId) {
        return get(effectId, true);
    }

    public static VibrationEffect get(int effectId, boolean fallback) {
        VibrationEffect effect = new Prebaked(effectId, fallback);
        effect.validate();
        return effect;
    }

    public static VibrationEffect get(Uri uri, Context context) {
        String[] uris = context.getResources().getStringArray(17236029);
        int i = 0;
        while (i < uris.length && i < RINGTONES.length) {
            if (uris[i] != null) {
                Uri mappedUri = context.getContentResolver().uncanonicalize(Uri.parse(uris[i]));
                if (mappedUri != null && mappedUri.equals(uri)) {
                    return get(RINGTONES[i]);
                }
            }
            i++;
        }
        return null;
    }

    public int describeContents() {
        return 0;
    }

    protected static int scale(int amplitude, float gamma, int maxAmplitude) {
        return (int) (((float) maxAmplitude) * MathUtils.pow(((float) amplitude) / 255.0f, gamma));
    }
}
