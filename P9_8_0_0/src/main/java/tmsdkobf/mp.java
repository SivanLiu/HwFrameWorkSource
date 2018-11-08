package tmsdkobf;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import tmsdk.common.module.intelli_sms.SmsCheckResult.SmsRuleTypeID;

public class mp implements Parcelable {
    public static final Creator<mp> CREATOR = new Creator<mp>() {
        public mp[] aV(int i) {
            return new mp[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return i(parcel);
        }

        public mp i(Parcel parcel) {
            Object -l_2_R = new mp();
            -l_2_R.fg = parcel.readInt();
            -l_2_R.fh = parcel.readInt();
            return -l_2_R;
        }

        public /* synthetic */ Object[] newArray(int i) {
            return aV(i);
        }
    };
    public int fg;
    public int fh;

    private mp() {
    }

    public mp(SmsRuleTypeID smsRuleTypeID) {
        this.fg = smsRuleTypeID.uiRuleType;
        this.fh = smsRuleTypeID.uiRuleTypeId;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.fg);
        parcel.writeInt(this.fh);
    }
}
