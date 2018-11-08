package android.hardware.radio;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class ProgramSelector implements Parcelable {
    public static final Creator<ProgramSelector> CREATOR = new Creator<ProgramSelector>() {
        public ProgramSelector createFromParcel(Parcel in) {
            return new ProgramSelector(in);
        }

        public ProgramSelector[] newArray(int size) {
            return new ProgramSelector[size];
        }
    };
    public static final int IDENTIFIER_TYPE_AMFM_FREQUENCY = 1;
    public static final int IDENTIFIER_TYPE_DAB_ENSEMBLE = 6;
    public static final int IDENTIFIER_TYPE_DAB_FREQUENCY = 8;
    public static final int IDENTIFIER_TYPE_DAB_SCID = 7;
    public static final int IDENTIFIER_TYPE_DAB_SIDECC = 5;
    public static final int IDENTIFIER_TYPE_DRMO_FREQUENCY = 10;
    public static final int IDENTIFIER_TYPE_DRMO_MODULATION = 11;
    public static final int IDENTIFIER_TYPE_DRMO_SERVICE_ID = 9;
    public static final int IDENTIFIER_TYPE_HD_STATION_ID_EXT = 3;
    public static final int IDENTIFIER_TYPE_HD_SUBCHANNEL = 4;
    public static final int IDENTIFIER_TYPE_RDS_PI = 2;
    public static final int IDENTIFIER_TYPE_SXM_CHANNEL = 13;
    public static final int IDENTIFIER_TYPE_SXM_SERVICE_ID = 12;
    public static final int IDENTIFIER_TYPE_VENDOR_PRIMARY_END = 1999;
    public static final int IDENTIFIER_TYPE_VENDOR_PRIMARY_START = 1000;
    public static final int PROGRAM_TYPE_AM = 1;
    public static final int PROGRAM_TYPE_AM_HD = 3;
    public static final int PROGRAM_TYPE_DAB = 5;
    public static final int PROGRAM_TYPE_DRMO = 6;
    public static final int PROGRAM_TYPE_FM = 2;
    public static final int PROGRAM_TYPE_FM_HD = 4;
    public static final int PROGRAM_TYPE_SXM = 7;
    public static final int PROGRAM_TYPE_VENDOR_END = 1999;
    public static final int PROGRAM_TYPE_VENDOR_START = 1000;
    private final Identifier mPrimaryId;
    private final int mProgramType;
    private final Identifier[] mSecondaryIds;
    private final long[] mVendorIds;

    public static final class Identifier implements Parcelable {
        public static final Creator<Identifier> CREATOR = new Creator<Identifier>() {
            public Identifier createFromParcel(Parcel in) {
                return new Identifier(in);
            }

            public Identifier[] newArray(int size) {
                return new Identifier[size];
            }
        };
        private final int mType;
        private final long mValue;

        public Identifier(int type, long value) {
            this.mType = type;
            this.mValue = value;
        }

        public int getType() {
            return this.mType;
        }

        public long getValue() {
            return this.mValue;
        }

        public String toString() {
            return "Identifier(" + this.mType + ", " + this.mValue + ")";
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Integer.valueOf(this.mType), Long.valueOf(this.mValue)});
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Identifier)) {
                return false;
            }
            Identifier other = (Identifier) obj;
            if (!(other.getType() == this.mType && other.getValue() == this.mValue)) {
                z = false;
            }
            return z;
        }

        private Identifier(Parcel in) {
            this.mType = in.readInt();
            this.mValue = in.readLong();
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mType);
            dest.writeLong(this.mValue);
        }

        public int describeContents() {
            return 0;
        }
    }

    public ProgramSelector(int programType, Identifier primaryId, Identifier[] secondaryIds, long[] vendorIds) {
        if (secondaryIds == null) {
            secondaryIds = new Identifier[0];
        }
        if (vendorIds == null) {
            vendorIds = new long[0];
        }
        if (Stream.of(secondaryIds).anyMatch(-$Lambda$YT5WdsCCCONt9rJHRq-uXhDUWbM.$INST$0)) {
            throw new IllegalArgumentException("secondaryIds list must not contain nulls");
        }
        this.mProgramType = programType;
        this.mPrimaryId = (Identifier) Objects.requireNonNull(primaryId);
        this.mSecondaryIds = secondaryIds;
        this.mVendorIds = vendorIds;
    }

    static /* synthetic */ boolean lambda$-android_hardware_radio_ProgramSelector_7454(Identifier id) {
        return id == null;
    }

    public int getProgramType() {
        return this.mProgramType;
    }

    public Identifier getPrimaryId() {
        return this.mPrimaryId;
    }

    public Identifier[] getSecondaryIds() {
        return this.mSecondaryIds;
    }

    public long getFirstId(int type) {
        if (this.mPrimaryId.getType() == type) {
            return this.mPrimaryId.getValue();
        }
        for (Identifier id : this.mSecondaryIds) {
            if (id.getType() == type) {
                return id.getValue();
            }
        }
        throw new IllegalArgumentException("Identifier " + type + " not found");
    }

    public Identifier[] getAllIds(int type) {
        List<Identifier> out = new ArrayList();
        if (this.mPrimaryId.getType() == type) {
            out.add(this.mPrimaryId);
        }
        for (Identifier id : this.mSecondaryIds) {
            if (id.getType() == type) {
                out.add(id);
            }
        }
        return (Identifier[]) out.toArray(new Identifier[out.size()]);
    }

    public long[] getVendorIds() {
        return this.mVendorIds;
    }

    public static ProgramSelector createAmFmSelector(int band, int frequencyKhz) {
        return createAmFmSelector(band, frequencyKhz, 0);
    }

    private static boolean isValidAmFmFrequency(boolean isAm, int frequencyKhz) {
        boolean z = true;
        boolean z2 = false;
        if (isAm) {
            if (frequencyKhz <= 150 || frequencyKhz >= 30000) {
                z = false;
            }
            return z;
        }
        if (frequencyKhz > ProvisioningThread.TIMEOUT_MS && frequencyKhz < 110000) {
            z2 = true;
        }
        return z2;
    }

    public static ProgramSelector createAmFmSelector(int band, int frequencyKhz, int subChannel) {
        boolean isAm = band == 0 || band == 3;
        boolean isDigital = band == 3 || band == 2;
        if (!isAm && (isDigital ^ 1) != 0 && band != 1) {
            throw new IllegalArgumentException("Unknown band: " + band);
        } else if (subChannel < 0 || subChannel > 8) {
            throw new IllegalArgumentException("Invalid subchannel: " + subChannel);
        } else if (subChannel > 0 && (isDigital ^ 1) != 0) {
            throw new IllegalArgumentException("Subchannels are not supported for non-HD radio");
        } else if (isValidAmFmFrequency(isAm, frequencyKhz)) {
            int programType = isAm ? 1 : 2;
            Identifier primary = new Identifier(1, (long) frequencyKhz);
            Identifier[] identifierArr = null;
            if (subChannel > 0) {
                identifierArr = new Identifier[]{new Identifier(4, (long) (subChannel - 1))};
            }
            return new ProgramSelector(programType, primary, identifierArr, null);
        } else {
            throw new IllegalArgumentException("Provided value is not a valid AM/FM frequency");
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ProgramSelector(type=").append(this.mProgramType).append(", primary=").append(this.mPrimaryId);
        if (this.mSecondaryIds.length > 0) {
            sb.append(", secondary=").append(this.mSecondaryIds);
        }
        if (this.mVendorIds.length > 0) {
            sb.append(", vendor=").append(this.mVendorIds);
        }
        sb.append(")");
        return sb.toString();
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(this.mProgramType), this.mPrimaryId});
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProgramSelector)) {
            return false;
        }
        ProgramSelector other = (ProgramSelector) obj;
        if (other.getProgramType() == this.mProgramType) {
            z = this.mPrimaryId.equals(other.getPrimaryId());
        }
        return z;
    }

    private ProgramSelector(Parcel in) {
        this.mProgramType = in.readInt();
        this.mPrimaryId = (Identifier) in.readTypedObject(Identifier.CREATOR);
        this.mSecondaryIds = (Identifier[]) in.createTypedArray(Identifier.CREATOR);
        if (Stream.of(this.mSecondaryIds).anyMatch(-$Lambda$YT5WdsCCCONt9rJHRq-uXhDUWbM.$INST$1)) {
            throw new IllegalArgumentException("secondaryIds list must not contain nulls");
        }
        this.mVendorIds = in.createLongArray();
    }

    static /* synthetic */ boolean lambda$-android_hardware_radio_ProgramSelector_14965(Identifier id) {
        return id == null;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mProgramType);
        dest.writeTypedObject(this.mPrimaryId, 0);
        dest.writeTypedArray(this.mSecondaryIds, 0);
        dest.writeLongArray(this.mVendorIds);
    }

    public int describeContents() {
        return 0;
    }
}
