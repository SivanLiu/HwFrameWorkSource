package com.android.internal.telephony.vsim;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HwVSimPhoneFactory;
import com.android.internal.telephony.vsim.HwVSimController.EnableParam;
import com.android.internal.telephony.vsim.HwVSimController.ProcessType;
import com.android.internal.telephony.vsim.HwVSimController.WorkModeParam;
import com.android.internal.telephony.vsim.HwVSimEventReport.VSimEventInfoUtils;
import com.android.internal.telephony.vsim.HwVSimModemAdapter.ExpectPara;
import com.android.internal.telephony.vsim.HwVSimModemAdapter.SimStateInfo;
import com.android.internal.telephony.vsim.HwVSimSlotSwitchController.CommrilMode;
import com.android.internal.telephony.vsim.process.HwVSimProcessor;
import java.util.Arrays;

public class HwVSimTriModem extends HwVSimModemAdapter {
    private static final String LOG_TAG = "HwVSimTriModem";
    private static final Object mLock = new Object();
    private static HwVSimTriModem sModem;

    public static HwVSimTriModem create(HwVSimController vsimController, Context context, CommandsInterface vsimCi, CommandsInterface[] cis) {
        HwVSimTriModem hwVSimTriModem;
        synchronized (mLock) {
            if (sModem == null) {
                sModem = new HwVSimTriModem(vsimController, context, vsimCi, cis);
                hwVSimTriModem = sModem;
            } else {
                throw new RuntimeException("TriModemController already created");
            }
        }
        return hwVSimTriModem;
    }

    public static HwVSimTriModem getInstance() {
        HwVSimTriModem hwVSimTriModem;
        synchronized (mLock) {
            if (sModem != null) {
                hwVSimTriModem = sModem;
            } else {
                throw new RuntimeException("TriModemController not yet created");
            }
        }
        return hwVSimTriModem;
    }

    private HwVSimTriModem(HwVSimController vsimController, Context context, CommandsInterface vsimCi, CommandsInterface[] cis) {
        super(vsimController, context, vsimCi, cis);
    }

    protected void logd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    public void onRadioPowerOffDone(HwVSimProcessor processor, AsyncResult ar) {
        if (processor != null && ar != null) {
            HwVSimRequest request = ar.userObj;
            if (request != null) {
                int subId = request.mSubId;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onRadioPowerOffDone : subId = ");
                stringBuilder.append(subId);
                logd(stringBuilder.toString());
                int subCount = request.getSubCount();
                for (int i = 0; i < subCount; i++) {
                    if (subId == request.getSubIdByIndex(i)) {
                        request.setPowerOnOffMark(i, false);
                    }
                }
                getSimState(processor, request, subId);
            }
        }
    }

    public void onGetSimSlotDone(HwVSimProcessor processor, AsyncResult ar) {
        HwVSimRequest request = ar.userObj;
        int subId = request.mSubId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetSimSlotDone : subId = ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        if (ar.exception == null && ar.result != null && ((int[]) ar.result).length == 3) {
            int mainSlot;
            boolean isVSimOnM0;
            int[] slots = ar.result;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onGetSimSlotDone : result = ");
            stringBuilder2.append(Arrays.toString(slots));
            logd(stringBuilder2.toString());
            setSimSlotTable(slots);
            if (slots[0] == 0 && slots[1] == 1 && slots[2] == 2) {
                mainSlot = 0;
                isVSimOnM0 = false;
            } else if (slots[0] == 1 && slots[1] == 0 && slots[2] == 2) {
                mainSlot = 1;
                isVSimOnM0 = false;
            } else if (slots[0] == 2 && slots[1] == 1 && slots[2] == 0) {
                mainSlot = 0;
                isVSimOnM0 = true;
            } else if (slots[0] == 2 && slots[1] == 0 && slots[2] == 1) {
                mainSlot = 1;
                isVSimOnM0 = true;
            } else {
                isVSimOnM0 = false;
                mainSlot = request.getMainSlot();
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onGetSimSlotDone : mainSlot = ");
            stringBuilder3.append(mainSlot);
            logd(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onGetSimSlotDone : isVSimOnM0 = ");
            stringBuilder3.append(isVSimOnM0);
            logd(stringBuilder3.toString());
            HwVSimPhoneFactory.setPropPersistRadioSimSlotCfg(slots);
            request.setMainSlot(mainSlot);
            request.setIsVSimOnM0(isVSimOnM0);
            request.setGotSimSlotMark(true);
            return;
        }
        processor.doProcessException(ar, request);
    }

    public SimStateInfo onGetSimStateDone(HwVSimProcessor processor, AsyncResult ar) {
        int subId = ar.userObj.mSubId;
        int simIndex = 1;
        if (subId == 2) {
            simIndex = 11;
        }
        int simEnable = 0;
        int simSub = 0;
        int simNetinfo = 0;
        if (ar.exception == null && ar.result != null && ((int[]) ar.result).length > 3) {
            simIndex = ((int[]) ar.result)[0];
            simEnable = ((int[]) ar.result)[1];
            simSub = ((int[]) ar.result)[2];
            simNetinfo = ((int[]) ar.result)[3];
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetSimStateDone : subId = ");
        stringBuilder.append(subId);
        stringBuilder.append(", simIndex = ");
        stringBuilder.append(simIndex);
        stringBuilder.append(", simEnable = ");
        stringBuilder.append(simEnable);
        stringBuilder.append(", simNetinfo = ");
        stringBuilder.append(simNetinfo);
        logd(stringBuilder.toString());
        return new SimStateInfo(simIndex, simEnable, simSub, simNetinfo);
    }

    public void onQueryCardTypeDone(HwVSimProcessor processor, AsyncResult ar) {
        HwVSimRequest request = ar.userObj;
        int subId = request.mSubId;
        request.setCardType(subId, ((int[]) ar.result)[0] & 15);
        request.setGotCardType(subId, true);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onQueryCardTypeDone : subId = ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        if (isGotAllCardTypes(request)) {
            request.logCardTypes();
            this.mVSimController.updateCardTypes(request.getCardTypes());
        }
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
        VSimEventInfoUtils.setPocessType(this.mVSimController.mEventInfo, 1);
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

    public void getModemSupportVSimVersion(HwVSimProcessor processor, int subId) {
        Message onCompleted = processor.obtainMessage(73, null);
        logd("start to get modem support vsim version.");
        CommandsInterface ci = getCiBySub(subId);
        if (ci != null) {
            ci.getModemSupportVSimVersion(onCompleted);
        }
    }

    public void onGetModemSupportVSimVersionDone(HwVSimProcessor processor, AsyncResult ar) {
        if (processor == null || ar == null) {
            logd("onGetModemSupportVSimVersionDone, param is null !");
            return;
        }
        int modemVer = parseModemSupportVSimVersionResult(processor, ar);
        this.mVSimController.setModemVSimVersion(modemVer);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("modem support vsim version is: ");
        stringBuilder.append(modemVer);
        logd(stringBuilder.toString());
    }

    public void getModemSupportVSimVersionInner(HwVSimProcessor processor, HwVSimRequest request) {
        Message onCompleted = processor.obtainMessage(30, request);
        logd("start to get modem support vsim version for inner.");
        CommandsInterface ci = getCiBySub(request.mSubId);
        if (ci != null) {
            ci.getModemSupportVSimVersion(onCompleted);
        }
    }

    public boolean isNeedRadioOnM2() {
        if (HwVSimUtilsInner.isVSimDsdsVersionOne()) {
            logd("isVSimDsdsVersionOne,M2 no need set radio_power On");
            return false;
        }
        int insertedCardCount = this.mVSimController.getInsertedCardCount();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insertedCardCount = ");
        stringBuilder.append(insertedCardCount);
        logd(stringBuilder.toString());
        if (insertedCardCount == 0 || insertedCardCount == 1) {
            logd("m2 no need to radio on");
            return false;
        }
        logd("m2 need to radio on");
        return true;
    }

    public void onSetNetworkRatAndSrvdomainDone(HwVSimProcessor processor, AsyncResult ar) {
    }

    public void checkEnableSimCondition(HwVSimProcessor processor, HwVSimRequest request) {
        HwVSimProcessor hwVSimProcessor = processor;
        HwVSimRequest hwVSimRequest = request;
        if (hwVSimRequest != null) {
            int[] cardTypes = request.getCardTypes();
            if (cardTypes != null && cardTypes.length != 0) {
                ExpectPara expectPara;
                boolean[] isCardPresent = HwVSimUtilsInner.getCardState(cardTypes);
                int insertedCardCount = HwVSimUtilsInner.getInsertedCardCount(cardTypes);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Enable: inserted card count = ");
                stringBuilder.append(insertedCardCount);
                logd(stringBuilder.toString());
                VSimEventInfoUtils.setCardPresent(this.mVSimController.mEventInfo, this.mVSimController.getCardPresentNumeric(isCardPresent));
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
                CommrilMode assumedCommrilMode = getAssumedCommrilMode(mainSlot, cardTypes);
                int savedMainSlot = getVSimSavedMainSlot();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Enable: savedMainSlot = ");
                stringBuilder.append(savedMainSlot);
                logd(stringBuilder.toString());
                if (savedMainSlot == -1) {
                    setVSimSavedMainSlot(mainSlot);
                }
                if (insertedCardCount == 0) {
                    expectPara = getExpectParaCheckEnableNoSim(hwVSimRequest, assumedCommrilMode);
                } else if (insertedCardCount == 1) {
                    expectPara = getExpectParaCheckEnableOneSim(hwVSimRequest, assumedCommrilMode);
                } else {
                    int reservedSub = this.mVSimController.getUserReservedSubId();
                    if (HwVSimUtilsInner.isVSimDsdsVersionOne() && reservedSub == -1) {
                        logd("Enable: reserved sub not set");
                        hwVSimProcessor.notifyResult(hwVSimRequest, Integer.valueOf(7));
                        hwVSimProcessor.transitionToState(0);
                        return;
                    } else if (HwVSimUtilsInner.isDualImsSupported()) {
                        expectPara = getExpectParaCheckEnableOrSwitch(hwVSimRequest);
                    } else {
                        expectPara = getExpectParaCheckEnableTwoSimULG(hwVSimRequest, assumedCommrilMode);
                    }
                }
                ExpectPara expectPara2 = expectPara;
                CommrilMode expectCommrilMode = expectPara2.getExpectCommrilMode();
                int expectSlot = expectPara2.getExpectSlot();
                setAlternativeUserReservedSubId(insertedCardCount, expectSlot);
                processAfterCheckEnableCondition(hwVSimProcessor, hwVSimRequest, expectCommrilMode, expectSlot, currentCommrilMode);
            }
        }
    }

    private ExpectPara getExpectParaCheckEnableOrSwitch(HwVSimRequest request) {
        StringBuilder stringBuilder;
        int expectSlot;
        ExpectPara expectPara = new ExpectPara();
        int mainSlot = request.getMainSlot();
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        int[] cardTypes = request.getCardTypes();
        boolean isVSimOn = request.getIsVSimOnM0();
        EnableParam param = this.mVSimController.getEnableParam(request);
        if (param == null || !HwVSimUtilsInner.isValidSlotId(param.cardInModem1)) {
            boolean isSwapFirst = HwVSimUtilsInner.isPlatformNeedWaitNvMatchUnsol();
            stringBuilder = new StringBuilder();
            stringBuilder.append("getExpectParaCheckEnableOrSwitch isSwapFirst = ");
            stringBuilder.append(isSwapFirst);
            logd(stringBuilder.toString());
            if (isSwapFirst) {
                expectSlot = mainSlot;
                if (this.mVSimController.getSubState(slaveSlot) == 0 && this.mVSimController.getSubState(mainSlot) != 0) {
                    logd("getExpectParaCheckEnableOrSwitch, slot in m1 is inactive, so move to m2.");
                    expectSlot = slaveSlot;
                }
            } else {
                expectSlot = isVSimOn ? mainSlot : slaveSlot;
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getExpectParaCheckEnableOrSwitch param.cardInModem1 = ");
            stringBuilder2.append(param.cardInModem1);
            logd(stringBuilder2.toString());
            expectSlot = HwVSimUtilsInner.getAnotherSlotId(param.cardInModem1);
        }
        int expectSlot2 = expectSlot;
        stringBuilder = new StringBuilder();
        stringBuilder.append("getExpectParaCheckEnableOrSwitch isVSimOn = ");
        stringBuilder.append(isVSimOn);
        stringBuilder.append(" expectSlot = ");
        stringBuilder.append(expectSlot2);
        logd(stringBuilder.toString());
        CommrilMode expectCommrilMode = HwVSimSlotSwitchController.getInstance().getVSimOnCommrilMode(expectSlot2, cardTypes);
        expectPara.setExpectSlot(expectSlot2);
        expectPara.setExpectCommrilMode(expectCommrilMode);
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("getExpectParaCheckEnableOrSwitch expectCommrilMode = ");
        stringBuilder3.append(expectCommrilMode);
        stringBuilder3.append(" expectSlot = ");
        stringBuilder3.append(expectSlot2);
        logd(stringBuilder3.toString());
        return expectPara;
    }

    private ExpectPara getExpectParaCheckEnableNoSim(HwVSimRequest request, CommrilMode assumedCommrilMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExpectParaCheckEnableNoSim assumedCommrilMode = ");
        stringBuilder.append(assumedCommrilMode);
        logd(stringBuilder.toString());
        ExpectPara expectPara = new ExpectPara();
        boolean isVSimOnM0 = request.getIsVSimOnM0();
        int mainSlot = request.getMainSlot();
        int[] cardTypes = request.getCardTypes();
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        CommrilMode expectCommrilMode = CommrilMode.NON_MODE;
        if (CommrilMode.isCLGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            if (HwVSimUtilsInner.isChinaTelecom()) {
                expectCommrilMode = CommrilMode.getCGMode();
            } else {
                expectCommrilMode = CommrilMode.getULGMode();
            }
        } else if (CommrilMode.isCGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getCGMode();
        } else if (CommrilMode.isULGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getULGMode();
        }
        int expectSlot = isVSimOnM0 ? mainSlot : slaveSlot;
        expectPara.setExpectSlot(expectSlot);
        expectPara.setExpectCommrilMode(expectCommrilMode);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getExpectParaCheckEnableNoSim expectCommrilMode = ");
        stringBuilder2.append(expectCommrilMode);
        stringBuilder2.append(" expectSlot = ");
        stringBuilder2.append(expectSlot);
        logd(stringBuilder2.toString());
        return expectPara;
    }

    private ExpectPara getExpectParaCheckEnableOneSim(HwVSimRequest request, CommrilMode assumedCommrilMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExpectParaCheckEnableOneSim assumedCommrilMode = ");
        stringBuilder.append(assumedCommrilMode);
        logd(stringBuilder.toString());
        ExpectPara expectPara = new ExpectPara();
        boolean isVSimOnM0 = request.getIsVSimOnM0();
        int mainSlot = request.getMainSlot();
        int[] cardTypes = request.getCardTypes();
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        CommrilMode expectCommrilMode = CommrilMode.NON_MODE;
        int expectSlot = -1;
        boolean[] isCardPresent = HwVSimUtilsInner.getCardState(cardTypes);
        if (CommrilMode.isCLGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getCGMode();
            expectSlot = slaveSlot;
            if (HwVSimUtilsInner.isChinaTelecom() && isVSimOnM0) {
                expectSlot = mainSlot;
            }
        } else if (CommrilMode.isCGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getCGMode();
            expectSlot = mainSlot;
        } else if (CommrilMode.isULGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getULGMode();
            expectSlot = (isCardPresent == null || !isCardPresent[mainSlot]) ? mainSlot : slaveSlot;
        }
        expectPara.setExpectSlot(expectSlot);
        expectPara.setExpectCommrilMode(expectCommrilMode);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getExpectParaCheckEnableOneSim expectCommrilMode = ");
        stringBuilder2.append(expectCommrilMode);
        stringBuilder2.append(" expectSlot = ");
        stringBuilder2.append(expectSlot);
        logd(stringBuilder2.toString());
        return expectPara;
    }

    private ExpectPara getExpectParaCheckEnableTwoSimULG(HwVSimRequest request, CommrilMode assumedCommrilMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExpectParaCheckEnableTwoSimULG assumedCommrilMode = ");
        stringBuilder.append(assumedCommrilMode);
        logd(stringBuilder.toString());
        ExpectPara expectPara = new ExpectPara();
        boolean isVSimOnM0 = request.getIsVSimOnM0();
        int mainSlot = request.getMainSlot();
        int[] cardTypes = request.getCardTypes();
        int reservedSub = this.mVSimController.getUserReservedSubId();
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        CommrilMode expectCommrilMode = CommrilMode.NON_MODE;
        int expectSlot = -1;
        if (CommrilMode.isCLGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            if (HwVSimUtilsInner.isChinaTelecom() && HwVSimUtilsInner.isPlatformRealTripple()) {
                expectCommrilMode = CommrilMode.getCGMode();
                expectSlot = isVSimOnM0 ? mainSlot : slaveSlot;
            } else if (reservedSub == mainSlot) {
                expectCommrilMode = CommrilMode.getCGMode();
                expectSlot = slaveSlot;
            } else if (reservedSub == slaveSlot) {
                if (HwVSimSlotSwitchController.isCDMACard(cardTypes[reservedSub])) {
                    expectCommrilMode = CommrilMode.getCGMode();
                } else {
                    expectCommrilMode = CommrilMode.getULGMode();
                }
                expectSlot = mainSlot;
            } else {
                expectCommrilMode = CommrilMode.getCGMode();
                expectSlot = slaveSlot;
            }
        } else if (CommrilMode.isCGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            if (reservedSub == mainSlot) {
                expectCommrilMode = CommrilMode.getULGMode();
                expectSlot = slaveSlot;
            } else {
                expectCommrilMode = CommrilMode.getCGMode();
                expectSlot = mainSlot;
            }
        } else if (CommrilMode.isULGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getULGMode();
            if (reservedSub == mainSlot) {
                expectSlot = slaveSlot;
            } else {
                expectSlot = mainSlot;
            }
        }
        expectPara.setExpectSlot(expectSlot);
        expectPara.setExpectCommrilMode(expectCommrilMode);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getExpectParaCheckEnableTwoSimULG expectCommrilMode = ");
        stringBuilder2.append(expectCommrilMode);
        stringBuilder2.append(" expectSlot = ");
        stringBuilder2.append(expectSlot);
        logd(stringBuilder2.toString());
        return expectPara;
    }

    private void processAfterCheckEnableCondition(HwVSimProcessor processor, HwVSimRequest request, CommrilMode expectCommrilMode, int expectSlot, CommrilMode currentCommrilMode) {
        logd("Enable: processAfterCheckEnableCondition");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Enable: expectCommrilMode = ");
        stringBuilder.append(expectCommrilMode);
        stringBuilder.append(" expectSlot = ");
        stringBuilder.append(expectSlot);
        stringBuilder.append(" currentCommrilMode = ");
        stringBuilder.append(currentCommrilMode);
        logd(stringBuilder.toString());
        int mainSlot = request.getMainSlot();
        boolean isVSimOnM0 = request.getIsVSimOnM0();
        boolean isNeedSwitchCommrilMode = calcIsNeedSwitchCommrilMode(expectCommrilMode, currentCommrilMode);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Enable: isNeedSwitchCommrilMode = ");
        stringBuilder2.append(isNeedSwitchCommrilMode);
        logd(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Enable: expectSlot = ");
        stringBuilder2.append(expectSlot);
        stringBuilder2.append(" mainSlot = ");
        stringBuilder2.append(mainSlot);
        logd(stringBuilder2.toString());
        if (IS_FAST_SWITCH_SIMSLOT && isNeedSwitchCommrilMode) {
            request.setIsNeedSwitchCommrilMode(true);
            request.setExpectCommrilMode(expectCommrilMode);
        }
        if (IS_FAST_SWITCH_SIMSLOT || !isNeedSwitchCommrilMode) {
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
        } else {
            request.setExpectSlot(expectSlot);
            switchCommrilMode(expectCommrilMode, expectSlot, mainSlot, isVSimOnM0, processor.obtainMessage(55, request));
        }
    }

    public void checkDisableSimCondition(HwVSimProcessor processor, HwVSimRequest request) {
        HwVSimProcessor hwVSimProcessor = processor;
        HwVSimRequest hwVSimRequest = request;
        if (hwVSimRequest != null) {
            int[] cardTypes = request.getCardTypes();
            if (cardTypes != null && cardTypes.length != 0) {
                CommrilMode expectCommrilMode;
                int savedMainSlot = getVSimSavedMainSlot();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Disable: savedMainSlot = ");
                stringBuilder.append(savedMainSlot);
                logd(stringBuilder.toString());
                CommrilMode currentCommrilMode = getCommrilMode();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Disable: currentCommrilMode = ");
                stringBuilder.append(currentCommrilMode);
                logd(stringBuilder.toString());
                int mainSlot = request.getMainSlot();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Disable: mainSlot = ");
                stringBuilder.append(mainSlot);
                logd(stringBuilder.toString());
                int expectSlot = getExpectSlotForDisable(cardTypes, mainSlot, savedMainSlot);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Disable: expectSlot = ");
                stringBuilder.append(expectSlot);
                logd(stringBuilder.toString());
                hwVSimRequest.setExpectSlot(expectSlot);
                if (HwVSimUtilsInner.isChinaTelecom()) {
                    expectCommrilMode = CommrilMode.getCLGMode();
                } else {
                    expectCommrilMode = getExpectCommrilMode(expectSlot, cardTypes);
                }
                CommrilMode expectCommrilMode2 = expectCommrilMode;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Disable: expectCommrilMode = ");
                stringBuilder.append(expectCommrilMode2);
                logd(stringBuilder.toString());
                boolean isNeedSwitchCommrilMode = calcIsNeedSwitchCommrilMode(expectCommrilMode2, currentCommrilMode);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Disable: isNeedSwitchCommrilMode = ");
                stringBuilder.append(isNeedSwitchCommrilMode);
                logd(stringBuilder.toString());
                hwVSimRequest.setIsNeedSwitchCommrilMode(isNeedSwitchCommrilMode);
                if (IS_FAST_SWITCH_SIMSLOT && isNeedSwitchCommrilMode) {
                    hwVSimRequest.setExpectCommrilMode(expectCommrilMode2);
                }
                if (!processor.isReadyProcess()) {
                    boolean isNeedSwitchCommrilMode2 = isNeedSwitchCommrilMode;
                    if (!IS_FAST_SWITCH_SIMSLOT && isNeedSwitchCommrilMode2) {
                        hwVSimProcessor.setProcessType(ProcessType.PROCESS_TYPE_SWAP);
                        VSimEventInfoUtils.setPocessType(this.mVSimController.mEventInfo, 11);
                    } else if (expectSlot == mainSlot) {
                        hwVSimProcessor.setProcessType(ProcessType.PROCESS_TYPE_SWAP);
                        VSimEventInfoUtils.setPocessType(this.mVSimController.mEventInfo, 11);
                    } else {
                        hwVSimProcessor.setProcessType(ProcessType.PROCESS_TYPE_CROSS);
                        VSimEventInfoUtils.setPocessType(this.mVSimController.mEventInfo, 12);
                    }
                    hwVSimProcessor.transitionToState(6);
                } else if (IS_FAST_SWITCH_SIMSLOT || !isNeedSwitchCommrilMode) {
                    HwVSimPhoneFactory.setIsVsimEnabledProp(false);
                    getIMSI(expectSlot);
                    hwVSimProcessor.transitionToState(0);
                } else {
                    switchCommrilMode(expectCommrilMode2, expectSlot, mainSlot, false, hwVSimProcessor.obtainMessage(55, hwVSimRequest));
                }
            }
        }
    }

    public void checkSwitchModeSimCondition(HwVSimProcessor processor, HwVSimRequest request) {
        HwVSimProcessor hwVSimProcessor = processor;
        HwVSimRequest hwVSimRequest = request;
        if (hwVSimRequest != null) {
            int[] cardTypes = request.getCardTypes();
            if (cardTypes != null && cardTypes.length != 0) {
                int insertedCardCount = HwVSimUtilsInner.getInsertedCardCount(cardTypes);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Switch mode: inserted card count = ");
                stringBuilder.append(insertedCardCount);
                logd(stringBuilder.toString());
                CommrilMode currentCommrilMode = getCommrilMode();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Switch mode: currentCommrilMode = ");
                stringBuilder.append(currentCommrilMode);
                logd(stringBuilder.toString());
                int mainSlot = request.getMainSlot();
                CommrilMode assumedCommrilMode = getAssumedCommrilMode(mainSlot, cardTypes);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Switch mode: assumedCommrilMode = ");
                stringBuilder.append(assumedCommrilMode);
                logd(stringBuilder.toString());
                if (request.getIsVSimOnM0()) {
                    ExpectPara expectPara;
                    if (insertedCardCount == 0) {
                        expectPara = getExpectParaCheckSwitchNoSim(hwVSimRequest, assumedCommrilMode);
                    } else if (insertedCardCount == 1) {
                        expectPara = getExpectParaCheckSwitchOneSim(hwVSimRequest, assumedCommrilMode);
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Switch mode: two sim, mainSlot = ");
                        stringBuilder2.append(mainSlot);
                        logd(stringBuilder2.toString());
                        int reservedSub = getSwtichModeUserReservedSubId(hwVSimRequest);
                        if (HwVSimUtilsInner.isVSimDsdsVersionOne() && reservedSub == -1) {
                            logd("Switch mode: reserved sub not set");
                            hwVSimProcessor.notifyResult(hwVSimRequest, Boolean.valueOf(false));
                            hwVSimProcessor.transitionToState(0);
                            return;
                        } else if (HwVSimUtilsInner.isDualImsSupported()) {
                            expectPara = getExpectParaCheckEnableOrSwitch(hwVSimRequest);
                        } else {
                            expectPara = getExpectParaCheckSwitchTwoSimULG(hwVSimRequest, assumedCommrilMode);
                        }
                    }
                    ExpectPara expectPara2 = expectPara;
                    processAfterCheckSwitchCondition(hwVSimProcessor, hwVSimRequest, expectPara2.getExpectCommrilMode(), expectPara2.getExpectSlot(), currentCommrilMode);
                    return;
                }
                hwVSimProcessor.notifyResult(hwVSimRequest, Boolean.valueOf(false));
                hwVSimProcessor.transitionToState(0);
            }
        }
    }

    private ExpectPara getExpectParaCheckSwitchNoSim(HwVSimRequest request, CommrilMode assumedCommrilMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExpectParaCheckSwitchNoSim assumedCommrilMode = ");
        stringBuilder.append(assumedCommrilMode);
        logd(stringBuilder.toString());
        ExpectPara expectPara = new ExpectPara();
        int mainSlot = request.getMainSlot();
        int[] cardTypes = request.getCardTypes();
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        CommrilMode expectCommrilMode = CommrilMode.NON_MODE;
        int expectSlot = -1;
        if (CommrilMode.isCLGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            if (HwVSimUtilsInner.isChinaTelecom()) {
                expectCommrilMode = CommrilMode.getCGMode();
                expectSlot = slaveSlot;
            } else {
                expectCommrilMode = CommrilMode.getULGMode();
                expectSlot = mainSlot;
            }
        } else if (CommrilMode.isCGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getULGMode();
            expectSlot = mainSlot;
        } else if (CommrilMode.isULGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getULGMode();
            expectSlot = mainSlot;
        }
        expectPara.setExpectSlot(expectSlot);
        expectPara.setExpectCommrilMode(expectCommrilMode);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getExpectParaCheckSwitchNoSim expectCommrilMode = ");
        stringBuilder2.append(expectCommrilMode);
        stringBuilder2.append(" expectSlot = ");
        stringBuilder2.append(expectSlot);
        logd(stringBuilder2.toString());
        return expectPara;
    }

    private ExpectPara getExpectParaCheckSwitchOneSim(HwVSimRequest request, CommrilMode assumedCommrilMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExpectParaCheckSwitchOneSim assumedCommrilMode = ");
        stringBuilder.append(assumedCommrilMode);
        logd(stringBuilder.toString());
        ExpectPara expectPara = new ExpectPara();
        int mainSlot = request.getMainSlot();
        int[] cardTypes = request.getCardTypes();
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        CommrilMode expectCommrilMode = CommrilMode.NON_MODE;
        int expectSlot = -1;
        boolean[] isCardPresent = HwVSimUtilsInner.getCardState(cardTypes);
        if (CommrilMode.isCLGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getCGMode();
            expectSlot = slaveSlot;
        } else if (CommrilMode.isCGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getCGMode();
            expectSlot = mainSlot;
        } else if (CommrilMode.isULGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getULGMode();
            if (isCardPresent == null || !isCardPresent[mainSlot]) {
                expectSlot = mainSlot;
            } else {
                expectSlot = slaveSlot;
            }
        }
        expectPara.setExpectSlot(expectSlot);
        expectPara.setExpectCommrilMode(expectCommrilMode);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getExpectParaCheckSwitchOneSim expectCommrilMode = ");
        stringBuilder2.append(expectCommrilMode);
        stringBuilder2.append(" expectSlot = ");
        stringBuilder2.append(expectSlot);
        logd(stringBuilder2.toString());
        return expectPara;
    }

    private ExpectPara getExpectParaCheckSwitchTwoSimULG(HwVSimRequest request, CommrilMode assumedCommrilMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getExpectParaCheckSwitchTwoSimULG assumedCommrilMode = ");
        stringBuilder.append(assumedCommrilMode);
        logd(stringBuilder.toString());
        ExpectPara expectPara = new ExpectPara();
        int mainSlot = request.getMainSlot();
        int[] cardTypes = request.getCardTypes();
        int reservedSub = this.mVSimController.getUserReservedSubId();
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        CommrilMode expectCommrilMode = CommrilMode.NON_MODE;
        int expectSlot = -1;
        if (CommrilMode.isCLGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            if (HwVSimUtilsInner.isChinaTelecom()) {
                expectCommrilMode = CommrilMode.getCGMode();
                expectSlot = slaveSlot;
            } else if (reservedSub == mainSlot) {
                expectCommrilMode = CommrilMode.getCGMode();
                expectSlot = slaveSlot;
            } else if (reservedSub == slaveSlot) {
                if (HwVSimSlotSwitchController.isCDMACard(cardTypes[reservedSub])) {
                    expectCommrilMode = CommrilMode.getCGMode();
                } else {
                    expectCommrilMode = CommrilMode.getULGMode();
                }
                expectSlot = mainSlot;
            } else {
                expectCommrilMode = CommrilMode.getCGMode();
                expectSlot = slaveSlot;
            }
        } else if (CommrilMode.isCGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            if (reservedSub == mainSlot) {
                expectCommrilMode = CommrilMode.getULGMode();
                expectSlot = slaveSlot;
            } else {
                expectCommrilMode = CommrilMode.getCGMode();
                expectSlot = mainSlot;
            }
        } else if (CommrilMode.isULGMode(assumedCommrilMode, cardTypes, mainSlot)) {
            expectCommrilMode = CommrilMode.getULGMode();
            if (reservedSub == mainSlot) {
                expectSlot = slaveSlot;
            } else {
                expectSlot = mainSlot;
            }
        }
        expectPara.setExpectSlot(expectSlot);
        expectPara.setExpectCommrilMode(expectCommrilMode);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getExpectParaCheckSwitchTwoSimULG expectCommrilMode = ");
        stringBuilder2.append(expectCommrilMode);
        stringBuilder2.append(" expectSlot = ");
        stringBuilder2.append(expectSlot);
        logd(stringBuilder2.toString());
        return expectPara;
    }

    private void processAfterCheckSwitchCondition(HwVSimProcessor processor, HwVSimRequest request, CommrilMode expectCommrilMode, int expectSlot, CommrilMode currentCommrilMode) {
        logd("Switch mode: processAfterCheckSwitchCondition");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Switch mode: expectCommrilMode = ");
        stringBuilder.append(expectCommrilMode);
        stringBuilder.append(" expectSlot = ");
        stringBuilder.append(expectSlot);
        stringBuilder.append(" currentCommrilMode = ");
        stringBuilder.append(currentCommrilMode);
        logd(stringBuilder.toString());
        int mainSlot = request.getMainSlot();
        boolean isVSimOnM0 = request.getIsVSimOnM0();
        boolean isNeedSwitchCommrilMode = calcIsNeedSwitchCommrilMode(expectCommrilMode, currentCommrilMode);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Switch mode: isNeedSwitchCommrilMode = ");
        stringBuilder2.append(isNeedSwitchCommrilMode);
        logd(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Switch mode: expectSlot = ");
        stringBuilder2.append(expectSlot);
        stringBuilder2.append(" mainSlot = ");
        stringBuilder2.append(mainSlot);
        logd(stringBuilder2.toString());
        if (expectSlot != mainSlot || isNeedSwitchCommrilMode) {
            if (HwVSimUtilsInner.isPlatformRealTripple() && HwVSimUtilsInner.IS_DSDSPOWER_SUPPORT) {
                this.mVSimController.updateSubState(0, 1);
                this.mVSimController.updateSubState(1, 1);
            }
            if (IS_FAST_SWITCH_SIMSLOT && isNeedSwitchCommrilMode) {
                request.setIsNeedSwitchCommrilMode(true);
                request.setExpectCommrilMode(expectCommrilMode);
            }
            if (IS_FAST_SWITCH_SIMSLOT || !isNeedSwitchCommrilMode) {
                if (expectSlot != mainSlot) {
                    processor.setProcessType(ProcessType.PROCESS_TYPE_CROSS);
                } else {
                    processor.setProcessType(ProcessType.PROCESS_TYPE_SWAP);
                }
                processor.transitionToState(12);
            } else {
                request.setExpectSlot(expectSlot);
                switchCommrilMode(expectCommrilMode, expectSlot, mainSlot, isVSimOnM0, processor.obtainMessage(55, request));
            }
            return;
        }
        logd("Switch mode: no need to switch sim slot and commril mode.");
        processor.notifyResult(request, Boolean.valueOf(true));
        processor.transitionToState(0);
    }

    public void switchSimSlot(HwVSimProcessor processor, HwVSimRequest request) {
        int modem0;
        int modem1;
        int modem2;
        int modem22;
        int mainSlot = request.getMainSlot();
        boolean needSwich = true;
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("switchSimSlot mainSlot = ");
        stringBuilder.append(mainSlot);
        stringBuilder.append(", slaveSlot = ");
        stringBuilder.append(slaveSlot);
        logd(stringBuilder.toString());
        int subId = mainSlot;
        if (processor.isEnableProcess() || processor.isSwitchModeProcess()) {
            if (processor.isSwapProcess()) {
                modem0 = 2;
                modem1 = slaveSlot;
                modem2 = mainSlot;
            } else if (processor.isCrossProcess()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("expectSlot = ");
                stringBuilder2.append(slaveSlot);
                logd(stringBuilder2.toString());
                request.setExpectSlot(slaveSlot);
                modem0 = 2;
                modem1 = mainSlot;
                modem2 = slaveSlot;
            } else {
                processor.doProcessException(null, request);
                return;
            }
            modem22 = modem2;
            if (request.getIsVSimOnM0()) {
                subId = 2;
            }
        } else if (processor.isDisableProcess()) {
            modem0 = request.getExpectSlot();
            if (processor.isSwapProcess()) {
                modem1 = mainSlot;
                modem2 = slaveSlot;
            } else if (modem0 == 0) {
                modem1 = 0;
                modem2 = 1;
            } else {
                modem1 = 1;
                modem2 = 0;
            }
            modem22 = 2;
            subId = 2;
            modem0 = modem1;
            modem1 = modem2;
        } else {
            processor.doProcessException(null, request);
            return;
        }
        modem2 = modem22;
        int[] oldSlots = getSimSlotTable();
        int[] slots = createSimSlotsTable(modem0, modem1, modem2);
        request.setSlots(slots);
        request.mSubId = subId;
        if (oldSlots.length == 3 && oldSlots[0] == slots[0] && oldSlots[1] == slots[1] && oldSlots[2] == slots[2]) {
            needSwich = false;
        }
        Message onCompleted = processor.obtainMessage(43, request);
        if (needSwich) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("switchSimSlot subId ");
            stringBuilder3.append(subId);
            stringBuilder3.append(" modem0 = ");
            stringBuilder3.append(modem0);
            stringBuilder3.append(" modem1 = ");
            stringBuilder3.append(modem1);
            stringBuilder3.append(" modem2 = ");
            stringBuilder3.append(modem2);
            logd(stringBuilder3.toString());
            CommandsInterface ci = getCiBySub(subId);
            if (ci != null) {
                ci.hotSwitchSimSlot(modem0, modem1, modem2, onCompleted);
            }
        } else {
            AsyncResult.forMessage(onCompleted, null, null);
            onCompleted.sendToTarget();
        }
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
            return;
        }
        if (processor.isEnableProcess()) {
            if (HwVSimUtilsInner.isPlatformRealTripple()) {
                ci.setActiveModemMode(1, onCompleted);
            } else if (processor.isSwapProcess()) {
                ci.setActiveModemMode(1, onCompleted);
                ci.setUEOperationMode(1, null);
            } else {
                ci.setActiveModemMode(0, onCompleted);
                ci.setUEOperationMode(0, null);
            }
        } else if (processor.isDisableProcess()) {
            if (!HwVSimUtilsInner.isPlatformRealTripple()) {
                ci.setActiveModemMode(0, onCompleted);
                ci.setUEOperationMode(0, null);
            } else if (this.mVSimController.getInsertedCardCount() == 1) {
                ci.setActiveModemMode(0, onCompleted);
            } else {
                ci.setActiveModemMode(1, onCompleted);
            }
        } else if (!processor.isSwitchModeProcess()) {
        } else {
            if (HwVSimUtilsInner.isPlatformRealTripple()) {
                ci.setActiveModemMode(1, onCompleted);
            } else if (processor.isSwapProcess()) {
                ci.setActiveModemMode(1, onCompleted);
                ci.setUEOperationMode(1, null);
            } else {
                ci.setActiveModemMode(0, onCompleted);
                ci.setUEOperationMode(0, null);
            }
        }
    }

    public void radioPowerOff(HwVSimProcessor processor, HwVSimRequest request) {
        if (processor != null && request != null) {
            request.createPowerOnOffMark();
            request.createGetSimStateMark();
            request.createCardOnOffMark();
            request.createGetIccCardStatusMark();
            int subCount = request.getSubCount();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("radioPowerOff:subCount = ");
            stringBuilder.append(subCount);
            logd(stringBuilder.toString());
            for (int i = 0; i < subCount; i++) {
                int subId = request.getSubIdByIndex(i);
                request.setPowerOnOffMark(i, true);
                request.setSimStateMark(i, true);
                request.setCardOnOffMark(i, true);
                request.setGetIccCardStatusMark(i, true);
                radioPowerOff(processor, request.clone(), subId);
            }
        }
    }

    public void getSimState(HwVSimProcessor processor, HwVSimRequest request) {
        if (processor != null && request != null) {
            int[] subs = getSimSlotTable();
            if (subs.length != 0) {
                request.setSubs(subs);
                request.createPowerOnOffMark();
                request.createGetSimStateMark();
                request.createCardOnOffMark();
                request.createGetIccCardStatusMark();
                int subCount = request.getSubCount();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getSimState:subCount = ");
                stringBuilder.append(subCount);
                logd(stringBuilder.toString());
                for (int i = 0; i < subCount; i++) {
                    int subId = request.getSubIdByIndex(i);
                    request.setSimStateMark(i, true);
                    getSimState(processor, request.clone(), subId);
                }
            }
        }
    }

    public int getPoffSubForEDWork(HwVSimRequest request) {
        if (request == null) {
            return -1;
        }
        return 2;
    }

    public void onEDWorkTransitionState(HwVSimProcessor processor) {
        if (processor != null) {
            processor.transitionToState(4);
        }
    }

    public void doEnableStateEnter(HwVSimProcessor processor, HwVSimRequest request) {
    }

    public void doDisableStateExit(HwVSimProcessor processor, HwVSimRequest request) {
    }

    private int[] createSimSlotsTable(int m0, int m1, int m2) {
        int[] slots = new int[MAX_SUB_COUNT];
        slots[0] = m0;
        slots[1] = m1;
        slots[2] = m2;
        return slots;
    }

    private int getSwtichModeUserReservedSubId(HwVSimRequest request) {
        WorkModeParam param = getWorkModeParam(request);
        if (param == null) {
            return -1;
        }
        switch (param.workMode) {
            case 0:
                return 0;
            case 1:
                return 1;
            default:
                return -1;
        }
    }

    private WorkModeParam getWorkModeParam(HwVSimRequest request) {
        if (this.mVSimController == null) {
            return null;
        }
        return this.mVSimController.getWorkModeParam(request);
    }

    private boolean isGotAllCardTypes(HwVSimRequest request) {
        return request != null && request.isGotAllCardTypes();
    }

    private void setAlternativeUserReservedSubId(int cardCount, int expectSlot) {
        if (HwVSimUtilsInner.isVSimDsdsVersionOne() && -1 == this.mVSimController.getUserReservedSubId()) {
            int modem1Slot = 1;
            if (cardCount == 0 || cardCount == 1) {
                if (expectSlot != 0) {
                    modem1Slot = 0;
                }
                this.mVSimController.setAlternativeUserReservedSubId(modem1Slot);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setAlternativeUserReservedSubId: set subId is ");
                stringBuilder.append(modem1Slot);
                stringBuilder.append(".");
                logd(stringBuilder.toString());
            }
        }
    }

    public void onSimHotPlugOut() {
    }

    public void onRadioPowerOffSlaveModemDone(HwVSimProcessor processor, HwVSimRequest request) {
    }

    public void onCardPowerOffDoneInEWork(HwVSimProcessor processor, int subId) {
    }

    public int getAllAbilityNetworkTypeOnModem1(boolean duallteCapOpened) {
        if (HwVSimUtilsInner.isDualImsSupported() && duallteCapOpened) {
            return 9;
        }
        if (HwVSimUtilsInner.isPlatformRealTripple()) {
            return 3;
        }
        return 1;
    }
}
