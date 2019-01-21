package com.android.internal.telephony.cdma;

import android.telephony.Rlog;

public class CdmaCallWaitingNotification {
    static final String LOG_TAG = "CdmaCallWaitingNotification";
    public int alertPitch = 0;
    public int isPresent = 0;
    public String name = null;
    public int namePresentation = 0;
    public String number = null;
    public int numberPlan = 0;
    public int numberPresentation = 0;
    public int numberType = 0;
    public int signal = 0;
    public int signalType = 0;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append("Call Waiting Notification   number: ");
        stringBuilder.append(this.number == null ? this.number : this.number.replaceAll("\\d{4}$", "****"));
        stringBuilder.append(" numberPresentation: ");
        stringBuilder.append(this.numberPresentation);
        stringBuilder.append(" name: ");
        stringBuilder.append(this.name);
        stringBuilder.append(" namePresentation: ");
        stringBuilder.append(this.namePresentation);
        stringBuilder.append(" numberType: ");
        stringBuilder.append(this.numberType);
        stringBuilder.append(" numberPlan: ");
        stringBuilder.append(this.numberPlan);
        stringBuilder.append(" isPresent: ");
        stringBuilder.append(this.isPresent);
        stringBuilder.append(" signalType: ");
        stringBuilder.append(this.signalType);
        stringBuilder.append(" alertPitch: ");
        stringBuilder.append(this.alertPitch);
        stringBuilder.append(" signal: ");
        stringBuilder.append(this.signal);
        return stringBuilder.toString();
    }

    public static int presentationFromCLIP(int cli) {
        switch (cli) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            default:
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected presentation ");
                stringBuilder.append(cli);
                Rlog.d(str, stringBuilder.toString());
                return 3;
        }
    }
}
