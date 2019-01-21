package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class SmsCbMessage implements Parcelable {
    public static final Creator<SmsCbMessage> CREATOR = new Creator<SmsCbMessage>() {
        public SmsCbMessage createFromParcel(Parcel in) {
            return new SmsCbMessage(in);
        }

        public SmsCbMessage[] newArray(int size) {
            return new SmsCbMessage[size];
        }
    };
    public static final int GEOGRAPHICAL_SCOPE_CELL_WIDE = 3;
    public static final int GEOGRAPHICAL_SCOPE_CELL_WIDE_IMMEDIATE = 0;
    public static final int GEOGRAPHICAL_SCOPE_LA_WIDE = 2;
    public static final int GEOGRAPHICAL_SCOPE_PLMN_WIDE = 1;
    protected static final String LOG_TAG = "SMSCB";
    public static final int MESSAGE_FORMAT_3GPP = 1;
    public static final int MESSAGE_FORMAT_3GPP2 = 2;
    public static final int MESSAGE_PRIORITY_EMERGENCY = 3;
    public static final int MESSAGE_PRIORITY_INTERACTIVE = 1;
    public static final int MESSAGE_PRIORITY_NORMAL = 0;
    public static final int MESSAGE_PRIORITY_URGENT = 2;
    private final String mBody;
    private final SmsCbCmasInfo mCmasWarningInfo;
    private final SmsCbEtwsInfo mEtwsWarningInfo;
    private final int mGeographicalScope;
    private final String mLanguage;
    private final SmsCbLocation mLocation;
    private final int mMessageFormat;
    private final int mPriority;
    private final int mSerialNumber;
    private final int mServiceCategory;

    public SmsCbMessage(int messageFormat, int geographicalScope, int serialNumber, SmsCbLocation location, int serviceCategory, String language, String body, int priority, SmsCbEtwsInfo etwsWarningInfo, SmsCbCmasInfo cmasWarningInfo) {
        this.mMessageFormat = messageFormat;
        this.mGeographicalScope = geographicalScope;
        this.mSerialNumber = serialNumber;
        this.mLocation = location;
        this.mServiceCategory = serviceCategory;
        this.mLanguage = language;
        this.mBody = body;
        this.mPriority = priority;
        this.mEtwsWarningInfo = etwsWarningInfo;
        this.mCmasWarningInfo = cmasWarningInfo;
    }

    public SmsCbMessage(Parcel in) {
        this.mMessageFormat = in.readInt();
        this.mGeographicalScope = in.readInt();
        this.mSerialNumber = in.readInt();
        this.mLocation = new SmsCbLocation(in);
        this.mServiceCategory = in.readInt();
        this.mLanguage = in.readString();
        this.mBody = in.readString();
        this.mPriority = in.readInt();
        int type = in.readInt();
        if (type == 67) {
            this.mEtwsWarningInfo = null;
            this.mCmasWarningInfo = new SmsCbCmasInfo(in);
        } else if (type != 69) {
            this.mEtwsWarningInfo = null;
            this.mCmasWarningInfo = null;
        } else {
            this.mEtwsWarningInfo = new SmsCbEtwsInfo(in);
            this.mCmasWarningInfo = null;
        }
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mMessageFormat);
        dest.writeInt(this.mGeographicalScope);
        dest.writeInt(this.mSerialNumber);
        this.mLocation.writeToParcel(dest, flags);
        dest.writeInt(this.mServiceCategory);
        dest.writeString(this.mLanguage);
        dest.writeString(this.mBody);
        dest.writeInt(this.mPriority);
        if (this.mEtwsWarningInfo != null) {
            dest.writeInt(69);
            this.mEtwsWarningInfo.writeToParcel(dest, flags);
        } else if (this.mCmasWarningInfo != null) {
            dest.writeInt(67);
            this.mCmasWarningInfo.writeToParcel(dest, flags);
        } else {
            dest.writeInt(48);
        }
    }

    public int getGeographicalScope() {
        return this.mGeographicalScope;
    }

    public int getSerialNumber() {
        return this.mSerialNumber;
    }

    public SmsCbLocation getLocation() {
        return this.mLocation;
    }

    public int getServiceCategory() {
        return this.mServiceCategory;
    }

    public String getLanguageCode() {
        return this.mLanguage;
    }

    public String getMessageBody() {
        return this.mBody;
    }

    public int getMessageFormat() {
        return this.mMessageFormat;
    }

    public int getMessagePriority() {
        return this.mPriority;
    }

    public SmsCbEtwsInfo getEtwsWarningInfo() {
        return this.mEtwsWarningInfo;
    }

    public SmsCbCmasInfo getCmasWarningInfo() {
        return this.mCmasWarningInfo;
    }

    public boolean isEmergencyMessage() {
        return this.mPriority == 3;
    }

    public boolean isEtwsMessage() {
        return this.mEtwsWarningInfo != null;
    }

    public boolean isCmasMessage() {
        return this.mCmasWarningInfo != null;
    }

    public String toString() {
        StringBuilder stringBuilder;
        String stringBuilder2;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("SmsCbMessage{geographicalScope=");
        stringBuilder3.append(this.mGeographicalScope);
        stringBuilder3.append(", serialNumber=");
        stringBuilder3.append(this.mSerialNumber);
        stringBuilder3.append(", location=");
        stringBuilder3.append(this.mLocation);
        stringBuilder3.append(", serviceCategory=");
        stringBuilder3.append(this.mServiceCategory);
        stringBuilder3.append(", language=");
        stringBuilder3.append(this.mLanguage);
        stringBuilder3.append(", body=");
        stringBuilder3.append(this.mBody);
        stringBuilder3.append(", priority=");
        stringBuilder3.append(this.mPriority);
        if (this.mEtwsWarningInfo != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(", ");
            stringBuilder.append(this.mEtwsWarningInfo.toString());
            stringBuilder2 = stringBuilder.toString();
        } else {
            stringBuilder2 = "";
        }
        stringBuilder3.append(stringBuilder2);
        if (this.mCmasWarningInfo != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(", ");
            stringBuilder.append(this.mCmasWarningInfo.toString());
            stringBuilder2 = stringBuilder.toString();
        } else {
            stringBuilder2 = "";
        }
        stringBuilder3.append(stringBuilder2);
        stringBuilder3.append('}');
        return stringBuilder3.toString();
    }

    public int describeContents() {
        return 0;
    }
}
