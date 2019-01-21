package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.format.Time;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.Arrays;

public class SmsCbEtwsInfo implements Parcelable {
    public static final Creator<SmsCbEtwsInfo> CREATOR = new Creator<SmsCbEtwsInfo>() {
        public SmsCbEtwsInfo createFromParcel(Parcel in) {
            return new SmsCbEtwsInfo(in);
        }

        public SmsCbEtwsInfo[] newArray(int size) {
            return new SmsCbEtwsInfo[size];
        }
    };
    public static final int ETWS_WARNING_TYPE_EARTHQUAKE = 0;
    public static final int ETWS_WARNING_TYPE_EARTHQUAKE_AND_TSUNAMI = 2;
    public static final int ETWS_WARNING_TYPE_OTHER_EMERGENCY = 4;
    public static final int ETWS_WARNING_TYPE_TEST_MESSAGE = 3;
    public static final int ETWS_WARNING_TYPE_TSUNAMI = 1;
    public static final int ETWS_WARNING_TYPE_UNKNOWN = -1;
    private final boolean mActivatePopup;
    private final boolean mEmergencyUserAlert;
    private final boolean mPrimary;
    private final byte[] mWarningSecurityInformation;
    private final int mWarningType;

    public SmsCbEtwsInfo(int warningType, boolean emergencyUserAlert, boolean activatePopup, boolean primary, byte[] warningSecurityInformation) {
        this.mWarningType = warningType;
        this.mEmergencyUserAlert = emergencyUserAlert;
        this.mActivatePopup = activatePopup;
        this.mPrimary = primary;
        this.mWarningSecurityInformation = warningSecurityInformation;
    }

    SmsCbEtwsInfo(Parcel in) {
        this.mWarningType = in.readInt();
        boolean z = false;
        this.mEmergencyUserAlert = in.readInt() != 0;
        this.mActivatePopup = in.readInt() != 0;
        if (in.readInt() != 0) {
            z = true;
        }
        this.mPrimary = z;
        this.mWarningSecurityInformation = in.createByteArray();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mWarningType);
        dest.writeInt(this.mEmergencyUserAlert);
        dest.writeInt(this.mActivatePopup);
        dest.writeInt(this.mPrimary);
        dest.writeByteArray(this.mWarningSecurityInformation);
    }

    public int getWarningType() {
        return this.mWarningType;
    }

    public boolean isEmergencyUserAlert() {
        return this.mEmergencyUserAlert;
    }

    public boolean isPopupAlert() {
        return this.mActivatePopup;
    }

    public boolean isPrimary() {
        return this.mPrimary;
    }

    public long getPrimaryNotificationTimestamp() {
        if (this.mWarningSecurityInformation == null || this.mWarningSecurityInformation.length < 7) {
            return 0;
        }
        int year = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[0]);
        int month = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[1]);
        int day = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[2]);
        int hour = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[3]);
        int minute = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[4]);
        int second = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[5]);
        byte tzByte = this.mWarningSecurityInformation[6];
        int timezoneOffset = IccUtils.gsmBcdByteToInt((byte) (tzByte & -9));
        timezoneOffset = (tzByte & 8) == 0 ? timezoneOffset : -timezoneOffset;
        Time time = new Time(Time.TIMEZONE_UTC);
        time.year = year + 2000;
        time.month = month - 1;
        time.monthDay = day;
        time.hour = hour;
        time.minute = minute;
        time.second = second;
        return time.toMillis(true) - ((long) (((timezoneOffset * 15) * 60) * 1000));
    }

    public byte[] getPrimaryNotificationSignature() {
        if (this.mWarningSecurityInformation == null || this.mWarningSecurityInformation.length < 50) {
            return null;
        }
        return Arrays.copyOfRange(this.mWarningSecurityInformation, 7, 50);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SmsCbEtwsInfo{warningType=");
        stringBuilder.append(this.mWarningType);
        stringBuilder.append(", emergencyUserAlert=");
        stringBuilder.append(this.mEmergencyUserAlert);
        stringBuilder.append(", activatePopup=");
        stringBuilder.append(this.mActivatePopup);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }
}
