package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.util.Log;
import com.android.internal.telephony.cat.AppInterface.CommandType;

public class HwCatCmdMessage extends CatCmdMessage {
    public String mLanguageNotification;

    /* renamed from: com.android.internal.telephony.cat.HwCatCmdMessage$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType = new int[CommandType.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[CommandType.LANGUAGE_NOTIFICATION.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    public HwCatCmdMessage(CommandParams cmdParams) {
        super(cmdParams);
        Log.d("HwCatCmdMessage", "construct HwCatCmdMessage for cmdParams");
        if (AnonymousClass1.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[getCmdType().ordinal()] == 1) {
            this.mLanguageNotification = ((HwCommandParams) cmdParams).language;
        }
    }

    public HwCatCmdMessage(Parcel in) {
        super(in);
        Log.d("HwCatCmdMessage", "construct HwCatCmdMessage for Parcel");
        if (AnonymousClass1.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[getCmdType().ordinal()] == 1) {
            this.mLanguageNotification = in.readString();
        }
    }

    public String getLanguageNotification() {
        return this.mLanguageNotification;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        if (AnonymousClass1.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[getCmdType().ordinal()] == 1) {
            dest.writeString(this.mLanguageNotification);
            dest.setDataPosition(0);
        }
    }
}
