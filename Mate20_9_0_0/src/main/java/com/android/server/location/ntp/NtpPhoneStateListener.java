package com.android.server.location.ntp;

import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.util.Log;

public class NtpPhoneStateListener extends PhoneStateListener {
    private static boolean DBG = true;
    private static final String TAG = "NtpPhoneStateListener";
    private boolean mIsCdma = false;

    public NtpPhoneStateListener(int subId) {
        super(Integer.valueOf(subId));
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NtpPhoneStateListener create subId:");
            stringBuilder.append(subId);
            Log.d(str, stringBuilder.toString());
        }
    }

    public void onServiceStateChanged(ServiceState state) {
        if (state != null) {
            this.mIsCdma = ServiceState.isCdma(state.getRilVoiceRadioTechnology());
            if (DBG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onServiceStateChanged subId:");
                stringBuilder.append(this.mSubId);
                stringBuilder.append(" isCdma=");
                stringBuilder.append(this.mIsCdma);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    public boolean isCdma() {
        return this.mIsCdma;
    }
}
