package com.android.internal.telephony.vsim;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HwVSimPhoneFactory;
import com.android.internal.telephony.vsim.HwVSimController.ProcessType;
import com.android.internal.telephony.vsim.HwVSimEventReport.VSimEventInfoUtils;
import com.android.internal.telephony.vsim.HwVSimModemAdapter.ExpectPara;
import com.android.internal.telephony.vsim.HwVSimModemAdapter.SimStateInfo;
import com.android.internal.telephony.vsim.HwVSimSlotSwitchController.CommrilMode;
import com.android.internal.telephony.vsim.process.HwVSimProcessor;
import java.util.Arrays;

public class HwVSimDualModem extends HwVSimModemAdapter {
    private static final String LOG_TAG = "HwVSimDualModem";
    private static final Object mLock = new Object();
    private static HwVSimDualModem sModem;

    public static HwVSimDualModem create(HwVSimController vsimController, Context context, CommandsInterface vsimCi, CommandsInterface[] cis) {
        HwVSimDualModem hwVSimDualModem;
        synchronized (mLock) {
            if (sModem == null) {
                sModem = new HwVSimDualModem(vsimController, context, vsimCi, cis);
                hwVSimDualModem = sModem;
            } else {
                throw new RuntimeException("VSimController already created");
            }
        }
        return hwVSimDualModem;
    }

    public static HwVSimDualModem getInstance() {
        HwVSimDualModem hwVSimDualModem;
        synchronized (mLock) {
            if (sModem != null) {
                hwVSimDualModem = sModem;
            } else {
                throw new RuntimeException("VSimController not yet created");
            }
        }
        return hwVSimDualModem;
    }

    private HwVSimDualModem(HwVSimController vsimController, Context context, CommandsInterface vsimCi, CommandsInterface[] cis) {
        super(vsimController, context, vsimCi, cis);
    }

    public void onGetSimSlotDone(HwVSimProcessor processor, AsyncResult ar) {
        if (ar == null || ar.userObj == null) {
            loge("onGetSimSlotDone, param is null !");
            return;
        }
        HwVSimRequest request = ar.userObj;
        int subId = request.mSubId;
        int mainSlot = 0;
        boolean isVSimOnM0 = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetSimSlotDone : subId = ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        if (ar.exception == null && ar.result != null && ((int[]) ar.result).length == 2) {
            StringBuilder stringBuilder2;
            int[] slots = ar.result;
            int[] responseSlots = new int[3];
            CommandsInterface ci = getCiBySub(2);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onGetSimSlotDone : result = ");
            stringBuilder3.append(Arrays.toString(slots));
            logd(stringBuilder3.toString());
            if (ci != null) {
                isVSimOnM0 = HwVSimUtilsInner.isRadioAvailable(2);
            }
            responseSlots[0] = slots[0];
            responseSlots[1] = slots[1];
            responseSlots[2] = 2;
            if (slots[0] == 0 && slots[1] == 1 && !isVSimOnM0) {
                mainSlot = 0;
            } else if (slots[0] == 1 && slots[1] == 0 && !isVSimOnM0) {
                mainSlot = 1;
            } else if (slots[0] == 2 && slots[1] == 1 && isVSimOnM0) {
                mainSlot = 0;
                responseSlots[2] = 0;
            } else if (slots[0] == 2 && slots[1] == 0 && isVSimOnM0) {
                mainSlot = 1;
                responseSlots[2] = 1;
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[2Cards]getSimSlot fail , setMainSlot = ");
                stringBuilder2.append(0);
                loge(stringBuilder2.toString());
            }
            setSimSlotTable(responseSlots);
            HwVSimPhoneFactory.setPropPersistRadioSimSlotCfg(slots);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onGetSimSlotDone : mainSlot = ");
            stringBuilder2.append(mainSlot);
            logd(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onGetSimSlotDone : isVSimOnM0 = ");
            stringBuilder2.append(isVSimOnM0);
            logd(stringBuilder2.toString());
            request.setMainSlot(mainSlot);
            request.setIsVSimOnM0(isVSimOnM0);
            request.setGotSimSlotMark(true);
            if (!isVSimOnM0) {
                HwVSimPhoneFactory.savePendingDeviceInfoToSP();
            }
        } else {
            processor.doProcessException(ar, request);
        }
    }

    public SimStateInfo onGetSimStateDone(HwVSimProcessor processor, AsyncResult ar) {
        int subId = ar.userObj.mSubId;
        int simIndex = 1;
        if (subId == 2) {
            simIndex = 11;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetSimStateDone : subId = ");
        stringBuilder.append(subId);
        stringBuilder.append(", simIndex = ");
        stringBuilder.append(simIndex);
        logd(stringBuilder.toString());
        int simEnable = 0;
        int simSub = 0;
        int simNetinfo = 0;
        if (ar.exception == null && ar.result != null && ((int[]) ar.result).length > 3) {
            simIndex = ((int[]) ar.result)[0];
            simEnable = ((int[]) ar.result)[1];
            simSub = ((int[]) ar.result)[2];
            simNetinfo = ((int[]) ar.result)[3];
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onGetSimStateDone : simIndex= ");
            stringBuilder2.append(simIndex);
            stringBuilder2.append(", simEnable= ");
            stringBuilder2.append(simEnable);
            stringBuilder2.append(", simSub= ");
            stringBuilder2.append(simSub);
            stringBuilder2.append(", simNetinfo= ");
            stringBuilder2.append(simNetinfo);
            logd(stringBuilder2.toString());
        }
        return new SimStateInfo(simIndex, simEnable, simSub, simNetinfo);
    }

    public void getAllCardTypes(HwVSimProcessor processor, HwVSimRequest request) {
        int subId = 0;
        while (subId < PHONE_COUNT) {
            if (getCiBySub(subId) == null || !HwVSimUtilsInner.isRadioAvailable(subId)) {
                request.setGotCardType(subId, true);
                int cardTypeBackup = HwVSimPhoneFactory.getUnReservedSubCardType();
                if (cardTypeBackup != -1) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getAllCardTypes: use backup cardtype ");
                    stringBuilder.append(cardTypeBackup);
                    stringBuilder.append(" instead of 0(NO_SIM)");
                    logd(stringBuilder.toString());
                    request.setCardType(subId, cardTypeBackup);
                } else {
                    request.setCardType(subId, 0);
                }
            } else {
                request.setGotCardType(subId, false);
                request.setCardType(subId, 0);
                getCardTypes(processor, request.clone(), subId);
            }
            subId++;
        }
    }

    public void onQueryCardTypeDone(HwVSimProcessor processor, AsyncResult ar) {
        if (processor == null || ar == null || ar.userObj == null) {
            loge("onQueryCardTypeDone, param is null !");
            return;
        }
        HwVSimRequest request = ar.userObj;
        int subId = request.mSubId;
        request.setCardType(subId, ((int[]) ar.result)[0] & 15);
        request.setGotCardType(subId, true);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onQueryCardTypeDone : subId = ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        if (request.isGotAllCardTypes()) {
            request.logCardTypes();
            this.mVSimController.updateCardTypes(request.getCardTypes());
        }
    }

    public void checkEnableSimCondition(HwVSimProcessor processor, HwVSimRequest request) {
        HwVSimRequest hwVSimRequest = request;
        if (processor == null || hwVSimRequest == null) {
            loge("checkEnableSimCondition, param is null !");
            return;
        }
        int[] cardTypes = request.getCardTypes();
        if (cardTypes == null) {
            loge("checkEnableSimCondition, cardTypes is null !");
        } else if (cardTypes.length == 0) {
            loge("checkEnableSimCondition, cardCount == 0 !");
        } else {
            ExpectPara expectPara;
            int insertedCardCount = HwVSimUtilsInner.getInsertedCardCount(cardTypes);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Enable: inserted card count = ");
            stringBuilder.append(insertedCardCount);
            logd(stringBuilder.toString());
            CommrilMode currentCommrilMode = getCommrilMode();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Enable: currentCommrilMode = ");
            stringBuilder.append(currentCommrilMode);
            logd(stringBuilder.toString());
            int mainSlot = request.getMainSlot();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Enable: mainSlot = ");
            stringBuilder.append(mainSlot);
            logd(stringBuilder.toString());
            int savedMainSlot = getVSimSavedMainSlot();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Enable: savedMainSlot = ");
            stringBuilder.append(savedMainSlot);
            logd(stringBuilder.toString());
            if (savedMainSlot == -1) {
                setVSimSavedMainSlot(mainSlot);
            }
            if (insertedCardCount == 0) {
                expectPara = getExpectParaCheckEnableNoSim(hwVSimRequest);
            } else if (insertedCardCount == 1) {
                expectPara = getExpectParaCheckEnableOneSim(hwVSimRequest);
            } else {
                int reservedSub = this.mVSimController.getUserReservedSubId();
                if (reservedSub == -1) {
                    reservedSub = mainSlot;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Enable: reserved sub not set, this time set to ");
                    stringBuilder.append(mainSlot);
                    logd(stringBuilder.toString());
                }
                expectPara = getExpectParaCheckEnableTwoSim(hwVSimRequest, reservedSub);
            }
            ExpectPara expectPara2 = expectPara;
            CommrilMode expectCommrilMode = expectPara2.getExpectCommrilMode();
            int expectSlot = expectPara2.getExpectSlot();
            setAlternativeUserReservedSubId(expectSlot);
            processAfterCheckEnableCondition(processor, hwVSimRequest, expectCommrilMode, expectSlot, currentCommrilMode);
        }
    }

    private ExpectPara getExpectParaCheckEnableNoSim(HwVSimRequest request) {
        ExpectPara expectPara = new ExpectPara();
        boolean isVSimOnM0 = request.getIsVSimOnM0();
        int mainSlot = request.getMainSlot();
        int slaveSlot = HwVSimUtilsInner.getAnotherSlotId(mainSlot);
        CommrilMode expectCommrilMode = CommrilMode.getCGMode();
        int expectSlot = isVSimOnM0 ? mainSlot : slaveSlot;
        expectPara.setExpectSlot(expectSlot);
        expectPara.setExpectCommrilMode(expectCommrilMode);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExpectParaCheckEnableNoSim expectCommrilMode = ");
        stringBuilder.append(expectCommrilMode);
        stringBuilder.append(" expectSlot = ");
        stringBuilder.append(expectSlot);
        logd(stringBuilder.toString());
        return expectPara;
    }

    private ExpectPara getExpectParaCheckEnableOneSim(HwVSimRequest request) {
        ExpectPara expectPara = new ExpectPara();
        boolean isVSimOnM0 = request.getIsVSimOnM0();
        int mainSlot = request.getMainSlot();
        int slaveSlot = HwVSimUtilsInner.getAnotherSlotId(mainSlot);
        CommrilMode expectCommrilMode = CommrilMode.getCGMode();
        int[] cardTypes = request.getCardTypes();
        int expectSlot = isVSimOnM0 ? mainSlot : slaveSlot;
        for (int i = 0; i < cardTypes.length; i++) {
            if (cardTypes[i] == 0) {
                expectSlot = i;
            }
        }
        expectPara.setExpectSlot(expectSlot);
        expectPara.setExpectCommrilMode(expectCommrilMode);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExpectParaCheckEnableOneSim expectCommrilMode = ");
        stringBuilder.append(expectCommrilMode);
        stringBuilder.append(" expectSlot = ");
        stringBuilder.append(expectSlot);
        logd(stringBuilder.toString());
        return expectPara;
    }

    private ExpectPara getExpectParaCheckEnableTwoSim(HwVSimRequest request, int reservedSub) {
        ExpectPara expectPara = new ExpectPara();
        int slotInM1 = reservedSub;
        int slotInM2 = HwVSimUtilsInner.getAnotherSlotId(slotInM1);
        if (this.mVSimController.getSubState(slotInM1) == 0 && this.mVSimController.getSubState(slotInM2) != 0) {
            logd("getExpectParaCheckEnableTwoSim, slot in m1 is inactive, so move to m2.");
            slotInM2 = slotInM1;
        }
        expectPara.setExpectSlot(slotInM2);
        CommrilMode expectCommrilMode = CommrilMode.HISI_CG_MODE;
        expectPara.setExpectCommrilMode(expectCommrilMode);
        int[] cardTypes = request.getCardTypes();
        if (slotInM2 >= 0 && slotInM2 < cardTypes.length) {
            HwVSimPhoneFactory.setUnReservedSubCardType(cardTypes[slotInM2]);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExpectParaCheckEnableTwoSim expectCommrilMode = ");
        stringBuilder.append(expectCommrilMode);
        stringBuilder.append(" expectSlot = ");
        stringBuilder.append(slotInM2);
        logd(stringBuilder.toString());
        return expectPara;
    }

    private void processAfterCheckEnableCondition(HwVSimProcessor processor, HwVSimRequest request, CommrilMode expectCommrilMode, int expectSlot, CommrilMode currentCommrilMode) {
        logd("Enable: processWhenCheckEnableCondition");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Enable: expectCommrilMode = ");
        stringBuilder.append(expectCommrilMode);
        stringBuilder.append(" expectSlot = ");
        stringBuilder.append(expectSlot);
        stringBuilder.append(" currentCommrilMode = ");
        stringBuilder.append(currentCommrilMode);
        logd(stringBuilder.toString());
        boolean isNeedSwitchCommrilMode = calcIsNeedSwitchCommrilMode(expectCommrilMode, currentCommrilMode);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Enable: isNeedSwitchCommrilMode = ");
        stringBuilder2.append(isNeedSwitchCommrilMode);
        logd(stringBuilder2.toString());
        if (isNeedSwitchCommrilMode) {
            request.setIsNeedSwitchCommrilMode(true);
            request.setExpectCommrilMode(expectCommrilMode);
        }
        request.setExpectSlot(expectSlot);
        int mainSlot = request.getMainSlot();
        boolean isVSimOnM0 = request.getIsVSimOnM0();
        if (expectSlot != mainSlot) {
            processor.setProcessType(ProcessType.PROCESS_TYPE_CROSS);
            VSimEventInfoUtils.setPocessType(this.mVSimController.mEventInfo, 2);
        } else if (!isVSimOnM0 || isNeedSwitchCommrilMode) {
            processor.setProcessType(ProcessType.PROCESS_TYPE_SWAP);
            VSimEventInfoUtils.setPocessType(this.mVSimController.mEventInfo, 1);
        } else {
            int[] subs = getSimSlotTable();
            if (subs.length == 0) {
                processor.doProcessException(null, request);
                return;
            }
            request.setSubs(subs);
            processor.setProcessType(ProcessType.PROCESS_TYPE_DIRECT);
            VSimEventInfoUtils.setPocessType(this.mVSimController.mEventInfo, 4);
        }
        processor.transitionToState(3);
    }

    public void checkDisableSimCondition(HwVSimProcessor processor, HwVSimRequest request) {
        if (request != null) {
            int[] cardTypes = request.getCardTypes();
            if (cardTypes != null && cardTypes.length != 0) {
                int insertedCardCount = HwVSimUtilsInner.getInsertedCardCount(cardTypes);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Disable: inserted card count = ");
                stringBuilder.append(insertedCardCount);
                logd(stringBuilder.toString());
                int savedMainSlot = getVSimSavedMainSlot();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Disable: savedMainSlot = ");
                stringBuilder2.append(savedMainSlot);
                logd(stringBuilder2.toString());
                CommrilMode currentCommrilMode = getCommrilMode();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Disable: currentCommrilMode = ");
                stringBuilder3.append(currentCommrilMode);
                logd(stringBuilder3.toString());
                int mainSlot = request.getMainSlot();
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Disable: mainSlot = ");
                stringBuilder4.append(mainSlot);
                logd(stringBuilder4.toString());
                int expectSlot = getExpectSlotForDisable(cardTypes, mainSlot, savedMainSlot);
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Disable: expectSlot = ");
                stringBuilder5.append(expectSlot);
                logd(stringBuilder5.toString());
                request.setExpectSlot(expectSlot);
                CommrilMode expectCommrilMode = getVSimOffCommrilMode(expectSlot, cardTypes);
                StringBuilder stringBuilder6 = new StringBuilder();
                stringBuilder6.append("Disable: expectCommrilMode = ");
                stringBuilder6.append(expectCommrilMode);
                logd(stringBuilder6.toString());
                boolean isNeedSwitchCommrilMode = calcIsNeedSwitchCommrilMode(expectCommrilMode, currentCommrilMode);
                StringBuilder stringBuilder7 = new StringBuilder();
                stringBuilder7.append("Disable: isNeedSwitchCommrilMode = ");
                stringBuilder7.append(isNeedSwitchCommrilMode);
                logd(stringBuilder7.toString());
                request.setIsNeedSwitchCommrilMode(isNeedSwitchCommrilMode);
                if (IS_FAST_SWITCH_SIMSLOT && isNeedSwitchCommrilMode) {
                    request.setExpectCommrilMode(expectCommrilMode);
                }
                if (processor.isReadyProcess()) {
                    HwVSimPhoneFactory.setIsVsimEnabledProp(false);
                    getIMSI(expectSlot);
                    processor.transitionToState(0);
                } else {
                    if (expectSlot == mainSlot) {
                        processor.setProcessType(ProcessType.PROCESS_TYPE_SWAP);
                    } else {
                        processor.setProcessType(ProcessType.PROCESS_TYPE_CROSS);
                    }
                    processor.transitionToState(6);
                }
            }
        }
    }

    public void radioPowerOff(HwVSimProcessor processor, HwVSimRequest request) {
        if (processor == null || request == null) {
            loge("radioPowerOff, param is null !");
            return;
        }
        request.createPowerOnOffMark();
        request.createGetSimStateMark();
        request.createCardOnOffMark();
        int subCount = request.getSubCount();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onEnter subCount = ");
        stringBuilder.append(subCount);
        logd(stringBuilder.toString());
        for (int i = 0; i < subCount; i++) {
            int subId = request.getSubIdByIndex(i);
            if (getCiBySub(subId) == null || !HwVSimUtilsInner.isRadioAvailable(subId)) {
                logd("[2cards]don't operate card in modem2.");
                request.setPowerOnOffMark(i, false);
                request.setSimStateMark(i, false);
                request.setCardOnOffMark(i, false);
                request.setGetIccCardStatusMark(i, false);
                ((GsmCdmaPhone) getPhoneBySub(subId)).getServiceStateTracker().setDesiredPowerState(false);
            } else {
                request.setPowerOnOffMark(i, true);
                request.setSimStateMark(i, true);
                request.setCardOnOffMark(i, true);
                request.setGetIccCardStatusMark(i, true);
                radioPowerOff(processor, request.clone(), subId);
            }
        }
    }

    public void onRadioPowerOffDone(HwVSimProcessor processor, AsyncResult ar) {
        if (processor == null || ar == null || ar.userObj == null) {
            loge("onRadioPowerOffDone, param is null !");
            return;
        }
        HwVSimRequest request = ar.userObj;
        int subId = request.mSubId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRadioPowerOffDone : subId = ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        int subCount = request.getSubCount();
        for (int i = 0; i < subCount; i++) {
            if (subId == request.getSubIdByIndex(i)) {
                request.setPowerOnOffMark(i, false);
                request.setSimStateMark(i, false);
            }
        }
        int simIndex = 1;
        if (subId == 2) {
            simIndex = 11;
        }
        cardPowerOff(processor, request, subId, simIndex);
    }

    public void onSwitchCommrilDone(HwVSimProcessor processor, AsyncResult ar) {
        HwVSimRequest request = ar.userObj;
        int subId = request.mSubId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSwitchCommrilDone : subId = ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        int mainSlot = request.getMainSlot();
        int expectSlot = request.getExpectSlot();
        if (!(mainSlot == expectSlot || expectSlot == -1)) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onSwitchCommrilDone : adjust mainSlot to ");
            stringBuilder2.append(expectSlot);
            logd(stringBuilder2.toString());
            request.setMainSlot(expectSlot);
        }
        processor.setProcessType(ProcessType.PROCESS_TYPE_SWAP);
    }

    public void switchSimSlot(HwVSimProcessor processor, HwVSimRequest request) {
        HwVSimProcessor hwVSimProcessor = processor;
        HwVSimRequest hwVSimRequest = request;
        if (hwVSimProcessor == null || hwVSimRequest == null) {
            loge("switchSimSlot, param is null !");
            return;
        }
        int modem2;
        int modem1;
        int modem0;
        int mainSlot = request.getMainSlot();
        int slaveSlot = HwVSimUtilsInner.getAnotherSlotId(mainSlot);
        int subId = mainSlot;
        boolean isSwap = processor.isSwapProcess();
        if (processor.isEnableProcess()) {
            logd("switchSimSlot, enable!");
            modem2 = request.getExpectSlot();
            modem1 = HwVSimUtilsInner.getAnotherSlotId(modem2);
            modem0 = 2;
            if (request.getIsVSimOnM0()) {
                subId = 2;
            }
        } else if (processor.isDisableProcess()) {
            logd("switchSimSlot, disable !");
            modem2 = request.getExpectSlot();
            if (isSwap) {
                modem1 = mainSlot;
                modem0 = slaveSlot;
            } else if (modem2 == 0) {
                modem1 = 0;
                modem0 = 1;
            } else {
                modem1 = 1;
                modem0 = 0;
            }
            subId = 2;
            modem2 = 2;
            int i = modem0;
            modem0 = modem1;
            modem1 = i;
        } else {
            hwVSimProcessor.doProcessException(null, hwVSimRequest);
            return;
        }
        int[] oldSlots = getSimSlotTable();
        int[] slots = createSimSlotsTable(modem0, modem1, modem2);
        hwVSimRequest.setSlots(slots);
        hwVSimRequest.mSubId = subId;
        boolean z = true;
        if (oldSlots.length == 3 && oldSlots[0] == slots[0] && oldSlots[1] == slots[1] && oldSlots[2] == slots[2]) {
            z = false;
        }
        boolean needSwich = z;
        Message onCompleted = hwVSimProcessor.obtainMessage(43, hwVSimRequest);
        if (needSwich) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("switchSimSlot subId ");
            stringBuilder.append(subId);
            stringBuilder.append(" modem0 = ");
            stringBuilder.append(modem0);
            stringBuilder.append(" modem1 = ");
            stringBuilder.append(modem1);
            stringBuilder.append(" modem2 = ");
            stringBuilder.append(modem2);
            logd(stringBuilder.toString());
            CommandsInterface ci = getCiBySub(subId);
            if (ci != null) {
                ci.hotSwitchSimSlot(modem0, modem1, modem2, onCompleted);
            }
        } else {
            logd("switchSimSlot return success");
            AsyncResult.forMessage(onCompleted, null, null);
            onCompleted.sendToTarget();
        }
    }

    public void onSwitchSlotDone(HwVSimProcessor processor, AsyncResult ar) {
        HwVSimRequest request = ar.userObj;
        int[] slots = request.getSlots();
        if (slots == null) {
            processor.doProcessException(null, request);
            return;
        }
        setSimSlotTable(slots);
        HwVSimPhoneFactory.setPropPersistRadioSimSlotCfg(slots);
    }

    public void setActiveModemMode(HwVSimProcessor processor, HwVSimRequest request, int subId) {
        request.mSubId = subId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setActiveModemMode, subId = ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        Message onCompleted = processor.obtainMessage(47, request);
        CommandsInterface ci = getCiBySub(subId);
        if (ci == null) {
            AsyncResult.forMessage(onCompleted);
            onCompleted.sendToTarget();
        } else if (processor.isEnableProcess()) {
            ci.setActiveModemMode(1, onCompleted);
        } else if (processor.isDisableProcess()) {
            if (request.getCardCount() == 1) {
                ci.setActiveModemMode(0, onCompleted);
            } else {
                ci.setActiveModemMode(1, onCompleted);
            }
        } else if (processor.isSwitchModeProcess()) {
            ci.setActiveModemMode(1, onCompleted);
        } else {
            AsyncResult.forMessage(onCompleted);
            onCompleted.sendToTarget();
        }
    }

    public int getPoffSubForEDWork(HwVSimRequest request) {
        if (request == null) {
            return -1;
        }
        request.mSubId = 2;
        return 2;
    }

    public void getModemSupportVSimVersion(HwVSimProcessor processor, int subId) {
        Message onCompleted = processor.obtainMessage(73, null);
        logd("start to get modem support vsim version.");
        onCompleted.sendToTarget();
    }

    public void onGetModemSupportVSimVersionDone(HwVSimProcessor processor, AsyncResult ar) {
        this.mVSimController.setModemVSimVersion(-2);
        logd("modem support vsim version is: -2");
    }

    public void getModemSupportVSimVersionInner(HwVSimProcessor processor, HwVSimRequest request) {
        Message onCompleted = processor.obtainMessage(30, request);
        logd("start to get modem support vsim version for inner.");
        AsyncResult.forMessage(onCompleted).exception = new CommandException(Error.REQUEST_NOT_SUPPORTED);
        onCompleted.sendToTarget();
    }

    public boolean isNeedRadioOnM2() {
        return false;
    }

    public void onSetNetworkRatAndSrvdomainDone(HwVSimProcessor processor, AsyncResult ar) {
    }

    public void doEnableStateEnter(HwVSimProcessor processor, HwVSimRequest request) {
    }

    public void doDisableStateExit(HwVSimProcessor processor, HwVSimRequest request) {
    }

    public void onEDWorkTransitionState(HwVSimProcessor processor) {
        if (processor != null) {
            processor.transitionToState(0);
        }
    }

    protected void logd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    protected void loge(String s) {
        HwVSimLog.VSimLogE(LOG_TAG, s);
    }

    private int[] createSimSlotsTable(int m0, int m1, int m2) {
        int[] slots = new int[MAX_SUB_COUNT];
        slots[0] = m0;
        slots[1] = m1;
        slots[2] = m2;
        return slots;
    }

    public void checkSwitchModeSimCondition(HwVSimProcessor processor, HwVSimRequest request) {
    }

    public void getSimState(HwVSimProcessor processor, HwVSimRequest request) {
    }

    private void setAlternativeUserReservedSubId(int expectSlot) {
        int slotInM1 = HwVSimUtilsInner.getAnotherSlotId(expectSlot);
        this.mVSimController.setAlternativeUserReservedSubId(slotInM1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAlternativeUserReservedSubId: set subId is ");
        stringBuilder.append(slotInM1);
        stringBuilder.append(".");
        logd(stringBuilder.toString());
    }

    private CommrilMode getVSimOffCommrilMode(int mainSlot, int[] cardTypes) {
        CommrilMode vSimOffCommrilMode;
        StringBuilder stringBuilder;
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        boolean mainSlotIsCDMACard = HwVSimSlotSwitchController.isCDMACard(cardTypes[mainSlot]);
        boolean slaveSlotIsCDMACard = HwVSimSlotSwitchController.isCDMACard(cardTypes[slaveSlot]);
        if (mainSlotIsCDMACard && slaveSlotIsCDMACard) {
            vSimOffCommrilMode = CommrilMode.HISI_CGUL_MODE;
        } else if (mainSlotIsCDMACard) {
            vSimOffCommrilMode = CommrilMode.HISI_CGUL_MODE;
        } else if (slaveSlotIsCDMACard) {
            vSimOffCommrilMode = CommrilMode.HISI_CG_MODE;
        } else {
            vSimOffCommrilMode = getCurrentCommrilMode();
            stringBuilder = new StringBuilder();
            stringBuilder.append("no c-card, not change commril mode. vSimOnCommrilMode = ");
            stringBuilder.append(vSimOffCommrilMode);
            logd(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("getVSimOffCommrilMode: mainSlot = ");
        stringBuilder.append(mainSlot);
        stringBuilder.append(", cardTypes = ");
        stringBuilder.append(Arrays.toString(cardTypes));
        stringBuilder.append(", mode = ");
        stringBuilder.append(vSimOffCommrilMode);
        logd(stringBuilder.toString());
        return vSimOffCommrilMode;
    }

    private CommrilMode getCurrentCommrilMode() {
        String mode = SystemProperties.get("persist.radio.commril_mode", "HISI_CGUL_MODE");
        CommrilMode result = CommrilMode.NON_MODE;
        try {
            return (CommrilMode) Enum.valueOf(CommrilMode.class, mode);
        } catch (IllegalArgumentException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCommrilMode, IllegalArgumentException, mode = ");
            stringBuilder.append(mode);
            logd(stringBuilder.toString());
            return result;
        }
    }

    public void onSimHotPlugOut() {
        HwVSimPhoneFactory.setUnReservedSubCardType(-1);
    }

    public void onRadioPowerOffSlaveModemDone(HwVSimProcessor processor, HwVSimRequest request) {
        if (processor.isEnableProcess()) {
            int slotIdInModem1 = request.mSubId;
            int slotIndex = slotIdInModem1 == 2 ? 11 : 1;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRadioPowerOffSlaveModemDone, slotIdInModem1 = ");
            stringBuilder.append(slotIdInModem1);
            logd(stringBuilder.toString());
            cardPowerOff(slotIdInModem1, slotIndex, null);
            return;
        }
        logd("onRadioPowerOffSlaveModemDone, do nothing.");
    }

    public void onCardPowerOffDoneInEWork(HwVSimProcessor processor, int subId) {
        if (processor.isEnableProcess() && processor.isWorkProcess()) {
            int unReservedSlotId = HwVSimUtilsInner.getAnotherSlotId(this.mVSimController.getUserReservedSubId());
            StringBuilder stringBuilder;
            if (subId == unReservedSlotId) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("onCardPowerOffDoneInEWork, will dispose card ");
                stringBuilder.append(subId);
                logd(stringBuilder.toString());
                this.mVSimController.disposeCard(unReservedSlotId);
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCardPowerOffDoneInEWork, not dispose card ");
            stringBuilder.append(subId);
            logd(stringBuilder.toString());
        }
    }

    public int getAllAbilityNetworkTypeOnModem1(boolean duallteCapOpened) {
        if (duallteCapOpened) {
            return 9;
        }
        return 3;
    }
}
