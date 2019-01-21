package com.android.internal.telephony.vsim;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HwVSimPhoneFactory;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

public class HwVSimSlotSwitchController extends StateMachine {
    public static final int CARD_TYPE_DUAL_MODE = 3;
    public static final int CARD_TYPE_INVALID = -1;
    public static final int CARD_TYPE_NO_SIM = 0;
    public static final int CARD_TYPE_SINGLE_CDMA = 2;
    public static final int CARD_TYPE_SINGLE_GSM = 1;
    private static final int CMD_SWITCH_COMMRIL_MODE = 11;
    private static final int COMBINE = 0;
    private static final int EVENT_GET_SIM_SLOT_DONE = 2;
    private static final int EVENT_GET_SIM_STATE_DONE = 3;
    private static final int EVENT_INITIAL_TIMEOUT = 35;
    private static final int EVENT_RADIO_POWER_OFF_DONE = 36;
    private static final int EVENT_SET_CDMA_MODE_SIDE_DONE = 15;
    private static final int EVENT_SET_SIM_STATE_DONE = 4;
    private static final int EVENT_SWITCH_COMMRIL_MODE_DONE = 12;
    private static final int EVENT_SWITCH_SLOT_DONE = 25;
    private static final long INITIAL_TIMEOUT = 120000;
    private static final int INVALID_COMBINE = -1;
    private static final int INVALID_SIM_SLOT = -1;
    public static final boolean IS_FAST_SWITCH_SIMSLOT = SystemProperties.getBoolean("ro.config.fast_switch_simslot", false);
    private static final boolean IS_HISI_CDMA_SUPPORTED = SystemProperties.getBoolean(PROPERTY_HISI_CDMA_SUPPORTED, false);
    private static final boolean IS_SUPPORT_FULL_NETWORK = SystemProperties.getBoolean("ro.config.full_network_support", false);
    private static final String LOG_TAG = "VSimSwitchController";
    private static final int NOT_COMBINE = 1;
    private static final int PHONE_COUNT = TelephonyManager.getDefault().getPhoneCount();
    private static final String PROPERTY_CG_STANDBY_MODE = "persist.radio.cg_standby_mode";
    private static final String PROPERTY_COMMRIL_MODE = "persist.radio.commril_mode";
    private static final String PROPERTY_FULL_NETWORK_SUPPORT = "ro.config.full_network_support";
    private static final String PROPERTY_HISI_CDMA_SUPPORTED = "ro.config.hisi_cdma_supported";
    private static final int RF0 = 0;
    private static final int RF1 = 1;
    private static final int SUB_COUNT = (PHONE_COUNT + 1);
    private static final int SUB_VSIM = PHONE_COUNT;
    private static HwVSimSlotSwitchController sInstance;
    private static final boolean sIsPlatformSupportVSim = SystemProperties.getBoolean("ro.radio.vsim_support", false);
    private static final Object sLock = new Object();
    private CommandsInterface[] mCis;
    private Message mCompleteMsg;
    private SlotSwitchDefaultState mDefaultState = new SlotSwitchDefaultState();
    private CommrilMode mExpectCommrilMode;
    private int mExpectSlot;
    private boolean mGotSimSlot;
    private boolean mInitDoneSent;
    private InitialState mInitialState = new InitialState();
    private boolean mIsVSimEnabled;
    private boolean mIsVSimOn;
    private int mMainSlot;
    private int mPollingCount;
    private boolean[] mRadioAvailable;
    private boolean[] mRadioPowerStatus = null;
    private boolean mSlotSwitchDone;
    private SwitchCommrilModeState mSwitchCommrilModeState = new SwitchCommrilModeState();
    private boolean mSwitchingCommrilMode;
    private Message mUserCompleteMsg;
    private HashMap<Integer, String> mWhatToStringMap;

    public enum CommrilMode {
        NON_MODE,
        SVLTE_MODE,
        CLG_MODE,
        CG_MODE,
        ULG_MODE,
        HISI_CGUL_MODE,
        HISI_CG_MODE,
        HISI_VSIM_MODE;

        public static CommrilMode getCLGMode() {
            if (HwVSimSlotSwitchController.IS_HISI_CDMA_SUPPORTED) {
                return HISI_CGUL_MODE;
            }
            return CLG_MODE;
        }

        public static CommrilMode getULGMode() {
            if (HwVSimSlotSwitchController.IS_HISI_CDMA_SUPPORTED) {
                return HISI_CGUL_MODE;
            }
            return ULG_MODE;
        }

        public static CommrilMode getCGMode() {
            if (HwVSimSlotSwitchController.IS_HISI_CDMA_SUPPORTED) {
                return HISI_CG_MODE;
            }
            return CG_MODE;
        }

        public static boolean isCLGMode(CommrilMode mode, int[] cardType, int mainSlot) {
            boolean z = false;
            if (!HwVSimSlotSwitchController.IS_SUPPORT_FULL_NETWORK && !HwVSimUtilsInner.isChinaTelecom()) {
                return false;
            }
            if (!HwVSimSlotSwitchController.IS_HISI_CDMA_SUPPORTED) {
                if (mode == CLG_MODE) {
                    z = true;
                }
                return z;
            } else if (mode != HISI_CGUL_MODE) {
                return false;
            } else {
                if (HwVSimUtilsInner.isChinaTelecom()) {
                    return true;
                }
                if (cardType[mainSlot] == 2 || cardType[mainSlot] == 3) {
                    z = true;
                }
                return z;
            }
        }

        public static boolean isULGMode(CommrilMode mode, int[] cardType, int mainSlot) {
            boolean z = false;
            if (!HwVSimSlotSwitchController.IS_SUPPORT_FULL_NETWORK) {
                if (HwVSimUtilsInner.isPlatformRealTripple()) {
                    return !HwVSimUtilsInner.isChinaTelecom();
                } else {
                    if (!HwVSimUtilsInner.isChinaTelecom()) {
                        return true;
                    }
                }
            }
            if (!HwVSimSlotSwitchController.IS_HISI_CDMA_SUPPORTED) {
                if (mode == ULG_MODE) {
                    z = true;
                }
                return z;
            } else if (mode != HISI_CGUL_MODE) {
                return false;
            } else {
                if (cardType[mainSlot] == 1 || cardType[mainSlot] == 0) {
                    z = true;
                }
                return z;
            }
        }

        public static boolean isCGMode(CommrilMode mode, int[] cardType, int mainSlot) {
            boolean z = false;
            if (!HwVSimSlotSwitchController.IS_SUPPORT_FULL_NETWORK && !HwVSimUtilsInner.isChinaTelecom()) {
                return false;
            }
            if (HwVSimSlotSwitchController.IS_HISI_CDMA_SUPPORTED) {
                if (mode == HISI_CG_MODE) {
                    z = true;
                }
                return z;
            }
            if (mode == CG_MODE) {
                z = true;
            }
            return z;
        }
    }

    private class InitialState extends State {
        private InitialState() {
        }

        public void enter() {
            HwVSimSlotSwitchController.this.logd("InitialState: enter");
            checkVSimEnabledStatus();
            for (int i = 0; i < HwVSimSlotSwitchController.SUB_COUNT; i++) {
                HwVSimSlotSwitchController.this.mCis[i].registerForAvailable(HwVSimSlotSwitchController.this.getHandler(), 83, Integer.valueOf(i));
            }
            if (HwVSimSlotSwitchController.this.getHandler() != null) {
                HwVSimSlotSwitchController.this.getHandler().sendEmptyMessageDelayed(35, 120000);
            }
        }

        public void exit() {
            HwVSimSlotSwitchController.this.logd("InitialState: exit");
            if (HwVSimSlotSwitchController.this.getHandler() != null) {
                HwVSimSlotSwitchController.this.getHandler().removeMessages(35);
            }
            for (int i = 0; i < HwVSimSlotSwitchController.SUB_COUNT; i++) {
                HwVSimSlotSwitchController.this.mCis[i].unregisterForAvailable(HwVSimSlotSwitchController.this.getHandler());
            }
        }

        public boolean processMessage(Message msg) {
            HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("InitialState: what = ");
            stringBuilder.append(HwVSimSlotSwitchController.this.getWhatToString(msg.what));
            hwVSimSlotSwitchController.logi(stringBuilder.toString());
            int i = msg.what;
            if (i == 35) {
                HwVSimSlotSwitchController.this.loge("warning, initial time out");
                if (HwVSimSlotSwitchController.this.mMainSlot == -1) {
                    HwVSimSlotSwitchController.this.mMainSlot = 0;
                    hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mMainSlot = ");
                    stringBuilder.append(HwVSimSlotSwitchController.this.mMainSlot);
                    hwVSimSlotSwitchController.logd(stringBuilder.toString());
                }
                checkSwitchSlotDone();
                return true;
            } else if (i != 83) {
                switch (i) {
                    case 2:
                        onGetSimSlotDone(msg);
                        return true;
                    case 3:
                        if (HwVSimSlotSwitchController.this.mSlotSwitchDone) {
                            onCheckSimMode(msg);
                            return true;
                        }
                        onGetSimStateDone(msg);
                        return true;
                    case 4:
                        onSetSimStateDone(msg);
                        return true;
                    default:
                        hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("InitialState: not handled msg.what = ");
                        stringBuilder.append(msg.what);
                        hwVSimSlotSwitchController.logi(stringBuilder.toString());
                        return false;
                }
            } else {
                onAvailableInitial(msg);
                return true;
            }
        }

        private void onAvailableInitial(Message msg) {
            Integer index = HwVSimUtilsInner.getCiIndex(msg);
            if (index.intValue() < 0 || index.intValue() >= HwVSimSlotSwitchController.this.mCis.length) {
                HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("InitialState: Invalid index : ");
                stringBuilder.append(index);
                stringBuilder.append(" received with event ");
                stringBuilder.append(msg.what);
                hwVSimSlotSwitchController.logd(stringBuilder.toString());
                return;
            }
            HwVSimSlotSwitchController.this.mCis[index.intValue()].getBalongSim(HwVSimSlotSwitchController.this.obtainMessage(2, index));
        }

        private void onGetSimSlotDone(Message msg) {
            HwVSimSlotSwitchController.this.logd("onGetSimSlotDone");
            if (HwVSimSlotSwitchController.this.mGotSimSlot) {
                HwVSimSlotSwitchController.this.logd("onGetSimSlotDone, mGotSimSlot done");
                return;
            }
            AsyncResult ar = msg.obj;
            if (ar == null) {
                HwVSimSlotSwitchController.this.logd("ar null");
                return;
            }
            if (ar.exception == null && ar.result != null && ((int[]) ar.result).length == 3) {
                onGetTriSimSlotDone(ar.result);
            } else if (ar.exception == null && ar.result != null && ((int[]) ar.result).length == 2) {
                onGetDualSimSlotDone((int[]) ar.result);
            } else {
                HwVSimSlotSwitchController.this.logd("onGetSimSlotDone got error");
                if (HwVSimSlotSwitchController.this.mMainSlot == -1) {
                    HwVSimSlotSwitchController.this.mMainSlot = 0;
                    HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mMainSlot = ");
                    stringBuilder.append(HwVSimSlotSwitchController.this.mMainSlot);
                    hwVSimSlotSwitchController.logd(stringBuilder.toString());
                }
            }
        }

        private void onGetTriSimSlotDone(int[] slots) {
            HwVSimSlotSwitchController.this.logd("onGetTriSimSlotDone");
            HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("result = ");
            stringBuilder.append(Arrays.toString(slots));
            hwVSimSlotSwitchController.logd(stringBuilder.toString());
            if (slots[0] == 0 && slots[1] == 1 && slots[2] == 2) {
                HwVSimSlotSwitchController.this.mMainSlot = 0;
                HwVSimSlotSwitchController.this.mIsVSimOn = false;
            } else if (slots[0] == 1 && slots[1] == 0 && slots[2] == 2) {
                HwVSimSlotSwitchController.this.mMainSlot = 1;
                HwVSimSlotSwitchController.this.mIsVSimOn = false;
            } else if (slots[0] == 2 && slots[1] == 1 && slots[2] == 0) {
                HwVSimSlotSwitchController.this.mMainSlot = 0;
                HwVSimSlotSwitchController.this.mIsVSimOn = true;
            } else if (slots[0] == 2 && slots[1] == 0 && slots[2] == 1) {
                HwVSimSlotSwitchController.this.mMainSlot = 1;
                HwVSimSlotSwitchController.this.mIsVSimOn = true;
            } else {
                HwVSimSlotSwitchController.this.mMainSlot = 0;
            }
            hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mMainSlot = ");
            stringBuilder.append(HwVSimSlotSwitchController.this.mMainSlot);
            hwVSimSlotSwitchController.logd(stringBuilder.toString());
            hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mIsVSimOn = ");
            stringBuilder.append(HwVSimSlotSwitchController.this.mIsVSimOn);
            hwVSimSlotSwitchController.logd(stringBuilder.toString());
            HwVSimSlotSwitchController.this.mGotSimSlot = true;
            HwVSimController.getInstance().setSimSlotTable(slots);
            getAllRatCombineMode();
        }

        private void onGetDualSimSlotDone(int[] slots) {
            HwVSimSlotSwitchController.this.logd("onGetDualSimSlotDone");
            HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("result = ");
            stringBuilder.append(Arrays.toString(slots));
            hwVSimSlotSwitchController.logd(stringBuilder.toString());
            int modem2 = 2;
            if (slots[0] == 0 && slots[1] == 1) {
                HwVSimSlotSwitchController.this.mMainSlot = 0;
                HwVSimSlotSwitchController.this.mIsVSimOn = false;
            } else if (slots[0] == 1 && slots[1] == 0) {
                HwVSimSlotSwitchController.this.mMainSlot = 1;
                HwVSimSlotSwitchController.this.mIsVSimOn = false;
            } else if (slots[0] == 2 && slots[1] == 0) {
                HwVSimSlotSwitchController.this.mMainSlot = 1;
                HwVSimSlotSwitchController.this.mIsVSimOn = true;
                modem2 = HwVSimSlotSwitchController.this.mMainSlot;
            } else if (slots[0] == 2 && slots[1] == 1) {
                HwVSimSlotSwitchController.this.mMainSlot = 0;
                HwVSimSlotSwitchController.this.mIsVSimOn = true;
                modem2 = HwVSimSlotSwitchController.this.mMainSlot;
            }
            int[] slotsTable = HwVSimUtilsInner.createSimSlotsTable(slots[0], slots[1], modem2);
            HwVSimSlotSwitchController hwVSimSlotSwitchController2 = HwVSimSlotSwitchController.this;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mMainSlot = ");
            stringBuilder2.append(HwVSimSlotSwitchController.this.mMainSlot);
            hwVSimSlotSwitchController2.logd(stringBuilder2.toString());
            hwVSimSlotSwitchController2 = HwVSimSlotSwitchController.this;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mIsVSimOn = ");
            stringBuilder2.append(HwVSimSlotSwitchController.this.mIsVSimOn);
            hwVSimSlotSwitchController2.logd(stringBuilder2.toString());
            HwVSimSlotSwitchController.this.mGotSimSlot = true;
            HwVSimController.getInstance().setSimSlotTable(slotsTable);
            getAllRatCombineMode();
        }

        private void onGetSimStateDone(Message msg) {
            AsyncResult ar = msg.obj;
            if (ar == null) {
                HwVSimSlotSwitchController.this.logd("onGetSimStateDone : ar null");
                return;
            }
            if (ar.exception != null || ar.result == null || ((int[]) ar.result).length <= 3) {
                HwVSimSlotSwitchController.this.loge("onGetSimStateDone got error !");
            } else {
                int simIndex = ((int[]) ar.result)[0];
                int simEnable = ((int[]) ar.result)[1];
                int simSub = ((int[]) ar.result)[2];
                int simNetinfo = ((int[]) ar.result)[3];
                int slaveSlot = HwVSimSlotSwitchController.this.mMainSlot == 0 ? 1 : 0;
                HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[2Cards]simIndex= ");
                stringBuilder.append(simIndex);
                stringBuilder.append(", simEnable= ");
                stringBuilder.append(simEnable);
                hwVSimSlotSwitchController.logd(stringBuilder.toString());
                hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[2Cards]simSub= ");
                stringBuilder.append(simSub);
                stringBuilder.append(", simNetinfo= ");
                stringBuilder.append(simNetinfo);
                hwVSimSlotSwitchController.logd(stringBuilder.toString());
                if (11 == simIndex && HwVSimUtilsInner.isRadioAvailable(2)) {
                    HwVSimSlotSwitchController.this.mIsVSimOn = true;
                } else {
                    HwVSimSlotSwitchController.this.mIsVSimOn = false;
                }
                hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mIsVSimOn = ");
                stringBuilder.append(HwVSimSlotSwitchController.this.mIsVSimOn);
                hwVSimSlotSwitchController.logd(stringBuilder.toString());
                int[] slots = new int[3];
                if (HwVSimSlotSwitchController.this.mIsVSimOn) {
                    slots[0] = 2;
                    slots[1] = slaveSlot;
                    slots[2] = HwVSimSlotSwitchController.this.mMainSlot;
                } else {
                    slots[0] = HwVSimSlotSwitchController.this.mMainSlot;
                    slots[1] = slaveSlot;
                    slots[2] = 2;
                }
                HwVSimController.getInstance().setSimSlotTable(slots);
                getAllRatCombineMode();
            }
        }

        private void checkVSimEnabledStatus() {
            HwVSimSlotSwitchController.this.logi("checkVSimEnabledStatus");
            if (HwVSimSlotSwitchController.sIsPlatformSupportVSim) {
                HwVSimSlotSwitchController.this.mIsVSimEnabled = HwVSimController.getInstance().isVSimEnabled();
            }
            HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mIsVSimEnabled = ");
            stringBuilder.append(HwVSimSlotSwitchController.this.mIsVSimEnabled);
            hwVSimSlotSwitchController.logi(stringBuilder.toString());
        }

        private void getAllRatCombineMode() {
            if (HwVSimSlotSwitchController.IS_HISI_CDMA_SUPPORTED) {
                HwVSimSlotSwitchController.this.logi("x mode, done");
                checkSwitchSlotDone();
            }
        }

        private void checkSwitchSlotDone() {
            HwVSimSlotSwitchController.this.logd("checkSwitchSlotDone");
            HwVSimSlotSwitchController.this.mSlotSwitchDone = true;
            if (HwVSimSlotSwitchController.this.mIsVSimEnabled) {
                HwVSimSlotSwitchController.this.sendInitDone();
                HwVSimSlotSwitchController.this.transitionTo(HwVSimSlotSwitchController.this.mDefaultState);
                return;
            }
            checkSimMode();
        }

        private void checkSimMode() {
            int balongSlot = HwVSimSlotSwitchController.this.mIsVSimOn ? 2 : HwVSimSlotSwitchController.this.mMainSlot;
            HwVSimSlotSwitchController.this.mCis[balongSlot].getSimState(HwVSimSlotSwitchController.this.obtainMessage(3, balongSlot));
        }

        private void onCheckSimMode(Message msg) {
            AsyncResult ar = msg.obj;
            if (ar == null) {
                HwVSimSlotSwitchController.this.logd("onGetSimStateDone : ar null");
                return;
            }
            if (ar.exception != null || ar.result == null || ((int[]) ar.result).length <= 3) {
                HwVSimSlotSwitchController.this.loge("onCheckSimMode got error !");
                checkSimModeDone();
            } else {
                int simIndex = ((int[]) ar.result)[0];
                int simEnable = ((int[]) ar.result)[1];
                int simSub = ((int[]) ar.result)[2];
                int simNetinfo = ((int[]) ar.result)[3];
                HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("simIndex= ");
                stringBuilder.append(simIndex);
                stringBuilder.append(", simEnable= ");
                stringBuilder.append(simEnable);
                hwVSimSlotSwitchController.logd(stringBuilder.toString());
                hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("simSub= ");
                stringBuilder.append(simSub);
                stringBuilder.append(", simNetinfo= ");
                stringBuilder.append(simNetinfo);
                hwVSimSlotSwitchController.logd(stringBuilder.toString());
                if (11 != simIndex || HwVSimSlotSwitchController.this.mIsVSimEnabled) {
                    checkSimModeDone();
                } else {
                    int balongSlot = HwVSimSlotSwitchController.this.mIsVSimOn ? 2 : HwVSimSlotSwitchController.this.mMainSlot;
                    HwVSimSlotSwitchController.this.mCis[balongSlot].setSimState(simIndex, 0, HwVSimSlotSwitchController.this.obtainMessage(4, balongSlot));
                }
            }
        }

        private void checkSimModeDone() {
            HwVSimSlotSwitchController.this.logd("checkSimModeDone");
            HwVSimSlotSwitchController.this.sendInitDone();
            HwVSimSlotSwitchController.this.transitionTo(HwVSimSlotSwitchController.this.mDefaultState);
        }

        private void onSetSimStateDone(Message msg) {
            HwVSimSlotSwitchController.this.mCis[HwVSimSlotSwitchController.this.mIsVSimOn ? 2 : HwVSimSlotSwitchController.this.mMainSlot].setSimState(1, 1, null);
            checkSimModeDone();
        }
    }

    private class SlotSwitchDefaultState extends State {
        private SlotSwitchDefaultState() {
        }

        public void enter() {
            HwVSimSlotSwitchController.this.logd("DefaultState: enter");
        }

        public void exit() {
            HwVSimSlotSwitchController.this.logd("DefaultState: exit");
        }

        public boolean processMessage(Message msg) {
            Integer index = HwVSimUtilsInner.getCiIndex(msg);
            HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DefaultState: what = ");
            stringBuilder.append(HwVSimSlotSwitchController.this.getWhatToString(msg.what));
            hwVSimSlotSwitchController.logd(stringBuilder.toString());
            int i = msg.what;
            if (i == 11) {
                HwVSimSlotSwitchController.this.transitionTo(HwVSimSlotSwitchController.this.mSwitchCommrilModeState);
            } else if (i != HwVSimSlotSwitchController.EVENT_RADIO_POWER_OFF_DONE) {
                HwVSimSlotSwitchController.this.unhandledMessage(msg);
            } else {
                hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DefaultState: EVENT_RADIO_POWER_OFF_DONE index:");
                stringBuilder.append(index);
                hwVSimSlotSwitchController.logd(stringBuilder.toString());
                HwVSimSlotSwitchController.this.onRadioPowerOffDone(index.intValue());
            }
            return true;
        }
    }

    private class SwitchCommrilModeState extends State {
        private SwitchCommrilModeState() {
        }

        public void enter() {
            HwVSimSlotSwitchController.this.logi("SwitchCommrilModeState: enter");
            int i = 0;
            HwVSimSlotSwitchController.this.mPollingCount = 0;
            while (true) {
                int i2 = i;
                if (i2 >= HwVSimSlotSwitchController.SUB_COUNT) {
                    break;
                }
                HwVSimSlotSwitchController.this.mCis[i2].registerForAvailable(HwVSimSlotSwitchController.this.getHandler(), 83, Integer.valueOf(i2));
                i = i2 + 1;
            }
            HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SwitchCommrilModeState: mExpectSlot : ");
            stringBuilder.append(HwVSimSlotSwitchController.this.mExpectSlot);
            hwVSimSlotSwitchController.logi(stringBuilder.toString());
            hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SwitchCommrilModeState: mMainSlot : ");
            stringBuilder.append(HwVSimSlotSwitchController.this.mMainSlot);
            hwVSimSlotSwitchController.logi(stringBuilder.toString());
            hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SwitchCommrilModeState: mIsVSimOn : ");
            stringBuilder.append(HwVSimSlotSwitchController.this.mIsVSimOn);
            hwVSimSlotSwitchController.logi(stringBuilder.toString());
            if (isNeedSwitchSlot()) {
                Message onCompleted = HwVSimSlotSwitchController.this.obtainMessage(25, Integer.valueOf(HwVSimSlotSwitchController.this.mMainSlot));
                if (HwVSimUtils.isPlatformTwoModems()) {
                    HwVSimSlotSwitchController.this.mCis[HwVSimSlotSwitchController.this.mMainSlot].switchBalongSim(HwVSimSlotSwitchController.this.mExpectSlot + 1, HwVSimSlotSwitchController.this.mMainSlot + 1, onCompleted);
                } else if (HwVSimSlotSwitchController.this.mIsVSimOn) {
                    HwVSimSlotSwitchController.this.mCis[HwVSimSlotSwitchController.this.mMainSlot].switchBalongSim(2, HwVSimSlotSwitchController.this.mMainSlot, HwVSimSlotSwitchController.this.mExpectSlot, onCompleted);
                } else {
                    HwVSimSlotSwitchController.this.mCis[HwVSimSlotSwitchController.this.mMainSlot].switchBalongSim(HwVSimSlotSwitchController.this.mExpectSlot, HwVSimSlotSwitchController.this.mMainSlot, 2, onCompleted);
                }
                HwVSimSlotSwitchController.this.mPollingCount = HwVSimSlotSwitchController.this.mPollingCount + 1;
                HwVSimSlotSwitchController hwVSimSlotSwitchController2 = HwVSimSlotSwitchController.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mPollingCount = ");
                stringBuilder2.append(HwVSimSlotSwitchController.this.mPollingCount);
                hwVSimSlotSwitchController2.logi(stringBuilder2.toString());
                return;
            }
            HwVSimSlotSwitchController.this.processSwitchCommrilMode();
        }

        public void exit() {
            HwVSimSlotSwitchController.this.logi("SwitchCommrilModeState: exit");
            int i = 0;
            HwVSimSlotSwitchController.this.mSwitchingCommrilMode = false;
            while (true) {
                int i2 = i;
                if (i2 < HwVSimSlotSwitchController.SUB_COUNT) {
                    HwVSimSlotSwitchController.this.mCis[i2].unregisterForAvailable(HwVSimSlotSwitchController.this.getHandler());
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }

        public boolean processMessage(Message msg) {
            boolean retVal = true;
            Integer index = HwVSimUtilsInner.getCiIndex(msg);
            HwVSimSlotSwitchController hwVSimSlotSwitchController;
            StringBuilder stringBuilder;
            if (index.intValue() < 0 || index.intValue() >= HwVSimSlotSwitchController.this.mCis.length) {
                hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SwitchCommrilModeState: Invalid index : ");
                stringBuilder.append(index);
                stringBuilder.append(" received with event ");
                stringBuilder.append(msg.what);
                hwVSimSlotSwitchController.logd(stringBuilder.toString());
                return true;
            }
            hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SwitchCommrilModeState: what = ");
            stringBuilder.append(msg.what);
            stringBuilder.append(", message = ");
            stringBuilder.append(HwVSimSlotSwitchController.this.getWhatToString(msg.what));
            stringBuilder.append(", on index ");
            stringBuilder.append(index);
            hwVSimSlotSwitchController.logi(stringBuilder.toString());
            AsyncResult ar = msg.obj;
            int i = msg.what;
            HwVSimSlotSwitchController hwVSimSlotSwitchController2;
            StringBuilder stringBuilder2;
            if (i == 15) {
                if (ar == null || ar.exception != null) {
                    hwVSimSlotSwitchController2 = HwVSimSlotSwitchController.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error! get message ");
                    stringBuilder2.append(HwVSimSlotSwitchController.this.getWhatToString(msg.what));
                    hwVSimSlotSwitchController2.loge(stringBuilder2.toString());
                } else {
                    HwVSimSlotSwitchController.this.mPollingCount = HwVSimSlotSwitchController.this.mPollingCount - 1;
                }
                hwVSimSlotSwitchController2 = HwVSimSlotSwitchController.this;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mPollingCount = ");
                stringBuilder2.append(HwVSimSlotSwitchController.this.mPollingCount);
                hwVSimSlotSwitchController2.logi(stringBuilder2.toString());
                if (HwVSimSlotSwitchController.this.mPollingCount == 0) {
                    restartRild();
                }
            } else if (i == 25) {
                onSwitchSlotDone(ar);
            } else if (i != 83) {
                hwVSimSlotSwitchController2 = HwVSimSlotSwitchController.this;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SwitchCommrilModeState: not handled msg.what = ");
                stringBuilder2.append(msg.what);
                hwVSimSlotSwitchController2.logi(stringBuilder2.toString());
                retVal = false;
            } else {
                onRadioAvailable(index.intValue());
            }
            return retVal;
        }

        private void onSwitchSlotDone(AsyncResult ar) {
            HwVSimSlotSwitchController hwVSimSlotSwitchController;
            StringBuilder stringBuilder;
            if (ar.exception == null) {
                HwVSimSlotSwitchController.this.logi("Switch Sim Slot ok");
                HwVSimSlotSwitchController.this.mPollingCount = HwVSimSlotSwitchController.this.mPollingCount - 1;
                hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mPollingCount = ");
                stringBuilder.append(HwVSimSlotSwitchController.this.mPollingCount);
                hwVSimSlotSwitchController.logi(stringBuilder.toString());
                HwVSimSlotSwitchController.this.processSwitchCommrilMode();
                int[] slots = new int[3];
                if (HwVSimSlotSwitchController.this.mIsVSimOn) {
                    slots[0] = 2;
                    slots[1] = HwVSimSlotSwitchController.this.mMainSlot;
                    slots[2] = HwVSimSlotSwitchController.this.mExpectSlot;
                } else {
                    slots[0] = HwVSimSlotSwitchController.this.mExpectSlot;
                    slots[1] = HwVSimSlotSwitchController.this.mMainSlot;
                    slots[2] = 2;
                }
                HwVSimController.getInstance().setSimSlotTable(slots);
                return;
            }
            hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception ");
            stringBuilder.append(ar.exception);
            hwVSimSlotSwitchController.loge(stringBuilder.toString());
            HwVSimSlotSwitchController.this.sendInitDone();
            HwVSimSlotSwitchController.this.transitionTo(HwVSimSlotSwitchController.this.mDefaultState);
        }

        private void onRadioAvailable(int index) {
            HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRadioAvailable index = ");
            stringBuilder.append(index);
            hwVSimSlotSwitchController.logd(stringBuilder.toString());
            hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mPollingCount = ");
            stringBuilder.append(HwVSimSlotSwitchController.this.mPollingCount);
            hwVSimSlotSwitchController.logi(stringBuilder.toString());
            if (HwVSimSlotSwitchController.this.mPollingCount == 0) {
                HwVSimSlotSwitchController hwVSimSlotSwitchController2;
                StringBuilder stringBuilder2;
                HwVSimSlotSwitchController.this.mRadioAvailable[index] = true;
                hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("mRadioAvailable[");
                stringBuilder3.append(index);
                stringBuilder3.append("] = ");
                stringBuilder3.append(HwVSimSlotSwitchController.this.mRadioAvailable[index]);
                hwVSimSlotSwitchController.logi(stringBuilder3.toString());
                boolean allDone = true;
                int i = 0;
                int i2;
                if (HwVSimSlotSwitchController.this.mIsVSimOn) {
                    if (HwVSimSlotSwitchController.this.mRadioAvailable[HwVSimSlotSwitchController.SUB_VSIM]) {
                        hwVSimSlotSwitchController2 = HwVSimSlotSwitchController.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Expect Slot mRadioAvailable[");
                        stringBuilder2.append(HwVSimSlotSwitchController.this.mExpectSlot);
                        stringBuilder2.append("] ");
                        stringBuilder2.append(HwVSimSlotSwitchController.this.mRadioAvailable[HwVSimSlotSwitchController.this.mExpectSlot]);
                        stringBuilder2.append(" --> true");
                        hwVSimSlotSwitchController2.logi(stringBuilder2.toString());
                        HwVSimSlotSwitchController.this.mRadioAvailable[HwVSimSlotSwitchController.this.mExpectSlot] = true;
                    }
                    while (true) {
                        i2 = i;
                        if (i2 >= HwVSimSlotSwitchController.SUB_COUNT) {
                            break;
                        } else if (!HwVSimSlotSwitchController.this.mRadioAvailable[i2]) {
                            allDone = false;
                            break;
                        } else {
                            i = i2 + 1;
                        }
                    }
                } else {
                    for (i2 = 0; i2 < HwVSimSlotSwitchController.PHONE_COUNT; i2++) {
                        HwVSimSlotSwitchController hwVSimSlotSwitchController3 = HwVSimSlotSwitchController.this;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("mRadioAvailable[");
                        stringBuilder4.append(i2);
                        stringBuilder4.append("] = ");
                        stringBuilder4.append(HwVSimSlotSwitchController.this.mRadioAvailable[i2]);
                        hwVSimSlotSwitchController3.logd(stringBuilder4.toString());
                    }
                    while (true) {
                        i2 = i;
                        if (i2 >= HwVSimSlotSwitchController.PHONE_COUNT) {
                            break;
                        } else if (!HwVSimSlotSwitchController.this.mRadioAvailable[i2]) {
                            allDone = false;
                            break;
                        } else {
                            i = i2 + 1;
                        }
                    }
                }
                if (allDone) {
                    HwVSimSlotSwitchController.this.logd("all done");
                    hwVSimSlotSwitchController2 = HwVSimSlotSwitchController.this;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("mCompleteMsg = ");
                    stringBuilder5.append(HwVSimSlotSwitchController.this.mCompleteMsg);
                    hwVSimSlotSwitchController2.logd(stringBuilder5.toString());
                    if (HwVSimSlotSwitchController.this.mCompleteMsg != null) {
                        HwVSimSlotSwitchController.this.logi("Switch CommrilMode Done!!");
                        AsyncResult.forMessage(HwVSimSlotSwitchController.this.mCompleteMsg);
                        HwVSimSlotSwitchController.this.mCompleteMsg.sendToTarget();
                        HwVSimSlotSwitchController.this.mCompleteMsg = null;
                        if (isNeedSwitchSlot()) {
                            hwVSimSlotSwitchController2 = HwVSimSlotSwitchController.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("update mainSlot to ");
                            stringBuilder2.append(HwVSimSlotSwitchController.this.mExpectSlot);
                            hwVSimSlotSwitchController2.logi(stringBuilder2.toString());
                            HwVSimSlotSwitchController.this.mMainSlot = HwVSimSlotSwitchController.this.mExpectSlot;
                            hwVSimSlotSwitchController2 = HwVSimSlotSwitchController.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("mMainSlot = ");
                            stringBuilder2.append(HwVSimSlotSwitchController.this.mMainSlot);
                            hwVSimSlotSwitchController2.logd(stringBuilder2.toString());
                        }
                        if (HwVSimSlotSwitchController.this.mUserCompleteMsg != null) {
                            HwVSimSlotSwitchController.this.logi("switchCommrilMode ------>>> end");
                            AsyncResult.forMessage(HwVSimSlotSwitchController.this.mUserCompleteMsg, Boolean.valueOf(true), null);
                            HwVSimSlotSwitchController.this.mUserCompleteMsg.sendToTarget();
                            HwVSimSlotSwitchController.this.mUserCompleteMsg = null;
                        }
                        HwVSimSlotSwitchController.this.sendInitDone();
                        HwVSimSlotSwitchController.this.transitionTo(HwVSimSlotSwitchController.this.mDefaultState);
                    }
                } else {
                    HwVSimSlotSwitchController.this.logd("not done");
                }
            }
        }

        private boolean isNeedSwitchSlot() {
            return HwVSimSlotSwitchController.this.mExpectSlot != HwVSimSlotSwitchController.this.mMainSlot;
        }

        private void restartRild() {
            if (HwVSimSlotSwitchController.this.mExpectCommrilMode != CommrilMode.NON_MODE) {
                HwVSimSlotSwitchController hwVSimSlotSwitchController = HwVSimSlotSwitchController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setCommrilMode to ");
                stringBuilder.append(HwVSimSlotSwitchController.this.mExpectCommrilMode);
                hwVSimSlotSwitchController.logi(stringBuilder.toString());
                HwVSimSlotSwitchController.this.setCommrilMode(HwVSimSlotSwitchController.this.mExpectCommrilMode);
                HwVSimSlotSwitchController.this.mExpectCommrilMode = CommrilMode.NON_MODE;
            }
            resetStatus();
            HwVSimSlotSwitchController.this.restartRildBySubState();
        }

        private void resetStatus() {
            HwVSimSlotSwitchController.this.logi("resetStatus");
            for (int i = 0; i < HwVSimSlotSwitchController.SUB_COUNT; i++) {
                HwVSimSlotSwitchController.this.mRadioAvailable[i] = false;
            }
        }
    }

    public static void create(Context context, CommandsInterface vsimCi, CommandsInterface[] cis) {
        slogd("create");
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new HwVSimSlotSwitchController(context, vsimCi, cis);
                sInstance.start();
            } else {
                throw new RuntimeException("VSimSlotSwitchController already created");
            }
        }
    }

    public static HwVSimSlotSwitchController getInstance() {
        HwVSimSlotSwitchController hwVSimSlotSwitchController;
        synchronized (sLock) {
            if (sInstance != null) {
                hwVSimSlotSwitchController = sInstance;
            } else {
                throw new RuntimeException("VSimSlotSwitchController not yet created");
            }
        }
        return hwVSimSlotSwitchController;
    }

    private HwVSimSlotSwitchController(Context c, CommandsInterface vsimCi, CommandsInterface[] cis) {
        int i;
        super(LOG_TAG, Looper.myLooper());
        logd("VSimSlotSwitchController");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IS_SUPPORT_FULL_NETWORK: ");
        stringBuilder.append(IS_SUPPORT_FULL_NETWORK);
        logi(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("IS_HISI_CDMA_SUPPORTED: ");
        stringBuilder.append(IS_HISI_CDMA_SUPPORTED);
        logi(stringBuilder.toString());
        this.mCis = new CommandsInterface[(cis.length + 1)];
        for (i = 0; i < cis.length; i++) {
            this.mCis[i] = cis[i];
        }
        this.mCis[cis.length] = vsimCi;
        initWhatToStringMap();
        this.mSlotSwitchDone = false;
        this.mGotSimSlot = false;
        this.mRadioAvailable = new boolean[SUB_COUNT];
        this.mRadioPowerStatus = new boolean[SUB_COUNT];
        for (i = 0; i < SUB_COUNT; i++) {
            this.mRadioAvailable[i] = false;
            this.mRadioPowerStatus[i] = false;
        }
        this.mMainSlot = -1;
        this.mExpectSlot = -1;
        this.mExpectCommrilMode = CommrilMode.NON_MODE;
        this.mIsVSimOn = false;
        this.mIsVSimEnabled = false;
        this.mInitDoneSent = false;
        this.mSwitchingCommrilMode = false;
        addState(this.mDefaultState);
        addState(this.mInitialState, this.mDefaultState);
        addState(this.mSwitchCommrilModeState, this.mDefaultState);
        setInitialState(this.mInitialState);
    }

    protected void logd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    private static void slogd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    protected void logi(String s) {
        HwVSimLog.VSimLogI(LOG_TAG, s);
    }

    protected void loge(String s) {
        HwVSimLog.VSimLogE(LOG_TAG, s);
    }

    protected void unhandledMessage(Message msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" - unhandledMessage: msg.what=");
        stringBuilder.append(msg.what);
        logd(stringBuilder.toString());
    }

    protected String getWhatToString(int what) {
        String result = null;
        if (this.mWhatToStringMap != null) {
            result = (String) this.mWhatToStringMap.get(Integer.valueOf(what));
        }
        if (result != null) {
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<unknown message> - ");
        stringBuilder.append(what);
        return stringBuilder.toString();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println(" dummy");
    }

    private void initWhatToStringMap() {
        this.mWhatToStringMap = new HashMap();
        this.mWhatToStringMap.put(Integer.valueOf(2), "EVENT_GET_SIM_SLOT_DONE");
        this.mWhatToStringMap.put(Integer.valueOf(3), "EVENT_GET_SIM_STATE_DONE");
        this.mWhatToStringMap.put(Integer.valueOf(4), "EVENT_SET_SIM_STATE_DONE");
        this.mWhatToStringMap.put(Integer.valueOf(11), "CMD_SWITCH_COMMRIL_MODE");
        this.mWhatToStringMap.put(Integer.valueOf(12), "EVENT_SWITCH_COMMRIL_MODE_DONE");
        this.mWhatToStringMap.put(Integer.valueOf(15), "EVENT_SET_CDMA_MODE_SIDE_DONE");
        this.mWhatToStringMap.put(Integer.valueOf(83), "EVENT_RADIO_AVAILABLE");
        this.mWhatToStringMap.put(Integer.valueOf(25), "EVENT_SWITCH_SLOT_DONE");
        this.mWhatToStringMap.put(Integer.valueOf(35), "EVENT_INITIAL_TIMEOUT");
        this.mWhatToStringMap.put(Integer.valueOf(EVENT_RADIO_POWER_OFF_DONE), "EVENT_RADIO_POWER_OFF_DONE");
    }

    private void sendInitDone() {
        if (!this.mInitDoneSent) {
            HwVSimController.getInstance().getHandler().sendEmptyMessage(5);
            this.mInitDoneSent = true;
        }
    }

    public CommrilMode getCommrilMode() {
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

    private void setCommrilMode(CommrilMode mode) {
        SystemProperties.set("persist.radio.commril_mode", mode.toString());
    }

    public CommrilMode getExpectCommrilMode(int mainSlot, int[] cardType) {
        CommrilMode expectCommrilMode = CommrilMode.NON_MODE;
        if (mainSlot == -1) {
            logd("main slot invalid");
            return expectCommrilMode;
        }
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        if (cardType[mainSlot] == 2 || cardType[mainSlot] == 3) {
            expectCommrilMode = IS_HISI_CDMA_SUPPORTED ? CommrilMode.HISI_CGUL_MODE : CommrilMode.CLG_MODE;
        } else if ((cardType[mainSlot] == 1 || cardType[mainSlot] == 0) && (cardType[slaveSlot] == 2 || cardType[slaveSlot] == 3)) {
            expectCommrilMode = IS_HISI_CDMA_SUPPORTED ? CommrilMode.HISI_CG_MODE : CommrilMode.CG_MODE;
        } else if (cardType[mainSlot] == 1 || cardType[slaveSlot] == 1) {
            expectCommrilMode = IS_HISI_CDMA_SUPPORTED ? CommrilMode.HISI_CGUL_MODE : CommrilMode.ULG_MODE;
        } else {
            expectCommrilMode = CommrilMode.NON_MODE;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[getExpectCommrilMode]: expectCommrilMode = ");
        stringBuilder.append(expectCommrilMode);
        logd(stringBuilder.toString());
        return expectCommrilMode;
    }

    private void switchCommrilMode() {
        logd("switchCommrilMode");
        this.mCompleteMsg = obtainMessage(12, Integer.valueOf(0));
        Message msg = obtainMessage(11, Integer.valueOf(0));
        AsyncResult.forMessage(msg);
        msg.sendToTarget();
    }

    private void processSwitchCommrilMode() {
        CommrilMode newCommrilMode = this.mExpectCommrilMode;
        int mainSlot = this.mMainSlot;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[switchCommrilMode]: newCommrilMode = ");
        stringBuilder.append(newCommrilMode);
        stringBuilder.append(", mainSlot = ");
        stringBuilder.append(mainSlot);
        stringBuilder.append(", is vsim on = ");
        stringBuilder.append(this.mIsVSimOn);
        logi(stringBuilder.toString());
        if (mainSlot < 0 || mainSlot >= this.mCis.length) {
            logi("main slot invalid");
            return;
        }
        int i;
        if (mainSlot == 0) {
            i = 1;
        } else {
            i = 0;
        }
        int balongSlot = this.mIsVSimOn ? 2 : mainSlot;
        switch (newCommrilMode) {
            case HISI_CGUL_MODE:
                this.mCis[balongSlot].setCdmaModeSide(0, obtainMessage(15));
                this.mPollingCount++;
                logi("[switchCommrilMode]: Send set HISI_CGUL_MODE request done...");
                break;
            case HISI_CG_MODE:
                this.mCis[balongSlot].setCdmaModeSide(1, obtainMessage(15));
                this.mPollingCount++;
                logi("[switchCommrilMode]: Send set HISI_CG_MODE request done...");
                break;
            case HISI_VSIM_MODE:
                this.mCis[balongSlot].setCdmaModeSide(2, obtainMessage(15));
                this.mPollingCount++;
                logi("[switchCommrilMode]: Send set HISI_VSIM_MODE request done...");
                break;
            default:
                loge("[switchCommrilMode]: Error!! Shouldn't enter here!!");
                break;
        }
    }

    public void switchCommrilMode(CommrilMode expectCommrilMode, int expectSlot, int mainSlot, boolean isVSimOn, Message onCompleteMsg) {
        logi("switchCommrilMode ------>>> begin");
        if (IS_SUPPORT_FULL_NETWORK || HwVSimUtilsInner.isChinaTelecom()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("expectCommrilMode = ");
            stringBuilder.append(expectCommrilMode);
            stringBuilder.append(", expectSlot = ");
            stringBuilder.append(expectSlot);
            stringBuilder.append(", mainSlot = ");
            stringBuilder.append(mainSlot);
            stringBuilder.append(", isVSimOn = ");
            stringBuilder.append(isVSimOn);
            logi(stringBuilder.toString());
            this.mExpectCommrilMode = expectCommrilMode;
            this.mUserCompleteMsg = onCompleteMsg;
            this.mExpectSlot = expectSlot;
            this.mMainSlot = mainSlot;
            this.mIsVSimOn = isVSimOn;
            if (IS_FAST_SWITCH_SIMSLOT) {
                setAllRaidoPowerOff();
            } else {
                switchCommrilMode();
            }
            return;
        }
        logi("switchCommrilMode ------>>> end, because full net and telecom not support");
        if (onCompleteMsg != null) {
            AsyncResult.forMessage(onCompleteMsg, Boolean.valueOf(false), null);
            onCompleteMsg.sendToTarget();
        }
    }

    private void setAllRaidoPowerOff() {
        int subId;
        HwVSimController mVSimController = HwVSimController.getInstance();
        mVSimController.setIsWaitingSwitchCdmaModeSide(true);
        for (subId = 0; subId < this.mCis.length; subId++) {
            this.mRadioPowerStatus[subId] = this.mCis[subId].getRadioState().isOn();
        }
        for (subId = 0; subId < this.mCis.length; subId++) {
            StringBuilder stringBuilder;
            if (this.mRadioPowerStatus[subId]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("setAllRaidoPowerOff:mRadioPowerOnStatus[");
                stringBuilder.append(subId);
                stringBuilder.append("]=");
                stringBuilder.append(this.mRadioPowerStatus[subId]);
                stringBuilder.append(" -> off");
                logd(stringBuilder.toString());
                Message onCompleted = obtainMessage(EVENT_RADIO_POWER_OFF_DONE, Integer.valueOf(subId));
                ((GsmCdmaPhone) mVSimController.getPhoneBySub(subId)).getServiceStateTracker().setDesiredPowerState(false);
                this.mCis[subId].setRadioPower(false, onCompleted);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("setAllRaidoPowerOff:mRadioPowerOnStatus[");
                stringBuilder.append(subId);
                stringBuilder.append("]=");
                stringBuilder.append(this.mRadioPowerStatus[subId]);
                stringBuilder.append(" is off");
                logd(stringBuilder.toString());
                onRadioPowerOffDone(subId);
            }
        }
    }

    private void onRadioPowerOffDone(int subId) {
        int i = 0;
        this.mRadioPowerStatus[subId] = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRadioPowerOffDone:mRadioPowerStatus[");
        stringBuilder.append(subId);
        stringBuilder.append("]=");
        stringBuilder.append(this.mRadioPowerStatus[subId]);
        logd(stringBuilder.toString());
        boolean isAllRadioPowerOff = true;
        while (i < this.mCis.length) {
            if (this.mRadioPowerStatus[i]) {
                isAllRadioPowerOff = false;
                break;
            }
            i++;
        }
        if (isAllRadioPowerOff && !this.mSwitchingCommrilMode) {
            logd("onRadioPowerOffDone: AllRadioPowerOff -> switchCommrilMode");
            this.mSwitchingCommrilMode = true;
            switchCommrilMode();
        }
    }

    public void restartRildBySubState() {
        HwVSimController mVSimController = HwVSimController.getInstance();
        for (int subId = 0; subId < PHONE_COUNT; subId++) {
            if (mVSimController.getSubState(subId) != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("restartRild : setDesiredPowerState is true for subId:");
                stringBuilder.append(subId);
                logd(stringBuilder.toString());
                ((GsmCdmaPhone) mVSimController.getPhoneBySub(subId)).getServiceStateTracker().setDesiredPowerState(true);
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("restartRild : no need to setDesiredPowerState is true for subId:");
                stringBuilder2.append(subId);
                logd(stringBuilder2.toString());
            }
        }
        logi("restart rild");
        if (this.mIsVSimOn) {
            this.mCis[SUB_VSIM].restartRild(null);
        } else {
            this.mCis[this.mMainSlot].restartRild(null);
        }
        if (!HwVSimController.getInstance().isVSimEnabled()) {
            HwVSimPhoneFactory.setIsVsimEnabledProp(false);
        }
        HwVSimController.getInstance().setIsWaitingSwitchCdmaModeSide(false);
        HwVSimController.getInstance().setIsWaitingNvMatchUnsol(false);
        HwVSimNvMatchController.getInstance().storeIfNeedRestartRildForNvMatch(false);
    }

    public static boolean isCDMACard(int cardtype) {
        return cardtype == 2 || cardtype == 3;
    }

    CommrilMode getVSimOnCommrilMode(int mainSlot, int[] cardTypes) {
        CommrilMode vSimOnCommrilMode;
        StringBuilder stringBuilder;
        int slaveSlot = mainSlot == 0 ? 1 : 0;
        boolean mainSlotIsCDMACard = isCDMACard(cardTypes[mainSlot]);
        boolean slaveSlotIsCDMACard = isCDMACard(cardTypes[slaveSlot]);
        if (mainSlotIsCDMACard && slaveSlotIsCDMACard) {
            vSimOnCommrilMode = CommrilMode.HISI_CG_MODE;
        } else if (mainSlotIsCDMACard) {
            vSimOnCommrilMode = CommrilMode.HISI_VSIM_MODE;
        } else if (slaveSlotIsCDMACard) {
            vSimOnCommrilMode = CommrilMode.HISI_CG_MODE;
        } else {
            vSimOnCommrilMode = getCommrilMode();
            stringBuilder = new StringBuilder();
            stringBuilder.append("no c-card, not change commril mode. vSimOnCommrilMode = ");
            stringBuilder.append(vSimOnCommrilMode);
            logd(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("getVSimOnCommrilMode: mainSlot = ");
        stringBuilder.append(mainSlot);
        stringBuilder.append(", cardTypes = ");
        stringBuilder.append(Arrays.toString(cardTypes));
        stringBuilder.append(", mode = ");
        stringBuilder.append(vSimOnCommrilMode);
        logd(stringBuilder.toString());
        return vSimOnCommrilMode;
    }
}
