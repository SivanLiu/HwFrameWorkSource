package com.android.internal.telephony;

import android.app.ActivityManager;
import android.app.UserSwitchObserver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageManager.Stub;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.service.euicc.EuiccProfileInfo;
import android.service.euicc.GetEuiccProfileInfoListResult;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.internal.telephony.euicc.EuiccController;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionInfoUpdater extends AbstractSubscriptionInfoUpdater {
    public static final String CURR_SUBID = "curr_subid";
    private static final int EVENT_GET_NETWORK_SELECTION_MODE_DONE = 2;
    private static final int EVENT_INVALID = -1;
    private static final int EVENT_REFRESH_EMBEDDED_SUBSCRIPTIONS = 12;
    private static final int EVENT_SIM_ABSENT = 4;
    private static final int EVENT_SIM_IMSI = 11;
    private static final int EVENT_SIM_IO_ERROR = 6;
    private static final int EVENT_SIM_LOADED = 3;
    private static final int EVENT_SIM_LOCKED = 5;
    private static final int EVENT_SIM_NOT_READY = 9;
    private static final int EVENT_SIM_READY = 10;
    private static final int EVENT_SIM_RESTRICTED = 8;
    private static final int EVENT_SIM_UNKNOWN = 7;
    private static final String ICCID_STRING_FOR_NO_SIM = "";
    private static final String LOG_TAG = "SubscriptionInfoUpdater";
    private static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    public static final int SIM_CHANGED = -1;
    public static final int SIM_NEW = -2;
    public static final int SIM_NOT_CHANGE = 0;
    public static final int SIM_NOT_INSERT = -99;
    public static final int SIM_REPOSITION = -3;
    public static final int STATUS_NO_SIM_INSERTED = 0;
    public static final int STATUS_SIM1_INSERTED = 1;
    public static final int STATUS_SIM2_INSERTED = 2;
    public static final int STATUS_SIM3_INSERTED = 4;
    public static final int STATUS_SIM4_INSERTED = 8;
    private static Context mContext = null;
    private static String[] mIccId = new String[PROJECT_SIM_NUM];
    private static int[] mInsertSimState = new int[PROJECT_SIM_NUM];
    private static Phone[] mPhone;
    private static int[] sSimApplicationState = new int[PROJECT_SIM_NUM];
    private static int[] sSimCardState = new int[PROJECT_SIM_NUM];
    private CarrierServiceBindHelper mCarrierServiceBindHelper;
    private int mCurrentlyActiveUserId;
    private EuiccManager mEuiccManager;
    private IPackageManager mPackageManager;
    private SubscriptionManager mSubscriptionManager = null;

    public SubscriptionInfoUpdater(Looper looper, Context context, Phone[] phone, CommandsInterface[] ci) {
        super(looper);
        logd("Constructor invoked");
        mContext = context;
        mPhone = phone;
        this.mSubscriptionManager = SubscriptionManager.from(mContext);
        this.mEuiccManager = (EuiccManager) mContext.getSystemService("euicc");
        this.mPackageManager = Stub.asInterface(ServiceManager.getService("package"));
        subscriptionInfoInit(this, context, ci);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        this.mCarrierServiceBindHelper = new CarrierServiceBindHelper(mContext);
        initializeCarrierApps();
    }

    private void initializeCarrierApps() {
        this.mCurrentlyActiveUserId = 0;
        try {
            ActivityManager.getService().registerUserSwitchObserver(new UserSwitchObserver() {
                public void onUserSwitching(int newUserId, IRemoteCallback reply) throws RemoteException {
                    SubscriptionInfoUpdater.this.mCurrentlyActiveUserId = newUserId;
                    CarrierAppUtils.disableCarrierAppsUntilPrivileged(SubscriptionInfoUpdater.mContext.getOpPackageName(), SubscriptionInfoUpdater.this.mPackageManager, TelephonyManager.getDefault(), SubscriptionInfoUpdater.mContext.getContentResolver(), SubscriptionInfoUpdater.this.mCurrentlyActiveUserId);
                    if (reply != null) {
                        try {
                            reply.sendResult(null);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }, LOG_TAG);
            this.mCurrentlyActiveUserId = ActivityManager.getService().getCurrentUser().id;
        } catch (RemoteException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't get current user ID; guessing it's 0: ");
            stringBuilder.append(e.getMessage());
            logd(stringBuilder.toString());
        }
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(mContext.getOpPackageName(), this.mPackageManager, TelephonyManager.getDefault(), mContext.getContentResolver(), this.mCurrentlyActiveUserId);
    }

    public void updateInternalIccState(String simStatus, String reason, int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateInternalIccState to simStatus ");
        stringBuilder.append(simStatus);
        stringBuilder.append(" reason ");
        stringBuilder.append(reason);
        stringBuilder.append(" slotId ");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        if (SubscriptionManager.isValidSlotIndex(slotId)) {
            int message = internalIccStateToMessage(simStatus);
            if (message != -1) {
                sendMessage(obtainMessage(message, slotId, -1, reason));
            }
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("updateInternalIccState contains invalid slotIndex: ");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int internalIccStateToMessage(String simStatus) {
        int i;
        switch (simStatus.hashCode()) {
            case -2044189691:
                if (simStatus.equals("LOADED")) {
                    i = 6;
                    break;
                }
            case -2044123382:
                if (simStatus.equals("LOCKED")) {
                    i = 5;
                    break;
                }
            case -1830845986:
                if (simStatus.equals("CARD_IO_ERROR")) {
                    i = 2;
                    break;
                }
            case 2251386:
                if (simStatus.equals("IMSI")) {
                    i = 8;
                    break;
                }
            case 77848963:
                if (simStatus.equals("READY")) {
                    i = 7;
                    break;
                }
            case 433141802:
                if (simStatus.equals("UNKNOWN")) {
                    i = 1;
                    break;
                }
            case 1034051831:
                if (simStatus.equals("NOT_READY")) {
                    i = 4;
                    break;
                }
            case 1599753450:
                if (simStatus.equals("CARD_RESTRICTED")) {
                    i = 3;
                    break;
                }
            case 1924388665:
                if (simStatus.equals("ABSENT")) {
                    i = 0;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return 4;
            case 1:
                return 7;
            case 2:
                return 6;
            case 3:
                return 8;
            case 4:
                return 9;
            case 5:
                return 5;
            case 6:
                return 3;
            case 7:
                return 10;
            case 8:
                return 11;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ignoring simStatus: ");
                stringBuilder.append(simStatus);
                logd(stringBuilder.toString());
                return -1;
        }
    }

    private boolean isAllIccIdQueryDone() {
        int i = 0;
        while (i < PROJECT_SIM_NUM) {
            StringBuilder stringBuilder;
            if (VSimUtilsInner.isPlatformTwoModems() && !VSimUtilsInner.isRadioAvailable(i)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[2Cards]Ignore pending sub");
                stringBuilder.append(i);
                logd(stringBuilder.toString());
                mIccId[i] = ICCID_STRING_FOR_NO_SIM;
            }
            if (mIccId[i] == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Wait for SIM");
                stringBuilder.append(i + 1);
                stringBuilder.append(" IccId");
                logd(stringBuilder.toString());
                return false;
            }
            i++;
        }
        logd("All IccIds query complete");
        return true;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 2:
                AsyncResult ar = msg.obj;
                Integer slotId = ar.userObj;
                if (ar.exception != null || ar.result == null) {
                    logd("EVENT_GET_NETWORK_SELECTION_MODE_DONE: error getting network mode.");
                    return;
                } else if (ar.result[0] == 1) {
                    mPhone[slotId.intValue()].setNetworkSelectionModeAutomatic(null);
                    return;
                } else {
                    return;
                }
            case 3:
                handleSimLoaded(msg.arg1);
                return;
            case 4:
                handleSimAbsent(msg.arg1);
                return;
            case 5:
                handleSimLocked(msg.arg1, (String) msg.obj);
                return;
            case 6:
                handleSimError(msg.arg1);
                return;
            case 7:
                updateCarrierServices(msg.arg1, "UNKNOWN");
                broadcastSimStateChanged(msg.arg1, "UNKNOWN", null);
                broadcastSimCardStateChanged(msg.arg1, 0);
                broadcastSimApplicationStateChanged(msg.arg1, 0);
                return;
            case 8:
                updateCarrierServices(msg.arg1, "CARD_RESTRICTED");
                broadcastSimStateChanged(msg.arg1, "CARD_RESTRICTED", "CARD_RESTRICTED");
                broadcastSimCardStateChanged(msg.arg1, 9);
                broadcastSimApplicationStateChanged(msg.arg1, 6);
                return;
            case 9:
                broadcastSimStateChanged(msg.arg1, "NOT_READY", null);
                broadcastSimCardStateChanged(msg.arg1, 11);
                broadcastSimApplicationStateChanged(msg.arg1, 6);
                break;
            case 10:
                broadcastSimStateChanged(msg.arg1, "READY", null);
                broadcastSimCardStateChanged(msg.arg1, 11);
                broadcastSimApplicationStateChanged(msg.arg1, 6);
                return;
            case 11:
                broadcastSimStateChanged(msg.arg1, "IMSI", null);
                return;
            case 12:
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown msg:");
                stringBuilder.append(msg.what);
                logd(stringBuilder.toString());
                handleMessageExtend(msg);
                return;
        }
        if (updateEmbeddedSubscriptions()) {
            SubscriptionController.getInstance().notifySubscriptionInfoChanged();
        }
        if (msg.obj != null) {
            ((Runnable) msg.obj).run();
        }
    }

    void requestEmbeddedSubscriptionInfoListRefresh(Runnable callback) {
        sendMessage(obtainMessage(12, callback));
    }

    private void handleSimLocked(int slotId, String reason) {
        if (mIccId[slotId] != null && mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SIM");
            stringBuilder.append(slotId + 1);
            stringBuilder.append(" hot plug in");
            logd(stringBuilder.toString());
            mIccId[slotId] = null;
        }
        String iccId = mIccId[slotId];
        if (iccId == null) {
            IccCard iccCard = mPhone[slotId].getIccCard();
            if (iccCard == null) {
                logd("handleSimLocked: IccCard null");
                return;
            }
            IccRecords records = iccCard.getIccRecords();
            if (records == null) {
                logd("handleSimLocked: IccRecords null");
                return;
            } else if (IccUtils.stripTrailingFs(records.getFullIccId()) == null) {
                logd("handleSimLocked: IccID null");
                return;
            } else {
                mIccId[slotId] = IccUtils.stripTrailingFs(records.getFullIccId());
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("NOT Querying IccId its already set sIccid[");
            stringBuilder2.append(slotId);
            stringBuilder2.append("]=");
            stringBuilder2.append(SubscriptionInfo.givePrintableIccid(iccId));
            logd(stringBuilder2.toString());
        }
        updateCarrierServices(slotId, "LOCKED");
        broadcastSimStateChanged(slotId, "LOCKED", reason);
        broadcastSimCardStateChanged(slotId, 11);
        broadcastSimApplicationStateChanged(slotId, getSimStateFromLockedReason(reason));
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0064 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0063 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0061  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005f  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0064 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0063 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0061  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005f  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0064 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0063 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0061  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005f  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0064 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0063 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0061  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int getSimStateFromLockedReason(String lockedReason) {
        int hashCode = lockedReason.hashCode();
        if (hashCode == -1733499378) {
            if (lockedReason.equals("NETWORK")) {
                hashCode = 2;
                switch (hashCode) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 79221) {
            if (lockedReason.equals("PIN")) {
                hashCode = 0;
                switch (hashCode) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 79590) {
            if (lockedReason.equals("PUK")) {
                hashCode = 1;
                switch (hashCode) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 190660331 && lockedReason.equals("PERM_DISABLED")) {
            hashCode = 3;
            switch (hashCode) {
                case 0:
                    return 2;
                case 1:
                    return 3;
                case 2:
                    return 4;
                case 3:
                    return 7;
                default:
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected SIM locked reason ");
                    stringBuilder.append(lockedReason);
                    Rlog.e(str, stringBuilder.toString());
                    return 0;
            }
        }
        hashCode = -1;
        switch (hashCode) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                break;
        }
    }

    private void handleSimLoaded(int slotId) {
        int i = slotId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleSimLoaded: slotId: ");
        stringBuilder.append(i);
        logd(stringBuilder.toString());
        int loadedSlotId = i;
        IccCard iccCard = mPhone[i].getIccCard();
        if (iccCard == null) {
            logd("handleSimLoaded: IccCard null");
            return;
        }
        IccRecords records = iccCard.getIccRecords();
        if (records == null) {
            logd("handleSimLoaded: IccRecords null");
        } else if (IccUtils.stripTrailingFs(records.getFullIccId()) == null) {
            logd("handleSimLoaded: IccID null");
        } else {
            StringBuilder stringBuilder2;
            IccRecords records2;
            int[] subIds;
            mIccId[i] = IccUtils.stripTrailingFs(records.getFullIccId());
            if (VSimUtilsInner.needBlockUnReservedForVsim(slotId)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("handleSimStateLoadedInternal: block Unreserved subId, don't set mIccId[");
                stringBuilder2.append(i);
                stringBuilder2.append("] from records");
                logd(stringBuilder2.toString());
            } else {
                String iccId;
                String[] strArr = mIccId;
                if (records.getIccId().trim().length() > 0) {
                    iccId = records.getIccId();
                } else {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("emptyiccid");
                    stringBuilder3.append(i);
                    iccId = stringBuilder3.toString();
                }
                strArr[i] = iccId;
            }
            int[] subIds2 = this.mSubscriptionManager.getActiveSubscriptionIdList();
            int length = subIds2.length;
            boolean z = false;
            int slotId2 = i;
            i = 0;
            while (i < length) {
                IccCard iccCard2;
                int subId = subIds2[i];
                TelephonyManager tm = TelephonyManager.getDefault();
                String operator = tm.getSimOperatorNumeric(subId);
                slotId2 = SubscriptionController.getInstance().getPhoneId(subId);
                if (TextUtils.isEmpty(operator)) {
                    logd("EVENT_RECORDS_LOADED Operator name is null");
                } else {
                    if (subId == SubscriptionController.getInstance().getDefaultSubId()) {
                        MccTable.updateMccMncConfiguration(mContext, operator, z);
                    }
                    SubscriptionController.getInstance().setMccMnc(operator, subId);
                }
                String msisdn = tm.getLine1Number(subId);
                ContentResolver contentResolver = mContext.getContentResolver();
                if (msisdn != null) {
                    SubscriptionController.getInstance().setDisplayNumber(msisdn, subId);
                }
                SubscriptionInfo subInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
                String simCarrierName = tm.getSimOperatorName(subId);
                if (subInfo != null) {
                    iccCard2 = iccCard;
                    if (subInfo.getNameSource() != 2) {
                        if (TextUtils.isEmpty(simCarrierName) == null) {
                            iccCard = simCarrierName;
                        } else if (tm.isMultiSimEnabled() != null) {
                            iccCard = new StringBuilder();
                            iccCard.append("CARD ");
                            iccCard.append(Integer.toString(slotId2 + 1));
                            iccCard = iccCard.toString();
                        } else {
                            iccCard = "CARD";
                        }
                        StringBuilder stringBuilder4 = new StringBuilder();
                        records2 = records;
                        stringBuilder4.append("sim name = ");
                        stringBuilder4.append(iccCard);
                        logd(stringBuilder4.toString());
                        SubscriptionController.getInstance().setDisplayName(iccCard, subId);
                    } else {
                        records2 = records;
                    }
                } else {
                    iccCard2 = iccCard;
                    records2 = records;
                }
                iccCard = PreferenceManager.getDefaultSharedPreferences(mContext);
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append(CURR_SUBID);
                stringBuilder5.append(slotId2);
                int storedSubId = iccCard.getInt(stringBuilder5.toString(), -1);
                if (storedSubId != subId) {
                    subIds = subIds2;
                    mPhone[slotId2].getNetworkSelectionMode(obtainMessage(2, new Integer(slotId2)));
                    Editor editor = iccCard.edit();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(CURR_SUBID);
                    stringBuilder2.append(slotId2);
                    editor.putInt(stringBuilder2.toString(), subId);
                    editor.apply();
                } else {
                    subIds = subIds2;
                }
                i++;
                iccCard = iccCard2;
                records = records2;
                subIds2 = subIds;
                z = false;
            }
            records2 = records;
            subIds = subIds2;
            CarrierAppUtils.disableCarrierAppsUntilPrivileged(mContext.getOpPackageName(), this.mPackageManager, TelephonyManager.getDefault(), mContext.getContentResolver(), this.mCurrentlyActiveUserId);
            broadcastSimStateChanged(loadedSlotId, "LOADED", null);
            broadcastSimCardStateChanged(loadedSlotId, 11);
            broadcastSimApplicationStateChanged(loadedSlotId, 10);
            updateCarrierServices(loadedSlotId, "LOADED");
        }
    }

    private void updateCarrierServices(int slotId, String simState) {
        ((CarrierConfigManager) mContext.getSystemService("carrier_config")).updateConfigForPhoneId(slotId, simState);
        this.mCarrierServiceBindHelper.updateForPhoneId(slotId, simState);
    }

    private void handleSimAbsent(int slotId) {
        if (!(mIccId[slotId] == null || mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM))) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SIM");
            stringBuilder.append(slotId + 1);
            stringBuilder.append(" hot plug out");
            logd(stringBuilder.toString());
        }
        updateCarrierServices(slotId, "ABSENT");
        broadcastSimStateChanged(slotId, "ABSENT", null);
        broadcastSimCardStateChanged(slotId, 1);
        broadcastSimApplicationStateChanged(slotId, 6);
    }

    private void handleSimError(int slotId) {
        if (!(mIccId[slotId] == null || mIccId[slotId].equals(ICCID_STRING_FOR_NO_SIM))) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SIM");
            stringBuilder.append(slotId + 1);
            stringBuilder.append(" Error ");
            logd(stringBuilder.toString());
        }
        updateCarrierServices(slotId, "CARD_IO_ERROR");
        broadcastSimStateChanged(slotId, "CARD_IO_ERROR", "CARD_IO_ERROR");
        broadcastSimCardStateChanged(slotId, 8);
        broadcastSimApplicationStateChanged(slotId, 6);
    }

    private synchronized void updateSubscriptionInfoByIccId() {
        synchronized (this) {
            int i;
            int i2;
            int i3;
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2;
            String str;
            logd("updateSubscriptionInfoByIccId:+ Start");
            int i4 = 0;
            mNeedUpdate = false;
            for (i = 0; i < PROJECT_SIM_NUM; i++) {
                mInsertSimState[i] = 0;
            }
            int insertedSimCount = PROJECT_SIM_NUM;
            i = 0;
            while (true) {
                i2 = -99;
                if (i >= PROJECT_SIM_NUM) {
                    break;
                }
                if (ICCID_STRING_FOR_NO_SIM.equals(mIccId[i])) {
                    insertedSimCount--;
                    mInsertSimState[i] = -99;
                }
                i++;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("insertedSimCount = ");
            stringBuilder3.append(insertedSimCount);
            logd(stringBuilder3.toString());
            if (SubscriptionController.getInstance().getActiveSubIdList().length > insertedSimCount) {
                SubscriptionController.getInstance().clearSubInfo();
            }
            int index = 0;
            i = 0;
            while (true) {
                i3 = 1;
                if (i >= PROJECT_SIM_NUM) {
                    break;
                }
                if (mInsertSimState[i] != -99) {
                    int index2 = 2;
                    int j = i + 1;
                    while (j < PROJECT_SIM_NUM) {
                        if (mInsertSimState[j] == 0 && mIccId[i] != null && mIccId[i].equals(mIccId[j])) {
                            mInsertSimState[i] = 1;
                            mInsertSimState[j] = index2;
                            index2++;
                        }
                        j++;
                    }
                    index = index2;
                }
                i++;
            }
            ContentResolver contentResolver = mContext.getContentResolver();
            String[] oldIccId = new String[PROJECT_SIM_NUM];
            String[] decIccId = new String[PROJECT_SIM_NUM];
            i = 0;
            while (i < PROJECT_SIM_NUM) {
                oldIccId[i] = null;
                List<SubscriptionInfo> oldSubInfo = SubscriptionController.getInstance().getSubInfoUsingSlotIndexPrivileged(i, false);
                decIccId[i] = IccUtils.getDecimalSubstring(mIccId[i]);
                if (oldSubInfo == null || oldSubInfo.size() <= 0) {
                    if (mInsertSimState[i] == 0) {
                        mInsertSimState[i] = -1;
                    }
                    oldIccId[i] = ICCID_STRING_FOR_NO_SIM;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateSubscriptionInfoByIccId: No SIM in slot ");
                    stringBuilder.append(i);
                    stringBuilder.append(" last time");
                    logd(stringBuilder.toString());
                } else {
                    oldIccId[i] = ((SubscriptionInfo) oldSubInfo.get(0)).getIccId();
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("updateSubscriptionInfoByIccId: oldSubId = ");
                    stringBuilder4.append(((SubscriptionInfo) oldSubInfo.get(0)).getSubscriptionId());
                    logd(stringBuilder4.toString());
                    if (mInsertSimState[i] == 0 && mIccId[i] != null && !mIccId[i].equals(oldIccId[i]) && (decIccId[i] == null || !decIccId[i].equals(oldIccId[i]))) {
                        mInsertSimState[i] = -1;
                    }
                    if (mInsertSimState[i] != 0) {
                        ContentValues value = new ContentValues(1);
                        value.put("sim_id", Integer.valueOf(-1));
                        Uri uri = SubscriptionManager.CONTENT_URI;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("sim_id=");
                        stringBuilder5.append(Integer.toString(((SubscriptionInfo) oldSubInfo.get(0)).getSubscriptionId()));
                        contentResolver.update(uri, value, stringBuilder5.toString(), null);
                        SubscriptionController.getInstance().refreshCachedActiveSubscriptionInfoList();
                    }
                }
                i++;
            }
            for (i = 0; i < PROJECT_SIM_NUM; i++) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateSubscriptionInfoByIccId: oldIccId[");
                stringBuilder2.append(i);
                stringBuilder2.append("] = ");
                stringBuilder2.append(SubscriptionInfo.givePrintableIccid(oldIccId[i]));
                stringBuilder2.append(", sIccId[");
                stringBuilder2.append(i);
                stringBuilder2.append("] = ");
                stringBuilder2.append(SubscriptionInfo.givePrintableIccid(mIccId[i]));
                logd(stringBuilder2.toString());
            }
            int nNewCardCount = 0;
            int nNewSimStatus = 0;
            i = 0;
            while (i < PROJECT_SIM_NUM) {
                if (mInsertSimState[i] == i2) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateSubscriptionInfoByIccId: No SIM inserted in slot ");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" this time");
                    logd(stringBuilder2.toString());
                    if (PROJECT_SIM_NUM == 1) {
                        HwTelephonyFactory.getHwUiccManager().updateUserPreferences(false);
                    }
                } else {
                    if (mInsertSimState[i] > 0) {
                        SubscriptionManager subscriptionManager = this.mSubscriptionManager;
                        StringBuilder stringBuilder6 = new StringBuilder();
                        stringBuilder6.append(mIccId[i]);
                        stringBuilder6.append(Integer.toString(mInsertSimState[i]));
                        subscriptionManager.addSubscriptionInfoRecord(stringBuilder6.toString(), i);
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("SUB");
                        stringBuilder2.append(i + 1);
                        stringBuilder2.append(" has invalid IccId");
                        logd(stringBuilder2.toString());
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateSubscriptionInfoByIccId: adding subscription info record: iccid: ");
                        stringBuilder2.append(SubscriptionInfo.givePrintableIccid(mIccId[i]));
                        stringBuilder2.append("slot: ");
                        stringBuilder2.append(i);
                        logd(stringBuilder2.toString());
                        this.mSubscriptionManager.addSubscriptionInfoRecord(mIccId[i], i);
                    }
                    if (mInsertSimState[i] == i + 1 && oldIccId[i] != null) {
                        str = oldIccId[i];
                        StringBuilder stringBuilder7 = new StringBuilder();
                        stringBuilder7.append(mIccId[i]);
                        stringBuilder7.append(Integer.toString(mInsertSimState[i]));
                        if (str.equals(stringBuilder7.toString())) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("same iccid not change index = ");
                            stringBuilder2.append(i);
                            logd(stringBuilder2.toString());
                            mInsertSimState[i] = 0;
                        }
                    }
                    if (isNewSim(mIccId[i], decIccId[i], oldIccId)) {
                        nNewCardCount++;
                        switch (i) {
                            case 0:
                                nNewSimStatus |= 1;
                                break;
                            case 1:
                                nNewSimStatus |= 2;
                                break;
                            case 2:
                                nNewSimStatus |= 4;
                                break;
                            default:
                                break;
                        }
                        mInsertSimState[i] = -2;
                    }
                }
                i++;
                i2 = -99;
            }
            for (i = 0; i < PROJECT_SIM_NUM; i++) {
                if (mInsertSimState[i] == -1) {
                    mInsertSimState[i] = -3;
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateSubscriptionInfoByIccId: sInsertSimState[");
                stringBuilder2.append(i);
                stringBuilder2.append("] = ");
                stringBuilder2.append(mInsertSimState[i]);
                logd(stringBuilder2.toString());
            }
            if (PROJECT_SIM_NUM > 1) {
                updateSubActivation(mInsertSimState, false);
            }
            List<SubscriptionInfo> subInfos = this.mSubscriptionManager.getActiveSubscriptionInfoList();
            int nSubCount = subInfos == null ? 0 : subInfos.size();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("updateSubscriptionInfoByIccId: nSubCount = ");
            stringBuilder3.append(nSubCount);
            logd(stringBuilder3.toString());
            while (i4 < nSubCount) {
                SubscriptionInfo temp = (SubscriptionInfo) subInfos.get(i4);
                str = TelephonyManager.getDefault().getLine1Number(temp.getSubscriptionId());
                if (str != null) {
                    ContentValues value2 = new ContentValues(i3);
                    value2.put("number", str);
                    Uri uri2 = SubscriptionManager.CONTENT_URI;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("sim_id=");
                    stringBuilder.append(Integer.toString(temp.getSubscriptionId()));
                    contentResolver.update(uri2, value2, stringBuilder.toString(), null);
                    SubscriptionController.getInstance().refreshCachedActiveSubscriptionInfoList();
                }
                i4++;
                i3 = 1;
            }
            broadcastSubinfoRecordUpdated(mIccId, oldIccId, nNewCardCount, nSubCount, nNewSimStatus);
            updateEmbeddedSubscriptions();
            SubscriptionController.getInstance().notifySubscriptionInfoChanged();
            logd("updateSubscriptionInfoByIccId:- SubscriptionInfo update complete");
        }
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public boolean updateEmbeddedSubscriptions() {
        if (!this.mEuiccManager.isEnabled()) {
            return false;
        }
        GetEuiccProfileInfoListResult result = EuiccController.get().blockingGetEuiccProfileInfoList();
        if (result == null) {
            return false;
        }
        EuiccProfileInfo[] embeddedProfiles;
        int i;
        ContentValues values;
        if (result.getResult() == 0) {
            EuiccProfileInfo[] embeddedProfiles2;
            List<EuiccProfileInfo> list = result.getProfiles();
            if (list == null || list.size() == 0) {
                embeddedProfiles2 = new EuiccProfileInfo[0];
            } else {
                embeddedProfiles2 = (EuiccProfileInfo[]) list.toArray(new EuiccProfileInfo[list.size()]);
            }
            embeddedProfiles = embeddedProfiles2;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updatedEmbeddedSubscriptions: error ");
            stringBuilder.append(result.getResult());
            stringBuilder.append(" listing profiles");
            logd(stringBuilder.toString());
            embeddedProfiles = new EuiccProfileInfo[0];
        }
        boolean isRemovable = result.getIsRemovable();
        String[] embeddedIccids = new String[embeddedProfiles.length];
        for (i = 0; i < embeddedProfiles.length; i++) {
            embeddedIccids[i] = embeddedProfiles[i].getIccid();
        }
        List<SubscriptionInfo> existingSubscriptions = SubscriptionController.getInstance().getSubscriptionInfoListForEmbeddedSubscriptionUpdate(embeddedIccids, isRemovable);
        ContentResolver contentResolver = mContext.getContentResolver();
        int length = embeddedProfiles.length;
        boolean hasChanges = false;
        i = 0;
        while (i < length) {
            byte[] bArr;
            EuiccProfileInfo embeddedProfile = embeddedProfiles[i];
            int index = findSubscriptionInfoForIccid(existingSubscriptions, embeddedProfile.getIccid());
            if (index < 0) {
                SubscriptionController.getInstance().insertEmptySubInfoRecord(embeddedProfile.getIccid(), -1);
            } else {
                existingSubscriptions.remove(index);
            }
            values = new ContentValues();
            values.put("is_embedded", Integer.valueOf(1));
            List<UiccAccessRule> ruleList = embeddedProfile.getUiccAccessRules();
            boolean isRuleListEmpty = false;
            if (ruleList == null || ruleList.size() == 0) {
                isRuleListEmpty = true;
            }
            String str = "access_rules";
            if (isRuleListEmpty) {
                bArr = null;
            } else {
                bArr = UiccAccessRule.encodeRules((UiccAccessRule[]) ruleList.toArray(new UiccAccessRule[ruleList.size()]));
            }
            values.put(str, bArr);
            values.put("is_removable", Boolean.valueOf(isRemovable));
            values.put("display_name", embeddedProfile.getNickname());
            values.put("name_source", Integer.valueOf(2));
            hasChanges = true;
            Uri uri = SubscriptionManager.CONTENT_URI;
            StringBuilder stringBuilder2 = new StringBuilder();
            GetEuiccProfileInfoListResult result2 = result;
            stringBuilder2.append("icc_id=\"");
            stringBuilder2.append(embeddedProfile.getIccid());
            stringBuilder2.append("\"");
            contentResolver.update(uri, values, stringBuilder2.toString(), null);
            SubscriptionController.getInstance().refreshCachedActiveSubscriptionInfoList();
            i++;
            result = result2;
        }
        if (!existingSubscriptions.isEmpty()) {
            List<String> iccidsToRemove = new ArrayList();
            for (int i2 = 0; i2 < existingSubscriptions.size(); i2++) {
                SubscriptionInfo info = (SubscriptionInfo) existingSubscriptions.get(i2);
                if (info.isEmbedded()) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("\"");
                    stringBuilder3.append(info.getIccId());
                    stringBuilder3.append("\"");
                    iccidsToRemove.add(stringBuilder3.toString());
                }
            }
            String whereClause = new StringBuilder();
            whereClause.append("icc_id IN (");
            whereClause.append(TextUtils.join(",", iccidsToRemove));
            whereClause.append(")");
            whereClause = whereClause.toString();
            values = new ContentValues();
            values.put("is_embedded", Integer.valueOf(0));
            hasChanges = true;
            contentResolver.update(SubscriptionManager.CONTENT_URI, values, whereClause, null);
            SubscriptionController.getInstance().refreshCachedActiveSubscriptionInfoList();
        }
        return hasChanges;
    }

    private static int findSubscriptionInfoForIccid(List<SubscriptionInfo> list, String iccid) {
        for (int i = 0; i < list.size(); i++) {
            if (TextUtils.equals(iccid, ((SubscriptionInfo) list.get(i)).getIccId())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isNewSim(String iccId, String decIccId, String[] oldIccId) {
        boolean newSim = true;
        int i = 0;
        while (i < PROJECT_SIM_NUM) {
            if (iccId == null || !iccId.equals(oldIccId[i])) {
                if (decIccId != null && decIccId.equals(oldIccId[i])) {
                    newSim = false;
                    break;
                }
                i++;
            } else {
                newSim = false;
                break;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("newSim = ");
        stringBuilder.append(newSim);
        logd(stringBuilder.toString());
        return newSim;
    }

    private void broadcastSimStateChanged(int slotId, String state, String reason) {
    }

    public synchronized void resetInsertSimState() {
        logd("[resetInsertSimState]: reset the sInsertSimState to not change");
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            mInsertSimState[i] = 0;
        }
    }

    public void cleanIccids() {
        for (int i = 0; i < mIccId.length; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("clean iccids i=");
            stringBuilder.append(i);
            logd(stringBuilder.toString());
            mIccId[i] = null;
        }
    }

    private void broadcastSimCardStateChanged(int phoneId, int state) {
        if (state != sSimCardState[phoneId]) {
            sSimCardState[phoneId] = state;
            Intent i = new Intent("android.telephony.action.SIM_CARD_STATE_CHANGED");
            i.addFlags(67108864);
            i.addFlags(16777216);
            i.putExtra("android.telephony.extra.SIM_STATE", state);
            SubscriptionManager.putPhoneIdAndSubIdExtra(i, phoneId);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Broadcasting intent ACTION_SIM_CARD_STATE_CHANGED ");
            stringBuilder.append(simStateString(state));
            stringBuilder.append(" for phone: ");
            stringBuilder.append(phoneId);
            logd(stringBuilder.toString());
            mContext.sendBroadcast(i, "android.permission.READ_PRIVILEGED_PHONE_STATE");
        }
    }

    private void broadcastSimApplicationStateChanged(int phoneId, int state) {
        if (state == sSimApplicationState[phoneId]) {
            return;
        }
        if (state != 6 || sSimApplicationState[phoneId] != 0) {
            sSimApplicationState[phoneId] = state;
            Intent i = new Intent("android.telephony.action.SIM_APPLICATION_STATE_CHANGED");
            i.addFlags(16777216);
            i.addFlags(67108864);
            i.putExtra("android.telephony.extra.SIM_STATE", state);
            SubscriptionManager.putPhoneIdAndSubIdExtra(i, phoneId);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Broadcasting intent ACTION_SIM_APPLICATION_STATE_CHANGED ");
            stringBuilder.append(simStateString(state));
            stringBuilder.append(" for phone: ");
            stringBuilder.append(phoneId);
            logd(stringBuilder.toString());
            mContext.sendBroadcast(i, "android.permission.READ_PRIVILEGED_PHONE_STATE");
        }
    }

    private static String simStateString(int state) {
        switch (state) {
            case 0:
                return "UNKNOWN";
            case 1:
                return "ABSENT";
            case 2:
                return "PIN_REQUIRED";
            case 3:
                return "PUK_REQUIRED";
            case 4:
                return "NETWORK_LOCKED";
            case 5:
                return "READY";
            case 6:
                return "NOT_READY";
            case 7:
                return "PERM_DISABLED";
            case 8:
                return "CARD_IO_ERROR";
            case 9:
                return "CARD_RESTRICTED";
            case 10:
                return "LOADED";
            case 11:
                return "PRESENT";
            default:
                return "INVALID";
        }
    }

    private void logd(String message) {
        Rlog.d(LOG_TAG, message);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SubscriptionInfoUpdater:");
        this.mCarrierServiceBindHelper.dump(fd, pw, args);
    }

    public String[] getIccIdHw() {
        return mIccId;
    }

    public int[] getInsertSimStateHw() {
        return mInsertSimState;
    }

    public boolean isAllIccIdQueryDoneHw() {
        return isAllIccIdQueryDone();
    }

    public void updateSubscriptionInfoByIccIdHw() {
        updateSubscriptionInfoByIccId();
    }
}
