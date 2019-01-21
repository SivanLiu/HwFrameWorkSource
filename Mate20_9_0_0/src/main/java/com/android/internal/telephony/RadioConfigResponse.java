package com.android.internal.telephony;

import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.config.V1_0.IRadioConfigResponse.Stub;
import android.hardware.radio.config.V1_0.SimSlotStatus;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.IccSlotStatus;
import java.util.ArrayList;

public class RadioConfigResponse extends Stub {
    private static final String TAG = "RadioConfigResponse";
    private final RadioConfig mRadioConfig;

    public RadioConfigResponse(RadioConfig radioConfig) {
        this.mRadioConfig = radioConfig;
    }

    public void getSimSlotsStatusResponse(RadioResponseInfo responseInfo, ArrayList<SimSlotStatus> slotStatus) {
        RILRequest rr = this.mRadioConfig.processResponse(responseInfo);
        if (rr != null) {
            ArrayList<IccSlotStatus> ret = RadioConfig.convertHalSlotStatus(slotStatus);
            String str;
            StringBuilder stringBuilder;
            RadioConfig radioConfig;
            if (responseInfo.error == 0) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(rr.serialString());
                stringBuilder.append("< ");
                radioConfig = this.mRadioConfig;
                stringBuilder.append(RadioConfig.requestToString(rr.mRequest));
                stringBuilder.append(" ");
                stringBuilder.append(ret.toString());
                Rlog.d(str, stringBuilder.toString());
                return;
            }
            rr.onError(responseInfo.error, ret);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(rr.serialString());
            stringBuilder.append("< ");
            radioConfig = this.mRadioConfig;
            stringBuilder.append(RadioConfig.requestToString(rr.mRequest));
            stringBuilder.append(" error ");
            stringBuilder.append(responseInfo.error);
            Rlog.e(str, stringBuilder.toString());
            return;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getSimSlotsStatusResponse: Error ");
        stringBuilder2.append(responseInfo.toString());
        Rlog.e(str2, stringBuilder2.toString());
    }

    public void setSimSlotsMappingResponse(RadioResponseInfo responseInfo) {
        RILRequest rr = this.mRadioConfig.processResponse(responseInfo);
        String str;
        StringBuilder stringBuilder;
        RadioConfig radioConfig;
        if (rr == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setSimSlotsMappingResponse: Error ");
            stringBuilder.append(responseInfo.toString());
            Rlog.e(str, stringBuilder.toString());
        } else if (responseInfo.error == 0) {
            RadioResponse.sendMessageResponse(rr.mResult, null);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(rr.serialString());
            stringBuilder.append("< ");
            radioConfig = this.mRadioConfig;
            stringBuilder.append(RadioConfig.requestToString(rr.mRequest));
            Rlog.d(str, stringBuilder.toString());
        } else {
            rr.onError(responseInfo.error, null);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(rr.serialString());
            stringBuilder.append("< ");
            radioConfig = this.mRadioConfig;
            stringBuilder.append(RadioConfig.requestToString(rr.mRequest));
            stringBuilder.append(" error ");
            stringBuilder.append(responseInfo.error);
            Rlog.e(str, stringBuilder.toString());
        }
    }
}
