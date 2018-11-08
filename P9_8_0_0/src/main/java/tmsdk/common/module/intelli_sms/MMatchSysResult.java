package tmsdk.common.module.intelli_sms;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import tmsdk.common.module.intelli_sms.SmsCheckResult.SmsRuleTypeID;
import tmsdkobf.mp;

public class MMatchSysResult implements Parcelable {
    public static final Creator<MMatchSysResult> CREATOR = new Creator<MMatchSysResult>() {
        public MMatchSysResult[] aT(int i) {
            return new MMatchSysResult[i];
        }

        public /* synthetic */ Object createFromParcel(Parcel parcel) {
            return h(parcel);
        }

        public MMatchSysResult h(Parcel parcel) {
            Object -l_2_R = new MMatchSysResult();
            -l_2_R.finalAction = parcel.readInt();
            -l_2_R.contentType = parcel.readInt();
            -l_2_R.matchCnt = parcel.readInt();
            -l_2_R.minusMark = parcel.readInt();
            -l_2_R.actionReason = parcel.readInt();
            Object -l_3_R = parcel.readArray(mp.class.getClassLoader());
            if (-l_3_R != null && -l_3_R.length > 0) {
                int -l_4_I = -l_3_R.length;
                Object -l_5_R = new mp[-l_4_I];
                for (int -l_6_I = 0; -l_6_I < -l_4_I; -l_6_I++) {
                    -l_5_R[-l_6_I] = (mp) -l_3_R[-l_6_I];
                }
                -l_2_R.ruleTypeID = -l_5_R;
            }
            return -l_2_R;
        }

        public /* synthetic */ Object[] newArray(int i) {
            return aT(i);
        }
    };
    public static final int EM_FINAL_ACTION_DOUBT = 3;
    public static final int EM_FINAL_ACTION_INTERCEPT = 2;
    public static final int EM_FINAL_ACTION_NEXT_STEP = 4;
    public static final int EM_FINAL_ACTION_PASS = 1;
    public int actionReason;
    public int contentType;
    public int finalAction;
    public int matchCnt;
    public int minusMark;
    public mp[] ruleTypeID;

    private MMatchSysResult() {
    }

    public MMatchSysResult(int i, int i2, int i3, int i4, int i5, mp[] mpVarArr) {
        this.finalAction = i;
        this.contentType = i2;
        this.matchCnt = i3;
        this.minusMark = i4;
        this.actionReason = i5;
        this.ruleTypeID = mpVarArr;
    }

    public MMatchSysResult(SmsCheckResult smsCheckResult) {
        this.finalAction = smsCheckResult.uiFinalAction;
        this.contentType = smsCheckResult.uiContentType;
        this.matchCnt = smsCheckResult.uiMatchCnt;
        this.minusMark = (int) smsCheckResult.fScore;
        this.actionReason = smsCheckResult.uiActionReason;
        if (smsCheckResult.stRuleTypeID == null) {
            this.ruleTypeID = null;
            return;
        }
        this.ruleTypeID = new mp[smsCheckResult.stRuleTypeID.size()];
        for (int -l_2_I = 0; -l_2_I < this.ruleTypeID.length; -l_2_I++) {
            this.ruleTypeID[-l_2_I] = new mp((SmsRuleTypeID) smsCheckResult.stRuleTypeID.get(-l_2_I));
        }
    }

    public static int getSuggestion(MMatchSysResult mMatchSysResult) {
        int -l_1_I = mMatchSysResult.finalAction;
        if (-l_1_I <= 0 || -l_1_I > 4) {
            return -1;
        }
        if (-l_1_I != 1) {
            return -l_1_I;
        }
        if (mMatchSysResult.actionReason == 1 || mMatchSysResult.actionReason == 5) {
            return mMatchSysResult.minusMark > 10 ? 4 : 1;
        } else {
            return -l_1_I;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.finalAction);
        parcel.writeInt(this.contentType);
        parcel.writeInt(this.matchCnt);
        parcel.writeInt(this.minusMark);
        parcel.writeInt(this.actionReason);
        parcel.writeArray(this.ruleTypeID);
    }
}
