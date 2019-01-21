package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class SmsBroadcastUndelivered {
    private static final boolean DBG = true;
    static final long DEFAULT_PARTIAL_SEGMENT_EXPIRE_AGE = 2592000000L;
    private static final String[] PDU_PENDING_MESSAGE_PROJECTION = new String[]{"pdu", "sequence", "destination_port", "date", "reference_number", "count", "address", HbpcdLookup.ID, "message_body", "display_originating_addr", "receive_time", "sub_id"};
    private static final String TAG = "SmsBroadcastUndelivered";
    private static SmsBroadcastUndelivered instance;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String str = SmsBroadcastUndelivered.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Received broadcast ");
            stringBuilder.append(intent.getAction());
            Rlog.d(str, stringBuilder.toString());
            if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                new ScanRawTableThread(SmsBroadcastUndelivered.this, context, null).start();
            }
        }
    };
    private final CdmaInboundSmsHandler mCdmaInboundSmsHandler;
    private final GsmInboundSmsHandler mGsmInboundSmsHandler;
    private final ContentResolver mResolver;
    private int mSubId;

    private class ScanRawTableThread extends Thread {
        private final Context context;

        /* synthetic */ ScanRawTableThread(SmsBroadcastUndelivered x0, Context x1, AnonymousClass1 x2) {
            this(x1);
        }

        private ScanRawTableThread(Context context) {
            this.context = context;
        }

        public void run() {
            SmsBroadcastUndelivered.this.scanRawTable(this.context);
            InboundSmsHandler.cancelNewMessageNotification(this.context);
        }
    }

    private static class SmsReferenceKey {
        final String mAddress;
        final int mMessageCount;
        final String mQuery;
        final int mReferenceNumber;

        SmsReferenceKey(InboundSmsTracker tracker) {
            this.mAddress = tracker.getAddress();
            this.mReferenceNumber = tracker.getReferenceNumber();
            this.mMessageCount = tracker.getMessageCount();
            this.mQuery = tracker.getQueryForSegments();
        }

        String[] getDeleteWhereArgs() {
            return new String[]{this.mAddress, Integer.toString(this.mReferenceNumber), Integer.toString(this.mMessageCount)};
        }

        String getDeleteWhere() {
            return this.mQuery;
        }

        public int hashCode() {
            return (((this.mReferenceNumber * 31) + this.mMessageCount) * 31) + this.mAddress.hashCode();
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof SmsReferenceKey)) {
                return false;
            }
            SmsReferenceKey other = (SmsReferenceKey) o;
            if (other.mAddress.equals(this.mAddress) && other.mReferenceNumber == this.mReferenceNumber && other.mMessageCount == this.mMessageCount) {
                z = true;
            }
            return z;
        }
    }

    public static void initialize(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler) {
        if (instance == null) {
            instance = new SmsBroadcastUndelivered(context, gsmInboundSmsHandler, cdmaInboundSmsHandler);
        }
        if (gsmInboundSmsHandler != null) {
            gsmInboundSmsHandler.sendMessage(6);
        }
        if (cdmaInboundSmsHandler != null) {
            cdmaInboundSmsHandler.sendMessage(6);
        }
    }

    public static void initialize(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler, int subId) {
        instance = new SmsBroadcastUndelivered(context, gsmInboundSmsHandler, cdmaInboundSmsHandler, subId);
        if (gsmInboundSmsHandler != null) {
            gsmInboundSmsHandler.sendMessage(6);
        }
        if (cdmaInboundSmsHandler != null) {
            cdmaInboundSmsHandler.sendMessage(6);
        }
    }

    private SmsBroadcastUndelivered(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler) {
        this.mResolver = context.getContentResolver();
        this.mGsmInboundSmsHandler = gsmInboundSmsHandler;
        this.mCdmaInboundSmsHandler = cdmaInboundSmsHandler;
        if (((UserManager) context.getSystemService("user")).isUserUnlocked()) {
            new ScanRawTableThread(this, context, null).start();
            return;
        }
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_UNLOCKED");
        context.registerReceiver(this.mBroadcastReceiver, userFilter);
    }

    public SmsBroadcastUndelivered(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler, int subId) {
        this.mResolver = context.getContentResolver();
        this.mGsmInboundSmsHandler = gsmInboundSmsHandler;
        this.mCdmaInboundSmsHandler = cdmaInboundSmsHandler;
        this.mSubId = subId;
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_UNLOCKED");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.CURRENT, userFilter, null, null);
    }

    private void scanRawTable(Context context) {
        Rlog.d(TAG, "scanning raw table for undelivered messages");
        long startTime = System.nanoTime();
        HashMap<SmsReferenceKey, Integer> multiPartReceivedCount = new HashMap(4);
        HashSet<SmsReferenceKey> oldMultiPartMessages = new HashSet(4);
        Cursor cursor = null;
        StringBuilder stringBuilder;
        String str;
        try {
            String stringBuilder2;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("deleted = 0");
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(" AND sub_id=");
                stringBuilder.append(this.mSubId);
                stringBuilder2 = stringBuilder.toString();
            } else {
                stringBuilder2 = "";
            }
            stringBuilder3.append(stringBuilder2);
            String where = stringBuilder3.toString();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("scanRawTable where=");
            stringBuilder.append(where);
            Rlog.d(str, stringBuilder.toString());
            cursor = this.mResolver.query(InboundSmsHandler.sRawUri, PDU_PENDING_MESSAGE_PROJECTION, where, null, null);
            if (cursor == null) {
                Rlog.e(TAG, "error getting pending message cursor");
                if (cursor != null) {
                    cursor.close();
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("finished scanning raw table in ");
                stringBuilder.append((System.nanoTime() - startTime) / 1000000);
                stringBuilder.append(" ms");
                Rlog.d(str, stringBuilder.toString());
                return;
            }
            SmsReferenceKey reference;
            boolean isCurrentFormat3gpp2 = InboundSmsHandler.isCurrentFormat3gpp2();
            while (true) {
                boolean isCurrentFormat3gpp22 = isCurrentFormat3gpp2;
                if (!cursor.moveToNext()) {
                    break;
                }
                try {
                    InboundSmsTracker tracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(cursor, isCurrentFormat3gpp22);
                    if (tracker.getMessageCount() == 1) {
                        Rlog.i(TAG, "scanRawTable: deliver single-part message");
                        broadcastSms(tracker);
                    } else {
                        Rlog.i(TAG, "scanRawTable: process multi-part message");
                        reference = new SmsReferenceKey(tracker);
                        Integer receivedCount = (Integer) multiPartReceivedCount.get(reference);
                        if (receivedCount == null) {
                            multiPartReceivedCount.put(reference, Integer.valueOf(1));
                            if (tracker.getTimestamp() < System.currentTimeMillis() - getUndeliveredSmsExpirationTime(context)) {
                                oldMultiPartMessages.add(reference);
                            }
                        } else {
                            int newCount = receivedCount.intValue() + 1;
                            if (newCount == tracker.getMessageCount()) {
                                Rlog.d(TAG, "found complete multi-part message");
                                broadcastSms(tracker);
                                oldMultiPartMessages.remove(reference);
                            } else {
                                multiPartReceivedCount.put(reference, Integer.valueOf(newCount));
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("error loading SmsTracker: ");
                    stringBuilder4.append(e);
                    Rlog.e(str2, stringBuilder4.toString());
                }
                isCurrentFormat3gpp2 = isCurrentFormat3gpp22;
            }
            Iterator it = oldMultiPartMessages.iterator();
            while (it.hasNext()) {
                reference = (SmsReferenceKey) it.next();
                int rows = this.mResolver.delete(InboundSmsHandler.sRawUriPermanentDelete, reference.getDeleteWhere(), reference.getDeleteWhereArgs());
                if (rows == 0) {
                    Rlog.e(TAG, "No rows were deleted from raw table!");
                } else {
                    String str3 = TAG;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Deleted ");
                    stringBuilder5.append(rows);
                    stringBuilder5.append(" rows from raw table for incomplete ");
                    stringBuilder5.append(reference.mMessageCount);
                    stringBuilder5.append(" part message");
                    Rlog.d(str3, stringBuilder5.toString());
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("finished scanning raw table in ");
            stringBuilder.append((System.nanoTime() - startTime) / 1000000);
            stringBuilder.append(" ms");
            Rlog.d(str, stringBuilder.toString());
        } catch (SQLException e2) {
            Rlog.e(TAG, "error reading pending SMS messages", e2);
            if (cursor != null) {
                cursor.close();
            }
            str = TAG;
            stringBuilder = new StringBuilder();
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("finished scanning raw table in ");
            stringBuilder.append((System.nanoTime() - startTime) / 1000000);
            stringBuilder.append(" ms");
            Rlog.d(TAG, stringBuilder.toString());
        }
    }

    private void broadcastSms(InboundSmsTracker tracker) {
        InboundSmsHandler handler;
        if (tracker.is3gpp2()) {
            handler = this.mCdmaInboundSmsHandler;
        } else {
            handler = this.mGsmInboundSmsHandler;
        }
        if (handler != null) {
            handler.sendMessage(2, tracker);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("null handler for ");
        stringBuilder.append(tracker.getFormat());
        stringBuilder.append(" format, can't deliver.");
        Rlog.e(str, stringBuilder.toString());
    }

    private long getUndeliveredSmsExpirationTime(Context context) {
        PersistableBundle bundle = ((CarrierConfigManager) context.getSystemService("carrier_config")).getConfigForSubId(SubscriptionManager.getDefaultSmsSubscriptionId());
        if (bundle != null) {
            return bundle.getLong("undelivered_sms_message_expiration_time", DEFAULT_PARTIAL_SEGMENT_EXPIRE_AGE);
        }
        return DEFAULT_PARTIAL_SEGMENT_EXPIRE_AGE;
    }
}
