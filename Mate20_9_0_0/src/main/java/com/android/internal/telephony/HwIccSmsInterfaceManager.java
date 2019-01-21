package com.android.internal.telephony;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hsm.HwSystemManager.Notifier;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.telephony.uicc.IccFileHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HwIccSmsInterfaceManager extends IccSmsInterfaceManager {
    public static final int AUTHENTICATE_RESULT_ALLOW = 0;
    public static final int AUTHENTICATE_RESULT_ALLOW_FOREVER = 1;
    public static final int AUTHENTICATE_RESULT_DISALLOW = 2;
    public static final int AUTHENTICATE_RESULT_DISALLOW_FOREVER = 3;
    private static final int CB_RANGE_START_END_STEP = 2;
    protected static final boolean DBG = true;
    private static final int EVENT_GET_SMSC_DONE = 101;
    private static final int EVENT_SET_SMSC_DONE = 102;
    protected static final String LOG_TAG = "HwIccSmsInterfaceManager";
    protected Handler mHwHandler = new Handler() {
        public void handleMessage(Message msg) {
            boolean z = false;
            AsyncResult ar;
            HwIccSmsInterfaceManager hwIccSmsInterfaceManager;
            switch (msg.what) {
                case HwIccSmsInterfaceManager.EVENT_GET_SMSC_DONE /*101*/:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        HwIccSmsInterfaceManager.this.smscAddr = null;
                    } else {
                        HwIccSmsInterfaceManager.this.smscAddr = (String) ar.result;
                    }
                    synchronized (HwIccSmsInterfaceManager.this.mLock) {
                        hwIccSmsInterfaceManager = HwIccSmsInterfaceManager.this;
                        if (ar.exception == null) {
                            z = true;
                        }
                        hwIccSmsInterfaceManager.mSuccess = z;
                        HwIccSmsInterfaceManager.this.mLock.notifyAll();
                    }
                    return;
                case HwIccSmsInterfaceManager.EVENT_SET_SMSC_DONE /*102*/:
                    ar = msg.obj;
                    synchronized (HwIccSmsInterfaceManager.this.mLock) {
                        hwIccSmsInterfaceManager = HwIccSmsInterfaceManager.this;
                        if (ar.exception == null) {
                            z = true;
                        }
                        hwIccSmsInterfaceManager.mSuccess = z;
                        HwIccSmsInterfaceManager.this.mLock.notifyAll();
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    };
    private MyCallback mNotifer = new MyCallback();
    private Map<Integer, SmsSendInfo> mSmsSendInfo = new HashMap();
    private int mWaitSendSmsId = 0;
    private String smscAddr;

    private class MyCallback implements Notifier {
        public int notifyResult(Bundle data) {
            Bundle bundle = data;
            if (bundle == null) {
                return 0;
            }
            int i;
            ArrayList<Integer> smsIds = bundle.getIntegerArrayList("sms_id");
            ArrayList<Integer> results = bundle.getIntegerArrayList("authenticate_result");
            ArrayList<Integer> arrayList;
            ArrayList<Integer> arrayList2;
            if (smsIds == null) {
                i = 0;
                arrayList = smsIds;
                arrayList2 = results;
            } else if (results == null) {
                i = 0;
                arrayList = smsIds;
                arrayList2 = results;
            } else {
                int size;
                int size2 = smsIds.size();
                int i2 = 0;
                while (i2 < size2) {
                    int result = ((Integer) results.get(i2)).intValue();
                    int smsId = ((Integer) smsIds.get(i2)).intValue();
                    HwIccSmsInterfaceManager hwIccSmsInterfaceManager = HwIccSmsInterfaceManager.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sms: id ");
                    stringBuilder.append(smsId);
                    stringBuilder.append(" result ");
                    stringBuilder.append(result);
                    hwIccSmsInterfaceManager.log(stringBuilder.toString());
                    SmsSendInfo smsInfo = (SmsSendInfo) HwIccSmsInterfaceManager.this.mSmsSendInfo.get(Integer.valueOf(smsId));
                    if (smsInfo == null) {
                        arrayList = smsIds;
                        arrayList2 = results;
                        size = size2;
                    } else {
                        if (!HwIccSmsInterfaceManager.this.isAuthAllow(result)) {
                            arrayList = smsIds;
                            arrayList2 = results;
                            size = size2;
                            HwIccSmsInterfaceManager.this.log("Auth DISALLOW Direct return failure");
                            if (smsInfo.isSinglepart) {
                                HwIccSmsInterfaceManager.this.sendErrorInPendingIntent(smsInfo.mSentIntent, 1);
                            } else {
                                HwIccSmsInterfaceManager.this.sendErrorInPendingIntents(smsInfo.mSentIntents, 1);
                            }
                        } else if (smsInfo.isSinglepart) {
                            arrayList = smsIds;
                            arrayList2 = results;
                            size = size2;
                            HwIccSmsInterfaceManager.this.mDispatchersController.sendText(smsInfo.mDestAddr, smsInfo.mScAddr, smsInfo.mText, smsInfo.mSentIntent, smsInfo.mDeliveryIntent, null, smsInfo.mCallingPackage, smsInfo.mPersistMessageForNonDefaultSmsApp, smsInfo.mPriority, smsInfo.mExpectMore, smsInfo.mValidityPeriod);
                        } else {
                            arrayList = smsIds;
                            arrayList2 = results;
                            size = size2;
                            HwIccSmsInterfaceManager.this.sendMultipartTextAfterAuth(smsInfo.mDestAddr, smsInfo.mScAddr, smsInfo.mParts, smsInfo.mSentIntents, smsInfo.mDeliveryIntents, smsInfo.mCallingPackage, smsInfo.mPersistMessageForNonDefaultSmsApp, smsInfo.mPriority, smsInfo.mExpectMore, smsInfo.mValidityPeriod);
                        }
                        HwIccSmsInterfaceManager.this.mSmsSendInfo.remove(Integer.valueOf(smsId));
                    }
                    i2++;
                    smsIds = arrayList;
                    results = arrayList2;
                    size2 = size;
                    bundle = data;
                }
                arrayList2 = results;
                size = size2;
                return 0;
            }
            return i;
        }
    }

    public static class SmsSendInfo {
        public boolean isSinglepart;
        public String mCallingPackage;
        public PendingIntent mDeliveryIntent;
        public List<PendingIntent> mDeliveryIntents;
        public String mDestAddr;
        public boolean mExpectMore;
        public List<String> mParts;
        public boolean mPersistMessageForNonDefaultSmsApp;
        public int mPriority;
        public String mScAddr;
        public PendingIntent mSentIntent;
        public List<PendingIntent> mSentIntents;
        public String mText;
        public int mValidityPeriod;

        private SmsSendInfo(String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, String callingPackage, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod) {
            this.isSinglepart = true;
            this.mDestAddr = destAddr;
            this.mScAddr = scAddr;
            this.mText = text;
            this.mSentIntent = sentIntent;
            this.mDeliveryIntent = deliveryIntent;
            this.mCallingPackage = callingPackage;
            this.mPersistMessageForNonDefaultSmsApp = persistMessageForNonDefaultSmsApp;
            this.mPriority = priority;
            this.mExpectMore = expectMore;
            this.mValidityPeriod = validityPeriod;
        }

        private SmsSendInfo(String destAddr, String scAddr, List<String> parts, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, String callingPackage, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod) {
            this.isSinglepart = false;
            this.mDestAddr = destAddr;
            this.mScAddr = scAddr;
            this.mParts = parts;
            this.mSentIntents = sentIntents;
            this.mDeliveryIntents = deliveryIntents;
            this.mCallingPackage = callingPackage;
            this.mPersistMessageForNonDefaultSmsApp = persistMessageForNonDefaultSmsApp;
            this.mPriority = priority;
            this.mExpectMore = expectMore;
            this.mValidityPeriod = validityPeriod;
        }
    }

    public void authenticateSmsSend(String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, String callingPackage, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod, int uid) {
        SmsSendInfo smsSendInfo = new SmsSendInfo(destAddr, scAddr, text, sentIntent, deliveryIntent, callingPackage, persistMessageForNonDefaultSmsApp, priority, expectMore, validityPeriod, null);
        synchronized (this.mSmsSendInfo) {
            try {
                this.mWaitSendSmsId++;
                this.mSmsSendInfo.put(Integer.valueOf(this.mWaitSendSmsId), smsSendInfo);
            } finally {
                String str = destAddr;
                String str2 = text;
                int i = uid;
                while (true) {
                }
            }
        }
        log("sendTextInternal go to authenticate");
        MyCallback myCallback = this.mNotifer;
        int i2 = this.mWaitSendSmsId;
    }

    public void authenticateSmsSends(String destAddr, String scAddr, List<String> parts, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, String callingPackage, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod, int uid) {
        SmsSendInfo smsSendInfo = new SmsSendInfo(destAddr, scAddr, (List) parts, (List) sentIntents, (List) deliveryIntents, callingPackage, persistMessageForNonDefaultSmsApp, priority, expectMore, validityPeriod, null);
        synchronized (this.mSmsSendInfo) {
            try {
                this.mWaitSendSmsId++;
                this.mSmsSendInfo.put(Integer.valueOf(this.mWaitSendSmsId), smsSendInfo);
            } finally {
                String str = destAddr;
                int i = uid;
                while (true) {
                }
            }
        }
        StringBuffer sb = new StringBuffer();
        for (String part : parts) {
            sb.append(part);
        }
        log("sendMultipartText go to authenticate");
        MyCallback myCallback = this.mNotifer;
        int i2 = this.mWaitSendSmsId;
        String stringBuffer = sb.toString();
    }

    private boolean isAuthAllow(int result) {
        if (result == 0 || 1 == result) {
            return true;
        }
        if (2 == result || 3 == result) {
            return false;
        }
        log("invalid auth result");
        return false;
    }

    private void sendErrorInPendingIntent(PendingIntent intent, int errorCode) {
        if (intent != null) {
            try {
                intent.send(errorCode);
            } catch (CanceledException e) {
                log("fail in send error pendingintent");
            }
        }
    }

    private void sendErrorInPendingIntents(List<PendingIntent> intents, int errorCode) {
        for (PendingIntent intent : intents) {
            sendErrorInPendingIntent(intent, errorCode);
        }
    }

    public HwIccSmsInterfaceManager(Phone phone) {
        super(phone);
    }

    protected byte[] getNewbyte() {
        if (2 == this.mPhone.getPhoneType()) {
            return new byte[HwSubscriptionManager.SUB_INIT_STATE];
        }
        return new byte[176];
    }

    protected int getRecordLength() {
        if (2 == this.mPhone.getPhoneType()) {
            return HwSubscriptionManager.SUB_INIT_STATE;
        }
        return 176;
    }

    protected IccFileHandler getIccFileHandler() {
        return this.mPhone.getIccFileHandler();
    }

    public String getSmscAddr() {
        log("getSmscAddress()");
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message response = this.mHwHandler.obtainMessage(EVENT_GET_SMSC_DONE);
            if (getIccFileHandler() == null) {
                return null;
            }
            getIccFileHandler().getSmscAddress(response);
            boolean isWait = true;
            while (isWait) {
                try {
                    this.mLock.wait();
                    isWait = false;
                } catch (InterruptedException e) {
                    log("interrupted while trying to update by index");
                    return this.smscAddr;
                }
            }
        }
    }

    public boolean setSmscAddr(String smscAddr) {
        log("setSmscAddr() ");
        this.mPhone.getContext().enforceCallingOrSelfPermission("huawei.permission.SET_SMSC_ADDRESS", "Requires Set Smsc Address permission");
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message response = this.mHwHandler.obtainMessage(EVENT_SET_SMSC_DONE);
            boolean z;
            if (getIccFileHandler() == null) {
                z = this.mSuccess;
                return z;
            }
            getIccFileHandler().setSmscAddress(smscAddr, response);
            z = true;
            while (z) {
                try {
                    this.mLock.wait();
                    z = false;
                } catch (InterruptedException e) {
                    log("interrupted while trying to update by index");
                    return this.mSuccess;
                }
            }
        }
    }

    protected boolean isHwMmsUid(int uid) {
        Log.d("XXXXXX", "HwIccSmsInterfaceManager isHwMmsUid begin");
        String HWMMS_PKG = "com.huawei.message";
        int mmsUid = -1;
        try {
            mmsUid = this.mContext.getPackageManager().getPackageUid("com.huawei.message", UserHandle.getUserId(uid));
        } catch (NameNotFoundException e) {
        }
        return mmsUid == uid;
    }

    public boolean setCellBroadcastRangeList(int[] messageIds, int ranType) {
        if (ranType == 0) {
            return setGsmBroadcastRangeList(messageIds);
        }
        if (ranType == 1) {
            return setCdmaBroadcastRangeList(messageIds);
        }
        throw new IllegalArgumentException("Not a supported RAN Type");
    }

    public synchronized boolean setGsmBroadcastRangeList(int[] messageIds) {
        this.mContext.enforceCallingPermission("android.permission.RECEIVE_SMS", "Enabling or disabling cell broadcast SMS");
        String client = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        getCellBroadcastRangeManager().startUpdate();
        int len = messageIds.length;
        for (int i = 0; i < len; i += 2) {
            getCellBroadcastRangeManager().addRange(messageIds[i], messageIds[i + 1], true);
        }
        boolean z = false;
        StringBuilder stringBuilder;
        if (getCellBroadcastRangeManager().finishUpdate()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Succeed to set GSM cell broadcast subscription for MID range ");
            stringBuilder.append(printForMessageIds(messageIds));
            stringBuilder.append(" from client ");
            stringBuilder.append(client);
            log(stringBuilder.toString());
            if (messageIds.length != 0) {
                z = true;
            }
            setCellBroadcastActivationHw(z);
            return true;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to set GSM cell broadcast subscription for MID range ");
        stringBuilder.append(printForMessageIds(messageIds));
        stringBuilder.append(" from client ");
        stringBuilder.append(client);
        log(stringBuilder.toString());
        return false;
    }

    public synchronized boolean setCdmaBroadcastRangeList(int[] messageIds) {
        this.mContext.enforceCallingPermission("android.permission.RECEIVE_SMS", "Enabling or disabling cdma broadcast SMS");
        String client = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        getCdmaBroadcastRangeManager().startUpdate();
        int len = messageIds.length;
        for (int i = 0; i < len; i += 2) {
            getCdmaBroadcastRangeManager().addRange(messageIds[i], messageIds[i + 1], true);
        }
        boolean z = false;
        StringBuilder stringBuilder;
        if (getCdmaBroadcastRangeManager().finishUpdate()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Succeed to set cdma broadcast subscription for MID range ");
            stringBuilder.append(printForMessageIds(messageIds));
            stringBuilder.append(" from client ");
            stringBuilder.append(client);
            log(stringBuilder.toString());
            if (messageIds.length != 0) {
                z = true;
            }
            setCdmaBroadcastActivationHw(z);
            return true;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to set cdma broadcast subscription for MID range ");
        stringBuilder.append(printForMessageIds(messageIds));
        stringBuilder.append(" from client ");
        stringBuilder.append(client);
        log(stringBuilder.toString());
        return false;
    }

    private String printForMessageIds(int[] messageIds) {
        StringBuilder stringBuilder = new StringBuilder();
        if (messageIds != null && messageIds.length % 2 == 0) {
            int len = messageIds.length;
            for (int i = 0; i < len; i += 2) {
                stringBuilder.append(messageIds[i]);
                stringBuilder.append("-");
                stringBuilder.append(messageIds[i + 1]);
                stringBuilder.append(" ");
            }
        }
        return stringBuilder.toString();
    }
}
