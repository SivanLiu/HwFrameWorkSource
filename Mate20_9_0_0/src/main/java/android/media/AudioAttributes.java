package android.media;

import android.annotation.SystemApi;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class AudioAttributes implements Parcelable {
    private static final int ALL_PARCEL_FLAGS = 1;
    private static final int ATTR_PARCEL_IS_NULL_BUNDLE = -1977;
    private static final int ATTR_PARCEL_IS_VALID_BUNDLE = 1980;
    public static final int CONTENT_TYPE_MOVIE = 3;
    public static final int CONTENT_TYPE_MUSIC = 2;
    public static final int CONTENT_TYPE_SONIFICATION = 4;
    public static final int CONTENT_TYPE_SPEECH = 1;
    public static final int CONTENT_TYPE_UNKNOWN = 0;
    public static final Creator<AudioAttributes> CREATOR = new Creator<AudioAttributes>() {
        public AudioAttributes createFromParcel(Parcel p) {
            return new AudioAttributes(p, null);
        }

        public AudioAttributes[] newArray(int size) {
            return new AudioAttributes[size];
        }
    };
    private static final int FLAG_ALL = 1023;
    private static final int FLAG_ALL_PUBLIC = 273;
    public static final int FLAG_AUDIBILITY_ENFORCED = 1;
    @SystemApi
    public static final int FLAG_BEACON = 8;
    @SystemApi
    public static final int FLAG_BYPASS_INTERRUPTION_POLICY = 64;
    @SystemApi
    public static final int FLAG_BYPASS_MUTE = 128;
    protected static final int FLAG_CONCURRENT_CAPTURE = 1610612736;
    public static final int FLAG_DEEP_BUFFER = 512;
    public static final int FLAG_HW_AV_SYNC = 16;
    @SystemApi
    public static final int FLAG_HW_HOTWORD = 32;
    public static final int FLAG_LOW_LATENCY = 256;
    public static final int FLAG_SCO = 4;
    public static final int FLAG_SECURE = 2;
    public static final int FLATTEN_TAGS = 1;
    public static final int[] SDK_USAGES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16};
    public static final int SUPPRESSIBLE_ALARM = 4;
    public static final int SUPPRESSIBLE_CALL = 2;
    public static final int SUPPRESSIBLE_MEDIA = 5;
    public static final int SUPPRESSIBLE_NEVER = 3;
    public static final int SUPPRESSIBLE_NOTIFICATION = 1;
    public static final int SUPPRESSIBLE_SYSTEM = 6;
    public static final SparseIntArray SUPPRESSIBLE_USAGES = new SparseIntArray();
    private static final String TAG = "AudioAttributes";
    public static final int USAGE_ALARM = 4;
    public static final int USAGE_ASSISTANCE_ACCESSIBILITY = 11;
    public static final int USAGE_ASSISTANCE_NAVIGATION_GUIDANCE = 12;
    public static final int USAGE_ASSISTANCE_SONIFICATION = 13;
    public static final int USAGE_ASSISTANT = 16;
    public static final int USAGE_GAME = 14;
    public static final int USAGE_MEDIA = 1;
    public static final int USAGE_NOTIFICATION = 5;
    public static final int USAGE_NOTIFICATION_COMMUNICATION_DELAYED = 9;
    public static final int USAGE_NOTIFICATION_COMMUNICATION_INSTANT = 8;
    public static final int USAGE_NOTIFICATION_COMMUNICATION_REQUEST = 7;
    public static final int USAGE_NOTIFICATION_EVENT = 10;
    public static final int USAGE_NOTIFICATION_RINGTONE = 6;
    public static final int USAGE_UNKNOWN = 0;
    public static final int USAGE_VIRTUAL_SOURCE = 15;
    public static final int USAGE_VOICE_COMMUNICATION = 2;
    public static final int USAGE_VOICE_COMMUNICATION_SIGNALLING = 3;
    private Bundle mBundle;
    private int mContentType;
    protected int mFlags;
    private String mFormattedTags;
    private int mSource;
    private HashSet<String> mTags;
    private int mUsage;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AttributeContentType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface AttributeUsage {
    }

    public static class Builder {
        private Bundle mBundle;
        private int mContentType = 0;
        private int mFlags = 0;
        private int mSource = -1;
        private HashSet<String> mTags = new HashSet();
        private int mUsage = 0;

        public Builder(AudioAttributes aa) {
            this.mUsage = aa.mUsage;
            this.mContentType = aa.mContentType;
            this.mFlags = aa.mFlags;
            this.mTags = (HashSet) aa.mTags.clone();
        }

        public AudioAttributes build() {
            AudioAttributes aa = new AudioAttributes();
            aa.mContentType = this.mContentType;
            aa.mUsage = this.mUsage;
            aa.mSource = this.mSource;
            aa.mFlags = this.mFlags;
            aa.mTags = (HashSet) this.mTags.clone();
            aa.mFormattedTags = TextUtils.join(";", this.mTags);
            if (this.mBundle != null) {
                aa.mBundle = new Bundle(this.mBundle);
            }
            return aa;
        }

        public Builder setUsage(int usage) {
            switch (usage) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    this.mUsage = usage;
                    break;
                default:
                    this.mUsage = 0;
                    break;
            }
            return this;
        }

        public Builder setContentType(int contentType) {
            switch (contentType) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    this.mContentType = contentType;
                    break;
                default:
                    this.mUsage = 0;
                    break;
            }
            return this;
        }

        public Builder setFlags(int flags) {
            this.mFlags |= flags & 1023;
            return this;
        }

        public Builder replaceFlags(int flags) {
            this.mFlags = flags & 1023;
            return this;
        }

        @SystemApi
        public Builder addBundle(Bundle bundle) {
            if (bundle != null) {
                if (this.mBundle == null) {
                    this.mBundle = new Bundle(bundle);
                } else {
                    this.mBundle.putAll(bundle);
                }
                return this;
            }
            throw new IllegalArgumentException("Illegal null bundle");
        }

        public Builder addTag(String tag) {
            this.mTags.add(tag);
            return this;
        }

        public Builder setLegacyStreamType(int streamType) {
            if (streamType != 10) {
                return setInternalLegacyStreamType(streamType);
            }
            throw new IllegalArgumentException("STREAM_ACCESSIBILITY is not a legacy stream type that was used for audio playback");
        }

        public Builder setInternalLegacyStreamType(int streamType) {
            switch (streamType) {
                case 0:
                    this.mContentType = 1;
                    break;
                case 1:
                    break;
                case 2:
                    this.mContentType = 4;
                    break;
                case 3:
                    this.mContentType = 2;
                    break;
                case 4:
                    this.mContentType = 4;
                    break;
                case 5:
                    this.mContentType = 4;
                    break;
                case 6:
                    this.mContentType = 1;
                    this.mFlags |= 4;
                    break;
                case 7:
                    this.mFlags = 1 | this.mFlags;
                    break;
                case 8:
                    this.mContentType = 4;
                    break;
                case 9:
                    this.mContentType = 4;
                    break;
                case 10:
                    this.mContentType = 1;
                    break;
                default:
                    String str = AudioAttributes.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid stream type ");
                    stringBuilder.append(streamType);
                    stringBuilder.append(" for AudioAttributes");
                    Log.e(str, stringBuilder.toString());
                    break;
            }
            this.mContentType = 4;
            this.mUsage = AudioAttributes.usageForStreamType(streamType);
            return this;
        }

        @SystemApi
        public Builder setCapturePreset(int preset) {
            switch (preset) {
                case 0:
                case 1:
                case 5:
                case 6:
                case 7:
                case 9:
                    this.mSource = preset;
                    break;
                default:
                    String str = AudioAttributes.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid capture preset ");
                    stringBuilder.append(preset);
                    stringBuilder.append(" for AudioAttributes");
                    Log.e(str, stringBuilder.toString());
                    break;
            }
            return this;
        }

        @SystemApi
        public Builder setInternalCapturePreset(int preset) {
            if (preset == 1999 || preset == 8 || preset == AudioSource.RADIO_TUNER || preset == 3 || preset == 2 || preset == 4) {
                this.mSource = preset;
            } else {
                setCapturePreset(preset);
            }
            return this;
        }
    }

    /* synthetic */ AudioAttributes(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    static {
        SUPPRESSIBLE_USAGES.put(5, 1);
        SUPPRESSIBLE_USAGES.put(6, 2);
        SUPPRESSIBLE_USAGES.put(7, 2);
        SUPPRESSIBLE_USAGES.put(8, 1);
        SUPPRESSIBLE_USAGES.put(9, 1);
        SUPPRESSIBLE_USAGES.put(10, 1);
        SUPPRESSIBLE_USAGES.put(11, 3);
        SUPPRESSIBLE_USAGES.put(2, 3);
        SUPPRESSIBLE_USAGES.put(4, 4);
        SUPPRESSIBLE_USAGES.put(1, 5);
        SUPPRESSIBLE_USAGES.put(12, 5);
        SUPPRESSIBLE_USAGES.put(14, 5);
        SUPPRESSIBLE_USAGES.put(16, 5);
        SUPPRESSIBLE_USAGES.put(0, 5);
        SUPPRESSIBLE_USAGES.put(3, 6);
        SUPPRESSIBLE_USAGES.put(13, 6);
    }

    private AudioAttributes() {
        this.mUsage = 0;
        this.mContentType = 0;
        this.mSource = -1;
        this.mFlags = 0;
    }

    public int getContentType() {
        return this.mContentType;
    }

    public int getUsage() {
        return this.mUsage;
    }

    @SystemApi
    public int getCapturePreset() {
        return this.mSource;
    }

    public int getFlags() {
        return this.mFlags & 273;
    }

    @SystemApi
    public int getAllFlags() {
        return this.mFlags & 1023;
    }

    @SystemApi
    public Bundle getBundle() {
        if (this.mBundle == null) {
            return this.mBundle;
        }
        return new Bundle(this.mBundle);
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(this.mTags);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mUsage);
        dest.writeInt(this.mContentType);
        dest.writeInt(this.mSource);
        dest.writeInt(this.mFlags);
        dest.writeInt(flags & 1);
        if ((flags & 1) == 0) {
            String[] tagsArray = new String[this.mTags.size()];
            this.mTags.toArray(tagsArray);
            dest.writeStringArray(tagsArray);
        } else if ((flags & 1) == 1) {
            dest.writeString(this.mFormattedTags);
        }
        if (this.mBundle == null) {
            dest.writeInt(ATTR_PARCEL_IS_NULL_BUNDLE);
            return;
        }
        dest.writeInt(ATTR_PARCEL_IS_VALID_BUNDLE);
        dest.writeBundle(this.mBundle);
    }

    private AudioAttributes(Parcel in) {
        boolean hasFlattenedTags = false;
        this.mUsage = 0;
        this.mContentType = 0;
        this.mSource = -1;
        this.mFlags = 0;
        this.mUsage = in.readInt();
        this.mContentType = in.readInt();
        this.mSource = in.readInt();
        this.mFlags = in.readInt();
        if ((in.readInt() & 1) == 1) {
            hasFlattenedTags = true;
        }
        this.mTags = new HashSet();
        if (hasFlattenedTags) {
            this.mFormattedTags = new String(in.readString());
            this.mTags.add(this.mFormattedTags);
        } else {
            String[] tagsArray = in.readStringArray();
            int i = tagsArray.length - 1;
            while (true) {
                int i2 = i;
                if (i2 < 0) {
                    break;
                }
                this.mTags.add(tagsArray[i2]);
                i = i2 - 1;
            }
            this.mFormattedTags = TextUtils.join(";", this.mTags);
        }
        int readInt = in.readInt();
        if (readInt == ATTR_PARCEL_IS_NULL_BUNDLE) {
            this.mBundle = null;
        } else if (readInt != ATTR_PARCEL_IS_VALID_BUNDLE) {
            Log.e(TAG, "Illegal value unmarshalling AudioAttributes, can't initialize bundle");
        } else {
            this.mBundle = new Bundle(in.readBundle());
        }
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioAttributes that = (AudioAttributes) o;
        if (!(this.mContentType == that.mContentType && this.mFlags == that.mFlags && this.mSource == that.mSource && this.mUsage == that.mUsage && this.mFormattedTags.equals(that.mFormattedTags))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(this.mContentType), Integer.valueOf(this.mFlags), Integer.valueOf(this.mSource), Integer.valueOf(this.mUsage), this.mFormattedTags, this.mBundle});
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AudioAttributes: usage=");
        stringBuilder.append(usageToString());
        stringBuilder.append(" content=");
        stringBuilder.append(contentTypeToString());
        stringBuilder.append(" flags=0x");
        stringBuilder.append(Integer.toHexString(this.mFlags).toUpperCase());
        stringBuilder.append(" tags=");
        stringBuilder.append(this.mFormattedTags);
        stringBuilder.append(" bundle=");
        stringBuilder.append(this.mBundle == null ? "null" : this.mBundle.toString());
        return new String(stringBuilder.toString());
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1159641169921L, this.mUsage);
        proto.write(1159641169922L, this.mContentType);
        proto.write(1120986464259L, this.mFlags);
        for (String t : this.mFormattedTags.split(";")) {
            String t2 = t2.trim();
            if (t2 != "") {
                proto.write(AudioAttributesProto.TAGS, t2);
            }
        }
        proto.end(token);
    }

    public String usageToString() {
        return usageToString(this.mUsage);
    }

    public static String usageToString(int usage) {
        switch (usage) {
            case 0:
                return new String("USAGE_UNKNOWN");
            case 1:
                return new String("USAGE_MEDIA");
            case 2:
                return new String("USAGE_VOICE_COMMUNICATION");
            case 3:
                return new String("USAGE_VOICE_COMMUNICATION_SIGNALLING");
            case 4:
                return new String("USAGE_ALARM");
            case 5:
                return new String("USAGE_NOTIFICATION");
            case 6:
                return new String("USAGE_NOTIFICATION_RINGTONE");
            case 7:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_REQUEST");
            case 8:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_INSTANT");
            case 9:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_DELAYED");
            case 10:
                return new String("USAGE_NOTIFICATION_EVENT");
            case 11:
                return new String("USAGE_ASSISTANCE_ACCESSIBILITY");
            case 12:
                return new String("USAGE_ASSISTANCE_NAVIGATION_GUIDANCE");
            case 13:
                return new String("USAGE_ASSISTANCE_SONIFICATION");
            case 14:
                return new String("USAGE_GAME");
            case 16:
                return new String("USAGE_ASSISTANT");
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown usage ");
                stringBuilder.append(usage);
                return new String(stringBuilder.toString());
        }
    }

    public String contentTypeToString() {
        switch (this.mContentType) {
            case 0:
                return new String("CONTENT_TYPE_UNKNOWN");
            case 1:
                return new String("CONTENT_TYPE_SPEECH");
            case 2:
                return new String("CONTENT_TYPE_MUSIC");
            case 3:
                return new String("CONTENT_TYPE_MOVIE");
            case 4:
                return new String("CONTENT_TYPE_SONIFICATION");
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown content type ");
                stringBuilder.append(this.mContentType);
                return new String(stringBuilder.toString());
        }
    }

    private static int usageForStreamType(int streamType) {
        switch (streamType) {
            case 0:
                return 2;
            case 1:
            case 7:
                return 13;
            case 2:
                return 6;
            case 3:
                return 1;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 2;
            case 8:
                return 3;
            case 10:
                return 11;
            default:
                return 0;
        }
    }

    public int getVolumeControlStream() {
        return toVolumeStreamType(true, this);
    }

    public static int toLegacyStreamType(AudioAttributes aa) {
        return toVolumeStreamType(false, aa);
    }

    private static int toVolumeStreamType(boolean fromGetVolumeControlStream, AudioAttributes aa) {
        int i = 1;
        if ((aa.getFlags() & 1) == 1) {
            if (!fromGetVolumeControlStream) {
                i = 7;
            }
            return i;
        }
        int i2 = 0;
        if ((aa.getFlags() & 4) == 4) {
            if (!fromGetVolumeControlStream) {
                i2 = 6;
            }
            return i2;
        }
        switch (aa.getUsage()) {
            case 0:
                return 3;
            case 1:
            case 12:
            case 14:
            case 16:
                return 3;
            case 2:
                return 0;
            case 3:
                if (!fromGetVolumeControlStream) {
                    i2 = 8;
                }
                return i2;
            case 4:
                return 4;
            case 5:
            case 7:
            case 8:
            case 9:
            case 10:
                return 5;
            case 6:
                return 2;
            case 11:
                return 10;
            case 13:
                return 1;
            default:
                if (!fromGetVolumeControlStream) {
                    return 3;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown usage value ");
                stringBuilder.append(aa.getUsage());
                stringBuilder.append(" in audio attributes");
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }
}
