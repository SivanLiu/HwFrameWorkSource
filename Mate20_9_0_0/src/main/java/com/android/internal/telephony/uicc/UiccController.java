package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RadioConfig;
import com.android.internal.telephony.SubscriptionInfoUpdater;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccSlotStatus.SlotState;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class UiccController extends AbstractUiccController {
    public static final int APP_FAM_3GPP = 1;
    public static final int APP_FAM_3GPP2 = 2;
    public static final int APP_FAM_IMS = 3;
    private static final boolean DBG = true;
    private static final int EVENT_BALONG_MODEM_RESET = 9;
    private static final int EVENT_GET_ICC_STATUS_DONE = 3;
    private static final int EVENT_GET_SLOT_STATUS_DONE = 4;
    private static final int EVENT_ICC_STATUS_CHANGED = 1;
    private static final int EVENT_RADIO_AVAILABLE = 6;
    private static final int EVENT_RADIO_ON = 5;
    private static final int EVENT_RADIO_UNAVAILABLE = 7;
    private static final int EVENT_SIM_REFRESH = 8;
    private static final int EVENT_SLOT_STATUS_CHANGED = 2;
    public static final int INVALID_SLOT_ID = -1;
    public static final boolean IS_QUICK_BROADCAST_STATUS = SystemProperties.getBoolean("ro.quick_broadcast_cardstatus", false);
    private static final String LOG_TAG = "UiccController";
    private static final boolean VDBG = false;
    private static UiccController mInstance;
    private static UiccStateChangedLauncher mLauncher;
    private static final Object mLock = new Object();
    private static ArrayList<IccSlotStatus> sLastSlotStatus;
    static LocalLog sLocalLog = new LocalLog(100);
    private CommandsInterface[] mCis;
    private Context mContext;
    protected RegistrantList mIccChangedRegistrants = new RegistrantList();
    private boolean mIsSlotStatusSupported = true;
    private int[] mPhoneIdToSlotId;
    private RadioConfig mRadioConfig;
    private UiccSlot[] mUiccSlots;

    public static UiccController make(Context c, CommandsInterface[] ci) {
        UiccController uiccController;
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new UiccController(c, ci);
                HwTelephonyFactory.getHwUiccManager().initHwDsdsController(c, ci);
                HwTelephonyFactory.getHwUiccManager().initHwAllInOneController(c, ci);
                HwTelephonyFactory.getHwUiccManager().initHwCarrierConfigCardManager(c);
                mLauncher = new UiccStateChangedLauncher(c, mInstance);
                uiccController = mInstance;
            } else {
                throw new RuntimeException("UiccController.make() should only be called once");
            }
        }
        return uiccController;
    }

    private UiccController(Context c, CommandsInterface[] ci) {
        log("Creating UiccController");
        this.mContext = c;
        this.mCis = ci;
        String logStr = new StringBuilder();
        logStr.append("config_num_physical_slots = ");
        logStr.append(c.getResources().getInteger(17694847));
        logStr = logStr.toString();
        log(logStr);
        sLocalLog.log(logStr);
        int numPhysicalSlots = c.getResources().getInteger(17694847);
        if (numPhysicalSlots < this.mCis.length) {
            numPhysicalSlots = this.mCis.length;
        }
        this.mUiccSlots = new UiccSlot[numPhysicalSlots];
        this.mPhoneIdToSlotId = new int[ci.length];
        Arrays.fill(this.mPhoneIdToSlotId, -1);
        this.mRadioConfig = RadioConfig.getInstance(this.mContext);
        this.mRadioConfig.registerForSimSlotStatusChanged(this, 2, null);
        for (int i = 0; i < this.mCis.length; i++) {
            this.mCis[i].registerForIccStatusChanged(this, 1, Integer.valueOf(i));
            if (HwModemCapability.isCapabilitySupport(9)) {
                this.mCis[i].registerForOn(this, 1, Integer.valueOf(i));
            }
            this.mCis[i].registerForAvailable(this, 1, Integer.valueOf(i));
            this.mCis[i].registerForNotAvailable(this, 7, Integer.valueOf(i));
            this.mCis[i].registerForIccRefresh(this, 8, Integer.valueOf(i));
            if (IS_QUICK_BROADCAST_STATUS) {
                this.mCis[i].registerForIccidChanged(this, 1, Integer.valueOf(i));
            }
            this.mCis[i].registerForUnsolBalongModemReset(this, 9, Integer.valueOf(i));
            UiccProfile.broadcastIccStateChangedIntent("NOT_READY", null, i);
        }
    }

    private int getSlotIdFromPhoneId(int phoneId) {
        return this.mPhoneIdToSlotId[phoneId];
    }

    public static UiccController getInstance() {
        UiccController uiccController;
        synchronized (mLock) {
            if (mInstance != null) {
                uiccController = mInstance;
            } else {
                throw new RuntimeException("UiccController.getInstance can't be called before make()");
            }
        }
        return uiccController;
    }

    public UiccCard getUiccCard(int phoneId) {
        UiccCard uiccCardForPhone;
        synchronized (mLock) {
            uiccCardForPhone = getUiccCardForPhone(phoneId);
        }
        return uiccCardForPhone;
    }

    public UiccCard getUiccCardForSlot(int slotId) {
        synchronized (mLock) {
            UiccSlot uiccSlot = getUiccSlot(slotId);
            if (uiccSlot != null) {
                UiccCard uiccCard = uiccSlot.getUiccCard();
                return uiccCard;
            }
            return null;
        }
    }

    public UiccCard getUiccCardForPhone(int phoneId) {
        synchronized (mLock) {
            if (isValidPhoneIndex(phoneId)) {
                UiccSlot uiccSlot = getUiccSlotForPhone(phoneId);
                if (uiccSlot != null) {
                    UiccCard uiccCard = uiccSlot.getUiccCard();
                    return uiccCard;
                }
            }
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0016, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public UiccProfile getUiccProfileForPhone(int phoneId) {
        synchronized (mLock) {
            UiccProfile uiccProfile = null;
            if (isValidPhoneIndex(phoneId)) {
                UiccCard uiccCard = getUiccCardForPhone(phoneId);
                if (uiccCard != null) {
                    uiccProfile = uiccCard.getUiccProfile();
                }
            } else {
                return null;
            }
        }
    }

    public UiccSlot[] getUiccSlots() {
        UiccSlot[] uiccSlotArr;
        synchronized (mLock) {
            uiccSlotArr = this.mUiccSlots;
        }
        return uiccSlotArr;
    }

    public void switchSlots(int[] physicalSlots, Message response) {
        this.mRadioConfig.setSimSlotsMapping(physicalSlots, response);
    }

    public UiccSlot getUiccSlot(int slotId) {
        synchronized (mLock) {
            if (isValidSlotIndex(slotId)) {
                UiccSlot uiccSlot = this.mUiccSlots[slotId];
                return uiccSlot;
            }
            return null;
        }
    }

    public UiccSlot getUiccSlotForPhone(int phoneId) {
        synchronized (mLock) {
            if (isValidPhoneIndex(phoneId)) {
                int slotId = getSlotIdFromPhoneId(phoneId);
                if (isValidSlotIndex(slotId)) {
                    UiccSlot uiccSlot = this.mUiccSlots[slotId];
                    return uiccSlot;
                }
            }
            return null;
        }
    }

    public int getUiccSlotForCardId(String cardId) {
        synchronized (mLock) {
            int idx = 0;
            for (int idx2 = 0; idx2 < this.mUiccSlots.length; idx2++) {
                if (this.mUiccSlots[idx2] != null) {
                    UiccCard uiccCard = this.mUiccSlots[idx2].getUiccCard();
                    if (uiccCard != null && cardId.equals(uiccCard.getCardId())) {
                        return idx2;
                    }
                }
            }
            while (idx < this.mUiccSlots.length) {
                if (this.mUiccSlots[idx] == null || !cardId.equals(this.mUiccSlots[idx].getIccId())) {
                    idx++;
                } else {
                    return idx;
                }
            }
            return -1;
        }
    }

    public IccRecords getIccRecords(int phoneId, int family) {
        synchronized (mLock) {
            UiccCardApplication app = getUiccCardApplication(phoneId, family);
            if (app != null) {
                IccRecords iccRecords = app.getIccRecords();
                return iccRecords;
            }
            return null;
        }
    }

    public CommandsInterface[] getmCis() {
        return (CommandsInterface[]) this.mCis.clone();
    }

    public IccFileHandler getIccFileHandler(int phoneId, int family) {
        synchronized (mLock) {
            UiccCardApplication app = getUiccCardApplication(phoneId, family);
            if (app != null) {
                IccFileHandler iccFileHandler = app.getIccFileHandler();
                return iccFileHandler;
            }
            return null;
        }
    }

    public void registerForIccChanged(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mIccChangedRegistrants.add(r);
            r.notifyRegistrant();
        }
        HwTelephonyFactory.getHwUiccManager().registerForIccChanged(h, what, obj);
    }

    public void unregisterForIccChanged(Handler h) {
        synchronized (mLock) {
            this.mIccChangedRegistrants.remove(h);
        }
        HwTelephonyFactory.getHwUiccManager().unregisterForIccChanged(h);
    }

    /* JADX WARNING: Missing block: B:32:0x011e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message msg) {
        synchronized (mLock) {
            StringBuilder stringBuilder;
            Integer phoneId = getCiIndex(msg);
            if (phoneId.intValue() >= 0) {
                if (phoneId.intValue() < this.mCis.length) {
                    LocalLog localLog = sLocalLog;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("handleMessage: Received ");
                    stringBuilder.append(msg.what);
                    stringBuilder.append(" for phoneId ");
                    stringBuilder.append(phoneId);
                    localLog.log(stringBuilder.toString());
                    AsyncResult ar = msg.obj;
                    switch (msg.what) {
                        case 1:
                            log("Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus");
                            this.mCis[phoneId.intValue()].getIccCardStatus(obtainMessage(3, phoneId));
                            break;
                        case 2:
                        case 4:
                            log("Received EVENT_SLOT_STATUS_CHANGED or EVENT_GET_SLOT_STATUS_DONE");
                            onGetSlotStatusDone(ar);
                            break;
                        case 3:
                            log("Received EVENT_GET_ICC_STATUS_DONE");
                            onGetIccCardStatusDone(ar, phoneId);
                            HwTelephonyFactory.getHwPhoneManager().saveUiccCardsToVirtualNet(getUiccCards());
                            break;
                        case 5:
                        case 6:
                            log("Received EVENT_RADIO_AVAILABLE/EVENT_RADIO_ON, calling getIccCardStatus");
                            this.mCis[phoneId.intValue()].getIccCardStatus(obtainMessage(3, phoneId));
                            if (phoneId.intValue() == 0) {
                                log("Received EVENT_RADIO_AVAILABLE/EVENT_RADIO_ON for phoneId 0, calling getIccSlotsStatus");
                                this.mRadioConfig.getSimSlotsStatus(obtainMessage(4, phoneId));
                                break;
                            }
                            break;
                        case 7:
                            log("EVENT_RADIO_UNAVAILABLE, dispose card");
                            UiccSlot uiccSlot = getUiccSlotForPhone(phoneId.intValue());
                            if (uiccSlot != null) {
                                uiccSlot.onRadioStateUnavailable();
                            }
                            this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult(null, phoneId, null));
                            HwTelephonyFactory.getHwPhoneManager().saveUiccCardsToVirtualNet(getUiccCards());
                            break;
                        case 8:
                            log("Received EVENT_SIM_REFRESH");
                            onSimRefresh(ar, phoneId);
                            break;
                        case 9:
                            log("EVENT_BALONG_MODEM_RESET, dispose all cards.");
                            for (int card_index = 0; card_index < this.mCis.length; card_index++) {
                                UiccSlot uiccSlot2 = getUiccSlotForPhone(card_index);
                                if (uiccSlot2 != null) {
                                    uiccSlot2.onRadioStateUnavailable();
                                }
                                this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult(null, Integer.valueOf(card_index), null));
                            }
                            HwTelephonyFactory.getHwPhoneManager().saveUiccCardsToVirtualNet(getUiccCards());
                            break;
                        default:
                            String str = LOG_TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(" Unknown Event ");
                            stringBuilder2.append(msg.what);
                            Rlog.e(str, stringBuilder2.toString());
                            break;
                    }
                }
            }
            String str2 = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid phoneId : ");
            stringBuilder.append(phoneId);
            stringBuilder.append(" received with event ");
            stringBuilder.append(msg.what);
            Rlog.e(str2, stringBuilder.toString());
        }
    }

    private Integer getCiIndex(Message msg) {
        Integer index = new Integer(0);
        if (msg == null) {
            return index;
        }
        if (msg.obj != null && (msg.obj instanceof Integer)) {
            return msg.obj;
        }
        if (msg.obj == null || !(msg.obj instanceof AsyncResult)) {
            return index;
        }
        AsyncResult ar = msg.obj;
        if (ar.userObj == null || !(ar.userObj instanceof Integer)) {
            return index;
        }
        return ar.userObj;
    }

    public UiccCardApplication getUiccCardApplication(int phoneId, int family) {
        synchronized (mLock) {
            UiccCard uiccCard = getUiccCardForPhone(phoneId);
            if (uiccCard != null) {
                UiccCardApplication application = uiccCard.getApplication(family);
                return application;
            }
            return null;
        }
    }

    static void updateInternalIccState(String value, String reason, int phoneId) {
        SubscriptionInfoUpdater subInfoUpdator = PhoneFactory.getSubscriptionInfoUpdater();
        if (subInfoUpdator != null) {
            subInfoUpdator.updateInternalIccState(value, reason, phoneId);
        } else {
            Rlog.e(LOG_TAG, "subInfoUpdate is null.");
        }
    }

    private synchronized void onGetIccCardStatusDone(AsyncResult ar, Integer index) {
        if (ar.exception != null) {
            Rlog.e(LOG_TAG, "Error getting ICC status. RIL_REQUEST_GET_ICC_STATUS should never return an error", ar.exception);
        } else if (isValidPhoneIndex(index.intValue())) {
            IccCardStatus status = ar.result;
            LocalLog localLog = sLocalLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onGetIccCardStatusDone: phoneId ");
            stringBuilder.append(index);
            stringBuilder.append(" IccCardStatus: ");
            stringBuilder.append(status);
            localLog.log(stringBuilder.toString());
            int slotId = status.physicalSlotIndex;
            if (slotId == -1) {
                slotId = index.intValue();
            }
            this.mPhoneIdToSlotId[index.intValue()] = slotId;
            if (status.mCardState == CardState.CARDSTATE_ABSENT) {
                TelephonyManager.getDefault();
                TelephonyManager.setTelephonyProperty(index.intValue(), IccRecords.PROPERTY_MCC_MATCHING_FYROM, "");
            }
            if (this.mUiccSlots[slotId] == null) {
                if (RadioState.RADIO_UNAVAILABLE == this.mCis[index.intValue()].getRadioState()) {
                    Rlog.e(LOG_TAG, "Current RadioState is RADIO_UNAVAILABLE,return immediatly");
                    return;
                } else {
                    this.mUiccSlots[slotId] = new UiccSlot(this.mContext, true);
                    HwTelephonyFactory.getHwUiccManager().initUiccCard(this.mUiccSlots[slotId], status, index);
                }
            }
            this.mUiccSlots[slotId].update(this.mCis[index.intValue()], status, index.intValue());
            HwTelephonyFactory.getHwUiccManager().updateUiccCard(getUiccCardForPhone(index.intValue()), status, index);
            HwTelephonyFactory.getHwUiccManager().onGetIccStatusDone(ar, index);
            if (HwTelephonyFactory.getHwUiccManager().uiccHwdsdsNeedSetActiveMode()) {
                log("onGetIccCardStatusDone: uiccHwdsdsWatingActiveMode ");
                return;
            }
            log("Notifying IccChangedRegistrants");
            this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult(null, index, null));
            processRadioPowerDownIfNoCard(getUiccCards());
        } else {
            String str = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onGetIccCardStatusDone: invalid index : ");
            stringBuilder2.append(index);
            Rlog.e(str, stringBuilder2.toString());
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0048, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void onGetSlotStatusDone(AsyncResult ar) {
        if (this.mIsSlotStatusSupported) {
            Throwable e = ar.exception;
            int i = 0;
            if (e != null) {
                String logStr;
                if (e instanceof CommandException) {
                    if (((CommandException) e).getCommandError() == Error.REQUEST_NOT_SUPPORTED) {
                        logStr = "onGetSlotStatusDone: request not supported; marking mIsSlotStatusSupported to false";
                        log(logStr);
                        sLocalLog.log(logStr);
                        this.mIsSlotStatusSupported = false;
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected error getting slot status: ");
                stringBuilder.append(ar.exception);
                logStr = stringBuilder.toString();
                Rlog.e(LOG_TAG, logStr);
                sLocalLog.log(logStr);
            } else {
                ArrayList<IccSlotStatus> status = ar.result;
                if (slotStatusChanged(status)) {
                    sLastSlotStatus = status;
                    int numActiveSlots = 0;
                    for (int i2 = 0; i2 < status.size(); i2++) {
                        IccSlotStatus iss = (IccSlotStatus) status.get(i2);
                        boolean isActive = iss.slotState == SlotState.SLOTSTATE_ACTIVE;
                        if (isActive) {
                            numActiveSlots++;
                            if (isValidPhoneIndex(iss.logicalSlotIndex)) {
                                this.mPhoneIdToSlotId[iss.logicalSlotIndex] = i2;
                            } else {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Logical slot index ");
                                stringBuilder2.append(iss.logicalSlotIndex);
                                stringBuilder2.append(" invalid for physical slot ");
                                stringBuilder2.append(i2);
                                throw new RuntimeException(stringBuilder2.toString());
                            }
                        }
                        if (this.mUiccSlots[i2] == null) {
                            this.mUiccSlots[i2] = new UiccSlot(this.mContext, isActive);
                        }
                        this.mUiccSlots[i2].update(isActive ? this.mCis[iss.logicalSlotIndex] : null, iss);
                    }
                    if (numActiveSlots == this.mPhoneIdToSlotId.length) {
                        Set<Integer> slotIds = new HashSet();
                        int[] iArr = this.mPhoneIdToSlotId;
                        int length = iArr.length;
                        while (i < length) {
                            int slotId = iArr[i];
                            if (slotIds.contains(Integer.valueOf(slotId))) {
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("slotId ");
                                stringBuilder3.append(slotId);
                                stringBuilder3.append(" mapped to multiple phoneIds");
                                throw new RuntimeException(stringBuilder3.toString());
                            }
                            slotIds.add(Integer.valueOf(slotId));
                            i++;
                        }
                        Intent intent = new Intent("android.telephony.action.SIM_SLOT_STATUS_CHANGED");
                        intent.addFlags(67108864);
                        intent.addFlags(16777216);
                        this.mContext.sendBroadcast(intent, "android.permission.READ_PRIVILEGED_PHONE_STATE");
                        return;
                    }
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Number of active slots ");
                    stringBuilder4.append(numActiveSlots);
                    stringBuilder4.append(" does not match the expected value ");
                    stringBuilder4.append(this.mPhoneIdToSlotId.length);
                    throw new RuntimeException(stringBuilder4.toString());
                }
                log("onGetSlotStatusDone: No change in slot status");
            }
        }
    }

    private boolean slotStatusChanged(ArrayList<IccSlotStatus> slotStatusList) {
        if (sLastSlotStatus == null || sLastSlotStatus.size() != slotStatusList.size()) {
            return true;
        }
        Iterator it = slotStatusList.iterator();
        while (it.hasNext()) {
            if (!sLastSlotStatus.contains((IccSlotStatus) it.next())) {
                return true;
            }
        }
        return false;
    }

    private void logPhoneIdToSlotIdMapping() {
        log("mPhoneIdToSlotId mapping:");
        for (int i = 0; i < this.mPhoneIdToSlotId.length; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    phoneId ");
            stringBuilder.append(i);
            stringBuilder.append(" slotId ");
            stringBuilder.append(this.mPhoneIdToSlotId[i]);
            log(stringBuilder.toString());
        }
    }

    private void onSimRefresh(AsyncResult ar, Integer index) {
        String str;
        StringBuilder stringBuilder;
        if (ar.exception != null) {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onSimRefresh: Sim REFRESH with exception: ");
            stringBuilder.append(ar.exception);
            Rlog.e(str, stringBuilder.toString());
        } else if (isValidPhoneIndex(index.intValue())) {
            IccRefreshResponse resp = ar.result;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onSimRefresh: ");
            stringBuilder.append(resp);
            log(stringBuilder.toString());
            LocalLog localLog = sLocalLog;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onSimRefresh: ");
            stringBuilder2.append(resp);
            localLog.log(stringBuilder2.toString());
            if (resp == null) {
                Rlog.e(LOG_TAG, "onSimRefresh: received without input");
                return;
            }
            UiccCard uiccCard = getUiccCardForPhone(index.intValue());
            if (uiccCard == null) {
                String str2 = LOG_TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("onSimRefresh: refresh on null card : ");
                stringBuilder3.append(index);
                Rlog.e(str2, stringBuilder3.toString());
                return;
            }
            boolean changed;
            switch (resp.refreshResult) {
                case 0:
                    if (IccRecords.EFID_SET.contains(Integer.valueOf(resp.efId))) {
                        log("FIEL_UPDATE resetAppWithAid");
                        changed = uiccCard.resetAppWithAid(resp.aid);
                        break;
                    }
                    return;
                case 1:
                case 2:
                    changed = uiccCard.resetAppWithAid(resp.aid);
                    break;
                default:
                    return;
            }
            if (changed && resp.refreshResult == 2) {
                ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).updateConfigForPhoneId(index.intValue(), "UNKNOWN");
                if (this.mContext.getResources().getBoolean(17957007)) {
                    this.mCis[index.intValue()].setRadioPower(false, null);
                }
            }
            this.mCis[index.intValue()].getIccCardStatus(obtainMessage(3, index));
        } else {
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onSimRefresh: invalid index : ");
            stringBuilder.append(index);
            Rlog.e(str, stringBuilder.toString());
        }
    }

    private boolean isValidPhoneIndex(int index) {
        return index >= 0 && index < TelephonyManager.getDefault().getPhoneCount();
    }

    private boolean isValidSlotIndex(int index) {
        return index >= 0 && index < this.mUiccSlots.length;
    }

    public void disposeCard(int index) {
        synchronized (mLock) {
            UiccSlot uiccSlot = getUiccSlotForPhone(index);
            if (uiccSlot != null) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Disposing card ");
                stringBuilder.append(index);
                Rlog.d(str, stringBuilder.toString());
                uiccSlot.onRadioStateUnavailable();
                HwTelephonyFactory.getHwPhoneManager().saveUiccCardsToVirtualNet(getUiccCards());
            }
        }
    }

    public void onRefresh(int slotId, int[] fileList) {
        boolean fileChanged = fileList != null && fileList.length > 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRefresh: fileChanged = ");
        stringBuilder.append(fileChanged);
        stringBuilder.append("  slotId = ");
        stringBuilder.append(slotId);
        log(stringBuilder.toString());
        if (fileChanged && isValidPhoneIndex(slotId) && getUiccCardForPhone(slotId) != null && getUiccCardForPhone(slotId).getApplicationIndex(slotId) != null) {
            IccRecords iccRecords = getUiccCardForPhone(slotId).getApplicationIndex(slotId).getIccRecords();
            if (iccRecords != null) {
                iccRecords.onRefresh(fileChanged, fileList);
            }
        }
    }

    private void log(String string) {
        Rlog.d(LOG_TAG, string);
    }

    public void addCardLog(String data) {
        sLocalLog.log(data);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UiccController: ");
        stringBuilder.append(this);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mContext=");
        stringBuilder.append(this.mContext);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mInstance=");
        stringBuilder.append(mInstance);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mIccChangedRegistrants: size=");
        stringBuilder.append(this.mIccChangedRegistrants.size());
        pw.println(stringBuilder.toString());
        int i = 0;
        for (int i2 = 0; i2 < this.mIccChangedRegistrants.size(); i2++) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mIccChangedRegistrants[");
            stringBuilder2.append(i2);
            stringBuilder2.append("]=");
            stringBuilder2.append(((Registrant) this.mIccChangedRegistrants.get(i2)).getHandler());
            pw.println(stringBuilder2.toString());
        }
        pw.println();
        pw.flush();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" mUiccSlots: size=");
        stringBuilder3.append(this.mUiccSlots.length);
        pw.println(stringBuilder3.toString());
        while (i < this.mUiccSlots.length) {
            if (this.mUiccSlots[i] == null) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("  mUiccSlots[");
                stringBuilder3.append(i);
                stringBuilder3.append("]=null");
                pw.println(stringBuilder3.toString());
            } else {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("  mUiccSlots[");
                stringBuilder3.append(i);
                stringBuilder3.append("]=");
                stringBuilder3.append(this.mUiccSlots[i]);
                pw.println(stringBuilder3.toString());
                this.mUiccSlots[i].dump(fd, pw, args);
            }
            i++;
        }
        pw.println(" sLocalLog= ");
        sLocalLog.dump(fd, pw, args);
    }

    public UiccCard[] getUiccCards() {
        if (getUiccSlots() == null) {
            log("haven't get all UiccCards done, please wait!");
            return null;
        }
        UiccSlot[] us = getUiccSlots();
        UiccCard[] uc = new UiccCard[us.length];
        for (int i = 0; i < us.length; i++) {
            if (us[i] != null) {
                uc[i] = us[i].getUiccCard();
            }
        }
        return uc;
    }
}
