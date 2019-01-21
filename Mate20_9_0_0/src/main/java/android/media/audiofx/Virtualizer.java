package android.media.audiofx;

import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.util.Log;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringTokenizer;

public class Virtualizer extends AudioEffect {
    private static final boolean DEBUG = false;
    public static final int PARAM_FORCE_VIRTUALIZATION_MODE = 3;
    public static final int PARAM_STRENGTH = 1;
    public static final int PARAM_STRENGTH_SUPPORTED = 0;
    public static final int PARAM_VIRTUALIZATION_MODE = 4;
    public static final int PARAM_VIRTUAL_SPEAKER_ANGLES = 2;
    private static final String TAG = "Virtualizer";
    public static final int VIRTUALIZATION_MODE_AUTO = 1;
    public static final int VIRTUALIZATION_MODE_BINAURAL = 2;
    public static final int VIRTUALIZATION_MODE_OFF = 0;
    public static final int VIRTUALIZATION_MODE_TRANSAURAL = 3;
    private BaseParameterListener mBaseParamListener = null;
    private OnParameterChangeListener mParamListener = null;
    private final Object mParamListenerLock = new Object();
    private boolean mStrengthSupported = false;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ForceVirtualizationMode {
    }

    public interface OnParameterChangeListener {
        void onParameterChange(Virtualizer virtualizer, int i, int i2, short s);
    }

    public static class Settings {
        public short strength;

        public Settings(String settings) {
            StringTokenizer st = new StringTokenizer(settings, "=;");
            int tokens = st.countTokens();
            if (st.countTokens() == 3) {
                String key = st.nextToken();
                StringBuilder stringBuilder;
                if (key.equals(Virtualizer.TAG)) {
                    try {
                        key = st.nextToken();
                        if (key.equals("strength")) {
                            this.strength = Short.parseShort(st.nextToken());
                            return;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("invalid key name: ");
                        stringBuilder.append(key);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    } catch (NumberFormatException e) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("invalid value for key: ");
                        stringBuilder2.append(key);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("invalid settings for Virtualizer: ");
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
            stringBuilder.append("Virtualizer;strength=");
            stringBuilder.append(Short.toString(this.strength));
            return new String(stringBuilder.toString());
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface VirtualizationMode {
    }

    private class BaseParameterListener implements android.media.audiofx.AudioEffect.OnParameterChangeListener {
        private BaseParameterListener() {
        }

        public void onParameterChange(AudioEffect effect, int status, byte[] param, byte[] value) {
            OnParameterChangeListener l = null;
            synchronized (Virtualizer.this.mParamListenerLock) {
                if (Virtualizer.this.mParamListener != null) {
                    l = Virtualizer.this.mParamListener;
                }
            }
            if (l != null) {
                int p = -1;
                short v = (short) -1;
                if (param.length == 4) {
                    p = AudioEffect.byteArrayToInt(param, 0);
                }
                if (value.length == 2) {
                    v = AudioEffect.byteArrayToShort(value, 0);
                }
                if (p != -1 && v != (short) -1) {
                    l.onParameterChange(Virtualizer.this, status, p, v);
                }
            }
        }
    }

    public Virtualizer(int priority, int audioSession) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        super(EFFECT_TYPE_VIRTUALIZER, EFFECT_TYPE_NULL, priority, audioSession);
        boolean z = false;
        if (audioSession == 0) {
            Log.w(TAG, "WARNING: attaching a Virtualizer to global output mix is deprecated!");
        }
        int[] value = new int[1];
        checkStatus(getParameter(0, value));
        if (value[0] != 0) {
            z = true;
        }
        this.mStrengthSupported = z;
    }

    public boolean getStrengthSupported() {
        return this.mStrengthSupported;
    }

    public void setStrength(short strength) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        checkStatus(setParameter(1, strength));
    }

    public short getRoundedStrength() throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        short[] value = new short[1];
        checkStatus(getParameter(1, value));
        return value[0];
    }

    private boolean getAnglesInt(int inputChannelMask, int deviceType, int[] angles) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        if (inputChannelMask != 0) {
            int channelMask = inputChannelMask == 1 ? 12 : inputChannelMask;
            int nbChannels = AudioFormat.channelCountFromOutChannelMask(channelMask);
            if (angles == null || angles.length >= nbChannels * 3) {
                ByteBuffer paramsConverter = ByteBuffer.allocate(12);
                paramsConverter.order(ByteOrder.nativeOrder());
                paramsConverter.putInt(2);
                paramsConverter.putInt(AudioFormat.convertChannelOutMaskToNativeMask(channelMask));
                paramsConverter.putInt(AudioDeviceInfo.convertDeviceTypeToInternalDevice(deviceType));
                byte[] result = new byte[((nbChannels * 4) * 3)];
                int status = getParameter((byte[]) paramsConverter.array(), result);
                int i = 0;
                if (status >= 0) {
                    if (angles != null) {
                        ByteBuffer resultConverter = ByteBuffer.wrap(result);
                        resultConverter.order(ByteOrder.nativeOrder());
                        while (i < nbChannels) {
                            angles[3 * i] = AudioFormat.convertNativeChannelMaskToOutMask(resultConverter.getInt((i * 4) * 3));
                            angles[(3 * i) + 1] = resultConverter.getInt(((i * 4) * 3) + 4);
                            angles[(3 * i) + 2] = resultConverter.getInt(((i * 4) * 3) + 8);
                            i++;
                        }
                    }
                    return true;
                } else if (status == -4) {
                    return false;
                } else {
                    checkStatus(status);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unexpected status code ");
                    stringBuilder.append(status);
                    stringBuilder.append(" after getParameter(PARAM_VIRTUAL_SPEAKER_ANGLES)");
                    Log.e(str, stringBuilder.toString());
                    return false;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Size of array for angles cannot accomodate number of channels in mask (");
            stringBuilder2.append(nbChannels);
            stringBuilder2.append(")");
            Log.e(TAG, stringBuilder2.toString());
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Virtualizer: array for channel / angle pairs is too small: is ");
            stringBuilder3.append(angles.length);
            stringBuilder3.append(", should be ");
            stringBuilder3.append(nbChannels * 3);
            throw new IllegalArgumentException(stringBuilder3.toString());
        }
        throw new IllegalArgumentException("Virtualizer: illegal CHANNEL_INVALID channel mask");
    }

    private static int getDeviceForModeQuery(int virtualizationMode) throws IllegalArgumentException {
        switch (virtualizationMode) {
            case 2:
                return 4;
            case 3:
                return 2;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Virtualizer: illegal virtualization mode ");
                stringBuilder.append(virtualizationMode);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static int getDeviceForModeForce(int virtualizationMode) throws IllegalArgumentException {
        if (virtualizationMode == 1) {
            return 0;
        }
        return getDeviceForModeQuery(virtualizationMode);
    }

    private static int deviceToMode(int deviceType) {
        if (deviceType != 19) {
            if (deviceType != 22) {
                switch (deviceType) {
                    case 1:
                    case 3:
                    case 4:
                    case 7:
                        break;
                    case 2:
                    case 5:
                    case 6:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                        break;
                    default:
                        return 0;
                }
            }
            return 2;
        }
        return 3;
    }

    public boolean canVirtualize(int inputChannelMask, int virtualizationMode) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        return getAnglesInt(inputChannelMask, getDeviceForModeQuery(virtualizationMode), null);
    }

    public boolean getSpeakerAngles(int inputChannelMask, int virtualizationMode, int[] angles) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        if (angles != null) {
            return getAnglesInt(inputChannelMask, getDeviceForModeQuery(virtualizationMode), angles);
        }
        throw new IllegalArgumentException("Virtualizer: illegal null channel / angle array");
    }

    public boolean forceVirtualizationMode(int virtualizationMode) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        int status = setParameter(3, AudioDeviceInfo.convertDeviceTypeToInternalDevice(getDeviceForModeForce(virtualizationMode)));
        if (status >= 0) {
            return true;
        }
        if (status == -4) {
            return false;
        }
        checkStatus(status);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unexpected status code ");
        stringBuilder.append(status);
        stringBuilder.append(" after setParameter(PARAM_FORCE_VIRTUALIZATION_MODE)");
        Log.e(str, stringBuilder.toString());
        return false;
    }

    public int getVirtualizationMode() throws IllegalStateException, UnsupportedOperationException {
        int[] value = new int[1];
        int status = getParameter(4, value);
        if (status >= 0) {
            return deviceToMode(AudioDeviceInfo.convertInternalDeviceToDeviceType(value[0]));
        }
        if (status == -4) {
            return 0;
        }
        checkStatus(status);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unexpected status code ");
        stringBuilder.append(status);
        stringBuilder.append(" after getParameter(PARAM_VIRTUALIZATION_MODE)");
        Log.e(str, stringBuilder.toString());
        return 0;
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
        Settings settings = new Settings();
        short[] value = new short[1];
        checkStatus(getParameter(1, value));
        settings.strength = value[0];
        return settings;
    }

    public void setProperties(Settings settings) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        checkStatus(setParameter(1, settings.strength));
    }
}
