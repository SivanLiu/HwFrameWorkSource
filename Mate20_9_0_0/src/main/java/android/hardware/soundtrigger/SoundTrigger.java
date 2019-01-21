package android.hardware.soundtrigger;

import android.annotation.SystemApi;
import android.media.AudioFormat;
import android.media.AudioFormat.Builder;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.system.OsConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

@SystemApi
public class SoundTrigger {
    public static final int RECOGNITION_MODE_USER_AUTHENTICATION = 4;
    public static final int RECOGNITION_MODE_USER_IDENTIFICATION = 2;
    public static final int RECOGNITION_MODE_VOICE_TRIGGER = 1;
    public static final int RECOGNITION_STATUS_ABORT = 1;
    public static final int RECOGNITION_STATUS_FAILURE = 2;
    public static final int RECOGNITION_STATUS_SUCCESS = 0;
    public static final int SERVICE_STATE_DISABLED = 1;
    public static final int SERVICE_STATE_ENABLED = 0;
    public static final int SOUNDMODEL_STATUS_UPDATED = 0;
    public static final int STATUS_BAD_VALUE = (-OsConstants.EINVAL);
    public static final int STATUS_DEAD_OBJECT = (-OsConstants.EPIPE);
    public static final int STATUS_ERROR = Integer.MIN_VALUE;
    public static final int STATUS_INVALID_OPERATION = (-OsConstants.ENOSYS);
    public static final int STATUS_NO_INIT = (-OsConstants.ENODEV);
    public static final int STATUS_OK = 0;
    public static final int STATUS_PERMISSION_DENIED = (-OsConstants.EPERM);

    public static class RecognitionEvent {
        public static final Creator<RecognitionEvent> CREATOR = new Creator<RecognitionEvent>() {
            public RecognitionEvent createFromParcel(Parcel in) {
                return RecognitionEvent.fromParcel(in);
            }

            public RecognitionEvent[] newArray(int size) {
                return new RecognitionEvent[size];
            }
        };
        public final boolean captureAvailable;
        public final int captureDelayMs;
        public final AudioFormat captureFormat;
        public final int capturePreambleMs;
        public final int captureSession;
        public final byte[] data;
        public final int soundModelHandle;
        public final int status;
        public final boolean triggerInData;

        public RecognitionEvent(int status, int soundModelHandle, boolean captureAvailable, int captureSession, int captureDelayMs, int capturePreambleMs, boolean triggerInData, AudioFormat captureFormat, byte[] data) {
            this.status = status;
            this.soundModelHandle = soundModelHandle;
            this.captureAvailable = captureAvailable;
            this.captureSession = captureSession;
            this.captureDelayMs = captureDelayMs;
            this.capturePreambleMs = capturePreambleMs;
            this.triggerInData = triggerInData;
            this.captureFormat = captureFormat;
            this.data = data;
        }

        public boolean isCaptureAvailable() {
            return this.captureAvailable;
        }

        public AudioFormat getCaptureFormat() {
            return this.captureFormat;
        }

        public int getCaptureSession() {
            return this.captureSession;
        }

        public byte[] getData() {
            return this.data;
        }

        protected static RecognitionEvent fromParcel(Parcel in) {
            int status = in.readInt();
            int soundModelHandle = in.readInt();
            boolean captureAvailable = in.readByte() == (byte) 1;
            int captureSession = in.readInt();
            int captureDelayMs = in.readInt();
            int capturePreambleMs = in.readInt();
            boolean triggerInData = in.readByte() == (byte) 1;
            AudioFormat captureFormat = null;
            if (in.readByte() == (byte) 1) {
                captureFormat = new Builder().setChannelMask(in.readInt()).setEncoding(in.readInt()).setSampleRate(in.readInt()).build();
            }
            return new RecognitionEvent(status, soundModelHandle, captureAvailable, captureSession, captureDelayMs, capturePreambleMs, triggerInData, captureFormat, in.readBlob());
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.status);
            dest.writeInt(this.soundModelHandle);
            dest.writeByte((byte) this.captureAvailable);
            dest.writeInt(this.captureSession);
            dest.writeInt(this.captureDelayMs);
            dest.writeInt(this.capturePreambleMs);
            dest.writeByte((byte) this.triggerInData);
            if (this.captureFormat != null) {
                dest.writeByte((byte) 1);
                dest.writeInt(this.captureFormat.getSampleRate());
                dest.writeInt(this.captureFormat.getEncoding());
                dest.writeInt(this.captureFormat.getChannelMask());
            } else {
                dest.writeByte((byte) 0);
            }
            dest.writeBlob(this.data);
        }

        public int hashCode() {
            int i = 1237;
            int result = 31 * ((31 * ((31 * ((31 * ((31 * 1) + (this.captureAvailable ? 1231 : 1237))) + this.captureDelayMs)) + this.capturePreambleMs)) + this.captureSession);
            if (this.triggerInData) {
                i = 1231;
            }
            result += i;
            if (this.captureFormat != null) {
                result = (31 * ((31 * ((31 * result) + this.captureFormat.getSampleRate())) + this.captureFormat.getEncoding())) + this.captureFormat.getChannelMask();
            }
            return (31 * ((31 * ((31 * result) + Arrays.hashCode(this.data))) + this.soundModelHandle)) + this.status;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            RecognitionEvent other = (RecognitionEvent) obj;
            if (this.captureAvailable != other.captureAvailable || this.captureDelayMs != other.captureDelayMs || this.capturePreambleMs != other.capturePreambleMs || this.captureSession != other.captureSession || !Arrays.equals(this.data, other.data) || this.soundModelHandle != other.soundModelHandle || this.status != other.status || this.triggerInData != other.triggerInData) {
                return false;
            }
            if (this.captureFormat == null) {
                if (other.captureFormat != null) {
                    return false;
                }
            } else if (other.captureFormat != null && this.captureFormat.getSampleRate() == other.captureFormat.getSampleRate() && this.captureFormat.getEncoding() == other.captureFormat.getEncoding() && this.captureFormat.getChannelMask() == other.captureFormat.getChannelMask()) {
                return true;
            } else {
                return false;
            }
            return true;
        }

        public String toString() {
            String str;
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("RecognitionEvent [status=");
            stringBuilder2.append(this.status);
            stringBuilder2.append(", soundModelHandle=");
            stringBuilder2.append(this.soundModelHandle);
            stringBuilder2.append(", captureAvailable=");
            stringBuilder2.append(this.captureAvailable);
            stringBuilder2.append(", captureSession=");
            stringBuilder2.append(this.captureSession);
            stringBuilder2.append(", captureDelayMs=");
            stringBuilder2.append(this.captureDelayMs);
            stringBuilder2.append(", capturePreambleMs=");
            stringBuilder2.append(this.capturePreambleMs);
            stringBuilder2.append(", triggerInData=");
            stringBuilder2.append(this.triggerInData);
            if (this.captureFormat == null) {
                str = "";
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(", sampleRate=");
                stringBuilder.append(this.captureFormat.getSampleRate());
                str = stringBuilder.toString();
            }
            stringBuilder2.append(str);
            if (this.captureFormat == null) {
                str = "";
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(", encoding=");
                stringBuilder.append(this.captureFormat.getEncoding());
                str = stringBuilder.toString();
            }
            stringBuilder2.append(str);
            if (this.captureFormat == null) {
                str = "";
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(", channelMask=");
                stringBuilder.append(this.captureFormat.getChannelMask());
                str = stringBuilder.toString();
            }
            stringBuilder2.append(str);
            stringBuilder2.append(", data=");
            stringBuilder2.append(this.data == null ? 0 : this.data.length);
            stringBuilder2.append("]");
            return stringBuilder2.toString();
        }
    }

    public static class SoundModel {
        public static final int TYPE_GENERIC_SOUND = 1;
        public static final int TYPE_KEYPHRASE = 0;
        public static final int TYPE_UNKNOWN = -1;
        public final byte[] data;
        public final int type;
        public final UUID uuid;
        public final UUID vendorUuid;

        public SoundModel(UUID uuid, UUID vendorUuid, int type, byte[] data) {
            this.uuid = uuid;
            this.vendorUuid = vendorUuid;
            this.type = type;
            this.data = data;
        }

        public int hashCode() {
            int i = 0;
            int result = 31 * ((31 * ((31 * ((31 * 1) + Arrays.hashCode(this.data))) + this.type)) + (this.uuid == null ? 0 : this.uuid.hashCode()));
            if (this.vendorUuid != null) {
                i = this.vendorUuid.hashCode();
            }
            return result + i;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof SoundModel)) {
                return false;
            }
            SoundModel other = (SoundModel) obj;
            if (!Arrays.equals(this.data, other.data) || this.type != other.type) {
                return false;
            }
            if (this.uuid == null) {
                if (other.uuid != null) {
                    return false;
                }
            } else if (!this.uuid.equals(other.uuid)) {
                return false;
            }
            if (this.vendorUuid == null) {
                if (other.vendorUuid != null) {
                    return false;
                }
            } else if (!this.vendorUuid.equals(other.vendorUuid)) {
                return false;
            }
            return true;
        }
    }

    public interface StatusListener {
        void onRecognition(RecognitionEvent recognitionEvent);

        void onServiceDied();

        void onServiceStateChange(int i);

        void onSoundModelUpdate(SoundModelEvent soundModelEvent);
    }

    public static class ConfidenceLevel implements Parcelable {
        public static final Creator<ConfidenceLevel> CREATOR = new Creator<ConfidenceLevel>() {
            public ConfidenceLevel createFromParcel(Parcel in) {
                return ConfidenceLevel.fromParcel(in);
            }

            public ConfidenceLevel[] newArray(int size) {
                return new ConfidenceLevel[size];
            }
        };
        public final int confidenceLevel;
        public final int userId;

        public ConfidenceLevel(int userId, int confidenceLevel) {
            this.userId = userId;
            this.confidenceLevel = confidenceLevel;
        }

        private static ConfidenceLevel fromParcel(Parcel in) {
            return new ConfidenceLevel(in.readInt(), in.readInt());
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.userId);
            dest.writeInt(this.confidenceLevel);
        }

        public int describeContents() {
            return 0;
        }

        public int hashCode() {
            return (31 * ((31 * 1) + this.confidenceLevel)) + this.userId;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ConfidenceLevel other = (ConfidenceLevel) obj;
            if (this.confidenceLevel == other.confidenceLevel && this.userId == other.userId) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ConfidenceLevel [userId=");
            stringBuilder.append(this.userId);
            stringBuilder.append(", confidenceLevel=");
            stringBuilder.append(this.confidenceLevel);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public static class GenericRecognitionEvent extends RecognitionEvent implements Parcelable {
        public static final Creator<GenericRecognitionEvent> CREATOR = new Creator<GenericRecognitionEvent>() {
            public GenericRecognitionEvent createFromParcel(Parcel in) {
                return GenericRecognitionEvent.fromParcelForGeneric(in);
            }

            public GenericRecognitionEvent[] newArray(int size) {
                return new GenericRecognitionEvent[size];
            }
        };

        public GenericRecognitionEvent(int status, int soundModelHandle, boolean captureAvailable, int captureSession, int captureDelayMs, int capturePreambleMs, boolean triggerInData, AudioFormat captureFormat, byte[] data) {
            super(status, soundModelHandle, captureAvailable, captureSession, captureDelayMs, capturePreambleMs, triggerInData, captureFormat, data);
        }

        private static GenericRecognitionEvent fromParcelForGeneric(Parcel in) {
            RecognitionEvent event = RecognitionEvent.fromParcel(in);
            return new GenericRecognitionEvent(event.status, event.soundModelHandle, event.captureAvailable, event.captureSession, event.captureDelayMs, event.capturePreambleMs, event.triggerInData, event.captureFormat, event.data);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                return super.equals(obj);
            }
            return false;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GenericRecognitionEvent ::");
            stringBuilder.append(super.toString());
            return stringBuilder.toString();
        }
    }

    public static class GenericSoundModel extends SoundModel implements Parcelable {
        public static final Creator<GenericSoundModel> CREATOR = new Creator<GenericSoundModel>() {
            public GenericSoundModel createFromParcel(Parcel in) {
                return GenericSoundModel.fromParcel(in);
            }

            public GenericSoundModel[] newArray(int size) {
                return new GenericSoundModel[size];
            }
        };

        public GenericSoundModel(UUID uuid, UUID vendorUuid, byte[] data) {
            super(uuid, vendorUuid, 1, data);
        }

        public int describeContents() {
            return 0;
        }

        private static GenericSoundModel fromParcel(Parcel in) {
            UUID uuid = UUID.fromString(in.readString());
            UUID vendorUuid = null;
            if (in.readInt() >= 0) {
                vendorUuid = UUID.fromString(in.readString());
            }
            return new GenericSoundModel(uuid, vendorUuid, in.readBlob());
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.uuid.toString());
            if (this.vendorUuid == null) {
                dest.writeInt(-1);
            } else {
                dest.writeInt(this.vendorUuid.toString().length());
                dest.writeString(this.vendorUuid.toString());
            }
            dest.writeBlob(this.data);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GenericSoundModel [uuid=");
            stringBuilder.append(this.uuid);
            stringBuilder.append(", vendorUuid=");
            stringBuilder.append(this.vendorUuid);
            stringBuilder.append(", type=");
            stringBuilder.append(this.type);
            stringBuilder.append(", data=");
            stringBuilder.append(this.data == null ? 0 : this.data.length);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public static class Keyphrase implements Parcelable {
        public static final Creator<Keyphrase> CREATOR = new Creator<Keyphrase>() {
            public Keyphrase createFromParcel(Parcel in) {
                return Keyphrase.fromParcel(in);
            }

            public Keyphrase[] newArray(int size) {
                return new Keyphrase[size];
            }
        };
        public final int id;
        public final String locale;
        public final int recognitionModes;
        public final String text;
        public final int[] users;

        public Keyphrase(int id, int recognitionModes, String locale, String text, int[] users) {
            this.id = id;
            this.recognitionModes = recognitionModes;
            this.locale = locale;
            this.text = text;
            this.users = users;
        }

        private static Keyphrase fromParcel(Parcel in) {
            int id = in.readInt();
            int recognitionModes = in.readInt();
            String locale = in.readString();
            String text = in.readString();
            int[] users = null;
            int numUsers = in.readInt();
            if (numUsers >= 0) {
                users = new int[numUsers];
                in.readIntArray(users);
            }
            return new Keyphrase(id, recognitionModes, locale, text, users);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.id);
            dest.writeInt(this.recognitionModes);
            dest.writeString(this.locale);
            dest.writeString(this.text);
            if (this.users != null) {
                dest.writeInt(this.users.length);
                dest.writeIntArray(this.users);
                return;
            }
            dest.writeInt(-1);
        }

        public int describeContents() {
            return 0;
        }

        public int hashCode() {
            int i = 0;
            int hashCode = 31 * ((31 * ((31 * 1) + (this.text == null ? 0 : this.text.hashCode()))) + this.id);
            if (this.locale != null) {
                i = this.locale.hashCode();
            }
            return (31 * ((31 * (hashCode + i)) + this.recognitionModes)) + Arrays.hashCode(this.users);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Keyphrase other = (Keyphrase) obj;
            if (this.text == null) {
                if (other.text != null) {
                    return false;
                }
            } else if (!this.text.equals(other.text)) {
                return false;
            }
            if (this.id != other.id) {
                return false;
            }
            if (this.locale == null) {
                if (other.locale != null) {
                    return false;
                }
            } else if (!this.locale.equals(other.locale)) {
                return false;
            }
            if (this.recognitionModes == other.recognitionModes && Arrays.equals(this.users, other.users)) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Keyphrase [id=");
            stringBuilder.append(this.id);
            stringBuilder.append(", recognitionModes=");
            stringBuilder.append(this.recognitionModes);
            stringBuilder.append(", locale=");
            stringBuilder.append(this.locale);
            stringBuilder.append(", text=");
            stringBuilder.append(this.text);
            stringBuilder.append(", users=");
            stringBuilder.append(Arrays.toString(this.users));
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public static class KeyphraseRecognitionEvent extends RecognitionEvent implements Parcelable {
        public static final Creator<KeyphraseRecognitionEvent> CREATOR = new Creator<KeyphraseRecognitionEvent>() {
            public KeyphraseRecognitionEvent createFromParcel(Parcel in) {
                return KeyphraseRecognitionEvent.fromParcelForKeyphrase(in);
            }

            public KeyphraseRecognitionEvent[] newArray(int size) {
                return new KeyphraseRecognitionEvent[size];
            }
        };
        public final KeyphraseRecognitionExtra[] keyphraseExtras;

        public KeyphraseRecognitionEvent(int status, int soundModelHandle, boolean captureAvailable, int captureSession, int captureDelayMs, int capturePreambleMs, boolean triggerInData, AudioFormat captureFormat, byte[] data, KeyphraseRecognitionExtra[] keyphraseExtras) {
            super(status, soundModelHandle, captureAvailable, captureSession, captureDelayMs, capturePreambleMs, triggerInData, captureFormat, data);
            this.keyphraseExtras = keyphraseExtras;
        }

        private static KeyphraseRecognitionEvent fromParcelForKeyphrase(Parcel in) {
            int status = in.readInt();
            int soundModelHandle = in.readInt();
            boolean captureAvailable = in.readByte() == (byte) 1;
            int captureSession = in.readInt();
            int captureDelayMs = in.readInt();
            int capturePreambleMs = in.readInt();
            boolean triggerInData = in.readByte() == (byte) 1;
            AudioFormat captureFormat = null;
            if (in.readByte() == (byte) 1) {
                captureFormat = new Builder().setChannelMask(in.readInt()).setEncoding(in.readInt()).setSampleRate(in.readInt()).build();
            }
            return new KeyphraseRecognitionEvent(status, soundModelHandle, captureAvailable, captureSession, captureDelayMs, capturePreambleMs, triggerInData, captureFormat, in.readBlob(), (KeyphraseRecognitionExtra[]) in.createTypedArray(KeyphraseRecognitionExtra.CREATOR));
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.status);
            dest.writeInt(this.soundModelHandle);
            dest.writeByte((byte) this.captureAvailable);
            dest.writeInt(this.captureSession);
            dest.writeInt(this.captureDelayMs);
            dest.writeInt(this.capturePreambleMs);
            dest.writeByte((byte) this.triggerInData);
            if (this.captureFormat != null) {
                dest.writeByte((byte) 1);
                dest.writeInt(this.captureFormat.getSampleRate());
                dest.writeInt(this.captureFormat.getEncoding());
                dest.writeInt(this.captureFormat.getChannelMask());
            } else {
                dest.writeByte((byte) 0);
            }
            dest.writeBlob(this.data);
            dest.writeTypedArray(this.keyphraseExtras, flags);
        }

        public int describeContents() {
            return 0;
        }

        public int hashCode() {
            return (31 * super.hashCode()) + Arrays.hashCode(this.keyphraseExtras);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || getClass() != obj.getClass()) {
                return false;
            }
            if (Arrays.equals(this.keyphraseExtras, ((KeyphraseRecognitionEvent) obj).keyphraseExtras)) {
                return true;
            }
            return false;
        }

        public String toString() {
            String str;
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("KeyphraseRecognitionEvent [keyphraseExtras=");
            stringBuilder2.append(Arrays.toString(this.keyphraseExtras));
            stringBuilder2.append(", status=");
            stringBuilder2.append(this.status);
            stringBuilder2.append(", soundModelHandle=");
            stringBuilder2.append(this.soundModelHandle);
            stringBuilder2.append(", captureAvailable=");
            stringBuilder2.append(this.captureAvailable);
            stringBuilder2.append(", captureSession=");
            stringBuilder2.append(this.captureSession);
            stringBuilder2.append(", captureDelayMs=");
            stringBuilder2.append(this.captureDelayMs);
            stringBuilder2.append(", capturePreambleMs=");
            stringBuilder2.append(this.capturePreambleMs);
            stringBuilder2.append(", triggerInData=");
            stringBuilder2.append(this.triggerInData);
            if (this.captureFormat == null) {
                str = "";
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(", sampleRate=");
                stringBuilder.append(this.captureFormat.getSampleRate());
                str = stringBuilder.toString();
            }
            stringBuilder2.append(str);
            if (this.captureFormat == null) {
                str = "";
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(", encoding=");
                stringBuilder.append(this.captureFormat.getEncoding());
                str = stringBuilder.toString();
            }
            stringBuilder2.append(str);
            if (this.captureFormat == null) {
                str = "";
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(", channelMask=");
                stringBuilder.append(this.captureFormat.getChannelMask());
                str = stringBuilder.toString();
            }
            stringBuilder2.append(str);
            stringBuilder2.append(", data=");
            stringBuilder2.append(this.data == null ? 0 : this.data.length);
            stringBuilder2.append("]");
            return stringBuilder2.toString();
        }
    }

    public static class KeyphraseRecognitionExtra implements Parcelable {
        public static final Creator<KeyphraseRecognitionExtra> CREATOR = new Creator<KeyphraseRecognitionExtra>() {
            public KeyphraseRecognitionExtra createFromParcel(Parcel in) {
                return KeyphraseRecognitionExtra.fromParcel(in);
            }

            public KeyphraseRecognitionExtra[] newArray(int size) {
                return new KeyphraseRecognitionExtra[size];
            }
        };
        public final int coarseConfidenceLevel;
        public final ConfidenceLevel[] confidenceLevels;
        public final int id;
        public final int recognitionModes;

        public KeyphraseRecognitionExtra(int id, int recognitionModes, int coarseConfidenceLevel, ConfidenceLevel[] confidenceLevels) {
            this.id = id;
            this.recognitionModes = recognitionModes;
            this.coarseConfidenceLevel = coarseConfidenceLevel;
            this.confidenceLevels = confidenceLevels;
        }

        private static KeyphraseRecognitionExtra fromParcel(Parcel in) {
            return new KeyphraseRecognitionExtra(in.readInt(), in.readInt(), in.readInt(), (ConfidenceLevel[]) in.createTypedArray(ConfidenceLevel.CREATOR));
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.id);
            dest.writeInt(this.recognitionModes);
            dest.writeInt(this.coarseConfidenceLevel);
            dest.writeTypedArray(this.confidenceLevels, flags);
        }

        public int describeContents() {
            return 0;
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * ((31 * 1) + Arrays.hashCode(this.confidenceLevels))) + this.id)) + this.recognitionModes)) + this.coarseConfidenceLevel;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            KeyphraseRecognitionExtra other = (KeyphraseRecognitionExtra) obj;
            if (Arrays.equals(this.confidenceLevels, other.confidenceLevels) && this.id == other.id && this.recognitionModes == other.recognitionModes && this.coarseConfidenceLevel == other.coarseConfidenceLevel) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("KeyphraseRecognitionExtra [id=");
            stringBuilder.append(this.id);
            stringBuilder.append(", recognitionModes=");
            stringBuilder.append(this.recognitionModes);
            stringBuilder.append(", coarseConfidenceLevel=");
            stringBuilder.append(this.coarseConfidenceLevel);
            stringBuilder.append(", confidenceLevels=");
            stringBuilder.append(Arrays.toString(this.confidenceLevels));
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public static class KeyphraseSoundModel extends SoundModel implements Parcelable {
        public static final Creator<KeyphraseSoundModel> CREATOR = new Creator<KeyphraseSoundModel>() {
            public KeyphraseSoundModel createFromParcel(Parcel in) {
                return KeyphraseSoundModel.fromParcel(in);
            }

            public KeyphraseSoundModel[] newArray(int size) {
                return new KeyphraseSoundModel[size];
            }
        };
        public final Keyphrase[] keyphrases;

        public KeyphraseSoundModel(UUID uuid, UUID vendorUuid, byte[] data, Keyphrase[] keyphrases) {
            super(uuid, vendorUuid, 0, data);
            this.keyphrases = keyphrases;
        }

        private static KeyphraseSoundModel fromParcel(Parcel in) {
            UUID uuid = UUID.fromString(in.readString());
            UUID vendorUuid = null;
            if (in.readInt() >= 0) {
                vendorUuid = UUID.fromString(in.readString());
            }
            return new KeyphraseSoundModel(uuid, vendorUuid, in.readBlob(), (Keyphrase[]) in.createTypedArray(Keyphrase.CREATOR));
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.uuid.toString());
            if (this.vendorUuid == null) {
                dest.writeInt(-1);
            } else {
                dest.writeInt(this.vendorUuid.toString().length());
                dest.writeString(this.vendorUuid.toString());
            }
            dest.writeBlob(this.data);
            dest.writeTypedArray(this.keyphrases, flags);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("KeyphraseSoundModel [keyphrases=");
            stringBuilder.append(Arrays.toString(this.keyphrases));
            stringBuilder.append(", uuid=");
            stringBuilder.append(this.uuid);
            stringBuilder.append(", vendorUuid=");
            stringBuilder.append(this.vendorUuid);
            stringBuilder.append(", type=");
            stringBuilder.append(this.type);
            stringBuilder.append(", data=");
            stringBuilder.append(this.data == null ? 0 : this.data.length);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public int hashCode() {
            return (31 * super.hashCode()) + Arrays.hashCode(this.keyphrases);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof KeyphraseSoundModel)) {
                return false;
            }
            if (Arrays.equals(this.keyphrases, ((KeyphraseSoundModel) obj).keyphrases)) {
                return true;
            }
            return false;
        }
    }

    public static class ModuleProperties implements Parcelable {
        public static final Creator<ModuleProperties> CREATOR = new Creator<ModuleProperties>() {
            public ModuleProperties createFromParcel(Parcel in) {
                return ModuleProperties.fromParcel(in);
            }

            public ModuleProperties[] newArray(int size) {
                return new ModuleProperties[size];
            }
        };
        public final String description;
        public final int id;
        public final String implementor;
        public final int maxBufferMs;
        public final int maxKeyphrases;
        public final int maxSoundModels;
        public final int maxUsers;
        public final int powerConsumptionMw;
        public final int recognitionModes;
        public final boolean returnsTriggerInEvent;
        public final boolean supportsCaptureTransition;
        public final boolean supportsConcurrentCapture;
        public final UUID uuid;
        public final int version;

        ModuleProperties(int id, String implementor, String description, String uuid, int version, int maxSoundModels, int maxKeyphrases, int maxUsers, int recognitionModes, boolean supportsCaptureTransition, int maxBufferMs, boolean supportsConcurrentCapture, int powerConsumptionMw, boolean returnsTriggerInEvent) {
            this.id = id;
            this.implementor = implementor;
            this.description = description;
            this.uuid = UUID.fromString(uuid);
            this.version = version;
            this.maxSoundModels = maxSoundModels;
            this.maxKeyphrases = maxKeyphrases;
            this.maxUsers = maxUsers;
            this.recognitionModes = recognitionModes;
            this.supportsCaptureTransition = supportsCaptureTransition;
            this.maxBufferMs = maxBufferMs;
            this.supportsConcurrentCapture = supportsConcurrentCapture;
            this.powerConsumptionMw = powerConsumptionMw;
            this.returnsTriggerInEvent = returnsTriggerInEvent;
        }

        private static ModuleProperties fromParcel(Parcel in) {
            return new ModuleProperties(in.readInt(), in.readString(), in.readString(), in.readString(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readByte() == (byte) 1, in.readInt(), in.readByte() == (byte) 1, in.readInt(), in.readByte() == (byte) 1);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.id);
            dest.writeString(this.implementor);
            dest.writeString(this.description);
            dest.writeString(this.uuid.toString());
            dest.writeInt(this.version);
            dest.writeInt(this.maxSoundModels);
            dest.writeInt(this.maxKeyphrases);
            dest.writeInt(this.maxUsers);
            dest.writeInt(this.recognitionModes);
            dest.writeByte((byte) this.supportsCaptureTransition);
            dest.writeInt(this.maxBufferMs);
            dest.writeByte((byte) this.supportsConcurrentCapture);
            dest.writeInt(this.powerConsumptionMw);
            dest.writeByte((byte) this.returnsTriggerInEvent);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ModuleProperties [id=");
            stringBuilder.append(this.id);
            stringBuilder.append(", implementor=");
            stringBuilder.append(this.implementor);
            stringBuilder.append(", description=");
            stringBuilder.append(this.description);
            stringBuilder.append(", uuid=");
            stringBuilder.append(this.uuid);
            stringBuilder.append(", version=");
            stringBuilder.append(this.version);
            stringBuilder.append(", maxSoundModels=");
            stringBuilder.append(this.maxSoundModels);
            stringBuilder.append(", maxKeyphrases=");
            stringBuilder.append(this.maxKeyphrases);
            stringBuilder.append(", maxUsers=");
            stringBuilder.append(this.maxUsers);
            stringBuilder.append(", recognitionModes=");
            stringBuilder.append(this.recognitionModes);
            stringBuilder.append(", supportsCaptureTransition=");
            stringBuilder.append(this.supportsCaptureTransition);
            stringBuilder.append(", maxBufferMs=");
            stringBuilder.append(this.maxBufferMs);
            stringBuilder.append(", supportsConcurrentCapture=");
            stringBuilder.append(this.supportsConcurrentCapture);
            stringBuilder.append(", powerConsumptionMw=");
            stringBuilder.append(this.powerConsumptionMw);
            stringBuilder.append(", returnsTriggerInEvent=");
            stringBuilder.append(this.returnsTriggerInEvent);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public static class RecognitionConfig implements Parcelable {
        public static final Creator<RecognitionConfig> CREATOR = new Creator<RecognitionConfig>() {
            public RecognitionConfig createFromParcel(Parcel in) {
                return RecognitionConfig.fromParcel(in);
            }

            public RecognitionConfig[] newArray(int size) {
                return new RecognitionConfig[size];
            }
        };
        public final boolean allowMultipleTriggers;
        public final boolean captureRequested;
        public final byte[] data;
        public final KeyphraseRecognitionExtra[] keyphrases;

        public RecognitionConfig(boolean captureRequested, boolean allowMultipleTriggers, KeyphraseRecognitionExtra[] keyphrases, byte[] data) {
            this.captureRequested = captureRequested;
            this.allowMultipleTriggers = allowMultipleTriggers;
            this.keyphrases = keyphrases;
            this.data = data;
        }

        private static RecognitionConfig fromParcel(Parcel in) {
            boolean allowMultipleTriggers = false;
            boolean captureRequested = in.readByte() == (byte) 1;
            if (in.readByte() == (byte) 1) {
                allowMultipleTriggers = true;
            }
            return new RecognitionConfig(captureRequested, allowMultipleTriggers, (KeyphraseRecognitionExtra[]) in.createTypedArray(KeyphraseRecognitionExtra.CREATOR), in.readBlob());
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) this.captureRequested);
            dest.writeByte((byte) this.allowMultipleTriggers);
            dest.writeTypedArray(this.keyphrases, flags);
            dest.writeBlob(this.data);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RecognitionConfig [captureRequested=");
            stringBuilder.append(this.captureRequested);
            stringBuilder.append(", allowMultipleTriggers=");
            stringBuilder.append(this.allowMultipleTriggers);
            stringBuilder.append(", keyphrases=");
            stringBuilder.append(Arrays.toString(this.keyphrases));
            stringBuilder.append(", data=");
            stringBuilder.append(Arrays.toString(this.data));
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public static class SoundModelEvent implements Parcelable {
        public static final Creator<SoundModelEvent> CREATOR = new Creator<SoundModelEvent>() {
            public SoundModelEvent createFromParcel(Parcel in) {
                return SoundModelEvent.fromParcel(in);
            }

            public SoundModelEvent[] newArray(int size) {
                return new SoundModelEvent[size];
            }
        };
        public final byte[] data;
        public final int soundModelHandle;
        public final int status;

        SoundModelEvent(int status, int soundModelHandle, byte[] data) {
            this.status = status;
            this.soundModelHandle = soundModelHandle;
            this.data = data;
        }

        private static SoundModelEvent fromParcel(Parcel in) {
            return new SoundModelEvent(in.readInt(), in.readInt(), in.readBlob());
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.status);
            dest.writeInt(this.soundModelHandle);
            dest.writeBlob(this.data);
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * 1) + Arrays.hashCode(this.data))) + this.soundModelHandle)) + this.status;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SoundModelEvent other = (SoundModelEvent) obj;
            if (Arrays.equals(this.data, other.data) && this.soundModelHandle == other.soundModelHandle && this.status == other.status) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SoundModelEvent [status=");
            stringBuilder.append(this.status);
            stringBuilder.append(", soundModelHandle=");
            stringBuilder.append(this.soundModelHandle);
            stringBuilder.append(", data=");
            stringBuilder.append(this.data == null ? 0 : this.data.length);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    public static native int listModules(ArrayList<ModuleProperties> arrayList);

    private SoundTrigger() {
    }

    public static SoundTriggerModule attachModule(int moduleId, StatusListener listener, Handler handler) {
        if (listener == null) {
            return null;
        }
        return new SoundTriggerModule(moduleId, listener, handler);
    }
}
