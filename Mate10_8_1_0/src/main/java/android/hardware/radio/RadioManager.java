package android.hardware.radio;

import android.content.Context;
import android.hardware.radio.IRadioService.Stub;
import android.hardware.radio.RadioTuner.Callback;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceManager.ServiceNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class RadioManager {
    public static final int BAND_AM = 0;
    public static final int BAND_AM_HD = 3;
    public static final int BAND_FM = 1;
    public static final int BAND_FM_HD = 2;
    public static final int BAND_INVALID = -1;
    public static final int CLASS_AM_FM = 0;
    public static final int CLASS_DT = 2;
    public static final int CLASS_SAT = 1;
    public static final int REGION_ITU_1 = 0;
    public static final int REGION_ITU_2 = 1;
    public static final int REGION_JAPAN = 3;
    public static final int REGION_KOREA = 4;
    public static final int REGION_OIRT = 2;
    public static final int STATUS_BAD_VALUE = -22;
    public static final int STATUS_DEAD_OBJECT = -32;
    public static final int STATUS_ERROR = Integer.MIN_VALUE;
    public static final int STATUS_INVALID_OPERATION = -38;
    public static final int STATUS_NO_INIT = -19;
    public static final int STATUS_OK = 0;
    public static final int STATUS_PERMISSION_DENIED = -1;
    public static final int STATUS_TIMED_OUT = -110;
    private static final String TAG = "BroadcastRadio.manager";
    private final Context mContext;
    private final IRadioService mService = Stub.asInterface(ServiceManager.getServiceOrThrow(Context.RADIO_SERVICE));

    public static class BandConfig implements Parcelable {
        public static final Creator<BandConfig> CREATOR = new Creator<BandConfig>() {
            public BandConfig createFromParcel(Parcel in) {
                int type = BandDescriptor.lookupTypeFromParcel(in);
                switch (type) {
                    case 0:
                    case 3:
                        return new AmBandConfig(in);
                    case 1:
                    case 2:
                        return new FmBandConfig(in);
                    default:
                        throw new IllegalArgumentException("Unsupported band: " + type);
                }
            }

            public BandConfig[] newArray(int size) {
                return new BandConfig[size];
            }
        };
        final BandDescriptor mDescriptor;

        BandConfig(BandDescriptor descriptor) {
            this.mDescriptor = descriptor;
        }

        BandConfig(int region, int type, int lowerLimit, int upperLimit, int spacing) {
            this.mDescriptor = new BandDescriptor(region, type, lowerLimit, upperLimit, spacing);
        }

        private BandConfig(Parcel in) {
            this.mDescriptor = new BandDescriptor(in);
        }

        BandDescriptor getDescriptor() {
            return this.mDescriptor;
        }

        public int getRegion() {
            return this.mDescriptor.getRegion();
        }

        public int getType() {
            return this.mDescriptor.getType();
        }

        public int getLowerLimit() {
            return this.mDescriptor.getLowerLimit();
        }

        public int getUpperLimit() {
            return this.mDescriptor.getUpperLimit();
        }

        public int getSpacing() {
            return this.mDescriptor.getSpacing();
        }

        public void writeToParcel(Parcel dest, int flags) {
            this.mDescriptor.writeToParcel(dest, flags);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "BandConfig [ " + this.mDescriptor.toString() + "]";
        }

        public int hashCode() {
            return this.mDescriptor.hashCode() + 31;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BandConfig)) {
                return false;
            }
            BandDescriptor otherDesc = ((BandConfig) obj).getDescriptor();
            if ((this.mDescriptor == null) != (otherDesc == null)) {
                return false;
            }
            return this.mDescriptor == null || (this.mDescriptor.equals(otherDesc) ^ 1) == 0;
        }
    }

    public static class AmBandConfig extends BandConfig {
        public static final Creator<AmBandConfig> CREATOR = new Creator<AmBandConfig>() {
            public AmBandConfig createFromParcel(Parcel in) {
                return new AmBandConfig(in);
            }

            public AmBandConfig[] newArray(int size) {
                return new AmBandConfig[size];
            }
        };
        private final boolean mStereo;

        public static class Builder {
            private final BandDescriptor mDescriptor;
            private boolean mStereo;

            public Builder(AmBandDescriptor descriptor) {
                this.mDescriptor = new BandDescriptor(descriptor.getRegion(), descriptor.getType(), descriptor.getLowerLimit(), descriptor.getUpperLimit(), descriptor.getSpacing());
                this.mStereo = descriptor.isStereoSupported();
            }

            public Builder(AmBandConfig config) {
                this.mDescriptor = new BandDescriptor(config.getRegion(), config.getType(), config.getLowerLimit(), config.getUpperLimit(), config.getSpacing());
                this.mStereo = config.getStereo();
            }

            public AmBandConfig build() {
                return new AmBandConfig(this.mDescriptor.getRegion(), this.mDescriptor.getType(), this.mDescriptor.getLowerLimit(), this.mDescriptor.getUpperLimit(), this.mDescriptor.getSpacing(), this.mStereo);
            }

            public Builder setStereo(boolean state) {
                this.mStereo = state;
                return this;
            }
        }

        AmBandConfig(AmBandDescriptor descriptor) {
            super((BandDescriptor) descriptor);
            this.mStereo = descriptor.isStereoSupported();
        }

        AmBandConfig(int region, int type, int lowerLimit, int upperLimit, int spacing, boolean stereo) {
            super(region, type, lowerLimit, upperLimit, spacing);
            this.mStereo = stereo;
        }

        public boolean getStereo() {
            return this.mStereo;
        }

        private AmBandConfig(Parcel in) {
            boolean z = true;
            super(in);
            if (in.readByte() != (byte) 1) {
                z = false;
            }
            this.mStereo = z;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (this.mStereo ? 1 : 0));
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "AmBandConfig [" + super.toString() + ", mStereo=" + this.mStereo + "]";
        }

        public int hashCode() {
            return (super.hashCode() * 31) + (this.mStereo ? 1 : 0);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof AmBandConfig)) {
                return false;
            }
            return this.mStereo == ((AmBandConfig) obj).getStereo();
        }
    }

    public static class BandDescriptor implements Parcelable {
        public static final Creator<BandDescriptor> CREATOR = new Creator<BandDescriptor>() {
            public BandDescriptor createFromParcel(Parcel in) {
                int type = BandDescriptor.lookupTypeFromParcel(in);
                switch (type) {
                    case 0:
                    case 3:
                        return new AmBandDescriptor(in);
                    case 1:
                    case 2:
                        return new FmBandDescriptor(in);
                    default:
                        throw new IllegalArgumentException("Unsupported band: " + type);
                }
            }

            public BandDescriptor[] newArray(int size) {
                return new BandDescriptor[size];
            }
        };
        private final int mLowerLimit;
        private final int mRegion;
        private final int mSpacing;
        private final int mType;
        private final int mUpperLimit;

        BandDescriptor(int region, int type, int lowerLimit, int upperLimit, int spacing) {
            if (type == 0 || type == 1 || type == 2 || type == 3) {
                this.mRegion = region;
                this.mType = type;
                this.mLowerLimit = lowerLimit;
                this.mUpperLimit = upperLimit;
                this.mSpacing = spacing;
                return;
            }
            throw new IllegalArgumentException("Unsupported band: " + type);
        }

        public int getRegion() {
            return this.mRegion;
        }

        public int getType() {
            return this.mType;
        }

        public boolean isAmBand() {
            return this.mType == 0 || this.mType == 3;
        }

        public boolean isFmBand() {
            return this.mType == 1 || this.mType == 2;
        }

        public int getLowerLimit() {
            return this.mLowerLimit;
        }

        public int getUpperLimit() {
            return this.mUpperLimit;
        }

        public int getSpacing() {
            return this.mSpacing;
        }

        private BandDescriptor(Parcel in) {
            this.mRegion = in.readInt();
            this.mType = in.readInt();
            this.mLowerLimit = in.readInt();
            this.mUpperLimit = in.readInt();
            this.mSpacing = in.readInt();
        }

        private static int lookupTypeFromParcel(Parcel in) {
            int pos = in.dataPosition();
            in.readInt();
            int type = in.readInt();
            in.setDataPosition(pos);
            return type;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mRegion);
            dest.writeInt(this.mType);
            dest.writeInt(this.mLowerLimit);
            dest.writeInt(this.mUpperLimit);
            dest.writeInt(this.mSpacing);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "BandDescriptor [mRegion=" + this.mRegion + ", mType=" + this.mType + ", mLowerLimit=" + this.mLowerLimit + ", mUpperLimit=" + this.mUpperLimit + ", mSpacing=" + this.mSpacing + "]";
        }

        public int hashCode() {
            return ((((((((this.mRegion + 31) * 31) + this.mType) * 31) + this.mLowerLimit) * 31) + this.mUpperLimit) * 31) + this.mSpacing;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BandDescriptor)) {
                return false;
            }
            BandDescriptor other = (BandDescriptor) obj;
            return this.mRegion == other.getRegion() && this.mType == other.getType() && this.mLowerLimit == other.getLowerLimit() && this.mUpperLimit == other.getUpperLimit() && this.mSpacing == other.getSpacing();
        }
    }

    public static class AmBandDescriptor extends BandDescriptor {
        public static final Creator<AmBandDescriptor> CREATOR = new Creator<AmBandDescriptor>() {
            public AmBandDescriptor createFromParcel(Parcel in) {
                return new AmBandDescriptor(in);
            }

            public AmBandDescriptor[] newArray(int size) {
                return new AmBandDescriptor[size];
            }
        };
        private final boolean mStereo;

        AmBandDescriptor(int region, int type, int lowerLimit, int upperLimit, int spacing, boolean stereo) {
            super(region, type, lowerLimit, upperLimit, spacing);
            this.mStereo = stereo;
        }

        public boolean isStereoSupported() {
            return this.mStereo;
        }

        private AmBandDescriptor(Parcel in) {
            boolean z = true;
            super(in);
            if (in.readByte() != (byte) 1) {
                z = false;
            }
            this.mStereo = z;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (this.mStereo ? 1 : 0));
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "AmBandDescriptor [ " + super.toString() + " mStereo=" + this.mStereo + "]";
        }

        public int hashCode() {
            return (super.hashCode() * 31) + (this.mStereo ? 1 : 0);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof AmBandDescriptor)) {
                return false;
            }
            return this.mStereo == ((AmBandDescriptor) obj).isStereoSupported();
        }
    }

    public static class FmBandConfig extends BandConfig {
        public static final Creator<FmBandConfig> CREATOR = new Creator<FmBandConfig>() {
            public FmBandConfig createFromParcel(Parcel in) {
                return new FmBandConfig(in);
            }

            public FmBandConfig[] newArray(int size) {
                return new FmBandConfig[size];
            }
        };
        private final boolean mAf;
        private final boolean mEa;
        private final boolean mRds;
        private final boolean mStereo;
        private final boolean mTa;

        public static class Builder {
            private boolean mAf;
            private final BandDescriptor mDescriptor;
            private boolean mEa;
            private boolean mRds;
            private boolean mStereo;
            private boolean mTa;

            public Builder(FmBandDescriptor descriptor) {
                this.mDescriptor = new BandDescriptor(descriptor.getRegion(), descriptor.getType(), descriptor.getLowerLimit(), descriptor.getUpperLimit(), descriptor.getSpacing());
                this.mStereo = descriptor.isStereoSupported();
                this.mRds = descriptor.isRdsSupported();
                this.mTa = descriptor.isTaSupported();
                this.mAf = descriptor.isAfSupported();
                this.mEa = descriptor.isEaSupported();
            }

            public Builder(FmBandConfig config) {
                this.mDescriptor = new BandDescriptor(config.getRegion(), config.getType(), config.getLowerLimit(), config.getUpperLimit(), config.getSpacing());
                this.mStereo = config.getStereo();
                this.mRds = config.getRds();
                this.mTa = config.getTa();
                this.mAf = config.getAf();
                this.mEa = config.getEa();
            }

            public FmBandConfig build() {
                return new FmBandConfig(this.mDescriptor.getRegion(), this.mDescriptor.getType(), this.mDescriptor.getLowerLimit(), this.mDescriptor.getUpperLimit(), this.mDescriptor.getSpacing(), this.mStereo, this.mRds, this.mTa, this.mAf, this.mEa);
            }

            public Builder setStereo(boolean state) {
                this.mStereo = state;
                return this;
            }

            public Builder setRds(boolean state) {
                this.mRds = state;
                return this;
            }

            public Builder setTa(boolean state) {
                this.mTa = state;
                return this;
            }

            public Builder setAf(boolean state) {
                this.mAf = state;
                return this;
            }

            public Builder setEa(boolean state) {
                this.mEa = state;
                return this;
            }
        }

        FmBandConfig(FmBandDescriptor descriptor) {
            super((BandDescriptor) descriptor);
            this.mStereo = descriptor.isStereoSupported();
            this.mRds = descriptor.isRdsSupported();
            this.mTa = descriptor.isTaSupported();
            this.mAf = descriptor.isAfSupported();
            this.mEa = descriptor.isEaSupported();
        }

        FmBandConfig(int region, int type, int lowerLimit, int upperLimit, int spacing, boolean stereo, boolean rds, boolean ta, boolean af, boolean ea) {
            super(region, type, lowerLimit, upperLimit, spacing);
            this.mStereo = stereo;
            this.mRds = rds;
            this.mTa = ta;
            this.mAf = af;
            this.mEa = ea;
        }

        public boolean getStereo() {
            return this.mStereo;
        }

        public boolean getRds() {
            return this.mRds;
        }

        public boolean getTa() {
            return this.mTa;
        }

        public boolean getAf() {
            return this.mAf;
        }

        public boolean getEa() {
            return this.mEa;
        }

        private FmBandConfig(Parcel in) {
            boolean z;
            boolean z2 = true;
            super(in);
            this.mStereo = in.readByte() == (byte) 1;
            if (in.readByte() == (byte) 1) {
                z = true;
            } else {
                z = false;
            }
            this.mRds = z;
            if (in.readByte() == (byte) 1) {
                z = true;
            } else {
                z = false;
            }
            this.mTa = z;
            if (in.readByte() == (byte) 1) {
                z = true;
            } else {
                z = false;
            }
            this.mAf = z;
            if (in.readByte() != (byte) 1) {
                z2 = false;
            }
            this.mEa = z2;
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i;
            int i2 = 1;
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (this.mStereo ? 1 : 0));
            if (this.mRds) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeByte((byte) i);
            if (this.mTa) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeByte((byte) i);
            if (this.mAf) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeByte((byte) i);
            if (!this.mEa) {
                i2 = 0;
            }
            dest.writeByte((byte) i2);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "FmBandConfig [" + super.toString() + ", mStereo=" + this.mStereo + ", mRds=" + this.mRds + ", mTa=" + this.mTa + ", mAf=" + this.mAf + ", mEa =" + this.mEa + "]";
        }

        public int hashCode() {
            int i;
            int i2 = 1;
            int hashCode = ((super.hashCode() * 31) + (this.mStereo ? 1 : 0)) * 31;
            if (this.mRds) {
                i = 1;
            } else {
                i = 0;
            }
            hashCode = (hashCode + i) * 31;
            if (this.mTa) {
                i = 1;
            } else {
                i = 0;
            }
            hashCode = (hashCode + i) * 31;
            if (this.mAf) {
                i = 1;
            } else {
                i = 0;
            }
            i = (hashCode + i) * 31;
            if (!this.mEa) {
                i2 = 0;
            }
            return i + i2;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof FmBandConfig)) {
                return false;
            }
            FmBandConfig other = (FmBandConfig) obj;
            return this.mStereo == other.mStereo && this.mRds == other.mRds && this.mTa == other.mTa && this.mAf == other.mAf && this.mEa == other.mEa;
        }
    }

    public static class FmBandDescriptor extends BandDescriptor {
        public static final Creator<FmBandDescriptor> CREATOR = new Creator<FmBandDescriptor>() {
            public FmBandDescriptor createFromParcel(Parcel in) {
                return new FmBandDescriptor(in);
            }

            public FmBandDescriptor[] newArray(int size) {
                return new FmBandDescriptor[size];
            }
        };
        private final boolean mAf;
        private final boolean mEa;
        private final boolean mRds;
        private final boolean mStereo;
        private final boolean mTa;

        FmBandDescriptor(int region, int type, int lowerLimit, int upperLimit, int spacing, boolean stereo, boolean rds, boolean ta, boolean af, boolean ea) {
            super(region, type, lowerLimit, upperLimit, spacing);
            this.mStereo = stereo;
            this.mRds = rds;
            this.mTa = ta;
            this.mAf = af;
            this.mEa = ea;
        }

        public boolean isStereoSupported() {
            return this.mStereo;
        }

        public boolean isRdsSupported() {
            return this.mRds;
        }

        public boolean isTaSupported() {
            return this.mTa;
        }

        public boolean isAfSupported() {
            return this.mAf;
        }

        public boolean isEaSupported() {
            return this.mEa;
        }

        private FmBandDescriptor(Parcel in) {
            boolean z;
            boolean z2 = true;
            super(in);
            this.mStereo = in.readByte() == (byte) 1;
            if (in.readByte() == (byte) 1) {
                z = true;
            } else {
                z = false;
            }
            this.mRds = z;
            if (in.readByte() == (byte) 1) {
                z = true;
            } else {
                z = false;
            }
            this.mTa = z;
            if (in.readByte() == (byte) 1) {
                z = true;
            } else {
                z = false;
            }
            this.mAf = z;
            if (in.readByte() != (byte) 1) {
                z2 = false;
            }
            this.mEa = z2;
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i;
            int i2 = 1;
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (this.mStereo ? 1 : 0));
            if (this.mRds) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeByte((byte) i);
            if (this.mTa) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeByte((byte) i);
            if (this.mAf) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeByte((byte) i);
            if (!this.mEa) {
                i2 = 0;
            }
            dest.writeByte((byte) i2);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "FmBandDescriptor [ " + super.toString() + " mStereo=" + this.mStereo + ", mRds=" + this.mRds + ", mTa=" + this.mTa + ", mAf=" + this.mAf + ", mEa =" + this.mEa + "]";
        }

        public int hashCode() {
            int i;
            int i2 = 1;
            int hashCode = ((super.hashCode() * 31) + (this.mStereo ? 1 : 0)) * 31;
            if (this.mRds) {
                i = 1;
            } else {
                i = 0;
            }
            hashCode = (hashCode + i) * 31;
            if (this.mTa) {
                i = 1;
            } else {
                i = 0;
            }
            hashCode = (hashCode + i) * 31;
            if (this.mAf) {
                i = 1;
            } else {
                i = 0;
            }
            i = (hashCode + i) * 31;
            if (!this.mEa) {
                i2 = 0;
            }
            return i + i2;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof FmBandDescriptor)) {
                return false;
            }
            FmBandDescriptor other = (FmBandDescriptor) obj;
            return this.mStereo == other.isStereoSupported() && this.mRds == other.isRdsSupported() && this.mTa == other.isTaSupported() && this.mAf == other.isAfSupported() && this.mEa == other.isEaSupported();
        }
    }

    public static class ModuleProperties implements Parcelable {
        public static final Creator<ModuleProperties> CREATOR = new Creator<ModuleProperties>() {
            public ModuleProperties createFromParcel(Parcel in) {
                return new ModuleProperties(in);
            }

            public ModuleProperties[] newArray(int size) {
                return new ModuleProperties[size];
            }
        };
        private final BandDescriptor[] mBands;
        private final int mClassId;
        private final int mId;
        private final String mImplementor;
        private final boolean mIsBgScanSupported;
        private final boolean mIsCaptureSupported;
        private final int mNumAudioSources;
        private final int mNumTuners;
        private final String mProduct;
        private final String mSerial;
        private final String mServiceName;
        private final Set<Integer> mSupportedIdentifierTypes;
        private final Set<Integer> mSupportedProgramTypes;
        private final Map<String, String> mVendorInfo;
        private final String mVersion;

        ModuleProperties(int id, String serviceName, int classId, String implementor, String product, String version, String serial, int numTuners, int numAudioSources, boolean isCaptureSupported, BandDescriptor[] bands, boolean isBgScanSupported, int[] supportedProgramTypes, int[] supportedIdentifierTypes, Map<String, String> map) {
            this.mId = id;
            if (TextUtils.isEmpty(serviceName)) {
                serviceName = "default";
            }
            this.mServiceName = serviceName;
            this.mClassId = classId;
            this.mImplementor = implementor;
            this.mProduct = product;
            this.mVersion = version;
            this.mSerial = serial;
            this.mNumTuners = numTuners;
            this.mNumAudioSources = numAudioSources;
            this.mIsCaptureSupported = isCaptureSupported;
            this.mBands = bands;
            this.mIsBgScanSupported = isBgScanSupported;
            this.mSupportedProgramTypes = arrayToSet(supportedProgramTypes);
            this.mSupportedIdentifierTypes = arrayToSet(supportedIdentifierTypes);
            if (map == null) {
                map = new HashMap();
            }
            this.mVendorInfo = map;
        }

        private static Set<Integer> arrayToSet(int[] arr) {
            return (Set) Arrays.stream(arr).boxed().collect(Collectors.toSet());
        }

        private static int[] setToArray(Set<Integer> set) {
            return set.stream().mapToInt(-$Lambda$NfISO-O3QdEm5B1InYI8yDsxQBY.$INST$0).toArray();
        }

        public int getId() {
            return this.mId;
        }

        public String getServiceName() {
            return this.mServiceName;
        }

        public int getClassId() {
            return this.mClassId;
        }

        public String getImplementor() {
            return this.mImplementor;
        }

        public String getProduct() {
            return this.mProduct;
        }

        public String getVersion() {
            return this.mVersion;
        }

        public String getSerial() {
            return this.mSerial;
        }

        public int getNumTuners() {
            return this.mNumTuners;
        }

        public int getNumAudioSources() {
            return this.mNumAudioSources;
        }

        public boolean isCaptureSupported() {
            return this.mIsCaptureSupported;
        }

        public boolean isBackgroundScanningSupported() {
            return this.mIsBgScanSupported;
        }

        public boolean isProgramTypeSupported(int type) {
            return this.mSupportedProgramTypes.contains(Integer.valueOf(type));
        }

        public boolean isProgramIdentifierSupported(int type) {
            return this.mSupportedIdentifierTypes.contains(Integer.valueOf(type));
        }

        public Map<String, String> getVendorInfo() {
            return this.mVendorInfo;
        }

        public BandDescriptor[] getBands() {
            return this.mBands;
        }

        private ModuleProperties(Parcel in) {
            boolean z;
            boolean z2 = true;
            this.mId = in.readInt();
            String serviceName = in.readString();
            if (TextUtils.isEmpty(serviceName)) {
                serviceName = "default";
            }
            this.mServiceName = serviceName;
            this.mClassId = in.readInt();
            this.mImplementor = in.readString();
            this.mProduct = in.readString();
            this.mVersion = in.readString();
            this.mSerial = in.readString();
            this.mNumTuners = in.readInt();
            this.mNumAudioSources = in.readInt();
            if (in.readInt() == 1) {
                z = true;
            } else {
                z = false;
            }
            this.mIsCaptureSupported = z;
            Parcelable[] tmp = in.readParcelableArray(BandDescriptor.class.getClassLoader());
            this.mBands = new BandDescriptor[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                this.mBands[i] = (BandDescriptor) tmp[i];
            }
            if (in.readInt() != 1) {
                z2 = false;
            }
            this.mIsBgScanSupported = z2;
            this.mSupportedProgramTypes = arrayToSet(in.createIntArray());
            this.mSupportedIdentifierTypes = arrayToSet(in.createIntArray());
            this.mVendorInfo = RadioManager.readStringMap(in);
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i = 1;
            dest.writeInt(this.mId);
            dest.writeString(this.mServiceName);
            dest.writeInt(this.mClassId);
            dest.writeString(this.mImplementor);
            dest.writeString(this.mProduct);
            dest.writeString(this.mVersion);
            dest.writeString(this.mSerial);
            dest.writeInt(this.mNumTuners);
            dest.writeInt(this.mNumAudioSources);
            dest.writeInt(this.mIsCaptureSupported ? 1 : 0);
            dest.writeParcelableArray(this.mBands, flags);
            if (!this.mIsBgScanSupported) {
                i = 0;
            }
            dest.writeInt(i);
            dest.writeIntArray(setToArray(this.mSupportedProgramTypes));
            dest.writeIntArray(setToArray(this.mSupportedIdentifierTypes));
            RadioManager.writeStringMap(dest, this.mVendorInfo);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "ModuleProperties [mId=" + this.mId + ", mServiceName=" + this.mServiceName + ", mClassId=" + this.mClassId + ", mImplementor=" + this.mImplementor + ", mProduct=" + this.mProduct + ", mVersion=" + this.mVersion + ", mSerial=" + this.mSerial + ", mNumTuners=" + this.mNumTuners + ", mNumAudioSources=" + this.mNumAudioSources + ", mIsCaptureSupported=" + this.mIsCaptureSupported + ", mIsBgScanSupported=" + this.mIsBgScanSupported + ", mBands=" + Arrays.toString(this.mBands) + "]";
        }

        public int hashCode() {
            int i;
            int i2 = 1;
            int hashCode = (((((((((((((((((this.mId + 31) * 31) + this.mServiceName.hashCode()) * 31) + this.mClassId) * 31) + (this.mImplementor == null ? 0 : this.mImplementor.hashCode())) * 31) + (this.mProduct == null ? 0 : this.mProduct.hashCode())) * 31) + (this.mVersion == null ? 0 : this.mVersion.hashCode())) * 31) + (this.mSerial == null ? 0 : this.mSerial.hashCode())) * 31) + this.mNumTuners) * 31) + this.mNumAudioSources) * 31;
            if (this.mIsCaptureSupported) {
                i = 1;
            } else {
                i = 0;
            }
            i = (((hashCode + i) * 31) + Arrays.hashCode(this.mBands)) * 31;
            if (!this.mIsBgScanSupported) {
                i2 = 0;
            }
            return ((i + i2) * 31) + this.mVendorInfo.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ModuleProperties)) {
                return false;
            }
            ModuleProperties other = (ModuleProperties) obj;
            if (this.mId != other.getId() || !TextUtils.equals(this.mServiceName, other.mServiceName) || this.mClassId != other.getClassId()) {
                return false;
            }
            if (this.mImplementor == null) {
                if (other.getImplementor() != null) {
                    return false;
                }
            } else if (!this.mImplementor.equals(other.getImplementor())) {
                return false;
            }
            if (this.mProduct == null) {
                if (other.getProduct() != null) {
                    return false;
                }
            } else if (!this.mProduct.equals(other.getProduct())) {
                return false;
            }
            if (this.mVersion == null) {
                if (other.getVersion() != null) {
                    return false;
                }
            } else if (!this.mVersion.equals(other.getVersion())) {
                return false;
            }
            if (this.mSerial == null) {
                if (other.getSerial() != null) {
                    return false;
                }
            } else if (!this.mSerial.equals(other.getSerial())) {
                return false;
            }
            return this.mNumTuners == other.getNumTuners() && this.mNumAudioSources == other.getNumAudioSources() && this.mIsCaptureSupported == other.isCaptureSupported() && Arrays.equals(this.mBands, other.getBands()) && this.mIsBgScanSupported == other.isBackgroundScanningSupported() && this.mVendorInfo.equals(other.mVendorInfo);
        }
    }

    public static class ProgramInfo implements Parcelable {
        public static final Creator<ProgramInfo> CREATOR = new Creator<ProgramInfo>() {
            public ProgramInfo createFromParcel(Parcel in) {
                return new ProgramInfo(in);
            }

            public ProgramInfo[] newArray(int size) {
                return new ProgramInfo[size];
            }
        };
        private static final int FLAG_LIVE = 1;
        private static final int FLAG_MUTED = 2;
        private static final int FLAG_TRAFFIC_ANNOUNCEMENT = 8;
        private static final int FLAG_TRAFFIC_PROGRAM = 4;
        private final boolean mDigital;
        private final int mFlags;
        private final RadioMetadata mMetadata;
        private final ProgramSelector mSelector;
        private final int mSignalStrength;
        private final boolean mStereo;
        private final boolean mTuned;
        private final Map<String, String> mVendorInfo;

        ProgramInfo(ProgramSelector selector, boolean tuned, boolean stereo, boolean digital, int signalStrength, RadioMetadata metadata, int flags, Map<String, String> map) {
            Map hashMap;
            this.mSelector = selector;
            this.mTuned = tuned;
            this.mStereo = stereo;
            this.mDigital = digital;
            this.mFlags = flags;
            this.mSignalStrength = signalStrength;
            this.mMetadata = metadata;
            if (map == null) {
                hashMap = new HashMap();
            }
            this.mVendorInfo = hashMap;
        }

        public ProgramSelector getSelector() {
            return this.mSelector;
        }

        @Deprecated
        public int getChannel() {
            try {
                return (int) this.mSelector.getFirstId(1);
            } catch (IllegalArgumentException e) {
                Log.w(RadioManager.TAG, "Not an AM/FM program");
                return 0;
            }
        }

        @Deprecated
        public int getSubChannel() {
            try {
                return ((int) this.mSelector.getFirstId(4)) + 1;
            } catch (IllegalArgumentException e) {
                return 0;
            }
        }

        public boolean isTuned() {
            return this.mTuned;
        }

        public boolean isStereo() {
            return this.mStereo;
        }

        public boolean isDigital() {
            return this.mDigital;
        }

        public boolean isLive() {
            return (this.mFlags & 1) != 0;
        }

        public boolean isMuted() {
            return (this.mFlags & 2) != 0;
        }

        public boolean isTrafficProgram() {
            return (this.mFlags & 4) != 0;
        }

        public boolean isTrafficAnnouncementActive() {
            return (this.mFlags & 8) != 0;
        }

        public int getSignalStrength() {
            return this.mSignalStrength;
        }

        public RadioMetadata getMetadata() {
            return this.mMetadata;
        }

        public Map<String, String> getVendorInfo() {
            return this.mVendorInfo;
        }

        private ProgramInfo(Parcel in) {
            boolean z;
            boolean z2 = false;
            this.mSelector = (ProgramSelector) in.readParcelable(null);
            this.mTuned = in.readByte() == (byte) 1;
            if (in.readByte() == (byte) 1) {
                z = true;
            } else {
                z = false;
            }
            this.mStereo = z;
            if (in.readByte() == (byte) 1) {
                z2 = true;
            }
            this.mDigital = z2;
            this.mSignalStrength = in.readInt();
            if (in.readByte() == (byte) 1) {
                this.mMetadata = (RadioMetadata) RadioMetadata.CREATOR.createFromParcel(in);
            } else {
                this.mMetadata = null;
            }
            this.mFlags = in.readInt();
            this.mVendorInfo = RadioManager.readStringMap(in);
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i;
            dest.writeParcelable(this.mSelector, 0);
            dest.writeByte((byte) (this.mTuned ? 1 : 0));
            if (this.mStereo) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeByte((byte) i);
            if (this.mDigital) {
                i = 1;
            } else {
                i = 0;
            }
            dest.writeByte((byte) i);
            dest.writeInt(this.mSignalStrength);
            if (this.mMetadata == null) {
                dest.writeByte((byte) 0);
            } else {
                dest.writeByte((byte) 1);
                this.mMetadata.writeToParcel(dest, flags);
            }
            dest.writeInt(this.mFlags);
            RadioManager.writeStringMap(dest, this.mVendorInfo);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "ProgramInfo [mSelector=" + this.mSelector + ", mTuned=" + this.mTuned + ", mStereo=" + this.mStereo + ", mDigital=" + this.mDigital + ", mFlags=" + this.mFlags + ", mSignalStrength=" + this.mSignalStrength + (this.mMetadata == null ? "" : ", mMetadata=" + this.mMetadata.toString()) + "]";
        }

        public int hashCode() {
            int i;
            int i2 = 1;
            int i3 = 0;
            int hashCode = (((this.mSelector.hashCode() + 31) * 31) + (this.mTuned ? 1 : 0)) * 31;
            if (this.mStereo) {
                i = 1;
            } else {
                i = 0;
            }
            i = (hashCode + i) * 31;
            if (!this.mDigital) {
                i2 = 0;
            }
            i = (((((i + i2) * 31) + this.mFlags) * 31) + this.mSignalStrength) * 31;
            if (this.mMetadata != null) {
                i3 = this.mMetadata.hashCode();
            }
            return ((i + i3) * 31) + this.mVendorInfo.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ProgramInfo)) {
                return false;
            }
            ProgramInfo other = (ProgramInfo) obj;
            if (!this.mSelector.equals(other.getSelector()) || this.mTuned != other.isTuned() || this.mStereo != other.isStereo() || this.mDigital != other.isDigital() || this.mFlags != other.mFlags || this.mSignalStrength != other.getSignalStrength()) {
                return false;
            }
            if (this.mMetadata == null) {
                if (other.getMetadata() != null) {
                    return false;
                }
            } else if (!this.mMetadata.equals(other.getMetadata())) {
                return false;
            }
            return this.mVendorInfo.equals(other.mVendorInfo);
        }
    }

    private native int nativeListModules(List<ModuleProperties> list);

    private static void writeStringMap(Parcel dest, Map<String, String> map) {
        dest.writeInt(map.size());
        for (Entry<String, String> entry : map.entrySet()) {
            dest.writeString((String) entry.getKey());
            dest.writeString((String) entry.getValue());
        }
    }

    private static Map<String, String> readStringMap(Parcel in) {
        int size = in.readInt();
        Map<String, String> map = new HashMap();
        int size2 = size;
        while (true) {
            size = size2 - 1;
            if (size2 <= 0) {
                return map;
            }
            map.put(in.readString(), in.readString());
            size2 = size;
        }
    }

    public int listModules(List<ModuleProperties> modules) {
        if (modules == null) {
            Log.e(TAG, "the output list must not be empty");
            return -22;
        }
        Log.d(TAG, "Listing available tuners...");
        try {
            List<ModuleProperties> returnedList = this.mService.listModules();
            if (returnedList == null) {
                Log.e(TAG, "Returned list was a null");
                return Integer.MIN_VALUE;
            }
            modules.addAll(returnedList);
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed listing available tuners", e);
            return -32;
        }
    }

    public RadioTuner openTuner(int moduleId, BandConfig config, boolean withAudio, Callback callback, Handler handler) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be empty");
        }
        Log.d(TAG, "Opening tuner " + moduleId + "...");
        try {
            ITuner tuner = this.mService.openTuner(moduleId, config, withAudio, new TunerCallbackAdapter(callback, handler));
            if (tuner == null) {
                Log.e(TAG, "Failed to open tuner");
                return null;
            }
            return new TunerAdapter(tuner, config != null ? config.getType() : -1);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to open tuner", e);
            return null;
        }
    }

    public RadioManager(Context context) throws ServiceNotFoundException {
        this.mContext = context;
    }
}
