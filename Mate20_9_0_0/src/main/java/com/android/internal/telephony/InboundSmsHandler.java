package com.android.internal.telephony;

import android.app.ActivityManager;
import android.app.BroadcastOptions;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager.Stub;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.SettingsEx.Systemex;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Intents;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Jlog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CarrierServicesSmsFilter.CarrierServicesSmsFilterCallbackInterface;
import com.android.internal.telephony.SmsConstants.MessageClass;
import com.android.internal.telephony.SmsDispatchersController.SmsInjectionCallback;
import com.android.internal.telephony.SmsHeader.ConcatRef;
import com.android.internal.telephony.SmsHeader.PortAddrs;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.util.HexDump;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.huawei.internal.telephony.HwRadarUtils;
import huawei.cust.HwCustUtils;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class InboundSmsHandler extends StateMachine {
    private static String ACTION_OPEN_SMS_APP = "com.android.internal.telephony.OPEN_DEFAULT_SMS_APP";
    public static final int ADDRESS_COLUMN = 6;
    public static final int COUNT_COLUMN = 5;
    public static final int DATE_COLUMN = 3;
    protected static final boolean DBG = true;
    private static final int DELETE_PERMANENTLY = 1;
    private static final String DELIVER_CHANNEL_INFO = SystemProperties.get("ro.config.hw_channel_info", "0,0,460,1,0");
    public static final int DESTINATION_PORT_COLUMN = 2;
    public static final int DISPLAY_ADDRESS_COLUMN = 9;
    protected static final int EVENT_BROADCAST_COMPLETE = 3;
    public static final int EVENT_BROADCAST_SMS = 2;
    public static final int EVENT_INJECT_SMS = 8;
    public static final int EVENT_NEW_SMS = 1;
    private static final int EVENT_RELEASE_WAKELOCK = 5;
    private static final int EVENT_RETURN_TO_IDLE = 4;
    public static final int EVENT_START_ACCEPTING_SMS = 6;
    private static final int EVENT_UPDATE_PHONE_OBJECT = 7;
    public static final int ID_COLUMN = 7;
    private static final String INCOMING_SMS_EXCEPTION_PATTERN = "incoming_sms_exception_pattern";
    private static final String INCOMING_SMS_RESTRICTION_PATTERN = "incoming_sms_restriction_pattern";
    private static final String INTERCEPTION_SMS_RECEIVED = "android.provider.Telephony.INTERCEPTION_SMS_RECEIVED";
    private static final int IOT_VERSION_INDEX = 4;
    private static final int MARK_DELETED = 2;
    private static final int MAX_SMS_LIST_DEFAULT = 30;
    public static final int MESSAGE_BODY_COLUMN = 8;
    private static final int NOTIFICATION_ID_NEW_MESSAGE = 1;
    private static final String NOTIFICATION_TAG = "InboundSmsHandler";
    private static final int NO_DESTINATION_PORT = -1;
    static final long PARTIAL_SEGMENT_EXPIRE_TIME = (86400000 * ((long) SystemProperties.getInt("ro.config.hw_drop_oldsms_day", 3)));
    public static final int PDU_COLUMN = 0;
    private static final String[] PDU_PROJECTION = new String[]{"pdu"};
    private static final String[] PDU_SEQUENCE_PORT_PROJECTION = new String[]{"pdu", "sequence", "destination_port", "display_originating_addr"};
    private static final Map<Integer, Integer> PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING = new HashMap<Integer, Integer>() {
        {
            put(Integer.valueOf(0), Integer.valueOf(0));
            put(Integer.valueOf(1), Integer.valueOf(1));
            put(Integer.valueOf(2), Integer.valueOf(2));
            put(Integer.valueOf(9), Integer.valueOf(3));
        }
    };
    private static final String RECEIVE_SMS_PERMISSION = "huawei.permission.RECEIVE_SMS_INTERCEPTION";
    public static final int RECEIVE_TIME_COLUMN = 10;
    public static final int REFERENCE_NUMBER_COLUMN = 4;
    public static final String SELECT_BY_ID = "_id=?";
    public static final String SELECT_BY_REFERENCE = "address=? AND reference_number=? AND count=? AND deleted=0";
    public static final int SEQUENCE_COLUMN = 1;
    private static final int SMS_BROADCAST_DURATION_TIMEOUT = 180000;
    private static final int SMS_INSERTDB_DURATION_TIMEOUT = 60000;
    private static final boolean VDBG = false;
    private static final int WAKELOCK_TIMEOUT = 3000;
    protected static final Uri sRawUri = Uri.withAppendedPath(Sms.CONTENT_URI, "raw");
    protected static final Uri sRawUriPermanentDelete = Uri.withAppendedPath(Sms.CONTENT_URI, "raw/permanentDelete");
    private String defaultSmsApplicationName = "";
    private boolean isAlreadyDurationTimeout = false;
    private boolean isClass0 = false;
    private AtomicInteger mAlreadyReceivedSms = new AtomicInteger(0);
    protected CellBroadcastHandler mCellBroadcastHandler;
    protected final Context mContext;
    private final DefaultState mDefaultState = new DefaultState(this, null);
    private final DeliveringState mDeliveringState = new DeliveringState(this, null);
    IDeviceIdleController mDeviceIdleController;
    private HwCustInboundSmsHandler mHwCust;
    private final IdleState mIdleState = new IdleState(this, null);
    private ContentObserver mInsertObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfUpdate) {
            InboundSmsHandler.this.mAlreadyReceivedSms.getAndDecrement();
            if (InboundSmsHandler.this.mAlreadyReceivedSms.get() < 0) {
                InboundSmsHandler.this.mAlreadyReceivedSms.set(0);
            }
        }
    };
    protected Phone mPhone;
    private final ContentResolver mResolver;
    private ArrayList<byte[]> mSmsList = new ArrayList();
    private final boolean mSmsReceiveDisabled;
    private final StartupState mStartupState = new StartupState(this, null);
    protected SmsStorageMonitor mStorageMonitor;
    private Runnable mUpdateCountRunner = new Runnable() {
        public void run() {
            if (InboundSmsHandler.this.mAlreadyReceivedSms.get() > 0) {
                String msg = new StringBuilder();
                msg.append("sms receive fail:");
                msg.append(InboundSmsHandler.this.defaultSmsApplicationName);
                HwRadarUtils.report(InboundSmsHandler.this.mContext, HwRadarUtils.ERR_SMS_RECEIVE, msg.toString(), InboundSmsHandler.this.subIdForReceivedSms);
            }
            InboundSmsHandler.this.mAlreadyReceivedSms.set(0);
        }
    };
    private UserManager mUserManager;
    private final WaitingState mWaitingState = new WaitingState(this, null);
    private final WakeLock mWakeLock;
    private int mWakeLockTimeout;
    private final WapPushOverSms mWapPush;
    private int subIdForReceivedSms = -1;

    private class DefaultState extends State {
        private DefaultState() {
        }

        /* synthetic */ DefaultState(InboundSmsHandler x0, AnonymousClass1 x1) {
            this();
        }

        public boolean processMessage(Message msg) {
            if (msg.what != 7) {
                String errorText = new StringBuilder();
                errorText.append("processMessage: unhandled message type ");
                errorText.append(msg.what);
                errorText.append(" currState=");
                errorText.append(InboundSmsHandler.this.getCurrentState().getName());
                errorText = errorText.toString();
                if (Build.IS_DEBUGGABLE) {
                    InboundSmsHandler.this.loge("---- Dumping InboundSmsHandler ----");
                    InboundSmsHandler inboundSmsHandler = InboundSmsHandler.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Total records=");
                    stringBuilder.append(InboundSmsHandler.this.getLogRecCount());
                    inboundSmsHandler.loge(stringBuilder.toString());
                    for (int i = Math.max(InboundSmsHandler.this.getLogRecSize() - 20, 0); i < InboundSmsHandler.this.getLogRecSize(); i++) {
                        if (InboundSmsHandler.this.getLogRec(i) != null) {
                            InboundSmsHandler inboundSmsHandler2 = InboundSmsHandler.this;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Rec[%d]: %s\n");
                            stringBuilder2.append(i);
                            stringBuilder2.append(InboundSmsHandler.this.getLogRec(i).toString());
                            inboundSmsHandler2.loge(stringBuilder2.toString());
                        }
                    }
                    InboundSmsHandler.this.loge("---- Dumped InboundSmsHandler ----");
                    throw new RuntimeException(errorText);
                }
                InboundSmsHandler.this.loge(errorText);
            } else {
                InboundSmsHandler.this.onUpdatePhoneObject((Phone) msg.obj);
            }
            return true;
        }
    }

    private class DeliveringState extends State {
        private DeliveringState() {
        }

        /* synthetic */ DeliveringState(InboundSmsHandler x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Delivering state");
        }

        public void exit() {
            InboundSmsHandler.this.log("leaving Delivering state");
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler inboundSmsHandler = InboundSmsHandler.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DeliveringState.processMessage:");
            stringBuilder.append(msg.what);
            inboundSmsHandler.log(stringBuilder.toString());
            switch (msg.what) {
                case 1:
                    InboundSmsHandler.this.handleNewSms((AsyncResult) msg.obj);
                    InboundSmsHandler.this.sendMessage(4);
                    return true;
                case 2:
                    if (InboundSmsHandler.this.processMessagePart(msg.obj)) {
                        InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mWaitingState);
                    } else {
                        InboundSmsHandler.this.log("No broadcast sent on processing EVENT_BROADCAST_SMS in Delivering state. Return to Idle state");
                        InboundSmsHandler.this.sendMessage(4);
                    }
                    return true;
                case 4:
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mIdleState);
                    return true;
                case 5:
                    InboundSmsHandler.this.mWakeLock.release();
                    if (!InboundSmsHandler.this.mWakeLock.isHeld()) {
                        InboundSmsHandler.this.loge("mWakeLock released while delivering/broadcasting!");
                    }
                    return true;
                case 8:
                    InboundSmsHandler.this.handleInjectSms((AsyncResult) msg.obj);
                    InboundSmsHandler.this.sendMessage(4);
                    return true;
                default:
                    return false;
            }
        }
    }

    private class IdleState extends State {
        private IdleState() {
        }

        /* synthetic */ IdleState(InboundSmsHandler x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Idle state");
            InboundSmsHandler.this.sendMessageDelayed(5, (long) InboundSmsHandler.this.getWakeLockTimeout());
        }

        public void exit() {
            InboundSmsHandler.this.mWakeLock.acquire();
            InboundSmsHandler.this.log("acquired wakelock, leaving Idle state");
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler inboundSmsHandler = InboundSmsHandler.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IdleState.processMessage:");
            stringBuilder.append(msg.what);
            inboundSmsHandler.log(stringBuilder.toString());
            inboundSmsHandler = InboundSmsHandler.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Idle state processing message type ");
            stringBuilder.append(msg.what);
            inboundSmsHandler.log(stringBuilder.toString());
            switch (msg.what) {
                case 1:
                case 2:
                case 8:
                    InboundSmsHandler.this.deferMessage(msg);
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mDeliveringState);
                    return true;
                case 4:
                    return true;
                case 5:
                    InboundSmsHandler.this.mWakeLock.release();
                    if (InboundSmsHandler.this.mWakeLock.isHeld()) {
                        InboundSmsHandler.this.log("mWakeLock is still held after release");
                    } else {
                        InboundSmsHandler.this.log("mWakeLock released");
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    private static class NewMessageNotificationActionReceiver extends BroadcastReceiver {
        private NewMessageNotificationActionReceiver() {
        }

        /* synthetic */ NewMessageNotificationActionReceiver(AnonymousClass1 x0) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (InboundSmsHandler.ACTION_OPEN_SMS_APP.equals(intent.getAction())) {
                context.startActivity(context.getPackageManager().getLaunchIntentForPackage(Sms.getDefaultSmsPackage(context)));
            }
        }
    }

    private final class SmsBroadcastReceiver extends BroadcastReceiver {
        private long mBroadcastTimeNano = System.nanoTime();
        private final String mDeleteWhere;
        private final String[] mDeleteWhereArgs;

        SmsBroadcastReceiver(InboundSmsTracker tracker) {
            this.mDeleteWhere = tracker.getDeleteWhere();
            this.mDeleteWhereArgs = tracker.getDeleteWhereArgs();
        }

        public void onReceive(Context context, Intent intent) {
            if (HwTelephonyFactory.getHwInnerSmsManager().shouldSendReceivedActionInPrivacyMode(InboundSmsHandler.this, context, intent, this.mDeleteWhere, this.mDeleteWhereArgs)) {
                String action = intent.getAction();
                if (action.equals("android.provider.Telephony.SMS_DELIVER")) {
                    intent.setAction("android.provider.Telephony.SMS_RECEIVED");
                    intent.addFlags(16777216);
                    intent.setComponent(null);
                    InboundSmsHandler.this.dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, InboundSmsHandler.this.handleSmsWhitelisting(null), this, UserHandle.ALL);
                } else if (action.equals("android.provider.Telephony.WAP_PUSH_DELIVER")) {
                    intent.setAction("android.provider.Telephony.WAP_PUSH_RECEIVED");
                    intent.setComponent(null);
                    intent.addFlags(16777216);
                    Bundle options = null;
                    try {
                        long duration = InboundSmsHandler.this.mDeviceIdleController.addPowerSaveTempWhitelistAppForMms(InboundSmsHandler.this.mContext.getPackageName(), 0, "mms-broadcast");
                        BroadcastOptions bopts = BroadcastOptions.makeBasic();
                        bopts.setTemporaryAppWhitelistDuration(duration);
                        options = bopts.toBundle();
                    } catch (RemoteException e) {
                    }
                    String mimeType = intent.getType();
                    InboundSmsHandler.this.dispatchIntent(intent, WapPushOverSms.getPermissionForType(mimeType), WapPushOverSms.getAppOpsPermissionForIntent(mimeType), options, this, UserHandle.SYSTEM);
                } else {
                    int rc;
                    if (!("android.intent.action.DATA_SMS_RECEIVED".equals(action) || "android.provider.Telephony.SMS_RECEIVED".equals(action) || "android.intent.action.DATA_SMS_RECEIVED".equals(action) || "android.provider.Telephony.WAP_PUSH_RECEIVED".equals(action))) {
                        InboundSmsHandler inboundSmsHandler = InboundSmsHandler.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unexpected BroadcastReceiver action: ");
                        stringBuilder.append(action);
                        inboundSmsHandler.loge(stringBuilder.toString());
                    }
                    if ("true".equals(Systemex.getString(InboundSmsHandler.this.mResolver, "disableErrWhenReceiveSMS"))) {
                        rc = -1;
                    } else {
                        rc = getResultCode();
                    }
                    if (rc == -1 || rc == 1) {
                        InboundSmsHandler.this.log("successful broadcast, deleting from raw table.");
                    } else {
                        InboundSmsHandler inboundSmsHandler2 = InboundSmsHandler.this;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("a broadcast receiver set the result code to ");
                        stringBuilder2.append(rc);
                        stringBuilder2.append(", deleting from raw table anyway!");
                        inboundSmsHandler2.loge(stringBuilder2.toString());
                    }
                    InboundSmsHandler.this.deleteFromRawTable(this.mDeleteWhere, this.mDeleteWhereArgs, 2);
                    InboundSmsHandler.this.sendMessage(3);
                    int durationMillis = (int) ((System.nanoTime() - this.mBroadcastTimeNano) / 1000000);
                    InboundSmsHandler inboundSmsHandler3;
                    StringBuilder stringBuilder3;
                    if (durationMillis >= AbstractPhoneBase.SET_TO_AOTO_TIME) {
                        inboundSmsHandler3 = InboundSmsHandler.this;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Slow ordered broadcast completion time: ");
                        stringBuilder3.append(durationMillis);
                        stringBuilder3.append(" ms");
                        inboundSmsHandler3.loge(stringBuilder3.toString());
                    } else {
                        inboundSmsHandler3 = InboundSmsHandler.this;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("ordered broadcast completed in: ");
                        stringBuilder3.append(durationMillis);
                        stringBuilder3.append(" ms");
                        inboundSmsHandler3.log(stringBuilder3.toString());
                    }
                    InboundSmsHandler.this.reportSmsReceiveTimeout(durationMillis);
                }
            }
        }
    }

    private class StartupState extends State {
        private StartupState() {
        }

        /* synthetic */ StartupState(InboundSmsHandler x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Startup state");
            InboundSmsHandler.this.setWakeLockTimeout(0);
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler inboundSmsHandler = InboundSmsHandler.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("StartupState.processMessage:");
            stringBuilder.append(msg.what);
            inboundSmsHandler.log(stringBuilder.toString());
            int i = msg.what;
            if (i != 6) {
                if (i != 8) {
                    switch (i) {
                        case 1:
                        case 2:
                            break;
                        default:
                            return false;
                    }
                }
                InboundSmsHandler.this.deferMessage(msg);
                return true;
            }
            InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mIdleState);
            return true;
        }
    }

    private class WaitingState extends State {
        private WaitingState() {
        }

        /* synthetic */ WaitingState(InboundSmsHandler x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Waiting state");
        }

        public void exit() {
            InboundSmsHandler.this.log("exiting Waiting state");
            InboundSmsHandler.this.setWakeLockTimeout(InboundSmsHandler.WAKELOCK_TIMEOUT);
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler inboundSmsHandler = InboundSmsHandler.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WaitingState.processMessage:");
            stringBuilder.append(msg.what);
            inboundSmsHandler.log(stringBuilder.toString());
            switch (msg.what) {
                case 2:
                    InboundSmsHandler.this.deferMessage(msg);
                    return true;
                case 3:
                    InboundSmsHandler.this.sendMessage(4);
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mDeliveringState);
                    return true;
                case 4:
                    return true;
                default:
                    return false;
            }
        }
    }

    private final class CarrierServicesSmsFilterCallback implements CarrierServicesSmsFilterCallbackInterface {
        private final int mDestPort;
        private final byte[][] mPdus;
        private final SmsBroadcastReceiver mSmsBroadcastReceiver;
        private final String mSmsFormat;
        private final boolean mUserUnlocked;

        CarrierServicesSmsFilterCallback(byte[][] pdus, int destPort, String smsFormat, SmsBroadcastReceiver smsBroadcastReceiver, boolean userUnlocked) {
            this.mPdus = pdus;
            this.mDestPort = destPort;
            this.mSmsFormat = smsFormat;
            this.mSmsBroadcastReceiver = smsBroadcastReceiver;
            this.mUserUnlocked = userUnlocked;
        }

        public void onFilterComplete(int result) {
            InboundSmsHandler inboundSmsHandler = InboundSmsHandler.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onFilterComplete: result is ");
            stringBuilder.append(result);
            inboundSmsHandler.logv(stringBuilder.toString());
            if ((result & 1) != 0) {
                InboundSmsHandler.this.dropSms(this.mSmsBroadcastReceiver);
            } else if (VisualVoicemailSmsFilter.filter(InboundSmsHandler.this.mContext, this.mPdus, this.mSmsFormat, this.mDestPort, InboundSmsHandler.this.mPhone.getSubId())) {
                InboundSmsHandler.this.log("Visual voicemail SMS dropped");
                InboundSmsHandler.this.dropSms(this.mSmsBroadcastReceiver);
            } else if (this.mUserUnlocked) {
                InboundSmsHandler.this.dispatchSmsDeliveryIntent(this.mPdus, this.mSmsFormat, this.mDestPort, this.mSmsBroadcastReceiver);
            } else {
                if (!InboundSmsHandler.this.isSkipNotifyFlagSet(result)) {
                    InboundSmsHandler.this.showNewMessageNotification();
                }
                InboundSmsHandler.this.sendMessage(3);
            }
        }
    }

    protected abstract void acknowledgeLastIncomingSms(boolean z, int i, Message message);

    protected abstract int dispatchMessageRadioSpecific(SmsMessageBase smsMessageBase);

    protected abstract boolean is3gpp2();

    protected InboundSmsHandler(String name, Context context, SmsStorageMonitor storageMonitor, Phone phone, CellBroadcastHandler cellBroadcastHandler) {
        super(name);
        this.mContext = context;
        this.mStorageMonitor = storageMonitor;
        this.mPhone = phone;
        this.mCellBroadcastHandler = cellBroadcastHandler;
        this.mResolver = context.getContentResolver();
        this.mWapPush = HwTelephonyFactory.getHwInnerSmsManager().createHwWapPushOverSms(context);
        HwTelephonyFactory.getHwInnerSmsManager().createSmsInterceptionService(context);
        this.mSmsReceiveDisabled = TelephonyManager.from(this.mContext).getSmsReceiveCapableForPhone(this.mPhone.getPhoneId(), this.mContext.getResources().getBoolean(17957026)) ^ 1;
        this.mWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, name);
        this.mWakeLock.acquire();
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mDeviceIdleController = TelephonyComponentFactory.getInstance().getIDeviceIdleController();
        addState(this.mDefaultState);
        addState(this.mStartupState, this.mDefaultState);
        addState(this.mIdleState, this.mDefaultState);
        addState(this.mDeliveringState, this.mDefaultState);
        addState(this.mWaitingState, this.mDeliveringState);
        setInitialState(this.mStartupState);
        addInboxInsertObserver(this.mContext);
        this.mHwCust = (HwCustInboundSmsHandler) HwCustUtils.createObj(HwCustInboundSmsHandler.class, new Object[0]);
        if (this.mHwCust != null && this.mHwCust.isIQIEnable()) {
            this.mHwCust.createIQClient(this.mContext);
        }
        log("created InboundSmsHandler");
    }

    public void dispose() {
        quit();
    }

    public void updatePhoneObject(Phone phone) {
        sendMessage(7, phone);
    }

    protected void onQuitting() {
        this.mWapPush.dispose();
        while (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
    }

    public Phone getPhone() {
        return this.mPhone;
    }

    private void handleNewSms(AsyncResult ar) {
        if (ar.exception != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception processing incoming SMS: ");
            stringBuilder.append(ar.exception);
            loge(stringBuilder.toString());
            return;
        }
        int result;
        Jlog.d(47, "JLID_RIL_RESPONSE_NEW_SMS");
        boolean handled = false;
        try {
            SmsMessage sms = ar.result;
            if (this.mHwCust != null && this.mHwCust.isIQIEnable() && this.mHwCust.isIQISms(sms)) {
                log("check SMS is true");
                result = 1;
            } else {
                result = dispatchMessage(sms.mWrappedSmsMessage);
            }
            if (sms.mWrappedSmsMessage.blacklistFlag) {
                result = -1;
                String msg = new StringBuilder();
                msg.append("receive a blacklist sms, modem has acked it, fw need't reply");
                msg.append(this.defaultSmsApplicationName);
                HwRadarUtils.report(this.mContext, HwRadarUtils.ERR_SMS_RECEIVE, msg.toString(), 0);
                log("receive a blacklist sms, modem has acked it, fw need't reply");
            }
        } catch (RuntimeException ex) {
            loge("Exception dispatching message", ex);
            result = 2;
        }
        int result2 = result;
        if (result2 != -1) {
            if (result2 == 1) {
                handled = true;
            }
            if (!handled) {
                Jlog.d(50, "JL_DISPATCH_SMS_FAILED");
            }
            if (this.mHwCust == null || !this.mHwCust.isNotNotifyWappushEnabled(ar)) {
                notifyAndAcknowledgeLastIncomingSms(handled, result2, null);
            } else {
                acknowledgeLastIncomingSms(handled, result2, null);
            }
        }
    }

    private void handleInjectSms(AsyncResult ar) {
        int result;
        SmsInjectionCallback callback = null;
        try {
            int result2;
            callback = (SmsInjectionCallback) ar.userObj;
            SmsMessage sms = ar.result;
            if (sms == null) {
                result2 = 2;
            } else {
                result2 = dispatchMessage(sms.mWrappedSmsMessage, false);
            }
            result = result2;
        } catch (RuntimeException ex) {
            loge("Exception dispatching message", ex);
            result = 2;
        }
        if (callback != null) {
            callback.onSmsInjectedResult(result);
        }
    }

    private int dispatchMessage(SmsMessageBase smsb) {
        return dispatchMessage(smsb, true);
    }

    private int dispatchMessage(SmsMessageBase smsb, boolean filterRepeatedMessage) {
        if (smsb == null) {
            loge("dispatchSmsMessage: message is null");
            return 2;
        } else if (this.mSmsReceiveDisabled) {
            log("Received short message on device which doesn't support receiving SMS. Ignored.");
            return 1;
        } else {
            boolean onlyCore = false;
            boolean z = filterRepeatedMessage || isIOTVersion();
            filterRepeatedMessage = z;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dispatchMessage, filterRepeatedMessage:");
            stringBuilder.append(filterRepeatedMessage);
            log(stringBuilder.toString());
            if (filterRepeatedMessage && hasSameSmsPdu(smsb.getPdu())) {
                log("receive a duplicated SMS and abandon it.");
                return 1;
            }
            try {
                onlyCore = Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
            } catch (RemoteException e) {
            }
            if (!onlyCore) {
                return dispatchMessageRadioSpecific(smsb);
            }
            log("Received a short message in encrypted state. Rejecting.");
            return 2;
        }
    }

    private boolean isIOTVersion() {
        String[] info = DELIVER_CHANNEL_INFO.split(",");
        int result = 0;
        if (4 < info.length && info[4] != null) {
            try {
                result = Integer.parseInt(info[4]);
            } catch (NumberFormatException e) {
                loge("Exception while parsing Integer");
            }
        }
        return result != 0;
    }

    /* JADX WARNING: Missing block: B:16:0x0059, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean hasSameSmsPdu(byte[] pdu) {
        log("hasSameSmsPdu: check if there is a same pdu in mSmsList.");
        synchronized (this.mSmsList) {
            Iterator it = this.mSmsList.iterator();
            while (it.hasNext()) {
                if (Arrays.equals(pdu, (byte[]) it.next())) {
                    return true;
                }
            }
            this.mSmsList.add(pdu);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hasSameSmsPdu: mSmsList.size() = ");
            stringBuilder.append(this.mSmsList.size());
            log(stringBuilder.toString());
            if (this.mSmsList.size() > 30) {
                log("hasSameSmsPdu: mSmsList.size() > MAX_SMS_LIST_DEFAULT");
                this.mSmsList.remove(0);
            }
        }
    }

    protected void onUpdatePhoneObject(Phone phone) {
        this.mPhone = phone;
        this.mStorageMonitor = this.mPhone.mSmsStorageMonitor;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onUpdatePhoneObject: phone=");
        stringBuilder.append(this.mPhone.getClass().getSimpleName());
        log(stringBuilder.toString());
    }

    private void notifyAndAcknowledgeLastIncomingSms(boolean success, int result, Message response) {
        if (!success) {
            Intent intent = new Intent("android.provider.Telephony.SMS_REJECTED");
            intent.putExtra("result", result);
            intent.addFlags(16777216);
            this.mContext.sendBroadcast(intent, "android.permission.RECEIVE_SMS");
        }
        acknowledgeLastIncomingSms(success, result, response);
    }

    protected int dispatchNormalMessage(SmsMessageBase sms) {
        int destPort;
        InboundSmsTracker tracker;
        SmsHeader smsHeader = sms.getUserDataHeader();
        boolean z = false;
        this.isClass0 = sms.getMessageClass() == MessageClass.CLASS_0;
        int destPort2 = -1;
        Jlog.d(48, "JL_DISPATCH_NORMAL_SMS");
        if (smsHeader == null || smsHeader.concatRef == null) {
            if (!(smsHeader == null || smsHeader.portAddrs == null)) {
                destPort2 = smsHeader.portAddrs.destPort;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("destination port: ");
                stringBuilder.append(destPort2);
                log(stringBuilder.toString());
            }
            destPort = destPort2;
            tracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(sms.getPdu(), sms.getTimestampMillis(), destPort2, is3gpp2(), false, sms.getOriginatingAddress(), sms.getDisplayOriginatingAddress(), sms.getMessageBody());
        } else {
            ConcatRef concatRef = smsHeader.concatRef;
            PortAddrs portAddrs = smsHeader.portAddrs;
            destPort = portAddrs != null ? portAddrs.destPort : -1;
            tracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(sms.getPdu(), sms.getTimestampMillis(), destPort, is3gpp2(), sms.getOriginatingAddress(), sms.getDisplayOriginatingAddress(), concatRef.refNumber, concatRef.seqNumber, concatRef.msgCount, false, sms.getMessageBody());
        }
        if (!SystemProperties.getBoolean("ro.config.comm_serv_tid_enable", false) || this.mHwCust == null) {
            SmsMessageBase smsMessageBase = sms;
        } else {
            if (this.mHwCust.dispatchMessageByDestPort(destPort, sms, this.mContext)) {
                return 1;
            }
        }
        log("dispatchNormalMessage and created tracker");
        if (tracker.getDestPort() == -1) {
            z = true;
        }
        return addTrackerToRawTableAndSendMessage(tracker, z);
    }

    protected int addTrackerToRawTableAndSendMessage(InboundSmsTracker tracker, boolean deDup) {
        int addTrackerToRawTable = addTrackerToRawTable(tracker, deDup);
        if (addTrackerToRawTable != 1) {
            return addTrackerToRawTable != 5 ? 2 : 1;
        } else {
            if (((PowerManager) this.mContext.getSystemService("power")).isScreenOn()) {
                Jlog.d(50, "JL_DISPATCH_SMS_FAILED");
            } else {
                Jlog.d(49, "JL_SEND_BROADCAST_SMS");
            }
            sendMessage(2, tracker);
            return 1;
        }
    }

    private void scanAndDeleteOlderPartialMessages(InboundSmsTracker tracker) {
        String address = tracker.getAddress();
        String refNumber = Integer.toString(tracker.getReferenceNumber());
        String count = Integer.toString(tracker.getMessageCount());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("date < ");
        stringBuilder.append(tracker.getTimestamp() - PARTIAL_SEGMENT_EXPIRE_TIME);
        stringBuilder.append(" AND ");
        stringBuilder.append(SELECT_BY_REFERENCE);
        StringBuilder deleteWhere = new StringBuilder(stringBuilder.toString());
        deleteWhereArgs = new String[3];
        int delCount = 0;
        deleteWhereArgs[0] = address;
        deleteWhereArgs[1] = refNumber;
        deleteWhereArgs[2] = count;
        try {
            delCount = this.mResolver.delete(sRawUri, deleteWhere.toString(), deleteWhereArgs);
        } catch (Exception e) {
            loge("scanAndDeleteOlderPartialMessages got exception ", e);
        }
        if (delCount > 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("scanAndDeleteOlderPartialMessages: delete ");
            stringBuilder2.append(delCount);
            stringBuilder2.append(" raw sms older than ");
            stringBuilder2.append(PARTIAL_SEGMENT_EXPIRE_TIME);
            stringBuilder2.append(" days for ");
            stringBuilder2.append(tracker.getAddress());
            log(stringBuilder2.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:145:0x03e7  */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x03ef  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean processMessagePart(InboundSmsTracker tracker) {
        SQLException e;
        int i;
        Throwable th;
        InboundSmsTracker inboundSmsTracker = tracker;
        int i2 = 0;
        if (tracker.isOldMessageInRawTable()) {
            log("processMessagePart, ignore old message kept in raw table.");
            return false;
        }
        int messageCount = tracker.getMessageCount();
        int destPort = tracker.getDestPort();
        if (destPort != 2948 && HwTelephonyFactory.getHwInnerSmsManager().isLimitNumOfSmsEnabled(false) && HwTelephonyFactory.getHwInnerSmsManager().isExceedSMSLimit(this.mContext, false)) {
            deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(), 2);
            return false;
        }
        boolean block = false;
        if (messageCount <= 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processMessagePart: returning false due to invalid message count ");
            stringBuilder.append(messageCount);
            loge(stringBuilder.toString());
            return false;
        }
        int destPort2;
        byte[][] pdus;
        StringBuilder stringBuilder2;
        int i3 = 1;
        boolean block2;
        if (messageCount == 1) {
            byte[][] pdus2 = new byte[][]{tracker.getPdu()};
            destPort2 = destPort;
            block2 = BlockChecker.isBlocked(this.mContext, tracker.getDisplayAddress(), null);
            pdus = pdus2;
        } else {
            Cursor cursor = null;
            try {
                String address = tracker.getAddress();
                String refNumber = Integer.toString(tracker.getReferenceNumber());
                String count = Integer.toString(tracker.getMessageCount());
                cursor = this.mResolver.query(sRawUri, PDU_SEQUENCE_PORT_PROJECTION, tracker.getQueryForSegmentsSubId(), new String[]{address, refNumber, count, String.valueOf(this.mPhone.getSubId())}, null);
                destPort2 = cursor.getCount();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("processMessagePart, cursorCount: ");
                stringBuilder3.append(destPort2);
                stringBuilder3.append(", ref|seq/count(");
                stringBuilder3.append(tracker.getReferenceNumber());
                stringBuilder3.append("|");
                stringBuilder3.append(tracker.getSequenceNumber());
                stringBuilder3.append("/");
                stringBuilder3.append(messageCount);
                stringBuilder3.append(")");
                log(stringBuilder3.toString());
                if (destPort2 < messageCount) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    return false;
                }
                pdus = new byte[messageCount][];
                while (cursor.moveToNext()) {
                    try {
                        int index = cursor.getInt(((Integer) PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(Integer.valueOf(i3))).intValue()) - tracker.getIndexOffset();
                        if (index < pdus.length) {
                            if (index >= 0) {
                                pdus[index] = HexDump.hexStringToByteArray(cursor.getString(((Integer) PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(Integer.valueOf(i2))).intValue()));
                                if (index == 0 && !cursor.isNull(((Integer) PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(Integer.valueOf(2))).intValue())) {
                                    i3 = InboundSmsTracker.getRealDestPort(cursor.getInt(((Integer) PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(Integer.valueOf(2))).intValue()));
                                    if (i3 != -1) {
                                        destPort = i3;
                                    }
                                }
                                if (!block) {
                                    block = BlockChecker.isBlocked(this.mContext, cursor.getString(((Integer) PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING.get(Integer.valueOf(9))).intValue()), null);
                                }
                                i2 = 0;
                                i3 = 1;
                            }
                        }
                        loge(String.format("processMessagePart: invalid seqNumber = %d, messageCount = %d", new Object[]{Integer.valueOf(tracker.getIndexOffset() + index), Integer.valueOf(messageCount)}));
                        i2 = 0;
                        i3 = 1;
                    } catch (SQLException e2) {
                        e = e2;
                        i = messageCount;
                        try {
                            loge("Can't access multipart SMS database", e);
                            if (cursor != null) {
                            }
                            return false;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        i = messageCount;
                        if (cursor != null) {
                        }
                        throw th;
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
                destPort2 = destPort;
                block2 = block;
            } catch (SQLException e3) {
                e = e3;
                i = messageCount;
                loge("Can't access multipart SMS database", e);
                if (cursor != null) {
                    cursor.close();
                }
                return false;
            } catch (Throwable th4) {
                th = th4;
                i = messageCount;
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
        }
        List<byte[]> pduList = Arrays.asList(pdus);
        if (pduList.size() == 0) {
        } else if (pduList.contains(null)) {
            i = messageCount;
        } else {
            BroadcastReceiver resultReceiver = new SmsBroadcastReceiver(inboundSmsTracker);
            if (!this.mUserManager.isUserUnlocked()) {
                return processMessagePartWithUserLocked(inboundSmsTracker, pdus, destPort2, resultReceiver);
            }
            if (destPort2 == 2948) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                String oriAddress = null;
                for (byte[] pdu : pdus) {
                    byte[] pdu2;
                    if (!tracker.is3gpp2()) {
                        SmsMessage msg = SmsMessage.createFromPdu(pdu2, "3gpp");
                        if (msg != null) {
                            pdu2 = msg.getUserData();
                            oriAddress = msg.getOriginatingAddress();
                        } else {
                            loge("processMessagePart: SmsMessage.createFromPdu returned null");
                            return false;
                        }
                    }
                    output.write(pdu2, 0, pdu2.length);
                }
                boolean z = this.mWapPush.isWapPushForMms(output.toByteArray(), this) && this.mPhone.mDcTracker.isRoamingPushDisabled() && this.mPhone.getServiceState().getDataRoaming();
                if (z) {
                    deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(), 2);
                    return false;
                }
                this.mWapPush.saveSmsTracker(inboundSmsTracker);
                if (this.mHwCust != null && this.mHwCust.isIQIEnable() && this.mHwCust.isIQIWapPush(output)) {
                    log("check WapPush is true");
                    destPort = -1;
                } else {
                    destPort = this.mWapPush.dispatchWapPdu(output.toByteArray(), oriAddress, resultReceiver, this, tracker.is3gpp2());
                }
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("dispatchWapPdu() returned ");
                stringBuilder4.append(destPort);
                log(stringBuilder4.toString());
                if (destPort == -1) {
                    return true;
                }
                deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(), 2);
                return false;
            }
            if (HwTelephonyFactory.getHwInnerSmsManager().isLimitNumOfSmsEnabled(false) && destPort2 != 2948) {
                HwTelephonyFactory.getHwInnerSmsManager().updateSmsUsedNum(this.mContext, false);
            }
            Intent intent = new Intent();
            intent.putExtra("pdus", pdus);
            intent.putExtra("format", tracker.getFormat());
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("putPhoneIdAndSubIdExtra");
            stringBuilder2.append(this.mPhone.getPhoneId());
            log(stringBuilder2.toString());
            if (-1 == destPort2 && !HwTelephonyFactory.getHwInnerSmsManager().isMatchSMSPattern(tracker.getAddress(), INCOMING_SMS_EXCEPTION_PATTERN, this.mPhone.getPhoneId()) && HwTelephonyFactory.getHwInnerSmsManager().isMatchSMSPattern(tracker.getAddress(), INCOMING_SMS_RESTRICTION_PATTERN, this.mPhone.getPhoneId())) {
                deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(), 2);
                log("forbid receive sms by mdm!");
                return false;
            } else if (-1 == destPort2 && block2) {
                HwTelephonyFactory.getHwInnerSmsManager().sendGoogleSmsBlockedRecord(intent);
                deleteFromRawTable(tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(), 2);
                return false;
            } else {
                if (-1 == destPort2) {
                    messageCount = intent;
                    if (HwTelephonyFactory.getHwInnerSmsManager().newSmsShouldBeIntercepted(this.mContext, intent, this, tracker.getDeleteWhere(), tracker.getDeleteWhereArgs(), null)) {
                        Intent secretIntent = new Intent(messageCount);
                        secretIntent.setAction(INTERCEPTION_SMS_RECEIVED);
                        dispatchIntent(secretIntent, RECEIVE_SMS_PERMISSION, 16, handleSmsWhitelisting(null), null, UserHandle.ALL);
                        return true;
                    }
                }
                messageCount = intent;
                if (!filterSms(pdus, destPort2, inboundSmsTracker, resultReceiver, true)) {
                    dispatchSmsDeliveryIntent(pdus, tracker.getFormat(), destPort2, resultReceiver);
                }
                return true;
            }
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("processMessagePart: returning false due to ");
        stringBuilder2.append(pduList.size() == 0 ? "pduList.size() == 0" : "pduList.contains(null)");
        loge(stringBuilder2.toString());
        return false;
    }

    private boolean processMessagePartWithUserLocked(InboundSmsTracker tracker, byte[][] pdus, int destPort, SmsBroadcastReceiver resultReceiver) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Credential-encrypted storage not available. Port: ");
        stringBuilder.append(destPort);
        log(stringBuilder.toString());
        if (destPort == 2948) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (byte[] pdu : pdus) {
                byte[] pdu2;
                if (!tracker.is3gpp2()) {
                    SmsMessage msg = SmsMessage.createFromPdu(pdu2, "3gpp");
                    if (msg != null) {
                        pdu2 = msg.getUserData();
                    } else {
                        loge("processMessagePartWithUserLocked: SmsMessage.createFromPdu returned null");
                        return false;
                    }
                }
                output.write(pdu2, 0, pdu2.length);
            }
            if (this.mWapPush.isWapPushForMms(output.toByteArray(), this)) {
                showNewMessageNotification();
                return false;
            }
        }
        if (destPort != -1) {
            return false;
        }
        if (filterSms(pdus, destPort, tracker, resultReceiver, false)) {
            return true;
        }
        showNewMessageNotification();
        return false;
    }

    private void showNewMessageNotification() {
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            log("Show new message notification.");
            ((NotificationManager) this.mContext.getSystemService("notification")).notify(NOTIFICATION_TAG, 1, new Builder(this.mContext).setSmallIcon(17301646).setAutoCancel(true).setVisibility(1).setDefaults(-1).setContentTitle(this.mContext.getString(17040557)).setContentText(this.mContext.getString(17040556)).setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_OPEN_SMS_APP), 1073741824)).setChannelId(NotificationChannelController.CHANNEL_ID_SMS).build());
        }
    }

    static void cancelNewMessageNotification(Context context) {
        ((NotificationManager) context.getSystemService("notification")).cancel(NOTIFICATION_TAG, 1);
    }

    private boolean filterSms(byte[][] pdus, int destPort, InboundSmsTracker tracker, SmsBroadcastReceiver resultReceiver, boolean userUnlocked) {
        if (new CarrierServicesSmsFilter(this.mContext, this.mPhone, pdus, destPort, tracker.getFormat(), new CarrierServicesSmsFilterCallback(pdus, destPort, tracker.getFormat(), resultReceiver, userUnlocked), getName()).filter()) {
            return true;
        }
        if (!VisualVoicemailSmsFilter.filter(this.mContext, pdus, tracker.getFormat(), destPort, this.mPhone.getSubId())) {
            return false;
        }
        log("Visual voicemail SMS dropped");
        dropSms(resultReceiver);
        return true;
    }

    /* JADX WARNING: Missing block: B:30:0x008f, code skipped:
            if (r2.isManagedProfile() == false) goto L_0x0092;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dispatchIntent(Intent intent, String permission, int appOp, Bundle opts, BroadcastReceiver resultReceiver, UserHandle user) {
        Intent intent2 = intent;
        intent2.addFlags(134217728);
        String action = intent.getAction();
        if ("android.provider.Telephony.SMS_DELIVER".equals(action) || "android.provider.Telephony.SMS_RECEIVED".equals(action) || "android.provider.Telephony.WAP_PUSH_DELIVER".equals(action) || "android.provider.Telephony.WAP_PUSH_RECEIVED".equals(action)) {
            intent2.addFlags(268435456);
        }
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent2, this.mPhone.getPhoneId());
        UserHandle userHandle = user;
        if (userHandle.equals(UserHandle.ALL)) {
            int[] users = null;
            try {
                users = ActivityManager.getService().getRunningUserIds();
            } catch (RemoteException e) {
            }
            if (users == null) {
                users = new int[]{user.getIdentifier()};
            }
            int[] users2 = users;
            int i = users2.length - 1;
            while (true) {
                int i2 = i;
                if (i2 >= 0) {
                    int[] users3;
                    UserHandle targetUser = new UserHandle(users2[i2]);
                    if (users2[i2] != 0) {
                        if (!this.mUserManager.hasUserRestriction("no_sms", targetUser)) {
                            UserInfo info = this.mUserManager.getUserInfo(users2[i2]);
                            if (info != null) {
                            }
                        }
                        users3 = users2;
                        i = i2 - 1;
                        users2 = users3;
                    }
                    users3 = users2;
                    this.mContext.sendOrderedBroadcastAsUser(intent2, targetUser, permission, appOp, opts, users2[i2] == 0 ? resultReceiver : null, getHandler(), -1, null, null);
                    i = i2 - 1;
                    users2 = users3;
                } else {
                    return;
                }
            }
        }
        this.mContext.sendOrderedBroadcastAsUser(intent2, userHandle, permission, appOp, opts, resultReceiver, getHandler(), -1, null, null);
        triggerInboxInsertDoneDetect(intent);
    }

    private void triggerInboxInsertDoneDetect(Intent intent) {
        if ("android.provider.Telephony.SMS_DELIVER".equals(intent.getAction()) && !this.isClass0) {
            ComponentName componentName = intent.getComponent();
            if (componentName != null) {
                this.defaultSmsApplicationName = componentName.getPackageName();
            }
            this.subIdForReceivedSms = intent.getIntExtra("subscription", SubscriptionManager.getDefaultSmsSubscriptionId());
            this.mAlreadyReceivedSms.getAndIncrement();
            getHandler().removeCallbacks(this.mUpdateCountRunner);
            getHandler().postDelayed(this.mUpdateCountRunner, 60000);
        }
    }

    private void addInboxInsertObserver(Context context) {
        context.getContentResolver().registerContentObserver(Uri.parse("content://sms/inbox-insert"), true, this.mInsertObserver);
    }

    private void reportSmsReceiveTimeout(int durationMillis) {
        if (!this.isAlreadyDurationTimeout && durationMillis >= SMS_BROADCAST_DURATION_TIMEOUT) {
            this.isAlreadyDurationTimeout = true;
            String msg = new StringBuilder();
            msg.append("sms receive timeout:");
            msg.append(durationMillis);
            msg.append(this.defaultSmsApplicationName);
            HwRadarUtils.report(this.mContext, HwRadarUtils.ERR_SMS_RECEIVE, msg.toString(), this.subIdForReceivedSms);
        }
    }

    private void deleteFromRawTable(String deleteWhere, String[] deleteWhereArgs, int deleteType) {
        int rows = this.mResolver.delete(deleteType == 1 ? sRawUriPermanentDelete : sRawUri, deleteWhere, deleteWhereArgs);
        if (rows == 0) {
            loge("No rows were deleted from raw table!");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Deleted ");
        stringBuilder.append(rows);
        stringBuilder.append(" rows from raw table.");
        log(stringBuilder.toString());
    }

    private Bundle handleSmsWhitelisting(ComponentName target) {
        String pkgName;
        String reason;
        if (target != null) {
            pkgName = target.getPackageName();
            reason = "sms-app";
        } else {
            pkgName = this.mContext.getPackageName();
            reason = "sms-broadcast";
        }
        try {
            long duration = this.mDeviceIdleController.addPowerSaveTempWhitelistAppForSms(pkgName, 0, reason);
            BroadcastOptions bopts = BroadcastOptions.makeBasic();
            bopts.setTemporaryAppWhitelistDuration(duration);
            return bopts.toBundle();
        } catch (RemoteException e) {
            return null;
        }
    }

    private void dispatchSmsDeliveryIntent(byte[][] pdus, String format, int destPort, SmsBroadcastReceiver resultReceiver) {
        Intent intent = new Intent();
        intent.putExtra("pdus", pdus);
        intent.putExtra("format", format);
        if (destPort == -1) {
            intent.setAction("android.provider.Telephony.SMS_DELIVER");
            ComponentName componentName = SmsApplication.getDefaultSmsApplication(this.mContext, true);
            if (componentName != null) {
                intent.setComponent(componentName);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Delivering SMS to: ");
                stringBuilder.append(componentName.getPackageName());
                stringBuilder.append(" ");
                stringBuilder.append(componentName.getClassName());
                log(stringBuilder.toString());
            } else {
                intent.setComponent(null);
            }
            if (SmsManager.getDefault().getAutoPersisting()) {
                Uri uri = writeInboxMessage(intent);
                if (uri != null) {
                    intent.putExtra("uri", uri.toString());
                }
            }
            if (this.mPhone.getAppSmsManager().handleSmsReceivedIntent(intent)) {
                dropSms(resultReceiver);
                return;
            }
        }
        intent.setAction("android.intent.action.DATA_SMS_RECEIVED");
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("sms://localhost:");
        stringBuilder2.append(destPort);
        intent.setData(Uri.parse(stringBuilder2.toString()));
        intent.setComponent(null);
        intent.addFlags(16777216);
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, handleSmsWhitelisting(intent.getComponent()), resultReceiver, UserHandle.SYSTEM);
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x00ed  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean duplicateExists(InboundSmsTracker tracker) throws SQLException {
        Throwable th;
        String address = tracker.getAddress();
        String refNumber = Integer.toString(tracker.getReferenceNumber());
        String count = Integer.toString(tracker.getMessageCount());
        String seqNumber = Integer.toString(tracker.getSequenceNumber());
        String date = Long.toString(tracker.getTimestamp());
        String messageBody = tracker.getMessageBody();
        if (messageBody == null) {
            log("messageBody is null");
            messageBody = "";
        }
        if (tracker.getMessageCount() == 1) {
            messageBody = "address=? AND reference_number=? AND count=? AND sequence=? AND date=? AND message_body=? AND sub_id=?";
        } else {
            messageBody = tracker.getQueryForMultiPartDuplicatesSubId();
            scanAndDeleteOlderPartialMessages(tracker);
        }
        String where = messageBody;
        Cursor cursor = null;
        Cursor cursor2;
        try {
            cursor2 = this.mResolver.query(sRawUri, PDU_PROJECTION, where, new String[]{address, refNumber, count, seqNumber, date, messageBody, String.valueOf(this.mPhone.getSubId())}, null);
            if (cursor2 != null) {
                try {
                    if (cursor2.moveToNext()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Discarding duplicate message segment, refNumber=");
                        stringBuilder.append(refNumber);
                        stringBuilder.append(" seqNumber=");
                        stringBuilder.append(seqNumber);
                        stringBuilder.append(" count=");
                        stringBuilder.append(count);
                        loge(stringBuilder.toString());
                        messageBody = cursor2.getString(0);
                        byte[] pdu = tracker.getPdu();
                        byte[] oldPdu = HexDump.hexStringToByteArray(messageBody);
                        if (!Arrays.equals(oldPdu, tracker.getPdu())) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Warning: dup message segment PDU of length ");
                            stringBuilder2.append(pdu.length);
                            stringBuilder2.append(" is different from existing PDU of length ");
                            stringBuilder2.append(oldPdu.length);
                            loge(stringBuilder2.toString());
                        }
                        if (cursor2 != null) {
                            cursor2.close();
                        }
                        return true;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (cursor2 != null) {
                        cursor2.close();
                    }
                    throw th;
                }
            }
            if (cursor2 != null) {
                cursor2.close();
            }
            return false;
        } catch (Throwable th3) {
            th = th3;
            cursor2 = cursor;
            if (cursor2 != null) {
            }
            throw th;
        }
    }

    private int addTrackerToRawTable(InboundSmsTracker tracker, boolean deDup) {
        InboundSmsTracker inboundSmsTracker = tracker;
        if (deDup) {
            try {
                if (duplicateExists(tracker)) {
                    return 5;
                }
            } catch (SQLException e) {
                SQLException sQLException = e;
                loge("Can't access SMS database", e);
                return 2;
            }
        }
        logd("Skipped message de-duping logic");
        String address = tracker.getAddress();
        String refNumber = Integer.toString(tracker.getReferenceNumber());
        String count = Integer.toString(tracker.getMessageCount());
        ContentValues values = tracker.getContentValues();
        values.put("sub_id", Integer.valueOf(this.mPhone.getSubId()));
        values.put("receive_time", Long.valueOf(System.currentTimeMillis()));
        Uri newUri = this.mResolver.insert(sRawUri, values);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("URI of new row -> ");
        stringBuilder.append(newUri);
        log(stringBuilder.toString());
        try {
            long rowId = ContentUris.parseId(newUri);
            if (tracker.getMessageCount() == 1) {
                inboundSmsTracker.setDeleteWhere(SELECT_BY_ID, new String[]{Long.toString(rowId)});
            } else {
                inboundSmsTracker.setDeleteWhere(tracker.getQueryForSegmentsSubId(), new String[]{address, refNumber, count, String.valueOf(this.mPhone.getSubId())});
            }
            return 1;
        } catch (Exception e2) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("error parsing URI for new row: ");
            stringBuilder2.append(newUri);
            loge(stringBuilder2.toString(), e2);
            return 2;
        }
    }

    static boolean isCurrentFormat3gpp2() {
        return 2 == TelephonyManager.getDefault().getCurrentPhoneType();
    }

    private void dropSms(SmsBroadcastReceiver receiver) {
        deleteFromRawTable(receiver.mDeleteWhere, receiver.mDeleteWhereArgs, 2);
        sendMessage(3);
    }

    private boolean isSkipNotifyFlagSet(int callbackResult) {
        return (callbackResult & 2) > 0;
    }

    protected void log(String s) {
        Rlog.d(getName(), s);
    }

    protected void loge(String s) {
        Rlog.e(getName(), s);
    }

    protected void loge(String s, Throwable e) {
        Rlog.e(getName(), s, e);
    }

    private Uri writeInboxMessage(Intent intent) {
        SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length < 1) {
            loge("Failed to parse SMS pdu");
            return null;
        }
        int length = messages.length;
        int i = 0;
        while (i < length) {
            try {
                messages[i].getDisplayMessageBody();
                i++;
            } catch (NullPointerException e) {
                loge("NPE inside SmsMessage");
                return null;
            }
        }
        ContentValues values = parseSmsMessage(messages);
        long identity = Binder.clearCallingIdentity();
        Uri e2;
        try {
            e2 = this.mContext.getContentResolver().insert(Inbox.CONTENT_URI, values);
            return e2;
        } catch (Exception e3) {
            e2 = e3;
            loge("Failed to persist inbox message", e2);
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static ContentValues parseSmsMessage(SmsMessage[] msgs) {
        SmsMessage sms = msgs[0];
        ContentValues values = new ContentValues();
        values.put("address", sms.getDisplayOriginatingAddress());
        values.put("body", buildMessageBodyFromPdus(msgs));
        values.put("date_sent", Long.valueOf(sms.getTimestampMillis()));
        values.put("date", Long.valueOf(System.currentTimeMillis()));
        values.put("protocol", Integer.valueOf(sms.getProtocolIdentifier()));
        values.put("seen", Integer.valueOf(0));
        values.put("read", Integer.valueOf(0));
        String subject = sms.getPseudoSubject();
        if (!TextUtils.isEmpty(subject)) {
            values.put("subject", subject);
        }
        values.put("reply_path_present", Integer.valueOf(sms.isReplyPathPresent()));
        values.put("service_center", sms.getServiceCenterAddress());
        return values;
    }

    private static String buildMessageBodyFromPdus(SmsMessage[] msgs) {
        int i = 0;
        if (msgs.length == 1) {
            return replaceFormFeeds(msgs[0].getDisplayMessageBody());
        }
        StringBuilder body = new StringBuilder();
        int length = msgs.length;
        while (i < length) {
            body.append(msgs[i].getDisplayMessageBody());
            i++;
        }
        return replaceFormFeeds(body.toString());
    }

    private static String replaceFormFeeds(String s) {
        return s == null ? "" : s.replace(12, 10);
    }

    @VisibleForTesting
    public WakeLock getWakeLock() {
        return this.mWakeLock;
    }

    @VisibleForTesting
    public int getWakeLockTimeout() {
        return this.mWakeLockTimeout;
    }

    private void setWakeLockTimeout(int timeOut) {
        this.mWakeLockTimeout = timeOut;
    }

    static void registerNewMessageNotificationActionHandler(Context context) {
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction(ACTION_OPEN_SMS_APP);
        context.registerReceiver(new NewMessageNotificationActionReceiver(), userFilter);
    }
}
