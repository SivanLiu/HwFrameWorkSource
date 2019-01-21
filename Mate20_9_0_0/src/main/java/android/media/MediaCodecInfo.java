package android.media;

import android.aps.IApsManager;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothHealth;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.fingerprint.FingerprintManager;
import android.mtp.MtpConstants;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MediaCodecInfo {
    private static final Range<Integer> BITRATE_RANGE = Range.create(Integer.valueOf(0), Integer.valueOf(500000000));
    private static final int DEFAULT_MAX_SUPPORTED_INSTANCES = 32;
    private static final int ERROR_NONE_SUPPORTED = 4;
    private static final int ERROR_UNRECOGNIZED = 1;
    private static final int ERROR_UNSUPPORTED = 2;
    private static final Range<Integer> FRAME_RATE_RANGE = Range.create(Integer.valueOf(0), Integer.valueOf(960));
    private static final int MAX_SUPPORTED_INSTANCES_LIMIT = 256;
    private static final Range<Integer> POSITIVE_INTEGERS = Range.create(Integer.valueOf(1), Integer.valueOf(Integer.MAX_VALUE));
    private static final Range<Long> POSITIVE_LONGS = Range.create(Long.valueOf(1), Long.valueOf(Long.MAX_VALUE));
    private static final Range<Rational> POSITIVE_RATIONALS = Range.create(new Rational(1, Integer.MAX_VALUE), new Rational(Integer.MAX_VALUE, 1));
    private static final Range<Integer> SIZE_RANGE = Range.create(Integer.valueOf(1), Integer.valueOf(32768));
    private Map<String, CodecCapabilities> mCaps = new HashMap();
    private boolean mIsEncoder;
    private String mName;

    public static final class AudioCapabilities {
        private static final int MAX_INPUT_CHANNEL_COUNT = 30;
        private static final String TAG = "AudioCapabilities";
        private Range<Integer> mBitrateRange;
        private int mMaxInputChannelCount;
        private CodecCapabilities mParent;
        private Range<Integer>[] mSampleRateRanges;
        private int[] mSampleRates;

        public Range<Integer> getBitrateRange() {
            return this.mBitrateRange;
        }

        public int[] getSupportedSampleRates() {
            return Arrays.copyOf(this.mSampleRates, this.mSampleRates.length);
        }

        public Range<Integer>[] getSupportedSampleRateRanges() {
            return (Range[]) Arrays.copyOf(this.mSampleRateRanges, this.mSampleRateRanges.length);
        }

        public int getMaxInputChannelCount() {
            return this.mMaxInputChannelCount;
        }

        private AudioCapabilities() {
        }

        public static AudioCapabilities create(MediaFormat info, CodecCapabilities parent) {
            AudioCapabilities caps = new AudioCapabilities();
            caps.init(info, parent);
            return caps;
        }

        private void init(MediaFormat info, CodecCapabilities parent) {
            this.mParent = parent;
            initWithPlatformLimits();
            applyLevelLimits();
            parseFromInfo(info);
        }

        private void initWithPlatformLimits() {
            this.mBitrateRange = Range.create(Integer.valueOf(0), Integer.valueOf(Integer.MAX_VALUE));
            this.mMaxInputChannelCount = 30;
            this.mSampleRateRanges = new Range[]{Range.create(Integer.valueOf(8000), Integer.valueOf(96000))};
            this.mSampleRates = null;
        }

        private boolean supports(Integer sampleRate, Integer inputChannels) {
            if (inputChannels == null || (inputChannels.intValue() >= 1 && inputChannels.intValue() <= this.mMaxInputChannelCount)) {
                return sampleRate == null || Utils.binarySearchDistinctRanges(this.mSampleRateRanges, sampleRate) >= 0;
            } else {
                return false;
            }
        }

        public boolean isSampleRateSupported(int sampleRate) {
            return supports(Integer.valueOf(sampleRate), null);
        }

        private void limitSampleRates(int[] rates) {
            Arrays.sort(rates);
            ArrayList<Range<Integer>> ranges = new ArrayList();
            for (int rate : rates) {
                if (supports(Integer.valueOf(rate), null)) {
                    ranges.add(Range.create(Integer.valueOf(rate), Integer.valueOf(rate)));
                }
            }
            this.mSampleRateRanges = (Range[]) ranges.toArray(new Range[ranges.size()]);
            createDiscreteSampleRates();
        }

        private void createDiscreteSampleRates() {
            this.mSampleRates = new int[this.mSampleRateRanges.length];
            for (int i = 0; i < this.mSampleRateRanges.length; i++) {
                this.mSampleRates[i] = ((Integer) this.mSampleRateRanges[i].getLower()).intValue();
            }
        }

        private void limitSampleRates(Range<Integer>[] rateRanges) {
            Utils.sortDistinctRanges(rateRanges);
            this.mSampleRateRanges = Utils.intersectSortedDistinctRanges(this.mSampleRateRanges, rateRanges);
            Range[] rangeArr = this.mSampleRateRanges;
            int length = rangeArr.length;
            int i = 0;
            while (i < length) {
                Range<Integer> range = rangeArr[i];
                if (((Integer) range.getLower()).equals(range.getUpper())) {
                    i++;
                } else {
                    this.mSampleRates = null;
                    return;
                }
            }
            createDiscreteSampleRates();
        }

        private void applyLevelLimits() {
            int[] sampleRates = null;
            Range<Integer> sampleRateRange = null;
            Range<Integer> bitRates = null;
            int maxChannels = 30;
            String mime = this.mParent.getMimeType();
            if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_MPEG)) {
                sampleRates = new int[]{8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000};
                bitRates = Range.create(Integer.valueOf(8000), Integer.valueOf(320000));
                maxChannels = 2;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_NB)) {
                sampleRates = new int[]{8000};
                bitRates = Range.create(Integer.valueOf(4750), Integer.valueOf(12200));
                maxChannels = 1;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_WB)) {
                sampleRates = new int[]{16000};
                bitRates = Range.create(Integer.valueOf(6600), Integer.valueOf(23850));
                maxChannels = 1;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                sampleRates = new int[]{7350, 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000, 64000, 88200, 96000};
                bitRates = Range.create(Integer.valueOf(8000), Integer.valueOf(510000));
                maxChannels = 48;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_VORBIS)) {
                bitRates = Range.create(Integer.valueOf(32000), Integer.valueOf(500000));
                sampleRateRange = Range.create(Integer.valueOf(8000), Integer.valueOf(AudioFormat.SAMPLE_RATE_HZ_MAX));
                maxChannels = 255;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_OPUS)) {
                bitRates = Range.create(Integer.valueOf(BluetoothHealth.HEALTH_OPERATION_SUCCESS), Integer.valueOf(510000));
                sampleRates = new int[]{8000, 12000, 16000, 24000, 48000};
                maxChannels = 255;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_RAW)) {
                sampleRateRange = Range.create(Integer.valueOf(1), Integer.valueOf(96000));
                bitRates = Range.create(Integer.valueOf(1), Integer.valueOf(10000000));
                maxChannels = AudioTrack.CHANNEL_COUNT_MAX;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_FLAC)) {
                sampleRateRange = Range.create(Integer.valueOf(1), Integer.valueOf(655350));
                maxChannels = 255;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_ALAW) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_MLAW)) {
                sampleRates = new int[]{8000};
                bitRates = Range.create(Integer.valueOf(64000), Integer.valueOf(64000));
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_MSGSM)) {
                sampleRates = new int[]{8000};
                bitRates = Range.create(Integer.valueOf(13000), Integer.valueOf(13000));
                maxChannels = 1;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AC3)) {
                maxChannels = 6;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_EAC3)) {
                maxChannels = 16;
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported mime ");
                stringBuilder.append(mime);
                Log.w(str, stringBuilder.toString());
                CodecCapabilities codecCapabilities = this.mParent;
                codecCapabilities.mError |= 2;
            }
            if (sampleRates != null) {
                limitSampleRates(sampleRates);
            } else if (sampleRateRange != null) {
                limitSampleRates(new Range[]{sampleRateRange});
            }
            applyLimits(maxChannels, bitRates);
        }

        private void applyLimits(int maxInputChannels, Range<Integer> bitRates) {
            this.mMaxInputChannelCount = ((Integer) Range.create(Integer.valueOf(1), Integer.valueOf(this.mMaxInputChannelCount)).clamp(Integer.valueOf(maxInputChannels))).intValue();
            if (bitRates != null) {
                this.mBitrateRange = this.mBitrateRange.intersect(bitRates);
            }
        }

        private void parseFromInfo(MediaFormat info) {
            int maxInputChannels = 30;
            Range<Integer> bitRates = MediaCodecInfo.POSITIVE_INTEGERS;
            if (info.containsKey("sample-rate-ranges")) {
                String[] rateStrings = info.getString("sample-rate-ranges").split(",");
                Range[] rateRanges = new Range[rateStrings.length];
                for (int i = 0; i < rateStrings.length; i++) {
                    rateRanges[i] = Utils.parseIntRange(rateStrings[i], null);
                }
                limitSampleRates(rateRanges);
            }
            if (info.containsKey("max-channel-count")) {
                maxInputChannels = Utils.parseIntSafely(info.getString("max-channel-count"), 30);
            } else if ((this.mParent.mError & 2) != 0) {
                maxInputChannels = 0;
            }
            if (info.containsKey("bitrate-range")) {
                bitRates = bitRates.intersect(Utils.parseIntRange(info.getString("bitrate-range"), bitRates));
            }
            applyLimits(maxInputChannels, bitRates);
        }

        public void getDefaultFormat(MediaFormat format) {
            if (((Integer) this.mBitrateRange.getLower()).equals(this.mBitrateRange.getUpper())) {
                format.setInteger(MediaFormat.KEY_BIT_RATE, ((Integer) this.mBitrateRange.getLower()).intValue());
            }
            if (this.mMaxInputChannelCount == 1) {
                format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            }
            if (this.mSampleRates != null && this.mSampleRates.length == 1) {
                format.setInteger(MediaFormat.KEY_SAMPLE_RATE, this.mSampleRates[0]);
            }
        }

        public boolean supportsFormat(MediaFormat format) {
            Map<String, Object> map = format.getMap();
            if (supports((Integer) map.get(MediaFormat.KEY_SAMPLE_RATE), (Integer) map.get(MediaFormat.KEY_CHANNEL_COUNT)) && CodecCapabilities.supportsBitrate(this.mBitrateRange, format)) {
                return true;
            }
            return false;
        }
    }

    public static final class CodecCapabilities {
        public static final int COLOR_Format12bitRGB444 = 3;
        public static final int COLOR_Format16bitARGB1555 = 5;
        public static final int COLOR_Format16bitARGB4444 = 4;
        public static final int COLOR_Format16bitBGR565 = 7;
        public static final int COLOR_Format16bitRGB565 = 6;
        public static final int COLOR_Format18BitBGR666 = 41;
        public static final int COLOR_Format18bitARGB1665 = 9;
        public static final int COLOR_Format18bitRGB666 = 8;
        public static final int COLOR_Format19bitARGB1666 = 10;
        public static final int COLOR_Format24BitABGR6666 = 43;
        public static final int COLOR_Format24BitARGB6666 = 42;
        public static final int COLOR_Format24bitARGB1887 = 13;
        public static final int COLOR_Format24bitBGR888 = 12;
        public static final int COLOR_Format24bitRGB888 = 11;
        public static final int COLOR_Format25bitARGB1888 = 14;
        public static final int COLOR_Format32bitABGR8888 = 2130747392;
        public static final int COLOR_Format32bitARGB8888 = 16;
        public static final int COLOR_Format32bitBGRA8888 = 15;
        public static final int COLOR_Format8bitRGB332 = 2;
        public static final int COLOR_FormatCbYCrY = 27;
        public static final int COLOR_FormatCrYCbY = 28;
        public static final int COLOR_FormatL16 = 36;
        public static final int COLOR_FormatL2 = 33;
        public static final int COLOR_FormatL24 = 37;
        public static final int COLOR_FormatL32 = 38;
        public static final int COLOR_FormatL4 = 34;
        public static final int COLOR_FormatL8 = 35;
        public static final int COLOR_FormatMonochrome = 1;
        public static final int COLOR_FormatRGBAFlexible = 2134288520;
        public static final int COLOR_FormatRGBFlexible = 2134292616;
        public static final int COLOR_FormatRawBayer10bit = 31;
        public static final int COLOR_FormatRawBayer8bit = 30;
        public static final int COLOR_FormatRawBayer8bitcompressed = 32;
        public static final int COLOR_FormatSurface = 2130708361;
        public static final int COLOR_FormatYCbYCr = 25;
        public static final int COLOR_FormatYCrYCb = 26;
        public static final int COLOR_FormatYUV411PackedPlanar = 18;
        public static final int COLOR_FormatYUV411Planar = 17;
        public static final int COLOR_FormatYUV420Flexible = 2135033992;
        public static final int COLOR_FormatYUV420PackedPlanar = 20;
        public static final int COLOR_FormatYUV420PackedSemiPlanar = 39;
        public static final int COLOR_FormatYUV420Planar = 19;
        public static final int COLOR_FormatYUV420SemiPlanar = 21;
        public static final int COLOR_FormatYUV422Flexible = 2135042184;
        public static final int COLOR_FormatYUV422PackedPlanar = 23;
        public static final int COLOR_FormatYUV422PackedSemiPlanar = 40;
        public static final int COLOR_FormatYUV422Planar = 22;
        public static final int COLOR_FormatYUV422SemiPlanar = 24;
        public static final int COLOR_FormatYUV444Flexible = 2135181448;
        public static final int COLOR_FormatYUV444Interleaved = 29;
        public static final int COLOR_QCOM_FormatYUV420SemiPlanar = 2141391872;
        public static final int COLOR_TI_FormatYUV420PackedSemiPlanar = 2130706688;
        public static final String FEATURE_AdaptivePlayback = "adaptive-playback";
        public static final String FEATURE_IntraRefresh = "intra-refresh";
        public static final String FEATURE_PartialFrame = "partial-frame";
        public static final String FEATURE_SecurePlayback = "secure-playback";
        public static final String FEATURE_TunneledPlayback = "tunneled-playback";
        private static final String TAG = "CodecCapabilities";
        private static final Feature[] decoderFeatures = new Feature[]{new Feature(FEATURE_AdaptivePlayback, 1, true), new Feature(FEATURE_SecurePlayback, 2, false), new Feature(FEATURE_TunneledPlayback, 4, false), new Feature(FEATURE_PartialFrame, 8, false)};
        private static final Feature[] encoderFeatures = new Feature[]{new Feature(FEATURE_IntraRefresh, 1, false)};
        public int[] colorFormats;
        private AudioCapabilities mAudioCaps;
        private MediaFormat mCapabilitiesInfo;
        private MediaFormat mDefaultFormat;
        private EncoderCapabilities mEncoderCaps;
        int mError;
        private int mFlagsRequired;
        private int mFlagsSupported;
        private int mFlagsVerified;
        private int mMaxSupportedInstances;
        private String mMime;
        private VideoCapabilities mVideoCaps;
        public CodecProfileLevel[] profileLevels;

        public final boolean isFeatureSupported(String name) {
            return checkFeature(name, this.mFlagsSupported);
        }

        public final boolean isFeatureRequired(String name) {
            return checkFeature(name, this.mFlagsRequired);
        }

        public String[] validFeatures() {
            Feature[] features = getValidFeatures();
            String[] res = new String[features.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = features[i].mName;
            }
            return res;
        }

        private Feature[] getValidFeatures() {
            if (isEncoder()) {
                return encoderFeatures;
            }
            return decoderFeatures;
        }

        private boolean checkFeature(String name, int flags) {
            boolean z = false;
            for (Feature feat : getValidFeatures()) {
                if (feat.mName.equals(name)) {
                    if ((feat.mValue & flags) != 0) {
                        z = true;
                    }
                    return z;
                }
            }
            return false;
        }

        public boolean isRegular() {
            for (Feature feat : getValidFeatures()) {
                if (!feat.mDefault && isFeatureRequired(feat.mName)) {
                    return false;
                }
            }
            return true;
        }

        public final boolean isFormatSupported(MediaFormat format) {
            Map<String, Object> map = format.getMap();
            String mime = (String) map.get(MediaFormat.KEY_MIME);
            if (mime != null && !this.mMime.equalsIgnoreCase(mime)) {
                return false;
            }
            for (Feature feat : getValidFeatures()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaFormat.KEY_FEATURE_);
                stringBuilder.append(feat.mName);
                Integer yesNo = (Integer) map.get(stringBuilder.toString());
                if (yesNo != null && ((yesNo.intValue() == 1 && !isFeatureSupported(feat.mName)) || (yesNo.intValue() == 0 && isFeatureRequired(feat.mName)))) {
                    return false;
                }
            }
            Integer profile = (Integer) map.get(MediaFormat.KEY_PROFILE);
            Integer level = (Integer) map.get(MediaFormat.KEY_LEVEL);
            if (profile != null) {
                if (!supportsProfileLevel(profile.intValue(), level)) {
                    return false;
                }
                int maxLevel = 0;
                for (CodecProfileLevel pl : this.profileLevels) {
                    if (pl.profile == profile.intValue() && pl.level > maxLevel) {
                        maxLevel = pl.level;
                    }
                }
                CodecCapabilities levelCaps = createFromProfileLevel(this.mMime, profile.intValue(), maxLevel);
                Map<String, Object> mapWithoutProfile = new HashMap(map);
                mapWithoutProfile.remove(MediaFormat.KEY_PROFILE);
                MediaFormat formatWithoutProfile = new MediaFormat(mapWithoutProfile);
                if (!(levelCaps == null || levelCaps.isFormatSupported(formatWithoutProfile))) {
                    return false;
                }
            }
            if (this.mAudioCaps != null && !this.mAudioCaps.supportsFormat(format)) {
                return false;
            }
            if (this.mVideoCaps != null && !this.mVideoCaps.supportsFormat(format)) {
                return false;
            }
            if (this.mEncoderCaps == null || this.mEncoderCaps.supportsFormat(format)) {
                return true;
            }
            return false;
        }

        private static boolean supportsBitrate(Range<Integer> bitrateRange, MediaFormat format) {
            Map<String, Object> map = format.getMap();
            Integer maxBitrate = (Integer) map.get(MediaFormat.KEY_MAX_BIT_RATE);
            Integer bitrate = (Integer) map.get(MediaFormat.KEY_BIT_RATE);
            if (bitrate == null) {
                bitrate = maxBitrate;
            } else if (maxBitrate != null) {
                bitrate = Integer.valueOf(Math.max(bitrate.intValue(), maxBitrate.intValue()));
            }
            if (bitrate == null || bitrate.intValue() <= 0) {
                return true;
            }
            return bitrateRange.contains(bitrate);
        }

        private boolean supportsProfileLevel(int profile, Integer level) {
            boolean z = false;
            for (CodecProfileLevel pl : this.profileLevels) {
                if (pl.profile == profile) {
                    if (level == null || this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                        return true;
                    }
                    if ((!this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_H263) || pl.level == level.intValue() || pl.level != 16 || level.intValue() <= 1) && (!this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_MPEG4) || pl.level == level.intValue() || pl.level != 4 || level.intValue() <= 1)) {
                        if (this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                            boolean supportsHighTier = (pl.level & 44739242) != 0;
                            if (((44739242 & level.intValue()) != 0) && !supportsHighTier) {
                            }
                        }
                        if (pl.level >= level.intValue()) {
                            if (createFromProfileLevel(this.mMime, profile, pl.level) == null) {
                                return true;
                            }
                            if (createFromProfileLevel(this.mMime, profile, level.intValue()) != null) {
                                z = true;
                            }
                            return z;
                        }
                    }
                }
            }
            return false;
        }

        public MediaFormat getDefaultFormat() {
            return this.mDefaultFormat;
        }

        public String getMimeType() {
            return this.mMime;
        }

        public int getMaxSupportedInstances() {
            return this.mMaxSupportedInstances;
        }

        private boolean isAudio() {
            return this.mAudioCaps != null;
        }

        public AudioCapabilities getAudioCapabilities() {
            return this.mAudioCaps;
        }

        private boolean isEncoder() {
            return this.mEncoderCaps != null;
        }

        public EncoderCapabilities getEncoderCapabilities() {
            return this.mEncoderCaps;
        }

        private boolean isVideo() {
            return this.mVideoCaps != null;
        }

        public VideoCapabilities getVideoCapabilities() {
            return this.mVideoCaps;
        }

        public CodecCapabilities dup() {
            CodecCapabilities caps = new CodecCapabilities();
            caps.profileLevels = (CodecProfileLevel[]) Arrays.copyOf(this.profileLevels, this.profileLevels.length);
            caps.colorFormats = Arrays.copyOf(this.colorFormats, this.colorFormats.length);
            caps.mMime = this.mMime;
            caps.mMaxSupportedInstances = this.mMaxSupportedInstances;
            caps.mFlagsRequired = this.mFlagsRequired;
            caps.mFlagsSupported = this.mFlagsSupported;
            caps.mFlagsVerified = this.mFlagsVerified;
            caps.mAudioCaps = this.mAudioCaps;
            caps.mVideoCaps = this.mVideoCaps;
            caps.mEncoderCaps = this.mEncoderCaps;
            caps.mDefaultFormat = this.mDefaultFormat;
            caps.mCapabilitiesInfo = this.mCapabilitiesInfo;
            return caps;
        }

        public static CodecCapabilities createFromProfileLevel(String mime, int profile, int level) {
            CodecProfileLevel pl = new CodecProfileLevel();
            pl.profile = profile;
            pl.level = level;
            MediaFormat defaultFormat = new MediaFormat();
            defaultFormat.setString(MediaFormat.KEY_MIME, mime);
            CodecCapabilities ret = new CodecCapabilities(new CodecProfileLevel[]{pl}, new int[0], true, 0, defaultFormat, new MediaFormat());
            if (ret.mError != 0) {
                return null;
            }
            return ret;
        }

        CodecCapabilities(CodecProfileLevel[] profLevs, int[] colFmts, boolean encoder, int flags, Map<String, Object> defaultFormatMap, Map<String, Object> capabilitiesMap) {
            this(profLevs, colFmts, encoder, flags, new MediaFormat(defaultFormatMap), new MediaFormat(capabilitiesMap));
        }

        CodecCapabilities(CodecProfileLevel[] profLevs, int[] colFmts, boolean encoder, int flags, MediaFormat defaultFormat, MediaFormat info) {
            MediaFormat mediaFormat = info;
            Map<String, Object> map = info.getMap();
            this.colorFormats = colFmts;
            this.mFlagsVerified = flags;
            this.mDefaultFormat = defaultFormat;
            this.mCapabilitiesInfo = mediaFormat;
            this.mMime = this.mDefaultFormat.getString(MediaFormat.KEY_MIME);
            CodecProfileLevel[] profLevs2 = profLevs;
            int i = 0;
            if (profLevs2.length == 0 && this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_VP9)) {
                CodecProfileLevel profLev = new CodecProfileLevel();
                profLev.profile = 1;
                profLev.level = VideoCapabilities.equivalentVP9Level(info);
                profLevs2 = new CodecProfileLevel[]{profLev};
            }
            this.profileLevels = profLevs2;
            if (this.mMime.toLowerCase().startsWith("audio/")) {
                this.mAudioCaps = AudioCapabilities.create(mediaFormat, this);
                this.mAudioCaps.getDefaultFormat(this.mDefaultFormat);
            } else if (this.mMime.toLowerCase().startsWith("video/") || this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC)) {
                this.mVideoCaps = VideoCapabilities.create(mediaFormat, this);
            }
            if (encoder) {
                this.mEncoderCaps = EncoderCapabilities.create(mediaFormat, this);
                this.mEncoderCaps.getDefaultFormat(this.mDefaultFormat);
            }
            this.mMaxSupportedInstances = Utils.parseIntSafely(MediaCodecList.getGlobalSettings().get("max-concurrent-instances"), 32);
            this.mMaxSupportedInstances = ((Integer) Range.create(Integer.valueOf(1), Integer.valueOf(256)).clamp(Integer.valueOf(Utils.parseIntSafely(map.get("max-concurrent-instances"), this.mMaxSupportedInstances)))).intValue();
            Feature[] validFeatures = getValidFeatures();
            int length = validFeatures.length;
            while (i < length) {
                Map<String, Object> map2;
                int i2;
                Feature feat = validFeatures[i];
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(MediaFormat.KEY_FEATURE_);
                stringBuilder.append(feat.mName);
                String key = stringBuilder.toString();
                Integer yesNo = (Integer) map.get(key);
                if (yesNo == null) {
                    map2 = map;
                    i2 = 1;
                } else {
                    if (yesNo.intValue() > 0) {
                        map2 = map;
                        this.mFlagsRequired |= feat.mValue;
                    } else {
                        map2 = map;
                    }
                    this.mFlagsSupported |= feat.mValue;
                    i2 = 1;
                    this.mDefaultFormat.setInteger(key, 1);
                }
                i++;
                int i3 = i2;
                map = map2;
                mediaFormat = info;
            }
        }
    }

    public static final class CodecProfileLevel {
        public static final int AACObjectELD = 39;
        public static final int AACObjectERLC = 17;
        public static final int AACObjectERScalable = 20;
        public static final int AACObjectHE = 5;
        public static final int AACObjectHE_PS = 29;
        public static final int AACObjectLC = 2;
        public static final int AACObjectLD = 23;
        public static final int AACObjectLTP = 4;
        public static final int AACObjectMain = 1;
        public static final int AACObjectSSR = 3;
        public static final int AACObjectScalable = 6;
        public static final int AACObjectXHE = 42;
        public static final int AVCLevel1 = 1;
        public static final int AVCLevel11 = 4;
        public static final int AVCLevel12 = 8;
        public static final int AVCLevel13 = 16;
        public static final int AVCLevel1b = 2;
        public static final int AVCLevel2 = 32;
        public static final int AVCLevel21 = 64;
        public static final int AVCLevel22 = 128;
        public static final int AVCLevel3 = 256;
        public static final int AVCLevel31 = 512;
        public static final int AVCLevel32 = 1024;
        public static final int AVCLevel4 = 2048;
        public static final int AVCLevel41 = 4096;
        public static final int AVCLevel42 = 8192;
        public static final int AVCLevel5 = 16384;
        public static final int AVCLevel51 = 32768;
        public static final int AVCLevel52 = 65536;
        public static final int AVCProfileBaseline = 1;
        public static final int AVCProfileConstrainedBaseline = 65536;
        public static final int AVCProfileConstrainedHigh = 524288;
        public static final int AVCProfileExtended = 4;
        public static final int AVCProfileHigh = 8;
        public static final int AVCProfileHigh10 = 16;
        public static final int AVCProfileHigh422 = 32;
        public static final int AVCProfileHigh444 = 64;
        public static final int AVCProfileMain = 2;
        public static final int DolbyVisionLevelFhd24 = 4;
        public static final int DolbyVisionLevelFhd30 = 8;
        public static final int DolbyVisionLevelFhd60 = 16;
        public static final int DolbyVisionLevelHd24 = 1;
        public static final int DolbyVisionLevelHd30 = 2;
        public static final int DolbyVisionLevelUhd24 = 32;
        public static final int DolbyVisionLevelUhd30 = 64;
        public static final int DolbyVisionLevelUhd48 = 128;
        public static final int DolbyVisionLevelUhd60 = 256;
        public static final int DolbyVisionProfileDvavPen = 2;
        public static final int DolbyVisionProfileDvavPer = 1;
        public static final int DolbyVisionProfileDvavSe = 512;
        public static final int DolbyVisionProfileDvheDen = 8;
        public static final int DolbyVisionProfileDvheDer = 4;
        public static final int DolbyVisionProfileDvheDtb = 128;
        public static final int DolbyVisionProfileDvheDth = 64;
        public static final int DolbyVisionProfileDvheDtr = 16;
        public static final int DolbyVisionProfileDvheSt = 256;
        public static final int DolbyVisionProfileDvheStn = 32;
        public static final int H263Level10 = 1;
        public static final int H263Level20 = 2;
        public static final int H263Level30 = 4;
        public static final int H263Level40 = 8;
        public static final int H263Level45 = 16;
        public static final int H263Level50 = 32;
        public static final int H263Level60 = 64;
        public static final int H263Level70 = 128;
        public static final int H263ProfileBackwardCompatible = 4;
        public static final int H263ProfileBaseline = 1;
        public static final int H263ProfileH320Coding = 2;
        public static final int H263ProfileHighCompression = 32;
        public static final int H263ProfileHighLatency = 256;
        public static final int H263ProfileISWV2 = 8;
        public static final int H263ProfileISWV3 = 16;
        public static final int H263ProfileInterlace = 128;
        public static final int H263ProfileInternet = 64;
        public static final int HEVCHighTierLevel1 = 2;
        public static final int HEVCHighTierLevel2 = 8;
        public static final int HEVCHighTierLevel21 = 32;
        public static final int HEVCHighTierLevel3 = 128;
        public static final int HEVCHighTierLevel31 = 512;
        public static final int HEVCHighTierLevel4 = 2048;
        public static final int HEVCHighTierLevel41 = 8192;
        public static final int HEVCHighTierLevel5 = 32768;
        public static final int HEVCHighTierLevel51 = 131072;
        public static final int HEVCHighTierLevel52 = 524288;
        public static final int HEVCHighTierLevel6 = 2097152;
        public static final int HEVCHighTierLevel61 = 8388608;
        public static final int HEVCHighTierLevel62 = 33554432;
        private static final int HEVCHighTierLevels = 44739242;
        public static final int HEVCMainTierLevel1 = 1;
        public static final int HEVCMainTierLevel2 = 4;
        public static final int HEVCMainTierLevel21 = 16;
        public static final int HEVCMainTierLevel3 = 64;
        public static final int HEVCMainTierLevel31 = 256;
        public static final int HEVCMainTierLevel4 = 1024;
        public static final int HEVCMainTierLevel41 = 4096;
        public static final int HEVCMainTierLevel5 = 16384;
        public static final int HEVCMainTierLevel51 = 65536;
        public static final int HEVCMainTierLevel52 = 262144;
        public static final int HEVCMainTierLevel6 = 1048576;
        public static final int HEVCMainTierLevel61 = 4194304;
        public static final int HEVCMainTierLevel62 = 16777216;
        public static final int HEVCProfileMain = 1;
        public static final int HEVCProfileMain10 = 2;
        public static final int HEVCProfileMain10HDR10 = 4096;
        public static final int HEVCProfileMainStill = 4;
        public static final int MPEG2LevelH14 = 2;
        public static final int MPEG2LevelHL = 3;
        public static final int MPEG2LevelHP = 4;
        public static final int MPEG2LevelLL = 0;
        public static final int MPEG2LevelML = 1;
        public static final int MPEG2Profile422 = 2;
        public static final int MPEG2ProfileHigh = 5;
        public static final int MPEG2ProfileMain = 1;
        public static final int MPEG2ProfileSNR = 3;
        public static final int MPEG2ProfileSimple = 0;
        public static final int MPEG2ProfileSpatial = 4;
        public static final int MPEG4Level0 = 1;
        public static final int MPEG4Level0b = 2;
        public static final int MPEG4Level1 = 4;
        public static final int MPEG4Level2 = 8;
        public static final int MPEG4Level3 = 16;
        public static final int MPEG4Level3b = 24;
        public static final int MPEG4Level4 = 32;
        public static final int MPEG4Level4a = 64;
        public static final int MPEG4Level5 = 128;
        public static final int MPEG4Level6 = 256;
        public static final int MPEG4ProfileAdvancedCoding = 4096;
        public static final int MPEG4ProfileAdvancedCore = 8192;
        public static final int MPEG4ProfileAdvancedRealTime = 1024;
        public static final int MPEG4ProfileAdvancedScalable = 16384;
        public static final int MPEG4ProfileAdvancedSimple = 32768;
        public static final int MPEG4ProfileBasicAnimated = 256;
        public static final int MPEG4ProfileCore = 4;
        public static final int MPEG4ProfileCoreScalable = 2048;
        public static final int MPEG4ProfileHybrid = 512;
        public static final int MPEG4ProfileMain = 8;
        public static final int MPEG4ProfileNbit = 16;
        public static final int MPEG4ProfileScalableTexture = 32;
        public static final int MPEG4ProfileSimple = 1;
        public static final int MPEG4ProfileSimpleFBA = 128;
        public static final int MPEG4ProfileSimpleFace = 64;
        public static final int MPEG4ProfileSimpleScalable = 2;
        public static final int VP8Level_Version0 = 1;
        public static final int VP8Level_Version1 = 2;
        public static final int VP8Level_Version2 = 4;
        public static final int VP8Level_Version3 = 8;
        public static final int VP8ProfileMain = 1;
        public static final int VP9Level1 = 1;
        public static final int VP9Level11 = 2;
        public static final int VP9Level2 = 4;
        public static final int VP9Level21 = 8;
        public static final int VP9Level3 = 16;
        public static final int VP9Level31 = 32;
        public static final int VP9Level4 = 64;
        public static final int VP9Level41 = 128;
        public static final int VP9Level5 = 256;
        public static final int VP9Level51 = 512;
        public static final int VP9Level52 = 1024;
        public static final int VP9Level6 = 2048;
        public static final int VP9Level61 = 4096;
        public static final int VP9Level62 = 8192;
        public static final int VP9Profile0 = 1;
        public static final int VP9Profile1 = 2;
        public static final int VP9Profile2 = 4;
        public static final int VP9Profile2HDR = 4096;
        public static final int VP9Profile3 = 8;
        public static final int VP9Profile3HDR = 8192;
        public int level;
        public int profile;

        public boolean equals(Object obj) {
            boolean z = false;
            if (obj == null || !(obj instanceof CodecProfileLevel)) {
                return false;
            }
            CodecProfileLevel other = (CodecProfileLevel) obj;
            if (other.profile == this.profile && other.level == this.level) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return Long.hashCode((((long) this.profile) << 32) | ((long) this.level));
        }
    }

    public static final class EncoderCapabilities {
        public static final int BITRATE_MODE_CBR = 2;
        public static final int BITRATE_MODE_CQ = 0;
        public static final int BITRATE_MODE_VBR = 1;
        private static final Feature[] bitrates = new Feature[]{new Feature("VBR", 1, true), new Feature("CBR", 2, false), new Feature("CQ", 0, false)};
        private int mBitControl;
        private Range<Integer> mComplexityRange;
        private Integer mDefaultComplexity;
        private Integer mDefaultQuality;
        private CodecCapabilities mParent;
        private Range<Integer> mQualityRange;
        private String mQualityScale;

        public Range<Integer> getQualityRange() {
            return this.mQualityRange;
        }

        public Range<Integer> getComplexityRange() {
            return this.mComplexityRange;
        }

        private static int parseBitrateMode(String mode) {
            for (Feature feat : bitrates) {
                if (feat.mName.equalsIgnoreCase(mode)) {
                    return feat.mValue;
                }
            }
            return 0;
        }

        public boolean isBitrateModeSupported(int mode) {
            for (Feature feat : bitrates) {
                if (mode == feat.mValue) {
                    boolean z = true;
                    if ((this.mBitControl & (1 << mode)) == 0) {
                        z = false;
                    }
                    return z;
                }
            }
            return false;
        }

        private EncoderCapabilities() {
        }

        public static EncoderCapabilities create(MediaFormat info, CodecCapabilities parent) {
            EncoderCapabilities caps = new EncoderCapabilities();
            caps.init(info, parent);
            return caps;
        }

        private void init(MediaFormat info, CodecCapabilities parent) {
            this.mParent = parent;
            this.mComplexityRange = Range.create(Integer.valueOf(0), Integer.valueOf(0));
            this.mQualityRange = Range.create(Integer.valueOf(0), Integer.valueOf(0));
            this.mBitControl = 2;
            applyLevelLimits();
            parseFromInfo(info);
        }

        private void applyLevelLimits() {
            String mime = this.mParent.getMimeType();
            if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_FLAC)) {
                this.mComplexityRange = Range.create(Integer.valueOf(0), Integer.valueOf(8));
                this.mBitControl = 1;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_NB) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_WB) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_ALAW) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_MLAW) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_MSGSM)) {
                this.mBitControl = 4;
            }
        }

        private void parseFromInfo(MediaFormat info) {
            Map<String, Object> map = info.getMap();
            if (info.containsKey("complexity-range")) {
                this.mComplexityRange = Utils.parseIntRange(info.getString("complexity-range"), this.mComplexityRange);
            }
            if (info.containsKey("quality-range")) {
                this.mQualityRange = Utils.parseIntRange(info.getString("quality-range"), this.mQualityRange);
            }
            if (info.containsKey("feature-bitrate-modes")) {
                for (String mode : info.getString("feature-bitrate-modes").split(",")) {
                    this.mBitControl |= 1 << parseBitrateMode(mode);
                }
            }
            try {
                this.mDefaultComplexity = Integer.valueOf(Integer.parseInt((String) map.get("complexity-default")));
            } catch (NumberFormatException e) {
            }
            try {
                this.mDefaultQuality = Integer.valueOf(Integer.parseInt((String) map.get("quality-default")));
            } catch (NumberFormatException e2) {
            }
            this.mQualityScale = (String) map.get("quality-scale");
        }

        private boolean supports(Integer complexity, Integer quality, Integer profile) {
            boolean ok = true;
            if (!(1 == null || complexity == null)) {
                ok = this.mComplexityRange.contains(complexity);
            }
            if (ok && quality != null) {
                ok = this.mQualityRange.contains(quality);
            }
            if (!ok || profile == null) {
                return ok;
            }
            boolean z = false;
            for (CodecProfileLevel pl : this.mParent.profileLevels) {
                if (pl.profile == profile.intValue()) {
                    profile = null;
                    break;
                }
            }
            if (profile == null) {
                z = true;
            }
            return z;
        }

        public void getDefaultFormat(MediaFormat format) {
            if (!(((Integer) this.mQualityRange.getUpper()).equals(this.mQualityRange.getLower()) || this.mDefaultQuality == null)) {
                format.setInteger(MediaFormat.KEY_QUALITY, this.mDefaultQuality.intValue());
            }
            if (!(((Integer) this.mComplexityRange.getUpper()).equals(this.mComplexityRange.getLower()) || this.mDefaultComplexity == null)) {
                format.setInteger(MediaFormat.KEY_COMPLEXITY, this.mDefaultComplexity.intValue());
            }
            for (Feature feat : bitrates) {
                if ((this.mBitControl & (1 << feat.mValue)) != 0) {
                    format.setInteger(MediaFormat.KEY_BITRATE_MODE, feat.mValue);
                    return;
                }
            }
        }

        public boolean supportsFormat(MediaFormat format) {
            Map<String, Object> map = format.getMap();
            String mime = this.mParent.getMimeType();
            Integer mode = (Integer) map.get(MediaFormat.KEY_BITRATE_MODE);
            if (mode != null && !isBitrateModeSupported(mode.intValue())) {
                return false;
            }
            Integer flacComplexity;
            Integer complexity = (Integer) map.get(MediaFormat.KEY_COMPLEXITY);
            if (MediaFormat.MIMETYPE_AUDIO_FLAC.equalsIgnoreCase(mime)) {
                flacComplexity = (Integer) map.get(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL);
                if (complexity == null) {
                    complexity = flacComplexity;
                } else if (!(flacComplexity == null || complexity.equals(flacComplexity))) {
                    throw new IllegalArgumentException("conflicting values for complexity and flac-compression-level");
                }
            }
            flacComplexity = (Integer) map.get(MediaFormat.KEY_PROFILE);
            if (MediaFormat.MIMETYPE_AUDIO_AAC.equalsIgnoreCase(mime)) {
                Integer aacProfile = (Integer) map.get(MediaFormat.KEY_AAC_PROFILE);
                if (flacComplexity == null) {
                    flacComplexity = aacProfile;
                } else if (!(aacProfile == null || aacProfile.equals(flacComplexity))) {
                    throw new IllegalArgumentException("conflicting values for profile and aac-profile");
                }
            }
            return supports(complexity, (Integer) map.get(MediaFormat.KEY_QUALITY), flacComplexity);
        }
    }

    private static class Feature {
        public boolean mDefault;
        public String mName;
        public int mValue;

        public Feature(String name, int value, boolean def) {
            this.mName = name;
            this.mValue = value;
            this.mDefault = def;
        }
    }

    public static final class VideoCapabilities {
        private static final String TAG = "VideoCapabilities";
        private boolean mAllowMbOverride;
        private Range<Rational> mAspectRatioRange;
        private Range<Integer> mBitrateRange;
        private Range<Rational> mBlockAspectRatioRange;
        private Range<Integer> mBlockCountRange;
        private int mBlockHeight;
        private int mBlockWidth;
        private Range<Long> mBlocksPerSecondRange;
        private Range<Integer> mFrameRateRange;
        private int mHeightAlignment;
        private Range<Integer> mHeightRange;
        private Range<Integer> mHorizontalBlockRange;
        private Map<Size, Range<Long>> mMeasuredFrameRates;
        private CodecCapabilities mParent;
        private int mSmallerDimensionUpperLimit;
        private Range<Integer> mVerticalBlockRange;
        private int mWidthAlignment;
        private Range<Integer> mWidthRange;

        public Range<Integer> getBitrateRange() {
            return this.mBitrateRange;
        }

        public Range<Integer> getSupportedWidths() {
            return this.mWidthRange;
        }

        public Range<Integer> getSupportedHeights() {
            return this.mHeightRange;
        }

        public int getWidthAlignment() {
            return this.mWidthAlignment;
        }

        public int getHeightAlignment() {
            return this.mHeightAlignment;
        }

        public int getSmallerDimensionUpperLimit() {
            return this.mSmallerDimensionUpperLimit;
        }

        public Range<Integer> getSupportedFrameRates() {
            return this.mFrameRateRange;
        }

        public Range<Integer> getSupportedWidthsFor(int height) {
            try {
                Range<Integer> range = this.mWidthRange;
                if (this.mHeightRange.contains(Integer.valueOf(height)) && height % this.mHeightAlignment == 0) {
                    int heightInBlocks = Utils.divUp(height, this.mBlockHeight);
                    range = range.intersect(Integer.valueOf(((Math.max(Utils.divUp(((Integer) this.mBlockCountRange.getLower()).intValue(), heightInBlocks), (int) Math.ceil(((Rational) this.mBlockAspectRatioRange.getLower()).doubleValue() * ((double) heightInBlocks))) - 1) * this.mBlockWidth) + this.mWidthAlignment), Integer.valueOf(this.mBlockWidth * Math.min(((Integer) this.mBlockCountRange.getUpper()).intValue() / heightInBlocks, (int) (((Rational) this.mBlockAspectRatioRange.getUpper()).doubleValue() * ((double) heightInBlocks)))));
                    if (height > this.mSmallerDimensionUpperLimit) {
                        range = range.intersect(Integer.valueOf(1), Integer.valueOf(this.mSmallerDimensionUpperLimit));
                    }
                    return range.intersect(Integer.valueOf((int) Math.ceil(((Rational) this.mAspectRatioRange.getLower()).doubleValue() * ((double) height))), Integer.valueOf((int) (((Rational) this.mAspectRatioRange.getUpper()).doubleValue() * ((double) height))));
                }
                throw new IllegalArgumentException("unsupported height");
            } catch (IllegalArgumentException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("could not get supported widths for ");
                stringBuilder.append(height);
                Log.v(TAG, stringBuilder.toString());
                throw new IllegalArgumentException("unsupported height");
            }
        }

        public Range<Integer> getSupportedHeightsFor(int width) {
            try {
                Range<Integer> range = this.mHeightRange;
                if (this.mWidthRange.contains(Integer.valueOf(width)) && width % this.mWidthAlignment == 0) {
                    int widthInBlocks = Utils.divUp(width, this.mBlockWidth);
                    range = range.intersect(Integer.valueOf(((Math.max(Utils.divUp(((Integer) this.mBlockCountRange.getLower()).intValue(), widthInBlocks), (int) Math.ceil(((double) widthInBlocks) / ((Rational) this.mBlockAspectRatioRange.getUpper()).doubleValue())) - 1) * this.mBlockHeight) + this.mHeightAlignment), Integer.valueOf(this.mBlockHeight * Math.min(((Integer) this.mBlockCountRange.getUpper()).intValue() / widthInBlocks, (int) (((double) widthInBlocks) / ((Rational) this.mBlockAspectRatioRange.getLower()).doubleValue()))));
                    if (width > this.mSmallerDimensionUpperLimit) {
                        range = range.intersect(Integer.valueOf(1), Integer.valueOf(this.mSmallerDimensionUpperLimit));
                    }
                    return range.intersect(Integer.valueOf((int) Math.ceil(((double) width) / ((Rational) this.mAspectRatioRange.getUpper()).doubleValue())), Integer.valueOf((int) (((double) width) / ((Rational) this.mAspectRatioRange.getLower()).doubleValue())));
                }
                throw new IllegalArgumentException("unsupported width");
            } catch (IllegalArgumentException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("could not get supported heights for ");
                stringBuilder.append(width);
                Log.v(TAG, stringBuilder.toString());
                throw new IllegalArgumentException("unsupported width");
            }
        }

        public Range<Double> getSupportedFrameRatesFor(int width, int height) {
            Range<Integer> range = this.mHeightRange;
            if (supports(Integer.valueOf(width), Integer.valueOf(height), null)) {
                int blockCount = Utils.divUp(width, this.mBlockWidth) * Utils.divUp(height, this.mBlockHeight);
                return Range.create(Double.valueOf(Math.max(((double) ((Long) this.mBlocksPerSecondRange.getLower()).longValue()) / ((double) blockCount), (double) ((Integer) this.mFrameRateRange.getLower()).intValue())), Double.valueOf(Math.min(((double) ((Long) this.mBlocksPerSecondRange.getUpper()).longValue()) / ((double) blockCount), (double) ((Integer) this.mFrameRateRange.getUpper()).intValue())));
            }
            throw new IllegalArgumentException("unsupported size");
        }

        private int getBlockCount(int width, int height) {
            return Utils.divUp(width, this.mBlockWidth) * Utils.divUp(height, this.mBlockHeight);
        }

        private Size findClosestSize(int width, int height) {
            int targetBlockCount = getBlockCount(width, height);
            Size closestSize = null;
            int minDiff = Integer.MAX_VALUE;
            for (Size size : this.mMeasuredFrameRates.keySet()) {
                int diff = Math.abs(targetBlockCount - getBlockCount(size.getWidth(), size.getHeight()));
                if (diff < minDiff) {
                    minDiff = diff;
                    closestSize = size;
                }
            }
            return closestSize;
        }

        private Range<Double> estimateFrameRatesFor(int width, int height) {
            Size size = findClosestSize(width, height);
            Range<Long> range = (Range) this.mMeasuredFrameRates.get(size);
            Double ratio = Double.valueOf(((double) getBlockCount(size.getWidth(), size.getHeight())) / ((double) Math.max(getBlockCount(width, height), 1)));
            return Range.create(Double.valueOf(((double) ((Long) range.getLower()).longValue()) * ratio.doubleValue()), Double.valueOf(((double) ((Long) range.getUpper()).longValue()) * ratio.doubleValue()));
        }

        public Range<Double> getAchievableFrameRatesFor(int width, int height) {
            if (!supports(Integer.valueOf(width), Integer.valueOf(height), null)) {
                throw new IllegalArgumentException("unsupported size");
            } else if (this.mMeasuredFrameRates != null && this.mMeasuredFrameRates.size() > 0) {
                return estimateFrameRatesFor(width, height);
            } else {
                Log.w(TAG, "Codec did not publish any measurement data.");
                return null;
            }
        }

        public boolean areSizeAndRateSupported(int width, int height, double frameRate) {
            return supports(Integer.valueOf(width), Integer.valueOf(height), Double.valueOf(frameRate));
        }

        public boolean isSizeSupported(int width, int height) {
            return supports(Integer.valueOf(width), Integer.valueOf(height), null);
        }

        private boolean supports(Integer width, Integer height, Number rate) {
            boolean z;
            boolean ok = true;
            boolean z2 = false;
            if (!(1 == null || width == null)) {
                z = this.mWidthRange.contains(width) && width.intValue() % this.mWidthAlignment == 0;
                ok = z;
            }
            if (ok && height != null) {
                z = this.mHeightRange.contains(height) && height.intValue() % this.mHeightAlignment == 0;
                ok = z;
            }
            if (ok && rate != null) {
                ok = this.mFrameRateRange.contains(Utils.intRangeFor(rate.doubleValue()));
            }
            if (!ok || height == null || width == null) {
                return ok;
            }
            ok = Math.min(height.intValue(), width.intValue()) <= this.mSmallerDimensionUpperLimit;
            int widthInBlocks = Utils.divUp(width.intValue(), this.mBlockWidth);
            int heightInBlocks = Utils.divUp(height.intValue(), this.mBlockHeight);
            int blockCount = widthInBlocks * heightInBlocks;
            if (ok && this.mBlockCountRange.contains(Integer.valueOf(blockCount)) && this.mBlockAspectRatioRange.contains(new Rational(widthInBlocks, heightInBlocks)) && this.mAspectRatioRange.contains(new Rational(width.intValue(), height.intValue()))) {
                z2 = true;
            }
            ok = z2;
            if (!ok || rate == null) {
                return ok;
            }
            return this.mBlocksPerSecondRange.contains(Utils.longRangeFor(((double) blockCount) * rate.doubleValue()));
        }

        public boolean supportsFormat(MediaFormat format) {
            Map<String, Object> map = format.getMap();
            if (supports((Integer) map.get(MediaFormat.KEY_WIDTH), (Integer) map.get(MediaFormat.KEY_HEIGHT), (Number) map.get(MediaFormat.KEY_FRAME_RATE)) && CodecCapabilities.supportsBitrate(this.mBitrateRange, format)) {
                return true;
            }
            return false;
        }

        private VideoCapabilities() {
        }

        public static VideoCapabilities create(MediaFormat info, CodecCapabilities parent) {
            VideoCapabilities caps = new VideoCapabilities();
            caps.init(info, parent);
            return caps;
        }

        private void init(MediaFormat info, CodecCapabilities parent) {
            this.mParent = parent;
            initWithPlatformLimits();
            applyLevelLimits();
            parseFromInfo(info);
            updateLimits();
        }

        public Size getBlockSize() {
            return new Size(this.mBlockWidth, this.mBlockHeight);
        }

        public Range<Integer> getBlockCountRange() {
            return this.mBlockCountRange;
        }

        public Range<Long> getBlocksPerSecondRange() {
            return this.mBlocksPerSecondRange;
        }

        public Range<Rational> getAspectRatioRange(boolean blocks) {
            return blocks ? this.mBlockAspectRatioRange : this.mAspectRatioRange;
        }

        private void initWithPlatformLimits() {
            this.mBitrateRange = MediaCodecInfo.BITRATE_RANGE;
            this.mWidthRange = MediaCodecInfo.SIZE_RANGE;
            this.mHeightRange = MediaCodecInfo.SIZE_RANGE;
            this.mFrameRateRange = MediaCodecInfo.FRAME_RATE_RANGE;
            this.mHorizontalBlockRange = MediaCodecInfo.SIZE_RANGE;
            this.mVerticalBlockRange = MediaCodecInfo.SIZE_RANGE;
            this.mBlockCountRange = MediaCodecInfo.POSITIVE_INTEGERS;
            this.mBlocksPerSecondRange = MediaCodecInfo.POSITIVE_LONGS;
            this.mBlockAspectRatioRange = MediaCodecInfo.POSITIVE_RATIONALS;
            this.mAspectRatioRange = MediaCodecInfo.POSITIVE_RATIONALS;
            this.mWidthAlignment = 2;
            this.mHeightAlignment = 2;
            this.mBlockWidth = 2;
            this.mBlockHeight = 2;
            this.mSmallerDimensionUpperLimit = ((Integer) MediaCodecInfo.SIZE_RANGE.getUpper()).intValue();
        }

        private Map<Size, Range<Long>> getMeasuredFrameRates(Map<String, Object> map) {
            Map<Size, Range<Long>> ret = new HashMap();
            String prefix = "measured-frame-rate-";
            for (String key : map.keySet()) {
                if (key.startsWith("measured-frame-rate-")) {
                    String subKey = key.substring("measured-frame-rate-".length());
                    String[] temp = key.split("-");
                    if (temp.length == 5) {
                        Size size = Utils.parseSize(temp[3], null);
                        if (size != null) {
                            if (size.getWidth() * size.getHeight() > 0) {
                                Range<Long> range = Utils.parseLongRange(map.get(key), null);
                                if (range != null && ((Long) range.getLower()).longValue() >= 0) {
                                    if (((Long) range.getUpper()).longValue() >= 0) {
                                        ret.put(size, range);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return ret;
        }

        private static Pair<Range<Integer>, Range<Integer>> parseWidthHeightRanges(Object o) {
            Pair<Size, Size> range = Utils.parseSizeRange(o);
            if (range != null) {
                try {
                    return Pair.create(Range.create(Integer.valueOf(((Size) range.first).getWidth()), Integer.valueOf(((Size) range.second).getWidth())), Range.create(Integer.valueOf(((Size) range.first).getHeight()), Integer.valueOf(((Size) range.second).getHeight())));
                } catch (IllegalArgumentException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("could not parse size range '");
                    stringBuilder.append(o);
                    stringBuilder.append("'");
                    Log.w(str, stringBuilder.toString());
                }
            }
            return null;
        }

        public static int equivalentVP9Level(MediaFormat info) {
            Map<String, Object> map = info.getMap();
            Size blockSize = Utils.parseSize(map.get("block-size"), new Size(8, 8));
            int BS = blockSize.getWidth() * blockSize.getHeight();
            Range<Integer> counts = Utils.parseIntRange(map.get("block-count-range"), null);
            int BR = 0;
            int FS = counts == null ? 0 : ((Integer) counts.getUpper()).intValue() * BS;
            Range<Long> blockRates = Utils.parseLongRange(map.get("blocks-per-second-range"), null);
            long SR = blockRates == null ? 0 : ((long) BS) * ((Long) blockRates.getUpper()).longValue();
            Pair<Range<Integer>, Range<Integer>> dimensionRanges = parseWidthHeightRanges(map.get("size-range"));
            int D = dimensionRanges == null ? 0 : Math.max(((Integer) ((Range) dimensionRanges.first).getUpper()).intValue(), ((Integer) ((Range) dimensionRanges.second).getUpper()).intValue());
            Range<Integer> bitRates = Utils.parseIntRange(map.get("bitrate-range"), null);
            if (bitRates != null) {
                BR = Utils.divUp(((Integer) bitRates.getUpper()).intValue(), 1000);
            }
            if (SR <= 829440 && FS <= 36864 && BR <= 200 && D <= 512) {
                return 1;
            }
            if (SR <= 2764800 && FS <= 73728 && BR <= 800 && D <= 768) {
                return 2;
            }
            if (SR <= 4608000 && FS <= 122880 && BR <= Device.WEARABLE_PAGER && D <= 960) {
                return 4;
            }
            if (SR <= 9216000 && FS <= 245760 && BR <= 3600 && D <= Device.PERIPHERAL_KEYBOARD) {
                return 8;
            }
            if (SR <= 20736000 && FS <= 552960 && BR <= 7200 && D <= 2048) {
                return 16;
            }
            if (SR <= 36864000 && FS <= 983040 && BR <= 12000 && D <= 2752) {
                return 32;
            }
            if (SR <= 83558400 && FS <= 2228224 && BR <= 18000 && D <= 4160) {
                return 64;
            }
            if (SR <= 160432128 && FS <= 2228224 && BR <= 30000 && D <= 4160) {
                return 128;
            }
            if (SR <= 311951360 && FS <= 8912896 && BR <= 60000 && D <= 8384) {
                return 256;
            }
            if (SR <= 588251136 && FS <= 8912896 && BR <= 120000 && D <= 8384) {
                return 512;
            }
            if (SR <= 1176502272 && FS <= 8912896 && BR <= 180000 && D <= 8384) {
                return 1024;
            }
            if (SR <= 1176502272 && FS <= 35651584 && BR <= 180000 && D <= 16832) {
                return 2048;
            }
            if (SR > 2353004544L || FS > 35651584 || BR > 240000 || D > 16832) {
                return (SR > 4706009088L || FS > 35651584 || BR > 480000 || D <= 16832) ? 8192 : 8192;
            } else {
                return 4096;
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:49:0x026b  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x01c6  */
        /* JADX WARNING: Removed duplicated region for block: B:51:0x0277  */
        /* JADX WARNING: Removed duplicated region for block: B:53:0x0283  */
        /* JADX WARNING: Removed duplicated region for block: B:55:0x028f  */
        /* JADX WARNING: Removed duplicated region for block: B:57:0x02ae  */
        /* JADX WARNING: Removed duplicated region for block: B:59:0x02ce  */
        /* JADX WARNING: Removed duplicated region for block: B:61:0x02ec  */
        /* JADX WARNING: Removed duplicated region for block: B:63:0x02f8  */
        /* JADX WARNING: Removed duplicated region for block: B:65:0x0304  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void parseFromInfo(MediaFormat info) {
            Range<Integer> extend;
            Range<Integer> heights;
            Range<Integer> frameRates;
            Range<Integer> widths;
            Range<Rational> ratios;
            Range<Rational> blockRatios;
            Range blockRates;
            Range<Rational> ratios2;
            Range<Rational> blockRatios2;
            Map<String, Object> map = info.getMap();
            Size blockSize = new Size(this.mBlockWidth, this.mBlockHeight);
            Size alignment = new Size(this.mWidthAlignment, this.mHeightAlignment);
            Range<Integer> heights2 = null;
            Range<Integer> widths2 = null;
            Size blockSize2 = Utils.parseSize(map.get("block-size"), blockSize);
            Size alignment2 = Utils.parseSize(map.get("alignment"), alignment);
            Range counts = Utils.parseIntRange(map.get("block-count-range"), null);
            Range<Long> blockRates2 = Utils.parseLongRange(map.get("blocks-per-second-range"), null);
            this.mMeasuredFrameRates = getMeasuredFrameRates(map);
            Pair<Range<Integer>, Range<Integer>> sizeRanges = parseWidthHeightRanges(map.get("size-range"));
            if (sizeRanges != null) {
                heights2 = sizeRanges.first;
                widths2 = sizeRanges.second;
            }
            if (map.containsKey("feature-can-swap-width-height")) {
                if (heights2 != null) {
                    this.mSmallerDimensionUpperLimit = Math.min(((Integer) heights2.getUpper()).intValue(), ((Integer) widths2.getUpper()).intValue());
                    extend = heights2.extend(widths2);
                    widths2 = extend;
                    heights2 = extend;
                } else {
                    Log.w(TAG, "feature can-swap-width-height is best used with size-range");
                    this.mSmallerDimensionUpperLimit = Math.min(((Integer) this.mWidthRange.getUpper()).intValue(), ((Integer) this.mHeightRange.getUpper()).intValue());
                    Range extend2 = this.mWidthRange.extend(this.mHeightRange);
                    this.mHeightRange = extend2;
                    this.mWidthRange = extend2;
                }
            }
            Range<Integer> range = widths2;
            widths2 = heights2;
            heights2 = range;
            Range<Rational> ratios3 = Utils.parseRationalRange(map.get("block-aspect-ratio-range"), null);
            Range<Rational> blockRatios3 = Utils.parseRationalRange(map.get("pixel-aspect-ratio-range"), null);
            Range<Integer> frameRates2 = Utils.parseIntRange(map.get("frame-rate-range"), null);
            if (frameRates2 != null) {
                try {
                    frameRates2 = frameRates2.intersect(MediaCodecInfo.FRAME_RATE_RANGE);
                } catch (IllegalArgumentException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("frame rate range (");
                    stringBuilder.append(frameRates2);
                    stringBuilder.append(") is out of limits: ");
                    stringBuilder.append(MediaCodecInfo.FRAME_RATE_RANGE);
                    Log.w(str, stringBuilder.toString());
                    frameRates2 = null;
                }
            }
            Range<Integer> frameRates3 = frameRates2;
            Range<Integer> bitRates = Utils.parseIntRange(map.get("bitrate-range"), null);
            if (bitRates != null) {
                try {
                    extend = bitRates.intersect(MediaCodecInfo.BITRATE_RANGE);
                } catch (IllegalArgumentException e2) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("bitrate range (");
                    stringBuilder2.append(bitRates);
                    stringBuilder2.append(") is out of limits: ");
                    stringBuilder2.append(MediaCodecInfo.BITRATE_RANGE);
                    Log.w(str2, stringBuilder2.toString());
                    bitRates = null;
                }
                MediaCodecInfo.checkPowerOfTwo(blockSize2.getWidth(), "block-size width must be power of two");
                MediaCodecInfo.checkPowerOfTwo(blockSize2.getHeight(), "block-size height must be power of two");
                MediaCodecInfo.checkPowerOfTwo(alignment2.getWidth(), "alignment width must be power of two");
                MediaCodecInfo.checkPowerOfTwo(alignment2.getHeight(), "alignment height must be power of two");
                heights = heights2;
                frameRates = frameRates3;
                widths = widths2;
                ratios = ratios3;
                blockRatios = blockRatios3;
                blockRates = blockRates2;
                applyMacroBlockLimits(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, blockSize2.getWidth(), blockSize2.getHeight(), alignment2.getWidth(), alignment2.getHeight());
                if ((this.mParent.mError & 2) == 0) {
                    widths2 = heights;
                    heights2 = widths;
                    frameRates3 = frameRates;
                    ratios2 = ratios;
                    blockRatios2 = blockRatios;
                } else if (this.mAllowMbOverride) {
                    widths2 = heights;
                    heights2 = widths;
                    frameRates3 = frameRates;
                    ratios2 = ratios;
                    blockRatios2 = blockRatios;
                } else {
                    heights2 = widths;
                    if (heights2 != null) {
                        this.mWidthRange = this.mWidthRange.intersect(heights2);
                    }
                    widths2 = heights;
                    if (widths2 != null) {
                        this.mHeightRange = this.mHeightRange.intersect(widths2);
                    }
                    if (counts != null) {
                        this.mBlockCountRange = this.mBlockCountRange.intersect(Utils.factorRange(counts, ((this.mBlockWidth * this.mBlockHeight) / blockSize2.getWidth()) / blockSize2.getHeight()));
                    }
                    if (blockRates != null) {
                        this.mBlocksPerSecondRange = this.mBlocksPerSecondRange.intersect(Utils.factorRange(blockRates, (long) (((this.mBlockWidth * this.mBlockHeight) / blockSize2.getWidth()) / blockSize2.getHeight())));
                    }
                    blockRatios2 = blockRatios;
                    if (blockRatios2 != null) {
                        this.mBlockAspectRatioRange = this.mBlockAspectRatioRange.intersect(Utils.scaleRange(blockRatios2, this.mBlockHeight / blockSize2.getHeight(), this.mBlockWidth / blockSize2.getWidth()));
                    }
                    ratios2 = ratios;
                    if (ratios2 != null) {
                        this.mAspectRatioRange = this.mAspectRatioRange.intersect(ratios2);
                    }
                    frameRates3 = frameRates;
                    if (frameRates3 != null) {
                        this.mFrameRateRange = this.mFrameRateRange.intersect(frameRates3);
                    }
                    if (extend != null) {
                        this.mBitrateRange = this.mBitrateRange.intersect(extend);
                    }
                    updateLimits();
                }
                if (heights2 != null) {
                    this.mWidthRange = MediaCodecInfo.SIZE_RANGE.intersect(heights2);
                }
                if (widths2 != null) {
                    this.mHeightRange = MediaCodecInfo.SIZE_RANGE.intersect(widths2);
                }
                if (counts != null) {
                    this.mBlockCountRange = MediaCodecInfo.POSITIVE_INTEGERS.intersect(Utils.factorRange(counts, ((this.mBlockWidth * this.mBlockHeight) / blockSize2.getWidth()) / blockSize2.getHeight()));
                }
                if (blockRates != null) {
                    this.mBlocksPerSecondRange = MediaCodecInfo.POSITIVE_LONGS.intersect(Utils.factorRange(blockRates, (long) (((this.mBlockWidth * this.mBlockHeight) / blockSize2.getWidth()) / blockSize2.getHeight())));
                }
                if (blockRatios2 != null) {
                    this.mBlockAspectRatioRange = MediaCodecInfo.POSITIVE_RATIONALS.intersect(Utils.scaleRange(blockRatios2, this.mBlockHeight / blockSize2.getHeight(), this.mBlockWidth / blockSize2.getWidth()));
                }
                if (ratios2 != null) {
                    this.mAspectRatioRange = MediaCodecInfo.POSITIVE_RATIONALS.intersect(ratios2);
                }
                if (frameRates3 != null) {
                    this.mFrameRateRange = MediaCodecInfo.FRAME_RATE_RANGE.intersect(frameRates3);
                }
                if (extend != null) {
                    if ((this.mParent.mError & 2) != 0) {
                        this.mBitrateRange = MediaCodecInfo.BITRATE_RANGE.intersect(extend);
                    } else {
                        this.mBitrateRange = this.mBitrateRange.intersect(extend);
                    }
                }
                updateLimits();
            }
            extend = bitRates;
            MediaCodecInfo.checkPowerOfTwo(blockSize2.getWidth(), "block-size width must be power of two");
            MediaCodecInfo.checkPowerOfTwo(blockSize2.getHeight(), "block-size height must be power of two");
            MediaCodecInfo.checkPowerOfTwo(alignment2.getWidth(), "alignment width must be power of two");
            MediaCodecInfo.checkPowerOfTwo(alignment2.getHeight(), "alignment height must be power of two");
            heights = heights2;
            frameRates = frameRates3;
            widths = widths2;
            ratios = ratios3;
            blockRatios = blockRatios3;
            blockRates = blockRates2;
            applyMacroBlockLimits(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, blockSize2.getWidth(), blockSize2.getHeight(), alignment2.getWidth(), alignment2.getHeight());
            if ((this.mParent.mError & 2) == 0) {
            }
            if (heights2 != null) {
            }
            if (widths2 != null) {
            }
            if (counts != null) {
            }
            if (blockRates != null) {
            }
            if (blockRatios2 != null) {
            }
            if (ratios2 != null) {
            }
            if (frameRates3 != null) {
            }
            if (extend != null) {
            }
            updateLimits();
        }

        private void applyBlockLimits(int blockWidth, int blockHeight, Range<Integer> counts, Range<Long> rates, Range<Rational> ratios) {
            MediaCodecInfo.checkPowerOfTwo(blockWidth, "blockWidth must be a power of two");
            MediaCodecInfo.checkPowerOfTwo(blockHeight, "blockHeight must be a power of two");
            int newBlockWidth = Math.max(blockWidth, this.mBlockWidth);
            int newBlockHeight = Math.max(blockHeight, this.mBlockHeight);
            int factor = ((newBlockWidth * newBlockHeight) / this.mBlockWidth) / this.mBlockHeight;
            if (factor != 1) {
                this.mBlockCountRange = Utils.factorRange(this.mBlockCountRange, factor);
                this.mBlocksPerSecondRange = Utils.factorRange(this.mBlocksPerSecondRange, (long) factor);
                this.mBlockAspectRatioRange = Utils.scaleRange(this.mBlockAspectRatioRange, newBlockHeight / this.mBlockHeight, newBlockWidth / this.mBlockWidth);
                this.mHorizontalBlockRange = Utils.factorRange(this.mHorizontalBlockRange, newBlockWidth / this.mBlockWidth);
                this.mVerticalBlockRange = Utils.factorRange(this.mVerticalBlockRange, newBlockHeight / this.mBlockHeight);
            }
            int factor2 = ((newBlockWidth * newBlockHeight) / blockWidth) / blockHeight;
            if (factor2 != 1) {
                counts = Utils.factorRange((Range) counts, factor2);
                rates = Utils.factorRange((Range) rates, (long) factor2);
                ratios = Utils.scaleRange(ratios, newBlockHeight / blockHeight, newBlockWidth / blockWidth);
            }
            this.mBlockCountRange = this.mBlockCountRange.intersect(counts);
            this.mBlocksPerSecondRange = this.mBlocksPerSecondRange.intersect(rates);
            this.mBlockAspectRatioRange = this.mBlockAspectRatioRange.intersect(ratios);
            this.mBlockWidth = newBlockWidth;
            this.mBlockHeight = newBlockHeight;
        }

        private void applyAlignment(int widthAlignment, int heightAlignment) {
            MediaCodecInfo.checkPowerOfTwo(widthAlignment, "widthAlignment must be a power of two");
            MediaCodecInfo.checkPowerOfTwo(heightAlignment, "heightAlignment must be a power of two");
            if (widthAlignment > this.mBlockWidth || heightAlignment > this.mBlockHeight) {
                applyBlockLimits(Math.max(widthAlignment, this.mBlockWidth), Math.max(heightAlignment, this.mBlockHeight), MediaCodecInfo.POSITIVE_INTEGERS, MediaCodecInfo.POSITIVE_LONGS, MediaCodecInfo.POSITIVE_RATIONALS);
            }
            this.mWidthAlignment = Math.max(widthAlignment, this.mWidthAlignment);
            this.mHeightAlignment = Math.max(heightAlignment, this.mHeightAlignment);
            this.mWidthRange = Utils.alignRange(this.mWidthRange, this.mWidthAlignment);
            this.mHeightRange = Utils.alignRange(this.mHeightRange, this.mHeightAlignment);
        }

        private void updateLimits() {
            this.mHorizontalBlockRange = this.mHorizontalBlockRange.intersect(Utils.factorRange(this.mWidthRange, this.mBlockWidth));
            this.mHorizontalBlockRange = this.mHorizontalBlockRange.intersect(Range.create(Integer.valueOf(((Integer) this.mBlockCountRange.getLower()).intValue() / ((Integer) this.mVerticalBlockRange.getUpper()).intValue()), Integer.valueOf(((Integer) this.mBlockCountRange.getUpper()).intValue() / ((Integer) this.mVerticalBlockRange.getLower()).intValue())));
            this.mVerticalBlockRange = this.mVerticalBlockRange.intersect(Utils.factorRange(this.mHeightRange, this.mBlockHeight));
            this.mVerticalBlockRange = this.mVerticalBlockRange.intersect(Range.create(Integer.valueOf(((Integer) this.mBlockCountRange.getLower()).intValue() / ((Integer) this.mHorizontalBlockRange.getUpper()).intValue()), Integer.valueOf(((Integer) this.mBlockCountRange.getUpper()).intValue() / ((Integer) this.mHorizontalBlockRange.getLower()).intValue())));
            this.mBlockCountRange = this.mBlockCountRange.intersect(Range.create(Integer.valueOf(((Integer) this.mHorizontalBlockRange.getLower()).intValue() * ((Integer) this.mVerticalBlockRange.getLower()).intValue()), Integer.valueOf(((Integer) this.mHorizontalBlockRange.getUpper()).intValue() * ((Integer) this.mVerticalBlockRange.getUpper()).intValue())));
            this.mBlockAspectRatioRange = this.mBlockAspectRatioRange.intersect(new Rational(((Integer) this.mHorizontalBlockRange.getLower()).intValue(), ((Integer) this.mVerticalBlockRange.getUpper()).intValue()), new Rational(((Integer) this.mHorizontalBlockRange.getUpper()).intValue(), ((Integer) this.mVerticalBlockRange.getLower()).intValue()));
            this.mWidthRange = this.mWidthRange.intersect(Integer.valueOf(((((Integer) this.mHorizontalBlockRange.getLower()).intValue() - 1) * this.mBlockWidth) + this.mWidthAlignment), Integer.valueOf(((Integer) this.mHorizontalBlockRange.getUpper()).intValue() * this.mBlockWidth));
            this.mHeightRange = this.mHeightRange.intersect(Integer.valueOf(((((Integer) this.mVerticalBlockRange.getLower()).intValue() - 1) * this.mBlockHeight) + this.mHeightAlignment), Integer.valueOf(((Integer) this.mVerticalBlockRange.getUpper()).intValue() * this.mBlockHeight));
            this.mAspectRatioRange = this.mAspectRatioRange.intersect(new Rational(((Integer) this.mWidthRange.getLower()).intValue(), ((Integer) this.mHeightRange.getUpper()).intValue()), new Rational(((Integer) this.mWidthRange.getUpper()).intValue(), ((Integer) this.mHeightRange.getLower()).intValue()));
            this.mSmallerDimensionUpperLimit = Math.min(this.mSmallerDimensionUpperLimit, Math.min(((Integer) this.mWidthRange.getUpper()).intValue(), ((Integer) this.mHeightRange.getUpper()).intValue()));
            this.mBlocksPerSecondRange = this.mBlocksPerSecondRange.intersect(Long.valueOf(((long) ((Integer) this.mBlockCountRange.getLower()).intValue()) * ((long) ((Integer) this.mFrameRateRange.getLower()).intValue())), Long.valueOf(((long) ((Integer) this.mBlockCountRange.getUpper()).intValue()) * ((long) ((Integer) this.mFrameRateRange.getUpper()).intValue())));
            this.mFrameRateRange = this.mFrameRateRange.intersect(Integer.valueOf((int) (((Long) this.mBlocksPerSecondRange.getLower()).longValue() / ((long) ((Integer) this.mBlockCountRange.getUpper()).intValue()))), Integer.valueOf((int) (((double) ((Long) this.mBlocksPerSecondRange.getUpper()).longValue()) / ((double) ((Integer) this.mBlockCountRange.getLower()).intValue()))));
        }

        private void applyMacroBlockLimits(int maxHorizontalBlocks, int maxVerticalBlocks, int maxBlocks, long maxBlocksPerSecond, int blockWidth, int blockHeight, int widthAlignment, int heightAlignment) {
            applyMacroBlockLimits(1, 1, maxHorizontalBlocks, maxVerticalBlocks, maxBlocks, maxBlocksPerSecond, blockWidth, blockHeight, widthAlignment, heightAlignment);
        }

        private void applyMacroBlockLimits(int minHorizontalBlocks, int minVerticalBlocks, int maxHorizontalBlocks, int maxVerticalBlocks, int maxBlocks, long maxBlocksPerSecond, int blockWidth, int blockHeight, int widthAlignment, int heightAlignment) {
            int i = maxHorizontalBlocks;
            int i2 = maxVerticalBlocks;
            applyAlignment(widthAlignment, heightAlignment);
            applyBlockLimits(blockWidth, blockHeight, Range.create(Integer.valueOf(1), Integer.valueOf(maxBlocks)), Range.create(Long.valueOf(1), Long.valueOf(maxBlocksPerSecond)), Range.create(new Rational(1, i2), new Rational(i, 1)));
            this.mHorizontalBlockRange = this.mHorizontalBlockRange.intersect(Integer.valueOf(Utils.divUp(minHorizontalBlocks, this.mBlockWidth / blockWidth)), Integer.valueOf(i / (this.mBlockWidth / blockWidth)));
            this.mVerticalBlockRange = this.mVerticalBlockRange.intersect(Integer.valueOf(Utils.divUp(minVerticalBlocks, this.mBlockHeight / blockHeight)), Integer.valueOf(i2 / (this.mBlockHeight / blockHeight)));
        }

        /* JADX WARNING: Removed duplicated region for block: B:288:0x01a7 A:{SYNTHETIC} */
        /* JADX WARNING: Removed duplicated region for block: B:49:0x01a3  */
        /* JADX WARNING: Removed duplicated region for block: B:49:0x01a3  */
        /* JADX WARNING: Removed duplicated region for block: B:288:0x01a7 A:{SYNTHETIC} */
        /* JADX WARNING: Missing block: B:59:0x0252, code skipped:
            r29 = r0;
            r30 = r13;
            r31 = r14;
            r8 = 0;
            r11 = 0;
            r14 = 0;
            r0 = 0;
            r13 = 0;
            r1 = r27;
     */
        /* JADX WARNING: Missing block: B:73:0x0352, code skipped:
            r29 = r0;
            r30 = r13;
            r31 = r14;
            r8 = r16;
            r11 = r18;
            r14 = r19;
            r0 = r20;
            r13 = r21;
     */
        /* JADX WARNING: Missing block: B:74:0x0362, code skipped:
            if (r22 == false) goto L_0x0368;
     */
        /* JADX WARNING: Missing block: B:75:0x0364, code skipped:
            r17 = r17 & -5;
     */
        /* JADX WARNING: Missing block: B:76:0x0368, code skipped:
            r33 = r14;
            r32 = r15;
            r4 = java.lang.Math.max((long) r1, r4);
            r6 = java.lang.Math.max(r8, r6);
            r2 = java.lang.Math.max(r11 * 1000, r2);
            r9 = java.lang.Math.max(r0, r9);
            r7 = java.lang.Math.max(r13, r7);
            r3 = java.lang.Math.max(r33, r3);
            r10 = r10 + 1;
            r8 = r28;
            r13 = r30;
            r14 = r31;
            r15 = r32;
     */
        /* JADX WARNING: Missing block: B:85:0x0438, code skipped:
            r35 = r0;
            r36 = r11;
            r37 = r14;
            r6 = 0;
            r7 = 0;
            r14 = 0;
            r0 = 0;
            r11 = 0;
            r1 = r34;
     */
        /* JADX WARNING: Missing block: B:124:0x0589, code skipped:
            r35 = r0;
            r36 = r11;
            r37 = r14;
            r6 = r18;
            r7 = r19;
            r14 = r20;
            r0 = r21;
            r11 = r22;
     */
        /* JADX WARNING: Missing block: B:131:0x05eb, code skipped:
            if (r24 == false) goto L_0x05ef;
     */
        /* JADX WARNING: Missing block: B:132:0x05ed, code skipped:
            r17 = r17 & -5;
     */
        /* JADX WARNING: Missing block: B:133:0x05ef, code skipped:
            r38 = r13;
            r4 = java.lang.Math.max((long) r1, r4);
            r8 = java.lang.Math.max(r6, r8);
            r2 = java.lang.Math.max(r7 * 1000, r2);
     */
        /* JADX WARNING: Missing block: B:134:0x0600, code skipped:
            if (r23 == false) goto L_0x0610;
     */
        /* JADX WARNING: Missing block: B:135:0x0602, code skipped:
            r12 = java.lang.Math.max(r0, r15);
            r9 = java.lang.Math.max(r11, r9);
            r3 = java.lang.Math.max(r14, r3);
            r15 = r12;
     */
        /* JADX WARNING: Missing block: B:136:0x0610, code skipped:
            r12 = (int) java.lang.Math.sqrt((double) (r6 * 2));
            r13 = java.lang.Math.max(r12, r15);
            r9 = java.lang.Math.max(r12, r9);
            r3 = java.lang.Math.max(java.lang.Math.max(r14, 60), r3);
            r15 = r13;
     */
        /* JADX WARNING: Missing block: B:137:0x062c, code skipped:
            r10 = r10 + 1;
            r11 = r36;
            r14 = r37;
            r13 = r38;
            r12 = r54;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void applyLevelLimits() {
            int errors;
            int i;
            VideoCapabilities minWidth = this;
            int maxDPBBlocks = 0;
            CodecProfileLevel[] profileLevels = minWidth.mParent.profileLevels;
            String mime = minWidth.mParent.getMimeType();
            int i2 = 4;
            int i3;
            int maxBlocks;
            int maxBps;
            int maxDPBBlocks2;
            int MBPS;
            int FS;
            int BR;
            int DPB;
            boolean supported;
            String str;
            int BR2;
            int MBPS2;
            int FS2;
            int DPB2;
            int i4;
            String str2;
            StringBuilder stringBuilder;
            CodecProfileLevel[] profileLevels2;
            CodecProfileLevel[] codecProfileLevelArr;
            if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                maxDPBBlocks = profileLevels.length;
                errors = 4;
                i3 = 0;
                maxBlocks = 99;
                maxBps = 64000;
                long maxBlocksPerSecond = 1485;
                maxDPBBlocks2 = 396;
                while (i3 < maxDPBBlocks) {
                    CodecProfileLevel profileLevel = profileLevels[i3];
                    MBPS = 0;
                    FS = 0;
                    BR = 0;
                    DPB = 0;
                    supported = true;
                    switch (profileLevel.level) {
                        case 1:
                            MBPS = 1485;
                            FS = 99;
                            BR = 64;
                            DPB = 396;
                            break;
                        case 2:
                            MBPS = 1485;
                            FS = 99;
                            BR = 128;
                            DPB = 396;
                            break;
                        case 4:
                            MBPS = FingerprintManager.HW_FINGERPRINT_ACQUIRED_VENDOR_BASE_END;
                            FS = 396;
                            BR = 192;
                            DPB = 900;
                            break;
                        case 8:
                            MBPS = BluetoothHealth.HEALTH_OPERATION_SUCCESS;
                            FS = 396;
                            BR = 384;
                            DPB = 2376;
                            break;
                        case 16:
                            MBPS = 11880;
                            FS = 396;
                            BR = 768;
                            DPB = 2376;
                            break;
                        case 32:
                            MBPS = 11880;
                            FS = 396;
                            BR = 2000;
                            DPB = 2376;
                            break;
                        case 64:
                            MBPS = 19800;
                            FS = 792;
                            BR = 4000;
                            DPB = 4752;
                            break;
                        case 128:
                            MBPS = 20250;
                            FS = 1620;
                            BR = 4000;
                            DPB = 8100;
                            break;
                        case 256:
                            MBPS = 40500;
                            FS = 1620;
                            BR = 10000;
                            DPB = 8100;
                            break;
                        case 512:
                            MBPS = 108000;
                            FS = 3600;
                            BR = 14000;
                            DPB = 18000;
                            break;
                        case 1024:
                            MBPS = 216000;
                            FS = 5120;
                            BR = HwMediaMonitorUtils.TYPE_MEDIA_RECORD_DTS_COUNT;
                            DPB = MtpConstants.DEVICE_PROPERTY_UNDEFINED;
                            break;
                        case 2048:
                            MBPS = 245760;
                            FS = 8192;
                            BR = HwMediaMonitorUtils.TYPE_MEDIA_RECORD_DTS_COUNT;
                            DPB = 32768;
                            break;
                        case 4096:
                            MBPS = 245760;
                            FS = 8192;
                            BR = SQLiteDatabase.SQLITE_MAX_LIKE_PATTERN_LENGTH;
                            DPB = 32768;
                            break;
                        case 8192:
                            MBPS = 522240;
                            FS = 8704;
                            BR = SQLiteDatabase.SQLITE_MAX_LIKE_PATTERN_LENGTH;
                            DPB = 34816;
                            break;
                        case 16384:
                            MBPS = 589824;
                            FS = 22080;
                            BR = 135000;
                            DPB = 110400;
                            break;
                        case 32768:
                            MBPS = 983040;
                            FS = 36864;
                            BR = 240000;
                            DPB = 184320;
                            break;
                        case 65536:
                            MBPS = 2073600;
                            FS = 36864;
                            BR = 240000;
                            DPB = 184320;
                            break;
                        default:
                            str = TAG;
                            BR2 = new StringBuilder();
                            BR2.append("Unrecognized level ");
                            BR2.append(profileLevel.level);
                            BR2.append(" for ");
                            BR2.append(mime);
                            Log.w(str, BR2.toString());
                            errors |= 1;
                            break;
                    }
                    MBPS2 = MBPS;
                    FS2 = FS;
                    BR2 = BR;
                    DPB2 = DPB;
                    i = profileLevel.profile;
                    if (i != i2) {
                        if (i != 8) {
                            if (i == 16) {
                                i4 = maxDPBBlocks;
                                BR2 *= FingerprintManager.HW_FINGERPRINT_ACQUIRED_VENDOR_BASE_END;
                            } else if (!(i == 32 || i == 64)) {
                                if (i != 65536) {
                                    if (i != 524288) {
                                        switch (i) {
                                            case 1:
                                            case 2:
                                                break;
                                            default:
                                                str2 = TAG;
                                                stringBuilder = new StringBuilder();
                                                i4 = maxDPBBlocks;
                                                stringBuilder.append("Unrecognized profile ");
                                                stringBuilder.append(profileLevel.profile);
                                                stringBuilder.append(" for ");
                                                stringBuilder.append(mime);
                                                Log.w(str2, stringBuilder.toString());
                                                errors |= 1;
                                                BR2 *= 1000;
                                                break;
                                        }
                                    }
                                }
                                i4 = maxDPBBlocks;
                                BR2 *= 1000;
                            }
                            if (supported) {
                                errors &= -5;
                            }
                            profileLevels2 = profileLevels;
                            maxBlocksPerSecond = Math.max((long) MBPS2, maxBlocksPerSecond);
                            maxBlocks = Math.max(FS2, maxBlocks);
                            maxBps = Math.max(BR2, maxBps);
                            maxDPBBlocks2 = Math.max(maxDPBBlocks2, DPB2);
                            i3++;
                            maxDPBBlocks = i4;
                            profileLevels = profileLevels2;
                            i2 = 4;
                        }
                        i4 = maxDPBBlocks;
                        BR2 *= 1250;
                        if (supported) {
                        }
                        profileLevels2 = profileLevels;
                        maxBlocksPerSecond = Math.max((long) MBPS2, maxBlocksPerSecond);
                        maxBlocks = Math.max(FS2, maxBlocks);
                        maxBps = Math.max(BR2, maxBps);
                        maxDPBBlocks2 = Math.max(maxDPBBlocks2, DPB2);
                        i3++;
                        maxDPBBlocks = i4;
                        profileLevels = profileLevels2;
                        i2 = 4;
                    }
                    i4 = maxDPBBlocks;
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported profile ");
                    stringBuilder.append(profileLevel.profile);
                    stringBuilder.append(" for ");
                    stringBuilder.append(mime);
                    Log.w(str2, stringBuilder.toString());
                    errors |= 2;
                    supported = false;
                    BR2 *= 1000;
                    if (supported) {
                    }
                    profileLevels2 = profileLevels;
                    maxBlocksPerSecond = Math.max((long) MBPS2, maxBlocksPerSecond);
                    maxBlocks = Math.max(FS2, maxBlocks);
                    maxBps = Math.max(BR2, maxBps);
                    maxDPBBlocks2 = Math.max(maxDPBBlocks2, DPB2);
                    i3++;
                    maxDPBBlocks = i4;
                    profileLevels = profileLevels2;
                    i2 = 4;
                }
                profileLevels2 = profileLevels;
                i3 = (int) Math.sqrt((double) (maxBlocks * 8));
                i = maxDPBBlocks2;
                long maxBlocksPerSecond2 = maxBlocksPerSecond;
                MBPS = maxBps;
                minWidth.applyMacroBlockLimits(i3, i3, maxBlocks, maxBlocksPerSecond2, 16, 16, 1, 1);
                i = MBPS;
                codecProfileLevelArr = profileLevels2;
                MBPS = maxBlocksPerSecond2;
                maxDPBBlocks = mime;
            } else {
                profileLevels2 = profileLevels;
                int maxBps2;
                int maxRate;
                int W;
                int FS3;
                String str3;
                StringBuilder stringBuilder2;
                CodecProfileLevel[] profileLevels3;
                int maxBps3;
                long maxBlocksPerSecond3;
                String str4;
                if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_MPEG2)) {
                    profileLevels = profileLevels2;
                    FS2 = profileLevels.length;
                    errors = 4;
                    i2 = 11;
                    i3 = 0;
                    maxBlocks = 1485;
                    BR2 = 99;
                    maxBps2 = 64000;
                    maxRate = 15;
                    MBPS2 = 9;
                    while (i3 < FS2) {
                        W = profileLevels[i3];
                        supported = true;
                        int MBPS3;
                        int i5;
                        switch (W.profile) {
                            case 0:
                                MBPS3 = 0;
                                i5 = FS2;
                                if (W.level == 1) {
                                    FS = 30;
                                    BR = 45;
                                    DPB = 36;
                                    maxDPBBlocks2 = 40500;
                                    FS3 = 1620;
                                    MBPS = 15000;
                                    break;
                                }
                                str3 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unrecognized profile/level ");
                                stringBuilder2.append(W.profile);
                                stringBuilder2.append("/");
                                stringBuilder2.append(W.level);
                                stringBuilder2.append(" for ");
                                stringBuilder2.append(mime);
                                Log.w(str3, stringBuilder2.toString());
                                errors |= 1;
                                break;
                            case 1:
                                MBPS3 = 0;
                                i5 = FS2;
                                switch (W.level) {
                                    case 0:
                                        FS = 30;
                                        BR = 22;
                                        DPB = 18;
                                        maxDPBBlocks2 = 11880;
                                        FS3 = 396;
                                        MBPS = 4000;
                                        break;
                                    case 1:
                                        FS = 30;
                                        BR = 45;
                                        DPB = 36;
                                        maxDPBBlocks2 = 40500;
                                        FS3 = 1620;
                                        MBPS = 15000;
                                        break;
                                    case 2:
                                        FS = 60;
                                        BR = 90;
                                        DPB = 68;
                                        maxDPBBlocks2 = 183600;
                                        FS3 = 6120;
                                        MBPS = 60000;
                                        break;
                                    case 3:
                                        FS = 60;
                                        BR = 120;
                                        DPB = 68;
                                        maxDPBBlocks2 = 244800;
                                        FS3 = 8160;
                                        MBPS = 80000;
                                        break;
                                    case 4:
                                        FS = 60;
                                        BR = 120;
                                        DPB = 68;
                                        maxDPBBlocks2 = 489600;
                                        FS3 = 8160;
                                        MBPS = 80000;
                                        break;
                                    default:
                                        str3 = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Unrecognized profile/level ");
                                        stringBuilder2.append(W.profile);
                                        stringBuilder2.append("/");
                                        stringBuilder2.append(W.level);
                                        stringBuilder2.append(" for ");
                                        stringBuilder2.append(mime);
                                        Log.w(str3, stringBuilder2.toString());
                                        errors |= 1;
                                        break;
                                }
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                                String str5 = TAG;
                                MBPS3 = 0;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                i5 = FS2;
                                stringBuilder3.append("Unsupported profile ");
                                stringBuilder3.append(W.profile);
                                stringBuilder3.append(" for ");
                                stringBuilder3.append(mime);
                                Log.i(str5, stringBuilder3.toString());
                                errors |= 2;
                                supported = false;
                                break;
                            default:
                                MBPS3 = 0;
                                i5 = FS2;
                                str3 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unrecognized profile ");
                                stringBuilder2.append(W.profile);
                                stringBuilder2.append(" for ");
                                stringBuilder2.append(mime);
                                Log.w(str3, stringBuilder2.toString());
                                errors |= 1;
                                break;
                        }
                    }
                    profileLevels3 = profileLevels;
                    String mime2 = mime;
                    maxBps3 = maxBps2;
                    mime = maxRate;
                    maxBlocksPerSecond3 = maxBlocks;
                    minWidth.applyMacroBlockLimits(i2, MBPS2, BR2, maxBlocks, 16, 16, 1, 1);
                    minWidth.mFrameRateRange = minWidth.mFrameRateRange.intersect(Integer.valueOf(12), Integer.valueOf(mime));
                    i = maxBps3;
                    codecProfileLevelArr = profileLevels3;
                    str4 = mime2;
                } else {
                    int maxDPBBlocks3 = 0;
                    profileLevels3 = profileLevels2;
                    str4 = mime;
                    int maxWidth;
                    int H;
                    StringBuilder stringBuilder4;
                    String str6;
                    CodecProfileLevel[] profileLevels4;
                    if (str4.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_MPEG4)) {
                        profileLevels = profileLevels3;
                        i = profileLevels.length;
                        errors = 4;
                        maxWidth = 11;
                        i3 = 0;
                        maxBlocks = 1485;
                        FS2 = 99;
                        maxBps2 = 64000;
                        maxRate = 15;
                        i2 = 9;
                        while (i3 < i) {
                            W = profileLevels[i3];
                            boolean strict = false;
                            boolean supported2 = true;
                            int MBPS4;
                            switch (W.profile) {
                                case 1:
                                    MBPS4 = 0;
                                    maxDPBBlocks2 = W.level;
                                    if (maxDPBBlocks2 != 4) {
                                        if (maxDPBBlocks2 != 8) {
                                            if (maxDPBBlocks2 != 16) {
                                                if (maxDPBBlocks2 != 64) {
                                                    if (maxDPBBlocks2 != 128) {
                                                        if (maxDPBBlocks2 == 256) {
                                                            BR = 30;
                                                            DPB = 80;
                                                            H = 45;
                                                            maxDPBBlocks2 = 108000;
                                                            MBPS = 3600;
                                                            FS = 12000;
                                                            break;
                                                        }
                                                        switch (maxDPBBlocks2) {
                                                            case 1:
                                                                strict = true;
                                                                BR = 15;
                                                                DPB = 11;
                                                                H = 9;
                                                                maxDPBBlocks2 = 1485;
                                                                MBPS = 99;
                                                                FS = 64;
                                                                break;
                                                            case 2:
                                                                strict = true;
                                                                BR = 15;
                                                                DPB = 11;
                                                                H = 9;
                                                                maxDPBBlocks2 = 1485;
                                                                MBPS = 99;
                                                                FS = 128;
                                                                break;
                                                            default:
                                                                str3 = TAG;
                                                                stringBuilder4 = new StringBuilder();
                                                                stringBuilder4.append("Unrecognized profile/level ");
                                                                stringBuilder4.append(W.profile);
                                                                stringBuilder4.append("/");
                                                                stringBuilder4.append(W.level);
                                                                stringBuilder4.append(" for ");
                                                                stringBuilder4.append(str4);
                                                                Log.w(str3, stringBuilder4.toString());
                                                                errors |= 1;
                                                                break;
                                                        }
                                                    }
                                                    BR = 30;
                                                    DPB = 45;
                                                    H = 36;
                                                    maxDPBBlocks2 = 40500;
                                                    MBPS = 1620;
                                                    FS = 8000;
                                                    break;
                                                }
                                                BR = 30;
                                                DPB = 40;
                                                H = 30;
                                                maxDPBBlocks2 = 36000;
                                                MBPS = 1200;
                                                FS = 4000;
                                                break;
                                            }
                                            BR = 30;
                                            DPB = 22;
                                            H = 18;
                                            maxDPBBlocks2 = 11880;
                                            MBPS = 396;
                                            FS = 384;
                                            break;
                                        }
                                        BR = 30;
                                        DPB = 22;
                                        H = 18;
                                        maxDPBBlocks2 = 5940;
                                        MBPS = 396;
                                        FS = 128;
                                        break;
                                    }
                                    BR = 30;
                                    DPB = 11;
                                    H = 9;
                                    maxDPBBlocks2 = 1485;
                                    MBPS = 99;
                                    FS = 64;
                                    break;
                                case 2:
                                case 4:
                                case 8:
                                case 16:
                                case 32:
                                case 64:
                                case 128:
                                case 256:
                                case 512:
                                case 1024:
                                case 2048:
                                case 4096:
                                case 8192:
                                case 16384:
                                    MBPS4 = 0;
                                    str3 = TAG;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("Unsupported profile ");
                                    stringBuilder4.append(W.profile);
                                    stringBuilder4.append(" for ");
                                    stringBuilder4.append(str4);
                                    Log.i(str3, stringBuilder4.toString());
                                    errors |= 2;
                                    supported2 = false;
                                    break;
                                case 32768:
                                    BR2 = W.level;
                                    if (BR2 != 1 && BR2 != 4) {
                                        if (BR2 != 8) {
                                            if (BR2 != 16) {
                                                if (BR2 != 24) {
                                                    if (BR2 != 32) {
                                                        if (BR2 == 128) {
                                                            MBPS4 = 0;
                                                            BR = 30;
                                                            DPB = 45;
                                                            H = 36;
                                                            maxDPBBlocks2 = 48600;
                                                            MBPS = 1620;
                                                            FS = 8000;
                                                            break;
                                                        }
                                                        str6 = TAG;
                                                        StringBuilder stringBuilder5 = new StringBuilder();
                                                        MBPS4 = 0;
                                                        stringBuilder5.append("Unrecognized profile/level ");
                                                        stringBuilder5.append(W.profile);
                                                        stringBuilder5.append("/");
                                                        stringBuilder5.append(W.level);
                                                        stringBuilder5.append(" for ");
                                                        stringBuilder5.append(str4);
                                                        Log.w(str6, stringBuilder5.toString());
                                                        errors |= 1;
                                                        break;
                                                    }
                                                    MBPS4 = 0;
                                                    BR = 30;
                                                    DPB = 44;
                                                    H = 36;
                                                    maxDPBBlocks2 = 23760;
                                                    MBPS = 792;
                                                    FS = FingerprintManager.HW_FINGERPRINT_ACQUIRED_VENDOR_BASE_END;
                                                    break;
                                                }
                                                MBPS4 = 0;
                                                BR = 30;
                                                DPB = 22;
                                                H = 18;
                                                maxDPBBlocks2 = 11880;
                                                MBPS = 396;
                                                FS = 1500;
                                                break;
                                            }
                                            MBPS4 = 0;
                                            BR = 30;
                                            DPB = 22;
                                            H = 18;
                                            maxDPBBlocks2 = 11880;
                                            MBPS = 396;
                                            FS = 768;
                                            break;
                                        }
                                        MBPS4 = 0;
                                        BR = 30;
                                        DPB = 22;
                                        H = 18;
                                        maxDPBBlocks2 = 5940;
                                        MBPS = 396;
                                        FS = 384;
                                        break;
                                    }
                                    MBPS4 = 0;
                                    BR = 30;
                                    DPB = 11;
                                    H = 9;
                                    maxDPBBlocks2 = 2970;
                                    MBPS = 99;
                                    FS = 128;
                                    break;
                                    break;
                                default:
                                    MBPS4 = 0;
                                    str3 = TAG;
                                    BR2 = new StringBuilder();
                                    BR2.append("Unrecognized profile ");
                                    BR2.append(W.profile);
                                    BR2.append(" for ");
                                    BR2.append(str4);
                                    Log.w(str3, BR2.toString());
                                    errors |= 1;
                                    break;
                            }
                        }
                        String mime3 = str4;
                        profileLevels4 = profileLevels;
                        str4 = maxBps2;
                        maxBps3 = maxRate;
                        maxBlocksPerSecond3 = maxBlocks;
                        FS3 = FS2;
                        applyMacroBlockLimits(maxWidth, i2, FS2, maxBlocks, 16, 16, 1, 1);
                        this.mFrameRateRange = this.mFrameRateRange.intersect(Integer.valueOf(12), Integer.valueOf(maxBps3));
                        i = str4;
                        codecProfileLevelArr = profileLevels4;
                    } else {
                        profileLevels4 = profileLevels3;
                        CodecProfileLevel profileLevel2;
                        CodecProfileLevel[] profileLevels5;
                        int minWidth2;
                        VideoCapabilities thisR;
                        if (str4.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_H263)) {
                            BR2 = 9;
                            FS2 = 11;
                            i2 = 9;
                            profileLevels = profileLevels4;
                            maxWidth = profileLevels.length;
                            errors = 4;
                            MBPS = 16;
                            i = FS2;
                            long maxBlocksPerSecond4 = 1485;
                            W = 0;
                            MBPS2 = 99;
                            maxBps2 = 64000;
                            maxRate = 15;
                            while (W < maxWidth) {
                                int FR;
                                profileLevel2 = profileLevels[W];
                                FS = 0;
                                BR = 0;
                                DPB = 0;
                                H = 0;
                                int H2 = 0;
                                DPB2 = i;
                                i4 = i2;
                                boolean strict2 = false;
                                i3 = profileLevel2.level;
                                int i6 = maxWidth;
                                if (i3 == 4) {
                                    profileLevels5 = profileLevels;
                                    strict2 = true;
                                    DPB = 30;
                                    H = 22;
                                    H2 = 18;
                                    BR = 6;
                                    FS = (22 * 18) * 30;
                                } else if (i3 == 8) {
                                    profileLevels5 = profileLevels;
                                    strict2 = true;
                                    DPB = 30;
                                    H = 22;
                                    H2 = 18;
                                    BR = 32;
                                    FS = (22 * 18) * 30;
                                } else if (i3 == 16) {
                                    profileLevels5 = profileLevels;
                                    boolean z = profileLevel2.profile == 1 || profileLevel2.profile == 4;
                                    strict2 = z;
                                    if (!strict2) {
                                        DPB2 = 1;
                                        i4 = 1;
                                        MBPS = 4;
                                    }
                                    DPB = 15;
                                    H = 11;
                                    H2 = 9;
                                    BR = 2;
                                    FS = (11 * 9) * 15;
                                } else if (i3 == 32) {
                                    profileLevels5 = profileLevels;
                                    DPB2 = 1;
                                    i4 = 1;
                                    MBPS = 4;
                                    DPB = 60;
                                    H = 22;
                                    H2 = 18;
                                    BR = 64;
                                    FS = (22 * 18) * 50;
                                } else if (i3 == 64) {
                                    profileLevels5 = profileLevels;
                                    DPB2 = 1;
                                    i4 = 1;
                                    MBPS = 4;
                                    DPB = 60;
                                    H = 45;
                                    H2 = 18;
                                    BR = 128;
                                    FS = (45 * 18) * 50;
                                } else if (i3 != 128) {
                                    switch (i3) {
                                        case 1:
                                            profileLevels5 = profileLevels;
                                            strict2 = true;
                                            DPB = 15;
                                            H = 11;
                                            H2 = 9;
                                            BR = 1;
                                            FS = (11 * 9) * 15;
                                            break;
                                        case 2:
                                            profileLevels5 = profileLevels;
                                            strict2 = true;
                                            DPB = 30;
                                            H = 22;
                                            H2 = 18;
                                            BR = 2;
                                            FS = (22 * 18) * 15;
                                            break;
                                        default:
                                            i3 = TAG;
                                            maxWidth = new StringBuilder();
                                            profileLevels5 = profileLevels;
                                            maxWidth.append("Unrecognized profile/level ");
                                            maxWidth.append(profileLevel2.profile);
                                            maxWidth.append("/");
                                            maxWidth.append(profileLevel2.level);
                                            maxWidth.append(" for ");
                                            maxWidth.append(str4);
                                            Log.w(i3, maxWidth.toString());
                                            errors |= 1;
                                            break;
                                    }
                                } else {
                                    profileLevels5 = profileLevels;
                                    DPB2 = 1;
                                    i4 = 1;
                                    MBPS = 4;
                                    DPB = 60;
                                    H = 45;
                                    H2 = 36;
                                    BR = 256;
                                    FS = (45 * 36) * 50;
                                }
                                int i7 = W;
                                i3 = FS;
                                W = DPB;
                                maxBps3 = H;
                                maxWidth = H2;
                                int minHeight = i2;
                                i2 = profileLevel2.profile;
                                int minWidth3 = i;
                                if (!(i2 == 4 || i2 == 8 || i2 == 16 || i2 == 32 || i2 == 64 || i2 == 128 || i2 == 256)) {
                                    switch (i2) {
                                        case 1:
                                        case 2:
                                            break;
                                        default:
                                            str2 = TAG;
                                            stringBuilder = new StringBuilder();
                                            FR = W;
                                            stringBuilder.append("Unrecognized profile ");
                                            stringBuilder.append(profileLevel2.profile);
                                            stringBuilder.append(" for ");
                                            stringBuilder.append(str4);
                                            Log.w(str2, stringBuilder.toString());
                                            errors |= 1;
                                            break;
                                    }
                                }
                                FR = W;
                                if (strict2) {
                                    W = 11;
                                    i2 = 9;
                                } else {
                                    minWidth.mAllowMbOverride = true;
                                    W = DPB2;
                                    i2 = i4;
                                }
                                errors &= -5;
                                maxBlocksPerSecond4 = Math.max((long) i3, maxBlocksPerSecond4);
                                MBPS2 = Math.max(maxBps3 * maxWidth, MBPS2);
                                maxBps2 = Math.max(64000 * BR, maxBps2);
                                FS2 = Math.max(maxBps3, FS2);
                                BR2 = Math.max(maxWidth, BR2);
                                maxRate = Math.max(FR, maxRate);
                                minWidth2 = Math.min(W, minWidth3);
                                int i8 = i3;
                                i2 = Math.min(i2, minHeight);
                                W = i7 + 1;
                                i = minWidth2;
                                maxWidth = i6;
                                profileLevels = profileLevels5;
                                minWidth = this;
                            }
                            i3 = i2;
                            minWidth2 = i;
                            profileLevels5 = profileLevels;
                            if (!this.mAllowMbOverride) {
                                this.mBlockAspectRatioRange = Range.create(new Rational(11, 9), new Rational(11, 9));
                            }
                            maxWidth = maxBps2;
                            int maxRate2 = maxRate;
                            long maxBlocksPerSecond5 = maxBlocksPerSecond4;
                            DPB = MBPS2;
                            applyMacroBlockLimits(minWidth2, i3, FS2, BR2, MBPS2, maxBlocksPerSecond5, 16, 16, MBPS, MBPS);
                            this.mFrameRateRange = Range.create(Integer.valueOf(1), Integer.valueOf(maxRate2));
                            i = maxWidth;
                            maxBlocksPerSecond3 = maxBlocksPerSecond5;
                            FS3 = DPB;
                            codecProfileLevelArr = profileLevels5;
                        } else {
                            thisR = minWidth;
                            profileLevels5 = profileLevels4;
                            CodecProfileLevel[] profileLevels6;
                            if (str4.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_VP8)) {
                                i = 100000000;
                                CodecProfileLevel[] profileLevels7 = profileLevels5;
                                errors = 4;
                                for (CodecProfileLevel profileLevel3 : profileLevels7) {
                                    String str7;
                                    StringBuilder stringBuilder6;
                                    maxRate = profileLevel3.level;
                                    if (!(maxRate == 4 || maxRate == 8)) {
                                        switch (maxRate) {
                                            case 1:
                                            case 2:
                                                break;
                                            default:
                                                str7 = TAG;
                                                stringBuilder6 = new StringBuilder();
                                                stringBuilder6.append("Unrecognized level ");
                                                stringBuilder6.append(profileLevel3.level);
                                                stringBuilder6.append(" for ");
                                                stringBuilder6.append(str4);
                                                Log.w(str7, stringBuilder6.toString());
                                                errors |= 1;
                                                break;
                                        }
                                    }
                                    if (profileLevel3.profile != 1) {
                                        str7 = TAG;
                                        stringBuilder6 = new StringBuilder();
                                        stringBuilder6.append("Unrecognized profile ");
                                        stringBuilder6.append(profileLevel3.profile);
                                        stringBuilder6.append(" for ");
                                        stringBuilder6.append(str4);
                                        Log.w(str7, stringBuilder6.toString());
                                        errors |= 1;
                                    }
                                    errors &= -5;
                                }
                                profileLevels6 = profileLevels7;
                                thisR.applyMacroBlockLimits(32767, 32767, Integer.MAX_VALUE, 2147483647L, 16, 16, 1, 1);
                                maxBlocksPerSecond3 = 2147483647L;
                                FS3 = Integer.MAX_VALUE;
                                i3 = profileLevels6;
                            } else {
                                profileLevels6 = profileLevels5;
                                String str8;
                                CodecProfileLevel[] profileLevels8;
                                if (str4.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_VP9)) {
                                    maxBps2 = 36864;
                                    BR2 = profileLevels6.length;
                                    i = 200000;
                                    errors = 4;
                                    i3 = 512;
                                    long maxBlocksPerSecond6 = 829440;
                                    W = 0;
                                    while (W < BR2) {
                                        long SR;
                                        int i9;
                                        profileLevel2 = profileLevels6[W];
                                        i2 = 0;
                                        maxWidth = 0;
                                        int FS4 = 0;
                                        switch (profileLevel2.level) {
                                            case 1:
                                                SR = 829440;
                                                maxBps = 36864;
                                                i2 = 200;
                                                maxWidth = 512;
                                                break;
                                            case 2:
                                                SR = 2764800;
                                                maxBps = 73728;
                                                i2 = 800;
                                                maxWidth = 768;
                                                break;
                                            case 4:
                                                SR = 4608000;
                                                maxBps = 122880;
                                                i2 = Device.WEARABLE_PAGER;
                                                maxWidth = 960;
                                                break;
                                            case 8:
                                                SR = 9216000;
                                                maxBps = 245760;
                                                i2 = 3600;
                                                maxWidth = Device.PERIPHERAL_KEYBOARD;
                                                break;
                                            case 16:
                                                SR = 20736000;
                                                maxBps = 552960;
                                                i2 = 7200;
                                                maxWidth = 2048;
                                                break;
                                            case 32:
                                                SR = 36864000;
                                                maxBps = 983040;
                                                i2 = 12000;
                                                maxWidth = 2752;
                                                break;
                                            case 64:
                                                SR = 83558400;
                                                maxBps = 2228224;
                                                i2 = 18000;
                                                maxWidth = 4160;
                                                break;
                                            case 128:
                                                SR = 160432128;
                                                maxBps = 2228224;
                                                i2 = 30000;
                                                maxWidth = 4160;
                                                break;
                                            case 256:
                                                SR = 311951360;
                                                maxBps = 8912896;
                                                i2 = 60000;
                                                maxWidth = 8384;
                                                break;
                                            case 512:
                                                SR = 588251136;
                                                maxBps = 8912896;
                                                i2 = 120000;
                                                maxWidth = 8384;
                                                break;
                                            case 1024:
                                                SR = 1176502272;
                                                maxBps = 8912896;
                                                i2 = 180000;
                                                maxWidth = 8384;
                                                break;
                                            case 2048:
                                                SR = 1176502272;
                                                maxBps = 35651584;
                                                i2 = 180000;
                                                maxWidth = 16832;
                                                break;
                                            case 4096:
                                                SR = 2353004544L;
                                                maxBps = 35651584;
                                                i2 = 240000;
                                                maxWidth = 16832;
                                                break;
                                            case 8192:
                                                SR = 4706009088L;
                                                maxBps = 35651584;
                                                i2 = 480000;
                                                maxWidth = 16832;
                                                break;
                                            default:
                                                str8 = TAG;
                                                i9 = BR2;
                                                stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("Unrecognized level ");
                                                stringBuilder4.append(profileLevel2.level);
                                                stringBuilder4.append(" for ");
                                                stringBuilder4.append(str4);
                                                Log.w(str8, stringBuilder4.toString());
                                                errors |= 1;
                                                maxBps = FS4;
                                                SR = 0;
                                                break;
                                        }
                                        i9 = BR2;
                                        BR2 = profileLevel2.profile;
                                        profileLevels8 = profileLevels6;
                                        if (!(BR2 == 4 || BR2 == 8 || BR2 == 4096 || BR2 == 8192)) {
                                            switch (BR2) {
                                                case 1:
                                                case 2:
                                                    break;
                                                default:
                                                    str6 = TAG;
                                                    profileLevels6 = new StringBuilder();
                                                    profileLevels6.append("Unrecognized profile ");
                                                    profileLevels6.append(profileLevel2.profile);
                                                    profileLevels6.append(" for ");
                                                    profileLevels6.append(str4);
                                                    Log.w(str6, profileLevels6.toString());
                                                    errors |= 1;
                                                    break;
                                            }
                                        }
                                        errors &= -5;
                                        maxBlocksPerSecond6 = Math.max(SR, maxBlocksPerSecond6);
                                        maxBps2 = Math.max(maxBps, maxBps2);
                                        i = Math.max(i2 * 1000, i);
                                        i3 = Math.max(maxWidth, i3);
                                        W++;
                                        BR2 = i9;
                                        profileLevels6 = profileLevels8;
                                    }
                                    profileLevels8 = profileLevels6;
                                    maxBps3 = Utils.divUp(i3, 8);
                                    maxWidth = Utils.divUp(maxBps2, 64);
                                    applyMacroBlockLimits(maxBps3, maxBps3, maxWidth, Utils.divUp(maxBlocksPerSecond6, 64), 8, 8, 1, 1);
                                    FS3 = maxWidth;
                                    codecProfileLevelArr = profileLevels8;
                                } else {
                                    profileLevels8 = profileLevels6;
                                    if (str4.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                                        long maxBlocksPerSecond7 = (long) (576 * 15);
                                        long maxBlocksPerSecond8 = maxBlocksPerSecond7;
                                        i = 576;
                                        minWidth2 = 128000;
                                        errors = 4;
                                        for (CodecProfileLevel profileLevel22 : profileLevels8) {
                                            double FR2 = 0.0d;
                                            maxBlocks = 0;
                                            BR2 = 0;
                                            switch (profileLevel22.level) {
                                                case 1:
                                                case 2:
                                                    FR2 = 15.0d;
                                                    maxBlocks = 36864;
                                                    BR2 = 128;
                                                    break;
                                                case 4:
                                                case 8:
                                                    FR2 = 30.0d;
                                                    maxBlocks = 122880;
                                                    BR2 = 1500;
                                                    break;
                                                case 16:
                                                case 32:
                                                    FR2 = 30.0d;
                                                    maxBlocks = 245760;
                                                    BR2 = FingerprintManager.HW_FINGERPRINT_ACQUIRED_VENDOR_BASE_END;
                                                    break;
                                                case 64:
                                                case 128:
                                                    FR2 = 30.0d;
                                                    maxBlocks = 552960;
                                                    BR2 = BluetoothHealth.HEALTH_OPERATION_SUCCESS;
                                                    break;
                                                case 256:
                                                case 512:
                                                    FR2 = 33.75d;
                                                    maxBlocks = 983040;
                                                    BR2 = 10000;
                                                    break;
                                                case 1024:
                                                    FR2 = 30.0d;
                                                    maxBlocks = 2228224;
                                                    BR2 = 12000;
                                                    break;
                                                case 2048:
                                                    FR2 = 30.0d;
                                                    maxBlocks = 2228224;
                                                    BR2 = 30000;
                                                    break;
                                                case 4096:
                                                    FR2 = 60.0d;
                                                    maxBlocks = 2228224;
                                                    BR2 = HwMediaMonitorUtils.TYPE_MEDIA_RECORD_DTS_COUNT;
                                                    break;
                                                case 8192:
                                                    FR2 = 60.0d;
                                                    maxBlocks = 2228224;
                                                    BR2 = SQLiteDatabase.SQLITE_MAX_LIKE_PATTERN_LENGTH;
                                                    break;
                                                case 16384:
                                                    FR2 = 30.0d;
                                                    maxBlocks = 8912896;
                                                    BR2 = 25000;
                                                    break;
                                                case 32768:
                                                    FR2 = 30.0d;
                                                    maxBlocks = 8912896;
                                                    BR2 = IApsManager.APS_CALLBACK_ENLARGE_FACTOR;
                                                    break;
                                                case 65536:
                                                    FR2 = 60.0d;
                                                    maxBlocks = 8912896;
                                                    BR2 = 40000;
                                                    break;
                                                case 131072:
                                                    FR2 = 60.0d;
                                                    maxBlocks = 8912896;
                                                    BR2 = 160000;
                                                    break;
                                                case 262144:
                                                    FR2 = 120.0d;
                                                    maxBlocks = 8912896;
                                                    BR2 = 60000;
                                                    break;
                                                case 524288:
                                                    FR2 = 120.0d;
                                                    maxBlocks = 8912896;
                                                    BR2 = 240000;
                                                    break;
                                                case 1048576:
                                                    FR2 = 30.0d;
                                                    maxBlocks = 35651584;
                                                    BR2 = 60000;
                                                    break;
                                                case 2097152:
                                                    FR2 = 30.0d;
                                                    maxBlocks = 35651584;
                                                    BR2 = 240000;
                                                    break;
                                                case 4194304:
                                                    FR2 = 60.0d;
                                                    maxBlocks = 35651584;
                                                    BR2 = 120000;
                                                    break;
                                                case 8388608:
                                                    FR2 = 60.0d;
                                                    maxBlocks = 35651584;
                                                    BR2 = 480000;
                                                    break;
                                                case 16777216:
                                                    FR2 = 120.0d;
                                                    maxBlocks = 35651584;
                                                    BR2 = 240000;
                                                    break;
                                                case 33554432:
                                                    FR2 = 120.0d;
                                                    maxBlocks = 35651584;
                                                    BR2 = 800000;
                                                    break;
                                                default:
                                                    str = TAG;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("Unrecognized level ");
                                                    stringBuilder2.append(profileLevel22.level);
                                                    stringBuilder2.append(" for ");
                                                    stringBuilder2.append(str4);
                                                    Log.w(str, stringBuilder2.toString());
                                                    errors |= 1;
                                                    break;
                                            }
                                            MBPS2 = profileLevel22.profile;
                                            if (MBPS2 != 4096) {
                                                switch (MBPS2) {
                                                    case 1:
                                                    case 2:
                                                        break;
                                                    default:
                                                        str = TAG;
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("Unrecognized profile ");
                                                        stringBuilder2.append(profileLevel22.profile);
                                                        stringBuilder2.append(" for ");
                                                        stringBuilder2.append(str4);
                                                        Log.w(str, stringBuilder2.toString());
                                                        errors |= 1;
                                                        break;
                                                }
                                            }
                                            maxBlocks >>= 6;
                                            errors &= -5;
                                            maxBlocksPerSecond8 = Math.max((long) ((int) (((double) maxBlocks) * FR2)), maxBlocksPerSecond8);
                                            i = Math.max(maxBlocks, i);
                                            minWidth2 = Math.max(BR2 * 1000, minWidth2);
                                        }
                                        i2 = (int) Math.sqrt((double) (i * 8));
                                        applyMacroBlockLimits(i2, i2, i, maxBlocksPerSecond8, 8, 8, 1, 1);
                                        FS3 = i;
                                        i = minWidth2;
                                        maxBlocksPerSecond3 = maxBlocksPerSecond8;
                                    } else {
                                        str8 = TAG;
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Unsupported mime ");
                                        stringBuilder4.append(str4);
                                        Log.w(str8, stringBuilder4.toString());
                                        errors = 4 | 2;
                                        maxBlocksPerSecond3 = 0;
                                        FS3 = 0;
                                        i = 64000;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            this.mBitrateRange = Range.create(Integer.valueOf(1), Integer.valueOf(i));
            CodecCapabilities codecCapabilities = this.mParent;
            codecCapabilities.mError |= errors;
        }
    }

    MediaCodecInfo(String name, boolean isEncoder, CodecCapabilities[] caps) {
        this.mName = name;
        this.mIsEncoder = isEncoder;
        for (CodecCapabilities c : caps) {
            this.mCaps.put(c.getMimeType(), c);
        }
    }

    public final String getName() {
        return this.mName;
    }

    public final boolean isEncoder() {
        return this.mIsEncoder;
    }

    public final String[] getSupportedTypes() {
        Set<String> typeSet = this.mCaps.keySet();
        String[] types = (String[]) typeSet.toArray(new String[typeSet.size()]);
        Arrays.sort(types);
        return types;
    }

    private static int checkPowerOfTwo(int value, String message) {
        if (((value - 1) & value) == 0) {
            return value;
        }
        throw new IllegalArgumentException(message);
    }

    public final CodecCapabilities getCapabilitiesForType(String type) {
        CodecCapabilities caps = (CodecCapabilities) this.mCaps.get(type);
        if (caps != null) {
            return caps.dup();
        }
        throw new IllegalArgumentException("codec does not support type");
    }

    public MediaCodecInfo makeRegular() {
        ArrayList<CodecCapabilities> caps = new ArrayList();
        for (CodecCapabilities c : this.mCaps.values()) {
            if (c.isRegular()) {
                caps.add(c);
            }
        }
        if (caps.size() == 0) {
            return null;
        }
        if (caps.size() == this.mCaps.size()) {
            return this;
        }
        return new MediaCodecInfo(this.mName, this.mIsEncoder, (CodecCapabilities[]) caps.toArray(new CodecCapabilities[caps.size()]));
    }
}
