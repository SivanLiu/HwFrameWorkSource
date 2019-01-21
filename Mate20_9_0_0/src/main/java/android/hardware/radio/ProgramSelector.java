package android.hardware.radio;

import android.annotation.SystemApi;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@SystemApi
public final class ProgramSelector implements Parcelable {
    public static final Creator<ProgramSelector> CREATOR = new Creator<ProgramSelector>() {
        public ProgramSelector createFromParcel(Parcel in) {
            return new ProgramSelector(in, null);
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
    public static final int IDENTIFIER_TYPE_DAB_SID_EXT = 5;
    public static final int IDENTIFIER_TYPE_DRMO_FREQUENCY = 10;
    @Deprecated
    public static final int IDENTIFIER_TYPE_DRMO_MODULATION = 11;
    public static final int IDENTIFIER_TYPE_DRMO_SERVICE_ID = 9;
    public static final int IDENTIFIER_TYPE_HD_STATION_ID_EXT = 3;
    public static final int IDENTIFIER_TYPE_HD_STATION_NAME = 10004;
    @Deprecated
    public static final int IDENTIFIER_TYPE_HD_SUBCHANNEL = 4;
    public static final int IDENTIFIER_TYPE_INVALID = 0;
    public static final int IDENTIFIER_TYPE_RDS_PI = 2;
    public static final int IDENTIFIER_TYPE_SXM_CHANNEL = 13;
    public static final int IDENTIFIER_TYPE_SXM_SERVICE_ID = 12;
    public static final int IDENTIFIER_TYPE_VENDOR_END = 1999;
    @Deprecated
    public static final int IDENTIFIER_TYPE_VENDOR_PRIMARY_END = 1999;
    @Deprecated
    public static final int IDENTIFIER_TYPE_VENDOR_PRIMARY_START = 1000;
    public static final int IDENTIFIER_TYPE_VENDOR_START = 1000;
    @Deprecated
    public static final int PROGRAM_TYPE_AM = 1;
    @Deprecated
    public static final int PROGRAM_TYPE_AM_HD = 3;
    @Deprecated
    public static final int PROGRAM_TYPE_DAB = 5;
    @Deprecated
    public static final int PROGRAM_TYPE_DRMO = 6;
    @Deprecated
    public static final int PROGRAM_TYPE_FM = 2;
    @Deprecated
    public static final int PROGRAM_TYPE_FM_HD = 4;
    @Deprecated
    public static final int PROGRAM_TYPE_INVALID = 0;
    @Deprecated
    public static final int PROGRAM_TYPE_SXM = 7;
    @Deprecated
    public static final int PROGRAM_TYPE_VENDOR_END = 1999;
    @Deprecated
    public static final int PROGRAM_TYPE_VENDOR_START = 1000;
    private final Identifier mPrimaryId;
    private final int mProgramType;
    private final Identifier[] mSecondaryIds;
    private final long[] mVendorIds;

    @Retention(RetentionPolicy.SOURCE)
    public @interface IdentifierType {
    }

    @Deprecated
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProgramType {
    }

    public static final class Identifier implements Parcelable {
        public static final Creator<Identifier> CREATOR = new Creator<Identifier>() {
            public Identifier createFromParcel(Parcel in) {
                return new Identifier(in, null);
            }

            public Identifier[] newArray(int size) {
                return new Identifier[size];
            }
        };
        private final int mType;
        private final long mValue;

        public Identifier(int type, long value) {
            if (type == ProgramSelector.IDENTIFIER_TYPE_HD_STATION_NAME) {
                type = 4;
            }
            this.mType = type;
            this.mValue = value;
        }

        public int getType() {
            if (this.mType != 4 || this.mValue <= 10) {
                return this.mType;
            }
            return ProgramSelector.IDENTIFIER_TYPE_HD_STATION_NAME;
        }

        public long getValue() {
            return this.mValue;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Identifier(");
            stringBuilder.append(this.mType);
            stringBuilder.append(", ");
            stringBuilder.append(this.mValue);
            stringBuilder.append(")");
            return stringBuilder.toString();
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

    /* synthetic */ ProgramSelector(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public ProgramSelector(int programType, Identifier primaryId, Identifier[] secondaryIds, long[] vendorIds) {
        Object[] secondaryIds2;
        if (secondaryIds2 == null) {
            secondaryIds2 = new Identifier[0];
        }
        if (vendorIds == null) {
            vendorIds = new long[0];
        }
        if (Stream.of(secondaryIds2).anyMatch(-$$Lambda$ProgramSelector$pP-Cu6h7-REdNveY60TFDS4pIKk.INSTANCE)) {
            throw new IllegalArgumentException("secondaryIds list must not contain nulls");
        }
        this.mProgramType = programType;
        this.mPrimaryId = (Identifier) Objects.requireNonNull(primaryId);
        this.mSecondaryIds = secondaryIds2;
        this.mVendorIds = vendorIds;
    }

    static /* synthetic */ boolean lambda$new$0(Identifier id) {
        return id == null;
    }

    @Deprecated
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Identifier ");
        stringBuilder.append(type);
        stringBuilder.append(" not found");
        throw new IllegalArgumentException(stringBuilder.toString());
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

    @Deprecated
    public long[] getVendorIds() {
        return this.mVendorIds;
    }

    public ProgramSelector withSecondaryPreferred(Identifier preferred) {
        return new ProgramSelector(this.mProgramType, this.mPrimaryId, (Identifier[]) Stream.concat(Arrays.stream(this.mSecondaryIds).filter(new -$$Lambda$ProgramSelector$TWK8H6GGx8Rt5rbA87tKag-pCqw(preferred.getType())), Stream.of(preferred)).toArray(-$$Lambda$ProgramSelector$kEsOH_p_eN5KvKLjoDTGZXYtuP4.INSTANCE), this.mVendorIds);
    }

    static /* synthetic */ boolean lambda$withSecondaryPreferred$1(int preferredType, Identifier id) {
        return id.getType() != preferredType;
    }

    public static ProgramSelector createAmFmSelector(int band, int frequencyKhz) {
        return createAmFmSelector(band, frequencyKhz, 0);
    }

    private static boolean isValidAmFmFrequency(boolean isAm, int frequencyKhz) {
        boolean z = false;
        if (isAm) {
            if (frequencyKhz > 150 && frequencyKhz <= 30000) {
                z = true;
            }
            return z;
        }
        if (frequencyKhz > 60000 && frequencyKhz < 110000) {
            z = true;
        }
        return z;
    }

    public static ProgramSelector createAmFmSelector(int band, int frequencyKhz, int subChannel) {
        int programType = 2;
        if (band == -1) {
            if (frequencyKhz < SQLiteDatabase.SQLITE_MAX_LIKE_PATTERN_LENGTH) {
                band = subChannel <= 0 ? 0 : 3;
            } else {
                band = subChannel <= 0 ? 1 : 2;
            }
        }
        boolean isAm = band == 0 || band == 3;
        boolean isDigital = band == 3 || band == 2;
        StringBuilder stringBuilder;
        if (!isAm && !isDigital && band != 1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown band: ");
            stringBuilder.append(band);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (subChannel < 0 || subChannel > 8) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid subchannel: ");
            stringBuilder.append(subChannel);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (subChannel > 0 && !isDigital) {
            throw new IllegalArgumentException("Subchannels are not supported for non-HD radio");
        } else if (isValidAmFmFrequency(isAm, frequencyKhz)) {
            if (isAm) {
                programType = 1;
            }
            Identifier primary = new Identifier(1, (long) frequencyKhz);
            Identifier[] secondary = null;
            if (subChannel > 0) {
                secondary = new Identifier[]{new Identifier(4, (long) (subChannel - 1))};
            }
            return new ProgramSelector(programType, primary, secondary, null);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Provided value is not a valid AM/FM frequency: ");
            stringBuilder.append(frequencyKhz);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ProgramSelector(type=");
        sb.append(this.mProgramType);
        sb.append(", primary=");
        sb = sb.append(this.mPrimaryId);
        if (this.mSecondaryIds.length > 0) {
            sb.append(", secondary=");
            sb.append(this.mSecondaryIds);
        }
        if (this.mVendorIds.length > 0) {
            sb.append(", vendor=");
            sb.append(this.mVendorIds);
        }
        sb.append(")");
        return sb.toString();
    }

    public int hashCode() {
        return this.mPrimaryId.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProgramSelector)) {
            return false;
        }
        return this.mPrimaryId.equals(((ProgramSelector) obj).getPrimaryId());
    }

    private ProgramSelector(Parcel in) {
        this.mProgramType = in.readInt();
        this.mPrimaryId = (Identifier) in.readTypedObject(Identifier.CREATOR);
        this.mSecondaryIds = (Identifier[]) in.createTypedArray(Identifier.CREATOR);
        if (Stream.of(this.mSecondaryIds).anyMatch(-$$Lambda$ProgramSelector$nFx6NE-itx7YUkyrPxAq5zDeJdQ.INSTANCE)) {
            throw new IllegalArgumentException("secondaryIds list must not contain nulls");
        }
        this.mVendorIds = in.createLongArray();
    }

    static /* synthetic */ boolean lambda$new$3(Identifier id) {
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
