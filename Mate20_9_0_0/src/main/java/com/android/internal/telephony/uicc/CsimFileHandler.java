package com.android.internal.telephony.uicc;

import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public final class CsimFileHandler extends IccFileHandler implements IccConstants {
    static final String LOG_TAG = "CsimFH";

    public CsimFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        super(app, aid, ci);
    }

    protected String getEFPath(int efid) {
        if (!(efid == IccConstants.EF_CSIM_IMSIM || efid == IccConstants.EF_CSIM_CDMAHOME || efid == IccConstants.EF_CST || efid == IccConstants.EF_CSIM_MDN || efid == IccConstants.EF_CSIM_MIPUPP || efid == IccConstants.EF_CSIM_EPRL)) {
            switch (efid) {
                case 28474:
                    return "3F007F10";
                case IccConstants.EF_FDN /*28475*/:
                case IccConstants.EF_SMS /*28476*/:
                    break;
                default:
                    switch (efid) {
                        case IccConstants.EF_MSISDN /*28480*/:
                        case 28481:
                            break;
                        default:
                            String path = getCommonIccEFPath(efid);
                            if (path == null) {
                                return "3F007F105F3A";
                            }
                            return path;
                    }
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
