package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Telephony.Carriers;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.vsim.HwVSimConstants;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class HwDataServiceChrManagerImpl implements HwDataServiceChrManager {
    private static final String CHR_BROADCAST_PERMISSION = "com.huawei.android.permission.GET_CHR_DATA";
    private static final int CLEANUP = 1;
    private static final boolean DBG = true;
    private static final int EVENT_APN_CHANGED = 4;
    private static final int EVENT_APN_CONNECTION_INFO_INTENT = 5;
    private static final int EVENT_GET_CDMA_CHR_INFO = 2;
    private static final int EVENT_TIMER_EXPIRE = 1;
    private static final int EVENT_WIFI_DISCONNECT_TIMER_EXPIRE = 3;
    private static final int GET_DATA_CALL_LIST = 0;
    private static final String LOG_TAG = "HwChrManagerImpl";
    private static final int MAX_PHONENUM = 3;
    private static final int MAX_SLOT = 3;
    private static final int RADIO_RESTART = 3;
    private static final int RADIO_RESTART_WITH_PROP = 4;
    private static final int REREGISTER = 2;
    private static final int TIMER_INTERVAL_CDMA_PDP_SAME_STATUS = 60000;
    private static final int TIMER_INTERVAL_CHECK_SIMLOADED_ISRECEIVED = 180000;
    private static final long TIMER_INTERVAL_SEND_APN_INFO_INTENT = 3000;
    private static Context mContext;
    private static HwDataServiceChrManager mInstance = new HwDataServiceChrManagerImpl();
    private int WIFI_TO_MOBILE_MONITOR_TIMER_INTERVAL = 20000;
    private Phone[] mActivePhone = new Phone[3];
    private ApnChangeObserver mApnObserver;
    private Timer mCdmaPdpSameStatusTimer;
    private GsmCdmaPhone mCdmaPhone;
    private boolean mCheckApnContextState = false;
    private int mChrCdmaPdpRilFailCause;
    private String mDataNotAllowedReason = null;
    private int mDataSubId;
    private boolean[] mDefaultAPNReported = new boolean[]{false, false, false};
    private ApnContext[] mDefaultApnContext = new ApnContext[3];
    private boolean[] mDunAPNReported = new boolean[]{false, false, false};
    private String mGetAnyDataEnabledFalseReason = null;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            StringBuilder stringBuilder;
            String str;
            try {
                switch (msg.what) {
                    case 1:
                        String str2 = HwDataServiceChrManagerImpl.LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("received EVENT_TIMER_EXPIRE message!isReceivedEventRecordsLoaded = ");
                        stringBuilder.append(HwDataServiceChrManagerImpl.this.getReceivedSimloadedMsg());
                        Log.d(str2, stringBuilder.toString());
                        HwDataServiceChrManagerImpl.this.stopTimer(HwDataServiceChrManagerImpl.this.mSimloadedTimer);
                        HwDataServiceChrManagerImpl.this.mSimloadedTimer = null;
                        HwDataServiceChrManagerImpl.this.stopTimer(HwDataServiceChrManagerImpl.this.mCdmaPdpSameStatusTimer);
                        HwDataServiceChrManagerImpl.this.mCdmaPdpSameStatusTimer = null;
                        HwDataServiceChrManagerImpl.this.cleanLastStatus();
                        HwDataServiceChrManagerImpl.this.mUserDataEnabled = TelephonyManager.getDefault().getDataEnabled();
                        if (HwDataServiceChrManagerImpl.this.getSimCardState(HwDataServiceChrManagerImpl.this.mDataSubId) == 1 || HwDataServiceChrManagerImpl.this.getSimCardState(HwDataServiceChrManagerImpl.this.mDataSubId) == 0) {
                            Log.d(HwDataServiceChrManagerImpl.LOG_TAG, "Timer expire,sim card is absent!");
                            return;
                        }
                        if (SubscriptionController.getInstance().getSubState(HwDataServiceChrManagerImpl.this.mDataSubId) == 0) {
                            Log.d(HwDataServiceChrManagerImpl.LOG_TAG, "startTimer again,getSubState = INACTIVE");
                            HwDataServiceChrManagerImpl.this.mSimloadedTimer = HwDataServiceChrManagerImpl.this.startTimer(HwDataServiceChrManagerImpl.this.mSimloadedTimer, HwDataServiceChrManagerImpl.TIMER_INTERVAL_CHECK_SIMLOADED_ISRECEIVED);
                        } else if (HwDataServiceChrManagerImpl.this.mDataSubId == SubscriptionController.getInstance().getDefaultDataSubId()) {
                            if (true == HwDataServiceChrManagerImpl.this.getRecordsLoadedRegistered() && !HwDataServiceChrManagerImpl.this.getReceivedSimloadedMsg()) {
                                str = HwDataServiceChrManagerImpl.LOG_TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Timer expire,simloaded msg is not received,sim card is present!trigger chr!SubId:");
                                stringBuilder.append(HwDataServiceChrManagerImpl.this.mDataSubId);
                                stringBuilder.append(" getSimCardState = ");
                                stringBuilder.append(HwDataServiceChrManagerImpl.this.getSimCardState(HwDataServiceChrManagerImpl.this.mDataSubId));
                                Log.d(str, stringBuilder.toString());
                                HwDataServiceChrManagerImpl.this.sendIntentWhenSimloadedMsgIsNotReceived(HwDataServiceChrManagerImpl.this.mDataSubId);
                            } else if (!HwDataServiceChrManagerImpl.this.getBringUp() && HwDataServiceChrManagerImpl.this.mDataSubId >= 0 && 3 > HwDataServiceChrManagerImpl.this.mDataSubId && HwDataServiceChrManagerImpl.this.mDefaultApnContext[HwDataServiceChrManagerImpl.this.mDataSubId] != null && HwDataServiceChrManagerImpl.this.mActivePhone[HwDataServiceChrManagerImpl.this.mDataSubId] != null && true == HwDataServiceChrManagerImpl.this.mDefaultApnContext[HwDataServiceChrManagerImpl.this.mDataSubId].isEnabled() && true == HwDataServiceChrManagerImpl.this.mUserDataEnabled && HwDataServiceChrManagerImpl.this.mActivePhone[HwDataServiceChrManagerImpl.this.mDataSubId].getServiceState().getDataRegState() == 0) {
                                Log.d(HwDataServiceChrManagerImpl.LOG_TAG, "Timer expire,pdp activing process return error,when data switch is on and ps is attached!");
                                HwDataServiceChrManagerImpl.this.sendIntentWhenPdpActFailBlockAtFw(HwDataServiceChrManagerImpl.this.mDataSubId);
                                HwDataServiceChrManagerImpl.this.setDataNotAllowedReasonToNull();
                                HwDataServiceChrManagerImpl.this.setAnyDataEnabledFalseReasonToNull();
                            }
                            str = HwDataServiceChrManagerImpl.LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("isBringUp=");
                            stringBuilder.append(HwDataServiceChrManagerImpl.this.getBringUp());
                            stringBuilder.append(" mDataSubId =");
                            stringBuilder.append(HwDataServiceChrManagerImpl.this.mDataSubId);
                            stringBuilder.append(" defaultApnContext.isEnabled()");
                            stringBuilder.append(HwDataServiceChrManagerImpl.this.mDefaultApnContext[HwDataServiceChrManagerImpl.this.mDataSubId].isEnabled());
                            stringBuilder.append(" mUserDataEnabled=");
                            stringBuilder.append(HwDataServiceChrManagerImpl.this.mUserDataEnabled);
                            stringBuilder.append(" isDataConnectionAttached=");
                            stringBuilder.append(HwDataServiceChrManagerImpl.this.mIsDataConnectionAttached);
                            Log.d(str, stringBuilder.toString());
                        }
                        return;
                    case 2:
                        HwDataServiceChrManagerImpl.this.sendIntentWhenCdmaPdpActFail(msg.obj);
                        HwDataServiceChrManagerImpl.this.mChrCdmaPdpRilFailCause = -1;
                        return;
                    case 3:
                        int DataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
                        if (DataSubId < 3) {
                            if (DataSubId >= 0) {
                                str = HwDataServiceChrManagerImpl.LOG_TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("handleMessage: DataSubId = ");
                                stringBuilder.append(DataSubId);
                                stringBuilder.append(" msg=");
                                stringBuilder.append(msg);
                                Log.d(str, stringBuilder.toString());
                                if (HwDataServiceChrManagerImpl.this.mDefaultApnContext[DataSubId] == null || HwDataServiceChrManagerImpl.this.mDefaultApnContext[DataSubId].isEnabled() || HwDataServiceChrManagerImpl.this.isWifiConnected()) {
                                    Log.d(HwDataServiceChrManagerImpl.LOG_TAG, "wifi to mobile process is ok, DefaultTypeAPN is enabled");
                                    return;
                                }
                                Intent apkIntent = new Intent("com.android.intent.action.wifi_switchto_mobile_fail");
                                apkIntent.putExtra("subscription", DataSubId);
                                HwDataServiceChrManagerImpl.mContext.sendBroadcast(apkIntent, "com.huawei.android.permission.GET_CHR_DATA");
                                return;
                            }
                        }
                        Log.e(HwDataServiceChrManagerImpl.LOG_TAG, "EVENT_WIFI_DISCONNECT_TIMER_EXPIRE DataSubId invaild");
                        return;
                    case 4:
                        Log.d(HwDataServiceChrManagerImpl.LOG_TAG, "CHR set all type APNReported as false because of apn changed");
                        for (int subId = 0; subId < 3; subId++) {
                            HwDataServiceChrManagerImpl.this.mDefaultAPNReported[subId] = false;
                            HwDataServiceChrManagerImpl.this.mMmsAPNReported[subId] = false;
                            HwDataServiceChrManagerImpl.this.mDunAPNReported[subId] = false;
                        }
                        return;
                    case 5:
                        Log.d(HwDataServiceChrManagerImpl.LOG_TAG, "CHR received the event EVENT_APN_CONNECTION_INFO_INTENT, then send the intent");
                        HwDataServiceChrManagerImpl.mContext.sendBroadcast(msg.obj, "com.huawei.android.permission.GET_CHR_DATA");
                        return;
                    default:
                        return;
                }
            } catch (Exception e) {
                str = HwDataServiceChrManagerImpl.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception: handleMessage fail! ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
    };
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String str;
            StringBuilder stringBuilder;
            try {
                String action = intent.getAction();
                str = HwDataServiceChrManagerImpl.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive: action=");
                stringBuilder.append(action);
                Log.d(str, stringBuilder.toString());
                String simState;
                if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                    int subId = intent.getIntExtra("subscription", 3);
                    simState = intent.getStringExtra("ss");
                    if (simState != null && "READY".equals(simState) && subId == SubscriptionController.getInstance().getDefaultDataSubId()) {
                        String str2 = HwDataServiceChrManagerImpl.LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ACTION_SIM_STATE_CHANGED: simState=");
                        stringBuilder2.append(simState);
                        stringBuilder2.append("  SUBSCRIPTION_KEY = ");
                        stringBuilder2.append(subId);
                        Log.d(str2, stringBuilder2.toString());
                        HwDataServiceChrManagerImpl.this.mDataSubId = subId;
                        if (HwDataServiceChrManagerImpl.this.mSimloadedTimer == null) {
                            HwDataServiceChrManagerImpl.this.mSimloadedTimer = HwDataServiceChrManagerImpl.this.startTimer(HwDataServiceChrManagerImpl.this.mSimloadedTimer, HwDataServiceChrManagerImpl.TIMER_INTERVAL_CHECK_SIMLOADED_ISRECEIVED);
                        }
                    }
                } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                    if (true == HwDataServiceChrManagerImpl.this.mIsWifiConnected && intent.getIntExtra("wifi_state", 4) == 1) {
                        HwDataServiceChrManagerImpl.this.removeMonitorWifiSwitchToMobileMessage();
                        HwDataServiceChrManagerImpl.this.sendMonitorWifiSwitchToMobileMessage(HwDataServiceChrManagerImpl.this.WIFI_TO_MOBILE_MONITOR_TIMER_INTERVAL);
                        HwDataServiceChrManagerImpl.this.mIsWifiConnected = false;
                    }
                    str = HwDataServiceChrManagerImpl.LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mIsWifiConnected = ");
                    stringBuilder.append(HwDataServiceChrManagerImpl.this.mIsWifiConnected);
                    Log.d(str, stringBuilder.toString());
                } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (networkInfo != null) {
                        if (true == HwDataServiceChrManagerImpl.this.mIsWifiConnected && networkInfo.getState() == State.DISCONNECTED) {
                            HwDataServiceChrManagerImpl.this.removeMonitorWifiSwitchToMobileMessage();
                            HwDataServiceChrManagerImpl.this.sendMonitorWifiSwitchToMobileMessage(HwDataServiceChrManagerImpl.this.WIFI_TO_MOBILE_MONITOR_TIMER_INTERVAL);
                        } else if (!HwDataServiceChrManagerImpl.this.mIsWifiConnected && networkInfo.getState() == State.CONNECTED) {
                            HwDataServiceChrManagerImpl.this.removeMonitorWifiSwitchToMobileMessage();
                        }
                        HwDataServiceChrManagerImpl.this.mIsWifiConnected = networkInfo.isConnected();
                    }
                    simState = HwDataServiceChrManagerImpl.LOG_TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("mIsWifiConnected = ");
                    stringBuilder3.append(HwDataServiceChrManagerImpl.this.mIsWifiConnected);
                    Log.d(simState, stringBuilder3.toString());
                }
            } catch (RuntimeException ex) {
                str = HwDataServiceChrManagerImpl.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception: onReceive fail! ");
                stringBuilder.append(ex);
                Log.e(str, stringBuilder.toString());
            } catch (Exception e) {
                str = HwDataServiceChrManagerImpl.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception: onReceive fail! ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
    };
    private boolean mIsBringUp = false;
    private boolean mIsDataConnectionAttached = false;
    private boolean mIsFirstReport = true;
    private boolean mIsReceivedSimloadedMsg = false;
    private boolean mIsRecordsLoadedRegistered = false;
    private boolean mIsWifiConnected = false;
    private int mLastDataCallFailStatus = -1;
    private boolean[] mMmsAPNReported = new boolean[]{false, false, false};
    private String mPdpActiveIpType = null;
    private int mSameStatusTimes = 0;
    private Timer mSimloadedTimer;
    private boolean mUserDataEnabled = false;

    private class ApnChangeObserver extends ContentObserver {
        public ApnChangeObserver() {
            super(HwDataServiceChrManagerImpl.this.mHandler);
        }

        public void onChange(boolean selfChange) {
            HwDataServiceChrManagerImpl.this.mHandler.sendMessage(HwDataServiceChrManagerImpl.this.mHandler.obtainMessage(4));
        }
    }

    public static HwDataServiceChrManager getDefault() {
        return mInstance;
    }

    public void init(Context context) {
        if (context != null) {
            try {
                mContext = context;
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.SIM_STATE_CHANGED");
                filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
                filter.addAction("android.net.wifi.STATE_CHANGE");
                mContext.registerReceiver(this.mIntentReceiver, filter);
                this.mApnObserver = new ApnChangeObserver();
                mContext.getContentResolver().registerContentObserver(Carriers.CONTENT_URI, true, this.mApnObserver);
            } catch (Exception e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception: init fail! ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    public void setPdpActiveIpType(String pdpActiveIpType, int subId) {
        this.mPdpActiveIpType = pdpActiveIpType;
        sendIntentWithPdpIpType(subId);
    }

    public String getPdpActiveIpType() {
        return this.mPdpActiveIpType;
    }

    public void setDataNotAllowedReason(Phone phone, boolean attachedState, boolean autoAttachOnCreation, boolean recordsLoaded, boolean internalDataEnabled, boolean userDataEnabled, boolean isPsRestricted) {
        String reason = "";
        if (phone != null) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(attachedState);
                stringBuilder.append(",");
                stringBuilder.append(autoAttachOnCreation);
                stringBuilder.append(",");
                stringBuilder.append(recordsLoaded);
                stringBuilder.append(",");
                stringBuilder.append(phone.getState());
                stringBuilder.append(",");
                stringBuilder.append(phone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed());
                stringBuilder.append(",");
                stringBuilder.append(internalDataEnabled);
                stringBuilder.append(",");
                stringBuilder.append(phone.getServiceState().getRoaming());
                stringBuilder.append(",");
                stringBuilder.append(phone.mDcTracker.getDataRoamingEnabled());
                stringBuilder.append(",");
                stringBuilder.append(userDataEnabled);
                stringBuilder.append(",");
                stringBuilder.append(isPsRestricted);
                stringBuilder.append(",");
                stringBuilder.append(phone.getServiceStateTracker().getDesiredPowerState());
                reason = stringBuilder.toString();
            } catch (Exception e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exception: setDataNotAllowedReason fail! ");
                stringBuilder2.append(e);
                Log.e(str, stringBuilder2.toString());
                return;
            }
        }
        Exception e2 = LOG_TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("DcTrackerBse setDataNotAllowedReason,reason=");
        stringBuilder3.append(reason);
        Log.d(e2, stringBuilder3.toString());
        this.mDataNotAllowedReason = reason;
    }

    public void setDataNotAllowedReasonToNull() {
        this.mDataNotAllowedReason = "";
    }

    public String getDataNotAllowedReason() {
        return this.mDataNotAllowedReason;
    }

    public void setAnyDataEnabledFalseReason(boolean internalDataEnabled, boolean userDataEnabled, boolean sPolicyDataEnabled, boolean checkUserDataEnabled) {
        String reason = "";
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(internalDataEnabled);
            stringBuilder.append(",");
            stringBuilder.append(userDataEnabled);
            stringBuilder.append(",");
            stringBuilder.append(sPolicyDataEnabled);
            stringBuilder.append(",");
            stringBuilder.append(checkUserDataEnabled);
            reason = stringBuilder.toString();
            String str = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DcTrackerBse setAnyDataEnabledFalseReason,flag=");
            stringBuilder2.append(reason);
            Log.d(str, stringBuilder2.toString());
            this.mGetAnyDataEnabledFalseReason = reason;
        } catch (Exception e) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Exception: setAnyDataEnabledFalseReason fail! ");
            stringBuilder3.append(e);
            Log.e(str2, stringBuilder3.toString());
        }
    }

    public void setAnyDataEnabledFalseReasonToNull() {
        this.mGetAnyDataEnabledFalseReason = "";
    }

    public String getAnyDataEnabledFalseReason() {
        return this.mGetAnyDataEnabledFalseReason;
    }

    public void setBringUp(boolean isBringUp) {
        this.mIsBringUp = isBringUp;
    }

    public boolean getBringUp() {
        return this.mIsBringUp;
    }

    public void setReceivedSimloadedMsg(Phone phone, boolean isReceivedSimloadedMsg, ConcurrentHashMap<String, ApnContext> apnContexts, boolean userDataEnabled) {
        if (phone == null || apnContexts == null) {
            Log.e(LOG_TAG, "setReceivedSimloadedMsg Phone is null ");
            return;
        }
        int sub = phone.getSubId();
        if (sub >= 3 || sub < 0) {
            Log.e(LOG_TAG, "setReceivedSimloadedMsg sub is invaild ");
            return;
        }
        this.mActivePhone[sub] = phone;
        this.mIsReceivedSimloadedMsg = isReceivedSimloadedMsg;
        this.mDefaultApnContext[sub] = (ApnContext) apnContexts.get("default");
        this.mUserDataEnabled = userDataEnabled;
    }

    public boolean getReceivedSimloadedMsg() {
        return this.mIsReceivedSimloadedMsg;
    }

    public void setRecordsLoadedRegistered(boolean isRecordsLoadedRegistered, int subId) {
        if (subId == this.mDataSubId) {
            this.mIsRecordsLoadedRegistered = isRecordsLoadedRegistered;
        }
    }

    public boolean getRecordsLoadedRegistered() {
        return this.mIsRecordsLoadedRegistered;
    }

    public void setCheckApnContextState(boolean checkApnContextState) {
        this.mCheckApnContextState = checkApnContextState;
    }

    public void getModemParamsWhenCdmaPdpActFail(Phone phone, int rilFailCause) {
        if (phone == null) {
            Log.e(LOG_TAG, "getModemParamsWhenCdmaPdpActFail Phone is null ");
            return;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            if (phone.getPhoneType() != 1) {
                this.mCdmaPhone = (GsmCdmaPhone) phone;
                String isSupportCdmaChr = SystemProperties.get("ro.sys.support_cdma_chr", "false");
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_GET_LAST_FAIL_DONE isSupportCdmaChr = ");
                stringBuilder.append(isSupportCdmaChr);
                Log.d(str, stringBuilder.toString());
                if ("true".equals(isSupportCdmaChr)) {
                    Log.d(LOG_TAG, "CDMAPhone pdp active fail");
                    this.mChrCdmaPdpRilFailCause = rilFailCause - 131072;
                }
            }
        } catch (Exception e) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: getModemParamsWhenCdmaPdpActFail fail! ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }

    public void sendIntentWhenDorecovery(Phone phone, int recoveryAction) {
        if (phone == null) {
            Log.e(LOG_TAG, "sendIntentWhenDorecovery Phone is null ");
            return;
        }
        try {
            if (phone.getServiceState().getDataRegState() == 0 && recoveryAction > 0) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendIntentWhenDorecovery recoveryAction = ");
                stringBuilder.append(recoveryAction);
                Log.d(str, stringBuilder.toString());
                Context context = phone.getContext();
                int subId = phone.getSubId();
                Intent intent = new Intent("com.android.intent.action.do_recovery");
                intent.putExtra("subscription", subId);
                intent.putExtra("recoveryAction", recoveryAction);
                context.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
            }
        } catch (Exception e) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exception: sendIntentWhenDorecovery fail! ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
        }
    }

    public void sendIntentDSUseStatistics(Phone phone, int duration) {
        try {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendIntentDSUseStatistics duration = ");
            stringBuilder.append(duration);
            Log.d(str, stringBuilder.toString());
            Context context = phone.getContext();
            int subId = phone.getSubId();
            Intent intent = new Intent("com.android.intent.action.use_statistics");
            intent.putExtra("subscription", subId);
            intent.putExtra("duration", duration);
            context.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
        } catch (Exception e) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exception: sendIntentDSUseStatistics fail! ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
        }
    }

    public void sendIntentWhenSetDataSubFail(int subId) {
        if (mContext != null) {
            Intent intent = new Intent("com.android.intent.action.set_data_sub_fail");
            intent.putExtra("subscription", subId);
            mContext.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
        }
    }

    public void sendIntentApnContextDisabledWhenWifiDisconnected(Phone phone, boolean isWifiConnected, boolean userDataEnabled, ApnContext apnContext) {
        if (phone == null || apnContext == null) {
            Log.e(LOG_TAG, "sendIntentApnContextDisabledWhenWifiDisconnected Phone is null ");
            return;
        }
        if (this.mCheckApnContextState) {
            try {
                if (apnContext.getApnType().equals("default") && !isWifiConnected && userDataEnabled && getReceivedSimloadedMsg() && !apnContext.isEnabled() && !ignoreReport(phone) && phone.getSubId() == SubscriptionController.getInstance().getDefaultDataSubId()) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ready to trigger chr: apnContext = ");
                    stringBuilder.append(apnContext.toString());
                    Log.d(str, stringBuilder.toString());
                    Intent intent = new Intent("com.intent.action.apn_disable_while_wifi_disconnect");
                    intent.putExtra("subscription", phone.getSubId());
                    mContext.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
                }
            } catch (Exception e) {
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exception: sendIntentApnContextDisabledWhenWifiDisconnected fail! ");
                stringBuilder2.append(e);
                Log.e(str2, stringBuilder2.toString());
            }
        }
    }

    private boolean isLabCard() {
        try {
            String hplmn = TelephonyManager.getDefault().getSimOperator();
            for (String tmp : new String[]{"46060", "00101"}) {
                if (tmp.equals(hplmn)) {
                    Log.d(LOG_TAG, "Lab card, Ignore report CHR event!");
                    return true;
                }
            }
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: isLabCard fail! ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
        return false;
    }

    private boolean isCardReady(Phone phone) {
        int simstate = 5;
        boolean z = false;
        if (phone == null) {
            Log.e(LOG_TAG, "isCardReady Phone is null ");
            return false;
        }
        try {
            SubscriptionController subscriptionController = SubscriptionController.getInstance();
            if (phone.getSubId() == SubscriptionManager.getDefaultSubscriptionId()) {
                if (subscriptionController.getSubState(phone.getSubId()) != 0) {
                    simstate = getSimCardState(phone.getSubId());
                    if (5 == simstate) {
                        z = true;
                    }
                    return z;
                }
            }
            Log.d(LOG_TAG, "isCardReady return false,subid != getDataSubscription || INACTIVE == getSubState");
            return false;
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: isCardReady fail! ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }

    private boolean ignoreReport(Phone phone) {
        try {
            if (!isCardReady(phone)) {
                Log.d(LOG_TAG, "Card not Ready! Ignore report CHR event!");
                return true;
            } else if (isLabCard()) {
                Log.d(LOG_TAG, "isLabCard! Ignore report CHR event!");
                return true;
            } else {
                if (phone.getServiceState().getRilDataRadioTechnology() != 0) {
                    if (phone.getServiceState().getDataRegState() == 0) {
                        return false;
                    }
                }
                Log.d(LOG_TAG, "ignoreReport: ps domain is not attached, skipped");
                return true;
            }
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: ignoreReport fail! ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }

    private void cleanLastStatus() {
        this.mLastDataCallFailStatus = 0;
        this.mSameStatusTimes = 0;
    }

    private boolean isNeedReportChrForSameFailStatus(int[] params) {
        if (params != null) {
            String str;
            try {
                if (3 == params[0] || 8 == params[0] || 9 == params[0] || 34 == params[0] || 35 == params[0] || 36 == params[0] || 37 == params[0]) {
                    if (this.mLastDataCallFailStatus == params[0]) {
                        this.mSameStatusTimes++;
                        str = LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mSameStatusTimes : ");
                        stringBuilder.append(this.mSameStatusTimes);
                        stringBuilder.append(" if mSameStatusTimes>3.return true,report chr. else .return false,dont report chr.");
                        Log.d(str, stringBuilder.toString());
                        if (this.mSameStatusTimes <= 3) {
                            return false;
                        }
                        cleanLastStatus();
                        stopTimer(this.mCdmaPdpSameStatusTimer);
                        this.mCdmaPdpSameStatusTimer = null;
                        return true;
                    }
                    cleanLastStatus();
                    stopTimer(this.mCdmaPdpSameStatusTimer);
                    this.mCdmaPdpSameStatusTimer = startTimer(this.mCdmaPdpSameStatusTimer, 60000);
                    this.mSameStatusTimes++;
                    this.mLastDataCallFailStatus = params[0];
                    Log.d(LOG_TAG, "new fail status!return false,dont report chr.");
                    return false;
                }
            } catch (Exception e) {
                str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exception: isNeedReportChrForSameFailStatus !!");
                stringBuilder2.append(e);
                Log.e(str, stringBuilder2.toString());
            }
        }
        return true;
    }

    private void sendIntentWithPdpIpType(int subId) {
        if (mContext != null) {
            Intent intent = new Intent("com.android.intent.action.pdp_act_ip_type");
            intent.putExtra("subscription", subId);
            intent.putExtra("pdpActIpType", getPdpActiveIpType());
            mContext.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
        }
    }

    public void SendIntentDNSfailure(String[] dnses) {
        if ((dnses == null || dnses.length == 0) && mContext != null) {
            Log.d(LOG_TAG, " send DNSfailureIntent,check dnses is null");
            mContext.sendBroadcast(new Intent("com.intent.action.dns_fail"), "com.huawei.android.permission.GET_CHR_DATA");
        }
    }

    private boolean isUserModifyApnSetting() {
        String[] selections = new String[]{"visible<>1", "visible is null"};
        Uri uri = Carriers.CONTENT_URI;
        int length = selections.length;
        int i = 0;
        while (i < length) {
            Cursor cursor = mContext.getContentResolver().query(uri, null, selections[i], null, null);
            if (cursor == null || cursor.getCount() <= 0) {
                if (cursor != null) {
                    cursor.close();
                }
                i++;
            } else {
                Log.d(LOG_TAG, "User ever add new APN!");
                cursor.close();
                return true;
            }
        }
        Log.d(LOG_TAG, "User never add APN setting!");
        return false;
    }

    public void sendIntentApnListEmpty(int subId) {
        if (mContext != null) {
            Log.d(LOG_TAG, " send ApnListEmpty");
            Intent intent = new Intent("com.intent.action.apn_list_empty");
            intent.putExtra("subscription", subId);
            intent.putExtra("userModifyApnList", isUserModifyApnSetting());
            mContext.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
        }
    }

    public void sendIntentDataConnectionSetupResult(int subId, String state, String reason, String apn, String apnType, LinkProperties linkProperties) {
        Log.d(LOG_TAG, " send sendIntentDataConnectionSetupResult");
        Intent intent = new Intent("com.intent.action.data_connection_setup_result");
        intent.putExtra("subscription", subId);
        if (state != null) {
            intent.putExtra("state", state);
        }
        if (reason != null) {
            intent.putExtra("reason", reason);
        }
        if (apn != null) {
            intent.putExtra("apn", apn);
        }
        if (apnType != null) {
            intent.putExtra(HwVSimConstants.ENABLE_PARA_APNTYPE, apnType);
        }
        if (linkProperties != null) {
            intent.putExtra("linkProperties", linkProperties);
            String iface = linkProperties.getInterfaceName();
            if (iface != null) {
                intent.putExtra("iface", iface);
            }
        }
        if (mContext != null) {
            mContext.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
        }
    }

    private void sendIntentWhenCdmaPdpActFail(AsyncResult cdmaChrAR) {
        if (cdmaChrAR == null) {
            Log.e(LOG_TAG, "sendIntentWhenCdmaPdpActFail cdmaChrAR is null ");
            return;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            int[] params = cdmaChrAR.result;
            if (isNeedReportChrForSameFailStatus(params)) {
                if (params != null) {
                    Intent intent;
                    str = LOG_TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("EVENT_GET_CDMA_CHR_INFO,params.length:");
                    stringBuilder.append(params.length);
                    Log.d(str, stringBuilder.toString());
                    str = getPdpActiveIpType();
                    if (this.mCdmaPhone.getServiceState().getDataRegState() == 0) {
                        intent = new Intent("com.android.intent.action.cdma_pdp_act_fail");
                        Log.d(LOG_TAG, "cdma pdp actived fail.");
                    } else {
                        intent = new Intent("com.android.intent.action.cdma_pdp_act_fail_fall_2g");
                        Log.d(LOG_TAG, "cdma pdp actived fail,because of fall to 2G.data register state is STATE_OUT_OF_SERVICE.");
                    }
                    intent.putExtra("subscription", this.mCdmaPhone.getSubId());
                    intent.putExtra("cdmaRilFailCause", this.mChrCdmaPdpRilFailCause);
                    intent.putExtra("pdpActIpType", str);
                    intent.putExtra("cdmaParams", params);
                    mContext.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
                } else {
                    Log.d(LOG_TAG, "EVENT_GET_CDMA_CHR_INFO,params is null");
                }
            }
        } catch (Exception e) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: sendIntentWhenCdmaPdpActFail fail! ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }

    private void sendIntentWhenSimloadedMsgIsNotReceived(int subId) {
        if (mContext != null) {
            Intent intent = new Intent("com.android.intent.action.simloaded_msg_not_received");
            intent.putExtra("subscription", subId);
            mContext.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
        }
    }

    private void sendIntentWhenPdpActFailBlockAtFw(int subId) {
        if (mContext != null) {
            Intent intent = new Intent("com.android.intent.action.pdp_fail_block_at_fw");
            intent.putExtra("subscription", subId);
            intent.putExtra("AnyDataEnabledFlag", getAnyDataEnabledFalseReason());
            intent.putExtra("DataNotAllowedReason", getDataNotAllowedReason());
            mContext.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
        }
    }

    private Timer startTimer(Timer timer, int timerInterval) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startTimer!getSubId =");
        stringBuilder.append(this.mDataSubId);
        stringBuilder.append(",SubscriptionManager.getDefaultSubscriptionId() =");
        stringBuilder.append(SubscriptionManager.getDefaultSubscriptionId());
        Log.d(str, stringBuilder.toString());
        try {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    Log.d(HwDataServiceChrManagerImpl.LOG_TAG, "TimerTask run enter");
                    HwDataServiceChrManagerImpl.this.mHandler.sendMessage(HwDataServiceChrManagerImpl.this.mHandler.obtainMessage(1));
                }
            }, (long) timerInterval);
            return timer;
        } catch (Exception e) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exception: startTimer fail! ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            return null;
        }
    }

    private void stopTimer(Timer timer) {
        if (timer != null) {
            try {
                Log.d(LOG_TAG, "mTimer!=null");
                timer.cancel();
            } catch (Exception e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception: stopTimer fail! ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    private int getSimCardState(int subId) {
        return TelephonyManager.getDefault().getSimState(subId);
    }

    public void sendIntentWhenDataConnected(Phone phone, ApnSetting apn, LinkProperties linkProperties) {
        if (phone == null || apn == null || linkProperties == null) {
            Log.e(LOG_TAG, "sendIntentWhenDataConnected paras is null ");
            return;
        }
        int subId = phone.getSubId();
        String str;
        StringBuilder stringBuilder;
        if (subId < 0 || subId >= 3) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("invalid subId: ");
            stringBuilder.append(subId);
            Log.e(str, stringBuilder.toString());
            return;
        }
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("CHR APN setttings = ");
        stringBuilder.append(apn);
        Log.d(str, stringBuilder.toString());
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("CHR Report flag: subId = ");
        stringBuilder.append(subId);
        stringBuilder.append(", default flag = ");
        stringBuilder.append(this.mDefaultAPNReported[subId]);
        stringBuilder.append(", mms flag = ");
        stringBuilder.append(this.mMmsAPNReported[subId]);
        stringBuilder.append(", dun flag = ");
        stringBuilder.append(this.mDunAPNReported[subId]);
        Log.d(str, stringBuilder.toString());
        if (isNeedToReport(apn, subId)) {
            int hasUserPassword = 0;
            if (!(apn.user == null || apn.user.length() == 0)) {
                hasUserPassword = 1;
            }
            int hasDns = 0;
            if (linkProperties.getDnsServers().size() != 0) {
                hasDns = 1;
            }
            int chrRilRat = phone.getServiceState().getRilDataRadioTechnology();
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CHR Report flag after processing the types : default flag = ");
            stringBuilder2.append(this.mDefaultAPNReported[subId]);
            stringBuilder2.append(" , mms flag = ");
            stringBuilder2.append(this.mMmsAPNReported[subId]);
            stringBuilder2.append(", dun flag = ");
            stringBuilder2.append(this.mDunAPNReported[subId]);
            Log.d(str2, stringBuilder2.toString());
            str2 = LOG_TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CHR chrRilRat = ");
            stringBuilder2.append(chrRilRat);
            Log.d(str2, stringBuilder2.toString());
            str2 = LOG_TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CHR send apn info intent mIsFirstReport = ");
            stringBuilder2.append(this.mIsFirstReport);
            Log.d(str2, stringBuilder2.toString());
            Intent intent = new Intent("com.intent.action.APN_CONNECTION_INFO");
            intent.putExtra("subscription", phone.getSubId());
            intent.putExtra("rilRat", chrRilRat);
            intent.putExtra("apnUserPassword", hasUserPassword);
            intent.putExtra("linkDns", hasDns);
            intent.putExtra("apnSetting", apn.toString());
            if (this.mIsFirstReport) {
                sendBroadcastDelayed(intent, TIMER_INTERVAL_SEND_APN_INFO_INTENT);
                this.mIsFirstReport = false;
            } else {
                mContext.sendBroadcast(intent, "com.huawei.android.permission.GET_CHR_DATA");
            }
        } else {
            Log.d(LOG_TAG, "CHR do not need to report , ignore it ");
        }
    }

    private boolean isNeedToReport(ApnSetting apn, int subId) {
        boolean needToReport = false;
        for (String t : apn.types) {
            if (t.equalsIgnoreCase("default")) {
                if (!this.mDefaultAPNReported[subId]) {
                    needToReport = true;
                    this.mDefaultAPNReported[subId] = true;
                }
            } else if (t.equalsIgnoreCase("mms")) {
                if (!this.mMmsAPNReported[subId]) {
                    needToReport = true;
                    this.mMmsAPNReported[subId] = true;
                }
            } else if (t.equalsIgnoreCase("dun")) {
                if (!this.mDunAPNReported[subId]) {
                    needToReport = true;
                    this.mDunAPNReported[subId] = true;
                }
            } else if (t.equalsIgnoreCase("*") && !(this.mDefaultAPNReported[subId] && this.mMmsAPNReported[subId] && this.mDunAPNReported[subId])) {
                needToReport = true;
                this.mDefaultAPNReported[subId] = true;
                this.mMmsAPNReported[subId] = true;
                this.mDunAPNReported[subId] = true;
            }
        }
        return needToReport;
    }

    private void sendBroadcastDelayed(Intent intent, long delayedTimer) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CHR sendBroadcastDelayed delayedTimer = ");
        stringBuilder.append(delayedTimer);
        Log.d(str, stringBuilder.toString());
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, intent), delayedTimer);
    }

    public void sendMonitorWifiSwitchToMobileMessage(int delayInterval) {
        Log.d(LOG_TAG, "wifi disconnect, sendMonitorWifiSwitchToMobileMessage!");
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3), (long) delayInterval);
    }

    public void removeMonitorWifiSwitchToMobileMessage() {
        this.mHandler.removeMessages(3);
    }

    private boolean isWifiConnected() {
        if (mContext != null) {
            ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService("connectivity");
            if (connManager != null) {
                NetworkInfo wifiInfo = connManager.getNetworkInfo(1);
                if (wifiInfo != null) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mWifiConnected = ");
                    stringBuilder.append(wifiInfo.isConnected());
                    Log.d(str, stringBuilder.toString());
                    return wifiInfo.isConnected();
                }
            }
        }
        Log.d(LOG_TAG, "Get WifiConnected Info failed!");
        return false;
    }
}
