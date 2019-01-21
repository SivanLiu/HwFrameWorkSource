package android.print;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.rms.iaware.AwareNRTConstant;
import android.service.notification.ZenModeConfig;
import android.telephony.PreciseDisconnectCause;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

public final class PrintAttributes implements Parcelable {
    public static final int COLOR_MODE_COLOR = 2;
    public static final int COLOR_MODE_MONOCHROME = 1;
    public static final Creator<PrintAttributes> CREATOR = new Creator<PrintAttributes>() {
        public PrintAttributes createFromParcel(Parcel parcel) {
            return new PrintAttributes(parcel, null);
        }

        public PrintAttributes[] newArray(int size) {
            return new PrintAttributes[size];
        }
    };
    public static final int DUPLEX_MODE_LONG_EDGE = 2;
    public static final int DUPLEX_MODE_NONE = 1;
    public static final int DUPLEX_MODE_SHORT_EDGE = 4;
    private static final int VALID_COLOR_MODES = 3;
    private static final int VALID_DUPLEX_MODES = 7;
    private int mColorMode;
    private int mDuplexMode;
    private MediaSize mMediaSize;
    private Margins mMinMargins;
    private Resolution mResolution;

    public static final class Builder {
        private final PrintAttributes mAttributes = new PrintAttributes();

        public Builder setMediaSize(MediaSize mediaSize) {
            this.mAttributes.setMediaSize(mediaSize);
            return this;
        }

        public Builder setResolution(Resolution resolution) {
            this.mAttributes.setResolution(resolution);
            return this;
        }

        public Builder setMinMargins(Margins margins) {
            this.mAttributes.setMinMargins(margins);
            return this;
        }

        public Builder setColorMode(int colorMode) {
            this.mAttributes.setColorMode(colorMode);
            return this;
        }

        public Builder setDuplexMode(int duplexMode) {
            this.mAttributes.setDuplexMode(duplexMode);
            return this;
        }

        public PrintAttributes build() {
            return this.mAttributes;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface ColorMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface DuplexMode {
    }

    public static final class Margins {
        public static final Margins NO_MARGINS = new Margins(0, 0, 0, 0);
        private final int mBottomMils;
        private final int mLeftMils;
        private final int mRightMils;
        private final int mTopMils;

        public Margins(int leftMils, int topMils, int rightMils, int bottomMils) {
            this.mTopMils = topMils;
            this.mLeftMils = leftMils;
            this.mRightMils = rightMils;
            this.mBottomMils = bottomMils;
        }

        public int getLeftMils() {
            return this.mLeftMils;
        }

        public int getTopMils() {
            return this.mTopMils;
        }

        public int getRightMils() {
            return this.mRightMils;
        }

        public int getBottomMils() {
            return this.mBottomMils;
        }

        void writeToParcel(Parcel parcel) {
            parcel.writeInt(this.mLeftMils);
            parcel.writeInt(this.mTopMils);
            parcel.writeInt(this.mRightMils);
            parcel.writeInt(this.mBottomMils);
        }

        static Margins createFromParcel(Parcel parcel) {
            return new Margins(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * ((31 * 1) + this.mBottomMils)) + this.mLeftMils)) + this.mRightMils)) + this.mTopMils;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Margins other = (Margins) obj;
            if (this.mBottomMils == other.mBottomMils && this.mLeftMils == other.mLeftMils && this.mRightMils == other.mRightMils && this.mTopMils == other.mTopMils) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Margins{");
            builder.append("leftMils: ");
            builder.append(this.mLeftMils);
            builder.append(", topMils: ");
            builder.append(this.mTopMils);
            builder.append(", rightMils: ");
            builder.append(this.mRightMils);
            builder.append(", bottomMils: ");
            builder.append(this.mBottomMils);
            builder.append("}");
            return builder.toString();
        }
    }

    public static final class MediaSize {
        public static final MediaSize ISO_A0 = new MediaSize("ISO_A0", ZenModeConfig.SYSTEM_AUTHORITY, 17040438, 33110, 46810);
        public static final MediaSize ISO_A1 = new MediaSize("ISO_A1", ZenModeConfig.SYSTEM_AUTHORITY, 17040439, 23390, 33110);
        public static final MediaSize ISO_A10 = new MediaSize("ISO_A10", ZenModeConfig.SYSTEM_AUTHORITY, 17040440, 1020, 1460);
        public static final MediaSize ISO_A2 = new MediaSize("ISO_A2", ZenModeConfig.SYSTEM_AUTHORITY, 17040441, 16540, 23390);
        public static final MediaSize ISO_A3 = new MediaSize("ISO_A3", ZenModeConfig.SYSTEM_AUTHORITY, 17040442, 11690, 16540);
        public static final MediaSize ISO_A4 = new MediaSize("ISO_A4", ZenModeConfig.SYSTEM_AUTHORITY, 17040443, 8270, 11690);
        public static final MediaSize ISO_A5 = new MediaSize("ISO_A5", ZenModeConfig.SYSTEM_AUTHORITY, 17040444, 5830, 8270);
        public static final MediaSize ISO_A6 = new MediaSize("ISO_A6", ZenModeConfig.SYSTEM_AUTHORITY, 17040445, 4130, 5830);
        public static final MediaSize ISO_A7 = new MediaSize("ISO_A7", ZenModeConfig.SYSTEM_AUTHORITY, 17040446, 2910, 4130);
        public static final MediaSize ISO_A8 = new MediaSize("ISO_A8", ZenModeConfig.SYSTEM_AUTHORITY, 17040447, AwareNRTConstant.FIRST_THERMAL_CTRL_EVENT_ID, 2910);
        public static final MediaSize ISO_A9 = new MediaSize("ISO_A9", ZenModeConfig.SYSTEM_AUTHORITY, 17040448, 1460, AwareNRTConstant.FIRST_THERMAL_CTRL_EVENT_ID);
        public static final MediaSize ISO_B0 = new MediaSize("ISO_B0", ZenModeConfig.SYSTEM_AUTHORITY, 17040449, 39370, 55670);
        public static final MediaSize ISO_B1 = new MediaSize("ISO_B1", ZenModeConfig.SYSTEM_AUTHORITY, 17040450, 27830, 39370);
        public static final MediaSize ISO_B10 = new MediaSize("ISO_B10", ZenModeConfig.SYSTEM_AUTHORITY, 17040451, PreciseDisconnectCause.LOCAL_HO_NOT_FEASIBLE, 1730);
        public static final MediaSize ISO_B2 = new MediaSize("ISO_B2", ZenModeConfig.SYSTEM_AUTHORITY, 17040452, 19690, 27830);
        public static final MediaSize ISO_B3 = new MediaSize("ISO_B3", ZenModeConfig.SYSTEM_AUTHORITY, 17040453, 13900, 19690);
        public static final MediaSize ISO_B4 = new MediaSize("ISO_B4", ZenModeConfig.SYSTEM_AUTHORITY, 17040454, 9840, 13900);
        public static final MediaSize ISO_B5 = new MediaSize("ISO_B5", ZenModeConfig.SYSTEM_AUTHORITY, 17040455, 6930, 9840);
        public static final MediaSize ISO_B6 = new MediaSize("ISO_B6", ZenModeConfig.SYSTEM_AUTHORITY, 17040456, 4920, 6930);
        public static final MediaSize ISO_B7 = new MediaSize("ISO_B7", ZenModeConfig.SYSTEM_AUTHORITY, 17040457, 3460, 4920);
        public static final MediaSize ISO_B8 = new MediaSize("ISO_B8", ZenModeConfig.SYSTEM_AUTHORITY, 17040458, 2440, 3460);
        public static final MediaSize ISO_B9 = new MediaSize("ISO_B9", ZenModeConfig.SYSTEM_AUTHORITY, 17040459, 1730, 2440);
        public static final MediaSize ISO_C0 = new MediaSize("ISO_C0", ZenModeConfig.SYSTEM_AUTHORITY, 17040460, 36100, 51060);
        public static final MediaSize ISO_C1 = new MediaSize("ISO_C1", ZenModeConfig.SYSTEM_AUTHORITY, 17040461, 25510, 36100);
        public static final MediaSize ISO_C10 = new MediaSize("ISO_C10", ZenModeConfig.SYSTEM_AUTHORITY, 17040462, 1100, 1570);
        public static final MediaSize ISO_C2 = new MediaSize("ISO_C2", ZenModeConfig.SYSTEM_AUTHORITY, 17040463, 18030, 25510);
        public static final MediaSize ISO_C3 = new MediaSize("ISO_C3", ZenModeConfig.SYSTEM_AUTHORITY, 17040464, 12760, 18030);
        public static final MediaSize ISO_C4 = new MediaSize("ISO_C4", ZenModeConfig.SYSTEM_AUTHORITY, 17040465, 9020, 12760);
        public static final MediaSize ISO_C5 = new MediaSize("ISO_C5", ZenModeConfig.SYSTEM_AUTHORITY, 17040466, 6380, 9020);
        public static final MediaSize ISO_C6 = new MediaSize("ISO_C6", ZenModeConfig.SYSTEM_AUTHORITY, 17040467, 4490, 6380);
        public static final MediaSize ISO_C7 = new MediaSize("ISO_C7", ZenModeConfig.SYSTEM_AUTHORITY, 17040468, 3190, 4490);
        public static final MediaSize ISO_C8 = new MediaSize("ISO_C8", ZenModeConfig.SYSTEM_AUTHORITY, 17040469, 2240, 3190);
        public static final MediaSize ISO_C9 = new MediaSize("ISO_C9", ZenModeConfig.SYSTEM_AUTHORITY, 17040470, 1570, 2240);
        public static final MediaSize JIS_B0 = new MediaSize("JIS_B0", ZenModeConfig.SYSTEM_AUTHORITY, 17040475, 40551, 57323);
        public static final MediaSize JIS_B1 = new MediaSize("JIS_B1", ZenModeConfig.SYSTEM_AUTHORITY, 17040476, 28661, 40551);
        public static final MediaSize JIS_B10 = new MediaSize("JIS_B10", ZenModeConfig.SYSTEM_AUTHORITY, 17040477, 1259, 1772);
        public static final MediaSize JIS_B2 = new MediaSize("JIS_B2", ZenModeConfig.SYSTEM_AUTHORITY, 17040478, 20276, 28661);
        public static final MediaSize JIS_B3 = new MediaSize("JIS_B3", ZenModeConfig.SYSTEM_AUTHORITY, 17040479, 14331, 20276);
        public static final MediaSize JIS_B4 = new MediaSize("JIS_B4", ZenModeConfig.SYSTEM_AUTHORITY, 17040480, 10118, 14331);
        public static final MediaSize JIS_B5 = new MediaSize("JIS_B5", ZenModeConfig.SYSTEM_AUTHORITY, 17040481, 7165, 10118);
        public static final MediaSize JIS_B6 = new MediaSize("JIS_B6", ZenModeConfig.SYSTEM_AUTHORITY, 17040482, 5049, 7165);
        public static final MediaSize JIS_B7 = new MediaSize("JIS_B7", ZenModeConfig.SYSTEM_AUTHORITY, 17040483, 3583, 5049);
        public static final MediaSize JIS_B8 = new MediaSize("JIS_B8", ZenModeConfig.SYSTEM_AUTHORITY, 17040484, 2520, 3583);
        public static final MediaSize JIS_B9 = new MediaSize("JIS_B9", ZenModeConfig.SYSTEM_AUTHORITY, 17040485, 1772, 2520);
        public static final MediaSize JIS_EXEC = new MediaSize("JIS_EXEC", ZenModeConfig.SYSTEM_AUTHORITY, 17040486, 8504, 12992);
        public static final MediaSize JPN_CHOU2 = new MediaSize("JPN_CHOU2", ZenModeConfig.SYSTEM_AUTHORITY, 17040471, 4374, 5748);
        public static final MediaSize JPN_CHOU3 = new MediaSize("JPN_CHOU3", ZenModeConfig.SYSTEM_AUTHORITY, 17040472, 4724, 9252);
        public static final MediaSize JPN_CHOU4 = new MediaSize("JPN_CHOU4", ZenModeConfig.SYSTEM_AUTHORITY, 17040473, 3543, 8071);
        public static final MediaSize JPN_HAGAKI = new MediaSize("JPN_HAGAKI", ZenModeConfig.SYSTEM_AUTHORITY, 17040474, 3937, 5827);
        public static final MediaSize JPN_KAHU = new MediaSize("JPN_KAHU", ZenModeConfig.SYSTEM_AUTHORITY, 17040487, 9449, 12681);
        public static final MediaSize JPN_KAKU2 = new MediaSize("JPN_KAKU2", ZenModeConfig.SYSTEM_AUTHORITY, 17040488, 9449, 13071);
        public static final MediaSize JPN_OUFUKU = new MediaSize("JPN_OUFUKU", ZenModeConfig.SYSTEM_AUTHORITY, 17040489, 5827, 7874);
        public static final MediaSize JPN_YOU4 = new MediaSize("JPN_YOU4", ZenModeConfig.SYSTEM_AUTHORITY, 17040490, 4134, 9252);
        private static final String LOG_TAG = "MediaSize";
        public static final MediaSize NA_FOOLSCAP = new MediaSize("NA_FOOLSCAP", ZenModeConfig.SYSTEM_AUTHORITY, 17040491, 8000, 13000);
        public static final MediaSize NA_GOVT_LETTER = new MediaSize("NA_GOVT_LETTER", ZenModeConfig.SYSTEM_AUTHORITY, 17040492, 8000, 10500);
        public static final MediaSize NA_INDEX_3X5 = new MediaSize("NA_INDEX_3X5", ZenModeConfig.SYSTEM_AUTHORITY, 17040493, 3000, 5000);
        public static final MediaSize NA_INDEX_4X6 = new MediaSize("NA_INDEX_4X6", ZenModeConfig.SYSTEM_AUTHORITY, 17040494, 4000, 6000);
        public static final MediaSize NA_INDEX_5X8 = new MediaSize("NA_INDEX_5X8", ZenModeConfig.SYSTEM_AUTHORITY, 17040495, 5000, 8000);
        public static final MediaSize NA_JUNIOR_LEGAL = new MediaSize("NA_JUNIOR_LEGAL", ZenModeConfig.SYSTEM_AUTHORITY, 17040496, 8000, 5000);
        public static final MediaSize NA_LEDGER = new MediaSize("NA_LEDGER", ZenModeConfig.SYSTEM_AUTHORITY, 17040497, 17000, 11000);
        public static final MediaSize NA_LEGAL = new MediaSize("NA_LEGAL", ZenModeConfig.SYSTEM_AUTHORITY, 17040498, 8500, 14000);
        public static final MediaSize NA_LETTER = new MediaSize("NA_LETTER", ZenModeConfig.SYSTEM_AUTHORITY, 17040499, 8500, 11000);
        public static final MediaSize NA_MONARCH = new MediaSize("NA_MONARCH", ZenModeConfig.SYSTEM_AUTHORITY, 17040500, 7250, 10500);
        public static final MediaSize NA_QUARTO = new MediaSize("NA_QUARTO", ZenModeConfig.SYSTEM_AUTHORITY, 17040501, 8000, 10000);
        public static final MediaSize NA_TABLOID = new MediaSize("NA_TABLOID", ZenModeConfig.SYSTEM_AUTHORITY, 17040502, 11000, 17000);
        public static final MediaSize OM_DAI_PA_KAI = new MediaSize("OM_DAI_PA_KAI", ZenModeConfig.SYSTEM_AUTHORITY, 17040422, 10827, 15551);
        public static final MediaSize OM_JUURO_KU_KAI = new MediaSize("OM_JUURO_KU_KAI", ZenModeConfig.SYSTEM_AUTHORITY, 17040423, 7796, 10827);
        public static final MediaSize OM_PA_KAI = new MediaSize("OM_PA_KAI", ZenModeConfig.SYSTEM_AUTHORITY, 17040424, 10512, 15315);
        public static final MediaSize PRC_1 = new MediaSize("PRC_1", ZenModeConfig.SYSTEM_AUTHORITY, 17040425, 4015, 6496);
        public static final MediaSize PRC_10 = new MediaSize("PRC_10", ZenModeConfig.SYSTEM_AUTHORITY, 17040426, 12756, 18032);
        public static final MediaSize PRC_16K = new MediaSize("PRC_16K", ZenModeConfig.SYSTEM_AUTHORITY, 17040427, 5749, 8465);
        public static final MediaSize PRC_2 = new MediaSize("PRC_2", ZenModeConfig.SYSTEM_AUTHORITY, 17040428, 4015, 6929);
        public static final MediaSize PRC_3 = new MediaSize("PRC_3", ZenModeConfig.SYSTEM_AUTHORITY, 17040429, 4921, 6929);
        public static final MediaSize PRC_4 = new MediaSize("PRC_4", ZenModeConfig.SYSTEM_AUTHORITY, 17040430, 4330, 8189);
        public static final MediaSize PRC_5 = new MediaSize("PRC_5", ZenModeConfig.SYSTEM_AUTHORITY, 17040431, 4330, 8661);
        public static final MediaSize PRC_6 = new MediaSize("PRC_6", ZenModeConfig.SYSTEM_AUTHORITY, 17040432, 4724, 12599);
        public static final MediaSize PRC_7 = new MediaSize("PRC_7", ZenModeConfig.SYSTEM_AUTHORITY, 17040433, 6299, 9055);
        public static final MediaSize PRC_8 = new MediaSize("PRC_8", ZenModeConfig.SYSTEM_AUTHORITY, 17040434, 4724, 12165);
        public static final MediaSize PRC_9 = new MediaSize("PRC_9", ZenModeConfig.SYSTEM_AUTHORITY, 17040435, 9016, 12756);
        public static final MediaSize ROC_16K = new MediaSize("ROC_16K", ZenModeConfig.SYSTEM_AUTHORITY, 17040436, 7677, 10629);
        public static final MediaSize ROC_8K = new MediaSize("ROC_8K", ZenModeConfig.SYSTEM_AUTHORITY, 17040437, 10629, 15354);
        public static final MediaSize UNKNOWN_LANDSCAPE = new MediaSize("UNKNOWN_LANDSCAPE", ZenModeConfig.SYSTEM_AUTHORITY, 17040503, Integer.MAX_VALUE, 1);
        public static final MediaSize UNKNOWN_PORTRAIT = new MediaSize("UNKNOWN_PORTRAIT", ZenModeConfig.SYSTEM_AUTHORITY, 17040504, 1, Integer.MAX_VALUE);
        private static final Map<String, MediaSize> sIdToMediaSizeMap = new ArrayMap();
        private final int mHeightMils;
        private final String mId;
        public final String mLabel;
        public final int mLabelResId;
        public final String mPackageName;
        private final int mWidthMils;

        public MediaSize(String id, String packageName, int labelResId, int widthMils, int heightMils) {
            this(id, null, packageName, widthMils, heightMils, labelResId);
            sIdToMediaSizeMap.put(this.mId, this);
        }

        public MediaSize(String id, String label, int widthMils, int heightMils) {
            this(id, label, null, widthMils, heightMils, 0);
        }

        public static ArraySet<MediaSize> getAllPredefinedSizes() {
            ArraySet<MediaSize> definedMediaSizes = new ArraySet(sIdToMediaSizeMap.values());
            definedMediaSizes.remove(UNKNOWN_PORTRAIT);
            definedMediaSizes.remove(UNKNOWN_LANDSCAPE);
            return definedMediaSizes;
        }

        public MediaSize(String id, String label, String packageName, int widthMils, int heightMils, int labelResId) {
            this.mPackageName = packageName;
            this.mId = (String) Preconditions.checkStringNotEmpty(id, "id cannot be empty.");
            this.mLabelResId = labelResId;
            this.mWidthMils = Preconditions.checkArgumentPositive(widthMils, "widthMils cannot be less than or equal to zero.");
            this.mHeightMils = Preconditions.checkArgumentPositive(heightMils, "heightMils cannot be less than or equal to zero.");
            this.mLabel = label;
            boolean z = true;
            boolean isEmpty = TextUtils.isEmpty(label) ^ 1;
            boolean z2 = (TextUtils.isEmpty(packageName) || labelResId == 0) ? false : true;
            if (isEmpty == z2) {
                z = false;
            }
            Preconditions.checkArgument(z, "label cannot be empty.");
        }

        public String getId() {
            return this.mId;
        }

        public String getLabel(PackageManager packageManager) {
            if (!TextUtils.isEmpty(this.mPackageName) && this.mLabelResId > 0) {
                try {
                    return packageManager.getResourcesForApplication(this.mPackageName).getString(this.mLabelResId);
                } catch (NameNotFoundException | NotFoundException e) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Could not load resouce");
                    stringBuilder.append(this.mLabelResId);
                    stringBuilder.append(" from package ");
                    stringBuilder.append(this.mPackageName);
                    Log.w(str, stringBuilder.toString());
                }
            }
            return this.mLabel;
        }

        public int getWidthMils() {
            return this.mWidthMils;
        }

        public int getHeightMils() {
            return this.mHeightMils;
        }

        public boolean isPortrait() {
            return this.mHeightMils >= this.mWidthMils;
        }

        public MediaSize asPortrait() {
            if (isPortrait()) {
                return this;
            }
            return new MediaSize(this.mId, this.mLabel, this.mPackageName, Math.min(this.mWidthMils, this.mHeightMils), Math.max(this.mWidthMils, this.mHeightMils), this.mLabelResId);
        }

        public MediaSize asLandscape() {
            if (isPortrait()) {
                return new MediaSize(this.mId, this.mLabel, this.mPackageName, Math.max(this.mWidthMils, this.mHeightMils), Math.min(this.mWidthMils, this.mHeightMils), this.mLabelResId);
            }
            return this;
        }

        void writeToParcel(Parcel parcel) {
            parcel.writeString(this.mId);
            parcel.writeString(this.mLabel);
            parcel.writeString(this.mPackageName);
            parcel.writeInt(this.mWidthMils);
            parcel.writeInt(this.mHeightMils);
            parcel.writeInt(this.mLabelResId);
        }

        static MediaSize createFromParcel(Parcel parcel) {
            return new MediaSize(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
        }

        public int hashCode() {
            return (31 * ((31 * 1) + this.mWidthMils)) + this.mHeightMils;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MediaSize other = (MediaSize) obj;
            if (this.mWidthMils == other.mWidthMils && this.mHeightMils == other.mHeightMils) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("MediaSize{");
            builder.append("id: ");
            builder.append(this.mId);
            builder.append(", label: ");
            builder.append(this.mLabel);
            builder.append(", packageName: ");
            builder.append(this.mPackageName);
            builder.append(", heightMils: ");
            builder.append(this.mHeightMils);
            builder.append(", widthMils: ");
            builder.append(this.mWidthMils);
            builder.append(", labelResId: ");
            builder.append(this.mLabelResId);
            builder.append("}");
            return builder.toString();
        }

        public static MediaSize getStandardMediaSizeById(String id) {
            return (MediaSize) sIdToMediaSizeMap.get(id);
        }
    }

    public static final class Resolution {
        private final int mHorizontalDpi;
        private final String mId;
        private final String mLabel;
        private final int mVerticalDpi;

        public Resolution(String id, String label, int horizontalDpi, int verticalDpi) {
            if (TextUtils.isEmpty(id)) {
                throw new IllegalArgumentException("id cannot be empty.");
            } else if (TextUtils.isEmpty(label)) {
                throw new IllegalArgumentException("label cannot be empty.");
            } else if (horizontalDpi <= 0) {
                throw new IllegalArgumentException("horizontalDpi cannot be less than or equal to zero.");
            } else if (verticalDpi > 0) {
                this.mId = id;
                this.mLabel = label;
                this.mHorizontalDpi = horizontalDpi;
                this.mVerticalDpi = verticalDpi;
            } else {
                throw new IllegalArgumentException("verticalDpi cannot be less than or equal to zero.");
            }
        }

        public String getId() {
            return this.mId;
        }

        public String getLabel() {
            return this.mLabel;
        }

        public int getHorizontalDpi() {
            return this.mHorizontalDpi;
        }

        public int getVerticalDpi() {
            return this.mVerticalDpi;
        }

        void writeToParcel(Parcel parcel) {
            parcel.writeString(this.mId);
            parcel.writeString(this.mLabel);
            parcel.writeInt(this.mHorizontalDpi);
            parcel.writeInt(this.mVerticalDpi);
        }

        static Resolution createFromParcel(Parcel parcel) {
            return new Resolution(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt());
        }

        public int hashCode() {
            return (31 * ((31 * 1) + this.mHorizontalDpi)) + this.mVerticalDpi;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Resolution other = (Resolution) obj;
            if (this.mHorizontalDpi == other.mHorizontalDpi && this.mVerticalDpi == other.mVerticalDpi) {
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Resolution{");
            builder.append("id: ");
            builder.append(this.mId);
            builder.append(", label: ");
            builder.append(this.mLabel);
            builder.append(", horizontalDpi: ");
            builder.append(this.mHorizontalDpi);
            builder.append(", verticalDpi: ");
            builder.append(this.mVerticalDpi);
            builder.append("}");
            return builder.toString();
        }
    }

    /* synthetic */ PrintAttributes(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    PrintAttributes() {
    }

    private PrintAttributes(Parcel parcel) {
        Margins margins = null;
        this.mMediaSize = parcel.readInt() == 1 ? MediaSize.createFromParcel(parcel) : null;
        this.mResolution = parcel.readInt() == 1 ? Resolution.createFromParcel(parcel) : null;
        if (parcel.readInt() == 1) {
            margins = Margins.createFromParcel(parcel);
        }
        this.mMinMargins = margins;
        this.mColorMode = parcel.readInt();
        if (this.mColorMode != 0) {
            enforceValidColorMode(this.mColorMode);
        }
        this.mDuplexMode = parcel.readInt();
        if (this.mDuplexMode != 0) {
            enforceValidDuplexMode(this.mDuplexMode);
        }
    }

    public MediaSize getMediaSize() {
        return this.mMediaSize;
    }

    public void setMediaSize(MediaSize mediaSize) {
        this.mMediaSize = mediaSize;
    }

    public Resolution getResolution() {
        return this.mResolution;
    }

    public void setResolution(Resolution resolution) {
        this.mResolution = resolution;
    }

    public Margins getMinMargins() {
        return this.mMinMargins;
    }

    public void setMinMargins(Margins margins) {
        this.mMinMargins = margins;
    }

    public int getColorMode() {
        return this.mColorMode;
    }

    public void setColorMode(int colorMode) {
        enforceValidColorMode(colorMode);
        this.mColorMode = colorMode;
    }

    public boolean isPortrait() {
        return this.mMediaSize.isPortrait();
    }

    public int getDuplexMode() {
        return this.mDuplexMode;
    }

    public void setDuplexMode(int duplexMode) {
        enforceValidDuplexMode(duplexMode);
        this.mDuplexMode = duplexMode;
    }

    public PrintAttributes asPortrait() {
        if (isPortrait()) {
            return this;
        }
        PrintAttributes attributes = new PrintAttributes();
        attributes.setMediaSize(getMediaSize().asPortrait());
        Resolution oldResolution = getResolution();
        attributes.setResolution(new Resolution(oldResolution.getId(), oldResolution.getLabel(), oldResolution.getVerticalDpi(), oldResolution.getHorizontalDpi()));
        attributes.setMinMargins(getMinMargins());
        attributes.setColorMode(getColorMode());
        attributes.setDuplexMode(getDuplexMode());
        return attributes;
    }

    public PrintAttributes asLandscape() {
        if (!isPortrait()) {
            return this;
        }
        PrintAttributes attributes = new PrintAttributes();
        attributes.setMediaSize(getMediaSize().asLandscape());
        Resolution oldResolution = getResolution();
        attributes.setResolution(new Resolution(oldResolution.getId(), oldResolution.getLabel(), oldResolution.getVerticalDpi(), oldResolution.getHorizontalDpi()));
        attributes.setMinMargins(getMinMargins());
        attributes.setColorMode(getColorMode());
        attributes.setDuplexMode(getDuplexMode());
        return attributes;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        if (this.mMediaSize != null) {
            parcel.writeInt(1);
            this.mMediaSize.writeToParcel(parcel);
        } else {
            parcel.writeInt(0);
        }
        if (this.mResolution != null) {
            parcel.writeInt(1);
            this.mResolution.writeToParcel(parcel);
        } else {
            parcel.writeInt(0);
        }
        if (this.mMinMargins != null) {
            parcel.writeInt(1);
            this.mMinMargins.writeToParcel(parcel);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.mColorMode);
        parcel.writeInt(this.mDuplexMode);
    }

    public int describeContents() {
        return 0;
    }

    public int hashCode() {
        int i = 0;
        int result = 31 * ((31 * ((31 * ((31 * ((31 * 1) + this.mColorMode)) + this.mDuplexMode)) + (this.mMinMargins == null ? 0 : this.mMinMargins.hashCode()))) + (this.mMediaSize == null ? 0 : this.mMediaSize.hashCode()));
        if (this.mResolution != null) {
            i = this.mResolution.hashCode();
        }
        return result + i;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PrintAttributes other = (PrintAttributes) obj;
        if (this.mColorMode != other.mColorMode || this.mDuplexMode != other.mDuplexMode) {
            return false;
        }
        if (this.mMinMargins == null) {
            if (other.mMinMargins != null) {
                return false;
            }
        } else if (!this.mMinMargins.equals(other.mMinMargins)) {
            return false;
        }
        if (this.mMediaSize == null) {
            if (other.mMediaSize != null) {
                return false;
            }
        } else if (!this.mMediaSize.equals(other.mMediaSize)) {
            return false;
        }
        if (this.mResolution == null) {
            if (other.mResolution != null) {
                return false;
            }
        } else if (!this.mResolution.equals(other.mResolution)) {
            return false;
        }
        return true;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PrintAttributes{");
        builder.append("mediaSize: ");
        builder.append(this.mMediaSize);
        if (this.mMediaSize != null) {
            builder.append(", orientation: ");
            builder.append(this.mMediaSize.isPortrait() ? "portrait" : "landscape");
        } else {
            builder.append(", orientation: ");
            builder.append("null");
        }
        builder.append(", resolution: ");
        builder.append(this.mResolution);
        builder.append(", minMargins: ");
        builder.append(this.mMinMargins);
        builder.append(", colorMode: ");
        builder.append(colorModeToString(this.mColorMode));
        builder.append(", duplexMode: ");
        builder.append(duplexModeToString(this.mDuplexMode));
        builder.append("}");
        return builder.toString();
    }

    public void clear() {
        this.mMediaSize = null;
        this.mResolution = null;
        this.mMinMargins = null;
        this.mColorMode = 0;
        this.mDuplexMode = 0;
    }

    public void copyFrom(PrintAttributes other) {
        this.mMediaSize = other.mMediaSize;
        this.mResolution = other.mResolution;
        this.mMinMargins = other.mMinMargins;
        this.mColorMode = other.mColorMode;
        this.mDuplexMode = other.mDuplexMode;
    }

    static String colorModeToString(int colorMode) {
        switch (colorMode) {
            case 1:
                return "COLOR_MODE_MONOCHROME";
            case 2:
                return "COLOR_MODE_COLOR";
            default:
                return "COLOR_MODE_UNKNOWN";
        }
    }

    static String duplexModeToString(int duplexMode) {
        if (duplexMode == 4) {
            return "DUPLEX_MODE_SHORT_EDGE";
        }
        switch (duplexMode) {
            case 1:
                return "DUPLEX_MODE_NONE";
            case 2:
                return "DUPLEX_MODE_LONG_EDGE";
            default:
                return "DUPLEX_MODE_UNKNOWN";
        }
    }

    static void enforceValidColorMode(int colorMode) {
        if ((colorMode & 3) == 0 || Integer.bitCount(colorMode) != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid color mode: ");
            stringBuilder.append(colorMode);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    static void enforceValidDuplexMode(int duplexMode) {
        if ((duplexMode & 7) == 0 || Integer.bitCount(duplexMode) != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid duplex mode: ");
            stringBuilder.append(duplexMode);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }
}
