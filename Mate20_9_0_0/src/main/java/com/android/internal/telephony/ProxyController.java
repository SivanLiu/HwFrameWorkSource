package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.hsm.HwSystemManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemProperties;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.uicc.UiccController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyController {
    private static final int EVENT_APPLY_RC_RESPONSE = 3;
    private static final int EVENT_FINISH_RC_RESPONSE = 4;
    private static final int EVENT_NOTIFICATION_RC_CHANGED = 1;
    private static final int EVENT_START_RC_RESPONSE = 2;
    private static final int EVENT_TIMEOUT = 5;
    private static final int INVALID = -1;
    public static final boolean IS_FAST_SWITCH_SIMSLOT = SystemProperties.getBoolean("ro.config.fast_switch_simslot", false);
    public static final boolean IS_QCRIL_CROSS_MAPPING = SystemProperties.getBoolean("ro.hwpp.qcril_cross_mapping", false);
    static final String LOG_TAG = "ProxyController";
    public static final String MODEM_0 = "0";
    public static final String MODEM_1 = "1";
    public static final String MODEM_2 = "2";
    private static final int SET_RC_STATUS_APPLYING = 3;
    private static final int SET_RC_STATUS_FAIL = 5;
    private static final int SET_RC_STATUS_IDLE = 0;
    private static final int SET_RC_STATUS_STARTED = 2;
    private static final int SET_RC_STATUS_STARTING = 1;
    private static final int SET_RC_STATUS_SUCCESS = 4;
    private static final int SET_RC_TIMEOUT_WAITING_MSEC = 45000;
    private static ProxyController sProxyController;
    private CommandsInterface[] mCi;
    private Context mContext;
    private String[] mCurrentLogicalModemIds;
    private int mExpectedMainSlotId = -1;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            ProxyController proxyController = ProxyController.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage msg.what=");
            stringBuilder.append(msg.what);
            proxyController.logd(stringBuilder.toString());
            switch (msg.what) {
                case 1:
                    ProxyController.this.onNotificationRadioCapabilityChanged(msg);
                    return;
                case 2:
                    ProxyController.this.onStartRadioCapabilityResponse(msg);
                    return;
                case 3:
                    ProxyController.this.onApplyRadioCapabilityResponse(msg);
                    return;
                case 4:
                    ProxyController.this.onFinishRadioCapabilityResponse(msg);
                    return;
                case 5:
                    ProxyController.this.onTimeoutRadioCapability(msg);
                    return;
                default:
                    return;
            }
        }
    };
    private String[] mNewLogicalModemIds;
    private int[] mNewRadioAccessFamily;
    private int[] mOldRadioAccessFamily;
    private PhoneSubInfoController mPhoneSubInfoController;
    private PhoneSwitcher mPhoneSwitcher;
    private Phone[] mPhones;
    private int mRadioAccessFamilyStatusCounter;
    private int mRadioCapabilitySessionId;
    private RadioCapability[] mRadioCapabilitys;
    private int[] mSetRadioAccessFamilyStatus;
    private boolean mTransactionFailed = false;
    private UiccController mUiccController;
    private UiccPhoneBookController mUiccPhoneBookController;
    private UiccSmsController mUiccSmsController;
    private AtomicInteger mUniqueIdGenerator = new AtomicInteger(new Random().nextInt());
    WakeLock mWakeLock;

    public static ProxyController getInstance(Context context, Phone[] phone, UiccController uiccController, CommandsInterface[] ci, PhoneSwitcher ps) {
        if (sProxyController == null) {
            sProxyController = new ProxyController(context, phone, uiccController, ci, ps);
        }
        return sProxyController;
    }

    public static ProxyController getInstance() {
        return sProxyController;
    }

    private ProxyController(Context context, Phone[] phone, UiccController uiccController, CommandsInterface[] ci, PhoneSwitcher phoneSwitcher) {
        int i = 0;
        logd("Constructor - Enter");
        this.mContext = context;
        this.mPhones = phone;
        this.mUiccController = uiccController;
        this.mCi = ci;
        this.mPhoneSwitcher = phoneSwitcher;
        this.mUiccPhoneBookController = HwTelephonyFactory.getHwUiccManager().createHwUiccPhoneBookController(this.mPhones);
        if (HwSystemManager.mPermissionEnabled == 0) {
            this.mPhoneSubInfoController = new PhoneSubInfoController(this.mContext, this.mPhones);
        } else {
            this.mPhoneSubInfoController = HwTelephonyFactory.getHwSubInfoController(this.mContext, this.mPhones);
        }
        this.mUiccSmsController = HwTelephonyFactory.getHwUiccManager().createHwUiccSmsController(this.mPhones);
        this.mSetRadioAccessFamilyStatus = new int[this.mPhones.length];
        this.mNewRadioAccessFamily = new int[this.mPhones.length];
        this.mOldRadioAccessFamily = new int[this.mPhones.length];
        this.mCurrentLogicalModemIds = new String[this.mPhones.length];
        this.mNewLogicalModemIds = new String[this.mPhones.length];
        this.mRadioCapabilitys = new RadioCapability[this.mPhones.length];
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, LOG_TAG);
        this.mWakeLock.setReferenceCounted(false);
        clearTransaction();
        while (i < this.mPhones.length) {
            this.mPhones[i].registerForRadioCapabilityChanged(this.mHandler, 1, null);
            i++;
        }
        logd("Constructor - Exit");
    }

    public void updateDataConnectionTracker(int sub) {
        this.mPhones[sub].updateDataConnectionTracker();
    }

    public void enableDataConnectivity(int sub) {
        this.mPhones[sub].setInternalDataEnabled(true, null);
    }

    public void disableDataConnectivity(int sub, Message dataCleanedUpMsg) {
        this.mPhones[sub].setInternalDataEnabled(false, dataCleanedUpMsg);
    }

    public void updateCurrentCarrierInProvider(int sub) {
        this.mPhones[sub].updateCurrentCarrierInProvider();
    }

    public void registerForAllDataDisconnected(int subId, Handler h, int what, Object obj) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            this.mPhones[phoneId].registerForAllDataDisconnected(h, what, obj);
        }
    }

    public void unregisterForAllDataDisconnected(int subId, Handler h) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            this.mPhones[phoneId].unregisterForAllDataDisconnected(h);
        }
    }

    public boolean isDataDisconnected(int subId) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            return true;
        }
        return this.mPhones[phoneId].mDcTracker.isDisconnected();
    }

    public int getRadioAccessFamily(int phoneId) {
        if (phoneId >= this.mPhones.length) {
            return 1;
        }
        return this.mPhones[phoneId].getRadioAccessFamily();
    }

    /* JADX WARNING: Missing block: B:15:0x0035, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:17:0x003a, code skipped:
            if (r1 >= r5.mPhones.length) goto L_0x0050;
     */
    /* JADX WARNING: Missing block: B:19:0x004a, code skipped:
            if (r5.mPhones[r1].getRadioAccessFamily() == r6[r1].getRadioAccessFamily()) goto L_0x004d;
     */
    /* JADX WARNING: Missing block: B:20:0x004c, code skipped:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:21:0x004d, code skipped:
            r1 = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:22:0x0050, code skipped:
            if (r0 == false) goto L_0x0067;
     */
    /* JADX WARNING: Missing block: B:23:0x0052, code skipped:
            logd("setRadioCapability: Already in requested configuration, nothing to do.");
            r5.mContext.sendBroadcast(new android.content.Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE"), "android.permission.READ_PHONE_STATE");
     */
    /* JADX WARNING: Missing block: B:24:0x0066, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:25:0x0067, code skipped:
            clearTransaction();
            r5.mWakeLock.acquire();
     */
    /* JADX WARNING: Missing block: B:26:0x0073, code skipped:
            return doSetRadioCapabilities(r6);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setRadioCapability(RadioAccessFamily[] rafs) {
        if (rafs.length == this.mPhones.length) {
            synchronized (this.mSetRadioAccessFamilyStatus) {
                int i = 0;
                for (int i2 = 0; i2 < this.mPhones.length; i2++) {
                    if (this.mSetRadioAccessFamilyStatus[i2] != 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("setRadioCapability: Phone[");
                        stringBuilder.append(i2);
                        stringBuilder.append("] is not idle. Rejecting request.");
                        loge(stringBuilder.toString());
                        return false;
                    }
                }
            }
        } else {
            throw new RuntimeException("Length of input rafs must equal to total phone count");
        }
    }

    private boolean doSetRadioCapabilities(RadioAccessFamily[] rafs) {
        this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
        int i = 0;
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, this.mRadioCapabilitySessionId, 0), 45000);
        synchronized (this.mSetRadioAccessFamilyStatus) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setRadioCapability: new request session id=");
            stringBuilder.append(this.mRadioCapabilitySessionId);
            logd(stringBuilder.toString());
            resetRadioAccessFamilyStatusCounter();
            while (i < rafs.length) {
                int phoneId = rafs[i].getPhoneId();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setRadioCapability: phoneId=");
                stringBuilder2.append(phoneId);
                stringBuilder2.append(" status=STARTING");
                logd(stringBuilder2.toString());
                this.mSetRadioAccessFamilyStatus[phoneId] = 1;
                this.mOldRadioAccessFamily[phoneId] = this.mPhones[phoneId].getRadioAccessFamily();
                int requestedRaf = rafs[i].getRadioAccessFamily();
                this.mNewRadioAccessFamily[phoneId] = requestedRaf;
                this.mCurrentLogicalModemIds[phoneId] = this.mPhones[phoneId].getModemUuId();
                this.mNewLogicalModemIds[phoneId] = getLogicalModemIdFromRaf(requestedRaf);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setRadioCapability: mOldRadioAccessFamily[");
                stringBuilder2.append(phoneId);
                stringBuilder2.append("]=");
                stringBuilder2.append(this.mOldRadioAccessFamily[phoneId]);
                logd(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setRadioCapability: mNewRadioAccessFamily[");
                stringBuilder2.append(phoneId);
                stringBuilder2.append("]=");
                stringBuilder2.append(this.mNewRadioAccessFamily[phoneId]);
                logd(stringBuilder2.toString());
                sendRadioCapabilityRequest(phoneId, this.mRadioCapabilitySessionId, 1, this.mOldRadioAccessFamily[phoneId], this.mCurrentLogicalModemIds[phoneId], 0, 2);
                i++;
            }
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:35:0x014e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onStartRadioCapabilityResponse(Message msg) {
        Message message = msg;
        synchronized (this.mSetRadioAccessFamilyStatus) {
            AsyncResult ar = message.obj;
            StringBuilder stringBuilder;
            if (TelephonyManager.getDefault().getPhoneCount() != 1 || ar.exception == null) {
                RadioCapability rc = (RadioCapability) ((AsyncResult) message.obj).result;
                if (rc != null) {
                    if (rc.getSession() == this.mRadioCapabilitySessionId) {
                        this.mRadioAccessFamilyStatusCounter--;
                        int id = rc.getPhoneId();
                        if (((AsyncResult) message.obj).exception != null) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onStartRadioCapabilityResponse: Error response session=");
                            stringBuilder.append(rc.getSession());
                            logd(stringBuilder.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onStartRadioCapabilityResponse: phoneId=");
                            stringBuilder.append(id);
                            stringBuilder.append(" status=FAIL");
                            logd(stringBuilder.toString());
                            this.mSetRadioAccessFamilyStatus[id] = 5;
                            this.mTransactionFailed = true;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onStartRadioCapabilityResponse: phoneId=");
                            stringBuilder.append(id);
                            stringBuilder.append(" status=STARTED");
                            logd(stringBuilder.toString());
                            this.mSetRadioAccessFamilyStatus[id] = 2;
                        }
                        if (this.mRadioAccessFamilyStatusCounter == 0) {
                            HashSet<String> modemsInUse = new HashSet(this.mNewLogicalModemIds.length);
                            int i = 0;
                            for (String modemId : this.mNewLogicalModemIds) {
                                if (!modemsInUse.add(modemId)) {
                                    this.mTransactionFailed = true;
                                    Log.wtf(LOG_TAG, "ERROR: sending down the same id for different phones");
                                }
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onStartRadioCapabilityResponse: success=");
                            stringBuilder.append(1 ^ this.mTransactionFailed);
                            logd(stringBuilder.toString());
                            if (!this.mTransactionFailed) {
                                resetRadioAccessFamilyStatusCounter();
                                while (true) {
                                    int i2 = i;
                                    if (i2 >= this.mPhones.length) {
                                        break;
                                    }
                                    sendRadioCapabilityRequest(i2, this.mRadioCapabilitySessionId, 2, this.mNewRadioAccessFamily[i2], this.mNewLogicalModemIds[i2], 0, 3);
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("onStartRadioCapabilityResponse: phoneId=");
                                    stringBuilder.append(i2);
                                    stringBuilder.append(" status=APPLYING");
                                    logd(stringBuilder.toString());
                                    this.mSetRadioAccessFamilyStatus[i2] = 3;
                                    i = i2 + 1;
                                }
                            } else {
                                issueFinish(this.mRadioCapabilitySessionId);
                            }
                        }
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("onStartRadioCapabilityResponse: Ignore session=");
                stringBuilder.append(this.mRadioCapabilitySessionId);
                stringBuilder.append(" rc=");
                stringBuilder.append(rc);
                logd(stringBuilder.toString());
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("onStartRadioCapabilityResponse got exception=");
            stringBuilder.append(ar.exception);
            logd(stringBuilder.toString());
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED"));
            clearTransaction();
        }
    }

    private void onApplyRadioCapabilityResponse(Message msg) {
        RadioCapability rc = ((AsyncResult) msg.obj).result;
        StringBuilder stringBuilder;
        if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("onApplyRadioCapabilityResponse: Ignore session=");
            stringBuilder.append(this.mRadioCapabilitySessionId);
            stringBuilder.append(" rc=");
            stringBuilder.append(rc);
            logd(stringBuilder.toString());
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("onApplyRadioCapabilityResponse: rc=");
        stringBuilder.append(rc);
        logd(stringBuilder.toString());
        if (((AsyncResult) msg.obj).exception != null) {
            synchronized (this.mSetRadioAccessFamilyStatus) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onApplyRadioCapabilityResponse: Error response session=");
                stringBuilder2.append(rc.getSession());
                logd(stringBuilder2.toString());
                int id = rc.getPhoneId();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("onApplyRadioCapabilityResponse: phoneId=");
                stringBuilder3.append(id);
                stringBuilder3.append(" status=FAIL");
                logd(stringBuilder3.toString());
                this.mSetRadioAccessFamilyStatus[id] = 5;
                this.mTransactionFailed = true;
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("onApplyRadioCapabilityResponse: Valid start expecting notification rc=");
            stringBuilder.append(rc);
            logd(stringBuilder.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:29:0x00e9  */
    /* JADX WARNING: Missing block: B:39:0x0121, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onNotificationRadioCapabilityChanged(Message msg) {
        RadioCapability rc = ((AsyncResult) msg.obj).result;
        if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onNotificationRadioCapabilityChanged: Ignore session=");
            stringBuilder.append(this.mRadioCapabilitySessionId);
            stringBuilder.append(" rc=");
            stringBuilder.append(rc);
            logd(stringBuilder.toString());
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onNotificationRadioCapabilityChanged: rc=");
            stringBuilder2.append(rc);
            logd(stringBuilder2.toString());
            if (rc.getSession() != this.mRadioCapabilitySessionId) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onNotificationRadioCapabilityChanged: Ignore session=");
                stringBuilder2.append(this.mRadioCapabilitySessionId);
                stringBuilder2.append(" rc=");
                stringBuilder2.append(rc);
                logd(stringBuilder2.toString());
                return;
            }
            int id = rc.getPhoneId();
            if (this.mSetRadioAccessFamilyStatus[id] != 3) {
                logd("onNotificationRadioCapabilityChanged: mSetRadioAccessFamilyStatus is not SET_RC_STATUS_APPLYING, return");
                return;
            }
            StringBuilder stringBuilder3;
            if (((AsyncResult) msg.obj).exception == null) {
                if (rc.getStatus() != 2) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("onNotificationRadioCapabilityChanged: phoneId=");
                    stringBuilder3.append(id);
                    stringBuilder3.append(" status=SUCCESS");
                    logd(stringBuilder3.toString());
                    this.mSetRadioAccessFamilyStatus[id] = 4;
                    if (IS_QCRIL_CROSS_MAPPING) {
                        this.mRadioCapabilitys[id] = rc;
                    } else {
                        this.mPhoneSwitcher.resendDataAllowed(id);
                        this.mPhones[id].radioCapabilityUpdated(rc);
                    }
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("onNotificationRadioCapabilityChanged: mRadioAccessFamilyStatusCounter = ");
                    stringBuilder3.append(this.mRadioAccessFamilyStatusCounter);
                    logd(stringBuilder3.toString());
                    this.mRadioAccessFamilyStatusCounter--;
                    if (this.mRadioAccessFamilyStatusCounter == 0) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("onNotificationRadioCapabilityChanged: APPLY URC success=");
                        stringBuilder3.append(this.mTransactionFailed);
                        logd(stringBuilder3.toString());
                        if (IS_QCRIL_CROSS_MAPPING && !this.mTransactionFailed) {
                            for (int i = 0; i < this.mRadioCapabilitys.length; i++) {
                                this.mPhones[i].radioCapabilityUpdated(this.mRadioCapabilitys[i]);
                            }
                        }
                        issueFinish(this.mRadioCapabilitySessionId);
                    }
                }
            }
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onNotificationRadioCapabilityChanged: phoneId=");
            stringBuilder3.append(id);
            stringBuilder3.append(" status=FAIL");
            logd(stringBuilder3.toString());
            this.mSetRadioAccessFamilyStatus[id] = 5;
            this.mTransactionFailed = true;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onNotificationRadioCapabilityChanged: mRadioAccessFamilyStatusCounter = ");
            stringBuilder3.append(this.mRadioAccessFamilyStatusCounter);
            logd(stringBuilder3.toString());
            this.mRadioAccessFamilyStatusCounter--;
            if (this.mRadioAccessFamilyStatusCounter == 0) {
            }
        }
    }

    void onFinishRadioCapabilityResponse(Message msg) {
        RadioCapability rc = ((AsyncResult) msg.obj).result;
        if (rc == null || rc.getSession() == this.mRadioCapabilitySessionId) {
            synchronized (this.mSetRadioAccessFamilyStatus) {
                if (rc == null) {
                    try {
                        logd("onFinishRadioCapabilityResponse: rc == null");
                        this.mTransactionFailed = true;
                    } catch (Throwable th) {
                    }
                } else {
                    StringBuilder stringBuilder;
                    int id = rc.getPhoneId();
                    if (((AsyncResult) msg.obj).exception == null) {
                        if (rc.getStatus() != 2) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onFinishRadioCapabilityResponse: phoneId=");
                            stringBuilder.append(id);
                            stringBuilder.append(" status=SUCCESS");
                            logd(stringBuilder.toString());
                            this.mSetRadioAccessFamilyStatus[id] = 0;
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onFinishRadioCapabilityResponse: phoneId=");
                    stringBuilder.append(id);
                    stringBuilder.append(" status=FAIL");
                    logd(stringBuilder.toString());
                    this.mTransactionFailed = true;
                    this.mSetRadioAccessFamilyStatus[id] = 0;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" onFinishRadioCapabilityResponse mRadioAccessFamilyStatusCounter=");
                stringBuilder2.append(this.mRadioAccessFamilyStatusCounter);
                logd(stringBuilder2.toString());
                this.mRadioAccessFamilyStatusCounter--;
                if (this.mRadioAccessFamilyStatusCounter == 0) {
                    completeRadioCapabilityTransaction();
                }
            }
            return;
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("onFinishRadioCapabilityResponse: Ignore session=");
        stringBuilder3.append(this.mRadioCapabilitySessionId);
        stringBuilder3.append(" rc=");
        stringBuilder3.append(rc);
        logd(stringBuilder3.toString());
    }

    private void onTimeoutRadioCapability(Message msg) {
        if (msg.arg1 != this.mRadioCapabilitySessionId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RadioCapability timeout: Ignore msg.arg1=");
            stringBuilder.append(msg.arg1);
            stringBuilder.append("!= mRadioCapabilitySessionId=");
            stringBuilder.append(this.mRadioCapabilitySessionId);
            logd(stringBuilder.toString());
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("RadioCapability timeout: mSetRadioAccessFamilyStatus[");
                stringBuilder2.append(i);
                stringBuilder2.append("]=");
                stringBuilder2.append(this.mSetRadioAccessFamilyStatus[i]);
                logd(stringBuilder2.toString());
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onTimeoutRadioCapability: mRadioAccessFamilyStatusCounter = ");
            stringBuilder3.append(this.mRadioAccessFamilyStatusCounter);
            logd(stringBuilder3.toString());
            this.mRadioAccessFamilyStatusCounter = 0;
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            this.mRadioAccessFamilyStatusCounter = 0;
            this.mTransactionFailed = true;
            issueFinish(this.mRadioCapabilitySessionId);
        }
    }

    private void issueFinish(int sessionId) {
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                int i2;
                String str;
                int i3;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("issueFinish: phoneId=");
                stringBuilder.append(i);
                stringBuilder.append(" sessionId=");
                stringBuilder.append(sessionId);
                stringBuilder.append(" mTransactionFailed=");
                stringBuilder.append(this.mTransactionFailed);
                logd(stringBuilder.toString());
                this.mRadioAccessFamilyStatusCounter++;
                if (this.mTransactionFailed) {
                    i2 = this.mOldRadioAccessFamily[i];
                } else {
                    i2 = this.mNewRadioAccessFamily[i];
                }
                int i4 = i2;
                if (this.mTransactionFailed) {
                    str = this.mCurrentLogicalModemIds[i];
                } else {
                    str = this.mNewLogicalModemIds[i];
                }
                String str2 = str;
                if (this.mTransactionFailed) {
                    i3 = 2;
                } else {
                    i3 = 1;
                }
                sendRadioCapabilityRequest(i, sessionId, 4, i4, str2, i3, 4);
                if (this.mTransactionFailed) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("issueFinish: phoneId: ");
                    stringBuilder.append(i);
                    stringBuilder.append(" status: FAIL");
                    logd(stringBuilder.toString());
                    this.mSetRadioAccessFamilyStatus[i] = 5;
                }
            }
        }
    }

    private void completeRadioCapabilityTransaction() {
        Intent intent;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onFinishRadioCapabilityResponse: success=");
        stringBuilder.append(this.mTransactionFailed ^ 1);
        logd(stringBuilder.toString());
        int i = 0;
        if (this.mTransactionFailed) {
            intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
            this.mTransactionFailed = false;
            if (IS_FAST_SWITCH_SIMSLOT) {
                logd("onFinishRadioCapabilityResponse: failed, broadcast ACTION_SET_RADIO_CAPABILITY_FAILED");
                clearTransaction();
            } else if (IS_QCRIL_CROSS_MAPPING) {
                synchronized (this.mSetRadioAccessFamilyStatus) {
                    for (int i2 = 0; i2 < this.mPhones.length; i2++) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("onFinishRadioCapabilityResponse: phoneId=");
                        stringBuilder2.append(i2);
                        stringBuilder2.append(" status=IDLE");
                        logd(stringBuilder2.toString());
                        this.mSetRadioAccessFamilyStatus[i2] = 0;
                    }
                    if (this.mWakeLock.isHeld()) {
                        this.mWakeLock.release();
                    }
                }
            } else {
                RadioAccessFamily[] rafs = new RadioAccessFamily[this.mPhones.length];
                while (i < this.mPhones.length) {
                    rafs[i] = new RadioAccessFamily(i, this.mOldRadioAccessFamily[i]);
                    i++;
                }
                doSetRadioCapabilities(rafs);
            }
        } else {
            ArrayList<RadioAccessFamily> phoneRAFList = new ArrayList();
            while (i < this.mPhones.length) {
                int raf = this.mPhones[i].getRadioAccessFamily();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("radioAccessFamily[");
                stringBuilder3.append(i);
                stringBuilder3.append("]=");
                stringBuilder3.append(raf);
                logd(stringBuilder3.toString());
                phoneRAFList.add(new RadioAccessFamily(i, raf));
                i++;
            }
            Intent intent2 = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
            intent2.putParcelableArrayListExtra("rafs", phoneRAFList);
            intent2.putExtra("intContent", this.mExpectedMainSlotId);
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            clearTransaction();
            intent = intent2;
        }
        this.mContext.sendBroadcast(intent, "android.permission.READ_PHONE_STATE");
    }

    public void retrySetRadioCapabilities() {
        RadioAccessFamily[] rafs = new RadioAccessFamily[this.mPhones.length];
        int i = 0;
        for (int phoneId = 0; phoneId < this.mPhones.length; phoneId++) {
            rafs[phoneId] = new RadioAccessFamily(phoneId, this.mNewRadioAccessFamily[phoneId]);
        }
        this.mWakeLock.acquire();
        this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, this.mRadioCapabilitySessionId, 0), 45000);
        synchronized (this.mSetRadioAccessFamilyStatus) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("retrySetRadioCapabilities: new request session id=");
            stringBuilder.append(this.mRadioCapabilitySessionId);
            logd(stringBuilder.toString());
            resetRadioAccessFamilyStatusCounter();
            while (i < rafs.length) {
                int phoneId2 = rafs[i].getPhoneId();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("retrySetRadioCapabilities: phoneId=");
                stringBuilder2.append(phoneId2);
                stringBuilder2.append(" status=STARTING");
                logd(stringBuilder2.toString());
                this.mSetRadioAccessFamilyStatus[phoneId2] = 1;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("retrySetRadioCapabilities: mOldRadioAccessFamily[");
                stringBuilder2.append(phoneId2);
                stringBuilder2.append("]=");
                stringBuilder2.append(this.mOldRadioAccessFamily[phoneId2]);
                logd(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("retrySetRadioCapabilities: mNewRadioAccessFamily[");
                stringBuilder2.append(phoneId2);
                stringBuilder2.append("]=");
                stringBuilder2.append(this.mNewRadioAccessFamily[phoneId2]);
                logd(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("retrySetRadioCapabilities: phoneId=");
                stringBuilder2.append(phoneId2);
                stringBuilder2.append(" mCurrentLogicalModemIds=");
                stringBuilder2.append(this.mCurrentLogicalModemIds[phoneId2]);
                stringBuilder2.append(" mNewLogicalModemIds=");
                stringBuilder2.append(this.mNewLogicalModemIds[phoneId2]);
                logd(stringBuilder2.toString());
                sendRadioCapabilityRequest(phoneId2, this.mRadioCapabilitySessionId, 1, this.mOldRadioAccessFamily[phoneId2], this.mCurrentLogicalModemIds[phoneId2], 0, 2);
                i++;
            }
        }
    }

    public void stopTransaction() {
        boolean isInTransaction = false;
        for (int i = 0; i < this.mPhones.length; i++) {
            if (this.mSetRadioAccessFamilyStatus[i] != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stopTransaction: mSetRadioAccessFamilyStatus[");
                stringBuilder.append(i);
                stringBuilder.append("] = ");
                stringBuilder.append(this.mSetRadioAccessFamilyStatus[i]);
                logd(stringBuilder.toString());
                isInTransaction = true;
                break;
            }
        }
        if (isInTransaction) {
            Intent intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
            clearTransaction();
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            logd("stopTransaction: broadcast ACTION_SET_RADIO_CAPABILITY_FAILED");
            this.mContext.sendBroadcast(intent, "android.permission.READ_PHONE_STATE");
            return;
        }
        logd("stopTransaction: not in transaction.");
    }

    public void syncRadioCapability(int mainStackPhoneId) {
        if (mainStackPhoneId >= 0 && mainStackPhoneId < this.mPhones.length) {
            int maxRafPhoneid = -1;
            for (int i = 0; i < this.mPhones.length; i++) {
                if (this.mPhones[i].getRadioAccessFamily() == getMaxRafSupported()) {
                    maxRafPhoneid = i;
                    break;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("syncRadioCapability maxRafPhoneid =");
            stringBuilder.append(maxRafPhoneid);
            stringBuilder.append("; mainStackPhoneId = ");
            stringBuilder.append(mainStackPhoneId);
            logd(stringBuilder.toString());
            if (maxRafPhoneid != -1 && maxRafPhoneid != mainStackPhoneId) {
                RadioCapability swapRadioCapability = this.mPhones[maxRafPhoneid].getRadioCapability();
                this.mPhones[maxRafPhoneid].radioCapabilityUpdated(this.mPhones[mainStackPhoneId].getRadioCapability());
                this.mPhones[mainStackPhoneId].radioCapabilityUpdated(swapRadioCapability);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("syncRadioCapability mPhones[");
                stringBuilder2.append(maxRafPhoneid);
                stringBuilder2.append("].getRadioAccessFamily = ");
                stringBuilder2.append(this.mPhones[maxRafPhoneid].getRadioAccessFamily());
                logd(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("syncRadioCapability mPhones[");
                stringBuilder2.append(mainStackPhoneId);
                stringBuilder2.append("].getRadioAccessFamily = ");
                stringBuilder2.append(this.mPhones[mainStackPhoneId].getRadioAccessFamily());
                logd(stringBuilder2.toString());
            }
        }
    }

    private void clearTransaction() {
        logd("clearTransaction");
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("clearTransaction: phoneId=");
                stringBuilder.append(i);
                stringBuilder.append(" status=IDLE");
                logd(stringBuilder.toString());
                this.mSetRadioAccessFamilyStatus[i] = 0;
                this.mOldRadioAccessFamily[i] = 0;
                this.mNewRadioAccessFamily[i] = 0;
                this.mTransactionFailed = false;
                this.mRadioCapabilitys[i] = null;
            }
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
    }

    private void resetRadioAccessFamilyStatusCounter() {
        this.mRadioAccessFamilyStatusCounter = this.mPhones.length;
    }

    private void sendRadioCapabilityRequest(int phoneId, int sessionId, int rcPhase, int radioFamily, String logicalModemId, int status, int eventId) {
        this.mPhones[phoneId].setRadioCapability(new RadioCapability(phoneId, sessionId, rcPhase, radioFamily, logicalModemId, status), this.mHandler.obtainMessage(eventId));
    }

    public int getMaxRafSupported() {
        int[] numRafSupported = new int[this.mPhones.length];
        int maxNumRafBit = 0;
        int maxRaf = 1;
        for (int len = 0; len < this.mPhones.length; len++) {
            numRafSupported[len] = Integer.bitCount(this.mPhones[len].getRadioAccessFamily());
            if (maxNumRafBit < numRafSupported[len]) {
                maxNumRafBit = numRafSupported[len];
                maxRaf = this.mPhones[len].getRadioAccessFamily();
            }
        }
        return maxRaf;
    }

    public int getMinRafSupported() {
        int[] numRafSupported = new int[this.mPhones.length];
        int minNumRafBit = 0;
        int minRaf = 1;
        int len = 0;
        while (len < this.mPhones.length) {
            numRafSupported[len] = Integer.bitCount(this.mPhones[len].getRadioAccessFamily());
            if (minNumRafBit == 0 || minNumRafBit > numRafSupported[len]) {
                minNumRafBit = numRafSupported[len];
                minRaf = this.mPhones[len].getRadioAccessFamily();
            }
            len++;
        }
        return minRaf;
    }

    private String getLogicalModemIdFromRaf(int raf) {
        for (int phoneId = 0; phoneId < this.mPhones.length; phoneId++) {
            if (this.mPhones[phoneId].getRadioAccessFamily() == raf) {
                return this.mPhones[phoneId].getModemUuId();
            }
        }
        return null;
    }

    public boolean setRadioCapability(int expectedMainSlotId, int cdmaSimSlotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setRadioCapability: expectedMainSlotId=");
        stringBuilder.append(expectedMainSlotId);
        stringBuilder.append(" cdmaSimSlotId=");
        stringBuilder.append(cdmaSimSlotId);
        logd(stringBuilder.toString());
        this.mExpectedMainSlotId = expectedMainSlotId;
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                if (this.mSetRadioAccessFamilyStatus[i] != 0) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setRadioCapability: Phone[");
                    stringBuilder2.append(i);
                    stringBuilder2.append("] is not idle. Rejecting request.");
                    loge(stringBuilder2.toString());
                    clearTransaction();
                    return false;
                }
            }
            clearTransaction();
            this.mWakeLock.acquire();
            doSetRadioCapabilities(expectedMainSlotId, cdmaSimSlotId);
            return true;
        }
    }

    private void doSetRadioCapabilities(int expectedMainSlotId, int cdmaSimSlotId) {
        if (SubscriptionManager.isValidSlotIndex(expectedMainSlotId)) {
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            int phoneId = 0;
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, this.mRadioCapabilitySessionId, 0), 45000);
            synchronized (this.mSetRadioAccessFamilyStatus) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setRadioCapability: new request session id=");
                stringBuilder.append(this.mRadioCapabilitySessionId);
                logd(stringBuilder.toString());
                resetRadioAccessFamilyStatusCounter();
                while (true) {
                    int phoneId2 = phoneId;
                    if (phoneId2 < this.mPhones.length) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("setRadioCapability: phoneId=");
                        stringBuilder2.append(phoneId2);
                        stringBuilder2.append(" status=STARTING");
                        logd(stringBuilder2.toString());
                        this.mSetRadioAccessFamilyStatus[phoneId2] = 1;
                        this.mOldRadioAccessFamily[phoneId2] = this.mPhones[phoneId2].getRadioAccessFamily();
                        phoneId = this.mPhones[phoneId2].getRadioAccessFamily();
                        if (phoneId2 == cdmaSimSlotId) {
                            phoneId |= 64;
                        } else {
                            phoneId &= -65;
                        }
                        this.mNewRadioAccessFamily[phoneId2] = phoneId;
                        this.mCurrentLogicalModemIds[phoneId2] = this.mPhones[phoneId2].getModemUuId();
                        if (phoneId2 == expectedMainSlotId) {
                            this.mNewLogicalModemIds[phoneId2] = MODEM_0;
                        } else {
                            this.mNewLogicalModemIds[phoneId2] = MODEM_1;
                        }
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("setRadioCapability: phoneId=");
                        stringBuilder3.append(phoneId2);
                        stringBuilder3.append(" mOldRadioAccessFamily=");
                        stringBuilder3.append(this.mOldRadioAccessFamily[phoneId2]);
                        stringBuilder3.append(" mNewRadioAccessFamily=");
                        stringBuilder3.append(this.mNewRadioAccessFamily[phoneId2]);
                        logd(stringBuilder3.toString());
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("setRadioCapability: phoneId=");
                        stringBuilder3.append(phoneId2);
                        stringBuilder3.append(" mCurrentLogicalModemIds=");
                        stringBuilder3.append(this.mCurrentLogicalModemIds[phoneId2]);
                        stringBuilder3.append(" mNewLogicalModemIds=");
                        stringBuilder3.append(this.mNewLogicalModemIds[phoneId2]);
                        logd(stringBuilder3.toString());
                        sendRadioCapabilityRequest(phoneId2, this.mRadioCapabilitySessionId, 1, this.mOldRadioAccessFamily[phoneId2], this.mCurrentLogicalModemIds[phoneId2], 0, 2);
                        phoneId = phoneId2 + 1;
                    }
                }
            }
        }
    }

    private void logd(String string) {
        Rlog.d(LOG_TAG, string);
    }

    private void loge(String string) {
        Rlog.e(LOG_TAG, string);
    }

    public PhoneSubInfoController getPhoneSubInfoController() {
        return this.mPhoneSubInfoController;
    }
}
