package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.internal.telephony.fullnetwork.HwFullNetworkManager;
import com.android.internal.telephony.vsim.HwVSimConstants;
import java.util.Map;

public class HwDualCardSwitcher extends Handler {
    private static final int EVENT_AUTO_SWITCH_DONE = 2;
    private static final int EVENT_AUTO_SWITCH_TIMEOUT = 3;
    private static final int EVENT_PROMPT_TO_SWITCH = 1;
    private static final String EXTRA_DATA_SWITCH = "data_switch";
    private static final int MAX_TIME_WAIT_AUTO_SWITCH = 30000;
    private static final int MAX_TIME_WAIT_TO_PROMPT = 10000;
    private static final int MCCMNC_LEN_MINIMUM = 5;
    private static final String MCC_OF_CN = "460";
    private static final int SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static final String TAG = "HwDualCardSwitcher";
    private static HwDualCardSwitcher mInstance;
    private static final Object mLock = new Object();
    private int mAutoSwitchingCount = 0;
    private Context mContext;
    private int mPrimarySlot = 0;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                HwDualCardSwitcher.this.loge("intent is null, return.");
                return;
            }
            if ("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(intent.getAction())) {
                HwDualCardSwitcher.this.processSubInfoRecordUpdated(intent);
            }
        }
    };
    private int mSecondarySlot = 1;
    private ServiceState[] mServiceState = new ServiceState[SIM_NUM];
    private boolean mShouldAutoSwitch = false;
    private Map<Integer, ServiceStateListener> sListeners = new ArrayMap();

    private class ServiceStateListener extends PhoneStateListener {
        public ServiceStateListener(int subId) {
            super(Integer.valueOf(subId));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ServiceStateListener create subId:");
            stringBuilder.append(subId);
            HwDualCardSwitcher.this.logd(stringBuilder.toString());
        }

        public void listen(Context context) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
            if (tm == null) {
                HwDualCardSwitcher.this.loge("Cannot create TelephonyManager");
            } else {
                tm.listen(this, 1);
            }
        }

        public void onServiceStateChanged(ServiceState state) {
            if (state != null) {
                HwDualCardSwitcher.this.mServiceState[this.mSubId.intValue()] = state;
                HwDualCardSwitcher.this.checkIfNeedToSwitchSlot();
            }
        }
    }

    private HwDualCardSwitcher(Context context) {
        this.mContext = context;
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED"));
        registerListeners();
    }

    public static HwDualCardSwitcher make(Context context) {
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new HwDualCardSwitcher(context);
            } else {
                throw new RuntimeException("HwDualCardSwitcher.make() should only be called once");
            }
        }
        return mInstance;
    }

    private void processSubInfoRecordUpdated(Intent intent) {
        int status = intent.getIntExtra("simDetectStatus", -1);
        if (status != -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sim state ");
            stringBuilder.append(status);
            logd(stringBuilder.toString());
        }
        if (status == 1 || status == 3) {
            logd("sim card changed, check if need to auto switch.");
            if (hasMessages(3)) {
                removeMessages(3);
            }
            sendMessageDelayed(obtainMessage(3), HwVSimConstants.WAIT_FOR_NV_CFG_MATCH_TIMEOUT);
            this.mShouldAutoSwitch = true;
            checkIfNeedToSwitchSlot();
        }
    }

    private void registerListeners() {
        for (int sub = 0; sub < SIM_NUM; sub++) {
            ServiceStateListener listener = new ServiceStateListener(sub);
            listener.listen(this.mContext);
            this.sListeners.put(Integer.valueOf(sub), listener);
        }
    }

    public void checkIfNeedToSwitchSlot() {
        if (!shouldSwitch() || HwFullNetworkManager.getInstance().get4GSlotInProgress() || HwFullNetworkManager.getInstance().isRestartRildProgress()) {
            if (hasMessages(1)) {
                removeMessages(1);
            }
        } else if (this.mShouldAutoSwitch) {
            HwFullNetworkManager.getInstance().setMainSlot(this.mSecondarySlot, obtainMessage(2, this.mSecondarySlot, 0));
            this.mAutoSwitchingCount++;
        } else if (hasMessages(1)) {
            if (this.mServiceState[this.mPrimarySlot].getState() == 0) {
                removeMessages(1);
                notifyDualCardsManager();
            }
        } else if (this.mServiceState[this.mPrimarySlot].getState() == 0) {
            notifyDualCardsManager();
        } else {
            sendMessageDelayed(obtainMessage(1), HwVSimConstants.VSIM_DISABLE_RETRY_TIMEOUT);
        }
    }

    private boolean shouldSwitch() {
        boolean otherIsInService = false;
        boolean otherIsRoaming = false;
        String mccNoService = null;
        for (int sub = 0; sub < SIM_NUM; sub++) {
            if (this.mServiceState[sub] == null) {
                return false;
            }
        }
        this.mPrimarySlot = HwFullNetworkManager.getInstance().getUserSwitchDualCardSlots();
        this.mSecondarySlot = this.mPrimarySlot == 0 ? 1 : 0;
        String regPlmn = null;
        boolean primaryIsRoaming = false;
        boolean primaryIsInService = false;
        for (int sub2 = 0; sub2 < SIM_NUM; sub2++) {
            if (this.mPrimarySlot == sub2) {
                if (this.mServiceState[sub2].getState() == 0) {
                    primaryIsInService = true;
                    primaryIsRoaming = this.mServiceState[sub2].getRoaming();
                }
            } else if (this.mServiceState[sub2].getState() == 0) {
                otherIsInService = true;
                otherIsRoaming = this.mServiceState[sub2].getRoaming();
                regPlmn = this.mServiceState[sub2].getOperatorNumeric();
            }
        }
        if (otherIsInService && !otherIsRoaming) {
            Phone[] phones;
            HwTelephonyManagerInner hwTelephonyManagerInner = HwTelephonyManagerInner.getDefault();
            if (!(hwTelephonyManagerInner == null || !hwTelephonyManagerInner.isCTSimCard(this.mSecondarySlot) || TextUtils.isEmpty(regPlmn) || regPlmn.startsWith("460"))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("CT card not roaming, register ");
                stringBuilder.append(regPlmn);
                logd(stringBuilder.toString());
                phones = PhoneFactory.getPhones();
                if (!(phones == null || phones[this.mPrimarySlot] == null)) {
                    String imsi = phones[this.mPrimarySlot].getSubscriberId();
                    if (TextUtils.isEmpty(imsi) || imsi.length() < 5) {
                        loge("Invalid imsi of primary card.");
                        return false;
                    } else if (imsi.startsWith("460")) {
                        return false;
                    }
                }
            }
            if (primaryIsInService && primaryIsRoaming) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("primary card is roaming, should switch to slot ");
                stringBuilder2.append(this.mSecondarySlot);
                logd(stringBuilder2.toString());
                return true;
            } else if (primaryIsInService || (hwTelephonyManagerInner != null && hwTelephonyManagerInner.isCTSimCard(this.mPrimarySlot) && !TextUtils.isEmpty(regPlmn) && regPlmn.startsWith("460"))) {
                return false;
            } else {
                phones = PhoneFactory.getPhones();
                if (!(phones == null || phones[this.mPrimarySlot] == null)) {
                    String imsi2 = phones[this.mPrimarySlot].getSubscriberId();
                    if (TextUtils.isEmpty(imsi2) || imsi2.length() < 5) {
                        return false;
                    }
                    mccNoService = imsi2.substring(0, 3);
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("secondary card register ");
                stringBuilder3.append(regPlmn);
                stringBuilder3.append(", primary card's mcc is ");
                stringBuilder3.append(mccNoService);
                logd(stringBuilder3.toString());
                if (!TextUtils.isEmpty(regPlmn) && regPlmn.length() >= 5) {
                    boolean isRoaming = regPlmn.substring(0, 3).equals(mccNoService) ^ true;
                    if (isRoaming) {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("primary card is no service, should switch to slot ");
                        stringBuilder4.append(this.mSecondarySlot);
                        logd(stringBuilder4.toString());
                    }
                    return isRoaming;
                }
            }
        }
        return false;
    }

    public void handleMessage(Message msg) {
        if (msg != null) {
            switch (msg.what) {
                case 1:
                    logd("Receive EVENT_PROMPT_TO_SWITCH");
                    handlePromptToSwitch();
                    break;
                case 2:
                    logd("Receive EVENT_AUTO_SWITCH_DONE");
                    handleAutoSwitchDone(msg);
                    break;
                case 3:
                    logd("Receive EVENT_AUTO_SWITCH_TIMEOUT");
                    cancelAutoSwitch(msg);
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("default message ");
                    stringBuilder.append(msg.what);
                    logd(stringBuilder.toString());
                    break;
            }
        }
    }

    private void notifyDualCardsManager() {
        Intent intent = new Intent("com.huawei.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        intent.putExtra(EXTRA_DATA_SWITCH, this.mSecondarySlot);
        this.mContext.sendBroadcast(intent, "android.permission.MODIFY_PHONE_STATE");
        logd("send ACTION_SUBINFO_RECORD_UPDATED");
    }

    private void handlePromptToSwitch() {
        if (shouldSwitch()) {
            notifyDualCardsManager();
        } else {
            logd("no need send ACTION_SUBINFO_RECORD_UPDATED");
        }
    }

    private void handleAutoSwitchDone(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar == null || ar.exception != null) {
            loge("auto switch data slot failed.");
            if (!this.mShouldAutoSwitch) {
                notifyDualCardsManager();
            }
        } else {
            logd("auto switch data slot success.");
            this.mShouldAutoSwitch = false;
            if (hasMessages(3)) {
                removeMessages(3);
            }
            if (HwSubscriptionManager.getInstance() != null) {
                HwSubscriptionManager.getInstance().setUserPrefDataSlotId(msg.arg1);
            } else {
                loge("HwSubscriptionManager is null!!");
            }
        }
        this.mAutoSwitchingCount--;
    }

    private void cancelAutoSwitch(Message msg) {
        if (this.mShouldAutoSwitch) {
            loge("cancel auto switch, try to prompt.");
            this.mShouldAutoSwitch = false;
            if (this.mAutoSwitchingCount == 0) {
                notifyDualCardsManager();
            }
        }
    }

    private void logd(String message) {
        Rlog.d(TAG, message);
    }

    private void loge(String message) {
        Rlog.e(TAG, message);
    }
}
