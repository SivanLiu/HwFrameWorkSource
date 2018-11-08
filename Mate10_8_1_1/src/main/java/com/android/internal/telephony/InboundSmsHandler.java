package com.android.internal.telephony;

import android.app.ActivityManager;
import android.app.BroadcastOptions;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
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
import com.android.internal.telephony.CarrierServicesSmsFilter.CarrierServicesSmsFilterCallbackInterface;
import com.android.internal.telephony.SmsConstants.MessageClass;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class InboundSmsHandler extends StateMachine {
    private static String ACTION_OPEN_SMS_APP = "com.android.internal.telephony.OPEN_DEFAULT_SMS_APP";
    public static final int ADDRESS_COLUMN = 6;
    public static final int COUNT_COLUMN = 5;
    public static final int DATE_COLUMN = 3;
    protected static final boolean DBG = true;
    private static final int DELETE_PERMANENTLY = 1;
    public static final int DESTINATION_PORT_COLUMN = 2;
    public static final int DISPLAY_ADDRESS_COLUMN = 9;
    protected static final int EVENT_BROADCAST_COMPLETE = 3;
    public static final int EVENT_BROADCAST_SMS = 2;
    public static final int EVENT_INJECT_SMS = 8;
    public static final int EVENT_NEW_SMS = 1;
    private static final int EVENT_RELEASE_WAKELOCK = 5;
    private static final int EVENT_RETURN_TO_IDLE = 4;
    public static final int EVENT_START_ACCEPTING_SMS = 6;
    private static final int EVENT_STATE_TIMEOUT = 10;
    private static final int EVENT_UPDATE_PHONE_OBJECT = 7;
    private static final int EVENT_UPDATE_TRACKER = 9;
    public static final int ID_COLUMN = 7;
    private static final String INCOMING_SMS_EXCEPTION_PATTERN = "incoming_sms_exception_pattern";
    private static final String INCOMING_SMS_RESTRICTION_PATTERN = "incoming_sms_restriction_pattern";
    private static final String INTERCEPTION_SMS_RECEIVED = "android.provider.Telephony.INTERCEPTION_SMS_RECEIVED";
    private static final int MARK_DELETED = 2;
    private static final int MAX_SMS_LIST_DEFAULT = 30;
    public static final int MESSAGE_BODY_COLUMN = 8;
    private static final int NOTIFICATION_ID_NEW_MESSAGE = 1;
    private static final String NOTIFICATION_TAG = "InboundSmsHandler";
    private static final int NO_DESTINATION_PORT = -1;
    static final long PARTIAL_SEGMENT_EXPIRE_TIME = (((long) SystemProperties.getInt("ro.config.hw_drop_oldsms_day", 3)) * 86400000);
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
    public static final int STATE_TIMEOUT = 300000;
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
    private final DefaultState mDefaultState = new DefaultState();
    private final DeliveringState mDeliveringState = new DeliveringState();
    IDeviceIdleController mDeviceIdleController;
    private HwCustInboundSmsHandler mHwCust;
    private final IdleState mIdleState = new IdleState();
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
    private final StartupState mStartupState = new StartupState();
    protected SmsStorageMonitor mStorageMonitor;
    private Runnable mUpdateCountRunner = new Runnable() {
        public void run() {
            if (InboundSmsHandler.this.mAlreadyReceivedSms.get() > 0) {
                HwRadarUtils.report(InboundSmsHandler.this.mContext, HwRadarUtils.ERR_SMS_RECEIVE, "sms receive fail:" + InboundSmsHandler.this.defaultSmsApplicationName, InboundSmsHandler.this.subIdForReceivedSms);
            }
            InboundSmsHandler.this.mAlreadyReceivedSms.set(0);
        }
    };
    private UserManager mUserManager;
    private final WaitingState mWaitingState = new WaitingState();
    private final WakeLock mWakeLock;
    private int mWakeLockTimeout;
    private final WapPushOverSms mWapPush;
    private int subIdForReceivedSms = -1;

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
            InboundSmsHandler.this.logv("onFilterComplete: result is " + result);
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

    private class DefaultState extends State {
        private DefaultState() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 7:
                    InboundSmsHandler.this.onUpdatePhoneObject((Phone) msg.obj);
                    break;
                default:
                    String errorText = "processMessage: unhandled message type " + msg.what + " currState=" + InboundSmsHandler.this.getCurrentState().getName();
                    if (!Build.IS_DEBUGGABLE) {
                        InboundSmsHandler.this.loge(errorText);
                        break;
                    }
                    InboundSmsHandler.this.loge("---- Dumping InboundSmsHandler ----");
                    InboundSmsHandler.this.loge("Total records=" + InboundSmsHandler.this.getLogRecCount());
                    for (int i = Math.max(InboundSmsHandler.this.getLogRecSize() - 20, 0); i < InboundSmsHandler.this.getLogRecSize(); i++) {
                        if (InboundSmsHandler.this.getLogRec(i) != null) {
                            InboundSmsHandler.this.loge("Rec[%d]: %s\n" + i + InboundSmsHandler.this.getLogRec(i).toString());
                        }
                    }
                    InboundSmsHandler.this.loge("---- Dumped InboundSmsHandler ----");
                    throw new RuntimeException(errorText);
            }
            return true;
        }
    }

    private class DeliveringState extends State {
        private DeliveringState() {
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Delivering state");
        }

        public void exit() {
            InboundSmsHandler.this.log("leaving Delivering state");
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler.this.log("DeliveringState.processMessage:" + msg.what);
            switch (msg.what) {
                case 1:
                    InboundSmsHandler.this.handleNewSms((AsyncResult) msg.obj);
                    InboundSmsHandler.this.sendMessage(4);
                    return true;
                case 2:
                    InboundSmsTracker inboundSmsTracker = msg.obj;
                    if (InboundSmsHandler.this.processMessagePart(inboundSmsTracker)) {
                        InboundSmsHandler.this.sendMessage(9, inboundSmsTracker);
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

        public void enter() {
            InboundSmsHandler.this.log("entering Idle state");
            InboundSmsHandler.this.sendMessageDelayed(5, (long) InboundSmsHandler.this.getWakeLockTimeout());
        }

        public void exit() {
            InboundSmsHandler.this.mWakeLock.acquire();
            InboundSmsHandler.this.log("acquired wakelock, leaving Idle state");
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler.this.log("IdleState.processMessage:" + msg.what);
            InboundSmsHandler.this.log("Idle state processing message type " + msg.what);
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
                    Intent intent2 = intent;
                    InboundSmsHandler.this.dispatchIntent(intent2, "android.permission.RECEIVE_SMS", 16, InboundSmsHandler.this.handleSmsWhitelisting(null), this, UserHandle.ALL);
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
                    if (!("android.intent.action.DATA_SMS_RECEIVED".equals(action) || ("android.provider.Telephony.SMS_RECEIVED".equals(action) ^ 1) == 0 || ("android.intent.action.DATA_SMS_RECEIVED".equals(action) ^ 1) == 0 || ("android.provider.Telephony.WAP_PUSH_RECEIVED".equals(action) ^ 1) == 0)) {
                        InboundSmsHandler.this.loge("unexpected BroadcastReceiver action: " + action);
                    }
                    if ("true".equals(Systemex.getString(InboundSmsHandler.this.mResolver, "disableErrWhenReceiveSMS"))) {
                        rc = -1;
                    } else {
                        rc = getResultCode();
                    }
                    if (rc == -1 || rc == 1) {
                        InboundSmsHandler.this.log("successful broadcast, deleting from raw table.");
                    } else {
                        InboundSmsHandler.this.loge("a broadcast receiver set the result code to " + rc + ", deleting from raw table anyway!");
                    }
                    InboundSmsHandler.this.deleteFromRawTable(this.mDeleteWhere, this.mDeleteWhereArgs, 2);
                    InboundSmsHandler.this.sendMessage(3);
                    int durationMillis = (int) ((System.nanoTime() - this.mBroadcastTimeNano) / 1000000);
                    if (durationMillis >= AbstractPhoneBase.SET_TO_AOTO_TIME) {
                        InboundSmsHandler.this.loge("Slow ordered broadcast completion time: " + durationMillis + " ms");
                    } else {
                        InboundSmsHandler.this.log("ordered broadcast completed in: " + durationMillis + " ms");
                    }
                    InboundSmsHandler.this.reportSmsReceiveTimeout(durationMillis);
                }
            }
        }
    }

    private class StartupState extends State {
        private StartupState() {
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Startup state");
            InboundSmsHandler.this.setWakeLockTimeout(0);
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler.this.log("StartupState.processMessage:" + msg.what);
            switch (msg.what) {
                case 1:
                case 2:
                case 8:
                    InboundSmsHandler.this.deferMessage(msg);
                    return true;
                case 6:
                    InboundSmsHandler.this.transitionTo(InboundSmsHandler.this.mIdleState);
                    return true;
                default:
                    return false;
            }
        }
    }

    private class WaitingState extends State {
        private InboundSmsTracker mTracker;

        private WaitingState() {
        }

        public void enter() {
            InboundSmsHandler.this.log("entering Waiting state");
            this.mTracker = null;
            InboundSmsHandler.this.sendMessageDelayed(10, 300000);
        }

        public void exit() {
            InboundSmsHandler.this.log("exiting Waiting state");
            InboundSmsHandler.this.setWakeLockTimeout(InboundSmsHandler.WAKELOCK_TIMEOUT);
            InboundSmsHandler.this.removeMessages(10);
            InboundSmsHandler.this.removeMessages(9);
        }

        public boolean processMessage(Message msg) {
            InboundSmsHandler.this.log("WaitingState.processMessage:" + msg.what);
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
                case 9:
                    this.mTracker = (InboundSmsTracker) msg.obj;
                    return true;
                case 10:
                    if (this.mTracker != null) {
                        InboundSmsHandler.this.log("WaitingState.processMessage: EVENT_STATE_TIMEOUT; dropping message");
                        InboundSmsHandler.this.dropSms(new SmsBroadcastReceiver(this.mTracker));
                    } else {
                        InboundSmsHandler.this.log("WaitingState.processMessage: EVENT_STATE_TIMEOUT; mTracker is null - sending EVENT_BROADCAST_COMPLETE");
                        InboundSmsHandler.this.sendMessage(3);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    private boolean processMessagePart(com.android.internal.telephony.InboundSmsTracker r45) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x01e9 in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r44 = this;
        r4 = r45.isOldMessageInRawTable();
        if (r4 == 0) goto L_0x0010;
    L_0x0006:
        r4 = "processMessagePart, ignore old message kept in raw table.";
        r0 = r44;
        r0.log(r4);
        r4 = 0;
        return r4;
    L_0x0010:
        r36 = r45.getMessageCount();
        r23 = r45.getDestPort();
        r4 = 2948; // 0xb84 float:4.131E-42 double:1.4565E-320;
        r0 = r23;
        if (r0 == r4) goto L_0x0048;
    L_0x001e:
        r4 = com.android.internal.telephony.HwTelephonyFactory.getHwInnerSmsManager();
        r5 = 0;
        r4 = r4.isLimitNumOfSmsEnabled(r5);
        if (r4 == 0) goto L_0x0048;
    L_0x0029:
        r4 = com.android.internal.telephony.HwTelephonyFactory.getHwInnerSmsManager();
        r0 = r44;
        r5 = r0.mContext;
        r6 = 0;
        r4 = r4.isExceedSMSLimit(r5, r6);
        if (r4 == 0) goto L_0x0048;
    L_0x0038:
        r4 = r45.getDeleteWhere();
        r5 = r45.getDeleteWhereArgs();
        r6 = 2;
        r0 = r44;
        r0.deleteFromRawTable(r4, r5, r6);
        r4 = 0;
        return r4;
    L_0x0048:
        r28 = 0;
        if (r36 > 0) goto L_0x0079;
    L_0x004c:
        r4 = 3;
        r4 = new java.lang.Object[r4];
        r5 = "72298611";
        r6 = 0;
        r4[r6] = r5;
        r5 = -1;
        r5 = java.lang.Integer.valueOf(r5);
        r6 = 1;
        r4[r6] = r5;
        r5 = "processMessagePart: invalid messageCount = %d";
        r6 = 1;
        r6 = new java.lang.Object[r6];
        r7 = java.lang.Integer.valueOf(r36);
        r9 = 0;
        r6[r9] = r7;
        r5 = java.lang.String.format(r5, r6);
        r6 = 2;
        r4[r6] = r5;
        r5 = 1397638484; // 0x534e4554 float:8.859264E11 double:6.905251603E-315;
        android.util.EventLog.writeEvent(r5, r4);
        r4 = 0;
        return r4;
    L_0x0079:
        r4 = 1;
        r0 = r36;
        if (r0 != r4) goto L_0x00cd;
    L_0x007e:
        r4 = 1;
        r0 = new byte[r4][];
        r22 = r0;
        r4 = r45.getPdu();
        r5 = 0;
        r22[r5] = r4;
        r0 = r44;
        r4 = r0.mContext;
        r5 = r45.getDisplayAddress();
        r28 = com.android.internal.telephony.BlockChecker.isBlocked(r4, r5);
    L_0x0096:
        r40 = java.util.Arrays.asList(r22);
        r4 = r40.size();
        if (r4 == 0) goto L_0x00a9;
    L_0x00a0:
        r4 = 0;
        r0 = r40;
        r4 = r0.contains(r4);
        if (r4 == 0) goto L_0x027d;
    L_0x00a9:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "processMessagePart: returning false due to ";
        r5 = r4.append(r5);
        r4 = r40.size();
        if (r4 != 0) goto L_0x0278;
    L_0x00bb:
        r4 = "pduList.size() == 0";
    L_0x00be:
        r4 = r5.append(r4);
        r4 = r4.toString();
        r0 = r44;
        r0.loge(r4);
        r4 = 0;
        return r4;
    L_0x00cd:
        r30 = 0;
        r27 = r45.getAddress();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r45.getReferenceNumber();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r42 = java.lang.Integer.toString(r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r45.getMessageCount();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r29 = java.lang.Integer.toString(r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = 4;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r8 = new java.lang.String[r4];	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = 0;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r8[r4] = r27;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = 1;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r8[r4] = r42;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = 2;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r8[r4] = r29;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r44;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r0.mPhone;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.getSubId();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = java.lang.String.valueOf(r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = 3;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r8[r5] = r4;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r44;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r0.mResolver;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = sRawUri;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r6 = PDU_SEQUENCE_PORT_PROJECTION;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r7 = r45.getQueryForSegmentsSubId();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r9 = 0;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r30 = r4.query(r5, r6, r7, r8, r9);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r31 = r30.getCount();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = new java.lang.StringBuilder;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4.<init>();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = "processMessagePart, cursorCount: ";	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r31;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.append(r0);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = ", ref|seq/count(";	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = r45.getReferenceNumber();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = "|";	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = r45.getSequenceNumber();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = "/";	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r36;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.append(r0);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = ")";	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.toString();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r44;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0.log(r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r31;
        r1 = r36;
        if (r0 >= r1) goto L_0x016d;
    L_0x0166:
        r4 = 0;
        if (r30 == 0) goto L_0x016c;
    L_0x0169:
        r30.close();
    L_0x016c:
        return r4;
    L_0x016d:
        r0 = r36;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = new byte[r0][];	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r22 = r0;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
    L_0x0173:
        r4 = r30.moveToNext();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        if (r4 == 0) goto L_0x026a;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
    L_0x0179:
        r4 = PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = 1;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = java.lang.Integer.valueOf(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.get(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = (java.lang.Integer) r4;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.intValue();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r30;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r0.getInt(r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = r45.getIndexOffset();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r34 = r4 - r5;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r22;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r0.length;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r34;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        if (r0 >= r4) goto L_0x019f;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
    L_0x019d:
        if (r34 >= 0) goto L_0x01ea;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
    L_0x019f:
        r4 = 3;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = new java.lang.Object[r4];	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = "72298611";	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r6 = 0;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4[r6] = r5;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = -1;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = java.lang.Integer.valueOf(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r6 = 1;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4[r6] = r5;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = "processMessagePart: invalid seqNumber = %d, messageCount = %d";	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r6 = 2;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r6 = new java.lang.Object[r6];	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r7 = r45.getIndexOffset();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r7 = r7 + r34;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r7 = java.lang.Integer.valueOf(r7);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r9 = 0;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r6[r9] = r7;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r7 = java.lang.Integer.valueOf(r36);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r9 = 1;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r6[r9] = r7;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = java.lang.String.format(r5, r6);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r6 = 2;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4[r6] = r5;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = 1397638484; // 0x534e4554 float:8.859264E11 double:6.905251603E-315;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        android.util.EventLog.writeEvent(r5, r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        goto L_0x0173;
    L_0x01d8:
        r32 = move-exception;
        r4 = "Can't access multipart SMS database";	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r44;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r1 = r32;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0.loge(r4, r1);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = 0;
        if (r30 == 0) goto L_0x01e9;
    L_0x01e6:
        r30.close();
    L_0x01e9:
        return r4;
    L_0x01ea:
        r4 = PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = 0;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = java.lang.Integer.valueOf(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.get(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = (java.lang.Integer) r4;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.intValue();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r30;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r0.getString(r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = com.android.internal.util.HexDump.hexStringToByteArray(r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r22[r34] = r4;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        if (r34 != 0) goto L_0x0246;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
    L_0x0209:
        r4 = PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = 2;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = java.lang.Integer.valueOf(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.get(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = (java.lang.Integer) r4;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.intValue();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r30;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r0.isNull(r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4 ^ 1;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        if (r4 == 0) goto L_0x0246;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
    L_0x0224:
        r4 = PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = 2;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = java.lang.Integer.valueOf(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.get(r5);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = (java.lang.Integer) r4;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.intValue();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r30;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r41 = r0.getInt(r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r41 = com.android.internal.telephony.InboundSmsTracker.getRealDestPort(r41);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = -1;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r41;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        if (r0 == r4) goto L_0x0246;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
    L_0x0244:
        r23 = r41;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
    L_0x0246:
        if (r28 != 0) goto L_0x0173;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
    L_0x0248:
        r0 = r44;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r5 = r0.mContext;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = PDU_SEQUENCE_PORT_PROJECTION_INDEX_MAPPING;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r6 = 9;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r6 = java.lang.Integer.valueOf(r6);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.get(r6);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = (java.lang.Integer) r4;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r4.intValue();	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r0 = r30;	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r4 = r0.getString(r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        r28 = com.android.internal.telephony.BlockChecker.isBlocked(r5, r4);	 Catch:{ SQLException -> 0x01d8, all -> 0x0271 }
        goto L_0x0173;
    L_0x026a:
        if (r30 == 0) goto L_0x0096;
    L_0x026c:
        r30.close();
        goto L_0x0096;
    L_0x0271:
        r4 = move-exception;
        if (r30 == 0) goto L_0x0277;
    L_0x0274:
        r30.close();
    L_0x0277:
        throw r4;
    L_0x0278:
        r4 = "pduList.contains(null)";
        goto L_0x00be;
    L_0x027d:
        r12 = new com.android.internal.telephony.InboundSmsHandler$SmsBroadcastReceiver;
        r0 = r44;
        r1 = r45;
        r12.<init>(r1);
        r0 = r44;
        r4 = r0.mUserManager;
        r4 = r4.isUserUnlocked();
        if (r4 != 0) goto L_0x029d;
    L_0x0290:
        r0 = r44;
        r1 = r45;
        r2 = r22;
        r3 = r23;
        r4 = r0.processMessagePartWithUserLocked(r1, r2, r3, r12);
        return r4;
    L_0x029d:
        r4 = 2948; // 0xb84 float:4.131E-42 double:1.4565E-320;
        r0 = r23;
        if (r0 != r4) goto L_0x0393;
    L_0x02a3:
        r38 = new java.io.ByteArrayOutputStream;
        r38.<init>();
        r11 = 0;
        r4 = 0;
        r0 = r22;
        r5 = r0.length;
    L_0x02ad:
        if (r4 >= r5) goto L_0x02e2;
    L_0x02af:
        r39 = r22[r4];
        r6 = r45.is3gpp2();
        if (r6 != 0) goto L_0x02ca;
    L_0x02b7:
        r6 = "3gpp";
        r0 = r39;
        r37 = android.telephony.SmsMessage.createFromPdu(r0, r6);
        if (r37 == 0) goto L_0x02d8;
    L_0x02c2:
        r39 = r37.getUserData();
        r11 = r37.getOriginatingAddress();
    L_0x02ca:
        r0 = r39;
        r6 = r0.length;
        r7 = 0;
        r0 = r38;
        r1 = r39;
        r0.write(r1, r7, r6);
        r4 = r4 + 1;
        goto L_0x02ad;
    L_0x02d8:
        r4 = "processMessagePart: SmsMessage.createFromPdu returned null";
        r0 = r44;
        r0.loge(r4);
        r4 = 0;
        return r4;
    L_0x02e2:
        r0 = r44;
        r4 = r0.mWapPush;
        r5 = r38.toByteArray();
        r0 = r44;
        r4 = r4.isWapPushForMms(r5, r0);
        if (r4 == 0) goto L_0x031c;
    L_0x02f2:
        r0 = r44;
        r4 = r0.mPhone;
        r4 = r4.mDcTracker;
        r4 = r4.isRoamingPushDisabled();
        if (r4 == 0) goto L_0x031c;
    L_0x02fe:
        r0 = r44;
        r4 = r0.mPhone;
        r4 = r4.getServiceState();
        r35 = r4.getDataRoaming();
    L_0x030a:
        if (r35 == 0) goto L_0x031f;
    L_0x030c:
        r4 = r45.getDeleteWhere();
        r5 = r45.getDeleteWhereArgs();
        r6 = 2;
        r0 = r44;
        r0.deleteFromRawTable(r4, r5, r6);
        r4 = 0;
        return r4;
    L_0x031c:
        r35 = 0;
        goto L_0x030a;
    L_0x031f:
        r0 = r44;
        r4 = r0.mWapPush;
        r0 = r45;
        r4.saveSmsTracker(r0);
        r0 = r44;
        r4 = r0.mHwCust;
        if (r4 == 0) goto L_0x0370;
    L_0x032e:
        r0 = r44;
        r4 = r0.mHwCust;
        r4 = r4.isIQIEnable();
        if (r4 == 0) goto L_0x0370;
    L_0x0338:
        r0 = r44;
        r4 = r0.mHwCust;
        r0 = r38;
        r4 = r4.isIQIWapPush(r0);
        if (r4 == 0) goto L_0x0370;
    L_0x0344:
        r4 = "check WapPush is true";
        r0 = r44;
        r0.log(r4);
        r43 = -1;
    L_0x034e:
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "dispatchWapPdu() returned ";
        r4 = r4.append(r5);
        r0 = r43;
        r4 = r4.append(r0);
        r4 = r4.toString();
        r0 = r44;
        r0.log(r4);
        r4 = -1;
        r0 = r43;
        if (r0 != r4) goto L_0x0383;
    L_0x036e:
        r4 = 1;
        return r4;
    L_0x0370:
        r0 = r44;
        r9 = r0.mWapPush;
        r10 = r38.toByteArray();
        r14 = r45.is3gpp2();
        r13 = r44;
        r43 = r9.dispatchWapPdu(r10, r11, r12, r13, r14);
        goto L_0x034e;
    L_0x0383:
        r4 = r45.getDeleteWhere();
        r5 = r45.getDeleteWhereArgs();
        r6 = 2;
        r0 = r44;
        r0.deleteFromRawTable(r4, r5, r6);
        r4 = 0;
        return r4;
    L_0x0393:
        r4 = com.android.internal.telephony.HwTelephonyFactory.getHwInnerSmsManager();
        r5 = 0;
        r4 = r4.isLimitNumOfSmsEnabled(r5);
        if (r4 == 0) goto L_0x03b0;
    L_0x039e:
        r4 = 2948; // 0xb84 float:4.131E-42 double:1.4565E-320;
        r0 = r23;
        if (r0 == r4) goto L_0x03b0;
    L_0x03a4:
        r4 = com.android.internal.telephony.HwTelephonyFactory.getHwInnerSmsManager();
        r0 = r44;
        r5 = r0.mContext;
        r6 = 0;
        r4.updateSmsUsedNum(r5, r6);
    L_0x03b0:
        r15 = new android.content.Intent;
        r15.<init>();
        r4 = "pdus";
        r0 = r22;
        r15.putExtra(r4, r0);
        r4 = "format";
        r5 = r45.getFormat();
        r15.putExtra(r4, r5);
        r0 = r44;
        r4 = r0.mPhone;
        r4 = r4.getPhoneId();
        android.telephony.SubscriptionManager.putPhoneIdAndSubIdExtra(r15, r4);
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "putPhoneIdAndSubIdExtra";
        r4 = r4.append(r5);
        r0 = r44;
        r5 = r0.mPhone;
        r5 = r5.getPhoneId();
        r4 = r4.append(r5);
        r4 = r4.toString();
        r0 = r44;
        r0.log(r4);
        r4 = -1;
        r0 = r23;
        if (r4 != r0) goto L_0x0444;
    L_0x03f8:
        r4 = com.android.internal.telephony.HwTelephonyFactory.getHwInnerSmsManager();
        r5 = r45.getAddress();
        r6 = "incoming_sms_exception_pattern";
        r0 = r44;
        r7 = r0.mPhone;
        r7 = r7.getPhoneId();
        r4 = r4.isMatchSMSPattern(r5, r6, r7);
        r4 = r4 ^ 1;
        if (r4 == 0) goto L_0x0444;
    L_0x0413:
        r4 = com.android.internal.telephony.HwTelephonyFactory.getHwInnerSmsManager();
        r5 = r45.getAddress();
        r6 = "incoming_sms_restriction_pattern";
        r0 = r44;
        r7 = r0.mPhone;
        r7 = r7.getPhoneId();
        r4 = r4.isMatchSMSPattern(r5, r6, r7);
        if (r4 == 0) goto L_0x0444;
    L_0x042c:
        r4 = r45.getDeleteWhere();
        r5 = r45.getDeleteWhereArgs();
        r6 = 2;
        r0 = r44;
        r0.deleteFromRawTable(r4, r5, r6);
        r4 = "forbid receive sms by mdm!";
        r0 = r44;
        r0.log(r4);
        r4 = 0;
        return r4;
    L_0x0444:
        r4 = -1;
        r0 = r23;
        if (r4 != r0) goto L_0x0462;
    L_0x0449:
        if (r28 == 0) goto L_0x0462;
    L_0x044b:
        r4 = com.android.internal.telephony.HwTelephonyFactory.getHwInnerSmsManager();
        r4.sendGoogleSmsBlockedRecord(r15);
        r4 = r45.getDeleteWhere();
        r5 = r45.getDeleteWhereArgs();
        r6 = 2;
        r0 = r44;
        r0.deleteFromRawTable(r4, r5, r6);
        r4 = 0;
        return r4;
    L_0x0462:
        r4 = -1;
        r0 = r23;
        if (r4 != r0) goto L_0x04a7;
    L_0x0467:
        r13 = com.android.internal.telephony.HwTelephonyFactory.getHwInnerSmsManager();
        r0 = r44;
        r14 = r0.mContext;
        r17 = r45.getDeleteWhere();
        r18 = r45.getDeleteWhereArgs();
        r19 = 0;
        r16 = r44;
        r4 = r13.newSmsShouldBeIntercepted(r14, r15, r16, r17, r18, r19);
        if (r4 == 0) goto L_0x04a7;
    L_0x0481:
        r17 = new android.content.Intent;
        r0 = r17;
        r0.<init>(r15);
        r4 = "android.provider.Telephony.INTERCEPTION_SMS_RECEIVED";
        r0 = r17;
        r0.setAction(r4);
        r4 = 0;
        r0 = r44;
        r20 = r0.handleSmsWhitelisting(r4);
        r18 = "huawei.permission.RECEIVE_SMS_INTERCEPTION";
        r22 = android.os.UserHandle.ALL;
        r19 = 16;
        r21 = 0;
        r16 = r44;
        r16.dispatchIntent(r17, r18, r19, r20, r21, r22);
        r4 = 1;
        return r4;
    L_0x04a7:
        r26 = 1;
        r21 = r44;
        r24 = r45;
        r25 = r12;
        r33 = r21.filterSms(r22, r23, r24, r25, r26);
        if (r33 != 0) goto L_0x04c2;
    L_0x04b5:
        r4 = r45.getFormat();
        r0 = r44;
        r1 = r22;
        r2 = r23;
        r0.dispatchSmsDeliveryIntent(r1, r4, r2, r12);
    L_0x04c2:
        r4 = 1;
        return r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.InboundSmsHandler.processMessagePart(com.android.internal.telephony.InboundSmsTracker):boolean");
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
        this.mSmsReceiveDisabled = TelephonyManager.from(this.mContext).getSmsReceiveCapableForPhone(this.mPhone.getPhoneId(), this.mContext.getResources().getBoolean(17957020)) ^ 1;
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
            loge("Exception processing incoming SMS: " + ar.exception);
            return;
        }
        int result;
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
                HwRadarUtils.report(this.mContext, HwRadarUtils.ERR_SMS_RECEIVE, "receive a blacklist sms, modem has acked it, fw need't reply" + this.defaultSmsApplicationName, 0);
                log("receive a blacklist sms, modem has acked it, fw need't reply");
            }
        } catch (RuntimeException ex) {
            loge("Exception dispatching message", ex);
            result = 2;
        }
        if (result != -1) {
            boolean handled = result == 1;
            if (!handled) {
                Jlog.d(50, "JL_DISPATCH_SMS_FAILED");
            }
            if (this.mHwCust == null || !this.mHwCust.isNotNotifyWappushEnabled(ar)) {
                notifyAndAcknowledgeLastIncomingSms(handled, result, null);
            } else {
                acknowledgeLastIncomingSms(handled, result, null);
            }
        }
    }

    private void handleInjectSms(AsyncResult ar) {
        int result;
        PendingIntent pendingIntent = null;
        try {
            pendingIntent = (PendingIntent) ar.userObj;
            SmsMessage sms = ar.result;
            if (sms == null) {
                result = 2;
            } else {
                result = dispatchMessage(sms.mWrappedSmsMessage);
            }
        } catch (RuntimeException ex) {
            loge("Exception dispatching message", ex);
            result = 2;
        }
        if (pendingIntent != null) {
            try {
                pendingIntent.send(result);
            } catch (CanceledException e) {
            }
        }
    }

    private int dispatchMessage(SmsMessageBase smsb) {
        if (smsb == null) {
            loge("dispatchSmsMessage: message is null");
            return 2;
        } else if (this.mSmsReceiveDisabled) {
            log("Received short message on device which doesn't support receiving SMS. Ignored.");
            return 1;
        } else if (hasSameSmsPdu(smsb.getPdu())) {
            log("receive a duplicated SMS and abandon it.");
            return 1;
        } else {
            boolean onlyCore = false;
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

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean hasSameSmsPdu(byte[] pdu) {
        log("hasSameSmsPdu: check if there is a same pdu in mSmsList.");
        synchronized (this.mSmsList) {
            for (byte[] oldPdu : this.mSmsList) {
                if (Arrays.equals(pdu, oldPdu)) {
                    return true;
                }
            }
            this.mSmsList.add(pdu);
            log("hasSameSmsPdu: mSmsList.size() = " + this.mSmsList.size());
            if (this.mSmsList.size() > 30) {
                log("hasSameSmsPdu: mSmsList.size() > MAX_SMS_LIST_DEFAULT");
                this.mSmsList.remove(0);
            }
        }
    }

    protected void onUpdatePhoneObject(Phone phone) {
        this.mPhone = phone;
        this.mStorageMonitor = this.mPhone.mSmsStorageMonitor;
        log("onUpdatePhoneObject: phone=" + this.mPhone.getClass().getSimpleName());
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
        InboundSmsTracker tracker;
        SmsHeader smsHeader = sms.getUserDataHeader();
        this.isClass0 = sms.getMessageClass() == MessageClass.CLASS_0;
        int destPort = -1;
        Jlog.d(48, "JL_DISPATCH_NORMAL_SMS");
        if (smsHeader == null || smsHeader.concatRef == null) {
            if (!(smsHeader == null || smsHeader.portAddrs == null)) {
                destPort = smsHeader.portAddrs.destPort;
                log("destination port: " + destPort);
            }
            tracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(sms.getPdu(), sms.getTimestampMillis(), destPort, is3gpp2(), false, sms.getOriginatingAddress(), sms.getDisplayOriginatingAddress(), sms.getMessageBody());
        } else {
            ConcatRef concatRef = smsHeader.concatRef;
            PortAddrs portAddrs = smsHeader.portAddrs;
            destPort = portAddrs != null ? portAddrs.destPort : -1;
            tracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(sms.getPdu(), sms.getTimestampMillis(), destPort, is3gpp2(), sms.getOriginatingAddress(), sms.getDisplayOriginatingAddress(), concatRef.refNumber, concatRef.seqNumber, concatRef.msgCount, false, sms.getMessageBody());
        }
        if (SystemProperties.getBoolean("ro.config.comm_serv_tid_enable", false) && this.mHwCust != null) {
            if (this.mHwCust.dispatchMessageByDestPort(destPort, sms, this.mContext)) {
                return 1;
            }
        }
        log("dispatchNormalMessage and created tracker");
        return addTrackerToRawTableAndSendMessage(tracker, tracker.getDestPort() == -1);
    }

    protected int addTrackerToRawTableAndSendMessage(InboundSmsTracker tracker, boolean deDup) {
        switch (addTrackerToRawTable(tracker, deDup)) {
            case 1:
                if (((PowerManager) this.mContext.getSystemService("power")).isScreenOn()) {
                    Jlog.d(50, "JL_DISPATCH_SMS_FAILED");
                } else {
                    Jlog.d(49, "JL_SEND_BROADCAST_SMS");
                }
                sendMessage(2, tracker);
                return 1;
            case 5:
                return 1;
            default:
                return 2;
        }
    }

    private void scanAndDeleteOlderPartialMessages(InboundSmsTracker tracker) {
        String address = tracker.getAddress();
        String refNumber = Integer.toString(tracker.getReferenceNumber());
        String count = Integer.toString(tracker.getMessageCount());
        int delCount = 0;
        try {
            delCount = this.mResolver.delete(sRawUri, new StringBuilder("date < " + (tracker.getTimestamp() - PARTIAL_SEGMENT_EXPIRE_TIME) + " AND " + SELECT_BY_REFERENCE).toString(), new String[]{address, refNumber, count});
        } catch (Exception e) {
            loge("scanAndDeleteOlderPartialMessages got exception ", e);
        }
        if (delCount > 0) {
            log("scanAndDeleteOlderPartialMessages: delete " + delCount + " raw sms older than " + PARTIAL_SEGMENT_EXPIRE_TIME + " days for " + tracker.getAddress());
        }
    }

    private boolean processMessagePartWithUserLocked(InboundSmsTracker tracker, byte[][] pdus, int destPort, SmsBroadcastReceiver resultReceiver) {
        log("Credential-encrypted storage not available. Port: " + destPort);
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
            ((NotificationManager) this.mContext.getSystemService("notification")).notify(NOTIFICATION_TAG, 1, new Builder(this.mContext).setSmallIcon(17301646).setAutoCancel(true).setVisibility(1).setDefaults(-1).setContentTitle(this.mContext.getString(17040481)).setContentText(this.mContext.getString(17040480)).setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_OPEN_SMS_APP), 1073741824)).setChannelId(NotificationChannelController.CHANNEL_ID_SMS).build());
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

    public void dispatchIntent(Intent intent, String permission, int appOp, Bundle opts, BroadcastReceiver resultReceiver, UserHandle user) {
        intent.addFlags(134217728);
        String action = intent.getAction();
        if ("android.provider.Telephony.SMS_DELIVER".equals(action) || "android.provider.Telephony.SMS_RECEIVED".equals(action) || "android.provider.Telephony.WAP_PUSH_DELIVER".equals(action) || "android.provider.Telephony.WAP_PUSH_RECEIVED".equals(action)) {
            intent.addFlags(268435456);
        }
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        if (user.equals(UserHandle.ALL)) {
            int[] users = null;
            try {
                users = ActivityManager.getService().getRunningUserIds();
            } catch (RemoteException e) {
            }
            if (users == null) {
                users = new int[]{user.getIdentifier()};
            }
            for (int i = users.length - 1; i >= 0; i--) {
                UserHandle targetUser = new UserHandle(users[i]);
                if (users[i] != 0) {
                    if (!this.mUserManager.hasUserRestriction("no_sms", targetUser)) {
                        UserInfo info = this.mUserManager.getUserInfo(users[i]);
                        if (info != null) {
                            if (info.isManagedProfile()) {
                            }
                        }
                    }
                }
                this.mContext.sendOrderedBroadcastAsUser(intent, targetUser, permission, appOp, opts, users[i] == 0 ? resultReceiver : null, getHandler(), -1, null, null);
            }
            return;
        }
        this.mContext.sendOrderedBroadcastAsUser(intent, user, permission, appOp, opts, resultReceiver, getHandler(), -1, null, null);
        triggerInboxInsertDoneDetect(intent);
    }

    private void triggerInboxInsertDoneDetect(Intent intent) {
        if ("android.provider.Telephony.SMS_DELIVER".equals(intent.getAction()) && (this.isClass0 ^ 1) != 0) {
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
            HwRadarUtils.report(this.mContext, HwRadarUtils.ERR_SMS_RECEIVE, "sms receive timeout:" + durationMillis + this.defaultSmsApplicationName, this.subIdForReceivedSms);
        }
    }

    private void deleteFromRawTable(String deleteWhere, String[] deleteWhereArgs, int deleteType) {
        int rows = this.mResolver.delete(deleteType == 1 ? sRawUriPermanentDelete : sRawUri, deleteWhere, deleteWhereArgs);
        if (rows == 0) {
            loge("No rows were deleted from raw table!");
        } else {
            log("Deleted " + rows + " rows from raw table.");
        }
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
                log("Delivering SMS to: " + componentName.getPackageName() + " " + componentName.getClassName());
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
        intent.setData(Uri.parse("sms://localhost:" + destPort));
        intent.setComponent(null);
        intent.addFlags(16777216);
        dispatchIntent(intent, "android.permission.RECEIVE_SMS", 16, handleSmsWhitelisting(intent.getComponent()), resultReceiver, UserHandle.SYSTEM);
    }

    private boolean duplicateExists(InboundSmsTracker tracker) throws SQLException {
        String where;
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
            where = "address=? AND reference_number=? AND count=? AND sequence=? AND date=? AND message_body=? AND sub_id=?";
        } else {
            where = tracker.getQueryForMultiPartDuplicatesSubId();
            scanAndDeleteOlderPartialMessages(tracker);
        }
        Cursor cursor = null;
        try {
            cursor = this.mResolver.query(sRawUri, PDU_PROJECTION, where, new String[]{address, refNumber, count, seqNumber, date, messageBody, String.valueOf(this.mPhone.getSubId())}, null);
            if (cursor == null || !cursor.moveToNext()) {
                if (cursor != null) {
                    cursor.close();
                }
                return false;
            }
            loge("Discarding duplicate message segment, refNumber=" + refNumber + " seqNumber=" + seqNumber + " count=" + count);
            String oldPduString = cursor.getString(0);
            byte[] pdu = tracker.getPdu();
            byte[] oldPdu = HexDump.hexStringToByteArray(oldPduString);
            if (!Arrays.equals(oldPdu, tracker.getPdu())) {
                loge("Warning: dup message segment PDU of length " + pdu.length + " is different from existing PDU of length " + oldPdu.length);
            }
            return true;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private int addTrackerToRawTable(InboundSmsTracker tracker, boolean deDup) {
        if (deDup) {
            try {
                if (duplicateExists(tracker)) {
                    return 5;
                }
            } catch (SQLException e) {
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
        log("URI of new row -> " + newUri);
        try {
            long rowId = ContentUris.parseId(newUri);
            if (tracker.getMessageCount() == 1) {
                tracker.setDeleteWhere(SELECT_BY_ID, new String[]{Long.toString(rowId)});
            } else {
                InboundSmsTracker inboundSmsTracker = tracker;
                inboundSmsTracker.setDeleteWhere(tracker.getQueryForSegmentsSubId(), new String[]{address, refNumber, count, String.valueOf(this.mPhone.getSubId())});
            }
            return 1;
        } catch (Exception e2) {
            loge("error parsing URI for new row: " + newUri, e2);
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
        Uri insert;
        SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length < 1) {
            loge("Failed to parse SMS pdu");
            return null;
        }
        int i = 0;
        int length = messages.length;
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
        try {
            insert = this.mContext.getContentResolver().insert(Inbox.CONTENT_URI, values);
            return insert;
        } catch (Exception e2) {
            insert = "Failed to persist inbox message";
            loge(insert, e2);
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static ContentValues parseSmsMessage(SmsMessage[] msgs) {
        int i = 0;
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
        String str = "reply_path_present";
        if (sms.isReplyPathPresent()) {
            i = 1;
        }
        values.put(str, Integer.valueOf(i));
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
        return s == null ? "" : s.replace('\f', '\n');
    }

    public WakeLock getWakeLock() {
        return this.mWakeLock;
    }

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
