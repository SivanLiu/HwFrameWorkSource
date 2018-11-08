package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Arrays;

public final class RadioAccessSpecifier implements Parcelable {
    public static final Creator<RadioAccessSpecifier> CREATOR = new Creator<RadioAccessSpecifier>() {
        public RadioAccessSpecifier createFromParcel(Parcel in) {
            return new RadioAccessSpecifier(in);
        }

        public RadioAccessSpecifier[] newArray(int size) {
            return new RadioAccessSpecifier[size];
        }
    };
    public int[] bands;
    public int[] channels;
    public int radioAccessNetwork;

    public RadioAccessSpecifier(int ran, int[] bands, int[] channels) {
        this.radioAccessNetwork = ran;
        this.bands = bands;
        this.channels = channels;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.radioAccessNetwork);
        dest.writeIntArray(this.bands);
        dest.writeIntArray(this.channels);
    }

    private RadioAccessSpecifier(Parcel in) {
        this.radioAccessNetwork = in.readInt();
        this.bands = in.createIntArray();
        this.channels = in.createIntArray();
    }

    public boolean equals(Object o) {
        boolean z = false;
        try {
            RadioAccessSpecifier ras = (RadioAccessSpecifier) o;
            if (o == null) {
                return false;
            }
            if (this.radioAccessNetwork == ras.radioAccessNetwork && Arrays.equals(this.bands, ras.bands)) {
                z = Arrays.equals(this.channels, ras.channels);
            }
            return z;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {
        return ((this.radioAccessNetwork * 31) + (Arrays.hashCode(this.bands) * 37)) + (Arrays.hashCode(this.channels) * 39);
    }
}
