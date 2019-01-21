package android.hardware.radio;

import android.annotation.SystemApi;
import android.content.Context;
import android.hardware.radio.Announcement.OnListUpdatedListener;
import android.hardware.radio.IAnnouncementListener.Stub;
import android.hardware.radio.ProgramSelector.Identifier;
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
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@SystemApi
public class RadioManager {
    public static final int BAND_AM = 0;
    public static final int BAND_AM_HD = 3;
    public static final int BAND_FM = 1;
    public static final int BAND_FM_HD = 2;
    public static final int BAND_INVALID = -1;
    public static final int CLASS_AM_FM = 0;
    public static final int CLASS_DT = 2;
    public static final int CLASS_SAT = 1;
    public static final int CONFIG_DAB_DAB_LINKING = 6;
    public static final int CONFIG_DAB_DAB_SOFT_LINKING = 8;
    public static final int CONFIG_DAB_FM_LINKING = 7;
    public static final int CONFIG_DAB_FM_SOFT_LINKING = 9;
    public static final int CONFIG_FORCE_ANALOG = 2;
    public static final int CONFIG_FORCE_DIGITAL = 3;
    public static final int CONFIG_FORCE_MONO = 1;
    public static final int CONFIG_RDS_AF = 4;
    public static final int CONFIG_RDS_REG = 5;
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
    private final Map<OnListUpdatedListener, ICloseHandle> mAnnouncementListeners = new HashMap();
    private final Context mContext;
    private final IRadioService mService;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Band {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ConfigFlag {
    }

    public static class BandConfig implements Parcelable {
        public static final Creator<BandConfig> CREATOR = new Creator<BandConfig>() {
            public BandConfig createFromParcel(Parcel in) {
                int type = BandDescriptor.lookupTypeFromParcel(in);
                switch (type) {
                    case 0:
                    case 3:
                        return new AmBandConfig(in, null);
                    case 1:
                    case 2:
                        return new FmBandConfig(in, null);
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported band: ");
                        stringBuilder.append(type);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            }

            public BandConfig[] newArray(int size) {
                return new BandConfig[size];
            }
        };
        final BandDescriptor mDescriptor;

        /* synthetic */ BandConfig(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        BandConfig(BandDescriptor descriptor) {
            this.mDescriptor = (BandDescriptor) Objects.requireNonNull(descriptor);
        }

        BandConfig(int region, int type, int lowerLimit, int upperLimit, int spacing) {
            this.mDescriptor = new BandDescriptor(region, type, lowerLimit, upperLimit, spacing);
        }

        private BandConfig(Parcel in) {
            this.mDescriptor = new BandDescriptor(in, null);
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BandConfig [ ");
            stringBuilder.append(this.mDescriptor.toString());
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public int hashCode() {
            return (31 * 1) + this.mDescriptor.hashCode();
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
            if (this.mDescriptor == null || this.mDescriptor.equals(otherDesc)) {
                return true;
            }
            return false;
        }
    }

    public static class BandDescriptor implements Parcelable {
        public static final Creator<BandDescriptor> CREATOR = new Creator<BandDescriptor>() {
            public BandDescriptor createFromParcel(Parcel in) {
                int type = BandDescriptor.lookupTypeFromParcel(in);
                switch (type) {
                    case 0:
                    case 3:
                        return new AmBandDescriptor(in, null);
                    case 1:
                    case 2:
                        return new FmBandDescriptor(in, null);
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported band: ");
                        stringBuilder.append(type);
                        throw new IllegalArgumentException(stringBuilder.toString());
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

        /* synthetic */ BandDescriptor(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        BandDescriptor(int region, int type, int lowerLimit, int upperLimit, int spacing) {
            if (type == 0 || type == 1 || type == 2 || type == 3) {
                this.mRegion = region;
                this.mType = type;
                this.mLowerLimit = lowerLimit;
                this.mUpperLimit = upperLimit;
                this.mSpacing = spacing;
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported band: ");
            stringBuilder.append(type);
            throw new IllegalArgumentException(stringBuilder.toString());
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BandDescriptor [mRegion=");
            stringBuilder.append(this.mRegion);
            stringBuilder.append(", mType=");
            stringBuilder.append(this.mType);
            stringBuilder.append(", mLowerLimit=");
            stringBuilder.append(this.mLowerLimit);
            stringBuilder.append(", mUpperLimit=");
            stringBuilder.append(this.mUpperLimit);
            stringBuilder.append(", mSpacing=");
            stringBuilder.append(this.mSpacing);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * ((31 * ((31 * 1) + this.mRegion)) + this.mType)) + this.mLowerLimit)) + this.mUpperLimit)) + this.mSpacing;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BandDescriptor)) {
                return false;
            }
            BandDescriptor other = (BandDescriptor) obj;
            if (this.mRegion == other.getRegion() && this.mType == other.getType() && this.mLowerLimit == other.getLowerLimit() && this.mUpperLimit == other.getUpperLimit() && this.mSpacing == other.getSpacing()) {
                return true;
            }
            return false;
        }
    }

    public static class ModuleProperties implements Parcelable {
        public static final Creator<ModuleProperties> CREATOR = new Creator<ModuleProperties>() {
            public ModuleProperties createFromParcel(Parcel in) {
                return new ModuleProperties(in, null);
            }

            public ModuleProperties[] newArray(int size) {
                return new ModuleProperties[size];
            }
        };
        private final BandDescriptor[] mBands;
        private final int mClassId;
        private final Map<String, Integer> mDabFrequencyTable;
        private final int mId;
        private final String mImplementor;
        private final boolean mIsBgScanSupported;
        private final boolean mIsCaptureSupported;
        private final boolean mIsInitializationRequired;
        private final int mNumAudioSources;
        private final int mNumTuners;
        private final String mProduct;
        private final String mSerial;
        private final String mServiceName;
        private final Set<Integer> mSupportedIdentifierTypes;
        private final Set<Integer> mSupportedProgramTypes;
        private final Map<String, String> mVendorInfo;
        private final String mVersion;

        /* synthetic */ ModuleProperties(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        public ModuleProperties(int id, String serviceName, int classId, String implementor, String product, String version, String serial, int numTuners, int numAudioSources, boolean isInitializationRequired, boolean isCaptureSupported, BandDescriptor[] bands, boolean isBgScanSupported, int[] supportedProgramTypes, int[] supportedIdentifierTypes, Map<String, Integer> dabFrequencyTable, Map<String, String> vendorInfo) {
            Map<String, Integer> map = dabFrequencyTable;
            this.mId = id;
            this.mServiceName = TextUtils.isEmpty(serviceName) ? "default" : serviceName;
            this.mClassId = classId;
            this.mImplementor = implementor;
            this.mProduct = product;
            this.mVersion = version;
            this.mSerial = serial;
            this.mNumTuners = numTuners;
            this.mNumAudioSources = numAudioSources;
            this.mIsInitializationRequired = isInitializationRequired;
            this.mIsCaptureSupported = isCaptureSupported;
            this.mBands = bands;
            this.mIsBgScanSupported = isBgScanSupported;
            this.mSupportedProgramTypes = arrayToSet(supportedProgramTypes);
            this.mSupportedIdentifierTypes = arrayToSet(supportedIdentifierTypes);
            if (map != null) {
                for (Entry<String, Integer> entry : dabFrequencyTable.entrySet()) {
                    Objects.requireNonNull((String) entry.getKey());
                    Objects.requireNonNull((Integer) entry.getValue());
                    int i = id;
                }
            }
            this.mDabFrequencyTable = map;
            this.mVendorInfo = vendorInfo == null ? new HashMap() : vendorInfo;
        }

        private static Set<Integer> arrayToSet(int[] arr) {
            return (Set) Arrays.stream(arr).boxed().collect(Collectors.toSet());
        }

        private static int[] setToArray(Set<Integer> set) {
            return set.stream().mapToInt(-$$Lambda$RadioManager$ModuleProperties$UV1wDVoVlbcxpr8zevj_aMFtUGw.INSTANCE).toArray();
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

        public boolean isInitializationRequired() {
            return this.mIsInitializationRequired;
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

        public Map<String, Integer> getDabFrequencyTable() {
            return this.mDabFrequencyTable;
        }

        public Map<String, String> getVendorInfo() {
            return this.mVendorInfo;
        }

        public BandDescriptor[] getBands() {
            return this.mBands;
        }

        private ModuleProperties(Parcel in) {
            this.mId = in.readInt();
            String serviceName = in.readString();
            this.mServiceName = TextUtils.isEmpty(serviceName) ? "default" : serviceName;
            this.mClassId = in.readInt();
            this.mImplementor = in.readString();
            this.mProduct = in.readString();
            this.mVersion = in.readString();
            this.mSerial = in.readString();
            this.mNumTuners = in.readInt();
            this.mNumAudioSources = in.readInt();
            boolean z = false;
            this.mIsInitializationRequired = in.readInt() == 1;
            this.mIsCaptureSupported = in.readInt() == 1;
            Parcelable[] tmp = in.readParcelableArray(BandDescriptor.class.getClassLoader());
            this.mBands = new BandDescriptor[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                this.mBands[i] = (BandDescriptor) tmp[i];
            }
            if (in.readInt() == 1) {
                z = true;
            }
            this.mIsBgScanSupported = z;
            this.mSupportedProgramTypes = arrayToSet(in.createIntArray());
            this.mSupportedIdentifierTypes = arrayToSet(in.createIntArray());
            this.mDabFrequencyTable = Utils.readStringIntMap(in);
            this.mVendorInfo = Utils.readStringMap(in);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mId);
            dest.writeString(this.mServiceName);
            dest.writeInt(this.mClassId);
            dest.writeString(this.mImplementor);
            dest.writeString(this.mProduct);
            dest.writeString(this.mVersion);
            dest.writeString(this.mSerial);
            dest.writeInt(this.mNumTuners);
            dest.writeInt(this.mNumAudioSources);
            dest.writeInt(this.mIsInitializationRequired);
            dest.writeInt(this.mIsCaptureSupported);
            dest.writeParcelableArray(this.mBands, flags);
            dest.writeInt(this.mIsBgScanSupported);
            dest.writeIntArray(setToArray(this.mSupportedProgramTypes));
            dest.writeIntArray(setToArray(this.mSupportedIdentifierTypes));
            Utils.writeStringIntMap(dest, this.mDabFrequencyTable);
            Utils.writeStringMap(dest, this.mVendorInfo);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ModuleProperties [mId=");
            stringBuilder.append(this.mId);
            stringBuilder.append(", mServiceName=");
            stringBuilder.append(this.mServiceName);
            stringBuilder.append(", mClassId=");
            stringBuilder.append(this.mClassId);
            stringBuilder.append(", mImplementor=");
            stringBuilder.append(this.mImplementor);
            stringBuilder.append(", mProduct=");
            stringBuilder.append(this.mProduct);
            stringBuilder.append(", mVersion=");
            stringBuilder.append(this.mVersion);
            stringBuilder.append(", mSerial=");
            stringBuilder.append(this.mSerial);
            stringBuilder.append(", mNumTuners=");
            stringBuilder.append(this.mNumTuners);
            stringBuilder.append(", mNumAudioSources=");
            stringBuilder.append(this.mNumAudioSources);
            stringBuilder.append(", mIsInitializationRequired=");
            stringBuilder.append(this.mIsInitializationRequired);
            stringBuilder.append(", mIsCaptureSupported=");
            stringBuilder.append(this.mIsCaptureSupported);
            stringBuilder.append(", mIsBgScanSupported=");
            stringBuilder.append(this.mIsBgScanSupported);
            stringBuilder.append(", mBands=");
            stringBuilder.append(Arrays.toString(this.mBands));
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Integer.valueOf(this.mId), this.mServiceName, Integer.valueOf(this.mClassId), this.mImplementor, this.mProduct, this.mVersion, this.mSerial, Integer.valueOf(this.mNumTuners), Integer.valueOf(this.mNumAudioSources), Boolean.valueOf(this.mIsInitializationRequired), Boolean.valueOf(this.mIsCaptureSupported), this.mBands, Boolean.valueOf(this.mIsBgScanSupported), this.mDabFrequencyTable, this.mVendorInfo});
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ModuleProperties)) {
                return false;
            }
            ModuleProperties other = (ModuleProperties) obj;
            if (this.mId == other.getId() && TextUtils.equals(this.mServiceName, other.mServiceName) && this.mClassId == other.mClassId && Objects.equals(this.mImplementor, other.mImplementor) && Objects.equals(this.mProduct, other.mProduct) && Objects.equals(this.mVersion, other.mVersion) && Objects.equals(this.mSerial, other.mSerial) && this.mNumTuners == other.mNumTuners && this.mNumAudioSources == other.mNumAudioSources && this.mIsInitializationRequired == other.mIsInitializationRequired && this.mIsCaptureSupported == other.mIsCaptureSupported && Objects.equals(this.mBands, other.mBands) && this.mIsBgScanSupported == other.mIsBgScanSupported && Objects.equals(this.mDabFrequencyTable, other.mDabFrequencyTable) && Objects.equals(this.mVendorInfo, other.mVendorInfo)) {
                return true;
            }
            return false;
        }
    }

    public static class ProgramInfo implements Parcelable {
        public static final Creator<ProgramInfo> CREATOR = new Creator<ProgramInfo>() {
            public ProgramInfo createFromParcel(Parcel in) {
                return new ProgramInfo(in, null);
            }

            public ProgramInfo[] newArray(int size) {
                return new ProgramInfo[size];
            }
        };
        private static final int FLAG_LIVE = 1;
        private static final int FLAG_MUTED = 2;
        private static final int FLAG_STEREO = 32;
        private static final int FLAG_TRAFFIC_ANNOUNCEMENT = 8;
        private static final int FLAG_TRAFFIC_PROGRAM = 4;
        private static final int FLAG_TUNED = 16;
        private final int mInfoFlags;
        private final Identifier mLogicallyTunedTo;
        private final RadioMetadata mMetadata;
        private final Identifier mPhysicallyTunedTo;
        private final Collection<Identifier> mRelatedContent;
        private final ProgramSelector mSelector;
        private final int mSignalQuality;
        private final Map<String, String> mVendorInfo;

        /* synthetic */ ProgramInfo(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        public ProgramInfo(ProgramSelector selector, Identifier logicallyTunedTo, Identifier physicallyTunedTo, Collection<Identifier> relatedContent, int infoFlags, int signalQuality, RadioMetadata metadata, Map<String, String> vendorInfo) {
            this.mSelector = (ProgramSelector) Objects.requireNonNull(selector);
            this.mLogicallyTunedTo = logicallyTunedTo;
            this.mPhysicallyTunedTo = physicallyTunedTo;
            if (relatedContent == null) {
                this.mRelatedContent = Collections.emptyList();
            } else {
                Preconditions.checkCollectionElementsNotNull(relatedContent, "relatedContent");
                this.mRelatedContent = relatedContent;
            }
            this.mInfoFlags = infoFlags;
            this.mSignalQuality = signalQuality;
            this.mMetadata = metadata;
            this.mVendorInfo = vendorInfo == null ? new HashMap() : vendorInfo;
        }

        public ProgramSelector getSelector() {
            return this.mSelector;
        }

        public Identifier getLogicallyTunedTo() {
            return this.mLogicallyTunedTo;
        }

        public Identifier getPhysicallyTunedTo() {
            return this.mPhysicallyTunedTo;
        }

        public Collection<Identifier> getRelatedContent() {
            return this.mRelatedContent;
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
            return (this.mInfoFlags & 16) != 0;
        }

        public boolean isStereo() {
            return (this.mInfoFlags & 32) != 0;
        }

        @Deprecated
        public boolean isDigital() {
            Identifier id = this.mLogicallyTunedTo;
            if (id == null) {
                id = this.mSelector.getPrimaryId();
            }
            int type = id.getType();
            return (type == 1 || type == 2) ? false : true;
        }

        public boolean isLive() {
            return (this.mInfoFlags & 1) != 0;
        }

        public boolean isMuted() {
            return (this.mInfoFlags & 2) != 0;
        }

        public boolean isTrafficProgram() {
            return (this.mInfoFlags & 4) != 0;
        }

        public boolean isTrafficAnnouncementActive() {
            return (this.mInfoFlags & 8) != 0;
        }

        public int getSignalStrength() {
            return this.mSignalQuality;
        }

        public RadioMetadata getMetadata() {
            return this.mMetadata;
        }

        public Map<String, String> getVendorInfo() {
            return this.mVendorInfo;
        }

        private ProgramInfo(Parcel in) {
            this.mSelector = (ProgramSelector) Objects.requireNonNull((ProgramSelector) in.readTypedObject(ProgramSelector.CREATOR));
            this.mLogicallyTunedTo = (Identifier) in.readTypedObject(Identifier.CREATOR);
            this.mPhysicallyTunedTo = (Identifier) in.readTypedObject(Identifier.CREATOR);
            this.mRelatedContent = in.createTypedArrayList(Identifier.CREATOR);
            this.mInfoFlags = in.readInt();
            this.mSignalQuality = in.readInt();
            this.mMetadata = (RadioMetadata) in.readTypedObject(RadioMetadata.CREATOR);
            this.mVendorInfo = Utils.readStringMap(in);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedObject(this.mSelector, flags);
            dest.writeTypedObject(this.mLogicallyTunedTo, flags);
            dest.writeTypedObject(this.mPhysicallyTunedTo, flags);
            Utils.writeTypedCollection(dest, this.mRelatedContent);
            dest.writeInt(this.mInfoFlags);
            dest.writeInt(this.mSignalQuality);
            dest.writeTypedObject(this.mMetadata, flags);
            Utils.writeStringMap(dest, this.mVendorInfo);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ProgramInfo [selector=");
            stringBuilder.append(this.mSelector);
            stringBuilder.append(", logicallyTunedTo=");
            stringBuilder.append(Objects.toString(this.mLogicallyTunedTo));
            stringBuilder.append(", physicallyTunedTo=");
            stringBuilder.append(Objects.toString(this.mPhysicallyTunedTo));
            stringBuilder.append(", relatedContent=");
            stringBuilder.append(this.mRelatedContent.size());
            stringBuilder.append(", infoFlags=");
            stringBuilder.append(this.mInfoFlags);
            stringBuilder.append(", mSignalQuality=");
            stringBuilder.append(this.mSignalQuality);
            stringBuilder.append(", mMetadata=");
            stringBuilder.append(Objects.toString(this.mMetadata));
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.mSelector, this.mLogicallyTunedTo, this.mPhysicallyTunedTo, this.mRelatedContent, Integer.valueOf(this.mInfoFlags), Integer.valueOf(this.mSignalQuality), this.mMetadata, this.mVendorInfo});
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ProgramInfo)) {
                return false;
            }
            ProgramInfo other = (ProgramInfo) obj;
            if (Objects.equals(this.mSelector, other.mSelector) && Objects.equals(this.mLogicallyTunedTo, other.mLogicallyTunedTo) && Objects.equals(this.mPhysicallyTunedTo, other.mPhysicallyTunedTo) && Objects.equals(this.mRelatedContent, other.mRelatedContent) && this.mInfoFlags == other.mInfoFlags && this.mSignalQuality == other.mSignalQuality && Objects.equals(this.mMetadata, other.mMetadata) && Objects.equals(this.mVendorInfo, other.mVendorInfo)) {
                return true;
            }
            return false;
        }
    }

    public static class AmBandConfig extends BandConfig {
        public static final Creator<AmBandConfig> CREATOR = new Creator<AmBandConfig>() {
            public AmBandConfig createFromParcel(Parcel in) {
                return new AmBandConfig(in, null);
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

        /* synthetic */ AmBandConfig(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        public AmBandConfig(AmBandDescriptor descriptor) {
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
            super(in, null);
            boolean z = true;
            if (in.readByte() != (byte) 1) {
                z = false;
            }
            this.mStereo = z;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) this.mStereo);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AmBandConfig [");
            stringBuilder.append(super.toString());
            stringBuilder.append(", mStereo=");
            stringBuilder.append(this.mStereo);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public int hashCode() {
            return (31 * super.hashCode()) + this.mStereo;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof AmBandConfig)) {
                return false;
            }
            if (this.mStereo != ((AmBandConfig) obj).getStereo()) {
                return false;
            }
            return true;
        }
    }

    public static class AmBandDescriptor extends BandDescriptor {
        public static final Creator<AmBandDescriptor> CREATOR = new Creator<AmBandDescriptor>() {
            public AmBandDescriptor createFromParcel(Parcel in) {
                return new AmBandDescriptor(in, null);
            }

            public AmBandDescriptor[] newArray(int size) {
                return new AmBandDescriptor[size];
            }
        };
        private final boolean mStereo;

        /* synthetic */ AmBandDescriptor(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        public AmBandDescriptor(int region, int type, int lowerLimit, int upperLimit, int spacing, boolean stereo) {
            super(region, type, lowerLimit, upperLimit, spacing);
            this.mStereo = stereo;
        }

        public boolean isStereoSupported() {
            return this.mStereo;
        }

        private AmBandDescriptor(Parcel in) {
            super(in, null);
            boolean z = true;
            if (in.readByte() != (byte) 1) {
                z = false;
            }
            this.mStereo = z;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) this.mStereo);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AmBandDescriptor [ ");
            stringBuilder.append(super.toString());
            stringBuilder.append(" mStereo=");
            stringBuilder.append(this.mStereo);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public int hashCode() {
            return (31 * super.hashCode()) + this.mStereo;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof AmBandDescriptor)) {
                return false;
            }
            if (this.mStereo != ((AmBandDescriptor) obj).isStereoSupported()) {
                return false;
            }
            return true;
        }
    }

    public static class FmBandConfig extends BandConfig {
        public static final Creator<FmBandConfig> CREATOR = new Creator<FmBandConfig>() {
            public FmBandConfig createFromParcel(Parcel in) {
                return new FmBandConfig(in, null);
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

        /* synthetic */ FmBandConfig(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        public FmBandConfig(FmBandDescriptor descriptor) {
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
            super(in, null);
            boolean z = false;
            this.mStereo = in.readByte() == (byte) 1;
            this.mRds = in.readByte() == (byte) 1;
            this.mTa = in.readByte() == (byte) 1;
            this.mAf = in.readByte() == (byte) 1;
            if (in.readByte() == (byte) 1) {
                z = true;
            }
            this.mEa = z;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) this.mStereo);
            dest.writeByte((byte) this.mRds);
            dest.writeByte((byte) this.mTa);
            dest.writeByte((byte) this.mAf);
            dest.writeByte((byte) this.mEa);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FmBandConfig [");
            stringBuilder.append(super.toString());
            stringBuilder.append(", mStereo=");
            stringBuilder.append(this.mStereo);
            stringBuilder.append(", mRds=");
            stringBuilder.append(this.mRds);
            stringBuilder.append(", mTa=");
            stringBuilder.append(this.mTa);
            stringBuilder.append(", mAf=");
            stringBuilder.append(this.mAf);
            stringBuilder.append(", mEa =");
            stringBuilder.append(this.mEa);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * ((31 * ((31 * super.hashCode()) + this.mStereo)) + this.mRds)) + this.mTa)) + this.mAf)) + this.mEa;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof FmBandConfig)) {
                return false;
            }
            FmBandConfig other = (FmBandConfig) obj;
            if (this.mStereo == other.mStereo && this.mRds == other.mRds && this.mTa == other.mTa && this.mAf == other.mAf && this.mEa == other.mEa) {
                return true;
            }
            return false;
        }
    }

    public static class FmBandDescriptor extends BandDescriptor {
        public static final Creator<FmBandDescriptor> CREATOR = new Creator<FmBandDescriptor>() {
            public FmBandDescriptor createFromParcel(Parcel in) {
                return new FmBandDescriptor(in, null);
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

        /* synthetic */ FmBandDescriptor(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        public FmBandDescriptor(int region, int type, int lowerLimit, int upperLimit, int spacing, boolean stereo, boolean rds, boolean ta, boolean af, boolean ea) {
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
            super(in, null);
            boolean z = false;
            this.mStereo = in.readByte() == (byte) 1;
            this.mRds = in.readByte() == (byte) 1;
            this.mTa = in.readByte() == (byte) 1;
            this.mAf = in.readByte() == (byte) 1;
            if (in.readByte() == (byte) 1) {
                z = true;
            }
            this.mEa = z;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) this.mStereo);
            dest.writeByte((byte) this.mRds);
            dest.writeByte((byte) this.mTa);
            dest.writeByte((byte) this.mAf);
            dest.writeByte((byte) this.mEa);
        }

        public int describeContents() {
            return 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FmBandDescriptor [ ");
            stringBuilder.append(super.toString());
            stringBuilder.append(" mStereo=");
            stringBuilder.append(this.mStereo);
            stringBuilder.append(", mRds=");
            stringBuilder.append(this.mRds);
            stringBuilder.append(", mTa=");
            stringBuilder.append(this.mTa);
            stringBuilder.append(", mAf=");
            stringBuilder.append(this.mAf);
            stringBuilder.append(", mEa =");
            stringBuilder.append(this.mEa);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * ((31 * ((31 * super.hashCode()) + this.mStereo)) + this.mRds)) + this.mTa)) + this.mAf)) + this.mEa;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || !(obj instanceof FmBandDescriptor)) {
                return false;
            }
            FmBandDescriptor other = (FmBandDescriptor) obj;
            if (this.mStereo == other.isStereoSupported() && this.mRds == other.isRdsSupported() && this.mTa == other.isTaSupported() && this.mAf == other.isAfSupported() && this.mEa == other.isEaSupported()) {
                return true;
            }
            return false;
        }
    }

    private native int nativeListModules(List<ModuleProperties> list);

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
        if (callback != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Opening tuner ");
            stringBuilder.append(moduleId);
            stringBuilder.append("...");
            Log.d(str, stringBuilder.toString());
            TunerCallbackAdapter halCallback = new TunerCallbackAdapter(callback, handler);
            try {
                ITuner tuner = this.mService.openTuner(moduleId, config, withAudio, halCallback);
                if (tuner == null) {
                    Log.e(TAG, "Failed to open tuner");
                    return null;
                }
                return new TunerAdapter(tuner, halCallback, config != null ? config.getType() : -1);
            } catch (RemoteException | IllegalArgumentException ex) {
                Log.e(TAG, "Failed to open tuner", ex);
                return null;
            }
        }
        throw new IllegalArgumentException("callback must not be empty");
    }

    public void addAnnouncementListener(Set<Integer> enabledAnnouncementTypes, OnListUpdatedListener listener) {
        addAnnouncementListener(-$$Lambda$RadioManager$cfMLnpQqL72UMrjmCGbrhAOHHgg.INSTANCE, enabledAnnouncementTypes, listener);
    }

    public void addAnnouncementListener(final Executor executor, Set<Integer> enabledAnnouncementTypes, final OnListUpdatedListener listener) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(listener);
        int[] types = enabledAnnouncementTypes.stream().mapToInt(-$$Lambda$RadioManager$UV1wDVoVlbcxpr8zevj_aMFtUGw.INSTANCE).toArray();
        IAnnouncementListener listenerIface = new Stub() {
            public void onListUpdated(List<Announcement> activeAnnouncements) {
                executor.execute(new -$$Lambda$RadioManager$1$yOwq8CG0kiZcgKFclFSIrjag008(listener, activeAnnouncements));
            }
        };
        synchronized (this.mAnnouncementListeners) {
            ICloseHandle closeHandle = null;
            try {
                closeHandle = this.mService.addAnnouncementListener(types, listenerIface);
            } catch (RemoteException ex) {
                ex.rethrowFromSystemServer();
            }
            Objects.requireNonNull(closeHandle);
            ICloseHandle oldCloseHandle = (ICloseHandle) this.mAnnouncementListeners.put(listener, closeHandle);
            if (oldCloseHandle != null) {
                Utils.close(oldCloseHandle);
            }
        }
    }

    public void removeAnnouncementListener(OnListUpdatedListener listener) {
        Objects.requireNonNull(listener);
        synchronized (this.mAnnouncementListeners) {
            ICloseHandle closeHandle = (ICloseHandle) this.mAnnouncementListeners.remove(listener);
            if (closeHandle != null) {
                Utils.close(closeHandle);
            }
        }
    }

    public RadioManager(Context context) throws ServiceNotFoundException {
        this.mContext = context;
        this.mService = IRadioService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.RADIO_SERVICE));
    }
}
