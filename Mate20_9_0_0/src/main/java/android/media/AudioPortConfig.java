package android.media;

public class AudioPortConfig {
    static final int CHANNEL_MASK = 2;
    static final int FORMAT = 4;
    static final int GAIN = 8;
    static final int SAMPLE_RATE = 1;
    private final int mChannelMask;
    int mConfigMask = 0;
    private final int mFormat;
    private final AudioGainConfig mGain;
    final AudioPort mPort;
    private final int mSamplingRate;

    AudioPortConfig(AudioPort port, int samplingRate, int channelMask, int format, AudioGainConfig gain) {
        this.mPort = port;
        this.mSamplingRate = samplingRate;
        this.mChannelMask = channelMask;
        this.mFormat = format;
        this.mGain = gain;
    }

    public AudioPort port() {
        return this.mPort;
    }

    public int samplingRate() {
        return this.mSamplingRate;
    }

    public int channelMask() {
        return this.mChannelMask;
    }

    public int format() {
        return this.mFormat;
    }

    public AudioGainConfig gain() {
        return this.mGain;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{mPort:");
        stringBuilder.append(this.mPort);
        stringBuilder.append(", mSamplingRate:");
        stringBuilder.append(this.mSamplingRate);
        stringBuilder.append(", mChannelMask: ");
        stringBuilder.append(this.mChannelMask);
        stringBuilder.append(", mFormat:");
        stringBuilder.append(this.mFormat);
        stringBuilder.append(", mGain:");
        stringBuilder.append(this.mGain);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
