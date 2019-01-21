package com.android.internal.telephony.uicc;

import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public final class RuimFileHandler extends IccFileHandler {
    static final String LOG_TAG = "RuimFH";

    public RuimFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        super(app, aid, ci);
    }

    public void loadEFImgTransparent(int fileid, int highOffset, int lowOffset, int length, Message onLoaded) {
        int i = fileid;
        int i2 = i;
        this.mCi.iccIOForApp(192, i2, getEFPath(IccConstants.EF_IMG), 0, 0, 10, null, null, this.mAid, obtainMessage(10, i, 0, onLoaded));
    }

    protected String getEFPath(int efid) {
        if (efid == 28474) {
            return "3F007F10";
        }
        if (efid == IccConstants.EF_CSIM_IMSIM || efid == IccConstants.EF_CSIM_CDMAHOME || efid == IccConstants.EF_CST || efid == 28474 || efid == IccConstants.EF_SMS || efid == 28481 || efid == IccConstants.EF_CSIM_MDN || efid == IccConstants.EF_CSIM_MIPUPP || efid == IccConstants.EF_CSIM_EPRL) {
            return "3F007F25";
        }
        return getCommonIccEFPath(efid);
    }

    protected void logd(String msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[RuimFileHandler] ");
        stringBuilder.append(msg);
        Rlog.d(str, stringBuilder.toString());
    }

    protected void loge(String msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[RuimFileHandler] ");
        stringBuilder.append(msg);
        Rlog.e(str, stringBuilder.toString());
    }
}
