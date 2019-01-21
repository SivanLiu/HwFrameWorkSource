package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public final class IsimFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "IsimFH";

    public IsimFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        super(app, aid, ci);
    }

    protected String getEFPath(int efid) {
        if (!(efid == IccConstants.EF_IST || efid == IccConstants.EF_PCSCF)) {
            switch (efid) {
                case IccConstants.EF_IMPI /*28418*/:
                case IccConstants.EF_DOMAIN /*28419*/:
                case IccConstants.EF_IMPU /*28420*/:
                    break;
                default:
                    return getCommonIccEFPath(efid);
            }
        }
        return "3F007FFF";
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
