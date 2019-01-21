package com.android.internal.telephony;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Telephony.Sms.Sent;
import android.service.carrier.ICarrierMessagingCallback.Stub;
import android.service.carrier.ICarrierMessagingService;
import android.telephony.CarrierMessagingServiceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.ArraySet;
import android.util.EventLog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.SmsHeader.ConcatRef;
import com.android.internal.telephony.SmsMessageBase.SubmitPduBase;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.huawei.internal.telephony.HwRadarUtils;
import huawei.cust.HwCfgFilePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SMSDispatcher extends AbstractSMSDispatcher {
    private static final String CONTACTS_PACKAGE_NAME = "com.android.contacts";
    static final boolean DBG = false;
    private static final String DEFAULT_SMS_APPLICATION = "com.android.mms";
    private static final int EVENT_CONFIRM_SEND_TO_POSSIBLE_PREMIUM_SHORT_CODE = 8;
    private static final int EVENT_CONFIRM_SEND_TO_PREMIUM_SHORT_CODE = 9;
    protected static final int EVENT_GET_IMS_SERVICE = 16;
    protected static final int EVENT_HANDLE_STATUS_REPORT = 10;
    protected static final int EVENT_ICC_CHANGED = 15;
    protected static final int EVENT_NEW_ICC_SMS = 14;
    private static final int EVENT_POP_TOAST = 17;
    static final int EVENT_SEND_CONFIRMED_SMS = 5;
    private static final int EVENT_SEND_LIMIT_REACHED_CONFIRMATION = 4;
    private static final int EVENT_SEND_RETRY = 3;
    protected static final int EVENT_SEND_SMS_COMPLETE = 2;
    private static final int EVENT_SEND_SMS_OVERLOAD = 20;
    static final int EVENT_STOP_SENDING = 7;
    private static final String HW_CSP_PACKAGE = "com.android.contacts";
    protected static final String MAP_KEY_DATA = "data";
    protected static final String MAP_KEY_DEST_ADDR = "destAddr";
    protected static final String MAP_KEY_DEST_PORT = "destPort";
    protected static final String MAP_KEY_PDU = "pdu";
    protected static final String MAP_KEY_SC_ADDR = "scAddr";
    protected static final String MAP_KEY_SMSC = "smsc";
    protected static final String MAP_KEY_TEXT = "text";
    private static final float MAX_LABEL_SIZE_PX = 500.0f;
    private static final int MAX_SEND_RETRIES = (SystemProperties.getBoolean("ro.config.close_sms_retry", false) ^ 1);
    private static final int MAX_SEND_RETRIES_FOR_VIA = 3;
    private static final String MESSAGE_STRING_NAME = "app_label";
    private static final int MO_MSG_QUEUE_LIMIT = 5;
    private static final String OUTGOING_SMS_EXCEPTION_PATTERN = "outgoing_sms_exception_pattern";
    private static final String OUTGOING_SMS_RESTRICTION_PATTERN = "outgoing_sms_restriction_pattern";
    private static final String PHONE_PACKAGE = "com.android.phone";
    private static final int PREMIUM_RULE_USE_BOTH = 3;
    private static final int PREMIUM_RULE_USE_NETWORK = 2;
    private static final int PREMIUM_RULE_USE_SIM = 1;
    private static final String RESOURCE_TYPE_STRING = "string";
    private static final String SEND_NEXT_MSG_EXTRA = "SendNextMsg";
    private static final int SEND_RETRY_DELAY = 2000;
    private static final int SEND_SMS_OVERlOAD_COUNT = 50;
    private static final int SEND_SMS_OVERlOAD_DURATION_TIMEOUT = 3600000;
    private static final int SINGLE_PART_SMS = 1;
    private static final String SYSTEM_MANAGER_PROCESS_NAME = "com.huawei.systemmanager:service";
    static final String TAG = "SMSDispatcher";
    private static final String TELECOM_PACKAGE_NAME = "com.android.server.telecom";
    private static final boolean TIP_PREMIUM_SHORT_CODE = SystemProperties.getBoolean("ro.huawei.flag.tip_premium", true);
    private static int sConcatenatedRef = new Random().nextInt(256);
    protected final ArrayList<SmsTracker> deliveryPendingList = new ArrayList();
    protected AtomicInteger mAlreadySentSms = new AtomicInteger(0);
    private String mCallingPackage = "";
    protected final CommandsInterface mCi;
    protected final Context mContext;
    private final ArraySet<String> mPackageSendSmsCount = new ArraySet();
    private int mPendingTrackerCount;
    protected Phone mPhone;
    private final AtomicInteger mPremiumSmsRule = new AtomicInteger(1);
    protected final ContentResolver mResolver;
    private final SettingsObserver mSettingsObserver;
    protected boolean mSmsCapable = true;
    protected SmsDispatchersController mSmsDispatchersController;
    protected boolean mSmsSendDisabled;
    protected final TelephonyManager mTelephonyManager;
    private Toast mToast = null;

    protected final class ConfirmDialogListener implements OnClickListener, OnCancelListener, OnCheckedChangeListener {
        private static final int NEVER_ALLOW = 1;
        private static final int RATE_LIMIT = 1;
        private static final int SHORT_CODE_MSG = 0;
        private int mConfirmationType;
        private Button mNegativeButton;
        private Button mPositiveButton;
        private boolean mRememberChoice;
        private final TextView mRememberUndoInstruction;
        private final SmsTracker mTracker;

        ConfirmDialogListener(SmsTracker tracker, TextView textView, int confirmationType) {
            this.mTracker = tracker;
            this.mRememberUndoInstruction = textView;
            this.mConfirmationType = confirmationType;
        }

        void setPositiveButton(Button button) {
            this.mPositiveButton = button;
        }

        void setNegativeButton(Button button) {
            this.mNegativeButton = button;
        }

        public void onClick(DialogInterface dialog, int which) {
            int newSmsPermission = 1;
            int i = -1;
            if (which == -1) {
                Rlog.d(SMSDispatcher.TAG, "CONFIRM sending SMS");
                if (this.mTracker.mAppInfo.applicationInfo != null) {
                    i = this.mTracker.mAppInfo.applicationInfo.uid;
                }
                EventLog.writeEvent(EventLogTags.EXP_DET_SMS_SENT_BY_USER, i);
                SMSDispatcher.this.sendMessage(SMSDispatcher.this.obtainMessage(5, this.mTracker));
                if (this.mRememberChoice) {
                    newSmsPermission = 3;
                }
            } else if (which == -2) {
                Rlog.d(SMSDispatcher.TAG, "DENY sending SMS");
                if (this.mTracker.mAppInfo.applicationInfo != null) {
                    i = this.mTracker.mAppInfo.applicationInfo.uid;
                }
                EventLog.writeEvent(EventLogTags.EXP_DET_SMS_DENIED_BY_USER, i);
                Message msg = SMSDispatcher.this.obtainMessage(7, this.mTracker);
                msg.arg1 = this.mConfirmationType;
                if (this.mRememberChoice) {
                    newSmsPermission = 2;
                    msg.arg2 = 1;
                }
                SMSDispatcher.this.sendMessage(msg);
            }
            SMSDispatcher.this.mSmsDispatchersController.setPremiumSmsPermission(this.mTracker.getAppPackageName(), newSmsPermission);
        }

        public void onCancel(DialogInterface dialog) {
            Rlog.d(SMSDispatcher.TAG, "dialog dismissed: don't send SMS");
            Message msg = SMSDispatcher.this.obtainMessage(7, this.mTracker);
            msg.arg1 = this.mConfirmationType;
            SMSDispatcher.this.sendMessage(msg);
        }

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remember this choice: ");
            stringBuilder.append(isChecked);
            Rlog.d(str, stringBuilder.toString());
            this.mRememberChoice = isChecked;
            if (isChecked) {
                this.mPositiveButton.setText(17041132);
                this.mNegativeButton.setText(17041135);
                if (this.mRememberUndoInstruction != null) {
                    this.mRememberUndoInstruction.setText(17041138);
                    this.mRememberUndoInstruction.setPadding(0, 0, 0, 32);
                    return;
                }
                return;
            }
            this.mPositiveButton.setText(17041131);
            this.mNegativeButton.setText(17041133);
            if (this.mRememberUndoInstruction != null) {
                this.mRememberUndoInstruction.setText("");
                this.mRememberUndoInstruction.setPadding(0, 0, 0, 0);
            }
        }
    }

    private final class MultipartSmsSenderCallback extends Stub {
        private final MultipartSmsSender mSmsSender;

        MultipartSmsSenderCallback(MultipartSmsSender smsSender) {
            this.mSmsSender = smsSender;
        }

        public void onSendSmsComplete(int result, int messageRef) {
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected onSendSmsComplete call with result: ");
            stringBuilder.append(result);
            Rlog.e(str, stringBuilder.toString());
        }

        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            this.mSmsSender.disposeConnection(SMSDispatcher.this.mContext);
            if (this.mSmsSender.mTrackers == null) {
                Rlog.e(SMSDispatcher.TAG, "Unexpected onSendMultipartSmsComplete call with null trackers.");
                return;
            }
            SMSDispatcher.this.checkCallerIsPhoneOrCarrierApp();
            long identity = Binder.clearCallingIdentity();
            int i = 0;
            while (i < this.mSmsSender.mTrackers.length) {
                try {
                    int messageRef = 0;
                    if (messageRefs != null && messageRefs.length > i) {
                        messageRef = messageRefs[i];
                    }
                    SMSDispatcher.this.processSendSmsResponse(this.mSmsSender.mTrackers[i], result, messageRef);
                    i++;
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onFilterComplete(int result) {
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected onFilterComplete call with result: ");
            stringBuilder.append(result);
            Rlog.e(str, stringBuilder.toString());
        }

        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected onSendMmsComplete call with result: ");
            stringBuilder.append(result);
            Rlog.e(str, stringBuilder.toString());
        }

        public void onDownloadMmsComplete(int result) {
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected onDownloadMmsComplete call with result: ");
            stringBuilder.append(result);
            Rlog.e(str, stringBuilder.toString());
        }
    }

    private static class SettingsObserver extends ContentObserver {
        private final Context mContext;
        private final AtomicInteger mPremiumSmsRule;

        SettingsObserver(Handler handler, AtomicInteger premiumSmsRule, Context context) {
            super(handler);
            this.mPremiumSmsRule = premiumSmsRule;
            this.mContext = context;
            onChange(false);
        }

        public void onChange(boolean selfChange) {
            this.mPremiumSmsRule.set(Global.getInt(this.mContext.getContentResolver(), "sms_short_code_rule", 1));
        }
    }

    protected final class SmsSenderCallback extends Stub {
        private final SmsSender mSmsSender;

        public SmsSenderCallback(SmsSender smsSender) {
            this.mSmsSender = smsSender;
        }

        public void onSendSmsComplete(int result, int messageRef) {
            SMSDispatcher.this.checkCallerIsPhoneOrCarrierApp();
            long identity = Binder.clearCallingIdentity();
            try {
                this.mSmsSender.disposeConnection(SMSDispatcher.this.mContext);
                SMSDispatcher.this.processSendSmsResponse(this.mSmsSender.mTracker, result, messageRef);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected onSendMultipartSmsComplete call with result: ");
            stringBuilder.append(result);
            Rlog.e(str, stringBuilder.toString());
        }

        public void onFilterComplete(int result) {
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected onFilterComplete call with result: ");
            stringBuilder.append(result);
            Rlog.e(str, stringBuilder.toString());
        }

        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected onSendMmsComplete call with result: ");
            stringBuilder.append(result);
            Rlog.e(str, stringBuilder.toString());
        }

        public void onDownloadMmsComplete(int result) {
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected onDownloadMmsComplete call with result: ");
            stringBuilder.append(result);
            Rlog.e(str, stringBuilder.toString());
        }
    }

    public static class SmsTracker {
        private AtomicBoolean mAnyPartFailed;
        public final PackageInfo mAppInfo;
        private final HashMap<String, Object> mData;
        public final PendingIntent mDeliveryIntent;
        public final String mDestAddress;
        public boolean mExpectMore;
        String mFormat;
        private String mFullMessageText;
        public int mImsRetry;
        private boolean mIsSinglePartOrLastPart;
        private boolean mIsText;
        public int mMessageRef;
        public Uri mMessageUri;
        private boolean mPersistMessage;
        public int mPriority;
        public int mRetryCount;
        public final PendingIntent mSentIntent;
        public final SmsHeader mSmsHeader;
        private int mSubId;
        private AtomicInteger mUnsentPartCount;
        private final int mUserId;
        public boolean mUsesImsServiceForIms;
        public int mValidityPeriod;

        private SmsTracker(HashMap<String, Object> data, PendingIntent sentIntent, PendingIntent deliveryIntent, PackageInfo appInfo, String destAddr, String format, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, SmsHeader smsHeader, boolean expectMore, String fullMessageText, int subId, boolean isText, boolean persistMessage, int userId, int priority, int validityPeriod) {
            this.mData = data;
            this.mSentIntent = sentIntent;
            this.mDeliveryIntent = deliveryIntent;
            this.mRetryCount = 0;
            this.mAppInfo = appInfo;
            this.mDestAddress = destAddr;
            this.mFormat = format;
            this.mExpectMore = expectMore;
            this.mImsRetry = 0;
            this.mUsesImsServiceForIms = false;
            this.mMessageRef = 0;
            this.mUnsentPartCount = unsentPartCount;
            this.mAnyPartFailed = anyPartFailed;
            this.mMessageUri = messageUri;
            this.mSmsHeader = smsHeader;
            this.mFullMessageText = fullMessageText;
            this.mSubId = subId;
            this.mIsText = isText;
            this.mPersistMessage = persistMessage;
            this.mUserId = userId;
            this.mPriority = priority;
            this.mValidityPeriod = validityPeriod;
        }

        boolean isMultipart() {
            return this.mData.containsKey("parts");
        }

        public HashMap<String, Object> getData() {
            return this.mData;
        }

        public String getAppPackageName() {
            return this.mAppInfo != null ? this.mAppInfo.packageName : null;
        }

        public void updateSentMessageStatus(Context context, int status) {
            if (this.mMessageUri != null) {
                ContentValues values = new ContentValues(1);
                values.put("status", Integer.valueOf(status));
                SqliteWrapper.update(context, context.getContentResolver(), this.mMessageUri, values, null, null);
            }
        }

        private void updateMessageState(Context context, int messageType, int errorCode) {
            if (this.mMessageUri != null) {
                ContentValues values = new ContentValues(2);
                values.put("type", Integer.valueOf(messageType));
                values.put("error_code", Integer.valueOf(errorCode));
                long identity = Binder.clearCallingIdentity();
                try {
                    if (SqliteWrapper.update(context, context.getContentResolver(), this.mMessageUri, values, null, null) != 1) {
                        String str = SMSDispatcher.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to move message to ");
                        stringBuilder.append(messageType);
                        Rlog.e(str, stringBuilder.toString());
                    }
                    Binder.restoreCallingIdentity(identity);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        private boolean isSentSmsFromRejectCall() {
            if (this.mSentIntent == null || this.mSentIntent.getIntent() == null || !SMSDispatcher.TELECOM_PACKAGE_NAME.equals(this.mSentIntent.getIntent().getStringExtra("packageName"))) {
                return false;
            }
            Rlog.d(SMSDispatcher.TAG, "isSentSmsFromRejectCall");
            return true;
        }

        private Uri persistSentMessageIfRequired(Context context, int messageType, int errorCode) {
            if (!this.mIsText || !this.mPersistMessage || (!isSentSmsFromRejectCall() && this.mAppInfo != null && !HwTelephonyFactory.getHwInnerSmsManager().checkShouldWriteSmsPackage(this.mAppInfo.packageName, context))) {
                return null;
            }
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Persist SMS into ");
            stringBuilder.append(messageType == 5 ? "FAILED" : "SENT");
            Rlog.d(str, stringBuilder.toString());
            ContentValues values = new ContentValues();
            values.put("sub_id", Integer.valueOf(this.mSubId));
            values.put("address", this.mDestAddress);
            values.put("body", this.mFullMessageText);
            values.put("date", Long.valueOf(System.currentTimeMillis()));
            values.put("seen", Integer.valueOf(1));
            values.put("read", Integer.valueOf(1));
            String creator = this.mAppInfo != null ? this.mAppInfo.packageName : null;
            if (!TextUtils.isEmpty(creator)) {
                values.put("creator", creator);
            }
            if (this.mDeliveryIntent != null) {
                values.put("status", Integer.valueOf(32));
            }
            if (errorCode != 0) {
                values.put("error_code", Integer.valueOf(errorCode));
            }
            long identity = Binder.clearCallingIdentity();
            ContentResolver resolver = context.getContentResolver();
            try {
                Uri uri = resolver.insert(Sent.CONTENT_URI, values);
                if (uri != null && messageType == 5) {
                    ContentValues updateValues = new ContentValues(1);
                    updateValues.put("type", Integer.valueOf(5));
                    resolver.update(uri, updateValues, null, null);
                }
                Binder.restoreCallingIdentity(identity);
                return uri;
            } catch (Exception e) {
                Rlog.e(SMSDispatcher.TAG, "writeOutboxMessage: Failed to persist outbox message", e);
                Binder.restoreCallingIdentity(identity);
                return null;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }

        private void persistOrUpdateMessage(Context context, int messageType, int errorCode) {
            if (this.mMessageUri != null) {
                updateMessageState(context, messageType, errorCode);
            } else {
                this.mMessageUri = persistSentMessageIfRequired(context, messageType, errorCode);
            }
        }

        public void onFailed(Context context, int error, int errorCode) {
            if (this.mAnyPartFailed != null) {
                this.mAnyPartFailed.set(true);
            }
            boolean isSinglePartOrLastPart = true;
            if (this.mUnsentPartCount != null) {
                isSinglePartOrLastPart = this.mUnsentPartCount.decrementAndGet() == 0;
            }
            if (isSinglePartOrLastPart) {
                persistOrUpdateMessage(context, 5, errorCode);
            }
            if (this.mSentIntent != null) {
                try {
                    Intent fillIn = new Intent();
                    if (this.mMessageUri != null) {
                        fillIn.putExtra("uri", this.mMessageUri.toString());
                    }
                    if (errorCode != 0) {
                        fillIn.putExtra("errorCode", errorCode);
                    }
                    if (this.mUnsentPartCount != null && isSinglePartOrLastPart) {
                        fillIn.putExtra(SMSDispatcher.SEND_NEXT_MSG_EXTRA, true);
                    }
                    this.mSentIntent.send(context, error, fillIn);
                } catch (CanceledException e) {
                    Rlog.e(SMSDispatcher.TAG, "Failed to send result");
                }
            }
        }

        public void onSent(Context context) {
            boolean isSinglePartOrLastPart = true;
            if (this.mUnsentPartCount != null) {
                isSinglePartOrLastPart = this.mUnsentPartCount.decrementAndGet() == 0;
            }
            this.mIsSinglePartOrLastPart = isSinglePartOrLastPart;
            if (isSinglePartOrLastPart) {
                int messageType = 2;
                if (this.mAnyPartFailed != null && this.mAnyPartFailed.get()) {
                    messageType = 5;
                }
                persistOrUpdateMessage(context, messageType, 0);
            }
            if (this.mSentIntent != null) {
                try {
                    Intent fillIn = new Intent();
                    if (this.mMessageUri != null) {
                        fillIn.putExtra("uri", this.mMessageUri.toString());
                    }
                    if (this.mUnsentPartCount != null && isSinglePartOrLastPart) {
                        fillIn.putExtra(SMSDispatcher.SEND_NEXT_MSG_EXTRA, true);
                    }
                    this.mSentIntent.send(context, -1, fillIn);
                } catch (CanceledException e) {
                    Rlog.e(SMSDispatcher.TAG, "Failed to send result");
                }
            }
        }

        public boolean isSinglePartOrLastPart() {
            String str = SMSDispatcher.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mIsSinglePartOrLastPart: ");
            stringBuilder.append(this.mIsSinglePartOrLastPart);
            Rlog.d(str, stringBuilder.toString());
            return this.mIsSinglePartOrLastPart;
        }
    }

    private final class MultipartSmsSender extends CarrierMessagingServiceManager {
        private final List<String> mParts;
        private volatile MultipartSmsSenderCallback mSenderCallback;
        public final SmsTracker[] mTrackers;

        MultipartSmsSender(ArrayList<String> parts, SmsTracker[] trackers) {
            this.mParts = parts;
            this.mTrackers = trackers;
        }

        void sendSmsByCarrierApp(String carrierPackageName, MultipartSmsSenderCallback senderCallback) {
            this.mSenderCallback = senderCallback;
            if (bindToCarrierMessagingService(SMSDispatcher.this.mContext, carrierPackageName)) {
                Rlog.d(SMSDispatcher.TAG, "bindService() for carrier messaging service succeeded");
                return;
            }
            Rlog.e(SMSDispatcher.TAG, "bindService() for carrier messaging service failed");
            this.mSenderCallback.onSendMultipartSmsComplete(1, null);
        }

        protected void onServiceReady(ICarrierMessagingService carrierMessagingService) {
            try {
                carrierMessagingService.sendMultipartTextSms(this.mParts, SMSDispatcher.this.getSubId(), this.mTrackers[0].mDestAddress, SMSDispatcher.getSendSmsFlag(this.mTrackers[0].mDeliveryIntent), this.mSenderCallback);
            } catch (RemoteException e) {
                String str = SMSDispatcher.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception sending the SMS: ");
                stringBuilder.append(e);
                Rlog.e(str, stringBuilder.toString());
                this.mSenderCallback.onSendMultipartSmsComplete(1, null);
            }
        }
    }

    protected abstract class SmsSender extends CarrierMessagingServiceManager {
        protected volatile SmsSenderCallback mSenderCallback;
        protected final SmsTracker mTracker;

        protected SmsSender(SmsTracker tracker) {
            this.mTracker = tracker;
        }

        public void sendSmsByCarrierApp(String carrierPackageName, SmsSenderCallback senderCallback) {
            this.mSenderCallback = senderCallback;
            if (bindToCarrierMessagingService(SMSDispatcher.this.mContext, carrierPackageName)) {
                Rlog.d(SMSDispatcher.TAG, "bindService() for carrier messaging service succeeded");
                return;
            }
            Rlog.e(SMSDispatcher.TAG, "bindService() for carrier messaging service failed");
            this.mSenderCallback.onSendSmsComplete(1, 0);
        }
    }

    protected final class DataSmsSender extends SmsSender {
        public DataSmsSender(SmsTracker tracker) {
            super(tracker);
        }

        protected void onServiceReady(ICarrierMessagingService carrierMessagingService) {
            HashMap<String, Object> map = this.mTracker.getData();
            byte[] data = (byte[]) map.get(SMSDispatcher.MAP_KEY_DATA);
            int destPort = ((Integer) map.get(SMSDispatcher.MAP_KEY_DEST_PORT)).intValue();
            if (data != null) {
                try {
                    carrierMessagingService.sendDataSms(data, SMSDispatcher.this.getSubId(), this.mTracker.mDestAddress, destPort, SMSDispatcher.getSendSmsFlag(this.mTracker.mDeliveryIntent), this.mSenderCallback);
                    return;
                } catch (RemoteException e) {
                    String str = SMSDispatcher.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception sending the SMS: ");
                    stringBuilder.append(e);
                    Rlog.e(str, stringBuilder.toString());
                    this.mSenderCallback.onSendSmsComplete(1, 0);
                    return;
                }
            }
            this.mSenderCallback.onSendSmsComplete(1, 0);
        }
    }

    protected final class TextSmsSender extends SmsSender {
        public TextSmsSender(SmsTracker tracker) {
            super(tracker);
        }

        protected void onServiceReady(ICarrierMessagingService carrierMessagingService) {
            String text = (String) this.mTracker.getData().get(SMSDispatcher.MAP_KEY_TEXT);
            if (text != null) {
                try {
                    carrierMessagingService.sendTextSms(text, SMSDispatcher.this.getSubId(), this.mTracker.mDestAddress, SMSDispatcher.getSendSmsFlag(this.mTracker.mDeliveryIntent), this.mSenderCallback);
                    return;
                } catch (RemoteException e) {
                    String str = SMSDispatcher.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception sending the SMS: ");
                    stringBuilder.append(e);
                    Rlog.e(str, stringBuilder.toString());
                    this.mSenderCallback.onSendSmsComplete(1, 0);
                    return;
                }
            }
            this.mSenderCallback.onSendSmsComplete(1, 0);
        }
    }

    protected abstract TextEncodingDetails calculateLength(CharSequence charSequence, boolean z);

    protected abstract String getFormat();

    protected abstract SubmitPduBase getSubmitPdu(String str, String str2, int i, byte[] bArr, boolean z);

    protected abstract SubmitPduBase getSubmitPdu(String str, String str2, String str3, boolean z, SmsHeader smsHeader, int i, int i2);

    protected abstract void sendSms(SmsTracker smsTracker);

    protected abstract boolean shouldBlockSmsForEcbm();

    protected void triggerSendSmsOverLoadCheck() {
        this.mAlreadySentSms.getAndIncrement();
        if (1 == this.mAlreadySentSms.get()) {
            removeMessages(20);
            sendMessageDelayed(obtainMessage(20), 3600000);
        }
    }

    private void handleSendSmsOverLoad() {
        if (this.mAlreadySentSms.get() > 50) {
            String defaultSmsApplicationName = "";
            StringBuffer buf = new StringBuffer();
            ComponentName componentName = SmsApplication.getDefaultSmsApplication(this.mContext, false);
            if (componentName != null) {
                defaultSmsApplicationName = componentName.getPackageName();
            }
            buf.append(defaultSmsApplicationName);
            buf.append(" SendSmsOverLoad");
            int sendSmsCount = this.mPackageSendSmsCount.size();
            boolean findThirdApp = false;
            for (int i = 0; i < sendSmsCount; i++) {
                String packageName = (String) this.mPackageSendSmsCount.valueAt(i);
                if (!(defaultSmsApplicationName.equals(packageName) || DEFAULT_SMS_APPLICATION.equals(packageName))) {
                    buf.append(" ");
                    buf.append(packageName);
                    findThirdApp = true;
                }
            }
            if (findThirdApp) {
                HwRadarUtils.report(this.mContext, HwRadarUtils.ERR_SMS_SEND_BACKGROUND, buf.toString(), getSubId());
            }
        }
        this.mAlreadySentSms.set(0);
        this.mPackageSendSmsCount.clear();
    }

    protected static int getNextConcatenatedRef() {
        sConcatenatedRef++;
        return sConcatenatedRef;
    }

    protected SMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        this.mPhone = phone;
        this.mSmsDispatchersController = smsDispatchersController;
        this.mContext = phone.getContext();
        this.mResolver = this.mContext.getContentResolver();
        this.mCi = phone.mCi;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mSettingsObserver = new SettingsObserver(this, this.mPremiumSmsRule, this.mContext);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("sms_short_code_rule"), false, this.mSettingsObserver);
        this.mSmsCapable = this.mContext.getResources().getBoolean(17957026);
        this.mSmsSendDisabled = this.mTelephonyManager.getSmsSendCapableForPhone(this.mPhone.getPhoneId(), this.mSmsCapable) ^ 1;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SMSDispatcher: ctor mSmsCapable=");
        stringBuilder.append(this.mSmsCapable);
        stringBuilder.append(" format=");
        stringBuilder.append(getFormat());
        stringBuilder.append(" mSmsSendDisabled=");
        stringBuilder.append(this.mSmsSendDisabled);
        Rlog.d(str, stringBuilder.toString());
    }

    protected void updatePhoneObject(Phone phone) {
        this.mPhone = phone;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Active phone changed to ");
        stringBuilder.append(this.mPhone.getPhoneName());
        Rlog.d(str, stringBuilder.toString());
    }

    public void dispose() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
    }

    protected void handleStatusReport(Object o) {
        Rlog.d(TAG, "handleStatusReport() called with no subclass.");
    }

    public void handleMessage(Message msg) {
        int i = msg.what;
        if (i == 17) {
            if (this.mToast != null) {
                this.mToast.cancel();
            }
            this.mToast = Toast.makeText(this.mContext, 17041260, 1);
            this.mToast.show();
        } else if (i != 20) {
            SmsTracker tracker;
            switch (i) {
                case 2:
                    handleSendComplete((AsyncResult) msg.obj);
                    return;
                case 3:
                    Rlog.d(TAG, "SMS retry..");
                    sendSmsImmediately((SmsTracker) msg.obj);
                    return;
                case 4:
                    handleReachSentLimit((SmsTracker) msg.obj);
                    return;
                case 5:
                    tracker = (SmsTracker) msg.obj;
                    if (tracker.isMultipart()) {
                        sendMultipartSms(tracker);
                    } else {
                        if (this.mPendingTrackerCount > 1) {
                            tracker.mExpectMore = true;
                        } else {
                            tracker.mExpectMore = false;
                        }
                        sendSms(tracker);
                    }
                    this.mPendingTrackerCount--;
                    return;
                default:
                    switch (i) {
                        case 7:
                            tracker = msg.obj;
                            if (msg.arg1 == 0) {
                                if (msg.arg2 == 1) {
                                    tracker.onFailed(this.mContext, 8, 0);
                                    Rlog.d(TAG, "SMSDispatcher: EVENT_STOP_SENDING - sending SHORT_CODE_NEVER_ALLOWED error code.");
                                } else {
                                    tracker.onFailed(this.mContext, 7, 0);
                                    Rlog.d(TAG, "SMSDispatcher: EVENT_STOP_SENDING - sending SHORT_CODE_NOT_ALLOWED error code.");
                                }
                            } else if (msg.arg1 == 1) {
                                tracker.onFailed(this.mContext, 5, 0);
                                Rlog.d(TAG, "SMSDispatcher: EVENT_STOP_SENDING - sending LIMIT_EXCEEDED error code.");
                            } else {
                                Rlog.e(TAG, "SMSDispatcher: EVENT_STOP_SENDING - unexpected cases.");
                            }
                            this.mPendingTrackerCount--;
                            return;
                        case 8:
                            handleConfirmShortCode(false, (SmsTracker) msg.obj);
                            return;
                        case 9:
                            handleConfirmShortCode(true, (SmsTracker) msg.obj);
                            return;
                        case 10:
                            handleStatusReport(msg.obj);
                            return;
                        default:
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("handleMessage() ignoring message of unexpected type ");
                            stringBuilder.append(msg.what);
                            Rlog.e(str, stringBuilder.toString());
                            return;
                    }
            }
        } else {
            handleSendSmsOverLoad();
        }
    }

    private static int getSendSmsFlag(PendingIntent deliveryIntent) {
        if (deliveryIntent == null) {
            return 0;
        }
        return 1;
    }

    private void processSendSmsResponse(SmsTracker tracker, int result, int messageRef) {
        if (tracker == null) {
            Rlog.e(TAG, "processSendSmsResponse: null tracker");
            return;
        }
        SmsResponse smsResponse = new SmsResponse(messageRef, null, -1);
        switch (result) {
            case 0:
                Rlog.d(TAG, "Sending SMS by IP succeeded.");
                sendMessage(obtainMessage(2, new AsyncResult(tracker, smsResponse, null)));
                break;
            case 1:
                Rlog.d(TAG, "Sending SMS by IP failed. Retry on carrier network.");
                sendSubmitPdu(tracker);
                break;
            case 2:
                Rlog.d(TAG, "Sending SMS by IP failed.");
                sendMessage(obtainMessage(2, new AsyncResult(tracker, smsResponse, new CommandException(Error.GENERIC_FAILURE))));
                break;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown result ");
                stringBuilder.append(result);
                stringBuilder.append(" Retry on carrier network.");
                Rlog.d(str, stringBuilder.toString());
                sendSubmitPdu(tracker);
                break;
        }
    }

    private void sendSubmitPdu(SmsTracker tracker) {
        if (shouldBlockSmsForEcbm()) {
            Rlog.d(TAG, "Block SMS in Emergency Callback mode");
            tracker.onFailed(this.mContext, 4, 0);
            return;
        }
        sendRawPdu(tracker);
    }

    protected void handleSendComplete(AsyncResult ar) {
        SmsTracker tracker = ar.userObj;
        PendingIntent sentIntent = tracker.mSentIntent;
        if (ar.result != null) {
            tracker.mMessageRef = ((SmsResponse) ar.result).mMessageRef;
        } else {
            Rlog.d(TAG, "SmsResponse was null");
        }
        if (ar.exception == null) {
            if (tracker.mDeliveryIntent != null) {
                this.deliveryPendingList.add(tracker);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("deliveryPendingList add mMessageRef: ");
                stringBuilder.append(tracker.mMessageRef);
                Rlog.d(str, stringBuilder.toString());
            }
            tracker.onSent(this.mContext);
            if (HwTelephonyFactory.getHwInnerSmsManager().isLimitNumOfSmsEnabled(true) && tracker.isSinglePartOrLastPart() && !PHONE_PACKAGE.equals(tracker.mAppInfo.packageName)) {
                HwTelephonyFactory.getHwInnerSmsManager().updateSmsUsedNum(this.mContext, true);
            }
        } else {
            String str2;
            int ss = this.mPhone.getServiceState().getState();
            if (tracker.mImsRetry > 0 && ss != 0) {
                tracker.mRetryCount = isViaAndCdma() ? 3 : MAX_SEND_RETRIES;
                str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("handleSendComplete: Skipping retry:  isIms()=");
                stringBuilder2.append(isIms());
                stringBuilder2.append(" mRetryCount=");
                stringBuilder2.append(tracker.mRetryCount);
                stringBuilder2.append(" mImsRetry=");
                stringBuilder2.append(tracker.mImsRetry);
                stringBuilder2.append(" mMessageRef=");
                stringBuilder2.append(tracker.mMessageRef);
                stringBuilder2.append(" SS= ");
                stringBuilder2.append(this.mPhone.getServiceState().getState());
                Rlog.d(str2, stringBuilder2.toString());
            }
            if (isIms() || ss == 0) {
                if (((CommandException) ar.exception).getCommandError() == Error.SMS_FAIL_RETRY) {
                    if (tracker.mRetryCount < (isViaAndCdma() ? 3 : MAX_SEND_RETRIES)) {
                        tracker.mRetryCount++;
                        sendMessageDelayed(obtainMessage(3, tracker), 2000);
                        str2 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("handleSendComplete: retry for Message  mRetryCount=");
                        stringBuilder3.append(tracker.mRetryCount);
                        stringBuilder3.append(" mMessageRef=");
                        stringBuilder3.append(tracker.mMessageRef);
                        Rlog.d(str2, stringBuilder3.toString());
                        return;
                    }
                }
                int errorCode = 0;
                if (ar.result != null) {
                    errorCode = ((SmsResponse) ar.result).mErrorCode;
                }
                int error = 1;
                if (((CommandException) ar.exception).getCommandError() == Error.FDN_CHECK_FAILURE) {
                    error = 6;
                }
                tracker.onFailed(this.mContext, error, errorCode);
                Context context = this.mContext;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("sms send fail:");
                stringBuilder4.append(errorCode);
                HwRadarUtils.report(context, HwRadarUtils.ERR_SMS_SEND, stringBuilder4.toString(), tracker.mSubId);
            } else {
                tracker.onFailed(this.mContext, getNotInServiceError(ss), 0);
            }
        }
        Rlog.d(TAG, "handleSendComplete send for next message");
        sendSmsSendingTimeOutMessageDelayed(tracker);
    }

    protected static void handleNotInService(int ss, PendingIntent sentIntent) {
        if (sentIntent == null) {
            return;
        }
        if (ss == 3) {
            try {
                sentIntent.send(2);
                return;
            } catch (CanceledException e) {
                Rlog.e(TAG, "Failed to send result");
                return;
            }
        }
        sentIntent.send(4);
    }

    protected static int getNotInServiceError(int ss) {
        if (ss == 3) {
            return 2;
        }
        return 4;
    }

    protected void sendData(String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        SubmitPduBase pdu = getSubmitPdu(scAddr, destAddr, destPort, data, deliveryIntent != null);
        if (pdu != null) {
            SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, destPort, data, pdu), sentIntent, deliveryIntent, getFormat(), null, false, null, false, true);
            if (!sendSmsByCarrierApp(true, tracker)) {
                sendSubmitPdu(tracker);
            }
            PendingIntent pendingIntent = sentIntent;
            return;
        }
        Rlog.e(TAG, "SMSDispatcher.sendData(): getSubmitPdu() returned null");
        triggerSentIntentForFailure(sentIntent);
    }

    public void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, Uri messageUri, String callingPkg, boolean persistMessage, int priority, boolean expectMore, int validityPeriod) {
        Rlog.d(TAG, "sendText");
        this.mCallingPackage = callingPkg;
        SubmitPduBase pdu = getSubmitPdu(scAddr, destAddr, text, deliveryIntent != null, null, priority, validityPeriod);
        if (pdu != null) {
            String str = text;
            SmsTracker tracker = getSmsTracker(getSmsTrackerMap(destAddr, scAddr, str, pdu), sentIntent, deliveryIntent, getFormat(), messageUri, expectMore, str, true, persistMessage, priority, validityPeriod);
            if (!sendSmsByCarrierApp(false, tracker)) {
                sendSubmitPdu(tracker);
            }
            PendingIntent pendingIntent = sentIntent;
            return;
        }
        Rlog.e(TAG, "SmsDispatcher.sendText(): getSubmitPdu() returned null");
        triggerSentIntentForFailure(sentIntent);
    }

    private void triggerSentIntentForFailure(PendingIntent sentIntent) {
        if (sentIntent != null) {
            try {
                sentIntent.send(1);
            } catch (CanceledException e) {
                Rlog.e(TAG, "Intent has been canceled!");
            }
        }
    }

    private boolean sendSmsByCarrierApp(boolean isDataSms, SmsTracker tracker) {
        String carrierPackage = getCarrierAppPackageName();
        if (carrierPackage == null) {
            return false;
        }
        SmsSender smsSender;
        Rlog.d(TAG, "Found carrier package.");
        if (isDataSms) {
            smsSender = new DataSmsSender(tracker);
        } else {
            smsSender = new TextSmsSender(tracker);
        }
        smsSender.sendSmsByCarrierApp(carrierPackage, new SmsSenderCallback(smsSender));
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x00a8  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00a5  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void sendMultipartText(String destAddr, String scAddr, ArrayList<String> parts, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg, boolean persistMessage, int priority, boolean expectMore, int validityPeriod) {
        boolean z;
        boolean encoding;
        TextEncodingDetails[] encodingForParts;
        int msgCount;
        int refNumber;
        SmsTracker[] trackers;
        SMSDispatcher sMSDispatcher = this;
        ArrayList arrayList = parts;
        ArrayList arrayList2 = sentIntents;
        ArrayList arrayList3 = deliveryIntents;
        String fullMessageText = sMSDispatcher.getMultipartMessageText(arrayList);
        int refNumber2 = getNextConcatenatedRef() & 255;
        int msgCount2 = parts.size();
        sMSDispatcher.mCallingPackage = callingPkg;
        TextEncodingDetails[] encodingForParts2 = new TextEncodingDetails[msgCount2];
        boolean z2 = false;
        boolean encoding2 = false;
        int i = 0;
        while (true) {
            z = true;
            if (i >= msgCount2) {
                break;
            }
            TextEncodingDetails details = sMSDispatcher.calculateLength((CharSequence) arrayList.get(i), false);
            if (encoding2 != details.codeUnitSize && (!encoding2 || encoding2)) {
                encoding2 = details.codeUnitSize;
            }
            encodingForParts2[i] = details;
            i++;
        }
        SmsTracker[] trackers2 = new SmsTracker[msgCount2];
        AtomicInteger unsentPartCount = new AtomicInteger(msgCount2);
        AtomicBoolean anyPartFailed = new AtomicBoolean(false);
        i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= msgCount2) {
                break;
            }
            PendingIntent sentIntent;
            PendingIntent deliveryIntent;
            int i3;
            SmsTracker[] trackers3;
            boolean z3;
            ArrayList<PendingIntent> arrayList22;
            ArrayList<PendingIntent> arrayList32;
            String str;
            ArrayList<String> arrayList4;
            ConcatRef concatRef = new ConcatRef();
            concatRef.refNumber = refNumber2;
            concatRef.seqNumber = i2 + 1;
            concatRef.msgCount = msgCount2;
            concatRef.isEightBits = z;
            SmsHeader smsHeader = new SmsHeader();
            smsHeader.concatRef = concatRef;
            if (encoding2 == z) {
                smsHeader.languageTable = encodingForParts2[i2].languageTable;
                smsHeader.languageShiftTable = encodingForParts2[i2].languageShiftTable;
            }
            if (arrayList22 != null) {
                if (sentIntents.size() > i2) {
                    sentIntent = (PendingIntent) arrayList22.get(i2);
                    deliveryIntent = null;
                    if (arrayList32 != null && deliveryIntents.size() > i2) {
                        deliveryIntent = (PendingIntent) arrayList32.get(i2);
                    }
                    i3 = i2;
                    trackers3 = trackers2;
                    encoding = encoding2;
                    encodingForParts = encodingForParts2;
                    msgCount = msgCount2;
                    z3 = i2 != msgCount2 + -1;
                    refNumber = refNumber2;
                    trackers = trackers3;
                    trackers[i3] = sMSDispatcher.getNewSubmitPduTracker(destAddr, scAddr, (String) arrayList4.get(i2), smsHeader, encoding, sentIntent, deliveryIntent, z3, unsentPartCount, anyPartFailed, messageUri, fullMessageText, priority, expectMore, validityPeriod);
                    trackers[i3].mPersistMessage = persistMessage;
                    i = i3 + 1;
                    sMSDispatcher = this;
                    arrayList22 = sentIntents;
                    arrayList32 = deliveryIntents;
                    str = callingPkg;
                    trackers2 = trackers;
                    z = true;
                    refNumber2 = refNumber;
                    encoding2 = encoding;
                    z2 = false;
                    encodingForParts2 = encodingForParts;
                    msgCount2 = msgCount;
                    arrayList4 = parts;
                }
            }
            sentIntent = null;
            deliveryIntent = null;
            deliveryIntent = (PendingIntent) arrayList32.get(i2);
            if (i2 != msgCount2 + -1) {
            }
            i3 = i2;
            trackers3 = trackers2;
            encoding = encoding2;
            encodingForParts = encodingForParts2;
            msgCount = msgCount2;
            z3 = i2 != msgCount2 + -1;
            refNumber = refNumber2;
            trackers = trackers3;
            trackers[i3] = sMSDispatcher.getNewSubmitPduTracker(destAddr, scAddr, (String) arrayList4.get(i2), smsHeader, encoding, sentIntent, deliveryIntent, z3, unsentPartCount, anyPartFailed, messageUri, fullMessageText, priority, expectMore, validityPeriod);
            trackers[i3].mPersistMessage = persistMessage;
            i = i3 + 1;
            sMSDispatcher = this;
            arrayList22 = sentIntents;
            arrayList32 = deliveryIntents;
            str = callingPkg;
            trackers2 = trackers;
            z = true;
            refNumber2 = refNumber;
            encoding2 = encoding;
            z2 = false;
            encodingForParts2 = encodingForParts;
            msgCount2 = msgCount;
            arrayList4 = parts;
        }
        trackers = trackers2;
        encoding = encoding2;
        boolean z4 = z2;
        encodingForParts = encodingForParts2;
        msgCount = msgCount2;
        refNumber = refNumber2;
        trackers2 = persistMessage;
        ArrayList<String> arrayList5 = parts;
        String str2;
        if (arrayList5 == null || trackers.length == 0 || trackers[z4] == null) {
            str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot send multipart text. parts=");
            stringBuilder.append(arrayList5);
            stringBuilder.append(" trackers=");
            stringBuilder.append(trackers);
            Rlog.e(str2, stringBuilder.toString());
            return;
        }
        str2 = getCarrierAppPackageName();
        if (str2 != null) {
            Rlog.d(TAG, "Found carrier package.");
            MultipartSmsSender smsSender = new MultipartSmsSender(arrayList5, trackers);
            smsSender.sendSmsByCarrierApp(str2, new MultipartSmsSenderCallback(smsSender));
        } else {
            Rlog.v(TAG, "No carrier package.");
            int length = trackers.length;
            for (int i4 = z4; i4 < length; i4++) {
                SmsTracker tracker = trackers[i4];
                if (tracker != null) {
                    sendSubmitPdu(tracker);
                } else {
                    Rlog.e(TAG, "Null tracker.");
                }
            }
        }
    }

    private SmsTracker getNewSubmitPduTracker(String destinationAddress, String scAddress, String message, SmsHeader smsHeader, int encoding, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean lastPart, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, String fullMessageText, int priority, boolean expectMore, int validityPeriod) {
        String str = destinationAddress;
        String str2 = scAddress;
        String str3 = message;
        SmsHeader smsHeader2 = smsHeader;
        boolean z = false;
        HashMap map;
        String format;
        if (isCdmaMo()) {
            UserData uData = new UserData();
            uData.payloadStr = str3;
            uData.userDataHeader = smsHeader2;
            if (encoding == 1) {
                uData.msgEncoding = 9;
            } else {
                uData.msgEncoding = 4;
            }
            uData.msgEncodingSet = true;
            boolean z2 = deliveryIntent != null && lastPart;
            SubmitPduBase submitPdu = SmsMessage.getSubmitPdu(str, uData, z2, priority);
            map = getSmsTrackerMap(str, str2, str3, submitPdu);
            format = getFormat();
            if (!lastPart || expectMore) {
                z = true;
            }
            return getSmsTracker(map, sentIntent, deliveryIntent, format, unsentPartCount, anyPartFailed, messageUri, smsHeader2, z, fullMessageText, true, true, priority, validityPeriod);
        }
        SmsHeader smsHeader3 = smsHeader;
        SubmitPduBase pdu = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, deliveryIntent != null, SmsHeader.toByteArray(smsHeader), encoding, smsHeader3.languageTable, smsHeader3.languageShiftTable, validityPeriod);
        if (pdu != null) {
            map = getSmsTrackerMap(destinationAddress, scAddress, message, pdu);
            format = getFormat();
            if (!lastPart || expectMore) {
                z = true;
            }
            return getSmsTracker(map, sentIntent, deliveryIntent, format, unsentPartCount, anyPartFailed, messageUri, smsHeader3, z, fullMessageText, true, false, priority, validityPeriod);
        }
        Rlog.e(TAG, "GsmSMSDispatcher.sendNewSubmitPdu(): getSubmitPdu() returned null");
        return null;
    }

    @VisibleForTesting
    public void sendRawPdu(SmsTracker tracker) {
        byte[] pdu = (byte[]) tracker.getData().get(MAP_KEY_PDU);
        if (!HwTelephonyFactory.getHwInnerSmsManager().isMatchSMSPattern(tracker.mDestAddress, OUTGOING_SMS_EXCEPTION_PATTERN, tracker.mSubId) && HwTelephonyFactory.getHwInnerSmsManager().isMatchSMSPattern(tracker.mDestAddress, OUTGOING_SMS_RESTRICTION_PATTERN, tracker.mSubId)) {
            Rlog.d(TAG, "Addressin black list");
            sendMessage(obtainMessage(17));
            sendMessage(obtainMessage(7, tracker));
        } else if (this.mSmsSendDisabled) {
            Rlog.e(TAG, "Device does not support sending sms.");
            tracker.onFailed(this.mContext, 4, 0);
        } else if (pdu == null) {
            Rlog.e(TAG, "Empty PDU");
            tracker.onFailed(this.mContext, 3, 0);
        } else {
            PackageManager pm = this.mContext.getPackageManager();
            String[] packageNames = pm.getPackagesForUid(Binder.getCallingUid());
            if (packageNames == null || packageNames.length == 0) {
                Rlog.e(TAG, "Can't get calling app package name: refusing to send SMS");
                tracker.onFailed(this.mContext, 1, 0);
                return;
            }
            try {
                PackageInfo appInfo = pm.getPackageInfoAsUser(packageNames[0], 64, tracker.mUserId);
                if (checkDestination(tracker)) {
                    if (this.mSmsDispatchersController.getUsageMonitor().check(appInfo.packageName, 1)) {
                        sendSms(tracker);
                    } else {
                        sendMessage(obtainMessage(4, tracker));
                        return;
                    }
                }
                if (PhoneNumberUtils.isLocalEmergencyNumber(this.mContext, tracker.mDestAddress)) {
                    new AsyncEmergencyContactNotifier(this.mContext).execute(new Void[0]);
                }
            } catch (NameNotFoundException e) {
                Rlog.e(TAG, "Can't get calling app package info: refusing to send SMS");
                tracker.onFailed(this.mContext, 1, 0);
            }
        }
    }

    boolean checkDestination(SmsTracker tracker) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.SEND_SMS_NO_CONFIRMATION") == 0) {
            return true;
        }
        String simCountryIso;
        int rule = this.mPremiumSmsRule.get();
        int smsCategory = 0;
        String simMccmnc = this.mTelephonyManager.getSimOperator(getSubId());
        if (rule == 1 || rule == 3) {
            simCountryIso = this.mTelephonyManager.getSimCountryIso(getSubId());
            if (simCountryIso == null || simCountryIso.length() != 2) {
                Rlog.e(TAG, "Can't get SIM country Iso: trying network country Iso");
                simCountryIso = this.mTelephonyManager.getNetworkCountryIso(getSubId());
            }
            smsCategory = this.mSmsDispatchersController.getUsageMonitor().checkDestinationHw(tracker.mDestAddress, simCountryIso, simMccmnc);
        }
        if (rule == 2 || rule == 3) {
            simCountryIso = this.mTelephonyManager.getNetworkCountryIso(getSubId());
            if (simCountryIso == null || simCountryIso.length() != 2) {
                Rlog.e(TAG, "Can't get Network country Iso: trying SIM country Iso");
                simCountryIso = this.mTelephonyManager.getSimCountryIso(getSubId());
            }
            smsCategory = SmsUsageMonitor.mergeShortCodeCategories(smsCategory, this.mSmsDispatchersController.getUsageMonitor().checkDestinationHw(tracker.mDestAddress, simCountryIso, simMccmnc));
        }
        if (smsCategory == 0 || smsCategory == 1 || smsCategory == 2) {
            return true;
        }
        if (Global.getInt(this.mResolver, "device_provisioned", 0) == 0) {
            Rlog.e(TAG, "Can't send premium sms during Setup Wizard");
            return false;
        }
        int premiumSmsPermission = this.mSmsDispatchersController.getUsageMonitor().getPremiumSmsPermission(tracker.getAppPackageName());
        if (premiumSmsPermission == 0) {
            premiumSmsPermission = 1;
        }
        switch (premiumSmsPermission) {
            case 2:
                Rlog.w(TAG, "User denied this app from sending to premium SMS");
                Message msg = obtainMessage(7, tracker);
                msg.arg1 = 0;
                msg.arg2 = 1;
                sendMessage(msg);
                return false;
            case 3:
                Rlog.d(TAG, "User approved this app to send to premium SMS");
                return true;
            default:
                boolean shouldIgnoreNotify;
                if (getTipPremiumFromSimValue() || !this.mSmsDispatchersController.getUsageMonitor().isCurrentPatternMatcherNull()) {
                    shouldIgnoreNotify = false;
                } else {
                    shouldIgnoreNotify = true;
                }
                boolean shouldIgnoreNotifyByHplmn = checkCustIgnoreShortCodeTips();
                if (shouldIgnoreNotify || shouldIgnoreNotifyByHplmn) {
                    Rlog.w(TAG, "flag.tip_premium is false, not notify premium msg.");
                    return true;
                }
                int event;
                if (smsCategory == 3) {
                    event = 8;
                } else {
                    event = 9;
                }
                sendMessage(obtainMessage(event, tracker));
                return false;
        }
    }

    private boolean denyIfQueueLimitReached(SmsTracker tracker) {
        if (this.mPendingTrackerCount >= 5) {
            Rlog.e(TAG, "Denied because queue limit reached");
            tracker.onFailed(this.mContext, 5, 0);
            return true;
        }
        this.mPendingTrackerCount++;
        return false;
    }

    private CharSequence getAppLabel(String appPackage, int userId) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            return convertSafeLabel(pm.getApplicationInfoAsUser(appPackage, null, userId).loadLabel(pm).toString(), appPackage);
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PackageManager Name Not Found for package ");
            stringBuilder.append(appPackage);
            Rlog.e(str, stringBuilder.toString());
            return appPackage;
        }
    }

    private CharSequence getMsgAppLabel(String appPackage, int userId) {
        PackageManager pm = this.mContext.getPackageManager();
        Resources lResources = null;
        try {
            int resId = pm.getResourcesForApplicationAsUser(appPackage, userId).getIdentifier(MESSAGE_STRING_NAME, RESOURCE_TYPE_STRING, appPackage);
            CharSequence lmsgApplabel = null;
            if (resId != 0) {
                lmsgApplabel = pm.getText(appPackage, resId, null);
            }
            if (resId == 0 || lmsgApplabel == null) {
                return getAppLabel(appPackage, userId);
            }
            return lmsgApplabel;
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PackageManager Name Not Found for package");
            stringBuilder.append(appPackage);
            Rlog.e(str, stringBuilder.toString());
            return appPackage;
        }
    }

    private CharSequence convertSafeLabel(String labelStr, String appPackage) {
        int labelLength = labelStr.length();
        String labelStr2 = labelStr;
        int offset = 0;
        while (offset < labelLength) {
            int codePoint = labelStr2.codePointAt(offset);
            int type = Character.getType(codePoint);
            if (type == 13 || type == 15 || type == 14) {
                labelStr2 = labelStr2.substring(0, offset);
                break;
            }
            if (type == 12) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(labelStr2.substring(0, offset));
                stringBuilder.append(" ");
                stringBuilder.append(labelStr2.substring(Character.charCount(codePoint) + offset));
                labelStr2 = stringBuilder.toString();
            }
            offset += Character.charCount(codePoint);
        }
        String labelStr3 = labelStr2.trim();
        if (labelStr3.isEmpty()) {
            return appPackage;
        }
        TextPaint paint = new TextPaint();
        paint.setTextSize(42.0f);
        return TextUtils.ellipsize(labelStr3, paint, MAX_LABEL_SIZE_PX, TruncateAt.END);
    }

    protected void handleReachSentLimit(SmsTracker tracker) {
        if (!denyIfQueueLimitReached(tracker)) {
            CharSequence appLabel = getAppLabel(tracker.getAppPackageName(), tracker.mUserId);
            Resources r = Resources.getSystem();
            Spanned messageText = Html.fromHtml(r.getString(17041125, new Object[]{appLabel}));
            ConfirmDialogListener listener = new ConfirmDialogListener(tracker, null, 1);
            AlertDialog d = new Builder(this.mContext, 33947691).setTitle(17041127).setIcon(17301642).setMessage(messageText).setPositiveButton(r.getString(17041128), listener).setNegativeButton(r.getString(17041126), listener).setOnCancelListener(listener).create();
            d.getWindow().setType(2003);
            d.show();
        }
    }

    protected void handleConfirmShortCode(boolean isPremium, SmsTracker tracker) {
        SmsTracker smsTracker = tracker;
        if (!denyIfQueueLimitReached(smsTracker)) {
            int detailsId;
            CharSequence appLabel;
            if (isPremium) {
                detailsId = 17041130;
            } else {
                detailsId = 17041136;
            }
            String packageName = tracker.getAppPackageName();
            if ("com.android.contacts".equals(smsTracker.mAppInfo.packageName)) {
                appLabel = getMsgAppLabel(packageName, tracker.mUserId);
            } else {
                appLabel = getAppLabel(packageName, tracker.mUserId);
            }
            Resources r = Resources.getSystem();
            Spanned messageText = Html.fromHtml(r.getString(17041134, new Object[]{appLabel, smsTracker.mDestAddress}));
            View layout = ((LayoutInflater) new ContextThemeWrapper(this.mContext, r.getIdentifier("androidhwext:style/Theme.Emui", null, null)).getSystemService("layout_inflater")).inflate(17367293, null);
            ConfirmDialogListener listener = new ConfirmDialogListener(smsTracker, (TextView) layout.findViewById(16909347), 0);
            ((TextView) layout.findViewById(16909342)).setText(messageText);
            ((TextView) ((ViewGroup) layout.findViewById(16909343)).findViewById(16909344)).setText(detailsId);
            CheckBox rememberChoice = (CheckBox) layout.findViewById(16909345);
            rememberChoice.setOnCheckedChangeListener(listener);
            AlertDialog d = new Builder(this.mContext, 33947691).setView(layout).setPositiveButton(r.getString(17041131), listener).setNegativeButton(r.getString(17041133), listener).setOnCancelListener(listener).create();
            d.getWindow().setType(2003);
            d.show();
            listener.setPositiveButton(d.getButton(-1));
            listener.setNegativeButton(d.getButton(-2));
        }
    }

    public void sendRetrySms(SmsTracker tracker) {
        if (this.mSmsDispatchersController != null) {
            this.mSmsDispatchersController.sendRetrySms(tracker);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mSmsDispatchersController);
        stringBuilder.append(" is null. Retry failed");
        Rlog.e(str, stringBuilder.toString());
    }

    private void sendMultipartSms(SmsTracker tracker) {
        SmsTracker smsTracker = tracker;
        HashMap<String, Object> map = tracker.getData();
        String destinationAddress = (String) map.get("destination");
        String scAddress = (String) map.get("scaddress");
        ArrayList<String> parts = (ArrayList) map.get("parts");
        ArrayList<PendingIntent> sentIntents = (ArrayList) map.get("sentIntents");
        ArrayList<PendingIntent> deliveryIntents = (ArrayList) map.get("deliveryIntents");
        int ss = this.mPhone.getServiceState().getState();
        if (isIms() || ss == 0) {
            boolean access$400 = tracker.mPersistMessage;
            int i = smsTracker.mPriority;
            boolean z = smsTracker.mExpectMore;
            int i2 = i;
            boolean z2 = z;
            sendMultipartText(destinationAddress, scAddress, parts, sentIntents, deliveryIntents, null, null, access$400, i2, z2, smsTracker.mValidityPeriod);
            return;
        }
        int i3 = 0;
        int count = parts.size();
        while (i3 < count) {
            PendingIntent sentIntent = null;
            if (sentIntents != null && sentIntents.size() > i3) {
                sentIntent = (PendingIntent) sentIntents.get(i3);
            }
            handleNotInService(ss, sentIntent);
            i3++;
        }
    }

    private int getDefaultIdxForCsp(String[] packageNames) {
        if (packageNames.length <= 1) {
            return 0;
        }
        for (int i = 0; i < packageNames.length; i++) {
            if ("com.android.contacts".equals(packageNames[i])) {
                return i;
            }
        }
        return 0;
    }

    protected SmsTracker getSmsTracker(HashMap<String, Object> data, PendingIntent sentIntent, PendingIntent deliveryIntent, String format, AtomicInteger unsentPartCount, AtomicBoolean anyPartFailed, Uri messageUri, SmsHeader smsHeader, boolean expectMore, String fullMessageText, boolean isText, boolean persistMessage, int priority, int validityPeriod) {
        PackageInfo appInfo;
        String str;
        StringBuilder stringBuilder;
        HashMap<String, Object> hashMap;
        int userId;
        PackageManager pm = this.mContext.getPackageManager();
        String[] packageNames = pm.getPackagesForUid(Binder.getCallingUid());
        String processName = getAppNameByPid(Binder.getCallingPid(), this.mContext);
        int userId2 = UserHandle.getCallingUserId();
        PackageInfo appInfo2 = null;
        if (packageNames != null && packageNames.length > 0) {
            try {
                int index = getDefaultIdxForCsp(packageNames);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("index =  ");
                stringBuilder2.append(index);
                Rlog.d(str2, stringBuilder2.toString());
                appInfo2 = pm.getPackageInfoAsUser(packageNames[index], 64, userId2);
                appInfo = HwTelephonyFactory.getHwInnerSmsManager().getPackageInfoByPid(appInfo2, pm, packageNames, this.mContext);
            } catch (NameNotFoundException e) {
            }
            if (SYSTEM_MANAGER_PROCESS_NAME.equals(processName) && appInfo != null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mCallingPackage ");
                stringBuilder.append(this.mCallingPackage);
                Rlog.d(str, stringBuilder.toString());
                appInfo.packageName = this.mCallingPackage;
            }
            if (!(appInfo == null || appInfo.packageName == null)) {
                this.mPackageSendSmsCount.add(appInfo.packageName);
            }
            hashMap = data;
            userId = userId2;
            return new SmsTracker(hashMap, sentIntent, deliveryIntent, appInfo, PhoneNumberUtils.extractNetworkPortion((String) hashMap.get(MAP_KEY_DEST_ADDR)), format, unsentPartCount, anyPartFailed, messageUri, smsHeader, expectMore, fullMessageText, getSubId(), isText, persistMessage, userId, priority, validityPeriod);
        }
        appInfo = appInfo2;
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCallingPackage ");
        stringBuilder.append(this.mCallingPackage);
        Rlog.d(str, stringBuilder.toString());
        appInfo.packageName = this.mCallingPackage;
        this.mPackageSendSmsCount.add(appInfo.packageName);
        hashMap = data;
        userId = userId2;
        return new SmsTracker(hashMap, sentIntent, deliveryIntent, appInfo, PhoneNumberUtils.extractNetworkPortion((String) hashMap.get(MAP_KEY_DEST_ADDR)), format, unsentPartCount, anyPartFailed, messageUri, smsHeader, expectMore, fullMessageText, getSubId(), isText, persistMessage, userId, priority, validityPeriod);
    }

    protected SmsTracker getSmsTracker(HashMap<String, Object> data, PendingIntent sentIntent, PendingIntent deliveryIntent, String format, Uri messageUri, boolean expectMore, String fullMessageText, boolean isText, boolean persistMessage) {
        return getSmsTracker(data, sentIntent, deliveryIntent, format, null, null, messageUri, null, expectMore, fullMessageText, isText, persistMessage, -1, -1);
    }

    protected SmsTracker getSmsTracker(HashMap<String, Object> data, PendingIntent sentIntent, PendingIntent deliveryIntent, String format, Uri messageUri, boolean expectMore, String fullMessageText, boolean isText, boolean persistMessage, int priority, int validityPeriod) {
        return getSmsTracker(data, sentIntent, deliveryIntent, format, null, null, messageUri, null, expectMore, fullMessageText, isText, persistMessage, priority, validityPeriod);
    }

    protected HashMap<String, Object> getSmsTrackerMap(String destAddr, String scAddr, String text, SubmitPduBase pdu) {
        HashMap<String, Object> map = new HashMap();
        map.put(MAP_KEY_DEST_ADDR, destAddr);
        map.put(MAP_KEY_SC_ADDR, scAddr);
        map.put(MAP_KEY_TEXT, text);
        map.put(MAP_KEY_SMSC, pdu.encodedScAddress);
        map.put(MAP_KEY_PDU, pdu.encodedMessage);
        return map;
    }

    protected HashMap<String, Object> getSmsTrackerMap(String destAddr, String scAddr, int destPort, byte[] data, SubmitPduBase pdu) {
        HashMap<String, Object> map = new HashMap();
        map.put(MAP_KEY_DEST_ADDR, destAddr);
        map.put(MAP_KEY_SC_ADDR, scAddr);
        map.put(MAP_KEY_DEST_PORT, Integer.valueOf(destPort));
        map.put(MAP_KEY_DATA, data);
        map.put(MAP_KEY_SMSC, pdu.encodedScAddress);
        map.put(MAP_KEY_PDU, pdu.encodedMessage);
        return map;
    }

    public boolean isIms() {
        if (this.mSmsDispatchersController != null) {
            return this.mSmsDispatchersController.isIms();
        }
        Rlog.e(TAG, "mSmsDispatchersController  is null");
        return false;
    }

    private String getMultipartMessageText(ArrayList<String> parts) {
        StringBuilder sb = new StringBuilder();
        Iterator it = parts.iterator();
        while (it.hasNext()) {
            String part = (String) it.next();
            if (part != null) {
                sb.append(part);
            }
        }
        return sb.toString();
    }

    protected String getCarrierAppPackageName() {
        UiccCard card = UiccController.getInstance().getUiccCard(this.mPhone.getPhoneId());
        if (card == null) {
            return null;
        }
        List<String> carrierPackages = card.getCarrierPackageNamesForIntent(this.mContext.getPackageManager(), new Intent("android.service.carrier.CarrierMessagingService"));
        if (carrierPackages == null || carrierPackages.size() != 1) {
            return CarrierSmsUtils.getCarrierImsPackageForIntent(this.mContext, this.mPhone, new Intent("android.service.carrier.CarrierMessagingService"));
        }
        return (String) carrierPackages.get(0);
    }

    protected int getSubId() {
        return SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mPhone.getPhoneId());
    }

    private void checkCallerIsPhoneOrCarrierApp() {
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) != 1001 && uid != 0) {
            try {
                if (!UserHandle.isSameApp(this.mContext.getPackageManager().getApplicationInfo(getCarrierAppPackageName(), 0).uid, Binder.getCallingUid())) {
                    throw new SecurityException("Caller is not phone or carrier app!");
                }
            } catch (NameNotFoundException e) {
                throw new SecurityException("Caller is not phone or carrier app!");
            }
        }
    }

    public static int getMaxSendRetriesHw() {
        return MAX_SEND_RETRIES;
    }

    public static int getEventSendRetryHw() {
        return 3;
    }

    protected boolean isCdmaMo() {
        return this.mSmsDispatchersController.isCdmaMo();
    }

    private boolean getTipPremiumFromSimValue() {
        boolean valueFromProp = TIP_PREMIUM_SHORT_CODE;
        Boolean valueFromCard = (Boolean) HwCfgFilePolicy.getValue("flag_tip_premium", this.mPhone.getPhoneId(), Boolean.class);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getTipPremiumFromSimValue, phoneId");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append(", card:");
        stringBuilder.append(valueFromCard);
        stringBuilder.append(", prop: ");
        stringBuilder.append(valueFromProp);
        Rlog.d(str, stringBuilder.toString());
        return valueFromCard != null ? valueFromCard.booleanValue() : valueFromProp;
    }

    private String getAppNameByPid(int pid, Context context) {
        String processName = "";
        for (RunningAppProcessInfo appInfo : ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses()) {
            if (pid == appInfo.pid) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pid: ");
                stringBuilder.append(appInfo.pid);
                stringBuilder.append(" processName: ");
                stringBuilder.append(appInfo.processName);
                Rlog.d(str, stringBuilder.toString());
                return appInfo.processName;
            }
        }
        return processName;
    }
}
