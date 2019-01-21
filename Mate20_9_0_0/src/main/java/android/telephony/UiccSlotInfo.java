package android.telephony;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

@SystemApi
public class UiccSlotInfo implements Parcelable {
    public static final int CARD_STATE_INFO_ABSENT = 1;
    public static final int CARD_STATE_INFO_ERROR = 3;
    public static final int CARD_STATE_INFO_PRESENT = 2;
    public static final int CARD_STATE_INFO_RESTRICTED = 4;
    public static final Creator<UiccSlotInfo> CREATOR = new Creator<UiccSlotInfo>() {
        public UiccSlotInfo createFromParcel(Parcel in) {
            return new UiccSlotInfo(in, null);
        }

        public UiccSlotInfo[] newArray(int size) {
            return new UiccSlotInfo[size];
        }
    };
    private final String mCardId;
    private final int mCardStateInfo;
    private final boolean mIsActive;
    private final boolean mIsEuicc;
    private final boolean mIsExtendedApduSupported;
    private final int mLogicalSlotIdx;

    @Retention(RetentionPolicy.SOURCE)
    public @interface CardStateInfo {
    }

    /* synthetic */ UiccSlotInfo(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    private UiccSlotInfo(Parcel in) {
        boolean z = false;
        this.mIsActive = in.readByte() != (byte) 0;
        this.mIsEuicc = in.readByte() != (byte) 0;
        this.mCardId = in.readString();
        this.mCardStateInfo = in.readInt();
        this.mLogicalSlotIdx = in.readInt();
        if (in.readByte() != (byte) 0) {
            z = true;
        }
        this.mIsExtendedApduSupported = z;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) this.mIsActive);
        dest.writeByte((byte) this.mIsEuicc);
        dest.writeString(this.mCardId);
        dest.writeInt(this.mCardStateInfo);
        dest.writeInt(this.mLogicalSlotIdx);
        dest.writeByte((byte) this.mIsExtendedApduSupported);
    }

    public int describeContents() {
        return 0;
    }

    public UiccSlotInfo(boolean isActive, boolean isEuicc, String cardId, int cardStateInfo, int logicalSlotIdx, boolean isExtendedApduSupported) {
        this.mIsActive = isActive;
        this.mIsEuicc = isEuicc;
        this.mCardId = cardId;
        this.mCardStateInfo = cardStateInfo;
        this.mLogicalSlotIdx = logicalSlotIdx;
        this.mIsExtendedApduSupported = isExtendedApduSupported;
    }

    public boolean getIsActive() {
        return this.mIsActive;
    }

    public boolean getIsEuicc() {
        return this.mIsEuicc;
    }

    public String getCardId() {
        return this.mCardId;
    }

    public int getCardStateInfo() {
        return this.mCardStateInfo;
    }

    public int getLogicalSlotIdx() {
        return this.mLogicalSlotIdx;
    }

    public boolean getIsExtendedApduSupported() {
        return this.mIsExtendedApduSupported;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UiccSlotInfo that = (UiccSlotInfo) obj;
        if (!(this.mIsActive == that.mIsActive && this.mIsEuicc == that.mIsEuicc && Objects.equals(this.mCardId, that.mCardId) && this.mCardStateInfo == that.mCardStateInfo && this.mLogicalSlotIdx == that.mLogicalSlotIdx && this.mIsExtendedApduSupported == that.mIsExtendedApduSupported)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * 1) + this.mIsActive)) + this.mIsEuicc)) + Objects.hashCode(this.mCardId))) + this.mCardStateInfo)) + this.mLogicalSlotIdx)) + this.mIsExtendedApduSupported;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UiccSlotInfo (mIsActive=");
        stringBuilder.append(this.mIsActive);
        stringBuilder.append(", mIsEuicc=");
        stringBuilder.append(this.mIsEuicc);
        stringBuilder.append(", mCardId=");
        stringBuilder.append(this.mCardId);
        stringBuilder.append(", cardState=");
        stringBuilder.append(this.mCardStateInfo);
        stringBuilder.append(", phoneId=");
        stringBuilder.append(this.mLogicalSlotIdx);
        stringBuilder.append(", mIsExtendedApduSupported=");
        stringBuilder.append(this.mIsExtendedApduSupported);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
