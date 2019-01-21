package android.media.audiofx;

import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

public class Equalizer extends AudioEffect {
    public static final int PARAM_BAND_FREQ_RANGE = 4;
    public static final int PARAM_BAND_LEVEL = 2;
    public static final int PARAM_CENTER_FREQ = 3;
    public static final int PARAM_CURRENT_PRESET = 6;
    public static final int PARAM_GET_BAND = 5;
    public static final int PARAM_GET_NUM_OF_PRESETS = 7;
    public static final int PARAM_GET_PRESET_NAME = 8;
    public static final int PARAM_LEVEL_RANGE = 1;
    public static final int PARAM_NUM_BANDS = 0;
    private static final int PARAM_PROPERTIES = 9;
    public static final int PARAM_STRING_SIZE_MAX = 32;
    private static final String TAG = "Equalizer";
    private BaseParameterListener mBaseParamListener = null;
    private short mNumBands = (short) 0;
    private int mNumPresets;
    private OnParameterChangeListener mParamListener = null;
    private final Object mParamListenerLock = new Object();
    private String[] mPresetNames;

    public interface OnParameterChangeListener {
        void onParameterChange(Equalizer equalizer, int i, int i2, int i3, int i4);
    }

    public static class Settings {
        public short[] bandLevels = null;
        public short curPreset;
        public short numBands = (short) 0;

        public Settings(String settings) {
            short i = (short) 0;
            StringTokenizer st = new StringTokenizer(settings, "=;");
            int tokens = st.countTokens();
            if (st.countTokens() >= 5) {
                String key = st.nextToken();
                StringBuilder stringBuilder;
                if (key.equals(Equalizer.TAG)) {
                    StringBuilder stringBuilder2;
                    try {
                        key = st.nextToken();
                        if (key.equals("curPreset")) {
                            this.curPreset = Short.parseShort(st.nextToken());
                            key = st.nextToken();
                            if (key.equals("numBands")) {
                                this.numBands = Short.parseShort(st.nextToken());
                                if (st.countTokens() == this.numBands * 2) {
                                    this.bandLevels = new short[this.numBands];
                                    while (i < this.numBands) {
                                        key = st.nextToken();
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("band");
                                        stringBuilder.append(i + 1);
                                        stringBuilder.append("Level");
                                        if (key.equals(stringBuilder.toString())) {
                                            this.bandLevels[i] = Short.parseShort(st.nextToken());
                                            i++;
                                        } else {
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("invalid key name: ");
                                            stringBuilder2.append(key);
                                            throw new IllegalArgumentException(stringBuilder2.toString());
                                        }
                                    }
                                    return;
                                }
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("settings: ");
                                stringBuilder.append(settings);
                                throw new IllegalArgumentException(stringBuilder.toString());
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("invalid key name: ");
                            stringBuilder.append(key);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("invalid key name: ");
                        stringBuilder.append(key);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    } catch (NumberFormatException e) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("invalid value for key: ");
                        stringBuilder2.append(key);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("invalid settings for Equalizer: ");
                stringBuilder.append(key);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("settings: ");
            stringBuilder3.append(settings);
            throw new IllegalArgumentException(stringBuilder3.toString());
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Equalizer;curPreset=");
            stringBuilder.append(Short.toString(this.curPreset));
            stringBuilder.append(";numBands=");
            stringBuilder.append(Short.toString(this.numBands));
            String str = new String(stringBuilder.toString());
            for (short i = (short) 0; i < this.numBands; i++) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(";band");
                stringBuilder2.append(i + 1);
                stringBuilder2.append("Level=");
                stringBuilder2.append(Short.toString(this.bandLevels[i]));
                str = str.concat(stringBuilder2.toString());
            }
            return str;
        }
    }

    private class BaseParameterListener implements android.media.audiofx.AudioEffect.OnParameterChangeListener {
        private BaseParameterListener() {
        }

        public void onParameterChange(AudioEffect effect, int status, byte[] param, byte[] value) {
            OnParameterChangeListener l = null;
            synchronized (Equalizer.this.mParamListenerLock) {
                if (Equalizer.this.mParamListener != null) {
                    l = Equalizer.this.mParamListener;
                }
            }
            if (l != null) {
                int p1 = -1;
                int p2 = -1;
                int v = -1;
                if (param.length >= 4) {
                    p1 = AudioEffect.byteArrayToInt(param, 0);
                    if (param.length >= 8) {
                        p2 = AudioEffect.byteArrayToInt(param, 4);
                    }
                }
                if (value.length == 2) {
                    v = AudioEffect.byteArrayToShort(value, 0);
                } else if (value.length == 4) {
                    v = AudioEffect.byteArrayToInt(value, 0);
                }
                int v2 = v;
                if (p1 != -1 && v2 != -1) {
                    l.onParameterChange(Equalizer.this, status, p1, p2, v2);
                }
            }
        }
    }

    public Equalizer(int priority, int audioSession) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        super(EFFECT_TYPE_EQUALIZER, EFFECT_TYPE_NULL, priority, audioSession);
        if (audioSession == 0) {
            Log.w(TAG, "WARNING: attaching an Equalizer to global output mix is deprecated!");
        }
        getNumberOfBands();
        this.mNumPresets = getNumberOfPresets();
        if (this.mNumPresets != 0) {
            this.mPresetNames = new String[this.mNumPresets];
            byte[] value = new byte[32];
            int[] param = new int[2];
            param[0] = 8;
            for (int i = 0; i < this.mNumPresets; i++) {
                param[1] = i;
                checkStatus(getParameter(param, value));
                int length = 0;
                while (value[length] != (byte) 0) {
                    length++;
                }
                try {
                    this.mPresetNames[i] = new String(value, 0, length, "ISO-8859-1");
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "preset name decode error");
                }
            }
        }
    }

    public short getNumberOfBands() throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        if (this.mNumBands != (short) 0) {
            return this.mNumBands;
        }
        short[] result = new short[1];
        checkStatus(getParameter(new int[]{0}, result));
        this.mNumBands = result[0];
        return this.mNumBands;
    }

    public short[] getBandLevelRange() throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        short[] result = new short[2];
        checkStatus(getParameter(1, result));
        return result;
    }

    public void setBandLevel(short band, short level) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        param = new int[2];
        short[] value = new short[]{2};
        param[1] = band;
        value[0] = level;
        checkStatus(setParameter(param, value));
    }

    public short getBandLevel(short band) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        param = new int[2];
        short[] result = new short[]{2};
        param[1] = band;
        checkStatus(getParameter(param, result));
        return result[0];
    }

    public int getCenterFreq(short band) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        param = new int[2];
        int[] result = new int[]{3};
        param[1] = band;
        checkStatus(getParameter(param, result));
        return result[0];
    }

    public int[] getBandFreqRange(short band) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        param = new int[2];
        int[] result = new int[]{4, band};
        checkStatus(getParameter(param, result));
        return result;
    }

    public short getBand(int frequency) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        param = new int[2];
        short[] result = new short[]{5};
        param[1] = frequency;
        checkStatus(getParameter(param, result));
        return result[0];
    }

    public short getCurrentPreset() throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        short[] result = new short[1];
        checkStatus(getParameter(6, result));
        return result[0];
    }

    public void usePreset(short preset) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        checkStatus(setParameter(6, preset));
    }

    public short getNumberOfPresets() throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        short[] result = new short[1];
        checkStatus(getParameter(7, result));
        return result[0];
    }

    public String getPresetName(short preset) {
        if (preset < (short) 0 || preset >= this.mNumPresets) {
            return "";
        }
        return this.mPresetNames[preset];
    }

    public void setParameterListener(OnParameterChangeListener listener) {
        synchronized (this.mParamListenerLock) {
            if (this.mParamListener == null) {
                this.mParamListener = listener;
                this.mBaseParamListener = new BaseParameterListener();
                super.setParameterListener(this.mBaseParamListener);
            }
        }
    }

    public Settings getProperties() throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        byte[] param = new byte[((this.mNumBands * 2) + 4)];
        checkStatus(getParameter(9, param));
        Settings settings = new Settings();
        short i = (short) 0;
        settings.curPreset = AudioEffect.byteArrayToShort(param, 0);
        settings.numBands = AudioEffect.byteArrayToShort(param, 2);
        settings.bandLevels = new short[this.mNumBands];
        while (i < this.mNumBands) {
            settings.bandLevels[i] = AudioEffect.byteArrayToShort(param, (2 * i) + 4);
            i++;
        }
        return settings;
    }

    public void setProperties(Settings settings) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        if (settings.numBands == settings.bandLevels.length && settings.numBands == this.mNumBands) {
            byte[] param = AudioEffect.concatArrays(AudioEffect.shortToByteArray(settings.curPreset), AudioEffect.shortToByteArray(this.mNumBands));
            for (short i = (short) 0; i < this.mNumBands; i++) {
                param = AudioEffect.concatArrays(param, AudioEffect.shortToByteArray(settings.bandLevels[i]));
            }
            checkStatus(setParameter(9, param));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("settings invalid band count: ");
        stringBuilder.append(settings.numBands);
        throw new IllegalArgumentException(stringBuilder.toString());
    }
}
