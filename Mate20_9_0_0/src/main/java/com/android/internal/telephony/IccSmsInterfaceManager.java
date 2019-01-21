package com.android.internal.telephony;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.hsm.HwSystemManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserManager;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.MultiSimVariants;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.util.HexDump;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class IccSmsInterfaceManager extends AbstractIccSmsInterfaceManager {
    private static final byte BYTE_LOW_HALF_BIT_MASK = (byte) 15;
    private static final byte BYTE_UIM_SUPPORT_MEID_BIT_MASK = (byte) 3;
    private static final byte CDMA_SERVICE_TABLE_BYTES = (byte) 3;
    private static final boolean CHINA_RELEASE_VERSION = "CN".equalsIgnoreCase(SystemProperties.get("ro.product.locale.region", ""));
    static final boolean DBG = true;
    private static final byte ESN_ME_NUM_BYTES = (byte) 4;
    private static final int EVENT_GET_MEID_OR_PESN_DONE = 103;
    private static final int EVENT_GET_UIM_SUPPORT_MEID_DONE = 105;
    private static final int EVENT_LOAD_DONE = 1;
    protected static final int EVENT_SET_BROADCAST_ACTIVATION_DONE = 3;
    protected static final int EVENT_SET_BROADCAST_CONFIG_DONE = 4;
    private static final int EVENT_SET_MEID_OR_PESN_DONE = 104;
    private static final int EVENT_UPDATE_DONE = 2;
    static final String LOG_TAG = "IccSmsInterfaceManager";
    private static boolean LONG_SMS_SEND_DELAY_RELEASE = HwModemCapability.isCapabilitySupport(17);
    private static final byte MEID_ME_NUM_BYTES = (byte) 7;
    private static final String MMS_PACKAGE_NAME = "com.android.mms";
    private static final int SMS_CB_CODE_SCHEME_MAX = 255;
    private static final int SMS_CB_CODE_SCHEME_MIN = 0;
    public static final int SMS_MESSAGE_PERIOD_NOT_SPECIFIED = -1;
    public static final int SMS_MESSAGE_PRIORITY_NOT_SPECIFIED = -1;
    protected final AppOpsManager mAppOps;
    private CdmaBroadcastRangeManager mCdmaBroadcastRangeManager;
    private CellBroadcastRangeManager mCellBroadcastRangeManager;
    protected final Context mContext;
    protected SmsDispatchersController mDispatchersController;
    protected Handler mHandler;
    private boolean mIsUimSupportMeid;
    protected final Object mLock;
    private String mMeidOrEsn;
    protected Phone mPhone;
    private List<SmsRawData> mSms;
    protected boolean mSuccess;
    private final UserManager mUserManager;

    class CdmaBroadcastRangeManager extends IntRangeManager {
        private ArrayList<CdmaSmsBroadcastConfigInfo> mConfigList = new ArrayList();

        CdmaBroadcastRangeManager() {
        }

        protected void startUpdate() {
            this.mConfigList.clear();
        }

        protected void addRange(int startId, int endId, boolean selected) {
            this.mConfigList.add(new CdmaSmsBroadcastConfigInfo(startId, endId, 1, selected));
        }

        protected boolean finishUpdate() {
            if (this.mConfigList.isEmpty()) {
                return true;
            }
            return IccSmsInterfaceManager.this.setCdmaBroadcastConfig((CdmaSmsBroadcastConfigInfo[]) this.mConfigList.toArray(new CdmaSmsBroadcastConfigInfo[this.mConfigList.size()]));
        }
    }

    class CellBroadcastRangeManager extends IntRangeManager {
        private ArrayList<SmsBroadcastConfigInfo> mConfigList = new ArrayList();

        CellBroadcastRangeManager() {
        }

        protected void startUpdate() {
            this.mConfigList.clear();
        }

        protected void addRange(int startId, int endId, boolean selected) {
            this.mConfigList.add(new SmsBroadcastConfigInfo(startId, endId, 0, 255, selected));
        }

        protected boolean finishUpdate() {
            if (this.mConfigList.isEmpty()) {
                return true;
            }
            return IccSmsInterfaceManager.this.setCellBroadcastConfig((SmsBroadcastConfigInfo[]) this.mConfigList.toArray(new SmsBroadcastConfigInfo[this.mConfigList.size()]));
        }
    }

    public boolean isUimSupportMeid() {
        this.mPhone.getContext().enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", "Is Uim Support Meid");
        log("isUimSupportMeid entry");
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message response = this.mHandler.obtainMessage(105);
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh != null) {
                fh.isUimSupportMeidValue(response);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    log("interrupted while trying to update by index");
                }
            } else {
                log("getIccFileHandler() is null, need return");
                return false;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mIsUimSupportMeid ret: ");
        stringBuilder.append(this.mIsUimSupportMeid);
        log(stringBuilder.toString());
        return this.mIsUimSupportMeid;
    }

    public String getMeidOrPesn() {
        this.mPhone.getContext().enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", "Is Uim Support Meid");
        log("getMeidOrPesn entry");
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message response = this.mHandler.obtainMessage(EVENT_GET_MEID_OR_PESN_DONE);
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh != null) {
                fh.getMeidOrPesnValue(response);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    log("interrupted while trying to getMeidOrPesn");
                }
            } else {
                log("getIccFileHandler() is null, need return");
                return null;
            }
        }
        return this.mMeidOrEsn;
    }

    public boolean setMeidOrPesn(String meid, String pesn) {
        this.mPhone.getContext().enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", "Is Uim Support Meid");
        log("setMeidOrPesn entry ");
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message response = this.mHandler.obtainMessage(104);
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh != null) {
                fh.setMeidOrPesnValue(meid, pesn, response);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    log("interrupted while trying to setMeidOrPesn");
                }
            } else {
                log("getIccFileHandler() is null, need return");
                boolean z = this.mSuccess;
                return z;
            }
        }
        return this.mSuccess;
    }

    protected IccSmsInterfaceManager(Phone phone) {
        this(phone, phone.getContext(), (AppOpsManager) phone.getContext().getSystemService("appops"), (UserManager) phone.getContext().getSystemService("user"), new SmsDispatchersController(phone, phone.mSmsStorageMonitor, phone.mSmsUsageMonitor));
    }

    @VisibleForTesting
    public IccSmsInterfaceManager(Phone phone, Context context, AppOpsManager appOps, UserManager userManager, SmsDispatchersController dispatchersController) {
        this.mLock = new Object();
        this.mCellBroadcastRangeManager = new CellBroadcastRangeManager();
        this.mCdmaBroadcastRangeManager = new CdmaBroadcastRangeManager();
        this.mMeidOrEsn = null;
        this.mIsUimSupportMeid = false;
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                int i = msg.what;
                boolean z = true;
                AsyncResult ar;
                IccSmsInterfaceManager iccSmsInterfaceManager;
                IccSmsInterfaceManager iccSmsInterfaceManager2;
                switch (i) {
                    case 1:
                        ar = (AsyncResult) msg.obj;
                        synchronized (IccSmsInterfaceManager.this.mLock) {
                            if (ar.exception == null) {
                                IccSmsInterfaceManager.this.mSms = IccSmsInterfaceManager.this.buildValidRawData((ArrayList) ar.result);
                                IccSmsInterfaceManager.this.markMessagesAsRead((ArrayList) ar.result);
                            } else {
                                if (Rlog.isLoggable("SMS", 3)) {
                                    IccSmsInterfaceManager.this.log("Cannot load Sms records");
                                }
                                IccSmsInterfaceManager.this.mSms = null;
                            }
                            IccSmsInterfaceManager.this.mLock.notifyAll();
                        }
                        return;
                    case 2:
                        ar = (AsyncResult) msg.obj;
                        synchronized (IccSmsInterfaceManager.this.mLock) {
                            iccSmsInterfaceManager = IccSmsInterfaceManager.this;
                            if (ar.exception != null) {
                                z = false;
                            }
                            iccSmsInterfaceManager.mSuccess = z;
                            IccSmsInterfaceManager.this.mLock.notifyAll();
                        }
                        return;
                    case 3:
                    case 4:
                        ar = (AsyncResult) msg.obj;
                        synchronized (IccSmsInterfaceManager.this.mLock) {
                            iccSmsInterfaceManager2 = IccSmsInterfaceManager.this;
                            if (ar.exception != null) {
                                z = false;
                            }
                            iccSmsInterfaceManager2.mSuccess = z;
                            IccSmsInterfaceManager.this.mLock.notifyAll();
                        }
                        return;
                    default:
                        switch (i) {
                            case IccSmsInterfaceManager.EVENT_GET_MEID_OR_PESN_DONE /*103*/:
                                IccSmsInterfaceManager.this.log("EVENT_GET_MEID_OR_PESN_DONE entry");
                                ar = (AsyncResult) msg.obj;
                                boolean bResult = ar.exception == null;
                                if (bResult) {
                                    IccIoResult ret = ar.result;
                                    if (ret == null || !ret.success()) {
                                        IccSmsInterfaceManager.this.log("else can not get meid or pesn");
                                        bResult = false;
                                    } else {
                                        byte[] uimDeviceId = ret.payload;
                                        if (4 == (uimDeviceId[0] & 15)) {
                                            IccSmsInterfaceManager.this.mMeidOrEsn = IccSmsInterfaceManager.this.bytesToHexString(uimDeviceId, 1, 4);
                                        } else if (7 == (uimDeviceId[0] & 15)) {
                                            IccSmsInterfaceManager.this.mMeidOrEsn = IccSmsInterfaceManager.this.bytesToHexString(uimDeviceId, 1, 7);
                                        }
                                    }
                                } else {
                                    IccSmsInterfaceManager.this.mMeidOrEsn = null;
                                }
                                synchronized (IccSmsInterfaceManager.this.mLock) {
                                    IccSmsInterfaceManager.this.mSuccess = bResult;
                                    IccSmsInterfaceManager.this.mLock.notifyAll();
                                }
                                return;
                            case 104:
                                IccSmsInterfaceManager.this.log("EVENT_SET_MEID_OR_PESN_DONE entry");
                                ar = (AsyncResult) msg.obj;
                                synchronized (IccSmsInterfaceManager.this.mLock) {
                                    iccSmsInterfaceManager = IccSmsInterfaceManager.this;
                                    if (ar.exception != null) {
                                        z = false;
                                    }
                                    iccSmsInterfaceManager.mSuccess = z;
                                    IccSmsInterfaceManager.this.mLock.notifyAll();
                                }
                                return;
                            case 105:
                                IccSmsInterfaceManager.this.log("EVENT_GET_UIM_SUPPORT_MEID_DONE entry");
                                ar = msg.obj;
                                boolean bResult2 = ar.exception == null;
                                if (bResult2) {
                                    IccIoResult ret2 = ar.result;
                                    if (ret2 != null && ret2.success()) {
                                        byte[] uimDeviceId2 = ret2.payload;
                                        if (3 != uimDeviceId2.length) {
                                            IccSmsInterfaceManager.this.mIsUimSupportMeid = false;
                                        } else if (3 == (uimDeviceId2[2] & 3)) {
                                            IccSmsInterfaceManager.this.mIsUimSupportMeid = true;
                                        }
                                    }
                                } else {
                                    IccSmsInterfaceManager.this.mIsUimSupportMeid = false;
                                }
                                iccSmsInterfaceManager2 = IccSmsInterfaceManager.this;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("mIsUimSupportMeid ");
                                stringBuilder.append(IccSmsInterfaceManager.this.mIsUimSupportMeid);
                                iccSmsInterfaceManager2.log(stringBuilder.toString());
                                synchronized (IccSmsInterfaceManager.this.mLock) {
                                    IccSmsInterfaceManager.this.mSuccess = bResult2;
                                    IccSmsInterfaceManager.this.mLock.notifyAll();
                                }
                                return;
                            default:
                                return;
                        }
                }
            }
        };
        this.mPhone = phone;
        this.mContext = context;
        this.mAppOps = appOps;
        this.mUserManager = userManager;
        this.mDispatchersController = dispatchersController;
    }

    protected void markMessagesAsRead(ArrayList<byte[]> messages) {
        ArrayList<byte[]> arrayList = messages;
        if (arrayList != null) {
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh == null) {
                if (Rlog.isLoggable("SMS", 3)) {
                    log("markMessagesAsRead - aborting, no icc card present.");
                }
                return;
            }
            int count = messages.size();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 < count) {
                    byte[] ba = (byte[]) arrayList.get(i2);
                    if (ba[0] == (byte) 3) {
                        int n = ba.length;
                        byte[] nba = new byte[(n - 1)];
                        System.arraycopy(ba, 1, nba, 0, n - 1);
                        fh.updateEFLinearFixed(IccConstants.EF_SMS, i2 + 1, makeSmsRecordData(1, nba), null, null);
                        if (Rlog.isLoggable("SMS", 3)) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("SMS ");
                            stringBuilder.append(i2 + 1);
                            stringBuilder.append(" marked as read");
                            log(stringBuilder.toString());
                        }
                    }
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
    }

    protected void updatePhoneObject(Phone phone) {
        this.mPhone = phone;
        this.mDispatchersController.updatePhoneObject(phone);
    }

    protected void enforceReceiveAndSend(String message) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECEIVE_SMS", message);
        this.mContext.enforceCallingOrSelfPermission("android.permission.SEND_SMS", message);
    }

    public boolean updateMessageOnIccEf(String callingPackage, int index, int status, byte[] pdu) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateMessageOnIccEf: index=");
        stringBuilder.append(index);
        stringBuilder.append(" status=");
        stringBuilder.append(status);
        stringBuilder.append(" ==> (");
        stringBuilder.append(Arrays.toString(pdu));
        stringBuilder.append(")");
        log(stringBuilder.toString());
        enforceReceiveAndSend("Updating message on Icc");
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message response = this.mHandler.obtainMessage(2);
            if (status != 0) {
                IccFileHandler fh = this.mPhone.getIccFileHandler();
                if (fh == null) {
                    response.recycle();
                    boolean z = this.mSuccess;
                    return z;
                }
                fh.updateEFLinearFixed(IccConstants.EF_SMS, index, makeSmsRecordData(status, pdu), null, response);
            } else if (1 == this.mPhone.getPhoneType()) {
                this.mPhone.mCi.deleteSmsOnSim(index, response);
            } else {
                this.mPhone.mCi.deleteSmsOnRuim(index, response);
            }
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to update by index");
            }
        }
        return this.mSuccess;
    }

    public boolean copyMessageToIccEf(String callingPackage, int status, byte[] pdu, byte[] smsc) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("copyMessageToIccEf: status=");
        stringBuilder.append(status);
        stringBuilder.append(" ==> pdu=(");
        stringBuilder.append(Arrays.toString(pdu));
        stringBuilder.append("), smsc=(");
        stringBuilder.append(Arrays.toString(smsc));
        stringBuilder.append(")");
        log(stringBuilder.toString());
        enforceReceiveAndSend("Copying message to Icc");
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message response = this.mHandler.obtainMessage(2);
            try {
                if (1 == this.mPhone.getPhoneType()) {
                    this.mPhone.mCi.writeSmsToSim(status, IccUtils.bytesToHexString(smsc), IccUtils.bytesToHexString(pdu), response);
                } else {
                    this.mPhone.mCi.writeSmsToRuim(status, new String(pdu, "ISO-8859-1"), response);
                }
                this.mLock.wait();
            } catch (UnsupportedEncodingException e) {
                log("copyMessageToIccEf: UnsupportedEncodingException");
            } catch (InterruptedException e2) {
                log("interrupted while trying to update by index");
            }
        }
        return this.mSuccess;
    }

    public List<SmsRawData> getAllMessagesFromIccEf(String callingPackage) {
        log("getAllMessagesFromEF");
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECEIVE_SMS", "Reading messages from Icc");
        synchronized (this.mLock) {
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh == null) {
                Rlog.e(LOG_TAG, "Cannot load Sms records. No icc card?");
                this.mSms = null;
                List list = this.mSms;
                return list;
            }
            fh.loadEFLinearFixedAll(IccConstants.EF_SMS, this.mHandler.obtainMessage(1));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to load from the Icc");
            }
        }
        return this.mSms;
    }

    public void sendDataWithSelfPermissions(String callingPackage, String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (checkCallingOrSelfSendSmsPermission(callingPackage, "Sending SMS message")) {
            sendDataInternal(destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
        }
    }

    public void sendData(String callingPackage, String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (checkCallingSendSmsPermission(callingPackage, "Sending SMS message")) {
            sendDataInternal(destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
        }
    }

    private void sendDataInternal(String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (Rlog.isLoggable("SMS", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendData: destAddr=");
            stringBuilder.append(destAddr);
            stringBuilder.append(" scAddr=");
            stringBuilder.append(scAddr);
            stringBuilder.append(" destPort=");
            stringBuilder.append(destPort);
            stringBuilder.append(" data='");
            stringBuilder.append(HexDump.toHexString(data));
            stringBuilder.append("' sentIntent=");
            stringBuilder.append(sentIntent);
            stringBuilder.append(" deliveryIntent=");
            stringBuilder.append(deliveryIntent);
            log(stringBuilder.toString());
        }
        destAddr = filterDestAddress(destAddr);
        if (HwSystemManager.allowOp(destAddr, data, sentIntent)) {
            this.mDispatchersController.sendData(destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
        }
    }

    public void sendText(String callingPackage, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessageForNonDefaultSmsApp) {
        String str = callingPackage;
        boolean z = persistMessageForNonDefaultSmsApp;
        if (checkCallingSendTextPermissions(z, str, "Sending SMS message")) {
            sendTextInternal(str, destAddr, scAddr, text, sentIntent, deliveryIntent, z, -1, false, -1);
        }
    }

    public void sendTextWithSelfPermissions(String callingPackage, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessage) {
        String str = callingPackage;
        if (checkCallingOrSelfSendSmsPermission(str, "Sending SMS message")) {
            sendTextInternal(str, destAddr, scAddr, text, sentIntent, deliveryIntent, persistMessage, -1, false, -1);
        }
    }

    private void sendTextInternal(String callingPackage, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod) {
        String str;
        PendingIntent pendingIntent;
        int i;
        String str2 = callingPackage;
        String str3 = destAddr;
        String str4 = text;
        PendingIntent pendingIntent2 = sentIntent;
        if (Rlog.isLoggable("SMS", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendText: destAddr=");
            stringBuilder.append(str3);
            stringBuilder.append(" scAddr=");
            str = scAddr;
            stringBuilder.append(str);
            stringBuilder.append(" text='");
            stringBuilder.append(str4);
            stringBuilder.append("' sentIntent=");
            stringBuilder.append(pendingIntent2);
            stringBuilder.append(" deliveryIntent=");
            pendingIntent = deliveryIntent;
            stringBuilder.append(pendingIntent);
            stringBuilder.append(" priority=");
            i = priority;
            stringBuilder.append(i);
            stringBuilder.append(" expectMore=");
            stringBuilder.append(expectMore);
            stringBuilder.append(" validityPeriod=");
            stringBuilder.append(validityPeriod);
            log(stringBuilder.toString());
        } else {
            str = scAddr;
            pendingIntent = deliveryIntent;
            i = priority;
            boolean z = expectMore;
            int i2 = validityPeriod;
        }
        String destAddr2 = filterDestAddress(str3);
        if (HwSystemManager.allowOp(destAddr2, str4, pendingIntent2)) {
            String destAddr3;
            if (LONG_SMS_SEND_DELAY_RELEASE && MultiSimVariants.DSDS == TelephonyManager.getDefault().getMultiSimConfiguration()) {
                this.mPhone.mCi.sendSMSSetLong(0, null);
                log("sendSMSSetLong 0 before sendText.");
            }
            if (CHINA_RELEASE_VERSION) {
                int uid = Binder.getCallingUid();
                if (uid != 1000) {
                    if (str2 == null || !str2.equals(MMS_PACKAGE_NAME)) {
                        authenticateSmsSend(destAddr2, str, str4, pendingIntent2, pendingIntent, str2, persistMessageForNonDefaultSmsApp, i, expectMore, validityPeriod, uid);
                        return;
                    }
                    destAddr3 = destAddr2;
                    this.mDispatchersController.sendText(destAddr3, scAddr, str4, pendingIntent2, deliveryIntent, null, str2, persistMessageForNonDefaultSmsApp, priority, expectMore, validityPeriod);
                }
            }
            destAddr3 = destAddr2;
            this.mDispatchersController.sendText(destAddr3, scAddr, str4, pendingIntent2, deliveryIntent, null, str2, persistMessageForNonDefaultSmsApp, priority, expectMore, validityPeriod);
        }
    }

    public void sendTextWithOptions(String callingPackage, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod) {
        if (checkCallingOrSelfSendSmsPermission(callingPackage, "Sending SMS message")) {
            sendTextInternal(callingPackage, destAddr, scAddr, text, sentIntent, deliveryIntent, persistMessageForNonDefaultSmsApp, priority, expectMore, validityPeriod);
        }
    }

    public void injectSmsPdu(byte[] pdu, String format, PendingIntent receivedIntent) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            enforceCallerIsImsAppOrCarrierApp("injectSmsPdu");
        }
        if (Rlog.isLoggable("SMS", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pdu: ");
            stringBuilder.append(pdu);
            stringBuilder.append("\n format=");
            stringBuilder.append(format);
            stringBuilder.append("\n receivedIntent=");
            stringBuilder.append(receivedIntent);
            log(stringBuilder.toString());
        }
        this.mDispatchersController.injectSmsPdu(pdu, format, new -$$Lambda$IccSmsInterfaceManager$rB1zRNxMbL7VadRMSxZ5tebvHwM(receivedIntent));
    }

    static /* synthetic */ void lambda$injectSmsPdu$0(PendingIntent receivedIntent, int result) {
        if (receivedIntent != null) {
            try {
                receivedIntent.send(result);
            } catch (CanceledException e) {
                Rlog.d(LOG_TAG, "receivedIntent cancelled.");
            }
        }
    }

    public void sendMultipartText(String callingPackage, String destAddr, String scAddr, List<String> parts, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp) {
        sendMultipartTextWithOptions(callingPackage, destAddr, scAddr, parts, sentIntents, deliveryIntents, persistMessageForNonDefaultSmsApp, -1, false, -1);
    }

    public void sendMultipartTextWithOptions(String callingPackage, String destAddr, String scAddr, List<String> parts, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod) {
        String str = callingPackage;
        String str2 = destAddr;
        boolean z = persistMessageForNonDefaultSmsApp;
        if (checkCallingSendTextPermissions(z, str, "Sending SMS message")) {
            if (Rlog.isLoggable("SMS", 2)) {
                int i = 0;
                for (String part : parts) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sendMultipartTextWithOptions: destAddr=");
                    stringBuilder.append(str2);
                    stringBuilder.append(", srAddr=");
                    stringBuilder.append(scAddr);
                    stringBuilder.append(", part[");
                    int i2 = i + 1;
                    stringBuilder.append(i);
                    stringBuilder.append("]=");
                    stringBuilder.append(part);
                    log(stringBuilder.toString());
                    i = i2;
                }
            }
            String str3 = scAddr;
            String destAddr2 = filterDestAddress(str2);
            List<String> list = parts;
            List<PendingIntent> list2 = sentIntents;
            if (HwSystemManager.allowOp(destAddr2, (String) list.get(0), list2)) {
                String destAddr3;
                if (CHINA_RELEASE_VERSION) {
                    int uid = Binder.getCallingUid();
                    if (uid != 1000) {
                        if (str == null || !str.equals(MMS_PACKAGE_NAME)) {
                            authenticateSmsSends(destAddr2, str3, list, list2, deliveryIntents, str, z, priority, expectMore, validityPeriod, uid);
                            return;
                        }
                        destAddr3 = destAddr2;
                        sendMultipartTextAfterAuth(destAddr3, str3, parts, sentIntents, deliveryIntents, str, z, priority, expectMore, validityPeriod);
                    }
                }
                destAddr3 = destAddr2;
                sendMultipartTextAfterAuth(destAddr3, str3, parts, sentIntents, deliveryIntents, str, z, priority, expectMore, validityPeriod);
            }
        }
    }

    protected void sendMultipartTextAfterAuth(String destAddr, String scAddr, List<String> parts, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, String callingPackage, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod) {
        List<String> list = parts;
        List<PendingIntent> list2 = sentIntents;
        List<PendingIntent> list3 = deliveryIntents;
        log("sendMultipartTextAfterAuth");
        if (parts.size() <= 1 || parts.size() >= 10 || SmsMessage.hasEmsSupport()) {
            this.mDispatchersController.sendMultipartText(destAddr, scAddr, (ArrayList) list, (ArrayList) list2, (ArrayList) list3, null, callingPackage, persistMessageForNonDefaultSmsApp, priority, expectMore, validityPeriod);
            return;
        }
        int i = 0;
        while (i < parts.size()) {
            String singlePart = (String) list.get(i);
            StringBuilder stringBuilder;
            if (SmsMessage.shouldAppendPageNumberAsPrefix()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(String.valueOf(i + 1));
                stringBuilder.append('/');
                stringBuilder.append(parts.size());
                stringBuilder.append(' ');
                stringBuilder.append(singlePart);
                singlePart = stringBuilder.toString();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(' ');
                stringBuilder.append(String.valueOf(i + 1));
                stringBuilder.append('/');
                stringBuilder.append(parts.size());
                singlePart = singlePart.concat(stringBuilder.toString());
            }
            PendingIntent singleSentIntent = null;
            if (list2 != null && sentIntents.size() > i) {
                singleSentIntent = (PendingIntent) list2.get(i);
            }
            PendingIntent singleSentIntent2 = singleSentIntent;
            singleSentIntent = null;
            if (list3 != null && deliveryIntents.size() > i) {
                singleSentIntent = (PendingIntent) list3.get(i);
            }
            PendingIntent singleDeliveryIntent = singleSentIntent;
            if (LONG_SMS_SEND_DELAY_RELEASE && i != parts.size() - 1) {
                this.mPhone.mCi.sendSMSSetLong(1, null);
                String str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("sendSMSSetLong i =");
                stringBuilder2.append(i);
                Log.e(str, stringBuilder2.toString());
            }
            this.mDispatchersController.sendText(destAddr, scAddr, singlePart, singleSentIntent2, singleDeliveryIntent, null, callingPackage, persistMessageForNonDefaultSmsApp, priority, expectMore, validityPeriod);
            i++;
        }
    }

    public int getPremiumSmsPermission(String packageName) {
        return this.mDispatchersController.getPremiumSmsPermission(packageName);
    }

    public void setPremiumSmsPermission(String packageName, int permission) {
        this.mDispatchersController.setPremiumSmsPermission(packageName, permission);
    }

    protected ArrayList<SmsRawData> buildValidRawData(ArrayList<byte[]> messages) {
        int count = messages.size();
        ArrayList<SmsRawData> ret = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            if (((byte[]) messages.get(i))[0] == (byte) 0) {
                ret.add(null);
            } else {
                ret.add(new SmsRawData((byte[]) messages.get(i)));
            }
        }
        return ret;
    }

    protected byte[] makeSmsRecordData(int status, byte[] pdu) {
        byte[] data;
        if (1 == this.mPhone.getPhoneType()) {
            data = new byte[176];
        } else {
            data = new byte[255];
        }
        data[0] = (byte) (status & 7);
        System.arraycopy(pdu, 0, data, 1, pdu.length);
        int j = pdu.length + 1;
        while (true) {
            int j2 = j;
            if (j2 >= data.length) {
                return data;
            }
            data[j2] = (byte) -1;
            j = j2 + 1;
        }
    }

    public boolean enableCellBroadcast(int messageIdentifier, int ranType) {
        return enableCellBroadcastRange(messageIdentifier, messageIdentifier, ranType);
    }

    public boolean disableCellBroadcast(int messageIdentifier, int ranType) {
        return disableCellBroadcastRange(messageIdentifier, messageIdentifier, ranType);
    }

    public boolean enableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) {
        if (ranType == 0) {
            return enableGsmBroadcastRange(startMessageId, endMessageId);
        }
        if (ranType == 1) {
            return enableCdmaBroadcastRange(startMessageId, endMessageId);
        }
        throw new IllegalArgumentException("Not a supportted RAN Type");
    }

    public boolean disableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) {
        if (ranType == 0) {
            return disableGsmBroadcastRange(startMessageId, endMessageId);
        }
        if (ranType == 1) {
            return disableCdmaBroadcastRange(startMessageId, endMessageId);
        }
        throw new IllegalArgumentException("Not a supportted RAN Type");
    }

    public synchronized boolean enableGsmBroadcastRange(int startMessageId, int endMessageId) {
        this.mContext.enforceCallingPermission("android.permission.RECEIVE_SMS", "Enabling cell broadcast SMS");
        String client = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        StringBuilder stringBuilder;
        if (this.mCellBroadcastRangeManager.enableRange(startMessageId, endMessageId, client)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Added GSM cell broadcast subscription for MID range ");
            stringBuilder.append(startMessageId);
            stringBuilder.append(" to ");
            stringBuilder.append(endMessageId);
            stringBuilder.append(" from client ");
            stringBuilder.append(client);
            log(stringBuilder.toString());
            setCellBroadcastActivation(this.mCellBroadcastRangeManager.isEmpty() ^ 1);
            return true;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to add GSM cell broadcast subscription for MID range ");
        stringBuilder.append(startMessageId);
        stringBuilder.append(" to ");
        stringBuilder.append(endMessageId);
        stringBuilder.append(" from client ");
        stringBuilder.append(client);
        log(stringBuilder.toString());
        return false;
    }

    public synchronized boolean disableGsmBroadcastRange(int startMessageId, int endMessageId) {
        this.mContext.enforceCallingPermission("android.permission.RECEIVE_SMS", "Disabling cell broadcast SMS");
        String client = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        StringBuilder stringBuilder;
        if (this.mCellBroadcastRangeManager.disableRange(startMessageId, endMessageId, client)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Removed GSM cell broadcast subscription for MID range ");
            stringBuilder.append(startMessageId);
            stringBuilder.append(" to ");
            stringBuilder.append(endMessageId);
            stringBuilder.append(" from client ");
            stringBuilder.append(client);
            log(stringBuilder.toString());
            setCellBroadcastActivation(this.mCellBroadcastRangeManager.isEmpty() ^ 1);
            return true;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to remove GSM cell broadcast subscription for MID range ");
        stringBuilder.append(startMessageId);
        stringBuilder.append(" to ");
        stringBuilder.append(endMessageId);
        stringBuilder.append(" from client ");
        stringBuilder.append(client);
        log(stringBuilder.toString());
        return false;
    }

    public synchronized boolean enableCdmaBroadcastRange(int startMessageId, int endMessageId) {
        this.mContext.enforceCallingPermission("android.permission.RECEIVE_SMS", "Enabling cdma broadcast SMS");
        String client = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        StringBuilder stringBuilder;
        if (this.mCdmaBroadcastRangeManager.enableRange(startMessageId, endMessageId, client)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Added cdma broadcast subscription for MID range ");
            stringBuilder.append(startMessageId);
            stringBuilder.append(" to ");
            stringBuilder.append(endMessageId);
            stringBuilder.append(" from client ");
            stringBuilder.append(client);
            log(stringBuilder.toString());
            setCdmaBroadcastActivation(this.mCdmaBroadcastRangeManager.isEmpty() ^ 1);
            return true;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to add cdma broadcast subscription for MID range ");
        stringBuilder.append(startMessageId);
        stringBuilder.append(" to ");
        stringBuilder.append(endMessageId);
        stringBuilder.append(" from client ");
        stringBuilder.append(client);
        log(stringBuilder.toString());
        return false;
    }

    public synchronized boolean disableCdmaBroadcastRange(int startMessageId, int endMessageId) {
        this.mContext.enforceCallingPermission("android.permission.RECEIVE_SMS", "Disabling cell broadcast SMS");
        String client = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        StringBuilder stringBuilder;
        if (this.mCdmaBroadcastRangeManager.disableRange(startMessageId, endMessageId, client)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Removed cdma broadcast subscription for MID range ");
            stringBuilder.append(startMessageId);
            stringBuilder.append(" to ");
            stringBuilder.append(endMessageId);
            stringBuilder.append(" from client ");
            stringBuilder.append(client);
            log(stringBuilder.toString());
            setCdmaBroadcastActivation(this.mCdmaBroadcastRangeManager.isEmpty() ^ 1);
            return true;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to remove cdma broadcast subscription for MID range ");
        stringBuilder.append(startMessageId);
        stringBuilder.append(" to ");
        stringBuilder.append(endMessageId);
        stringBuilder.append(" from client ");
        stringBuilder.append(client);
        log(stringBuilder.toString());
        return false;
    }

    private boolean setCellBroadcastConfig(SmsBroadcastConfigInfo[] configs) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling setGsmBroadcastConfig with ");
        stringBuilder.append(configs.length);
        stringBuilder.append(" configurations");
        log(stringBuilder.toString());
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(4);
            this.mSuccess = false;
            this.mPhone.mCi.setGsmBroadcastConfig(configs, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cell broadcast config");
            }
        }
        return this.mSuccess;
    }

    private boolean setCellBroadcastActivation(boolean activate) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling setCellBroadcastActivation(");
        stringBuilder.append(activate);
        stringBuilder.append(')');
        log(stringBuilder.toString());
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(3);
            this.mSuccess = false;
            this.mPhone.mCi.setGsmBroadcastActivation(activate, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cell broadcast activation");
            }
        }
        return this.mSuccess;
    }

    private boolean setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling setCdmaBroadcastConfig with ");
        stringBuilder.append(configs.length);
        stringBuilder.append(" configurations");
        log(stringBuilder.toString());
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(4);
            this.mSuccess = false;
            this.mPhone.mCi.setCdmaBroadcastConfig(configs, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cdma broadcast config");
            }
        }
        return this.mSuccess;
    }

    private boolean setCdmaBroadcastActivation(boolean activate) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling setCdmaBroadcastActivation(");
        stringBuilder.append(activate);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(3);
            this.mSuccess = false;
            this.mPhone.mCi.setCdmaBroadcastActivation(activate, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cdma broadcast activation");
            }
        }
        return this.mSuccess;
    }

    protected void log(String msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[IccSmsInterfaceManager] ");
        stringBuilder.append(msg);
        Log.d(str, stringBuilder.toString());
    }

    public boolean isImsSmsSupported() {
        return this.mDispatchersController.isIms();
    }

    public String getImsSmsFormat() {
        return this.mDispatchersController.getImsSmsFormat();
    }

    public void sendStoredText(String callingPkg, Uri messageUri, String scAddress, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        Uri uri = messageUri;
        PendingIntent pendingIntent = sentIntent;
        String str = callingPkg;
        if (checkCallingSendSmsPermission(str, "Sending SMS message")) {
            String str2;
            PendingIntent pendingIntent2;
            if (Rlog.isLoggable("SMS", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendStoredText: scAddr=");
                str2 = scAddress;
                stringBuilder.append(str2);
                stringBuilder.append(" messageUri=");
                stringBuilder.append(uri);
                stringBuilder.append(" sentIntent=");
                stringBuilder.append(pendingIntent);
                stringBuilder.append(" deliveryIntent=");
                pendingIntent2 = deliveryIntent;
                stringBuilder.append(pendingIntent2);
                log(stringBuilder.toString());
            } else {
                str2 = scAddress;
                pendingIntent2 = deliveryIntent;
            }
            ContentResolver resolver = this.mContext.getContentResolver();
            if (isFailedOrDraft(resolver, uri)) {
                String[] textAndAddress = loadTextAndAddress(resolver, uri);
                if (textAndAddress == null) {
                    Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredText: can not load text");
                    returnUnspecifiedFailure(pendingIntent);
                    return;
                } else if (HwSystemManager.allowOp(textAndAddress[1], textAndAddress[0], pendingIntent)) {
                    textAndAddress[1] = filterDestAddress(textAndAddress[1]);
                    this.mDispatchersController.sendText(textAndAddress[1], str2, textAndAddress[0], pendingIntent, pendingIntent2, uri, str, true, -1, false, -1);
                    return;
                } else {
                    return;
                }
            }
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredText: not FAILED or DRAFT message");
            returnUnspecifiedFailure(pendingIntent);
        }
    }

    public void sendStoredMultipartText(String callingPkg, Uri messageUri, String scAddress, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) {
        Uri uri = messageUri;
        List list = sentIntents;
        List<PendingIntent> list2 = deliveryIntents;
        String str = callingPkg;
        if (checkCallingSendSmsPermission(str, "Sending SMS message")) {
            ContentResolver resolver = this.mContext.getContentResolver();
            if (isFailedOrDraft(resolver, uri)) {
                String[] textAndAddress = loadTextAndAddress(resolver, uri);
                if (textAndAddress == null) {
                    Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredMultipartText: can not load text");
                    returnUnspecifiedFailure(list);
                    return;
                }
                int i = 0;
                ArrayList<String> parts = SmsManager.getDefault().divideMessage(textAndAddress[0]);
                ContentResolver contentResolver;
                if (parts != null) {
                    int i2 = 1;
                    ArrayList<String> arrayList;
                    if (parts.size() < 1) {
                        arrayList = parts;
                        contentResolver = resolver;
                    } else if (HwSystemManager.allowOp(textAndAddress[1], (String) parts.get(0), list)) {
                        textAndAddress[1] = filterDestAddress(textAndAddress[1]);
                        if (parts.size() <= 1 || parts.size() >= 10 || SmsMessage.hasEmsSupport()) {
                            contentResolver = resolver;
                            String str2 = scAddress;
                            ArrayList<String> arrayList2 = parts;
                            this.mDispatchersController.sendMultipartText(textAndAddress[1], str2, arrayList2, (ArrayList) list, (ArrayList) list2, uri, callingPkg, true, -1, false, -1);
                            return;
                        }
                        while (true) {
                            int i3 = i;
                            if (i3 < parts.size()) {
                                String singlePart = (String) parts.get(i3);
                                StringBuilder stringBuilder;
                                if (SmsMessage.shouldAppendPageNumberAsPrefix()) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(String.valueOf(i3 + 1));
                                    stringBuilder.append('/');
                                    stringBuilder.append(parts.size());
                                    stringBuilder.append(' ');
                                    stringBuilder.append(singlePart);
                                    singlePart = stringBuilder.toString();
                                } else {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(' ');
                                    stringBuilder.append(String.valueOf(i3 + 1));
                                    stringBuilder.append('/');
                                    stringBuilder.append(parts.size());
                                    singlePart = singlePart.concat(stringBuilder.toString());
                                }
                                String singlePart2 = singlePart;
                                PendingIntent singleSentIntent = null;
                                if (list != null && sentIntents.size() > i3) {
                                    singleSentIntent = (PendingIntent) list.get(i3);
                                }
                                PendingIntent singleSentIntent2 = singleSentIntent;
                                singleSentIntent = null;
                                if (list2 != null && deliveryIntents.size() > i3) {
                                    singleSentIntent = (PendingIntent) list2.get(i3);
                                }
                                int i4 = i3;
                                int i5 = i2;
                                arrayList = parts;
                                contentResolver = resolver;
                                this.mDispatchersController.sendText(textAndAddress[i2], scAddress, singlePart2, singleSentIntent2, singleSentIntent, uri, str, true, -1, false, -1);
                                i = i4 + 1;
                                str = callingPkg;
                                parts = arrayList;
                                resolver = contentResolver;
                                i2 = i5;
                            } else {
                                contentResolver = resolver;
                                return;
                            }
                        }
                    } else {
                        return;
                    }
                }
                contentResolver = resolver;
                Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredMultipartText: can not divide text");
                returnUnspecifiedFailure(list);
                return;
            }
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredMultipartText: not FAILED or DRAFT message");
            returnUnspecifiedFailure(list);
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0034, code skipped:
            if (r2 != null) goto L_0x0043;
     */
    /* JADX WARNING: Missing block: B:21:0x0041, code skipped:
            if (r2 == null) goto L_0x0046;
     */
    /* JADX WARNING: Missing block: B:22:0x0043, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:23:0x0046, code skipped:
            android.os.Binder.restoreCallingIdentity(r0);
     */
    /* JADX WARNING: Missing block: B:24:0x004a, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isFailedOrDraft(ContentResolver resolver, Uri messageUri) {
        long identity = Binder.clearCallingIdentity();
        Cursor cursor = null;
        boolean z = false;
        try {
            cursor = resolver.query(messageUri, new String[]{"type"}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int type = cursor.getInt(0);
                if (type == 3 || type == 5) {
                    z = true;
                }
                if (cursor != null) {
                    cursor.close();
                }
                Binder.restoreCallingIdentity(identity);
                return z;
            }
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]isFailedOrDraft: query message type failed", e);
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    /*  JADX ERROR: NullPointerException in pass: ProcessVariables
        java.lang.NullPointerException
        	at jadx.core.dex.visitors.regions.ProcessVariables.addToUsageMap(ProcessVariables.java:278)
        	at jadx.core.dex.visitors.regions.ProcessVariables.access$000(ProcessVariables.java:31)
        	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processInsn(ProcessVariables.java:163)
        	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processBlockTraced(ProcessVariables.java:129)
        	at jadx.core.dex.visitors.regions.TracedRegionVisitor.processBlock(TracedRegionVisitor.java:23)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:53)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverse(DepthRegionTraversal.java:18)
        	at jadx.core.dex.visitors.regions.ProcessVariables.visit(ProcessVariables.java:183)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private java.lang.String[] loadTextAndAddress(android.content.ContentResolver r13, android.net.Uri r14) {
        /*
        r12 = this;
        r0 = android.os.Binder.clearCallingIdentity();
        r2 = 0;
        r3 = r2;
        r4 = "body";	 Catch:{ SQLiteException -> 0x003f }
        r5 = "address";	 Catch:{ SQLiteException -> 0x003f }
        r8 = new java.lang.String[]{r4, r5};	 Catch:{ SQLiteException -> 0x003f }
        r9 = 0;	 Catch:{ SQLiteException -> 0x003f }
        r10 = 0;	 Catch:{ SQLiteException -> 0x003f }
        r11 = 0;	 Catch:{ SQLiteException -> 0x003f }
        r6 = r13;	 Catch:{ SQLiteException -> 0x003f }
        r7 = r14;	 Catch:{ SQLiteException -> 0x003f }
        r4 = r6.query(r7, r8, r9, r10, r11);	 Catch:{ SQLiteException -> 0x003f }
        r3 = r4;	 Catch:{ SQLiteException -> 0x003f }
        if (r3 == 0) goto L_0x003a;	 Catch:{ SQLiteException -> 0x003f }
    L_0x001a:
        r4 = r3.moveToFirst();	 Catch:{ SQLiteException -> 0x003f }
        if (r4 == 0) goto L_0x003a;	 Catch:{ SQLiteException -> 0x003f }
    L_0x0020:
        r4 = 2;	 Catch:{ SQLiteException -> 0x003f }
        r4 = new java.lang.String[r4];	 Catch:{ SQLiteException -> 0x003f }
        r5 = 0;	 Catch:{ SQLiteException -> 0x003f }
        r6 = r3.getString(r5);	 Catch:{ SQLiteException -> 0x003f }
        r4[r5] = r6;	 Catch:{ SQLiteException -> 0x003f }
        r5 = 1;	 Catch:{ SQLiteException -> 0x003f }
        r6 = r3.getString(r5);	 Catch:{ SQLiteException -> 0x003f }
        r4[r5] = r6;	 Catch:{ SQLiteException -> 0x003f }
        if (r3 == 0) goto L_0x0036;
    L_0x0033:
        r3.close();
    L_0x0036:
        android.os.Binder.restoreCallingIdentity(r0);
        return r4;
    L_0x003a:
        if (r3 == 0) goto L_0x004c;
    L_0x003c:
        goto L_0x0049;
    L_0x003d:
        r2 = move-exception;
        goto L_0x0051;
    L_0x003f:
        r4 = move-exception;
        r5 = "IccSmsInterfaceManager";	 Catch:{ all -> 0x003d }
        r6 = "[IccSmsInterfaceManager]loadText: query message text failed";	 Catch:{ all -> 0x003d }
        android.util.Log.e(r5, r6, r4);	 Catch:{ all -> 0x003d }
        if (r3 == 0) goto L_0x004c;
    L_0x0049:
        r3.close();
    L_0x004c:
        android.os.Binder.restoreCallingIdentity(r0);
        return r2;
    L_0x0051:
        if (r3 == 0) goto L_0x0056;
    L_0x0053:
        r3.close();
    L_0x0056:
        android.os.Binder.restoreCallingIdentity(r0);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.IccSmsInterfaceManager.loadTextAndAddress(android.content.ContentResolver, android.net.Uri):java.lang.String[]");
    }

    private void returnUnspecifiedFailure(PendingIntent pi) {
        if (pi != null) {
            try {
                pi.send(1);
            } catch (CanceledException e) {
            }
        }
    }

    private void returnUnspecifiedFailure(List<PendingIntent> pis) {
        if (pis != null) {
            for (PendingIntent pi : pis) {
                returnUnspecifiedFailure(pi);
            }
        }
    }

    @VisibleForTesting
    public boolean checkCallingSendTextPermissions(boolean persistMessageForNonDefaultSmsApp, String callingPackage, String message) {
        if (!persistMessageForNonDefaultSmsApp) {
            try {
                enforceCallerIsImsAppOrCarrierApp(message);
                return true;
            } catch (SecurityException e) {
                this.mContext.enforceCallingPermission("android.permission.MODIFY_PHONE_STATE", message);
            }
        }
        return checkCallingSendSmsPermission(callingPackage, message);
    }

    private boolean checkCallingOrSelfSendSmsPermission(String callingPackage, String message) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SEND_SMS", message);
        return this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) == 0;
    }

    private boolean checkCallingSendSmsPermission(String callingPackage, String message) {
        this.mContext.enforceCallingPermission("android.permission.SEND_SMS", message);
        return this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) == 0;
    }

    @VisibleForTesting
    public void enforceCallerIsImsAppOrCarrierApp(String message) {
        int callingUid = Binder.getCallingUid();
        String carrierImsPackage = CarrierSmsUtils.getCarrierImsPackageForIntent(this.mContext, this.mPhone, new Intent("android.service.carrier.CarrierMessagingService"));
        if (carrierImsPackage != null) {
            try {
                if (callingUid == this.mContext.getPackageManager().getPackageUid(carrierImsPackage, 0)) {
                    return;
                }
            } catch (NameNotFoundException e) {
                if (Rlog.isLoggable("SMS", 3)) {
                    log("Cannot find configured carrier ims package");
                }
            }
        }
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(this.mPhone.getSubId(), message);
    }

    private String filterDestAddress(String destAddr) {
        String result = SmsNumberUtils.filterDestAddr(this.mPhone, destAddr);
        return result != null ? result : destAddr;
    }

    private String bytesToHexString(byte[] data, int start, int len) {
        String ret = "";
        if (start < 0 || len < 0 || len > data.length - start) {
            throw new StringIndexOutOfBoundsException();
        }
        String ret2 = ret;
        for (int i = start; i < start + len; i++) {
            StringBuilder stringBuilder;
            String hex = Integer.toHexString(data[(len + 1) - i] & 255);
            if (hex.length() == 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append('0');
                stringBuilder.append(hex);
                hex = stringBuilder.toString();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(hex);
            stringBuilder.append(ret2);
            ret2 = stringBuilder.toString();
        }
        return ret2.toUpperCase(Locale.US);
    }

    public CellBroadcastRangeManager getCellBroadcastRangeManager() {
        return this.mCellBroadcastRangeManager;
    }

    public CdmaBroadcastRangeManager getCdmaBroadcastRangeManager() {
        return this.mCdmaBroadcastRangeManager;
    }

    public boolean setCellBroadcastActivationHw(boolean activate) {
        return setCellBroadcastActivation(activate);
    }

    public boolean setCdmaBroadcastActivationHw(boolean activate) {
        return setCdmaBroadcastActivation(activate);
    }
}
