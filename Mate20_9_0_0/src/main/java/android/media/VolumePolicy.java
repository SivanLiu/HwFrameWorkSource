package android.media;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Objects;

public final class VolumePolicy implements Parcelable {
    public static final int A11Y_MODE_INDEPENDENT_A11Y_VOLUME = 1;
    public static final int A11Y_MODE_MEDIA_A11Y_VOLUME = 0;
    public static final Creator<VolumePolicy> CREATOR = new Creator<VolumePolicy>() {
        public VolumePolicy createFromParcel(Parcel p) {
            boolean z = false;
            boolean z2 = p.readInt() != 0;
            boolean z3 = p.readInt() != 0;
            if (p.readInt() != 0) {
                z = true;
            }
            return new VolumePolicy(z2, z3, z, p.readInt());
        }

        public VolumePolicy[] newArray(int size) {
            return new VolumePolicy[size];
        }
    };
    public static final VolumePolicy DEFAULT = new VolumePolicy(false, true, false, 400);
    public final boolean doNotDisturbWhenSilent;
    public final int vibrateToSilentDebounce;
    public final boolean volumeDownToEnterSilent;
    public final boolean volumeUpToExitSilent;

    public VolumePolicy(boolean volumeDownToEnterSilent, boolean volumeUpToExitSilent, boolean doNotDisturbWhenSilent, int vibrateToSilentDebounce) {
        this.volumeDownToEnterSilent = volumeDownToEnterSilent;
        this.volumeUpToExitSilent = volumeUpToExitSilent;
        this.doNotDisturbWhenSilent = doNotDisturbWhenSilent;
        this.vibrateToSilentDebounce = vibrateToSilentDebounce;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("VolumePolicy[volumeDownToEnterSilent=");
        stringBuilder.append(this.volumeDownToEnterSilent);
        stringBuilder.append(",volumeUpToExitSilent=");
        stringBuilder.append(this.volumeUpToExitSilent);
        stringBuilder.append(",doNotDisturbWhenSilent=");
        stringBuilder.append(this.doNotDisturbWhenSilent);
        stringBuilder.append(",vibrateToSilentDebounce=");
        stringBuilder.append(this.vibrateToSilentDebounce);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Boolean.valueOf(this.volumeDownToEnterSilent), Boolean.valueOf(this.volumeUpToExitSilent), Boolean.valueOf(this.doNotDisturbWhenSilent), Integer.valueOf(this.vibrateToSilentDebounce)});
    }

    public boolean equals(Object o) {
        if (!(o instanceof VolumePolicy)) {
            return false;
        }
        boolean z = true;
        if (o == this) {
            return true;
        }
        VolumePolicy other = (VolumePolicy) o;
        if (!(other.volumeDownToEnterSilent == this.volumeDownToEnterSilent && other.volumeUpToExitSilent == this.volumeUpToExitSilent && other.doNotDisturbWhenSilent == this.doNotDisturbWhenSilent && other.vibrateToSilentDebounce == this.vibrateToSilentDebounce)) {
            z = false;
        }
        return z;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.volumeDownToEnterSilent);
        dest.writeInt(this.volumeUpToExitSilent);
        dest.writeInt(this.doNotDisturbWhenSilent);
        dest.writeInt(this.vibrateToSilentDebounce);
    }
}
