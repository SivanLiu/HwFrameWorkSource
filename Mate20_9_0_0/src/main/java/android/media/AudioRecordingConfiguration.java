package android.media;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Objects;

public final class AudioRecordingConfiguration implements Parcelable {
    public static final Creator<AudioRecordingConfiguration> CREATOR = new Creator<AudioRecordingConfiguration>() {
        public AudioRecordingConfiguration createFromParcel(Parcel p) {
            return new AudioRecordingConfiguration(p, null);
        }

        public AudioRecordingConfiguration[] newArray(int size) {
            return new AudioRecordingConfiguration[size];
        }
    };
    private static final String TAG = new String("AudioRecordingConfiguration");
    private final AudioFormat mClientFormat;
    private final String mClientPackageName;
    private final int mClientSource;
    private final int mClientUid;
    private final AudioFormat mDeviceFormat;
    private final int mPatchHandle;
    private final int mSessionId;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AudioSource {
    }

    /* synthetic */ AudioRecordingConfiguration(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public AudioRecordingConfiguration(int uid, int session, int source, AudioFormat clientFormat, AudioFormat devFormat, int patchHandle, String packageName) {
        this.mClientUid = uid;
        this.mSessionId = session;
        this.mClientSource = source;
        this.mDeviceFormat = devFormat;
        this.mClientFormat = clientFormat;
        this.mPatchHandle = patchHandle;
        this.mClientPackageName = packageName;
    }

    public void dump(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  ");
        stringBuilder.append(toLogFriendlyString(this));
        pw.println(stringBuilder.toString());
    }

    public static String toLogFriendlyString(AudioRecordingConfiguration arc) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("session:");
        stringBuilder.append(arc.mSessionId);
        stringBuilder.append(" -- source:");
        stringBuilder.append(MediaRecorder.toLogFriendlyAudioSource(arc.mClientSource));
        stringBuilder.append(" -- uid:");
        stringBuilder.append(arc.mClientUid);
        stringBuilder.append(" -- patch:");
        stringBuilder.append(arc.mPatchHandle);
        stringBuilder.append(" -- pack:");
        stringBuilder.append(arc.mClientPackageName);
        stringBuilder.append(" -- format client=");
        stringBuilder.append(arc.mClientFormat.toLogFriendlyString());
        stringBuilder.append(", dev=");
        stringBuilder.append(arc.mDeviceFormat.toLogFriendlyString());
        return new String(stringBuilder.toString());
    }

    public static AudioRecordingConfiguration anonymizedCopy(AudioRecordingConfiguration in) {
        return new AudioRecordingConfiguration(-1, in.mSessionId, in.mClientSource, in.mClientFormat, in.mDeviceFormat, in.mPatchHandle, "");
    }

    public int getClientAudioSource() {
        return this.mClientSource;
    }

    public int getClientAudioSessionId() {
        return this.mSessionId;
    }

    public AudioFormat getFormat() {
        return this.mDeviceFormat;
    }

    public AudioFormat getClientFormat() {
        return this.mClientFormat;
    }

    public String getClientPackageName() {
        return this.mClientPackageName;
    }

    public int getClientUid() {
        return this.mClientUid;
    }

    public AudioDeviceInfo getAudioDevice() {
        ArrayList<AudioPatch> patches = new ArrayList();
        if (AudioManager.listAudioPatches(patches) != 0) {
            Log.e(TAG, "Error retrieving list of audio patches");
            return null;
        }
        int j = 0;
        for (int i = 0; i < patches.size(); i++) {
            AudioPatch patch = (AudioPatch) patches.get(i);
            if (patch.id() == this.mPatchHandle) {
                AudioPortConfig[] sources = patch.sources();
                if (sources != null && sources.length > 0) {
                    int devId = sources[0].port().id();
                    AudioDeviceInfo[] devices = AudioManager.getDevicesStatic(1);
                    while (j < devices.length) {
                        if (devices[j].getId() == devId) {
                            return devices[j];
                        }
                        j++;
                    }
                }
                Log.e(TAG, "Couldn't find device for recording, did recording end already?");
                return null;
            }
        }
        Log.e(TAG, "Couldn't find device for recording, did recording end already?");
        return null;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(this.mSessionId), Integer.valueOf(this.mClientSource)});
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSessionId);
        dest.writeInt(this.mClientSource);
        this.mClientFormat.writeToParcel(dest, 0);
        this.mDeviceFormat.writeToParcel(dest, 0);
        dest.writeInt(this.mPatchHandle);
        dest.writeString(this.mClientPackageName);
        dest.writeInt(this.mClientUid);
    }

    private AudioRecordingConfiguration(Parcel in) {
        this.mSessionId = in.readInt();
        this.mClientSource = in.readInt();
        this.mClientFormat = (AudioFormat) AudioFormat.CREATOR.createFromParcel(in);
        this.mDeviceFormat = (AudioFormat) AudioFormat.CREATOR.createFromParcel(in);
        this.mPatchHandle = in.readInt();
        this.mClientPackageName = in.readString();
        this.mClientUid = in.readInt();
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof AudioRecordingConfiguration)) {
            return false;
        }
        AudioRecordingConfiguration that = (AudioRecordingConfiguration) o;
        if (!(this.mClientUid == that.mClientUid && this.mSessionId == that.mSessionId && this.mClientSource == that.mClientSource && this.mPatchHandle == that.mPatchHandle && this.mClientFormat.equals(that.mClientFormat) && this.mDeviceFormat.equals(that.mDeviceFormat) && this.mClientPackageName.equals(that.mClientPackageName))) {
            z = false;
        }
        return z;
    }
}
