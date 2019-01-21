package com.android.internal.telephony;

import android.content.Context;
import android.os.Bundle;
import android.provider.BlockedNumberContract.SystemContract;
import android.telephony.Rlog;

public class BlockChecker {
    private static final String TAG = "BlockChecker";
    private static final boolean VDBG = false;

    @Deprecated
    public static boolean isBlocked(Context context, String phoneNumber) {
        return isBlocked(context, phoneNumber, null);
    }

    public static boolean isBlocked(Context context, String phoneNumber, Bundle extras) {
        String str;
        StringBuilder stringBuilder;
        boolean isBlocked = false;
        long startTimeNano = System.nanoTime();
        try {
            if (SystemContract.shouldSystemBlockNumber(context, phoneNumber, extras)) {
                Rlog.d(TAG, "phone number is blocked.");
                isBlocked = true;
            }
        } catch (Exception e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception checking for blocked number: ");
            stringBuilder.append(e);
            Rlog.e(str, stringBuilder.toString());
        }
        int durationMillis = (int) ((System.nanoTime() - startTimeNano) / 1000000);
        if (durationMillis > 500) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Blocked number lookup took: ");
            stringBuilder.append(durationMillis);
            stringBuilder.append(" ms.");
            Rlog.d(str, stringBuilder.toString());
        }
        return isBlocked;
    }
}
