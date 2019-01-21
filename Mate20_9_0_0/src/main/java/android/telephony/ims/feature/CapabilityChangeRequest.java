package android.telephony.ims.feature;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.ArraySet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SystemApi
public final class CapabilityChangeRequest implements Parcelable {
    public static final Creator<CapabilityChangeRequest> CREATOR = new Creator<CapabilityChangeRequest>() {
        public CapabilityChangeRequest createFromParcel(Parcel in) {
            return new CapabilityChangeRequest(in);
        }

        public CapabilityChangeRequest[] newArray(int size) {
            return new CapabilityChangeRequest[size];
        }
    };
    private final Set<CapabilityPair> mCapabilitiesToDisable;
    private final Set<CapabilityPair> mCapabilitiesToEnable;

    public static class CapabilityPair {
        private final int mCapability;
        private final int radioTech;

        public CapabilityPair(int capability, int radioTech) {
            this.mCapability = capability;
            this.radioTech = radioTech;
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (!(o instanceof CapabilityPair)) {
                return false;
            }
            CapabilityPair that = (CapabilityPair) o;
            if (getCapability() != that.getCapability()) {
                return false;
            }
            if (getRadioTech() != that.getRadioTech()) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return (31 * getCapability()) + getRadioTech();
        }

        public int getCapability() {
            return this.mCapability;
        }

        public int getRadioTech() {
            return this.radioTech;
        }
    }

    public CapabilityChangeRequest() {
        this.mCapabilitiesToEnable = new ArraySet();
        this.mCapabilitiesToDisable = new ArraySet();
    }

    public void addCapabilitiesToEnableForTech(int capabilities, int radioTech) {
        addAllCapabilities(this.mCapabilitiesToEnable, capabilities, radioTech);
    }

    public void addCapabilitiesToDisableForTech(int capabilities, int radioTech) {
        addAllCapabilities(this.mCapabilitiesToDisable, capabilities, radioTech);
    }

    public List<CapabilityPair> getCapabilitiesToEnable() {
        return new ArrayList(this.mCapabilitiesToEnable);
    }

    public List<CapabilityPair> getCapabilitiesToDisable() {
        return new ArrayList(this.mCapabilitiesToDisable);
    }

    private void addAllCapabilities(Set<CapabilityPair> set, int capabilities, int tech) {
        long highestCapability = Long.highestOneBit((long) capabilities);
        for (int i = 1; ((long) i) <= highestCapability; i *= 2) {
            if ((i & capabilities) > 0) {
                set.add(new CapabilityPair(i, tech));
            }
        }
    }

    protected CapabilityChangeRequest(Parcel in) {
        int i;
        int enableSize = in.readInt();
        this.mCapabilitiesToEnable = new ArraySet(enableSize);
        int i2 = 0;
        for (i = 0; i < enableSize; i++) {
            this.mCapabilitiesToEnable.add(new CapabilityPair(in.readInt(), in.readInt()));
        }
        i = in.readInt();
        this.mCapabilitiesToDisable = new ArraySet(i);
        while (i2 < i) {
            this.mCapabilitiesToDisable.add(new CapabilityPair(in.readInt(), in.readInt()));
            i2++;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mCapabilitiesToEnable.size());
        for (CapabilityPair pair : this.mCapabilitiesToEnable) {
            dest.writeInt(pair.getCapability());
            dest.writeInt(pair.getRadioTech());
        }
        dest.writeInt(this.mCapabilitiesToDisable.size());
        for (CapabilityPair pair2 : this.mCapabilitiesToDisable) {
            dest.writeInt(pair2.getCapability());
            dest.writeInt(pair2.getRadioTech());
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CapabilityChangeRequest)) {
            return false;
        }
        CapabilityChangeRequest that = (CapabilityChangeRequest) o;
        if (this.mCapabilitiesToEnable.equals(that.mCapabilitiesToEnable)) {
            return this.mCapabilitiesToDisable.equals(that.mCapabilitiesToDisable);
        }
        return false;
    }

    public int hashCode() {
        return (31 * this.mCapabilitiesToEnable.hashCode()) + this.mCapabilitiesToDisable.hashCode();
    }
}
