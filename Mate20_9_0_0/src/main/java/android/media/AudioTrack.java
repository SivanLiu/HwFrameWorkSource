package android.media;

import android.hsm.MediaTransactWrapper;
import android.media.VolumeShaper.Configuration;
import android.media.VolumeShaper.Operation;
import android.media.VolumeShaper.State;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Process;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.NioUtils;
import java.util.concurrent.Executor;

public class AudioTrack extends PlayerBase implements AudioRouting, VolumeAutomation {
    private static final int AUDIO_OUTPUT_FLAG_DEEP_BUFFER = 8;
    private static final int AUDIO_OUTPUT_FLAG_FAST = 4;
    public static final int CHANNEL_COUNT_MAX = native_get_FCC_8();
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -2;
    public static final int ERROR_DEAD_OBJECT = -6;
    public static final int ERROR_INVALID_OPERATION = -3;
    private static final int ERROR_NATIVESETUP_AUDIOSYSTEM = -16;
    private static final int ERROR_NATIVESETUP_INVALIDCHANNELMASK = -17;
    private static final int ERROR_NATIVESETUP_INVALIDFORMAT = -18;
    private static final int ERROR_NATIVESETUP_INVALIDSTREAMTYPE = -19;
    private static final int ERROR_NATIVESETUP_NATIVEINITFAILED = -20;
    public static final int ERROR_WOULD_BLOCK = -7;
    private static final float GAIN_MAX = 1.0f;
    private static final float GAIN_MIN = 0.0f;
    private static final float HEADER_V2_SIZE_BYTES = 20.0f;
    public static final int MODE_STATIC = 0;
    public static final int MODE_STREAM = 1;
    private static final int NATIVE_EVENT_MARKER = 3;
    private static final int NATIVE_EVENT_MORE_DATA = 0;
    private static final int NATIVE_EVENT_NEW_IAUDIOTRACK = 6;
    private static final int NATIVE_EVENT_NEW_POS = 4;
    private static final int NATIVE_EVENT_STREAM_END = 7;
    public static final int PERFORMANCE_MODE_LOW_LATENCY = 1;
    public static final int PERFORMANCE_MODE_NONE = 0;
    public static final int PERFORMANCE_MODE_POWER_SAVING = 2;
    public static final int PLAYSTATE_PAUSED = 2;
    public static final int PLAYSTATE_PLAYING = 3;
    public static final int PLAYSTATE_STOPPED = 1;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_NO_STATIC_DATA = 2;
    public static final int STATE_UNINITIALIZED = 0;
    public static final int SUCCESS = 0;
    private static final int SUPPORTED_OUT_CHANNELS = 7420;
    private static final String TAG = "android.media.AudioTrack";
    public static final int WRITE_BLOCKING = 0;
    public static final int WRITE_NON_BLOCKING = 1;
    private int mAudioFormat;
    private int mAvSyncBytesRemaining;
    private ByteBuffer mAvSyncHeader;
    private int mChannelConfiguration;
    private int mChannelCount;
    private int mChannelIndexMask;
    private int mChannelMask;
    private int mDataLoadMode;
    private NativePositionEventHandlerDelegate mEventHandlerDelegate;
    private final Looper mInitializationLooper;
    private long mJniData;
    private int mNativeBufferSizeInBytes;
    private int mNativeBufferSizeInFrames;
    protected long mNativeTrackInJavaObj;
    private int mOffset;
    private int mPlayState;
    private final Object mPlayStateLock;
    private AudioDeviceInfo mPreferredDevice;
    @GuardedBy("mRoutingChangeListeners")
    private ArrayMap<android.media.AudioRouting.OnRoutingChangedListener, NativeRoutingEventHandlerDelegate> mRoutingChangeListeners;
    private int mSampleRate;
    private int mSessionId;
    private int mState;
    private StreamEventCallback mStreamEventCb;
    private final Object mStreamEventCbLock;
    private Executor mStreamEventExec;
    private int mStreamType;

    public static class Builder {
        private AudioAttributes mAttributes;
        private int mBufferSizeInBytes;
        private AudioFormat mFormat;
        private int mMode = 1;
        private boolean mOffload = false;
        private int mPerformanceMode = 0;
        private int mSessionId = 0;

        public Builder setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException {
            if (attributes != null) {
                this.mAttributes = attributes;
                return this;
            }
            throw new IllegalArgumentException("Illegal null AudioAttributes argument");
        }

        public Builder setAudioFormat(AudioFormat format) throws IllegalArgumentException {
            if (format != null) {
                this.mFormat = format;
                return this;
            }
            throw new IllegalArgumentException("Illegal null AudioFormat argument");
        }

        public Builder setBufferSizeInBytes(int bufferSizeInBytes) throws IllegalArgumentException {
            if (bufferSizeInBytes > 0) {
                this.mBufferSizeInBytes = bufferSizeInBytes;
                return this;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid buffer size ");
            stringBuilder.append(bufferSizeInBytes);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public Builder setTransferMode(int mode) throws IllegalArgumentException {
            switch (mode) {
                case 0:
                case 1:
                    this.mMode = mode;
                    return this;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid transfer mode ");
                    stringBuilder.append(mode);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public Builder setSessionId(int sessionId) throws IllegalArgumentException {
            if (sessionId == 0 || sessionId >= 1) {
                this.mSessionId = sessionId;
                return this;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid audio session ID ");
            stringBuilder.append(sessionId);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public Builder setPerformanceMode(int performanceMode) {
            switch (performanceMode) {
                case 0:
                case 1:
                case 2:
                    this.mPerformanceMode = performanceMode;
                    return this;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid performance mode ");
                    stringBuilder.append(performanceMode);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public Builder setOffloadedPlayback(boolean offload) {
            this.mOffload = offload;
            return this;
        }

        /* JADX WARNING: Missing block: B:7:0x0042, code skipped:
            if (android.media.AudioTrack.access$000(r9.mAttributes, r9.mFormat, r9.mBufferSizeInBytes, r9.mMode) == false) goto L_0x0060;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public AudioTrack build() throws UnsupportedOperationException {
            if (this.mAttributes == null) {
                this.mAttributes = new android.media.AudioAttributes.Builder().setUsage(1).build();
            }
            switch (this.mPerformanceMode) {
                case 0:
                    break;
                case 1:
                    this.mAttributes = new android.media.AudioAttributes.Builder(this.mAttributes).replaceFlags((this.mAttributes.getAllFlags() | 256) & -513).build();
                    break;
                case 2:
                    this.mAttributes = new android.media.AudioAttributes.Builder(this.mAttributes).replaceFlags((this.mAttributes.getAllFlags() | 512) & -257).build();
                    break;
            }
            if (this.mFormat == null) {
                this.mFormat = new android.media.AudioFormat.Builder().setChannelMask(12).setEncoding(1).build();
            }
            if (this.mOffload) {
                if (this.mAttributes.getUsage() != 1) {
                    throw new UnsupportedOperationException("Cannot create AudioTrack, offload requires USAGE_MEDIA");
                } else if (!AudioSystem.isOffloadSupported(this.mFormat)) {
                    throw new UnsupportedOperationException("Cannot create AudioTrack, offload format not supported");
                }
            }
            try {
                if (this.mMode == 1 && this.mBufferSizeInBytes == 0) {
                    int channelCount = this.mFormat.getChannelCount();
                    AudioFormat audioFormat = this.mFormat;
                    this.mBufferSizeInBytes = channelCount * AudioFormat.getBytesPerSample(this.mFormat.getEncoding());
                }
                AudioTrack audioTrack = new AudioTrack(this.mAttributes, this.mFormat, this.mBufferSizeInBytes, this.mMode, this.mSessionId, this.mOffload, null);
                if (audioTrack.getState() != 0) {
                    return audioTrack;
                }
                throw new UnsupportedOperationException("Cannot create AudioTrack");
            } catch (IllegalArgumentException e) {
                throw new UnsupportedOperationException(e.getMessage());
            }
        }
    }

    public static final class MetricsConstants {
        public static final String CHANNELMASK = "android.media.audiorecord.channelmask";
        public static final String CONTENTTYPE = "android.media.audiotrack.type";
        public static final String SAMPLERATE = "android.media.audiorecord.samplerate";
        public static final String STREAMTYPE = "android.media.audiotrack.streamtype";
        public static final String USAGE = "android.media.audiotrack.usage";

        private MetricsConstants() {
        }
    }

    private class NativePositionEventHandlerDelegate {
        private final Handler mHandler;

        NativePositionEventHandlerDelegate(AudioTrack track, OnPlaybackPositionUpdateListener listener, Handler handler) {
            Looper looper;
            if (handler != null) {
                looper = handler.getLooper();
            } else {
                looper = AudioTrack.this.mInitializationLooper;
            }
            if (looper != null) {
                final AudioTrack audioTrack = AudioTrack.this;
                final AudioTrack audioTrack2 = track;
                final OnPlaybackPositionUpdateListener onPlaybackPositionUpdateListener = listener;
                this.mHandler = new Handler(looper) {
                    public void handleMessage(Message msg) {
                        if (audioTrack2 != null) {
                            switch (msg.what) {
                                case 3:
                                    if (onPlaybackPositionUpdateListener != null) {
                                        onPlaybackPositionUpdateListener.onMarkerReached(audioTrack2);
                                        break;
                                    }
                                    break;
                                case 4:
                                    if (onPlaybackPositionUpdateListener != null) {
                                        onPlaybackPositionUpdateListener.onPeriodicNotification(audioTrack2);
                                        break;
                                    }
                                    break;
                                default:
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Unknown native event type: ");
                                    stringBuilder.append(msg.what);
                                    AudioTrack.loge(stringBuilder.toString());
                                    break;
                            }
                        }
                    }
                };
                return;
            }
            this.mHandler = null;
        }

        Handler getHandler() {
            return this.mHandler;
        }
    }

    public interface OnPlaybackPositionUpdateListener {
        void onMarkerReached(AudioTrack audioTrack);

        void onPeriodicNotification(AudioTrack audioTrack);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PerformanceMode {
    }

    public static abstract class StreamEventCallback {
        public void onTearDown(AudioTrack track) {
        }

        public void onStreamPresentationEnd(AudioTrack track) {
        }

        public void onStreamDataRequest(AudioTrack track) {
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface TransferMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface WriteMode {
    }

    @Deprecated
    public interface OnRoutingChangedListener extends android.media.AudioRouting.OnRoutingChangedListener {
        void onRoutingChanged(AudioTrack audioTrack);

        void onRoutingChanged(AudioRouting router) {
            if (router instanceof AudioTrack) {
                onRoutingChanged((AudioTrack) router);
            }
        }
    }

    private native int native_applyVolumeShaper(Configuration configuration, Operation operation);

    private final native int native_attachAuxEffect(int i);

    private final native void native_disableDeviceCallback();

    private final native void native_enableDeviceCallback();

    private final native void native_finalize();

    private final native void native_flush();

    private native PersistableBundle native_getMetrics();

    private final native int native_getRoutedDeviceId();

    private native State native_getVolumeShaperState(int i);

    private static native int native_get_FCC_8();

    private final native int native_get_buffer_capacity_frames();

    private final native int native_get_buffer_size_frames();

    private final native int native_get_flags();

    private final native int native_get_latency();

    private final native int native_get_marker_pos();

    private static final native int native_get_min_buff_size(int i, int i2, int i3);

    private static final native int native_get_output_sample_rate(int i);

    private final native PlaybackParams native_get_playback_params();

    private final native int native_get_playback_rate();

    private final native int native_get_pos_update_period();

    private final native int native_get_position();

    private final native int native_get_timestamp(long[] jArr);

    private final native int native_get_underrun_count();

    private final native void native_pause();

    private final native int native_reload_static();

    private final native int native_setAuxEffectSendLevel(float f);

    private final native boolean native_setOutputDevice(int i);

    private final native int native_setPresentation(int i, int i2);

    private final native void native_setVolume(float f, float f2);

    private final native int native_set_buffer_size_frames(int i);

    private final native int native_set_loop(int i, int i2, int i3);

    private final native int native_set_marker_pos(int i);

    private final native void native_set_playback_params(PlaybackParams playbackParams);

    private final native int native_set_playback_rate(int i);

    private final native int native_set_pos_update_period(int i);

    private final native int native_set_position(int i);

    private final native int native_setup(Object obj, Object obj2, int[] iArr, int i, int i2, int i3, int i4, int i5, int[] iArr2, long j, boolean z);

    private final native void native_start();

    private final native void native_stop();

    private final native int native_write_byte(byte[] bArr, int i, int i2, int i3, boolean z);

    private final native int native_write_float(float[] fArr, int i, int i2, int i3, boolean z);

    private final native int native_write_native_bytes(Object obj, int i, int i2, int i3, boolean z);

    private final native int native_write_short(short[] sArr, int i, int i2, int i3, boolean z);

    public final native void native_release();

    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode) throws IllegalArgumentException {
        this(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode, 0);
    }

    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode, int sessionId) throws IllegalArgumentException {
        this(new android.media.AudioAttributes.Builder().setLegacyStreamType(streamType).build(), new android.media.AudioFormat.Builder().setChannelMask(channelConfig).setEncoding(audioFormat).setSampleRate(sampleRateInHz).build(), bufferSizeInBytes, mode, sessionId);
        PlayerBase.deprecateStreamTypeForPlayback(streamType, "AudioTrack", "AudioTrack()");
    }

    public AudioTrack(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes, int mode, int sessionId) throws IllegalArgumentException {
        this(attributes, format, bufferSizeInBytes, mode, sessionId, false);
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x0176  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x00dc  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private AudioTrack(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes, int mode, int sessionId, boolean offload) throws IllegalArgumentException {
        AudioFormat audioFormat = format;
        int i = bufferSizeInBytes;
        int i2 = sessionId;
        super(attributes, 1);
        this.mState = 0;
        this.mPlayState = 1;
        this.mPlayStateLock = new Object();
        this.mNativeBufferSizeInBytes = 0;
        this.mNativeBufferSizeInFrames = 0;
        this.mChannelCount = 1;
        this.mChannelMask = 4;
        this.mStreamType = 3;
        this.mDataLoadMode = 1;
        this.mChannelConfiguration = 4;
        this.mChannelIndexMask = 0;
        this.mSessionId = 0;
        this.mAvSyncHeader = null;
        this.mAvSyncBytesRemaining = 0;
        this.mOffset = 0;
        this.mPreferredDevice = null;
        this.mRoutingChangeListeners = new ArrayMap();
        this.mStreamEventCbLock = new Object();
        if (audioFormat != null) {
            int channelMask;
            int channelMask2;
            int i3 = mode;
            if (shouldEnablePowerSaving(this.mAttributes, audioFormat, i, i3)) {
                this.mAttributes = new android.media.AudioAttributes.Builder(this.mAttributes).replaceFlags((this.mAttributes.getAllFlags() | 512) & -257).build();
            }
            HwMediaMonitorManager.writeMediaBigData(Process.myPid(), HwMediaMonitorManager.getStreamBigDataType(AudioAttributes.toLegacyStreamType(attributes)), "AudioTrack");
            Looper myLooper = Looper.myLooper();
            Looper looper = myLooper;
            if (myLooper == null) {
                looper = Looper.getMainLooper();
            }
            Looper looper2 = looper;
            int rate = format.getSampleRate();
            if (rate == 0) {
                rate = 0;
            }
            int rate2 = rate;
            rate = 0;
            if ((format.getPropertySetMask() & 8) != 0) {
                rate = format.getChannelIndexMask();
            }
            int channelIndexMask = rate;
            if ((4 & format.getPropertySetMask()) != 0) {
                channelMask = format.getChannelMask();
            } else if (channelIndexMask == 0) {
                channelMask = 12;
            } else {
                channelMask2 = 0;
                channelMask = 1;
                if ((format.getPropertySetMask() & 1) != 0) {
                    channelMask = format.getEncoding();
                }
                audioParamCheck(rate2, channelMask2, channelIndexMask, channelMask, i3);
                this.mStreamType = -1;
                audioBuffSizeCheck(i);
                this.mInitializationLooper = looper2;
                StringBuilder stringBuilder;
                if (i2 < 0) {
                    int[] sampleRate = new int[]{this.mSampleRate};
                    int[] session = new int[]{i2};
                    WeakReference weakReference = new WeakReference(this);
                    AudioAttributes audioAttributes = this.mAttributes;
                    int i4 = this.mChannelMask;
                    int i5 = this.mChannelIndexMask;
                    int i6 = this.mAudioFormat;
                    int[] session2 = session;
                    int i7 = i6;
                    int i8 = 1;
                    channelMask = native_setup(weakReference, audioAttributes, sampleRate, i4, i5, i7, this.mNativeBufferSizeInBytes, this.mDataLoadMode, session2, 0, offload);
                    if (channelMask != 0) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error code ");
                        stringBuilder.append(channelMask);
                        stringBuilder.append(" when initializing AudioTrack.");
                        loge(stringBuilder.toString());
                        return;
                    }
                    this.mSampleRate = sampleRate[0];
                    this.mSessionId = session2[0];
                    if ((this.mAttributes.getFlags() & 16) != 0) {
                        int frameSizeInBytes;
                        if (AudioFormat.isEncodingLinearFrames(this.mAudioFormat)) {
                            frameSizeInBytes = this.mChannelCount * AudioFormat.getBytesPerSample(this.mAudioFormat);
                        } else {
                            frameSizeInBytes = i8;
                        }
                        rate = frameSizeInBytes;
                        this.mOffset = ((int) Math.ceil((double) (HEADER_V2_SIZE_BYTES / ((float) rate)))) * rate;
                    }
                    if (this.mDataLoadMode == 0) {
                        this.mState = 2;
                    } else {
                        this.mState = i8;
                    }
                    baseRegisterPlayer();
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid audio session ID: ");
                stringBuilder.append(sessionId);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            channelMask2 = channelMask;
            channelMask = 1;
            if ((format.getPropertySetMask() & 1) != 0) {
            }
            audioParamCheck(rate2, channelMask2, channelIndexMask, channelMask, i3);
            this.mStreamType = -1;
            audioBuffSizeCheck(i);
            this.mInitializationLooper = looper2;
            if (i2 < 0) {
            }
        } else {
            int i9 = i2;
            throw new IllegalArgumentException("Illegal null AudioFormat");
        }
    }

    AudioTrack(long nativeTrackInJavaObj) {
        super(new android.media.AudioAttributes.Builder().build(), 1);
        this.mState = 0;
        this.mPlayState = 1;
        this.mPlayStateLock = new Object();
        this.mNativeBufferSizeInBytes = 0;
        this.mNativeBufferSizeInFrames = 0;
        this.mChannelCount = 1;
        this.mChannelMask = 4;
        this.mStreamType = 3;
        this.mDataLoadMode = 1;
        this.mChannelConfiguration = 4;
        this.mChannelIndexMask = 0;
        this.mSessionId = 0;
        this.mAvSyncHeader = null;
        this.mAvSyncBytesRemaining = 0;
        this.mOffset = 0;
        this.mPreferredDevice = null;
        this.mRoutingChangeListeners = new ArrayMap();
        this.mStreamEventCbLock = new Object();
        this.mNativeTrackInJavaObj = 0;
        this.mJniData = 0;
        Looper myLooper = Looper.myLooper();
        Looper looper = myLooper;
        if (myLooper == null) {
            looper = Looper.getMainLooper();
        }
        this.mInitializationLooper = looper;
        if (nativeTrackInJavaObj != 0) {
            baseRegisterPlayer();
            deferred_connect(nativeTrackInJavaObj);
            return;
        }
        this.mState = 0;
    }

    void deferred_connect(long nativeTrackInJavaObj) {
        if (this.mState != 1) {
            int[] session = new int[]{0};
            int initResult = native_setup(new WeakReference(this), null, new int[]{0}, 0, 0, 0, 0, 0, session, nativeTrackInJavaObj, false);
            if (initResult != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error code ");
                stringBuilder.append(initResult);
                stringBuilder.append(" when initializing AudioTrack.");
                loge(stringBuilder.toString());
                return;
            }
            this.mSessionId = session[0];
            this.mState = 1;
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0024, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:29:0x0073, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean shouldEnablePowerSaving(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes, int mode) {
        if ((attributes != null && (attributes.getAllFlags() != 0 || attributes.getUsage() != 1 || (attributes.getContentType() != 0 && attributes.getContentType() != 2 && attributes.getContentType() != 3))) || format == null || format.getSampleRate() == 0 || !AudioFormat.isEncodingLinearPcm(format.getEncoding()) || !AudioFormat.isValidEncoding(format.getEncoding()) || format.getChannelCount() < 1 || mode != 1) {
            return false;
        }
        if (bufferSizeInBytes != 0) {
            if (((long) bufferSizeInBytes) < (((100 * ((long) format.getChannelCount())) * ((long) AudioFormat.getBytesPerSample(format.getEncoding()))) * ((long) format.getSampleRate())) / 1000) {
                return false;
            }
        }
        return true;
    }

    private void audioParamCheck(int sampleRateInHz, int channelConfig, int channelIndexMask, int audioFormat, int mode) {
        if ((sampleRateInHz < 4000 || sampleRateInHz > AudioFormat.SAMPLE_RATE_HZ_MAX) && sampleRateInHz != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(sampleRateInHz);
            stringBuilder.append("Hz is not a supported sample rate.");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.mSampleRate = sampleRateInHz;
        if (audioFormat != 13 || channelConfig == 12) {
            this.mChannelConfiguration = channelConfig;
            if (channelConfig != 12) {
                switch (channelConfig) {
                    case 1:
                    case 2:
                    case 4:
                        this.mChannelCount = 1;
                        this.mChannelMask = 4;
                        break;
                    case 3:
                        break;
                    default:
                        if (channelConfig == 0 && channelIndexMask != 0) {
                            this.mChannelCount = 0;
                            break;
                        } else if (isMultichannelConfigSupported(channelConfig)) {
                            this.mChannelMask = channelConfig;
                            this.mChannelCount = AudioFormat.channelCountFromOutChannelMask(channelConfig);
                            break;
                        } else {
                            throw new IllegalArgumentException("Unsupported channel configuration.");
                        }
                        break;
                }
            }
            this.mChannelCount = 2;
            this.mChannelMask = 12;
            this.mChannelIndexMask = channelIndexMask;
            if (this.mChannelIndexMask != 0) {
                if (((~((1 << CHANNEL_COUNT_MAX) - 1)) & channelIndexMask) == 0) {
                    int channelIndexCount = Integer.bitCount(channelIndexMask);
                    if (this.mChannelCount == 0) {
                        this.mChannelCount = channelIndexCount;
                    } else if (this.mChannelCount != channelIndexCount) {
                        throw new IllegalArgumentException("Channel count must match");
                    }
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unsupported channel index configuration ");
                stringBuilder2.append(channelIndexMask);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
            if (audioFormat == 1) {
                audioFormat = 2;
            }
            if (AudioFormat.isPublicEncoding(audioFormat)) {
                this.mAudioFormat = audioFormat;
                if ((mode == 1 || mode == 0) && (mode == 1 || AudioFormat.isEncodingLinearPcm(this.mAudioFormat))) {
                    this.mDataLoadMode = mode;
                    return;
                }
                throw new IllegalArgumentException("Invalid mode.");
            }
            throw new IllegalArgumentException("Unsupported audio encoding.");
        }
        throw new IllegalArgumentException("ENCODING_IEC61937 must be configured as CHANNEL_OUT_STEREO");
    }

    private static boolean isMultichannelConfigSupported(int channelConfig) {
        if ((channelConfig & SUPPORTED_OUT_CHANNELS) != channelConfig) {
            loge("Channel configuration features unsupported channels");
            return false;
        }
        int channelCount = AudioFormat.channelCountFromOutChannelMask(channelConfig);
        if (channelCount > CHANNEL_COUNT_MAX) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Channel configuration contains too many channels ");
            stringBuilder.append(channelCount);
            stringBuilder.append(">");
            stringBuilder.append(CHANNEL_COUNT_MAX);
            loge(stringBuilder.toString());
            return false;
        } else if ((channelConfig & 12) != 12) {
            loge("Front channels must be present in multichannel configurations");
            return false;
        } else if ((channelConfig & 192) != 0 && (channelConfig & 192) != 192) {
            loge("Rear channels can't be used independently");
            return false;
        } else if ((channelConfig & 6144) == 0 || (channelConfig & 6144) == 6144) {
            return true;
        } else {
            loge("Side channels can't be used independently");
            return false;
        }
    }

    private void audioBuffSizeCheck(int audioBufferSize) {
        int frameSizeInBytes;
        if (AudioFormat.isEncodingLinearFrames(this.mAudioFormat)) {
            frameSizeInBytes = this.mChannelCount * AudioFormat.getBytesPerSample(this.mAudioFormat);
        } else {
            frameSizeInBytes = 1;
        }
        if (audioBufferSize % frameSizeInBytes != 0 || audioBufferSize < 1) {
            throw new IllegalArgumentException("Invalid audio buffer size.");
        }
        this.mNativeBufferSizeInBytes = audioBufferSize;
        this.mNativeBufferSizeInFrames = audioBufferSize / frameSizeInBytes;
    }

    public void release() {
        try {
            stop();
        } catch (IllegalStateException e) {
        }
        baseRelease();
        native_release();
        this.mState = 0;
    }

    protected void finalize() {
        baseRelease();
        native_finalize();
    }

    public static float getMinVolume() {
        return 0.0f;
    }

    public static float getMaxVolume() {
        return 1.0f;
    }

    public int getSampleRate() {
        return this.mSampleRate;
    }

    public int getPlaybackRate() {
        return native_get_playback_rate();
    }

    public PlaybackParams getPlaybackParams() {
        return native_get_playback_params();
    }

    public int getAudioFormat() {
        return this.mAudioFormat;
    }

    public int getStreamType() {
        return this.mStreamType;
    }

    public int getChannelConfiguration() {
        return this.mChannelConfiguration;
    }

    public AudioFormat getFormat() {
        android.media.AudioFormat.Builder builder = new android.media.AudioFormat.Builder().setSampleRate(this.mSampleRate).setEncoding(this.mAudioFormat);
        if (this.mChannelConfiguration != 0) {
            builder.setChannelMask(this.mChannelConfiguration);
        }
        if (this.mChannelIndexMask != 0) {
            builder.setChannelIndexMask(this.mChannelIndexMask);
        }
        return builder.build();
    }

    public int getChannelCount() {
        return this.mChannelCount;
    }

    public int getState() {
        return this.mState;
    }

    public int getPlayState() {
        int i;
        synchronized (this.mPlayStateLock) {
            i = this.mPlayState;
        }
        return i;
    }

    public int getBufferSizeInFrames() {
        return native_get_buffer_size_frames();
    }

    public int setBufferSizeInFrames(int bufferSizeInFrames) {
        if (this.mDataLoadMode == 0 || this.mState == 0) {
            return -3;
        }
        if (bufferSizeInFrames < 0) {
            return -2;
        }
        return native_set_buffer_size_frames(bufferSizeInFrames);
    }

    public int getBufferCapacityInFrames() {
        return native_get_buffer_capacity_frames();
    }

    @Deprecated
    protected int getNativeFrameCount() {
        return native_get_buffer_capacity_frames();
    }

    public int getNotificationMarkerPosition() {
        return native_get_marker_pos();
    }

    public int getPositionNotificationPeriod() {
        return native_get_pos_update_period();
    }

    public int getPlaybackHeadPosition() {
        return native_get_position();
    }

    public int getLatency() {
        return native_get_latency();
    }

    public int getUnderrunCount() {
        return native_get_underrun_count();
    }

    public int getPerformanceMode() {
        int flags = native_get_flags();
        if ((flags & 4) != 0) {
            return 1;
        }
        if ((flags & 8) != 0) {
            return 2;
        }
        return 0;
    }

    public static int getNativeOutputSampleRate(int streamType) {
        return native_get_output_sample_rate(streamType);
    }

    public static int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat) {
        int channelCount;
        if (channelConfig != 12) {
            switch (channelConfig) {
                case 2:
                case 4:
                    channelCount = 1;
                    break;
                case 3:
                    break;
                default:
                    if (isMultichannelConfigSupported(channelConfig)) {
                        channelCount = AudioFormat.channelCountFromOutChannelMask(channelConfig);
                        break;
                    }
                    loge("getMinBufferSize(): Invalid channel configuration.");
                    return -2;
            }
        }
        channelCount = 2;
        if (!AudioFormat.isPublicEncoding(audioFormat)) {
            loge("getMinBufferSize(): Invalid audio format.");
            return -2;
        } else if (sampleRateInHz < 4000 || sampleRateInHz > AudioFormat.SAMPLE_RATE_HZ_MAX) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getMinBufferSize(): ");
            stringBuilder.append(sampleRateInHz);
            stringBuilder.append(" Hz is not a supported sample rate.");
            loge(stringBuilder.toString());
            return -2;
        } else {
            int size = native_get_min_buff_size(sampleRateInHz, channelCount, audioFormat);
            if (size > 0) {
                return size;
            }
            loge("getMinBufferSize(): error querying hardware");
            return -1;
        }
    }

    public int getAudioSessionId() {
        return this.mSessionId;
    }

    public boolean getTimestamp(AudioTimestamp timestamp) {
        if (timestamp != null) {
            long[] longArray = new long[2];
            if (native_get_timestamp(longArray) != 0) {
                return false;
            }
            timestamp.framePosition = longArray[0];
            timestamp.nanoTime = longArray[1];
            return true;
        }
        throw new IllegalArgumentException();
    }

    public int getTimestampWithStatus(AudioTimestamp timestamp) {
        if (timestamp != null) {
            long[] longArray = new long[2];
            int ret = native_get_timestamp(longArray);
            timestamp.framePosition = longArray[0];
            timestamp.nanoTime = longArray[1];
            return ret;
        }
        throw new IllegalArgumentException();
    }

    public PersistableBundle getMetrics() {
        return native_getMetrics();
    }

    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener) {
        setPlaybackPositionUpdateListener(listener, null);
    }

    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener, Handler handler) {
        if (listener != null) {
            this.mEventHandlerDelegate = new NativePositionEventHandlerDelegate(this, listener, handler);
        } else {
            this.mEventHandlerDelegate = null;
        }
    }

    private static float clampGainOrLevel(float gainOrLevel) {
        if (Float.isNaN(gainOrLevel)) {
            throw new IllegalArgumentException();
        } else if (gainOrLevel < 0.0f) {
            return 0.0f;
        } else {
            if (gainOrLevel > 1.0f) {
                return 1.0f;
            }
            return gainOrLevel;
        }
    }

    @Deprecated
    public int setStereoVolume(float leftGain, float rightGain) {
        if (this.mState == 0) {
            return -3;
        }
        baseSetVolume(leftGain, rightGain);
        return 0;
    }

    void playerSetVolume(boolean muting, float leftVolume, float rightVolume) {
        float f = 0.0f;
        leftVolume = clampGainOrLevel(muting ? 0.0f : leftVolume);
        if (!muting) {
            f = rightVolume;
        }
        native_setVolume(leftVolume, clampGainOrLevel(f));
    }

    public int setVolume(float gain) {
        return setStereoVolume(gain, gain);
    }

    int playerApplyVolumeShaper(Configuration configuration, Operation operation) {
        return native_applyVolumeShaper(configuration, operation);
    }

    State playerGetVolumeShaperState(int id) {
        return native_getVolumeShaperState(id);
    }

    public VolumeShaper createVolumeShaper(Configuration configuration) {
        return new VolumeShaper(configuration, this);
    }

    public int setPlaybackRate(int sampleRateInHz) {
        if (this.mState != 1) {
            return -3;
        }
        if (sampleRateInHz <= 0) {
            return -2;
        }
        return native_set_playback_rate(sampleRateInHz);
    }

    public void setPlaybackParams(PlaybackParams params) {
        if (params != null) {
            native_set_playback_params(params);
            return;
        }
        throw new IllegalArgumentException("params is null");
    }

    public int setNotificationMarkerPosition(int markerInFrames) {
        if (this.mState == 0) {
            return -3;
        }
        return native_set_marker_pos(markerInFrames);
    }

    public int setPositionNotificationPeriod(int periodInFrames) {
        if (this.mState == 0) {
            return -3;
        }
        return native_set_pos_update_period(periodInFrames);
    }

    public int setPlaybackHeadPosition(int positionInFrames) {
        if (this.mDataLoadMode == 1 || this.mState == 0 || getPlayState() == 3) {
            return -3;
        }
        if (positionInFrames < 0 || positionInFrames > this.mNativeBufferSizeInFrames) {
            return -2;
        }
        return native_set_position(positionInFrames);
    }

    public int setLoopPoints(int startInFrames, int endInFrames, int loopCount) {
        if (this.mDataLoadMode == 1 || this.mState == 0 || getPlayState() == 3) {
            return -3;
        }
        if (loopCount != 0 && (startInFrames < 0 || startInFrames >= this.mNativeBufferSizeInFrames || startInFrames >= endInFrames || endInFrames > this.mNativeBufferSizeInFrames)) {
            return -2;
        }
        return native_set_loop(startInFrames, endInFrames, loopCount);
    }

    public int setPresentation(AudioPresentation presentation) {
        if (presentation != null) {
            return native_setPresentation(presentation.getPresentationId(), presentation.getProgramId());
        }
        throw new IllegalArgumentException("audio presentation is null");
    }

    @Deprecated
    protected void setState(int state) {
        this.mState = state;
    }

    public void play() throws IllegalStateException {
        if (this.mState == 1) {
            final int delay = getStartDelayMs();
            if (delay == 0) {
                startImpl();
                return;
            } else {
                new Thread() {
                    public void run() {
                        try {
                            Thread.sleep((long) delay);
                        } catch (InterruptedException e) {
                            Log.w(AudioTrack.TAG, "InterruptedException when delay in play run");
                        }
                        AudioTrack.this.baseSetStartDelayMs(0);
                        try {
                            AudioTrack.this.startImpl();
                        } catch (IllegalStateException e2) {
                        }
                    }
                }.start();
                return;
            }
        }
        throw new IllegalStateException("play() called on uninitialized AudioTrack.");
    }

    private void startImpl() {
        synchronized (this.mPlayStateLock) {
            baseStart();
            native_start();
            if (3 != this.mPlayState) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[HSM] AudioTrace play() uid: ");
                stringBuilder.append(Process.myUid());
                stringBuilder.append(", pid: ");
                stringBuilder.append(Process.myPid());
                Log.d(str, stringBuilder.toString());
                MediaTransactWrapper.musicPlaying(Process.myUid(), Process.myPid());
            }
            this.mPlayState = 3;
        }
    }

    public void stop() throws IllegalStateException {
        if (this.mState == 1) {
            synchronized (this.mPlayStateLock) {
                native_stop();
                baseStop();
                if (1 != this.mPlayState) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[HSM] AudioTrace stop() uid: ");
                    stringBuilder.append(Process.myUid());
                    stringBuilder.append(", pid: ");
                    stringBuilder.append(Process.myPid());
                    Log.d(str, stringBuilder.toString());
                    MediaTransactWrapper.musicPausedOrStopped(Process.myUid(), Process.myPid());
                }
                this.mPlayState = 1;
                this.mAvSyncHeader = null;
                this.mAvSyncBytesRemaining = 0;
            }
            return;
        }
        throw new IllegalStateException("stop() called on uninitialized AudioTrack.");
    }

    public void pause() throws IllegalStateException {
        if (this.mState == 1) {
            synchronized (this.mPlayStateLock) {
                native_pause();
                basePause();
                if (2 != this.mPlayState) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[HSM] AudioTrace pause() uid: ");
                    stringBuilder.append(Process.myUid());
                    stringBuilder.append(", pid: ");
                    stringBuilder.append(Process.myPid());
                    Log.d(str, stringBuilder.toString());
                    MediaTransactWrapper.musicPausedOrStopped(Process.myUid(), Process.myPid());
                }
                this.mPlayState = 2;
            }
            return;
        }
        throw new IllegalStateException("pause() called on uninitialized AudioTrack.");
    }

    public void flush() {
        if (this.mState == 1) {
            native_flush();
            this.mAvSyncHeader = null;
            this.mAvSyncBytesRemaining = 0;
        }
    }

    public int write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        return write(audioData, offsetInBytes, sizeInBytes, 0);
    }

    public int write(byte[] audioData, int offsetInBytes, int sizeInBytes, int writeMode) {
        if (this.mState == 0 || this.mAudioFormat == 4) {
            return -3;
        }
        if (writeMode != 0 && writeMode != 1) {
            Log.e(TAG, "AudioTrack.write() called with invalid blocking mode");
            return -2;
        } else if (audioData == null || offsetInBytes < 0 || sizeInBytes < 0 || offsetInBytes + sizeInBytes < 0 || offsetInBytes + sizeInBytes > audioData.length) {
            return -2;
        } else {
            int ret = native_write_byte(audioData, offsetInBytes, sizeInBytes, this.mAudioFormat, writeMode == 0);
            if (this.mDataLoadMode == 0 && this.mState == 2 && ret > 0) {
                this.mState = 1;
            }
            return ret;
        }
    }

    public int write(short[] audioData, int offsetInShorts, int sizeInShorts) {
        return write(audioData, offsetInShorts, sizeInShorts, 0);
    }

    public int write(short[] audioData, int offsetInShorts, int sizeInShorts, int writeMode) {
        if (this.mState == 0 || this.mAudioFormat == 4) {
            return -3;
        }
        if (writeMode != 0 && writeMode != 1) {
            Log.e(TAG, "AudioTrack.write() called with invalid blocking mode");
            return -2;
        } else if (audioData == null || offsetInShorts < 0 || sizeInShorts < 0 || offsetInShorts + sizeInShorts < 0 || offsetInShorts + sizeInShorts > audioData.length) {
            return -2;
        } else {
            int ret = native_write_short(audioData, offsetInShorts, sizeInShorts, this.mAudioFormat, writeMode == 0);
            if (this.mDataLoadMode == 0 && this.mState == 2 && ret > 0) {
                this.mState = 1;
            }
            return ret;
        }
    }

    public int write(float[] audioData, int offsetInFloats, int sizeInFloats, int writeMode) {
        if (this.mState == 0) {
            Log.e(TAG, "AudioTrack.write() called in invalid state STATE_UNINITIALIZED");
            return -3;
        } else if (this.mAudioFormat != 4) {
            Log.e(TAG, "AudioTrack.write(float[] ...) requires format ENCODING_PCM_FLOAT");
            return -3;
        } else if (writeMode != 0 && writeMode != 1) {
            Log.e(TAG, "AudioTrack.write() called with invalid blocking mode");
            return -2;
        } else if (audioData == null || offsetInFloats < 0 || sizeInFloats < 0 || offsetInFloats + sizeInFloats < 0 || offsetInFloats + sizeInFloats > audioData.length) {
            Log.e(TAG, "AudioTrack.write() called with invalid array, offset, or size");
            return -2;
        } else {
            int ret = native_write_float(audioData, offsetInFloats, sizeInFloats, this.mAudioFormat, writeMode == 0);
            if (this.mDataLoadMode == 0 && this.mState == 2 && ret > 0) {
                this.mState = 1;
            }
            return ret;
        }
    }

    public int write(ByteBuffer audioData, int sizeInBytes, int writeMode) {
        if (this.mState == 0) {
            Log.e(TAG, "AudioTrack.write() called in invalid state STATE_UNINITIALIZED");
            return -3;
        } else if (writeMode != 0 && writeMode != 1) {
            Log.e(TAG, "AudioTrack.write() called with invalid blocking mode");
            return -2;
        } else if (audioData == null || sizeInBytes < 0 || sizeInBytes > audioData.remaining()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AudioTrack.write() called with invalid size (");
            stringBuilder.append(sizeInBytes);
            stringBuilder.append(") value");
            Log.e(str, stringBuilder.toString());
            return -2;
        } else {
            int ret;
            if (audioData.isDirect()) {
                ret = native_write_native_bytes(audioData, audioData.position(), sizeInBytes, this.mAudioFormat, writeMode == 0);
            } else {
                ret = native_write_byte(NioUtils.unsafeArray(audioData), audioData.position() + NioUtils.unsafeArrayOffset(audioData), sizeInBytes, this.mAudioFormat, writeMode == 0);
            }
            if (this.mDataLoadMode == 0 && this.mState == 2 && ret > 0) {
                this.mState = 1;
            }
            if (ret > 0) {
                audioData.position(audioData.position() + ret);
            }
            return ret;
        }
    }

    public int write(ByteBuffer audioData, int sizeInBytes, int writeMode, long timestamp) {
        if (this.mState == 0) {
            Log.e(TAG, "AudioTrack.write() called in invalid state STATE_UNINITIALIZED");
            return -3;
        } else if (writeMode != 0 && writeMode != 1) {
            Log.e(TAG, "AudioTrack.write() called with invalid blocking mode");
            return -2;
        } else if (this.mDataLoadMode != 1) {
            Log.e(TAG, "AudioTrack.write() with timestamp called for non-streaming mode track");
            return -3;
        } else if ((this.mAttributes.getFlags() & 16) == 0) {
            Log.d(TAG, "AudioTrack.write() called on a regular AudioTrack. Ignoring pts...");
            return write(audioData, sizeInBytes, writeMode);
        } else if (audioData == null || sizeInBytes < 0 || sizeInBytes > audioData.remaining()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AudioTrack.write() called with invalid size (");
            stringBuilder.append(sizeInBytes);
            stringBuilder.append(") value");
            Log.e(str, stringBuilder.toString());
            return -2;
        } else {
            int ret;
            if (this.mAvSyncHeader == null) {
                this.mAvSyncHeader = ByteBuffer.allocate(this.mOffset);
                this.mAvSyncHeader.order(ByteOrder.BIG_ENDIAN);
                this.mAvSyncHeader.putInt(1431633922);
            }
            if (this.mAvSyncBytesRemaining == 0) {
                this.mAvSyncHeader.putInt(4, sizeInBytes);
                this.mAvSyncHeader.putLong(8, timestamp);
                this.mAvSyncHeader.putInt(16, this.mOffset);
                this.mAvSyncHeader.position(0);
                this.mAvSyncBytesRemaining = sizeInBytes;
            }
            if (this.mAvSyncHeader.remaining() != 0) {
                ret = write(this.mAvSyncHeader, this.mAvSyncHeader.remaining(), writeMode);
                if (ret < 0) {
                    Log.e(TAG, "AudioTrack.write() could not write timestamp header!");
                    this.mAvSyncHeader = null;
                    this.mAvSyncBytesRemaining = 0;
                    return ret;
                } else if (this.mAvSyncHeader.remaining() > 0) {
                    Log.v(TAG, "AudioTrack.write() partial timestamp header written.");
                    return 0;
                }
            }
            ret = write(audioData, Math.min(this.mAvSyncBytesRemaining, sizeInBytes), writeMode);
            if (ret < 0) {
                Log.e(TAG, "AudioTrack.write() could not write audio data!");
                this.mAvSyncHeader = null;
                this.mAvSyncBytesRemaining = 0;
                return ret;
            }
            this.mAvSyncBytesRemaining -= ret;
            return ret;
        }
    }

    public int reloadStaticData() {
        if (this.mDataLoadMode == 1 || this.mState != 1) {
            return -3;
        }
        return native_reload_static();
    }

    public int attachAuxEffect(int effectId) {
        if (this.mState == 0) {
            return -3;
        }
        return native_attachAuxEffect(effectId);
    }

    public int setAuxEffectSendLevel(float level) {
        if (this.mState == 0) {
            return -3;
        }
        return baseSetAuxEffectSendLevel(level);
    }

    int playerSetAuxEffectSendLevel(boolean muting, float level) {
        return native_setAuxEffectSendLevel(clampGainOrLevel(muting ? 0.0f : level)) == 0 ? 0 : -1;
    }

    public boolean setPreferredDevice(AudioDeviceInfo deviceInfo) {
        int preferredDeviceId = 0;
        if (deviceInfo != null && !deviceInfo.isSink()) {
            return false;
        }
        if (deviceInfo != null) {
            preferredDeviceId = deviceInfo.getId();
        }
        boolean status = native_setOutputDevice(preferredDeviceId);
        if (status) {
            synchronized (this) {
                this.mPreferredDevice = deviceInfo;
            }
        }
        return status;
    }

    public AudioDeviceInfo getPreferredDevice() {
        AudioDeviceInfo audioDeviceInfo;
        synchronized (this) {
            audioDeviceInfo = this.mPreferredDevice;
        }
        return audioDeviceInfo;
    }

    public AudioDeviceInfo getRoutedDevice() {
        int deviceId = native_getRoutedDeviceId();
        if (deviceId == 0) {
            return null;
        }
        AudioDeviceInfo[] devices = AudioManager.getDevicesStatic(2);
        for (int i = 0; i < devices.length; i++) {
            if (devices[i].getId() == deviceId) {
                return devices[i];
            }
        }
        return null;
    }

    @GuardedBy("mRoutingChangeListeners")
    private void testEnableNativeRoutingCallbacksLocked() {
        if (this.mRoutingChangeListeners.size() == 0) {
            native_enableDeviceCallback();
        }
    }

    @GuardedBy("mRoutingChangeListeners")
    private void testDisableNativeRoutingCallbacksLocked() {
        if (this.mRoutingChangeListeners.size() == 0) {
            native_disableDeviceCallback();
        }
    }

    public void addOnRoutingChangedListener(android.media.AudioRouting.OnRoutingChangedListener listener, Handler handler) {
        synchronized (this.mRoutingChangeListeners) {
            if (listener != null) {
                try {
                    if (!this.mRoutingChangeListeners.containsKey(listener)) {
                        testEnableNativeRoutingCallbacksLocked();
                        ArrayMap arrayMap = this.mRoutingChangeListeners;
                        Handler handler2 = handler != null ? handler : new Handler(this.mInitializationLooper);
                        arrayMap.put(listener, new NativeRoutingEventHandlerDelegate(this, listener, handler2));
                    }
                } finally {
                }
            }
        }
    }

    public void removeOnRoutingChangedListener(android.media.AudioRouting.OnRoutingChangedListener listener) {
        synchronized (this.mRoutingChangeListeners) {
            if (this.mRoutingChangeListeners.containsKey(listener)) {
                this.mRoutingChangeListeners.remove(listener);
            }
            testDisableNativeRoutingCallbacksLocked();
        }
    }

    @Deprecated
    public void addOnRoutingChangedListener(OnRoutingChangedListener listener, Handler handler) {
        addOnRoutingChangedListener((android.media.AudioRouting.OnRoutingChangedListener) listener, handler);
    }

    @Deprecated
    public void removeOnRoutingChangedListener(OnRoutingChangedListener listener) {
        removeOnRoutingChangedListener((android.media.AudioRouting.OnRoutingChangedListener) listener);
    }

    private void broadcastRoutingChange() {
        AudioManager.resetAudioPortGeneration();
        synchronized (this.mRoutingChangeListeners) {
            for (NativeRoutingEventHandlerDelegate delegate : this.mRoutingChangeListeners.values()) {
                delegate.notifyClient();
            }
        }
    }

    public void setStreamEventCallback(Executor executor, StreamEventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null StreamEventCallback");
        } else if (executor != null) {
            synchronized (this.mStreamEventCbLock) {
                this.mStreamEventExec = executor;
                this.mStreamEventCb = eventCallback;
            }
        } else {
            throw new IllegalArgumentException("Illegal null Executor for the StreamEventCallback");
        }
    }

    public void removeStreamEventCallback() {
        synchronized (this.mStreamEventCbLock) {
            this.mStreamEventExec = null;
            this.mStreamEventCb = null;
        }
    }

    void playerStart() {
        play();
    }

    void playerPause() {
        pause();
    }

    void playerStop() {
        stop();
    }

    private static void postEventFromNative(Object audiotrack_ref, int what, int arg1, int arg2, Object obj) {
        AudioTrack track = (AudioTrack) ((WeakReference) audiotrack_ref).get();
        if (track != null) {
            if (what == 1000) {
                track.broadcastRoutingChange();
                return;
            }
            Executor exec;
            if (what == 0 || what == 6 || what == 7) {
                StreamEventCallback cb;
                synchronized (track.mStreamEventCbLock) {
                    exec = track.mStreamEventExec;
                    cb = track.mStreamEventCb;
                }
                if (exec != null && cb != null) {
                    if (what != 0) {
                        switch (what) {
                            case 6:
                                exec.execute(new -$$Lambda$AudioTrack$m_q5GeJNFuHKP4bKA5zNcUJmptg(cb, track));
                                return;
                            case 7:
                                exec.execute(new -$$Lambda$AudioTrack$om39tqtuoUKWEwKYDHE7uiykjxw(cb, track));
                                return;
                        }
                    }
                    exec.execute(new -$$Lambda$AudioTrack$RYzHLsveZX4qW27TDViuZeb3nTQ(cb, track));
                    return;
                }
                return;
            }
            NativePositionEventHandlerDelegate delegate = track.mEventHandlerDelegate;
            if (delegate != null) {
                exec = delegate.getHandler();
                if (exec != null) {
                    exec.sendMessage(exec.obtainMessage(what, arg1, arg2, obj));
                }
            }
        }
    }

    private static void logd(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
