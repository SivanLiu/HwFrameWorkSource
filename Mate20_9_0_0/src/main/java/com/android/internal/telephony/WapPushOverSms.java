package com.android.internal.telephony;

import android.app.BroadcastOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IDeviceIdleController;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Inbox;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.util.Jlog;
import android.util.Log;
import com.android.internal.telephony.IWapPushManager.Stub;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.DeliveryInd;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadOrigInd;
import java.util.HashMap;

public class WapPushOverSms extends AbstractWapPushOverSms {
    private static final boolean DBG = false;
    private static final String LOCATION_SELECTION = "m_type=? AND ct_l =?";
    private static final String TAG = "WAP PUSH";
    private static final String THREAD_ID_SELECTION = "m_id=? AND m_type=?";
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String str = WapPushOverSms.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Received broadcast ");
            stringBuilder.append(intent.getAction());
            Rlog.d(str, stringBuilder.toString());
            if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                new BindServiceThread(WapPushOverSms.this, WapPushOverSms.this.mContext, null).start();
            }
        }
    };
    private final Context mContext;
    private IDeviceIdleController mDeviceIdleController;
    protected String mOriginalAddr;
    protected InboundSmsTracker mSmsTracker;
    private volatile IWapPushManager mWapPushManager;
    private String mWapPushManagerPackage;

    private class BindServiceThread extends Thread {
        private final Context context;

        /* synthetic */ BindServiceThread(WapPushOverSms x0, Context x1, AnonymousClass1 x2) {
            this(x1);
        }

        private BindServiceThread(Context context) {
            this.context = context;
        }

        public void run() {
            WapPushOverSms.this.bindWapPushManagerService(this.context);
        }
    }

    private final class DecodedResult {
        String contentType;
        HashMap<String, String> contentTypeParameters;
        byte[] header;
        int headerLength;
        int headerStartIndex;
        byte[] intentData;
        String mimeType;
        GenericPdu parsedPdu;
        WspTypeDecoder pduDecoder;
        int pduType;
        int phoneId;
        int statusCode;
        int subId;
        int transactionId;
        String wapAppId;

        private DecodedResult() {
        }

        /* synthetic */ DecodedResult(WapPushOverSms x0, AnonymousClass1 x1) {
            this();
        }
    }

    private void bindWapPushManagerService(Context context) {
        Intent intent = new Intent(IWapPushManager.class.getName());
        ComponentName comp = intent.resolveSystemService(context.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !context.bindService(intent, this, 1)) {
            Rlog.e(TAG, "bindService() for wappush manager failed");
            return;
        }
        synchronized (this) {
            this.mWapPushManagerPackage = comp.getPackageName();
        }
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        this.mWapPushManager = Stub.asInterface(service);
    }

    public void onServiceDisconnected(ComponentName name) {
        this.mWapPushManager = null;
    }

    public WapPushOverSms(Context context) {
        this.mContext = context;
        this.mDeviceIdleController = TelephonyComponentFactory.getInstance().getIDeviceIdleController();
        if (((UserManager) this.mContext.getSystemService("user")).isUserUnlocked()) {
            bindWapPushManagerService(this.mContext);
            return;
        }
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_UNLOCKED");
        context.registerReceiver(this.mBroadcastReceiver, userFilter);
    }

    public void dispose() {
        if (this.mWapPushManager != null) {
            this.mContext.unbindService(this);
        } else {
            Rlog.e(TAG, "dispose: not bound to a wappush manager");
        }
    }

    private DecodedResult decodeWapPdu(byte[] pdu, InboundSmsHandler handler, boolean isMmsWapPush) {
        byte[] bArr = pdu;
        DecodedResult result = new DecodedResult(this, null);
        int index = 0 + 1;
        String wapAppId;
        try {
            int index2;
            int transactionId = bArr[0] & 255;
            int index3 = index + 1;
            index = bArr[index] & 255;
            int phoneId = handler.getPhone().getPhoneId();
            if (!(index == 6 || index == 7)) {
                index3 = this.mContext.getResources().getInteger(17694877);
                if (index3 != -1) {
                    index2 = index3 + 1;
                    transactionId = bArr[index3] & 255;
                    index3 = index2 + 1;
                    index = bArr[index2] & 255;
                    if (!(index == 6 || index == 7)) {
                        result.statusCode = 1;
                        return result;
                    }
                }
                result.statusCode = 1;
                return result;
            }
            int pduType = index;
            index = transactionId;
            WspTypeDecoder pduDecoder = HwTelephonyFactory.getHwInnerSmsManager().createHwWspTypeDecoder(bArr);
            if (pduDecoder.decodeUintvarInteger(index3)) {
                index2 = (int) pduDecoder.getValue32();
                index3 += pduDecoder.getDecodedDataLength();
                int headerStartIndex = index3;
                if (pduDecoder.decodeContentType(index3)) {
                    byte[] intentData;
                    GenericPdu parsedPdu;
                    int headerStartIndex2;
                    String mimeType = pduDecoder.getValueString();
                    long binaryContentType = pduDecoder.getValue32();
                    index3 += pduDecoder.getDecodedDataLength();
                    byte[] header = new byte[index2];
                    System.arraycopy(bArr, headerStartIndex, header, 0, header.length);
                    if (mimeType == null || !mimeType.equals(WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO)) {
                        transactionId = headerStartIndex + index2;
                        intentData = new byte[(bArr.length - transactionId)];
                        System.arraycopy(bArr, transactionId, intentData, 0, intentData.length);
                    } else {
                        intentData = bArr;
                    }
                    int[] subIds = SubscriptionManager.getSubId(phoneId);
                    if (subIds == null || subIds.length <= 0) {
                        transactionId = SmsManager.getDefaultSmsSubscriptionId();
                    } else {
                        transactionId = subIds[0];
                    }
                    int subId = transactionId;
                    GenericPdu parsedPdu2 = null;
                    try {
                        parsedPdu = new PduParser(intentData, shouldParseContentDisposition(subId)).parse();
                        int[] iArr = subIds;
                        headerStartIndex2 = headerStartIndex;
                    } catch (Exception e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        headerStartIndex2 = headerStartIndex;
                        stringBuilder.append("Unable to parse PDU: ");
                        stringBuilder.append(e.toString());
                        Rlog.e(str, stringBuilder.toString());
                        parsedPdu = parsedPdu2;
                    }
                    if (parsedPdu != null && parsedPdu.getMessageType() == 130) {
                        NotificationInd nInd = (NotificationInd) parsedPdu;
                        if (nInd.getFrom() != null) {
                            if (BlockChecker.isBlocked(this.mContext, nInd.getFrom().getString(), null) != null) {
                                Intent intent = new Intent("android.provider.Telephony.WAP_PUSH_DELIVER");
                                intent.setType(mimeType);
                                intent.putExtra("transactionId", index);
                                intent.putExtra("pduType", pduType);
                                intent.putExtra("header", header);
                                intent.putExtra("data", intentData);
                                intent.putExtra("contentTypeParameters", pduDecoder.getContentParameters());
                                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, phoneId);
                                intent.putExtra("sender", this.mOriginalAddr);
                                intent.putExtra("isWapPush", true);
                                if (!isMmsWapPush) {
                                    HwTelephonyFactory.getHwInnerSmsManager().sendGoogleSmsBlockedRecord(intent);
                                }
                                result.statusCode = 1;
                                return result;
                            }
                        }
                    }
                    if (pduDecoder.seekXWapApplicationId(index3, (index3 + index2) - 1)) {
                        String contentType;
                        pduDecoder.decodeXWapApplicationId((int) pduDecoder.getValue32());
                        wapAppId = pduDecoder.getValueString();
                        if (wapAppId == null) {
                            wapAppId = Integer.toString((int) pduDecoder.getValue32());
                        } else {
                            String str2 = wapAppId;
                        }
                        result.wapAppId = wapAppId;
                        if (mimeType == null) {
                            contentType = Long.toString(binaryContentType);
                        } else {
                            contentType = mimeType;
                        }
                        result.contentType = contentType;
                    }
                    result.subId = subId;
                    result.phoneId = phoneId;
                    result.parsedPdu = parsedPdu;
                    result.mimeType = mimeType;
                    result.transactionId = index;
                    result.pduType = pduType;
                    result.header = header;
                    result.intentData = intentData;
                    result.contentTypeParameters = pduDecoder.getContentParameters();
                    result.statusCode = -1;
                    result.headerStartIndex = headerStartIndex2;
                    result.headerLength = index2;
                    result.pduDecoder = pduDecoder;
                    return result;
                }
                result.statusCode = 2;
                return result;
            }
            result.statusCode = 2;
            return result;
        } catch (ArrayIndexOutOfBoundsException aie) {
            wapAppId = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("ignoring dispatchWapPdu() array index exception: ");
            stringBuilder2.append(aie);
            Rlog.e(wapAppId, stringBuilder2.toString());
            result.statusCode = 2;
        }
    }

    public int dispatchWapPdu(byte[] pdu, BroadcastReceiver receiver, InboundSmsHandler handler) {
        byte[] bArr = pdu;
        InboundSmsHandler inboundSmsHandler = handler;
        DecodedResult result = decodeWapPdu(bArr, inboundSmsHandler, false);
        if (result.statusCode != -1) {
            return result.statusCode;
        }
        if (SmsManager.getDefault().getAutoPersisting()) {
            writeInboxMessage(result.subId, result.parsedPdu);
        }
        if (result.wapAppId != null) {
            boolean processFurther = true;
            try {
                IWapPushManager wapPushMan = this.mWapPushManager;
                if (wapPushMan != null) {
                    synchronized (this) {
                        this.mDeviceIdleController.addPowerSaveTempWhitelistAppForMms(this.mWapPushManagerPackage, 0, "mms-mgr");
                    }
                    Intent intent = new Intent();
                    intent.putExtra("transactionId", result.transactionId);
                    intent.putExtra("pduType", result.pduType);
                    intent.putExtra("header", result.header);
                    intent.putExtra("data", result.intentData);
                    intent.putExtra("contentTypeParameters", result.contentTypeParameters);
                    SubscriptionManager.putPhoneIdAndSubIdExtra(intent, result.phoneId);
                    int procRet = wapPushMan.processMessage(result.wapAppId, result.contentType, intent);
                    if ((procRet & 1) > 0 && (32768 & procRet) == 0) {
                        processFurther = false;
                    }
                }
                if (!processFurther) {
                    return 1;
                }
            } catch (RemoteException e) {
            }
        }
        if (result.mimeType == null) {
            return 2;
        }
        if (HwTelephonyFactory.getHwInnerSmsManager().handleWapPushExtraMimeType(result.mimeType)) {
            if (!dispatchWapPduForWbxml(bArr, result.pduDecoder, result.transactionId, result.pduType, result.headerStartIndex, result.headerLength, inboundSmsHandler, receiver)) {
                return 1;
            }
        }
        Bundle options;
        if (result.mimeType.equals("application/vnd.wap.mms-message") && SystemProperties.get("ro.hwpp.wcdma_voice_preference", "false").equals("true")) {
            DcTracker dcTracker = handler.getPhone().mDcTracker;
            if (1 == dcTracker.mVpStatus) {
                dcTracker.onVPEnded();
                dcTracker.mVpStatus = 0;
            }
        }
        Intent intent2 = new Intent("android.provider.Telephony.WAP_PUSH_DELIVER");
        intent2.setType(result.mimeType);
        intent2.putExtra("transactionId", result.transactionId);
        intent2.putExtra("pduType", result.pduType);
        intent2.putExtra("header", result.header);
        intent2.putExtra("data", result.intentData);
        intent2.putExtra("contentTypeParameters", result.contentTypeParameters);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent2, result.phoneId);
        intent2.putExtra("sender", this.mOriginalAddr);
        if ("application/vnd.omaloc-supl-init".equals(result.mimeType)) {
            Rlog.v(TAG, "add appid to intent");
            intent2.putExtra("x-application-id-field", result.wapAppId);
        }
        if (this.mSmsTracker != null) {
            if (HwTelephonyFactory.getHwInnerSmsManager().newSmsShouldBeIntercepted(this.mContext, intent2, inboundSmsHandler, this.mSmsTracker.getDeleteWhere(), this.mSmsTracker.getDeleteWhereArgs(), true)) {
                return -1;
            }
        }
        ComponentName componentName = SmsApplication.getDefaultMmsApplication(this.mContext, true);
        if (componentName != null) {
            intent2.setComponent(componentName);
            try {
                long duration = this.mDeviceIdleController.addPowerSaveTempWhitelistAppForMms(componentName.getPackageName(), 0, "mms-app");
                BroadcastOptions bopts = BroadcastOptions.makeBasic();
                bopts.setTemporaryAppWhitelistDuration(duration);
                options = bopts.toBundle();
            } catch (RemoteException e2) {
            }
            inboundSmsHandler.dispatchIntent(intent2, getPermissionForType(result.mimeType), getAppOpsPermissionForIntent(result.mimeType), options, receiver, UserHandle.SYSTEM);
        }
        options = null;
        inboundSmsHandler.dispatchIntent(intent2, getPermissionForType(result.mimeType), getAppOpsPermissionForIntent(result.mimeType), options, receiver, UserHandle.SYSTEM);
        Jlog.d(51, "JL_WAP_DISPATCH_PDU");
        return -1;
    }

    public boolean isWapPushForMms(byte[] pdu, InboundSmsHandler handler) {
        DecodedResult result = decodeWapPdu(pdu, handler, true);
        if (result.statusCode == -1 && "application/vnd.wap.mms-message".equals(result.mimeType)) {
            return true;
        }
        return false;
    }

    private static boolean shouldParseContentDisposition(int subId) {
        return SmsManager.getSmsManagerForSubscriptionId(subId).getCarrierConfigValues().getBoolean("supportMmsContentDisposition", true);
    }

    private void writeInboxMessage(int subId, GenericPdu pdu) {
        GenericPdu genericPdu = pdu;
        if (genericPdu == null) {
            Rlog.e(TAG, "Invalid PUSH PDU");
        }
        PduPersister persister = PduPersister.getPduPersister(this.mContext);
        int type = pdu.getMessageType();
        String str;
        StringBuilder stringBuilder;
        MmsException e;
        if (type == 130) {
            NotificationInd e2 = (NotificationInd) genericPdu;
            Bundle configs = SmsManager.getSmsManagerForSubscriptionId(subId).getCarrierConfigValues();
            if (configs != null && configs.getBoolean("enabledTransID", false)) {
                byte[] contentLocation = e2.getContentLocation();
                if ((byte) 61 == contentLocation[contentLocation.length - 1]) {
                    byte[] transactionId = e2.getTransactionId();
                    byte[] contentLocationWithId = new byte[(contentLocation.length + transactionId.length)];
                    System.arraycopy(contentLocation, 0, contentLocationWithId, 0, contentLocation.length);
                    System.arraycopy(transactionId, 0, contentLocationWithId, contentLocation.length, transactionId.length);
                    e2.setContentLocation(contentLocationWithId);
                }
            }
            if (isDuplicateNotification(this.mContext, e2)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Skip storing duplicate MMS WAP push notification ind: ");
                stringBuilder.append(new String(e2.getContentLocation()));
                Rlog.d(str, stringBuilder.toString());
                return;
            }
            if (persister.persist(genericPdu, Inbox.CONTENT_URI, true, true, null) == null) {
                Rlog.e(TAG, "Failed to save MMS WAP push notification ind");
            }
        } else if (type == 134 || type == 136) {
            long threadId = getDeliveryOrReadReportThreadId(this.mContext, genericPdu);
            if (threadId == -1) {
                Rlog.e(TAG, "Failed to find delivery or read report's thread id");
                return;
            }
            e = persister.persist(genericPdu, Inbox.CONTENT_URI, true, true, null);
            if (e == null) {
                Rlog.e(TAG, "Failed to persist delivery or read report");
                return;
            }
            ContentValues values = new ContentValues(1);
            values.put("thread_id", Long.valueOf(threadId));
            if (SqliteWrapper.update(this.mContext, this.mContext.getContentResolver(), e, values, null, null) != 1) {
                Rlog.e(TAG, "Failed to update delivery or read report thread id");
            }
        } else {
            try {
                Log.e(TAG, "Received unrecognized WAP Push PDU.");
            } catch (MmsException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to save MMS WAP push data: type=");
                stringBuilder.append(type);
                Log.e(str, stringBuilder.toString(), e3);
            } catch (RuntimeException e32) {
                Log.e(TAG, "Unexpected RuntimeException in persisting MMS WAP push data", e32);
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0061, code skipped:
            if (r3 != null) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:17:0x0063, code skipped:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:22:0x0071, code skipped:
            if (r3 == null) goto L_0x0074;
     */
    /* JADX WARNING: Missing block: B:23:0x0074, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static long getDeliveryOrReadReportThreadId(Context context, GenericPdu pdu) {
        String messageId;
        if (pdu instanceof DeliveryInd) {
            messageId = new String(((DeliveryInd) pdu).getMessageId());
        } else if (pdu instanceof ReadOrigInd) {
            messageId = new String(((ReadOrigInd) pdu).getMessageId());
        } else {
            messageId = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WAP Push data is neither delivery or read report type: ");
            stringBuilder.append(pdu.getClass().getCanonicalName());
            Rlog.e(messageId, stringBuilder.toString());
            return -1;
        }
        Cursor cursor = null;
        try {
            cursor = SqliteWrapper.query(context, context.getContentResolver(), Mms.CONTENT_URI, new String[]{"thread_id"}, THREAD_ID_SELECTION, new String[]{DatabaseUtils.sqlEscapeString(messageId), Integer.toString(128)}, null);
            if (cursor != null && cursor.moveToFirst()) {
                long j = cursor.getLong(0);
                if (cursor != null) {
                    cursor.close();
                }
                return j;
            }
        } catch (SQLiteException e) {
            Rlog.e(TAG, "Failed to query delivery or read report thread id", e);
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0049, code skipped:
            if (r5 != null) goto L_0x0058;
     */
    /* JADX WARNING: Missing block: B:16:0x0056, code skipped:
            if (r5 == null) goto L_0x0062;
     */
    /* JADX WARNING: Missing block: B:17:0x0058, code skipped:
            r5.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isDuplicateNotification(Context context, NotificationInd nInd) {
        if (nInd.getContentLocation() != null) {
            String[] selectionArgs = new String[]{new String(nInd.getContentLocation())};
            Cursor cursor = null;
            try {
                cursor = SqliteWrapper.query(context, context.getContentResolver(), Mms.CONTENT_URI, new String[]{HbpcdLookup.ID}, LOCATION_SELECTION, new String[]{Integer.toString(130), new String(rawLocation)}, null);
                if (cursor != null && cursor.getCount() > 0) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    return true;
                }
            } catch (SQLiteException e) {
                Rlog.e(TAG, "failed to query existing notification ind", e);
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
        }
        return false;
    }

    public static String getPermissionForType(String mimeType) {
        if ("application/vnd.wap.mms-message".equals(mimeType)) {
            return "android.permission.RECEIVE_MMS";
        }
        return "android.permission.RECEIVE_WAP_PUSH";
    }

    public static int getAppOpsPermissionForIntent(String mimeType) {
        if ("application/vnd.wap.mms-message".equals(mimeType)) {
            return 18;
        }
        return 19;
    }
}
