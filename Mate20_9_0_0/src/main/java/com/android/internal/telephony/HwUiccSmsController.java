package com.android.internal.telephony;

import android.telephony.Rlog;

public class HwUiccSmsController extends UiccSmsController {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "HwUiccSmsController";
    private static UiccSmsControllerUtils utils = new UiccSmsControllerUtils();

    public HwUiccSmsController(Phone[] phone) {
    }

    public String getSmscAddr() {
        return getSmscAddrForSubscriber((long) getPreferredSmsSubscription());
    }

    public String getSmscAddrForSubscriber(long subId) {
        IccSmsInterfaceManager iccSmsIntMgr = utils.getIccSmsInterfaceManager(this, (int) subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.getSmscAddr();
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getSmscAddr iccSmsIntMgr is null for Subscription: ");
        stringBuilder.append(subId);
        Rlog.e(str, stringBuilder.toString());
        return null;
    }

    public boolean setSmscAddr(String smscAddr) {
        return setSmscAddrForSubscriber((long) getPreferredSmsSubscription(), smscAddr);
    }

    public boolean setSmscAddrForSubscriber(long subId, String smscAddr) {
        IccSmsInterfaceManager iccSmsIntMgr = utils.getIccSmsInterfaceManager(this, (int) subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.setSmscAddr(smscAddr);
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setSmscAddr iccSmsIntMgr is null for Subscription: ");
        stringBuilder.append(subId);
        Rlog.e(str, stringBuilder.toString());
        return false;
    }

    public boolean setCellBroadcastRangeListForSubscriber(int subId, int[] messageIds, int ranType) {
        IccSmsInterfaceManager iccSmsIntMgr = utils.getIccSmsInterfaceManager(this, subId);
        if (iccSmsIntMgr != null) {
            return iccSmsIntMgr.setCellBroadcastRangeList(messageIds, ranType);
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCellBroadcastRangeListForSubscriber iccSmsIntMgr is null for Subscription: ");
        stringBuilder.append(subId);
        Rlog.e(str, stringBuilder.toString());
        return false;
    }
}
