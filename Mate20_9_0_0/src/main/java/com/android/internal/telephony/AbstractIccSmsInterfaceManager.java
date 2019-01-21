package com.android.internal.telephony;

import android.app.PendingIntent;
import java.util.List;

public abstract class AbstractIccSmsInterfaceManager {
    protected byte[] getNewbyte() {
        return new byte[]{(byte) 0};
    }

    protected int getRecordLength() {
        return -1;
    }

    protected boolean isHwMmsUid(int uid) {
        return false;
    }

    public String getSmscAddr() {
        return null;
    }

    public boolean setSmscAddr(String smscAddr) {
        return false;
    }

    public void authenticateSmsSend(String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, String callingPackage, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod, int uid) {
    }

    public void authenticateSmsSends(String destAddr, String scAddr, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, String callingPackage, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod, int uid) {
    }

    public boolean setCellBroadcastRangeList(int[] messageIds, int ranType) {
        return false;
    }
}
