package com.android.server.security.tsmagent.logic.ese;

import android.content.Context;
import com.android.server.security.tsmagent.utils.HexByteHelper;
import com.android.server.security.tsmagent.utils.HwLog;
import com.leisen.wallet.sdk.tsm.TSMOperator;
import com.leisen.wallet.sdk.tsm.TSMOperatorResponse;

public class ESEInfoManager implements TSMOperatorResponse {
    private static final String AMSD_AID = "A000000151000000";
    private static ESEInfoManager instance;
    private static final Object serviceLock = new Object();
    private String cplc;
    private Context mContext;

    public static ESEInfoManager getInstance(Context context) {
        synchronized (serviceLock) {
            if (instance == null) {
                instance = new ESEInfoManager(context);
            }
        }
        return instance;
    }

    private ESEInfoManager(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public void onOperSuccess(String response) {
        this.cplc = "";
        try {
            this.cplc = response.substring(6, (HexByteHelper.hexStringToDecimalInteger(response.substring(4, 6)) * 2) + 6);
        } catch (Exception e) {
            HwLog.d("queryCplc err");
        }
    }

    public void onOperFailure(int result, Error e) {
        this.cplc = null;
    }

    public String queryCplc() {
        String str;
        synchronized (serviceLock) {
            this.cplc = null;
            TSMOperator tsmOperator = TSMOperator.getInstance(this.mContext, null);
            tsmOperator.setTsmOperatorResponse(this);
            tsmOperator.getCPLC(AMSD_AID);
            tsmOperator.setTsmOperatorResponse(null);
            str = this.cplc;
        }
        return str;
    }
}
