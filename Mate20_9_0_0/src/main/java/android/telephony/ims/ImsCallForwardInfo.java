package android.telephony.ims;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

@SystemApi
public final class ImsCallForwardInfo implements Parcelable {
    public static final Creator<ImsCallForwardInfo> CREATOR = new Creator<ImsCallForwardInfo>() {
        public ImsCallForwardInfo createFromParcel(Parcel in) {
            return new ImsCallForwardInfo(in);
        }

        public ImsCallForwardInfo[] newArray(int size) {
            return new ImsCallForwardInfo[size];
        }
    };
    public int mCondition;
    public int mEndHour = 0;
    public int mEndMinute = 0;
    public String mNumber;
    public int mServiceClass;
    public int mStartHour = 0;
    public int mStartMinute = 0;
    public int mStatus;
    public int mTimeSeconds;
    public int mToA;

    public ImsCallForwardInfo(int condition, int status, int toA, int serviceClass, String number, int replyTimerSec) {
        this.mCondition = condition;
        this.mStatus = status;
        this.mToA = toA;
        this.mServiceClass = serviceClass;
        this.mNumber = number;
        this.mTimeSeconds = replyTimerSec;
    }

    public ImsCallForwardInfo(Parcel in) {
        readFromParcel(in);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mCondition);
        out.writeInt(this.mStatus);
        out.writeInt(this.mToA);
        out.writeString(this.mNumber);
        out.writeInt(this.mTimeSeconds);
        out.writeInt(this.mServiceClass);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append(", Condition: ");
        stringBuilder.append(this.mCondition);
        stringBuilder.append(", Status: ");
        stringBuilder.append(this.mStatus == 0 ? "disabled" : "enabled");
        stringBuilder.append(", ToA: ");
        stringBuilder.append(this.mToA);
        stringBuilder.append(", Service Class: ");
        stringBuilder.append(this.mServiceClass);
        stringBuilder.append(", Number=");
        stringBuilder.append(this.mNumber);
        stringBuilder.append(", Time (seconds): ");
        stringBuilder.append(this.mTimeSeconds);
        stringBuilder.append(", mStartHour=");
        stringBuilder.append(this.mStartHour);
        stringBuilder.append(", mStartMinute=");
        stringBuilder.append(this.mStartMinute);
        stringBuilder.append(", mEndHour=");
        stringBuilder.append(this.mEndHour);
        stringBuilder.append(", mEndMinute");
        stringBuilder.append(this.mEndMinute);
        return stringBuilder.toString();
    }

    private void readFromParcel(Parcel in) {
        this.mCondition = in.readInt();
        this.mStatus = in.readInt();
        this.mToA = in.readInt();
        this.mNumber = in.readString();
        this.mTimeSeconds = in.readInt();
        this.mServiceClass = in.readInt();
    }

    public int getCondition() {
        return this.mCondition;
    }

    public int getStatus() {
        return this.mStatus;
    }

    public int getToA() {
        return this.mToA;
    }

    public int getServiceClass() {
        return this.mServiceClass;
    }

    public String getNumber() {
        return this.mNumber;
    }

    public int getTimeSeconds() {
        return this.mTimeSeconds;
    }
}
